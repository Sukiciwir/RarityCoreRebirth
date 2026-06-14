package dev.raritycore;

import dev.raritycore.affix.AffixManager;
import dev.raritycore.api.RarityCoreAPI;
import dev.raritycore.collection.CollectionGUI;
import dev.raritycore.command.ItemStoryCommand;
import dev.raritycore.command.RarityCommand;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.config.Registries;
import dev.raritycore.cosmetic.ParticleTask;
import dev.raritycore.discovery.DiscoveryListener;
import dev.raritycore.generation.GenerationManager;
import dev.raritycore.gui.GachaGUI;
import dev.raritycore.gui.GUIManager;
import dev.raritycore.gui.StatsGUI;
import dev.raritycore.hooks.HookManager;
import dev.raritycore.identity.IdentityManager;
import dev.raritycore.legacy.LegacySystem;
import dev.raritycore.rarity.RarityItemFactory;
import dev.raritycore.salvage.SalvageGUI;
import dev.raritycore.salvage.SalvageManager;
import dev.raritycore.set.SetBonusManager;
import dev.raritycore.stats.StatsListener;
import dev.raritycore.storage.StorageManager;
import dev.raritycore.upgrade.UpgradeGUI;
import dev.raritycore.util.ItemUtil;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class RarityCorePlugin extends JavaPlugin {

    // ─── Managers ─────────────────────────────────────────────────────────────
    private ConfigManager       configManager;
    private Registries          registries;
    private GenerationManager   generationManager;
    private IdentityManager     identityManager;
    private LegacySystem        legacySystem;
    private AffixManager        affixManager;
    private SetBonusManager     setBonusManager;
    private SalvageManager      salvageManager;
    private StorageManager      storageManager;
    private RarityItemFactory   rarityItemFactory;
    private dev.raritycore.trait.TraitSystem traitSystem;
    private dev.raritycore.util.BroadcastManager broadcastManager;
    private HookManager hookManager;

    // ─── GUIs ─────────────────────────────────────────────────────────────────
    private CollectionGUI   collectionGUI;
    private SalvageGUI      salvageGUI;
    private UpgradeGUI      upgradeGUI;
    private StatsGUI        statsGUI;
    private GachaGUI        gachaGUI;
    private GUIManager      guiManager;

    // ─── Tasks ────────────────────────────────────────────────────────────────
    private BukkitTask particleTask;
    private BukkitTask autoSaveTask;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        ItemUtil.init(this);

        configManager = new ConfigManager(this);
        configManager.reload();

        registries = new Registries(this);
        registries.load();

        affixManager = new AffixManager(this);
        affixManager.load();

        traitSystem = new dev.raritycore.trait.TraitSystem(this, registries.getTraits());
        broadcastManager = new dev.raritycore.util.BroadcastManager(this);

        storageManager = new StorageManager(this);

        generationManager = new GenerationManager(this, registries);
        identityManager = new IdentityManager(this);
        legacySystem = new LegacySystem(this);

        rarityItemFactory = new RarityItemFactory(this, configManager, affixManager, traitSystem);

        salvageManager  = new SalvageManager(this);
        salvageManager.load();

        setBonusManager = new SetBonusManager(this, configManager);
        setBonusManager.load();

        // Pass registries.getItems() where rarityRegistry used to be expected
        collectionGUI = new CollectionGUI(this, configManager, registries.getItems(), storageManager);
        salvageGUI    = new SalvageGUI(this, configManager, salvageManager);
        upgradeGUI    = new UpgradeGUI(this, configManager, registries.getItems(), rarityItemFactory);
        statsGUI      = new StatsGUI(this, configManager, registries.getItems(), storageManager);
        gachaGUI      = new GachaGUI(this, configManager, registries.getItems(), rarityItemFactory, storageManager);
        guiManager    = new GUIManager(collectionGUI, salvageGUI, upgradeGUI, statsGUI, gachaGUI);

        registerListeners();

        RarityCommand rarityCommand = new RarityCommand(
                this, configManager, registries, rarityItemFactory, guiManager);
        getCommand("rarity").setExecutor(rarityCommand);
        getCommand("rarity").setTabCompleter(rarityCommand);
        
        if (getCommand("itemstory") != null) {
            getCommand("itemstory").setExecutor(new ItemStoryCommand(this));
        }
        if (getCommand("raritycore") != null) {
            dev.raritycore.command.AdminCommands adminCommands = new dev.raritycore.command.AdminCommands(this);
            getCommand("raritycore").setExecutor(adminCommands);
            getCommand("raritycore").setTabCompleter(adminCommands);
        }

        startTasks();

        hookManager = new HookManager(this);
        hookManager.load();
        
        // Wire world core hook into ContextRoller if available
        generationManager.getContextRoller().setWorldCoreHook(hookManager.getWorldCoreHook());

        RarityCoreAPI.init(this);

        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("RarityCore v" + getDescription().getVersion()
                + " enabled in " + elapsed + "ms — "
                + registries.getItems().size() + " items loaded.");
    }

    @Override
    public void onDisable() {
        if (particleTask != null) particleTask.cancel();
        if (autoSaveTask != null) autoSaveTask.cancel();

        if (storageManager != null) {
            storageManager.saveAll();
            storageManager.getCacheManager().flushSync();
        }

        if (hookManager != null) hookManager.unload();

        RarityCoreAPI.shutdown();
        HandlerList.unregisterAll(this);

        getLogger().info("RarityCore disabled. All data saved.");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new DiscoveryListener(this, configManager, registries.getItems(), storageManager), this);
        pm.registerEvents(new StatsListener(this), this);
        pm.registerEvents(setBonusManager, this);
        
        pm.registerEvents(new dev.raritycore.trait.listener.CombatHub(this), this);
        pm.registerEvents(new dev.raritycore.generation.GenerationListener(this), this);

        pm.registerEvents(collectionGUI, this);
        pm.registerEvents(salvageGUI,    this);
        pm.registerEvents(upgradeGUI,    this);
        pm.registerEvents(statsGUI,      this);
        pm.registerEvents(gachaGUI,      this);

        pm.registerEvents(new dev.raritycore.listener.DestructionListener(this), this);
        pm.registerEvents(new dev.raritycore.listener.UpgradeListener(this), this);

        pm.registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                storageManager.load(e.getPlayer().getUniqueId());
            }
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                storageManager.unload(e.getPlayer().getUniqueId());
            }
        }, this);
    }

    private void startTasks() {
        int interval = configManager.getParticleIntervalTicks();
        particleTask = new ParticleTask(this, configManager, setBonusManager)
                .runTaskTimer(this, interval, interval);

        int saveIntervalTicks = configManager.getAutoSaveInterval() * 60 * 20;
        autoSaveTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this, storageManager::saveAll, saveIntervalTicks, saveIntervalTicks);
    }

    @NotNull public ConfigManager      getConfigManager()      { return configManager; }
    @NotNull public Registries         getRegistries()         { return registries; }
    @NotNull public GenerationManager  getGenerationManager()  { return generationManager; }
    @NotNull public IdentityManager    getIdentityManager()    { return identityManager; }
    @NotNull public LegacySystem       getLegacySystem()       { return legacySystem; }
    @NotNull public AffixManager       getAffixManager()       { return affixManager; }
    @NotNull public SetBonusManager    getSetBonusManager()    { return setBonusManager; }
    @NotNull public SalvageManager     getSalvageManager()     { return salvageManager; }
    @NotNull public StorageManager     getStorageManager()     { return storageManager; }
    @NotNull public RarityItemFactory  getRarityItemFactory()  { return rarityItemFactory; }
    @NotNull public HookManager        getHookManager()        { return hookManager; }
    @NotNull public dev.raritycore.trait.TraitSystem getTraitSystem() { return traitSystem; }
    @NotNull public GUIManager         getGuiManager()         { return guiManager; }
    @NotNull public dev.raritycore.util.BroadcastManager getBroadcastManager() { return broadcastManager; }
}
