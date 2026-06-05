package com.lx862.mtrtm.mixin;

import org.mtr.core.data.VehicleExtraData;
import org.mtr.core.data.VehicleRidingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(VehicleExtraData.class)
public interface VehicleExtraDataAccessorMixin {
    @Invoker("removeRidingEntitiesIf")
    void mtrtm$removeRidingEntitiesIf(Predicate<VehicleRidingEntity> predicate);
}
