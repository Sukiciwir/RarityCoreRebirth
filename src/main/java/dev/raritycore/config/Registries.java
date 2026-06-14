package dev.raritycore.config;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.quality.QualityManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityRegistry;
import dev.raritycore.trait.TraitManager;

/**
 * Wraps dynamic configuration-based registries for RarityCore.
 */
public final class Registries {

    private final RarityRegistry rarityRegistry;
    private final ItemRegistry itemRegistry;
    private final QualityManager qualityManager;
    private final TraitManager traitManager;
    
    public Registries(RarityCorePlugin plugin) {
        this.rarityRegistry = new RarityRegistry(plugin);
        this.itemRegistry = new ItemRegistry(plugin, this);
        this.qualityManager = new QualityManager(plugin);
        this.traitManager = new TraitManager(plugin);
    }
    
    public void load() {
        rarityRegistry.load();
        qualityManager.load();
        traitManager.load();
        itemRegistry.load(); // Depends on rarityRegistry
    }
    
    public RarityRegistry getRarities() {
        return rarityRegistry;
    }
    
    public ItemRegistry getItems() {
        return itemRegistry;
    }

    public QualityManager getQualities() {
        return qualityManager;
    }

    public TraitManager getTraits() {
        return traitManager;
    }
}
