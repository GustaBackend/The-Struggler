package com.struggler.world.entity;

/**
 * The damage facts the classification depends on, with no Minecraft types.
 *
 * <p>Keeping the decision inputs as plain booleans lets the policy be covered by
 * fast unit tests, while the entity fills the same record from the real Forge
 * damage tags. No field is derived from an entity name or a death message.</p>
 *
 * @param bypassesShield    source declares vanilla shield bypass
 * @param explosion         source is tagged as an explosion
 * @param fire              source is tagged as fire
 * @param fall              source is tagged as fall damage
 * @param freezing          source is tagged as freezing
 * @param drowning          source is tagged as drowning
 * @param lightning         source is tagged as lightning
 * @param projectile        source is tagged as a projectile
 * @param physicalProjectile the direct entity is an actual projectile entity
 * @param melee             source is one of the physical contact damage types
 */
public record GuardDamageFacts(boolean bypassesShield,
                               boolean explosion,
                               boolean fire,
                               boolean fall,
                               boolean freezing,
                               boolean drowning,
                               boolean lightning,
                               boolean projectile,
                               boolean physicalProjectile,
                               boolean melee) {
}
