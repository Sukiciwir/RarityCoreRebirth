package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LuckPerms integration hook.
 *
 * <p>Usage: checks whether a player has a specific LuckPerms permission node,
 * enabling permission-gated rarity drop-rate modifiers in the future.
 * Currently exposed as a utility for other systems to call.
 */
public final class LuckPermsHook {

    private final RarityCorePlugin plugin;
    private final LuckPerms api;

    public LuckPermsHook(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
        this.api = LuckPermsProvider.get();
    }

    /** Returns true if the player has the given node explicitly set. */
    public boolean hasNode(@NotNull Player player, @NotNull String node) {
        try {
            User user = api.getPlayerAdapter(Player.class).getUser(player);
            return user.getCachedData().getPermissionData().checkPermission(node).asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns a drop rate multiplier based on the player's permission nodes.
     * Admin can grant {@code raritycore.multiplier.2x} etc.
     */
    public double getDropMultiplier(@NotNull Player player) {
        for (double mult : new double[]{3.0, 2.5, 2.0, 1.5}) {
            String node = "raritycore.multiplier." + (int)(mult * 10) + "x";
            if (hasNode(player, node)) return mult;
        }
        return 1.0;
    }
}
