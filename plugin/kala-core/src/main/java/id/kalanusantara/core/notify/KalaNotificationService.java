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

    public KalaNotificationService(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Kirim title ke pemain dengan validasi prioritas dan cooldown
     */
    public void sendTitle(Player player, Priority priority, Component title, Component subtitle) {
        if (player == null || !player.isOnline()) return;

        // Cek preferensi pemain untuk notifikasi tips/kosmetik
        if (priority == Priority.TIPS_COSMETIC && !isTipsEnabled(player.getUniqueId())) {
            // Jika pemain mematikan tips, alihkan ke actionbar ringan saja
            player.sendActionBar(title);
            return;
        }

        // Title standard timing: 10 ticks fadeIn, 60 ticks stay, 20 ticks fadeOut
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
     * Kirim actionbar dengan deduplikasi 3 detik agar tidak spam
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
     * Broadcast actionbar ke semua pemain
     */
    public void broadcastActionBar(Component message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendActionBar(message);
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
