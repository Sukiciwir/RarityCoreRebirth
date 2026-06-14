package dev.raritycore.generation;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GenerationListener implements Listener {

    private final RarityCorePlugin plugin;

    public GenerationListener(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        // getCurrentItem() itu udah bener — dia return item di result slot crafting table.
        // e.getRecipe().getResult() sebenernya return template recipe-nya (bisa beda stack size),
        // jadi getCurrentItem() lebih akurat karena reflect actual item yang bakal diambil.
        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;
        if (!isEligibleMaterial(result.getType())) return;

        // FIX #3: isRarityItem check dipindah ke sini (level listener),
        // lebih efisien karena kita skip semua logic di bawah kalau udah rarity item,
        // daripada baru dicek di dalam transformToRarity() setelah semua kondisi lolos.
        if (ItemUtil.isRarityItem(result)) return;

        Player player = (Player) e.getWhoClicked();

        // FIX #1: Handle shift-click secara terpisah.
        // Kalau shift-click, Bukkit bypass setCurrentItem() dan langsung dump semua
        // hasil craft ke inventory player — jadi transform kita diabaikan total.
        // Solusinya: cancel event, hitung jumlah craft, transform manual, kasih ke inventory.
        if (e.isShiftClick()) {
            e.setCancelled(true);

            int craftAmount = calculateCraftAmount(e.getInventory().getMatrix(), result.getAmount());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Loop sebanyak jumlah craft yang bisa dilakukan
                for (int i = 0; i < craftAmount; i++) {
                    ItemStack converted = transformToRarity(result.clone(), player.getName());
                    // Kalau inventory penuh, drop ke ground biar item nggak hilang
                    if (!player.getInventory().addItem(converted).isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), converted);
                    }
                    consumeIngredients(e.getInventory().getMatrix(), e.getInventory());
                }
            });

        } else {
            // Normal click: setCurrentItem() masih bekerja normal
            ItemStack converted = transformToRarity(result.clone(), player.getName());
            e.setCurrentItem(converted);
        }
    }

    // FIX #2: Tambah ignoreCancelled = true.
    // EntityDeathEvent bisa di-cancel plugin lain (misal MythicMobs, protection plugin, dsb).
    // Kalau kita tetap proses drop dari entity yang "cancelled", bisa bikin behavior aneh
    // atau konflik sama plugin lain. Konsisten pake ignoreCancelled = true.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        for (int i = 0; i < e.getDrops().size(); i++) {
            ItemStack drop = e.getDrops().get(i);

            // FIX #3 (sama kayak onCraft): isRarityItem check di level listener
            if (drop == null || !isEligibleMaterial(drop.getType())) continue;
            if (ItemUtil.isRarityItem(drop)) continue;

            ItemStack converted = transformToRarity(drop, null);
            e.getDrops().set(i, converted);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH && e.getCaught() instanceof org.bukkit.entity.Item caughtItem) {
            ItemStack drop = caughtItem.getItemStack();

            // FIX #3 (konsisten): isRarityItem check di level listener
            if (!isEligibleMaterial(drop.getType())) return;
            if (ItemUtil.isRarityItem(drop)) return;

            ItemStack converted = transformToRarity(drop, e.getPlayer().getName());
            caughtItem.setItemStack(converted);
        }
    }

    // Hitung berapa kali craft bisa dilakukan berdasarkan jumlah ingredient paling sedikit
    private int calculateCraftAmount(ItemStack[] matrix, int resultAmount) {
        int minCount = Integer.MAX_VALUE;
        for (ItemStack ingredient : matrix) {
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                minCount = Math.min(minCount, ingredient.getAmount());
            }
        }
        return minCount == Integer.MAX_VALUE ? 0 : minCount;
    }

    // Kurangi ingredient secara manual karena kita cancel event-nya
    private void consumeIngredients(ItemStack[] matrix, org.bukkit.inventory.CraftingInventory inv) {
        for (int i = 0; i < matrix.length; i++) {
            ItemStack ingredient = matrix[i];
            if (ingredient == null || ingredient.getType() == Material.AIR) continue;

            if (ingredient.getAmount() <= 1) {
                inv.setItem(i + 1, null); // +1 karena slot 0 = result slot
            } else {
                ingredient.setAmount(ingredient.getAmount() - 1);
                inv.setItem(i + 1, ingredient);
            }
        }
    }

    private boolean isEligibleMaterial(Material material) {
        String name = material.name();

        if (name.startsWith("WOODEN_") || name.startsWith("STONE_") || name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_") || name.startsWith("TURTLE_")) {
            return false;
        }

        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")
            || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
            || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("FISHING_ROD") || name.equals("ELYTRA")
            || name.equals("MACE") || name.equals("TRIDENT");
    }

    private ItemStack transformToRarity(ItemStack original, String ownerName) {
        // FIX #3: Check ini dihapus dari sini karena udah dihandle di level listener masing-masing
        RarityTier tier = plugin.getGenerationManager().rollRarity();

        String id = "generated_" + original.getType().name().toLowerCase();
        String displayName = "<white>" + capitalize(original.getType().name().replace("_", " "));

        RarityItem dummyTemplate = new RarityItem(
            id,
            original.getType(),
            tier,
            displayName,
            List.of(),
            null,
            true
        );

        ItemStack newStack = plugin.getRarityItemFactory().build(dummyTemplate, ownerName, null, 0);
        newStack.addUnsafeEnchantments(original.getEnchantments());

        if (original.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable originalDamageMeta
            && newStack.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable newDamageMeta) {
            newDamageMeta.setDamage(originalDamageMeta.getDamage());
            newStack.setItemMeta(newDamageMeta);
        }

        newStack.setAmount(original.getAmount());
        return newStack;
    }

    private String capitalize(String str) {
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}