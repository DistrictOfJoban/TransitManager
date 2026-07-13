package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.TransitManager;
import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.mtr.core.Main;
import org.mtr.core.simulation.Simulator;
import org.mtr.mapping.holder.*;

public class TscCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tsc")
            .requires(ctx -> ctx.hasPermission(2))
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
                            context.getSource().sendSuccess(() -> Component.literal("TSC simulation for " + new Identifier(context.getSource().getLevel().dimension().location()) + " is now unfrozen.").withStyle(ChatFormatting.GREEN), false);
                        } else {
                            TransitManager.frozenTsc.add(simulator);
                            context.getSource().sendSuccess(() -> Component.literal("TSC is now freezed!").withStyle(ChatFormatting.GREEN), false);
                            context.getSource().sendSuccess(() -> Component.literal("Use /tsc freeze to unfreeze.").withStyle(ChatFormatting.GREEN), false);
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
