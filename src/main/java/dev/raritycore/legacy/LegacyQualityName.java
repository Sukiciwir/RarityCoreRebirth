package dev.raritycore.legacy;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the visual identities of a QUALITY_LEGACY item.
 */
public enum LegacyQualityName {
    SOULBOUND("Soulbound"),
    SOULFORGED("Soulforged"),
    TRANSCENDENT("Transcendent"),
    ETERNAL("Eternal"),
    WILLWEAVER("Willweaver"); // The rarest

    private final String displayName;

    LegacyQualityName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LegacyQualityName random() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.05) return WILLWEAVER;
        if (roll < 0.20) return ETERNAL;
        if (roll < 0.40) return TRANSCENDENT;
        if (roll < 0.70) return SOULFORGED;
        return SOULBOUND;
    }
}
