package com.struggler.world.entity;

/**
 * Single source of truth for which clip owns the Midland Guard model (0.3.3).
 *
 * <p>The order used to live inside the GeckoLib controller, where it could only
 * be observed by watching the game. It is now a pure function so every
 * transition required by the refinement (idle to blocking, blocking to attack,
 * blocking through hurt and back) can be asserted in a unit test.</p>
 *
 * <p>Order: death, real strike window, hurt, shield, QA preview, locomotion.
 * Death is absolute. The strike window outranks hurt because taking a hit must
 * not cancel the swing the server is already resolving. Hurt outranks the shield
 * only for the ten ticks of {@code hurtTime}; the shield returns by itself while
 * the defensive state is still active, so a damage reaction can never hide the
 * shield permanently.</p>
 */
public final class GuardAnimationPriority {
    public static final String DEATH = "death";
    public static final String ATTACK = "attack";
    public static final String HURT = "hurt";
    public static final String BLOCK = "block";
    public static final String WALK = "walk";
    public static final String IDLE = "idle";

    private GuardAnimationPriority() {
    }

    /**
     * Resolves the clip name for the current state.
     *
     * @param dead          death animation owns the model
     * @param attackActive  the synchronized strike window is open
     * @param hurt          the vanilla hurt reaction is still running
     * @param blocking      the shield is effectively raised on the server
     * @param previewActive a QA preview window is open
     * @param previewClip   clip requested by the QA preview
     * @param moving        the entity is moving
     */
    public static String resolve(boolean dead, boolean attackActive, boolean hurt,
                                 boolean blocking, boolean previewActive, String previewClip,
                                 boolean moving) {
        if (dead) {
            return DEATH;
        }
        if (attackActive) {
            return ATTACK;
        }
        if (hurt) {
            return HURT;
        }
        if (blocking) {
            return BLOCK;
        }
        if (previewActive && previewClip != null && !previewClip.isEmpty()) {
            return previewClip;
        }
        return moving ? WALK : IDLE;
    }
}
