package com.velorise.staves.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.FlamestriderBikeRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    @Unique
    private final Deque<Boolean> velorise$bikePoseStack = new ArrayDeque<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void velorise$onRenderPre(T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, CallbackInfo ci) {
        ClientAuraRenderer.applyFrozenPoseLock(livingEntity);
        velorise$bikePoseStack.push(FlamestriderBikeRenderer.pushRiderBikeTransform(livingEntity, partialTicks, poseStack));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void velorise$renderAura(T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, CallbackInfo ci) {
        if (!velorise$bikePoseStack.isEmpty() && velorise$bikePoseStack.pop()) {
            poseStack.popPose();
        }
        ClientAuraRenderer.renderFrozenIceOverlay(livingEntity, this.model, poseStack, multiBufferSource);
        ClientAuraRenderer.renderJadeShieldBarrier(livingEntity, poseStack, multiBufferSource);
        ClientAuraRenderer.renderNightsFocusBarrier(livingEntity, poseStack, multiBufferSource);
        ClientAuraRenderer.render(livingEntity, poseStack, multiBufferSource, packedLight);
    }
}
