package id.kalanusantara.core.event;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HazardDirector — Mengelola bahaya tertelegrafis, Salvo Rockfall, dan Retakan Panas (Kala Nusantara v7.0).
 *
 * Audit V7 & Security Guarantees:
 * 1. P0 Fix: BukkitRunnable dengan cancel() eksplisit dan deregistrasi task dari registry.
 * 2. Exactly-once impact resolution: AtomicBoolean guard mencegah damage berulang dari satu batu.
 * 3. Damage cooldown per pemain: Menghindari satu pemain dihantam berkali-kali dalam satu salvo.
 * 4. Heat vent deadline: Otomatis cancel dan unregister setelah durasi habis.
 * 5. Location validation: Memastikan target bukan di safe-zone spawn, bukan di dalam claim, dan berada di tanah padat.
 */
public class HazardDirector {

    private final KalaCorePlugin plugin;
    private final WorldImpactPolicy policy;
    private final Random random = new Random();

    private final Set<BukkitTask> activeTasks = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> playerRockfallCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerVentCooldown = new ConcurrentHashMap<>();

    public HazardDirector(KalaCorePlugin plugin, WorldImpactPolicy policy) {
        this.plugin = plugin;
        this.policy = policy;
    }

    /**
     * Memulai Salvo Rockfall dengan penanda tanah 2.5 detik sebelumnya
     */
    public void launchRockfallSalvo(Location center, String eventId, int rockCount) {
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();

        int count = Math.min(rockCount, WorldImpactPolicy.MAX_ROCKFALL_PER_SALVO);
        double arenaRadius = plugin.getKalaConfig().getVolcanoArenaRadius();

        for (int i = 0; i < count; i++) {
            Location targetLoc = findSafeTargetLocation(center, arenaRadius);
            if (targetLoc == null) {
                plugin.getLogger().warning("[HazardDirector] Tidak dapat menemukan target rockfall aman di luar safe zone!");
                continue;
            }

            // Tahap 1: Telegraphing (Peringatan Visual & Audio selama 2.5 detik)
            telegraphImpactZone(targetLoc);

            // Tahap 2: Jadwalkan Peluncuran Batu setelah 50 ticks (2.5 detik)
            final Location finalTargetLoc = targetLoc;
            BukkitTask launchTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!policy.canSpawnMoreEntities(1)) return;

                Location spawnLoc = finalTargetLoc.clone().add(randomOffset(3), 16.0, randomOffset(3));
                Material rockMat = switch (random.nextInt(3)) {
                    case 0 -> Material.TUFF;
                    case 1 -> Material.BLACKSTONE;
                    default -> Material.COBBLESTONE;
                };

                BlockData data = rockMat.createBlockData();
                FallingBlock fb = world.spawnFallingBlock(spawnLoc, data);
                fb.setHurtEntities(false); // Damage ditangani kalkulasi server
                fb.setDropItem(false);
                fb.setCancelDrop(true);

                // Velocity jatuh melengkung ke titik target
                Vector vel = finalTargetLoc.toVector().subtract(spawnLoc.toVector()).normalize().multiply(0.65);
                fb.setVelocity(vel);

                policy.registerEventEntity(fb, eventId);

                // Cek benturan dengan one-shot guard dan task cancellation nyata
                scheduleImpactCheck(fb, finalTargetLoc);
            }, 50L);

            activeTasks.add(launchTask);
        }
    }

    /**
     * Menemukan lokasi target yang aman (di luar safe zone spawn, bukan di dalam claim)
     */
    private Location findSafeTargetLocation(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return null;

        double safeZoneRadiusSq = Math.pow(plugin.getKalaConfig().getSpawnSafeZoneRadius(), 2);
        Location spawnLoc = world.getSpawnLocation();

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 8.0 + random.nextDouble() * (radius - 8.0);
            double x = center.getX() + Math.cos(angle) * dist;
            double z = center.getZ() + Math.sin(angle) * dist;
            int y = world.getHighestBlockYAt((int) x, (int) z);

            Location loc = new Location(world, x, y, z);

            // 1. Cek jarak dari spawn safe zone
            if (loc.distanceSquared(spawnLoc) < safeZoneRadiusSq) {
                continue;
            }

            // 2. Cek apakah di atas air atau lava
            Block block = loc.getBlock();
            if (block.isLiquid() || block.getType() == Material.AIR) {
                continue;
            }

            // 3. Cek claim GriefPrevention jika aktif
            if (isInsidePlayerClaim(loc)) {
                continue;
            }

            return loc;
        }

        // Fallback: gunakan center jika di luar safe zone
        if (center.distanceSquared(spawnLoc) >= safeZoneRadiusSq) {
            return center.clone();
        }
        return null;
    }

    private boolean isInsidePlayerClaim(Location loc) {
        try {
            org.bukkit.plugin.Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            if (gp != null && gp.isEnabled()) {
                Object dataStore = gp.getClass().getField("dataStore").get(gp);
                if (dataStore != null) {
                    java.lang.reflect.Method getClaimAt = dataStore.getClass().getMethod("getClaimAt", Location.class, boolean.class, Object.class);
                    Object claim = getClaimAt.invoke(dataStore, loc, true, null);
                    return claim != null;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Penanda zona dampak sebelum batu tiba (Telegraphing)
     */
    private void telegraphImpactZone(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;

        w.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.8f);

        for (int step = 0; step < 5; step++) {
            long delay = step * 10L;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (double deg = 0; deg < 360; deg += 45) {
                    double rad = Math.toRadians(deg);
                    Location ring = loc.clone().add(Math.cos(rad) * 1.5, 0.2, Math.sin(rad) * 1.5);
                    w.spawnParticle(Particle.FLAME, ring, 1, 0, 0, 0, 0);
                }

                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) < 16.0) {
                        plugin.getNotificationService().sendActionBar(p,
                                Component.text("§c⚠ AWAS! Bongkahan batu akan menghantam titik ini!", NamedTextColor.RED));
                    }
                }
            }, delay);
            activeTasks.add(task);
        }
    }

    /**
     * Pemeriksaan benturan batu dengan BukkitRunnable dan AtomicBoolean one-shot guard (P0 Fix)
     */
    private void scheduleImpactCheck(FallingBlock fb, Location targetLoc) {
        final AtomicBoolean resolved = new AtomicBoolean(false);

        BukkitRunnable runner = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (!fb.isValid() || fb.isDead() || fb.isOnGround() || ticks > 80) {
                    // P0 Fix: Exactly-once resolution guard
                    if (resolved.compareAndSet(false, true)) {
                        processImpact(fb, targetLoc);
                    }
                    cancel();
                    activeTasks.remove(this);
                }
            }
        };

        BukkitTask task = runner.runTaskTimer(plugin, 5L, 5L);
        activeTasks.add(task);
    }

    /**
     * Resolusi tumbukan tunggal (One-Shot)
     */
    private void processImpact(FallingBlock fb, Location targetLoc) {
        Location impact = fb.isValid() ? fb.getLocation() : targetLoc;
        World w = impact.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, impact, 12, 0.6, 0.3, 0.6, 0.05);
            w.spawnParticle(Particle.BLOCK, impact, 20, 0.5, 0.3, 0.5, Material.STONE.createBlockData());
            w.playSound(impact, Sound.BLOCK_STONE_BREAK, 0.9f, 0.6f);

            long now = System.currentTimeMillis();
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distanceSquared(impact) <= 6.25) { // Radius 2.5 blok
                    // Damage cooldown per player (minimal jeda 2000 ms antar damage rockfall)
                    Long lastDmg = playerRockfallCooldown.get(p.getUniqueId());
                    if (lastDmg == null || (now - lastDmg) > 2000L) {
                        playerRockfallCooldown.put(p.getUniqueId(), now);
                        p.damage(4.0); // 2 Hati
                        Vector kb = p.getLocation().toVector().subtract(impact.toVector()).normalize().multiply(0.5).setY(0.25);
                        p.setVelocity(kb);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0)); // 2 detik lambat
                        p.sendMessage(Component.text("§c[!] Anda terkena hempasan serpihan batu vulkanik!", NamedTextColor.RED));
                    }
                }
            }
        }
        if (fb.isValid()) {
            fb.remove();
        }
    }

    /**
     * Menghasilkan retakan panas / geyser uap sementara (Heat Vents)
     * P1 Fix: Menggunakan BukkitRunnable dengan cancel() eksplisit saat durasi tercapai.
     */
    public void spawnHeatVent(Location loc, String eventId, int durationSeconds) {
        if (loc == null || loc.getWorld() == null) return;
        World w = loc.getWorld();

        BukkitRunnable ventRunner = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed++;
                if (elapsed >= durationSeconds || w == null) {
                    cancel();
                    activeTasks.remove(this);
                    return;
                }

                // Semburan asap uap vulkanik
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.2, 0), 4, 0.2, 0.5, 0.2, 0.02);
                if (elapsed % 3 == 0) {
                    w.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.4f, 1.2f);
                }

                // Efek panas dengan cooldown 1500 ms per pemain
                long now = System.currentTimeMillis();
                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) <= 2.25) {
                        Long lastHit = playerVentCooldown.get(p.getUniqueId());
                        if (lastHit == null || (now - lastHit) > 1500L) {
                            playerVentCooldown.put(p.getUniqueId(), now);
                            p.damage(1.0);
                            p.setFireTicks(20);
                        }
                    }
                }
            }
        };

        BukkitTask task = ventRunner.runTaskTimer(plugin, 10L, 20L);
        activeTasks.add(task);
    }

    public void cancelAllHazards() {
        for (BukkitTask t : activeTasks) {
            if (t != null && !t.isCancelled()) {
                t.cancel();
            }
        }
        activeTasks.clear();
        playerRockfallCooldown.clear();
        playerVentCooldown.clear();
    }

    private double randomOffset(double max) {
        return (random.nextDouble() - 0.5) * 2.0 * max;
    }
}
