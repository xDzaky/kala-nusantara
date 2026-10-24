package id.kalanusantara.core.identity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerProfile {

    private UUID uuid;
    private String name;
    private String profesi;
    private String kampung;
    private double balance;
    private long firstJoin;
    private long lastSeen;
    private long playtimeSeconds;
    private boolean tutorialDone;
    private String locale;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.profesi = "Pengembara";
        this.kampung = null;
        this.balance = 500.0;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.playtimeSeconds = 0;
        this.tutorialDone = false;
        this.locale = "id";
    }

    // === Getters & Setters ===
    public UUID getUuid()                     { return uuid; }
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public String getProfesi()                { return profesi; }
    public void setProfesi(String profesi)    { this.profesi = profesi; }
    public String getKampung()                { return kampung; }
    public void setKampung(String kampung)    { this.kampung = kampung; }
    public double getBalance()                { return balance; }
    public void setBalance(double balance)    { this.balance = balance; }
    public long getFirstJoin()               { return firstJoin; }
    public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }
    public long getLastSeen()               { return lastSeen; }
    public void setLastSeen(long lastSeen)  { this.lastSeen = lastSeen; }
    public long getPlaytimeSeconds()        { return playtimeSeconds; }
    public void setPlaytimeSeconds(long v)  { this.playtimeSeconds = v; }
    public boolean isTutorialDone()         { return tutorialDone; }
    public void setTutorialDone(boolean v)  { this.tutorialDone = v; }
    public String getLocale()               { return locale; }
    public void setLocale(String locale)    { this.locale = locale; }

    // Helper
    public void addBalance(double amount)   { this.balance += amount; }
    public boolean hasBalance(double amount) { return this.balance >= amount; }
    public boolean deductBalance(double amount) {
        if (!hasBalance(amount)) return false;
        this.balance -= amount;
        return true;
    }

    public boolean isNewPlayer() {
        return !tutorialDone;
    }
}
