package id.kalanusantara.core.quest;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * QuestProgressTracker — menyimpan dan memajukan progres quest pemain.
 *
 * Progress disimpan di tabel quest_progress (SQLite).
 * Cache in-memory (step + count) untuk pemain online.
 *
 * Format cache key: uuid + ":" + questId
 * Cache value: {step, count} sebagai int[]
 */
public class QuestProgressTracker {

    private final KalaCorePlugin plugin;
    private final Logger log;

    // Cache: uuid -> Map<questId, int[]{currentStep, currentCount}>
    private final Map<UUID, Map<String, int[]>> cache = new ConcurrentHashMap<>();

    public QuestProgressTracker(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    // ─────────────────────────────────────────────────────────
    // API utama
    // ─────────────────────────────────────────────────────────

    /**
     * Dipanggil saat pemain login — muat progres aktif dari DB ke cache.
     */
    public void onLogin(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = player.getUniqueId();
            Map<String, int[]> playerCache = new ConcurrentHashMap<>();
            try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                    "SELECT quest_id, step, step_count FROM quest_progress WHERE uuid=? AND status='ACTIVE'")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String qid = rs.getString("quest_id");
                    int step = rs.getInt("step");
                    int cnt = rs.getInt("step_count");
                    playerCache.put(qid, new int[]{step, cnt});
                }
            } catch (SQLException e) {
                log.warning("[QuestTracker] Gagal load progres " + player.getName() + ": " + e.getMessage());
            }
            cache.put(uuid, playerCache);
        });
    }

    /**
     * Dipanggil saat pemain logout — flush cache ke DB.
     */
    public void onLogout(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, int[]> playerCache = cache.remove(uuid);
        if (playerCache == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            flushToDB(uuid, playerCache);
        });
    }

    /**
     * Mulai quest untuk pemain (auto-dipanggil dari NPC atau command).
     */
    public boolean startQuest(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        Optional<Quest> opt = plugin.getQuestManager().getQuest(questId);
        if (opt.isEmpty()) {
            log.warning("[QuestTracker] Quest tidak ditemukan: " + questId);
            return false;
        }

        // Cek apakah sudah ada progres
        Map<String, int[]> playerCache = getOrCreateCache(uuid);
        if (playerCache.containsKey(questId)) return false; // sudah aktif

        // Cek status di DB (mungkin sudah selesai)
        if (isCompletedInDB(uuid, questId)) return false;

        // Mulai dari step 0, count 0
        playerCache.put(questId, new int[]{0, 0});

        // Simpan ke DB
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                    "INSERT OR IGNORE INTO quest_progress(uuid, quest_id, step, step_count, status, started_at) VALUES(?,?,0,0,'ACTIVE',?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, questId);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[QuestTracker] Gagal simpan start quest: " + e.getMessage());
            }
        });

        Quest quest = opt.get();
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ QUEST DIMULAI: ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(quest.getTitle(), NamedTextColor.YELLOW)));
        if (!quest.getSteps().isEmpty()) {
            QuestStep firstStep = quest.getSteps().get(0);
            player.sendMessage(Component.text("  ➤ ", NamedTextColor.AQUA)
                .append(Component.text(firstStep.getDescription(), NamedTextColor.WHITE)));
        }
        player.sendMessage(Component.empty());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        return true;
    }

    /**
     * Dipanggil dari event listener — progress action tertentu.
     * action: "COLLECT", "CRAFT", "TALK", "MANUAL"
     * target: material/npc name (case-insensitive match)
     * amount: berapa unit sekarang (biasanya 1)
     */
    public void progress(Player player, String action, String target, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, int[]> playerCache = cache.get(uuid);
        if (playerCache == null || playerCache.isEmpty()) return;

        // Loop semua quest aktif pemain ini
        for (Map.Entry<String, int[]> entry : playerCache.entrySet()) {
            String questId = entry.getKey();
            int[] state = entry.getValue(); // [step, count]

            Optional<Quest> qOpt = plugin.getQuestManager().getQuest(questId);
            if (qOpt.isEmpty()) continue;

            Quest quest = qOpt.get();
            List<QuestStep> steps = quest.getSteps();
            if (steps.isEmpty()) continue;

            int currentStepIdx = state[0];
            if (currentStepIdx >= steps.size()) continue;

            QuestStep currentStep = steps.get(currentStepIdx);

            // Cek action cocok (case insensitive)
            if (!currentStep.getAction().equalsIgnoreCase(action)) continue;

            // Cek target cocok (case insensitive)
            if (!currentStep.getTarget().equalsIgnoreCase(target)) continue;

            // Tambah count
            state[1] += amount;

            int needed = currentStep.getCount();
            int got = state[1];

            // Notifikasi progres
            if (got < needed) {
                player.sendActionBar(Component.text(
                    "§e" + currentStep.getDescription() + " §7[" + got + "/" + needed + "]"));
            }

            // Step selesai?
            if (got >= needed) {
                state[0]++; // next step
                state[1] = 0; // reset count

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);

                if (state[0] >= steps.size()) {
                    // Quest selesai!
                    completeQuest(player, questId, quest);
                    playerCache.remove(questId);
                } else {
                    // Step berikutnya
                    QuestStep nextStep = steps.get(state[0]);
                    player.sendMessage(Component.text("  ✔ ", NamedTextColor.GREEN)
                        .append(Component.text(currentStep.getDescription(), NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH)));
                    player.sendMessage(Component.text("  ➤ ", NamedTextColor.AQUA)
                        .append(Component.text(nextStep.getDescription(), NamedTextColor.WHITE)));
                }
            }
        }
    }

    /**
     * Manual progress — untuk action MANUAL/TALK dipanggil dari luar.
     */
    public void progressManual(Player player, String questId, String targetAction) {
        progress(player, "MANUAL", targetAction, 1);
        progress(player, "TALK", targetAction, 1);
    }

    // ─────────────────────────────────────────────────────────
    // Quest completion
    // ─────────────────────────────────────────────────────────

    private void completeQuest(Player player, String questId, Quest quest) {
        UUID uuid = player.getUniqueId();

        // Tampilkan pesan
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  ✦ QUEST SELESAI! ", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(quest.getTitle(), NamedTextColor.YELLOW)));
        if (quest.getRewardRupiah() > 0) {
            player.sendMessage(Component.text("  + Rp " + (int)quest.getRewardRupiah() + " Rupiah Kala", NamedTextColor.GREEN));
        }
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.empty());

        // Efek suara
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);

        // Beri reward (async-safe: schedule on main thread karena inventory)
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Rupiah reward
            if (quest.getRewardRupiah() > 0) {
                plugin.getEconomy().deposit(uuid, quest.getRewardRupiah(), "Quest: " + questId);
            }

            // Item rewards
            for (String itemStr : quest.getRewardItems()) {
                try {
                    String[] parts = itemStr.split(":");
                    org.bukkit.Material mat = org.bukkit.Material.valueOf(parts[0]);
                    int qty = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    ItemStack item = new ItemStack(mat, qty);
                    // Drop jika inventory penuh
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    leftover.values().forEach(li -> player.getWorld().dropItemNaturally(player.getLocation(), li));
                } catch (Exception e) {
                    log.warning("[QuestTracker] Gagal beri item reward " + itemStr + ": " + e.getMessage());
                }
            }

            // Mark tutorial done jika quest tutorial
            if (quest.getType() == Quest.QuestType.TUTORIAL) {
                plugin.getIdentityManager().get(uuid).ifPresent(p -> p.setTutorialDone(true));
            }

            // Auto-start next quest
            if (quest.getNextQuestId() != null && !quest.getNextQuestId().isEmpty()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) startQuest(player, quest.getNextQuestId());
                }, 60L); // 3 detik delay
            }
        });

        // Simpan ke DB
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                    "UPDATE quest_progress SET status='DONE', step=?, step_count=0, completed_at=? WHERE uuid=? AND quest_id=?")) {
                ps.setInt(1, quest.getSteps().size());
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, uuid.toString());
                ps.setString(4, questId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[QuestTracker] Gagal mark quest DONE: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    // Query helpers
    // ─────────────────────────────────────────────────────────

    public boolean isQuestActive(UUID uuid, String questId) {
        Map<String, int[]> playerCache = cache.get(uuid);
        if (playerCache != null) return playerCache.containsKey(questId);
        return isActiveInDB(uuid, questId);
    }

    public boolean isQuestCompleted(UUID uuid, String questId) {
        return isCompletedInDB(uuid, questId);
    }

    public List<String> getActiveQuestIds(UUID uuid) {
        Map<String, int[]> playerCache = cache.get(uuid);
        if (playerCache != null) return new ArrayList<>(playerCache.keySet());
        return Collections.emptyList();
    }

    public int[] getStepState(UUID uuid, String questId) {
        Map<String, int[]> playerCache = cache.get(uuid);
        if (playerCache != null && playerCache.containsKey(questId)) {
            return playerCache.get(questId).clone();
        }
        return new int[]{0, 0};
    }

    // ─────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────

    private Map<String, int[]> getOrCreateCache(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    }

    private boolean isCompletedInDB(UUID uuid, String questId) {
        try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                "SELECT 1 FROM quest_progress WHERE uuid=? AND quest_id=? AND status='DONE'")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, questId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isActiveInDB(UUID uuid, String questId) {
        try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                "SELECT 1 FROM quest_progress WHERE uuid=? AND quest_id=? AND status='ACTIVE'")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, questId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private void flushToDB(UUID uuid, Map<String, int[]> playerCache) {
        for (Map.Entry<String, int[]> entry : playerCache.entrySet()) {
            String questId = entry.getKey();
            int[] state = entry.getValue();
            try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                    "UPDATE quest_progress SET step=?, step_count=? WHERE uuid=? AND quest_id=? AND status='ACTIVE'")) {
                ps.setInt(1, state[0]);
                ps.setInt(2, state[1]);
                ps.setString(3, uuid.toString());
                ps.setString(4, questId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[QuestTracker] Gagal flush progres " + uuid + "/" + questId + ": " + e.getMessage());
            }
        }
    }
}
