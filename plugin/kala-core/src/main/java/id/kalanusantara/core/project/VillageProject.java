package id.kalanusantara.core.project;

import org.bukkit.Material;

/**
 * VillageProject — model data untuk proyek gotong royong kampung
 */
public class VillageProject {

    private final String id;
    private final String name;
    private final String description;
    private final Material targetMaterial;
    private final int targetAmount;
    private int currentAmount;
    private String status; // ACTIVE, COMPLETED
    private Long completedAt;

    public VillageProject(String id, String name, String description, Material targetMaterial, int targetAmount, int currentAmount, String status, Long completedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.targetMaterial = targetMaterial;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.status = status;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Material getTargetMaterial() { return targetMaterial; }
    public int getTargetAmount() { return targetAmount; }
    public int getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(int currentAmount) { this.currentAmount = currentAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status) || currentAmount >= targetAmount;
    }

    public double getProgressPercentage() {
        if (targetAmount <= 0) return 100.0;
        return Math.min(100.0, ((double) currentAmount / targetAmount) * 100.0);
    }

    public String getProgressBar(int length) {
        int filled = (int) Math.round((getProgressPercentage() / 100.0) * length);
        StringBuilder sb = new StringBuilder();
        sb.append("§a");
        for (int i = 0; i < filled; i++) sb.append("█");
        sb.append("§7");
        for (int i = filled; i < length; i++) sb.append("░");
        return sb.toString();
    }
}
