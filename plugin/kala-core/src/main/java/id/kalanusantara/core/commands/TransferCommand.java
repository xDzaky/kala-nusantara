package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.economy.RupiahEconomy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /transfer — Mengirim Rupiah Kala antar pemain (alias: /pay, /kirimuang)
 */
public class TransferCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public TransferCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Hanya pemain yang dapat mentransfer uang!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("§cPenggunaan: §e/" + label + " <nama_pemain> <jumlah>"));
            return true;
        }

        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null) {
            player.sendMessage(Component.text("§cPemain '" + args[0] + "' tidak sedang online!", NamedTextColor.RED));
            return true;
        }

        if (recipient.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("§cKamu tidak bisa mengirim uang ke diri sendiri!", NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("§cJumlah harus berupa angka valid!", NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("§cJumlah transfer harus lebih dari 0!", NamedTextColor.RED));
            return true;
        }

        RupiahEconomy.TransferResult result = plugin.getEconomy().transfer(player.getUniqueId(), recipient.getUniqueId(), amount);
        if (result == RupiahEconomy.TransferResult.SUCCESS) {
            String formatted = plugin.getEconomy().format(amount);
            player.sendMessage(Component.text("§aBerhasil mentransfer §e" + formatted + " §ake §b" + recipient.getName() + "§a."));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);

            recipient.sendMessage(Component.text("§aKamu menerima transfer §e" + formatted + " §adari §b" + player.getName() + "§a!"));
            recipient.playSound(recipient.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        } else if (result == RupiahEconomy.TransferResult.INSUFFICIENT_FUNDS) {
            player.sendMessage(Component.text("§cSaldo Rupiah kamu tidak mencukupi untuk transfer ini!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
        } else {
            player.sendMessage(Component.text("§cGagal memproses transfer.", NamedTextColor.RED));
        }

        return true;
    }
}
