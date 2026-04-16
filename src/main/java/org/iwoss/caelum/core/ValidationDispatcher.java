package org.iwoss.caelum.core;

import net.minecraft.world.level.block.Blocks;
import org.iwoss.caelum.CaelumConfig;
import org.iwoss.caelum.api.rules.IValidationRule;
import org.iwoss.caelum.impl.rules.*;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ValidationDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Queue<CaelumTask> taskQueue = new ConcurrentLinkedQueue<>();
    private final List<IValidationRule> rules = new ArrayList<>();

    public ValidationDispatcher() {
        registerRule(new AirPlatformRule());   // теперь только одно правило
    }

    public void registerRule(IValidationRule rule) {
        rules.add(rule);
        rules.sort(Comparator.comparingInt(IValidationRule::getPriority));
        LOGGER.debug("Registered rule: {} with priority {}", rule.getName(), rule.getPriority());
    }

    public void enqueue(CaelumTask task) {
        taskQueue.add(task);
    }

    /**
     * Вызывается каждый серверный тик (20 раз в секунду).
     * Обрабатывает не более maxTasksPerTick задач синхронно.
     */
    public void tick() {
        if (taskQueue.isEmpty()) return;

        int maxTasks = CaelumConfig.SERVER.maxTasksPerTick.get();
        int processed = 0;
        while (!taskQueue.isEmpty() && processed < maxTasks) {
            CaelumTask task = taskQueue.poll();
            if (task == null) continue;
            processed++;

            // Пропускаем, если мир или позиция не загружены
            if (!task.level().isLoaded(task.pos())) {
                LOGGER.debug("Skipping task at {} – chunk not loaded", task.pos());
                continue;
            }

            String violatedRule = null;
            for (IValidationRule rule : rules) {
                try {
                    if (rule.isInvalid(task)) {
                        violatedRule = rule.getName();
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.error("Rule {} threw an exception at {}", rule.getName(), task.pos(), e);
                }
            }

            if (violatedRule != null) {
                // Синхронно заменяем блок на воздух
                task.level().setBlock(task.pos(), Blocks.AIR.defaultBlockState(), 3);
                LOGGER.debug("Removed block at {} by rule {}", task.pos(), violatedRule);
            }
        }
        if (processed > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processed {} tasks this tick, {} remaining", processed, taskQueue.size());
        }
    }
}