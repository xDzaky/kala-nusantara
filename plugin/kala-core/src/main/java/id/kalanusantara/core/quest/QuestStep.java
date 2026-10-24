package id.kalanusantara.core.quest;

/**
 * QuestStep — satu langkah dalam quest
 */
public class QuestStep {

    private final int index;
    private final String description;
    private final String action;   // COLLECT, CRAFT, TALK, CLAIM, MANUAL, etc.
    private final String target;   // material/npc/quest-id target
    private final int count;       // berapa kali/banyak

    public QuestStep(int index, String description, String action, String target, int count) {
        this.index = index;
        this.description = description;
        this.action = action;
        this.target = target;
        this.count = count;
    }

    public int getIndex()         { return index; }
    public String getDescription() { return description; }
    public String getAction()     { return action; }
    public String getTarget()     { return target; }
    public int getCount()         { return count; }
}
