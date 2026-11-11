package com.struggler.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the tick math that guarantees one damage event per strike. */
class GuardCombatTimelineTest {
    private final GuardCombatTimeline timeline = new GuardCombatTimeline(8, 2, 8);

    @Test
    void impactOccursExactlyOncePerStrike() {
        int impacts = 0;
        for (int tick = 0; tick < timeline.totalTicks(); tick++) {
            if (timeline.isImpactTick(tick)) {
                impacts++;
            }
        }
        assertEquals(1, impacts, "a strike must expose exactly one impact tick");
        assertFalse(timeline.isImpactTick(timeline.totalTicks()), "no impact after the strike ends");
        assertFalse(timeline.isImpactTick(-1), "no impact before the strike starts");
    }

    @Test
    void phasesFollowTheDocumentedOrder() {
        assertEquals(GuardCombatPhase.WINDUP, timeline.phaseAt(0));
        assertEquals(GuardCombatPhase.WINDUP, timeline.phaseAt(7));
        assertEquals(GuardCombatPhase.ATTACKING, timeline.phaseAt(8));
        assertEquals(GuardCombatPhase.ATTACKING, timeline.phaseAt(9));
        assertEquals(GuardCombatPhase.RECOVERY, timeline.phaseAt(10));
        assertEquals(GuardCombatPhase.RECOVERY, timeline.phaseAt(17));
        assertEquals(GuardCombatPhase.IDLE, timeline.phaseAt(18));
    }

    @Test
    void impactTickIsTheFirstAttackingTick() {
        assertEquals(8, timeline.impactTick());
        assertEquals(GuardCombatPhase.ATTACKING, timeline.phaseAt(timeline.impactTick()));
    }

    @Test
    void strikeCycleIsShorterThanTheServerCooldown() {
        assertTrue(timeline.totalTicks() < GuardCombatTuning.COOLDOWN_TICKS,
                "the animation must not define the damage rate");
    }

    @Test
    void tuningKeepsTheDocumentedValues() {
        assertEquals(4.0D, GuardCombatTuning.BASE_ATTACK_DAMAGE);
        assertEquals(0.0D, GuardCombatTuning.BASE_ATTACK_KNOCKBACK);
        assertTrue(GuardCombatTuning.IMPACT_REACH >= GuardCombatTuning.ATTACK_REACH);
        assertTrue(GuardCombatTuning.PURSUIT_RANGE > GuardCombatTuning.ATTACK_REACH);
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GuardCombatTimeline(0, 2, 8));
        assertThrows(IllegalArgumentException.class, () -> new GuardCombatTimeline(8, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> new GuardCombatTimeline(8, 2, -1));
        assertThrows(IllegalArgumentException.class, () -> timeline.phaseAt(-1));
    }
}
