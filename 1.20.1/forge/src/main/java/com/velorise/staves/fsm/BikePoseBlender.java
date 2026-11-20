package com.velorise.staves.fsm;

import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block 4 of Mavuika's 4-Block HFSM:
 * Evaluates canonical per-attack bike transforms from BikeActionState and blends smoothly
 * across state transitions (e.g. NA -> DASH, CA_SPIN_LOOP -> CA_SPIN_FINISHER, GLIDE -> PLUNGE)
 * using frame-rate-independent exponential damping (PoseBlender).
 */
public final class BikePoseBlender {
    private BikePoseBlender() {}

    /**
     * Mutable pose snapshot for FlamestriderBikeRenderer.applyActionTransforms().
     */
    public static final class BikePoseSnapshot {
        public float offsetX = 0.0F;
        public float offsetY = 0.0F;
        public float offsetZ = 0.0F;
        public float pitchDeg = 0.0F;
        public float yawDeg = 0.0F;
        public float rollDeg = 0.0F;
        public float pivotZ = 0.0F;
        public long lastUpdateNanos = 0L;
        public BikeActionState lastState = BikeActionState.IDLE;

        public void zero() {
            offsetX = 0.0F;
            offsetY = 0.0F;
            offsetZ = 0.0F;
            pitchDeg = 0.0F;
            yawDeg = 0.0F;
            rollDeg = 0.0F;
            pivotZ = 0.0F;
        }

        public void copyFrom(BikePoseSnapshot other) {
            this.offsetX = other.offsetX;
            this.offsetY = other.offsetY;
            this.offsetZ = other.offsetZ;
            this.pitchDeg = other.pitchDeg;
            this.yawDeg = other.yawDeg;
            this.rollDeg = other.rollDeg;
            this.pivotZ = other.pivotZ;
        }
    }

    private static final Map<Integer, BikePoseSnapshot> ENTITY_POSES = new ConcurrentHashMap<>();

    public static void clearAll() {
        ENTITY_POSES.clear();
    }

    public static void remove(int entityId) {
        ENTITY_POSES.remove(entityId);
    }

    /**
     * Evaluates target action pose for the active BikeActionState and smoothly blends
     * toward it so mid-combo dash cancels and finisher transitions never snap harshly.
     */
    public static BikePoseSnapshot evaluateAndBlend(
            int entityId,
            BikeActionState state,
            int ultiT,
            int plungeWindup,
            int naStep,
            float smoothNaTicks,
            float finisherProgress,
            float partialTicks
    ) {
        BikePoseSnapshot target = new BikePoseSnapshot();
        switch (state) {
            case GLIDE -> evalGlidePose(target);
            case CA_SPIN_LOOP -> evalSpinPose(target);
            case PLUNGE_WINDUP, PLUNGE_DIVE -> evalPlungePose(plungeWindup, partialTicks, target);
            case ULTI_BURST -> evalUltiPose(ultiT, partialTicks, target);
            case NA_1, NA_2, NA_3, NA_4, NA_5 -> evalNaPose(naStep, smoothNaTicks, target);
            case CA_SPIN_FINISHER -> evalFinisherPose(finisherProgress, target);
            case MOUNT_TAIL_WHIP -> evalMountTailWhipPose(smoothNaTicks, target);
            case DASH -> evalDashPose(target);
            default -> target.zero();
        }

        BikePoseSnapshot current = ENTITY_POSES.computeIfAbsent(entityId, k -> new BikePoseSnapshot());
        long nowNanos = System.nanoTime();
        // Fast stiffness during high-speed flips/bursts so arc fidelity is 100% crisp,
        // softer damping when settling back to IDLE/DRIVE/GLIDE or dash-canceling.
        boolean snapArc = (state == BikeActionState.ULTI_BURST
                || state == BikeActionState.NA_5
                || state == BikeActionState.CA_SPIN_FINISHER
                || state == BikeActionState.MOUNT_TAIL_WHIP);
        if (current.lastUpdateNanos <= 0L || (snapArc && current.lastState == state)) {
            current.copyFrom(target);
            current.lastUpdateNanos = nowNanos;
            current.lastState = state;
            return current;
        }

        float dt = PoseBlender.clampDtSeconds(nowNanos, current.lastUpdateNanos);
        current.lastUpdateNanos = nowNanos;
        float stiffness = state.isCombatState() ? 32.0F : 18.0F;
        current.offsetX = PoseBlender.damp(current.offsetX, target.offsetX, stiffness, dt);
        current.offsetY = PoseBlender.damp(current.offsetY, target.offsetY, stiffness, dt);
        current.offsetZ = PoseBlender.damp(current.offsetZ, target.offsetZ, stiffness, dt);
        current.pitchDeg = PoseBlender.damp(current.pitchDeg, target.pitchDeg, stiffness, dt);
        current.yawDeg = PoseBlender.damp(current.yawDeg, target.yawDeg, stiffness, dt);
        current.rollDeg = PoseBlender.damp(current.rollDeg, target.rollDeg, stiffness, dt);
        current.pivotZ = PoseBlender.damp(current.pivotZ, target.pivotZ, stiffness, dt);
        current.lastState = state;
        return current;
    }

