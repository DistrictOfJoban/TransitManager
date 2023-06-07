package com.lx.mtrtm.commands;

import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.lx.mtrtm.mixin.TrainServerAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mtr.data.*;
import net.minecraft.SharedConstants;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class whattrain {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("whattrain")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> getNearestTrain(context, EntityArgumentType.getPlayer(context, "player")))
                        )
                .executes(context -> getNearestTrain(context, context.getSource().getPlayer())));
    }

    private static int getNearestTrain(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getWorld(), player);

        if(trainData == null) {
            context.getSource().sendFeedback(Mappings.literalText("Cannot find any train.").formatted(Formatting.RED), false);
            return 1;
        }

        if(trainData.isManual) {
            trainData.isCurrentlyManual = ((TrainAccessorMixin)trainData.train).getIsCurrentlyManual();
            if(trainData.isCurrentlyManual) {
                trainData.accelerationSign = ((TrainAccessorMixin) trainData.train).getManualNotch();
                trainData.manualCooldown = ((TrainServerAccessorMixin)trainData.train).getManualCoolDown();
                trainData.manualToAutomaticTime = ((TrainAccessorMixin) trainData.train).getManualToAutomaticTime();
            }
        }

        long sidingId = trainData.train.sidingId;
        List<Siding> sidingList = data.sidings.stream().filter(sd -> sd.id == sidingId).toList();

        if(sidingList.isEmpty()) {
            context.getSource().sendFeedback(Mappings.literalText("Cannot find corresponding siding.").formatted(Formatting.RED), false);
            return 1;
        }

        Siding siding = sidingList.get(0);
        Depot sidingDepot = null;

        for(Depot depot : data.depots) {
            if(depot.inArea(siding.getMidPos().getX(), siding.getMidPos().getZ())) {
                sidingDepot = depot;
                break;
            }
        }

        if(sidingDepot == null) {
            context.getSource().sendFeedback(Mappings.literalText("No depot associated with that siding.").formatted(Formatting.RED), false);
            return 1;
        }

        List<Route> trainRoutes = data.routes.stream().filter(rt -> rt.id == trainData.routeId).toList();
        BlockPos sidingMidPos = siding.getMidPos();
        String currentRouteName = "N/A";
        final int currentRouteColor;
        String currentRouteDestination = null;
        String dwellString;

        double remainingDwell = (trainData.train.getTotalDwellTicks() - trainData.train.getElapsedDwellTicks()) / SharedConstants.TICKS_PER_SECOND;
        int displayedDwell = (int)Math.round(remainingDwell);
        if(remainingDwell < 0) {
            dwellString = "0s (" + Math.abs(displayedDwell) + "s overdue)";
        } else {
            dwellString = displayedDwell + "s";
        }


        if(!trainRoutes.isEmpty()) {
            Route runningRoute = trainRoutes.get(0);
            currentRouteColor = runningRoute.color;
            currentRouteName = IGui.formatStationName(runningRoute.name);

            long lastPlatformId = runningRoute.getLastPlatformId();
            Station lastStation = data.dataCache.platformIdToStation.get(lastPlatformId);
            Platform platform = data.dataCache.platformIdMap.get(lastPlatformId);
            if(lastStation == null) {
                BlockPos midPos = platform.getMidPos();
                currentRouteDestination = "Platform " + platform.name + " (" + midPos.getX() + ", " + midPos.getY() + ", " + midPos.getZ()  + ")";
            } else {
                currentRouteDestination = IGui.formatStationName(lastStation.name) + " (" + platform.name + ")";
            }
        } else {
            currentRouteColor = 0;
        }

        final int depotColor = sidingDepot.color;
        final String depotSidingName = IGui.formatStationName(sidingDepot.name)  + " (Siding " + siding.name + ")";

        MutableText depotName = Mappings.literalText(depotSidingName).styled(style -> style.withColor(depotColor));
        MutableText routeName = Mappings.literalText(currentRouteName).styled(style -> style.withColor(currentRouteColor));
        MutableText destinationName = currentRouteDestination == null ? null : Mappings.literalText(currentRouteDestination).formatted(Formatting.GREEN);
        String title = IGui.formatStationName(trainData.train.trainId) + " (" + trainData.train.trainCars + "-cars)";
        MutableText pos = Mappings.literalText(String.format("%d, %d, %d", Math.round(trainData.positions[0].getX()), Math.round(trainData.positions[0].getY()), Math.round(trainData.positions[0].getZ()))).formatted(Formatting.GREEN);
        MutableText dwell = Mappings.literalText(dwellString).formatted(Formatting.GREEN);
        MutableText isManual = Mappings.literalText(trainData.isManual ? trainData.isCurrentlyManual ? "Manual (Currently Manual)" : "Manual (Current ATO)" : "ATO").formatted(Formatting.GREEN);

        MutableText trainNotch = Mappings.literalText(trainData.accelerationSign == -2 ? "B2" : trainData.accelerationSign == -1 ? "B1" : trainData.accelerationSign == 0 ? "N" : trainData.accelerationSign == 1 ? "P1" : "P2").formatted(Formatting.GREEN);
        MutableText PMLeft = Mappings.literalText((Math.round((((trainData.manualToAutomaticTime * 10) - trainData.manualCooldown)) / 20F)) + "s").formatted(Formatting.GREEN);

        Set<UUID> ridingEntities = ((TrainAccessorMixin)trainData.train).getRidingEntities();
        StringBuilder ridingEntitiesStr = new StringBuilder();
        for (UUID uuid : ((TrainAccessorMixin)trainData.train).getRidingEntities()) {
            ServerPlayerEntity ridingPlayer = context.getSource().getServer().getPlayerManager().getPlayer(uuid);
            if (ridingPlayer == null) continue;

            ridingEntitiesStr.append(ridingPlayer.getGameProfile().getName()).append("\n");
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText(ridingEntitiesStr.toString()).formatted(Formatting.GREEN));
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/traininv " + siding.id);
        HoverEvent hoverEventTpToSiding = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText("Teleport to siding").formatted(Formatting.GREEN));
        ClickEvent clickEventTpToSiding = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + sidingMidPos.getX() + " " + sidingMidPos.getY() + " " + sidingMidPos.getZ());

        context.getSource().sendFeedback(Mappings.literalText("===== " + title + " =====").formatted(Formatting.GREEN), false);
        sendKeyValueFeedback(context, Mappings.literalText("Mode: "), isManual);
        sendKeyValueFeedback(context, Mappings.literalText("Depot/Siding: "), depotName.styled(style -> style.withHoverEvent(hoverEventTpToSiding).withClickEvent(clickEventTpToSiding)));
        sendKeyValueFeedback(context, Mappings.literalText("Position: "), pos);
        sendKeyValueFeedback(context, Mappings.literalText("Running Route: "), routeName);
        if(trainData.train.getSpeed() == 0 && trainData.train.getTotalDwellTicks() > 0) {
            sendKeyValueFeedback(context, Mappings.literalText("Dwell left: "), dwell);
        }

        if(destinationName != null) {
            sendKeyValueFeedback(context, Mappings.literalText("Destination: "), destinationName);
        }

        if(!((TrainAccessorMixin)trainData.train).getInventory().isEmpty()) {
            context.getSource().sendFeedback(Mappings.literalText("Train Inventory: (Click Here)").formatted(Formatting.GOLD).styled(style -> style.withClickEvent(clickEvent)), false);
        }

        if(trainData.isManual && trainData.isCurrentlyManual) {
            sendKeyValueFeedback(context, Mappings.literalText("Train Notch: "), trainNotch);
            sendKeyValueFeedback(context, Mappings.literalText("Switching to ATO in: "), PMLeft);
        }

        if(ridingEntities.size() > 0) {
            context.getSource().sendFeedback(Mappings.literalText("Riding players: (Hover Here)").formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(hoverEvent)), false);
        }
        return 1;
    }

    private static void sendKeyValueFeedback(CommandContext<ServerCommandSource> context, MutableText key, MutableText value) {
        context.getSource().sendFeedback(key.formatted(Formatting.GOLD).append(value), false);
    }
}
