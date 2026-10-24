package id.kalanusantara.core.kala;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.notify.KalaNotificationService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import java.util.logging.Logger;

/**
 * KalaEventManager — Sistem Aktivitas Gunung Murka & Event Siklus Vulkanik.
 * Mengintegrasikan Kyori Adventure BossBar, efek partikel atmosferik aman vanilla,
 * suara gempa seismik non-destruktif, serta mekanik Sajen Gotong Royong (Kala Expansion v4).
 */
public class KalaEventManager {

    public enum KalaPhase {
        IDLE("Tenang", "§7Tenang", BossBar.Color.WHITE),
        TANDA_KALA("Waspada", "§e⚠ Waspada", BossBar.Color.YELLOW),
        PERSIAPAN("Siaga", "§6⏳ Siaga", BossBar.Color.RED),
        PUNCAK("Erupsi Murka", "§c🌋 Erupsi Murka", BossBar.Color.RED),
        PEMULIHAN("Pemulihan", "§a🌿 Pemulihan Subur", BossBar.Color.GREEN);

        private final String simpleName;
        private final String displayName;
        private final BossBar.Color bossBarColor;

        KalaPhase(String simpleName, String displayName, BossBar.Color bossBarColor) {
            this.simpleName = simpleName;
            this.displayName = displayName;
            this.bossBarColor = bossBarColor;
        }

        public String getSimpleName() { return simpleName; }
        public String getDisplayName() { return displayName; }
        public BossBar.Color getBossBarColor() { return bossBarColor; }
    }

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final Random random = new Random();

    private KalaPhase currentPhase = KalaPhase.IDLE;
    private BukkitTask autoAdvanceTask;
    private BukkitTask atmosphericTask;
    private BukkitTask autoSchedulerTask;

    private int phaseRemainingSeconds = 0;
    private int phaseTotalSeconds = 0;

    // Gotong Royong Sajen / Offerings Pool
    private int currentOfferings = 0;
    private int targetOfferings = 100;

    // Otomatisasi Siklus Vulkanik (Pilihan A: Otomatis Berkala 24-48 Jam)
    private long lastEruptionTime = System.currentTimeMillis();
    private long autoTriggerIntervalMs = 24L * 3600L * 1000L; // Default: 24 jam

    // Durasi tiap fase dalam detik (V7: Unified 10-Minute PUNCAK Timeline)
    private static final int TANDA_DURATION_SEC     = 120; // 2 menit
    private static final int PERSIAPAN_DURATION_SEC = 180; // 3 menit
    private static final int PUNCAK_DURATION_SEC    = 600; // 10 menit bersatu dengan VolcanoEncounterDirector
    private static final int PEMULIHAN_DURATION_SEC = 180; // 3 menit

    // Kyori Adventure BossBar persisten tunggal (UI Ownership Matrix)
    private BossBar eventBossBar;

