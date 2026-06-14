package dev.raritycore.gui;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.collection.CollectionGUI;
import dev.raritycore.salvage.SalvageGUI;
import dev.raritycore.upgrade.UpgradeGUI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Central router that opens the correct GUI for a player given a sub-command.
 */
public final class GUIManager {

    private final CollectionGUI collectionGUI;
    private final SalvageGUI    salvageGUI;
    private final UpgradeGUI    upgradeGUI;
    private final StatsGUI      statsGUI;
    private final GachaGUI      gachaGUI;

    public GUIManager(@NotNull CollectionGUI collectionGUI,
                      @NotNull SalvageGUI    salvageGUI,
                      @NotNull UpgradeGUI    upgradeGUI,
                      @NotNull StatsGUI      statsGUI,
                      @NotNull GachaGUI      gachaGUI) {
        this.collectionGUI = collectionGUI;
        this.salvageGUI    = salvageGUI;
        this.upgradeGUI    = upgradeGUI;
        this.statsGUI      = statsGUI;
        this.gachaGUI      = gachaGUI;
    }

    public void openCollection(@NotNull Player player) { collectionGUI.open(player, 0); }
    public void openSalvage   (@NotNull Player player) { salvageGUI.open(player); }
    public void openUpgrade   (@NotNull Player player) { upgradeGUI.open(player); }
    public void openStats     (@NotNull Player player) { statsGUI.open(player); }
    public void openGacha     (@NotNull Player player) { gachaGUI.open(player); }
}
