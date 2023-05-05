package com.lx.mtrtm.commands;

import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.TransitManager;
import com.lx.mtrtm.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public class trainCollision {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("trainCollision")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                        .then(CommandManager.literal("disable")
                                .executes(context -> execute(context, context.getSource().getPlayer().getPos(), true))
                        )
                .then(CommandManager.literal("enable")
                        .executes(context -> execute(context, context.getSource().getPlayer().getPos(), false))
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context, Vec3d playerPos, boolean disable) {
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getWorld(), playerPos);

        if(trainData == null) {
            context.getSource().sendFeedback(Mappings.literalText("Cannot find any nearest train.").formatted(Formatting.RED), false);
            return 1;
        }

        if(!disable) {
            TransitManager.disableTrainCollision.remove(trainData.train.sidingId);
        } else {
            TransitManager.disableTrainCollision.add(trainData.train.sidingId);
        }

        context.getSource().sendFeedback(Mappings.literalText("Collision detection for the nearest train is now " + (disable ? "disabled" : "enabled")).formatted(Formatting.GREEN), false);
        return 1;
    }
}
