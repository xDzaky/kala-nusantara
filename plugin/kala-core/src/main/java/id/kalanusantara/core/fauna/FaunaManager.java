package id.kalanusantara.core.fauna;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.journal.JurnalManager;
import id.kalanusantara.core.notify.KalaNotificationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * FaunaManager — Pengelola Atlas Fauna Nusantara (Tingkat 1 Vanilla-Safe).
 * Menyimpan progres observasi satwa dan memberikan penghargaan penemuan.
 */
public class FaunaManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final Map<UUID, Set<String>> discoveriesCache = new ConcurrentHashMap<>();

    public FaunaManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        log.info("[FaunaManager] Inisialisasi Atlas Fauna Nusantara selesai.");
    }

    /**
     * Memuat daftar penemuan pemain dari database saat pemain login
     */
    public void loadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> found = new HashSet<>();
            String sql = "SELECT fauna_id FROM player_fauna_discovery WHERE uuid = ?";
            try (Connection conn = plugin.getStorageManager().ensureConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    found.add(rs.getString("fauna_id"));
                }
                discoveriesCache.put(uuid, found);
            } catch (SQLException e) {
                log.warning("[FaunaManager] Gagal memuat fauna untuk " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void unloadPlayer(UUID uuid) {
        discoveriesCache.remove(uuid);
    }

    public boolean isDiscovered(UUID uuid, String faunaId) {
        Set<String> set = discoveriesCache.get(uuid);
        return set != null && set.contains(faunaId);
    }

    public Set<String> getDiscovered(UUID uuid) {
        return discoveriesCache.getOrDefault(uuid, Collections.emptySet());
    }

    public int getDiscoveredCount(UUID uuid) {
        return getDiscovered(uuid).size();
    }

    /**
     * Merekam penemuan fauna baru oleh pemain
     */
    public boolean discover(Player player, FaunaSpecies species) {
        if (player == null || species == null) return false;
        UUID uuid = player.getUniqueId();

        Set<String> set = discoveriesCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (set.contains(species.getId())) {
            return false; // Sudah pernah ditemukan
        }

        set.add(species.getId());

        // Simpan ke database async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT OR IGNORE INTO player_fauna_discovery (uuid, fauna_id, discovered_at) VALUES (?, ?, ?)";
            try (Connection conn = plugin.getStorageManager().ensureConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, species.getId());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.severe("[FaunaManager] Gagal menyimpan penemuan fauna: " + e.getMessage());
            }
        });

        // 1. Hadiah Rupiah
        plugin.getEconomy().deposit(uuid, species.getRewardRupiah(), "Hadiah penemuan " + species.getDisplayName());

        // 2. Catat ke Jurnal Penjelajah
        plugin.getJurnalManager().addEntry(
                player,
                "discovery",
                "Menemukan " + species.getDisplayName(),
                "Berhasil mengamati " + species.getDisplayName() + " di habitat " + species.getHabitat() +
                        ". Lore: " + species.getLore() + " (+Rp " + String.format("%.0f", species.getRewardRupiah()) + ")"
        );

        // 3. Notifikasi visual & audio per UI Ownership Matrix
        plugin.getNotificationService().sendTitle(
                player,
                KalaNotificationService.Priority.QUEST,
                Component.text("✦ SATWA DITEMUKAN! ✦", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text(species.getDisplayName() + " — Atlas Diperbarui", NamedTextColor.YELLOW)
        );

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        player.sendMessage(Component.text("§8[§a🐾 Atlas Fauna§8] §aSelamat! Anda berhasil mencatat §e" +
                species.getDisplayName() + " §ake Atlas!"));
        player.sendMessage(Component.text("§8[§a🐾 Atlas Fauna§8] §7Hadiah konservasi: §6+Rp " +
                String.format("%.0f", species.getRewardRupiah()) + " Rupiah Kala§7. Buka §e/fauna §7untuk melihat detail."));

        // 4. Periksa apakah sudah menemukan semua (6/6)
        if (set.size() >= FaunaSpecies.values().length) {
            plugin.getCosmeticManager().unlockTitle(player, "SAHABAT_SATWA");
            player.sendMessage(Component.text("§6§l✦ PENCAPAIAN BESAR: §eKoleksi Atlas Fauna Nusantara Lengkap! Gelar §b[Sahabat Satwa] §eterbuka di §e/kosmetik§6!"));
        }

        return true;
    }
}
