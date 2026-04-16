package org.iwoss.caelum.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Record для хранения минимальных данных о блоке, который нужно проверить.
 * Используем record, так как данные не должны меняться после создания задачи.
 *
 * @param pos       Координаты установленного блока.
 * @param level     Уровень (мир), где произошло событие.
 * @param timestamp Время создания задачи (пригодится для отложенных проверок).
 */
public record CaelumTask(BlockPos pos, ServerLevel level, long timestamp, boolean placedByPlayer) {

    /**
     * Вспомогательный конструктор для быстрого создания задачи.
     * Важно: Всегда вызываем .immutable() для BlockPos.
     * BlockPlaceEvent часто переиспользует один и тот же объект BlockPos,
     * и если мы сохраним ссылку на него в асинхронном потоке, координаты могут
     * измениться до того, как мы начнем проверку.
     */
    public CaelumTask(BlockPos pos, ServerLevel level, boolean placedByPlayer) {
        this(pos.immutable(), level, System.currentTimeMillis(), placedByPlayer);
    }
}