    public KalaEventManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
        // Muat timer persisten
        this.lastEruptionTime = plugin.getKalaConfig().getRaw().getLong("volcano.last_eruption_time", System.currentTimeMillis());
        initBossBar();
        startAutoScheduler();
    }

    private void initBossBar() {
        eventBossBar = BossBar.bossBar(
                Component.text("🌋 Gunung Murka — Tenang", NamedTextColor.GRAY),
                1.0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
        );
    }

    /**
     * Otomatisasi Siklus Vulkanik: Mengecek timer berkala di latar belakang
     */
    public void startAutoScheduler() {
        if (autoSchedulerTask != null) autoSchedulerTask.cancel();
        // Cek setiap 5 menit (6000 ticks)
        autoSchedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentPhase == KalaPhase.IDLE && !Bukkit.getOnlinePlayers().isEmpty()) {
                long elapsed = System.currentTimeMillis() - lastEruptionTime;
                if (elapsed >= autoTriggerIntervalMs) {
                    log.info("[VolcanoManager] Interval erupsi berkala tercapai (" + (elapsed / 3600000L) + " jam). Memulai siklus Gunung Murka!");
                    startEvent();
                }
            }
        }, 6000L, 6000L);
    }

    // ─────────────────────────────────────────────
    // Control API
    // ─────────────────────────────────────────────

    public boolean startEvent() {
        if (currentPhase != KalaPhase.IDLE) return false;
        transitionTo(KalaPhase.TANDA_KALA);
        return true;
    }

    public boolean advancePhase() {
        KalaPhase next = getNextPhase(currentPhase);
        if (next == null) return false;
        transitionTo(next);
        return true;
    }

    public boolean stopEvent() {
        if (currentPhase == KalaPhase.IDLE) return false;
        if (plugin.getVolcanoEncounterDirector() != null) {
            plugin.getVolcanoEncounterDirector().abortEncounter("Admin/System stop");
        }
        cancelTasks();
        transitionTo(KalaPhase.IDLE);
        return true;
    }

    public KalaPhase getCurrentPhase() { return currentPhase; }
    public String getPhaseDisplayName() { return currentPhase.getDisplayName(); }
    public int getPhaseRemainingSeconds() { return phaseRemainingSeconds; }
    public int getCurrentOfferings() { return currentOfferings; }
    public int getTargetOfferings() { return targetOfferings; }

    public boolean isRecoveryActive() {
        return currentPhase == KalaPhase.PEMULIHAN;
    }

    // ─────────────────────────────────────────────
    // Phase transitions
    // ─────────────────────────────────────────────

    private void transitionTo(KalaPhase phase) {
        KalaPhase prev = currentPhase;
        currentPhase = phase;
        cancelTasks();

        log.info("[VolcanoManager] Transisi fase Gunung Murka: " + prev + " → " + phase);

        switch (phase) {
            case IDLE        -> onIdle(prev);
            case TANDA_KALA  -> onTandaKala();
            case PERSIAPAN   -> onPersiapan();
            case PUNCAK      -> onPuncak();
            case PEMULIHAN   -> onPemulihan();
        }
    }

    // ─────────────────────────────────────────────
    // Phase handlers
    // ─────────────────────────────────────────────

    private void onIdle(KalaPhase prev) {
        if (plugin.getVolcanoEncounterDirector() != null) {
            plugin.getVolcanoEncounterDirector().abortEncounter("Fase Idle");
        }
        hideBossBarFromAll();
        currentOfferings = 0;
        phaseRemainingSeconds = 0;
        phaseTotalSeconds = 0;
        lastEruptionTime = System.currentTimeMillis();
        plugin.getKalaConfig().getRaw().set("volcano.last_eruption_time", lastEruptionTime);
        plugin.saveConfig();

        if (prev != KalaPhase.IDLE) {
            plugin.getNotificationService().broadcastActionBar(
                    Component.text("§7[Gunung Murka] Suasana kepulauan telah kembali tenang dan damai...", NamedTextColor.GRAY)
            );
        }
    }

    private void onTandaKala() {
        phaseTotalSeconds = TANDA_DURATION_SEC;
        phaseRemainingSeconds = TANDA_DURATION_SEC;
        currentOfferings = 0;

        updateBossBar(
                Component.text("🌋 Gunung Murka: Status WASPADA", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        );
        showBossBarToAll();

        plugin.getNotificationService().broadcastTitle(
                KalaNotificationService.Priority.STORY_EVENT,
                Component.text("⚠ TANDA VULKANIK ⚠", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Gunung Murka bergemuruh. Status: WASPADA!", NamedTextColor.GOLD),
                Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.6f, 0.7f
        );

        broadcastChat("§8[§6🌋 Gunung Murka§8] §eMbah Kala: §f\"Aku merasakan tanah bergetar pelan... Asap tipis mulai membubung dari puncak.\"");
        broadcastChat("§8[§6🌋 Gunung Murka§8] §7Periksa perlengkapan dan amati perkembangan status di §e/kompas§7.");

        startAtmosphericLoop();
        scheduleAutoAdvance(TANDA_DURATION_SEC, KalaPhase.PERSIAPAN);
    }

    private void onPersiapan() {
        phaseTotalSeconds = PERSIAPAN_DURATION_SEC;
        phaseRemainingSeconds = PERSIAPAN_DURATION_SEC;

        updateBossBar(
                Component.text("🌋 Gunung Murka: Status SIAGA [Kawah Bergejolak!]", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10
        );
        showBossBarToAll();

        plugin.getNotificationService().broadcastTitle(
                KalaNotificationService.Priority.STORY_EVENT,
                Component.text("⏳ STATUS SIAGA ⏳", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Gejolak kawah meningkat pesat! Siapkan gotong royong.", NamedTextColor.YELLOW),
                Sound.BLOCK_BELL_USE, 0.8f, 0.8f
        );

        broadcastChat("§8[§c🌋 Gunung Murka§8] §6Fase Siaga Vulkanik dimulai! Getaran seismik makin sering terasa.");
        broadcastChat("§8[§c🌋 Gunung Murka§8] §7• Kumpulkan material sajen/bantuan darurat (Tembaga, Arang, Gandum).");
        broadcastChat("§8[§c🌋 Gunung Murka§8] §7• Puncak aktivitas diperkirakan dalam §e" + (PERSIAPAN_DURATION_SEC / 60) + " menit§7!");

        startAtmosphericLoop();
        scheduleAutoAdvance(PERSIAPAN_DURATION_SEC, KalaPhase.PUNCAK);
    }

    private void onPuncak() {
        phaseTotalSeconds = PUNCAK_DURATION_SEC;
        phaseRemainingSeconds = PUNCAK_DURATION_SEC;
        currentOfferings = 0; // Reset target offerings untuk fase erupsi

        updateBossBar(
                Component.text("🌋 PUNCAK GUNUNG MURKA: ERUPSI VULKANIK!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_20
        );
        showBossBarToAll();

        plugin.getNotificationService().broadcastTitle(
                KalaNotificationService.Priority.STORY_EVENT,
                Component.text("🌋 PUNCAK ERUPSI MURKA 🌋", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text("Hujan abu turun! Sumbang Sajen di Altar untuk menenangkan Murka.", NamedTextColor.YELLOW),
                Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.7f
        );

        broadcastChat("§8[§4§l🌋 ERUPSI§8] §c§lGUNUNG MURKA MELETUS DENGAN SUARA MENGGELEGAR!");
        broadcastChat("§8[§4§l🌋 ERUPSI§8] §eHujan abu vulkanik menyelimuti kepulauan. Wilayah Labuhan Awal tetap terlindungi mantra kuno.");
        broadcastChat("§8[§4§l🌋 ERUPSI§8] §aGotong Royong Pereda Murka dibuka! §7Gunakan §e/gunung §7atau setor Tembaga/Arang/Pangan untuk menenangkan amukan!");

        startAtmosphericLoop();

        // Mulai Encounter Sequencer subfase V6
        if (plugin.getVolcanoEncounterDirector() != null) {
            plugin.getVolcanoEncounterDirector().startEncounter("kala_erupsi_" + System.currentTimeMillis());
        }

        scheduleAutoAdvance(PUNCAK_DURATION_SEC, KalaPhase.PEMULIHAN);
    }

    private void onPemulihan() {
        if (plugin.getVolcanoEncounterDirector() != null) {
            plugin.getVolcanoEncounterDirector().completeEncounter();
        }

        phaseTotalSeconds = PEMULIHAN_DURATION_SEC;
        phaseRemainingSeconds = PEMULIHAN_DURATION_SEC;

        updateBossBar(
                Component.text("🌿 BERKAH ABU VULKANIK: TANAH NUSANTARA SUBUR!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );
        showBossBarToAll();

        plugin.getNotificationService().broadcastTitle(
                KalaNotificationService.Priority.STORY_EVENT,
                Component.text("🌿 BERKAH PEMULIHAN 🌿", NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                Component.text("Tanah subur! Bonus panen & bonus harga jual aktif.", NamedTextColor.YELLOW),
                Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f
        );

        broadcastChat("§8[§a🌿 Pemulihan§8] §aErupsi Gunung Murka telah mereda! Abu vulkanik menyuburkan tanah seluruh pulau.");
        broadcastChat("§8[§a🌿 Pemulihan§8] §e• Petani: Kecepatan panen dan hasil bumi bertambah!");
        broadcastChat("§8[§a🌿 Pemulihan§8] §e• Pedagang Pasar: Membeli material pemulihan dengan bonus +25% Rupiah!");
        broadcastChat("§8[§a🌿 Pemulihan§8] §7Terima kasih kepada seluruh pengembara yang telah bergotong royong.");

        startAtmosphericLoop();
        scheduleAutoAdvance(PEMULIHAN_DURATION_SEC, KalaPhase.IDLE);
    }

    // ─────────────────────────────────────────────
    // Sajen / Gotong Royong Contribution Mechanics
    // ─────────────────────────────────────────────

    public boolean contributeOffering(Player player, Material material, int amount) {
        if (currentPhase != KalaPhase.PUNCAK && currentPhase != KalaPhase.PERSIAPAN) {
            player.sendMessage(Component.text("§8[§6🌋 Sajen§8] §cAltar hanya menerima persembahan saat fase Siaga atau Erupsi!"));
            return false;
        }

        int pointsPerItem = switch (material) {
            case COPPER_INGOT -> 4;
            case RAW_COPPER   -> 3;
            case COAL, CHARCOAL -> 1;
            case WHEAT        -> 1;
            case BREAD        -> 2;
            case COOKED_BEEF, COOKED_PORKCHOP -> 3;
            default -> 0;
        };

        if (pointsPerItem == 0) {
            player.sendMessage(Component.text("§8[§6🌋 Sajen§8] §cItem ini tidak sesuai sebagai persembahan gotong royong!"));
            return false;
        }

        if (!player.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(material), amount)) {
            player.sendMessage(Component.text("§8[§6🌋 Sajen§8] §cAnda tidak memiliki cukup item tersebut di kantong!"));
            return false;
        }

        // Ambil item dari tas pemain
        player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(material, amount));

        int earnedPoints = pointsPerItem * amount;
        currentOfferings += earnedPoints;

        // Hadiah Rupiah & EXP langsung kepada penyumbang
        double rupiahReward = earnedPoints * 5.0;
        plugin.getEconomy().deposit(player.getUniqueId(), rupiahReward, "Imbalan persembahan pereda murka");
        player.giveExp(earnedPoints * 2);

        // Catat ke database async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO volcano_offerings (uuid, player_name, material, amount, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = plugin.getStorageManager().ensureConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, player.getName());
                ps.setString(3, material.name());
                ps.setInt(4, amount);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[VolcanoManager] Gagal mencatat sajen: " + e.getMessage());
            }
        });

        // Catat ke EventContributionLedger v6
        if (plugin.getEventContributionLedger() != null) {
            String evId = plugin.getVolcanoEncounterDirector() != null && plugin.getVolcanoEncounterDirector().getCurrentEventId() != null
                    ? plugin.getVolcanoEncounterDirector().getCurrentEventId()
                    : "kala_erupsi_manual";
            plugin.getEventContributionLedger().recordContribution(evId, player, "SAJEN_OFFERING", earnedPoints);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        player.sendMessage(Component.text("§8[§6🌋 Sajen§8] §aPersembahan berhasil diserahkan! §e+" + earnedPoints + " Poin Pereda Murka."));
        player.sendMessage(Component.text("§8[§6🌋 Sajen§8] §7Imbalan gotong royong: §6+Rp " + String.format("%.0f", rupiahReward) + " Rupiah Kala§7."));

        // Update boss bar progress berdasarkan donasi
        float progress = Math.min(1.0f, (float) currentOfferings / (float) targetOfferings);
        eventBossBar.progress(1.0f - progress);

        broadcastChat("§8[§6🌋 Sajen§8] §e" + player.getName() + " §7menyumbangkan " + amount + "x " + material.name() +
                "! Progres Pereda Murka: §a" + currentOfferings + "/" + targetOfferings + " Poin");

        // Jika target tercapai, erupsi ditenangkan lebih cepat!
        if (currentOfferings >= targetOfferings && currentPhase == KalaPhase.PUNCAK) {
            broadcastChat("§6§l✦ KEAJAIBAN GOTONG ROYONG: §ePersembahan warga berhasil menenteramkan Murka Gunung!");
            transitionTo(KalaPhase.PEMULIHAN);
        }

        return true;
    }

    // ─────────────────────────────────────────────
    // Atmospheric Particle & Rumble Loop
    // ─────────────────────────────────────────────

    private void startAtmosphericLoop() {
        if (atmosphericTask != null) atmosphericTask.cancel();

        atmosphericTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentPhase == KalaPhase.IDLE) return;

            // Update timer detik
            if (phaseRemainingSeconds > 0) {
                phaseRemainingSeconds--;
                if (phaseTotalSeconds > 0 && currentPhase != KalaPhase.PUNCAK) {
                    float progress = Math.max(0.0f, (float) phaseRemainingSeconds / (float) phaseTotalSeconds);
                    eventBossBar.progress(progress);
                }
            }

            // Loop efek visual ke semua pemain di dunia overworld kala_main
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().getName().equals("kala_main")) {
                    spawnPlayerAtmospherics(p);
                }
            }
        }, 20L, 20L); // Berjalan setiap 1 detik
    }

    private void spawnPlayerAtmospherics(Player p) {
        Location loc = p.getLocation();

        switch (currentPhase) {
            case TANDA_KALA -> {
                // Tiap 8-10 detik: partikel abu tipis
                if (random.nextInt(4) == 0) {
                    p.spawnParticle(Particle.ASH, loc.clone().add(randomOffset(), 2.0, randomOffset()), 12, 3.0, 1.5, 3.0, 0.0);
                }
                // Rumble halus
                if (random.nextInt(10) == 0) {
                    p.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.25f, 0.5f);
                    plugin.getNotificationService().sendActionBar(p, Component.text("§7[≈] Getaran halus terasa di bawah kakimu...", NamedTextColor.GRAY));
                }
            }
            case PERSIAPAN -> {
                // Partikel abu dan asap pekat
                p.spawnParticle(Particle.ASH, loc.clone().add(randomOffset(), 2.5, randomOffset()), 25, 4.0, 2.0, 4.0, 0.01);
                if (random.nextInt(3) == 0) {
                    p.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(randomOffset(), 3.0, randomOffset()), 5, 2.0, 1.0, 2.0, 0.02);
                }
                // Gemuruh lebih sering
                if (random.nextInt(7) == 0) {
                    p.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.5f);
                    p.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.6f);
                    plugin.getNotificationService().sendActionBar(p, Component.text("§6[≈] Kawah Gunung Murka bergemuruh di kejauhan!", NamedTextColor.GOLD));
                }
            }
            case PUNCAK -> {
                // Partikel badai abu tebal & bunga api vulkanik
                p.spawnParticle(Particle.ASH, loc.clone().add(randomOffset(), 3.0, randomOffset()), 40, 5.0, 2.5, 5.0, 0.02);
                p.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(randomOffset(), 3.5, randomOffset()), 8, 3.0, 1.5, 3.0, 0.03);
                if (random.nextInt(2) == 0) {
                    p.spawnParticle(Particle.FLAME, loc.clone().add(randomOffset(), 2.0, randomOffset()), 4, 2.0, 1.0, 2.0, 0.01);
                }
                // Suara petir vulkanik & ledakan kawah
                if (random.nextInt(5) == 0) {
                    p.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.7f);
                    p.playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.8f);
                    plugin.getNotificationService().sendActionBar(p, Component.text("§c🌋 Hujan abu vulkanik sedang berlangsung! Berikan sajen di /gunung", NamedTextColor.RED));
                }
            }
            case PEMULIHAN -> {
                // Partikel spora berkah kesuburan
                if (random.nextInt(3) == 0) {
                    p.spawnParticle(Particle.COMPOSTER, loc.clone().add(randomOffset(), 1.0, randomOffset()), 6, 2.0, 1.0, 2.0);
                }
            }
            default -> {}
        }
    }

    private double randomOffset() {
        return (random.nextDouble() - 0.5) * 8.0;
    }

    // ─────────────────────────────────────────────
    // BossBar Management
    // ─────────────────────────────────────────────

    private void updateBossBar(Component title, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        eventBossBar.name(title);
        eventBossBar.progress(progress);
        eventBossBar.color(color);
        eventBossBar.overlay(overlay);
    }

    public void showBossBarToPlayer(Player player) {
        if (currentPhase != KalaPhase.IDLE) {
            player.showBossBar(eventBossBar);
        }
    }

    public void hideBossBarFromPlayer(Player player) {
        player.hideBossBar(eventBossBar);
    }

    private void showBossBarToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(eventBossBar);
        }
    }

    private void hideBossBarFromAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(eventBossBar);
        }
    }

    // ─────────────────────────────────────────────
    // Scheduler helpers
    // ─────────────────────────────────────────────

    private void scheduleAutoAdvance(int seconds, KalaPhase nextPhase) {
        autoAdvanceTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            log.info("[VolcanoManager] Auto-advance otomatis dari " + currentPhase + " ke " + nextPhase);
            transitionTo(nextPhase);
        }, seconds * 20L);
    }

    private void cancelTasks() {
        if (autoAdvanceTask != null && !autoAdvanceTask.isCancelled()) {
            autoAdvanceTask.cancel();
            autoAdvanceTask = null;
        }
        if (atmosphericTask != null && !atmosphericTask.isCancelled()) {
            atmosphericTask.cancel();
            atmosphericTask = null;
        }
    }

    private void broadcastChat(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(Component.text(message));
        }
        log.info("[VolcanoBroadcast] " + message);
    }

    private KalaPhase getNextPhase(KalaPhase phase) {
        return switch (phase) {
            case IDLE        -> KalaPhase.TANDA_KALA;
            case TANDA_KALA  -> KalaPhase.PERSIAPAN;
            case PERSIAPAN   -> KalaPhase.PUNCAK;
            case PUNCAK      -> KalaPhase.PEMULIHAN;
            case PEMULIHAN   -> KalaPhase.IDLE;
        };
    }
}
