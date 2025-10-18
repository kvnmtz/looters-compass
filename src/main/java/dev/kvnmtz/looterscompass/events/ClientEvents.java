package dev.kvnmtz.looterscompass.events;

import dev.kvnmtz.looterscompass.LootersCompassMod;
import dev.kvnmtz.looterscompass.client.CompassGlowManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LootersCompassMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        var minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            CompassGlowManager.tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        CompassGlowManager.clearGlow();
    }
}