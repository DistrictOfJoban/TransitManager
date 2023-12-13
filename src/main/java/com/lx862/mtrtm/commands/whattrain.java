package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.data.ExposedTrainData;
import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.Util;
import com.lx862.mtrtm.mixin.TrainAccessorMixin;
import com.lx862.mtrtm.mixin.TrainServerAccessorMixin;
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

    public static int getNearestTrain(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getLevel(), player, player.getEyePosition());

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
        String currentRouteName = "N/A";
        final int currentRouteColor;
        String currentRouteDestination = null;
        String dwellString;

        double remainingDwell = (trainData.train.getTotalDwellTicks() - trainData.train.getElapsedDwellTicks()) / SharedConstants.TICKS_PER_SECOND;
        int displayedDwell = (int)Math.round(remainingDwell);
        if(remainingDwell < 0) {
            dwellString = "0s (" + Math.abs(displayedDwell) + " overdue)";
        } else {
            dwellString = Util.getReadableTimeMs(displayedDwell * 1000L);
        }

        Platform lastPlatformInRoute = null;

        if(!trainRoutes.isEmpty()) {
            Route runningRoute = trainRoutes.get(0);
            currentRouteColor = runningRoute.color;
            currentRouteName = IGui.formatStationName(runningRoute.name);

            long lastPlatformId = runningRoute.getLastPlatformId();
            Station lastStation = data.dataCache.platformIdToStation.get(lastPlatformId);
            lastPlatformInRoute = data.dataCache.platformIdMap.get(lastPlatformId);
            if(lastStation == null) {
                BlockPos midPos = lastPlatformInRoute.getMidPos();
                currentRouteDestination = "Platform " + lastPlatformInRoute.name + " (" + midPos.getX() + ", " + midPos.getY() + ", " + midPos.getZ()  + ")";
            } else {
                currentRouteDestination = IGui.formatStationName(lastStation.name) + " (" + lastPlatformInRoute.name + ")";
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

        int manualToAutoTimeMs = (trainData.manualToAutomaticTime * 10) * 50;
        int manualCooldownMs = trainData.manualCooldown * 50;
        MutableComponent PMLeft = Mappings.literalText(Util.getReadableTimeMs(manualToAutoTimeMs - manualCooldownMs)).withStyle(ChatFormatting.GREEN);

        Set<UUID> ridingEntities = ((TrainAccessorMixin)trainData.train).getRidingEntities();
        StringBuilder ridingEntitiesStr = new StringBuilder();
        for (UUID uuid : ((TrainAccessorMixin)trainData.train).getRidingEntities()) {
            ServerPlayer ridingPlayer = context.getSource().getServer().getPlayerList().getPlayer(uuid);
            if (ridingPlayer == null) continue;

            ridingEntitiesStr.append(ridingPlayer.getGameProfile().getName()).append("\n");
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText(ridingEntitiesStr.toString()).withStyle(ChatFormatting.GREEN));
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/traininv " + siding.id);

        context.getSource().sendSuccess(Mappings.literalText("===== " + title + " =====").withStyle(ChatFormatting.GREEN), false);
        sendKeyValueFeedback(context, Mappings.literalText("Mode: "), isManual);
        sendKeyValueFeedback(context, Mappings.literalText("Depot/Siding: "), teleportToSavedRailText(depotName, siding));
        sendKeyValueFeedback(context, Mappings.literalText("Position: "), pos);
        sendKeyValueFeedback(context, Mappings.literalText("Running Route: "), routeName);
        if(trainData.train.getSpeed() == 0 && trainData.train.getTotalDwellTicks() > 0) {
            sendKeyValueFeedback(context, Mappings.literalText("Dwell left: "), dwell);
        }

        if(destinationName != null) {
            sendKeyValueFeedback(context, Mappings.literalText("Destination: "), teleportToSavedRailText(destinationName, lastPlatformInRoute));
        }

        if(!((TrainAccessorMixin)trainData.train).getInventory().isEmpty()) {
            context.getSource().sendSuccess(Mappings.literalText("Train Inventory: (Click Here)").withStyle(ChatFormatting.GOLD).withStyle(style -> style.withClickEvent(clickEvent)), false);
        }

        if(trainData.isManual && trainData.isCurrentlyManual) {
            sendKeyValueFeedback(context, Mappings.literalText("Train Notch: "), trainNotch);
            sendKeyValueFeedback(context, Mappings.literalText("Switching to ATO in: "), PMLeft);
        }

        if(!ridingEntities.isEmpty()) {
            context.getSource().sendSuccess(Mappings.literalText("Riding players: (Hover Here)").withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(hoverEvent)), false);
        }
        return 1;
    }

    private static void sendKeyValueFeedback(CommandContext<CommandSourceStack> context, MutableComponent key, MutableComponent value) {
        context.getSource().sendSuccess(key.withStyle(ChatFormatting.GOLD).append(value), false);
    }

    private static MutableComponent teleportToSavedRailText(MutableComponent originalText, SavedRailBase savedRail) {
        if(savedRail == null) return originalText;
        BlockPos midPos = savedRail.getMidPos();
        HoverEvent hoverEventTp = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText("Click to teleport").withStyle(ChatFormatting.GREEN));
        ClickEvent clickEventTp = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + midPos.getX() + " " + midPos.getY() + " " + midPos.getZ());
        return originalText.withStyle(ChatFormatting.UNDERLINE).withStyle(e -> e.withHoverEvent(hoverEventTp).withClickEvent(clickEventTp));
    }
}
