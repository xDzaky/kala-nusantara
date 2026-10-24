package id.kalanusantara.core.listeners;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import id.kalanusantara.core.quest.Quest;
import id.kalanusantara.core.quest.QuestProgressTracker;
import id.kalanusantara.core.quest.QuestStep;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * QuestEventListener — mendengarkan event gameplay dan meneruskan ke QuestProgressTracker.
 *
 * Action yang didukung:
 *   COLLECT  — BlockBreakEvent (material cocok)
 *   CRAFT    — CraftItemEvent  (material cocok)
 *   TALK     — PlayerInteractEntityEvent (ditangani NpcManager/MbahKalaNpc)
 *   MANUAL   — tidak auto-tracked; selesai lewat command admin
 */
public class QuestEventListener implements Listener {

    private final KalaCorePlugin plugin;

    public QuestEventListener(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────
    // COLLECT — pemain menebang/menambang blok
    // ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String materialName = event.getBlock().getType().name();

        // Drop juga menghasilkan item — cukup trigger sekali per break
        plugin.getQuestProgressTracker().progress(player, "COLLECT", materialName, 1);
    }

    // ─────────────────────────────────────────────
    // CRAFT — pemain men-craft item
    // ─────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.AIR) return;

        // Hitung kuantitas yang di-craft (shift-click bisa banyak sekaligus)
        int amount = result.getAmount();
        // Jika shift-click, cek berapa kali bisa dibuat
        if (event.isShiftClick()) {
            // Pakai nilai aman: amount * count maks dari matrix
            int times = getMaxCraftTimes(event);
            amount = result.getAmount() * times;
        }

        plugin.getQuestProgressTracker().progress(player, "CRAFT", result.getType().name(), amount);
    }

    /**
     * Hitung max craft count untuk shift-click (estimasi sederhana).
     */
    private int getMaxCraftTimes(CraftItemEvent event) {
        int min = Integer.MAX_VALUE;
        for (ItemStack ing : event.getInventory().getMatrix()) {
            if (ing != null && ing.getType() != Material.AIR) {
                min = Math.min(min, ing.getAmount());
            }
        }
        return min == Integer.MAX_VALUE ? 1 : min;
    }
}
