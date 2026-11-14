package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.HydroSalonSummonEntity;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.animal.Squid;
import org.joml.Matrix4f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Renders Furina's Salon Solitaire (Gentilhomme Usher, Surintendante Chevalmarin, Mademoiselle Crabaletta)
 * and Singer of Many Waters using Vanilla ModelLayers (SQUID, ALLAY, SPIDER) tinted with luminous Hydro
 * shaders plus custom 3D Hat, Ribbon, Bonnet, and Pneuma Tiara accessories.
 */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HydroSalonSummonRenderer extends EntityRenderer<HydroSalonSummonEntity> {
    private static final ResourceLocation SQUID_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/squid/squid.png");
    private static final ResourceLocation ALLAY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/allay/allay.png");
    private static final ResourceLocation SPIDER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");

    private static final RenderType ACCESSORY_RENDER_TYPE = HydroAccessoryRenderType.create();

    private final SquidModel<Squid> squidModel;
    private final AllayModel allayModel;
    private final SpiderModel<Spider> spiderModel;

    public HydroSalonSummonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.squidModel = new SquidModel<>(context.bakeLayer(ModelLayers.SQUID));
        this.allayModel = new AllayModel(context.bakeLayer(ModelLayers.ALLAY));
        this.spiderModel = new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER));
        this.shadowRadius = 0.35F;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VeloriseStavesMod.HYDRO_SALON_SUMMON.get(), HydroSalonSummonRenderer::new);
    }

    @Override
    public void render(
            HydroSalonSummonEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        int role = entity.getRole();
        float ageInTicks = entity.tickCount + partialTicks;
        int sinceAction = entity.tickCount - entity.getLastActionTick();
        float actionPulse = (sinceAction >= 0 && sinceAction < 10)
                ? Mth.sin((Mth.clamp(sinceAction + partialTicks, 0.0F, 10.0F) / 10.0F) * (float) Math.PI)
                : 0.0F;

        int fullBright = LightTexture.FULL_BRIGHT;

        poseStack.pushPose();
        float bob = Mth.sin(ageInTicks * 0.12F + role * 1.7F) * 0.06F;
        poseStack.translate(0.0F, 0.35F + bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));

        if (role == HydroSalonSummonEntity.ROLE_USHER) {
            // Gentilhomme Usher: Compact royal-blue Hydro Octopus + Gentleman's Top Hat
            poseStack.pushPose();
            float s = 0.46F + actionPulse * 0.06F;
            poseStack.scale(-s, -s, s);
            poseStack.translate(0.0F, -1.15F, 0.0F);
            float tentaclePhase = entity.getTentaclePhase(partialTicks);
            float tentacleAngle = 0.20F + Mth.abs(Mth.sin(tentaclePhase)) * 0.65F + actionPulse * 0.45F;
            this.squidModel.setupAnim(null, 0.0F, 0.0F, tentacleAngle, 0.0F, 0.0F);
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SQUID_TEXTURE));
            this.squidModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0xFF2878F0);
            poseStack.popPose();

            // 3D Top Hat on Usher's head
            VertexConsumer hatVc = bufferSource.getBuffer(ACCESSORY_RENDER_TYPE);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.56F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
            Matrix4f mat = poseStack.last().pose();
            // Brim (Deep Navy Ousia)
            drawBox(hatVc, mat, -0.24F, 0.0F, -0.24F, 0.24F, 0.035F, 0.24F, 0.08F, 0.16F, 0.38F, 0.95F);
            // Ribbon band (Glowing Cyan)
            drawBox(hatVc, mat, -0.155F, 0.035F, -0.155F, 0.155F, 0.085F, 0.155F, 0.35F, 0.88F, 1.0F, 0.95F);
            // Tall Cylinder Crown
            drawBox(hatVc, mat, -0.15F, 0.085F, -0.15F, 0.15F, 0.30F, 0.15F, 0.10F, 0.22F, 0.50F, 0.95F);
            poseStack.popPose();

        } else if (role == HydroSalonSummonEntity.ROLE_CHEVALMARIN) {
            // Surintendante Chevalmarin: Agile Cerulean Seahorse-Sprite + Maid Bow Ribbon
            poseStack.pushPose();
            float s = 0.88F + actionPulse * 0.08F;
            poseStack.scale(-s, -s, s);
            poseStack.translate(0.0F, -1.35F, 0.0F);
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ALLAY_TEXTURE));
            this.allayModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0xFF3898FF);
            poseStack.popPose();

            // 3D Maid Ribbon Bow on Chevalmarin's head
            VertexConsumer bowVc = bufferSource.getBuffer(ACCESSORY_RENDER_TYPE);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.46F, 0.02F);
            Matrix4f mat = poseStack.last().pose();
            // Center pearl knot
            drawBox(bowVc, mat, -0.05F, 0.0F, -0.04F, 0.05F, 0.08F, 0.04F, 0.92F, 0.98F, 1.0F, 0.95F);
            // Left & Right Maid Bow Wings
            drawBox(bowVc, mat, -0.20F, -0.02F, -0.03F, -0.05F, 0.11F, 0.03F, 0.25F, 0.75F, 1.0F, 0.92F);
            drawBox(bowVc, mat, 0.05F, -0.02F, -0.03F, 0.20F, 0.11F, 0.03F, 0.25F, 0.75F, 1.0F, 0.92F);
            poseStack.popPose();

        } else if (role == HydroSalonSummonEntity.ROLE_CRABALETTA) {
            // Mademoiselle Crabaletta: Armored Hydro Crab + Frilled Noble Bonnet
            poseStack.pushPose();
            float s = 0.58F + actionPulse * 0.10F;
            poseStack.scale(-s * 1.05F, -s * 0.78F, s * 0.85F);
            poseStack.translate(0.0F, -1.32F, 0.0F);
            this.spiderModel.setupAnim(null, ageInTicks * 0.35F, 0.35F, ageInTicks, 0.0F, 0.0F);
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SPIDER_TEXTURE));
            this.spiderModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0xFF42A5FF);
            poseStack.popPose();

            // 3D Frilled Noble Bonnet on Crabaletta's carapace
            VertexConsumer bonnetVc = bufferSource.getBuffer(ACCESSORY_RENDER_TYPE);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.32F, 0.04F);
            Matrix4f mat = poseStack.last().pose();
            // White lace frill brim
            drawBox(bonnetVc, mat, -0.22F, 0.0F, -0.18F, 0.22F, 0.04F, 0.18F, 0.95F, 0.98F, 1.0F, 0.95F);
            // Azure bonnet dome
            drawBox(bonnetVc, mat, -0.16F, 0.04F, -0.14F, 0.16F, 0.14F, 0.14F, 0.32F, 0.78F, 1.0F, 0.95F);
            poseStack.popPose();

        } else {
            // Singer of Many Waters (Pneuma): Luminous Aqua-Gold Spirit + 3-Peak Tiara + Healing Rings
            poseStack.pushPose();
            float s = 1.18F + actionPulse * 0.10F;
            poseStack.scale(-s, -s, s);
            poseStack.translate(0.0F, -1.35F, 0.0F);
            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ALLAY_TEXTURE));
            this.allayModel.renderToBuffer(poseStack, vc, fullBright, OverlayTexture.NO_OVERLAY, 0xFF88F0FF);
            poseStack.popPose();

            // 3D Three-Peak Pneuma Tiara Crown
            VertexConsumer crownVc = bufferSource.getBuffer(ACCESSORY_RENDER_TYPE);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.62F, 0.0F);
            Matrix4f mat = poseStack.last().pose();
            // Golden Pneuma base band
            drawBox(crownVc, mat, -0.16F, 0.0F, -0.14F, 0.16F, 0.045F, 0.14F, 1.0F, 0.90F, 0.48F, 0.95F);
            // Center tall aqua-gold peak
            drawBox(crownVc, mat, -0.04F, 0.045F, -0.14F, 0.04F, 0.18F, -0.10F, 0.55F, 0.96F, 1.0F, 0.95F);
            // Left & Right side peaks
            drawBox(crownVc, mat, -0.15F, 0.045F, -0.14F, -0.09F, 0.13F, -0.10F, 1.0F, 0.92F, 0.55F, 0.95F);
            drawBox(crownVc, mat, 0.09F, 0.045F, -0.14F, 0.15F, 0.13F, -0.10F, 1.0F, 0.92F, 0.55F, 0.95F);
            poseStack.popPose();
        }

        poseStack.popPose();

        // Render expanding Pneuma healing ring on ground for Singer of Many Waters
        if (role == HydroSalonSummonEntity.ROLE_SINGER && sinceAction >= 0 && sinceAction < 28) {
            float progress = Mth.clamp((sinceAction + partialTicks) / 28.0F, 0.0F, 1.0F);
            float maxR = entity.isNetherite() ? 8.0F : 6.0F;
            float curR = 0.6F + progress * maxR;
            float alpha = (1.0F - progress) * 0.65F;
            VertexConsumer ringVc = bufferSource.getBuffer(ACCESSORY_RENDER_TYPE);
            Matrix4f pose = poseStack.last().pose();
            drawHorizontalRing(ringVc, pose, curR, 0.18F, 0.05F, 0.45F, 0.92F, 1.0F, alpha);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void drawHorizontalRing(
            VertexConsumer vc,
            Matrix4f mat,
            float radius,
            float halfWidth,
            float y,
            float r,
            float g,
            float b,
            float a
    ) {
        int segments = 24;
        float rIn = Math.max(0.05F, radius - halfWidth);
        float rOut = radius + halfWidth;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (i * 2.0 * Math.PI / segments);
            float a1 = (float) ((i + 1) * 2.0 * Math.PI / segments);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            emitVertex(vc, mat, c0 * rIn, y, s0 * rIn, r, g, b, a);
            emitVertex(vc, mat, c1 * rIn, y, s1 * rIn, r, g, b, a);
            emitVertex(vc, mat, c1 * rOut, y, s1 * rOut, r, g, b, 0.0F);
            emitVertex(vc, mat, c0 * rOut, y, s0 * rOut, r, g, b, 0.0F);
        }
    }

    private static void drawBox(
            VertexConsumer vc,
            Matrix4f mat,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float r, float g, float b, float a
    ) {
        // Top (+Y)
        emitVertex(vc, mat, x0, y1, z0, r, g, b, a);
        emitVertex(vc, mat, x0, y1, z1, r, g, b, a);
        emitVertex(vc, mat, x1, y1, z1, r, g, b, a);
        emitVertex(vc, mat, x1, y1, z0, r, g, b, a);
        // Bottom (-Y)
        emitVertex(vc, mat, x0, y0, z0, r * 0.8F, g * 0.8F, b * 0.8F, a);
        emitVertex(vc, mat, x1, y0, z0, r * 0.8F, g * 0.8F, b * 0.8F, a);
        emitVertex(vc, mat, x1, y0, z1, r * 0.8F, g * 0.8F, b * 0.8F, a);
        emitVertex(vc, mat, x0, y0, z1, r * 0.8F, g * 0.8F, b * 0.8F, a);
        // Front (+Z)
        emitVertex(vc, mat, x0, y0, z1, r * 0.92F, g * 0.92F, b * 0.92F, a);
        emitVertex(vc, mat, x1, y0, z1, r * 0.92F, g * 0.92F, b * 0.92F, a);
        emitVertex(vc, mat, x1, y1, z1, r * 0.92F, g * 0.92F, b * 0.92F, a);
        emitVertex(vc, mat, x0, y1, z1, r * 0.92F, g * 0.92F, b * 0.92F, a);
        // Back (-Z)
        emitVertex(vc, mat, x0, y0, z0, r * 0.88F, g * 0.88F, b * 0.88F, a);
        emitVertex(vc, mat, x0, y1, z0, r * 0.88F, g * 0.88F, b * 0.88F, a);
        emitVertex(vc, mat, x1, y1, z0, r * 0.88F, g * 0.88F, b * 0.88F, a);
        emitVertex(vc, mat, x1, y0, z0, r * 0.88F, g * 0.88F, b * 0.88F, a);
        // Left (-X)
        emitVertex(vc, mat, x0, y0, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emitVertex(vc, mat, x0, y0, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emitVertex(vc, mat, x0, y1, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emitVertex(vc, mat, x0, y1, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        // Right (+X)
        emitVertex(vc, mat, x1, y0, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emitVertex(vc, mat, x1, y1, z0, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emitVertex(vc, mat, x1, y1, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
        emitVertex(vc, mat, x1, y0, z1, r * 0.85F, g * 0.85F, b * 0.85F, a);
    }

    private static void emitVertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(mat, x, y, z).setColor(r, g, b, a);
    }

    @Override
    public ResourceLocation getTextureLocation(HydroSalonSummonEntity entity) {
        return switch (entity.getRole()) {
            case HydroSalonSummonEntity.ROLE_USHER -> SQUID_TEXTURE;
            case HydroSalonSummonEntity.ROLE_CRABALETTA -> SPIDER_TEXTURE;
            default -> ALLAY_TEXTURE;
        };
    }

    protected static class HydroAccessoryRenderType extends RenderType {
        public static RenderType create() {
            CompositeState state = CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false);
            return create("velorise_hydro_accessory", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true, state);
        }

        private HydroAccessoryRenderType(
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
