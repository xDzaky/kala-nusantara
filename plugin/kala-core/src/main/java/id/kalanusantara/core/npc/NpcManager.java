package id.kalanusantara.core.npc;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * NpcManager — Mengelola NPC berbasis ArmorStand untuk Kala Nusantara.
 *
 * NPC disimpan di plugins/KalaCore/npcs.yml
 * Format:
 *   npcs:
 *     mbah_kala:
 *       world: kala_main
 *       x: 0.5
 *       y: 64.0
 *       z: 0.5
 *       yaw: 0.0
 *       name: "§6✦ §eMbah Kala §6✦"
 *       type: MBAH_KALA
 *
 * NPC direspawn setiap server restart karena ArmorStand tidak persist antara restart
 * kecuali via NBT. Kita simpan UUID entity di RAM saja.
 */
public class NpcManager implements Listener {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final File npcsFile;
    private FileConfiguration npcsConfig;

    // Map npcId -> ArmorStand UUID (sesi ini saja)
    private final Map<String, UUID> spawnedNpcs = new LinkedHashMap<>();

    // Cooldown interact (mencegah spam)
    private final Map<UUID, Long> interactCooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 1500;

    public NpcManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.npcsFile = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void initialize() {
        loadConfig();
        // Respawn semua NPC yang tersimpan setelah 2 detik server boot
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnAll, 40L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void shutdown() {
        despawnAll();
    }

    // ─────────────────────────────────────────────────────────
    // Spawn & despawn
    // ─────────────────────────────────────────────────────────

    /**
     * Spawn NPC baru di lokasi pemain, simpan ke config.
     * @return npcId yang dibuat
     */
    public String spawnNpc(Player player, String npcId, String npcType) {
        Location loc = player.getLocation();
        String worldName = loc.getWorld().getName();

        // Simpan ke config
        String path = "npcs." + npcId;
        npcsConfig.set(path + ".world", worldName);
        npcsConfig.set(path + ".x", loc.getX());
        npcsConfig.set(path + ".y", loc.getY());
        npcsConfig.set(path + ".z", loc.getZ());
        npcsConfig.set(path + ".yaw", loc.getYaw());
        npcsConfig.set(path + ".type", npcType);
        npcsConfig.set(path + ".name", getNpcDisplayName(npcType));
        saveConfig();

        // Spawn entity
        doSpawnNpc(npcId);
        log.info("[NpcManager] NPC '" + npcId + "' (" + npcType + ") di-spawn di " + worldName);
        return npcId;
    }

    /**
     * Hapus NPC berdasarkan ID.
     */
    public boolean removeNpc(String npcId) {
        UUID entityUuid = spawnedNpcs.remove(npcId);
        if (entityUuid != null) {
            // Hapus entity dari world
            for (World world : Bukkit.getWorlds()) {
                world.getEntities().stream()
                    .filter(e -> e.getUniqueId().equals(entityUuid))
                    .forEach(org.bukkit.entity.Entity::remove);
            }
        }
        npcsConfig.set("npcs." + npcId, null);
        saveConfig();
        return entityUuid != null;
    }

    public Map<String, UUID> getSpawnedNpcs() {
        return Collections.unmodifiableMap(spawnedNpcs);
    }

    // ─────────────────────────────────────────────────────────
    // Interact handler
    // ─────────────────────────────────────────────────────────

    @EventHandler
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;

        Player player = event.getPlayer();
        UUID entityUuid = stand.getUniqueId();

        // Cari npcId dari UUID
        String npcId = null;
        for (Map.Entry<String, UUID> entry : spawnedNpcs.entrySet()) {
            if (entry.getValue().equals(entityUuid)) {
                npcId = entry.getKey();
                break;
            }
        }
        if (npcId == null) return;

        event.setCancelled(true); // jangan ambil item dari armor stand

        // Cooldown check
        long now = System.currentTimeMillis();
        Long last = interactCooldown.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) return;
        interactCooldown.put(player.getUniqueId(), now);

