package dev.raritycore.trait;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TraitEffectFactory {

    /**
     * Executes the effects of a trait.
     * @param player The player triggering the trait
     * @param trait The static trait definition
     * @param instance The active trait instance on the item (to scale with mastery level)
     * @param context Event context for targeted effects or value modification
     */
    public void execute(Player player, Trait trait, TraitInstance instance, EventContext context) {
        for (String effectString : trait.getEffects()) {
            String[] parts = effectString.split(":");
            String action = parts[0].toUpperCase();
            
            switch (action) {
                case "HEAL":
                    if (parts.length > 1) {
                        double amount = Double.parseDouble(parts[1]);
                        // Scale with mastery level if needed
                        double newHealth = Math.min(player.getHealth() + amount, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                        player.setHealth(newHealth);
                    }
                    break;
                case "BUFF":
                    if (parts.length > 3) {
                        String typeName = parts[1].toUpperCase();
                        int durationTicks = Integer.parseInt(parts[2]);
                        int amplifier = Integer.parseInt(parts[3]);
                        
                        PotionEffectType type = PotionEffectType.getByName(typeName);
                        if (type != null) {
                            player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
                        }
                    }
                    break;
                case "MULTIPLY_DAMAGE":
                    if (parts.length > 1) {
                        double mult = Double.parseDouble(parts[1]);
                        context.setValue(context.getValue() * mult);
                    }
                    break;
                case "REDUCE_DAMAGE":
                    if (parts.length > 1) {
                        double reduction = Double.parseDouble(parts[1]);
                        context.setValue(context.getValue() * (1.0 - reduction));
                    }
                    break;
                // Add more effects as needed...
            }
        }
    }
}
