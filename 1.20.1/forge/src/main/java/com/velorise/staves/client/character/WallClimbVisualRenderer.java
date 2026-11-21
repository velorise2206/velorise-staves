package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.WallClimbHelper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ParCool!-inspired wall climbing visual renderer:
 * 1. Whole-body waist-pivot (0, 0.9, 0) PoseStack transform in LivingEntityRenderer.render()
 *    with smooth wall-yaw lock (zero yBodyRot snapping), wall-hug translation, pitch lean,
 *    climber weight-shift roll, and vertical heave.
 * 2. 6-DOF keyframe limb animation (rotation + pixel translation for shoulder reach & bent-knee
 *    foothold illusion) blended smoothly into vanilla setupAnim via Quaternionf.slerp.
 * 3. Automatic dirty-part resetPose() at HumanoidModel.setupAnim HEAD so zero bone offsets
 *    or rotations ever leak after detaching from a wall.
 */
public final class WallClimbVisualRenderer {
    private static float lastRenderPartialTick = 0.0F;
    private static final Set<Integer> DIRTY_POSE_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final float TWO_PI = (float) (Math.PI * 2.0D);

    private WallClimbVisualRenderer() {}

    private record PartPose(float rx, float ry, float rz, float ox, float oy, float oz) {
        static final PartPose ZERO = new PartPose(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        static PartPose lerp(PartPose a, PartPose b, float t) {
            float s = t * t * (3.0F - 2.0F * t);
            return new PartPose(
                    Mth.lerp(s, a.rx, b.rx),
                    Mth.lerp(s, a.ry, b.ry),
                    Mth.lerp(s, a.rz, b.rz),
                    Mth.lerp(s, a.ox, b.ox),
                    Mth.lerp(s, a.oy, b.oy),
                    Mth.lerp(s, a.oz, b.oz)
            );
        }
    }

    private record ClimbFrame(
            PartPose rArm,
            PartPose lArm,
            PartPose rLeg,
            PartPose lLeg,
            float bodyPitch,
            float bodyRoll,
            float bodyTwist,
            float heaveY,
            float headPitchBias
    ) {
        static ClimbFrame lerp(ClimbFrame a, ClimbFrame b, float t) {
            float s = t * t * (3.0F - 2.0F * t);
            return new ClimbFrame(
                    PartPose.lerp(a.rArm, b.rArm, t),
                    PartPose.lerp(a.lArm, b.lArm, t),
                    PartPose.lerp(a.rLeg, b.rLeg, t),
                    PartPose.lerp(a.lLeg, b.lLeg, t),
                    Mth.lerp(s, a.bodyPitch, b.bodyPitch),
                    Mth.lerp(s, a.bodyRoll, b.bodyRoll),
                    Mth.lerp(s, a.bodyTwist, b.bodyTwist),
                    Mth.lerp(s, a.heaveY, b.heaveY),
                    Mth.lerp(s, a.headPitchBias, b.headPitchBias)
            );
        }
    }

    // -------------------------------------------------------------------------
    // Keyframe libraries (ParCool!-style 6-DOF bone translation + 3-axis angles)
    // -------------------------------------------------------------------------
    private static final ClimbFrame IDLE_FRAME = new ClimbFrame(
            new PartPose(-2.68F, -0.22F,  0.18F,  0.0F, -1.1F, -1.8F),
            new PartPose(-2.06F,  0.20F, -0.22F,  0.0F,  0.2F, -1.6F),
            new PartPose(-0.88F,  0.14F,  0.26F,  0.2F, -2.3F, -2.0F),
            new PartPose(-0.26F, -0.10F, -0.16F, -0.1F, -0.4F, -0.6F),
            0.09F, 0.03F, -0.05F, 0.0F, -0.12F
    );

