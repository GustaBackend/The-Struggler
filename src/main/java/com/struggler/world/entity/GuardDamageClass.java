package com.struggler.world.entity;

/**
 * How the Midland Guard's physical shield relates to a damage source (0.3.3).
 *
 * <p>The shield is a piece of steel: it stops what physically hits it and does
 * not stop what does not. Instead of inferring that from "there is a living
 * entity attached to the source" (which would let magic and explosions through
 * the shield just because a witch or a creeper caused them), every hit is
 * classified first and only the defensible classes reach the reduction.</p>
 */
public enum GuardDamageClass {
    /** Magic, fire, explosions, falls, environment and bypassing sources. */
    NOT_DEFENSIBLE,
    /** Physical contact attack from a living attacker (sword, fist, claws). */
    MELEE,
    /** Physical projectile: arrow, trident, thrown item, skull, mob projectile. */
    PROJECTILE;

    /** True when a shield can physically cover this hit. */
    public boolean isDefensible() {
        return this != NOT_DEFENSIBLE;
    }
}
