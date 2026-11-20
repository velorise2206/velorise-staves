package com.velorise.staves.fsm;

/**
 * Block 2 of Mavuika's 4-Block HFSM:
 * Authoritative action & locomotion state enum for the Flamestrider bike.
 * Replaces scattered boolean/timer priority checks with a single priority & cancel-window matrix.
 */
public enum BikeActionState {
    // Locomotion states (priority 10..26)
    IDLE(10, -1, 0, false),
    DRIVE(12, -1, 0, false),
    SPRINT(15, -1, 0, false),
    AIRBORNE(18, -1, 0, false),
    WATER_SURF(20, -1, 0, false),
    GLIDE(22, -1, 0, false),
    WALL_CLIMB(24, -1, 0, false),
    WALL_CREST(26, 6, 4, false),

    // Combat states (priority 35..60)
    PLUNGE_RECOVERY(35, 8, 3, true),
    NA_1(40, 10, 4, true),
    NA_2(40, 10, 4, true),
    NA_3(40, 10, 4, true),
    NA_4(40, 10, 5, true),
    NA_5(42, 14, 7, true),          // 360-degree front-flip slam
    MOUNT_TAIL_WHIP(48, 10, 5, true), // Hold E mount 180-deg half-donut tail whip
    CA_SPIN_LOOP(50, -1, 0, true),  // Donut Charged Attack continuous loop
    CA_SPIN_FINISHER(55, 14, 6, true),
    PLUNGE_WINDUP(58, 5, 5, true),
    PLUNGE_DIVE(60, -1, 0, true),

    // High-priority interrupts (priority 80..100)
    DASH(80, 8, 3, true),           // I-frame dash-cancel
    ULTI_BURST(100, 30, 22, true);  // Crucible of Death and Life burst slam

    private final int priority;
    private final int durationTicks;
    private final int cancelWindowStartTicks;
    private final boolean combatState;

    BikeActionState(int priority, int durationTicks, int cancelWindowStartTicks, boolean combatState) {
        this.priority = priority;
        this.durationTicks = durationTicks;
        this.cancelWindowStartTicks = cancelWindowStartTicks;
        this.combatState = combatState;
    }

    public int priority() {
        return priority;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int cancelWindowStartTicks() {
        return cancelWindowStartTicks;
    }

    public boolean isCombatState() {
        return combatState;
    }

    public boolean isNormalAttack() {
        return this == NA_1 || this == NA_2 || this == NA_3 || this == NA_4 || this == NA_5;
    }

    public boolean isPlungeState() {
        return this == PLUNGE_WINDUP || this == PLUNGE_DIVE || this == PLUNGE_RECOVERY;
    }

    public boolean isSpinState() {
        return this == CA_SPIN_LOOP || this == CA_SPIN_FINISHER;
    }

    public int toNaStep() {
        return switch (this) {
            case NA_1 -> 1;
            case NA_2 -> 2;
            case NA_3 -> 3;
            case NA_4 -> 4;
            case NA_5 -> 5;
            case CA_SPIN_FINISHER -> 6;
            case MOUNT_TAIL_WHIP -> 7;
            default -> 0;
        };
    }

    public static BikeActionState fromNaStep(int step) {
        return switch (step) {
            case 1 -> NA_1;
            case 2 -> NA_2;
            case 3 -> NA_3;
            case 4 -> NA_4;
            case 5 -> NA_5;
            case 6 -> CA_SPIN_FINISHER;
            case 7 -> MOUNT_TAIL_WHIP;
            default -> IDLE;
        };
    }

    public static BikeActionState fromOrdinal(int ordinal) {
        BikeActionState[] vals = values();
        if (ordinal < 0 || ordinal >= vals.length) return IDLE;
        return vals[ordinal];
    }

    /**
     * Authoritative transition guard enforcing state priorities and cancel windows.
     */
    public static boolean canTransition(BikeActionState current, BikeActionState next, int elapsedTicks) {
        if (current == null || next == null) return false;
        if (current == next) return true;
        // ULTI_BURST always preempts any non-burst state
        if (next == ULTI_BURST) return true;
        // Lock all transitions during active ULTI_BURST until recovery window (u <= 8 -> elapsed >= 22)
        if (current == ULTI_BURST) {
            return elapsedTicks >= current.cancelWindowStartTicks;
        }
        // DASH can cancel any NA, CA_SPIN_LOOP, CA_SPIN_FINISHER, PLUNGE_RECOVERY, or locomotion
        if (next == DASH) {
            return current != PLUNGE_WINDUP && current != PLUNGE_DIVE;
        }
        // Plunge can only be initiated from airborne/glide states
        if (next == PLUNGE_WINDUP || next == PLUNGE_DIVE) {
            return current == AIRBORNE || current == GLIDE || current == PLUNGE_WINDUP;
        }
        // Strict priority preemption or cancel window expiry
        if (next.priority > current.priority) {
            return true;
        }
        if (current.durationTicks > 0 && elapsedTicks >= current.durationTicks) {
            return true;
        }
        return elapsedTicks >= current.cancelWindowStartTicks;
    }

    /**
     * Resolves the canonical BikeActionState from entity runtime flags in strict priority order.
     */
    public static BikeActionState resolveActiveState(
            int ultiTicks,
            boolean plunging,
            int plungeWindup,
            int plungeRecoveryTicks,
            boolean dashing,
            boolean spinning,
            int activeNaStep,
            int activeNaTicks,
            boolean wallCresting,
            boolean wallClimbing,
            boolean gliding,
            boolean waterSurfing,
            boolean sprinting,
            boolean airborne,
            boolean moving
    ) {
        if (ultiTicks > 0) return ULTI_BURST;
        if (plunging) return plungeWindup > 0 ? PLUNGE_WINDUP : PLUNGE_DIVE;
        if (dashing) return DASH;
        if (spinning) return CA_SPIN_LOOP;
        if (activeNaTicks > 0 && activeNaStep > 0) {
            return fromNaStep(activeNaStep);
        }
        if (plungeRecoveryTicks > 0) return PLUNGE_RECOVERY;
        if (wallCresting) return WALL_CREST;
        if (wallClimbing) return WALL_CLIMB;
        if (gliding) return GLIDE;
        if (waterSurfing) return WATER_SURF;
        if (airborne) return AIRBORNE;
        if (sprinting) return SPRINT;
        return moving ? DRIVE : IDLE;
    }
}
