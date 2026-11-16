package com.velorise.staves.client.character;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.PlanetBefallMeteorRenderer;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated visual renderer for Geo Archon Zhongli (Vortex Vanquisher):
 * - Jade Shield barrier, Liyue diamond runes, and floating monolith armor plates
 * - Planet Befall charge HUD icon and meteor mesh delegation
 * - Dominus Lapidis Hold/Tap/Burst & Planet Befall 1st-person and 3rd-person animations
 */
public final class ZhongliVisualRenderer {
    private ZhongliVisualRenderer() {}

    public static void drawJadeArmorBox(
            VertexConsumer vc, Matrix4f mat,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int r, int g, int b, int a
    ) {
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y0, z1, r, g, b, a, x1, y0, z1, r, g, b, a, x1, y1, z1, r, g, b, a, x0, y1, z1, r, g, b, a);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x1, y0, z0, r, g, b, a, x0, y0, z0, r, g, b, a, x0, y1, z0, r, g, b, a, x1, y1, z0, r, g, b, a);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y1, z1, r, g, b, a, x1, y1, z1, r, g, b, a, x1, y1, z0, r, g, b, a, x0, y1, z0, r, g, b, a);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y0, z0, r, g, b, a, x1, y0, z0, r, g, b, a, x1, y0, z1, r, g, b, a, x0, y0, z1, r, g, b, a);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x0, y0, z0, r, g, b, a, x0, y0, z1, r, g, b, a, x0, y1, z1, r, g, b, a, x0, y1, z0, r, g, b, a);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, x1, y0, z1, r, g, b, a, x1, y0, z0, r, g, b, a, x1, y1, z0, r, g, b, a, x1, y1, z1, r, g, b, a);
    }

    public static void drawShieldDiamondRing(
            VertexConsumer vc, Matrix4f mat,
            float z, float cy, float rad, float thick,
            int r, int g, int b, int a
    ) {
        float rIn = Math.max(0.01F, rad - thick);
        float[][] pts = {
                {0.0F,  1.0F},
                {1.0F,  0.0F},
                {0.0F, -1.0F},
                {-1.0F, 0.0F}
        };
        for (int i = 0; i < 4; i++) {
            float[] p0 = pts[i];
            float[] p1 = pts[(i + 1) % 4];
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    p0[0] * rIn, cy + p0[1] * rIn, z, r, g, b, a,
                    p0[0] * rad, cy + p0[1] * rad, z, r, g, b, a,
                    p1[0] * rad, cy + p1[1] * rad, z, r, g, b, a,
                    p1[0] * rIn, cy + p1[1] * rIn, z, r, g, b, a);
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    p1[0] * rIn, cy + p1[1] * rIn, z, r, g, b, a,
                    p1[0] * rad, cy + p1[1] * rad, z, r, g, b, a,
                    p0[0] * rad, cy + p0[1] * rad, z, r, g, b, a,
                    p0[0] * rIn, cy + p0[1] * rIn, z, r, g, b, a);
        }
    }

    public static void renderJadeShieldBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (ClientAuraRenderer.isRenderingScreen() || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        if (!ClientAuraTracker.hasJadeShield(entity)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        float time = entity.tickCount + mc.getTimer().getGameTimeDeltaPartialTick(true);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
        Matrix4f mat = poseStack.last().pose();

        int segs = 16;
        float radius = Math.max(0.42F, entity.getBbWidth() * 0.62F);
        float h = entity.getBbHeight();
        float yBot = h * 0.23F;
        float yTop = h * 0.77F;
        float yMid = (yBot + yTop) * 0.5F;

        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2.0D * Math.PI / segs);
            float a1 = (float) ((i + 1) * 2.0D * Math.PI / segs);
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;

            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    x0, yBot, z0, 255, 200, 50, 14,
                    x1, yBot, z1, 255, 200, 50, 14,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x0, yMid, z0, 255, 212, 68, 56);
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    x0, yMid, z0, 255, 212, 68, 56,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x1, yBot, z1, 255, 200, 50, 14,
                    x0, yBot, z0, 255, 200, 50, 14);

            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    x0, yMid, z0, 255, 212, 68, 56,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x1, yTop, z1, 255, 228, 105, 14,
                    x0, yTop, z0, 255, 228, 105, 14);
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    x0, yTop, z0, 255, 228, 105, 14,
                    x1, yTop, z1, 255, 228, 105, 14,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x0, yMid, z0, 255, 212, 68, 56);

            float bandH = 0.018F;
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    x0, yBot, z0, 255, 218, 75, 130,
                    x1, yBot, z1, 255, 218, 75, 130,
                    x1, yBot + bandH, z1, 255, 238, 120, 0,
                    x0, yBot + bandH, z0, 255, 238, 120, 0);
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    x0, yTop - bandH, z0, 255, 238, 120, 0,
                    x1, yTop - bandH, z1, 255, 238, 120, 0,
                    x1, yTop, z1, 255, 218, 75, 130,
                    x0, yTop, z0, 255, 218, 75, 130);
        }

        float runeR = radius + 0.006F;
        float dSize = 0.11F;
        for (int k = 0; k < 4; k++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(k * 90.0F));
            Matrix4f rMat = poseStack.last().pose();
            drawShieldDiamondRing(vc, rMat, runeR, yMid, dSize, 0.015F, 255, 224, 82, 165);
            drawShieldDiamondRing(vc, rMat, runeR + 0.002F, yMid, dSize * 0.52F, 0.012F, 255, 245, 150, 195);
            poseStack.popPose();
        }
        poseStack.popPose();

        float camYaw = mc.gameRenderer.getMainCamera().getYRot();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camYaw));
        float bob = Mth.sin(time * 0.10F) * 0.02F;
        float plateDist = radius + 0.04F;

        float[][] corners = {
                {-0.31F, yTop - 0.14F + bob,  36.0F},
                { 0.31F, yTop - 0.14F - bob, -36.0F},
                {-0.31F, yBot + 0.14F - bob, -36.0F},
                { 0.31F, yBot + 0.14F + bob,  36.0F}
        };

        for (int c = 0; c < 4; c++) {
            float cx = corners[c][0];
            float cy = corners[c][1];
            float tiltDeg = corners[c][2];

            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                poseStack.translate(cx, cy, side * plateDist * 0.68F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(tiltDeg));
                Matrix4f pMat = poseStack.last().pose();

                drawJadeArmorBox(vc, pMat, -0.085F, -0.028F, -0.013F, 0.085F, 0.028F, 0.013F, 48, 30, 20, 235);
                drawJadeArmorBox(vc, pMat, -0.11F, -0.016F, -0.010F, -0.085F, 0.016F, 0.010F, 36, 22, 15, 235);
                drawJadeArmorBox(vc, pMat,  0.085F, -0.016F, -0.010F,  0.11F, 0.016F, 0.010F, 36, 22, 15, 235);
                float innerY = (cy > yMid) ? -0.036F : 0.028F;
                drawJadeArmorBox(vc, pMat, -0.075F, innerY, -0.009F, 0.075F, innerY + 0.011F, 0.009F, 255, 215, 64, 240);
                drawJadeArmorBox(vc, pMat, -0.06F, -0.006F, -0.014F, 0.06F, 0.006F, 0.014F, 245, 185, 48, 245);

                poseStack.popPose();
            }
        }
        poseStack.popPose();

        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }

    public static void renderEntityEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (ClientAuraRenderer.isRenderingScreen() || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        if (d == null) return;
        long nowMs = System.currentTimeMillis();
        float dt = d.zhongliIdleLastMs > 0L ? Math.min(0.1F, Math.max(0.0F, (nowMs - d.zhongliIdleLastMs) / 1000.0F)) : 0.016F;
        d.zhongliIdleLastMs = nowMs;
        if (d.zhongliIdleMeteor) {
            d.zhongliIdleFade = Math.min(1.0F, d.zhongliIdleFade + dt / 0.45F);
        } else {
            d.zhongliIdleFade = Math.max(0.0F, d.zhongliIdleFade - dt / 0.35F);
        }
        float fade = WindGliderRenderer.smoothStep01(d.zhongliIdleFade);
        if (fade <= 0.01F) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) return;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        // Orbit around the player, bobbing up and down, rotating randomly on all axes
        float orbitAngle = time * 0.045F;
        float orbitRadius = 0.92F;
        float ox = Mth.cos(orbitAngle) * orbitRadius;
        float oz = Mth.sin(orbitAngle) * orbitRadius;
        float wave = 0.65F * Mth.sin(time * 0.135F + 0.9F) + 0.35F * Mth.cos(time * 0.052F);
        float oyOffset = Mth.clamp(wave, -1.0F, 1.0F) * 0.375F;
        float oy = 1.425F + oyOffset;

        float rotY = time * 2.30F + Mth.sin(time * 0.06F) * 35.0F;
        float rotX = time * 1.60F + Mth.cos(time * 0.05F) * 40.0F;
        float rotZ = time * 1.15F;

        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        PlanetBefallMeteorRenderer.renderMeteorMesh(poseStack, buffer, 0.18F * fade, Math.max(8, Math.round(255.0F * fade)));
        poseStack.popPose();

    }

    public static void renderFirstPersonIdleMeteor(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(mc.player.getId());
        if (d == null) return;
        long nowMs = System.currentTimeMillis();
        float dt = d.zhongliIdleLastMs > 0L ? Math.min(0.1F, Math.max(0.0F, (nowMs - d.zhongliIdleLastMs) / 1000.0F)) : 0.016F;
        d.zhongliIdleLastMs = nowMs;
        if (d.zhongliIdleMeteor) {
            d.zhongliIdleFade = Math.min(1.0F, d.zhongliIdleFade + dt / 0.45F);
        } else {
            d.zhongliIdleFade = Math.max(0.0F, d.zhongliIdleFade - dt / 0.35F);
        }
        float fade = WindGliderRenderer.smoothStep01(d.zhongliIdleFade);
        if (fade <= 0.01F) return;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = mc.player.tickCount + pt;
        float orbitAngle = time * 0.045F;
        float orbitRadius = 0.92F;
        float ox = Mth.cos(orbitAngle) * orbitRadius;
        float oz = Mth.sin(orbitAngle) * orbitRadius;
        float wave = 0.65F * Mth.sin(time * 0.135F + 0.9F) + 0.35F * Mth.cos(time * 0.052F);
        float oy = 1.425F + Mth.clamp(wave, -1.0F, 1.0F) * 0.375F;

        float rotY = time * 2.30F + Mth.sin(time * 0.06F) * 35.0F;
        float rotX = time * 1.60F + Mth.cos(time * 0.05F) * 40.0F;
        float rotZ = time * 1.15F;

        Vec3 pPos = mc.player.getPosition(pt).subtract(camPos);
        poseStack.pushPose();
        poseStack.translate(pPos.x + ox, pPos.y + oy, pPos.z + oz);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        PlanetBefallMeteorRenderer.renderMeteorMesh(poseStack, bufferSource, 0.18F * fade, Math.max(8, Math.round(255.0F * fade)));
        poseStack.popPose();
    }

    public static void renderWorldMeteor(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        PlanetBefallMeteorRenderer.render(poseStack, bufferSource, camPos, now);
    }

    public static void renderChargeHud(GuiGraphics guiGraphics, int sw, int sh, float pct) {
        ResourceLocation meteorTex = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/planet_befall_charge.png");
        int iconX = sw / 2 - 8;
        int iconY = sh / 2 + 12;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.35F, 0.35F, 0.35F, 0.65F);
        guiGraphics.blit(meteorTex, iconX, iconY, 0, 0, 16, 16, 16, 16);
        int fillH = Math.max(1, Math.round(16 * pct));
        int topY = iconY + (16 - fillH);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(meteorTex, iconX, topY, 0, 16 - fillH, 16, fillH, 16, 16);
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (entity.isUsingItem() || entity.swinging) return false;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        if (d == null || (!d.zhongliIdleMeteor && d.zhongliIdleFade <= 0.01F)) return false;
        float w = WindGliderRenderer.smoothStep01(d.zhongliIdleFade);
        if (w <= 0.01F) return false;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        // Arms crossed across chest (matching Planet Befall Q stance)
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.28F);
        model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.22F);
        model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.78F);
        model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.78F);
        model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.22F);
        model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0.22F);
        // Wide regal stance (legs apart)
        model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.14F);
        model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.14F);
        model.rightLeg.yRot = Mth.lerp(w, model.rightLeg.yRot, 0.10F);
        model.leftLeg.yRot = Mth.lerp(w, model.leftLeg.yRot, -0.10F);
        model.head.xRot = Mth.lerp(w, model.head.xRot, 0.04F);
        model.hat.copyFrom(model.head);
        return true;
    }

    public static boolean applyHumanoidAnimation(
            LivingEntity entity,
            HumanoidModel<?> model,
            float ageInTicks,
            int activeAnim,
            float t
    ) {
        if (entity.isUsingItem()) {
            ItemStack useStack = entity.getUseItem();
            if (useStack.getItem() instanceof ArcaneStaffItem staff
                    && ArcaneStaffItem.getEffectiveKind(useStack) == ArcaneStaffItem.Kind.VORTEX) {
                int elapsed = Math.max(0, staff.getUseDuration(useStack, entity) - entity.getUseItemRemainingTicks());
                float w = WindGliderRenderer.smoothStep01(elapsed / 18.0F);
                float tremor = Mth.sin(ageInTicks * 1.65F) * 0.032F * w;
                model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.32F + tremor);
                model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.32F - tremor);
                model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.72F);
                model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.72F);
                model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.18F);
                model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0.18F);
                model.head.xRot += 0.15F * w;
                model.hat.copyFrom(model.head);
                return true;
            }
        }
        if (activeAnim == ClientAuraTracker.ANIM_VORTEX_HOLD_BURST) {
            float sweep = Mth.sin(Mth.clamp(t / 0.24F, 0.0F, 1.0F) * ((float) Math.PI * 0.5F));
            float w = (t < 0.64F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.64F) / 0.36F) * (float) Math.PI));

            float targetX = Mth.lerp(sweep, -1.32F, -1.45F);
            float targetRightY = Mth.lerp(sweep, -0.72F, 0.82F);
            float targetLeftY = Mth.lerp(sweep, 0.72F, -0.82F);
            float targetRightZ = Mth.lerp(sweep, -0.18F, 0.95F);
            float targetLeftZ = Mth.lerp(sweep, 0.18F, -0.95F);

            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, targetX);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, targetX);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, targetRightY);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, targetLeftY);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, targetRightZ);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, targetLeftZ);

            model.head.xRot -= 0.14F * sweep * w;
            model.hat.copyFrom(model.head);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_VORTEX_TAP) {
            float w = Mth.sin(t * (float) Math.PI);
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.VORTEX);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;

            mainPart.xRot = Mth.lerp(w, mainPart.xRot, -1.48F);
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.18F);
            offPart.xRot = Mth.lerp(w, offPart.xRot, 0.32F);
            offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.24F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL) {
            float w;
            if (t < 0.16F) {
                w = WindGliderRenderer.smoothStep01(t / 0.16F);
            } else if (t < 0.78F) {
                w = 1.0F;
            } else {
                w = 1.0F - WindGliderRenderer.smoothStep01((t - 0.78F) / 0.22F);
            }
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.28F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.22F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.78F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.78F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.22F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0.22F);
            model.head.xRot += 0.10F * w;
            model.hat.copyFrom(model.head);

            model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.22F);
            model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.22F);
            model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.05F);
            model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, 0.05F);
            return true;
        }
        return false;
    }

    public static boolean applyThirdPersonItemTransform(
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
        if (effKind != ArcaneStaffItem.Kind.VORTEX) return false;
        if (activeAnim == ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.12F) ? WindGliderRenderer.smoothStep01(t / 0.12F)
                    : (t < 0.80F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.80F) / 0.20F));
            float s = Math.max(0.001F, 1.0F - w);
            poseStack.scale(s, s, s);
        } else if (entity.isUsingItem() && entity.getUseItem() == stack) {
            int elapsed = Math.max(0, staff.getUseDuration(stack, entity) - entity.getUseItemRemainingTicks());
            float w = WindGliderRenderer.smoothStep01(elapsed / 18.0F);
            float side = leftHand ? -1.0F : 1.0F;
            poseStack.translate(side * 0.06F * w, 0.05F * w, 0.04F * w);
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 28.0F * w));
            poseStack.mulPose(Axis.XP.rotationDegrees(-22.0F * w));
        }
        return true;
    }

    public static boolean applyFirstPersonTransform(
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
        if (effKind != ArcaneStaffItem.Kind.VORTEX) return false;
        if (player.isUsingItem() && player.getUseItem() == stack) {
            int elapsed = Math.max(0, staff.getUseDuration(stack, player) - player.getUseItemRemainingTicks());
            float w = WindGliderRenderer.smoothStep01((elapsed + partialTicks) / 18.0F);
            float tremor = Mth.sin((player.tickCount + partialTicks) * 1.65F) * 0.010F * w;
            poseStack.translate(-dir * 0.48F * w + tremor, 0.14F * w, -0.18F * w);
            poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 14.0F * w));
            poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 12.0F * w));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.16F) ? WindGliderRenderer.smoothStep01(t / 0.16F)
                    : (t < 0.78F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.78F) / 0.22F));
            poseStack.translate(0.0F, -0.85F * w, 0.0F);
            float s = Math.max(0.01F, 1.0F - 0.85F * w);
            poseStack.scale(s, s, s);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_VORTEX_HOLD_BURST && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float sweep = Mth.sin(Mth.clamp(t / 0.24F, 0.0F, 1.0F) * ((float) Math.PI * 0.5F));
            float w = (t < 0.64F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.64F) / 0.36F) * (float) Math.PI));

            float tx = Mth.lerp(sweep, -dir * 0.48F, dir * 0.14F) * w;
            float ty = Mth.lerp(sweep, 0.14F, 0.06F) * w;
            float tz = Mth.lerp(sweep, -0.18F, -0.10F) * w;
            poseStack.translate(tx, ty, tz);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(sweep, dir * 14.0F, -dir * 24.0F) * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(sweep, -dir * 12.0F, -dir * 32.0F) * w));
            return true;
        }
        return true;
    }

    public static boolean setupFirstPersonVortexMainArmPose(
            AbstractClientPlayer player,
            float partialTicks,
            HumanoidArm mainArm,
            PoseStack poseStack
    ) {
        if (player == null) return false;
        ItemStack mainStack = player.getMainHandItem();
        if (!(mainStack.getItem() instanceof ArcaneStaffItem staff)) return false;
        if (ArcaneStaffItem.getEffectiveKind(mainStack) != ArcaneStaffItem.Kind.VORTEX) return false;
        return setupFirstPersonVortexArmPose(player, staff, mainStack, partialTicks, mainArm, poseStack);
    }

    public static boolean setupFirstPersonVortexArmPose(
            AbstractClientPlayer player,
            ArcaneStaffItem staff,
            ItemStack mainStack,
            float partialTicks,
            HumanoidArm arm,
            PoseStack poseStack
    ) {
        float armDir = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (player.isUsingItem() && player.getUseItem() == mainStack) {
            int elapsed = Math.max(0, staff.getUseDuration(mainStack, player) - player.getUseItemRemainingTicks());
            float w = WindGliderRenderer.smoothStep01((elapsed + partialTicks) / 18.0F);
            if (w <= 0.02F) return false;
            float tremor = Mth.sin((player.tickCount + partialTicks) * 1.65F) * 0.010F * w;
            poseStack.translate(armDir * 0.06F * w + armDir * tremor, -0.22F * (1.0F - w) + 0.02F * w, -0.10F * w);
            poseStack.mulPose(Axis.ZP.rotationDegrees(armDir * 38.0F * w));
            poseStack.mulPose(Axis.XP.rotationDegrees(26.0F * w));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.16F) ? WindGliderRenderer.smoothStep01(t / 0.16F)
                    : (t < 0.78F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.78F) / 0.22F));
            if (w <= 0.02F) return false;
            poseStack.translate(-armDir * 0.22F * w, -0.06F * w + 0.03F * armDir * w, -0.14F * w);
            poseStack.mulPose(Axis.ZP.rotationDegrees(armDir * 68.0F * w));
            poseStack.mulPose(Axis.XP.rotationDegrees(28.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(-armDir * 18.0F * w));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_VORTEX_HOLD_BURST && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float sweep = Mth.sin(Mth.clamp(t / 0.16F, 0.0F, 1.0F) * ((float) Math.PI * 0.5F));
            float w = (t < 0.64F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.64F) / 0.36F) * (float) Math.PI));
            if (w <= 0.02F) return false;

            float tx = Mth.lerp(sweep, armDir * 0.06F, armDir * 0.34F);
            float ty = Mth.lerp(sweep, 0.02F, -0.04F) - 0.22F * (1.0F - w);
            poseStack.translate(tx, ty, -0.10F * w);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(sweep, armDir * 38.0F, armDir * 18.0F) * w));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(sweep, 26.0F, 20.0F) * w));
            return true;
        }
        return false;
    }
}
