package com.struggler.world.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the animation priority required by the 0.3.3 refinement.
 *
 * <p>Each test walks a real transition instead of checking one isolated state, so
 * a clip that would get stuck is caught here.</p>
 */
class GuardAnimationPriorityTest {

    private static String clip(boolean dead, boolean attack, boolean hurt, boolean block,
                               boolean preview, String previewClip, boolean moving) {
        return GuardAnimationPriority.resolve(dead, attack, hurt, block, preview, previewClip, moving);
    }

    @Test
    @DisplayName("IDLE -> BLOCKING -> IDLE")
    void idleBlockingIdle() {
        assertEquals("idle", clip(false, false, false, false, false, null, false));
        assertEquals("block", clip(false, false, false, true, false, null, false));
        assertEquals("idle", clip(false, false, false, false, false, null, false));
    }

    @Test
    @DisplayName("CHASE -> BLOCKING -> IDLE")
    void chaseBlockingIdle() {
        assertEquals("walk", clip(false, false, false, false, false, null, true));
        assertEquals("block", clip(false, false, false, true, false, null, true));
        assertEquals("idle", clip(false, false, false, false, false, null, false));
    }

    @Test
    @DisplayName("BLOCKING -> ATTACKING")
    void blockingToAttacking() {
        assertEquals("block", clip(false, false, false, true, false, null, false));
        // The strike window opens and the shield is already lowered by the entity.
        assertEquals("attack", clip(false, true, false, false, false, null, false));
    }

    @Test
    @DisplayName("ATTACKING -> RECOVERY -> IDLE")
    void attackingRecoveryIdle() {
        assertEquals("attack", clip(false, true, false, false, false, null, false));
        // Recovery: the strike is over but the clip window is still open.
        assertEquals("attack", clip(false, true, false, false, false, null, true));
        assertEquals("idle", clip(false, false, false, false, false, null, false));
    }

    @Test
    @DisplayName("BLOCKING -> HURT -> BLOCKING while the shield is still up")
    void blockingHurtBlocking() {
        assertEquals("block", clip(false, false, false, true, false, null, false));
        assertEquals("hurt", clip(false, false, true, true, false, null, false));
        assertEquals("block", clip(false, false, false, true, false, null, false));
    }

    @Test
    @DisplayName("BLOCKING -> HURT -> IDLE when the window closed during the reaction")
    void blockingHurtIdle() {
        assertEquals("hurt", clip(false, false, true, true, false, null, false));
        assertEquals("idle", clip(false, false, false, false, false, null, false));
    }

    @Test
    @DisplayName("death has absolute priority over every other state")
    void deathWins() {
        assertEquals("death", clip(true, true, true, true, true, "run", true));
    }

    @Test
    @DisplayName("the strike window outranks hurt so a hit never cancels a swing")
    void attackOutranksHurt() {
        assertEquals("attack", clip(false, true, true, false, false, null, false));
    }

    @Test
    @DisplayName("the QA preview never outranks blocking or combat")
    void previewLosesToRealStates() {
        assertEquals("run", clip(false, false, false, false, true, "run", true));
        assertEquals("block", clip(false, false, false, true, true, "run", true));
        assertEquals("attack", clip(false, true, false, false, true, "run", true));
    }

    @Test
    @DisplayName("an expired or empty preview falls back to locomotion")
    void previewFallback() {
        assertEquals("idle", clip(false, false, false, false, true, "", false));
        assertEquals("walk", clip(false, false, false, false, true, null, true));
    }
}
