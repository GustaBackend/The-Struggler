package com.struggler.world.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Explicit policy for what the Midland Guard's shield is allowed to stop (0.3.3).
 *
 * <p>The policy is a deny-by-default whitelist built on the vanilla damage tags,
 * not on entity names or death messages:</p>
 *
 * <ol>
 *   <li>Anything in {@code #bypasses_shield} is never blocked (anvils, stalactites
 *       and every type that already bypasses armour).</li>
 *   <li>Explosions, fire, fall, freezing, drowning and lightning are never
 *       blocked: a shield is not a wall, wings or a fireproof blanket.</li>
 *   <li>A projectile is blocked only when a real projectile entity delivered it,
 *       and only if it is not in one of the categories above.</li>
 *   <li>Physical contact attacks ({@code player_attack}, {@code mob_attack},
 *       {@code mob_attack_no_aggro}, {@code sting}) are blockable.</li>
 *   <li>Everything else — magic, indirect magic, thorns, sonic boom, wither,
 *       dragon breath, cactus, cramming, generic and any modded type that does not
 *       opt in — stays unblockable.</li>
 * </ol>
 *
 * <p>Because fire and explosion are checked before the projectile rule, a ghast
 * fireball (tagged both as projectile and as fire) is treated as fire and is not
 * blocked. That is the documented reading of "fire and explosions are not blocked
 * by default"; changing it is a one-line change in {@link #classify}.</p>
 */
public final class GuardDamagePolicy {
    private GuardDamagePolicy() {
    }

    /** Classifies a live damage source using the Forge/vanilla damage tags. */
    public static GuardDamageClass of(DamageSource source) {
        if (source == null) {
            return GuardDamageClass.NOT_DEFENSIBLE;
        }
        boolean physicalProjectile = source.getDirectEntity() instanceof Projectile;
        return classify(new GuardDamageFacts(
                source.is(DamageTypeTags.BYPASSES_SHIELD),
                source.is(DamageTypeTags.IS_EXPLOSION),
                source.is(DamageTypeTags.IS_FIRE),
                source.is(DamageTypeTags.IS_FALL),
                source.is(DamageTypeTags.IS_FREEZING),
                source.is(DamageTypeTags.IS_DROWNING),
                source.is(DamageTypeTags.IS_LIGHTNING),
                source.is(DamageTypeTags.IS_PROJECTILE),
                physicalProjectile,
                isPhysicalContact(source)));
    }

    /** The pure decision table, covered by unit tests. */
    public static GuardDamageClass classify(GuardDamageFacts facts) {
        if (facts.bypassesShield()) {
            return GuardDamageClass.NOT_DEFENSIBLE;
        }
        if (facts.explosion() || facts.fire() || facts.fall()
                || facts.freezing() || facts.drowning() || facts.lightning()) {
            return GuardDamageClass.NOT_DEFENSIBLE;
        }
        if (facts.projectile()) {
            return facts.physicalProjectile()
                    ? GuardDamageClass.PROJECTILE
                    : GuardDamageClass.NOT_DEFENSIBLE;
        }
        return facts.melee() ? GuardDamageClass.MELEE : GuardDamageClass.NOT_DEFENSIBLE;
    }

    /**
     * Where the attack logically came from.
     *
     * <p>For projectiles the shooter is used when the game knows it, so an arrow
     * is judged by who fired it and not by the few centimetres it travelled during
     * the tick it landed. When there is no attacker at all, the delivering entity
     * is used; when there is neither, the hit has no direction and cannot be
     * called frontal.</p>
     *
     * @return the origin position, or {@code null} when the source has none
     */
    public static Vec3 originOf(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            return living.position();
        }
        if (attacker != null) {
            return attacker.position();
        }
        Entity direct = source.getDirectEntity();
        return direct != null ? direct.position() : null;
    }

    private static boolean isPhysicalContact(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.is(DamageTypes.STING);
    }
}
