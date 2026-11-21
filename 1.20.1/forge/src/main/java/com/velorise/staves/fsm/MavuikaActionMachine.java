package com.velorise.staves.fsm;

import com.velorise.staves.fsm.MavuikaInputBuffer.BikeInput;
import com.velorise.staves.fsm.MavuikaInputBuffer.TickDecision;

/**
 * Pure Flag-Free Finite State Machine for Mavuika's Flamestrider bike.
 *
 * Zero boolean mode flags (no caSustainActive, no dashFromCharged, no hidden tap counters):
 * every mode is an explicit BikeActionState governed by a single switch(currentState) transition table
 * and BikeFrameData cancel windows.
 *
 * Explicit Charged Attack graph:
 *   IDLE/NA (hold 4t) -> CA_SPIN_HOLD
 *   CA_SPIN_HOLD (release) -> CA_SPIN_RELEASE_GRACE (4t)
 *   CA_SPIN_RELEASE_GRACE (tap #1) -> CA_SPIN_TAP_ARMED (6t)
 *   CA_SPIN_RELEASE_GRACE (timeout 4t) -> CA_SPIN_FINISHER (25t)
 *   CA_SPIN_TAP_ARMED (tap #2) -> CA_SPIN_CONTINUOUS (hands-free up to 6.0s = 120t)
 *   CA_SPIN_TAP_ARMED (hold 4t) -> CA_SPIN_HOLD
 *   CA_SPIN_TAP_ARMED (timeout 6t) -> CA_SPIN_FINISHER (25t)
 *   Any CA_* + Dash -> DASH_CHARGE_CANCEL (caCancel = 2t -> CA_SPIN_HOLD when CA_HOLD_START buffered)
 *   Any IDLE/NA + Dash -> DASH (caCancel = 6t)
 */
public final class MavuikaActionMachine {

    /** Neutral hold ticks before N0 startup branches into Charged Attack Spin (~200ms = 4 ticks). */
    public static final int NEUTRAL_CA_HOLD_TICKS = 4;
    /** Input buffer TTL (6 ticks = 300ms, aligns with gcsim 0-15f charge buffer + Dash 2t hitmark). */
    public static final int BUFFER_TTL_TICKS = 6;
    /** Minimum Donut Spin ticks before Finisher can commit (~180 deg half-rotation = 6 ticks). */
    public static final int MIN_SPIN_BEFORE_FINISHER_TICKS = 6;
    /** Maximum continuous Donut Spin duration (120 ticks = 6.0s max, matching Genshin Impact). */
    public static final int MAX_CONTINUOUS_SPIN_TICKS = 120;

    private final InputBuffer<BikeInput> attackBuffer = new InputBuffer<>();
    private final InputBuffer<BikeInput> actionBuffer = new InputBuffer<>();
    private BikeActionState currentState = BikeActionState.IDLE;
    private int stateTick = 0;
    private int hitlagTicks = 0;
    private int holdDurationTicks = 0;
    private int totalSpinTicks = 0;
    private boolean prevAttackDown = false;
    private boolean pendingSpinStopFromJump = false;
    private int queuedClicks = 0;

    public void reset() {
        attackBuffer.clear();
        actionBuffer.clear();
        currentState = BikeActionState.IDLE;
        stateTick = 0;
        hitlagTicks = 0;
        holdDurationTicks = 0;
        totalSpinTicks = 0;
        prevAttackDown = false;
        pendingSpinStopFromJump = false;
        queuedClicks = 0;
    }

    public void resetOnMount() {
        queuedClicks = 0;
        pendingSpinStopFromJump = false;
    }

    public BikeActionState getCurrentState() {
        return currentState;
    }

    public int getStateTick() {
        return stateTick;
    }

    public boolean isSpinActive() {
        return currentState.isChargedSpinState();
    }

    public boolean isContinuousSpinActive() {
        return currentState == BikeActionState.CA_SPIN_CONTINUOUS;
    }

