package id.kalanusantara.core.cosmetics;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
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
 * CosmeticManager — Gelar dan kosmetik etis 100% berbasis pencapaian gameplay (Non-P2W).
 * Master Design Bab 1 & Bab 13: Kosmetik etis tidak memberi keuntungan tempur/ekonomi.
 */
public class CosmeticManager {

    private final KalaCorePlugin plugin;
    private final Logger log;

    // Cache UUID -> Data Kosmetik
    private final Map<UUID, PlayerCosmeticData> cache = new ConcurrentHashMap<>();

    public CosmeticManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        // Task memunculkan partikel trail setiap 8 tick (0.4 detik) untuk pemain aktif
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerCosmeticData data = cache.get(player.getUniqueId());
                if (data == null || "NONE".equalsIgnoreCase(data.activeTrail)) continue;

                // Hanya spawn jika bergerak atau berdiri di darat
                switch (data.activeTrail.toUpperCase()) {
                    case "MELATI" -> player.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER, player.getLocation().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0.0
                    );
                    case "BARA" -> player.getWorld().spawnParticle(
                        Particle.FLAME, player.getLocation().add(0, 0.2, 0), 2, 0.1, 0.05, 0.1, 0.01
                    );
                    case "OMBAK" -> player.getWorld().spawnParticle(
                        Particle.SPLASH, player.getLocation().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.05
                    );
                    case "RIMBA" -> player.getWorld().spawnParticle(
                        Particle.FALLING_SPORE_BLOSSOM, player.getLocation().add(0, 1.0, 0), 2, 0.3, 0.3, 0.3, 0.0
                    );
                }
            }
        }, 40L, 8L);
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getStorageManager().ensureConnection();
            if (conn == null) return;

            String sql = "SELECT active_title, active_trail, unlocked_titles, unlocked_trails FROM player_cosmetics WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PlayerCosmeticData data = new PlayerCosmeticData(
                            rs.getString("active_title"),
                            rs.getString("active_trail"),
                            new HashSet<>(Arrays.asList(rs.getString("unlocked_titles").split(","))),
                            new HashSet<>(Arrays.asList(rs.getString("unlocked_trails").split(",")))
                        );
                        cache.put(uuid, data);
                    } else {
                        // Profil kosmetik baru
                        PlayerCosmeticData data = new PlayerCosmeticData(
                            "PENGEMBARA", "NONE",
                            new HashSet<>(List.of("PENGEMBARA")),
                            new HashSet<>(List.of("NONE", "MELATI"))
                        );
                        cache.put(uuid, data);
                        savePlayer(uuid, data);
                    }
                }
            } catch (SQLException e) {
                log.warning("[CosmeticManager] Gagal memuat kosmetik pemain: " + e.getMessage());
            }
        });
    }

    public void unloadPlayer(UUID uuid) {
        PlayerCosmeticData data = cache.remove(uuid);
        if (data != null) {
            savePlayer(uuid, data);
        }
    }

    public PlayerCosmeticData getData(UUID uuid) {
        return cache.getOrDefault(uuid, new PlayerCosmeticData("PENGEMBARA", "NONE", new HashSet<>(List.of("PENGEMBARA")), new HashSet<>(List.of("NONE"))));
    }

    public void setActiveTitle(Player player, String titleKey) {
        PlayerCosmeticData data = cache.get(player.getUniqueId());
        if (data == null) return;

        data.activeTitle = titleKey.toUpperCase();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        player.sendMessage(Component.text("✔ Gelar aktifmu diubah menjadi: " + getTitleFormatted(titleKey), NamedTextColor.GREEN));
        savePlayer(player.getUniqueId(), data);
    }

    public void setActiveTrail(Player player, String trailKey) {
        PlayerCosmeticData data = cache.get(player.getUniqueId());
        if (data == null) return;

        data.activeTrail = trailKey.toUpperCase();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        player.sendMessage(Component.text("✔ Efek langkah aktif: §e" + trailKey, NamedTextColor.GREEN));
        savePlayer(player.getUniqueId(), data);
    }

    public void unlockTitle(Player player, String titleKey) {
        PlayerCosmeticData data = cache.get(player.getUniqueId());
        if (data != null && data.unlockedTitles.add(titleKey.toUpperCase())) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("✦ Gelar Baru Terbuka: " + getTitleFormatted(titleKey) + " §a✦", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Gunakan menu Kompas -> Progres -> Gelar untuk memasangnya.", NamedTextColor.GRAY));
            player.sendMessage(Component.empty());
            savePlayer(player.getUniqueId(), data);
        }
    }

    public String getTitleFormatted(String key) {
        return switch (key.toUpperCase()) {
            case "PENGEMBARA" -> "§7[§fPengembara Awam§7]";
            case "PEMBANGUN_KAMPUNG" -> "§e[§6Pembangun Kampung§e]";
            case "SAUDAGAR_NUSANTARA" -> "§a[§2Saudagar Nusantara§a]";
            case "PENAKLUK_KALA" -> "§c[§4Penakluk Kala§c]";
            case "PENJELAJAH_SAMUDRA" -> "§b[§3Penjelajah Samudra§b]";
            case "SAHABAT_SATWA" -> "§a[§2Sahabat Satwa§a]";
            default -> "§7[" + key + "]";
        };
    }

    private void savePlayer(UUID uuid, PlayerCosmeticData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getStorageManager().ensureConnection();
            if (conn == null) return;

            String sql = """
                INSERT INTO player_cosmetics (uuid, active_title, active_trail, unlocked_titles, unlocked_trails)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    active_title = excluded.active_title,
                    active_trail = excluded.active_trail,
                    unlocked_titles = excluded.unlocked_titles,
                    unlocked_trails = excluded.unlocked_trails
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, data.activeTitle);
                ps.setString(3, data.activeTrail);
                ps.setString(4, String.join(",", data.unlockedTitles));
                ps.setString(5, String.join(",", data.unlockedTrails));
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[CosmeticManager] Gagal menyimpan data kosmetik: " + e.getMessage());
            }
        });
    }

    public static class PlayerCosmeticData {
        public String activeTitle;
        public String activeTrail;
        public Set<String> unlockedTitles;
        public Set<String> unlockedTrails;

        public PlayerCosmeticData(String activeTitle, String activeTrail, Set<String> unlockedTitles, Set<String> unlockedTrails) {
            this.activeTitle = activeTitle;
            this.activeTrail = activeTrail;
            this.unlockedTitles = unlockedTitles;
            this.unlockedTrails = unlockedTrails;
        }
    }
}
