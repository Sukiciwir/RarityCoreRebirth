package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityItemFactory;
import dev.raritycore.rarity.RarityTier;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * MythicMobs integration: drops configurable rarity items when a MythicMob dies.
 *
 * <p>Configure drop chances in {@code config.yml} under {@code hooks.mythicmobs}
 * (future). Currently uses a flat 10% chance to drop a random item from the
 * mob's tier or below, demonstrating the integration point.
 */
public final class MythicMobsHook implements Listener {

    private final RarityCorePlugin plugin;
    private final ItemRegistry registry;
    private final RarityItemFactory factory;

    public MythicMobsHook(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getRegistries().getItems();
        this.factory  = plugin.getRarityItemFactory();
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        if (!(event.getKiller() instanceof Player killer)) return;

        // Determine rarity tier based on mob level (rough mapping)
        int level = (int) event.getMob().getLevel();
        RarityTier rarity = levelToRarity(level);

        // 10% chance to drop a rarity item
        if (Math.random() > 0.10 || rarity == null) return;

        RarityItem template = registry.random(rarity);
        if (template == null) return;

        ItemStack drop = factory.build(template, killer.getName(), null, 0);
        org.bukkit.Location loc = event.getMob().getEntity().getBukkitEntity().getLocation();
loc.getWorld().dropItemNaturally(loc, drop);
    }

    private RarityTier levelToRarity(int level) {
        if (level >= 50) return plugin.getRegistries().getRarities().get("legendary");
        if (level >= 30) return plugin.getRegistries().getRarities().get("epic");
        if (level >= 20) return plugin.getRegistries().getRarities().get("rare");
        if (level >= 10) return plugin.getRegistries().getRarities().get("uncommon");
        return plugin.getRegistries().getRarities().get("common");
    }
}
