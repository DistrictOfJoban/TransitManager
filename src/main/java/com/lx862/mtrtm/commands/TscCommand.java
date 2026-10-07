package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.TransitManager;
import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.lx862.mtrtm.util.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.mtr.core.Main;
import org.mtr.core.simulation.Simulator;
import org.mtr.mapping.holder.*;

public class TscCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tsc")
            .requires(ctx -> ctx.hasPermission(2))
            .executes(context -> {
                Main tsc = InitAccessorMixin.mtrtm$getMain();
                Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());

                long timeDifference = System.currentTimeMillis() - simulator.getCurrentMillis();

                context.getSource().sendSuccess(() -> Component.literal("===== Transport Simulation Core =====").withColor(0xB42249), false);
                context.getSource().sendSuccess(() -> Component.literal("Dimension - " + context.getSource().getLevel().dimension().location().toString()).withStyle(ChatFormatting.GREEN), false);
                if(timeDifference <= 75) {
                    context.getSource().sendSuccess(() -> Component.literal("Simulation caught up.").withStyle(ChatFormatting.GREEN), false);
                } else {
                    context.getSource().sendSuccess(() -> Component.literal("Simulation is falling " + Util.getReadableTimeMs(timeDifference) + " behind!").withStyle(ChatFormatting.RED), false);
                }

                Util.sendKeyValueFeedback(context, Component.literal("Stations: "), Component.literal(String.valueOf(simulator.stations.size())));
                Util.sendKeyValueFeedback(context, Component.literal("Routes: "), Component.literal(String.valueOf(simulator.routes.size())));
                Util.sendKeyValueFeedback(context, Component.literal("Depots: "), Component.literal(String.valueOf(simulator.depots.size())));
                Util.sendKeyValueFeedback(context, Component.literal("Lifts: "), Component.literal(String.valueOf(simulator.lifts.size())));
                Util.sendKeyValueFeedback(context, Component.literal("Rails: "), Component.literal(String.valueOf(simulator.rails.size())));


                return 1;
            })
            .then(Commands.literal("ping")
                .executes(context -> {
                    Main tsc = InitAccessorMixin.mtrtm$getMain();
                    Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());

                    long timeSend = System.currentTimeMillis();
                    simulator.run(() -> {
                        long timeDifference = System.currentTimeMillis() - timeSend;

                        context.getSource().getServer().execute(() -> {
                            context.getSource().sendSuccess(() -> Component.literal("Took " + timeDifference + "ms!"), false);
                        });
                    });
                    return 1;
                })
            )
            .then(Commands.literal("freeze")
                .executes(context -> {
                    Main tsc = InitAccessorMixin.mtrtm$getMain();
                    Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());
                    if(TransitManager.frozenTsc.contains(simulator)) {
                        TransitManager.frozenTsc.remove(simulator);
                        context.getSource().sendSuccess(() -> Component.literal("TSC simulation for " + context.getSource().getLevel().dimension().location() + " is now unfrozen.").withStyle(ChatFormatting.GREEN), false);
                    } else {
                        context.getSource().sendSuccess(() -> Component.literal("WARNING: This would pause all MTR simulation in the current dimension!").withStyle(ChatFormatting.YELLOW), false);
                        context.getSource().sendSuccess(() -> Component.literal("Only use for debugging.").withStyle(ChatFormatting.YELLOW), false);
                        context.getSource().sendSuccess(() -> Component.literal("Run /tsc freeze ignoreDanger to confirm.").withStyle(ChatFormatting.YELLOW), false);
                    }
                    return 1;
                })
                .then(Commands.literal("ignoreDanger")
                    .executes(context -> {
                        Main tsc = InitAccessorMixin.mtrtm$getMain();
                        Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());
                        if(TransitManager.frozenTsc.contains(simulator)) {
                            TransitManager.frozenTsc.remove(simulator);
                            context.getSource().sendSuccess(() -> Component.literal("TSC simulation for " + context.getSource().getLevel().dimension().location() + " is now unfrozen.").withStyle(ChatFormatting.GREEN), false);
                        } else {
                            TransitManager.frozenTsc.add(simulator);
                            context.getSource().sendSuccess(() -> Component.literal("TSC is now freezed!").withStyle(ChatFormatting.GREEN), false);
                            context.getSource().sendSuccess(() -> Component.literal("Use /tsc freeze to unfreeze.").withStyle(ChatFormatting.GREEN), false);
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("map")
                    .then(Commands.literal("hide")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> toggleTsmVisibility(ctx, EntityArgument.getPlayer(ctx, "player")))
                            )
                            .executes(ctx -> toggleTsmVisibility(ctx, ctx.getSource().getPlayerOrException()))
                    )
            )
        );
    }

    private static int toggleTsmVisibility(CommandContext<CommandSourceStack> ctx, Player player) {
        String uuid = player.getStringUUID();
        if(TransitManager.playersHiddenFromTsm.contains(uuid)) {
            TransitManager.playersHiddenFromTsm.remove(uuid);
            ctx.getSource().sendSuccess(() -> Component.literal("Player now visible in Transport System Map!").withStyle(ChatFormatting.GREEN), true);
        } else {
            TransitManager.playersHiddenFromTsm.add(uuid);
            ctx.getSource().sendSuccess(() -> Component.literal("Player now hidden from Transport System Map!").withStyle(ChatFormatting.GREEN), true);
        }
        return 1;
    }
}
