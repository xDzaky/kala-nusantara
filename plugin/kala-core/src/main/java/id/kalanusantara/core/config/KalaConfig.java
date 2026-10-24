package id.kalanusantara.core.config;

import id.kalanusantara.core.KalaCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * KalaConfig — manajemen konfigurasi plugin
 */
public class KalaConfig {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private FileConfiguration config;

    // === Cached values ===
    private String defaultLocale;
    private boolean debugMode;
    private String spawnWorld;
    private double spawnX, spawnY, spawnZ;
    private double startingBalance;
    private String currencyName;
    private String currencySymbol;
    private boolean tutorialEnabled;
    private int worldBorderRadius;
    private String serverTheme;

    public KalaConfig(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Cache semua nilai
        this.defaultLocale    = config.getString("locale.default", "id");
        this.debugMode        = config.getBoolean("debug", false);
        this.spawnWorld       = config.getString("spawn.world", "kala_main");
        this.spawnX           = config.getDouble("spawn.x", 0);
        this.spawnY           = config.getDouble("spawn.y", 64);
        this.spawnZ           = config.getDouble("spawn.z", 0);
        this.startingBalance  = config.getDouble("economy.starting-balance", 500.0);
        this.currencyName     = config.getString("economy.currency-name", "Rupiah Kala");
        this.currencySymbol   = config.getString("economy.currency-symbol", "Rp");
        this.tutorialEnabled  = config.getBoolean("tutorial.enabled", true);
        this.worldBorderRadius = config.getInt("world.border-radius", 3000);
        this.serverTheme      = config.getString("server.theme", "nusantara");

        log.info("Konfigurasi dimuat: locale=" + defaultLocale + " debug=" + debugMode);
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    // === Getters ===
    public String getDefaultLocale()    { return defaultLocale; }
    public boolean isDebugMode()        { return debugMode; }
    public String getSpawnWorld()       { return spawnWorld; }
    public double getSpawnX()           { return spawnX; }
    public double getSpawnY()           { return spawnY; }
    public double getSpawnZ()           { return spawnZ; }
    public double getStartingBalance()  { return startingBalance; }
    public String getCurrencyName()     { return currencyName; }
    public String getCurrencySymbol()   { return currencySymbol; }
    public boolean isTutorialEnabled()  { return tutorialEnabled; }
    public int getWorldBorderRadius()   { return worldBorderRadius; }
    public String getServerTheme()      { return serverTheme; }

    public FileConfiguration getRaw()   { return config; }
}
