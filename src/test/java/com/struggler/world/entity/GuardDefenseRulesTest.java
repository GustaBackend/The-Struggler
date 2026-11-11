package com.struggler.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the two rules that decide a blocked hit: the frontal arc and
 * the single damage reduction. No Minecraft classes are involved.
 */
final class GuardDefenseRulesTest {
    private static final double TOLERANCE = 1.0E-4D;

    @Test
    void attackerDirectlyInFrontIsInsideTheCone() {
        assertTrue(GuardDefenseRules.isInsideFrontalCone(0.0D, 1.0D, 0.0D, 2.0D));
    }

    @Test
    void attackerAtTheEdgeOfTheConeIsStillCovered() {
        double radians = Math.toRadians(59.0D);
        assertTrue(GuardDefenseRules.isInsideFrontalCone(0.0D, 1.0D,
                Math.sin(radians) * 3.0D, Math.cos(radians) * 3.0D));
    }

    @Test
    void attackerOutsideTheConeIsNotCovered() {
        double radians = Math.toRadians(61.0D);
        assertFalse(GuardDefenseRules.isInsideFrontalCone(0.0D, 1.0D,
                Math.sin(radians) * 3.0D, Math.cos(radians) * 3.0D));
    }

    @Test
    void attackerBehindIsNotCovered() {
        assertFalse(GuardDefenseRules.isInsideFrontalCone(0.0D, 1.0D, 0.0D, -2.0D));
    }

    @Test
    void attackerAtContactRangeCountsAsFrontal() {
        assertTrue(GuardDefenseRules.isInsideFrontalCone(0.0D, 1.0D, 0.0D, 0.0D));
    }

    @Test
    void guardWithoutLookDirectionBlocksNothing() {
        assertFalse(GuardDefenseRules.isInsideFrontalCone(0.0D, 0.0D, 0.0D, 2.0D));
    }

    @Test
    void frontalDamageIsReducedByFortyPercent() {
        assertEquals(3.6D, GuardDefenseRules.reducedDamage(6.0F), TOLERANCE);
        assertEquals(6.0D, GuardDefenseRules.reducedDamage(10.0F), TOLERANCE);
    }

    @Test
    void blockedHitKeepsTheMinimumDamageFloor() {
        assertEquals(1.0D, GuardDefenseRules.reducedDamage(1.0F), TOLERANCE);
        assertEquals(0.5D, GuardDefenseRules.reducedDamage(0.5F), TOLERANCE);
    }

    @Test
    void blockingNeverMakesTheGuardInvulnerable() {
        for (float amount = 0.5F; amount <= 40.0F; amount += 0.5F) {
            float reduced = GuardDefenseRules.reducedDamage(amount);
            assertTrue(reduced > 0.0F, "a blocked hit must still hurt: " + amount);
            assertTrue(reduced >= Math.min(amount, 1.0F) - TOLERANCE,
                    "reduction went below the floor: " + amount);
        }
    }

    @Test
    void preventedDamageMatchesTheDifference() {
        assertEquals(2.4D, GuardDefenseRules.preventedDamage(6.0F), TOLERANCE);
        assertEquals(0.0D, GuardDefenseRules.preventedDamage(0.0F), TOLERANCE);
    }
}
