package dev.kvnmtz.looterscompass.client;

import dev.kvnmtz.looterscompass.config.ServerConfig;
import dev.kvnmtz.looterscompass.mixin.ClientLevelAccessor;
import dev.kvnmtz.looterscompass.mixin.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlowEntityManager {
    
    // For block glow (spawned shulker entities)
    private static Shulker currentGlowEntity = null;
    private static long blockGlowStartTime = 0;
    
    // For entity glow (existing entities like minecarts)
    private static Entity currentGlowingEntity = null;
    private static long entityGlowStartTime = 0;

    public static void addGlowToBlock(BlockPos pos, Level level) {
        removeGlowFromBlock();

        var glowEntity = new Shulker(EntityType.SHULKER, level);

        var blockCenter = Vec3.atCenterOf(pos.below());
        glowEntity.setPos(blockCenter.x, blockCenter.y, blockCenter.z);

        glowEntity.setInvisible(true);
        glowEntity.setNoAi(true);
        glowEntity.setNoGravity(true);
        glowEntity.setSilent(true);
        glowEntity.setInvulnerable(true);
        glowEntity.setCustomNameVisible(false);
        setClientEntityGlowing(glowEntity, true);

        addClientEntity(level, glowEntity);

        currentGlowEntity = glowEntity;
        blockGlowStartTime = System.currentTimeMillis();
    }

    public static void setClientEntityGlowing(Entity entity, boolean glowing) {
        ((EntityAccessor) entity).callSetSharedFlag(6, glowing);
    }

    private static void addClientEntity(Level level, Entity glowEntity) {
        var entityId = level.getFreeMapId();
        glowEntity.setId(entityId);

        ((ClientLevelAccessor) level).callAddEntity(entityId, glowEntity);
    }

    public static void removeGlowFromBlock() {
        if (currentGlowEntity != null && currentGlowEntity.isAlive()) {
            currentGlowEntity.discard();
        }

        currentGlowEntity = null;
        blockGlowStartTime = 0;
    }
    
    public static void removeGlowFromEntity() {
        if (currentGlowingEntity != null) {
            setClientEntityGlowing(currentGlowingEntity, false);
        }
        
        currentGlowingEntity = null;
        entityGlowStartTime = 0;
    }
    
    public static void addGlowToEntity(Entity entity) {
        removeGlowFromEntity();

        setClientEntityGlowing(entity, true);

        currentGlowingEntity = entity;
        entityGlowStartTime = System.currentTimeMillis();
    }
    
    public static void tickGlowEntities() {
        var currentTime = System.currentTimeMillis();
        var glowDurationMs = ServerConfig.GLOW_DURATION.get() * 1000;

        if (currentGlowEntity != null) {
            if (currentTime - blockGlowStartTime > glowDurationMs) {
                removeGlowFromBlock();
            }
        }

        if (currentGlowingEntity != null) {
            if (currentTime - entityGlowStartTime > glowDurationMs) {
                removeGlowFromEntity();
            }
        }
    }
}