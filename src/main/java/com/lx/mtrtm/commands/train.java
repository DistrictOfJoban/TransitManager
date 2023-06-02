package com.lx.mtrtm.commands;

import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.TransitManager;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.mixin.SidingAccessorMixin;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.lx.mtrtm.mixin.TrainServerAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import mtr.data.RailType;
import mtr.data.RailwayData;
import mtr.data.Siding;
import mtr.data.TrainServer;
import mtr.path.PathData;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class train {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("train")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                .then(CommandManager.literal("collision")
                        .then(CommandManager.literal("disable")
                                .executes(context -> setCollision(context, true))
                        )
                        .then(CommandManager.literal("enable")
                                .executes(context -> setCollision(context, false))
                        )
                )
                .then(CommandManager.literal("ejectAllPassengers")
                        .executes(context -> ejectPassengers(context))
                )
                .then(CommandManager.literal("clear")
                        .executes(context -> clearNearestTrain(context))
                )
                .then(CommandManager.literal("deploy")
                        .executes(context -> deploy(context))
                )
                .then(CommandManager.literal("jump")
                        .then(CommandManager.literal("siding")
                                .executes(context -> jump(context, true, false, false, false, true))
                        )
                        .then(CommandManager.literal("next")
                                .then(CommandManager.literal("platform")
                                        .executes(context -> jump(context, true, false, true, false, false))
                                )
                                .then(CommandManager.literal("path")
                                        .executes(context -> jump(context, true, true, false, false, false))
                                )
                                .then(CommandManager.literal("stopPosition")
                                        .executes(context -> jump(context, true, false, false, true, false))
                                )
                        )
                        .then(CommandManager.literal("previous")
                                .then(CommandManager.literal("platform")
                                        .executes(context -> jump(context, false, false, true, false, false))
                                )
                                .then(CommandManager.literal("path")
                                        .executes(context -> jump(context, false, true, false, false, false))
                                )
                        )
                )
        );
    }

    private static int deploy(CommandContext<ServerCommandSource> context) {
        RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        List<Siding> trainSidings = data.sidings.stream().filter(siding -> siding.id == nearestTrain.train.sidingId).toList();
        Siding trainSiding = null;
        if(!trainSidings.isEmpty()) {
            trainSiding = trainSidings.get(0);
        }

        nearestTrain.train.deployTrain();
        context.getSource().sendFeedback(Mappings.literalText("Deploying the nearest train (Siding " + trainSiding.name + ")...").formatted(Formatting.GREEN), false);
        context.getSource().sendFeedback(Mappings.literalText("Train ID: " + trainSiding.getTrainId()).formatted(Formatting.GREEN), false);

        if(nearestTrain.isManual) {
            context.getSource().sendFeedback(Mappings.literalText("NOTE: Train is currently in manual mode.").formatted(Formatting.YELLOW), false);
        }
        return 1;
    }

    private static int jump(CommandContext<ServerCommandSource> context, boolean next, boolean isPath, boolean isPlatform, boolean isNextStop, boolean isSiding) {
        ExposedTrainData trainData = getNearestTrainOrError(context);
        double currentRailProgress = trainData.train.getRailProgress();
        List<Double> distances = ((TrainAccessorMixin)trainData.train).getDistances();

        double targetDistance = -1;

        if(isNextStop) {
            targetDistance = distances.get(((TrainAccessorMixin)trainData.train).getNextStoppingIndex());
        } else if(isSiding) {
            targetDistance = distances.get(0);
        } else {
            int i = 0;
            IntList validPathIndex = new IntArrayList();

            for(PathData path : trainData.train.path) {
                boolean isStoppablePlatform = path.dwellTime > 0 && path.rail.railType == RailType.PLATFORM;
                if((isPlatform && isStoppablePlatform) || isPath) {
                    validPathIndex.add(i);
                    double dist = distances.get(i);
                    if(dist > currentRailProgress) {
                        if(!next) {
                            boolean last2Path = !isPlatform;
                            dist = distances.get(validPathIndex.getInt(Math.max(0, validPathIndex.size() - 1 - (last2Path ? 2 : 1))));
                        }
                        targetDistance = dist;
                        break;
                    }
                }
                i++;
            }
        }

        if(targetDistance != -1) {
            ((TrainAccessorMixin)trainData.train).setRailProgress(targetDistance);
            Util.syncTrainToPlayers(trainData.train, context.getSource().getWorld().getPlayers());

            context.getSource().sendFeedback(Mappings.literalText("Jumped to distance " + Math.round(targetDistance) + "m.").formatted(Formatting.GREEN), false);
        } else {
            throw new CommandException(Mappings.literalText("Cannot find the next path to stop to."));
        }
        return 1;
    }

    private static int ejectPassengers(CommandContext<ServerCommandSource> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        ((TrainAccessorMixin)nearestTrain.train).getRidingEntities().clear();
        Util.syncTrainToPlayers(nearestTrain.train, context.getSource().getWorld().getPlayers());
        context.getSource().sendFeedback(Mappings.literalText("All passengers cleared from train!").formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int clearNearestTrain(CommandContext<ServerCommandSource> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        RailwayData railwayData = RailwayData.getInstance(context.getSource().getWorld());

        long sidingId = nearestTrain.train.sidingId;
        Siding trainSiding = railwayData.dataCache.sidingIdMap.get(sidingId);
        trainSiding.clearTrains();

        context.getSource().sendFeedback(Mappings.literalText("Siding cleared!").formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int setCollision(CommandContext<ServerCommandSource> context, boolean disable) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        if(!disable) {
            TransitManager.disableTrainCollision.remove(nearestTrain.train.sidingId);
        } else {
            TransitManager.disableTrainCollision.add(nearestTrain.train.sidingId);
        }

        context.getSource().sendFeedback(Mappings.literalText("Collision detection for the nearest train is now " + (disable ? "disabled" : "enabled")).formatted(Formatting.GREEN), false);
        return 1;
    }

    private static ExposedTrainData getNearestTrainOrError(CommandContext<ServerCommandSource> context) throws CommandException {
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getWorld(), context.getSource().getPlayer());

        if(trainData == null) {
            throw new CommandException(Mappings.literalText("Cannot find any nearest train!"));
        }
        return trainData;
    }
}
