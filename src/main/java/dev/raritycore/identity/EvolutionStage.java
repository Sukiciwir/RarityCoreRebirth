package dev.raritycore.identity;

import dev.raritycore.storage.ItemStatistics;
import org.jetbrains.annotations.Nullable;

/**
 * Handles cosmetic evolution stages based on kills.
 */
public final class EvolutionStage {

    public enum Stage {
        NONE(0, ""),
        SEASONED(500, "Seasoned"),
        VETERAN(2000, "Veteran"),
        BATTLE_WORN(10000, "Battle-Worn"),
        LEGENDARY_SLAYER(30000, "Legendary Slayer");

        private final int requiredKills;
        private final String title;

        Stage(int requiredKills, String title) {
            this.requiredKills = requiredKills;
            this.title = title;
        }

        public int getRequiredKills() { return requiredKills; }
        public String getTitle() { return title; }
    }

    @Nullable
    public Stage determineStage(ItemStatistics stats) {
        int kills = stats.getKills();
        
        if (kills >= Stage.LEGENDARY_SLAYER.getRequiredKills()) return Stage.LEGENDARY_SLAYER;
        if (kills >= Stage.BATTLE_WORN.getRequiredKills()) return Stage.BATTLE_WORN;
        if (kills >= Stage.VETERAN.getRequiredKills()) return Stage.VETERAN;
        if (kills >= Stage.SEASONED.getRequiredKills()) return Stage.SEASONED;
        
        return Stage.NONE;
    }
}
