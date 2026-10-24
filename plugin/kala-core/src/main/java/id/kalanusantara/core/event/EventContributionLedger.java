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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * EventContributionLedger — Buku besar pencatatan kontribusi dan imbalan gotong royong.
 *
 * Audit V8:
 * - Outbox Ledger Pattern: State machine (PENDING -> PAID / FAILED_RETRYABLE) dengan claim_id unik.
 * - Database sebagai Single Source of Truth (SSOT): tidak mengandalkan cache sebelum commit.
 * - Rekonsiliasi otomatis saat startup untuk transaksi PENDING pasca-crash.
 * - Pencegahan double payout: query eksplisit SQLite sebelum pemrosesan reward.
 */
public class EventContributionLedger {

    public enum ClaimStatus {
        PENDING,
        PAID,
        FAILED_RETRYABLE
    }

    private final KalaCorePlugin plugin;
    private final Logger log;

    // Cache: eventId -> Map<UUID, Integer> totalPoints
    private final Map<String, Map<UUID, Integer>> pointsCache = new ConcurrentHashMap<>();
    // Cache: eventId -> Set<UUID> claimedPlayers (hanya untuk status PAID)
    private final Map<String, Set<UUID>> claimedCache = new ConcurrentHashMap<>();

    public EventContributionLedger(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        loadAndReconcileFromDatabase();
    }

