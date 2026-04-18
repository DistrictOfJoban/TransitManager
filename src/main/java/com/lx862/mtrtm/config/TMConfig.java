package com.lx862.mtrtm.config;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.DisplayName;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import net.fabricmc.loader.api.FabricLoader;

@DisplayName("TransitManager Config")
public class TMConfig extends WrappedConfig {
    public static TMConfig INSTANCE = createToml(FabricLoader.getInstance().getConfigDir(), "transitmanager", "server", TMConfig.class);

    @Comment("Operator level required for the PSD Top to be sheared.")
    @Comment("Useful for exhibition-alike server to prevent visitors griefing with shears.")
    @IntegerRange(min = 0, max = 4)
    public int shearPSDOpLevel = 0;

    public static void init() {
        // Static init
    }
}
