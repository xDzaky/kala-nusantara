package id.kalanusantara.core.commands;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.event.WorldImpactPolicy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /kalaadmin — perintah admin
 */
public class KalaAdminCommand implements CommandExecutor {

    private final KalaCorePlugin plugin;

    public KalaAdminCommand(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kala.admin")) {
            sender.sendMessage(Component.text(
                plugin.getLocaleManager().get("id", "error.no-permission"),
                NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getKalaConfig().reload();
                plugin.getLocaleManager().reload();
                plugin.getQuestManager().loadQuests();
                sender.sendMessage(Component.text(
                    plugin.getLocaleManager().get("id", "admin.reload"),
                    NamedTextColor.GREEN));
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                Location loc = player.getLocation();
                // Update config spawn
                plugin.getKalaConfig().getRaw().set("spawn.world", loc.getWorld().getName());
                plugin.getKalaConfig().getRaw().set("spawn.x", loc.getX());
                plugin.getKalaConfig().getRaw().set("spawn.y", loc.getY());
                plugin.getKalaConfig().getRaw().set("spawn.z", loc.getZ());
                plugin.saveConfig();
                // Set server spawn
                loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                sender.sendMessage(Component.text(
                    plugin.getLocaleManager().get("id", "admin.setspawn"),
                    NamedTextColor.GREEN));
            }
            case "setworldborder", "border" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka border <radius>", NamedTextColor.RED));
                    return true;
                }
                try {
                    int radius = Integer.parseInt(args[1]);
                    Bukkit.getWorlds().stream()
                        .filter(w -> w.getName().equals(plugin.getKalaConfig().getSpawnWorld()))
                        .findFirst()
                        .ifPresent(w -> {
                            w.getWorldBorder().setCenter(0, 0);
                            w.getWorldBorder().setSize(radius * 2);
                        });
                    sender.sendMessage(Component.text(
                        plugin.getLocaleManager().get("id", "admin.setworldborder", radius),
                        NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Radius harus angka!", NamedTextColor.RED));
                }
            }
            case "give-rupiah" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Penggunaan: /ka give-rupiah <pemain> <jumlah>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Pemain tidak ditemukan!", NamedTextColor.RED));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    plugin.getEconomy().deposit(target.getUniqueId(), amount, "Admin give-rupiah");
                    sender.sendMessage(Component.text("Berhasil memberi Rp " + amount + " ke " + target.getName(), NamedTextColor.GREEN));
                    target.sendMessage(Component.text("Admin memberimu Rp " + amount + "!", NamedTextColor.GOLD));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Jumlah harus angka!", NamedTextColor.RED));
                }
            }
            case "debug" -> {
                boolean debug = !plugin.getKalaConfig().isDebugMode();
                plugin.getKalaConfig().getRaw().set("debug", debug);
                plugin.saveConfig();
                plugin.getKalaConfig().reload();
                sender.sendMessage(Component.text(
                    plugin.getLocaleManager().get("id", debug ? "admin.debug-on" : "admin.debug-off"),
                    NamedTextColor.YELLOW));
            }
            case "kala", "volcano" -> {
                // /ka kala <start|next|stop|status>
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka volcano <start|next|stop|status>", NamedTextColor.RED));
                    return true;
                }
                var kem = plugin.getKalaEventManager();
                switch (args[1].toLowerCase()) {
                    case "start" -> {
                        boolean ok = kem.startEvent();
                        sender.sendMessage(ok
                            ? Component.text("Event Kala dimulai! Fase: " + kem.getPhaseDisplayName(), NamedTextColor.GREEN)
                            : Component.text("Event sudah berjalan! Gunakan /ka kala next atau stop.", NamedTextColor.RED));
                    }
                    case "next" -> {
                        boolean ok = kem.advancePhase();
                        sender.sendMessage(ok
                            ? Component.text("Fase maju! Sekarang: " + kem.getPhaseDisplayName(), NamedTextColor.GREEN)
                            : Component.text("Tidak bisa maju fase (IDLE atau sudah akhir).", NamedTextColor.RED));
                    }
                    case "stop" -> {
                        boolean ok = kem.stopEvent();
                        sender.sendMessage(ok
                            ? Component.text("Event Kala dihentikan.", NamedTextColor.GREEN)
                            : Component.text("Event tidak sedang berjalan.", NamedTextColor.RED));
                    }
                    case "status" -> sender.sendMessage(Component.text(
                        "Fase Kala saat ini: " + kem.getPhaseDisplayName(), NamedTextColor.GOLD));
                    default -> sender.sendMessage(Component.text(
                        "Subcommand: start, next, stop, status", NamedTextColor.RED));
                }
            }
            case "encounter" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka encounter <start|next|stop>", NamedTextColor.RED));
                    return true;
                }
                var director = plugin.getVolcanoEncounterDirector();
                if (director == null) {
                    sender.sendMessage(Component.text("VolcanoEncounterDirector belum aktif!", NamedTextColor.RED));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "start" -> {
                        director.startEncounter("admin_test_" + System.currentTimeMillis());
                        sender.sendMessage(Component.text("Encounter subfase PUNCAK dimulai!", NamedTextColor.GREEN));
                    }
                    case "next" -> {
                        director.advanceNextSubphase();
                        sender.sendMessage(Component.text("Encounter maju ke subfase berikutnya: " +
                            (director.getCurrentSubphase() != null ? director.getCurrentSubphase().getDisplayName() : "SELESAI"), NamedTextColor.GREEN));
                    }
                    case "stop" -> {
                        director.stopEncounter();
                        sender.sendMessage(Component.text("Encounter dihentikan dan dibersihkan.", NamedTextColor.YELLOW));
                    }
                    default -> sender.sendMessage(Component.text("Subcommand: start, next, stop", NamedTextColor.RED));
                }
            }
            case "season-event" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka season-event <badai|abu|stop|status>", NamedTextColor.RED));
                    return true;
                }
                var sed = plugin.getSeasonalEventDirector();
                if (sed == null) {
                    sender.sendMessage(Component.text("SeasonalEventDirector belum aktif!", NamedTextColor.RED));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "badai" -> {
                        sed.startEvent(id.kalanusantara.core.event.SeasonalEventDirector.SeasonalEventType.BADAI_SELAT);
                        sender.sendMessage(Component.text("Mikro-event Badai Selat dimulai!", NamedTextColor.AQUA));
                    }
                    case "abu" -> {
                        sed.startEvent(id.kalanusantara.core.event.SeasonalEventDirector.SeasonalEventType.ABU_DAN_BARA);
                        sender.sendMessage(Component.text("Mikro-event Abu dan Bara dimulai!", NamedTextColor.GOLD));
                    }
                    case "stop" -> {
                        sed.stopEvent();
                        sender.sendMessage(Component.text("Event musiman dihentikan dan dibersihkan.", NamedTextColor.YELLOW));
                    }
                    case "status" -> {
                        sender.sendMessage(Component.text("Event musiman aktif: " + sed.getActiveEvent().getDisplayName(), NamedTextColor.YELLOW));
                    }
                    default -> sender.sendMessage(Component.text("Subcommand: badai, abu, stop, status", NamedTextColor.RED));
                }
            }
            case "rockfall" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                int count = args.length >= 2 ? Integer.parseInt(args[1]) : 3;
                plugin.getHazardDirector().launchRockfallSalvo(player.getLocation(), "admin_test", count);
                sender.sendMessage(Component.text("Meluncurkan salvo rockfall tertelegrafis (" + count + " batu, zero block damage)!", NamedTextColor.GOLD));
            }
            case "vent" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                plugin.getHazardDirector().spawnHeatVent(player.getLocation(), "admin_test", 30);
                sender.sendMessage(Component.text("Membuat retakan panas / heat vent selama 30 detik!", NamedTextColor.GOLD));
            }
            case "backup" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka backup <create|list>", NamedTextColor.RED));
                    return true;
                }
                if (args[1].equalsIgnoreCase("create")) {
                    plugin.getBackupManager().createBackup(sender);
                } else if (args[1].equalsIgnoreCase("list")) {
                    plugin.getBackupManager().listBackups(sender);
                }
            }
            case "resource", "tambang" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
                    sender.sendMessage(Component.text("Penggunaan: /ka resource reset confirm", NamedTextColor.RED));
                    return true;
                }
                boolean confirmed = args.length >= 3 && args[2].equalsIgnoreCase("confirm");
                plugin.getResourceWorldManager().resetResourceWorld(sender, confirmed);
            }
            case "project", "proyek" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Penggunaan: /ka project <reset> <id>", NamedTextColor.RED));
                    return true;
                }
                if (args[1].equalsIgnoreCase("reset")) {
                    plugin.getKampungProjectManager().resetProject(args[2]);
                    sender.sendMessage(Component.text("Proyek '" + args[2] + "' telah direset ke 0.", NamedTextColor.GREEN));
                }
            }
            case "npc" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka npc <spawn|remove|list> [id] [type]", NamedTextColor.RED));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "spawn" -> {
                        if (args.length < 4) {
                            sender.sendMessage(Component.text("Penggunaan: /ka npc spawn <id> <type>", NamedTextColor.RED));
                            sender.sendMessage(Component.text("Type: MBAH_KALA, PEDAGANG", NamedTextColor.GRAY));
                            return true;
                        }
                        String npcId = args[2].toLowerCase();
                        String npcType = args[3].toUpperCase();
                        plugin.getNpcManager().spawnNpc(player, npcId, npcType);
                        sender.sendMessage(Component.text("NPC '" + npcId + "' (" + npcType + ") di-spawn!", NamedTextColor.GREEN));
                    }
                    case "remove" -> {
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Penggunaan: /ka npc remove <id>", NamedTextColor.RED));
                            return true;
                        }
                        String npcId2 = args[2].toLowerCase();
                        boolean ok2 = plugin.getNpcManager().removeNpc(npcId2);
                        sender.sendMessage(ok2
                            ? Component.text("NPC '" + npcId2 + "' dihapus.", NamedTextColor.GREEN)
                            : Component.text("NPC '" + npcId2 + "' tidak ditemukan.", NamedTextColor.RED));
                    }
                    case "list" -> {
                        var npcs = plugin.getNpcManager().getSpawnedNpcs();
                        if (npcs.isEmpty()) {
                            sender.sendMessage(Component.text("Tidak ada NPC aktif.", NamedTextColor.GRAY));
                        } else {
                            sender.sendMessage(Component.text("NPC aktif:", NamedTextColor.GOLD));
                            npcs.forEach((nId, uid) ->
                                sender.sendMessage(Component.text("  - " + nId + " (" + uid + ")", NamedTextColor.WHITE)));
                        }
                    }
                    default -> sender.sendMessage(Component.text("Subcommand: spawn, remove, list", NamedTextColor.RED));
                }
            }
            case "quest" -> {
                if (args.length < 4 || !args[1].equalsIgnoreCase("start")) {
                    sender.sendMessage(Component.text("Penggunaan: /ka quest start <pemain> <questId>", NamedTextColor.RED));
                    return true;
                }
                Player qtarget = Bukkit.getPlayer(args[2]);
                if (qtarget == null) {
                    sender.sendMessage(Component.text("Pemain tidak ditemukan!", NamedTextColor.RED));
                    return true;
                }
                boolean qok = plugin.getQuestProgressTracker().startQuest(qtarget, args[3]);
                sender.sendMessage(qok
                    ? Component.text("Quest '" + args[3] + "' dimulai untuk " + qtarget.getName(), NamedTextColor.GREEN)
                    : Component.text("Gagal mulai quest.", NamedTextColor.RED));
            }
            case "staging" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka staging <create|delete|tp|info> [seed]", NamedTextColor.RED));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "create" -> {
                        long seed = args.length > 2 ? args[2].hashCode() : "KalaStaging2026".hashCode();
                        plugin.getResourceWorldManager().createStagingWorld(sender, seed);
                    }
                    case "delete" -> {
                        plugin.getResourceWorldManager().deleteStagingWorld(sender);
                    }
                    case "tp" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage("Hanya pemain!");
                            return true;
                        }
                        plugin.getResourceWorldManager().teleportToStaging(p);
                    }
                    case "info" -> {
                        var sw = plugin.getResourceWorldManager().getStagingWorld();
                        if (sw == null) {
                            sender.sendMessage(Component.text("Staging world tidak aktif.", NamedTextColor.YELLOW));
                        } else {
                            sender.sendMessage(Component.text("─── Info Staging World ───", NamedTextColor.GOLD));
                            sender.sendMessage(Component.text("Nama: " + sw.getName(), NamedTextColor.WHITE));
                            sender.sendMessage(Component.text("Border: " + (int) sw.getWorldBorder().getSize() + " blok", NamedTextColor.WHITE));
                            sender.sendMessage(Component.text("Pemain di dunia: " + sw.getPlayers().size(), NamedTextColor.WHITE));
                            sender.sendMessage(Component.text("Loaded Chunks: " + sw.getLoadedChunks().length, NamedTextColor.WHITE));
                        }
                    }
                    default -> sender.sendMessage(Component.text("Subcommand staging: create, delete, tp, info", NamedTextColor.RED));
                }
            }
            case "terraform" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Penggunaan: /ka terraform <create|tp|info> [seed]", NamedTextColor.RED));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "create" -> {
                        long seed = args.length > 2 ? args[2].hashCode() : "KalaTerraform2026".hashCode();
                        plugin.getResourceWorldManager().createTerraformStagingWorld(sender, seed);
                    }
                    case "tp" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                            return true;
                        }
                        plugin.getResourceWorldManager().teleportToTerraformStaging(p);
                    }
                    case "info" -> {
                        var world = plugin.getResourceWorldManager().getTerraformStagingWorld();
                        if (world == null) {
                            sender.sendMessage(Component.text("Terraform staging belum aktif.", NamedTextColor.YELLOW));
                        } else {
                            sender.sendMessage(Component.text("Terraform staging: " + world.getName()
                                + " | border " + (int) world.getWorldBorder().getSize()
                                + " | chunks aktif " + world.getLoadedChunks().length, NamedTextColor.GOLD));
                        }
                    }
                    default -> sender.sendMessage(Component.text("Subcommand Terraform: create, tp, info", NamedTextColor.RED));
                }
            }
            case "structure" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Hanya pemain!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
                    sender.sendMessage(Component.text("Penggunaan: /ka structure spawn <pos_pelaut|menara|kuil|gua>", NamedTextColor.RED));
                    return true;
                }
                String sType = args[2].toLowerCase();
                id.kalanusantara.core.world.structure.NusantaraStructureEngine.StructureType chosen = null;
                switch (sType) {
                    case "pos_pelaut", "pelaut", "dermaga" -> chosen = id.kalanusantara.core.world.structure.NusantaraStructureEngine.StructureType.POS_PELAUT;
                    case "menara", "menara_pengawas" -> chosen = id.kalanusantara.core.world.structure.NusantaraStructureEngine.StructureType.MENARA_PENGAWAS;
                    case "kuil", "kuil_kala", "altar" -> chosen = id.kalanusantara.core.world.structure.NusantaraStructureEngine.StructureType.KUIL_KALA;
                    case "gua", "gua_tambang", "tambang" -> chosen = id.kalanusantara.core.world.structure.NusantaraStructureEngine.StructureType.GUA_TAMBANG;
                }
                if (chosen == null) {
                    sender.sendMessage(Component.text("Tipe struktur tidak valid! Pilih: pos_pelaut, menara, kuil, gua", NamedTextColor.RED));
                    return true;
                }
                plugin.getStructureEngine().spawnStructure(player.getLocation(), chosen, sender);
            }
            case "status" -> {
                sender.sendMessage(Component.text("─── KalaCore Status ───", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Versi: " + plugin.getDescription().getVersion() + " (V7 Robust)", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("Pemain online: " + Bukkit.getOnlinePlayers().size(), NamedTextColor.WHITE));
                sender.sendMessage(Component.text("Quest terdaftar: " + plugin.getQuestManager().getAllQuests().size(), NamedTextColor.WHITE));
                sender.sendMessage(Component.text("NPC aktif: " + plugin.getNpcManager().getSpawnedNpcs().size(), NamedTextColor.WHITE));
                sender.sendMessage(Component.text("Entitas Event Aktif: " + plugin.getWorldImpactPolicy().getActiveEntityCount() + " / " + WorldImpactPolicy.TOTAL_EVENT_ENTITY_CAP, NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Gunung Murka: " + plugin.getKalaEventManager().getCurrentPhase().getDisplayName(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Event Musiman: " + plugin.getSeasonalEventDirector().getActiveEvent().getDisplayName(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Debug: " + plugin.getKalaConfig().isDebugMode(), NamedTextColor.WHITE));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("─── KalaAdmin Commands ───", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/ka reload", NamedTextColor.YELLOW).append(Component.text(" — Reload config+locale+quests", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka setspawn", NamedTextColor.YELLOW).append(Component.text(" — Set spawn ke posisimu", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka border <r>", NamedTextColor.YELLOW).append(Component.text(" — Set world border radius", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka terraform <create|tp|info>", NamedTextColor.YELLOW).append(Component.text(" — Kelola staging Terraform", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka give-rupiah <p> <n>", NamedTextColor.YELLOW).append(Component.text(" — Beri rupiah ke pemain", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka npc spawn <id> <type>", NamedTextColor.YELLOW).append(Component.text(" — Spawn NPC di posisimu", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka npc remove/list", NamedTextColor.YELLOW).append(Component.text(" — Kelola NPC", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka quest start <p> <id>", NamedTextColor.YELLOW).append(Component.text(" — Mulai quest", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka kala <start|next|stop|status>", NamedTextColor.YELLOW).append(Component.text(" — Kelola event Kala", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka encounter <start|next|stop>", NamedTextColor.YELLOW).append(Component.text(" — Urutan subfase PUNCAK V6", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka season-event <badai|abu|stop>", NamedTextColor.YELLOW).append(Component.text(" — Mikro-event musiman V6", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka rockfall [count]", NamedTextColor.YELLOW).append(Component.text(" — Test salvo rockfall tertelegrafis", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka vent", NamedTextColor.YELLOW).append(Component.text(" — Test retakan panas / heat vent", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka backup <create|list>", NamedTextColor.YELLOW).append(Component.text(" — Snapshot backup data", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka staging <create|delete|tp|info>", NamedTextColor.YELLOW).append(Component.text(" — Kelola staging world test", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka structure spawn <type>", NamedTextColor.YELLOW).append(Component.text(" — Spawn struktur Nusantara terkurasi", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka resource reset", NamedTextColor.YELLOW).append(Component.text(" — Reset aman dunia tambang", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka project reset <id>", NamedTextColor.YELLOW).append(Component.text(" — Reset proyek kampung", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka debug", NamedTextColor.YELLOW).append(Component.text(" — Toggle debug mode", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ka status", NamedTextColor.YELLOW).append(Component.text(" — Status KalaCore", NamedTextColor.GRAY)));
    }
}
