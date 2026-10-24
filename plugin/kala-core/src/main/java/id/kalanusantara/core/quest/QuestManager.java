package id.kalanusantara.core.quest;

import id.kalanusantara.core.KalaCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * QuestManager — memuat dan mengelola sistem quest YAML-driven
 */
public class QuestManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final Map<String, Quest> questRegistry = new LinkedHashMap<>();

    public QuestManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void loadQuests() {
        questRegistry.clear();

        // Salin quest bawaan dari resources
        String[] bundledQuests = {"tutorial_labuhan.yml", "quest_tukang_kayu.yml"};
        for (String qFile : bundledQuests) {
            File dest = new File(plugin.getDataFolder(), "quests/" + qFile);
            if (!dest.exists()) {
                InputStream is = plugin.getResource("quests/" + qFile);
                if (is != null) plugin.saveResource("quests/" + qFile, false);
            }
        }

        // Muat semua quest dari folder quests/
        File questDir = new File(plugin.getDataFolder(), "quests");
        File[] files = questDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            log.warning("Folder quests/ kosong");
            return;
        }

        for (File f : files) {
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                String id = cfg.getString("id", f.getName().replace(".yml", ""));
                Quest quest = Quest.fromConfig(id, cfg);
                questRegistry.put(id, quest);
                log.info("Quest dimuat: " + id + " (" + quest.getTitle() + ")");
            } catch (Exception e) {
                log.warning("Gagal memuat quest " + f.getName() + ": " + e.getMessage());
            }
        }

        log.info("Total quest dimuat: " + questRegistry.size());
    }

    public Optional<Quest> getQuest(String id) {
        return Optional.ofNullable(questRegistry.get(id));
    }

    public Collection<Quest> getAllQuests() {
        return Collections.unmodifiableCollection(questRegistry.values());
    }

    public Map<String, Quest> getRegistry() {
        return Collections.unmodifiableMap(questRegistry);
    }
}
