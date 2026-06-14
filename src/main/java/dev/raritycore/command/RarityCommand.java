package dev.raritycore.command;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.config.MessagesConfig;
import dev.raritycore.config.Registries;
import dev.raritycore.gui.GUIManager;
import dev.raritycore.rarity.ItemRegistry;
import dev.raritycore.rarity.RarityItem;
import dev.raritycore.rarity.RarityItemFactory;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.util.ColorUtil;
import dev.raritycore.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Main /rarity command handler with tab completion.
 *
 * Sub-commands:
 *   reload, give, info, collection, salvage, upgrade, gacha, stats, help
 */
public final class RarityCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "inspect", "collection", "salvage", "upgrade", "gacha", "stats", "help"
    );

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final Registries registries;
    private final ItemRegistry registry;
    private final RarityItemFactory factory;
    private final GUIManager guiManager;

    public RarityCommand(@NotNull RarityCorePlugin plugin,
                         @NotNull ConfigManager configManager,
                         @NotNull Registries registries,
                         @NotNull RarityItemFactory factory,
                         @NotNull GUIManager guiManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registries = registries;
        this.registry = registries.getItems();
        this.factory = factory;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        MessagesConfig msgs = configManager.getMessages();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            // ── /rarity reload ────────────────────────────────────────────────
            case "reload" -> {
                if (!sender.hasPermission("raritycore.admin.reload")) {
                    sender.sendMessage(msgs.get("no-permission"));
                    return true;
                }
                configManager.reload();
                registries.load();
                sender.sendMessage(msgs.get("reload-success"));
            }



            // ── /rarity inspect ──────────────────────────────────────────────────
            case "inspect" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(msgs.get("player-only"));
                    return true;
                }
                if (!player.hasPermission("raritycore.player.inspect")) {
                    player.sendMessage(msgs.get("no-permission"));
                    return true;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                if (!ItemUtil.isRarityItem(held)) {
                    player.sendMessage(msgs.get("not-rarity-item"));
                    return true;
                }
                sendItemInspect(player, held);
            }

            // ── /rarity collection [player] ───────────────────────────────────
            case "collection" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(msgs.get("player-only"));
                    return true;
                }
                if (!player.hasPermission("raritycore.player.collection")) {
                    player.sendMessage(msgs.get("no-permission"));
                    return true;
                }
                guiManager.openCollection(player);
            }

            // ── /rarity salvage ───────────────────────────────────────────────
            case "salvage" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(msgs.get("player-only"));
                    return true;
                }
                if (!player.hasPermission("raritycore.player.salvage")) {
                    player.sendMessage(msgs.get("no-permission"));
                    return true;
                }
                guiManager.openSalvage(player);
            }

            // ── /rarity upgrade ───────────────────────────────────────────────
            case "upgrade" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(msgs.get("player-only"));
                    return true;
                }
                if (!player.hasPermission("raritycore.player.upgrade")) {
                    player.sendMessage(msgs.get("no-permission"));
                    return true;
                }
                guiManager.openUpgrade(player);
            }

            // ── /rarity gacha ─────────────────────────────────────────────────
            case "gacha" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(msgs.get("player-only"));
                    return true;
                }
                if (!player.hasPermission("raritycore.player.gacha")) {
                    player.sendMessage(msgs.get("no-permission"));
                    return true;
                }
                guiManager.openGacha(player);
            }

            // ── /rarity stats ─────────────────────────────────────────────────
            case "stats" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(msgs.get("player-only"));
                    return true;
                }
                if (!player.hasPermission("raritycore.player.stats")) {
                    player.sendMessage(msgs.get("no-permission"));
                    return true;
                }
                guiManager.openStats(player);
            }

            // ── /rarity help ──────────────────────────────────────────────────
            case "help" -> sendHelp(sender);

            default -> sender.sendMessage(msgs.get("unknown-command"));
        }
        return true;
    }

    // ─── Tab Completion ────────────────────────────────────────────────────────

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        return List.of();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void sendItemInspect(Player player, ItemStack held) {
        ItemMeta meta = held.getItemMeta();
        if (meta == null) return;

        java.util.UUID itemUuid = plugin.getStorageManager().getMigrationManager().migrateOrGetUUID(held);
        if (itemUuid == null) {
            player.sendMessage("§cInvalid RarityCore item.");
            return;
        }

        dev.raritycore.storage.ItemStatistics stats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(itemUuid);

        String itemName = "Unknown Item";
        if (meta.hasDisplayName()) {
            itemName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(meta.displayName()).replaceAll("§x(§[0-9a-f]){6}", "");
        }

        String rarityId = ItemUtil.getRarity(meta);
        String rarityName = rarityId != null ? capitalize(rarityId) : "Unknown";
        
        String affixId = ItemUtil.getAffix(meta);
        dev.raritycore.affix.Affix affix = affixId != null && plugin.getAffixManager() != null ? plugin.getAffixManager().getAffix(affixId) : null;

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

        player.sendMessage("§8====================================");
        player.sendMessage("§6" + itemName);
        player.sendMessage("§8UUID: §7" + itemUuid.toString());
        player.sendMessage("");
        player.sendMessage("§7Rarity: §f" + rarityName);
        player.sendMessage("§7Current Quality: §f" + currentQuality);
        player.sendMessage("§7Original Quality: §f" + originalQuality);
        
        List<dev.raritycore.trait.TraitInstance> traits = ItemUtil.getTraits(meta);
        if (!traits.isEmpty()) {
            player.sendMessage("§7Traits: §f" + traits.size());
        }
        if (affix != null) {
            player.sendMessage("§7Affix: §f" + affix.getDisplay());
        }
        player.sendMessage("");
        
        player.sendMessage("§7Kills: §f" + stats.getKills());
        player.sendMessage("§7Blocks Mined: §f" + stats.getBlocksMined());
        player.sendMessage("§7Fish Caught: §f" + stats.getFishCaught());
        
        long age = ((System.currentTimeMillis() - stats.getCreationTimestamp()) / (1000L * 60 * 60 * 24));
        player.sendMessage("§7Age: §f" + age + " days");
        player.sendMessage("");
        
        player.sendMessage("§7Creator: §f" + (stats.getCreatorName() != null ? stats.getCreatorName() : "Unknown"));
        player.sendMessage("§7Current Owner: §f" + (stats.getCurrentMaster() != null ? stats.getCurrentMaster() : "Unknown"));
        
        double res = stats.getDebugState().getForcedResonance() != null ? 
            stats.getDebugState().getForcedResonance() : 
            plugin.getLegacySystem().getResonanceSystem().calculateResonance(stats, player);
        player.sendMessage("§7Resonance: §e" + String.format("%.2f", res));
        
        dev.raritycore.identity.EvolutionStage.Stage stage = stats.getDebugState().getForcedEvolution();
        if (stage == null) stage = new dev.raritycore.identity.EvolutionStage().determineStage(stats);
        if (stage != null && stage != dev.raritycore.identity.EvolutionStage.Stage.NONE) {
            player.sendMessage("§7Evolution: §f" + stage.getTitle());
        }

        String epithet = plugin.getIdentityManager().getEpithetGenerator().generateEpithet(stats);
        if (epithet != null && !epithet.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("§7Epithet: §e\"" + epithet + "\"");
        }
        player.sendMessage("§8====================================");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(
                "<dark_gray>══ <gradient:#FFD700:#9B30FF>RarityCore Commands</gradient> ══\n"
                + "<gray>/rarity reload     <dark_gray>- Reload all configs\n"
                + "<gray>/rarity inspect    <dark_gray>- Inspect held item stats\n"
                + "<gray>/rarity collection <dark_gray>- Open collection log\n"
                + "<gray>/rarity salvage    <dark_gray>- Open salvage workshop\n"
                + "<gray>/rarity upgrade    <dark_gray>- Open upgrade station\n"
                + "<gray>/rarity gacha      <dark_gray>- Open the gacha\n"
                + "<gray>/rarity stats      <dark_gray>- Server statistics"
        ));
    }

    private List<String> filter(List<String> source, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : source) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        }
        return result;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
