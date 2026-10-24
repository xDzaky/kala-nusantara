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
 * JurnalCommand — /jurnal [halaman]
 * Menampilkan entri Jurnal Penjelajah pemain.
 */
public class JurnalCommand implements CommandExecutor, TabCompleter {

    private final KalaCorePlugin plugin;

    public JurnalCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini hanya untuk pemain.");
            return true;
        }

        int page = 1;
        if (args.length >= 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text(
                    "§cPenggunaan: /jurnal [halaman]", NamedTextColor.RED).asComponent());
                return true;
            }
        }

        plugin.getJurnalManager().showJurnal(player, page);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("1", "2", "3");
        return List.of();
    }
}
