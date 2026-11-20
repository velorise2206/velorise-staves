package com.velorise.staves.fsm;

/**
 * Block 3 of Mavuika's 4-Block HFSM:
 * Centralizes Flamestrider client input buffering (5-tick TTL), tap-vs-hold NA/CA detection,
 * Genshin Hands-Free Continuous Charged Attack Double-Tap Tech, and fast CCD/CD spin re-entry.
 * Shared identically across NeoForge, Fabric, and Forge ClientInputHandlers.
 */
public final class MavuikaInputBuffer {
    public enum BikeInput {
        NA_CLICK,
        CA_HOLD_START,
        CA_RELEASE,
        DASH,
        JUMP_OR_GLIDE,
        PLUNGE,
        ULTI,
        TOGGLE_BIKE
    }

    public static final int DEFAULT_TTL_TICKS = 5;
    public static final int NEUTRAL_HOLD_THRESHOLD_TICKS = 6;   // 300ms hold from neutral -> CA Spin
    public static final int FAST_REENTRY_HOLD_TICKS = 1;        // 1-tick fast re-entry for CCD / CD tech
    public static final int DOUBLE_TAP_WINDOW_TICKS = 10;       // 0.5s double-tap latch window

    private final InputBuffer<BikeInput> buffer = new InputBuffer<>();
    private int attackHoldTicks = 0;
    private boolean spinActive = false;
    private int spinReleaseWindowTicks = 0;
    private boolean heldThroughDash = false;
    private boolean autoSpinLatched = false;
    private int spinTapCount = 0;
    private int doubleTapWindowTicks = 0;
    private int queuedFastClicks = 0;

    /**
     * Per-tick output decisions returned to ClientInputHandler for network dispatch.
     */
    public static final class TickDecision {
        public boolean sendSpinStart = false;
        public boolean sendSpinStop = false;
        public boolean sendSpinTapPulse = false;
        public boolean sendNormalAttack = false;
        public boolean cancelClientFinisherToSpin = false;
        public boolean triggerClientFinisher = false;
    }

    public void reset() {
        buffer.clear();
        attackHoldTicks = 0;
        spinActive = false;
        spinReleaseWindowTicks = 0;
        heldThroughDash = false;
        autoSpinLatched = false;
        spinTapCount = 0;
        doubleTapWindowTicks = 0;
        queuedFastClicks = 0;
    }

    public void resetOnMount() {
        queuedFastClicks = 0;
    }

    public void push(BikeInput input) {
        buffer.push(input, DEFAULT_TTL_TICKS);
    }

    public void push(BikeInput input, int ttlTicks) {
        buffer.push(input, ttlTicks);
    }

    public void queueFastAttackClick() {
        queuedFastClicks++;
        buffer.push(BikeInput.NA_CLICK, DEFAULT_TTL_TICKS);
    }

    public boolean hasBuffered(BikeInput input) {
        return buffer.peek() == input;
    }

    public boolean consumeIf(BikeInput input) {
        return buffer.consumeIf(i -> i == input) != null;
    }

    public boolean isSpinActive() {
        return spinActive;
    }

    public boolean isAutoSpinLatched() {
        return autoSpinLatched;
    }

    public void notifyJumpPressed() {
        autoSpinLatched = false;
        buffer.push(BikeInput.JUMP_OR_GLIDE, DEFAULT_TTL_TICKS);
    }

    public void notifyDashTriggered(boolean attackStillHeld, boolean bikeWasSpinning) {
        boolean wasSpinningOrHolding = spinActive || bikeWasSpinning || spinReleaseWindowTicks > 0
                || autoSpinLatched || attackStillHeld;
        heldThroughDash = wasSpinningOrHolding && attackStillHeld;
        spinReleaseWindowTicks = wasSpinningOrHolding ? 7 : 0;
        autoSpinLatched = false;
        spinTapCount = 0;
        doubleTapWindowTicks = 0;
        attackHoldTicks = 0;
        spinActive = false;
        buffer.push(BikeInput.DASH, DEFAULT_TTL_TICKS);
    }

