package dev.raritycore.identity;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.storage.ItemStatistics;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import dev.raritycore.storage.MigrationManager;

/**
 * Handles the progressive revealing of Unknown items (Rarity -> Quality -> Traits -> Affixes).
 */
public final class FamiliaritySystem {

    private final RarityCorePlugin plugin;

    public FamiliaritySystem(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Re-evaluates familiarity and reveals features if thresholds are met.
     */
    public void evaluateFamiliarity(ItemStack item, ItemStatistics stats) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        int currentState = pdc.getOrDefault(MigrationManager.KEY_REVEAL_STATE, PersistentDataType.INTEGER, 0);
        if (currentState >= 4) return; // Fully revealed

        int familiarityScore = calculateFamiliarityScore(item, stats);
        
        // Example thresholds (these should be configurable)
        int threshold1 = 100;
        int threshold2 = 500;
        int threshold3 = 1000;
        int threshold4 = 2500;

        boolean changed = false;
        if (currentState < 1 && familiarityScore >= threshold1) {
            currentState = 1; // Rarity revealed
            changed = true;
        }
        if (currentState < 2 && familiarityScore >= threshold2) {
            currentState = 2; // Quality revealed
            changed = true;
        }
        if (currentState < 3 && familiarityScore >= threshold3) {
            currentState = 3; // Traits revealed
            changed = true;
        }
        if (currentState < 4 && familiarityScore >= threshold4) {
            currentState = 4; // Affixes revealed
            changed = true;
        }

        if (changed) {
            pdc.set(MigrationManager.KEY_REVEAL_STATE, PersistentDataType.INTEGER, currentState);
            item.setItemMeta(meta);
            plugin.getRarityItemFactory().rebuildLore(item);
        }
    }

    /**
     * Computes a weighted score based on item family.
     */
    private int calculateFamiliarityScore(ItemStack item, ItemStatistics stats) {
        String type = item.getType().name();
        
        if (type.endsWith("_SWORD") || type.endsWith("_AXE")) {
            return stats.getKills() * 10 + stats.getDamageDealt() / 10;
        } else if (type.endsWith("_PICKAXE") || type.endsWith("_SHOVEL")) {
            return stats.getBlocksMined() * 2;
        } else if (type.endsWith("FISHING_ROD")) {
            return stats.getFishCaught() * 15;
        } else if (type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") || type.endsWith("_LEGGINGS")) {
            return stats.getDamageAbsorbed() / 10;
        } else if (type.endsWith("_BOOTS")) {
            return stats.getDistanceTraveled() / 100;
        }
        
        return stats.getKills() + stats.getBlocksMined(); // Fallback
    }
}
