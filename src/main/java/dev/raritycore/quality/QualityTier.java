package dev.raritycore.quality;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a quality tier (e.g., Broken, Normal, Masterwork).
 */
public final class QualityTier implements Comparable<QualityTier> {

    private final String id;
    private final int tier;
    private final String displayName;
    private final String symbol;
    private final int statMin;
    private final int statMax;

    public QualityTier(@NotNull String id, int tier, @NotNull String displayName, String symbol, int statMin, int statMax) {
        this.id = id.toLowerCase();
        this.tier = tier;
        this.displayName = displayName;
        this.symbol = symbol != null ? symbol : "";
        this.statMin = statMin;
        this.statMax = statMax;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public int getTier() {
        return tier;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public int getStatMin() {
        return statMin;
    }
    
    public int getStatMax() {
        return statMax;
    }

    public boolean isLegacy() {
        return "quality_legacy".equals(id);
    }

    @Override
    public int compareTo(@NotNull QualityTier o) {
        return Integer.compare(this.tier, o.tier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof QualityTier)) return false;
        return this.id.equals(((QualityTier) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "QualityTier{" + id + "}";
    }
}
