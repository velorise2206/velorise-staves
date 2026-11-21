package com.velorise.staves.fsm;

/**
 * Block 2 of Mavuika's 4-Block HFSM:
 * Authoritative pure Finite State Machine enum for the Flamestrider bike.
 * Every behavioral mode (Hold Spin, Release Grace, Tap Armed, Hands-Free Continuous Spin,
 * Normal Dash, Charge-Cancel Dash) is a first-class state — zero hidden boolean mode flags.
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
    NA_1(40, 9, 4, true),
    NA_2(40, 11, 5, true),
    NA_3(40, 12, 6, true),
    NA_4(40, 10, 5, true),
    NA_5(42, 23, 12, true),               // 360-degree front-flip slam
    MOUNT_TAIL_WHIP(48, 10, 5, true),     // Hold E mount 180-deg half-donut tail whip with forward surge
    MOUNT_CATCH(46, 10, 0, true),         // Re-mount catch-up: bike rushes in from behind to scoop airborne rider

    // Explicit Charged Attack Donut Spin sub-states (0 boolean mode flags!)
    CA_SPIN_HOLD(50, 120, 0, true),          // Spinning while LMB held (6.0s max)
    CA_SPIN_RELEASE_GRACE(50, 4, 0, true),   // 4t release grace before committing to Finisher
    CA_SPIN_TAP_ARMED(50, 6, 0, true),       // Armed after tap #1; tap #2 -> CA_SPIN_CONTINUOUS
    CA_SPIN_CONTINUOUS(50, 120, 0, true),    // Hands-Free Continuous Donut after double-tap CC (6.0s max)

    CA_SPIN_FINISHER(55, 27, 27, true),      // Authentic 27t Genshin Finisher: 3t pull + 7t rider aerial vault + 4t plunge slam + 13t wheelie tail-slide
    PLUNGE_WINDUP(58, 5, 5, true),
    PLUNGE_DIVE(60, -1, 0, true),

    // High-priority interrupts (priority 80..100)
    DASH(80, 7, 6, true),                    // Standard Dash (caCancel = 6t)
    DASH_CHARGE_CANCEL(80, 7, 2, true),      // Dash entered from CA_* (dC early caCancel = 2t)
    ULTI_BURST(100, 30, 22, true);           // Crucible of Death and Life burst slam

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

    public boolean isChargedSpinState() {
        return this == CA_SPIN_HOLD
                || this == CA_SPIN_RELEASE_GRACE
                || this == CA_SPIN_TAP_ARMED
                || this == CA_SPIN_CONTINUOUS;
    }

    public boolean isDashState() {
        return this == DASH || this == DASH_CHARGE_CANCEL;
    }

    public boolean isSpinState() {
        return isChargedSpinState() || this == CA_SPIN_FINISHER;
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
            case MOUNT_CATCH -> 8;
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
            case 8 -> MOUNT_CATCH;
            default -> IDLE;
        };
    }

    public static BikeActionState fromOrdinal(int ordinal) {
        BikeActionState[] vals = values();
        if (ordinal < 0 || ordinal >= vals.length) return IDLE;
        return vals[ordinal];
    }

    public static boolean canTransition(BikeActionState current, BikeActionState next, int elapsedTicks) {
        return BikeFrameData.canCancelInto(current, next, elapsedTicks);
    }

    /**
     * Resolves the visual BikeActionState from entity runtime data in strict priority order.
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
        if (spinning) return CA_SPIN_HOLD;
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
