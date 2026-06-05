package com.lx862.mtrtm;

import com.lx862.mtrtm.config.TMConfig;
import com.lx862.mtrtm.util.MtrUtil;
import it.unimi.dsi.fastutil.longs.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransitManager implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("TransitManager");

    @Override
    public void onInitialize() {
        LOGGER.info("[TransitManager] TransitManager initialized \\(＾▽＾)/");
        TMConfig.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, third) -> {
            Commands.registerCommands(dispatcher);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((detail, server) -> {
            MtrUtil.removeVehicleRiders(detail.getPlayer());
        });
    }
}
