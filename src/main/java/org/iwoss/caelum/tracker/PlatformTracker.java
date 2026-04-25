package org.iwoss.caelum.tracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.iwoss.caelum.util.CaelumLogger;

import java.util.*;

public class PlatformTracker {
    private static final CaelumLogger LOGGER = new CaelumLogger(
            com.mojang.logging.LogUtils.getLogger());

    private final Map<GlobalPos, Long> pendingChecks = new HashMap<>();
    private static final int NEARBY_SEARCH_LIMIT = 15;

    public void add(GlobalPos pos, long expireTime) {
        pendingChecks.put(pos, expireTime);
    }

    public void update(GlobalPos pos, long newExpireTime) {
        pendingChecks.put(pos, newExpireTime);
    }

    public void remove(GlobalPos pos) {
        pendingChecks.remove(pos);
    }

    public boolean contains(GlobalPos pos) {
        return pendingChecks.containsKey(pos);
    }

    public int size() {
        return pendingChecks.size();
    }

    public void removeAll(Collection<BlockPos> positions, ServerLevel level) {
        for (BlockPos p : positions) {
            pendingChecks.remove(GlobalPos.of(level.dimension(), p));
        }
    }

    public List<GlobalPos> getExpired(long now) {
        List<GlobalPos> expired = new ArrayList<>();
        for (Map.Entry<GlobalPos, Long> entry : pendingChecks.entrySet()) {
            if (now > entry.getValue()) {
                expired.add(entry.getKey());
            }
        }
        return expired;
    }

    public GlobalPos findNearbySeed(ServerLevel level, BlockPos start) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        int checks = 0;
        while (!queue.isEmpty() && checks < NEARBY_SEARCH_LIMIT) {
            BlockPos current = queue.poll();
            checks++;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;

                GlobalPos gpos = GlobalPos.of(level.dimension(), neighbor.immutable());
                if (pendingChecks.containsKey(gpos)) return gpos;

                BlockState state = level.getBlockState(neighbor);
                if (!state.isAir()) {
                    visited.add(neighbor.immutable());
                    queue.add(neighbor.immutable());
                }
            }
        }
        return null;
    }

    public Map<GlobalPos, Long> getPendingEntries() {
        return new HashMap<>(pendingChecks);
    }
    public Set<GlobalPos> getPendingPositions() {
        return new HashSet<>(pendingChecks.keySet());
    }
}