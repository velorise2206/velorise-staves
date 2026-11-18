package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies custom third-person upper-body and arm choreography for Furina's Splendor of Tranquil Waters
 * (Summon: cane plant + skyward hand raise; Arkhe Switch: cane plant) and Zhongli's Vortex Vanquisher
 * (Hold: arms crossed in front of chest -> wide outward radial sweep on Jade Shield release).
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
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
