package org.iwoss.caelum;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.iwoss.caelum.core.ValidationDispatcher;
import org.iwoss.caelum.handler.CaelumEventHandler;
import org.slf4j.Logger;

/**
 * Главный класс мода Caelum.
 * Здесь происходит инициализация диспетчера и регистрация обработчиков событий.
 */
@Mod(Caelum.MODID)
public class Caelum {
    public static final String MODID = "caelum";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Создаем единственный экземпляр диспетчера на весь мод
    private final ValidationDispatcher dispatcher;

    public Caelum() {
        LOGGER.info("Caelum: Инициализация системы небесной валидации...");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CaelumConfig.SERVER_SPEC);
        // 1. Инициализируем сердце мода
        this.dispatcher = new ValidationDispatcher();

        // 2. Регистрируем наш обработчик событий в главной шине Forge
        // Мы передаем диспетчер в конструктор хендлера
        MinecraftForge.EVENT_BUS.register(new CaelumEventHandler(this.dispatcher));

        LOGGER.info("Caelum: Система запущена и готова к очистке неба!");
    }
}