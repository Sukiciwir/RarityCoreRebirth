package dev.raritycore.trait;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TraitSynergyManager {

    /**
     * Checks an item's current traits and returns any synergy effects that apply.
     * For example, "BLOODLUST" + "BERSERKER" might return a list containing "HEAL:1" and "BUFF:HASTE:60:1".
     * In a full implementation, these combinations would be defined in synergies.yml or traits.yml.
     */
    public List<String> getSynergyEffects(List<TraitInstance> instances) {
        List<String> effects = new ArrayList<>();
        Set<String> ids = instances.stream().map(TraitInstance::getTraitId).collect(Collectors.toSet());
        
        // Example hardcoded synergy
        if (ids.contains("bloodlust") && ids.contains("berserker")) {
            // "Bloodreaver" synergy: additional small heal on kill
            effects.add("HEAL:1");
        }
        
        if (ids.contains("lucky_miner") && ids.contains("gem_hunter")) {
            // "Prospector's Soul" synergy
            effects.add("BUFF:FAST_DIGGING:100:1");
        }
        
        return effects;
    }
}
