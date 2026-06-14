package dev.raritycore.generation;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.hooks.WorldCoreHook;
import dev.raritycore.trait.Trait;

import java.util.List;

/**
 * Adjusts generation weights based on current server context.
 */
public final class ContextRoller {

    private final RarityCorePlugin plugin;
    private WorldCoreHook worldCoreHook;

    public ContextRoller(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void setWorldCoreHook(WorldCoreHook hook) {
        this.worldCoreHook = hook;
    }

    public double getRarityMultiplier() {
        if (worldCoreHook != null) {
            String event = worldCoreHook.getCurrentEvent();
            if ("DIAMOND_FEVER".equalsIgnoreCase(event)) {
                return 1.5; // 50% boost to rarity
            }
        }
        return 1.0;
    }

    public List<Trait> filterTraitsByContext(List<Trait> pool) {
        if (worldCoreHook == null) return pool;

        String season = worldCoreHook.getCurrentSeason();
        String event = worldCoreHook.getCurrentEvent();

        // For simplicity, just returning the pool.
        // In a complete implementation, this would weight traits like "Frost" higher in Winter.
        return pool;
    }
}
