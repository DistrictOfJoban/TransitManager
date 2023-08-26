package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mtr.data.RailwayData;
import mtr.data.Station;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import java.util.List;

public class warpstn {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warpstn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((commandContext, suggestionsBuilder) -> {
                            RailwayData data = RailwayData.getInstance(commandContext.getSource().getLevel());
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
                            Station stn = Util.findStation(name, context.getSource().getLevel());
                            if(stn == null) {
                                context.getSource().sendSuccess(Mappings.literalText("Cannot find station."), false);
                                return 1;
                            }

                            context.getSource().getPlayer().dismountTo(getMidPoint(stn.corner1.getA(), stn.corner2.getA()), context.getSource().getPlayer().getY(), getMidPoint(stn.corner1.getB(), stn.corner2.getB()));
                            context.getSource().sendSuccess(Mappings.literalText("Warped to " + String.join(" ", getStationName(stn.name))).withStyle(ChatFormatting.GREEN), false);
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
