package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Dedicated visual renderer for Electro Archon Raiden Shogun (Engulfing Lightning):
 * - Chakra Desiderata HUD and 60-stack mandala ring + Eye of Stormy Judgment
 * - Musou no Hitotachi 3-phase chest draw and Musou Isshin N1-N5 + CA sword poses
 * - World violet/amethyst dimensional slash arcs
 */
public final class RaidenVisualRenderer {
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static int raidenAttackHoldTicks = 0;

    private RaidenVisualRenderer() {}

    public static void tickClientAttackHold(Minecraft mc) {
        if (mc.level != null && mc.player != null) {
            if (mc.screen == null && mc.options.keyAttack.isDown()
                    && ClientAuraTracker.isRaidenMusouActive(mc.player.getId())
                    && mc.player.getMainHandItem().getItem() instanceof ArcaneStaffItem
                    && ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                ClientAuraTracker.localRaidenAttackHeld = true;
                raidenAttackHoldTicks++;
                if (raidenAttackHoldTicks >= 6 && ClientAuraTracker.tryConsumeLocalRaidenSwing(true)) {
                    ClientAuraTracker.triggerStaffAnimation(mc.player.getId(), ClientAuraTracker.ANIM_RAIDEN_CA);
                    ModNetwork.sendRaidenSlash(true);
                }
            } else {
                ClientAuraTracker.localRaidenAttackHeld = false;
                raidenAttackHoldTicks = 0;
            }
        }
    }

    public static void renderRaidenEyeRing(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        ClientAuraTracker.RaidenDisplayState state = ClientAuraTracker.getRaiden(entity.getId());
        if (state == null) return;
        float eyeTicks = state.effectiveEyeTicks();
        float musouTicks = state.effectiveMusouTicks();
        if (eyeTicks <= 0.0F && musouTicks <= 0.0F && state.resolve <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        boolean holdingRaiden = ArcaneStaffItem.getEffectiveKind(entity.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                || ArcaneStaffItem.getEffectiveKind(entity.getOffhandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (holdingRaiden && (state.resolve > 0 || musouTicks > 0.0F || eyeTicks > 0.0F)) {
            float resolveFrac = musouTicks > 0.0F ? 1.0F : Mth.clamp(state.resolve / 60.0F, 0.0F, 1.0F);
            ResourceLocation ringTex = new ResourceLocation("velorise_staves", "textures/entity/raiden_chakra_ring.png");
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(ringTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.76F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, -0.44F);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float pulse = 1.0F + 0.025F * Mth.sin(time * 0.20F);
            float half = (musouTicks > 0.0F ? 0.94F : Mth.lerp(resolveFrac, 0.78F, 0.88F)) * pulse;
            int baseCr = musouTicks > 0.0F ? 225 : 145;
            int baseCg = musouTicks > 0.0F ? (state.netherite ? 175 : 150) : 75;
            int baseCb = 255;
            int baseCa = musouTicks > 0.0F ? 210 : (state.resolve > 0 ? 115 : 65);
            org.joml.Matrix3f norm = pose.normal();
            vc.vertex(mat, -half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat, -half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();

            vc.vertex(mat, -half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat, -half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();

            if (resolveFrac > 0.001F) {
                VertexConsumer glowVc = buffer.getBuffer(RenderType.lightning());
                float rMid = half * 0.74F;
                float rIn = rMid - half * 0.065F;
                float rOut = rMid + half * 0.065F;
                float rCoreIn = rMid - half * 0.028F;
                float rCoreOut = rMid + half * 0.028F;
                float maxTheta = (float) Math.PI * resolveFrac;
                int segs = 24;
                boolean burstReady = true;
                int outerR = burstReady ? 195 : 145;
                int outerG = burstReady ? 105 : 70;
                int outerB = 255;
                int outerAlpha = musouTicks > 0.0F ? 235 : (burstReady ? 195 : 120);
                int coreAlpha = burstReady ? 250 : 165;

                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < segs; i++) {
                        float t0 = (float) i / (float) segs;
                        float t1 = (float) (i + 1) / (float) segs;
                        float a0 = -(float) (Math.PI * 0.5) + side * (maxTheta * t0);
                        float a1 = -(float) (Math.PI * 0.5) + side * (maxTheta * t1);
                        float aLo = Math.min(a0, a1);
                        float aHi = Math.max(a0, a1);
                        float c0 = Mth.cos(aLo), s0 = Mth.sin(aLo);
                        float c1 = Mth.cos(aHi), s1 = Mth.sin(aHi);

                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                c0 * rIn,  s0 * rIn,  0.004F, outerR, outerG, outerB, outerAlpha,
                                c0 * rOut, s0 * rOut, 0.004F, outerR, outerG, outerB, outerAlpha,
                                c1 * rOut, s1 * rOut, 0.004F, outerR, outerG, outerB, outerAlpha,
                                c1 * rIn,  s1 * rIn,  0.004F, outerR, outerG, outerB, outerAlpha);
                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                c0 * rIn,  s0 * rIn,  -0.004F, outerR, outerG, outerB, outerAlpha,
                                c1 * rIn,  s1 * rIn,  -0.004F, outerR, outerG, outerB, outerAlpha,
                                c1 * rOut, s1 * rOut, -0.004F, outerR, outerG, outerB, outerAlpha,
                                c0 * rOut, s0 * rOut, -0.004F, outerR, outerG, outerB, outerAlpha);

                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                c0 * rCoreIn,  s0 * rCoreIn,  0.005F, 245, 225, 255, coreAlpha,
                                c0 * rCoreOut, s0 * rCoreOut, 0.005F, 245, 225, 255, coreAlpha,
                                c1 * rCoreOut, s1 * rCoreOut, 0.005F, 245, 225, 255, coreAlpha,
                                c1 * rCoreIn,  s1 * rCoreIn,  0.005F, 245, 225, 255, coreAlpha);
                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                c0 * rCoreIn,  s0 * rCoreIn,  -0.005F, 245, 225, 255, coreAlpha,
                                c1 * rCoreIn,  s1 * rCoreIn,  -0.005F, 245, 225, 255, coreAlpha,
                                c1 * rCoreOut, s1 * rCoreOut, -0.005F, 245, 225, 255, coreAlpha,
                                c0 * rCoreOut, s0 * rCoreOut, -0.005F, 245, 225, 255, coreAlpha);
                    }
                }
            }
            poseStack.popPose();
        }

