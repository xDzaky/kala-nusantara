package id.kalanusantara.core;

import id.kalanusantara.core.backup.BackupManager;
import id.kalanusantara.core.bootstrap.KalaBootstrap;
import id.kalanusantara.core.commands.*;
import id.kalanusantara.core.config.KalaConfig;
import id.kalanusantara.core.cosmetics.CosmeticManager;
import id.kalanusantara.core.economy.RupiahEconomy;
import id.kalanusantara.core.event.*;
import id.kalanusantara.core.identity.PlayerIdentityManager;
import id.kalanusantara.core.journal.JurnalManager;
import id.kalanusantara.core.kala.KalaEventManager;
import id.kalanusantara.core.listeners.ClaimBoundaryListener;
import id.kalanusantara.core.listeners.KompassListener;
import id.kalanusantara.core.listeners.PlayerJoinListener;
import id.kalanusantara.core.listeners.ProfessionPerkListener;
import id.kalanusantara.core.listeners.QuestEventListener;
import id.kalanusantara.core.locale.LocaleManager;
import id.kalanusantara.core.npc.MbahKalaNpc;
import id.kalanusantara.core.npc.NpcManager;
import id.kalanusantara.core.npc.PesananManager;
import id.kalanusantara.core.profession.ProfesiManager;
import id.kalanusantara.core.project.KampungProjectManager;
import id.kalanusantara.core.quest.QuestManager;
import id.kalanusantara.core.quest.QuestProgressTracker;
import id.kalanusantara.core.season.ExpeditionManager;
import id.kalanusantara.core.season.SeasonManager;
import id.kalanusantara.core.storage.StorageManager;
import id.kalanusantara.core.ui.KalaScoreboardManager;
import id.kalanusantara.core.ui.MenuManager;
import id.kalanusantara.core.ui.TabListManager;
import id.kalanusantara.core.world.ResourceWorldManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * KalaCore — Plugin inti untuk server Kala Nusantara RPG
 *
 * Arsitektur (Phase 3):
 *   bootstrap    — lifecycle & dependency check
 *   config       — konfigurasi YAML
 *   locale       — teks ID/EN
 *   storage      — SQLite
 *   identity     — profil pemain
 *   economy      — mata uang Rupiah Kala
 *   quest        — QuestManager + QuestProgressTracker
 *   npc          — NpcManager, MbahKalaNpc, PesananManager
 *   profession   — ProfesiManager + KalaProfession enum
 *   journal      — JurnalManager (entri otomatis)
 *   kala         — KalaEventManager (state machine event)
 *   ui           — Kompas, Scoreboard, TabList
 */
public final class KalaCorePlugin extends JavaPlugin {

    private static KalaCorePlugin instance;

    // ── Core ──
    private KalaConfig kalaConfig;
    private LocaleManager localeManager;
    private StorageManager storageManager;
    private PlayerIdentityManager identityManager;
    private RupiahEconomy economy;

    // ── Quest ──
    private QuestManager questManager;
    private QuestProgressTracker questProgressTracker;

    // ── NPC ──
    private NpcManager npcManager;
    private MbahKalaNpc mbahKalaNpc;
    private PesananManager pesananManager;

    // ── Phase 3 ──
    private ProfesiManager profesiManager;
    private JurnalManager jurnalManager;
    private KalaEventManager kalaEventManager;

    // ── Phase 4 & 5 ──
    private ResourceWorldManager resourceWorldManager;
    private KampungProjectManager kampungProjectManager;
    private BackupManager backupManager;
    private id.kalanusantara.core.world.structure.NusantaraStructureEngine structureEngine;
    private id.kalanusantara.core.economy.KalaVaultEconomyProvider vaultEconomyProvider;

    // ── Phase 6 (Musiman, Gelar & Ekspedisi) ──
    private SeasonManager seasonManager;
    private CosmeticManager cosmeticManager;
    private ExpeditionManager expeditionManager;

    // ── Expansion v4 (Notifikasi, Atlas Fauna & Vulkanik) ──
    private id.kalanusantara.core.notify.KalaNotificationService notificationService;
    private id.kalanusantara.core.fauna.FaunaManager faunaManager;

    // ── V6 Event Framework & Policy (Kala Nusantara v6.0) ──
    private WorldImpactPolicy worldImpactPolicy;
    private HazardDirector hazardDirector;
    private EventContributionLedger eventContributionLedger;
    private VolcanoEncounterDirector volcanoEncounterDirector;
    private SeasonalEventDirector seasonalEventDirector;

    // ── UI ──
    private MenuManager menuManager;
    private KalaScoreboardManager scoreboardManager;
    private TabListManager tabListManager;

