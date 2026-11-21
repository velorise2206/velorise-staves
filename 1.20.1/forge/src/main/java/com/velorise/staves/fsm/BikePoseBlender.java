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
                || state == BikeActionState.NA_2
                || state == BikeActionState.NA_3
                || state == BikeActionState.NA_4
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
        boolean stateChanged = current.lastState != state;
        if (stateChanged) {
            // Normalize wrapped yaw/pitch on state boundary so 360-deg spins seamlessly connect to 0 deg without unwinding backward
            current.yawDeg = Mth.wrapDegrees(current.yawDeg);
            current.pitchDeg = Mth.wrapDegrees(current.pitchDeg);
            if (state == BikeActionState.NA_4 && current.yawDeg > 0.0F) {
                current.yawDeg -= 360.0F;
            }
        }
        float stiffness = state.isCombatState() ? 26.0F : 14.0F;
        current.offsetX = PoseBlender.damp(current.offsetX, target.offsetX, stiffness, dt);
        current.offsetY = PoseBlender.damp(current.offsetY, target.offsetY, stiffness, dt);
        current.offsetZ = PoseBlender.damp(current.offsetZ, target.offsetZ, stiffness, dt);
        current.pitchDeg = PoseBlender.damp(current.pitchDeg, target.pitchDeg, stiffness, dt);
        if (state.isCombatState()) {
            current.yawDeg = PoseBlender.damp(current.yawDeg, target.yawDeg, stiffness, dt);
        } else {
            current.yawDeg = PoseBlender.dampAngleDeg(current.yawDeg, target.yawDeg, stiffness, dt);
        }
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

    /**
     * Evaluates the Flamestrider bike's 40-tick Genshin Co-op Ultimate pose:
     * - T00..T08: Sky launch (+4.8m Y, +1.8m forward Z, -38 deg nose-up toward sun)
     * - T08..T14: Bike separates from leaping rider and rockets forward-up (+7.2m Y, +6.5m Z) while fading out
     * - T14..T25: Bike hidden (scale = 0 in FlamestriderBikeRenderer)
     * - T25..T34: Re-summoned directly under Steve in iconic sideways wheelie (+68 deg yaw LEFT, -36 deg nose-up, +12 deg roll)
     * - T34..T40: Smooth wheelie touchdown back to 0 deg
     */
    private static void evalUltiPose(int ultiT, float partialTicks, BikePoseSnapshot out) {
        float u = Math.max(0.0F, (float) ultiT - partialTicks);
        float t = Mth.clamp(40.0F - u, 0.0F, 40.0F);
        if (t < 8.0F) {
            float e = easeOutQuad(t / 8.0F);
            out.offsetY = e * 4.80F;
            out.offsetZ = e * 1.80F;
            out.pitchDeg = Mth.lerp(e, 0.0F, -38.0F);
        } else if (t < 14.0F) {
            float e = (t - 8.0F) / 6.0F;
            out.offsetY = Mth.lerp(e, 4.80F, 7.20F);
            out.offsetZ = Mth.lerp(e, 1.80F, 6.50F);
            out.pitchDeg = Mth.lerp(e, -38.0F, -44.0F);
        } else if (t < 25.0F) {
            // Hidden mid-air while Steve meteor-dives and cleaves
            out.offsetY = 0.0F;
            out.offsetZ = 0.0F;
            out.yawDeg = 68.0F;
            out.pitchDeg = -36.0F;
            out.rollDeg = 12.0F;
        } else if (t < 34.0F) {
            // Re-summoned iconic sideways wheelie hold (nose LEFT +68 deg, nose-up -36 deg, roll +12 deg)
            float settle = t < 27.0F ? easeOutCubic((t - 25.0F) / 2.0F) : 1.0F;
            out.offsetY = Mth.lerp(settle, 0.04F, 0.18F);
            out.offsetZ = 0.0F;
            out.yawDeg = 68.0F;
            out.pitchDeg = Mth.lerp(settle, -18.0F, -36.0F);
            out.rollDeg = Mth.lerp(settle, 6.0F, 12.0F);
        } else {
            float e = easeInOutSine((t - 34.0F) / 6.0F);
            out.offsetY = Mth.lerp(e, 0.18F, 0.0F);
            out.offsetZ = 0.0F;
            out.yawDeg = Mth.lerp(e, 68.0F, 0.0F);
            out.pitchDeg = Mth.lerp(e, -36.0F, 0.0F);
            out.rollDeg = Mth.lerp(e, 12.0F, 0.0F);
        }
    }

    /**
     * Evaluates the detached rider (Steve/Mavuika) transform during the 40-tick Ultimate:
     * - T00..T08: Rides the launching bike up to +4.8m
     * - T08..T14: Ejects off bike at sky apex (+5.2m) while bike flies away
     * - T14..T22: Vertical meteor dive straight down to +0.35m & windup
     * - T22..T25: Ground-skimming 360-deg Sunfell Slice cleave
     * - T25..T40: Reunited with re-summoned bike in sideways wheelie hold -> touchdown
     */
    public static void evalUltiRiderPose(int ultiT, float partialTicks, BikePoseSnapshot out) {
        out.zero();
        float u = Math.max(0.0F, (float) ultiT - partialTicks);
        float t = Mth.clamp(40.0F - u, 0.0F, 40.0F);
        if (t < 8.0F) {
            evalUltiPose(ultiT, partialTicks, out);
        } else if (t < 14.0F) {
            float e = easeOutQuad((t - 8.0F) / 6.0F);
            out.offsetY = Mth.lerp(e, 4.80F, 5.20F);
            out.offsetZ = Mth.lerp(e, 1.80F, 1.05F);
            out.pitchDeg = Mth.lerp(e, -28.0F, 12.0F);
        } else if (t < 19.0F) {
            float e = easeInQuad((t - 14.0F) / 5.0F);
            out.offsetY = Mth.lerp(e, 5.20F, 0.85F);
            out.offsetZ = Mth.lerp(e, 1.05F, 0.15F);
            out.pitchDeg = Mth.lerp(e, 12.0F, 22.0F);
        } else if (t < 22.0F) {
            float e = easeOutCubic((t - 19.0F) / 3.0F);
            out.offsetY = Mth.lerp(e, 0.85F, 0.32F);
            out.offsetZ = Mth.lerp(e, 0.15F, 0.0F);
            out.pitchDeg = Mth.lerp(e, 22.0F, 4.0F);
            out.yawDeg = Mth.lerp(e, 0.0F, -24.0F);
        } else if (t < 25.0F) {
            float e = easeOutCubic((t - 22.0F) / 3.0F);
            out.offsetY = Mth.lerp(e, 0.32F, 0.04F);
            out.pitchDeg = Mth.lerp(e, 4.0F, 0.0F);
            out.yawDeg = Mth.lerp(e, -24.0F, 68.0F);
        } else {
            evalUltiPose(ultiT, partialTicks, out);
        }
    }

    public static float getUltiBikeVisibility(int ultiT, float partialTicks) {
        if (ultiT <= 0) return 1.0F;
        float t = Mth.clamp(40.0F - Math.max(0.0F, (float) ultiT - partialTicks), 0.0F, 40.0F);
        if (t < 10.0F) return 1.0F;
        if (t < 13.5F) return Mth.clamp(1.0F - (t - 10.0F) / 3.5F, 0.0F, 1.0F);
        if (t < 25.0F) return 0.0F;
        if (t < 27.0F) return Mth.clamp((t - 25.0F) / 2.0F, 0.0F, 1.0F);
        return 1.0F;
    }

    private static float easeInQuad(float u) {
        return u * u;
    }

    private static float easeOutQuad(float u) {
        float inv = 1.0F - u;
        return 1.0F - inv * inv;
    }

    private static float easeOutCubic(float u) {
        float inv = 1.0F - u;
        return 1.0F - inv * inv * inv;
    }

    private static float easeInCubic(float u) {
        return u * u * u;
    }

    private static float easeInOutSine(float u) {
        return 0.5F - 0.5F * Mth.cos(u * (float) Math.PI);
    }

    private static void evalNaPose(int step, float naTf, BikePoseSnapshot out) {
        // Minecraft Axis.YP sign convention: +yawDeg turns nose LEFT (CCW from above), -yawDeg turns nose RIGHT (CW from above).
        if (step == 1) {
            // Genshin NA1 (22t full, cancel T11): Windup right (-10 deg) -> Drift nose LEFT (+85 deg, tail right) -> Hold sideways (+78 deg) -> Smooth slow recover to 0 deg
            float t = Mth.clamp(22.0F - naTf, 0.0F, 22.0F);
            if (t < 3.0F) {
                float e = easeInQuad(t / 3.0F);
                out.yawDeg = Mth.lerp(e, 0.0F, -10.0F);
                out.pitchDeg = Mth.lerp(e, 0.0F, -4.0F);
                out.rollDeg = Mth.lerp(e, 0.0F, -6.0F);
                out.offsetZ = Mth.lerp(e, 0.0F, 0.20F);
            } else if (t < 7.0F) {
                float u = (t - 3.0F) / 4.0F;
                float e = easeOutCubic(u);
                out.yawDeg = Mth.lerp(e, -10.0F, 85.0F);
                out.pitchDeg = Mth.lerp(e, -4.0F, -8.0F);
                out.rollDeg = Mth.lerp(e, -6.0F, 22.0F);
                out.offsetY = Mth.sin(u * (float) Math.PI) * 0.12F;
                out.offsetZ = Mth.lerp(e, 0.20F, 0.45F);
            } else if (t < 12.0F) {
                float e = easeInOutSine((t - 7.0F) / 5.0F);
                out.yawDeg = Mth.lerp(e, 85.0F, 78.0F);
                out.pitchDeg = Mth.lerp(e, -8.0F, -2.0F);
                out.rollDeg = Mth.lerp(e, 22.0F, 10.0F);
                out.offsetZ = Mth.lerp(e, 0.45F, 0.36F);
            } else {
                float e = easeInOutSine((t - 12.0F) / 10.0F);
                out.yawDeg = Mth.lerp(e, 78.0F, 0.0F);
                out.pitchDeg = Mth.lerp(e, -2.0F, 0.0F);
                out.rollDeg = Mth.lerp(e, 10.0F, 0.0F);
                out.offsetZ = Mth.lerp(e, 0.36F, 0.0F);
            }
        } else if (step == 2) {
            // Genshin NA2 (24t full, cancel T14): Continuous CLOCKWISE 360-deg spin (+78 deg -> 0 -> -90 -> -180 -> -275 -> -360/0 deg)
            float t = Mth.clamp(24.0F - naTf, 0.0F, 24.0F);
            out.pivotZ = 0.48F;
            if (t < 4.0F) {
                float e = easeInQuad(t / 4.0F);
                out.yawDeg = Mth.lerp(e, 78.0F, -45.0F);
                out.rollDeg = Mth.lerp(e, 10.0F, -18.0F);
                out.pitchDeg = Mth.lerp(e, -2.0F, -6.0F);
                out.offsetZ = Mth.lerp(e, 0.36F, 0.45F);
            } else if (t < 9.0F) {
                float u = (t - 4.0F) / 5.0F;
                out.yawDeg = Mth.lerp(u, -45.0F, -275.0F);
                out.rollDeg = Mth.lerp(u, -18.0F, -30.0F);
                out.pitchDeg = Mth.lerp(u, -6.0F, -10.0F);
                out.offsetY = Mth.sin(u * (float) Math.PI) * 0.16F;
                out.offsetZ = Mth.lerp(u, 0.45F, 0.65F);
            } else if (t < 15.0F) {
                float e = easeOutCubic((t - 9.0F) / 6.0F);
                out.yawDeg = Mth.lerp(e, -275.0F, -360.0F);
                out.rollDeg = Mth.lerp(e, -30.0F, 0.0F);
                out.pitchDeg = Mth.lerp(e, -10.0F, 0.0F);
                out.offsetZ = Mth.lerp(e, 0.65F, 0.22F);
                out.pivotZ = Mth.lerp(e, 0.48F, 0.0F);
            } else {
                float e = easeInOutSine((t - 15.0F) / 9.0F);
                out.yawDeg = -360.0F;
                out.offsetZ = Mth.lerp(e, 0.22F, 0.0F);
                out.pivotZ = 0.0F;
            }
        } else if (step == 3) {
            // Genshin NA3 (22t full, cancel T14): Front-wheel Stoppie (+72 deg pitch) + CLOCKWISE 180-deg pivot spin (0 -> -180 deg), hold until T16 for smooth NA4 chain
            float t = Mth.clamp(22.0F - naTf, 0.0F, 22.0F);
            if (t < 5.5F) {
                float e = easeOutQuad(t / 5.5F);
                out.pivotZ = 0.68F;
                out.pitchDeg = Mth.lerp(e, 0.0F, 72.0F);
                out.yawDeg = Mth.lerp(e, 0.0F, -165.0F);
                out.offsetY = Mth.lerp(e, 0.0F, 0.46F);
                out.offsetZ = Mth.lerp(e, 0.0F, 0.18F);
                out.rollDeg = Mth.lerp(e, 0.0F, 8.0F);
            } else if (t < 8.0F) {
                float e = easeInCubic((t - 5.5F) / 2.5F);
                out.pivotZ = 0.68F;
                out.pitchDeg = Mth.lerp(e, 72.0F, -8.0F);
                out.yawDeg = Mth.lerp(e, -165.0F, -180.0F);
                out.offsetY = Mth.lerp(e, 0.46F, -0.08F);
                out.offsetZ = Mth.lerp(e, 0.18F, 0.06F);
                out.rollDeg = Mth.lerp(e, 8.0F, -4.0F);
            } else if (t < 16.0F) {
                float e = easeOutCubic(Mth.clamp((t - 8.0F) / 6.0F, 0.0F, 1.0F));
                out.pivotZ = Mth.lerp(e, 0.68F, 0.0F);
                out.pitchDeg = Mth.lerp(e, -8.0F, 0.0F);
                out.yawDeg = -180.0F;
                out.offsetY = Mth.lerp(e, -0.08F, 0.0F);
                out.offsetZ = Mth.lerp(e, 0.06F, 0.0F);
                out.rollDeg = Mth.lerp(e, -4.0F, 0.0F);
            } else {
                // If player pauses past T16 after NA3, smoothly turn the remaining 180 deg CW back to -360/0 deg
                float e = easeInOutSine((t - 16.0F) / 6.0F);
                out.yawDeg = Mth.lerp(e, -180.0F, -360.0F);
            }
        } else if (step == 4) {
            // Genshin NA4 (20t full, cancel T10): From NA3's -180 deg, pop -35 deg Wheelie & blast exhaust sparks -> smooth CLOCKWISE 180-deg drift back to -360/0 deg
            float t = Mth.clamp(20.0F - naTf, 0.0F, 20.0F);
            if (t < 3.5F) {
                float e = easeOutQuad(t / 3.5F);
                out.yawDeg = Mth.lerp(e, -180.0F, -205.0F);
                out.pitchDeg = Mth.lerp(e, 0.0F, -35.0F);
                out.rollDeg = Mth.lerp(e, 0.0F, -18.0F);
                out.offsetY = Mth.lerp(e, 0.0F, 0.20F);
                out.offsetZ = Mth.lerp(e, 0.0F, 0.25F);
            } else if (t < 10.5F) {
                float e = easeOutCubic((t - 3.5F) / 7.0F);
                out.yawDeg = Mth.lerp(e, -205.0F, -360.0F);
                out.pitchDeg = Mth.lerp(e, -35.0F, -8.0F);
                out.rollDeg = Mth.lerp(e, -18.0F, -6.0F);
                out.offsetY = Mth.lerp(e, 0.20F, 0.06F);
                out.offsetZ = Mth.lerp(e, 0.25F, 0.45F);
            } else {
                float e = easeInOutSine((t - 10.5F) / 9.5F);
                out.yawDeg = -360.0F;
                out.pitchDeg = Mth.lerp(e, -8.0F, 0.0F);
                out.rollDeg = Mth.lerp(e, -6.0F, 0.0F);
                out.offsetY = Mth.lerp(e, 0.06F, 0.0F);
                out.offsetZ = Mth.lerp(e, 0.45F, 0.0F);
            }
        } else if (step == 5) {
            // Genshin NA5 (28t full, hitmark T12): Wheelie launch (-58 deg) -> 6t Front-vault Superman arc (-58 -> 0 -> +78 deg nose-down, never belly-up backflip!) -> Dive slam -> Wheelie bounce & settle
            float t = Mth.clamp(28.0F - naTf, 0.0F, 28.0F);
            if (t < 3.5F) {
                float e = easeOutQuad(t / 3.5F);
                out.pitchDeg = Mth.lerp(e, 0.0F, -58.0F);
                out.offsetY = Mth.lerp(e, 0.0F, 1.35F);
                out.offsetZ = Mth.lerp(e, 0.0F, 0.55F);
            } else if (t < 9.5F) {
                float u = (t - 3.5F) / 6.0F;
                float e = easeInOutSine(u);
                out.offsetY = 1.35F + Mth.sin(u * (float) Math.PI) * 0.30F;
                out.offsetZ = Mth.lerp(u, 0.55F, 0.85F);
                out.pitchDeg = Mth.lerp(e, -58.0F, 78.0F); // Front-vault forward arc from nose-up to nose-down dive!
            } else if (t < 12.5F) {
                float u = (t - 9.5F) / 3.0F;
                float drop = (float) Math.pow(u, 2.4D);
                out.offsetY = Mth.lerp(drop, 1.35F, -0.14F);
                out.offsetZ = Mth.lerp(u, 0.85F, 0.40F);
                out.pitchDeg = Mth.lerp(u, 78.0F, 8.0F); // Nose-down dive -> grounded impact
            } else if (t < 17.5F) {
                float e = easeOutQuad((t - 12.5F) / 5.0F);
                out.offsetY = Mth.lerp(e, -0.14F, 0.12F);
                out.offsetZ = Mth.lerp(e, 0.40F, 0.25F);
                out.pitchDeg = Mth.lerp(e, 8.0F, -22.0F);
                out.yawDeg = Mth.lerp(e, 0.0F, 15.0F);
                out.rollDeg = Mth.lerp(e, 0.0F, 8.0F);
            } else {
                float e = easeInOutSine((t - 17.5F) / 10.5F);
                out.offsetY = Mth.lerp(e, 0.12F, 0.0F);
                out.offsetZ = Mth.lerp(e, 0.25F, 0.0F);
                out.pitchDeg = Mth.lerp(e, -22.0F, 0.0F);
                out.yawDeg = Mth.lerp(e, 15.0F, 0.0F);
                out.rollDeg = Mth.lerp(e, 8.0F, 0.0F);
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