    // Climb Up 4-keyframe rock-climber cycle: Right Reach -> Compact Pull -> Left Reach -> Compact Pull
    private static final ClimbFrame[] CLIMB_UP_KEYS = new ClimbFrame[] {
            new ClimbFrame(
                    new PartPose(-2.96F, -0.26F,  0.14F,  0.0F, -1.8F, -2.0F),
                    new PartPose(-1.02F,  0.14F, -0.28F,  0.0F,  1.2F, -1.2F),
                    new PartPose( 0.28F,  0.05F,  0.12F,  0.0F,  0.5F,  0.6F),
                    new PartPose(-1.12F, -0.16F, -0.30F, -0.3F, -3.0F, -2.4F),
                    0.11F, -0.09F, -0.14F,  0.045F, -0.30F
            ),
            new ClimbFrame(
                    new PartPose(-2.14F, -0.20F,  0.22F,  0.0F, -0.5F, -1.8F),
                    new PartPose(-2.04F,  0.20F, -0.22F,  0.0F, -0.3F, -1.8F),
                    new PartPose(-0.54F,  0.12F,  0.22F,  0.1F, -1.4F, -1.4F),
                    new PartPose(-0.58F, -0.12F, -0.22F, -0.1F, -1.6F, -1.5F),
                    0.16F,  0.00F,  0.00F, -0.030F, -0.24F
            ),
            new ClimbFrame(
                    new PartPose(-1.02F, -0.14F,  0.28F,  0.0F,  1.2F, -1.2F),
                    new PartPose(-2.96F,  0.26F, -0.14F,  0.0F, -1.8F, -2.0F),
                    new PartPose(-1.12F,  0.16F,  0.30F,  0.3F, -3.0F, -2.4F),
                    new PartPose( 0.28F, -0.05F, -0.12F,  0.0F,  0.5F,  0.6F),
                    0.11F,  0.09F,  0.14F,  0.045F, -0.30F
            ),
            new ClimbFrame(
                    new PartPose(-2.04F, -0.20F,  0.22F,  0.0F, -0.3F, -1.8F),
                    new PartPose(-2.14F,  0.20F, -0.22F,  0.0F, -0.5F, -1.8F),
                    new PartPose(-0.58F,  0.12F,  0.22F,  0.1F, -1.6F, -1.5F),
                    new PartPose(-0.54F, -0.12F, -0.22F, -0.1F, -1.4F, -1.4F),
                    0.16F,  0.00F,  0.00F, -0.030F, -0.24F
            )
    };

    // Lateral Shimmy 4-keyframe cycle (ParCool hang_moving_right style)
    private static final ClimbFrame[] SHIMMY_RIGHT_KEYS = new ClimbFrame[] {
            new ClimbFrame(
                    new PartPose(-2.55F, -0.24F,  0.82F, -1.2F, -1.0F, -1.8F),
                    new PartPose(-2.22F,  0.16F, -0.14F, -0.6F,  0.0F, -1.6F),
                    new PartPose(-0.62F,  0.14F,  0.46F, -0.6F, -1.8F, -1.6F),
                    new PartPose(-0.34F, -0.08F, -0.12F,  0.0F, -0.6F, -0.8F),
                    0.10F,  0.14F,  0.12F,  0.025F, -0.10F
            ),
            new ClimbFrame(
                    new PartPose(-2.40F, -0.18F,  0.26F,  0.0F, -0.4F, -1.8F),
                    new PartPose(-2.40F,  0.18F, -0.26F,  0.0F, -0.4F, -1.8F),
                    new PartPose(-0.48F,  0.10F,  0.18F,  0.0F, -1.2F, -1.2F),
                    new PartPose(-0.48F, -0.10F, -0.18F,  0.0F, -1.2F, -1.2F),
                    0.14F,  0.04F,  0.00F, -0.020F, -0.10F
            ),
            new ClimbFrame(
                    new PartPose(-2.22F, -0.16F,  0.14F,  0.6F,  0.0F, -1.6F),
                    new PartPose(-2.55F,  0.24F, -0.82F,  1.2F, -1.0F, -1.8F),
                    new PartPose(-0.34F,  0.08F,  0.12F,  0.0F, -0.6F, -0.8F),
                    new PartPose(-0.62F, -0.14F, -0.46F,  0.6F, -1.8F, -1.6F),
                    0.10F,  0.12F, -0.12F,  0.025F, -0.10F
            ),
            new ClimbFrame(
                    new PartPose(-2.40F, -0.18F,  0.26F,  0.0F, -0.4F, -1.8F),
                    new PartPose(-2.40F,  0.18F, -0.26F,  0.0F, -0.4F, -1.8F),
                    new PartPose(-0.48F,  0.10F,  0.18F,  0.0F, -1.2F, -1.2F),
                    new PartPose(-0.48F, -0.10F, -0.18F,  0.0F, -1.2F, -1.2F),
                    0.14F,  0.04F,  0.00F, -0.020F, -0.10F
            )
    };

