package com.velorise.staves.element;

/**
 * Tracks the active Gauge Units (U) and decay rate for a lingering elemental aura.
 * Implements the standard 0.8x Aura Tax upon initial application and Genshin EGT decay/refresh rules.
 */
public final class GaugeState {
    private final Element element;
    private float units;
    private float decayPerTick;
    private int maxDurationTicks;
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

    private GaugeState(Element element, float directUnits, int customDurationTicks) {
        this.element = element;
        this.units = Math.max(0.01F, directUnits);
        this.maxDurationTicks = Math.max(20, customDurationTicks);
        this.decayPerTick = this.units / (float) this.maxDurationTicks;
        this.ageTicks = 0;
    }

    /**
     * Genshin Impact Quicken Aura formula:
     * U_Quicken = min(U_aura, U_trigger * 0.8), Duration = (5 * U_Quicken + 6) seconds.
     */
    public static GaugeState createQuicken(float quickenUnits) {
        float u = Math.max(0.1F, quickenUnits);
        int ticks = Math.max(20, Math.round((5.0F * u + 6.0F) * 20.0F));
        return new GaugeState(Element.DENDRO, u, ticks);
    }

    /**
     * Genshin Impact Freeze Aura formula for Frozen duration (default 70 ticks = 3.5s).
     */
    public static GaugeState createFrozen(float freezeUnits, int durationTicks) {
        return new GaugeState(Element.CRYO, Math.max(0.1F, freezeUnits), durationTicks);
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

    public void resetAge() {
        this.ageTicks = 0;
    }

    /**
     * Refreshes an existing aura with a new same-element application:
     * Updates units to max(currentUnits, appliedUnits * 0.8), recalculates duration, and resets ageTicks.
     */
    public void refresh(float appliedUnits) {
        float taxed = Math.max(0.01F, appliedUnits * 0.8F);
        this.units = Math.max(this.units, taxed);
        float durationSeconds = 2.5F * appliedUnits + 7.0F;
        int newMaxTicks = Math.max(20, Math.round(durationSeconds * 20.0F));
        this.maxDurationTicks = Math.max(this.maxDurationTicks, newMaxTicks);
        this.decayPerTick = this.units / (float) this.maxDurationTicks;
        this.ageTicks = 0;
    }

    public void tick() {
        this.units -= decayPerTick;
        this.ageTicks++;
    }

    public boolean isExpired() {
        return units <= 0.001F || ageTicks >= maxDurationTicks;
    }

    public int getRemainingTicks() {
        if (decayPerTick <= 1.0E-6F) {
            return Math.max(0, maxDurationTicks - ageTicks);
        }
        int byUnits = Math.round(units / decayPerTick);
        int byAge = maxDurationTicks - ageTicks;
        return Math.max(0, Math.min(byUnits, byAge));
    }
}
