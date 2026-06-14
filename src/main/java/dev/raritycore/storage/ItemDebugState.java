package dev.raritycore.storage;

import dev.raritycore.identity.EvolutionStage;

/**
 * Stores forced overrides set by admins for testing and debugging purposes.
 */
public class ItemDebugState {
    
    private Double forcedResonance = null;
    private EvolutionStage.Stage forcedEvolution = null;
    private String forcedLegacyTitle = null;

    public ItemDebugState() {}

    public Double getForcedResonance() {
        return forcedResonance;
    }

    public void setForcedResonance(Double forcedResonance) {
        this.forcedResonance = forcedResonance;
    }

    public EvolutionStage.Stage getForcedEvolution() {
        return forcedEvolution;
    }

    public void setForcedEvolution(EvolutionStage.Stage forcedEvolution) {
        this.forcedEvolution = forcedEvolution;
    }

    public String getForcedLegacyTitle() {
        return forcedLegacyTitle;
    }

    public void setForcedLegacyTitle(String forcedLegacyTitle) {
        this.forcedLegacyTitle = forcedLegacyTitle;
    }
}
