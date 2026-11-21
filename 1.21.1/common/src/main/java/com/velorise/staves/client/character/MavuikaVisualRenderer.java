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
        com.velorise.staves.fsm.BikePoseBlender.clearAll();
    }
    private static final ResourceLocation MAVUIKA_HELMET_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/mavuika_helmet_texture.png");
    private static final ResourceLocation MAVUIKA_SUN_AURA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/mavuika_sun_aura.png");
    private static final ResourceLocation MAVUIKA_SUN_CHARGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/mavuika_sun_charge.png");
    private static final ResourceLocation MAVUIKA_NIGHTSOUL_FLAME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/mavuika_nightsoul_flame.png");
    private static final ResourceLocation MAVUIKA_NIGHTSOUL_FILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/mavuika_nightsoul_fill.png");
    private static final ResourceLocation MAVUIKA_NIGHTSOUL_FRAME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/mavuika_nightsoul_frame.png");
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

        float ultiCdTicks = state.effectiveUltiCooldownTicks();
        boolean cdReady = ultiCdTicks <= 0.0F;
        boolean qReady = points >= 5.0F && cdReady;

        if (auraTicks > 0.0F) {
            float fuelFrac = Mth.clamp(state.nightsoulFuel() / state.maxNightsoulFuel(), 0.0F, 1.0F);
            int wingSize = 54;
            int wingX = Math.round(cx) - 48;
            int wingY = Math.round(cy) - wingSize / 2;
            long nowMs = System.currentTimeMillis();
            float flicker = 0.88F + 0.12F * Mth.sin(nowMs * 0.017F);
            float flameBobY = Mth.sin(nowMs * 0.012F) * 0.9F;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 1. Outer blazing Nightsoul flame aura behind the wing (animated flicker & gentle sway)
            if (fuelFrac > 0.02F) {
                float flameAlpha = Mth.clamp(0.45F + 0.55F * fuelFrac, 0.38F, 1.0F) * flicker;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0.0F, flameBobY, 0.0F);
                RenderSystem.setShaderColor(1.0F, 0.96F + 0.04F * flicker, 0.92F, flameAlpha);
                guiGraphics.blit(MAVUIKA_NIGHTSOUL_FLAME_TEXTURE, wingX, wingY, wingSize, wingSize, 0.0F, 0.0F, 128, 128, 128, 128);
                guiGraphics.pose().popPose();
            }

            // 2. Dark warm-maroon obsidian empty trough backing (exact empty interior color from Genshin)
            RenderSystem.setShaderColor(0.44F, 0.22F, 0.20F, 0.92F);
            guiGraphics.blit(MAVUIKA_NIGHTSOUL_FILL_TEXTURE, wingX, wingY, wingSize, wingSize, 0.0F, 0.0F, 128, 128, 128, 128);

            // 3. Dynamic inner fiery fill clipped smoothly bottom-to-top within active wing bounds (v: 22..114 of 128)
            if (fuelFrac > 0.005F) {
                float activeTopV = 22.0F;
                float activeBotV = 114.0F;
                float clipV = Mth.lerp(fuelFrac, activeBotV, activeTopV);
                int srcV = Math.round(clipV);
                int srcH = Math.max(1, 128 - srcV);
                int dstOffsetY = Math.round((srcV / 128.0F) * wingSize);
                int dstH = Math.max(1, wingSize - dstOffsetY);
                RenderSystem.setShaderColor(1.0F, 0.97F + 0.03F * flicker, 0.94F, 0.98F);
                guiGraphics.blit(MAVUIKA_NIGHTSOUL_FILL_TEXTURE, wingX, wingY + dstOffsetY, wingSize, dstH, 0.0F, (float) srcV, 128, srcH, 128, 128);
            }

            // 4. Crisp 128x128 Natlan wing frame, spiky diagonal crimson ribs, and concave golden horn trim on top
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(MAVUIKA_NIGHTSOUL_FRAME_TEXTURE, wingX, wingY, wingSize, wingSize, 0.0F, 0.0F, 128, 128, 128, 128);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

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
        // 3rd-person head/neck fix: align head smoothly along bike nose (never twist backward like an owl when orbiting camera behind/front); 1st-person unchanged
        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPerson = mc != null && mc.options != null && mc.options.getCameraType().isFirstPerson();
        boolean thirdPersonOrRemote = !(entity == mc.player && isFirstPerson);
        if (thirdPersonOrRemote) {
            float relHeadYaw = Mth.wrapDegrees(entity.yHeadRot - bike.getYRot());
            float clampedYawDeg;
            if (Math.abs(relHeadYaw) <= 75.0F) {
                clampedYawDeg = Mth.clamp(relHeadYaw, -28.0F, 28.0F);
            } else {
                float tBack = Mth.clamp((Math.abs(relHeadYaw) - 75.0F) / 45.0F, 0.0F, 1.0F);
                clampedYawDeg = Mth.lerp(tBack, Math.signum(relHeadYaw) * 28.0F, 0.0F);
            }
            model.head.yRot = clampedYawDeg * ((float) Math.PI / 180.0F);
            model.head.xRot = Mth.clamp(model.head.xRot, -0.32F, 0.32F);
            model.hat.yRot = model.head.yRot;
            model.hat.xRot = model.head.xRot;
        }
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
            float t = Mth.clamp(40.0F - ultiTf, 0.0F, 40.0F);
            if (t < 8.0F) {
                // Phase 1 (Launch into Sun): Low aero tuck, both hands firmly gripping handlebars
                float e = WindGliderRenderer.smoothStep01(t / 8.0F);
                model.body.xRot = Mth.lerp(e, 0.28F, 0.42F);
                model.body.yRot = 0.0F;
                handleArm.xRot = -1.05F;
                handleArm.yRot = -0.10F * dir;
                handleArm.zRot = 0.10F * dir;
                weaponArm.xRot = -1.05F;
                weaponArm.yRot = -0.10F * dir;
                weaponArm.zRot = 0.10F * dir;
            } else if (t < 15.0F) {
                // Phase 2 (Mid-air Eject off bike): Acrobatic aerial tuck, knees bent, arms wide for balance
                float e = WindGliderRenderer.smoothStep01((t - 8.0F) / 7.0F);
                model.body.xRot = Mth.lerp(e, 0.42F, -0.12F);
                model.body.yRot = Mth.lerp(e, 0.0F, 0.25F) * dir;
                model.rightLeg.xRot = Mth.lerp(e, -0.52F, -0.82F);
                model.leftLeg.xRot = Mth.lerp(e, -0.52F, 0.42F);
                handleArm.xRot = Mth.lerp(e, -1.05F, -0.65F);
                handleArm.zRot = Mth.lerp(e, 0.10F, -0.78F) * dir;
                weaponArm.xRot = Mth.lerp(e, -1.05F, -2.35F);
                weaponArm.yRot = Mth.lerp(e, -0.10F, -0.35F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.10F, 0.28F) * dir;
            } else if (t < 22.0F) {
                // Phase 3 (Meteor Dive & Claymore Draw): Right arm raised high behind shoulder, left arm outstretched for balance
                float e = WindGliderRenderer.smoothStep01((t - 15.0F) / 7.0F);
                model.body.xRot = Mth.lerp(e, -0.12F, 0.16F);
                model.body.yRot = Mth.lerp(e, 0.25F, 0.62F) * dir;
                model.rightLeg.xRot = Mth.lerp(e, -0.82F, -0.35F);
                model.leftLeg.xRot = Mth.lerp(e, 0.42F, 0.22F);
                handleArm.xRot = -0.55F;
                handleArm.zRot = -0.95F * dir;
                weaponArm.xRot = Mth.lerp(e, -2.35F, -2.65F);
                weaponArm.yRot = Mth.lerp(e, -0.35F, -0.85F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.28F, -0.32F) * dir;
            } else if (t < 26.0F) {
                // Phase 4 (Sunfell Slice 360-deg horizontal cleave): Explosive full-body sweep
                float u = (t - 22.0F) / 4.0F;
                float e = 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
                model.body.xRot = Mth.lerp(e, 0.16F, 0.24F);
                model.body.yRot = Mth.lerp(e, 0.62F, -0.68F) * dir;
                model.rightLeg.xRot = Mth.lerp(e, -0.35F, -0.52F);
                model.leftLeg.xRot = Mth.lerp(e, 0.22F, -0.52F);
                handleArm.xRot = Mth.lerp(e, -0.55F, -0.94F);
                handleArm.zRot = Mth.lerp(e, -0.95F, 0.10F) * dir;
                weaponArm.xRot = Mth.lerp(e, -2.65F, -0.75F);
                weaponArm.yRot = Mth.lerp(e, -0.85F, 0.95F) * dir;
                weaponArm.zRot = Mth.lerp(e, -0.32F, 0.65F) * dir;
            } else if (t < 34.0F) {
                // Phase 5 (Iconic Sideways Wheelie Hold): Left hand grips handlebar, right arm extends straight back-down pointing claymore behind rear wheel!
                float e = t < 28.0F ? WindGliderRenderer.smoothStep01((t - 26.0F) / 2.0F) : 1.0F;
                model.body.xRot = 0.14F;
                model.body.yRot = Mth.lerp(e, -0.68F, 0.22F) * dir;
                handleArm.xRot = -0.94F;
                handleArm.yRot = -0.10F * dir;
                handleArm.zRot = 0.10F * dir;
                weaponArm.xRot = Mth.lerp(e, -0.75F, 0.42F);
                weaponArm.yRot = Mth.lerp(e, 0.95F, -0.38F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.65F, -0.46F) * dir;
            } else {
                // Phase 6 (Smooth Wheelie Touchdown): Return right arm smoothly to handlebar
                float e = WindGliderRenderer.smoothStep01((t - 34.0F) / 6.0F);
                model.body.xRot = Mth.lerp(e, 0.14F, 0.28F);
                model.body.yRot = Mth.lerp(e, 0.22F, 0.0F) * dir;
                handleArm.xRot = -0.94F;
                handleArm.yRot = -0.10F * dir;
                handleArm.zRot = 0.10F * dir;
                weaponArm.xRot = Mth.lerp(e, 0.42F, -0.94F);
                weaponArm.yRot = Mth.lerp(e, -0.38F, -0.10F) * dir;
                weaponArm.zRot = Mth.lerp(e, -0.46F, 0.10F) * dir;
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
            if (p < 0.37F) {
                // Phase 0+1: Athletic Genshin aerial vault above bike (split legs, arched torso, Claymore raised overhead!)
                float u = p / 0.37F;
                float w = 1.0F - (1.0F - u) * (1.0F - u);
                float air = Mth.clamp((p - 0.08F) / 0.22F, 0.0F, 1.0F);
                float airS = air * air * (3.0F - 2.0F * air);
                model.body.xRot = Mth.lerp(w, 0.26F, -0.18F);
                model.body.yRot = Mth.lerp(w, -0.22F, 0.32F) * dir;
                model.rightLeg.xRot = Mth.lerp(airS, -0.52F, 0.68F);
                model.rightLeg.zRot = Mth.lerp(airS, 0.16F, 0.26F);
                model.leftLeg.xRot = Mth.lerp(airS, -0.52F, -0.88F);
                model.leftLeg.zRot = Mth.lerp(airS, -0.16F, -0.20F);
                // Both hands rise overhead gripping/charging the skyward Claymore (Genshin 10.62s..10.95s)
                handleArm.xRot = Mth.lerp(airS, -0.94F, -2.48F);
                handleArm.yRot = Mth.lerp(airS, 0.10F * dir, 0.36F * dir);
                handleArm.zRot = Mth.lerp(airS, -0.10F * dir, 0.16F * dir);
                weaponArm.xRot = Mth.lerp(w, -0.95F, -2.76F);
                weaponArm.yRot = Mth.lerp(w, -0.75F, -0.22F) * dir;
                weaponArm.zRot = Mth.lerp(w, 0.32F, -0.10F) * dir;
            } else if (p < 0.52F) {
                // Phase 2: Plunge onto saddle & execute wide HORIZONTAL LEFT-to-RIGHT sweep (Genshin 10.98s..11.12s)
                float u = (p - 0.37F) / 0.15F;
                float s = u * u;
                model.body.xRot = Mth.lerp(s, -0.18F, 0.24F);
                model.body.yRot = Mth.lerp(s, 0.42F, -0.56F) * dir;
                model.rightLeg.xRot = Mth.lerp(s, 0.68F, -0.52F);
                model.rightLeg.zRot = Mth.lerp(s, 0.26F, 0.16F);
                model.leftLeg.xRot = Mth.lerp(s, -0.88F, -0.52F);
                model.leftLeg.zRot = Mth.lerp(s, -0.20F, -0.16F);
                handleArm.xRot = Mth.lerp(s, -2.48F, -0.94F);
                handleArm.yRot = Mth.lerp(s, 0.36F * dir, 0.10F * dir);
                handleArm.zRot = Mth.lerp(s, 0.16F * dir, -0.10F * dir);
                weaponArm.xRot = Mth.lerp(s, -2.76F, -1.32F);
                weaponArm.yRot = Mth.lerp(s, -1.08F, 1.24F) * dir;
                weaponArm.zRot = Mth.lerp(s, -0.18F, 0.72F) * dir;
            } else if (p < 0.86F) {
                // Phase 3a: Iconic sideways wheelie — upright torso, right arm extended straight DOWN planting Claymore tip into ground (Genshin 11.32s..11.48s)
                float u = Mth.clamp((p - 0.52F) / 0.14F, 0.0F, 1.0F);
                float e = 1.0F - (1.0F - u) * (1.0F - u);
                model.body.xRot = Mth.lerp(e, 0.24F, 0.06F);
                model.body.yRot = Mth.lerp(e, -0.56F, -0.22F) * dir;
                weaponArm.xRot = Mth.lerp(e, -1.32F, -0.32F);
                weaponArm.yRot = Mth.lerp(e, 1.24F, 0.18F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.72F, 0.42F) * dir;
            } else {
                // Phase 3b: Clean return to riding grip
                float r = Mth.clamp((p - 0.86F) / 0.14F, 0.0F, 1.0F);
                float e = r * r * (3.0F - 2.0F * r);
                model.body.xRot = Mth.lerp(e, 0.06F, 0.28F);
                model.body.yRot = Mth.lerp(e, -0.22F, 0.0F) * dir;
                weaponArm.xRot = Mth.lerp(e, -0.32F, -0.94F);
                weaponArm.yRot = Mth.lerp(e, 0.18F, -0.10F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.42F, 0.10F) * dir;
            }
        } else if (step == 8 && naT > 0) {
            // Genshin Re-Mount catch-up: Steve hops up straddling the incoming bike, swings leg over saddle, and grabs handlebars
            float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
            if (p < 0.45F) {
                float u = WindGliderRenderer.smoothStep01(p / 0.45F);
                model.body.xRot = Mth.lerp(u, 0.08F, 0.16F);
                model.body.yRot = Mth.lerp(u, -0.18F, -0.10F);
                model.rightLeg.xRot = Mth.lerp(u, -0.18F, -0.26F);
                model.rightLeg.zRot = Mth.lerp(u, 0.64F, 0.56F);
                model.leftLeg.xRot = Mth.lerp(u, -0.88F, -0.74F);
                model.leftLeg.zRot = -0.18F;
                handleArm.xRot = Mth.lerp(u, -1.24F, -1.06F);
                handleArm.zRot = Mth.lerp(u, -0.42F * dir, -0.18F * dir);
                weaponArm.xRot = Mth.lerp(u, -1.24F, -1.06F);
                weaponArm.zRot = Mth.lerp(u, 0.42F * dir, 0.18F * dir);
            } else {
                float u = WindGliderRenderer.smoothStep01((p - 0.45F) / 0.55F);
                model.body.xRot = Mth.lerp(u, 0.16F, 0.28F);
                model.body.yRot = Mth.lerp(u, -0.10F, 0.0F);
                model.rightLeg.xRot = Mth.lerp(u, -0.26F, -0.52F);
                model.rightLeg.zRot = Mth.lerp(u, 0.56F, 0.16F);
                model.leftLeg.xRot = Mth.lerp(u, -0.74F, -0.52F);
                model.leftLeg.zRot = Mth.lerp(u, -0.18F, -0.16F);
                handleArm.xRot = Mth.lerp(u, -1.06F, -0.94F);
                handleArm.zRot = Mth.lerp(u, -0.18F * dir, -0.10F * dir);
                weaponArm.xRot = Mth.lerp(u, -1.06F, -0.94F);
                weaponArm.zRot = Mth.lerp(u, 0.18F * dir, 0.10F * dir);
            }
            smoothBikeWeaponArm(entity.getId(), weaponArm);
            return true;
        } else if (step == 7 && naT > 0) {
            // Hold E Mount 180-deg tail whip: counterbalance torso & sweep sword wide
            float dur = Math.max(1.0F, (float) com.velorise.staves.fsm.BikeFrameData.get(com.velorise.staves.fsm.BikeActionState.MOUNT_TAIL_WHIP).durationTicks());
            float s = WindGliderRenderer.smoothStep01(1.0F - (naTf / dur));
            float w = Mth.sin(s * (float) Math.PI);
            model.body.xRot = 0.28F - 0.08F * w;
            model.body.yRot = Mth.lerp(s, 0.32F, -0.28F) * w * dir;
            weaponArm.xRot = Mth.lerp(s, -1.45F, -0.85F);
            weaponArm.yRot = Mth.lerp(s, -0.95F, 0.65F) * dir;
            weaponArm.zRot = Mth.lerp(s, -0.22F, 0.42F) * dir;
        } else if (step == 1 && naT > 0) {
            // Genshin NA1 (22t): Asymmetric 3-phase Right->Left horizontal claymore cleave + held follow-through + smooth return
            float t = Mth.clamp(22.0F - naTf, 0.0F, 22.0F);
            if (t < 3.0F) {
                float e = WindGliderRenderer.smoothStep01(t / 3.0F);
                model.body.xRot = Mth.lerp(e, 0.28F, 0.18F);
                model.body.yRot = Mth.lerp(e, 0.0F, 0.48F) * dir;
                weaponArm.xRot = Mth.lerp(e, -0.94F, -1.48F);
                weaponArm.yRot = Mth.lerp(e, -0.10F, -1.18F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.10F, -0.35F) * dir;
            } else if (t < 7.0F) {
                float u = (t - 3.0F) / 4.0F;
                float e = 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
                model.body.xRot = 0.16F;
                model.body.yRot = Mth.lerp(e, 0.48F, -0.56F) * dir;
                weaponArm.xRot = Mth.lerp(e, -1.48F, -1.22F);
                weaponArm.yRot = Mth.lerp(e, -1.18F, 1.12F) * dir;
                weaponArm.zRot = Mth.lerp(e, -0.35F, 0.68F) * dir;
            } else if (t < 15.0F) {
                float e = WindGliderRenderer.smoothStep01((t - 7.0F) / 8.0F);
                model.body.xRot = Mth.lerp(e, 0.16F, 0.22F);
                model.body.yRot = Mth.lerp(e, -0.56F, -0.32F) * dir;
                weaponArm.xRot = Mth.lerp(e, -1.22F, -1.04F);
                weaponArm.yRot = Mth.lerp(e, 1.12F, 0.78F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.68F, 0.42F) * dir;
            } else {
                float e = WindGliderRenderer.smoothStep01((t - 15.0F) / 7.0F);
                model.body.xRot = Mth.lerp(e, 0.22F, 0.28F);
                model.body.yRot = Mth.lerp(e, -0.32F, 0.0F) * dir;
                weaponArm.xRot = Mth.lerp(e, -1.04F, -0.94F);
                weaponArm.yRot = Mth.lerp(e, 0.78F, -0.10F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.42F, 0.10F) * dir;
            }
        } else if (step == 2 && naT > 0) {
            // Genshin NA2 (24t): Pull claymore over left shoulder -> 360-deg Left->Right sweep -> hold low-right -> smooth return
            float t = Mth.clamp(24.0F - naTf, 0.0F, 24.0F);
            if (t < 4.0F) {
                float e = WindGliderRenderer.smoothStep01(t / 4.0F);
                model.body.xRot = 0.20F;
                model.body.yRot = Mth.lerp(e, -0.32F, -0.62F) * dir;
                weaponArm.xRot = Mth.lerp(e, -1.04F, -1.58F);
                weaponArm.yRot = Mth.lerp(e, 0.78F, 1.24F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.42F, 0.72F) * dir;
            } else if (t < 9.0F) {
                float u = (t - 4.0F) / 5.0F;
                float e = 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
                model.body.xRot = 0.18F;
                model.body.yRot = Mth.lerp(e, -0.62F, 0.58F) * dir;
                weaponArm.xRot = Mth.lerp(e, -1.58F, -0.96F);
                weaponArm.yRot = Mth.lerp(e, 1.24F, -1.16F) * dir;
                weaponArm.zRot = Mth.lerp(e, 0.72F, -0.44F) * dir;
            } else if (t < 18.0F) {
                float e = WindGliderRenderer.smoothStep01((t - 9.0F) / 9.0F);
                model.body.xRot = Mth.lerp(e, 0.18F, 0.24F);
                model.body.yRot = Mth.lerp(e, 0.58F, 0.34F) * dir;
                weaponArm.xRot = Mth.lerp(e, -0.96F, -0.82F);
                weaponArm.yRot = Mth.lerp(e, -1.16F, -0.92F) * dir;
                weaponArm.zRot = Mth.lerp(e, -0.44F, -0.30F) * dir;
            } else {
                float e = WindGliderRenderer.smoothStep01((t - 18.0F) / 6.0F);
                model.body.xRot = Mth.lerp(e, 0.24F, 0.28F);
                model.body.yRot = Mth.lerp(e, 0.34F, 0.0F) * dir;
                weaponArm.xRot = Mth.lerp(e, -0.82F, -0.94F);
                weaponArm.yRot = Mth.lerp(e, -0.92F, -0.10F) * dir;
                weaponArm.zRot = Mth.lerp(e, -0.30F, 0.10F) * dir;
            }
        } else if (step == 3 && naT > 0) {
            // Genshin NA3 (22t): NO SWORD — BOTH HANDS GRIP HANDLEBARS during 180-deg Stoppie & rear-tire slam!
            float t = Mth.clamp(22.0F - naTf, 0.0F, 22.0F);
            float kick = t < 5.5F ? WindGliderRenderer.smoothStep01(t / 5.5F)
                    : (t < 8.5F ? 1.0F - WindGliderRenderer.smoothStep01((t - 5.5F) / 3.0F) : 0.0F);
            model.body.xRot = Mth.lerp(kick, 0.28F, 0.56F);
            model.rightLeg.xRot = Mth.lerp(kick, -0.52F, 0.42F);
            model.leftLeg.xRot = Mth.lerp(kick, -0.52F, 0.42F);
            weaponArm.xRot = Mth.lerp(kick, -0.94F, -1.18F);
            weaponArm.yRot = -0.10F * dir;
            weaponArm.zRot = 0.10F * dir;
            handleArm.xRot = Mth.lerp(kick, -0.94F, -1.18F);
        } else if (step == 4 && naT > 0) {
            // Genshin NA4 (20t): NO SWORD — BOTH HANDS GRIP HANDLEBARS during -35 deg Wheelie Drift!
            float t = Mth.clamp(20.0F - naTf, 0.0F, 20.0F);
            float wheelie = t < 3.5F ? WindGliderRenderer.smoothStep01(t / 3.5F)
                    : (t < 11.0F ? 1.0F - 0.55F * WindGliderRenderer.smoothStep01((t - 3.5F) / 7.5F)
                    : 0.45F * (1.0F - WindGliderRenderer.smoothStep01((t - 11.0F) / 9.0F)));
            model.body.xRot = Mth.lerp(wheelie, 0.28F, 0.04F);
            model.body.zRot = -0.12F * wheelie;
            weaponArm.xRot = Mth.lerp(wheelie, -0.94F, -0.78F);
            weaponArm.yRot = -0.10F * dir;
            weaponArm.zRot = 0.10F * dir;
            handleArm.xRot = Mth.lerp(wheelie, -0.94F, -0.78F);
        } else if (step == 5 && naT > 0) {
            // Genshin NA5 (28t): NO SWORD — BOTH HANDS GRIP HANDLEBARS, 6t Superman trailing legs (T03.5..T09.5) -> heavy slam!
            float t = Mth.clamp(28.0F - naTf, 0.0F, 28.0F);
            float air = (t < 3.5F) ? WindGliderRenderer.smoothStep01(t / 3.5F)
                    : (t < 9.5F ? 1.0F : (t < 12.5F ? 1.0F - WindGliderRenderer.smoothStep01((t - 9.5F) / 3.0F) : 0.0F));
            model.body.xRot = Mth.lerp(air, 0.28F, -0.38F);
            model.rightLeg.xRot = Mth.lerp(air, -0.52F, 1.24F);
            model.leftLeg.xRot = Mth.lerp(air, -0.52F, 1.06F);
            model.rightLeg.zRot = Mth.lerp(air, 0.16F, 0.24F);
            model.leftLeg.zRot = Mth.lerp(air, -0.16F, -0.24F);
            handleArm.xRot = Mth.lerp(air, -0.94F, -1.78F);
            weaponArm.xRot = Mth.lerp(air, -0.94F, -1.78F);
            weaponArm.yRot = Mth.lerp(air, -0.10F, -0.14F) * dir;
            weaponArm.zRot = Mth.lerp(air, 0.10F, 0.14F) * dir;
        } else {
            weaponArm.xRot = -0.94F;
            weaponArm.yRot = -0.10F * dir;
            weaponArm.zRot = 0.10F * dir;
            // Genshin stopped UX: plant the foot on the leaned side onto the ground while both hands stay on handlebars
            float idleStop = bike.getIdleStopBlend(pt);
            if (idleStop > 0.005F) {
                float side = bike.getIdleStopSide();
                ModelPart braceLeg = (side > 0.0F) ? model.rightLeg : model.leftLeg;
                braceLeg.xRot = Mth.lerp(idleStop, -0.52F, 0.06F);
                braceLeg.zRot = Mth.lerp(idleStop, 0.16F * side, 0.34F * side);
                braceLeg.yRot = Mth.lerp(idleStop, 0.26F * side, 0.14F * side);
                braceLeg.y += 0.65F * idleStop;
                model.body.zRot = -0.05F * side * idleStop;
            } else {
                model.body.zRot = 0.0F;
            }
        }
        smoothBikeWeaponArm(entity.getId(), weaponArm);
        return true;
    }

    public static boolean applyBurstHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model, int activeAnim, float t) {
        if (activeAnim == ClientAuraTracker.ANIM_MAVUIKA_DISMOUNT) {
            // Genshin smooth aerial vault dismount: push off saddle, stylish midair side-kick, controlled soft landing
            float rise = WindGliderRenderer.smoothStep01(Math.min(1.0F, t / 0.28F));
            float descend = (t < 0.28F) ? 0.0F : WindGliderRenderer.smoothStep01(Math.min(1.0F, (t - 0.28F) / 0.42F));
            float blendOut = (t < 0.72F) ? 1.0F : 1.0F - WindGliderRenderer.smoothStep01((t - 0.72F) / 0.28F);
            float bodyX = (t < 0.28F) ? Mth.lerp(rise, 0.20F, -0.14F) : Mth.lerp(descend, -0.14F, 0.10F);
            float bodyY = (t < 0.28F) ? Mth.lerp(rise, 0.0F, -0.22F) : Mth.lerp(descend, -0.22F, 0.0F);
            float rLegX = (t < 0.28F) ? Mth.lerp(rise, -0.50F, 0.54F) : Mth.lerp(descend, 0.54F, 0.04F);
            float rLegZ = (t < 0.28F) ? Mth.lerp(rise, 0.18F, 0.36F) : Mth.lerp(descend, 0.36F, 0.04F);
            float lLegX = (t < 0.28F) ? Mth.lerp(rise, -0.50F, -0.64F) : Mth.lerp(descend, -0.64F, -0.04F);
            float lLegZ = (t < 0.28F) ? Mth.lerp(rise, -0.18F, -0.22F) : Mth.lerp(descend, -0.22F, -0.04F);
            float armX = (t < 0.28F) ? Mth.lerp(rise, -0.90F, -0.44F) : Mth.lerp(descend, -0.44F, -0.08F);
            float armZ = (t < 0.28F) ? Mth.lerp(rise, 0.14F, 0.88F) : Mth.lerp(descend, 0.88F, 0.14F);
            model.body.xRot = Mth.lerp(blendOut, model.body.xRot, bodyX);
            model.body.yRot = Mth.lerp(blendOut, model.body.yRot, bodyY);
            model.rightLeg.xRot = Mth.lerp(blendOut, model.rightLeg.xRot, rLegX);
            model.rightLeg.zRot = Mth.lerp(blendOut, model.rightLeg.zRot, rLegZ);
            model.leftLeg.xRot = Mth.lerp(blendOut, model.leftLeg.xRot, lLegX);
            model.leftLeg.zRot = Mth.lerp(blendOut, model.leftLeg.zRot, lLegZ);
            model.rightArm.xRot = Mth.lerp(blendOut, model.rightArm.xRot, armX);
            model.rightArm.zRot = Mth.lerp(blendOut, model.rightArm.zRot, armZ);
            model.leftArm.xRot = Mth.lerp(blendOut, model.leftArm.xRot, armX);
            model.leftArm.zRot = Mth.lerp(blendOut, model.leftArm.zRot, -armZ);
            return true;
        }
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
            float total = (float) com.velorise.staves.fsm.BikeFrameData.get(com.velorise.staves.fsm.BikeActionState.fromNaStep(step)).durationTicks();
            float nt = Mth.clamp(1.0F - (bike.getSmoothNaTicks(pt) / Math.max(1.0F, total)), 0.0F, 1.0F);
            if (step == 6) {
                float p = bike.getFinisherProgress(pt);
                if (p < 0.37F) {
                    // Sword points skyward/diagonally UP along raised arms (no horizontal bar behind neck!)
                    float u = p / 0.37F;
                    float w = 1.0F - (1.0F - u) * (1.0F - u);
                    poseStack.translate(Mth.lerp(w, 0.04F, 0.0F) * sign, Mth.lerp(w, -0.04F, 0.08F), Mth.lerp(w, -0.12F, -0.06F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(w, -20.0F, -10.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(w, 52.0F, 12.0F) * sign));
                    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(w, -24.0F, -8.0F) * sign));
                } else if (p < 0.52F) {
                    // Wide horizontal LEFT-to-RIGHT blade sweep parallel to ground
                    float u = (p - 0.37F) / 0.15F;
                    float s = u * u;
                    poseStack.translate(0.0F, Mth.lerp(s, 0.08F, -0.04F), Mth.lerp(s, -0.06F, -0.14F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, -10.0F, -22.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(s, 12.0F, 82.0F) * sign));
                    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, -42.0F, 52.0F) * sign));
                } else if (p < 0.86F) {
                    // Plant Claymore tip straight down toward ground during 58-deg sideways wheelie
                    float u = Mth.clamp((p - 0.52F) / 0.14F, 0.0F, 1.0F);
                    float e = 1.0F - (1.0F - u) * (1.0F - u);
                    poseStack.translate(0.0F, Mth.lerp(e, -0.04F, -0.12F), Mth.lerp(e, -0.14F, -0.06F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(e, -22.0F, 24.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(e, 82.0F, 18.0F) * sign));
                    poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(e, 52.0F, 10.0F) * sign));
                } else {
                    float r = 1.0F - Mth.clamp((p - 0.86F) / 0.14F, 0.0F, 1.0F);
                    poseStack.translate(0.0F, -0.12F * r, -0.06F * r);
                    poseStack.mulPose(Axis.XP.rotationDegrees(24.0F * r));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(18.0F * sign * r));
                    poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * sign * r));
                }
            } else if (step == 3 || step == 4 || step == 5) {
                // Genshin NA3, NA4, NA5: Claymore dissolves at T00 of NA3; both hands grip handlebars!
                poseStack.scale(0.0F, 0.0F, 0.0F);
            } else if (step == 7) {
                poseStack.translate(0.0F, 0.04F, -0.12F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-26.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(75.0F * sign));
            } else if (step == 1) {
                // Genshin NA1 (22t): Fade in T00..T01.5 -> flat horizontal cleave T03..T07 -> hold up-left -> fade out T18.5..T22
                float t = Mth.clamp(22.0F - bike.getSmoothNaTicks(pt), 0.0F, 22.0F);
                float vis = (t < 1.5F) ? Mth.lerp(t / 1.5F, 0.25F, 1.0F)
                        : (t > 18.5F ? Mth.clamp((22.0F - t) / 3.5F, 0.0F, 1.0F) : 1.0F);
                poseStack.scale(vis, vis, vis);
                float slash = (t < 3.0F) ? 0.0F : (t < 7.0F ? (t - 3.0F) / 4.0F : 1.0F);
                float bladeFlat = (t < 7.5F) ? 1.0F : Mth.clamp(1.0F - (t - 7.5F) / 4.0F, 0.0F, 1.0F);
                poseStack.translate(0.0F, 0.02F, -0.14F);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(bladeFlat, -26.0F, -16.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(bladeFlat, -34.0F, -78.0F) * sign));
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(slash, -28.0F, 24.0F) * sign));
            } else {
                // Genshin NA2 (24t): Continuous from NA1 -> flat 360-deg horizontal cleave T04..T09 -> hold low-right -> fade out T20.5..T24
                float t = Mth.clamp(24.0F - bike.getSmoothNaTicks(pt), 0.0F, 24.0F);
                float vis = (t > 20.5F) ? Mth.clamp((24.0F - t) / 3.5F, 0.0F, 1.0F) : 1.0F;
                poseStack.scale(vis, vis, vis);
                float slash = (t < 4.0F) ? 0.0F : (t < 9.0F ? (t - 4.0F) / 5.0F : 1.0F);
                float bladeFlat = (t < 9.5F) ? 1.0F : Mth.clamp(1.0F - (t - 9.5F) / 4.0F, 0.0F, 1.0F);
                poseStack.translate(0.0F, 0.02F, -0.14F);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(bladeFlat, 22.0F, -16.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(bladeFlat, 28.0F, 80.0F) * sign));
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(slash, 34.0F, -26.0F) * sign));
            }
            return true;
        }
        if (ultiT > 0) {
            float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            float t = Mth.clamp(40.0F - Math.max(0.0F, (float) ultiT - pt), 0.0F, 40.0F);
            if (t < 15.0F) {
                // Phase 1-2 (Launch & Mid-air Eject): Claymore hidden until drawn mid-dive
                poseStack.scale(0.0F, 0.0F, 0.0F);
                return true;
            }
            float vis = (t < 17.0F) ? Mth.clamp((t - 15.0F) / 2.0F, 0.0F, 1.0F)
                    : (t > 36.0F ? Mth.clamp((40.0F - t) / 4.0F, 0.0F, 1.0F) : 1.0F);
            poseStack.scale(vis, vis, vis);
            if (t < 22.0F) {
                // Phase 3 (Meteor Dive): Claymore raised high behind shoulder
                poseStack.translate(0.0F, 0.08F, -0.06F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sign * 16.0F));
            } else if (t < 26.0F) {
                // Phase 4 (Sunfell Slice): Flat horizontal 360-deg blade sweep
                float u = (t - 22.0F) / 4.0F;
                poseStack.translate(0.0F, 0.02F, -0.16F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sign * 82.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(sign * Mth.lerp(u, -36.0F, 36.0F)));
            } else {
                // Phase 5-6 (Iconic Sideways Wheelie Hold -> Touchdown): Blade points diagonally straight down toward ground behind rear wheel!
                float e = t < 28.0F ? WindGliderRenderer.smoothStep01((t - 26.0F) / 2.0F) : 1.0F;
                poseStack.translate(0.0F, Mth.lerp(e, 0.02F, -0.10F), Mth.lerp(e, -0.16F, -0.04F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(e, -18.0F, 34.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sign * Mth.lerp(e, 82.0F, 22.0F)));
                poseStack.mulPose(Axis.YP.rotationDegrees(sign * Mth.lerp(e, 36.0F, 14.0F)));
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
                float total = (float) com.velorise.staves.fsm.BikeFrameData.get(com.velorise.staves.fsm.BikeActionState.fromNaStep(step)).durationTicks();
                float nt = Mth.clamp(1.0F - (bike.getSmoothNaTicks(partialTicks) / Math.max(1.0F, total)), 0.0F, 1.0F);
                float w = Mth.sin(nt * (float) Math.PI);
                if (step == 6) {
                    float p = bike.getFinisherProgress(partialTicks);
                    if (p < 0.37F) {
                        // Continuous lift from Donut Spin posture to overhead windup apex (p = 0.28)
                        float u = p / 0.37F;
                        float lift = 1.0F - (1.0F - u) * (1.0F - u);
                        poseStack.translate(
                                dir * Mth.lerp(lift, 0.14F, -0.32F),
                                Mth.lerp(lift, -0.12F, 0.36F),
                                Mth.lerp(lift, -0.18F, -0.24F)
                        );
                        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(lift, -24.0F, -28.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(lift, 54.0F, 68.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(lift, 12.0F, 16.0F)));
                    } else if (p < 0.52F) {
                        // Accelerated diagonal-overhead cleave slam hitting right at hitmark (p = 0.48, tick 12)
                        float u = (p - 0.37F) / 0.15F;
                        float s = u * u;
                        poseStack.translate(
                                dir * Mth.lerp(s, -0.32F, 0.40F),
                                Mth.lerp(s, 0.36F, -0.20F),
                                -0.24F - 0.12F * Mth.sin(s * (float) Math.PI)
                        );
                        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, -28.0F, -54.0F)));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(s, 68.0F, -74.0F)));
                        poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(s, 16.0F, -22.0F)));
                    } else {
                        // Smooth follow-through fade (0.48 -> 1.00)
                        float r = 1.0F - (0.5F - 0.5F * Mth.cos(((p - 0.52F) / 0.48F) * (float) Math.PI));
                        poseStack.translate(dir * 0.40F * r, -0.20F * r, -0.24F * r);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-54.0F * r));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-dir * 74.0F * r));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 22.0F * r));
                    }
                } else if (step == 3 || step == 4 || step == 5) {
                    // Genshin NA3, NA4, NA5: Sword hidden (both hands on handlebars)
                    poseStack.scale(0.0F, 0.0F, 0.0F);
                } else if (step == 1) {
                    float t = Mth.clamp(22.0F - bike.getSmoothNaTicks(partialTicks), 0.0F, 22.0F);
                    float vis = (t < 1.5F) ? Mth.lerp(t / 1.5F, 0.25F, 1.0F)
                            : (t > 18.5F ? Mth.clamp((22.0F - t) / 3.5F, 0.0F, 1.0F) : 1.0F);
                    poseStack.scale(vis, vis, vis);
                    float sweep = (t < 3.0F) ? 0.0F : (t < 7.0F ? (t - 3.0F) / 4.0F : 1.0F);
                    poseStack.translate(dir * Mth.lerp(sweep, 0.38F, -0.36F), Mth.lerp(sweep, 0.10F, 0.02F), -0.22F * Mth.sin(sweep * (float) Math.PI));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-32.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(sweep, -68.0F, 64.0F)));
                } else if (step == 2) {
                    float t = Mth.clamp(24.0F - bike.getSmoothNaTicks(partialTicks), 0.0F, 24.0F);
                    float vis = (t > 20.5F) ? Mth.clamp((24.0F - t) / 3.5F, 0.0F, 1.0F) : 1.0F;
                    poseStack.scale(vis, vis, vis);
                    float sweep = (t < 4.0F) ? 0.0F : (t < 9.0F ? (t - 4.0F) / 5.0F : 1.0F);
                    poseStack.translate(dir * Mth.lerp(sweep, -0.36F, 0.38F), Mth.lerp(sweep, 0.08F, -0.14F), -0.24F * Mth.sin(sweep * (float) Math.PI));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(sweep, 68.0F, -62.0F)));
                } else {
                    poseStack.translate(dir * Mth.lerp(nt, 0.34F, -0.34F), 0.0F, -0.22F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-26.0F * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(nt, -48.0F, 52.0F)));
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
            double arcScale = isUlti ? 1.48D : (isFinisher ? 1.08D : 0.48D);
            double fadeScale = (1.18D - prog * 0.32D) * arcScale;
            double bowOffset = isUlti ? 0.44D : (isFinisher ? 0.42D : 0.28D);
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
