package dev.raritycore.affix;

import dev.raritycore.RarityCorePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Loads and provides access to all {@link Affix} definitions from {@code affixes.yml}.
 */
public final class AffixManager {

    private final RarityCorePlugin plugin;
    private final Map<String, Affix> affixes = new LinkedHashMap<>();

    public AffixManager(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        affixes.clear();
        File file = new File(plugin.getDataFolder(), "affixes.yml");
        if (!file.exists()) {
            plugin.saveResource("affixes.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(id);
            if (sec == null) continue;

            Affix affix = new Affix(
                    id,
                    sec.getString("display", id),
                    sec.getString("description", ""),
                    sec.getString("attribute", null),
                    sec.getString("operation", null),
                    sec.getDouble("value", 0),
                    sec.getString("slot-group", "ANY"),
                    sec.getString("secondary-attribute", null),
                    sec.getString("secondary-operation", null),
                    sec.getDouble("secondary-value", 0)
            );
            affixes.put(id, affix);
        }

        plugin.getLogger().info("Loaded " + affixes.size() + " affix(es) from affixes.yml.");
    }

    @Nullable
    public Affix getAffix(@NotNull String id) {
        return affixes.get(id);
    }

    @Nullable
    public Affix randomAffix() {
        if (affixes.isEmpty()) return null;
        List<Affix> list = new ArrayList<>(affixes.values());
        return list.get((int) (Math.random() * list.size()));
    }

    @NotNull
    public Collection<Affix> getAll() {
        return Collections.unmodifiableCollection(affixes.values());
    }
}
