package dev.raritycore.affix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable definition of a single affix loaded from {@code affixes.yml}.
 */
public final class Affix {

    private final String id;
    private final String display;          // MiniMessage
    private final String description;     // MiniMessage
    private final @Nullable String attribute;       // Bukkit Attribute name
    private final @Nullable String operation;       // AttributeModifier.Operation name
    private final double value;
    private final @Nullable String slotGroup;       // EquipmentSlotGroup name

    private final @Nullable String secondaryAttribute;
    private final @Nullable String secondaryOperation;
    private final double secondaryValue;

    public Affix(@NotNull String id,
                 @NotNull String display,
                 @NotNull String description,
                 @Nullable String attribute,
                 @Nullable String operation,
                 double value,
                 @Nullable String slotGroup,
                 @Nullable String secondaryAttribute,
                 @Nullable String secondaryOperation,
                 double secondaryValue) {
        this.id = id;
        this.display = display;
        this.description = description;
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
        this.slotGroup = slotGroup;
        this.secondaryAttribute = secondaryAttribute;
        this.secondaryOperation = secondaryOperation;
        this.secondaryValue = secondaryValue;
    }

    @NotNull  public String getId()          { return id; }
    @NotNull  public String getDisplay()     { return display; }
    @NotNull  public String getDescription() { return description; }
    @Nullable public String getAttribute()   { return attribute; }
    @Nullable public String getOperation()   { return operation; }
    public    double getValue()              { return value; }
    @Nullable public String getSlotGroup()   { return slotGroup; }
    @Nullable public String getSecondaryAttribute()  { return secondaryAttribute; }
    @Nullable public String getSecondaryOperation()  { return secondaryOperation; }
    public    double getSecondaryValue()             { return secondaryValue; }

    public boolean hasRealAttribute() { return attribute != null && operation != null; }
}
