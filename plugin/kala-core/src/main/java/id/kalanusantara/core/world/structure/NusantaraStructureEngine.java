package id.kalanusantara.core.world.structure;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Logger;

/**
 * NusantaraStructureEngine — Engine in-house untuk menghasilkan struktur eksplorasi
 * berciri khas kepulauan Nusantara tanpa ketergantungan pada plugin eksternal pihak ketiga.
 * 
 * Struktur terkurasi:
 * 1. POS_PELAUT (Pos Pelaut & Dermaga Samudra)
 * 2. MENARA_PENGAWAS (Menara Pandang Pesisir & Perbukitan)
 * 3. KUIL_KALA (Altar Mistis Purba Penjaga Kala)
 * 4. GUA_TAMBANG (Pos Penambang Terbengkalai Bawah Tanah)
 */
public class NusantaraStructureEngine implements Listener {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private final Random random = new Random();

    // Cache koordinat struktur untuk menjaga jarak minimum (agar tidak terlalu padat)
    private final List<Location> spawnedLocations = new ArrayList<>();
    private static final int MIN_DISTANCE_BETWEEN_STRUCTURES = 256; // 16 chunk minimum

    public enum StructureType {
        POS_PELAUT("Pos Pelaut Nusantara", Material.MANGROVE_LOG),
        MENARA_PENGAWAS("Menara Pengawas Tua", Material.COBBLESTONE),
        KUIL_KALA("Altar Purba Kala", Material.MOSSY_STONE_BRICKS),
        GUA_TAMBANG("Pos Tambang Kuno", Material.RAW_IRON_BLOCK);

        private final String displayName;
        private final Material icon;

        StructureType(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }
    }

    public NusantaraStructureEngine(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        log.info("[StructureEngine] NusantaraStructureEngine aktif — 4 tipe struktur terkurasi siap digenerasikan.");
    }

