package id.kalanusantara.core.notify;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KalaNotificationService — Mengatur antrean notifikasi, title, dan actionbar
 * sesuai UI Ownership Matrix (Kala Nusantara Expansion v4).
 *
 * Mencegah layar tertutup title bertumpuk (terutama di layar HP Bedrock/MCPE)
 * dan memprioritaskan peringatan sistem darurat serta event cerita.
 */
public class KalaNotificationService {

    public enum Priority {
        SYSTEM(4),        // Peringatan darurat, restart, reset dunia tambang
        STORY_EVENT(3),   // Erupsi Gunung Murka, pergantian Musim Kala
        QUEST(2),         // Milestone misi selesai, babak cerita baru
        TIPS_COSMETIC(1); // Penemuan fauna, tips pengembara, info gelar

        private final int level;
        Priority(int level) { this.level = level; }
        public int getLevel() { return level; }
    }

    private final KalaCorePlugin plugin;
    // Cooldown map: playerUUID:messageKey -> timestamp ms
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    // Notification preferences: playerUUID -> allowTips
    private final Map<UUID, Boolean> tipsEnabled = new ConcurrentHashMap<>();

    // Map untuk melacak level prioritas title aktif per pemain dan waktu berakhirnya
    private final Map<UUID, Priority> currentTitlePriority = new ConcurrentHashMap<>();
    private final Map<UUID, Long> currentTitleExpiry = new ConcurrentHashMap<>();

    public KalaNotificationService(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Kirim title ke pemain dengan validasi prioritas, preemption, dan cooldown (V7)
     */
    public void sendTitle(Player player, Priority priority, Component title, Component subtitle) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Cek apakah ada title berprioritas lebih tinggi yang masih aktif
        Priority activePri = currentTitlePriority.get(uuid);
        Long expiry = currentTitleExpiry.get(uuid);
        if (activePri != null && expiry != null && now < expiry) {
            if (priority.getLevel() < activePri.getLevel()) {
                // Alihkan pesan prioritas lebih rendah ke ActionBar agar tidak menimpa Story/System event
                sendActionBar(player, title);
                return;
            }
        }

        // Cek preferensi pemain untuk notifikasi tips/kosmetik
        if (priority == Priority.TIPS_COSMETIC && !isTipsEnabled(uuid)) {
            sendActionBar(player, title);
            return;
        }

        currentTitlePriority.put(uuid, priority);
        currentTitleExpiry.put(uuid, now + 3500L); // 3.5 detik durasi tayang

        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofMillis(3000),
                Duration.ofMillis(1000)
        );

        player.showTitle(Title.title(title, subtitle, times));
    }

    /**
     * Broadcast title ke seluruh pemain online dengan suara tematik
     */
    public void broadcastTitle(Priority priority, Component title, Component subtitle,
                               Sound sound, float volume, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendTitle(p, priority, title, subtitle);
            if (sound != null) {
                p.playSound(p.getLocation(), sound, volume, pitch);
            }
        }
    }

    /**
     * Kirim actionbar dengan deduplikasi 2.5 detik agar tidak spam
     */
    public void sendActionBar(Player player, Component message) {
        if (player == null || !player.isOnline()) return;

        String key = player.getUniqueId().toString() + ":" + message.toString().hashCode();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(key);

        if (last != null && (now - last) < 2500) {
            return; // Cegah spam actionbar identik dalam rentang 2.5 detik
        }

        cooldowns.put(key, now);
        player.sendActionBar(message);
    }

    /**
     * Broadcast actionbar ke semua pemain dengan deduplikasi terukur
     */
    public void broadcastActionBar(Component message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendActionBar(p, message);
        }
    }

    public boolean isTipsEnabled(UUID uuid) {
        return tipsEnabled.getOrDefault(uuid, true);
    }

    public void setTipsEnabled(UUID uuid, boolean enabled) {
        tipsEnabled.put(uuid, enabled);
    }

    public boolean toggleTips(UUID uuid) {
        boolean current = isTipsEnabled(uuid);
        tipsEnabled.put(uuid, !current);
        return !current;
    }
}
