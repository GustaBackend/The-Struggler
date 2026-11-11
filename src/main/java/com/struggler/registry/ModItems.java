package com.struggler.registry;

import com.struggler.TheStruggler;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheStruggler.MOD_ID);

    public static final RegistryObject<Item> BEHELIT =
            ITEMS.register("behelit", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MIDLAND_GUARD_SPAWN_EGG =
            ITEMS.register("midland_guard_spawn_egg", () ->
                    new ForgeSpawnEggItem(
                            ModEntities.MIDLAND_GUARD,
                            0x6D5748,
                            0xB8A58A,
                            new Item.Properties()
                    ));

    private ModItems() {}
}