    /**
     * Steps timers and evaluates NA click vs CA Spin start/release decisions for one client tick.
     */
    public TickDecision tickAttackInput(
            boolean attackDown,
            int extraAttackClicks,
            boolean ultiLocked,
            boolean bikeIsSpinning,
            int activeNaStep,
            int activeNaTicks
    ) {
        TickDecision out = new TickDecision();
        buffer.tick();

        int attackClicks = queuedFastClicks + extraAttackClicks;
        queuedFastClicks = 0;

        if (spinReleaseWindowTicks > 0) {
            spinReleaseWindowTicks--;
        }
        if (doubleTapWindowTicks > 0) {
            doubleTapWindowTicks--;
            if (doubleTapWindowTicks == 0) {
                spinTapCount = 0;
            }
        }

        if (ultiLocked) {
            attackHoldTicks = 0;
            spinActive = false;
            spinReleaseWindowTicks = 0;
            heldThroughDash = false;
            autoSpinLatched = false;
            spinTapCount = 0;
            doubleTapWindowTicks = 0;
            return out;
        }

        if (!attackDown && heldThroughDash) {
            heldThroughDash = false;
        }

        boolean committedFinisherSlam = activeNaStep == 6 && activeNaTicks > 5 && activeNaTicks <= 8;
        boolean fastSpinReentry = spinReleaseWindowTicks > 0 || heldThroughDash
                || (activeNaStep == 6 && activeNaTicks > 0 && (activeNaTicks >= 9 || activeNaTicks <= 5));
        int requiredHoldTicks = fastSpinReentry ? FAST_REENTRY_HOLD_TICKS : NEUTRAL_HOLD_THRESHOLD_TICKS;

        if (attackDown) {
            if (attackHoldTicks == 0 && (bikeIsSpinning || spinReleaseWindowTicks > 0 || autoSpinLatched)) {
                if (autoSpinLatched && doubleTapWindowTicks == 0) {
                    autoSpinLatched = false;
                } else {
                    spinTapCount++;
                    doubleTapWindowTicks = DOUBLE_TAP_WINDOW_TICKS;
                    if (spinTapCount >= 2) {
                        autoSpinLatched = true;
                    }
                }
            }
            attackHoldTicks++;
            if (!spinActive && !committedFinisherSlam && attackHoldTicks >= requiredHoldTicks) {
                spinActive = true;
                if (doubleTapWindowTicks == 0 && !autoSpinLatched) {
                    spinTapCount = 0;
                    doubleTapWindowTicks = DOUBLE_TAP_WINDOW_TICKS;
                }
                out.cancelClientFinisherToSpin = true;
                out.sendSpinStart = true;
                buffer.push(BikeInput.CA_HOLD_START, DEFAULT_TTL_TICKS);
            }
        } else if (attackHoldTicks > 0) {
            if (spinActive) {
                if (autoSpinLatched) {
                    spinReleaseWindowTicks = 8;
                } else {
                    spinReleaseWindowTicks = 6;
                    if (doubleTapWindowTicks == 0) {
                        doubleTapWindowTicks = DOUBLE_TAP_WINDOW_TICKS;
                    }
                    out.triggerClientFinisher = true;
                    out.sendSpinStop = true;
                    spinActive = false;
                    buffer.push(BikeInput.CA_RELEASE, DEFAULT_TTL_TICKS);
                }
            } else {
                out.sendNormalAttack = true;
                buffer.push(BikeInput.NA_CLICK, DEFAULT_TTL_TICKS);
            }
            attackHoldTicks = 0;
        } else if (attackClicks > 0) {
            if (bikeIsSpinning || spinReleaseWindowTicks > 0 || autoSpinLatched) {
                spinTapCount += attackClicks;
                doubleTapWindowTicks = DOUBLE_TAP_WINDOW_TICKS;
                if (spinTapCount >= 2) {
                    autoSpinLatched = true;
                    spinActive = true;
                    out.sendSpinStart = true;
                } else {
                    spinReleaseWindowTicks = 6;
                    out.sendSpinTapPulse = true;
                }
            } else {
                out.sendNormalAttack = true;
                buffer.push(BikeInput.NA_CLICK, DEFAULT_TTL_TICKS);
            }
        }

        return out;
    }
}
