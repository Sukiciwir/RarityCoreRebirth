package dev.raritycore.rarity;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable definition of a rarity item loaded from items.yml.
 * This is a <em>template</em>: actual ItemStacks are produced by {@link RarityItemFactory}.
 */
public final class RarityItem {

    private final String id;
    private final Material material;
    private final RarityTier rarity;
    private final String displayName;           // MiniMessage string
    private final List<String> lore;            // MiniMessage strings
    private final @Nullable String setId;
    private final boolean canHaveAffix;

    public RarityItem(
            @NotNull String id,
            @NotNull Material material,
            @NotNull RarityTier rarity,
            @NotNull String displayName,
            @NotNull List<String> lore,
            @Nullable String setId,
            boolean canHaveAffix) {
        this.id = id;
        this.material = material;
        this.rarity = rarity;
        this.displayName = displayName;
        this.lore = List.copyOf(lore);
        this.setId = setId;
        this.canHaveAffix = canHaveAffix;
    }

    /** Unique registry ID (e.g. {@code "mythic_netherite_sword"}). */
    @NotNull public String getId() { return id; }

    /** The vanilla material this item is based on. */
    @NotNull public Material getMaterial() { return material; }

    /** The rarity tier for this template. */
    @NotNull public RarityTier getRarity() { return rarity; }

    /** MiniMessage display name string. */
    @NotNull public String getDisplayName() { return displayName; }

    /** MiniMessage lore lines (immutable). */
    @NotNull public List<String> getLore() { return lore; }

    /** Set ID, or {@code null} if this item does not belong to a set. */
    @Nullable public String getSetId() { return setId; }

    /** Whether this item is eligible to receive an affix. */
    public boolean canHaveAffix() { return canHaveAffix; }
}
