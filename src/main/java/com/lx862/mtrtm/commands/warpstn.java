package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.MtrUtil;
import com.lx862.mtrtm.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mtr.data.RailwayData;
import mtr.data.Station;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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
                            Level world = context.getSource().getLevel();
                            String name = StringArgumentType.getString(context, "name");
                            Station stn = MtrUtil.findStation(name, context.getSource().getLevel());
                            if(stn == null) {
                                context.getSource().sendSuccess(Mappings.literalText("Cannot find station."), false);
                                return 1;
                            }

                            double midpointX = getMidPoint(stn.corner1.getA(), stn.corner2.getA());
                            double midpointZ = getMidPoint(stn.corner1.getB(), stn.corner2.getB());
                            double playerY = context.getSource().getPlayer().getY();
                            BlockPos targetPos = new BlockPos(midpointX, playerY, midpointZ);
                            BlockPos finalPos = Util.getNonOccupiedPos(world, targetPos);

                            context.getSource().getPlayer().dismountTo(finalPos.getX(), finalPos.getY(), finalPos.getZ());
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
