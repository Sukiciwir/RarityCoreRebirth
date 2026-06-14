package dev.raritycore.legacy;

import dev.raritycore.storage.ItemStatistics;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Manages the transition of ownership and resonance levels for legacy items.
 */
public final class ResonanceSystem {

    private final dev.raritycore.RarityCorePlugin plugin;

    public ResonanceSystem(dev.raritycore.RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculates the current resonance of the item for a given player.
     * Original owner = 1.0 (100%)
     * Others start lower but can increase through the SuccessorManager.
     */
    public double calculateResonance(@NotNull ItemStatistics stats, @NotNull Player player) {
        String playerName = player.getName();
        String currentMaster = stats.getCurrentMaster();
        
        if (currentMaster != null && currentMaster.equals(playerName)) {
            return 1.0;
        }

        // They are not the master. The SuccessorManager handles progression.
        // We look at how many kills/blocks they have compared to the threshold.
        int threshold = plugin.getConfigManager().getLegacyReqKills() / 2;
        int progress = stats.getSuccessorProgress();

        // Starts at 0.3 (30%), grows up to 1.0 (100%) as progress nears threshold
        double resonance = 0.3 + (0.7 * ((double) progress / Math.max(1, threshold)));
        return Math.min(1.0, resonance);
    }
}
