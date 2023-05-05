package com.lx.mtrtm.commands;

import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.TransitManager;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.mixin.RailwayDataPathGenerationModuleAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mtr.data.Depot;
import mtr.data.IGui;
import mtr.data.RailwayData;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;

public class mtrpath {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mtrpath")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                .then(CommandManager.literal("status")
                .executes(context -> {
                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getWorld());
                    if(railwayData != null) {
                        Map<Long, Thread> generatingPathThreads = ((RailwayDataPathGenerationModuleAccessorMixin)railwayData.railwayDataPathGenerationModule).getGeneratingPathThreads();
                        generatingPathThreads.keySet().removeIf(e -> !generatingPathThreads.get(e).isAlive());

                        context.getSource().sendFeedback(Mappings.literalText("===== Paths on " + generatingPathThreads.size() + " depot(s) are being refreshed =====").formatted(Formatting.GOLD), false);
                        for(Map.Entry<Long, Thread> entry : generatingPathThreads.entrySet()) {
                            Depot depot = railwayData.dataCache.depotIdMap.get(entry.getKey());
                            if(depot != null) {
                                context.getSource().sendFeedback(Mappings.literalText(IGui.formatStationName(depot.name).formatted(Formatting.GREEN)), false);
                            }
                        }
                    }
                    return 1;
                }))
                .then(CommandManager.literal("refresh")
                        .then(CommandManager.argument("depotName", StringArgumentType.greedyString())
                                .suggests((context, suggestionsBuilder) -> {
                                    RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
                                    if(data != null) {
                                        String target = suggestionsBuilder.getRemainingLowerCase();
                                        List<String> toBeAdded = Util.formulateMatchingString(target, data.depots.stream().map(e -> e.name).toList());
                                        for(String dp : toBeAdded) {
                                            suggestionsBuilder.suggest(dp);
                                        }
                                    }
                                    return suggestionsBuilder.buildFuture();
                                })
                                .executes(context -> {
                                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getWorld());
                                    String depotName = StringArgumentType.getString(context, "depotName");
                                    Depot depot = Util.findDepot(depotName, context.getSource().getWorld());
                                    if(depot != null && railwayData != null) {
                                        railwayData.railwayDataPathGenerationModule.generatePath(context.getSource().getServer(), depot.id);
                                        context.getSource().sendFeedback(Mappings.literalText("Refreshing " + String.join(" ", depot.name) + " (" + depot.routeIds.size() + " Routes instructions)").formatted(Formatting.GREEN), false);
                                    }
                                    return 1;
                                })
                        ))
                .then(CommandManager.literal("abort")
                        .then(CommandManager.argument("depotName", StringArgumentType.greedyString())
                                .suggests((context, suggestionsBuilder) -> {
                                    RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
                                    if(data != null) {
                                        String target = suggestionsBuilder.getRemainingLowerCase();
                                        List<String> toBeAdded = Util.formulateMatchingString(target, data.depots.stream().map(e -> e.name).toList());
                                        for(String dp : toBeAdded) {
                                            suggestionsBuilder.suggest(dp);
                                        }
                                    }
                                    return suggestionsBuilder.buildFuture();
                                })
                                .executes(context -> {
                                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getWorld());
                                    String depotName = StringArgumentType.getString(context, "depotName");
                                    Depot depot = Util.findDepot(depotName, context.getSource().getWorld());
                                    if(depot != null && railwayData != null) {
                                        long id = depot.id;
                                        TransitManager.depotPathToBeInterrupted.add(id);
                                        railwayData.railwayDataPathGenerationModule.generatePath(context.getSource().getServer(), id);
                                        context.getSource().sendFeedback(Mappings.literalText("Path generation interrupted.").formatted(Formatting.GREEN), false);
                                        PacketTrainDataGuiServer.generatePathS2C(context.getSource().getWorld(), id, 0);
                                    }
                                    return 1;
                                })
                        )
                )
            );
    }
}
