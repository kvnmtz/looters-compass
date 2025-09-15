package dev.kvnmtz.looterscompass.events;

import dev.kvnmtz.looterscompass.LootersCompassMod;
import dev.kvnmtz.looterscompass.config.CommonConfig;
import dev.kvnmtz.looterscompass.item.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LootersCompassMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static final ResourceLocation TROPHY_TABLE_ID = ResourceLocation.fromNamespaceAndPath("lootr", "trophy");

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!CommonConfig.ADD_TO_100_LOOT_ADVANCEMENT_REWARDS.get()) return;

        if (!event.getName().equals(TROPHY_TABLE_ID)) return;

        var pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ModItems.LOOTERS_COMPASS.get()))
                .build();

        event.getTable().addPool(pool);
    }
}
