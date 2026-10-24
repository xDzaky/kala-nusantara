package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ekspedisi — Membuka GUI Ekspedisi Musiman
 */
public class ExpeditionCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public ExpeditionCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Perintah ini hanya untuk pemain!", NamedTextColor.RED));
            return true;
        }

        plugin.getMenuManager().openExpeditionGUI(player);
        return true;
    }
}
