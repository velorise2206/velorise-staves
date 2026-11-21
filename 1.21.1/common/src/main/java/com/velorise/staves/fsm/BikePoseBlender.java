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

    public static BikePoseSnapshot getCurrentPose(int entityId) {
        return ENTITY_POSES.get(entityId);
    }

    public static boolean hasActivePose(int entityId) {
        BikePoseSnapshot p = ENTITY_POSES.get(entityId);
        if (p == null) return false;
        return Math.abs(p.offsetX) > 0.002F || Math.abs(p.offsetY) > 0.002F || Math.abs(p.offsetZ) > 0.002F
                || Math.abs(p.pitchDeg) > 0.1F || Math.abs(p.yawDeg) > 0.1F || Math.abs(p.rollDeg) > 0.1F;
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
            case CA_SPIN_HOLD, CA_SPIN_RELEASE_GRACE, CA_SPIN_TAP_ARMED, CA_SPIN_CONTINUOUS -> evalSpinPose(target);
            case PLUNGE_WINDUP, PLUNGE_DIVE -> evalPlungePose(plungeWindup, partialTicks, target);
            case ULTI_BURST -> evalUltiPose(ultiT, partialTicks, target);
            case NA_1, NA_2, NA_3, NA_4, NA_5 -> evalNaPose(naStep, smoothNaTicks, target);
            case CA_SPIN_FINISHER -> evalFinisherPose(finisherProgress, target);
            case MOUNT_TAIL_WHIP -> evalMountTailWhipPose(smoothNaTicks, target);
            case MOUNT_CATCH -> evalMountCatchPose(smoothNaTicks, target);
            case DASH, DASH_CHARGE_CANCEL -> evalDashPose(target);
            default -> target.zero();
        }

        BikePoseSnapshot current = ENTITY_POSES.computeIfAbsent(entityId, k -> new BikePoseSnapshot());
        long nowNanos = System.nanoTime();
        // Fast stiffness during high-speed flips/bursts so arc fidelity is 100% crisp,
        // softer damping when settling back to IDLE/DRIVE/GLIDE or dash-canceling.
        boolean snapArc = (state == BikeActionState.ULTI_BURST
                || state == BikeActionState.NA_5
                || state == BikeActionState.CA_SPIN_FINISHER
                || state == BikeActionState.MOUNT_TAIL_WHIP
                || state == BikeActionState.MOUNT_CATCH);
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
        // Genshin Flamestrider Dash: crisp wheelie pop lifting the front nose up with forward surge
        out.pitchDeg = -20.0F;
        out.offsetY = 0.12F;
        out.offsetZ = 0.14F;
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
            // Genshin NA1 (9t): Aggressive sideways drift left (-62 deg) with wide right-to-left cleave
            float p = Mth.clamp(1.0F - (naTf / 9.0F), 0.0F, 1.0F);
            float swing = Mth.sin(p * (float) Math.PI);
            float s = p * p * (3.0F - 2.0F * p);
            out.offsetY = swing * 0.18F;
            out.offsetZ = swing * 0.22F;
            out.yawDeg = Mth.lerp(s, 14.0F, -62.0F) * swing;
            out.rollDeg = -24.0F * swing;
            out.pitchDeg = -10.0F * swing;
        } else if (step == 2) {
            // Genshin NA2 (11t): 180-deg counter-spin tail whip around front tire (+68 deg) carving horizontal slash
            float p = Mth.clamp(1.0F - (naTf / 11.0F), 0.0F, 1.0F);
            float s = p * p * (3.0F - 2.0F * p);
            float swing = Mth.sin(p * (float) Math.PI);
            out.pivotZ = 0.62F;
            out.offsetY = swing * 0.20F;
            out.yawDeg = Mth.lerp(s, -58.0F, 68.0F) * swing;
            out.rollDeg = 28.0F * swing;
            out.pitchDeg = -12.0F * swing;
        } else if (step == 3) {
            // Genshin NA3 (12t): Vertical front-wheelie stoppie (tail kicks +54 deg high!) -> heavy rear-tire fire slam!
            float p = Mth.clamp(1.0F - (naTf / 12.0F), 0.0F, 1.0F);
            if (p < 0.42F) {
                float u = p / 0.42F;
                float e = 1.0F - (1.0F - u) * (1.0F - u);
                out.offsetY = e * 0.42F;
                out.offsetZ = e * 0.16F;
                out.pitchDeg = e * 54.0F; // nose down, rear tire kicked high!
                out.rollDeg = -10.0F * e;
            } else if (p < 0.60F) {
                float u = (p - 0.42F) / 0.18F;
                float slam = u * u;
                out.offsetY = Mth.lerp(slam, 0.42F, -0.08F);
                out.offsetZ = Mth.lerp(slam, 0.16F, 0.04F);
                out.pitchDeg = Mth.lerp(slam, 54.0F, -14.0F);
                out.rollDeg = Mth.lerp(slam, -10.0F, 8.0F);
            } else {
                float u = (p - 0.60F) / 0.40F;
                float e = u * u * (3.0F - 2.0F * u);
                out.offsetY = Mth.lerp(e, -0.08F, 0.0F);
                out.pitchDeg = Mth.lerp(e, -14.0F, 0.0F);
                out.rollDeg = Mth.lerp(e, 8.0F, 0.0F);
            }
        } else if (step == 4) {
            // Genshin NA4 (10t): High wheelie side-drift right (+68 deg, roll +30 deg, nose-up -26 deg) uppercut!
            float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
            float swing = Mth.sin(p * (float) Math.PI);
            float s = p * p * (3.0F - 2.0F * p);
            out.offsetY = swing * 0.36F;
            out.offsetZ = swing * 0.18F;
            out.yawDeg = Mth.lerp(s, -18.0F, 68.0F) * swing;
            out.rollDeg = 30.0F * swing;
            out.pitchDeg = -26.0F * swing;
        } else if (step == 5) {
            // Genshin NA5 (23t): Massive airborne 360-deg front-flip Superman slam (+1.45m apex -> hitmark tick 12 -> wheelie bounce!)
            float p = Mth.clamp(1.0F - (naTf / 23.0F), 0.0F, 1.0F);
            if (p < 0.32F) {
                float u = p / 0.32F;
                out.offsetY = (1.0F - (1.0F - u) * (1.0F - u)) * 1.45F;
                out.offsetZ = u * 0.35F;
                out.pitchDeg = u * 205.0F;
            } else if (p < 0.52F) {
                float u = (p - 0.32F) / 0.20F;
                out.offsetY = (1.0F - (float) Math.pow(u, 2.5D)) * 1.45F;
                out.offsetZ = Mth.lerp(u, 0.35F, 0.20F);
                out.pitchDeg = 205.0F + u * 155.0F;
            } else if (p < 0.62F) {
                float u = (p - 0.52F) / 0.10F;
                out.offsetY = -Mth.sin(u * (float) Math.PI) * 0.14F;
                out.pitchDeg = Mth.lerp(u, 10.0F, -22.0F);
            } else if (p < 0.80F) {
                float u = (p - 0.62F) / 0.18F;
                out.offsetY = Mth.sin((1.0F - u) * (float) Math.PI * 0.5F) * 0.10F;
                out.pitchDeg = Mth.lerp(u, -22.0F, 0.0F);
            } else {
                out.offsetY = 0.0F;
                out.pitchDeg = 0.0F;
            }
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

    private static void evalMountCatchPose(float naTf, BikePoseSnapshot out) {
        // Genshin Re-Mount: translucent bike materializes behind the leaping rider (-2.4m) and rushes forward to scoop them up
        float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
        if (p < 0.55F) {
            float u = p / 0.55F;
            float easeOut = 1.0F - (1.0F - u) * (1.0F - u);
            out.offsetZ = Mth.lerp(easeOut, -2.40F, 0.0F);
            out.offsetY = Mth.sin(u * (float) Math.PI) * 0.10F;
            out.pitchDeg = Mth.lerp(easeOut, -11.0F, -2.0F);
        } else {
            float u = (p - 0.55F) / 0.45F;
            out.offsetZ = 0.0F;
            out.offsetY = -Mth.sin(u * (float) Math.PI) * 0.06F;
            out.pitchDeg = Mth.lerp(u, -2.0F, 0.0F);
        }
    }

    private static void evalFinisherPose(float p, BikePoseSnapshot out) {
        // Authentic 27-tick Genshin Finisher bike choreography (measured 12fps from Genshin video):
        // Phase 0 (0.00..0.11, 3t): grounded pull onto forward centerline
        // Phase 1 (0.11..0.37, 7t): bike glides low & sleek beneath the airborne vaulting rider
        // Phase 2 (0.37..0.52, 4t): rider plunges onto saddle -> heavy impact compression at hitmark (p = 0.52, tick 14)
        // Phase 3a (0.52..0.60): explosive pop into sideways wheelie tail-slide
        // Phase 3b (0.60..0.85): iconic Genshin sideways nose-up wheelie hold (pitch -34 deg, yaw +28 deg, roll +14 deg)
        // Phase 3c (0.85..1.00): smooth front-wheel touchdown back to 0 deg
        if (p < 0.11F) {
            float u = p / 0.11F;
            out.offsetY = 0.0F;
            out.offsetZ = 0.0F;
            out.yawDeg = Mth.lerp(u, 0.0F, -4.0F);
            out.pitchDeg = Mth.sin(u * (float) Math.PI) * 3.5F;
            out.rollDeg = Mth.lerp(u, 0.0F, -3.0F);
        } else if (p < 0.37F) {
            // Bike curves noticeably back-left underneath the vaulting rider (Genshin 10.55s..10.92s)
            float u = (p - 0.11F) / 0.26F;
            float s = Mth.sin(u * (float) Math.PI);
            out.offsetX = -s * 0.58F;
            out.offsetY = s * 0.08F;
            out.offsetZ = -s * 0.65F;
            out.yawDeg = Mth.lerp(u, -4.0F, -28.0F);
            out.pitchDeg = Mth.lerp(u, 0.0F, -8.0F);
            out.rollDeg = Mth.lerp(u, -3.0F, -16.0F);
        } else if (p < 0.52F) {
            // Bike swings cleanly back underneath rider right on the horizontal cleave (Genshin 10.98s..11.08s)
            float u = (p - 0.37F) / 0.15F;
            float e = u * u;
            out.offsetX = Mth.lerp(e, -0.34F, 0.0F);
            out.offsetY = Mth.lerp(e, 0.06F, -0.08F);
            out.offsetZ = Mth.lerp(e, -0.38F, 0.08F);
            out.yawDeg = Mth.lerp(e, -28.0F, 6.0F);
            out.pitchDeg = Mth.lerp(e, -8.0F, 10.0F);
            out.rollDeg = Mth.lerp(e, -16.0F, 5.0F);
        } else if (p < 0.58F) {
            // Hold low grounded cleave impact while giant horizontal slash sweeps left-to-right!
            float u = (p - 0.52F) / 0.06F;
            out.offsetY = Mth.lerp(u, -0.08F, -0.04F);
            out.offsetZ = 0.08F;
            out.yawDeg = Mth.lerp(u, 6.0F, 18.0F);
            out.pitchDeg = Mth.lerp(u, 10.0F, 0.0F);
            out.rollDeg = Mth.lerp(u, 5.0F, 8.0F);
        } else if (p < 0.68F) {
            // Whip 58 deg sideways & pop 40 deg nose-up wheelie AFTER the slash blooms (Genshin 11.25s..11.48s)
            float u = (p - 0.58F) / 0.10F;
            float e = 1.0F - (1.0F - u) * (1.0F - u);
            out.offsetY = Mth.lerp(e, -0.04F, 0.22F);
            out.offsetZ = Mth.lerp(e, 0.08F, -0.14F);
            out.yawDeg = Mth.lerp(e, 18.0F, 58.0F);
            out.pitchDeg = Mth.lerp(e, 0.0F, -40.0F);
            out.rollDeg = Mth.lerp(e, 8.0F, 16.0F);
        } else if (p < 0.86F) {
            float u = (p - 0.68F) / 0.18F;
            float settle = 1.0F - u * 0.14F;
            out.offsetY = 0.22F * settle;
            out.offsetZ = -0.14F * settle;
            out.yawDeg = 58.0F * (1.0F - u * 0.08F);
            out.pitchDeg = -40.0F * settle;
            out.rollDeg = 16.0F * settle;
        } else {
            float u = Mth.clamp((p - 0.86F) / 0.14F, 0.0F, 1.0F);
            float e = u * u * (3.0F - 2.0F * u);
            out.offsetY = Mth.lerp(e, 0.189F, 0.0F);
            out.offsetZ = Mth.lerp(e, -0.120F, 0.0F);
            out.yawDeg = Mth.lerp(e, 53.4F, 0.0F);
            out.pitchDeg = Mth.lerp(e, -34.4F, 0.0F);
            out.rollDeg = Mth.lerp(e, 13.8F, 0.0F);
        }
    }
}
