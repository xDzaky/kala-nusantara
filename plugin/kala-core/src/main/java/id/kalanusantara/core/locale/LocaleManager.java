package id.kalanusantara.core.locale;

import id.kalanusantara.core.KalaCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * LocaleManager — sistem multi-bahasa (ID & EN)
 * 
 * Penggunaan:
 *   localeManager.get(player, "welcome.message")
 *   localeManager.get("id", "welcome.message")
 */
public class LocaleManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final Map<String, FileConfiguration> locales = new HashMap<>();
    private String defaultLocale;

    public LocaleManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void load() {
        this.defaultLocale = plugin.getKalaConfig().getDefaultLocale();
        String[] availableLocales = {"id", "en"};

        for (String lang : availableLocales) {
            loadLocale(lang);
        }
        log.info("Locale dimuat: " + locales.keySet() + " (default: " + defaultLocale + ")");
    }

    private void loadLocale(String lang) {
        File localeFile = new File(plugin.getDataFolder(), "locale/" + lang + ".yml");

        // Salin dari resources jika belum ada
        if (!localeFile.exists()) {
            InputStream resource = plugin.getResource("locale/" + lang + ".yml");
            if (resource != null) {
                plugin.saveResource("locale/" + lang + ".yml", false);
            } else {
                log.warning("File locale tidak ditemukan di resources: locale/" + lang + ".yml");
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(localeFile);

        // Merge dengan bundled defaults
        InputStream defStream = plugin.getResource("locale/" + lang + ".yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }

        locales.put(lang, config);
    }

    /**
     * Ambil teks berdasarkan Player (nanti bisa personalisasi per-player)
     */
    public String get(Player player, String key, Object... args) {
        return get(defaultLocale, key, args);
    }

    /**
     * Ambil teks berdasarkan kode bahasa
     */
    public String get(String lang, String key, Object... args) {
        FileConfiguration config = locales.getOrDefault(lang, locales.get(defaultLocale));
        if (config == null) {
            return "§c[MISSING LOCALE: " + key + "]";
        }

        String text = config.getString(key);
        if (text == null) {
            // Fallback ke bahasa default
            config = locales.get(defaultLocale);
            if (config != null) text = config.getString(key);
        }
        if (text == null) {
            return "§c[MISSING: " + key + "]";
        }

        // Apply color codes dan placeholders
        text = text.replace("&", "§");
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                text = text.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return text;
    }

    public void reload() {
        locales.clear();
        load();
    }
}
