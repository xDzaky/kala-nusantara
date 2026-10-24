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
 * EventContributionLedger — Buku Besar Partisipasi Event & Pencegahan Duplikasi Reward (Kala Nusantara v7.0).
 *
 * Audit V7:
 * 1. P1 Fix: Tabel persisten `event_claims` dengan compound UNIQUE(event_id, uuid) menjamin durable single-claim.
 * 2. P1 Fix: Rebuild score cache dari SQLite saat inisialisasi agar data bertahan lintas restart.
 * 3. P1 Fix: Thread-safe data capture (player name & uuid ditangkap di main thread sebelum async).
 * 4. P1 Fix: Sinkronisasi database lock untuk mencegah SQLITE_BUSY.
 * 5. Anti-Exploit / Anti-Inflasi: Kurva diminishing returns (>80 poin) dan scaling reward terukur.
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
        loadFromDatabase();
    }

    /**
     * P1 Fix: Memuat ulang cache kontribusi dan klaim yang sudah ada di database saat startup.
     */
    public void loadFromDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (plugin.getStorageManager().getDbLock()) {
                // 1. Muat riwayat poin
                String sqlPoints = "SELECT event_id, uuid, SUM(points) as total FROM event_contribution_ledger GROUP BY event_id, uuid";
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlPoints);
                     ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        String eventId = rs.getString("event_id");
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        int total = rs.getInt("total");
                        pointsCache.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>()).put(uuid, total);
                        count++;
                    }
                    if (count > 0) {
                        log.info("[EventLedger] Rekonstruksi " + count + " riwayat poin dari database.");
                    }
                } catch (SQLException e) {
                    log.warning("[EventLedger] Gagal memuat points cache: " + e.getMessage());
                }

                // 2. Muat riwayat klaim
                String sqlClaims = "SELECT event_id, uuid FROM event_claims";
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlClaims);
                     ResultSet rs = ps.executeQuery()) {
                    int claimCount = 0;
                    while (rs.next()) {
                        String eventId = rs.getString("event_id");
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        claimedCache.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                        claimCount++;
                    }
                    if (claimCount > 0) {
                        log.info("[EventLedger] Rekonstruksi " + claimCount + " riwayat klaim persisten.");
                    }
                } catch (SQLException e) {
                    log.warning("[EventLedger] Gagal memuat claims cache: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Catat kontribusi pemain pada event tertentu
     */
    public void recordContribution(String eventId, Player player, String objectiveId, int points) {
        if (player == null || eventId == null) return;
        // P1 Fix: Tangkap data player di main thread
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        // 1. Hitung dengan aturan diminishing returns
        Map<UUID, Integer> eventPoints = pointsCache.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        int current = eventPoints.getOrDefault(uuid, 0);

        int effectivePoints = points;
        if (current > 80) {
            effectivePoints = Math.max(1, points / 2); // Diminishing returns jika > 80 poin
        }
        eventPoints.put(uuid, current + effectivePoints);

        // 2. Simpan ke SQLite secara asynchronous dengan dbLock
        final int finalPoints = effectivePoints;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO event_contribution_ledger (event_id, uuid, player_name, objective_id, points, reward_claimed, timestamp)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                ON CONFLICT(event_id, uuid, objective_id) DO UPDATE SET
                    points = points + excluded.points,
                    timestamp = excluded.timestamp
            """;
            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, eventId);
                    ps.setString(2, uuid.toString());
                    ps.setString(3, playerName);
                    ps.setString(4, objectiveId);
                    ps.setInt(5, finalPoints);
                    ps.setLong(6, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    log.severe("[EventLedger] Gagal mencatat kontribusi: " + e.getMessage());
                }
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
     * Klaim reward event komunitas secara durable & idempotent (P1 Fix).
     * Memeriksa dan mencatat ke tabel `event_claims` sebelum mendistribusikan uang & EXP.
     */
    public boolean claimReward(String eventId, Player player, double baseRupiah, int baseExp) {
        if (player == null || eventId == null) return false;
        UUID uuid = player.getUniqueId();

        // 1. Cek cache in-memory terlebih dahulu
        Set<UUID> claimed = claimedCache.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
        if (claimed.contains(uuid)) {
            player.sendMessage(Component.text("§c[!] Anda sudah pernah mengklaim hadiah dari event ini!", NamedTextColor.RED));
            return false;
        }

        int points = getPlayerPoints(eventId, uuid);
        if (points <= 0) {
            player.sendMessage(Component.text("§c[!] Anda belum berkontribusi dalam event ini untuk menerima imbalan.", NamedTextColor.RED));
            return false;
        }

        // Tandai di cache
        claimed.add(uuid);

        // Skala reward berdasarkan kontribusi (maksimal multiplier 2.5x)
        double multiplier = Math.min(2.5, 1.0 + (points / 50.0));
        double totalRupiah = baseRupiah * multiplier;
        int totalExp = (int) (baseExp * multiplier);

        // 2. Simpan klaim secara durable ke tabel SQLite event_claims
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String insertClaimSql = "INSERT INTO event_claims (event_id, uuid, amount, exp, timestamp) VALUES (?, ?, ?, ?, ?)";
            String updateLedgerSql = "UPDATE event_contribution_ledger SET reward_claimed = 1 WHERE event_id = ? AND uuid = ?";

            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement psClaim = conn.prepareStatement(insertClaimSql);
                         PreparedStatement psUpdate = conn.prepareStatement(updateLedgerSql)) {
                        psClaim.setString(1, eventId);
                        psClaim.setString(2, uuid.toString());
                        psClaim.setDouble(3, totalRupiah);
                        psClaim.setInt(4, totalExp);
                        psClaim.setLong(5, System.currentTimeMillis());
                        psClaim.executeUpdate();

                        psUpdate.setString(1, eventId);
                        psUpdate.setString(2, uuid.toString());
                        psUpdate.executeUpdate();

                        conn.commit();
                    } catch (SQLException e) {
                        conn.rollback();
                        log.severe("[EventLedger] Rollback klaim gagal: " + e.getMessage());
                        return;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    log.severe("[EventLedger] Gagal memproses transaksi klaim: " + e.getMessage());
                    return;
                }
            }

            // Payout diantarkan kembali ke main thread jika transaksi database berhasil
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getEconomy().deposit(uuid, totalRupiah, "Hadiah event " + eventId);
                if (player.isOnline()) {
                    player.giveExp(totalExp);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("✦ IMBALAN GOTONG ROYONG DITERIMA ✦", NamedTextColor.GOLD));
                    player.sendMessage(Component.text("Kontribusi: §e" + points + " Poin §7| Pengali: §a" + String.format("%.1fx", multiplier), NamedTextColor.GRAY));
                    player.sendMessage(Component.text("Hadiah: §6+Rp " + String.format("%.0f", totalRupiah) + " Rupiah Kala §7dan §b+" + totalExp + " EXP", NamedTextColor.YELLOW));
                    player.sendMessage(Component.empty());
                }
            });
        });

        return true;
    }
}
