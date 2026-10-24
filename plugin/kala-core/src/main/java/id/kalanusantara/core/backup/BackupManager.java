package id.kalanusantara.core.backup;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * BackupManager — Mengelola snapshot dan backup database serta konfigurasi server.
 * Menjamin kesiapan operasional Fase 5 (Public Beta) dengan kemampuan disaster recovery.
 */
public class BackupManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final File backupDir;

    public BackupManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.backupDir = new File(plugin.getServer().getWorldContainer(), "backups");
    }

    public void initialize() {
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    /**
     * Membuat snapshot backup database dan konfigurasi penting secara asynchronous.
     */
    public void createBackup(CommandSender sender) {
        sender.sendMessage(Component.text("Memulai proses backup snapshot Kala Nusantara...", NamedTextColor.YELLOW));

        // Flush WAL checkpoint sebelum penyalinan file database
        plugin.getStorageManager().checkpointWal();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File zipFile = new File(backupDir, "kala_snapshot_" + timestamp + ".zip");

                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    // 1. Backup SQLite database
                    File dbFile = new File(plugin.getDataFolder(), "data/kala_data.db");
                    if (dbFile.exists()) {
                        addFileToZip(zos, dbFile, "kala_data.db");
                    }

                    // 2. Backup npcs.yml
                    File npcFile = new File(plugin.getDataFolder(), "npcs.yml");
                    if (npcFile.exists()) {
                        addFileToZip(zos, npcFile, "npcs.yml");
                    }

                    // 3. Backup config.yml
                    File configFile = new File(plugin.getDataFolder(), "config.yml");
                    if (configFile.exists()) {
                        addFileToZip(zos, configFile, "config.yml");
                    }
                }

                long sizeKb = zipFile.length() / 1024;
                log.info("[BackupManager] Snapshot berhasil dibuat: " + zipFile.getName() + " (" + sizeKb + " KB)");
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("✔ Backup snapshot berhasil disimpan: " + zipFile.getName() + " (" + sizeKb + " KB)", NamedTextColor.GREEN))
                );
            } catch (Exception e) {
                log.severe("[BackupManager] Gagal membuat backup: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("✘ Gagal membuat backup: " + e.getMessage(), NamedTextColor.RED))
                );
            }
        });
    }

    public void listBackups(CommandSender sender) {
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null || files.length == 0) {
            sender.sendMessage(Component.text("Belum ada backup snapshot yang tersimpan.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("─── Daftar Snapshot Backup ───", NamedTextColor.GOLD));
        for (File f : files) {
            long kb = f.length() / 1024;
            sender.sendMessage(Component.text("• " + f.getName() + " §7(" + kb + " KB)", NamedTextColor.YELLOW));
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }
}
