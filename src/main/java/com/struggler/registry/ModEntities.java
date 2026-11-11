package com.struggler.registry;

import com.struggler.TheStruggler;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheStruggler.MOD_ID);

    public static final RegistryObject<EntityType<MidlandGuard>> MIDLAND_GUARD =
            ENTITY_TYPES.register("midland_guard", () ->
                    EntityType.Builder.of(MidlandGuard::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .build("midland_guard"));

    private ModEntities() {}

    @Mod.EventBusSubscriber(modid = TheStruggler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEntityEvents {
        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(MIDLAND_GUARD.get(), MidlandGuard.createAttributes().build());
        }
    }
}
