package id.kalanusantara.core.economy;

import id.kalanusantara.core.KalaCorePlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * KalaVaultEconomyProvider — Implementasi Vault Economy resmi untuk Kala Nusantara.
 * 
 * Menghilangkan dependensi terhadap plugin ekonomi eksternal seperti ThunderEconomy.
 * Seluruh saldo pemain terhubung langsung ke SQLite database kala_core.db.
 */
public class KalaVaultEconomyProvider implements Economy {

    private final KalaCorePlugin plugin;
    private final RupiahEconomy economy;

    public KalaVaultEconomyProvider(KalaCorePlugin plugin, RupiahEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "KalaRupiah";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getKalaConfig().getCurrencyName();
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getKalaConfig().getCurrencyName();
    }

    @Override
    public boolean hasAccount(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return op != null;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return op != null ? getBalance(op) : 0.0;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null) return 0.0;
        return economy.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        if (op == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Pemain tidak ditemukan");
        }
        return withdrawPlayer(op, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Pemain tidak ditemukan");
        }
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Jumlah tidak valid");
        }

        UUID uuid = player.getUniqueId();
        double current = economy.getBalance(uuid);
        if (current < amount) {
            return new EconomyResponse(0, current, EconomyResponse.ResponseType.FAILURE, "Saldo Rupiah tidak mencukupi");
        }

        boolean ok = economy.withdraw(uuid, amount, "Vault withdraw");
        double newBal = economy.getBalance(uuid);
        return ok 
            ? new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null)
            : new EconomyResponse(0, current, EconomyResponse.ResponseType.FAILURE, "Gagal memproses penarikan");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        if (op == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Pemain tidak ditemukan");
        }
        return depositPlayer(op, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Pemain tidak ditemukan");
        }
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Jumlah tidak valid");
        }

        UUID uuid = player.getUniqueId();
        boolean ok = economy.deposit(uuid, amount, "Vault deposit");
        double newBal = economy.getBalance(uuid);
        return ok
            ? new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null)
            : new EconomyResponse(0, newBal, EconomyResponse.ResponseType.FAILURE, "Gagal memproses setoran");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    // --- Bank Operations (Unsupported by Design in Nusantara Simple Economy) ---

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Sistem bank tidak didukung");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }
}
