package dev.raritycore.storage;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.util.ItemUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles lazy migration of old PDC-only RarityCore items into the new Hybrid (PDC + SQLite) model.
 */
public final class MigrationManager {

    private final RarityCorePlugin plugin;
    public static NamespacedKey KEY_ITEM_UUID;
    public static NamespacedKey KEY_REVEAL_STATE;

    public MigrationManager(RarityCorePlugin plugin) {
        this.plugin = plugin;
        KEY_ITEM_UUID = new NamespacedKey(plugin, "item_uuid");
        KEY_REVEAL_STATE = new NamespacedKey(plugin, "reveal_state");
    }

    /**
     * Examines an item and migrates it if it's an old-format RarityCore item.
     * Generates a UUID, creates ItemStatistics, schedules SQLite save, and updates PDC.
     * Returns the UUID of the item (newly generated or existing).
     */
    public UUID migrateOrGetUUID(@NotNull ItemStack stack) {
        if (!stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(ItemUtil.KEY_RARITY, PersistentDataType.STRING)) return null;

        // Already migrated if it has a UUID
        if (pdc.has(KEY_ITEM_UUID, PersistentDataType.STRING)) {
            UUID id = UUID.fromString(pdc.get(KEY_ITEM_UUID, PersistentDataType.STRING));
            ItemStatistics cachedStats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(id);
            if (cachedStats != null && cachedStats.isDestroyed()) {
                cachedStats.setDestroyed(false);
                plugin.getStorageManager().getCacheManager().markDirty(cachedStats);
                plugin.getLogger().info("RarityCore Item Resurrected: " + id + " (Detected via PDC)");
            }
            return id;
        }

        // Migrate Old Item
        UUID newUuid = UUID.randomUUID();
        ItemStatistics stats = new ItemStatistics(newUuid);

        // Extract old numeric quality if present (or new generated quality)
        if (pdc.has(ItemUtil.KEY_QUALITY, PersistentDataType.INTEGER)) {
            int oldQuality = pdc.get(ItemUtil.KEY_QUALITY, PersistentDataType.INTEGER);
            
            // Keep the INTEGER in the PDC! It's supposed to be an integer (1-100).
            
            // Add a history entry with a naive string mapped ID
            String mappedQualityId = "normal";
            if (oldQuality >= 95) mappedQualityId = "masterwork";
            else if (oldQuality >= 80) mappedQualityId = "flawless";
            else if (oldQuality >= 60) mappedQualityId = "superior";
            else if (oldQuality >= 40) mappedQualityId = "fine";
            else if (oldQuality >= 20) mappedQualityId = "normal";
            else if (oldQuality >= 10) mappedQualityId = "poor";
            else mappedQualityId = "broken";
            
            String reason = pdc.has(ItemUtil.KEY_FIRST_DATE, PersistentDataType.STRING) ? "Initial Roll" : "Legacy Migration";
            stats.addQualityHistoryEntry(new QualityHistoryEntry(mappedQualityId, System.currentTimeMillis(), reason));
        } else if (pdc.has(ItemUtil.KEY_QUALITY, PersistentDataType.STRING)) {
            // Fix corrupted item from previous buggy migration
            String corrupted = pdc.get(ItemUtil.KEY_QUALITY, PersistentDataType.STRING);
            int restoredQuality = 50;
            if ("masterwork".equals(corrupted)) restoredQuality = 95;
            else if ("flawless".equals(corrupted)) restoredQuality = 80;
            else if ("superior".equals(corrupted)) restoredQuality = 60;
            else if ("fine".equals(corrupted)) restoredQuality = 40;
            else if ("normal".equals(corrupted)) restoredQuality = 20;
            else if ("poor".equals(corrupted)) restoredQuality = 10;
            else if ("broken".equals(corrupted)) restoredQuality = 0;
            
            pdc.set(ItemUtil.KEY_QUALITY, PersistentDataType.INTEGER, restoredQuality);
        }

        // Extract owner if present
        if (pdc.has(ItemUtil.KEY_FIRST_OWNER, PersistentDataType.STRING)) {
            String firstOwner = pdc.get(ItemUtil.KEY_FIRST_OWNER, PersistentDataType.STRING);
            stats.setCreatorName(firstOwner);
            stats.setOriginalOwnerName(firstOwner);
            stats.addOwner(firstOwner);
        }

        if (plugin.getHookManager().getWorldCoreHook() != null) {
            stats.setCreationSeason(plugin.getHookManager().getWorldCoreHook().getCurrentSeason());
            stats.setCreationEvent(plugin.getHookManager().getWorldCoreHook().getCurrentEvent());
        }

        // Apply new UUID
        pdc.set(KEY_ITEM_UUID, PersistentDataType.STRING, newUuid.toString());
        // Apply Reveal state (fully revealed since it's an old item)
        pdc.set(KEY_REVEAL_STATE, PersistentDataType.INTEGER, dev.raritycore.rarity.RevealFlags.FLAG_ALL);

        stack.setItemMeta(meta);

        // Async save the initial stats to SQLite to establish the record
        // We do this immediately so that if they unequip it, it's not lost
        plugin.getStorageManager().getSQLiteStorage().saveStats(stats);
        // Also put it in the cache for immediate use
        plugin.getStorageManager().getCacheManager().loadIntoCache(stats);

        return newUuid;
    }
}
