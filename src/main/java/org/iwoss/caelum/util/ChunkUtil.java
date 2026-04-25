package org.iwoss.caelum.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChunkUtil {
    public static List<LevelChunk> getLoadedChunks(ServerLevel level) {
        Set<LevelChunk> unique = new HashSet<>();
        for (var player : level.players()) {
            unique.add(level.getChunkAt(player.blockPosition()));
        }
        return new ArrayList<>(unique);
    }
}