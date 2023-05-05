package com.lx.mtrtm.mixin;

import mtr.data.Train;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;
import java.util.UUID;


@Mixin(value = Train.class, remap = false)
public interface TrainAccessorMixin {
    @Accessor
    boolean getReversed();

    @Accessor
    boolean getIsManualAllowed();

    @Accessor
    boolean getIsCurrentlyManual();

    @Accessor
    int getManualNotch();

    @Accessor
    int getManualToAutomaticTime();

    @Accessor
    Set<UUID> getRidingEntities();

    @Accessor
    SimpleInventory getInventory();

    @Invoker("getRoutePosition")
    Vec3d getTheRoutePosition(int car, int trainSpacing);

    @Mutable
    @Accessor("isManualAllowed")
    void setManualAllowed(boolean isManual);

    @Mutable
    @Accessor("isCurrentlyManual")
    void setCurrentlyManual(boolean isCurrentlyManual);
}
