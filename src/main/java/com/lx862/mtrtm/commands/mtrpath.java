package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.MtrUtil;
import com.lx862.mtrtm.TransitManager;
import com.lx862.mtrtm.Util;
import com.lx862.mtrtm.mixin.RailwayDataPathGenerationModuleAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mtr.data.Depot;
import mtr.data.IGui;
import mtr.data.RailwayData;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Map;

public class mtrpath {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mtrpath")
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("status")
                .executes(context -> {
                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
                    if(railwayData != null) {
                        Map<Long, Thread> generatingPathThreads = ((RailwayDataPathGenerationModuleAccessorMixin)railwayData.railwayDataPathGenerationModule).getGeneratingPathThreads();
                        generatingPathThreads.keySet().removeIf(e -> !generatingPathThreads.get(e).isAlive());

                        context.getSource().sendSuccess(Mappings.literalText("===== Paths on " + generatingPathThreads.size() + " depot(s) are being refreshed =====").withStyle(ChatFormatting.GOLD), false);
                        for(Map.Entry<Long, Thread> entry : generatingPathThreads.entrySet()) {
                            Depot depot = railwayData.dataCache.depotIdMap.get(entry.getKey());
                            if(depot != null) {
                                MutableComponent text = Mappings.literalText("- " + IGui.formatStationName(depot.name)).withStyle(ChatFormatting.GREEN);
                                MutableComponent time = Mappings.literalText(" (" + Util.getReadableTimeMs(System.currentTimeMillis() - TransitManager.pathGenerationTimer.get(depot.id)) + " elapsed)").withStyle(ChatFormatting.YELLOW);
                                context.getSource().sendSuccess(text.append(time), false);
                            }
                        }
                    }
                    return 1;
                }))
                .then(Commands.literal("refresh")
                        .then(Commands.argument("depotName", StringArgumentType.greedyString())
                                .suggests((context, suggestionsBuilder) -> {
                                    RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
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
                                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
                                    String depotName = StringArgumentType.getString(context, "depotName");
                                    Depot depot = MtrUtil.findDepot(depotName, context.getSource().getLevel());
                                    if(depot != null && railwayData != null) {
                                        railwayData.railwayDataPathGenerationModule.generatePath(context.getSource().getServer(), depot.id);
                                        context.getSource().sendSuccess(Mappings.literalText("Refreshing " + String.join(" ", depot.name) + " (" + depot.routeIds.size() + " Routes instructions)").withStyle(ChatFormatting.GREEN), false);
                                    }
                                    return 1;
                                })
                        ))
                .then(Commands.literal("abort")
                        .then(Commands.argument("depotName", StringArgumentType.greedyString())
                                .suggests((context, suggestionsBuilder) -> {
                                    RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
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
                                    RailwayData railwayData = RailwayData.getInstance(context.getSource().getLevel());
                                    String depotName = StringArgumentType.getString(context, "depotName");
                                    Depot depot = MtrUtil.findDepot(depotName, context.getSource().getLevel());
                                    if(depot != null && railwayData != null) {
                                        long id = depot.id;
                                        TransitManager.stopPathGenDepotList.add(id);
                                        railwayData.railwayDataPathGenerationModule.generatePath(context.getSource().getServer(), id);
                                        context.getSource().sendSuccess(Mappings.literalText("Path generation has been forcefully stopped.").withStyle(ChatFormatting.GREEN), false);
                                        PacketTrainDataGuiServer.generatePathS2C(context.getSource().getLevel(), id, 0);
                                    }
                                    return 1;
                                })
                        )
                )
            );
    }
}
