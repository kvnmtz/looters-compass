package dev.kvnmtz.looterscompass.mixin;

import com.google.gson.JsonElement;
import dev.kvnmtz.looterscompass.LootersCompassMod;
import dev.kvnmtz.looterscompass.config.CommonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    private static final ResourceLocation RECIPE_TO_REMOVE = LootersCompassMod.asResource("looters_compass");

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void removeCraftingRecipe(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        if (!CommonConfig.DISABLE_RECIPE.get()) return;

        var iterator = object.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!entry.getKey().equals(RECIPE_TO_REMOVE)) continue;

            iterator.remove();
            break;
        }
    }
}
