package com.lx.mtrtm.mixin;

import mtr.data.Siding;
import mtr.data.TrainServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;


@Mixin(value = Siding.class, remap = false)
public interface SidingAccessorMixin {
    @Accessor
    Set<TrainServer> getTrains();
}