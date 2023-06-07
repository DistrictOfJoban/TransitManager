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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class train {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("train")
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("collision")
                        .then(Commands.literal("disable")
                                .executes(context -> setCollision(context, true))
                        )
                        .then(Commands.literal("enable")
                                .executes(context -> setCollision(context, false))
                        )
                )
                .then(Commands.literal("board")
                        .executes(context -> board(context))
                )
                .then(Commands.literal("ejectAllPassengers")
                        .executes(context -> ejectPassengers(context))
                )
                .then(Commands.literal("clear")
                        .executes(context -> clearNearestTrain(context))
                )
                .then(Commands.literal("deploy")
                        .executes(context -> deploy(context))
                )
                .then(Commands.literal("skipDwell")
                        .executes(context -> skipDwell(context))
                )
                .then(Commands.literal("jump")
                        .then(Commands.literal("siding")
                                .executes(context -> jump(context, true, false, false, false, true))
                        )
                        .then(Commands.literal("next")
                                .then(Commands.literal("platform")
                                        .executes(context -> jump(context, true, false, true, false, false))
                                )
                                .then(Commands.literal("path")
                                        .executes(context -> jump(context, true, true, false, false, false))
                                )
                                .then(Commands.literal("stopPosition")
                                        .executes(context -> jump(context, true, false, false, true, false))
                                )
                        )
                        .then(Commands.literal("previous")
                                .then(Commands.literal("platform")
                                        .executes(context -> jump(context, false, false, true, false, false))
                                )
                                .then(Commands.literal("path")
                                        .executes(context -> jump(context, false, true, false, false, false))
                                )
                        )
                )
                .executes(context -> whattrain.getNearestTrain(context, context.getSource().getPlayer()))
        );
    }

    private static int deploy(CommandContext<CommandSourceStack> context) {
        RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        List<Siding> trainSidings = data.sidings.stream().filter(siding -> siding.id == nearestTrain.train.sidingId).toList();
        Siding trainSiding = null;
        if(!trainSidings.isEmpty()) {
            trainSiding = trainSidings.get(0);
        }

        nearestTrain.train.deployTrain();
        context.getSource().sendSuccess(Mappings.literalText("Deploying the nearest train (Siding " + trainSiding.name + ")...").withStyle(ChatFormatting.GREEN), false);
        context.getSource().sendSuccess(Mappings.literalText("Train ID: " + trainSiding.getTrainId()).withStyle(ChatFormatting.GREEN), false);

        if(nearestTrain.isManual) {
            context.getSource().sendSuccess(Mappings.literalText("NOTE: Train is currently in manual mode.").withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int jump(CommandContext<CommandSourceStack> context, boolean next, boolean isPath, boolean isPlatform, boolean isNextStop, boolean isSiding) {
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
            Util.syncTrainToPlayers(trainData.train, context.getSource().getLevel().players());

            context.getSource().sendSuccess(Mappings.literalText("Jumped to distance " + Math.round(targetDistance) + "m.").withStyle(ChatFormatting.GREEN), false);
        } else {
            throw new CommandRuntimeException(Mappings.literalText("Cannot find the next path to stop to."));
        }
        return 1;
    }

    private static int ejectPassengers(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        ((TrainAccessorMixin)nearestTrain.train).getRidingEntities().clear();
        Util.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());
        context.getSource().sendSuccess(Mappings.literalText("All passengers cleared from train!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int board(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        ((TrainAccessorMixin)nearestTrain.train).getRidingEntities().add(context.getSource().getPlayer().getUUID());
        Util.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());
        context.getSource().sendSuccess(Mappings.literalText("Train boarded!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearNearestTrain(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());

        long sidingId = nearestTrain.train.sidingId;
        Siding trainSiding = railwayData.dataCache.sidingIdMap.get(sidingId);
        trainSiding.clearTrains();

        context.getSource().sendSuccess(Mappings.literalText("Siding cleared!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int skipDwell(CommandContext<CommandSourceStack> context) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
        ((TrainAccessorMixin)nearestTrain.train).setElapsedDwellTicks(nearestTrain.train.getTotalDwellTicks());
        Util.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());

        context.getSource().sendSuccess(Mappings.literalText("Dwell time skipped!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int setCollision(CommandContext<CommandSourceStack> context, boolean disable) {
        ExposedTrainData nearestTrain = getNearestTrainOrError(context);

        if(!disable) {
            TransitManager.disableTrainCollision.remove(nearestTrain.train.sidingId);
        } else {
            TransitManager.disableTrainCollision.add(nearestTrain.train.sidingId);
        }

        context.getSource().sendSuccess(Mappings.literalText("Collision detection for the nearest train is now " + (disable ? "disabled" : "enabled")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static ExposedTrainData getNearestTrainOrError(CommandContext<CommandSourceStack> context) throws CommandRuntimeException {
        ServerPlayer player = context.getSource().getPlayer();
        ExposedTrainData trainData;
        if (player != null) {
            trainData = Util.getNearestTrain(context.getSource().getLevel(), context.getSource().getPlayer(), context.getSource().getPlayer().getEyePosition());
        } else {
            trainData = Util.getNearestTrain(context.getSource().getLevel(), null, context.getSource().getPosition());
        }

        if(trainData == null) {
            throw new CommandRuntimeException(Mappings.literalText("Cannot find any nearest train!"));
        }
        return trainData;
    }
}
