package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.config.Config;
import com.lx862.mtrtm.Mappings;
import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class mtrtm {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            dispatcher.register(Commands.literal("mtrtm")
                    .requires(ctx -> ctx.hasPermission(2))
                    .then(Commands.literal("reload")
                    .executes(context -> {
                        context.getSource().sendSuccess(Mappings.literalText("Reloading Config...").withStyle(ChatFormatting.GOLD), false);
                        List<String> error = reloadConfig();
                        if(!error.isEmpty()) {
                            String failed = String.join(",", error);
                            context.getSource().sendSuccess(Mappings.literalText("Config Reloaded. " + failed + " failed to load.").withStyle(ChatFormatting.RED), false);
                            context.getSource().sendSuccess(Mappings.literalText("Please check whether the JSON syntax is correct!").withStyle(ChatFormatting.RED), false);
                        } else {
                            context.getSource().sendSuccess(Mappings.literalText("Config Reloaded!").withStyle(ChatFormatting.GREEN), false);

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
