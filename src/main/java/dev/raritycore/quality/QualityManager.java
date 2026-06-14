package dev.raritycore.quality;

import dev.raritycore.RarityCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Registry for loading and managing QualityTiers.
 */
public final class QualityManager {

    private final RarityCorePlugin plugin;
    private final Map<String, QualityTier> qualities = new LinkedHashMap<>();

    public QualityManager(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        qualities.clear();
        File file = new File(plugin.getDataFolder(), "qualities.yml");
        if (!file.exists()) {
            plugin.saveResource("qualities.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        
        List<QualityTier> loaded = new ArrayList<>();
        int tierOrder = 0;
        for (String id : cfg.getKeys(false)) {
            String displayName = cfg.getString(id + ".display-name", id);
            String symbol = cfg.getString(id + ".symbol", "");
            int statMin = cfg.getInt(id + ".stat-min", 80);
            int statMax = cfg.getInt(id + ".stat-max", 120);
            QualityTier tier = new QualityTier(id, tierOrder++, displayName, symbol, statMin, statMax);
            loaded.add(tier);
        }
        
        // Ensure QUALITY_LEGACY exists if it wasn't defined
        boolean hasLegacy = loaded.stream().anyMatch(QualityTier::isLegacy);
        if (!hasLegacy) {
            loaded.add(new QualityTier("quality_legacy", tierOrder, "Legacy", "⚜", 120, 150));
        }
        
        loaded.sort(Comparator.comparingInt(QualityTier::getTier));
        for (QualityTier q : loaded) {
            qualities.put(q.getId(), q);
        }
        
        plugin.getLogger().info("Loaded " + qualities.size() + " quality tiers.");
    }

    @Nullable
    public QualityTier get(@NotNull String id) {
        return qualities.get(id.toLowerCase());
    }

    @NotNull
    public Collection<QualityTier> getAll() {
        return Collections.unmodifiableCollection(qualities.values());
    }

    @Nullable
    public QualityTier getMaxQuality() {
        return qualities.values().stream()
                .filter(q -> !q.isLegacy())
                .max(Comparator.comparingInt(QualityTier::getTier))
                .orElse(null);
    }

    @Nullable
    public QualityTier getQualityForPercentage(int percentage) {
        QualityTier best = null;
        for (QualityTier q : qualities.values()) {
            if (q.isLegacy() && percentage <= 120) continue;
            if (percentage >= q.getStatMin() && percentage <= q.getStatMax()) {
                best = q;
            }
        }
        if (best != null) return best;
        
        // Fallback
        for (QualityTier q : qualities.values()) {
            if (!q.isLegacy()) return q;
        }
        return null;
    }
}
