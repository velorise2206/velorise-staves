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
            float sway = Mth.sin(ageInTicks * 0.10F);
            float sitDropY = 3.2F;
            float swayZ = -1.65F * sway;
            model.body.y += sitDropY;
            model.head.y += sitDropY;
            model.hat.y += sitDropY;
            model.rightArm.y += sitDropY;
            model.leftArm.y += sitDropY;
            model.rightLeg.y += sitDropY;
            model.leftLeg.y += sitDropY;

            model.body.z += swayZ;
            model.head.z += swayZ;
            model.hat.z += swayZ;
            model.rightArm.z += swayZ;
            model.leftArm.z += swayZ;
            model.rightLeg.z += swayZ - 1.8F;
            model.leftLeg.z += swayZ - 1.8F;

            model.body.xRot = 0.06F * sway;
            model.rightLeg.xRot = -1.28F + 0.16F * sway;
            model.leftLeg.xRot = -1.28F - 0.16F * sway;
            model.rightLeg.yRot = 0.08F;
            model.leftLeg.yRot = -0.08F;
            model.rightArm.xRot = -0.86F + 0.05F * sway;
            model.leftArm.xRot = -0.86F + 0.05F * sway;
            model.rightArm.zRot = 0.18F;
            model.leftArm.zRot = -0.18F;
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
                // Step 3 (Penultimate): Light hop bounce & forward cast (NO body spin)
                model.body.yRot = -0.15F * hop;
                model.rightLeg.xRot = -0.45F * hop;
                model.leftLeg.xRot = 0.35F * hop;
                model.rightArm.xRot = -1.45F * hop;
                model.leftArm.xRot = -1.45F * hop;
                model.rightArm.zRot = 0.65F * hop;
                model.leftArm.zRot = -0.65F * hop;
            } else {
                // Step 4 (Finisher): Light hop bounce & finger snap finish (NO body spin)
                model.body.yRot = 0.15F * hop;
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
            float sweep = Mth.sin(t * (float) Math.PI);
            float hopY = -2.5F * sweep;
            model.body.y += hopY;
            model.head.y += hopY;
            model.hat.y += hopY;

            model.body.yRot = 0.0F;
            model.head.yRot = 0.0F;
            model.hat.copyFrom(model.head);

            model.rightArm.y = 2.0F + hopY;
            model.rightArm.xRot = -1.25F * sweep;
            model.rightArm.yRot = -0.20F * sweep;
            model.rightArm.zRot = 0.45F * sweep;

            model.leftArm.y = 2.0F + hopY;
            model.leftArm.xRot = -1.25F * sweep;
            model.leftArm.yRot = 0.20F * sweep;
            model.leftArm.zRot = -0.45F * sweep;

            model.rightLeg.y = 12.0F + hopY;
            model.rightLeg.xRot = -0.35F * sweep;

            model.leftLeg.y = 12.0F + hopY;
            model.leftLeg.xRot = 0.25F * sweep;
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
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_BURST && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float raise = WindGliderRenderer.smoothStep01(Math.min(1.0F, t * 3.5F));
            float floatBob = Mth.sin(t * (float) Math.PI * 3.0F) * 0.025F;
            poseStack.translate(0.0F, -0.18F * raise + floatBob, -0.38F * raise);
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F * raise));
            return true;
        }
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
            ResourceLocation ropeTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_swing_rope.png");
            ResourceLocation seatTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_swing_seat.png");
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            renderNahidaIdleSwing3D(poseStack, buffer, time, ropeTex, seatTex);
            poseStack.popPose();
            vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        }

        if (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4) {
            ResourceLocation keycapTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_keycap_tile.png");
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            renderHopscotchKeycap3D(poseStack, buffer, keycapTex, 0.58F, 0.18F, 1.0F);
            poseStack.popPose();
            vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
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

        if (shrine && d != null) {
            // Spawn world-anchored footstep Dendro flower-grass clumps (Image 1) whenever walking inside the Shrine of Maya
            double curX = entity.getX();
            double curZ = entity.getZ();
            double dx = curX - d.lastShrineStepX;
            double dz = curZ - d.lastShrineStepZ;
            boolean movedEnough = Double.isNaN(d.lastShrineStepX) || (dx * dx + dz * dz) >= 0.42D;
            if (movedEnough && (now - d.lastShrineStepMs) >= 180L && (entity.onGround() || Math.abs(entity.getDeltaMovement().y) < 0.25D)) {
                d.lastShrineStepX = curX;
                d.lastShrineStepZ = curZ;
                d.lastShrineStepMs = now;
                int varIdx = d.shrineStepVariant++;
                float sideSign = (varIdx & 1) == 0 ? 0.20F : -0.20F;
                float yawRad = (float) Math.toRadians(bodyYaw);
                double rightX = Math.cos(yawRad) * sideSign;
                double rightZ = Math.sin(yawRad) * sideSign;
                double fwdX = -Math.sin(yawRad);
                double fwdZ = Math.cos(yawRad);
                Vec3 footStart = new Vec3(curX + rightX, entity.getY(), curZ + rightZ);
                Vec3 footEnd = footStart.add(fwdX, 0.0D, fwdZ);
                ClientAuraTracker.addArc(-8, footStart, -8, footEnd, 36, 0x4E420000 | (varIdx & 7));
            }
            if (!isFirstPersonSelf) {
                ResourceLocation stepGrassTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_shrine_step_grass.png");
                VertexConsumer grassVc = buffer.getBuffer(RenderType.entityCutoutNoCull(stepGrassTex));
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
                poseStack.translate(0.18F, 0.02F, -0.12F);
                renderUnfurlingTexturedRibbon(grassVc, poseStack, 0.26F, 0.46F, 1.0F, time * 0.05F, 0.04F, 0.5F, 8);
                poseStack.popPose();
            }
        }
    }



    private static void renderNahidaIdleSwing3D(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float time,
            ResourceLocation ropeTex,
            ResourceLocation seatTex
    ) {
        // Pendulum swing around the floating top 4-petal flower anchors (y = 2.68F, NO horizontal top bar!)
        float swingDeg = Mth.sin(time * 0.10F) * 4.5F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.68F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(swingDeg));
        poseStack.translate(0.0F, -2.68F, 0.0F);

        // 1. Left & Right Braided Vine Ropes with attached leaves, white-rimmed 4-petal clovers & top flower anchor
        VertexConsumer ropeVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ropeTex));
        float ropeHalfW = 0.24F;
        float ropeBotY = 0.44F;
        float ropeTopY = 2.74F;
        for (float rx : new float[]{-0.42F, 0.42F}) {
            poseStack.pushPose();
            poseStack.translate(rx, 0.0F, 0.0F);
            for (int plane = 0; plane < 2; plane++) {
                poseStack.pushPose();
                if (plane == 1) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(75.0F));
                }
                PoseStack.Pose p = poseStack.last();
                // Mirror UV horizontally on right rope so flower clusters frame Nahida symmetrically
                float u0 = (rx < 0.0F) ? 0.0F : 1.0F;
                float u1 = (rx < 0.0F) ? 1.0F : 0.0F;
                WindGliderRenderer.emitWingQuad(ropeVc, p,
                        -ropeHalfW, ropeBotY, 0.0F, u0, 1.0F,
                         ropeHalfW, ropeBotY, 0.0F, u1, 1.0F,
                         ropeHalfW, ropeTopY, 0.0F, u1, 0.0F,
                        -ropeHalfW, ropeTopY, 0.0F, u0, 0.0F,
                        0.0F, 0.0F, 1.0F);
                WindGliderRenderer.emitWingQuad(ropeVc, p,
                        -ropeHalfW, ropeTopY, 0.0F, u0, 0.0F,
                         ropeHalfW, ropeTopY, 0.0F, u1, 0.0F,
                         ropeHalfW, ropeBotY, 0.0F, u1, 1.0F,
                        -ropeHalfW, ropeBotY, 0.0F, u0, 1.0F,
                        0.0F, 0.0F, -1.0F);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        // 2. Ornate Translucent Seat Top & Bottom Surfaces (nahida_swing_seat.png)
        float sx = 0.49F;
        float sz = 0.19F;
        float syBot = 0.41F;
        float syTop = 0.51F;
        VertexConsumer seatVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(seatTex));
        for (float yFace : new float[]{syTop, syBot}) {
            poseStack.pushPose();
            poseStack.translate(0.0F, yFace, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            PoseStack.Pose p = poseStack.last();
            WindGliderRenderer.emitWingQuad(seatVc, p, -sx, -sz, 0.0F, 0.0F, 1.0F, sx, -sz, 0.0F, 1.0F, 1.0F, sx, sz, 0.0F, 1.0F, 0.0F, -sx, sz, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            WindGliderRenderer.emitWingQuad(seatVc, p, -sx, sz, 0.0F, 0.0F, 0.0F, sx, sz, 0.0F, 1.0F, 0.0F, sx, -sz, 0.0F, 1.0F, 1.0F, -sx, -sz, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
            poseStack.popPose();
        }

        // 3. 3D Hollow Rectangular Wireframe Prism Cage (glowing neon lime-yellow edges & vertical struts matching Images 2, 3, 4)
        VertexConsumer wireVc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        Matrix4f mat = poseStack.last().pose();
        float tBar = 0.016F;
        for (float yR : new float[]{syBot, syTop}) {
            // Front & Back horizontal bars
            WindGliderRenderer.emitDoubleQuad(wireVc, mat, -sx, yR - tBar, -sz, sx, yR - tBar, -sz, sx, yR + tBar, -sz, -sx, yR + tBar, -sz, 215, 255, 155, 240);
            WindGliderRenderer.emitDoubleQuad(wireVc, mat, -sx, yR - tBar,  sz, sx, yR - tBar,  sz, sx, yR + tBar,  sz, -sx, yR + tBar,  sz, 215, 255, 155, 240);
            // Left & Right horizontal bars
            WindGliderRenderer.emitDoubleQuad(wireVc, mat, -sx, yR - tBar, -sz, -sx, yR - tBar, sz, -sx, yR + tBar, sz, -sx, yR + tBar, -sz, 195, 255, 140, 235);
            WindGliderRenderer.emitDoubleQuad(wireVc, mat,  sx, yR - tBar, -sz,  sx, yR - tBar, sz,  sx, yR + tBar, sz,  sx, yR + tBar, -sz, 195, 255, 140, 235);
        }
        // Vertical struts connecting top and bottom rims along front & back (Images 3 & 4)
        for (float vx : new float[]{-sx, -0.24F, 0.0F, 0.24F, sx}) {
            for (float vz : new float[]{-sz, sz}) {
                WindGliderRenderer.emitDoubleQuad(wireVc, mat, vx - tBar, syBot, vz, vx + tBar, syBot, vz, vx + tBar, syTop, vz, vx - tBar, syTop, vz, 210, 255, 150, 235);
            }
        }
        poseStack.popPose();
    }

    private static void renderNahidaShrineFootstepGrass(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 camPos,
            ClientAuraTracker.ActiveArc arc,
            long now
    ) {
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float grow = WindGliderRenderer.smoothStep01(Mth.clamp(prog / 0.22F, 0.0F, 1.0F));
        float fade = prog > 0.65F ? Mth.clamp((1.0F - prog) / 0.35F, 0.0F, 1.0F) : 1.0F;
        float timeSec = (now - arc.startTime) * 0.001F;
        int variant = arc.color & 7;

        Vec3 dir = arc.end.subtract(arc.start);
        Vec3 horiz = new Vec3(dir.x, 0.0D, dir.z);
        if (horiz.lengthSqr() < 1.0E-4D) horiz = new Vec3(0.0D, 0.0D, 1.0D);
        else horiz = horiz.normalize();
        float yawDeg = (float) Math.toDegrees(Math.atan2(-horiz.x, horiz.z)) + (variant - 3) * 18.0F;

        ResourceLocation stepGrassTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_shrine_step_grass.png");
        VertexConsumer grassVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(stepGrassTex));

        float sx = (float) (arc.start.x - camPos.x);
        float sy = (float) (arc.start.y - camPos.y);
        float sz = (float) (arc.start.z - camPos.z);

        poseStack.pushPose();
        poseStack.translate(sx, sy + 0.02F, sz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawDeg));

        // Primary lush 4-petal flower-grass clump sprouting at the footstep
        float mainW = 0.36F * fade;
        float mainH = 0.68F * fade;
        renderUnfurlingTexturedRibbon(grassVc, poseStack, mainW, mainH, grow, timeSec, 0.055F, variant * 0.9F, 8);

        // Secondary smaller companion sprout slightly offset for natural clustering (as seen in Image 1)
        poseStack.pushPose();
        poseStack.translate((variant % 2 == 0) ? 0.22F : -0.22F, 0.0F, -0.16F);
        poseStack.mulPose(Axis.YP.rotationDegrees(38.0F));
        renderUnfurlingTexturedRibbon(grassVc, poseStack, 0.24F * fade, 0.46F * fade, grow, timeSec, 0.045F, variant * 1.5F + 1.0F, 8);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderHopscotchKeycap3D(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation keycapTex,
            float topHalf,
            float height,
            float alphaScale
    ) {
        float botHalf = topHalf * 1.24F;
        int sideAlpha = Math.max(18, Math.round(95.0F * alphaScale));
        VertexConsumer sideVc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        Matrix4f mat = poseStack.last().pose();
        // 4 beveled translucent emerald keycap side walls (frustum skirt)
        WindGliderRenderer.emitDoubleQuad(sideVc, mat, -botHalf, 0.02F, -botHalf,  botHalf, 0.02F, -botHalf,  topHalf, height, -topHalf, -topHalf, height, -topHalf, 52, 210, 88, sideAlpha);
        WindGliderRenderer.emitDoubleQuad(sideVc, mat, -botHalf, 0.02F,  botHalf,  botHalf, 0.02F,  botHalf,  topHalf, height,  topHalf, -topHalf, height,  topHalf, 52, 210, 88, sideAlpha);
        WindGliderRenderer.emitDoubleQuad(sideVc, mat, -botHalf, 0.02F, -botHalf, -botHalf, 0.02F,  botHalf, -topHalf, height,  topHalf, -topHalf, height, -topHalf, 46, 195, 80, sideAlpha);
        WindGliderRenderer.emitDoubleQuad(sideVc, mat,  botHalf, 0.02F, -botHalf,  botHalf, 0.02F,  botHalf,  topHalf, height,  topHalf,  topHalf, height, -topHalf, 46, 195, 80, sideAlpha);

        // Top pixel-art keycap surface with 4-petal knotwork clover
        VertexConsumer topVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(keycapTex));
        poseStack.pushPose();
        poseStack.translate(0.0F, height + 0.005F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        PoseStack.Pose p = poseStack.last();
        WindGliderRenderer.emitWingQuad(topVc, p, -topHalf, -topHalf, 0.0F, 0.0F, 1.0F, topHalf, -topHalf, 0.0F, 1.0F, 1.0F, topHalf, topHalf, 0.0F, 1.0F, 0.0F, -topHalf, topHalf, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        WindGliderRenderer.emitWingQuad(topVc, p, -topHalf, topHalf, 0.0F, 0.0F, 0.0F, topHalf, topHalf, 0.0F, 1.0F, 0.0F, topHalf, -topHalf, 0.0F, 1.0F, 1.0F, -topHalf, -topHalf, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
        poseStack.popPose();
    }

    private static void renderUnfurlingTexturedRibbon(
            VertexConsumer vc,
            PoseStack poseStack,
            float halfW,
            float fullH,
            float growProg,
            float timeSec,
            float curveAmp,
            float phase,
            int slices
    ) {
        float maxH = fullH * growProg;
        if (maxH <= 0.02F) return;
        for (int plane = 0; plane < 2; plane++) {
            poseStack.pushPose();
            if (plane == 1) {
                poseStack.mulPose(Axis.YP.rotationDegrees(68.0F));
            }
            PoseStack.Pose p = poseStack.last();
            for (int s = 0; s < slices; s++) {
                float f0 = s / (float) slices;
                float f1 = (s + 1) / (float) slices;
                float y0 = f0 * maxH;
                float y1 = f1 * maxH;
                float v0 = 1.0F - f0 * growProg;
                float v1 = 1.0F - f1 * growProg;
                float sway0 = Mth.sin(f0 * (float) Math.PI * 1.25F - timeSec * 5.2F + phase) * curveAmp * f0;
                float sway1 = Mth.sin(f1 * (float) Math.PI * 1.25F - timeSec * 5.2F + phase) * curveAmp * f1;
                WindGliderRenderer.emitWingQuad(vc, p,
                        -halfW + sway0, y0, 0.0F, 0.0F, v0,
                         halfW + sway0, y0, 0.0F, 1.0F, v0,
                         halfW + sway1, y1, 0.0F, 1.0F, v1,
                        -halfW + sway1, y1, 0.0F, 0.0F, v1,
                        0.0F, 0.0F, 1.0F);
                WindGliderRenderer.emitWingQuad(vc, p,
                        -halfW + sway1, y1, 0.0F, 0.0F, v1,
                         halfW + sway1, y1, 0.0F, 1.0F, v1,
                         halfW + sway0, y0, 0.0F, 1.0F, v0,
                        -halfW + sway0, y0, 0.0F, 0.0F, v0,
                        0.0F, 0.0F, -1.0F);
            }
            poseStack.popPose();
        }
    }

    private static void renderGroundStarSeal(
            VertexConsumer starVc,
            PoseStack poseStack,
            float x,
            float y,
            float z,
            float halfSize,
            float rotDeg
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        PoseStack.Pose p = poseStack.last();
        WindGliderRenderer.emitWingQuad(starVc, p, -halfSize, -halfSize, 0.0F, 0.0F, 1.0F, halfSize, -halfSize, 0.0F, 1.0F, 1.0F, halfSize, halfSize, 0.0F, 1.0F, 0.0F, -halfSize, halfSize, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        WindGliderRenderer.emitWingQuad(starVc, p, -halfSize, halfSize, 0.0F, 0.0F, 0.0F, halfSize, halfSize, 0.0F, 1.0F, 0.0F, halfSize, -halfSize, 0.0F, 1.0F, 1.0F, -halfSize, -halfSize, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
        poseStack.popPose();
    }

    private static void renderNahidaNormalAttackWorldVfx(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 camPos,
            ClientAuraTracker.ActiveArc arc,
            long now,
            ResourceLocation skandhaTex
    ) {
        int step = Math.max(1, Math.min(4, arc.color & 0xFF));
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float grow = WindGliderRenderer.smoothStep01(Mth.clamp(prog / 0.34F, 0.0F, 1.0F));
        float fade = prog > 0.72F ? Mth.clamp((1.0F - prog) / 0.28F, 0.0F, 1.0F) : 1.0F;
        float timeSec = (now - arc.startTime) * 0.001F;

        Vec3 dir = arc.end.subtract(arc.start);
        Vec3 horiz = new Vec3(dir.x, 0.0D, dir.z);
        if (horiz.lengthSqr() < 1.0E-4D) horiz = new Vec3(0.0D, 0.0D, 1.0D);
        else horiz = horiz.normalize();
        float yawDeg = (float) Math.toDegrees(Math.atan2(-horiz.x, horiz.z));

        ResourceLocation keycapTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_keycap_tile.png");
        ResourceLocation starTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_ground_star.png");
        ResourceLocation vineTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_vine_flower.png");
        ResourceLocation heartTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/nahida_heart_vine.png");

        float sx = (float) (arc.start.x - camPos.x);
        float sy = (float) (arc.start.y - camPos.y);
        float sz = (float) (arc.start.z - camPos.z);

        // 1. Anchored 3D Hopscotch Keycap Tile at hop origin (arc.start)
        poseStack.pushPose();
        poseStack.translate(sx, sy, sz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawDeg));
        float keyBounce = 0.14F + 0.08F * Mth.sin(prog * (float) Math.PI);
        renderHopscotchKeycap3D(poseStack, bufferSource, keycapTex, 0.62F * fade, keyBounce * fade, fade);

        // 2. Sprouting Curved Pixel-Art Vine-Flowers & Step 4 Heart of Vines Arch along attack path
        if (step <= 3) {
            float[][] stemLayout = switch (step) {
                case 1 -> new float[][]{ {0.0F, 1.85F, 1.55F, 0.0F} };
                case 2 -> new float[][]{ {-0.55F, 1.70F, 1.40F, 0.0F}, {0.52F, 2.35F, 1.75F, 1.1F} };
                default -> new float[][]{ {-0.78F, 2.05F, 1.48F, 0.0F}, {0.0F, 2.75F, 1.92F, 0.8F}, {0.78F, 2.15F, 1.56F, 1.6F} };
            };

            VertexConsumer starVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(starTex));
            for (float[] st : stemLayout) {
                renderGroundStarSeal(starVc, poseStack, st[0], 0.03F, st[1], (0.38F + 0.24F * grow) * fade, timeSec * 30.0F + st[3] * 25.0F);
            }

            VertexConsumer vineVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(vineTex));
            for (float[] st : stemLayout) {
                poseStack.pushPose();
                poseStack.translate(st[0], 0.03F, st[1]);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(timeSec * 3.0F + st[3]) * 12.0F));
                renderUnfurlingTexturedRibbon(vineVc, poseStack, 0.52F * fade, st[2] * fade, grow, timeSec, 0.11F, st[3], 10);
                poseStack.popPose();
            }

            // Floating pixel-art 4-petal blossoms drifting up around the stems
            VertexConsumer petalVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
            for (int pIdx = 0; pIdx < step + 2; pIdx++) {
                float ang = pIdx * 2.1F + timeSec * 2.4F;
                float pr = 0.45F + (pIdx % 2) * 0.42F;
                float px = Mth.cos(ang) * pr;
                float pz = 1.45F + pIdx * 0.45F + Mth.sin(ang) * 0.25F;
                float py = 0.18F + prog * (0.65F + (pIdx % 3) * 0.22F);
                float pHalf = (0.14F + (pIdx % 2) * 0.05F) * fade;
                renderGroundStarSeal(petalVc, poseStack, px, py, pz, pHalf, timeSec * 85.0F + pIdx * 45.0F);
            }
        } else {
            // Step 4 Finisher: Grand Twin-Scroll Heart of Vines Arch + 2 Side Vine-Flowers + Ground Blossom Scatter
            float burstZ = 3.25F;
            VertexConsumer starVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(starTex));
            renderGroundStarSeal(starVc, poseStack, 0.0F, 0.035F, burstZ, (0.95F + 0.45F * grow) * fade, timeSec * 22.0F);
            renderGroundStarSeal(starVc, poseStack, -1.15F, 0.03F, burstZ - 0.55F, (0.42F + 0.20F * grow) * fade, -timeSec * 35.0F);
            renderGroundStarSeal(starVc, poseStack,  1.15F, 0.03F, burstZ - 0.55F, (0.42F + 0.20F * grow) * fade,  timeSec * 35.0F);

            // Translucent vertical emerald light curtain rising from the ground seal
            VertexConsumer beamVc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
            Matrix4f bMat = poseStack.last().pose();
            int beamAlpha = Math.max(15, Math.round(75.0F * fade * grow));
            for (int b = 0; b < 4; b++) {
                float ba0 = (float) (b * Math.PI * 0.5D + timeSec * 0.6F);
                float ba1 = ba0 + 0.55F;
                float br = 0.85F;
                WindGliderRenderer.emitDoubleQuad(beamVc, bMat,
                        Mth.cos(ba0) * br, 0.04F, burstZ + Mth.sin(ba0) * br,
                        Mth.cos(ba1) * br, 0.04F, burstZ + Mth.sin(ba1) * br,
                        Mth.cos(ba1) * br, 2.65F * grow, burstZ + Mth.sin(ba1) * br,
                        Mth.cos(ba0) * br, 2.65F * grow, burstZ + Mth.sin(ba0) * br,
                        95, 255, 135, beamAlpha);
            }

            // Side curved vine-flowers flanking the Heart Arch
            VertexConsumer vineVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(vineTex));
            for (float side : new float[]{-1.15F, 1.15F}) {
                poseStack.pushPose();
                poseStack.translate(side, 0.03F, burstZ - 0.55F);
                renderUnfurlingTexturedRibbon(vineVc, poseStack, 0.48F * fade, 1.65F * fade, grow, timeSec, 0.09F, side, 10);
                poseStack.popPose();
            }

            // Center Twin-Scroll Heart of Vines Arch (nahida_heart_vine.png) unfurling from bottom to top
            VertexConsumer heartVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(heartTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.05F, burstZ);
            float heartHalfW = (1.35F + 0.25F * grow) * fade;
            float heartH = 2.85F * fade;
            renderUnfurlingTexturedRibbon(heartVc, poseStack, heartHalfW, heartH, grow, timeSec, 0.05F, 0.0F, 12);
            poseStack.popPose();

            // Ground-scattered & rising waffle 4-petal clovers (matching Image 4 foreground & ground sparkles)
            VertexConsumer petalVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
            for (int k = 0; k < 8; k++) {
                float ang = (float) (k * Math.PI * 0.25D + 0.35D);
                float rad = 1.05F + (k % 3) * 0.65F;
                float gx = Mth.cos(ang) * rad;
                float gz = 1.65F + Mth.sin(ang) * rad;
                float gy = 0.05F + (k % 2) * (0.25F * prog);
                float gHalf = (0.18F + (k % 3) * 0.06F) * fade * grow;
                renderGroundStarSeal(petalVc, poseStack, gx, gy, gz, gHalf, timeSec * 60.0F + k * 45.0F);
            }
        }
        poseStack.popPose();
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
                float pillarH = 9.3F; // Task 12: Scaled 1.5x (from 6.2F -> 9.3F)
                float domeApexY = 16.2F; // Task 12: Scaled 1.5x (from 10.8F -> 16.2F)
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

                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.54F, 0.02F, pz - 0.54F, px + 0.54F, 0.72F, pz + 0.54F, 240, 205, 85, aTrim);
                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.33F, 0.72F, pz - 0.33F, px + 0.33F, pillarH - 0.63F, pz + 0.33F, 68, 235, 115, aMain);
                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.57F, pillarH - 0.63F, pz - 0.57F, px + 0.57F, pillarH, pz + 0.57F, 245, 215, 95, aTrim);

                    float archPeakY = pillarH + 2.025F;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            px, pillarH, pz,
                            mx, archPeakY, mz,
                            mx, archPeakY + 0.33F, mz,
                            px, pillarH + 0.33F, pz,
                            240, 212, 92, aTrim);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx, archPeakY, mz,
                            nx, pillarH, nz,
                            nx, pillarH + 0.33F, nz,
                            mx, archPeakY + 0.33F, mz,
                            240, 212, 92, aTrim);

                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx - 0.18F, archPeakY - 0.675F, mz - 0.18F,
                            mx + 0.18F, archPeakY - 0.675F, mz + 0.18F,
                            mx + 0.03F, archPeakY, mz + 0.03F,
                            mx - 0.03F, archPeakY, mz - 0.03F,
                            95, 255, 145, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 0.975F, mz, 0.15F, 85, 255, 130, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 1.35F, mz, 0.12F, 120, 255, 160, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 1.725F, mz, 0.09F, 215, 255, 185, aTrim);

                    float midR = pRadius * 0.54F;
                    float midY = pillarH + 3.9F;
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
                            px - 0.12F, pillarH, pz - 0.12F,
                            px + 0.12F, pillarH, pz + 0.12F,
                            mx0 + 0.09F, midY, mz0 + 0.09F,
                            mx0 - 0.09F, midY, mz0 - 0.09F,
                            250, 225, 110, aTrim);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx0 - 0.09F, midY, mz0 - 0.09F,
                            mx0 + 0.09F, midY, mz0 + 0.09F,
                            0.06F, domeApexY, 0.06F,
                            -0.06F, domeApexY, -0.06F,
                            250, 225, 110, aTrim);
                }

                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.36F, domeApexY - 0.225F, -0.36F, 0.36F, domeApexY + 2.175F, 0.36F, 245, 220, 95, aTrim);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.18F, domeApexY + 2.175F, -0.18F, 0.18F, domeApexY + 3.525F, 0.18F, 120, 255, 155, aTrim);
                poseStack.popPose();

                VertexConsumer skVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
                poseStack.pushPose();
                poseStack.translate(cx, cy + domeApexY - 2.025F + Mth.sin(timeSec * 2.2F) * 0.22F, cz);
                poseStack.mulPose(Axis.YP.rotationDegrees(timeSec * 45.0F));
                float cloverHalf = 1.725F;
                for (int plane = 0; plane < 2; plane++) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(plane * 90.0F));
                    PoseStack.Pose p = poseStack.last();
                    WindGliderRenderer.emitWingQuad(skVc, p, -cloverHalf, -cloverHalf, 0.0F, 0.0F, 1.0F, cloverHalf, -cloverHalf, 0.0F, 1.0F, 1.0F, cloverHalf, cloverHalf, 0.0F, 1.0F, 0.0F, -cloverHalf, cloverHalf, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                    WindGliderRenderer.emitWingQuad(skVc, p, -cloverHalf, cloverHalf, 0.0F, 0.0F, 0.0F, cloverHalf, cloverHalf, 0.0F, 1.0F, 0.0F, cloverHalf, -cloverHalf, 0.0F, 1.0F, 1.0F, -cloverHalf, -cloverHalf, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                    poseStack.popPose();
                }
                poseStack.popPose();
            } else if (arc.fromEntityId == -8 && (arc.color & 0xFFFF0000) == 0x4E410000) {
                renderNahidaNormalAttackWorldVfx(poseStack, bufferSource, camPos, arc, now, skandhaTex);
            } else if (arc.fromEntityId == -8 && (arc.color & 0xFFFF0000) == 0x4E420000) {
                renderNahidaShrineFootstepGrass(poseStack, bufferSource, camPos, arc, now);
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
