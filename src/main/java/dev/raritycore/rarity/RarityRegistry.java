package dev.raritycore.rarity;

import dev.raritycore.RarityCorePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Registry for loading and managing dynamic RarityTiers.
 */
public final class RarityRegistry {

    private final RarityCorePlugin plugin;
    private final Map<String, RarityTier> rarities = new LinkedHashMap<>();

    public RarityRegistry(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        rarities.clear();
        File file = new File(plugin.getDataFolder(), "rarities.yml");
        if (!file.exists()) {
            plugin.saveResource("rarities.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        
        List<RarityTier> loaded = new ArrayList<>();
        int order = 0;
        for (String id : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(id);
            if (sec == null) continue;
            
            RarityTier tier = new RarityTier(id, order++, sec);
            loaded.add(tier);
        }
        
        // Sort by tier just in case
        loaded.sort(Comparator.comparingInt(RarityTier::getTier));
        for (RarityTier r : loaded) {
            rarities.put(r.getId(), r);
        }
        
        plugin.getLogger().info("Loaded " + rarities.size() + " rarity tiers.");
    }

    @Nullable
    public RarityTier get(@NotNull String id) {
        return rarities.get(id.toLowerCase());
    }

    @NotNull
    public Collection<RarityTier> getAll() {
        return Collections.unmodifiableCollection(rarities.values());
    }
    
    @Nullable
    public RarityTier getNextUpgradeable(@NotNull RarityTier current) {
        RarityTier next = null;
        for (RarityTier r : rarities.values()) {
            if (r.getTier() > current.getTier() && r.isUpgradeableTo()) {
                if (next == null || r.getTier() < next.getTier()) {
                    next = r;
                }
            }
        }
        return next;
    }
}
