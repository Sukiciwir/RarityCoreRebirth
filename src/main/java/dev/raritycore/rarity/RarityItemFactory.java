package dev.raritycore.rarity;

import dev.raritycore.RarityCorePlugin;
import dev.raritycore.affix.Affix;
import dev.raritycore.affix.AffixManager;
import dev.raritycore.config.ConfigManager;
import dev.raritycore.quality.QualityGenerator;
import dev.raritycore.util.ColorUtil;
import dev.raritycore.util.GlowUtil;
import dev.raritycore.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link ItemStack} instances from {@link RarityItem} templates,
 * injecting display name, lore, glow, quality, affix, and PDC metadata.
 */
public final class RarityItemFactory {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final RarityCorePlugin plugin;
    private final ConfigManager configManager;
    private final AffixManager affixManager;
    private final dev.raritycore.trait.TraitSystem traitSystem;

    public RarityItemFactory(RarityCorePlugin plugin,
                             ConfigManager configManager,
                             AffixManager affixManager,
                             dev.raritycore.trait.TraitSystem traitSystem) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.affixManager = affixManager;
        this.traitSystem = traitSystem;
    }

    /**
     * Creates a fully-built {@link ItemStack} for the given template.
     *
     * @param template      the item template
     * @param firstOwner    player name (null if admin-spawned without owner)
     * @param forceAffix    specific affix to apply, or null to let config decide
     * @param forceQuality  0 to generate randomly, otherwise 1-100 fixed
     */
    @NotNull
    public ItemStack build(@NotNull RarityItem template,
                           @Nullable String firstOwner,
                           @Nullable String forceAffix,
                           int forceQuality) {

        RarityTier tier = template.getRarity();
        ItemStack stack = new ItemStack(template.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // ── Display Name ──────────────────────────────────────────
        meta.displayName(ColorUtil.parse(template.getDisplayName()));

        // ── Glow ─────────────────────────────────────────────────
        if (tier.isGlow()) {
            GlowUtil.applyGlow(meta);
        }

        // ── Quality ───────────────────────────────────────────────
        int quality = (forceQuality > 0) ? forceQuality : QualityGenerator.generate();

        // ── Affix ─────────────────────────────────────────────────
        Affix affix = null;
        if (template.canHaveAffix() && affixManager != null) {
            if (forceAffix != null) {
                affix = affixManager.getAffix(forceAffix);
            } else if (shouldGrantAffix(template.getRarity())) {
                affix = affixManager.randomAffix();
            }
            if (affix != null) {
                applyAffixModifier(meta, affix);
            }
        }

        // ── Traits ────────────────────────────────────────────────────────────────
        List<dev.raritycore.trait.TraitInstance> instances = new ArrayList<>();
        if (Math.random() <= tier.getTraitChance() && tier.getTraitMax() > 0) {
            int max = tier.getTraitMax();
            int min = tier.getTraitMin();
            int amount = min + (int) (Math.random() * ((max - min) + 1));
            
            String family = getFamily(stack.getType().name());
            List<dev.raritycore.trait.Trait> traits = traitSystem.getManager().rollTraits(family, amount, traitSystem.getConflictManager());
            
            for (dev.raritycore.trait.Trait t : traits) {
                instances.add(new dev.raritycore.trait.TraitInstance(t.getId(), false, 0));
            }
        }

        // ── Lore ──────────────────────────────────────────────────
        int revealState = RevealFlags.FLAG_ALL;
        if (plugin.getGenerationManager().rollHiddenPotential(stack.getType(), tier)) {
            revealState = RevealFlags.FLAG_NONE;
        }
        
        List<Component> loreComponents = buildLore(template, tier, quality, affix, firstOwner, instances, revealState, null);
        meta.lore(loreComponents);

        // ── PDC Metadata ──────────────────────────────────────────
        var pdc = meta.getPersistentDataContainer();
        pdc.set(dev.raritycore.storage.MigrationManager.KEY_REVEAL_STATE, PersistentDataType.INTEGER, revealState);
        
        writePdc(meta, template, quality, affix != null ? affix.getId() : null, firstOwner, instances);

        // ── Flags ─────────────────────────────────────────────────
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        stack.setItemMeta(meta);

        // ── Broadcasts ────────────────────────────────────────────
        if (tier.getId().equals("mythic")) {
            plugin.getBroadcastManager().queueBroadcast(net.kyori.adventure.text.Component.text("§d⚜ " + (firstOwner != null ? firstOwner : "Someone") + " has found a Mythic " + template.getDisplayName() + "!"));
        } else if (tier.getId().equals("divine")) {
            plugin.getBroadcastManager().queueBroadcast(net.kyori.adventure.text.Component.text("§b✦ " + (firstOwner != null ? firstOwner : "Someone") + " has obtained a Divine " + template.getDisplayName() + "!"));
        } else if (tier.getId().equals("ancient")) {
            plugin.getBroadcastManager().queueBroadcast(net.kyori.adventure.text.Component.text("§c☠ " + (firstOwner != null ? firstOwner : "Someone") + " has uncovered an Ancient relic: " + template.getDisplayName() + "!"));
        }

        return stack;
    }

    // ─── Lore Builder ──────────────────────────────────────────────────────────

    private List<Component> buildLore(RarityItem template, RarityTier rarityTier,
                                      int quality, @Nullable Affix affix,
                                      @Nullable String firstOwner,
                                      List<dev.raritycore.trait.TraitInstance> traits,
                                      int revealState,
                                      @Nullable dev.raritycore.storage.ItemStatistics stats) {
        List<Component> lines = new ArrayList<>();

        // Custom item lore
        for (String line : template.getLore()) {
            lines.add(ColorUtil.parse(line));
        }

        lines.add(Component.empty());

        // Rarity line
        if (RevealFlags.hasFlag(revealState, RevealFlags.FLAG_RARITY)) {
            String rarityColor = "<color:" + rarityTier.getDisplayNameColor() + ">";
            lines.add(ColorUtil.parse(
                    "<dark_gray>Rarity: " + rarityColor + rarityTier.getPrefix()
                            + capitalize(template.getRarity().getId()) + "</color>"));
        } else {
            lines.add(ColorUtil.parse("<dark_gray>Rarity: <gray>???"));
        }

        // Quality line
        if (RevealFlags.hasFlag(revealState, RevealFlags.FLAG_QUALITY)) {
            String qName = null;
            String qSymbol = "";
            if (stats != null && stats.getQualityHistory().stream().anyMatch(q -> q.getQualityId().equals("quality_legacy"))) {
                qName = "Soulbound";
                var opt = stats.getQualityHistory().stream().filter(q -> q.getQualityId().equals("quality_legacy")).findFirst();
                if (opt.isPresent()) {
                    String note = opt.get().getReason();
                    if (note != null && note.contains("(")) {
                        qName = note.substring(note.indexOf("(") + 1, note.indexOf(")"));
                    }
                }
                if (stats.getDebugState().getForcedLegacyTitle() != null) qName = stats.getDebugState().getForcedLegacyTitle();
                lines.add(ColorUtil.parse("<dark_gray>Quality: <gold>" + qName + " ⚜"));
            } else {
                dev.raritycore.quality.QualityTier qTier = plugin.getRegistries().getQualities().getQualityForPercentage(quality);
                if (qTier != null) {
                    lines.add(ColorUtil.parse("<dark_gray>Quality: <white>" + qTier.getDisplayName() + " <yellow>" + qTier.getSymbol()));
                } else {
                    lines.add(ColorUtil.parse("<dark_gray>Quality: <white>" + quality + "%"));
                }
            }
        } else {
            lines.add(ColorUtil.parse("<dark_gray>Quality: <gray>???"));
        }

        // Affix line
        if (affix != null) {
            if (RevealFlags.hasFlag(revealState, RevealFlags.FLAG_AFFIXES)) {
                lines.add(ColorUtil.parse("<dark_gray>Affix: " + affix.getDisplay()));
                lines.add(ColorUtil.parse("  " + affix.getDescription()));
            } else {
                lines.add(ColorUtil.parse("<dark_gray>Affix: <gray>???"));
            }
        }

        // Traits
        if (!traits.isEmpty()) {
            lines.add(Component.empty());
            lines.add(ColorUtil.parse("<dark_gray>Traits:"));
            if (RevealFlags.hasFlag(revealState, RevealFlags.FLAG_TRAITS)) {
                for (dev.raritycore.trait.TraitInstance inst : traits) {
                    if (!inst.isDiscovered()) {
                        lines.add(ColorUtil.parse("  <dark_gray>???"));
                    } else {
                        dev.raritycore.trait.Trait t = traitSystem.getManager().get(inst.getTraitId());
                        if (t != null) {
                            lines.add(ColorUtil.parse("  " + t.getDisplayName()));
                            for (String d : t.getDescription()) {
                                lines.add(ColorUtil.parse("    " + d));
                            }
                        }
                    }
                }
            } else {
                lines.add(ColorUtil.parse("  <dark_gray>Hidden Potential..."));
            }
        }

        // Set line
        if (template.getSetId() != null) {
            lines.add(Component.empty());
            lines.add(ColorUtil.parse("<dark_gray>Set: <yellow>" + template.getSetId()
                    .replace("_", " ")
                    .replace("\\b.", Character.toString(Character.toUpperCase(template.getSetId().charAt(0))))));
        }

        // First owner
        if (firstOwner != null) {
            lines.add(Component.empty());
            lines.add(ColorUtil.parse("<dark_gray>First Owner: <white>" + firstOwner));
            lines.add(ColorUtil.parse("<dark_gray>Discovered: <white>"
                    + LocalDate.now().format(DATE_FORMAT)));
        }

        // Stats
        if (stats != null) {
            lines.add(Component.empty());
            String family = getFamily(template.getMaterial().name());
            if (family.equals("PICKAXE") || family.equals("ANY")) {
                if (stats.getBlocksMined() > 0) lines.add(ColorUtil.parse("<dark_gray>Blocks Mined: <white>" + stats.getBlocksMined()));
            }
            if (family.equals("FISHING_ROD") || family.equals("ANY")) {
                if (stats.getFishCaught() > 0) lines.add(ColorUtil.parse("<dark_gray>Fish Caught: <white>" + stats.getFishCaught()));
            }
            if (family.equals("SWORD") || family.equals("AXE") || family.equals("BOW") || family.equals("ANY")) {
                lines.add(ColorUtil.parse("<dark_gray>Kills: <white>" + stats.getKills()));
            }
        }

        return lines;
    }

    public void rebuildLore(ItemStack stack) {
        if (!ItemUtil.isRarityItem(stack)) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        
        var pdc = meta.getPersistentDataContainer();
        String rarityId = pdc.get(ItemUtil.KEY_RARITY, PersistentDataType.STRING);
        String itemId = pdc.get(ItemUtil.KEY_ITEM_ID, PersistentDataType.STRING);
        Integer quality = ItemUtil.getQuality(meta);
        String affixId = pdc.get(ItemUtil.KEY_AFFIX, PersistentDataType.STRING);
        String firstOwner = pdc.get(ItemUtil.KEY_FIRST_OWNER, PersistentDataType.STRING);
        int rawRevealState = pdc.getOrDefault(dev.raritycore.storage.MigrationManager.KEY_REVEAL_STATE, PersistentDataType.INTEGER, RevealFlags.FLAG_ALL);
        int revealState = rawRevealState;
        
        // Migrate old tiered system to bitmask system if needed (1=rarity, 2=quality, 3=traits, 4=affixes)
        if (rawRevealState == 1) revealState = RevealFlags.FLAG_RARITY;
        else if (rawRevealState == 2) revealState = RevealFlags.FLAG_RARITY | RevealFlags.FLAG_QUALITY;
        else if (rawRevealState == 3) revealState = RevealFlags.FLAG_RARITY | RevealFlags.FLAG_QUALITY | RevealFlags.FLAG_TRAITS;
        else if (rawRevealState == 4) revealState = RevealFlags.FLAG_ALL;
        
        RarityItem template = plugin.getRegistries().getItems().get(itemId);
        
        dev.raritycore.rarity.RarityTier rarityTier = plugin.getRegistries().getRarities().get(rarityId);
        
        if (template == null && itemId != null && itemId.startsWith("generated_")) {
            org.bukkit.Material mat = stack.getType();
            String nameStr = mat.name().replace("_", " ");
            StringBuilder sb = new StringBuilder();
            for (String word : nameStr.split(" ")) {
                if (!word.isEmpty()) {
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
                    sb.append(" ");
                }
            }
            String displayName = "<white>" + sb.toString().trim();
            template = new dev.raritycore.rarity.RarityItem(itemId, mat, rarityTier, displayName, java.util.List.of(), null, true);
        }
        
        if (template == null || rarityTier == null) return;
        
        Affix affix = affixId != null ? affixManager.getAffix(affixId) : null;
        List<dev.raritycore.trait.TraitInstance> traits = ItemUtil.getTraits(meta);
        
        dev.raritycore.storage.ItemStatistics stats = null;
        String uuidStr = pdc.get(dev.raritycore.storage.MigrationManager.KEY_ITEM_UUID, PersistentDataType.STRING);
        if (uuidStr != null) {
            try {
                stats = plugin.getStorageManager().getCacheManager().getCachedOrCreate(java.util.UUID.fromString(uuidStr));
            } catch (Exception ignored) {}
        }
        
        if (RevealFlags.hasFlag(revealState, RevealFlags.FLAG_RARITY)) {
            String name = template.getDisplayName();
            if (stats != null) {
                if (stats.getQualityHistory().stream().anyMatch(q -> q.getQualityId().equals("quality_legacy"))) {
                    String forcedTitle = stats.getDebugState().getForcedLegacyTitle();
                    if (forcedTitle != null) name = "<gold>⚜ " + forcedTitle;
                    else if (stats.getCachedEpithet() != null) name = "<gold>⚜ " + stats.getCachedEpithet();
                    else name = "<gold>⚜ " + name;
                } else {
                    dev.raritycore.identity.EvolutionStage.Stage stage = stats.getDebugState().getForcedEvolution();
                    if (stage == null) stage = new dev.raritycore.identity.EvolutionStage().determineStage(stats);
                    if (stage != null && stage != dev.raritycore.identity.EvolutionStage.Stage.NONE) {
                        name = "⚔ " + stage.getTitle() + " " + name;
                    }
                }
            }
            meta.displayName(ColorUtil.parse(name));
        } else {
            meta.displayName(ColorUtil.parse("<gray>Unknown " + capitalize(stack.getType().name().replace("_", " "))));
        }
        
        List<Component> lore = buildLore(template, rarityTier, quality != null ? quality : 80, affix, firstOwner, traits, revealState, stats);
        meta.lore(lore);
        stack.setItemMeta(meta);
    }

    // ─── Affix Attribute Modifier ──────────────────────────────────────────────

    private void applyAffixModifier(ItemMeta meta, Affix affix) {
        if (affix.getAttribute() == null) return;
        try {
            Attribute attribute = Attribute.valueOf(affix.getAttribute());
            AttributeModifier.Operation operation =
                    AttributeModifier.Operation.valueOf(affix.getOperation());
            EquipmentSlotGroup slotGroup = parseSlotGroup(affix.getSlotGroup());

            NamespacedKey key = new NamespacedKey(plugin, "raritycore_affix_" + affix.getId());
            AttributeModifier modifier = new AttributeModifier(key, affix.getValue(), operation, slotGroup);
            meta.addAttributeModifier(attribute, modifier);

            // Secondary modifier (e.g. Heavy affix has speed penalty)
            if (affix.getSecondaryAttribute() != null) {
                Attribute secAttr = Attribute.valueOf(affix.getSecondaryAttribute());
                AttributeModifier.Operation secOp =
                        AttributeModifier.Operation.valueOf(affix.getSecondaryOperation());
                NamespacedKey secKey = new NamespacedKey(plugin, "raritycore_affix_sec_" + affix.getId());
                AttributeModifier secMod = new AttributeModifier(secKey, affix.getSecondaryValue(), secOp, slotGroup);
                meta.addAttributeModifier(secAttr, secMod);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid attribute/operation in affix '" + affix.getId() + "': " + e.getMessage());
        }
    }

    private EquipmentSlotGroup parseSlotGroup(String group) {
        if (group == null) return EquipmentSlotGroup.ANY;
        return switch (group.toUpperCase()) {
            case "MAINHAND" -> EquipmentSlotGroup.MAINHAND;
            case "OFFHAND"  -> EquipmentSlotGroup.OFFHAND;
            case "HEAD"     -> EquipmentSlotGroup.HEAD;
            case "CHEST"    -> EquipmentSlotGroup.CHEST;
            case "LEGS"     -> EquipmentSlotGroup.LEGS;
            case "FEET"     -> EquipmentSlotGroup.FEET;
            case "ARMOR"    -> EquipmentSlotGroup.ARMOR;
            case "HAND"     -> EquipmentSlotGroup.HAND;
            default         -> EquipmentSlotGroup.ANY;
        };
    }

    // ─── PDC Writer ────────────────────────────────────────────────────────────

    private void writePdc(ItemMeta meta, RarityItem template,
                          int quality, @Nullable String affixId,
                          @Nullable String firstOwner,
                          List<dev.raritycore.trait.TraitInstance> traits) {
        var pdc = meta.getPersistentDataContainer();
        pdc.set(ItemUtil.KEY_RARITY,    PersistentDataType.STRING, template.getRarity().getId());
        pdc.set(ItemUtil.KEY_ITEM_ID,   PersistentDataType.STRING, template.getId());
        pdc.set(ItemUtil.KEY_QUALITY,   PersistentDataType.INTEGER, quality);
        if (affixId != null) {
            pdc.set(ItemUtil.KEY_AFFIX, PersistentDataType.STRING, affixId);
        }
        if (template.getSetId() != null) {
            pdc.set(ItemUtil.KEY_SET_ID, PersistentDataType.STRING, template.getSetId());
        }
        if (firstOwner != null) {
            pdc.set(ItemUtil.KEY_FIRST_OWNER, PersistentDataType.STRING, firstOwner);
            pdc.set(ItemUtil.KEY_FIRST_DATE,  PersistentDataType.STRING,
                    LocalDate.now().format(DATE_FORMAT));
        }
        
        ItemUtil.setTraits(meta, traits);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private boolean shouldGrantAffix(RarityTier rarity) {
        // Respect minimum-rarity from config
        int minTier = configManager.getAffixMinimumRarity();
        if (rarity.getTier() < minTier) return false;
        double chance = configManager.getAffixChance();
        return Math.random() < chance;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
    
    private String getFamily(String matName) {
        if (matName.endsWith("_SWORD")) return "SWORD";
        if (matName.endsWith("_AXE")) return "AXE";
        if (matName.endsWith("_PICKAXE")) return "PICKAXE";
        if (matName.endsWith("_HELMET") || matName.endsWith("_CHESTPLATE") || matName.endsWith("_LEGGINGS") || matName.endsWith("_BOOTS")) return "ARMOR";
        if (matName.equals("BOW") || matName.equals("CROSSBOW")) return "BOW";
        if (matName.equals("FISHING_ROD")) return "FISHING_ROD";
        return "ANY";
    }
    
    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }
}
