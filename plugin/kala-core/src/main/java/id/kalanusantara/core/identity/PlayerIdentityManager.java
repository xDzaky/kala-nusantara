package id.kalanusantara.core.identity;

import id.kalanusantara.core.KalaCorePlugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * PlayerIdentityManager — profil pemain, kasta, kampung
 */
public class PlayerIdentityManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    // Cache in-memory saat pemain online
    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public PlayerIdentityManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void initialize() {
        log.info("IdentityManager siap (cache in-memory + SQLite)");
    }

    /**
     * Muat profil dari DB, buat baru jika belum ada
     */
    public PlayerProfile loadOrCreate(UUID uuid, String name) {
        if (cache.containsKey(uuid)) return cache.get(uuid);

        try {
            Connection conn = plugin.getStorageManager().ensureConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM player_profile WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            PlayerProfile profile;
            if (rs.next()) {
                profile = ProfileMapper.fromResultSet(rs);
                profile.setName(name); // update nama kalau ganti
            } else {
                // Pemain baru!
                profile = new PlayerProfile(uuid, name);
                profile.setFirstJoin(System.currentTimeMillis());
                profile.setLastSeen(System.currentTimeMillis());
                profile.setBalance(plugin.getKalaConfig().getStartingBalance());
                insertProfile(conn, profile);
                log.info("Profil baru dibuat untuk: " + name);
            }
            rs.close();
            ps.close();

            cache.put(uuid, profile);
            return profile;

        } catch (SQLException e) {
            log.severe("Gagal load profil " + name + ": " + e.getMessage());
            // Return temporary profile (tidak disimpan)
            return new PlayerProfile(uuid, name);
        }
    }

    private void insertProfile(Connection conn, PlayerProfile p) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO player_profile (uuid, name, profesi, kampung, balance, first_join, last_seen, tutorial_done) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setString(1, p.getUuid().toString());
        ps.setString(2, p.getName());
        ps.setString(3, p.getProfesi());
        ps.setString(4, p.getKampung());
        ps.setDouble(5, p.getBalance());
        ps.setLong(6, p.getFirstJoin());
        ps.setLong(7, p.getLastSeen());
        ps.setInt(8, p.isTutorialDone() ? 1 : 0);
        ps.executeUpdate();
        ps.close();
    }

    public void saveProfile(PlayerProfile profile) {
        try {
            Connection conn = plugin.getStorageManager().ensureConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE player_profile SET name=?, profesi=?, kampung=?, balance=?, " +
                "last_seen=?, playtime_seconds=?, tutorial_done=?, locale=? WHERE uuid=?");
            ps.setString(1, profile.getName());
            ps.setString(2, profile.getProfesi());
            ps.setString(3, profile.getKampung());
            ps.setDouble(4, profile.getBalance());
            ps.setLong(5, profile.getLastSeen());
            ps.setLong(6, profile.getPlaytimeSeconds());
            ps.setInt(7, profile.isTutorialDone() ? 1 : 0);
            ps.setString(8, profile.getLocale());
            ps.setString(9, profile.getUuid().toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            log.severe("Gagal menyimpan profil " + profile.getName() + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        cache.values().forEach(this::saveProfile);
        log.info("Semua profil pemain disimpan (" + cache.size() + " profil)");
    }

    public void unload(UUID uuid) {
        PlayerProfile p = cache.remove(uuid);
        if (p != null) {
            p.setLastSeen(System.currentTimeMillis());
            saveProfile(p);
        }
    }

    public Optional<PlayerProfile> get(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public boolean isOnline(UUID uuid) {
        return cache.containsKey(uuid);
    }
}
