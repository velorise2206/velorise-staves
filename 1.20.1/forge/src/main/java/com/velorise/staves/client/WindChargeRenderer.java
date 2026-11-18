package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.WindChargeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
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

/**
 * 1.20.1 backport of Minecraft 1.21.1 WindChargeRenderer + WindChargeModel
 * using the official 64x32 swirling wind charge projectile texture.
 */
@Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class WindChargeRenderer extends EntityRenderer<WindChargeEntity> {
    public static final ModelLayerLocation WIND_CHARGE_LAYER =
            new ModelLayerLocation(new ResourceLocation(VeloriseStavesMod.MODID, "wind_charge"), "main");
    private static final float MIN_CAMERA_DISTANCE_SQUARED = Mth.square(3.5F);
    private static final ResourceLocation TEXTURE_LOCATION =
            new ResourceLocation(VeloriseStavesMod.MODID, "textures/entity/projectiles/wind_charge.png");

    private final WindChargeModel model;

    public WindChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new WindChargeModel(context.bakeLayer(WIND_CHARGE_LAYER));
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WIND_CHARGE_LAYER, WindChargeModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VeloriseStavesMod.WIND_CHARGE_ENTITY.get(), WindChargeRenderer::new);
    }

    @Override
    public void render(
            WindChargeEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (entity.tickCount < 2
                && this.entityRenderDispatcher.camera != null
                && this.entityRenderDispatcher.camera.getEntity() != null
                && this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < MIN_CAMERA_DISTANCE_SQUARED) {
            return;
        }
        float age = entity.tickCount + partialTicks;
        float uOffset = (age * 0.03F) % 1.0F;
        VertexConsumer consumer = bufferSource.getBuffer(BreezeWindRenderType.create(TEXTURE_LOCATION, uOffset));
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WindChargeEntity entity) {
        return TEXTURE_LOCATION;
    }

    public static final class WindChargeModel extends HierarchicalModel<WindChargeEntity> {
        private static final float ROTATION_SPEED = 16.0F;
        private final ModelPart bone;
        private final ModelPart windCharge;
        private final ModelPart wind;

        public WindChargeModel(ModelPart root) {
            super(RenderType::entityTranslucent);
            this.bone = root.getChild("bone");
            this.wind = this.bone.getChild("wind");
            this.windCharge = this.bone.getChild("wind_charge");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
            bone.addOrReplaceChild(
                    "wind",
                    CubeListBuilder.create()
                            .texOffs(15, 20)
                            .addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                            .texOffs(0, 9)
                            .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
            );
            bone.addOrReplaceChild(
                    "wind_charge",
                    CubeListBuilder.create()
                            .texOffs(0, 0)
                            .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                    PartPose.offset(0.0F, 0.0F, 0.0F)
            );
            return LayerDefinition.create(mesh, 64, 32);
        }

        @Override
        public void setupAnim(WindChargeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float rad = ROTATION_SPEED * ((float) Math.PI / 180.0F);
            this.windCharge.yRot = -ageInTicks * rad;
            this.wind.yRot = ageInTicks * rad;
        }

        @Override
        public ModelPart root() {
            return this.bone;
        }
    }

    private static final class BreezeWindRenderType extends RenderType {
        private BreezeWindRenderType(
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

        static RenderType create(ResourceLocation texture, float uOffset) {
            CompositeState state = CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(uOffset, 0.0F))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false);
            return create("velorise_breeze_wind", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, state);
        }
    }
}