    private static void evalGlidePose(BikePoseSnapshot out) {
        out.pitchDeg = -45.0F;
    }

    private static void evalDashPose(BikePoseSnapshot out) {
        out.pitchDeg = -7.0F;
        out.offsetZ = 0.08F;
    }

    private static void evalSpinPose(BikePoseSnapshot out) {
        // Genshin Donut Spin: front tire planted, rear wheel slightly lifted & drifting outward
        out.offsetY = 0.06F;
        out.pitchDeg = 7.5F;
    }

    private static void evalPlungePose(int windup, float partialTicks, BikePoseSnapshot out) {
        if (windup > 0) {
            float w = Mth.clamp((5.0F - ((float) windup - partialTicks)) / 5.0F, 0.0F, 1.0F);
            out.offsetY = (float) (Math.sin(w * Math.PI) * 0.18D);
            out.pitchDeg = Mth.lerp(w, -28.0F, 62.0F);
        } else {
            out.pitchDeg = 62.0F;
        }
    }

    private static void evalUltiPose(int ultiT, float partialTicks, BikePoseSnapshot out) {
        float u = Math.max(0.0F, (float) ultiT - partialTicks);
        if (u > 8.0F) {
            if (u >= 19.0F) {
                // Half 1 (u: 30.0 -> 19.0): steady climb + 180-deg half-donut spin behind/beside rider
                float p1 = Mth.clamp((30.0F - u) / 11.0F, 0.0F, 1.0F);
                float s1 = p1 * p1 * (3.0F - 2.0F * p1);
                float theta = s1 * (float) Math.PI;
                out.offsetX = Mth.sin(theta) * 1.15F;
                out.offsetY = p1 * 0.85F;
                out.offsetZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                out.yawDeg = -s1 * 180.0F;
                out.rollDeg = -Mth.sin(theta) * 32.0F;
                out.pitchDeg = -Mth.sin(theta) * 20.0F;
            } else {
                // Half 2 (u: 19.0 -> 8.0): complete loop 180 -> 360 deg forward, descend, and cleave at touchdown
                float p2 = Mth.clamp((19.0F - u) / 11.0F, 0.0F, 1.0F);
                float s2 = p2 * p2 * (3.0F - 2.0F * p2);
                float theta = (float) Math.PI * (1.0F + s2);
                out.offsetX = Mth.sin(theta) * 1.15F;
                out.offsetY = (1.0F - p2 * p2) * 0.85F;
                out.offsetZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                out.yawDeg = -180.0F - s2 * 180.0F;
                out.rollDeg = Mth.sin(s2 * (float) Math.PI) * 28.0F;
                out.pitchDeg = Mth.sin(s2 * (float) Math.PI) * 22.0F;
            }
        } else {
            float rec = Mth.clamp(u / 8.0F, 0.0F, 1.0F);
            out.offsetZ = rec * 0.25F;
            out.rollDeg = rec * 14.0F;
            out.pitchDeg = rec * 8.0F;
        }
    }

