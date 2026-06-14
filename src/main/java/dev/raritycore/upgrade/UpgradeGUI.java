package dev.raritycore.upgrade;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItemFactory;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.util.ColorUtil;
import dev.raritycore.util.GlowUtil;
import dev.raritycore.util.ItemUtil;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Custom 54-slot upgrade GUI.
 *
 * Layout:
 *  - Slot 20: Input rarity item
 *  - Slot 24: Upgrade stone
 *  - Slot 31: Result preview (click to execute)
 *  - Slot 49: Close
 *  - Everything else: border
 */
public final class UpgradeGUI implements Listener {

    private static final int ITEM_SLOT   = 20;
    private static final int STONE_SLOT  = 24;
    private static final int RESULT_SLOT = 31;
    private static final int CLOSE_SLOT  = 49;
    private static final int INFO_SLOT   = 22;  // Arrow showing direction

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final ItemRegistry registry;
    private final RarityItemFactory factory;

    private final Set<UUID> openInventories = new HashSet<>();

    public UpgradeGUI(RarityCorePlugin plugin, ConfigManager configManager,
                      ItemRegistry registry, RarityItemFactory factory) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registry = registry;
        this.factory = factory;
    }

    public void open(Player player) {
        String title = configManager.getMessages().getRaw("gui-upgrade-title");
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.parse(title));
        fillBorders(inv);
        inv.setItem(INFO_SLOT,  buildArrowItem());
        inv.setItem(RESULT_SLOT, buildEmptyResult());
        inv.setItem(CLOSE_SLOT,  buildCloseItem());
        player.openInventory(inv);
        openInventories.add(player.getUniqueId());
    }

    // ─── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openInventories.contains(player.getUniqueId())) return;

        int slot = event.getRawSlot();

        // Allow interaction only in item and stone slots
        if (slot == ITEM_SLOT || slot == STONE_SLOT) {
            // Let vanilla handle placing/taking; re-evaluate result after 1 tick
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    updateResult(player, event.getView().getTopInventory()));
            return;
        }

        event.setCancelled(true);

        if (slot == RESULT_SLOT) {
            attemptUpgrade(player, event.getView().getTopInventory());
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!openInventories.contains(player.getUniqueId())) return;
        openInventories.remove(player.getUniqueId());

        // Return any items left in the GUI
        Inventory inv = event.getInventory();
        returnItem(player, inv, ITEM_SLOT);
        returnItem(player, inv, STONE_SLOT);
    }

    // ─── Core Logic ────────────────────────────────────────────────────────────

    private void updateResult(Player player, Inventory inv) {
        ItemStack inputItem = inv.getItem(ITEM_SLOT);
        ItemStack stone     = inv.getItem(STONE_SLOT);

        if (!ItemUtil.isRarityItem(inputItem) || !ItemUtil.isUpgradeStone(stone)) {
            inv.setItem(RESULT_SLOT, buildEmptyResult());
            return;
        }

        ItemMeta inputMeta = inputItem.getItemMeta();
        if (inputMeta == null) return;

        String rarityName = ItemUtil.getRarity(inputMeta);
        if (rarityName == null) return;
        
        RarityTier current = plugin.getRegistries().getRarities().get(rarityName.toLowerCase());
        if (current == null) return;

        // Check stone tier matches current rarity
        ItemMeta stoneMeta = stone.getItemMeta();
        if (stoneMeta == null) return;
        String stoneTier = ItemUtil.getStoneTier(stoneMeta);
        if (!current.getId().equalsIgnoreCase(stoneTier)) {
            inv.setItem(RESULT_SLOT, buildWrongStoneItem());
            return;
        }

        RarityTier next = getNextTier(current);
        if (next == null) {
            inv.setItem(RESULT_SLOT, buildMaxTierItem());
            return;
        }

        // Build preview
        String itemId = ItemUtil.getItemId(inputMeta);
        if (itemId == null) return;
        // Find same item ID but upgraded rarity — or just build a preview display
        inv.setItem(RESULT_SLOT, buildPreviewItem(inputItem, current, next));
    }

    private void attemptUpgrade(Player player, Inventory inv) {
        ItemStack inputItem = inv.getItem(ITEM_SLOT);
        ItemStack stone     = inv.getItem(STONE_SLOT);

        if (!ItemUtil.isRarityItem(inputItem) || !ItemUtil.isUpgradeStone(stone)) {
            player.sendMessage(configManager.getMessages().get("upgrade-no-item"));
            return;
        }

        ItemMeta inputMeta = inputItem.getItemMeta();
        if (inputMeta == null) return;

        String rarityName = ItemUtil.getRarity(inputMeta);
        if (rarityName == null) return;

        RarityTier current = plugin.getRegistries().getRarities().get(rarityName.toLowerCase());
        if (current == null) return;

        // Stone tier check
        ItemMeta stoneMeta = stone.getItemMeta();
        if (stoneMeta == null) return;
        String stoneTier = ItemUtil.getStoneTier(stoneMeta);
        if (!current.getId().equalsIgnoreCase(stoneTier)) {
            player.sendMessage(configManager.getMessages().get("upgrade-wrong-stone"));
            return;
        }

        RarityTier next = getNextTier(current);
        if (next == null) {
            player.sendMessage(configManager.getMessages().get("upgrade-max-tier"));
            return;
        }

        // Consume stone
        inv.setItem(STONE_SLOT, null);

        // Roll success chance
        String key = current.getId().toUpperCase() + "_TO_" + next.getId().toUpperCase();
        double chance = configManager.getUpgradeSuccessChance(key);
        boolean success = Math.random() < chance;

        if (success) {
            // Build upgraded item
            String firstOwner = ItemUtil.getFirstOwner(inputMeta);
            Integer quality = ItemUtil.getQuality(inputMeta);

            // Find the same item template — upgrade its rarity in-place
            ItemStack upgraded = upgradeItem(inputItem, next, firstOwner, quality != null ? quality : 50);
            inv.setItem(ITEM_SLOT, null);
            inv.setItem(RESULT_SLOT, buildEmptyResult());

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(upgraded);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            player.sendMessage(configManager.getMessages().get("upgrade-success",
                    "item", inputItem.getType().name(),
                    "new_rarity", "<color:" + next.getDisplayNameColor() + ">"
                            + capitalize(next.getId())));
        } else {
            String failOutcome = configManager.getUpgradeFailureOutcome();
            if ("DOWNGRADE".equalsIgnoreCase(failOutcome)) {
                RarityTier prev = getPrevTier(current);
                if (prev != current && prev != null) {
                    ItemStack downgraded = upgradeItem(inputItem, prev, null, 50);
                    inv.setItem(ITEM_SLOT, downgraded);
                }
            }
            player.sendMessage(configManager.getMessages().get("upgrade-fail"));
            inv.setItem(RESULT_SLOT, buildEmptyResult());
        }
    }
    
    private RarityTier getNextTier(RarityTier current) {
        List<RarityTier> all = new ArrayList<>(plugin.getRegistries().getRarities().getAll());
        all.sort(Comparator.comparingInt(RarityTier::getTier));
        boolean found = false;
        for (RarityTier r : all) {
            if (found) {
                if (r.isUpgradeableTo()) return r;
            } else if (r.getId().equalsIgnoreCase(current.getId())) {
                found = true;
            }
        }
        return null;
    }
    
    private RarityTier getPrevTier(RarityTier current) {
        List<RarityTier> all = new ArrayList<>(plugin.getRegistries().getRarities().getAll());
        all.sort(Comparator.comparingInt(RarityTier::getTier));
        RarityTier last = null;
        for (RarityTier r : all) {
            if (r.getId().equalsIgnoreCase(current.getId())) {
                return last != null ? last : current;
            }
            last = r;
        }
        return current;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates an upgraded copy of the item by rewriting its PDC rarity and
     * rebuilding display name and lore according to the new rarity config.
     */
    private ItemStack upgradeItem(ItemStack original, RarityTier newRarity,
                                  String firstOwner, int quality) {
        ItemStack copy = original.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;

        // Update PDC rarity
        meta.getPersistentDataContainer()
                .set(ItemUtil.KEY_RARITY, PersistentDataType.STRING, newRarity.getId());

        // Update display name (replace old rarity color / prefix)
        String baseName = copy.getType().name()
                .replace("_", " ")
                .toLowerCase();
        String newName = "<color:" + newRarity.getDisplayNameColor() + ">"
                + newRarity.getPrefix()
                + capitalize(newRarity.getId()) + " "
                + capitalizeWords(baseName) + "</color>";
        meta.displayName(ColorUtil.parse(newName));

        // Apply/remove glow
        if (newRarity.isGlow()) GlowUtil.applyGlow(meta);

        // Update quality PDC
        meta.getPersistentDataContainer()
                .set(ItemUtil.KEY_QUALITY, PersistentDataType.INTEGER, quality);

        copy.setItemMeta(meta);
        return copy;
    }

    private void fillBorders(Inventory inv) {
        ItemStack border = buildBorder();
        for (int i = 0; i < 54; i++) {
            if (i != ITEM_SLOT && i != STONE_SLOT && i != RESULT_SLOT
                    && i != CLOSE_SLOT && i != INFO_SLOT) {
                inv.setItem(i, border);
            }
        }
    }

    private void returnItem(Player player, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && !item.getType().isAir()) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            inv.setItem(slot, null);
        }
    }

    private ItemStack buildBorder() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack buildArrowItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse("<gray>Item ▶ Stone ▶ Result"));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("<dark_gray>Place item + upgrade stone"));
            lore.add(ColorUtil.parse("<dark_gray>then click the result to upgrade."));
            lore.add(Component.empty());
            lore.add(ColorUtil.parse("<gold>✦ Mythic, Divine & Ancient cannot be"));
            lore.add(ColorUtil.parse("<gold>  upgraded to — gacha/admin only."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildEmptyResult() {
        ItemStack item = new ItemStack(Material.GRAY_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse("<gray>Awaiting input..."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildPreviewItem(ItemStack original, RarityTier current, RarityTier next) {
        ItemStack preview = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = preview.getItemMeta();
        if (meta == null) return preview;
        String nextColor = "<color:" + next.getDisplayNameColor() + ">";
        meta.displayName(ColorUtil.parse("<green>✔ Click to Upgrade!"));
        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.parse("<gray>" + capitalize(current.getId())
                + " → " + nextColor + capitalize(next.getId())));
        String key = current.getId().toUpperCase() + "_TO_" + next.getId().toUpperCase();
        int pct = (int) (configManager.getUpgradeSuccessChance(key) * 100);
        lore.add(ColorUtil.parse("<dark_gray>Success Chance: <yellow>" + pct + "%"));
        meta.lore(lore);
        preview.setItemMeta(meta);
        return preview;
    }

    private ItemStack buildWrongStoneItem() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse("<red>Wrong Upgrade Stone!"));
        meta.lore(List.of(ColorUtil.parse("<dark_gray>The stone must match the item's rarity.")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildMaxTierItem() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse("<gold>Maximum Upgradeable Tier"));
        meta.lore(List.of(
                ColorUtil.parse("<dark_gray>This item is already at the max upgradeable tier."),
                ColorUtil.parse("<dark_gray>Some tiers require the gacha.")));
        item.setItemMeta(meta);
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

    private static String capitalizeWords(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) sb.append(capitalize(word)).append(" ");
        }
        return sb.toString().trim();
    }
}
