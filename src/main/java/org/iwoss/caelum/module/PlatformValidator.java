package org.iwoss.caelum.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.iwoss.caelum.CaelumConfig;
import org.iwoss.caelum.util.CaelumHelper;
import com.mojang.logging.LogUtils;
import org.iwoss.caelum.util.CaelumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PlatformValidator {
    private static final CaelumLogger LOGGER = new CaelumLogger(LogUtils.getLogger());
    private static final int MAX_PLATFORM_BLOCKS = 2000;
    private static final int PROCESS_INTERVAL_TICKS = 60;
    private static final int MAX_PROCESS_PER_TICK = 15;
    private static final long CACHE_TIMEOUT_MS = 10_000;

    private final Queue<BlockPos> pending = new ConcurrentLinkedQueue<>();
    private int tickCounter = 0;
    private final Set<BlockPos> processedRecently = new HashSet<>();
    private final Map<BlockPos, Long> cacheLastChecked = new ConcurrentHashMap<>();

    //Stats
    private static int removedPlatforms = 0;
    private static PlatformValidator instance;

    public PlatformValidator() {
        instance = this;
    }

    public static PlatformValidator getInstance() { return instance; }
    public static int getRemovedPlatforms() { return removedPlatforms; }
    public static void resetRemovedPlatforms() { removedPlatforms = 0; }
    public int getPendingSize() { return pending.size(); }
    public int getCacheSize() { return cacheLastChecked.size(); }

    public void forceCheckPlatform(ServerLevel level, BlockPos pos) {
        checkAndRemovePlatform(level, pos);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Player)) return;
        BlockPos pos = event.getPos();
        if (!CaelumHelper.isAboveThreshold(level, pos)) return;
        pending.add(pos.immutable());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (!CaelumHelper.isAboveThreshold(level, pos)) return;
        for (int i = 1; i <= 10; i++) {
            BlockPos above = pos.above(i);
            if (level.getBlockState(above).isAir()) break;
            if (CaelumHelper.isAboveThreshold(level, above)) {
                pending.add(above.immutable());
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter < PROCESS_INTERVAL_TICKS) return;
        tickCounter = 0;

        if (processedRecently.size() > 2000) processedRecently.clear();

        long now = System.currentTimeMillis();
        cacheLastChecked.entrySet().removeIf(e -> now - e.getValue() > CACHE_TIMEOUT_MS);

        ServerLevel level = event.getServer().overworld();
        if (level == null) return;

        int processed = 0;
        while (!pending.isEmpty() && processed < MAX_PROCESS_PER_TICK) {
            BlockPos pos = pending.poll();
            if (pos == null) continue;
            if (processedRecently.contains(pos)) continue;
            processedRecently.add(pos);

            Long lastChecked = cacheLastChecked.get(pos);
            if (lastChecked != null && (now - lastChecked) < CACHE_TIMEOUT_MS) continue;

            if (level.isLoaded(pos)) {
                checkAndRemovePlatform(level, pos);
                cacheLastChecked.put(pos, now);
            }
        }
    }

    private void checkAndRemovePlatform(ServerLevel level, BlockPos start) {
        Set<BlockPos> platform = findHorizontalPlatform(level, start);
        if (platform.isEmpty()) return;
        int size = platform.size();
        if (size > MAX_PLATFORM_BLOCKS) {
            LOGGER.warn("Platform at {} too large ({} blocks), skipping", start, size);
            return;
        }

        int minSizeToCheck = CaelumConfig.SERVER.minPlatformSizeToCheck.get();
        if (size < minSizeToCheck) {
            LOGGER.debug("Platform too small ({} < {}), ignoring", size, minSizeToCheck);
            return;
        }

        int supported = 0;
        for (BlockPos p : platform) {
            if (CaelumHelper.hasDirectSupport(level, p)) supported++;
        }

        int required = CaelumConfig.SERVER.minSupportedBlocksForLarge.get();
        if (supported < required) {
            removePlatform(level, platform);
            warnNearbyPlayers(level, start, size);
            LOGGER.info("Removed platform of {} blocks: only {}/{} have direct support (need {})", size, supported, size, required);
        } else {
            LOGGER.debug("Platform kept: {}/{} blocks have direct support", supported, size);
        }
    }

    private Set<BlockPos> findHorizontalPlatform(Level level, BlockPos start) {
        Set<BlockPos> platform = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        platform.add(start);
        int y = start.getY();

        int minX = start.getX() - 50;
        int maxX = start.getX() + 50;
        int minZ = start.getZ() - 50;
        int maxZ = start.getZ() + 50;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(dir);
                if (neighbor.getY() != y) continue;
                if (neighbor.getX() < minX || neighbor.getX() > maxX || neighbor.getZ() < minZ || neighbor.getZ() > maxZ) continue;
                if (platform.contains(neighbor)) continue;
                BlockState state = level.getBlockState(neighbor);
                if (state.isAir()) continue;
                if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA) continue;
                platform.add(neighbor);
                queue.add(neighbor);
            }
        }
        return platform;
    }

    private void removePlatform(ServerLevel level, Set<BlockPos> platform) {
        List<BlockPos> sorted = new ArrayList<>(platform);
        sorted.sort(Comparator.comparingInt(BlockPos::getY));
        for (BlockPos p : sorted) {
            if (level.getBlockState(p).isAir()) continue;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        removedPlatforms++;
        LOGGER.info("Removed floating platform of {} blocks", platform.size());
    }

    private void warnNearbyPlayers(ServerLevel level, BlockPos pos, int blockCount) {
        String message = String.format("§c[Caelum] §eВаша платформа из %d блоков удалена – недостаточно опоры!", blockCount);
        for (var player : level.players()) {
            if (player.blockPosition().distSqr(pos) < 2500) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            }
        }
    }
}