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
    private static final java.util.Map<Integer, float[]> TSARITSA_FP_CAM_SMOOTH = new java.util.concurrent.ConcurrentHashMap<>();
    private static final ResourceLocation TSARITSA_THRONE_TEXTURE =
            new ResourceLocation("velorise_staves", "textures/entity/tsaritsa_ice_throne.png");
    private static com.velorise.staves.client.model.TsaritsaIceThroneModel<net.minecraft.world.entity.Entity> throneModel;

    private static com.velorise.staves.client.model.TsaritsaIceThroneModel<net.minecraft.world.entity.Entity> getOrBakeThroneModel() {
        if (throneModel == null) {
            throneModel = new com.velorise.staves.client.model.TsaritsaIceThroneModel<>(
                    com.velorise.staves.client.model.TsaritsaIceThroneModel.createBodyLayer().bakeRoot()
            );
        }
        return throneModel;
    }

    public static void clearClientCaches() {
        TSARITSA_FP_CAM_SMOOTH.clear();
    }

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

    private static float stepSpearStanceFade(LivingEntity entity, boolean active) {
        if (entity == null) return active ? 1.0F : 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        if (d == null) return active ? 1.0F : 0.0F;
        long nowMs = System.currentTimeMillis();
        if (d.tsaritsaSpearLastMs <= 0L) {
            d.tsaritsaSpearLastMs = nowMs;
            d.tsaritsaSpearFade = active ? 1.0F : 0.0F;
        } else if (nowMs > d.tsaritsaSpearLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.tsaritsaSpearLastMs) / 1000.0F);
            d.tsaritsaSpearLastMs = nowMs;
            if (active) {
                d.tsaritsaSpearFade = Math.min(1.0F, d.tsaritsaSpearFade + dt / 0.22F);
            } else {
                d.tsaritsaSpearFade = Math.max(0.0F, d.tsaritsaSpearFade - dt / 0.35F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.tsaritsaSpearFade);
    }

    public static boolean applySpearStanceGlidePose(LivingEntity entity, HumanoidModel<?> model, int activeAnim) {
        if (entity == null || model == null) return false;
        boolean spearStance = ClientAuraTracker.isTsaritsaSpearStance(entity.getId())
                || ArcaneStaffItem.getEffectiveKind(entity.getMainHandItem()) == ArcaneStaffItem.Kind.TSARITSA_SPEAR
                || (!ArcaneStaffItem.isGenshinCharacterWeapon(entity.getMainHandItem())
                && ArcaneStaffItem.getEffectiveKind(entity.getOffhandItem()) == ArcaneStaffItem.Kind.TSARITSA_SPEAR);
        float wSpear = stepSpearStanceFade(entity, spearStance);
        if (wSpear <= 0.001F) return false;
        if (activeAnim >= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1 && activeAnim <= ClientAuraTracker.ANIM_TSARITSA_UNSEAL) return false;
        if (entity.isUsingItem() || entity.swinging || entity.getVehicle() != null) return false;

        HumanoidArm spearArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.TSARITSA_SPEAR);
        ModelPart mainArm = (spearArm == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        float castDir = (spearArm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;

        // Smoothly lerp reverse-grip spear behind back when entering/exiting spear stance
        float targetX = entity.isSprinting() ? 0.95F : 0.85F;
        mainArm.xRot = Mth.lerp(wSpear, mainArm.xRot, targetX);
        mainArm.yRot = Mth.lerp(wSpear, mainArm.yRot, 0.28F * castDir);
        mainArm.zRot = Mth.lerp(wSpear, mainArm.zRot, 0.26F * castDir);

        // Lean torso forward by 10 degrees (~0.175 rad) when sprinting with the spear
        if (entity.isSprinting()) {
            model.body.xRot = Mth.lerp(wSpear, model.body.xRot, 0.175F);
        }

        // Allow normal walking legs/arms while keeping weapon arm angled back with reverse-grip spear
        return false;
    }

    public static boolean applyThirdPersonItemTransform(
            LivingEntity entity,
            ArcaneStaffItem.Kind effKind,
            boolean leftHand,
            PoseStack poseStack,
            int activeAnim
    ) {
        boolean isBowOrSpear = (effKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR || effKind == ArcaneStaffItem.Kind.TSARITSA_BOW);
        if (!isBowOrSpear) return false;
        boolean spearStance = (effKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR)
                || ClientAuraTracker.isTsaritsaSpearStance(entity.getId());
        ClientAuraTracker.AuraDisplayData dFade = ClientAuraTracker.get(entity.getId());
        float wSpear = spearStance ? 1.0F : (dFade != null ? WindGliderRenderer.smoothStep01(dFade.tsaritsaSpearFade) : 0.0F);
        if (wSpear <= 0.001F) return false;
        if (activeAnim >= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1 && activeAnim <= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_3) {
            ClientAuraTracker.AuraDisplayData dItem = ClientAuraTracker.get(entity.getId());
            float wItem = 1.0F;
            if (dItem != null) {
                long nowMs = System.currentTimeMillis();
                float totalMs = Math.max(1.0F, (float) (dItem.staffAnimUntil - dItem.staffAnimStartTime));
                float tItem = Mth.clamp((nowMs - dItem.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                wItem = (tItem < 0.18F) ? WindGliderRenderer.smoothStep01(tItem / 0.18F)
                        : (tItem < 0.68F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((tItem - 0.68F) / 0.32F));
            }
            // Smoothly transition spear item between reverse-grip behind back and forward slash arc
            poseStack.translate(0.0F, Mth.lerp(wItem, -0.04F, 0.06F), Mth.lerp(wItem, 0.09F, -0.10F));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(wItem, -80.0F, -78.0F)));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_DPS) {
            ClientAuraTracker.AuraDisplayData dBurst = ClientAuraTracker.get(entity.getId());
            float tB = 0.0F;
            if (dBurst != null) {
                long nowMs = System.currentTimeMillis();
                float totalMs = Math.max(1.0F, (float) (dBurst.staffAnimUntil - dBurst.staffAnimStartTime));
                tB = Mth.clamp((nowMs - dBurst.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            }
            if (tB > 0.74F) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
            // Exact vanilla Trident 3rd-person overhead horizontal aim & throw orientation
            float hurl = (tB < 0.52F) ? 0.0F : WindGliderRenderer.smoothStep01((tB - 0.52F) / 0.20F);
            poseStack.translate(0.0F, 0.02F, -0.04F);
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(hurl, 190.0F, 115.0F)));
            return true;
        }
        if (activeAnim > ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_3 && activeAnim <= ClientAuraTracker.ANIM_TSARITSA_UNSEAL) return false;
        if (entity.isUsingItem() || entity.swinging) return false;
        // Align reverse-grip spear collinearly with the arm axis, smoothly lerping back to bow
        poseStack.translate(0.0F, -0.04F * wSpear, 0.09F * wSpear);
        poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F * wSpear));
        return true;
    }

    public static float getBurstDpsLevitationOffsetY(net.minecraft.world.entity.Entity entity, float partialTick) {
        if (entity == null) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        long now = System.currentTimeMillis();
        if (d == null || now >= d.staffAnimUntil || d.staffAnimType != ClientAuraTracker.ANIM_TSARITSA_BURST_DPS) {
            return 0.0F;
        }
        float totalMs = Math.max(1.0F, (float) (d.staffAnimUntil - d.staffAnimStartTime));
        float t = Mth.clamp((now - d.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
        float rise = (t < 0.25F) ? WindGliderRenderer.smoothStep01(t / 0.25F)
                : (t < 0.76F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.76F) / 0.24F));
        return 0.68F * rise;
    }

    public static boolean applyActionHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model, int activeAnim, float t) {
        if (activeAnim >= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1 && activeAnim <= ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_3) {
            HumanoidArm castArm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.TSARITSA_SPEAR);
            ModelPart mainPart = castArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            ModelPart offPart = castArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;
            // Start from reverse-grip spear stance behind back so NA transitions smoothly in and out
            mainPart.xRot = 0.85F;
            mainPart.yRot = 0.28F * castDir;
            mainPart.zRot = 0.26F * castDir;
            float w = (t < 0.18F) ? WindGliderRenderer.smoothStep01(t / 0.18F)
                    : (t < 0.68F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.68F) / 0.32F));

            if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_1) {
                float slash = WindGliderRenderer.smoothStep01(t / 0.55F);

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, -1.38F);
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, -0.92F, 0.85F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, -0.36F, 0.42F));

                offPart.xRot = 0.0F;
                offPart.yRot = 0.0F;
                offPart.zRot = 0.0F;

                model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.28F);
                model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.22F);
                model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.08F);
                model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.08F);
                model.head.xRot += 0.06F * w;
                model.hat.copyFrom(model.head);
            } else if (activeAnim == ClientAuraTracker.ANIM_TSARITSA_SPEAR_NA_2) {
                float slash = WindGliderRenderer.smoothStep01(t / 0.55F);

                mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -0.65F, -2.15F));
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, 0.88F, -0.72F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, 0.42F, -0.38F));

                offPart.xRot = 0.0F;
                offPart.yRot = 0.0F;
                offPart.zRot = 0.0F;

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

                offPart.xRot = 0.0F;
                offPart.yRot = 0.0F;
                offPart.zRot = 0.0F;

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
            float liftProg = (t < 0.25F) ? WindGliderRenderer.smoothStep01(t / 0.25F) : (t < 0.76F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.76F) / 0.24F));
            float liftY = -0.68F * 16.0F * liftProg;
            model.head.y += liftY;
            model.hat.y += liftY;
            model.body.y += liftY;
            model.rightArm.y += liftY;
            model.leftArm.y += liftY;
            model.rightLeg.y += liftY;
            model.leftLeg.y += liftY;

            // Graceful mid-air jump/hover leg pose
            model.leftLeg.xRot = Mth.lerp(liftProg, model.leftLeg.xRot, 0.28F);
            model.rightLeg.xRot = Mth.lerp(liftProg, model.rightLeg.xRot, -0.42F);
            model.leftLeg.zRot = Mth.lerp(liftProg, model.leftLeg.zRot, -0.08F);
            model.rightLeg.zRot = Mth.lerp(liftProg, model.rightLeg.zRot, 0.08F);

            if (t < 0.52F) {
                float prep = WindGliderRenderer.smoothStep01(t / 0.16F);
                model.body.yRot = 0.14F * prep * w;
                // Vanilla Trident overhead charge arm pose (arm straight up -PI)
                model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -3.14F);
                model.rightArm.yRot = 0.0F;
                model.rightArm.zRot = 0.0F;
            } else {
                float hurl = WindGliderRenderer.smoothStep01(Math.min(1.0F, (t - 0.52F) / 0.22F));
                model.body.yRot = Mth.lerp(hurl, 0.14F, -0.28F) * w;
                model.body.xRot = 0.22F * hurl * w;
                model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, Mth.lerp(hurl, -3.14F, -0.45F));
                model.rightArm.yRot = 0.0F;
                model.rightArm.zRot = 0.0F;
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
            if (t > 0.74F) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
            if (t < 0.52F) {
                float prep = WindGliderRenderer.smoothStep01(t / 0.16F);
                float charge = WindGliderRenderer.smoothStep01(t / 0.52F);
                // Exact vanilla Trident 1st-person charge pose
                poseStack.translate(dir * (-0.24F * prep), 0.36F * prep + 0.12F * charge, 0.10F * prep);
                poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F * prep));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 35.3F * prep));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * -9.785F * prep));
            } else {
                float hurl = WindGliderRenderer.smoothStep01((t - 0.52F) / 0.20F);
                poseStack.translate(dir * Mth.lerp(hurl, -0.24F, -0.08F), Mth.lerp(hurl, 0.48F, -0.45F), Mth.lerp(hurl, 0.10F, -0.88F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(hurl, -55.0F, -96.0F)));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(hurl, 35.3F, 6.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * -9.785F));
            }
            return true;
        }
        // In spear stance when not actively slashing/bursting, hide the 1st-person spear since it is held behind her back
        poseStack.scale(0.0F, 0.0F, 0.0F);
        return true;
    }

    public static void renderEntityEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        long now = System.currentTimeMillis();
        int activeAnim = (d != null && now < d.staffAnimUntil) ? d.staffAnimType : 0;
        boolean skillCrown = ClientAuraTracker.isTsaritsaCrownActive(id) && !ClientAuraTracker.isTsaritsaSpearStance(id);
        boolean idleCrown = ClientAuraTracker.isTsaritsaIdle(id) && !entity.isUsingItem() && !entity.swinging;
        boolean crown = skillCrown || idleCrown;
        boolean supportBurst = ClientAuraTracker.isTsaritsaSupportBurst(id) || activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_SUPPORT;
        boolean dpsSeal = ClientAuraTracker.isTsaritsaDpsSeal(id);

        renderIdleThrone(entity, poseStack, buffer);
        if (!crown && !supportBurst && !dpsSeal && activeAnim != ClientAuraTracker.ANIM_TSARITSA_BURST_DPS) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        if (isFirstPersonSelf) return;

        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        if (d != null && !d.tsaritsaIdleYawSet) {
            d.tsaritsaIdleYaw = entity.yBodyRot;
            d.tsaritsaIdleYawSet = true;
        }
        float bodyYaw = (d != null && d.tsaritsaIdleYawSet) ? d.tsaritsaIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer. getTranslucentNoDepthWrite());

        float idleLift = ClientAuraRenderer.getTotalLevitationOffsetY(entity, pt);

        if (crown) {
            VertexConsumer crownVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
            poseStack.pushPose();
            float crownScale = (!skillCrown && idleCrown) ? stepIdleFade(d, now) : 1.0F;
            float sitOffset = (!skillCrown && idleCrown) ? (-0.1875F * crownScale) : 0.0F;
            float bob = Mth.sin(time * 0.15F) * 0.05F;
            poseStack.translate(0.0F, entity.getBbHeight() + 0.42F + idleLift + sitOffset + bob, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.2F));
            poseStack.scale(crownScale, crownScale, crownScale);
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





        if (dpsSeal) {
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.55F + idleLift, 0.0F);
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
        if (!ClientAuraTracker.isTsaritsaIdle(mc.player.getId())) {
            TSARITSA_FP_CAM_SMOOTH.remove(mc.player.getId());
            return;
        }
        ClientAuraTracker.AuraDisplayData d1p = ClientAuraTracker.get(mc.player.getId());
        float fade1p = stepIdleFade(d1p, System.currentTimeMillis());
        if (fade1p <= 0.01F) return;
        float pt1p = mc.getFrameTime();
        float time1p = mc.player.tickCount + pt1p;
        float deskBob1p = Mth.sin(time1p * 0.10F) * 0.014F;
        if (d1p != null && !d1p.tsaritsaIdleYawSet) {
            d1p.tsaritsaIdleYaw = mc.player.getViewYRot(pt1p);
            d1p.tsaritsaIdleYawSet = true;
        }
        float bodyYaw1p = mc.player.getViewYRot(pt1p);
        Vec3 pPos = mc.player.getPosition(pt1p).subtract(camPos);
        poseStack.pushPose();
        poseStack.translate(pPos.x, pPos.y, pPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw1p));
        poseStack.translate(0.0F, 0.0F, -0.08F);
        poseStack.scale(fade1p, fade1p, fade1p);
        float writeTime1p = (d1p != null && d1p.tsaritsaIdleStartMs > 0L) ? ((System.currentTimeMillis() - d1p.tsaritsaIdleStartMs) * 0.012F) : 0.0F;
        renderFloatingDeskAndItems(poseStack, bufferSource, writeTime1p, deskBob1p, false);
        poseStack.popPose();
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

    private static float stepIdleFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.tsaritsaIdleLastMs <= 0L) {
            d.tsaritsaIdleLastMs = nowMs;
            if (d.tsaritsaIdle) {
                d.tsaritsaIdleFade = Math.min(1.0F, d.tsaritsaIdleFade + 0.016F / 0.90F);
            }
        } else if (nowMs > d.tsaritsaIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.tsaritsaIdleLastMs) / 1000.0F);
            d.tsaritsaIdleLastMs = nowMs;
            if (d.tsaritsaIdle) {
                d.tsaritsaIdleFade = Math.min(1.0F, d.tsaritsaIdleFade + dt / 0.90F);
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
        // Synced 8.25s 2-page writing, page-turn (left hand), & inkwell-dip (right hand) cycle
        float writeTicks3p = (d != null && d.tsaritsaIdleStartMs > 0L) ? ((System.currentTimeMillis() - d.tsaritsaIdleStartMs) * 0.012F) : 0.0F;
        float[] ws3p = getTsaritsaWriteState(writeTicks3p);
        float handX3p = -ws3p[0] / 0.145F;
        float handZ3p = -ws3p[1] / 0.065F;
        float flipLift3p = ws3p[2];
        float cycleSec3p = ws3p[4];
        float pageFlipProg3p = (cycleSec3p >= 6.4F && cycleSec3p < 7.5F)
                ? WindGliderRenderer.smoothStep01((cycleSec3p - 6.4F) / 1.1F) : 0.0F;
        float pageFlipArc3p = Mth.sin(pageFlipProg3p * (float) Math.PI);
        float inkDip3p = (cycleSec3p >= 6.4F && cycleSec3p < 7.5F)
                ? Math.max(0.0F, Mth.sin(pageFlipProg3p * (float) Math.PI)) : 0.0F;
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.135F - handZ3p * 0.065F - flipLift3p * 0.14F + inkDip3p * 0.14F);
        model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.36F + handX3p * 0.14F);
        model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.10F - handX3p * 0.05F - flipLift3p * 0.12F);
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
        if (d != null && !d.tsaritsaIdleYawSet) {
            d.tsaritsaIdleYaw = player.getViewYRot(partialTicks);
            d.tsaritsaIdleYawSet = true;
        }
        float targetPitch = player.getViewXRot(partialTicks);
        float camYaw = player.getViewYRot(partialTicks);
        float deskYaw = camYaw;
        float targetRelYaw = Mth.wrapDegrees(camYaw - deskYaw);

        poseStack.mulPose(Axis.XP.rotationDegrees(targetPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(targetRelYaw));
        float writeTicks1p = (d != null && d.tsaritsaIdleStartMs > 0L) ? ((System.currentTimeMillis() - d.tsaritsaIdleStartMs) * 0.012F) : 0.0F;
        float[] ws1p = getTsaritsaWriteState(writeTicks1p);
        float handX1p = -ws1p[0] / 0.145F; // -1 (left page) .. +1 (right page) .. +1.65 (inkwell)
        float handZ1p = -ws1p[1] / 0.065F; // -1 (top) .. +1 (bottom)
        float flipLift1p = ws1p[2];
        float cycleSec1p = ws1p[4];
        float pageFlipProg1p = (cycleSec1p >= 6.4F && cycleSec1p < 7.5F)
                ? WindGliderRenderer.smoothStep01((cycleSec1p - 6.4F) / 1.1F) : 0.0F;
        float pageFlipArc1p = Mth.sin(pageFlipProg1p * (float) Math.PI);
        float inkDip1p = (cycleSec1p >= 6.4F && cycleSec1p < 7.5F)
                ? Math.max(0.0F, Mth.sin(pageFlipProg1p * (float) Math.PI)) : 0.0F;

        if (arm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
            // Right arm lowered another 1/3 onto desk & dips quill into inkwell during page flip
            poseStack.translate(-0.14F * w + handX1p * 0.075F * w, -0.49F * w - handZ1p * 0.030F * w + flipLift1p * 0.05F * w - inkDip1p * 0.06F * w, -0.22F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(-34.0F * w - handZ1p * 5.0F * w - flipLift1p * 6.0F * w - inkDip1p * 12.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(18.0F * w - handX1p * 10.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(22.0F * w + flipLift1p * 8.0F * w));
            return true;
        } else {
            // Left arm shifted outward so it does not cover the book, resting calmly while book auto-flips
            poseStack.translate(-0.22F * w, -1.26F * w, -0.08F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(-32.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(-16.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * w));
            return true;
        }
    }

    private static float[] getTsaritsaWriteState(float timeTicks) {
        // 8.25s (165 ticks) cycle:
        // 0.00..5.60s: write lines 0..3 on left page (+X) and lines 4..6 on right page (-X)
        // 5.60..6.40s: draw a heart at the bottom of page 2 (-X)
        // 6.40..7.50s: two-sided page flip from right (-X) to left (+X), revealing clean blank pages
        // 7.50..8.25s: settle quill smoothly onto top-left of fresh blank page
        int cycleIdx = (int) (timeTicks / 165.0F);
        float shapeFlag = (Math.abs(Integer.hashCode(cycleIdx)) % 2 == 1) ? 1.0F : 0.0F;
        float cycleSec = (timeTicks % 165.0F) / 20.0F;
        if (cycleSec < 5.6F) {
            float u = cycleSec / 5.6F;
            float varProg = Mth.clamp(u + 0.025F * Mth.sin(u * (float) Math.PI * 6.0F), 0.0F, 1.0F);
            float totalLines = varProg * 7.0F;
            int lineIdx = Math.min(6, (int) totalLines);
            float inLine = totalLines - lineIdx;
            float xStart = (lineIdx < 4) ? 0.142F : -0.028F;
            float xEnd = (lineIdx < 4) ? 0.030F : -0.142F;
            float zCur = 0.062F - (lineIdx % 4) * 0.042F;
            float qx, qz, lift = 0.0F;
            if (inLine <= 0.84F) {
                float writeT = inLine / 0.84F;
                qx = Mth.lerp(writeT, xStart, xEnd);
                qz = zCur + Mth.sin(timeTicks * 0.45F) * 0.003F;
            } else {
                float retT = WindGliderRenderer.smoothStep01((inLine - 0.84F) / 0.16F);
                int nextIdx = lineIdx + 1;
                float nextX = (nextIdx < 4) ? 0.142F : (nextIdx < 7 ? -0.028F : -0.085F);
                float nextZ = (nextIdx < 7) ? (0.062F - (nextIdx % 4) * 0.042F) : -0.060F;
                qx = Mth.lerp(retT, xEnd, nextX);
                qz = Mth.lerp(retT, zCur, nextZ);
                lift = 0.25F * Mth.sin(retT * (float) Math.PI);
            }
            return new float[]{qx, qz, lift, varProg, cycleSec, shapeFlag};
        } else if (cycleSec < 6.4F) {
            // Draw either a Heart or a 5-pointed Star at bottom center of page 2 (-0.085F, -0.065F)
            float hp = Mth.clamp((cycleSec - 5.6F) / 0.8F, 0.0F, 1.0F);
            if (shapeFlag > 0.5F) {
                float[] sp = evalStarPoint(hp);
                return new float[]{-0.085F + sp[0], -0.065F + sp[1], 0.0F, 1.0F + hp, cycleSec, shapeFlag};
            }
            double theta = hp * Math.PI * 2.0D;
            float hx = (float) (16.0D * Math.pow(Math.sin(theta), 3)) * 0.0018F;
            float hz = (float) (13.0D * Math.cos(theta) - 5.0D * Math.cos(2.0D * theta) - 2.0D * Math.cos(3.0D * theta) - Math.cos(4.0D * theta)) * 0.0016F;
            return new float[]{-0.085F - hx, -0.065F + hz, 0.0F, 1.0F + hp, cycleSec, shapeFlag};
        } else if (cycleSec < 7.5F) {
            // Left hand flips page while right hand dips quill into the inkwell (-0.24F, -0.02F)
            float fp = WindGliderRenderer.smoothStep01((cycleSec - 6.4F) / 1.1F);
            float flipLift = (fp < 0.35F) ? Mth.sin((fp / 0.35F) * (float) Math.PI) * 0.7F
                    : -Mth.sin(((fp - 0.35F) / 0.65F) * (float) Math.PI) * 0.45F;
            float moveT = WindGliderRenderer.smoothStep01(fp / 0.42F);
            float qx = Mth.lerp(moveT, -0.085F, -0.24F);
            float qz = Mth.lerp(moveT, -0.065F, -0.02F);
            return new float[]{qx, qz, flipLift, 2.0F, cycleSec, shapeFlag};
        } else {
            // Lift quill from inkwell and settle onto top-left of fresh blank page
            float settle = WindGliderRenderer.smoothStep01((cycleSec - 7.5F) / 0.75F);
            float lift = Mth.sin(settle * (float) Math.PI) * 0.85F;
            float qx = Mth.lerp(settle, -0.24F, 0.142F);
            float qz = Mth.lerp(settle, -0.02F, 0.062F);
            return new float[]{qx, qz, lift, 0.0F, cycleSec, shapeFlag};
        }
    }

    private static void drawSquigglyCyrillicLine(
            VertexConsumer solidVc, Matrix4f mat,
            float xStart, float xFullEnd, float zBase,
            float y0, float y1, float lineFrac, int lineIdx
    ) {
        float[] lenScale = new float[]{ 1.00F, 0.72F, 0.94F, 0.58F, 0.96F, 0.68F, 0.85F };
        float scale = lenScale[Math.floorMod(lineIdx, lenScale.length)];
        float targetEnd = Mth.lerp(scale, xStart, xFullEnd);
        int totalSegs = 28;
        int drawnSegs = Math.max(1, Math.round(totalSegs * Mth.clamp(lineFrac, 0.0F, 1.0F)));
        float yTop = y1 + 0.0004F;
        float hw = 0.0032F;
        for (int s = 0; s < drawnSegs; s++) {
            // 1-2 clean word-space breaks per line while keeping each word completely continuous
            if ((s == 11 && lineIdx % 2 == 0) || (s == 17 && lineIdx % 2 == 1) || (s == 21 && lineIdx % 3 == 0)) {
                continue;
            }
            float f0 = s / (float) totalSegs;
            float f1 = (s + 1) / (float) totalSegs;
            float sx0 = Mth.lerp(f0, xStart, targetEnd);
            float sx1 = Mth.lerp(f1, xStart, targetEnd);
            float w0 = Mth.sin(s * 1.15F + lineIdx * 1.9F) * 0.0055F + Mth.cos(s * 2.1F + lineIdx) * 0.0022F;
            float w1 = Mth.sin((s + 1) * 1.15F + lineIdx * 1.9F) * 0.0055F + Mth.cos((s + 1) * 2.1F + lineIdx) * 0.0022F;
            float z0 = zBase + 0.004F + w0;
            float z1 = zBase + 0.004F + w1;
            // Continuous connected cursive ribbon quad (top & bottom)
            ClientAuraRenderer.emitCrystalQuad(solidVc, mat,
                    sx0, yTop, z0 - hw, 36, 62, 128, 255,
                    sx1, yTop, z1 - hw, 36, 62, 128, 255,
                    sx1, yTop, z1 + hw, 36, 62, 128, 255,
                    sx0, yTop, z0 + hw, 36, 62, 128, 255);
            ClientAuraRenderer.emitCrystalQuad(solidVc, mat,
                    sx0, yTop, z0 + hw, 36, 62, 128, 255,
                    sx1, yTop, z1 + hw, 36, 62, 128, 255,
                    sx1, yTop, z1 - hw, 36, 62, 128, 255,
                    sx0, yTop, z0 - hw, 36, 62, 128, 255);
        }
    }

    private static float[] evalStarPoint(float t) {
        float segF = Mth.clamp(t, 0.0F, 0.9999F) * 10.0F;
        int idx = (int) segF;
        float frac = segF - idx;
        double a0 = Math.PI * 0.5D + idx * (Math.PI / 5.0D);
        double a1 = Math.PI * 0.5D + (idx + 1) * (Math.PI / 5.0D);
        float r0 = (idx % 2 == 0) ? 0.030F : 0.012F;
        float r1 = ((idx + 1) % 2 == 0) ? 0.030F : 0.012F;
        float x0 = (float) Math.cos(a0) * r0;
        float z0 = (float) Math.sin(a0) * r0;
        float x1 = (float) Math.cos(a1) * r1;
        float z1 = (float) Math.sin(a1) * r1;
        return new float[]{Mth.lerp(frac, x0, x1), Mth.lerp(frac, z0, z1)};
    }

    private static void drawConnectedShape(
            VertexConsumer solidVc, Matrix4f mat,
            float y, float prog, boolean drawStar
    ) {
        int totalSegs = drawStar ? 10 : 48;
        int drawnSegs = Math.max(1, Math.round(totalSegs * Mth.clamp(prog, 0.0F, 1.0F)));
        float hw = 0.0028F;
        for (int s = 0; s < drawnSegs; s++) {
            float t0 = s / (float) totalSegs;
            float t1 = (s + 1) / (float) totalSegs;
            float x0, z0, x1, z1;
            if (drawStar) {
                float[] p0 = evalStarPoint(t0);
                float[] p1 = evalStarPoint(t1 % 1.0F);
                x0 = -0.085F + p0[0];
                z0 = -0.065F + p0[1];
                x1 = -0.085F + p1[0];
                z1 = -0.065F + p1[1];
            } else {
                double th0 = t0 * Math.PI * 2.0D;
                double th1 = t1 * Math.PI * 2.0D;
                float hx0 = (float) (16.0D * Math.pow(Math.sin(th0), 3)) * 0.0018F;
                float hz0 = (float) (13.0D * Math.cos(th0) - 5.0D * Math.cos(2.0D * th0) - 2.0D * Math.cos(3.0D * th0) - Math.cos(4.0D * th0)) * 0.0016F;
                float hx1 = (float) (16.0D * Math.pow(Math.sin(th1), 3)) * 0.0018F;
                float hz1 = (float) (13.0D * Math.cos(th1) - 5.0D * Math.cos(2.0D * th1) - 2.0D * Math.cos(3.0D * th1) - Math.cos(4.0D * th1)) * 0.0016F;
                x0 = -0.085F - hx0;
                z0 = -0.065F + hz0;
                x1 = -0.085F - hx1;
                z1 = -0.065F + hz1;
            }
            float dx = x1 - x0;
            float dz = z1 - z0;
            float len = Mth.sqrt(dx * dx + dz * dz);
            if (len < 1.0e-5F) continue;
            float nx = (-dz / len) * hw;
            float nz = (dx / len) * hw;
            ClientAuraRenderer.emitCrystalQuad(solidVc, mat,
                    x0 - nx, y, z0 - nz, 48, 82, 168, 255,
                    x1 - nx, y, z1 - nz, 48, 82, 168, 255,
                    x1 + nx, y, z1 + nz, 48, 82, 168, 255,
                    x0 + nx, y, z0 + nz, 48, 82, 168, 255);
            ClientAuraRenderer.emitCrystalQuad(solidVc, mat,
                    x0 + nx, y, z0 + nz, 48, 82, 168, 255,
                    x1 + nx, y, z1 + nz, 48, 82, 168, 255,
                    x1 - nx, y, z1 - nz, 48, 82, 168, 255,
                    x0 - nx, y, z0 - nz, 48, 82, 168, 255);
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

        // Both pages start 100% blank white; writes Left (+X) then Right (-X) top-to-bottom, left-to-right, then flips to 2 blank pages
        float[] ws = getTsaritsaWriteState(time);
        float qx = ws[0];
        float qz = ws[1];
        float flipLift = ws[2] * 0.065F;
        float varProg = ws[3];
        float cycleSec = ws[4];
        // Left page (+X) & Right page (-X) ink:
        // Once page flip starts (6.4s), right page underneath is already clean blank white;
        // left page stays inked until flip lands (7.45s), while the flipping page has ink on its front face and blank white on its back face!
        float lineProg = Mth.clamp(varProg, 0.0F, 1.0F);
        float heartProg = Mth.clamp(varProg - 1.0F, 0.0F, 1.0F);
        if (cycleSec < 7.45F) {
            float totalLines = lineProg * 7.0F;
            int maxLine = (cycleSec < 6.4F) ? 7 : 4; // Right page is already clean blank under the flipping sheet
            for (int line = 0; line < maxLine; line++) {
                float lineFrac = Mth.clamp(totalLines - line, 0.0F, 1.0F);
                if (lineFrac <= 0.02F) continue;
                float lz = 0.058F - (line % 4) * 0.042F;
                if (line < 4) {
                    drawSquigglyCyrillicLine(solidVc, dMat, 0.142F, 0.030F, lz, 0.9112F, 0.9135F, lineFrac, line);
                } else {
                    drawSquigglyCyrillicLine(solidVc, dMat, -0.028F, -0.142F, lz, 0.9112F, 0.9135F, lineFrac, line);
                }
            }
            // Continuous connected Heart or 5-pointed Star ink line at bottom of page 2 while writing (5.6..6.4s)
            boolean drawStar = ws[5] > 0.5F;
            if (cycleSec < 6.4F && heartProg > 0.01F) {
                drawConnectedShape(solidVc, dMat, 0.9138F, heartProg, drawStar);
            }
        }
        // Two-sided flipping sheet (6.4..7.5s): front/top face carries page-2 ink & heart; back face is clean blank white
        if (cycleSec >= 6.4F && cycleSec < 7.5F) {
            float flipProg = WindGliderRenderer.smoothStep01((cycleSec - 6.4F) / 1.1F);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.9125F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-flipProg * 180.0F));
            Matrix4f fMat = poseStack.last().pose();
            // White two-sided parchment sheet
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, fMat, -0.155F, 0.0F, -0.102F, -0.004F, 0.003F, 0.102F, 248, 242, 228, 255);
            // Ink & heart ONLY on the top face (y > 0.003) so when flipped past 90 deg the back face is clean white!
            if (flipProg < 0.50F) {
                for (int line = 4; line < 7; line++) {
                    float lz = 0.058F - (line % 4) * 0.042F;
                    drawSquigglyCyrillicLine(solidVc, fMat, -0.028F, -0.142F, lz, 0.0032F, 0.0045F, 1.0F, line);
                }
                boolean drawStarFlip = ws[5] > 0.5F;
                drawConnectedShape(solidVc, fMat, 0.0048F, 1.0F, drawStarFlip);
            }
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(qx, 0.914F + flipLift, qz);
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
        float pt = mc.getFrameTime();
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

        // 1-3. Blockbench 3D Tsaritsa Ice Throne Model
        poseStack.pushPose();
        float throneScale = 0.72F;
        poseStack.scale(-throneScale, -throneScale, throneScale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.50F, 0.0F);
        VertexConsumer throneVc = buffer.getBuffer(RenderType.entityTranslucent(TSARITSA_THRONE_TEXTURE));
        getOrBakeThroneModel().renderToBuffer(poseStack, throneVc, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // 4. Thin Floating Royal Ice Desk (NO support legs underneath, gently levitating!)
        float writeTime3p = (d != null && d.tsaritsaIdleStartMs > 0L) ? ((System.currentTimeMillis() - d.tsaritsaIdleStartMs) * 0.012F) : 0.0F;
        renderFloatingDeskAndItems(poseStack, buffer, writeTime3p, deskBob, false);

        // 5. Orbiting Pointed Cryo Crystal Shards around the throne
        VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
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
