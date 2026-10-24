package id.kalanusantara.core.bootstrap;

import id.kalanusantara.core.KalaCorePlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.logging.Logger;

/**
 * Bootstrap — cek semua prasyarat sebelum plugin aktif penuh
 */
public class KalaBootstrap {

    public static boolean initialize(KalaCorePlugin plugin) {
        Logger log = plugin.getLogger();

        // Buat folder-folder yang dibutuhkan
        File dataFolder = plugin.getDataFolder();
        String[] dirs = {"locale", "quests", "data", "backup"};
        for (String dir : dirs) {
            File d = new File(dataFolder, dir);
            if (!d.exists() && !d.mkdirs()) {
                log.severe("Tidak bisa membuat folder: " + dir);
                return false;
            }
        }

        // Cek versi Paper (minimal Paper 1.21+)
        String version = Bukkit.getVersion();
        log.info("Server version: " + version);

        // Cek Geyser (opsional, log warning saja)
        if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") == null) {
            log.warning("Geyser tidak ditemukan — mode Bedrock tidak aktif");
        } else {
            log.info("Geyser terdeteksi — mode crossplay aktif");
        }

        // Cek PlaceholderAPI (opsional)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            log.info("PlaceholderAPI terdeteksi");
        } else {
            log.warning("PlaceholderAPI tidak ditemukan — beberapa placeholder tidak akan berfungsi");
        }

        // Cek Vault (opsional tapi direkomendasikan)
        if (Bukkit.getPluginManager().getPlugin("Vault") != null ||
            Bukkit.getPluginManager().getPlugin("VaultUnlocked") != null) {
            log.info("Vault/VaultUnlocked terdeteksi — integrasi ekonomi aktif");
        } else {
            log.warning("Vault tidak ditemukan — ekonomi eksternal tidak tersambung");
        }

        log.info("Bootstrap selesai ✓");
        return true;
    }
}
