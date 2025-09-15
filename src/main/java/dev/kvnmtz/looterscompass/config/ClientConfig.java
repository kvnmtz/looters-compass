package dev.kvnmtz.looterscompass.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue SEARCH_FREQUENCY_TICKS;

    static {
        SEARCH_FREQUENCY_TICKS = BUILDER
                .comment("How often to search for chests (in ticks)")
                .comment("20 ticks = 1 second")
                .defineInRange("search_frequency", 20, 1, 200);

        SPEC = BUILDER.build();
    }
}