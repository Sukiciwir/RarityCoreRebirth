package dev.raritycore.legacy;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.storage.ItemStatistics;
import dev.raritycore.storage.QualityHistoryEntry;

/**
 * Checks ascension requirements and triggers server-wide Legacy Ascension events.
 */
public final class LegacyManager {

    private final RarityCorePlugin plugin;
    private final ResonanceSystem resonanceSystem;
    private final SuccessorManager successorManager;

    public LegacyManager(RarityCorePlugin plugin, ResonanceSystem resonanceSystem, SuccessorManager successorManager) {
        this.plugin = plugin;
        this.resonanceSystem = resonanceSystem;
        this.successorManager = successorManager;
    }

    /**
     * Attempts to ascend the item to QUALITY_LEGACY if requirements are met.
     */
    public boolean checkAscension(org.bukkit.inventory.ItemStack item, ItemStatistics stats) {
        if (isAlreadyLegacy(stats)) return false;

        int requiredKills = plugin.getConfigManager().getLegacyReqKills();
        long requiredAgeDays = plugin.getConfigManager().getLegacyReqAgeDays();
        int maxOwners = plugin.getConfigManager().getLegacyReqMaxOwners();

        long ageDays = (System.currentTimeMillis() - stats.getCreationTimestamp()) / (1000 * 60 * 60 * 24);

        if (item != null && item.hasItemMeta()) {
            Integer qVal = dev.raritycore.util.ItemUtil.getQuality(item.getItemMeta());
            int quality = qVal != null ? qVal : 0;
            dev.raritycore.quality.QualityTier qTier = plugin.getRegistries().getQualities().getQualityForPercentage(quality);
            dev.raritycore.quality.QualityTier maxQTier = plugin.getRegistries().getQualities().getMaxQuality();
            
            if (qTier == null || maxQTier == null || qTier.getTier() < maxQTier.getTier()) {
                return false;
            }
        } else {
            return false; // Can't ascend if we don't know it's masterwork
        }

        if (stats.getKills() >= requiredKills && ageDays >= requiredAgeDays && stats.getOwnersHistory().size() <= maxOwners) {
            ascend(item, stats);
            return true;
        }

        return false;
    }

    private boolean isAlreadyLegacy(ItemStatistics stats) {
        return stats.getQualityHistory().stream().anyMatch(q -> q.getQualityId().equals("quality_legacy"));
    }

    private void ascend(org.bukkit.inventory.ItemStack item, ItemStatistics stats) {
        LegacyQualityName legacyName = LegacyQualityName.random();
        
        dev.raritycore.storage.QualityHistoryEntry entry = new dev.raritycore.storage.QualityHistoryEntry("quality_legacy", System.currentTimeMillis(), "Legacy Ascension (" + legacyName.name() + ")");
        stats.addQualityHistoryEntry(entry);
        plugin.getStorageManager().getCacheManager().markDirty(stats);
        
        if (item != null && item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(dev.raritycore.util.ItemUtil.KEY_QUALITY, org.bukkit.persistence.PersistentDataType.INTEGER, 150);
            item.setItemMeta(meta);
            plugin.getRarityItemFactory().rebuildLore(item);
        }
        
        // Broadcast
        String owner = stats.getOriginalOwnerName() != null ? stats.getOriginalOwnerName() : "Unknown";
        plugin.getBroadcastManager().queueBroadcast(net.kyori.adventure.text.Component.text("§6⚜ " + owner + "'s item has ascended beyond Masterwork!"));
        plugin.getBroadcastManager().queueBroadcast(net.kyori.adventure.text.Component.text("§eThe item has become §6" + legacyName.getDisplayName() + "§e."));
    }

    public void forceAscension(ItemStatistics stats, org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack item) {
        ascend(item, stats);
    }

    public void broadcastLegacy(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack item, ItemStatistics stats) {
        String owner = stats.getOriginalOwnerName() != null ? stats.getOriginalOwnerName() : player.getName();
        String legacyDisplayName = "Soulbound";
        var opt = stats.getQualityHistory().stream().filter(q -> q.getQualityId().equals("quality_legacy")).findFirst();
        if (opt.isPresent()) {
            String note = opt.get().getReason();
            if (note != null && note.contains("(")) {
                legacyDisplayName = note.substring(note.indexOf("(") + 1, note.indexOf(")"));
            }
        }
        
        plugin.getBroadcastManager().queueBroadcast(net.kyori.adventure.text.Component.text("§6⚜ " + owner + "'s item has ascended beyond Masterwork!"));
        plugin.getBroadcastManager().queueBroadcast(net.kyori.adventure.text.Component.text("§eThe item has become §6" + legacyDisplayName + "§e."));
    }
}
