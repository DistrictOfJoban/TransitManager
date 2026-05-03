package com.lx862.mtrtm;

import com.lx862.mtrtm.commands.*;
import com.mojang.brigadier.CommandDispatcher;

public class Commands {
    public static void registerCommands(CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        TrainCommand.register(dispatcher);
        PlatformCommand.register(dispatcher);
        WarpCommands.register(dispatcher);
    }
}
