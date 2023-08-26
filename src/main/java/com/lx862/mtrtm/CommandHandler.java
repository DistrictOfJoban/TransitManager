package com.lx862.mtrtm;

import com.lx862.mtrtm.commands.*;
import com.mojang.brigadier.CommandDispatcher;

public class CommandHandler {
    public static void registerCommands(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        whatplatform.register(dispatcher);
        mtrtm.register(dispatcher);
        whattrain.register(dispatcher);
        train.register(dispatcher);
        traininv.register(dispatcher);
        mtrpath.register(dispatcher);
        warpstn.register(dispatcher);
        warpdepot.register(dispatcher);
    }
}
