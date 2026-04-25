package org.iwoss.caelum.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class BlockPhysics {
    public static void destroyWithPhysics(ServerLevel level, Set<BlockPos> blocks) {
        List<BlockPos> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparingInt(BlockPos::getY));
        for (BlockPos p : sorted) {
            if (!level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}