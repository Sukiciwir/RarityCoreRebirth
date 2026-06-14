package dev.raritycore.identity;

import dev.raritycore.quality.QualityTier;
import dev.raritycore.rarity.RarityTier;
import org.jetbrains.annotations.Nullable;

/**
 * Generates the item's display name based on rarity, quality, and legacy state.
 */
public final class NameGenerator {

    public String generateName(String baseName, RarityTier rarity, @Nullable QualityTier quality, @Nullable EvolutionStage.Stage stage) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(rarity.getColor());
        
        if (stage != null && stage != EvolutionStage.Stage.NONE) {
            sb.append("[").append(stage.getTitle()).append("] ");
        }
        
        sb.append(baseName);
        
        return sb.toString();
    }

    public String generateEpithet(dev.raritycore.storage.ItemStatistics stats, org.bukkit.inventory.ItemStack item) {
        // Placeholder for future epithet generation logic
        return "The Awakened";
    }
}
