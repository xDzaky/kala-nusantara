package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.profession.KalaProfession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * ProfesiCommand — /profesi [pilih|lihat]
 *
 * /profesi         → buka GUI pilih profesi
 * /profesi lihat   → tampilkan profesi aktif di chat
 */
public class ProfesiCommand implements CommandExecutor, TabCompleter {

    private final KalaCorePlugin plugin;

    public ProfesiCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini hanya untuk pemain.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("pilih")) {
            plugin.getProfesiManager().openProfesiGui(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("lihat") || args[0].equalsIgnoreCase("info")) {
            showInfo(player);
            return true;
        }

        player.sendMessage(Component.text(
            "§cPenggunaan: /profesi [pilih|lihat]", NamedTextColor.RED).asComponent());
        return true;
    }

    private void showInfo(Player player) {
        plugin.getIdentityManager().get(player.getUniqueId()).ifPresentOrElse(profile -> {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("─── Profesimu ───", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Profesi: §e" + profile.getProfesi()).asComponent());

            // Temukan deskripsi dari enum
            KalaProfession prof = KalaProfession.fromId(
                profile.getProfesi().toUpperCase().replace(" ", "_"));
            player.sendMessage(Component.text("§7" + prof.getDescription()).asComponent());
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("§7Ketik §e/profesi pilih §7untuk ganti profesi.").asComponent());
        }, () -> player.sendMessage(Component.text(
            "§cProfil tidak ditemukan.", NamedTextColor.RED).asComponent()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("pilih", "lihat");
        return List.of();
    }
}
