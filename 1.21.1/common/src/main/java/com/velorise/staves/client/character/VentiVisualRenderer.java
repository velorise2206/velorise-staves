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
import net.minecraft.client.player.AbstractClientPlayer;
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
    private static final java.util.Set<Integer> SKILL_E_WIND_ORB_IDS = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public static void clearClientCaches() {
        SKILL_E_WIND_ORB_IDS.clear();
    }

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
        Minecraft mcOrb = Minecraft.getInstance();
        if (mcOrb.level != null && (!SKILL_E_WIND_ORB_IDS.isEmpty() || (now - ClientAuraTracker.lastVentiSkillETimeMs) <= 2500L)) {
            float ptOrb = mcOrb.getTimer().getGameTimeDeltaPartialTick(true);
            for (net.minecraft.world.entity.Entity e : mcOrb.level.entitiesForRendering()) {
                if (e instanceof net.minecraft.world.entity.projectile.windcharge.WindCharge wc && wc.isAlive()) {
                    if (wc.tickCount <= 6 && wc.getOwner() != null) {
                        ClientAuraTracker.AuraDisplayData od = ClientAuraTracker.get(wc.getOwner().getId());
                        if (od != null && od.staffAnimType == ClientAuraTracker.ANIM_VENTI_SKILL_E && now < od.staffAnimUntil) {
                            SKILL_E_WIND_ORB_IDS.add(wc.getId());
                        }
                    }
                    if (SKILL_E_WIND_ORB_IDS.contains(wc.getId())) {
                        Vec3 wp = wc.getPosition(ptOrb).add(0.0D, wc.getBbHeight() * 0.5D, 0.0D).subtract(camPos);
                        float tSec = (wc.tickCount + ptOrb) * 0.05F;
                        RenderType wt = RenderType.breezeWind(VENTI_WIND_CHARGE_TEX, (tSec * 0.9F) % 1.0F, 0.0F);
                        VertexConsumer wVc = bufferSource.getBuffer(wt);
                        float[] oScales = { 0.62F, 0.48F, 0.34F };
                        for (int k = 0; k < 3; k++) {
                            poseStack.pushPose();
                            poseStack.translate(wp.x, wp.y, wp.z);
                            poseStack.mulPose(Axis.XP.rotationDegrees(k * 42.0F - 30.0F));
                            poseStack.mulPose(Axis.YP.rotationDegrees(tSec * (180.0F + k * 90.0F)));
                            drawWindStreamBand(wVc, poseStack.last().pose(), oScales[k], 14, 155, 255, 228, 225);
                            poseStack.popPose();
                        }
                        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
                            bs.endBatch(wt);
                        }
                    }
                }
            }
            SKILL_E_WIND_ORB_IDS.removeIf(eid -> mcOrb.level.getEntity(eid) == null || !mcOrb.level.getEntity(eid).isAlive());
        }
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
        boolean isStellarSwirl = (arc.color == 0xFF66E6FF);
        int streamCount = isStellarSwirl ? 2 : 6;
        int effStreamAlpha = isStellarSwirl ? Math.max(16, Math.round(streamAlpha * 0.65F)) : streamAlpha;
        for (int s = 0; s < streamCount; s++) {
            float yawTilt = s * (isStellarSwirl ? 3.1416F : 1.0472F) + timeSec * (0.45F + s * 0.11F);
            float pitchTilt = isStellarSwirl ? (-0.18F + s * 0.36F) : (-0.95F + (s % 3) * 0.65F + Mth.sin(timeSec * 1.3F + s) * 0.25F);
            float cy = Mth.cos(yawTilt), sy = Mth.sin(yawTilt);
            float cp = Mth.cos(pitchTilt), sp = Mth.sin(pitchTilt);
            Vec3 uAxis = new Vec3(cy, 0.0D, sy);
            Vec3 nAxis = new Vec3(-sy * sp, cp, cy * sp);
            Vec3 vAxis = nAxis.cross(uAxis).normalize();
            double rStream = isStellarSwirl ? (baseRadius * (0.55D + 0.25D * s)) : (baseRadius * (0.36D + 0.11D * s));
            float phaseStart = timeSec * (3.6F + s * 0.7F) * ((s % 2 == 0) ? 1.0F : -1.0F) + s * 1.9F;
            float sweepRad = isStellarSwirl ? 2.20F : 2.85F;
            Vec3 prev = null;
            for (int seg = 0; seg <= 9; seg++) {
                float f = seg / 9.0F;
                float ang = phaseStart + f * sweepRad;
                double waveR = isStellarSwirl ? rStream : (rStream * (1.0D + 0.16D * Mth.sin(ang * 2.5F + timeSec * 5.0F + s)));
                double waveH = isStellarSwirl ? (baseRadius * 0.05D * Mth.cos(ang - timeSec * 3.0F)) : (baseRadius * 0.16D * Mth.cos(ang * 1.8F - timeSec * 4.2F));
                Vec3 pt = cCam
                        .add(uAxis.scale(Math.cos(ang) * waveR))
                        .add(vAxis.scale(Math.sin(ang) * waveR))
                        .add(nAxis.scale(waveH));
                if (prev != null) {
                    if (isStellarSwirl) {
                        ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, prev, pt, nAxis, 0.022D + 0.010D * Math.sin(f * Math.PI), sr, sg, sb, effStreamAlpha);
                    } else {
                        ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, prev, pt, nAxis, 0.045D + 0.020D * Math.sin(f * Math.PI), sr, sg, sb, effStreamAlpha);
                        ElementalReactionBadgeRenderer.drawQuadRibbon(consumer, matrix, prev, pt, uAxis, 0.018D, cr, cg, cb, coreAlpha);
                    }
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
                d.ventiIdleFade = Math.min(1.0F, d.ventiIdleFade + 0.016F / 1.35F);
            }
        } else if (nowMs > d.ventiIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.ventiIdleLastMs) / 1000.0F);
            d.ventiIdleLastMs = nowMs;
            if (d.ventiIdle) {
                d.ventiIdleFade = Math.min(1.0F, d.ventiIdleFade + dt / 1.35F);
            } else {
                d.ventiIdleFade = Math.max(0.0F, d.ventiIdleFade - dt / 0.55F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.ventiIdleFade);
    }

    public static boolean renderUpdraftArc(
            ClientAuraTracker.ActiveArc arc,
            Vec3 startPos,
            Vec3 endPos,
            Vec3 camPos,
            long now,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        if (arc.color != 0xFF5CE6C8) return false;
        float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
        float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
        float fade = (prog < 0.20F) ? (prog / 0.20F) : (1.0F - (prog - 0.20F) / 0.80F);
        int alpha = Math.max(12, Math.round(220.0F * fade));

        VertexConsumer vc = bufferSource.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        Vec3 base = startPos.subtract(camPos);
        double height = Math.max(1.5D, endPos.y - startPos.y);
        poseStack.pushPose();
        poseStack.translate(base.x, base.y, base.z);
        Matrix4f mat = poseStack.last().pose();

        // Multiple clean continuous circular Anemo wind rings pushing mobs upward
        boolean isLargeE = height > 5.5D;
        int ringCount = isLargeE ? 4 : 3;
        float baseRad = isLargeE ? 2.40F : 1.15F;
        float halfH = isLargeE ? 0.085F : 0.052F;
        int segs = 24;
        for (int r = 0; r < ringCount; r++) {
            float rp = Mth.clamp(prog * 1.32F - r * 0.14F, 0.0F, 1.0F);
            if (rp <= 0.01F) continue;
            float ry = (float) (height * rp);
            float rad = baseRad + rp * (isLargeE ? 1.40F : 0.65F) - r * 0.08F;
            float spin = prog * 5.2F + r * 0.8F;
            int rAlpha = Math.max(12, Math.round(alpha * (1.0F - r * 0.12F)));
            for (int s = 0; s < segs; s++) {
                float a0 = (float) (s * Math.PI * 2.0D / segs) + spin;
                float a1 = (float) ((s + 1) * Math.PI * 2.0D / segs) + spin;
                WindGliderRenderer.emitDoubleQuad(vc, mat,
                        Mth.cos(a0) * rad, ry - halfH, Mth.sin(a0) * rad,
                        Mth.cos(a1) * rad, ry - halfH, Mth.sin(a1) * rad,
                        Mth.cos(a1) * rad, ry + halfH, Mth.sin(a1) * rad,
                        Mth.cos(a0) * rad, ry + halfH, Mth.sin(a0) * rad,
                        120, 255, 222, rAlpha);
            }
        }
        poseStack.popPose();
        return true;
    }

    public static boolean setupFirstPersonLyreArmPose(
            AbstractClientPlayer player,
            float partialTicks,
            HumanoidArm arm,
            PoseStack poseStack
    ) {
        if (player == null || !ClientAuraTracker.isVentiIdle(player.getId())) return false;
        if (player.isUsingItem() || player.swinging) return false;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(player.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F) return false;
        int mode = (d != null) ? (d.ventiIdleMode > 0 ? d.ventiIdleMode : d.ventiIdleLastMode) : 1;
        float age = player.tickCount + partialTicks;
        if (mode == 2) {
            if (arm == HumanoidArm.LEFT) {
                poseStack.translate(0.08F * w, -0.02F * w + Mth.sin(age * 0.12F) * 0.015F * w, -0.16F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(-18.0F * w));
                return true;
            }
            return false;
        }
        if (arm == HumanoidArm.RIGHT) {
            float strum = Mth.sin(age * 0.26F);
            poseStack.translate(-0.24F * w, 0.34F * w + strum * 0.045F * w, -0.04F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(-26.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(32.0F * w - strum * 8.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(24.0F * w));
            poseStack.scale(0.68F, 0.68F, 0.68F);
        } else {
            poseStack.translate(-0.30F * w, 0.04F * w, -0.48F * w);
            poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F * w));
            poseStack.mulPose(Axis.YP.rotationDegrees(-24.0F * w));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-16.0F * w));
        }
        return true;
    }

    public static float getLevitationCameraOffsetY(net.minecraft.world.entity.Entity entity, float partialTick) {
        if (!(entity instanceof LivingEntity le) || le.isUsingItem() || le.swinging) return 0.0F;
        if (!ClientAuraTracker.isVentiIdle(le.getId())) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(le.getId());
        if (d == null) return 0.0F;
        int mode = d.ventiIdleMode > 0 ? d.ventiIdleMode : d.ventiIdleLastMode;
        if (mode != 2) return 0.0F;
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F) return 0.0F;
        float hover = Mth.sin((le.tickCount + partialTick) * 0.12F);
        return (0.53F + hover * 0.047F) * w;
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        Minecraft mcChk = Minecraft.getInstance();
        boolean localBowPull = mcChk.player != null && entity.getId() == mcChk.player.getId()
                && (ClientAuraTracker.localDaybreakBowPullTicks > 0 || ClientAuraTracker.ventiUltiHoldTicks > 0);
        if (entity.isUsingItem() || entity.swinging || localBowPull) {
            ClientAuraTracker.AuraDisplayData dAct = ClientAuraTracker.get(entity.getId());
            if (dAct != null) {
                dAct.ventiIdle = false;
                dAct.ventiIdleFade = 0.0F;
                dAct.ventiIdleYawSet = false;
            }
            return false;
        }
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


        int mode = (d != null) ? (d.ventiIdleMode > 0 ? d.ventiIdleMode : d.ventiIdleLastMode) : 1;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        if (mode == 2) {
            // Levitating Anemo Breeze idle: floating upward, one knee bent, left hand holding Wind Orb
            float hover = Mth.sin(ageInTicks * 0.12F);
            float liftY = -8.5F * w + hover * 0.75F * w;
            model.head.y += liftY;
            model.hat.y += liftY;
            model.body.y += liftY;
            model.rightArm.y += liftY;
            model.leftArm.y += liftY;
            model.rightLeg.y += liftY;
            model.leftLeg.y += liftY;

            model.body.xRot = Mth.lerp(w, model.body.xRot, -0.06F);
            model.body.zRot = Mth.lerp(w, model.body.zRot, 0.04F * hover);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.18F);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.22F);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.58F);
            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -0.32F);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.18F);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.52F);
            model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, 0.14F);
            model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.05F);
            model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, -0.58F + hover * 0.06F);
            model.leftLeg.yRot = Mth.lerp(w, model.leftLeg.yRot, 0.12F);
            model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.08F);
            model.head.zRot = Mth.lerp(w, model.head.zRot, -0.06F);
            model.head.xRot = Mth.lerp(w, model.head.xRot, 0.14F);
            model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.45F, 0.45F), -0.48F);
            model.hat.copyFrom(model.head);
            return true;
        }

        float strum = Mth.sin(ageInTicks * 0.26F);
        float sway = Mth.sin(ageInTicks * 0.08F);

        // Graceful bard stance: weight on left leg, right leg relaxed/tucked slightly back, gentle torso & head tilt
        model.body.zRot = Mth.lerp(w, model.body.zRot, -0.05F + sway * 0.02F);
        model.body.yRot = Mth.lerp(w, model.body.yRot, 0.10F);
        model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, 0.04F);
        model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.04F);
        model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, 0.26F);
        model.rightLeg.yRot = Mth.lerp(w, model.rightLeg.yRot, -0.18F);
        model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.08F);

        // Left hand cradles the Holy Lyre against his left chest/shoulder
        model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.08F + sway * 0.03F);
        model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.52F);
        model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.14F);
        // Right hand raised delicately in front of chest plucking the strings
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.02F + strum * 0.12F);
        model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.42F - strum * 0.08F);
        model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.16F);

        model.head.zRot = Mth.lerp(w, model.head.zRot, 0.14F + sway * 0.04F);
        model.head.xRot = Mth.lerp(w, model.head.xRot, 0.10F);
        model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.45F, 0.45F), -0.08F);
        model.hat.copyFrom(model.head);
        return true;
    }

    public static void renderIdleEffects(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        Minecraft mcChk = Minecraft.getInstance();
        boolean localBowPull = mcChk.player != null && entity.getId() == mcChk.player.getId()
                && (ClientAuraTracker.localDaybreakBowPullTicks > 0 || ClientAuraTracker.ventiUltiHoldTicks > 0);
        if (entity.isUsingItem() || entity.swinging || localBowPull) {
            ClientAuraTracker.AuraDisplayData dAct = ClientAuraTracker.get(id);
            if (dAct != null) {
                dAct.ventiIdle = false;
                dAct.ventiIdleFade = 0.0F;
                dAct.ventiIdleYawSet = false;
            }
            return;
        }
        if (!ClientAuraTracker.isVentiIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.01F) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPersonSelf = mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson();

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        int mode = (d != null) ? (d.ventiIdleMode > 0 ? d.ventiIdleMode : d.ventiIdleLastMode) : 1;
        if (mode == 2) {
            renderLevitationWindIdle(entity, poseStack, buffer, time, pt, fade, isFirstPersonSelf);
            return;
        }
        poseStack.pushPose();
        if (isFirstPersonSelf) {
            poseStack.translate(-0.16F, -0.16F + Mth.sin(time * 0.12F) * 0.010F, -0.82F);
            poseStack.mulPose(Axis.YP.rotationDegrees(20.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
            poseStack.scale(fade * 0.78F, fade * 0.78F, fade * 0.78F);
        } else {
            float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.04F, 1.22F + Mth.sin(time * 0.12F) * 0.018F, 0.46F);
            poseStack.mulPose(Axis.YP.rotationDegrees(72.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(14.0F));
            poseStack.scale(fade, fade, fade);
        }

        VertexConsumer solidVc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());
        Matrix4f mat = poseStack.last().pose();

        // Holy Lyre "Der Frühling": curved golden-bronze base & soundbox with glowing Anemo teal gem
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.11F, -0.13F, -0.028F, 0.11F, -0.05F, 0.028F, 198, 148, 74, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.13F, -0.06F, -0.024F, 0.13F, -0.01F, 0.024F, 232, 186, 98, 255);
        // Glowing Anemo teal gem in center of base
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.036F, -0.10F, -0.034F, 0.036F, -0.025F, 0.034F, 65, 235, 195, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.020F, -0.085F, -0.038F, 0.020F, -0.040F, 0.038F, 185, 255, 238, 255);

        // Two Anemo wing/leaf flourishes flaring at the base
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.12F, -0.06F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 34.0F));
            Matrix4f wMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, wMat, -0.055F, -0.018F, -0.012F, 0.055F, 0.024F, 0.012F, 195, 248, 224, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, wMat, -0.035F, 0.024F, -0.010F, 0.035F, 0.060F, 0.010F, 130, 235, 198, 255);
            poseStack.popPose();
        }

        // Angled curved golden-bronze lyre arms + pointed wing-tip finials
        for (int side : new int[]{-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(side * 0.115F, -0.02F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 12.0F));
            Matrix4f aMat = poseStack.last().pose();
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, aMat, -0.020F, 0.0F, -0.018F, 0.020F, 0.23F, 0.018F, 218, 172, 92, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, aMat, -0.016F, 0.23F, -0.015F, 0.016F, 0.30F, 0.015F, 255, 224, 128, 255);
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, aMat, -0.009F, 0.30F, -0.009F, 0.009F, 0.35F, 0.009F, 255, 245, 195, 255);
            poseStack.popPose();
        }

        // Curved upper crossbar
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.155F, 0.215F, -0.014F, 0.155F, 0.245F, 0.014F, 245, 205, 108, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, -0.085F, 0.245F, -0.014F, 0.085F, 0.262F, 0.014F, 255, 228, 140, 255);

        // 6 shimmering Anemo-gold strings
        for (int s = 0; s < 6; s++) {
            float sx = -0.075F + s * 0.030F;
            ZhongliVisualRenderer.drawJadeArmorBox(solidVc, mat, sx - 0.0035F, -0.015F, -0.004F, sx + 0.0035F, 0.225F, 0.004F, 165, 255, 232, 255);
        }

        // Swirling wind notes around Venti
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());
        int aNote = Math.max(15, Math.round(200.0F * fade));
        for (int n = 0; n < 3; n++) {
            float ang = (float) (time * 0.09D + n * Math.PI * 2.0D / 3.0D);
            float nx = Mth.cos(ang) * 0.44F;
            float nz = Mth.sin(ang) * 0.44F - (isFirstPersonSelf ? 0.05F : 0.22F);
            float ny = 0.05F + Mth.sin(time * 0.16F + n * 1.9F) * 0.12F;
            poseStack.pushPose();
            poseStack.translate(nx, ny, nz);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.0F));
            Matrix4f nMat = poseStack.last().pose();
            WindGliderRenderer.emitDoubleQuad(transVc, nMat, -0.040F, -0.040F, 0.0F, 0.040F, -0.040F, 0.0F, 0.040F, 0.040F, 0.0F, -0.040F, 0.040F, 0.0F, 125, 255, 215, aNote);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
    private static void renderLevitationWindIdle(
            LivingEntity entity,
            PoseStack poseStack,
            MultiBufferSource buffer,
            float time,
            float pt,
            float fade,
            boolean isFirstPersonSelf
    ) {
        float hover = -Mth.sin(time * 0.12F) * 0.047F;
        VertexConsumer transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());

        // 1. Mini Wind's Grand Ode Ulti Vortex scaled to fit cleanly in Venti's left hand
        poseStack.pushPose();
        if (isFirstPersonSelf) {
            poseStack.translate(-0.22F, -0.06F + hover * 0.5F, -0.46F);
            poseStack.scale(fade * 0.78F, fade * 0.78F, fade * 0.78F);
        } else {
            ClientAuraTracker.AuraDisplayData dOrb = ClientAuraTracker.get(entity.getId());
            float bodyYaw = (dOrb != null && dOrb.ventiIdleYawSet) ? dOrb.ventiIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.36F, 1.72F + hover, 0.52F);
            poseStack.scale(fade, fade, fade);
        }
        float timeSec = time * 0.05F;
        float uScroll = (timeSec * 0.75F) % 1.0F;
        RenderType windType = RenderType.breezeWind(VENTI_WIND_CHARGE_TEX, uScroll, 0.0F);
        VertexConsumer windConsumer = buffer.getBuffer(windType);
        float baseR = 0.22F;
        float[] scales = { baseR * 0.88F, baseR * 0.72F, baseR * 0.56F, baseR * 0.42F };
        float[] speedY = { 105.0F, -155.0F, 210.0F, -270.0F };
        float[] tiltX = { 24.0F, -38.0F, 56.0F, -64.0F };
        float[] tiltZ = { 12.0F, 28.0F, -22.0F, 42.0F };
        int[] alphas = { Math.max(15, Math.round(155.0F * fade)), Math.max(18, Math.round(190.0F * fade)), Math.max(22, Math.round(220.0F * fade)), Math.max(25, Math.round(248.0F * fade)) };
        for (int k = 0; k < 4; k++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(tiltX[k]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(tiltZ[k]));
            poseStack.mulPose(Axis.YP.rotationDegrees(timeSec * speedY[k]));
            Matrix4f m = poseStack.last().pose();
            drawWindStreamBand(windConsumer, m, scales[k], 16, k >= 2 ? 235 : 205, 255, k >= 2 ? 250 : 242, alphas[k]);
            poseStack.popPose();
        }
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(windType);
        }
        // Wind Charge burst/explosion swirl sphere using RenderType.breezeWind on a smooth sphere
        float coreU = (timeSec * 0.65F) % 1.0F;
        RenderType coreWindType = RenderType.breezeWind(VENTI_WIND_CHARGE_TEX, coreU, 0.0F);
        VertexConsumer coreWindVc = buffer.getBuffer(coreWindType);
        poseStack.pushPose();
        float burstRad = (0.085F + 0.014F * Mth.sin(time * 0.14F)) * fade;
        int coreA = Math.max(30, Math.round(225.0F * fade));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 24.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 14.0F));
        Matrix4f coreMat = poseStack.last().pose();
        int cSegs = 12, cRings = 6;
        for (int ci = 0; ci < cRings; ci++) {
            float p0 = (float) (-Math.PI * 0.5D + Math.PI * ci / cRings);
            float p1 = (float) (-Math.PI * 0.5D + Math.PI * (ci + 1) / cRings);
            float y0 = Mth.sin(p0) * burstRad, y1 = Mth.sin(p1) * burstRad;
            float r0 = Mth.cos(p0) * burstRad, r1 = Mth.cos(p1) * burstRad;
            float v0 = ci / (float) cRings, v1 = (ci + 1) / (float) cRings;
            for (int cj = 0; cj < cSegs; cj++) {
                float t0 = (float) (cj * 2.0D * Math.PI / cSegs);
                float t1 = (float) ((cj + 1) * 2.0D * Math.PI / cSegs);
                float u0 = cj / (float) cSegs, u1 = (cj + 1) / (float) cSegs;
                float x00 = Mth.cos(t0) * r0, z00 = Mth.sin(t0) * r0;
                float x10 = Mth.cos(t1) * r0, z10 = Mth.sin(t1) * r0;
                float x11 = Mth.cos(t1) * r1, z11 = Mth.sin(t1) * r1;
                float x01 = Mth.cos(t0) * r1, z01 = Mth.sin(t0) * r1;
                coreWindVc.addVertex(coreMat, x00, y0, z00).setColor(235, 255, 250, coreA).setUv(u0, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
                coreWindVc.addVertex(coreMat, x10, y0, z10).setColor(235, 255, 250, coreA).setUv(u1, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
                coreWindVc.addVertex(coreMat, x11, y1, z11).setColor(235, 255, 250, coreA).setUv(u1, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
                coreWindVc.addVertex(coreMat, x01, y1, z01).setColor(235, 255, 250, coreA).setUv(u0, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
            }
        }
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(coreWindType);
        }
        poseStack.popPose();
        poseStack.popPose();
        transVc = buffer.getBuffer(ClientAuraRenderer.getTranslucentNoDepthWrite());

        // 2. Multi-tier swirling Anemo whirlwind cyclone & spiral wind ribbons beneath & around feet
        if (!isFirstPersonSelf) {
            ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
            float bodyYaw = (d != null && d.ventiIdleYawSet) ? d.ventiIdleYaw : Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.scale(fade, fade, fade);

            int aRing = Math.max(15, Math.round(180.0F * fade));
            int aCore = Math.min(255, aRing + 45);

            // 5 ascending vortex tiers from ground up to thighs/knees
            int tierCount = 5;
            for (int ring = 0; ring < tierCount; ring++) {
                float ringFrac = ring / (float) (tierCount - 1);
                float ry = 0.06F + ringFrac * 0.96F + hover * (0.3F + ringFrac * 0.4F);
                // Funnel geometry: wider base (0.78F), narrows at waist (0.50F), slightly flares top (0.62F)
                float rRad = 0.78F - 0.38F * Mth.sin(ringFrac * (float) Math.PI * 0.85F);
                float dir = (ring % 2 == 0) ? 1.0F : -1.0F;
                float spin = time * (0.16F + ring * 0.04F) * dir;
                int segs = 20;
                Matrix4f fMat = poseStack.last().pose();

                for (int s = 0; s < segs; s++) {
                    if ((s + ring) % 5 == 4) continue; // Wispy broken ribbon gaps
                    float a0 = (float) (s * Math.PI * 2.0D / segs) + spin;
                    float a1 = (float) ((s + 1) * Math.PI * 2.0D / segs) + spin;
                    float wPhase = a0 * 2.5F + time * (0.12F * dir) + ring * 0.8F;
                    float y0 = ry + Mth.sin(wPhase) * 0.035F;
                    float y1 = ry + Mth.sin(wPhase + 0.35F) * 0.035F;

                    // Outer turquoise wind stream
                    WindGliderRenderer.emitDoubleQuad(transVc, fMat,
                            Mth.cos(a0) * rRad, y0 - 0.022F, Mth.sin(a0) * rRad,
                            Mth.cos(a1) * rRad, y1 - 0.022F, Mth.sin(a1) * rRad,
                            Mth.cos(a1) * rRad, y1 + 0.022F, Mth.sin(a1) * rRad,
                            Mth.cos(a0) * rRad, y0 + 0.022F, Mth.sin(a0) * rRad,
                            110, 245, 218, aRing);

                    // Inner bright luminous core ribbon
                    float coreRad = rRad * 0.94F;
                    WindGliderRenderer.emitDoubleQuad(transVc, fMat,
                            Mth.cos(a0) * coreRad, y0 - 0.009F, Mth.sin(a0) * coreRad,
                            Mth.cos(a1) * coreRad, y1 - 0.009F, Mth.sin(a1) * coreRad,
                            Mth.cos(a1) * coreRad, y1 + 0.009F, Mth.sin(a1) * coreRad,
                            Mth.cos(a0) * coreRad, y0 + 0.009F, Mth.sin(a0) * coreRad,
                            225, 255, 245, aCore);
                }
            }

            // Ascending spiral wind wisps / breeze particles drifting upward around the cyclone
            Matrix4f fMat = poseStack.last().pose();
            int pCount = 8;
            for (int p = 0; p < pCount; p++) {
                float pProg = ((time * 0.032F + p * (1.0F / pCount)) % 1.0F);
                float py = 0.05F + pProg * 1.30F + hover * 0.4F;
                float pr = 0.68F - 0.22F * Mth.sin(pProg * (float) Math.PI);
                float pAng = time * 0.18F + p * ((float) Math.PI * 2.0F / pCount);
                float px = Mth.cos(pAng) * pr;
                float pz = Mth.sin(pAng) * pr;
                float pSize = 0.030F * (0.6F + 0.4F * Mth.sin(pProg * (float) Math.PI));
                int pAlpha = Math.max(10, Math.round(200.0F * fade * Mth.sin(pProg * (float) Math.PI)));

                ClientAuraRenderer.emitCrystalQuad(transVc, fMat,
                        px - pSize, py, pz - pSize, 180, 255, 235, pAlpha,
                        px + pSize, py, pz - pSize, 180, 255, 235, pAlpha,
                        px + pSize, py + pSize * 1.5F, pz + pSize, 235, 255, 250, pAlpha,
                        px - pSize, py + pSize * 1.5F, pz + pSize, 235, 255, 250, pAlpha);
            }

            poseStack.popPose();
        }
    }

}