    @Override
    public void onEnable() {
        instance = this;
        Logger log = getLogger();

        log.info("╔══════════════════════════════════════╗");
        log.info("║   Kala Nusantara RPG — KalaCore      ║");
        log.info("║   versi " + getDescription().getVersion() + " [Phase 6 - Produksi]    ║");
        log.info("╚══════════════════════════════════════╝");

        // 1. Bootstrap
        if (!KalaBootstrap.initialize(this)) {
            log.severe("Bootstrap gagal! Plugin dinonaktifkan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Config
        kalaConfig = new KalaConfig(this);
        kalaConfig.load();

        // 3. Locale
        localeManager = new LocaleManager(this);
        localeManager.load();

        // 4. Storage
        storageManager = new StorageManager(this);
        if (!storageManager.initialize()) {
            log.severe("Gagal inisialisasi database! Plugin dinonaktifkan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 4b. Notification Service (UI Ownership Matrix)
        notificationService = new id.kalanusantara.core.notify.KalaNotificationService(this);

        // 5. Identity
        identityManager = new PlayerIdentityManager(this);
        identityManager.initialize();

        // 6. Economy
        economy = new RupiahEconomy(this);
        economy.initialize();

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultEconomyProvider = new id.kalanusantara.core.economy.KalaVaultEconomyProvider(this, economy);
            getServer().getServicesManager().register(
                net.milkbowl.vault.economy.Economy.class,
                vaultEconomyProvider,
                this,
                org.bukkit.plugin.ServicePriority.Highest
            );
            log.info("Vault terdeteksi: KalaVaultEconomyProvider terdaftar sebagai penyedia resmi!");
        }

        // 7. Quest
        questManager = new QuestManager(this);
        questManager.loadQuests();
        questProgressTracker = new QuestProgressTracker(this);

        // 8. Journal (sebelum NPC & Profesi karena keduanya memakai jurnal)
        jurnalManager = new JurnalManager(this);

        // 9. NPC System
        mbahKalaNpc   = new MbahKalaNpc(this);
        pesananManager = new PesananManager(this);
        npcManager     = new NpcManager(this);
        npcManager.initialize();

        // 10. Profesi
        profesiManager = new ProfesiManager(this);

        // 11. Event System & World Impact (Kala Nusantara v6.0)
        worldImpactPolicy = new WorldImpactPolicy(this);
        hazardDirector = new HazardDirector(this, worldImpactPolicy);
        eventContributionLedger = new EventContributionLedger(this);
        volcanoEncounterDirector = new VolcanoEncounterDirector(this, worldImpactPolicy, hazardDirector, eventContributionLedger);
        seasonalEventDirector = new SeasonalEventDirector(this, worldImpactPolicy, eventContributionLedger);

        // 11b. Kala Event Manager
        kalaEventManager = new KalaEventManager(this);

        // 12. Resource World & Kampung Projects (Phase 4)
        resourceWorldManager = new ResourceWorldManager(this);
        resourceWorldManager.initialize();

        structureEngine = new id.kalanusantara.core.world.structure.NusantaraStructureEngine(this);
        structureEngine.initialize();

        kampungProjectManager = new KampungProjectManager(this);
        kampungProjectManager.initialize();

        // 13. Backup & Disaster Recovery (Phase 5)
        backupManager = new BackupManager(this);
        backupManager.initialize();

        // 14. Musim, Gelar & Ekspedisi (Phase 6)
        seasonManager = new SeasonManager(this);
        seasonManager.initialize();

        cosmeticManager = new CosmeticManager(this);
        cosmeticManager.initialize();

        expeditionManager = new ExpeditionManager(this);
        expeditionManager.initialize();

        // 14b. Atlas Fauna Nusantara (Expansion v4)
        faunaManager = new id.kalanusantara.core.fauna.FaunaManager(this);
        faunaManager.initialize();

        // 15. UI
        menuManager   = new MenuManager(this);
        scoreboardManager = new KalaScoreboardManager(this);
        tabListManager    = new TabListManager(this);

        // Jadwalkan update scoreboard + tablist setiap 2 detik
        getServer().getScheduler().runTaskTimer(this, () -> {
            scoreboardManager.updateAll();
            tabListManager.updateAll();
        }, 40L, 40L);

        // 13. Commands
        registerCommands();

        // 14. Listeners
        registerListeners();

        log.info("KalaCore [Phase 6 - Produksi] berhasil dimuat! Selamat datang di Nusantara!");
    }

    @Override
    public void onDisable() {
        if (seasonalEventDirector != null) {
            seasonalEventDirector.stopEvent();
        }
        if (volcanoEncounterDirector != null) {
            volcanoEncounterDirector.stopEncounter();
        }
        if (worldImpactPolicy != null) {
            worldImpactPolicy.cleanupAllEventEntities();
        }
        if (kalaEventManager != null && kalaEventManager.getCurrentPhase() != KalaEventManager.KalaPhase.IDLE) {
            kalaEventManager.stopEvent();
        }
        if (npcManager != null) npcManager.shutdown();
        if (storageManager != null) storageManager.shutdown();
        if (identityManager != null) identityManager.saveAll();
        getLogger().info("KalaCore telah dimatikan. Sampai jumpa, Pengembara!");
        instance = null;
    }

    private void registerCommands() {
        getCommand("kala").setExecutor(new KalaCommand(this));
        getCommand("kalaadmin").setExecutor(new KalaAdminCommand(this));
        getCommand("kompas").setExecutor(new KompasCommand(this));
        getCommand("jurnal").setExecutor(new JurnalCommand(this));
        getCommand("pesanan").setExecutor(new PesananCommand(this));
        getCommand("profesi").setExecutor(new ProfesiCommand(this));

        ResourceCommand resCmd = new ResourceCommand(this);
        getCommand("tambang").setExecutor(resCmd);
        getCommand("tambang").setTabCompleter(resCmd);

        ProyekCommand proyCmd = new ProyekCommand(this);
        getCommand("proyek").setExecutor(proyCmd);
        getCommand("proyek").setTabCompleter(proyCmd);

        SpeedCommand speedCmd = new SpeedCommand(this);
        getCommand("speed").setExecutor(speedCmd);
        getCommand("speed").setTabCompleter(speedCmd);

        getCommand("musim").setExecutor(new SeasonCommand(this));
        getCommand("kosmetik").setExecutor(new CosmeticCommand(this));
        getCommand("ekspedisi").setExecutor(new ExpeditionCommand(this));

        getCommand("dompet").setExecutor(new DompetCommand(this));
        getCommand("transfer").setExecutor(new TransferCommand(this));

        // Expansion v4 Commands
        getCommand("fauna").setExecutor(new id.kalanusantara.core.commands.FaunaCommand(this));
        getCommand("gunung").setExecutor(new id.kalanusantara.core.commands.VolcanoCommand(this));
        getCommand("panduan").setExecutor(new id.kalanusantara.core.commands.PanduanCommand(this));
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new KompassListener(this), this);
        pm.registerEvents(new QuestEventListener(this), this);
        pm.registerEvents(profesiManager, this);
        pm.registerEvents(new ProfessionPerkListener(this), this);
        pm.registerEvents(new ClaimBoundaryListener(this), this);
        pm.registerEvents(new id.kalanusantara.core.listeners.FaunaDiscoveryListener(this, faunaManager), this);
        pm.registerEvents(new id.kalanusantara.core.listeners.EventHazardListener(
                this, worldImpactPolicy, seasonalEventDirector, volcanoEncounterDirector, eventContributionLedger
        ), this);
        // NpcManager mendaftarkan dirinya sendiri di initialize()
    }

    // ── Static ──
    public static KalaCorePlugin getInstance() { return instance; }

    // ── Getters ──
    public KalaConfig getKalaConfig()                       { return kalaConfig; }
    public LocaleManager getLocaleManager()                 { return localeManager; }
    public StorageManager getStorageManager()               { return storageManager; }
    public PlayerIdentityManager getIdentityManager()       { return identityManager; }
    public RupiahEconomy getEconomy()                       { return economy; }
    public QuestManager getQuestManager()                   { return questManager; }
    public QuestProgressTracker getQuestProgressTracker()   { return questProgressTracker; }
    public JurnalManager getJurnalManager()                 { return jurnalManager; }
    public NpcManager getNpcManager()                       { return npcManager; }
    public MbahKalaNpc getMbahKalaNpc()                     { return mbahKalaNpc; }
    public PesananManager getPesananManager()               { return pesananManager; }
    public ProfesiManager getProfesiManager()               { return profesiManager; }
    public KalaEventManager getKalaEventManager()           { return kalaEventManager; }
    public ResourceWorldManager getResourceWorldManager()   { return resourceWorldManager; }
    public KampungProjectManager getKampungProjectManager() { return kampungProjectManager; }
    public BackupManager getBackupManager()                 { return backupManager; }
    public SeasonManager getSeasonManager()                 { return seasonManager; }
    public CosmeticManager getCosmeticManager()             { return cosmeticManager; }
    public ExpeditionManager getExpeditionManager()         { return expeditionManager; }
    public id.kalanusantara.core.notify.KalaNotificationService getNotificationService() { return notificationService; }
    public id.kalanusantara.core.fauna.FaunaManager getFaunaManager() { return faunaManager; }
    public id.kalanusantara.core.world.structure.NusantaraStructureEngine getStructureEngine() { return structureEngine; }
    public MenuManager getMenuManager()                     { return menuManager; }
    public KalaScoreboardManager getScoreboardManager()     { return scoreboardManager; }
    public TabListManager getTabListManager()               { return tabListManager; }

    // ── V6 Event Framework Getters ──
    public WorldImpactPolicy getWorldImpactPolicy()         { return worldImpactPolicy; }
    public HazardDirector getHazardDirector()               { return hazardDirector; }
    public EventContributionLedger getEventContributionLedger() { return eventContributionLedger; }
    public VolcanoEncounterDirector getVolcanoEncounterDirector() { return volcanoEncounterDirector; }
    public SeasonalEventDirector getSeasonalEventDirector() { return seasonalEventDirector; }
}
