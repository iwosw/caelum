package org.iwoss.caelum.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.iwoss.caelum.CaelumConfig;

public class HeightUtil {
    public static boolean isAboveThreshold(ServerLevel level, BlockPos pos) {
        int threshold = CaelumConfig.SERVER.seaLevel.get()
                + CaelumConfig.SERVER.minHeightAboveSeaLevel.get();
        return pos.getY() > threshold;
    }
}