package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * Dedicated client-side idle visual renderer for Hydro Archon Furina (Splendor of Tranquil Waters):
 * - Ousia (mode 1): Playful sway + 3 orbiting Salon Members
 * - Pneuma (mode 2): Floating Hydro Bubble seat + rotating 3-tier Cake Tower
 */
public final class FurinaVisualRenderer {
    private FurinaVisualRenderer() {}

    private static float stepIdleFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.furinaIdleLastMs <= 0L) {
            d.furinaIdleLastMs = nowMs;
            if (d.furinaIdleMode > 0) {
                d.furinaIdleFade = Math.min(1.0F, d.furinaIdleFade + 0.016F / 0.45F);
            }
        } else if (nowMs > d.furinaIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.furinaIdleLastMs) / 1000.0F);
            d.furinaIdleLastMs = nowMs;
            if (d.furinaIdleMode > 0) {
                d.furinaIdleFade = Math.min(1.0F, d.furinaIdleFade + dt / 0.45F);
            } else {
                d.furinaIdleFade = Math.max(0.0F, d.furinaIdleFade - dt / 0.35F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.furinaIdleFade);
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (!ClientAuraTracker.isFurinaIdle(entity.getId())) return false;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F || d == null) return false;
        int mode = d.furinaIdleMode > 0 ? d.furinaIdleMode : d.furinaIdleLastMode;
        if (mode == 2) {
            float bob = Mth.sin(ageInTicks * 0.12F) * 0.35F * w;
            float sitDropY = 3.2F * w + bob;
            model.body.y += sitDropY;
            model.head.y += sitDropY;
            model.hat.y += sitDropY;
            model.rightArm.y += sitDropY;
            model.leftArm.y += sitDropY;
            model.rightLeg.y += sitDropY;
            model.leftLeg.y += sitDropY;

            model.body.xRot = Mth.lerp(w, model.body.xRot, 0.10F);
            model.head.xRot = Mth.lerp(w, model.head.xRot, 0.20F);
            model.head.zRot = Mth.lerp(w, model.head.zRot, 0.10F);
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -0.82F + Mth.sin(ageInTicks * 0.15F) * 0.08F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.25F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, 0.25F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.30F);
            model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -1.25F);
            model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, -1.12F);
            model.rightLeg.yRot = Mth.lerp(w, model.rightLeg.yRot, 0.12F);
            model.leftLeg.yRot = Mth.lerp(w, model.leftLeg.yRot, -0.12F);
        } else {
            float sway = Mth.sin(ageInTicks * 0.14F);
            model.body.yRot = Mth.lerp(w, model.body.yRot, 0.12F * sway);
            model.head.zRot = Mth.lerp(w, model.head.zRot, 0.10F * sway);
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -0.55F + 0.15F * sway);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.42F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -0.42F - 0.15F * sway);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.42F);
        }
        return true;
    }

    public static void renderIdleEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        if (!ClientAuraTracker.isFurinaIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.01F || d == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) return;

        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        int mode = d.furinaIdleMode > 0 ? d.furinaIdleMode : d.furinaIdleLastMode;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.scale(fade, fade, fade);

        if (mode == 2) {
            float bob = Mth.sin(time * 0.12F) * 0.035F;
            VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.46F + bob, 0.0F);
            Matrix4f bMat = poseStack.last().pose();
            int bAlpha = Math.max(15, Math.round(115.0F * fade));
            float br = 0.42F;
            for (int seg = 0; seg < 8; seg++) {
                float a0 = (float) (seg * Math.PI * 2.0D / 8.0D);
                float a1 = (float) ((seg + 1) * Math.PI * 2.0D / 8.0D);
                float x0 = Mth.cos(a0) * br, z0 = Mth.sin(a0) * br;
                float x1 = Mth.cos(a1) * br, z1 = Mth.sin(a1) * br;
                WindGliderRenderer.emitDoubleQuad(transVc, bMat, x0, -0.22F, z0, x1, -0.22F, z1, x1 * 0.85F, 0.24F, z1 * 0.85F, x0 * 0.85F, 0.24F, z0 * 0.85F, 120, 225, 255, bAlpha);
                WindGliderRenderer.emitDoubleQuad(transVc, bMat, x0 * 0.85F, 0.24F, z0 * 0.85F, x1 * 0.85F, 0.24F, z1 * 0.85F, 0.0F, 0.36F, 0.0F, 0.0F, 0.36F, 0.0F, 185, 245, 255, bAlpha);
                WindGliderRenderer.emitDoubleQuad(transVc, bMat, x0, -0.22F, z0, x1, -0.22F, z1, 0.0F, -0.36F, 0.0F, 0.0F, -0.36F, 0.0F, 95, 195, 255, bAlpha);
            }
            poseStack.popPose();

            VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.72F + bob * 1.2F, 0.72F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
            Matrix4f cMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, cMat, -0.20F, 0.0F, -0.20F, 0.20F, 0.03F, 0.20F, 255, 210, 75, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, cMat, -0.04F, -0.10F, -0.04F, 0.04F, 0.0F, 0.04F, 255, 210, 75, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, cMat, -0.16F, 0.03F, -0.16F, 0.16F, 0.13F, 0.16F, 255, 240, 225, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, cMat, -0.165F, 0.11F, -0.165F, 0.165F, 0.14F, 0.165F, 255, 155, 190, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, cMat, -0.115F, 0.14F, -0.115F, 0.115F, 0.23F, 0.115F, 210, 240, 255, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, cMat, -0.07F, 0.23F, -0.07F, 0.07F, 0.30F, 0.07F, 255, 225, 235, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, cMat, -0.025F, 0.30F, -0.025F, 0.025F, 0.35F, 0.025F, 235, 45, 75, 255);
            poseStack.popPose();
        } else {
            VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
            for (int m = 0; m < 3; m++) {
                float ang = (float) (time * 0.07D + m * Math.PI * 2.0D / 3.0D);
                float mx = Mth.cos(ang) * 0.85F;
                float mz = Mth.sin(ang) * 0.85F;
                float my = 0.72F + Mth.sin(time * 0.18F + m * 2.1F) * 0.14F;
                poseStack.pushPose();
                poseStack.translate(mx, my, mz);
                poseStack.mulPose(Axis.YP.rotationDegrees(-ang * 57.2958F));
                Matrix4f mMat = poseStack.last().pose();
                if (m == 0) {
                    ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mMat, -0.10F, -0.08F, -0.10F, 0.10F, 0.10F, 0.10F, 55, 115, 225, 255);
                    ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mMat, -0.08F, 0.10F, -0.08F, 0.08F, 0.13F, 0.08F, 25, 30, 55, 255);
                    ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mMat, -0.055F, 0.13F, -0.055F, 0.055F, 0.21F, 0.055F, 35, 40, 75, 255);
                } else if (m == 1) {
                    ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mMat, -0.07F, -0.10F, -0.08F, 0.07F, 0.12F, 0.08F, 85, 205, 255, 255);
                    ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mMat, -0.04F, 0.12F, -0.04F, 0.04F, 0.18F, 0.04F, 255, 215, 85, 255);
                } else {
                    ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mMat, -0.12F, -0.06F, -0.09F, 0.12F, 0.08F, 0.09F, 115, 225, 255, 255);
                    ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mMat, -0.05F, 0.08F, -0.03F, 0.05F, 0.13F, 0.03F, 255, 130, 175, 255);
                }
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }
}
