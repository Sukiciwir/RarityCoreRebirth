package dev.raritycore.command;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.identity.EvolutionStage;
import dev.raritycore.rarity.RevealFlags;
import dev.raritycore.storage.ItemStatistics;
import dev.raritycore.storage.MigrationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AdminCommands implements CommandExecutor, TabCompleter {

    private final RarityCorePlugin plugin;

    public AdminCommands(RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("raritycore.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /raritycore <inspect|simulate|reveal|quality|rarity|owner|epithet|traits|affixes|familiarity|rename|ascend|broadcast|stats|age|resonance|evolution|cache|migrate|reload>");
            return true;
        }

        String sub = args[0].toLowerCase();
        
        // Commands that do NOT require holding an item
        if (sub.equals("reload")) {
            if (!sender.hasPermission("raritycore.admin.reload")) { sender.sendMessage("§cNo permission."); return true; }
            plugin.getConfigManager().reload();
            plugin.getRegistries().load();
            sender.sendMessage("§aReloaded all RarityCore configurations and registries.");
            return true;
        }
        
        if (sub.equals("cache")) {
            if (!sender.hasPermission("raritycore.admin.cache")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length < 2) { sender.sendMessage("§cUsage: /raritycore cache <flush|reload>"); return true; }
            if (args[1].equalsIgnoreCase("flush")) {
                plugin.getStorageManager().getCacheManager().flushSync();
                sender.sendMessage("§aAll item stats flushed to database and cache cleared.");
            } else if (args[1].equalsIgnoreCase("reload")) {
                sender.sendMessage("§eReloading cache... §7(Note: Only currently loaded items are kept in memory)");
            }
            return true;
        }

        if (sub.equals("give")) {
            if (!sender.hasPermission("raritycore.admin.give")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /raritycore give <player> <item-id> [amount] [key=value...]");
                return true;
            }
            Player target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            String itemId = args[2];
            dev.raritycore.rarity.RarityItem template = plugin.getRegistries().getItems().get(itemId);
            if (template == null) {
                sender.sendMessage("§cUnknown item ID: §e" + itemId);
                return true;
            }
            int amount = 1;
            int argStartIndex = 3;
            if (args.length > 3 && !args[3].contains("=")) {
                try { amount = Math.max(1, Integer.parseInt(args[3])); argStartIndex = 4; }
                catch (NumberFormatException e) { /* ignore, use 1 */ }
            }
            
            ItemStack stack = plugin.getRarityItemFactory().build(template, target.getName(), null, 0);
            stack.setAmount(Math.min(amount, stack.getMaxStackSize()));
            
            // Check for extended arguments
            if (args.length > argStartIndex) {
                java.util.UUID itemUuid = plugin.getStorageManager().getMigrationManager().migrateOrGetUUID(stack);
                if (itemUuid != null) {
                    ItemStatistics stats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(itemUuid);
                    for (int i = argStartIndex; i < args.length; i++) {
                        String[] kv = args[i].split("=", 2);
                        if (kv.length == 2) {
                            String key = kv[0].toLowerCase();
                            String value = kv[1];
                            try {
                                switch (key) {
                                    case "age" -> stats.setCreationTimestamp(System.currentTimeMillis() - (Long.parseLong(value) * 24L * 60 * 60 * 1000));
                                    case "kills" -> stats.setKills(Integer.parseInt(value));
                                    case "blocks" -> stats.setBlocksMined(Integer.parseInt(value));
                                    case "fish" -> stats.setFishCaught(Integer.parseInt(value));
                                    case "repairs" -> stats.setRepairsCount(Integer.parseInt(value));
                                    case "owner" -> {
                                        stats.addOwner(value);
                                        stats.setCurrentMaster(value);
                                    }
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    plugin.getStorageManager().getCacheManager().markDirty(stats);
                    plugin.getIdentityManager().getFamiliaritySystem().evaluateFamiliarity(stack, stats);
                    plugin.getLegacySystem().getLegacyManager().checkAscension(stack, stats);
                    plugin.getRarityItemFactory().rebuildLore(stack);
                }
            }

            java.util.Map<Integer, ItemStack> overflow = target.getInventory().addItem(stack);
            for (ItemStack leftover : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
            }
            sender.sendMessage("§aGave " + amount + "x " + template.getDisplayName() + " to " + target.getName());
            return true;
        }

        // The rest of the commands require a player holding a RarityCore item
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only for item commands.");
            return true;
        }

        if (sub.equals("migrate")) {
            if (!player.hasPermission("raritycore.admin.migrate")) { player.sendMessage("§cNo permission."); return true; }
            if (args.length < 2) { player.sendMessage("§cUsage: /raritycore migrate <hand|inventory|player>"); return true; }
            MigrationManager migration = plugin.getStorageManager().getMigrationManager();
            if (args[1].equalsIgnoreCase("hand")) {
                ItemStack item = player.getInventory().getItemInMainHand();
                UUID uuid = migration.migrateOrGetUUID(item);
                if (uuid != null) player.sendMessage("§aHand item migrated/loaded: " + uuid);
                else player.sendMessage("§cNot a valid legacy item to migrate.");
            } else if (args[1].equalsIgnoreCase("inventory")) {
                int count = 0;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && migration.migrateOrGetUUID(item) != null) count++;
                }
                player.sendMessage("§aMigrated/Loaded " + count + " items in your inventory.");
            } else if (args[1].equalsIgnoreCase("player") && args.length >= 3) {
                Player target = org.bukkit.Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }
                int count = 0;
                for (ItemStack item : target.getInventory().getContents()) {
                    if (item != null && migration.migrateOrGetUUID(item) != null) count++;
                }
                player.sendMessage("§aMigrated/Loaded " + count + " items in " + target.getName() + "'s inventory.");
            }
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
        
        // Permission check generic helper
        if (!player.hasPermission("raritycore.admin." + sub)) {
            player.sendMessage("§cNo permission for subcommand: " + sub);
            return true;
        }

        switch (sub) {
            case "inspect" -> {
                String currentQuality = "Unknown";
                String originalQuality = "Unknown";
                if (!stats.getQualityHistory().isEmpty()) {
                    String firstId = stats.getQualityHistory().get(0).getQualityId();
                    String lastId = stats.getQualityHistory().get(stats.getQualityHistory().size() - 1).getQualityId();
                    dev.raritycore.quality.QualityTier firstQ = plugin.getRegistries().getQualities().get(firstId);
                    dev.raritycore.quality.QualityTier lastQ = plugin.getRegistries().getQualities().get(lastId);
                    
                    originalQuality = firstId.equals("quality_legacy") ? "Legacy" : (firstQ != null ? firstQ.getDisplayName() : firstId);
                    currentQuality = lastId.equals("quality_legacy") ? "Legacy" : (lastQ != null ? lastQ.getDisplayName() : lastId);
                }

                player.sendMessage("§6== Item Inspection ==");
                player.sendMessage("§eUUID: §7" + stats.getItemUuid());
                player.sendMessage("§eCurrent Quality: §f" + currentQuality);
                player.sendMessage("§eOriginal Quality: §f" + originalQuality);
                player.sendMessage("§eCurrent Owner: §f" + stats.getCurrentMaster());
                player.sendMessage("§7Original Owner: §a" + stats.getOriginalOwnerName());
                player.sendMessage("§7Age (days): §a" + stats.getAgeInDays());
                player.sendMessage("§7Kills: §a" + stats.getKills());
                player.sendMessage("§7Blocks Mined: §a" + stats.getBlocksMined());
                player.sendMessage("§7Fish Caught: §a" + stats.getFishCaught());
                player.sendMessage("§7Repairs: §a" + stats.getRepairsCount());
                
                double res = stats.getDebugState().getForcedResonance() != null ? 
                    stats.getDebugState().getForcedResonance() : 
                    plugin.getLegacySystem().getResonanceSystem().calculateResonance(stats, player);
                player.sendMessage("§7Resonance: §e" + res + (stats.getDebugState().getForcedResonance() != null ? " (Forced)" : ""));
                
                player.sendMessage("§7Reveal State: §b" + item.getItemMeta().getPersistentDataContainer().getOrDefault(MigrationManager.KEY_REVEAL_STATE, org.bukkit.persistence.PersistentDataType.INTEGER, RevealFlags.FLAG_ALL));
                
                EvolutionStage.Stage evo = stats.getDebugState().getForcedEvolution();
                sender.sendMessage("§7Evolution: §f" + (evo != null ? evo.name() : "Dynamic"));
                
                if (stats.getDebugState().getForcedLegacyTitle() != null) {
                    player.sendMessage("§7Legacy Title: §6" + stats.getDebugState().getForcedLegacyTitle() + " (Forced)");
                }
            }
            case "simulate" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /raritycore simulate <kills|blocks|fish|damage|distance|age|owner> <amount|player>");
                    return true;
                }
                String simType = args[1].toLowerCase();
                try {
                    if (simType.equals("owner")) {
                        stats.addOwner(args[2]);
                        plugin.getStorageManager().getCacheManager().markDirty(stats);
                        player.sendMessage("§aSimulated new owner: " + args[2]);
                        return true;
                    }
                    
                    int amount = Integer.parseInt(args[2]);
                    if (simType.equals("kills")) {
                        stats.addKills(amount);
                        player.sendMessage("§aSimulated " + amount + " kills.");
                    } else if (simType.equals("blocks")) {
                        stats.addBlocksMined(amount);
                        player.sendMessage("§aSimulated " + amount + " blocks mined.");
                    } else if (simType.equals("fish")) {
                        stats.addFishCaught(amount);
                        player.sendMessage("§aSimulated " + amount + " fish caught.");
                    } else if (simType.equals("damage")) {
                        stats.addDamageDealt(amount);
                        player.sendMessage("§aSimulated " + amount + " damage dealt.");
                    } else if (simType.equals("distance")) {
                        stats.addDistanceTraveled(amount);
                        player.sendMessage("§aSimulated " + amount + " distance traveled.");
                    } else if (simType.equals("age")) {
                        stats.setCreationTimestamp(System.currentTimeMillis() - (amount * 24L * 60 * 60 * 1000));
                        player.sendMessage("§aSimulated age to " + amount + " days ago.");
                    }
                    plugin.getStorageManager().getCacheManager().markDirty(stats);
                    plugin.getIdentityManager().getFamiliaritySystem().evaluateFamiliarity(item, stats);
                    plugin.getLegacySystem().getLegacyManager().checkAscension(item, stats);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number.");
                }
            }
            case "reveal" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /raritycore reveal <all|rarity|quality|traits|affixes>");
                    return true;
                }
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                int currentFlags = meta.getPersistentDataContainer().getOrDefault(MigrationManager.KEY_REVEAL_STATE, org.bukkit.persistence.PersistentDataType.INTEGER, RevealFlags.FLAG_ALL);
                String target = args[1].toLowerCase();
                
                if (target.equals("all")) {
                    currentFlags = RevealFlags.FLAG_ALL;
                    List<dev.raritycore.trait.TraitInstance> traits = dev.raritycore.util.ItemUtil.getTraits(meta);
                    for (dev.raritycore.trait.TraitInstance t : traits) t.setDiscovered(true);
                    dev.raritycore.util.ItemUtil.setTraits(meta, traits);
                } else if (target.equals("rarity")) {
                    currentFlags = RevealFlags.addFlag(currentFlags, RevealFlags.FLAG_RARITY);
                } else if (target.equals("quality")) {
                    currentFlags = RevealFlags.addFlag(currentFlags, RevealFlags.FLAG_QUALITY);
                } else if (target.equals("traits")) {
                    currentFlags = RevealFlags.addFlag(currentFlags, RevealFlags.FLAG_TRAITS);
                    List<dev.raritycore.trait.TraitInstance> traits = dev.raritycore.util.ItemUtil.getTraits(meta);
                    for (dev.raritycore.trait.TraitInstance t : traits) t.setDiscovered(true);
                    dev.raritycore.util.ItemUtil.setTraits(meta, traits);
                } else if (target.equals("affixes")) {
                    currentFlags = RevealFlags.addFlag(currentFlags, RevealFlags.FLAG_AFFIXES);
                }
                
                meta.getPersistentDataContainer().set(MigrationManager.KEY_REVEAL_STATE, org.bukkit.persistence.PersistentDataType.INTEGER, currentFlags);
                item.setItemMeta(meta);
                plugin.getRarityItemFactory().rebuildLore(item);
                player.getInventory().setItemInMainHand(item);
                player.updateInventory();
                player.sendMessage("§aItem reveal state updated.");
            }
            case "quality" -> {
                if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
                    player.sendMessage("§cUsage: /raritycore quality set <quality_id|legacy>");
                    return true;
                }
                if (args[2].equalsIgnoreCase("legacy")) {
                    plugin.getLegacySystem().getLegacyManager().forceAscension(stats, player, item);
                    player.getInventory().setItemInMainHand(item);
                    player.updateInventory();
                    player.sendMessage("§aForced Legacy Ascension.");
                } else {
                    dev.raritycore.quality.QualityTier qTier = plugin.getRegistries().getQualities().get(args[2]);
                    if (qTier != null) {
                        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                        meta.getPersistentDataContainer().set(dev.raritycore.util.ItemUtil.KEY_QUALITY, org.bukkit.persistence.PersistentDataType.INTEGER, qTier.getStatMin());
                        item.setItemMeta(meta);
                        
                        stats.addQualityHistoryEntry(new dev.raritycore.storage.QualityHistoryEntry(
                            qTier.getId(), 
                            System.currentTimeMillis(), 
                            "Admin Command by " + player.getName()
                        ));
                        plugin.getStorageManager().getCacheManager().markDirty(stats);
                        
                        plugin.getRarityItemFactory().rebuildLore(item);
                        player.getInventory().setItemInMainHand(item);
                        player.updateInventory();
                        player.sendMessage("§aSet quality to " + qTier.getId());
                    } else {
                        // Check if it's a forced legacy title
                        stats.getDebugState().setForcedLegacyTitle(args[2]);
                        plugin.getStorageManager().getCacheManager().markDirty(stats);
                        plugin.getRarityItemFactory().rebuildLore(item);
                        player.getInventory().setItemInMainHand(item);
                        player.updateInventory();
                        player.sendMessage("§aSet forced legacy title to: " + args[2]);
                    }
                }
            }
            case "rarity" -> {
                if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
                    player.sendMessage("§cUsage: /raritycore rarity set <rarity_id>");
                    return true;
                }
                dev.raritycore.rarity.RarityTier rTier = plugin.getRegistries().getRarities().get(args[2]);
                if (rTier != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    meta.getPersistentDataContainer().set(dev.raritycore.util.ItemUtil.KEY_RARITY, org.bukkit.persistence.PersistentDataType.STRING, rTier.getId());
                    item.setItemMeta(meta);
                    plugin.getRarityItemFactory().rebuildLore(item);
                    player.sendMessage("§aSet rarity to " + rTier.getId());
                } else {
                    player.sendMessage("§cInvalid rarity ID.");
                }
            }
            case "traits" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /raritycore traits <force|remove|clear|reroll>");
                    return true;
                }
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                List<dev.raritycore.trait.TraitInstance> traits = dev.raritycore.util.ItemUtil.getTraits(meta);
                
                if (args[1].equalsIgnoreCase("clear")) {
                    dev.raritycore.util.ItemUtil.setTraits(meta, new ArrayList<>());
                    player.sendMessage("§aCleared all traits.");
                } else if (args[1].equalsIgnoreCase("remove") && args.length >= 3) {
                    traits.removeIf(t -> t.getTraitId().equalsIgnoreCase(args[2]));
                    dev.raritycore.util.ItemUtil.setTraits(meta, traits);
                    player.sendMessage("§aRemoved trait " + args[2]);
                } else if (args[1].equalsIgnoreCase("force") && args.length >= 4) {
                    traits.add(new dev.raritycore.trait.TraitInstance(args[2], true, Integer.parseInt(args[3])));
                    dev.raritycore.util.ItemUtil.setTraits(meta, traits);
                    player.sendMessage("§aForced trait " + args[2] + " with progress " + args[3]);
                } else if (args[1].equalsIgnoreCase("reroll")) {
                    String rarityId = meta.getPersistentDataContainer().get(dev.raritycore.util.ItemUtil.KEY_RARITY, org.bukkit.persistence.PersistentDataType.STRING);
                    dev.raritycore.rarity.RarityTier rTier = rarityId != null ? plugin.getRegistries().getRarities().get(rarityId) : null;
                    if (rTier != null) {
                        String family = plugin.getGenerationManager().determineFamily(item.getType());
                        int max = rTier.getTraitMax();
                        int min = rTier.getTraitMin();
                        int amount = min + (int) (Math.random() * ((max - min) + 1));
                        List<dev.raritycore.trait.Trait> rolled = plugin.getTraitSystem().getManager().rollTraits(family, amount, plugin.getTraitSystem().getConflictManager());
                        List<dev.raritycore.trait.TraitInstance> newTraits = new java.util.ArrayList<>();
                        for (dev.raritycore.trait.Trait t : rolled) newTraits.add(new dev.raritycore.trait.TraitInstance(t.getId(), false, 0));
                        dev.raritycore.util.ItemUtil.setTraits(meta, newTraits);
                        player.sendMessage("§aRerolled traits.");
                    }
                }
                item.setItemMeta(meta);
                plugin.getRarityItemFactory().rebuildLore(item);
            }
            case "affixes" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /raritycore affixes <force|clear|reroll>");
                    return true;
                }
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (args[1].equalsIgnoreCase("clear")) {
                    meta.getPersistentDataContainer().remove(dev.raritycore.util.ItemUtil.KEY_AFFIX);
                    player.sendMessage("§aCleared affix.");
                } else if (args[1].equalsIgnoreCase("force") && args.length >= 3) {
                    meta.getPersistentDataContainer().set(dev.raritycore.util.ItemUtil.KEY_AFFIX, org.bukkit.persistence.PersistentDataType.STRING, args[2]);
                    player.sendMessage("§aForced affix " + args[2]);
                } else if (args[1].equalsIgnoreCase("reroll")) {
                    String rarityId = meta.getPersistentDataContainer().get(dev.raritycore.util.ItemUtil.KEY_RARITY, org.bukkit.persistence.PersistentDataType.STRING);
                    dev.raritycore.rarity.RarityTier rTier = rarityId != null ? plugin.getRegistries().getRarities().get(rarityId) : null;
                    if (rTier != null) {
                        dev.raritycore.affix.Affix newAffix = plugin.getAffixManager().randomAffix();
                        if (newAffix != null) {
                            meta.getPersistentDataContainer().set(dev.raritycore.util.ItemUtil.KEY_AFFIX, org.bukkit.persistence.PersistentDataType.STRING, newAffix.getId());
                            player.sendMessage("§aRerolled affix: " + newAffix.getId());
                        } else {
                            meta.getPersistentDataContainer().remove(dev.raritycore.util.ItemUtil.KEY_AFFIX);
                            player.sendMessage("§aRerolled affix: None");
                        }
                    }
                }
                item.setItemMeta(meta);
                plugin.getRarityItemFactory().rebuildLore(item);
            }
            case "familiarity" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /raritycore familiarity <set <score>|reset>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("reset")) {
                    stats.setKills(0);
                    stats.setBlocksMined(0);
                    stats.setFishCaught(0);
                    player.sendMessage("§aFamiliarity stats reset.");
                } else if (args[1].equalsIgnoreCase("set") && args.length >= 3) {
                    int score = Integer.parseInt(args[2]);
                    stats.setKills(score);
                    stats.setBlocksMined(score);
                    player.sendMessage("§aFamiliarity score approx set to " + score);
                }
                plugin.getStorageManager().getCacheManager().markDirty(stats);
                plugin.getIdentityManager().getFamiliaritySystem().evaluateFamiliarity(item, stats);
                plugin.getRarityItemFactory().rebuildLore(item);
            }
            case "owner" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /raritycore owner <set|add> <player>");
                    return true;
                }
                String newOwner = args[2];
                if (args[1].equalsIgnoreCase("set")) {
                    stats.setCurrentMaster(newOwner);
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    meta.getPersistentDataContainer().set(dev.raritycore.util.ItemUtil.KEY_FIRST_OWNER, org.bukkit.persistence.PersistentDataType.STRING, newOwner);
                    item.setItemMeta(meta);
                    player.sendMessage("§aSet current owner and first owner to " + newOwner);
                } else if (args[1].equalsIgnoreCase("add")) {
                    stats.addOwner(newOwner);
                    player.sendMessage("§aAdded " + newOwner + " to owner history.");
                }
                plugin.getStorageManager().getCacheManager().markDirty(stats);
                plugin.getRarityItemFactory().rebuildLore(item);
            }
            case "epithet" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /raritycore epithet <set <text>|regenerate>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("regenerate")) {
                    String epithet = plugin.getIdentityManager().getNameGenerator().generateEpithet(stats, item);
                    stats.setCachedEpithet(epithet);
                    player.sendMessage("§aRegenerated epithet: " + epithet);
                } else if (args[1].equalsIgnoreCase("set") && args.length >= 3) {
                    String epithet = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    stats.setCachedEpithet(epithet);
                    player.sendMessage("§aSet epithet to " + epithet);
                }
                plugin.getStorageManager().getCacheManager().markDirty(stats);
                plugin.getRarityItemFactory().rebuildLore(item);
            }
            case "rename" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /raritycore rename <name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                meta.displayName(dev.raritycore.util.ColorUtil.parse(name));
                item.setItemMeta(meta);
                player.sendMessage("§aRenamed item to " + name);
            }
            case "ascend" -> {
                if (args.length >= 2) {
                    stats.getDebugState().setForcedLegacyTitle(args[1]);
                    plugin.getStorageManager().getCacheManager().markDirty(stats);
                }
                plugin.getLegacySystem().getLegacyManager().forceAscension(stats, player, item);
                player.sendMessage("§aForced ascension" + (args.length >= 2 ? " with title " + args[1] : "") + ".");
            }
            case "broadcast" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("legacy")) {
                    plugin.getLegacySystem().getLegacyManager().broadcastLegacy(player, item, stats);
                } else {
                    player.sendMessage("§cUsage: /raritycore broadcast legacy");
                }
            }
            case "stats" -> {
                if (args.length >= 4 && args[2].equalsIgnoreCase("add")) {
                    int amount = Integer.parseInt(args[3]);
                    if (args[1].equalsIgnoreCase("kills")) stats.addKills(amount);
                    else if (args[1].equalsIgnoreCase("blocks")) stats.addBlocksMined(amount);
                    else if (args[1].equalsIgnoreCase("fish")) stats.addFishCaught(amount);
                    else if (args[1].equalsIgnoreCase("repairs")) stats.setRepairsCount(stats.getRepairsCount() + amount);
                    
                    plugin.getStorageManager().getCacheManager().markDirty(stats);
                    plugin.getIdentityManager().getFamiliaritySystem().evaluateFamiliarity(item, stats);
                    player.sendMessage("§aAdded " + amount + " " + args[1] + ".");
                } else {
                    player.sendMessage("§cUsage: /raritycore stats <kills|blocks|fish|repairs> add <amount>");
                }
            }
            case "age" -> {
                if (args.length >= 3 && args[1].equalsIgnoreCase("set")) {
                    int days = Integer.parseInt(args[2]);
                    stats.setCreationTimestamp(System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000));
                    plugin.getStorageManager().getCacheManager().markDirty(stats);
                    player.sendMessage("§aSet age to " + days + " days.");
                } else {
                    player.sendMessage("§cUsage: /raritycore age set <days>");
                }
            }
            case "resonance" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
                    stats.getDebugState().setForcedResonance(null);
                    player.sendMessage("§aReset forced resonance.");
                } else if (args.length >= 3 && args[1].equalsIgnoreCase("set")) {
                    double val = Double.parseDouble(args[2]);
                    stats.getDebugState().setForcedResonance(val);
                    player.sendMessage("§aForced resonance to " + val);
                } else {
                    player.sendMessage("§cUsage: /raritycore resonance <set <val>|reset>");
                    return true;
                }
                plugin.getStorageManager().getCacheManager().markDirty(stats);
                plugin.getRarityItemFactory().rebuildLore(item);
            }
            case "evolution" -> {
                if (args.length >= 3 && args[1].equalsIgnoreCase("set")) {
                    try {
                        EvolutionStage.Stage stage = EvolutionStage.Stage.valueOf(args[2].toUpperCase());
                        stats.getDebugState().setForcedEvolution(stage);
                        player.sendMessage("§aForced evolution stage to " + stage.name());
                        plugin.getStorageManager().getCacheManager().markDirty(stats);
                        plugin.getRarityItemFactory().rebuildLore(item);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid evolution stage.");
                    }
                } else {
                    player.sendMessage("§cUsage: /raritycore evolution set <stage>");
                }
            }
            default -> player.sendMessage("§cUnknown subcommand.");
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("raritycore.admin")) return completions;

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                "inspect", "simulate", "reveal", "ascend", "broadcast", 
                "quality", "rarity", "owner", "epithet", "traits", "affixes",
                "familiarity", "rename", "stats", "age", "resonance", "evolution",
                "cache", "migrate", "reload", "give"
            );
            String input = args[0].toLowerCase();
            for (String sub : subcommands) {
                if (sub.startsWith(input) && sender.hasPermission("raritycore.admin." + sub)) completions.add(sub);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            if (sub.equals("simulate")) {
                for (String s : Arrays.asList("kills", "blocks", "fish", "damage", "distance", "age", "owner")) {
                    if (s.startsWith(input)) completions.add(s);
                }
            } else if (sub.equals("quality") || sub.equals("rarity") || sub.equals("owner") || sub.equals("epithet") || sub.equals("familiarity") || sub.equals("resonance") || sub.equals("evolution")) {
                if ("set".startsWith(input)) completions.add("set");
                if (sub.equals("familiarity") && "reset".startsWith(input)) completions.add("reset");
                if (sub.equals("resonance") && "reset".startsWith(input)) completions.add("reset");
                if (sub.equals("owner") && "add".startsWith(input)) completions.add("add");
                if (sub.equals("epithet") && "regenerate".startsWith(input)) completions.add("regenerate");
            } else if (sub.equals("broadcast")) {
                if ("legacy".startsWith(input)) completions.add("legacy");
            } else if (sub.equals("traits") || sub.equals("affixes")) {
                if ("clear".startsWith(input)) completions.add("clear");
                if ("force".startsWith(input)) completions.add("force");
                if ("reroll".startsWith(input)) completions.add("reroll");
                if (sub.equals("traits") && "remove".startsWith(input)) completions.add("remove");
            } else if (sub.equals("stats")) {
                for (String s : Arrays.asList("kills", "blocks", "fish", "repairs")) {
                    if (s.startsWith(input)) completions.add(s);
                }
            } else if (sub.equals("age")) {
                if ("set".startsWith(input)) completions.add("set");
            } else if (sub.equals("reveal")) {
                for (String s : Arrays.asList("all", "rarity", "quality", "traits", "affixes")) {
                    if (s.startsWith(input)) completions.add(s);
                }
            } else if (sub.equals("cache")) {
                if ("flush".startsWith(input)) completions.add("flush");
                if ("reload".startsWith(input)) completions.add("reload");
            } else if (sub.equals("migrate")) {
                if ("hand".startsWith(input)) completions.add("hand");
                if ("inventory".startsWith(input)) completions.add("inventory");
                if ("player".startsWith(input)) completions.add("player");
            } else if (sub.equals("give")) {
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String input2 = args[1].toLowerCase();
            if (sub.equals("quality") && input2.equals("set")) {
                for (dev.raritycore.quality.QualityTier q : plugin.getRegistries().getQualities().getAll()) {
                    if (q.getId().startsWith(args[2].toLowerCase())) completions.add(q.getId());
                }
                if ("legacy".startsWith(args[2].toLowerCase())) completions.add("legacy");
            } else if (sub.equals("rarity") && input2.equals("set")) {
                for (dev.raritycore.rarity.RarityTier r : plugin.getRegistries().getRarities().getAll()) {
                    if (r.getId().startsWith(args[2].toLowerCase())) completions.add(r.getId());
                }
            } else if (sub.equals("stats")) {
                if ("add".startsWith(args[2].toLowerCase())) completions.add("add");
            } else if (sub.equals("evolution") && input2.equals("set")) {
                for (EvolutionStage.Stage stage : EvolutionStage.Stage.values()) {
                    if (stage.name().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(stage.name().toLowerCase());
                }
            } else if (sub.equals("give")) {
                for (dev.raritycore.rarity.RarityItem rItem : plugin.getRegistries().getItems().getAll()) {
                    if (rItem.getId().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(rItem.getId());
                }
            }
        } else if (args.length > 3 && args[0].equalsIgnoreCase("give")) {
            String[] params = {"age=", "kills=", "blocks=", "fish=", "repairs=", "owner="};
            for (String p : params) {
                if (p.startsWith(args[args.length - 1].toLowerCase())) completions.add(p);
            }
        }
        return completions;
    }
}
