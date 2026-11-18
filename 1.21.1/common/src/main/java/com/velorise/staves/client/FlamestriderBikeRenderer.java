package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.client.model.FireBikeModel;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renders Mavuika's Flamestrider motorcycle in crisp Minecraft voxel/pixel blocks:
 * spinning front/rear wheels, crimson-gold-obsidian chassis, dragonic front cowling,
 * twin exhausts, deployable side jet-wings, 360-degree spin CA, and Ulti slam pitch.
 */
public class FlamestriderBikeRenderer extends EntityRenderer<FlamestriderBikeEntity> {
    private static final ResourceLocation FIRE_BIKE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/fire_bike_texture.png");
    private static FireBikeModel<FlamestriderBikeEntity> bikeModel;
    private static final RenderType BIKE_RENDER_TYPE = BikeRenderType.create();

    public FlamestriderBikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.75F;
    }

    @Override
    public void render(
            FlamestriderBikeEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();

        float yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        float climbPitch = Mth.lerp(partialTicks, entity.prevWallClimbPitch, entity.wallClimbPitch);
        if (Math.abs(climbPitch) > 0.05F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(climbPitch));
        }
        float spinLean = Mth.lerp(partialTicks, entity.prevSpinLean, entity.spinLean);
        if (Math.abs(spinLean) > 0.05F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(spinLean));
        }
        int ultiT = entity.getUltiTicks();
        int naT = entity.getActiveNaTicks();
        applyActionTransforms(entity, ultiT, naT, partialTicks, poseStack);

        renderBikeMesh(
                poseStack,
                bufferSource,
                this.getFont(),
                entity.isNetherite(),
                entity.frontWheelSpinAngle,
                entity.rearWheelSpinAngle,
                (float) entity.getCurrentSpeedBlocksPerSec(),
                (float) entity.getDistanceTraveledBlocks(),
                entity.isGliding(),
                entity.isBikeSprinting() || entity.isSpinning(),
                packedLight
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    public static void renderBikeMesh(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            net.minecraft.client.gui.Font font,
            boolean neth,
            float wheelSpinAngle,
            float speedBps,
            float odoMeters,
            boolean gliding,
            boolean sprinting
    ) {
        renderBikeMesh(poseStack, bufferSource, font, neth, wheelSpinAngle, wheelSpinAngle, speedBps, odoMeters, gliding, sprinting);
    }

    private static FireBikeModel<FlamestriderBikeEntity> getOrBakeModel() {
        if (bikeModel == null) {
            bikeModel = new FireBikeModel<>(FireBikeModel.createBodyLayer().bakeRoot());
        }
        return bikeModel;
    }

    public static void renderBikeMesh(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            net.minecraft.client.gui.Font font,
            boolean neth,
            float frontWheelSpinAngle,
            float rearWheelSpinAngle,
            float speedBps,
            float odoMeters,
            boolean gliding,
            boolean sprinting
    ) {
        renderBikeMesh(poseStack, bufferSource, font, neth, frontWheelSpinAngle, rearWheelSpinAngle, speedBps, odoMeters, gliding, sprinting, 0xF000F0);
    }

    public static void renderBikeMesh(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            net.minecraft.client.gui.Font font,
            boolean neth,
            float frontWheelSpinAngle,
            float rearWheelSpinAngle,
            float speedBps,
            float odoMeters,
            boolean gliding,
            boolean sprinting,
            int packedLight
    ) {
        FireBikeModel<FlamestriderBikeEntity> model = getOrBakeModel();
        model.front_wheel.xRot = -frontWheelSpinAngle;
        model.rear_wheel.xRot = -rearWheelSpinAngle;
        model.steering.yRot = 0.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.50F, 0.0F);
        VertexConsumer modelVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(FIRE_BIKE_TEXTURE));
                    model.renderToBuffer(poseStack, modelVc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();

        // Deployable Natlan Jet-Wings when gliding
        if (gliding) {
            VertexConsumer vc = bufferSource.getBuffer(BIKE_RENDER_TYPE);
            float crR = neth ? 0.86F : 0.76F, crG = 0.13F, crB = 0.11F;
            float gdR = 1.00F, gdG = 0.78F, gdB = 0.18F;
            for (int side : new int[]{-1, 1}) {
                poseStack.pushPose();
                poseStack.translate(side * 0.23F, 0.64F, -0.04F);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 22.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 10.0F));
                float x0 = side < 0 ? -0.58F : 0.0F;
                float x1 = side < 0 ? 0.0F : 0.58F;
                drawBox(poseStack, vc, x0, -0.03F, -0.22F, x1, 0.04F, 0.12F, crR, crG, crB, 1.0F);
                float tx0 = side < 0 ? -0.68F : 0.46F;
                float tx1 = side < 0 ? -0.46F : 0.68F;
                drawBox(poseStack, vc, tx0, -0.02F, -0.26F, tx1, 0.03F, 0.06F, gdR, gdG, gdB, 0.95F);
                poseStack.popPose();
            }
        }

        // Digital cockpit text & License plate text
        if (font != null) {
            String driveState = gliding ? "[AIR]" : (sprinting ? "[S]" : (speedBps > 0.5F ? "[D]" : "[N]"));
            String spdText = String.format(java.util.Locale.ROOT, "%.1f B/s %s", speedBps, driveState);
            String odoText = String.format(java.util.Locale.ROOT, "ODO: %d m", Math.round(odoMeters));

            poseStack.pushPose();
            poseStack.translate(0.0F, 0.34F, 0.86F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
            poseStack.translate(0.0F, 0.70F, -0.12F);
            poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
            poseStack.translate(0.0F, 0.058F, -0.029F);
            poseStack.scale(-0.0032F, -0.0032F, 0.0032F);
            Matrix4f dashMat = poseStack.last().pose();
            font.drawInBatch(spdText, -font.width(spdText) / 2.0F, -2.0F, 0xFFFFCC44, false, dashMat, bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(odoText, -font.width(odoText) / 2.0F, 7.0F, 0xFF66EEFF, false, dashMat, bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();

            String plateText = "NATLAN • 500";
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.42F, -0.92F);
            poseStack.mulPose(Axis.XP.rotationDegrees(14.0F));
            poseStack.translate(0.0F, 0.006F, -0.025F);
            poseStack.scale(-0.0034F, -0.0034F, 0.0034F);
            Matrix4f plateMat = poseStack.last().pose();
            font.drawInBatch(plateText, -font.width(plateText) / 2.0F, -3.0F, 0xFFFFB833, false, plateMat, bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
            poseStack.popPose();
        }
    }

    private static void drawBox(
            PoseStack poseStack, VertexConsumer vc,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a
    ) {
        Matrix4f mat = poseStack.last().pose();
        // Top (+Y)
        emit(vc, mat, x0, y1, z1, r, g, b, a);
        emit(vc, mat, x1, y1, z1, r, g, b, a);
        emit(vc, mat, x1, y1, z0, r, g, b, a);
        emit(vc, mat, x0, y1, z0, r, g, b, a);
        // Bottom (-Y)
        emit(vc, mat, x0, y0, z0, r * 0.65F, g * 0.65F, b * 0.65F, a);
        emit(vc, mat, x1, y0, z0, r * 0.65F, g * 0.65F, b * 0.65F, a);
        emit(vc, mat, x1, y0, z1, r * 0.65F, g * 0.65F, b * 0.65F, a);
        emit(vc, mat, x0, y0, z1, r * 0.65F, g * 0.65F, b * 0.65F, a);
        // Front (+Z)
        emit(vc, mat, x0, y0, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        emit(vc, mat, x1, y0, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        emit(vc, mat, x1, y1, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        emit(vc, mat, x0, y1, z1, r * 0.90F, g * 0.90F, b * 0.90F, a);
        // Back (-Z)
        emit(vc, mat, x1, y0, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        emit(vc, mat, x0, y0, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        emit(vc, mat, x0, y1, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        emit(vc, mat, x1, y1, z0, r * 0.80F, g * 0.80F, b * 0.80F, a);
        // Left (-X)
        emit(vc, mat, x0, y0, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x0, y0, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x0, y1, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x0, y1, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        // Right (+X)
        emit(vc, mat, x1, y0, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x1, y0, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x1, y1, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emit(vc, mat, x1, y1, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
    }

    private static void emit(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(mat, x, y, z).setColor(r, g, b, a);
    }

    private static void applyActionTransforms(FlamestriderBikeEntity entity, int ultiT, int naT, float partialTicks, PoseStack poseStack) {
        if (entity.isGliding() && !entity.isPlunging() && ultiT <= 0 && naT <= 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        }
        if (entity.isSpinning() && ultiT <= 0 && !entity.isPlunging()) {
            // Genshin Donut Spin: front tire planted, rear wheel slightly lifted & drifting outward
            poseStack.translate(0.0D, 0.06D, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(7.5F));
        }
        if (entity.isPlunging()) {
            int windup = entity.getPlungeWindup();
            if (windup > 0) {
                float w = Mth.clamp((5.0F - ((float) windup - partialTicks)) / 5.0F, 0.0F, 1.0F);
                poseStack.translate(0.0D, Math.sin(w * Math.PI) * 0.18D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(w, -28.0F, 62.0F)));
            } else {
                poseStack.mulPose(Axis.XP.rotationDegrees(62.0F));
            }
        }
        if (ultiT > 0) {
            float u = Math.max(0.0F, (float) ultiT - partialTicks);
            if (u > 8.0F) {
                if (u >= 19.0F) {
                    // Half 1 (u: 30.0 -> 19.0): steady climb + 180-deg half-donut spin behind/beside rider
                    float p1 = Mth.clamp((30.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s1 = p1 * p1 * (3.0F - 2.0F * p1);
                    float theta = s1 * (float) Math.PI;
                    float sideX = Mth.sin(theta) * 1.15F;
                    float backZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                    float arcY = p1 * 0.85F;
                    poseStack.translate(sideX, arcY, backZ);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-s1 * 180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(theta) * 32.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.sin(theta) * 20.0F));
                } else {
                    // Half 2 (u: 19.0 -> 8.0): complete loop 180 -> 360 deg forward, descend, and cleave at touchdown (u == 8)
                    float p2 = Mth.clamp((19.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s2 = p2 * p2 * (3.0F - 2.0F * p2);
                    float theta = (float) Math.PI * (1.0F + s2);
                    float sideX = Mth.sin(theta) * 1.15F;
                    float backZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                    float arcY = (1.0F - p2 * p2) * 0.85F;
                    poseStack.translate(sideX, arcY, backZ);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F - s2 * 180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(s2 * (float) Math.PI) * 28.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(s2 * (float) Math.PI) * 22.0F));
                }
            } else {
                float rec = Mth.clamp(u / 8.0F, 0.0F, 1.0F);
                poseStack.translate(0.0F, 0.0F, rec * 0.25F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(rec * 14.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(rec * 8.0F));
            }
        }

        if (naT > 0 && !entity.isSpinning()) {
            int step = entity.getActiveNaStep();
            float naTf = entity.getSmoothNaTicks(partialTicks);
            if (step == 1) {
                float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
                float s = p * p * (3.0F - 2.0F * p);
                float swing = Mth.sin(p * (float) Math.PI);
                float pitch = (s < 0.45F)
                        ? Mth.lerp(s / 0.45F, 0.0F, -16.0F)
                        : Mth.lerp((s - 0.45F) / 0.55F, -16.0F, 14.0F) * (1.0F - p * 0.65F);
                poseStack.translate(0.0D, swing * 0.16D, swing * 0.10D);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, 10.0F, -14.0F) * swing));
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F * swing));
            } else if (step == 2) {
                float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
                float s = p * p * (3.0F - 2.0F * p);
                float swing = Mth.sin(p * (float) Math.PI);
                poseStack.translate(0.0D, swing * 0.10D, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, -34.0F, 36.0F) * swing));
                poseStack.mulPose(Axis.ZP.rotationDegrees(20.0F * swing));
                poseStack.mulPose(Axis.XP.rotationDegrees(8.0F * swing));
            } else if (step == 3) {
                float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
                float s = p * p * (3.0F - 2.0F * p);
                float swing = Mth.sin(p * (float) Math.PI);
                poseStack.translate(0.0D, swing * 0.08D, swing * 0.12D);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, 38.0F, -38.0F) * swing));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-22.0F * swing));
            } else if (step == 4) {
                float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
                float s = p * p * (3.0F - 2.0F * p);
                float swing = Mth.sin(p * (float) Math.PI);
                float pitch = (s < 0.52F)
                        ? Mth.lerp(s / 0.52F, 0.0F, -34.0F)
                        : Mth.lerp((s - 0.52F) / 0.48F, -34.0F, 18.0F) * (1.0F - (p - 0.52F));
                poseStack.translate(0.0D, swing * 0.36D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F * swing));
            } else if (step == 5) {
                float p = Mth.clamp(1.0F - (naTf / 14.0F), 0.0F, 1.0F);
                float s = p * p * (3.0F - 2.0F * p);
                poseStack.translate(0.0D, Math.sin(p * Math.PI) * 0.88D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(s * 360.0F));
            } else if (step == 6) {
                // Genshin Charged Attack Final (Finisher): higher continuous C1 leap & overhead slam arc
                float p = entity.getFinisherProgress(partialTicks);
                float jumpArc = (p < 0.72F) ? Mth.sin((p / 0.72F) * (float) Math.PI) * 0.68F : 0.0F;
                float surgeZ = Mth.sin(p * (float) Math.PI) * 0.20F;
                poseStack.translate(0.0D, jumpArc, surgeZ);
                float yawWhip;
                float pitch;
                float roll;
                if (p < 0.36F) {
                    float e = 0.5F - 0.5F * Mth.cos((p / 0.36F) * (float) Math.PI);
                    yawWhip = Mth.lerp(e, 0.0F, -8.0F);
                    pitch = Mth.lerp(e, 0.0F, -22.0F);
                    roll = Mth.lerp(e, 0.0F, -8.0F);
                } else if (p < 0.72F) {
                    float e = 0.5F - 0.5F * Mth.cos(((p - 0.36F) / 0.36F) * (float) Math.PI);
                    yawWhip = Mth.lerp(e, -8.0F, 10.0F);
                    pitch = Mth.lerp(e, -22.0F, 14.0F);
                    roll = Mth.lerp(e, -8.0F, 10.0F);
                } else {
                    float e = 0.5F - 0.5F * Mth.cos(((p - 0.72F) / 0.28F) * (float) Math.PI);
                    yawWhip = Mth.lerp(e, 10.0F, 0.0F);
                    pitch = Mth.lerp(e, 14.0F, 0.0F);
                    roll = Mth.lerp(e, 10.0F, 0.0F);
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(yawWhip));
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
            } else if (step == 7) {
                // Genshin Hold E Mount: 180-deg half-donut rear-wheel tail whip around front tire pivot
                float p = Mth.clamp(1.0F - (naTf / 10.0F), 0.0F, 1.0F);
                float s = p * p * (3.0F - 2.0F * p);
                float swing = Mth.sin(p * (float) Math.PI);
                poseStack.translate(0.0F, swing * 0.10F, 0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees((1.0F - s) * 360.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(26.0F * swing));
                poseStack.mulPose(Axis.XP.rotationDegrees(12.0F * swing));
                poseStack.translate(0.0F, 0.0F, -0.72F);
            }
        }
    }

    /**
     * Orbits and leans the rider's rendered model alongside the Flamestrider during Donut Spin,
     * NA combo flips, Finisher, and Ulti while keeping the entity/player hitbox at center.
     */
    public static boolean pushRiderBikeTransform(net.minecraft.world.entity.LivingEntity rider, float partialTicks, PoseStack poseStack) {
        if (!(rider.getVehicle() instanceof FlamestriderBikeEntity bike)) {
            float flipProg = ClientAuraTracker.getXilonenJumpFlipProgress(rider.getId());
            if (flipProg >= 0.0F && !rider.onGround()) {
                poseStack.pushPose();
                float bodyYaw = Mth.rotLerp(partialTicks, rider.yBodyRotO, rider.yBodyRot);
                // Smooth single 360-degree front flip anchored to jump start time (~740ms), finishing at 360 deg right on landing
                float easedFlip = Mth.clamp(flipProg * (1.12F - 0.12F * flipProg), 0.0F, 1.0F);
                float flipDeg = easedFlip * 360.0F;
                poseStack.translate(0.0D, 0.90D, 0.0D);
                poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(flipDeg));
                poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
                poseStack.translate(0.0D, -0.90D, 0.0D);
                return true;
            }
            return false;
        }
        boolean spinning = bike.isSpinning();
        float spinLean = Mth.lerp(partialTicks, bike.prevSpinLean, bike.spinLean);
        float climbPitch = Mth.lerp(partialTicks, bike.prevWallClimbPitch, bike.wallClimbPitch);
        int ultiT = bike.getUltiTicks();
        int naT = bike.getActiveNaTicks();
        if (!spinning && Math.abs(spinLean) <= 0.05F && Math.abs(climbPitch) <= 0.05F && ultiT <= 0 && naT <= 0 && !bike.isGliding() && !bike.isPlunging()) {
            return false;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.55D, 0.0D);

        float yaw = Mth.rotLerp(partialTicks, bike.yRotO, bike.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        if (Math.abs(climbPitch) > 0.05F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(climbPitch));
        }
        if (Math.abs(spinLean) > 0.05F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(spinLean));
        }

        applyActionTransforms(bike, ultiT, naT, partialTicks, poseStack);

        float bodyYaw = Mth.rotLerp(partialTicks, rider.yBodyRotO, rider.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        poseStack.translate(0.0D, 0.55D, 0.0D);
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(FlamestriderBikeEntity entity) {
        return FIRE_BIKE_TEXTURE;
    }

    protected static class BikeRenderType extends RenderType {
        public static RenderType create() {
            CompositeState state = CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false);
            return create("velorise_flamestrider_bike", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 512, false, true, state);
        }

        private BikeRenderType(
                String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
        ) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }
    }
}
