package dev.raritycore.trait;

import dev.raritycore.RarityCorePlugin;

public class TraitSystem {

    private final RarityCorePlugin plugin;
    private final TraitManager manager;
    private final TraitConflictManager conflictManager;
    private final TraitEffectFactory effectFactory;
    private final TraitProgressManager progressManager;
    private final TraitSynergyManager synergyManager;

    public TraitSystem(RarityCorePlugin plugin, TraitManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.conflictManager = new TraitConflictManager();
        this.effectFactory = new TraitEffectFactory();
        this.progressManager = new TraitProgressManager(plugin);
        this.synergyManager = new TraitSynergyManager();
    }

    public TraitManager getManager() {
        return manager;
    }

    public TraitConflictManager getConflictManager() {
        return conflictManager;
    }

    public TraitEffectFactory getEffectFactory() {
        return effectFactory;
    }

    public TraitProgressManager getProgressManager() {
        return progressManager;
    }

    public TraitSynergyManager getSynergyManager() {
        return synergyManager;
    }
}
