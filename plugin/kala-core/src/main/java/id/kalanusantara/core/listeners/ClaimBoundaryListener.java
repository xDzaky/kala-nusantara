package id.kalanusantara.core.listeners;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * ClaimBoundaryListener — Mencegah klaim lahan di Dunia Tambang (kala_resource).
 * Klaim hanya diperbolehkan di Pulau Utama (kala_main).
 */
public class ClaimBoundaryListener implements Listener {

    private final KalaCorePlugin plugin;

    public ClaimBoundaryListener(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onShovelClaim(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.GOLDEN_SHOVEL) return;

        // Cek apakah pemain berada di dunia tambang
        if (plugin.getResourceWorldManager().isResourceWorld(player.getWorld())) {
            event.setCancelled(true);
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("§c[!] Klaim tanah dinonaktifkan di Dunia Tambang!", NamedTextColor.RED));
            player.sendMessage(Component.text("§7Dunia ini direset secara berkala untuk menjaga regenerasi mineral.", NamedTextColor.GRAY));
            player.sendMessage(Component.text("§eGunakan /tambang pulang §7dan lakukan klaim di pulau utama (Labuhan Awal).", NamedTextColor.YELLOW));
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
        }
    }
}
