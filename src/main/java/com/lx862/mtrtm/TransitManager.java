package com.lx862.mtrtm;

import com.lx862.mtrtm.config.Config;
import com.lx862.mtrtm.data.TrainState;
import it.unimi.dsi.fastutil.longs.*;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransitManager implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("TransitManager");
    public static final LongList stopPathGenDepotList = new LongArrayList();
    public static final Long2IntOpenHashMap trainStateList = new Long2IntOpenHashMap();
    public static final Long2LongArrayMap pathGenerationTimer = new Long2LongArrayMap();

    @Override
    public void onInitialize() {
        LOGGER.info("[TransitManager] TransitManager initialized \\(＾▽＾)/");
        Config.load();

        Mappings.registerCommands((dispatcher) -> {
            CommandHandler.registerCommands(dispatcher);
        });
    }

    public static boolean getTrainState(long trainId, TrainState trainState) {
        int state = trainStateList.get(trainId);
        int pos = trainState.getPos();

        return ((state >> pos) & 1) == 1;
    }

    public static void setTrainState(long trainId, TrainState trainState, boolean value) {
        int state = trainStateList.getOrDefault(trainId, 0);
        int pos = trainState.getPos();
        if(value) {
            state = state | (1 << pos);
        } else {
            state = state & ~(1 << pos);
        }


        trainStateList.put(trainId, state);
    }
}
