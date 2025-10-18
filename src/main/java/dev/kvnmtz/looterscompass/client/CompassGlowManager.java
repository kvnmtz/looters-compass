package dev.kvnmtz.looterscompass.client;

import dev.kvnmtz.looterscompass.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompassGlowManager {

    private static BlockPos glowingPosition = null;
    private static long glowStartTime = 0;

    public static void activateGlow(BlockPos position) {
        clearGlow();

        var level = Minecraft.getInstance().level;
        if (level != null) {
            var blockEntity = level.getBlockEntity(position);
            if (blockEntity == null) {
                // TODO: probably a better check would be if it is a full block that's not translucent, but it isn't
                //  really necessary anyways
                if (level.getBlockState(position).isAir()) {
                    return;
                }

                GlowEntityManager.addGlowToBlock(position, level);
            } else {
                var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);

                // if it has a custom renderer, the LevelRendererMixin (or the Embeddium counterpart) will handle it
                var hasCustomRenderer = renderer != null;
                if (!hasCustomRenderer) {
                    GlowEntityManager.addGlowToBlock(position, level);
                }
            }
        }

        glowingPosition = position;
        glowStartTime = System.currentTimeMillis();
    }

    public static void activateGlowForEntity(Entity entity) {
        clearGlow();

        glowingPosition = null;
        glowStartTime = System.currentTimeMillis();

        GlowEntityManager.addGlowToEntity(entity);
    }

    public static void clearGlow() {
        GlowEntityManager.removeGlowFromBlock();
        GlowEntityManager.removeGlowFromEntity();

        glowingPosition = null;
        glowStartTime = 0;
    }

    public static boolean isGlowing(BlockPos position) {
        if (glowingPosition == null || !glowingPosition.equals(position)) {
            return false;
        }

        var currentTime = System.currentTimeMillis();
        var glowDurationMs = ServerConfig.GLOW_DURATION.get() * 1000;
        var isActive = (currentTime - glowStartTime) < glowDurationMs;

        if (!isActive) {
            clearGlow();
        }

        return isActive;
    }

    public static void tick() {
        GlowEntityManager.tickGlowEntities();
    }
}