package com.lx.mtrtm.commands;

import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mtr.data.RailwayData;
import mtr.data.Siding;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class deployTrain {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("deployTrain")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                .executes(context -> deploy(context, context.getSource().getPlayer().getPos()))
        );
    }

    private static int deploy(CommandContext<ServerCommandSource> context, Vec3d playerPos) {
        RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getWorld(), playerPos);

        if(trainData == null) {
            context.getSource().sendFeedback(Mappings.literalText("Cannot find any train.").formatted(Formatting.RED), false);
            return 1;
        }

        List<Siding> trainSidings = data.sidings.stream().filter(siding -> siding.id == trainData.train.sidingId).toList();
        Siding trainSiding = null;
        if(!trainSidings.isEmpty()) {
            trainSiding = trainSidings.get(0);
        }

        if(trainSiding == null) {
            context.getSource().sendFeedback(Mappings.literalText("Cannot find the corresponding train siding, please refresh and clear depot.").formatted(Formatting.RED), false);
            return 1;
        }

        trainData.train.deployTrain();
        context.getSource().sendFeedback(Mappings.literalText("Deploying the nearest train (Siding " + trainSiding.name + ")...").formatted(Formatting.GREEN), false);
        context.getSource().sendFeedback(Mappings.literalText("Train ID: " + trainSiding.getTrainId()).formatted(Formatting.GREEN), false);

        if(trainData.isManual) {
            ((TrainAccessorMixin)trainData.train).setCurrentlyManual(false);
            context.getSource().sendFeedback(Mappings.literalText("NOTE: Train is in manual mode, automatically setting train to ATO.").formatted(Formatting.GREEN), false);
        }
        return 1;
    }
}
