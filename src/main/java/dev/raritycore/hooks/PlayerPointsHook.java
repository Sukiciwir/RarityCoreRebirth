package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerPointsHook {

    private final RarityCorePlugin plugin;
    private PlayerPointsAPI ppAPI = null;

    public PlayerPointsHook(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupPlayerPoints() {
        Plugin ppPlugin = plugin.getServer().getPluginManager().getPlugin("PlayerPoints");
        if (ppPlugin instanceof PlayerPoints playerPoints) {
            this.ppAPI = playerPoints.getAPI();
            return true;
        }
        return false;
    }

    public boolean hasEnough(Player player, int amount) {
        if (ppAPI == null) return false;
        return ppAPI.look(player.getUniqueId()) >= amount;
    }

    public boolean withdraw(Player player, int amount) {
        if (ppAPI == null) return false;
        return ppAPI.take(player.getUniqueId(), amount);
    }

    public boolean deposit(Player player, int amount) {
        if (ppAPI == null) return false;
        return ppAPI.give(player.getUniqueId(), amount);
    }
}
