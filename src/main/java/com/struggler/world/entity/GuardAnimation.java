package com.struggler.world.entity;

import java.util.Locale;

/**
 * Network-visible preview action identifiers and timing for the Midland Guard.
 *
 * <p>The client model consumes the seven data-driven clips (idle, walk, run, attack, defend,
 * hurt, death).  Only the five command actions are synchronized as a compact byte; hurt and
 * death continue to come from LivingEntity's natural timers.</p>
 */
public final class GuardAnimation {
    public static final int PREVIEW_GOAL_DISTANCE = 8;

    public enum Action {
        IDLE((byte) 0, "idle", 100),
        WALK((byte) 1, "walk", 100),
        RUN((byte) 2, "run", 100),
        ATTACK((byte) 3, "attack", 32),
        DEFEND((byte) 4, "defend", 40);

        private final byte id;
        private final String commandName;
        private final int durationTicks;

        Action(byte id, String commandName, int durationTicks) {
            this.id = id;
            this.commandName = commandName;
            this.durationTicks = durationTicks;
        }

        public byte id() {
            return this.id;
        }

        public String commandName() {
            return this.commandName;
        }

        public int durationTicks() {
            return this.durationTicks;
        }

        public boolean moves() {
            return this == WALK || this == RUN;
        }

        public static Action fromId(byte id) {
            for (Action action : values()) {
                if (action.id == id) {
                    return action;
                }
            }
            return IDLE;
        }

        public static Action fromCommand(String command) {
            if (command == null) {
                return null;
            }
            String normalized = command.toLowerCase(Locale.ROOT);
            for (Action action : values()) {
                if (action.commandName.equals(normalized)) {
                    return action;
                }
            }
            return null;
        }

        public static String[] commandNames() {
            return new String[]{"idle", "walk", "run", "attack", "defend", "reset"};
        }
    }

    private GuardAnimation() {
    }
}
