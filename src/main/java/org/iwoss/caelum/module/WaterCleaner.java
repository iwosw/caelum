package org.iwoss.caelum.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.iwoss.caelum.util.CaelumLogger;
import org.iwoss.caelum.util.HeightUtil;
import com.mojang.logging.LogUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WaterCleaner {
    private static final CaelumLogger LOGGER = new CaelumLogger(LogUtils.getLogger());
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 10;
    private static final Map<GlobalPos, Long> PENDING = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 2000;

    public static void markForClean(ServerLevel level, BlockPos pos) {
        if (HeightUtil.isAboveThreshold(level, pos)) {
            PENDING.put(GlobalPos.of(level.dimension(), pos.immutable()), System.currentTimeMillis());
            LOGGER.debug("Marked {} for cleanup", pos);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(e -> now - e.getValue() > TIMEOUT);
        if (PENDING.isEmpty()) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            Iterator<Map.Entry<GlobalPos, Long>> it = PENDING.entrySet().iterator();
            while (it.hasNext()) {
                GlobalPos gPos = it.next().getKey();
                if (gPos.dimension() != level.dimension()) continue;

                BlockPos pos = gPos.pos();
                if (!level.isLoaded(pos)) continue;
                if (level.getBlockState(pos).getBlock() == Blocks.WATER ||
                        level.getBlockState(pos).getBlock() == Blocks.LAVA) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    LOGGER.warn("Removed illegal fluid at {}", pos);
                }
                it.remove();
            }
        }
    }
}