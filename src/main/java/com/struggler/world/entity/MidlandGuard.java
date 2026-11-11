package com.struggler.world.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
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

/**
 * Midland Guard: a vanilla-proportioned soldier with GeckoLib visuals and
 * server-authoritative melee combat.
 *
 * <p>Combat state (phase, strike target, cooldown, counters) is server-only and
 * transient. The only combat information sent to clients is a strike id plus
 * the game-time window in which the attack clip should play, so every tracking
 * player sees the same swing without a custom packet channel.</p>
 */
public class MidlandGuard extends PathfinderMob implements GeoEntity {
    /** Strike timing shared by the goal, the entity and the unit tests. */
    public static final GuardCombatTimeline STRIKE_TIMELINE = new GuardCombatTimeline(
            GuardCombatTuning.WINDUP_TICKS,
            GuardCombatTuning.STRIKE_TICKS,
            GuardCombatTuning.RECOVERY_TICKS);

    private static final Map<String, RawAnimation> ANIMATIONS = Map.of(
            "idle", RawAnimation.begin().thenLoop("animation.midland_guard.idle"),
            "walk", RawAnimation.begin().thenLoop("animation.midland_guard.walk"),
            "run", RawAnimation.begin().thenLoop("animation.midland_guard.run"),
            "attack", RawAnimation.begin().thenPlay("animation.midland_guard.attack"),
            "defend", RawAnimation.begin().thenPlay("animation.midland_guard.defend"),
            "block", RawAnimation.begin().thenLoop("animation.midland_guard.block"),
            "hurt", RawAnimation.begin().thenPlay("animation.midland_guard.hurt"),
            "death", RawAnimation.begin().thenPlayAndHold("animation.midland_guard.death"));

