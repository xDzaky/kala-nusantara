package id.kalanusantara.core.backup;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * BackupManager — Mengelola snapshot konsisten SQLite & Full Disaster Recovery (Audit V8).
 *
 * Audit V8:
 * - Kategori 1: Database Snapshot (WAL checkpoint + atomic file copy di bawah dbLock).
 * - Kategori 2: Full Server Snapshot (DB, configs, manifest.json ber-checksum SHA-256).
 * - Retensi otomatis: Membatasi maksimal 10 file backup untuk mencegah kejenuhan storage.
 * - Fitur verifikasi integritas backup (verifyBackup).
 */
public class BackupManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final File backupDir;
    private final File dbBackupDir;

    private static final int MAX_RETAINED_BACKUPS = 10;

    public BackupManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.backupDir = new File(plugin.getServer().getWorldContainer(), "backups");
        this.dbBackupDir = new File(backupDir, "db");
    }

    public void initialize() {
        if (!backupDir.exists()) backupDir.mkdirs();
        if (!dbBackupDir.exists()) dbBackupDir.mkdirs();
    }

    /**
     * Snapshot Konsisten SQLite (Kategori 1).
     * Melakukan WAL checkpoint dan menyalin file database di bawah dbLock.
     */
    public void backupDatabase(CommandSender sender) {
        sender.sendMessage(Component.text("Memulai backup konsisten SQLite Kala Nusantara...", NamedTextColor.YELLOW));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File targetDb = new File(dbBackupDir, "kala_db_snapshot_" + timestamp + ".db");

                synchronized (plugin.getStorageManager().getDbLock()) {
                    plugin.getStorageManager().checkpointWal();
                    File sourceDb = new File(plugin.getDataFolder(), "data/kala_data.db");
                    if (!sourceDb.exists()) {
                        throw new FileNotFoundException("File database kala_data.db tidak ditemukan.");
                    }
                    Files.copy(sourceDb.toPath(), targetDb.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                pruneOldBackups(dbBackupDir, ".db");

                long kb = targetDb.length() / 1024;
                log.info("[BackupManager] Snapshot SQLite berhasil dibuat: " + targetDb.getName() + " (" + kb + " KB)");
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("✔ Database snapshot tersimpan: " + targetDb.getName() + " (" + kb + " KB)", NamedTextColor.GREEN))
                );
            } catch (Exception e) {
                log.severe("[BackupManager] Gagal backup database: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("✘ Gagal backup database: " + e.getMessage(), NamedTextColor.RED))
                );
            }
        });
    }

    /**
     * Full Server Snapshot (Kategori 2).
     * Termasuk database konsisten, config plugin, manifest ber-SHA256, dan npcs.yml.
     */
    public void backupFullServer(CommandSender sender) {
        sender.sendMessage(Component.text("Memulai pembuatan Full Backup Server Kala Nusantara...", NamedTextColor.YELLOW));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File zipFile = new File(backupDir, "kala_full_backup_" + timestamp + ".zip");

                // 1. Ambil salinan DB aman di bawah dbLock
                File tempDbCopy = File.createTempFile("kala_db_snap_", ".tmp");
                synchronized (plugin.getStorageManager().getDbLock()) {
                    plugin.getStorageManager().checkpointWal();
                    File sourceDb = new File(plugin.getDataFolder(), "data/kala_data.db");
                    if (sourceDb.exists()) {
                        Files.copy(sourceDb.toPath(), tempDbCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                // 2. Hitung SHA-256 database
                String dbSha256 = calculateSha256(tempDbCopy);

                // 3. Bangun manifest data
                String manifestContent = String.format("""
                    {
                      "server": "Kala Nusantara RPG",
                      "version": "%s",
                      "timestamp": "%s",
                      "engine": "PaperMC 26.3",
                      "database_sha256": "%s"
                    }
                    """,
                    plugin.getDescription().getVersion(),
                    timestamp,
                    dbSha256
                );

                // 4. Pack ke file ZIP
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    if (tempDbCopy.exists() && tempDbCopy.length() > 0) {
                        addFileToZip(zos, tempDbCopy, "kala_data.db");
                    }
                    File npcFile = new File(plugin.getDataFolder(), "npcs.yml");
                    if (npcFile.exists()) {
                        addFileToZip(zos, npcFile, "npcs.yml");
                    }
                    File configFile = new File(plugin.getDataFolder(), "config.yml");
                    if (configFile.exists()) {
                        addFileToZip(zos, configFile, "config.yml");
                    }
                    File propExample = new File(plugin.getServer().getWorldContainer(), "server.properties.example");
                    if (propExample.exists()) {
                        addFileToZip(zos, propExample, "server.properties.example");
                    }
                    // Entry manifest
                    zos.putNextEntry(new ZipEntry("manifest.json"));
                    zos.write(manifestContent.getBytes());
                    zos.closeEntry();
                } finally {
                    tempDbCopy.delete();
                }

                pruneOldBackups(backupDir, ".zip");

                long sizeKb = zipFile.length() / 1024;
                log.info("[BackupManager] Full Backup berhasil dibuat: " + zipFile.getName() + " (" + sizeKb + " KB)");
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("✔ Full backup server berhasil disimpan: " + zipFile.getName() + " (" + sizeKb + " KB)", NamedTextColor.GREEN))
                );
            } catch (Exception e) {
                log.severe("[BackupManager] Gagal membuat full backup: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(Component.text("✘ Gagal membuat full backup: " + e.getMessage(), NamedTextColor.RED))
                );
            }
        });
    }

    public void createBackup(CommandSender sender) {
        backupFullServer(sender);
    }

    public void listBackups(CommandSender sender) {
        File[] fullFiles = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));
        File[] dbFiles = dbBackupDir.listFiles((dir, name) -> name.endsWith(".db"));

        sender.sendMessage(Component.text("─── Daftar Snapshot Backup Kala Nusantara ───", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("• Full Server Backups (ZIP):", NamedTextColor.YELLOW));
        if (fullFiles != null && fullFiles.length > 0) {
            for (File f : fullFiles) {
                sender.sendMessage(Component.text("  - " + f.getName() + " §7(" + (f.length() / 1024) + " KB)", NamedTextColor.GRAY));
            }
        } else {
            sender.sendMessage(Component.text("  (Belum ada full backup ZIP)", NamedTextColor.DARK_GRAY));
        }

        sender.sendMessage(Component.text("• Database Snapshots (DB):", NamedTextColor.YELLOW));
        if (dbFiles != null && dbFiles.length > 0) {
            for (File f : dbFiles) {
                sender.sendMessage(Component.text("  - " + f.getName() + " §7(" + (f.length() / 1024) + " KB)", NamedTextColor.GRAY));
            }
        } else {
            sender.sendMessage(Component.text("  (Belum ada snapshot DB)", NamedTextColor.DARK_GRAY));
        }
    }

    /**
     * Memverifikasi integritas file backup ZIP (Audit V8)
     */
    public boolean verifyBackup(File zipFile) {
        if (!zipFile.exists() || zipFile.length() == 0) return false;
        try (ZipFile zf = new ZipFile(zipFile)) {
            boolean hasDb = zf.getEntry("kala_data.db") != null;
            boolean hasManifest = zf.getEntry("manifest.json") != null;
            return hasDb && hasManifest;
        } catch (IOException e) {
            log.warning("[BackupManager] File backup corrupt/gagal diverifikasi: " + zipFile.getName());
            return false;
        }
    }

    private void pruneOldBackups(File dir, String extension) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
        if (files != null && files.length > MAX_RETAINED_BACKUPS) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            int toDelete = files.length - MAX_RETAINED_BACKUPS;
            for (int i = 0; i < toDelete; i++) {
                files[i].delete();
            }
        }
    }

    private String calculateSha256(File file) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }
    }
}
