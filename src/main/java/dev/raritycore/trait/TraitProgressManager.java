package dev.raritycore.trait;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.util.ItemUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TraitProgressManager {

    private final RarityCorePlugin plugin;
    private final int DISCOVERY_THRESHOLD = 50;

    public TraitProgressManager(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void checkProgress(Player player, ItemStack item, TraitInstance instance) {
        boolean changed = false;
        
        // Check Discovery
        if (!instance.isDiscovered() && instance.getProgress() >= DISCOVERY_THRESHOLD) {
            instance.setDiscovered(true);
            changed = true;
            
            Trait trait = plugin.getTraitSystem().getManager().get(instance.getTraitId());
            if (trait != null) {
                player.sendMessage(dev.raritycore.util.ColorUtil.parse("<yellow>✨ Your item has revealed a hidden trait: <gold>" + trait.getDisplayName() + "</gold>!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        }

        
        // Save back to PDC if changed or to track progress incrementally
        // Actually, we should always save progress if it incremented
        // But rewriting PDC every kill might be heavy. For now we will just write it.
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            List<TraitInstance> traits = ItemUtil.getTraits(meta);
            
            // update the specific instance in the list
            for (int i = 0; i < traits.size(); i++) {
                if (traits.get(i).getTraitId().equals(instance.getTraitId())) {
                    traits.set(i, instance);
                    break;
                }
            }
            ItemUtil.setTraits(meta, traits);
            item.setItemMeta(meta);
        }
    }
}
