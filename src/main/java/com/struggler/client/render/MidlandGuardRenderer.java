package com.struggler.client.render;

import com.struggler.TheStruggler;
import com.struggler.client.model.MidlandGuardModel;
import com.struggler.registry.ModEntities;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mod.EventBusSubscriber(modid = TheStruggler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MidlandGuardRenderer extends GeoEntityRenderer<MidlandGuard> {
    public MidlandGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new MidlandGuardModel());
        this.shadowRadius = 0.5F;
    }

    @Override
    protected float getDeathMaxRotation(MidlandGuard entity) {
        return 0.0F; // The authored GeckoLib death clip owns the collapse.
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MIDLAND_GUARD.get(), MidlandGuardRenderer::new);
    }
}
