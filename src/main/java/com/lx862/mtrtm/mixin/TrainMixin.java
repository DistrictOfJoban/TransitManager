package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.TransitManager;
import com.lx862.mtrtm.data.TrainState;
import mtr.data.Depot;
import mtr.data.Train;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Train.class, remap = false)
public class TrainMixin {
    @Shadow protected float speed;

    @Shadow protected float elapsedDwellTicks;

    /* Disable train collision */
    @Inject(method = "simulateTrain", at = @At("TAIL"), cancellable = true)
    public void isRailBlocked(Level world, float ticksElapsed, Depot depot, CallbackInfo ci) {
        long trainId = ((Train)((Object)this)).id;
        if(TransitManager.getTrainState(trainId, TrainState.HALT_SPEED)) {
            this.speed = 0;
        }
        if(TransitManager.getTrainState(trainId, TrainState.HALT_DWELL)) {
            this.elapsedDwellTicks -= ticksElapsed;
        }
    }
}