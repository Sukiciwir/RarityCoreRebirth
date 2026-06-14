package dev.raritycore.gui;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityItemFactory;
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
 * Built-in gacha GUI (54-slot chest). Players spend XP levels to pull a random
 * rarity item. Mythic, Divine & Ancient are exclusively available here or via admin give.
 *
 * Layout:
 *  Row 1-3: decorative glass
 *  Slot 22: x1 Pull (costs N levels)
 *  Slot 24: x10 Pull (costs 10×N levels)
 *  Slot 49: Close
 */
public final class GachaGUI implements Listener {

    private static final int PULL_ONE_SLOT  = 22;
    private static final int PULL_TEN_SLOT  = 24;
    private static final int CLOSE_SLOT     = 49;
    private static final int INFO_SLOT      = 31;

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final ItemRegistry registry;
    private final RarityItemFactory factory;
    private final StorageManager storage;

    private final Set<UUID> openInventories = new HashSet<>();

    public GachaGUI(RarityCorePlugin plugin, ConfigManager configManager,
                    ItemRegistry registry, RarityItemFactory factory,
                    StorageManager storage) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registry = registry;
        this.factory = factory;
        this.storage = storage;
    }

    public void open(Player player) {
        if (!configManager.isGachaEnabled()) {
            player.sendMessage(configManager.getMessages().get("unknown-command"));
            return;
        }
        String title = configManager.getMessages().getRaw("gui-gacha-title");
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.parse(title));
        populate(inv, player);
        player.openInventory(inv);
        openInventories.add(player.getUniqueId());
    }

    private void populate(Inventory inv, Player player) {
        int cost = configManager.getGachaCostLevels();
        int pity = storage.get(player.getUniqueId()).getPity();
        int pityLeft = Math.max(0, configManager.getGachaPityThreshold() - pity);

        // Background
        ItemStack bg = buildBg();
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        // x1 pull
        inv.setItem(PULL_ONE_SLOT, buildPullItem(1, cost, pityLeft));
        // x10 pull
        inv.setItem(PULL_TEN_SLOT, buildPullItem(10, cost * 10, pityLeft));
        // Info
        inv.setItem(INFO_SLOT, buildInfoItem(pity, pityLeft));
        // Close
        inv.setItem(CLOSE_SLOT, buildCloseItem());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openInventories.contains(player.getUniqueId())) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == PULL_ONE_SLOT) {
            doPull(player, 1, event.getView().getTopInventory());
        } else if (slot == PULL_TEN_SLOT) {
            doPull(player, 10, event.getView().getTopInventory());
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }

    // ─── Pull Logic ────────────────────────────────────────────────────────────

    private void doPull(Player player, int pulls, Inventory inv) {
        int cost = configManager.getGachaCostLevels() * pulls;
        if (player.getLevel() < cost) {
            player.sendMessage(configManager.getMessages().get("gacha-no-levels",
                    "levels", String.valueOf(cost)));
            return;
        }

        player.setLevel(player.getLevel() - cost);
        player.sendMessage(configManager.getMessages().get("gacha-pull"));

        for (int i = 0; i < pulls; i++) {
            rollAndGive(player);
        }

        // Refresh GUI
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (openInventories.contains(player.getUniqueId())) {
                populate(inv, player);
            }
        });
    }

    private void rollAndGive(Player player) {
        StorageManager.PlayerData data = storage.get(player.getUniqueId());
        data.incrementPity();

        boolean pityTriggered = data.getPity() >= configManager.getGachaPityThreshold();
        RarityTier rolled = pityTriggered ? forcePityRoll() : weightedRoll();
        if (pityTriggered) {
            data.resetPity();
            player.sendMessage(configManager.getMessages().get("gacha-pity"));
        }

        RarityItem template = registry.random(rolled);
        if (template == null) {
            // Fallback — try lower rarities
            List<RarityTier> allRarities = new ArrayList<>(plugin.getRegistries().getRarities().getAll());
            allRarities.sort(Comparator.comparingInt(RarityTier::getTier));
            
            for (int t = rolled.getTier() - 1; t >= 0 && template == null; t--) {
                final int currentTier = t;
                RarityTier lowerTier = allRarities.stream().filter(r -> r.getTier() == currentTier).findFirst().orElse(null);
                if (lowerTier != null) {
                    template = registry.random(lowerTier);
                }
            }
            if (template == null) return;
        }

        ItemStack result = factory.build(template, player.getName(), null, 0);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(configManager.getMessages().get("gacha-result",
                "rarity_color", "<color:" + rolled.getDisplayNameColor() + ">",
                "item_name", ColorUtil.strip(template.getDisplayName())));
    }

    private RarityTier weightedRoll() {
        Map<String, Double> rates = configManager.getGachaRates();
        double roll = Math.random();
        double cumulative = 0;
        
        List<RarityTier> sorted = new ArrayList<>(plugin.getRegistries().getRarities().getAll());
        sorted.sort((r1, r2) -> Integer.compare(r2.getTier(), r1.getTier())); // highest to lowest
        
        for (RarityTier r : sorted) {
            cumulative += rates.getOrDefault(r.getId().toLowerCase(), 0.0);
            if (roll <= cumulative) return r;
        }
        
        return plugin.getRegistries().getRarities().getAll().stream().min(Comparator.comparingInt(RarityTier::getTier)).orElse(null);
    }

    /** Pity always gives at least LEGENDARY. (Tier >= 4 assumed or ID fallback) */
    private RarityTier forcePityRoll() {
        Map<String, Double> rates = configManager.getGachaRates();
        double roll = Math.random();
        
        RarityTier legendary = plugin.getRegistries().getRarities().get("legendary");
        RarityTier mythic = plugin.getRegistries().getRarities().get("mythic");
        RarityTier divine = plugin.getRegistries().getRarities().get("divine");
        RarityTier ancient = plugin.getRegistries().getRarities().get("ancient");

        List<RarityTier> highTier = new ArrayList<>();
        if (ancient != null) highTier.add(ancient);
        if (divine != null) highTier.add(divine);
        if (mythic != null) highTier.add(mythic);
        if (legendary != null) highTier.add(legendary);
        
        if (highTier.isEmpty()) return weightedRoll(); // fallback if config completely changed
        
        double total = 0.0;
        for (RarityTier r : highTier) {
            total += rates.getOrDefault(r.getId().toLowerCase(), 0.05);
        }
        
        double norm = roll * total;
        double cumulative = 0;
        for (RarityTier r : highTier) {
            cumulative += rates.getOrDefault(r.getId().toLowerCase(), 0.05);
            if (norm <= cumulative) return r;
        }
        return highTier.get(highTier.size() - 1);
    }

    // ─── Item Builders ─────────────────────────────────────────────────────────

    private ItemStack buildPullItem(int pulls, int totalCost, int pityLeft) {
        Material mat = pulls == 1 ? Material.ENDER_PEARL : Material.ENDER_EYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse("<gold>✦ Pull x" + pulls));
        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.parse("<gray>Cost: <yellow>" + totalCost + " XP Levels"));
        lore.add(ColorUtil.parse("<gray>Pity in: <aqua>" + pityLeft + " pulls"));
        lore.add(Component.empty());
        lore.add(ColorUtil.parse("<dark_gray>Possible rewards:"));
        
        List<RarityTier> allRarities = new ArrayList<>(plugin.getRegistries().getRarities().getAll());
        allRarities.sort(Comparator.comparingInt(RarityTier::getTier));
        
        for (RarityTier r : allRarities) {
            double rate = configManager.getGachaRates().getOrDefault(r.getId().toLowerCase(), 0.0) * 100;
            lore.add(ColorUtil.parse("  <color:" + r.getDisplayNameColor() + ">"
                    + r.getPrefix() + capitalize(r.getId())
                    + " <dark_gray>(" + String.format("%.2f", rate) + "%)"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem(int pity, int pityLeft) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse("<aqua>Gacha Info"));
        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.parse("<gray>Current pity: <yellow>" + pity));
        lore.add(ColorUtil.parse("<gray>Pulls until pity: <aqua>" + pityLeft));
        lore.add(Component.empty());
        lore.add(ColorUtil.parse("<dark_gray>Pity guarantees at least Legendary."));
        lore.add(ColorUtil.parse("<dark_gray>Mythic, Divine & Ancient: exclusive to gacha/admin."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBg() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
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
