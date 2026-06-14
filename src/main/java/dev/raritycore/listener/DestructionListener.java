package dev.raritycore.listener;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.storage.ItemStatistics;
import dev.raritycore.storage.MigrationManager;
import dev.raritycore.util.ItemUtil;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class DestructionListener implements Listener {

    private final RarityCorePlugin plugin;

    public DestructionListener(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    private void handleDestruction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        
        String uuidStr = item.getItemMeta().getPersistentDataContainer().get(MigrationManager.KEY_ITEM_UUID, PersistentDataType.STRING);
        if (uuidStr == null) return;

        try {
            UUID uuid = UUID.fromString(uuidStr);
            ItemStatistics stats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(uuid);
            if (stats != null && !stats.isDestroyed()) {
                stats.setDestroyed(true);
                plugin.getStorageManager().getCacheManager().markDirty(stats);
                plugin.getLogger().info("RarityCore Item Destroyed: " + uuid + " (Flagged for Chronicle)");
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        handleDestruction(event.getEntity().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item itemEntity) {
            // Lava, Fire, Cactus, Explosion
            if (event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.CONTACT || // Cactus
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                
                // If the damage is going to destroy the item entity
                // (Items usually have 5 health)
                if (itemEntity.getHealth() - event.getFinalDamage() <= 0) {
                    handleDestruction(itemEntity.getItemStack());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        handleDestruction(event.getBrokenItem());
    }
}
