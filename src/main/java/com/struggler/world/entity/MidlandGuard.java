package com.struggler.world.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Map;

public class MidlandGuard extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private long renderedPreviewStamp = Long.MIN_VALUE;
    private static final Map<String, RawAnimation> ANIMATIONS = Map.of(
            "idle", RawAnimation.begin().thenLoop("animation.midland_guard.idle"),
            "walk", RawAnimation.begin().thenLoop("animation.midland_guard.walk"),
            "run", RawAnimation.begin().thenLoop("animation.midland_guard.run"),
            "attack", RawAnimation.begin().thenPlay("animation.midland_guard.attack"),
            "defend", RawAnimation.begin().thenPlay("animation.midland_guard.defend"),
            "hurt", RawAnimation.begin().thenPlay("animation.midland_guard.hurt"),
            "death", RawAnimation.begin().thenPlayAndHold("animation.midland_guard.death"));
    private static final EntityDataAccessor<Byte> DATA_PREVIEW_ACTION =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_PREVIEW_STARTED_AT =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_PREVIEW_EXPIRES_AT =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.LONG);

    private static final double PREVIEW_WALK_SPEED = 0.80D;
    private static final double PREVIEW_RUN_SPEED = 1.25D;

    /** The server-only straight-segment endpoint used by walk/run preview actions. */
    private Vec3 previewGoal;

    public MidlandGuard(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "guard", 0, this::animationState));
    }

    private PlayState animationState(AnimationState<MidlandGuard> state) {
        if (isPreviewActionActive() && renderedPreviewStamp != getPreviewActionStartedAt()) {
            renderedPreviewStamp = getPreviewActionStartedAt();
            state.resetCurrentAnimation();
        }
        String selected;
        if (this.deathTime > 0) {
            selected = "death";
        } else if (this.hurtTime > 0) {
            selected = "hurt";
        } else if (isPreviewActionActive()) {
            selected = getPreviewAction().commandName();
        } else {
            selected = state.isMoving() ? "walk" : "idle";
        }
        return state.setAndContinue(ANIMATIONS.get(selected));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PREVIEW_ACTION, GuardAnimation.Action.IDLE.id());
        this.entityData.define(DATA_PREVIEW_STARTED_AT, 0L);
        this.entityData.define(DATA_PREVIEW_EXPIRES_AT, 0L);
    }

    /**
     * Starts a short server-authoritative animation preview. Commands are the only caller;
     * this is not a combat state and does not select targets or apply damage/blocking.
     */
    public void setPreviewAction(GuardAnimation.Action action) {
        if (this.level().isClientSide) {
            return;
        }
        GuardAnimation.Action requested = action == null ? GuardAnimation.Action.IDLE : action;
        long startedAt = this.level().getGameTime();
        long expiresAt = startedAt + requested.durationTicks();
        this.entityData.set(DATA_PREVIEW_ACTION, requested.id());
        this.entityData.set(DATA_PREVIEW_STARTED_AT, startedAt);
        this.entityData.set(DATA_PREVIEW_EXPIRES_AT, expiresAt);

        this.previewGoal = null;
        this.getNavigation().stop();
        this.getNavigation().setSpeedModifier(1.0D);
        if (requested.moves()) {
            this.previewGoal = this.makePreviewGoal();
            this.getNavigation().moveTo(this.previewGoal.x, this.previewGoal.y, this.previewGoal.z,
                    requested == GuardAnimation.Action.RUN ? PREVIEW_RUN_SPEED : PREVIEW_WALK_SPEED);
        }
    }

    /** Reset the preview state immediately; vanilla goals can run again on the next AI tick. */
    public void resetPreviewAction() {
        if (this.level().isClientSide) {
            return;
        }
        this.entityData.set(DATA_PREVIEW_ACTION, GuardAnimation.Action.IDLE.id());
        this.entityData.set(DATA_PREVIEW_STARTED_AT, this.level().getGameTime());
        this.entityData.set(DATA_PREVIEW_EXPIRES_AT, 0L);
        this.previewGoal = null;
        this.getNavigation().stop();
        this.getNavigation().setSpeedModifier(1.0D);
    }

    private GuardAnimation.Action getRawPreviewAction() {
        return GuardAnimation.Action.fromId(this.entityData.get(DATA_PREVIEW_ACTION));
    }

    /** True while the synchronized action window is still open, including explicit idle hold. */
    public boolean isPreviewActionActive() {
        return this.entityData.get(DATA_PREVIEW_EXPIRES_AT) > this.level().getGameTime();
    }

    public GuardAnimation.Action getPreviewAction() {
        return this.isPreviewActionActive() ? this.getRawPreviewAction() : GuardAnimation.Action.IDLE;
    }

    public long getPreviewActionStartedAt() {
        return this.entityData.get(DATA_PREVIEW_STARTED_AT);
    }

    public long getPreviewActionExpiresAt() {
        return this.entityData.get(DATA_PREVIEW_EXPIRES_AT);
    }

    /** Fractional action time used by the client model; server and client share world timestamps. */
    public float getPreviewActionTime(float partialTick) {
        if (!this.isPreviewActionActive()) {
            return 0.0F;
        }
        float elapsed = (float) (this.level().getGameTime() - this.getPreviewActionStartedAt()) + partialTick;
        int duration = this.getRawPreviewAction().durationTicks();
        return Math.max(0.0F, Math.min(elapsed, duration));
    }

    public int getPreviewActionTimeTicks() {
        if (!this.isPreviewActionActive()) {
            return 0;
        }
        long elapsed = this.level().getGameTime() - this.getPreviewActionStartedAt();
        return (int) Math.max(0L, Math.min(elapsed, (long) this.getRawPreviewAction().durationTicks()));
    }

    private Vec3 makePreviewGoal() {
        double yawRadians = Math.toRadians(this.getYRot());
        double x = -Math.sin(yawRadians);
        double z = Math.cos(yawRadians);
        return this.position().add(x * GuardAnimation.PREVIEW_GOAL_DISTANCE, 0.0D,
                z * GuardAnimation.PREVIEW_GOAL_DISTANCE);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!this.isPreviewActionActive()) {
            if (this.getPreviewActionExpiresAt() != 0) {
                this.resetPreviewAction();
            }
            return;
        }

        GuardAnimation.Action action = this.getRawPreviewAction();
        if (!action.moves()) {
            // Idle/attack/defend are film holds; do not let an existing goal move the guard.
            this.getNavigation().stop();
            return;
        }

        if (this.previewGoal == null) {
            this.previewGoal = this.makePreviewGoal();
        }
        // Reassert the stored endpoint after vanilla goals have ticked. This affects only the
        // bounded preview window and leaves the original goals and base movement attribute intact.
        this.getNavigation().moveTo(this.previewGoal.x, this.previewGoal.y, this.previewGoal.z,
                action == GuardAnimation.Action.RUN ? PREVIEW_RUN_SPEED : PREVIEW_WALK_SPEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.isPreviewActionActive()
                && this.getPreviewActionExpiresAt() != 0) {
            this.resetPreviewAction();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // These transient QA fields make /data query useful without restoring a preview on load.
        tag.putString("AnimationAction", this.getRawPreviewAction().commandName());
        tag.putLong("AnimationStartedAt", this.getPreviewActionStartedAt());
        tag.putLong("AnimationExpiresAt", this.getPreviewActionExpiresAt());
        tag.putInt("AnimationTime", this.getPreviewActionTimeTicks());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (this.level().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("dialogue.struggler.midland_guard.greeting"), false);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
}