    /**
     * Membangun struktur pada lokasi tertentu secara aman.
     */
    public boolean spawnStructure(Location loc, StructureType type, CommandSender sender) {
        World world = loc.getWorld();
        if (world == null) return false;

        // Validasi Proteksi 1: Larangan spawn di zona aman Labuhan Awal (kala_main radius 150)
        if (world.getName().equalsIgnoreCase(plugin.getKalaConfig().getSpawnWorld())) {
            Location spawnLoc = world.getSpawnLocation();
            if (loc.distance(spawnLoc) < 150) {
                if (sender != null) {
                    sender.sendMessage(Component.text("§c[!] Gagal: Struktur tidak boleh dibangun di dalam zona Labuhan Awal (radius 150 blok)!", NamedTextColor.RED));
                }
                return false;
            }
        }

        // Validasi Proteksi 2: Cek apakah area dilindungi oleh GriefPrevention (bila terpasang)
        if (isInsidePlayerClaim(loc)) {
            if (sender != null) {
                sender.sendMessage(Component.text("§c[!] Gagal: Lokasi ini berada di dalam klaim lahan pemain!", NamedTextColor.RED));
            }
            return false;
        }

        switch (type) {
            case POS_PELAUT -> buildPosPelaut(loc);
            case MENARA_PENGAWAS -> buildMenaraPengawas(loc);
            case KUIL_KALA -> buildKuilKala(loc);
            case GUA_TAMBANG -> buildGuaTambang(loc);
        }

        spawnedLocations.add(loc.clone());
        log.info("[StructureEngine] Struktur " + type.name() + " berhasil dibangun di " +
            world.getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");

        if (sender != null) {
            sender.sendMessage(Component.text("§a[✓] Berhasil mendirikan §e" + type.getDisplayName() + " §adi koordinat " +
                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        }
        return true;
    }

    // =========================================================================
    // BUILD LOGIC STRUKTUR TERKURASI
    // =========================================================================

    /**
     * 1. Pos Pelaut Nusantara:
     * Dermaga kayu akasia/mangrove di tepi pantai/danau, tong perbekalan garam samudra, lampion,
     * peti perlengkapan bahari, dan catatan jurnal petualang samudra.
     */
    private void buildPosPelaut(Location base) {
        World w = base.getWorld();
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        // Platform 5x5 kayu mangrove / akasia
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                w.getBlockAt(bx + x, by, bz + z).setType(Material.MANGROVE_PLANKS);
                // Tiang pancang ke dasar tanah/air
                for (int y = by - 1; y >= by - 4; y--) {
                    Block sub = w.getBlockAt(bx + x, y, bz + z);
                    if (sub.getType() == Material.WATER || sub.getType() == Material.AIR) {
                        sub.setType(Material.DARK_OAK_FENCE);
                    } else {
                        break;
                    }
                }
            }
        }

        // Pagar pembatas
        for (int x = -2; x <= 2; x++) {
            w.getBlockAt(bx + x, by + 1, bz - 2).setType(Material.MANGROVE_FENCE);
            w.getBlockAt(bx + x, by + 1, bz + 2).setType(Material.MANGROVE_FENCE);
        }
        for (int z = -2; z <= 2; z++) {
            w.getBlockAt(bx - 2, by + 1, bz + z).setType(Material.MANGROVE_FENCE);
            w.getBlockAt(bx + 2, by + 1, bz + z).setType(Material.MANGROVE_FENCE);
        }
        // Pintu masuk dermaga
        w.getBlockAt(bx, by + 1, bz - 2).setType(Material.AIR);

        // Tong dan lampion
        w.getBlockAt(bx - 1, by + 1, bz + 1).setType(Material.BARREL);
        w.getBlockAt(bx + 1, by + 1, bz + 1).setType(Material.LANTERN);

        // Tiang bendera / lampion
        w.getBlockAt(bx + 2, by + 2, bz + 2).setType(Material.MANGROVE_FENCE);
        w.getBlockAt(bx + 2, by + 3, bz + 2).setType(Material.LANTERN);

        // Peti Loot Bahari
        Block chestBlock = w.getBlockAt(bx, by + 1, bz + 1);
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest chest) {
            var inv = chest.getBlockInventory();
            inv.setItem(4, createNamedItem(Material.COMPASS, "§e§lKompas Pelaut Usang", "§7Menunjuk arah angin pelayaran Nusantara lama."));
            inv.setItem(11, createNamedItem(Material.DRIED_KELP, "§aRansum Rumput Laut Kering", "§7Makanan darurat pelaut Kala."));
            inv.setItem(13, new ItemStack(Material.COOKED_COD, 8));
            inv.setItem(15, createNamedItem(Material.SUGAR, "§f§lGaram Samudra Murni", "§7Hasil kristalisasi laut selatan. Bernilai tinggi untuk bumbu."));
            inv.setItem(22, createSailorJournal());
        }
    }

