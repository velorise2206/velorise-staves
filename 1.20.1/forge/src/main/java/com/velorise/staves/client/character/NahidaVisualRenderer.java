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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated visual renderer for Dendro Archon Nahida (A Thousand Floating Dreams):
 * - Jade crystal vine swing, hopscotch N1-N4 ground tiles & sprouting clovers, CA viewfinder box & giant clover burst
 * - Seed of Skandha 3D mark & world link arcs
 * - Shrine of Maya 3D geodesic lotus dome mesh & sanctuary pillars
 * - 360-degree pirouette hop, viewfinder framing HUD & poses
 */
public final class NahidaVisualRenderer {
    private NahidaVisualRenderer() {}

    public static boolean renderViewfinderHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !ClientAuraTracker.isNahidaViewfinder(mc.player.getId())) return false;
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int cx = sw / 2;
        int cy = sh / 2;
        int hw = 70;
        int hh = 52;
        int borderCol = 0xEE4AF072;
        int starCol = 0xFFF0FFC8;
        int dotCol = 0xCC74FF92;
        int scanCol = 0x1440E060;

        guiGraphics.fill(cx - hw, cy - hh, cx + hw, cy + hh, scanCol);

        int arm = 16;
        int thick = 2;
        guiGraphics.fill(cx - hw, cy - hh, cx - hw + arm, cy - hh + thick, borderCol);
        guiGraphics.fill(cx - hw, cy - hh, cx - hw + thick, cy - hh + arm, borderCol);
        guiGraphics.fill(cx + hw - arm, cy - hh, cx + hw, cy - hh + thick, borderCol);
        guiGraphics.fill(cx + hw - thick, cy - hh, cx + hw, cy - hh + arm, borderCol);
        guiGraphics.fill(cx - hw, cy + hh - thick, cx - hw + arm, cy + hh, borderCol);
        guiGraphics.fill(cx - hw, cy + hh - arm, cx - hw + thick, cy + hh, borderCol);
        guiGraphics.fill(cx + hw - arm, cy + hh - thick, cx + hw, cy + hh, borderCol);
        guiGraphics.fill(cx + hw - thick, cy + hh - arm, cx + hw, cy + hh, borderCol);

        for (int[] corner : new int[][]{{cx - hw, cy - hh}, {cx + hw, cy - hh}, {cx - hw, cy + hh}, {cx + hw, cy + hh}}) {
            guiGraphics.fill(corner[0] - 2, corner[1] - 1, corner[0] + 3, corner[1] + 2, starCol);
            guiGraphics.fill(corner[0] - 1, corner[1] - 2, corner[0] + 2, corner[1] + 3, starCol);
            guiGraphics.fill(corner[0], corner[1], corner[0] + 1, corner[1] + 1, 0xFFFFFFFF);
        }

        for (int offset = -40; offset <= 40; offset += 20) {
            if (offset != 0) {
                guiGraphics.fill(cx + offset - 1, cy - hh, cx + offset + 1, cy - hh + 2, dotCol);
                guiGraphics.fill(cx + offset - 1, cy + hh - 2, cx + offset + 1, cy + hh, dotCol);
            }
        }
        for (int offset = -30; offset <= 30; offset += 20) {
            if (offset != 0) {
                guiGraphics.fill(cx - hw, cy + offset - 1, cx - hw + 2, cy + offset + 1, dotCol);
                guiGraphics.fill(cx + hw - 2, cy + offset - 1, cx + hw, cy + offset + 1, dotCol);
            }
        }

        guiGraphics.fill(cx - 5, cy - 1, cx - 2, cy + 1, borderCol);
        guiGraphics.fill(cx + 2, cy - 1, cx + 5, cy + 1, borderCol);
        guiGraphics.fill(cx - 1, cy - 5, cx + 1, cy - 2, borderCol);
        guiGraphics.fill(cx - 1, cy + 2, cx + 1, cy + 5, borderCol);
        return true;
    }

    private static float stepIdleSwingFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.nahidaIdleSwingLastMs <= 0L) {
            d.nahidaIdleSwingLastMs = nowMs;
            if (d.nahidaIdleSwing) {
                d.nahidaIdleSwingFade = Math.min(1.0F, d.nahidaIdleSwingFade + 0.016F / 0.96F);
            }
        } else if (nowMs > d.nahidaIdleSwingLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.nahidaIdleSwingLastMs) / 1000.0F);
            d.nahidaIdleSwingLastMs = nowMs;
            if (d.nahidaIdleSwing) {
                d.nahidaIdleSwingFade = Math.min(1.0F, d.nahidaIdleSwingFade + dt / 0.96F);
            } else {
                d.nahidaIdleSwingFade = Math.max(0.0F, d.nahidaIdleSwingFade - dt / 0.35F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.nahidaIdleSwingFade);
    }

    private static float stepViewfinderFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.nahidaViewfinderLastMs <= 0L) {
            d.nahidaViewfinderLastMs = nowMs;
            if (d.nahidaViewfinder) {
                d.nahidaViewfinderFade = Math.min(1.0F, d.nahidaViewfinderFade + 0.016F / 0.42F);
            }
        } else if (nowMs > d.nahidaViewfinderLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.nahidaViewfinderLastMs) / 1000.0F);
            d.nahidaViewfinderLastMs = nowMs;
            if (d.nahidaViewfinder) {
                d.nahidaViewfinderFade = Math.min(1.0F, d.nahidaViewfinderFade + dt / 0.42F);
            } else {
                d.nahidaViewfinderFade = Math.max(0.0F, d.nahidaViewfinderFade - dt / 0.42F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.nahidaViewfinderFade);
    }

    public static float getViewfinderFade(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof LivingEntity le)) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(le.getId());
        if (d == null || (!d.nahidaViewfinder && d.nahidaViewfinderFade <= 0.001F)) return 0.0F;
        return stepViewfinderFade(d, System.currentTimeMillis());
    }

    public static float getNaPlatformFade(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof LivingEntity le)) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(le.getId());
        if (d == null) return 0.0F;
        long nowMs = System.currentTimeMillis();
        boolean active = nowMs < d.nahidaNaPlatformUntilMs && !d.nahidaViewfinder && !d.nahidaIdleSwing;
        if (!active && d.nahidaNaPlatformFade <= 0.001F) {
            d.nahidaNaPlatformFade = 0.0F;
            d.nahidaNaPlatformLastMs = nowMs;
            return 0.0F;
        }
        if (d.nahidaNaPlatformLastMs <= 0L) {
            d.nahidaNaPlatformLastMs = nowMs;
            if (active) d.nahidaNaPlatformFade = Math.min(1.0F, d.nahidaNaPlatformFade + 0.016F / 0.16F);
        } else if (nowMs > d.nahidaNaPlatformLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.nahidaNaPlatformLastMs) / 1000.0F);
            d.nahidaNaPlatformLastMs = nowMs;
            if (active) {
                d.nahidaNaPlatformFade = Math.min(1.0F, d.nahidaNaPlatformFade + dt / 0.16F);
            } else {
                d.nahidaNaPlatformFade = Math.max(0.0F, d.nahidaNaPlatformFade - dt / 0.45F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.nahidaNaPlatformFade);
    }

    public static float getNaPlatformHopY(LivingEntity entity) {
        if (entity == null) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        if (d == null) return 0.0F;
        long nowMs = System.currentTimeMillis();
        if (nowMs >= d.staffAnimUntil) return 0.0F;
        int anim = d.staffAnimType;
        if (anim < ClientAuraTracker.ANIM_NAHIDA_NA_1 || anim > ClientAuraTracker.ANIM_NAHIDA_NA_4) return 0.0F;
        float totalMs = Math.max(1.0F, (float) (d.staffAnimUntil - d.staffAnimStartTime));
        float t = Mth.clamp((nowMs - d.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
        return Mth.sin(t * (float) Math.PI) * 0.24F;
    }

    public static float getViewfinderCameraOffsetY(net.minecraft.world.entity.Entity entity, float partialTick) {
        if (!(entity instanceof LivingEntity le)) return 0.0F;
        float w = getViewfinderFade(entity);
        float vfOff = 0.0F;
        if (w > 0.001F) {
            float hover = Mth.sin((le.tickCount + partialTick) * 0.12F);
            vfOff = (0.48F + hover * 0.04F) * w;
        }
        float platW = getNaPlatformFade(entity);
        float hopY = platW > 0.001F ? getNaPlatformHopY(le) * platW : 0.0F;
        return vfOff + (platW > 0.001F ? 0.34F * platW + hopY : 0.0F);
    }

    public static float getSwingCameraPitchOffset(net.minecraft.world.entity.Entity entity, float partialTick) {
        if (!(entity instanceof LivingEntity le) || !ClientAuraTracker.isNahidaIdleSwing(le.getId())) return 0.0F;
        if (le.isUsingItem() || le.swinging) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(le.getId());
        float w = stepIdleSwingFade(d, System.currentTimeMillis());
        if (w <= 0.01F) return 0.0F;
        float sway = Mth.sin((le.tickCount + partialTick) * 0.10F);
        return sway * 4.2F * w;
    }

    public static void renderFirstPersonSwing(AbstractClientPlayer player, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        if (player == null || !ClientAuraTracker.isNahidaIdleSwing(player.getId())) return;
        if (player.isUsingItem() || player.swinging) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(player.getId());
        float fade = stepIdleSwingFade(d, System.currentTimeMillis());
        if (fade <= 0.02F) return;
        float sway = Mth.sin((player.tickCount + partialTicks) * 0.10F);
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.34F, -0.08F + sway * 0.03F, -0.42F + sway * 0.04F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-sway * 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 4.0F));
            Matrix4f mat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.014F, -0.45F, -0.014F, 0.014F, 0.55F, 0.014F, 120, 225, 110, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.022F, 0.02F, -0.022F, 0.022F, 0.09F, 0.022F, 225, 255, 185, 255);
            poseStack.popPose();
        }
    }

    public static boolean applyIdleAndViewfinderHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (ClientAuraTracker.isNahidaIdleSwing(entity.getId())) {
            ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
            float w = stepIdleSwingFade(d, System.currentTimeMillis());
            float sway = Mth.sin(ageInTicks * 0.10F);
            float sitDropY = 3.2F * w;
            float swayZ = -3.45F * sway * w;
            model.body.y += sitDropY;
            model.head.y += sitDropY;
            model.hat.y += sitDropY;
            model.rightArm.y += sitDropY;
            model.leftArm.y += sitDropY;
            model.rightLeg.y += sitDropY;
            model.leftLeg.y += sitDropY;

            model.body.z += swayZ;
            model.head.z += swayZ;
            model.hat.z += swayZ;
            model.rightArm.z += swayZ;
            model.leftArm.z += swayZ;
            model.rightLeg.z += swayZ - 1.8F * w;
            model.leftLeg.z += swayZ - 1.8F * w;

            model.body.xRot = Mth.lerp(w, model.body.xRot, 0.06F * sway);
            model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -1.28F + 0.16F * sway);
            model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, -1.28F - 0.16F * sway);
            model.rightLeg.yRot = Mth.lerp(w, model.rightLeg.yRot, 0.08F);
            model.leftLeg.yRot = Mth.lerp(w, model.leftLeg.yRot, -0.08F);
            model.head.xRot = Mth.lerp(w, model.head.xRot, 0.08F + 0.04F * sway);
            model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.45F, 0.45F), 0.0F);
            model.head.zRot = Mth.lerp(w, model.head.zRot, 0.05F * sway);
            model.hat.copyFrom(model.head);
            return true;
        }
        float vfW = getViewfinderFade(entity);
        if (vfW > 0.01F) {
            float hover = Mth.sin(ageInTicks * 0.12F);
            float liftY = -(0.48F + hover * 0.04F) * 16.0F * vfW;
            model.head.y += liftY;
            model.hat.y += liftY;
            model.body.y += liftY;
            model.rightArm.y += liftY;
            model.leftArm.y += liftY;
            model.rightLeg.y += liftY;
            model.leftLeg.y += liftY;

            if (ClientAuraTracker.isNahidaViewfinder(entity.getId())) {
                model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
                model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            }
            model.rightArm.xRot = Mth.lerp(vfW, model.rightArm.xRot, -1.52F + model.head.xRot * 0.75F);
            model.leftArm.xRot = Mth.lerp(vfW, model.leftArm.xRot, -1.52F + model.head.xRot * 0.75F);
            model.rightArm.yRot = Mth.lerp(vfW, model.rightArm.yRot, -0.28F + model.head.yRot * 0.5F);
            model.leftArm.yRot = Mth.lerp(vfW, model.leftArm.yRot, 0.28F + model.head.yRot * 0.5F);
            model.rightArm.zRot = Mth.lerp(vfW, model.rightArm.zRot, 0.18F);
            model.leftArm.zRot = Mth.lerp(vfW, model.leftArm.zRot, -0.18F);

            model.head.yRot = Mth.lerp(vfW, model.head.yRot, 0.0F);
            model.hat.copyFrom(model.head);
            model.body.yRot = Mth.lerp(vfW, model.body.yRot, 0.0F);

            model.leftLeg.xRot = Mth.lerp(vfW, model.leftLeg.xRot, 0.22F + hover * 0.05F);
            model.rightLeg.xRot = Mth.lerp(vfW, model.rightLeg.xRot, 0.34F - hover * 0.05F);
            model.leftLeg.zRot = Mth.lerp(vfW, model.leftLeg.zRot, -0.08F);
            model.rightLeg.zRot = Mth.lerp(vfW, model.rightLeg.zRot, 0.08F);
            return true;
        }
        float platW = getNaPlatformFade(entity);
        if (platW > 0.005F) {
            float hopY = getNaPlatformHopY(entity) * platW;
            float liftY = -(0.34F * platW + hopY) * 16.0F;
            model.head.y += liftY;
            model.hat.y += liftY;
            model.body.y += liftY;
            model.rightArm.y += liftY;
            model.leftArm.y += liftY;
            model.rightLeg.y += liftY;
            model.leftLeg.y += liftY;
        }
        return false;
    }

    public static boolean applyActionHumanoidAnimation(
            HumanoidModel<?> model,
            float ageInTicks,
            int activeAnim,
            float t
    ) {
        if (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4) {
            float hop = Mth.sin(t * (float) Math.PI);
            int step = activeAnim - ClientAuraTracker.ANIM_NAHIDA_NA_1 + 1;
            if (step == 1) {
                model.body.yRot = -0.22F * hop;
                model.rightLeg.xRot = -0.75F * hop;
                model.leftLeg.xRot = 0.20F * hop;
                model.rightArm.xRot = -0.95F * hop;
                model.leftArm.xRot = 0.45F * hop;
                model.rightArm.zRot = 0.35F * hop;
            } else if (step == 2) {
                model.body.yRot = 0.22F * hop;
                model.leftLeg.xRot = -0.75F * hop;
                model.rightLeg.xRot = 0.20F * hop;
                model.leftArm.xRot = -0.95F * hop;
                model.rightArm.xRot = 0.45F * hop;
                model.leftArm.zRot = -0.35F * hop;
            } else if (step == 3) {
                // Step 3 (Penultimate): Light hop bounce & forward cast (NO body spin)
                model.body.yRot = -0.15F * hop;
                model.rightLeg.xRot = -0.45F * hop;
                model.leftLeg.xRot = 0.35F * hop;
                model.rightArm.xRot = -1.45F * hop;
                model.leftArm.xRot = -1.45F * hop;
                model.rightArm.zRot = 0.65F * hop;
                model.leftArm.zRot = -0.65F * hop;
            } else {
                // Step 4 (Finisher): Light hop bounce & finger snap finish (NO body spin)
                model.body.yRot = 0.15F * hop;
                model.rightLeg.xRot = -0.68F * hop;
                model.leftLeg.xRot = 0.48F * hop;
                model.rightLeg.zRot = 0.08F * hop;
                model.leftLeg.zRot = -0.08F * hop;
                model.rightArm.xRot = -1.65F * hop;
                model.leftArm.xRot = -1.65F * hop;
                model.rightArm.yRot = -0.42F * hop;
                model.leftArm.yRot = 0.42F * hop;
            }
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_HOLD_E) {
            float w = (t < 0.15F) ? WindGliderRenderer.smoothStep01(t / 0.15F) : 1.0F;
            float sway = Mth.sin(ageInTicks * 0.12F) * 0.04F;
            model.leftLeg.xRot = 0.22F + sway;
            model.rightLeg.xRot = 0.32F - sway;
            model.leftLeg.zRot = -0.08F;
            model.rightLeg.zRot = 0.08F;
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.45F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.45F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.42F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.42F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.15F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.15F);
            model.head.xRot += 0.04F * w;
            model.hat.copyFrom(model.head);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_TAP_E) {
            float sweep = Mth.sin(t * (float) Math.PI);
            float hopY = -6.5F * sweep;
            float spinY = -WindGliderRenderer.smoothStep01(t) * (float) (Math.PI * 2.0D);
            float cs = Mth.cos(spinY);
            float sn = Mth.sin(spinY);

            model.body.y += hopY;
            model.head.y += hopY;
            model.hat.y += hopY;

            model.body.yRot = spinY;
            model.head.yRot += spinY;
            model.hat.copyFrom(model.head);

            model.rightArm.x = -5.0F * cs;
            model.rightArm.z =  5.0F * sn;
            model.rightArm.y = 2.0F + hopY;
            model.rightArm.xRot = -0.85F * sweep;
            model.rightArm.yRot = spinY - 0.20F * sweep;
            model.rightArm.zRot = 0.65F * sweep;

            model.leftArm.x =  5.0F * cs;
            model.leftArm.z = -5.0F * sn;
            model.leftArm.y = 2.0F + hopY;
            model.leftArm.xRot = -0.85F * sweep;
            model.leftArm.yRot = spinY + 0.20F * sweep;
            model.leftArm.zRot = -0.65F * sweep;

            model.rightLeg.x = -1.9F * cs;
            model.rightLeg.z =  1.9F * sn;
            model.rightLeg.y = 12.0F + hopY;
            model.rightLeg.yRot = spinY;
            model.rightLeg.xRot = -0.30F * sweep;

            model.leftLeg.x =  1.9F * cs;
            model.leftLeg.z = -1.9F * sn;
            model.leftLeg.y = 12.0F + hopY;
            model.leftLeg.yRot = spinY;
            model.leftLeg.xRot = 0.25F * sweep;
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_CA) {
            float w = (t < 0.20F) ? WindGliderRenderer.smoothStep01(t / 0.20F) : (t < 0.70F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.70F) / 0.30F));
            model.body.xRot = Mth.lerp(t, 0.08F, -0.12F);
            model.body.yRot = Mth.lerp(t, -0.22F, 0.25F);
            model.rightArm.xRot = Mth.lerp(t, 0.20F, -1.85F);
            model.rightArm.yRot = Mth.lerp(t, -0.65F, 0.45F);
            model.rightArm.zRot = Mth.lerp(t, -0.45F, 0.35F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, 0.45F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, -0.32F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_BURST) {
            float w = (t < 0.20F) ? WindGliderRenderer.smoothStep01(t / 0.20F) : (t < 0.80F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.80F) / 0.20F));
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.35F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.35F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.72F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.72F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.18F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0.18F);
            model.head.xRot += 0.16F * w;
            model.hat.copyFrom(model.head);
            model.leftLeg.xRot = 0.12F * w;
            model.rightLeg.xRot = 0.12F * w;
            return true;
        }
        return false;
    }

    public static boolean applyThirdPersonItemTransform(
            LivingEntity entity,
            ArcaneStaffItem.Kind effKind,
            PoseStack poseStack,
            int activeAnim
    ) {
        if (effKind != ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) return false;
        if (ClientAuraTracker.isNahidaViewfinder(entity.getId())) {
            poseStack.scale(0.0F, 0.0F, 0.0F);
            return true;
        }
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        boolean isNaCa = (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4)
                || activeAnim == ClientAuraTracker.ANIM_NAHIDA_CA
                || entity.swinging;
        if (isNaCa && d != null) {
            d.nahidaWeaponHideUntil = Math.max(d.nahidaWeaponHideUntil, now + 2000L);
        }
        if (d != null && d.nahidaWeaponHideUntil > 0L) {
            if (now < d.nahidaWeaponHideUntil) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
        }
        return false;
    }

    public static boolean setupFirstPersonCameraArmPose(
            AbstractClientPlayer player,
            HumanoidArm arm,
            PoseStack poseStack
    ) {
        if (player == null) return false;
        float vfW = getViewfinderFade(player);
        if (vfW <= 0.01F) return false;
        ItemStack mainStack = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(mainStack) != ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) return false;
        float armDir = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        // Smoothly raise/lower first-person framing hands in sync with Nahida viewfinder levitation
        poseStack.translate(-armDir * 0.16F * vfW, Mth.lerp(vfW, -0.65F, -0.28F), -0.18F * vfW);
        poseStack.mulPose(Axis.ZP.rotationDegrees(armDir * 34.0F * vfW));
        poseStack.mulPose(Axis.XP.rotationDegrees(22.0F * vfW));
        poseStack.mulPose(Axis.YP.rotationDegrees(-armDir * 10.0F * vfW));
        return true;
    }

    public static boolean applyFirstPersonTransform(
            ArcaneStaffItem.Kind effKind,
            float dir,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (effKind != ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && ClientAuraTracker.isNahidaViewfinder(mc.player.getId())) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(0.001F, 0.001F, 0.001F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NAHIDA_BURST && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float raise = WindGliderRenderer.smoothStep01(Math.min(1.0F, t * 3.5F));
            float floatBob = Mth.sin(t * (float) Math.PI * 3.0F) * 0.025F;
            poseStack.translate(0.0F, -0.18F * raise + floatBob, -0.38F * raise);
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F * raise));
            return true;
        }
        boolean isNaCa = (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4)
                || activeAnim == ClientAuraTracker.ANIM_NAHIDA_CA;
        if (isNaCa && data != null) {
            data.nahidaWeaponHideUntil = Math.max(data.nahidaWeaponHideUntil, now + 2000L);
        }
        if (data != null && data.nahidaWeaponHideUntil > 0L) {
            if (now < data.nahidaWeaponHideUntil) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
        }
        return false;
    }

    public static void renderEntityEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        long now = System.currentTimeMillis();
        int activeAnim = (d != null && now < d.staffAnimUntil) ? d.staffAnimType : 0;
        boolean idleSwing = ClientAuraTracker.isNahidaIdleSwing(id);
        boolean skandha = ClientAuraTracker.hasSkandhaMark(id);
        boolean shrine = ClientAuraTracker.hasNahidaShrine(id);
        boolean nahidaAnim = (activeAnim >= ClientAuraTracker.ANIM_NAHIDA_NA_1 && activeAnim <= ClientAuraTracker.ANIM_NAHIDA_NA_4);
        float platFade = getNaPlatformFade(entity);

        if (!idleSwing && !skandha && !shrine && !nahidaAnim && platFade <= 0.01F) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);

        if (platFade > 0.01F && !isFirstPersonSelf) {
            ResourceLocation keycapTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_keycap_tile.png");
            ResourceLocation starTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_ground_star.png");
            float hopY = getNaPlatformHopY(entity) * platFade;
            poseStack.pushPose();
            poseStack.translate(0.0F, Math.max(0.02F, 0.36F * platFade - 0.14F + hopY * 0.45F), 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            renderHopscotchKeycap3D(poseStack, buffer, keycapTex, 0.56F * platFade, 0.14F * platFade, platFade);
            VertexConsumer starVc = buffer.getBuffer(RenderType.entityCutoutNoCull(starTex));
            renderGroundStarSeal(starVc, poseStack, 0.0F, 0.15F * platFade, 0.0F, 0.72F * platFade, time * 3.5F);
            poseStack.popPose();
            vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        }

        if (idleSwing) {
            float swingFade = stepIdleSwingFade(d, now);
            if (!isFirstPersonSelf && swingFade > 0.02F) {
                ResourceLocation ropeTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_swing_rope.png");
                ResourceLocation seatTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_swing_seat.png");
                float swingBodyYaw = (d != null && d.nahidaIdleYawSet) ? d.nahidaIdleYaw : bodyYaw;
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(-swingBodyYaw));
                poseStack.translate(0.0F, 1.5F * (1.0F - swingFade), 0.0F);
                poseStack.scale(swingFade, swingFade, swingFade);
                renderNahidaIdleSwing3D(poseStack, buffer, time, ropeTex, seatTex, swingFade);
                poseStack.popPose();
                vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
            }
        }

        if (skandha) {
            ResourceLocation skandhaTex = new ResourceLocation("velorise_staves", "textures/entity/seed_of_skandha.png");
            VertexConsumer skVc = buffer.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.58F + Mth.sin(time * 0.14F) * 0.04F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.5F));
            float half = Math.max(0.38F, entity.getBbWidth() * 0.58F);
            for (int plane = 0; plane < 2; plane++) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(plane * 90.0F));
                PoseStack.Pose p = poseStack.last();
                WindGliderRenderer.emitWingQuad(skVc, p, -half, -half, 0.0F, 0.0F, 1.0F, half, -half, 0.0F, 1.0F, 1.0F, half, half, 0.0F, 1.0F, 0.0F, -half, half, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                WindGliderRenderer.emitWingQuad(skVc, p, -half, half, 0.0F, 0.0F, 0.0F, half, half, 0.0F, 1.0F, 0.0F, half, -half, 0.0F, 1.0F, 1.0F, -half, -half, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                poseStack.popPose();
            }
            poseStack.popPose();
            vc = buffer.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        }

        if (shrine && d != null) {
            // Spawn world-anchored footstep Dendro flower-grass clumps fixedly at the spot just walked past with a slight offset
            double curX = entity.getX();
            double curY = entity.getY();
            double curZ = entity.getZ();
            if (Double.isNaN(d.lastShrineStepX) || Double.isNaN(d.lastShrineStepZ) || Double.isNaN(d.lastShrineStepY)) {
                d.lastShrineStepX = curX;
                d.lastShrineStepY = curY;
                d.lastShrineStepZ = curZ;
            } else {
                double dx = curX - d.lastShrineStepX;
                double dz = curZ - d.lastShrineStepZ;
                boolean movedEnough = (dx * dx + dz * dz) >= 0.42D;
                if (movedEnough && (now - d.lastShrineStepMs) >= 180L && entity.onGround()) {
                    double prevX = d.lastShrineStepX;
                    double prevY = d.lastShrineStepY;
                    double prevZ = d.lastShrineStepZ;
                    d.lastShrineStepX = curX;
                    d.lastShrineStepY = curY;
                    d.lastShrineStepZ = curZ;
                    d.lastShrineStepMs = now;
                    int varIdx = d.shrineStepVariant++;
                    double moveLen = Math.sqrt(dx * dx + dz * dz);
                    double fwdX = moveLen > 1.0E-4D ? (dx / moveLen) : -Math.sin(Math.toRadians(bodyYaw));
                    double fwdZ = moveLen > 1.0E-4D ? (dz / moveLen) : Math.cos(Math.toRadians(bodyYaw));
                    double rightUnitX = -fwdZ;
                    double rightUnitZ = fwdX;
                    float sideOffset = ((varIdx & 1) == 0 ? 0.24F : -0.24F) + ((varIdx % 3 - 1) * 0.06F);
                    float alongOffset = ((varIdx % 5) - 2) * 0.05F;
                    Vec3 footStart = new Vec3(
                            prevX + rightUnitX * sideOffset + fwdX * alongOffset,
                            prevY,
                            prevZ + rightUnitZ * sideOffset + fwdZ * alongOffset
                    );
                    Vec3 footEnd = footStart.add(fwdX, 0.0D, fwdZ);
                    ClientAuraTracker.addArc(-8, footStart, -8, footEnd, 36, 0x4E420000 | (varIdx & 7));
                }
            }
        }
    }



    private static void renderNahidaIdleSwing3D(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float time,
            ResourceLocation ropeTex,
            ResourceLocation seatTex,
            float swingFade
    ) {
        // Pendulum swing around the floating top 4-petal flower anchors (y = 2.68F, NO horizontal top bar!)
        float swingDeg = -Mth.sin(time * 0.10F) * 4.6F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.68F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(swingDeg));
        poseStack.translate(0.0F, -2.68F, 0.0F);

        // 1. Left & Right Braided Vine Ropes with attached leaves, white-rimmed 4-petal clovers & top flower anchor
        VertexConsumer ropeVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ropeTex));
        float ropeHalfW = 0.24F;
        float ropeBotY = 0.44F;
        float ropeTopY = 2.74F;
        for (float rx : new float[]{-0.42F, 0.42F}) {
            poseStack.pushPose();
            poseStack.translate(rx, 0.0F, 0.0F);
            for (int plane = 0; plane < 2; plane++) {
                poseStack.pushPose();
                if (plane == 1) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(75.0F));
                }
                PoseStack.Pose p = poseStack.last();
                // Mirror UV horizontally on right rope so flower clusters frame Nahida symmetrically
                float u0 = (rx < 0.0F) ? 0.0F : 1.0F;
                float u1 = (rx < 0.0F) ? 1.0F : 0.0F;
                WindGliderRenderer.emitWingQuad(ropeVc, p,
                        -ropeHalfW, ropeBotY, 0.0F, u0, 1.0F,
                         ropeHalfW, ropeBotY, 0.0F, u1, 1.0F,
                         ropeHalfW, ropeTopY, 0.0F, u1, 0.0F,
                        -ropeHalfW, ropeTopY, 0.0F, u0, 0.0F,
                        0.0F, 0.0F, 1.0F);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        // 2. Ornate Translucent Seat Top & Bottom Surfaces (nahida_swing_seat.png)
        float sx = 0.49F;
        float sz = 0.19F;
        float syBot = 0.41F;
        float syTop = 0.51F;
        VertexConsumer seatVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(seatTex));
        for (float yFace : new float[]{syTop, syBot}) {
            poseStack.pushPose();
            poseStack.translate(0.0F, yFace, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            PoseStack.Pose p = poseStack.last();
            WindGliderRenderer.emitWingQuad(seatVc, p, -sx, -sz, 0.0F, 0.0F, 1.0F, sx, -sz, 0.0F, 1.0F, 1.0F, sx, sz, 0.0F, 1.0F, 0.0F, -sx, sz, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            poseStack.popPose();
        }

        // 3. 3D Hollow Rectangular Wireframe Prism Cage (glowing neon lime-yellow edges & vertical struts matching Images 2, 3, 4)
        VertexConsumer wireVc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        Matrix4f mat = poseStack.last().pose();
        float tBar = 0.016F;
        for (float yR : new float[]{syBot, syTop}) {
            // Front & Back horizontal bars
            WindGliderRenderer.emitDoubleQuad(wireVc, mat, -sx, yR - tBar, -sz, sx, yR - tBar, -sz, sx, yR + tBar, -sz, -sx, yR + tBar, -sz, 215, 255, 155, 240);
            WindGliderRenderer.emitDoubleQuad(wireVc, mat, -sx, yR - tBar,  sz, sx, yR - tBar,  sz, sx, yR + tBar,  sz, -sx, yR + tBar,  sz, 215, 255, 155, 240);
            // Left & Right horizontal bars
            WindGliderRenderer.emitDoubleQuad(wireVc, mat, -sx, yR - tBar, -sz, -sx, yR - tBar, sz, -sx, yR + tBar, sz, -sx, yR + tBar, -sz, 195, 255, 140, 235);
            WindGliderRenderer.emitDoubleQuad(wireVc, mat,  sx, yR - tBar, -sz,  sx, yR - tBar, sz,  sx, yR + tBar, sz,  sx, yR + tBar, -sz, 195, 255, 140, 235);
        }
        // Vertical struts connecting top and bottom rims along front & back (Images 3 & 4)
        for (float vx : new float[]{-sx, -0.24F, 0.0F, 0.24F, sx}) {
            for (float vz : new float[]{-sz, sz}) {
                WindGliderRenderer.emitDoubleQuad(wireVc, mat, vx - tBar, syBot, vz, vx + tBar, syBot, vz, vx + tBar, syTop, vz, vx - tBar, syTop, vz, 210, 255, 150, 235);
            }
        }
        poseStack.popPose();
    }

    private static void renderNahidaShrineFootstepGrass(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 camPos,
            ClientAuraTracker.ActiveArc arc,
            long now
    ) {
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float grow = WindGliderRenderer.smoothStep01(Mth.clamp(prog / 0.22F, 0.0F, 1.0F));
        float fade = prog > 0.65F ? Mth.clamp((1.0F - prog) / 0.35F, 0.0F, 1.0F) : 1.0F;
        float timeSec = (now - arc.startTime) * 0.001F;
        int variant = arc.color & 7;

        Vec3 dir = arc.end.subtract(arc.start);
        Vec3 horiz = new Vec3(dir.x, 0.0D, dir.z);
        if (horiz.lengthSqr() < 1.0E-4D) horiz = new Vec3(0.0D, 0.0D, 1.0D);
        else horiz = horiz.normalize();
        float yawDeg = (float) Math.toDegrees(Math.atan2(-horiz.x, horiz.z)) + (variant - 3) * 18.0F;

        ResourceLocation stepGrassTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_shrine_step_grass.png");
        VertexConsumer grassVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(stepGrassTex));

        float sx = (float) (arc.start.x - camPos.x);
        float sy = (float) (arc.start.y - camPos.y);
        float sz = (float) (arc.start.z - camPos.z);

        poseStack.pushPose();
        poseStack.translate(sx, sy + 0.02F, sz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawDeg));

        // Primary lush 4-petal flower-grass clump sprouting at the footstep (scaled to 60%)
        float mainW = 0.36F * 0.80F * 0.60F * fade;
        float mainH = 0.68F * 0.80F * 0.60F * fade;
        renderUnfurlingTexturedRibbon(grassVc, poseStack, mainW, mainH, grow, timeSec, 0.044F * 0.60F, variant * 0.9F, 8);

        // Secondary smaller companion sprout slightly offset (scaled to 60%)
        poseStack.pushPose();
        poseStack.translate(((variant % 2 == 0) ? 0.176F : -0.176F) * 0.60F, 0.0F, -0.128F * 0.60F);
        poseStack.mulPose(Axis.YP.rotationDegrees(38.0F));
        renderUnfurlingTexturedRibbon(grassVc, poseStack, 0.24F * 0.80F * 0.60F * fade, 0.46F * 0.80F * 0.60F * fade, grow, timeSec, 0.036F * 0.60F, variant * 1.5F + 1.0F, 8);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderHopscotchKeycap3D(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation keycapTex,
            float topHalf,
            float height,
            float alphaScale
    ) {
        float botHalf = topHalf * 1.24F;
        int sideAlpha = Math.max(18, Math.round(95.0F * alphaScale));
        VertexConsumer sideVc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        Matrix4f mat = poseStack.last().pose();
        // 4 beveled translucent emerald keycap side walls (frustum skirt)
        WindGliderRenderer.emitDoubleQuad(sideVc, mat, -botHalf, 0.02F, -botHalf,  botHalf, 0.02F, -botHalf,  topHalf, height, -topHalf, -topHalf, height, -topHalf, 52, 210, 88, sideAlpha);
        WindGliderRenderer.emitDoubleQuad(sideVc, mat, -botHalf, 0.02F,  botHalf,  botHalf, 0.02F,  botHalf,  topHalf, height,  topHalf, -topHalf, height,  topHalf, 52, 210, 88, sideAlpha);
        WindGliderRenderer.emitDoubleQuad(sideVc, mat, -botHalf, 0.02F, -botHalf, -botHalf, 0.02F,  botHalf, -topHalf, height,  topHalf, -topHalf, height, -topHalf, 46, 195, 80, sideAlpha);
        WindGliderRenderer.emitDoubleQuad(sideVc, mat,  botHalf, 0.02F, -botHalf,  botHalf, 0.02F,  botHalf,  topHalf, height,  topHalf,  topHalf, height, -topHalf, 46, 195, 80, sideAlpha);

        // Top pixel-art keycap surface with 4-petal knotwork clover
        VertexConsumer topVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(keycapTex));
        poseStack.pushPose();
        poseStack.translate(0.0F, height + 0.005F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        PoseStack.Pose p = poseStack.last();
        WindGliderRenderer.emitWingQuad(topVc, p, -topHalf, -topHalf, 0.0F, 0.0F, 1.0F, topHalf, -topHalf, 0.0F, 1.0F, 1.0F, topHalf, topHalf, 0.0F, 1.0F, 0.0F, -topHalf, topHalf, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        poseStack.popPose();
    }

    private static void renderUnfurlingRetractingRibbon(
            VertexConsumer vc,
            PoseStack poseStack,
            float halfW,
            float fullH,
            float growProg,
            float retractProg,
            float timeSec,
            float curveAmp,
            float phase,
            int slices
    ) {
        float minF = Mth.clamp(retractProg, 0.0F, 1.0F);
        float maxF = Mth.clamp(growProg, 0.0F, 1.0F);
        if (maxF - minF <= 0.015F) return;
        for (int plane = 0; plane < 1; plane++) {
            poseStack.pushPose();
            PoseStack.Pose p = poseStack.last();
            for (int s = 0; s < slices; s++) {
                float f0 = Mth.lerp(s / (float) slices, minF, maxF);
                float f1 = Mth.lerp((s + 1) / (float) slices, minF, maxF);
                float y0 = f0 * fullH;
                float y1 = f1 * fullH;
                float v0 = 1.0F - f0;
                float v1 = 1.0F - f1;
                float sway0 = Mth.sin(f0 * (float) Math.PI * 1.25F - timeSec * 5.2F + phase) * curveAmp * f0;
                float sway1 = Mth.sin(f1 * (float) Math.PI * 1.25F - timeSec * 5.2F + phase) * curveAmp * f1;
                WindGliderRenderer.emitWingQuad(vc, p,
                        -halfW + sway0, y0, 0.0F, 0.0F, v0,
                         halfW + sway0, y0, 0.0F, 1.0F, v0,
                         halfW + sway1, y1, 0.0F, 1.0F, v1,
                        -halfW + sway1, y1, 0.0F, 0.0F, v1,
                        0.0F, 0.0F, 1.0F);
                WindGliderRenderer.emitWingQuad(vc, p,
                        -halfW + sway1, y1, 0.0F, 0.0F, v1,
                         halfW + sway1, y1, 0.0F, 1.0F, v1,
                         halfW + sway0, y0, 0.0F, 1.0F, v0,
                        -halfW + sway0, y0, 0.0F, 0.0F, v0,
                        0.0F, 0.0F, -1.0F);
            }
            poseStack.popPose();
        }
    }

    private static void renderUnfurlingTexturedRibbon(
            VertexConsumer vc,
            PoseStack poseStack,
            float halfW,
            float fullH,
            float growProg,
            float timeSec,
            float curveAmp,
            float phase,
            int slices
    ) {
        float maxH = fullH * growProg;
        if (maxH <= 0.02F) return;
        for (int plane = 0; plane < 2; plane++) {
            poseStack.pushPose();
            if (plane == 1) {
                poseStack.mulPose(Axis.YP.rotationDegrees(68.0F));
            }
            PoseStack.Pose p = poseStack.last();
            for (int s = 0; s < slices; s++) {
                float f0 = s / (float) slices;
                float f1 = (s + 1) / (float) slices;
                float y0 = f0 * maxH;
                float y1 = f1 * maxH;
                float v0 = 1.0F - f0 * growProg;
                float v1 = 1.0F - f1 * growProg;
                float sway0 = Mth.sin(f0 * (float) Math.PI * 1.25F - timeSec * 5.2F + phase) * curveAmp * f0;
                float sway1 = Mth.sin(f1 * (float) Math.PI * 1.25F - timeSec * 5.2F + phase) * curveAmp * f1;
                WindGliderRenderer.emitWingQuad(vc, p,
                        -halfW + sway0, y0, 0.0F, 0.0F, v0,
                         halfW + sway0, y0, 0.0F, 1.0F, v0,
                         halfW + sway1, y1, 0.0F, 1.0F, v1,
                        -halfW + sway1, y1, 0.0F, 0.0F, v1,
                        0.0F, 0.0F, 1.0F);
                WindGliderRenderer.emitWingQuad(vc, p,
                        -halfW + sway1, y1, 0.0F, 0.0F, v1,
                         halfW + sway1, y1, 0.0F, 1.0F, v1,
                         halfW + sway0, y0, 0.0F, 1.0F, v0,
                        -halfW + sway0, y0, 0.0F, 0.0F, v0,
                        0.0F, 0.0F, -1.0F);
            }
            poseStack.popPose();
        }
    }

    private static void renderGroundStarSeal(
            VertexConsumer starVc,
            PoseStack poseStack,
            float x,
            float y,
            float z,
            float halfSize,
            float rotDeg
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        PoseStack.Pose p = poseStack.last();
        WindGliderRenderer.emitWingQuad(starVc, p, -halfSize, -halfSize, 0.0F, 0.0F, 1.0F, halfSize, -halfSize, 0.0F, 1.0F, 1.0F, halfSize, halfSize, 0.0F, 1.0F, 0.0F, -halfSize, halfSize, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        WindGliderRenderer.emitWingQuad(starVc, p, -halfSize, halfSize, 0.0F, 0.0F, 0.0F, halfSize, halfSize, 0.0F, 1.0F, 0.0F, halfSize, -halfSize, 0.0F, 1.0F, 1.0F, -halfSize, -halfSize, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
        poseStack.popPose();
    }

    private static void renderNahidaNormalAttackWorldVfx(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Vec3 camPos,
            ClientAuraTracker.ActiveArc arc,
            long now,
            ResourceLocation skandhaTex
    ) {
        int step = Math.max(1, Math.min(4, arc.color & 0xFF));
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float grow = WindGliderRenderer.smoothStep01(Mth.clamp(prog / 0.34F, 0.0F, 1.0F));
        float retract = prog > 0.68F ? WindGliderRenderer.smoothStep01((prog - 0.68F) / 0.32F) : 0.0F;
        float fade = 1.0F;
        float timeSec = (now - arc.startTime) * 0.001F;

        Vec3 dir = arc.end.subtract(arc.start);
        Vec3 horiz = new Vec3(dir.x, 0.0D, dir.z);
        if (horiz.lengthSqr() < 1.0E-4D) horiz = new Vec3(0.0D, 0.0D, 1.0D);
        else horiz = horiz.normalize();
        float yawDeg = (float) Math.toDegrees(Math.atan2(-horiz.x, horiz.z));

        ResourceLocation keycapTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_keycap_tile.png");
        ResourceLocation starTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_ground_star.png");
        ResourceLocation vineTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_vine_flower.png");
        ResourceLocation heartTex = new ResourceLocation("velorise_staves", "textures/entity/nahida_heart_vine.png");

        float sx = (float) (arc.start.x - camPos.x);
        float sy = (float) (arc.start.y - camPos.y);
        float sz = (float) (arc.start.z - camPos.z);

        // 1. Anchored forward attack origin (pedestal is rendered once under entity in renderEntityEffects)
        poseStack.pushPose();
        poseStack.translate(sx, sy, sz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawDeg));

        // 2. Sprouting Curved Pixel-Art Vine-Flowers & Step 4 Heart of Vines Arch along 1.5x extended attack path
        if (step <= 3) {
            float[][] stemLayout = switch (step) {
                case 1 -> new float[][]{ {0.0F, 2.75F, 1.55F, 0.0F} };
                case 2 -> new float[][]{ {-0.62F, 2.55F, 1.40F, 0.0F}, {0.58F, 3.55F, 1.75F, 1.1F} };
                default -> new float[][]{ {-0.88F, 3.10F, 1.48F, 0.0F}, {0.0F, 4.15F, 1.92F, 0.8F}, {0.88F, 3.25F, 1.56F, 1.6F} };
            };

            VertexConsumer starVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(starTex));
            for (float[] st : stemLayout) {
                renderGroundStarSeal(starVc, poseStack, st[0], 0.03F, st[1], (0.44F + 0.28F * grow) * fade, timeSec * 30.0F + st[3] * 25.0F);
            }

            VertexConsumer vineVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(vineTex));
            for (float[] st : stemLayout) {
                poseStack.pushPose();
                poseStack.translate(st[0], 0.03F, st[1]);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(timeSec * 3.0F + st[3]) * 12.0F));
                renderUnfurlingRetractingRibbon(vineVc, poseStack, 0.56F, st[2], grow, retract, timeSec, 0.11F, st[3], 10);
                poseStack.popPose();
            }

            // Floating pixel-art 4-petal blossoms drifting up around the stems
            VertexConsumer petalVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
            for (int pIdx = 0; pIdx < step + 2; pIdx++) {
                float ang = pIdx * 2.1F + timeSec * 2.4F;
                float pr = 0.50F + (pIdx % 2) * 0.45F;
                float px = Mth.cos(ang) * pr;
                float pz = 2.15F + pIdx * 0.68F + Mth.sin(ang) * 0.25F;
                float py = 0.18F + prog * (0.65F + (pIdx % 3) * 0.22F);
                float pHalf = (0.14F + (pIdx % 2) * 0.05F) * fade;
                renderGroundStarSeal(petalVc, poseStack, px, py, pz, pHalf, timeSec * 85.0F + pIdx * 45.0F);
            }
        } else {
            // Step 4 Finisher: Grand Twin-Scroll Heart of Vines Arch + 2 Side Vine-Flowers + Ground Blossom Scatter (1.5x extended)
            float burstZ = 4.88F;
            VertexConsumer starVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(starTex));
            renderGroundStarSeal(starVc, poseStack, 0.0F, 0.035F, burstZ, (1.10F + 0.52F * grow) * fade, timeSec * 22.0F);
            renderGroundStarSeal(starVc, poseStack, -1.25F, 0.03F, burstZ - 0.80F, (0.48F + 0.24F * grow) * fade, -timeSec * 35.0F);
            renderGroundStarSeal(starVc, poseStack,  1.25F, 0.03F, burstZ - 0.80F, (0.48F + 0.24F * grow) * fade,  timeSec * 35.0F);

