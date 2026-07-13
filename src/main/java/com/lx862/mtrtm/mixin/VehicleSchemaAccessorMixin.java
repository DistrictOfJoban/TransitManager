package com.lx862.mtrtm.mixin;

import org.mtr.core.generated.data.VehicleSchema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VehicleSchema.class, remap = false)
public interface VehicleSchemaAccessorMixin {
    @Accessor("railProgress")
    double mtrtm$getRailProgress();

    @Accessor("nextStoppingIndexAto")
    long mtrtm$getNextStoppingIndexAto();

    @Accessor("speed")
    double mtrtm$getSpeed();

    @Accessor("elapsedDwellTime")
    long mtrtm$getElapsedDwellTime();

    @Accessor("nextStoppingIndexAto")
    void mtrtm$setNextStoppingIndexAto(long newStoppingIndexAto);

    @Accessor("railProgress")
    void mtrtm$setRailProgress(double newRailProgress);

    @Accessor("elapsedDwellTime")
    void mtrtm$setElapsedDwellTime(long newElapsedDwellTime);
}
