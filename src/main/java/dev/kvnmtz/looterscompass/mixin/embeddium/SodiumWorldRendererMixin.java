package dev.kvnmtz.looterscompass.mixin.embeddium;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SodiumWorldRenderer.class)
public class SodiumWorldRendererMixin {

    @Redirect(
            method = "renderBlockEntity(Lcom/mojang/blaze3d/vertex/PoseStack;" +
                    "Lnet/minecraft/client/renderer/RenderBuffers;Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;" +
                    "FLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;" +
                    "DDDLnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;" +
                    "Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render" +
                            "(Lnet/minecraft/world/level/block/entity/BlockEntity;" +
                            "FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
            )
    )
    private static void redirectRender(
            BlockEntityRenderDispatcher instance,
            BlockEntity entity,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource consumer,
            @Local(index = 1) RenderBuffers bufferBuilders
    ) {
        var bufferToUse = consumer;

        if (CompassGlowManager.isGlowing(entity.getBlockPos())) {
            var outlineBufferSource = bufferBuilders.outlineBufferSource();
            outlineBufferSource.setColor(255, 255, 255, 255);
            bufferToUse = outlineBufferSource;
        }

        instance.render(entity, tickDelta, matrices, bufferToUse);
    }
}
