package com.struggler.world.entity;

/**
 * Pure defense math for the Midland Guard (version 0.3.2).
 *
 * <p>This class deliberately contains no Minecraft types: the cone test and the
 * damage reduction are the two rules that decide whether a hit is blocked, so
 * they are kept as plain arithmetic and covered by unit tests. The entity only
 * feeds positions and damage values in, which keeps a single place responsible
 * for the numbers and makes duplicated reduction impossible to hide.</p>
 */
public final class GuardDefenseRules {
    /** Below this length a direction vector carries no usable heading. */
    private static final double MIN_DIRECTION_LENGTH = 1.0E-4D;
    /** Two entities closer than this on the horizontal plane count as frontal. */
    public static final double CONTACT_DISTANCE = 0.1D;

    private GuardDefenseRules() {
    }

    /**
     * True when the attacker lies inside the frontal arc of the guard.
     *
     * @param lookX        horizontal X of the guard's look direction
     * @param lookZ        horizontal Z of the guard's look direction
     * @param toAttackerX  horizontal X from the guard to the attacker
     * @param toAttackerZ  horizontal Z from the guard to the attacker
     */
    public static boolean isInsideFrontalCone(double lookX, double lookZ,
                                              double toAttackerX, double toAttackerZ) {
        double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double targetLength = Math.sqrt(toAttackerX * toAttackerX + toAttackerZ * toAttackerZ);
        if (lookLength < MIN_DIRECTION_LENGTH) {
            return false;
        }
        if (targetLength < MIN_DIRECTION_LENGTH) {
            // Touching the guard: there is no meaningful bearing, so it is not a backstab.
            return true;
        }
        double dot = (lookX * toAttackerX + lookZ * toAttackerZ) / (lookLength * targetLength);
        double cosHalfAngle = Math.cos(Math.toRadians(GuardCombatTuning.BLOCK_HALF_ANGLE_DEGREES));
        return dot >= cosHalfAngle;
    }

    /**
     * Applies the shield reduction exactly once.
     *
     * <p>The result never drops below {@link GuardCombatTuning#BLOCK_MIN_DAMAGE}
     * (capped at the incoming value), so a blocked hit always hurts at least a
     * little and the guard can never become invulnerable.</p>
     */
    public static float reducedDamage(float incoming) {
        if (incoming <= 0.0F) {
            return 0.0F;
        }
        float reduced = incoming * (1.0F - (float) GuardCombatTuning.BLOCK_DAMAGE_REDUCTION);
        float floor = Math.min(incoming, (float) GuardCombatTuning.BLOCK_MIN_DAMAGE);
        return Math.max(reduced, floor);
    }

    /** Damage removed by the shield, used only for the QA diagnostics counter. */
    public static float preventedDamage(float incoming) {
        return Math.max(0.0F, incoming - reducedDamage(incoming));
    }
}
