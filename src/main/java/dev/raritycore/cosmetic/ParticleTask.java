package dev.raritycore.cosmetic;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.set.SetBonusManager;
import dev.raritycore.util.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs on the main thread every N ticks, spawning cosmetic particles around
 * players who have at least one active set piece equipped.
 *
 * <p>The particle type/density is determined by the highest-rarity set piece
 * currently worn by the player.
 */
public final class ParticleTask extends BukkitRunnable {

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final SetBonusManager setBonusManager;

    public ParticleTask(RarityCorePlugin plugin,
                        ConfigManager configManager,
                        SetBonusManager setBonusManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.setBonusManager = setBonusManager;
    }

    @Override
    public void run() {
        if (!configManager.isParticlesOnSetActive()) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!setBonusManager.hasAnySetPieceEquipped(player)) continue;

            // Find the highest rarity among equipped set pieces
            RarityTier highestRarity = findHighestEquippedRarity(player);
            if (highestRarity == null) continue;

            Particle particle = highestRarity.getParticle();
            int density = highestRarity.getParticleDensity();

            if (particle == null || density <= 0) continue;

            spawnParticles(player, particle, density);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private RarityTier findHighestEquippedRarity(Player player) {
        RarityTier highest = null;
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack held = player.getInventory().getItemInMainHand();

        ItemStack[] toCheck = new ItemStack[armor.length + 2];
        System.arraycopy(armor, 0, toCheck, 0, armor.length);
        toCheck[armor.length]     = held;
        toCheck[armor.length + 1] = player.getInventory().getItemInOffHand();

        for (ItemStack item : toCheck) {
            if (item == null || item.getType().isAir()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            // Only consider items that have a set ID (part of a set)
            String setId = meta.getPersistentDataContainer()
                    .get(ItemUtil.KEY_SET_ID, PersistentDataType.STRING);
            if (setId == null) continue;

            String rarityName = meta.getPersistentDataContainer()
                    .get(ItemUtil.KEY_RARITY, PersistentDataType.STRING);
            if (rarityName == null) continue;

            RarityTier r = plugin.getRegistries().getRarities().get(rarityName.toLowerCase());
            if (r != null && (highest == null || r.getTier() > highest.getTier())) {
                highest = r;
            }
        }
        return highest;
    }

    private void spawnParticles(Player player, Particle particle, int density) {
        Location loc = player.getLocation().add(0, 1.0, 0);
        try {
            // Spawn particles in a small ring around the player
            for (int i = 0; i < density; i++) {
                double angle = (2 * Math.PI / density) * i;
                double x = Math.cos(angle) * 0.5;
                double z = Math.sin(angle) * 0.5;
                loc.getWorld().spawnParticle(particle,
                        loc.clone().add(x, 0, z),
                        1, 0, 0, 0, 0);
            }
        } catch (Exception e) {
            // Silently ignore any particle-related errors (e.g. unsupported particle type)
        }
    }
}
