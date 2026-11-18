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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;

public final class MavuikaVisualRenderer {
    private static final java.util.Map<Integer, float[]> BIKE_FP_CAM_SMOOTH = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Integer, float[]> BIKE_ARM_SMOOTH = new java.util.concurrent.ConcurrentHashMap<>();

    public static void clearClientCaches() {
        BIKE_FP_CAM_SMOOTH.clear();
        BIKE_ARM_SMOOTH.clear();
    }
    private static final ResourceLocation MAVUIKA_HELMET_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/mavuika_helmet_texture.png");
    private static final ResourceLocation MAVUIKA_SUN_AURA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/mavuika_sun_aura.png");
    private static final ResourceLocation MAVUIKA_SUN_CHARGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/mavuika_sun_charge.png");
    private static com.velorise.staves.client.model.MavuikaHelmetModel<LivingEntity> helmetModel;

    private static com.velorise.staves.client.model.MavuikaHelmetModel<LivingEntity> getOrBakeHelmetModel() {
        if (helmetModel == null) {
            helmetModel = new com.velorise.staves.client.model.MavuikaHelmetModel<>(
                    com.velorise.staves.client.model.MavuikaHelmetModel.createBodyLayer().bakeRoot()
            );
        }
        return helmetModel;
    }

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
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float bodyYaw = (d != null && d.mavuikaIdleYawSet) ? d.mavuikaIdleYaw : Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        float entityScale = entity.getScale();
        poseStack.scale(-entityScale, -entityScale, entityScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        humanoid.head.translateAndRotate(poseStack);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(MAVUIKA_HELMET_TEXTURE));
        getOrBakeHelmetModel().renderToBuffer(poseStack, vc, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.entityTranslucent(MAVUIKA_HELMET_TEXTURE));
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
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);

        ResourceLocation sunTex = MAVUIKA_SUN_AURA_TEXTURE;
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

        vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
        vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
        vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
        vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);

        vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
        vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
        vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
        vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(WindGliderRenderer.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);

        poseStack.popPose();
    }

    public static void renderChargeHud(GuiGraphics guiGraphics, int sw, int sh, float pct) {
        ResourceLocation sunsTex = MAVUIKA_SUN_CHARGE_TEXTURE;
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

    public static void renderFirstPersonSunglassesHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        if (mc.player.isUsingItem() || mc.player.swinging || mc.player.getVehicle() instanceof FlamestriderBikeEntity) return;
        int id = mc.player.getId();
        if (!ClientAuraTracker.isMavuikaIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.02F) return;

        float elapsedSec = (d != null && d.mavuikaIdleStartMs > 0L)
                ? Math.max(0.0F, (System.currentTimeMillis() - d.mavuikaIdleStartMs) / 1000.0F)
                : 2.0F;
        float putOn = WindGliderRenderer.smoothStep01((elapsedSec - 0.45F) / 0.65F);
        float alphaFrac = fade * putOn;
        if (alphaFrac <= 0.02F) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();

        // Subtle sunglasses screen tint/darkening without obstructive frame geometry
        int dimA = Math.round(52.0F * alphaFrac);
        int tintCol = (dimA << 24) | 0x140e18;
        guiGraphics.fill(0, 0, sw, sh, tintCol);
    }

    public static void renderMavuikaNightsoulHud(GuiGraphics guiGraphics, Minecraft mc) {
        renderFirstPersonSunglassesHud(guiGraphics, mc);
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
        // Lock rider body yaw to motorcycle yaw so hands stay locked onto the handlebars during turns
        entity.yBodyRot = bike.getYRot();
        entity.yBodyRotO = bike.yRotO;
        // Lower seated posture onto the bike saddle and place feet down onto footpegs / brake pedal
        float seatDrop = 5.2F;
        model.head.y += seatDrop;
        model.hat.y += seatDrop;
        model.body.y += seatDrop;
        model.rightArm.y += seatDrop;
        model.leftArm.y += seatDrop;
        model.rightLeg.y += seatDrop;
        model.leftLeg.y += seatDrop;
        model.rightLeg.xRot = -0.52F;
        model.leftLeg.xRot = -0.52F;
        model.rightLeg.yRot = 0.26F;
        model.leftLeg.yRot = -0.26F;
        model.rightLeg.zRot = 0.16F;
        model.leftLeg.zRot = -0.16F;
        HumanoidArm arm = WindGliderRenderer.resolveStaffArm(entity, ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS);
        float dir = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        ModelPart weaponArm = (arm == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        ModelPart handleArm = (arm == HumanoidArm.RIGHT) ? model.leftArm : model.rightArm;

        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        int ultiT = bike.getUltiTicks();
        int step = bike.getActiveNaStep();
        int naT = bike.getActiveNaTicks();
        if (ultiT > 0) {
            float ultiTf = Math.max(0.0F, (float) ultiT - pt);
            handleArm.xRot = -0.96F;
            handleArm.yRot = -0.14F * dir;
            handleArm.zRot = 0.12F * dir;
            if (ultiTf >= 19.0F) {
                float p1 = WindGliderRenderer.smoothStep01((30.0F - ultiTf) / 11.0F);
                model.body.xRot = Mth.lerp(p1, 0.22F, 0.04F);
                model.body.yRot = 0.28F * p1 * dir;
                weaponArm.xRot = Mth.lerp(p1, -1.15F, -1.48F);
                weaponArm.yRot = Mth.lerp(p1, -0.16F, -1.05F) * dir;
                weaponArm.zRot = Mth.lerp(p1, 0.05F, -0.35F) * dir;
            } else if (ultiTf > 8.0F) {
                float p2 = (19.0F - ultiTf) / 11.0F;
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
                float rec = WindGliderRenderer.smoothStep01((8.0F - ultiTf) / 8.0F);
                model.body.xRot = Mth.lerp(rec, 0.32F, 0.22F);
                model.body.yRot = Mth.lerp(rec, -0.42F, 0.0F) * dir;
                weaponArm.xRot = Mth.lerp(rec, -1.28F, -1.15F);
                weaponArm.yRot = Mth.lerp(rec, 1.05F, -0.16F) * dir;
                weaponArm.zRot = Mth.lerp(rec, 0.72F, 0.05F) * dir;
            }
            smoothBikeWeaponArm(entity.getId(), weaponArm);
            return true;
        }
        if (bike.isSpinning()) {
            // Genshin Donut Spin posture: torso banked into turn, weapon arm extended outward dragging Claymore along ground
            model.body.xRot = 0.26F;
            model.body.yRot = -0.28F * dir;
            handleArm.xRot = -0.96F;
            handleArm.yRot = -0.14F * dir;
            handleArm.zRot = 0.12F * dir;
            weaponArm.xRot = -0.72F;
            weaponArm.yRot = -1.10F * dir;
            weaponArm.zRot = 0.46F * dir;
            smoothBikeWeaponArm(entity.getId(), weaponArm);
            return true;
        }
        model.body.xRot = 0.28F;
        model.body.yRot = 0.0F;
        handleArm.xRot = -0.94F;
        handleArm.yRot = 0.10F * dir;
        handleArm.zRot = -0.10F * dir;
        float naTf = bike.getSmoothNaTicks(pt);
        if (step == 6 && naT > 0) {
            float p = bike.getFinisherProgress(pt);
            if (p < 0.36F) {
                // Phase 1: Continuous rise from Donut Spin / riding posture up to overhead apex
                float w = 0.5F - 0.5F * Mth.cos((p / 0.36F) * (float) Math.PI);
                model.body.xRot = Mth.lerp(w, 0.26F, 0.08F);
                model.body.yRot = Mth.lerp(w, -0.22F, 0.26F) * dir;
                weaponArm.xRot = Mth.lerp(w, -0.95F, -2.68F);
                weaponArm.yRot = Mth.lerp(w, -0.75F, -0.52F) * dir;
                weaponArm.zRot = Mth.lerp(w, 0.32F, -0.26F) * dir;
            } else if (p < 0.72F) {
                // Phase 2: Single unbroken accelerated overhead slam cleave all the way through impact
                float s = 0.5F - 0.5F * Mth.cos(((p - 0.36F) / 0.36F) * (float) Math.PI);
                model.body.xRot = Mth.lerp(s, 0.08F, 0.34F);
                model.body.yRot = Mth.lerp(s, 0.26F, -0.36F) * dir;
                weaponArm.xRot = Mth.lerp(s, -2.68F, -0.78F);
                weaponArm.yRot = Mth.lerp(s, -0.52F, 1.02F) * dir;
                weaponArm.zRot = Mth.lerp(s, -0.26F, 0.70F) * dir;
            } else {
                // Phase 3: Smooth recovery back to neutral handlebar posture
                float r = 0.5F - 0.5F * Mth.cos(((p - 0.72F) / 0.28F) * (float) Math.PI);
                model.body.xRot = Mth.lerp(r, 0.34F, 0.28F);
                model.body.yRot = Mth.lerp(r, -0.36F, 0.0F) * dir;
                weaponArm.xRot = Mth.lerp(r, -0.78F, -1.15F);
                weaponArm.yRot = Mth.lerp(r, 1.02F, -0.16F) * dir;
                weaponArm.zRot = Mth.lerp(r, 0.70F, 0.05F) * dir;
            }
        } else if (step == 7 && naT > 0) {
            // Hold E Mount 180-deg tail whip: counterbalance torso & sweep sword wide
            float s = WindGliderRenderer.smoothStep01(1.0F - (naTf / 10.0F));
            float w = Mth.sin(s * (float) Math.PI);
            model.body.xRot = 0.28F - 0.08F * w;
            model.body.yRot = Mth.lerp(s, 0.32F, -0.28F) * w * dir;
            weaponArm.xRot = Mth.lerp(s, -1.45F, -0.85F);
            weaponArm.yRot = Mth.lerp(s, -0.95F, 0.65F) * dir;
            weaponArm.zRot = Mth.lerp(s, -0.22F, 0.42F) * dir;
        } else if (step == 1 && naT > 0) {
            float s = WindGliderRenderer.smoothStep01(1.0F - (naTf / 10.0F));
            model.body.yRot = Mth.lerp(s, 0.22F, -0.24F) * dir;
            weaponArm.xRot = Mth.lerp(s, -2.25F, -0.45F);
            weaponArm.yRot = Mth.lerp(s, -0.75F, 0.55F) * dir;
            weaponArm.zRot = Mth.lerp(s, -0.18F, 0.35F) * dir;
        } else if (step == 2 && naT > 0) {
            float s = WindGliderRenderer.smoothStep01(1.0F - (naTf / 10.0F));
            model.body.yRot = Mth.lerp(s, -0.26F, 0.28F) * dir;
            weaponArm.xRot = Mth.lerp(s, -0.45F, -2.15F);
            weaponArm.yRot = Mth.lerp(s, 0.55F, -0.75F) * dir;
            weaponArm.zRot = Mth.lerp(s, 0.40F, -0.15F) * dir;
        } else if (step == 3 && naT > 0) {
            float s = WindGliderRenderer.smoothStep01(1.0F - (naTf / 10.0F));
            model.body.yRot = Mth.lerp(s, 0.28F, -0.32F) * dir;
            weaponArm.xRot = -1.35F;
            weaponArm.yRot = Mth.lerp(s, -0.88F, 0.78F) * dir;
            weaponArm.zRot = Mth.lerp(s, -0.20F, 0.55F) * dir;
        } else if (step == 4 && naT > 0) {
            float s = WindGliderRenderer.smoothStep01(1.0F - (naTf / 10.0F));
            model.body.xRot = Mth.lerp(s, 0.14F, 0.30F);
            model.body.yRot = Mth.lerp(s, -0.22F, 0.24F) * dir;
            weaponArm.xRot = Mth.lerp(s, -0.95F, -1.75F);
            weaponArm.yRot = Mth.lerp(s, 0.75F, -0.82F) * dir;
            weaponArm.zRot = Mth.lerp(s, 0.50F, -0.25F) * dir;
        } else if (step == 5 && naT > 0) {
            float s = WindGliderRenderer.smoothStep01(1.0F - (naTf / 14.0F));
            model.body.xRot = Mth.lerp(s, 0.12F, 0.34F);
            weaponArm.xRot = Mth.lerp(s, -2.65F, -0.30F);
            weaponArm.yRot = Mth.lerp(s, -0.60F, 0.35F) * dir;
            weaponArm.zRot = Mth.lerp(s, -0.10F, 0.25F) * dir;
        } else {
            weaponArm.xRot = -0.94F;
            weaponArm.yRot = -0.10F * dir;
            weaponArm.zRot = 0.10F * dir;
        }
        smoothBikeWeaponArm(entity.getId(), weaponArm);
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
        if (entity.getVehicle() instanceof FlamestriderBikeEntity bike
                && bike.getUltiTicks() <= 0 && !bike.isSpinning() && bike.getActiveNaTicks() <= 0 && !bike.isPlunging()) {
            poseStack.scale(0.0F, 0.0F, 0.0F);
            return true;
        }
        if (effKind != ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) return false;
        if (!(entity.getVehicle() instanceof FlamestriderBikeEntity bike)) return false;
        int ultiT = bike.getUltiTicks();
        int naT = bike.getActiveNaTicks();
        boolean activeAttack = ultiT > 0 || naT > 0 || bike.isSpinning() || bike.isPlunging();
        float sign = leftHand ? -1.0F : 1.0F;
        if (!activeAttack) {
            float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
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
        if (ultiT <= 0 && bike.isSpinning() && !bike.isPlunging()) {
            // Genshin Donut Spin: Claymore extended outward and angled down so the fiery blade drags along the ground
            poseStack.translate(0.04F * sign, -0.06F, -0.14F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(68.0F * sign));
            poseStack.mulPose(Axis.YP.rotationDegrees(-28.0F * sign));
            return true;
        }
        if (ultiT <= 0 && naT > 0 && !bike.isSpinning() && !bike.isPlunging()) {
            float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            int step = bike.getActiveNaStep();
            float total = (step == 6 || step == 5) ? 14.0F : 10.0F;
            float nt = Mth.clamp(1.0F - (bike.getSmoothNaTicks(pt) / total), 0.0F, 1.0F);
            if (step == 6) {
                float p = bike.getFinisherProgress(pt);
                if (p < 0.36F) {
                    float w = 0.5F - 0.5F * Mth.cos((p / 0.36F) * (float) Math.PI);
                    poseStack.translate(Mth.lerp(w, 0.04F, 0.0F) * sign, Mth.lerp(w, -0.04F, 0.14F), Mth.lerp(w, -0.12F, -0.10F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(w, -20.0F, -34.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(w, 64.0F, 76.0F) * sign));
                    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(w, -24.0F, -26.0F) * sign));
                } else if (p < 0.72F) {
                    float s = 0.5F - 0.5F * Mth.cos(((p - 0.36F) / 0.36F) * (float) Math.PI);
                    poseStack.translate(0.0F, Mth.lerp(s, 0.14F, -0.04F), Mth.lerp(s, -0.10F, -0.16F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, -34.0F, -48.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(s, 76.0F, 80.0F) * sign));
                    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, -26.0F, 28.0F) * sign));
                } else {
                    float r = 1.0F - (0.5F - 0.5F * Mth.cos(((p - 0.72F) / 0.28F) * (float) Math.PI));
                    poseStack.translate(0.0F, -0.04F * r, -0.16F * r);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-48.0F * r));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F * sign * r));
                    poseStack.mulPose(Axis.YP.rotationDegrees(28.0F * sign * r));
                }
            } else if (step == 3 || step == 4 || step == 7) {
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

    public static boolean setupFirstPersonBikeArmPose(
            AbstractClientPlayer player,
            float partialTicks,
            HumanoidArm arm,
            PoseStack poseStack
    ) {
        if (player == null || !(player.getVehicle() instanceof FlamestriderBikeEntity bike)) return false;
        if (bike.getNaTicks() > 0 || bike.isSpinning() || bike.isPlunging() || bike.getUltiTicks() > 0) return false;
        // Smoothly lock 1st-person hands onto the bike's handlebars without tick-sync jitter when looking around
        float targetPitch = player.getViewXRot(partialTicks);
        float camYaw = player.getViewYRot(partialTicks);
        float bikeYaw = Mth.rotLerp(partialTicks, bike.yRotO, bike.getYRot());
        float targetRelYaw = Mth.wrapDegrees(camYaw - bikeYaw);
        float targetLean = Mth.lerp(partialTicks, bike.prevSpinLean, bike.spinLean);

        long nowMs = System.currentTimeMillis();
        float[] cs = BIKE_FP_CAM_SMOOTH.get(player.getId());
        float camPitch = targetPitch;
        float relYaw = targetRelYaw;
        float spinLean = targetLean;
        if (cs == null) {
            BIKE_FP_CAM_SMOOTH.put(player.getId(), new float[]{targetPitch, targetRelYaw, targetLean, (float) (nowMs & 0xFFFFFFL)});
        } else {
            long prevLow = (long) cs[3];
            long curLow = nowMs & 0xFFFFFFL;
            float dt = ((curLow - prevLow) & 0xFFFFFFL) / 1000.0F;
            if (dt > 0.0005F) {
                dt = Mth.clamp(dt, 0.001F, 0.05F);
                float alpha = 1.0F - (float) Math.exp(-26.0F * dt);
                cs[0] = Mth.lerp(alpha, cs[0], targetPitch);
                cs[1] = cs[1] + Mth.wrapDegrees(targetRelYaw - cs[1]) * alpha;
                cs[2] = Mth.lerp(alpha, cs[2], targetLean);
                cs[3] = (float) curLow;
            }
            camPitch = cs[0];
            relYaw = cs[1];
            spinLean = cs[2];
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(camPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(relYaw));
        if (Math.abs(spinLean) > 0.05F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(spinLean));
        }

        float armDir = (arm == HumanoidArm.RIGHT) ? -1.0F : 1.0F;
        // Pull both hands inward & lower onto the gold handlebar grips, rock-solid with zero vibration
        poseStack.translate(armDir * 0.24F, -0.10F, -0.76F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-76.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(armDir * 6.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-armDir * 14.0F));
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
            int naT = bike.getActiveNaTicks();
            if (bike.isSpinning()) {
                float spinBob = Mth.sin(((float) bike.tickCount + partialTicks) * 0.65F) * 0.025F;
                poseStack.translate(dir * 0.14F, -0.12F + spinBob, -0.18F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 54.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 12.0F));
                return true;
            }
            if (naT <= 0 && !bike.isPlunging()) {
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
            if (naT > 0) {
                int step = bike.getActiveNaStep();
                float total = (step == 6 || step == 5) ? 14.0F : 10.0F;
                float nt = Mth.clamp(1.0F - (bike.getSmoothNaTicks(partialTicks) / total), 0.0F, 1.0F);
                float w = Mth.sin(nt * (float) Math.PI);
                if (step == 6) {
                    float p = bike.getFinisherProgress(partialTicks);
                    if (p < 0.36F) {
                        // Continuous lift from Donut Spin posture to overhead windup apex
                        float lift = 0.5F - 0.5F * Mth.cos((p / 0.36F) * (float) Math.PI);
                        poseStack.translate(
                                dir * Mth.lerp(lift, 0.14F, -0.32F),
                                Mth.lerp(lift, -0.12F, 0.36F),
                                Mth.lerp(lift, -0.18F, -0.24F)
                        );
                        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(lift, -24.0F, -28.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(lift, 54.0F, 68.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(lift, 12.0F, 16.0F)));
                    } else if (p < 0.76F) {
                        // Unbroken diagonal-overhead cleave slam across the screen
                        float s = 0.5F - 0.5F * Mth.cos(((p - 0.36F) / 0.40F) * (float) Math.PI);
                        poseStack.translate(
                                dir * Mth.lerp(s, -0.32F, 0.40F),
                                Mth.lerp(s, 0.36F, -0.20F),
                                -0.24F - 0.12F * Mth.sin(s * (float) Math.PI)
                        );
                        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, -28.0F, -54.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(s, 68.0F, -74.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(s, 16.0F, -22.0F)));
                    } else {
                        // Smooth follow-through fade
                        float r = 1.0F - (0.5F - 0.5F * Mth.cos(((p - 0.76F) / 0.24F) * (float) Math.PI));
                        poseStack.translate(dir * 0.40F * r, -0.20F * r, -0.24F * r);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-54.0F * r));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * 74.0F * r));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 22.0F * r));
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

    private static float stepIdleFade(ClientAuraTracker.AuraDisplayData d, long nowMs) {
        if (d == null) return 0.0F;
        if (d.mavuikaIdleLastMs <= 0L) {
            d.mavuikaIdleLastMs = nowMs;
            if (d.mavuikaIdle) {
                d.mavuikaIdleFade = Math.min(1.0F, d.mavuikaIdleFade + 0.016F / 0.90F);
            }
        } else if (nowMs > d.mavuikaIdleLastMs) {
            float dt = Math.min(0.1F, (nowMs - d.mavuikaIdleLastMs) / 1000.0F);
            d.mavuikaIdleLastMs = nowMs;
            if (d.mavuikaIdle) {
                d.mavuikaIdleFade = Math.min(1.0F, d.mavuikaIdleFade + dt / 0.90F);
            } else {
                d.mavuikaIdleFade = Math.max(0.0F, d.mavuikaIdleFade - dt / 0.35F);
            }
        }
        return WindGliderRenderer.smoothStep01(d.mavuikaIdleFade);
    }

    public static float getIdleCameraOffsetY(net.minecraft.world.entity.Entity entity, float partialTick) {
        if (!(entity instanceof LivingEntity le) || le.isUsingItem() || le.swinging || le.getVehicle() instanceof FlamestriderBikeEntity) return 0.0F;
        if (!ClientAuraTracker.isMavuikaIdle(le.getId())) return 0.0F;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(le.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F) return 0.0F;
        return -0.24F * w;
    }

    public static boolean applyIdleHumanoidPose(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (!ClientAuraTracker.isMavuikaIdle(entity.getId()) || entity.getVehicle() instanceof FlamestriderBikeEntity) {
            ClientAuraTracker.AuraDisplayData dOff = ClientAuraTracker.get(entity.getId());
            if (dOff != null) dOff.mavuikaIdleYawSet = false;
            return false;
        }
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(entity.getId());
        float w = stepIdleFade(d, System.currentTimeMillis());
        if (w <= 0.01F) {
            if (d != null && !d.mavuikaIdle) d.mavuikaIdleYawSet = false;
            return false;
        }


        // Sitting/leaning right in the middle of the tilted Flamestrider bike
        float leanBack = -0.12F * w;
        float sitDropY = 1.6F * w;
        float shiftZ = 1.4F * w;
        model.body.y += sitDropY;
        model.head.y += sitDropY;
        model.hat.y += sitDropY;
        model.rightArm.y += sitDropY;
        model.leftArm.y += sitDropY;
        model.rightLeg.y += sitDropY;
        model.leftLeg.y += sitDropY;
        model.body.z += shiftZ;
        model.head.z += shiftZ;
        model.hat.z += shiftZ;
        model.rightArm.z += shiftZ;
        model.leftArm.z += shiftZ;
        model.rightLeg.z += shiftZ;
        model.leftLeg.z += shiftZ;

        model.body.xRot = Mth.lerp(w, model.body.xRot, leanBack);
        model.head.yRot = Mth.lerp(w, Mth.clamp(model.head.yRot, -0.45F, 0.45F), 0.26F);
        model.head.xRot = Mth.lerp(w, model.head.xRot, 0.08F);
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        // Right arm resting back on the tank/handlebar
        model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, 0.42F);
        model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.26F);
        // Left arm: appear with sunglasses -> raise to eyes (0.45s..1.15s) -> lower back to hip (1.15s..1.55s)
        float elapsedSec = (d != null && d.mavuikaIdleStartMs > 0L)
                ? Math.max(0.0F, (System.currentTimeMillis() - d.mavuikaIdleStartMs) / 1000.0F)
                : 2.0F;
        float raiseW = WindGliderRenderer.smoothStep01((elapsedSec - 0.30F) / 0.55F)
                * (1.0F - WindGliderRenderer.smoothStep01((elapsedSec - 1.15F) / 0.40F));
        float lArmX = Mth.lerp(raiseW, 0.14F, -1.72F);
        float lArmY = Mth.lerp(raiseW, 0.0F, 0.36F);
        float lArmZ = Mth.lerp(raiseW, -0.22F, -0.18F);
        model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, lArmX);
        model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, lArmY);
        model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, lArmZ);
        // One leg crossed/extended forward, other supporting
        model.rightLeg.xRot = Mth.lerp(w, model.rightLeg.xRot, -0.44F);
        model.rightLeg.yRot = Mth.lerp(w, model.rightLeg.yRot, -0.20F);
        model.leftLeg.xRot = Mth.lerp(w, model.leftLeg.xRot, -0.16F);
        model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.06F);
        return true;
    }

    public static void renderIdleSunglasses(
            LivingEntity entity,
            EntityModel<?> model,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        int id = entity.getId();
        if (!ClientAuraTracker.isMavuikaIdle(id)) return;
        if (entity.getVehicle() instanceof FlamestriderBikeEntity) return;
        if (!(model instanceof HumanoidModel<?> humanoid)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.02F) return;

        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        float entityScale = entity.getScale();
        poseStack.scale(-entityScale, -entityScale, entityScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        humanoid.head.translateAndRotate(poseStack);
        float elapsedSec = (d != null && d.mavuikaIdleStartMs > 0L)
                ? Math.max(0.0F, (System.currentTimeMillis() - d.mavuikaIdleStartMs) / 1000.0F)
                : 2.0F;
        float glassAppear = WindGliderRenderer.smoothStep01(elapsedSec / 0.45F);
        float putOn = WindGliderRenderer.smoothStep01((elapsedSec - 0.45F) / 0.65F);
        float gScale = fade * glassAppear;
        if (gScale <= 0.02F) {
            poseStack.popPose();
            return;
        }
        float invPut = 1.0F - putOn;
        if (invPut > 0.001F) {
            poseStack.translate(0.24F * invPut, 0.34F * invPut, -0.22F * invPut);
            poseStack.mulPose(Axis.XP.rotationDegrees(26.0F * invPut));
            poseStack.mulPose(Axis.YP.rotationDegrees(-32.0F * invPut));
        }
        poseStack.scale(gScale, gScale, gScale);

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(ClientAuraRenderer.getMeteorSolid());

        // Dark tinted angular sunglasses lenses (positioned squarely over the eyes)
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.235F, -0.190F, -0.285F, -0.032F, -0.092F, -0.252F, 18, 16, 24, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, 0.032F, -0.190F, -0.285F, 0.235F, -0.092F, -0.252F, 18, 16, 24, 255);
        // Golden nose bridge & top brow frame
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.250F, -0.208F, -0.292F, 0.250F, -0.184F, -0.248F, 255, 198, 45, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.036F, -0.188F, -0.288F, 0.036F, -0.145F, -0.250F, 255, 198, 45, 255);
        // Side temples with fiery orange accent
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, -0.265F, -0.195F, -0.275F, -0.242F, -0.135F, 0.060F, 255, 115, 20, 255);
        ZhongliVisualRenderer.drawJadeArmorBox(vc, mat, 0.242F, -0.195F, -0.275F, 0.265F, -0.135F, 0.060F, 255, 115, 20, 255);

        poseStack.popPose();
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(ClientAuraRenderer.getMeteorSolid());
        }
    }

    public static void renderFirstPersonIdleBike(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camPos, long now) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.player.isUsingItem() || mc.player.swinging || mc.player.getVehicle() instanceof FlamestriderBikeEntity) return;
        int id = mc.player.getId();
        if (!ClientAuraTracker.isMavuikaIdle(id)) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.02F) return;

        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float bodyYaw = (d != null && d.mavuikaIdleYawSet) ? d.mavuikaIdleYaw : Mth.rotLerp(pt, mc.player.yBodyRotO, mc.player.yBodyRot);
        ClientAuraTracker.MavuikaDisplayState mState = ClientAuraTracker.getMavuika(id);
        boolean neth = mState != null && mState.netherite;
        Vec3 pPos = mc.player.getPosition(pt).subtract(camPos);

        poseStack.pushPose();
        poseStack.translate(pPos.x, pPos.y, pPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.translate(0.16F, 0.0F, -0.47F);
        poseStack.mulPose(Axis.YP.rotationDegrees(72.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F));
        poseStack.scale(fade, fade, fade);
        com.velorise.staves.client.FlamestriderBikeRenderer.renderBikeMesh(
                poseStack, bufferSource, mc.font, neth, 0.0F, 0.0F, 0.0F, false, false);
        poseStack.popPose();
    }

    public static void renderIdleBike(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        int id = entity.getId();
        if (!ClientAuraTracker.isMavuikaIdle(id)) return;
        if (entity.getVehicle() instanceof FlamestriderBikeEntity) return;
        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
        float fade = stepIdleFade(d, System.currentTimeMillis());
        if (fade <= 0.02F) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && id == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (d != null && !d.mavuikaIdleYawSet) {
            d.mavuikaIdleYaw = entity.yBodyRot;
            d.mavuikaIdleYawSet = true;
        }
        float bodyYaw = (d != null && d.mavuikaIdleYawSet) ? d.mavuikaIdleYaw : entity.yBodyRot;
        ClientAuraTracker.MavuikaDisplayState mState = ClientAuraTracker.getMavuika(id);
        boolean neth = mState != null && mState.netherite;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.translate(0.16F, 0.0F, -0.47F);
        poseStack.mulPose(Axis.YP.rotationDegrees(72.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F));
        poseStack.scale(fade, fade, fade);

        com.velorise.staves.client.FlamestriderBikeRenderer.renderBikeMesh(
                poseStack,
                buffer,
                mc.font,
                neth,
                0.0F,
                0.0F,
                0.0F,
                false,
                false
        );

        poseStack.popPose();
    }

    private static void smoothBikeWeaponArm(int entityId, ModelPart weaponArm) {
        long nowMs = System.currentTimeMillis();
        float[] st = BIKE_ARM_SMOOTH.get(entityId);
        if (st == null) {
            BIKE_ARM_SMOOTH.put(entityId, new float[]{weaponArm.xRot, weaponArm.yRot, weaponArm.zRot, (float) (nowMs & 0xFFFFFFL)});
            return;
        }
        long prevLow = (long) st[3];
        long curLow = nowMs & 0xFFFFFFL;
        long diffMs = (curLow - prevLow) & 0xFFFFFFL;
        if (diffMs == 0L) {
            weaponArm.xRot = st[0];
            weaponArm.yRot = st[1];
            weaponArm.zRot = st[2];
            return;
        }
        float dt = Mth.clamp(diffMs / 1000.0F, 0.001F, 0.08F);
        float alpha = 1.0F - (float) Math.exp(-32.0F * dt);
        st[0] = Mth.lerp(alpha, st[0], weaponArm.xRot);
        st[1] = Mth.lerp(alpha, st[1], weaponArm.yRot);
        st[2] = Mth.lerp(alpha, st[2], weaponArm.zRot);
        st[3] = (float) curLow;
        weaponArm.xRot = st[0];
        weaponArm.yRot = st[1];
        weaponArm.zRot = st[2];
    }
}
