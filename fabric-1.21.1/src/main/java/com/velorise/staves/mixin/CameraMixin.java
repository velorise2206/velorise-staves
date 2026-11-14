package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float yRot;
    @Shadow
    private float xRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract net.minecraft.world.phys.Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void velorise$anchorDonutSpinCamera(
            BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci
    ) {
        if (entity != null && entity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike && bike.isSpinCameraAnchored()) {
            double bikeX = net.minecraft.util.Mth.lerp(partialTick, bike.xo, bike.getX());
            double bikeY = net.minecraft.util.Mth.lerp(partialTick, bike.yo, bike.getY());
            double bikeZ = net.minecraft.util.Mth.lerp(partialTick, bike.zo, bike.getZ());
            double pivotX = bike.getSpinPivotX(partialTick);
            double pivotZ = bike.getSpinPivotZ(partialTick);
            double dx = pivotX - bikeX;
            double dz = pivotZ - bikeZ;
            net.minecraft.world.phys.Vec3 pos = this.getPosition();
            net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(pos.x + dx, pos.y, pos.z + dz);
            if (level instanceof net.minecraft.world.level.Level lvl) {
                net.minecraft.world.phys.Vec3 pivotEye = new net.minecraft.world.phys.Vec3(pivotX, bikeY + entity.getEyeHeight(), pivotZ);
                net.minecraft.world.phys.BlockHitResult hit = lvl.clip(new net.minecraft.world.level.ClipContext(
                        pivotEye, targetPos,
                        net.minecraft.world.level.ClipContext.Block.VISUAL,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        entity
                ));
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    net.minecraft.world.phys.Vec3 hitLoc = hit.getLocation();
                    net.minecraft.world.phys.Vec3 dir = targetPos.subtract(pivotEye);
                    double len = dir.length();
                    if (len > 1.0E-4D) {
                        double safeDist = Math.max(0.15D, hitLoc.distanceTo(pivotEye) - 0.18D);
                        targetPos = pivotEye.add(dir.scale(safeDist / len));
                    } else {
                        targetPos = hitLoc;
                    }
                }
            }
            this.setPosition(targetPos.x, targetPos.y, targetPos.z);
        }
    }

    @Inject(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void velorise$overrideFrozenCamera(
            BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci
    ) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity le && System.currentTimeMillis() < com.velorise.staves.client.ClientAuraTracker.silentDrainUntil) {
            le.invulnerableTime = 0;
            le.hurtTime = 0;
            le.hurtDuration = 0;
        }
        float yaw = ClientAuraRenderer.getFrozenCameraYaw(entity, this.yRot);
        float pitch = ClientAuraRenderer.getFrozenCameraPitch(entity, this.xRot);
        if (yaw != this.yRot || pitch != this.xRot) {
            this.setRotation(yaw, pitch);
        }
    }
}
