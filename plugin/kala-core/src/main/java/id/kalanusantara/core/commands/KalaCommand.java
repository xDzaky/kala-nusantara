package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /kala — perintah utama dengan subcommands
 */
public class KalaCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public KalaCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(sender);
            case "saldo", "balance", "bal" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                double bal = plugin.getEconomy().getBalance(player);
                sender.sendMessage(Component.text(
                    plugin.getLocaleManager().get(player, "economy.balance",
                        String.format("%,.0f", bal),
                        plugin.getKalaConfig().getCurrencySymbol()),
                    NamedTextColor.YELLOW));
            }
            case "profil", "profile", "me" -> {
                if (!(sender instanceof Player player)) return true;
                var profile = plugin.getIdentityManager().get(player.getUniqueId());
                var locale = plugin.getLocaleManager();
                sender.sendMessage(Component.text(locale.get(player, "identity.profile.header"), NamedTextColor.GOLD));
                profile.ifPresent(p -> {
                    sender.sendMessage(Component.text(locale.get(player, "identity.profile.name", p.getName()), NamedTextColor.WHITE));
                    sender.sendMessage(Component.text(locale.get(player, "identity.profile.profesi", p.getProfesi()), NamedTextColor.WHITE));
                    sender.sendMessage(Component.text(locale.get(player, "identity.profile.kampung",
                        p.getKampung() != null ? p.getKampung() : "Belum ada"), NamedTextColor.WHITE));
                });
            }
            case "reload" -> {
                if (!sender.hasPermission("kala.admin")) {
                    sender.sendMessage(Component.text(
                        plugin.getLocaleManager().get("id", "error.no-permission"),
                        NamedTextColor.RED));
                    return true;
                }
                plugin.getKalaConfig().reload();
                plugin.getLocaleManager().reload();
                plugin.getQuestManager().loadQuests();
                sender.sendMessage(Component.text(
                    plugin.getLocaleManager().get("id", "admin.reload"),
                    NamedTextColor.GREEN));
            }
            case "claim-event" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Penggunaan: /kala claim-event <id_event>", NamedTextColor.RED));
                    return true;
                }
                String eventId = args[1];
                boolean claimed = plugin.getEventContributionLedger().claimReward(eventId, player, 1500.0, 50);
                if (!claimed) {
                    player.sendMessage(Component.text("§cAnda tidak memiliki poin kontribusi atau reward sudah pernah diklaim untuk event ini!", NamedTextColor.RED));
                }
            }
            case "pay", "transfer" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(Component.text("Penggunaan: /kala pay <pemain> <jumlah>", NamedTextColor.RED));
                    return true;
                }
                String targetName = args[1];
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    player.sendMessage(Component.text("Pemain target tidak online atau tidak ditemukan!", NamedTextColor.RED));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    var result = plugin.getEconomy().transfer(player.getUniqueId(), target.getUniqueId(), amount);
                    switch (result) {
                        case SUCCESS -> {
                            player.sendMessage(Component.text("✔ Berhasil mentransfer " + plugin.getEconomy().format(amount) + " ke " + target.getName() + "!", NamedTextColor.GREEN));
                            target.sendMessage(Component.text("✦ Anda menerima transfer " + plugin.getEconomy().format(amount) + " dari " + player.getName() + "!", NamedTextColor.GOLD));
                        }
                        case INSUFFICIENT_FUNDS -> player.sendMessage(Component.text("✘ Saldo Anda tidak mencukupi untuk transfer ini!", NamedTextColor.RED));
                        case SAME_PLAYER -> player.sendMessage(Component.text("✘ Anda tidak dapat mentransfer uang ke diri sendiri!", NamedTextColor.RED));
                        case INVALID_AMOUNT -> player.sendMessage(Component.text("✘ Jumlah transfer tidak valid!", NamedTextColor.RED));
                        case DATABASE_ERROR -> player.sendMessage(Component.text("✘ Terjadi kesalahan transaksi database!", NamedTextColor.RED));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Jumlah transfer harus berupa angka!", NamedTextColor.RED));
                }
            }
            case "kontrak" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("─── Kontrak Gotong Royong Kampung (Minggu Ini) ───", NamedTextColor.GOLD));
                for (var c : plugin.getVillageContractManager().getActiveContracts()) {
                    sender.sendMessage(Component.text("• §e" + c.title() + " §7(" + c.category() + ")")
                        .append(Component.text("\n  Target: §f" + c.targetCount() + " §7| Hadiah: §6" + plugin.getEconomy().format(c.rewardRupiah()) + " §7+ §b" + c.rewardExp() + " EXP")));
                }
            }
            case "penemuan", "koleksi" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                int count = plugin.getDiscoveryManager().getDiscoveryCount(player.getUniqueId());
                player.sendMessage(Component.text("─── Katalog Penemuan Nusantara ───", NamedTextColor.GOLD));
                player.sendMessage(Component.text("Total Catatan Observasi: §a" + count + " Entri", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("§7Gunakan Kit Survei atau jelajahi pulau untuk mencatat satwa dan landmark Nusantara!", NamedTextColor.GRAY));
            }
            case "version", "ver" -> {
                sender.sendMessage(Component.text(
                    "KalaCore v" + plugin.getDescription().getVersion(),
                    NamedTextColor.GOLD));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("─── Kala Nusantara ───", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/kompas", NamedTextColor.YELLOW)
            .append(Component.text(" — Buka menu utama", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/jurnal", NamedTextColor.YELLOW)
            .append(Component.text(" — Buka jurnal penjelajah", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/kala saldo", NamedTextColor.YELLOW)
            .append(Component.text(" — Cek saldo Rupiah Kala", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/kala profil", NamedTextColor.YELLOW)
            .append(Component.text(" — Lihat profilmu", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/claim", NamedTextColor.YELLOW)
            .append(Component.text(" — Klaim lahan (GriefPrevention)", NamedTextColor.GRAY)));
    }
}
