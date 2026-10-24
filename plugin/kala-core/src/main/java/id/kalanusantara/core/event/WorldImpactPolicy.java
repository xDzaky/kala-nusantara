package id.kalanusantara.core.event;

import id.kalanusantara.core.KalaCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * WorldImpactPolicy — Gerbang Tunggal Keamanan Dampak Dunia (Kala Nusantara v7.0).
 *
 * Audit V7:
 * - Level 0 (Atmospheric): Zero block modifications di kala_main (default).
 * - Performance Caps terstandarisasi: 6 batu per salvo, 12 hazard aktif, 6 mob encounter (total maks 24 entitas).
 * - P1 Fix: Cleanup terisolasi per eventId sehingga penghentian satu event tidak menghapus entitas event lain.
 */
public class WorldImpactPolicy {

    public enum ImpactLevel {
        LEVEL_0_ATMOSPHERIC, // Default kala_main: hanya partikel, audio, damage zona server
        LEVEL_1_LOCAL_ZONE,  // Khusus arena terkontrol
        LEVEL_2_STAGING_ONLY // Khusus pengujian
    }

    private final KalaCorePlugin plugin;
    private final Logger log;

    public final NamespacedKey EVENT_ENTITY_KEY;
    public final NamespacedKey EVENT_ID_KEY;

    // Performance Budget Caps
    public static final int MAX_ACTIVE_HAZARDS = 12;
    public static final int MAX_ROCKFALL_PER_SALVO = 6;
    public static final int MAX_EVENT_MOBS = 6;
    public static final int TOTAL_EVENT_ENTITY_CAP = 24;

    private final Set<Entity> trackedEntities = ConcurrentHashMap.newKeySet();

    public WorldImpactPolicy(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.EVENT_ENTITY_KEY = new NamespacedKey(plugin, "kala_event_entity");
        this.EVENT_ID_KEY = new NamespacedKey(plugin, "kala_event_id");
    }

    /**
     * Memeriksa apakah suatu dunia mengizinkan modifikasi blok
     */
    public boolean canMutateBlock(Block block, String eventId) {
        if (block == null) return false;
        World world = block.getWorld();

        // kala_main selalu terkunci pada Level 0 (Atmospheric murni)
        if (world.getName().equals("kala_main")) {
            return false;
        }

        // Jangan pernah memodifikasi container, redstone, atau wilayah claim
        if (block.getState() instanceof org.bukkit.block.Container) return false;
        if (block.getType().name().contains("REDSTONE")) return false;
        if (block.getType().name().contains("BED")) return false;

        return false; // Default aman: tolak semua modifikasi blok fisik
    }

    /**
     * Mendaftarkan entity sementara milik event dengan tag persistent data
     */
    public void registerEventEntity(Entity entity, String eventId) {
        if (entity == null) return;
        entity.getPersistentDataContainer().set(EVENT_ENTITY_KEY, PersistentDataType.BYTE, (byte) 1);
        if (eventId != null) {
            entity.getPersistentDataContainer().set(EVENT_ID_KEY, PersistentDataType.STRING, eventId);
        }
        trackedEntities.add(entity);
    }

    public boolean isEventEntity(Entity entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(EVENT_ENTITY_KEY, PersistentDataType.BYTE);
    }

    public String getEventId(Entity entity) {
        if (entity == null) return null;
        return entity.getPersistentDataContainer().get(EVENT_ID_KEY, PersistentDataType.STRING);
    }

    public int getActiveEntityCount() {
        trackedEntities.removeIf(e -> !e.isValid() || e.isDead());
        return trackedEntities.size();
    }

    public boolean canSpawnMoreEntities(int upcomingCount) {
        return (getActiveEntityCount() + upcomingCount) <= TOTAL_EVENT_ENTITY_CAP;
    }

    /**
     * P1 Fix: Membersihkan entitas sementara khusus untuk satu eventId tertentu.
     * Tidak menyentuh entitas milik event lain.
     */
    public void cleanupEvent(String eventId) {
        if (eventId == null) {
            cleanupAll();
            return;
        }

        int removed = 0;
        for (Iterator<Entity> it = trackedEntities.iterator(); it.hasNext(); ) {
            Entity e = it.next();
            if (e != null && e.isValid()) {
                String id = getEventId(e);
                if (eventId.equals(id)) {
                    e.remove();
                    it.remove();
                    removed++;
                }
            } else {
                it.remove();
            }
        }

        // Scan darurat terisolasi di world aktif jika ada entitas tertinggal
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (isEventEntity(e) && eventId.equals(getEventId(e))) {
                    e.remove();
                    removed++;
                }
            }
        }

        if (removed > 0) {
            log.info("[WorldImpactPolicy] Cleanup terisolasi event [" + eventId + "]: " + removed + " entitas dibersihkan.");
        }
    }

    /**
     * Membersihkan seluruh entitas event secara darurat / saat shutdown server.
     */
    public void cleanupAll() {
        int removed = 0;
        for (Entity e : trackedEntities) {
            if (e != null && e.isValid()) {
                e.remove();
                removed++;
            }
        }
        trackedEntities.clear();

        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (isEventEntity(e)) {
                    e.remove();
                    removed++;
                }
            }
        }

        if (removed > 0) {
            log.info("[WorldImpactPolicy] Emergency Cleanup selesai: " + removed + " entitas dibersihkan.");
        }
    }

    public void cleanupAllEventEntities() {
        cleanupAll();
    }

    public void cleanupAllEventEntities(String eventId) {
        cleanupEvent(eventId);
    }
}
