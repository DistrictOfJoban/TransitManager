package com.lx.mtrtm.commands;

import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.lx.mtrtm.mixin.TrainServerAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mtr.data.*;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

public class whattrain {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("whattrain")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> getNearestTrain(context, EntityArgumentType.getPlayer(context, "player").getPos()))
                        )
                .executes(context -> getNearestTrain(context, context.getSource().getPlayer().getPos())));
    }

    private static int getNearestTrain(CommandContext<ServerCommandSource> context, Vec3d playerPos) {
        RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
        ExposedTrainData trainData = Util.getNearestTrain(context.getSource().getWorld(), playerPos);

        if(trainData == null) {
            context.getSource().sendFeedback(Mappings.literalText("Cannot find any train.").formatted(Formatting.RED), false);
            return 1;
        }

        if(trainData.isManual) {
            trainData.isCurrentlyManual = ((TrainAccessorMixin)trainData.train).getIsCurrentlyManual();
            if(trainData.isCurrentlyManual) {
                trainData.accelerationSign = ((TrainAccessorMixin) trainData.train).getManualNotch();
                trainData.ridingEntities = ((TrainAccessorMixin) trainData.train).getRidingEntities();
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

        List<Route> currentlyRunningRoute = data.routes.stream().filter(rt -> rt.id == trainData.routeId).toList();

        String currentRouteName = "N/A";
        if(!currentlyRunningRoute.isEmpty()) {
            currentRouteName = IGui.formatStationName(currentlyRunningRoute.get(0).name);
        }

        final int depotColor = sidingDepot.color;

        MutableText depotName = Mappings.literalText(IGui.formatStationName(sidingDepot.name)).styled(style -> style.withColor(depotColor));
        MutableText routeName = Mappings.literalText(currentRouteName).formatted(Formatting.GREEN);
        MutableText sidingName = Mappings.literalText(IGui.formatStationName(siding.name)).formatted(Formatting.GREEN);
        MutableText trainType = Mappings.literalText(IGui.formatStationName(trainData.train.trainId)).formatted(Formatting.GREEN);
        MutableText trainCars = Mappings.literalText(" (" + trainData.train.trainCars + "-cars)").formatted(Formatting.GREEN);
        MutableText pos = Mappings.literalText(String.format("%d, %d, %d", Math.round(trainData.positions[0].getX()), Math.round(trainData.positions[0].getY()), Math.round(trainData.positions[0].getZ()))).formatted(Formatting.GREEN);

        MutableText isManual = Mappings.literalText(trainData.isManual ? trainData.isCurrentlyManual ? "Manual (Currently Manual)" : "Manual (Current ATO)" : "ATO").formatted(Formatting.GREEN);

        MutableText trainNotch = Mappings.literalText(trainData.accelerationSign == -2 ? "B2" : trainData.accelerationSign == -1 ? "B1" : trainData.accelerationSign == 0 ? "N" : trainData.accelerationSign == 1 ? "P1" : "P2").formatted(Formatting.GREEN);
        MutableText PMLeft = Mappings.literalText((Math.round((((trainData.manualToAutomaticTime * 10) - trainData.manualCooldown)) / 20F)) + "s").formatted(Formatting.GREEN);

        StringBuilder ridingEntities = new StringBuilder();
        if(trainData.ridingEntities != null) {
            for (UUID uuid : trainData.ridingEntities) {
                ServerPlayerEntity ridingPlayer = context.getSource().getServer().getPlayerManager().getPlayer(uuid);
                if (ridingPlayer == null) continue;

                ridingEntities.append(ridingPlayer.getGameProfile().getName()).append("\n");
            }
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText(ridingEntities.toString()).formatted(Formatting.GREEN));
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/traininv " + siding.id);
        context.getSource().sendFeedback(Mappings.literalText("===== Nearest Train =====").formatted(Formatting.GREEN), false);
        context.getSource().sendFeedback(Mappings.literalText("Mode: ").formatted(Formatting.GOLD).append(isManual), false);
        context.getSource().sendFeedback(Mappings.literalText("Depots: ").formatted(Formatting.GOLD).append(depotName), false);
        context.getSource().sendFeedback(Mappings.literalText("Siding Number: ").formatted(Formatting.GOLD).append(sidingName), false);
        context.getSource().sendFeedback(Mappings.literalText("Running Route: ").formatted(Formatting.GOLD).append(routeName), false);
        context.getSource().sendFeedback(Mappings.literalText("Train Type: ").formatted(Formatting.GOLD).append(trainType).append(trainCars), false);
        context.getSource().sendFeedback(Mappings.literalText("Position: ").formatted(Formatting.GOLD).append(pos), false);

        if(trainData.ridingEntities != null && trainData.ridingEntities.size() > 0) {
            context.getSource().sendFeedback(Mappings.literalText("Riding players: (Hover Here)").formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(hoverEvent)), false);
        }

        if(!trainData.inventory.isEmpty()) {
            context.getSource().sendFeedback(Mappings.literalText("Train Inventory: (Click Here)").formatted(Formatting.GOLD).styled(style -> style.withClickEvent(clickEvent)), false);
        }

        if(trainData.isManual && trainData.isCurrentlyManual) {
            context.getSource().sendFeedback(Mappings.literalText("Train Notch: ").formatted(Formatting.GOLD).append(trainNotch), false);
            context.getSource().sendFeedback(Mappings.literalText("Switching to ATO in: ").formatted(Formatting.GOLD).append(PMLeft), false);
        }
        return 1;
    }
}
