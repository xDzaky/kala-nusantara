package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PanduanCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public PanduanCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Hanya pemain yang dapat membuka Panduan Bedrock & HP!");
            return true;
        }

        plugin.getMenuManager().openBedrockGuideGUI(player);
        return true;
    }
}
