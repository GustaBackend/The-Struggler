package com.struggler.gametest;

import com.struggler.TheStruggler;
import com.struggler.registry.ModEntities;
import com.struggler.world.entity.GuardCombatPhase;
import com.struggler.world.entity.GuardCombatTuning;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.function.BooleanSupplier;

/**
 * Server-side GameTests for the shield defense added in 0.3.2.
 *
 * <p>Every test runs inside a real Minecraft server level. Damage is applied
 * through the same {@code hurt} path the game uses, so the numbers below are the
 * numbers a player would see; nothing here calls the reduction directly.</p>
 */
@GameTestHolder(TheStruggler.MOD_ID)
public final class GuardDefenseGameTests {
    private static final String ARENA = "combat_arena";
    private static final float TOLERANCE = 0.01F;

    private GuardDefenseGameTests() {
    }

    /** Requirement 1: no shield, no reduction. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDefenseOpen")
    public static void guardTakesFullDamageWithoutBlocking(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Wolf wolf = helper.spawn(EntityType.WOLF, 4, 1, 6);
        wolf.setNoAi(true);
        guard.setTarget(null);
        guard.stopBlocking();
        faceNorth(guard);

        float before = guard.getHealth();
        guard.hurt(guard.damageSources().mobAttack(wolf), 6.0F);

        helper.runAfterDelay(2, () -> {
            float taken = before - guard.getHealth();
            if (Math.abs(taken - 6.0F) > TOLERANCE) {
                helper.fail("guard without a shield took " + taken + " instead of 6.0");
                return;
            }
            if (guard.getBlockedHits() != 0) {
                helper.fail("a hit was counted as blocked while no shield was up");
                return;
            }
            helper.succeed();
        });
    }

    /** Requirement 2 and 5: a frontal hit is reduced once, by exactly 40%. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDefenseFrontal")
    public static void guardBlocksFrontalDamage(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Wolf wolf = helper.spawn(EntityType.WOLF, 4, 1, 6);
        wolf.setNoAi(true);
        guard.setTarget(null);
        faceNorth(guard);
        guard.startBlocking();

        if (!guard.isShieldRaised()) {
            helper.fail("guard refused to raise the shield");
            return;
        }

        float before = guard.getHealth();
        boolean hurtResult = guard.hurt(guard.damageSources().mobAttack(wolf), 10.0F);

        helper.runAfterDelay(2, () -> {
            float taken = before - guard.getHealth();
            if (Math.abs(taken - 6.0F) > TOLERANCE) {
                helper.fail("blocked hit applied " + taken
                        + " instead of 6.0 (10.0 with 40% off; 3.6 would mean a double reduction)"
                        + " [hurt=" + hurtResult + " invulnerableTime=" + guard.invulnerableTime
                        + " hurtTime=" + guard.hurtTime + " blocking=" + guard.isShieldRaised()
                        + " health=" + guard.getHealth() + "]");
                return;
            }
            if (guard.getBlockedHits() != 1) {
                helper.fail("expected exactly one blocked hit, got " + guard.getBlockedHits());
                return;
            }
            if (Math.abs(guard.getDamagePrevented() - 4.0F) > TOLERANCE) {
                helper.fail("prevented damage was " + guard.getDamagePrevented() + " instead of 4.0");
                return;
            }
            helper.succeed();
        });
    }

    /** Requirement 3: the shield only covers the front arc. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDefenseBackstab")
    public static void guardDoesNotBlockFromBehind(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Wolf wolf = helper.spawn(EntityType.WOLF, 4, 1, 2);
        wolf.setNoAi(true);
        guard.setTarget(null);
        faceNorth(guard);
        guard.startBlocking();

        float before = guard.getHealth();
        guard.hurt(guard.damageSources().mobAttack(wolf), 10.0F);

        helper.runAfterDelay(2, () -> {
            float taken = before - guard.getHealth();
            if (Math.abs(taken - 10.0F) > TOLERANCE) {
                helper.fail("a hit from behind was reduced: took " + taken + " instead of 10.0");
                return;
            }
            if (guard.getBlockedHits() != 0) {
                helper.fail("a hit from behind was counted as blocked");
                return;
            }
            helper.succeed();
        });
    }

    /** Requirement 4: blocking never makes the guard invulnerable. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDefenseNotInvulnerable")
    public static void guardIsNeverInvulnerableWhileBlocking(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Wolf wolf = helper.spawn(EntityType.WOLF, 4, 1, 6);
        wolf.setNoAi(true);
        guard.setTarget(null);
        faceNorth(guard);
        guard.startBlocking();

        float before = guard.getHealth();
        guard.hurt(guard.damageSources().mobAttack(wolf), 30.0F);

        helper.runAfterDelay(2, () -> {
            float taken = before - guard.getHealth();
            if (taken < 1.0F) {
                helper.fail("blocking made the guard nearly invulnerable: took " + taken);
                return;
            }
            if (Math.abs(taken - 18.0F) > TOLERANCE) {
                helper.fail("expected 18.0 damage from 30.0 while blocking, got " + taken);
                return;
            }
            if (!guard.isAlive()) {
                helper.fail("guard died to a hit that should have been survivable");
                return;
            }
            helper.succeed();
        });
    }

    /** Requirement 6: the defensive window closes on its own. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDefenseTimeout")
    public static void blockingEndsAfterItsWindow(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        guard.setTarget(null);
        guard.startBlocking();

        if (!guard.isShieldRaised()) {
            helper.fail("guard refused to raise the shield");
            return;
        }

        helper.runAfterDelay(GuardCombatTuning.BLOCK_HOLD_TICKS + 10, () -> {
            if (guard.isShieldRaised()) {
                helper.fail("shield stayed up after the defensive window expired");
                return;
            }
            helper.succeed();
        });
    }

    /**
     * Requirement 7 and 9: the guard blocks between strikes on its own, the
     * block is cleared while it attacks, and it returns to combat afterwards.
     */
    @GameTest(template = ARENA, timeoutTicks = 400, batch = "guardDefenseCycle")
    public static void guardBlocksBetweenStrikesAndAttacksAgain(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 3);
        Husk husk = helper.spawn(EntityType.HUSK, 4, 1, 5);
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(400.0D);
        husk.setHealth(400.0F);

