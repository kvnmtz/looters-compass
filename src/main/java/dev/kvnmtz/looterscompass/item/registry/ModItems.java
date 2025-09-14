package dev.kvnmtz.looterscompass.item.registry;

import dev.kvnmtz.looterscompass.LootersCompassMod;
import dev.kvnmtz.looterscompass.item.LootersCompassItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LootersCompassMod.MOD_ID);

    public static final RegistryObject<Item> LOOTERS_COMPASS = ITEMS.register("looters_compass", LootersCompassItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
