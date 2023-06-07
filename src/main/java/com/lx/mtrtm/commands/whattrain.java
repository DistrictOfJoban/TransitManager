package com.lx.mtrtm.commands;

import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.lx.mtrtm.mixin.TrainServerAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mtr.data.*;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class whattrain {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whattrain")
                .requires(ctx -> ctx.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> getNearestTrain(context, EntityArgument.getPlayer(context, "player")))
                        )
                .executes(context -> getNearestTrain(context, context.getSource().getPlayer())));
    }

    private static int getNearestTrain(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getLevel(), player);

        if(trainData == null) {
            context.getSource().sendSuccess(Mappings.literalText("Cannot find any train.").withStyle(ChatFormatting.RED), false);
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
            context.getSource().sendSuccess(Mappings.literalText("Cannot find corresponding siding.").withStyle(ChatFormatting.RED), false);
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
            context.getSource().sendSuccess(Mappings.literalText("No depot associated with that siding.").withStyle(ChatFormatting.RED), false);
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

        MutableComponent depotName = Mappings.literalText(depotSidingName).withStyle(style -> style.withColor(depotColor));
        MutableComponent routeName = Mappings.literalText(currentRouteName).withStyle(style -> style.withColor(currentRouteColor));
        MutableComponent destinationName = currentRouteDestination == null ? null : Mappings.literalText(currentRouteDestination).withStyle(ChatFormatting.GREEN);
        String title = IGui.formatStationName(trainData.train.trainId) + " (" + trainData.train.trainCars + "-cars)";
        MutableComponent pos = Mappings.literalText(String.format("%d, %d, %d", Math.round(trainData.positions[0].x()), Math.round(trainData.positions[0].y()), Math.round(trainData.positions[0].z()))).withStyle(ChatFormatting.GREEN);
        MutableComponent dwell = Mappings.literalText(dwellString).withStyle(ChatFormatting.GREEN);
        MutableComponent isManual = Mappings.literalText(trainData.isManual ? trainData.isCurrentlyManual ? "Manual (Currently Manual)" : "Manual (Current ATO)" : "ATO").withStyle(ChatFormatting.GREEN);

        MutableComponent trainNotch = Mappings.literalText(trainData.accelerationSign == -2 ? "B2" : trainData.accelerationSign == -1 ? "B1" : trainData.accelerationSign == 0 ? "N" : trainData.accelerationSign == 1 ? "P1" : "P2").withStyle(ChatFormatting.GREEN);
        MutableComponent PMLeft = Mappings.literalText((Math.round((((trainData.manualToAutomaticTime * 10) - trainData.manualCooldown)) / 20F)) + "s").withStyle(ChatFormatting.GREEN);

        Set<UUID> ridingEntities = ((TrainAccessorMixin)trainData.train).getRidingEntities();
        StringBuilder ridingEntitiesStr = new StringBuilder();
        for (UUID uuid : ((TrainAccessorMixin)trainData.train).getRidingEntities()) {
            ServerPlayer ridingPlayer = context.getSource().getServer().getPlayerList().getPlayer(uuid);
            if (ridingPlayer == null) continue;

            ridingEntitiesStr.append(ridingPlayer.getGameProfile().getName()).append("\n");
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText(ridingEntitiesStr.toString()).withStyle(ChatFormatting.GREEN));
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/traininv " + siding.id);
        HoverEvent hoverEventTpToSiding = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText("Teleport to siding").withStyle(ChatFormatting.GREEN));
        ClickEvent clickEventTpToSiding = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + sidingMidPos.getX() + " " + sidingMidPos.getY() + " " + sidingMidPos.getZ());

        context.getSource().sendSuccess(Mappings.literalText("===== " + title + " =====").withStyle(ChatFormatting.GREEN), false);
        sendKeyValueFeedback(context, Mappings.literalText("Mode: "), isManual);
        sendKeyValueFeedback(context, Mappings.literalText("Depot/Siding: "), depotName.withStyle(style -> style.withHoverEvent(hoverEventTpToSiding).withClickEvent(clickEventTpToSiding)));
        sendKeyValueFeedback(context, Mappings.literalText("Position: "), pos);
        sendKeyValueFeedback(context, Mappings.literalText("Running Route: "), routeName);
        if(trainData.train.getSpeed() == 0 && trainData.train.getTotalDwellTicks() > 0) {
            sendKeyValueFeedback(context, Mappings.literalText("Dwell left: "), dwell);
        }

        if(destinationName != null) {
            sendKeyValueFeedback(context, Mappings.literalText("Destination: "), destinationName);
        }

        if(!((TrainAccessorMixin)trainData.train).getInventory().isEmpty()) {
            context.getSource().sendSuccess(Mappings.literalText("Train Inventory: (Click Here)").withStyle(ChatFormatting.GOLD).withStyle(style -> style.withClickEvent(clickEvent)), false);
        }

        if(trainData.isManual && trainData.isCurrentlyManual) {
            sendKeyValueFeedback(context, Mappings.literalText("Train Notch: "), trainNotch);
            sendKeyValueFeedback(context, Mappings.literalText("Switching to ATO in: "), PMLeft);
        }

        if(ridingEntities.size() > 0) {
            context.getSource().sendSuccess(Mappings.literalText("Riding players: (Hover Here)").withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(hoverEvent)), false);
        }
        return 1;
    }

    private static void sendKeyValueFeedback(CommandContext<CommandSourceStack> context, MutableComponent key, MutableComponent value) {
        context.getSource().sendSuccess(key.withStyle(ChatFormatting.GOLD).append(value), false);
    }
}
