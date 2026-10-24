package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /dompet — Melihat saldo Rupiah Kala Nusantara (alias: /uang, /balance, /bal)
 */
public class DompetCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public DompetCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                double bal = plugin.getEconomy().getBalance(target.getUniqueId());
                sender.sendMessage("Saldo " + target.getName() + ": " + plugin.getEconomy().format(bal));
            } else {
                sender.sendMessage("Penggunaan konsol: /dompet <pemain>");
            }
            return true;
        }

        if (args.length > 0 && player.hasPermission("kala.admin")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            double bal = plugin.getEconomy().getBalance(target.getUniqueId());
            player.sendMessage(Component.text("─── Dompet Pemain ───", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Pemilik: ", NamedTextColor.GRAY).append(Component.text(target.getName() != null ? target.getName() : args[0], NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Saldo: ", NamedTextColor.GRAY).append(Component.text(plugin.getEconomy().format(bal), NamedTextColor.GREEN)));
            return true;
        }

        double balance = plugin.getEconomy().getBalance(player.getUniqueId());
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§6§l╔═══════════════════════════════════════╗"));
        player.sendMessage(Component.text("§6§l║      §e§lDOMPET KALA NUSANTARA          §6§l║"));
        player.sendMessage(Component.text("§6§l╠═══════════════════════════════════════╣"));
        player.sendMessage(Component.text("§6§l║ §7Pemilik : §f" + player.getName()));
        player.sendMessage(Component.text("§6§l║ §7Saldo   : §a§l" + plugin.getEconomy().format(balance)));
        player.sendMessage(Component.text("§6§l║ §7Kirim   : §e/transfer <pemain> <jumlah>"));
        player.sendMessage(Component.text("§6§l╚═══════════════════════════════════════╝"));
        player.sendMessage(Component.empty());
        return true;
    }
}
