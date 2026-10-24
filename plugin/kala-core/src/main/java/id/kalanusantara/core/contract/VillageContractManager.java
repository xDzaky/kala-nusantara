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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * VillageContractManager — Mengelola Kontrak Gotong Royong Kampung Mingguan (Audit V8).
 * Menyediakan 3–5 kontrak mingguan dengan batas penyelesaian agar tidak terjadi inflasi ekonomi.
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

    public void initialize() {
        setupWeeklyContracts();
        log.info("[VillageContracts] Sistem Kontrak Kampung Mingguan siap (" + activeContracts.size() + " kontrak aktif).");
    }

    private void setupWeeklyContracts() {
        String currentWeek = "WEEK_2026_W43";
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

    /**
     * Menambahkan progress kontribusi pemain untuk kontrak tertentu
     */
    public void addProgress(Player player, String contractId, int amount) {
        if (player == null || amount <= 0) return;
        VillageContract contract = activeContracts.get(contractId);
        if (contract == null) return;

        final UUID uuid = player.getUniqueId();
        Set<String> completed = completedContracts.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        if (completed.contains(contractId)) {
            return; // Sudah selesai
        }

        Map<String, Integer> progMap = playerProgress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        int newCount = progMap.getOrDefault(contractId, 0) + amount;
        progMap.put(contractId, newCount);

        if (newCount >= contract.targetCount()) {
            completed.add(contractId);
            completeContract(player, contract);
        } else {
            player.sendMessage(Component.text("§7[Kontrak] " + contract.title() + ": §e" + newCount + "/" + contract.targetCount(), NamedTextColor.GRAY));
        }

        // Simpan progress ke database secara asynchronous
        final int finalCount = newCount;
        final boolean isCompleted = newCount >= contract.targetCount();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO player_contract_progress (uuid, contract_id, current_count, completed, claimed, completed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid, contract_id) DO UPDATE SET
                    current_count = excluded.current_count,
                    completed = excluded.completed,
                    completed_at = CASE WHEN excluded.completed = 1 THEN excluded.completed_at ELSE player_contract_progress.completed_at END
            """;
            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, contractId);
                    ps.setInt(3, finalCount);
                    ps.setInt(4, isCompleted ? 1 : 0);
                    ps.setInt(5, isCompleted ? 1 : 0);
                    ps.setLong(6, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    log.severe("[VillageContracts] Gagal update progress kontrak: " + e.getMessage());
                }
            }
        });
    }

    private void completeContract(Player player, VillageContract contract) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ KONTRAK KAMPUNG SELESAI ✦", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Judul: §e" + contract.title(), NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Imbalan: §6+" + plugin.getEconomy().format(contract.rewardRupiah()) + " §7dan §b+" + contract.rewardExp() + " EXP", NamedTextColor.GREEN));
        player.sendMessage(Component.empty());

        plugin.getEconomy().deposit(player.getUniqueId(), contract.rewardRupiah(), "Kontrak " + contract.title());
        player.giveExp(contract.rewardExp());
    }
}