        String npcType = npcsConfig.getString("npcs." + npcId + ".type", "GENERIC");
        handleNpcInteract(player, npcId, npcType);
    }

    /**
     * Dispatch interact ke handler yang tepat.
     */
    private void handleNpcInteract(Player player, String npcId, String npcType) {
        switch (npcType) {
            case "MBAH_KALA" -> plugin.getMbahKalaNpc().onInteract(player);
            case "PEDAGANG" -> plugin.getPesananManager().showOrderMenu(player);
            default -> player.sendMessage(Component.text(
                "[NPC] Halo, Pengembara! Ada yang bisa saya bantu?", NamedTextColor.YELLOW));
        }
    }

    // ─────────────────────────────────────────────────────────
    // Internal spawn helpers
    // ─────────────────────────────────────────────────────────

    private void respawnAll() {
        ConfigurationSection sec = npcsConfig.getConfigurationSection("npcs");
        if (sec == null) return;
        for (String npcId : sec.getKeys(false)) {
            doSpawnNpc(npcId);
        }
    }

    private void doSpawnNpc(String npcId) {
        String path = "npcs." + npcId;
        String worldName = npcsConfig.getString(path + ".world");
        if (worldName == null) return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            log.warning("[NpcManager] World '" + worldName + "' tidak ditemukan untuk NPC: " + npcId);
            return;
        }

        double x = npcsConfig.getDouble(path + ".x");
        double y = npcsConfig.getDouble(path + ".y");
        double z = npcsConfig.getDouble(path + ".z");
        float yaw = (float) npcsConfig.getDouble(path + ".yaw");
        String name = npcsConfig.getString(path + ".name", npcId);

        Location loc = new Location(world, x, y, z, yaw, 0f);

        // Hapus ArmorStand lama di lokasi ini (jika ada dari sesi sebelumnya yang masih tersisa)
        UUID oldUuid = spawnedNpcs.get(npcId);
        if (oldUuid != null) {
            world.getEntities().stream()
                .filter(e -> e.getUniqueId().equals(oldUuid))
                .forEach(org.bukkit.entity.Entity::remove);
        }

        // Spawn ArmorStand
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setCustomName(name); // legacy API masih berfungsi di Paper
        stand.customName(Component.text(name)); // Adventure API
        stand.setCustomNameVisible(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setVisible(false); // invisible body
        stand.setSmall(false);
        stand.setArms(false);
        stand.setPersistent(false); // tidak disimpan ke chunk (kita kelola sendiri)

        // Hologram nama — spawn floating text 2 blok di atas
        ArmorStand label = (ArmorStand) world.spawnEntity(loc.clone().add(0, 2.1, 0), EntityType.ARMOR_STAND);
        label.customName(Component.text(name));
        label.setCustomNameVisible(true);
        label.setGravity(false);
        label.setInvulnerable(true);
        label.setVisible(false);
        label.setSmall(true);
        label.setPersistent(false);

        spawnedNpcs.put(npcId, stand.getUniqueId());
        log.info("[NpcManager] NPC '" + npcId + "' berhasil di-spawn");
    }

    private void despawnAll() {
        for (Map.Entry<String, UUID> entry : spawnedNpcs.entrySet()) {
            String npcId = entry.getKey();
            UUID uuid = entry.getValue();
            for (World world : Bukkit.getWorlds()) {
                world.getEntities().stream()
                    .filter(e -> e.getUniqueId().equals(uuid))
                    .forEach(org.bukkit.entity.Entity::remove);
            }
        }
        spawnedNpcs.clear();
    }

    // ─────────────────────────────────────────────────────────
    // Config helpers
    // ─────────────────────────────────────────────────────────

    private void loadConfig() {
        if (!npcsFile.exists()) {
            npcsConfig = new YamlConfiguration();
        } else {
            npcsConfig = YamlConfiguration.loadConfiguration(npcsFile);
        }
    }

    private void saveConfig() {
        try {
            npcsConfig.save(npcsFile);
        } catch (IOException e) {
            log.warning("[NpcManager] Gagal simpan npcs.yml: " + e.getMessage());
        }
    }

    private String getNpcDisplayName(String npcType) {
        return switch (npcType) {
            case "MBAH_KALA" -> "§6✦ §eMbah Kala §6✦";
            case "PEDAGANG" -> "§b⚑ Pedagang Pasar";
            default -> "§7NPC";
        };
    }
}
