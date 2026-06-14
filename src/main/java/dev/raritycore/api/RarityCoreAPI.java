package dev.raritycore.api;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityItemFactory;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.storage.StorageManager;
import dev.raritycore.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Public API surface for other plugins to interact with RarityCore.
 *
 * <p>Usage:
 * <pre>
 *   RarityCoreAPI api = RarityCoreAPI.get();
 *   ItemStack legendary = api.buildItem("legendary_netherite_sword", null, null, 0);
 * </pre>
 */
public final class RarityCoreAPI {

    private static RarityCoreAPI instance;

    private final RarityCorePlugin plugin;
    private final ItemRegistry registry;
    private final RarityItemFactory factory;
    private final StorageManager storage;

    private RarityCoreAPI(@NotNull RarityCorePlugin plugin) {
        this.plugin   = plugin;
        this.registry = plugin.getRegistries().getItems();
        this.factory  = plugin.getRarityItemFactory();
        this.storage  = plugin.getStorageManager();
    }

    /** Returns the singleton API instance. Throws if RarityCore is not loaded. */
    @NotNull
    public static RarityCoreAPI get() {
        if (instance == null)
            throw new IllegalStateException("RarityCore is not loaded or has been disabled.");
        return instance;
    }

    /** Called internally by the plugin on enable. */
    public static void init(@NotNull RarityCorePlugin plugin) {
        instance = new RarityCoreAPI(plugin);
    }

    /** Called internally by the plugin on disable. */
    public static void shutdown() { instance = null; }

    // ─── Item Management ───────────────────────────────────────────────────────

    /**
     * Builds an ItemStack from the given registry ID.
     *
     * @param itemId      registry ID from items.yml
     * @param firstOwner  player name to record as first owner, or null
     * @param forceAffix  specific affix ID to apply, or null for random
     * @param forceQuality 0 = random, 1-100 = fixed quality
     */
    @Nullable
    public ItemStack buildItem(@NotNull String itemId, @Nullable String firstOwner,
                               @Nullable String forceAffix, int forceQuality) {
        RarityItem template = registry.get(itemId);
        if (template == null) return null;
        return factory.build(template, firstOwner, forceAffix, forceQuality);
    }

    /** Returns all registered rarity item IDs. */
    @NotNull
    public Set<String> getItemIds() {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        registry.getAll().forEach(i -> ids.add(i.getId()));
        return ids;
    }

    /** Returns the rarity of an ItemStack, or null if it's not a rarity item. */
    @Nullable
    public RarityTier getRarity(@NotNull ItemStack stack) {
        if (!ItemUtil.isRarityItem(stack)) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        return plugin.getRegistries().getRarities().get(ItemUtil.getRarity(meta));
    }

    /** Returns true if the item is a RarityCore item. */
    public boolean isRarityItem(@NotNull ItemStack stack) {
        return ItemUtil.isRarityItem(stack);
    }

    // ─── Player Data ───────────────────────────────────────────────────────────

    /** Returns all item IDs discovered by the player. */
    @NotNull
    public Set<String> getDiscovered(@NotNull Player player) {
        return storage.get(player.getUniqueId()).getDiscovered();
    }

    /** Returns the number of rarity items of a given tier the player has found. */
    public int getDiscoveryCount(@NotNull Player player, @NotNull String rarityId) {
        return storage.get(player.getUniqueId()).getDiscoveryCount(rarityId);
    }

    /** Returns the player's current gacha pity counter. */
    public int getPity(@NotNull Player player) {
        return storage.get(player.getUniqueId()).getPity();
    }
}
