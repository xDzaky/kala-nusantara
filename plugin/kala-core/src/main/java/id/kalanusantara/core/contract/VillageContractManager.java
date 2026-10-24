package id.kalanusantara.core.contract;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * VillageContractManager — Mengelola Kontrak Gotong Royong Kampung Mingguan (Audit V8/V9).
 * Menggunakan rotasi minggu ISO-8601 dinamis, pemuatan progress persisten dari DB,
 * idempotency key (uuid, week_id, contract_id), dan jaminan entitlement offline.
 */
public class VillageContractManager {

    public record VillageContract(
            String id,
            String weekId,
            String title,
            String category,
            int targetCount,
            double rewardRupiah,
            int rewardExp
    ) {}

    private final KalaCorePlugin plugin;
    private final Logger log;

    private final Map<String, VillageContract> activeContracts = new ConcurrentHashMap<>();
    // Cache: uuid -> Map<contractId, Integer currentCount>
    private final Map<UUID, Map<String, Integer>> playerProgress = new ConcurrentHashMap<>();
    // Cache: uuid -> Set<contractId completed>
    private final Map<UUID, Set<String>> completedContracts = new ConcurrentHashMap<>();

    public VillageContractManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    /**
     * Menghitung ID minggu ISO-8601 dinamis (contoh: "WEEK_2026_W43")
     */
    public static String getCurrentIsoWeekId() {
        LocalDate now = LocalDate.now();
        int year = now.get(IsoFields.WEEK_BASED_YEAR);
        int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("WEEK_%04d_W%02d", year, week);
    }

    public void initialize() {
        activeContracts.clear();
        playerProgress.clear();
        completedContracts.clear();

        setupWeeklyContracts();

        // Muat progress untuk seluruh pemain online saat inisialisasi / reload
        for (Player p : Bukkit.getOnlinePlayers()) {
            loadPlayerProgress(p.getUniqueId());
        }

        log.info("[VillageContracts] Sistem Kontrak Kampung Mingguan siap (" + activeContracts.size() + " kontrak aktif untuk " + getCurrentIsoWeekId() + ").");
    }

    private void setupWeeklyContracts() {
        String currentWeek = getCurrentIsoWeekId();
        activeContracts.put("kontrak_panen_padi", new VillageContract(
                "kontrak_panen_padi", currentWeek, "Gotong Royong Panen Lumbung",
                "PERTANIAN", 64, 500.0, 80
        ));
        activeContracts.put("kontrak_pasokan_kayu", new VillageContract(
                "kontrak_pasokan_kayu", currentWeek, "Pasokan Kayu Dermaga Pesisir",
                "KEHUTANAN", 48, 450.0, 70
        ));
        activeContracts.put("kontrak_sampel_vulkanik", new VillageContract(
                "kontrak_sampel_vulkanik", currentWeek, "Pengumpulan Sampel Kawah Murka",
                "RISET", 3, 750.0, 120
        ));
        activeContracts.put("kontrak_ikan_pesisir", new VillageContract(
                "kontrak_ikan_pesisir", currentWeek, "Pasokan Hasil Laut Labuhan",
                "PERIKANAN", 24, 600.0, 100
        ));
    }

    public Collection<VillageContract> getActiveContracts() {
        return Collections.unmodifiableCollection(activeContracts.values());
    }

    public VillageContract getContract(String id) {
        return activeContracts.get(id);
    }

    public boolean isCompleted(UUID uuid, String contractId) {
        Set<String> set = completedContracts.get(uuid);
        return set != null && set.contains(contractId);
    }

    public int getPlayerProgress(UUID uuid, String contractId) {
        Map<String, Integer> map = playerProgress.get(uuid);
        return map != null ? map.getOrDefault(contractId, 0) : 0;
    }

    public void loadPlayerProgress(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String currentWeek = getCurrentIsoWeekId();
            String sql = "SELECT contract_id, current_count, completed FROM player_contract_progress WHERE uuid = ? AND week_id = ?";
            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, currentWeek);
                    try (ResultSet rs = ps.executeQuery()) {
                        Map<String, Integer> progMap = playerProgress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                        Set<String> compSet = completedContracts.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
                        while (rs.next()) {
                            String cId = rs.getString("contract_id");
                            int count = rs.getInt("current_count");
                            boolean done = rs.getInt("completed") == 1;
                            progMap.put(cId, count);
                            if (done) {
                                compSet.add(cId);
                            }
                        }
                    }
                } catch (SQLException e) {
                    log.warning("[VillageContracts] Gagal memuat progress kontrak untuk " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    public void onPlayerQuit(UUID uuid) {
        playerProgress.remove(uuid);
        completedContracts.remove(uuid);
    }

    /**
     * Menambahkan progress kontribusi pemain untuk kontrak tertentu secara idempoten
     */
    public void addProgress(Player player, String contractId, int amount) {
        if (player == null || amount <= 0) return;
        VillageContract contract = activeContracts.get(contractId);
        if (contract == null) return;

        final UUID uuid = player.getUniqueId();
        final String currentWeek = getCurrentIsoWeekId();

        Set<String> completed = completedContracts.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        if (completed.contains(contractId)) {
            return; // Sudah selesai untuk minggu ini
        }

        Map<String, Integer> progMap = playerProgress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        int newCount = progMap.getOrDefault(contractId, 0) + amount;
        progMap.put(contractId, newCount);

        final boolean isTargetReached = newCount >= contract.targetCount();

        if (isTargetReached) {
            completed.add(contractId);
        } else {
            player.sendMessage(Component.text("§7[Kontrak] " + contract.title() + ": §e" + newCount + "/" + contract.targetCount(), NamedTextColor.GRAY));
        }

        // Simpan progress ke database secara asynchronous di bawah dbLock
        final int finalCount = newCount;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO player_contract_progress (uuid, week_id, contract_id, current_count, completed, claimed, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid, week_id, contract_id) DO UPDATE SET
                    current_count = excluded.current_count,
                    completed = excluded.completed,
                    completed_at = CASE WHEN excluded.completed = 1 THEN excluded.completed_at ELSE player_contract_progress.completed_at END
            """;
            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, currentWeek);
                    ps.setString(3, contractId);
                    ps.setInt(4, finalCount);
                    ps.setInt(5, isTargetReached ? 1 : 0);
                    ps.setInt(6, isTargetReached ? 1 : 0);
                    ps.setLong(7, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    log.severe("[VillageContracts] Gagal update progress kontrak: " + e.getMessage());
                }
            }

            if (isTargetReached) {
                completeContract(player, contract);
            }
        });
    }

    private void completeContract(Player player, VillageContract contract) {
        UUID uuid = player.getUniqueId();
        String currentWeek = getCurrentIsoWeekId();

        boolean depositSuccess = plugin.getEconomy().deposit(uuid, contract.rewardRupiah(), "Kontrak " + contract.title());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("✦ KONTRAK KAMPUNG SELESAI ✦", NamedTextColor.GOLD));
                player.sendMessage(Component.text("Judul: §e" + contract.title(), NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Imbalan: §6+" + plugin.getEconomy().format(contract.rewardRupiah()) + " §7dan §b+" + contract.rewardExp() + " EXP", NamedTextColor.GREEN));
                player.sendMessage(Component.empty());
                player.giveExp(contract.rewardExp());
            } else {
                // Pemain offline saat reward selesai diproses
                plugin.getStorageManager().recordRewardEntitlement(
                        uuid,
                        "CONTRACT",
                        contract.id() + "_" + currentWeek,
                        depositSuccess ? 0.0 : contract.rewardRupiah(),
                        contract.rewardExp()
                );
            }
        });
    }
}
