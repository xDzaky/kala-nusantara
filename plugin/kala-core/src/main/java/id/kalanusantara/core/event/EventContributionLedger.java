package id.kalanusantara.core.event;

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
 * EventContributionLedger — Buku Besar Partisipasi Event & Pencegahan Duplikasi Reward (Kala Nusantara v6.0).
 *
 * Menerapkan prinsip:
 * 1. Idempotency: Satu pemain hanya dapat mengklaim reward event satu kali per event_id.
 * 2. Multi-Objective: Poin dapat diperoleh dari logistik, observasi, penyelamatan, maupun pemulihan.
 * 3. Anti-Exploit / Anti-Inflasi: Diminishing returns setelah batas kontribusi tertentu.
 */
public class EventContributionLedger {

    private final KalaCorePlugin plugin;
    private final Logger log;

    // Cache: eventId -> Map<UUID, Integer> totalPoints
    private final Map<String, Map<UUID, Integer>> pointsCache = new ConcurrentHashMap<>();
    // Cache: eventId -> Set<UUID> claimedPlayers
    private final Map<String, Set<UUID>> claimedCache = new ConcurrentHashMap<>();

    public EventContributionLedger(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    /**
     * Catat kontribusi pemain pada event tertentu
     */
    public void recordContribution(String eventId, Player player, String objectiveId, int points) {
        if (player == null || eventId == null) return;
        UUID uuid = player.getUniqueId();

        // 1. Hitung dengan aturan diminishing returns
        Map<UUID, Integer> eventPoints = pointsCache.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        int current = eventPoints.getOrDefault(uuid, 0);

        int effectivePoints = points;
        if (current > 80) {
            effectivePoints = Math.max(1, points / 2); // Diminishing returns jika > 80 poin
        }
        eventPoints.put(uuid, current + effectivePoints);

        // 2. Simpan ke SQLite secara asynchronous
        int finalPoints = effectivePoints;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO event_contribution_ledger (event_id, uuid, player_name, objective_id, points, reward_claimed, timestamp)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                ON CONFLICT(event_id, uuid, objective_id) DO UPDATE SET
                    points = points + excluded.points,
                    timestamp = excluded.timestamp
            """;
            try (Connection conn = plugin.getStorageManager().ensureConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, eventId);
                ps.setString(2, uuid.toString());
                ps.setString(3, player.getName());
                ps.setString(4, objectiveId);
                ps.setInt(5, finalPoints);
                ps.setLong(6, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.severe("[EventLedger] Gagal mencatat kontribusi: " + e.getMessage());
            }
        });
    }

    public int getPlayerPoints(String eventId, UUID uuid) {
        Map<UUID, Integer> eventPoints = pointsCache.get(eventId);
        return eventPoints != null ? eventPoints.getOrDefault(uuid, 0) : 0;
    }

    public boolean hasClaimed(String eventId, UUID uuid) {
        Set<UUID> claimed = claimedCache.get(eventId);
        return claimed != null && claimed.contains(uuid);
    }

    /**
     * Klaim reward event komunitas secara idempotent (maksimal 1x per event_id)
     */
    public boolean claimReward(String eventId, Player player, double baseRupiah, int baseExp) {
        if (player == null || eventId == null) return false;
        UUID uuid = player.getUniqueId();

        Set<UUID> claimed = claimedCache.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
        if (claimed.contains(uuid)) {
            player.sendMessage(Component.text("§c[!] Anda sudah mengklaim hadiah dari event ini!", NamedTextColor.RED));
            return false;
        }

        int points = getPlayerPoints(eventId, uuid);
        if (points <= 0) {
            player.sendMessage(Component.text("§c[!] Anda belum berkontribusi dalam event ini untuk menerima imbalan.", NamedTextColor.RED));
            return false;
        }

        // Tandai klaim di memori
        claimed.add(uuid);

        // Skala reward berdasarkan kontribusi (maksimal multiplier 2.5x)
        double multiplier = Math.min(2.5, 1.0 + (points / 50.0));
        double totalRupiah = baseRupiah * multiplier;
        int totalExp = (int) (baseExp * multiplier);

        plugin.getEconomy().deposit(uuid, totalRupiah, "Hadiah event " + eventId);
        player.giveExp(totalExp);

        // Update database async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE event_contribution_ledger SET reward_claimed = 1 WHERE event_id = ? AND uuid = ?";
            try (Connection conn = plugin.getStorageManager().ensureConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, eventId);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.severe("[EventLedger] Gagal menandai klaim: " + e.getMessage());
            }
        });

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ IMBALAN GOTONG ROYONG DITERIMA ✦", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Kontribusi: §e" + points + " Poin §7| Pengali: §a" + String.format("%.1fx", multiplier), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Hadiah: §6+Rp " + String.format("%.0f", totalRupiah) + " Rupiah Kala §7dan §b+" + totalExp + " EXP", NamedTextColor.YELLOW));
        player.sendMessage(Component.empty());

        return true;
    }
}
