package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated 3D renderer for Zhongli's Planet Befall Geo meteor, Sky Rift portal,
 * descent trails, ground Geo lock-on seal, and impact shockwave fissures.
 * Uses the carved bronze-basalt {@code stone_stele.png} texture on the outer Rubik sub-cubes
 * paired with glowing golden Geo cores and 3D diamond emblems.
 */
public final class PlanetBefallMeteorRenderer {
    private static final ResourceLocation STELE_TEXTURE =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/entity/stone_stele.png");

    private PlanetBefallMeteorRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        boolean anyMeteor = false;
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId == -3 && arc.start != null && arc.end != null) {
                anyMeteor = true;
                break;
            }
        }
        if (!anyMeteor) return;

        // Pass 1A: Textured Liyue bronze-basalt Rubik sub-cubes (stone_stele.png atlas)
        VertexConsumer texVc = bufferSource.getBuffer(RenderType.entityTranslucent(STELE_TEXTURE));
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId != -3 || arc.start == null || arc.end == null) continue;
            long elapsedMs = Math.max(0L, now - arc.startTime);
            if (elapsedMs > 1220L) continue;

            float fallT = Mth.clamp(elapsedMs / 1100.0F, 0.0F, 1.0F);
            float eased = fallT * fallT * (0.35F + 0.65F * fallT);
            float appear = Mth.clamp(elapsedMs / 160.0F, 0.0F, 1.0F);
            float crushFade = elapsedMs <= 1100L ? 1.0F : Mth.clamp(1.0F - (elapsedMs - 1100.0F) / 120.0F, 0.0F, 1.0F);
            int alpha = ClientAuraRenderer.clampColor((int) (255.0F * appear * crushFade));
            if (alpha <= 4) continue;

            float aoeRadius = Math.max(4.0F, arc.color / 10.0F);
            float s = (0.85F + 0.35F * Mth.clamp(aoeRadius / 8.5F, 0.7F, 1.15F)) * (0.35F + 0.65F * appear);
            Vec3 curPos = arc.start.lerp(arc.end, eased);
            if (elapsedMs > 1100L) {
                curPos = arc.end.add(0.0D, -0.35D * (1.0F - crushFade), 0.0D);
            }

            poseStack.pushPose();
            poseStack.translate(curPos.x - camPos.x, curPos.y - camPos.y, curPos.z - camPos.z);
            float spin = elapsedMs * 0.085F;
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(28.0F + spin * 0.35F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(18.0F - spin * 0.22F));
            Matrix4f mat = poseStack.last().pose();

            float step = 0.62F * s;
            float cubeHalf = 0.24F * s;
            for (int ix = -1; ix <= 1; ix++) {
                for (int iy = -1; iy <= 1; iy++) {
                    for (int iz = -1; iz <= 1; iz++) {
                        int absSum = Math.abs(ix) + Math.abs(iy) + Math.abs(iz);
                        if (absSum <= 1) continue; // keep '+' channels & core unobstructed
                        boolean isCorner = absSum == 3;
                        float radial = isCorner ? step * 1.06F : step * 0.96F;
                        float half = isCorner ? cubeHalf * 1.05F : cubeHalf * 0.94F;
                        int r = isCorner ? 195 : 245;
                        int g = isCorner ? 175 : 225;
                        int b = isCorner ? 145 : 190;
                        drawTexturedSteleCube(texVc, mat, ix * radial, iy * radial, iz * radial, half, half, half, r, g, b, alpha);
                    }
                }
            }
            poseStack.popPose();
        }

        // Pass 1B: Glowing golden Geo core, axial '+' channels & 3D face-center Geo diamond emblems
        VertexConsumer solidVc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.METEOR_SOLID);
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId != -3 || arc.start == null || arc.end == null) continue;
            long elapsedMs = Math.max(0L, now - arc.startTime);
            if (elapsedMs > 1220L) continue;

            float fallT = Mth.clamp(elapsedMs / 1100.0F, 0.0F, 1.0F);
            float eased = fallT * fallT * (0.35F + 0.65F * fallT);
            float appear = Mth.clamp(elapsedMs / 160.0F, 0.0F, 1.0F);
            float crushFade = elapsedMs <= 1100L ? 1.0F : Mth.clamp(1.0F - (elapsedMs - 1100.0F) / 120.0F, 0.0F, 1.0F);
            int alpha = ClientAuraRenderer.clampColor((int) (255.0F * appear * crushFade));
            if (alpha <= 4) continue;

            float aoeRadius = Math.max(4.0F, arc.color / 10.0F);
            float s = (0.85F + 0.35F * Mth.clamp(aoeRadius / 8.5F, 0.7F, 1.15F)) * (0.35F + 0.65F * appear);
            Vec3 curPos = arc.start.lerp(arc.end, eased);
            if (elapsedMs > 1100L) {
                curPos = arc.end.add(0.0D, -0.35D * (1.0F - crushFade), 0.0D);
            }

            poseStack.pushPose();
            poseStack.translate(curPos.x - camPos.x, curPos.y - camPos.y, curPos.z - camPos.z);
            float spin = elapsedMs * 0.085F;
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(28.0F + spin * 0.35F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(18.0F - spin * 0.22F));
            Matrix4f mat = poseStack.last().pose();

            // Glowing golden core & 3 axial '+' monolith beams
            drawShadedMeteorBox(solidVc, mat, 0.0F, 0.0F, 0.0F, 0.56F * s, 0.56F * s, 0.56F * s, 255, 224, 96, alpha);
            drawShadedMeteorBox(solidVc, mat, 0.0F, 0.0F, 0.0F, 1.08F * s, 0.28F * s, 0.28F * s, 255, 206, 72, alpha);
            drawShadedMeteorBox(solidVc, mat, 0.0F, 0.0F, 0.0F, 0.28F * s, 1.08F * s, 0.28F * s, 255, 206, 72, alpha);
            drawShadedMeteorBox(solidVc, mat, 0.0F, 0.0F, 0.0F, 0.28F * s, 0.28F * s, 1.08F * s, 255, 206, 72, alpha);

            // 6 protruding golden face-center glyph caps + glowing Geo diamond emblems
            float capDist = 0.74F * s;
            float capHalf = 0.18F * s;
            drawShadedMeteorBox(solidVc, mat, capDist, 0, 0, capHalf * 0.55F, capHalf, capHalf, 255, 212, 82, alpha);
            drawShadedMeteorBox(solidVc, mat, -capDist, 0, 0, capHalf * 0.55F, capHalf, capHalf, 255, 212, 82, alpha);
            drawShadedMeteorBox(solidVc, mat, 0, capDist, 0, capHalf, capHalf * 0.55F, capHalf, 255, 212, 82, alpha);
            drawShadedMeteorBox(solidVc, mat, 0, -capDist, 0, capHalf, capHalf * 0.55F, capHalf, 255, 212, 82, alpha);
            drawShadedMeteorBox(solidVc, mat, 0, 0, capDist, capHalf, capHalf, capHalf * 0.55F, 255, 212, 82, alpha);
            drawShadedMeteorBox(solidVc, mat, 0, 0, -capDist, capHalf, capHalf, capHalf * 0.55F, 255, 212, 82, alpha);

            drawFaceDiamonds(solidVc, mat, capDist + capHalf * 0.60F, 0.22F * s, alpha);

            poseStack.popPose();
        }

        // Pass 2: Sky Rift portal, comet trails, ground Geo seal & impact shockwave
        VertexConsumer glowVc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        Matrix4f worldMat = poseStack.last().pose();
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId != -3 || arc.start == null || arc.end == null) continue;
            long elapsedMs = Math.max(0L, now - arc.startTime);
            float aoeRadius = Math.max(4.0F, arc.color / 10.0F);
            Vec3 skyCam = arc.start.subtract(camPos);
            Vec3 impCam = arc.end.subtract(camPos);
            Vec3 descentDir = impCam.subtract(skyCam);
            float descLen = (float) descentDir.length();
            if (descLen < 0.01F) continue;
            Vec3 fwd = descentDir.scale(1.0D / descLen);
            Vec3 upRef = Math.abs(fwd.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 right = fwd.cross(upRef).normalize();
            Vec3 portalUp = right.cross(fwd).normalize();

            // 2A. Swirling Dark-Obsidian & Amber Sky Rift at skyStart
            float portalOpen = Mth.clamp(elapsedMs / 200.0F, 0.0F, 1.0F);
            float portalClose = elapsedMs < 950L ? 1.0F : Mth.clamp(1.0F - (elapsedMs - 950.0F) / 500.0F, 0.0F, 1.0F);
            float portalVis = portalOpen * portalClose;
            if (portalVis > 0.01F) {
                float pRad = 3.2F * (0.4F + 0.6F * portalOpen) * (0.5F + 0.5F * portalClose);
                float rot = elapsedMs * 0.004F;
                int segs = 16;
                int darkA = ClientAuraRenderer.clampColor((int) (215.0F * portalVis));
                int ringA = ClientAuraRenderer.clampColor((int) (230.0F * portalVis));
                for (int i = 0; i < segs; i++) {
                    float a0 = rot + (float) (i * Math.PI * 2.0D / segs);
                    float a1 = rot + (float) ((i + 1) * Math.PI * 2.0D / segs);
                    Vec3 dir0 = right.scale(Math.cos(a0)).add(portalUp.scale(Math.sin(a0)));
                    Vec3 dir1 = right.scale(Math.cos(a1)).add(portalUp.scale(Math.sin(a1)));
                    Vec3 i0 = skyCam.add(dir0.scale(pRad * 0.72D));
                    Vec3 i1 = skyCam.add(dir1.scale(pRad * 0.72D));
                    ClientAuraRenderer.emitCrystalQuad(glowVc, worldMat,
                            (float) skyCam.x, (float) skyCam.y, (float) skyCam.z, 38, 22, 12, darkA,
                            (float) skyCam.x, (float) skyCam.y, (float) skyCam.z, 38, 22, 12, darkA,
                            (float) i1.x, (float) i1.y, (float) i1.z, 18, 12, 22, darkA,
                            (float) i0.x, (float) i0.y, (float) i0.z, 18, 12, 22, darkA);
                    Vec3 o0 = skyCam.add(dir0.scale(pRad));
                    Vec3 o1 = skyCam.add(dir1.scale(pRad));
                    ClientAuraRenderer.emitCrystalQuad(glowVc, worldMat,
                            (float) i0.x, (float) i0.y, (float) i0.z, 255, 200, 72, ringA,
                            (float) i1.x, (float) i1.y, (float) i1.z, 255, 200, 72, ringA,
                            (float) o1.x, (float) o1.y, (float) o1.z, 220, 125, 28, 0,
                            (float) o0.x, (float) o0.y, (float) o0.z, 220, 125, 28, 0);
                }
            }

            // 2B. Meteor descent ribbons & Ground Geo Seal
            if (elapsedMs <= 1150L) {
                float fallT = Mth.clamp(elapsedMs / 1100.0F, 0.0F, 1.0F);
                float eased = fallT * fallT * (0.35F + 0.65F * fallT);
                Vec3 curCam = skyCam.lerp(impCam, eased);
                float trailFade = elapsedMs <= 1100L ? 1.0F : Mth.clamp(1.0F - (elapsedMs - 1100.0F) / 50.0F, 0.0F, 1.0F);
                double trailLen = Math.min(5.8D, eased * descLen * 0.65D);
                if (trailLen > 0.2D && trailFade > 0.01F) {
                    Vec3 tailCam = curCam.subtract(fwd.scale(trailLen));
                    int headA = ClientAuraRenderer.clampColor((int) (200.0F * trailFade));
                    float hw = 0.72F;
                    for (int k = 0; k < 2; k++) {
                        Vec3 basis = k == 0 ? right : portalUp;
                        Vec3 h0 = curCam.add(basis.scale(hw));
                        Vec3 h1 = curCam.subtract(basis.scale(hw));
                        Vec3 t0 = tailCam.add(basis.scale(0.10D));
                        Vec3 t1 = tailCam.subtract(basis.scale(0.10D));
                        ClientAuraRenderer.emitCrystalQuad(glowVc, worldMat,
                                (float) h0.x, (float) h0.y, (float) h0.z, 255, 218, 92, headA,
                                (float) h1.x, (float) h1.y, (float) h1.z, 255, 218, 92, headA,
                                (float) t1.x, (float) t1.y, (float) t1.z, 240, 145, 32, 0,
                                (float) t0.x, (float) t0.y, (float) t0.z, 240, 145, 32, 0);
                    }
                }

                // Ground target ring under impactPos
                float lockExpand = Mth.clamp(elapsedMs / 320.0F, 0.0F, 1.0F);
                float lockRad = aoeRadius * (0.55F + 0.45F * lockExpand);
                int sealA = ClientAuraRenderer.clampColor((int) (135.0F * (0.35F + 0.65F * fallT)));
                float yRing = (float) impCam.y + 0.06F;
                float spinSeal = elapsedMs * 0.0022F;
                int ringSegs = 16;
                for (int i = 0; i < ringSegs; i++) {
                    double a0 = spinSeal + i * Math.PI * 2.0D / ringSegs;
                    double a1 = spinSeal + (i + 1) * Math.PI * 2.0D / ringSegs;
                    float ix0 = (float) impCam.x + (float) Math.cos(a0) * (lockRad - 0.18F);
                    float iz0 = (float) impCam.z + (float) Math.sin(a0) * (lockRad - 0.18F);
                    float ix1 = (float) impCam.x + (float) Math.cos(a1) * (lockRad - 0.18F);
                    float iz1 = (float) impCam.z + (float) Math.sin(a1) * (lockRad - 0.18F);
                    float ox0 = (float) impCam.x + (float) Math.cos(a0) * lockRad;
                    float oz0 = (float) impCam.z + (float) Math.sin(a0) * lockRad;
                    float ox1 = (float) impCam.x + (float) Math.cos(a1) * lockRad;
                    float oz1 = (float) impCam.z + (float) Math.sin(a1) * lockRad;
                    ClientAuraRenderer.emitCrystalQuad(glowVc, worldMat,
                            ix0, yRing, iz0, 255, 198, 68, sealA,
                            ix1, yRing, iz1, 255, 198, 68, sealA,
                            ox1, yRing, oz1, 255, 165, 42, sealA,
                            ox0, yRing, oz0, 255, 165, 42, sealA);
                }
            }

            // 2C. Impact Shockwave Dome & Radial Geo Fissures
            if (elapsedMs >= 1060L) {
                float impT = Mth.clamp((elapsedMs - 1060.0F) / 720.0F, 0.0F, 1.0F);
                float wave = Mth.sin(impT * ((float) Math.PI * 0.5F));
                float fade = 1.0F - impT;
                float rWave = aoeRadius * (0.28F + 0.80F * wave);
                int domeA = ClientAuraRenderer.clampColor((int) (175.0F * fade * fade));
                float yBase = (float) impCam.y + 0.05F;
                float hWave = (0.6F + 1.8F * wave) * fade;
                int segs = 20;
                for (int i = 0; i < segs; i++) {
                    double a0 = i * Math.PI * 2.0D / segs;
                    double a1 = (i + 1) * Math.PI * 2.0D / segs;
                    float bx0 = (float) impCam.x + (float) Math.cos(a0) * rWave;
                    float bz0 = (float) impCam.z + (float) Math.sin(a0) * rWave;
                    float bx1 = (float) impCam.x + (float) Math.cos(a1) * rWave;
                    float bz1 = (float) impCam.z + (float) Math.sin(a1) * rWave;
                    ClientAuraRenderer.emitCrystalQuad(glowVc, worldMat,
                            bx0, yBase, bz0, 255, 206, 72, domeA,
                            bx1, yBase, bz1, 255, 206, 72, domeA,
                            bx1, yBase + hWave, bz1, 255, 232, 140, 0,
                            bx0, yBase + hWave, bz0, 255, 232, 140, 0);
                }

                int fisA = ClientAuraRenderer.clampColor((int) (210.0F * fade));
                int rays = 12;
                for (int i = 0; i < rays; i++) {
                    double ang = i * Math.PI * 2.0D / rays + 0.15D;
                    float c = (float) Math.cos(ang);
                    float sn = (float) Math.sin(ang);
                    float px = -sn * 0.14F;
                    float pz = c * 0.14F;
                    float far = aoeRadius * (0.45F + 0.55F * wave);
                    float sx = (float) impCam.x + c * 0.45F;
                    float sz = (float) impCam.z + sn * 0.45F;
                    float ex = (float) impCam.x + c * far;
                    float ez = (float) impCam.z + sn * far;
                    ClientAuraRenderer.emitCrystalQuad(glowVc, worldMat,
                            sx - px, yBase + 0.03F, sz - pz, 255, 218, 92, fisA,
                            sx + px, yBase + 0.03F, sz + pz, 255, 218, 92, fisA,
                            ex + px * 0.25F, yBase + 0.03F, ez + pz * 0.25F, 215, 130, 28, 0,
                            ex - px * 0.25F, yBase + 0.03F, ez - pz * 0.25F, 215, 130, 28, 0);
                }
            }
        }
    }

    private static void drawTexturedSteleCube(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float hx, float hy, float hz,
            int baseR, int baseG, int baseB, int alpha
    ) {
        float x0 = cx - hx, x1 = cx + hx;
        float y0 = cy - hy, y1 = cy + hy;
        float z0 = cz - hz, z1 = cz + hz;
        float u0 = 0.0F, u1 = 0.25F, u2 = 0.50F, u3 = 0.75F, u4 = 1.0F;
        float vTop = 0.0F, vBot = 0.50F;

        int sr = ClientAuraRenderer.clampColor((int) (baseR * 0.94F));
        int sg = ClientAuraRenderer.clampColor((int) (baseG * 0.94F));
        int sb = ClientAuraRenderer.clampColor((int) (baseB * 0.94F));
        int dr = ClientAuraRenderer.clampColor((int) (baseR * 0.78F));
        int dg = ClientAuraRenderer.clampColor((int) (baseG * 0.78F));
        int db = ClientAuraRenderer.clampColor((int) (baseB * 0.78F));
        int br = ClientAuraRenderer.clampColor((int) (baseR * 0.60F));
        int bg = ClientAuraRenderer.clampColor((int) (baseG * 0.60F));
        int bb = ClientAuraRenderer.clampColor((int) (baseB * 0.60F));

        // North (-Z)
        emitTexVert(vc, mat, x0, y1, z0, u0, vTop, sr, sg, sb, alpha, 0.0F, 0.0F, -1.0F);
        emitTexVert(vc, mat, x1, y1, z0, u1, vTop, sr, sg, sb, alpha, 0.0F, 0.0F, -1.0F);
        emitTexVert(vc, mat, x1, y0, z0, u1, vBot, sr, sg, sb, alpha, 0.0F, 0.0F, -1.0F);
        emitTexVert(vc, mat, x0, y0, z0, u0, vBot, sr, sg, sb, alpha, 0.0F, 0.0F, -1.0F);

        // South (+Z)
        emitTexVert(vc, mat, x1, y1, z1, u1, vTop, sr, sg, sb, alpha, 0.0F, 0.0F, 1.0F);
        emitTexVert(vc, mat, x0, y1, z1, u2, vTop, sr, sg, sb, alpha, 0.0F, 0.0F, 1.0F);
        emitTexVert(vc, mat, x0, y0, z1, u2, vBot, sr, sg, sb, alpha, 0.0F, 0.0F, 1.0F);
        emitTexVert(vc, mat, x1, y0, z1, u1, vBot, sr, sg, sb, alpha, 0.0F, 0.0F, 1.0F);

        // East (+X)
        emitTexVert(vc, mat, x1, y1, z0, u2, vTop, dr, dg, db, alpha, 1.0F, 0.0F, 0.0F);
        emitTexVert(vc, mat, x1, y1, z1, u3, vTop, dr, dg, db, alpha, 1.0F, 0.0F, 0.0F);
        emitTexVert(vc, mat, x1, y0, z1, u3, vBot, dr, dg, db, alpha, 1.0F, 0.0F, 0.0F);
        emitTexVert(vc, mat, x1, y0, z0, u2, vBot, dr, dg, db, alpha, 1.0F, 0.0F, 0.0F);

        // West (-X)
        emitTexVert(vc, mat, x0, y1, z1, u3, vTop, dr, dg, db, alpha, -1.0F, 0.0F, 0.0F);
        emitTexVert(vc, mat, x0, y1, z0, u4, vTop, dr, dg, db, alpha, -1.0F, 0.0F, 0.0F);
        emitTexVert(vc, mat, x0, y0, z0, u4, vBot, dr, dg, db, alpha, -1.0F, 0.0F, 0.0F);
        emitTexVert(vc, mat, x0, y0, z1, u3, vBot, dr, dg, db, alpha, -1.0F, 0.0F, 0.0F);

        // Top (+Y)
        emitTexVert(vc, mat, x0, y1, z0, 0.0F,  0.50F, baseR, baseG, baseB, alpha, 0.0F, 1.0F, 0.0F);
        emitTexVert(vc, mat, x0, y1, z1, 0.0F,  0.75F, baseR, baseG, baseB, alpha, 0.0F, 1.0F, 0.0F);
        emitTexVert(vc, mat, x1, y1, z1, 0.25F, 0.75F, baseR, baseG, baseB, alpha, 0.0F, 1.0F, 0.0F);
        emitTexVert(vc, mat, x1, y1, z0, 0.25F, 0.50F, baseR, baseG, baseB, alpha, 0.0F, 1.0F, 0.0F);

        // Bottom (-Y)
        emitTexVert(vc, mat, x0, y0, z1, 0.25F, 0.50F, br, bg, bb, alpha, 0.0F, -1.0F, 0.0F);
        emitTexVert(vc, mat, x0, y0, z0, 0.25F, 0.75F, br, bg, bb, alpha, 0.0F, -1.0F, 0.0F);
        emitTexVert(vc, mat, x1, y0, z0, 0.50F, 0.75F, br, bg, bb, alpha, 0.0F, -1.0F, 0.0F);
        emitTexVert(vc, mat, x1, y0, z1, 0.50F, 0.50F, br, bg, bb, alpha, 0.0F, -1.0F, 0.0F);
    }

    private static void emitTexVert(
            VertexConsumer vc, Matrix4f mat,
            float x, float y, float z,
            float u, float v,
            int r, int g, int b, int a,
            float nx, float ny, float nz
    ) {
        vc.vertex(mat, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(nx, ny, nz)
                .endVertex();
    }

    private static void drawFaceDiamonds(VertexConsumer vc, Matrix4f mat, float dist, float rad, int alpha) {
        int r = 255, g = 242, b = 158;
        // +Z / -Z
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                0.0F, rad, dist, r, g, b, alpha,
                rad, 0.0F, dist, r, g, b, alpha,
                0.0F, -rad, dist, r, g, b, alpha,
                -rad, 0.0F, dist, r, g, b, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                0.0F, rad, -dist, r, g, b, alpha,
                -rad, 0.0F, -dist, r, g, b, alpha,
                0.0F, -rad, -dist, r, g, b, alpha,
                rad, 0.0F, -dist, r, g, b, alpha);
        // +X / -X
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                dist, rad, 0.0F, r, g, b, alpha,
                dist, 0.0F, rad, r, g, b, alpha,
                dist, -rad, 0.0F, r, g, b, alpha,
                dist, 0.0F, -rad, r, g, b, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                -dist, rad, 0.0F, r, g, b, alpha,
                -dist, 0.0F, -rad, r, g, b, alpha,
                -dist, -rad, 0.0F, r, g, b, alpha,
                -dist, 0.0F, rad, r, g, b, alpha);
        // +Y / -Y
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                0.0F, dist, rad, r, g, b, alpha,
                rad, dist, 0.0F, r, g, b, alpha,
                0.0F, dist, -rad, r, g, b, alpha,
                -rad, dist, 0.0F, r, g, b, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                0.0F, -dist, -rad, r, g, b, alpha,
                rad, -dist, 0.0F, r, g, b, alpha,
                0.0F, -dist, rad, r, g, b, alpha,
                -rad, -dist, 0.0F, r, g, b, alpha);
    }

    private static void drawShadedMeteorBox(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float hx, float hy, float hz,
            int baseR, int baseG, int baseB, int alpha
    ) {
        float x0 = cx - hx, x1 = cx + hx;
        float y0 = cy - hy, y1 = cy + hy;
        float z0 = cz - hz, z1 = cz + hz;
        int tr = ClientAuraRenderer.clampColor((int) (baseR * 1.15F));
        int tg = ClientAuraRenderer.clampColor((int) (baseG * 1.15F));
        int tb = ClientAuraRenderer.clampColor((int) (baseB * 1.15F));
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y1, z1, tr, tg, tb, alpha, x1, y1, z1, tr, tg, tb, alpha, x1, y1, z0, tr, tg, tb, alpha, x0, y1, z0, tr, tg, tb, alpha);
        int br = ClientAuraRenderer.clampColor((int) (baseR * 0.55F));
        int bg = ClientAuraRenderer.clampColor((int) (baseG * 0.55F));
        int bb = ClientAuraRenderer.clampColor((int) (baseB * 0.55F));
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y0, z0, br, bg, bb, alpha, x1, y0, z0, br, bg, bb, alpha, x1, y0, z1, br, bg, bb, alpha, x0, y0, z1, br, bg, bb, alpha);
        int sr = ClientAuraRenderer.clampColor((int) (baseR * 0.90F));
        int sg = ClientAuraRenderer.clampColor((int) (baseG * 0.90F));
        int sb = ClientAuraRenderer.clampColor((int) (baseB * 0.90F));
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y0, z1, sr, sg, sb, alpha, x1, y0, z1, sr, sg, sb, alpha, x1, y1, z1, sr, sg, sb, alpha, x0, y1, z1, sr, sg, sb, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x1, y0, z0, sr, sg, sb, alpha, x0, y0, z0, sr, sg, sb, alpha, x0, y1, z0, sr, sg, sb, alpha, x1, y1, z0, sr, sg, sb, alpha);
        int dr = ClientAuraRenderer.clampColor((int) (baseR * 0.72F));
        int dg = ClientAuraRenderer.clampColor((int) (baseG * 0.72F));
        int db = ClientAuraRenderer.clampColor((int) (baseB * 0.72F));
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y0, z0, dr, dg, db, alpha, x0, y0, z1, dr, dg, db, alpha, x0, y1, z1, dr, dg, db, alpha, x0, y1, z0, dr, dg, db, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x1, y0, z1, dr, dg, db, alpha, x1, y0, z0, dr, dg, db, alpha, x1, y1, z0, dr, dg, db, alpha, x1, y1, z1, dr, dg, db, alpha);
    }
}
