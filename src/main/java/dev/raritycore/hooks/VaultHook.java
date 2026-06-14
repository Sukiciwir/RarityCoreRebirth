package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.entity.Player;

public class VaultHook {

    private final RarityCorePlugin plugin;
    private Economy econ = null;

    public VaultHook(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean hasEnough(Player player, double amount) {
        if (econ == null) return false;
        return econ.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (econ == null) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (econ == null) return false;
        return econ.depositPlayer(player, amount).transactionSuccess();
    }
}
