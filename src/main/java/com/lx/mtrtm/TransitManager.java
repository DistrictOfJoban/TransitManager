package com.lx.mtrtm;

import com.lx.mtrtm.config.Config;
import com.lx.mtrtm.mixin.RailwayDataPathGenerationModuleAccessorMixin;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import mtr.data.RailwayData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class TransitManager implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("TransitManager");
    public static final LongList depotPathToBeInterrupted = new LongArrayList();
    public static final LongList disableTrainCollision = new LongArrayList();

    @Override
    public void onInitialize() {
        LOGGER.info("[TransitManager] TransitManager initialized \\(＾▽＾)/");
        Config.load();

        Mappings.registerCommands((dispatcher) -> {
            CommandHandler.registerCommands(dispatcher);
        });

        /* Abort MTR Path Generation Thread on Server Shutdown */
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for(World world : server.getWorlds()) {
                RailwayData data = RailwayData.getInstance(world);
                if(data != null) {
                    depotPathToBeInterrupted.addAll(((RailwayDataPathGenerationModuleAccessorMixin)data.railwayDataPathGenerationModule).getGeneratingPathThreads().keySet());
                    for(Long id : new ArrayList<>(depotPathToBeInterrupted)) {
                        data.railwayDataPathGenerationModule.generatePath(server, id);
                        LOGGER.info("[TransitManager] Terminating Path Generation Thread for Depot ID: " + id + " as server is shutting down!");
                    }
                }
            }
        });
    }
}
