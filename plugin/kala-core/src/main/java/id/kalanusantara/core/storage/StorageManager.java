package id.kalanusantara.core.storage;

import id.kalanusantara.core.KalaCorePlugin;

import java.io.File;
import java.sql.*;
import java.util.logging.Logger;

/**
 * StorageManager — koneksi SQLite dan schema management
 */
public class StorageManager {

    private final KalaCorePlugin plugin;
    private final Logger log;
    private Connection connection;
    private File dbFile;

    public StorageManager(KalaCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public boolean initialize() {
        try {
            dbFile = new File(plugin.getDataFolder(), "data/kala_data.db");
            dbFile.getParentFile().mkdirs();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // WAL mode for better concurrent read performance
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();
            log.info("Database SQLite berhasil dibuka: " + dbFile.getName());
            return true;
        } catch (Exception e) {
            log.severe("Gagal membuka database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void createTables() throws SQLException {
        String[] tables = {
            // Profil pemain
            """
            CREATE TABLE IF NOT EXISTS player_profile (
                uuid TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                profesi TEXT DEFAULT 'Pengembara',
                kampung TEXT DEFAULT NULL,
                balance REAL DEFAULT 500.0,
                first_join INTEGER NOT NULL,
                last_seen INTEGER NOT NULL,
                playtime_seconds INTEGER DEFAULT 0,
                tutorial_done INTEGER DEFAULT 0,
                locale TEXT DEFAULT 'id',
                data TEXT DEFAULT '{}'
            )
            """,
            // Jurnal penjelajah
            """
            CREATE TABLE IF NOT EXISTS journal_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(uuid) REFERENCES player_profile(uuid)
            )
            """,
            // Quest progress
            """
            CREATE TABLE IF NOT EXISTS quest_progress (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                quest_id TEXT NOT NULL,
                step INTEGER DEFAULT 0,
                step_count INTEGER DEFAULT 0,
                status TEXT DEFAULT 'ACTIVE',
                started_at INTEGER NOT NULL,
                completed_at INTEGER DEFAULT NULL,
                UNIQUE(uuid, quest_id)
            )
            """,
            // Transaksi ekonomi
            """
            CREATE TABLE IF NOT EXISTS economy_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                type TEXT NOT NULL,
                amount REAL NOT NULL,
                description TEXT,
                timestamp INTEGER NOT NULL
            )
            """,
            // Penemuan / Discovery
            """
            CREATE TABLE IF NOT EXISTS discoveries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                discovery_id TEXT NOT NULL,
                discovered_at INTEGER NOT NULL,
                UNIQUE(uuid, discovery_id)
            )
            """,
            // Proyek Kampung (Gotong Royong)
            """
            CREATE TABLE IF NOT EXISTS village_projects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                target_material TEXT NOT NULL,
                target_amount INTEGER NOT NULL,
                current_amount INTEGER DEFAULT 0,
                status TEXT DEFAULT 'ACTIVE',
                completed_at INTEGER DEFAULT NULL
            )
            """,
            // Kontribusi Proyek
            """
            CREATE TABLE IF NOT EXISTS project_contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id TEXT NOT NULL,
                uuid TEXT NOT NULL,
                amount INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(project_id) REFERENCES village_projects(id)
            )
            """,
            // Gelar & Kosmetik Pemain (Etis / Non-Pay-To-Win)
            """
            CREATE TABLE IF NOT EXISTS player_cosmetics (
                uuid TEXT PRIMARY KEY,
                active_title TEXT DEFAULT 'PENGEMBARA_AWAL',
                active_trail TEXT DEFAULT 'NONE',
                unlocked_titles TEXT DEFAULT 'PENGEMBARA_AWAL',
                unlocked_trails TEXT DEFAULT 'NONE'
            )
            """,
            // Atlas Fauna Nusantara (Discovery Satwa Unik)
            """
            CREATE TABLE IF NOT EXISTS player_fauna_discovery (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                fauna_id TEXT NOT NULL,
                discovered_at INTEGER NOT NULL,
                UNIQUE(uuid, fauna_id)
            )
            """,
            // Kontribusi Gotong Royong Sajen / Pereda Murka Gunung
            """
            CREATE TABLE IF NOT EXISTS volcano_offerings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                material TEXT NOT NULL,
                amount INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """,
            // Event Contribution & Reward Ledger (Idempotent V6)
            """
            CREATE TABLE IF NOT EXISTS event_contribution_ledger (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id TEXT NOT NULL,
                uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                objective_id TEXT NOT NULL,
                points INTEGER DEFAULT 0,
                reward_claimed INTEGER DEFAULT 0,
                timestamp INTEGER NOT NULL,
                UNIQUE(event_id, uuid, objective_id)
            )
            """,
            // Event Single-Claim Ledger (Audit V7/V8 Outbox Pattern)
            """
            CREATE TABLE IF NOT EXISTS event_claims (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                claim_id TEXT UNIQUE,
                event_id TEXT NOT NULL,
                uuid TEXT NOT NULL,
                player_name TEXT,
                score INTEGER DEFAULT 0,
                amount REAL NOT NULL,
                exp INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING',
                timestamp INTEGER NOT NULL,
                paid_at INTEGER,
                UNIQUE(event_id, uuid)
            )
            """,
            // Entri Penemuan Nusantara (Discovery Catalog V8)
            """
            CREATE TABLE IF NOT EXISTS nusantara_discoveries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                discovery_type TEXT NOT NULL,
                entry_id TEXT NOT NULL,
                island TEXT NOT NULL,
                discovered_at INTEGER NOT NULL,
                UNIQUE(uuid, discovery_type, entry_id)
            )
            """,
            // Kontrak Kampung Mingguan (V8)
            """
            CREATE TABLE IF NOT EXISTS village_contracts (
                id TEXT PRIMARY KEY,
                week_id TEXT NOT NULL,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                target_count INTEGER NOT NULL,
                reward_rupiah REAL NOT NULL,
                reward_exp INTEGER NOT NULL,
                expiry INTEGER NOT NULL
            )
            """,
            // Progres Kontrak Pemain (V8)
            """
            CREATE TABLE IF NOT EXISTS player_contract_progress (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                contract_id TEXT NOT NULL,
                current_count INTEGER DEFAULT 0,
                completed INTEGER DEFAULT 0,
                claimed INTEGER DEFAULT 0,
                completed_at INTEGER,
                UNIQUE(uuid, contract_id)
            )
            """
        };

        try (Statement st = connection.createStatement()) {
            for (String sql : tables) {
                st.execute(sql);
            }
            // Migrations:
            try {
                st.execute("ALTER TABLE quest_progress ADD COLUMN step_count INTEGER DEFAULT 0");
            } catch (SQLException ignored) {}

            // Migrations untuk event_claims (V8 Outbox pattern)
            try {
                st.execute("ALTER TABLE event_claims ADD COLUMN claim_id TEXT");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE event_claims ADD COLUMN player_name TEXT");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE event_claims ADD COLUMN score INTEGER DEFAULT 0");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE event_claims ADD COLUMN status TEXT DEFAULT 'PAID'");
            } catch (SQLException ignored) {}
            try {
                st.execute("ALTER TABLE event_claims ADD COLUMN paid_at INTEGER");
            } catch (SQLException ignored) {}
        }
        log.info("Schema database OK (" + tables.length + " tabel)");
    }

    private final Object dbLock = new Object();

    public Object getDbLock() {
        return dbLock;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Pastikan koneksi masih aktif (reconnect jika timeout)
     */
    public synchronized Connection ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                log.warning("Koneksi database terputus, reconnecting...");
                initialize();
            }
        } catch (SQLException e) {
            log.severe("Gagal cek koneksi: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Melakukan checkpoint WAL agar seluruh transaksi di buffer masuk ke disk secara konsisten (V7)
     */
    public void checkpointWal() {
        synchronized (dbLock) {
            try (Statement st = ensureConnection().createStatement()) {
                st.execute("PRAGMA wal_checkpoint(TRUNCATE);");
                log.info("[StorageManager] WAL Checkpoint TRUNCATE selesai.");
            } catch (SQLException e) {
                log.warning("[StorageManager] Gagal melakukan WAL checkpoint: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        checkpointWal();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.info("Database ditutup dengan bersih.");
            }
        } catch (SQLException e) {
            log.warning("Error menutup database: " + e.getMessage());
        }
    }
}
