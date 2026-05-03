package com.lx862.mtrtm.mixin;

import org.mtr.core.generated.data.VehicleSchema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VehicleSchema.class, remap = false)
public interface VehicleSchemaAccessorMixin {
    @Accessor("railProgress")
    double getRailProgress();

    @Accessor("speed")
    double getSpeed();

    @Accessor("elapsedDwellTime")
    long getElapsedDwellTime();
}
