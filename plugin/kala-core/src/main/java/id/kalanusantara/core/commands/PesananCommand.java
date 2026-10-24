package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * PesananCommand — /pesanan [lihat|serahkan <nomor>]
 */
public class PesananCommand implements CommandExecutor, TabCompleter {

    private final KalaCorePlugin plugin;

    public PesananCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini hanya untuk pemain.");
            return true;
        }

        if (args.length == 0) {
            plugin.getMenuManager().openPesananMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("lihat") || args[0].equalsIgnoreCase("chat")) {
            plugin.getPesananManager().showOrderMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("serahkan")) {
            if (args.length < 2) {
                player.sendMessage(Component.text(
                    "§cPenggunaan: /pesanan serahkan <nomor>", NamedTextColor.RED).asComponent());
                return true;
            }
            try {
                int idx = Integer.parseInt(args[1]);
                plugin.getPesananManager().submitOrder(player, idx);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text(
                    "§cNomor pesanan harus berupa angka.", NamedTextColor.RED).asComponent());
            }
            return true;
        }

        // Default: tampilkan menu
        plugin.getPesananManager().showOrderMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("lihat", "serahkan");
        }
        return List.of();
    }
}