// NA4 vertical light beam removed

            // Side curved vine-flowers flanking the Heart Arch
            VertexConsumer vineVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(vineTex));
            for (float side : new float[]{-1.25F, 1.25F}) {
                poseStack.pushPose();
                poseStack.translate(side, 0.03F, burstZ - 0.80F);
                renderUnfurlingRetractingRibbon(vineVc, poseStack, 0.52F, 1.75F, grow, retract, timeSec, 0.09F, side, 10);
                poseStack.popPose();
            }

            // Center Twin-Scroll Heart of Vines Arch (nahida_heart_vine.png) unfurling from bottom to top
            VertexConsumer heartVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(heartTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.05F, burstZ);
            float heartHalfW = (1.50F + 0.30F * grow) * fade;
            float heartH = 3.05F * fade;
            renderUnfurlingRetractingRibbon(heartVc, poseStack, heartHalfW, heartH, grow, retract, timeSec, 0.05F, 0.0F, 12);
            poseStack.popPose();

            // Ground-scattered & rising waffle 4-petal clovers
            VertexConsumer petalVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
            for (int k = 0; k < 8; k++) {
                float ang = (float) (k * Math.PI * 0.25D + 0.35D);
                float rad = 1.15F + (k % 3) * 0.72F;
                float gx = Mth.cos(ang) * rad;
                float gz = 2.45F + Mth.sin(ang) * rad;
                float gy = 0.05F + (k % 2) * (0.25F * prog);
                float gHalf = (0.18F + (k % 3) * 0.06F) * fade * grow;
                renderGroundStarSeal(petalVc, poseStack, gx, gy, gz, gHalf, timeSec * 60.0F + k * 45.0F);
            }
        }
        poseStack.popPose();
    }

    public static void renderWorldArcs(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ResourceLocation skandhaTex = new ResourceLocation("velorise_staves", "textures/entity/seed_of_skandha.png");

        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.start == null || arc.end == null) continue;
            if (arc.fromEntityId == -5) {
                float radius = (float) Math.max(4.0D, arc.end.x);
                float cx = (float) (arc.start.x - camPos.x);
                float cy = (float) (arc.start.y - camPos.y);
                float cz = (float) (arc.start.z - camPos.z);
                float fadeIn = Mth.clamp((now - arc.startTime) / 350.0F, 0.0F, 1.0F);
                float fadeOut = Mth.clamp((arc.expireTime - now) / 450.0F, 0.0F, 1.0F);
                float fade = Math.min(fadeIn, fadeOut);
                float timeSec = (now - arc.startTime) * 0.001F;
                int aMain = Math.max(20, Math.round(165.0F * fade));
                int aTrim = Math.max(25, Math.round(220.0F * fade));

                VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
                poseStack.pushPose();
                poseStack.translate(cx, cy, cz);
                Matrix4f mat = poseStack.last().pose();

                int floorSegs = 32;
                float rOuter = radius * 0.96F;
                float rInner = radius * 0.88F;
                float rMid1 = radius * 0.52F;
                float rMid0 = radius * 0.46F;
                for (int i = 0; i < floorSegs; i++) {
                    float a0 = (float) (i * Math.PI * 2.0D / floorSegs) + timeSec * 0.25F;
                    float a1 = (float) ((i + 1) * Math.PI * 2.0D / floorSegs) + timeSec * 0.25F;
                    float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
                    float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            c0 * rInner, 0.05F, s0 * rInner,
                            c0 * rOuter, 0.05F, s0 * rOuter,
                            c1 * rOuter, 0.05F, s1 * rOuter,
                            c1 * rInner, 0.05F, s1 * rInner,
                            85, 255, 130, aMain);
                }

                int pillars = 8;
                float pRadius = radius * 0.86F;
                float pillarH = 9.3F; // Task 12: Scaled 1.5x (from 6.2F -> 9.3F)
                float domeApexY = 16.2F; // Task 12: Scaled 1.5x (from 10.8F -> 16.2F)
                for (int p = 0; p < pillars; p++) {
                    float ang = (float) (p * Math.PI * 2.0D / pillars);
                    float nextAng = (float) ((p + 1) * Math.PI * 2.0D / pillars);
                    float midAng = (ang + nextAng) * 0.5F;
                    float px = Mth.cos(ang) * pRadius;
                    float pz = Mth.sin(ang) * pRadius;
                    float nx = Mth.cos(nextAng) * pRadius;
                    float nz = Mth.sin(nextAng) * pRadius;
                    float mx = Mth.cos(midAng) * pRadius;
                    float mz = Mth.sin(midAng) * pRadius;

                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.54F, 0.02F, pz - 0.54F, px + 0.54F, 0.72F, pz + 0.54F, 240, 205, 85, aTrim);
                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.33F, 0.72F, pz - 0.33F, px + 0.33F, pillarH - 0.63F, pz + 0.33F, 68, 235, 115, aMain);
                    ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, px - 0.57F, pillarH - 0.63F, pz - 0.57F, px + 0.57F, pillarH, pz + 0.57F, 245, 215, 95, aTrim);

                    float archPeakY = pillarH + 2.025F;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            px, pillarH, pz,
                            mx, archPeakY, mz,
                            mx, archPeakY + 0.33F, mz,
                            px, pillarH + 0.33F, pz,
                            240, 212, 92, aTrim);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx, archPeakY, mz,
                            nx, pillarH, nz,
                            nx, pillarH + 0.33F, nz,
                            mx, archPeakY + 0.33F, mz,
                            240, 212, 92, aTrim);

                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx - 0.18F, archPeakY - 0.675F, mz - 0.18F,
                            mx + 0.18F, archPeakY - 0.675F, mz + 0.18F,
                            mx + 0.03F, archPeakY, mz + 0.03F,
                            mx - 0.03F, archPeakY, mz - 0.03F,
                            95, 255, 145, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 0.975F, mz, 0.15F, 85, 255, 130, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 1.35F, mz, 0.12F, 120, 255, 160, aTrim);
                    WindGliderRenderer.renderFourLeafCloverXZ(vc, mat, mx, archPeakY - 1.725F, mz, 0.09F, 215, 255, 185, aTrim);

                    float midR = pRadius * 0.54F;
                    float midY = pillarH + 3.9F;
                    float mx0 = Mth.cos(ang) * midR, mz0 = Mth.sin(ang) * midR;
                    float mx1 = Mth.cos(nextAng) * midR, mz1 = Mth.sin(nextAng) * midR;
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            px, pillarH, pz,
                            nx, pillarH, nz,
                            mx1, midY, mz1,
                            mx0, midY, mz0,
                            65, 235, 115, Math.max(18, aMain / 2));
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx0, midY, mz0,
                            mx1, midY, mz1,
                            0.0F, domeApexY, 0.0F,
                            0.0F, domeApexY, 0.0F,
                            95, 255, 145, Math.max(22, (aMain * 2) / 3));
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            px - 0.12F, pillarH, pz - 0.12F,
                            px + 0.12F, pillarH, pz + 0.12F,
                            mx0 + 0.09F, midY, mz0 + 0.09F,
                            mx0 - 0.09F, midY, mz0 - 0.09F,
                            250, 225, 110, aTrim);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,
                            mx0 - 0.09F, midY, mz0 - 0.09F,
                            mx0 + 0.09F, midY, mz0 + 0.09F,
                            0.06F, domeApexY, 0.06F,
                            -0.06F, domeApexY, -0.06F,
                            250, 225, 110, aTrim);
                }

                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.36F, domeApexY - 0.225F, -0.36F, 0.36F, domeApexY + 2.175F, 0.36F, 245, 220, 95, aTrim);
                ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.18F, domeApexY + 2.175F, -0.18F, 0.18F, domeApexY + 3.525F, 0.18F, 120, 255, 155, aTrim);
                poseStack.popPose();

                VertexConsumer skVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
                poseStack.pushPose();
                poseStack.translate(cx, cy + domeApexY - 2.025F + Mth.sin(timeSec * 2.2F) * 0.22F, cz);
                poseStack.mulPose(Axis.YP.rotationDegrees(timeSec * 45.0F));
                float cloverHalf = 1.725F;
                for (int plane = 0; plane < 2; plane++) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(plane * 90.0F));
                    PoseStack.Pose p = poseStack.last();
                    WindGliderRenderer.emitWingQuad(skVc, p, -cloverHalf, -cloverHalf, 0.0F, 0.0F, 1.0F, cloverHalf, -cloverHalf, 0.0F, 1.0F, 1.0F, cloverHalf, cloverHalf, 0.0F, 1.0F, 0.0F, -cloverHalf, cloverHalf, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                    WindGliderRenderer.emitWingQuad(skVc, p, -cloverHalf, cloverHalf, 0.0F, 0.0F, 0.0F, cloverHalf, cloverHalf, 0.0F, 1.0F, 0.0F, cloverHalf, -cloverHalf, 0.0F, 1.0F, 1.0F, -cloverHalf, -cloverHalf, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                    poseStack.popPose();
                }
                poseStack.popPose();
            } else if (arc.fromEntityId == -8 && (arc.color & 0xFFFF0000) == 0x4E430000) {
                // Nahida Charged Attack (world-anchored 3x3x3 box 1 block in front, visible in 1st & 3rd person)
                float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
                float caProg = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
                float cx = (float) (arc.end.x - camPos.x);
                float cy = (float) (arc.end.y - camPos.y);
                float cz = (float) (arc.end.z - camPos.z);

                poseStack.pushPose();
                poseStack.translate(cx, cy, cz);
                if (caProg < 0.62F) {
                    // Phase 1: Box opens and grows larger until explosion (1.5x scaled frame)
                    float boxScale = 0.22F + 1.45F * WindGliderRenderer.smoothStep01(caProg / 0.62F);
                    float h = 1.15F * boxScale;
                    VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
                    Matrix4f mat = poseStack.last().pose();
                    int faceA = 48;
                    WindGliderRenderer.emitDoubleQuad(vc, mat, -h, -h, -h,  h, -h, -h,  h,  h, -h, -h,  h, -h, 130, 230, 68, faceA);
                    WindGliderRenderer.emitDoubleQuad(vc, mat, -h, -h,  h,  h, -h,  h,  h,  h,  h, -h,  h,  h, 130, 230, 68, faceA);
                    WindGliderRenderer.emitDoubleQuad(vc, mat, -h, -h, -h, -h, -h,  h, -h,  h,  h, -h,  h, -h, 120, 220, 64, faceA);
                    WindGliderRenderer.emitDoubleQuad(vc, mat,  h, -h, -h,  h, -h,  h,  h,  h,  h,  h,  h, -h, 120, 220, 64, faceA);
                    WindGliderRenderer.emitDoubleQuad(vc, mat, -h,  h, -h,  h,  h, -h,  h,  h,  h, -h,  h,  h, 145, 242, 78, faceA);
                    WindGliderRenderer.emitDoubleQuad(vc, mat, -h, -h, -h,  h, -h, -h,  h, -h,  h, -h, -h,  h, 110, 210, 58, faceA);
                    float tEdge = 0.028F;
                    float[][] edges = {
                            {-h,-h,-h, h,-h,-h}, {-h, h,-h, h, h,-h}, {-h,-h, h, h,-h, h}, {-h, h, h, h, h, h},
                            {-h,-h,-h,-h, h,-h}, { h,-h,-h, h, h,-h}, {-h,-h, h,-h, h, h}, { h,-h, h, h, h, h},
                            {-h,-h,-h,-h,-h, h}, { h,-h,-h, h,-h, h}, {-h, h,-h,-h, h, h}, { h, h,-h, h, h, h}
                    };
                    for (float[] e : edges) {
                        boolean isY = Math.abs(e[4] - e[1]) > 0.01F;
                        float dxO = isY ? tEdge : 0.0F;
                        float dyO = isY ? 0.0F : tEdge;
                        WindGliderRenderer.emitDoubleQuad(vc, mat,
                                e[0]-dxO, e[1]-dyO, e[2], e[0]+dxO, e[1]+dyO, e[2],
                                e[3]+dxO, e[4]+dyO, e[5], e[3]-dxO, e[4]-dyO, e[5],
                                215, 255, 130, 238);
                        for (int s = 1; s <= 4; s++) {
                            float f = s * 0.20F;
                            float sx = Mth.lerp(f, e[0], e[3]);
                            float sy = Mth.lerp(f, e[1], e[4]);
                            float sz = Mth.lerp(f, e[2], e[5]);
                            WindGliderRenderer.renderCloverCross2D(vc, mat, sx, sy, sz, 0.06F * Math.max(0.45F, boxScale), 235, 255, 165, 240);
                        }
                    }
                    for (int dIdx = 0; dIdx < 5; dIdx++) {
                        float dx = ((dIdx % 3) - 1) * 0.45F * boxScale;
                        float dz = (dIdx < 2 ? -0.35F : 0.35F) * boxScale;
                        float dy = Mth.sin(caProg * 10.0F + dIdx * 1.4F) * 0.35F * boxScale;
                        WindGliderRenderer.renderCloverCross2D(vc, mat, dx, dy, dz, 0.09F * boxScale, 195, 255, 120, 210);
                    }
                    // Dotted glowing Dendro cursor ring at top-right corner
                    float cRad = 0.28F * Math.max(0.45F, boxScale);
                    for (int rIdx = 0; rIdx < 18; rIdx++) {
                        float ra0 = (float) (rIdx * Math.PI * 2.0D / 18.0D + caProg * 3.0F);
                        WindGliderRenderer.renderCloverCross2D(vc, mat, h + Mth.cos(ra0) * cRad, h + Mth.sin(ra0) * cRad, -h, 0.028F, 210, 255, 145, 240);
                    }
                    WindGliderRenderer.renderFourLeafClover2D(vc, mat, h, h, -h - 0.01F, 0.13F * Math.max(0.45F, boxScale), 225, 255, 130, 245);
                } else {
                    // Phase 2: Fast explosion using seed_of_skandha.png, growing larger and 50% more transparent
                    float boom = (caProg - 0.62F) / 0.38F;
                    float expHalf = 0.65F + boom * 1.65F;
                    int expAlpha = Math.max(4, Math.round(125.0F * (1.0F - boom)));
                    VertexConsumer skVc = bufferSource.getBuffer(RenderType.entityTranslucent(skandhaTex));
                    for (int plane = 0; plane < 2; plane++) {
                        poseStack.pushPose();
                        poseStack.mulPose(Axis.YP.rotationDegrees(plane * 90.0F + boom * 35.0F));
                        PoseStack.Pose p = poseStack.last();
                        emitTexQuadAlpha(skVc, p, -expHalf, -expHalf, 0.0F, 0.0F, 1.0F, expHalf, -expHalf, 0.0F, 1.0F, 1.0F, expHalf, expHalf, 0.0F, 1.0F, 0.0F, -expHalf, expHalf, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, expAlpha);
                        emitTexQuadAlpha(skVc, p, -expHalf, expHalf, 0.0F, 0.0F, 0.0F, expHalf, expHalf, 0.0F, 1.0F, 0.0F, expHalf, -expHalf, 0.0F, 1.0F, 1.0F, -expHalf, -expHalf, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, expAlpha);
                        poseStack.popPose();
                    }
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    PoseStack.Pose ph = poseStack.last();
                    emitTexQuadAlpha(skVc, ph, -expHalf, -expHalf, 0.0F, 0.0F, 1.0F, expHalf, -expHalf, 0.0F, 1.0F, 1.0F, expHalf, expHalf, 0.0F, 1.0F, 0.0F, -expHalf, expHalf, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, expAlpha);
                    emitTexQuadAlpha(skVc, ph, -expHalf, expHalf, 0.0F, 0.0F, 0.0F, expHalf, expHalf, 0.0F, 1.0F, 0.0F, expHalf, -expHalf, 0.0F, 1.0F, 1.0F, -expHalf, -expHalf, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, expAlpha);
                    poseStack.popPose();
                }
                poseStack.popPose();
            } else if (arc.fromEntityId == -8 && (arc.color & 0xFFFF0000) == 0x4E440000) {
                // Nahida Tap E: Sweeps from knee (0.45) to neck (1.45) across >half-circle in front, dim -> bright, varied-size 4-leaf seeds
                float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
                float eProg = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
                float fadeOut = eProg > 0.78F ? Mth.clamp((1.0F - eProg) / 0.22F, 0.0F, 1.0F) : 1.0F;
                Vec3 fwd = arc.end.subtract(arc.start);
                Vec3 hFwd = new Vec3(fwd.x, 0.0D, fwd.z);
                if (hFwd.lengthSqr() < 1.0E-4D) hFwd = new Vec3(0.0D, 0.0D, 1.0D);
                else hFwd = hFwd.normalize();
                double baseAngle = Math.atan2(hFwd.z, hFwd.x);
                double arcSpan = Math.PI * 2.0D; // full 360-degree sweep matching body spin
                double startAng = baseAngle + arcSpan * 0.5D; // reversed sweep direction
                float eRadius = 4.0F;

                poseStack.pushPose();
                poseStack.translate((float) (arc.start.x - camPos.x), (float) (arc.start.y - camPos.y), (float) (arc.start.z - camPos.z));
                VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
                Matrix4f eMat = poseStack.last().pose();

                int segs = 28;
                float sweepFrac = Mth.clamp(eProg / 0.62F, 0.0F, 1.0F);
                float overallReveal = Mth.clamp(eProg / 0.35F, 0.25F, 1.0F);
                int activeSegs = Math.max(1, Math.round(segs * sweepFrac));
                for (int s = 0; s < activeSegs; s++) {
                    float f0 = s / (float) segs;
                    float f1 = (s + 1) / (float) segs;
                    double a0 = startAng - f0 * arcSpan;
                    double a1 = startAng - f1 * arcSpan;
                    float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                    float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                    // Tail rises higher than head (knee 0.42F -> above neck 1.55F)
                    float y0 = Mth.lerp(f0, 0.42F, 1.55F);
                    float y1 = Mth.lerp(f1, 0.42F, 1.55F);
                    float w0 = 0.06F + 0.15F * f0;
                    float w1 = 0.06F + 0.15F * f1;
                    // Dim at start -> bright & clear later
                    int aTrail = Math.max(8, Math.round(Mth.lerp(f0, 18.0F, 240.0F) * overallReveal * fadeOut));
                    int aCore = Math.max(12, Math.round(Mth.lerp(f0, 26.0F, 255.0F) * overallReveal * fadeOut));
                    WindGliderRenderer.emitDoubleQuad(vc, eMat,
                            c0 * (eRadius - w0), y0, s0 * (eRadius - w0),
                            c0 * (eRadius + w0), y0, s0 * (eRadius + w0),
                            c1 * (eRadius + w1), y1, s1 * (eRadius + w1),
                            c1 * (eRadius - w1), y1, s1 * (eRadius - w1),
                            90, 250, 128, aTrail);
                    WindGliderRenderer.emitDoubleQuad(vc, eMat,
                            c0 * (eRadius - 0.03F), y0 + 0.015F, s0 * (eRadius - 0.03F),
                            c0 * (eRadius + 0.03F), y0 + 0.015F, s0 * (eRadius + 0.03F),
                            c1 * (eRadius + 0.03F), y1 + 0.015F, s1 * (eRadius + 0.03F),
                            c1 * (eRadius - 0.03F), y1 + 0.015F, s1 * (eRadius - 0.03F),
                            220, 255, 175, aCore);
                }
                // Varied large & small 4-leaf seed_of_skandha particles both along the trail AND scattered around it
                float[][] seedSpecs = {
                        // {frac, radiusOffset, yOffset, size}
                        {0.10F,  0.00F,  0.04F, 0.12F},
                        {0.24F,  0.00F, -0.04F, 0.34F},
                        {0.39F,  0.00F,  0.05F, 0.14F},
                        {0.54F,  0.00F, -0.03F, 0.40F},
                        {0.70F,  0.00F,  0.06F, 0.15F},
                        {0.85F,  0.00F, -0.04F, 0.32F},
                        {0.96F,  0.00F,  0.04F, 0.22F},
                        // Surrounding scattered seeds (to & nho xung quanh)
                        {0.18F,  0.42F,  0.22F, 0.11F},
                        {0.32F, -0.45F, -0.18F, 0.26F},
                        {0.48F,  0.52F, -0.24F, 0.13F},
                        {0.62F, -0.38F,  0.28F, 0.29F},
                        {0.76F,  0.48F,  0.20F, 0.16F},
                        {0.90F, -0.50F, -0.16F, 0.22F}
                };
                VertexConsumer skVc = bufferSource.getBuffer(RenderType.entityTranslucent(skandhaTex));
                for (float[] sp : seedSpecs) {
                    if (sp[0] > sweepFrac) continue;
                    double ka = startAng - sp[0] * arcSpan;
                    float rad = eRadius + sp[1];
                    float kx = (float) Math.cos(ka) * rad;
                    float kz = (float) Math.sin(ka) * rad;
                    float ky = Mth.lerp(sp[0], 0.42F, 1.55F) + sp[2];
                    float sz = sp[3];
                    int sAlpha = Math.max(16, Math.round(Mth.lerp(sp[0], 60.0F, 245.0F) * overallReveal * fadeOut));
                    poseStack.pushPose();
                    poseStack.translate(kx, ky, kz);
                    poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                    PoseStack.Pose p = poseStack.last();
                    emitTexQuadAlpha(skVc, p, -sz, -sz, 0.0F, 0.0F, 1.0F, sz, -sz, 0.0F, 1.0F, 1.0F, sz, sz, 0.0F, 1.0F, 0.0F, -sz, sz, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, sAlpha);
                    poseStack.popPose();
                }
                poseStack.popPose();
            } else if (arc.fromEntityId == -8 && (arc.color & 0xFFFF0000) == 0x4E410000) {
                renderNahidaNormalAttackWorldVfx(poseStack, bufferSource, camPos, arc, now, skandhaTex);
            } else if (arc.fromEntityId == -8 && (arc.color & 0xFFFF0000) == 0x4E420000) {
                renderNahidaShrineFootstepGrass(poseStack, bufferSource, camPos, arc, now);
            } else if (arc.fromEntityId == -8) {
                float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
                float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
                float fade = 1.0F - prog * 0.65F;
                Vec3 sCam = arc.start.subtract(camPos);
                Vec3 eCam = arc.end.subtract(camPos);
                Vec3 chord = eCam.subtract(sCam);
                if (chord.lengthSqr() > 1.0E-4D) {
                    VertexConsumer linkVc = bufferSource.getBuffer(RenderType.lightning());
                    Matrix4f linkMat = poseStack.last().pose();
                    Vec3 n = new Vec3(0.0D, 1.0D, 0.0D);
                    ElementalReactionBadgeRenderer.drawQuadRibbon(linkVc, linkMat, sCam, eCam, n, 0.042D, 75, 245, 115, Math.max(25, Math.round(210 * fade)));
                    ElementalReactionBadgeRenderer.drawQuadRibbon(linkVc, linkMat, sCam, eCam, n, 0.016D, 220, 255, 225, Math.max(35, Math.round(245 * fade)));
                }
                VertexConsumer skVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skandhaTex));
                Vec3 midCam = sCam.lerp(eCam, 0.5D);
                for (Vec3 node : new Vec3[]{sCam, midCam, eCam}) {
                    poseStack.pushPose();
                    poseStack.translate(node.x, node.y, node.z);
                    poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                    float h = 0.28F;
                    PoseStack.Pose p = poseStack.last();
                    WindGliderRenderer.emitWingQuad(skVc, p, -h, -h, 0.0F, 0.0F, 1.0F, h, -h, 0.0F, 1.0F, 1.0F, h, h, 0.0F, 1.0F, 0.0F, -h, h, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                    WindGliderRenderer.emitWingQuad(skVc, p, -h, h, 0.0F, 0.0F, 0.0F, h, h, 0.0F, 1.0F, 0.0F, h, -h, 0.0F, 1.0F, 1.0F, -h, -h, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
                    poseStack.popPose();
                }
            }
        }
    }
    private static void emitTexQuadAlpha(
            VertexConsumer vc, PoseStack.Pose pose,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz, int alpha
    ) {
        Matrix4f mat = pose.pose();
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        vc.vertex(mat, x0, y0, z0).color(255, 255, 255, alpha).uv(u0, v0).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(pose.normal(), nx, ny, nz).endVertex();
        vc.vertex(mat, x1, y1, z1).color(255, 255, 255, alpha).uv(u1, v1).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(pose.normal(), nx, ny, nz).endVertex();
        vc.vertex(mat, x2, y2, z2).color(255, 255, 255, alpha).uv(u2, v2).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(pose.normal(), nx, ny, nz).endVertex();
        vc.vertex(mat, x3, y3, z3).color(255, 255, 255, alpha).uv(u3, v3).overlayCoords(overlay).uv2(WindGliderRenderer.FULL_BRIGHT).normal(pose.normal(), nx, ny, nz).endVertex();
    }

    public static float getWeaponFadeAlpha(LivingEntity entity, ItemStack stack) {
        if (entity == null || stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArcaneStaffItem)) return 1.0F;
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) return 1.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        if (d == null || d.nahidaWeaponHideUntil <= 0L) return 1.0F;
        long now = System.currentTimeMillis();
        if (now < d.nahidaWeaponHideUntil) return 0.0F;
        long elapsed = now - d.nahidaWeaponHideUntil;
        if (elapsed >= 500L) return 1.0F;
        return Mth.clamp(WindGliderRenderer.smoothStep01(elapsed / 500.0F), 0.02F, 1.0F);
    }

    public static MultiBufferSource wrapTranslucentBuffer(MultiBufferSource delegate, float alpha) {
        return renderType -> new AlphaVertexConsumer(
                delegate.getBuffer(RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)),
                alpha
        );
    }

    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        private AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(r, g, b, Mth.clamp(Math.round(a * alpha), 0, 255));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, Mth.clamp(Math.round(a * alpha), 0, 255));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
