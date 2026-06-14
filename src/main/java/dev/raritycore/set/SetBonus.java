package dev.raritycore.set;

import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable definition of an equipment set bonus loaded from {@code sets.yml}.
 */
public final class SetBonus {

    private final String id;
    private final String displayName;          // MiniMessage
    private final int requiredPieces;
    private final List<String> pieceIds;       // Item IDs from items.yml
    private final List<String> bonusDescription; // MiniMessage lines

    // Potion effect applied when the set is complete
    private final @Nullable PotionEffectType effectType;
    private final int amplifier;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;

    // Extra bonuses handled in code (e.g. Farmer crop bonus)
    private final double cropDropBonus;

    public SetBonus(@NotNull String id,
                    @NotNull String displayName,
                    int requiredPieces,
                    @NotNull List<String> pieceIds,
                    @NotNull List<String> bonusDescription,
                    @Nullable PotionEffectType effectType,
                    int amplifier,
                    boolean ambient,
                    boolean showParticles,
                    boolean showIcon,
                    double cropDropBonus) {
        this.id = id;
        this.displayName = displayName;
        this.requiredPieces = requiredPieces;
        this.pieceIds = List.copyOf(pieceIds);
        this.bonusDescription = List.copyOf(bonusDescription);
        this.effectType = effectType;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.cropDropBonus = cropDropBonus;
    }

    @NotNull  public String getId()               { return id; }
    @NotNull  public String getDisplayName()       { return displayName; }
    public    int getRequiredPieces()              { return requiredPieces; }
    @NotNull  public List<String> getPieceIds()    { return pieceIds; }
    @NotNull  public List<String> getBonusDescription() { return bonusDescription; }
    @Nullable public PotionEffectType getEffectType()  { return effectType; }
    public    int getAmplifier()                   { return amplifier; }
    public    boolean isAmbient()                  { return ambient; }
    public    boolean isShowParticles()            { return showParticles; }
    public    boolean isShowIcon()                 { return showIcon; }
    public    double getCropDropBonus()            { return cropDropBonus; }
}
