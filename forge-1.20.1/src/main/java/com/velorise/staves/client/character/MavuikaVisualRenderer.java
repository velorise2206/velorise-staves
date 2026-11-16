package com.velorise.staves.client.character;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;

public final class MavuikaVisualRenderer {
    private MavuikaVisualRenderer() {}

    public static void renderMavuikaVirtualHelmet(
            LivingEntity entity,
            EntityModel<?> model,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        if (!(entity.getVehicle() instanceof FlamestriderBikeEntity bike)) return;
        if (!entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;
        if (!(model instanceof HumanoidModel<?> humanoid)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        boolean neth = bike.isNetherite();
        int crR = neth ? 220 : 195, crG = 36, crB = 26;
        int obR = 28, obG = 22, obB = 28;
        int gdR = 255, gdG = 198, gdB = 45;
        int flR = 255, flG = 115, flB = 20;

        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        float entityScale = 1.0F;
        poseStack.scale(-entityScale, -entityScale, entityScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        humanoid.head.translateAndRotate(poseStack);

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());

        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.285F, -0.545F, -0.285F, 0.285F, -0.445F, 0.285F, crR, crG, crB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.065F, -0.585F, -0.305F, 0.065F, -0.445F, 0.265F, gdR, gdG, gdB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.032F, -0.605F, -0.260F, 0.032F, -0.580F, 0.180F, flR, flG, flB, 255);

        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.285F, -0.445F, 0.220F, 0.285F, 0.015F, 0.290F, obR, obG, obB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.295F, -0.445F, -0.245F, -0.242F, 0.015F, 0.285F, obR, obG, obB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, 0.242F, -0.445F, -0.245F, 0.295F, 0.015F, 0.285F, obR, obG, obB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.312F, -0.410F, -0.140F, -0.285F, -0.180F, 0.250F, crR, crG, crB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, 0.285F, -0.410F, -0.140F, 0.312F, -0.180F, 0.250F, crR, crG, crB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.322F, -0.360F, -0.060F, -0.305F, -0.240F, 0.270F, gdR, gdG, gdB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, 0.305F, -0.360F, -0.060F, 0.322F, -0.240F, 0.270F, gdR, gdG, gdB, 255);

        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.285F, -0.135F, -0.305F, 0.285F, 0.025F, -0.180F, obR, obG, obB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.185F, -0.115F, -0.318F, 0.185F, 0.010F, -0.295F, crR, crG, crB, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.095F, -0.045F, -0.324F, 0.095F, 0.018F, -0.305F, gdR, gdG, gdB, 255);

        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.275F, -0.435F, -0.302F, 0.275F, -0.135F, -0.245F, flR, flG, flB, 238);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.235F, -0.335F, -0.310F, 0.235F, -0.215F, -0.295F, 255, 228, 88, 248);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.290F, -0.455F, -0.312F, 0.290F, -0.415F, -0.240F, gdR, gdG, gdB, 255);

        poseStack.popPose();
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(ClientAuraRenderer.getMeteorSolid());
        }
    }

    public static void renderMavuikaSunAuraRing(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (!ClientAuraTracker.isMavuikaAuraActive(entity.getId())) return;
        if (entity.getVehicle() instanceof FlamestriderBikeEntity) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        ClientAuraTracker.MavuikaDisplayState state = ClientAuraTracker.getMavuika(entity.getId());
        boolean netherite = state != null && state.netherite;
        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);

        ResourceLocation sunTex = new ResourceLocation("velorise_staves", "textures/entity/mavuika_sun_aura.png");
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(sunTex));
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.72F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.translate(0.0F, 0.0F, -0.46F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 1.6F));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        float pulse = 1.0F + 0.03F * Mth.sin(time * 0.22F);
        float half = 0.88F * pulse;
        int overlay = OverlayTexture.NO_OVERLAY;
        int cr = 255, cg = netherite ? 255 : 225, cb = netherite ? 255 : 195, ca = 240;

        org.joml.Matrix3f normMat = pose.normal();
        vc.vertex(mat, -half, -half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 1.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(mat,  half, -half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 1.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(mat,  half,  half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 0.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(mat, -half,  half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 0.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, 1.0F).endVertex();

        vc.vertex(mat, -half,  half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 0.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        vc.vertex(mat,  half,  half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 0.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        vc.vertex(mat,  half, -half, 0.0F).color(cr, cg, cb, ca).uv(1.0F, 1.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();
        vc.vertex(mat, -half, -half, 0.0F).color(cr, cg, cb, ca).uv(0.0F, 1.0F).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(normMat, 0.0F, 0.0F, -1.0F).endVertex();

        poseStack.popPose();
    }

    public static void renderChargeHud(GuiGraphics guiGraphics, int sw, int sh, float pct) {
        ResourceLocation sunsTex = new ResourceLocation(VeloriseStavesMod.MODID, "textures/gui/sun_charge.png");
        int iconX = sw / 2 - 8;
        int iconY = sh / 2 + 12;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.35F, 0.35F, 0.35F, 0.65F);
        guiGraphics.blit(sunsTex, iconX, iconY, 0, 0, 16, 16, 16, 16);
        int fillH = Math.max(1, Math.round(16 * pct));
        int topY = iconY + (16 - fillH);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(sunsTex, iconX, topY, 0, 16 - fillH, 16, fillH, 16, 16);
    }

    public static void renderMavuikaNightsoulHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        boolean holdingSuns = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS
                || ArcaneStaffItem.getEffectiveKind(mc.player.getOffhandItem()) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS
                || (mc.player.getVehicle() instanceof FlamestriderBikeEntity);
        if (!holdingSuns) return;
        ClientAuraTracker.MavuikaDisplayState state = ClientAuraTracker.getMavuika(mc.player.getId());
        if (state == null) return;
        float auraTicks = state.effectiveAuraTicks();
        float points = state.nightsoulPoints;
        if (auraTicks <= 0.0F && points <= 0.001F) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        float cx = sw * 0.5F - 38.0F;
        float cy = sh * 0.5F;

        float startDeg = 128.0F;
        float endDeg = 232.0F;
        int segments = 24;

        Matrix4f mat = guiGraphics.pose().last().pose();
        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        float ultiCdTicks = state.effectiveUltiCooldownTicks();
        boolean cdReady = ultiCdTicks <= 0.0F;
        boolean qReady = points >= 5.0F && cdReady;

        WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 29.0F, 35.0F, startDeg, endDeg, segments, 18, 12, 16, qReady ? 195 : 135);

        float nsFrac = Mth.clamp(points / 10.0F, 0.0F, 1.0F);
        if (nsFrac > 0.005F) {
            float fillEnd = Mth.lerp(nsFrac, startDeg, endDeg);
            float pulse = qReady ? (0.85F + 0.15F * Mth.sin(System.currentTimeMillis() * 0.01F)) : 1.0F;
            int r = qReady ? 255 : (cdReady ? 175 : 120);
            int g = qReady ? Math.round(215 * pulse) : (cdReady ? 75 : 48);
            int b = qReady ? Math.round(70 * pulse) : (cdReady ? 22 : 18);
            int a = qReady ? 250 : 175;
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 29.5F, 34.5F, startDeg, fillEnd, Math.max(2, Math.round(segments * nsFrac)), r, g, b, a);
        }

        float midDeg = (startDeg + endDeg) * 0.5F;
        WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 27.5F, 36.5F, midDeg - 1.8F, midDeg + 1.8F, 1,
                qReady ? 255 : 150, qReady ? 245 : 135, qReady ? 190 : 105, qReady ? 245 : 155);

        if (auraTicks > 0.0F) {
            float fuelFrac = Mth.clamp(state.nightsoulFuel() / state.maxNightsoulFuel(), 0.0F, 1.0F);
            int fuelTicks = state.netherite ? 12 : 8;
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 35.0F, 42.5F, startDeg, endDeg, segments, 24, 10, 10, 210);
            if (fuelFrac > 0.005F) {
                int fillSegs = Math.max(2, Math.round(segments * fuelFrac));
                for (int s = 0; s < fillSegs; s++) {
                    float f0 = (float) s / segments;
                    float f1 = Math.min(fuelFrac, (float) (s + 1) / segments);
                    if (f1 <= f0) break;
                    float d0 = Mth.lerp(f0, startDeg, endDeg);
                    float d1 = Mth.lerp(f1, startDeg, endDeg);
                    float t = (f0 + f1) * 0.5F;
                    int r = 240 + Math.round(15.0F * t);
                    int g = 38 + Math.round(135.0F * t);
                    int b = 16 + Math.round(28.0F * t);
                    WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 35.8F, 41.7F, d0, d1, 1, r, g, b, 245);
                }
            }
            for (int tick = 1; tick < fuelTicks; tick++) {
                float tickDeg = Mth.lerp(tick / (float) fuelTicks, startDeg, endDeg);
                WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 34.8F, 42.7F, tickDeg - 1.3F, tickDeg + 1.3F, 1, 10, 6, 6, 255);
            }
        }

        guiGraphics.flush();

        Font font = mc.font;
        int labelX = Math.round(cx - 46);
        int labelY = Math.round(cy - 4);
        String valText = String.format(Locale.ROOT, "%.1f", points);
        int valColor = qReady ? 0xFFFFE066 : (cdReady ? 0xFFCC7744 : 0xFF996655);
        guiGraphics.drawString(font, valText, labelX, labelY - 5, valColor, true);
        if (qReady) {
            guiGraphics.drawString(font, "[R]", labelX + 2, labelY + 5, 0xFFFFF0A0, true);
        } else if (!cdReady) {
            String cdText = String.format(Locale.ROOT, "%.0fs", Math.ceil(ultiCdTicks / 20.0F));
            guiGraphics.drawString(font, cdText, labelX + 2, labelY + 5, 0xFFAA8877, true);
        }
    }

    public static boolean applyBikeRidingHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model) {
        if (!(entity.getVehicle() instanceof FlamestriderBikeEntity bike)) return false;
        HumanoidArm arm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS);
        float dir = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        ModelPart weaponArm = (arm == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        ModelPart handleArm = (arm == HumanoidArm.RIGHT) ? model.leftArm : model.rightArm;

        int ultiT = bike.getUltiTicks();
        int step = bike.getNaStep();
        int naT = bike.getNaTicks();
        if (ultiT > 0) {
            handleArm.xRot = -1.15F;
            handleArm.yRot = 0.16F * dir;
            handleArm.zRot = -0.05F * dir;
            if (ultiT >= 19) {
                float p1 = WindGliderRenderer.smoothStep01((30.0F - ultiT) / 11.0F);
                model.body.xRot = Mth.lerp(p1, 0.22F, 0.04F);
                model.body.yRot = 0.28F * p1 * dir;
                weaponArm.xRot = Mth.lerp(p1, -1.15F, -1.48F);
                weaponArm.yRot = Mth.lerp(p1, -0.16F, -1.05F) * dir;
                weaponArm.zRot = Mth.lerp(p1, 0.05F, -0.35F) * dir;
            } else if (ultiT > 8) {
                float p2 = (19.0F - ultiT) / 11.0F;
                if (p2 < 0.55F) {
                    model.body.xRot = 0.04F;
                    model.body.yRot = 0.28F * dir;
                    weaponArm.xRot = -1.48F;
                    weaponArm.yRot = -1.05F * dir;
                    weaponArm.zRot = -0.35F * dir;
                } else {
                    float slashP = WindGliderRenderer.smoothStep01((p2 - 0.55F) / 0.45F);
                    model.body.xRot = Mth.lerp(slashP, 0.04F, 0.32F);
                    model.body.yRot = Mth.lerp(slashP, 0.28F, -0.42F) * dir;
                    weaponArm.xRot = Mth.lerp(slashP, -1.48F, -1.28F);
                    weaponArm.yRot = Mth.lerp(slashP, -1.05F, 1.05F) * dir;
                    weaponArm.zRot = Mth.lerp(slashP, -0.35F, 0.72F) * dir;
                }
            } else {
                float rec = WindGliderRenderer.smoothStep01((8.0F - ultiT) / 8.0F);
                model.body.xRot = Mth.lerp(rec, 0.32F, 0.22F);
                model.body.yRot = Mth.lerp(rec, -0.42F, 0.0F) * dir;
                weaponArm.xRot = Mth.lerp(rec, -1.28F, -1.15F);
                weaponArm.yRot = Mth.lerp(rec, 1.05F, -0.16F) * dir;
                weaponArm.zRot = Mth.lerp(rec, 0.72F, 0.05F) * dir;
            }
            return true;
        }
        if (bike.isSpinning()) {
            model.body.xRot = 0.22F;
            model.body.yRot = -0.15F * dir;
            handleArm.xRot = -1.15F;
            handleArm.yRot = 0.16F * dir;
            handleArm.zRot = -0.05F * dir;
            weaponArm.xRot = -1.05F;
            weaponArm.yRot = -0.95F * dir;
            weaponArm.zRot = 0.35F * dir;
            return true;
        }
        model.body.xRot = 0.22F;
        model.body.yRot = 0.0F;
        handleArm.xRot = -1.15F;
        handleArm.yRot = 0.16F * dir;
        handleArm.zRot = -0.05F * dir;
        if (step == 6 && naT > 0) {
            if (naT > 12) {
                float w = (16.0F - naT) / 4.0F;
                weaponArm.xRot = Mth.lerp(w, -1.15F, -2.75F);
                weaponArm.yRot = Mth.lerp(w, -0.16F, -0.20F) * dir;
                weaponArm.zRot = Mth.lerp(w, 0.05F, -0.22F) * dir;
            } else if (naT > 8) {
                float c = (12.0F - naT) / 4.0F;
                weaponArm.xRot = Mth.lerp(c, -2.75F, -1.55F);
                weaponArm.yRot = Mth.lerp(c, -0.20F, -1.05F) * dir;
                weaponArm.zRot = Mth.lerp(c, -0.22F, -0.18F) * dir;
            } else if (naT > 2) {
                float s = (8.0F - naT) / 6.0F;
                weaponArm.xRot = Mth.lerp(s, -1.55F, -0.78F);
                weaponArm.yRot = Mth.lerp(s, -1.05F, 1.05F) * dir;
                weaponArm.zRot = Mth.lerp(s, -0.18F, 0.72F) * dir;
            } else {
                float r = (2.0F - naT) / 2.0F;
                weaponArm.xRot = Mth.lerp(r, -0.78F, -1.15F);
                weaponArm.yRot = Mth.lerp(r, 1.05F, -0.16F) * dir;
                weaponArm.zRot = Mth.lerp(r, 0.72F, 0.05F) * dir;
            }
        } else if (step == 1 && naT > 0) {
            float t = 1.0F - (naT / 10.0F);
            weaponArm.xRot = Mth.lerp(t, -2.25F, -0.45F);
            weaponArm.yRot = Mth.lerp(t, 0.45F, -0.75F) * dir;
            weaponArm.zRot = Mth.lerp(t, 0.35F, -0.18F) * dir;
        } else if (step == 2 && naT > 0) {
            float t = 1.0F - (naT / 10.0F);
            weaponArm.xRot = Mth.lerp(t, -0.45F, -2.15F);
            weaponArm.yRot = Mth.lerp(t, -0.75F, 0.55F) * dir;
            weaponArm.zRot = Mth.lerp(t, -0.15F, 0.40F) * dir;
        } else if (step == 3 && naT > 0) {
            float t = 1.0F - (naT / 10.0F);
            weaponArm.xRot = -1.35F;
            weaponArm.yRot = Mth.lerp(t, 0.78F, -0.88F) * dir;
            weaponArm.zRot = Mth.lerp(t, 0.55F, -0.20F) * dir;
        } else if (step == 4 && naT > 0) {
            float t = 1.0F - (naT / 10.0F);
            weaponArm.xRot = Mth.lerp(t, -0.95F, -1.65F);
            weaponArm.yRot = Mth.lerp(t, -0.82F, 0.75F) * dir;
            weaponArm.zRot = Mth.lerp(t, -0.25F, 0.50F) * dir;
        } else if (step == 5 && naT > 0) {
            float t = 1.0F - (naT / 10.0F);
            weaponArm.xRot = Mth.lerp(t, -2.65F, -0.30F);
            weaponArm.yRot = Mth.lerp(t, 0.28F, -0.60F) * dir;
            weaponArm.zRot = Mth.lerp(t, 0.25F, -0.10F) * dir;
        } else {
            weaponArm.xRot = -1.15F;
            weaponArm.yRot = -0.16F * dir;
            weaponArm.zRot = 0.05F * dir;
        }
        return true;
    }

    public static boolean applyBurstHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model, int activeAnim, float t) {
        if (activeAnim != ClientAuraTracker.ANIM_MAVUIKA_BURST) return false;
        HumanoidArm arm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS);
        float dir = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        ModelPart mainArm = (arm == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        ModelPart offArm = (arm == HumanoidArm.RIGHT) ? model.leftArm : model.rightArm;
        float lift = WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.38F));
        float cleave = (t < 0.38F) ? 0.0F : WindGliderRenderer.smoothStep01(Math.min(1.0F, (t - 0.38F) / 0.32F));
        float w = (t < 0.78F) ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.78F) / 0.22F);
        float armX = (t < 0.38F) ? Mth.lerp(lift, -1.20F, -2.80F) : Mth.lerp(cleave, -2.80F, -0.35F);
        mainArm.xRot = Mth.lerp(w, mainArm.xRot, armX);
        offArm.xRot = Mth.lerp(w, offArm.xRot, armX * 0.92F);
        mainArm.yRot = Mth.lerp(w, mainArm.yRot, -0.22F * dir);
        offArm.yRot = Mth.lerp(w, offArm.yRot, 0.22F * dir);
        model.body.xRot = Mth.lerp(cleave, -0.14F, 0.24F) * w;
        return true;
    }

    public static boolean applyThirdPersonItemTransform(
            LivingEntity entity,
            ArcaneStaffItem.Kind effKind,
            boolean leftHand,
            PoseStack poseStack
    ) {
        if (effKind != ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) return false;
        if (!(entity.getVehicle() instanceof FlamestriderBikeEntity bike)) return false;
        int ultiT = bike.getUltiTicks();
        int naT = bike.getNaTicks();
        boolean activeAttack = ultiT > 0 || naT > 0 || bike.isSpinning() || bike.isPlunging();
        float sign = leftHand ? -1.0F : 1.0F;
        if (!activeAttack) {
            float pt = Minecraft.getInstance().getFrameTime();
            float bob = Mth.sin(((float) entity.tickCount + pt) * 0.12F) * 0.04F;
            poseStack.translate((leftHand ? 1.0F : -1.0F) / 16.0F, -0.125F, 0.625F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotation(1.15F));
            poseStack.mulPose(Axis.YP.rotation(0.16F * sign));
            poseStack.mulPose(Axis.ZP.rotation(-0.05F * sign));
            poseStack.translate(0.3125F * sign, -0.32F + bob, 0.30F);
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-55.0F * sign));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F * sign));
            return true;
        }
        if (ultiT <= 0 && naT > 0 && !bike.isSpinning() && !bike.isPlunging()) {
            float pt = Minecraft.getInstance().getFrameTime();
            int step = bike.getNaStep();
            float total = (step == 6) ? 16.0F : 10.0F;
            float nt = Mth.clamp(1.0F - ((naT - pt) / total), 0.0F, 1.0F);
            if (step == 6) {
                if (nt < 0.28F) {
                    float w = nt / 0.28F;
                    poseStack.translate(0.0F, 0.14F * w, -0.06F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(w, -8.0F, -38.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(48.0F * sign * w));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-14.0F * sign * w));
                } else if (nt < 0.50F) {
                    float u = (nt - 0.28F) / 0.22F;
                    poseStack.translate(0.0F, Mth.lerp(u, 0.14F, 0.06F), Mth.lerp(u, -0.06F, -0.14F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(u, -38.0F, -25.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(u, 48.0F, 82.0F) * sign));
                    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(u, -14.0F, -28.0F) * sign));
                } else {
                    float s = Mth.clamp((nt - 0.50F) / 0.375F, 0.0F, 1.0F);
                    poseStack.translate(0.0F, Mth.lerp(s, 0.06F, -0.04F), -0.16F);
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, -25.0F, -48.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(78.0F * sign));
                    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, -28.0F, 28.0F) * sign));
                }
            } else if (step == 3 || step == 4) {
                float dirSign = (step == 3 ? -1.0F : 1.0F) * sign;
                poseStack.translate(0.0F, 0.04F, -0.12F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-26.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(75.0F * dirSign));
            } else {
                float w = Mth.sin(nt * (float) Math.PI);
                poseStack.translate(0.0F, 0.04F, -0.12F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F * w - 12.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees((step == 2 ? 32.0F : -24.0F) * sign));
            }
            return true;
        }
        if (ultiT > 0) {
            if (ultiT >= 19) {
                float p1 = WindGliderRenderer.smoothStep01((30.0F - ultiT) / 11.0F);
                poseStack.translate(-sign * 0.10F * p1, 0.06F * p1, -0.08F * p1);
                poseStack.mulPose(Axis.XP.rotationDegrees(-32.0F * p1));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sign * 72.0F * p1));
                poseStack.mulPose(Axis.YP.rotationDegrees(-sign * 20.0F * p1));
            } else if (ultiT > 8) {
                float p2 = (19.0F - ultiT) / 11.0F;
                float slashP = (p2 < 0.55F) ? 0.0F : WindGliderRenderer.smoothStep01((p2 - 0.55F) / 0.45F);
                poseStack.translate(sign * Mth.lerp(slashP, -0.10F, 0.12F), Mth.lerp(slashP, 0.06F, -0.04F), Mth.lerp(slashP, -0.08F, -0.18F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slashP, -32.0F, -44.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sign * Mth.lerp(slashP, 72.0F, -76.0F)));
                poseStack.mulPose(Axis.YP.rotationDegrees(sign * Mth.lerp(slashP, -20.0F, 26.0F)));
            } else {
                float r = 1.0F - WindGliderRenderer.smoothStep01((8.0F - ultiT) / 8.0F);
                poseStack.translate(sign * 0.12F * r, -0.04F * r, -0.18F * r);
                poseStack.mulPose(Axis.XP.rotationDegrees(-44.0F * r));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-sign * 76.0F * r));
                poseStack.mulPose(Axis.YP.rotationDegrees(sign * 26.0F * r));
            }
        }
        return true;
    }

    public static boolean applyFirstPersonTransform(
            AbstractClientPlayer player,
            float partialTicks,
            float dir,
            ArcaneStaffItem.Kind effKind,
            int activeAnim,
            ClientAuraTracker.AuraDisplayData data,
            long now,
            PoseStack poseStack
    ) {
        if (effKind == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS
                && player.getVehicle() instanceof FlamestriderBikeEntity bike) {
            int ultiT = bike.getUltiTicks();
            if (ultiT > 0) {
                float ut = ultiT - partialTicks;
                if (ut >= 19.0F) {
                    float p1 = WindGliderRenderer.smoothStep01((30.0F - ut) / 11.0F);
                    poseStack.translate(-dir * 0.28F * p1, 0.06F * p1, -0.16F * p1);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-26.0F * p1));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 72.0F * p1));
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * 16.0F * p1));
                } else if (ut > 8.0F) {
                    float p2 = (19.0F - ut) / 11.0F;
                    float slashP = (p2 < 0.55F) ? 0.0F : WindGliderRenderer.smoothStep01((p2 - 0.55F) / 0.45F);
                    poseStack.translate(dir * Mth.lerp(slashP, -0.28F, 0.34F), Mth.lerp(slashP, 0.06F, -0.06F), Mth.lerp(slashP, -0.16F, -0.30F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slashP, -26.0F, -46.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slashP, 72.0F, -74.0F)));
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(slashP, 16.0F, -20.0F)));
                } else {
                    float r = 1.0F - WindGliderRenderer.smoothStep01((8.0F - ut) / 8.0F);
                    poseStack.translate(dir * 0.34F * r, -0.06F * r, -0.30F * r);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-46.0F * r));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * 74.0F * r));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 20.0F * r));
                }
                return true;
            }
            int naT = bike.getNaTicks();
            if (naT <= 0 && !bike.isSpinning() && !bike.isPlunging()) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
            if (naT > 0) {
                int step = bike.getNaStep();
                float total = (step == 6) ? 16.0F : 10.0F;
                float nt = Mth.clamp(1.0F - ((naT - partialTicks) / total), 0.0F, 1.0F);
                float w = Mth.sin(nt * (float) Math.PI);
                if (step == 6) {
                    if (nt < 0.25F) {
                        float lift = WindGliderRenderer.smoothStep01(nt / 0.25F);
                        poseStack.translate(dir * Mth.lerp(lift, 0.08F, 0.04F), Mth.lerp(lift, 0.16F, 0.42F), Mth.lerp(lift, -0.10F, -0.18F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(lift, -14.0F, -8.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(lift, 18.0F, 28.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(lift, 4.0F, 6.0F)));
                    } else if (nt < 0.48F) {
                        float u = WindGliderRenderer.smoothStep01((nt - 0.25F) / 0.23F);
                        poseStack.translate(dir * Mth.lerp(u, 0.04F, -0.38F), Mth.lerp(u, 0.42F, 0.22F), Mth.lerp(u, -0.18F, -0.24F));
                        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(u, -8.0F, -34.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(u, 28.0F, 72.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(u, 6.0F, 18.0F)));
                    } else if (nt < 0.84F) {
                        float s = WindGliderRenderer.smoothStep01((nt - 0.48F) / 0.36F);
                        poseStack.translate(dir * Mth.lerp(s, -0.38F, 0.42F), Mth.lerp(s, 0.22F, -0.18F), -0.24F - 0.12F * Mth.sin(s * (float) Math.PI));
                        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, -34.0F, -54.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(s, 72.0F, -76.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(s, 18.0F, -24.0F)));
                    } else {
                        float r = 1.0F - WindGliderRenderer.smoothStep01((nt - 0.84F) / 0.16F);
                        poseStack.translate(dir * 0.42F * r, -0.18F * r, -0.24F * r);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-54.0F * r));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * 76.0F * r));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 24.0F * r));
                    }
                } else if (step == 1) {
                    poseStack.translate(dir * Mth.lerp(nt, 0.34F, -0.34F), Mth.lerp(nt, 0.26F, -0.26F), -0.22F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-42.0F * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(nt, -18.0F, 48.0F)));
                } else if (step == 2) {
                    poseStack.translate(dir * Mth.lerp(nt, -0.32F, 0.34F), Mth.lerp(nt, -0.24F, 0.24F), -0.22F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-36.0F * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(nt, 48.0F, -22.0F)));
                } else if (step == 3) {
                    poseStack.translate(dir * Mth.lerp(nt, 0.38F, -0.38F), 0.0F, -0.24F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(nt, -52.0F, 58.0F)));
                } else if (step == 4) {
                    poseStack.translate(dir * Mth.lerp(nt, -0.36F, 0.36F), Mth.lerp(nt, -0.06F, 0.14F), -0.24F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-26.0F * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(nt, 54.0F, -48.0F)));
                } else {
                    poseStack.translate(dir * Mth.lerp(nt, 0.20F, -0.24F), Mth.lerp(nt, 0.34F, -0.34F), -0.26F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(nt, 22.0F, -72.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(nt, -10.0F, 32.0F)));
                }
                return true;
            }
        }
        if (activeAnim == ClientAuraTracker.ANIM_MAVUIKA_BURST && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = Mth.sin(t * (float) Math.PI);
            float slash = (t < 0.40F) ? -WindGliderRenderer.smoothStep01(t / 0.40F) : WindGliderRenderer.smoothStep01(Math.min(1.0F, (t - 0.40F) / 0.35F));
            poseStack.translate(dir * 0.18F * slash, -0.22F * slash, -0.24F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(-48.0F * slash));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * 38.0F * slash));
            return true;
        }
        return false;
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
        boolean isUlti = (arc.color == 0xFFFF6A00 || arc.color == 0xFFFF6A01);
        boolean isFinisher = (arc.color == 0xFFFF7A18 || arc.color == 0xFFFF7A19);
        boolean isNaRightToLeft = (arc.color == 0xFFFF8825 || arc.color == 0xFFFF7A19 || arc.color == 0xFFFF6A01);
        boolean isNaLeftToRight = (arc.color == 0xFFFF8824);
        if (!isUlti && !isFinisher && !isNaLeftToRight && !isNaRightToLeft) return false;

        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        double headRaw = Mth.clamp(prog / (isUlti ? 0.28D : 0.40D), 0.0D, 1.0D);
        double headT = 1.0D - (1.0D - headRaw) * (1.0D - headRaw);
        double tailRaw = isUlti
                ? Mth.clamp((prog - 0.42D) / 0.58D, 0.0D, 1.0D)
                : Mth.clamp((prog - 0.24D) / 0.74D, 0.0D, 1.0D);
        double tailT = tailRaw * tailRaw;
        if (headT - tailT < 0.02D) return true;
        float fade = isUlti
                ? ((prog < 0.22F) ? (prog / 0.22F) : (1.0F - (prog - 0.22F) / 0.78F))
                : ((prog < 0.18F) ? (prog / 0.18F) : (1.0F - (prog - 0.18F) / 0.82F));
        int alpha = Mth.clamp((int) (255.0F * fade), 10, 255);
        Vec3 sCam = startPos.subtract(camPos);
        Vec3 eCam = endPos.subtract(camPos);
        Vec3 chord = eCam.subtract(sCam);
        double span = chord.length();
        if (span > 1.0E-3D) {
            Vec3 horizChord = new Vec3(chord.x, 0.0D, chord.z);
            double bowSign = isNaRightToLeft ? -1.0D : 1.0D;
            Vec3 forwardBowDir = horizChord.lengthSqr() > 1.0E-4D
                    ? new Vec3(0.0D, 1.0D, 0.0D).cross(horizChord).normalize().scale(bowSign)
                    : new Vec3(0.0D, 0.0D, 1.0D);
            double arcScale = isUlti ? 1.0D : (isFinisher ? 0.66D : 0.42D);
            double fadeScale = (1.15D - prog * 0.35D) * arcScale;
            double bowOffset = isUlti ? 0.34D : (isFinisher ? 0.32D : 0.28D);
            int segs = 16;
            Vec3[] pts = new Vec3[segs + 1];
            for (int i = 0; i <= segs; i++) {
                double localT = i / (double) segs;
                double u = Mth.lerp(localT, tailT, headT);
                double bow = Math.sin(u * Math.PI);
                pts[i] = sCam.lerp(eCam, u)
                        .add(forwardBowDir.scale(bow * span * bowOffset))
                        .add(0.0D, bow * span * 0.08D, 0.0D);
            }
            double[] layerHalfWidths = {0.78D, 0.50D, 0.28D, 0.11D};
            int[][] layerColors = {
                    {220, 25, 10, Math.max(10, alpha / 2)},
                    {255, 80, 14, alpha},
                    {255, 178, 35, alpha},
                    {255, 250, 225, alpha}
            };
            Vec3[] prevOff = new Vec3[4];
            for (int i = 0; i <= segs; i++) {
                double t = i / (double) segs;
                double taper = Math.pow(Math.sin(t * Math.PI), 0.65D);
                Vec3 tangent = (i == 0 ? pts[1].subtract(pts[0])
                        : (i == segs ? pts[segs].subtract(pts[segs - 1])
                        : pts[i + 1].subtract(pts[i - 1]))).normalize();
                Vec3 toCam = pts[i].lengthSqr() > 1.0E-4D ? pts[i].scale(-1.0D).normalize() : new Vec3(0.0D, 1.0D, 0.0D);
                Vec3 perp = tangent.cross(toCam);
                if (perp.lengthSqr() < 1.0E-4D) perp = tangent.cross(new Vec3(1.0D, 0.0D, 0.0D));
                perp = perp.normalize();
                for (int l = 0; l < 4; l++) {
                    Vec3 curOff = perp.scale(layerHalfWidths[l] * fadeScale * taper);
                    if (i > 0) {
                        Vec3 a0 = pts[i - 1].subtract(prevOff[l]);
                        Vec3 b0 = pts[i - 1].add(prevOff[l]);
                        Vec3 b1 = pts[i].add(curOff);
                        Vec3 a1 = pts[i].subtract(curOff);
                        int[] c = layerColors[l];
                        ClientAuraRenderer.emitCrystalQuadPublic(consumer, matrix,
                                (float) a0.x, (float) a0.y, (float) a0.z, c[0], c[1], c[2], c[3],
                                (float) b0.x, (float) b0.y, (float) b0.z, c[0], c[1], c[2], c[3],
                                (float) b1.x, (float) b1.y, (float) b1.z, c[0], c[1], c[2], c[3],
                                (float) a1.x, (float) a1.y, (float) a1.z, c[0], c[1], c[2], c[3]);
                    }
                    prevOff[l] = curOff;
                }
            }
        }
        return true;
    }
}
