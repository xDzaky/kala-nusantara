package id.kalanusantara.core.event;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.notify.KalaNotificationService;
import id.kalanusantara.core.season.SeasonManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * SeasonalEventDirector — Pengarah Mikro-Event Bencana Musiman Berbasis Gameplay (Kala Nusantara v6.0).
 *
 * Mengikuti kalender SeasonManager KalaCore yang ada (8 minggu).
 * Mengatur 2 MVP Mikro-Event:
 * 1. BADAI_SELAT: Angin, kabut pesisir, mengamankan peti hanyut (driftwood) & menyalakan sinyal suar.
 * 2. ABU_DAN_BARA: Abu vulkanik, titik bara semu yang perlu dipadamkan, & pengumpulan sampel abu.
 *
 * Sesuai WorldImpactPolicy Level 0: Nol kerusakan blok pada kala_main, murni gameplay partisipatif.
 */
public class SeasonalEventDirector {

    public enum SeasonalEventType {
        NONE("Tidak Ada", "§7Tidak Ada"),
        BADAI_SELAT("Badai Selat", "§b🌊 Badai Selat"),
        ABU_DAN_BARA("Abu dan Bara", "§6🔥 Abu dan Bara");

        private final String simpleName;
        private final String displayName;

        SeasonalEventType(String simpleName, String displayName) {
            this.simpleName = simpleName;
            this.displayName = displayName;
        }

        public String getSimpleName() { return simpleName; }
        public String getDisplayName() { return displayName; }
    }

    private final KalaCorePlugin plugin;
    private final WorldImpactPolicy policy;
    private final EventContributionLedger ledger;
    private final Logger log;
    private final Random random = new Random();

    private SeasonalEventType activeEvent = SeasonalEventType.NONE;
    private String currentEventId = null;
    private int eventRemainingSeconds = 0;
    private int eventTotalSeconds = 0;

    private int objectiveTarget = 15;
    private int objectiveCurrent = 0;

    private BukkitTask eventTickTask;
    private BukkitTask atmosphericTask;
    private BossBar seasonalBossBar;

    // Lokasi hotspot aktif (misal bara semu atau peti hanyut)
    private final List<Location> activeHotspots = Collections.synchronizedList(new ArrayList<>());
    private final Set<Item> spawnedEventItems = ConcurrentHashMap.newKeySet();

    public SeasonalEventDirector(KalaCorePlugin plugin, WorldImpactPolicy policy, EventContributionLedger ledger) {
        this.plugin = plugin;
        this.policy = policy;
        this.ledger = ledger;
        this.log = plugin.getLogger();
        initBossBar();
    }