    // ParCool ClimbUp 3-keyframe vault: Grip Lip -> Push Over & Knee Up -> Stand Cleanly
    private static final ClimbFrame MANTLE_START = new ClimbFrame(
            new PartPose(-2.50F, -0.18F,  0.22F, 0.0F, -1.0F, -1.8F),
            new PartPose(-2.50F,  0.18F, -0.22F, 0.0F, -1.0F, -1.8F),
            new PartPose(-0.80F,  0.10F,  0.20F, 0.0F, -2.0F, -1.6F),
            new PartPose(-0.35F, -0.08F, -0.14F, 0.0F, -0.6F, -0.6F),
            0.14F, 0.0F, 0.0F, 0.0F, 0.10F
    );
    private static final ClimbFrame MANTLE_MID = new ClimbFrame(
            new PartPose(-0.65F, -0.12F,  0.28F, 0.0F,  1.4F, -0.8F),
            new PartPose(-0.65F,  0.12F, -0.28F, 0.0F,  1.4F, -0.8F),
            new PartPose(-1.28F,  0.12F,  0.22F, 0.2F, -3.2F, -2.4F),
            new PartPose( 0.42F,  0.00F, -0.10F, 0.0F,  0.4F,  0.8F),
            0.52F, 0.0F, 0.0F, 0.06F, 0.28F
    );
    private static final ClimbFrame MANTLE_END = new ClimbFrame(
            new PartPose(-0.18F,  0.00F,  0.08F, 0.0F,  0.0F,  0.0F),
            new PartPose(-0.18F,  0.00F, -0.08F, 0.0F,  0.0F,  0.0F),
            new PartPose(-0.15F,  0.00F,  0.04F, 0.0F, -0.3F, -0.2F),
            new PartPose( 0.12F,  0.00F, -0.04F, 0.0F,  0.0F,  0.0F),
            0.06F, 0.0F, 0.0F, 0.0F, 0.05F
    );

    // Genshin 1-block low-obstacle symmetric two-hand push-vault (hands plant DOWN on waist-high edge, push back cleanly, zero side tilt!)
    private static final ClimbFrame LOW_MANTLE_START = new ClimbFrame(
            new PartPose(-1.18F, -0.08F,  0.16F, 0.0F,  0.4F, -1.6F),
            new PartPose(-1.18F,  0.08F, -0.16F, 0.0F,  0.4F, -1.6F),
            new PartPose(-0.86F,  0.04F,  0.06F, 0.0F, -1.8F, -1.4F),
            new PartPose( 0.34F, -0.04F, -0.06F, 0.0F,  0.2F,  0.5F),
            0.26F, 0.0F, 0.0F, 0.02F, 0.14F
    );
    private static final ClimbFrame LOW_MANTLE_MID = new ClimbFrame(
            new PartPose( 0.28F, -0.06F,  0.20F, 0.0F,  1.0F,  0.4F),
            new PartPose( 0.28F,  0.06F, -0.20F, 0.0F,  1.0F,  0.4F),
            new PartPose(-0.42F,  0.02F,  0.04F, 0.0F, -0.8F, -0.6F),
            new PartPose(-0.56F, -0.02F, -0.04F, 0.0F, -1.2F, -0.9F),
            0.20F, 0.0F, 0.0F, 0.04F, 0.10F
    );
    private static final ClimbFrame LOW_MANTLE_END = new ClimbFrame(
            new PartPose(-0.08F,  0.00F,  0.06F, 0.0F,  0.0F,  0.0F),
            new PartPose(-0.08F,  0.00F, -0.06F, 0.0F,  0.0F,  0.0F),
            new PartPose(-0.06F,  0.00F,  0.02F, 0.0F,  0.0F,  0.0F),
            new PartPose( 0.06F,  0.00F, -0.02F, 0.0F,  0.0F,  0.0F),
            0.03F, 0.0F, 0.0F, 0.0F, 0.02F
    );

