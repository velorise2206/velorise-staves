package com.velorise.staves.mixin;

import com.velorise.staves.entity.FlamestriderBikeEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void velorise$anchorDonutSpinCamera(
            BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci
    ) {
        if (entity != null && entity.getVehicle() instanceof FlamestriderBikeEntity bike && bike.isSpinCameraAnchored()) {
            double bikeX = Mth.lerp(partialTick, bike.xo, bike.getX());
            double eyeY = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight();
            double bikeZ = Mth.lerp(partialTick, bike.zo, bike.getZ());
            double dx = bike.getSpinCameraOffsetX(partialTick);
            double dz = bike.getSpinCameraOffsetZ(partialTick);
            double pivotX = bikeX + dx;
            double pivotZ = bikeZ + dz;
            Vec3 pos = this.getPosition();
            Vec3 targetPos = new Vec3(pos.x + dx, pos.y, pos.z + dz);
            if (level instanceof net.minecraft.world.level.Level lvl) {
                Vec3 pivotEye = new Vec3(pivotX, eyeY, pivotZ);
                net.minecraft.world.phys.BlockHitResult hit = lvl.clip(new net.minecraft.world.level.ClipContext(
                        pivotEye, targetPos,
                        net.minecraft.world.level.ClipContext.Block.VISUAL,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        entity
                ));
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    Vec3 hitLoc = hit.getLocation();
                    Vec3 dir = targetPos.subtract(pivotEye);
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
        if (!detached && entity != null) {
            float liftY = com.velorise.staves.client.character.VentiVisualRenderer.getLevitationCameraOffsetY(entity, partialTick)
                    + com.velorise.staves.client.character.MavuikaVisualRenderer.getIdleCameraOffsetY(entity, partialTick)
                    + com.velorise.staves.client.character.RaidenVisualRenderer.getLevitationCameraOffsetY(entity, partialTick)
                    + com.velorise.staves.client.character.NahidaVisualRenderer.getViewfinderCameraOffsetY(entity, partialTick)
                    + com.velorise.staves.client.character.TsaritsaVisualRenderer.getBurstDpsLevitationOffsetY(entity, partialTick);
            if (Math.abs(liftY) > 0.001F) {
                net.minecraft.world.phys.Vec3 camPos = this.getPosition();
                this.setPosition(camPos.x, camPos.y + liftY, camPos.z);
            }
        }
        if (detached && !thirdPersonReverse && entity != null) {
            float vfW = com.velorise.staves.client.character.NahidaVisualRenderer.getViewfinderFade(entity);
            if (vfW > 0.001F) {
                float yawRad = ((Camera) (Object) this).getYRot() * ((float) Math.PI / 180.0F);
                double rightX = -Math.cos(yawRad);
                double rightZ = -Math.sin(yawRad);
                double shiftRight = 2.05D * vfW;
                double shiftUp = 1.35D * vfW;
                net.minecraft.world.phys.Vec3 pos = this.getPosition();
                net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(
                        pos.x + rightX * shiftRight,
                        pos.y + shiftUp,
                        pos.z + rightZ * shiftRight
                );
                if (level instanceof net.minecraft.world.level.Level lvl) {
                    double px = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
                    double py = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight();
                    double pz = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());
                    net.minecraft.world.phys.Vec3 eyePos = new net.minecraft.world.phys.Vec3(px, py, pz);
                    net.minecraft.world.phys.BlockHitResult hit = lvl.clip(new net.minecraft.world.level.ClipContext(
                            eyePos, targetPos,
                            net.minecraft.world.level.ClipContext.Block.VISUAL,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            entity
                    ));
                    if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                        net.minecraft.world.phys.Vec3 hitLoc = hit.getLocation();
                        net.minecraft.world.phys.Vec3 dir = targetPos.subtract(eyePos);
                        double len = dir.length();
                        if (len > 1.0E-4D) {
                            double safeDist = Math.max(0.25D, hitLoc.distanceTo(eyePos) - 0.18D);
                            targetPos = eyePos.add(dir.scale(safeDist / len));
                        } else {
                            targetPos = hitLoc;
                        }
                    }
                }
                this.setPosition(targetPos.x, targetPos.y, targetPos.z);
            }
        }
    }
}
