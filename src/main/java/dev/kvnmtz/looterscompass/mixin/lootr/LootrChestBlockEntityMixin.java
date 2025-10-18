package dev.kvnmtz.looterscompass.mixin.lootr;

import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import noobanidus.mods.lootr.block.entities.LootrChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LootrChestBlockEntity.class, remap = false)
public abstract class LootrChestBlockEntityMixin extends BlockEntity {

    public LootrChestBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public boolean hasCustomOutlineRendering(Player player) {
        return super.hasCustomOutlineRendering(player) || CompassGlowManager.isGlowing(getBlockPos());
    }
}
