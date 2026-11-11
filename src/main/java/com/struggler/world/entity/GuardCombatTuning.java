package com.struggler.world.entity;

/**
 * Tunable combat values for the Midland Guard (version 0.3.1).
 *
 * <p>Damage is deliberately modest: 4.0 (2 hearts) per landed strike with a
 * 30-tick server cooldown, so a guard deals at most ~2.7 damage per second.
 * The strike cycle (8 windup + 2 strike + 8 recovery = 18 ticks) is shorter
 * than the cooldown, which keeps the visual animation from defining the
 * damage rate. Adjust these constants rather than the goal logic.</p>
 */
public final class GuardCombatTuning {
    /** Base melee damage in half-hearts; 4.0 = two hearts. */
    public static final double BASE_ATTACK_DAMAGE = 4.0D;
    /** Vanilla knockback attribute; 0.0 keeps the strike from launching targets. */
    public static final double BASE_ATTACK_KNOCKBACK = 0.0D;

    /** Distance at which a new strike may start, measured center to center. */
    public static final double ATTACK_REACH = 2.4D;
    /** More generous distance allowed at the impact tick so a step back is forgiven. */
    public static final double IMPACT_REACH = 3.2D;
    /** Maximum distance at which an existing target is still pursued. */
    public static final double PURSUIT_RANGE = 32.0D;

    public static final int WINDUP_TICKS = 8;
    public static final int STRIKE_TICKS = 2;
    public static final int RECOVERY_TICKS = 8;
    /** Server-authoritative delay between two strikes. */
    public static final int COOLDOWN_TICKS = 30;
    /** Path recomputation interval while chasing, to avoid stop/start stutter. */
    public static final int REPATH_INTERVAL = 10;
    /** Length of the authored attack clip, used only to keep the visual coherent. */
    public static final int ATTACK_CLIP_TICKS = 32;
    /** Navigation speed modifier; 1.0 keeps the 0.23 movement attribute as-is. */
    public static final double CHASE_SPEED = 1.0D;

    private GuardCombatTuning() {
    }
}
