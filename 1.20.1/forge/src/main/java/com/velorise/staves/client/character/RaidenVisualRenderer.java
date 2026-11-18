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
        if (mc == null || mc.player == null || !ClientAuraTracker.isRaidenMusouActive(mc.player.getId())) {
            ClientAuraTracker.localRaidenAttackHeld = false;
        }
        raidenAttackHoldTicks = 0;
    }

    public static void renderRaidenEyeRing(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        ClientAuraTracker.RaidenDisplayState state = ClientAuraTracker.getRaiden(entity.getId());
        boolean isIdle = ClientAuraTracker.isRaidenIdle(entity.getId());
        if (state == null && !isIdle) return;
        float eyeTicks = (state != null) ? state.effectiveEyeTicks() : 0.0F;
        float musouTicks = (state != null) ? state.effectiveMusouTicks() : 0.0F;
        int resolve = (state != null) ? state.resolve : 0;
        boolean netherite = (state != null) && state.netherite;
        if (eyeTicks <= 0.0F && musouTicks <= 0.0F && resolve <= 0 && !isIdle) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float liftOffset = ClientAuraRenderer.getTotalLevitationOffsetY(entity, pt);
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float bodyYaw = (d != null && d.raidenIdleYawSet) ? d.raidenIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        boolean holdingRaiden = ArcaneStaffItem.getEffectiveKind(entity.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                || ArcaneStaffItem.getEffectiveKind(entity.getOffhandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (!isIdle && holdingRaiden && (resolve > 0 || musouTicks > 0.0F || eyeTicks > 0.0F)) {
            float resolveFrac = (musouTicks > 0.0F || isIdle) ? 1.0F : Mth.clamp(resolve / 60.0F, 0.0F, 1.0F);
            ResourceLocation ringTex = new ResourceLocation("velorise_staves", "textures/entity/raiden_chakra_ring.png");
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(ringTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.76F + liftOffset, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, -0.44F);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float pulse = 1.0F + 0.025F * Mth.sin(time * 0.20F);
            float half = (musouTicks > 0.0F ? 0.94F : Mth.lerp(resolveFrac, 0.78F, 0.88F)) * pulse;
            int baseCr = musouTicks > 0.0F ? 225 : 145;
            int baseCg = musouTicks > 0.0F ? (netherite ? 175 : 150) : 75;
            int baseCb = 255;
            int baseCa = musouTicks > 0.0F ? 210 : (resolve > 0 ? 115 : 65);
            org.joml.Matrix3f norm = pose.normal();
            vc.vertex(mat, -half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat, -half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();

            vc.vertex(mat, -half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(1.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat, -half, -half, 0.0F).color(baseCr, baseCg, baseCb, baseCa).uv(0.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();

            if (resolveFrac > 0.001F) {
                // Task 6: Illuminate only individual Chakra Desiderata Resolve beads/dots along the ring circumference (no neon arc ribbon)
                VertexConsumer glowVc = buffer.getBuffer(RenderType.lightning());
                float rBeadRing = half * 0.742F;
                int beadsPerSide = 9; // 18 beads total symmetrically distributed from bottom (-90 deg) to top (+90 deg)
                float litProgress = resolveFrac * beadsPerSide;
                int haloR = netherite ? 215 : 191;
                int haloG = netherite ? 130 : 90;
                int haloB = 255;

                for (int side = -1; side <= 1; side += 2) {
                    for (int bIdx = 0; bIdx < beadsPerSide; bIdx++) {
                        float beadFill = Mth.clamp(litProgress - bIdx, 0.0F, 1.0F);
                        if (beadFill <= 0.02F) continue;
                        float fracAngle = (bIdx + 0.5F) / (float) beadsPerSide;
                        float theta = -(float) (Math.PI * 0.5D) + side * ((float) Math.PI * fracAngle);
                        float bx = Mth.cos(theta) * rBeadRing;
                        float by = Mth.sin(theta) * rBeadRing;

                        float beadPulse = 1.0F + 0.10F * Mth.sin(time * 0.28F + bIdx * 0.7F);
                        float outerRad = half * 0.036F * (0.65F + 0.35F * beadFill) * beadPulse;
                        float coreRad = outerRad * 0.52F;
                        int haloAlpha = Math.round((musouTicks > 0.0F ? 235.0F : 205.0F) * beadFill);
                        int coreAlpha = Math.round(252.0F * beadFill);

                        // Diamond/circular bead glow (front & back faces)
                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                bx, by - outerRad, 0.004F, haloR, haloG, haloB, haloAlpha,
                                bx + outerRad, by, 0.004F, haloR, haloG, haloB, haloAlpha,
                                bx, by + outerRad, 0.004F, haloR, haloG, haloB, haloAlpha,
                                bx - outerRad, by, 0.004F, haloR, haloG, haloB, haloAlpha);
                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                bx, by - outerRad, -0.004F, haloR, haloG, haloB, haloAlpha,
                                bx - outerRad, by, -0.004F, haloR, haloG, haloB, haloAlpha,
                                bx, by + outerRad, -0.004F, haloR, haloG, haloB, haloAlpha,
                                bx + outerRad, by, -0.004F, haloR, haloG, haloB, haloAlpha);

                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                bx, by - coreRad, 0.005F, 252, 238, 255, coreAlpha,
                                bx + coreRad, by, 0.005F, 252, 238, 255, coreAlpha,
                                bx, by + coreRad, 0.005F, 252, 238, 255, coreAlpha,
                                bx - coreRad, by, 0.005F, 252, 238, 255, coreAlpha);
                        ClientAuraRenderer.emitCrystalQuad(glowVc, mat,
                                bx, by - coreRad, -0.005F, 252, 238, 255, coreAlpha,
                                bx - coreRad, by, -0.005F, 252, 238, 255, coreAlpha,
                                bx, by + coreRad, -0.005F, 252, 238, 255, coreAlpha,
                                bx + coreRad, by, -0.005F, 252, 238, 255, coreAlpha);
                    }
                }
            }
            poseStack.popPose();
        }

        if (eyeTicks > 0.0F || isIdle) {
            ResourceLocation eyeTex = new ResourceLocation("velorise_staves", "textures/entity/raiden_eye_aura.png");
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(eyeTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() + 0.36F + liftOffset + 0.04F * Mth.sin(time * 0.14F), 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, -0.22F);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float half = 0.34F;
            int cr = 255, cg = netherite ? 235 : 210, cb = 255, ca = 245;
            org.joml.Matrix3f norm = pose.normal();
            vc.vertex(mat, -half, -half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();
            vc.vertex(mat, -half,  half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, 1.0F).endVertex();

            vc.vertex(mat, -half,  half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half,  half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 0.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat,  half, -half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
            vc.vertex(mat, -half, -half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 1.0F).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(norm, 0.0F, 0.0F, -1.0F).endVertex();
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

        // 1st-person Musou no Hitotachi Ulti overlay: cosmic eye rift + gently rotating Electro icon -> horizontal slash flash
        ClientAuraTracker.AuraDisplayData animData = ClientAuraTracker.get(mc.player.getId());
        long nowMs = System.currentTimeMillis();
        if (mc.options.getCameraType().isFirstPerson()
                && animData != null
                && nowMs < animData.staffAnimUntil
                && animData.staffAnimType == ClientAuraTracker.ANIM_RAIDEN_DRAW) {
            float totalMs = Math.max(1.0F, (float) (animData.staffAnimUntil - animData.staffAnimStartTime));
            float t = Mth.clamp((nowMs - animData.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float cx0 = sw * 0.5F;
            float cy0 = sh * 0.5F;


            if (t >= 0.70F) {
                // Blinding white-violet horizontal slash flash ("Torn to oblivion!") synced with slash & damage
                float fp = Mth.clamp((t - 0.70F) / 0.28F, 0.0F, 1.0F);
                float flashA = (fp < 0.22F) ? (fp / 0.22F) : (1.0F - WindGliderRenderer.smoothStep01((fp - 0.22F) / 0.78F));
                int screenFlashA = Math.round(130.0F * flashA);
                int beamA = Math.round(245.0F * flashA);
                int coreA = Math.round(255.0F * flashA);
                guiGraphics.fill(0, 0, sw, sh, (screenFlashA << 24) | 0x7A38E8);
                int beamHalf = Math.max(4, Math.round(28.0F * flashA));
                guiGraphics.fill(0, Math.round(cy0) - beamHalf, sw, Math.round(cy0) + beamHalf, (beamA << 24) | 0xD288FF);
                int coreHalf = Math.max(2, Math.round(10.0F * flashA));
                guiGraphics.fill(0, Math.round(cy0) - coreHalf, sw, Math.round(cy0) + coreHalf, (coreA << 24) | 0xFFFFFF);
            }
        }
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
        boolean isMusouOrCa = (arc.color == 0xFFE0B0FF);
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float fade = (prog < 0.22F) ? (prog / 0.22F) : (1.0F - (prog - 0.22F) / 0.78F);
        int alpha = Mth.clamp((int) (255.0F * fade), 10, 255);
        Vec3 sCam = startPos.subtract(camPos);
        Vec3 eCam = endPos.subtract(camPos);
        if (isMusouOrCa) {
            Vec3 mid = sCam.lerp(eCam, 0.5D);
            sCam = mid.add(sCam.subtract(mid).scale(3.0D));
            eCam = mid.add(eCam.subtract(mid).scale(3.0D));
        }
        Vec3 slashDir = eCam.subtract(sCam);
        double chordLen = slashDir.length();
        if (chordLen > 1.0E-4D) {
            Vec3 unit = slashDir.scale(1.0D / chordLen);
            Vec3 midChord = sCam.lerp(eCam, 0.5D);
            Vec3 toCam = midChord.scale(-1.0D);
            Vec3 perp = unit.cross(toCam.lengthSqr() > 1.0E-4D ? toCam.normalize() : new Vec3(0.0D, 1.0D, 0.0D));
            if (perp.lengthSqr() < 1.0E-4D) perp = unit.cross(new Vec3(1.0D, 0.0D, 0.0D));
            perp = perp.normalize();
            Vec3 curveOut = perp.cross(unit).normalize();
            if (curveOut.dot(toCam) > 0.0D) {
                curveOut = curveOut.scale(-1.0D);
            }

            // Smooth 12-segment crescent moon spline with 3-tier Electro gradient (0x7828C8 -> 0xBF5AF2 -> 0xFFFFFF)
            int segs = 12;
            double archDepth = chordLen * (isMusouOrCa ? 0.28D : 0.20D) * (0.92D + prog * 0.16D);
            double maxHalfWidth = (isMusouOrCa ? 0.90D : 0.16D) * (1.08D - prog * 0.32D);

            for (int i = 0; i < segs; i++) {
                double u0 = (double) i / (double) segs;
                double u1 = (double) (i + 1) / (double) segs;
                double s0 = Math.sin(u0 * Math.PI);
                double s1 = Math.sin(u1 * Math.PI);

                Vec3 c0 = sCam.lerp(eCam, u0).add(curveOut.scale(archDepth * s0));
                Vec3 c1 = sCam.lerp(eCam, u1).add(curveOut.scale(archDepth * s1));

                double w0 = maxHalfWidth * Math.pow(s0, 0.75D);
                double w1 = maxHalfWidth * Math.pow(s1, 0.75D);

                Vec3 outA0 = c0.add(perp.scale(w0)).add(curveOut.scale(w0 * 0.35D));
                Vec3 outB0 = c0.subtract(perp.scale(w0)).subtract(curveOut.scale(w0 * 0.25D));
                Vec3 outA1 = c1.add(perp.scale(w1)).add(curveOut.scale(w1 * 0.35D));
                Vec3 outB1 = c1.subtract(perp.scale(w1)).subtract(curveOut.scale(w1 * 0.25D));

                int aOuter0 = Math.max(8, (int) (alpha * 0.55F * s0));
                int aOuter1 = Math.max(8, (int) (alpha * 0.55F * s1));
                int aMid0 = Math.max(12, (int) (alpha * 0.88F * s0));
                int aMid1 = Math.max(12, (int) (alpha * 0.88F * s1));
                int aCore0 = Math.max(16, (int) (alpha * s0));
                int aCore1 = Math.max(16, (int) (alpha * s1));

                // Layer 1: Deep Inazuma Electro-Violet outer crescent aura (0x7828C8)
                ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                        (float) outA0.x, (float) outA0.y, (float) outA0.z, 120, 40, 200, aOuter0,
                        (float) outA1.x, (float) outA1.y, (float) outA1.z, 120, 40, 200, aOuter1,
                        (float) outB1.x, (float) outB1.y, (float) outB1.z, 120, 40, 200, aOuter1,
                        (float) outB0.x, (float) outB0.y, (float) outB0.z, 120, 40, 200, aOuter0);

                // Layer 2: Vibrant Raiden Amethyst mid blade (0xBF5AF2)
                Vec3 midA0 = c0.add(perp.scale(w0 * 0.62D)).add(curveOut.scale(w0 * 0.22D));
                Vec3 midB0 = c0.subtract(perp.scale(w0 * 0.52D));
                Vec3 midA1 = c1.add(perp.scale(w1 * 0.62D)).add(curveOut.scale(w1 * 0.22D));
                Vec3 midB1 = c1.subtract(perp.scale(w1 * 0.52D));
                ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                        (float) midA0.x, (float) midA0.y, (float) midA0.z, 191, 90, 242, aMid0,
                        (float) midA1.x, (float) midA1.y, (float) midA1.z, 191, 90, 242, aMid1,
                        (float) midB1.x, (float) midB1.y, (float) midB1.z, 191, 90, 242, aMid1,
                        (float) midB0.x, (float) midB0.y, (float) midB0.z, 191, 90, 242, aMid0);

                // Layer 3: Incandescent White-Violet lightning core (0xFFFFFF)
                Vec3 coreA0 = c0.add(perp.scale(w0 * 0.25D)).add(curveOut.scale(w0 * 0.12D));
                Vec3 coreB0 = c0.subtract(perp.scale(w0 * 0.20D));
                Vec3 coreA1 = c1.add(perp.scale(w1 * 0.25D)).add(curveOut.scale(w1 * 0.12D));
                Vec3 coreB1 = c1.subtract(perp.scale(w1 * 0.20D));
                ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                        (float) coreA0.x, (float) coreA0.y, (float) coreA0.z, 255, 245, 255, aCore0,
                        (float) coreA1.x, (float) coreA1.y, (float) coreA1.z, 255, 245, 255, aCore1,
                        (float) coreB1.x, (float) coreB1.y, (float) coreB1.z, 255, 245, 255, aCore1,
                        (float) coreB0.x, (float) coreB0.y, (float) coreB0.z, 255, 245, 255, aCore0);
            }
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
                float slash = WindGliderRenderer.smoothStep01((t - 0.8125F) / 0.125F);
                model.body.xRot = Mth.lerp(slash, 0.0F, 0.10F);
                model.body.yRot = castDir * Mth.lerp(slash, -0.40F, 0.38F);
                // Sweep straight arm from left (-1.05) across to stretched out right (+1.57), matching 1st-person slash
                mainPart.xRot = -1.57F;
                mainPart.yRot = castDir * Mth.lerp(slash, -1.05F, 1.57F);
                mainPart.zRot = 0.0F;
                offPart.xRot = Mth.lerp(slash, -0.35F, -1.25F);
                offPart.yRot = offDir * Mth.lerp(slash, 0.10F, -1.10F);
                offPart.zRot = 0.0F;
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
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, -0.95F, 0.85F));
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, -0.35F, 0.45F));
                    offPart.xRot = Mth.lerp(w, offPart.xRot, 0.25F * slash);
                    model.body.yRot = castDir * Mth.lerp(slash, -0.22F, 0.25F) * w;
                }
                case 1 -> {
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -0.55F, -2.05F));
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, 0.65F, -0.85F));
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, 0.35F, -0.45F));
                    offPart.xRot = Mth.lerp(w, offPart.xRot, 0.30F * slash);
                    model.body.yRot = castDir * Mth.lerp(slash, 0.20F, -0.22F) * w;
                }
                case 2 -> {
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -2.10F, -0.45F));
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, -0.85F, 0.75F));
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, -0.45F, 0.40F));
                    offPart.xRot = Mth.lerp(w, offPart.xRot, 0.25F);
                    model.body.yRot = castDir * Mth.lerp(slash, -0.22F, 0.26F) * w;
                }
                case 3 -> {
                    if (t < 0.44F) {
                        float f1 = WindGliderRenderer.smoothStep01(t / 0.44F);
                        mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(f1, -0.95F, -1.65F));
                        mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(f1, 0.90F, -0.85F));
                        mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(f1, 0.48F, -0.48F));
                        model.body.yRot = castDir * Mth.lerp(f1, 0.22F, -0.24F) * w;
                    } else {
                        float f2 = WindGliderRenderer.smoothStep01((t - 0.44F) / 0.40F);
                        mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(f2, -1.65F, -1.05F));
                        mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(f2, -0.95F, 0.90F));
                        mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(f2, -0.52F, 0.48F));
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
            float w = (t < 0.80F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.80F) / 0.20F) * (float) Math.PI));
            if (t < 0.22F) {
                float dashP = WindGliderRenderer.smoothStep01(t / 0.22F);
                model.body.xRot = Mth.lerp(w, model.body.xRot, 0.28F * dashP);
                mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(dashP, -1.20F, 0.85F));
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * 0.28F * dashP);
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.26F * dashP);
                offPart.xRot = Mth.lerp(w, offPart.xRot, -0.55F * dashP);
            } else {
                // Post-dash pose matching Tsaritsa spear stance: arm angled back, torso leaned 10 deg forward
                model.body.xRot = Mth.lerp(w, model.body.xRot, 0.175F);
                mainPart.xRot = Mth.lerp(w, mainPart.xRot, 0.85F);
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * 0.28F);
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.26F);
                offPart.xRot = Mth.lerp(w, offPart.xRot, -0.45F);
                offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * 0.25F);
                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.32F);
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.28F);
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
            if (t < 0.625F) {
                float reach = WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.35F));
                poseStack.translate(-side * 0.10F * reach, -0.04F * reach, 0.04F * reach);
                poseStack.mulPose(Axis.XP.rotationDegrees(-152.0F * reach));
            } else if (t < 0.8125F) {
                float drawOut = WindGliderRenderer.smoothStep01((t - 0.625F) / 0.1875F);
                poseStack.translate(-side * Mth.lerp(drawOut, 0.10F, 0.0F), Mth.lerp(drawOut, -0.04F, 0.04F), Mth.lerp(drawOut, 0.04F, -0.08F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(drawOut, -152.0F, -45.0F)));
            } else {
                // Keep sword rigidly along the exact same axis as the sweeping arm (no direction flip)
                poseStack.translate(0.0F, 0.04F, -0.08F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
            }
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_SLASH && data != null) {
            poseStack.translate(0.0F, 0.06F, -0.10F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-74.0F));
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_CA && data != null) {
            // Reverse-grip spear collinear with the arm axis, matching Tsaritsa's spear stance
            poseStack.translate(0.0F, -0.04F, 0.09F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
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
                float slash = WindGliderRenderer.smoothStep01((t - 0.72F) / 0.18F);
                float w = (t < 0.90F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.90F) / 0.10F) * (float) Math.PI));
                poseStack.translate(dir * Mth.lerp(slash, -0.75F, 0.28F) * w, 0.04F * w, -0.36F * w);
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(slash, 48.0F, -52.0F) * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(-82.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 86.0F * w));
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
            poseStack.scale(0.0F, 0.0F, 0.0F);
            return true;
        }
        if (false && activeAnim == ClientAuraTracker.ANIM_RAIDEN_CA && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.76F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.76F) / 0.24F) * (float) Math.PI));
            if (t < 0.24F) {
                float wind = WindGliderRenderer.smoothStep01(t / 0.24F);
                float tremor = Mth.sin((player.tickCount + partialTicks) * 3.2F) * 0.006F * wind;
                poseStack.translate(dir * 0.26F * wind * w + tremor, -0.30F * wind * w, -0.16F * wind * w);
                poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 18.0F * wind * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F * wind * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 96.0F * wind * w));
            } else {
                float rawSweep = Mth.clamp((t - 0.24F) / 0.52F, 0.0F, 1.0F);
                float sweep = 1.0F - (1.0F - rawSweep) * (1.0F - rawSweep) * (1.0F - rawSweep); // Cubic ease-out upward crescent whip
                float arcLift = Mth.sin(sweep * (float) Math.PI) * 0.10F;
                poseStack.translate(dir * Mth.lerp(sweep, 0.26F, -0.42F) * w, (Mth.lerp(sweep, -0.30F, 0.34F) + arcLift) * w, Mth.lerp(sweep, -0.16F, -0.34F) * w);
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(sweep, -18.0F, 28.0F) * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(sweep, -18.0F, -76.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(sweep, 96.0F, -84.0F) * w));
            }
            return true;
        }
        return true;
    }

    private static float stepIdleFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.raidenIdleLastMs <= 0L) {
            d.raidenIdleLastMs = nowMs;
            if (d.raidenIdle) {
                d.raidenIdleFade = Math.min(1.0F, d.raidenIdleFade + 0.016F / 0.45F);
            }
        } else if (nowMs > d.raidenIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.raidenIdleLastMs) / 1000.0F);
            d.raidenIdleLastMs = nowMs;
            if (d.raidenIdle) {
                d.raidenIdleFade = Math.min(1.0F, d.raidenIdleFade + dt / 1.60F);
            } else {
                d.raidenIdleFade = Math.max(0.0F, d.raidenIdleFade - dt / 0.45F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.raidenIdleFade);
    }

    public static float getLevitationCameraOffsetY(net.minecraft.world.entity.Entity entity, float partialTick) {
        if (!(entity instanceof LivingEntity le) || le.isUsingItem() || le.swinging) return 0.0F;
        if (!ClientAuraTracker.isRaidenIdle(le.getId())) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(le.getId());
        if (d == null) return 0.0F;
        stepIdleFade(d, System.currentTimeMillis());
        float liftProg = WindGliderRenderer.smoothStep01(d.raidenIdleFade / 0.55F);
        if (liftProg <= 0.01F) return 0.0F;
        float hover = Mth.sin((le.tickCount + partialTick) * 0.10F);
        return (0.72F + hover * 0.053F) * liftProg;
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (entity.isUsingItem() || entity.swinging) {
            ClientAuraTracker.AuraDisplayData dAct = ClientAuraTracker.get(entity.getId());
            if (dAct != null) {
                dAct.raidenIdle = false;
                dAct.raidenIdleFade = 0.0F;
            }
            return false;
        }
        if (!ClientAuraTracker.isRaidenIdle(entity.getId())) return false;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        stepIdleFade(d, System.currentTimeMillis());
        float w = (d != null) ? WindGliderRenderer.smoothStep01(d.raidenIdleFade / 0.55F) : 0.0F;
        if (w <= 0.01F) return false;

        // Levitating gently up and down above the giant Magatsu Mitake Narukami no Mikoto hands
        float hover = Mth.sin(ageInTicks * 0.10F);
        float liftY = -11.5F * w + hover * 0.85F * w;
        model.head.y += liftY;
        model.hat.y += liftY;
        model.body.y += liftY;
        model.rightArm.y += liftY;
        model.leftArm.y += liftY;
        model.rightLeg.y += liftY;
        model.leftLeg.y += liftY;

        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.05F + hover * 0.04F);
        model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.36F);
        model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.28F);
        model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, 0.14F - hover * 0.03F);
        model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.12F);
        model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.24F);
        model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.10F);
        model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.06F);
        model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.38F + hover * 0.05F);
        model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.08F);
        model.head.xRot = Mth.lerp(w, model.head.xRot, 0.06F);
        model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.45F, 0.45F), -0.10F);
        model.hat.copyFrom(model.head);
        return true;
    }

    private static void drawGauntletHand(
            VertexConsumer solidVc,
            PoseStack poseStack,
            float side,
            float time
    ) {
        poseStack.pushPose();
        poseStack.scale(1.42F, 1.42F, 1.42F);
        Matrix4f mat = poseStack.last().pose();
        // Detailed layered wrist cuff with gold/violet armor plates
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.16F, -0.060F, -0.28F, 0.16F, 0.060F, -0.14F, 112, 78, 152, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.17F, -0.052F, -0.16F, 0.17F, 0.052F, -0.12F, 222, 182, 122, 255);
        // Sculpted ivory-bronze gauntlet palm + outer knuckle ridge
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.20F, -0.048F, -0.12F, 0.20F, 0.048F, 0.17F, 186, 158, 128, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.21F, -0.056F, 0.07F, 0.21F, 0.056F, 0.18F, 134, 86, 182, 255);
        // Glowing Electro Eye sigil on the palm (outer ring + bright inner core)
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.092F, 0.040F, -0.055F, 0.092F, 0.056F, 0.075F, 158, 62, 228, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.062F, 0.052F, -0.035F, 0.062F, 0.064F, 0.055F, 215, 115, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.028F, 0.062F, -0.014F, 0.028F, 0.072F, 0.032F, 255, 245, 255, 255);

        // 4 thicker 3-segment articulated armored fingers curling upward
        for (int f = 0; f < 4; f++) {
            float fx = -0.142F + f * 0.095F;
            float fLen = (f == 1 || f == 2) ? 0.165F : 0.135F;
            poseStack.pushPose();
            poseStack.translate(fx, 0.0F, 0.17F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-16.0F - Mth.sin(time * 0.08F + f) * 4.0F));
            Matrix4f fMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.035F, -0.032F, 0.0F, 0.035F, 0.032F, fLen, 190, 164, 134, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.038F, -0.020F, fLen - 0.03F, 0.038F, 0.036F, fLen + 0.01F, 132, 86, 178, 255);
            poseStack.translate(0.0F, 0.0F, fLen);
            poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
            Matrix4f fMat2 = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat2, -0.029F, -0.026F, 0.0F, 0.029F, 0.026F, fLen * 0.82F, 216, 192, 166, 255);
            poseStack.popPose();
        }
        // Articulated 2-segment thumb on inner side
        poseStack.pushPose();
        poseStack.translate(-side * 0.20F, 0.01F, 0.02F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 38.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
        Matrix4f tMat = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, tMat, -0.036F, -0.032F, 0.0F, 0.036F, 0.032F, 0.14F, 190, 164, 134, 255);
        poseStack.translate(0.0F, 0.0F, 0.14F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, poseStack.last().pose(), -0.030F, -0.026F, 0.0F, 0.030F, 0.026F, 0.11F, 216, 192, 166, 255);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void drawPuppetArm(
            VertexConsumer solidVc,
            PoseStack poseStack,
            float scale,
            float elbowDeg,
            boolean openHand
    ) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        Matrix4f m0 = poseStack.last().pose();
        if (!openHand) {
            // Muscular Giant Right Mech Arm: flared spiked shoulder pauldron + 3 trailing Mitsudomoe plates + deltoid orb
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.15F, -0.11F, -0.15F, 0.15F, 0.13F, 0.15F, 152, 92, 218, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.17F, 0.04F, -0.17F, 0.17F, 0.17F, 0.17F, 182, 128, 242, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.065F, 0.16F, -0.065F, 0.065F, 0.34F, 0.065F, 225, 178, 255, 255);
            // Muscular deltoid & sculpted bicep with glowing Electro core
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.118F, -0.36F, -0.115F, 0.118F, -0.04F, 0.115F, 196, 166, 238, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.132F, -0.24F, -0.095F, 0.132F, -0.08F, 0.095F, 218, 192, 250, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.142F, -0.21F, -0.065F, 0.142F, -0.10F, 0.065F, 245, 195, 255, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.148F, -0.18F, -0.035F, 0.148F, -0.13F, 0.035F, 255, 242, 255, 255);
        } else {
            // Left Puppet Arm: pointed diamond shoulder cap + muscular tapered upper arm
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.095F, -0.08F, -0.095F, 0.095F, 0.09F, 0.095F, 152, 98, 218, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.045F, 0.09F, -0.045F, 0.045F, 0.21F, 0.045F, 218, 172, 255, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.082F, -0.36F, -0.082F, 0.082F, -0.05F, 0.082F, 196, 168, 236, 255);
        }
        // Armored elbow joint orb + outer spur
        poseStack.translate(0.0F, -0.36F, 0.0F);
        Matrix4f me = poseStack.last().pose();
        float eRad = openHand ? 0.082F : 0.112F;
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, me, -eRad, -0.065F, -eRad, eRad, 0.065F, eRad, 225, 155, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, me, -eRad * 0.65F, -0.045F, -eRad - 0.025F, eRad * 0.65F, 0.045F, eRad + 0.025F, 255, 228, 255, 255);
        // Muscular forearm bent inward/forward
        poseStack.mulPose(Axis.XP.rotationDegrees(elbowDeg));
        Matrix4f m1 = poseStack.last().pose();
        float fa = openHand ? 0.084F : 0.108F;
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m1, -fa, -0.34F, -fa, fa, 0.0F, fa, 212, 186, 246, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m1, -fa * 1.12F, -0.22F, -fa * 0.85F, fa * 1.12F, -0.06F, fa * 0.85F, 232, 208, 255, 255);
        // Deep violet armored wrist cuff
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m1, -fa * 1.14F, -0.30F, -fa * 1.14F, fa * 1.14F, -0.22F, fa * 1.14F, 128, 62, 208, 255);
        // Articulated hand
        poseStack.translate(0.0F, -0.35F, 0.0F);
        Matrix4f mh = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mh, -0.082F, -0.095F, -0.048F, 0.082F, 0.02F, 0.048F, 228, 205, 252, 255);
        if (openHand) {
            for (int f = -2; f <= 2; f++) {
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mh, f * 0.030F - 0.012F, -0.19F, -0.016F, f * 0.030F + 0.012F, -0.095F, 0.016F, 242, 226, 255, 255);
            }
        } else {
            // 4 articulated clenched fingers + thumb gripping the sword hilt
            for (int f = 0; f < 4; f++) {
                float fx = -0.056F + f * 0.037F;
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mh, fx - 0.014F, -0.125F, -0.062F, fx + 0.014F, -0.025F, 0.062F, 242, 222, 255, 255);
            }
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mh, -0.095F, -0.085F, -0.055F, -0.055F, -0.015F, 0.055F, 236, 212, 255, 255);
        }
        poseStack.popPose();
    }

    public static void renderIdleEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        if (entity.isUsingItem() || entity.swinging) {
            ClientAuraTracker.AuraDisplayData dAct = ClientAuraTracker.get(id);
            if (dAct != null) {
                dAct.raidenIdle = false;
                dAct.raidenIdleFade = 0.0F;
            }
            return;
        }
        if (!ClientAuraTracker.isRaidenIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        long nowMs = System.currentTimeMillis();
        stepIdleFade(d, nowMs);
        // Robot construct gradually materializes after Ei rises into the air
        float fade = (d != null) ? WindGliderRenderer.smoothStep01(d.raidenIdleFade / 0.55F) : 0.0F;
        if (fade <= 0.01F) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float hover = Mth.sin(time * 0.10F) * 0.055F;

        if (isFirstPersonSelf) {
            // In 1st-person, hilt is on the RIGHT (+X) gripped by her right mech arm, blade sweeps down-left (-X)
            VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
            poseStack.pushPose();
            poseStack.translate(-0.05F, -0.44F + hover * 0.4F, -0.82F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-22.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-12.0F));
            poseStack.scale(fade * 0.62F, fade * 0.62F, fade * 0.62F);
            Matrix4f bMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -1.15F, -0.038F, -0.038F, -0.58F, 0.038F, 0.038F, 95, 38, 145, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.60F, -0.11F, -0.045F, -0.52F, 0.11F, 0.045F, 215, 115, 255, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.52F, -0.048F, -0.018F, 1.10F, 0.048F, 0.018F, 175, 72, 245, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.50F, -0.022F, -0.024F, 1.18F, 0.022F, 0.024F, 255, 232, 255, 255);
            // Robot mech forearm & clenched gauntlet fist gripping the hilt at x = -0.86F
            poseStack.pushPose();
            poseStack.translate(-0.86F, 0.02F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F));
            Matrix4f fMat = poseStack.last().pose();
            // Clenched fist & 4 wrapped fingers over the hilt
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.11F, -0.075F, -0.070F, 0.11F, 0.095F, 0.070F, 212, 188, 245, 255);
            for (int f = 0; f < 4; f++) {
                float fx = -0.085F + f * 0.056F;
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, fx - 0.020F, -0.092F, -0.078F, fx + 0.020F, 0.035F, 0.078F, 235, 214, 255, 255);
            }
            // Violet wrist cuff & muscular armored mech forearm extending up-right
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.122F, 0.095F, -0.092F, 0.122F, 0.175F, 0.092F, 128, 62, 208, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.112F, 0.175F, -0.086F, 0.112F, 0.58F, 0.086F, 206, 180, 244, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.128F, 0.28F, -0.075F, 0.128F, 0.48F, 0.075F, 228, 204, 252, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.075F, 0.32F, -0.098F, 0.075F, 0.44F, 0.098F, 242, 182, 255, 255);
            // Elbow joint orb & upper arm + shoulder pauldron extending back to connect behind the camera
            poseStack.translate(0.0F, 0.60F, 0.0F);
            Matrix4f eMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, eMat, -0.115F, -0.075F, -0.115F, 0.115F, 0.075F, 0.115F, 225, 155, 255, 255);
            poseStack.mulPose(Axis.XP.rotationDegrees(-46.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F));
            Matrix4f uMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, uMat, -0.125F, 0.02F, -0.120F, 0.125F, 0.58F, 0.120F, 196, 166, 238, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, uMat, -0.145F, 0.20F, -0.095F, 0.145F, 0.44F, 0.095F, 235, 185, 255, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, uMat, -0.165F, 0.54F, -0.165F, 0.165F, 0.78F, 0.165F, 152, 92, 218, 255);
            poseStack.popPose();
            poseStack.popPose();
            return;
        }

        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.scale(fade, fade, fade);

        // PASS 1: Solid 3D Magatsu Mitake Narukami no Mikoto armor, giant hands, puppet arms, and Odachi blade
        VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());

        // 1A. Twin Giant Gauntlet Hands (swapped sides & +15 deg wider outward each side)
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.74F, 0.42F + hover * 0.45F, 0.16F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 49.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 12.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
            poseStack.scale(1.18F, 1.18F, 1.18F);
            drawGauntletHand(solidVc, poseStack, -side, time);
            poseStack.popPose();
        }

        // 1B. Giant Right-Side Mech Arm (-X) + seamless shoulder bridge & 3 Mitsudomoe drum plates connecting to Ei's back
        poseStack.pushPose();
        poseStack.translate(-0.78F, 2.22F + hover * 0.7F, 0.14F);
        poseStack.mulPose(Axis.YP.rotationDegrees(32.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-4.0F));
        Matrix4f bridgeMat = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bridgeMat, -0.46F, -0.12F, -0.13F, 0.42F, 0.12F, 0.13F, 152, 92, 218, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bridgeMat, -0.38F, 0.08F, -0.10F, 0.32F, 0.20F, 0.10F, 212, 158, 255, 255);
        // 3 trailing Mitsudomoe Electro drum plates above shoulder bridge
        for (int p = 0; p < 3; p++) {
            float px = -0.10F + p * 0.22F;
            float py = 0.28F - p * 0.14F;
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bridgeMat, px - 0.08F, py - 0.08F, -0.04F, px + 0.08F, py + 0.08F, 0.04F, 185, 118, 245, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bridgeMat, px - 0.04F, py - 0.04F, -0.05F, px + 0.04F, py + 0.04F, 0.05F, 250, 215, 255, 255);
        }
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-1.12F, 2.24F + hover * 0.7F, 0.36F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-6.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.0F));
        drawPuppetArm(solidVc, poseStack, 1.65F, -42.0F, false);
        // 1C. Giant Smoothly Curved Electro Odachi Katana aligned strictly along the clenched mech fist grip axis
        poseStack.translate(0.0F, -0.36F * 1.65F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-42.0F));
        poseStack.translate(0.0F, -0.425F * 1.65F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
        poseStack.translate(0.96F, 0.0F, 0.0F);
        Matrix4f swordMat = poseStack.last().pose();
        // Hilt (tsuka) + ornate pommel + 5 diamond tsuka-ito wraps
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -1.28F, -0.044F, -0.044F, -0.64F, 0.044F, 0.044F, 88, 32, 138, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -1.38F, -0.062F, -0.062F, -1.28F, 0.062F, 0.062F, 225, 165, 255, 255);
        for (int w = 0; w < 5; w++) {
            float wx = -1.20F + w * 0.115F;
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, wx, -0.050F, -0.050F, wx + 0.045F, 0.050F, 0.050F, 198, 105, 255, 255);
        }
        // Ornate Electro Mitsudomoe tsuba guard + glowing core
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -0.66F, -0.16F, -0.064F, -0.56F, 0.16F, 0.064F, 185, 78, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -0.64F, -0.105F, -0.078F, -0.58F, 0.105F, 0.078F, 245, 195, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -0.62F, -0.045F, -0.088F, -0.60F, 0.045F, 0.088F, 255, 245, 255, 255);
        // Smooth 12-segment curved glowing Odachi blade with Electro lightning motifs
        poseStack.pushPose();
        poseStack.translate(-0.56F, 0.0F, 0.0F);
        int bladeSegs = 12;
        float segLen = 0.165F;
        for (int bs = 0; bs < bladeSegs; bs++) {
            float frac = bs / (float) (bladeSegs - 1);
            float halfH = Mth.lerp(frac, 0.056F, 0.018F);
            float coreH = Mth.lerp(frac, 0.024F, 0.010F);
            float halfZ = Mth.lerp(frac, 0.018F, 0.010F);
            Matrix4f bMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, 0.0F, -halfH, -halfZ, segLen + 0.008F, halfH, halfZ, 175, 68, 248, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, 0.0F, -coreH, -(halfZ + 0.005F), segLen + 0.010F, coreH, halfZ + 0.005F, 255, 238, 255, 255);
            if (bs % 2 == 0 && bs < bladeSegs - 1) {
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, 0.03F, -halfH - 0.006F, -(halfZ + 0.006F), 0.11F, -coreH, halfZ + 0.006F, 235, 165, 255, 255);
            }
            poseStack.translate(segLen, 0.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(1.9F + frac * 0.9F));
        }
        // Sharp pointed katana tip (kissaki) where upper and lower blade edges meet at a point
        Matrix4f tipMat = poseStack.last().pose();
        float tipLen = 0.22F;
        float th = 0.018F, ch = 0.010F, tz = 0.010F, cz = 0.015F;
        float tipY = 0.012F;
        // Outer violet blade tip (front, back, top, bottom)
        ClientAuraRenderer.emitCrystalQuad(solidVc, tipMat, 0.0F, -th,  tz, 175, 68, 248, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255, 0.0F,  th,  tz, 175, 68, 248, 255);
        ClientAuraRenderer.emitCrystalQuad(solidVc, tipMat, 0.0F,  th, -tz, 175, 68, 248, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255, 0.0F, -th, -tz, 175, 68, 248, 255);
        ClientAuraRenderer.emitCrystalQuad(solidVc, tipMat, 0.0F,  th, -tz, 175, 68, 248, 255, 0.0F,  th,  tz, 175, 68, 248, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255);
        ClientAuraRenderer.emitCrystalQuad(solidVc, tipMat, 0.0F, -th,  tz, 175, 68, 248, 255, 0.0F, -th, -tz, 175, 68, 248, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255, tipLen, tipY, 0.0F, 215, 130, 255, 255);
        // Bright white-lavender core tip meeting at the same sharp point
        ClientAuraRenderer.emitCrystalQuad(solidVc, tipMat, 0.0F, -ch,  cz, 255, 238, 255, 255, tipLen * 1.04F, tipY, 0.0F, 255, 248, 255, 255, tipLen * 1.04F, tipY, 0.0F, 255, 248, 255, 255, 0.0F,  ch,  cz, 255, 238, 255, 255);
        ClientAuraRenderer.emitCrystalQuad(solidVc, tipMat, 0.0F,  ch, -cz, 255, 238, 255, 255, tipLen * 1.04F, tipY, 0.0F, 255, 248, 255, 255, tipLen * 1.04F, tipY, 0.0F, 255, 248, 255, 255, 0.0F, -ch, -cz, 255, 238, 255, 255);
        poseStack.popPose();
        poseStack.popPose();

        // 1D. Three Left-Side Puppet Arms (+X): Top angled upward facing front-up, Middle gripping Electro Vajra spear, Bottom open palm
        float[][] leftArms = new float[][]{
            {1.12F, 2.66F, -0.06F,  54.0F, -62.0F, 0.94F, -26.0F},
            {1.28F, 1.94F, -0.10F,  94.0F, -52.0F, 0.94F, -10.0F},
            {1.14F, 1.30F, -0.04F, 118.0F, -48.0F, 0.98F,  12.0F}
        };
        for (int i = 0; i < leftArms.length; i++) {
            float[] la = leftArms[i];
            float armBob = Mth.sin(time * 0.12F + i * 1.4F) * 0.045F;
            poseStack.pushPose();
            poseStack.translate(la[0], la[1] + hover * 0.6F + armBob, la[2]);
            poseStack.mulPose(Axis.ZP.rotationDegrees(la[3]));
            poseStack.mulPose(Axis.XP.rotationDegrees(la[6]));
            poseStack.mulPose(Axis.YP.rotationDegrees(-22.0F));
            drawPuppetArm(solidVc, poseStack, la[5], la[4], i != 1);
            poseStack.popPose();
        }
        // Smooth Triangular Double-ended Electro Vajra Dagger gripped directly inside the middle left puppet arm's fist
        poseStack.pushPose();
        poseStack.translate(1.78F, 1.88F + hover * 0.6F + Mth.sin(time * 0.12F + 1.4F) * 0.045F, 0.34F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(16.0F));
        Matrix4f vMat = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, vMat, -0.026F, -0.10F, -0.026F, 0.026F, 0.10F, 0.026F, 132, 62, 212, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, vMat, -0.042F, -0.035F, -0.042F, 0.042F, 0.035F, 0.042F, 242, 200, 255, 255);
        for (int end : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(0.0F, end * 0.10F, 0.0F);
            if (end < 0) poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            Matrix4f pMat = poseStack.last().pose();
            float bw = 0.072F, tipH = 0.28F;
            ClientAuraRenderer.emitCrystalQuad(solidVc, pMat, -bw, 0.0F,  bw, 195, 95, 255, 255,  bw, 0.0F,  bw, 195, 95, 255, 255, 0.0F, tipH, 0.0F, 195, 95, 255, 255, 0.0F, tipH, 0.0F, 195, 95, 255, 255);
            ClientAuraRenderer.emitCrystalQuad(solidVc, pMat,  bw, 0.0F, -bw, 195, 95, 255, 255, -bw, 0.0F, -bw, 195, 95, 255, 255, 0.0F, tipH, 0.0F, 195, 95, 255, 255, 0.0F, tipH, 0.0F, 195, 95, 255, 255);
            ClientAuraRenderer.emitCrystalQuad(solidVc, pMat, -bw, 0.0F, -bw, 235, 165, 255, 255, -bw, 0.0F,  bw, 235, 165, 255, 255, 0.0F, tipH, 0.0F, 235, 165, 255, 255, 0.0F, tipH, 0.0F, 235, 165, 255, 255);
            ClientAuraRenderer.emitCrystalQuad(solidVc, pMat,  bw, 0.0F,  bw, 235, 165, 255, 255,  bw, 0.0F, -bw, 235, 165, 255, 255, 0.0F, tipH, 0.0F, 235, 165, 255, 255, 0.0F, tipH, 0.0F, 235, 165, 255, 255);
            poseStack.popPose();
        }
        poseStack.popPose();

        // PASS 2: Smooth Open-Top Crescent Electro Halo Ring, 6 Growing Orbs & 45-deg Electro Icon behind head
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.02F + hover * 0.75F, -0.52F);
        Matrix4f haloMat = poseStack.last().pose();
        int aHalo = Math.max(20, Math.round(215.0F * fade));
        int aCore = Math.max(25, Math.round(252.0F * fade));
        int segsPerSide = 28;
        float baseR = 0.94F;
        for (int side : new int[]{-1, 1}) {
            float[] px = new float[segsPerSide + 1];
            float[] py = new float[segsPerSide + 1];
            float[] hw = new float[segsPerSide + 1];
            for (int i = 0; i <= segsPerSide; i++) {
                float s = i / (float) segsPerSide; // 0 = bottom (-PI/2), 1 = sharp upward tip
                float ang = (float) (-Math.PI * 0.5D + s * (Math.PI * 0.88D));
                float flareIn = (s > 0.65F) ? WindGliderRenderer.smoothStep01((s - 0.65F) / 0.35F) : 0.0F;
                float r = baseR + 0.06F * Mth.sin(s * (float) Math.PI * 0.7F) - 0.14F * flareIn;
                px[i] = side * Mth.cos(ang) * r;
                py[i] = Mth.sin(ang) * r + flareIn * 0.34F;
                hw[i] = (s < 0.72F) ? 0.075F : 0.075F * (1.0F - WindGliderRenderer.smoothStep01((s - 0.72F) / 0.28F));
            }
            for (int i = 0; i < segsPerSide; i++) {
                float dx = px[i + 1] - px[i];
                float dy = py[i + 1] - py[i];
                float len = Mth.sqrt(dx * dx + dy * dy);
                if (len < 1.0E-5F) continue;
                float nx = -dy / len;
                float ny = dx / len;
                float x0i = px[i] - nx * hw[i], y0i = py[i] - ny * hw[i];
                float x0o = px[i] + nx * hw[i], y0o = py[i] + ny * hw[i];
                float x1o = px[i + 1] + nx * hw[i + 1], y1o = py[i + 1] + ny * hw[i + 1];
                float x1i = px[i + 1] - nx * hw[i + 1], y1i = py[i + 1] - ny * hw[i + 1];
                WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                        x0i, y0i, 0.0F, x0o, y0o, 0.0F, x1o, y1o, 0.0F, x1i, y1i, 0.0F,
                        110, 40, 172, aHalo);
                float c0 = hw[i] * 0.42F, c1 = hw[i + 1] * 0.42F;
                WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                        px[i] - nx * c0, py[i] - ny * c0, 0.004F,
                        px[i] + nx * c0, py[i] + ny * c0, 0.004F,
                        px[i + 1] + nx * c1, py[i + 1] + ny * c1, 0.004F,
                        px[i + 1] - nx * c1, py[i + 1] - ny * c1, 0.004F,
                        165, 136, 179, aCore);
            }
        }
        // 6 glowing orbs (3 per side), shifted cleanly outside the ring curve to prevent layer overlap
        float[] orbS = new float[]{ 0.34F, 0.56F, 0.80F };
        float[] orbRad = new float[]{ 0.042F, 0.064F, 0.088F };
        for (int side : new int[]{-1, 1}) {
            for (int b = 0; b < 3; b++) {
                float s = orbS[b];
                float ang = (float) (-Math.PI * 0.5D + s * (Math.PI * 0.88D));
                float flareIn = (s > 0.65F) ? WindGliderRenderer.smoothStep01((s - 0.65F) / 0.35F) : 0.0F;
                float r = baseR + 0.06F * Mth.sin(s * (float) Math.PI * 0.7F) - 0.14F * flareIn;
                float rad = orbRad[b];
                float outOffset = 0.115F + rad * 1.45F;
                float outX = side * Mth.cos(ang);
                float outY = Mth.sin(ang);
                float bx = outX * (r + outOffset);
                float by = outY * (r + outOffset) + flareIn * 0.34F;
                int oct = 8;
                for (int k = 0; k < oct; k++) {
                    float a0 = (float) (k * Math.PI * 2.0D / oct);
                    float a1 = (float) ((k + 1) * Math.PI * 2.0D / oct);
                    WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                            bx, by, 0.022F,
                            bx + Mth.cos(a0) * rad * 1.45F, by + Mth.sin(a0) * rad * 1.45F, 0.022F,
                            bx + Mth.cos(a1) * rad * 1.45F, by + Mth.sin(a1) * rad * 1.45F, 0.022F,
                            bx, by, 0.022F,
                            136, 74, 179, Math.max(20, Math.round(170.0F * fade)));
                    WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                            bx, by, 0.028F,
                            bx + Mth.cos(a0) * rad * 0.85F, by + Mth.sin(a0) * rad * 0.85F, 0.028F,
                            bx + Mth.cos(a1) * rad * 0.85F, by + Mth.sin(a1) * rad * 0.85F, 0.028F,
                            bx, by, 0.028F,
                            179, 168, 179, aCore);
                }
            }
        }
        poseStack.popPose();

        poseStack.popPose();
    }
}
