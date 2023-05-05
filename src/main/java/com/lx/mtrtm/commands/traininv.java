package com.lx.mtrtm.commands;

import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.mixin.SidingAccessorMixin;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mtr.data.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Set;

public class traininv {

    /* This command is only meant to be executed by clicking a hoverable text provided by /whattrain, it is normal that regular player
    * does not know what sidingID is. */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("traininv")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                        .then(CommandManager.argument("sidingID", LongArgumentType.longArg())
                                .executes(context -> openTrainInventory(context, context.getSource().getPlayer().getPos(), LongArgumentType.getLong(context, "sidingID")))
                        )
        );
    }

    private static int openTrainInventory(CommandContext<ServerCommandSource> context, Vec3d playerPos, long sidingID) throws CommandSyntaxException {
        RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
        List<Siding> sidings = data.sidings.stream().filter(siding1 -> siding1.id == sidingID).toList();
        if(sidings.isEmpty()) {
            return 1;
        }

        Set<TrainServer> trainList = ((SidingAccessorMixin) sidings.get(0)).getTrains();

        for(TrainServer trainServer : trainList) {
            /* The inventory is fixed to 54 slots probably for display reasons */
            SimpleInventory inventory = new SimpleInventory(54);
            SimpleInventory trainInventory = ((TrainAccessorMixin)trainServer).getInventory();
            PlayerInventory playerInventory = context.getSource().getPlayer().getInventory();
            MutableText unusedSlotName = Mappings.literalText("Unusable Slots").formatted(Formatting.ITALIC).formatted(Formatting.GRAY);

            /* Put the item in the new inventory with 54 slots */
            for(int i = 0; i < 54; i++) {
                /* A train can only carry the amount of cars it has. (8-cars = 8 stacks) */
                /* If we're currently looping over the train inventory's size limit, we replace them with gray glass pane, suggesting this slot can not be used*/
                if(i >= trainInventory.size()) {
                    inventory.setStack(i, new ItemStack(Blocks.GRAY_STAINED_GLASS_PANE.asItem()).setCustomName(unusedSlotName));
                } else {
                    inventory.setStack(i, trainInventory.getStack(i));
                }
            }

            NamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((syncId, inv, player) -> {
                return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, inventory);
            }, Mappings.literalText(trainServer.trainId + " (Read-Only)"));

            context.getSource().getPlayer().openHandledScreen(screen);
        }
        return 1;
    }
}
