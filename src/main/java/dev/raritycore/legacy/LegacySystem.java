package dev.raritycore.legacy;

import dev.raritycore.RarityCorePlugin;

/**
 * Groups all components related to the Legacy endgame mechanics.
 */
public final class LegacySystem {

    private final LegacyManager legacyManager;
    private final ResonanceSystem resonanceSystem;
    private final SuccessorManager successorManager;

    public LegacySystem(RarityCorePlugin plugin) {
        this.resonanceSystem = new ResonanceSystem(plugin);
        this.successorManager = new SuccessorManager(plugin);
        this.legacyManager = new LegacyManager(plugin, resonanceSystem, successorManager);
    }

    public LegacyManager getLegacyManager() { return legacyManager; }
    public ResonanceSystem getResonanceSystem() { return resonanceSystem; }
    public SuccessorManager getSuccessorManager() { return successorManager; }
}