    // Genshin-style 3-keyframe athletic climb-jump (Launch Push-off -> Cross-Drive Apex -> Firm Re-Grab)
    private static final ClimbFrame JUMP_LAUNCH = new ClimbFrame(
            new PartPose(-3.06F, -0.20F,  0.14F,  0.0F, -1.8F, -1.6F),
            new PartPose(-0.92F,  0.14F, -0.24F,  0.0F,  1.2F, -0.9F),
            new PartPose( 0.34F,  0.04F,  0.10F,  0.0F,  0.6F,  0.8F),
            new PartPose( 0.18F, -0.06F, -0.12F,  0.0F,  0.3F,  0.5F),
            -0.10F, -0.08F, -0.14F, 0.05F, -0.34F
    );
    private static final ClimbFrame JUMP_APEX = new ClimbFrame(
            new PartPose(-2.22F, -0.22F,  0.24F,  0.0F, -0.6F, -1.7F),
            new PartPose(-2.98F,  0.22F, -0.14F,  0.0F, -1.7F, -1.8F),
            new PartPose(-1.08F,  0.14F,  0.26F,  0.2F, -2.8F, -2.2F),
            new PartPose( 0.16F, -0.06F, -0.12F,  0.0F,  0.3F,  0.4F),
            -0.14F,  0.08F,  0.14F, 0.07F, -0.32F
    );
    private static final ClimbFrame JUMP_REGRAB = new ClimbFrame(
            new PartPose(-2.58F, -0.20F,  0.20F,  0.0F, -1.0F, -1.8F),
            new PartPose(-2.14F,  0.20F, -0.22F,  0.0F, -0.2F, -1.6F),
            new PartPose(-0.74F,  0.12F,  0.22F,  0.1F, -2.0F, -1.8F),
            new PartPose(-0.42F, -0.10F, -0.16F, -0.1F, -0.9F, -1.0F),
            0.08F,  0.02F, -0.04F, 0.01F, -0.18F
    );

    public static boolean isWallClimbPoseActive(LivingEntity entity) {
        return computeWallClimbBlend(entity, 0.5F) > 0.05F;
    }

    public static float computeWallClimbBlend(LivingEntity entity, float partialTick) {
        if (!(entity instanceof Player player)) return 0.0F;
        int id = entity.getId();
        boolean blocked = ClientAuraTracker.isXilonenSkating(id)
                || ClientAuraTracker.isPlunging(id)
                || ClientAuraTracker.isGliding(id)
                || player.getVehicle() != null;
        if (blocked) return 0.0F;

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(id);
        boolean activeNow = ClientAuraTracker.isSteveWallClimbing(id)
                || (ClientAuraTracker.isLocalPlayer(entity) && ClientAuraTracker.isLocalWallClimbing());
        if (data == null) {
            return activeNow ? 1.0F : 0.0F;
        }
        float w = Mth.clamp(Mth.lerp(partialTick, data.prevWallClimbFade, data.steveWallClimbFade), 0.0F, 1.0F);
        long nowMs = System.currentTimeMillis();
        if (nowMs < data.steveWallMantleUntilMs) {
            // Smoothly blend out of MANTLE_END directly into normal standing/walking pose on the last 28% of mantle
            float dur = Math.max(100.0F, data.steveWallMantleDurationMs);
            float mt = Mth.clamp((nowMs - data.steveWallMantleStartMs) / dur, 0.0F, 1.0F);
            if (mt > 0.72F) {
                float out = 1.0F - Mth.clamp((mt - 0.72F) / 0.28F, 0.0F, 1.0F);
                w *= out;
            }
        } else if (activeNow && w < 0.25F) {
            w = 0.25F;
        }
        if (w < 0.015F) return 0.0F;
        return w * w * (3.0F - 2.0F * w);
    }

    /**
     * Resets model parts at HumanoidModel.setupAnim HEAD if wall climbing previously modified them,
     * ensuring zero leaked bone translations or Z/Y rotations after detaching from a wall.
     */
    public static void resetDirtyModelParts(LivingEntity entity, HumanoidModel<?> model) {
        if (entity == null || model == null) return;
        int id = entity.getId();
        if (!DIRTY_POSE_ENTITIES.contains(id)) return;
        model.head.resetPose();
        model.hat.resetPose();
        model.body.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
        if (model instanceof PlayerModel<?> pm) {
            pm.jacket.resetPose();
            pm.rightSleeve.resetPose();
            pm.leftSleeve.resetPose();
            pm.rightPants.resetPose();
            pm.leftPants.resetPose();
        }
        if (!isWallClimbPoseActive(entity)) {
            DIRTY_POSE_ENTITIES.remove(id);
        }
    }

