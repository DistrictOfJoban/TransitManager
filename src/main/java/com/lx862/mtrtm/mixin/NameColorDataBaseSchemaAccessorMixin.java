package com.lx862.mtrtm.mixin;

import org.mtr.core.data.Data;
import org.mtr.core.generated.data.NameColorDataBaseSchema;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NameColorDataBaseSchema.class)
public interface NameColorDataBaseSchemaAccessorMixin {
    @Accessor("data")
    Data mtrtm$getData();
}
