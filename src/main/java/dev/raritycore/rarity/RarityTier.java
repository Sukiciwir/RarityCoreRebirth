package dev.raritycore.rarity;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a dynamically loaded rarity tier.
 * Combines the old Rarity enum and RarityConfig.
 */
public final class RarityTier implements Comparable<RarityTier> {

    private final String id;
    private final int tier;
    private final boolean gachaOnly;

    private final String color;
    private final String prefix;
    private final String displayNameColor;
    private final boolean glow;
    private final boolean broadcast;
    private final @Nullable Particle particle;
    private final int particleDensity;
    private final boolean upgradeableTo;
    
    private final double traitChance;
    private final int traitMin;
    private final int traitMax;

    public RarityTier(@NotNull String id, int tier, @NotNull ConfigurationSection sec) {
        this.id = id.toLowerCase();
        this.tier = tier;
        this.gachaOnly = sec.getBoolean("gacha-only", false);

        this.color = sec.getString("color", "<white>");
        this.prefix = sec.getString("prefix", "");
        this.displayNameColor = sec.getString("display-name-color", "#FFFFFF");
        this.glow = sec.getBoolean("glow", false);
        this.broadcast = sec.getBoolean("broadcast", false);
        this.particleDensity = sec.getInt("particle-density", 0);

        String particleName = sec.getString("particle", "NONE");
        Particle p = null;
        if (!"NONE".equalsIgnoreCase(particleName)) {
            try { p = Particle.valueOf(particleName.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        this.particle = p;

        boolean configUpgradeable = sec.getBoolean("upgradeable-into", !this.gachaOnly);
        this.upgradeableTo = !this.gachaOnly && configUpgradeable;

        this.traitChance = sec.getDouble("trait-chance", 0.0);
        this.traitMin = sec.getInt("trait-min", 0);
        this.traitMax = sec.getInt("trait-max", 0);
    }

    /** The unique ID of this rarity (used in configs). */
    @NotNull public String getId() { return id; }
    /** Numeric tier index (higher means rarer). */
    public int getTier() { return tier; }
    public boolean isGachaOnly() { return gachaOnly; }

    @NotNull public String getColor() { return color; }
    @NotNull public String getPrefix() { return prefix; }
    @NotNull public String getDisplayNameColor() { return displayNameColor; }
    public boolean isGlow() { return glow; }
    public boolean isBroadcast() { return broadcast; }
    @Nullable public Particle getParticle() { return particle; }
    public int getParticleDensity() { return particleDensity; }
    public boolean isUpgradeableTo() { return upgradeableTo; }
    
    public double getTraitChance() { return traitChance; }
    public int getTraitMin() { return traitMin; }
    public int getTraitMax() { return traitMax; }

    @Override
    public int compareTo(@NotNull RarityTier o) {
        return Integer.compare(this.tier, o.tier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RarityTier)) return false;
        return this.id.equals(((RarityTier) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RarityTier{" + id + "}";
    }
}
