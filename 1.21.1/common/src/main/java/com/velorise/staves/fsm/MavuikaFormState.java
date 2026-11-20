package com.velorise.staves.fsm;

import net.minecraft.util.Mth;

/**
 * Block 1 of Mavuika's 4-Block HFSM:
 * Tracks high-level Nightsoul form (NONE / RING_OF_SEARING_RADIANCE / FLAMESTRIDER),
 * 3 independent terrain traversal meters (wall climb, water surf, mid-air glide),
 * and Genshin-authentic additive Nightsoul drain calculation.
 */
public final class MavuikaFormState {
    public enum Form {
        NONE,
        RING_OF_SEARING_RADIANCE,
        FLAMESTRIDER
    }

    // Additive Nightsoul drain rates (points per tick at 20 TPS)
    public static final float DRAIN_RING_BASE = 0.325F;          // ~6.5 pts/s off-bike Sun Ring
    public static final float DRAIN_BIKE_BASE = 0.45F;           // 9.0 pts/s base Flamestrider riding
    public static final float DRAIN_ADD_DASH = 1.00F;            // +20.0 pts/s while dashing (total 1.45)
    public static final float DRAIN_ADD_WALL_CLIMB = 0.20F;      // +4.0 pts/s cliff climbing (total 0.65)
    public static final float DRAIN_ADD_WATER_SURF = 0.25F;      // +5.0 pts/s surfing water (total 0.70)
    public static final float DRAIN_ADD_GLIDE = 0.15F;           // +3.0 pts/s mid-air gliding (total 0.60)
    public static final float DRAIN_ADD_SPRINT = 0.25F;          // +5.0 pts/s hold-sprint (total 0.70)
    public static final float DRAIN_ADD_SPIN = 0.10F;            // +2.0 pts/s Charged Attack donut spin (total 0.55)
    public static final float DRAIN_MAX_CAP = 1.65F;             // Hard cap so stacked terrain+dash never drains instantly

    // 3 independent terrain meter limits (ticks at 20 TPS)
    public static final int MAX_WALL_CLIMB_TICKS = 60;           // 3.0s continuous cliff climb per ground touch
    public static final int MAX_WATER_SURF_TICKS = 100;          // 5.0s continuous water surf per ground touch
    public static final int MAX_GLIDE_TICKS = 60;                // 3.0s mid-air glide per air charge

    private Form form = Form.NONE;
    private int wallClimbTicksRemaining = MAX_WALL_CLIMB_TICKS;
    private int waterSurfTicksRemaining = MAX_WATER_SURF_TICKS;
    private int glideTicksRemaining = MAX_GLIDE_TICKS;
    private int glideCharges = 1;

    public Form getForm() {
        return form;
    }

    public void setForm(Form nextForm) {
        if (nextForm == null) nextForm = Form.NONE;
        if (this.form != nextForm) {
            this.form = nextForm;
            if (nextForm == Form.NONE) {
                resetTerrainMeters();
            }
        }
    }

    public void resetTerrainMeters() {
        this.wallClimbTicksRemaining = MAX_WALL_CLIMB_TICKS;
        this.waterSurfTicksRemaining = MAX_WATER_SURF_TICKS;
        this.glideTicksRemaining = MAX_GLIDE_TICKS;
        this.glideCharges = 1;
    }

    public void rechargeOnGround() {
        this.wallClimbTicksRemaining = Math.min(MAX_WALL_CLIMB_TICKS, this.wallClimbTicksRemaining + 6);
        this.waterSurfTicksRemaining = Math.min(MAX_WATER_SURF_TICKS, this.waterSurfTicksRemaining + 8);
        this.glideTicksRemaining = MAX_GLIDE_TICKS;
        this.glideCharges = 1;
    }

    public boolean consumeWallClimbTick() {
        if (wallClimbTicksRemaining <= 0) return false;
        wallClimbTicksRemaining--;
        return true;
    }

    public boolean consumeWaterSurfTick() {
        if (waterSurfTicksRemaining <= 0) return false;
        waterSurfTicksRemaining--;
        return true;
    }

    public boolean consumeGlideTick() {
        if (glideTicksRemaining <= 0) return false;
        glideTicksRemaining--;
        return true;
    }

    public boolean tryStartGlideCharge() {
        if (glideCharges <= 0 || glideTicksRemaining <= 0) return false;
        glideCharges--;
        return true;
    }

    public int getWallClimbTicksRemaining() {
        return wallClimbTicksRemaining;
    }

    public int getWaterSurfTicksRemaining() {
        return waterSurfTicksRemaining;
    }

    public int getGlideTicksRemaining() {
        return glideTicksRemaining;
    }

    public int getGlideCharges() {
        return glideCharges;
    }

    public float getWallClimbFraction() {
        return Mth.clamp(wallClimbTicksRemaining / (float) MAX_WALL_CLIMB_TICKS, 0.0F, 1.0F);
    }

    public float getWaterSurfFraction() {
        return Mth.clamp(waterSurfTicksRemaining / (float) MAX_WATER_SURF_TICKS, 0.0F, 1.0F);
    }

    public float getGlideFraction() {
        return Mth.clamp(glideTicksRemaining / (float) MAX_GLIDE_TICKS, 0.0F, 1.0F);
    }

    /**
     * Computes Genshin-authentic additive Nightsoul consumption per tick.
     * Frozen at 0.0 during Crucible of Death and Life (7s burst state).
     */
    public static float computeDrainPerTick(
            Form form,
            boolean crucibleActive,
            boolean dashing,
            boolean wallClimbing,
            boolean waterSurfing,
            boolean gliding,
            boolean sprinting,
            boolean spinning
    ) {
        if (crucibleActive || form == Form.NONE) {
            return 0.0F;
        }
        if (form == Form.RING_OF_SEARING_RADIANCE) {
            return DRAIN_RING_BASE;
        }
        float drain = DRAIN_BIKE_BASE;
        if (dashing) drain += DRAIN_ADD_DASH;
        if (wallClimbing) drain += DRAIN_ADD_WALL_CLIMB;
        if (waterSurfing) drain += DRAIN_ADD_WATER_SURF;
        if (gliding) drain += DRAIN_ADD_GLIDE;
        if (sprinting && !dashing) drain += DRAIN_ADD_SPRINT;
        if (spinning && !dashing) drain += DRAIN_ADD_SPIN;
        return Math.min(DRAIN_MAX_CAP, drain);
    }
}
