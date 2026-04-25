package org.iwoss.caelum;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.iwoss.caelum.command.CommandManager;
import org.iwoss.caelum.module.FluidValidator;
import org.iwoss.caelum.module.PlatformValidator;
import org.iwoss.caelum.module.WaterCleaner;
import org.slf4j.Logger;

import java.nio.file.Path;

@Mod(Caelum.MODID)
public class Caelum {
    public static final String MODID = "caelum";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Caelum() {
        LOGGER.info("Caelum initializing (optimized server version)...");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CaelumConfig.SERVER_SPEC);
        MinecraftForge.EVENT_BUS.register(new PlatformValidator());
        MinecraftForge.EVENT_BUS.register(new FluidValidator());
        MinecraftForge.EVENT_BUS.register(new WaterCleaner());
        LOGGER.info("Caelum ready");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            CommandManager.register(event.getServer().getCommands().getDispatcher());

            PlatformValidator pv = PlatformValidator.getInstance();
            if (pv != null) {
                Path worldDir = event.getServer().getServerDirectory().toPath().resolve("world");
                pv.loadPendingPlatforms(worldDir);
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            PlatformValidator pv = PlatformValidator.getInstance();
            if (pv != null) {
                Path worldDir = event.getServer().getServerDirectory().toPath().resolve("world");
                pv.savePendingPlatforms(worldDir);
            }
        }
    }
}