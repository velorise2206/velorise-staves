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
                int elapsed = Math.max(0, s.getUseDuration(useStack, entity) - entity.getUseItemRemainingTicks());
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
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
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

    private static final ResourceLocation VENTI_WIND_CHARGE_TEX =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/wind_charge.png");

    public static void renderVentiWindChargeOrbs(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        boolean showHitbox = Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId != -2 || arc.start == null || arc.end == null) continue;
            float baseRadius = (float) Math.max(1.0D, arc.end.x);
            float cx = (float) (arc.start.x - camPos.x);
            float cy = (float) (arc.start.y - camPos.y);
            float cz = (float) (arc.start.z - camPos.z);
            float fadeIn = Mth.clamp((now - arc.startTime) / 220.0F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((arc.expireTime - now) / 320.0F, 0.0F, 1.0F);
            float fade = Math.min(fadeIn, fadeOut);
            float timeSec = (now - arc.startTime) * 0.001F;
            float uScroll = (timeSec * 0.75F) % 1.0F;
            int r = 175, g = 255, b = 232;
            if (arc.color != 0 && arc.color != 0xFF66FFD8) {
                r = (arc.color >> 16) & 0xFF;
                g = (arc.color >> 8) & 0xFF;
                b = arc.color & 0xFF;
            }

            RenderType windType = RenderType.breezeWind(VENTI_WIND_CHARGE_TEX, uScroll, 0.0F);
            VertexConsumer windConsumer = bufferSource.getBuffer(windType);
            float[] scales = { baseRadius * 0.88F, baseRadius * 0.72F, baseRadius * 0.56F, baseRadius * 0.42F };
            float[] speedY = { 105.0F, -155.0F, 210.0F, -270.0F };
            float[] tiltX = { 24.0F, -38.0F, 56.0F, -64.0F };
            float[] tiltZ = { 12.0F, 28.0F, -22.0F, 42.0F };
            int[] alphas = { Math.round(115.0F * fade), Math.round(150.0F * fade), Math.round(185.0F * fade), Math.round(215.0F * fade) };

            for (int k = 0; k < 4; k++) {
                poseStack.pushPose();
                poseStack.translate(cx, cy, cz);
                poseStack.mulPose(Axis.XP.rotationDegrees(tiltX[k]));
                poseStack.mulPose(Axis.ZP.rotationDegrees(tiltZ[k]));
                poseStack.mulPose(Axis.YP.rotationDegrees(timeSec * speedY[k]));
                Matrix4f m = poseStack.last().pose();
                drawWindStreamBand(windConsumer, m, scales[k], 18, r, g, b, Math.max(12, alphas[k]));
                poseStack.popPose();
            }
            if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(windType);
            }
            if (showHitbox) {
                renderVentiDebugHitbox(poseStack, bufferSource, cx, cy - 1.0F, cz, baseRadius, r / 255.0F, g / 255.0F, b / 255.0F);
            }
        }
    }

    private static void renderVentiDebugHitbox(
            PoseStack poseStack, MultiBufferSource bufferSource,
            float hcx, float hcy, float hcz, float radius,
            float rF, float gF, float bF
    ) {
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                poseStack, lineConsumer,
                hcx - radius, hcy - radius, hcz - radius,
                hcx + radius, hcy + radius, hcz + radius,
                1.0F, 1.0F, 1.0F, 1.0F);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        int r = Math.round(rF * 255.0F);
        int g = Math.round(gF * 255.0F);
        int b = Math.round(bF * 255.0F);
        int segs = 36;
        for (int plane = 0; plane < 3; plane++) {
            for (int i = 0; i < segs; i++) {
                float a0 = (float) (2.0 * Math.PI * i / segs);
                float a1 = (float) (2.0 * Math.PI * (i + 1) / segs);
                float x0 = hcx, y0 = hcy, z0 = hcz;
                float x1 = hcx, y1 = hcy, z1 = hcz;
                if (plane == 0) {
                    x0 += Mth.cos(a0) * radius; z0 += Mth.sin(a0) * radius;
                    x1 += Mth.cos(a1) * radius; z1 += Mth.sin(a1) * radius;
                } else if (plane == 1) {
                    x0 += Mth.cos(a0) * radius; y0 += Mth.sin(a0) * radius;
                    x1 += Mth.cos(a1) * radius; y1 += Mth.sin(a1) * radius;
                } else {
                    y0 += Mth.cos(a0) * radius; z0 += Mth.sin(a0) * radius;
                    y1 += Mth.cos(a1) * radius; z1 += Mth.sin(a1) * radius;
                }
                float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
                float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 1.0E-5F) {
                    dx /= len; dy /= len; dz /= len;
                }
                lineConsumer.addVertex(mat, x0, y0, z0).setColor(r, g, b, 255).setNormal(pose, dx, dy, dz);
                lineConsumer.addVertex(mat, x1, y1, z1).setColor(r, g, b, 255).setNormal(pose, dx, dy, dz);
            }
        }
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lines());
        }
    }

    private static void drawWindStreamBand(
            VertexConsumer consumer, Matrix4f matrix,
            float radius, int lonBands,
            int r, int g, int b, int a
    ) {
        float theta0 = (float) (Math.PI * 0.43D);
        float theta1 = (float) (Math.PI * 0.57D);
        float sin0 = Mth.sin(theta0), cos0 = Mth.cos(theta0);
        float sin1 = Mth.sin(theta1), cos1 = Mth.cos(theta1);
        float v0 = 0.56F;
        float v1 = 0.88F;
        for (int lon = 0; lon < lonBands; lon++) {
            float phi0 = (float) (2.0 * Math.PI * lon / lonBands);
            float phi1 = (float) (2.0 * Math.PI * (lon + 1) / lonBands);
            float cPhi0 = Mth.cos(phi0), sPhi0 = Mth.sin(phi0);
            float cPhi1 = Mth.cos(phi1), sPhi1 = Mth.sin(phi1);
            float u0 = (float) lon / lonBands;
            float u1 = (float) (lon + 1) / lonBands;

            float nx00 = sin0 * cPhi0, ny00 = cos0, nz00 = sin0 * sPhi0;
            float nx10 = sin1 * cPhi0, ny10 = cos1, nz10 = sin1 * sPhi0;
            float nx11 = sin1 * cPhi1, ny11 = cos1, nz11 = sin1 * sPhi1;
            float nx01 = sin0 * cPhi1, ny01 = cos0, nz01 = sin0 * sPhi1;

            consumer.addVertex(matrix, radius * nx00, radius * ny00, radius * nz00)
                    .setColor(r, g, b, a).setUv(u0, v0)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                    .setNormal(nx00, ny00, nz00);
            consumer.addVertex(matrix, radius * nx10, radius * ny10, radius * nz10)
                    .setColor(r, g, b, a).setUv(u0, v1)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                    .setNormal(nx10, ny10, nz10);
            consumer.addVertex(matrix, radius * nx11, radius * ny11, radius * nz11)
                    .setColor(r, g, b, a).setUv(u1, v1)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                    .setNormal(nx11, ny11, nz11);
            consumer.addVertex(matrix, radius * nx01, radius * ny01, radius * nz01)
                    .setColor(r, g, b, a).setUv(u1, v0)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                    .setNormal(nx01, ny01, nz01);
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
