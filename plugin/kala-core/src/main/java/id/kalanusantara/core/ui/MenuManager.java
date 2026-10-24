package id.kalanusantara.core.ui;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.fauna.FaunaSpecies;
import id.kalanusantara.core.identity.PlayerProfile;
import id.kalanusantara.core.npc.PesananManager;
import id.kalanusantara.core.project.VillageProject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MenuManager — Mengelola seluruh GUI interaktif Kala Nusantara RPG.
 * 
 * Filosofi Desain: Pemain tidak perlu menghafal command.
 * Seluruh fitur (Tambang, Proyek, Pesanan, Profesi, Jurnal, Klaim)
 * dapat diakses lewat GUI visual 1-klik.
 */
public class MenuManager {

    private final KalaCorePlugin plugin;

    // Judul GUI unik sebagai identifier
    public static final String KOMPAS_GUI_TITLE    = "✦ Kompas Pengembara ✦";
    public static final String TAMBANG_GUI_TITLE   = "⛏ Dunia Tambang Nusantara";
    public static final String PROYEK_GUI_TITLE    = "🏛 Proyek Gotong Royong";
    public static final String PESANAN_GUI_TITLE   = "⚑ Pesanan Pedagang Pasar";
    public static final String JURNAL_GUI_TITLE    = "📖 Jurnal Penjelajah";
    public static final String MUSIM_GUI_TITLE     = "📅 Kalender Musim Nusantara";
    public static final String KOSMETIK_GUI_TITLE  = "✨ Gelar & Kosmetik Nusantara";
    public static final String EKSPEDISI_GUI_TITLE = "⛵ Ekspedisi Musiman";
    public static final String FAUNA_GUI_TITLE     = "🐾 Atlas Fauna Nusantara";
    public static final String VOLCANO_GUI_TITLE   = "🌋 Aktivitas Gunung Murka";
    public static final String BEDROCK_GUI_TITLE   = "📱 Panduan Kontrol & HP";

    public MenuManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────
    // 1. KOMPAS PENGEMBARA (Hub Navigasi Utama — 54 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openKompas(Player player, KompasTab tab) {
        Inventory inv = Bukkit.createInventory(null, 54,
            Component.text(KOMPAS_GUI_TITLE, NamedTextColor.GOLD));

        Optional<PlayerProfile> profileOpt = plugin.getIdentityManager().get(player.getUniqueId());

        setTabHeaders(inv, tab);

        switch (tab) {
            case MISI -> fillMisiTab(inv, player, profileOpt.orElse(null));
            case PETA -> fillPetaTab(inv, player);
            case KAMPUNG -> fillKampungTab(inv, player);
            case PROGRES -> fillProgresTab(inv, player, profileOpt.orElse(null));
            case PANDUAN -> fillPanduanTab(inv, player);
        }

