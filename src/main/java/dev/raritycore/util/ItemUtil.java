package dev.raritycore.util;

import dev.raritycore.RarityCorePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper for reading/writing RarityCore PDC (PersistentDataContainer) entries
 * on ItemStacks. All keys are pre-allocated as constants for consistency.
 */
public final class ItemUtil {

    // ── PDC Keys ──────────────────────────────────────────────────────────────

    public static NamespacedKey KEY_RARITY;
    public static NamespacedKey KEY_ITEM_ID;
    public static NamespacedKey KEY_QUALITY;
    public static NamespacedKey KEY_AFFIX;
    public static NamespacedKey KEY_SET_ID;
    public static NamespacedKey KEY_FIRST_OWNER;
    public static NamespacedKey KEY_FIRST_DATE;
    public static NamespacedKey KEY_UPGRADE_STONE;   // marks an upgrade stone item
    public static NamespacedKey KEY_STONE_TIER;      // which upgrade tier this stone handles
    public static NamespacedKey KEY_FRAGMENT;        // marks a salvage fragment item
    public static NamespacedKey KEY_TRAITS;          // CSV of serialized TraitInstances

    /** Must be called once from the main plugin class before any item operations. */
    public static void init(@NotNull RarityCorePlugin plugin) {
        KEY_RARITY       = new NamespacedKey(plugin, "rarity");
        KEY_ITEM_ID      = new NamespacedKey(plugin, "item_id");
        KEY_QUALITY      = new NamespacedKey(plugin, "quality");
        KEY_AFFIX        = new NamespacedKey(plugin, "affix");
        KEY_SET_ID       = new NamespacedKey(plugin, "set_id");
        KEY_FIRST_OWNER  = new NamespacedKey(plugin, "first_owner");
        KEY_FIRST_DATE   = new NamespacedKey(plugin, "first_date");
        KEY_UPGRADE_STONE= new NamespacedKey(plugin, "upgrade_stone");
        KEY_STONE_TIER   = new NamespacedKey(plugin, "stone_tier");
        KEY_FRAGMENT     = new NamespacedKey(plugin, "fragment");
        KEY_TRAITS       = new NamespacedKey(plugin, "traits");
    }

    // ── Read Helpers ──────────────────────────────────────────────────────────

    /** Returns true if the item has the raritycore rarity PDC tag. */
    public static boolean isRarityItem(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY_RARITY, PersistentDataType.STRING);
    }

    /** Returns true if the item is a RarityCore upgrade stone. */
    public static boolean isUpgradeStone(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY_UPGRADE_STONE, PersistentDataType.BYTE);
    }

    /** Returns true if the item is a RarityCore salvage fragment. */
    public static boolean isFragment(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY_FRAGMENT, PersistentDataType.STRING);
    }

    @Nullable
    public static String getRarity(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(KEY_RARITY, PersistentDataType.STRING);
    }

    @Nullable
    public static String getItemId(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(KEY_ITEM_ID, PersistentDataType.STRING);
    }

    @Nullable
    public static Integer getQuality(@NotNull ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_QUALITY, PersistentDataType.INTEGER)) {
            return pdc.get(KEY_QUALITY, PersistentDataType.INTEGER);
        } else if (pdc.has(KEY_QUALITY, PersistentDataType.STRING)) {
            String str = pdc.get(KEY_QUALITY, PersistentDataType.STRING);
            if (str == null) return 50;
            if (str.equals("masterwork")) return 95;
            if (str.equals("flawless")) return 80;
            if (str.equals("superior")) return 60;
            if (str.equals("fine")) return 40;
            if (str.equals("normal")) return 20;
            if (str.equals("poor")) return 10;
            if (str.equals("broken")) return 0;
            return 50;
        }
        return null;
    }

    @Nullable
    public static String getAffix(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(KEY_AFFIX, PersistentDataType.STRING);
    }

    @Nullable
    public static String getSetId(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(KEY_SET_ID, PersistentDataType.STRING);
    }

    @Nullable
    public static String getFirstOwner(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(KEY_FIRST_OWNER, PersistentDataType.STRING);
    }

    @Nullable
    public static String getFirstDate(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(KEY_FIRST_DATE, PersistentDataType.STRING);
    }

    @Nullable
    public static String getStoneTier(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(KEY_STONE_TIER, PersistentDataType.STRING);
    }
    
    @NotNull
    public static java.util.List<dev.raritycore.trait.TraitInstance> getTraits(@NotNull ItemMeta meta) {
        String data = meta.getPersistentDataContainer().get(KEY_TRAITS, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) return new java.util.ArrayList<>();
        
        java.util.List<dev.raritycore.trait.TraitInstance> list = new java.util.ArrayList<>();
        for (String part : data.split(",")) {
            dev.raritycore.trait.TraitInstance inst = dev.raritycore.trait.TraitInstance.deserialize(part);
            if (inst != null) list.add(inst);
        }
        return list;
    }
    
    public static void setTraits(@NotNull ItemMeta meta, @NotNull java.util.List<dev.raritycore.trait.TraitInstance> traits) {
        if (traits.isEmpty()) {
            meta.getPersistentDataContainer().remove(KEY_TRAITS);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < traits.size(); i++) {
            sb.append(traits.get(i).serialize());
            if (i < traits.size() - 1) sb.append(",");
        }
        meta.getPersistentDataContainer().set(KEY_TRAITS, PersistentDataType.STRING, sb.toString());
    }

    /** Sets the first owner on an existing item if not already set. Returns true if it was set. */
    public static boolean setFirstOwnerIfAbsent(@NotNull ItemStack stack, @NotNull String ownerName,
                                                 @NotNull String date) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(KEY_FIRST_OWNER, PersistentDataType.STRING)) return false;
        pdc.set(KEY_FIRST_OWNER, PersistentDataType.STRING, ownerName);
        pdc.set(KEY_FIRST_DATE,  PersistentDataType.STRING, date);
        stack.setItemMeta(meta);
        return true;
    }

    private ItemUtil() {}
}
