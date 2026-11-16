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
        ClientAuraTracker.AuraDisplayData burstData = ClientAuraTracker.get(entity.getId());
        long nowMs = System.currentTimeMillis();
        if (burstData != null && nowMs < burstData.staffAnimUntil
                && burstData.staffAnimType == ClientAuraTracker.ANIM_VENTI_BURST
                && hasDaybreakEquipped(entity)) {
            float totalMs = Math.max(1.0F, (float) (burstData.staffAnimUntil - burstData.staffAnimStartTime));
            float t = Mth.clamp((nowMs - burstData.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            if (t < 0.78F) {
                return WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.65F));
            }
            return Math.max(0.0F, 1.0F - WindGliderRenderer.smoothStep01((t - 0.78F) / 0.14F));
        }
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
        ClientAuraTracker.AuraDisplayData burstData = ClientAuraTracker.get(entity.getId());
        if (burstData != null && System.currentTimeMillis() < burstData.staffAnimUntil
                && burstData.staffAnimType == ClientAuraTracker.ANIM_VENTI_BURST
                && (entity.getMainHandItem() == stack || entity.getOffhandItem() == stack)) {
            return true;
        }
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
        return applyBurstHumanoidAnimation(null, model, activeAnim, t);
    }

    public static boolean applyBurstHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model, int activeAnim, float t) {
        if (activeAnim != ClientAuraTracker.ANIM_VENTI_BURST) return false;
        HumanoidArm bowArm = entity != null
                ? WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.DAYBREAK)
                : HumanoidArm.RIGHT;
        ModelPart bowPart = bowArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart drawPart = bowArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        float bowDir = bowArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        if (t < 0.78F) {
            float stanceW = WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.20F));
            float pull01 = WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.65F));
            float tremor = Mth.sin(t * 52.0F) * 0.024F * pull01;

            model.body.xRot = -0.10F * stanceW;
            model.body.yRot = -bowDir * 0.32F * stanceW;
            model.leftLeg.xRot = Mth.lerp(stanceW, model.leftLeg.xRot, 0.18F);
            model.rightLeg.xRot = Mth.lerp(stanceW, model.rightLeg.xRot, -0.18F);
            model.leftLeg.zRot = Mth.lerp(stanceW, model.leftLeg.zRot, -0.14F);
            model.rightLeg.zRot = Mth.lerp(stanceW, model.rightLeg.zRot, 0.14F);

            bowPart.yRot = -bowDir * 0.12F + model.head.yRot;
            bowPart.xRot = Mth.lerp(stanceW, bowPart.xRot, -1.62F + model.head.xRot) + tremor;
            bowPart.zRot = -bowDir * 0.06F * stanceW;

            drawPart.yRot = bowDir * Mth.lerp(pull01, 0.42F, 0.78F) + model.head.yRot * 0.65F;
            drawPart.xRot = Mth.lerp(stanceW, drawPart.xRot, -1.58F + model.head.xRot + 0.38F * pull01) - tremor;
            drawPart.zRot = -bowDir * 0.10F * stanceW;

            model.head.xRot -= 0.08F * pull01;
            model.hat.copyFrom(model.head);
        } else {
            float rel = WindGliderRenderer.smoothStep01((t - 0.78F) / 0.22F);
            float fade = 1.0F - rel;
            model.body.xRot = -0.16F * fade;
            model.body.yRot = -bowDir * 0.20F * fade;
            model.leftLeg.xRot = Mth.lerp(fade, model.leftLeg.xRot, 0.18F);
            model.rightLeg.xRot = Mth.lerp(fade, model.rightLeg.xRot, -0.18F);
            model.leftLeg.zRot = Mth.lerp(fade, model.leftLeg.zRot, -0.14F);
            model.rightLeg.zRot = Mth.lerp(fade, model.rightLeg.zRot, 0.14F);

            bowPart.xRot = Mth.lerp(rel, -1.78F + model.head.xRot, -0.85F);
            bowPart.yRot = -bowDir * 0.18F * fade + model.head.yRot * fade;
            bowPart.zRot = -bowDir * 0.12F * fade;

            drawPart.xRot = Mth.lerp(rel, -1.20F + model.head.xRot, -0.45F);
            drawPart.yRot = bowDir * Mth.lerp(rel, 0.86F, 0.22F);
            drawPart.zRot = -bowDir * 0.18F * fade;

            model.head.xRot -= 0.12F * fade;
            model.hat.copyFrom(model.head);
        }
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
        if (effKind != ArcaneStaffItem.Kind.DAYBREAK) return false;
        if (activeAnim == ClientAuraTracker.ANIM_VENTI_BURST && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            if (t < 0.78F) {
                float pull01 = WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.65F));
                float tremor = Mth.sin(t * 55.0F) * 0.005F * pull01;
                poseStack.translate(-dir * 0.22F * pull01 + tremor, 0.10F * pull01, -0.18F * pull01);
                poseStack.mulPose(Axis.XP.rotationDegrees(-12.0F * pull01));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 14.0F * pull01));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * 8.0F * pull01));
            } else {
                float rel = WindGliderRenderer.smoothStep01((t - 0.78F) / 0.22F);
                float w = 1.0F - rel;
                poseStack.translate(-dir * 0.22F * w, (0.10F + 0.14F * Mth.sin(rel * (float) Math.PI)) * w, (-0.18F + 0.12F * rel) * w);
                poseStack.mulPose(Axis.XP.rotationDegrees((-12.0F + 26.0F * Mth.sin(rel * (float) Math.PI)) * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 14.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * 8.0F * w));
            }
            return true;
        }
        return false;
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

    private static float stepIdleFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.ventiIdleLastMs <= 0L) {
            d.ventiIdleLastMs = nowMs;
            if (d.ventiIdle) {
                d.ventiIdleFade = Math.min(1.0F, d.ventiIdleFade + 0.016F / 0.45F);
            }
        } else if (nowMs > d.ventiIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.ventiIdleLastMs) / 1000.0F);
            d.ventiIdleLastMs = nowMs;
            if (d.ventiIdle) {
                d.ventiIdleFade = Math.min(1.0F, d.ventiIdleFade + dt / 0.45F);
            } else {
                d.ventiIdleFade = Math.max(0.0F, d.ventiIdleFade - dt / 0.35F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.ventiIdleFade);
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (!ClientAuraTracker.isVentiIdle(entity.getId())) {
            ClientAuraTracker.AuraDisplayData dOff = ClientAuraTracker.get(entity.getId());
            if (dOff != null) dOff.ventiIdleYawSet = false;
            return false;
        }
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F) {
            if (d != null && !d.ventiIdle) d.ventiIdleYawSet = false;
            return false;
        }
        if (d != null) {
            if (!d.ventiIdleYawSet) {
                d.ventiIdleYaw = entity.yBodyRot;
                d.ventiIdleYawSet = true;
            }
            entity.yBodyRot = d.ventiIdleYaw;
            entity.yBodyRotO = d.ventiIdleYaw;
        }

        float strum = Mth.sin(ageInTicks * 0.28F);
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        // Left hand cradles the Holy Lyre in front of chest
        model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.05F);
        model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.46F);
        // Right hand strums across the strings
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -0.98F + strum * 0.14F);
        model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.35F - strum * 0.08F);
        model.head.zRot = Mth.lerp(w, model.head.zRot, 0.12F);
        model.head.xRot = Mth.lerp(w, model.head.xRot, 0.14F);
        model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.45F, 0.45F), 0.0F);
        return true;
    }

    public static void renderIdleEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        if (!ClientAuraTracker.isVentiIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.01F) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();

        float pt = mc.getFrameTime();
        float time = entity.tickCount + pt;
        poseStack.pushPose();
        if (isFirstPersonSelf) {
            float camYaw = Mth.rotLerp(pt, entity.yRotO, entity.getYRot());
            float camPitch = Mth.rotLerp(pt, entity.xRotO, entity.getXRot());
            poseStack.translate(0.0F, entity.getEyeHeight(), 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-camYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(camPitch));
            poseStack.translate(-0.16F, -0.20F + Mth.sin(time * 0.12F) * 0.012F, 0.44F);
            poseStack.mulPose(Axis.YP.rotationDegrees(16.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(12.0F));
            poseStack.scale(fade * 0.85F, fade * 0.85F, fade * 0.85F);
        } else {
            if (d != null && !d.ventiIdleYawSet) {
                d.ventiIdleYaw = entity.yBodyRot;
                d.ventiIdleYawSet = true;
            }
            float bodyYaw = (d != null && d.ventiIdleYawSet) ? d.ventiIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.06F, 1.08F + Mth.sin(time * 0.12F) * 0.02F, 0.38F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(12.0F));
            poseStack.scale(fade, fade, fade);
        }

        VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
        Matrix4f mat = poseStack.last().pose();
        // Holy Lyre golden frame & teal soundbox
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.14F, -0.12F, -0.03F, 0.14F, -0.02F, 0.03F, 45, 195, 165, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.16F, -0.02F, -0.025F, -0.11F, 0.24F, 0.025F, 255, 212, 75, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, 0.11F, -0.02F, -0.025F, 0.16F, 0.24F, 0.025F, 255, 212, 75, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.14F, 0.21F, -0.02F, 0.14F, 0.26F, 0.02F, 255, 212, 75, 255);
        // 5 glowing Anemo strings
        for (int s = -2; s <= 2; s++) {
            float sx = s * 0.042F;
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, sx - 0.005F, -0.02F, -0.006F, sx + 0.005F, 0.21F, 0.006F, 130, 255, 225, 255);
        }

        // Swirling wind notes around Venti
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        int aNote = Math.max(15, Math.round(200.0F * fade));
        for (int n = 0; n < 3; n++) {
            float ang = (float) (time * 0.09D + n * Math.PI * 2.0D / 3.0D);
            float nx = Mth.cos(ang) * 0.48F;
            float nz = Mth.sin(ang) * 0.48F - 0.25F;
            float ny = 0.05F + Mth.sin(time * 0.16F + n * 1.9F) * 0.14F;
            poseStack.pushPose();
            poseStack.translate(nx, ny, nz);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.0F));
            Matrix4f nMat = poseStack.last().pose();
            WindGliderRenderer.emitDoubleQuad(transVc, nMat, -0.045F, -0.045F, 0.0F, 0.045F, -0.045F, 0.0F, 0.045F, 0.045F, 0.0F, -0.045F, 0.045F, 0.0F, 125, 255, 215, aNote);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
