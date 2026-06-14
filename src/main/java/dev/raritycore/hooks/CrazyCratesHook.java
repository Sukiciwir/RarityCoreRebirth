package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import org.jetbrains.annotations.NotNull;

/**
 * CrazyCrates integration hook.
 *
 * <p>RarityCore items can be added as CrazyCrates prizes by using the
 * {@code /rarity give} command in the crate's prize configuration, or by
 * using CrazyCrates' custom item format referencing the RarityCore give command.
 *
 * <p>Future: implement a proper CrazyCrates ItemProvider when their API supports it.
 */
public final class CrazyCratesHook {

    private final RarityCorePlugin plugin;

    public CrazyCratesHook(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("[CrazyCrates] Hook initialized. "
                + "Use /rarity give in prize commands to reward RarityCore items.");
    }

    /**
     * Called to verify the hook is active.
     * Future versions will implement a full ItemProvider here.
     */
    public boolean isActive() { return true; }
}
