package org.iwoss.caelum.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.iwoss.caelum.CaelumConfig;
import org.iwoss.caelum.physics.BlockPhysics;
import org.iwoss.caelum.physics.PlatformScanner;
import org.iwoss.caelum.tracker.PlatformTracker;
import org.iwoss.caelum.util.*;

import java.nio.file.Path;
import java.util.*;

public class PlatformValidator {
    private static final CaelumLogger LOGGER = new CaelumLogger(
            com.mojang.logging.LogUtils.getLogger());

    private final PlatformTracker tracker = new PlatformTracker();
    private final Queue<PlatformScanner.ScanTask> scanQueue = new ArrayDeque<>();
    private final List<PlatformScanner.ScanTask> activeScans = new ArrayList<>();

    // IN PlatformValidator
    public void savePendingPlatforms(Path worldDir) {
        PlatformPersistence.save(tracker.getPendingEntries(), worldDir);
    }

    public void loadPendingPlatforms(Path worldDir) {
        Map<GlobalPos, Long> loaded = PlatformPersistence.load(worldDir);
        for (Map.Entry<GlobalPos, Long> entry : loaded.entrySet()) {
            tracker.add(entry.getKey(), entry.getValue());
        }
    }


    private static int removedPlatforms = 0;
    private int tickCounter = 0;
    private int debugParticleTickCounter = 0;
    private static PlatformValidator instance;

    private static final int CHECK_INTERVAL_TICKS = 200;
    private static final int MAX_NEW_SCANS_PER_TICK = 2;
    private static final int BLOCKS_PER_TICK = 300;

    public PlatformValidator() {
        instance = this;
    }

    public static PlatformValidator getInstance() {
        return instance;
    }

    public static int getRemovedPlatforms() {
        return removedPlatforms;
    }

    public static void resetRemovedPlatforms() {
        removedPlatforms = 0;
    }

    public int getPendingSize() {
        return tracker.size();
    }

    public int getCacheSize() {
        return tracker.size();
    }

    public int getQueueSize() {
        return scanQueue.size();
    }

    public int getActiveScanSize() {
        return activeScans.size();
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();

        if (!HeightUtil.isAboveThreshold(level, pos)) return;

        if (level.getBlockState(pos.below()).isAir()) {
            GlobalPos existingSeed = tracker.findNearbySeed(level, pos);
            long newTime = System.currentTimeMillis()
                    + (CaelumConfig.SERVER.marinationTimeMinutes.get() * 60000L);

            if (existingSeed != null) {
                tracker.update(existingSeed, newTime);
                LOGGER.debug("Updated platform seed at {}. Timer reset.", existingSeed.pos());
            } else {
                GlobalPos gpos = GlobalPos.of(level.dimension(), pos.immutable());
                tracker.add(gpos, newTime);

                if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                    PlayerNotifier.notifyPlatformDetected(player);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter >= CHECK_INTERVAL_TICKS) {
            tickCounter = 0;
            long now = System.currentTimeMillis();

            List<GlobalPos> expired = new ArrayList<>(tracker.getExpired(now));
            for (GlobalPos gpos : expired) {
                tracker.remove(gpos);
                ServerLevel level = event.getServer().getLevel(gpos.dimension());
                if (level != null && level.isLoaded(gpos.pos())) {
                    scanQueue.add(new PlatformScanner.ScanTask(level, gpos.pos()));
                }
            }
        }

        int started = 0;
        while (!scanQueue.isEmpty() && started < MAX_NEW_SCANS_PER_TICK) {
            activeScans.add(scanQueue.poll());
            started++;
        }

        Iterator<PlatformScanner.ScanTask> it = activeScans.iterator();
        while (it.hasNext()) {
            PlatformScanner.ScanTask task = it.next();
            boolean finished = task.processSteps(BLOCKS_PER_TICK);
            if (finished) {
                finishScan(task);
                it.remove();
            }
        }

        PlayerNotifier.cleanupOldEntries();

        if (CaelumConfig.SERVER.debugParticles.get()) {
            debugParticleTickCounter++;
            if (debugParticleTickCounter >= CaelumConfig.SERVER.debugParticleIntervalTicks.get()) {
                debugParticleTickCounter = 0;
                spawnDebugParticles(event.getServer());
            }
        }
    }

    public void forceCheckPlatform(ServerLevel level, BlockPos pos) {
        scanQueue.add(new PlatformScanner.ScanTask(level, pos));
    }

    private void finishScan(PlatformScanner.ScanTask task) {
        ServerLevel level = task.getLevel();
        var allBlocks = task.getAllBlocks();

        tracker.removeAll(allBlocks, level);

        if (allBlocks.isEmpty()) return;

        double ratio = (double) task.getSupportCount() / allBlocks.size();
        boolean isIllegal = !task.isTouchesGround()
                || ratio < CaelumConfig.SERVER.minStabilityRatio.get();

        if (isIllegal) {
            BlockPhysics.destroyWithPhysics(level, allBlocks);
            removedPlatforms++;
            String msg = String.format("Skybase collapsed! Size: %d blocks at %s",
                    allBlocks.size(), task.getStart().toShortString());
            LOGGER.warn(msg);
            WebhookUtil.sendUpdate(msg);
        }
    }

    private void spawnDebugParticles(MinecraftServer server) {
        for (GlobalPos gpos : tracker.getPendingPositions()) {
            ServerLevel level = server.getLevel(gpos.dimension());
            if (level == null || !level.isLoaded(gpos.pos())) continue;
            BlockPos pos = gpos.pos();
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    1, 0.0, 0.0, 0.0, 0.0
            );
        }

        for (PlatformScanner.ScanTask task : activeScans) {
            ServerLevel level = task.getLevel();
            if (level == null) continue;
            Set<BlockPos> blocks = task.getAllBlocks();
            int maxParticles = Math.min(blocks.size(), 30);
            int count = 0;
            for (BlockPos pos : blocks) {
                if (count >= maxParticles) break;
                if (blocks.size() > maxParticles && Math.random() > (double) maxParticles / blocks.size())
                    continue;
                count++;
                level.sendParticles(
                        ParticleTypes.PORTAL,
                        pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        1, 0.0, 0.0, 0.0, 0.02
                );
            }
        }
    }
}