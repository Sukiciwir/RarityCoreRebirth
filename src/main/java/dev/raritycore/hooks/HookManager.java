package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Manages optional soft-dependency hooks. Each hook is loaded only if the
 * corresponding plugin is present and enabled on the server.
 */
public final class HookManager {

    private final RarityCorePlugin plugin;
    private final Logger log;

    private @Nullable PlaceholderAPIHook placeholderAPI;
    private @Nullable LuckPermsHook luckPerms;
    private @Nullable CrazyCratesHook crazyCrates;
    private @Nullable MythicMobsHook mythicMobs;
    private @Nullable EvenMoreFishHook evenMoreFish;
    private @Nullable VaultHook vault;
    private @Nullable PlayerPointsHook playerPoints;
    private @Nullable WorldCoreHook worldCoreHook;

    public HookManager(RarityCorePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void load() {
        loadPlaceholderAPI();
        loadLuckPerms();
        loadCrazyCrates();
        loadMythicMobs();
        loadEvenMoreFish();
        loadVault();
        loadPlayerPoints();
        loadWorldCore();
    }

    public void unload() {
        if (placeholderAPI != null) placeholderAPI.unload();
    }

    // ─── Loaders ──────────────────────────────────────────────────────────────

    private void loadPlaceholderAPI() {
        if (!isEnabled("PlaceholderAPI")) return;
        try {
            placeholderAPI = new PlaceholderAPIHook(plugin);
            placeholderAPI.register();
            log.info("[Hook] PlaceholderAPI: registered expansion.");
        } catch (Exception e) {
            log.warning("[Hook] PlaceholderAPI: failed to load — " + e.getMessage());
        }
    }

    private void loadLuckPerms() {
        if (!isEnabled("LuckPerms")) return;
        try {
            luckPerms = new LuckPermsHook(plugin);
            log.info("[Hook] LuckPerms: connected.");
        } catch (Exception e) {
            log.warning("[Hook] LuckPerms: failed to load — " + e.getMessage());
        }
    }

    private void loadCrazyCrates() {
        if (!isEnabled("CrazyCrates")) return;
        try {
            crazyCrates = new CrazyCratesHook(plugin);
            log.info("[Hook] CrazyCrates: connected.");
        } catch (Exception e) {
            log.warning("[Hook] CrazyCrates: failed to load — " + e.getMessage());
        }
    }

    private void loadMythicMobs() {
        if (!isEnabled("MythicMobs")) return;
        try {
            mythicMobs = new MythicMobsHook(plugin);
            plugin.getServer().getPluginManager().registerEvents(mythicMobs, plugin);
            log.info("[Hook] MythicMobs: drop listener registered.");
        } catch (Exception e) {
            log.warning("[Hook] MythicMobs: failed to load — " + e.getMessage());
        }
    }

    private void loadEvenMoreFish() {
        if (!isEnabled("EvenMoreFish")) return;
        try {
            evenMoreFish = new EvenMoreFishHook(plugin);
            plugin.getServer().getPluginManager().registerEvents(evenMoreFish, plugin);
            log.info("[Hook] EvenMoreFish: fish listener registered.");
        } catch (Exception e) {
            log.warning("[Hook] EvenMoreFish: failed to load — " + e.getMessage());
        }
    }

    private void loadVault() {
        if (!isEnabled("Vault")) return;
        try {
            vault = new VaultHook(plugin);
            if (vault.setupEconomy()) {
                log.info("[Hook] Vault: connected to economy.");
            } else {
                vault = null;
                log.warning("[Hook] Vault: found plugin but no economy provider.");
            }
        } catch (Exception e) {
            log.warning("[Hook] Vault: failed to load — " + e.getMessage());
        }
    }

    private void loadPlayerPoints() {
        if (!isEnabled("PlayerPoints")) return;
        try {
            playerPoints = new PlayerPointsHook(plugin);
            if (playerPoints.setupPlayerPoints()) {
                log.info("[Hook] PlayerPoints: connected.");
            } else {
                playerPoints = null;
            }
        } catch (Exception e) {
            log.warning("[Hook] PlayerPoints: failed to load — " + e.getMessage());
        }
    }

    private void loadWorldCore() {
        if (!isEnabled("WorldCore")) return;
        try {
            worldCoreHook = new WorldCoreHook();
            worldCoreHook.load();
            log.info("[Hook] WorldCore: connected.");
        } catch (Exception e) {
            log.warning("[Hook] WorldCore: failed to load — " + e.getMessage());
        }
    }

    // ─── Utilities ─────────────────────────────────────────────────────────────

    private boolean isEnabled(String pluginName) {
        return plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    public boolean hasPlaceholderAPI() { return placeholderAPI != null; }
    public boolean hasLuckPerms()      { return luckPerms != null; }
    public boolean hasCrazyCrates()    { return crazyCrates != null; }
    public boolean hasMythicMobs()     { return mythicMobs != null; }
    public boolean hasEvenMoreFish()   { return evenMoreFish != null; }
    public boolean hasVault()          { return vault != null; }
    public boolean hasPlayerPoints()   { return playerPoints != null; }
    public boolean hasWorldCore()      { return worldCoreHook != null; }

    @Nullable public PlaceholderAPIHook getPlaceholderAPI() { return placeholderAPI; }
    @Nullable public LuckPermsHook getLuckPerms()           { return luckPerms; }
    @Nullable public CrazyCratesHook getCrazyCrates()       { return crazyCrates; }
    @Nullable public MythicMobsHook getMythicMobs()         { return mythicMobs; }
    @Nullable public EvenMoreFishHook getEvenMoreFish()     { return evenMoreFish; }
    @Nullable public VaultHook getVault()                   { return vault; }
    @Nullable public PlayerPointsHook getPlayerPoints()     { return playerPoints; }
    @Nullable public WorldCoreHook getWorldCoreHook()       { return worldCoreHook; }
}
