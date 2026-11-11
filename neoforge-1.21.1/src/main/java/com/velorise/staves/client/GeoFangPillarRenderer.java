package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.GeoFangPillarEntity;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/**
 * Renders the 1x2x1 Geo Construct pillar using the vanilla Evoker Fangs 3D model tinted golden Geo,
 * along with smooth expanding concentric Geo shockwave rings.
 */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GeoFangPillarRenderer extends EntityRenderer<GeoFangPillarEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/illager/evoker_fangs.png");
    private static final RenderType RING_RENDER_TYPE = GeoRingRenderType.create();
    private static final int RING_SEGMENTS = 64;

    private final EvokerFangsModel<GeoFangPillarEntity> model;

    public GeoFangPillarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EvokerFangsModel<>(context.bakeLayer(ModelLayers.EVOKER_FANGS));
        this.shadowRadius = 0.5F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VeloriseStavesMod.GEO_FANG_PILLAR.get(), GeoFangPillarRenderer::new);
    }

    @Override
    public void render(
            GeoFangPillarEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        int pulseElapsed = entity.tickCount - entity.getLastClientWaveStartTick();
        float pulseWave = (pulseElapsed >= -1 && pulseElapsed < 9)
                ? Mth.sin((Mth.clamp(pulseElapsed + partialTicks, 0.0F, 9.0F) / 9.0F) * (float) Math.PI)
                : 0.0F;
        int hurtElapsed = entity.tickCount - entity.getLastHurtTick();
        boolean hurtFlash = hurtElapsed >= -1 && hurtElapsed < 4;

        float biteProgress = 0.48F - pulseWave * 0.14F;
        float rise = Mth.clamp((entity.tickCount + partialTicks) / 6.0F, 0.0F, 1.0F);
        float sink = entity.tickCount > (GeoFangPillarEntity.MAX_LIFETIME_TICKS - 12)
                ? Mth.clamp((entity.tickCount + partialTicks - (GeoFangPillarEntity.MAX_LIFETIME_TICKS - 12)) / 12.0F, 0.0F, 1.0F)
                : 0.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entity.getYRot()));

        this.model.setupAnim(entity, biteProgress, 0.0F, 0.0F, entity.getYRot(), entity.getXRot());
        float f2 = (biteProgress + Mth.sin(biteProgress * 2.7F)) * 0.6F * 12.0F;
        float scaleXZ = 1.52F + pulseWave * 0.12F;
        float scaleY = 1.23F + pulseWave * 0.06F;
        poseStack.scale(-scaleXZ, -scaleY, scaleXZ);
        poseStack.translate(0.0F, -(36.0F - f2) / 16.0F + ((1.0F - rise) + sink) * 1.35F, 0.0F);

        int tint = hurtFlash ? 0xFFFF9080 : (pulseWave > 0.15F ? 0xFFFFF0A6 : 0xFFFFDA75);
        VertexConsumer consumer = bufferSource.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, tint);
        poseStack.popPose();

        if (!entity.getClientWaveAges().isEmpty()) {
            renderExpandingRings(entity, partialTicks, poseStack, bufferSource);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void renderExpandingRings(
            GeoFangPillarEntity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        VertexConsumer ringConsumer = bufferSource.getBuffer(RING_RENDER_TYPE);
        Matrix4f pose = poseStack.last().pose();

        for (float waveAge : entity.getClientWaveAges()) {
            float t = Mth.clamp((waveAge + partialTicks) / (float) GeoFangPillarEntity.WAVE_DURATION_TICKS, 0.0F, 1.0F);
            float alpha = t < 0.10F
                    ? (t / 0.10F)
                    : (t > 0.72F ? Math.max(0.0F, (1.0F - t) / 0.28F) : 1.0F);
            if (alpha <= 0.01F) continue;

            float outerR = (float) Mth.lerp(t, GeoFangPillarEntity.WAVE_START_RADIUS, GeoFangPillarEntity.WAVE_END_RADIUS);
            drawConcentricBand(ringConsumer, pose, outerR, 0.16F, 0.09F, 0.34F, 1.00F, 0.83F, 0.22F, alpha * 0.88F);

            float innerR = (float) Mth.lerp(t, 0.40D, 3.05D);
            drawConcentricBand(ringConsumer, pose, innerR, 0.09F, 0.11F, 0.22F, 1.00F, 0.92F, 0.48F, alpha * 0.65F);
        }
    }

    private static void drawConcentricBand(
            VertexConsumer consumer,
            Matrix4f pose,
            float radius,
            float halfWidth,
            float yBase,
            float yWallTop,
            float r,
            float g,
            float b,
            float a
    ) {
        float rIn = Math.max(0.05F, radius - halfWidth);
        float rOut = radius + halfWidth;
        float wallAlpha = a * 0.72F;

        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a0 = (Math.PI * 2.0D * i) / RING_SEGMENTS;
            double a1 = (Math.PI * 2.0D * (i + 1)) / RING_SEGMENTS;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);

            // Inner feather of ground band
            consumer.addVertex(pose, c0 * rIn, yBase, s0 * rIn).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, c1 * rIn, yBase, s1 * rIn).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, c1 * radius, yBase, s1 * radius).setColor(r, g, b, a);
            consumer.addVertex(pose, c0 * radius, yBase, s0 * radius).setColor(r, g, b, a);

            // Outer feather of ground band
            consumer.addVertex(pose, c0 * radius, yBase, s0 * radius).setColor(r, g, b, a);
            consumer.addVertex(pose, c1 * radius, yBase, s1 * radius).setColor(r, g, b, a);
            consumer.addVertex(pose, c1 * rOut, yBase, s1 * rOut).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, c0 * rOut, yBase, s0 * rOut).setColor(r, g, b, 0.0F);

            // Vertical luminous wave crest (visible from ground eye-level and over foliage)
            consumer.addVertex(pose, c0 * radius, yBase - 0.04F, s0 * radius).setColor(r, g, b, wallAlpha);
            consumer.addVertex(pose, c1 * radius, yBase - 0.04F, s1 * radius).setColor(r, g, b, wallAlpha);
            consumer.addVertex(pose, c1 * radius, yWallTop, s1 * radius).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, c0 * radius, yWallTop, s0 * radius).setColor(r, g, b, 0.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GeoFangPillarEntity entity) {
        return TEXTURE;
    }

    protected static class GeoRingRenderType extends RenderType {
        public static RenderType create() {
            CompositeState state = CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false);
            return create("velorise_geo_ring", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true, state);
        }

        private GeoRingRenderType(
                String name,
                VertexFormat format,
                VertexFormat.Mode mode,
                int bufferSize,
                boolean affectsCrumbling,
                boolean sortOnUpload,
                Runnable setupState,
                Runnable clearState
        ) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }
    }
}