    private static void evalNaPose(int step, float naTf, BikePoseSnapshot out) {
        if (step == 1) {
            float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
            float s = p * p * (3.0F - 2.0F * p);
            float swing = Mth.sin(p * (float) Math.PI);
            float pitch = (s < 0.45F)
                    ? Mth.lerp(s / 0.45F, 0.0F, -16.0F)
                    : Mth.lerp((s - 0.45F) / 0.55F, -16.0F, 14.0F) * (1.0F - p * 0.65F);
            out.offsetY = swing * 0.16F;
            out.offsetZ = swing * 0.10F;
            out.yawDeg = Mth.lerp(s, 10.0F, -14.0F) * swing;
            out.pitchDeg = pitch;
            out.rollDeg = 10.0F * swing;
        } else if (step == 2) {
            float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
            float s = p * p * (3.0F - 2.0F * p);
            float swing = Mth.sin(p * (float) Math.PI);
            out.offsetY = swing * 0.10F;
            out.yawDeg = Mth.lerp(s, -34.0F, 36.0F) * swing;
            out.rollDeg = 20.0F * swing;
            out.pitchDeg = 8.0F * swing;
        } else if (step == 3) {
            float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
            float s = p * p * (3.0F - 2.0F * p);
            float swing = Mth.sin(p * (float) Math.PI);
            out.offsetY = swing * 0.08F;
            out.offsetZ = swing * 0.12F;
            out.yawDeg = Mth.lerp(s, 38.0F, -38.0F) * swing;
            out.rollDeg = -22.0F * swing;
        } else if (step == 4) {
            float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
            float s = p * p * (3.0F - 2.0F * p);
            float swing = Mth.sin(p * (float) Math.PI);
            float pitch = (s < 0.52F)
                    ? Mth.lerp(s / 0.52F, 0.0F, -34.0F)
                    : Mth.lerp((s - 0.52F) / 0.48F, -34.0F, 18.0F) * (1.0F - (p - 0.52F));
            out.offsetY = swing * 0.36F;
            out.pitchDeg = pitch;
            out.rollDeg = -12.0F * swing;
        } else if (step == 5) {
            float p = Mth.clamp(1.0F - (naTf / 14.0F), 0.0F, 1.0F);
            float s = p * p * (3.0F - 2.0F * p);
            out.offsetY = (float) (Math.sin(p * Math.PI) * 0.88D);
            out.pitchDeg = s * 360.0F;
        }
    }

    private static void evalMountTailWhipPose(float naTf, BikePoseSnapshot out) {
        // Genshin Hold E Mount: 180-deg half-donut rear-wheel tail whip around front tire pivot
        float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
        float s = p * p * (3.0F - 2.0F * p);
        float swing = Mth.sin(p * (float) Math.PI);
        out.pivotZ = 0.72F;
        out.offsetY = swing * 0.10F;
        out.yawDeg = (1.0F - s) * 360.0F;
        out.rollDeg = 26.0F * swing;
        out.pitchDeg = 12.0F * swing;
    }

    private static void evalFinisherPose(float p, BikePoseSnapshot out) {
        float jumpArc = (p < 0.72F) ? Mth.sin((p / 0.72F) * (float) Math.PI) * 0.68F : 0.0F;
        float surgeZ = Mth.sin(p * (float) Math.PI) * 0.20F;
        out.offsetY = jumpArc;
        out.offsetZ = surgeZ;
        if (p < 0.36F) {
            float e = 0.5F - 0.5F * Mth.cos((p / 0.36F) * (float) Math.PI);
            out.yawDeg = Mth.lerp(e, 0.0F, -8.0F);
            out.pitchDeg = Mth.lerp(e, 0.0F, -22.0F);
            out.rollDeg = Mth.lerp(e, 0.0F, -8.0F);
        } else if (p < 0.72F) {
            float e = 0.5F - 0.5F * Mth.cos(((p - 0.36F) / 0.36F) * (float) Math.PI);
            out.yawDeg = Mth.lerp(e, -8.0F, 10.0F);
            out.pitchDeg = Mth.lerp(e, -22.0F, 14.0F);
            out.rollDeg = Mth.lerp(e, -8.0F, 10.0F);
        } else {
            float e = 0.5F - 0.5F * Mth.cos(((p - 0.72F) / 0.28F) * (float) Math.PI);
            out.yawDeg = Mth.lerp(e, 10.0F, 0.0F);
            out.pitchDeg = Mth.lerp(e, 14.0F, 0.0F);
            out.rollDeg = Mth.lerp(e, 10.0F, 0.0F);
        }
    }
}
