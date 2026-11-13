package com.velorise.staves.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    @Inject(method = "render", at = @At("HEAD"))
    private void velorise$onRenderPre(T livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        ClientAuraRenderer.applyFrozenPoseLock(livingEntity);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void velorise$renderAura(T livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        ClientAuraRenderer.renderFrozenIceOverlay(livingEntity, this.model, poseStack, multiBufferSource);
        ClientAuraRenderer.renderJadeShieldBarrier(livingEntity, poseStack, multiBufferSource);
        ClientAuraRenderer.render(livingEntity, poseStack, multiBufferSource, i);
    }
}
