package id.kalanusantara.core.ui;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KalaScoreboardManager — Sidebar Scoreboard Persisten Nusantara (Kala Nusantara v7.0).
 *
 * Audit V7 P2 Fix:
 * 1. Tidak lagi unregister/register Objective setiap 2 detik (menghilangkan packet storm & screen flicker).
 * 2. Reusable Objective & Teams dengan caching baris sebelumnya.
 * 3. Batas ketat 7 baris esensial (tanpa spacer ganda atau baris redundant).
 * 4. Komponen Adventure modern yang ramah crossplay Java & Bedrock MCPE.
 */
public class KalaScoreboardManager {

    private final KalaCorePlugin plugin;
    private final Map<UUID, Scoreboard> playerBoards = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, Component>> lastRenderedLines = new ConcurrentHashMap<>();
    private static final DecimalFormat NUM_FMT = new DecimalFormat("#,###");

    public KalaScoreboardManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Buat atau perbarui scoreboard untuk seorang pemain
     */
    public void update(Player player) {
        if (player == null || !player.isOnline()) return;

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        Scoreboard board = playerBoards.computeIfAbsent(player.getUniqueId(), k -> mgr.getNewScoreboard());

        // P2 Fix: Reusable Objective — jangan unregister setiap update!
        Objective obj = board.getObjective("kala_sidebar");
        if (obj == null) {
            obj = board.registerNewObjective(
                    "kala_sidebar",
                    Criteria.DUMMY,
                    Component.text("✦ KALA NUSANTARA ✦", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            );
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        PlayerProfile profile = plugin.getIdentityManager().get(player.getUniqueId()).orElse(null);

        // ── Data pemain & lingkungan ──────────────────────────
        String profesi = profile != null ? profile.getProfesi() : "Pengembara";
        double saldo   = profile != null ? profile.getBalance() : 0;
        int online     = Bukkit.getOnlinePlayers().size();

        String musim = plugin.getSeasonManager() != null
                ? plugin.getSeasonManager().getSeasonName()
                : "Musim I";

        String worldName = player.getWorld().getName();
        String lokasi = switch (worldName) {
            case "kala_main" -> "Labuhan Awal";
            case "kala_resource" -> "Dunia Tambang";
            default -> worldName;
        };

        String gunungStatus = plugin.getKalaEventManager() != null
                ? plugin.getKalaEventManager().getCurrentPhase().getDisplayName()
                : "§7Tenang";

        // Proyek aktif
        String proyekStr = "§7-";
        if (plugin.getKampungProjectManager() != null) {
            var all = plugin.getKampungProjectManager().getAllProjects();
            for (var p : all) {
                if ("ACTIVE".equalsIgnoreCase(p.getStatus())) {
                    proyekStr = p.getName() + " §e" + (int) p.getProgressPercentage() + "%";
                    break;
                }
            }
        }

        // ── 7 Baris Esensial (Audit V7 Standar) ────────────────────────
        Map<Integer, Component> userCache = lastRenderedLines.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());

        updateLineIfChanged(board, obj, userCache, 7, Component.text("📅 Musim: ", NamedTextColor.GOLD).append(Component.text(musim, NamedTextColor.WHITE)));
        updateLineIfChanged(board, obj, userCache, 6, Component.text("📍 Lokasi: ", NamedTextColor.GREEN).append(Component.text(lokasi, NamedTextColor.WHITE)));
        updateLineIfChanged(board, obj, userCache, 5, Component.text("⚔ Profesi: ", NamedTextColor.YELLOW).append(Component.text(profesi, NamedTextColor.WHITE)));
        updateLineIfChanged(board, obj, userCache, 4, Component.text("🏛 Proyek: ", NamedTextColor.AQUA).append(Component.text(proyekStr, NamedTextColor.WHITE)));
        updateLineIfChanged(board, obj, userCache, 3, Component.text("💰 Dompet: ", NamedTextColor.GOLD).append(Component.text("Rp " + formatNumber(saldo), NamedTextColor.GREEN)));
        updateLineIfChanged(board, obj, userCache, 2, Component.text("🌋 Gunung: ", NamedTextColor.RED).append(Component.text(gunungStatus)));
        updateLineIfChanged(board, obj, userCache, 1, Component.text("👥 Online: ", NamedTextColor.GRAY).append(Component.text(online + " warga", NamedTextColor.WHITE)));

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    /**
     * P2 Fix: Hanya update prefix Team jika komponen berubah
     */
    private void updateLineIfChanged(Scoreboard board, Objective obj, Map<Integer, Component> cache, int score, Component component) {
        Component previous = cache.get(score);
        if (previous != null && previous.equals(component)) {
            return; // Konten sama persis — hindari kirim paket
        }
        cache.put(score, component);

        String teamName = "kl_" + score;
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        String entry = getEntry(score);
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }

        team.prefix(component);
        obj.getScore(entry).setScore(score);
    }

    public void remove(Player player) {
        if (player == null) return;
        playerBoards.remove(player.getUniqueId());
        lastRenderedLines.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }

    private String getEntry(int score) {
        return switch (score) {
            case 7 -> "§1§r";
            case 6 -> "§2§r";
            case 5 -> "§3§r";
            case 4 -> "§4§r";
            case 3 -> "§5§r";
            case 2 -> "§6§r";
            case 1 -> "§7§r";
            default -> "§8§r";
        };
    }

    private String formatNumber(double value) {
        return NUM_FMT.format(value);
    }
}
