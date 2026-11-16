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
import net.minecraft.client.model.geom.ModelPart;
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

public final class TsaritsaVisualRenderer {
    private TsaritsaVisualRenderer() {}

    public static boolean isTsaritsaPulling(LivingEntity entity, ItemStack stack) {
        if (entity == null || stack == null || stack.isEmpty()) return false;
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.TSARITSA_BOW) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity.getId() != mc.player.getId()) return false;
        if (ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) return false;
        return ClientAuraTracker.localTsaritsaBowPullTicks > 0
                && (entity.getMainHandItem() == stack || entity.getOffhandItem() == stack);
    }

    public static float getTsaritsaPullProgress(LivingEntity entity, ItemStack stack) {
        if (!isTsaritsaPulling(entity, stack)) return 0.0F;
        return Mth.clamp(ClientAuraTracker.localTsaritsaBowPullTicks / 16.0F, 0.0F, 1.0F);
    }

    public static boolean renderHoldEChargeHud(GuiGraphics guiGraphics) {
        if (ClientAuraTracker.localTsaritsaHoldETicks > 0 && !ClientAuraTracker.localTsaritsaHoldELocked) {
            float pct = Mth.clamp(ClientAuraTracker.localTsaritsaHoldETicks / 16.0F, 0.0F, 1.0F);
            int sw = guiGraphics.guiWidth();
            int sh = guiGraphics.guiHeight();
            int cx = sw / 2;
            int cy = sh / 2;
            int crownY = cy + 10;
            int crownCol = 0xFF7DD3FC;
            guiGraphics.fill(cx - 5, crownY + 3, cx + 5, crownY + 5, crownCol);
            guiGraphics.fill(cx - 5, crownY + 1, cx - 3, crownY + 3, crownCol);
            guiGraphics.fill(cx - 1, crownY, cx + 1, crownY + 3, 0xFFE0F2FE);
            guiGraphics.fill(cx + 3, crownY + 1, cx + 5, crownY + 3, crownCol);
            int barW = 40;
            int barH = 3;
            int bx = cx - barW / 2;
            int by = cy + 17;
            guiGraphics.fill(bx - 1, by - 1, bx + barW + 1, by + barH + 1, 0x90000000);
            int fillW = Math.round(barW * pct);
            if (fillW > 0) {
                guiGraphics.fill(bx, by, bx + fillW, by + barH, pct >= 1.0F ? 0xFFE0F2FE : 0xFF38BDF8);
            }
            return true;
        }
        return false;
    }

    public static boolean applyBowPullHumanoidPose(LivingEntity entity, HumanoidModel<?> model) {
        Minecraft mcLocal = Minecraft.getInstance();
        if (mcLocal.player != null && entity.getId() == mcLocal.player.getId()
                && ClientAuraTracker.localTsaritsaBowPullTicks > 0
                && !ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) {
            float pull01 = Mth.clamp(ClientAuraTracker.localTsaritsaBowPullTicks / 16.0F, 0.0F, 1.0F);
            VentiVisualRenderer.applyDaybreakBowHumanoidPose(model, entity.getMainArm(), pull01);
            return true;
        }
        return false;
    }

    public static boolean applyActionHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model, int activeAnim, float t) {
        if (activeAnim >= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1 && activeAnim <= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_3) {
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.TSARITSA_SPEAR);
            ModelPart mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            ModelPart offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;
            float w = (t < 0.65F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.65F) / 0.35F) * (float) Math.PI));

            if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1) {
                float slash = WindGliderRenderer.smoothStep01(t / 0.55F);

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, -1.38F);
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, 0.85F, -0.92F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, 0.42F, -0.36F));

                offPart.xRot = Mth.lerp(w, offPart.xRot, Mth.lerp(slash, -0.75F, 0.20F));
                offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * Mth.lerp(slash, 0.35F, -0.45F));

                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.28F);
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.22F);
                model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.08F);
                model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.08F);
                model.head.xRot += 0.06F * w;
                model.hat.copyFrom(model.head);
            } else if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_2) {
                float slash = WindGliderRenderer.smoothStep01(t / 0.55F);

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -0.65F, -2.15F));
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, -0.88F, 0.72F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, -0.42F, 0.38F));

                offPart.xRot = Mth.lerp(w, offPart.xRot, Mth.lerp(slash, 0.15F, -0.65F));
                offPart.yRot = Mth.lerp(w, offPart.yRot, offDir * Mth.lerp(slash, -0.30F, 0.40F));

                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, -0.18F);
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, 0.30F);
                model.head.xRot += 0.04F * w;
                model.hat.copyFrom(model.head);
            } else {
                float lift = WindGliderRenderer.smoothStep01(t / 0.32F);
                float slam = WindGliderRenderer.smoothStep01((t - 0.32F) / 0.45F);
                float armX = (t < 0.32F) ? Mth.lerp(lift, -1.35F, -2.65F) : Mth.lerp(slam, -2.65F, -0.45F);

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, armX);
                mainPart.yRot = castDir * (t < 0.32F ? 0.20F : -0.10F) * w;
                mainPart.zRot = castDir * (t < 0.32F ? 0.18F : -0.08F) * w;

                offPart.xRot = Mth.lerp(w, offPart.xRot, (t < 0.32F) ? Mth.lerp(lift, -0.55F, -1.85F) : Mth.lerp(slam, -1.85F, -0.65F));
                offPart.yRot = offDir * 0.25F * w;

                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, (t < 0.32F) ? 0.18F : 0.42F);
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, (t < 0.32F) ? -0.18F : -0.36F);
                model.head.xRot += (t < 0.32F ? -0.12F : 0.18F) * w;
                model.hat.copyFrom(model.head);
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_TAP_E || activeAnim == ClientAuraTracker.ANIM_TSARITSA_HOLD_E) {
            float w = Mth.sin(t * (float) Math.PI);
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -2.25F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.45F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_SUPPORT) {
            float w = (t < 0.20F) ? WindGliderRenderer.smoothStep01(t / 0.20F) : (t < 0.80F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.80F) / 0.20F));
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.15F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.15F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.82F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.82F);
            model.head.xRot -= 0.16F * w;
            model.hat.copyFrom(model.head);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_DPS) {
            float w = (t < 0.18F) ? WindGliderRenderer.smoothStep01(t / 0.18F) : (t < 0.82F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.82F) / 0.18F));
            if (t < 0.52F) {
                float prep = WindGliderRenderer.smoothStep01(t / 0.52F);
                model.body.yRot = 0.38F * prep * w;
                model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, Mth.lerp(prep, -1.60F, -2.90F));
                model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.22F * prep);
                model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.15F * prep);
            } else {
                float hurl = WindGliderRenderer.smoothStep01(Math.min(1.0F, (t - 0.52F) / 0.24F));
                model.body.yRot = Mth.lerp(hurl, 0.38F, -0.34F) * w;
                model.body.xRot = 0.22F * hurl * w;
                model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, Mth.lerp(hurl, -2.90F, -0.55F));
                model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, Mth.lerp(hurl, -1.15F, 0.35F));
            }
            return true;
        }
        return false;
    }

    public static boolean applyFirstPersonTransform(
            AbstractClientPlayer player,
            float dir,
            ArcaneStaffItem.Kind effKind,
            int activeAnim,
            ClientAuraTracker.AuraDisplayData data,
            long now,
            PoseStack poseStack
    ) {
        if (effKind != ArcaneStaffItem.Kind.TSARITSA_SPEAR
                && !(effKind == ArcaneStaffItem.Kind.TSARITSA_BOW && ClientAuraTracker.isTsaritsaSpearStance(player.getId()))) {
            return false;
        }
        if (activeAnim >= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1 && activeAnim <= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_3 && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.65F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.65F) / 0.35F) * (float) Math.PI));
            if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1) {
                float slash = WindGliderRenderer.smoothStep01(t / 0.52F);
                poseStack.translate(dir * Mth.lerp(slash, 0.28F, -0.32F) * w, Mth.lerp(slash, -0.06F, 0.10F) * w, -0.26F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slash, -22.0F, -46.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, -55.0F, 68.0F) * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 18.0F * w));
            } else if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_2) {
                float slash = WindGliderRenderer.smoothStep01(t / 0.52F);
                poseStack.translate(dir * Mth.lerp(slash, -0.28F, 0.26F) * w, Mth.lerp(slash, 0.18F, -0.16F) * w, -0.24F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slash, -52.0F, -16.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, 64.0F, -54.0F) * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 14.0F * w));
            } else {
                float lift = WindGliderRenderer.smoothStep01(t / 0.32F);
                float drop = WindGliderRenderer.smoothStep01((t - 0.32F) / 0.45F);
                float ty = (t < 0.32F) ? Mth.lerp(lift, 0.02F, 0.30F) : Mth.lerp(drop, 0.30F, -0.32F);
                float tz = (t < 0.32F) ? Mth.lerp(lift, -0.18F, -0.10F) : Mth.lerp(drop, -0.10F, -0.38F);
                float xp = (t < 0.32F) ? Mth.lerp(lift, -20.0F, 24.0F) : Mth.lerp(drop, 24.0F, -82.0F);
                poseStack.translate(dir * 0.05F * w, ty * w, tz * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(xp * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 12.0F * w));
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_DPS && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float w = (t < 0.85F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.85F) / 0.15F) * (float) Math.PI));
            if (t < 0.52F) {
                float prep = WindGliderRenderer.smoothStep01(t / 0.52F);
                poseStack.translate(dir * 0.12F * prep * w, 0.14F * prep * w, -0.12F * prep * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(-68.0F * prep * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 22.0F * prep * w));
            } else {
                float hurl = WindGliderRenderer.smoothStep01((t - 0.52F) / 0.28F);
                poseStack.translate(dir * Mth.lerp(hurl, 0.12F, -0.05F) * w, Mth.lerp(hurl, 0.14F, -0.26F) * w, Mth.lerp(hurl, -0.12F, -0.52F) * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(hurl, -68.0F, -18.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(hurl, 22.0F, -8.0F) * w));
            }
            return true;
        }
        return false;
    }

    public static void renderEntityEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        long now = System.currentTimeMillis();
        int activeAnim = (d != null && now < d.staffAnimUntil) ? d.staffAnimType : 0;
        boolean crown = ClientAuraTracker.isTsaritsaCrownActive(id) && !ClientAuraTracker.isTsaritsaSpearStance(id);
        boolean supportBurst = ClientAuraTracker.isTsaritsaSupportBurst(id) || activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_SUPPORT;
        boolean dpsSeal = ClientAuraTracker.isTsaritsaDpsSeal(id);

        renderIdleThrone(entity, poseStack, buffer);
        if (!crown && !supportBurst && !dpsSeal && activeAnim != ClientAuraTracker.ANIM_TSARITSA_BURST_DPS) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        if (isFirstPersonSelf) return;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        if (d != null && !d.tsaritsaIdleYawSet) {
            d.tsaritsaIdleYaw = entity.yBodyRot;
            d.tsaritsaIdleYawSet = true;
        }
        float bodyYaw = (d != null && d.tsaritsaIdleYawSet) ? d.tsaritsaIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer. getTranslucentNoDepthWrite());

        if (crown) {
            VertexConsumer crownVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
            poseStack.pushPose();
            float bob = Mth.sin(time * 0.15F) * 0.05F;
            poseStack.translate(0.0F, entity.getBbHeight() + 0.42F + bob, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.2F));
            Matrix4f mat = poseStack.last().pose();
            float hs = 0.25F;
            float baseH = 0.11F;
            float peakH = 0.24F;
            int br = 180, bg = 238, bb = 255;
            int pr = 235, pg = 252, pb = 255;
            float[][] faces = new float[][]{
                    {-hs, -hs,  hs, -hs},
                    { hs,  hs, -hs,  hs},
                    {-hs,  hs, -hs, -hs},
                    { hs, -hs,  hs,  hs}
            };
            for (float[] f : faces) {
                float x0 = f[0], z0 = f[1], x1 = f[2], z1 = f[3];
                float mx = (x0 + x1) * 0.5F, mz = (z0 + z1) * 0.5F;
                WindGliderRenderer.emitDoubleQuad(crownVc, mat, x0, 0.0F, z0, x1, 0.0F, z1, x1, baseH, z1, x0, baseH, z0, br, bg, bb, 255);
                WindGliderRenderer.emitDoubleQuad(crownVc, mat, x0, baseH, z0, x1, baseH, z1, mx, peakH, mz, mx, peakH, mz, pr, pg, pb, 255);
                WindGliderRenderer.renderCloverCrossXZ(crownVc, mat, mx, peakH + 0.04F, mz, 0.08F, 245, 255, 255, 255);
            }
            poseStack.popPose();
        }



        if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_DPS) {
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() + 0.35F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
            Matrix4f mat = poseStack.last().pose();
            WindGliderRenderer.emitDoubleQuad(vc, mat, -0.08F, 0.0F, -1.2F, 0.08F, 0.0F, -1.2F, 0.02F, 0.0F, 1.4F, -0.02F, 0.0F, 1.4F, 155, 235, 255, 240);
            WindGliderRenderer.emitDoubleQuad(vc, mat, 0.0F, -0.08F, -1.2F, 0.0F, 0.08F, -1.2F, 0.0F, 0.02F, 1.4F, 0.0F, -0.02F, 1.4F, 195, 245, 255, 240);
            poseStack.popPose();
        }

        if (dpsSeal) {
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.55F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-time * 3.5F));
            Matrix4f mat = poseStack.last().pose();
            for (int i = 0; i < 3; i++) {
                float ang = (float) (i * Math.PI * 2.0D / 3.0D);
                float sx = Mth.cos(ang) * 0.78F;
                float sz = Mth.sin(ang) * 0.78F;
                WindGliderRenderer.emitDoubleQuad(vc, mat, sx - 0.09F, -0.09F, sz, sx + 0.09F, -0.09F, sz, sx + 0.09F, 0.09F, sz, sx - 0.09F, 0.09F, sz, 175, 195, 255, 230);
            }
            poseStack.popPose();
        }
    }

    public static void renderFirstPersonIdleDesk(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.player.isUsingItem() || mc.player.swinging) return;
        if (!ClientAuraTracker.isTsaritsaIdle(mc.player.getId())) return;
        ClientAuraTracker.AuraDisplayData d1p = ClientAuraTracker.get(mc.player.getId());
        float fade1p = stepIdleFade(d1p, System.currentTimeMillis());
        if (fade1p <= 0.01F) return;
        float pt1p = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time1p = mc.player.tickCount + pt1p;
        float deskBob1p = Mth.sin(time1p * 0.10F) * 0.014F;
        float bodyYaw1p = Mth.rotLerp(pt1p, mc.player.yBodyRotO, mc.player.yBodyRot);
        Vec3 pPos = mc.player.getPosition(pt1p).subtract(camPos);
        poseStack.pushPose();
        poseStack.translate(pPos.x, pPos.y, pPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw1p));
        poseStack.translate(0.0F, 0.0F, -0.08F);
        poseStack.scale(fade1p, fade1p, fade1p);
        renderFloatingDeskAndItems(poseStack, bufferSource, time1p, deskBob1p, false);
        poseStack.popPose();
    }

    public static void renderWorldArcs(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ResourceLocation cryoArrowTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/projectiles/cryo_arrow.png");

        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.start == null || arc.end == null) continue;
            if (arc.fromEntityId == -6) {
                float radius = (float) Math.max(8.0D, arc.end.x);
                float cx = (float) (arc.start.x - camPos.x);
                float cy = (float) (arc.start.y - camPos.y);
                float cz = (float) (arc.start.z - camPos.z);
                float fadeIn = Mth.clamp((now - arc.startTime) / 300.0F, 0.0F, 1.0F);
                float fadeOut = Mth.clamp((arc.expireTime - now) / 400.0F, 0.0F, 1.0F);
                float fade = Math.min(fadeIn, fadeOut);
                float timeSec = (now - arc.startTime) * 0.001F;
                int aField = Math.max(20, Math.round(175.0F * fade));
                int aPillar = Math.max(30, Math.round(225.0F * fade));

                VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
                poseStack.pushPose();
                poseStack.translate(cx, cy, cz);
                Matrix4f mat = poseStack.last().pose();

                int segs = 32;
                float rOut = radius;
                float rRimIn = radius * 0.93F;
                float rMidOut = radius * 0.54F;
                float rMidIn = radius * 0.50F;
                for (int i = 0; i < segs; i++) {
                    float a0 = (float) (i * Math.PI * 2.0D / segs) - timeSec * 0.25F;
                    float a1 = (float) ((i + 1) * Math.PI * 2.0D / segs) - timeSec * 0.25F;
                    float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
                    float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            c0 * rRimIn, 0.05F, s0 * rRimIn,
                            c0 * rOut, 0.05F, s0 * rOut,
                            c1 * rOut, 0.05F, s1 * rOut,
                            c1 * rRimIn, 0.05F, s1 * rRimIn,
                            185, 242, 255, aField);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            c0 * rMidIn, 0.055F, s0 * rMidIn,
                            c0 * rMidOut, 0.055F, s0 * rMidOut,
                            c1 * rMidOut, 0.055F, s1 * rMidOut,
                            c1 * rMidIn, 0.055F, s1 * rMidIn,
                            220, 248, 255, Math.max(15, aField / 2));
                }
                for (int k = 0; k < 8; k++) {
                    float ang = (float) (k * Math.PI * 0.25D) + timeSec * 0.15F;
                    float ca = Mth.cos(ang), sa = Mth.sin(ang);
                    float px = -sa * 0.08F, pz = ca * 0.08F;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            ca * rMidIn - px, 0.06F, sa * rMidIn - pz,
                            ca * rMidIn + px, 0.06F, sa * rMidIn + pz,
                            ca * (rOut * 0.96F), 0.06F, sa * (rOut * 0.96F),
                            ca * (rOut * 0.96F), 0.06F, sa * (rOut * 0.96F),
                            220, 248, 255, aPillar);
                }

                float starHoverY = 1.25F + Mth.sin(timeSec * 2.0F) * 0.08F;
                poseStack.pushPose();
                poseStack.translate(0.0F, starHoverY, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(timeSec * 45.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(timeSec * 1.5F) * 8.0F));
                WindGliderRenderer.renderFourPointStar3D(vc, poseStack.last().pose(), 0.36F, 0.09F, 240, 252, 255, aPillar, 140, 220, 255, aField);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                WindGliderRenderer.renderFourPointStar3D(vc, poseStack.last().pose(), 0.24F, 0.06F, 220, 248, 255, aPillar, 110, 200, 255, aField);
                poseStack.popPose();

                WindGliderRenderer.renderCloverCrossXZ(vc, mat, 0.0F, 0.055F, 0.0F, 0.55F, 215, 245, 255, Math.max(15, aField / 2));

                poseStack.popPose();
            } else if (arc.fromEntityId == -7) {
                float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
                float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
                Vec3 diff = arc.end.subtract(arc.start);
                double len = diff.length();
                if (len < 1.0E-3D) continue;
                Vec3 dir = diff.scale(1.0D / len);
                float headFrac = Mth.clamp(prog * 1.35F, 0.0F, 1.0F);
                float tailFrac = Mth.clamp(headFrac - 0.38F, 0.0F, 1.0F);
                Vec3 headPos = arc.start.lerp(arc.end, headFrac);
                Vec3 tailPos = arc.start.lerp(arc.end, tailFrac);

                VertexConsumer trailVc = bufferSource.getBuffer(RenderType.lightning());
                Matrix4f trailMat = poseStack.last().pose();
                Vec3 hCam = headPos.subtract(camPos);
                Vec3 tCam = tailPos.subtract(camPos);
                Vec3 worldUp = Math.abs(dir.y) < 0.95D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
                Vec3 side = dir.cross(worldUp).normalize();
                ElementalReactionBadgeRenderer.drawQuadRibbon(trailVc, trailMat, tCam, hCam, side, 0.045D, 135, 228, 255, 195);
                ElementalReactionBadgeRenderer.drawQuadRibbon(trailVc, trailMat, tCam, hCam, worldUp, 0.022D, 235, 252, 255, 240);

                VertexConsumer arrowVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(cryoArrowTex));
                poseStack.pushPose();
                poseStack.translate(hCam.x, hCam.y, hCam.z);
                float yawDeg = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
                float pitchDeg = (float) Math.toDegrees(Math.asin(Mth.clamp(dir.y, -1.0D, 1.0D)));
                poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg - 90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(pitchDeg));
                poseStack.mulPose(Axis.XP.rotationDegrees(prog * 360.0F));
                float arrowLen = 1.05F;
                float arrowHalfH = 0.22F;
                for (int plane = 0; plane < 2; plane++) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.XP.rotationDegrees(plane * 90.0F));
                    PoseStack.Pose p = poseStack.last();
                    WindGliderRenderer.emitWingQuad(arrowVc, p, -arrowLen, -arrowHalfH, 0.0F, 0.0F, 1.0F, 0.15F, -arrowHalfH, 0.0F, 1.0F, 1.0F, 0.15F, arrowHalfH, 0.0F, 1.0F, 0.0F, -arrowLen, arrowHalfH, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                    WindGliderRenderer.emitWingQuad(arrowVc, p, -arrowLen, arrowHalfH, 0.0F, 0.0F, 0.0F, 0.15F, arrowHalfH, 0.0F, 1.0F, 0.0F, 0.15F, -arrowHalfH, 0.0F, 1.0F, 1.0F, -arrowLen, -arrowHalfH, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
        }
    }

    private static float stepIdleFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.tsaritsaIdleLastMs <= 0L) {
            d.tsaritsaIdleLastMs = nowMs;
            if (d.tsaritsaIdle) {
                d.tsaritsaIdleFade = Math.min(1.0F, d.tsaritsaIdleFade + 0.016F / 0.45F);
            }
        } else if (nowMs > d.tsaritsaIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.tsaritsaIdleLastMs) / 1000.0F);
            d.tsaritsaIdleLastMs = nowMs;
            if (d.tsaritsaIdle) {
                d.tsaritsaIdleFade = Math.min(1.0F, d.tsaritsaIdleFade + dt / 0.45F);
            } else {
                d.tsaritsaIdleFade = Math.max(0.0F, d.tsaritsaIdleFade - dt / 0.35F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.tsaritsaIdleFade);
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (entity.isUsingItem() || entity.swinging) {
            ClientAuraTracker.AuraDisplayData dAct = ClientAuraTracker.get(entity.getId());
            if (dAct != null) {
                dAct.tsaritsaIdle = false;
                dAct.tsaritsaIdleFade = 0.0F;
                dAct.tsaritsaIdleYawSet = false;
            }
            return false;
        }
        if (!ClientAuraTracker.isTsaritsaIdle(entity.getId())) {
            ClientAuraTracker.AuraDisplayData dOff = ClientAuraTracker.get(entity.getId());
            if (dOff != null) dOff.tsaritsaIdleYawSet = false;
            return false;
        }
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F) {
            if (d != null && !d.tsaritsaIdle) d.tsaritsaIdleYawSet = false;
            return false;
        }


        float sitDropY = 3.0F * w;
        model.body.y += sitDropY;
        model.head.y += sitDropY;
        model.hat.y += sitDropY;
        model.rightArm.y += sitDropY;
        model.leftArm.y += sitDropY;
        model.rightLeg.y += sitDropY;
        model.leftLeg.y += sitDropY;

        // Leaning slightly forward reviewing documents on the floating ice desk; head gently sways side-to-side reading
        float headSwayY = Mth.sin(ageInTicks * 0.06F) * 0.14F;
        float headTiltZ = Mth.sin(ageInTicks * 0.045F) * 0.06F;
        model.body.xRot = Mth.lerp(w, model.body.xRot, 0.08F);
        model.head.xRot = Mth.lerp(w, model.head.xRot, 0.36F + Mth.sin(ageInTicks * 0.09F) * 0.03F);
        model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.45F, 0.45F), headSwayY);
        model.head.zRot = Mth.lerp(w, model.head.zRot, headTiltZ);
        model.hat.copyFrom(model.head);
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        // Right hand reaches inward onto the open book and moves in exact sync with the writing ice quill
        float writeHoriz = Mth.sin(ageInTicks * 0.28F);
        float writeDepth = Mth.cos(ageInTicks * 0.16F);
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -0.96F - writeDepth * 0.065F);
        model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.36F - writeHoriz * 0.11F);
        model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.10F + writeHoriz * 0.04F);
        model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -0.78F);
        model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, -0.24F);
        model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0.12F);
        // Crossed regal seated legs under the floating desk
        model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -1.42F);
        model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, -1.24F);
        model.rightLeg.yRot = Mth.lerp(w, model.rightLeg.yRot, 0.34F);
        model.leftLeg.yRot = Mth.lerp(w, model.leftLeg.yRot, -0.20F);
        model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, -0.16F);
        model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, 0.08F);
        return true;
    }

    public static boolean setupFirstPersonQuillArmPose(
            net.minecraft.client.player.AbstractClientPlayer player,
            float partialTicks,
            net.minecraft.world.entity.HumanoidArm arm,
            PoseStack poseStack
    ) {
        if (player == null || !ClientAuraTracker.isTsaritsaIdle(player.getId())) return false;
        if (player.isUsingItem() || player.swinging) return false;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(player.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F) return false;
        float age = player.tickCount + partialTicks;
        float writeHoriz = Mth.sin(age * 0.28F);
        float writeDepth = Mth.cos(age * 0.16F);

        if (arm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
            // Right arm reaching forward and slightly down over desk, writing
            poseStack.translate(-0.16F * w + writeHoriz * 0.045F * w, -0.06F * w - writeDepth * 0.035F * w, -0.22F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(-42.0F * w - writeDepth * 6.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(28.0F * w - writeHoriz * 8.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(14.0F * w));
            return true;
        } else {
            // Left arm resting slightly forward on the desk
            poseStack.translate(0.12F * w, -0.04F * w, -0.16F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * w));
            return true;
        }
    }

    private static void drawIceSpire(
            VertexConsumer vc, Matrix4f mat,
            float baseRx, float baseRz, float midH, float tipH,
            int r0, int g0, int b0,
            int r1, int g1, int b1,
            int rTip, int gTip, int bTip,
            int alpha
    ) {
        float midRx = baseRx * 0.58F;
        float midRz = baseRz * 0.58F;
        // Bottom cap
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                -baseRx, 0.0F, -baseRz, r0, g0, b0, alpha,
                 baseRx, 0.0F, -baseRz, r0, g0, b0, alpha,
                 baseRx, 0.0F,  baseRz, r0, g0, b0, alpha,
                -baseRx, 0.0F,  baseRz, r0, g0, b0, alpha);
        // 4 tapered shaft faces (base -> mid)
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                -baseRx, 0.0F,  baseRz, r0, g0, b0, alpha,
                 baseRx, 0.0F,  baseRz, r0, g0, b0, alpha,
                 midRx,  midH,  midRz,  r1, g1, b1, alpha,
                -midRx,  midH,  midRz,  r1, g1, b1, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                 baseRx, 0.0F, -baseRz, r0, g0, b0, alpha,
                -baseRx, 0.0F, -baseRz, r0, g0, b0, alpha,
                -midRx,  midH, -midRz,  r1, g1, b1, alpha,
                 midRx,  midH, -midRz,  r1, g1, b1, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                -baseRx, 0.0F, -baseRz, r0, g0, b0, alpha,
                -baseRx, 0.0F,  baseRz, r0, g0, b0, alpha,
                -midRx,  midH,  midRz,  r1, g1, b1, alpha,
                -midRx,  midH, -midRz,  r1, g1, b1, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                 baseRx, 0.0F,  baseRz, r0, g0, b0, alpha,
                 baseRx, 0.0F, -baseRz, r0, g0, b0, alpha,
                 midRx,  midH, -midRz,  r1, g1, b1, alpha,
                 midRx,  midH,  midRz,  r1, g1, b1, alpha);
        // 4 sharp pyramid faces converging to needle tip (0, tipH, 0)
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                -midRx, midH,  midRz, r1, g1, b1, alpha,
                 midRx, midH,  midRz, r1, g1, b1, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                 midRx, midH, -midRz, r1, g1, b1, alpha,
                -midRx, midH, -midRz, r1, g1, b1, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                -midRx, midH, -midRz, r1, g1, b1, alpha,
                -midRx, midH,  midRz, r1, g1, b1, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat,
                 midRx, midH,  midRz, r1, g1, b1, alpha,
                 midRx, midH, -midRz, r1, g1, b1, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha,
                 0.0F,  tipH,  0.0F,  rTip, gTip, bTip, alpha);
    }

    private static void drawCrystalShard(
            VertexConsumer vc, Matrix4f mat,
            float rx, float rz, float halfH,
            int rMid, int gMid, int bMid,
            int rTip, int gTip, int bTip,
            int alpha
    ) {
        // Upper pyramid
        ClientAuraRenderer.emitCrystalQuad(vc, mat, -rx, 0.0F, rz, rMid, gMid, bMid, alpha, rx, 0.0F, rz, rMid, gMid, bMid, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, rx, 0.0F, -rz, rMid, gMid, bMid, alpha, -rx, 0.0F, -rz, rMid, gMid, bMid, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, -rx, 0.0F, -rz, rMid, gMid, bMid, alpha, -rx, 0.0F, rz, rMid, gMid, bMid, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, rx, 0.0F, rz, rMid, gMid, bMid, alpha, rx, 0.0F, -rz, rMid, gMid, bMid, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, halfH, 0.0F, rTip, gTip, bTip, alpha);
        // Lower inverted pyramid
        ClientAuraRenderer.emitCrystalQuad(vc, mat, rx, 0.0F, rz, rMid, gMid, bMid, alpha, -rx, 0.0F, rz, rMid, gMid, bMid, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, -rx, 0.0F, -rz, rMid, gMid, bMid, alpha, rx, 0.0F, -rz, rMid, gMid, bMid, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, -rx, 0.0F, rz, rMid, gMid, bMid, alpha, -rx, 0.0F, -rz, rMid, gMid, bMid, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha);
        ClientAuraRenderer.emitCrystalQuad(vc, mat, rx, 0.0F, -rz, rMid, gMid, bMid, alpha, rx, 0.0F, rz, rMid, gMid, bMid, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha, 0.0F, -halfH, 0.0F, rTip, gTip, bTip, alpha);
    }

    private static void drawPointedStar(
            VertexConsumer vc, Matrix4f mat,
            float armLen, float topLen, float thick, float depth,
            int rOut, int gOut, int bOut,
            int rCore, int gCore, int bCore,
            int alpha
    ) {
        // Vertical & horizontal needle rays (4-point star with elongated upper spire)
        float[][] rays = new float[][]{
            {0.0F, topLen, -thick, 0.0F, thick, 0.0F},
            {0.0F, -armLen, thick, 0.0F, -thick, 0.0F},
            {armLen, 0.0F, 0.0F, thick, 0.0F, -thick},
            {-armLen, 0.0F, 0.0F, -thick, 0.0F, thick}
        };
        for (float[] r : rays) {
            float tx = r[0], ty = r[1], ax = r[2], ay = r[3], bx = r[4], by = r[5];
            // Front & back diamond faces
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    ax, ay, 0.0F, rCore, gCore, bCore, alpha,
                    0.0F, 0.0F, depth, rCore, gCore, bCore, alpha,
                    bx, by, 0.0F, rCore, gCore, bCore, alpha,
                    tx, ty, 0.0F, rOut, gOut, bOut, alpha);
            ClientAuraRenderer.emitCrystalQuad(vc, mat,
                    bx, by, 0.0F, rCore, gCore, bCore, alpha,
                    0.0F, 0.0F, -depth, rCore, gCore, bCore, alpha,
                    ax, ay, 0.0F, rCore, gCore, bCore, alpha,
                    tx, ty, 0.0F, rOut, gOut, bOut, alpha);
        }
    }

    private static void renderFloatingDeskAndItems(
            PoseStack poseStack,
            MultiBufferSource buffer,
            float time,
            float deskBob,
            boolean firstPerson
    ) {
        VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
        poseStack.pushPose();
        if (!firstPerson) {
            poseStack.translate(0.0F, deskBob, 0.66F);
        } else {
            poseStack.translate(0.0F, deskBob, 0.0F);
        }
        Matrix4f dMat = poseStack.last().pose();
        // Sleek thin floating ice slab (only 0.032F thick!) + crisp bright-cyan outer rim
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, dMat, -0.76F, 0.836F, -0.26F, 0.76F, 0.864F, 0.26F, 72, 152, 224, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, dMat, -0.785F, 0.844F, -0.282F, 0.785F, 0.868F, 0.282F, 195, 244, 255, 255);

        // 4 sharp tiny crystal corner tips on the floating desk
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                poseStack.pushPose();
                poseStack.translate(sx * 0.785F, 0.856F, sz * 0.282F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-sx * 90.0F));
                drawIceSpire(solidVc, poseStack.last().pose(), 0.018F, 0.018F, 0.03F, 0.085F,
                        155, 228, 255, 210, 248, 255, 250, 255, 255, 255);
                poseStack.popPose();
            }
        }

        // Center: Open Royal Dossier / Book + Inkwell
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, dMat, -0.18F, 0.868F, -0.12F, 0.18F, 0.888F, 0.12F, 42, 68, 122, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, dMat, -0.165F, 0.888F, -0.105F, -0.01F, 0.911F, 0.105F, 244, 236, 218, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, dMat, 0.01F, 0.888F, -0.105F, 0.165F, 0.911F, 0.105F, 244, 236, 218, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, dMat, -0.27F, 0.868F, -0.05F, -0.21F, 0.928F, 0.01F, 32, 42, 72, 255);

        // Animated 3D Ice Quill writing smoothly across the open book
        float qx = -0.06F + Mth.sin(time * 0.28F) * 0.06F;
        float qz = -0.02F + Mth.cos(time * 0.16F) * 0.04F;
        poseStack.pushPose();
        poseStack.translate(qx, 0.914F, qz);
        poseStack.mulPose(Axis.ZP.rotationDegrees(26.0F + Mth.sin(time * 0.35F) * 5.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
        Matrix4f qMat = poseStack.last().pose();
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, qMat, -0.010F, 0.0F, -0.010F, 0.010F, 0.14F, 0.010F, 215, 248, 255, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, qMat, -0.022F, 0.07F, -0.005F, 0.022F, 0.19F, 0.005F, 155, 228, 255, 255);
        poseStack.popPose();

        // Left side of desk (+X): Neat stack of 4 books & parchment
        for (int b = 0; b < 4; b++) {
            float by = 0.868F + b * 0.048F;
            float rot = (b % 2 == 0) ? 4.0F : -6.0F;
            poseStack.pushPose();
            poseStack.translate(-0.46F, by, 0.02F);
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            Matrix4f bMat = poseStack.last().pose();
            int cr = (b % 2 == 0) ? 55 : 85;
            int cg = (b % 2 == 0) ? 88 : 148;
            int cb = (b % 2 == 0) ? 148 : 215;
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.13F, 0.0F, -0.10F, 0.13F, 0.036F, 0.10F, cr, cg, cb, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.115F, 0.036F, -0.088F, 0.115F, 0.048F, 0.088F, 242, 234, 216, 255);
            poseStack.popPose();
        }

        // Right side of desk (-X): Tall stack of 8 documents/tomes
        for (int b = 0; b < 8; b++) {
            float by = 0.868F + b * 0.046F;
            float rot = ((b * 11) % 17) - 8.0F;
            float ox = ((b % 3) - 1) * 0.012F;
            poseStack.pushPose();
            poseStack.translate(0.48F + ox, by, -0.02F);
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            Matrix4f bMat = poseStack.last().pose();
            boolean blueCover = (b % 3 == 0);
            if (blueCover) {
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.14F, 0.0F, -0.11F, 0.14F, 0.032F, 0.11F, 48, 82, 145, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.125F, 0.032F, -0.095F, 0.125F, 0.046F, 0.095F, 242, 234, 214, 255);
            } else {
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.135F, 0.0F, -0.105F, 0.135F, 0.042F, 0.105F, 238, 232, 216, 255);
                ZhongliVisualRenderer.drawJadeArmorBox(solidVc, bMat, -0.138F, 0.016F, -0.108F, 0.138F, 0.024F, 0.108F, 135, 198, 245, 255);
            }
            poseStack.popPose();
        }
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        ClientAuraRenderer.emitCrystalQuad(transVc, dMat,
                -0.68F, 0.832F, -0.22F, 145, 228, 255, 130,
                 0.68F, 0.832F, -0.22F, 145, 228, 255, 130,
                 0.68F, 0.832F,  0.22F, 145, 228, 255, 130,
                -0.68F, 0.832F,  0.22F, 145, 228, 255, 130);
        poseStack.popPose();
    }

    public static void renderIdleThrone(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        if (entity.isUsingItem() || entity.swinging) {
            ClientAuraTracker.AuraDisplayData dAct = ClientAuraTracker.get(id);
            if (dAct != null) {
                dAct.tsaritsaIdle = false;
                dAct.tsaritsaIdleFade = 0.0F;
                dAct.tsaritsaIdleYawSet = false;
            }
            return;
        }
        if (!ClientAuraTracker.isTsaritsaIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.01F) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        float deskBob = Mth.sin(time * 0.10F) * 0.014F;

        if (isFirstPersonSelf) {
            return;
        }

        float bodyYaw = (d != null && d.tsaritsaIdleYawSet) ? d.tsaritsaIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.translate(0.0F, 0.0F, -0.08F);
        poseStack.scale(fade, fade, fade);

        VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
        Matrix4f mat = poseStack.last().pose();

        // 1. Stepped Frosted Obsidian & Glacial-Blue Dais + Seat + Corner Ice Spikes
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.70F, 0.0F, -0.60F, 0.70F, 0.07F, 0.50F, 28, 56, 102, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.58F, 0.07F, -0.52F, 0.58F, 0.14F, 0.40F, 48, 104, 175, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.45F, 0.14F, -0.44F, 0.45F, 0.56F, 0.28F, 62, 138, 212, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.42F, 0.56F, -0.41F, 0.42F, 0.63F, 0.27F, 175, 236, 255, 255);
        // Front seat diamond frost crest
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.40F, 0.29F);
        drawPointedStar(solidVc, poseStack.last().pose(), 0.13F, 0.16F, 0.045F, 0.025F, 220, 248, 255, 135, 215, 255, 255);
        poseStack.popPose();
        // 4 Sharp corner crystal spikes on the dais
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                poseStack.pushPose();
                poseStack.translate(sx * 0.64F, 0.06F, sz < 0 ? -0.54F : 0.44F);
                poseStack.mulPose(Axis.XP.rotationDegrees(sz < 0 ? -18.0F : 16.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-sx * 20.0F));
                drawIceSpire(solidVc, poseStack.last().pose(), 0.065F, 0.065F, 0.14F, 0.34F, 48, 112, 188, 135, 218, 255, 240, 253, 255, 255);
                poseStack.popPose();
            }
        }

        // 2. Spiky Armrests with Sharp Pointed Crystal Claws & Downward Icicles
        for (int side : new int[]{-1, 1}) {
            float ax0 = side < 0 ? -0.55F : 0.39F;
            float ax1 = side < 0 ? -0.39F : 0.55F;
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, ax0, 0.14F, -0.40F, ax1, 0.86F, 0.30F, 46, 108, 182, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, ax0 - side * 0.02F, 0.83F, -0.38F, ax1 - side * 0.02F, 0.89F, 0.34F, 175, 236, 255, 255);
            // 3 sharp pointed crystal claws per armrest
            float[][] claws = new float[][]{
                {side * 0.47F, 0.88F, 0.32F, 34.0F, -side * 12.0F, 0.055F, 0.36F},
                {side * 0.53F, 0.82F, 0.14F, 18.0F, -side * 32.0F, 0.050F, 0.32F},
                {side * 0.55F, 0.68F, -0.06F, 8.0F, -side * 46.0F, 0.045F, 0.28F}
            };
            for (float[] cl : claws) {
                poseStack.pushPose();
                poseStack.translate(cl[0], cl[1], cl[2]);
                poseStack.mulPose(Axis.XP.rotationDegrees(cl[3]));
                poseStack.mulPose(Axis.ZP.rotationDegrees(cl[4]));
                drawIceSpire(solidVc, poseStack.last().pose(), cl[5], cl[5], cl[6] * 0.45F, cl[6],
                        72, 162, 232, 160, 230, 255, 245, 254, 255, 255);
                poseStack.popPose();
            }
            // Downward icicle spike under front armrest corner
            poseStack.pushPose();
            poseStack.translate(side * 0.48F, 0.82F, 0.32F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            drawIceSpire(solidVc, poseStack.last().pose(), 0.042F, 0.042F, 0.10F, 0.26F,
                    65, 148, 222, 155, 225, 255, 235, 252, 255, 255);
            poseStack.popPose();
        }

        // 3. Tall Gothic Pointed Cryo Backrest (Sharp Faceted Spires + Translucent Arch + Lattice + Apex Star)
        // Lower backrest frame
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.48F, 0.56F, -0.46F, 0.48F, 1.38F, -0.35F, 44, 102, 174, 255);
        // Translucent glacial inner pane for the central gothic arch
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        ClientAuraRenderer.emitCrystalQuad(transVc, mat,
                -0.34F, 0.64F, -0.345F, 130, 215, 255, 155,
                 0.34F, 0.64F, -0.345F, 130, 215, 255, 155,
                 0.26F, 1.56F, -0.345F, 185, 238, 255, 175,
                -0.26F, 1.56F, -0.345F, 185, 238, 255, 175);
        ClientAuraRenderer.emitCrystalQuad(transVc, mat,
                -0.26F, 1.56F, -0.345F, 185, 238, 255, 175,
                 0.26F, 1.56F, -0.345F, 185, 238, 255, 175,
                 0.0F,  2.54F, -0.345F, 240, 253, 255, 210,
                 0.0F,  2.54F, -0.345F, 240, 253, 255, 210);
        solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
        // Frost filigree lattice ribs inside the central arch
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.25F, 1.48F, -0.34F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 16.0F));
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, poseStack.last().pose(), -0.025F, 0.0F, -0.025F, 0.025F, 1.06F, 0.025F, 195, 244, 255, 255);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0.0F, 1.15F, -0.34F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 34.0F));
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, poseStack.last().pose(), -0.018F, -0.36F, -0.018F, 0.018F, 0.36F, 0.018F, 175, 236, 255, 255);
            poseStack.popPose();
        }

        // Tall central needle spire at the gothic apex
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.38F, -0.40F);
        drawIceSpire(solidVc, poseStack.last().pose(), 0.15F, 0.095F, 0.56F, 1.30F,
                58, 138, 218, 150, 226, 255, 245, 254, 255, 255);
        poseStack.popPose();

        // 2 Symmetrical flanking sharp-pointed crystal spires (forming 3-spire silhouette: 1 center + 2 side spires)
        float[][] spires = new float[][]{
            {-0.40F, 1.18F, -0.39F, -4.0F,  6.0F, 0.115F, 1.08F},
            { 0.40F, 1.18F, -0.39F, -4.0F, -6.0F, 0.115F, 1.08F}
        };
        for (float[] sp : spires) {
            poseStack.pushPose();
            poseStack.translate(sp[0], sp[1], sp[2]);
            poseStack.mulPose(Axis.XP.rotationDegrees(sp[3]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sp[4]));
            drawIceSpire(solidVc, poseStack.last().pose(), sp[5], sp[5] * 0.85F, sp[6] * 0.44F, sp[6],
                    54, 132, 212, 148, 224, 255, 242, 253, 255, 255);
            poseStack.popPose();
        }

        // Rear-facing jagged crystal buttress spines (detailed back view)
        float[][] rearSpines = new float[][]{
            { 0.0F,  0.96F, -0.45F, -48.0F,  0.0F, 0.075F, 0.46F},
            {-0.26F, 0.84F, -0.44F, -42.0F, 18.0F, 0.060F, 0.38F},
            { 0.26F, 0.84F, -0.44F, -42.0F,-18.0F, 0.060F, 0.38F},
            { 0.0F,  0.48F, -0.44F, -62.0F,  0.0F, 0.065F, 0.34F}
        };
        for (float[] rs : rearSpines) {
            poseStack.pushPose();
            poseStack.translate(rs[0], rs[1], rs[2]);
            poseStack.mulPose(Axis.XP.rotationDegrees(rs[3]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rs[4]));
            drawIceSpire(solidVc, poseStack.last().pose(), rs[5], rs[5], rs[6] * 0.42F, rs[6],
                    42, 98, 172, 125, 208, 252, 232, 250, 255, 255);
            poseStack.popPose();
        }

        // Glowing Pointed 8-Ray Cryo Star / Snowflake at Throne Apex
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.74F, -0.40F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.08F) * 6.0F));
        Matrix4f starMat = poseStack.last().pose();
        drawPointedStar(solidVc, starMat, 0.20F, 0.28F, 0.048F, 0.042F, 235, 252, 255, 165, 232, 255, 255);
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        drawPointedStar(solidVc, poseStack.last().pose(), 0.13F, 0.13F, 0.032F, 0.032F, 200, 245, 255, 255, 255, 255, 255);
        poseStack.popPose();

        // 4. Thin Floating Royal Ice Desk (NO support legs underneath, gently levitating!)
        renderFloatingDeskAndItems(poseStack, buffer, time, deskBob, false);

        // 5. Orbiting Pointed Cryo Crystal Shards around the throne
        solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
        for (int i = 0; i < 4; i++) {
            float ang = (float) (time * 0.06D + i * Math.PI * 0.5D);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(ang) * 0.90F, 1.65F + Mth.sin(time * 0.12F + i) * 0.08F, -0.55F + Mth.sin(ang) * 0.18F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.0F + i * 45.0F));
            drawCrystalShard(solidVc, poseStack.last().pose(), 0.048F, 0.048F, 0.14F, 135, 222, 255, 245, 254, 255, 255);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
