package com.lx.mtrtm.mixin;

import mtr.data.RailwayDataPathGenerationModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = RailwayDataPathGenerationModule.class, remap = false)
public interface RailwayDataPathGenerationModuleAccessorMixin {
    @Accessor
    Map<Long, Thread> getGeneratingPathThreads();
}
