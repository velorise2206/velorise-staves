package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated visual renderer for Xilonen (Peak Patrol Song):
 * - 3D voxel DJ turntable booth (platter rotations, tonearms, VU-meters, spotlights, holographic notes)
 * - 3D Geo roller skates and Source Samples equalizer ring
 * - Xilonen Nightsoul HUD bar
 * - Roller skate stride, wall climbing, kicks N1/N2, and DJ dance/scratch animation
 */
public final class XilonenVisualRenderer {
    private XilonenVisualRenderer() {}

    public static void renderRollerSkatesAndSamples(
            LivingEntity entity,
            EntityModel<?> model,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        if (!(model instanceof HumanoidModel<?> humanoid)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        boolean skating = ClientAuraTracker.isXilonenSkating(entity.getId());
        boolean hasSamples = ClientAuraTracker.hasXilonenSourceSamples(entity.getId());
        if (!skating && !hasSamples) return;

        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float entityScale = entity.getScale();
        float time = entity.tickCount + partialTicks;

        if (skating) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            poseStack.scale(-entityScale, -entityScale, entityScale);
            poseStack.translate(0.0F, -1.501F, 0.0F);

            float wheelSpin = entity.getDeltaMovement().horizontalDistanceSqr() > 4.0E-4D ? (-time * 0.65F) : 0.0F;
            VertexConsumer skateVc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(com.velorise.staves.client.model.XilonenSkatesModel.TEXTURE));

            poseStack.pushPose();
            humanoid.rightLeg.translateAndRotate(poseStack);
            com.velorise.staves.client.model.XilonenSkatesModel.renderRightSkate(poseStack, skateVc, wheelSpin, 0xF000F0);
            poseStack.popPose();

            poseStack.pushPose();
            humanoid.leftLeg.translateAndRotate(poseStack);
            com.velorise.staves.client.model.XilonenSkatesModel.renderLeftSkate(poseStack, skateVc, wheelSpin, 0xF000F0);
            poseStack.popPose();

            poseStack.popPose();
            if (buffer instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(com.velorise.staves.client.model.XilonenSkatesModel.TEXTURE));
            }
        }

        if (hasSamples || skating) {
            ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
            int mask = d != null ? d.xilonenSampleMask : 1;
            if (mask == 0) mask = 1;
            VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.18F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.2F));
            Matrix4f mat = poseStack.last().pose();

            int segs = 24;
            float rIn = 0.72F;
            float rOut = 0.84F;
            for (int i = 0; i < segs; i++) {
                float a0 = (float) (i * 2.0D * Math.PI / segs);
                float a1 = (float) ((i + 1) * 2.0D * Math.PI / segs);
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
                float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                int alpha = (i % 2 == 0) ? 185 : 95;
                ClientAuraRenderer.emitCrystalQuad(vc, mat,
                        c0 * rIn, 0.0F, s0 * rIn, 255, 210, 55, alpha,
                        c1 * rIn, 0.0F, s1 * rIn, 255, 210, 55, alpha,
                        c1 * rOut, 0.0F, s1 * rOut, 75, 238, 224, alpha,
                        c0 * rOut, 0.0F, s0 * rOut, 75, 238, 224, alpha);
            }
            poseStack.popPose();
        }
    }

    public static void renderXilonenNightsoulHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        boolean holdingXilo = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG
                || ArcaneStaffItem.getEffectiveKind(mc.player.getOffhandItem()) == ArcaneStaffItem.Kind.PEAK_PATROL_SONG;
        boolean skating = ClientAuraTracker.isXilonenSkating(mc.player.getId());
        boolean hasSamples = ClientAuraTracker.hasXilonenSourceSamples(mc.player.getId());
        if (!holdingXilo && !skating && !hasSamples) return;

        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(mc.player.getId());
        int pts = (d != null && skating) ? d.xilonenNightsoulPoints : 0;
        if (!skating && !hasSamples) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int barW = 54;
        int barH = 4;
        int bx = sw / 2 - barW / 2;
        int by = sh / 2 + 20;

        if (skating) {
            float frac = Mth.clamp(pts / 90.0F, 0.0F, 1.0F);
            guiGraphics.fill(bx - 1, by - 1, bx + barW + 1, by + barH + 1, 0xAA141018);
            int fillW = Math.round(barW * frac);
            if (fillW > 0) {
                guiGraphics.fill(bx, by, bx + fillW, by + barH, 0xFFFFD644);
                guiGraphics.fill(bx, by, bx + fillW, by + 1, 0xFF55F2E2);
            }
            String ptStr = pts + "/90";
            guiGraphics.drawString(mc.font, ptStr, bx + barW + 4, by - 2, 0xFFFFE066, true);
        }
    }

    public static boolean applySkateStrideAndWallClimb(
            LivingEntity entity,
            HumanoidModel<?> model,
            float ageInTicks,
            int activeAnim
    ) {
        if (!ClientAuraTracker.isXilonenSkating(entity.getId())
                || activeAnim == ClientAuraTracker.ANIM_XILONEN_KICK_1
                || activeAnim == ClientAuraTracker.ANIM_XILONEN_KICK_2
                || activeAnim == ClientAuraTracker.ANIM_XILONEN_BURST) {
            return false;
        }
        if (ClientAuraTracker.isXilonenWallClimbing(entity.getId())) {
            float climb = Mth.sin(ageInTicks * 0.85F);
            model.body.xRot = -0.18F;
            model.rightLeg.xRot = -0.65F + climb * 0.55F;
            model.leftLeg.xRot = -0.65F - climb * 0.55F;
            // Relaxed arms during skate wall climb; legs alone propel her upward
            model.rightArm.xRot = 0.10F;
            model.leftArm.xRot = 0.10F;
            model.rightArm.zRot = 0.14F;
            model.leftArm.zRot = -0.14F;
            return true;
        }
        float flipProg = ClientAuraTracker.getXilonenJumpFlipProgress(entity.getId());
        if (flipProg >= 0.0F && !entity.onGround()) {
            float tuck = Mth.sin(flipProg * (float) Math.PI);
            float stride = Mth.sin(ageInTicks * 0.38F) * (1.0F - tuck);
            model.body.xRot = Mth.lerp(tuck, 0.18F, 0.32F);
            model.body.yRot = stride * 0.14F;
            model.rightLeg.xRot = Mth.lerp(tuck, stride * 0.52F, -0.85F);
            model.leftLeg.xRot = Mth.lerp(tuck, -stride * 0.52F, -0.85F);
            model.rightLeg.zRot = Mth.lerp(tuck, 0.12F, 0.06F);
            model.leftLeg.zRot = Mth.lerp(tuck, -0.12F, -0.06F);
            model.rightArm.xRot = Mth.lerp(tuck, -stride * 0.42F - 0.15F, -1.05F);
            model.leftArm.xRot = Mth.lerp(tuck, stride * 0.42F - 0.15F, -1.05F);
            model.rightArm.zRot = Mth.lerp(tuck, 0.18F, 0.22F);
            model.leftArm.zRot = Mth.lerp(tuck, -0.18F, -0.22F);
            return true;
        }
        double dy = entity.getDeltaMovement().y;
        if (!entity.onGround() && (dy < -0.45D || dy > 0.15D)) {
            model.body.xRot = 0.30F;
            model.rightLeg.xRot = -0.82F;
            model.leftLeg.xRot = -0.82F;
            model.rightArm.xRot = -1.05F;
            model.leftArm.xRot = -1.05F;
            model.rightArm.zRot = 0.22F;
            model.leftArm.zRot = -0.22F;
            return true;
        }
        float stride = Mth.sin(ageInTicks * 0.38F);
        model.body.xRot = 0.18F;
        model.body.yRot = stride * 0.14F;
        model.rightLeg.xRot = stride * 0.52F;
        model.leftLeg.xRot = -stride * 0.52F;
        model.rightLeg.zRot = 0.12F;
        model.leftLeg.zRot = -0.12F;
        model.rightArm.xRot = -stride * 0.42F - 0.15F;
        model.leftArm.xRot = stride * 0.42F - 0.15F;
        model.rightArm.zRot = 0.18F;
        model.leftArm.zRot = -0.18F;
        return false;
    }

    public static boolean applyActionHumanoidAnimation(
            HumanoidModel<?> model,
            int activeAnim,
            float t
    ) {
        if (activeAnim == ClientAuraTracker.ANIM_XILONEN_SKATE_DASH) {
            float dash = Mth.sin(t * (float) Math.PI);
            model.body.xRot = 0.32F * dash;
            model.head.xRot = Mth.lerp(dash, model.head.xRot, 0.5236F);
            model.hat.copyFrom(model.head);
            model.rightLeg.xRot = -0.65F * dash;
            model.leftLeg.xRot = 0.55F * dash;
            model.rightArm.xRot = -0.85F * dash;
            model.leftArm.xRot = 0.45F * dash;
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_XILONEN_KICK_1) {
            float k = Mth.sin(t * (float) Math.PI);
            model.body.yRot = -0.45F * k;
            model.body.xRot = -0.16F * k;
            model.rightLeg.xRot = -1.42F * k;
            model.rightLeg.yRot = -0.35F * k;
            model.rightLeg.zRot = 0.28F * k;
            model.rightArm.xRot = 0.55F * k;
            model.leftArm.xRot = -0.95F * k;
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_XILONEN_KICK_2) {
            float k = Mth.sin(t * (float) Math.PI);
            model.body.yRot = 0.48F * k;
            model.body.xRot = 0.22F * k;
            model.leftLeg.xRot = -1.58F * k;
            model.leftLeg.yRot = 0.32F * k;
            model.leftLeg.zRot = -0.26F * k;
            model.rightArm.xRot = -1.15F * k;
            model.leftArm.xRot = 0.60F * k;
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_XILONEN_BURST) {
            float beat = Mth.abs(Mth.sin(t * (float) Math.PI * 6.0F));
            float bobY = beat * 1.5F;
            float grooveSway = Mth.sin(t * (float) Math.PI * 3.0F);

            model.body.y += bobY;
            model.head.y += bobY;
            model.hat.y += bobY;
            model.rightArm.y += bobY;
            model.leftArm.y += bobY;
            model.rightLeg.y += bobY * 0.4F;
            model.leftLeg.y += bobY * 0.4F;

            model.body.xRot = 0.08F + 0.06F * beat;
            model.body.yRot = 0.10F * grooveSway;
            model.body.zRot = 0.06F * grooveSway;

            model.head.xRot = 0.18F * beat + 0.05F;
            model.head.yRot = 0.08F * grooveSway;
            model.head.zRot = 0.05F * grooveSway;
            model.hat.copyFrom(model.head);

            float scratch = Mth.sin(t * (float) Math.PI * 14.0F) * 0.16F;
            model.leftArm.xRot = -0.76F + scratch;
            model.leftArm.yRot = 0.32F + scratch * 0.6F;
            model.leftArm.zRot = 0.16F + scratch * 0.3F;

            float pump = Mth.sin(t * (float) Math.PI * 6.0F) * 0.16F;
            model.rightArm.xRot = -2.75F + pump;
            model.rightArm.yRot = -0.22F;
            model.rightArm.zRot = 0.28F + pump * 0.4F;

            model.rightLeg.xRot = 0.14F * beat;
            model.leftLeg.xRot = 0.14F * beat;
            model.rightLeg.zRot = 0.06F;
            model.leftLeg.zRot = -0.06F;
            return true;
        }
        return false;
    }

    public static boolean applyFirstPersonTransform(
            float dir,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (data == null) return false;
        if (activeAnim == ClientAuraTracker.ANIM_XILONEN_BURST) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = Mth.sin(t * (float) Math.PI);
            float scratch = Mth.sin(t * (float) Math.PI * 14.0F) * 0.08F;
            poseStack.translate(-dir * (0.20F * w + scratch), -0.22F * w, -0.18F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(24.0F * w + scratch * 25.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * (18.0F * w + scratch * 15.0F)));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_XILONEN_KICK_1
                || activeAnim == ClientAuraTracker.ANIM_XILONEN_KICK_2
                || activeAnim == ClientAuraTracker.ANIM_XILONEN_SKATE_DASH) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = Mth.sin(t * (float) Math.PI);
            poseStack.translate(-dir * 0.16F * w, 0.22F * w, -0.20F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(28.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 24.0F * w));
            return true;
        }
        return false;
    }

    public static void renderXilonenDjBooth(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        long now = System.currentTimeMillis();
        int activeAnim = (d != null && now < d.staffAnimUntil) ? d.staffAnimType : 0;
        if (activeAnim != ClientAuraTracker.ANIM_XILONEN_BURST) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        if (isFirstPersonSelf) return;

        float pt = (mc.level != null) ? mc.getTimer().getGameTimeDeltaPartialTick(true) : 0.0F;
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        float totalMs = Math.max(1.0F, (float) (d.staffAnimUntil - d.staffAnimStartTime));
        float t = Mth.clamp((now - d.staffAnimStartTime) / totalMs, 0.0F, 1.0F);

        float alphaScale = (t < 0.08F) ? (t / 0.08F) : (t > 0.90F ? (1.0F - t) / 0.10F : 1.0F);
        int alphaBase = Math.round(255 * alphaScale);
        if (alphaBase <= 8) return;

        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

        renderXilonenDjBoothGeometry(vc, poseStack.last().pose(), time, t, alphaBase);

        poseStack.popPose();
    }

    public static void renderFirstPersonXilonenDjBooth(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(mc.player.getId());
        if (d == null || now >= d.staffAnimUntil || d.staffAnimType != ClientAuraTracker.ANIM_XILONEN_BURST) return;

        float totalMs = Math.max(1.0F, (float) (d.staffAnimUntil - d.staffAnimStartTime));
        float t = Mth.clamp((now - d.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
        float alphaScale = (t < 0.08F) ? (t / 0.08F) : (t > 0.90F ? (1.0F - t) / 0.10F : 1.0F);
        int alphaBase = Math.round(255 * alphaScale);
        if (alphaBase <= 8) return;

        float pt = (mc.level != null) ? mc.getTimer().getGameTimeDeltaPartialTick(true) : 0.0F;
        float time = mc.player.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, mc.player.yBodyRotO, mc.player.yBodyRot);

        float cx = (float) (mc.player.getX() - camPos.x);
        float cy = (float) (mc.player.getY() - camPos.y);
        float cz = (float) (mc.player.getZ() - camPos.z);

        VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        poseStack.pushPose();
        poseStack.translate(cx, cy, cz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

        renderXilonenDjBoothGeometry(vc, poseStack.last().pose(), time, t, alphaBase);

        poseStack.popPose();
    }

    private static void renderXilonenDjBoothGeometry(VertexConsumer vc, Matrix4f mat, float time, float t, int alphaBase) {
        float hoverBob = Mth.sin(time * 0.28F) * 0.025F;
        float tableY = 0.78F + hoverBob;
        float tableZ = 0.58F;

        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.62F, tableY, tableZ - 0.18F, 0.62F, tableY + 0.075F, tableZ + 0.18F, 34, 30, 38, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.72F, tableY + 0.01F, tableZ - 0.14F, -0.62F, tableY + 0.065F, tableZ + 0.14F, 28, 24, 32, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat,  0.62F, tableY + 0.01F, tableZ - 0.14F,  0.72F, tableY + 0.065F, tableZ + 0.14F, 28, 24, 32, alphaBase);

        int gR = 255, gG = 196, gB = 45;
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.72F, tableY - 0.012F, tableZ + 0.18F,  0.72F, tableY + 0.082F, tableZ + 0.205F, gR, gG, gB, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.72F, tableY - 0.012F, tableZ - 0.205F, 0.72F, tableY + 0.082F, tableZ - 0.18F,  gR, gG, gB, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.74F, tableY - 0.012F, tableZ - 0.15F, -0.72F, tableY + 0.082F, tableZ + 0.15F, gR, gG, gB, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat,  0.72F, tableY - 0.012F, tableZ - 0.15F,  0.74F, tableY + 0.082F, tableZ + 0.15F, gR, gG, gB, alphaBase);

        int cR = 40, cG = 245, cB = 230;
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.68F, tableY + 0.028F, tableZ + 0.206F, 0.68F, tableY + 0.048F, tableZ + 0.212F, cR, cG, cB, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.742F, tableY + 0.028F, tableZ - 0.12F, -0.738F, tableY + 0.048F, tableZ + 0.12F, cR, cG, cB, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat,  0.738F, tableY + 0.028F, tableZ - 0.12F,  0.742F, tableY + 0.048F, tableZ + 0.12F, cR, cG, cB, alphaBase);

        float leftPlatterX = -0.38F;
        float rightPlatterX = 0.38F;
        float platterY = tableY + 0.076F;
        float platterRadius = 0.165F;
        int numSegs = 16;

        float leftScratchAngle = (float) Math.toRadians(Mth.sin(t * (float) Math.PI * 14.0F) * 65.0F + (time * 16.0F));
        float rightSpinAngle = (float) Math.toRadians(time * 24.0F);

        renderTurntablePlatter(vc, mat, leftPlatterX, platterY, tableZ, platterRadius, leftScratchAngle, numSegs, alphaBase, true);
        renderTurntablePlatter(vc, mat, rightPlatterX, platterY, tableZ, platterRadius, rightSpinAngle, numSegs, alphaBase, false);

        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.15F, tableY + 0.076F, tableZ - 0.15F, 0.15F, tableY + 0.081F, tableZ + 0.15F, 22, 20, 26, alphaBase);

        float[] eqXs = {-0.10F, -0.06F, -0.02F, 0.02F, 0.06F, 0.10F};
        for (int ch = 0; ch < eqXs.length; ch++) {
            float eqX = eqXs[ch];
            float freq = Mth.abs(Mth.sin(t * (float) Math.PI * (8.0F + ch * 1.8F) + ch * 1.1F));
            int segments = Math.min(6, (int) (freq * 7.5F));
            for (int seg = 0; seg < 6; seg++) {
                float segZ0 = tableZ - 0.08F + seg * 0.026F;
                float segZ1 = segZ0 + 0.020F;
                boolean lit = seg <= segments;
                int r, g, b, a;
                if (lit) {
                    if (seg < 3) {
                        r = 45; g = 240; b = 130; a = alphaBase;
                    } else if (seg < 5) {
                        r = 255; g = 220; b = 40; a = alphaBase;
                    } else {
                        r = 255; g = 60; b = 35; a = alphaBase;
                    }
                } else {
                    r = 35; g = 40; b = 42; a = Math.min(alphaBase, 80);
                }
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, eqX - 0.012F, tableY + 0.082F, segZ0, eqX + 0.012F, tableY + 0.088F, segZ1, r, g, b, a);
            }
        }

        float faderX = Mth.sin(t * (float) Math.PI * 7.0F) * 0.06F;
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, faderX - 0.015F, tableY + 0.082F, tableZ + 0.10F, faderX + 0.015F, tableY + 0.096F, tableZ + 0.125F, 245, 245, 250, alphaBase);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.09F, tableY + 0.081F, tableZ + 0.11F, 0.09F, tableY + 0.083F, tableZ + 0.115F, 12, 10, 14, alphaBase);

        float flareAlpha = 0.35F + 0.15F * Mth.sin(time * 0.45F);
        int coneAlpha = Math.round(alphaBase * flareAlpha);
        renderHoverSpotlight(vc, mat, leftPlatterX, tableY, tableZ, 0.11F, 0.28F, 0.72F, coneAlpha);
        renderHoverSpotlight(vc, mat, rightPlatterX, tableY, tableZ, 0.11F, 0.28F, 0.72F, coneAlpha);

        renderDjHologramNotes(vc, mat, tableY, tableZ, time, t, alphaBase);
    }

    private static void renderTurntablePlatter(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float radius, float rotAngle,
            int segs, int alpha, boolean isScratching
    ) {
        float rOut = radius + 0.018F;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2.0D * Math.PI / segs);
            float a1 = (float) ((i + 1) * 2.0D * Math.PI / segs);
            WindGliderRenderer.emitDoubleQuad(vc, mat,
                    cx + Mth.cos(a0) * radius, cy + 0.002F, cz + Mth.sin(a0) * radius,
                    cx + Mth.cos(a1) * radius, cy + 0.002F, cz + Mth.sin(a1) * radius,
                    cx + Mth.cos(a1) * rOut, cy + 0.002F, cz + Mth.sin(a1) * rOut,
                    cx + Mth.cos(a0) * rOut, cy + 0.002F, cz + Mth.sin(a0) * rOut,
                    255, 198, 48, alpha);
        }

        float rLabel = radius * 0.36F;
        for (int i = 0; i < segs; i++) {
            float a0 = rotAngle + (float) (i * 2.0D * Math.PI / segs);
            float a1 = rotAngle + (float) ((i + 1) * 2.0D * Math.PI / segs);
            int vinylTone = (i % 2 == 0) ? 22 : 16;
            WindGliderRenderer.emitDoubleQuad(vc, mat,
                    cx + Mth.cos(a0) * rLabel, cy + 0.004F, cz + Mth.sin(a0) * rLabel,
                    cx + Mth.cos(a1) * rLabel, cy + 0.004F, cz + Mth.sin(a1) * rLabel,
                    cx + Mth.cos(a1) * radius, cy + 0.004F, cz + Mth.sin(a1) * radius,
                    cx + Mth.cos(a0) * radius, cy + 0.004F, cz + Mth.sin(a0) * radius,
                    vinylTone, vinylTone, vinylTone + 2, alpha);
        }

        for (int i = 0; i < segs; i++) {
            float a0 = rotAngle + (float) (i * 2.0D * Math.PI / segs);
            float a1 = rotAngle + (float) ((i + 1) * 2.0D * Math.PI / segs);
            WindGliderRenderer.emitDoubleQuad(vc, mat,
                    cx, cy + 0.006F, cz,
                    cx + Mth.cos(a0) * rLabel, cy + 0.006F, cz + Mth.sin(a0) * rLabel,
                    cx + Mth.cos(a1) * rLabel, cy + 0.006F, cz + Mth.sin(a1) * rLabel,
                    cx, cy + 0.006F, cz,
                    255, 215, 60, alpha);
        }
        WindGliderRenderer.renderCloverCrossXZ(vc, mat, cx, cy + 0.008F, cz, 0.025F, 50, 245, 235, alpha);

        float armPivotX = cx + (isScratching ? -0.21F : 0.21F);
        float armPivotZ = cz - 0.12F;
        float armTipX = cx + (isScratching ? -0.06F : 0.06F);
        float armTipZ = cz + 0.04F;
        WindGliderRenderer.emitDoubleQuad(vc, mat,
                armPivotX - 0.008F, cy + 0.016F, armPivotZ - 0.008F,
                armPivotX + 0.008F, cy + 0.016F, armPivotZ + 0.008F,
                armTipX + 0.008F, cy + 0.009F, armTipZ + 0.008F,
                armTipX - 0.008F, cy + 0.009F, armTipZ - 0.008F,
                230, 200, 75, alpha);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, armPivotX - 0.018F, cy + 0.004F, armPivotZ - 0.018F, armPivotX + 0.018F, cy + 0.022F, armPivotZ + 0.018F, 255, 195, 45, alpha);
    }

    private static void renderHoverSpotlight(
            VertexConsumer vc, Matrix4f mat,
            float cx, float tableY, float cz,
            float topRadius, float bottomRadius, float coneHeight,
            int alpha
    ) {
        if (alpha <= 5) return;
        float yTop = tableY - 0.015F;
        float yBottom = Math.max(0.04F, tableY - coneHeight);
        int segs = 12;

        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2.0D * Math.PI / segs);
            float a1 = (float) ((i + 1) * 2.0D * Math.PI / segs);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            WindGliderRenderer.emitDoubleQuad(vc, mat,
                    cx + c0 * topRadius, yTop, cz + s0 * topRadius,
                    cx + c1 * topRadius, yTop, cz + s1 * topRadius,
                    cx + c1 * bottomRadius, yBottom, cz + s1 * bottomRadius,
                    cx + c0 * bottomRadius, yBottom, cz + s0 * bottomRadius,
                    255, 210, 65, alpha);
        }

        WindGliderRenderer.renderCloverCrossXZ(vc, mat, cx, yTop, cz, 0.10F, 255, 240, 130, Math.min(255, alpha * 2));
    }

    private static void renderDjHologramNotes(
            VertexConsumer vc, Matrix4f mat,
            float tableY, float tableZ,
            float time, float t, int alphaBase
    ) {
        float[][] noteOffsets = {
                {-0.36F, 0.22F, -0.04F, 0.12F, 40, 245, 230},
                {-0.22F, 0.36F,  0.02F, 0.10F, 255, 215, 60},
                { 0.24F, 0.28F,  0.03F, 0.11F, 255, 215, 60},
                { 0.38F, 0.42F, -0.02F, 0.13F, 40, 245, 230},
                { 0.00F, 0.32F, -0.05F, 0.09F, 255, 160, 40}
        };

        for (int i = 0; i < noteOffsets.length; i++) {
            float[] n = noteOffsets[i];
            float bob = Mth.sin(time * 0.35F + i * 1.3F) * 0.04F;
            float nx = n[0];
            float ny = tableY + 0.10F + n[1] + bob;
            float nz = tableZ + n[2];
            float noteSize = n[3];
            int r = (int) n[4];
            int g = (int) n[5];
            int b = (int) n[6];
            int a = Math.round(alphaBase * 0.85F);
            WindGliderRenderer.renderCloverCross2D(vc, mat, nx, ny, nz, noteSize, r, g, b, a);
        }
    }
}
