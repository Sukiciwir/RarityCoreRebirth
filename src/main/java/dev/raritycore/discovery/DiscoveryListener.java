package dev.raritycore.discovery;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.storage.StorageManager;
import dev.raritycore.util.ColorUtil;
import dev.raritycore.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Listens for item pickup / fish / mob drop events and triggers
 * discovery tracking + server-wide broadcasts for rare items.
 */
public final class DiscoveryListener implements Listener {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final ItemRegistry registry;
    private final StorageManager storage;

    public DiscoveryListener(RarityCorePlugin plugin,
                             ConfigManager configManager,
                             ItemRegistry registry,
                             StorageManager storage) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registry = registry;
        this.storage = storage;
    }

    // ─── Item Pickup ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        processItem(player, event.getItem().getItemStack());
    }

    // ─── Inventory Click (chest/trade etc.) ────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            processItem(player, cursor);
        }
    }

    // ─── Fishing ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() == null) return;
        if (event.getCaught() instanceof org.bukkit.entity.Item dropped) {
            processItem(event.getPlayer(), dropped.getItemStack());
        }
    }

    // ─── Mob Drop ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        for (ItemStack drop : event.getDrops()) {
            processItem(killer, drop);
        }
    }

    // ─── Core Logic ────────────────────────────────────────────────────────────

    private void processItem(Player player, ItemStack item) {
        if (!ItemUtil.isRarityItem(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String itemId = ItemUtil.getItemId(meta);
        String rarityName = ItemUtil.getRarity(meta);
        if (itemId == null || rarityName == null) return;

        RarityTier rarity = plugin.getRegistries().getRarities().get(rarityName.toLowerCase());
        if (rarity == null) return;

        RarityItem template = registry.get(itemId);

        // ── Set first owner if not yet assigned ──────────────────────────────
        String today = LocalDate.now().format(DATE_FORMAT);
        boolean isFirstOwner = ItemUtil.setFirstOwnerIfAbsent(item, player.getName(), today);

        // ── Record in collection log ──────────────────────────────────────────
        boolean firstDiscovery = !storage.hasDiscovered(player.getUniqueId(), itemId);
        if (firstDiscovery) {
            storage.recordDiscovery(player.getUniqueId(), itemId, rarityName.toLowerCase());
        }

        // ── Notify the player on first discovery ─────────────────────────────
        if (isFirstOwner && template != null) {
            String rColor = "<color:" + rarity.getDisplayNameColor() + ">";
            player.sendMessage(configManager.getMessages().get("discovery-first",
                    "rarity_color", rColor,
                    "item_name", ColorUtil.strip(template.getDisplayName())));
        }

        // ── Broadcast if tier is high enough ─────────────────────────────────
        if (configManager.isBroadcastEnabled()
                && rarity.isBroadcast()
                && rarity.getTier() >= configManager.getBroadcastMinimumRarity()
                && template != null) {
            broadcastDiscovery(player, template, rarity);
        }
    }

    private void broadcastDiscovery(Player player, RarityItem template, RarityTier rarity) {
        String rColor = "<color:" + rarity.getDisplayNameColor() + ">";
        String rawMsg = configManager.getMessages().getRaw("discovery-broadcast",
                "player",        player.getName(),
                "rarity_color",  rColor,
                "rarity_prefix", rarity.getPrefix(),
                "item_name",     ColorUtil.strip(template.getDisplayName()));
        Component broadcast = ColorUtil.parse(rawMsg);
        plugin.getBroadcastManager().queueBroadcast(broadcast);
    }
}
