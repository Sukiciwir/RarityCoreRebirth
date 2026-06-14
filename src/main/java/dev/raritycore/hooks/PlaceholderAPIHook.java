package dev.raritycore.hooks;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.storage.StorageManager;
import dev.raritycore.util.ItemUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for RarityCore.
 *
 * <p>Available placeholders:
 * <ul>
 *   <li>{@code %raritycore_tier%}        — rarity of held item (e.g. LEGENDARY)</li>
 *   <li>{@code %raritycore_quality%}     — quality % of held item</li>
 *   <li>{@code %raritycore_affix%}       — affix ID of held item (or "none")</li>
 *   <li>{@code %raritycore_collection_common%}    — discovered/total for COMMON</li>
 *   <li>{@code %raritycore_collection_legendary%} — discovered/total for LEGENDARY</li>
 *   <li>(same pattern for all rarities)</li>
 *   <li>{@code %raritycore_pity%}        — current gacha pity counter</li>
 *   <li>{@code %raritycore_kills%}       — kills of held item</li>
 *   <li>{@code %raritycore_epithet%}     — epithet of held item</li>
 *   <li>{@code %raritycore_resonance%}   — resonance of held item</li>
 * </ul>
 */
public final class PlaceholderAPIHook extends PlaceholderExpansion {

    private final RarityCorePlugin plugin;

    public PlaceholderAPIHook(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "raritycore"; }
    @Override
    public @NotNull String getAuthor()     { return "RarityCore"; }
    @Override
    public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override
    public boolean persist() { return true; }

    public void unload() { unregister(); }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "?";

        String stat = params.toLowerCase();

        // ── Gacha pity ────────────────────────────────────────────────────────
        if (stat.equals("pity")) {
            return String.valueOf(plugin.getStorageManager().get(player.getUniqueId()).getPity());
        }

        // ── Collection per rarity ─────────────────────────────────────────────
        if (stat.startsWith("collection_")) {
            String rarityName = stat.substring("collection_".length());
            RarityTier rarity = plugin.getRegistries().getRarities().get(rarityName);
            if (rarity == null) return "?";
            StorageManager.PlayerData data = plugin.getStorageManager().get(player.getUniqueId());
            int discovered = data.getDiscoveryCount(rarityName);
            int total = plugin.getRegistries().getItems().getByRarity(rarity).size();
            return discovered + "/" + total;
        }

        // Determine slot and stat. Format: <stat> (implies mainhand) OR <slot>_<stat>
        ItemStack item = null;
        if (stat.startsWith("offhand_")) {
            item = player.getInventory().getItemInOffHand();
            stat = stat.substring(8);
        } else if (stat.startsWith("helmet_")) {
            item = player.getInventory().getHelmet();
            stat = stat.substring(7);
        } else if (stat.startsWith("chestplate_")) {
            item = player.getInventory().getChestplate();
            stat = stat.substring(11);
        } else if (stat.startsWith("leggings_")) {
            item = player.getInventory().getLeggings();
            stat = stat.substring(9);
        } else if (stat.startsWith("boots_")) {
            item = player.getInventory().getBoots();
            stat = stat.substring(6);
        } else {
            if (stat.startsWith("mainhand_")) stat = stat.substring(9);
            item = player.getInventory().getItemInMainHand();
        }

        if (item == null || !ItemUtil.isRarityItem(item)) return "none";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "none";

        // Resolve generic PDC stats without needing DB
        switch (stat) {
            case "tier", "rarity" -> {
                String r = ItemUtil.getRarity(meta);
                return r != null ? r : "?";
            }
            case "quality" -> {
                Integer q = ItemUtil.getQuality(meta);
                return q != null ? q + "%" : "?";
            }
            case "affix" -> {
                String a = ItemUtil.getAffix(meta);
                return a != null ? a : "none";
            }
            case "trait_1" -> {
                var traits = ItemUtil.getTraits(meta);
                return traits.size() > 0 ? traits.get(0).getTraitId() : "none";
            }
            case "trait_2" -> {
                var traits = ItemUtil.getTraits(meta);
                return traits.size() > 1 ? traits.get(1).getTraitId() : "none";
            }
            case "trait_3" -> {
                var traits = ItemUtil.getTraits(meta);
                return traits.size() > 2 ? traits.get(2).getTraitId() : "none";
            }
        }

        // Require DB stats
        java.util.UUID itemUuid = plugin.getStorageManager().getMigrationManager().migrateOrGetUUID(item);
        if (itemUuid == null) return "0";
        dev.raritycore.storage.ItemStatistics stats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(itemUuid);

        return switch (stat) {
            case "owner" -> stats.getCurrentMaster() != null ? stats.getCurrentMaster() : "none";
            case "kills" -> String.valueOf(stats.getKills());
            case "blocks" -> String.valueOf(stats.getBlocksMined());
            case "fish" -> String.valueOf(stats.getFishCaught());
            case "age" -> String.valueOf(stats.getAgeInDays());
            case "epithet" -> stats.getCachedEpithet() != null ? stats.getCachedEpithet() : "none";
            case "resonance" -> {
                Double forced = stats.getDebugState().getForcedResonance();
                if (forced != null) yield String.format("%.0f%%", forced * 100);
                double res = plugin.getLegacySystem().getResonanceSystem().calculateResonance(stats, player);
                yield String.format("%.0f%%", res * 100);
            }
            case "evolution" -> {
                if (stats.getDebugState().getForcedEvolution() != null) {
                    yield stats.getDebugState().getForcedEvolution().name();
                }
                yield "Dynamic";
            }
            default -> "?";
        };
    }
}
