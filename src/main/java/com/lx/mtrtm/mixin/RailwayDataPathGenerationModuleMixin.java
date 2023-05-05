package com.lx.mtrtm.mixin;

import com.lx.mtrtm.TransitManager;
import com.lx.mtrtm.config.Config;
import mtr.data.RailwayDataPathGenerationModule;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RailwayDataPathGenerationModule.class, remap = false)
public class RailwayDataPathGenerationModuleMixin {
    @Shadow @Final private Map<Long, Thread> generatingPathThreads;

    @Inject(method = "generatePath", at = @At("HEAD"), cancellable = true)
    public void generatePath(MinecraftServer minecraftServer, long depotId, CallbackInfo ci) {
        /* Abort path generation if requested */
        if(TransitManager.depotPathToBeInterrupted.contains(depotId)) {
            TransitManager.depotPathToBeInterrupted.remove(depotId);
            if(Config.forceKillMTRPathThread) {
                generatingPathThreads.get(depotId).stop();
            } else {
                generatingPathThreads.get(depotId).interrupt();
            }
            generatingPathThreads.remove(depotId);
            ci.cancel();
            return;
        }

        if(Config.forceKillMTRPathThread) {
            /* Force kill thread before regenerating path */
            if(generatingPathThreads.containsKey(depotId)) {
                generatingPathThreads.get(depotId).stop();
                generatingPathThreads.remove(depotId);
            }
        }
    }
}
