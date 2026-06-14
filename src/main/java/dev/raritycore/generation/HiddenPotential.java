package dev.raritycore.generation;

import dev.raritycore.rarity.RarityTier;
import org.bukkit.Material;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Evaluates whether an item should be born "Unknown".
 */
public final class HiddenPotential {

    public boolean rollHiddenPotential(Material material, RarityTier rarity) {
        if (!isEligibleMaterial(material)) return false;

        // Configurable chance based on rarity
        double chance = getChanceForRarity(rarity);
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private boolean isEligibleMaterial(Material material) {
        String name = material.name();
        return name.contains("IRON_") || name.contains("DIAMOND_") || name.contains("NETHERITE_");
    }

    private double getChanceForRarity(RarityTier rarity) {
        // Defaults: rare 5%, epic 15%, legendary 35%, mythic 70%, divine 100%
        switch (rarity.getId()) {
            case "rare": return 0.05;
            case "epic": return 0.15;
            case "legendary": return 0.35;
            case "mythic": return 0.70;
            case "divine": return 1.0;
            case "ancient": return 1.0;
            default: return 0.0;
        }
    }
}
