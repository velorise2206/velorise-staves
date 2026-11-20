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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Frostcrown of the Cryo Archon Tsaritsa (Anastasya Feodorovna Snezhnaya).
 * Modeled after Genshin Impact 7.3 Kokoshnik Frostcrown design.
 * Root bone 'head' aligns directly with HumanoidModel.head (0.0F, 0.0F, 0.0F).
 * Compatible with Forge 1.20.1, NeoForge 1.21.1, and Fabric 1.21.1 (Mojang Mappings).
 */
public class TsaritsaCrownModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.tryBuild("velorise_staves", "tsaritsa_crown"), "main");

    private final ModelPart head;
    private final ModelPart circletBand;
    private final ModelPart kokoshnikArch;
    private final ModelPart centerFrostSpire;
    private final ModelPart radialCrystalSpikes;
    private final ModelPart templePendants;

    public TsaritsaCrownModel(ModelPart root) {
        this.head = root.getChild("head");
        this.circletBand = this.head.getChild("circlet_band");
        this.kokoshnikArch = this.head.getChild("kokoshnik_arch");
        this.centerFrostSpire = this.kokoshnikArch.getChild("center_frost_spire");
        this.radialCrystalSpikes = this.kokoshnikArch.getChild("radial_crystal_spikes");
        this.templePendants = this.head.getChild("temple_pendants");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition circlet_band = head.addOrReplaceChild("circlet_band", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.4F, -0.5F, -4.6F, 8.8F, 1.0F, 0.7F, new CubeDeformation(0.0F))
                .texOffs(0, 8).addBox(-4.4F, -0.5F, 3.9F, 8.8F, 1.0F, 0.6F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-4.6F, -0.5F, -4.4F, 0.7F, 1.0F, 8.8F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(3.9F, -0.5F, -4.4F, 0.7F, 1.0F, 8.8F, new CubeDeformation(0.0F)),
                PartPose.offset(0F, -6.3F, 0F));
        circlet_band.addOrReplaceChild("forehead_gem_r1", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-0.65F, -0.65F, -0.3F, 1.3F, 1.3F, 0.6F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0F, 0.65F, -4.55F, 0.0F, 0.0F, 0.7854F));

        PartDefinition kokoshnik_arch = head.addOrReplaceChild("kokoshnik_arch", CubeListBuilder.create(),
                PartPose.offset(0F, -8.0F, -3.6F));
        kokoshnik_arch.addOrReplaceChild("arch_base_l_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.35F, -0.85F, -0.45F, 2.7F, 1.7F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.85F, 0.65F, -0.45F, 0.2094F, -0.2094F, 0.3142F));
        kokoshnik_arch.addOrReplaceChild("arch_base_r_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.35F, -0.85F, -0.45F, 2.7F, 1.7F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.85F, 0.65F, -0.45F, 0.2094F, 0.2094F, -0.3142F));
        kokoshnik_arch.addOrReplaceChild("arch_mid_l_r1", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.2F, -1.0F, -0.45F, 2.4F, 2.0F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.0F, -0.8F, -0.05F, 0.2443F, -0.1396F, 0.5585F));
        kokoshnik_arch.addOrReplaceChild("arch_mid_r_r1", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.2F, -1.0F, -0.45F, 2.4F, 2.0F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -0.8F, -0.05F, 0.2443F, 0.1396F, -0.5585F));
        kokoshnik_arch.addOrReplaceChild("arch_crest_bridge_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.2F, -0.65F, -0.45F, 4.4F, 1.3F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0F, -1.25F, 0.15F, 0.2443F, 0.0F, 0.0F));

        PartDefinition center_frost_spire = kokoshnik_arch.addOrReplaceChild("center_frost_spire", CubeListBuilder.create(),
                PartPose.offset(0F, -2.0F, 0.2F));
        center_frost_spire.addOrReplaceChild("center_gem_backing_r1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.2F, -1.25F, -0.45F, 2.4F, 2.5F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0F, 0.25F, -0.1F, 0.2443F, 0.0F, 0.7854F));
        center_frost_spire.addOrReplaceChild("center_sapphire_core_r1", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-0.75F, -0.75F, -0.4F, 1.5F, 1.5F, 0.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0F, 0.35F, -0.35F, 0.2443F, 0.0F, 0.7854F));
        center_frost_spire.addOrReplaceChild("center_tall_crystal_r1", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-0.55F, -1.7F, -0.4F, 1.1F, 3.4F, 0.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0F, -1.9F, 0.15F, 0.2443F, 0.0F, 0.0F));
        center_frost_spire.addOrReplaceChild("center_cross_flare_r1", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.4F, -0.4F, -0.35F, 2.8F, 0.8F, 0.7F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0F, -1.5F, 0.15F, 0.2443F, 0.0F, 0.0F));

        PartDefinition radial_crystal_spikes = kokoshnik_arch.addOrReplaceChild("radial_crystal_spikes", CubeListBuilder.create(),
                PartPose.offset(0F, -1.0F, 0.3F));
        radial_crystal_spikes.addOrReplaceChild("spike_inner_l_r1", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-0.45F, -1.5F, -0.4F, 0.9F, 3.0F, 0.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.95F, -1.7F, 0.0F, 0.2443F, -0.1047F, 0.3927F));
        radial_crystal_spikes.addOrReplaceChild("spike_inner_r_r1", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-0.45F, -1.5F, -0.4F, 0.9F, 3.0F, 0.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.95F, -1.7F, 0.0F, 0.2443F, 0.1047F, -0.3927F));
        radial_crystal_spikes.addOrReplaceChild("spike_mid_l_r1", CubeListBuilder.create()
                .texOffs(32, 32).addBox(-0.5F, -1.55F, -0.4F, 1.0F, 3.1F, 0.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.7F, -0.65F, -0.1F, 0.2094F, -0.2094F, 0.733F));
        radial_crystal_spikes.addOrReplaceChild("spike_mid_r_r1", CubeListBuilder.create()
                .texOffs(32, 32).addBox(-0.5F, -1.55F, -0.4F, 1.0F, 3.1F, 0.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.7F, -0.65F, -0.1F, 0.2094F, 0.2094F, -0.733F));
        radial_crystal_spikes.addOrReplaceChild("spike_outer_l_r1", CubeListBuilder.create()
                .texOffs(32, 32).addBox(-0.45F, -1.25F, -0.35F, 0.9F, 2.5F, 0.7F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.95F, 0.95F, -0.25F, 0.1745F, -0.2793F, 1.0821F));
        radial_crystal_spikes.addOrReplaceChild("spike_outer_r_r1", CubeListBuilder.create()
                .texOffs(32, 32).addBox(-0.45F, -1.25F, -0.35F, 0.9F, 2.5F, 0.7F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.95F, 0.95F, -0.25F, 0.1745F, 0.2793F, -1.0821F));

        PartDefinition temple_pendants = head.addOrReplaceChild("temple_pendants", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.75F, -1.0F, -0.3F, 0.6F, 2.0F, 0.6F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(4.15F, -1.0F, -0.3F, 0.6F, 2.0F, 0.6F, new CubeDeformation(0.0F)),
                PartPose.offset(0F, -5.0F, -2.9F));
        temple_pendants.addOrReplaceChild("pendant_l_crystal_r1", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-0.45F, -0.8F, -0.45F, 0.9F, 1.6F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.45F, 1.7F, 0.0F, 0.0F, 0.0F, 0.7854F));
        temple_pendants.addOrReplaceChild("pendant_r_crystal_r1", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-0.45F, -0.8F, -0.45F, 0.9F, 1.6F, 0.9F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.45F, 1.7F, 0.0F, 0.0F, 0.0F, 0.7854F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /**
     * Syncs the crown rotation/position directly to a player or humanoid head ModelPart.
     */
    public void copyFromHead(ModelPart humanoidHead) {
        this.head.copyFrom(humanoidHead);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public ModelPart getHead() {
        return this.head;
    }
}