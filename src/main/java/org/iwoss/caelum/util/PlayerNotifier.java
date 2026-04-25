package org.iwoss.caelum.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.iwoss.caelum.CaelumConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNotifier {
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN_MS = 60000;
    private static final long MAX_AGE_MS = 600000;

    public static void notifyPlatformDetected(Player player) {
        long now = System.currentTimeMillis();
        long lastMsg = COOLDOWNS.getOrDefault(player.getUUID(), 0L);
        if (now - lastMsg > COOLDOWN_MS) {
            player.sendSystemMessage(Component.translatable(
                    "caelum.physics.platform_detected",
                    CaelumConfig.SERVER.marinationTimeMinutes.get()));
            COOLDOWNS.put(player.getUUID(), now);
        }
    }

    public static void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        COOLDOWNS.entrySet().removeIf(e -> now - e.getValue() > MAX_AGE_MS);
    }
}