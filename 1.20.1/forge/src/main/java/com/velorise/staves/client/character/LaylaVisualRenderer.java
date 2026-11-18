package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated client-side VFX renderer for Layla (Night's Focus):
 * - Elemental Skill: Curtain of Slumber Fresnel UV-Sphere + 1-4 orbiting Night Star billboards
 * - Elemental Burst: World-anchored Celestial Dreamsphere (top Astrolabe Ring, Dreamsphere Orb, 4 hanging Night Stars)
 */
public final class LaylaVisualRenderer {
    private static final ResourceLocation LAYLA_DREAMSPHERE_TEX =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/entity/layla_dreamsphere.png");
    private static final ResourceLocation LAYLA_ASTROLABE_TEX =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/entity/layla_astrolabe_ring.png");
    private static final ResourceLocation LAYLA_NIGHT_STAR_TEX =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/entity/layla_night_star.png");

    private LaylaVisualRenderer() {}

    public static void renderWorldLaylaDreamspheres(PoseStack poseStack, MultiBufferSource buffer, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientAuraRenderer.isRenderingScreen() || mc.level == null) return;
        long now = System.currentTimeMillis();
        boolean anyRendered = false;

        for (ClientAuraTracker.AuraDisplayData d : ClientAuraTracker.getAllDisplayData()) {
            if (d == null || now >= d.laylaDreamsphereUntil || d.laylaDreamsphereAnchorPos == null) continue;
            anyRendered = true;
            float elapsedTicks = Math.max(0.0F, (now - d.laylaDreamsphereStartMs) / 50.0F);
            float time = elapsedTicks;
            float openProg = Mth.clamp(elapsedTicks / 8.0F, 0.0F, 1.0F);
            float spawnScale = Mth.lerp(WindGliderRenderer.smoothStep01(openProg), 0.10F, 1.00F);
            float remMs = (float) (d.laylaDreamsphereUntil - now);
            float fadeOut = remMs < 250.0F ? Mth.clamp(remMs / 250.0F, 0.0F, 1.0F) : 1.0F;

            float ringAngleDeg = time * 1.25F;
            float ringBob = 0.04F * Mth.sin(time * 0.08F);

            Vec3 rel = d.laylaDreamsphereAnchorPos.subtract(camPos);
            poseStack.pushPose();
            poseStack.translate(rel.x, rel.y, rel.z);
            poseStack.scale(spawnScale, spawnScale, spawnScale);

            float orbHalf = 0.45F;
            float pulse = 0.85F + 0.15F * (0.5F + 0.5F * Mth.sin(time * 0.14F));
            int orbCol = Math.min(255, Math.round(255.0F * pulse));
            int orbAlpha = Math.round(245.0F * fadeOut);
            poseStack.pushPose();
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 0.75F));
            VertexConsumer orbVc = buffer.getBuffer(RenderType.entityTranslucentEmissive(LAYLA_DREAMSPHERE_TEX));
            Matrix4f orbMat = poseStack.last().pose();
            orbVc.vertex(orbMat, -orbHalf, -orbHalf, 0.0F).color(orbCol, orbCol, 255, orbAlpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
            orbVc.vertex(orbMat,  orbHalf, -orbHalf, 0.0F).color(orbCol, orbCol, 255, orbAlpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
            orbVc.vertex(orbMat,  orbHalf,  orbHalf, 0.0F).color(orbCol, orbCol, 255, orbAlpha).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
            orbVc.vertex(orbMat, -orbHalf,  orbHalf, 0.0F).color(orbCol, orbCol, 255, orbAlpha).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
            poseStack.popPose();

            float ringR = 1.10F;
            float ringY = 0.85F + ringBob;
            int ringAlpha = Math.round(240.0F * fadeOut);
            poseStack.pushPose();
            poseStack.translate(0.0F, ringY, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(ringAngleDeg));
            VertexConsumer ringVc = buffer.getBuffer(RenderType.entityTranslucentEmissive(LAYLA_ASTROLABE_TEX));
            Matrix4f ringMat = poseStack.last().pose();
            for (float dy : new float[]{0.015F, -0.015F}) {
                float ny = dy > 0.0F ? 1.0F : -1.0F;
                ringVc.vertex(ringMat, -ringR, dy, -ringR).color(255, 245, 210, ringAlpha).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, ny, 0.0F).endVertex();
                ringVc.vertex(ringMat,  ringR, dy, -ringR).color(255, 245, 210, ringAlpha).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, ny, 0.0F).endVertex();
                ringVc.vertex(ringMat,  ringR, dy,  ringR).color(255, 245, 210, ringAlpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, ny, 0.0F).endVertex();
                ringVc.vertex(ringMat, -ringR, dy,  ringR).color(255, 245, 210, ringAlpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, ny, 0.0F).endVertex();
            }
            poseStack.popPose();

            float anchorR = 0.86F;
            VertexConsumer strVc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
            Matrix4f baseMat = poseStack.last().pose();
            int strAlpha = Math.round(155.0F * fadeOut);
            float[] starX = new float[4];
            float[] starY = new float[4];
            float[] starZ = new float[4];
            for (int idx = 0; idx < 4; idx++) {
                double angRad = Math.toRadians(ringAngleDeg + idx * 90.0F);
                float hx = (float) Math.cos(angRad) * anchorR;
                float hz = (float) Math.sin(angRad) * anchorR;
                float strLen = (idx % 2 == 0) ? 0.55F : 0.75F;
                float hyTop = ringY;
                float hyBot = ringY - strLen;
                starX[idx] = hx;
                starY[idx] = hyBot;
                starZ[idx] = hz;
                float sw = 0.010F;
                WindGliderRenderer.emitDoubleQuad(strVc, baseMat, hx - sw, hyBot, hz, hx + sw, hyBot, hz, hx + sw, hyTop, hz, hx - sw, hyTop, hz, 235, 230, 175, strAlpha);
                WindGliderRenderer.emitDoubleQuad(strVc, baseMat, hx, hyBot, hz - sw, hx, hyBot, hz + sw, hx, hyTop, hz + sw, hx, hyTop, hz - sw, 235, 230, 175, strAlpha);
            }

            VertexConsumer starVc = buffer.getBuffer(RenderType.entityTranslucentEmissive(LAYLA_NIGHT_STAR_TEX));
            float starHalf = 0.14F;
            int starAlpha = Math.round(250.0F * fadeOut);
            for (int idx = 0; idx < 4; idx++) {
                poseStack.pushPose();
                poseStack.translate(starX[idx], starY[idx], starZ[idx]);
                poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                Matrix4f sm = poseStack.last().pose();
                starVc.vertex(sm, -starHalf, -starHalf, 0.0F).color(240, 248, 255, starAlpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                starVc.vertex(sm,  starHalf, -starHalf, 0.0F).color(240, 248, 255, starAlpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                starVc.vertex(sm,  starHalf,  starHalf, 0.0F).color(240, 248, 255, starAlpha).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                starVc.vertex(sm, -starHalf,  starHalf, 0.0F).color(240, 248, 255, starAlpha).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        if (anyRendered && buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.entityTranslucentEmissive(LAYLA_DREAMSPHERE_TEX));
            bs.endBatch(RenderType.entityTranslucentEmissive(LAYLA_ASTROLABE_TEX));
            bs.endBatch(RenderType.entityTranslucentEmissive(LAYLA_NIGHT_STAR_TEX));
            bs.endBatch(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        }
    }

    public static void renderNightsFocusBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (ClientAuraRenderer.isRenderingScreen() || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        if (!ClientAuraTracker.hasNightsFocusShield(entity)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float h = entity.getBbHeight();
        float idleLift = ClientAuraRenderer.getTotalLevitationOffsetY(entity, pt);
        float centerY = h * 0.52F + idleLift;
        float pulse = 1.0F + 0.015F * Mth.sin(time * 0.15F);
        ClientAuraTracker.AuraDisplayData sd = ClientAuraTracker.get(entity.getId());
        long nowMs = System.currentTimeMillis();
        float openScale = (sd != null && sd.nightsFocusAppliedTime > 0L)
                ? Mth.lerp(WindGliderRenderer.smoothStep01(Mth.clamp((nowMs - sd.nightsFocusAppliedTime) / 250.0F, 0.0F, 1.0F)), 0.30F, 1.00F)
                : 1.00F;
        float shieldFade = (sd != null && sd.nightsFocusUntil > nowMs && (sd.nightsFocusUntil - nowMs) < 200L)
                ? Mth.clamp((sd.nightsFocusUntil - nowMs) / 200.0F, 0.0F, 1.0F)
                : 1.00F;
        float rSphere = 1.25F * pulse * openScale;

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 shieldCenter = entity.getPosition(pt).add(0.0D, centerY, 0.0D);
        Vec3 toCam = camPos.subtract(shieldCenter);
        double toCamLen = toCam.length();
        float vx = toCamLen > 1.0E-4D ? (float) (toCam.x / toCamLen) : 0.0F;
        float vy = toCamLen > 1.0E-4D ? (float) (toCam.y / toCamLen) : 0.0F;
        float vz = toCamLen > 1.0E-4D ? (float) (toCam.z / toCamLen) : 1.0F;

        poseStack.pushPose();
        poseStack.translate(0.0F, centerY, 0.0F);

        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        Matrix4f sphereMat = poseStack.last().pose();
        int stacks = 16;
        int slices = 24;
        for (int i = 0; i < stacks; i++) {
            float phi0 = (float) (-Math.PI * 0.5D + Math.PI * i / stacks);
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * (i + 1) / stacks);
            float ny0 = Mth.sin(phi0), ny1 = Mth.sin(phi1);
            float cr0 = Mth.cos(phi0), cr1 = Mth.cos(phi1);
            for (int j = 0; j < slices; j++) {
                float th0 = (float) (j * 2.0D * Math.PI / slices);
                float th1 = (float) ((j + 1) * 2.0D * Math.PI / slices);
                float c0 = Mth.cos(th0), s0 = Mth.sin(th0);
                float c1 = Mth.cos(th1), s1 = Mth.sin(th1);

                float nx00 = c0 * cr0, nz00 = s0 * cr0;
                float nx01 = c1 * cr0, nz01 = s1 * cr0;
                float nx10 = c0 * cr1, nz10 = s0 * cr1;
                float nx11 = c1 * cr1, nz11 = s1 * cr1;

                float f00 = (float) Math.pow(1.0F - Math.min(1.0F, Math.abs(nx00 * vx + ny0 * vy + nz00 * vz)), 2.2F);
                float f01 = (float) Math.pow(1.0F - Math.min(1.0F, Math.abs(nx01 * vx + ny0 * vy + nz01 * vz)), 2.2F);
                float f10 = (float) Math.pow(1.0F - Math.min(1.0F, Math.abs(nx10 * vx + ny1 * vy + nz10 * vz)), 2.2F);
                float f11 = (float) Math.pow(1.0F - Math.min(1.0F, Math.abs(nx11 * vx + ny1 * vy + nz11 * vz)), 2.2F);

                int a00 = Math.round((0.05F + 0.80F * f00) * 255.0F * shieldFade);
                int a01 = Math.round((0.05F + 0.80F * f01) * 255.0F * shieldFade);
                int a10 = Math.round((0.05F + 0.80F * f10) * 255.0F * shieldFade);
                int a11 = Math.round((0.05F + 0.80F * f11) * 255.0F * shieldFade);

                int r00 = Math.round(Mth.lerp(f00, 140.0F, 240.0F)), g00 = Math.round(Mth.lerp(f00, 224.0F, 250.0F));
                int r01 = Math.round(Mth.lerp(f01, 140.0F, 240.0F)), g01 = Math.round(Mth.lerp(f01, 224.0F, 250.0F));
                int r10 = Math.round(Mth.lerp(f10, 140.0F, 240.0F)), g10 = Math.round(Mth.lerp(f10, 224.0F, 250.0F));
                int r11 = Math.round(Mth.lerp(f11, 140.0F, 240.0F)), g11 = Math.round(Mth.lerp(f11, 224.0F, 250.0F));

                vc.vertex(sphereMat, nx00 * rSphere, ny0 * rSphere, nz00 * rSphere).color(r00, g00, 255, a00).endVertex();
                vc.vertex(sphereMat, nx01 * rSphere, ny0 * rSphere, nz01 * rSphere).color(r01, g01, 255, a01).endVertex();
                vc.vertex(sphereMat, nx11 * rSphere, ny1 * rSphere, nz11 * rSphere).color(r11, g11, 255, a11).endVertex();
                vc.vertex(sphereMat, nx10 * rSphere, ny1 * rSphere, nz10 * rSphere).color(r10, g10, 255, a10).endVertex();
            }
        }

        int stars = ClientAuraTracker.getNightsFocusStars(entity);
        if (stars > 0) {
            VertexConsumer starVc = buffer.getBuffer(RenderType.entityTranslucentEmissive(LAYLA_NIGHT_STAR_TEX));
            float rStar = 0.96F * openScale;
            float starHalf = 0.16F;
            int starAlpha = Math.round(250.0F * shieldFade);
            float baseDeg = time * 2.5F;
            float stepDeg = 360.0F / Math.max(1, stars);
            for (int s = 0; s < stars; s++) {
                double ang = Math.toRadians(baseDeg + s * stepDeg);
                float sx = (float) Math.cos(ang) * rStar;
                float sz = (float) Math.sin(ang) * rStar;
                float sy = 0.25F + 0.08F * Mth.sin(time * 0.20F + s * 1.5F);
                poseStack.pushPose();
                poseStack.translate(sx, sy, sz);
                poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                Matrix4f sm = poseStack.last().pose();
                starVc.vertex(sm, -starHalf, -starHalf, 0.0F).color(235, 248, 255, starAlpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                starVc.vertex(sm,  starHalf, -starHalf, 0.0F).color(235, 248, 255, starAlpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                starVc.vertex(sm,  starHalf,  starHalf, 0.0F).color(235, 248, 255, starAlpha).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                starVc.vertex(sm, -starHalf,  starHalf, 0.0F).color(235, 248, 255, starAlpha).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(WindGliderRenderer.FULL_BRIGHT).normal(0.0F, 0.0F, 1.0F).endVertex();
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    public static boolean renderStarProjectileArc(
            ClientAuraTracker.ActiveArc arc,
            Vec3 startPos,
            Vec3 endPos,
            Vec3 camPos,
            long now,
            PoseStack poseStack,
            VertexConsumer consumer,
            Matrix4f matrix
    ) {
        if (arc.color != 0xFF7BE8FF) return false;
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        Vec3 arcHeight = new Vec3(0.0D, Math.sin(prog * Math.PI) * 1.15D, 0.0D);
        Vec3 headPos = startPos.lerp(endPos, prog).add(arcHeight);
        float tailProg = Math.max(0.0F, prog - 0.32F);
        Vec3 tailPos = startPos.lerp(endPos, tailProg).add(0.0D, Math.sin(tailProg * Math.PI) * 1.15D, 0.0D);

        poseStack.pushPose();
        poseStack.translate(headPos.x - camPos.x, headPos.y - camPos.y, headPos.z - camPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(prog * 720.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(prog * 360.0F));
        WindGliderRenderer.renderFourPointStar3D(consumer, poseStack.last().pose(), 0.22F, 0.07F, 240, 252, 255, 255, 110, 210, 255, 130);
        poseStack.popPose();

        Vec3 hCam = headPos.subtract(camPos);
        Vec3 tCam = tailPos.subtract(camPos);
        Vec3 dir = hCam.subtract(tCam);
        if (dir.lengthSqr() > 1.0E-5D) {
            Vec3 side = dir.normalize().cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 1.0E-4D) side = new Vec3(1.0D, 0.0D, 0.0D);
            side = side.normalize().scale(0.045D);
            ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                    (float) (tCam.x - side.x), (float) (tCam.y - side.y), (float) (tCam.z - side.z), 110, 175, 255, 20,
                    (float) (tCam.x + side.x), (float) (tCam.y + side.y), (float) (tCam.z + side.z), 110, 175, 255, 20,
                    (float) (hCam.x + side.x * 1.8D), (float) (hCam.y + side.y * 1.8D), (float) (hCam.z + side.z * 1.8D), 220, 248, 255, 220,
                    (float) (hCam.x - side.x * 1.8D), (float) (hCam.y - side.y * 1.8D), (float) (hCam.z - side.z * 1.8D), 220, 248, 255, 220);
        }
        return true;
    }
}
