package dev.kvnmtz.looterscompass.item;

import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import dev.kvnmtz.looterscompass.config.ClientConfig;
import dev.kvnmtz.looterscompass.config.ServerConfig;
import dev.kvnmtz.looterscompass.mixin.ClientLevelAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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

import java.util.Objects;
import java.util.UUID;

import java.util.List;

public class LootersCompassItem extends Item {
    private static final String FOUND_POS_TAG = "FoundPos";
    private static final String FOUND_ENTITY_UUID_TAG = "FoundEntityUUID";
    private static final String IS_TRACKING_ENTITY_TAG = "IsTrackingEntity";

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
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);

        if (!ServerConfig.ENABLE_LOOT_CONTAINER_GLOW.get()) {
            return InteractionResultHolder.success(stack);
        }

        if (level.isClientSide) {
            if (isTrackingEntity(stack)) {
                var entityUUID = getFoundEntityUUID(stack);
                if (entityUUID != null) {
                    var entity = findEntityByUUID((ClientLevel) level, entityUUID);
                    if (entity instanceof LootrChestMinecartEntity minecart) {
                        CompassGlowManager.activateGlowForEntity(minecart);
                    }
                }
            } else {
                var targetPos = getFoundPosition(stack);
                if (targetPos != null) {
                    CompassGlowManager.activateGlow(targetPos);
                } else {
                    return InteractionResultHolder.success(stack);
                }
            }
        }

        var cooldown = ServerConfig.GLOW_COOLDOWN.get();
        if (cooldown > 0) {
            player.getCooldowns().addCooldown(this, ServerConfig.GLOW_COOLDOWN.get());
        }

        return InteractionResultHolder.success(stack);
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

        if (isTrackingEntity(stack)) {
            var entityUUID = getFoundEntityUUID(stack);
            if (entityUUID != null) {
                var trackedEntity = findEntityByUUID((ClientLevel) level, entityUUID);
                if (trackedEntity != null) {
                    setFoundPosition(stack, trackedEntity.blockPosition());
                } else {
                    clearFoundPosition(stack);
                }
            }
        }

        if (player.tickCount % ClientConfig.SEARCH_FREQUENCY_TICKS.get() != 0) return;
        searchForLootContainers(stack, level, player);
    }

    private void searchForLootContainers(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        var playerPos = player.blockPosition();
        var nearestDistance = Double.MAX_VALUE;

        var maxRadiusXZ = ServerConfig.SEARCH_RADIUS_XZ.get();
        var maxRadiusY = ServerConfig.SEARCH_RADIUS_Y.get();

        var nearestContainer = searchLootrContainers(level, player, playerPos, maxRadiusXZ, maxRadiusY);
        if (nearestContainer != null) {
            nearestDistance = playerPos.distSqr(nearestContainer);
        }

        var nearestMinecart = searchLootrMinecarts(level, playerPos, maxRadiusXZ, maxRadiusY);
        if (nearestMinecart != null) {
            var minecartDistance = playerPos.distSqr(nearestMinecart.blockPosition());
            if (minecartDistance < nearestDistance) {
                setFoundEntity(stack, nearestMinecart);
                return;
            }
        }

        if (nearestContainer != null) {
            var tag = stack.getOrCreateTag();
            tag.remove(FOUND_ENTITY_UUID_TAG);
            tag.remove(IS_TRACKING_ENTITY_TAG);
            setFoundPosition(stack, nearestContainer);
        } else {
            clearFoundPosition(stack);
        }
    }

    @Nullable
    private BlockPos searchLootrContainers(@NotNull Level level, @NotNull Player player, @NotNull BlockPos playerPos, int maxRadiusXZ, int maxRadiusY) {
        BlockPos nearestContainer = null;
        double nearestDistanceSq = Double.MAX_VALUE;

        for (var radius = 1; radius <= maxRadiusXZ; radius++) {
            for (var x = -radius; x <= radius; x++) {
                for (var z = -radius; z <= radius; z++) {
                    var isPositionAtEdgeOfRadius = Math.abs(x) == radius || Math.abs(z) == radius;
                    if (!isPositionAtEdgeOfRadius) continue;

                    for (var y = -maxRadiusY; y <= maxRadiusY; y++) {
                        var checkPos = playerPos.offset(x, y, z);
                        if (isUnopenedLootrContainer(level, checkPos, player)) {
                            var distanceSq = playerPos.distSqr(checkPos);
                            if (distanceSq < nearestDistanceSq) {
                                nearestDistanceSq = distanceSq;
                                nearestContainer = checkPos;
                            }
                        }
                    }
                }
            }
        }

        return nearestContainer;
    }

    @Nullable
    private LootrChestMinecartEntity searchLootrMinecarts(@NotNull Level level, @NotNull BlockPos playerPos, int maxRadiusXZ, int maxRadiusY) {
        var searchBox = new AABB(
                playerPos.getX() - maxRadiusXZ, playerPos.getY() - maxRadiusY, playerPos.getZ() - maxRadiusXZ,
                playerPos.getX() + maxRadiusXZ, playerPos.getY() + maxRadiusY, playerPos.getZ() + maxRadiusXZ
        );

        var minecarts = level.getEntitiesOfClass(LootrChestMinecartEntity.class, searchBox);

        LootrChestMinecartEntity nearestMinecart = null;
        var nearestDistance = Double.MAX_VALUE;

        for (var minecart : minecarts) {
            if (minecart.isOpened()) continue;

            var minecartPos = minecart.blockPosition();
            var distance = playerPos.distSqr(minecartPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMinecart = minecart;
            }
        }

        return nearestMinecart;
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
        var hasPositionChanged = getFoundPosition(stack) == null || Objects.requireNonNull(getFoundPosition(stack)).asLong() != pos.asLong();
        if (!hasPositionChanged)
            return;

        var tag = stack.getOrCreateTag();
        var posTag = new CompoundTag();
        {
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
        }
        tag.put(FOUND_POS_TAG, posTag);

        CompassGlowManager.clearGlow();
    }

    private void clearFoundPosition(@NotNull ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null) {
            tag.remove(FOUND_POS_TAG);
            tag.remove(FOUND_ENTITY_UUID_TAG);
            tag.remove(IS_TRACKING_ENTITY_TAG);
        }

        CompassGlowManager.clearGlow();
    }

    private void setFoundEntity(@NotNull ItemStack stack, @NotNull Entity entity) {
        var tag = stack.getOrCreateTag();
        tag.putString(FOUND_ENTITY_UUID_TAG, entity.getUUID().toString());
        tag.putBoolean(IS_TRACKING_ENTITY_TAG, true);

        setFoundPosition(stack, entity.blockPosition());
    }
    
    @Nullable
    private UUID getFoundEntityUUID(@NotNull ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(FOUND_ENTITY_UUID_TAG)) return null;
        
        try {
            return UUID.fromString(tag.getString(FOUND_ENTITY_UUID_TAG));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    private boolean isTrackingEntity(@NotNull ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.getBoolean(IS_TRACKING_ENTITY_TAG);
    }
    
    private @Nullable Entity findEntityByUUID(@NotNull ClientLevel level, @NotNull UUID uuid) {
        return ((ClientLevelAccessor) level).callGetEntities().get(uuid);
    }
}
