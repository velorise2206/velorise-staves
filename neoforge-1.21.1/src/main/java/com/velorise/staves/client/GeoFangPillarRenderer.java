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
 * Renders the Geo Construct pillar (1x1x1 for Basic, 1x2x1 for Netherite) using the vanilla
 * Evoker Fangs 3D model tinted golden Geo, along with smooth expanding concentric square Geo shockwaves.
 */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GeoFangPillarRenderer extends EntityRenderer<GeoFangPillarEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/illager/evoker_fangs.png");
    private static final RenderType RING_RENDER_TYPE = GeoRingRenderType.create();
    private static final float[][] SQUARE_CORNERS = {
            {-1.0F, -1.0F},
            { 1.0F, -1.0F},
            { 1.0F,  1.0F},
            {-1.0F,  1.0F}
    };

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
        boolean netherite = entity.isNetherite() || entity.isStoneStele();
        this.shadowRadius = netherite ? 0.5F : 0.38F;

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
        float scaleXZ = (netherite ? 1.52F : 1.05F) + pulseWave * (netherite ? 0.12F : 0.08F);
        float scaleY = (netherite ? 1.23F : 0.62F) + pulseWave * (netherite ? 0.06F : 0.04F);
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
        float maxExtent = (float) entity.getWaveEndRadius();
        float innerEnd = Math.max(1.8F, maxExtent * 0.68F);

        for (float waveAge : entity.getClientWaveAges()) {
            float t = Mth.clamp((waveAge + partialTicks) / (float) GeoFangPillarEntity.WAVE_DURATION_TICKS, 0.0F, 1.0F);
            float alpha = t < 0.10F
                    ? (t / 0.10F)
                    : (t > 0.72F ? Math.max(0.0F, (1.0F - t) / 0.28F) : 1.0F);
            if (alpha <= 0.01F) continue;

            float outerR = Mth.lerp(t, (float) GeoFangPillarEntity.WAVE_START_RADIUS, maxExtent);
            drawConcentricSquareBand(ringConsumer, pose, outerR, 0.16F, 0.09F, 0.34F, 1.00F, 0.83F, 0.22F, alpha * 0.88F);

            float innerR = Mth.lerp(t, 0.40F, innerEnd);
            drawConcentricSquareBand(ringConsumer, pose, innerR, 0.09F, 0.11F, 0.22F, 1.00F, 0.92F, 0.48F, alpha * 0.65F);
        }
    }

    private static void drawConcentricSquareBand(
            VertexConsumer consumer,
            Matrix4f pose,
            float halfExtent,
            float halfWidth,
            float yBase,
            float yWallTop,
            float r,
            float g,
            float b,
            float a
    ) {
        float rIn = Math.max(0.05F, halfExtent - halfWidth);
        float rOut = halfExtent + halfWidth;
        float wallAlpha = a * 0.72F;

        for (int i = 0; i < SQUARE_CORNERS.length; i++) {
            float[] c0 = SQUARE_CORNERS[i];
            float[] c1 = SQUARE_CORNERS[(i + 1) % SQUARE_CORNERS.length];
            float x0 = c0[0], z0 = c0[1];
            float x1 = c1[0], z1 = c1[1];

            // Inner feather of ground band
            consumer.addVertex(pose, x0 * rIn, yBase, z0 * rIn).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, x1 * rIn, yBase, z1 * rIn).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, x1 * halfExtent, yBase, z1 * halfExtent).setColor(r, g, b, a);
            consumer.addVertex(pose, x0 * halfExtent, yBase, z0 * halfExtent).setColor(r, g, b, a);

            // Outer feather of ground band
            consumer.addVertex(pose, x0 * halfExtent, yBase, z0 * halfExtent).setColor(r, g, b, a);
            consumer.addVertex(pose, x1 * halfExtent, yBase, z1 * halfExtent).setColor(r, g, b, a);
            consumer.addVertex(pose, x1 * rOut, yBase, z1 * rOut).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, x0 * rOut, yBase, z0 * rOut).setColor(r, g, b, 0.0F);

            // Vertical luminous wave crest (visible from ground eye-level and over foliage)
            consumer.addVertex(pose, x0 * halfExtent, yBase - 0.04F, z0 * halfExtent).setColor(r, g, b, wallAlpha);
            consumer.addVertex(pose, x1 * halfExtent, yBase - 0.04F, z1 * halfExtent).setColor(r, g, b, wallAlpha);
            consumer.addVertex(pose, x1 * halfExtent, yWallTop, z1 * halfExtent).setColor(r, g, b, 0.0F);
            consumer.addVertex(pose, x0 * halfExtent, yWallTop, z0 * halfExtent).setColor(r, g, b, 0.0F);
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
