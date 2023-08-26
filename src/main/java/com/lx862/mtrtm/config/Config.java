package com.lx862.mtrtm.config;

import com.google.gson.*;
import com.lx862.mtrtm.TransitManager;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class Config {
    private static final Path CONFIG_PATH = Paths.get(FabricLoader.getInstance().getConfigDir().toString(), "transitmanager");
    public static boolean forceKillMTRPathThread = false;
    public static int mtrJourneyPlannerTickTime = 0;
    public static int shearPSDTopPermLevel = 0;

    public static boolean load() {
        if(!Files.exists(CONFIG_PATH.resolve("config.json"))) {
            TransitManager.LOGGER.info("[TransitManager] Config not found, generating one...");
            writeConfig();
            return true;
        }

        TransitManager.LOGGER.info("[TransitManager] Reading Train Config...");
        try {
            final JsonObject jsonConfig = new JsonParser().parse(String.join("", Files.readAllLines(CONFIG_PATH.resolve("config.json")))).getAsJsonObject();
            try {
                forceKillMTRPathThread = jsonConfig.get("forceKillMTRPathThread").getAsBoolean();
            } catch (Exception ignored) {}

            try {
                mtrJourneyPlannerTickTime = jsonConfig.get("mtrJourneyPlannerTickTime").getAsInt();
            } catch (Exception ignored) {}

            try {
                shearPSDTopPermLevel = jsonConfig.get("shearPSDTopPermLevel").getAsInt();
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void writeConfig() {
        TransitManager.LOGGER.info("[TransitManager] Writing Config...");
        final JsonObject jsonConfig = new JsonObject();
        jsonConfig.addProperty("forceKillMTRPathThread", forceKillMTRPathThread);
        jsonConfig.addProperty("mtrJourneyPlannerTickTime", mtrJourneyPlannerTickTime);

        CONFIG_PATH.toFile().mkdirs();

        try {
            Files.write(CONFIG_PATH.resolve("config.json"), Collections.singleton(new GsonBuilder().setPrettyPrinting().create().toJson(jsonConfig)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
