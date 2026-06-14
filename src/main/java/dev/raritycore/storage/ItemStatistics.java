package dev.raritycore.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data structure representing an item's history and statistics.
 * This is primarily stored in SQLite and cached by ItemCacheManager.
 */
public final class ItemStatistics {

    private final UUID itemUuid;
    
    private String creatorName;
    private String originalOwnerName;
    private List<String> ownersHistory = new ArrayList<>();
    
    private String currentMaster;
    private int successorProgress;
    
    private long creationTimestamp;
    private String creationSeason;
    private String creationEvent;
    
    private int repairsCount;
    
    private int kills;
    private int blocksMined;
    private int fishCaught;
    private int damageDealt;
    private int damageAbsorbed;
    private int distanceTraveled;
    
    private String cachedEpithet;
    private boolean isDestroyed;
    
    private ItemDebugState debugState = new ItemDebugState();
    
    private final List<QualityHistoryEntry> qualityHistory = new ArrayList<>();

    public ItemStatistics(@NotNull UUID itemUuid) {
        this.itemUuid = itemUuid;
        this.creationTimestamp = System.currentTimeMillis();
    }

    @NotNull public UUID getItemUuid() { return itemUuid; }

    @Nullable public String getCreatorName() { return creatorName; }
    public void setCreatorName(@Nullable String creatorName) { this.creatorName = creatorName; }

    @Nullable public String getOriginalOwnerName() { return originalOwnerName; }
    public void setOriginalOwnerName(@Nullable String originalOwnerName) { this.originalOwnerName = originalOwnerName; }

    @NotNull public List<String> getOwnersHistory() { return ownersHistory; }
    public void setOwnersHistory(@NotNull List<String> ownersHistory) { this.ownersHistory = new ArrayList<>(ownersHistory); }
    public void addOwner(@NotNull String owner) {
        if (!ownersHistory.contains(owner)) {
            ownersHistory.add(owner);
        }
    }

    @Nullable public String getCurrentMaster() { return currentMaster != null ? currentMaster : originalOwnerName; }
    public void setCurrentMaster(@Nullable String currentMaster) { this.currentMaster = currentMaster; }

    public int getSuccessorProgress() { return successorProgress; }
    public void setSuccessorProgress(int successorProgress) { this.successorProgress = successorProgress; }
    public void addSuccessorProgress(int amount) { this.successorProgress += amount; }

    public long getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(long creationTimestamp) { this.creationTimestamp = creationTimestamp; }
    public long getAgeInDays() { return (System.currentTimeMillis() - creationTimestamp) / (1000 * 60 * 60 * 24); }

    @Nullable public String getCreationSeason() { return creationSeason; }
    public void setCreationSeason(@Nullable String creationSeason) { this.creationSeason = creationSeason; }

    @Nullable public String getCreationEvent() { return creationEvent; }
    public void setCreationEvent(@Nullable String creationEvent) { this.creationEvent = creationEvent; }

    public int getRepairsCount() { return repairsCount; }
    public void setRepairsCount(int repairsCount) { this.repairsCount = repairsCount; }
    public void incrementRepairs() { this.repairsCount++; }

    // Stats
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public void addKills(int amount) { this.kills += amount; }

    public int getBlocksMined() { return blocksMined; }
    public void setBlocksMined(int blocksMined) { this.blocksMined = blocksMined; }
    public void addBlocksMined(int amount) { this.blocksMined += amount; }

    public int getFishCaught() { return fishCaught; }
    public void setFishCaught(int fishCaught) { this.fishCaught = fishCaught; }
    public void addFishCaught(int amount) { this.fishCaught += amount; }

    public int getDamageDealt() { return damageDealt; }
    public void setDamageDealt(int damageDealt) { this.damageDealt = damageDealt; }
    public void addDamageDealt(int amount) { this.damageDealt += amount; }

    public int getDamageAbsorbed() { return damageAbsorbed; }
    public void setDamageAbsorbed(int damageAbsorbed) { this.damageAbsorbed = damageAbsorbed; }
    public void addDamageAbsorbed(int amount) { this.damageAbsorbed += amount; }

    public int getDistanceTraveled() { return distanceTraveled; }
    public void setDistanceTraveled(int distanceTraveled) { this.distanceTraveled = distanceTraveled; }
    public void addDistanceTraveled(int amount) { this.distanceTraveled += amount; }

    @Nullable public String getCachedEpithet() { return cachedEpithet; }
    public void setCachedEpithet(@Nullable String cachedEpithet) { this.cachedEpithet = cachedEpithet; }

    @NotNull public List<QualityHistoryEntry> getQualityHistory() { return qualityHistory; }
    public void addQualityHistoryEntry(@NotNull QualityHistoryEntry entry) {
        this.qualityHistory.add(entry);
    }
    
    @NotNull public ItemDebugState getDebugState() { return debugState; }
    public void setDebugState(@NotNull ItemDebugState debugState) { this.debugState = debugState; }
    
    public boolean isDestroyed() { return isDestroyed; }
    public void setDestroyed(boolean destroyed) { this.isDestroyed = destroyed; }
}
