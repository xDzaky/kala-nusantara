package id.kalanusantara.core.npc;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * PesananManager — sistem pesanan kampung sederhana.
 *
 * Pedagang minta X item → pemain serahkan → dapat Rupiah Kala.
 * Pesanan dirotasi setiap periode tertentu (konfigurabel).
 * Saat ini: pesanan statis dari kode (nanti bisa YAML-driven).
 */
public class PesananManager {

    private final KalaCorePlugin plugin;

    // Daftar pesanan aktif (statis untuk MVP)
    private final List<Pesanan> pesananList = new ArrayList<>();

    // Cooldown per pemain (UUID -> last submit time ms)
    private final Map<UUID, Long> submitCooldown = new HashMap<>();
    private static final long SUBMIT_COOLDOWN_MS = 3000;

    public PesananManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        initDefaultOrders();
    }

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Tampilkan daftar pesanan ke pemain melalui chat.
     * (Bedrock-friendly: pakai teks, bukan GUI kompleks)
     */
    public void showOrderMenu(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.7f, 1.0f);
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("━━━━━━ §b⚑ Pesanan Pedagang Pasar §7━━━━━━").asComponent());
        player.sendMessage(Component.text("§7Serahkan item ke pedagang untuk mendapat Rupiah Kala.").asComponent());
        player.sendMessage(Component.empty());

        for (int i = 0; i < pesananList.size(); i++) {
            Pesanan p = pesananList.get(i);
            String status = hasSufficientItems(player, p) ? "§a[✔ Bisa Serahkan]" : "§c[✘ Belum cukup]";
            player.sendMessage(Component.text(
                String.format("  §e[%d] §f%s x%d §7→ §aRp %,d  %s",
                    i + 1, formatMaterial(p.material), p.amount, (int) p.reward, status)
            ).asComponent());
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§7Ketik §e/pesanan serahkan <nomor> §7untuk menyerahkan item.").asComponent());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").asComponent());
        player.sendMessage(Component.empty());
    }

    /**
     * Serahkan pesanan ke-idx (1-based).
     */
    public void submitOrder(Player player, int orderIndex) {
        // Cooldown check
        long now = System.currentTimeMillis();
        Long last = submitCooldown.get(player.getUniqueId());
        if (last != null && now - last < SUBMIT_COOLDOWN_MS) {
            player.sendMessage(Component.text("§cTunggu sebentar sebelum menyerahkan lagi.", NamedTextColor.RED).asComponent());
            return;
        }

        if (orderIndex < 1 || orderIndex > pesananList.size()) {
            player.sendMessage(Component.text("§cNomor pesanan tidak valid.", NamedTextColor.RED).asComponent());
            return;
        }

        Pesanan p = pesananList.get(orderIndex - 1);

        // Cek item di inventory
        if (!hasSufficientItems(player, p)) {
            player.sendMessage(Component.text(
                "§cKamu tidak punya cukup §f" + formatMaterial(p.material) +
                " §cx" + p.amount + "§c.", NamedTextColor.RED).asComponent());
            return;
        }

        // Ambil item dari inventory
        removeItems(player, p.material, p.amount);

        // Beri reward
        plugin.getEconomy().deposit(player.getUniqueId(), p.reward,
            "Pesanan: " + p.material.name() + " x" + p.amount);

        submitCooldown.put(player.getUniqueId(), now);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("§a✔ Pesanan diterima! §eRp " + String.format("%,d", (int) p.reward) +
            " §adidepositkan ke akunmu.").asComponent());
        player.sendMessage(Component.empty());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);

        // Progress quest jika ada step first_order
        plugin.getQuestProgressTracker().progressManual(player, "tutorial_labuhan", "first_order");
    }

    // ─────────────────────────────────────────────────────────
    // Default orders
    // ─────────────────────────────────────────────────────────

    private void initDefaultOrders() {
        pesananList.clear();
        // Pesanan dasar untuk MVP — nanti YAML-driven
        pesananList.add(new Pesanan(Material.OAK_LOG, 10, 150));
        pesananList.add(new Pesanan(Material.OAK_PLANKS, 16, 80));
        pesananList.add(new Pesanan(Material.COBBLESTONE, 32, 120));
        pesananList.add(new Pesanan(Material.WHEAT, 10, 200));
        pesananList.add(new Pesanan(Material.BREAD, 5, 180));
        pesananList.add(new Pesanan(Material.COAL, 8, 160));
        pesananList.add(new Pesanan(Material.IRON_INGOT, 3, 350));
        pesananList.add(new Pesanan(Material.DIRT, 64, 60));
    }

    // ─────────────────────────────────────────────────────────
    // Inventory helpers
    // ─────────────────────────────────────────────────────────

    public boolean hasSufficientItems(Player player, Pesanan p) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == p.material) {
                count += item.getAmount();
            }
        }
        return count >= p.amount;
    }

    private void removeItems(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == mat) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        player.updateInventory();
    }

    public String formatMaterial(Material mat) {
        String name = mat.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────
    // Data model
    // ─────────────────────────────────────────────────────────

    public record Pesanan(Material material, int amount, double reward) {}

    public List<Pesanan> getPesananList() {
        return Collections.unmodifiableList(pesananList);
    }
}
