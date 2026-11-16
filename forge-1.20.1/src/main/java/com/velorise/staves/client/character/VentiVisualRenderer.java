package com.velorise.staves.client.character;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.client.reaction.ElementalReactionBadgeRenderer;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class VentiVisualRenderer {
    private VentiVisualRenderer() {}

    public static boolean hasDaybreakEquipped(LivingEntity entity) {
        return ArcaneStaffItem.getEffectiveKind(entity.getMainHandItem()) == ArcaneStaffItem.Kind.DAYBREAK
                || ArcaneStaffItem.getEffectiveKind(entity.getOffhandItem()) == ArcaneStaffItem.Kind.DAYBREAK;
    }

    public static float getDaybreakPullProgress(LivingEntity entity, float partialTick) {
        if (entity == null) return 0.0F;
        if (entity.isUsingItem()) {
            ItemStack useStack = entity.getUseItem();
            if (useStack.getItem() instanceof ArcaneStaffItem s
                    && ArcaneStaffItem.getEffectiveKind(useStack) == ArcaneStaffItem.Kind.DAYBREAK) {
                int elapsed = Math.max(0, s.getUseDuration(useStack) - entity.getUseItemRemainingTicks());
                return Mth.clamp((elapsed + partialTick) / 20.0F, 0.0F, 1.0F);
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && hasDaybreakEquipped(entity)) {
            int ticks = Math.max(ClientAuraTracker.localDaybreakBowPullTicks, ClientAuraTracker.ventiUltiHoldTicks);
            if (ticks > 0) {
                return Mth.clamp((ticks + partialTick) / 16.0F, 0.0F, 1.0F);
            }
        }
        return 0.0F;
    }

    public static boolean isDaybreakPulling(LivingEntity entity, ItemStack stack) {
        if (entity == null || stack == null || stack.isEmpty()) return false;
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.DAYBREAK) return false;
        if (entity.isUsingItem() && entity.getUseItem() == stack) return true;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && entity.getId() == mc.player.getId()
                && (ClientAuraTracker.localDaybreakBowPullTicks > 0 || ClientAuraTracker.ventiUltiHoldTicks > 0)
                && (entity.getMainHandItem() == stack || entity.getOffhandItem() == stack);
    }

    public static float getDaybreakPullProgress(LivingEntity entity, ItemStack stack) {
        return isDaybreakPulling(entity, stack) ? getDaybreakPullProgress(entity, 0.0F) : 0.0F;
    }

    public static void applyDaybreakBowHumanoidPose(HumanoidModel<?> model, HumanoidArm bowArm, float pull01) {
        ModelPart bowPart = bowArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart drawPart = bowArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        float bowDir = bowArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        bowPart.yRot = -bowDir * 0.10F + model.head.yRot;
        bowPart.xRot = -1.48F + model.head.xRot;
        bowPart.zRot = 0.0F;

        drawPart.yRot = bowDir * Mth.lerp(pull01, 0.55F, 0.72F) + model.head.yRot * 0.65F;
        drawPart.xRot = -1.48F + model.head.xRot + 0.36F * pull01;
        drawPart.zRot = -bowDir * 0.08F;
    }

    public static boolean applyBowPullHumanoidPose(LivingEntity entity, HumanoidModel<?> model) {
        Minecraft mcLocal = Minecraft.getInstance();
        if (mcLocal.player != null && entity.getId() == mcLocal.player.getId()
                && ClientAuraTracker.localDaybreakBowPullTicks > 0
                && hasDaybreakEquipped(entity)) {
            float pull01 = Mth.clamp(ClientAuraTracker.localDaybreakBowPullTicks / 16.0F, 0.0F, 1.0F);
            applyDaybreakBowHumanoidPose(model, entity.getMainArm(), pull01);
            return true;
        }
        return false;
    }

    public static boolean applyBurstHumanoidAnimation(HumanoidModel<?> model, int activeAnim, float t) {
        if (activeAnim != ClientAuraTracker.ANIM_VENTI_BURST) return false;
        float w = (t < 0.20F) ? WindGliderRenderer.smoothStep01(t / 0.20F) : (t < 0.78F ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.78F) / 0.22F));
        model.body.xRot = -0.12F * w;
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -2.15F);
        model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.95F);
        model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.28F);
        model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.35F);
        model.head.xRot -= 0.18F * w;
        model.hat.copyFrom(model.head);
        return true;
    }

    public static void renderDaybreakNockedWindCharge(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        if (ArcaneStaffItem.getEffectiveKind(stack) != ArcaneStaffItem.Kind.DAYBREAK) return;
        boolean isFirst = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean isThird = displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (!isFirst && !isThird) return;

        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getFrameTime();
        float pull01 = getDaybreakPullProgress(entity, pt);
        if (pull01 <= 0.01F) return;

        poseStack.pushPose();
        float side = leftHand ? -1.0F : 1.0F;
        if (isFirst) {
            poseStack.translate(side * (1.13F / 16.0F), 3.2F / 16.0F, 1.13F / 16.0F);
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                    0.0F,
                    (float) Math.toRadians(-90.0F),
                    (float) Math.toRadians(25.0F)
            ));
            poseStack.scale(0.68F, 0.68F, 0.68F);
        } else {
            poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                    0.0F,
                    (float) Math.toRadians(-90.0F),
                    (float) Math.toRadians(55.0F)
            ));
            poseStack.scale(0.85F, 0.85F, 0.85F);
        }

        Vec3 midStr = new Vec3(0.06D, 0.0D, 0.0D);
        Vec3 nockPt = midStr.add(0.24D * pull01, -0.24D * pull01, 0.0D);

        VertexConsumer lightVc = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();

        float chargeR = 0.055F + 0.075F * pull01;
        WindGliderRenderer.drawTranslucentSphere(lightVc, mat, (float) nockPt.x, (float) nockPt.y, (float) nockPt.z,
                chargeR * 0.55F, 8, 12, 225, 255, 250, Math.round(170 + 75 * pull01));
        WindGliderRenderer.drawTranslucentSphere(lightVc, mat, (float) nockPt.x, (float) nockPt.y, (float) nockPt.z,
                chargeR, 8, 12, 70, 225, 205, Math.round(90 + 85 * pull01));

        float time = (entity.tickCount + pt) * 0.16F;
        poseStack.translate(nockPt.x, nockPt.y, nockPt.z);
        for (int ring = 0; ring < 2; ring++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(ring == 0 ? 48.0F : -52.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees((time * 95.0F) * (ring == 0 ? 1.0F : -1.35F)));
            Matrix4f rm = poseStack.last().pose();
            float rr = chargeR * (1.25F + ring * 0.25F);
            for (int seg = 0; seg < 10; seg++) {
                float a0 = (float) (2.0 * Math.PI * seg / 10.0);
                float a1 = (float) (2.0 * Math.PI * (seg + 1) / 10.0);
                Vec3 p0 = new Vec3(Mth.cos(a0) * rr, Mth.sin(a0 * 2.0F) * rr * 0.18F, Mth.sin(a0) * rr);
                Vec3 p1 = new Vec3(Mth.cos(a1) * rr, Mth.sin(a1 * 2.0F) * rr * 0.18F, Mth.sin(a1) * rr);
                ElementalReactionBadgeRenderer.drawQuadRibbon(lightVc, rm, p0, p1, new Vec3(0.0D, 1.0D, 0.0D), 0.008D + 0.006D * pull01, 150, 255, 238, Math.round(130 + 95 * pull01));
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void renderVentiWindChargeOrbs(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) return;
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId != -2 || arc.start == null || arc.end == null) continue;
            float baseRadius = (float) Math.max(1.0D, arc.end.x);
            float hcx = (float) (arc.start.x - camPos.x);
            float hcy = (float) (arc.start.y - camPos.y) - 1.0F;
            float hcz = (float) (arc.start.z - camPos.z);
            int r = 175, g = 255, b = 232;
            if (arc.color != 0 && arc.color != 0xFF66FFD8) {
                r = (arc.color >> 16) & 0xFF;
                g = (arc.color >> 8) & 0xFF;
                b = arc.color & 0xFF;
            }
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                    poseStack, lineConsumer,
                    hcx - baseRadius, hcy - baseRadius, hcz - baseRadius,
                    hcx + baseRadius, hcy + baseRadius, hcz + baseRadius,
                    1.0F, 1.0F, 1.0F, 1.0F);
            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            org.joml.Matrix3f normMat = pose.normal();
            int segs = 36;
            for (int plane = 0; plane < 3; plane++) {
                for (int i = 0; i < segs; i++) {
                    float a0 = (float) (2.0 * Math.PI * i / segs);
                    float a1 = (float) (2.0 * Math.PI * (i + 1) / segs);
                    float x0 = hcx, y0 = hcy, z0 = hcz;
                    float x1 = hcx, y1 = hcy, z1 = hcz;
                    if (plane == 0) {
                        x0 += Mth.cos(a0) * baseRadius; z0 += Mth.sin(a0) * baseRadius;
                        x1 += Mth.cos(a1) * baseRadius; z1 += Mth.sin(a1) * baseRadius;
                    } else if (plane == 1) {
                        x0 += Mth.cos(a0) * baseRadius; y0 += Mth.sin(a0) * baseRadius;
                        x1 += Mth.cos(a1) * baseRadius; y1 += Mth.sin(a1) * baseRadius;
                    } else {
                        y0 += Mth.cos(a0) * baseRadius; z0 += Mth.sin(a0) * baseRadius;
                        y1 += Mth.cos(a1) * baseRadius; z1 += Mth.sin(a1) * baseRadius;
                    }
                    float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
                    float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                    if (len > 1.0E-5F) {
                        dx /= len; dy /= len; dz /= len;
                    }
                    lineConsumer.vertex(mat, x0, y0, z0).color(r, g, b, 255).normal(normMat, dx, dy, dz).endVertex();
                    lineConsumer.vertex(mat, x1, y1, z1).color(r, g, b, 255).normal(normMat, dx, dy, dz).endVertex();
                }
            }
            if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderType.lines());
            }
        }
    }

    public static boolean renderVortexStreamArc(
            ClientAuraTracker.ActiveArc arc,
            Vec3 startPos,
            Vec3 endPos,
            Vec3 camPos,
            long now,
            VertexConsumer consumer,
            Matrix4f matrix
    ) {
        if (arc.fromEntityId != -2) return false;
        float baseRadius = (float) Math.max(1.0D, endPos.x);
        float fadeIn = Mth.clamp((now - arc.startTime) / 220.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((arc.expireTime - now) / 320.0F, 0.0F, 1.0F);
        float fade = Math.min(fadeIn, fadeOut);
        float timeSec = (now - arc.startTime) * 0.001F;
        Vec3 cCam = startPos.subtract(camPos);
        int streamAlpha = Math.max(20, Math.round(205.0F * fade));
        int coreAlpha = Math.max(30, Math.round(240.0F * fade));
        int sr = 95, sg = 240, sb = 215;
        int cr = 225, cg = 255, cb = 248;
        if (arc.color != 0 && arc.color != 0xFF66FFD8) {
            sr = (arc.color >> 16) & 0xFF;
            sg = (arc.color >> 8) & 0xFF;
            sb = arc.color & 0xFF;
            cr = (sr + 255) / 2;
            cg = (sg + 255) / 2;
            cb = (sb + 255) / 2;
        }
        for (int s = 0; s < 6; s++) {
            float yawTilt = s * 1.0472F + timeSec * (0.45F + s * 0.11F);
            float pitchTilt = -0.95F + (s % 3) * 0.65F + Mth.sin(timeSec * 1.3F + s) * 0.25F;
            float cy = Mth.cos(yawTilt), sy = Mth.sin(yawTilt);
            float cp = Mth.cos(pitchTilt), sp = Mth.sin(pitchTilt);
            Vec3 uAxis = new Vec3(cy, 0.0D, sy);
            Vec3 nAxis = new Vec3(-sy * sp, cp, cy * sp);
            Vec3 vAxis = nAxis.cross(uAxis).normalize();
            double rStream = baseRadius * (0.36D + 0.11D * s);
            float phaseStart = timeSec * (3.6F + s * 0.7F) * ((s % 2 == 0) ? 1.0F : -1.0F) + s * 1.9F;
            float sweepRad = 2.85F;
            Vec3 prev = null;
            for (int seg = 0; seg <= 9; seg++) {
                float f = seg / 9.0F;
                float ang = phaseStart + f * sweepRad;
                double waveR = rStream * (1.0D + 0.16D * Mth.sin(ang * 2.5F + timeSec * 5.0F + s));
                double waveH = baseRadius * 0.16D * Mth.cos(ang * 1.8F - timeSec * 4.2F);
                Vec3 pt = cCam
                        .add(uAxis.scale(Math.cos(ang) * waveR))
                        .add(vAxis.scale(Math.sin(ang) * waveR))
                        .add(nAxis.scale(waveH));
                if (prev != null) {
                    ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, prev, pt, nAxis, 0.045D + 0.020D * Math.sin(f * Math.PI), sr, sg, sb, streamAlpha);
                    ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, prev, pt, uAxis, 0.018D, cr, cg, cb, coreAlpha);
                }
                prev = pt;
            }
        }
        return true;
    }
}
