package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /musim — Membuka GUI Kalender 8 Minggu Musim Nusantara
 */
public class SeasonCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public SeasonCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Musim aktif: " + plugin.getSeasonManager().getSeasonName() + " (Minggu " + plugin.getSeasonManager().getCurrentWeek() + "/8)", NamedTextColor.GOLD));
            return true;
        }

        plugin.getMenuManager().openSeasonCalendarGUI(player);
        return true;
    }
}
