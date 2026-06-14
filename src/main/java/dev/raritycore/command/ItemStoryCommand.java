package dev.raritycore.command;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.storage.ItemStatistics;
import dev.raritycore.storage.MigrationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ItemStoryCommand implements CommandExecutor {

    private final RarityCorePlugin plugin;

    public ItemStoryCommand(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        MigrationManager migration = plugin.getStorageManager().getMigrationManager();
        UUID itemUuid = migration.migrateOrGetUUID(item);

        if (itemUuid == null) {
            player.sendMessage("§cYou are not holding a RarityCore item.");
            return true;
        }

        ItemStatistics stats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(itemUuid);

        String itemName = "Unknown Item";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            itemName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName()).replaceAll("§x(§[0-9a-f]){6}", "");
        } else {
            String matName = item.getType().name().replace("_", " ").toLowerCase();
            String capMatName = "";
            for (String word : matName.split(" ")) capMatName += word.substring(0, 1).toUpperCase() + word.substring(1) + " ";
            itemName = capMatName.trim();
        }

        String rarityId = item.getItemMeta().getPersistentDataContainer().get(dev.raritycore.util.ItemUtil.KEY_RARITY, org.bukkit.persistence.PersistentDataType.STRING);
        String rarityName = rarityId != null ? rarityId.substring(0, 1).toUpperCase() + rarityId.substring(1) : "Unknown";
        String matName = item.getType().name().replace("_", " ").toLowerCase();
        String capMatName = "";
        for (String word : matName.split(" ")) capMatName += word.substring(0, 1).toUpperCase() + word.substring(1) + " ";
        String typeString = rarityName + " " + capMatName.trim();

        String originalQuality = "Unknown";
        String currentQuality = "Unknown";
        if (!stats.getQualityHistory().isEmpty()) {
            String firstId = stats.getQualityHistory().get(0).getQualityId();
            String lastId = stats.getQualityHistory().get(stats.getQualityHistory().size() - 1).getQualityId();
            dev.raritycore.quality.QualityTier firstQ = plugin.getRegistries().getQualities().get(firstId);
            dev.raritycore.quality.QualityTier lastQ = plugin.getRegistries().getQualities().get(lastId);
            
            if (firstId.equals("quality_legacy")) {
                String note = stats.getQualityHistory().get(0).getReason();
                originalQuality = (note != null && note.contains("(")) ? note.substring(note.indexOf("(") + 1, note.indexOf(")")) : "Legacy";
            } else {
                originalQuality = firstQ != null ? firstQ.getDisplayName() : firstId;
            }
            
            if (lastId.equals("quality_legacy")) {
                String note = stats.getQualityHistory().get(stats.getQualityHistory().size() - 1).getReason();
                currentQuality = (note != null && note.contains("(")) ? note.substring(note.indexOf("(") + 1, note.indexOf(")")) : "Legacy";
            } else {
                currentQuality = lastQ != null ? lastQ.getDisplayName() : lastId;
            }
        }

        player.sendMessage("§8====================================");
        player.sendMessage("§6" + itemName);
        player.sendMessage("§7" + typeString);
        player.sendMessage("");
        player.sendMessage("§7Current Quality: §f" + currentQuality);
        player.sendMessage("§7Original Quality: §f" + originalQuality);
        player.sendMessage("");
        
        long age = ((System.currentTimeMillis() - stats.getCreationTimestamp()) / (1000L * 60 * 60 * 24));
        if (typeString.contains("Pickaxe") || typeString.contains("Shovel")) {
            player.sendMessage("§7Blocks Mined: §f" + stats.getBlocksMined());
        } else if (typeString.contains("Rod")) {
            player.sendMessage("§7Fish Caught: §f" + stats.getFishCaught());
        } else {
            player.sendMessage("§7Kills: §f" + stats.getKills());
        }
        
        player.sendMessage("§7Age: §f" + age + " days");
        player.sendMessage("§7Creator: §f" + (stats.getCreatorName() != null ? stats.getCreatorName() : "Unknown"));
        player.sendMessage("§7Owners: §f" + Math.max(1, stats.getOwnersHistory().size()));
        player.sendMessage("");
        player.sendMessage("§7Season: §f" + (stats.getCreationSeason() != null ? stats.getCreationSeason() : "Unknown"));
        player.sendMessage("§7Event: §f" + (stats.getCreationEvent() != null ? stats.getCreationEvent() : "None"));
        
        String epithet = plugin.getIdentityManager().getEpithetGenerator().generateEpithet(stats);
        if (epithet != null && !epithet.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("§7Epithet:");
            player.sendMessage("§e\"" + epithet + "\"");
        }
        player.sendMessage("§8====================================");

        return true;
    }
}
