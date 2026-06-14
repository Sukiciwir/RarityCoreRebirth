package dev.raritycore.trait;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a dynamically loaded Trait that belongs to certain material families.
 * Stores data-driven triggers, effects, weights, and incompatibilities.
 */
public final class Trait {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final List<String> allowedFamilies;

    private final double weight;
    private final String category;
    private final List<String> incompatible;
    private final String trigger;
    private final List<String> effects;
    
    private final @Nullable String availability; // Seasonal/Event requirement

    public Trait(@NotNull String id, @NotNull ConfigurationSection sec) {
        this.id = id.toLowerCase();
        this.displayName = sec.getString("display-name", id);
        this.description = List.copyOf(sec.getStringList("description"));
        this.allowedFamilies = List.copyOf(sec.getStringList("allowed-families"));
        
        this.weight = sec.getDouble("weight", 10.0);
        this.category = sec.getString("category", "UTILITY").toUpperCase();
        this.incompatible = List.copyOf(sec.getStringList("incompatible"));
        this.trigger = sec.getString("trigger", "NONE").toUpperCase();
        this.effects = List.copyOf(sec.getStringList("effects"));
        this.availability = sec.getString("availability");
    }

    @NotNull public String getId() { return id; }
    @NotNull public String getDisplayName() { return displayName; }
    @NotNull public List<String> getDescription() { return description; }
    @NotNull public List<String> getAllowedFamilies() { return allowedFamilies; }

    public double getWeight() { return weight; }
    @NotNull public String getCategory() { return category; }
    @NotNull public List<String> getIncompatible() { return incompatible; }
    @NotNull public String getTrigger() { return trigger; }
    @NotNull public List<String> getEffects() { return effects; }
    @Nullable public String getAvailability() { return availability; }
}
