package com.lx.mtrtm;

import com.mojang.brigadier.CommandDispatcher;
import com.lx.mtrtm.commands.*;

public class CommandHandler {
    public static void registerCommands(CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        whatplatform.register(dispatcher);
        mtrtm.register(dispatcher);
        whattrain.register(dispatcher);
        train.register(dispatcher);
        traininv.register(dispatcher);
        mtrpath.register(dispatcher);
        warpstn.register(dispatcher);
    }
}
