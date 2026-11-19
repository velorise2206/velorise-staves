package com.velorise.staves.fsm;

/**
 * Unified state enum and transition rules for universal player air/wall movement
 * (WallClimb, ClimbJump, LedgeMantle, Gliding, PlungeDive, ExhaustedFall).
 * Shared identically by Client prediction and Server authority.
 */
public enum MovementActionState {
    GROUNDED,
    WALL_CLING,
    WALL_CLIMB,
    WALL_JUMP,
    LEDGE_MANTLE,
    GLIDING,
    PLUNGE_DIVE,
    EXHAUSTED_FALL;

    public static final int WALL_JUMP_DURATION_TICKS = 8;
    public static final int WALL_JUMP_CANCEL_WINDOW_TICKS = 5;
    public static final int LEDGE_MANTLE_DURATION_TICKS = 6;
    public static final int LEDGE_MANTLE_CANCEL_WINDOW_TICKS = 5;

    /**
     * Global movement authority ladder (highest to lowest priority).
     */
    public enum MovementLayer {
        MAVUIKA_BIKE,
        XILONEN_SKATE,
        VANILLA_CLIMBABLE,
        UNIVERSAL_AIR_MOVE
    }

    /**
     * Buffered inputs consumed when entering a valid transition window.
     */
    public enum BufferedInput {
        CLIMB_JUMP,
        DROP_OFF,
        TOGGLE_GLIDER,
        PLUNGE_ATTACK
    }

    public boolean isWallState() {
        return this == WALL_CLING || this == WALL_CLIMB || this == WALL_JUMP || this == LEDGE_MANTLE;
    }

    /**
     * Single source of truth for allowed state transitions (authentic Genshin rules).
     */
    public static boolean canTransition(MovementActionState from, MovementActionState to, int elapsedTicks) {
        if (from == null || to == null) return false;
        if (from == to) return true;
        if (to == GROUNDED) return true;

        switch (from) {
            case GROUNDED:
                return to != EXHAUSTED_FALL;
            case WALL_CLING:
            case WALL_CLIMB:
                // Authentic Genshin: cannot plunge directly from a wall; must drop off first.
                return to != PLUNGE_DIVE;
            case WALL_JUMP:
                if (to == EXHAUSTED_FALL) return true;
                if (to == PLUNGE_DIVE) return false;
                return elapsedTicks >= WALL_JUMP_CANCEL_WINDOW_TICKS;
            case LEDGE_MANTLE:
                return elapsedTicks >= LEDGE_MANTLE_CANCEL_WINDOW_TICKS && to != PLUNGE_DIVE;
            case GLIDING:
                // Instant glide -> plunge dive or wall grab.
                return to == PLUNGE_DIVE || to == WALL_CLING || to == WALL_CLIMB || to == EXHAUSTED_FALL;
            case PLUNGE_DIVE:
                // Committed until landing or fluid/vehicle abort.
                return false;
            case EXHAUSTED_FALL:
                // Authentic Genshin: falling from climbing stamina depletion locks Plunging Attack and re-grabbing walls.
                return to == GLIDING;
            default:
                return false;
        }
    }
}
