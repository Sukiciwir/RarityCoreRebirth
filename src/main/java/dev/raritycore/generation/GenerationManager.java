package dev.raritycore.generation;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.Registries;
import dev.raritycore.quality.QualityTier;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.trait.Trait;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles all aspects of item birth/generation.
 */
public final class GenerationManager {

    private final RarityCorePlugin plugin;
    private final Registries registries;

    private final ContextRoller contextRoller;
    private final HiddenPotential hiddenPotential;

    public GenerationManager(RarityCorePlugin plugin, Registries registries) {
        this.plugin = plugin;
        this.registries = registries;
        this.contextRoller = new ContextRoller(plugin);
        this.hiddenPotential = new HiddenPotential();
    }

    public ContextRoller getContextRoller() {
        return contextRoller;
    }

    /**
     * Rolls a rarity tier using configured gacha rates and context weights.
     */
    public RarityTier rollRarity() {
        double contextMultiplier = contextRoller.getRarityMultiplier();
        java.util.Map<String, Double> rates = plugin.getConfigManager().getGenerationRates();
        
        List<java.util.Map.Entry<String, Double>> sortedRates = new ArrayList<>(rates.entrySet());
        sortedRates.sort(java.util.Map.Entry.comparingByValue()); // Rarest first

        double totalWeight = 0.0;
        for (java.util.Map.Entry<String, Double> entry : sortedRates) {
            RarityTier r = registries.getRarities().get(entry.getKey());
            if (r == null || r.isGachaOnly()) continue;
            double chance = entry.getValue();
            if (!entry.getKey().equalsIgnoreCase("common")) chance *= contextMultiplier;
            totalWeight += chance;
        }
        
        double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0.0;
        
        for (java.util.Map.Entry<String, Double> entry : sortedRates) {
            RarityTier r = registries.getRarities().get(entry.getKey());
            if (r == null || r.isGachaOnly()) continue;
            double chance = entry.getValue();
            if (!entry.getKey().equalsIgnoreCase("common")) chance *= contextMultiplier;
            cumulative += chance;
            if (roll <= cumulative) {
                return r;
            }
        }

        RarityTier result = registries.getRarities().get("common");
        return result != null ? result : registries.getRarities().getAll().iterator().next();
    }

    /**
     * Rolls an initial quality tier (Broken to Masterwork).
     * Cannot roll Legacy quality initially.
     */
    public QualityTier rollInitialQuality() {
        List<QualityTier> pool = new ArrayList<>();
        for (QualityTier q : registries.getQualities().getAll()) {
            if (!q.isLegacy()) pool.add(q);
        }

        if (pool.isEmpty()) return null;

        // Simplified logic: randomly pick one, weighted by the configured exponent
        double exponent = plugin.getConfigManager().getQualityWeightExponent();
        double raw = Math.random();
        double biased = 1.0 - Math.pow(raw, 1.0 / exponent);

        int index = (int) (biased * pool.size());
        index = Math.max(0, Math.min(pool.size() - 1, index));

        return pool.get(index);
    }

    /**
     * Rolls a random trait appropriate for the given material family and context.
     */
    @Nullable
    public Trait rollTrait(Material material) {
        String family = determineFamily(material);
        List<Trait> pool = new ArrayList<>();

        for (Trait t : registries.getTraits().getAll()) {
            if (t.getAllowedFamilies().contains(family) || t.getAllowedFamilies().contains("ALL")) {
                pool.add(t);
            }
        }

        pool = contextRoller.filterTraitsByContext(pool);

        if (pool.isEmpty()) return null;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /**
     * Determines if an item should be born "Unknown".
     */
    public boolean rollHiddenPotential(Material material, RarityTier rarity) {
        return hiddenPotential.rollHiddenPotential(material, rarity);
    }

    public String determineFamily(Material material) {
        String name = material.name();
        if (name.endsWith("_SWORD") || name.endsWith("_AXE")) return "WEAPON";
        if (name.endsWith("_PICKAXE")) return "PICKAXE";
        if (name.endsWith("FISHING_ROD")) return "FISHING_ROD";
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) return "ARMOR";
        return "GENERAL";
    }
}
