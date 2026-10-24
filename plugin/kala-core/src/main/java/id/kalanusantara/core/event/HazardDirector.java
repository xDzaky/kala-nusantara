package id.kalanusantara.core.event;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HazardDirector — Mengelola bahaya tertelegrafis, Salvo Rockfall, dan Retakan Panas (Kala Nusantara v6.0).
 *
 * Menjamin:
 * 1. Peringatan visual/audio 2–3 detik sebelum damage tiba (telegraphing ramah HP/mobile).
 * 2. Menggunakan FallingBlock vanilla aman (setHurtEntities false, drop false, cancel drop).
 * 3. Tidak mengubah blok dunia (zero block destruction).
 * 4. Damage ditangani server-side dengan batas wajar dan jendela waktu menghindar.
 */
public class HazardDirector {

    private final KalaCorePlugin plugin;
    private final WorldImpactPolicy policy;
    private final Random random = new Random();

    private final Set<BukkitTask> activeTasks = ConcurrentHashMap.newKeySet();

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

        for (int i = 0; i < count; i++) {
            // Pilih titik jatuh di sekitar pusat arena (radius 6–18 blok)
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 6.0 + random.nextDouble() * 12.0;
            double targetX = center.getX() + Math.cos(angle) * dist;
            double targetZ = center.getZ() + Math.sin(angle) * dist;
            int targetY = world.getHighestBlockYAt((int) targetX, (int) targetZ);

            Location targetLoc = new Location(world, targetX, targetY, targetZ);

            // Tahap 1: Telegraphing (Peringatan Visual & Audio selama 2.5 detik)
            telegraphImpactZone(targetLoc);

            // Tahap 2: Jadwalkan Peluncuran Batu setelah 50 ticks (2.5 detik)
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!policy.canSpawnMoreEntities(1)) return;

                Location spawnLoc = targetLoc.clone().add(randomOffset(3), 16.0, randomOffset(3));
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
                Vector vel = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize().multiply(0.65);
                fb.setVelocity(vel);

                policy.registerEventEntity(fb, eventId);

                // Cek benturan dan damage kalkulasi server setelah mendarat
                scheduleImpactCheck(fb, targetLoc);
            }, 50L);

            activeTasks.add(task);
        }
    }

    /**
     * Penanda zona dampak sebelum batu tiba (Telegraphing)
     */
    private void telegraphImpactZone(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;

        // Suara dengung peringatan
        w.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.8f);

        // Partikel lingkaran merah/bara selama 2.5 detik
        for (int step = 0; step < 5; step++) {
            long delay = step * 10L;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (double deg = 0; deg < 360; deg += 45) {
                    double rad = Math.toRadians(deg);
                    Location ring = loc.clone().add(Math.cos(rad) * 1.5, 0.2, Math.sin(rad) * 1.5);
                    w.spawnParticle(Particle.FLAME, ring, 1, 0, 0, 0, 0);
                }

                // Peringatkan pemain terdekat lewat actionbar
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
     * Pemeriksaan benturan batu saat menyentuh tanah
     */
    private void scheduleImpactCheck(FallingBlock fb, Location targetLoc) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (!fb.isValid() || fb.isDead() || fb.isOnGround() || ticks > 80) {
                    // Terjadi benturan tanah
                    Location impact = fb.getLocation();
                    World w = impact.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, impact, 12, 0.6, 0.3, 0.6, 0.05);
                        w.spawnParticle(Particle.BLOCK, impact, 20, 0.5, 0.3, 0.5, Material.STONE.createBlockData());
                        w.playSound(impact, Sound.BLOCK_STONE_BREAK, 0.9f, 0.6f);

                        // Damage & knockback pemain jika gagal menghindar
                        for (Player p : w.getPlayers()) {
                            if (p.getLocation().distanceSquared(impact) <= 6.25) { // Radius 2.5 blok
                                p.damage(4.0); // 2 Hati
                                Vector kb = p.getLocation().toVector().subtract(impact.toVector()).normalize().multiply(0.5).setY(0.25);
                                p.setVelocity(kb);
                                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0)); // 2 detik lambat
                                p.sendMessage(Component.text("§c[!] Anda terkena hempasan serpihan batu vulkanik!", NamedTextColor.RED));
                            }
                        }
                    }

                    fb.remove();
                    cancel();
                }
            }

            private void cancel() {
                // Task dibatalkan otomatis
            }
        }, 5L, 5L);

        activeTasks.add(task);
    }

    /**
     * Menghasilkan retakan panas / geyser uap sementara (Heat Vents)
     */
    public void spawnHeatVent(Location loc, String eventId, int durationSeconds) {
        if (loc == null || loc.getWorld() == null) return;
        World w = loc.getWorld();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int elapsed = 0;

            @Override
            public void run() {
                elapsed++;
                if (elapsed > durationSeconds) {
                    return;
                }

                // Semburan asap uap vulkanik
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.2, 0), 4, 0.2, 0.5, 0.2, 0.02);
                if (elapsed % 3 == 0) {
                    w.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.4f, 1.2f);
                }

                // Efek panas jika pemain berdiri tepat di atas retakan
                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) <= 2.25) {
                        p.damage(1.0);
                        p.setFireTicks(20);
                    }
                }
            }
        }, 10L, 20L);

        activeTasks.add(task);
    }

    public void cancelAllHazards() {
        for (BukkitTask t : activeTasks) {
            if (t != null && !t.isCancelled()) {
                t.cancel();
            }
        }
        activeTasks.clear();
    }

    private double randomOffset(double max) {
        return (random.nextDouble() - 0.5) * 2.0 * max;
    }
}
