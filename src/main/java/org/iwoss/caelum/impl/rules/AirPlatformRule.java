package org.iwoss.caelum.impl.rules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import org.iwoss.caelum.CaelumConfig;
import org.iwoss.caelum.api.rules.IValidationRule;
import org.iwoss.caelum.core.CaelumTask;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AirPlatformRule implements IValidationRule {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Храним подозреваемые блоки с временем добавления
    private static final Map<BlockPos, Long> SUSPECTS = new ConcurrentHashMap<>();
    private static final long SUSPECT_TIMEOUT_MS = 300_000; // 5 минут

    private static final int CHECK_INTERVAL_TICKS = 100; // 5 секунд
    private static final int MAX_SUPPORT_DEPTH = 50;     // глубина рекурсивного поиска опоры
    private static int tickCounter = 0;

    @Override
    public boolean isInvalid(CaelumTask task) {
        if (!task.placedByPlayer()) return false;

        BlockPos pos = task.pos();
        BlockState state = task.level().getBlockState(pos);

        // Натуральная листва (сгенерированная миром) не трогаем
        if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
            return false;
        }

        // Только блоки, имеющие горизонтальных соседей (часть платформы)
        if (!hasHorizontalNeighbors(task, pos)) return false;

        SUSPECTS.put(pos.immutable(), System.currentTimeMillis());
        LOGGER.debug("Added suspect platform block at {}", pos);
        return false;
    }

    public static void tick(ServerLevel level) {
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        cleanupOldSuspects();
        if (SUSPECTS.isEmpty()) return;

        int maxBlocks = CaelumConfig.SERVER.maxPlatformBlocks.get();
        double minRatio = CaelumConfig.SERVER.minSupportRatio.get();
        int minSupportCount = CaelumConfig.SERVER.minSupportCount.get();

        Iterator<Map.Entry<BlockPos, Long>> iterator = SUSPECTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            BlockPos suspectPos = entry.getKey();
            if (!level.isLoaded(suspectPos)) continue;

            // Ищем только горизонтальные блоки на том же Y
            Set<BlockPos> platform = findHorizontalPlatform(level, suspectPos, 20);
            if (platform.isEmpty()) {
                iterator.remove();
                continue;
            }

            int total = platform.size();
            if (total > maxBlocks) {
                LOGGER.warn("Platform too large ({} blocks) at {}, skipping", total, suspectPos);
                iterator.remove();
                continue;
            }

            // Проверяем, сколько блоков платформы имеют опору (вертикальный столб до земли)
            int supported = 0;
            for (BlockPos p : platform) {
                if (hasTrueSupport(level, p, MAX_SUPPORT_DEPTH, new HashSet<>())) {
                    supported++;
                }
            }

            double ratio = (double) supported / total;
            boolean shouldRemove = (ratio < minRatio) || (supported < minSupportCount);

            if (shouldRemove) {
                removePlatform(level, platform, suspectPos);
                iterator.remove();
            } else {
                LOGGER.debug("Platform at {} has {}/{} support ({}%), keeping", suspectPos, supported, total, (int)(ratio*100));
                iterator.remove();
            }
        }
    }

    // Поиск только горизонтальных соседей на одном Y
    private static Set<BlockPos> findHorizontalPlatform(Level level, BlockPos start, int maxRadius) {
        Set<BlockPos> platform = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        platform.add(start);
        int startY = start.getY();

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (Math.abs(current.getX() - start.getX()) > maxRadius ||
                    Math.abs(current.getZ() - start.getZ()) > maxRadius) continue;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(dir);
                if (platform.contains(neighbor)) continue;
                if (level.getBlockState(neighbor).isAir()) continue;
                if (neighbor.getY() != startY) continue; // строго тот же Y
                platform.add(neighbor);
                queue.add(neighbor);
            }
        }
        return platform;
    }

    private static boolean hasTrueSupport(Level level, BlockPos pos, int maxDepth, Set<BlockPos> visited) {
        if (!visited.add(pos)) return false;
        if (visited.size() > 1000) return false;
        if (maxDepth <= 0) return false;

        // Идём вниз, пока не встретим твёрдую землю или не кончится глубина
        for (int i = 1; i <= maxDepth; i++) {
            BlockPos below = pos.below(i);
            if (level.isOutsideBuildHeight(below)) return false;
            BlockState state = level.getBlockState(below);
            if (state.isAir()) continue;

            if (isSolidGround(state)) {
                return true; // нашли настоящую землю
            }

            // Если блок не земля, проверяем его опору рекурсивно
            if (hasTrueSupport(level, below, maxDepth - i, visited)) {
                return true;
            }
        }
        return false;
    }

    // Определение "настоящей земли" (не воздух, не жидкость, не падающий блок)
    private static boolean isSolidGround(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) return false;
        if (state.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) return false;
        return state.isSolid();
    }

    // Удаление платформы снизу вверх (для корректной обработки падающих блоков)
    private static void removePlatform(ServerLevel level, Set<BlockPos> platform, BlockPos center) {
        List<BlockPos> sorted = platform.stream()
                .sorted(Comparator.comparingInt(BlockPos::getY))
                .toList();
        for (BlockPos p : sorted) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        LOGGER.info("Removed floating platform of {} blocks at {}", platform.size(), center);
        warnNearbyPlayers(level, center, platform.size());
    }

    private static void warnNearbyPlayers(ServerLevel level, BlockPos pos, int blockCount) {
        Component message = Component.literal(
                String.format("§c[Caelum] §eВаша платформа из %d блоков удалена – недостаточно опоры!", blockCount)
        );
        for (var player : level.players()) {
            if (player.blockPosition().distSqr(pos) < 2500) {
                player.sendSystemMessage(message);
            }
        }
    }

    // Очистка старых подозреваемых
    private static void cleanupOldSuspects() {
        long now = System.currentTimeMillis();
        SUSPECTS.entrySet().removeIf(entry -> now - entry.getValue() > SUSPECT_TIMEOUT_MS);
    }

    private boolean hasHorizontalNeighbors(CaelumTask task, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!task.level().getBlockState(pos.relative(dir)).isAir()) return true;
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 35;
    }
}