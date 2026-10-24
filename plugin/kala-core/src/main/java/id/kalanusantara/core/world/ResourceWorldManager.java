package id.kalanusantara.core.world;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * ResourceWorldManager — Mengelola dunia tambang terpisah (kala_resource).
 *
 * Mencegah kerusakan lanskap dunia utama (kala_main) akibat strip-mining masif.
 * Dilengkapi protokol reset aman (safe evacuation & world recreation).
 */
public class ResourceWorldManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    public static final String RESOURCE_WORLD_NAME = "kala_resource";
    public static final String STAGING_WORLD_NAME = "kala_resource_test";
    public static final String TERRAFORM_STAGING_WORLD_NAME = "kala_terraform_test";
    public static final int RESOURCE_BORDER_RADIUS = 1500; // diameter 3000
    public static final int STAGING_BORDER_RADIUS = 500;   // diameter 1000 untuk staging test

    private World resourceWorld;
    private World stagingWorld;
    private World terraformStagingWorld;
    private final AtomicBoolean resetInProgress = new AtomicBoolean(false);

    public ResourceWorldManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        resourceWorld = Bukkit.getWorld(RESOURCE_WORLD_NAME);
        if (resourceWorld == null) {
            log.info("[ResourceWorld] Memuat atau membuat dunia tambang: " + RESOURCE_WORLD_NAME);
            WorldCreator creator = new WorldCreator(RESOURCE_WORLD_NAME);
            creator.environment(World.Environment.NORMAL);
            if (Bukkit.getPluginManager().getPlugin("TerraformGenerator") != null) {
                creator.generator("TerraformGenerator");
            }
            resourceWorld = creator.createWorld();
        }

        if (resourceWorld != null) {
            // Pasang world border di dunia tambang
            WorldBorder border = resourceWorld.getWorldBorder();
            border.setCenter(0, 0);
            border.setSize(RESOURCE_BORDER_RADIUS * 2);
            log.info("[ResourceWorld] Dunia tambang '" + RESOURCE_WORLD_NAME + "' siap! Border: " + (RESOURCE_BORDER_RADIUS * 2) + " blok.");
        } else {
            log.severe("[ResourceWorld] Gagal memuat dunia tambang '" + RESOURCE_WORLD_NAME + "'!");
        }

        if (plugin.getConfig().getBoolean("world.terraform-staging.auto-create", true)
            && Bukkit.getWorld(TERRAFORM_STAGING_WORLD_NAME) == null
            && Bukkit.getPluginManager().getPlugin("TerraformGenerator") != null) {
            long seed = plugin.getConfig().getLong("world.terraform-staging.seed", "KalaTerraform2026".hashCode());
            createTerraformStagingWorld(null, seed);
        }
    }

    public World getResourceWorld() {
        if (resourceWorld == null) {
            resourceWorld = Bukkit.getWorld(RESOURCE_WORLD_NAME);
        }
        return resourceWorld;
    }

    public boolean isResourceWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(RESOURCE_WORLD_NAME);
    }

    public boolean isResetInProgress() {
        return resetInProgress.get();
    }

    /**
     * Teleport pemain ke dunia tambang.
     */
    public void teleportToResource(Player player) {
        if (isResetInProgress()) {
            player.sendMessage(Component.text("Dunia tambang sedang direset. Coba lagi beberapa saat lagi.", NamedTextColor.RED));
            return;
        }
        World resWorld = getResourceWorld();
        if (resWorld == null) {
            player.sendMessage(Component.text("Dunia tambang sedang tidak tersedia!", NamedTextColor.RED));
            return;
        }

        if (player.getWorld().equals(resWorld)) {
            player.sendMessage(Component.text("Kamu sudah berada di Dunia Tambang! Gunakan /tambang pulang untuk kembali.", NamedTextColor.YELLOW));
            return;
        }

        Location spawn = resWorld.getSpawnLocation();
        int safeY = resWorld.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ()) + 1;
        Location safeLoc = new Location(resWorld, spawn.getX(), safeY, spawn.getZ(), spawn.getYaw(), spawn.getPitch());

        player.teleport(safeLoc);
        player.playSound(safeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        Title title = Title.title(
            Component.text("DUNIA TAMBANG", NamedTextColor.GOLD),
            Component.text("Eksploitasi sumber daya bebas tanpa merusak kampung", NamedTextColor.GRAY),
            Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))
        );
        player.showTitle(title);
        player.sendMessage(Component.text("⛏ Berhasil tiba di Dunia Tambang! Gunakan /tambang pulang untuk kembali ke Labuhan Awal.", NamedTextColor.GREEN));
    }

    /**
     * Teleport pemain kembali ke dunia utama (Labuhan Awal).
     */
    public void teleportToMain(Player player) {
        World mainWorld = Bukkit.getWorld(plugin.getKalaConfig().getSpawnWorld());
        if (mainWorld == null) {
            mainWorld = Bukkit.getWorlds().get(0);
        }

        if (player.getWorld().equals(mainWorld)) {
            player.sendMessage(Component.text("Kamu sudah berada di Dunia Utama!", NamedTextColor.YELLOW));
            return;
        }

        Location spawn = mainWorld.getSpawnLocation();
        player.teleport(spawn);
        player.playSound(spawn, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

        Title title = Title.title(
            Component.text("LABUHAN AWAL", NamedTextColor.AQUA),
            Component.text("Selamat datang kembali di pemukiman warga", NamedTextColor.GRAY),
            Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))
        );
        player.showTitle(title);
        player.sendMessage(Component.text("🏛 Selamat datang kembali di Labuhan Awal!", NamedTextColor.AQUA));
    }

    /**
     * Reset aman dunia tambang:
     * 1. Evakuasi pemain ke main world
     * 2. Unload kala_resource
     * 3. Hapus data region/chunk
     * 4. Re-generate world bersih
     */
    public boolean resetResourceWorld(CommandSender sender, boolean confirmed) {
        if (!confirmed) {
            sender.sendMessage(Component.text("Reset membuang semua resource world. Ketik /ka resource reset confirm untuk melanjutkan.", NamedTextColor.RED));
            return false;
        }
        if (!resetInProgress.compareAndSet(false, true)) {
            sender.sendMessage(Component.text("Reset dunia tambang sudah berjalan. Tidak ada reset kedua yang dijalankan.", NamedTextColor.YELLOW));
            return false;
        }

        World target = getResourceWorld();
        if (target == null) {
            sender.sendMessage(Component.text("Dunia tambang tidak ditemukan!", NamedTextColor.RED));
            resetInProgress.set(false);
            return false;
        }

        // SAFETY GATE KETAT: JANGAN PERNAH RESET KALA_MAIN
        if (!target.getName().equals(RESOURCE_WORLD_NAME)) {
            sender.sendMessage(Component.text("FATAL: Target bukan dunia tambang! Pembatalan darurat.", NamedTextColor.DARK_RED));
            log.severe("PERCOBAAN RESET PADA WORLD ILEGAL: " + target.getName());
            resetInProgress.set(false);
            return false;
        }

        World mainWorld = Bukkit.getWorld(plugin.getKalaConfig().getSpawnWorld());
        if (mainWorld == null) mainWorld = Bukkit.getWorlds().get(0);
        final Location mainSpawn = mainWorld.getSpawnLocation();

        final File worldDir = target.getWorldFolder();
        if (!isSafeResourceWorldFolder(worldDir)) {
            sender.sendMessage(Component.text("FATAL: path dunia tambang tidak lolos validasi keselamatan.", NamedTextColor.DARK_RED));
            log.severe("RESET DIBATALKAN: path world tidak aman: " + worldDir);
            resetInProgress.set(false);
            return false;
        }

        // Broadcast pengumuman evakuasi
        Bukkit.broadcast(Component.text("§8[§6⛏ Tambang§8] §cPERINGATAN: Pembaruan dunia tambang sedang berjalan! Seluruh pemain dievakuasi..."));

        // Evakuasi semua pemain
        for (Player p : target.getPlayers()) {
            p.teleport(mainSpawn);
            p.playSound(mainSpawn, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            p.sendMessage(Component.text("§c[!] Kamu telah dievakuasi ke Labuhan Awal karena dunia tambang sedang direset.", NamedTextColor.YELLOW));
        }

        // Jadwalkan unload dan hapus region pada tick berikutnya
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                boolean unloaded = Bukkit.unloadWorld(target, false);
                if (!unloaded) {
                    throw new IOException("world tidak dapat di-unload");
                }

                deleteResettableWorldData(worldDir.toPath());

                WorldCreator creator = new WorldCreator(RESOURCE_WORLD_NAME);
                creator.environment(World.Environment.NORMAL);
                if (Bukkit.getPluginManager().getPlugin("TerraformGenerator") != null) {
                    creator.generator("TerraformGenerator");
                }
                resourceWorld = creator.createWorld();
                if (resourceWorld == null) {
                    throw new IOException("world tidak dapat dibuat ulang");
                }

                WorldBorder border = resourceWorld.getWorldBorder();
                border.setCenter(0, 0);
                border.setSize(RESOURCE_BORDER_RADIUS * 2);
                writeResetAudit("SUCCESS", "resource world dibuat ulang");
                Bukkit.broadcast(Component.text("§8[§6⛏ Tambang§8] §aDunia tambang nusantara telah selesai direset dan diperbarui! Sumber daya alam siap dipanen kembali."));
                sender.sendMessage(Component.text("Reset dunia tambang berhasil dilaksanakan!", NamedTextColor.GREEN));
            } catch (Exception exception) {
                writeResetAudit("FAILURE", exception.getMessage());
                log.severe("Reset dunia tambang gagal: " + exception.getMessage());
                sender.sendMessage(Component.text("Reset dunia tambang gagal. Periksa log KalaCore sebelum mencoba kembali.", NamedTextColor.RED));
            } finally {
                resetInProgress.set(false);
            }
        }, 40L); // 2 detik delay untuk memastikan semua chunk tersimpan aman

        return true;
    }

    public World getStagingWorld() {
        if (stagingWorld == null) {
            stagingWorld = Bukkit.getWorld(STAGING_WORLD_NAME);
        }
        return stagingWorld;
    }

    public boolean isStagingWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(STAGING_WORLD_NAME);
    }

    public World getTerraformStagingWorld() {
        if (terraformStagingWorld == null) {
            terraformStagingWorld = Bukkit.getWorld(TERRAFORM_STAGING_WORLD_NAME);
        }
        return terraformStagingWorld;
    }

    public boolean isTerraformStagingWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(TERRAFORM_STAGING_WORLD_NAME);
    }

    /**
     * Membuat staging test world kala_resource_test secara terisolasi.
     */
    public boolean createStagingWorld(CommandSender sender, long seed) {
        if (Bukkit.getWorld(STAGING_WORLD_NAME) != null) {
            if (sender != null) sender.sendMessage(Component.text("Staging world '" + STAGING_WORLD_NAME + "' sudah aktif!", NamedTextColor.YELLOW));
            return false;
        }

        if (sender != null) sender.sendMessage(Component.text("Membuat staging world '" + STAGING_WORLD_NAME + "' dengan seed: " + seed + "...", NamedTextColor.GRAY));
        WorldCreator creator = new WorldCreator(STAGING_WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.seed(seed);
        if (Bukkit.getPluginManager().getPlugin("TerraformGenerator") != null) {
            creator.generator("TerraformGenerator");
        }
        stagingWorld = creator.createWorld();

        if (stagingWorld != null) {
            WorldBorder border = stagingWorld.getWorldBorder();
            border.setCenter(0, 0);
            border.setSize(STAGING_BORDER_RADIUS * 2);
            if (sender != null) sender.sendMessage(Component.text("§a[✓] Staging world '" + STAGING_WORLD_NAME + "' berhasil dibuat! Border: " + (STAGING_BORDER_RADIUS * 2) + " blok."));
            log.info("[ResourceWorld] Staging world '" + STAGING_WORLD_NAME + "' berhasil dibuat.");
            return true;
        } else {
            if (sender != null) sender.sendMessage(Component.text("§c[!] Gagal membuat staging world!", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Membuat world staging khusus TerraformGenerator. World ini tidak pernah
     * dipakai sebagai world utama atau resource production.
     */
    public boolean createTerraformStagingWorld(CommandSender sender, long seed) {
        if (Bukkit.getPluginManager().getPlugin("TerraformGenerator") == null) {
            if (sender != null) sender.sendMessage(Component.text("TerraformGenerator belum aktif. Instal plugin lalu restart server.", NamedTextColor.RED));
            return false;
        }
        if (Bukkit.getWorld(TERRAFORM_STAGING_WORLD_NAME) != null) {
            if (sender != null) sender.sendMessage(Component.text("Terraform staging world sudah aktif.", NamedTextColor.YELLOW));
            return false;
        }

        if (sender != null) sender.sendMessage(Component.text("Membuat Terraform staging world dengan seed: " + seed + "...", NamedTextColor.GRAY));
        log.info("[ResourceWorld] Membuat Terraform staging world '" + TERRAFORM_STAGING_WORLD_NAME + "' dengan seed: " + seed);
        WorldCreator creator = new WorldCreator(TERRAFORM_STAGING_WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.seed(seed);
        creator.generator("TerraformGenerator");
        terraformStagingWorld = creator.createWorld();

        if (terraformStagingWorld == null) {
            if (sender != null) sender.sendMessage(Component.text("Gagal membuat Terraform staging world. Periksa log server.", NamedTextColor.RED));
            return false;
        }

        WorldBorder border = terraformStagingWorld.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(STAGING_BORDER_RADIUS * 2);
        log.info("[ResourceWorld] Terraform staging world '" + TERRAFORM_STAGING_WORLD_NAME + "' berhasil dibuat.");
        if (sender != null) sender.sendMessage(Component.text("Terraform staging world siap. Border: " + (STAGING_BORDER_RADIUS * 2) + " blok.", NamedTextColor.GREEN));
        return true;
    }

    public void teleportToTerraformStaging(Player player) {
        World world = getTerraformStagingWorld();
        if (world == null) {
            player.sendMessage(Component.text("Terraform staging belum dibuat. Jalankan /ka terraform create dahulu.", NamedTextColor.RED));
            return;
        }
        Location spawn = world.getSpawnLocation();
        int safeY = world.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ()) + 1;
        player.teleport(new Location(world, spawn.getX(), safeY, spawn.getZ(), spawn.getYaw(), spawn.getPitch()));
        player.sendMessage(Component.text("Tiba di Terraform staging: " + TERRAFORM_STAGING_WORLD_NAME, NamedTextColor.GREEN));
    }

    /**
     * Menghapus staging world secara aman.
     */
    public boolean deleteStagingWorld(CommandSender sender) {
        World target = getStagingWorld();
        if (target == null) {
            if (sender != null) sender.sendMessage(Component.text("Staging world '" + STAGING_WORLD_NAME + "' tidak aktif atau tidak ditemukan.", NamedTextColor.YELLOW));
            return false;
        }

        // SAFETY: Jangan pernah hapus kala_main atau kala_resource
        if (!target.getName().equals(STAGING_WORLD_NAME)) {
            if (sender != null) sender.sendMessage(Component.text("FATAL: Target bukan staging world!", NamedTextColor.DARK_RED));
            return false;
        }

        World mainWorld = Bukkit.getWorld(plugin.getKalaConfig().getSpawnWorld());
        if (mainWorld == null) mainWorld = Bukkit.getWorlds().get(0);
        Location mainSpawn = mainWorld.getSpawnLocation();

        for (Player p : target.getPlayers()) {
            p.teleport(mainSpawn);
            p.sendMessage(Component.text("§e[!] Staging world ditutup, kamu dipindahkan ke Labuhan Awal."));
        }

        File folder = target.getWorldFolder();
        boolean unloaded = Bukkit.unloadWorld(target, false);
        if (!unloaded) {
            if (sender != null) sender.sendMessage(Component.text("Gagal unload staging world.", NamedTextColor.RED));
            return false;
        }

        deleteDirectory(folder);
        stagingWorld = null;
        if (sender != null) sender.sendMessage(Component.text("§a[✓] Staging world '" + STAGING_WORLD_NAME + "' berhasil dihapus.", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Teleport pemain ke staging world.
     */
    public void teleportToStaging(Player player) {
        World sWorld = getStagingWorld();
        if (sWorld == null) {
            player.sendMessage(Component.text("Staging world belum dibuat! Jalankan /ka staging create dahulu.", NamedTextColor.RED));
            return;
        }

        Location spawn = sWorld.getSpawnLocation();
        int safeY = sWorld.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ()) + 1;
        Location safeLoc = new Location(sWorld, spawn.getX(), safeY, spawn.getZ(), spawn.getYaw(), spawn.getPitch());
        player.teleport(safeLoc);
        player.sendMessage(Component.text("§aTiba di Staging Test World: " + STAGING_WORLD_NAME, NamedTextColor.GREEN));
    }

    private void deleteDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    private boolean isSafeResourceWorldFolder(File worldDir) {
        try {
            Path path = worldDir.toPath().toRealPath();
            return path.getFileName() != null
                && RESOURCE_WORLD_NAME.equals(path.getFileName().toString())
                && !path.equals(plugin.getServer().getWorldContainer().toPath().toRealPath());
        } catch (IOException exception) {
            return false;
        }
    }

    private void deleteResettableWorldData(Path worldPath) throws IOException {
        for (String directory : List.of("region", "entities", "poi")) {
            Path target = worldPath.resolve(directory).normalize();
            if (!target.getParent().equals(worldPath)) {
                throw new IOException("target reset berada di luar world: " + target);
            }
            deleteDirectory(target.toFile());
        }
    }

    private void writeResetAudit(String outcome, String detail) {
        File auditFile = new File(plugin.getDataFolder(), "resource-reset-audit.log");
        String safeDetail = detail == null ? "" : detail.replace('\n', ' ').replace('\r', ' ');
        String line = Instant.now() + " | " + outcome + " | " + RESOURCE_WORLD_NAME + " | " + safeDetail + System.lineSeparator();
        try {
            Files.writeString(auditFile.toPath(), line, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException exception) {
            log.warning("Gagal menulis audit reset: " + exception.getMessage());
        }
    }
}
