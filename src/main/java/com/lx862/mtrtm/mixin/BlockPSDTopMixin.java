package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.config.TMConfig;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.block.BlockPSDTop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockPSDTop.class, remap = false)
public class BlockPSDTopMixin {

    @Inject(method = "onUse2", at = @At("HEAD"), cancellable = true)
    public void use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if(!player.hasPermissionLevel(TMConfig.INSTANCE.shearPSDOpLevel) && player.isHolding(org.mtr.mapping.holder.Items.getShearsMapped())) {
            cir.setReturnValue(ActionResult.FAIL);
            player.sendMessage(Text.cast(TextHelper.literal("You don't have permission to shear the Platform Screen Doors.")), true);
        }
    }
}
