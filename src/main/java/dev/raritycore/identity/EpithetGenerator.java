package dev.raritycore.identity;

import dev.raritycore.storage.ItemStatistics;
import java.util.List;

/**
 * Generates procedural flavor text based on an item's history.
 */
public final class EpithetGenerator {

    public String generateEpithet(ItemStatistics stats) {
        StringBuilder epithet = new StringBuilder();

        // 1. Creation context
        if (stats.getCreationTimestamp() > 0) {
            epithet.append("Forged long ago. ");
        } else {
            epithet.append("Its origins have faded with time. ");
        }

        // 2. Battle history
        if (stats.getKills() > 10000) {
            epithet.append("Tempered through countless battles. ");
        } else if (stats.getKills() > 1000) {
            epithet.append("It has seen its fair share of bloodshed. ");
        }

        // 3. Ownership history
        List<String> owners = stats.getOwnersHistory();
        if (owners.size() > 5) {
            epithet.append("Passed through many hands. ");
        } else if (owners.size() == 1) {
            epithet.append("Loyal to a single master. ");
        }

        // 4. Repairs
        if (stats.getRepairsCount() > 20) {
            epithet.append("Shattered and rebuilt, yet unbroken.");
        }

        return epithet.toString().trim();
    }
}
