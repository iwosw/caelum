package org.iwoss.caelum;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CaelumConfig {
    public static class Server {
        public final ForgeConfigSpec.IntValue maxTasksPerTick;
        public final ForgeConfigSpec.IntValue maxPlatformBlocks;
        public final ForgeConfigSpec.DoubleValue minSupportRatio;
        public final ForgeConfigSpec.IntValue minSupportCount; // минимальное количество опорных блоков

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Caelum Anti-Floating Platform Rules").push("caelum");

            maxTasksPerTick = builder
                    .comment("Maximum number of block validations per server tick (20 ticks/sec)")
                    .defineInRange("maxTasksPerTick", 50, 1, 500);

            maxPlatformBlocks = builder
                    .comment("Maximum number of blocks in a platform to check (larger platforms are ignored to prevent lag)")
                    .defineInRange("maxPlatformBlocks", 500, 100, 5000);

            minSupportRatio = builder
                    .comment("Minimum ratio of supported blocks to total platform blocks (0.1 = 10%%)")
                    .defineInRange("minSupportRatio", 0.1, 0.0, 1.0);

            minSupportCount = builder
                    .comment("Minimum absolute number of supported blocks required (e.g., 2 means at least 2 blocks must have support)")
                    .defineInRange("minSupportCount", 1, 0, 100);

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