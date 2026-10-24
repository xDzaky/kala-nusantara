package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.project.KampungProjectManager;
import id.kalanusantara.core.project.VillageProject;
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
 * /proyek — Perintah Proyek Kampung (Gotong Royong)
 */
public class ProyekCommand implements CommandExecutor, TabCompleter {

    private final KalaCorePlugin plugin;

    public ProyekCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Hanya pemain yang dapat melihat proyek!", NamedTextColor.RED));
            return true;
        }

        KampungProjectManager manager = plugin.getKampungProjectManager();

        if (args.length == 0) {
            plugin.getMenuManager().openProyekMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("lihat") || args[0].equalsIgnoreCase("chat")) {
            showProjectList(player, manager);
            return true;
        }

        if (args[0].equalsIgnoreCase("setor") || args[0].equalsIgnoreCase("sumbang")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Penggunaan: /proyek setor <id_proyek> [jumlah]", NamedTextColor.RED));
                player.sendMessage(Component.text("Contoh: /proyek setor lumbung_padi 32", NamedTextColor.GRAY));
                return true;
            }

            String id = args[1].toLowerCase();
            int amount = -1; // default all available
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        player.sendMessage(Component.text("Jumlah harus lebih dari 0!", NamedTextColor.RED));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Jumlah harus berupa angka!", NamedTextColor.RED));
                    return true;
                }
            }

            manager.contribute(player, id, amount);
            return true;
        }

        player.sendMessage(Component.text("Subcommand: /proyek lihat atau /proyek setor <id> [jumlah]", NamedTextColor.RED));
        return true;
    }

    private void showProjectList(Player player, KampungProjectManager manager) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("🏛 ══ Proyek Gotong Royong Kampung ══", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Bantu desa membangun infrastruktur dengan menyumbang material!", NamedTextColor.GRAY));
        player.sendMessage(Component.empty());

        for (VillageProject p : manager.getAllProjects()) {
            NamedTextColor titleColor = p.isCompleted() ? NamedTextColor.DARK_GRAY : NamedTextColor.YELLOW;
            player.sendMessage(Component.text("• " + p.getName() + " §8[" + p.getId() + "]", titleColor));
            player.sendMessage(Component.text("  " + p.getDescription(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("  Material: §f" + p.getTargetMaterial().name() + " §8| Target: §e" + p.getTargetAmount()));
            player.sendMessage(Component.text("  Progress: " + p.getProgressBar(15) + " §e" + String.format("%.1f%%", p.getProgressPercentage()) + " (" + p.getCurrentAmount() + "/" + p.getTargetAmount() + ")"));

            if (p.isCompleted()) {
                player.sendMessage(Component.text("  Status: §a✔ SELESAI DIBANGUN", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("  Setor: §f/proyek setor " + p.getId() + " [jumlah]", NamedTextColor.AQUA));
            }
            player.sendMessage(Component.empty());
        }
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("lihat");
            completions.add("setor");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setor")) {
            for (VillageProject p : plugin.getKampungProjectManager().getAllProjects()) {
                if (!p.isCompleted()) {
                    completions.add(p.getId());
                }
            }
        }
        return completions;
    }
}
