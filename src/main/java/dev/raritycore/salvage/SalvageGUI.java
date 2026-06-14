package dev.raritycore.salvage;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.util.ColorUtil;
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

import java.util.*;

/**
 * Inventory GUI for the salvage system.
 *
 * <pre>
 * [  border  ][  border  ][ ITEM IN  ][ border  ][  border  ]
 * [  border  ][ RESULT1  ][ RESULT2  ][ RESULT3 ][  border  ]
 * [  border  ][  border  ][ CONFIRM  ][ border  ][  border  ]
 * </pre>
 *
 * Layout (27-slot chest):
 *  - Slot 11: Input item
 *  - Slot 15: Confirm / Salvage button
 *  - Slots 18-26: Preview output
 */
public final class SalvageGUI implements Listener {

    private static final int INPUT_SLOT   = 11;
    private static final int CONFIRM_SLOT = 13;
    private static final int CANCEL_SLOT  = 15;
    private static final int[] BORDER_SLOTS =
            {0,1,2,3,4,5,6,7,8, 9,10,12,14,16,17, 18,19,20,21,22,23,24,25,26};

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final SalvageManager salvageManager;

    private final Set<UUID> openInventories = new HashSet<>();

    public SalvageGUI(RarityCorePlugin plugin, ConfigManager configManager,
                      SalvageManager salvageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.salvageManager = salvageManager;
    }

    public void open(Player player) {
        String title = configManager.getMessages().getRaw("gui-salvage-title");
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtil.parse(title));

        // Fill borders
        ItemStack border = buildBorder();
        for (int slot : BORDER_SLOTS) {
            inv.setItem(slot, border);
        }

        // Confirm / cancel
        inv.setItem(CONFIRM_SLOT, buildConfirmItem(false));
        inv.setItem(CANCEL_SLOT,  buildCancelItem());

        player.openInventory(inv);
        openInventories.add(player.getUniqueId());
    }

    // ─── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openInventories.contains(player.getUniqueId())) return;

        // Allow placing item in input slot; cancel everything else
        int slot = event.getRawSlot();
        if (slot == INPUT_SLOT) return; // let them place/take
        event.setCancelled(true);

        if (slot == CONFIRM_SLOT) {
            doSalvage(player, event.getView().getTopInventory());
        } else if (slot == CANCEL_SLOT) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!openInventories.contains(player.getUniqueId())) return;
        openInventories.remove(player.getUniqueId());

        // Return any item in the input slot
        ItemStack input = event.getInventory().getItem(INPUT_SLOT);
        if (input != null && !input.getType().isAir()) {
            player.getInventory().addItem(input);
            event.getInventory().setItem(INPUT_SLOT, null);
        }
    }

    // ─── Logic ─────────────────────────────────────────────────────────────────

    private void doSalvage(Player player, Inventory inv) {
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (!ItemUtil.isRarityItem(input)) {
            player.sendMessage(configManager.getMessages().get("salvage-no-item"));
            return;
        }

        ItemMeta meta = input.getItemMeta();
        if (meta == null) return;
        String rarityName = ItemUtil.getRarity(meta);
        if (rarityName == null) return;

        RarityTier rarity = plugin.getRegistries().getRarities().get(rarityName.toLowerCase());
        if (rarity == null) return;

        // Remove item
        inv.setItem(INPUT_SLOT, null);

        // Give fragments
        List<ItemStack> fragments = salvageManager.computeSalvageOutput(rarity);
        for (ItemStack frag : fragments) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(frag);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        player.sendMessage(configManager.getMessages().get("salvage-success"));
        player.closeInventory();
    }

    // ─── Item Builders ─────────────────────────────────────────────────────────

    private ItemStack buildBorder() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildConfirmItem(boolean hasItem) {
        ItemStack item = new ItemStack(hasItem ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(hasItem
                    ? "<green>✔ Salvage Item"
                    : "<gray>Place an item to salvage"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildCancelItem() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse("<red>✘ Cancel"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
