package com.lx.mtrtm.commands;

import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mtr.data.RailwayData;
import mtr.data.Station;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;

import java.util.List;

public class warpstn {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("warpstn")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .suggests((commandContext, suggestionsBuilder) -> {
                            RailwayData data = RailwayData.getInstance(commandContext.getSource().getWorld());
                                String target = suggestionsBuilder.getRemainingLowerCase();

                                List<String> toBeSuggested = Util.formulateMatchingString(target, data.stations.stream().map(e -> e.name).toList());
                                for(String stn : toBeSuggested) {
                                    suggestionsBuilder.suggest(stn);
                                }
                                return suggestionsBuilder.buildFuture();
                            }
                        )
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            Station stn = Util.findStation(name, context.getSource().getWorld());
                            if(stn == null) {
                                context.getSource().sendFeedback(Mappings.literalText("Cannot find station."), false);
                                return 1;
                            }

                            context.getSource().getPlayer().requestTeleportAndDismount(getMidPoint(stn.corner1.getLeft(), stn.corner2.getLeft()), context.getSource().getPlayer().getY(), getMidPoint(stn.corner1.getRight(), stn.corner2.getRight()));
                            context.getSource().sendFeedback(Mappings.literalText("Warped to " + String.join(" ", getStationName(stn.name))).formatted(Formatting.GREEN), false);
                            return 1;
                        }))
        );
    }

    public static double getMidPoint(int p1, int p2) {
        return (p1 + p2) / 2.0;
    }

    public static String[] getStationName(String stationName) {
        return stationName.split("\\|");
    }
}