    /**
     * 2. Menara Pengawas Tua:
     * Menara 3 lantai dari batu berlumut dan kayu rimba. Dilengkapi teropong, busur, dan panah.
     */
    private void buildMenaraPengawas(Location base) {
        World w = base.getWorld();
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        // Pondasi batu bulat 4x4 setinggi 8 blok
        for (int y = 0; y < 8; y++) {
            for (int x = -1; x <= 2; x++) {
                for (int z = -1; z <= 2; z++) {
                    boolean edge = (x == -1 || x == 2 || z == -1 || z == 2);
                    Block b = w.getBlockAt(bx + x, by + y, bz + z);
                    if (edge) {
                        b.setType(random.nextBoolean() ? Material.MOSSY_COBBLESTONE : Material.COBBLESTONE);
                    } else {
                        // Tangga tangga di dalam
                        if (x == 0 && z == 0) {
                            b.setType(Material.LADDER);
                        } else {
                            b.setType(Material.AIR);
                        }
                    }
                }
            }
        }

        // Pintu masuk bawah
        w.getBlockAt(bx + 1, by + 1, bz - 1).setType(Material.AIR);
        w.getBlockAt(bx + 1, by + 2, bz - 1).setType(Material.AIR);

        // Dek pengawas lantai atas (y + 8)
        int topY = by + 8;
        for (int x = -2; x <= 3; x++) {
            for (int z = -2; z <= 3; z++) {
                w.getBlockAt(bx + x, topY, bz + z).setType(Material.SPRUCE_SLAB);
                if (x == -2 || x == 3 || z == -2 || z == 3) {
                    w.getBlockAt(bx + x, topY + 1, bz + z).setType(Material.SPRUCE_FENCE);
                }
            }
        }

        // Lampion di pojok atas
        w.getBlockAt(bx - 2, topY + 2, bz - 2).setType(Material.LANTERN);
        w.getBlockAt(bx + 3, topY + 2, bz + 3).setType(Material.LANTERN);

        // Peti pengawas di dek atas
        Block chestBlock = w.getBlockAt(bx + 2, topY + 1, bz + 1);
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest chest) {
            var inv = chest.getBlockInventory();
            inv.setItem(4, createNamedItem(Material.SPYGLASS, "§6§lTeropong Pengawas Selat", "§7Digunakan pengintai untuk memantau aktivitas Gunung Murka."));
            inv.setItem(12, new ItemStack(Material.BOW, 1));
            inv.setItem(14, new ItemStack(Material.ARROW, 24));
            inv.setItem(22, createNamedItem(Material.MAP, "§ePeta Sketsa Koridor Murka", "§7Garis pandang dari bukit menuju kawah utama."));
        }
    }

    /**
     * 3. Kuil Purba Kala:
     * Altar batu andesit berlumut dengan Soul Campfire (Api Jiwa Kala) dan persembahan kuno.
     */
    private void buildKuilKala(Location base) {
        World w = base.getWorld();
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        // Altar batu berundak 5x5
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                w.getBlockAt(bx + x, by, bz + z).setType(
                    (x + z) % 2 == 0 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS
                );
            }
        }

        // Pilar pojok
        int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
        for (int[] c : corners) {
            w.getBlockAt(bx + c[0], by + 1, bz + c[1]).setType(Material.CHISELED_STONE_BRICKS);
            w.getBlockAt(bx + c[0], by + 2, bz + c[1]).setType(Material.STONE_BRICK_WALL);
            w.getBlockAt(bx + c[0], by + 3, bz + c[1]).setType(Material.SOUL_LANTERN);
        }

        // Api Jiwa Kala di tengah altar
        w.getBlockAt(bx, by + 1, bz).setType(Material.CRYING_OBSIDIAN);
        w.getBlockAt(bx, by + 2, bz).setType(Material.SOUL_CAMPFIRE);

        // Peti persembahan
        Block chestBlock = w.getBlockAt(bx, by + 1, bz + 1);
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest chest) {
            var inv = chest.getBlockInventory();
            inv.setItem(4, createNamedItem(Material.AMETHYST_SHARD, "§d§lKristal Waktu Kala", "§7Pecahan mineral yang memancarkan aura siklus delapan minggu."));
            inv.setItem(11, new ItemStack(Material.LAPIS_LAZULI, 12));
            inv.setItem(15, new ItemStack(Material.GOLD_INGOT, 4));
            inv.setItem(22, createKuilScripture());
        }
    }

    /**
     * 4. Pos Tambang Terbengkalai:
     * Kerangka kayu penyangga lereng, rel tambang, gerobak peti dengan bijih mineral.
     */
    private void buildGuaTambang(Location base) {
        World w = base.getWorld();
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        // Balok kayu penyangga terowongan
        for (int y = 0; y <= 3; y++) {
            w.getBlockAt(bx - 2, by + y, bz).setType(Material.DARK_OAK_LOG);
            w.getBlockAt(bx + 2, by + y, bz).setType(Material.DARK_OAK_LOG);
        }
        for (int x = -2; x <= 2; x++) {
            w.getBlockAt(bx + x, by + 3, bz).setType(Material.DARK_OAK_LOG);
        }

        // Rel kereta tambang
        for (int z = -2; z <= 2; z++) {
            w.getBlockAt(bx, by, z + bz).setType(Material.COBBLESTONE);
            w.getBlockAt(bx, by + 1, z + bz).setType(Material.RAIL);
        }

        // Peti Tambang
        Block chestBlock = w.getBlockAt(bx + 1, by + 1, bz + 1);
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest chest) {
            var inv = chest.getBlockInventory();
            inv.setItem(4, createNamedItem(Material.IRON_PICKAXE, "§b§lBeliung Penambang Tua", "§7Kuat dan kokoh untuk menembus dinding batuan keras."));
            inv.setItem(10, new ItemStack(Material.RAW_IRON, 16));
            inv.setItem(12, new ItemStack(Material.RAW_COPPER, 20));
            inv.setItem(14, new ItemStack(Material.COAL, 32));
            inv.setItem(16, new ItemStack(Material.TORCH, 16));
        }
    }

    // =========================================================================
    // ITEM & LORE GENERATORS
    // =========================================================================

    private ItemStack createNamedItem(Material mat, String name, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(loreText, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSailorJournal() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle("Catatan Pelaut Selat");
            meta.setAuthor("Mpu Samudra");
            meta.addPages(
                Component.text("§0Hari ke-14 berlayar dari Labuhan Awal.\n\nGunung Murka terlihat gagah di horizon tenggara saat senja tiba. Angin membawa bau belerang halus."),
                Component.text("§0Bagi para pengembara yang singgah di dermaga ini:\n\nJagalah gotong royong kampung. Selesaikan pesanan di balai desa, kumpulkan rempah dan bebatuan untuk lumbung utama.")
            );
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack createKuilScripture() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle("Prasasti Sang Kala");
            meta.setAuthor("Resi Nusantara");
            meta.addPages(
                Component.text("§0Waktu berputar dalam delapan cakra musim.\n\nMusim Rempah membuka perniagaan, Musim Topan menguji ketangguhan lumbung."),
                Component.text("§0Barangsiapa merawat tanah Nusantara tanpa merusak alamnya, tanah ini akan melimpahkan berkah tanpa batas.")
            );
            book.setItemMeta(meta);
        }
        return book;
    }

    // =========================================================================
    // GENERASI PROCEDURAL PADA CHUNK POPULATE (SEMUA OVERWORLD KALA)
    // =========================================================================

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        World world = event.getWorld();
        // Hanya world NORMAL yang dicantumkan eksplisit di config. Nether/End
        // dibiarkan vanilla agar portal, progression, dan navigasi tidak berubah.
        if (world.getEnvironment() != World.Environment.NORMAL || !isStructureWorld(world)) {
            return;
        }

        // Peluang 0.33% per chunk (sekitar 1 struktur per 300 chunk)
        if (random.nextInt(300) != 0) return;

        Chunk chunk = event.getChunk();
        int cx = (chunk.getX() << 4) + 8;
        int cz = (chunk.getZ() << 4) + 8;
        int cy = world.getHighestBlockYAt(cx, cz);

        // Jangan generate di bawah laut atau terlalu tinggi
        if (cy < 62 || cy > 140) return;

        Location target = new Location(world, cx, cy, cz);

        // Cek jarak dengan struktur terdekat
        for (Location loc : spawnedLocations) {
            if (loc.getWorld().equals(world) && loc.distance(target) < MIN_DISTANCE_BETWEEN_STRUCTURES) {
                return;
            }
        }

        // Pilih tipe struktur secara acak
        StructureType[] types = StructureType.values();
        StructureType chosen = types[random.nextInt(types.length)];
        spawnStructure(target, chosen, null);
    }

    private boolean isInsidePlayerClaim(Location loc) {
        // Cek integrasi GriefPrevention jika ada
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
            try {
                Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                Object instance = gpClass.getField("instance").get(null);
                Object dataStore = gpClass.getMethod("getDataStore").invoke(instance);
                Object claim = dataStore.getClass().getMethod("getClaimAt", Location.class, boolean.class, Object.class)
                    .invoke(dataStore, loc, true, null);
                return claim != null;
            } catch (Exception ignored) {
                // GriefPrevention tidak aktif atau method berubah
            }
        }
        return false;
    }

    private boolean isStructureWorld(World world) {
        List<String> configuredWorlds = plugin.getConfig().getStringList("world.structures.enabled-worlds");
        return configuredWorlds.stream().anyMatch(name -> name.equalsIgnoreCase(world.getName()));
    }
}
