package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.client.reaction.ElementalReactionBadgeRenderer;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dedicated visual renderer for Neuvillette (Guardian Lens):
 * - Sourcewater Droplets & Equitable Judgment charging ring HUD
 * - Hydro cannon beam vortex & converging water seal rings
 * - Equitable Judgment channeling, Skill E, and Burst humanoid & first-person poses
 */
public final class NeuvilletteVisualRenderer {
    private NeuvilletteVisualRenderer() {}

    public static boolean renderChannelingHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !ClientAuraTracker.isNeuvilletteChargingOrFiring(mc.player.getId())) return false;
        ClientAuraTracker.AuraDisplayData nd = ClientAuraTracker.get(mc.player.getId());
        if (nd == null) return false;
        float pct = nd.neuvilletteFiringBeam
                ? Mth.clamp(1.0F - (nd.neuvilletteBeamElapsed / 60.0F), 0.05F, 1.0F)
                : Mth.clamp(nd.neuvilletteChargeProgress / 60.0F, 0.0F, 1.0F);
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        float cx = sw * 0.5F;
        float cy = sh * 0.5F;
        Matrix4f mat = guiGraphics.pose().last().pose();
        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 12.0F, 15.0F, 0.0F, 360.0F, 32, 10, 26, 48, 145);
        if (pct > 0.005F) {
            float halfDeg = 180.0F * pct;
            int segs = Math.max(2, Math.round(16 * pct));
            int r = nd.neuvilletteFiringBeam ? 175 : 85;
            int g = nd.neuvilletteFiringBeam ? 250 : 225;
            int b = 255;
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 12.3F, 14.7F, 90.0F, 90.0F + halfDeg, segs, r, g, b, 240);
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 12.3F, 14.7F, 90.0F - halfDeg, 90.0F, segs, r, g, b, 240);
        }
        guiGraphics.flush();
        return true;
    }

    public static void renderUseChargeHud(GuiGraphics guiGraphics, int sw, int sh, float pct) {
        float cx = sw * 0.5F;
        float cy = sh * 0.5F;
        Matrix4f mat = guiGraphics.pose().last().pose();
        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 12.0F, 15.0F, 0.0F, 360.0F, 32, 10, 26, 48, 145);
        if (pct > 0.005F) {
            float halfDeg = 180.0F * pct;
            int segs = Math.max(2, Math.round(16 * pct));
            int r = pct >= 1.0F ? 175 : 85;
            int g = pct >= 1.0F ? 250 : 225;
            int b = 255;
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 12.3F, 14.7F, 90.0F, 90.0F + halfDeg, segs, r, g, b, 240);
            WindGliderRenderer.drawHudArc(vc, mat, cx, cy, 12.3F, 14.7F, 90.0F - halfDeg, 90.0F, segs, r, g, b, 240);
        }
        guiGraphics.flush();
    }

    public static boolean applyChannelingHumanoidPose(LivingEntity entity, HumanoidModel<?> model, int activeAnim) {
        if (ClientAuraTracker.isNeuvilletteChargingOrFiring(entity.getId()) && activeAnim != ClientAuraTracker.ANIM_NEUVILLETTE_BURST) {
            model.body.xRot = 0.0F;
            model.body.yRot = 0.0F;
            model.body.zRot = 0.0F;

            model.rightArm.xRot = -1.42F;
            model.rightArm.yRot = -0.15F;
            model.rightArm.zRot = 0.05F;

            model.leftArm.xRot = -1.15F;
            model.leftArm.yRot = 0.25F;
            model.leftArm.zRot = -0.05F;

            model.leftLeg.xRot = 0.05F;
            model.leftLeg.yRot = 0.0F;
            model.leftLeg.zRot = -0.05F;

            model.rightLeg.xRot = 0.05F;
            model.rightLeg.yRot = 0.0F;
            model.rightLeg.zRot = 0.05F;
            return true;
        }
        return false;
    }

    public static boolean applyActionHumanoidAnimation(HumanoidModel<?> model, int activeAnim, float t) {
        if (activeAnim == ClientAuraTracker.ANIM_NEUVILLETTE_SKILL_E) {
            float strike = Mth.sin(t * (float) Math.PI);
            model.rightArm.xRot = Mth.lerp(strike, model.rightArm.xRot, -1.65F);
            model.rightArm.yRot = Mth.lerp(strike, model.rightArm.yRot, -0.22F);
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_NEUVILLETTE_BURST) {
            float w = (t < 0.20F) ? WindGliderRenderer.smoothStep01(t / 0.20F) : (t < 0.80F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.80F) / 0.20F));
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -2.35F);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -2.15F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.42F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.42F);

            model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.22F);
            model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.22F);
            model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.05F);
            model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, 0.05F);
            return true;
        }
        return false;
    }

    public static boolean applyFirstPersonTransform(
            float dir,
            PoseStack poseStack,
            ClientAuraTracker.AuraDisplayData data,
            int activeAnim,
            long now
    ) {
        if (data != null && (activeAnim == ClientAuraTracker.ANIM_NEUVILLETTE_BURST || activeAnim == ClientAuraTracker.ANIM_NEUVILLETTE_SKILL_E)) {
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

    public static void renderGuardianHydroBeams(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 camPos = camera.getPosition();
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = null;

        for (net.minecraft.world.entity.player.Player entity : mc.level.players()) {
            if (!entity.isAlive() || entity.isSpectator()) continue;
            ClientAuraTracker.AuraDisplayData nd = ClientAuraTracker.get(entity.getId());
            boolean newStateActive = nd != null && System.currentTimeMillis() < nd.neuvilletteActiveUntil
                    && (nd.neuvilletteFiringBeam || nd.neuvilletteChargeProgress > 0);
            int elapsed;
            boolean firingBeam;
            boolean isNeth;
            if (newStateActive) {
                firingBeam = nd.neuvilletteFiringBeam;
                elapsed = firingBeam ? (60 + nd.neuvilletteBeamElapsed) : nd.neuvilletteChargeProgress;
                ItemStack main = entity.getMainHandItem();
                ItemStack off = entity.getOffhandItem();
                isNeth = (main.getItem() instanceof ArcaneStaffItem s1 && s1.isNetherite())
                        || (off.getItem() instanceof ArcaneStaffItem s2 && s2.isNetherite());
            } else {
                if (!entity.isUsingItem()) continue;
                ItemStack useStack = entity.getUseItem();
                if (!(useStack.getItem() instanceof ArcaneStaffItem s) || ArcaneStaffItem.getEffectiveKind(useStack) != ArcaneStaffItem.Kind.GUARDIAN) continue;
                int rawElapsed = Math.max(0, s.getUseDuration(useStack, entity) - entity.getUseItemRemainingTicks());
                boolean instant = ArcaneStaffItem.isGuardianInstant(useStack);
                firingBeam = instant || rawElapsed >= 60;
                elapsed = firingBeam ? (instant ? 60 + rawElapsed : rawElapsed) : rawElapsed;
                isNeth = s.isNetherite();
            }
            double maxRange = isNeth ? 22.0D : 16.0D;

            Vec3 eye = entity.getEyePosition(partialTicks);
            Vec3 look = entity.getViewVector(partialTicks).normalize();
            Vec3 worldUp = Math.abs(look.y) < 0.95D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 right = look.cross(worldUp).normalize();
            Vec3 up = right.cross(look).normalize();

            Vec3 muzzle = eye.add(look.scale(0.85D)).add(0.0D, -0.18D, 0.0D);
            if (consumer == null) {
                consumer = bufferSource.getBuffer(RenderType.lightning());
            }

            float time = (entity.tickCount + partialTicks) * 0.25F;
            double ringRadius = elapsed < 10 ? Mth.lerp(elapsed / 10.0F, 0.34F, 0.19F) : 0.19D;
            int ringSegs = 16;
            for (int i = 0; i < ringSegs; i++) {
                double a1 = (2.0D * Math.PI * i / ringSegs) + time;
                double a2 = (2.0D * Math.PI * (i + 1) / ringSegs) + time;
                Vec3 p1 = muzzle.add(right.scale(Math.cos(a1) * ringRadius)).add(up.scale(Math.sin(a1) * ringRadius)).subtract(camPos);
                Vec3 p2 = muzzle.add(right.scale(Math.cos(a2) * ringRadius)).add(up.scale(Math.sin(a2) * ringRadius)).subtract(camPos);
                ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, p1, p2, look, 0.020D, 75, 205, 255, 215);
            }

            if (!firingBeam) continue;

            Vec3 maxEnd = eye.add(look.scale(maxRange));
            HitResult blockHit = entity.level().clip(new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            Vec3 endPos = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : maxEnd;

            Vec3 a = muzzle.subtract(camPos);
            Vec3 b = endPos.subtract(camPos);
            Vec3 beamDir = b.subtract(a);
            double beamLen = beamDir.length();
            if (beamLen < 0.1D) continue;

            Vec3 toCam = a.scale(-1.0D);
            Vec3 normal = beamDir.cross(toCam);
            if (normal.lengthSqr() < 1e-6D) normal = up;
            else normal = normal.normalize();

            ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, a, b, normal, 0.075D, 45, 180, 255, 165);
            ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, a, b, normal, 0.028D, 220, 248, 255, 235);

            Vec3 unitDir = beamDir.scale(1.0D / beamLen);
            int spiralSteps = Math.max(8, (int) (beamLen * 2.5D));
            Vec3 prevSpiral = null;
            for (int s = 0; s <= spiralSteps; s++) {
                double dist = (beamLen * s) / spiralSteps;
                double angle = dist * 2.8D - time * 3.2D;
                double sr = 0.095D;
                Vec3 offset = right.scale(Math.cos(angle) * sr).add(up.scale(Math.sin(angle) * sr));
                Vec3 pt = a.add(unitDir.scale(dist)).add(offset);
                if (prevSpiral != null) {
                    ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, prevSpiral, pt, normal, 0.012D, 130, 225, 255, 175);
                }
                prevSpiral = pt;
            }
        }
        if (consumer != null && bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }
}
