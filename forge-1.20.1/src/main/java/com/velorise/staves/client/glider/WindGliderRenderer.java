package com.velorise.staves.client.glider;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated renderer for 3D Wind Glider wings, aerodynamic flapping curves,
 * torso attachments, universal plunge/glider animations, shared geometry primitives,
 * and auxiliary staff poses (Furina, Layla, Ghastfire).
 */
public final class WindGliderRenderer {
    public static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private WindGliderRenderer() {}

    public static float smoothStep01(float x) {
        float c = Mth.clamp(x, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    public static HumanoidArm resolveStaffArm(LivingEntity entity, ArcaneStaffItem.Kind targetKind) {
        HumanoidArm main = entity.getMainArm();
        if (entity.isUsingItem()) {
            return entity.getUsedItemHand() == InteractionHand.OFF_HAND ? main.getOpposite() : main;
        }
        if (targetKind != null) {
            if (ArcaneStaffItem.getEffectiveKind(entity.getMainHandItem()) == targetKind) {
                return main;
            }
            if (!ArcaneStaffItem.isGenshinCharacterWeapon(entity.getMainHandItem())
                    && ArcaneStaffItem.getEffectiveKind(entity.getOffhandItem()) == targetKind) {
                return main.getOpposite();
            }
        }
        return main;
    }

    public static void emitWingQuad(
            VertexConsumer vc, PoseStack.Pose pose,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz
    ) {
        Matrix4f mat = pose.pose();
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        org.joml.Matrix3f normMat = pose.normal();
        vc.vertex(mat, x0, y0, z0).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(normMat, nx, ny, nz).endVertex();
        vc.vertex(mat, x1, y1, z1).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(normMat, nx, ny, nz).endVertex();
        vc.vertex(mat, x2, y2, z2).color(255, 255, 255, 255).uv(u2, v2).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(normMat, nx, ny, nz).endVertex();
        vc.vertex(mat, x3, y3, z3).color(255, 255, 255, 255).uv(u3, v3).overlayCoords(overlay).uv2(FULL_BRIGHT).normal(normMat, nx, ny, nz).endVertex();
    }

    public static void renderGliderWings(
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
        if (!ClientAuraTracker.isGliding(entity.getId())) return;

        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float entityScale = 1.0F;
        float time = entity.tickCount + partialTicks;

        ResourceLocation gliderTex = new ResourceLocation(VeloriseStavesMod.MODID, StavesConfig.isUse32pxGliderTexture() ? "textures/entity/wind_glider_32px.png" : "textures/entity/wind_glider.png");
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(gliderTex));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-entityScale, -entityScale, entityScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        humanoid.body.translateAndRotate(poseStack);

        float waveInner = Mth.sin(time * 0.18F) * 2.5F;
        float waveOuter = Mth.sin(time * 0.18F - 0.85F) * 4.0F;
        float pitchBreath = Mth.cos(time * 0.18F) * 1.5F;
        float u0 = 0.0F, uMid = 11.0F / 64.0F, u1 = 20.0F / 64.0F;
        float v0 = 0.0F, v1 = 16.0F / 32.0F;
        float innerW = 0.95F;
        float outerW = 0.85F;
        float wingH = 1.25F;

        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.065F, 0.015F, 0.068F);
            poseStack.mulPose(Axis.XP.rotationDegrees(62.0F + pitchBreath));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * (14.0F + waveInner)));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * -13.0F));
            PoseStack.Pose innerPose = poseStack.last();
            if (side > 0) {
                emitWingQuad(vc, innerPose,
                        0.0F, 0.0F, 0.0F, u0, v0,
                        innerW, 0.0F, 0.0F, uMid, v0,
                        innerW, wingH, 0.0F, uMid, v1,
                        0.0F, wingH, 0.0F, u0, v1,
                        0.0F, 1.0F, 0.0F);
                poseStack.translate(innerW, 0.0F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F + waveOuter));
                poseStack.mulPose(Axis.YP.rotationDegrees(20.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
                PoseStack.Pose outerPose = poseStack.last();
                emitWingQuad(vc, outerPose,
                        0.0F, 0.0F, 0.0F, uMid, v0,
                        outerW, 0.0F, 0.0F, u1, v0,
                        outerW, wingH, 0.0F, u1, v1,
                        0.0F, wingH, 0.0F, uMid, v1,
                        0.0F, 1.0F, 0.0F);
            } else {
                emitWingQuad(vc, innerPose,
                        0.0F, 0.0F, 0.0F, u0, v0,
                        -innerW, 0.0F, 0.0F, uMid, v0,
                        -innerW, wingH, 0.0F, uMid, v1,
                        0.0F, wingH, 0.0F, u0, v1,
                        0.0F, 1.0F, 0.0F);
                poseStack.translate(-innerW, 0.0F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F - waveOuter));
                poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
                PoseStack.Pose outerPose = poseStack.last();
                emitWingQuad(vc, outerPose,
                        0.0F, 0.0F, 0.0F, uMid, v0,
                        -outerW, 0.0F, 0.0F, u1, v0,
                        -outerW, wingH, 0.0F, u1, v1,
                        0.0F, wingH, 0.0F, uMid, v1,
                        0.0F, 1.0F, 0.0F);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static boolean applyPlungeAndGliderHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (ClientAuraTracker.isPlunging(entity.getId())) {
            model.attackTime = 0.0F;
            model.body.xRot = 0.36F;
            model.rightArm.xRot = -1.32F;
            model.leftArm.xRot = -1.22F;
            model.rightArm.yRot = -0.42F;
            model.leftArm.yRot = 0.38F;
            model.rightArm.zRot = -0.20F;
            model.leftArm.zRot = 0.20F;
            model.rightLeg.xRot = 0.32F;
            model.leftLeg.xRot = 0.52F;
            model.head.xRot = 0.26F;
            model.hat.copyFrom(model.head);
            return true;
        }
        if (ClientAuraTracker.isGliding(entity.getId())) {
            float breeze = Mth.sin(ageInTicks * 0.16F) * 0.04F;
            model.body.xRot = 0.22F;
            model.body.yRot = 0.0F;
            model.body.zRot = 0.0F;
            model.rightArm.xRot = -0.08F + breeze;
            model.leftArm.xRot = -0.08F - breeze;
            model.rightArm.yRot = -0.05F;
            model.leftArm.yRot = 0.05F;
            model.rightArm.zRot = 1.38F + breeze;
            model.leftArm.zRot = -1.38F - breeze;
            model.rightLeg.xRot = 0.28F + breeze;
            model.leftLeg.xRot = 0.18F - breeze;
            model.rightLeg.zRot = 0.06F;
            model.leftLeg.zRot = -0.06F;
            model.head.xRot = -0.16F;
            model.hat.copyFrom(model.head);
            return true;
        }
        return false;
    }

    public static boolean applyThirdPersonPlungeTransform(LivingEntity entity, boolean leftHand, PoseStack poseStack) {
        if (ClientAuraTracker.isPlunging(entity.getId())) {
            float side = leftHand ? -1.0F : 1.0F;
            poseStack.translate(-side * 0.08F, 0.05F, -0.06F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 28.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F));
            return true;
        }
        return false;
    }

    public static boolean applyFirstPersonPlungeTransform(AbstractClientPlayer player, float dir, PoseStack poseStack) {
        if (ClientAuraTracker.isPlunging(player.getId())) {
            poseStack.translate(-dir * 0.32F, 0.10F, -0.18F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 32.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-42.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 14.0F));
            return true;
        }
        return false;
    }

    public static void emitDoubleQuad(
            VertexConsumer vc, Matrix4f mat,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int r, int g, int b, int a
    ) {
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x1, y1, z1, r, g, b, a, x2, y2, z2, r, g, b, a, x3, y3, z3, r, g, b, a, x4, y4, z4, r, g, b, a);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x4, y4, z4, r, g, b, a, x3, y3, z3, r, g, b, a, x2, y2, z2, r, g, b, a, x1, y1, z1, r, g, b, a);
    }

    public static void drawHudArc(VertexConsumer vc, Matrix4f mat, float cx, float cy,
                                  float rIn, float rOut, float startDeg, float endDeg, int segs,
                                  int r, int g, int b, int a) {
        for (int i = 0; i < segs; i++) {
            float a0 = (float) Math.toRadians(Mth.lerp((float) i / segs, startDeg, endDeg));
            float a1 = (float) Math.toRadians(Mth.lerp((float) (i + 1) / segs, startDeg, endDeg));
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            vc.vertex(mat, cx + c0 * rIn, cy + s0 * rIn, 0.0F).color(r, g, b, a).endVertex();
            vc.vertex(mat, cx + c1 * rIn, cy + s1 * rIn, 0.0F).color(r, g, b, a).endVertex();
            vc.vertex(mat, cx + c1 * rOut, cy + s1 * rOut, 0.0F).color(r, g, b, a).endVertex();
            vc.vertex(mat, cx + c0 * rOut, cy + s0 * rOut, 0.0F).color(r, g, b, a).endVertex();
        }
    }

    public static void drawTranslucentSphere(
            VertexConsumer consumer, Matrix4f matrix,
            float cx, float cy, float cz, float radius,
            int latBands, int lonBands, int r, int g, int b, int a
    ) {
        for (int lat = 0; lat < latBands; lat++) {
            float theta0 = (float) (Math.PI * lat / latBands);
            float theta1 = (float) (Math.PI * (lat + 1) / latBands);
            float sin0 = Mth.sin(theta0), cos0 = Mth.cos(theta0);
            float sin1 = Mth.sin(theta1), cos1 = Mth.cos(theta1);
            for (int lon = 0; lon < lonBands; lon++) {
                float phi0 = (float) (2.0 * Math.PI * lon / lonBands);
                float phi1 = (float) (2.0 * Math.PI * (lon + 1) / lonBands);
                float cPhi0 = Mth.cos(phi0), sPhi0 = Mth.sin(phi0);
                float cPhi1 = Mth.cos(phi1), sPhi1 = Mth.sin(phi1);

                float x00 = cx + radius * sin0 * cPhi0, y00 = cy + radius * cos0, z00 = cz + radius * sin0 * sPhi0;
                float x10 = cx + radius * sin1 * cPhi0, y10 = cy + radius * cos1, z10 = cz + radius * sin1 * sPhi0;
                float x11 = cx + radius * sin1 * cPhi1, y11 = cy + radius * cos1, z11 = cz + radius * sin1 * sPhi1;
                float x01 = cx + radius * sin0 * cPhi1, y01 = cy + radius * cos0, z01 = cz + radius * sin0 * sPhi1;

                ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                        x00, y00, z00, r, g, b, a,
                        x10, y10, z10, r, g, b, a,
                        x11, y11, z11, r, g, b, a,
                        x01, y01, z01, r, g, b, a);
            }
        }
    }

    public static void renderFourPointStar3D(
            VertexConsumer vc, Matrix4f mat, float outerLen, float innerThick,
            int cr, int cg, int cb, int ca, int gr, int gg, int gb, int ga) {
        emitStarDiamond(vc, mat, outerLen * 1.45F, innerThick * 1.45F, gr, gg, gb, ga);
        emitStarDiamond(vc, mat, outerLen, innerThick, cr, cg, cb, ca);
    }

    public static void emitStarDiamond(VertexConsumer vc, Matrix4f mat, float len, float w, int r, int g, int b, int a) {
        float[][] tips = {{0, len, 0}, {0, -len, 0}, {len, 0, 0}, {-len, 0, 0}, {0, 0, len}, {0, 0, -len}};
        for (float[] tip : tips) {
            if (Math.abs(tip[1]) > 0.001F) {
                ClientAuraRenderer.emitCrystalQuad(vc, mat,
                        -w, 0, 0, r, g, b, a,
                        0, tip[1], 0, r, g, b, a,
                        w, 0, 0, r, g, b, a,
                        0, 0, w, r, g, b, a);
                ClientAuraRenderer.emitCrystalQuad(vc, mat,
                        w, 0, 0, r, g, b, a,
                        0, tip[1], 0, r, g, b, a,
                        -w, 0, 0, r, g, b, a,
                        0, 0, -w, r, g, b, a);
            } else if (Math.abs(tip[0]) > 0.001F) {
                ClientAuraRenderer.emitCrystalQuad(vc, mat,
                        0, -w, 0, r, g, b, a,
                        tip[0], 0, 0, r, g, b, a,
                        0, w, 0, r, g, b, a,
                        0, 0, w, r, g, b, a);
                ClientAuraRenderer.emitCrystalQuad(vc, mat,
                        0, w, 0, r, g, b, a,
                        tip[0], 0, 0, r, g, b, a,
                        0, -w, 0, r, g, b, a,
                        0, 0, -w, r, g, b, a);
            } else {
                ClientAuraRenderer.emitCrystalQuad(vc, mat,
                        -w, 0, 0, r, g, b, a,
                        0, 0, tip[2], r, g, b, a,
                        w, 0, 0, r, g, b, a,
                        0, w, 0, r, g, b, a);
                ClientAuraRenderer.emitCrystalQuad(vc, mat,
                        w, 0, 0, r, g, b, a,
                        0, 0, tip[2], r, g, b, a,
                        -w, 0, 0, r, g, b, a,
                        0, -w, 0, r, g, b, a);
            }
        }
    }

    public static void renderCloverCross2D(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float size, int r, int g, int b, int a
    ) {
        float tip = size * 1.35F;
        float w = size * 0.32F;
        emitDoubleQuad(vc, mat,
                cx - tip, cy - w, cz,
                cx + tip, cy - w, cz,
                cx + tip, cy + w, cz,
                cx - tip, cy + w, cz,
                r, g, b, a);
        emitDoubleQuad(vc, mat,
                cx - w, cy - tip, cz,
                cx + w, cy - tip, cz,
                cx + w, cy + tip, cz,
                cx - w, cy + tip, cz,
                r, g, b, a);
        float d = size * 0.45F;
        emitDoubleQuad(vc, mat,
                cx, cy - d, cz + 0.005F,
                cx + d, cy, cz + 0.005F,
                cx, cy + d, cz + 0.005F,
                cx - d, cy, cz + 0.005F,
                255, 255, 220, Math.min(255, a + 30));
    }

    public static void renderFourLeafClover2D(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float radius, int r, int g, int b, int a
    ) {
        float petalR = radius * 0.52F;
        float offset = radius * 0.48F;
        float[][] offsets = {{0, offset}, {0, -offset}, {offset, 0}, {-offset, 0}};
        for (float[] off : offsets) {
            float px = cx + off[0], py = cy + off[1];
            emitDoubleQuad(vc, mat,
                    px - petalR, py - petalR, cz,
                    px + petalR, py - petalR, cz,
                    px + petalR, py + petalR, cz,
                    px - petalR, py + petalR, cz,
                    r, g, b, a);
        }
        float coreR = radius * 0.35F;
        emitDoubleQuad(vc, mat,
                cx - coreR, cy - coreR, cz + 0.005F,
                cx + coreR, cy - coreR, cz + 0.005F,
                cx + coreR, cy + coreR, cz + 0.005F,
                cx - coreR, cy + coreR, cz + 0.005F,
                245, 255, 210, Math.min(255, a + 40));
    }

    public static void renderFourLeafCloverXZ(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float radius, int r, int g, int b, int a
    ) {
        float petalR = radius * 0.52F;
        float offset = radius * 0.48F;
        float[][] offsets = {{0, offset}, {0, -offset}, {offset, 0}, {-offset, 0}};
        for (float[] off : offsets) {
            float px = cx + off[0], pz = cz + off[1];
            emitDoubleQuad(vc, mat,
                    px - petalR, cy, pz - petalR,
                    px + petalR, cy, pz - petalR,
                    px + petalR, cy, pz + petalR,
                    px - petalR, cy, pz + petalR,
                    r, g, b, a);
        }
        float coreR = radius * 0.35F;
        emitDoubleQuad(vc, mat,
                cx - coreR, cy + 0.005F, cz - coreR,
                cx + coreR, cy + 0.005F, cz - coreR,
                cx + coreR, cy + 0.005F, cz + coreR,
                cx - coreR, cy + 0.005F, cz + coreR,
                245, 255, 210, Math.min(255, a + 40));
    }

    public static void renderCloverCrossXZ(
            VertexConsumer vc, Matrix4f mat,
            float cx, float cy, float cz,
            float size, int r, int g, int b, int a
    ) {
        float tip = size * 1.35F;
        float w = size * 0.32F;
        emitDoubleQuad(vc, mat,
                cx - tip, cy, cz - w,
                cx + tip, cy, cz - w,
                cx + tip, cy, cz + w,
                cx - tip, cy, cz + w,
                r, g, b, a);
        emitDoubleQuad(vc, mat,
                cx - w, cy, cz - tip,
                cx + w, cy, cz - tip,
                cx + w, cy, cz + tip,
                cx - w, cy, cz + tip,
                r, g, b, a);
    }

    public static void renderCelestialOrbitRing(
            PoseStack poseStack, VertexConsumer vc, float ringR, float halfThick,
            float tiltX, float tiltZ, float spinY, int r, int g, int b, int a) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltZ));
        poseStack.mulPose(Axis.YP.rotationDegrees(spinY));
        Matrix4f mat = poseStack.last().pose();
        int segs = 28;
        float rIn = ringR - halfThick;
        float rOut = ringR + halfThick;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2.0D * Math.PI / segs);
            float a1 = (float) ((i + 1) * 2.0D * Math.PI / segs);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    c0 * rIn, 0.0F, s0 * rIn, r, g, b, a,
                    c1 * rIn, 0.0F, s1 * rIn, r, g, b, a,
                    c1 * rOut, 0.0F, s1 * rOut, r, g, b, a,
                    c0 * rOut, 0.0F, s0 * rOut, r, g, b, a);
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    c0 * rOut, 0.0F, s0 * rOut, r, g, b, a,
                    c1 * rOut, 0.0F, s1 * rOut, r, g, b, a,
                    c1 * rIn, 0.0F, s1 * rIn, r, g, b, a,
                    c0 * rIn, 0.0F, s0 * rIn, r, g, b, a);
        }
        poseStack.popPose();
    }

    private static void renderLaylaOverheadDreamsphere(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        Minecraft mc = Minecraft.getInstance();
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        float time = entity.tickCount + mc.getFrameTime();
        float h = entity.getBbHeight();

        poseStack.pushPose();
        poseStack.translate(0.0F, h + 1.15F + 0.04F * Mth.sin(time * 0.12F), 0.0F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.4F));
        Matrix4f sphereMat = poseStack.last().pose();
        float rOrb = 0.34F;
        int stacks = 6;
        int slices = 12;
        for (int i = 0; i < stacks; i++) {
            float phi0 = (float) (-Math.PI * 0.5D + Math.PI * i / stacks);
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * (i + 1) / stacks);
            float y0 = Mth.sin(phi0) * rOrb, y1 = Mth.sin(phi1) * rOrb;
            float rr0 = Mth.cos(phi0) * rOrb, rr1 = Mth.cos(phi1) * rOrb;
            for (int j = 0; j < slices; j++) {
                float th0 = (float) (j * 2.0D * Math.PI / slices);
                float th1 = (float) ((j + 1) * 2.0D * Math.PI / slices);
                ClientAuraRenderer.emitCrystalQuad(vc, sphereMat,
                        Mth.cos(th0) * rr0, y0, Mth.sin(th0) * rr0, 95, 165, 255, 95,
                        Mth.cos(th1) * rr0, y0, Mth.sin(th1) * rr0, 95, 165, 255, 95,
                        Mth.cos(th1) * rr1, y1, Mth.sin(th1) * rr1, 160, 225, 255, 125,
                        Mth.cos(th0) * rr1, y1, Mth.sin(th0) * rr1, 160, 225, 255, 125);
            }
        }
        renderFourPointStar3D(vc, sphereMat, 0.18F, 0.06F, 245, 252, 255, 255, 135, 210, 255, 185);
        poseStack.popPose();

        renderCelestialOrbitRing(poseStack, vc, 0.46F, 0.020F, 22.0F, -14.0F, time * 3.2F, 255, 228, 120, 210);
        renderCelestialOrbitRing(poseStack, vc, 0.52F, 0.016F, -28.0F, 18.0F, -time * 2.5F, 155, 225, 255, 185);
        poseStack.popPose();
    }

    public static void renderNightsFocusBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (ClientAuraRenderer.isRenderingScreen() || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        boolean hasShield = ClientAuraTracker.hasNightsFocusShield(entity);
        boolean hasDreamsphere = ClientAuraTracker.hasLaylaDreamsphere(entity.getId());
        if (!hasShield && !hasDreamsphere) return;
        if (hasDreamsphere) {
            renderLaylaOverheadDreamsphere(entity, poseStack, buffer);
        }
        if (!hasShield) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        float time = entity.tickCount + mc.getFrameTime();
        float h = entity.getBbHeight();
        float radius = Math.max(0.95F, h * 0.62F);
        float centerY = h * 0.50F;
        float pulse = 1.0F + 0.025F * Mth.sin(time * 0.14F);
        float rSphere = radius * pulse;

        poseStack.pushPose();
        poseStack.translate(0.0F, centerY, 0.0F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.4F));
        Matrix4f sphereMat = poseStack.last().pose();
        int stacks = 10;
        int slices = 18;
        for (int i = 0; i < stacks; i++) {
            float phi0 = (float) (-Math.PI * 0.5D + Math.PI * i / stacks);
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * (i + 1) / stacks);
            float y0 = Mth.sin(phi0) * rSphere;
            float y1 = Mth.sin(phi1) * rSphere;
            float ringR0 = Mth.cos(phi0) * rSphere;
            float ringR1 = Mth.cos(phi1) * rSphere;

            float eq0 = 1.0F - Math.abs(Mth.sin(phi0));
            float eq1 = 1.0F - Math.abs(Mth.sin(phi1));
            int r0 = (int) Mth.lerp(eq0, 110, 145);
            int g0 = (int) Mth.lerp(eq0, 130, 225);
            int b0 = 255;
            int a0 = (int) Mth.lerp(eq0, 16, 38);
            int r1 = (int) Mth.lerp(eq1, 110, 145);
            int g1 = (int) Mth.lerp(eq1, 130, 225);
            int b1 = 255;
            int a1 = (int) Mth.lerp(eq1, 16, 38);

            for (int j = 0; j < slices; j++) {
                float th0 = (float) (j * 2.0D * Math.PI / slices);
                float th1 = (float) ((j + 1) * 2.0D * Math.PI / slices);
                float x00 = Mth.cos(th0) * ringR0, z00 = Mth.sin(th0) * ringR0;
                float x01 = Mth.cos(th1) * ringR0, z01 = Mth.sin(th1) * ringR0;
                float x10 = Mth.cos(th0) * ringR1, z10 = Mth.sin(th0) * ringR1;
                float x11 = Mth.cos(th1) * ringR1, z11 = Mth.sin(th1) * ringR1;

                ClientAuraRenderer.emitCrystalQuad(vc, sphereMat,
                        x00, y0, z00, r0, g0, b0, a0,
                        x01, y0, z01, r0, g0, b0, a0,
                        x11, y1, z11, r1, g1, b1, a1,
                        x10, y1, z10, r1, g1, b1, a1);
                ClientAuraRenderer.emitCrystalQuad(vc, sphereMat,
                        x10, y1, z10, r1, g1, b1, a1,
                        x11, y1, z11, r1, g1, b1, a1,
                        x01, y0, z01, r0, g0, b0, a0,
                        x00, y0, z00, r0, g0, b0, a0);
            }
        }
        poseStack.popPose();

        renderCelestialOrbitRing(poseStack, vc, rSphere * 1.03F, 0.022F, 24.0F, 12.0F, time * 2.6F, 160, 235, 255, 135);
        renderCelestialOrbitRing(poseStack, vc, rSphere * 1.06F, 0.018F, -32.0F, -18.0F, -time * 1.9F, 150, 175, 255, 110);

        int stars = ClientAuraTracker.getNightsFocusStars(entity);
        if (stars > 0) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.4F));
            float[][] archOffsets = {
                    {-0.56F, 0.34F, 0.22F},
                    {-0.24F, 0.56F, 0.34F},
                    { 0.24F, 0.56F, 0.34F},
                    { 0.56F, 0.34F, 0.22F}
            };
            Matrix4f archMat = poseStack.last().pose();
            for (int s = 0; s < stars; s++) {
                float sx = archOffsets[s][0] * rSphere;
                float sy = archOffsets[s][1] * rSphere + 0.03F * Mth.sin(time * 0.16F + s * 1.4F);
                float sz = archOffsets[s][2] * rSphere;
                float ceilY = (float) Math.sqrt(Math.max(0.04F, rSphere * rSphere - sx * sx - sz * sz));
                float tw = 0.012F;
                ClientAuraRenderer.emitCrystalQuad(vc, archMat,
                        sx - tw, sy + 0.10F, sz, 180, 235, 255, 165,
                        sx + tw, sy + 0.10F, sz, 180, 235, 255, 165,
                        sx + tw, ceilY, sz, 140, 210, 255, 70,
                        sx - tw, ceilY, sz, 140, 210, 255, 70);
                ClientAuraRenderer.emitCrystalQuad(vc, archMat,
                        sx, sy + 0.10F, sz - tw, 180, 235, 255, 165,
                        sx, sy + 0.10F, sz + tw, 180, 235, 255, 165,
                        sx, ceilY, sz + tw, 140, 210, 255, 70,
                        sx, ceilY, sz - tw, 140, 210, 255, 70);

                poseStack.pushPose();
                poseStack.translate(sx, sy, sz);
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.0F + s * 45.0F));
                renderFourPointStar3D(vc, poseStack.last().pose(), 0.14F, 0.05F, 235, 250, 255, 250, 125, 195, 255, 125);
                poseStack.popPose();
            }
            poseStack.popPose();
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
        renderFourPointStar3D(consumer, poseStack.last().pose(), 0.22F, 0.07F, 240, 252, 255, 255, 110, 210, 255, 130);
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

    public static boolean applyAuxiliaryStaffHumanoidAnimation(
            LivingEntity entity,
            HumanoidModel<?> model,
            float ageInTicks,
            int activeAnim,
            float t
    ) {
        if (entity.isUsingItem()) {
            ItemStack useStack = entity.getUseItem();
            if (useStack.getItem() instanceof ArcaneStaffItem staff) {
                ArcaneStaffItem.Kind useKind = ArcaneStaffItem.getEffectiveKind(useStack);
                int elapsed = Math.max(0, staff.getUseDuration(useStack) - entity.getUseItemRemainingTicks());
                if (useKind == ArcaneStaffItem.Kind.SPLENDOR && elapsed >= 2 && activeAnim == 0) {
                    float w = smoothStep01((elapsed - 1.5F) / 7.5F);
                    HumanoidArm castArm = resolveStaffArm(entity, ArcaneStaffItem.Kind.SPLENDOR);
                    var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
                    float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                    float offDir = -castDir;

                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, -0.68F);
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.18F);
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.04F);
                    offPart.xRot = Mth.lerp(w, offPart.xRot, -0.35F);
                    offPart.yRot = Mth.lerp(w, offPart.yRot, -offDir * 0.18F);
                    offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.22F);
                    return true;
                }
                if (useKind == ArcaneStaffItem.Kind.GHASTFIRE) {
                    float w = smoothStep01(elapsed / 16.0F);
                    float tremor = Mth.sin(ageInTicks * 1.8F) * 0.025F * w;
                    HumanoidArm castArm = resolveStaffArm(entity, ArcaneStaffItem.Kind.GHASTFIRE);
                    var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, -2.65F + tremor);
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.14F);
                    return true;
                }
            }
        }
        if (activeAnim == ClientAuraTracker.ANIM_GHASTFIRE_SLAM) {
            float slam = smoothStep01(t / 0.35F);
            float w = (t < 0.45F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.45F) / 0.55F) * (float) Math.PI));
            HumanoidArm castArm = resolveStaffArm(entity, ArcaneStaffItem.Kind.GHASTFIRE);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slam, -2.65F, -0.25F));
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.10F * (1.0F - slam));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON) {
            float w;
            if (t < 0.18F) {
                w = smoothStep01(t / 0.18F);
            } else if (t < 0.75F) {
                w = 1.0F;
            } else {
                w = 1.0F - smoothStep01((t - 0.75F) / 0.25F);
            }
            float curtsy = Mth.sin(Mth.clamp(t / 0.40F, 0.0F, 1.0F) * ((float) Math.PI * 0.5F));

            HumanoidArm castArm = resolveStaffArm(entity, ArcaneStaffItem.Kind.SPLENDOR);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;

            model.body.xRot = Mth.lerp(w, model.body.xRot, 0.18F * curtsy);
            model.body.yRot = Mth.lerp(w, model.body.yRot, castDir * -0.15F * curtsy);
            model.head.xRot += 0.12F * curtsy * w;
            model.hat.copyFrom(model.head);

            model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.20F * curtsy);
            model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.28F * curtsy);

            mainPart.xRot = Mth.lerp(w, mainPart.xRot, -0.55F);
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.22F);
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.12F);

            offPart.xRot = Mth.lerp(w, offPart.xRot, -0.85F);
            offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * 0.75F);
            offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.65F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SWITCH) {
            float w;
            if (t < 0.12F) {
                w = smoothStep01(t / 0.12F);
            } else if (t < 0.88F) {
                w = 1.0F;
            } else {
                w = 1.0F - smoothStep01((t - 0.88F) / 0.12F);
            }

            HumanoidArm castArm = resolveStaffArm(entity, ArcaneStaffItem.Kind.SPLENDOR);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;

            float bow = Mth.sin(t * (float) Math.PI);
            model.body.xRot = Mth.lerp(w, model.body.xRot, 0.08F * bow);
            model.body.yRot = Mth.lerp(w, model.body.yRot, castDir * 0.12F * bow);
            model.body.zRot = Mth.lerp(w, model.body.zRot, castDir * 0.04F * bow);
            model.head.xRot = Mth.lerp(w, model.head.xRot, 0.12F * bow);
            model.head.yRot = Mth.lerp(w, model.head.yRot, -castDir * 0.10F * bow);
            model.hat.copyFrom(model.head);

            model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.10F * bow);
            model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.10F * bow);

            mainPart.xRot = Mth.lerp(w, mainPart.xRot, -0.42F);
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * 0.28F);
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.16F);

            offPart.xRot = Mth.lerp(w, offPart.xRot, -0.75F);
            offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * 0.52F);
            offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.38F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_FURINA_BURST) {
            float w = (t < 0.18F) ? smoothStep01(t / 0.18F) : (t < 0.80F ? 1.0F : 1.0F - smoothStep01((t - 0.80F) / 0.20F));
            model.body.xRot = Mth.lerp(w, model.body.xRot, -0.06F);
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.35F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, 0.55F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.85F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.35F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, -0.55F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.85F);
            model.head.xRot = Mth.lerp(w, model.head.xRot, -0.18F);
            model.hat.copyFrom(model.head);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_LAYLA_BURST) {
            float w = (t < 0.20F) ? smoothStep01(t / 0.20F) : (t < 0.78F ? 1.0F : 1.0F - smoothStep01((t - 0.78F) / 0.22F));
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -2.25F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -2.25F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.38F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.38F);
            model.head.xRot -= 0.20F * w;
            model.hat.copyFrom(model.head);
            return true;
        }
        return false;
    }

    public static boolean applyAuxiliaryThirdPersonItemTransform(
            LivingEntity entity,
            ItemStack stack,
            ArcaneStaffItem staff,
            ArcaneStaffItem.Kind effKind,
            boolean leftHand,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (effKind != ArcaneStaffItem.Kind.SPLENDOR) return false;
        float plantW = 0.0F;
        float downOffset = 0.0F;
        float targetPitchDeg = -103.0F;
        if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            if (t < 0.16F) {
                plantW = Mth.sin((t / 0.16F) * ((float) Math.PI * 0.5F));
            } else if (t < 0.72F) {
                plantW = 1.0F;
            } else {
                plantW = 0.5F * (1.0F + Mth.cos(((t - 0.72F) / 0.28F) * (float) Math.PI));
            }
            float slamDown = smoothStep01(t / 0.24F);
            downOffset = slamDown * 0.14F;
            float armDeg = Mth.lerp(slamDown, -39.0F, -23.0F);
            targetPitchDeg = armDeg - 80.0F;
        } else if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SWITCH && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.12F) ? smoothStep01(t / 0.12F) : (t < 0.88F ? 1.0F : 1.0F - smoothStep01((t - 0.88F) / 0.12F));
            float side = leftHand ? -1.0F : 1.0F;
            poseStack.translate(side * 0.04F * w, -0.08F * w, 0.06F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(28.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 14.0F * w));
        } else if (entity.isUsingItem() && entity.getUseItem() == stack) {
            int elapsed = Math.max(0, staff.getUseDuration(stack) - entity.getUseItemRemainingTicks());
            if (elapsed >= 2) {
                plantW = smoothStep01((elapsed - 1.5F) / 7.5F);
                float armDeg = Mth.lerp(plantW, -24.0F, -39.0F);
                targetPitchDeg = armDeg - 80.0F;
            }
        }
        if (plantW > 0.001F) {
            float side = leftHand ? -1.0F : 1.0F;
            poseStack.translate(0.0F, 0.06F * plantW, -0.08F * plantW + downOffset);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 10.3F * plantW));
            poseStack.mulPose(Axis.XP.rotationDegrees(targetPitchDeg * plantW));
        }
        return true;
    }

    public static boolean applyAuxiliaryFirstPersonTransform(
            AbstractClientPlayer player,
            float partialTicks,
            ItemStack stack,
            ArcaneStaffItem staff,
            ArcaneStaffItem.Kind effKind,
            float dir,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (effKind == ArcaneStaffItem.Kind.SPLENDOR) {
            if (player.isUsingItem() && player.getUseItem() == stack && activeAnim == 0) {
                int elapsed = Math.max(0, staff.getUseDuration(stack) - player.getUseItemRemainingTicks());
                if (elapsed >= 2) {
                    float w = smoothStep01((elapsed + partialTicks - 1.5F) / 7.5F);
                    poseStack.translate(-dir * 0.20F * w, -0.08F * w, -0.16F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-195.0F * w));
                    return true;
                }
            }
            if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SWITCH && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float w = (t < 0.12F) ? smoothStep01(t / 0.12F) : (t < 0.88F ? 1.0F : 1.0F - smoothStep01((t - 0.88F) / 0.12F));
                float dip = Mth.sin(t * (float) Math.PI);
                poseStack.translate(-dir * 0.18F * w, (-0.12F - 0.06F * dip) * w, -0.14F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees((-22.0F - 12.0F * dip) * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 16.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 10.0F * w));
                return true;
            }
            if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float w;
                if (t < 0.16F) {
                    w = Mth.sin((t / 0.16F) * ((float) Math.PI * 0.5F));
                } else if (t < 0.70F) {
                    w = 1.0F;
                } else {
                    w = 0.5F * (1.0F + Mth.cos(((t - 0.70F) / 0.30F) * (float) Math.PI));
                }
                float lift = (t < 0.14F) ? Mth.sin((t / 0.14F) * (float) Math.PI) : 0.0F;
                float plantDown = smoothStep01(Math.max(0.0F, t - 0.08F) / 0.20F);

                poseStack.translate(-dir * 0.20F * w, (-0.06F * lift - 0.28F * plantDown) * w, -0.18F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(plantDown, -165.0F, -200.0F) * w));
                return true;
            }
        } else if (effKind == ArcaneStaffItem.Kind.GHASTFIRE) {
            if (player.isUsingItem() && player.getUseItem() == stack) {
                int elapsed = Math.max(0, staff.getUseDuration(stack) - player.getUseItemRemainingTicks());
                float w = smoothStep01((elapsed + partialTicks) / 16.0F);
                float tremor = Mth.sin((player.tickCount + partialTicks) * 1.8F) * 0.005F * w;
                poseStack.translate(dir * 0.22F * w + tremor, 0.38F * w, -0.06F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(-32.0F * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 18.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 16.0F * w));
                return true;
            }
            if (activeAnim == ClientAuraTracker.ANIM_GHASTFIRE_SLAM && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float slam = smoothStep01(t / 0.35F);
                float w = (t < 0.45F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.45F) / 0.55F) * (float) Math.PI));
                poseStack.translate(dir * 0.22F * (1.0F - slam) * w, Mth.lerp(slam, 0.38F, -0.22F) * w, Mth.lerp(slam, -0.06F, -0.24F) * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slam, -32.0F, -68.0F) * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 18.0F * (1.0F - slam) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 16.0F * (1.0F - slam) * w));
                return true;
            }
        }
        if (data != null && (activeAnim == ClientAuraTracker.ANIM_FURINA_BURST || activeAnim == ClientAuraTracker.ANIM_LAYLA_BURST)) {
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

    public static boolean setupFirstPersonSplendorOffhandPose(
            AbstractClientPlayer player,
            HumanoidArm offArm,
            PoseStack poseStack
    ) {
        float offDir = (offArm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;
        if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w;
            if (t < 0.18F) {
                w = Mth.sin((t / 0.18F) * ((float) Math.PI * 0.5F));
            } else if (t < 0.72F) {
                w = 1.0F;
            } else {
                w = 0.5F * (1.0F + Mth.cos(((t - 0.72F) / 0.28F) * (float) Math.PI));
            }
            if (w <= 0.02F) return false;
            poseStack.translate(-offDir * 0.12F * w, -0.45F * (1.0F - w) + 0.38F * w, -0.22F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(38.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(offDir * 18.0F * w));
            return true;
        }
        return false;
    }

    public static void renderGenericChargeBar(GuiGraphics guiGraphics, ArcaneStaffItem staff, float pct) {
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int barW = 56;
        int barH = 4;
        int x = (sw - barW) / 2;
        int y = sh / 2 + 12;
        guiGraphics.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0x90000000);
        Element elem = staff.getElement();
        Integer rgb = elem != null ? elem.color().getColor() : null;
        int elemColor = (rgb != null ? rgb : 0xFFFFFF) | 0xFF000000;
        int fillColor = pct >= 1.0F ? 0xFFFFE066 : elemColor;
        int fillW = Math.round(barW * pct);
        if (fillW > 0) {
            guiGraphics.fill(x, y, x + fillW, y + barH, fillColor);
        }
    }

    private static final java.util.Map<Integer, Integer> LAST_USE_TICKS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Integer, ArcaneStaffItem.Kind> LAST_USE_KIND = new java.util.concurrent.ConcurrentHashMap<>();

    public static void pollClientUseTransitions(LivingEntity entity) {
        if (entity == null) return;
        int id = entity.getId();
        if (entity.isUsingItem()) {
            ItemStack useStack = entity.getUseItem();
            if (useStack.getItem() instanceof ArcaneStaffItem staff) {
                ArcaneStaffItem.Kind k = ArcaneStaffItem.getEffectiveKind(useStack);
                if (k == ArcaneStaffItem.Kind.VORTEX
                        || k == ArcaneStaffItem.Kind.SPLENDOR
                        || k == ArcaneStaffItem.Kind.GHASTFIRE) {
                    int elapsed = Math.max(0, staff.getUseDuration(useStack) - entity.getUseItemRemainingTicks());
                    LAST_USE_TICKS.put(id, elapsed);
                    LAST_USE_KIND.put(id, k);
                    if (k == ArcaneStaffItem.Kind.SPLENDOR && elapsed == 10) {
                        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
                        if (d == null || System.currentTimeMillis() >= d.staffAnimUntil) {
                            ClientAuraTracker.triggerStaffAnimation(id, ClientAuraTracker.ANIM_SPLENDOR_SWITCH);
                        }
                    }
                    return;
                }
            }
        }
        Integer prevElapsed = LAST_USE_TICKS.remove(id);
        ArcaneStaffItem.Kind prevKind = LAST_USE_KIND.remove(id);
        if (prevElapsed != null && prevKind != null) {
            ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
            long now = System.currentTimeMillis();
            boolean alreadyTriggered = d != null && now < d.staffAnimUntil;
            if (!alreadyTriggered) {
                if (prevKind == ArcaneStaffItem.Kind.VORTEX) {
                    boolean heldFull = prevElapsed >= 26 || entity.isShiftKeyDown();
                    ClientAuraTracker.triggerStaffAnimation(id, heldFull ? ClientAuraTracker.ANIM_VORTEX_HOLD_BURST : ClientAuraTracker.ANIM_VORTEX_TAP);
                } else if (prevKind == ArcaneStaffItem.Kind.SPLENDOR && prevElapsed < 10) {
                    ClientAuraTracker.triggerStaffAnimation(
                            id,
                            entity.isShiftKeyDown() ? ClientAuraTracker.ANIM_SPLENDOR_SWITCH : ClientAuraTracker.ANIM_SPLENDOR_SUMMON
                    );
                } else if (prevKind == ArcaneStaffItem.Kind.GHASTFIRE && prevElapsed >= 6) {
                    ClientAuraTracker.triggerStaffAnimation(id, ClientAuraTracker.ANIM_GHASTFIRE_SLAM);
                }
            }
        }
        if (entity != Minecraft.getInstance().player && entity.swinging && entity.swingTime == 0 && ClientAuraTracker.isRaidenMusouActive(id)) {
            ItemStack main = entity.getMainHandItem();
            if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
                long now = System.currentTimeMillis();
                if (d == null || now >= d.staffAnimUntil) {
                    ClientAuraTracker.triggerStaffAnimation(id, ClientAuraTracker.ANIM_RAIDEN_SLASH);
                }
            }
        }
    }

    public static void renderChargeProgressHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        if (com.velorise.staves.client.character.NahidaVisualRenderer.renderViewfinderHud(guiGraphics, mc)) return;
        if (com.velorise.staves.client.character.TsaritsaVisualRenderer.renderHoldEChargeHud(guiGraphics)) return;
        if (com.velorise.staves.client.character.NeuvilletteVisualRenderer.renderChannelingHud(guiGraphics, mc)) return;
        if (!mc.player.isUsingItem()) return;
        ItemStack stack = mc.player.getUseItem();
        ArcaneStaffItem.Kind hudKind = ArcaneStaffItem.getEffectiveKind(stack);
        if (!(stack.getItem() instanceof ArcaneStaffItem staff) || hudKind == null) return;
        int elapsed = Math.max(0, staff.getUseDuration(stack) - mc.player.getUseItemRemainingTicks());
        int targetTicks = switch (hudKind) {
            case VORTEX -> 28;
            case SPLENDOR -> 10;
            case THOUSAND_BLAZING_SUNS -> 7;
            case GHASTFIRE -> 40;
            case LIGHTNING -> 60;
            case GUARDIAN -> ArcaneStaffItem.isGuardianInstant(stack) ? 60 : (elapsed < 60 ? 60 : 120);
            default -> 30;
        };
        float pct = Mth.clamp((float) elapsed / (float) targetTicks, 0.0F, 1.0F);
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        if (hudKind == ArcaneStaffItem.Kind.GUARDIAN) {
            com.velorise.staves.client.character.NeuvilletteVisualRenderer.renderUseChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.DAYBREAK || hudKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS
                || hudKind == ArcaneStaffItem.Kind.TSARITSA_BOW || hudKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR) {
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.VORTEX) {
            com.velorise.staves.client.character.ZhongliVisualRenderer.renderChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        if (hudKind == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            com.velorise.staves.client.character.MavuikaVisualRenderer.renderChargeHud(guiGraphics, sw, sh, pct);
            return;
        }
        renderGenericChargeBar(guiGraphics, staff, pct);
    }
}
