package com.lx.mtrtm.commands;

import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import com.lx.mtrtm.mixin.SidingAccessorMixin;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mtr.data.RailwayData;
import mtr.data.Siding;
import mtr.data.TrainServer;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Set;
import java.util.UUID;

public class ejectFromTrain {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ejectFromTrain")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ejectFromTrain::execute)
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");

        RailwayData data = RailwayData.getInstance(context.getSource().getWorld());

        boolean trainFoundAndRemoved = false;
        for(Siding siding : data.sidings) {
            /* Loop through each train in each siding */
            for(TrainServer train : ((SidingAccessorMixin)siding).getTrains()) {
                Set<UUID> ridingEntities = ((TrainAccessorMixin)train).getRidingEntities();
                if(ridingEntities.contains(player.getUuid())) {
                    ridingEntities.remove(player.getUuid());
                    Util.syncTrainToPlayers(train, context.getSource().getWorld().getPlayers());
                    trainFoundAndRemoved = true;
                    break;
                }
            }
        }

        if(!trainFoundAndRemoved) {
            context.getSource().sendFeedback(Mappings.literalText("Player is not riding any train.").formatted(Formatting.RED), false);
            return 0;
        } else {
            context.getSource().sendFeedback(Mappings.literalText("Dismounted player.").formatted(Formatting.GREEN), false);
            return 1;
        }
    }
}
