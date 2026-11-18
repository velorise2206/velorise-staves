// Made with Blockbench 4.10 (Modded Entity)
package com.velorise.staves.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class MavuikaHelmetModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("velorise_staves", "mavuika_helmet"), "main");
    public final ModelPart helmet;
    public final ModelPart helmet_shell;
    public final ModelPart crest;
    public final ModelPart visor;
    public final ModelPart side_fins;

    public MavuikaHelmetModel(ModelPart root) {
        this.helmet = root.getChild("helmet");
        this.helmet_shell = this.helmet.getChild("helmet_shell");
        this.crest = this.helmet.getChild("crest");
        this.visor = this.helmet.getChild("visor");
        this.side_fins = this.helmet.getChild("side_fins");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition helmet = partdefinition.addOrReplaceChild("helmet", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition helmet_shell = helmet.addOrReplaceChild("helmet_shell", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-4.6F, -9.2F, -4.6F, 9.2F, 2.2F, 9.2F, CubeDeformation.NONE)
            .texOffs(41, 0).addBox(-4.2F, -9.6F, -3.4F, 1.4F, 0.7F, 7.6F, CubeDeformation.NONE)
            .texOffs(62, 0).addBox(2.8F, -9.6F, -3.4F, 1.4F, 0.7F, 7.6F, CubeDeformation.NONE)
            .texOffs(83, 0).addBox(-4.6F, -7.2F, 2.8F, 9.2F, 7.3F, 2.0F, CubeDeformation.NONE)
            .texOffs(108, 0).addBox(-4.8F, -7.2F, -3.8F, 1.1F, 7.3F, 7.2F, CubeDeformation.NONE)
            .texOffs(0, 17).addBox(3.7F, -7.2F, -3.8F, 1.1F, 7.3F, 7.2F, CubeDeformation.NONE)
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        helmet_shell.addOrReplaceChild("rear_spoiler_r", CubeListBuilder.create().texOffs(21, 17).addBox(-3.6F, -0.6F, -0.6F, 7.2F, 1.3F, 2.4F, CubeDeformation.NONE), PartPose.offsetAndRotation(0F, -8.2F, 4.2F, -0.3142F, 0.0F, 0.0F));
        helmet_shell.addOrReplaceChild("rear_fin_L_r", CubeListBuilder.create().texOffs(44, 17).addBox(-0.5F, -0.9F, -0.6F, 1.0F, 1.8F, 2.2F, CubeDeformation.NONE), PartPose.offsetAndRotation(-2.5F, -8.4F, 4.6F, -0.4189F, 0.1745F, 0.0F));
        helmet_shell.addOrReplaceChild("rear_fin_R_r", CubeListBuilder.create().texOffs(53, 17).addBox(-0.5F, -0.9F, -0.6F, 1.0F, 1.8F, 2.2F, CubeDeformation.NONE), PartPose.offsetAndRotation(2.5F, -8.4F, 4.6F, -0.4189F, -0.1745F, 0.0F));

        PartDefinition crest = helmet.addOrReplaceChild("crest", CubeListBuilder.create()
            .texOffs(62, 17).addBox(-1.0F, -9.8F, -5.0F, 2.0F, 1.5F, 8.8F, CubeDeformation.NONE)
            .texOffs(85, 17).addBox(-0.5F, -10.3F, -4.0F, 1.0F, 0.8F, 6.4F, CubeDeformation.NONE)
            .texOffs(102, 17).addBox(-4.8F, -7.4F, -5.1F, 9.6F, 0.9F, 1.2F, CubeDeformation.NONE)
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        crest.addOrReplaceChild("brow_horn_L_r", CubeListBuilder.create().texOffs(0, 34).addBox(-0.9F, -0.8F, -0.5F, 1.8F, 1.0F, 1.1F, CubeDeformation.NONE), PartPose.offsetAndRotation(-4.4F, -7.4F, -4.6F, 0.0F, 0.0F, -0.3142F));
        crest.addOrReplaceChild("brow_horn_R_r", CubeListBuilder.create().texOffs(9, 34).addBox(-0.9F, -0.8F, -0.5F, 1.8F, 1.0F, 1.1F, CubeDeformation.NONE), PartPose.offsetAndRotation(4.4F, -7.4F, -4.6F, 0.0F, 0.0F, 0.3142F));

        PartDefinition visor = helmet.addOrReplaceChild("visor", CubeListBuilder.create()
            .texOffs(18, 34).addBox(-4.5F, -6.6F, -4.95F, 9.0F, 3.4F, 1.1F, CubeDeformation.NONE)
            .texOffs(41, 34).addBox(-3.7F, -5.4F, -5.1F, 7.4F, 1.3F, 0.4F, CubeDeformation.NONE)
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        visor.addOrReplaceChild("chin_guard_r", CubeListBuilder.create().texOffs(60, 34).addBox(-4.5F, -1.6F, -1.0F, 9.0F, 3.2F, 2.2F, CubeDeformation.NONE), PartPose.offsetAndRotation(0F, -1.6F, -4.0F, 0.1745F, 0.0F, 0.0F));
        visor.addOrReplaceChild("chin_plate_r", CubeListBuilder.create().texOffs(85, 34).addBox(-2.8F, -1.1F, -0.35F, 5.6F, 2.3F, 0.6F, CubeDeformation.NONE), PartPose.offsetAndRotation(0F, -1.6F, -4.9F, 0.1396F, 0.0F, 0.0F));
        visor.addOrReplaceChild("chin_emblem_r", CubeListBuilder.create().texOffs(100, 34).addBox(-1.3F, -0.6F, -0.3F, 2.6F, 1.3F, 0.4F, CubeDeformation.NONE), PartPose.offsetAndRotation(0F, -1.2F, -5.1F, 0.1396F, 0.0F, 0.0F));

        PartDefinition side_fins = helmet.addOrReplaceChild("side_fins", CubeListBuilder.create()
            .texOffs(109, 34).addBox(-5.1F, -6.6F, -2.0F, 0.9F, 3.2F, 5.0F, CubeDeformation.NONE)
            .texOffs(0, 44).addBox(4.2F, -6.6F, -2.0F, 0.9F, 3.2F, 5.0F, CubeDeformation.NONE)
            .texOffs(13, 44).addBox(-5.3F, -5.6F, -1.4F, 0.4F, 0.9F, 3.4F, CubeDeformation.NONE)
            .texOffs(24, 44).addBox(4.9F, -5.6F, -1.4F, 0.4F, 0.9F, 3.4F, CubeDeformation.NONE)
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        side_fins.addOrReplaceChild("aero_fin_L_r", CubeListBuilder.create().texOffs(35, 44).addBox(-0.4F, -0.9F, -1.8F, 0.6F, 1.7F, 4.6F, CubeDeformation.NONE), PartPose.offsetAndRotation(-5.2F, -5.4F, 1.4F, -0.3491F, 0.2094F, 0.0F));
        side_fins.addOrReplaceChild("aero_fin_R_r", CubeListBuilder.create().texOffs(48, 44).addBox(-0.2F, -0.9F, -1.8F, 0.6F, 1.7F, 4.6F, CubeDeformation.NONE), PartPose.offsetAndRotation(5.2F, -5.4F, 1.4F, -0.3491F, -0.2094F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.helmet.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
