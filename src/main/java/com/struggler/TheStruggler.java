package com.struggler;

import com.struggler.registry.ModCreativeModeTabs;
import com.struggler.registry.ModEntities;
import com.struggler.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TheStruggler.MOD_ID)
public class TheStruggler {
    public static final String MOD_ID = "struggler";

    public TheStruggler() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);
    }
}