    private void initBossBar() {
        seasonalBossBar = BossBar.bossBar(
                Component.text("Musim Nusantara", NamedTextColor.AQUA),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
    }

    // ─────────────────────────────────────────────
    // Kontrol Event Musiman
    // ─────────────────────────────────────────────

    public boolean startEvent(SeasonalEventType type) {
        if (activeEvent != SeasonalEventType.NONE) {
            stopEvent();
        }

        if (type == SeasonalEventType.NONE) return false;

        this.activeEvent = type;
        this.currentEventId = "season_" + type.name().toLowerCase() + "_" + System.currentTimeMillis();
        this.objectiveCurrent = 0;
        this.objectiveTarget = Math.max(10, Bukkit.getOnlinePlayers().size() * 4);
        this.eventTotalSeconds = 300; // 5 menit
        this.eventRemainingSeconds = eventTotalSeconds;

        log.info("[SeasonalDirector] Memulai Event Musiman: " + type.getSimpleName() + " (ID: " + currentEventId + ")");

        // Notifikasi dan Pengumuman
        announceStart(type);
        updateBossBar();
        showBossBarToAll();

        // Spawn Hotspot dan Mulai Loop
        setupEventScenario(type);
        startEventLoops();

        return true;
    }

    public void stopEvent() {
        if (activeEvent == SeasonalEventType.NONE) return;

        SeasonalEventType ended = activeEvent;
        String endedId = currentEventId;

        // Batalkan Task & Bersihkan
        if (eventTickTask != null) eventTickTask.cancel();
        if (atmosphericTask != null) atmosphericTask.cancel();

        hideBossBarFromAll();
        cleanupSpawnedHotspots();

        // Kembalikan cuaca jika Badai Selat
        if (ended == SeasonalEventType.BADAI_SELAT) {
            World world = Bukkit.getWorld("kala_main");
            if (world != null) {
                world.setStorm(false);
                world.setThundering(false);
            }
        }

        // Tentukan hasil (Evaluasi Gotong Royong)
        boolean success = objectiveCurrent >= objectiveTarget;
        announceEnd(ended, endedId, success);

        activeEvent = SeasonalEventType.NONE;
        currentEventId = null;
        log.info("[SeasonalDirector] Event musiman selesai: " + ended.getSimpleName() + " (Sukses: " + success + ")");
    }

    public SeasonalEventType getActiveEvent() {
        return activeEvent;
    }

    public String getCurrentEventId() {
        return currentEventId;
    }

    // ─────────────────────────────────────────────
    // Setup Skenario & Hotspot
    // ─────────────────────────────────────────────

    private void setupEventScenario(SeasonalEventType type) {
        activeHotspots.clear();
        World world = Bukkit.getWorld("kala_main");
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) return;

        Location spawnLoc = world.getSpawnLocation();

        if (type == SeasonalEventType.BADAI_SELAT) {
            world.setStorm(true);
            world.setWeatherDuration(eventTotalSeconds * 20);

            // Sebar 8 titik peti hanyut / kayu apung di sekitar pesisir / spawn
            for (int i = 0; i < 8; i++) {
                double x = spawnLoc.getX() + (random.nextDouble() - 0.5) * 80;
                double z = spawnLoc.getZ() + (random.nextDouble() - 0.5) * 80;
                int y = world.getHighestBlockYAt((int) x, (int) z);
                Location loc = new Location(world, x, y + 1, z);
                activeHotspots.add(loc);

                // Spawn Driftwood Crate Item
                ItemStack crate = new ItemStack(Material.CHEST);
                ItemMeta meta = crate.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("📦 Peti Hanyut Selat", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
                    meta.lore(List.of(
                            Component.text("§7Peti logistik terombang-ambing akibat ombak badai.", NamedTextColor.GRAY),
                            Component.text("§eKlik kanan / ambil untuk menyelamatkan!", NamedTextColor.YELLOW)
                    ));
                    crate.setItemMeta(meta);
                }

                Item itemEntity = world.dropItem(loc, crate);
                itemEntity.setGlowing(true);
                policy.registerEventEntity(itemEntity, currentEventId);
                spawnedEventItems.add(itemEntity);
            }
        } else if (type == SeasonalEventType.ABU_DAN_BARA) {
            // Sebar 10 titik bara semu & sampel abu di sekitar bukit / daratan
            for (int i = 0; i < 10; i++) {
                double x = spawnLoc.getX() + (random.nextDouble() - 0.5) * 60;
                double z = spawnLoc.getZ() + (random.nextDouble() - 0.5) * 60;
                int y = world.getHighestBlockYAt((int) x, (int) z);
                Location loc = new Location(world, x, y + 1, z);
                activeHotspots.add(loc);
            }
        }
    }

