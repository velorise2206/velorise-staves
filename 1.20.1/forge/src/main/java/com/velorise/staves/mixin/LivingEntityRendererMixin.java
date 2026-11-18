package com.velorise.staves.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.velorise.staves.client.FlamestriderBikeRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import com.velorise.staves.client.ClientAuraRenderer;
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

    @Inject(method = "getBob(Lnet/minecraft/world/entity/LivingEntity;F)F", at = @At("HEAD"), cancellable = true)
    private void velorise$freezeBobOnPetrifyOrFreeze(T livingEntity, float partialTicks, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if (livingEntity != null && com.velorise.staves.client.ClientAuraTracker.isFrozen(livingEntity.getId())) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void velorise$onRenderPre(T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, CallbackInfo ci) {
        if (livingEntity != null && livingEntity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
            livingEntity.yBodyRot = bike.getYRot();
            livingEntity.yBodyRotO = bike.yRotO;
        } else if (livingEntity != null && (com.velorise.staves.client.character.NahidaVisualRenderer.getViewfinderFade(livingEntity) > 0.01F
                || com.velorise.staves.client.ClientAuraTracker.isRaidenSlashingOrBursting(livingEntity.getId()))) {
            livingEntity.yBodyRot = livingEntity.getYRot();
            livingEntity.yBodyRotO = livingEntity.yRotO;
        }
        velorise$bikePoseStack.push(FlamestriderBikeRenderer.pushRiderBikeTransform(livingEntity, partialTicks, poseStack));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void velorise$onRenderPost(T livingEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, CallbackInfo ci) {
        ClientAuraRenderer.renderMavuikaVirtualHelmet(livingEntity, this.model, partialTicks, poseStack, multiBufferSource);
        if (!velorise$bikePoseStack.isEmpty() && velorise$bikePoseStack.pop()) {
            poseStack.popPose();
        }
    }
}
