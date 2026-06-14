package dev.raritycore.identity;

import dev.raritycore.RarityCorePlugin;

/**
 * Manages how an item evolves and builds its identity over time.
 */
public final class IdentityManager {

    private final RarityCorePlugin plugin;
    private final FamiliaritySystem familiaritySystem;
    private final EvolutionStage evolutionStage;
    private final EpithetGenerator epithetGenerator;
    private final NameGenerator nameGenerator;

    public IdentityManager(RarityCorePlugin plugin) {
        this.plugin = plugin;
        this.familiaritySystem = new FamiliaritySystem(plugin);
        this.evolutionStage = new EvolutionStage();
        this.epithetGenerator = new EpithetGenerator();
        this.nameGenerator = new NameGenerator();
    }

    public FamiliaritySystem getFamiliaritySystem() { return familiaritySystem; }
    public EvolutionStage getEvolutionStage() { return evolutionStage; }
    public EpithetGenerator getEpithetGenerator() { return epithetGenerator; }
    public NameGenerator getNameGenerator() { return nameGenerator; }
}
