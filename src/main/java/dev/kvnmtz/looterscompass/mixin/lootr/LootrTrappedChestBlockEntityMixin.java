package dev.kvnmtz.looterscompass.mixin.lootr;

import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import noobanidus.mods.lootr.block.entities.LootrTrappedChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LootrTrappedChestBlockEntity.class, remap = false)
public abstract class LootrTrappedChestBlockEntityMixin extends BlockEntity {

    public LootrTrappedChestBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public boolean hasCustomOutlineRendering(Player player) {
        return super.hasCustomOutlineRendering(player) || CompassGlowManager.isGlowing(getBlockPos());
    }
}
