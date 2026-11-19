package com.velorise.staves.fsm;

/**
 * Reusable finite state node contract shared by MovementMachine (WallClimb / Glider / Plunge)
 * and character HFSMs (such as Mavuika's Flamestrider state machine).
 *
 * @param <C> Mutable state-machine context
 * @param <S> State identifier enum
 */
public interface StateNode<C, S extends Enum<S>> {
    S state();

    /**
     * Fixed duration in ticks, or -1 for open-ended states.
     */
    default int durationTicks() {
        return -1;
    }

    /**
     * First elapsed tick from which buffered cancels/transitions are permitted.
     */
    default int cancelWindowStartTicks() {
        return 0;
    }

    default void onEnter(C ctx) {}

    default void onTick(C ctx, int elapsedTicks) {}

    default boolean canTransitionTo(S nextState, int elapsedTicks) {
        return elapsedTicks >= cancelWindowStartTicks();
    }

    default void onExit(C ctx) {}
}
