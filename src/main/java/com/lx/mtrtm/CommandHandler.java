package com.lx.mtrtm;

import com.mojang.brigadier.CommandDispatcher;
import com.lx.mtrtm.commands.*;

public class CommandHandler {
    public static void registerCommands(CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        deployTrain.register(dispatcher);
        ejectFromTrain.register(dispatcher);
        manualMode.register(dispatcher);
        whatplatform.register(dispatcher);
        mtrtm.register(dispatcher);
        whattrain.register(dispatcher);
        trainCollision.register(dispatcher);
        traininv.register(dispatcher);
        mtrpath.register(dispatcher);
        warpstn.register(dispatcher);
    }
}
