package org.iwoss.caelum.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.iwoss.caelum.CaelumConfig;
import org.iwoss.caelum.module.PlatformValidator;
import org.iwoss.caelum.util.ChunkUtil;
import org.iwoss.caelum.util.FluidUtil;
import org.iwoss.caelum.util.HeightUtil;

import java.util.*;

public class CleanupCommand implements ICommand {
    private static boolean cleaning = false;
    private static final Queue<ChunkTask> taskQueue = new LinkedList<>();
    private static CommandSourceStack currentSource = null;
    private static final Set<ChunkPos> processedChunks = new HashSet<>();
    private static final Set<GlobalPos> checkedPositions = new HashSet<>();

    public CleanupCommand() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("caelum")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("cleanup")
                        .executes(this::cleanup))
        );
    }

    private int cleanup(CommandContext<CommandSourceStack> ctx) {
        if (cleaning) {
            ctx.getSource().sendFailure(Component.translatable("caelum.command.cleanup.already_running"));
            return 0;
        }
        cleaning = true;
        currentSource = ctx.getSource();
        processedChunks.clear();
        checkedPositions.clear();
        ctx.getSource().sendSuccess(() -> Component.translatable("caelum.command.cleanup.started"), false);

        try {
            for (ServerLevel level : currentSource.getServer().getAllLevels()) {
                for (LevelChunk chunk : ChunkUtil.getLoadedChunks(level)) {
                    if (processedChunks.add(chunk.getPos())) {
                        taskQueue.add(new ChunkTask(level, chunk));
                    }
                }
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("caelum.command.cleanup.error", e.getMessage()));
            cleaning = false;
            processedChunks.clear();
            checkedPositions.clear();
        }
        return 1;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !cleaning) return;

        int chunksThisTick = 2;
        for (int i = 0; i < chunksThisTick; i++) {
            if (taskQueue.isEmpty()) {
                if (cleaning) {
                    cleaning = false;
                    if (currentSource != null) {
                        currentSource.sendSuccess(() -> Component.translatable("caelum.command.cleanup.finished"), false);
                    }
                    processedChunks.clear();
                    checkedPositions.clear();
                }
                return;
            }
            processChunk(taskQueue.poll());
        }
    }

    private void processChunk(ChunkTask task) {
        PlatformValidator validator = PlatformValidator.getInstance();
        if (validator == null) return;

        ServerLevel level = task.level;
        LevelChunk chunk = task.chunk;
        int threshold = CaelumConfig.SERVER.seaLevel.get()
                + CaelumConfig.SERVER.minHeightAboveSeaLevel.get();

        if (!level.isLoaded(chunk.getPos().getWorldPosition())) return;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = threshold + 1; y < level.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(
                            chunk.getPos().getMinBlockX() + x, y,
                            chunk.getPos().getMinBlockZ() + z);
                    if (!level.isLoaded(pos)) continue;

                    if (FluidUtil.isInvalidFluidPlacement(level, pos)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }

                    if (!level.getBlockState(pos).isAir() && HeightUtil.isAboveThreshold(level, pos)) {
                        GlobalPos gPos = GlobalPos.of(level.dimension(), pos);
                        if (checkedPositions.add(gPos)) {
                            validator.forceCheckPlatform(level, pos);
                        }
                    }
                }
            }
        }
    }

    private static class ChunkTask {
        final ServerLevel level;
        final LevelChunk chunk;
        ChunkTask(ServerLevel l, LevelChunk c) {
            this.level = l;
            this.chunk = c;
        }
    }
}