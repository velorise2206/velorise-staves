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
