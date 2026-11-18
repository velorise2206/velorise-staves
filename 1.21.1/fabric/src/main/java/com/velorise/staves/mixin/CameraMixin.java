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
    private float neuvilletteCamFade = 0.0F;
    private long neuvilletteCamLastNs = 0L;
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
            double eyeY = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight();
            double bikeZ = net.minecraft.util.Mth.lerp(partialTick, bike.zo, bike.getZ());
            double dx = bike.getSpinCameraOffsetX(partialTick);
            double dz = bike.getSpinCameraOffsetZ(partialTick);
            double pivotX = bikeX + dx;
            double pivotZ = bikeZ + dz;
            net.minecraft.world.phys.Vec3 pos = this.getPosition();
            net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(pos.x + dx, pos.y, pos.z + dz);
            if (level instanceof net.minecraft.world.level.Level lvl) {
                net.minecraft.world.phys.Vec3 pivotEye = new net.minecraft.world.phys.Vec3(pivotX, eyeY, pivotZ);
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
        if (!detached && entity != null) {
            if (entity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity ultiBike && ultiBike.getUltiTicks() > 0) {
                net.minecraft.world.phys.Vec3 ultiOff = ultiBike.getFirstPersonUltiCameraOffset(partialTick);
                net.minecraft.world.phys.Vec3 curCam = this.getPosition();
                this.setPosition(curCam.x + ultiOff.x, curCam.y + ultiOff.y, curCam.z + ultiOff.z);
            }
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
            long nowNs = System.nanoTime();
            float dtCam = neuvilletteCamLastNs == 0L ? 0.016F : Math.min(0.05F, (nowNs - neuvilletteCamLastNs) / 1_000_000_000.0F);
            neuvilletteCamLastNs = nowNs;
            boolean neuvAiming = vfW <= 0.001F && com.velorise.staves.client.ClientAuraTracker.isNeuvilletteChargingOrFiring(entity.getId());
            neuvilletteCamFade = neuvAiming
                    ? Math.min(1.0F, neuvilletteCamFade + dtCam / 0.18F)
                    : Math.max(0.0F, neuvilletteCamFade - dtCam / 0.18F);
            if (neuvilletteCamFade > 0.001F) {
                float neuvW = neuvilletteCamFade * neuvilletteCamFade * (3.0F - 2.0F * neuvilletteCamFade);
                float yawRad = ((Camera) (Object) this).getYRot() * ((float) Math.PI / 180.0F);
                double rightX = -Math.cos(yawRad);
                double rightZ = -Math.sin(yawRad);
                net.minecraft.world.phys.Vec3 pos = this.getPosition();
                this.setPosition(pos.x + rightX * (2.05D * neuvW), pos.y + (1.35D * neuvW), pos.z + rightZ * (2.05D * neuvW));
            }
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
        float bikePitchOffset = 0.0F;
        if (!detached && entity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike
                && bike.shouldOverrideFirstPersonCamera()) {
            yaw = bike.getFirstPersonCameraYaw(partialTick, yaw);
            bikePitchOffset = bike.getFirstPersonFinisherPitchOffset(partialTick);
        }
        float swingPitch = (!detached ? com.velorise.staves.client.character.NahidaVisualRenderer.getSwingCameraPitchOffset(entity, partialTick) : 0.0F) + bikePitchOffset;
        float dragonShake = com.velorise.staves.client.ClientAuraTracker.getDragonfireSkyfallShakeIntensity(entity.getId());
        float shakePitch = 0.0F;
        float shakeYaw = 0.0F;
        if (dragonShake > 0.001F) {
            double t = System.currentTimeMillis() * 0.055D;
            float env = dragonShake * dragonShake;
            shakePitch = (float) ((Math.sin(t * 2.1D) * 0.7D + Math.sin(t * 5.3D) * 0.3D) * 3.8D * env);
            shakeYaw = (float) ((Math.cos(t * 1.7D) * 0.65D + Math.sin(t * 4.1D) * 0.35D) * 2.4D * env);
        }
        if (yaw != this.yRot || pitch != this.xRot || Math.abs(swingPitch) > 0.001F || dragonShake > 0.001F) {
            this.setRotation(yaw + shakeYaw, pitch + swingPitch + shakePitch);
        }
    }
}
