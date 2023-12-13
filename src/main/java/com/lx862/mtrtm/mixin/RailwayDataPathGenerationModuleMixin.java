package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.TransitManager;
import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
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
        if(TransitManager.stopPathGenDepotList.contains(depotId)) {
            Thread thread = generatingPathThreads.get(depotId);
            if(thread != null && thread.isAlive()) {
                try {
                    thread.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                    TransitManager.LOGGER.warn("[TransitManager] Failed to abort path generation thread!");
                }
            }

            TransitManager.stopPathGenDepotList.remove(depotId);
            generatingPathThreads.remove(depotId);
            ci.cancel();
        } else {
            TransitManager.pathGenerationTimer.put(depotId, System.currentTimeMillis());
        }
    }
}
