package dev.raritycore.storage;

import dev.raritycore.RarityCorePlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flat-YAML storage backend.
 * One file per player: {@code plugins/RarityCore/data/<uuid>.yml}
 *
 * <p>Structure:
 * <pre>
 * discovered:
 *   - "mythic_netherite_sword"
 *   - "rare_iron_sword"
 * statistics:
 *   COMMON: 15
 *   RARE: 3
 * pity: 7
 * </pre>
 */
public final class StorageManager {

    private final RarityCorePlugin plugin;
    private final File dataFolder;

    /** In-memory cache: uuid → player data */
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    private final SQLiteStorage sqliteStorage;
    private final ItemCacheManager itemCacheManager;
    private final MigrationManager migrationManager;

    public StorageManager(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        //noinspection ResultOfMethodCallIgnored
        dataFolder.mkdirs();

        this.sqliteStorage = new SQLiteStorage(plugin);
        this.itemCacheManager = new ItemCacheManager(plugin, sqliteStorage);
        this.migrationManager = new MigrationManager(plugin);
    }

    public SQLiteStorage getSQLiteStorage() { return sqliteStorage; }
    public ItemCacheManager getCacheManager() { return itemCacheManager; }
    public MigrationManager getMigrationManager() { return migrationManager; }

    // ─── Load / Save ───────────────────────────────────────────────────────────

    @NotNull
    public PlayerData load(@NotNull UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);

        File file = playerFile(uuid);
        if (!file.exists()) {
            PlayerData fresh = new PlayerData(uuid);
            cache.put(uuid, fresh);
            return fresh;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(uuid);
        data.getDiscovered().addAll(cfg.getStringList("discovered"));

        // Statistics
        if (cfg.isConfigurationSection("statistics")) {
            for (String r : cfg.getConfigurationSection("statistics").getKeys(false)) {
                int count = cfg.getInt("statistics." + r, 0);
                if (count > 0) data.getStatistics().put(r, count);
            }
        }

        data.setPity(cfg.getInt("pity", 0));

        cache.put(uuid, data);
        return data;
    }

    public void save(@NotNull UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;

        File file = playerFile(uuid);
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("discovered", new ArrayList<>(data.getDiscovered()));

        for (Map.Entry<String, Integer> entry : data.getStatistics().entrySet()) {
            cfg.set("statistics." + entry.getKey(), entry.getValue());
        }

        cfg.set("pity", data.getPity());

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data for " + uuid + ": " + e.getMessage());
        }
    }

    /** Saves all loaded player data. Call from auto-save task and plugin disable. */
    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }

    public void unload(@NotNull UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    // ─── Data Helpers ──────────────────────────────────────────────────────────

    /** Returns the player's data, loading from disk if needed. */
    @NotNull
    public PlayerData get(@NotNull UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadDirect);
    }

    private PlayerData loadDirect(UUID uuid) {
        return load(uuid);
    }

    /** Marks an item as discovered and increments the rarity stat. */
    public void recordDiscovery(@NotNull UUID uuid, @NotNull String itemId, @NotNull String rarityId) {
        PlayerData data = get(uuid);
        data.getDiscovered().add(itemId);
        data.getStatistics().merge(rarityId, 1, Integer::sum);
    }

    public boolean hasDiscovered(@NotNull UUID uuid, @NotNull String itemId) {
        return get(uuid).getDiscovered().contains(itemId);
    }

    /** Aggregates server-wide discovery counts per rarity. */
    @NotNull
    public Map<String, Integer> getServerStats() {
        Map<String, Integer> totals = new HashMap<>();
        for (PlayerData data : cache.values()) {
            for (Map.Entry<String, Integer> e : data.getStatistics().entrySet()) {
                totals.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        return totals;
    }

    // ─── Inner: PlayerData ─────────────────────────────────────────────────────

    public static final class PlayerData {
        private final UUID uuid;
        private final Set<String> discovered = new LinkedHashSet<>();
        private final Map<String, Integer> statistics = new HashMap<>();
        private int pity = 0; // Gacha pity counter

        public PlayerData(@NotNull UUID uuid) { this.uuid = uuid; }

        public UUID getUuid() { return uuid; }
        public Set<String> getDiscovered() { return discovered; }
        public Map<String, Integer> getStatistics() { return statistics; }
        public int getPity() { return pity; }
        public void setPity(int pity) { this.pity = pity; }
        public void incrementPity() { this.pity++; }
        public void resetPity() { this.pity = 0; }
        public int getDiscoveryCount(@NotNull String rarityId) {
            return statistics.getOrDefault(rarityId, 0);
        }
    }

    private File playerFile(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }
}
