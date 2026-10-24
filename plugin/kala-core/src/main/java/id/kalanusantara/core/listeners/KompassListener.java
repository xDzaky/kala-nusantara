package id.kalanusantara.core.listeners;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.ui.MenuManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * KompassListener — Menangani seluruh interaksi GUI menu game Kala Nusantara:
 * - Klik kanan Kompas item → Buka Kompas Pengembara GUI
 * - Klik tab dan tombol di Kompas GUI
 * - Klik di menu Dunia Tambang, Proyek Gotong Royong, dan Pesanan Pasar
 */
public class KompassListener implements Listener {

    private final KalaCorePlugin plugin;

    public KompassListener(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 1. Interaksi Kompas Pengembara
        if (item.getType() == Material.COMPASS) {
            event.setCancelled(true);
            plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.MISI);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.7f, 1.2f);
            return;
        }

        // 2. Interaksi Altar Fisik Gunung Murka (Kuil Kala: Crying Obsidian / Soul Campfire / Lodestone)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Material clickedMat = event.getClickedBlock().getType();
            if (clickedMat == Material.CRYING_OBSIDIAN || clickedMat == Material.SOUL_CAMPFIRE || clickedMat == Material.LODESTONE) {
                event.setCancelled(true);
                plugin.getMenuManager().openVolcanoGUI(player);
                player.playSound(event.getClickedBlock().getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.2f);
                player.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME,
                        event.getClickedBlock().getLocation().clone().add(0.5, 1.0, 0.5), 10, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText()
            .serialize(event.getView().title());

        // ─────────────────────────────────────────────
        // 1. KOMPAS PENGEMBARA
        // ─────────────────────────────────────────────
        if (title.contains("Kompas Pengembara")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 54) return;

            // Navigasi Tab Header (Slot 1-5)
            switch (slot) {
                case 1 -> {
                    plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.MISI);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                    return;
                }
                case 2 -> {
                    plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PETA);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                    return;
                }
                case 3 -> {
                    plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.KAMPUNG);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                    return;
                }
                case 4 -> {
                    plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PROGRES);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                    return;
                }
                case 5 -> {
                    plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PANDUAN);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                    return;
                }
                case 49 -> {
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);
                    return;
                }
            }

            // Aksi di Tab Peta (Ekspedisi, Gunung Murka & Kalender)
            if (slot == 19 || slot == 20) {
                plugin.getMenuManager().openExpeditionGUI(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            } else if (slot == 23) {
                plugin.getMenuManager().openVolcanoGUI(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            } else if (slot == 24 || slot == 25) {
                plugin.getMenuManager().openSeasonCalendarGUI(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            }

            // Aksi di Tab Progres (Atlas Fauna & Kosmetik)
            if (slot == 22) {
                plugin.getMenuManager().openAtlasFaunaGUI(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            } else if (slot == 23 || slot == 24) {
                plugin.getMenuManager().openCosmeticsGUI(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                return;
            }

            // Aksi di Tab Kampung
            if (slot == 20) { // Proyek
                plugin.getMenuManager().openProyekMenu(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            } else if (slot == 22) { // Pesanan
                plugin.getMenuManager().openPesananMenu(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            } else if (slot == 24) { // Dunia Tambang
                plugin.getMenuManager().openTambangMenu(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            } else if (slot == 30) { // Profesi
                plugin.getProfesiManager().openProfesiGui(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            } else if (slot == 32) { // Jurnal
                player.closeInventory();
                plugin.getJurnalManager().showJurnal(player, 1);
            }

            // Aksi di Tab Panduan (Tautan Cepat)
            if (slot == 19) plugin.getMenuManager().openBedrockGuideGUI(player);
            else if (slot == 20) plugin.getMenuManager().openAtlasFaunaGUI(player);
            else if (slot == 21) plugin.getMenuManager().openVolcanoGUI(player);
            else if (slot == 22) plugin.getMenuManager().openTambangMenu(player);
            else if (slot == 23) plugin.getMenuManager().openProyekMenu(player);
            else if (slot == 24) plugin.getMenuManager().openPesananMenu(player);
            else if (slot == 25) {
                player.closeInventory();
                plugin.getJurnalManager().showJurnal(player, 1);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 2. DUNIA TAMBANG GUI
        // ─────────────────────────────────────────────
        if (title.contains("Dunia Tambang Nusantara")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) { // Berangkat
                player.closeInventory();
                plugin.getResourceWorldManager().teleportToResource(player);
            } else if (slot == 15) { // Pulang
                player.closeInventory();
                plugin.getResourceWorldManager().teleportToMain(player);
            } else if (slot == 22) { // Kembali ke Kompas
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.KAMPUNG);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 3. PROYEK GOTONG ROYONG GUI
        // ─────────────────────────────────────────────
        if (title.contains("Proyek Gotong Royong")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                plugin.getKampungProjectManager().contribute(player, "lumbung_padi", -1);
                plugin.getMenuManager().openProyekMenu(player);
            } else if (slot == 13) {
                plugin.getKampungProjectManager().contribute(player, "dermaga_kayu", -1);
                plugin.getMenuManager().openProyekMenu(player);
            } else if (slot == 15) {
                plugin.getKampungProjectManager().contribute(player, "benteng_batu", -1);
                plugin.getMenuManager().openProyekMenu(player);
            } else if (slot == 22) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.KAMPUNG);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 4. PESANAN PEDAGANG PASAR GUI
        // ─────────────────────────────────────────────
        if (title.contains("Pesanan Pedagang Pasar")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int[] orderSlots = {10, 11, 12, 13, 14, 15, 16, 19};

            for (int i = 0; i < orderSlots.length; i++) {
                if (slot == orderSlots[i]) {
                    int orderIndex = i + 1; // 1-based index
                    plugin.getPesananManager().submitOrder(player, orderIndex);
                    plugin.getMenuManager().openPesananMenu(player);
                    return;
                }
            }

            if (slot == 22) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.KAMPUNG);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 5. KALENDER MUSIM GUI
        // ─────────────────────────────────────────────
        if (title.contains("Kalender Musim Nusantara")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 22) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PETA);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 6. GELAR & KOSMETIK GUI
        // ─────────────────────────────────────────────
        if (title.contains("Gelar & Kosmetik Nusantara")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            // Pasang Gelar (Slot 10..14)
            String[] titleKeys = {"PENGEMBARA", "PEMBANGUN_KAMPUNG", "SAUDAGAR_NUSANTARA", "PENAKLUK_KALA", "PENJELAJAH_SAMUDRA"};
            if (slot >= 10 && slot <= 14) {
                int idx = slot - 10;
                String selected = titleKeys[idx];
                var data = plugin.getCosmeticManager().getData(player.getUniqueId());
                if (data.unlockedTitles.contains(selected)) {
                    plugin.getCosmeticManager().setActiveTitle(player, selected);
                    plugin.getMenuManager().openCosmeticsGUI(player);
                } else {
                    player.sendMessage(net.kyori.adventure.text.Component.text("§cGelar ini belum kamu buka! Selesaikan tantangan petualangan untuk membukanya."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
                }
                return;
            }

            // Pasang Partikel Trail (Slot 19..23)
            String[] trailKeys = {"NONE", "MELATI", "BARA", "OMBAK", "RIMBA"};
            if (slot >= 19 && slot <= 23) {
                int idx = slot - 19;
                plugin.getCosmeticManager().setActiveTrail(player, trailKeys[idx]);
                plugin.getMenuManager().openCosmeticsGUI(player);
                return;
            }

            if (slot == 26) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PROGRES);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 7. EKSPEDISI MUSIMAN GUI
        // ─────────────────────────────────────────────
        if (title.contains("Ekspedisi Musiman")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 22) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PETA);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 8. ATLAS FAUNA NUSANTARA GUI
        // ─────────────────────────────────────────────
        if (title.contains("Atlas Fauna Nusantara")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 31) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PANDUAN);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            } else if (slot == 35) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 9. STATUS & SAJEN GUNUNG MURKA GUI
        // ─────────────────────────────────────────────
        if (title.contains("Aktivitas Gunung Murka")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (slot == 19) {
                // Donasi Arang / Batubara
                Material mat = player.getInventory().contains(Material.COAL) ? Material.COAL : Material.CHARCOAL;
                if (plugin.getKalaEventManager().contributeOffering(player, mat, 1)) {
                    plugin.getMenuManager().openVolcanoGUI(player);
                }
            } else if (slot == 21) {
                // Donasi Tembaga
                Material mat = player.getInventory().contains(Material.COPPER_INGOT) ? Material.COPPER_INGOT : Material.RAW_COPPER;
                if (plugin.getKalaEventManager().contributeOffering(player, mat, 1)) {
                    plugin.getMenuManager().openVolcanoGUI(player);
                }
            } else if (slot == 23) {
                // Donasi Bahan Makanan
                Material mat = player.getInventory().contains(Material.BREAD) ? Material.BREAD : Material.WHEAT;
                if (plugin.getKalaEventManager().contributeOffering(player, mat, 1)) {
                    plugin.getMenuManager().openVolcanoGUI(player);
                }
            } else if (slot == 26) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PETA);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }

        // ─────────────────────────────────────────────
        // 10. PANDUAN KONTROL SENTUH BEDROCK GUI
        // ─────────────────────────────────────────────
        if (title.contains("Panduan Kontrol & HP")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (slot == 16) {
                // Toggle notifikasi tips/audio
                if (plugin.getNotificationService() != null) {
                    boolean newState = plugin.getNotificationService().toggleTips(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                    player.sendMessage(net.kyori.adventure.text.Component.text("§8[§b📱 Bedrock§8] §7Notifikasi tips: " + (newState ? "§aDiaktifkan" : "§cDinonaktifkan")));
                    plugin.getMenuManager().openBedrockGuideGUI(player);
                }
            } else if (slot == 22) {
                plugin.getMenuManager().openKompas(player, MenuManager.KompasTab.PANDUAN);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            }
            return;
        }
    }
}
