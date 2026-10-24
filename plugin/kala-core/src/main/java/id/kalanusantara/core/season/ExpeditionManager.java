package id.kalanusantara.core.season;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * ExpeditionManager — Mengelola ekspedisi musiman (Fase 6).
 * Pemain menjelajah ke koordinat pulau luar untuk menemukan reruntuhan kuno,
 * membuka entri lore jurnal, dan mendapatkan gelar penjelajah samudra.
 */
public class ExpeditionManager {

    private final KalaCorePlugin plugin;

    // Lokasi Ekspedisi Musim 1
    private final String expeditionName = "Reruntuhan Kuil Rempah Kuno";
    private final String targetWorld = "kala_main";
    private final int targetX = 500;
    private final int targetZ = -500;

    // Set pemain yang sudah menyelesaikan ekspedisi musim ini
    private final Set<UUID> completedPlayers = new HashSet<>();

    public ExpeditionManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // Periodic check (setiap 20 tick / 1 detik) bagi pemain yang mendekati situs ekspedisi
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld(targetWorld);
            if (world == null) return;

            for (Player player : world.getPlayers()) {
                if (completedPlayers.contains(player.getUniqueId())) continue;

                Location loc = player.getLocation();
                double dx = loc.getX() - targetX;
                double dz = loc.getZ() - targetZ;
                double distanceSq = (dx * dx) + (dz * dz);

                // Radius 25 blok dari koordinat reruntuhan
                if (distanceSq <= 25 * 25) {
                    discoverExpedition(player);
                }
            }
        }, 100L, 20L);
    }

    private void discoverExpedition(Player player) {
        completedPlayers.add(player.getUniqueId());

        // Efek visual & audio
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        Title title = Title.title(
            Component.text("EKSPEDISI SELESAI!", NamedTextColor.GOLD),
            Component.text("Menemukan " + expeditionName, NamedTextColor.YELLOW),
            Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3000), Duration.ofMillis(800))
        );
        player.showTitle(title);

        // Reward ekonomi & kosmetik
        plugin.getEconomy().deposit(player.getUniqueId(), 250.0, "Hadiah Ekspedisi: " + expeditionName);
        plugin.getCosmeticManager().unlockTitle(player, "PENJELAJAH_SAMUDRA");

        // Catat di Jurnal Kepulauan
        plugin.getJurnalManager().addEntry(
            player,
            "discovery",
            "Ekspedisi: " + expeditionName,
            "Berhasil menyeberangi lautan dan menemukan situs peninggalan rempah leluhur di koordinat X: " + targetX + ", Z: " + targetZ + "."
        );

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("🏛 Penemuan Bersejarah Nusantara!", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Kamu telah menemukan §e" + expeditionName + "§f!"));
        player.sendMessage(Component.text("Hadiah: §a+Rp 250.00 Rupiah Kala §7& Gelar §b[Penjelajah Samudra]", NamedTextColor.GREEN));
        player.sendMessage(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.empty());
    }

    public String getExpeditionName() { return expeditionName; }
    public int getTargetX() { return targetX; }
    public int getTargetZ() { return targetZ; }

    public int getDistanceToTarget(Player player) {
        if (!player.getWorld().getName().equals(targetWorld)) return -1;
        double dx = player.getLocation().getX() - targetX;
        double dz = player.getLocation().getZ() - targetZ;
        return (int) Math.sqrt((dx * dx) + (dz * dz));
    }
}
