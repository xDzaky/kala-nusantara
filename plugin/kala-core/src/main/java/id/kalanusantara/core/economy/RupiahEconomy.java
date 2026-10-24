package id.kalanusantara.core.economy;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * RupiahEconomy — sistem ekonomi internal Kala Nusantara
 * 
 * Mata uang: Rupiah Kala (Rp)
 * Terintegrasi dengan VaultUnlocked via listener terpisah.
 */
public class RupiahEconomy {

    private final KalaCorePlugin plugin;
    private final Logger log;

    public RupiahEconomy(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        log.info("Ekonomi Rupiah Kala aktif — mata uang: " +
            plugin.getKalaConfig().getCurrencyName());
    }

    /**
     * Dapatkan saldo pemain
     */
    public double getBalance(UUID uuid) {
        return plugin.getIdentityManager().get(uuid)
            .map(PlayerProfile::getBalance)
            .orElseGet(() -> queryBalanceFromDb(uuid));
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    /**
     * Tambah saldo
     */
    public boolean deposit(UUID uuid, double amount, String description) {
        if (amount <= 0) return false;

        plugin.getIdentityManager().get(uuid).ifPresent(p -> p.addBalance(amount));
        logTransaction(uuid, "DEPOSIT", amount, description);
        return true;
    }

    /**
     * Kurangi saldo
     */
    public boolean withdraw(UUID uuid, double amount, String description) {
        if (amount <= 0) return false;

        var profileOpt = plugin.getIdentityManager().get(uuid);
        if (profileOpt.isEmpty()) return false;

        PlayerProfile profile = profileOpt.get();
        if (!profile.hasBalance(amount)) return false;

        profile.deductBalance(amount);
        logTransaction(uuid, "WITHDRAW", amount, description);
        return true;
    }

    /**
     * Transfer antar pemain
     */
    public TransferResult transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return TransferResult.INVALID_AMOUNT;
        if (!hasBalance(from, amount)) return TransferResult.INSUFFICIENT_FUNDS;

        withdraw(from, amount, "Transfer ke " + getPlayerName(to));
        deposit(to, amount, "Transfer dari " + getPlayerName(from));

        return TransferResult.SUCCESS;
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
        try {
            Connection conn = plugin.getStorageManager().ensureConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT balance FROM player_profile WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            double bal = rs.next() ? rs.getDouble("balance") : 0.0;
            rs.close(); ps.close();
            return bal;
        } catch (SQLException e) {
            log.severe("Gagal query saldo: " + e.getMessage());
            return 0.0;
        }
    }

    private void logTransaction(UUID uuid, String type, double amount, String desc) {
        try {
            Connection conn = plugin.getStorageManager().ensureConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO economy_transactions (uuid, type, amount, description, timestamp) VALUES (?,?,?,?,?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, desc);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            log.warning("Gagal log transaksi: " + e.getMessage());
        }
    }

    private String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString().substring(0, 8);
    }

    public enum TransferResult {
        SUCCESS, INSUFFICIENT_FUNDS, INVALID_AMOUNT, PLAYER_NOT_FOUND
    }
}
