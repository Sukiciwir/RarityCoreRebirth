package dev.raritycore.trait.listener;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.trait.EventContext;
import dev.raritycore.trait.Trait;
import dev.raritycore.trait.TraitInstance;
import dev.raritycore.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CombatHub implements Listener {

    private final RarityCorePlugin plugin;

    public CombatHub(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() != null) {
            Player player = e.getEntity().getKiller();
            ItemStack item = player.getInventory().getItemInMainHand();
            
            if (item == null || !item.hasItemMeta()) return;
            
            List<TraitInstance> traits = ItemUtil.getTraits(item.getItemMeta());
            for (TraitInstance inst : traits) {
                Trait trait = plugin.getTraitSystem().getManager().get(inst.getTraitId());
                if (trait != null && trait.getTrigger().equals("ENTITY_KILL")) {
                    EventContext ctx = new EventContext().withTargetEntity(e.getEntity());
                    plugin.getTraitSystem().getEffectFactory().execute(player, trait, inst, ctx);
                    
                    // Increment progress
                    inst.addProgress(1);
                    plugin.getTraitSystem().getProgressManager().checkProgress(player, item, inst);
                }
            }
            
            // Check synergies
            List<String> synergies = plugin.getTraitSystem().getSynergyManager().getSynergyEffects(traits);
            for (String effectString : synergies) {
                // Wrap in a temporary trait to execute
                Trait tempTrait = new Trait("synergy", new org.bukkit.configuration.file.YamlConfiguration());
                try {
                    java.lang.reflect.Field f = Trait.class.getDeclaredField("effects");
                    f.setAccessible(true);
                    f.set(tempTrait, List.of(effectString));
                } catch (Exception ignored) {}
                plugin.getTraitSystem().getEffectFactory().execute(player, tempTrait, new TraitInstance("synergy", true, 0), new EventContext());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || !item.hasItemMeta()) return;
            
            List<TraitInstance> traits = ItemUtil.getTraits(item.getItemMeta());
            for (TraitInstance inst : traits) {
                Trait trait = plugin.getTraitSystem().getManager().get(inst.getTraitId());
                if (trait != null && trait.getTrigger().equals("ENTITY_DAMAGE")) {
                    EventContext ctx = new EventContext().withTargetEntity(e.getEntity()).withValue(e.getDamage());
                    plugin.getTraitSystem().getEffectFactory().execute(player, trait, inst, ctx);
                    e.setDamage(ctx.getValue());
                }
            }
        }
    }
}
