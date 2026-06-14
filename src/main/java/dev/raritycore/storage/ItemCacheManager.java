package dev.raritycore.storage;

import dev.raritycore.RarityCorePlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the in-memory cache of ItemStatistics to avoid continuous DB writes.
 * Periodically flushes changes to SQLiteStorage.
 */
public final class ItemCacheManager {

    private final RarityCorePlugin plugin;
    private final SQLiteStorage storage;
    
    private final Map<UUID, ItemStatistics> cache = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStatistics> dirty = new ConcurrentHashMap<>();

    public ItemCacheManager(RarityCorePlugin plugin, SQLiteStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        startFlushTask();
    }

    private void startFlushTask() {
        int intervalTicks = plugin.getConfigManager().getAutoSaveInterval() * 20 * 60;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::flush, intervalTicks, intervalTicks);
    }

    /**
     * Gets from cache, or returns a new instance. Does NOT block to load from DB.
     * DB loading should be handled asynchronously via MigrationManager or explicit load.
     */
    public ItemStatistics getCachedOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, ItemStatistics::new);
    }

    /**
     * Puts an item stat into the cache and marks it dirty.
     */
    public void markDirty(ItemStatistics stats) {
        cache.put(stats.getItemUuid(), stats);
        dirty.put(stats.getItemUuid(), stats);
    }

    /**
     * Replaces the cached instance (used when DB load finishes).
     */
    public void loadIntoCache(ItemStatistics stats) {
        cache.put(stats.getItemUuid(), stats);
    }

    /**
     * Unloads from cache if not dirty. If dirty, saves first.
     */
    public void unload(UUID uuid) {
        ItemStatistics stats = dirty.remove(uuid);
        if (stats != null) {
            storage.saveStats(stats).thenRun(() -> cache.remove(uuid));
        } else {
            cache.remove(uuid);
        }
    }

    public void flush() {
        if (dirty.isEmpty()) return;
        
        plugin.getLogger().info("Flushing " + dirty.size() + " item stats to database...");
        for (ItemStatistics stats : dirty.values()) {
            storage.saveStats(stats);
        }
        dirty.clear();
    }
    
    public void flushSync() {
        if (dirty.isEmpty()) return;
        for (ItemStatistics stats : dirty.values()) {
            // Wait for future to complete sync
            storage.saveStats(stats).join();
        }
        dirty.clear();
    }
}
