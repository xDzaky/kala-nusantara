package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * SpeedCommand — /speed <1-5/custom> atau /speed <walk|fly|both> <nilai> [pemain]
 *
 * Nilai 1 = Kecepatan normal (1x) (Walk: 0.2, Fly: 0.1)
 * Nilai 2 = 2x kecepatan
 * Nilai 5 = 5x kecepatan (Maksimal Walk: 1.0)
 * Mendukung desimal (misal 1.5, 2.5) dan opsi reset.
 */
public class SpeedCommand implements CommandExecutor, TabCompleter {

    private final KalaCorePlugin plugin;

    public SpeedCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kala.speed") && !sender.hasPermission("kala.admin")) {
            sender.sendMessage(Component.text("Kamu tidak memiliki izin untuk menggunakan perintah ini.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // Cek reset
        if (args[0].equalsIgnoreCase("reset")) {
            Player target = (sender instanceof Player p) ? p : null;
            if (args.length >= 2) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Pemain tidak ditemukan!", NamedTextColor.RED));
                    return true;
                }
            }
            if (target == null) {
                sender.sendMessage(Component.text("Tentukan pemain untuk reset speed!", NamedTextColor.RED));
                return true;
            }
            target.setWalkSpeed(0.2f);
            target.setFlySpeed(0.1f);
            target.sendMessage(Component.text("Kecepatan gerak dan terbang dikembalikan ke normal (1x).", NamedTextColor.GREEN));
            if (!target.equals(sender)) {
                sender.sendMessage(Component.text("Kecepatan " + target.getName() + " direset ke normal.", NamedTextColor.GREEN));
            }
            return true;
        }

        // Subcommand: walk, fly, both/all
        String mode = "auto";
        String speedStr = args[0];
        Player target = (sender instanceof Player p) ? p : null;

        if (args[0].equalsIgnoreCase("walk") || args[0].equalsIgnoreCase("fly") || args[0].equalsIgnoreCase("both") || args[0].equalsIgnoreCase("all")) {
            if (args.length < 2) {
                sendUsage(sender);
                return true;
            }
            mode = args[0].toLowerCase();
            speedStr = args[1];

            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Pemain tidak ditemukan!", NamedTextColor.RED));
                    return true;
                }
            }
        } else {
            // Format: /speed <nilai> [target]
            if (args.length >= 2) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Pemain tidak ditemukan!", NamedTextColor.RED));
                    return true;
                }
            }
        }

        if (target == null) {
            sender.sendMessage(Component.text("Command ini memerlukan target pemain jika dijalankan dari console.", NamedTextColor.RED));
            return true;
        }

        float multiplier;
        try {
            multiplier = Float.parseFloat(speedStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Nilai kecepatan harus berupa angka (misal: 1, 2, 3, 4, 5 atau 1.5).", NamedTextColor.RED));
            return true;
        }

        if (multiplier < 0.0f || multiplier > 10.0f) {
            sender.sendMessage(Component.text("Nilai kecepatan harus di antara 0 sampai 10.", NamedTextColor.RED));
            return true;
        }

        // Bukkit Walk default = 0.2f (max 1.0f)
        // Bukkit Fly default = 0.1f (max 1.0f)
        float walkSpeed = Math.min(1.0f, Math.max(0.0f, multiplier * 0.2f));
        float flySpeed = Math.min(1.0f, Math.max(0.0f, multiplier * 0.1f));

        if (mode.equals("auto")) {
            if (target.isFlying()) {
                target.setFlySpeed(flySpeed);
                notifySpeed(sender, target, "terbang (fly)", multiplier);
            } else {
                target.setWalkSpeed(walkSpeed);
                notifySpeed(sender, target, "jalan (walk)", multiplier);
            }
        } else if (mode.equals("walk")) {
            target.setWalkSpeed(walkSpeed);
            notifySpeed(sender, target, "jalan (walk)", multiplier);
        } else if (mode.equals("fly")) {
            target.setFlySpeed(flySpeed);
            notifySpeed(sender, target, "terbang (fly)", multiplier);
        } else {
            // both / all
            target.setWalkSpeed(walkSpeed);
            target.setFlySpeed(flySpeed);
            notifySpeed(sender, target, "jalan & terbang (all)", multiplier);
        }

        return true;
    }

    private void notifySpeed(CommandSender sender, Player target, String type, float multiplier) {
        String formatted = (multiplier == (int) multiplier) ? String.valueOf((int) multiplier) : String.valueOf(multiplier);
        Component msg = Component.text("Kecepatan " + type + " diatur ke " + formatted + "x.", NamedTextColor.GREEN);
        target.sendMessage(msg);
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text("Kecepatan " + type + " untuk " + target.getName() + " diatur ke " + formatted + "x.", NamedTextColor.GREEN));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("─── Speed Commands ───", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/speed <1-5/custom>", NamedTextColor.YELLOW)
                .append(Component.text(" — Atur kecepatan (1=normal, 2=2x, 5=5x/maks)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/speed <walk|fly|both> <nilai>", NamedTextColor.YELLOW)
                .append(Component.text(" — Atur kecepatan spesifik", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/speed reset", NamedTextColor.YELLOW)
                .append(Component.text(" — Kembalikan kecepatan ke normal (1x)", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kala.speed") && !sender.hasPermission("kala.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("1", "2", "3", "4", "5", "walk", "fly", "both", "reset"));
            return list.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("walk") || args[0].equalsIgnoreCase("fly") || args[0].equalsIgnoreCase("both") || args[0].equalsIgnoreCase("all"))) {
            return List.of("1", "2", "3", "4", "5");
        }

        return List.of();
    }
}
