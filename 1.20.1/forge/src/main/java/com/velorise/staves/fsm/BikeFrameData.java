package com.velorise.staves.fsm;

import java.util.EnumMap;
import java.util.Map;

/**
 * Authoritative gcsim / KQM-aligned Frame Data & Cancel Window table for Mavuika's Flamestrider
 * (converted from 60 FPS Genshin frames to 20 TPS Minecraft ticks via frames / 3).
 *
 * Pure state-table transitions: zero boolean mode parameters.
 */
public final class BikeFrameData {

    private BikeFrameData() {}

    public record FrameSpec(
            int durationTicks,
            int hitmarkTick,
            int hitlagTicks,
            int dashCancelTick,
            int jumpCancelTick,
            int naCancelTick,
            int caCancelTick,
            int burstCancelTick
    ) {}

    private static final Map<BikeActionState, FrameSpec> SPECS = new EnumMap<>(BikeActionState.class);

    static {
        FrameSpec free = new FrameSpec(-1, -1, 0, 0, 0, 0, 0, 0);
        SPECS.put(BikeActionState.IDLE, free);
        SPECS.put(BikeActionState.DRIVE, free);
        SPECS.put(BikeActionState.SPRINT, free);
        SPECS.put(BikeActionState.AIRBORNE, free);
        SPECS.put(BikeActionState.WATER_SURF, free);
        SPECS.put(BikeActionState.GLIDE, free);
        SPECS.put(BikeActionState.WALL_CLIMB, free);
        SPECS.put(BikeActionState.WALL_CREST, new FrameSpec(6, -1, 0, 4, 4, 4, 4, 0));

        // Flamestrider Normal Attacks (gcsim 60 FPS / 3 -> 20 TPS)
        SPECS.put(BikeActionState.NA_1, new FrameSpec(9,  4, 2, 4, 4, 6, 6, 0));
        SPECS.put(BikeActionState.NA_2, new FrameSpec(11, 5, 2, 5, 5, 7, 7, 0));
        SPECS.put(BikeActionState.NA_3, new FrameSpec(12, 6, 1, 6, 6, 8, 8, 0));
        SPECS.put(BikeActionState.NA_4, new FrameSpec(10, 5, 1, 5, 5, 7, 7, 0));
        SPECS.put(BikeActionState.NA_5, new FrameSpec(23, 12, 0, 12, 12, 18, 16, 0));

        // Hold E mount tail whip & Re-mount catch-up
        SPECS.put(BikeActionState.MOUNT_TAIL_WHIP, new FrameSpec(10, 5, 1, 5, 5, 8, 8, 0));
        SPECS.put(BikeActionState.MOUNT_CATCH,     new FrameSpec(10, 0, 0, 4, 4, 6, 6, 0));

        // Explicit Charged Attack Donut Spin states (0 hitlag, instant Dash/Jump/Burst cancel)
        SPECS.put(BikeActionState.CA_SPIN_HOLD,          new FrameSpec(120, 0, 0, 0, 0, -1, 0, 0));
        SPECS.put(BikeActionState.CA_SPIN_RELEASE_GRACE, new FrameSpec(4,   0, 0, 0, 0, -1, 0, 0));
        SPECS.put(BikeActionState.CA_SPIN_TAP_ARMED,     new FrameSpec(6,   0, 0, 0, 0, -1, 0, 0));
        SPECS.put(BikeActionState.CA_SPIN_CONTINUOUS,    new FrameSpec(120, 0, 0, 0, 0, -1, 0, 0));

        // Charged Attack Final Slam (once entered, ONLY Dash/Burst/post-hit Jump can cancel — LMB cannot cancel Finisher!)
        SPECS.put(BikeActionState.CA_SPIN_FINISHER, new FrameSpec(27, 14, 1, 0, 14, 27, 27, 0));

        // Plunge states
        SPECS.put(BikeActionState.PLUNGE_WINDUP, new FrameSpec(5, -1, 0, -1, -1, -1, -1, 0));
        SPECS.put(BikeActionState.PLUNGE_DIVE, new FrameSpec(-1, -1, 0, -1, -1, -1, -1, 0));
        SPECS.put(BikeActionState.PLUNGE_RECOVERY, new FrameSpec(8, 0, 1, 3, 3, 5, 5, 0));

        // Explicit Dash states: DASH (normal, caCancel=6t) vs DASH_CHARGE_CANCEL (from CA_*, early dC caCancel=2t)
        SPECS.put(BikeActionState.DASH,               new FrameSpec(7, 2, 0, -1, 3, 5, 6, 0));
        SPECS.put(BikeActionState.DASH_CHARGE_CANCEL, new FrameSpec(7, 2, 0, -1, 3, 5, 2, 0));

        // Crucible of Death and Life Burst
        SPECS.put(BikeActionState.ULTI_BURST, new FrameSpec(30, 22, 0, 22, 22, 22, 22, 30));
    }

    public static FrameSpec get(BikeActionState state) {
        return SPECS.getOrDefault(state, SPECS.get(BikeActionState.IDLE));
    }

    public static boolean canCancelInto(BikeActionState current, BikeActionState next, int elapsedTicks) {
        if (current == null || next == null) return false;
        if (current == next) return true;

        FrameSpec spec = get(current);
        if (spec.durationTicks() > 0 && elapsedTicks >= spec.durationTicks()) {
            return true;
        }
        if (next == BikeActionState.ULTI_BURST) {
            return spec.burstCancelTick() >= 0 && elapsedTicks >= spec.burstCancelTick();
        }
        if (next.isDashState()) {
            return spec.dashCancelTick() >= 0 && elapsedTicks >= spec.dashCancelTick();
        }
        if (next == BikeActionState.AIRBORNE || next == BikeActionState.GLIDE) {
            return spec.jumpCancelTick() >= 0 && elapsedTicks >= spec.jumpCancelTick();
        }
        if (next == BikeActionState.PLUNGE_WINDUP || next == BikeActionState.PLUNGE_DIVE) {
            return current == BikeActionState.AIRBORNE || current == BikeActionState.GLIDE || current == BikeActionState.PLUNGE_WINDUP;
        }
        if (next.isChargedSpinState()) {
            return spec.caCancelTick() >= 0 && elapsedTicks >= spec.caCancelTick();
        }
        if (next == BikeActionState.CA_SPIN_FINISHER) {
            return current.isChargedSpinState();
        }
        if (next.isNormalAttack() || next == BikeActionState.MOUNT_TAIL_WHIP || next == BikeActionState.MOUNT_CATCH) {
            return spec.naCancelTick() >= 0 && elapsedTicks >= spec.naCancelTick();
        }
        return false;
    }
}
