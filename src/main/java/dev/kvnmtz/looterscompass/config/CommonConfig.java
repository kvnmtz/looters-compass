package dev.kvnmtz.looterscompass.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue SEARCH_RADIUS_XZ;
    public static final ForgeConfigSpec.IntValue SEARCH_RADIUS_Y;
    public static final ForgeConfigSpec.IntValue SEARCH_FREQUENCY_TICKS;

    static {
        SEARCH_RADIUS_XZ = BUILDER
                .comment("Horizontal search radius for finding chests (in blocks)")
                .defineInRange("search_radius_horizontal", 64, 1, 256);

        SEARCH_RADIUS_Y = BUILDER
                .comment("Vertical search radius for finding chests (in blocks)")
                .defineInRange("search_radius_vertical", 16, 1, 64);

        SEARCH_FREQUENCY_TICKS = BUILDER
                .comment("How often to search for chests (in ticks)")
                .comment("20 ticks = 1 second")
                .defineInRange("search_frequency", 20, 1, 200);

        SPEC = BUILDER.build();
    }
}