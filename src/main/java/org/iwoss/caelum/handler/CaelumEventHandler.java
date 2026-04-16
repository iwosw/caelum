package org.iwoss.caelum.handler;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.iwoss.caelum.impl.rules.FluidPlacementRule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.iwoss.caelum.core.CaelumTask;
import org.iwoss.caelum.core.ValidationDispatcher;
import org.iwoss.caelum.impl.rules.AirPlatformRule;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = "caelum")
public class CaelumEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final ValidationDispatcher dispatcher;

    public CaelumEventHandler(ValidationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        boolean placedByPlayer = event.getEntity() instanceof Player;
        dispatcher.enqueue(new CaelumTask(event.getPos(), level, placedByPlayer));
    }

    @SubscribeEvent
    public void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (FluidPlacementRule.isInvalidFluidPlacement(level, pos)) {
            event.setCanceled(true);
            FluidPlacementRule.warnPlayer(level, pos);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        for (int i = 1; i <= 10; i++) {
            BlockPos above = pos.above(i);
            if (level.getBlockState(above).isAir()) break;
            dispatcher.enqueue(new CaelumTask(above, level, false));
        }
        dispatcher.enqueue(new CaelumTask(pos, level, false));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerLevel overworld = event.getServer().overworld();
            AirPlatformRule.tick(overworld);
            dispatcher.tick();
        }
    }
}