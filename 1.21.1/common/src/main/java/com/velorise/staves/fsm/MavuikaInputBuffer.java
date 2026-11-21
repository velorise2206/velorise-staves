package com.velorise.staves.fsm;

/**
 * Block 3 of Mavuika's 4-Block HFSM:
 * Thin compatibility facade delegating all Flamestrider combat input buffering and state transitions
 * to the authoritative {@link MavuikaActionMachine} + {@link BikeFrameData} cancel matrix.
 * Shared identically across NeoForge 1.21.1, Fabric 1.21.1, and Forge 1.20.1.
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

    public static final int DEFAULT_TTL_TICKS = MavuikaActionMachine.BUFFER_TTL_TICKS;

    private final MavuikaActionMachine machine = new MavuikaActionMachine();

    /**
     * Per-tick output decisions returned to ClientInputHandler / VeloriseStavesClient for network dispatch.
     */
    public static final class TickDecision {
        public boolean sendSpinStart = false;
        public boolean sendSpinStop = false;
        public boolean sendSpinTapPulse = false;
        public boolean sendNormalAttack = false;
        public boolean cancelClientFinisherToSpin = false;
        public boolean triggerClientFinisher = false;
    }

    public MavuikaActionMachine getMachine() {
        return machine;
    }

    public void reset() {
        machine.reset();
    }

    public void resetOnMount() {
        machine.resetOnMount();
    }

    public void push(BikeInput input) {
        machine.pushInput(input, DEFAULT_TTL_TICKS);
    }

    public void push(BikeInput input, int ttlTicks) {
        machine.pushInput(input, ttlTicks);
    }

    public void queueFastAttackClick() {
        machine.queueFastAttackClick();
    }

    public boolean hasBuffered(BikeInput input) {
        return machine.hasBuffered(input);
    }

    public boolean consumeIf(BikeInput input) {
        return machine.consumeIf(input);
    }

    public boolean isSpinActive() {
        return machine.isSpinActive();
    }

    public boolean isAutoSpinLatched() {
        return machine.isContinuousSpinActive();
    }

    public void notifyJumpPressed() {
        machine.notifyJumpPressed();
    }

    public void notifyDashTriggered(boolean attackStillHeld, boolean bikeWasSpinning) {
        machine.notifyDashTriggered(bikeWasSpinning);
    }

    public TickDecision tickAttackInput(
            boolean attackDown,
            int extraAttackClicks,
            boolean ultiLocked,
            boolean bikeIsSpinning,
            int activeNaStep,
            int activeNaTicks
    ) {
        return machine.step(attackDown, extraAttackClicks, ultiLocked, bikeIsSpinning, activeNaStep, activeNaTicks);
    }
}
