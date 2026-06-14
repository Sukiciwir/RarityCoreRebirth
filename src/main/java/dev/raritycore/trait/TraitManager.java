package dev.raritycore.trait;

import dev.raritycore.RarityCorePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Registry for loading and managing Traits.
 */
public final class TraitManager {

    private final RarityCorePlugin plugin;
    private final Map<String, Trait> traits = new LinkedHashMap<>();

    public TraitManager(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        traits.clear();
        File file = new File(plugin.getDataFolder(), "traits.yml");
        if (!file.exists()) {
            plugin.saveResource("traits.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        
        for (String id : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(id);
            if (sec == null) continue;
            
            Trait trait = new Trait(id, sec);
            traits.put(trait.getId(), trait);
        }
        
        plugin.getLogger().info("Loaded " + traits.size() + " traits.");
    }

    @Nullable
    public Trait get(@NotNull String id) {
        return traits.get(id.toLowerCase());
    }

    @NotNull
    public Collection<Trait> getAll() {
        return Collections.unmodifiableCollection(traits.values());
    }

    /**
     * Rolls random traits for an item based on its family, respecting weights and conflicts.
     * @param family The item family (e.g., "SWORD", "ANY")
     * @param amount The number of traits to roll
     * @param conflictManager The conflict manager to validate compatibility
     * @return A list of unique, compatible traits
     */
    @NotNull
    public List<Trait> rollTraits(@NotNull String family, int amount, @NotNull TraitConflictManager conflictManager) {
        List<Trait> result = new ArrayList<>();
        if (amount <= 0) return result;

        List<Trait> validPool = new ArrayList<>();
        for (Trait t : traits.values()) {
            if (t.getAllowedFamilies().contains("ANY") || t.getAllowedFamilies().contains(family.toUpperCase())) {
                validPool.add(t);
            }
        }

        for (int i = 0; i < amount; i++) {
            // Recalculate total weight for the remaining valid pool (excluding already picked and conflicts)
            double totalWeight = 0;
            List<Trait> currentPool = new ArrayList<>();
            
            for (Trait t : validPool) {
                if (result.contains(t)) continue;
                if (conflictManager.isConflict(t, result)) continue;
                
                totalWeight += t.getWeight();
                currentPool.add(t);
            }
            
            if (currentPool.isEmpty()) break; // No more valid traits to pick
            
            double r = Math.random() * totalWeight;
            double current = 0;
            for (Trait t : currentPool) {
                current += t.getWeight();
                if (r <= current) {
                    result.add(t);
                    break;
                }
            }
        }

        return result;
    }
}
