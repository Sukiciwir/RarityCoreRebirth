package dev.raritycore.trait;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an active instance of a trait on a specific item.
 * Tracks discovery state and mastery progress (e.g. kills, blocks mined).
 */
public final class TraitInstance {
    
    private final String traitId;
    private boolean discovered;
    private int progress;
    
    public TraitInstance(@NotNull String traitId, boolean discovered, int progress) {
        this.traitId = traitId;
        this.discovered = discovered;
        this.progress = progress;
    }
    
    @NotNull public String getTraitId() { return traitId; }
    public boolean isDiscovered() { return discovered; }
    public void setDiscovered(boolean discovered) { this.discovered = discovered; }
    
    public int getProgress() { return progress; }
    public void addProgress(int amount) { this.progress += amount; }
    public void setProgress(int progress) { this.progress = progress; }
    

    /**
     * Serializes this instance to a string for PDC storage.
     * Format: "traitId:discovered:progress"
     */
    public String serialize() {
        return traitId + ":" + (discovered ? "1" : "0") + ":" + progress;
    }
    
    /**
     * Deserializes from a PDC string.
     */
    public static TraitInstance deserialize(String data) {
        String[] parts = data.split(":");
        // Backwards compatibility: old format had level at parts[3]
        if (parts.length >= 3) {
            return new TraitInstance(parts[0], "1".equals(parts[1]), Integer.parseInt(parts[2]));
        } else if (parts.length >= 1) {
            // Fallback
            return new TraitInstance(parts[0], false, 0);
        }
        return null;
    }
}
