package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.client.reaction.ElementalReactionBadgeRenderer;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated visual renderer for Dendro Archon Nahida (A Thousand Floating Dreams):
 * - Jade crystal vine swing, hopscotch N1-N4 ground tiles & sprouting clovers, CA viewfinder box & giant clover burst
 * - Seed of Skandha 3D mark & world link arcs
 * - Shrine of Maya 3D geodesic lotus dome mesh & sanctuary pillars
 * - 360-degree pirouette hop, viewfinder framing HUD & poses
 */
public final class NahidaVisualRenderer {
    private NahidaVisualRenderer() {}

    public static boolean renderViewfinderHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !ClientAuraTracker.isNahidaViewfinder(mc.player.getId())) return false;
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int cx = sw / 2;
        int cy = sh / 2;
        int hw = 70;
        int hh = 52;
        int borderCol = 0xEE4AF072;
        int starCol = 0xFFF0FFC8;
        int dotCol = 0xCC74FF92;
        int scanCol = 0x1440E060;

        guiGraphics.fill(cx - hw, cy - hh, cx + hw, cy + hh, scanCol);

        int arm = 16;
        int thick = 2;
        guiGraphics.fill(cx - hw, cy - hh, cx - hw + arm, cy - hh + thick, borderCol);
        guiGraphics.fill(cx - hw, cy - hh, cx - hw + thick, cy - hh + arm, borderCol);
        guiGraphics.fill(cx + hw - arm, cy - hh, cx + hw, cy - hh + thick, borderCol);
        guiGraphics.fill(cx + hw - thick, cy - hh, cx + hw, cy - hh + arm, borderCol);
        guiGraphics.fill(cx - hw, cy + hh - thick, cx - hw + arm, cy + hh, borderCol);
        guiGraphics.fill(cx - hw, cy + hh - arm, cx - hw + thick, cy + hh, borderCol);
        guiGraphics.fill(cx + hw - arm, cy + hh - thick, cx + hw, cy + hh, borderCol);
        guiGraphics.fill(cx + hw - thick, cy + hh - arm, cx + hw, cy + hh, borderCol);

        for (int[] corner : new int[][]{{cx - hw, cy - hh}, {cx + hw, cy - hh}, {cx - hw, cy + hh}, {cx + hw, cy + hh}}) {
            guiGraphics.fill(corner[0] - 2, corner[1] - 1, corner[0] + 3, corner[1] + 2, starCol);
            guiGraphics.fill(corner[0] - 1, corner[1] - 2, corner[0] + 2, corner[1] + 3, starCol);
            guiGraphics.fill(corner[0], corner[1], corner[0] + 1, corner[1] + 1, 0xFFFFFFFF);
        }

        for (int offset = -40; offset <= 40; offset += 20) {
            if (offset != 0) {
                guiGraphics.fill(cx + offset - 1, cy - hh, cx + offset + 1, cy - hh + 2, dotCol);
                guiGraphics.fill(cx + offset - 1, cy + hh - 2, cx + offset + 1, cy + hh, dotCol);
            }
        }
        for (int offset = -30; offset <= 30; offset += 20) {
            if (offset != 0) {
                guiGraphics.fill(cx - hw, cy + offset - 1, cx - hw + 2, cy + offset + 1, dotCol);
                guiGraphics.fill(cx + hw - 2, cy + offset - 1, cx + hw, cy + offset + 1, dotCol);
            }
        }

        guiGraphics.fill(cx - 5, cy - 1, cx - 2, cy + 1, borderCol);
        guiGraphics.fill(cx + 2, cy - 1, cx + 5, cy + 1, borderCol);
        guiGraphics.fill(cx - 1, cy - 5, cx + 1, cy - 2, borderCol);
        guiGraphics.fill(cx - 1, cy + 2, cx + 1, cy + 5, borderCol);
        return true;
    }

    public static boolean applyIdleAndViewfinderHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (ClientAuraTracker.isNahidaIdleSwing(entity.getId())) {
            float sway = Mth.sin(ageInTicks * 0.12F);
            model.body.xRot = 0.06F * sway;
            model.rightLeg.xRot = -1.25F + 0.18F * sway;
            model.leftLeg.xRot = -1.25F - 0.18F * sway;
            model.rightArm.xRot = -0.82F + 0.05F * sway;
            model.leftArm.xRot = -0.82F + 0.05F * sway;
            model.rightArm.zRot = 0.22F;
            model.leftArm.zRot = -0.22F;
            return true;
        }
        if (ClientAuraTracker.isNahidaViewfinder(entity.getId())) {
            model.rightArm.xRot = -1.55F + model.head.xRot * 0.65F;
            model.leftArm.xRot = -1.55F + model.head.xRot * 0.65F;
            model.rightArm.yRot = -0.42F + model.head.yRot * 0.5F;
            model.leftArm.yRot = 0.42F + model.head.yRot * 0.5F;
            return true;
        }
        return false;
    }

    public static boolean applyActionHumanoidAnimation(
            HumanoidModel<?> model,
            float ageInTicks,
            int activeAnim,
            float t
    ) {
        if (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4) {
            float hop = Mth.sin(t * (float) Math.PI);
            int step = activeAnim - ClientAuraTracker.ANIM_NAHIDA_NA_1 + 1;
            if (step == 1) {
                model.body.yRot = -0.22F * hop;
                model.rightLeg.xRot = -0.75F * hop;
                model.leftLeg.xRot = 0.20F * hop;
                model.rightArm.xRot = -0.95F * hop;
                model.leftArm.xRot = 0.45F * hop;
                model.rightArm.zRot = 0.35F * hop;
            } else if (step == 2) {
                model.body.yRot = 0.22F * hop;
                model.leftLeg.xRot = -0.75F * hop;
                model.rightLeg.xRot = 0.20F * hop;
                model.leftArm.xRot = -0.95F * hop;
                model.rightArm.xRot = 0.45F * hop;
                model.leftArm.zRot = -0.35F * hop;
            } else if (step == 3) {
                // Step 3 (Penultimate): Light hop bounce & 360-degree pirouette spin
                model.body.yRot = (float) Math.PI * 2.0F * t;
                model.rightLeg.xRot = -0.45F * hop;
                model.leftLeg.xRot = 0.35F * hop;
                model.rightArm.xRot = -1.45F * hop;
                model.leftArm.xRot = -1.45F * hop;
                model.rightArm.zRot = 0.65F * hop;
                model.leftArm.zRot = -0.65F * hop;
            } else {
                // Step 4 (Finisher): Light hop bounce & 360-degree pirouette spin with finger snap finish
                model.body.yRot = (float) Math.PI * 2.0F * t;
                model.rightLeg.xRot = -0.55F * hop;
                model.leftLeg.xRot = -0.55F * hop;
                model.rightArm.xRot = -1.65F * hop;
                model.leftArm.xRot = -1.65F * hop;
                model.rightArm.yRot = -0.42F * hop;
                model.leftArm.yRot = 0.42F * hop;
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_HOLD_E) {
            float w = (t < 0.15F) ? WindGliderRenderer.smoothStep01(t / 0.15F) : 1.0F;
            float sway = Mth.sin(ageInTicks * 0.12F) * 0.04F;
            model.leftLeg.xRot = 0.22F + sway;
            model.rightLeg.xRot = 0.32F - sway;
            model.leftLeg.zRot = -0.08F;
            model.rightLeg.zRot = 0.08F;
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.45F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.45F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.42F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.42F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.15F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.15F);
            model.head.xRot += 0.04F * w;
            model.hat.copyFrom(model.head);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_TAP_E) {
            float spinAngle = (float) Math.PI * 2.0F * t;
            float cosS = Mth.cos(spinAngle);
            float sinS = Mth.sin(spinAngle);
            float sweep = Mth.sin(t * (float) Math.PI);

            float hopY = -2.5F * sweep;
            model.body.y += hopY;
            model.head.y += hopY;
            model.hat.y += hopY;

            model.body.yRot = spinAngle;
            model.head.yRot = spinAngle;
            model.hat.copyFrom(model.head);

            model.rightArm.x = -5.0F * cosS;
            model.rightArm.z = -5.0F * sinS;
            model.rightArm.y = 2.0F + hopY;
            model.rightArm.xRot = -0.55F * sweep;
            model.rightArm.yRot = spinAngle - 0.20F * sweep;
            model.rightArm.zRot = 0.72F * sweep;

            model.leftArm.x = 5.0F * cosS;
            model.leftArm.z = 5.0F * sinS;
            model.leftArm.y = 2.0F + hopY;
            model.leftArm.xRot = -0.55F * sweep;
            model.leftArm.yRot = spinAngle + 0.20F * sweep;
            model.leftArm.zRot = -0.72F * sweep;

            model.rightLeg.x = -1.9F * cosS;
            model.rightLeg.z = -1.9F * sinS;
            model.rightLeg.y = 12.0F + hopY;
            model.rightLeg.yRot = spinAngle;
            model.rightLeg.xRot = 0.22F * sweep;

            model.leftLeg.x = 1.9F * cosS;
            model.leftLeg.z = 1.9F * sinS;
            model.leftLeg.y = 12.0F + hopY;
            model.leftLeg.yRot = spinAngle;
            model.leftLeg.xRot = -0.15F * sweep;
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_CA) {
            float w = (t < 0.20F) ? WindGliderRenderer.smoothStep01(t / 0.20F) : (t < 0.70F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.70F) / 0.30F));
            model.body.xRot = Mth.lerp(t, 0.08F, -0.12F);
            model.body.yRot = Mth.lerp(t, -0.22F, 0.25F);
            model.rightArm.xRot = Mth.lerp(t, 0.20F, -1.85F);
            model.rightArm.yRot = Mth.lerp(t, -0.65F, 0.45F);
            model.rightArm.zRot = Mth.lerp(t, -0.45F, 0.35F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, 0.45F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, -0.32F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_BURST) {
            float w = (t < 0.20F) ? WindGliderRenderer.smoothStep01(t / 0.20F) : (t < 0.80F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.80F) / 0.20F));
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.35F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.35F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.72F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.72F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.18F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0.18F);
            model.head.xRot += 0.16F * w;
            model.hat.copyFrom(model.head);
            model.leftLeg.xRot = 0.12F * w;
            model.rightLeg.xRot = 0.12F * w;
            return true;
        }
        return false;
    }

    public static boolean applyFirstPersonTransform(
            ArcaneStaffItem.Kind effKind,
            float dir,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (effKind != ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) return false;
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_CA && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            poseStack.translate(-dir * 0.35F * (1.0F - t) + dir * 0.22F * t, -0.38F + 0.65F * t, -0.22F - 0.25F * t);
            poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F + 55.0F * t));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * (25.0F - 45.0F * t)));
            return true;
        }
        return false;
    }

    public static void renderEntityEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        long now = System.currentTimeMillis();
        int activeAnim = (d != null && now < d.staffAnimUntil) ? d.staffAnimType : 0;
        boolean idleSwing = ClientAuraTracker.isNahidaIdleSwing(id);
        boolean skandha = ClientAuraTracker.hasSkandhaMark(id);
        boolean shrine = ClientAuraTracker.hasNahidaShrine(id);
        boolean nahidaAnim = (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4)
                || activeAnim == ClientAuraTracker.ANIM_NAHIDA_CA;

        if (!idleSwing && !skandha && !shrine && !nahidaAnim) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);

        if (idleSwing && !isFirstPersonSelf) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            float sway = Mth.sin(time * 0.10F) * 0.08F;
            poseStack.translate(0.0F, 0.0F, sway);
            Matrix4f mat = poseStack.last().pose();
            WindGliderRenderer.emitDoubleQuad(vc, mat, -0.46F, 0.46F, -0.16F, 0.46F, 0.46F, -0.16F, 0.46F, 0.52F, 0.16F, -0.46F, 0.52F, 0.16F, 60, 235, 120, 220);
            WindGliderRenderer.emitDoubleQuad(vc, mat, -0.48F, 0.45F, -0.17F, 0.48F, 0.45F, -0.17F, 0.48F, 0.47F, 0.17F, -0.48F, 0.47F, 0.17F, 235, 215, 90, 240);
            WindGliderRenderer.emitDoubleQuad(vc, mat, -0.48F, 2.62F, -0.04F, 0.48F, 2.62F, -0.04F, 0.48F, 2.68F, 0.04F, -0.48F, 2.68F, 0.04F, 240, 220, 95, 230);
            WindGliderRenderer.emitDoubleQuad(vc, mat, -0.43F, 0.48F, -0.02F, -0.39F, 0.48F, 0.02F, -0.39F, 2.65F, 0.02F, -0.43F, 2.65F, -0.02F, 85, 235, 115, 225);
            WindGliderRenderer.emitDoubleQuad(vc, mat, 0.39F, 0.48F, -0.02F, 0.43F, 0.48F, 0.02F, 0.43F, 2.65F, 0.02F, 0.39F, 2.65F, -0.02F, 85, 235, 115, 225);
            float[] yStars = {0.90F, 1.40F, 1.90F, 2.40F};
            for (float yStar : yStars) {
                WindGliderRenderer.renderCloverCross2D(vc, mat, -0.41F, yStar, 0.0F, 0.11F, 240, 255, 160, 245);
                WindGliderRenderer.renderCloverCross2D(vc, mat,  0.41F, yStar, 0.0F, 0.11F, 240, 255, 160, 245);
            }
            WindGliderRenderer.renderCloverCross2D(vc, mat, -0.46F, 0.49F, 0.0F, 0.12F, 245, 255, 175, 250);
            WindGliderRenderer.renderCloverCross2D(vc, mat,  0.46F, 0.49F, 0.0F, 0.12F, 245, 255, 175, 250);
            poseStack.popPose();
        }

        if (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            Matrix4f mat = poseStack.last().pose();
            int step = activeAnim - ClientAuraTracker.ANIM_NAHIDA_NA_1 + 1;

            float tileHalf = 0.45F;
            WindGliderRenderer.renderCloverCrossXZ(vc, mat, -tileHalf, 0.045F, -tileHalf, 0.16F, 225, 255, 140, 230);
            WindGliderRenderer.renderCloverCrossXZ(vc, mat,  tileHalf, 0.045F, -tileHalf, 0.16F, 225, 255, 140, 230);
            WindGliderRenderer.renderCloverCrossXZ(vc, mat,  tileHalf, 0.045F,  tileHalf, 0.16F, 225, 255, 140, 230);
            WindGliderRenderer.renderCloverCrossXZ(vc, mat, -tileHalf, 0.045F,  tileHalf, 0.16F, 225, 255, 140, 230);
            WindGliderRenderer.emitDoubleQuad(vc, mat, -tileHalf, 0.042F, -tileHalf,  tileHalf, 0.042F, -tileHalf,  tileHalf, 0.042F, -tileHalf + 0.03F, -tileHalf, 0.042F, -tileHalf + 0.03F, 95, 255, 140, 210);
            WindGliderRenderer.emitDoubleQuad(vc, mat, -tileHalf, 0.042F,  tileHalf - 0.03F,  tileHalf, 0.042F,  tileHalf - 0.03F,  tileHalf, 0.042F,  tileHalf, -tileHalf, 0.042F,  tileHalf, 95, 255, 140, 210);
            WindGliderRenderer.emitDoubleQuad(vc, mat, -tileHalf, 0.042F, -tileHalf, -tileHalf + 0.03F, 0.042F, -tileHalf, -tileHalf + 0.03F, 0.042F,  tileHalf, -tileHalf, 0.042F,  tileHalf, 95, 255, 140, 210);
            WindGliderRenderer.emitDoubleQuad(vc, mat,  tileHalf - 0.03F, 0.042F, -tileHalf,  tileHalf, 0.042F, -tileHalf,  tileHalf, 0.042F,  tileHalf,  tileHalf - 0.03F, 0.042F,  tileHalf, 95, 255, 140, 210);
            for (int s = 0; s < 12; s++) {
                float sa0 = (float) (s * Math.PI * 2.0D / 12);
                float sa1 = (float) ((s + 1) * Math.PI * 2.0D / 12);
                WindGliderRenderer.emitDoubleQuad(vc, mat,
                        Mth.cos(sa0) * 0.22F, 0.044F, Mth.sin(sa0) * 0.22F,
                        Mth.cos(sa1) * 0.22F, 0.044F, Mth.sin(sa1) * 0.22F,
                        Mth.cos(sa1) * 0.25F, 0.044F, Mth.sin(sa1) * 0.25F,
                        Mth.cos(sa0) * 0.25F, 0.044F, Mth.sin(sa0) * 0.25F,
                        235, 250, 160, 185);
            }

            if (step == 1) {
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.03F, 0.04F, 0.85F, 0.03F, 0.04F, 0.85F, 0.03F, 0.68F, 0.85F, -0.03F, 0.68F, 0.85F, 75, 240, 110, 220);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, 0.0F, 0.72F, 0.85F, 0.22F, 120, 255, 150, 235);
            } else if (step == 2) {
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.31F, 0.04F, 0.92F, -0.25F, 0.04F, 0.92F, -0.25F, 0.62F, 0.92F, -0.31F, 0.62F, 0.92F, 75, 240, 110, 220);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, -0.28F, 0.66F, 0.92F, 0.20F, 120, 255, 150, 235);
                WindGliderRenderer.emitDoubleQuad(vc, mat, 0.25F, 0.04F, 0.92F, 0.31F, 0.04F, 0.92F, 0.31F, 0.92F, 0.92F, 0.25F, 0.92F, 0.92F, 75, 240, 110, 220);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, 0.28F, 0.96F, 0.92F, 0.24F, 135, 255, 165, 240);
            } else if (step == 3) {
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.38F, 0.04F, 1.02F, -0.32F, 0.04F, 1.02F, -0.32F, 0.75F, 1.02F, -0.38F, 0.75F, 1.02F, 75, 240, 110, 220);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, -0.35F, 0.79F, 1.02F, 0.22F, 120, 255, 150, 235);
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.03F, 0.04F, 1.08F, 0.03F, 0.04F, 1.08F, 0.03F, 1.15F, 1.08F, -0.03F, 1.15F, 1.08F, 85, 245, 120, 230);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, 0.0F, 1.20F, 1.08F, 0.28F, 140, 255, 175, 245);
                WindGliderRenderer.emitDoubleQuad(vc, mat, 0.32F, 0.04F, 1.02F, 0.38F, 0.04F, 1.02F, 0.38F, 0.85F, 1.02F, 0.32F, 0.85F, 1.02F, 75, 240, 110, 220);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, 0.35F, 0.89F, 1.02F, 0.22F, 120, 255, 150, 235);
            } else {
                float zH = 1.15F;
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.45F, 0.06F, zH, -0.39F, 0.06F, zH, -0.50F, 0.65F, zH, -0.56F, 0.65F, zH, 75, 240, 110, 225);
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.56F, 0.65F, zH, -0.50F, 0.65F, zH, -0.42F, 1.15F, zH, -0.48F, 1.15F, zH, 85, 245, 120, 235);
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.48F, 1.15F, zH, -0.42F, 1.15F, zH, -0.22F, 1.32F, zH, -0.25F, 1.38F, zH, 110, 255, 145, 240);
                WindGliderRenderer.emitDoubleQuad(vc, mat, -0.25F, 1.38F, zH, -0.22F, 1.32F, zH, 0.0F, 0.95F, zH, 0.0F, 1.02F, zH, 130, 255, 160, 245);
                WindGliderRenderer.emitDoubleQuad(vc, mat, 0.39F, 0.06F, zH, 0.45F, 0.06F, zH, 0.56F, 0.65F, zH, 0.50F, 0.65F, zH, 75, 240, 110, 225);
                WindGliderRenderer.emitDoubleQuad(vc, mat, 0.50F, 0.65F, zH, 0.56F, 0.65F, zH, 0.48F, 1.15F, zH, 0.42F, 1.15F, zH, 85, 245, 120, 235);
                WindGliderRenderer.emitDoubleQuad(vc, mat, 0.42F, 1.15F, zH, 0.48F, 1.15F, zH, 0.25F, 1.38F, zH, 0.22F, 1.32F, zH, 110, 255, 145, 240);
                WindGliderRenderer.emitDoubleQuad(vc, mat, 0.22F, 1.32F, zH, 0.25F, 1.38F, zH, 0.0F, 1.02F, zH, 0.0F, 0.95F, zH, 130, 255, 160, 245);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, -0.25F, 1.35F, zH, 0.24F, 140, 255, 175, 245);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat,  0.25F, 1.35F, zH, 0.24F, 140, 255, 175, 245);
                WindGliderRenderer.renderFourLeafClover2D(vc, mat,  0.0F,  0.98F, zH, 0.26F, 220, 255, 190, 250);
            }
            poseStack.popPose();
        } else if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_CA && !isFirstPersonSelf && d != null) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            Matrix4f mat = poseStack.last().pose();
            float z = 1.10F;
            float cy = 1.15F;
            float caProg = Mth.clamp(1.0F - (d.staffAnimUntil - now) / 500.0F, 0.0F, 1.0F);

            if (caProg < 0.52F) {
                float hw = 0.62F;
                float hh = 0.44F;
                WindGliderRenderer.emitDoubleQuad(vc, mat, -hw, cy - hh, z,  hw, cy - hh, z,  hw, cy - hh + 0.03F, z, -hw, cy - hh + 0.03F, z, 220, 255, 140, 225);
                WindGliderRenderer.emitDoubleQuad(vc, mat, -hw, cy + hh - 0.03F, z,  hw, cy + hh - 0.03F, z,  hw, cy + hh, z, -hw, cy + hh, z, 220, 255, 140, 225);
                WindGliderRenderer.emitDoubleQuad(vc, mat, -hw, cy - hh, z, -hw + 0.03F, cy - hh, z, -hw + 0.03F, cy + hh, z, -hw, cy + hh, z, 220, 255, 140, 225);
                WindGliderRenderer.emitDoubleQuad(vc, mat,  hw - 0.03F, cy - hh, z,  hw, cy - hh, z,  hw, cy + hh, z,  hw - 0.03F, cy + hh, z, 220, 255, 140, 225);
                WindGliderRenderer.renderCloverCross2D(vc, mat, -hw, cy - hh, z, 0.14F, 245, 255, 150, 245);
                WindGliderRenderer.renderCloverCross2D(vc, mat,  hw, cy - hh, z, 0.14F, 245, 255, 150, 245);
                WindGliderRenderer.renderCloverCross2D(vc, mat,  hw, cy + hh, z, 0.14F, 245, 255, 150, 245);
                WindGliderRenderer.renderCloverCross2D(vc, mat, -hw, cy + hh, z, 0.14F, 245, 255, 150, 245);
            } else {
                float boom = (caProg - 0.52F) / 0.48F;
                float cloverRad = 0.72F + boom * 0.55F;
                int aBoom = Math.round(245.0F * (1.0F - boom * 0.35F));
                WindGliderRenderer.renderFourLeafClover2D(vc, mat, 0.0F, cy, z, cloverRad, 95, 255, 140, aBoom);
                WindGliderRenderer.renderCloverCross2D(vc, mat, 0.0F, cy, z - 0.01F, cloverRad * 0.45F, 250, 255, 200, aBoom);
                float ringR = 0.45F + boom * 1.15F;
                for (int s = 0; s < 16; s++) {
                    float ra0 = (float) (s * Math.PI * 2.0D / 16);
                    float ra1 = (float) ((s + 1) * Math.PI * 2.0D / 16);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            Mth.cos(ra0) * ringR, cy + Mth.sin(ra0) * ringR, z,
                            Mth.cos(ra1) * ringR, cy + Mth.sin(ra1) * ringR, z,
                            Mth.cos(ra1) * (ringR + 0.06F), cy + Mth.sin(ra1) * (ringR + 0.06F), z,
                            Mth.cos(ra0) * (ringR + 0.06F), cy + Mth.sin(ra0) * (ringR + 0.06F), z,
                            210, 255, 175, Math.max(15, Math.round(180.0F * (1.0F - boom))));
                }
            }
            poseStack.popPose();
        }

        if (skandha) {
            ResourceLocation skandhaTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/seed_of_skandha.png");
            VertexConsumer skVc = buffer.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.58F + Mth.sin(time * 0.14F) * 0.04F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.5F));
            float half = Math.max(0.38F, entity.getBbWidth() * 0.58F);
            for (int plane = 0; plane < 2; plane++) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(plane * 90.0F));
                PoseStack.Pose p = poseStack.last();
                WindGliderRenderer.emitWingQuad(skVc, p, -half, -half, 0.0F, 0.0F, 1.0F, half, -half, 0.0F, 1.0F, 1.0F, half, half, 0.0F, 1.0F, 0.0F, -half, half, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                WindGliderRenderer.emitWingQuad(skVc, p, -half, half, 0.0F, 0.0F, 0.0F, half, half, 0.0F, 1.0F, 0.0F, half, -half, 0.0F, 1.0F, 1.0F, -half, -half, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                poseStack.popPose();
            }
            poseStack.popPose();
            vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        }

        if (shrine && !isFirstPersonSelf) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
            Matrix4f mat = poseStack.last().pose();
            int segs = 16;
            float rIn = 1.35F, rOut = 1.52F;
            for (int i = 0; i < segs; i++) {
                float a0 = (float) (i * Math.PI * 2.0D / segs);
                float a1 = (float) ((i + 1) * Math.PI * 2.0D / segs);
                WindGliderRenderer.emitDoubleQuad(vc, mat,
                        Mth.cos(a0) * rIn, 0.05F, Mth.sin(a0) * rIn,
                        Mth.cos(a0) * rOut, 0.05F, Mth.sin(a0) * rOut,
                        Mth.cos(a1) * rOut, 0.05F, Mth.sin(a1) * rOut,
                        Mth.cos(a1) * rIn, 0.05F, Mth.sin(a1) * rIn,
                        85, 255, 130, 175);
            }
            poseStack.popPose();
        }
    }

    public static void renderWorldArcs(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ResourceLocation skandhaTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/seed_of_skandha.png");

        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.start == null || arc.end == null) continue;
            if (arc.fromEntityId == -5) {
                float radius = (float) Math.max(4.0D, arc.end.x);
                float cx = (float) (arc.start.x - camPos.x);
                float cy = (float) (arc.start.y - camPos.y);
                float cz = (float) (arc.start.z - camPos.z);
                float fadeIn = Mth.clamp((now - arc.startTime) / 350.0F, 0.0F, 1.0F);
                float fadeOut = Mth.clamp((arc.expireTime - now) / 450.0F, 0.0F, 1.0F);
                float fade = Math.min(fadeIn, fadeOut);
                float timeSec = (now - arc.startTime) * 0.001F;
                int aMain = Math.max(20, Math.round(165.0F * fade));
                int aTrim = Math.max(25, Math.round(220.0F * fade));

                VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
                poseStack.pushPose();
                poseStack.translate(cx, cy, cz);
                Matrix4f mat = poseStack.last().pose();

                int floorSegs = 32;
                float rOuter = radius * 0.96F;
                float rInner = radius * 0.88F;
                float rMid1 = radius * 0.52F;
                float rMid0 = radius * 0.46F;
                for (int i = 0; i < floorSegs; i++) {
                    float a0 = (float) (i * Math.PI * 2.0D / floorSegs) + timeSec * 0.25F;
                    float a1 = (float) ((i + 1) * Math.PI * 2.0D / floorSegs) + timeSec * 0.25F;
                    float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
                    float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            c0 * rInner, 0.05F, s0 * rInner,
                            c0 * rOuter, 0.05F, s0 * rOuter,
                            c1 * rOuter, 0.05F, s1 * rOuter,
                            c1 * rInner, 0.05F, s1 * rInner,
                            85, 255, 130, aMain);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            c0 * rMid0, 0.05F, s0 * rMid0,
                            c0 * rMid1, 0.05F, s0 * rMid1,
                            c1 * rMid1, 0.05F, s1 * rMid1,
                            c1 * rMid0, 0.05F, s1 * rMid0,
                            235, 215, 95, aMain);
                }

                int pillars = 8;
                float pRadius = radius * 0.86F;
                float pillarH = 6.2F;
                float domeApexY = 10.8F;
                for (int p = 0; p < pillars; p++) {
                    float ang = (float) (p * Math.PI * 2.0D / pillars);
                    float nextAng = (float) ((p + 1) * Math.PI * 2.0D / pillars);
                    float midAng = (ang + nextAng) * 0.5F;
                    float px = Mth.cos(ang) * pRadius;
                    float pz = Mth.sin(ang) * pRadius;
                    float nx = Mth.cos(nextAng) * pRadius;
                    float nz = Mth.sin(nextAng) * pRadius;
                    float mx = Mth.cos(midAng) * pRadius;
                    float mz = Mth.sin(midAng) * pRadius;

                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.36F, 0.02F, pz - 0.36F, px + 0.36F, 0.48F, pz + 0.36F, 240, 205, 85, aTrim);
                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.22F, 0.48F, pz - 0.22F, px + 0.22F, pillarH - 0.42F, pz + 0.22F, 68, 235, 115, aMain);
                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.38F, pillarH - 0.42F, pz - 0.38F, px + 0.38F, pillarH, pz + 0.38F, 245, 215, 95, aTrim);

                    float archPeakY = pillarH + 1.35F;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            px, pillarH, pz,
                            mx, archPeakY, mz,
                            mx, archPeakY + 0.22F, mz,
                            px, pillarH + 0.22F, pz,
                            240, 212, 92, aTrim);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx, archPeakY, mz,
                            nx, pillarH, nz,
                            nx, pillarH + 0.22F, nz,
                            mx, archPeakY + 0.22F, mz,
                            240, 212, 92, aTrim);

                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx - 0.12F, archPeakY - 0.45F, mz - 0.12F,
                            mx + 0.12F, archPeakY - 0.45F, mz + 0.12F,
                            mx + 0.02F, archPeakY, mz + 0.02F,
                            mx - 0.02F, archPeakY, mz - 0.02F,
                            95, 255, 145, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 0.65F, mz, 0.10F, 85, 255, 130, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 0.90F, mz, 0.08F, 120, 255, 160, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 1.15F, mz, 0.06F, 215, 255, 185, aTrim);

                    float midR = pRadius * 0.54F;
                    float midY = pillarH + 2.6F;
                    float mx0 = Mth.cos(ang) * midR, mz0 = Mth.sin(ang) * midR;
                    float mx1 = Mth.cos(nextAng) * midR, mz1 = Mth.sin(nextAng) * midR;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            px, pillarH, pz,
                            nx, pillarH, nz,
                            mx1, midY, mz1,
                            mx0, midY, mz0,
                            65, 235, 115, Math.max(18, aMain / 2));
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx0, midY, mz0,
                            mx1, midY, mz1,
                            0.0F, domeApexY, 0.0F,
                            0.0F, domeApexY, 0.0F,
                            95, 255, 145, Math.max(22, (aMain * 2) / 3));
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            px - 0.08F, pillarH, pz - 0.08F,
                            px + 0.08F, pillarH, pz + 0.08F,
                            mx0 + 0.06F, midY, mz0 + 0.06F,
                            mx0 - 0.06F, midY, mz0 - 0.06F,
                            250, 225, 110, aTrim);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx0 - 0.06F, midY, mz0 - 0.06F,
                            mx0 + 0.06F, midY, mz0 + 0.06F,
                            0.04F, domeApexY, 0.04F,
                            -0.04F, domeApexY, -0.04F,
                            250, 225, 110, aTrim);
                }

                int raySegs = 16;
                float rayRTop = 0.75F;
                float rayRBot = 3.20F;
                int aRay = Math.max(10, Math.round(52.0F * fade));
                for (int s = 0; s < raySegs; s++) {
                    float ra0 = (float) (s * Math.PI * 2.0D / raySegs) + timeSec * 0.15F;
                    float ra1 = (float) ((s + 1) * Math.PI * 2.0D / raySegs) + timeSec * 0.15F;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            Mth.cos(ra0) * rayRBot, 0.06F, Mth.sin(ra0) * rayRBot,
                            Mth.cos(ra1) * rayRBot, 0.06F, Mth.sin(ra1) * rayRBot,
                            Mth.cos(ra1) * rayRTop, domeApexY - 0.20F, Mth.sin(ra1) * rayRTop,
                            Mth.cos(ra0) * rayRTop, domeApexY - 0.20F, Mth.sin(ra0) * rayRTop,
                            255, 252, 210, aRay);
                }

                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.24F, domeApexY - 0.15F, -0.24F, 0.24F, domeApexY + 1.45F, 0.24F, 245, 220, 95, aTrim);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.12F, domeApexY + 1.45F, -0.12F, 0.12F, domeApexY + 2.35F, 0.12F, 120, 255, 155, aTrim);
                poseStack.popPose();

                VertexConsumer skVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
                poseStack.pushPose();
                poseStack.translate(cx, cy + domeApexY - 1.35F + Mth.sin(timeSec * 2.2F) * 0.15F, cz);
                poseStack.mulPose(Axis.YP.rotationDegrees(timeSec * 45.0F));
                float cloverHalf = 1.15F;
                for (int plane = 0; plane < 2; plane++) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(plane * 90.0F));
                    PoseStack.Pose p = poseStack.last();
                    WindGliderRenderer.emitWingQuad(skVc, p, -cloverHalf, -cloverHalf, 0.0F, 0.0F, 1.0F, cloverHalf, -cloverHalf, 0.0F, 1.0F, 1.0F, cloverHalf, cloverHalf, 0.0F, 1.0F, 0.0F, -cloverHalf, cloverHalf, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                    WindGliderRenderer.emitWingQuad(skVc, p, -cloverHalf, cloverHalf, 0.0F, 0.0F, 0.0F, cloverHalf, cloverHalf, 0.0F, 1.0F, 0.0F, cloverHalf, -cloverHalf, 0.0F, 1.0F, 1.0F, -cloverHalf, -cloverHalf, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                    poseStack.popPose();
                }
                poseStack.popPose();
            } else if (arc.fromEntityId == -8) {
                float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
                float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
                float fade = 1.0F - prog * 0.65F;
                Vec3 sCam = arc.start.subtract(camPos);
                Vec3 eCam = arc.end.subtract(camPos);
                Vec3 chord = eCam.subtract(sCam);
                if (chord.lengthSqr() > 1.0E-4D) {
                    VertexConsumer linkVc = bufferSource.getBuffer(RenderType.lightning());
                    Matrix4f linkMat = poseStack.last().pose();
                    Vec3 n = new Vec3(0.0D, 1.0D, 0.0D);
                    ElementalReactionBadgeRenderer.drawQuadRibbon(linkVc, linkMat, sCam, eCam, n, 0.042D, 75, 245, 115, Math.max(25, Math.round(210 * fade)));
                    ElementalReactionBadgeRenderer.drawQuadRibbon(linkVc, linkMat, sCam, eCam, n, 0.016D, 220, 255, 225, Math.max(35, Math.round(245 * fade)));
                }
                VertexConsumer skVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
                Vec3 midCam = sCam.lerp(eCam, 0.5D);
                for (Vec3 node : new Vec3[]{sCam, midCam, eCam}) {
                    poseStack.pushPose();
                    poseStack.translate(node.x, node.y, node.z);
                    poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                    float h = 0.28F;
                    PoseStack.Pose p = poseStack.last();
                    WindGliderRenderer.emitWingQuad(skVc, p, -h, -h, 0.0F, 0.0F, 1.0F, h, -h, 0.0F, 1.0F, 1.0F, h, h, 0.0F, 1.0F, 0.0F, -h, h, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                    WindGliderRenderer.emitWingQuad(skVc, p, -h, h, 0.0F, 0.0F, 0.0F, h, h, 0.0F, 1.0F, 0.0F, h, -h, 0.0F, 1.0F, 1.0F, -h, -h, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                    poseStack.popPose();
                }
            }
        }
    }
}