        if (eyeTicks > 0.0F) {
            ResourceLocation eyeTex = new ResourceLocation("velorise_staves", "textures/entity/raiden_eye_aura.png");
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(eyeTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() + 0.36F + 0.04F * Mth.sin(time * 0.14F), 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, -0.22F);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float half = 0.34F;
            int cr = 255, cg = state.netherite ? 235 : 210, cb = 255, ca = 245;
            org.joml.Matrix3f norm = pose.normal();
            vc.vertex(mat, -half, -half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat, -half,  half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();

            vc.vertex(mat, -half,  half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 0.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat, -half, -half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 1.0F).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            poseStack.popPose();
        }
    }

    public static void renderRaidenHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        ClientAuraTracker.RaidenDisplayState state = ClientAuraTracker.getRaiden(mc.player.getId());
        boolean holdingRaiden = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                || ArcaneStaffItem.getEffectiveKind(mc.player.getOffhandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (!holdingRaiden || state == null) return;
        float eyeTicks = state.effectiveEyeTicks();
        float musouTicks = state.effectiveMusouTicks();
        float burstCdTicks = state.effectiveBurstCooldownTicks();

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        float cx = sw * 0.5F + 38.0F;
        float cy = sh * 0.5F;

        Matrix4f mat = guiGraphics.pose().last().pose();
        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        float startDeg = -52.0F;
        float endDeg = 52.0F;
        int segments = 20;

        boolean burstReady = musouTicks <= 0.0F && burstCdTicks <= 0.0F;

        WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 29.0F, 35.0F, startDeg, endDeg, segments,
                burstReady ? 28 : 22, burstReady ? 12 : 16, burstReady ? 42 : 28, burstReady ? 175 : 120);
        float resolveFrac = Mth.clamp(state.resolve / 60.0F, 0.0F, 1.0F);
        if (resolveFrac > 0.01F) {
            float fillEnd = Mth.lerp(resolveFrac, startDeg, endDeg);
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 29.5F, 34.5F, startDeg, fillEnd, Math.max(2, Math.round(segments * resolveFrac)),
                    burstReady ? 205 : 110, burstReady ? 105 : 82, burstReady ? 255 : 138, burstReady ? 245 : 155);
        }

        if (musouTicks > 0.0F) {
            float mFrac = Mth.clamp(musouTicks / 140.0F, 0.0F, 1.0F);
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 36.0F, 41.0F, startDeg, endDeg, segments, 22, 8, 36, 190);
            if (mFrac > 0.01F) {
                float mEnd = Mth.lerp(mFrac, startDeg, endDeg);
                WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 36.5F, 40.5F, startDeg, mEnd, Math.max(2, Math.round(segments * mFrac)), 235, 175, 255, 250);
            }
        }
        guiGraphics.flush();

        Font font = mc.font;
        int labelX = Math.round(cx + 38);
        int labelY = Math.round(cy - 9);
        if (eyeTicks > 0.0F) {
            String eyeStr = String.format(Locale.ROOT, "◉ %.0fs", Math.ceil(eyeTicks / 20.0F));
            guiGraphics.drawString(font, eyeStr, labelX, labelY, 0xFFD99BFF, true);
            labelY += 10;
        }
        String resolveLabel;
        if (burstReady) {
            resolveLabel = "Resolve " + state.resolve + "/60 [R]";
        } else if (musouTicks > 0.0F) {
            resolveLabel = "Resolve " + state.resolve + "/60";
        } else {
            resolveLabel = String.format(Locale.ROOT, "Resolve %d/60 (%.1fs)", state.resolve, burstCdTicks / 20.0F);
        }
        guiGraphics.drawString(font, resolveLabel, labelX, labelY, burstReady ? 0xFFE8CAFF : 0xAA7E6B8F, true);
        if (musouTicks > 0.0F) {
            String mStr = String.format(Locale.ROOT, "Musou %.1fs", musouTicks / 20.0F);
            guiGraphics.drawString(font, mStr, labelX, labelY + 10, 0xFFFFD6FF, true);
        }
    }

    public static boolean renderSlashArc(
            ClientAuraTracker.ActiveArc arc,
            Vec3 startPos,
            Vec3 endPos,
            Vec3 camPos,
            long now,
            VertexConsumer consumer,
            Matrix4f matrix
    ) {
        if (arc.color != 0xFFB84DFF && arc.color != 0xFFE0B0FF) return false;
        boolean isMusou = (arc.color == 0xFFE0B0FF);
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float fade = (prog < 0.25F) ? (prog / 0.25F) : (1.0F - (prog - 0.25F) / 0.75F);
        int alpha = Mth.clamp((int) (255.0F * fade), 10, 255);
        Vec3 sCam = startPos.subtract(camPos);
        Vec3 eCam = endPos.subtract(camPos);
        Vec3 slashDir = eCam.subtract(sCam);
        if (slashDir.lengthSqr() > 1.0E-5D) {
            Vec3 unit = slashDir.normalize();
            Vec3 toCam = sCam.add(eCam).scale(-0.5D);
            Vec3 perp = unit.cross(toCam.lengthSqr() > 1.0E-4D ? toCam.normalize() : new Vec3(0.0D, 1.0D, 0.0D));
            if (perp.lengthSqr() < 1.0E-4D) perp = unit.cross(new Vec3(1.0D, 0.0D, 0.0D));
            perp = perp.normalize();
            double outerHalf = (isMusou ? 0.24D : 0.13D) * (1.05D - prog * 0.35D);
            double innerHalf = outerHalf * 0.36D;
            Vec3 midCam = sCam.lerp(eCam, 0.5D);
            Vec3 pOut = perp.scale(outerHalf);
            Vec3 pIn = perp.scale(innerHalf);
            int outR = 178, outG = 72, outB = 255;
            int midR = 192, midG = 92, midB = 255;
            int inR = 245, inG = 225, inB = 255;
            ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                    (float) sCam.x, (float) sCam.y, (float) sCam.z, outR, outG, outB, alpha / 2,
                    (float) (midCam.x + pOut.x), (float) (midCam.y + pOut.y), (float) (midCam.z + pOut.z), midR, midG, midB, alpha,
                    (float) eCam.x, (float) eCam.y, (float) eCam.z, outR, outG, outB, alpha / 2,
                    (float) (midCam.x - pOut.x), (float) (midCam.y - pOut.y), (float) (midCam.z - pOut.z), midR, midG, midB, alpha);
            ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                    (float) sCam.x, (float) sCam.y, (float) sCam.z, inR, inG, inB, alpha,
                    (float) (midCam.x + pIn.x), (float) (midCam.y + pIn.y), (float) (midCam.z + pIn.z), 255, 250, 240, alpha,
                    (float) eCam.x, (float) eCam.y, (float) eCam.z, inR, inG, inB, alpha,
                    (float) (midCam.x - pIn.x), (float) (midCam.y - pIn.y), (float) (midCam.z - pIn.z), 255, 250, 240, alpha);
        }
        return true;
    }

    public static boolean applyHumanoidAnimation(
            LivingEntity entity,
            HumanoidModel<?> model,
            float ageInTicks,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            float t
    ) {
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_EYE) {
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float slash = WindGliderRenderer.smoothStep01(t / 0.48F);
            float w = (t < 0.58F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.58F) / 0.42F) * (float) Math.PI));
            mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -1.35F, -1.55F));
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, -0.55F, 0.65F));
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, -0.35F, 0.45F));
            offPart.xRot = Mth.lerp(w, offPart.xRot, 0.25F * slash);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW) {
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;

            float stanceW = (t < 0.88F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.88F) / 0.12F) * (float) Math.PI));
            model.leftLeg.xRot = Mth.lerp(stanceW, model.leftLeg.xRot, 0.22F);
            model.rightLeg.xRot = Mth.lerp(stanceW, model.rightLeg.xRot, -0.18F);
            model.leftLeg.zRot = Mth.lerp(stanceW, model.leftLeg.zRot, -0.08F);
            model.rightLeg.zRot = Mth.lerp(stanceW, model.rightLeg.zRot, 0.08F);
            if (t < 0.625F) {
                float reach = WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.35F));
                model.body.xRot = Mth.lerp(reach, model.body.xRot, -0.15F);
                mainPart.xRot = Mth.lerp(reach, mainPart.xRot, -1.1F);
                mainPart.yRot = Mth.lerp(reach, mainPart.yRot, castDir * -0.6F);
                mainPart.zRot = Mth.lerp(reach, mainPart.zRot, castDir * 0.4F);
                offPart.xRot = Mth.lerp(reach, offPart.xRot, -0.85F);
                offPart.yRot = Mth.lerp(reach, offPart.yRot, offDir * -0.35F);
                offPart.zRot = Mth.lerp(reach, offPart.zRot, offDir * 0.20F);
                model.head.xRot = Mth.lerp(reach, model.head.xRot, 0.2F);
                model.hat.copyFrom(model.head);
            } else if (t < 0.8125F) {
                float pull = WindGliderRenderer.smoothStep01((t - 0.625F) / 0.1875F);
                float tremor = Mth.sin(ageInTicks * 2.4F) * 0.02F * pull;
                model.body.xRot = Mth.lerp(pull, -0.15F, 0.0F);
                model.body.yRot = castDir * Mth.lerp(pull, 0.0F, -0.4F);
                mainPart.xRot = Mth.lerp(pull, -1.1F, -2.2F) + tremor;
                mainPart.yRot = castDir * Mth.lerp(pull, -0.6F, 0.2F);
                mainPart.zRot = castDir * Mth.lerp(pull, 0.4F, 0.1F);
                offPart.xRot = Mth.lerp(pull, -0.85F, -0.35F);
                model.head.xRot = Mth.lerp(pull, 0.2F, 0.0F);
                model.hat.copyFrom(model.head);
            } else {
                float slash = WindGliderRenderer.smoothStep01((t - 0.8125F) / 0.1875F);
                model.body.xRot = Mth.lerp(slash, 0.0F, 0.2F);
                model.body.yRot = castDir * Mth.lerp(slash, -0.4F, 0.25F);
                mainPart.xRot = Mth.lerp(slash, -2.2F, -0.3F);
                mainPart.yRot = castDir * Mth.lerp(slash, 0.2F, 1.2F);
                mainPart.zRot = castDir * Mth.lerp(slash, 0.1F, -0.5F);
                offPart.xRot = Mth.lerp(slash, -0.35F, 0.30F);
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_SLASH) {
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            int step = data != null ? Math.floorMod(data.raidenComboStep, 5) : 0;
            float slash = WindGliderRenderer.smoothStep01(t / 0.48F);
            float w = (t < 0.60F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.60F) / 0.40F) * (float) Math.PI));
            switch (step) {
                case 0 -> {
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, -1.30F);
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, 0.85F, -0.95F));
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, 0.45F, -0.35F));
                    offPart.xRot = Mth.lerp(w, offPart.xRot, 0.25F * slash);
                    model.body.yRot = castDir * Mth.lerp(slash, -0.22F, 0.25F) * w;
                }
                case 1 -> {
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -0.55F, -2.05F));
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, -0.85F, 0.65F));
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, -0.45F, 0.35F));
                    offPart.xRot = Mth.lerp(w, offPart.xRot, 0.30F * slash);
                    model.body.yRot = castDir * Mth.lerp(slash, 0.20F, -0.22F) * w;
                }
                case 2 -> {
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -2.10F, -0.45F));
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, 0.75F, -0.85F));
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, 0.40F, -0.45F));
                    offPart.xRot = Mth.lerp(w, offPart.xRot, 0.25F);
                    model.body.yRot = castDir * Mth.lerp(slash, -0.22F, 0.26F) * w;
                }
                case 3 -> {
                    if (t < 0.44F) {
                        float f1 = WindGliderRenderer.smoothStep01(t / 0.44F);
                        mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(f1, -0.95F, -1.65F));
                        mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(f1, -0.85F, 0.90F));
                        mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(f1, -0.48F, 0.48F));
                        model.body.yRot = castDir * Mth.lerp(f1, 0.22F, -0.24F) * w;
                    } else {
                        float f2 = WindGliderRenderer.smoothStep01((t - 0.44F) / 0.40F);
                        mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(f2, -1.65F, -1.05F));
                        mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(f2, 0.90F, -0.95F));
                        mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(f2, 0.48F, -0.52F));
                        model.body.yRot = castDir * Mth.lerp(f2, -0.24F, 0.26F) * w;
                    }
                    offPart.xRot = Mth.lerp(w, offPart.xRot, -0.35F);
                }
                default -> {
                    float lift = WindGliderRenderer.smoothStep01(t / 0.28F);
                    float drop = WindGliderRenderer.smoothStep01((t - 0.28F) / 0.36F);
                    float targetX = (t < 0.28F) ? Mth.lerp(lift, -1.45F, -2.75F) : Mth.lerp(drop, -2.75F, -0.25F);
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, targetX);
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, 0.0F);
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.08F);
                    offPart.xRot = Mth.lerp(w, offPart.xRot, targetX * 0.92F);
                    offPart.yRot = Mth.lerp(w, offPart.yRot, -castDir * 0.35F);
                    model.body.xRot = Mth.lerp(drop, -0.08F, 0.22F) * w;
                }
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_CA) {
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;
            float w = (t < 0.74F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.74F) / 0.26F) * (float) Math.PI));
            if (t < 0.28F) {
                float wind = WindGliderRenderer.smoothStep01(t / 0.28F);
                model.body.xRot = Mth.lerp(w, model.body.xRot, 0.22F * wind);
                model.body.yRot = Mth.lerp(w, model.body.yRot, -castDir * 0.32F * wind);
                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.38F * wind);
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.32F * wind);
                model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.12F * wind);

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, -0.45F);
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * -0.92F);
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * -0.55F);
                offPart.xRot = Mth.lerp(w, offPart.xRot, -0.52F);
                offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * 0.45F);
                model.head.xRot += 0.08F * wind * w;
            } else {
                float sweep = WindGliderRenderer.smoothStep01((t - 0.28F) / 0.46F);
                model.body.xRot = Mth.lerp(w, model.body.xRot, Mth.lerp(sweep, 0.22F, 0.28F));
                model.body.yRot = Mth.lerp(w, model.body.yRot, castDir * Mth.lerp(sweep, -0.32F, 0.42F));
                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, Mth.lerp(sweep, 0.38F, -0.25F));
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, Mth.lerp(sweep, -0.32F, 0.45F));

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(sweep, -0.45F, -1.85F));
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(sweep, -0.92F, 0.65F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(sweep, -0.55F, 0.45F));
                offPart.xRot = Mth.lerp(w, offPart.xRot, Mth.lerp(sweep, -0.52F, 0.48F));
                offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * -0.35F);
            }
            return true;
        }
        return false;
    }

    public static boolean applyThirdPersonItemTransform(
            ArcaneStaffItem.Kind effKind,
            boolean leftHand,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (effKind != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) return false;
        float side = leftHand ? -1.0F : 1.0F;
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            if (t < 0.22F) {
                float reach = WindGliderRenderer.smoothStep01(t / 0.22F);
                poseStack.translate(-side * 0.12F * reach, -0.04F * reach, 0.04F * reach);
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 12.0F * reach));
                poseStack.mulPose(Axis.XP.rotationDegrees(-152.0F * reach));
            } else if (t < 0.62F) {
                float pull = WindGliderRenderer.smoothStep01((t - 0.22F) / 0.40F);
                poseStack.translate(-side * Mth.lerp(pull, 0.12F, 0.08F), Mth.lerp(pull, -0.04F, 0.02F), 0.04F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(pull, 12.0F, 4.0F)));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pull, -152.0F, -210.0F)));
            } else if (t < 0.72F) {
                float prep = WindGliderRenderer.smoothStep01((t - 0.62F) / 0.10F);
                poseStack.translate(-side * 0.08F, 0.02F, Mth.lerp(prep, 0.04F, -0.04F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(prep, 4.0F, 64.0F)));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(prep, -210.0F, -36.0F)));
            } else {
                float slash = WindGliderRenderer.smoothStep01((t - 0.72F) / 0.16F);
                float w = (t < 0.88F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.88F) / 0.12F) * (float) Math.PI));
                poseStack.translate(side * Mth.lerp(slash, -0.08F, 0.06F) * w, 0.02F * w, -0.06F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(-36.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(slash, 64.0F, -68.0F) * w));
            }
        }
        return true;
    }

    public static boolean applyFirstPersonTransform(
            AbstractClientPlayer player,
            float partialTicks,
            ArcaneStaffItem.Kind effKind,
            float dir,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (effKind != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) return false;
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_EYE && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float slash = WindGliderRenderer.smoothStep01(t / 0.48F);
            float w = (t < 0.58F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.58F) / 0.42F) * (float) Math.PI));
            poseStack.translate(dir * Mth.lerp(slash, 0.24F, -0.32F) * w, Mth.lerp(slash, -0.06F, 0.14F) * w, -0.26F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slash, -28.0F, -52.0F) * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, -55.0F, 62.0F) * w));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            if (t < 0.22F) {
                float reach = WindGliderRenderer.smoothStep01(t / 0.22F);
                poseStack.translate(-dir * 0.42F * reach, -0.24F * reach, -0.18F * reach);
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 172.0F * reach));
                poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F * reach));
            } else if (t < 0.62F) {
                float pull = WindGliderRenderer.smoothStep01((t - 0.22F) / 0.40F);
                float tremor = Mth.sin((player.tickCount + partialTicks) * 2.4F) * 0.006F * pull;
                poseStack.translate(-dir * 0.42F + tremor, Mth.lerp(pull, -0.24F, 0.30F), -0.18F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 172.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
            } else if (t < 0.72F) {
                float prep = WindGliderRenderer.smoothStep01((t - 0.62F) / 0.10F);
                poseStack.translate(-dir * Mth.lerp(prep, 0.42F, 0.46F), Mth.lerp(prep, 0.30F, -0.02F), Mth.lerp(prep, -0.18F, -0.28F));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(prep, 0.0F, -24.0F)));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(prep, -10.0F, -58.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(prep, 172.0F, 82.0F)));
            } else {
                float slash = WindGliderRenderer.smoothStep01((t - 0.72F) / 0.16F);
                float w = (t < 0.88F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.88F) / 0.12F) * (float) Math.PI));
                poseStack.translate(dir * Mth.lerp(slash, -0.46F, 0.34F) * w, -0.02F * w, -0.28F * w);
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(slash, -24.0F, 52.0F) * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(-58.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, 82.0F, -76.0F) * w));
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_SLASH && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float slash = WindGliderRenderer.smoothStep01(t / 0.48F);
            float w = (t < 0.60F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.60F) / 0.40F) * (float) Math.PI));
            switch (Math.floorMod(data.raidenComboStep, 5)) {
                case 0 -> {
                    poseStack.translate(dir * Mth.lerp(slash, 0.28F, -0.32F) * w, 0.02F * w, -0.24F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-34.0F * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, -64.0F, 68.0F) * w));
                }
                case 1 -> {
                    poseStack.translate(dir * Mth.lerp(slash, -0.26F, 0.24F) * w, Mth.lerp(slash, -0.16F, 0.18F) * w, -0.24F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slash, -16.0F, -58.0F) * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, 58.0F, -50.0F) * w));
                }
                case 2 -> {
                    poseStack.translate(dir * Mth.lerp(slash, 0.24F, -0.28F) * w, Mth.lerp(slash, 0.18F, -0.18F) * w, -0.22F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slash, -56.0F, -12.0F) * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, -50.0F, 56.0F) * w));
                }
                case 3 -> {
                    if (t < 0.44F) {
                        float f1 = WindGliderRenderer.smoothStep01(t / 0.44F);
                        poseStack.translate(dir * Mth.lerp(f1, -0.26F, 0.26F) * w, Mth.lerp(f1, -0.12F, 0.12F) * w, -0.26F * w);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-44.0F * w));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(f1, 64.0F, -58.0F) * w));
                    } else {
                        float f2 = WindGliderRenderer.smoothStep01((t - 0.44F) / 0.40F);
                        poseStack.translate(dir * Mth.lerp(f2, 0.26F, -0.28F) * w, Mth.lerp(f2, 0.12F, -0.12F) * w, -0.26F * w);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-44.0F * w));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(f2, -58.0F, 64.0F) * w));
                    }
                }
                default -> {
                    float lift = WindGliderRenderer.smoothStep01(t / 0.28F);
                    float drop = WindGliderRenderer.smoothStep01((t - 0.28F) / 0.36F);
                    float ty = (t < 0.28F) ? Mth.lerp(lift, 0.04F, 0.32F) : Mth.lerp(drop, 0.32F, -0.26F);
                    float xp = (t < 0.28F) ? Mth.lerp(lift, -24.0F, 18.0F) : Mth.lerp(drop, 18.0F, -82.0F);
                    poseStack.translate(-dir * 0.12F * w, ty * w, -0.28F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(xp * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 8.0F * w));
                }
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_CA && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.74F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.74F) / 0.26F) * (float) Math.PI));
            if (t < 0.28F) {
                float wind = WindGliderRenderer.smoothStep01(t / 0.28F);
                float tremor = Mth.sin((player.tickCount + partialTicks) * 2.8F) * 0.006F * wind;
                poseStack.translate(dir * 0.18F * wind * w + tremor, -0.22F * wind * w, -0.15F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F * wind * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 85.0F * wind * w));
            } else {
                float sweep = WindGliderRenderer.smoothStep01((t - 0.28F) / 0.46F);
                poseStack.translate(dir * Mth.lerp(sweep, 0.18F, -0.36F) * w, Mth.lerp(sweep, -0.22F, 0.28F) * w, -0.32F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(sweep, -24.0F, -68.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(sweep, 85.0F, -78.0F) * w));
            }
            return true;
        }
        return true;
    }
}
