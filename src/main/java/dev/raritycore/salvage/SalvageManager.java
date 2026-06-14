package dev.raritycore.salvage;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.rarity.RarityTier;
import dev.raritycore.util.ColorUtil;
import dev.raritycore.util.GlowUtil;
import dev.raritycore.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the salvage mechanic: dismantling a rarity item into fragment stacks.
 * Output is defined in {@code salvage.yml}.
 */
public final class SalvageManager {

    private final RarityCorePlugin plugin;
    private FileConfiguration salvageCfg;

    public SalvageManager(@NotNull RarityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "salvage.yml");
        if (!file.exists()) plugin.saveResource("salvage.yml", false);
        salvageCfg = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Computes the salvage output for an item of the given rarity.
     *
     * @return list of ItemStacks to give to the player, or empty if nothing defined
     */
    @NotNull
    public List<ItemStack> computeSalvageOutput(@NotNull RarityTier rarity) {
        List<ItemStack> output = new ArrayList<>();
        List<?> entries = salvageCfg.getList("salvage." + rarity.getId().toLowerCase());
        if (entries == null) return output;

        Random rng = ThreadLocalRandom.current();
        for (Object obj : entries) {
            ConfigurationSection sec;
            if (obj instanceof ConfigurationSection cs) {
                sec = cs; 
            } else if (obj instanceof java.util.Map<?,?> map) {
        sec = mapToSection(map);
    } else {
        continue;
    }
            if (sec == null) continue;

            String matName = sec.getString("material", "DIRT");
            Material mat;
            try { mat = Material.valueOf(matName.toUpperCase()); }
            catch (IllegalArgumentException e) { continue; }

            int amount = parseAmount(sec.getString("amount", "1"), rng);
            String name = sec.getString("display-name", "<gray>Fragment");
            List<String> lore = sec.getStringList("lore");

            ItemStack frag = new ItemStack(mat, amount);
            ItemMeta meta = frag.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtil.parse(name));
                List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(ColorUtil.parse(line));
                }
                meta.lore(loreComponents);
                GlowUtil.applyGlow(meta);
                meta.getPersistentDataContainer()
                        .set(ItemUtil.KEY_FRAGMENT, PersistentDataType.STRING, rarity.getId());
                frag.setItemMeta(meta);
            }
            output.add(frag);
        }
        return output;
    }

    private int parseAmount(String amountStr, Random rng) {
        if (amountStr.contains("-")) {
            String[] parts = amountStr.split("-");
            try {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return min + rng.nextInt(max - min + 1);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        try { return Integer.parseInt(amountStr.trim()); }
        catch (NumberFormatException e) { return 1; }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private ConfigurationSection mapToSection(java.util.Map<?,?> map) {
        org.bukkit.configuration.MemoryConfiguration mem =
                new org.bukkit.configuration.MemoryConfiguration();
        for (java.util.Map.Entry<?,?> entry : map.entrySet()) {
            mem.set(entry.getKey().toString(), entry.getValue());
        }
        return mem;
    }
}
