package org.iwoss.caelum;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CaelumConfig {
    public static class Server {
        public final ForgeConfigSpec.IntValue seaLevel;
        public final ForgeConfigSpec.IntValue minHeightAboveSeaLevel;
        public final ForgeConfigSpec.IntValue waterSupportBlocksRequired;
        public final ForgeConfigSpec.IntValue minPlatformSizeToCheck;
        public final ForgeConfigSpec.IntValue minSupportedBlocksForLarge;
        public final ForgeConfigSpec.BooleanValue enableLogging;  // управление логами

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Caelum Anti-Floating Platform & Water Rules").push("caelum");
            seaLevel = builder
                    .comment("Sea level (ocean height) for this dimension. Default 63 for Overworld.")
                    .defineInRange("seaLevel", 63, -64, 256);
            minHeightAboveSeaLevel = builder
                    .comment("Minimum height ABOVE sea level to start checking. Example: 40 means Y > seaLevel+40 will be checked.")
                    .defineInRange("minHeightAboveSeaLevel", 40, 0, 200);
            waterSupportBlocksRequired = builder
                    .comment("Minimum solid blocks in 3x3 area under water to allow placement")
                    .defineInRange("waterSupportBlocksRequired", 5, 1, 9);
            minPlatformSizeToCheck = builder
                    .comment("Platforms with size < this value are never removed (allow small floating platforms).")
                    .defineInRange("minPlatformSizeToCheck", 4, 1, 100);
            minSupportedBlocksForLarge = builder
                    .comment("For platforms with size >= minPlatformSizeToCheck, require at least this many blocks to have direct vertical support (a pillar below).")
                    .defineInRange("minSupportedBlocksForLarge", 5, 1, 100);
            enableLogging = builder
                    .comment("Enable or disable all Caelum logs (true by default). Set to false to reduce console spam.")
                    .define("enableLogging", true);
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