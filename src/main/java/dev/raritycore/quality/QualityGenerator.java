package dev.raritycore.quality;

import dev.raritycore.RarityCorePlugin;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates cosmetic quality values (1–100) for rarity items.
 * Quality is purely visual — it does not affect gameplay stats.
 */
public final class QualityGenerator {

    /** Generates a random quality value between 80 and 120 (inclusive). */
    public static int generate() {
        return ThreadLocalRandom.current().nextInt(80, 121);
    }

    /**
     * Generates a quality value biased toward lower scores using an exponent.
     * Higher exponent = more items at low quality (realistic rarity bell curve).
     * {@code exponent = 1.0} → uniform.
     */
    public static int generateWeighted(double exponent) {
        double raw = Math.random(); // 0.0–1.0
        // Apply power curve: pow(1 - raw, exponent) shifts distribution toward 1
        double biased = 1.0 - Math.pow(raw, 1.0 / exponent);
        int quality = (int) Math.round(biased * 40) + 80;
        return Math.min(120, Math.max(80, quality));
    }

    private QualityGenerator() {}
}
