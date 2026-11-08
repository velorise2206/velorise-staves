package com.velorise.staves.element;

/**
 * Tracks the active Gauge Units (U) and decay rate for a lingering elemental aura.
 * Implements the standard 0.8x Aura Tax upon initial application.
 */
public final class GaugeState {
    private final Element element;
    private float units;
    private final float decayPerTick;
    private final int maxDurationTicks;
    private int ageTicks;

    public GaugeState(Element element, float appliedUnits) {
        this.element = element;
        // Aura Tax: 0.8x of applied attack units
        this.units = Math.max(0.01F, appliedUnits * 0.8F);
        // Genshin standard duration formula: 2.5 * appliedUnits + 7.0 seconds
        float durationSeconds = 2.5F * appliedUnits + 7.0F;
        this.maxDurationTicks = Math.max(20, Math.round(durationSeconds * 20.0F));
        this.decayPerTick = this.units / (float) this.maxDurationTicks;
        this.ageTicks = 0;
    }

    public Element getElement() {
        return element;
    }

    public float getUnits() {
        return units;
    }

    public void consume(float amount) {
        this.units = Math.max(0.0F, this.units - amount);
    }

    public void addUnits(float amount) {
        this.units += amount;
    }

    public void tick() {
        this.units -= decayPerTick;
        this.ageTicks++;
    }

    public boolean isExpired() {
        return units <= 0.001F || ageTicks >= maxDurationTicks;
    }
}
