package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityItemFactory;
import dev.raritycore.rarity.RarityTier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * EvenMoreFish integration: when a player catches a fish, there is a small
 * chance to also receive a rarity item from the COMMON or UNCOMMON pool.
 *
 * <p>Note: This hook does not depend on EvenMoreFish's internal API — it simply
 * listens to the vanilla {@link PlayerFishEvent} and fires alongside EMF.
 * A full EMF reward integration would require their RewardManager API.
 */
public final class EvenMoreFishHook implements Listener {

    private final RarityCorePlugin plugin;
    private final ItemRegistry registry;
    private final RarityItemFactory factory;

    public EvenMoreFishHook(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getRegistries().getItems();
        this.factory  = plugin.getRarityItemFactory();
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        // 5% chance to award a bonus rarity item from COMMON or UNCOMMON
        if (Math.random() > 0.05) return;

        RarityTier rarity = Math.random() < 0.8 ? plugin.getRegistries().getRarities().get("common") : plugin.getRegistries().getRarities().get("uncommon");
        if (rarity == null) return;
        RarityItem template = registry.random(rarity);
        if (template == null) return;

        ItemStack bonus = factory.build(template, event.getPlayer().getName(), null, 0);
        Map<Integer, ItemStack> overflow = event.getPlayer().getInventory().addItem(bonus);
        for (ItemStack leftover : overflow.values()) {
            event.getPlayer().getWorld().dropItemNaturally(
                    event.getPlayer().getLocation(), leftover);
        }

        event.getPlayer().sendMessage(plugin.getConfigManager().getMessages().get("discovery-first",
                "rarity_color", "<gray>",
                "item_name", template.getDisplayName().replaceAll("<[^>]+>", "")));
    }
}
