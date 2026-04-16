package org.iwoss.caelum.api.rules;

import org.iwoss.caelum.core.CaelumTask;

/**
 * Базовый интерфейс для всех проверок валидации в Caelum.
 * Каждое правило (Connectivity, Stability, Fluid и т.д.) должно реализовать этот интерфейс.
 */
public interface IValidationRule {

    /**
     * Основной метод проверки.
     * * @param task Объект задачи, содержащий BlockPos и ServerLevel.
     * @return true, если блок НАРУШАЕТ правило (должен быть удален).
     * false, если блок легален (проходит проверку).
     */
    boolean isInvalid(CaelumTask task);

    /**
     * Имя правила для логов или отладки.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Приоритет выполнения. Чем ниже число, тем раньше правило сработает.
     * Сначала запускаем легкие правила (Heightmap), затем тяжелые (Pathfinding).
     */
    default int getPriority() {
        return 100;
    }
}