    private static final EntityDataAccessor<Byte> DATA_PREVIEW_ACTION =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_PREVIEW_STARTED_AT =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_PREVIEW_EXPIRES_AT =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.LONG);
    /** Monotonic strike counter; a change tells clients to restart the attack clip. */
    private static final EntityDataAccessor<Integer> DATA_STRIKE_ID =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.INT);
    /** World game time until which the attack clip should own the model. */
    private static final EntityDataAccessor<Long> DATA_ATTACK_ANIM_UNTIL =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.LONG);
    /**
     * True while the shield is up. Synchronized because the defensive pose is a
     * real server state that every tracking client must render, not a local guess.
     */
    private static final EntityDataAccessor<Boolean> DATA_BLOCKING =
            SynchedEntityData.defineId(MidlandGuard.class, EntityDataSerializers.BOOLEAN);

    private static final double PREVIEW_WALK_SPEED = 0.80D;
    private static final double PREVIEW_RUN_SPEED = 1.25D;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    /** Server-only combat state. Never written to or restored from NBT. */
    private GuardCombatPhase combatPhase = GuardCombatPhase.IDLE;
    private LivingEntity strikeTarget;
    private int strikeAge;
    private boolean impactApplied;
    private long nextAttackTick;
    private int strikeCount;
    private int impactAttempts;
    private int damageHits;
    /** Server-only defense state; the synced flag above mirrors it for rendering. */
    private long blockUntilTick;
    private int blockedHits;
    private float damagePrevented;
    /** QA counters: how often a block was requested and why it was refused. */
    private int blockAttempts;
    private int blockRefusals;

    private long renderedPreviewStamp = Long.MIN_VALUE;
    private int renderedStrikeId = -1;

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
        // A single controller owns every bone, so no two controllers can fight
        // over the same transforms.
        controllers.add(new AnimationController<>(this, "guard", 0, this::animationState));
    }

    /**
     * Priority: death, real strike window, hurt, QA preview, locomotion.
     * A strike window outranks hurt on purpose: taking a hit must not cancel
     * the swing that the server is already resolving.
     */
    private PlayState animationState(AnimationState<MidlandGuard> state) {
        if (this.deathTime > 0) {
            return state.setAndContinue(ANIMATIONS.get("death"));
        }

        if (isAttackAnimationActive()) {
            if (getStrikeId() != this.renderedStrikeId) {
                this.renderedStrikeId = getStrikeId();
                state.resetCurrentAnimation();
            }
            return state.setAndContinue(ANIMATIONS.get("attack"));
        }

        if (this.hurtTime > 0) {
            return state.setAndContinue(ANIMATIONS.get("hurt"));
        }

        // The shield pose is a real synchronized state: it outranks locomotion and
        // the QA preview, and it is always cleared before a strike starts.
        if (isShieldRaised()) {
            return state.setAndContinue(ANIMATIONS.get("block"));
        }

        if (isPreviewActionActive()) {
            if (this.renderedPreviewStamp != getPreviewActionStartedAt()) {
                this.renderedPreviewStamp = getPreviewActionStartedAt();
                state.resetCurrentAnimation();
            }
            return state.setAndContinue(ANIMATIONS.get(getPreviewAction().commandName()));
        }

        return state.setAndContinue(ANIMATIONS.get(state.isMoving() ? "walk" : "idle"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, GuardCombatTuning.BASE_ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, GuardCombatTuning.BASE_ATTACK_KNOCKBACK);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GuardCombatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Retaliation only: the guard answers whoever actually damaged it and
        // does not alert other guards. NearestAttackableTargetGoal<Monster>
        // covers hostile vanilla creatures only (never animals, villagers or
        // other guards); players are never targeted without being provoked.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PREVIEW_ACTION, GuardAnimation.Action.IDLE.id());
        this.entityData.define(DATA_PREVIEW_STARTED_AT, 0L);
        this.entityData.define(DATA_PREVIEW_EXPIRES_AT, 0L);
        this.entityData.define(DATA_STRIKE_ID, 0);
        this.entityData.define(DATA_ATTACK_ANIM_UNTIL, 0L);
        this.entityData.define(DATA_BLOCKING, false);
    }

    // ------------------------------------------------------------------
    // Combat state (server only)
    // ------------------------------------------------------------------

    /** A target is fightable while it is alive, in range, and not another guard. */
    public boolean canFightTarget(LivingEntity target) {
        if (target == null || target == this || !target.isAlive()) {
            return false;
        }
        if (target instanceof MidlandGuard) {
            return false;
        }
        if (target.level() != this.level()) {
            return false;
        }
        double range = GuardCombatTuning.PURSUIT_RANGE;
        if (this.distanceToSqr(target) > range * range) {
            return false;
        }
        return this.canAttack(target);
    }

    public boolean isStrikeInProgress() {
        return this.combatPhase.isStriking();
    }

    /** True while the guard is chasing, striking, or holding a valid target. */
    public boolean isCombatActive() {
        return this.isStrikeInProgress() || this.getTarget() != null;
    }

    public boolean canStartStrike() {
        return !this.isStrikeInProgress() && this.level().getGameTime() >= this.nextAttackTick;
    }

    /** Records the target for this strike and opens the synchronized attack window. */
    public void beginStrike(LivingEntity target) {
        if (this.level().isClientSide) {
            return;
        }
        // Attacking and blocking are mutually exclusive on purpose: the shield is
        // lowered the moment the guard commits to a swing.
        this.stopBlocking();
        this.strikeTarget = target;
        this.strikeAge = 0;
        this.impactApplied = false;
        this.combatPhase = GuardCombatPhase.WINDUP;
        this.strikeCount++;
        this.getNavigation().stop();
        this.getNavigation().setSpeedModifier(1.0D);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.entityData.set(DATA_STRIKE_ID, this.entityData.get(DATA_STRIKE_ID) + 1);
        this.entityData.set(DATA_ATTACK_ANIM_UNTIL,
                this.level().getGameTime() + GuardCombatTuning.ATTACK_CLIP_TICKS);
    }

    /** Advances one tick of the strike; called exactly once per server tick. */
    public void tickStrike() {
        if (this.level().isClientSide || !this.combatPhase.isStriking()) {
            return;
        }
        GuardCombatPhase phase = STRIKE_TIMELINE.phaseAt(this.strikeAge);
        if (STRIKE_TIMELINE.isImpactTick(this.strikeAge)) {
            this.applyImpact();
        }
        if (phase == GuardCombatPhase.IDLE) {
            this.finishStrike();
            return;
        }
        this.combatPhase = phase;
        this.strikeAge++;
    }

    /**
     * Resolves the single damage event of this strike. Every condition is
     * re-checked here because the target may have escaped, died or become
     * invalid during the windup.
     */
    private void applyImpact() {
        if (this.impactApplied) {
            return;
        }
        this.impactApplied = true;
        this.impactAttempts++;

        LivingEntity target = this.strikeTarget;
        if (target == null || !target.isAlive() || target.level() != this.level()) {
            return;
        }
        if (!this.canAttack(target)) {
            return;
        }
        double reach = GuardCombatTuning.IMPACT_REACH;
        if (this.distanceToSqr(target) > reach * reach) {
            return;
        }
        if (!this.getSensing().hasLineOfSight(target)) {
            return;
        }
        if (this.doHurtTarget(target)) {
            this.damageHits++;
        }
    }

    private void finishStrike() {
        this.combatPhase = GuardCombatPhase.IDLE;
        this.strikeTarget = null;
        this.impactApplied = false;
        this.strikeAge = 0;
        this.nextAttackTick = this.level().getGameTime() + GuardCombatTuning.COOLDOWN_TICKS;
    }

    /** Aborts the current strike without damage and starts the cooldown. */
    public void cancelStrike() {
        if (this.level().isClientSide) {
            return;
        }
        boolean wasStriking = this.combatPhase.isStriking();
        this.combatPhase = GuardCombatPhase.IDLE;
        this.strikeTarget = null;
        this.impactApplied = false;
        this.strikeAge = 0;
        if (wasStriking) {
            this.nextAttackTick = this.level().getGameTime() + GuardCombatTuning.COOLDOWN_TICKS;
        }
    }

    /** Drops every transient combat field; used on load and by the QA reset path. */
    public void resetCombatState() {
        this.combatPhase = GuardCombatPhase.IDLE;
        this.strikeTarget = null;
        this.impactApplied = false;
        this.strikeAge = 0;
        this.nextAttackTick = 0L;
        this.stopBlocking();
    }

    // ------------------------------------------------------------------
    // Shield defense (server authoritative, version 0.3.2)
    // ------------------------------------------------------------------

    /**
     * Raises the shield for {@link GuardCombatTuning#BLOCK_HOLD_TICKS} ticks.
     * Refused while a strike is being resolved, so attack and defense can never
     * overlap by accident. The synced flag is only written on the server.
     */
    public void startBlocking() {
        if (!this.level().isClientSide) {
            this.blockAttempts++;
        }
        if (this.level().isClientSide || this.isStrikeInProgress()) {
            if (!this.level().isClientSide) {
                this.blockRefusals++;
            }
            return;
        }
        this.blockUntilTick = this.level().getGameTime() + GuardCombatTuning.BLOCK_HOLD_TICKS;
        if (!this.entityData.get(DATA_BLOCKING)) {
            this.entityData.set(DATA_BLOCKING, true);
            this.getNavigation().stop();
        }
    }

    /** Lowers the shield immediately; safe to call repeatedly. */
    public void stopBlocking() {
        this.blockUntilTick = 0L;
        if (!this.level().isClientSide && this.entityData.get(DATA_BLOCKING)) {
            this.entityData.set(DATA_BLOCKING, false);
        }
    }

    /** True while the shield is up, on both sides (the flag is synchronized). */
    public boolean isShieldRaised() {
        return this.entityData.get(DATA_BLOCKING);
    }

    public int getBlockTicksRemaining() {
        return (int) Math.max(0L, this.blockUntilTick - this.level().getGameTime());
    }

    public int getBlockedHits() {
        return this.blockedHits;
    }

    public float getDamagePrevented() {
        return this.damagePrevented;
    }

    public int getBlockAttempts() {
        return this.blockAttempts;
    }

    public int getBlockRefusals() {
        return this.blockRefusals;
    }

    /** Ends the defensive window once it expires or a strike takes over. */
    private void tickDefense() {
        if (this.level().isClientSide) {
            return;
        }
        if (this.isStrikeInProgress() || this.level().getGameTime() >= this.blockUntilTick) {
            this.stopBlocking();
        }
    }

    /** True when the attacker is inside the frontal arc the shield covers. */
    private boolean isAttackerInFront(LivingEntity attacker) {
        Vec3 look = this.getLookAngle();
        Vec3 toAttacker = attacker.position().subtract(this.position());
        return GuardDefenseRules.isInsideFrontalCone(look.x, look.z, toAttacker.x, toAttacker.z);
    }

    /**
     * Single reduction point for the whole mod.
     *
     * <p>The shield only covers directional hits that arrive inside the frontal
     * arc while the block state is active; everything else (fire, fall, magic,
     * explosions, backstabs, hits taken during a swing) reaches
     * {@code super.hurt} untouched. Because the reduction is applied here and
     * nowhere else, the same hit can never be reduced twice.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean shieldUp = !this.level().isClientSide && this.isShieldRaised();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        boolean frontal = shieldUp && attacker != null && this.isAttackerInFront(attacker);

        float applied = frontal ? GuardDefenseRules.reducedDamage(amount) : amount;
        boolean damaged = super.hurt(source, applied);

        if (damaged && !this.level().isClientSide) {
            if (frontal) {
                this.blockedHits++;
                this.damagePrevented += Math.max(0.0F, amount - applied);
            }
            // A frontal hit is a reason to keep the shield up, even if the guard
            // had no target yet: the next hit is already covered.
            if (attacker != null && this.isAttackerInFront(attacker)) {
                this.startBlocking();
            }
        }
        return damaged;
    }

    /**
     * Keeps the target stable for the duration of a strike. Clearing is always
     * allowed so death and invalidation can still release the guard.
     */
    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && target != this.getTarget() && this.isStrikeInProgress()) {
            return;
        }
        super.setTarget(target);
    }

    public GuardCombatPhase getCombatPhase() {
        return this.combatPhase;
    }

    public int getStrikeCount() {
        return this.strikeCount;
    }

    public int getImpactAttempts() {
        return this.impactAttempts;
    }

    public int getDamageHits() {
        return this.damageHits;
    }

    public int getCooldownTicks() {
        return (int) Math.max(0L, this.nextAttackTick - this.level().getGameTime());
    }

    public int getStrikeId() {
        return this.entityData.get(DATA_STRIKE_ID);
    }

    public boolean isAttackAnimationActive() {
        return this.level().getGameTime() < this.entityData.get(DATA_ATTACK_ANIM_UNTIL);
    }

    // ------------------------------------------------------------------
    // QA preview (unchanged surface, isolated from combat)
    // ------------------------------------------------------------------

    /**
     * Starts a short server-authoritative animation preview. Commands are the
     * only caller; this is not a combat state and never deals damage.
     *
     * @return false when real combat is active and the preview was refused
     */
    public boolean setPreviewAction(GuardAnimation.Action action) {
        if (this.level().isClientSide || isCombatActive()) {
            return false;
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
        return true;
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

    /** Fractional action time used by QA tooling; server and client share world timestamps. */
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

        // The shield window is resolved before any goal runs, so a strike that
        // starts on this tick already finds the block cleared.
        this.tickDefense();

        // Real combat owns navigation. The QA preview may never stop a chase,
        // redirect the guard or interfere with the strike/cooldown timeline.
        if (isCombatActive()) {
            return;
        }

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

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    /**
     * Guards are placed soldiers: they do not vanish when the player walks
     * away, which keeps spawn-egg guards predictable in a built position.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Read-only diagnostics for QA and /data queries. These fields are
        // deliberately never restored: see readAdditionalSaveData.
        tag.putString("AnimationAction", this.getRawPreviewAction().commandName());
        tag.putLong("AnimationStartedAt", this.getPreviewActionStartedAt());
        tag.putLong("AnimationExpiresAt", this.getPreviewActionExpiresAt());
        tag.putInt("AnimationTime", this.getPreviewActionTimeTicks());
        tag.putString("CombatState", this.combatPhase.name());
        tag.putInt("CombatStrikes", this.strikeCount);
        tag.putInt("CombatImpactAttempts", this.impactAttempts);
        tag.putInt("CombatDamageHits", this.damageHits);
        tag.putInt("CombatCooldown", this.getCooldownTicks());
        tag.putBoolean("DefenseBlocking", this.isShieldRaised());
        tag.putInt("DefenseBlockTicks", this.getBlockTicksRemaining());
        tag.putInt("DefenseBlockedHits", this.blockedHits);
        tag.putFloat("DefenseDamagePrevented", this.damagePrevented);
    }

    /**
     * Combat is never resumed from disk. A saved WINDUP/ATTACKING phase must not
     * produce damage after a reload, so every transient field starts clean and
     * the vanilla target is dropped.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.resetCombatState();
        this.setTarget(null);
        this.previewGoal = null;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_ATTACK_ANIM_UNTIL, 0L);
        }
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