    private void startEventLoops() {
        // Tick Task: Hitung mundur dan update BossBar
        eventTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (eventRemainingSeconds <= 0) {
                stopEvent();
                return;
            }
            eventRemainingSeconds--;
            updateBossBar();
        }, 20L, 20L);

        // Atmospheric Task: Partikel dan suara atmosferik
        atmosphericTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld("kala_main");
            if (world == null && !Bukkit.getWorlds().isEmpty()) {
                world = Bukkit.getWorlds().get(0);
            }
            if (world == null) return;

            if (activeEvent == SeasonalEventType.BADAI_SELAT) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(world)) {
                        // Kabut badai & desau angin
                        player.spawnParticle(Particle.CLOUD, player.getLocation().add(0, 2, 0), 20, 10, 2, 10, 0.05);
                        if (random.nextInt(6) == 0) {
                            player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 0.7f, 0.6f);
                        }
                    }
                }
            } else if (activeEvent == SeasonalEventType.ABU_DAN_BARA) {
                // Efek titik bara semu pada hotspots
                for (Location loc : activeHotspots) {
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.4, 0.4, 0.4, 0.02);
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 3, 0.2, 0.6, 0.2, 0.01);
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(world)) {
                        player.spawnParticle(Particle.ASH, player.getLocation().add(0, 3, 0), 25, 8, 3, 8, 0.0);
                    }
                }
            }
        }, 20L, 20L);
    }

    // ─────────────────────────────────────────────
    // Interaksi Pemain & Objective
    // ─────────────────────────────────────────────

    /**
     * Dipanggil saat pemain mengambil peti hanyut atau memadamkan bara
     */
    public boolean handlePlayerInteraction(Player player, Location targetLoc) {
        if (activeEvent == SeasonalEventType.NONE || targetLoc == null) return false;

        if (activeEvent == SeasonalEventType.ABU_DAN_BARA) {
            // Cek apakah lokasi dekat dengan salah satu hotspot bara semu
            for (Iterator<Location> it = activeHotspots.iterator(); it.hasNext(); ) {
                Location hotspot = it.next();
                if (hotspot.getWorld().equals(targetLoc.getWorld()) && hotspot.distanceSquared(targetLoc) <= 9.0) {
                    it.remove();
                    objectiveCurrent++;

                    // Visual & Audio pemadaman bara
                    targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 20, 0.5, 0.5, 0.5, 0.05);
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.2f);

                    // Catat ke ledger
                    ledger.recordContribution(currentEventId, player, "PADAMKAN_BARA", 15);
                    player.sendMessage(Component.text("§8[§6🔥 Abu & Bara§8] §aBara semu berhasil dipadamkan! (+15 Poin Kontribusi)", NamedTextColor.GREEN));

                    // Reward sampel abu
                    ItemStack ashSample = new ItemStack(Material.GUNPOWDER);
                    ItemMeta meta = ashSample.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("🌋 Sampel Abu Vulkanik", NamedTextColor.GOLD));
                        meta.lore(List.of(Component.text("§7Material penelitian yang berguna untuk kesuburan.", NamedTextColor.GRAY)));
                        ashSample.setItemMeta(meta);
                    }
                    player.getInventory().addItem(ashSample);

                    updateBossBar();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Dipanggil saat pemain memungut Peti Hanyut (Badai Selat)
     */
    public void handleCratePickup(Player player, Item item) {
        if (activeEvent != SeasonalEventType.BADAI_SELAT) return;
        if (!spawnedEventItems.contains(item)) return;

        spawnedEventItems.remove(item);
        objectiveCurrent++;

        // Ledger kontribusi
        ledger.recordContribution(currentEventId, player, "SELAMATKAN_PETI", 20);

        player.sendMessage(Component.text("§8[§b🌊 Badai Selat§8] §aPeti hanyut berhasil diselamatkan! (+20 Poin Kontribusi)", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        // Hadiah rempah/logistik langsung
        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 4));
        player.getInventory().addItem(new ItemStack(Material.SWEET_BERRIES, 8));

        updateBossBar();
    }

    private void cleanupSpawnedHotspots() {
        for (Item item : spawnedEventItems) {
            if (item != null && item.isValid()) {
                item.remove();
            }
        }
        spawnedEventItems.clear();
        activeHotspots.clear();
    }

    // ─────────────────────────────────────────────
    // BossBar & Notifikasi
    // ─────────────────────────────────────────────

    private void updateBossBar() {
        if (activeEvent == SeasonalEventType.NONE) return;

        float progress = Math.min(1.0f, (float) objectiveCurrent / Math.max(1, objectiveTarget));
        int minutes = eventRemainingSeconds / 60;
        int seconds = eventRemainingSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        Component title = switch (activeEvent) {
            case BADAI_SELAT -> Component.text("🌊 BADAI SELAT: Kargo Pesisir Hanyut (" + objectiveCurrent + "/" + objectiveTarget + ") [" + timeStr + "]", NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
            case ABU_DAN_BARA -> Component.text("🔥 ABU & BARA: Padamkan Titik Hotspot (" + objectiveCurrent + "/" + objectiveTarget + ") [" + timeStr + "]", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
            default -> Component.text("Musim Nusantara", NamedTextColor.WHITE);
        };

        BossBar.Color color = switch (activeEvent) {
            case BADAI_SELAT -> BossBar.Color.BLUE;
            case ABU_DAN_BARA -> BossBar.Color.YELLOW;
            default -> BossBar.Color.WHITE;
        };

        seasonalBossBar.name(title);
        seasonalBossBar.progress(progress);
        seasonalBossBar.color(color);
    }

    private void showBossBarToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(seasonalBossBar);
        }
    }

    private void hideBossBarFromAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(seasonalBossBar);
        }
    }

    private void announceStart(SeasonalEventType type) {
        Component title;
        Component subtitle;
        String chatMsg;

        if (type == SeasonalEventType.BADAI_SELAT) {
            title = Component.text("🌊 BADAI SELAT MENERJANG 🌊", NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Amankan peti kargo yang hanyut di perairan dan pesisir!", NamedTextColor.WHITE);
            chatMsg = "§8[§b🌊 Badai Selat§8] §bAngin kencang menerjang pesisir! Peti kargo logistik terombang-ambing di pantai. Segera selamatkan logistik gotong royong!";
        } else {
            title = Component.text("🔥 ABU & BARA VULKANIK 🔥", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Padamkan bara semu dan kumpulkan sampel abu lereng!", NamedTextColor.YELLOW);
            chatMsg = "§8[§6🔥 Abu & Bara§8] §eEmbusan abu dan serpihan bara panas turun di sekitar perbukitan. Dekati bara semu untuk memadamkannya sebelum merusak kebun!";
        }

        plugin.getNotificationService().broadcastTitle(
                KalaNotificationService.Priority.STORY_EVENT,
                title, subtitle,
                Sound.ITEM_TRIDENT_THUNDER, 0.8f, 0.8f
        );
        Bukkit.broadcast(Component.text(chatMsg));
    }

    private void announceEnd(SeasonalEventType type, String eventId, boolean success) {
        if (success) {
            Bukkit.broadcast(Component.empty());
            Bukkit.broadcast(Component.text("═══════════════════════════════════════", NamedTextColor.GREEN));
            Bukkit.broadcast(Component.text("🏆 KEMENANGAN GOTONG ROYONG: " + type.getSimpleName().toUpperCase(), NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
            Bukkit.broadcast(Component.text("§fTarget bersama tercapai (" + objectiveCurrent + "/" + objectiveTarget + ")! Cuaca telah kembali normal."));
            Bukkit.broadcast(Component.text("§eGunakan §a/kala claim-event " + eventId + " §euntuk mengambil reward partisipasi!"));
            Bukkit.broadcast(Component.text("═══════════════════════════════════════", NamedTextColor.GREEN));
            Bukkit.broadcast(Component.empty());

            // Reward sound
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            }
        } else {
            Bukkit.broadcast(Component.empty());
            Bukkit.broadcast(Component.text("═══════════════════════════════════════", NamedTextColor.GRAY));
            Bukkit.broadcast(Component.text("⌛ EVENT SELESAI: " + type.getSimpleName().toUpperCase(), NamedTextColor.YELLOW));
            Bukkit.broadcast(Component.text("§7Sebagian kargo/bara belum tertangani tepat waktu, namun badai telah berlalu."));
            Bukkit.broadcast(Component.text("§ePemain yang berpartisipasi tetap dapat mengklaim reward proporsional: §a/kala claim-event " + eventId));
            Bukkit.broadcast(Component.text("═══════════════════════════════════════", NamedTextColor.GRAY));
            Bukkit.broadcast(Component.empty());
        }
    }
}
