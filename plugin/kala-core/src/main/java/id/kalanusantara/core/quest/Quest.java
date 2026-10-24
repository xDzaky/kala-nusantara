package id.kalanusantara.core.quest;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Quest — model data satu quest
 */
public class Quest {

    private final String id;
    private final String title;
    private final String description;
    private final QuestType type;
    private final List<QuestStep> steps;
    private final double rewardRupiah;
    private final List<String> rewardItems;
    private final String nextQuestId;

    public Quest(String id, String title, String description, QuestType type,
                 List<QuestStep> steps, double rewardRupiah,
                 List<String> rewardItems, String nextQuestId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.steps = steps;
        this.rewardRupiah = rewardRupiah;
        this.rewardItems = rewardItems;
        this.nextQuestId = nextQuestId;
    }

    public static Quest fromConfig(String id, FileConfiguration cfg) {
        String title = cfg.getString("title", id);
        String desc = cfg.getString("description", "");
        QuestType type = QuestType.valueOf(cfg.getString("type", "STORY").toUpperCase());
        double reward = cfg.getDouble("reward.rupiah", 0);
        List<String> rewardItems = cfg.getStringList("reward.items");
        String nextId = cfg.getString("next-quest", null);

        List<QuestStep> steps = new ArrayList<>();
        ConfigurationSection stepsSec = cfg.getConfigurationSection("steps");
        if (stepsSec != null) {
            for (String key : stepsSec.getKeys(false)) {
                ConfigurationSection s = stepsSec.getConfigurationSection(key);
                if (s == null) continue;
                steps.add(new QuestStep(
                    Integer.parseInt(key),
                    s.getString("description", ""),
                    s.getString("action", "MANUAL"),
                    s.getString("target", ""),
                    s.getInt("count", 1)
                ));
            }
        }

        return new Quest(id, title, desc, type, steps, reward, rewardItems, nextId);
    }

    // === Getters ===
    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getDescription()     { return description; }
    public QuestType getType()         { return type; }
    public List<QuestStep> getSteps()  { return steps; }
    public double getRewardRupiah()    { return rewardRupiah; }
    public List<String> getRewardItems() { return rewardItems; }
    public String getNextQuestId()     { return nextQuestId; }
    public int getTotalSteps()         { return steps.size(); }

    public enum QuestType {
        TUTORIAL, STORY, DAILY, PROFESSION, KAMPUNG
    }
}
