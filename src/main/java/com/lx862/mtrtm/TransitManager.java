package com.lx862.mtrtm;

import com.lx862.mtrtm.config.Config;
import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransitManager implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("TransitManager");
    public static final LongList stopPathGenDepotList = new LongArrayList();
    public static final LongList disableTrainCollision = new LongArrayList();
    public static final Long2LongArrayMap pathGenerationTimer = new Long2LongArrayMap();

    @Override
    public void onInitialize() {
        LOGGER.info("[TransitManager] TransitManager initialized \\(＾▽＾)/");
        Config.load();

        Mappings.registerCommands((dispatcher) -> {
            CommandHandler.registerCommands(dispatcher);
        });
    }
}
