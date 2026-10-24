package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.ui.MenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /kompas — buka Kompas Pengembara (menu utama)
 */
public class KompasCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public KompasCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                plugin.getLocaleManager().get("id", "error.player-only"),
                NamedTextColor.RED));
            return true;
        }

        MenuManager.KompasTab tab = MenuManager.KompasTab.MISI;
        if (args.length > 0) {
            try {
                tab = MenuManager.KompasTab.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        plugin.getMenuManager().openKompas(player, tab);
        return true;
    }
}
