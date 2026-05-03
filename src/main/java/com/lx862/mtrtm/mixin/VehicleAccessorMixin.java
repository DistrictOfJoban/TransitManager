package com.lx862.mtrtm.mixin;

import org.mtr.core.data.Vehicle;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Vehicle.class, remap = false)
public interface VehicleAccessorMixin {
    @Invoker("getPosition")
    Vector getPositionAt(double value, DoubleArrayList objectArrayList);

    @Accessor("manualCooldown")
    long getManualCooldown();

    @Accessor("deviation")
    long getDeviation();
}
