package org.iwoss.caelum.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import org.iwoss.caelum.CaelumConfig;
import org.iwoss.caelum.module.PlatformValidator;
import org.iwoss.caelum.util.CaelumHelper;

public class CleanupCommand implements ICommand {
    private static boolean cleaning = false;

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
            ctx.getSource().sendFailure(Component.literal("§cОчистка уже выполняется!"));
            return 0;
        }
        cleaning = true;
        ctx.getSource().sendSuccess(() -> Component.literal("§a[Caelum] Запущена полная очистка... возможны кратковременные лаги."), false);
        try {
            performCleanup(ctx.getSource());
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cОшибка при очистке: " + e.getMessage()));
            e.printStackTrace();
        } finally {
            cleaning = false;
        }
        return 1;
    }

    private void performCleanup(CommandSourceStack source) {
        int removedWater = 0;
        int removedPlatformsStart = PlatformValidator.getRemovedPlatforms();
        PlatformValidator validator = PlatformValidator.getInstance();
        if (validator == null) {
            source.sendFailure(Component.literal("§cPlatformValidator не инициализирован"));
            return;
        }

        for (ServerLevel level : source.getServer().getAllLevels()) {
            int threshold = CaelumConfig.SERVER.seaLevel.get() + CaelumConfig.SERVER.minHeightAboveSeaLevel.get();

            for (LevelChunk chunk : CaelumHelper.getLoadedChunks(level)) {
                if (!level.isLoaded(chunk.getPos().getWorldPosition())) continue;

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = threshold + 1; y < level.getMaxBuildHeight(); y++) {
                            BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                            if (!level.isLoaded(pos)) continue;

                            // check fluid
                            if (CaelumHelper.isInvalidFluidPlacement(level, pos)) {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                removedWater++;
                                continue;
                            }

                            //check platform
                            if (!level.getBlockState(pos).isAir() && CaelumHelper.isAboveThreshold(level, pos)) {
                                validator.forceCheckPlatform(level, pos);
                            }
                        }
                    }
                }
            }
        }

        int removedPlatforms = PlatformValidator.getRemovedPlatforms() - removedPlatformsStart;
        int finalWater = removedWater;
        int finalPlatforms = removedPlatforms;
        source.sendSuccess(() -> Component.literal(
                "§a[Caelum] Очистка завершена!\n" +
                        "§eУдалено воды: §f" + finalWater + "\n" +
                        "§eУдалено платформ: §f" + finalPlatforms
        ), false);
    }
}