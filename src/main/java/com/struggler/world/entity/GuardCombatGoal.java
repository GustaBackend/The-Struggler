package com.struggler.world.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Server-authoritative melee behaviour for the Midland Guard.
 *
 * <p>Goal selectors only tick on the logical server, so every state change,
 * navigation request, cooldown and damage call below is decided by the server.
 * The goal never applies damage directly: it asks the entity to advance its
 * strike, and the entity owns the single-impact guard.</p>
 */
public final class GuardCombatGoal extends Goal {
    private final MidlandGuard guard;
    private int repathCooldown;

    public GuardCombatGoal(MidlandGuard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.guard.canFightTarget(this.guard.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        return this.guard.canFightTarget(this.guard.getTarget());
    }

    @Override
    public void start() {
        this.repathCooldown = 0;
    }

    @Override
    public void stop() {
        this.guard.cancelStrike();
        this.guard.stopBlocking();
        this.guard.getNavigation().stop();
        this.repathCooldown = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.guard.getTarget();
        if (!this.guard.canFightTarget(target)) {
            this.guard.setTarget(null);
            return;
        }

        this.guard.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // A strike in progress owns the guard: no movement, no target change.
        if (this.guard.isStrikeInProgress()) {
            this.guard.tickStrike();
            return;
        }

        double reach = GuardCombatTuning.ATTACK_REACH;
        if (this.guard.distanceToSqr(target) > reach * reach) {
            // Chasing means the shield comes down: a guard never blocks and runs.
            this.guard.stopBlocking();
            this.chase(target);
            return;
        }

        this.guard.getNavigation().stop();
        if (this.guard.canStartStrike() && this.guard.getSensing().hasLineOfSight(target)) {
            this.guard.beginStrike(target);
            return;
        }

        // In reach but still on cooldown: hold the shield instead of standing
        // open to a free hit. The window is bounded by BLOCK_HOLD_TICKS.
        //
        // The old code re-tested a 4.0 block range here, but this branch only
        // runs when the guard is already inside ATTACK_REACH (2.4), so that test
        // could never be false. The stance therefore begins exactly at melee
        // reach, which is the behaviour the tests already measured.
        this.guard.startBlocking();
    }

    private void chase(LivingEntity target) {
        if (this.repathCooldown-- <= 0 || this.guard.getNavigation().isDone()) {
            this.repathCooldown = GuardCombatTuning.REPATH_INTERVAL;
            this.guard.getNavigation().moveTo(target, GuardCombatTuning.CHASE_SPEED);
        }
    }
}
