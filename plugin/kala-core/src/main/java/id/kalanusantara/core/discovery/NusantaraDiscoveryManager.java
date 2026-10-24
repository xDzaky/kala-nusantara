package id.kalanusantara.core.discovery;

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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * NusantaraDiscoveryManager — Mengelola Katalog Penemuan Nusantara (Audit V8/V9).
 * Memberikan progresi jangka panjang observasi satwa, landmark, dan sampel geologi tanpa kekerasan.
 * SQLite sebagai Single Source of Truth (SSOT), dengan fallback entitlement bila pemain offline.
 */
public class NusantaraDiscoveryManager {

    public enum DiscoveryType {
        FAUNA,
        GEOLOGI,
        LANDMARK
    }

    private final KalaCorePlugin plugin;
    private final Logger log;

    // Cache: uuid -> Set<String> (discovery_type + ":" + entry_id)
    private final Map<UUID, Set<String>> playerDiscoveries = new ConcurrentHashMap<>();

    public NusantaraDiscoveryManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        log.info("[DiscoveryManager] Katalog Penemuan Nusantara diinisialisasi.");
    }

    public boolean hasDiscovered(UUID uuid, DiscoveryType type, String entryId) {
        Set<String> set = playerDiscoveries.get(uuid);
        return set != null && set.contains(type.name() + ":" + entryId);
    }

    /**
     * Mencatat observasi baru secara durable ke SQLite dengan unique constraint
     */
    public boolean recordDiscovery(Player player, DiscoveryType type, String entryId, String island, String displayName) {
        if (player == null || type == null || entryId == null) return false;
        final UUID uuid = player.getUniqueId();
        final String key = type.name() + ":" + entryId;

        Set<String> set = playerDiscoveries.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        if (set.contains(key)) {
            return false; // Sudah pernah ditemukan dalam cache lokal
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT OR IGNORE INTO nusantara_discoveries (uuid, discovery_type, entry_id, island, discovered_at) VALUES (?, ?, ?, ?, ?)";
            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, type.name());
                    ps.setString(3, entryId);
                    ps.setString(4, island != null ? island : "Nusantara");
                    ps.setLong(5, System.currentTimeMillis());
                    int inserted = ps.executeUpdate();

                    if (inserted > 0) {
                        // DB insertion succeeded — commit to local cache (SSOT alignment)
                        set.add(key);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                                player.sendMessage(Component.empty());
                                player.sendMessage(Component.text("✦ CATATAN PENEMUAN BARU TERBUKA ✦", NamedTextColor.GOLD));
                                player.sendMessage(Component.text("Kategori: §e" + type.name() + " §7| Wilayah: §b" + island, NamedTextColor.GRAY));
                                player.sendMessage(Component.text("Entri: §a" + displayName, NamedTextColor.GREEN));
                                player.sendMessage(Component.text("Hadiah: §6+Rp 250 Rupiah Kala §7dan §b+50 EXP", NamedTextColor.YELLOW));
                                player.sendMessage(Component.empty());

                                plugin.getEconomy().deposit(uuid, 250.0, "Hadiah observasi " + displayName);
                                player.giveExp(50);
                            } else {
                                // Pemain offline saat hadiah siap diberikan: simpan ke player_reward_entitlements
                                plugin.getStorageManager().recordRewardEntitlement(uuid, "DISCOVERY", key, 250.0, 50);
                            }
                        });
                    }
                } catch (SQLException e) {
                    log.severe("[DiscoveryManager] Gagal mencatat penemuan: " + e.getMessage());
                }
            }
        });

        return true;
    }

    public void loadPlayerDiscoveries(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT discovery_type, entry_id FROM nusantara_discoveries WHERE uuid = ?";
            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        Set<String> set = playerDiscoveries.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
                        while (rs.next()) {
                            set.add(rs.getString("discovery_type") + ":" + rs.getString("entry_id"));
                        }
                    }
                } catch (SQLException e) {
                    log.warning("[DiscoveryManager] Gagal memuat discovery untuk " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    public int getDiscoveryCount(UUID uuid) {
        Set<String> set = playerDiscoveries.get(uuid);
        return set != null ? set.size() : 0;
    }
}
