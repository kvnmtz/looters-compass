package dev.kvnmtz.looterscompass.mixin.lootr;

import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import noobanidus.mods.lootr.block.entities.LootrShulkerBlockEntity;
import noobanidus.mods.lootr.client.block.LootrShulkerBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LootrShulkerBlockRenderer.class, remap = false)
public abstract class LootrShulkerBlockRendererMixin implements BlockEntityRenderer<LootrShulkerBlockEntity> {

    @Override
    public boolean shouldRenderOffScreen(LootrShulkerBlockEntity blockEntity) {
        return CompassGlowManager.isGlowing(blockEntity.getBlockPos());
    }
}