package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.data.TargetVehicle;
import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.mixin.VehicleAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.lx862.mtrtm.util.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import org.mtr.core.Main;
import org.mtr.core.data.*;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.holder.TextColor;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.data.IGui;

public class TrainCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("train")
                .requires(ctx -> ctx.hasPermission(2))
//                .then(Commands.literal("board")
//                        .executes(context -> board(context))
//                )
//                .then(Commands.literal("ejectAllPassengers")
//                        .executes(context -> ejectPassengers(context))
//                )
//                .then(Commands.literal("clear")
//                        .executes(context -> clearNearestTrain(context))
//                )
//                .then(Commands.literal("deploy")
//                        .executes(context -> deploy(context))
//                )
//                .then(Commands.literal("skipDwell")
//                        .executes(context -> skipDwell(context))
//                )
//                .then(Commands.literal("jump")
//                        .then(Commands.literal("siding")
//                                .executes(context -> jump(context, true, false, false, false, true))
//                        )
//                        .then(Commands.literal("next")
//                                .then(Commands.literal("platform")
//                                        .executes(context -> jump(context, true, false, true, false, false))
//                                )
//                                .then(Commands.literal("path")
//                                        .executes(context -> jump(context, true, true, false, false, false))
//                                )
//                                .then(Commands.literal("stopPosition")
//                                        .executes(context -> jump(context, true, false, false, true, false))
//                                )
//                        )
//                        .then(Commands.literal("previous")
//                                .then(Commands.literal("platform")
//                                        .executes(context -> jump(context, false, false, true, false, false))
//                                )
//                                .then(Commands.literal("path")
//                                        .executes(context -> jump(context, false, true, false, false, false))
//                                )
//                        )
//                )
                .executes(TrainCommand::getNearestVehicle)
        );
    }

    private static int deploy(CommandContext<CommandSourceStack> context) {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//
//        List<Siding> trainSidings = data.sidings.stream().filter(siding -> siding.id == nearestTrain.train.sidingId).toList();
//        Siding trainSiding = null;
//        if(!trainSidings.isEmpty()) {
//            trainSiding = trainSidings.get(0);
//        }
//
//        nearestTrain.train.deployTrain();
//        context.getSource().sendSuccess(Mappings.literalText("Deploying the nearest train (Siding " + trainSiding.name + ")...").withStyle(ChatFormatting.GREEN), false);
//        context.getSource().sendSuccess(Mappings.literalText("Train ID: " + trainSiding.getTrainId()).withStyle(ChatFormatting.GREEN), false);
//
//        if(nearestTrain.isManual) {
//            context.getSource().sendSuccess(Mappings.literalText("NOTE: Train is currently in manual mode.").withStyle(ChatFormatting.YELLOW), false);
//        }
        return 1;
    }

    private static int jump(CommandContext<CommandSourceStack> context, boolean next, boolean isPath, boolean isPlatform, boolean isNextStop, boolean isSiding) {
//        ExposedTrainData trainData = getNearestTrainOrError(context);
//        double currentRailProgress = trainData.train.getRailProgress();
//        List<Double> distances = ((TrainAccessorMixin)trainData.train).getDistances();
//
//        double targetDistance = -1;
//        int pathIndex = -1;
//
//        if(isNextStop) {
//            targetDistance = distances.get(((TrainAccessorMixin)trainData.train).getNextStoppingIndex());
//        } else if(isSiding) {
//            targetDistance = distances.get(0);
//        } else {
//            boolean justOneMorePath = false;
//            for(int i = 0; i < trainData.train.path.size(); i++) {
//                int pIndex;
//                if(next) {
//                    pIndex = i;
//                } else {
//                    pIndex = trainData.train.path.size() - 1 - i;
//                }
//
//                PathData path = trainData.train.path.get(pIndex);
//
//                boolean isStoppablePlatform = path.dwellTime > 0 && path.rail.railType == RailType.PLATFORM;
//
//                if((isPlatform && isStoppablePlatform) || isPath) {
//                    double dist = distances.get(pIndex);
//                    if(next && dist > currentRailProgress) {
//                        targetDistance = dist;
//                        pathIndex = pIndex;
//                        break;
//                    }
//
//                    if(!next && dist < currentRailProgress) {
//                        if((trainData.train.getSpeed() == 0) || isPlatform || (trainData.train.getSpeed() > 0 && justOneMorePath) /* 1 more path if train is running */) {
//                            targetDistance = dist;
//                            pathIndex = pIndex;
//                            break;
//                        }
//
//                        justOneMorePath = true;
//                    }
//                }
//            }
//        }
//
//        if(targetDistance != -1) {
//            ((TrainAccessorMixin)trainData.train).setNextStoppingIndex(Math.max(pathIndex, ((TrainAccessorMixin) trainData.train).getNextStoppingIndex()));
//            ((TrainAccessorMixin)trainData.train).setRailProgress(targetDistance);
//            MtrUtil.syncTrainToPlayers(trainData.train, context.getSource().getLevel().players());
//
//            context.getSource().sendSuccess(Mappings.literalText("Jumped to distance " + Math.round(targetDistance) + "m.").withStyle(ChatFormatting.GREEN), false);
//        } else {
//            throw new CommandRuntimeException(Mappings.literalText("Cannot find the next path to stop to."));
//        }
        return 1;
    }

    private static int ejectPassengers(CommandContext<CommandSourceStack> context) {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//
//        ((TrainAccessorMixin)nearestTrain.train).getRidingEntities().clear();
//        MtrUtil.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());
//        context.getSource().sendSuccess(Mappings.literalText("All passengers cleared from train!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int board(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//
//        ((TrainAccessorMixin)nearestTrain.train).getRidingEntities().add(context.getSource().getPlayerOrException().getUUID());
//        MtrUtil.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());
//        context.getSource().sendSuccess(Mappings.literalText("Train boarded!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearNearestTrain(CommandContext<CommandSourceStack> context) {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//        RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
//
//        long sidingId = nearestTrain.train.sidingId;
//        Siding trainSiding = railwayData.dataCache.sidingIdMap.get(sidingId);
//
//        ((SidingAccessorMixin)trainSiding).getTrains().removeIf(train -> train.id == nearestTrain.train.id);
//        context.getSource().sendSuccess(Mappings.literalText("Siding cleared!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int skipDwell(CommandContext<CommandSourceStack> context) {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//        ((TrainAccessorMixin)nearestTrain.train).setElapsedDwellTicks(nearestTrain.train.getTotalDwellTicks());
//        MtrUtil.syncTrainToPlayers(nearestTrain.train, context.getSource().getLevel().players());
//
//        context.getSource().sendSuccess(Mappings.literalText("Dwell time skipped!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int haltDwell(CommandContext<CommandSourceStack> context) {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//        boolean halted = TransitManager.getTrainState(nearestTrain.train.id, TrainState.HALT_DWELL);
//        TransitManager.setTrainState(nearestTrain.train.id, TrainState.HALT_DWELL, !halted);
//
//        context.getSource().sendSuccess(Mappings.literalText("Dwell timer for the nearest train has been " + (!halted ? "paused" : "resumed")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int haltSpeed(CommandContext<CommandSourceStack> context) {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//        boolean halted = TransitManager.getTrainState(nearestTrain.train.id, TrainState.HALT_SPEED);
//        TransitManager.setTrainState(nearestTrain.train.id, TrainState.HALT_SPEED, !halted);
//
//        context.getSource().sendSuccess(Mappings.literalText("The nearest train has " + (!halted ? "been brought to a halt" : "resumed it's operation")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int toggleCollision(CommandContext<CommandSourceStack> context) {
//        ExposedTrainData nearestTrain = getNearestTrainOrError(context);
//        boolean skipCollision = TransitManager.getTrainState(nearestTrain.train.id, TrainState.SKIP_COLLISION);
//        TransitManager.setTrainState(nearestTrain.train.id, TrainState.SKIP_COLLISION, !skipCollision);
//
//        context.getSource().sendSuccess(Mappings.literalText("Collision detection for the nearest train is now " + (!skipCollision ? "bypassed" : "reset to normal")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static TargetVehicle requireNearestVehicle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        Main tsc = InitAccessorMixin.getMain();
        Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).getSimulators(), context.getSource().getLevel());

        TargetVehicle targetVehicle = MtrUtil.getNearestTrain(player, Util.toVector(context.getSource().getPosition()), simulator);

        if(targetVehicle == null) {
            throw new SimpleCommandExceptionType(TextHelper.literal("Cannot find the nearest vehicle!").data).create();
        }
        return targetVehicle;
    }

    public static int getNearestVehicle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Main tsc = InitAccessorMixin.getMain();
        Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).getSimulators(), context.getSource().getLevel());
        Vector targetPosition = Util.toVector(context.getSource().getPosition());
        TargetVehicle targetVehicle = requireNearestVehicle(context);

        Siding siding = simulator.sidings.stream().filter(sdg -> sdg.getId() == targetVehicle.vehicle.vehicleExtraData.getSidingId()).findFirst().orElse(null);
        if(siding == null) {
            context.getSource().sendSuccess(() -> TextHelper.literal("Cannot find corresponding siding.").formatted(TextFormatting.RED).data, false);
            return 1;
        }

        Depot depot = siding.area;
        if(depot == null) {
            context.getSource().sendSuccess(() -> TextHelper.literal("No depot associated with this siding.").formatted(TextFormatting.RED).data, false);
            return 1;
        }

        String currentRouteName = MtrUtil.getRouteName(targetVehicle.vehicle.vehicleExtraData.getThisRouteName());
        if(currentRouteName.isEmpty()) currentRouteName = "N/A";
        final int currentRouteColor = targetVehicle.vehicle.vehicleExtraData.getThisRouteColor();
        String currentRouteDestination = null;
        String dwellString;

        double remainingDwell = (targetVehicle.totalDwellTime - targetVehicle.elapsedDwellTime);
        int displayedDwell = (int)Math.round(remainingDwell);
        if(remainingDwell < 0) {
            dwellString = "0s (" + Util.getReadableTimeMs(displayedDwell) + " overdue)";
        } else {
            dwellString = Util.getReadableTimeMs(displayedDwell);
        }

        Route runningRoute = simulator.routeIdMap.get(targetVehicle.vehicle.vehicleExtraData.getThisRouteId());
        Platform lastRoutePlatform = null;
        if(runningRoute != null && !runningRoute.getRoutePlatforms().isEmpty()) {
            lastRoutePlatform = runningRoute.getRoutePlatforms().get(runningRoute.getRoutePlatforms().size()-1).getPlatform();
            Station lastStation = lastRoutePlatform.area;
            if(lastStation == null) {
                Position midPos = lastRoutePlatform.getMidPosition();
                currentRouteDestination = "Platform " + lastRoutePlatform.getName() + " (" + midPos.getX() + ", " + midPos.getY() + ", " + midPos.getZ()  + ")";
            } else {
                currentRouteDestination = IGui.formatStationName(lastStation.getName()) + " (" + lastRoutePlatform.getName() + ")";
            }
        }

        final String depotSidingName = IGui.formatStationName(depot.getName())  + " (Siding " + siding.getName() + ")";

        MutableText manualTimeRemainingText = TextHelper.literal(Util.getReadableTimeMs(targetVehicle.manualCooldownMs)).formatted(TextFormatting.GREEN);
        MutableText depotNameText = TextHelper.setStyle(TextHelper.literal(depotSidingName), Style.getEmptyMapped().withColor(TextColor.fromRgb(depot.getColor())));
        MutableText routeNameText = TextHelper.setStyle(TextHelper.literal(currentRouteName), Style.getEmptyMapped().withColor(TextColor.fromRgb(currentRouteColor)));
        MutableText destinationText = currentRouteDestination == null ? null : TextHelper.literal(currentRouteDestination).formatted(TextFormatting.GREEN);
        MutableText dwellText = TextHelper.literal(dwellString).formatted(TextFormatting.GREEN);
        MutableText runningModeText = TextHelper.literal(targetVehicle.isManual ? targetVehicle.isCurrentlyManual ? "Manual" : "ATO (Manual Available)" : "ATO").formatted(TextFormatting.GREEN);
        String title = targetVehicle.vehicle.getHexId() + " (" + targetVehicle.positions.length + "-cars)";

        StringBuilder ridingEntitiesStr = new StringBuilder();
        for(int i = 0; i < targetVehicle.ridingEntities.size(); i++) {
            VehicleRidingEntity vehicleRidingEntity = targetVehicle.ridingEntities.get(i);
            ServerPlayer ridingPlayer = context.getSource().getServer().getPlayerList().getPlayer(vehicleRidingEntity.uuid);
            if(ridingPlayer != null) {
                ridingEntitiesStr.append(String.format("%s (Car %d)", ridingPlayer.getGameProfile().getName(), vehicleRidingEntity.getRidingCar()+1));
                if(i != targetVehicle.ridingEntities.size()-1) ridingEntitiesStr.append("\n");
            }
        }

        context.getSource().sendSuccess(() -> TextHelper.literal("===== " + title + " =====").formatted(TextFormatting.GREEN).data, false);
        sendKeyValueFeedback(context, TextHelper.literal("Distance: "), TextHelper.literal(Math.round(Util.getManhattenDistance(targetVehicle.positions[targetVehicle.closestCar], targetPosition)) + "m"));
        sendKeyValueFeedback(context, TextHelper.literal("Mode: "), runningModeText);
        if(targetVehicle.isManual && targetVehicle.isCurrentlyManual) {
            sendKeyValueFeedback(context, TextHelper.literal("Switching to ATO in: "), manualTimeRemainingText);
        }
        sendKeyValueFeedback(context, TextHelper.literal("Depot/Siding: "), teleportToSavedRailText(depotNameText, siding));
        sendKeyValueFeedback(context, TextHelper.literal("Running Route: "), routeNameText);
        sendKeyValueFeedback(context, TextHelper.literal("Schedule Deviation: "), getDeviationText(targetVehicle.vehicle));
        if(targetVehicle.speedKmh == 0 && targetVehicle.totalDwellTime > 0) {
            sendKeyValueFeedback(context, TextHelper.literal("Dwell left: "), dwellText);
        }

        if(destinationText != null) {
            sendKeyValueFeedback(context, TextHelper.literal("Destination: "), teleportToSavedRailText(destinationText, lastRoutePlatform));
        }

        if(!targetVehicle.ridingEntities.isEmpty()) {
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextHelper.literal(ridingEntitiesStr.toString()).formatted(TextFormatting.GREEN).data);
            context.getSource().sendSuccess(() -> TextHelper.setStyle(TextHelper.literal("Riding players: (Hover Here)"), new Style(Style.getEmptyMapped().withColor(TextFormatting.GOLD).data.withHoverEvent(hoverEvent))).data, false);
        }
        return 1;
    }

    private static void sendKeyValueFeedback(CommandContext<CommandSourceStack> context, MutableText key, MutableText value) {
        context.getSource().sendSuccess(() -> TextHelper.setStyle(key, Style.getEmptyMapped().withColor(TextFormatting.GOLD)).data.append(value.data), false);
    }

    private static MutableText teleportToSavedRailText(MutableText originalText, SavedRailBase<?, ?> savedRail) {
        if(savedRail == null) return originalText;
        Position midPos = savedRail.getMidPosition();
        HoverEvent hoverEventTp = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextHelper.literal("Click to teleport").data.withStyle(ChatFormatting.GREEN));
        ClickEvent clickEventTp = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + midPos.getX() + " " + midPos.getY() + " " + midPos.getZ());
        return new MutableText(originalText.data.withStyle(ChatFormatting.UNDERLINE).withStyle(e -> e.withHoverEvent(hoverEventTp).withClickEvent(clickEventTp)));
    }

    private static MutableText getDeviationText(Vehicle vehicle) {
        long deviation = ((VehicleAccessorMixin)vehicle).getDeviation();
        if(deviation > 0) {
            return TextHelper.literal("+" + Util.getReadableTimeMs(Math.abs(deviation))).formatted(TextFormatting.RED);
        } else if(deviation < 0) {
            return TextHelper.literal("-" + Util.getReadableTimeMs(Math.abs(deviation))).formatted(TextFormatting.GREEN);
        } else {
            return TextHelper.literal("On-Time").formatted(TextFormatting.GREEN);
        }
    }
}
