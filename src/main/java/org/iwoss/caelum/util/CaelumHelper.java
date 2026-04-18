package org.iwoss.caelum.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.iwoss.caelum.CaelumConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CaelumHelper {

    public static boolean isAboveThreshold(ServerLevel level, BlockPos pos) {
        int seaLevel = CaelumConfig.SERVER.seaLevel.get();
        int minAbove = CaelumConfig.SERVER.minHeightAboveSeaLevel.get();
        return pos.getY() > seaLevel + minAbove;
    }

    public static boolean hasDirectSupport(Level level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir();
    }


    public static int countSolidBlocksBelow(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        int solidBlocks = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getBlockState(below.offset(dx, 0, dz)).isSolid()) {
                    solidBlocks++;
                }
            }
        }
        return solidBlocks;
    }



    public static boolean isInvalidFluidPlacement(ServerLevel level, BlockPos pos) {
        if (!isAboveThreshold(level, pos)) return false;
        int required = CaelumConfig.SERVER.waterSupportBlocksRequired.get();
        int solidBlocks = countSolidBlocksBelow(level, pos);
        return solidBlocks < required;
    }

    public static List<LevelChunk> getLoadedChunks(ServerLevel level) {
        List<LevelChunk> chunks = new ArrayList<>();
        try {
            Method getChunksMethod = net.minecraft.server.level.ChunkMap.class.getDeclaredMethod("getChunks");
            getChunksMethod.setAccessible(true);
            Object chunkMap = level.getChunkSource().chunkMap;
            Iterable<?> holders = (Iterable<?>) getChunksMethod.invoke(chunkMap);
            for (Object holder : holders) {
                LevelChunk chunk = null;
                try {
                    Method getChunkMethod = holder.getClass().getMethod("getChunkIfPresent");
                    chunk = (LevelChunk) getChunkMethod.invoke(holder);
                } catch (NoSuchMethodException e) {
                    Method getFullChunk = holder.getClass().getMethod("getFullChunk");
                    chunk = (LevelChunk) getFullChunk.invoke(holder);
                }
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chunks;
    }


}