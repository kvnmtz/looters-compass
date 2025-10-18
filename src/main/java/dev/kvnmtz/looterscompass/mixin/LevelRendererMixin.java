package dev.kvnmtz.looterscompass.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    private RenderBuffers renderBuffers;

    @Redirect(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity" +
                    "/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;" +
                    "FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")
    )
    private void redirectRender(
            BlockEntityRenderDispatcher dispatcher,
            BlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource) {

        var bufferToUse = bufferSource;

        if (CompassGlowManager.isGlowing(blockEntity.getBlockPos())) {
            var outlineBufferSource = this.renderBuffers.outlineBufferSource();
            outlineBufferSource.setColor(255, 255, 255, 255);
            bufferToUse = outlineBufferSource;
        }

        dispatcher.render(blockEntity, partialTick, poseStack, bufferToUse);
    }
}