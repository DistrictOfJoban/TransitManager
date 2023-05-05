package com.lx.mtrtm.commands;

import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class manualMode {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("manualMode")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                        .then(CommandManager.literal("manual")
                                .executes(context -> execute(context, true, false, false)))
                        .then(CommandManager.literal("allowManual")
                                .executes(context -> execute(context, false, true, false)))
                        .then(CommandManager.literal("auto")
                                .executes(context -> execute(context, false, false, true)))
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context, boolean isFullManual, boolean isAllowManual, boolean isFullAuto) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getWorld(), player.getPos());

        if(trainData == null) {
            context.getSource().sendFeedback(Mappings.literalText("Cannot find any train.").formatted(Formatting.RED), false);
            return 1;
        }

        if(isFullAuto) {
            ((TrainAccessorMixin)trainData.train).setCurrentlyManual(false);
            ((TrainAccessorMixin)trainData.train).setManualAllowed(false);
        }

        if(isAllowManual) {
            ((TrainAccessorMixin)trainData.train).setManualAllowed(true);
            ((TrainAccessorMixin)trainData.train).setCurrentlyManual(false);
        }

        if(isFullManual) {
            ((TrainAccessorMixin)trainData.train).setManualAllowed(true);
            ((TrainAccessorMixin)trainData.train).setCurrentlyManual(true);
        }
        Util.syncTrainToPlayers(trainData.train, context.getSource().getWorld().getPlayers());
        context.getSource().sendFeedback(Mappings.literalText("Manual Mode Set!").formatted(Formatting.GOLD), false);
        return 1;
    }
}