        setFooter(inv, player, profileOpt.orElse(null));
        player.openInventory(inv);
    }

    private void setTabHeaders(Inventory inv, KompasTab activeTab) {
        KompasTab[] tabs = KompasTab.values();
        int[] headerSlots = {1, 2, 3, 4, 5};

        for (int i = 0; i < tabs.length && i < headerSlots.length; i++) {
            KompasTab t = tabs[i];
            boolean isActive = t == activeTab;
            Material mat = isActive ? t.getActiveMaterial() : t.getMaterial();

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                Component name = Component.text(t.getDisplayName(),
                    isActive ? NamedTextColor.GOLD : NamedTextColor.GRAY);
                if (isActive) name = name.decorate(TextDecoration.BOLD);
                meta.displayName(name.decoration(TextDecoration.ITALIC, false));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(isActive ? "▶ Tab Aktif" : "Klik untuk membuka",
                    isActive ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(headerSlots[i], item);
        }

        ItemStack border = makeBorder();
        for (int slot : new int[]{0, 6, 7, 8}) {
            inv.setItem(slot, border);
        }
    }

    private void fillMisiTab(Inventory inv, Player player, PlayerProfile profile) {
        int slot = 22; // Slot tengah
        boolean hasTutorial = profile != null && !profile.isTutorialDone();

        if (hasTutorial) {
            ItemStack questItem = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = questItem.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("📚 Misi Aktif: Labuhan Awal", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Selesaikan petunjuk Mbah Kala", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("untuk memulai perjalananmu di Nusantara!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(Component.text("▶ Temui Mbah Kala di desa spawn", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                questItem.setItemMeta(meta);
            }
            inv.setItem(slot, questItem);
        } else {
            ItemStack questItem = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = questItem.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("✨ Tutorial Selesai!", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Dunia Nusantara terbuka lebar untukmu.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Pilih profesi di tab Kampung atau bantu warga desa!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                questItem.setItemMeta(meta);
            }
            inv.setItem(slot, questItem);
        }
    }

    private void fillKampungTab(Inventory inv, Player player) {
        // Baris 2 & 3: Ikon aksi interaktif kampung
        // 1. Proyek Gotong Royong (Slot 20)
        inv.setItem(20, createGuiItem(
            Material.CHEST,
            "🏛 Proyek Gotong Royong",
            NamedTextColor.GOLD,
            List.of(
                "Sumbang material bersama warga untuk",
                "membangun fasilitas desa (Lumbung, Dermaga, dll).",
                "",
                "§e▶ Klik untuk membuka menu Proyek!"
            )
        ));

        // 2. Pesanan Pasar (Slot 22)
        inv.setItem(22, createGuiItem(
            Material.EMERALD,
            "⚑ Pesanan Pedagang Pasar",
            NamedTextColor.GREEN,
            List.of(
                "Jual hasil panen, kayu, dan tambang",
                "langsung ke pedagang untuk Rupiah Kala.",
                "",
                "§e▶ Klik untuk membuka menu Pesanan!"
            )
        ));

        // 3. Dunia Tambang (Slot 24)
        inv.setItem(24, createGuiItem(
            Material.IRON_PICKAXE,
            "⛏ Dunia Tambang (Resource World)",
            NamedTextColor.AQUA,
            List.of(
                "Menambang dan menebang bebas tanpa",
                "merusak keindahan pemukiman pulau utama.",
                "",
                "§e▶ Klik untuk pergi ke Dunia Tambang!"
            )
        ));

        // 4. Pilih Profesi (Slot 30)
        inv.setItem(30, createGuiItem(
            Material.ANVIL,
            "🛡 Pilih / Ganti Profesi",
            NamedTextColor.YELLOW,
            List.of(
                "Tukang Kayu, Pandai Besi, Petani,",
                "Nelayan, Peramu, Pelaut, atau Pengembara.",
                "",
                "§e▶ Klik untuk membuka menu Profesi!"
            )
        ));

        // 5. Jurnal Penjelajah (Slot 32)
        inv.setItem(32, createGuiItem(
            Material.BOOK,
            "📖 Jurnal Penjelajah",
            NamedTextColor.LIGHT_PURPLE,
            List.of(
                "Lihat catatan sejarah perjalanan,",
                "pencapaian, dan peristiwa Kala.",
                "",
                "§e▶ Klik untuk membuka Jurnal!"
            )
        ));
    }

    private void fillPetaTab(Inventory inv, Player player) {
        String worldName = player.getWorld().getName();
        String locStr = String.format("X: %d | Y: %d | Z: %d",
            player.getLocation().getBlockX(),
            player.getLocation().getBlockY(),
            player.getLocation().getBlockZ());

        // Slot 19: Ekspedisi Musiman
        inv.setItem(19, createGuiItem(
            Material.SPYGLASS,
            "⛵ Ekspedisi Musiman",
            NamedTextColor.AQUA,
            List.of(
                "Jelajahi pulau seberang dan temukan",
                "reruntuhan kuno serta harta leluhur.",
                "",
                "§e▶ Klik untuk membuka Ekspedisi!"
            )
        ));

        // Slot 21: Peta Kepulauan
        inv.setItem(21, createGuiItem(
            Material.FILLED_MAP,
            "🗺 Peta Kepulauan Nusantara",
            NamedTextColor.GOLD,
            List.of(
                "Dunia saat ini: §e" + worldName,
                "Koordinatmu: §f" + locStr,
                "Batas Dunia: §aRadius 3,000 Blok",
                "",
                "§7Pulau utama dilindungi dari kehancuran total.",
                "§7Gunakan Dunia Tambang untuk eksploitasi mineral."
            )
        ));

        // Slot 23: Status Gunung Murka
        String statusMurka = plugin.getKalaEventManager() != null 
                ? plugin.getKalaEventManager().getCurrentPhase().getDisplayName() 
                : "§7Tenang";
        inv.setItem(23, createGuiItem(
            Material.MAGMA_BLOCK,
            "🌋 Aktivitas Gunung Murka",
            NamedTextColor.RED,
            List.of(
                "Status Saat Ini: " + statusMurka,
                "Amati getaran seismik dan salurkan",
                "persembahan gotong royong pereda erupsi.",
                "",
                "§e▶ Klik untuk melihat status & sajen!"
            )
        ));

        // Slot 25: Kalender Musim
        inv.setItem(25, createGuiItem(
            Material.CLOCK,
            "📅 Kalender Musim Nusantara",
            NamedTextColor.YELLOW,
            List.of(
                "Lihat jadwal 8 minggu musiman,",
                "bonus aktif, dan siklus datangnya Kala.",
                "",
                "§e▶ Klik untuk membuka Kalender!"
            )
        ));
    }

    private void fillPanduanTab(Inventory inv, Player player) {
        inv.setItem(19, createGuiItem(Material.COMPASS, "📱 Panduan Bedrock & HP", NamedTextColor.AQUA, List.of("Panduan kontrol layar sentuh,", "klaim tanah mobile, & notifikasi.", "", "§e▶ Klik untuk buka Panduan HP!")));
        inv.setItem(20, createGuiItem(Material.FEATHER, "🐾 Atlas Fauna Nusantara", NamedTextColor.GREEN, List.of("Koleksi observasi satwa unik pulau.", "", "§e▶ Klik untuk buka Atlas Fauna!")));
        inv.setItem(21, createGuiItem(Material.MAGMA_BLOCK, "🌋 Gunung Murka & Sajen", NamedTextColor.RED, List.of("Info erupsi & sajen gotong royong.", "", "§e▶ Klik untuk buka info Gunung!")));
        inv.setItem(22, createGuiItem(Material.IRON_PICKAXE, "⛏ Dunia Tambang", NamedTextColor.AQUA, List.of("Tambang mineral bebas tanpa merusak alam pulau.", "", "§e▶ Klik untuk pergi ke Tambang!")));
        inv.setItem(23, createGuiItem(Material.CHEST, "🏛 Proyek Kampung", NamedTextColor.GOLD, List.of("Gotong royong membangun infrastruktur desa.", "", "§e▶ Klik untuk buka Proyek!")));
        inv.setItem(24, createGuiItem(Material.EMERALD, "⚑ Pesanan Pasar", NamedTextColor.GREEN, List.of("Jual barang untuk mendapatkan Rupiah Kala.", "", "§e▶ Klik untuk buka Pesanan!")));
        inv.setItem(25, createGuiItem(Material.BOOK, "📖 Jurnal Pribadi", NamedTextColor.LIGHT_PURPLE, List.of("Pencatat otomatis pencapaian & sejarahmu.", "", "§e▶ Klik untuk buka Jurnal!")));
    }

    private void fillProgresTab(Inventory inv, Player player, PlayerProfile profile) {
        if (profile == null) return;

        ItemStack profileItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = profileItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("👤 " + profile.getName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Profesi: " + profile.getProfesi(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Saldo: Rp " + String.format("%,.0f", profile.getBalance()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Status Tutorial: " + (profile.isTutorialDone() ? "✅ Selesai" : "⏳ Belum"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Bergabung: Sejak awal petualangan", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            profileItem.setItemMeta(meta);
        }
        inv.setItem(20, profileItem);

        // Slot 22: Atlas Fauna Nusantara
        int foundCount = plugin.getFaunaManager() != null ? plugin.getFaunaManager().getDiscoveredCount(player.getUniqueId()) : 0;
        inv.setItem(22, createGuiItem(
            Material.FEATHER,
            "🐾 Atlas Fauna Nusantara",
            NamedTextColor.GREEN,
            List.of(
                "Satwa Ditemukan: §a" + foundCount + " / 6 Spesies",
                "Jelajahi pulau untuk melengkapi koleksi",
                "dan dapatkan gelar [Sahabat Satwa]!",
                "",
                "§e▶ Klik untuk membuka Atlas Fauna!"
            )
        ));

        // Slot 24: Gelar & Kosmetik
        inv.setItem(24, createGuiItem(
            Material.NETHER_STAR,
            "✨ Gelar & Efek Langkah",
            NamedTextColor.LIGHT_PURPLE,
            List.of(
                "Pasang gelar kehormatan dan efek partikel",
                "langkah kaki yang kamu peroleh dari petualangan.",
                "",
                "§e▶ Klik untuk memilih Gelar & Efek!"
            )
        ));
    }

    private void setFooter(Inventory inv, Player player, PlayerProfile profile) {
        ItemStack footer = makeBorder();
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, footer);
        }

        // Tombol Tutup (Slot 49)
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✖ Tutup Menu", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            close.setItemMeta(meta);
        }
        inv.setItem(49, close);

        // Tampilan Saldo (Slot 45)
        if (profile != null) {
            ItemStack balItem = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta bMeta = balItem.getItemMeta();
            if (bMeta != null) {
                bMeta.displayName(Component.text("Dompet: Rp " + String.format("%,.0f", profile.getBalance()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                balItem.setItemMeta(bMeta);
            }
            inv.setItem(45, balItem);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. DUNIA TAMBANG GUI (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openTambangMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(TAMBANG_GUI_TITLE, NamedTextColor.GOLD));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        boolean inResource = plugin.getResourceWorldManager().isResourceWorld(player.getWorld());

        // Tombol Berangkat (Slot 11)
        inv.setItem(11, createGuiItem(
            Material.IRON_PICKAXE,
            "✦ Berangkat ke Dunia Tambang ✦",
            NamedTextColor.GREEN,
            List.of(
                "Masuk ke dimensi tambang nusantara.",
                "Bebas menambang bijih & menebang pohon",
                "tanpa merusak estetika kampung.",
                "",
                inResource ? "§a▶ Kamu sudah berada di sini!" : "§e▶ Klik untuk Berangkat!"
            )
        ));

        // Info Dunia (Slot 13)
        inv.setItem(13, createGuiItem(
            Material.BOOK,
            "📜 Status & Ketentuan Tambang",
            NamedTextColor.YELLOW,
            List.of(
                "• Lokasi: §f" + player.getWorld().getName(),
                "• Perbatasan: §a3,000 Blok",
                "• Klaim Tanah: §cNonaktif",
                "• Direset berkala untuk regenerasi mineral.",
                "• Pemain dievakuasi aman saat reset berlangsung."
            )
        ));

        // Tombol Pulang (Slot 15)
        inv.setItem(15, createGuiItem(
            Material.OAK_DOOR,
            "🏛 Pulang ke Labuhan Awal",
            NamedTextColor.AQUA,
            List.of(
                "Kembali ke pemukiman warga Labuhan Awal",
                "untuk menyimpan hasil tambang & berdagang.",
                "",
                inResource ? "§e▶ Klik untuk Pulang!" : "§a▶ Kamu sudah di dunia utama!"
            )
        ));

        // Tombol Kembali ke Kompas (Slot 22)
        inv.setItem(22, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. PROYEK GOTONG ROYONG GUI (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openProyekMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(PROYEK_GUI_TITLE, NamedTextColor.GOLD));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        var projects = plugin.getKampungProjectManager().getAllProjects();
        int[] slots = {11, 13, 15};
        int idx = 0;

        for (VillageProject p : projects) {
            if (idx >= slots.length) break;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(p.getDescription(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Material: §f" + p.getTargetMaterial().name() + " §8| Butuh: §e" + p.getTargetAmount()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Progress: " + p.getProgressBar(10) + " §e" + String.format("%.1f%%", p.getProgressPercentage()) + " (" + p.getCurrentAmount() + "/" + p.getTargetAmount() + ")").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            if (p.isCompleted()) {
                lore.add(Component.text("§a✔ Proyek Telah Selesai Dibangun!", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("§e▶ Klik untuk menyetor material dari tasmu!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("§7(Dapat imbalan Rupiah Kala langsung)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }

            ItemStack item = new ItemStack(p.getTargetMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("🏛 " + p.getName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[idx++], item);
        }

        // Tombol Kembali ke Kompas (Slot 22)
        inv.setItem(22, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. PESANAN PASAR GUI (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openPesananMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(PESANAN_GUI_TITLE, NamedTextColor.GOLD));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        PesananManager pm = plugin.getPesananManager();
        var orders = pm.getPesananList();
        int[] orderSlots = {10, 11, 12, 13, 14, 15, 16, 19};

        for (int i = 0; i < orders.size() && i < orderSlots.length; i++) {
            PesananManager.Pesanan order = orders.get(i);
            boolean canSubmit = pm.hasSufficientItems(player, order);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Jumlah: §e" + order.amount() + " unit").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Hadiah: §a+Rp " + String.format("%,.0f", order.reward())).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            if (canSubmit) {
                lore.add(Component.text("§a✔ Klik untuk serahkan item!", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("§c✘ Item di tasmu belum cukup", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            }

            ItemStack item = new ItemStack(order.material());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("⚑ " + pm.formatMaterial(order.material()),
                    canSubmit ? NamedTextColor.GREEN : NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(orderSlots[i], item);
        }

        // Tombol Kembali ke Kompas (Slot 22)
        inv.setItem(22, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. KALENDER MUSIM GUI (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openSeasonCalendarGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(MUSIM_GUI_TITLE, NamedTextColor.GOLD));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        var sm = plugin.getSeasonManager();
        int curWeek = sm.getCurrentWeek();

        // 8 Minggu (Slot 9 s/d 16)
        for (int w = 1; w <= 8; w++) {
            boolean isActive = w == curWeek;
            Material mat = isActive ? Material.EMERALD_BLOCK : (w == 7 ? Material.REDSTONE_BLOCK : Material.COPPER_BLOCK);

            List<String> lore = new ArrayList<>();
            lore.add("Deskripsi: §7" + sm.getWeekDescription(w));
            lore.add("Dampak: §a" + sm.getWeekBonus(w));
            lore.add("");
            if (isActive) {
                lore.add("§a▶ MINGGU INI AKTIF ◀");
            } else if (w < curWeek) {
                lore.add("§8[Telah Berlalu]");
            } else {
                lore.add("§e[Akan Datang]");
            }

            inv.setItem(8 + w, createGuiItem(
                mat,
                sm.getWeekTitle(w),
                isActive ? NamedTextColor.GREEN : (w == 7 ? NamedTextColor.RED : NamedTextColor.GOLD),
                lore
            ));
        }

        // Info Musim Aktif (Slot 4)
        inv.setItem(4, createGuiItem(
            Material.CLOCK,
            "Musim: " + sm.getSeasonName(),
            NamedTextColor.YELLOW,
            List.of("Siklus 8 minggu bertema Nusantara", "Saat ini: Minggu ke-" + curWeek + " dari 8")
        ));

        // Tombol Kembali ke Kompas (Slot 22)
        inv.setItem(22, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. GELAR & KOSMETIK GUI (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openCosmeticsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(KOSMETIK_GUI_TITLE, NamedTextColor.GOLD));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        var cm = plugin.getCosmeticManager();
        var data = cm.getData(player.getUniqueId());

        // Baris 1: Gelar Kehormatan (Slot 10..14)
        String[][] titles = {
            {"PENGEMBARA", "Pengembara Awam", "BOOK", "Gelar awal seluruh pengembara Nusantara."},
            {"PEMBANGUN_KAMPUNG", "Pembangun Kampung", "OAK_PLANKS", "Bantu menyumbang material proyek desa."},
            {"SAUDAGAR_NUSANTARA", "Saudagar Nusantara", "EMERALD", "Selesaikan pesanan dagang di pasar."},
            {"PENAKLUK_KALA", "Penakluk Kala", "NETHER_STAR", "Bertahan melewati puncak bencana Kala."},
            {"PENJELAJAH_SAMUDRA", "Penjelajah Samudra", "COMPASS", "Selesaikan ekspedisi ke kuil pulau luar."}
        };

        for (int i = 0; i < titles.length; i++) {
            String key = titles[i][0];
            String name = titles[i][1];
            Material mat = Material.matchMaterial(titles[i][2]);
            if (mat == null) mat = Material.PAPER;
            String desc = titles[i][3];

            boolean unlocked = data.unlockedTitles.contains(key);
            boolean active = key.equalsIgnoreCase(data.activeTitle);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + desc);
            lore.add("");
            if (active) {
                lore.add("§a✔ Sedang Dipasang");
            } else if (unlocked) {
                lore.add("§e▶ Klik untuk Memasang Gelar ini!");
            } else {
                lore.add("§c🔒 Belum Terbuka (Selesaikan Tantangan)");
            }

            inv.setItem(10 + i, createGuiItem(
                mat,
                "Gelar: " + name,
                active ? NamedTextColor.GREEN : (unlocked ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY),
                lore
            ));
        }

        // Baris 2: Partikel Trail Langkah (Slot 19..23)
        String[][] trails = {
            {"NONE", "Tanpa Efek", "BARRIER", "Matikan efek partikel langkah."},
            {"MELATI", "Harum Melati", "LILY_OF_THE_VALLEY", "Kelopak melati memancar saat melangkah."},
            {"BARA", "Bara Api Kala", "BLAZE_POWDER", "Percikan bara api mistis di jejakmu."},
            {"OMBAK", "Buih Ombak", "WATER_BUCKET", "Percikan air laut samudra nusantara."},
            {"RIMBA", "Kelopak Rimba", "CHERRY_SAPLING", "Kelopak bunga rimba berjatuhan."}
        };

        for (int i = 0; i < trails.length; i++) {
            String key = trails[i][0];
            String name = trails[i][1];
            Material mat = Material.matchMaterial(trails[i][2]);
            if (mat == null) mat = Material.FEATHER;
            String desc = trails[i][3];

            boolean active = key.equalsIgnoreCase(data.activeTrail);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + desc);
            lore.add("");
            if (active) {
                lore.add("§a✔ Efek Sedang Aktif");
            } else {
                lore.add("§e▶ Klik untuk Memasang Efek ini!");
            }

            inv.setItem(19 + i, createGuiItem(
                mat,
                "Efek: " + name,
                active ? NamedTextColor.GREEN : NamedTextColor.AQUA,
                lore
            ));
        }

        // Tombol Kembali ke Kompas (Slot 26)
        inv.setItem(26, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 7. EKSPEDISI MUSIMAN GUI (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openExpeditionGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(EKSPEDISI_GUI_TITLE, NamedTextColor.GOLD));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        var em = plugin.getExpeditionManager();
        int dist = em.getDistanceToTarget(player);

        // Slot 11: Info Situs
        inv.setItem(11, createGuiItem(
            Material.MAP,
            "Situs: " + em.getExpeditionName(),
            NamedTextColor.GOLD,
            List.of(
                "Sebuah kuil kuno penyimpanan rempah pusaka",
                "terletak di pulau terpencil di seberang samudra.",
                "",
                "Dunia: §fkala_main",
                "Target Koordinat: §eX: " + em.getTargetX() + " | Z: " + em.getTargetZ()
            )
        ));

        // Slot 13: Radar / Kompas Jarak
        inv.setItem(13, createGuiItem(
            Material.COMPASS,
            "Navigasi Petualang",
            NamedTextColor.AQUA,
            List.of(
                dist >= 0 ? ("Jarak saat ini: §e" + dist + " blok") : "Dunia berbeda!",
                "",
                "Berlayarlah menggunakan perahu ke koordinat",
                "untuk menemukan reruntuhan kuno dan",
                "mencatatkan penemuanmu di Jurnal Kepulauan!"
            )
        ));

        // Slot 15: Hadiah
        inv.setItem(15, createGuiItem(
            Material.GOLD_BLOCK,
            "Harta Karun Penemuan",
            NamedTextColor.YELLOW,
            List.of(
                "• Hadiah Dana: §a+Rp 250.00 Rupiah Kala",
                "• Gelar Eksklusif: §b[Penjelajah Samudra]",
                "• Catatan Rahasia di Jurnal Penjelajah"
            )
        ));

        // Tombol Kembali ke Kompas (Slot 22)
        inv.setItem(22, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 8. ATLAS FAUNA NUSANTARA GUI (36 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openAtlasFaunaGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36,
            Component.text(FAUNA_GUI_TITLE, NamedTextColor.GOLD));

        ItemStack border = makeBorder();
        for (int i = 0; i < 36; i++) inv.setItem(i, border);

        var fm = plugin.getFaunaManager();
        int foundCount = fm != null ? fm.getDiscoveredCount(player.getUniqueId()) : 0;
        int totalSpecies = FaunaSpecies.values().length;
        int percent = (foundCount * 100) / totalSpecies;

        // Slot 4: Progress Book
        inv.setItem(4, createGuiItem(
            Material.BOOK,
            "Atlas Observasi Satwa Kepulauan",
            NamedTextColor.YELLOW,
            List.of(
                "Progres Penemuan: §a" + foundCount + " / " + totalSpecies + " (" + percent + "%)",
                "Temukan dan amati semua satwa liar",
                "untuk mencatatnya dalam Jurnal dan",
                "membuka gelar langka [Sahabat Satwa]!"
            )
        ));

        // Slot 11..16: 6 Spesies Fauna Nusantara
        FaunaSpecies[] speciesList = FaunaSpecies.values();
        int[] faunaSlots = {11, 12, 13, 14, 15, 16};

        for (int i = 0; i < speciesList.length && i < faunaSlots.length; i++) {
            FaunaSpecies s = speciesList[i];
            boolean isDiscovered = fm != null && fm.isDiscovered(player.getUniqueId(), s.getId());

            if (isDiscovered) {
                inv.setItem(faunaSlots[i], createGuiItem(
                    s.getIcon(),
                    "✓ " + s.getDisplayName(),
                    NamedTextColor.GREEN,
                    List.of(
                        "Status: §aSudah Diamati",
                        "Habitat: §f" + s.getHabitat(),
                        "Konservasi: §e" + s.getConservationStatus(),
                        "",
                        "§7" + s.getLore(),
                        "",
                        "§6Hadiah: +Rp " + String.format("%.0f", s.getRewardRupiah()) + " (Terklaim)"
                    )
                ));
            } else {
                inv.setItem(faunaSlots[i], createGuiItem(
                    Material.GRAY_DYE,
                    "? " + s.getDisplayName() + " [Terkunci]",
                    NamedTextColor.GRAY,
                    List.of(
                        "Status: §cBelum Ditemukan",
                        "Petunjuk Habitat: §e" + s.getHabitat(),
                        "Konservasi: §7" + s.getConservationStatus(),
                        "",
                        "§8Jelajahi pulau dan dekati satwa ini",
                        "§8untuk mencatatnya ke dalam Atlas!",
                        "",
                        "§eImbalan Konservasi: +Rp " + String.format("%.0f", s.getRewardRupiah())
                    )
                ));
            }
        }

        // Slot 31: Tombol Kembali ke Kompas
        inv.setItem(31, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        // Slot 35: Tombol Tutup
        inv.setItem(35, createGuiItem(
            Material.BARRIER,
            "✖ Tutup Menu",
            NamedTextColor.RED,
            List.of("Tutup jendela inventori")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 9. STATUS & SAJEN GUNUNG MURKA GUI (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openVolcanoGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(VOLCANO_GUI_TITLE, NamedTextColor.RED));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        var vm = plugin.getKalaEventManager();
        String phaseName = vm != null ? vm.getPhaseDisplayName() : "§7Tenang";
        int remainingSec = vm != null ? vm.getPhaseRemainingSeconds() : 0;
        int currentOfferings = vm != null ? vm.getCurrentOfferings() : 0;
        int targetOfferings = vm != null ? vm.getTargetOfferings() : 100;

        // Slot 11: Info Status Vulkanik
        inv.setItem(11, createGuiItem(
            Material.MAGMA_BLOCK,
            "Aktivitas Gunung Murka",
            NamedTextColor.GOLD,
            List.of(
                "Status Saat Ini: " + phaseName,
                "Sisa Durasi Fase: §e" + (remainingSec > 0 ? (remainingSec / 60) + "m " + (remainingSec % 60) + "s" : "Stabil"),
                "",
                "§7Gunung Murka adalah gunung berapi purba",
                "§7di pusat kepulauan. Aktivitasnya membawa",
                "§7ujian bencana sekaligus berkah kesuburan."
            )
        ));

        // Slot 13: Gotong Royong Pereda Murka
        inv.setItem(13, createGuiItem(
            Material.FIRE_CHARGE,
            "Altar Sajen Pereda Murka",
            NamedTextColor.RED,
            List.of(
                "Progres Gotong Royong: §a" + currentOfferings + " / " + targetOfferings + " Poin",
                "",
                "Setorkan bahan tambang dan logistik desa",
                "untuk mempercepat redanya amukan gunung!",
                "Target tercapai = langsung masuk fase Pemulihan!"
            )
        ));

        // Slot 15: Berkah Fase Pemulihan
        inv.setItem(15, createGuiItem(
            Material.BONE_MEAL,
            "Berkah Tanah Vulkanik",
            NamedTextColor.GREEN,
            List.of(
                "Manfaat Pasca Erupsi (Fase Pemulihan):",
                "• §ePetani: §aPeluang panen ekstra melonjak ke 60%!",
                "• §ePedagang: §aPesanan hasil bumi bernilai +25% Rupiah!",
                "• §ePembangun: §aBonus gotong royong proyek desa!"
            )
        ));

        // Slot 19, 21, 23: Tombol Donasi Sajen Cepat
        inv.setItem(19, createGuiItem(
            Material.COAL,
            "Sumbang Arang / Batu Bara",
            NamedTextColor.GRAY,
            List.of(
                "Memberi: §e+1 Poin Pereda Murka",
                "Imbalan: §6+Rp 5.00 Rupiah Kala + 2 EXP",
                "",
                "§e▶ Klik untuk menyumbang 1x Arang/Batubara"
            )
        ));

        inv.setItem(21, createGuiItem(
            Material.COPPER_INGOT,
            "Sumbang Tembaga Mentah / Batangan",
            NamedTextColor.GOLD,
            List.of(
                "Memberi: §e+4 Poin Pereda Murka",
                "Imbalan: §6+Rp 20.00 Rupiah Kala + 8 EXP",
                "",
                "§e▶ Klik untuk menyumbang 1x Tembaga"
            )
        ));

        inv.setItem(23, createGuiItem(
            Material.BREAD,
            "Sumbang Bahan Makanan (Roti/Gandum)",
            NamedTextColor.YELLOW,
            List.of(
                "Memberi: §e+2 Poin Pereda Murka",
                "Imbalan: §6+Rp 10.00 Rupiah Kala + 4 EXP",
                "",
                "§e▶ Klik untuk menyumbang 1x Makanan"
            )
        ));

        // Slot 26: Kembali
        inv.setItem(26, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // 10. PANDUAN KONTROL SENTUH BEDROCK & PENGATURAN (27 Slot)
    // ─────────────────────────────────────────────────────────────

    public void openBedrockGuideGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text(BEDROCK_GUI_TITLE, NamedTextColor.AQUA));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        boolean tipsActive = plugin.getNotificationService() != null && plugin.getNotificationService().isTipsEnabled(player.getUniqueId());

        // Slot 10: Kontrol Sentuh HP / MCPE
        inv.setItem(10, createGuiItem(
            Material.TARGET,
            "Kontrol Layar Sentuh Mobile",
            NamedTextColor.YELLOW,
            List.of(
                "• Ketuk 1x ikon menu untuk memilih tombol.",
                "• Tidak perlu Shift-Click atau drag item.",
                "• Ketuk NPC / Warga untuk berbicara.",
                "• Tahan layar pada blok/pintu untuk interaksi."
            )
        ));

        // Slot 12: Perlindungan Klaim Tanah di HP
        inv.setItem(12, createGuiItem(
            Material.GOLDEN_SHOVEL,
            "Klaim Tanah Tanpa Ribet",
            NamedTextColor.GOLD,
            List.of(
                "Lindungi rumahmu dari kerusakan:",
                "1. Pegang Sekop Emas (diberikan saat pemula).",
                "2. Ketuk sudut 1 lahan, lalu ketuk sudut seberang.",
                "3. Gunakan Tongkat kayu untuk cek batas klaim."
            )
        ));

        // Slot 14: Perintah Cepat Chat
        inv.setItem(14, createGuiItem(
            Material.WRITABLE_BOOK,
            "Perintah Pintas Pengembara",
            NamedTextColor.GREEN,
            List.of(
                "• §e/kompas §7— Buka menu utama kapan saja",
                "• §e/fauna §7— Buka buku Atlas Fauna",
                "• §e/gunung §7— Cek status Gunung Murka",
                "• §e/dompet §7— Cek saldo & transfer uang"
            )
        ));

        // Slot 16: Pengaturan Notifikasi / Tips
        inv.setItem(16, createGuiItem(
            Material.BELL,
            tipsActive ? "Notifikasi Tips: §aAKTIF" : "Notifikasi Tips: §cNONAKTIF",
            tipsActive ? NamedTextColor.GREEN : NamedTextColor.RED,
            List.of(
                "Status: " + (tipsActive ? "§aMenerima tips dan pesan ambient" : "§cMode tenang (hanya pesan darurat)"),
                "",
                "§e▶ Ketuk untuk mengubah pengaturan ini!"
            )
        ));

        // Slot 22: Kembali
        inv.setItem(22, createGuiItem(
            Material.ARROW,
            "◀ Kembali ke Kompas",
            NamedTextColor.WHITE,
            List.of("Buka menu utama Kompas Pengembara")
        ));

        player.openInventory(inv);
    }

    // Helper Item Builders
    // ─────────────────────────────────────────────────────────────

    private ItemStack createGuiItem(Material mat, String name, NamedTextColor color, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String l : loreLines) {
                lore.add(Component.text(l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeBorder() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            glass.setItemMeta(meta);
        }
        return glass;
    }

    public enum KompasTab {
        MISI("⚔ Misi", Material.BOOK, Material.ENCHANTED_BOOK),
        PETA("🗺 Peta", Material.MAP, Material.FILLED_MAP),
        KAMPUNG("🏘 Kampung", Material.OAK_PLANKS, Material.CRAFTING_TABLE),
        PROGRES("📊 Progres", Material.EXPERIENCE_BOTTLE, Material.NETHER_STAR),
        PANDUAN("📖 Panduan", Material.PAPER, Material.WRITABLE_BOOK);

        private final String displayName;
        private final Material material;
        private final Material activeMaterial;

        KompasTab(String displayName, Material material, Material activeMaterial) {
            this.displayName = displayName;
            this.material = material;
            this.activeMaterial = activeMaterial;
        }

        public String getDisplayName() { return displayName; }
        public Material getMaterial()  { return material; }
        public Material getActiveMaterial() { return activeMaterial; }
    }
}
