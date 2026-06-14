package dev.raritycore.legacy;

import dev.raritycore.storage.ItemStatistics;
import org.bukkit.entity.Player;

/**
 * Handles the logic where a legacy item slowly accepts a new owner.
 */
public final class SuccessorManager {

    private final dev.raritycore.RarityCorePlugin plugin;

    public SuccessorManager(dev.raritycore.RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the player has used the item enough to become the new primary master.
     * Note: the original owner history is always preserved.
     */
    public void evaluateSuccession(ItemStatistics stats, Player currentPlayer, int scoreDelta) {
        String currentMaster = stats.getCurrentMaster();
        
        // If they are already the master, no succession logic needed, just ensure progress is 0
        if (currentMaster != null && currentMaster.equals(currentPlayer.getName())) {
            if (stats.getSuccessorProgress() != 0) {
                stats.setSuccessorProgress(0);
            }
            return;
        }
        
        // They are not the master; add progress
        stats.addSuccessorProgress(scoreDelta);
        
        // Let's assume threshold is 5000 actions. In a real environment, read from config.
        int threshold = plugin.getConfigManager().getLegacyReqKills() / 2; // Half of legacy req, so 5000
        
        if (stats.getSuccessorProgress() >= threshold) {
            stats.setCurrentMaster(currentPlayer.getName());
            stats.setSuccessorProgress(0);
            
            // Rebuild Lore to show the new master? Or just broadcast.
            plugin.getServer().broadcastMessage("§6⚜ The item has accepted " + currentPlayer.getName() + " as its new Master!");
        }
    }
}
