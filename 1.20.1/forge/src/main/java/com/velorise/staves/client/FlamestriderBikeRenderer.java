package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.model.FireBikeModel;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Renders Mavuika's Flamestrider motorcycle in crisp Minecraft voxel/pixel blocks:
 * spinning front/rear wheels, crimson-gold-obsidian chassis, dragonic front cowling,
 * twin exhausts, deployable side jet-wings, 360-degree spin CA, and Ulti slam pitch.
 */
@Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FlamestriderBikeRenderer extends EntityRenderer<FlamestriderBikeEntity> {
    private static final ResourceLocation FIRE_BIKE_TEXTURE =
            new ResourceLocation("velorise_staves", "textures/entity/fire_bike_texture.png");
    private static FireBikeModel<FlamestriderBikeEntity> bikeModel;
    private static final RenderType BIKE_RENDER_TYPE = BikeRenderType.create();

    public FlamestriderBikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.75F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VeloriseStavesMod.FLAMESTRIDER_BIKE.get(), FlamestriderBikeRenderer::new);
        event.registerEntityRenderer(VeloriseStavesMod.CRYO_REGISVINE.get(), CryoRegisvineRenderer::new);
        event.registerEntityRenderer(VeloriseStavesMod.CRYO_ICICLE.get(), CryoIcicleRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.resources.ResourceLocation pullId = new net.minecraft.resources.ResourceLocation("pull");
            net.minecraft.resources.ResourceLocation pullingId = new net.minecraft.resources.ResourceLocation("pulling");
            for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                    VeloriseStavesMod.THE_DAYBREAK_CHRONICLES.get(),
                    VeloriseStavesMod.NETHERITE_THE_DAYBREAK_CHRONICLES.get()
            }) {
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.getDaybreakPullProgress(entity, stack));
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.isDaybreakPulling(entity, stack) ? 1.0F : 0.0F);
            }
            net.minecraft.resources.ResourceLocation spearStanceId = new net.minecraft.resources.ResourceLocation("velorise_staves", "spear_stance");
            for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                    VeloriseStavesMod.TSARITSA_BOW.get(),
                    VeloriseStavesMod.NETHERITE_TSARITSA_BOW.get()
            }) {
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.getTsaritsaPullProgress(entity, stack));
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                        (stack, level, entity, seed) -> ClientAuraRenderer.isTsaritsaPulling(entity, stack) ? 1.0F : 0.0F);
                net.minecraft.client.renderer.item.ItemProperties.register(item, spearStanceId,
                        (stack, level, entity, seed) -> (entity != null && ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) ? 1.0F : 0.0F);
            }
            for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                    VeloriseStavesMod.OMNI_STAFF.get(),
                    VeloriseStavesMod.NETHERITE_OMNI_STAFF.get()
            }) {
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                        (stack, level, entity, seed) -> com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(stack) == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK
                                ? ClientAuraRenderer.getDaybreakPullProgress(entity, stack)
                                : ClientAuraRenderer.getTsaritsaPullProgress(entity, stack));
                net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                        (stack, level, entity, seed) -> (ClientAuraRenderer.isDaybreakPulling(entity, stack) || ClientAuraRenderer.isTsaritsaPulling(entity, stack)) ? 1.0F : 0.0F);
                net.minecraft.client.renderer.item.ItemProperties.register(item, spearStanceId,
                        (stack, level, entity, seed) -> (entity != null && ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) ? 1.0F : 0.0F);
            }
        });
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

        float frontWheelSpin = Mth.lerp(partialTicks, entity.prevFrontWheelSpinAngle, entity.frontWheelSpinAngle);
        float rearWheelSpin = Mth.lerp(partialTicks, entity.prevRearWheelSpinAngle, entity.rearWheelSpinAngle);
        float steeringYawRad = Mth.lerp(partialTicks, entity.prevSteeringAngle, entity.steeringAngle);

        renderBikeMesh(
                poseStack,
                bufferSource,
                this.getFont(),
                entity.isNetherite(),
                frontWheelSpin,
                rearWheelSpin,
                steeringYawRad,
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
        renderBikeMesh(poseStack, bufferSource, font, neth, frontWheelSpinAngle, rearWheelSpinAngle, 0.0F, speedBps, odoMeters, gliding, sprinting, packedLight);
    }

    public static void renderBikeMesh(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            net.minecraft.client.gui.Font font,
            boolean neth,
            float frontWheelSpinAngle,
            float rearWheelSpinAngle,
            float steeringYawRad,
            float speedBps,
            float odoMeters,
            boolean gliding,
            boolean sprinting,
            int packedLight
    ) {
        FireBikeModel<FlamestriderBikeEntity> model = getOrBakeModel();
        model.applyWheelAndSteering(frontWheelSpinAngle, rearWheelSpinAngle, steeringYawRad);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.50F, 0.0F);
        VertexConsumer modelVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(FIRE_BIKE_TEXTURE));
        model.renderToBuffer(poseStack, modelVc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
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
        vc.vertex(mat, x, y, z).color(r, g, b, a).endVertex();
    }

    private static void applyActionTransforms(FlamestriderBikeEntity entity, int ultiT, int naT, float partialTicks, PoseStack poseStack) {
        com.velorise.staves.fsm.BikeActionState activeState = entity.getActionState();
        com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot pose =
                com.velorise.staves.fsm.BikePoseBlender.evaluateAndBlend(
                        entity.getId(),
                        activeState,
                        ultiT,
                        entity.getPlungeWindup(),
                        entity.getActiveNaStep(),
                        entity.getSmoothNaTicks(partialTicks),
                        entity.getFinisherProgress(partialTicks),
                        partialTicks
                );
        if (Math.abs(pose.pivotZ) > 0.0001F) {
            poseStack.translate(pose.offsetX, pose.offsetY, pose.offsetZ + pose.pivotZ);
            if (Math.abs(pose.yawDeg) > 0.001F) poseStack.mulPose(Axis.YP.rotationDegrees(pose.yawDeg));
            if (Math.abs(pose.rollDeg) > 0.001F) poseStack.mulPose(Axis.ZP.rotationDegrees(pose.rollDeg));
            if (Math.abs(pose.pitchDeg) > 0.001F) poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitchDeg));
            poseStack.translate(0.0F, 0.0F, -pose.pivotZ);
        } else {
            if (Math.abs(pose.offsetX) > 0.0001F || Math.abs(pose.offsetY) > 0.0001F || Math.abs(pose.offsetZ) > 0.0001F) {
                poseStack.translate(pose.offsetX, pose.offsetY, pose.offsetZ);
            }
            if (Math.abs(pose.yawDeg) > 0.001F) poseStack.mulPose(Axis.YP.rotationDegrees(pose.yawDeg));
            if (Math.abs(pose.pitchDeg) > 0.001F) poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitchDeg));
            if (Math.abs(pose.rollDeg) > 0.001F) poseStack.mulPose(Axis.ZP.rotationDegrees(pose.rollDeg));
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
