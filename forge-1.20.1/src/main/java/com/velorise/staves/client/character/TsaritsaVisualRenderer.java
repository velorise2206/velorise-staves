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
                model.body.yRot = castDir * Mth.lerp(slash, -0.32F, 0.38F) * w;
                model.body.xRot = 0.08F * w;

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
                model.body.yRot = castDir * Mth.lerp(slash, 0.35F, -0.35F) * w;
                model.body.xRot = Mth.lerp(slash, 0.06F, -0.10F) * w;

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
                float bodyX = (t < 0.32F) ? Mth.lerp(lift, 0.0F, -0.12F) : Mth.lerp(slam, -0.12F, 0.34F);

                model.body.xRot = Mth.lerp(w, model.body.xRot, bodyX);
                model.body.yRot = castDir * (t < 0.32F ? -0.15F * lift : 0.22F * slam) * w;

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

        if (!crown && !supportBurst && !dpsSeal && activeAnim != ClientAuraTracker.ANIM_TSARITSA_BURST_DPS) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        if (isFirstPersonSelf) return;

        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer. getTranslucentNoDepthWrite());

        if (crown) {
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
                WindGliderRenderer.emitDoubleQuad(vc, mat, x0, 0.0F, z0, x1, 0.0F, z1, x1, baseH, z1, x0, baseH, z0, br, bg, bb, 235);
                WindGliderRenderer.emitDoubleQuad(vc, mat, x0, baseH, z0, x1, baseH, z1, mx, peakH, mz, mx, peakH, mz, pr, pg, pb, 245);
                WindGliderRenderer.renderCloverCrossXZ(vc, mat, mx, peakH + 0.04F, mz, 0.08F, 245, 255, 255, 250);
            }
            poseStack.popPose();
        }

        if (supportBurst) {
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.68F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, 0.18F);
            Matrix4f mat = poseStack.last().pose();
            float flap = Mth.sin(time * 0.25F) * 0.12F;
            for (int side = -1; side <= 1; side += 2) {
                for (int b = 0; b < 3; b++) {
                    float yOff = 0.25F - b * 0.22F;
                    float xOut = side * (0.95F - b * 0.14F);
                    float zOut = 0.28F + flap * side;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            side * 0.10F, yOff, 0.0F,
                            xOut, yOff + 0.28F, zOut,
                            xOut * 0.88F, yOff - 0.08F, zOut,
                            side * 0.10F, yOff - 0.12F, 0.0F,
                            145, 225, 255, 205);
                }
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

    public static void renderWorldArcs(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ResourceLocation cryoArrowTex = new ResourceLocation("velorise_staves", "textures/entity/projectiles/cryo_arrow.png");

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
}
