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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import noobanidus.mods.lootr.api.blockentity.ILootBlockEntity;
import net.minecraft.world.phys.AABB;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

import java.util.List;

@ParametersAreNonnullByDefault
public class LootersCompassItem extends Item {

    private static final String FOUND_POS_TAG = "FoundPos";

    private static final int SEARCH_CACHE_DURATION_TICKS = 100;

    public LootersCompassItem() {
        super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON));

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LootersCompassItemClient.registerItemProperties(this));
    }

    @OnlyIn(Dist.CLIENT)
    private static class LootersCompassItemClient {
        private static final ClientCompassData CACHE = new ClientCompassData();
        
        public static ClientCompassData getCache() {
            return CACHE;
        }

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
        
        public static class ClientCompassData {
            public UUID trackedEntityUUID;
            public boolean isTrackingEntity;
            public BlockPos lastSearchPos;
            public BlockPos lastSearchResult;
            public int lastSearchTime;
            
            public ClientCompassData() {}
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);

        if (!ServerConfig.ENABLE_LOOT_CONTAINER_GLOW.get()) {
            return InteractionResultHolder.success(stack);
        }

        var targetPos = getFoundPosition(stack);
        if (targetPos == null) {
            return InteractionResultHolder.success(stack);
        }

        if (level.isClientSide) {
            var cache = LootersCompassItemClient.getCache();
            if (cache.isTrackingEntity && cache.trackedEntityUUID != null) {
                var entity = findEntityByUUID((ClientLevel) level, cache.trackedEntityUUID);
                if (entity instanceof LootrChestMinecartEntity minecart) {
                    CompassGlowManager.activateGlowForEntity(minecart);
                }
            } else {
                CompassGlowManager.activateGlow(targetPos);
            }
        }

        var cooldown = ServerConfig.GLOW_COOLDOWN.get();
        if (cooldown > 0) {
            player.getCooldowns().addCooldown(this, ServerConfig.GLOW_COOLDOWN.get());
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("item.looters_compass.looters_compass.desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        var cache = LootersCompassItemClient.getCache();
        if (cache.isTrackingEntity && cache.trackedEntityUUID != null) {
            var trackedEntity = findEntityByUUID((ClientLevel) level, cache.trackedEntityUUID);
            if (trackedEntity != null) {
                setFoundPosition(stack, trackedEntity.blockPosition());
            } else {
                clearFoundPosition(stack);
                cache.isTrackingEntity = false;
                cache.trackedEntityUUID = null;
            }
        }

        if (player.tickCount % ClientConfig.SEARCH_FREQUENCY_TICKS.get() != 0) return;
        searchForLootContainers(stack, level, player);
    }

    @OnlyIn(Dist.CLIENT)
    private void searchForLootContainers(ItemStack stack, Level level, Player player) {
        var playerPos = player.blockPosition();
        var cache = LootersCompassItemClient.getCache();

        var playerDidntMoveMuch = cache.lastSearchPos != null && playerPos.distSqr(cache.lastSearchPos) <= 9;
        var isCacheApplicable = cache.lastSearchTime > 0 && player.tickCount - cache.lastSearchTime <= SEARCH_CACHE_DURATION_TICKS && playerDidntMoveMuch;

        if (isCacheApplicable) {
            if (cache.lastSearchResult != null) {
                cache.isTrackingEntity = false;
                cache.trackedEntityUUID = null;
                setFoundPosition(stack, cache.lastSearchResult);
            } else {
                clearFoundPosition(stack);
            }
            return;
        }

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
                cacheSearchResult(nearestMinecart.blockPosition(), playerPos, player.tickCount);
                return;
            }
        }

        if (nearestContainer != null) {
            cache.isTrackingEntity = false;
            cache.trackedEntityUUID = null;
            setFoundPosition(stack, nearestContainer);
            cacheSearchResult(nearestContainer, playerPos, player.tickCount);
        } else {
            clearFoundPosition(stack);
            cacheSearchResult(null, playerPos, player.tickCount);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private BlockPos searchLootrContainers(Level level, Player player, BlockPos playerPos, int maxRadiusXZ, int maxRadiusY) {
        BlockPos nearestContainer = null;
        var nearestDistanceSq = Double.MAX_VALUE;

        // convert blocks to chunks (add one to be safe)
        var chunkRadius = (maxRadiusXZ >> 4) + 1;
        var playerChunkX = playerPos.getX() >> 4;
        var playerChunkZ = playerPos.getZ() >> 4;

        for (var chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
            for (var chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                var isChunkLoaded = level.isLoaded(new BlockPos(chunkX << 4, playerPos.getY(), chunkZ << 4));
                if (!isChunkLoaded) continue;
                
                var chunk = level.getChunk(chunkX, chunkZ);

                var blockEntities = chunk.getBlockEntities();
                for (var entry : blockEntities.entrySet()) {
                    var pos = entry.getKey();
                    var blockEntity = entry.getValue();

                    if (!isUnopenedLootrContainer(blockEntity, player)) continue;

                    var dx = pos.getX() - playerPos.getX();
                    var dy = pos.getY() - playerPos.getY();
                    var dz = pos.getZ() - playerPos.getZ();

                    var isWithinSearchRadius = Math.abs(dx) <= maxRadiusXZ && Math.abs(dz) <= maxRadiusXZ && Math.abs(dy) <= maxRadiusY;
                    if (!isWithinSearchRadius) {
                        continue;
                    }

                    var distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq < nearestDistanceSq) {
                        nearestDistanceSq = distanceSq;
                        nearestContainer = pos;
                    }
                }
            }
        }
        
        return nearestContainer;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private LootrChestMinecartEntity searchLootrMinecarts(Level level, BlockPos playerPos, int maxRadiusXZ, int maxRadiusY) {
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

    @OnlyIn(Dist.CLIENT)
    private boolean isUnopenedLootrContainer(BlockEntity blockEntity, Player player) {
        if (!(blockEntity instanceof ILootBlockEntity lootBlockEntity)) {
            return false;
        }

        return !lootBlockEntity.getOpeners().contains(player.getUUID());
    }

    @Nullable
    private BlockPos getFoundPosition(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(FOUND_POS_TAG)) return null;

        var posTag = tag.getCompound(FOUND_POS_TAG);
        return new BlockPos(
                posTag.getInt("x"),
                posTag.getInt("y"),
                posTag.getInt("z")
        );
    }

    @OnlyIn(Dist.CLIENT)
    private void setFoundPosition(ItemStack stack, BlockPos pos) {
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
    }

    @OnlyIn(Dist.CLIENT)
    private void clearFoundPosition(ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null) {
            tag.remove(FOUND_POS_TAG);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void setFoundEntity(ItemStack stack, Entity entity) {
        var cache = LootersCompassItemClient.getCache();
        cache.trackedEntityUUID = entity.getUUID();
        cache.isTrackingEntity = true;

        setFoundPosition(stack, entity.blockPosition());
    }

    @OnlyIn(Dist.CLIENT)
    private @Nullable Entity findEntityByUUID(ClientLevel level, UUID uuid) {
        return ((ClientLevelAccessor) level).callGetEntities().get(uuid);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void cacheSearchResult(@Nullable BlockPos result, BlockPos playerPos, int currentTick) {
        var cache = LootersCompassItemClient.getCache();
        cache.lastSearchPos = playerPos;
        cache.lastSearchResult = result;
        cache.lastSearchTime = currentTick;
    }
}
