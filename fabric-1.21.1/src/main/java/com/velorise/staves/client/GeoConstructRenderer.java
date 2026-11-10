package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.entity.GeoConstructEntity;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders the 1x2x1 Geo Construct pillar using the vanilla Evoker Fangs 3D model tinted golden Geo. */
public class GeoConstructRenderer extends EntityRenderer<GeoConstructEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/illager/evoker_fangs.png");

    private final EvokerFangsModel<GeoConstructEntity> model;

    public GeoConstructRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EvokerFangsModel<>(context.bakeLayer(ModelLayers.EVOKER_FANGS));
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(
            GeoConstructEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        int pulseElapsed = entity.tickCount - entity.getLastPulseTick();
        float pulseWave = (pulseElapsed >= -1 && pulseElapsed < 9)
                ? Mth.sin((Mth.clamp(pulseElapsed + partialTicks, 0.0F, 9.0F) / 9.0F) * (float) Math.PI)
                : 0.0F;
        int hurtElapsed = entity.tickCount - entity.getLastHurtTick();
        boolean hurtFlash = hurtElapsed >= -1 && hurtElapsed < 4;

        float biteProgress = 0.48F - pulseWave * 0.14F;
        float rise = Mth.clamp((entity.tickCount + partialTicks) / 6.0F, 0.0F, 1.0F);
        float sink = entity.tickCount > (GeoConstructEntity.MAX_LIFETIME_TICKS - 12)
                ? Mth.clamp((entity.tickCount + partialTicks - (GeoConstructEntity.MAX_LIFETIME_TICKS - 12)) / 12.0F, 0.0F, 1.0F)
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

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GeoConstructEntity entity) {
        return TEXTURE;
    }
}
