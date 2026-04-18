package com.lx862.mtrtm;

import com.lx862.mtrtm.config.TMConfig;
import com.lx862.mtrtm.data.TrainState;
import it.unimi.dsi.fastutil.longs.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransitManager implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("TransitManager");
    public static final Long2IntOpenHashMap trainStateList = new Long2IntOpenHashMap();

    @Override
    public void onInitialize() {
        LOGGER.info("[TransitManager] TransitManager initialized \\(＾▽＾)/");
        TMConfig.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, third) -> {
            Commands.registerCommands(dispatcher);
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
