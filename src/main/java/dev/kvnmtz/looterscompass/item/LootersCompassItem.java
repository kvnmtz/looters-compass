package dev.kvnmtz.looterscompass.item;

import dev.kvnmtz.looterscompass.config.CommonConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import noobanidus.mods.lootr.api.blockentity.ILootBlockEntity;
import net.minecraft.world.phys.AABB;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LootersCompassItem extends Item {
    private static final String FOUND_POS_TAG = "FoundPos";

    public LootersCompassItem() {
        super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON));

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LootersCompassItemClient.registerItemProperties(this));
    }

    @OnlyIn(Dist.CLIENT)
    private static class LootersCompassItemClient {
        public static void registerItemProperties(LootersCompassItem item) {
            ItemProperties.register(item, ResourceLocation.parse("angle"),
                    new CompassItemPropertyFunction((level, stack, entity) -> {
                        if (!(stack.getItem() instanceof LootersCompassItem compass)) return null;

                        var foundPos = compass.getFoundPosition(stack);
                        if (foundPos == null) return null;

                        return GlobalPos.of(level.dimension(), foundPos);
                    }));

            ItemProperties.register(item, ResourceLocation.parse("spinning"),
                    (stack, level, entity, seed) -> {
                        if (!(stack.getItem() instanceof LootersCompassItem compass)) return 0.0f;
                        var foundPos = compass.getFoundPosition(stack);
                        return foundPos == null ? 1.0f : 0.0f;
                    });
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.looters_compass.looters_compass.desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        if (player.tickCount % CommonConfig.SEARCH_FREQUENCY_TICKS.get() != 0) return;

        searchForLootContainers(stack, level, player);
    }

    private void searchForLootContainers(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        var playerPos = player.blockPosition();
        var nearestDistance = Double.MAX_VALUE;

        var maxRadiusXZ = CommonConfig.SEARCH_RADIUS_XZ.get();
        var maxRadiusY = CommonConfig.SEARCH_RADIUS_Y.get();

        var nearestContainer = searchLootrContainers(level, player, playerPos, maxRadiusXZ, maxRadiusY);
        if (nearestContainer != null) {
            nearestDistance = playerPos.distSqr(nearestContainer);
        }

        var nearestMinecartPos = searchLootrMinecarts(level, player, playerPos, maxRadiusXZ, maxRadiusY);
        if (nearestMinecartPos != null) {
            var minecartDistance = playerPos.distSqr(nearestMinecartPos);
            if (minecartDistance < nearestDistance) {
                nearestContainer = nearestMinecartPos;
            }
        }

        if (nearestContainer != null) {
            setFoundPosition(stack, nearestContainer);
        } else {
            clearFoundPosition(stack);
        }
    }

    @Nullable
    private BlockPos searchLootrContainers(@NotNull Level level, @NotNull Player player, @NotNull BlockPos playerPos, int maxRadiusXZ, int maxRadiusY) {
        for (var radius = 1; radius <= maxRadiusXZ; radius++) {
            for (var x = -radius; x <= radius; x++) {
                for (var z = -radius; z <= radius; z++) {
                    var isPositionAtEdgeOfRadius = Math.abs(x) == radius || Math.abs(z) == radius;
                    if (!isPositionAtEdgeOfRadius) continue;

                    for (var y = -maxRadiusY; y <= maxRadiusY; y++) {
                        var checkPos = playerPos.offset(x, y, z);
                        if (isUnopenedLootrContainer(level, checkPos, player)) return checkPos;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private BlockPos searchLootrMinecarts(@NotNull Level level, @NotNull Player player, @NotNull BlockPos playerPos, int maxRadiusXZ, int maxRadiusY) {
        var searchBox = new AABB(
                playerPos.getX() - maxRadiusXZ, playerPos.getY() - maxRadiusY, playerPos.getZ() - maxRadiusXZ,
                playerPos.getX() + maxRadiusXZ, playerPos.getY() + maxRadiusY, playerPos.getZ() + maxRadiusXZ
        );

        var minecarts = level.getEntitiesOfClass(LootrChestMinecartEntity.class, searchBox);

        BlockPos nearestMinecartPos = null;
        var nearestDistance = Double.MAX_VALUE;

        for (var minecart : minecarts) {
            if (minecart.isOpened()) continue;

            var minecartPos = minecart.blockPosition();
            var distance = playerPos.distSqr(minecartPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMinecartPos = minecartPos;
            }
        }

        return nearestMinecartPos;
    }

    private boolean isUnopenedLootrContainer(@NotNull Level level, @NotNull BlockPos pos, @NotNull Player player) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return false;

        if (!(blockEntity instanceof ILootBlockEntity lootBlockEntity)) return false;

        return !lootBlockEntity.getOpeners().contains(player.getUUID());
    }

    @Nullable
    private BlockPos getFoundPosition(@NotNull ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(FOUND_POS_TAG)) return null;

        var posTag = tag.getCompound(FOUND_POS_TAG);
        return new BlockPos(
                posTag.getInt("x"),
                posTag.getInt("y"),
                posTag.getInt("z")
        );
    }

    private void setFoundPosition(@NotNull ItemStack stack, @NotNull BlockPos pos) {
        var tag = stack.getOrCreateTag();
        var posTag = new CompoundTag();
        {
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
        }
        tag.put(FOUND_POS_TAG, posTag);
    }

    private void clearFoundPosition(@NotNull ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null) {
            tag.remove(FOUND_POS_TAG);
        }
    }
}
