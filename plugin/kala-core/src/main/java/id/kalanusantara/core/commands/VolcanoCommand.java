package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VolcanoCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public VolcanoCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Status Gunung Murka: " + plugin.getKalaEventManager().getPhaseDisplayName());
            return true;
        }

        plugin.getMenuManager().openVolcanoGUI(player);
        return true;
    }
}
