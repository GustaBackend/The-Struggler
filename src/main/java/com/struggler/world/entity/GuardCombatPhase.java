package com.struggler.world.entity;

/** Server-side phases of one melee strike. */
public enum GuardCombatPhase {
    IDLE,
    CHASE,
    WINDUP,
    ATTACKING,
    RECOVERY;

    public boolean isStriking() {
        return this == WINDUP || this == ATTACKING || this == RECOVERY;
    }

    public static GuardCombatPhase fromId(byte id) {
        GuardCombatPhase[] values = values();
        return id >= 0 && id < values.length ? values[id] : IDLE;
    }
}
