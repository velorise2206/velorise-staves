package com.velorise.staves.fsm;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Lightweight single-slot input buffer with tick TTL (4-5 ticks) for responsive
 * cancel windows at 20 TPS (shared by MovementMachine and character combat FSMs).
 */
public final class InputBuffer<I> {
    @Nullable
    private I bufferedInput = null;
    private int remainingTicks = 0;

    public void push(I input, int ttlTicks) {
        if (input == null || ttlTicks <= 0) return;
        this.bufferedInput = input;
        this.remainingTicks = ttlTicks;
    }

    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0) {
                bufferedInput = null;
            }
        }
    }

    public boolean hasBuffered() {
        return bufferedInput != null && remainingTicks > 0;
    }

    @Nullable
    public I peek() {
        return hasBuffered() ? bufferedInput : null;
    }

    @Nullable
    public I consume() {
        if (!hasBuffered()) return null;
        I out = bufferedInput;
        bufferedInput = null;
        remainingTicks = 0;
        return out;
    }

    @Nullable
    public I consumeIf(Predicate<I> predicate) {
        if (!hasBuffered() || predicate == null) return null;
        if (predicate.test(bufferedInput)) {
            return consume();
        }
        return null;
    }

    public void clear() {
        bufferedInput = null;
        remainingTicks = 0;
    }
}