    /**
     * Audit V8: Memuat riwayat dan merekonsiliasi record klaim PENDING dari sesi sebelum shutdown/crash.
     */
    public void loadAndReconcileFromDatabase() {
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

                // 2. Muat klaim PAID dan rekonsiliasi PENDING
                String sqlClaims = "SELECT claim_id, event_id, uuid, player_name, amount, exp, status FROM event_claims";
                try (Connection conn = plugin.getStorageManager().ensureConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlClaims);
                     ResultSet rs = ps.executeQuery()) {
                    int paidCount = 0;
                    int pendingCount = 0;

                    while (rs.next()) {
                        String eventId = rs.getString("event_id");
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String statusStr = rs.getString("status");
                        double amount = rs.getDouble("amount");
                        int exp = rs.getInt("exp");
                        String claimId = rs.getString("claim_id");

                        if ("PAID".equalsIgnoreCase(statusStr)) {
                            claimedCache.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                            paidCount++;
                        } else if ("PENDING".equalsIgnoreCase(statusStr)) {
                            pendingCount++;
                            // Rekonsiliasi klaim yang belum dibayar sebelum crash
                            reconcilePendingClaim(claimId, eventId, uuid, amount, exp);
                        }
                    }

                    if (paidCount > 0 || pendingCount > 0) {
                        log.info("[EventLedger] Rekonstruksi klaim: " + paidCount + " PAID, " + pendingCount + " direkonsiliasi.");
                    }
                } catch (SQLException e) {
                    log.warning("[EventLedger] Gagal memuat/rekonsiliasi klaim: " + e.getMessage());
                }
            }
        });
    }

    private void reconcilePendingClaim(String claimId, String eventId, UUID uuid, double amount, int exp) {
        log.info("[EventLedger] Menuntaskan payout PENDING untuk claim: " + claimId + " (" + uuid + ")");
        plugin.getEconomy().deposit(uuid, amount, "Rekonsiliasi hadiah event " + eventId);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.giveExp(exp);
            } else if (exp > 0) {
                plugin.getStorageManager().recordRewardEntitlement(uuid, "EVENT_EXP", (claimId != null ? claimId : (eventId + "_" + uuid)), 0.0, exp);
            }
        });

        // Update status menjadi PAID
        String updateSql = "UPDATE event_claims SET status = 'PAID', paid_at = ? WHERE claim_id = ? OR (event_id = ? AND uuid = ?)";
        try (Connection conn = plugin.getStorageManager().ensureConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, claimId != null ? claimId : (eventId + "_" + uuid));
            ps.setString(3, eventId);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();
            claimedCache.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(uuid);
        } catch (SQLException e) {
            log.severe("[EventLedger] Gagal update rekonsiliasi status ke PAID: " + e.getMessage());
        }
    }

    /**
     * Catat kontribusi pemain pada event tertentu
     */
    public void recordContribution(String eventId, Player player, String objectiveId, int points) {
        if (player == null || eventId == null) return;
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        Map<UUID, Integer> eventPoints = pointsCache.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        int current = eventPoints.getOrDefault(uuid, 0);

        int effectivePoints = points;
        if (current > 80) {
            effectivePoints = Math.max(1, points / 2); // Diminishing returns jika > 80 poin
        }
        eventPoints.put(uuid, current + effectivePoints);

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
     * Klaim reward event gotong royong dengan Outbox Ledger Pattern (Audit V8 P0 Fix).
     */
    public boolean claimReward(String eventId, Player player, double baseRupiah, int baseExp) {
        if (player == null || eventId == null) return false;
        final UUID uuid = player.getUniqueId();
        final String playerName = player.getName();

        // 1. Cek cache in-memory terlebih dahulu untuk fast-reject
        if (hasClaimed(eventId, uuid)) {
            player.sendMessage(Component.text("§c[!] Anda sudah pernah mengklaim hadiah dari event ini!", NamedTextColor.RED));
            return false;
        }

        int points = getPlayerPoints(eventId, uuid);
        if (points <= 0) {
            player.sendMessage(Component.text("§c[!] Anda belum berkontribusi dalam event ini untuk menerima imbalan.", NamedTextColor.RED));
            return false;
        }

        // Skala reward berdasarkan kontribusi (maksimal multiplier 2.5x)
        double multiplier = Math.min(2.5, 1.0 + (points / 50.0));
        final double totalRupiah = Math.round((baseRupiah * multiplier) * 100.0) / 100.0;
        final int totalExp = (int) (baseExp * multiplier);
        final String claimId = eventId + "_" + uuid;

        // 2. Eksekusi asynchronous via SSOT Database Outbox
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean shouldPayout = false;

            synchronized (plugin.getStorageManager().getDbLock()) {
                try (Connection conn = plugin.getStorageManager().ensureConnection()) {
                    // Cek apakah sudah pernah ada klaim di database
                    String checkSql = "SELECT status FROM event_claims WHERE event_id = ? AND uuid = ?";
                    try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                        psCheck.setString(1, eventId);
                        psCheck.setString(2, uuid.toString());
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next()) {
                                String status = rs.getString("status");
                                if ("PAID".equalsIgnoreCase(status)) {
                                    claimedCache.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                                    Bukkit.getScheduler().runTask(plugin, () ->
                                        player.sendMessage(Component.text("§c[!] Hadiah event ini sudah berhasil Anda klaim sebelumnya.", NamedTextColor.RED))
                                    );
                                    return;
                                }
                                // Jika PENDING, jangan insert lagi, langsung lanjutkan payout
                                shouldPayout = true;
                            }
                        }
                    }

                    if (!shouldPayout) {
                        // Insert record PENDING
                        String insertSql = """
                            INSERT INTO event_claims (claim_id, event_id, uuid, player_name, score, amount, exp, status, timestamp, paid_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, NULL)
                        """;
                        try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                            psInsert.setString(1, claimId);
                            psInsert.setString(2, eventId);
                            psInsert.setString(3, uuid.toString());
                            psInsert.setString(4, playerName);
                            psInsert.setInt(5, points);
                            psInsert.setDouble(6, totalRupiah);
                            psInsert.setInt(7, totalExp);
                            psInsert.setLong(8, System.currentTimeMillis());
                            psInsert.executeUpdate();
                            shouldPayout = true;
                        }
                    }
                } catch (SQLException e) {
                    log.severe("[EventLedger] Gagal mencatat outbox claim: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(Component.text("§c[!] Gagal memproses klaim reward (Database error). Silakan coba lagi.", NamedTextColor.RED))
                    );
                    return;
                }
            }

            if (shouldPayout) {
                // Eksekusi pembayaran ke pemain
                boolean depositSuccess = plugin.getEconomy().deposit(uuid, totalRupiah, "Hadiah event " + eventId);

                synchronized (plugin.getStorageManager().getDbLock()) {
                    try (Connection conn = plugin.getStorageManager().ensureConnection()) {
                        if (depositSuccess) {
                            String markPaidSql = "UPDATE event_claims SET status = 'PAID', paid_at = ? WHERE claim_id = ? OR (event_id = ? AND uuid = ?)";
                            try (PreparedStatement psPaid = conn.prepareStatement(markPaidSql)) {
                                psPaid.setLong(1, System.currentTimeMillis());
                                psPaid.setString(2, claimId);
                                psPaid.setString(3, eventId);
                                psPaid.setString(4, uuid.toString());
                                psPaid.executeUpdate();
                            }
                            // Update status di ledger utama
                            String updateLedgerSql = "UPDATE event_contribution_ledger SET reward_claimed = 1 WHERE event_id = ? AND uuid = ?";
                            try (PreparedStatement psLedger = conn.prepareStatement(updateLedgerSql)) {
                                psLedger.setString(1, eventId);
                                psLedger.setString(2, uuid.toString());
                                psLedger.executeUpdate();
                            }
                            // Berhasil penuh: update in-memory cache
                            claimedCache.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(uuid);

                            // Notifikasi ke pemain di main thread
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (player.isOnline()) {
                                    player.giveExp(totalExp);
                                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                                    player.sendMessage(Component.empty());
                                    player.sendMessage(Component.text("✦ IMBALAN GOTONG ROYONG DITERIMA ✦", NamedTextColor.GOLD));
                                    player.sendMessage(Component.text("Kontribusi: §e" + points + " Poin §7| Pengali: §a" + String.format("%.1fx", multiplier), NamedTextColor.GRAY));
                                    player.sendMessage(Component.text("Hadiah: §6+Rp " + String.format("%.0f", totalRupiah) + " Rupiah Kala §7dan §b+" + totalExp + " EXP", NamedTextColor.YELLOW));
                                    player.sendMessage(Component.empty());
                                } else if (totalExp > 0) {
                                    plugin.getStorageManager().recordRewardEntitlement(uuid, "EVENT_EXP", claimId, 0.0, totalExp);
                                }
                            });
                        } else {
                            // Tandai sebagai FAILED_RETRYABLE
                            String markFailedSql = "UPDATE event_claims SET status = 'FAILED_RETRYABLE' WHERE claim_id = ?";
                            try (PreparedStatement psFailed = conn.prepareStatement(markFailedSql)) {
                                psFailed.setString(1, claimId);
                                psFailed.executeUpdate();
                            }
                            Bukkit.getScheduler().runTask(plugin, () ->
                                player.sendMessage(Component.text("§c[!] Terjadi kendala saat penyaluran saldo. Anda dapat mencoba kembali.", NamedTextColor.RED))
                            );
                        }
                    } catch (SQLException e) {
                        log.severe("[EventLedger] Gagal memperbarui status claim payout: " + e.getMessage());
                    }
                }
            }
        });

        return true;
    }
}
