package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.TransitManager;
import org.mtr.core.data.Client;
import org.mtr.core.servlet.SystemMapServlet;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SystemMapServlet.class)
public class SystemMapServletMixin {
    @Inject(method = "lambda$getClients$6", at = @At("HEAD"), cancellable = true)
    private static void mtrtm$hideTsmPlayer(Object2ObjectAVLTreeMap clients, Simulator simulator, Client client, CallbackInfo ci) {
        String uuid = client.uuid.toString();
        if(TransitManager.playersHiddenFromTsm.contains(uuid)) ci.cancel();
    }
}
