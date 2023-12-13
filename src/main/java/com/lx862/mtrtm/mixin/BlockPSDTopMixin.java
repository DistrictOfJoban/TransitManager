package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.config.Config;
import mtr.block.BlockPSDTop;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPSDTop.class)
public class BlockPSDTopMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if(!player.hasPermissions(Config.shearPSDOpLevel) && player.isHolding(Items.SHEARS)) {
            cir.setReturnValue(InteractionResult.FAIL);
            player.displayClientMessage(Mappings.literalText("You don't have permission to shear the Platform Screen Doors."), true);
        }
    }
}
