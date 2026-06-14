package dev.raritycore.hooks;

import org.bukkit.Bukkit;

/**
 * Soft-depend hook for WorldCore.
 */
public final class WorldCoreHook {

    private boolean isHooked = false;

    public void load() {
        if (Bukkit.getPluginManager().getPlugin("WorldCore") != null) {
            isHooked = true;
        }
    }

    public String getCurrentSeason() {
        if (!isHooked) return "NONE";
        // Call WorldCore API here
        return "NONE";
    }

    public String getCurrentEvent() {
        if (!isHooked) return "NONE";
        // Call WorldCore API here
        return "NONE";
    }
}
