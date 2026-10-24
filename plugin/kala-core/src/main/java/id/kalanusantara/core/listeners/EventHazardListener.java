package id.kalanusantara.core.listeners;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.event.EventContributionLedger;
import id.kalanusantara.core.event.SeasonalEventDirector;
import id.kalanusantara.core.event.VolcanoEncounterDirector;
import id.kalanusantara.core.event.WorldImpactPolicy;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * EventHazardListener — Penjaga Integritas Dunia & Pencegah Kerusakan Blok Event (Kala Nusantara v6.0).
 *
 * Mengawal:
 * 1. WorldImpactPolicy Level 0: Membatalkan EntityChangeBlockEvent dari FallingBlock event sehingga 0 blok termutasi.
 * 2. Pembersihan ledakan event (EntityExplodeEvent & BlockExplodeEvent) agar blockList kosong.
 * 3. Penghubung interaksi pemain untuk SeasonalEventDirector (Peti hanyut & Bara semu).
 * 4. Poin kontribusi tempur untuk VolcanoEncounterDirector saat menyerang mob penjaga kawah.
 */
public class EventHazardListener implements Listener {

    private final KalaCorePlugin plugin;
    private final WorldImpactPolicy policy;
    private final SeasonalEventDirector seasonalDirector;
    private final VolcanoEncounterDirector volcanoDirector;
    private final EventContributionLedger ledger;

    public EventHazardListener(KalaCorePlugin plugin, WorldImpactPolicy policy,
                               SeasonalEventDirector seasonalDirector,
                               VolcanoEncounterDirector volcanoDirector,
                               EventContributionLedger ledger) {
        this.plugin = plugin;
        this.policy = policy;
        this.seasonalDirector = seasonalDirector;
        this.volcanoDirector = volcanoDirector;
        this.ledger = ledger;
    }

    /**
     * Kunci Pencegahan Kerusakan Blok:
     * Saat FallingBlock mendarat, Paper akan memanggil EntityChangeBlockEvent.
     * Kita batalkan jika entitas tersebut ditandai oleh WorldImpactPolicy.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof FallingBlock && policy.isEventEntity(entity)) {
            event.setCancelled(true);
            entity.remove();
        }
    }

    /**
     * Cegah kehancuran blok oleh ledakan yang dipicu event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (policy.isEventEntity(event.getEntity())) {
            event.blockList().clear(); // Zero block destruction
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Jika dunia kala_main berada dalam level 0, blok tidak boleh hancur karena ledakan kosmetik
        if (event.getBlock().getWorld().getName().equals("kala_main")) {
            event.blockList().clear();
        }
    }

    /**
     * Interaksi pemain untuk event musiman:
     * - Memadamkan bara semu dengan klik blok atau klik entity marker (Audit V8 Bedrock Friendly)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null) {
                Location clicked = event.getClickedBlock().getLocation();
                if (seasonalDirector.handlePlayerInteraction(event.getPlayer(), clicked)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (policy.isEventEntity(entity)) {
            if (seasonalDirector.handlePlayerInteraction(event.getPlayer(), entity.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Pengambilan item peti kargo hanyut (Badai Selat)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            Item item = event.getItem();
            if (policy.isEventEntity(item)) {
                seasonalDirector.handleCratePickup(player, item);
            }
        }
    }

    /**
     * Kontribusi Tempur: Saat pemain menyerang mob kawah / percik bara
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            Entity victim = event.getEntity();
            if (policy.isEventEntity(victim)) {
                String eventId = volcanoDirector.getCurrentEventId();
                if (eventId != null) {
                    ledger.recordContribution(eventId, player, "COMBAT_DEFENSE", 5);
                }
            }
        }
    }
}
