package dev.raritycore.rarity;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.Registries;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Central catalog of all {@link RarityItem} templates loaded from {@code items.yml}.
 */
public final class ItemRegistry {

    private final RarityCorePlugin plugin;
    private final Registries registries;
    private final Map<String, RarityItem> items = new LinkedHashMap<>();

    public ItemRegistry(RarityCorePlugin plugin, Registries registries) {
        this.plugin = plugin;
        this.registries = registries;
    }

    /** Loads or reloads all items from items.yml. */
    public void load() {
        items.clear();
        Logger log = plugin.getLogger();

        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("items");
        if (section == null) {
            log.warning("items.yml has no 'items' section — no items loaded.");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            String materialName = entry.getString("material", "STONE");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warning("Unknown material '" + materialName + "' for item '" + id + "' — skipping.");
                continue;
            }

            String rarityName = entry.getString("rarity", "COMMON");
            RarityTier rarity = registries.getRarities().get(rarityName);
            if (rarity == null) {
                log.warning("Unknown rarity '" + rarityName + "' for item '" + id + "' — skipping.");
                continue;
            }

            String displayName = entry.getString("display-name", "<gray>" + id);
            List<String> lore = entry.getStringList("lore");
            String setId = entry.getString("set-id", null);
            boolean canHaveAffix = entry.getBoolean("can-have-affix", true);

            items.put(id, new RarityItem(id, material, rarity, displayName, lore, setId, canHaveAffix));
        }

        log.info("Loaded " + items.size() + " rarity item(s) from items.yml.");
    }

    /** Returns all registered items (immutable view). */
    @NotNull
    public Collection<RarityItem> getAll() {
        return Collections.unmodifiableCollection(items.values());
    }

    /** Returns all items of a given rarity. */
    @NotNull
    public List<RarityItem> getByRarity(@NotNull RarityTier rarity) {
        List<RarityItem> result = new ArrayList<>();
        for (RarityItem item : items.values()) {
            if (item.getRarity().equals(rarity)) result.add(item);
        }
        return result;
    }

    /** Returns all items that belong to a given set. */
    @NotNull
    public List<RarityItem> getBySet(@NotNull String setId) {
        List<RarityItem> result = new ArrayList<>();
        for (RarityItem item : items.values()) {
            if (setId.equals(item.getSetId())) result.add(item);
        }
        return result;
    }

    /** Looks up a template by its registry ID. */
    @Nullable
    public RarityItem get(@NotNull String id) {
        return items.get(id.toLowerCase());
    }

    /** Returns a random item of the specified rarity, or null if none exist. */
    @Nullable
    public RarityItem random(@NotNull RarityTier rarity) {
        List<RarityItem> pool = getByRarity(rarity);
        if (pool.isEmpty()) return null;
        return pool.get((int) (Math.random() * pool.size()));
    }

    public int size() { return items.size(); }
}
