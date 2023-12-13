package com.lx862.mtrtm.mixin;

import mtr.data.Train;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
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
    SimpleContainer getInventory();

    @Invoker("getRoutePosition")
    Vec3 getTheRoutePosition(int car, int trainSpacing);

    @Accessor
    List<Double> getDistances();

    @Accessor
    int getNextStoppingIndex();

    @Accessor("elapsedDwellTicks")
    void setElapsedDwellTicks(float dwellTicks);

    @Accessor("railProgress")
    void setRailProgress(double railProgress);

    @Mutable
    @Accessor("isCurrentlyManual")
    void setCurrentlyManual(boolean isCurrentlyManual);

    @Accessor("nextStoppingIndex")
    void setNextStoppingIndex(int nextStoppingIndex);
}
