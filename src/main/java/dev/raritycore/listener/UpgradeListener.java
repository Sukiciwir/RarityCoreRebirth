package dev.raritycore.listener;

import dev.raritycore.RarityCorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

public class UpgradeListener implements Listener {

    private final RarityCorePlugin plugin;

    public UpgradeListener(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    private void transferPDC(ItemStack source, ItemStack result) {
        if (source == null || result == null) return;
        if (!source.hasItemMeta() || !result.hasItemMeta()) return;

        ItemMeta sourceMeta = source.getItemMeta();
        ItemMeta resultMeta = result.getItemMeta();
        
        PersistentDataContainer sourcePdc = sourceMeta.getPersistentDataContainer();
        PersistentDataContainer resultPdc = resultMeta.getPersistentDataContainer();

        // Paper/Spigot natively copies PDC in Anvils but sometimes drops it in custom Smithing plugins or edge cases.
        // We explicitly force-copy all keys belonging to RarityCore just in case.
        for (org.bukkit.NamespacedKey key : sourcePdc.getKeys()) {
            if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                // Determine type based on key (we only use STRING and INTEGER in RarityCore)
                try {
                    if (sourcePdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                        resultPdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, sourcePdc.get(key, org.bukkit.persistence.PersistentDataType.STRING));
                    } else if (sourcePdc.has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                        resultPdc.set(key, org.bukkit.persistence.PersistentDataType.INTEGER, sourcePdc.get(key, org.bukkit.persistence.PersistentDataType.INTEGER));
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // Ensure standard lore display format persists. We re-apply factory logic.
        result.setItemMeta(resultMeta);
        
        // Wait, if it's a rarity item, let's just use the factory to rebuild it directly on the result item
        if (result.getItemMeta().getPersistentDataContainer().has(dev.raritycore.util.ItemUtil.KEY_RARITY, org.bukkit.persistence.PersistentDataType.STRING)) {
            plugin.getRarityItemFactory().rebuildLore(result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack result = event.getResult();
        
        if (first != null && first.hasItemMeta() && first.getItemMeta().getPersistentDataContainer().has(dev.raritycore.util.ItemUtil.KEY_RARITY, org.bukkit.persistence.PersistentDataType.STRING)) {
            if (result != null) {
                transferPDC(first, result);
                event.setResult(result);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSmithingPrepare(PrepareSmithingEvent event) {
        ItemStack equipment = event.getInventory().getItem(1); // The equipment slot
        ItemStack result = event.getResult();
        
        if (equipment != null && equipment.hasItemMeta() && equipment.getItemMeta().getPersistentDataContainer().has(dev.raritycore.util.ItemUtil.KEY_RARITY, org.bukkit.persistence.PersistentDataType.STRING)) {
            if (result != null) {
                transferPDC(equipment, result);
                event.setResult(result);
            }
        }
    }
}
