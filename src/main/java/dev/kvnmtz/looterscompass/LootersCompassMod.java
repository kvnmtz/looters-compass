package dev.kvnmtz.looterscompass;

import dev.kvnmtz.looterscompass.config.CommonConfig;
import dev.kvnmtz.looterscompass.item.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LootersCompassMod.MOD_ID)
public final class LootersCompassMod {
    public static final String MOD_ID = "looters_compass";

    public LootersCompassMod(FMLJavaModLoadingContext context) {
        var modEventBus = context.getModEventBus();
        ModItems.register(modEventBus);

        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
