package dev.raritycore.trait;

import java.util.Collection;

public class TraitConflictManager {
    
    /**
     * Checks if a candidate trait is incompatible with any of the already assigned traits.
     */
    public boolean isConflict(Trait candidate, Collection<Trait> assigned) {
        for (Trait existing : assigned) {
            // Check if existing trait declares candidate as incompatible
            if (existing.getIncompatible().contains(candidate.getId())) {
                return true;
            }
            // Check if candidate trait declares existing as incompatible
            if (candidate.getIncompatible().contains(existing.getId())) {
                return true;
            }
        }
        return false;
    }
}
