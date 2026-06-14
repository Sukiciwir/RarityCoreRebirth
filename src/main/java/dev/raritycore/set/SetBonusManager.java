package dev.raritycore.set;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.config.MessagesConfig;
import dev.raritycore.util.ColorUtil;
import dev.raritycore.util.ItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Loads set definitions from {@code sets.yml} and manages active set bonuses
 * per player via potion effects. Listens for armor equip/unequip events.
 */
public final class SetBonusManager implements Listener {

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, SetBonus> sets = new LinkedHashMap<>();

    /** Tracks which sets are currently active for each player. */
    private final Map<UUID, Set<String>> activeSetsByPlayer = new HashMap<>();

    public SetBonusManager(@NotNull RarityCorePlugin plugin,
                           @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // ─── Load ──────────────────────────────────────────────────────────────────

    public void load() {
        sets.clear();
        File file = new File(plugin.getDataFolder(), "sets.yml");
        if (!file.exists()) plugin.saveResource("sets.yml", false);

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(id);
            if (sec == null) continue;

            List<String> pieceIds = sec.getStringList("pieces");
            List<String> bonusDesc = sec.getStringList("bonus-description");

            // Parse potion effect
            ConfigurationSection effectSec = sec.getConfigurationSection("effect");
            PotionEffectType effectType = null;
            int amplifier = 0;
            boolean ambient = false, showParticles = false, showIcon = true;
            if (effectSec != null) {
                String typeName = effectSec.getString("type");
                if (typeName != null) {
                    effectType = PotionEffectType.getByName(typeName);
                    if (effectType == null) {
                        plugin.getLogger().warning("Unknown potion effect '" + typeName
                                + "' for set '" + id + "'");
                    }
                }
                amplifier = effectSec.getInt("amplifier", 0);
                ambient = effectSec.getBoolean("ambient", false);
                showParticles = effectSec.getBoolean("show-particles", false);
                showIcon = effectSec.getBoolean("show-icon", true);
            }

            double cropBonus = sec.getDouble("crop-drop-bonus", 0.0);

            sets.put(id, new SetBonus(id,
                    sec.getString("display-name", id),
                    sec.getInt("required-pieces", 4),
                    pieceIds, bonusDesc,
                    effectType, amplifier, ambient, showParticles, showIcon,
                    cropBonus));
        }

        plugin.getLogger().info("Loaded " + sets.size() + " set(s) from sets.yml.");
    }

    // ─── Events ────────────────────────────────────────────────────────────────
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin,
                () -> updatePlayerSets(player));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin,
                () -> updatePlayerSets(player));
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin,
                () -> updatePlayerSets(player));
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        plugin.getServer().getScheduler().runTask(plugin,
                () -> updatePlayerSets(event.getPlayer()));
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin,
                () -> updatePlayerSets(event.getPlayer()));
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin,
                () -> updatePlayerSets(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Remove all set effects on quit so they don't persist
        clearAllSetEffects(event.getPlayer());
        activeSetsByPlayer.remove(uuid);
    }

    /** Handles the Farmer set's crop drop bonus. */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Set<String> active = activeSetsByPlayer.getOrDefault(player.getUniqueId(), Set.of());
        for (String setId : active) {
            SetBonus set = sets.get(setId);
            if (set != null && set.getCropDropBonus() > 0) {
                // Add bonus drops (10% extra → 1.1x multiplier applied via random chance)
                if (Math.random() < set.getCropDropBonus()) {
                    Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand());
                    for (ItemStack drop : drops) {
                        if (!drop.getType().isAir()) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                        }
                    }
                }
            }
        }
    }

    // ─── Core Logic ────────────────────────────────────────────────────────────

    /** Re-evaluates all sets for a player and applies/removes effects as needed. */
    public void updatePlayerSets(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> previouslyActive = activeSetsByPlayer.getOrDefault(uuid, new HashSet<>());
        Set<String> nowActive = new HashSet<>();

        // Count set pieces currently equipped
        Map<String, Integer> pieceCounts = new HashMap<>();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack held = player.getInventory().getItemInMainHand();

        List<ItemStack> toCheck = new ArrayList<>(Arrays.asList(armor));
        toCheck.add(held);
        toCheck.add(player.getInventory().getItemInOffHand());

        for (ItemStack item : toCheck) {
            if (item == null || item.getType().isAir()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String setId = meta.getPersistentDataContainer()
                    .get(ItemUtil.KEY_SET_ID, PersistentDataType.STRING);
            if (setId != null) {
                pieceCounts.merge(setId, 1, Integer::sum);
            }
        }

        // Determine which sets are now complete
        for (Map.Entry<String, SetBonus> entry : sets.entrySet()) {
            String setId = entry.getKey();
            SetBonus set = entry.getValue();
            int count = pieceCounts.getOrDefault(setId, 0);
            if (count >= set.getRequiredPieces()) {
                nowActive.add(setId);
            }
        }

        // Apply newly activated sets
        for (String setId : nowActive) {
            if (!previouslyActive.contains(setId)) {
                applySetEffect(player, sets.get(setId));
                MessagesConfig msgs = configManager.getMessages();
                player.sendMessage(msgs.get("set-bonus-activated",
                        "set_name", ColorUtil.strip(sets.get(setId).getDisplayName())));
            }
        }

        // Remove deactivated sets
        for (String setId : previouslyActive) {
            if (!nowActive.contains(setId)) {
                removeSetEffect(player, sets.get(setId));
                MessagesConfig msgs = configManager.getMessages();
                player.sendMessage(msgs.get("set-bonus-deactivated",
                        "set_name", ColorUtil.strip(sets.get(setId).getDisplayName())));
            }
        }

        activeSetsByPlayer.put(uuid, nowActive);
    }

    /** Returns the set of active set IDs for a player (may be empty). */
    @NotNull
    public Set<String> getActiveSets(@NotNull Player player) {
        return activeSetsByPlayer.getOrDefault(player.getUniqueId(), Set.of());
    }

    /** Returns true if the player has at least one set piece equipped. */
    public boolean hasAnySetPieceEquipped(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        return !activeSetsByPlayer.getOrDefault(uuid, Set.of()).isEmpty();
    }

    @Nullable
    public SetBonus getSet(@NotNull String id) { return sets.get(id); }

    @NotNull
    public Collection<SetBonus> getAll() { return Collections.unmodifiableCollection(sets.values()); }

    // ─── Private Helpers ───────────────────────────────────────────────────────

    private void applySetEffect(@NotNull Player player, @Nullable SetBonus set) {
        if (set == null || set.getEffectType() == null) return;
        player.addPotionEffect(new PotionEffect(
                set.getEffectType(),
                Integer.MAX_VALUE,
                set.getAmplifier(),
                set.isAmbient(),
                set.isShowParticles(),
                set.isShowIcon()
        ));
    }

    private void removeSetEffect(@NotNull Player player, @Nullable SetBonus set) {
        if (set == null || set.getEffectType() == null) return;
        player.removePotionEffect(set.getEffectType());
    }

    private void clearAllSetEffects(@NotNull Player player) {
        Set<String> active = activeSetsByPlayer.getOrDefault(player.getUniqueId(), Set.of());
        for (String setId : active) {
            removeSetEffect(player, sets.get(setId));
        }
    }
}
