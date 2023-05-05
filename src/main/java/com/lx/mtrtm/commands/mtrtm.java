package com.lx.mtrtm.commands;

import com.lx.mtrtm.config.Config;
import com.lx.mtrtm.Mappings;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class mtrtm {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
            dispatcher.register(CommandManager.literal("mtrtm")
                    .requires(ctx -> ctx.hasPermissionLevel(2))
                    .then(CommandManager.literal("reload")
                    .executes(context -> {
                        context.getSource().sendFeedback(Mappings.literalText("Reloading Config...").formatted(Formatting.GOLD), false);
                        List<String> error = reloadConfig();
                        if(!error.isEmpty()) {
                            String failed = String.join(",", error);
                            context.getSource().sendFeedback(Mappings.literalText("Config Reloaded. " + failed + " failed to load.").formatted(Formatting.RED), false);
                            context.getSource().sendFeedback(Mappings.literalText("Please check whether the JSON syntax is correct!").formatted(Formatting.RED), false);
                        } else {
                            context.getSource().sendFeedback(Mappings.literalText("Config Reloaded!").formatted(Formatting.GREEN), false);

                        }
                        return 1;
                    }))
            );
    }

    public static List<String> reloadConfig() {
        List<String> error = new ArrayList<>();
        if(!Config.load()) {
            error.add("Main Config");
        }

        return error;
    }
}
