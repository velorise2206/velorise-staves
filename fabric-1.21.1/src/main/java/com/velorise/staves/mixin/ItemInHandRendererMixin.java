package com.velorise.staves.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animates First-Person hands & held staves and Third-Person scepter/polearm orientation
 * for Splendor of Tranquil Waters (Furina) and Vortex Vanquisher (Zhongli).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    protected abstract void renderPlayerArm(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            float equippedProgress,
            float swingProgress,
            HumanoidArm side
    );

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void velorise$onRenderThirdPersonItemPre(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int seed,
            CallbackInfo ci
    ) {
        poseStack.pushPose();
        ClientAuraRenderer.applyThirdPersonStaffItemTransform(entity, stack, displayContext, leftHand, poseStack);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void velorise$onRenderThirdPersonItemPost(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int seed,
            CallbackInfo ci
    ) {
        poseStack.popPose();
    }

    @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void velorise$onRenderFirstPersonArmPre(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo ci
    ) {
        ClientAuraRenderer.applyFirstPersonStaffTransform(player, partialTicks, hand, stack, poseStack);
    }

    @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void velorise$onRenderFirstPersonExtraOffhand(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo ci
    ) {
        if (hand == InteractionHand.MAIN_HAND && player != null && !player.isScoping()) {
            HumanoidArm mainArm = player.getMainArm();
            poseStack.pushPose();
            if (ClientAuraRenderer.setupFirstPersonVortexMainArmPose(player, partialTicks, mainArm, poseStack)) {
                this.renderPlayerArm(poseStack, buffer, combinedLight, 0.0F, 0.0F, mainArm);
            }
            poseStack.popPose();

            if (player.getOffhandItem().isEmpty()) {
                HumanoidArm offArm = mainArm.getOpposite();
                poseStack.pushPose();
                if (ClientAuraRenderer.setupFirstPersonEmptyOffhandPose(player, partialTicks, offArm, poseStack)) {
                    this.renderPlayerArm(poseStack, buffer, combinedLight, 0.0F, 0.0F, offArm);
                }
                poseStack.popPose();
            }
        }
    }
}
