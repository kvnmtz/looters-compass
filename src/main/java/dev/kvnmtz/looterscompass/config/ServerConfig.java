package dev.kvnmtz.looterscompass.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue SEARCH_RADIUS_XZ;
    public static final ForgeConfigSpec.IntValue SEARCH_RADIUS_Y;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LOOT_CONTAINER_GLOW;
    public static final ForgeConfigSpec.IntValue GLOW_COOLDOWN;
    public static final ForgeConfigSpec.IntValue GLOW_DURATION;

    static {
        SEARCH_RADIUS_XZ = BUILDER
                .comment("Horizontal search radius for finding loot containers (in blocks)")
                .defineInRange("search_radius_horizontal", 32, 1, 256);

        SEARCH_RADIUS_Y = BUILDER
                .comment("Vertical search radius for finding loot containers (in blocks)")
                .defineInRange("search_radius_vertical", 16, 1, 64);

        ENABLE_LOOT_CONTAINER_GLOW = BUILDER
                .comment("Enable the ability to make loot containers glow when right-clicking with the compass")
                .define("enable_loot_container_glow", true);

        GLOW_COOLDOWN = BUILDER
                .comment("Cooldown for the glow feature in ticks (20 ticks = 1 second)")
                .defineInRange("glow_cooldown", 300, 0, 1200);

        GLOW_DURATION = BUILDER
                .comment("Duration for how long loot containers should glow in seconds")
                .defineInRange("glow_duration", 5, 1, 60);

        SPEC = BUILDER.build();
    }
}