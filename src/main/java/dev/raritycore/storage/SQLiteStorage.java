package dev.raritycore.storage;

import dev.raritycore.RarityCorePlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Handles SQLite database operations for ItemStatistics.
 */
public final class SQLiteStorage {

    private final RarityCorePlugin plugin;
    private final String url;

    public SQLiteStorage(RarityCorePlugin plugin) {
        this.plugin = plugin;
        File dbFile = new File(plugin.getDataFolder(), "items.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        initSchema();
    }

    private void initSchema() {
        String createStatsTable = "CREATE TABLE IF NOT EXISTS item_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "creator_name VARCHAR(64), " +
                "original_owner VARCHAR(64), " +
                "creation_time BIGINT, " +
                "repairs INT, " +
                "kills INT, " +
                "blocks_mined INT, " +
                "fish_caught INT, " +
                "damage_dealt INT, " +
                "damage_absorbed INT, " +
                "distance_traveled INT, " +
                "cached_epithet TEXT, " +
                "current_master VARCHAR(64), " +
                "successor_progress INT, " +
                "debug_state_json TEXT, " +
                "is_destroyed BOOLEAN" +
                ");";

        String createOwnersTable = "CREATE TABLE IF NOT EXISTS item_owners (" +
                "uuid VARCHAR(36), " +
                "owner_name VARCHAR(64), " +
                "UNIQUE(uuid, owner_name)" +
                ");";

        String createQualityHistoryTable = "CREATE TABLE IF NOT EXISTS item_quality_history (" +
                "uuid VARCHAR(36), " +
                "quality_id VARCHAR(64), " +
                "timestamp BIGINT, " +
                "reason TEXT" +
                ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createStatsTable);
            stmt.execute(createOwnersTable);
            stmt.execute(createQualityHistoryTable);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite schema", e);
        }
    }

    public CompletableFuture<ItemStatistics> loadStats(UUID itemUuid) {
        return CompletableFuture.supplyAsync(() -> {
            ItemStatistics stats = new ItemStatistics(itemUuid);
            
            try (Connection conn = DriverManager.getConnection(url)) {
                // 1. Load basic stats
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM item_stats WHERE uuid = ?")) {
                    ps.setString(1, itemUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            stats.setCreatorName(rs.getString("creator_name"));
                            stats.setOriginalOwnerName(rs.getString("original_owner"));
                            stats.setCreationTimestamp(rs.getLong("creation_time"));
                            stats.setRepairsCount(rs.getInt("repairs"));
                            stats.setKills(rs.getInt("kills"));
                            stats.setBlocksMined(rs.getInt("blocks_mined"));
                            stats.setFishCaught(rs.getInt("fish_caught"));
                            stats.setDamageDealt(rs.getInt("damage_dealt"));
                            stats.setDamageAbsorbed(rs.getInt("damage_absorbed"));
                            stats.setDistanceTraveled(rs.getInt("distance_traveled"));
                            stats.setCachedEpithet(rs.getString("cached_epithet"));
                            
                            // Check column existence for backwards compatibility
                            try {
                                stats.setCurrentMaster(rs.getString("current_master"));
                                stats.setSuccessorProgress(rs.getInt("successor_progress"));
                            } catch (SQLException ignored) {}
                            try {
                                String debugJson = rs.getString("debug_state_json");
                                if (debugJson != null && !debugJson.isEmpty()) {
                                    ItemDebugState state = new com.google.gson.Gson().fromJson(debugJson, ItemDebugState.class);
                                    if (state != null) stats.setDebugState(state);
                                }
                            } catch (SQLException ignored) {}
                            try {
                                stats.setDestroyed(rs.getBoolean("is_destroyed"));
                            } catch (SQLException ignored) {}
                        }
                    }
                }

                // 2. Load owners history
                List<String> owners = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT owner_name FROM item_owners WHERE uuid = ?")) {
                    ps.setString(1, itemUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            owners.add(rs.getString("owner_name"));
                        }
                    }
                }
                stats.setOwnersHistory(owners);

                // 3. Load quality history
                try (PreparedStatement ps = conn.prepareStatement("SELECT quality_id, timestamp, reason FROM item_quality_history WHERE uuid = ? ORDER BY timestamp ASC")) {
                    ps.setString(1, itemUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            stats.addQualityHistoryEntry(new QualityHistoryEntry(
                                    rs.getString("quality_id"),
                                    rs.getLong("timestamp"),
                                    rs.getString("reason")
                            ));
                        }
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load item stats for " + itemUuid, e);
            }
            
            return stats;
        });
    }

    public CompletableFuture<Void> saveStats(ItemStatistics stats) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(url)) {
                conn.setAutoCommit(false);
                
                // Add columns if they don't exist
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE item_stats ADD COLUMN current_master VARCHAR(64);");
                } catch (SQLException ignored) {}
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE item_stats ADD COLUMN successor_progress INT;");
                } catch (SQLException ignored) {}
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE item_stats ADD COLUMN debug_state_json TEXT;");
                } catch (SQLException ignored) {}
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE item_stats ADD COLUMN is_destroyed BOOLEAN;");
                } catch (SQLException ignored) {}

                String upsertStats = "INSERT OR REPLACE INTO item_stats (uuid, creator_name, original_owner, creation_time, repairs, kills, blocks_mined, fish_caught, damage_dealt, damage_absorbed, distance_traveled, cached_epithet, current_master, successor_progress, debug_state_json, is_destroyed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(upsertStats)) {
                    ps.setString(1, stats.getItemUuid().toString());
                    ps.setString(2, stats.getCreatorName());
                    ps.setString(3, stats.getOriginalOwnerName());
                    ps.setLong(4, stats.getCreationTimestamp());
                    ps.setInt(5, stats.getRepairsCount());
                    ps.setInt(6, stats.getKills());
                    ps.setInt(7, stats.getBlocksMined());
                    ps.setInt(8, stats.getFishCaught());
                    ps.setInt(9, stats.getDamageDealt());
                    ps.setInt(10, stats.getDamageAbsorbed());
                    ps.setInt(11, stats.getDistanceTraveled());
                    ps.setString(12, stats.getCachedEpithet());
                    ps.setString(13, stats.getCurrentMaster() != null ? stats.getCurrentMaster() : stats.getOriginalOwnerName());
                    ps.setInt(14, stats.getSuccessorProgress());
                    ps.setString(15, new com.google.gson.Gson().toJson(stats.getDebugState()));
                    ps.setBoolean(16, stats.isDestroyed());
                    ps.executeUpdate();
                }

                // Owners history (insert ignore approach)
                String insertOwner = "INSERT OR IGNORE INTO item_owners (uuid, owner_name) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertOwner)) {
                    for (String owner : stats.getOwnersHistory()) {
                        ps.setString(1, stats.getItemUuid().toString());
                        ps.setString(2, owner);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Quality history (delete & insert, or just append new ones. Re-inserting is simplest if list isn't huge)
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM item_quality_history WHERE uuid = ?")) {
                    ps.setString(1, stats.getItemUuid().toString());
                    ps.executeUpdate();
                }
                
                String insertQH = "INSERT INTO item_quality_history (uuid, quality_id, timestamp, reason) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertQH)) {
                    for (QualityHistoryEntry qhe : stats.getQualityHistory()) {
                        ps.setString(1, stats.getItemUuid().toString());
                        ps.setString(2, qhe.getQualityId());
                        ps.setLong(3, qhe.getTimestamp());
                        ps.setString(4, qhe.getReason());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save item stats for " + stats.getItemUuid(), e);
            }
        });
    }

    public CompletableFuture<java.util.Map<String, Integer>> getTopLegacyOwners(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.Map<String, Integer> top = new java.util.LinkedHashMap<>();
            String query = "SELECT current_master as player_name, COUNT(DISTINCT s.uuid) as legacy_count " +
                           "FROM item_stats s " +
                           "JOIN item_quality_history q ON s.uuid = q.uuid " +
                           "WHERE q.quality_id = 'quality_legacy' AND s.is_destroyed = 0 " +
                           "GROUP BY current_master " +
                           "ORDER BY legacy_count DESC " +
                           "LIMIT ?";
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("player_name");
                        if (name == null || name.isEmpty()) name = "Unknown";
                        int count = rs.getInt("legacy_count");
                        top.put(name, count);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch top legacy owners", e);
            }
            return top;
        });
    }
}
