package com.struggler.client.model;

import com.struggler.TheStruggler;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/** Native GeckoLib geometry and keyframes; no custom polygon renderer remains. */
public final class MidlandGuardModel extends GeoModel<MidlandGuard> {
    @Override
    public ResourceLocation getModelResource(MidlandGuard entity) {
        return new ResourceLocation(TheStruggler.MOD_ID, "geo/midland_guard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MidlandGuard entity) {
        return new ResourceLocation(TheStruggler.MOD_ID, "textures/entity/midland_guard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MidlandGuard entity) {
        return new ResourceLocation(TheStruggler.MOD_ID, "animations/midland_guard.animation.json");
    }

    @Override
    public void setCustomAnimations(MidlandGuard entity, long instanceId,
                                    AnimationState<MidlandGuard> state) {
        super.setCustomAnimations(entity, instanceId, state);
        if (entity.deathTime == 0) {
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            getBone("head").ifPresent(head -> {
                head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
                head.setRotY(head.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
            });
        }
    }
}
