package com.lx862.mtrtm.mixin;

import org.mtr.core.data.PathData;
import org.mtr.core.data.Siding;
import org.mtr.core.data.Vehicle;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(value = Siding.class, remap = false)
public interface SidingAccessorMixin {
    @Accessor("vehicles")
    ObjectArraySet<Vehicle> mtrtm$getVehicles();

    @Accessor("departures")
    LongArrayList mtrtm$getDepartures();

    @Accessor("pathSidingToMainRoute")
    ObjectArrayList<PathData> mtrtm$getPathSidingToMainRoute();

    @Accessor("pathMainRoute")
    ObjectArrayList<PathData> mtrtm$getPathMainRoute();

    @Accessor("pathMainRouteToSiding")
    ObjectArrayList<PathData> mtrtm$getPathMainRouteToSiding();
}