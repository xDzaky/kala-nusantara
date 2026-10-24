package id.kalanusantara.core.event;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.notify.KalaNotificationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.MagmaCube;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

/**
 * VolcanoEncounterDirector — Mengurutkan subfase encounter berirama selama fase PUNCAK (Kala Nusantara v7.0).
 *
 * Audit V7:
 * 1. P1 Fix: Menggunakan arena Gunung Murka dari konfigurasi (getVolcanoCenter), BUKAN world spawn.
 * 2. P1 Fix: Pemisahan alur penyelesaian normal (completeEncounter) dan pembatalan (abortEncounter).
 * 3. Unified Timeline 10 Menit (600 detik) yang selaras dengan PUNCAK_DURATION_SEC di KalaEventManager.
 * 4. P1 Fix: Cleanup terisolasi dengan cleanupEvent(currentEventId).
 */
public class VolcanoEncounterDirector {

    public enum EncounterSubphase {
        PEMBUKA_KAWAH("Pembuka Kawah", 0, 45),
        GELOMBANG_ABU("Gelombang Abu & Pengamatan", 45, 120),
        ROCKFALL_SALVO("Hujan Bongkahan Batu", 120, 240),
        RETAKAN_PANAS("Retakan Panas Kawah", 240, 360),
        ENCOUNTER_BARA("Ancaman Penjaga Bara", 360, 480),
        OBJECTIVE_FINAL("Resolusi Altar Gotong Royong", 480, 600);

        private final String displayName;
        private final int startSec;
        private final int endSec;

        EncounterSubphase(String displayName, int startSec, int endSec) {
            this.displayName = displayName;
            this.startSec = startSec;
            this.endSec = endSec;
        }

        public String getDisplayName() { return displayName; }
        public int getStartSec() { return startSec; }
        public int getEndSec() { return endSec; }
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
        cancelEncounterTask();

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
            } else if (encounterSeconds >= 600) {
                completeEncounter();
                return;
            }

            // Loop aksi per subfase
            tickCurrentSubphase();
        }, 20L, 20L);
    }

    private void setSubphase(EncounterSubphase subphase) {
        this.currentSubphase = subphase;
        log.info("[VolcanoDirector] Subfase encounter: " + subphase.getDisplayName() + " [" + encounterSeconds + "s]");

        Location arenaCenter = getArenaLocation();
        if (arenaCenter == null) {
            log.warning("[VolcanoDirector] Gagal menemukan koordinat arena Gunung Murka!");
            return;
        }

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
                // Munculkan 2-3 heat vents di sekitar arena gunung
                for (int i = 0; i < 3; i++) {
                    Location ventLoc = arenaCenter.clone().add((Math.random() - 0.5) * 30, 0, (Math.random() - 0.5) * 30);
                    hazardDirector.spawnHeatVent(ventLoc, currentEventId, 45);
                }
            }
            case ENCOUNTER_BARA -> {
                spawnBaraMinions(arenaCenter);
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
        Location arenaCenter = getArenaLocation();
        if (arenaCenter == null) return;

        // Subfase 3: Salvo berkala setiap 25 detik di arena gunung
        if (currentSubphase == EncounterSubphase.ROCKFALL_SALVO) {
            if (encounterSeconds % 25 == 0) {
                hazardDirector.launchRockfallSalvo(arenaCenter, currentEventId, 4);
            }
        }
    }

    /**
     * Mendapatkan lokasi pusat arena Gunung Murka dari konfigurasi (Bukan Spawn)
     */
    private Location getArenaLocation() {
        World world = Bukkit.getWorld(plugin.getKalaConfig().getVolcanoWorld());
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) return null;
        return plugin.getKalaConfig().getVolcanoCenter(world);
    }

    /**
     * Spawn minion bara terkontrol di sekitar arena gunung (maks 3 entitas)
     */
    private void spawnBaraMinions(Location center) {
        if (center == null || center.getWorld() == null) return;
        if (!policy.canSpawnMoreEntities(3)) return;

        World world = center.getWorld();
        for (int i = 0; i < 3; i++) {
            Location loc = center.clone().add((Math.random() - 0.5) * 20, 1, (Math.random() - 0.5) * 20);
            MagmaCube cube = (MagmaCube) world.spawnEntity(loc, EntityType.MAGMA_CUBE);
            cube.setSize(1); // Ukuran kecil aman
            cube.customName(Component.text("§cPercik Bara Kawah", NamedTextColor.RED));
            cube.setCustomNameVisible(true);
            policy.registerEventEntity(cube, currentEventId);
        }

        plugin.getNotificationService().broadcastActionBar(
                Component.text("§c[Encounter] Makhluk bara kawah muncul di sekitar arena gunung!", NamedTextColor.RED)
        );
    }

    /**
     * P1 Fix: Penyelesaian Normal Encounter.
     * Mengumumkan reward klaim manual via /kala claim-event dan membersihkan entitas.
     */
    public void completeEncounter() {
        cancelEncounterTask();
        String finishedEventId = currentEventId;

        if (finishedEventId != null) {
            log.info("[VolcanoDirector] Encounter PUNCAK selesai normal untuk event: " + finishedEventId);
            Bukkit.broadcast(Component.empty());
            Bukkit.broadcast(Component.text("§6§l✦ PUNCAK ERUPSI MEREDA ✦", NamedTextColor.GOLD));
            Bukkit.broadcast(Component.text("§7Semua pengembara yang telah bergotong royong dapat mengklaim imbalan:", NamedTextColor.GRAY));
            Bukkit.broadcast(Component.text("§eGunakan perintah: §a/kala claim-event " + finishedEventId, NamedTextColor.YELLOW));
            Bukkit.broadcast(Component.empty());

            policy.cleanupEvent(finishedEventId);
            hazardDirector.cancelAllHazards();
        }

        currentSubphase = null;
        currentEventId = null;
        encounterSeconds = 0;
    }

    /**
     * P1 Fix: Pembatalan Encounter (Admin stop atau Server Shutdown).
     * Tidak melakukan auto-payout; hanya membersihkan entitas dan task secara idempotent.
     */
    public void abortEncounter(String reason) {
        cancelEncounterTask();
        String abortedId = currentEventId;

        if (abortedId != null) {
            log.info("[VolcanoDirector] Encounter dibatalkan (" + reason + ") untuk event: " + abortedId);
            policy.cleanupEvent(abortedId);
            hazardDirector.cancelAllHazards();
        }

        currentSubphase = null;
        currentEventId = null;
        encounterSeconds = 0;
    }

    public void stopEncounter() {
        abortEncounter("Manual/Shutdown");
    }

    private void cancelEncounterTask() {
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
            completeEncounter();
        }
    }
}
