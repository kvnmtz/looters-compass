package dev.kvnmtz.looterscompass.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ADD_TO_100_LOOT_ADVANCEMENT_REWARDS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_RECIPE;

    static {
        ADD_TO_100_LOOT_ADVANCEMENT_REWARDS = BUILDER
                .comment("Whether the Looter's Compass should also be rewarded to the player when the \"Centennial\" advancement is made")
                .comment("(This is Lootr's advancement for opening 100 loot containers)")
                .define("add_to_centennial_advancement_rewards", false);

        DISABLE_RECIPE = BUILDER
                .comment("Whether the crafting recipe for the Looter's Compass should be disabled")
                .define("disable_recipe", false);

        SPEC = BUILDER.build();
    }
}