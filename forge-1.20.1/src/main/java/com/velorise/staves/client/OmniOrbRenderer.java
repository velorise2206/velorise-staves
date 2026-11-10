package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.entity.OmniOrbEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Renders the spinning 3D crystalline octahedron prism orb projectile launched by Omni Staff.
 */
@Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class OmniOrbRenderer extends EntityRenderer<OmniOrbEntity> {
    private static final ResourceLocation FALLBACK_TEXTURE =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/element/omni.png");
    private static final RenderType ORB_RENDER_TYPE = OrbRenderType.create();

    public OmniOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VeloriseStavesMod.OMNI_ORB.get(), OmniOrbRenderer::new);
    }

    @Override
    protected int getBlockLightLevel(OmniOrbEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(OmniOrbEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(
            OmniOrbEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        float time = entity.tickCount + partialTicks;
        int mode = entity.getOmniMode();
        Element fixed = OmniOrbEntity.modeToElement(mode);
        boolean isNeth = entity.isNetheriteTier();

        int primaryRgb = fixed != null
                ? OmniOrbEntity.getElementRgb(fixed)
                : OmniOrbEntity.getIridescentRgb(time + entity.getId() * 3.0F);
        float pr = ((primaryRgb >> 16) & 0xFF) / 255.0F;
        float pg = ((primaryRgb >> 8) & 0xFF) / 255.0F;
        float pb = (primaryRgb & 0xFF) / 255.0F;

        int secondaryRgb = fixed != null
                ? primaryRgb
                : OmniOrbEntity.getIridescentRgb(time + entity.getId() * 3.0F + 7.5F);
        float sr = ((secondaryRgb >> 16) & 0xFF) / 255.0F;
        float sg = ((secondaryRgb >> 8) & 0xFF) / 255.0F;
        float sb = (secondaryRgb & 0xFF) / 255.0F;

        VertexConsumer consumer = bufferSource.getBuffer(ORB_RENDER_TYPE);
        float scale = isNeth ? 1.24F : 1.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.12D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 14.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 9.0F));
        drawOctahedron(consumer, poseStack.last().pose(), 0.14F * scale,
                1.0F, 0.98F, 0.92F, 0.96F,
                Math.min(1.0F, pr * 0.45F + 0.55F), Math.min(1.0F, pg * 0.45F + 0.55F), Math.min(1.0F, pb * 0.45F + 0.55F));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.12D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 8.5F));
        poseStack.mulPose(Axis.XP.rotationDegrees(22.0F + Mth.sin(time * 0.25F) * 14.0F));
        drawOctahedron(consumer, poseStack.last().pose(), 0.28F * scale,
                pr, pg, pb, 0.62F,
                sr, sg, sb);

        poseStack.mulPose(Axis.XP.rotationDegrees(55.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 11.0F));
        drawOrbitRing(consumer, poseStack.last().pose(), 0.34F * scale, 0.38F * scale,
                1.0F, 0.92F, 0.68F, 0.85F, 24);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void drawOctahedron(
            VertexConsumer consumer, Matrix4f pose, float r,
            float tr, float tg, float tb, float alpha,
            float br, float bg, float bb
    ) {
        float yStretch = r * 1.25F;
        float[][] dirs = {
                {r, 0, 0}, {0, 0, r}, {-r, 0, 0}, {0, 0, -r}
        };
        for (int i = 0; i < 4; i++) {
            float[] a = dirs[i];
            float[] b = dirs[(i + 1) & 3];
            float shade = (i % 2 == 0) ? 1.0F : 0.82F;
            drawTriQuad(consumer, pose,
                    0, yStretch, 0,
                    a[0], a[1], a[2],
                    b[0], b[1], b[2],
                    tr * shade, tg * shade, tb * shade, alpha);
            drawTriQuad(consumer, pose,
                    0, -yStretch, 0,
                    b[0], b[1], b[2],
                    a[0], a[1], a[2],
                    br * shade, bg * shade, bb * shade, alpha);
        }
    }

    private static void drawTriQuad(
            VertexConsumer consumer, Matrix4f pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a
    ) {
        consumer.vertex(pose, x0, y0, z0).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1, y1, z1).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x2, y2, z2).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x2, y2, z2).color(r, g, b, a).endVertex();
    }

    private static void drawOrbitRing(
            VertexConsumer consumer, Matrix4f pose, float innerR, float outerR,
            float r, float g, float b, float a, int segments
    ) {
        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0D * i) / segments;
            double a1 = (Math.PI * 2.0D * (i + 1)) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            consumer.vertex(pose, c0 * innerR, 0.0F, s0 * innerR).color(r, g, b, a).endVertex();
            consumer.vertex(pose, c0 * outerR, 0.0F, s0 * outerR).color(r, g, b, a).endVertex();
            consumer.vertex(pose, c1 * outerR, 0.0F, s1 * outerR).color(r, g, b, a).endVertex();
            consumer.vertex(pose, c1 * innerR, 0.0F, s1 * innerR).color(r, g, b, a).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(OmniOrbEntity entity) {
        return FALLBACK_TEXTURE;
    }

    private static class OrbRenderType extends RenderType {
        static RenderType create() {
            CompositeState state = CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false);
            return create("velorise_omni_orb", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true, state);
        }

        private OrbRenderType(
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
