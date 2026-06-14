package dev.raritycore.trait;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

/**
 * Encapsulates the context of an event triggering a trait.
 */
public class EventContext {
    private Entity targetEntity;
    private Block targetBlock;
    private double value; // e.g., damage amount, or modified drop amount
    
    public EventContext() {}
    
    public EventContext withTargetEntity(Entity entity) {
        this.targetEntity = entity;
        return this;
    }
    
    public EventContext withTargetBlock(Block block) {
        this.targetBlock = block;
        return this;
    }
    
    public EventContext withValue(double value) {
        this.value = value;
        return this;
    }

    public Entity getTargetEntity() { return targetEntity; }
    public Block getTargetBlock() { return targetBlock; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
