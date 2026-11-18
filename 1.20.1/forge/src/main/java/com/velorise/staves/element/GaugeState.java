package com.velorise.staves.element;

/**
 * Tracks the active Gauge Units (U) and decay rate for a lingering elemental aura.
 * Implements the standard 0.8x Aura Tax upon initial application and Genshin EGT decay/refresh rules:
 * - Standard duration: T = 2.5 * U_source + 7.0 seconds
 * - Decay Rate Inheritance: Hydro, Cryo, Electro, and Dendro inherit the initial aura's decay rate
 *   when refreshed with same-element applications until the gauge is fully depleted.
 * - Pyro 3.0 Rule: Pyro updates its decay rate to the trigger's decay rate only if the new post-tax
 *   gauge is strictly greater than the current remaining gauge.
 * - Quicken Duration: T_Quicken = 5 * U_Quicken + 6.0 seconds
 * - Freeze Duration: T_Freeze = 2 * sqrt(5 * U_Freeze + 4) - 4.0 seconds
 */
public final class GaugeState {
    private final Element element;
    private float units;
    private float decayPerTick;
    private int maxDurationTicks;
    private int ageTicks;

    public GaugeState(Element element, float appliedUnits) {
        this(element, appliedUnits, 1.0F);
    }

    public GaugeState(Element element, float appliedUnits, float durationScale) {
        this.element = element;
        // Aura Tax: 0.8x of applied attack units
        this.units = Math.max(0.01F, appliedUnits * 0.8F);
        // Genshin standard duration formula: 2.5 * appliedUnits + 7.0 seconds
        float scale = Math.max(0.1F, durationScale);
        float durationSeconds = (2.5F * appliedUnits + 7.0F) * scale;
        this.maxDurationTicks = Math.max(10, Math.round(durationSeconds * 20.0F));
        this.decayPerTick = this.units / (float) this.maxDurationTicks;
        this.ageTicks = 0;
    }

    public static GaugeState createWithDurationScale(Element element, float appliedUnits, float durationScale) {
        return new GaugeState(element, appliedUnits, durationScale);
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
     * Genshin Impact Burning Aura formula:
     * Burning applies an untaxed 2.0U Pyro aura.
     * Duration = 2.5 * U + 7.0 seconds.
     */
    public static GaugeState createBurning(float burningUnits) {
        float u = Math.max(0.1F, burningUnits);
        float durationSeconds = 2.5F * u + 7.0F;
        int ticks = Math.max(20, Math.round(durationSeconds * 20.0F));
        return new GaugeState(Element.PYRO, u, ticks);
    }

    /**
     * Genshin Impact Leftover Trigger Aura:
     * When a trigger attack has higher gauge units than the reacted aura,
     * the unconsumed trigger gauge lingers with the decay rate of the trigger attack.
     */
    public static GaugeState createLeftover(Element element, float leftoverUnits, float originalAttackU) {
        float safeLeftover = (Float.isNaN(leftoverUnits) || Float.isInfinite(leftoverUnits)) ? 0.01F : Math.max(0.01F, leftoverUnits);
        float safeAttackU = (Float.isNaN(originalAttackU) || Float.isInfinite(originalAttackU)) ? 1.0F : Math.max(0.1F, originalAttackU);
        float totalDurationSec = 2.5F * safeAttackU + 7.0F;
        int totalTicks = Math.max(20, Math.round(totalDurationSec * 20.0F));
        float decayPerTick = Math.max(1.0E-5F, (safeAttackU * 0.8F) / (float) totalTicks);
        int remainingTicks = Math.max(1, Math.round(safeLeftover / decayPerTick));
        return new GaugeState(element, safeLeftover, remainingTicks);
    }

    /**
     * Genshin Impact Freeze Aura formula:
     * U_Freeze = 2 * min(U_aura, U_trigger)
     * Duration = 2 * sqrt(5 * U_Freeze + 4) - 4 seconds.
     */
    public static GaugeState createFrozen(float freezeUnits) {
        float u = (Float.isNaN(freezeUnits) || Float.isInfinite(freezeUnits)) ? 0.8F : Math.max(0.1F, freezeUnits);
        double radicand = Math.max(4.0D, 5.0D * u + 4.0D);
        double durationSeconds = Math.max(0.5D, 2.0D * Math.sqrt(radicand) - 4.0D);
        int ticks = Math.max(10, (int) Math.round(durationSeconds * 20.0D));
        return new GaugeState(Element.CRYO, u, ticks);
    }

    public static GaugeState createFrozen(float freezeUnits, int durationTicks) {
        return new GaugeState(Element.CRYO, Math.max(0.1F, freezeUnits), durationTicks);
    }

    public Element getElement() {
        return element;
    }

    public float getUnits() {
        return units;
    }

    public float getDecayPerTick() {
        return decayPerTick;
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
     * Refreshes an existing aura with a new same-element application following Genshin Impact EGT:
     * - Hydro, Cryo, Electro, Dendro: inherit the original aura's decayPerTick; units = max(currentUnits, appliedUnits * 0.8).
     * - Pyro (Version 3.0+ rule): if new taxed units > currentUnits, update decayPerTick to the new trigger's decay rate;
     *   otherwise retain the current decayPerTick.
     */
    public void refresh(float appliedUnits) {
        refresh(appliedUnits, 1.0F);
    }

    public void refresh(float appliedUnits, float durationScale) {
        float taxed = Math.max(0.01F, appliedUnits * 0.8F);
        float scale = Math.max(0.1F, durationScale);
        float triggerDurationSeconds = (2.5F * appliedUnits + 7.0F) * scale;
        int triggerMaxTicks = Math.max(10, Math.round(triggerDurationSeconds * 20.0F));
        float triggerDecayPerTick = taxed / (float) triggerMaxTicks;

        if (this.element == Element.PYRO || scale < 1.0F) {
            if (taxed > this.units || scale < 1.0F || this.decayPerTick <= 1.0E-6F) {
                this.decayPerTick = triggerDecayPerTick;
            }
        } else if (this.decayPerTick <= 1.0E-6F) {
            this.decayPerTick = triggerDecayPerTick;
        }

        this.units = Math.max(this.units, taxed);
        this.maxDurationTicks = Math.max(1, Math.round(this.units / Math.max(1.0E-6F, this.decayPerTick)));
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
