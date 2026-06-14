package dev.raritycore.util;

import dev.raritycore.RarityCorePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.Queue;

public class BroadcastManager {

    private final RarityCorePlugin plugin;
    private final Queue<Component> broadcastQueue = new LinkedList<>();
    private long lastBroadcastTime = 0;
    // 10 second cooldown
    private final long COOLDOWN_MS = 10000;

    public BroadcastManager(RarityCorePlugin plugin) {
        this.plugin = plugin;
        startQueueTask();
    }

    public void queueBroadcast(Component message) {
        broadcastQueue.add(message);
    }

    private void startQueueTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!broadcastQueue.isEmpty()) {
                long now = System.currentTimeMillis();
                if (now - lastBroadcastTime >= COOLDOWN_MS) {
                    Component msg = broadcastQueue.poll();
                    if (msg != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcast(msg));
                        lastBroadcastTime = now;
                    }
                }
            }
        }, 20L, 20L); // Check every second
    }
}
