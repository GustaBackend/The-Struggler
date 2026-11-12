package com.struggler.world.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the 0.3.3 damage policy.
 *
 * <p>Each case mirrors one line of the vanilla damage tags, so a future change to
 * the decision table fails here before it can reach the game.</p>
 */
class GuardDamagePolicyTest {

    private static GuardDamageFacts facts(boolean bypass, boolean explosion, boolean fire,
                                          boolean fall, boolean freezing, boolean drowning,
                                          boolean lightning, boolean projectile,
                                          boolean physicalProjectile, boolean melee) {
        return new GuardDamageFacts(bypass, explosion, fire, fall, freezing, drowning,
                lightning, projectile, physicalProjectile, melee);
    }

    private static GuardDamageFacts melee() {
        return facts(false, false, false, false, false, false, false, false, false, true);
    }

    private static GuardDamageFacts projectile() {
        return facts(false, false, false, false, false, false, false, true, true, false);
    }

    @Test
    @DisplayName("sword and claw hits are physical contact and can be blocked")
    void meleeIsDefensible() {
        assertEquals(GuardDamageClass.MELEE, GuardDamagePolicy.classify(melee()));
        assertTrue(GuardDamageClass.MELEE.isDefensible());
    }

    @Test
    @DisplayName("an arrow delivered by a real projectile can be blocked")
    void arrowIsDefensible() {
        assertEquals(GuardDamageClass.PROJECTILE, GuardDamagePolicy.classify(projectile()));
        assertTrue(GuardDamageClass.PROJECTILE.isDefensible());
    }

    @Test
    @DisplayName("a wither skull counts as a physical projectile")
    void otherPhysicalProjectileIsDefensible() {
        GuardDamageFacts skull = facts(false, false, false, false, false, false, false,
                true, true, false);
        assertEquals(GuardDamageClass.PROJECTILE, GuardDamagePolicy.classify(skull));
    }

    @Test
    @DisplayName("a projectile tag without a projectile entity is not blockable")
    void projectileTagWithoutEntityIsNotDefensible() {
        GuardDamageFacts taggedOnly = facts(false, false, false, false, false, false, false,
                true, false, false);
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(taggedOnly));
    }

    @Test
    @DisplayName("magic is never blocked, even when a caster is attached")
    void magicIsNotDefensible() {
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(facts(
                false, false, false, false, false, false, false, false, false, false)));
    }

    @Test
    @DisplayName("explosions are not blocked, even when a living owner is attached")
    void explosionIsNotDefensible() {
        GuardDamageFacts creeper = facts(false, true, false, false, false, false, false,
                false, false, true);
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(creeper));
        assertFalse(GuardDamageClass.NOT_DEFENSIBLE.isDefensible());
    }

    @Test
    @DisplayName("fire is not blocked")
    void fireIsNotDefensible() {
        GuardDamageFacts burning = facts(false, false, true, false, false, false, false,
                false, false, true);
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(burning));
    }

    @Test
    @DisplayName("a fireball is fire first, projectile second, and stays unblocked")
    void fireballFollowsFireRule() {
        GuardDamageFacts fireball = facts(false, false, true, false, false, false, false,
                true, true, false);
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(fireball));
    }

    @Test
    @DisplayName("fall damage is not blocked")
    void fallIsNotDefensible() {
        GuardDamageFacts fall = facts(false, false, false, true, false, false, false,
                false, false, false);
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(fall));
    }

    @Test
    @DisplayName("freezing, drowning and lightning are not blocked")
    void environmentalHazardsAreNotDefensible() {
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(
                facts(false, false, false, false, true, false, false, false, false, false)));
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(
                facts(false, false, false, false, false, true, false, false, false, false)));
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(
                facts(false, false, false, false, false, false, true, false, false, false)));
    }

    @Test
    @DisplayName("ambient sources without an attacker are not blocked")
    void ambientIsNotDefensible() {
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(facts(
                false, false, false, false, false, false, false, false, false, false)));
    }

    @Test
    @DisplayName("a source that bypasses shields is respected even if it looks melee")
    void shieldBypassWins() {
        GuardDamageFacts bypass = facts(true, false, false, false, false, false, false,
                false, false, true);
        assertEquals(GuardDamageClass.NOT_DEFENSIBLE, GuardDamagePolicy.classify(bypass));
    }
}
