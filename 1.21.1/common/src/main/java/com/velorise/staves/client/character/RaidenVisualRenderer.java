package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.item.ArcaneStaffItem;
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
    private static final ResourceLocation RAIDEN_CHAKRA_RING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_chakra_ring.png");
    private static final ResourceLocation RAIDEN_EYE_AURA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_eye_aura.png");
    private static final ResourceLocation RAIDEN_MUSOU_SLASH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_musou_slash.png");
    private static final ResourceLocation RAIDEN_ROBOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_robot_narukami_atlas.png");
    private static final ResourceLocation RAIDEN_COSMIC_EYE_RIFT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_cosmic_eye_rift.png");
    private static final ResourceLocation RAIDEN_EYE_SYMBOL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_eye_symbol.png");
    private static final ResourceLocation RAIDEN_HITOTACHI_SLASH_BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_hitotachi_slash_beam.png");
    private static final ResourceLocation RAIDEN_COSMIC_BACKDROP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_cosmic_backdrop.png");
    private static final ResourceLocation RAIDEN_STARBURST_RAYS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_starburst_rays.png");
    // Deterministic dimensional glass shatter shards flying outward on Hitotachi cut (angleDeg, speed, sizeW, sizeH, rotSpeed, isBright)
    private static final float[][] SHATTER_SHARDS = {
            {-165.0F, 1.15F, 26.0F, 9.0F, 140.0F, 1.0F}, {-142.0F, 0.92F, 18.0F, 7.0F, -110.0F, 0.0F},
            {-118.0F, 1.28F, 22.0F, 8.0F, 170.0F, 1.0F}, {-88.0F,  1.05F, 15.0F, 6.0F, -145.0F, 0.0F},
            {-58.0F,  1.32F, 24.0F, 8.0F, 125.0F, 1.0F}, {-32.0F,  1.45F, 30.0F, 9.0F, -160.0F, 1.0F},
            {-14.0F,  1.60F, 34.0F, 7.0F, 95.0F,  1.0F}, {12.0F,   1.38F, 28.0F, 8.0F, -135.0F, 0.0F},
            {38.0F,   1.18F, 20.0F, 7.0F, 155.0F, 1.0F}, {66.0F,   1.02F, 16.0F, 6.0F, -120.0F, 0.0F},
            {96.0F,   1.22F, 22.0F, 8.0F, 130.0F, 1.0F}, {126.0F,  1.35F, 25.0F, 8.0F, -150.0F, 0.0F},
            {154.0F,  1.50F, 32.0F, 9.0F, 115.0F, 1.0F}, {176.0F,  1.42F, 28.0F, 7.0F, -90.0F,  1.0F},
            {-72.0F,  0.82F, 14.0F, 5.0F, 180.0F, 0.0F}, {112.0F,  0.86F, 15.0F, 6.0F, -175.0F, 0.0F}
    };

    private static void renderFirstPersonMusouEyeOverlay(GuiGraphics guiGraphics, int sw, int sh, long elapsedMs) {
        if (elapsedMs > 1650L) return;
        float cx0 = sw * 0.5F;
        float cy0 = sh * 0.5F;

        // 1. Plane of Euthymia cosmic galaxy backdrop (Alpha Blend, deep coverage like Genshin Ulti)
        float bgFrac;
        if (elapsedMs < 220L) {
            bgFrac = WindGliderRenderer.smoothStep01(elapsedMs / 220.0F);
        } else if (elapsedMs <= 1220L) {
            bgFrac = 1.0F;
        } else {
            bgFrac = 1.0F - WindGliderRenderer.smoothStep01((elapsedMs - 1220.0F) / 380.0F);
        }
        float bgAlpha = Mth.clamp(0.94F * bgFrac, 0.0F, 1.0F);
        if (bgAlpha > 0.01F) {
            guiGraphics.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, bgAlpha);
            guiGraphics.blit(RAIDEN_COSMIC_BACKDROP_TEXTURE, 0, 0, sw, sh, 0.0F, 0.0F, 512, 256, 512, 256);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // 2. Eye timeline: Phase 1 (0-220ms slit), Phase 2 (220-420ms snap-open + 420-980ms almond hold), Phase 3 (980-1220ms collapse)
        if (elapsedMs <= 1260L) {
            float riftScaleX;
            float riftScaleY;
            float riftAlpha;
            float symScale;
            float symAlpha;
            float jitterX = 0.0F;
            float jitterY = 0.0F;

            if (elapsedMs < 220L) {
                float p = WindGliderRenderer.smoothStep01(elapsedMs / 220.0F);
                riftScaleX = Mth.lerp(p, 0.70F, 1.05F);
                riftScaleY = Mth.lerp(p, 0.02F, 0.11F);
                riftAlpha = Mth.lerp(p, 0.30F, 0.95F);
                symScale = 0.52F;
                symAlpha = 0.0F;
            } else if (elapsedMs < 420L) {
                // Snappy Genshin-style eye opening (cubic ease-out)
                float p = (elapsedMs - 220.0F) / 200.0F;
                float openEase = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);
                riftScaleX = Mth.lerp(openEase, 1.05F, 1.0F);
                riftScaleY = Mth.lerp(openEase, 0.11F, 0.86F);
                riftAlpha = 1.0F;
                symScale = Mth.lerp(openEase, 0.56F, 1.02F);
                symAlpha = Mth.clamp(openEase * 1.25F, 0.0F, 1.0F);
            } else if (elapsedMs < 980L) {
                // Fierce almond-eye hold with subtle breathing pulse & micro energy tremor
                float holdT = (elapsedMs - 420.0F) * 0.015F;
                float pulse = 0.025F * Mth.sin(holdT);
                riftScaleX = 1.0F + pulse * 0.5F;
                riftScaleY = 0.86F + pulse;
                riftAlpha = 1.0F;
                symScale = 1.0F + 0.03F * Mth.sin(holdT * 1.3F);
                symAlpha = 1.0F;
                jitterX = Mth.sin(elapsedMs * 0.09F) * 0.85F;
                jitterY = Mth.cos(elapsedMs * 0.11F) * 0.65F;
            } else {
                // Rapid vertical collapse + horizontal stretch into slash line
                float p = WindGliderRenderer.smoothStep01((elapsedMs - 980.0F) / 240.0F);
                riftScaleX = Mth.lerp(p, 1.0F, 1.58F);
                riftScaleY = Mth.lerp(p, 0.86F, 0.05F);
                riftAlpha = 1.0F - p * 0.28F;
                symScale = Mth.lerp(p, 1.0F, 1.22F);
                symAlpha = 1.0F - WindGliderRenderer.smoothStep01(p * 1.45F);
            }

            // Edge-to-edge almond eye dimensions (slash tips extend past left/right screen edges)
            int riftW = Math.max(320, Math.round(sw * 1.38F));
            int riftH = Math.max(115, Math.round(riftW * 0.36F));
            int symSize = Math.max(32, Math.round(riftH * 0.35F));

            // 2a. Authentic cosmic eye rift base (Normal Alpha Blend: dark void pupil cleanly masks the galaxy backdrop)
            if (riftAlpha > 0.02F) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(cx0 + jitterX, cy0 + jitterY, 0.0F);
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-9.0F));
                guiGraphics.pose().scale(riftScaleX, riftScaleY, 1.0F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp(riftAlpha * bgFrac, 0.0F, 1.0F));
                guiGraphics.blit(RAIDEN_COSMIC_EYE_RIFT_TEXTURE, -riftW / 2, -riftH / 2, riftW, riftH, 0.0F, 0.0F, 512, 256, 512, 256);
                guiGraphics.pose().popPose();
            }

            // 2b. Additive glowing plasma bloom + snap-open radial starburst rays + gently rotating Electro Mitsudomoe crest
            guiGraphics.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFunc(
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
            );

            // Radial starburst rays on Eye Snap-Open (210-500ms, matching Genshin 00:12)
            if (elapsedMs >= 210L && elapsedMs <= 520L) {
                float rp = Mth.clamp((elapsedMs - 210.0F) / 310.0F, 0.0F, 1.0F);
                float rayA = (rp < 0.24F) ? (rp / 0.24F) : (1.0F - WindGliderRenderer.smoothStep01((rp - 0.24F) / 0.76F));
                float rayScale = Mth.lerp(rp, 0.65F, 1.75F);
                int raySize = Math.max(sw, sh);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(cx0, cy0, 0.0F);
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-9.0F + rp * 8.0F));
                guiGraphics.pose().scale(rayScale, rayScale, 1.0F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 0.92F, 1.0F, Mth.clamp(rayA * 0.88F, 0.0F, 1.0F));
                guiGraphics.blit(RAIDEN_STARBURST_RAYS_TEXTURE, -raySize / 2, -raySize / 2, raySize, raySize, 0.0F, 0.0F, 512, 512, 512, 512);
                guiGraphics.pose().popPose();
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(cx0 + jitterX, cy0 + jitterY, 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-9.0F));
            guiGraphics.pose().scale(riftScaleX, riftScaleY, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp(riftAlpha, 0.0F, 1.0F));
            guiGraphics.blit(RAIDEN_COSMIC_EYE_RIFT_TEXTURE, -riftW / 2, -riftH / 2, riftW, riftH, 0.0F, 0.0F, 512, 256, 512, 256);
            guiGraphics.pose().popPose();

            if (symAlpha > 0.02F) {
                float rotProg = Mth.clamp((elapsedMs - 210.0F) / 970.0F, 0.0F, 1.0F);
                float snapSpin = WindGliderRenderer.smoothStep01(Mth.clamp((elapsedMs - 210.0F) / 280.0F, 0.0F, 1.0F));
                float symRotDeg = -9.0F + (-16.0F * (1.0F - snapSpin)) + (rotProg * 18.0F);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(cx0 + jitterX, cy0 + jitterY, 0.0F);
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(symRotDeg));
                guiGraphics.pose().scale(symScale, symScale, 1.0F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 0.96F, 1.0F, Mth.clamp(symAlpha, 0.0F, 1.0F));
                guiGraphics.blit(RAIDEN_EYE_SYMBOL_TEXTURE, -symSize / 2, -symSize / 2, symSize, symSize, 0.0F, 0.0F, 256, 256, 256, 256);
                guiGraphics.pose().popPose();
            }

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        }

        // 3. Phase 4 (1160-1650ms): Soft additive plasma Hitotachi slash beam synced with tick 28 (1400ms)
        if (elapsedMs >= 1160L && elapsedMs <= 1650L) {
            float fp = Mth.clamp((elapsedMs - 1160.0F) / 490.0F, 0.0F, 1.0F);
            float flashA = (fp < 0.22F)
                    ? WindGliderRenderer.smoothStep01(fp / 0.22F)
                    : (1.0F - WindGliderRenderer.smoothStep01((fp - 0.22F) / 0.78F));
            int screenFlashA = Math.round(92.0F * flashA);
            if (screenFlashA > 2) {
                guiGraphics.fill(0, 0, sw, sh, (screenFlashA << 24) | 0x6A2CE0);
            }

            guiGraphics.flush();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFunc(
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
            );

            int beamW = Math.round((float) Math.hypot(sw, sh) * 1.18F);
            int beamH = Math.max(28, Math.round(sh * 0.34F * Mth.lerp(flashA, 0.22F, 1.12F)));

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(cx0, cy0, 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-9.0F));
            // Main glowing plasma slash beam
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp(flashA, 0.0F, 1.0F));
            guiGraphics.blit(RAIDEN_HITOTACHI_SLASH_BEAM_TEXTURE, -beamW / 2, -beamH / 2, beamW, beamH, 0.0F, 0.0F, 512, 128, 512, 128);
            // Secondary razor-hot overbright core bloom
            int coreH = Math.max(12, Math.round(beamH * 0.42F));
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 0.94F, 1.0F, Mth.clamp(flashA * 0.78F, 0.0F, 1.0F));
            guiGraphics.blit(RAIDEN_HITOTACHI_SLASH_BEAM_TEXTURE, -beamW / 2, -coreH / 2, beamW, coreH, 0.0F, 0.0F, 512, 128, 512, 128);
            guiGraphics.pose().popPose();

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

            // Dimensional space-glass shatter shards flying outward (matching Genshin 00:16-00:18)
            float shP = Mth.clamp((elapsedMs - 1180.0F) / 470.0F, 0.0F, 1.0F);
            float shAlpha = (shP < 0.18F) ? (shP / 0.18F) : (1.0F - WindGliderRenderer.smoothStep01((shP - 0.18F) / 0.82F));
            if (shAlpha > 0.03F) {
                float travelBase = Math.max(sw, sh) * 0.52F * (1.0F - (1.0F - shP) * (1.0F - shP));
                for (float[] sd : SHATTER_SHARDS) {
                    float rad = (float) Math.toRadians(sd[0]);
                    float dist = travelBase * sd[1];
                    float sx = cx0 + Mth.cos(rad) * dist;
                    float sy = cy0 + Mth.sin(rad) * dist;
                    int hw = Math.max(2, Math.round(sd[2] * 0.5F * (1.0F - shP * 0.35F)));
                    int hh = Math.max(1, Math.round(sd[3] * 0.5F * (1.0F - shP * 0.35F)));
                    int aOuter = Math.round((sd[5] > 0.5F ? 225.0F : 190.0F) * shAlpha);
                    int colOuter = (sd[5] > 0.5F) ? 0xD896FF : 0x26124A;
                    int colInner = (sd[5] > 0.5F) ? 0xFFFFFF : 0x9F5CFF;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(sx, sy, 0.0F);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(sd[0] + shP * sd[4]));
                    guiGraphics.fill(-hw, -hh, hw, hh, (aOuter << 24) | colOuter);
                    guiGraphics.fill(-Math.max(1, hw / 2), -Math.max(1, hh / 2), Math.max(1, hw / 2), Math.max(1, hh / 2), (aOuter << 24) | colInner);
                    guiGraphics.pose().popPose();
                }
            }
        }
    }
    private static com.velorise.staves.client.model.RaidenRobotModel<net.minecraft.world.entity.Entity> robotModel;
    private static int raidenAttackHoldTicks = 0;

    private static com.velorise.staves.client.model.RaidenRobotModel<net.minecraft.world.entity.Entity> getOrBakeRobotModel() {
        if (robotModel == null) {
            robotModel = new com.velorise.staves.client.model.RaidenRobotModel<>(
                    com.velorise.staves.client.model.RaidenRobotModel.createBodyLayer().bakeRoot()
            );
        }
        return robotModel;
    }

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
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        float liftOffset = ClientAuraRenderer.getTotalLevitationOffsetY(entity, pt);
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float bodyYaw = (d != null && d.raidenIdleYawSet) ? d.raidenIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        boolean holdingRaiden = ArcaneStaffItem.getEffectiveKind(entity.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                || ArcaneStaffItem.getEffectiveKind(entity.getOffhandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (!isIdle && holdingRaiden && musouTicks <= 0.0F && (resolve > 0 || eyeTicks > 0.0F)) {
            float resolveFrac = (musouTicks > 0.0F || isIdle) ? 1.0F : Mth.clamp(resolve / 60.0F, 0.0F, 1.0F);
            ResourceLocation ringTex = RAIDEN_CHAKRA_RING_TEXTURE;
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
            vc.addVertex(mat, -half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat, -half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);

            vc.addVertex(mat, -half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat, -half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);

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
            ResourceLocation eyeTex = RAIDEN_EYE_AURA_TEXTURE;
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(eyeTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() + 0.36F + liftOffset + 0.04F * Mth.sin(time * 0.14F), 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, -0.22F);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float half = 0.34F;
            int cr = 255, cg = netherite ? 235 : 210, cb = 255, ca = 245;
            vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);

            vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
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
            long elapsedMs = Math.max(0L, nowMs - animData.staffAnimStartTime);
            renderFirstPersonMusouEyeOverlay(guiGraphics, sw, sh, elapsedMs);
            if (elapsedMs <= 1650L) {
                return;
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
        if (arc.color != 0xFFB84DFF && arc.color != 0xFFE0B0FF && arc.color != 0xFF9E42FF) return false;
        boolean isEyeProc = (arc.color == 0xFF9E42FF);
        boolean isMusouOrCa = (arc.color == 0xFFE0B0FF);
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float fade = (prog < 0.14F) ? (prog / 0.14F) : Math.max(0.0F, 1.0F - (prog - 0.14F) / 0.42F);
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
            double archDepth = isMusouOrCa ? 0.0D : (chordLen * 0.20D * (0.92D + prog * 0.16D));
            double maxHalfWidth;
            if (isEyeProc) {
                maxHalfWidth = 0.075D * (1.08D - prog * 0.32D); // Eye proc mỏng gấp đôi
            } else if (isMusouOrCa) {
                maxHalfWidth = 0.380D * (1.08D - prog * 0.32D); // CA & Ulti Musou mỏng lại còn 1x
            } else {
                maxHalfWidth = 0.220D * (1.08D - prog * 0.32D); // Musou NA
            }

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

    public static void renderSlashRiftTexture(
            ClientAuraTracker.ActiveArc arc,
            Vec3 startPos,
            Vec3 endPos,
            Vec3 camPos,
            long now,
            MultiBufferSource bufferSource,
            PoseStack.Pose pose
    ) {
        if (arc.color != 0xFFB84DFF && arc.color != 0xFFE0B0FF && arc.color != 0xFF9E42FF) return;
        boolean isEyeProc = (arc.color == 0xFF9E42FF);
        boolean isMusouOrCa = (arc.color == 0xFFE0B0FF);
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        // Keep space-tear visible through hair-thin filament collapse, fading cleanly in final 22%
        float fade = (prog < 0.08F) ? (prog / 0.08F) : ((prog < 0.78F) ? 1.0F : (1.0F - smootherStep01((prog - 0.78F) / 0.22F)));
        int alpha = Mth.clamp((int) (252.0F * fade), 0, 255);
        if (alpha <= 6) return;

        Vec3 sCam = startPos.subtract(camPos);
        Vec3 eCam = endPos.subtract(camPos);
        Vec3 slashDir = eCam.subtract(sCam);
        double chordLen = slashDir.length();
        if (chordLen <= 1.0E-4D) return;

        Vec3 midChord = sCam.lerp(eCam, 0.5D);
        double lengthScale = isEyeProc ? 1.0D : 1.35D;
        Vec3 halfSpan = eCam.subtract(sCam).scale(0.5D * lengthScale);
        sCam = midChord.subtract(halfSpan);
        eCam = midChord.add(halfSpan);
        slashDir = eCam.subtract(sCam);
        chordLen = slashDir.length();
        Vec3 unit = slashDir.scale(1.0D / chordLen);
        Vec3 toCam = midChord.scale(-1.0D);
        Vec3 perp = unit.cross(toCam.lengthSqr() > 1.0E-4D ? toCam.normalize() : new Vec3(0.0D, 1.0D, 0.0D));
        if (perp.lengthSqr() < 1.0E-4D) perp = unit.cross(new Vec3(1.0D, 0.0D, 0.0D));
        perp = perp.normalize();
        Vec3 curveOut = perp.cross(unit).normalize();
        if (curveOut.dot(toCam) > 0.0D) {
            curveOut = curveOut.scale(-1.0D);
        }

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(RAIDEN_MUSOU_SLASH_TEXTURE));
        Matrix4f mat = pose.pose();
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        int segs = 14;
        double archDepth = 0.0D; // Straight razor-cut space-tear rift
        // Authentic 3-stage Genshin space-tear lifecycle: snap-open -> cosmic hold -> hair-thin black filament collapse
        double widthCurve;
        if (prog < 0.14F) {
            float p = prog / 0.14F;
            widthCurve = Mth.lerp(1.0F - (1.0F - p) * (1.0F - p), 0.24F, 1.18F);
        } else if (prog < 0.46F) {
            float p = (prog - 0.14F) / 0.32F;
            widthCurve = Mth.lerp(p, 1.18F, 0.96F);
        } else {
            float p = (prog - 0.46F) / 0.54F;
            widthCurve = Mth.lerp(p * p, 0.96F, 0.045F);
        }
        double maxHalfWidth;
        if (isEyeProc) {
            maxHalfWidth = 0.135D * widthCurve; // Eye proc mỏng gấp đôi
        } else if (isMusouOrCa) {
            maxHalfWidth = 0.496D * widthCurve; // CA & Ulti Musou mỏng lại còn 1x
        } else {
            maxHalfWidth = 0.320D * widthCurve; // Musou NA
        }

        for (int i = 0; i < segs; i++) {
            float u0 = (float) i / (float) segs;
            float u1 = (float) (i + 1) / (float) segs;
            double s0 = Math.sin(u0 * Math.PI);
            double s1 = Math.sin(u1 * Math.PI);

            Vec3 c0 = sCam.lerp(eCam, u0).add(curveOut.scale(archDepth * s0));
            Vec3 c1 = sCam.lerp(eCam, u1).add(curveOut.scale(archDepth * s1));

            Vec3 top0 = c0.add(perp.scale(maxHalfWidth));
            Vec3 bot0 = c0.subtract(perp.scale(maxHalfWidth));
            Vec3 top1 = c1.add(perp.scale(maxHalfWidth));
            Vec3 bot1 = c1.subtract(perp.scale(maxHalfWidth));

            // Front face
            vc.addVertex(mat, (float) top0.x, (float) top0.y, (float) top0.z).setColor(255, 255, 255, alpha).setUv(u0, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
            vc.addVertex(mat, (float) top1.x, (float) top1.y, (float) top1.z).setColor(255, 255, 255, alpha).setUv(u1, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
            vc.addVertex(mat, (float) bot1.x, (float) bot1.y, (float) bot1.z).setColor(255, 255, 255, alpha).setUv(u1, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
            vc.addVertex(mat, (float) bot0.x, (float) bot0.y, (float) bot0.z).setColor(255, 255, 255, alpha).setUv(u0, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);

            // Back face
            vc.addVertex(mat, (float) bot0.x, (float) bot0.y, (float) bot0.z).setColor(255, 255, 255, alpha).setUv(u0, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, -1.0F, 0.0F);
            vc.addVertex(mat, (float) bot1.x, (float) bot1.y, (float) bot1.z).setColor(255, 255, 255, alpha).setUv(u1, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, -1.0F, 0.0F);
            vc.addVertex(mat, (float) top1.x, (float) top1.y, (float) top1.z).setColor(255, 255, 255, alpha).setUv(u1, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, -1.0F, 0.0F);
            vc.addVertex(mat, (float) top0.x, (float) top0.y, (float) top0.z).setColor(255, 255, 255, alpha).setUv(u0, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, -1.0F, 0.0F);
        }
    }

    // =========================================================================
    // Musou Isshin Tick-Accurate Multi-Keyframe Animation Engine (20 TPS = 50ms/tick)
    // Researched frame-by-frame (20fps) from Genshin Impact Raiden Shogun Showcase:
    //   - Ultimate (ANIM_RAIDEN_DRAW, 54t / 2700ms): chest draw -> coil -> Hitotachi cleave -> front flourish -> dissolve
    //   - N1 (step 0, 36t / 1800ms): diagonal top-left -> bottom-right -> hip-back rest -> dissolve
    //   - N2 (step 1, 38t / 1900ms): flat horizontal right -> left -> right hold -> vertical wrist spin -> dissolve
    //   - N3 (step 2, 46t / 2300ms): steep rising vertical slash -> high hold -> slow arc to horizontal -> flick dissolve
    //   - N4 (step 3, 50t / 2500ms): rapid 2-hit cross spin -> low diagonal hold -> vertical lift -> horizontal flick dissolve
    //   - N5 (step 4, 48t / 2400ms): low iaijutsu crouch -> wide horizontal space-cleave -> crouched sheathe -> dissolve -> rise
    //   - Charged Attack (ANIM_RAIDEN_CA, 44t / 2200ms): low iai coil -> twin X-crescent dash -> low hold -> vertical dissolve
    // =========================================================================
    private static final class MusouKey {
        final float tick;
        final float bodyX, bodyY, bodyZ, crouchY;
        final float headX, headY;
        final float mainX, mainY, mainZ;
        final float offX, offY, offZ;
        final float legLX, legLZ, legRX, legRZ;
        // Third-person held sword local transform (applied before handheld model display)
        final float swTx, swTy, swTz;
        final float swRx, swRy, swRz;
        // Sword materialization / dissolution alpha [0.0 = hidden, 1.0 = fully visible]
        final float swAlpha;
        // Pose blend weight [0.0 = vanilla idle, 1.0 = full choreography pose]
        final float poseWeight;

        MusouKey(
                float tick,
                float bodyX, float bodyY, float bodyZ, float crouchY,
                float headX, float headY,
                float mainX, float mainY, float mainZ,
                float offX, float offY, float offZ,
                float legLX, float legLZ, float legRX, float legRZ,
                float swTx, float swTy, float swTz,
                float swRx, float swRy, float swRz,
                float swAlpha, float poseWeight
        ) {
            this.tick = tick;
            this.bodyX = bodyX; this.bodyY = bodyY; this.bodyZ = bodyZ; this.crouchY = crouchY;
            this.headX = headX; this.headY = headY;
            this.mainX = mainX; this.mainY = mainY; this.mainZ = mainZ;
            this.offX = offX; this.offY = offY; this.offZ = offZ;
            this.legLX = legLX; this.legLZ = legLZ; this.legRX = legRX; this.legRZ = legRZ;
            this.swTx = swTx; this.swTy = swTy; this.swTz = swTz;
            this.swRx = swRx; this.swRy = swRy; this.swRz = swRz;
            this.swAlpha = swAlpha;
            this.poseWeight = poseWeight;
        }
    }

    private static final class SampledMusouPose {
        float bodyX, bodyY, bodyZ, crouchY;
        float headX, headY;
        float mainX, mainY, mainZ;
        float offX, offY, offZ;
        float legLX, legLZ, legRX, legRZ;
        float swTx, swTy, swTz;
        float swRx, swRy, swRz;
        float swAlpha;
        float poseWeight;
    }

    private static float smootherStep01(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static SampledMusouPose sampleTrack(MusouKey[] keys, float tick) {
        SampledMusouPose out = new SampledMusouPose();
        if (keys == null || keys.length == 0) return out;
        if (tick <= keys[0].tick) {
            copyKey(keys[0], out);
            return out;
        }
        MusouKey last = keys[keys.length - 1];
        if (tick >= last.tick) {
            copyKey(last, out);
            return out;
        }
        int idx = 0;
        for (int i = 0; i < keys.length - 1; i++) {
            if (tick >= keys[i].tick && tick <= keys[i + 1].tick) {
                idx = i;
                break;
            }
        }
        MusouKey a = keys[idx];
        MusouKey b = keys[idx + 1];
        float span = Math.max(0.0001F, b.tick - a.tick);
        float u = smootherStep01((tick - a.tick) / span);
        out.bodyX = Mth.lerp(u, a.bodyX, b.bodyX);
        out.bodyY = Mth.lerp(u, a.bodyY, b.bodyY);
        out.bodyZ = Mth.lerp(u, a.bodyZ, b.bodyZ);
        out.crouchY = Mth.lerp(u, a.crouchY, b.crouchY);
        out.headX = Mth.lerp(u, a.headX, b.headX);
        out.headY = Mth.lerp(u, a.headY, b.headY);
        out.mainX = Mth.lerp(u, a.mainX, b.mainX);
        out.mainY = Mth.lerp(u, a.mainY, b.mainY);
        out.mainZ = Mth.lerp(u, a.mainZ, b.mainZ);
        out.offX = Mth.lerp(u, a.offX, b.offX);
        out.offY = Mth.lerp(u, a.offY, b.offY);
        out.offZ = Mth.lerp(u, a.offZ, b.offZ);
        out.legLX = Mth.lerp(u, a.legLX, b.legLX);
        out.legLZ = Mth.lerp(u, a.legLZ, b.legLZ);
        out.legRX = Mth.lerp(u, a.legRX, b.legRX);
        out.legRZ = Mth.lerp(u, a.legRZ, b.legRZ);
        out.swTx = Mth.lerp(u, a.swTx, b.swTx);
        out.swTy = Mth.lerp(u, a.swTy, b.swTy);
        out.swTz = Mth.lerp(u, a.swTz, b.swTz);
        out.swRx = Mth.lerp(u, a.swRx, b.swRx);
        out.swRy = Mth.lerp(u, a.swRy, b.swRy);
        out.swRz = Mth.lerp(u, a.swRz, b.swRz);
        out.swAlpha = Mth.lerp(u, a.swAlpha, b.swAlpha);
        out.poseWeight = Mth.lerp(u, a.poseWeight, b.poseWeight);
        return out;
    }

    private static void copyKey(MusouKey k, SampledMusouPose out) {
        out.bodyX = k.bodyX; out.bodyY = k.bodyY; out.bodyZ = k.bodyZ; out.crouchY = k.crouchY;
        out.headX = k.headX; out.headY = k.headY;
        out.mainX = k.mainX; out.mainY = k.mainY; out.mainZ = k.mainZ;
        out.offX = k.offX; out.offY = k.offY; out.offZ = k.offZ;
        out.legLX = k.legLX; out.legLZ = k.legLZ; out.legRX = k.legRX; out.legRZ = k.legRZ;
        out.swTx = k.swTx; out.swTy = k.swTy; out.swTz = k.swTz;
        out.swRx = k.swRx; out.swRy = k.swRy; out.swRz = k.swRz;
        out.swAlpha = k.swAlpha;
        out.poseWeight = k.poseWeight;
    }

    // -------------------------------------------------------------------------
    // ULTIMATE: Musou no Hitotachi (54 ticks = 2700ms; micro-lunge T26..28, impact T28)
    // 3D-matrix-verified zero-crookedness blade orientation across draw -> coil -> horizontal cleave
    // -------------------------------------------------------------------------
    private static final MusouKey[] TRACK_ULTIMATE_DRAW = new MusouKey[] {
            // T0: Begin reaching toward chest, sword still hidden inside sternum
            new MusouKey(0.0F,   0.00F,  0.00F, 0.00F, 0.0F,  0.00F,  0.00F,  -0.30F, -0.15F,  0.10F,  -0.20F,  0.10F, -0.08F,  0.05F, -0.04F, -0.05F,  0.04F,  -0.08F, -0.04F,  0.04F, -148.0F, -43.0F,  32.0F, 0.00F, 0.25F),
            // T5: Hand grips hilt emerging straight vertically from chest, sword starts materializing
            new MusouKey(5.0F,  -0.14F, -0.08F, 0.00F, 0.0F,  0.22F,  0.00F,  -1.08F, -0.58F,  0.22F,  -0.86F,  0.38F, -0.22F,  0.16F, -0.07F, -0.14F,  0.07F,  -0.10F, -0.05F,  0.04F, -148.0F, -43.0F,  32.0F, 0.12F, 1.00F),
            // T10: Hilt fully materialized straight vertical (blade = 0, -1, 0), steady poise
            new MusouKey(10.0F, -0.16F, -0.10F, 0.00F, 0.0F,  0.24F,  0.00F,  -1.15F, -0.56F,  0.20F,  -0.88F,  0.36F, -0.20F,  0.18F, -0.08F, -0.16F,  0.08F,  -0.09F, -0.03F,  0.03F, -150.0F, -39.0F,  26.0F, 1.00F, 1.00F),
            // T20: Drawing Musou Isshin straight vertically upward out of chest (blade stays strictly 0, -1, 0!)
            new MusouKey(20.0F, -0.02F, -0.16F, 0.00F, 0.0F, -0.04F,  0.00F,  -2.20F, -0.18F,  0.08F,  -0.36F,  0.18F, -0.18F,  0.22F, -0.09F, -0.20F,  0.09F,  -0.02F,  0.04F, -0.04F, -206.0F, -11.0F,  -2.0F, 1.00F, 1.00F),
            // T22.5: Clean overhead-left wrist transition into horizontal iai plane (zero crooked flip)
            new MusouKey(22.5F,  0.04F, -0.34F, -0.02F, 0.2F, 0.00F,  0.10F,  -1.92F, -0.68F,  0.04F,  -0.40F,  0.02F, -0.22F,  0.24F, -0.09F, -0.24F,  0.09F,   0.00F,  0.04F, -0.06F,  -88.0F,  42.0F, -52.0F, 1.00F, 1.00F),
            // T25: Coil left for Musou no Hitotachi horizontal cleave — blade strictly flat horizontal pointing Left-Forward
            new MusouKey(25.0F,  0.10F, -0.48F, -0.04F, 0.5F, 0.02F,  0.18F,  -1.55F, -1.10F,  0.00F,  -0.42F, -0.12F, -0.25F,  0.28F, -0.10F, -0.28F,  0.10F,   0.00F,  0.04F, -0.08F,    0.0F,  79.0F, -88.0F, 1.00F, 1.00F),
            // T28: Impact peak + forward micro-lunge — blade strictly flat horizontal pointing Straight Forward (0, 0, +1)
            new MusouKey(28.0F,  0.16F,  0.18F,  0.04F, 0.7F, 0.04F, -0.06F,  -1.55F,  0.00F,  0.00F,   0.18F, -0.25F, -0.35F,  0.34F, -0.10F, -0.36F,  0.10F,   0.00F,  0.04F, -0.08F,    0.0F,  79.0F, -88.0F, 1.00F, 1.00F),
            // T31: Full follow-through — right arm extended straight right, blade strictly horizontal pointing Right (+0.99, 0, +0.12)
            new MusouKey(31.0F,  0.14F,  0.52F,  0.06F, 0.5F, 0.04F, -0.18F,  -1.55F,  1.45F,  0.00F,   0.42F, -0.34F, -0.40F,  0.30F, -0.10F, -0.32F,  0.10F,   0.00F,  0.04F, -0.08F,    0.0F,  79.0F, -88.0F, 1.00F, 1.00F),
            // T37: Graceful post-Hitotachi hold, torso begins uncoiling slowly
            new MusouKey(37.0F,  0.06F,  0.30F,  0.02F, 0.2F, 0.02F, -0.10F,  -1.42F,  0.96F,  0.00F,   0.26F, -0.20F, -0.24F,  0.18F, -0.07F, -0.18F,  0.07F,   0.00F,  0.04F, -0.07F,  -28.0F,  68.0F, -86.0F, 1.00F, 1.00F),
            // T45: Iconic horizontal front presentation across chest (video #105-#109)
            new MusouKey(45.0F,  0.02F,  0.06F,  0.00F, 0.0F, 0.00F,  0.00F,  -1.18F, -0.24F,  0.00F,  -0.38F,  0.22F, -0.14F,  0.08F, -0.04F, -0.08F,  0.04F,   0.02F,  0.04F, -0.05F, -159.0F,  15.0F, -96.0F, 1.00F, 1.00F),
            // T47: Crisp downward-right flick as electro gleam triggers dissolution (video #111)
            new MusouKey(47.0F,  0.00F,  0.08F,  0.00F, 0.0F, 0.00F,  0.00F,  -0.78F,  0.22F,  0.16F,  -0.14F,  0.10F, -0.12F,  0.04F, -0.03F, -0.04F,  0.03F,   0.00F,  0.02F, -0.04F, -120.0F,  24.0F, -64.0F, 0.85F, 0.92F),
            // T52: Sword fully dissolved into electro sparks (video #115)
            new MusouKey(52.0F,  0.00F,  0.02F,  0.00F, 0.0F, 0.00F,  0.00F,  -0.18F,  0.06F,  0.06F,   0.00F,  0.00F, -0.04F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -80.0F,  12.0F, -24.0F, 0.00F, 0.30F),
            // T54: Relaxed neutral stance
            new MusouKey(54.0F,  0.00F,  0.00F,  0.00F, 0.0F, 0.00F,  0.00F,   0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -80.0F,   0.0F,   0.0F, 0.00F, 0.00F)
    };

    // -------------------------------------------------------------------------
    // N1 (step 0, 36 ticks = 1800ms): Diagonal Top-Left -> Bottom-Right -> Hip-Back Rest -> Dissolve
    // -------------------------------------------------------------------------
    private static final MusouKey[] TRACK_N1 = new MusouKey[] {
            // T0: Sword materializes quickly as arm raises high-left
            new MusouKey(0.0F,   0.02F, -0.22F, -0.03F, 0.0F,  0.00F,  0.10F,  -1.65F, -0.68F, -0.22F,  -0.25F,  0.12F, -0.15F,  0.14F, -0.05F, -0.14F,  0.05F,   0.00F,  0.05F, -0.08F,  -52.0F, -14.0F, -42.0F, 0.35F, 0.85F),
            // T3: Peak wind-up upper-left
            new MusouKey(3.0F,   0.04F, -0.34F, -0.05F, 0.2F,  0.02F,  0.14F,  -2.18F, -0.86F, -0.30F,  -0.32F,  0.16F, -0.20F,  0.20F, -0.07F, -0.18F,  0.07F,   0.00F,  0.05F, -0.09F,  -50.0F, -16.0F, -48.0F, 1.00F, 1.00F),
            // T7: Fast crisp diagonal cleave down to lower-right (video #168-#171)
            new MusouKey(7.0F,   0.12F,  0.36F,  0.05F, 0.4F,  0.04F, -0.14F,  -0.46F,  0.78F,  0.42F,   0.36F, -0.22F, -0.30F,  0.24F, -0.08F, -0.26F,  0.08F,   0.00F,  0.04F, -0.08F,  -40.0F,  12.0F,  34.0F, 1.00F, 1.00F),
            // T16: Poised low-right follow-through hold (video #173-#180)
            new MusouKey(16.0F,  0.08F,  0.28F,  0.03F, 0.2F,  0.02F, -0.10F,  -0.42F,  0.66F,  0.36F,   0.26F, -0.16F, -0.22F,  0.18F, -0.06F, -0.20F,  0.06F,   0.00F,  0.03F, -0.07F,  -36.0F,  18.0F,  28.0F, 1.00F, 1.00F),
            // T25: Smoothly sweeps blade behind right hip pointing back-downward (video #185-#190)
            new MusouKey(25.0F,  0.02F,  0.14F,  0.00F, 0.0F,  0.00F, -0.04F,   0.28F,  0.24F,  0.22F,  -0.22F,  0.24F, -0.12F,  0.08F, -0.04F, -0.08F,  0.04F,   0.02F,  0.00F, -0.02F,  -16.0F,  28.0F,  16.0F, 1.00F, 0.95F),
            // T31: Smooth dissolution while resting at right hip (video #191-#194)
            new MusouKey(31.0F,  0.00F,  0.04F,  0.00F, 0.0F,  0.00F,  0.00F,   0.14F,  0.10F,  0.10F,  -0.08F,  0.08F, -0.06F,  0.02F, -0.01F, -0.02F,  0.01F,   0.00F,  0.00F,  0.00F,  -12.0F,  14.0F,   8.0F, 0.00F, 0.45F),
            // T36: Neutral idle
            new MusouKey(36.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -12.0F,   0.0F,   0.0F, 0.00F, 0.00F)
    };

    // -------------------------------------------------------------------------
    // N2 (step 1, 38 ticks = 1900ms): Flat Horizontal Right -> Left -> Right Hold -> Vertical Wrist Spin -> Dissolve
    // -------------------------------------------------------------------------
    private static final MusouKey[] TRACK_N2 = new MusouKey[] {
            // T0: Coil right at waist height, blade flat horizontal
            new MusouKey(0.0F,   0.04F,  0.34F,  0.04F, 0.2F,  0.02F, -0.14F,  -1.26F,  0.96F,  0.44F,   0.20F, -0.18F, -0.22F,  0.18F, -0.06F, -0.18F,  0.06F,   0.00F,  0.04F, -0.08F,  -46.0F,   6.0F,  82.0F, 0.55F, 0.90F),
            // T3: Peak right wind-up
            new MusouKey(3.0F,   0.06F,  0.42F,  0.05F, 0.3F,  0.02F, -0.16F,  -1.42F,  1.12F,  0.48F,   0.26F, -0.22F, -0.26F,  0.22F, -0.07F, -0.22F,  0.07F,   0.00F,  0.04F, -0.09F,  -46.0F,   4.0F,  86.0F, 1.00F, 1.00F),
            // T7: Razor-flat horizontal backhand slash Right -> Left (video #235-#238)
            new MusouKey(7.0F,   0.10F, -0.40F, -0.05F, 0.3F,  0.02F,  0.16F,  -1.48F, -0.96F, -0.34F,   0.38F,  0.24F, -0.34F,  0.26F, -0.08F, -0.24F,  0.08F,   0.00F,  0.04F, -0.09F,  -48.0F,  -6.0F,  86.0F, 1.00F, 1.00F),
            // T13: Smooth pull back across to poised right-side horizontal hold (video #241-#253)
            new MusouKey(13.0F,  0.04F,  0.16F,  0.02F, 0.1F,  0.00F, -0.06F,  -1.36F,  0.72F,  0.24F,   0.12F, -0.10F, -0.18F,  0.14F, -0.05F, -0.14F,  0.05F,   0.00F,  0.04F, -0.08F,  -48.0F,  12.0F,  78.0F, 1.00F, 1.00F),
            // T22: Hold poised horizontal right stance
            new MusouKey(22.0F,  0.02F,  0.12F,  0.00F, 0.0F,  0.00F, -0.04F,  -1.32F,  0.64F,  0.22F,   0.08F, -0.08F, -0.14F,  0.10F, -0.04F, -0.10F,  0.04F,   0.00F,  0.04F, -0.08F,  -48.0F,  16.0F,  74.0F, 1.00F, 1.00F),
            // T26: Iconic wrist spin tilting blade 45° upward (video #256-#257)
            new MusouKey(26.0F,  0.00F,  0.04F,  0.00F, 0.0F,  0.00F,  0.00F,  -1.18F,  0.18F,  0.08F,  -0.12F,  0.12F, -0.12F,  0.06F, -0.03F, -0.06F,  0.03F,   0.00F,  0.04F, -0.05F,  -14.0F,  10.0F,  28.0F, 1.00F, 0.98F),
            // T29: Blade upright 90° vertical right in front of chest (video #259-#260)
            new MusouKey(29.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,  -1.08F, -0.14F,  0.04F,  -0.22F,  0.18F, -0.12F,  0.04F, -0.02F, -0.04F,  0.02F,   0.00F,  0.04F, -0.03F,   16.0F,   0.0F,   0.0F, 1.00F, 0.95F),
            // T34: Smooth vertical sword dissolution (video #261-#265)
            new MusouKey(34.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,  -0.36F, -0.04F,  0.02F,  -0.06F,  0.06F, -0.04F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.02F,  0.00F,   16.0F,   0.0F,   0.0F, 0.00F, 0.42F),
            // T38: Neutral idle
            new MusouKey(38.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,    0.0F,   0.0F,   0.0F, 0.00F, 0.00F)
    };

    // -------------------------------------------------------------------------
    // N3 (step 2, 46 ticks = 2300ms): Low-Left -> Steep Rising Vertical Slash -> High Hold -> Slow Arc to Horizontal -> Flick Dissolve
    // -------------------------------------------------------------------------
    private static final MusouKey[] TRACK_N3 = new MusouKey[] {
            // T0: Slight crouch, right arm drops low-left
            new MusouKey(0.0F,   0.10F, -0.26F, -0.04F, 0.4F,  0.06F,  0.10F,  -0.38F, -0.62F, -0.24F,   0.22F,  0.14F, -0.18F,  0.20F, -0.06F, -0.20F,  0.06F,   0.00F,  0.02F, -0.06F,  -32.0F, -10.0F, -16.0F, 0.65F, 0.92F),
            // T3: Launch point low-left
            new MusouKey(3.0F,   0.12F, -0.32F, -0.05F, 0.5F,  0.06F,  0.12F,  -0.30F, -0.72F, -0.28F,   0.28F,  0.18F, -0.22F,  0.22F, -0.07F, -0.22F,  0.07F,   0.00F,  0.02F, -0.07F,  -30.0F, -12.0F, -18.0F, 1.00F, 1.00F),
            // T8: Explosive steep rising vertical slash straight up (video #312-#316)
            new MusouKey(8.0F,  -0.12F,  0.20F,  0.04F, 0.0F, -0.10F, -0.08F,  -2.74F,  0.18F,  0.16F,   0.34F, -0.20F, -0.28F, -0.18F, -0.06F,  0.20F,  0.06F,   0.00F,  0.06F, -0.08F,  -42.0F,   4.0F,   8.0F, 1.00F, 1.00F),
            // T13: Settle into regal high-poised vertical katana stance (video #318-#335)
            new MusouKey(13.0F, -0.02F,  0.12F,  0.00F, 0.0F, -0.02F, -0.04F,  -1.64F,  0.14F,  0.10F,  -0.18F,  0.16F, -0.14F,  0.10F, -0.04F, -0.10F,  0.04F,   0.00F,  0.05F, -0.04F,   18.0F,   4.0F,   0.0F, 1.00F, 1.00F),
            // T24: Hold high vertical blade calmly
            new MusouKey(24.0F,  0.00F,  0.10F,  0.00F, 0.0F,  0.00F, -0.02F,  -1.56F,  0.12F,  0.08F,  -0.22F,  0.18F, -0.14F,  0.08F, -0.04F, -0.08F,  0.04F,   0.00F,  0.05F, -0.04F,   18.0F,   6.0F,   0.0F, 1.00F, 1.00F),
            // T31: Slow regal downward arc (blade at ~45°, video #340-#342)
            new MusouKey(31.0F,  0.00F,  0.06F,  0.00F, 0.0F,  0.00F,  0.00F,  -1.36F, -0.06F, -0.04F,  -0.36F,  0.26F, -0.16F,  0.06F, -0.03F, -0.06F,  0.03F,   0.01F,  0.04F, -0.05F,  -22.0F,  44.0F, -10.0F, 1.00F, 1.00F),
            // T37: Locked horizontal across chest (video #345-#347)
            new MusouKey(37.0F,  0.02F,  0.04F,  0.00F, 0.0F,  0.00F,  0.00F,  -1.22F, -0.22F, -0.12F,  -0.44F,  0.30F, -0.16F,  0.04F, -0.02F, -0.04F,  0.02F,   0.02F,  0.04F, -0.06F,  -60.0F,  78.0F, -14.0F, 1.00F, 1.00F),
            // T39: Crisp horizontal flick right + electro flash (video #348-#350)
            new MusouKey(39.0F,  0.02F,  0.14F,  0.02F, 0.0F,  0.00F, -0.04F,  -1.18F,  0.52F,  0.22F,  -0.18F,  0.10F, -0.16F,  0.04F, -0.02F, -0.04F,  0.02F,   0.00F,  0.04F, -0.07F,  -48.0F,  28.0F, -72.0F, 0.85F, 0.94F),
            // T42: Sword fully dissolved (video #351-#353)
            new MusouKey(42.0F,  0.00F,  0.04F,  0.00F, 0.0F,  0.00F,  0.00F,  -0.38F,  0.18F,  0.08F,  -0.06F,  0.04F, -0.06F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -45.0F,  10.0F, -30.0F, 0.00F, 0.42F),
            // T46: Neutral idle
            new MusouKey(46.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -45.0F,   0.0F,   0.0F, 0.00F, 0.00F)
    };

    // -------------------------------------------------------------------------
    // N4 (step 3, 50 ticks = 2500ms): Spin Wind-up -> Rapid 2-Hit Cross -> Low Diagonal Hold -> Vertical Lift -> Horizontal Flick Dissolve
    // -------------------------------------------------------------------------
    private static final MusouKey[] TRACK_N4 = new MusouKey[] {
            // T0: Coil right for spin entry
            new MusouKey(0.0F,   0.06F,  0.38F,  0.04F, 0.3F,  0.02F, -0.14F,  -1.36F,  1.08F,  0.46F,  -0.30F, -0.16F, -0.24F,  0.22F, -0.07F, -0.22F,  0.07F,   0.00F,  0.04F, -0.08F,  -48.0F,   8.0F,  78.0F, 0.65F, 0.94F),
            // T4: Peak spin coil right (video #418-#419)
            new MusouKey(4.0F,   0.10F,  0.46F,  0.06F, 0.5F,  0.04F, -0.18F,  -1.42F,  1.22F,  0.52F,  -0.42F, -0.24F, -0.32F,  0.26F, -0.09F, -0.26F,  0.09F,   0.00F,  0.04F, -0.09F,  -46.0F,   6.0F,  82.0F, 1.00F, 1.00F),
            // T8: Hit 4A — Wide horizontal-diagonal sweep Right -> Left (video #420-#423)
            new MusouKey(8.0F,   0.12F, -0.46F, -0.06F, 0.5F,  0.04F,  0.18F,  -1.66F, -1.08F, -0.44F,   0.38F,  0.28F, -0.36F, -0.24F, -0.08F,  0.26F,  0.08F,   0.00F,  0.05F, -0.09F,  -46.0F, -10.0F,  64.0F, 1.00F, 1.00F),
            // T13: Hit 4B — Instant reverse-spin counter-slash Left -> Low-Right (video #424-#428)
            new MusouKey(13.0F,  0.14F,  0.44F,  0.06F, 0.4F,  0.04F, -0.16F,  -0.62F,  0.96F,  0.46F,   0.44F, -0.28F, -0.36F,  0.28F, -0.09F, -0.28F,  0.09F,   0.00F,  0.04F, -0.08F,  -40.0F,  14.0F,  36.0F, 1.00F, 1.00F),
            // T25: Poised low-diagonal follow-through hold (video #432-#443)
            new MusouKey(25.0F,  0.06F,  0.24F,  0.02F, 0.1F,  0.02F, -0.08F,  -0.54F,  0.52F,  0.28F,   0.18F, -0.10F, -0.20F,  0.16F, -0.06F, -0.16F,  0.06F,   0.00F,  0.03F, -0.07F,  -38.0F,  18.0F,  26.0F, 1.00F, 1.00F),
            // T33: Graceful vertical lift — blade sweeps straight UP 90° (video #447-#449)
            new MusouKey(33.0F,  0.00F,  0.06F,  0.00F, 0.0F,  0.00F, -0.02F,  -1.52F,  0.16F,  0.08F,  -0.22F,  0.18F, -0.14F,  0.08F, -0.04F, -0.08F,  0.04F,   0.00F,  0.05F, -0.04F,   18.0F,   6.0F,   0.0F, 1.00F, 1.00F),
            // T41: Smoothly arcs down into horizontal chest lock (video #454-#457)
            new MusouKey(41.0F,  0.02F,  0.04F,  0.00F, 0.0F,  0.00F,  0.00F,  -1.20F, -0.20F, -0.12F,  -0.42F,  0.28F, -0.16F,  0.04F, -0.02F, -0.04F,  0.02F,   0.02F,  0.04F, -0.06F,  -60.0F,  76.0F, -14.0F, 1.00F, 1.00F),
            // T43: Bright horizontal electro flash flick (video #458-#460)
            new MusouKey(43.0F,  0.02F,  0.14F,  0.02F, 0.0F,  0.00F, -0.04F,  -1.16F,  0.54F,  0.22F,  -0.16F,  0.10F, -0.16F,  0.04F, -0.02F, -0.04F,  0.02F,   0.00F,  0.04F, -0.07F,  -48.0F,  28.0F, -72.0F, 0.85F, 0.94F),
            // T46: Sword smoothly dissolves (video #461-#464)
            new MusouKey(46.0F,  0.00F,  0.04F,  0.00F, 0.0F,  0.00F,  0.00F,  -0.36F,  0.16F,  0.08F,  -0.06F,  0.04F, -0.06F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -45.0F,  10.0F, -30.0F, 0.00F, 0.42F),
            // T50: Neutral idle
            new MusouKey(50.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -45.0F,   0.0F,   0.0F, 0.00F, 0.00F)
    };

    // -------------------------------------------------------------------------
    // N5 (step 4, 48 ticks = 2400ms): Low Iaijutsu Crouch -> Massive Horizontal Space-Cleave -> Crouched Sheathe -> Dissolve -> Rise
    // -------------------------------------------------------------------------
    private static final MusouKey[] TRACK_N5 = new MusouKey[] {
            // T0: Begin dropping into low iai stance, arm drawing left
            new MusouKey(0.0F,   0.18F, -0.30F, -0.04F, 1.0F,  0.06F,  0.12F,  -1.15F, -0.78F, -0.26F,  -0.56F,  0.34F, -0.18F,  0.28F, -0.09F, -0.28F,  0.09F,   0.00F,  0.03F, -0.07F,  -48.0F, -10.0F, -82.0F, 0.70F, 0.94F),
            // T8: Deep low iaijutsu crouch coiled left, left hand poised at waist scabbard (video #547-#549)
            new MusouKey(8.0F,   0.36F, -0.58F, -0.07F, 2.5F,  0.10F,  0.26F,  -1.24F, -1.16F, -0.36F,  -0.78F,  0.52F, -0.22F,  0.48F, -0.12F, -0.44F,  0.12F,   0.00F,  0.03F, -0.08F,  -48.0F, -14.0F, -88.0F, 1.00F, 1.00F),
            // T13: MASSIVE LOW HORIZONTAL SPACE-CLEAVING IAI SLASH Left -> Extended Right-Back (video #550-#555)
            new MusouKey(13.0F,  0.38F,  0.62F,  0.08F, 2.6F,  0.08F, -0.26F,  -1.16F,  1.56F,  0.46F,   0.52F, -0.42F, -0.46F,  0.46F, -0.12F, -0.46F,  0.12F,   0.00F,  0.04F, -0.09F,  -46.0F,   0.0F, -88.0F, 1.00F, 1.00F),
            // T24: Hold iconic low crouched follow-through pose (video #558-#565)
            new MusouKey(24.0F,  0.34F,  0.52F,  0.06F, 2.4F,  0.06F, -0.22F,  -1.12F,  1.38F,  0.38F,   0.40F, -0.32F, -0.36F,  0.44F, -0.11F, -0.42F,  0.11F,   0.00F,  0.04F, -0.08F,  -46.0F,   8.0F, -84.0F, 1.00F, 1.00F),
            // T32: Slow crouched draw-back / sheathing across waist with electro glint (video #566-#573)
            new MusouKey(32.0F,  0.28F,  0.18F,  0.02F, 2.0F,  0.08F, -0.08F,  -0.76F,  0.32F,  0.18F,  -0.36F,  0.28F, -0.18F,  0.38F, -0.10F, -0.36F,  0.10F,   0.02F,  0.02F, -0.05F,  -54.0F,  48.0F, -48.0F, 1.00F, 1.00F),
            // T39: Smooth sword dissolution while still crouched low (video #574-#581)
            new MusouKey(39.0F,  0.22F,  0.08F,  0.00F, 1.5F,  0.06F, -0.02F,  -0.46F,  0.12F,  0.10F,  -0.22F,  0.16F, -0.12F,  0.28F, -0.08F, -0.26F,  0.08F,   0.00F,  0.00F, -0.02F,  -48.0F,  28.0F, -24.0F, 0.00F, 0.85F),
            // T44: Gracefully rising upright out of the crouch (video #582-#586)
            new MusouKey(44.0F,  0.06F,  0.02F,  0.00F, 0.4F,  0.02F,  0.00F,  -0.16F,  0.04F,  0.04F,  -0.08F,  0.06F, -0.04F,  0.08F, -0.03F, -0.08F,  0.03F,   0.00F,  0.00F,  0.00F,  -45.0F,   8.0F,  -8.0F, 0.00F, 0.35F),
            // T48: Regal standing neutral
            new MusouKey(48.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  -45.0F,   0.0F,   0.0F, 0.00F, 0.00F)
    };

    // -------------------------------------------------------------------------
    // CHARGED ATTACK (ANIM_RAIDEN_CA, 44 ticks = 2200ms): Low Iai Coil -> Twin X-Crescent Dash -> Low Hold -> Vertical Hold -> Dissolve
    // -------------------------------------------------------------------------
    private static final MusouKey[] TRACK_CA = new MusouKey[] {
            // T0: Quick low-left iai coil (video #597-#601)
            new MusouKey(0.0F,   0.18F, -0.34F, -0.04F, 0.8F,  0.06F,  0.14F,  -1.10F, -0.88F, -0.28F,  -0.52F,  0.30F, -0.18F,  0.28F, -0.08F, -0.26F,  0.08F,   0.00F,  0.03F, -0.07F,  -48.0F, -10.0F, -64.0F, 0.65F, 0.94F),
            // T4: Peak coil before dash
            new MusouKey(4.0F,   0.26F, -0.44F, -0.06F, 1.2F,  0.08F,  0.18F,  -0.68F, -1.04F, -0.34F,  -0.60F,  0.34F, -0.22F,  0.36F, -0.10F, -0.34F,  0.10F,   0.00F,  0.03F, -0.08F,  -46.0F, -12.0F, -54.0F, 1.00F, 1.00F),
            // T9: Twin X-Slash Hit 1 — explosive upward-diagonal whip low-left -> high-right (video #619-#623)
            new MusouKey(9.0F,   0.22F,  0.46F,  0.06F, 0.8F,  0.04F, -0.18F,  -2.24F,  1.04F,  0.42F,   0.44F, -0.30F, -0.36F, -0.32F, -0.09F,  0.36F,  0.09F,   0.00F,  0.05F, -0.09F,  -44.0F,   8.0F,  44.0F, 1.00F, 1.00F),
            // T15: Twin X-Slash Hit 2 — counter diagonal slash high-right -> low-right lunge (video #627-#635)
            new MusouKey(15.0F,  0.18F,  0.24F,  0.04F, 0.5F,  0.04F, -0.10F,  -0.52F,  0.66F,  0.36F,   0.32F, -0.22F, -0.28F,  0.30F, -0.09F, -0.30F,  0.09F,   0.00F,  0.03F, -0.08F,  -40.0F,  14.0F,  30.0F, 1.00F, 1.00F),
            // T25: Poised low-diagonal follow-through stance (video #639-#653)
            new MusouKey(25.0F,  0.06F,  0.14F,  0.02F, 0.1F,  0.02F, -0.06F,  -0.44F,  0.46F,  0.26F,   0.14F, -0.10F, -0.16F,  0.16F, -0.05F, -0.16F,  0.05F,   0.00F,  0.02F, -0.06F,  -36.0F,  16.0F,  22.0F, 1.00F, 1.00F),
            // T34: Smooth wrist rotation lifting katana straight UP vertically beside right side (video #655-#669)
            new MusouKey(34.0F,  0.00F,  0.04F,  0.00F, 0.0F,  0.00F,  0.00F,  -0.78F,  0.14F,  0.12F,  -0.08F,  0.08F, -0.10F,  0.06F, -0.03F, -0.06F,  0.03F,   0.00F,  0.04F, -0.03F,   20.0F,   6.0F,   0.0F, 1.00F, 0.96F),
            // T39: Vertical electro flash & smooth sword dissolution (video #671-#675)
            new MusouKey(39.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,  -0.32F,  0.06F,  0.06F,  -0.04F,  0.04F, -0.04F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.02F,  0.00F,   20.0F,   2.0F,   0.0F, 0.00F, 0.44F),
            // T44: Neutral idle
            new MusouKey(44.0F,  0.00F,  0.00F,  0.00F, 0.0F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,  0.00F,   0.00F,  0.00F,  0.00F,    0.0F,   0.0F,   0.0F, 0.00F, 0.00F)
    };

    private static MusouKey[] resolveMusouTrack(int activeAnim, ClientAuraTracker.AuraDisplayData data) {
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW) return TRACK_ULTIMATE_DRAW;
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_CA) return TRACK_CA;
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_SLASH) {
            int step = data != null ? Math.floorMod(data.raidenComboStep, 5) : 0;
            return switch (step) {
                case 0 -> TRACK_N1;
                case 1 -> TRACK_N2;
                case 2 -> TRACK_N3;
                case 3 -> TRACK_N4;
                default -> TRACK_N5;
            };
        }
        return null;
    }

    private static float getActiveCombatCutoffTick(int activeAnim, ClientAuraTracker.AuraDisplayData data) {
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW) return 32.0F;
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_CA) return 16.0F;
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_SLASH) {
            int step = data != null ? Math.floorMod(data.raidenComboStep, 5) : 0;
            return (step == 3 || step == 4) ? 15.0F : 9.0F;
        }
        return 0.0F;
    }

    /**
     * Returns smooth weapon visibility alpha [0..1] for Raiden Shogun's Musou Isshin sword:
     * - Normal polearm mode: always 1.0F (untouched).
     * - Musou Isshin active attack/draw/CA: exact tick-keyframed materialization & dissolution alpha.
     * - Musou Isshin idle (between attacks after dissolution): smoothly hidden (0.0F), matching Genshin Impact.
     */
    public static float getMusouWeaponFadeAlpha(LivingEntity entity, net.minecraft.world.item.ItemStack stack) {
        if (entity == null || stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArcaneStaffItem)) return 1.0F;
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) return 1.0F;
        int id = entity.getId();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(id);
        long now = System.currentTimeMillis();
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;
        MusouKey[] track = resolveMusouTrack(activeAnim, data);
        if (track != null && data != null) {
            float tick = Math.max(0.0F, (now - data.staffAnimStartTime) / 50.0F);
            SampledMusouPose pose = sampleTrack(track, tick);
            return Mth.clamp(pose.swAlpha, 0.0F, 1.0F);
        }
        if (ClientAuraTracker.isRaidenMusouActive(id)) {
            return 0.0F;
        }
        return 1.0F;
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
        MusouKey[] track = resolveMusouTrack(activeAnim, data);
        if (track != null && data != null) {
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            var mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            var offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            var leadLeg = castArm == HumanoidArm.RIGHT ? model.rightLeg : model.leftLeg;
            var trailLeg = castArm == HumanoidArm.RIGHT ? model.leftLeg : model.rightLeg;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

            float tick = Math.max(0.0F, (System.currentTimeMillis() - data.staffAnimStartTime) / 50.0F);
            SampledMusouPose p = sampleTrack(track, tick);
            float w = Mth.clamp(p.poseWeight, 0.0F, 1.0F);
            if (w <= 0.001F) return false;

            // Subtle high-energy tremor during Ultimate chest pull (ticks 10..20)
            float tremor = (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW && tick >= 10.0F && tick <= 20.0F)
                    ? Mth.sin(ageInTicks * 2.4F) * 0.016F
                    : 0.0F;

            // Upper/lower body blending: during passive recovery flourish, if player starts walking,
            // smoothly release leg & crouch overrides so legs walk naturally while arms finish sheathing
            float activeCutoff = getActiveCombatCutoffTick(activeAnim, data);
            float walkSpeed = entity.walkAnimation.speed();
            float moveRelease = (tick > activeCutoff && walkSpeed > 0.08F)
                    ? Mth.clamp((walkSpeed - 0.08F) / 0.28F, 0.0F, 1.0F)
                    : 0.0F;
            float legW = w * (1.0F - moveRelease);
            float crouchOffset = p.crouchY * legW;

            if (crouchOffset > 0.01F) {
                model.head.y += crouchOffset;
                model.hat.y += crouchOffset;
                model.body.y += crouchOffset;
                model.rightArm.y += crouchOffset;
                model.leftArm.y += crouchOffset;
            }

            model.body.xRot = Mth.lerp(w, model.body.xRot, p.bodyX);
            model.body.yRot = Mth.lerp(w, model.body.yRot, castDir * p.bodyY);
            model.body.zRot = Mth.lerp(w, model.body.zRot, castDir * p.bodyZ);

            mainPart.xRot = Mth.lerp(w, mainPart.xRot, p.mainX + tremor);
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * p.mainY);
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * p.mainZ);

            offPart.xRot = Mth.lerp(w, offPart.xRot, p.offX);
            offPart.yRot = Mth.lerp(w, offPart.yRot, castDir * p.offY);
            offPart.zRot = Mth.lerp(w, offPart.zRot, castDir * p.offZ);

            if (legW > 0.001F) {
                trailLeg.xRot = Mth.lerp(legW, trailLeg.xRot, p.legLX);
                trailLeg.zRot = Mth.lerp(legW, trailLeg.zRot, castDir * p.legLZ);
                leadLeg.xRot = Mth.lerp(legW, leadLeg.xRot, p.legRX);
                leadLeg.zRot = Mth.lerp(legW, leadLeg.zRot, castDir * p.legRZ);
            }

            // Head poise: strong during active window, subtle during recovery so player can look around
            float headW = (tick <= activeCutoff) ? w : (w * 0.45F * (1.0F - moveRelease));
            if (headW > 0.001F) {
                model.head.xRot = Mth.lerp(headW, model.head.xRot, p.headX);
                model.head.yRot = Mth.lerp(headW, model.head.yRot, castDir * p.headY);
                model.hat.copyFrom(model.head);
            }
            return true;
        }
        return false;
    }

    public static boolean applyThirdPersonItemTransform(
            LivingEntity entity,
            ArcaneStaffItem.Kind effKind,
            boolean leftHand,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (effKind != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) return false;
        float side = leftHand ? -1.0F : 1.0F;
        MusouKey[] track = resolveMusouTrack(activeAnim, data);
        if (track != null && data != null) {
            float tick = Math.max(0.0F, (now - data.staffAnimStartTime) / 50.0F);
            SampledMusouPose p = sampleTrack(track, tick);
            if (p.swAlpha <= 0.02F) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
            poseStack.translate(side * p.swTx, p.swTy, p.swTz);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * p.swRy));
            poseStack.mulPose(Axis.XP.rotationDegrees(p.swRx));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * p.swRz));
            if (p.swAlpha < 0.995F) {
                float s = Mth.lerp(p.swAlpha, 0.86F, 1.0F);
                poseStack.scale(s, s, s);
            }
            return true;
        }
        // When Musou Isshin is active and no attack/draw is playing, sword has dissolved from hand
        if (entity != null && ClientAuraTracker.isRaidenMusouActive(entity.getId())) {
            poseStack.scale(0.0F, 0.0F, 0.0F);
            return true;
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
        MusouKey[] track = resolveMusouTrack(activeAnim, data);
        if (track != null && data != null) {
            float tick = Math.max(0.0F, (now - data.staffAnimStartTime) / 50.0F);
            SampledMusouPose p = sampleTrack(track, tick);
            if (p.swAlpha <= 0.02F) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
            if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW) {
                if (tick < 10.0F) {
                    float reach = smootherStep01(tick / 10.0F);
                    poseStack.translate(-dir * 0.42F * reach, -0.26F * reach, -0.18F * reach);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 172.0F * reach));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F * reach));
                } else if (tick < 20.0F) {
                    float pull = smootherStep01((tick - 10.0F) / 10.0F);
                    float tremor = Mth.sin((player.tickCount + partialTicks) * 2.4F) * 0.006F * pull;
                    poseStack.translate(-dir * 0.42F + tremor, Mth.lerp(pull, -0.26F, 0.30F), -0.18F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 172.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
                } else if (tick < 25.0F) {
                    float prep = smootherStep01((tick - 20.0F) / 5.0F);
                    poseStack.translate(-dir * Mth.lerp(prep, 0.42F, 0.56F), Mth.lerp(prep, 0.30F, -0.02F), Mth.lerp(prep, -0.18F, -0.30F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(prep, 0.0F, 34.0F)));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(prep, -10.0F, -72.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(prep, 172.0F, 86.0F)));
                } else if (tick < 31.0F) {
                    float slash = smootherStep01((tick - 25.0F) / 6.0F);
                    poseStack.translate(dir * Mth.lerp(slash, -0.56F, 0.34F), 0.02F, -0.34F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(slash, 34.0F, -54.0F)));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-82.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 86.0F));
                } else if (tick < 45.0F) {
                    float hold = smootherStep01((tick - 31.0F) / 14.0F);
                    poseStack.translate(dir * Mth.lerp(hold, 0.34F, -0.10F), Mth.lerp(hold, 0.02F, -0.06F), Mth.lerp(hold, -0.34F, -0.28F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(hold, -54.0F, 12.0F)));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(hold, -82.0F, -56.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(hold, 86.0F, 72.0F)));
                } else {
                    float diss = smootherStep01((tick - 45.0F) / 7.0F);
                    poseStack.translate(dir * Mth.lerp(diss, -0.10F, 0.16F), Mth.lerp(diss, -0.06F, -0.22F), -0.26F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(diss, 12.0F, -18.0F)));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(diss, -56.0F, -32.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(diss, 72.0F, 24.0F)));
                }
            } else {
                // Unified smooth 1st-person projection directly driven by the tick-accurate 3rd-person pose!
                float w = p.poseWeight;
                float tx = dir * Mth.clamp(p.mainY * 0.28F, -0.44F, 0.42F) * w;
                float ty = Mth.clamp((-p.mainX - 1.10F) * 0.20F - p.crouchY * 0.05F, -0.30F, 0.30F) * w;
                float tz = (-0.24F - 0.10F * Mth.clamp(Math.abs(p.mainY), 0.0F, 1.0F)) * w;
                poseStack.translate(tx, ty, tz);
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.clamp(-p.mainY * 32.0F + p.swRy * 0.25F, -65.0F, 65.0F) * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.clamp((p.swRx + 15.0F) * 0.85F, -86.0F, 28.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.clamp(-p.swRz * 0.85F, -92.0F, 92.0F) * w));
            }
            if (p.swAlpha < 0.995F) {
                float s = Mth.lerp(p.swAlpha, 0.84F, 1.0F);
                poseStack.scale(s, s, s);
            }
            return true;
        }
        if (ClientAuraTracker.isRaidenMusouActive(player.getId())) {
            poseStack.scale(0.0F, 0.0F, 0.0F);
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
        return (0.674F - hover * 0.050F) * liftProg;
    }

    /**
     * Pushes Raiden's whole-body idle levitation altitude directly onto the LivingEntityRenderer PoseStack
     * in world space (before setupRotations/scale) so external animation libraries cannot override bone Y.
     */
    public static boolean pushIdleBodyTransform(LivingEntity entity, float partialTicks, PoseStack poseStack) {
        if (entity == null || entity.isUsingItem() || entity.swinging) return false;
        if (!ClientAuraTracker.isRaidenIdle(entity.getId())) return false;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        if (d == null) return false;
        stepIdleFade(d, System.currentTimeMillis());
        float w = WindGliderRenderer.smoothStep01(d.raidenIdleFade / 0.55F);
        if (w <= 0.01F) return false;
        float ageInTicks = (float) entity.tickCount + partialTicks;
        float hover = Mth.sin(ageInTicks * 0.10F);
        // Exact v1.1.0 altitude (-11.5px + 0.85*hover in 0.9375 PlayerRenderer scale), now isolated from RenderLivingEvent.Post
        double liftBlocks = (11.5F * w - hover * 0.85F * w) * (0.9375D / 16.0D);
        poseStack.pushPose();
        poseStack.translate(0.0D, liftBlocks, 0.0D);
        return true;
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

        // Whole-body levitation altitude is applied via PoseStack in applyIdleBodyTransform()
        float hover = Mth.sin(ageInTicks * 0.10F);

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
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
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
        poseStack.translate(0.0F, hover * 0.55F, 0.0F);
        poseStack.scale(-fade, -fade, fade);
        poseStack.translate(0.0F, -1.50F, 0.0F);
        VertexConsumer robotVc = buffer.getBuffer(RenderType.entityTranslucent(RAIDEN_ROBOT_TEXTURE));
        var robot = getOrBakeRobotModel();
        robot.setupAnim(entity, 0.0F, 0.0F, time, 0.0F, 0.0F);
        robot.renderToBuffer(poseStack, robotVc, FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
