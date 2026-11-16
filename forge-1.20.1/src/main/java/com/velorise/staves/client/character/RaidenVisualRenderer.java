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
        ClientAuraTracker.localRaidenAttackHeld = false;
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
        float liftOffset = getLevitationCameraOffsetY(entity, pt);
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float bodyYaw = (d != null && d.raidenIdleYawSet) ? d.raidenIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        boolean holdingRaiden = ArcaneStaffItem.getEffectiveKind(entity.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                || ArcaneStaffItem.getEffectiveKind(entity.getOffhandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (holdingRaiden && (resolve > 0 || musouTicks > 0.0F || eyeTicks > 0.0F || isIdle)) {
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
                // Task 6: Illuminate only individual Chakra Desiderata Resolve beads/dots along the ring circumference (no neon arc ribbon)
                VertexConsumer glowVc = buffer.getBuffer(RenderType.lightning());
                float rBeadRing = half * 0.742F;
                int beadsPerSide = 9; // 18 beads total symmetrically distributed from bottom (-90 deg) to top (+90 deg)
                float litProgress = resolveFrac * beadsPerSide;
                int haloR = state.netherite ? 215 : 191;
                int haloG = state.netherite ? 130 : 90;
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
        boolean isMusouOrCa = (arc.color == 0xFFE0B0FF);
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float fade = (prog < 0.22F) ? (prog / 0.22F) : (1.0F - (prog - 0.22F) / 0.78F);
        int alpha = Mth.clamp((int) (255.0F * fade), 10, 255);
        Vec3 sCam = startPos.subtract(camPos);
        Vec3 eCam = endPos.subtract(camPos);
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
            double maxHalfWidth = (isMusouOrCa ? 0.30D : 0.16D) * (1.08D - prog * 0.32D);

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
                float slash = WindGliderRenderer.smoothStep01((t - 0.8125F) / 0.1875F);
                model.body.xRot = Mth.lerp(slash, 0.0F, 0.18F);
                model.body.yRot = castDir * Mth.lerp(slash, -0.45F, 0.55F);
                mainPart.xRot = Mth.lerp(slash, -2.2F, -1.48F);
                mainPart.yRot = castDir * Mth.lerp(slash, 0.85F, -1.25F);
                mainPart.zRot = castDir * Mth.lerp(slash, -0.35F, 1.15F);
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
            // Task 14: Authentic Inazuma Upward Crescent Moon Slash / Spatial Fan Sweep
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;
            float w = (t < 0.76F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.76F) / 0.24F) * (float) Math.PI));
            if (t < 0.24F) {
                float wind = WindGliderRenderer.smoothStep01(t / 0.24F);
                model.body.xRot = Mth.lerp(w, model.body.xRot, 0.26F * wind);
                model.body.yRot = Mth.lerp(w, model.body.yRot, -castDir * 0.48F * wind);
                model.body.zRot = Mth.lerp(w, model.body.zRot, -castDir * 0.08F * wind);
                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.42F * wind);
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.36F * wind);
                model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.14F * wind);
                model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.12F * wind);

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, -0.18F * wind);
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * -1.05F * wind);
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * -0.62F * wind);
                offPart.xRot = Mth.lerp(w, offPart.xRot, -0.58F * wind);
                offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * 0.52F * wind);
                offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * -0.22F * wind);
                model.head.xRot += 0.12F * wind * w;
                model.hat.copyFrom(model.head);
            } else {
                float rawSweep = Mth.clamp((t - 0.24F) / 0.52F, 0.0F, 1.0F);
                float sweep = 1.0F - (1.0F - rawSweep) * (1.0F - rawSweep) * (1.0F - rawSweep); // Cubic ease-out crescent launch
                model.body.xRot = Mth.lerp(w, model.body.xRot, Mth.lerp(sweep, 0.26F, -0.14F));
                model.body.yRot = Mth.lerp(w, model.body.yRot, castDir * Mth.lerp(sweep, -0.48F, 0.54F));
                model.body.zRot = Mth.lerp(w, model.body.zRot, castDir * Mth.lerp(sweep, -0.08F, 0.10F));
                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, Mth.lerp(sweep, 0.42F, -0.28F));
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, Mth.lerp(sweep, -0.36F, 0.46F));
                model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.14F * (1.0F - sweep * 0.5F));
                model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.12F * (1.0F - sweep * 0.5F));

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(sweep, -0.18F, -2.42F));
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(sweep, -1.05F, 0.58F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(sweep, -0.62F, 0.42F));
                offPart.xRot = Mth.lerp(w, offPart.xRot, Mth.lerp(sweep, -0.58F, 0.55F));
                offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * Mth.lerp(sweep, 0.52F, -0.42F));
                offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * Mth.lerp(sweep, -0.22F, 0.32F));
                model.head.xRot += Mth.lerp(sweep, 0.12F, -0.16F) * w;
                model.hat.copyFrom(model.head);
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
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_CA && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.76F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.76F) / 0.24F) * (float) Math.PI));
            if (t < 0.24F) {
                float wind = WindGliderRenderer.smoothStep01(t / 0.24F);
                poseStack.translate(side * 0.08F * wind * w, -0.06F * wind * w, 0.04F * wind * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(28.0F * wind * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 34.0F * wind * w));
            } else {
                float rawSweep = Mth.clamp((t - 0.24F) / 0.52F, 0.0F, 1.0F);
                float sweep = 1.0F - (1.0F - rawSweep) * (1.0F - rawSweep) * (1.0F - rawSweep);
                poseStack.translate(side * Mth.lerp(sweep, 0.08F, -0.06F) * w, Mth.lerp(sweep, -0.06F, 0.08F) * w, -0.06F * sweep * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(sweep, 28.0F, -48.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(sweep, 34.0F, -38.0F) * w));
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
                float slash = WindGliderRenderer.smoothStep01((t - 0.72F) / 0.18F);
                float w = (t < 0.90F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.90F) / 0.10F) * (float) Math.PI));
                poseStack.translate(dir * Mth.lerp(slash, -0.90F, 1.25F) * w, 0.04F * w, -0.35F * w);
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(slash, -78.0F, 95.0F) * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(-88.0F * w));
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
        poseStack.scale(1.38F, 1.38F, 1.38F);
        Matrix4f mat = poseStack.last().pose();
        // Forearm wrist cuff with gold/violet armor plates
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.15F, -0.055F, -0.26F, 0.15F, 0.055F, -0.13F, 118, 86, 156, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.16F, -0.048F, -0.15F, 0.16F, 0.048F, -0.12F, 215, 175, 118, 255);
        // Sculpted ivory-bronze gauntlet palm
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.19F, -0.045F, -0.12F, 0.19F, 0.045F, 0.16F, 178, 152, 124, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.20F, -0.052F, 0.08F, 0.20F, 0.052F, 0.17F, 128, 82, 175, 255);
        // Glowing Electro Eye core on the palm
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.075F, 0.042F, -0.045F, 0.075F, 0.058F, 0.065F, 205, 95, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.032F, 0.056F, -0.015F, 0.032F, 0.066F, 0.035F, 255, 240, 255, 255);

        // 4 thicker articulated fingers curling upward
        for (int f = 0; f < 4; f++) {
            float fx = -0.138F + f * 0.092F;
            float fLen = (f == 1 || f == 2) ? 0.16F : 0.13F;
            poseStack.pushPose();
            poseStack.translate(fx, 0.0F, 0.16F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F - Mth.sin(time * 0.08F + f) * 4.0F));
            Matrix4f fMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.032F, -0.030F, 0.0F, 0.032F, 0.030F, fLen, 185, 160, 132, 255);
            poseStack.translate(0.0F, 0.0F, fLen);
            poseStack.mulPose(Axis.XP.rotationDegrees(-26.0F));
            Matrix4f fMat2 = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat2, -0.026F, -0.024F, 0.0F, 0.026F, 0.024F, fLen * 0.80F, 212, 188, 162, 255);
            poseStack.popPose();
        }
        // Thumb on inner side
        poseStack.pushPose();
        poseStack.translate(-side * 0.19F, 0.01F, 0.02F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 38.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
        Matrix4f tMat = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, tMat, -0.034F, -0.030F, 0.0F, 0.034F, 0.030F, 0.15F, 185, 160, 132, 255);
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
        // Ornate shoulder pauldron + spiked horn + glowing Electro core
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.11F, -0.09F, -0.11F, 0.11F, 0.11F, 0.11F, 148, 98, 212, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.05F, 0.11F, -0.05F, 0.05F, 0.22F, 0.05F, 215, 165, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.06F, -0.06F, -0.125F, 0.06F, 0.06F, 0.125F, 245, 195, 255, 255);
        // Sculpted upper arm
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m0, -0.072F, -0.36F, -0.072F, 0.072F, -0.05F, 0.072F, 192, 164, 232, 255);
        // Elbow joint orb
        poseStack.translate(0.0F, -0.36F, 0.0F);
        Matrix4f me = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, me, -0.075F, -0.055F, -0.075F, 0.075F, 0.055F, 0.075F, 225, 155, 255, 255);
        // Forearm bent inward/forward
        poseStack.mulPose(Axis.XP.rotationDegrees(elbowDeg));
        Matrix4f m1 = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m1, -0.078F, -0.34F, -0.078F, 0.078F, 0.0F, 0.078F, 208, 182, 244, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, m1, -0.088F, -0.28F, -0.088F, 0.088F, -0.21F, 0.088F, 132, 68, 212, 255);
        // Articulated hand
        poseStack.translate(0.0F, -0.35F, 0.0F);
        Matrix4f mh = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mh, -0.075F, -0.09F, -0.045F, 0.075F, 0.02F, 0.045F, 225, 202, 250, 255);
        if (openHand) {
            for (int f = -2; f <= 2; f++) {
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mh, f * 0.028F - 0.011F, -0.18F, -0.015F, f * 0.028F + 0.011F, -0.09F, 0.015F, 240, 225, 255, 255);
            }
        } else {
            // Clenched fist fingers gripping the sword hilt
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mh, -0.078F, -0.11F, -0.065F, 0.078F, -0.03F, 0.065F, 238, 218, 255, 255);
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
        float fade = (d != null) ? WindGliderRenderer.smoothStep01((d.raidenIdleFade - 0.42F) / 0.58F) : 0.0F;
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
            // Violet wrist cuff & armored mech forearm extending up-right
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.115F, 0.095F, -0.085F, 0.115F, 0.175F, 0.085F, 132, 68, 212, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.105F, 0.175F, -0.080F, 0.105F, 0.56F, 0.080F, 198, 172, 238, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.075F, 0.32F, -0.092F, 0.075F, 0.44F, 0.092F, 235, 165, 255, 255);
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

        // 1A. Twin Giant Gauntlet Hands raised clearly above the ground beneath Raiden's hovering feet
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.56F, 0.44F + hover * 0.45F, 0.12F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 24.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 10.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
            drawGauntletHand(solidVc, poseStack, side, time);
            poseStack.popPose();
        }

        // 1B. Giant Right-Side Mech Arm (-X) whose fist grips directly onto the Odachi sword hilt
        poseStack.pushPose();
        poseStack.translate(-1.26F, 2.24F + hover * 0.7F, -0.08F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(14.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(18.0F));
        drawPuppetArm(solidVc, poseStack, 1.65F, -42.0F, false);
        poseStack.popPose();

        // 1C. Giant Curved Electro Odachi Katana with hilt positioned inside the right mech fist
        poseStack.pushPose();
        poseStack.translate(-0.08F, 0.96F + hover * 0.65F, 0.34F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-24.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-6.0F));
        Matrix4f swordMat = poseStack.last().pose();
        // Hilt (tsuka) + pommel + diamond tsuka-ito accents
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -1.28F, -0.044F, -0.044F, -0.64F, 0.044F, 0.044F, 88, 32, 138, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -1.36F, -0.058F, -0.058F, -1.28F, 0.058F, 0.058F, 215, 145, 255, 255);
        for (int w = 0; w < 4; w++) {
            float wx = -1.18F + w * 0.14F;
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, wx, -0.048F, -0.048F, wx + 0.05F, 0.048F, 0.048F, 185, 90, 245, 255);
        }
        // Ornate Electro tsuba guard
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -0.66F, -0.14F, -0.058F, -0.56F, 0.14F, 0.058F, 195, 90, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -0.63F, -0.085F, -0.072F, -0.59F, 0.085F, 0.072F, 255, 232, 255, 255);
        // Multi-segment curved glowing Odachi blade
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -0.56F, -0.054F, -0.018F, 0.36F, 0.054F, 0.018F, 168, 62, 245, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, swordMat, -0.54F, -0.024F, -0.024F, 0.38F, 0.024F, 0.024F, 255, 235, 255, 255);
        poseStack.pushPose();
        poseStack.translate(0.36F, 0.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(7.0F));
        Matrix4f midMat = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, midMat, 0.0F, -0.048F, -0.017F, 0.56F, 0.048F, 0.017F, 178, 70, 248, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, midMat, 0.0F, -0.020F, -0.023F, 0.58F, 0.020F, 0.023F, 255, 240, 255, 255);
        poseStack.translate(0.56F, 0.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
        Matrix4f tipMat = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, tipMat, 0.0F, -0.036F, -0.014F, 0.44F, 0.036F, 0.014F, 195, 92, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, tipMat, 0.0F, -0.015F, -0.020F, 0.50F, 0.015F, 0.020F, 255, 248, 255, 255);
        poseStack.popPose();
        poseStack.popPose();

        // 1D. Three Horizontal/Angled Left-Side Puppet Arms (+X) & 2 Sharp Electro Vajra Blades
        float[][] leftArms = new float[][]{
            {1.15F, 2.52F, -0.10F,  78.0F, -55.0F, 0.95F},
            {1.28F, 1.92F, -0.14F,  96.0F, -48.0F, 0.92F},
            {1.12F, 1.32F, -0.08F, 118.0F, -52.0F, 0.98F}
        };
        for (int i = 0; i < leftArms.length; i++) {
            float[] la = leftArms[i];
            float armBob = Mth.sin(time * 0.12F + i * 1.4F) * 0.045F;
            poseStack.pushPose();
            poseStack.translate(la[0], la[1] + hover * 0.6F + armBob, la[2]);
            poseStack.mulPose(Axis.ZP.rotationDegrees(la[3]));
            poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F));
            drawPuppetArm(solidVc, poseStack, la[5], la[4], true);
            poseStack.popPose();
        }
        // Two floating Electro Vajra/Kunai spikes between the left puppet arms
        for (int k = 0; k < 2; k++) {
            float ky = (k == 0 ? 2.22F : 1.62F) + hover * 0.6F + Mth.cos(time * 0.14F + k * 2.0F) * 0.04F;
            poseStack.pushPose();
            poseStack.translate(1.36F, ky, 0.08F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(k == 0 ? -18.0F : 18.0F));
            Matrix4f kMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, kMat, -0.032F, -0.22F, -0.032F, 0.032F, 0.22F, 0.032F, 175, 70, 245, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, kMat, -0.016F, -0.32F, -0.016F, 0.016F, 0.32F, 0.016F, 255, 232, 255, 255);
            poseStack.popPose();
        }

        // PASS 2: Translucent Giant Crescent Electro Halo Ring & Mitsudomoe Crest behind her back
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.16F + hover * 0.75F, -0.50F);
        Matrix4f haloMat = poseStack.last().pose();
        int aHalo = Math.max(20, Math.round(210.0F * fade));
        int aCore = Math.max(25, Math.round(252.0F * fade));
        float rIn = 0.88F, rOut = 1.05F;
        int segs = 24;
        for (int s = 0; s < segs; s++) {
            float a0 = (float) (-Math.PI * 0.22D + s * (Math.PI * 1.44D) / segs);
            float a1 = (float) (-Math.PI * 0.22D + (s + 1) * (Math.PI * 1.44D) / segs);
            WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                    Mth.cos(a0) * rIn,  Mth.sin(a0) * rIn,  0.0F,
                    Mth.cos(a0) * rOut, Mth.sin(a0) * rOut, 0.0F,
                    Mth.cos(a1) * rOut, Mth.sin(a1) * rOut, 0.0F,
                    Mth.cos(a1) * rIn,  Mth.sin(a1) * rIn,  0.0F,
                    158, 58, 245, aHalo);
        }
        // Upward-pointing crescent horns at the top of the halo
        for (int side : new int[]{-1, 1}) {
            WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                    side * 0.22F, 0.96F, 0.0F,
                    side * 0.36F, 0.92F, 0.0F,
                    side * 0.14F, 1.38F, 0.0F,
                    side * 0.12F, 1.38F, 0.0F,
                    215, 135, 255, aCore);
        }
        // 6 glowing Electro beads along the upper arc
        for (int b = 0; b < 6; b++) {
            float ang = (float) (Math.PI * 0.14D + b * (Math.PI * 0.72D) / 5.0D);
            float bx = Mth.cos(ang) * 0.96F;
            float by = Mth.sin(ang) * 0.96F;
            WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                    bx - 0.070F, by - 0.070F, 0.01F,
                    bx + 0.070F, by - 0.070F, 0.01F,
                    bx + 0.070F, by + 0.070F, 0.01F,
                    bx - 0.070F, by + 0.070F, 0.01F,
                    245, 205, 255, aCore);
        }
        // Mitsudomoe tri-crest above her head
        for (int m = 0; m < 3; m++) {
            float ang = (float) (time * 0.05D + m * Math.PI * 2.0D / 3.0D);
            float mx = Mth.cos(ang) * 0.18F;
            float my = 0.56F + Mth.sin(ang) * 0.18F;
            WindGliderRenderer.emitDoubleQuad(transVc, haloMat,
                    mx - 0.060F, my - 0.060F, 0.02F,
                    mx + 0.060F, my - 0.060F, 0.02F,
                    mx + 0.060F, my + 0.060F, 0.02F,
                    mx - 0.060F, my + 0.060F, 0.02F,
                    252, 228, 255, aCore);
        }
        poseStack.popPose();

        poseStack.popPose();
    }
}
