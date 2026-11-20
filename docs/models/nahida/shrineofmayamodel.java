// Exported for Minecraft 1.18 - 1.21 (Mojang Mappings: Forge / NeoForge / Fabric / MCreator)
// Model: Nahida's GIANT Ultimate Shrine of Maya (Đền Thờ Maya - Illusory Heart)
// Domain scale: ~24 blocks diameter x ~20 blocks tall | Texture: 512x512 RGBA
// Uses RenderType::entityTranslucent so the glowing Dendro stained-glass dome renders translucent!
package com.example.mod.client.model;

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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ShrineOfMayaModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation("modid", "shrine_of_maya"), "main");

    private final ModelPart shrine_of_maya;
    private final ModelPart floating_ornaments;
    private final ModelPart akasha_ring_outer;
    private final ModelPart akasha_ring_inner;
    private final ModelPart hanging_pendants;

    public ShrineOfMayaModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.shrine_of_maya = root.getChild("shrine_of_maya");
        this.floating_ornaments = this.shrine_of_maya.getChild("floating_ornaments");
        this.akasha_ring_outer = this.floating_ornaments.getChild("akasha_ring_outer");
        this.akasha_ring_inner = this.floating_ornaments.getChild("akasha_ring_inner");
        this.hanging_pendants = this.floating_ornaments.getChild("hanging_pendants");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition shrine_of_maya = partdefinition.addOrReplaceChild("shrine_of_maya",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition arena_floor = shrine_of_maya.addOrReplaceChild("arena_floor",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition floor_outer_ring_0 = arena_floor.addOrReplaceChild("floor_outer_ring_0",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2618F, 0.0F));
        PartDefinition floor_outer_ring_1 = arena_floor.addOrReplaceChild("floor_outer_ring_1",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        PartDefinition floor_outer_ring_2 = arena_floor.addOrReplaceChild("floor_outer_ring_2",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.309F, 0.0F));
        PartDefinition floor_outer_ring_3 = arena_floor.addOrReplaceChild("floor_outer_ring_3",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.8326F, 0.0F));
        PartDefinition floor_outer_ring_4 = arena_floor.addOrReplaceChild("floor_outer_ring_4",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        PartDefinition floor_outer_ring_5 = arena_floor.addOrReplaceChild("floor_outer_ring_5",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.8798F, 0.0F));
        PartDefinition floor_outer_ring_6 = arena_floor.addOrReplaceChild("floor_outer_ring_6",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.4034F, 0.0F));
        PartDefinition floor_outer_ring_7 = arena_floor.addOrReplaceChild("floor_outer_ring_7",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.927F, 0.0F));
        PartDefinition floor_outer_ring_8 = arena_floor.addOrReplaceChild("floor_outer_ring_8",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.4506F, 0.0F));
        PartDefinition floor_outer_ring_9 = arena_floor.addOrReplaceChild("floor_outer_ring_9",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.9742F, 0.0F));
        PartDefinition floor_outer_ring_10 = arena_floor.addOrReplaceChild("floor_outer_ring_10",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.4978F, 0.0F));
        PartDefinition floor_outer_ring_11 = arena_floor.addOrReplaceChild("floor_outer_ring_11",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-51.0F, -2.0F, -193.0F, 102.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(225, 0).addBox(-35.0F, -1.2F, -131.0F, 70.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(378, 0).addBox(-18.0F, -1.2F, -66.0F, 36.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -6.0214F, 0.0F));
        PartDefinition floor_center_sigil = arena_floor.addOrReplaceChild("floor_center_sigil",
                CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-24.0F, -1.1F, -24.0F, 48.0F, 1.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition floor_center_sigil_rot = floor_center_sigil.addOrReplaceChild("floor_center_sigil_rot",
                CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-24.0F, -1.15F, -24.0F, 48.0F, 1.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        PartDefinition colonnade_pillars = shrine_of_maya.addOrReplaceChild("colonnade_pillars",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition pillar_0 = colonnade_pillars.addOrReplaceChild("pillar_0",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition pillar_1 = colonnade_pillars.addOrReplaceChild("pillar_1",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F));
        PartDefinition pillar_2 = colonnade_pillars.addOrReplaceChild("pillar_2",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.0472F, 0.0F));
        PartDefinition pillar_3 = colonnade_pillars.addOrReplaceChild("pillar_3",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        PartDefinition pillar_4 = colonnade_pillars.addOrReplaceChild("pillar_4",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));
        PartDefinition pillar_5 = colonnade_pillars.addOrReplaceChild("pillar_5",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));
        PartDefinition pillar_6 = colonnade_pillars.addOrReplaceChild("pillar_6",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        PartDefinition pillar_7 = colonnade_pillars.addOrReplaceChild("pillar_7",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.6652F, 0.0F));
        PartDefinition pillar_8 = colonnade_pillars.addOrReplaceChild("pillar_8",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.1888F, 0.0F));
        PartDefinition pillar_9 = colonnade_pillars.addOrReplaceChild("pillar_9",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        PartDefinition pillar_10 = colonnade_pillars.addOrReplaceChild("pillar_10",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.236F, 0.0F));
        PartDefinition pillar_11 = colonnade_pillars.addOrReplaceChild("pillar_11",
                CubeListBuilder.create()
                        .texOffs(193, 13).addBox(-9.0F, -8.0F, -191.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(266, 13).addBox(-7.0F, -14.0F, -189.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(323, 13).addBox(-5.0F, -61.0F, -187.0F, 10.0F, 47.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(364, 13).addBox(-6.5F, -66.0F, -188.5F, 13.0F, 5.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(417, 13).addBox(-5.0F, -110.0F, -187.0F, 10.0F, 44.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 71).addBox(-7.5F, -116.0F, -189.5F, 15.0F, 6.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(61, 71).addBox(-10.0F, -124.0F, -192.0F, 20.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.7596F, 0.0F));
        PartDefinition palace_archways = shrine_of_maya.addOrReplaceChild("palace_archways",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition arch_bay_0 = palace_archways.addOrReplaceChild("arch_bay_0",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(226, 100).addBox(-33.0F, -162.0F, -184.0F, 66.0F, 26.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 156).addBox(-36.0F, -168.0F, -185.0F, 72.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(165, 156).addBox(-12.0F, -162.0F, -184.0F, 24.0F, 24.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(-42.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(-44.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(-42.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(-40.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(34.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(32.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(34.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(36.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2618F, 0.0F));
        PartDefinition bay_0_arch_l = arch_bay_0.addOrReplaceChild("bay_0_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_0_arch_r = arch_bay_0.addOrReplaceChild("bay_0_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_1 = palace_archways.addOrReplaceChild("arch_bay_1",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        PartDefinition bay_1_arch_l = arch_bay_1.addOrReplaceChild("bay_1_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_1_arch_r = arch_bay_1.addOrReplaceChild("bay_1_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_2 = palace_archways.addOrReplaceChild("arch_bay_2",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.309F, 0.0F));
        PartDefinition bay_2_arch_l = arch_bay_2.addOrReplaceChild("bay_2_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_2_arch_r = arch_bay_2.addOrReplaceChild("bay_2_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_3 = palace_archways.addOrReplaceChild("arch_bay_3",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(226, 100).addBox(-33.0F, -162.0F, -184.0F, 66.0F, 26.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 156).addBox(-36.0F, -168.0F, -185.0F, 72.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(165, 156).addBox(-12.0F, -162.0F, -184.0F, 24.0F, 24.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(-42.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(-44.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(-42.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(-40.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(34.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(32.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(34.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(36.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.8326F, 0.0F));
        PartDefinition bay_3_arch_l = arch_bay_3.addOrReplaceChild("bay_3_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_3_arch_r = arch_bay_3.addOrReplaceChild("bay_3_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_4 = palace_archways.addOrReplaceChild("arch_bay_4",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        PartDefinition bay_4_arch_l = arch_bay_4.addOrReplaceChild("bay_4_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_4_arch_r = arch_bay_4.addOrReplaceChild("bay_4_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_5 = palace_archways.addOrReplaceChild("arch_bay_5",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.8798F, 0.0F));
        PartDefinition bay_5_arch_l = arch_bay_5.addOrReplaceChild("bay_5_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_5_arch_r = arch_bay_5.addOrReplaceChild("bay_5_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_6 = palace_archways.addOrReplaceChild("arch_bay_6",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(226, 100).addBox(-33.0F, -162.0F, -184.0F, 66.0F, 26.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 156).addBox(-36.0F, -168.0F, -185.0F, 72.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(165, 156).addBox(-12.0F, -162.0F, -184.0F, 24.0F, 24.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(-42.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(-44.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(-42.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(-40.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(34.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(32.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(34.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(36.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.4034F, 0.0F));
        PartDefinition bay_6_arch_l = arch_bay_6.addOrReplaceChild("bay_6_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_6_arch_r = arch_bay_6.addOrReplaceChild("bay_6_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_7 = palace_archways.addOrReplaceChild("arch_bay_7",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.927F, 0.0F));
        PartDefinition bay_7_arch_l = arch_bay_7.addOrReplaceChild("bay_7_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_7_arch_r = arch_bay_7.addOrReplaceChild("bay_7_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_8 = palace_archways.addOrReplaceChild("arch_bay_8",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.4506F, 0.0F));
        PartDefinition bay_8_arch_l = arch_bay_8.addOrReplaceChild("bay_8_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_8_arch_r = arch_bay_8.addOrReplaceChild("bay_8_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_9 = palace_archways.addOrReplaceChild("arch_bay_9",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(226, 100).addBox(-33.0F, -162.0F, -184.0F, 66.0F, 26.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 156).addBox(-36.0F, -168.0F, -185.0F, 72.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(165, 156).addBox(-12.0F, -162.0F, -184.0F, 24.0F, 24.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(-42.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(-44.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(-42.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(-40.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 156).addBox(34.0F, -164.0F, -184.0F, 8.0F, 42.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(255, 156).addBox(32.0F, -172.0F, -186.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(304, 156).addBox(34.0F, -182.0F, -184.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(337, 156).addBox(36.0F, -198.0F, -182.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.9742F, 0.0F));
        PartDefinition bay_9_arch_l = arch_bay_9.addOrReplaceChild("bay_9_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_9_arch_r = arch_bay_9.addOrReplaceChild("bay_9_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_10 = palace_archways.addOrReplaceChild("arch_bay_10",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.4978F, 0.0F));
        PartDefinition bay_10_arch_l = arch_bay_10.addOrReplaceChild("bay_10_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_10_arch_r = arch_bay_10.addOrReplaceChild("bay_10_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition arch_bay_11 = palace_archways.addOrReplaceChild("arch_bay_11",
                CubeListBuilder.create()
                        .texOffs(142, 71).addBox(-47.0F, -130.0F, -182.0F, 94.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 100).addBox(-45.0F, -138.0F, -179.0F, 90.0F, 8.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 207).addBox(-41.0F, -20.0F, -178.0F, 82.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(173, 207).addBox(-40.0F, -122.0F, -177.5F, 80.0F, 46.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -6.0214F, 0.0F));
        PartDefinition bay_11_arch_l = arch_bay_11.addOrReplaceChild("bay_11_arch_l",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(-26.0F, -84.0F, -176.0F, 0.0F, 0.0F, 0.6283F));
        PartDefinition bay_11_arch_r = arch_bay_11.addOrReplaceChild("bay_11_arch_r",
                CubeListBuilder.create()
                        .texOffs(193, 100).addBox(-3.5F, -46.0F, -4.5F, 7.0F, 46.0F, 9.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(26.0F, -84.0F, -176.0F, 0.0F, 0.0F, -0.6283F));
        PartDefinition lotus_crown = shrine_of_maya.addOrReplaceChild("lotus_crown",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, -136.0F, 0.0F));
        PartDefinition lotus_wrap_0 = lotus_crown.addOrReplaceChild("lotus_wrap_0",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition lotus_petal_0 = lotus_wrap_0.addOrReplaceChild("lotus_petal_0",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_1 = lotus_crown.addOrReplaceChild("lotus_wrap_1",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F));
        PartDefinition lotus_petal_1 = lotus_wrap_1.addOrReplaceChild("lotus_petal_1",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_2 = lotus_crown.addOrReplaceChild("lotus_wrap_2",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.0472F, 0.0F));
        PartDefinition lotus_petal_2 = lotus_wrap_2.addOrReplaceChild("lotus_petal_2",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_3 = lotus_crown.addOrReplaceChild("lotus_wrap_3",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        PartDefinition lotus_petal_3 = lotus_wrap_3.addOrReplaceChild("lotus_petal_3",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_4 = lotus_crown.addOrReplaceChild("lotus_wrap_4",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));
        PartDefinition lotus_petal_4 = lotus_wrap_4.addOrReplaceChild("lotus_petal_4",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_5 = lotus_crown.addOrReplaceChild("lotus_wrap_5",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));
        PartDefinition lotus_petal_5 = lotus_wrap_5.addOrReplaceChild("lotus_petal_5",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_6 = lotus_crown.addOrReplaceChild("lotus_wrap_6",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        PartDefinition lotus_petal_6 = lotus_wrap_6.addOrReplaceChild("lotus_petal_6",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_7 = lotus_crown.addOrReplaceChild("lotus_wrap_7",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.6652F, 0.0F));
        PartDefinition lotus_petal_7 = lotus_wrap_7.addOrReplaceChild("lotus_petal_7",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_8 = lotus_crown.addOrReplaceChild("lotus_wrap_8",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.1888F, 0.0F));
        PartDefinition lotus_petal_8 = lotus_wrap_8.addOrReplaceChild("lotus_petal_8",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_9 = lotus_crown.addOrReplaceChild("lotus_wrap_9",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        PartDefinition lotus_petal_9 = lotus_wrap_9.addOrReplaceChild("lotus_petal_9",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_10 = lotus_crown.addOrReplaceChild("lotus_wrap_10",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.236F, 0.0F));
        PartDefinition lotus_petal_10 = lotus_wrap_10.addOrReplaceChild("lotus_petal_10",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition lotus_wrap_11 = lotus_crown.addOrReplaceChild("lotus_wrap_11",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.7596F, 0.0F));
        PartDefinition lotus_petal_11 = lotus_wrap_11.addOrReplaceChild("lotus_petal_11",
                CubeListBuilder.create()
                        .texOffs(340, 207).addBox(-12.0F, -22.0F, -2.0F, 24.0F, 22.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(397, 207).addBox(-7.0F, -36.0F, -2.0F, 14.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -184.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition grand_maya_dome = shrine_of_maya.addOrReplaceChild("grand_maya_dome",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, -136.0F, 0.0F));
        PartDefinition dome_rib_0 = grand_maya_dome.addOrReplaceChild("dome_rib_0",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition dome_rib_0_t1 = dome_rib_0.addOrReplaceChild("dome_rib_0_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_0_t2 = dome_rib_0.addOrReplaceChild("dome_rib_0_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_0_t3 = dome_rib_0.addOrReplaceChild("dome_rib_0_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_0_t4 = dome_rib_0.addOrReplaceChild("dome_rib_0_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_1 = grand_maya_dome.addOrReplaceChild("dome_rib_1",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F));
        PartDefinition dome_rib_1_t1 = dome_rib_1.addOrReplaceChild("dome_rib_1_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_1_t2 = dome_rib_1.addOrReplaceChild("dome_rib_1_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_1_t3 = dome_rib_1.addOrReplaceChild("dome_rib_1_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_1_t4 = dome_rib_1.addOrReplaceChild("dome_rib_1_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_2 = grand_maya_dome.addOrReplaceChild("dome_rib_2",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.0472F, 0.0F));
        PartDefinition dome_rib_2_t1 = dome_rib_2.addOrReplaceChild("dome_rib_2_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_2_t2 = dome_rib_2.addOrReplaceChild("dome_rib_2_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_2_t3 = dome_rib_2.addOrReplaceChild("dome_rib_2_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_2_t4 = dome_rib_2.addOrReplaceChild("dome_rib_2_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_3 = grand_maya_dome.addOrReplaceChild("dome_rib_3",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        PartDefinition dome_rib_3_t1 = dome_rib_3.addOrReplaceChild("dome_rib_3_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_3_t2 = dome_rib_3.addOrReplaceChild("dome_rib_3_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_3_t3 = dome_rib_3.addOrReplaceChild("dome_rib_3_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_3_t4 = dome_rib_3.addOrReplaceChild("dome_rib_3_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_4 = grand_maya_dome.addOrReplaceChild("dome_rib_4",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));
        PartDefinition dome_rib_4_t1 = dome_rib_4.addOrReplaceChild("dome_rib_4_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_4_t2 = dome_rib_4.addOrReplaceChild("dome_rib_4_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_4_t3 = dome_rib_4.addOrReplaceChild("dome_rib_4_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_4_t4 = dome_rib_4.addOrReplaceChild("dome_rib_4_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_5 = grand_maya_dome.addOrReplaceChild("dome_rib_5",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));
        PartDefinition dome_rib_5_t1 = dome_rib_5.addOrReplaceChild("dome_rib_5_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_5_t2 = dome_rib_5.addOrReplaceChild("dome_rib_5_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_5_t3 = dome_rib_5.addOrReplaceChild("dome_rib_5_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_5_t4 = dome_rib_5.addOrReplaceChild("dome_rib_5_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_6 = grand_maya_dome.addOrReplaceChild("dome_rib_6",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        PartDefinition dome_rib_6_t1 = dome_rib_6.addOrReplaceChild("dome_rib_6_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_6_t2 = dome_rib_6.addOrReplaceChild("dome_rib_6_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_6_t3 = dome_rib_6.addOrReplaceChild("dome_rib_6_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_6_t4 = dome_rib_6.addOrReplaceChild("dome_rib_6_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_7 = grand_maya_dome.addOrReplaceChild("dome_rib_7",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.6652F, 0.0F));
        PartDefinition dome_rib_7_t1 = dome_rib_7.addOrReplaceChild("dome_rib_7_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_7_t2 = dome_rib_7.addOrReplaceChild("dome_rib_7_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_7_t3 = dome_rib_7.addOrReplaceChild("dome_rib_7_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_7_t4 = dome_rib_7.addOrReplaceChild("dome_rib_7_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_8 = grand_maya_dome.addOrReplaceChild("dome_rib_8",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.1888F, 0.0F));
        PartDefinition dome_rib_8_t1 = dome_rib_8.addOrReplaceChild("dome_rib_8_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_8_t2 = dome_rib_8.addOrReplaceChild("dome_rib_8_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_8_t3 = dome_rib_8.addOrReplaceChild("dome_rib_8_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_8_t4 = dome_rib_8.addOrReplaceChild("dome_rib_8_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_9 = grand_maya_dome.addOrReplaceChild("dome_rib_9",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        PartDefinition dome_rib_9_t1 = dome_rib_9.addOrReplaceChild("dome_rib_9_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_9_t2 = dome_rib_9.addOrReplaceChild("dome_rib_9_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_9_t3 = dome_rib_9.addOrReplaceChild("dome_rib_9_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_9_t4 = dome_rib_9.addOrReplaceChild("dome_rib_9_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_10 = grand_maya_dome.addOrReplaceChild("dome_rib_10",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.236F, 0.0F));
        PartDefinition dome_rib_10_t1 = dome_rib_10.addOrReplaceChild("dome_rib_10_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_10_t2 = dome_rib_10.addOrReplaceChild("dome_rib_10_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_10_t3 = dome_rib_10.addOrReplaceChild("dome_rib_10_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_10_t4 = dome_rib_10.addOrReplaceChild("dome_rib_10_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_rib_11 = grand_maya_dome.addOrReplaceChild("dome_rib_11",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.7596F, 0.0F));
        PartDefinition dome_rib_11_t1 = dome_rib_11.addOrReplaceChild("dome_rib_11_t1",
                CubeListBuilder.create()
                        .texOffs(434, 207).addBox(-4.0F, -43.0F, -4.0F, 8.0F, 43.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -174.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_rib_11_t2 = dome_rib_11.addOrReplaceChild("dome_rib_11_t2",
                CubeListBuilder.create()
                        .texOffs(467, 207).addBox(-4.0F, -47.0F, -4.0F, 8.0F, 47.0F, 8.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -162.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_rib_11_t3 = dome_rib_11.addOrReplaceChild("dome_rib_11_t3",
                CubeListBuilder.create()
                        .texOffs(0, 263).addBox(-3.5F, -53.0F, -3.5F, 7.0F, 53.0F, 7.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -135.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_rib_11_t4 = dome_rib_11.addOrReplaceChild("dome_rib_11_t4",
                CubeListBuilder.create()
                        .texOffs(29, 263).addBox(-3.0F, -72.0F, -3.0F, 6.0F, 72.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -92.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_0 = grand_maya_dome.addOrReplaceChild("dome_panel_0",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2618F, 0.0F));
        PartDefinition dome_panel_0_p1 = dome_panel_0.addOrReplaceChild("dome_panel_0_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_0_p2 = dome_panel_0.addOrReplaceChild("dome_panel_0_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_0_p3 = dome_panel_0.addOrReplaceChild("dome_panel_0_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_0_p4 = dome_panel_0.addOrReplaceChild("dome_panel_0_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_1 = grand_maya_dome.addOrReplaceChild("dome_panel_1",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        PartDefinition dome_panel_1_p1 = dome_panel_1.addOrReplaceChild("dome_panel_1_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_1_p2 = dome_panel_1.addOrReplaceChild("dome_panel_1_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_1_p3 = dome_panel_1.addOrReplaceChild("dome_panel_1_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_1_p4 = dome_panel_1.addOrReplaceChild("dome_panel_1_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_2 = grand_maya_dome.addOrReplaceChild("dome_panel_2",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.309F, 0.0F));
        PartDefinition dome_panel_2_p1 = dome_panel_2.addOrReplaceChild("dome_panel_2_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_2_p2 = dome_panel_2.addOrReplaceChild("dome_panel_2_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_2_p3 = dome_panel_2.addOrReplaceChild("dome_panel_2_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_2_p4 = dome_panel_2.addOrReplaceChild("dome_panel_2_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_3 = grand_maya_dome.addOrReplaceChild("dome_panel_3",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.8326F, 0.0F));
        PartDefinition dome_panel_3_p1 = dome_panel_3.addOrReplaceChild("dome_panel_3_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_3_p2 = dome_panel_3.addOrReplaceChild("dome_panel_3_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_3_p3 = dome_panel_3.addOrReplaceChild("dome_panel_3_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_3_p4 = dome_panel_3.addOrReplaceChild("dome_panel_3_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_4 = grand_maya_dome.addOrReplaceChild("dome_panel_4",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        PartDefinition dome_panel_4_p1 = dome_panel_4.addOrReplaceChild("dome_panel_4_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_4_p2 = dome_panel_4.addOrReplaceChild("dome_panel_4_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_4_p3 = dome_panel_4.addOrReplaceChild("dome_panel_4_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_4_p4 = dome_panel_4.addOrReplaceChild("dome_panel_4_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_5 = grand_maya_dome.addOrReplaceChild("dome_panel_5",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.8798F, 0.0F));
        PartDefinition dome_panel_5_p1 = dome_panel_5.addOrReplaceChild("dome_panel_5_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_5_p2 = dome_panel_5.addOrReplaceChild("dome_panel_5_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_5_p3 = dome_panel_5.addOrReplaceChild("dome_panel_5_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_5_p4 = dome_panel_5.addOrReplaceChild("dome_panel_5_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_6 = grand_maya_dome.addOrReplaceChild("dome_panel_6",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.4034F, 0.0F));
        PartDefinition dome_panel_6_p1 = dome_panel_6.addOrReplaceChild("dome_panel_6_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_6_p2 = dome_panel_6.addOrReplaceChild("dome_panel_6_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_6_p3 = dome_panel_6.addOrReplaceChild("dome_panel_6_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_6_p4 = dome_panel_6.addOrReplaceChild("dome_panel_6_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_7 = grand_maya_dome.addOrReplaceChild("dome_panel_7",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.927F, 0.0F));
        PartDefinition dome_panel_7_p1 = dome_panel_7.addOrReplaceChild("dome_panel_7_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_7_p2 = dome_panel_7.addOrReplaceChild("dome_panel_7_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_7_p3 = dome_panel_7.addOrReplaceChild("dome_panel_7_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_7_p4 = dome_panel_7.addOrReplaceChild("dome_panel_7_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_8 = grand_maya_dome.addOrReplaceChild("dome_panel_8",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.4506F, 0.0F));
        PartDefinition dome_panel_8_p1 = dome_panel_8.addOrReplaceChild("dome_panel_8_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_8_p2 = dome_panel_8.addOrReplaceChild("dome_panel_8_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_8_p3 = dome_panel_8.addOrReplaceChild("dome_panel_8_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_8_p4 = dome_panel_8.addOrReplaceChild("dome_panel_8_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_9 = grand_maya_dome.addOrReplaceChild("dome_panel_9",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.9742F, 0.0F));
        PartDefinition dome_panel_9_p1 = dome_panel_9.addOrReplaceChild("dome_panel_9_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_9_p2 = dome_panel_9.addOrReplaceChild("dome_panel_9_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_9_p3 = dome_panel_9.addOrReplaceChild("dome_panel_9_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_9_p4 = dome_panel_9.addOrReplaceChild("dome_panel_9_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_10 = grand_maya_dome.addOrReplaceChild("dome_panel_10",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.4978F, 0.0F));
        PartDefinition dome_panel_10_p1 = dome_panel_10.addOrReplaceChild("dome_panel_10_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_10_p2 = dome_panel_10.addOrReplaceChild("dome_panel_10_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_10_p3 = dome_panel_10.addOrReplaceChild("dome_panel_10_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_10_p4 = dome_panel_10.addOrReplaceChild("dome_panel_10_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_panel_11 = grand_maya_dome.addOrReplaceChild("dome_panel_11",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -6.0214F, 0.0F));
        PartDefinition dome_panel_11_p1 = dome_panel_11.addOrReplaceChild("dome_panel_11_p1",
                CubeListBuilder.create()
                        .texOffs(54, 263).addBox(-43.0F, -43.0F, -1.5F, 86.0F, 43.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, -167.0F, -0.2793F, 0.0F, 0.0F));
        PartDefinition dome_panel_11_p2 = dome_panel_11.addOrReplaceChild("dome_panel_11_p2",
                CubeListBuilder.create()
                        .texOffs(233, 263).addBox(-39.0F, -47.0F, -1.5F, 78.0F, 47.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -39.0F, -155.0F, -0.6283F, 0.0F, 0.0F));
        PartDefinition dome_panel_11_p3 = dome_panel_11.addOrReplaceChild("dome_panel_11_p3",
                CubeListBuilder.create()
                        .texOffs(0, 342).addBox(-31.0F, -53.0F, -1.5F, 62.0F, 53.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -75.0F, -129.0F, -0.9774F, 0.0F, 0.0F));
        PartDefinition dome_panel_11_p4 = dome_panel_11.addOrReplaceChild("dome_panel_11_p4",
                CubeListBuilder.create()
                        .texOffs(131, 342).addBox(-19.0F, -70.0F, -1.5F, 38.0F, 70.0F, 3.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, -103.0F, -88.0F, -1.2915F, 0.0F, 0.0F));
        PartDefinition dome_crown_spire = shrine_of_maya.addOrReplaceChild("dome_crown_spire",
                CubeListBuilder.create()
                        .texOffs(355, 342).addBox(-13.0F, -20.0F, -13.0F, 26.0F, 8.0F, 26.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 416).addBox(-9.0F, -40.0F, -9.0F, 18.0F, 20.0F, 18.0F, new CubeDeformation(0.0F))
                        .texOffs(73, 416).addBox(-6.0F, -46.0F, -6.0F, 12.0F, 6.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(122, 416).addBox(-3.0F, -66.0F, -3.0F, 6.0F, 20.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offset(0.0F, -256.0F, 0.0F));
        PartDefinition oculus_ring_0 = dome_crown_spire.addOrReplaceChild("oculus_ring_0",
                CubeListBuilder.create()
                        .texOffs(214, 342).addBox(-11.0F, -12.0F, -24.0F, 22.0F, 12.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition oculus_ring_1 = dome_crown_spire.addOrReplaceChild("oculus_ring_1",
                CubeListBuilder.create()
                        .texOffs(214, 342).addBox(-11.0F, -12.0F, -24.0F, 22.0F, 12.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F));
        PartDefinition oculus_ring_2 = dome_crown_spire.addOrReplaceChild("oculus_ring_2",
                CubeListBuilder.create()
                        .texOffs(214, 342).addBox(-11.0F, -12.0F, -24.0F, 22.0F, 12.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.0472F, 0.0F));
        PartDefinition oculus_ring_3 = dome_crown_spire.addOrReplaceChild("oculus_ring_3",
                CubeListBuilder.create()
                        .texOffs(214, 342).addBox(-11.0F, -12.0F, -24.0F, 22.0F, 12.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        PartDefinition oculus_ring_4 = dome_crown_spire.addOrReplaceChild("oculus_ring_4",
                CubeListBuilder.create()
                        .texOffs(214, 342).addBox(-11.0F, -12.0F, -24.0F, 22.0F, 12.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));
        PartDefinition oculus_ring_5 = dome_crown_spire.addOrReplaceChild("oculus_ring_5",
                CubeListBuilder.create()
                        .texOffs(214, 342).addBox(-11.0F, -12.0F, -24.0F, 22.0F, 12.0F, 48.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));
        PartDefinition floating_ornaments = shrine_of_maya.addOrReplaceChild("floating_ornaments",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, -204.0F, 0.0F));
        PartDefinition akasha_ring_outer = floating_ornaments.addOrReplaceChild("akasha_ring_outer",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1396F, 0.0F, 0.1047F));
        PartDefinition ro_seg_0 = akasha_ring_outer.addOrReplaceChild("ro_seg_0",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition ro_seg_1 = akasha_ring_outer.addOrReplaceChild("ro_seg_1",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F));
        PartDefinition ro_seg_2 = akasha_ring_outer.addOrReplaceChild("ro_seg_2",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.0472F, 0.0F));
        PartDefinition ro_seg_3 = akasha_ring_outer.addOrReplaceChild("ro_seg_3",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        PartDefinition ro_seg_4 = akasha_ring_outer.addOrReplaceChild("ro_seg_4",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));
        PartDefinition ro_seg_5 = akasha_ring_outer.addOrReplaceChild("ro_seg_5",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));
        PartDefinition ro_seg_6 = akasha_ring_outer.addOrReplaceChild("ro_seg_6",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        PartDefinition ro_seg_7 = akasha_ring_outer.addOrReplaceChild("ro_seg_7",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.6652F, 0.0F));
        PartDefinition ro_seg_8 = akasha_ring_outer.addOrReplaceChild("ro_seg_8",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.1888F, 0.0F));
        PartDefinition ro_seg_9 = akasha_ring_outer.addOrReplaceChild("ro_seg_9",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        PartDefinition ro_seg_10 = akasha_ring_outer.addOrReplaceChild("ro_seg_10",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.236F, 0.0F));
        PartDefinition ro_seg_11 = akasha_ring_outer.addOrReplaceChild("ro_seg_11",
                CubeListBuilder.create()
                        .texOffs(147, 416).addBox(-23.0F, -2.0F, -88.5F, 46.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.7596F, 0.0F));
        PartDefinition akasha_ring_inner = floating_ornaments.addOrReplaceChild("akasha_ring_inner",
                CubeListBuilder.create()
                , PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, 0.2094F, -0.2618F, -0.1396F));
        PartDefinition ri_seg_0 = akasha_ring_inner.addOrReplaceChild("ri_seg_0",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition ri_seg_1 = akasha_ring_inner.addOrReplaceChild("ri_seg_1",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F));
        PartDefinition ri_seg_2 = akasha_ring_inner.addOrReplaceChild("ri_seg_2",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.0472F, 0.0F));
        PartDefinition ri_seg_3 = akasha_ring_inner.addOrReplaceChild("ri_seg_3",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        PartDefinition ri_seg_4 = akasha_ring_inner.addOrReplaceChild("ri_seg_4",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));
        PartDefinition ri_seg_5 = akasha_ring_inner.addOrReplaceChild("ri_seg_5",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));
        PartDefinition ri_seg_6 = akasha_ring_inner.addOrReplaceChild("ri_seg_6",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        PartDefinition ri_seg_7 = akasha_ring_inner.addOrReplaceChild("ri_seg_7",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.6652F, 0.0F));
        PartDefinition ri_seg_8 = akasha_ring_inner.addOrReplaceChild("ri_seg_8",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.1888F, 0.0F));
        PartDefinition ri_seg_9 = akasha_ring_inner.addOrReplaceChild("ri_seg_9",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        PartDefinition ri_seg_10 = akasha_ring_inner.addOrReplaceChild("ri_seg_10",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.236F, 0.0F));
        PartDefinition ri_seg_11 = akasha_ring_inner.addOrReplaceChild("ri_seg_11",
                CubeListBuilder.create()
                        .texOffs(250, 416).addBox(-14.0F, -1.0F, -54.0F, 28.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.7596F, 0.0F));
        PartDefinition hanging_pendants = floating_ornaments.addOrReplaceChild("hanging_pendants",
                CubeListBuilder.create()
                , PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition pendant_0 = hanging_pendants.addOrReplaceChild("pendant_0",
                CubeListBuilder.create()
                        .texOffs(315, 416).addBox(-1.0F, -2.0F, -113.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(324, 416).addBox(-4.0F, 22.0F, -116.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(357, 416).addBox(-3.0F, 26.0F, -115.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F));
        PartDefinition pendant_1 = hanging_pendants.addOrReplaceChild("pendant_1",
                CubeListBuilder.create()
                        .texOffs(315, 416).addBox(-1.0F, -2.0F, -113.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(324, 416).addBox(-4.0F, 22.0F, -116.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(357, 416).addBox(-3.0F, 26.0F, -115.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        PartDefinition pendant_2 = hanging_pendants.addOrReplaceChild("pendant_2",
                CubeListBuilder.create()
                        .texOffs(315, 416).addBox(-1.0F, -2.0F, -113.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(324, 416).addBox(-4.0F, 22.0F, -116.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(357, 416).addBox(-3.0F, 26.0F, -115.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.618F, 0.0F));
        PartDefinition pendant_3 = hanging_pendants.addOrReplaceChild("pendant_3",
                CubeListBuilder.create()
                        .texOffs(315, 416).addBox(-1.0F, -2.0F, -113.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(324, 416).addBox(-4.0F, 22.0F, -116.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(357, 416).addBox(-3.0F, 26.0F, -115.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.6652F, 0.0F));
        PartDefinition pendant_4 = hanging_pendants.addOrReplaceChild("pendant_4",
                CubeListBuilder.create()
                        .texOffs(315, 416).addBox(-1.0F, -2.0F, -113.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(324, 416).addBox(-4.0F, 22.0F, -116.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(357, 416).addBox(-3.0F, 26.0F, -115.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        PartDefinition pendant_5 = hanging_pendants.addOrReplaceChild("pendant_5",
                CubeListBuilder.create()
                        .texOffs(315, 416).addBox(-1.0F, -2.0F, -113.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(324, 416).addBox(-4.0F, 22.0F, -116.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(357, 416).addBox(-3.0F, 26.0F, -115.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.7596F, 0.0F));

        return LayerDefinition.create(meshdefinition, 512, 512);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Gentle idle animation for overhead Akasha rings & crystal pendants
        this.floating_ornaments.y = -204.0F + (float) Math.sin(ageInTicks * 0.05F) * 2.5F;
        this.akasha_ring_outer.yRot = ageInTicks * 0.015F;
        this.akasha_ring_inner.yRot = -ageInTicks * 0.025F;
        this.hanging_pendants.yRot = ageInTicks * 0.008F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        shrine_of_maya.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}