package dev.raritycore.storage;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single event in an item's quality history.
 */
public final class QualityHistoryEntry {

    private final String qualityId;
    private final long timestamp;
    private final String reason;

    public QualityHistoryEntry(@NotNull String qualityId, long timestamp, @NotNull String reason) {
        this.qualityId = qualityId;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    @NotNull
    public String getQualityId() {
        return qualityId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NotNull
    public String getReason() {
        return reason;
    }
}
