package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.mixin.SidingAccessorMixin;
import com.lx862.mtrtm.mixin.TrainAccessorMixin;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mtr.data.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Set;

public class traininv {

    /* This command is only meant to be executed by clicking a hoverable text provided by /whattrain, it is normal that regular player
    * does not know what sidingID is. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("traininv")
                .requires(ctx -> ctx.hasPermission(2))
                        .then(Commands.argument("sidingID", LongArgumentType.longArg())
                                .executes(context -> openTrainInventory(context, context.getSource().getPlayerOrException().position(), LongArgumentType.getLong(context, "sidingID")))
                        )
        );
    }

    private static int openTrainInventory(CommandContext<CommandSourceStack> context, Vec3 playerPos, long sidingID) throws CommandSyntaxException {
        RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
        List<Siding> sidings = data.sidings.stream().filter(siding1 -> siding1.id == sidingID).toList();
        if(sidings.isEmpty()) {
            return 1;
        }

        Set<TrainServer> trainList = ((SidingAccessorMixin) sidings.get(0)).getTrains();

        for(TrainServer trainServer : trainList) {
            /* The inventory is fixed to 54 slots probably for display reasons */
            SimpleContainer inventory = new SimpleContainer(54);
            SimpleContainer trainInventory = ((TrainAccessorMixin)trainServer).getInventory();
            Inventory playerInventory = context.getSource().getPlayerOrException().getInventory();
            MutableComponent unusedSlotName = Mappings.literalText("Unusable Slots").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);

            /* Put the item in the new inventory with 54 slots */
            for(int i = 0; i < 54; i++) {
                /* A train can only carry the amount of cars it has. (8-cars = 8 stacks) */
                /* If we're currently looping over the train inventory's size limit, we replace them with gray glass pane, suggesting this slot can not be used*/
                if(i >= trainInventory.getContainerSize()) {
                    inventory.setItem(i, new ItemStack(Blocks.GRAY_STAINED_GLASS_PANE.asItem()).setHoverName(unusedSlotName));
                } else {
                    inventory.setItem(i, trainInventory.getItem(i));
                }
            }

            MenuProvider screen = new SimpleMenuProvider((syncId, inv, player) -> {
                return ChestMenu.sixRows(syncId, playerInventory, inventory);
            }, Mappings.literalText(trainServer.trainId + " (Read-Only)"));

            context.getSource().getPlayerOrException().openMenu(screen);
        }
        return 1;
    }
}
