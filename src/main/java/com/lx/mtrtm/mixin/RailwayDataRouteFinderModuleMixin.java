package com.lx.mtrtm.mixin;

import com.lx.mtrtm.config.Config;
import mtr.MTR;
import mtr.data.RailwayDataRouteFinderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RailwayDataRouteFinderModule.class, remap = false)
public class RailwayDataRouteFinderModuleMixin {

    /* Rate limit MTR Journey Planner */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        if(Config.mtrJourneyPlannerTickTime == -1 || (Config.mtrJourneyPlannerTickTime != 0 && !MTR.isGameTickInterval(Config.mtrJourneyPlannerTickTime))) {
            ci.cancel();
        } else {
            System.out.println("Tick");
        }
    }
}