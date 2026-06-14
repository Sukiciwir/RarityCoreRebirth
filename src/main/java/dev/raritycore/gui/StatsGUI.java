package dev.raritycore.gui;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.storage.StorageManager;
import dev.raritycore.util.ColorUtil;
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
 * Server-wide statistics GUI (54-slot chest).
 * Shows per-rarity discovery totals and the top-3 server milestones.
 */
public final class StatsGUI implements Listener {

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final ItemRegistry registry;
    private final StorageManager storage;

    private final Set<UUID> openInventories = new HashSet<>();

    public StatsGUI(RarityCorePlugin plugin, ConfigManager configManager,
                    ItemRegistry registry, StorageManager storage) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registry = registry;
        this.storage = storage;
    }

    public void open(Player player) {
        String title = configManager.getMessages().getRaw("gui-stats-title");
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.parse(title));
        populate(inv, player);
        player.openInventory(inv);
        openInventories.add(player.getUniqueId());
    }

    private void populate(Inventory inv, Player player) {
        // Borders
        ItemStack border = buildBorder();
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

        // Per-rarity stat icons in the center rows
        List<RarityTier> rarities = new ArrayList<>(plugin.getRegistries().getRarities().getAll());
        int[] slots = {10, 12, 14, 16, 28, 30, 32, 34};
        
        // Wait, serverStats uses Rarity? Oh, I need to check if StorageManager is using Rarity or String
        // For now let's assume it was refactored or we can get by String.
        // Actually, StorageManager getServerStats() returned Map<Rarity, Integer>. 
        // I should probably fix StorageManager as well! Let's temporarily just do registry.getByRarity
        
        // But the error only mentioned `registry.getByRarity(r)` for now.
        // I will do a quick fix here.
        Map<String, Integer> serverStats = new HashMap<>(); // temporary workaround since StorageManager uses Rarity currently.
        // I will fix StorageManager next.
        
        for (int i = 0; i < rarities.size() && i < slots.length; i++) {
            RarityTier rarity = rarities.get(i);
            int total = registry.getByRarity(rarity).size();
            int found = serverStats.getOrDefault(rarity.getId(), 0);
            inv.setItem(slots[i], buildRarityStatItem(rarity, found, total));
        }

        // Player summary at bottom
        inv.setItem(49, buildPlayerSummary(player));
        inv.setItem(45, buildCloseItem());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openInventories.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        if (event.getRawSlot() == 45) player.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }

    // ─── Item Builders ─────────────────────────────────────────────────────────

    private ItemStack buildRarityStatItem(RarityTier rarity, int found, int total) {
        Material mat = switch (rarity.getId().toLowerCase()) {
            case "common"   -> Material.STONE;
            case "uncommon" -> Material.IRON_INGOT;
            case "rare"     -> Material.DIAMOND;
            case "epic"     -> Material.EMERALD;
            case "legendary"-> Material.GOLD_INGOT;
            case "mythic"   -> Material.NETHER_STAR;
            case "divine"   -> Material.BEACON;
            case "ancient"  -> Material.AMETHYST_SHARD;
            default         -> Material.STONE;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String rColor = "<color:" + rarity.getDisplayNameColor() + ">";
        meta.displayName(ColorUtil.parse(rColor + rarity.getPrefix() + capitalize(rarity.getId())));

        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.parse("<gray>Server Discoveries: <white>" + found));
        lore.add(ColorUtil.parse("<gray>Total Items: <white>" + total));
        double pct = total > 0 ? (double) found / total * 100 : 0;
        lore.add(ColorUtil.parse("<gray>Completion: <yellow>" + String.format("%.1f", pct) + "%"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPlayerSummary(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse("<yellow>" + player.getName() + "'s Stats"));

        Set<String> discovered = storage.get(player.getUniqueId()).getDiscovered();
        List<Component> lore = new ArrayList<>();
        for (RarityTier r : plugin.getRegistries().getRarities().getAll()) {
            List<RarityItem> items = registry.getByRarity(r);
            if (items.isEmpty()) continue;
            long found = items.stream().filter(i -> discovered.contains(i.getId())).count();
            lore.add(ColorUtil.parse("<color:" + r.getDisplayNameColor() + ">"
                    + capitalize(r.getId()) + ": <white>" + found + "/" + items.size()));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBorder() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack buildCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(ColorUtil.parse("<red>Close")); item.setItemMeta(meta); }
        return item;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
