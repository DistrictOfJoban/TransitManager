package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.TransitManager;
import org.mtr.core.simulation.Simulator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Simulator.class)
public class SimulatorMixin {
    @Inject(method = "tickUntilCaughtUp", at = @At("HEAD"), cancellable = true)
    public void mtrtm$freezeTsc(CallbackInfoReturnable<Integer> cir) {
        if(TransitManager.frozenTsc.contains((Simulator)(Object)this)) cir.setReturnValue(0);
    }
}