    public void pushInput(BikeInput input, int ttlTicks) {
        if (input == null) return;
        if (input == BikeInput.DASH || input == BikeInput.JUMP_OR_GLIDE || input == BikeInput.ULTI || input == BikeInput.PLUNGE) {
            actionBuffer.push(input, ttlTicks);
        } else {
            attackBuffer.push(input, ttlTicks);
        }
    }

    public void queueFastAttackClick() {
        queuedClicks++;
        if (!currentState.isChargedSpinState()) {
            attackBuffer.push(BikeInput.NA_CLICK, BUFFER_TTL_TICKS);
        }
    }

    public boolean hasBuffered(BikeInput input) {
        return attackBuffer.peek() == input || actionBuffer.peek() == input;
    }

    public boolean consumeIf(BikeInput input) {
        if (attackBuffer.consumeIf(i -> i == input) != null) return true;
        return actionBuffer.consumeIf(i -> i == input) != null;
    }

    public void notifyJumpPressed() {
        actionBuffer.push(BikeInput.JUMP_OR_GLIDE, BUFFER_TTL_TICKS);
        if (BikeFrameData.canCancelInto(currentState, BikeActionState.AIRBORNE, stateTick)) {
            boolean wasSpinning = currentState.isChargedSpinState();
            actionBuffer.consumeIf(i -> i == BikeInput.JUMP_OR_GLIDE);
            transitionTo(BikeActionState.AIRBORNE);
            if (wasSpinning) {
                pendingSpinStopFromJump = true;
            }
        }
    }

    public void notifyDashTriggered(boolean bikeWasSpinning) {
        if (bikeWasSpinning && !currentState.isChargedSpinState()) {
            currentState = BikeActionState.CA_SPIN_HOLD;
        }
        BikeActionState dashTarget = (currentState.isChargedSpinState() || currentState == BikeActionState.CA_SPIN_FINISHER)
                ? BikeActionState.DASH_CHARGE_CANCEL
                : BikeActionState.DASH;
        actionBuffer.push(BikeInput.DASH, BUFFER_TTL_TICKS);
        if (BikeFrameData.canCancelInto(currentState, dashTarget, stateTick)) {
            actionBuffer.consumeIf(i -> i == BikeInput.DASH);
            transitionTo(dashTarget);
        }
    }

    private void transitionTo(BikeActionState next) {
        if (!next.isChargedSpinState()) {
            totalSpinTicks = 0;
        } else if (!currentState.isChargedSpinState()) {
            totalSpinTicks = 0;
        }
        currentState = next;
        stateTick = 0;
        hitlagTicks = 0;
    }

