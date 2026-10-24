package id.kalanusantara.core.journal;

import id.kalanusantara.core.KalaCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * JurnalManager — Mengelola Jurnal Penjelajah.
 *
 * Entry jurnal ditambahkan otomatis saat:
 *   - Quest selesai
 *   - Profesi dipilih
 *   - Discovery landmark
 *   - Event Kala dimulai/selesai
 *
 * Disimpan ke tabel journal_entries (SQLite).
 * Ditampilkan lewat /jurnal command (chat-based, Bedrock friendly).
 *
 * Tipe entry:
 *   quest      — penyelesaian quest
 *   profesi    — memilih profesi
 *   discovery  — menemukan sesuatu baru
 *   kala       — event Kala
 *   admin      — entri manual dari admin
 */
public class JurnalManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public JurnalManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
    }

    // ─────────────────────────────────────────────
    // API utama
    // ─────────────────────────────────────────────

    /**
     * Tambahkan entri jurnal (thread-safe, async DB write).
     */
    public void addEntry(Player player, String type, String title, String content) {
        UUID uuid = player.getUniqueId();
        long now  = System.currentTimeMillis();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                    "INSERT INTO journal_entries(uuid, type, title, content, timestamp) VALUES(?,?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type);
                ps.setString(3, title);
                ps.setString(4, content);
                ps.setLong(5, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[JurnalManager] Gagal simpan entri: " + e.getMessage());
            }
        });
    }

    /**
     * Tambah entri dari admin (tidak perlu Player online).
     */
    public void addEntryByUuid(UUID uuid, String type, String title, String content) {
        long now = System.currentTimeMillis();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                    "INSERT INTO journal_entries(uuid, type, title, content, timestamp) VALUES(?,?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, type);
                ps.setString(3, title);
                ps.setString(4, content);
                ps.setLong(5, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[JurnalManager] Gagal simpan entri by UUID: " + e.getMessage());
            }
        });
    }

    /**
     * Ambil entri jurnal terbaru untuk pemain (max 20, halaman).
     */
    public List<JurnalEntry> getEntries(UUID uuid, int page) {
        List<JurnalEntry> entries = new ArrayList<>();
        int offset = (page - 1) * 10;
        try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                "SELECT id, type, title, content, timestamp FROM journal_entries " +
                "WHERE uuid=? ORDER BY timestamp DESC LIMIT 10 OFFSET ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(new JurnalEntry(
                    rs.getLong("id"),
                    rs.getString("type"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            log.warning("[JurnalManager] Gagal baca jurnal: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Hitung total entri pemain.
     */
    public int countEntries(UUID uuid) {
        try (PreparedStatement ps = plugin.getStorageManager().ensureConnection().prepareStatement(
                "SELECT COUNT(*) FROM journal_entries WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.warning("[JurnalManager] Gagal count jurnal: " + e.getMessage());
        }
        return 0;
    }

    // ─────────────────────────────────────────────
    // Display helper — tampilkan jurnal ke pemain
    // ─────────────────────────────────────────────

    public void showJurnal(Player player, int page) {
        UUID uuid = player.getUniqueId();

        // Async baca, lalu sync display
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<JurnalEntry> entries = getEntries(uuid, page);
            int total = countEntries(uuid);
            int maxPage = Math.max(1, (int) Math.ceil(total / 10.0));

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text(
                    "━━━━━ §6✦ Jurnal Penjelajah §e" + player.getName() + " §6✦ §r━━━━━").asComponent());
                player.sendMessage(Component.text(
                    "§7Halaman §f" + page + "§7/§f" + maxPage +
                    " §7(§f" + total + " §7entri)").asComponent());
                player.sendMessage(Component.empty());

                if (entries.isEmpty()) {
                    player.sendMessage(Component.text(
                        "§7Jurnalmu masih kosong. Mulai petualanganmu!", NamedTextColor.GRAY).asComponent());
                } else {
                    for (JurnalEntry e : entries) {
                        String prefix = getTypePrefix(e.type());
                        String dateStr = FMT.format(
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(e.timestamp()), ZoneId.of("Asia/Jakarta")));
                        player.sendMessage(Component.text(
                            prefix + e.title()).asComponent());
                        player.sendMessage(Component.text(
                            "  §8" + dateStr).asComponent());
                        if (e.content() != null && !e.content().isEmpty()) {
                            // Tampilkan content (maks 80 char)
                            String preview = e.content().length() > 80
                                ? e.content().substring(0, 77) + "..."
                                : e.content();
                            player.sendMessage(Component.text("  §7" + preview).asComponent());
                        }
                        player.sendMessage(Component.empty());
                    }
                }

                if (page < maxPage) {
                    player.sendMessage(Component.text(
                        "§7Ketik §e/jurnal " + (page + 1) + " §7untuk halaman berikutnya.").asComponent());
                }
                player.sendMessage(Component.text(
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").asComponent());
            });
        });
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private String getTypePrefix(String type) {
        return switch (type) {
            case "quest"     -> "§6📜 ";
            case "profesi"   -> "§b⚒ ";
            case "discovery" -> "§a🗺 ";
            case "kala"      -> "§c🌋 ";
            default          -> "§7✦ ";
        };
    }

    // ─────────────────────────────────────────────
    // Data record
    // ─────────────────────────────────────────────

    public record JurnalEntry(long id, String type, String title, String content, long timestamp) {}
}
