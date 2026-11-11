package com.struggler.client.render;

import com.struggler.TheStruggler;
import com.struggler.client.model.MidlandGuardModel;
import com.struggler.registry.ModEntities;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheStruggler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MidlandGuardRenderer extends MobRenderer<MidlandGuard, MidlandGuardModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TheStruggler.MOD_ID, "textures/entity/midland_guard.png");

    public MidlandGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new MidlandGuardModel(context.bakeLayer(MidlandGuardModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MidlandGuard entity) {
        return TEXTURE;
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MIDLAND_GUARD.get(), MidlandGuardRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MidlandGuardModel.LAYER_LOCATION, MidlandGuardModel::createBodyLayer);
    }
}