        poll(helper, guard::isShieldRaised, 30, 5, () -> {
            if (guard.getStrikeCount() < 1) {
                helper.fail("guard raised the shield without ever striking");
                return;
            }
            if (guard.isStrikeInProgress()) {
                helper.fail("guard was blocking in the middle of a strike");
                return;
            }
            poll(helper, () -> guard.getStrikeCount() >= 2, 40, 5,
                    helper::succeed,
                    "guard never attacked again after defending (strikes="
                            + guard.getStrikeCount() + ")");
        }, "guard never entered the defensive state between strikes");
    }

    /** Requirement 8: retaliation still records the player who attacked. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDefenseRetaliation")
    public static void retaliationStillWorksWhileDefending(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Player player = helper.makeMockPlayer();
        player.moveTo(guard.getX(), guard.getY(), guard.getZ() + 2.0D, 0.0F, 0.0F);
        guard.setTarget(null);
        faceNorth(guard);
        guard.startBlocking();

        float before = guard.getHealth();
        guard.hurt(guard.damageSources().playerAttack(player), 10.0F);

        helper.runAfterDelay(5, () -> {
            if (guard.getLastHurtByMob() != player) {
                helper.fail("guard did not record the attacking player while defending");
                return;
            }
            if (!guard.canFightTarget(player)) {
                helper.fail("guard refuses to fight the player that attacked it");
                return;
            }
            float taken = before - guard.getHealth();
            if (Math.abs(taken - 6.0F) > TOLERANCE) {
                helper.fail("player hit while blocking applied " + taken + " instead of 6.0");
                return;
            }
            helper.succeed();
        });
    }

    /** Requirement 9: attack and defense are mutually exclusive. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDefenseExclusive")
    public static void attackAndDefenseAreMutuallyExclusive(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Husk husk = helper.spawn(EntityType.HUSK, 4, 1, 5);
        husk.setNoAi(true);
        husk.setPersistenceRequired();

        guard.setTarget(husk);
        guard.startBlocking();
        if (!guard.isShieldRaised()) {
            helper.fail("guard refused to raise the shield before the strike");
            return;
        }

        guard.beginStrike(husk);
        if (guard.isShieldRaised()) {
            helper.fail("shield stayed up while the guard started a strike");
            return;
        }
        if (guard.getCombatPhase() == GuardCombatPhase.IDLE) {
            helper.fail("strike did not start");
            return;
        }

        // Raising the shield during a strike must be refused.
        guard.startBlocking();
        if (guard.isShieldRaised()) {
            helper.fail("guard blocked in the middle of a strike");
            return;
        }
        helper.succeed();
    }

    private static void faceNorth(MidlandGuard guard) {
        guard.setYRot(0.0F);
        guard.setYHeadRot(0.0F);
    }

    /**
     * Repeats a check every {@code interval} ticks so timing-sensitive states
     * (a block window, a cooldown) do not depend on one lucky tick.
     */
    private static void poll(GameTestHelper helper, BooleanSupplier condition, int attempts,
                             int interval, Runnable onFound, String failureMessage) {
        if (condition.getAsBoolean()) {
            onFound.run();
            return;
        }
        if (attempts <= 0) {
            helper.fail(failureMessage);
            return;
        }
        helper.runAfterDelay(interval, () -> poll(helper, condition, attempts - 1, interval,
                onFound, failureMessage));
    }
}