    private static ClimbFrame sampleCyclic(ClimbFrame[] keys, float normalizedPhase) {
        float u = normalizedPhase - Mth.floor(normalizedPhase);
        float scaled = u * keys.length;
        int idx0 = ((int) scaled) % keys.length;
        int idx1 = (idx0 + 1) % keys.length;
        float frac = scaled - (int) scaled;
        return ClimbFrame.lerp(keys[idx0], keys[idx1], frac);
    }

    private static ClimbFrame mirrorFrame(ClimbFrame f) {
        return new ClimbFrame(
                new PartPose(f.lArm.rx, -f.lArm.ry, -f.lArm.rz, -f.lArm.ox, f.lArm.oy, f.lArm.oz),
                new PartPose(f.rArm.rx, -f.rArm.ry, -f.rArm.rz, -f.rArm.ox, f.rArm.oy, f.rArm.oz),
                new PartPose(f.lLeg.rx, -f.lLeg.ry, -f.lLeg.rz, -f.lLeg.ox, f.lLeg.oy, f.lLeg.oz),
                new PartPose(f.rLeg.rx, -f.rLeg.ry, -f.rLeg.rz, -f.rLeg.ox, f.rLeg.oy, f.rLeg.oz),
                f.bodyPitch, -f.bodyRoll, -f.bodyTwist, f.heaveY, f.headPitchBias
        );
    }

    private static ClimbFrame evaluateClimbFrame(LivingEntity entity, float partialTick) {
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        long nowMs = System.currentTimeMillis();

        if (data != null && nowMs < data.steveWallMantleUntilMs) {
            float dur = Math.max(100.0F, data.steveWallMantleDurationMs);
            float t = Mth.clamp((nowMs - data.steveWallMantleStartMs) / dur, 0.0F, 1.0F);
            if (data.steveWallLowMantle) {
                if (t < 0.50F) {
                    return ClimbFrame.lerp(LOW_MANTLE_START, LOW_MANTLE_MID, t / 0.50F);
                }
                return ClimbFrame.lerp(LOW_MANTLE_MID, LOW_MANTLE_END, (t - 0.50F) / 0.50F);
            }
            if (t < 0.55F) {
                return ClimbFrame.lerp(MANTLE_START, MANTLE_MID, t / 0.55F);
            }
            return ClimbFrame.lerp(MANTLE_MID, MANTLE_END, (t - 0.55F) / 0.45F);
        }
        if (data != null && nowMs < data.steveWallJumpUntilMs) {
            float dur = Math.max(120.0F, (float) (data.steveWallJumpUntilMs - data.steveWallJumpStartMs));
            float jt = Mth.clamp((nowMs - data.steveWallJumpStartMs) / dur, 0.0F, 1.0F);
            ClimbFrame jf;
            if (jt < 0.34F) {
                jf = ClimbFrame.lerp(JUMP_LAUNCH, JUMP_APEX, jt / 0.34F);
            } else {
                jf = ClimbFrame.lerp(JUMP_APEX, JUMP_REGRAB, (jt - 0.34F) / 0.66F);
            }
            return data.steveWallJumpMirror ? mirrorFrame(jf) : jf;
        }

        float dist = data != null ? Mth.lerp(partialTick, data.prevWallClimbDist, data.steveWallClimbDist) : 0.0F;
        float moveY = data != null ? Mth.lerp(partialTick, data.prevWallMoveY, data.steveWallMoveY) : 0.0F;
        float moveX = data != null ? Mth.lerp(partialTick, data.prevWallMoveX, data.steveWallMoveX) : 0.0F;
        float moveMag = Mth.clamp((float) Math.hypot(moveX, moveY), 0.0F, 1.0F);

        float ageInTicks = (float) entity.tickCount + partialTick;
        float breath = Mth.sin(ageInTicks * 0.13F) * 0.035F;
        ClimbFrame idleWithBreath = new ClimbFrame(
                new PartPose(IDLE_FRAME.rArm.rx + breath, IDLE_FRAME.rArm.ry, IDLE_FRAME.rArm.rz, IDLE_FRAME.rArm.ox, IDLE_FRAME.rArm.oy + breath * 4.0F, IDLE_FRAME.rArm.oz),
                new PartPose(IDLE_FRAME.lArm.rx - breath, IDLE_FRAME.lArm.ry, IDLE_FRAME.lArm.rz, IDLE_FRAME.lArm.ox, IDLE_FRAME.lArm.oy - breath * 4.0F, IDLE_FRAME.lArm.oz),
                IDLE_FRAME.rLeg,
                IDLE_FRAME.lLeg,
                IDLE_FRAME.bodyPitch + breath * 0.35F,
                IDLE_FRAME.bodyRoll,
                IDLE_FRAME.bodyTwist,
                breath * 0.18F,
                IDLE_FRAME.headPitchBias
        );

        if (moveMag < 0.03F) {
            return idleWithBreath;
        }

        float cycleU = dist / TWO_PI;
        ClimbFrame active;
        if (Math.abs(moveX) > 0.18F && Math.abs(moveY) < 0.22F) {
            ClimbFrame shimmy = sampleCyclic(SHIMMY_RIGHT_KEYS, cycleU);
            active = moveX < 0.0F ? mirrorFrame(shimmy) : shimmy;
        } else {
            ClimbFrame vertical = sampleCyclic(CLIMB_UP_KEYS, cycleU);
            if (moveY < -0.05F) {
                active = new ClimbFrame(
                        vertical.rArm, vertical.lArm, vertical.rLeg, vertical.lLeg,
                        vertical.bodyPitch, vertical.bodyRoll, vertical.bodyTwist,
                        -vertical.heaveY, 0.28F
                );
            } else {
                active = vertical;
            }
        }
        return ClimbFrame.lerp(idleWithBreath, active, moveMag);
    }

