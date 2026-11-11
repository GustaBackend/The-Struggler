package com.struggler.world.entity;

/**
 * Tick math for exactly one strike. Kept free of Minecraft types so the
 * single-impact guarantee can be unit-tested on a plain JVM.
 *
 * <p>Timeline for elapsed tick {@code t} (0 = first tick of the windup):
 * {@code [0, windup)} WINDUP, {@code [windup, windup + strike)} ATTACKING,
 * {@code [windup + strike, total)} RECOVERY, then IDLE. The impact tick is
 * {@code windup} and occurs exactly once per strike.</p>
 */
public final class GuardCombatTimeline {
    private final int windupTicks;
    private final int strikeTicks;
    private final int recoveryTicks;

    public GuardCombatTimeline(int windupTicks, int strikeTicks, int recoveryTicks) {
        if (windupTicks < 1) {
            throw new IllegalArgumentException("windupTicks must be at least 1");
        }
        if (strikeTicks < 1) {
            throw new IllegalArgumentException("strikeTicks must be at least 1");
        }
        if (recoveryTicks < 0) {
            throw new IllegalArgumentException("recoveryTicks must not be negative");
        }
        this.windupTicks = windupTicks;
        this.strikeTicks = strikeTicks;
        this.recoveryTicks = recoveryTicks;
    }

    public int windupTicks() {
        return this.windupTicks;
    }

    public int strikeTicks() {
        return this.strikeTicks;
    }

    public int recoveryTicks() {
        return this.recoveryTicks;
    }

    public int impactTick() {
        return this.windupTicks;
    }

    public int totalTicks() {
        return this.windupTicks + this.strikeTicks + this.recoveryTicks;
    }

    public boolean isImpactTick(int elapsedTicks) {
        return elapsedTicks == this.windupTicks;
    }

    public GuardCombatPhase phaseAt(int elapsedTicks) {
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks must not be negative");
        }
        if (elapsedTicks < this.windupTicks) {
            return GuardCombatPhase.WINDUP;
        }
        if (elapsedTicks < this.windupTicks + this.strikeTicks) {
            return GuardCombatPhase.ATTACKING;
        }
        if (elapsedTicks < this.totalTicks()) {
            return GuardCombatPhase.RECOVERY;
        }
        return GuardCombatPhase.IDLE;
    }
}
