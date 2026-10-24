package id.kalanusantara.core.ui;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * TabListManager — Atur header & footer Tab List (player list)
 *
 * Header:
 *   ✦ ═══════════════════════════════════ ✦
 *      KALA NUSANTARA — Co-op Survival RPG
 *   ✦ ═══════════════════════════════════ ✦
 *
 * Footer:
 *   Java 192.168.1.14:25565 | Bedrock :19132
 *   Online: X/20 | Musim: Kemarau
 */
public class TabListManager {

    private final KalaCorePlugin plugin;

    public TabListManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    /** Update tab header & footer untuk satu pemain */
    public void update(Player player) {
        Optional<PlayerProfile> profileOpt = plugin.getIdentityManager()
                .get(player.getUniqueId());

        String profesi = profileOpt.map(PlayerProfile::getProfesi).orElse("Pengembara");
        int online = Bukkit.getOnlinePlayers().size();
        int max    = Bukkit.getMaxPlayers();

        // ── Header ────────────────────────────────────────────────
        Component header = Component.empty()
            .append(Component.newline())
            .append(Component.text("✦ ══════════════════════════════════ ✦", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("   KALA NUSANTARA", NamedTextColor.YELLOW)
                              .decorate(TextDecoration.BOLD))
            .append(Component.text("  —  Co-op Survival RPG Nusantara", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("✦ ══════════════════════════════════ ✦", NamedTextColor.GOLD))
            .append(Component.newline());

        // ── Footer ────────────────────────────────────────────────
        Component footer = Component.empty()
            .append(Component.newline())
            .append(Component.text("  Profesimu: ", NamedTextColor.GRAY))
            .append(Component.text(profesi, NamedTextColor.YELLOW))
            .append(Component.text("   |   ", NamedTextColor.DARK_GRAY))
            .append(Component.text("Online: ", NamedTextColor.GRAY))
            .append(Component.text(online + "/" + max, NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("  Java: ", NamedTextColor.DARK_GRAY))
            .append(Component.text("192.168.1.14:25565", NamedTextColor.GRAY))
            .append(Component.text("   Bedrock: ", NamedTextColor.DARK_GRAY))
            .append(Component.text(":19132", NamedTextColor.GRAY))
            .append(Component.newline());

        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    /** Update semua pemain online */
    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }

    /** Reset ke default (hapus header/footer) */
    public void reset(Player player) {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }
}
