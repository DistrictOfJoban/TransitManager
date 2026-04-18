package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.util.MtrUtil;
import com.lx862.mtrtm.util.Util;
import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.mtr.core.Main;
import org.mtr.core.data.*;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.holder.TextColor;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.mapper.TextHelper;

import java.util.function.Function;

public class WarpCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerWarp(dispatcher, "warpsta", data -> data.stations);
        registerWarp(dispatcher, "warpdepot", data -> data.depots);
    }

    private static void registerWarp(CommandDispatcher<CommandSourceStack> dispatcher, String commandName, Function<Data, ObjectArraySet<? extends AreaBase<?, ?>>> getData) {
        dispatcher.register(Commands.literal(commandName)
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((context, suggestionsBuilder) -> {
                                    Main tsc = InitAccessorMixin.getMain();
                                    Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).getSimulators(), context.getSource().getLevel());
                                    String target = suggestionsBuilder.getRemainingLowerCase();

                                    Util.formulateMatchingString(target, getData.apply(simulator).stream().map(NameColorDataBase::getName).toList())
                                            .forEach(suggestionsBuilder::suggest);

                                    return suggestionsBuilder.buildFuture();
                                }
                        )
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Level world = context.getSource().getLevel();
                            String name = StringArgumentType.getString(context, "name");
                            Main tsc = InitAccessorMixin.getMain();
                            Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).getSimulators(), context.getSource().getLevel());

                            AreaBase<?, ?> area = MtrUtil.findArea(name, getData.apply(simulator)).stream().findAny().orElse(null);

                            if(area == null) {
                                context.getSource().sendFailure(TextHelper.literal("Cannot find area \"" + name + "\"").data);
                                return 1;
                            }

                            double midpointX = area.getCenter().getX();
                            double midpointZ = area.getCenter().getZ();
                            double playerY = player.getY();
                            BlockPos targetPos = new BlockPos((int)midpointX, (int)playerY, (int)midpointZ);
                            BlockPos finalPos = MtrUtil.getNonOccupiedPos(world, targetPos, area);

                            player.removeVehicle();
                            player.teleportTo(finalPos.getX(), finalPos.getY(), finalPos.getZ());

                            MutableText text = TextHelper.literal("Warped to ");
                            TextHelper.setStyle(text, Style.getEmptyMapped().withColor(TextFormatting.GREEN));

                            MutableText areaNameText = TextHelper.literal(String.join(" ", getStationName(area.getName())));
                            TextHelper.setStyle(areaNameText, Style.getEmptyMapped().withColor(TextColor.fromRgb(area.getColor())));

                            TextHelper.append(text, areaNameText);

                            context.getSource().sendSuccess(() -> text.data, false);
                            return 1;
                        })
                )
        );
    }

    public static String[] getStationName(String stationName) {
        return stationName.split("\\|");
    }
}
