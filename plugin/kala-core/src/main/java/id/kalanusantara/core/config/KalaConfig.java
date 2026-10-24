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

    // === Volcano Arena & Timeline (V7) ===
    private String volcanoWorld;
    private double volcanoCenterX, volcanoCenterY, volcanoCenterZ;
    private double volcanoArenaRadius;
    private double spawnSafeZoneRadius;
    private int puncakDurationSec;

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

        // Volcano Arena & Timeline (V7)
        this.volcanoWorld     = config.getString("volcano.world", "kala_main");
        this.volcanoCenterX   = config.getDouble("volcano.center.x", 0.0);
        this.volcanoCenterY   = config.getDouble("volcano.center.y", 110.0);
        this.volcanoCenterZ   = config.getDouble("volcano.center.z", -350.0);
        this.volcanoArenaRadius = config.getDouble("volcano.arena-radius", 65.0);
        this.spawnSafeZoneRadius = config.getDouble("volcano.spawn-safe-zone-radius", 120.0);
        this.puncakDurationSec = config.getInt("volcano.timeline.puncak-duration-sec", 600);

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

    public String getVolcanoWorld()     { return volcanoWorld; }
    public double getVolcanoCenterX()   { return volcanoCenterX; }
    public double getVolcanoCenterY()   { return volcanoCenterY; }
    public double getVolcanoCenterZ()   { return volcanoCenterZ; }
    public double getVolcanoArenaRadius() { return volcanoArenaRadius; }
    public double getSpawnSafeZoneRadius() { return spawnSafeZoneRadius; }
    public int getPuncakDurationSec()   { return puncakDurationSec; }

    public org.bukkit.Location getVolcanoCenter(org.bukkit.World fallbackWorld) {
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(volcanoWorld);
        if (w == null) w = fallbackWorld;
        if (w == null) return null;
        return new org.bukkit.Location(w, volcanoCenterX, volcanoCenterY, volcanoCenterZ);
    }

    public FileConfiguration getRaw()   { return config; }
}
