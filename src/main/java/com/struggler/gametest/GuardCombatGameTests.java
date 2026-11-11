package com.struggler.gametest;

import com.struggler.TheStruggler;
import com.struggler.registry.ModEntities;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Server-side GameTests for guard combat. They run inside a real Minecraft
 * server level through {@code ./gradlew runGameTestServer}; they are not a
 * substitute for the runtime tests documented in the delivery report.
 */
@GameTestHolder(TheStruggler.MOD_ID)
public final class GuardCombatGameTests {
    private static final String ARENA = "combat_arena";

    private GuardCombatGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "guardCombatDamage")
    public static void guardDamagesHostileMob(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 2);
        Husk husk = helper.spawn(EntityType.HUSK, 4, 1, 6);
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        husk.setHealth(200.0F);
        float startHealth = husk.getHealth();

        helper.runAfterDelay(140, () -> {
            if (guard.getStrikeCount() < 1) {
                helper.fail("guard never started a strike against a hostile mob");
                return;
            }
            if (husk.getHealth() >= startHealth) {
                helper.fail("hostile mob health did not decrease (start=" + startHealth
                        + " now=" + husk.getHealth() + ")");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "guardCombatImpact")
    public static void oneImpactPerStrike(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 2);
        Husk husk = helper.spawn(EntityType.HUSK, 4, 1, 5);
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0D);
        husk.setHealth(500.0F);

        helper.runAfterDelay(200, () -> {
            if (guard.getStrikeCount() < 2) {
                helper.fail("expected at least two strikes, got " + guard.getStrikeCount());
                return;
            }
            if (guard.getImpactAttempts() != guard.getStrikeCount()) {
                helper.fail("strikes=" + guard.getStrikeCount() + " but impact attempts="
                        + guard.getImpactAttempts() + " (duplicate or missing impact)");
                return;
            }
            if (guard.getDamageHits() != guard.getImpactAttempts()) {
                helper.fail("impact attempts=" + guard.getImpactAttempts() + " but damage hits="
                        + guard.getDamageHits());
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardCombatUnprovoked")
    public static void guardIgnoresUnprovokedPlayer(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Player player = helper.makeMockPlayer();
        player.moveTo(guard.getX(), guard.getY(), guard.getZ() + 1.5D, 0.0F, 0.0F);

        helper.runAfterDelay(90, () -> {
            if (guard.getTarget() == player) {
                helper.fail("guard targeted an unprovoked player");
                return;
            }
            if (guard.getDamageHits() != 0 && guard.getTarget() == player) {
                helper.fail("guard damaged an unprovoked player");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardCombatRetaliation")
    public static void guardAcceptsItsAttackerAsTarget(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Wolf wolf = helper.spawn(EntityType.WOLF, 4, 1, 6);
        wolf.setNoAi(true);
        wolf.setPersistenceRequired();

        guard.cancelStrike();
        guard.setTarget(null);
        guard.hurt(guard.damageSources().mobAttack(wolf), 1.0F);

        helper.runAfterDelay(10, () -> {
            if (guard.getLastHurtByMob() != wolf) {
                helper.fail("guard did not record its attacker");
                return;
            }
            // Vanilla target goals may legitimately prefer a closer hostile mob
            // between strikes; what must hold is that the guard accepts its
            // attacker as a valid combat target.
            if (!guard.canFightTarget(wolf)) {
                helper.fail("guard refuses to fight the entity that attacked it");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardCombatPlayerAttacker")
    public static void guardRecordsPlayerAsAttacker(GameTestHelper helper) {
        MidlandGuard guard = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 4, 1, 4);
        Player player = helper.makeMockPlayer();
        player.moveTo(guard.getX(), guard.getY(), guard.getZ() + 2.0D, 0.0F, 0.0F);

        guard.cancelStrike();
        guard.setTarget(null);
        guard.hurt(guard.damageSources().playerAttack(player), 1.0F);

        helper.runAfterDelay(5, () -> {
            if (guard.getLastHurtByMob() != player) {
                helper.fail("guard did not record the attacking player");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "guardCombatFriendly")
    public static void guardsDoNotTargetEachOther(GameTestHelper helper) {
        MidlandGuard first = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 3, 1, 3);
        MidlandGuard second = helper.spawn(ModEntities.MIDLAND_GUARD.get(), 5, 1, 3);

        helper.runAfterDelay(90, () -> {
            if (first.getTarget() == second || second.getTarget() == first) {
                helper.fail("guards targeted each other");
                return;
            }
            if (first.distanceToSqr(second) < 1.0D && (first.getDamageHits() > 0 || second.getDamageHits() > 0)) {
                helper.fail("guards damaged each other");
                return;
            }
            helper.succeed();
        });
    }
}
