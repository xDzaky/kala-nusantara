package id.kalanusantara.core.economy;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * RupiahEconomy — Implementasi sistem ekonomi berbasis mata uang Rupiah Kala.
 *
 * Audit V8:
 * - Benar-benar transaksional & atomik untuk transfer antar pemain (baik online maupun offline).
 * - Seluruh akses baca/tulis database diserialisasi secara konsisten via dbLock.
 * - Validasi ketat nominal: round ke 2 desimal, anti-NaN, anti-Infinity, anti-negatif, anti-self transfer.
 * - Tidak mengembalikan SUCCESS sebelum commit database berhasil.
 */
public class RupiahEconomy {

    public enum TransferResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        INVALID_AMOUNT,
        SAME_PLAYER,
        DATABASE_ERROR
    }

    private final KalaCorePlugin plugin;
    private final Logger log;

    public RupiahEconomy(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        log.info("[RupiahEconomy] Sistem ekonomi Rupiah Kala siap (Mata uang: " +
            plugin.getKalaConfig().getCurrencyName() + ")");
    }

    /**
     * Dapatkan saldo pemain
     */
    public double getBalance(UUID uuid) {
        if (uuid == null) return 0.0;
        Optional<PlayerProfile> profileOpt = plugin.getIdentityManager().get(uuid);
        if (profileOpt.isPresent()) {
            return profileOpt.get().getBalance();
        }
        return queryBalanceFromDb(uuid);
    }

    public double getBalance(Player player) {
        return player != null ? getBalance(player.getUniqueId()) : 0.0;
    }

    /**
     * Tambah saldo secara sinkron dan aman
     */
    public synchronized boolean deposit(UUID uuid, double amount, String description) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) return false;
        double rounded = Math.round(amount * 100.0) / 100.0;

        synchronized (plugin.getStorageManager().getDbLock()) {
            var profileOpt = plugin.getIdentityManager().get(uuid);
            if (profileOpt.isPresent()) {
                profileOpt.get().addBalance(rounded);
            } else {
                if (!executeOfflineBalanceUpdate(uuid, rounded)) {
                    return false;
                }
            }
            logTransaction(uuid, "DEPOSIT", rounded, description);
        }
        return true;
    }

    /**
     * Kurangi saldo secara sinkron dan aman
     */
    public synchronized boolean withdraw(UUID uuid, double amount, String description) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) return false;
        double rounded = Math.round(amount * 100.0) / 100.0;

        synchronized (plugin.getStorageManager().getDbLock()) {
            var profileOpt = plugin.getIdentityManager().get(uuid);
            if (profileOpt.isPresent()) {
                PlayerProfile profile = profileOpt.get();
                if (!profile.hasBalance(rounded)) return false;
                profile.deductBalance(rounded);
            } else {
                double cur = queryBalanceFromDb(uuid);
                if (cur < rounded) return false;
                if (!executeOfflineBalanceUpdate(uuid, -rounded)) {
                    return false;
                }
            }
            logTransaction(uuid, "WITHDRAW", rounded, description);
        }
        return true;
    }

    /**
     * Transfer antar pemain secara atomik dalam satu transaksi SQLite (Audit V8 P0 Fix).
     * Menjamin saldo sender dan receiver ter-update bersamaan atau di-rollback bersamaan.
     */
    public synchronized TransferResult transfer(UUID from, UUID to, double amount) {
        if (from == null || to == null) return TransferResult.INVALID_AMOUNT;
        if (from.equals(to)) return TransferResult.SAME_PLAYER;
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) return TransferResult.INVALID_AMOUNT;

        final double rounded = Math.round(amount * 100.0) / 100.0;

        synchronized (plugin.getStorageManager().getDbLock()) {
            // 1. Cek saldo pengirim
            double senderBalance = getBalance(from);
            if (senderBalance < rounded) {
                return TransferResult.INSUFFICIENT_FUNDS;
            }

            // 2. Eksekusi transaksi atomik SQLite
            try (Connection conn = plugin.getStorageManager().ensureConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Update balance pengirim di DB
                    String sqlFrom = "UPDATE player_profile SET balance = MAX(0.0, balance - ?) WHERE uuid = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlFrom)) {
                        ps.setDouble(1, rounded);
                        ps.setString(2, from.toString());
                        ps.executeUpdate();
                    }

                    // Update balance penerima di DB
                    String sqlTo = "UPDATE player_profile SET balance = balance + ? WHERE uuid = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlTo)) {
                        ps.setDouble(1, rounded);
                        ps.setString(2, to.toString());
                        ps.executeUpdate();
                    }

                    // Catat log transaksi pengirim
                    String sqlLog = "INSERT INTO economy_transactions (uuid, type, amount, description, timestamp) VALUES (?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlLog)) {
                        ps.setString(1, from.toString());
                        ps.setString(2, "TRANSFER_OUT");
                        ps.setDouble(3, rounded);
                        ps.setString(4, "Transfer ke " + getPlayerName(to));
                        ps.setLong(5, System.currentTimeMillis());
                        ps.executeUpdate();

                        ps.setString(1, to.toString());
                        ps.setString(2, "TRANSFER_IN");
                        ps.setDouble(3, rounded);
                        ps.setString(4, "Transfer dari " + getPlayerName(from));
                        ps.setLong(5, System.currentTimeMillis());
                        ps.executeUpdate();
                    }

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    log.severe("[RupiahEconomy] Rollback transaksi transfer atomik: " + e.getMessage());
                    return TransferResult.DATABASE_ERROR;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.severe("[RupiahEconomy] Gagal membuka koneksi transfer: " + e.getMessage());
                return TransferResult.DATABASE_ERROR;
            }

            // 3. Update memory state jika profil sedang aktif di server
            plugin.getIdentityManager().get(from).ifPresent(p -> p.deductBalance(rounded));
            plugin.getIdentityManager().get(to).ifPresent(p -> p.addBalance(rounded));

            return TransferResult.SUCCESS;
        }
    }

    private boolean executeOfflineBalanceUpdate(UUID uuid, double delta) {
        String sql = "UPDATE player_profile SET balance = MAX(0.0, balance + ?) WHERE uuid = ?";
        try (Connection conn = plugin.getStorageManager().ensureConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, delta);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.severe("[Economy] Gagal update saldo offline: " + e.getMessage());
            return false;
        }
    }

    public boolean hasBalance(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public String format(double amount) {
        return plugin.getKalaConfig().getCurrencySymbol() + " " +
            String.format("%,.0f", amount);
    }

    // === Internal helpers ===

    private double queryBalanceFromDb(UUID uuid) {
        synchronized (plugin.getStorageManager().getDbLock()) {
            try (Connection conn = plugin.getStorageManager().ensureConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT balance FROM player_profile WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getDouble("balance") : 0.0;
                }
            } catch (SQLException e) {
                log.severe("Gagal query saldo: " + e.getMessage());
                return 0.0;
            }
        }
    }

    private void logTransaction(UUID uuid, String type, double amount, String desc) {
        try (Connection conn = plugin.getStorageManager().ensureConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO economy_transactions (uuid, type, amount, description, timestamp) VALUES (?,?,?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, desc);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("Gagal log transaksi: " + e.getMessage());
        }
    }

    private String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        return plugin.getIdentityManager().get(uuid)
            .map(PlayerProfile::getName)
            .orElse(uuid.toString().substring(0, 8));
    }
}
