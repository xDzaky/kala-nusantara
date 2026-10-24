package id.kalanusantara.core.listeners;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import id.kalanusantara.core.profession.KalaProfession;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.Random;

/**
 * ProfessionPerkListener — Memberikan efek pasif dan bonus gameplay sesuai profesi pemain.
 */
public class ProfessionPerkListener implements Listener {

    private final KalaCorePlugin plugin;
    private final Random random = new Random();

    public ProfessionPerkListener(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Optional<PlayerProfile> profileOpt = plugin.getIdentityManager().get(player.getUniqueId());
        if (profileOpt.isEmpty()) return;

        KalaProfession prof = KalaProfession.fromId(profileOpt.get().getProfesi());
        Material mat = event.getBlock().getType();
        String matName = mat.name();

        switch (prof) {
            case TUKANG_KAYU -> {
                // Bonus 25% peluang dapat ekstra kayu
                if (matName.endsWith("_LOG") || matName.endsWith("_WOOD")) {
                    if (random.nextDouble() < 0.25) {
                        event.getBlock().getWorld().dropItemNaturally(
                            event.getBlock().getLocation(),
                            new ItemStack(mat, 1)
                        );
                        player.sendActionBar(Component.text("§6[Tukang Kayu] §a+1 Kayu Ekstra!"));
                        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.7f, 1.6f);
                    }
                }
            }
            case PANDAI_BESI -> {
                // Bonus 20% peluang dapat ekstra bijih mentah
                if (matName.endsWith("_ORE") || matName.equals("ANCIENT_DEBRIS")) {
                    if (random.nextDouble() < 0.20) {
                        event.getBlock().getWorld().dropItemNaturally(
                            event.getBlock().getLocation(),
                            new ItemStack(mat, 1)
                        );
                        player.sendActionBar(Component.text("§7[Pandai Besi] §e+1 Bijih Tambahan!"));
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 2.0f);
                    }
                }
            }
            case PETANI -> {
                // Bonus 25% peluang ekstra panen saat tanaman matang (meningkat ke 60% saat Fase Pemulihan Vulkanik)
                BlockData bd = event.getBlock().getBlockData();
                if (bd instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
                    double chance = plugin.getKalaEventManager() != null && plugin.getKalaEventManager().isRecoveryActive() ? 0.60 : 0.25;
                    if (random.nextDouble() < chance) {
                        event.getBlock().getWorld().dropItemNaturally(
                            event.getBlock().getLocation(),
                            new ItemStack(mat, 1)
                        );
                        if (plugin.getKalaEventManager() != null && plugin.getKalaEventManager().isRecoveryActive()) {
                            player.sendActionBar(Component.text("§a🌿 [Berkah Vulkanik] +1 Panen Subur Ekstra!"));
                        } else {
                            player.sendActionBar(Component.text("§e[Petani] §a+1 Hasil Panen Ekstra!"));
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.5f);
                    }
                }
            }
            case PERAMU -> {
                // Bonus 25% peluang ekstra bahan herbal / jamur / bunga
                if (matName.contains("MUSHROOM") || matName.endsWith("FLOWER") ||
                    matName.contains("FERN") || matName.contains("BERRY") || matName.equals("NETHER_WART")) {
                    if (random.nextDouble() < 0.25) {
                        event.getBlock().getWorld().dropItemNaturally(
                            event.getBlock().getLocation(),
                            new ItemStack(mat, 1)
                        );
                        player.sendActionBar(Component.text("§d[Peramu] §b+1 Bahan Herbal!"));
                        player.playSound(player.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 0.7f, 1.3f);
                    }
                }
            }
            default -> {}
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        Optional<PlayerProfile> profileOpt = plugin.getIdentityManager().get(player.getUniqueId());
        if (profileOpt.isEmpty()) return;

        KalaProfession prof = KalaProfession.fromId(profileOpt.get().getProfesi());
        if (prof == KalaProfession.NELAYAN) {
            // Bonus 30% peluang mendapat berkah laut (+Rp 15.00)
            if (random.nextDouble() < 0.30) {
                plugin.getEconomy().deposit(player.getUniqueId(), 15.0, "Berkah Laut Nelayan");
                player.sendActionBar(Component.text("§b[Nelayan] §6Tangkapan Berkah! (+Rp 15.00)"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMove(PlayerMoveEvent event) {
        // Cek hanya ketika pemain berpindah blok penuh untuk menghemat CPU
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Optional<PlayerProfile> profileOpt = plugin.getIdentityManager().get(player.getUniqueId());
        if (profileOpt.isEmpty()) return;

        KalaProfession prof = KalaProfession.fromId(profileOpt.get().getProfesi());

        if (prof == KalaProfession.PELAUT) {
            // Di atas kapal atau di dalam air: dapat napas air / kelincahan lumba-lumba
            if (player.isInsideVehicle() && player.getVehicle() instanceof Boat) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0, false, false, false));
            } else if (player.isInWater()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, false, false, false));
            }
        } else if (prof == KalaProfession.PENGEMBARA) {
            // Kelincahan pengembara di daratan nusantara (Speed I singkat)
            if (player.isSprinting() && !player.hasPotionEffect(PotionEffectType.SPEED)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, false));
            }
        }
    }
}
