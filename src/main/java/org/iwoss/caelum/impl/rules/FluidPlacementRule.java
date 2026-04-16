package org.iwoss.caelum.impl.rules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.network.chat.Component;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class FluidPlacementRule {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MIN_HEIGHT_ABOVE_GROUND = 10;

    public static boolean isInvalidFluidPlacement(ServerLevel level, BlockPos pos) {
        LOGGER.info("=== Fluid placement check at {} ===", pos);

        // 1. Высота над поверхностью
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        int heightAbove = pos.getY() - surfaceY;
        LOGGER.info("Surface Y = {}, block Y = {}, height above = {}", surfaceY, pos.getY(), heightAbove);

        if (pos.getY() <= surfaceY + MIN_HEIGHT_ABOVE_GROUND) {
            LOGGER.info("Height below threshold ({} <= {} + {}), allowing placement",
                    pos.getY(), surfaceY, MIN_HEIGHT_ABOVE_GROUND);
            return false;
        }
        LOGGER.info("Height above threshold, continuing check");

        // 2. Проверка блока снизу
        BlockPos below = pos.below();
        boolean hasSolidBelow = !level.getBlockState(below).isAir() && level.getBlockState(below).isSolid();
        LOGGER.info("Block below {}: {} (solid: {})", below, level.getBlockState(below).getBlock(), hasSolidBelow);

        boolean hasSolidSide = false;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            boolean solid = !level.getBlockState(side).isAir() && level.getBlockState(side).isSolid();
            LOGGER.info("Side {}: {} (solid: {})", side, level.getBlockState(side).getBlock(), solid);
            if (solid) {
                hasSolidSide = true;
            }
        }

        // 4. Решение
        boolean cancel = !hasSolidBelow && !hasSolidSide;
        LOGGER.info("hasSolidBelow={}, hasSolidSide={}, cancel={}", hasSolidBelow, hasSolidSide, cancel);
        return cancel;
    }

    public static void warnPlayer(ServerLevel level, BlockPos pos) {
        var players = level.players();
        for (var player : players) {
            if (player.blockPosition().distSqr(pos) < 2500) {
                player.sendSystemMessage(Component.literal("§c[Caelum] §eНельзя разливать воду/лаву в воздухе!"));
                LOGGER.info("Sent warning to player {} at {}", player.getName().getString(), player.blockPosition());
                break;
            }
        }
    }
}