package id.kalanusantara.core.event;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.kala.KalaEventManager;
import id.kalanusantara.core.notify.KalaNotificationService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * SeasonalEventDirector — Pengarah Mikro-Event Bencana Musiman Berbasis Gameplay (Kala Nusantara v7.0).
 *
 * Audit V7:
 * 1. P1 Fix: objectiveTarget dinamis yang selalu sesuai dengan jumlah objective yang benar-benar di-spawn.
 * 2. P1 Fix: Weather Lease & Snapshot persis mengembalikan cuaca awal (hasStorm, isThundering, duration).
 * 3. P1 Fix: Eksklusivitas event (tidak boleh bentrok dengan erupsi Gunung Murka).
 * 4. P1 Fix: Sampel abu vulkanik diberi tag persisten PersistentDataContainer (bukan plain item).
 * 5. P1 Fix: Cleanup terisolasi per eventId.
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

    private int objectiveTarget = 0;
    private int objectiveCurrent = 0;

    // Snapshot cuaca awal
    private boolean savedStormState = false;
    private boolean savedThunderingState = false;
    private int savedWeatherDuration = 6000;

    private BukkitTask eventTickTask;
    private BukkitTask atmosphericTask;
    private BossBar seasonalBossBar;

    private final List<Location> activeHotspots = Collections.synchronizedList(new ArrayList<>());
    private final Set<Item> spawnedEventItems = ConcurrentHashMap.newKeySet();

    private final NamespacedKey CUSTOM_ITEM_KEY;

    public SeasonalEventDirector(KalaCorePlugin plugin, WorldImpactPolicy policy, EventContributionLedger ledger) {
        this.plugin = plugin;
        this.policy = policy;
        this.ledger = ledger;
        this.log = plugin.getLogger();
        this.CUSTOM_ITEM_KEY = new NamespacedKey(plugin, "custom_item_id");
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
        if (type == SeasonalEventType.NONE) return false;

        // P1 Fix: Eksklusivitas event — jangan tumpang tindih dengan erupsi Gunung Murka
        if (plugin.getKalaEventManager() != null && plugin.getKalaEventManager().getCurrentPhase() != KalaEventManager.KalaPhase.IDLE) {
            log.warning("[SeasonalDirector] Ditolak: Tidak dapat memulai event musiman saat Gunung Murka sedang aktif!");
            return false;
        }

        if (activeEvent != SeasonalEventType.NONE) {
            stopEvent();
        }

        this.activeEvent = type;
        this.currentEventId = "season_" + type.name().toLowerCase() + "_" + System.currentTimeMillis();
        this.objectiveCurrent = 0;
        this.eventTotalSeconds = 300; // 5 menit
        this.eventRemainingSeconds = eventTotalSeconds;

        // Setup Skenario & kalkulasi target nyata
        setupEventScenario(type);

        log.info("[SeasonalDirector] Memulai Event Musiman: " + type.getSimpleName() +
                " (ID: " + currentEventId + ", Target Realistis: " + objectiveTarget + ")");

        // Notifikasi dan Pengumuman
        announceStart(type);
        updateBossBar();
        showBossBarToAll();

        startEventLoops();
        return true;
    }

    public void stopEvent() {
        if (activeEvent == SeasonalEventType.NONE) return;

        SeasonalEventType ended = activeEvent;
        String endedId = currentEventId;

        // Batalkan Task
        if (eventTickTask != null) {
            eventTickTask.cancel();
            eventTickTask = null;
        }
        if (atmosphericTask != null) {
            atmosphericTask.cancel();
            atmosphericTask = null;
        }

        hideBossBarFromAll();
        cleanupSpawnedHotspots(endedId);

        // P1 Fix: Kembalikan kondisi cuaca persis seperti sebelum event
        if (ended == SeasonalEventType.BADAI_SELAT) {
            World world = Bukkit.getWorld("kala_main");
            if (world != null) {
                world.setStorm(savedStormState);
                world.setThundering(savedThunderingState);
                world.setWeatherDuration(savedWeatherDuration);
            }
        }

        // Tentukan hasil (Evaluasi Gotong Royong)
        boolean success = objectiveCurrent >= objectiveTarget && objectiveTarget > 0;
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
        spawnedEventItems.clear();

        World world = Bukkit.getWorld("kala_main");
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            this.objectiveTarget = 0;
            return;
        }

        Location spawnLoc = world.getSpawnLocation();
        int online = Bukkit.getOnlinePlayers().size();

        if (type == SeasonalEventType.BADAI_SELAT) {
            // Simpan snapshot cuaca awal
            savedStormState = world.hasStorm();
            savedThunderingState = world.isThundering();
            savedWeatherDuration = world.getWeatherDuration();

            world.setStorm(true);
            world.setWeatherDuration(eventTotalSeconds * 20);

            // Jumlah peti dinamis berdasarkan pemain online (antara 6 s.d. 14)
            int crateCount = Math.min(14, Math.max(6, online * 2 + 6));

            for (int i = 0; i < crateCount; i++) {
                double x = spawnLoc.getX() + (random.nextDouble() - 0.5) * 100;
                Location loc = findCoastalLocation(spawnLoc, 80.0);
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

            // P1 Fix: Target objective persis sama dengan jumlah peti yang berhasil di-spawn!
            this.objectiveTarget = spawnedEventItems.size();

        } else if (type == SeasonalEventType.ABU_DAN_BARA) {
            // Jumlah hotspot dinamis (antara 6 s.d. 12)
            int hotspotCount = Math.min(12, Math.max(6, online * 2 + 6));

            for (int i = 0; i < hotspotCount; i++) {
                double x = spawnLoc.getX() + (random.nextDouble() - 0.5) * 80;
                double z = spawnLoc.getZ() + (random.nextDouble() - 0.5) * 80;
                int y = world.getHighestBlockYAt((int) x, (int) z);
                Location loc = new Location(world, Math.floor(x) + 0.5, y + 1.0, Math.floor(z) + 0.5);
                activeHotspots.add(loc);

                // Audit V8: Spawn marker fisik untuk kemudahan tap sentuh pada layar HP Bedrock
                try {
                    org.bukkit.entity.ArmorStand marker = world.spawn(loc, org.bukkit.entity.ArmorStand.class, as -> {
                        as.setVisible(false);
                        as.setSmall(true);
                        as.setMarker(false);
                        as.setGravity(false);
                        as.setCustomNameVisible(true);
                        as.customName(Component.text("🔥 Bara Kawah §7(Klik)", NamedTextColor.GOLD));
                        if (as.getEquipment() != null) {
                            as.getEquipment().setHelmet(new ItemStack(Material.MAGMA_BLOCK));
                        }
                    });
                    policy.registerEventEntity(marker, currentEventId);
                } catch (Exception ignored) {}
            }

            // P1 Fix: Target objective persis sama dengan jumlah hotspot aktif!
            this.objectiveTarget = activeHotspots.size();
        }
    }

    private Location findCoastalLocation(Location origin, double radius) {
        World world = origin.getWorld();
        if (world == null) return origin;

        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 12.0 + random.nextDouble() * (radius - 12.0);
            int x = (int) (origin.getX() + Math.cos(angle) * dist);
            int z = (int) (origin.getZ() + Math.sin(angle) * dist);
            int y = world.getHighestBlockYAt(x, z);

            Block b = world.getBlockAt(x, y, z);
            boolean isNearWater = b.isLiquid() ||
                world.getBlockAt(x + 1, y, z).isLiquid() ||
                world.getBlockAt(x - 1, y, z).isLiquid() ||
                world.getBlockAt(x, y, z + 1).isLiquid() ||
                world.getBlockAt(x, y, z - 1).isLiquid() ||
                b.getType() == Material.SAND || b.getType() == Material.GRAVEL;

            if (isNearWater) {
                return new Location(world, x + 0.5, Math.max(62.0, y + 1.0), z + 0.5);
            }
        }
        return origin.clone().add(15.0, 1.0, 15.0);
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

        // Atmospheric Task: Partikel dan suara atmosferik terukur
        atmosphericTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld("kala_main");
            if (world == null && !Bukkit.getWorlds().isEmpty()) {
                world = Bukkit.getWorlds().get(0);
            }
            if (world == null) return;

            if (activeEvent == SeasonalEventType.BADAI_SELAT) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(world)) {
                        // Partikel hemat
                        player.spawnParticle(Particle.CLOUD, player.getLocation().add(0, 2, 0), 10, 8, 2, 8, 0.03);
                        if (random.nextInt(8) == 0) {
                            player.playSound(player.getLocation(), Sound.WEATHER_RAIN_ABOVE, 0.6f, 0.6f);
                        }
                    }
                }
            } else if (activeEvent == SeasonalEventType.ABU_DAN_BARA) {
                for (Location loc : activeHotspots) {
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.3, 0.3, 0.3, 0.02);
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 2, 0.2, 0.4, 0.2, 0.01);
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(world)) {
                        player.spawnParticle(Particle.ASH, player.getLocation().add(0, 3, 0), 12, 6, 2, 6, 0.0);
                    }
                }
            }
        }, 20L, 20L);
    }

    // ─────────────────────────────────────────────
    // Interaksi Pemain & Objective
    // ─────────────────────────────────────────────

    /**
     * Dipanggil saat pemain berinteraksi dengan titik bara semu (radius ketat <= 2.5 blok)
     */
    public boolean handlePlayerInteraction(Player player, Location targetLoc) {
        if (activeEvent != SeasonalEventType.ABU_DAN_BARA || targetLoc == null) return false;

        for (Iterator<Location> it = activeHotspots.iterator(); it.hasNext(); ) {
            Location hotspot = it.next();
            if (hotspot.getWorld().equals(targetLoc.getWorld()) && hotspot.distanceSquared(targetLoc) <= 6.25) { // Radius 2.5 blok
                it.remove();
                objectiveCurrent++;

                // Visual & Audio pemadaman bara
                targetLoc.getWorld().spawnParticle(Particle.SMOKE, targetLoc, 15, 0.4, 0.4, 0.4, 0.04);
                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.9f, 1.2f);

                // Audit V8: Hapus marker fisik di titik ini
                for (org.bukkit.entity.Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc, 2.5, 2.5, 2.5)) {
                    if (policy.isEventEntity(e) && currentEventId.equals(policy.getEventId(e))) {
                        e.remove();
                    }
                }

                // Catat ke ledger
                ledger.recordContribution(currentEventId, player, "PADAMKAN_BARA", 15);
                player.sendMessage(Component.text("§8[§6🔥 Abu & Bara§8] §aBara semu berhasil dipadamkan! (+15 Poin Kontribusi)", NamedTextColor.GREEN));

                // P1 Fix: Sampel abu vulkanik diberi metadata PDC khusus
                ItemStack ashSample = new ItemStack(Material.GUNPOWDER);
                ItemMeta meta = ashSample.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("🌋 Sampel Abu Vulkanik", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                    meta.lore(List.of(
                            Component.text("§7Material penelitian asli dari endapan kawah Murka.", NamedTextColor.GRAY),
                            Component.text("§8KalaCore Tag: volcano_ash_sample", NamedTextColor.DARK_GRAY)
                    ));
                    meta.getPersistentDataContainer().set(CUSTOM_ITEM_KEY, PersistentDataType.STRING, "volcano_ash_sample");
                    ashSample.setItemMeta(meta);
                }
                player.getInventory().addItem(ashSample);

                updateBossBar();
                return true;
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

        // Hadiah logistik terukur
        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 4));
        player.getInventory().addItem(new ItemStack(Material.SWEET_BERRIES, 6));

        updateBossBar();
    }

    private void cleanupSpawnedHotspots(String eventId) {
        for (Item item : spawnedEventItems) {
            if (item != null && item.isValid()) {
                item.remove();
            }
        }
        spawnedEventItems.clear();
        activeHotspots.clear();

        // P1 Fix: Cleanup terisolasi hanya untuk eventId musiman ini
        if (eventId != null) {
            policy.cleanupEvent(eventId);
        }
    }

    // ─────────────────────────────────────────────
    // BossBar & Notifikasi
    // ─────────────────────────────────────────────

    private void updateBossBar() {
        if (activeEvent == SeasonalEventType.NONE) return;

        float progress = objectiveTarget > 0 ? Math.min(1.0f, (float) objectiveCurrent / (float) objectiveTarget) : 0f;
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
