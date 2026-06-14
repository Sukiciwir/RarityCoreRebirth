package dev.raritycore.stats;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.storage.ItemStatistics;
import dev.raritycore.storage.MigrationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listens for events to update item statistics in the ItemCacheManager.
 */
public final class StatsListener implements Listener {

    private final RarityCorePlugin plugin;

    public StatsListener(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        processStat(weapon, ItemStatistics::addKills, killer, 1, true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        processStat(tool, ItemStatistics::addBlocksMined, player, 1, true);
    }

    @EventHandler
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            processStat(weapon, ItemStatistics::addDamageDealt, player, (int) event.getFinalDamage(), true);
        }
    }

    @EventHandler
    public void onDamageAbsorbed(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                processStat(armor, ItemStatistics::addDamageAbsorbed, player, (int) event.getFinalDamage(), true);
            }
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            ItemStack rod = event.getPlayer().getInventory().getItemInMainHand();
            processStat(rod, ItemStatistics::addFishCaught, event.getPlayer(), 1, true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            
            ItemStack boots = event.getPlayer().getInventory().getBoots();
            processStat(boots, ItemStatistics::addDistanceTraveled, event.getPlayer(), 1, false);
        }
    }

    private void processStat(ItemStack item, StatUpdater updater, Player player, int amount, boolean rebuildLore) {
        if (item == null || !item.hasItemMeta()) return;
        
        MigrationManager migration = plugin.getStorageManager().getMigrationManager();
        UUID itemUuid = migration.migrateOrGetUUID(item);
        if (itemUuid == null) return; // Not a RarityCore item

        ItemStatistics stats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(itemUuid);
        stats.addOwner(player.getName()); // Ensure current owner is tracked
        
        updater.update(stats, amount);
        
        // Mark dirty for DB flush
        plugin.getStorageManager().getCacheManager().markDirty(stats);
        
        // Re-evaluate familiarity and legacy ascension
        plugin.getIdentityManager().getFamiliaritySystem().evaluateFamiliarity(item, stats);
        plugin.getLegacySystem().getLegacyManager().checkAscension(item, stats);
        
        // Evaluate succession
        plugin.getLegacySystem().getSuccessorManager().evaluateSuccession(stats, player, amount);

        if (rebuildLore) {
            plugin.getRarityItemFactory().rebuildLore(item);
        }
    }

    @FunctionalInterface
    private interface StatUpdater {
        void update(ItemStatistics stats, int amount);
    }
}
