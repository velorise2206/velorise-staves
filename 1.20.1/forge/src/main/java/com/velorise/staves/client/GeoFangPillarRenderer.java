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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Renders the custom Zhongli-inspired Liyue Stone Stele Geo Construct (1x1x1 for Basic Fang Pillar,
 * 1x2x1 for Netherite / Vortex Vanquisher Stone Stele) with pixel-art bronze-basalt relief geometry,
 * luminous golden Geo diamond cores, glowing base runes, and expanding square Geo resonance shockwaves.
 */
@Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GeoFangPillarRenderer extends EntityRenderer<GeoFangPillarEntity> {
    private static final ResourceLocation STELE_TEXTURE =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/entity/stone_stele.png");
    private static final ResourceLocation FANGS_TEXTURE =
            new ResourceLocation("textures/entity/illager/evoker_fangs.png");
    private static final RenderType RING_RENDER_TYPE = GeoRingRenderType.create();
    private static final float[][] SQUARE_CORNERS = {
            {-1.0F, -1.0F},
            { 1.0F, -1.0F},
            { 1.0F,  1.0F},
            {-1.0F,  1.0F}
    };

    private final EvokerFangsModel<GeoFangPillarEntity> fangsModel;

    public GeoFangPillarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.fangsModel = new EvokerFangsModel<>(context.bakeLayer(ModelLayers.EVOKER_FANGS));
        this.shadowRadius = 0.46F;
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
        boolean tallStele = entity.isNetherite() || entity.isStoneStele();
        this.shadowRadius = tallStele ? 0.48F : 0.38F;

        int pulseElapsed = entity.tickCount - entity.getLastClientWaveStartTick();
        float pulseWave = (pulseElapsed >= -1 && pulseElapsed < 10)
                ? Mth.sin((Mth.clamp(pulseElapsed + partialTicks, 0.0F, 10.0F) / 10.0F) * (float) Math.PI)
                : 0.0F;
        int hurtElapsed = entity.tickCount - entity.getLastHurtTick();
        boolean hurtFlash = hurtElapsed >= -1 && hurtElapsed < 4;

        if (!entity.isStoneStele()) {
            boolean netherite = entity.isNetherite();
            this.shadowRadius = netherite ? 0.5F : 0.38F;
            float fangPulse = (pulseElapsed >= -1 && pulseElapsed < 9)
                    ? Mth.sin((Mth.clamp(pulseElapsed + partialTicks, 0.0F, 9.0F) / 9.0F) * (float) Math.PI)
                    : 0.0F;
            float biteProgress = 0.48F - fangPulse * 0.14F;
            float rise = Mth.clamp((entity.tickCount + partialTicks) / 6.0F, 0.0F, 1.0F);
            float sink = entity.tickCount > (GeoFangPillarEntity.MAX_LIFETIME_TICKS - 12)
                    ? Mth.clamp((entity.tickCount + partialTicks - (GeoFangPillarEntity.MAX_LIFETIME_TICKS - 12)) / 12.0F, 0.0F, 1.0F)
                    : 0.0F;

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entity.getYRot()));

            this.fangsModel.setupAnim(entity, biteProgress, 0.0F, 0.0F, entity.getYRot(), entity.getXRot());
            float f2 = (biteProgress + Mth.sin(biteProgress * 2.7F)) * 0.6F * 12.0F;
            float scaleXZ = (netherite ? 1.52F : 1.05F) + fangPulse * (netherite ? 0.12F : 0.08F);
            float scaleY = (netherite ? 1.23F : 0.62F) + fangPulse * (netherite ? 0.06F : 0.04F);
            poseStack.scale(-scaleXZ, -scaleY, scaleXZ);
            poseStack.translate(0.0F, -(36.0F - f2) / 16.0F + ((1.0F - rise) + sink) * 1.35F, 0.0F);

            float r = 1.0F;
            float g = hurtFlash ? 0.56F : (fangPulse > 0.15F ? 0.94F : 0.85F);
            float b = hurtFlash ? 0.50F : (fangPulse > 0.15F ? 0.65F : 0.46F);
            VertexConsumer consumer = bufferSource.getBuffer(this.fangsModel.renderType(FANGS_TEXTURE));
            this.fangsModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
            poseStack.popPose();

            if (!entity.getClientWaveAges().isEmpty()) {
                renderExpandingRings(entity, partialTicks, poseStack, bufferSource);
            }

            super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
            return;
        }

        float rise = Mth.clamp((entity.tickCount + partialTicks) / 5.5F, 0.0F, 1.0F);
        float sink = entity.tickCount > (GeoFangPillarEntity.MAX_LIFETIME_TICKS - 12)
                ? Mth.clamp((entity.tickCount + partialTicks - (GeoFangPillarEntity.MAX_LIFETIME_TICKS - 12)) / 12.0F, 0.0F, 1.0F)
                : 0.0F;

        float heightBlocks = entity.isStoneStele() ? 3.5F : (entity.isNetherite() ? 2.0F : 1.15F);
        float halfW = tallStele ? 0.34F : 0.30F;
        float yOffset = -((1.0F - rise) + sink) * heightBlocks;

        poseStack.pushPose();
        if (entity.isHorizontalStele()) {
            net.minecraft.core.Direction dir = entity.getAttachDirection();
            float horizYaw = switch (dir) {
                case NORTH -> 180.0F;
                case EAST -> 90.0F;
                case WEST -> -90.0F;
                default -> 0.0F;
            };
            poseStack.translate(0.0F, 0.45F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(horizYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.0F, yOffset, 0.0F);
        } else {
            poseStack.translate(0.0F, yOffset, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        }

        float scalePulse = 1.0F + pulseWave * 0.035F;
        poseStack.scale(scalePulse, 1.0F, scalePulse);

        // 1. Textured Pixel-Art Zhongli Stone Stele Monolith Body
        VertexConsumer texVc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(STELE_TEXTURE));
        int bodyLight = Math.max(packedLight, LightTexture.pack(11, 13));
        int r = 255;
        int g = hurtFlash ? 145 : (pulseWave > 0.1F ? 250 : 240);
        int b = hurtFlash ? 130 : (pulseWave > 0.1F ? 225 : 210);
        renderTexturedSteleColumn(poseStack.last().pose(), texVc, halfW, heightBlocks, r, g, b, bodyLight);

        // 2. Sculpted 3D Architectural Relief Pillars, Crown, Base Plinth & Glowing Geo Diamond Cores
        VertexConsumer reliefVc = bufferSource.getBuffer(RING_RENDER_TYPE);
        renderSteleArchitecturalRelief(poseStack.last().pose(), reliefVc, halfW, heightBlocks, pulseWave, hurtFlash);

        poseStack.popPose();

        if (!entity.getClientWaveAges().isEmpty()) {
            renderExpandingRings(entity, partialTicks, poseStack, bufferSource);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void renderTexturedSteleColumn(
            Matrix4f mat,
            VertexConsumer vc,
            float hw,
            float h,
            int r, int g, int b,
            int light
    ) {
        float u0 = 0.0F, u1 = 0.25F, u2 = 0.50F, u3 = 0.75F, u4 = 1.0F;
        float vTop = 0.0F, vBot = 0.50F;

        // North (-Z)
        emitTexVert(vc, mat, -hw, h, -hw, u0, vTop, r, g, b, 0.0F, 0.0F, -1.0F, light);
        emitTexVert(vc, mat,  hw, h, -hw, u1, vTop, r, g, b, 0.0F, 0.0F, -1.0F, light);
        emitTexVert(vc, mat,  hw, 0, -hw, u1, vBot, r, g, b, 0.0F, 0.0F, -1.0F, light);
        emitTexVert(vc, mat, -hw, 0, -hw, u0, vBot, r, g, b, 0.0F, 0.0F, -1.0F, light);

        // South (+Z)
        emitTexVert(vc, mat,  hw, h,  hw, u1, vTop, r, g, b, 0.0F, 0.0F, 1.0F, light);
        emitTexVert(vc, mat, -hw, h,  hw, u2, vTop, r, g, b, 0.0F, 0.0F, 1.0F, light);
        emitTexVert(vc, mat, -hw, 0,  hw, u2, vBot, r, g, b, 0.0F, 0.0F, 1.0F, light);
        emitTexVert(vc, mat,  hw, 0,  hw, u1, vBot, r, g, b, 0.0F, 0.0F, 1.0F, light);

        // East (+X)
        emitTexVert(vc, mat,  hw, h, -hw, u2, vTop, r, g, b, 1.0F, 0.0F, 0.0F, light);
        emitTexVert(vc, mat,  hw, h,  hw, u3, vTop, r, g, b, 1.0F, 0.0F, 0.0F, light);
        emitTexVert(vc, mat,  hw, 0,  hw, u3, vBot, r, g, b, 1.0F, 0.0F, 0.0F, light);
        emitTexVert(vc, mat,  hw, 0, -hw, u2, vBot, r, g, b, 1.0F, 0.0F, 0.0F, light);

        // West (-X)
        emitTexVert(vc, mat, -hw, h,  hw, u3, vTop, r, g, b, -1.0F, 0.0F, 0.0F, light);
        emitTexVert(vc, mat, -hw, h, -hw, u4, vTop, r, g, b, -1.0F, 0.0F, 0.0F, light);
        emitTexVert(vc, mat, -hw, 0, -hw, u4, vBot, r, g, b, -1.0F, 0.0F, 0.0F, light);
        emitTexVert(vc, mat, -hw, 0,  hw, u3, vBot, r, g, b, -1.0F, 0.0F, 0.0F, light);

        // Top Cap (+Y) -> U:[0..0.25], V:[0.50..0.75]
        emitTexVert(vc, mat, -hw, h, -hw, 0.0F,  0.50F, r, g, b, 0.0F, 1.0F, 0.0F, LightTexture.FULL_BRIGHT);
        emitTexVert(vc, mat, -hw, h,  hw, 0.0F,  0.75F, r, g, b, 0.0F, 1.0F, 0.0F, LightTexture.FULL_BRIGHT);
        emitTexVert(vc, mat,  hw, h,  hw, 0.25F, 0.75F, r, g, b, 0.0F, 1.0F, 0.0F, LightTexture.FULL_BRIGHT);
        emitTexVert(vc, mat,  hw, h, -hw, 0.25F, 0.50F, r, g, b, 0.0F, 1.0F, 0.0F, LightTexture.FULL_BRIGHT);

        // Bottom Cap (-Y) -> U:[0.25..0.50], V:[0.50..0.75]
        emitTexVert(vc, mat, -hw, 0,  hw, 0.25F, 0.50F, r, g, b, 0.0F, -1.0F, 0.0F, light);
        emitTexVert(vc, mat, -hw, 0, -hw, 0.25F, 0.75F, r, g, b, 0.0F, -1.0F, 0.0F, light);
        emitTexVert(vc, mat,  hw, 0, -hw, 0.50F, 0.75F, r, g, b, 0.0F, -1.0F, 0.0F, light);
        emitTexVert(vc, mat,  hw, 0,  hw, 0.50F, 0.50F, r, g, b, 0.0F, -1.0F, 0.0F, light);
    }

    private static void emitTexVert(
            VertexConsumer vc, Matrix4f mat,
            float x, float y, float z,
            float u, float v,
            int r, int g, int b,
            float nx, float ny, float nz,
            int light
    ) {
        vc.vertex(mat, x, y, z)
                .color(r, g, b, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(nx, ny, nz)
                .endVertex();
    }

    private static void renderSteleArchitecturalRelief(
            Matrix4f mat,
            VertexConsumer vc,
            float hw,
            float h,
            float pulseWave,
            boolean hurtFlash
    ) {
        // Dark Bronze-Basalt structural ribs & stepped base/crown
        float br = hurtFlash ? 0.45F : 0.16F;
        float bg = hurtFlash ? 0.18F : 0.11F;
        float bb = hurtFlash ? 0.14F : 0.08F;

        // Stepped Basal Plinth
        float baseW = hw + 0.045F;
        drawSolidBox(vc, mat, -baseW, 0.0F, -baseW, baseW, 0.16F, baseW, br, bg, bb, 0.96F);

        // Stepped Upper Crown Collar & Tapered Capstone
        float crownW = hw + 0.032F;
        drawSolidBox(vc, mat, -crownW, h - 0.16F, -crownW, crownW, h - 0.05F, crownW, br, bg, bb, 0.96F);
        float capW = hw - 0.05F;
        drawSolidBox(vc, mat, -capW, h, -capW, capW, h + 0.06F, capW, br * 1.15F, bg * 1.15F, bb * 1.15F, 0.96F);

        // 4 Corner Vertical Basalt Ribs (gives the unmistakable beveled octagonal-square Zhongli silhouette)
        float ribThick = 0.045F;
        float ribOut = hw + 0.015F;
        drawSolidBox(vc, mat, -ribOut, 0.16F, -ribOut, -ribOut + ribThick, h - 0.16F, -ribOut + ribThick, br, bg, bb, 0.96F);
        drawSolidBox(vc, mat,  ribOut - ribThick, 0.16F, -ribOut,  ribOut, h - 0.16F, -ribOut + ribThick, br, bg, bb, 0.96F);
        drawSolidBox(vc, mat, -ribOut, 0.16F,  ribOut - ribThick, -ribOut + ribThick, h - 0.16F,  ribOut, br, bg, bb, 0.96F);
        drawSolidBox(vc, mat,  ribOut - ribThick, 0.16F,  ribOut - ribThick,  ribOut, h - 0.16F,  ribOut, br, bg, bb, 0.96F);

        // Luminous Geo Gold Core & Diamond Emblems on all 4 faces
        float gr = 1.0F;
        float gg = hurtFlash ? 0.55F : (0.82F + pulseWave * 0.16F);
        float gb = hurtFlash ? 0.35F : (0.24F + pulseWave * 0.28F);
        float coreAlpha = 0.92F + pulseWave * 0.08F;

        // Upper-middle main glowing Geo Diamond on all 4 faces (matches Zhongli's iconic glowing core)
        float mainCoreY = h * 0.70F;
        float diamondR = (h > 1.5F ? 0.145F : 0.115F) + pulseWave * 0.025F;
        float faceOut = hw + 0.012F;
        drawAllFourFaceDiamonds(vc, mat, faceOut, mainCoreY, diamondR, gr, gg, gb, coreAlpha);
        drawAllFourFaceDiamonds(vc, mat, faceOut + 0.006F, mainCoreY, diamondR * 0.52F, 1.0F, 0.97F, 0.78F, 0.98F);

        // Secondary mid-lower Geo Diamond on tall Stone Steles
        if (h > 1.5F) {
            float lowerCoreY = h * 0.38F;
            float subR = 0.095F + pulseWave * 0.018F;
            drawAllFourFaceDiamonds(vc, mat, faceOut, lowerCoreY, subR, gr, gg, gb, coreAlpha * 0.88F);
        }

        // Glowing golden basal energy band around the bottom of the pillar (like in Genshin when placed)
        float glowW = baseW + 0.012F;
        drawSolidBox(vc, mat, -glowW, 0.02F, -glowW, glowW, 0.09F, glowW, 1.0F, 0.80F, 0.22F, 0.62F + pulseWave * 0.25F);
    }

    private static void drawAllFourFaceDiamonds(
            VertexConsumer vc,
            Matrix4f mat,
            float dist,
            float cy,
            float rad,
            float r, float g, float b, float a
    ) {
        // North (-Z)
        vc.vertex(mat, 0.0F, cy + rad, -dist).color(r, g, b, a).endVertex();
        vc.vertex(mat,  rad, cy,       -dist).color(r, g, b, a).endVertex();
        vc.vertex(mat, 0.0F, cy - rad, -dist).color(r, g, b, a).endVertex();
        vc.vertex(mat, -rad, cy,       -dist).color(r, g, b, a).endVertex();
        // South (+Z)
        vc.vertex(mat, 0.0F, cy + rad,  dist).color(r, g, b, a).endVertex();
        vc.vertex(mat, -rad, cy,        dist).color(r, g, b, a).endVertex();
        vc.vertex(mat, 0.0F, cy - rad,  dist).color(r, g, b, a).endVertex();
        vc.vertex(mat,  rad, cy,        dist).color(r, g, b, a).endVertex();
        // East (+X)
        vc.vertex(mat,  dist, cy + rad, 0.0F).color(r, g, b, a).endVertex();
        vc.vertex(mat,  dist, cy,        rad).color(r, g, b, a).endVertex();
        vc.vertex(mat,  dist, cy - rad, 0.0F).color(r, g, b, a).endVertex();
        vc.vertex(mat,  dist, cy,       -rad).color(r, g, b, a).endVertex();
        // West (-X)
        vc.vertex(mat, -dist, cy + rad, 0.0F).color(r, g, b, a).endVertex();
        vc.vertex(mat, -dist, cy,       -rad).color(r, g, b, a).endVertex();
        vc.vertex(mat, -dist, cy - rad, 0.0F).color(r, g, b, a).endVertex();
        vc.vertex(mat, -dist, cy,        rad).color(r, g, b, a).endVertex();
    }

    private static void drawSolidBox(
            VertexConsumer vc,
            Matrix4f mat,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float r, float g, float b, float a
    ) {
        // Top (+Y)
        vc.vertex(mat, x0, y1, z0).color(r * 1.08F, g * 1.08F, b * 1.08F, a).endVertex();
        vc.vertex(mat, x0, y1, z1).color(r * 1.08F, g * 1.08F, b * 1.08F, a).endVertex();
        vc.vertex(mat, x1, y1, z1).color(r * 1.08F, g * 1.08F, b * 1.08F, a).endVertex();
        vc.vertex(mat, x1, y1, z0).color(r * 1.08F, g * 1.08F, b * 1.08F, a).endVertex();
        // Bottom (-Y)
        vc.vertex(mat, x0, y0, z0).color(r * 0.75F, g * 0.75F, b * 0.75F, a).endVertex();
        vc.vertex(mat, x1, y0, z0).color(r * 0.75F, g * 0.75F, b * 0.75F, a).endVertex();
        vc.vertex(mat, x1, y0, z1).color(r * 0.75F, g * 0.75F, b * 0.75F, a).endVertex();
        vc.vertex(mat, x0, y0, z1).color(r * 0.75F, g * 0.75F, b * 0.75F, a).endVertex();
        // North (-Z)
        vc.vertex(mat, x0, y1, z0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x1, y1, z0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x1, y0, z0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x0, y0, z0).color(r, g, b, a).endVertex();
        // South (+Z)
        vc.vertex(mat, x1, y1, z1).color(r * 0.94F, g * 0.94F, b * 0.94F, a).endVertex();
        vc.vertex(mat, x0, y1, z1).color(r * 0.94F, g * 0.94F, b * 0.94F, a).endVertex();
        vc.vertex(mat, x0, y0, z1).color(r * 0.94F, g * 0.94F, b * 0.94F, a).endVertex();
        vc.vertex(mat, x1, y0, z1).color(r * 0.94F, g * 0.94F, b * 0.94F, a).endVertex();
        // East (+X)
        vc.vertex(mat, x1, y1, z0).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
        vc.vertex(mat, x1, y1, z1).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
        vc.vertex(mat, x1, y0, z1).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
        vc.vertex(mat, x1, y0, z0).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
        // West (-X)
        vc.vertex(mat, x0, y1, z1).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
        vc.vertex(mat, x0, y1, z0).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
        vc.vertex(mat, x0, y0, z0).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
        vc.vertex(mat, x0, y0, z1).color(r * 0.88F, g * 0.88F, b * 0.88F, a).endVertex();
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

            consumer.vertex(pose, x0 * rIn, yBase, z0 * rIn).color(r, g, b, 0.0F).endVertex();
            consumer.vertex(pose, x1 * rIn, yBase, z1 * rIn).color(r, g, b, 0.0F).endVertex();
            consumer.vertex(pose, x1 * halfExtent, yBase, z1 * halfExtent).color(r, g, b, a).endVertex();
            consumer.vertex(pose, x0 * halfExtent, yBase, z0 * halfExtent).color(r, g, b, a).endVertex();

            consumer.vertex(pose, x0 * halfExtent, yBase, z0 * halfExtent).color(r, g, b, a).endVertex();
            consumer.vertex(pose, x1 * halfExtent, yBase, z1 * halfExtent).color(r, g, b, a).endVertex();
            consumer.vertex(pose, x1 * rOut, yBase, z1 * rOut).color(r, g, b, 0.0F).endVertex();
            consumer.vertex(pose, x0 * rOut, yBase, z0 * rOut).color(r, g, b, 0.0F).endVertex();

            consumer.vertex(pose, x0 * halfExtent, yBase - 0.04F, z0 * halfExtent).color(r, g, b, wallAlpha).endVertex();
            consumer.vertex(pose, x1 * halfExtent, yBase - 0.04F, z1 * halfExtent).color(r, g, b, wallAlpha).endVertex();
            consumer.vertex(pose, x1 * halfExtent, yWallTop, z1 * halfExtent).color(r, g, b, 0.0F).endVertex();
            consumer.vertex(pose, x0 * halfExtent, yWallTop, z0 * halfExtent).color(r, g, b, 0.0F).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GeoFangPillarEntity entity) {
        return entity.isStoneStele() ? STELE_TEXTURE : FANGS_TEXTURE;
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
