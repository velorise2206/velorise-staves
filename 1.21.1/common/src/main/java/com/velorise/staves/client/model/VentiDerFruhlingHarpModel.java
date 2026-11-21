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
 * Venti's Holy Lyre 'Der Fruhling' converted from venti_der_fruhling_harp.bbmodel (128x128, 137 elements).
 */
public class VentiDerFruhlingHarpModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("velorise_staves", "venti_der_fruhling_harp"), "main");
    private final ModelPart root;
    private final ModelPart harpBody;
    private final ModelPart[] strings = new ModelPart[7];
    private final ModelPart windRing;
    private final ModelPart[] notes = new ModelPart[3];
    private final ModelPart[] petals = new ModelPart[2];

    public VentiDerFruhlingHarpModel(ModelPart bakedRoot) {
        this.root = bakedRoot.getChild("root");
        this.harpBody = this.root.getChild("harp_body");
        ModelPart stringsGroup = this.harpBody.getChild("strings_group");
        for (int i = 0; i < 7; i++) {
            this.strings[i] = stringsGroup.getChild("string_" + (i + 1));
        }
        ModelPart anemoFx = this.harpBody.getChild("anemo_fx");
        this.windRing = anemoFx.getChild("wind_ring");
        for (int i = 0; i < 3; i++) {
            this.notes[i] = anemoFx.getChild("note_" + (i + 1));
        }
        for (int i = 0; i < 2; i++) {
            this.petals[i] = anemoFx.getChild("petal_" + (i + 1));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition harp_body = root.addOrReplaceChild("harp_body", CubeListBuilder.create(), PartPose.offset(0.0F, -10.0F, 0.0F));
        PartDefinition soundbox = harp_body.addOrReplaceChild("soundbox", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 2.8F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.12F)).texOffs(4, 0).addBox(-0.5F, 2.6F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)).texOffs(8, 0).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(-0.04F)).texOffs(16, 0).addBox(-1.5F, 1.4F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(26, 0).addBox(-2.5F, 0.6F, -1.0F, 5.0F, 1.0F, 2.0F, new CubeDeformation(-0.01F)).texOffs(40, 0).addBox(-3.5F, -1.2F, -1.5F, 7.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(60, 0).addBox(-4.5F, -2.8F, -1.5F, 9.0F, 2.0F, 3.0F, new CubeDeformation(-0.02F)).texOffs(84, 0).addBox(-5.0F, -3.2F, -1.5F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.02F)).texOffs(110, 0).addBox(-2.5F, -2.5F, -1.65F, 5.0F, 3.0F, 1.0F, new CubeDeformation(-0.08F)).texOffs(0, 5).addBox(-2.5F, -2.5F, 0.65F, 5.0F, 3.0F, 1.0F, new CubeDeformation(-0.08F)).texOffs(36, 5).addBox(-4.5F, -3.3F, -1.7F, 9.0F, 1.0F, 1.0F, new CubeDeformation(-0.18F)).texOffs(56, 5).addBox(-4.5F, -3.3F, 0.7F, 9.0F, 1.0F, 1.0F, new CubeDeformation(-0.18F)).texOffs(108, 5).addBox(-3.5F, -3.9F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(112, 5).addBox(-2.5F, -3.9F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(116, 5).addBox(-1.5F, -3.9F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(120, 5).addBox(-0.5F, -3.9F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(124, 5).addBox(0.5F, -3.9F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(0, 10).addBox(1.5F, -3.9F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(4, 10).addBox(2.5F, -3.9F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)), PartPose.offset(0.0F, 5.0F, 0.0F));
        soundbox.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(12, 5).addBox(-0.5F, -1.8F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.8F, 0.8F, 0.0F, 0.0F, 0.0F, 0.6632F));
        soundbox.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(18, 5).addBox(-0.5F, -1.8F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.8F, 0.8F, 0.0F, 0.0F, 0.0F, -0.6632F));
        soundbox.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(24, 5).addBox(-0.5F, -1.8F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(-0.04F)), PartPose.offsetAndRotation(-4.3F, -1.2F, 0.0F, 0.0F, 0.0F, 0.384F));
        soundbox.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(30, 5).addBox(-0.5F, -1.8F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(-0.04F)), PartPose.offsetAndRotation(4.3F, -1.2F, 0.0F, 0.0F, 0.0F, -0.384F));
        soundbox.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(76, 5).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(-2.2F, -1.0F, -1.25F, 0.0F, 0.0F, 0.4887F));
        soundbox.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(80, 5).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.24F)), PartPose.offsetAndRotation(-3.1F, -1.4F, -1.25F, 0.0F, 0.0F, -0.6109F));
        soundbox.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(84, 5).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(-2.2F, -1.0F, 1.25F, 0.0F, 0.0F, 0.4887F));
        soundbox.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(88, 5).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.24F)), PartPose.offsetAndRotation(-3.1F, -1.4F, 1.25F, 0.0F, 0.0F, -0.6109F));
        soundbox.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(92, 5).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(2.2F, -1.0F, -1.25F, 0.0F, 0.0F, -0.4887F));
        soundbox.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(96, 5).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.24F)), PartPose.offsetAndRotation(3.1F, -1.4F, -1.25F, 0.0F, 0.0F, 0.6109F));
        soundbox.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(100, 5).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(2.2F, -1.0F, 1.25F, 0.0F, 0.0F, -0.4887F));
        soundbox.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(104, 5).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.24F)), PartPose.offsetAndRotation(3.1F, -1.4F, 1.25F, 0.0F, 0.0F, 0.6109F));
        PartDefinition anemo_gem = soundbox.addOrReplaceChild("anemo_gem", CubeListBuilder.create().texOffs(18, 10).addBox(-0.5F, -1.8F, -1.85F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.22F)).texOffs(22, 10).addBox(-0.5F, 0.8F, -1.85F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.22F)).texOffs(36, 10).addBox(-0.5F, -1.8F, 0.85F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.22F)).texOffs(40, 10).addBox(-0.5F, 0.8F, 0.85F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.22F)), PartPose.offset(0.0F, -0.5F, 0.0F));
        anemo_gem.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(8, 10).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, -1.35F, 0.0F, 0.0F, 0.7854F));
        anemo_gem.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(14, 10).addBox(-0.5F, -0.5F, -0.68F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(0.0F, 0.0F, -1.35F, 0.0F, 0.0F, 0.7854F));
        anemo_gem.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(26, 10).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.35F, 0.0F, 0.0F, 0.7854F));
        anemo_gem.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(32, 10).addBox(-0.5F, -0.5F, -0.32F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.35F, 0.0F, 0.0F, 0.7854F));
        PartDefinition left_arm = harp_body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(116, 10).addBox(-1.7F, -11.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.15F)), PartPose.offset(-4.5F, 2.5F, 0.0F));
        left_arm.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(44, 10).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, -1.4F, 0.0F, 0.0F, 0.0F, 0.4887F));
        left_arm.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(52, 10).addBox(-1.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(-0.1F, -1.4F, 0.0F, 0.0F, 0.0F, 0.4887F));
        left_arm.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(56, 10).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.02F)), PartPose.offsetAndRotation(-1.2F, -3.5F, 0.0F, 0.0F, 0.0F, 0.2443F));
        left_arm.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(64, 10).addBox(-1.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(-1.2F, -3.5F, 0.0F, 0.0F, 0.0F, 0.2443F));
        left_arm.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(68, 10).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7F, -5.8F, 0.0F, 0.0F, 0.0F, -0.0698F));
        left_arm.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(76, 10).addBox(-1.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(-1.7F, -5.8F, 0.0F, 0.0F, 0.0F, -0.0698F));
        left_arm.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(80, 10).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.02F)), PartPose.offsetAndRotation(-1.3F, -8.1F, 0.0F, 0.0F, 0.0F, -0.3491F));
        left_arm.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(88, 10).addBox(-1.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(-1.3F, -8.1F, 0.0F, 0.0F, 0.0F, -0.3491F));
        left_arm.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(92, 10).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.4F, -10.0F, 0.0F, 0.0F, 0.0F, -0.5585F));
        left_arm.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(100, 10).addBox(-1.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(-0.4F, -10.0F, 0.0F, 0.0F, 0.0F, -0.5585F));
        left_arm.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(104, 10).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-0.7F, -10.7F, 0.0F, 0.0F, 0.0F, 0.4189F));
        left_arm.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(108, 10).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-1.5F, -11.7F, 0.0F, 0.0F, 0.0F, 1.0821F));
        left_arm.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(112, 10).addBox(-0.5F, -0.6F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.1F, -11.1F, 0.0F, 0.0F, 0.0F, 1.9199F));
        left_arm.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(120, 10).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(0.7F, -1.5F, 0.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition left_wing_deco = left_arm.addOrReplaceChild("left_wing_deco", CubeListBuilder.create().texOffs(84, 15).addBox(-0.9F, -1.7F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.12F)), PartPose.offset(-2.0F, -4.0F, 0.0F));
        left_wing_deco.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(80, 15).addBox(-0.5F, -1.8F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-0.2F, -1.1F, 0.0F, 0.0F, 0.0F, 0.3142F));
        left_wing_deco.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(88, 15).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-0.9F, -2.0F, 0.0F, 0.0F, 0.0F, 0.6283F));
        left_wing_deco.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(92, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(-3.65F, -4.6F, 0.0F, 0.0F, 0.0F, 0.6283F));
        left_wing_deco.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(96, 15).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.12F)), PartPose.offsetAndRotation(-1.2F, -0.2F, 0.0F, 0.0F, 0.0F, 0.8727F));
        left_wing_deco.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(100, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(-3.95F, -2.8F, 0.0F, 0.0F, 0.0F, 0.8727F));
        left_wing_deco.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(104, 15).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(-0.14F)), PartPose.offsetAndRotation(-0.9F, 1.4F, 0.0F, 0.0F, 0.0F, 1.117F));
        left_wing_deco.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(108, 15).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(-0.2F, 2.7F, 0.0F, 0.0F, 0.0F, 1.3614F));
        PartDefinition right_arm = harp_body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(72, 15).addBox(0.7F, -11.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.15F)), PartPose.offset(4.5F, 2.5F, 0.0F));
        right_arm.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(0, 15).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -1.4F, 0.0F, 0.0F, 0.0F, -0.4887F));
        right_arm.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(8, 15).addBox(0.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(0.1F, -1.4F, 0.0F, 0.0F, 0.0F, -0.4887F));
        right_arm.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(12, 15).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.02F)), PartPose.offsetAndRotation(1.2F, -3.5F, 0.0F, 0.0F, 0.0F, -0.2443F));
        right_arm.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(20, 15).addBox(0.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(1.2F, -3.5F, 0.0F, 0.0F, 0.0F, -0.2443F));
        right_arm.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(24, 15).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7F, -5.8F, 0.0F, 0.0F, 0.0F, 0.0698F));
        right_arm.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(32, 15).addBox(0.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(1.7F, -5.8F, 0.0F, 0.0F, 0.0F, 0.0698F));
        right_arm.addOrReplaceChild("cube_r44", CubeListBuilder.create().texOffs(36, 15).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.02F)), PartPose.offsetAndRotation(1.3F, -8.1F, 0.0F, 0.0F, 0.0F, 0.3491F));
        right_arm.addOrReplaceChild("cube_r45", CubeListBuilder.create().texOffs(44, 15).addBox(0.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(1.3F, -8.1F, 0.0F, 0.0F, 0.0F, 0.3491F));
        right_arm.addOrReplaceChild("cube_r46", CubeListBuilder.create().texOffs(48, 15).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, -10.0F, 0.0F, 0.0F, 0.0F, 0.5585F));
        right_arm.addOrReplaceChild("cube_r47", CubeListBuilder.create().texOffs(56, 15).addBox(0.35F, -1.7F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(0.4F, -10.0F, 0.0F, 0.0F, 0.0F, 0.5585F));
        right_arm.addOrReplaceChild("cube_r48", CubeListBuilder.create().texOffs(60, 15).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.7F, -10.7F, 0.0F, 0.0F, 0.0F, -0.4189F));
        right_arm.addOrReplaceChild("cube_r49", CubeListBuilder.create().texOffs(64, 15).addBox(-0.5F, -1.4F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(1.5F, -11.7F, 0.0F, 0.0F, 0.0F, -1.0821F));
        right_arm.addOrReplaceChild("cube_r50", CubeListBuilder.create().texOffs(68, 15).addBox(-0.5F, -0.6F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.1F, -11.1F, 0.0F, 0.0F, 0.0F, -1.9199F));
        right_arm.addOrReplaceChild("cube_r51", CubeListBuilder.create().texOffs(76, 15).addBox(-0.5F, -1.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(-0.7F, -1.5F, 0.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition right_wing_deco = right_arm.addOrReplaceChild("right_wing_deco", CubeListBuilder.create().texOffs(116, 15).addBox(-0.1F, -1.7F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.12F)), PartPose.offset(2.0F, -4.0F, 0.0F));
        right_wing_deco.addOrReplaceChild("cube_r52", CubeListBuilder.create().texOffs(112, 15).addBox(-0.5F, -1.8F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.2F, -1.1F, 0.0F, 0.0F, 0.0F, -0.3142F));
        right_wing_deco.addOrReplaceChild("cube_r53", CubeListBuilder.create().texOffs(120, 15).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.9F, -2.0F, 0.0F, 0.0F, 0.0F, -0.6283F));
        right_wing_deco.addOrReplaceChild("cube_r54", CubeListBuilder.create().texOffs(124, 15).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(3.65F, -4.6F, 0.0F, 0.0F, 0.0F, -0.6283F));
        right_wing_deco.addOrReplaceChild("cube_r55", CubeListBuilder.create().texOffs(0, 21).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.12F)), PartPose.offsetAndRotation(1.2F, -0.2F, 0.0F, 0.0F, 0.0F, -0.8727F));
        right_wing_deco.addOrReplaceChild("cube_r56", CubeListBuilder.create().texOffs(4, 21).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(3.95F, -2.8F, 0.0F, 0.0F, 0.0F, -0.8727F));
        right_wing_deco.addOrReplaceChild("cube_r57", CubeListBuilder.create().texOffs(8, 21).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(-0.14F)), PartPose.offsetAndRotation(0.9F, 1.4F, 0.0F, 0.0F, 0.0F, -1.117F));
        right_wing_deco.addOrReplaceChild("cube_r58", CubeListBuilder.create().texOffs(12, 21).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.16F)), PartPose.offsetAndRotation(0.2F, 2.7F, 0.0F, 0.0F, 0.0F, -1.3614F));
        PartDefinition top_bar = harp_body.addOrReplaceChild("top_bar", CubeListBuilder.create().texOffs(16, 21).addBox(-5.0F, -0.3F, -1.0F, 10.0F, 1.0F, 2.0F, new CubeDeformation(0.06F)).texOffs(40, 21).addBox(-4.5F, -1.1F, -0.5F, 9.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)).texOffs(60, 21).addBox(-4.5F, 0.3F, -0.5F, 9.0F, 1.0F, 1.0F, new CubeDeformation(-0.22F)).texOffs(80, 21).addBox(-5.5F, -0.3F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.1F)).texOffs(86, 21).addBox(4.5F, -0.3F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.1F)).texOffs(92, 21).addBox(-3.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)).texOffs(96, 21).addBox(-3.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(100, 21).addBox(-2.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)).texOffs(104, 21).addBox(-2.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(108, 21).addBox(-1.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)).texOffs(112, 21).addBox(-1.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(116, 21).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)).texOffs(120, 21).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(124, 21).addBox(0.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)).texOffs(0, 27).addBox(0.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(4, 27).addBox(1.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)).texOffs(8, 27).addBox(1.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(12, 27).addBox(2.5F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.26F)).texOffs(16, 27).addBox(2.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)).texOffs(20, 27).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(-0.05F)).texOffs(44, 27).addBox(-0.5F, -3.9F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offset(0.0F, -7.5F, 0.0F));
        top_bar.addOrReplaceChild("cube_r59", CubeListBuilder.create().texOffs(28, 27).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.7854F));
        top_bar.addOrReplaceChild("cube_r60", CubeListBuilder.create().texOffs(32, 27).addBox(-1.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(-1.0F, -1.2F, 0.0F, 0.0F, 0.0F, -0.384F));
        top_bar.addOrReplaceChild("cube_r61", CubeListBuilder.create().texOffs(38, 27).addBox(-1.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(1.0F, -1.2F, 0.0F, 0.0F, 0.0F, 0.384F));
        PartDefinition strings_group = harp_body.addOrReplaceChild("strings_group", CubeListBuilder.create(), PartPose.offset(0.0F, -2.5F, 0.0F));
        PartDefinition string_1 = strings_group.addOrReplaceChild("string_1", CubeListBuilder.create().texOffs(48, 27).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.38F)), PartPose.offset(-3.0F, 0.0F, 0.0F));
        PartDefinition string_2 = strings_group.addOrReplaceChild("string_2", CubeListBuilder.create().texOffs(52, 27).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.38F)), PartPose.offset(-2.0F, 0.0F, 0.0F));
        PartDefinition string_3 = strings_group.addOrReplaceChild("string_3", CubeListBuilder.create().texOffs(56, 27).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.38F)), PartPose.offset(-1.0F, 0.0F, 0.0F));
        PartDefinition string_4 = strings_group.addOrReplaceChild("string_4", CubeListBuilder.create().texOffs(60, 27).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.38F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition string_5 = strings_group.addOrReplaceChild("string_5", CubeListBuilder.create().texOffs(64, 27).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.38F)), PartPose.offset(1.0F, 0.0F, 0.0F));
        PartDefinition string_6 = strings_group.addOrReplaceChild("string_6", CubeListBuilder.create().texOffs(68, 27).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.38F)), PartPose.offset(2.0F, 0.0F, 0.0F));
        PartDefinition string_7 = strings_group.addOrReplaceChild("string_7", CubeListBuilder.create().texOffs(72, 27).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(-0.38F)), PartPose.offset(3.0F, 0.0F, 0.0F));
        PartDefinition anemo_fx = harp_body.addOrReplaceChild("anemo_fx", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition wind_ring = anemo_fx.addOrReplaceChild("wind_ring", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r62", CubeListBuilder.create().texOffs(76, 27).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(9.2F, 0.0F, 0.0F, -0.2094F, 0.0F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r63", CubeListBuilder.create().texOffs(84, 27).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(6.505F, -1.2F, 6.505F, -0.1482F, 0.7854F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r64", CubeListBuilder.create().texOffs(92, 27).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(0.0F, 0.0F, 9.2F, 0.0F, 1.5708F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r65", CubeListBuilder.create().texOffs(100, 27).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(-6.505F, 1.2F, 6.505F, 0.1482F, 2.3562F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r66", CubeListBuilder.create().texOffs(108, 27).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(-9.2F, 0.0F, 0.0F, 0.2094F, 3.1416F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r67", CubeListBuilder.create().texOffs(116, 27).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(-6.505F, -1.2F, -6.505F, 0.1482F, 3.927F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r68", CubeListBuilder.create().texOffs(0, 37).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(0.0F, 0.0F, -9.2F, 0.0F, 4.7124F, 0.0F));
        wind_ring.addOrReplaceChild("cube_r69", CubeListBuilder.create().texOffs(8, 37).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(6.505F, 1.2F, -6.505F, -0.1482F, 5.4978F, 0.0F));
        PartDefinition note_1 = anemo_fx.addOrReplaceChild("note_1", CubeListBuilder.create().texOffs(16, 37).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.16F)).texOffs(20, 37).addBox(0.1F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.34F)), PartPose.offset(-7.5F, -3.0F, -2.0F));
        note_1.addOrReplaceChild("cube_r70", CubeListBuilder.create().texOffs(24, 37).addBox(-0.3F, -0.6F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.7F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5585F));
        PartDefinition note_2 = anemo_fx.addOrReplaceChild("note_2", CubeListBuilder.create().texOffs(28, 37).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.16F)).texOffs(32, 37).addBox(0.1F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.34F)), PartPose.offset(7.5F, -4.5F, -1.5F));
        note_2.addOrReplaceChild("cube_r71", CubeListBuilder.create().texOffs(36, 37).addBox(-0.3F, -0.6F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.7F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5585F));
        PartDefinition note_3 = anemo_fx.addOrReplaceChild("note_3", CubeListBuilder.create().texOffs(40, 37).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.16F)).texOffs(44, 37).addBox(0.1F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.34F)), PartPose.offset(1.5F, -9.5F, -2.5F));
        note_3.addOrReplaceChild("cube_r72", CubeListBuilder.create().texOffs(48, 37).addBox(-0.3F, -0.6F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.7F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5585F));
        PartDefinition petal_1 = anemo_fx.addOrReplaceChild("petal_1", CubeListBuilder.create(), PartPose.offset(-6.0F, 5.0F, -3.0F));
        petal_1.addOrReplaceChild("cube_r73", CubeListBuilder.create().texOffs(52, 37).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.28F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3142F, -0.4363F, 0.7854F));
        petal_1.addOrReplaceChild("cube_r74", CubeListBuilder.create().texOffs(56, 37).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2618F, 0.3491F, 0.0F));
        PartDefinition petal_2 = anemo_fx.addOrReplaceChild("petal_2", CubeListBuilder.create(), PartPose.offset(6.5F, 2.0F, 2.5F));
        petal_2.addOrReplaceChild("cube_r75", CubeListBuilder.create().texOffs(60, 37).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.28F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3142F, -0.4363F, 0.7854F));
        petal_2.addOrReplaceChild("cube_r76", CubeListBuilder.create().texOffs(64, 37).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.32F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2618F, 0.3491F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    public void animatePlaying(float time) {
        // Subtle harp breath, plucked string shimmer, spinning wind ring, and floating notes/petals
        this.harpBody.zRot = Mth.sin(time * 0.10F) * 0.035F;
        for (int i = 0; i < 7; i++) {
            float wave = Mth.sin(time * 0.85F - i * 0.72F);
            this.strings[i].z = wave * 0.14F;
        }
        this.windRing.yRot = time * 0.055F;
        this.windRing.zRot = Mth.sin(time * 0.08F) * 0.08F;
        for (int i = 0; i < 3; i++) {
            float phase = i * 2.094F;
            this.notes[i].yRot = Mth.sin(time * 0.12F + phase) * 0.28F;
            this.notes[i].zRot = Mth.cos(time * 0.14F + phase) * 0.12F;
        }
        for (int i = 0; i < 2; i++) {
            float phase = i * 3.1416F;
            this.petals[i].yRot = time * 0.09F + phase;
            this.petals[i].xRot = Mth.sin(time * 0.16F + phase) * 0.22F;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        animatePlaying(ageInTicks);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
