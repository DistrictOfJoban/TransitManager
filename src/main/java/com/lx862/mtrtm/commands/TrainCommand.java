package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.data.TargetVehicle;
import com.lx862.mtrtm.mixin.*;
import com.lx862.mtrtm.util.DepartureIndexHelper;
import com.lx862.mtrtm.util.MtrUtil;
import com.lx862.mtrtm.util.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import net.minecraft.network.chat.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.mtr.core.Main;
import org.mtr.core.data.*;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Vector;
import org.mtr.mod.data.IGui;

public class TrainCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("train")
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("clear")
                        .executes(TrainCommand::clearNearestTrain)
                )
                .then(Commands.literal("deploy")
                        .then(Commands.argument("departureIndex", StringArgumentType.greedyString())
                            .suggests(DepartureIndexHelper::suggestDepartureIndex)
                            .executes(TrainCommand::deploy)
                        )
                )
                .then(Commands.literal("skipDwell")
                        .executes(TrainCommand::skipDwell)
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
                                .then(Commands.literal("stop")
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
                .executes(TrainCommand::printVehicleInfo)
        );
    }

    private static int deploy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String chosenDepartureIndex = StringArgumentType.getString(context, "departureIndex");
        int departureIndex = Integer.MAX_VALUE;
        try {
            departureIndex = Integer.parseInt(chosenDepartureIndex);
        } catch (NumberFormatException e) {
            try {
                departureIndex = Integer.parseInt(chosenDepartureIndex.substring(2).split(" ")[0]);
            } catch (Exception ignored) {
            }
        }

        Main tsc = InitAccessorMixin.mtrtm$getMain();
        Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());
        TargetVehicle targetVehicle = requireNearestVehicle(context);
        long sidingId = targetVehicle.vehicle.vehicleExtraData.getSidingId();
        Siding siding = simulator.sidingIdMap.get(sidingId);
        if(siding == null) return 0;
        LongArrayList sidingDepartures = ((SidingAccessorMixin)(Object)siding).mtrtm$getDepartures();


        if(departureIndex != -1) {
            LongArrayList usedDepartureIndex = new LongArrayList();
            ((SidingAccessorMixin)(Object)siding).mtrtm$getVehicles().forEach(e -> {
                usedDepartureIndex.add(e.getDepartureIndex());
            });

            if(usedDepartureIndex.contains(departureIndex)) {
                context.getSource().sendFailure(Component.literal("Train already deployed!"));
                return 0;
            }
        }

        if(departureIndex == Integer.MAX_VALUE || departureIndex >= sidingDepartures.size()) {
            context.getSource().sendFailure(Component.literal("Invalid departure index."));
            return 0;
        }

        targetVehicle.vehicle.startUp(departureIndex, departureIndex == -1 ? (((NameColorDataBaseSchemaAccessorMixin)targetVehicle.vehicle).mtrtm$getData().getCurrentMillis()) : sidingDepartures.getLong(departureIndex));

        final int departureIndexToUse = departureIndex;
        context.getSource().sendSuccess(() -> Component.literal("Deploying vehicle with departure index " + departureIndexToUse + " (Siding " + siding.getName() + ")...").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int jump(CommandContext<CommandSourceStack> context, boolean next, boolean isPath, boolean isPlatform, boolean isNextStop, boolean isSiding) throws CommandSyntaxException {
        TargetVehicle targetVehicle = requireNearestVehicle(context);

        VehicleSchemaAccessorMixin vehicleAccessor = ((VehicleSchemaAccessorMixin)targetVehicle.vehicle);
        double currentRailProgress = vehicleAccessor.mtrtm$getRailProgress();
        double targetDistance = -1;
        int pathIndex = -1;
        if(isNextStop) {
            targetDistance = targetVehicle.vehicle.vehicleExtraData.immutablePath.get((int)vehicleAccessor.mtrtm$getNextStoppingIndexAto()).getEndDistance();
        } else if(isSiding) {
            targetDistance = targetVehicle.vehicle.vehicleExtraData.immutablePath.get(0).getEndDistance();
        } else {
            var vehiclePath = new ObjectImmutableList<>(targetVehicle.vehicle.vehicleExtraData.immutablePath);
            boolean justOneMorePath = false;
            for(int i = 0; i < vehiclePath.size(); i++) {
                int pIndex;
                if(next) {
                    pIndex = i;
                } else {
                    pIndex = vehiclePath.size() - 1 - i;
                }

                PathData path = vehiclePath.get(pIndex);

                boolean isStoppablePlatform = path.getDwellTime() > 0;

                if((isPlatform && isStoppablePlatform) || isPath) {
                    double dist = path.getEndDistance();
                    if(next && dist > currentRailProgress) {
                        targetDistance = dist;
                        pathIndex = pIndex;
                        break;
                    }

                    if(!next && dist < currentRailProgress) {
                        if((targetVehicle.speedKmh == 0) || isPlatform || (targetVehicle.speedKmh > 0 && justOneMorePath) /* 1 more path if train is running */) {
                            targetDistance = dist;
                            pathIndex = pIndex;
                            break;
                        }

                        justOneMorePath = true;
                    }
                }
            }
        }

        if(targetDistance != -1) {
            final double finalTargetDistance = Math.round(targetDistance);
            vehicleAccessor.mtrtm$setNextStoppingIndexAto(Math.max(pathIndex, vehicleAccessor.mtrtm$getNextStoppingIndexAto()));
            vehicleAccessor.mtrtm$setRailProgress(targetDistance);
            ((VehicleExtraDataAccessorMixin)targetVehicle.vehicle.vehicleExtraData).mtrtm$forceUpdate(true);
            context.getSource().sendSuccess(() -> Component.literal("Jumped to distance " + finalTargetDistance + "m.").withStyle(ChatFormatting.GREEN), false);
        } else {
            context.getSource().sendFailure(Component.literal("Cannot find the next path to stop to."));
            return 0;
        }
        return 1;
    }

    private static int clearNearestTrain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Main tsc = InitAccessorMixin.mtrtm$getMain();
        Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());
        TargetVehicle targetVehicle = requireNearestVehicle(context);

        long sidingId = targetVehicle.vehicle.vehicleExtraData.getSidingId();
        Siding trainSiding = simulator.sidingIdMap.get(sidingId);

        ((SidingAccessorMixin)(Object)trainSiding).mtrtm$getVehicles().removeIf(vehicle -> vehicle.getId() == targetVehicle.vehicle.getId());
        context.getSource().sendSuccess(() -> Component.literal("Train cleared!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int skipDwell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TargetVehicle targetVehicle = requireNearestVehicle(context);

        ((VehicleSchemaAccessorMixin)targetVehicle.vehicle).mtrtm$setElapsedDwellTime(targetVehicle.totalDwellTime);
        context.getSource().sendSuccess(() -> Component.literal("Dwell time skipped!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    public static TargetVehicle requireNearestVehicle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        Main tsc = InitAccessorMixin.mtrtm$getMain();
        Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());

        TargetVehicle targetVehicle = MtrUtil.getNearestTrain(player, Util.toVector(context.getSource().getPosition()), simulator);

        if(targetVehicle == null) {
            throw new SimpleCommandExceptionType(Component.literal("Cannot find the nearest vehicle!")).create();
        }
        return targetVehicle;
    }

    public static int printVehicleInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Main tsc = InitAccessorMixin.mtrtm$getMain();
            Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());
            Vector targetPosition = Util.toVector(context.getSource().getPosition());
            TargetVehicle targetVehicle = requireNearestVehicle(context);
            double currentRailProgress = ((VehicleSchemaAccessorMixin)targetVehicle.vehicle).mtrtm$getRailProgress();

            Siding siding = simulator.sidings.stream().filter(sdg -> sdg.getId() == targetVehicle.vehicle.vehicleExtraData.getSidingId()).findFirst().orElse(null);
            if(siding == null) {
                context.getSource().sendSuccess(() -> Component.literal("Cannot find corresponding siding.").withStyle(ChatFormatting.RED), false);
                return 1;
            }

            Depot depot = siding.area;
            if(depot == null) {
                context.getSource().sendSuccess(() -> Component.literal("No depot associated with this siding.").withStyle(ChatFormatting.RED), false);
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

            MutableComponent manualTimeRemainingText = Component.literal(Util.getReadableTimeMs(targetVehicle.manualCooldownMs)).withStyle(ChatFormatting.GREEN);
            MutableComponent depotNameText = Component.literal(depotSidingName).setStyle(Style.EMPTY.withColor(depot.getColor()));
            MutableComponent routeNameText = Component.literal(currentRouteName).setStyle(Style.EMPTY.withColor(currentRouteColor));
            MutableComponent destinationText = currentRouteDestination == null ? null : Component.literal(currentRouteDestination).withStyle(ChatFormatting.GREEN);
            MutableComponent dwellText = Component.literal(dwellString).withStyle(ChatFormatting.GREEN);
            MutableComponent runningModeText = Component.literal(targetVehicle.isManual ? targetVehicle.isCurrentlyManual ? "Manual" : "ATO (Manual Available)" : "ATO").withStyle(ChatFormatting.GREEN);
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

            context.getSource().sendSuccess(() -> Component.literal("===== " + title + " =====").withStyle(ChatFormatting.GREEN), false);
            sendKeyValueFeedback(context, Component.literal("Rail Progress: "), Component.literal(String.format("%.1f", currentRailProgress) + "m"));
            sendKeyValueFeedback(context, Component.literal("Relative distance: "), Component.literal(Math.round(Util.getManhattenDistance(targetVehicle.positions[targetVehicle.closestCar], targetPosition)) + "m"));
            sendKeyValueFeedback(context, Component.literal("Mode: "), runningModeText);
            if(targetVehicle.isManual && targetVehicle.isCurrentlyManual) {
                sendKeyValueFeedback(context, Component.literal("Switching to ATO in: "), manualTimeRemainingText);
            }
            sendKeyValueFeedback(context, Component.literal("Depot/Siding: "), teleportToSavedRailText(depotNameText, siding));
            sendKeyValueFeedback(context, Component.literal("Running Route: "), routeNameText);
            sendKeyValueFeedback(context, Component.literal("Schedule Deviation: "), getDeviationText(targetVehicle.vehicle));
            if(targetVehicle.speedKmh == 0 && targetVehicle.totalDwellTime > 0) {
                sendKeyValueFeedback(context, Component.literal("Dwell left: "), dwellText);
            }

            if(destinationText != null) {
                sendKeyValueFeedback(context, Component.literal("Destination: "), teleportToSavedRailText(destinationText, lastRoutePlatform));
            }

            if(!targetVehicle.ridingEntities.isEmpty()) {
                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(ridingEntitiesStr.toString()).withStyle(ChatFormatting.GREEN));
                context.getSource().sendSuccess(() -> Component.literal("Riding players: (Hover Here)").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withHoverEvent(hoverEvent)), false);
            }
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void sendKeyValueFeedback(CommandContext<CommandSourceStack> context, MutableComponent key, MutableComponent value) {
        context.getSource().sendSuccess(() -> key.withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)).append(value), false);
    }

    private static MutableComponent teleportToSavedRailText(MutableComponent originalText, SavedRailBase<?, ?> savedRail) {
        if(savedRail == null) return originalText;
        Position midPos = savedRail.getMidPosition();
        HoverEvent hoverEventTp = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport").withStyle(ChatFormatting.GREEN));
        ClickEvent clickEventTp = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + midPos.getX() + " " + midPos.getY() + " " + midPos.getZ());
        return originalText.withStyle(ChatFormatting.UNDERLINE).withStyle(e -> e.withHoverEvent(hoverEventTp).withClickEvent(clickEventTp));
    }

    private static MutableComponent getDeviationText(Vehicle vehicle) {
        long deviation = ((VehicleAccessorMixin)vehicle).mtrtm$getDeviation();
        if(deviation > 0) {
            return Component.literal("+" + Util.getReadableTimeMs(Math.abs(deviation))).withStyle(ChatFormatting.RED);
        } else if(deviation < 0) {
            return Component.literal("-" + Util.getReadableTimeMs(Math.abs(deviation))).withStyle(ChatFormatting.GREEN);
        } else {
            return Component.literal("On-Time").withStyle(ChatFormatting.GREEN);
        }
    }
}
