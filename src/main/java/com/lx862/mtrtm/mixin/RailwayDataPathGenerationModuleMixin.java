package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.TransitManager;
import com.lx862.mtrtm.config.Config;
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
            Thread thread = generatingPathThreads.get(depotId);
            if(thread != null) {
                if(Config.forceKillMTRPathThread) {
                    thread.stop();
                } else {
                    thread.interrupt();
                }
            }

            TransitManager.depotPathToBeInterrupted.remove(depotId);
            generatingPathThreads.remove(depotId);
            ci.cancel();
            return;
        }

        if(Config.forceKillMTRPathThread) {
            /* Force kill thread before regenerating path */
            if(generatingPathThreads.containsKey(depotId)) {
                Thread thread = generatingPathThreads.get(depotId);
                if(thread != null) {
                    thread.stop();
                }
                generatingPathThreads.remove(depotId);
            }
        }
    }
}