    public TickDecision step(
            boolean attackDown,
            int extraAttackClicks,
            boolean ultiLocked,
            boolean serverSpinning,
            int activeNaStep,
            int activeNaTicks
    ) {
        TickDecision out = new TickDecision();
        attackBuffer.tick();
        actionBuffer.tick();

        if (pendingSpinStopFromJump) {
            pendingSpinStopFromJump = false;
            out.sendSpinStop = true;
        }

        int clicks = queuedClicks + extraAttackClicks;
        queuedClicks = 0;

        // 1. Authoritative Burst lock
        if (ultiLocked) {
            if (currentState != BikeActionState.ULTI_BURST) {
                transitionTo(BikeActionState.ULTI_BURST);
            }
            stateTick++;
            holdDurationTicks = 0;
            prevAttackDown = attackDown;
            return out;
        } else if (currentState == BikeActionState.ULTI_BURST) {
            transitionTo(BikeActionState.IDLE);
        }

        // Reconcile externally driven server Finisher/Spin state
        if (serverSpinning && currentState == BikeActionState.IDLE) {
            transitionTo(BikeActionState.CA_SPIN_HOLD);
        } else if (activeNaStep == 6 && activeNaTicks > 0 && currentState == BikeActionState.IDLE) {
            currentState = BikeActionState.CA_SPIN_FINISHER;
            stateTick = Math.max(0, BikeFrameData.get(BikeActionState.CA_SPIN_FINISHER).durationTicks() - activeNaTicks);
        }

        // 2. Advance state clock (respecting hitlag freeze on hitmark)
        BikeFrameData.FrameSpec spec = BikeFrameData.get(currentState);
        if (hitlagTicks > 0) {
            hitlagTicks--;
        } else {
            stateTick++;
            if (spec.hitmarkTick() > 0 && stateTick == spec.hitmarkTick() && spec.hitlagTicks() > 0) {
                hitlagTicks = spec.hitlagTicks();
            }
        }
        if (currentState.isChargedSpinState()) {
            totalSpinTicks++;
        } else if (spec.durationTicks() > 0 && stateTick >= spec.durationTicks()) {
            transitionTo(BikeActionState.IDLE);
        }

        // 3. Pure hardware edge-triggered input capture (backed by GenshinInputState GLFW_PRESS)
        boolean risingEdge = (attackDown && !prevAttackDown);
        int newPresses = Math.max(clicks, risingEdge ? 1 : 0);

        if (attackDown) {
            holdDurationTicks++;
            if (newPresses > 0) {
                attackBuffer.push(BikeInput.CA_HOLD_START, BUFFER_TTL_TICKS);
            }
        } else {
            if (clicks > 0 && !currentState.isChargedSpinState()) {
                attackBuffer.push(BikeInput.NA_CLICK, BUFFER_TTL_TICKS);
            } else if (prevAttackDown && !currentState.isChargedSpinState() && holdDurationTicks < NEUTRAL_CA_HOLD_TICKS) {
                attackBuffer.push(BikeInput.NA_CLICK, BUFFER_TTL_TICKS);
            }
            holdDurationTicks = 0;
        }
        prevAttackDown = attackDown;

        // 4. Buffered Dash interrupt (checked before state body; never touches attackBuffer)
        if (actionBuffer.peek() == BikeInput.DASH) {
            BikeActionState dashTarget = (currentState.isChargedSpinState() || currentState == BikeActionState.CA_SPIN_FINISHER)
                    ? BikeActionState.DASH_CHARGE_CANCEL
                    : BikeActionState.DASH;
            if (BikeFrameData.canCancelInto(currentState, dashTarget, stateTick)) {
                actionBuffer.consume();
                transitionTo(dashTarget);
                return out;
            }
        }

        // 5. Pure Finite State Machine transition table: switch(currentState)
        switch (currentState) {
            case CA_SPIN_HOLD -> {
                if (totalSpinTicks >= MAX_CONTINUOUS_SPIN_TICKS) {
                    enterFinisher(out);
                } else if (!attackDown) {
                    transitionTo(BikeActionState.CA_SPIN_RELEASE_GRACE);
                    // Handle ultra-fast same-tick release + tap #1
                    if (newPresses >= 2) {
                        transitionTo(BikeActionState.CA_SPIN_CONTINUOUS);
                    } else if (newPresses == 1) {
                        transitionTo(BikeActionState.CA_SPIN_TAP_ARMED);
                    }
                }
                return out;
            }
            case CA_SPIN_RELEASE_GRACE -> {
                if (totalSpinTicks >= MAX_CONTINUOUS_SPIN_TICKS) {
                    enterFinisher(out);
                } else if (newPresses >= 2) {
                    transitionTo(BikeActionState.CA_SPIN_CONTINUOUS);
                } else if (newPresses == 1) {
                    transitionTo(BikeActionState.CA_SPIN_TAP_ARMED);
                } else if (attackDown) {
                    transitionTo(BikeActionState.CA_SPIN_HOLD);
                } else if (stateTick >= BikeFrameData.get(BikeActionState.CA_SPIN_RELEASE_GRACE).durationTicks()
                        && totalSpinTicks >= MIN_SPIN_BEFORE_FINISHER_TICKS) {
                    enterFinisher(out);
                }
                return out;
            }
            case CA_SPIN_TAP_ARMED -> {
                if (totalSpinTicks >= MAX_CONTINUOUS_SPIN_TICKS) {
                    enterFinisher(out);
                } else if (newPresses >= 1) {
                    // 2nd genuine physical tap -> Hands-Free Continuous Donut state!
                    transitionTo(BikeActionState.CA_SPIN_CONTINUOUS);
                } else if (attackDown && holdDurationTicks >= NEUTRAL_CA_HOLD_TICKS) {
                    // Player chose to hold down LMB rather than double-tap -> return to CA_SPIN_HOLD
                    transitionTo(BikeActionState.CA_SPIN_HOLD);
                } else if (!attackDown && stateTick >= BikeFrameData.get(BikeActionState.CA_SPIN_TAP_ARMED).durationTicks()) {
                    enterFinisher(out);
                }
                return out;
            }
            case CA_SPIN_CONTINUOUS -> {
                // Hands-Free Continuous Donut: stays spinning with LMB released until 6.0s cap, Dash, Jump, or Burst!
                if (totalSpinTicks >= MAX_CONTINUOUS_SPIN_TICKS) {
                    enterFinisher(out);
                }
                return out;
            }
            case DASH, DASH_CHARGE_CANCEL -> {
                // Only enter CA_SPIN_HOLD from Dash when a fresh edge-triggered CA_HOLD_START is buffered
                if (attackDown && attackBuffer.peek() == BikeInput.CA_HOLD_START
                        && BikeFrameData.canCancelInto(currentState, BikeActionState.CA_SPIN_HOLD, stateTick)) {
                    attackBuffer.consume();
                    transitionTo(BikeActionState.CA_SPIN_HOLD);
                    out.sendSpinStart = true;
                    return out;
                }
                if (attackBuffer.peek() == BikeInput.NA_CLICK
                        && BikeFrameData.canCancelInto(currentState, BikeActionState.NA_1, stateTick)) {
                    attackBuffer.consume();
                    transitionTo(BikeActionState.NA_1);
                    out.sendNormalAttack = true;
                }
                return out;
            }
            case CA_SPIN_FINISHER -> {
                // Strictly locked against LMB until duration expires (Dash/Burst already handled above)
                return out;
            }
            default -> {
                // IDLE / DRIVE / NA_1..NA_5 / AIRBORNE / etc.
                if (attackDown && holdDurationTicks >= NEUTRAL_CA_HOLD_TICKS
                        && BikeFrameData.canCancelInto(currentState, BikeActionState.CA_SPIN_HOLD, stateTick)) {
                    attackBuffer.consumeIf(i -> i == BikeInput.CA_HOLD_START || i == BikeInput.NA_CLICK);
                    transitionTo(BikeActionState.CA_SPIN_HOLD);
                    out.sendSpinStart = true;
                    return out;
                }
                if (attackBuffer.peek() == BikeInput.NA_CLICK
                        && BikeFrameData.canCancelInto(currentState, BikeActionState.NA_1, stateTick)) {
                    attackBuffer.consume();
                    transitionTo(nextNormalAttack(currentState));
                    out.sendNormalAttack = true;
                }
                return out;
            }
        }
    }

    private void enterFinisher(TickDecision out) {
        transitionTo(BikeActionState.CA_SPIN_FINISHER);
        out.triggerClientFinisher = true;
        out.sendSpinStop = true;
    }

    private static BikeActionState nextNormalAttack(BikeActionState current) {
        return switch (current) {
            case NA_1 -> BikeActionState.NA_2;
            case NA_2 -> BikeActionState.NA_3;
            case NA_3 -> BikeActionState.NA_4;
            case NA_4 -> BikeActionState.NA_5;
            default -> BikeActionState.NA_1;
        };
    }
}
