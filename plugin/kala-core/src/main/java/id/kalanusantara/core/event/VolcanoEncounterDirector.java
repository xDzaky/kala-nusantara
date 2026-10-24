package id.kalanusantara.core.event;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.kala.KalaEventManager;
import id.kalanusantara.core.notify.KalaNotificationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

/**
 * VolcanoEncounterDirector — Mengurutkan subfase encounter berirama selama fase PUNCAK (Kala Nusantara v6.0).
 *
 * Mengatur:
 * 1. Pembuka Kawah (0–45s)
 * 2. Gelombang Abu & Observasi (45–120s)
 * 3. Salvo Rockfall Tertelegrafis (120–240s)
 * 4. Retakan Panas & Jalur Kawah (240–360s)
 * 5. Encounter Bara Opsional (360–480s)
 * 6. Objective Final & Resolusi Altar (480–600s)
 */
public class VolcanoEncounterDirector {

    public enum EncounterSubphase {
        PEMBUKA_KAWAH("Pembuka Kawah"),
        GELOMBANG_ABU("Gelombang Abu & Pengamatan"),
        ROCKFALL_SALVO("Hujan Bongkahan Batu"),
        RETAKAN_PANAS("Retakan Panas Kawah"),
        ENCOUNTER_BARA("Ancaman Penjaga Bara"),
        OBJECTIVE_FINAL("Resolusi Altar Gotong Royong");

        private final String displayName;
        EncounterSubphase(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    private final KalaCorePlugin plugin;
    private final WorldImpactPolicy policy;
    private final HazardDirector hazardDirector;
    private final EventContributionLedger ledger;
    private final Logger log;

    private EncounterSubphase currentSubphase = null;
    private BukkitTask sequenceTask;
    private String currentEventId = null;
    private int encounterSeconds = 0;

    public VolcanoEncounterDirector(KalaCorePlugin plugin, WorldImpactPolicy policy,
                                    HazardDirector hazardDirector, EventContributionLedger ledger) {
        this.plugin = plugin;
        this.policy = policy;
        this.hazardDirector = hazardDirector;
        this.ledger = ledger;
        this.log = plugin.getLogger();
    }

    /**
     * Memulai urutan encounter saat fase PUNCAK dimulai di KalaEventManager
     */
    public void startEncounter(String eventId) {
        this.currentEventId = eventId;
        this.encounterSeconds = 0;
        cancelEncounter();

        log.info("[VolcanoDirector] Memulai encounter terstruktur PUNCAK untuk event: " + eventId);
        setSubphase(EncounterSubphase.PEMBUKA_KAWAH);

        // Sequence runner setiap 1 detik
        sequenceTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            encounterSeconds++;

            // Ritme Subfase berdasarkan detik
            if (encounterSeconds == 45) {
                setSubphase(EncounterSubphase.GELOMBANG_ABU);
            } else if (encounterSeconds == 120) {
                setSubphase(EncounterSubphase.ROCKFALL_SALVO);
            } else if (encounterSeconds == 240) {
                setSubphase(EncounterSubphase.RETAKAN_PANAS);
            } else if (encounterSeconds == 360) {
                setSubphase(EncounterSubphase.ENCOUNTER_BARA);
            } else if (encounterSeconds == 480) {
                setSubphase(EncounterSubphase.OBJECTIVE_FINAL);
            }

            // Loop aksi per subfase
            tickCurrentSubphase();
        }, 20L, 20L);
    }

