package org.iwoss.caelum.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.iwoss.caelum.CaelumConfig;

public class FluidUtil {
    public static boolean isInvalidFluidPlacement(ServerLevel level, BlockPos pos) {
        if (!HeightUtil.isAboveThreshold(level, pos)) return false;

        int solid = 0;
        BlockPos below = pos.below();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!level.getBlockState(below.offset(x, 0, z)).isAir()) solid++;
            }
        }
        return solid < CaelumConfig.SERVER.waterSupportBlocksRequired.get();
    }
}