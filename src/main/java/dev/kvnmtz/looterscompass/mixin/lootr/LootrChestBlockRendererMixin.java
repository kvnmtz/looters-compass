package dev.kvnmtz.looterscompass.mixin.lootr;

import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import noobanidus.mods.lootr.block.entities.LootrChestBlockEntity;
import noobanidus.mods.lootr.client.block.LootrChestBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LootrChestBlockRenderer.class, remap = false)
public abstract class LootrChestBlockRendererMixin implements BlockEntityRenderer<LootrChestBlockEntity> {

    @Override
    public boolean shouldRenderOffScreen(LootrChestBlockEntity blockEntity) {
        return CompassGlowManager.isGlowing(blockEntity.getBlockPos());
    }
}