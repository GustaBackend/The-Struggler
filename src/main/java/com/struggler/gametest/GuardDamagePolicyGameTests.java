package com.struggler.gametest;

import com.struggler.TheStruggler;
import com.struggler.registry.ModEntities;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Server-side GameTests for the 0.3.3 damage classification.
 *
 * <p>Every case builds a real {@link DamageSource} through the vanilla factory
 * methods and pushes it through the same {@code hurt} path a player would, so a
 * wrong classification shows up as a wrong health delta inside a running server
 * level. Nothing here calls the policy or the reduction directly.</p>
 */
@GameTestHolder(TheStruggler.MOD_ID)
public final class GuardDamagePolicyGameTests {
    private static final String ARENA = "combat_arena";
    private static final float TOLERANCE = 0.01F;
    private static final float HIT = 10.0F;
    private static final float BLOCKED = 6.0F;

    private GuardDamagePolicyGameTests() {
    }

    /** A sword hit from the front is physical contact and is reduced once. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageSword")
    public static void swordFrontalIsReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        Player player = helper.makeMockPlayer();
        player.moveTo(guard.getX(), guard.getY(), guard.getZ() + 2.0D, 0.0F, 0.0F);

        expectDamage(helper, guard, guard.damageSources().playerAttack(player), BLOCKED, 1,
                "sword hit from the front");
    }

    /** An arrow fired from the front is a physical projectile and is reduced once. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageArrow")
    public static void arrowFrontalIsReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        Wolf shooter = helper.spawn(EntityType.WOLF, 4, 1, 6);
        shooter.setNoAi(true);
        Arrow arrow = helper.spawn(EntityType.ARROW, 4, 1, 5);

        expectDamage(helper, guard, guard.damageSources().arrow(arrow, shooter), BLOCKED, 1,
                "arrow from the front");
    }

    /** A non-arrow mob projectile from the front follows the same rule. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageMobProjectile")
    public static void mobProjectileFrontalIsReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        Wolf shooter = helper.spawn(EntityType.WOLF, 4, 1, 6);
        shooter.setNoAi(true);
        Arrow projectile = helper.spawn(EntityType.ARROW, 4, 1, 5);

        expectDamage(helper, guard, guard.damageSources().mobProjectile(projectile, shooter),
                BLOCKED, 1, "mob projectile from the front");
    }

    /** Magic is not physical and must never be reduced. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageMagic")
    public static void magicIsNotReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        expectDamage(helper, guard, guard.damageSources().magic(), HIT, 0, "magic");
    }

    /** An explosion with a living owner is still an explosion. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageExplosion")
    public static void explosionIsNotReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        Husk owner = helper.spawn(EntityType.HUSK, 4, 1, 5);
        owner.setNoAi(true);
        expectDamage(helper, guard, guard.damageSources().explosion(owner, owner), HIT, 0,
                "explosion owned by a husk");
    }

    /** Fire is not blocked, even though the guard is holding a shield. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageFire")
    public static void fireIsNotReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        expectDamage(helper, guard, guard.damageSources().onFire(), HIT, 0, "fire");
    }

    /** Fall damage is not blocked. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageFall")
    public static void fallIsNotReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        expectDamage(helper, guard, guard.damageSources().fall(), HIT, 0, "fall");
    }

    /** Ambient damage without an attacker is not a physical attack. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageAmbient")
    public static void ambientIsNotReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        expectDamage(helper, guard, guard.damageSources().cactus(), HIT, 0, "ambient cactus");
    }

    /** A projectile from behind is physical but outside the arc. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageArrowBack")
    public static void arrowFromBehindIsNotReduced(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        Wolf shooter = helper.spawn(EntityType.WOLF, 4, 1, 2);
        shooter.setNoAi(true);
        Arrow arrow = helper.spawn(EntityType.ARROW, 4, 1, 3);

        expectDamage(helper, guard, guard.damageSources().arrow(arrow, shooter), HIT, 0,
                "arrow from behind");
    }

    /** A spell from the front must not raise a shield that was down. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageNoAutoRaise")
    public static void nonDefensibleHitDoesNotRaiseShield(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Wolf wolf = helper.spawn(EntityType.WOLF, 4, 1, 6);
        wolf.setNoAi(true);
        guard.setTarget(null);
        faceNorth(guard);
        guard.stopBlocking();

        guard.hurt(guard.damageSources().magic(), 4.0F);

        helper.runAfterDelay(2, () -> {
            if (guard.isShieldRaised()) {
                helper.fail("a magic hit raised the physical shield");
                return;
            }
            helper.succeed();
        });
    }

    /** Death must never leave the synced shield flag stuck on. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardDamageDeathClears")
    public static void shieldDropsOnDeath(GameTestHelper helper) {
        MidlandGuard guard = blockingGuard(helper);
        if (!guard.isShieldRaised()) {
            helper.fail("guard refused to raise the shield before the lethal hit");
            return;
        }
        guard.hurt(guard.damageSources().genericKill(), 1000.0F);

        helper.runAfterDelay(2, () -> {
            if (guard.isShieldRaised()) {
                helper.fail("the shield stayed raised after death");
                return;
            }
            helper.succeed();
        });
    }

    private static MidlandGuard blockingGuard(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        guard.setTarget(null);
        faceNorth(guard);
        guard.startBlocking();
        return guard;
    }

    private static void expectDamage(GameTestHelper helper, MidlandGuard guard, DamageSource source,
                                     float expected, int expectedBlockedHits, String label) {
        if (!guard.isShieldRaised()) {
            helper.fail("guard refused to raise the shield before " + label);
            return;
        }
        float before = guard.getHealth();
        guard.hurt(source, HIT);

        helper.runAfterDelay(2, () -> {
            float taken = before - guard.getHealth();
            if (Math.abs(taken - expected) > TOLERANCE) {
                helper.fail(label + " applied " + taken + " instead of " + expected);
                return;
            }
            if (guard.getBlockedHits() != expectedBlockedHits) {
                helper.fail(label + " counted " + guard.getBlockedHits()
                        + " blocked hits instead of " + expectedBlockedHits);
                return;
            }
            helper.succeed();
        });
    }

    private static void faceNorth(MidlandGuard guard) {
        guard.setYRot(0.0F);
        guard.setYHeadRot(0.0F);
    }
}
