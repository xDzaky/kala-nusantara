package id.kalanusantara.core.identity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ProfileMapper {

    public static PlayerProfile fromResultSet(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");
        PlayerProfile p = new PlayerProfile(uuid, name);
        p.setProfesi(rs.getString("profesi"));
        p.setKampung(rs.getString("kampung"));
        p.setBalance(rs.getDouble("balance"));
        p.setFirstJoin(rs.getLong("first_join"));
        p.setLastSeen(rs.getLong("last_seen"));
        p.setPlaytimeSeconds(rs.getLong("playtime_seconds"));
        p.setTutorialDone(rs.getInt("tutorial_done") == 1);
        String locale = rs.getString("locale");
        p.setLocale(locale != null ? locale : "id");
        return p;
    }
}
