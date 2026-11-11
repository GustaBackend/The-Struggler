package com.struggler.registry;

import com.struggler.TheStruggler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheStruggler.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.struggler.main"))
                    .icon(() -> ModItems.BEHELIT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BEHELIT.get());
                        output.accept(ModItems.MIDLAND_GUARD_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }
}