    private void setSubphase(EncounterSubphase subphase) {
        this.currentSubphase = subphase;
        log.info("[VolcanoDirector] Subfase encounter: " + subphase.getDisplayName());

        World world = Bukkit.getWorld("kala_main");
        if (world == null) return;
        Location spawnLoc = world.getSpawnLocation();

        switch (subphase) {
            case PEMBUKA_KAWAH -> {
                plugin.getNotificationService().broadcastTitle(
                        KalaNotificationService.Priority.STORY_EVENT,
                        Component.text("🌋 KAWAH TERBUKA 🌋", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Amati tanda-tanda alam dan jauhi tebing lereng!", NamedTextColor.YELLOW),
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.6f
                );
            }
            case GELOMBANG_ABU -> {
                plugin.getNotificationService().broadcastActionBar(
                        Component.text("§6[Misi Lapangan] Kumpulkan sampel abu vulkanik atau amankan lumbung desa!", NamedTextColor.GOLD)
                );
            }
            case ROCKFALL_SALVO -> {
                plugin.getNotificationService().broadcastTitle(
                        KalaNotificationService.Priority.STORY_EVENT,
                        Component.text("⚠ HUJAN BONGKAHAN BATU ⚠", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                        Component.text("Perhatikan penanda bara di tanah dan segera menghindar!", NamedTextColor.RED),
                        Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f
                );
            }
            case RETAKAN_PANAS -> {
                plugin.getNotificationService().broadcastActionBar(
                        Component.text("§c[!] Retakan panas muncul di sekitar lereng kawah!", NamedTextColor.RED)
                );
                // Munculkan 2-3 heat vents di sekitar spawn arena
                for (int i = 0; i < 3; i++) {
                    Location ventLoc = spawnLoc.clone().add((Math.random() - 0.5) * 20, 0, (Math.random() - 0.5) * 20);
                    hazardDirector.spawnHeatVent(ventLoc, currentEventId, 45);
                }
            }
            case ENCOUNTER_BARA -> {
                // Munculkan Percik Bara (Magma Cube kecil aman) jika pemain siap
                spawnBaraMinions(spawnLoc);
            }
            case OBJECTIVE_FINAL -> {
                plugin.getNotificationService().broadcastTitle(
                        KalaNotificationService.Priority.STORY_EVENT,
                        Component.text("✦ RESOLUSI TERAKHIR ✦", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                        Component.text("Satukan persembahan di Altar untuk menenangkan Murka!", NamedTextColor.GOLD),
                        Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f
                );
            }
        }
    }

    private void tickCurrentSubphase() {
        if (currentSubphase == null) return;
        World world = Bukkit.getWorld("kala_main");
        if (world == null) return;

        // Subfase 3: Salvo berkala setiap 25 detik
        if (currentSubphase == EncounterSubphase.ROCKFALL_SALVO) {
            if (encounterSeconds % 25 == 0) {
                hazardDirector.launchRockfallSalvo(world.getSpawnLocation(), currentEventId, 4);
            }
        }
    }

    /**
     * Spawn minion bara terkontrol (maksimal 4 entitas vanilla, zero block fire)
     */
    private void spawnBaraMinions(Location center) {
        if (center == null || center.getWorld() == null) return;
        if (!policy.canSpawnMoreEntities(3)) return;

        World world = center.getWorld();
        for (int i = 0; i < 3; i++) {
            Location loc = center.clone().add((Math.random() - 0.5) * 16, 1, (Math.random() - 0.5) * 16);
            MagmaCube cube = (MagmaCube) world.spawnEntity(loc, EntityType.MAGMA_CUBE);
            cube.setSize(1); // Ukuran kecil yang aman
            cube.customName(Component.text("§cPercik Bara Kawah", NamedTextColor.RED));
            cube.setCustomNameVisible(true);
            policy.registerEventEntity(cube, currentEventId);
        }

        plugin.getNotificationService().broadcastActionBar(
                Component.text("§c[Encounter] Makhluk bara kawah muncul di sekitar arena!", NamedTextColor.RED)
        );
    }

    /**
     * Menghentikan encounter dan membersihkan seluruh entitas serta hazard
     */
    public void stopEncounter() {
        cancelEncounter();
        if (currentEventId != null) {
            // Berikan imbalan partisipasi kepada semua pemain di kala_main
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().getName().equals("kala_main")) {
                    ledger.claimReward(currentEventId, p, 50.0, 30);
                }
            }

            policy.cleanupAllEventEntities(currentEventId);
            hazardDirector.cancelAllHazards();
        }
        currentSubphase = null;
        currentEventId = null;
        encounterSeconds = 0;
    }

    private void cancelEncounter() {
        if (sequenceTask != null && !sequenceTask.isCancelled()) {
            sequenceTask.cancel();
            sequenceTask = null;
        }
    }

    public EncounterSubphase getCurrentSubphase() {
        return currentSubphase;
    }

    public String getCurrentEventId() {
        return currentEventId;
    }

    public void advanceNextSubphase() {
        if (currentSubphase == null) {
            setSubphase(EncounterSubphase.PEMBUKA_KAWAH);
            return;
        }
        EncounterSubphase next = switch (currentSubphase) {
            case PEMBUKA_KAWAH -> EncounterSubphase.GELOMBANG_ABU;
            case GELOMBANG_ABU -> EncounterSubphase.ROCKFALL_SALVO;
            case ROCKFALL_SALVO -> EncounterSubphase.RETAKAN_PANAS;
            case RETAKAN_PANAS -> EncounterSubphase.ENCOUNTER_BARA;
            case ENCOUNTER_BARA -> EncounterSubphase.OBJECTIVE_FINAL;
            case OBJECTIVE_FINAL -> null;
        };

        if (next != null) {
            setSubphase(next);
        } else {
            stopEncounter();
        }
    }
}
