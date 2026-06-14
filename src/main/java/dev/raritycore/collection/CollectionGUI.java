package dev.raritycore.collection;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.storage.StorageManager;
import dev.raritycore.util.ColorUtil;
import dev.raritycore.util.GlowUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Chest-based GUI displaying the player's collection log.
 * Shows one item per registry entry; green = collected, gray skull = not collected.
 * Supports pagination (9 items per row × 4 content rows = 36 per page).
 */
public final class CollectionGUI implements Listener {

    private static final int PAGE_SIZE = 36; // slots 0-35 in a 54-slot chest
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int CLOSE_SLOT = 49;

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final ItemRegistry registry;
    private final StorageManager storage;

    /** Tracks which page each player is on. */
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    /** Marks inventories managed by this GUI. */
    private final Set<UUID> openInventories = new HashSet<>();

    public CollectionGUI(RarityCorePlugin plugin, ConfigManager configManager,
                         ItemRegistry registry, StorageManager storage) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registry = registry;
        this.storage = storage;
    }

    /** Opens the collection GUI for a player. */
    public void open(Player player, int page) {
        List<RarityItem> allItems = new ArrayList<>(registry.getAll());
        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = configManager.getMessages().getRaw("gui-collection-title");
        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.parse(title + " <dark_gray>(" + (page + 1) + "/" + totalPages + ")"));

        // Fill content slots
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allItems.size());
        Set<String> discovered = storage.get(player.getUniqueId()).getDiscovered();

        for (int i = start; i < end; i++) {
            RarityItem template = allItems.get(i);
            boolean found = discovered.contains(template.getId());
            inv.setItem(i - start, buildCollectionSlot(template, found));
        }

        // Navigation
        inv.setItem(PREV_SLOT, buildNavItem(Material.ARROW, "<gray>◀ Previous Page", page > 0));
        inv.setItem(NEXT_SLOT, buildNavItem(Material.ARROW, "<gray>Next Page ▶", page < totalPages - 1));
        inv.setItem(CLOSE_SLOT, buildCloseItem());

        // Summary row (bottom row, slots 45-53 already used for nav)
        inv.setItem(46, buildSummaryItem(player));

        player.openInventory(inv);
        playerPages.put(player.getUniqueId(), page);
        openInventories.add(player.getUniqueId());
    }

    // ─── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openInventories.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == PREV_SLOT && currentPage > 0) {
            open(player, currentPage - 1);
        } else if (slot == NEXT_SLOT) {
            open(player, currentPage + 1);
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }

    // ─── Item Builders ─────────────────────────────────────────────────────────

    private ItemStack buildCollectionSlot(RarityItem template, boolean discovered) {
        ItemStack stack = discovered
                ? new ItemStack(template.getMaterial())
                : new ItemStack(Material.GRAY_DYE);

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        RarityTier tier = template.getRarity();
        String rColor = "<color:" + tier.getDisplayNameColor() + ">";

        if (discovered) {
            meta.displayName(ColorUtil.parse(template.getDisplayName()));
            if (tier.isGlow()) GlowUtil.applyGlow(meta);
        } else {
            meta.displayName(ColorUtil.parse("<dark_gray>???"));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.parse(rColor + tier.getPrefix()
                + capitalize(tier.getId())));
        if (!discovered) {
            lore.add(ColorUtil.parse("<dark_gray><i>Not yet discovered</i>"));
        } else {
            lore.add(ColorUtil.parse("<green>✔ Discovered"));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildNavItem(Material mat, String name, boolean active) {
        ItemStack stack = new ItemStack(active ? mat : Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(ColorUtil.parse(active ? name : "<dark_gray>No more pages"));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildCloseItem() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(ColorUtil.parse("<red>Close"));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildSummaryItem(Player player) {
        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(ColorUtil.parse("<gold>Collection Summary"));

        Set<String> discovered = storage.get(player.getUniqueId()).getDiscovered();
        List<Component> lore = new ArrayList<>();
        for (RarityTier r : plugin.getRegistries().getRarities().getAll()) {
            List<RarityItem> items = registry.getByRarity(r);
            if (items.isEmpty()) continue;
            long found = items.stream().filter(i -> discovered.contains(i.getId())).count();
            String rColor = "<color:" + r.getDisplayNameColor() + ">";
            lore.add(ColorUtil.parse(rColor + capitalize(r.getId()) + ": <white>"
                    + found + "/" + items.size()));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
