package id.kalanusantara.core.npc;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * MbahKalaNpc — logika dialog dan quest-start untuk NPC Mbah Kala.
 *
 * Mbah Kala adalah tetua desa di Labuhan Awal.
 * Ia memberikan quest tutorial_labuhan saat pertama kali berinteraksi,
 * dan memberi info konteks di sesi berikutnya.
 */
public class MbahKalaNpc {

    private final KalaCorePlugin plugin;

    public MbahKalaNpc(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Dipanggil saat pemain interact dengan ArmorStand Mbah Kala.
     */
    public void onInteract(Player player) {
        var tracker = plugin.getQuestProgressTracker();
        var uuid = player.getUniqueId();

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.7f, 0.9f);

        boolean tutorialDone = plugin.getIdentityManager()
            .get(uuid).map(p -> p.isTutorialDone()).orElse(false);

        boolean tutorialActive = tracker.isQuestActive(uuid, "tutorial_labuhan");
        boolean tutorialCompleted = tracker.isQuestCompleted(uuid, "tutorial_labuhan");

        if (!tutorialActive && !tutorialCompleted) {
            // Pemain belum mulai tutorial — berikan quest
            sendDialog(player, new String[]{
                "§6§lMbah Kala:",
                "§eAh, pengembara baru! Selamat datang di Labuhan Awal.",
                "§eKepulauan Nusantara ini penuh dengan kesempatan...",
                "§edan juga bahaya yang perlu kamu siapkan diri.",
                "",
                "§eMari aku ajarkan dasarnya. Terima tugasku!"
            });
            // Mulai quest tutorial
            tracker.startQuest(player, "tutorial_labuhan");
            // Progress step TALK mbah_kala
            tracker.progressManual(player, "tutorial_labuhan", "mbah_kala");

        } else if (tutorialActive) {
            // Tutorial sedang berjalan
            int[] state = tracker.getStepState(uuid, "tutorial_labuhan");
            int step = state[0];

            if (step == 0) {
                // Step masih di TALK (seharusnya sudah lewat, tapi jaga-jaga)
                tracker.progressManual(player, "tutorial_labuhan", "mbah_kala");
            } else {
                // Tutorial berjalan, beri semangat
                sendDialog(player, new String[]{
                    "§6§lMbah Kala:",
                    "§eBagus! Teruslah berjuang, Pengembara.",
                    "§7[Langkah " + step + " dari " + getMaxStep() + " selesai]",
                    "§eLihat Kompas Pengembara-mu untuk tahu apa selanjutnya."
                });
            }

        } else {
            // Tutorial selesai — beri konteks tentang dunia
            sendKaleLore(player);
        }
    }

    private void sendDialog(Player player, String[] lines) {
        player.sendMessage(Component.empty());
        for (String line : lines) {
            if (line.isEmpty()) {
                player.sendMessage(Component.empty());
            } else {
                player.sendMessage(Component.text(line));
            }
        }
        player.sendMessage(Component.empty());
    }

    private void sendKaleLore(Player player) {
        String[] lore = {
            "§6§lMbah Kala:",
            "§eAku sudah melihat banyak musim di kepulauan ini...",
            "§eTiap musim, Kala — kekuatan alam — bergerak.",
            "§eTerkadang gunung bicara. Terkadang laut mengamuk.",
            "§eTapi kami selalu membangun kembali, bersama-sama.",
            "",
            "§7[Gunakan /kompas untuk melihat aktivitas yang tersedia]"
        };
        sendDialog(player, lore);
    }

    private int getMaxStep() {
        return plugin.getQuestManager().getQuest("tutorial_labuhan")
            .map(q -> q.getTotalSteps()).orElse(5);
    }
}
