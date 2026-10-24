package id.kalanusantara.core.season;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * SeasonManager — Mengelola siklus 8 minggu musiman Kala Nusantara (Fase 6).
 * Sesuai Master Design Bab 8:
 * - Siklus 8 minggu dengan dampak bertahap
 * - Minggu 1-4: Pembangunan & Perdagangan Normal
 * - Minggu 5-6: Peringatan & Persiapan Logistik
 * - Minggu 7: Peristiwa Akbar Kala
 * - Minggu 8: Pemulihan & Festival Kepulauan
 */
public class SeasonManager {

    private final KalaCorePlugin plugin;
    private final Logger log;

    private int seasonNumber = 1;
    private int currentWeek = 1;
    private String seasonName = "Musim I: Rempah Samudra Nusantara";

    public SeasonManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        // Load dari config
        seasonNumber = plugin.getKalaConfig().getRaw().getInt("season.number", 1);
        currentWeek  = plugin.getKalaConfig().getRaw().getInt("season.current_week", 1);
        seasonName   = plugin.getKalaConfig().getRaw().getString("season.name", "Musim I: Rempah Samudra Nusantara");
        log.info("[SeasonManager] Musim aktif: " + seasonName + " (Minggu " + currentWeek + "/8)");
    }

    public int getSeasonNumber() { return seasonNumber; }
    public int getCurrentWeek()  { return currentWeek; }
    public String getSeasonName(){ return seasonName; }

    public void setCurrentWeek(int week) {
        if (week < 1) week = 1;
        if (week > 8) week = 8;
        this.currentWeek = week;
        plugin.getKalaConfig().getRaw().set("season.current_week", week);
        plugin.saveConfig();

        // Broadcast perubahan minggu
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("📅 Pergantian Musim Nusantara!", NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text(getWeekTitle(week), NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("Dampak: " + getWeekBonus(week), NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text("═══════════════════════════════════════", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.empty());
    }

    public void advanceWeek() {
        int next = currentWeek + 1;
        if (next > 8) {
            next = 1;
            seasonNumber++;
            plugin.getKalaConfig().getRaw().set("season.number", seasonNumber);
        }
        setCurrentWeek(next);
    }

    public String getWeekTitle(int week) {
        return switch (week) {
            case 1 -> "Minggu 1: Fajar Musim Baru (Eksplorasi)";
            case 2 -> "Minggu 2: Pelayaran Rempah Samudra";
            case 3 -> "Minggu 3: Panen Raya & Kerajinan Desa";
            case 4 -> "Minggu 4: Pembangunan Infrastruktur Bersama";
            case 5 -> "Minggu 5: Tanda-tanda Gejolak Alam Mistis";
            case 6 -> "Minggu 6: Sirine Darurat & Lumbung Logistik";
            case 7 -> "Minggu 7: Puncak Peristiwa Akbar Kala";
            case 8 -> "Minggu 8: Pemulihan Lahan & Festival Nusantara";
            default -> "Minggu " + week;
        };
    }

    public String getWeekDescription(int week) {
        return switch (week) {
            case 1 -> "Warga membuka lahan baru dan memulai rute perdagangan antar-pulau.";
            case 2 -> "Arus laut tenang, hasil pancingan dan navigasi kapal berlimpah.";
            case 3 -> "Kesuburan tanah meningkat, hasil panen dan perkebunan melimpah.";
            case 4 -> "Fokus gotong royong menyelesaikan proyek-proyek kampung.";
            case 5 -> "Mbah Kala mencatat getaran tanah; peta risiko mulai dipasang.";
            case 6 -> "Warga diimbau mengamankan barang berharga dan mengisi lumbung padi.";
            case 7 -> "Kemarahan alam Kala memuncak; langit malam gelap dan petir atmosferik.";
            case 8 -> "Alam kembali tenang; berkah kesuburan dan reruntuhan kuno terbuka!";
            default -> "Siklus hidup damai di kepulauan.";
        };
    }

    public String getWeekBonus(int week) {
        return switch (week) {
            case 1 -> "Bonus EXP +15% untuk semua aktivitas eksplorasi";
            case 2 -> "Tangkapan nelayan & kecepatan kapal meningkat";
            case 3 -> "Peluang double crop panen meningkat untuk petani";
            case 4 -> "Bonus imbalan Rupiah untuk gotong royong proyek desa";
            case 5 -> "Petunjuk lokasi reruntuhan kuno mulai tersingkap";
            case 6 -> "Harga pesanan bahan pangan di pasar meningkat 25%";
            case 7 -> "Tantangan bertahan hidup aktif; spawn reruntuhan mistis";
            case 8 -> "Festival pasar: bonus belanja & pesta rakyat nusantara";
            default -> "Kondisi normal";
        };
    }
}
