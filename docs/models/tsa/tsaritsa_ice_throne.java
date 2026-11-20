// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class tsaritsa_ice_throne<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "tsaritsa_ice_throne"), "main");
	private final ModelPart root;
	private final ModelPart throne_base;
	private final ModelPart throne_seat;
	private final ModelPart armrest_left;
	private final ModelPart armrest_right;
	private final ModelPart backrest_center_spire;
	private final ModelPart crown_polestar;
	private final ModelPart pillar_left;
	private final ModelPart pillar_left_floating_tip;
	private final ModelPart pillar_right;
	private final ModelPart pillar_right_floating_tip;
	private final ModelPart floating_frost_table;
	private final ModelPart ice_chalice;

	public tsaritsa_ice_throne(ModelPart root) {
		this.root = root.getChild("root");
		this.throne_base = this.root.getChild("throne_base");
		this.throne_seat = this.root.getChild("throne_seat");
		this.armrest_left = this.throne_seat.getChild("armrest_left");
		this.armrest_right = this.throne_seat.getChild("armrest_right");
		this.backrest_center_spire = this.root.getChild("backrest_center_spire");
		this.crown_polestar = this.backrest_center_spire.getChild("crown_polestar");
		this.pillar_left = this.backrest_center_spire.getChild("pillar_left");
		this.pillar_left_floating_tip = this.pillar_left.getChild("pillar_left_floating_tip");
		this.pillar_right = this.backrest_center_spire.getChild("pillar_right");
		this.pillar_right_floating_tip = this.pillar_right.getChild("pillar_right_floating_tip");
		this.floating_frost_table = this.root.getChild("floating_frost_table");
		this.ice_chalice = this.floating_frost_table.getChild("ice_chalice");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition throne_base = root.addOrReplaceChild("throne_base", CubeListBuilder.create().texOffs(0, 0).addBox(-11.0F, -2.0F, -9.0F, 22.0F, 2.0F, 20.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(-9.0F, -4.0F, -7.0F, 18.0F, 2.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition base_skirt_front_r_r1 = throne_base.addOrReplaceChild("base_skirt_front_r_r1", CubeListBuilder.create().texOffs(32, 16).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -1.0F, -8.0F, -0.2618F, 0.3927F, 0.0F));

		PartDefinition base_skirt_front_l_r1 = throne_base.addOrReplaceChild("base_skirt_front_l_r1", CubeListBuilder.create().texOffs(32, 16).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -1.0F, -8.0F, -0.2618F, -0.3927F, 0.0F));

		PartDefinition base_skirt_right_r1 = throne_base.addOrReplaceChild("base_skirt_right_r1", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -1.5F, -5.0F, 6.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.0F, -2.0F, 1.0F, 0.0F, 0.2618F, -0.3491F));

		PartDefinition base_skirt_left_r1 = throne_base.addOrReplaceChild("base_skirt_left_r1", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -1.5F, -5.0F, 6.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.0F, -2.0F, 1.0F, 0.0F, -0.2618F, 0.3491F));

		PartDefinition throne_seat = root.addOrReplaceChild("throne_seat", CubeListBuilder.create().texOffs(64, 0).addBox(-7.0F, -2.0F, -5.0F, 14.0F, 6.0F, 13.0F, new CubeDeformation(0.0F))
		.texOffs(64, 20).addBox(-8.0F, -4.0F, -7.0F, 16.0F, 2.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, 0.0F));

		PartDefinition seat_front_crest_r1 = throne_seat.addOrReplaceChild("seat_front_crest_r1", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -3.0F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, -6.5F, 0.1745F, 0.0F, 0.0F));

		PartDefinition armrest_left = throne_seat.addOrReplaceChild("armrest_left", CubeListBuilder.create(), PartPose.offset(9.0F, -5.0F, 0.0F));

		PartDefinition armrest_l_top_r1 = armrest_left.addOrReplaceChild("armrest_l_top_r1", CubeListBuilder.create().texOffs(32, 48).addBox(-2.0F, -1.0F, -7.0F, 4.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.0873F, 0.0F, 0.0873F));

		PartDefinition armrest_l_support_r1 = armrest_left.addOrReplaceChild("armrest_l_support_r1", CubeListBuilder.create().texOffs(0, 48).addBox(-1.5F, -3.0F, -5.0F, 3.0F, 6.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));

		PartDefinition armrest_right = throne_seat.addOrReplaceChild("armrest_right", CubeListBuilder.create(), PartPose.offset(-9.0F, -5.0F, 0.0F));

		PartDefinition armrest_r_top_r1 = armrest_right.addOrReplaceChild("armrest_r_top_r1", CubeListBuilder.create().texOffs(32, 48).addBox(-2.0F, -1.0F, -7.0F, 4.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.0873F, 0.0F, -0.0873F));

		PartDefinition armrest_r_support_r1 = armrest_right.addOrReplaceChild("armrest_r_support_r1", CubeListBuilder.create().texOffs(0, 48).addBox(-1.5F, -3.0F, -5.0F, 3.0F, 6.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));

		PartDefinition backrest_center_spire = root.addOrReplaceChild("backrest_center_spire", CubeListBuilder.create().texOffs(0, 56).addBox(-7.0F, -4.0F, -1.25F, 14.0F, 16.0F, 2.5F, new CubeDeformation(0.0F))
		.texOffs(32, 56).addBox(-5.5F, -14.0F, -1.05F, 11.0F, 10.0F, 2.1F, new CubeDeformation(0.0F))
		.texOffs(64, 56).addBox(-3.5F, -21.0F, -0.85F, 7.0F, 7.0F, 1.7F, new CubeDeformation(0.0F))
		.texOffs(104, 72).addBox(-1.0F, -23.0F, -1.55F, 2.0F, 25.0F, 3.1F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, 7.25F));

		PartDefinition spire_lattice_diag_r_r1 = backrest_center_spire.addOrReplaceChild("spire_lattice_diag_r_r1", CubeListBuilder.create().texOffs(104, 72).addBox(-0.75F, -6.0F, -1.45F, 1.5F, 12.0F, 2.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.25F, -10.0F, 0.0F, 0.0F, 0.0F, 0.3142F));

		PartDefinition spire_lattice_diag_l_r1 = backrest_center_spire.addOrReplaceChild("spire_lattice_diag_l_r1", CubeListBuilder.create().texOffs(104, 72).addBox(-0.75F, -6.0F, -1.45F, 1.5F, 12.0F, 2.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.25F, -10.0F, 0.0F, 0.0F, 0.0F, -0.3142F));

		PartDefinition spire_frame_upper_r_r1 = backrest_center_spire.addOrReplaceChild("spire_frame_upper_r_r1", CubeListBuilder.create().texOffs(84, 72).addBox(-1.5F, -10.5F, -1.75F, 3.0F, 21.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -13.5F, 0.0F, 0.0F, 0.0F, 0.3927F));

		PartDefinition spire_frame_upper_l_r1 = backrest_center_spire.addOrReplaceChild("spire_frame_upper_l_r1", CubeListBuilder.create().texOffs(84, 72).addBox(-1.5F, -10.5F, -1.75F, 3.0F, 21.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -13.5F, 0.0F, 0.0F, 0.0F, -0.3927F));

		PartDefinition spire_frame_lower_r_r1 = backrest_center_spire.addOrReplaceChild("spire_frame_lower_r_r1", CubeListBuilder.create().texOffs(64, 72).addBox(-1.5F, -8.0F, -1.75F, 3.0F, 16.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, 4.0F, 0.0F, 0.0F, 0.0F, -0.1396F));

		PartDefinition spire_frame_lower_l_r1 = backrest_center_spire.addOrReplaceChild("spire_frame_lower_l_r1", CubeListBuilder.create().texOffs(64, 72).addBox(-1.5F, -8.0F, -1.75F, 3.0F, 16.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.1396F));

		PartDefinition backrest_apex_point_r1 = backrest_center_spire.addOrReplaceChild("backrest_apex_point_r1", CubeListBuilder.create().texOffs(96, 56).addBox(-2.5F, -2.5F, -0.75F, 5.0F, 5.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -21.5F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition crown_polestar = backrest_center_spire.addOrReplaceChild("crown_polestar", CubeListBuilder.create().texOffs(0, 96).addBox(-1.0F, -7.5F, -1.05F, 2.0F, 14.0F, 2.1F, new CubeDeformation(0.0F))
		.texOffs(16, 96).addBox(-6.5F, -1.0F, -1.05F, 13.0F, 2.0F, 2.1F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -28.5F, 0.0F));

		PartDefinition polestar_core_diamond_r1 = crown_polestar.addOrReplaceChild("polestar_core_diamond_r1", CubeListBuilder.create().texOffs(48, 96).addBox(-2.0F, -2.0F, -1.45F, 4.0F, 4.0F, 2.9F, new CubeDeformation(0.0F))
		.texOffs(16, 108).addBox(-4.5F, -0.8F, -0.95F, 9.0F, 1.6F, 1.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition polestar_diag_ray_2_r1 = crown_polestar.addOrReplaceChild("polestar_diag_ray_2_r1", CubeListBuilder.create().texOffs(16, 108).addBox(-4.5F, -0.8F, -0.95F, 9.0F, 1.6F, 1.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

		PartDefinition pillar_left = backrest_center_spire.addOrReplaceChild("pillar_left", CubeListBuilder.create(), PartPose.offset(12.0F, -2.0F, 0.25F));

		PartDefinition pillar_l_crown_facet_r1 = pillar_left.addOrReplaceChild("pillar_l_crown_facet_r1", CubeListBuilder.create().texOffs(48, 96).addBox(-2.0F, -2.0F, -1.1F, 4.0F, 4.0F, 2.2F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -9.0F, 0.0F, 0.0F, -0.1396F, 0.9948F));

		PartDefinition pillar_l_shaft_r1 = pillar_left.addOrReplaceChild("pillar_l_shaft_r1", CubeListBuilder.create().texOffs(0, 56).addBox(-2.5F, -9.0F, -1.0F, 5.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1396F, 0.2094F));

		PartDefinition pillar_left_floating_tip = pillar_left.addOrReplaceChild("pillar_left_floating_tip", CubeListBuilder.create(), PartPose.offset(3.0F, -16.0F, 0.0F));

		PartDefinition pillar_l_floating_spike_r1 = pillar_left_floating_tip.addOrReplaceChild("pillar_l_floating_spike_r1", CubeListBuilder.create().texOffs(0, 96).addBox(-1.2F, -2.5F, -1.0F, 2.4F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1396F, 0.2094F));

		PartDefinition pillar_right = backrest_center_spire.addOrReplaceChild("pillar_right", CubeListBuilder.create(), PartPose.offset(-12.0F, -2.0F, 0.25F));

		PartDefinition pillar_r_crown_facet_r1 = pillar_right.addOrReplaceChild("pillar_r_crown_facet_r1", CubeListBuilder.create().texOffs(48, 96).addBox(-2.0F, -2.0F, -1.1F, 4.0F, 4.0F, 2.2F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -9.0F, 0.0F, 0.0F, 0.1396F, -0.9948F));

		PartDefinition pillar_r_shaft_r1 = pillar_right.addOrReplaceChild("pillar_r_shaft_r1", CubeListBuilder.create().texOffs(0, 56).addBox(-2.5F, -9.0F, -1.0F, 5.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.1396F, -0.2094F));

		PartDefinition pillar_right_floating_tip = pillar_right.addOrReplaceChild("pillar_right_floating_tip", CubeListBuilder.create(), PartPose.offset(-3.0F, -16.0F, 0.0F));

		PartDefinition pillar_r_floating_spike_r1 = pillar_right_floating_tip.addOrReplaceChild("pillar_r_floating_spike_r1", CubeListBuilder.create().texOffs(0, 96).addBox(-1.2F, -2.5F, -1.0F, 2.4F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.1396F, -0.2094F));

		PartDefinition floating_frost_table = root.addOrReplaceChild("floating_frost_table", CubeListBuilder.create(), PartPose.offset(0.0F, -18.0F, -9.0F));

		PartDefinition ice_chalice = floating_frost_table.addOrReplaceChild("ice_chalice", CubeListBuilder.create(), PartPose.offset(-8.5F, -4.0F, -0.5F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}