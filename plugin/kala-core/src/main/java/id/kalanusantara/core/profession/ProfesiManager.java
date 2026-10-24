package id.kalanusantara.core.profession;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * ProfesiManager — Mengelola sistem pemilihan profesi.
 *
 * Membuka GUI 54-slot untuk memilih profesi.
 * Disimpan ke PlayerProfile.profesi (String = enum key).
 * Satu kali pilih, bisa ganti lewat command admin atau quest tertentu.
 */
public class ProfesiManager implements Listener {

    private final KalaCorePlugin plugin;

    // Inventory title untuk identifikasi
    private static final String GUI_TITLE = "✦ Pilih Profesimu ✦";

    public ProfesiManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────
    // Buka GUI pemilihan profesi
    // ─────────────────────────────────────────────

    public void openProfesiGui(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54,
            Component.text(GUI_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));

        // Profesi yang bisa dipilih (PENGEMBARA tidak bisa dipilih manual)
        KalaProfession[] available = {
            KalaProfession.TUKANG_KAYU,
            KalaProfession.PANDAI_BESI,
            KalaProfession.PETANI,
            KalaProfession.NELAYAN,
            KalaProfession.PERAMU,
            KalaProfession.PELAUT
        };

        // Slot tengah yang estetik: row 2 & 3, kolom 2-7
        int[] slots = {10, 12, 14, 16, 28, 30};

        // Dapatkan profesi saat ini
        String currentProfesi = plugin.getIdentityManager()
            .get(player.getUniqueId())
            .map(p -> p.getProfesi())
            .orElse("Pengembara");

        for (int i = 0; i < available.length && i < slots.length; i++) {
            KalaProfession prof = available[i];
            boolean isCurrent = prof.getDisplayName().equalsIgnoreCase(currentProfesi);

            ItemStack item = createProfesiItem(prof, isCurrent);
            inv.setItem(slots[i], item);
        }

        // Border dekorasi
        ItemStack border = createGlass(
            Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }

        // Info pemain saat ini
        ItemStack info = createInfo(player, currentProfesi);
        inv.setItem(4, info);

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────
    // Klik handler
    // ─────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title().equals(Component.text(GUI_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD))) {
            event.setCancelled(true);
        }

        // Cek judul inventory
        String title = event.getView().title().toString();
        if (!title.contains(GUI_TITLE.replace("✦ ", "").replace(" ✦", ""))) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        // Cari profesi dari display name item
        String displayName = clicked.getItemMeta().displayName().toString();

        KalaProfession chosen = null;
        for (KalaProfession p : KalaProfession.values()) {
            if (displayName.contains(p.getDisplayName())) {
                chosen = p;
                break;
            }
        }
        if (chosen == null || chosen == KalaProfession.PENGEMBARA) return;

        setProfesi(player, chosen);
        player.closeInventory();
    }

    // ─────────────────────────────────────────────
    // Set profesi
    // ─────────────────────────────────────────────

    public void setProfesi(Player player, KalaProfession profesi) {
        UUID uuid = player.getUniqueId();

        plugin.getIdentityManager().get(uuid).ifPresent(profile -> {
            String oldProfesi = profile.getProfesi();
            profile.setProfesi(profesi.getDisplayName());

            // Simpan async
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getIdentityManager().saveProfile(profile)
            );

            // Notifikasi
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            player.sendMessage(Component.text("  ✦ PROFESI DIPILIH!", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.text("  " + profesi.getDisplayName(), NamedTextColor.YELLOW, TextDecoration.BOLD));
            player.sendMessage(Component.text("  " + profesi.getDescription(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);

            // Update scoreboard
            plugin.getScoreboardManager().update(player);

            // Jurnal entry
            plugin.getJurnalManager().addEntry(player, "profesi",
                "Memilih Profesi: " + profesi.getDisplayName(),
                "Aku memutuskan untuk menjadi seorang " + profesi.getDisplayName() + ".\n" + profesi.getDescription());

            // Auto-start quest profesi jika ada
            if (profesi.getStartQuestId() != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getQuestProgressTracker().startQuest(player, profesi.getStartQuestId());
                    }
                }, 60L);
            }
        });
    }

    // ─────────────────────────────────────────────
    // Item builders
    // ─────────────────────────────────────────────

    private ItemStack createProfesiItem(KalaProfession prof, boolean isCurrent) {
        Material mat;
        try {
            mat = Material.valueOf(prof.getIconMaterial());
        } catch (Exception e) {
            mat = Material.BOOK;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String prefix = isCurrent ? "§a✔ " : "§e";
        meta.displayName(Component.text(prefix + prof.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + prof.getDescription()));
        lore.add(Component.empty());
        if (isCurrent) {
            lore.add(Component.text("§a✔ Profesi Aktifmu", NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("§eKlik untuk memilih profesi ini", NamedTextColor.YELLOW));
        }
        if (prof.getStartQuestId() != null) {
            lore.add(Component.text("§7Quest: §f" + prof.getStartQuestId(), NamedTextColor.GRAY));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfo(Player player, String currentProfesi) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6" + player.getName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Profesi saat ini: §e" + currentProfesi));
            lore.add(Component.empty());
            lore.add(Component.text("§7Pilih profesi untuk memulai", NamedTextColor.GRAY));
            lore.add(Component.text("§7jalur keahlianmu.", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
