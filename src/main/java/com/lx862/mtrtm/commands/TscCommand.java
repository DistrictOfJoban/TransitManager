package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
        );
    }
}
