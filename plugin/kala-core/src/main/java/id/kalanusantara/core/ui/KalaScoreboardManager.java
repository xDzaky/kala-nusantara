package id.kalanusantara.core.ui;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ScoreboardManager — Sidebar scoreboard kustom Kala Nusantara
 *
 * Layout sidebar:
 * ─────────────────
 *  ✦ KALA NUSANTARA ✦
 * ─────────────────
 *  Profesi: Pengembara
 *  Kampung: -
 *
 *  ⚔ Misi Aktif
 *  Tutorial: Labuhan...
 *
 *  💰 Saldo
 *  Rp 500
 *
 *  🌐 Online: 1
 * ─────────────────
 */
public class KalaScoreboardManager {

    private final KalaCorePlugin plugin;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    public KalaScoreboardManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    /** Buat atau perbarui scoreboard untuk seorang pemain */
    public void update(Player player) {
        PlayerProfile profile = plugin.getIdentityManager()
                .get(player.getUniqueId()).orElse(null);

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        Scoreboard board = playerBoards.computeIfAbsent(
                player.getUniqueId(), k -> mgr.getNewScoreboard());

        // Hapus objective lama lalu buat ulang agar baris bisa diupdate
        Objective old = board.getObjective("kala_sidebar");
        if (old != null) old.unregister();

        Objective obj = board.registerNewObjective(
                "kala_sidebar",
                Criteria.DUMMY,
                Component.text("✦ KALA NUSANTARA ✦", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // ── Data pemain & lingkungan ──────────────────────────
        String profesi   = profile != null ? profile.getProfesi() : "Pengembara";
        double saldo     = profile != null ? profile.getBalance() : 0;
        boolean tutorial = profile != null && profile.isTutorialDone();
        int online       = Bukkit.getOnlinePlayers().size();

        String musim = plugin.getSeasonManager() != null 
                ? plugin.getSeasonManager().getSeasonName() 
                : "Musim I: Samudra";
        
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

        // ── Baris (skor = urutan tampil, makin besar = makin atas) ───
        int line = 16;

        setLine(board, obj, line--, "§r");                                       // spasi atas
        setLine(board, obj, line--, "§6📅 Musim: §f" + musim);
        setLine(board, obj, line--, "§a📍 Lokasi: §f" + lokasi);
        setLine(board, obj, line--, "§e⚔ Misi: §f" + (tutorial ? "§aBantu Desa ✓" : "§7Tutorial Awal"));
        setLine(board, obj, line--, "§b🏛 Proyek: §f" + proyekStr);
        setLine(board, obj, line--, "§6💰 Dompet: §aRp " + formatNumber(saldo));
        setLine(board, obj, line--, "§c🌋 Gunung: " + gunungStatus);
        setLine(board, obj, line--, "§7👥 Online: §f" + online + " §7pengembara");
        setLine(board, obj, line--, "§r§4");                                     // spasi
        setLine(board, obj, line,   "§e/kompas — Menu Utama");

        player.setScoreboard(board);
    }

    /** Hapus scoreboard — kembali ke default server */
    public void remove(Player player) {
        playerBoards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /** Perbarui semua pemain online (dipanggil setiap N detik) */
    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            update(p);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void setLine(Scoreboard board, Objective obj, int score, String text) {
        // Setiap entri harus unik — kita pakai text langsung sebagai team
        String teamName = "kl_" + score;
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        // Entry unik per baris (invisible color code trick)
        String entry = getEntry(score);
        team.addEntry(entry);

        // Prefix = konten baris (max 64 char setelah MC 1.13 tidak ada batasan via Adventure)
        team.prefix(Component.text(text));

        // Beri skor
        Score s = obj.getScore(entry);
        s.setScore(score);
    }

    /** Entry unik per skor menggunakan kode warna tersembunyi */
    private String getEntry(int score) {
        // Pakai kombinasi §0–§f untuk membuat string unik per baris
        String[] codes = {"§0","§1","§2","§3","§4","§5","§6","§7",
                          "§8","§9","§a","§b","§c","§d","§e","§f","§k"};
        return codes[score % codes.length] + "§r";
    }

    private String formatNumber(double val) {
        if (val >= 1_000_000) return String.format("%.1fJt", val / 1_000_000);
        if (val >= 1_000)     return String.format("%.1fRb", val / 1_000);
        return String.format("%.0f", val);
    }
}
