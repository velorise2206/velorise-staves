package com.velorise.staves.client.character;

import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.WallClimbHelper;
import com.velorise.staves.fsm.PoseBlender;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Renders Genshin-style wall climbing poses (Cling, Climb Cycle, Climb Jump, Ledge Mantle)
 * using frame-rate independent exponential damping via PoseBlender.
 */
public final class WallClimbVisualRenderer {
    private static final float BLEND_STIFFNESS = 18.0F;

    private WallClimbVisualRenderer() {}

    public static boolean applyWallClimbHumanoidPose(
            LivingEntity entity,
            HumanoidModel<?> model,
            float ageInTicks
    ) {
        if (!(entity instanceof Player player)) return false;
        int id = entity.getId();
        if (!ClientAuraTracker.isSteveWallClimbing(id)
                || ClientAuraTracker.isXilonenSkating(id)
                || ClientAuraTracker.isPlunging(id)
                || ClientAuraTracker.isGliding(id)
                || player.getVehicle() != null) {
            return false;
        }

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(id);
        if (data == null) return false;

        long nowMs = System.currentTimeMillis();
        long nowNanos = System.nanoTime();

        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        double stepDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean moving = stepDist > 0.015D;
        if (moving) {
            data.steveWallClimbDist += (float) stepDist;
        }

        PoseBlender.HumanoidPose target = new PoseBlender.HumanoidPose();
        if (nowMs < data.steveWallMantleUntilMs) {
            target.set(0.35F, 0.0F, -0.65F, -0.65F, 0.14F, -0.14F, 0.25F, -0.45F);
        } else if (nowMs < data.steveWallJumpUntilMs) {
            target.set(-0.10F, 0.0F, -2.85F, -2.85F, 0.12F, -0.12F, -0.55F, -0.55F);
        } else if (moving) {
            float phase = Mth.sin(data.steveWallClimbDist * 4.5F);
            target.set(
                    0.08F,
                    phase * 0.08F,
                    -2.20F + phase * 0.65F,
                    -2.20F - phase * 0.65F,
                    0.18F,
                    -0.18F,
                    -0.30F - phase * 0.45F,
                    -0.30F + phase * 0.45F
            );
        } else {
            target.set(0.08F, 0.0F, -2.35F, -2.05F, 0.18F, -0.18F, -0.35F, -0.15F);
        }

        data.wallClimbPose.blendToward(target, BLEND_STIFFNESS, nowNanos);

        model.body.xRot = data.wallClimbPose.bodyX;
        model.body.yRot = data.wallClimbPose.bodyY;
        model.rightArm.xRot = data.wallClimbPose.rightArmX;
        model.leftArm.xRot = data.wallClimbPose.leftArmX;
        model.rightArm.zRot = data.wallClimbPose.rightArmZ;
        model.leftArm.zRot = data.wallClimbPose.leftArmZ;
        model.rightLeg.xRot = data.wallClimbPose.rightLegX;
        model.leftLeg.xRot = data.wallClimbPose.leftLegX;

        model.head.yRot = Mth.clamp(model.head.yRot, -1.30F, 1.30F);
        model.hat.copyFrom(model.head);
        return true;
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
