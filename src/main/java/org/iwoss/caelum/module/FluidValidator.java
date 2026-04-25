package org.iwoss.caelum.module;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.iwoss.caelum.util.CaelumLogger;
import org.iwoss.caelum.util.FluidUtil;
import org.iwoss.caelum.util.HeightUtil;
import com.mojang.logging.LogUtils;

public class FluidValidator {
    private static final CaelumLogger LOGGER = new CaelumLogger(LogUtils.getLogger());
    private static int blockedWater = 0;

    public static int getBlockedWater() { return blockedWater; }
    public static void resetBlockedWater() { blockedWater = 0; }

    @SubscribeEvent
    public void onBucketUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getItemStack().getItem() instanceof BucketItem)) return;

        BlockPos targetPos = event.getPos().relative(event.getFace());
        if (!HeightUtil.isAboveThreshold(level, targetPos)) {
            LOGGER.debug("[FLUID] Below threshold, allow");
            return;
        }

        if (FluidUtil.isInvalidFluidPlacement(level, targetPos)) {
            event.setCanceled(true);
            blockedWater++;
            LOGGER.warn("[FLUID] Blocked bucket at {}", targetPos);
            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            WaterCleaner.markForClean(level, targetPos);

            if (event.getEntity() instanceof ServerPlayer player) {
                player.containerMenu.broadcastFullState();
                player.inventoryMenu.broadcastChanges();
            }
        }
    }

    @SubscribeEvent
    public void onFluidPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (event.getPlacedBlock().getBlock() instanceof LiquidBlock) {
            if (FluidUtil.isInvalidFluidPlacement(level, pos)) {
                event.setCanceled(true);
                blockedWater++;
                LOGGER.warn("[FLUID] Blocked fluid block placement at {}", pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                WaterCleaner.markForClean(level, pos);
            }
        }
    }
}