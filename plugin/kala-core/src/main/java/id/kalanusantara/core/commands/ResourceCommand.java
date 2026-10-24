package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.world.ResourceWorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /tambang — Perintah akses Dunia Tambang Nusantara
 */
public class ResourceCommand implements CommandExecutor, TabCompleter {

    private final KalaCorePlugin plugin;

    public ResourceCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Perintah ini hanya untuk pemain!", NamedTextColor.RED));
            return true;
        }

        ResourceWorldManager manager = plugin.getResourceWorldManager();

        if (args.length == 0) {
            plugin.getMenuManager().openTambangMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pergi", "go", "tp" -> manager.teleportToResource(player);
            case "pulang", "spawn", "back" -> manager.teleportToMain(player);
            case "info", "aturan" -> {
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("⛏ ══ Dunia Tambang Nusantara ══", NamedTextColor.GOLD));
                player.sendMessage(Component.text("• Tempat menambang bijih mineral, menebang kayu, dan mencari material bebas.", NamedTextColor.GRAY));
                player.sendMessage(Component.text("• Klaim tanah dinonaktifkan di dunia ini.", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("• Dunia ini direset berkala agar sumber daya alam selalu terisi ulang.", NamedTextColor.AQUA));
                player.sendMessage(Component.text("• Gunakan §e/tambang pulang §7kapan pun untuk kembali ke Labuhan Awal.", NamedTextColor.GRAY));
                player.sendMessage(Component.text("══════════════════════════════════", NamedTextColor.GOLD));
            }
            case "reset" -> {
                if (!player.hasPermission("kala.admin")) {
                    player.sendMessage(Component.text("Hanya admin yang dapat mereset dunia tambang!", NamedTextColor.RED));
                    return true;
                }
                boolean confirmed = args.length >= 2 && args[1].equalsIgnoreCase("confirm");
                manager.resetResourceWorld(player, confirmed);
            }
            default -> player.sendMessage(Component.text("Penggunaan: /tambang [pergi|pulang|info|reset confirm]", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("pergi");
            list.add("pulang");
            list.add("info");
            if (sender.hasPermission("kala.admin")) {
                list.add("reset");
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset") && sender.hasPermission("kala.admin")) {
            list.add("confirm");
        }
        return list;
    }
}
