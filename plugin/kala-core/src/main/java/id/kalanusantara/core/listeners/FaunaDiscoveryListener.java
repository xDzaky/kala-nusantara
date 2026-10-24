package id.kalanusantara.core.listeners;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.fauna.FaunaManager;
import id.kalanusantara.core.fauna.FaunaSpecies;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * FaunaDiscoveryListener — Mendeteksi interaksi pemain dengan satwa liar
 * untuk didaftarkan ke Atlas Fauna Nusantara secara otomatis.
 */
public class FaunaDiscoveryListener implements Listener {

    private final KalaCorePlugin plugin;
    private final FaunaManager faunaManager;

    public FaunaDiscoveryListener(KalaCorePlugin plugin, FaunaManager faunaManager) {
        this.plugin = plugin;
        this.faunaManager = faunaManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity target = event.getRightClicked();

        FaunaSpecies species = FaunaSpecies.fromEntityType(target.getType());
        if (species != null) {
            // Cek apakah pemain belum menemukan satwa ini
            if (!faunaManager.isDiscovered(player.getUniqueId(), species.getId())) {
                faunaManager.discover(player, species);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        faunaManager.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        faunaManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
