package org.iwoss.caelum;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CaelumConfig {
    public static class Server {
        public final ForgeConfigSpec.IntValue seaLevel;
        public final ForgeConfigSpec.IntValue minHeightAboveSeaLevel;
        public final ForgeConfigSpec.IntValue waterSupportBlocksRequired;

        // RP Physics settings
        public final ForgeConfigSpec.IntValue marinationTimeMinutes;
        public final ForgeConfigSpec.DoubleValue minStabilityRatio;
        public final ForgeConfigSpec.ConfigValue<String> webhookUrl;
        public final ForgeConfigSpec.BooleanValue enableLogging;

        // Debug particles
        public final ForgeConfigSpec.BooleanValue debugParticles;
        public final ForgeConfigSpec.IntValue debugParticleIntervalTicks;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Caelum Advanced RP Physics & Anti-Grief").push("caelum");

            seaLevel = builder.defineInRange("seaLevel", 63, -64, 256);
            minHeightAboveSeaLevel = builder.defineInRange("minHeightAboveSeaLevel", 40, 0, 200);
            waterSupportBlocksRequired = builder.defineInRange("waterSupportBlocksRequired", 5, 1, 9);

            marinationTimeMinutes = builder
                    .comment("How many minutes to wait before checking if building is stable")
                    .defineInRange("marinationTimeMinutes", 60, 1, 1440);

            minStabilityRatio = builder
                    .comment("Ratio of support blocks to total blocks (0.05 = 5% support needed)")
                    .defineInRange("minStabilityRatio", 0.05, 0.0, 1.0);

            webhookUrl = builder
                    .comment("Discord/Telegram Webhook URL for alerts")
                    .define("webhookUrl", "");

            enableLogging = builder.define("enableLogging", true);

            debugParticles = builder
                    .comment("Show particles over pending/active platforms (for admins)")
                    .define("debugParticles", false);

            debugParticleIntervalTicks = builder
                    .comment("Tick interval for spawning debug particles")
                    .defineInRange("debugParticleIntervalTicks", 20, 5, 200);

            builder.pop();
        }
    }

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }
}