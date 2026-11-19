package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.character.WallClimbVisualRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resets any dirty wall-climb bone offsets/rotations before vanilla setupAnim runs
 * (preventing permanent pose leakage) and applies custom third-person staff choreography.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("HEAD")
    )
    private void velorise$resetDirtyWallClimbParts(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        WallClimbVisualRenderer.resetDirtyModelParts(entity, (HumanoidModel<?>) (Object) this);
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void velorise$applyCustomStaffAnimations(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        ClientAuraRenderer.applyStaffHumanoidAnimation(entity, (HumanoidModel<?>) (Object) this, ageInTicks);
    }
}
