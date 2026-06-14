package dev.raritycore.util;

import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Applies a cosmetic enchantment glow to items without adding a real enchantment.
 * Uses {@link ItemMeta#setEnchantmentGlintOverride(Boolean)} which was added in
 * Paper/Bukkit 1.20.5 and is available on all Paper 1.21 builds.
 */
public final class GlowUtil {

    /**
     * Forces the enchantment glint on this item's metadata.
     * The caller must call {@code stack.setItemMeta(meta)} after this.
     */
    public static void applyGlow(@NotNull ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
    }

    /**
     * Removes the forced enchantment glint from this item's metadata.
     */
    public static void removeGlow(@NotNull ItemMeta meta) {
        meta.setEnchantmentGlintOverride(false);
    }

    private GlowUtil() {}
}
