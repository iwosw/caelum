package org.iwoss.caelum.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.iwoss.caelum.CaelumConfig;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class PlatformScanner {

    public static class ScanResult {
        public Set<BlockPos> allBlocks = new HashSet<>();
        public int supportCount = 0;
        public boolean touchesGround = false;
    }

    public static ScanResult scanPlatformStability(ServerLevel level, BlockPos start) {
        ScanResult result = new ScanResult();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        result.allBlocks.add(start);

        int maxSearch = 5000;
        int seaLevel = CaelumConfig.SERVER.seaLevel.get();

        while (!queue.isEmpty() && result.allBlocks.size() < maxSearch) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (result.allBlocks.contains(neighbor)) continue;

                BlockState state = level.getBlockState(neighbor);
                if (state.isAir()) continue;

                result.allBlocks.add(neighbor.immutable());
                queue.add(neighbor.immutable());

                if (neighbor.getY() <= seaLevel + 1) {
                    result.touchesGround = true;
                    result.supportCount++;
                }
            }
        }
        return result;
    }

    public static class ScanTask {
        private final ServerLevel level;
        private final BlockPos start;
        private final Queue<BlockPos> queue = new ArrayDeque<>();
        private final Set<BlockPos> allBlocks = new HashSet<>();
        private int supportCount = 0;
        private boolean touchesGround = false;
        private static final int MAX_SCAN_BLOCKS = 5000;

        public ScanTask(ServerLevel level, BlockPos start) {
            this.level = level;
            this.start = start.immutable();
            this.queue.add(this.start);
            this.allBlocks.add(this.start);
        }

        public ServerLevel getLevel() { return level; }
        public BlockPos getStart() { return start; }
        public Set<BlockPos> getAllBlocks() { return allBlocks; }
        public int getSupportCount() { return supportCount; }
        public boolean isTouchesGround() { return touchesGround; }

        public boolean processSteps(int steps) {
            int processed = 0;
            int seaLevel = CaelumConfig.SERVER.seaLevel.get();

            while (!queue.isEmpty() && allBlocks.size() < MAX_SCAN_BLOCKS && processed < steps) {
                BlockPos current = queue.poll();
                processed++;

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (allBlocks.contains(neighbor)) continue;

                    BlockState state = level.getBlockState(neighbor);
                    if (state.isAir()) continue;

                    allBlocks.add(neighbor.immutable());
                    queue.add(neighbor.immutable());

                    if (neighbor.getY() <= seaLevel + 1) {
                        touchesGround = true;
                        supportCount++;
                    }
                }
            }
            return queue.isEmpty() || allBlocks.size() >= MAX_SCAN_BLOCKS;
        }
    }
}