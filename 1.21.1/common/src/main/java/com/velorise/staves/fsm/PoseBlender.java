package com.velorise.staves.fsm;

import net.minecraft.util.Mth;

/**
 * Frame-rate independent exponential damping utilities (1 - exp(-k * dt)) using System.nanoTime()
 * for smooth pose transitions across HumanoidModel and custom entity renderers.
 */
public final class PoseBlender {
    private PoseBlender() {}

    public static float clampDtSeconds(long nowNanos, long prevNanos) {
        if (prevNanos <= 0L) return 0.016F;
        float dt = (float) ((nowNanos - prevNanos) * 1.0E-9D);
        return Mth.clamp(dt, 0.0005F, 0.05F);
    }

    public static float alpha(float stiffness, float dtSeconds) {
        return 1.0F - (float) Math.exp(-stiffness * dtSeconds);
    }

    public static float damp(float current, float target, float stiffness, float dtSeconds) {
        return Mth.lerp(alpha(stiffness, dtSeconds), current, target);
    }

    public static float dampAngleDeg(float currentDeg, float targetDeg, float stiffness, float dtSeconds) {
        float diff = Mth.wrapDegrees(targetDeg - currentDeg);
        return currentDeg + diff * alpha(stiffness, dtSeconds);
    }

    /**
     * Mutable humanoid pose snapshot for smooth wall-climb / jump / mantle / air blending.
     */
    public static final class HumanoidPose {
        public float bodyX;
        public float bodyY;
        public float rightArmX;
        public float leftArmX;
        public float rightArmZ;
        public float leftArmZ;
        public float rightLegX;
        public float leftLegX;
        public long lastUpdateNanos = 0L;

        public void set(
                float bodyX, float bodyY,
                float rightArmX, float leftArmX,
                float rightArmZ, float leftArmZ,
                float rightLegX, float leftLegX
        ) {
            this.bodyX = bodyX;
            this.bodyY = bodyY;
            this.rightArmX = rightArmX;
            this.leftArmX = leftArmX;
            this.rightArmZ = rightArmZ;
            this.leftArmZ = leftArmZ;
            this.rightLegX = rightLegX;
            this.leftLegX = leftLegX;
        }

        public void blendToward(HumanoidPose target, float stiffness, long nowNanos) {
            float dt = clampDtSeconds(nowNanos, lastUpdateNanos);
            lastUpdateNanos = nowNanos;
            this.bodyX = damp(this.bodyX, target.bodyX, stiffness, dt);
            this.bodyY = damp(this.bodyY, target.bodyY, stiffness, dt);
            this.rightArmX = damp(this.rightArmX, target.rightArmX, stiffness, dt);
            this.leftArmX = damp(this.leftArmX, target.leftArmX, stiffness, dt);
            this.rightArmZ = damp(this.rightArmZ, target.rightArmZ, stiffness, dt);
            this.leftArmZ = damp(this.leftArmZ, target.leftArmZ, stiffness, dt);
            this.rightLegX = damp(this.rightLegX, target.rightLegX, stiffness, dt);
            this.leftLegX = damp(this.leftLegX, target.leftLegX, stiffness, dt);
        }
    }
}