    /**
     * ParCool!-style whole-body PoseStack transform around the waist pivot (0, 0.9, 0),
     * invoked right after setupRotations() and right before PoseStack.scale(-1, -1, 1).
     * Smoothly locks body yaw to wall normal without snapping yBodyRot, hugs the wall face,
     * and applies whole-body pitch, twist, roll, and vertical heave.
     */
    public static void applyWallClimbBodyTransform(LivingEntity entity, float partialTick, PoseStack poseStack) {
        float blend = computeWallClimbBlend(entity, partialTick);
        if (blend < 0.015F || !(entity instanceof Player)) return;

        ClimbFrame frame = evaluateClimbFrame(entity, partialTick);
        ClientAuraTracker.AuraDisplayData bodyData = ClientAuraTracker.get(entity.getId());
        boolean lowStepVault = bodyData != null && bodyData.steveWallLowMantle && System.currentTimeMillis() < bodyData.steveWallMantleUntilMs;
        float vanillaBodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float wallYaw = resolveWallBodyYaw(entity);
        float yawDiff = lowStepVault ? 0.0F : Mth.wrapDegrees(wallYaw - vanillaBodyYaw);

        lastRenderPartialTick = partialTick;
        poseStack.translate(0.0D, 0.9D, 0.0D);
        if (Math.abs(yawDiff) > 0.01F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-yawDiff * blend));
        }
        poseStack.translate(0.0D, frame.heaveY * blend, lowStepVault ? 0.0D : (-0.175D * blend));
        poseStack.mulPose(Axis.XP.rotation(-frame.bodyPitch * blend));
        poseStack.mulPose(Axis.YP.rotation(frame.bodyTwist * blend));
        poseStack.mulPose(Axis.ZP.rotation(frame.bodyRoll * blend));
        poseStack.translate(0.0D, -0.9D, 0.0D);
    }

    /**
     * Applies 6-DOF limb pose via Quaternionf.slerp (zero Euler gimbal wobble) once per render
     * right after EntityModel.setupAnim().
     */
    public static boolean applyWallClimbHumanoidPose(
            LivingEntity entity,
            HumanoidModel<?> model,
            float ageInTicks
    ) {
        if (!(entity instanceof Player)) return false;
        float partialTick = (ageInTicks > 0.001F)
                ? Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F)
                : lastRenderPartialTick;
        float blend = computeWallClimbBlend(entity, partialTick);
        if (blend < 0.015F) {
            return false;
        }

        DIRTY_POSE_ENTITIES.add(entity.getId());
        ClimbFrame frame = evaluateClimbFrame(entity, partialTick);

        if (blend > 0.25F) {
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        }

        applyPartSlerp(model.rightArm, blend, frame.rArm);
        applyPartSlerp(model.leftArm, blend, frame.lArm);
        applyPartSlerp(model.rightLeg, blend, frame.rLeg);
        applyPartSlerp(model.leftLeg, blend, frame.lLeg);

        // Compensate head yaw for the smooth lockBody rotation in applyWallClimbBodyTransform
        float vanillaBodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float wallYaw = resolveWallBodyYaw(entity);
        float yawDiffRad = (float) Math.toRadians(Mth.wrapDegrees(wallYaw - vanillaBodyYaw));
        model.head.yRot = Mth.clamp(model.head.yRot - yawDiffRad * blend, -1.15F, 1.15F);
        model.head.xRot = Mth.lerp(blend * 0.42F, model.head.xRot, frame.headPitchBias);
        model.head.zRot = 0.0F;
        model.hat.copyFrom(model.head);

        if (model instanceof PlayerModel<?> pm) {
            pm.jacket.copyFrom(pm.body);
            pm.rightSleeve.copyFrom(pm.rightArm);
            pm.leftSleeve.copyFrom(pm.leftArm);
            pm.rightPants.copyFrom(pm.rightLeg);
            pm.leftPants.copyFrom(pm.leftLeg);
        }

        return blend > 0.05F;
    }

    private static void applyPartSlerp(ModelPart part, float blend, PartPose target) {
        Quaternionf qCur = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
        Quaternionf qTgt = new Quaternionf().rotationZYX(target.rz, target.ry, target.rx);
        qCur.slerp(qTgt, blend);

        float x = qCur.x;
        float y = qCur.y;
        float z = qCur.z;
        float w = qCur.w;
        float sinp = 2.0F * (w * y - z * x);
        part.yRot = Math.abs(sinp) >= 1.0F
                ? Math.copySign((float) (Math.PI * 0.5D), sinp)
                : (float) Math.asin(sinp);
        part.xRot = (float) Math.atan2(2.0F * (w * x + y * z), 1.0F - 2.0F * (x * x + y * y));
        part.zRot = (float) Math.atan2(2.0F * (w * z + x * y), 1.0F - 2.0F * (y * y + z * z));

        part.x += target.ox * blend;
        part.y += target.oy * blend;
        part.z += target.oz * blend;
    }

    /**
     * Crisp, unobstructed 1st-person climbing hand motion synced to the 3rd-person reach-pull gait.
     */
    public static boolean applyFirstPersonWallClimbTransform(
            AbstractClientPlayer player,
            float partialTicks,
            InteractionHand hand,
            PoseStack poseStack
    ) {
        float blend = computeWallClimbBlend(player, partialTicks);
        if (blend < 0.05F) return false;
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        float dist = data != null ? Mth.lerp(partialTicks, data.prevWallClimbDist, data.steveWallClimbDist) : 0.0F;
        float moveY = data != null ? Mth.lerp(partialTicks, data.prevWallMoveY, data.steveWallMoveY) : 0.0F;
        float moveX = data != null ? Mth.lerp(partialTicks, data.prevWallMoveX, data.steveWallMoveX) : 0.0F;
        float moveMag = Mth.clamp((float) Math.hypot(moveX, moveY), 0.0F, 1.0F);
        float phase = Mth.sin(dist);

        HumanoidArm arm = (hand == InteractionHand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
        float sideSign = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        float stroke = phase * sideSign * moveMag;

        poseStack.translate(-sideSign * 0.04F * blend, (-0.10F + stroke * 0.095F) * blend, (-0.05F - Math.max(0.0F, stroke) * 0.05F) * blend);
        poseStack.mulPose(Axis.XP.rotationDegrees((12.0F + stroke * 16.0F) * blend));
        poseStack.mulPose(Axis.ZP.rotationDegrees((-sideSign * 6.0F) * blend));
        return true;
    }

    public static boolean setupFirstPersonEmptyOffhandClimbPose(
            AbstractClientPlayer player,
            float partialTicks,
            HumanoidArm offArm,
            PoseStack poseStack
    ) {
        return false;
    }

    public static float resolveWallBodyYaw(LivingEntity entity) {
        if (!(entity instanceof Player player)) return entity.yBodyRot;
        int id = entity.getId();
        Direction face = ClientAuraTracker.getWallNormalFace(id);
        if (face == null) {
            face = WallClimbHelper.findClimbWall(player, WallClimbHelper.SERVER_WALL_TOLERANCE);
        }
        return face != null ? WallClimbHelper.getFacingWallYaw(face) : entity.yBodyRot;
    }
}
