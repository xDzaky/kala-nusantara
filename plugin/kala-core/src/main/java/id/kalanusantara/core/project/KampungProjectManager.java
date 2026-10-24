package id.kalanusantara.core.project;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.journal.JurnalManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * KampungProjectManager — Mengelola proyek gotong royong warga kampung.
 *
 * Pemain menyumbangkan material (kayu, batu, hasil panen) bersama-sama.
 * Saat selesai, seluruh warga merayakan dan mendapat hadiah.
 */
public class KampungProjectManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final Map<String, VillageProject> projects = new ConcurrentHashMap<>();

    public KampungProjectManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        loadProjectsFromDb();
        if (projects.isEmpty()) {
            seedDefaultProjects();
        }
        log.info("[KampungProject] Dimuat " + projects.size() + " proyek kampung gotong royong.");
    }

    private void seedDefaultProjects() {
        VillageProject[] defaults = {
            new VillageProject(
                "lumbung_padi",
                "Lumbung Padi Bersama",
                "Mengumpulkan cadangan pangan gandum warga untuk ketahanan saat musim paceklik dan Kala.",
                Material.WHEAT,
                128,
                0,
                "ACTIVE",
                null
            ),
            new VillageProject(
                "dermaga_kayu",
                "Dermaga Pelabuhan Nusantara",
                "Merestorasi dermaga kayu labuhan awal agar perahu nelayan dan pedagang dapat bersandar.",
                Material.OAK_PLANKS,
                128,
                0,
                "ACTIVE",
                null
            ),
            new VillageProject(
                "benteng_batu",
                "Benteng Pertahanan Kampung",
                "Membangun dinding batu pelindung permukiman dari ancaman malam.",
                Material.COBBLESTONE,
                256,
                0,
                "ACTIVE",
                null
            )
        };

        for (VillageProject p : defaults) {
            saveProjectToDb(p);
            projects.put(p.getId(), p);
        }
    }

    private void loadProjectsFromDb() {
        Connection conn = plugin.getStorageManager().ensureConnection();
        if (conn == null) return;

        String sql = "SELECT id, name, description, target_material, target_amount, current_amount, status, completed_at FROM village_projects";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String desc = rs.getString("description");
                Material mat = Material.matchMaterial(rs.getString("target_material"));
                if (mat == null) mat = Material.OAK_LOG;
                int target = rs.getInt("target_amount");
                int current = rs.getInt("current_amount");
                String status = rs.getString("status");
                long completedAt = rs.getLong("completed_at");

                VillageProject project = new VillageProject(
                    id, name, desc, mat, target, current, status,
                    completedAt > 0 ? completedAt : null
                );
                projects.put(id, project);
            }
        } catch (SQLException e) {
            log.severe("Gagal memuat proyek kampung dari DB: " + e.getMessage());
        }
    }

    private void saveProjectToDb(VillageProject p) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getStorageManager().ensureConnection();
            if (conn == null) return;

            String sql = """
                INSERT INTO village_projects (id, name, description, target_material, target_amount, current_amount, status, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    current_amount = excluded.current_amount,
                    status = excluded.status,
                    completed_at = excluded.completed_at
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getId());
                ps.setString(2, p.getName());
                ps.setString(3, p.getDescription());
                ps.setString(4, p.getTargetMaterial().name());
                ps.setInt(5, p.getTargetAmount());
                ps.setInt(6, p.getCurrentAmount());
                ps.setString(7, p.getStatus());
                ps.setObject(8, p.getCompletedAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.severe("Gagal menyimpan proyek ke DB: " + e.getMessage());
            }
        });
    }

    /**
     * Setor material ke proyek kampung.
     */
    public boolean contribute(Player player, String projectId, int requestAmount) {
        VillageProject project = projects.get(projectId.toLowerCase());
        if (project == null) {
            player.sendMessage(Component.text("Proyek tidak ditemukan! Cek /proyek.", NamedTextColor.RED));
            return false;
        }

        if (project.isCompleted()) {
            player.sendMessage(Component.text("Proyek '" + project.getName() + "' sudah selesai dibangun!", NamedTextColor.YELLOW));
            return false;
        }

        Material requiredMat = project.getTargetMaterial();
        int available = countItem(player, requiredMat);
        if (available <= 0) {
            player.sendMessage(Component.text("Kamu tidak memiliki material " + requiredMat.name() + " di dalam tasmu!", NamedTextColor.RED));
            return false;
        }

        // Hitung sisa kebutuhan
        int remainingNeeded = project.getTargetAmount() - project.getCurrentAmount();
        int toContribute = Math.min(requestAmount > 0 ? requestAmount : available, available);
        toContribute = Math.min(toContribute, remainingNeeded);

        if (toContribute <= 0) {
            player.sendMessage(Component.text("Proyek ini sudah terpenuhi!", NamedTextColor.YELLOW));
            return false;
        }

        // Ambil item dari tas
        removeItem(player, requiredMat, toContribute);

        // Update jumlah proyek
        project.setCurrentAmount(project.getCurrentAmount() + toContribute);

        // Imbalan ekonomi: Rp 2.5 per item yang disumbangkan
        double reward = toContribute * 2.5;
        plugin.getEconomy().deposit(player.getUniqueId(), reward, "Kontribusi proyek " + project.getName());

        // Catat ke DB kontribusi
        final int finalContributed = toContribute;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getStorageManager().ensureConnection();
            if (conn != null) {
                String sql = "INSERT INTO project_contributions (project_id, uuid, amount, timestamp) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, project.getId());
                    ps.setString(2, player.getUniqueId().toString());
                    ps.setInt(3, finalContributed);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    log.warning("Gagal mencatat kontribusi: " + e.getMessage());
                }
            }
        });

        // Suara & pesan
        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.5f);
        player.sendMessage(Component.text("✦ Gotong Royong: Menyetor " + finalContributed + "x " + requiredMat.name() + " ke '" + project.getName() + "'!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("  Imbalan jasa: +Rp " + String.format("%.2f", reward) + " Rupiah Kala", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  Progress: " + project.getProgressBar(15) + " §e" + String.format("%.1f%%", project.getProgressPercentage()) + " (" + project.getCurrentAmount() + "/" + project.getTargetAmount() + ")"));

        // Cek apakah selesai
        if (project.getCurrentAmount() >= project.getTargetAmount()) {
            completeProject(project);
        } else {
            saveProjectToDb(project);
        }

        return true;
    }

    private void completeProject(VillageProject project) {
        project.setStatus("COMPLETED");
        project.setCompletedAt(System.currentTimeMillis());
        saveProjectToDb(project);

        // Broadcast selebrasi
        Title victoryTitle = Title.title(
            Component.text("PROYEK KAMPUNG SELESAI!", NamedTextColor.GOLD),
            Component.text(project.getName() + " telah rampung dibangun!", NamedTextColor.YELLOW),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(victoryTitle);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.sendMessage(Component.empty());
            p.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
            p.sendMessage(Component.text("🏛 Gotong Royong Nusantara Berhasil!", NamedTextColor.YELLOW));
            p.sendMessage(Component.text("Proyek '§e" + project.getName() + "§f' telah selesai berkat kerja keras bersama."));
            p.sendMessage(Component.text("Kemakmuran kampung meningkat!", NamedTextColor.GREEN));
            p.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
            p.sendMessage(Component.empty());

            // Tulis jurnal untuk semua pemain yang berpartisipasi
            plugin.getJurnalManager().addEntry(
                p,
                "proyek",
                "Pembangun Nusantara: " + project.getName(),
                "Bersama warga kampung berhasil menyelesaikan pembangunan " + project.getName() + ". Semangat gotong royong terus hidup di Nusantara!"
            );
        }
    }

    public void resetProject(String id) {
        VillageProject p = projects.get(id.toLowerCase());
        if (p != null) {
            p.setCurrentAmount(0);
            p.setStatus("ACTIVE");
            p.setCompletedAt(null);
            saveProjectToDb(p);
        }
    }

    public Collection<VillageProject> getAllProjects() {
        return Collections.unmodifiableCollection(projects.values());
    }

    public VillageProject getProject(String id) {
        return projects.get(id.toLowerCase());
    }

    private int countItem(Player player, Material mat) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItem(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == mat) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
        player.updateInventory();
    }
}
