package org.iwoss.caelum.util;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.iwoss.caelum.CaelumConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PlatformPersistence {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save(Map<GlobalPos, Long> pendingChecks, Path worldDir) {
        Path file = worldDir.resolve("caelum_platforms.json");
        JsonArray array = new JsonArray();
        long now = System.currentTimeMillis();

        for (Map.Entry<GlobalPos, Long> entry : pendingChecks.entrySet()) {
            GlobalPos gpos = entry.getKey();
            long expireTime = entry.getValue();
            if (expireTime <= now) continue; // не сохраняем просроченные

            JsonObject obj = new JsonObject();
            obj.addProperty("dimension", gpos.dimension().location().toString());
            obj.addProperty("x", gpos.pos().getX());
            obj.addProperty("y", gpos.pos().getY());
            obj.addProperty("z", gpos.pos().getZ());
            obj.addProperty("expireTime", expireTime);
            array.add(obj);
        }

        try {
            Files.createDirectories(worldDir);
            try (Writer writer = new OutputStreamWriter(
                    Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
            if (CaelumConfig.SERVER.enableLogging.get()) {
                LOGGER.info("Saved {} pending platforms", array.size());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save platforms", e);
        }
    }

    public static Map<GlobalPos, Long> load(Path worldDir) {
        Map<GlobalPos, Long> loaded = new HashMap<>();
        Path file = worldDir.resolve("caelum_platforms.json");
        if (!Files.exists(file)) return loaded;

        try (Reader reader = new InputStreamReader(
                Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            long now = System.currentTimeMillis();

            for (JsonElement elem : array) {
                JsonObject obj = elem.getAsJsonObject();
                String dimStr = obj.get("dimension").getAsString();
                ResourceLocation dimKey = ResourceLocation.tryParse(dimStr);
                if (dimKey == null) continue;

                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int z = obj.get("z").getAsInt();
                long expireTime = obj.get("expireTime").getAsLong();
                if (expireTime <= now) continue;

                ResourceKey<Level> dimension = ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, dimKey);
                GlobalPos gpos = GlobalPos.of(dimension, new BlockPos(x, y, z));
                loaded.put(gpos, expireTime);
            }
            LOGGER.info("Loaded {} pending platforms", loaded.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load platforms", e);
        }
        return loaded;
    }
}