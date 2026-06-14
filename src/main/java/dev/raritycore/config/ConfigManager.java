package dev.raritycore.config;

import dev.raritycore.RarityCorePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches all configuration files.
 * Call {@link #reload()} to hot-reload everything without a server restart.
 */
public final class ConfigManager {

    private final RarityCorePlugin plugin;

    // ── Messages ─────────────────────────────────────────────────────────────
    private MessagesConfig messages;

    // ── Main config values ────────────────────────────────────────────────────
    private String prefix;
    private boolean broadcastEnabled;
    private int broadcastMinimumRarity;
    private boolean qualityEnabled;
    private double qualityWeightExponent;
    private boolean affixEnabled;
    private int affixMinimumRarity;
    private double affixChance;
    private boolean gachaEnabled;
    private int gachaCostLevels;
    private int gachaPityThreshold;
    private Map<String, Double> gachaRates;
    private Map<String, Double> generationRates;
    private Map<String, Double> upgradeSuccessChance;
    private String upgradeFailureOutcome;
    private boolean particlesOnSetActive;
    private int particleIntervalTicks;
    private int autoSaveInterval;
    
    // ── Legacy Requirements ───────────────────────────────────────────────────
    private int legacyReqKills;
    private int legacyReqAgeDays;
    private int legacyReqMaxOwners;

    public ConfigManager(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    /** Saves all default resource files and loads everything. */
    public void reload() {
        saveDefaults();
        loadMainConfig();
        loadMessages();
    }

    // ─── Loaders ──────────────────────────────────────────────────────────────

    private void saveDefaults() {
        String[] files = {"config.yml", "rarities.yml", "items.yml", "affixes.yml",
                          "sets.yml", "salvage.yml", "messages.yml", "qualities.yml", "traits.yml"};
        for (String f : files) {
            if (!new File(plugin.getDataFolder(), f).exists()) {
                try {
                    plugin.saveResource(f, false);
                } catch (IllegalArgumentException ignored) {
                    // Resource might not exist in jar, skip
                }
            }
        }
        plugin.reloadConfig();
    }

    private void loadMainConfig() {
        FileConfiguration cfg = plugin.getConfig();

        prefix = cfg.getString("general.prefix",
                "<dark_gray>[<gradient:#FFD700:#9B30FF>RarityCore</gradient>]</dark_gray> ");
        broadcastEnabled = cfg.getBoolean("general.broadcast-enabled", true);
        broadcastMinimumRarity = cfg.getInt("general.broadcast-minimum-rarity", 4); // Example: 4 = LEGENDARY

        qualityEnabled = cfg.getBoolean("quality.enabled", true);
        qualityWeightExponent = cfg.getDouble("quality.weight-exponent", 1.5);

        affixEnabled = cfg.getBoolean("affix.enabled", true);
        affixMinimumRarity = cfg.getInt("affix.minimum-rarity", 2); // 2 = RARE
        affixChance = cfg.getDouble("affix.chance", 0.40);

        gachaEnabled = cfg.getBoolean("gacha.enabled", true);
        gachaCostLevels = cfg.getInt("gacha.cost-levels", 5);
        gachaPityThreshold = cfg.getInt("gacha.pity-threshold", 50);

        gachaRates = new HashMap<>();
        ConfigurationSection rates = cfg.getConfigurationSection("gacha.rates");
        if (rates != null) {
            for (String key : rates.getKeys(false)) {
                gachaRates.put(key.toLowerCase(), rates.getDouble(key, 0));
            }
        }
        // Defaults if config is missing
        if (gachaRates.isEmpty()) {
            gachaRates.put("common",   0.45);
            gachaRates.put("uncommon", 0.25);
            gachaRates.put("rare",     0.15);
            gachaRates.put("epic",     0.09);
            gachaRates.put("legendary",0.05);
            gachaRates.put("mythic",   0.006);
            gachaRates.put("divine",   0.003);
            gachaRates.put("ancient",  0.001);
        }

        generationRates = new HashMap<>();
        ConfigurationSection genRates = cfg.getConfigurationSection("generation.rates");
        if (genRates != null) {
            for (String key : genRates.getKeys(false)) {
                generationRates.put(key.toLowerCase(), genRates.getDouble(key, 0));
            }
        }
        if (generationRates.isEmpty()) {
            generationRates.put("common",   0.60);
            generationRates.put("uncommon", 0.25);
            generationRates.put("rare",     0.10);
            generationRates.put("epic",     0.04);
            generationRates.put("legendary",0.01);
            generationRates.put("mythic",   0.00);
            generationRates.put("divine",   0.00);
            generationRates.put("ancient",  0.00);
        }

        upgradeSuccessChance = new HashMap<>();
        ConfigurationSection upSec = cfg.getConfigurationSection("upgrade.success-chance");
        if (upSec != null) {
            for (String key : upSec.getKeys(false)) {
                upgradeSuccessChance.put(key, upSec.getDouble(key, 1.0));
            }
        }
        // Defaults
        if (upgradeSuccessChance.isEmpty()) {
            upgradeSuccessChance.put("COMMON_TO_UNCOMMON",  0.90);
            upgradeSuccessChance.put("UNCOMMON_TO_RARE",    0.75);
            upgradeSuccessChance.put("RARE_TO_EPIC",        0.60);
            upgradeSuccessChance.put("EPIC_TO_LEGENDARY",   0.40);
        }

        upgradeFailureOutcome = cfg.getString("upgrade.failure-outcome", "NOTHING");

        particlesOnSetActive = cfg.getBoolean("cosmetic.particles-on-set-active", true);
        particleIntervalTicks = cfg.getInt("cosmetic.particle-interval-ticks", 40);
        autoSaveInterval = cfg.getInt("storage.auto-save-interval", 10);
        
        legacyReqKills = cfg.getInt("legacy.requirements.kills", 10000);
        legacyReqAgeDays = cfg.getInt("legacy.requirements.age-days", 90);
        legacyReqMaxOwners = cfg.getInt("legacy.requirements.max-owners", 1);
    }

    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        messages = new MessagesConfig(prefix, cfg);
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    @NotNull public MessagesConfig getMessages() { return messages; }

    @NotNull public String getPrefix() { return prefix; }
    public boolean isBroadcastEnabled() { return broadcastEnabled; }
    public int getBroadcastMinimumRarity() { return broadcastMinimumRarity; }
    public boolean isQualityEnabled() { return qualityEnabled; }
    public double getQualityWeightExponent() { return qualityWeightExponent; }
    public boolean isAffixEnabled() { return affixEnabled; }
    public int getAffixMinimumRarity() { return affixMinimumRarity; }
    public double getAffixChance() { return affixChance; }
    public boolean isGachaEnabled() { return gachaEnabled; }
    public int getGachaCostLevels() { return gachaCostLevels; }
    public int getGachaPityThreshold() { return gachaPityThreshold; }
    @NotNull public Map<String, Double> getGachaRates() { return gachaRates; }
    @NotNull public Map<String, Double> getGenerationRates() { return generationRates; }
    public double getUpgradeSuccessChance(@NotNull String key) {
        return upgradeSuccessChance.getOrDefault(key, 1.0);
    }
    @NotNull public String getUpgradeFailureOutcome() { return upgradeFailureOutcome; }
    public boolean isParticlesOnSetActive() { return particlesOnSetActive; }
    public int getParticleIntervalTicks() { return particleIntervalTicks; }
    public int getAutoSaveInterval() { return autoSaveInterval; }
    
    public int getLegacyReqKills() { return legacyReqKills; }
    public int getLegacyReqAgeDays() { return legacyReqAgeDays; }
    public int getLegacyReqMaxOwners() { return legacyReqMaxOwners; }
}
