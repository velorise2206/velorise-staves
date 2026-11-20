// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class raiden_robot_narukami<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "raiden_robot_narukami"), "main");
	private final ModelPart magatsu_mitake_narukami;
	private final ModelPart gauntlet_hand_right;
	private final ModelPart gauntlet_hand_left;
	private final ModelPart chakra_halo_ring;
	private final ModelPart mitsudomoe_drum_0;
	private final ModelPart mitsudomoe_drum_1;
	private final ModelPart mitsudomoe_drum_2;
	private final ModelPart mitsudomoe_drum_3;
	private final ModelPart mitsudomoe_drum_4;
	private final ModelPart right_arm_upper;
	private final ModelPart right_arm_lower;
	private final ModelPart odachi_katana;
	private final ModelPart left_arm_1_upper;
	private final ModelPart left_arm_1_lower;
	private final ModelPart left_arm_1_hand;
	private final ModelPart left_arm_2_upper;
	private final ModelPart left_arm_2_lower;
	private final ModelPart electro_vajra_dagger;
	private final ModelPart left_arm_3_upper;
	private final ModelPart left_arm_3_lower;

	public raiden_robot_narukami(ModelPart root) {
		this.magatsu_mitake_narukami = root.getChild("magatsu_mitake_narukami");
		this.gauntlet_hand_right = this.magatsu_mitake_narukami.getChild("gauntlet_hand_right");
		this.gauntlet_hand_left = this.magatsu_mitake_narukami.getChild("gauntlet_hand_left");
		this.chakra_halo_ring = this.magatsu_mitake_narukami.getChild("chakra_halo_ring");
		this.mitsudomoe_drum_0 = this.chakra_halo_ring.getChild("mitsudomoe_drum_0");
		this.mitsudomoe_drum_1 = this.chakra_halo_ring.getChild("mitsudomoe_drum_1");
		this.mitsudomoe_drum_2 = this.chakra_halo_ring.getChild("mitsudomoe_drum_2");
		this.mitsudomoe_drum_3 = this.chakra_halo_ring.getChild("mitsudomoe_drum_3");
		this.mitsudomoe_drum_4 = this.chakra_halo_ring.getChild("mitsudomoe_drum_4");
		this.right_arm_upper = this.magatsu_mitake_narukami.getChild("right_arm_upper");
		this.right_arm_lower = this.right_arm_upper.getChild("right_arm_lower");
		this.odachi_katana = this.right_arm_lower.getChild("odachi_katana");
		this.left_arm_1_upper = this.magatsu_mitake_narukami.getChild("left_arm_1_upper");
		this.left_arm_1_lower = this.left_arm_1_upper.getChild("left_arm_1_lower");
		this.left_arm_1_hand = this.left_arm_1_lower.getChild("left_arm_1_hand");
		this.left_arm_2_upper = this.magatsu_mitake_narukami.getChild("left_arm_2_upper");
		this.left_arm_2_lower = this.left_arm_2_upper.getChild("left_arm_2_lower");
		this.electro_vajra_dagger = this.left_arm_2_lower.getChild("electro_vajra_dagger");
		this.left_arm_3_upper = this.magatsu_mitake_narukami.getChild("left_arm_3_upper");
		this.left_arm_3_lower = this.left_arm_3_upper.getChild("left_arm_3_lower");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition magatsu_mitake_narukami = partdefinition.addOrReplaceChild("magatsu_mitake_narukami", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition gauntlet_hand_right = magatsu_mitake_narukami.addOrReplaceChild("gauntlet_hand_right", CubeListBuilder.create().texOffs(2, 47).addBox(-5.5F, -1.5F, -4.7F, 11.0F, 3.0F, 10.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.8F, -8.2F, -0.5F, -0.3142F, 0.6981F, -0.2443F));

		PartDefinition thumb_seg2_r1 = gauntlet_hand_right.addOrReplaceChild("thumb_seg2_r1", CubeListBuilder.create().texOffs(2, 137).addBox(-1.2F, -1.0F, -2.695F, 2.4F, 2.0F, 5.39F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.6522F, -3.3586F, 5.787F, 0.3061F, 0.6325F, -0.202F));

		PartDefinition thumb_mid_joint_r1 = gauntlet_hand_right.addOrReplaceChild("thumb_mid_joint_r1", CubeListBuilder.create().texOffs(182, 198).addBox(-1.275F, -1.075F, -0.6875F, 2.55F, 2.15F, 1.375F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.3369F, -2.3387F, 3.7498F, 0.3061F, 0.6325F, -0.202F));

		PartDefinition thumb_seg1_r1 = gauntlet_hand_right.addOrReplaceChild("thumb_seg1_r1", CubeListBuilder.create().texOffs(46, 115).addBox(-1.55F, -1.3F, -2.695F, 3.1F, 2.6F, 5.39F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.6105F, -1.4815F, 2.1491F, 0.2094F, 0.8727F, -0.1396F));

		PartDefinition thumb_base_joint_r1 = gauntlet_hand_right.addOrReplaceChild("thumb_base_joint_r1", CubeListBuilder.create().texOffs(170, 198).addBox(-1.45F, -1.2F, -0.9075F, 2.9F, 2.4F, 1.815F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8459F, -0.7784F, 0.6007F, 0.2094F, 0.8727F, -0.1396F));

		PartDefinition f3_p3_r1 = gauntlet_hand_right.addOrReplaceChild("f3_p3_r1", CubeListBuilder.create().texOffs(158, 198).addBox(-0.7955F, -0.81F, -1.485F, 1.591F, 1.62F, 2.97F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.4195F, -2.2733F, 14.8741F, 0.5236F, -0.1745F, 0.1222F));

		PartDefinition f3_k2_r1 = gauntlet_hand_right.addOrReplaceChild("f3_k2_r1", CubeListBuilder.create().texOffs(52, 224).addBox(-0.817F, -0.8F, -0.5225F, 1.634F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.2756F, -1.5336F, 13.5531F, 0.5236F, -0.1745F, 0.1222F));

		PartDefinition f3_p2_r1 = gauntlet_hand_right.addOrReplaceChild("f3_p2_r1", CubeListBuilder.create().texOffs(106, 174).addBox(-0.946F, -1.0F, -1.595F, 1.892F, 2.0F, 3.19F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0817F, -0.9811F, 12.08F, 0.2793F, -0.1745F, 0.1222F));

		PartDefinition f3_k1_r1 = gauntlet_hand_right.addOrReplaceChild("f3_k1_r1", CubeListBuilder.create().texOffs(42, 224).addBox(-0.946F, -0.95F, -0.6325F, 1.892F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.85F, -0.5442F, 10.4733F, 0.2793F, -0.1745F, 0.1222F));

		PartDefinition f3_p1_r1 = gauntlet_hand_right.addOrReplaceChild("f3_p1_r1", CubeListBuilder.create().texOffs(20, 147).addBox(-1.075F, -1.15F, -2.09F, 2.15F, 2.3F, 4.18F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.52F, -0.3195F, 8.4605F, 0.0349F, -0.1745F, 0.1222F));

		PartDefinition f3_k0_r1 = gauntlet_hand_right.addOrReplaceChild("f3_k0_r1", CubeListBuilder.create().texOffs(146, 198).addBox(-1.032F, -1.075F, -0.7425F, 2.064F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.1679F, -0.2646F, 6.4408F, 0.0349F, -0.1745F, 0.1222F));

		PartDefinition f2_p3_r1 = gauntlet_hand_right.addOrReplaceChild("f2_p3_r1", CubeListBuilder.create().texOffs(92, 174).addBox(-0.888F, -0.81F, -1.76F, 1.776F, 1.62F, 3.52F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7766F, -2.6294F, 16.7345F, 0.5236F, -0.0524F, 0.0524F));

		PartDefinition f2_k2_r1 = gauntlet_hand_right.addOrReplaceChild("f2_k2_r1", CubeListBuilder.create().texOffs(32, 224).addBox(-0.912F, -0.8F, -0.5225F, 1.824F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7392F, -1.7846F, 15.1787F, 0.5236F, -0.0524F, 0.0524F));

		PartDefinition f2_p2_r1 = gauntlet_hand_right.addOrReplaceChild("f2_p2_r1", CubeListBuilder.create().texOffs(226, 166).addBox(-1.056F, -1.0F, -1.98F, 2.112F, 2.0F, 3.96F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.68F, -1.1003F, 13.367F, 0.3142F, -0.0524F, 0.0524F));

		PartDefinition f2_k1_r1 = gauntlet_hand_right.addOrReplaceChild("f2_k1_r1", CubeListBuilder.create().texOffs(164, 218).addBox(-1.056F, -0.95F, -0.6325F, 2.112F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.608F, -0.5255F, 11.4205F, 0.3142F, -0.0524F, 0.0524F));

		PartDefinition f2_p1_r1 = gauntlet_hand_right.addOrReplaceChild("f2_p1_r1", CubeListBuilder.create().texOffs(170, 115).addBox(-1.2F, -1.15F, -2.585F, 2.4F, 2.3F, 5.17F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.493F, -0.2336F, 8.9388F, 0.0698F, -0.0524F, 0.0524F));

		PartDefinition f2_k0_r1 = gauntlet_hand_right.addOrReplaceChild("f2_k0_r1", CubeListBuilder.create().texOffs(134, 198).addBox(-1.152F, -1.075F, -0.7425F, 2.304F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.3685F, -0.1123F, 6.4441F, 0.0698F, -0.0524F, 0.0524F));

		PartDefinition f1_p3_r1 = gauntlet_hand_right.addOrReplaceChild("f1_p3_r1", CubeListBuilder.create().texOffs(78, 174).addBox(-0.9324F, -0.81F, -1.98F, 1.8648F, 1.62F, 3.96F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0683F, -2.9758F, 17.7265F, 0.4538F, 0.0698F, -0.0349F));

		PartDefinition f1_k2_r1 = gauntlet_hand_right.addOrReplaceChild("f1_k2_r1", CubeListBuilder.create().texOffs(22, 224).addBox(-0.9576F, -0.8F, -0.5225F, 1.9152F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.9719F, -2.1533F, 15.9383F, 0.4538F, 0.0698F, -0.0349F));

		PartDefinition f1_p2_r1 = gauntlet_hand_right.addOrReplaceChild("f1_p2_r1", CubeListBuilder.create().texOffs(62, 157).addBox(-1.1088F, -1.0F, -2.2F, 2.2176F, 2.0F, 4.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8571F, -1.4162F, 13.9298F, 0.3142F, 0.0698F, -0.0349F));

		PartDefinition f1_k1_r1 = gauntlet_hand_right.addOrReplaceChild("f1_k1_r1", CubeListBuilder.create().texOffs(152, 218).addBox(-1.1088F, -0.95F, -0.6325F, 2.2176F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.73F, -0.7793F, 11.7956F, 0.3142F, 0.0698F, -0.0349F));

		PartDefinition f1_p1_r1 = gauntlet_hand_right.addOrReplaceChild("f1_p1_r1", CubeListBuilder.create().texOffs(150, 115).addBox(-1.26F, -1.15F, -2.805F, 2.52F, 2.3F, 5.61F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5569F, -0.3867F, 9.1251F, 0.1047F, 0.0698F, -0.0349F));

		PartDefinition f1_k0_r1 = gauntlet_hand_right.addOrReplaceChild("f1_k0_r1", CubeListBuilder.create().texOffs(122, 198).addBox(-1.2096F, -1.075F, -0.7425F, 2.4192F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.377F, -0.1577F, 6.4402F, 0.1047F, 0.0698F, -0.0349F));

		PartDefinition f0_p3_r1 = gauntlet_hand_right.addOrReplaceChild("f0_p3_r1", CubeListBuilder.create().texOffs(64, 174).addBox(-0.8954F, -0.81F, -1.815F, 1.7908F, 1.62F, 3.63F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.3027F, -2.3339F, 16.6864F, -0.0698F, 0.2269F, -0.1396F));

		PartDefinition f0_k2_r1 = gauntlet_hand_right.addOrReplaceChild("f0_k2_r1", CubeListBuilder.create().texOffs(12, 224).addBox(-0.9196F, -0.8F, -0.5225F, 1.8392F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8745F, -2.4522F, 14.9207F, -0.0698F, 0.2269F, -0.1396F));

		PartDefinition f0_p2_r1 = gauntlet_hand_right.addOrReplaceChild("f0_p2_r1", CubeListBuilder.create().texOffs(210, 166).addBox(-1.0648F, -1.0F, -1.98F, 2.1296F, 2.0F, 3.96F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5159F, -1.9317F, 13.069F, 0.2443F, 0.2269F, -0.1396F));

		PartDefinition f0_k1_r1 = gauntlet_hand_right.addOrReplaceChild("f0_k1_r1", CubeListBuilder.create().texOffs(140, 218).addBox(-1.0648F, -0.95F, -0.6325F, 2.1296F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.1341F, -1.441F, 11.1356F, 0.2443F, 0.2269F, -0.1396F));

		PartDefinition f0_p1_r1 = gauntlet_hand_right.addOrReplaceChild("f0_p1_r1", CubeListBuilder.create().texOffs(130, 115).addBox(-1.21F, -1.15F, -2.53F, 2.42F, 2.3F, 5.06F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.6661F, -0.8681F, 8.7827F, 0.1745F, 0.2269F, -0.1396F));

		PartDefinition f0_k0_r1 = gauntlet_hand_right.addOrReplaceChild("f0_k0_r1", CubeListBuilder.create().texOffs(110, 198).addBox(-1.1616F, -1.075F, -0.7425F, 2.3232F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.1773F, -0.4294F, 6.4217F, 0.1745F, 0.2269F, -0.1396F));

		PartDefinition wrist_spike_tip_r1 = gauntlet_hand_right.addOrReplaceChild("wrist_spike_tip_r1", CubeListBuilder.create().texOffs(2, 224).addBox(-0.95F, -0.8F, -0.95F, 1.9F, 1.6F, 1.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.85F, -13.4F, -0.2094F, 0.0F, 0.0F));

		PartDefinition wrist_spike_base_r1 = gauntlet_hand_right.addOrReplaceChild("wrist_spike_base_r1", CubeListBuilder.create().texOffs(194, 166).addBox(-2.0F, -1.5F, -1.15F, 4.0F, 3.0F, 2.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.65F, -11.7F, -0.1745F, 0.0F, 0.0F));

		PartDefinition wrist_dome_2_r1 = gauntlet_hand_right.addOrReplaceChild("wrist_dome_2_r1", CubeListBuilder.create().texOffs(156, 126).addBox(-3.4F, -2.1F, -1.35F, 6.8F, 4.2F, 2.7F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.45F, -9.8F, -0.1396F, 0.0F, 0.0F));

		PartDefinition wrist_dome_oct_r1 = gauntlet_hand_right.addOrReplaceChild("wrist_dome_oct_r1", CubeListBuilder.create().texOffs(80, 78).addBox(-3.8F, -2.9F, -1.65F, 7.6F, 5.8F, 3.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.25F, -7.5F, -0.1047F, 0.0F, 0.7854F));

		PartDefinition wrist_dome_1_r1 = gauntlet_hand_right.addOrReplaceChild("wrist_dome_1_r1", CubeListBuilder.create().texOffs(20, 103).addBox(-4.8F, -2.5F, -1.75F, 9.6F, 5.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.25F, -7.3F, -0.1047F, 0.0F, 0.0F));

		PartDefinition wrist_purple_ring_r1 = gauntlet_hand_right.addOrReplaceChild("wrist_purple_ring_r1", CubeListBuilder.create().texOffs(122, 166).addBox(-4.8F, -1.7F, -0.8F, 9.6F, 3.4F, 1.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, -5.2F, -0.0698F, 0.0F, 0.0F));

		PartDefinition palm_core_glow_r1 = gauntlet_hand_right.addOrReplaceChild("palm_core_glow_r1", CubeListBuilder.create().texOffs(200, 212).addBox(-1.5F, -0.175F, -1.5F, 3.0F, 0.35F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(118, 147).addBox(-2.65F, -0.005F, -2.65F, 5.3F, 0.45F, 5.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.78F, 0.6F, 0.0F, -0.7854F, 0.0F));

		PartDefinition knuckle_bar_r1 = gauntlet_hand_right.addOrReplaceChild("knuckle_bar_r1", CubeListBuilder.create().texOffs(58, 166).addBox(-5.7F, -1.425F, -1.3F, 11.4F, 2.85F, 2.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.22F, 5.9F, 0.0698F, 0.0F, 0.0F));

		PartDefinition palm_inner_bevel_r1 = gauntlet_hand_right.addOrReplaceChild("palm_inner_bevel_r1", CubeListBuilder.create().texOffs(180, 47).addBox(-0.95F, -1.35F, -4.6F, 1.9F, 2.7F, 9.2F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, -0.45F, 0.2F, 0.0F, 0.0F, 0.3142F));

		PartDefinition palm_outer_bevel_r1 = gauntlet_hand_right.addOrReplaceChild("palm_outer_bevel_r1", CubeListBuilder.create().texOffs(48, 63).addBox(-1.1F, -1.35F, -4.5F, 2.2F, 2.7F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.6F, -0.55F, 0.2F, 0.0F, 0.0F, -0.3491F));

		PartDefinition gauntlet_hand_left = magatsu_mitake_narukami.addOrReplaceChild("gauntlet_hand_left", CubeListBuilder.create().texOffs(48, 47).addBox(-5.5F, -1.5F, -4.7F, 11.0F, 3.0F, 10.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.8F, -8.2F, -0.5F, -0.3142F, -0.6981F, 0.2443F));

		PartDefinition thumb_seg2_r2 = gauntlet_hand_left.addOrReplaceChild("thumb_seg2_r2", CubeListBuilder.create().texOffs(22, 137).addBox(-1.2F, -1.0F, -2.695F, 2.4F, 2.0F, 5.39F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.6522F, -3.3586F, 5.787F, 0.3061F, -0.6325F, 0.202F));

		PartDefinition thumb_mid_joint_r2 = gauntlet_hand_left.addOrReplaceChild("thumb_mid_joint_r2", CubeListBuilder.create().texOffs(14, 205).addBox(-1.275F, -1.075F, -0.6875F, 2.55F, 2.15F, 1.375F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.3369F, -2.3387F, 3.7498F, 0.3061F, -0.6325F, 0.202F));

		PartDefinition thumb_seg1_r2 = gauntlet_hand_left.addOrReplaceChild("thumb_seg1_r2", CubeListBuilder.create().texOffs(68, 115).addBox(-1.55F, -1.3F, -2.695F, 3.1F, 2.6F, 5.39F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.6105F, -1.4815F, 2.1491F, 0.2094F, -0.8727F, 0.1396F));

		PartDefinition thumb_base_joint_r2 = gauntlet_hand_left.addOrReplaceChild("thumb_base_joint_r2", CubeListBuilder.create().texOffs(2, 205).addBox(-1.45F, -1.2F, -0.9075F, 2.9F, 2.4F, 1.815F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.8459F, -0.7784F, 0.6007F, 0.2094F, -0.8727F, 0.1396F));

		PartDefinition f3_p3_r2 = gauntlet_hand_left.addOrReplaceChild("f3_p3_r2", CubeListBuilder.create().texOffs(242, 198).addBox(-0.7955F, -0.81F, -1.485F, 1.591F, 1.62F, 2.97F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4195F, -2.2733F, 14.8741F, 0.5236F, 0.1745F, -0.1222F));

		PartDefinition f3_k2_r2 = gauntlet_hand_left.addOrReplaceChild("f3_k2_r2", CubeListBuilder.create().texOffs(112, 224).addBox(-0.817F, -0.8F, -0.5225F, 1.634F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.2756F, -1.5336F, 13.5531F, 0.5236F, 0.1745F, -0.1222F));

		PartDefinition f3_p2_r2 = gauntlet_hand_left.addOrReplaceChild("f3_p2_r2", CubeListBuilder.create().texOffs(162, 174).addBox(-0.946F, -1.0F, -1.595F, 1.892F, 2.0F, 3.19F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0817F, -0.9811F, 12.08F, 0.2793F, 0.1745F, -0.1222F));

		PartDefinition f3_k1_r2 = gauntlet_hand_left.addOrReplaceChild("f3_k1_r2", CubeListBuilder.create().texOffs(102, 224).addBox(-0.946F, -0.95F, -0.6325F, 1.892F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.85F, -0.5442F, 10.4733F, 0.2793F, 0.1745F, -0.1222F));

		PartDefinition f3_p1_r2 = gauntlet_hand_left.addOrReplaceChild("f3_p1_r2", CubeListBuilder.create().texOffs(38, 147).addBox(-1.075F, -1.15F, -2.09F, 2.15F, 2.3F, 4.18F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.52F, -0.3195F, 8.4605F, 0.0349F, 0.1745F, -0.1222F));

		PartDefinition f3_k0_r2 = gauntlet_hand_left.addOrReplaceChild("f3_k0_r2", CubeListBuilder.create().texOffs(230, 198).addBox(-1.032F, -1.075F, -0.7425F, 2.064F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.1679F, -0.2646F, 6.4408F, 0.0349F, 0.1745F, -0.1222F));

		PartDefinition f2_p3_r2 = gauntlet_hand_left.addOrReplaceChild("f2_p3_r2", CubeListBuilder.create().texOffs(148, 174).addBox(-0.888F, -0.81F, -1.76F, 1.776F, 1.62F, 3.52F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7766F, -2.6294F, 16.7345F, 0.5236F, 0.0524F, -0.0524F));

		PartDefinition f2_k2_r2 = gauntlet_hand_left.addOrReplaceChild("f2_k2_r2", CubeListBuilder.create().texOffs(92, 224).addBox(-0.912F, -0.8F, -0.5225F, 1.824F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7392F, -1.7846F, 15.1787F, 0.5236F, 0.0524F, -0.0524F));

		PartDefinition f2_p2_r2 = gauntlet_hand_left.addOrReplaceChild("f2_p2_r2", CubeListBuilder.create().texOffs(34, 174).addBox(-1.056F, -1.0F, -1.98F, 2.112F, 2.0F, 3.96F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.68F, -1.1003F, 13.367F, 0.3142F, 0.0524F, -0.0524F));

		PartDefinition f2_k1_r2 = gauntlet_hand_left.addOrReplaceChild("f2_k1_r2", CubeListBuilder.create().texOffs(200, 218).addBox(-1.056F, -0.95F, -0.6325F, 2.112F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.608F, -0.5255F, 11.4205F, 0.3142F, 0.0524F, -0.0524F));

		PartDefinition f2_p1_r2 = gauntlet_hand_left.addOrReplaceChild("f2_p1_r2", CubeListBuilder.create().texOffs(230, 115).addBox(-1.2F, -1.15F, -2.585F, 2.4F, 2.3F, 5.17F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.493F, -0.2336F, 8.9388F, 0.0698F, 0.0524F, -0.0524F));

		PartDefinition f2_k0_r2 = gauntlet_hand_left.addOrReplaceChild("f2_k0_r2", CubeListBuilder.create().texOffs(218, 198).addBox(-1.152F, -1.075F, -0.7425F, 2.304F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.3685F, -0.1123F, 6.4441F, 0.0698F, 0.0524F, -0.0524F));

		PartDefinition f1_p3_r2 = gauntlet_hand_left.addOrReplaceChild("f1_p3_r2", CubeListBuilder.create().texOffs(134, 174).addBox(-0.9324F, -0.81F, -1.98F, 1.8648F, 1.62F, 3.96F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0683F, -2.9758F, 17.7265F, 0.4538F, -0.0698F, 0.0349F));

		PartDefinition f1_k2_r2 = gauntlet_hand_left.addOrReplaceChild("f1_k2_r2", CubeListBuilder.create().texOffs(82, 224).addBox(-0.9576F, -0.8F, -0.5225F, 1.9152F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9719F, -2.1533F, 15.9383F, 0.4538F, -0.0698F, 0.0349F));

		PartDefinition f1_p2_r2 = gauntlet_hand_left.addOrReplaceChild("f1_p2_r2", CubeListBuilder.create().texOffs(80, 157).addBox(-1.1088F, -1.0F, -2.2F, 2.2176F, 2.0F, 4.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8571F, -1.4162F, 13.9298F, 0.3142F, -0.0698F, 0.0349F));

		PartDefinition f1_k1_r2 = gauntlet_hand_left.addOrReplaceChild("f1_k1_r2", CubeListBuilder.create().texOffs(188, 218).addBox(-1.1088F, -0.95F, -0.6325F, 2.2176F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.73F, -0.7793F, 11.7956F, 0.3142F, -0.0698F, 0.0349F));

		PartDefinition f1_p1_r2 = gauntlet_hand_left.addOrReplaceChild("f1_p1_r2", CubeListBuilder.create().texOffs(210, 115).addBox(-1.26F, -1.15F, -2.805F, 2.52F, 2.3F, 5.61F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5569F, -0.3867F, 9.1251F, 0.1047F, -0.0698F, 0.0349F));

		PartDefinition f1_k0_r2 = gauntlet_hand_left.addOrReplaceChild("f1_k0_r2", CubeListBuilder.create().texOffs(206, 198).addBox(-1.2096F, -1.075F, -0.7425F, 2.4192F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.377F, -0.1577F, 6.4402F, 0.1047F, -0.0698F, 0.0349F));

		PartDefinition f0_p3_r2 = gauntlet_hand_left.addOrReplaceChild("f0_p3_r2", CubeListBuilder.create().texOffs(120, 174).addBox(-0.8954F, -0.81F, -1.815F, 1.7908F, 1.62F, 3.63F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.3027F, -2.3339F, 16.6864F, -0.0698F, -0.2269F, 0.1396F));

		PartDefinition f0_k2_r2 = gauntlet_hand_left.addOrReplaceChild("f0_k2_r2", CubeListBuilder.create().texOffs(72, 224).addBox(-0.9196F, -0.8F, -0.5225F, 1.8392F, 1.6F, 1.045F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.8745F, -2.4522F, 14.9207F, -0.0698F, -0.2269F, 0.1396F));

		PartDefinition f0_p2_r2 = gauntlet_hand_left.addOrReplaceChild("f0_p2_r2", CubeListBuilder.create().texOffs(18, 174).addBox(-1.0648F, -1.0F, -1.98F, 2.1296F, 2.0F, 3.96F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5159F, -1.9317F, 13.069F, 0.2443F, -0.2269F, 0.1396F));

		PartDefinition f0_k1_r2 = gauntlet_hand_left.addOrReplaceChild("f0_k1_r2", CubeListBuilder.create().texOffs(176, 218).addBox(-1.0648F, -0.95F, -0.6325F, 2.1296F, 1.9F, 1.265F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.1341F, -1.441F, 11.1356F, 0.2443F, -0.2269F, 0.1396F));

		PartDefinition f0_p1_r2 = gauntlet_hand_left.addOrReplaceChild("f0_p1_r2", CubeListBuilder.create().texOffs(190, 115).addBox(-1.21F, -1.15F, -2.53F, 2.42F, 2.3F, 5.06F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.6661F, -0.8681F, 8.7827F, 0.1745F, -0.2269F, 0.1396F));

		PartDefinition f0_k0_r2 = gauntlet_hand_left.addOrReplaceChild("f0_k0_r2", CubeListBuilder.create().texOffs(194, 198).addBox(-1.1616F, -1.075F, -0.7425F, 2.3232F, 2.15F, 1.485F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.1773F, -0.4294F, 6.4217F, 0.1745F, -0.2269F, 0.1396F));

		PartDefinition wrist_spike_tip_r2 = gauntlet_hand_left.addOrReplaceChild("wrist_spike_tip_r2", CubeListBuilder.create().texOffs(62, 224).addBox(-0.95F, -0.8F, -0.95F, 1.9F, 1.6F, 1.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.85F, -13.4F, -0.2094F, 0.0F, 0.0F));

		PartDefinition wrist_spike_base_r2 = gauntlet_hand_left.addOrReplaceChild("wrist_spike_base_r2", CubeListBuilder.create().texOffs(2, 174).addBox(-2.0F, -1.5F, -1.15F, 4.0F, 3.0F, 2.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.65F, -11.7F, -0.1745F, 0.0F, 0.0F));

		PartDefinition wrist_dome_2_r2 = gauntlet_hand_left.addOrReplaceChild("wrist_dome_2_r2", CubeListBuilder.create().texOffs(178, 126).addBox(-3.4F, -2.1F, -1.35F, 6.8F, 4.2F, 2.7F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.45F, -9.8F, -0.1396F, 0.0F, 0.0F));

		PartDefinition wrist_dome_oct_r2 = gauntlet_hand_left.addOrReplaceChild("wrist_dome_oct_r2", CubeListBuilder.create().texOffs(106, 78).addBox(-3.8F, -2.9F, -1.65F, 7.6F, 5.8F, 3.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.25F, -7.5F, -0.1047F, 0.0F, 0.7854F));

		PartDefinition wrist_dome_1_r2 = gauntlet_hand_left.addOrReplaceChild("wrist_dome_1_r2", CubeListBuilder.create().texOffs(50, 103).addBox(-4.8F, -2.5F, -1.75F, 9.6F, 5.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.25F, -7.3F, -0.1047F, 0.0F, 0.0F));

		PartDefinition wrist_purple_ring_r2 = gauntlet_hand_left.addOrReplaceChild("wrist_purple_ring_r2", CubeListBuilder.create().texOffs(148, 166).addBox(-4.8F, -1.7F, -0.8F, 9.6F, 3.4F, 1.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, -5.2F, -0.0698F, 0.0F, 0.0F));

		PartDefinition palm_core_glow_r2 = gauntlet_hand_left.addOrReplaceChild("palm_core_glow_r2", CubeListBuilder.create().texOffs(214, 212).addBox(-1.5F, -0.175F, -1.5F, 3.0F, 0.35F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(144, 147).addBox(-2.65F, -0.005F, -2.65F, 5.3F, 0.45F, 5.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.78F, 0.6F, 0.0F, -0.7854F, 0.0F));

		PartDefinition knuckle_bar_r2 = gauntlet_hand_left.addOrReplaceChild("knuckle_bar_r2", CubeListBuilder.create().texOffs(90, 166).addBox(-5.7F, -1.425F, -1.3F, 11.4F, 2.85F, 2.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.22F, 5.9F, 0.0698F, 0.0F, 0.0F));

		PartDefinition palm_inner_bevel_r2 = gauntlet_hand_left.addOrReplaceChild("palm_inner_bevel_r2", CubeListBuilder.create().texOffs(206, 47).addBox(-0.95F, -1.35F, -4.6F, 1.9F, 2.7F, 9.2F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, -0.45F, 0.2F, 0.0F, 0.0F, -0.3142F));

		PartDefinition palm_outer_bevel_r2 = gauntlet_hand_left.addOrReplaceChild("palm_outer_bevel_r2", CubeListBuilder.create().texOffs(74, 63).addBox(-1.1F, -1.35F, -4.5F, 2.2F, 2.7F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.6F, -0.55F, 0.2F, 0.0F, 0.0F, 0.3491F));

		PartDefinition chakra_halo_ring = magatsu_mitake_narukami.addOrReplaceChild("chakra_halo_ring", CubeListBuilder.create().texOffs(176, 174).addBox(-12.75F, -1.7967F, -0.825F, 1.9F, 3.5934F, 1.65F, new CubeDeformation(0.0F))
		.texOffs(232, 182).addBox(-12.275F, -1.7247F, -1.0F, 0.95F, 3.4494F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.6F, -52.6F, 11.8F, -0.3706F, 0.9212F, -0.1787F));

		PartDefinition ring_glow_27_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_27_r1", CubeListBuilder.create().texOffs(202, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(202, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.5041F, 2.6257F, 0.0F, 0.0F, 0.0F, 6.0588F));

		PartDefinition ring_glow_26_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_26_r1", CubeListBuilder.create().texOffs(194, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(192, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.6314F, 5.1198F, 0.0F, 0.0F, 0.0F, 5.8344F));

		PartDefinition ring_glow_25_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_25_r1", CubeListBuilder.create().texOffs(186, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(182, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.2256F, 7.3572F, 0.0F, 0.0F, 0.0F, 5.61F));

		PartDefinition ring_glow_24_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_24_r1", CubeListBuilder.create().texOffs(178, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(172, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.3572F, 9.2256F, 0.0F, 0.0F, 0.0F, 5.3856F));

		PartDefinition ring_glow_23_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_23_r1", CubeListBuilder.create().texOffs(170, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(162, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.1198F, 10.6314F, 0.0F, 0.0F, 0.0F, 5.1612F));

		PartDefinition ring_glow_22_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_22_r1", CubeListBuilder.create().texOffs(162, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(152, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6257F, 11.5041F, 0.0F, 0.0F, 0.0F, 4.9368F));

		PartDefinition ring_glow_21_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_21_r1", CubeListBuilder.create().texOffs(154, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4494F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(142, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5934F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.8F, 0.0F, 0.0F, 0.0F, 4.7124F));

		PartDefinition ring_glow_20_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_20_r1", CubeListBuilder.create().texOffs(146, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(132, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6257F, 11.5041F, 0.0F, 0.0F, 0.0F, 4.488F));

		PartDefinition ring_glow_19_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_19_r1", CubeListBuilder.create().texOffs(138, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(122, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.1198F, 10.6314F, 0.0F, 0.0F, 0.0F, 4.2636F));

		PartDefinition ring_glow_18_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_18_r1", CubeListBuilder.create().texOffs(130, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(112, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.3572F, 9.2256F, 0.0F, 0.0F, 0.0F, 4.0392F));

		PartDefinition ring_glow_17_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_17_r1", CubeListBuilder.create().texOffs(122, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(102, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.2256F, 7.3572F, 0.0F, 0.0F, 0.0F, 3.8148F));

		PartDefinition ring_glow_16_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_16_r1", CubeListBuilder.create().texOffs(114, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(92, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.6314F, 5.1198F, 0.0F, 0.0F, 0.0F, 3.5904F));

		PartDefinition ring_glow_15_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_15_r1", CubeListBuilder.create().texOffs(106, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(82, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.5041F, 2.6257F, 0.0F, 0.0F, 0.0F, 3.366F));

		PartDefinition ring_glow_14_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_14_r1", CubeListBuilder.create().texOffs(98, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4494F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(72, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5934F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.8F, 0.0F, 0.0F, 0.0F, 0.0F, 3.1416F));

		PartDefinition ring_glow_13_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_13_r1", CubeListBuilder.create().texOffs(90, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(62, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.5041F, -2.6257F, 0.0F, 0.0F, 0.0F, 2.9172F));

		PartDefinition ring_glow_12_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_12_r1", CubeListBuilder.create().texOffs(82, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(52, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.6314F, -5.1198F, 0.0F, 0.0F, 0.0F, 2.6928F));

		PartDefinition ring_glow_11_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_11_r1", CubeListBuilder.create().texOffs(74, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(42, 182).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.2256F, -7.3572F, 0.0F, 0.0F, 0.0F, 2.4684F));

		PartDefinition ring_glow_10_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_10_r1", CubeListBuilder.create().texOffs(66, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.3572F, -9.2256F, 0.0F, 0.0F, 0.0F, 2.244F));

		PartDefinition ring_glow_9_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_9_r1", CubeListBuilder.create().texOffs(58, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(22, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.1198F, -10.6314F, 0.0F, 0.0F, 0.0F, 2.0196F));

		PartDefinition ring_glow_8_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_8_r1", CubeListBuilder.create().texOffs(50, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(12, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6257F, -11.5041F, 0.0F, 0.0F, 0.0F, 1.7952F));

		PartDefinition ring_glow_7_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_7_r1", CubeListBuilder.create().texOffs(42, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4494F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(2, 182).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5934F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.8F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition ring_glow_6_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_6_r1", CubeListBuilder.create().texOffs(34, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(236, 174).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6257F, -11.5041F, 0.0F, 0.0F, 0.0F, 1.3464F));

		PartDefinition ring_glow_5_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_5_r1", CubeListBuilder.create().texOffs(26, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(226, 174).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.1198F, -10.6314F, 0.0F, 0.0F, 0.0F, 1.122F));

		PartDefinition ring_glow_4_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_4_r1", CubeListBuilder.create().texOffs(18, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(216, 174).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.3572F, -9.2256F, 0.0F, 0.0F, 0.0F, 0.8976F));

		PartDefinition ring_glow_3_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_3_r1", CubeListBuilder.create().texOffs(10, 190).addBox(-0.475F, -1.7247F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(206, 174).addBox(-0.95F, -1.7966F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.2256F, -7.3572F, 0.0F, 0.0F, 0.0F, 0.6732F));

		PartDefinition ring_glow_2_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_2_r1", CubeListBuilder.create().texOffs(2, 190).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(196, 174).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.6314F, -5.1198F, 0.0F, 0.0F, 0.0F, 0.4488F));

		PartDefinition ring_glow_1_r1 = chakra_halo_ring.addOrReplaceChild("ring_glow_1_r1", CubeListBuilder.create().texOffs(240, 182).addBox(-0.475F, -1.7248F, -1.0F, 0.95F, 3.4495F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(186, 174).addBox(-0.95F, -1.7967F, -0.825F, 1.9F, 3.5933F, 1.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.5041F, -2.6257F, 0.0F, 0.0F, 0.0F, 0.2244F));

		PartDefinition mitsudomoe_drum_0 = chakra_halo_ring.addOrReplaceChild("mitsudomoe_drum_0", CubeListBuilder.create().texOffs(178, 78).addBox(-3.1F, -3.1F, -1.025F, 6.2F, 6.2F, 2.05F, new CubeDeformation(0.0F))
		.texOffs(192, 147).addBox(-3.35F, -2.2F, -0.9F, 6.7F, 4.4F, 1.8F, new CubeDeformation(0.0F))
		.texOffs(2, 126).addBox(-2.2F, -3.35F, -0.9F, 4.4F, 6.7F, 1.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6464F, -11.2225F, 0.0F, 0.0F, 0.0F, -0.3142F));

		PartDefinition drum_0_inner_pt_r1 = mitsudomoe_drum_0.addOrReplaceChild("drum_0_inner_pt_r1", CubeListBuilder.create().texOffs(210, 190).addBox(-1.35F, -1.35F, -0.625F, 2.7F, 2.7F, 1.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.6F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_0_petal_tip_r1 = mitsudomoe_drum_0.addOrReplaceChild("drum_0_petal_tip_r1", CubeListBuilder.create().texOffs(26, 205).addBox(-1.0F, -1.3F, -0.55F, 2.0F, 2.6F, 1.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.3F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_0_petal_mid_r1 = mitsudomoe_drum_0.addOrReplaceChild("drum_0_petal_mid_r1", CubeListBuilder.create().texOffs(98, 157).addBox(-2.15F, -2.05F, -0.675F, 4.3F, 4.1F, 1.35F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_0_frame_base_r1 = mitsudomoe_drum_0.addOrReplaceChild("drum_0_frame_base_r1", CubeListBuilder.create().texOffs(156, 78).addBox(-3.55F, -3.55F, -0.75F, 7.1F, 7.1F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition mitsudomoe_drum_1 = chakra_halo_ring.addOrReplaceChild("mitsudomoe_drum_1", CubeListBuilder.create().texOffs(222, 78).addBox(-3.1F, -3.1F, -1.025F, 6.2F, 6.2F, 2.05F, new CubeDeformation(0.0F))
		.texOffs(212, 147).addBox(-3.35F, -2.2F, -0.9F, 6.7F, 4.4F, 1.8F, new CubeDeformation(0.0F))
		.texOffs(18, 126).addBox(-2.2F, -3.35F, -0.9F, 4.4F, 6.7F, 1.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.5464F, -6.9359F, 0.0F, 0.0F, 0.0F, 0.9425F));

		PartDefinition drum_1_inner_pt_r1 = mitsudomoe_drum_1.addOrReplaceChild("drum_1_inner_pt_r1", CubeListBuilder.create().texOffs(222, 190).addBox(-1.35F, -1.35F, -0.625F, 2.7F, 2.7F, 1.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.6F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_1_petal_tip_r1 = mitsudomoe_drum_1.addOrReplaceChild("drum_1_petal_tip_r1", CubeListBuilder.create().texOffs(36, 205).addBox(-1.0F, -1.3F, -0.55F, 2.0F, 2.6F, 1.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.3F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_1_petal_mid_r1 = mitsudomoe_drum_1.addOrReplaceChild("drum_1_petal_mid_r1", CubeListBuilder.create().texOffs(114, 157).addBox(-2.15F, -2.05F, -0.675F, 4.3F, 4.1F, 1.35F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_1_frame_base_r1 = mitsudomoe_drum_1.addOrReplaceChild("drum_1_frame_base_r1", CubeListBuilder.create().texOffs(200, 78).addBox(-3.55F, -3.55F, -0.75F, 7.1F, 7.1F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition mitsudomoe_drum_2 = chakra_halo_ring.addOrReplaceChild("mitsudomoe_drum_2", CubeListBuilder.create().texOffs(24, 91).addBox(-3.1F, -3.1F, -1.025F, 6.2F, 6.2F, 2.05F, new CubeDeformation(0.0F))
		.texOffs(232, 147).addBox(-3.35F, -2.2F, -0.9F, 6.7F, 4.4F, 1.8F, new CubeDeformation(0.0F))
		.texOffs(34, 126).addBox(-2.2F, -3.35F, -0.9F, 4.4F, 6.7F, 1.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.5464F, 6.9359F, 0.0F, 0.0F, 0.0F, 2.1991F));

		PartDefinition drum_2_inner_pt_r1 = mitsudomoe_drum_2.addOrReplaceChild("drum_2_inner_pt_r1", CubeListBuilder.create().texOffs(234, 190).addBox(-1.35F, -1.35F, -0.625F, 2.7F, 2.7F, 1.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.6F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_2_petal_tip_r1 = mitsudomoe_drum_2.addOrReplaceChild("drum_2_petal_tip_r1", CubeListBuilder.create().texOffs(46, 205).addBox(-1.0F, -1.3F, -0.55F, 2.0F, 2.6F, 1.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.3F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_2_petal_mid_r1 = mitsudomoe_drum_2.addOrReplaceChild("drum_2_petal_mid_r1", CubeListBuilder.create().texOffs(130, 157).addBox(-2.15F, -2.05F, -0.675F, 4.3F, 4.1F, 1.35F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_2_frame_base_r1 = mitsudomoe_drum_2.addOrReplaceChild("drum_2_frame_base_r1", CubeListBuilder.create().texOffs(2, 91).addBox(-3.55F, -3.55F, -0.75F, 7.1F, 7.1F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition mitsudomoe_drum_3 = chakra_halo_ring.addOrReplaceChild("mitsudomoe_drum_3", CubeListBuilder.create().texOffs(68, 91).addBox(-3.1F, -3.1F, -1.025F, 6.2F, 6.2F, 2.05F, new CubeDeformation(0.0F))
		.texOffs(2, 157).addBox(-3.35F, -2.2F, -0.9F, 6.7F, 4.4F, 1.8F, new CubeDeformation(0.0F))
		.texOffs(50, 126).addBox(-2.2F, -3.35F, -0.9F, 4.4F, 6.7F, 1.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6464F, 11.2225F, 0.0F, 0.0F, 0.0F, 3.4558F));

		PartDefinition drum_3_inner_pt_r1 = mitsudomoe_drum_3.addOrReplaceChild("drum_3_inner_pt_r1", CubeListBuilder.create().texOffs(2, 198).addBox(-1.35F, -1.35F, -0.625F, 2.7F, 2.7F, 1.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.6F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_3_petal_tip_r1 = mitsudomoe_drum_3.addOrReplaceChild("drum_3_petal_tip_r1", CubeListBuilder.create().texOffs(56, 205).addBox(-1.0F, -1.3F, -0.55F, 2.0F, 2.6F, 1.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.3F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_3_petal_mid_r1 = mitsudomoe_drum_3.addOrReplaceChild("drum_3_petal_mid_r1", CubeListBuilder.create().texOffs(146, 157).addBox(-2.15F, -2.05F, -0.675F, 4.3F, 4.1F, 1.35F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_3_frame_base_r1 = mitsudomoe_drum_3.addOrReplaceChild("drum_3_frame_base_r1", CubeListBuilder.create().texOffs(46, 91).addBox(-3.55F, -3.55F, -0.75F, 7.1F, 7.1F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition mitsudomoe_drum_4 = chakra_halo_ring.addOrReplaceChild("mitsudomoe_drum_4", CubeListBuilder.create().texOffs(112, 91).addBox(-3.1F, -3.1F, -1.025F, 6.2F, 6.2F, 2.05F, new CubeDeformation(0.0F))
		.texOffs(22, 157).addBox(-3.35F, -2.2F, -0.9F, 6.7F, 4.4F, 1.8F, new CubeDeformation(0.0F))
		.texOffs(66, 126).addBox(-2.2F, -3.35F, -0.9F, 4.4F, 6.7F, 1.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.8F, 0.0F, 0.0F, 0.0F, 0.0F, 4.7124F));

		PartDefinition drum_4_inner_pt_r1 = mitsudomoe_drum_4.addOrReplaceChild("drum_4_inner_pt_r1", CubeListBuilder.create().texOffs(14, 198).addBox(-1.35F, -1.35F, -0.625F, 2.7F, 2.7F, 1.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.6F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_4_petal_tip_r1 = mitsudomoe_drum_4.addOrReplaceChild("drum_4_petal_tip_r1", CubeListBuilder.create().texOffs(66, 205).addBox(-1.0F, -1.3F, -0.55F, 2.0F, 2.6F, 1.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.3F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_4_petal_mid_r1 = mitsudomoe_drum_4.addOrReplaceChild("drum_4_petal_mid_r1", CubeListBuilder.create().texOffs(162, 157).addBox(-2.15F, -2.05F, -0.675F, 4.3F, 4.1F, 1.35F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition drum_4_frame_base_r1 = mitsudomoe_drum_4.addOrReplaceChild("drum_4_frame_base_r1", CubeListBuilder.create().texOffs(90, 91).addBox(-3.55F, -3.55F, -0.75F, 7.1F, 7.1F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition right_arm_upper = magatsu_mitake_narukami.addOrReplaceChild("right_arm_upper", CubeListBuilder.create().texOffs(82, 2).addBox(-9.268F, -6.62F, -8.75F, 10.296F, 8.58F, 10.14F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.8F, -43.4F, 23.6F, 0.6862F, -0.2549F, -1.1098F));

		PartDefinition upper_arm_eye_r1 = right_arm_upper.addOrReplaceChild("upper_arm_eye_r1", CubeListBuilder.create().texOffs(2, 63).addBox(-1.386F, -2.442F, -3.51F, 2.772F, 4.884F, 7.02F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.176F, 6.1224F, 4.25F, 0.0F, -0.4887F, 0.0F));

		PartDefinition upper_arm_eye_ring_r1 = right_arm_upper.addOrReplaceChild("upper_arm_eye_ring_r1", CubeListBuilder.create().texOffs(74, 28).addBox(-1.584F, -3.0118F, -4.16F, 3.168F, 6.0236F, 8.32F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.384F, 6.1224F, 3.73F, 0.0F, -0.4887F, 0.0F));

		PartDefinition upper_arm_front_ridge_r1 = right_arm_upper.addOrReplaceChild("upper_arm_front_ridge_r1", CubeListBuilder.create().texOffs(180, 28).addBox(-3.168F, -5.4538F, -1.625F, 6.336F, 10.9076F, 3.25F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.348F, 7.9132F, 4.51F, -0.0698F, -0.2094F, 0.0F));

		PartDefinition upper_arm_core_bot_r1 = right_arm_upper.addOrReplaceChild("upper_arm_core_bot_r1", CubeListBuilder.create().texOffs(38, 28).addBox(-4.092F, -4.1026F, -3.9F, 8.184F, 8.2052F, 7.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.632F, 10.8436F, 1.0F, 0.0F, -0.1396F, -0.0349F));

		PartDefinition upper_arm_core_top_r1 = right_arm_upper.addOrReplaceChild("upper_arm_core_top_r1", CubeListBuilder.create().texOffs(128, 2).addBox(-5.412F, -4.8319F, -4.81F, 10.824F, 9.6638F, 9.62F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.896F, 5.4712F, 1.26F, 0.0F, -0.1396F, 0.0524F));

		PartDefinition pauldron_inner_wing_r1 = right_arm_upper.addOrReplaceChild("pauldron_inner_wing_r1", CubeListBuilder.create().texOffs(102, 28).addBox(-1.716F, -4.62F, -2.73F, 3.432F, 9.24F, 5.46F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.912F, -6.51F, 1.78F, 0.0F, 0.2793F, 0.384F));

		PartDefinition pauldron_trim_2_r1 = right_arm_upper.addOrReplaceChild("pauldron_trim_2_r1", CubeListBuilder.create().texOffs(204, 28).addBox(-1.32F, -5.94F, -1.3F, 2.64F, 11.88F, 2.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.78F, 0.09F, 4.64F, -0.0698F, -0.3142F, -0.0349F));

		PartDefinition pauldron_trim_1_r1 = right_arm_upper.addOrReplaceChild("pauldron_trim_1_r1", CubeListBuilder.create().texOffs(124, 28).addBox(-1.056F, -6.27F, -1.3F, 2.112F, 12.54F, 2.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.272F, -10.25F, 4.9F, 0.3017F, -0.1653F, -0.4132F));

		PartDefinition pauldron_crest_tip_r1 = right_arm_upper.addOrReplaceChild("pauldron_crest_tip_r1", CubeListBuilder.create().texOffs(95, 125).addBox(-1.076F, -4.47F, -2.105F, 2.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.592F, -14.73F, 2.56F, 0.0649F, -0.1619F, 0.962F));

		PartDefinition pauldron_crest_2_r1 = right_arm_upper.addOrReplaceChild("pauldron_crest_2_r1", CubeListBuilder.create().texOffs(26, 78).addBox(-1.98F, -0.59F, -1.95F, 6.0F, 4.0F, 3.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.104F, -13.19F, 2.56F, 0.0204F, -0.1731F, 1.1575F));

		PartDefinition pauldron_crest_1_r1 = right_arm_upper.addOrReplaceChild("pauldron_crest_1_r1", CubeListBuilder.create().texOffs(124, 47).addBox(-3.168F, -3.96F, -2.86F, 6.336F, 7.92F, 5.72F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.896F, -11.57F, 2.43F, 0.0873F, -0.1396F, 0.5236F));

		PartDefinition pauldron_crest_0_r1 = right_arm_upper.addOrReplaceChild("pauldron_crest_0_r1", CubeListBuilder.create().texOffs(2, 28).addBox(-4.488F, -4.29F, -3.77F, 8.976F, 8.58F, 7.54F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.424F, -6.73F, 2.3F, 0.0698F, -0.1396F, 0.384F));

		PartDefinition pauldron_shell_bevel_r1 = right_arm_upper.addOrReplaceChild("pauldron_shell_bevel_r1", CubeListBuilder.create().texOffs(48, 2).addBox(-3.96F, -6.6F, -3.77F, 7.92F, 13.2F, 7.54F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.404F, -1.67F, 3.34F, 0.1047F, -0.384F, -0.1047F));

		PartDefinition pauldron_shell_main_r1 = right_arm_upper.addOrReplaceChild("pauldron_shell_main_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-5.676F, -6.93F, -4.94F, 11.352F, 13.86F, 9.88F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.16F, -0.57F, 2.04F, 0.0698F, -0.1396F, -0.0698F));

		PartDefinition shoulder_orb_o3_r1 = right_arm_upper.addOrReplaceChild("shoulder_orb_o3_r1", CubeListBuilder.create().texOffs(172, 2).addBox(-3.96F, -3.3F, -5.59F, 7.92F, 6.6F, 11.18F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.48F, -8.93F, 0.22F, 0.0F, 0.0F, 0.7854F));

		PartDefinition shoulder_orb_o2_r1 = right_arm_upper.addOrReplaceChild("shoulder_orb_o2_r1", CubeListBuilder.create().texOffs(214, 2).addBox(-3.96F, -4.73F, -3.9F, 7.92F, 9.46F, 7.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.12F, -2.33F, -3.68F, -0.7854F, 0.0F, 0.0F));

		PartDefinition shoulder_orb_o1_r1 = right_arm_upper.addOrReplaceChild("shoulder_orb_o1_r1", CubeListBuilder.create().texOffs(138, 28).addBox(-5.676F, -3.3F, -3.9F, 11.352F, 6.6F, 7.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.12F, -2.33F, -3.68F, 0.0F, -0.7854F, 0.0F));

		PartDefinition right_arm_lower = right_arm_upper.addOrReplaceChild("right_arm_lower", CubeListBuilder.create().texOffs(94, 47).addBox(-3.025F, -3.025F, -3.025F, 6.05F, 6.05F, 6.05F, new CubeDeformation(0.0F))
		.texOffs(152, 47).addBox(-3.05F, 0.7155F, -2.85F, 6.1F, 7.513F, 5.7F, new CubeDeformation(0.0F))
		.texOffs(26, 63).addBox(-2.35F, 5.5522F, -2.25F, 4.7F, 7.1276F, 4.5F, new CubeDeformation(0.0F))
		.texOffs(214, 63).addBox(-0.75F, 2.064F, 1.3F, 3.5F, 9.632F, 1.8F, new CubeDeformation(0.0F))
		.texOffs(42, 157).addBox(-2.1F, 11.008F, -2.0F, 4.2F, 2.064F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 13.6F, 1.0F, 0.5236F, -0.0698F, 1.3265F));

		PartDefinition g_thumb_2_r1 = right_arm_lower.addOrReplaceChild("g_thumb_2_r1", CubeListBuilder.create().texOffs(166, 205).addBox(-0.6785F, -1.121F, -0.6785F, 1.357F, 2.242F, 1.357F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6849F, 16.8729F, 1.7667F, 0.668F, 0.1294F, 0.9F));

		PartDefinition g_thumb_1_r1 = right_arm_lower.addOrReplaceChild("g_thumb_1_r1", CubeListBuilder.create().texOffs(156, 205).addBox(-0.7965F, -1.475F, -0.7965F, 1.593F, 2.95F, 1.593F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0242F, 14.958F, 1.2606F, 0.2006F, 0.0921F, 0.6215F));

		PartDefinition g_finger_3_knuckle_r1 = right_arm_lower.addOrReplaceChild("g_finger_3_knuckle_r1", CubeListBuilder.create().texOffs(242, 218).addBox(-0.59F, -0.5015F, -0.5015F, 1.18F, 1.003F, 1.003F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1147F, 15.484F, 1.8382F, -0.1745F, 0.0F, 0.0698F));

		PartDefinition g_finger_3_b_r1 = right_arm_lower.addOrReplaceChild("g_finger_3_b_r1", CubeListBuilder.create().texOffs(146, 205).addBox(-0.5428F, -1.239F, -0.7375F, 1.0856F, 2.478F, 1.475F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2709F, 17.7176F, -0.4138F, 1.1868F, 0.0F, 0.0698F));

		PartDefinition g_finger_3_a_r1 = right_arm_lower.addOrReplaceChild("g_finger_3_a_r1", CubeListBuilder.create().texOffs(136, 205).addBox(-0.59F, -1.475F, -0.7965F, 1.18F, 2.95F, 1.593F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1907F, 16.5718F, 1.2266F, 0.3142F, 0.0F, 0.0698F));

		PartDefinition g_finger_2_knuckle_r1 = right_arm_lower.addOrReplaceChild("g_finger_2_knuckle_r1", CubeListBuilder.create().texOffs(232, 218).addBox(-0.59F, -0.5015F, -0.5015F, 1.18F, 1.003F, 1.003F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.761F, 15.5787F, 1.8382F, -0.1745F, 0.0F, 0.0698F));

		PartDefinition g_finger_2_b_r1 = right_arm_lower.addOrReplaceChild("g_finger_2_b_r1", CubeListBuilder.create().texOffs(126, 205).addBox(-0.5428F, -1.239F, -0.7375F, 1.0856F, 2.478F, 1.475F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.9172F, 17.8122F, -0.4138F, 1.1868F, 0.0F, 0.0698F));

		PartDefinition g_finger_2_a_r1 = right_arm_lower.addOrReplaceChild("g_finger_2_a_r1", CubeListBuilder.create().texOffs(116, 205).addBox(-0.59F, -1.475F, -0.7965F, 1.18F, 2.95F, 1.593F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.837F, 16.6664F, 1.2266F, 0.3142F, 0.0F, 0.0698F));

		PartDefinition g_finger_1_knuckle_r1 = right_arm_lower.addOrReplaceChild("g_finger_1_knuckle_r1", CubeListBuilder.create().texOffs(222, 218).addBox(-0.59F, -0.5015F, -0.5015F, 1.18F, 1.003F, 1.003F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5927F, 15.6733F, 1.8382F, -0.1745F, 0.0F, 0.0698F));

		PartDefinition g_finger_1_b_r1 = right_arm_lower.addOrReplaceChild("g_finger_1_b_r1", CubeListBuilder.create().texOffs(106, 205).addBox(-0.5428F, -1.239F, -0.7375F, 1.0856F, 2.478F, 1.475F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4365F, 17.9068F, -0.4138F, 1.1868F, 0.0F, 0.0698F));

		PartDefinition g_finger_1_a_r1 = right_arm_lower.addOrReplaceChild("g_finger_1_a_r1", CubeListBuilder.create().texOffs(96, 205).addBox(-0.59F, -1.475F, -0.7965F, 1.18F, 2.95F, 1.593F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5167F, 16.761F, 1.2266F, 0.3142F, 0.0F, 0.0698F));

		PartDefinition g_finger_0_knuckle_r1 = right_arm_lower.addOrReplaceChild("g_finger_0_knuckle_r1", CubeListBuilder.create().texOffs(212, 218).addBox(-0.59F, -0.5015F, -0.5015F, 1.18F, 1.0031F, 1.003F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.9464F, 15.768F, 1.8382F, -0.1745F, 0.0F, 0.0698F));

		PartDefinition g_finger_0_b_r1 = right_arm_lower.addOrReplaceChild("g_finger_0_b_r1", CubeListBuilder.create().texOffs(86, 205).addBox(-0.5428F, -1.239F, -0.7375F, 1.0856F, 2.478F, 1.475F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7902F, 18.0015F, -0.4138F, 1.1868F, 0.0F, 0.0698F));

		PartDefinition g_finger_0_a_r1 = right_arm_lower.addOrReplaceChild("g_finger_0_a_r1", CubeListBuilder.create().texOffs(76, 205).addBox(-0.59F, -1.475F, -0.7965F, 1.18F, 2.95F, 1.593F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8704F, 16.8557F, 1.2266F, 0.3142F, 0.0F, 0.0698F));

		PartDefinition g_hand_palm_r1 = right_arm_lower.addOrReplaceChild("g_hand_palm_r1", CubeListBuilder.create().texOffs(2, 78).addBox(-2.832F, -2.596F, -2.124F, 5.664F, 5.192F, 4.248F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0142F, 14.2025F, -0.1272F, -0.1745F, 0.0F, 0.0698F));

		PartDefinition forearm_eye_r1 = right_arm_lower.addOrReplaceChild("forearm_eye_r1", CubeListBuilder.create().texOffs(104, 147).addBox(-0.9F, -1.72F, -1.8F, 1.8F, 3.44F, 3.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.7F, 5.848F, 2.2F, 0.0F, -0.4887F, 0.0F));

		PartDefinition forearm_eye_ring_r1 = right_arm_lower.addOrReplaceChild("forearm_eye_ring_r1", CubeListBuilder.create().texOffs(2, 103).addBox(-1.05F, -2.15F, -2.2F, 2.1F, 4.3F, 4.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.3F, 5.848F, 1.9F, 0.0F, -0.4887F, 0.0F));

		PartDefinition elbow_chevron_r_r1 = right_arm_lower.addOrReplaceChild("elbow_chevron_r_r1", CubeListBuilder.create().texOffs(130, 212).addBox(-1.9F, -0.9F, -1.0F, 3.8F, 1.8F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, 2.8F, 2.4F, -0.1745F, 0.2094F, 0.384F));

		PartDefinition elbow_chevron_l_r1 = right_arm_lower.addOrReplaceChild("elbow_chevron_l_r1", CubeListBuilder.create().texOffs(82, 212).addBox(-2.4F, -0.9F, -1.0F, 4.8F, 1.8F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, 2.8F, 2.4F, -0.1745F, -0.2094F, -0.384F));

		PartDefinition elbow_glow_ring_r1 = right_arm_lower.addOrReplaceChild("elbow_glow_ring_r1", CubeListBuilder.create().texOffs(54, 78).addBox(-2.95F, -1.6F, -2.95F, 5.9F, 3.2F, 5.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

		PartDefinition odachi_katana = right_arm_lower.addOrReplaceChild("odachi_katana", CubeListBuilder.create().texOffs(2, 166).addBox(-2.9F, -1.17F, -1.0701F, 23.7F, 2.34F, 2.1401F, new CubeDeformation(0.0F))
		.texOffs(50, 174).addBox(20.05F, -1.35F, -1.15F, 2.5F, 2.7F, 2.3F, new CubeDeformation(0.0F))
		.texOffs(38, 198).addBox(21.75F, -0.85F, -1.05F, 1.5F, 1.3F, 2.1F, new CubeDeformation(0.0F))
		.texOffs(194, 157).addBox(-4.05F, -1.6F, -1.25F, 1.9F, 3.2F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1915F, 16.7391F, 0.0236F, -0.2867F, 0.0959F, -1.0357F));

		PartDefinition blade_tip_edge_2_r1 = odachi_katana.addOrReplaceChild("blade_tip_edge_2_r1", CubeListBuilder.create().texOffs(196, 235).addBox(-0.896F, -0.08F, -0.0864F, 1.792F, 0.16F, 0.1728F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-64.0469F, -10.7215F, 0.0F, 0.0F, 0.0F, 0.576F));

		PartDefinition blade_tip_body_2_r1 = odachi_katana.addOrReplaceChild("blade_tip_body_2_r1", CubeListBuilder.create().texOffs(188, 235).addBox(-0.896F, -0.13F, -0.12F, 1.792F, 0.26F, 0.24F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-64.0016F, -10.7913F, 0.0F, 0.0F, 0.0F, 0.576F));

		PartDefinition blade_tip_edge_1_r1 = odachi_katana.addOrReplaceChild("blade_tip_edge_1_r1", CubeListBuilder.create().texOffs(180, 235).addBox(-0.952F, -0.1176F, -0.1224F, 1.904F, 0.2352F, 0.2448F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-62.8019F, -9.8434F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition blade_tip_body_1_r1 = odachi_katana.addOrReplaceChild("blade_tip_body_1_r1", CubeListBuilder.create().texOffs(172, 235).addBox(-0.952F, -0.28F, -0.17F, 1.904F, 0.56F, 0.34F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-62.7123F, -9.9986F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition blade_tip_edge_0_r1 = odachi_katana.addOrReplaceChild("blade_tip_edge_0_r1", CubeListBuilder.create().texOffs(162, 235).addBox(-1.008F, -0.1932F, -0.1512F, 2.016F, 0.3864F, 0.3024F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-61.4362F, -8.968F, 0.0F, 0.0F, 0.0F, 0.4712F));

		PartDefinition blade_tip_body_0_r1 = odachi_katana.addOrReplaceChild("blade_tip_body_0_r1", CubeListBuilder.create().texOffs(152, 235).addBox(-1.008F, -0.46F, -0.21F, 2.016F, 0.92F, 0.42F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-61.3026F, -9.2303F, 0.0F, 0.0F, 0.0F, 0.4712F));

		PartDefinition blade_edge_15_r1 = odachi_katana.addOrReplaceChild("blade_edge_15_r1", CubeListBuilder.create().texOffs(206, 230).addBox(-1.9095F, -0.24F, -0.147F, 3.819F, 0.48F, 0.294F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-59.1682F, -7.7556F, 0.0F, 0.0F, 0.0F, 0.4189F));

		PartDefinition blade_body_15_r1 = odachi_katana.addOrReplaceChild("blade_body_15_r1", CubeListBuilder.create().texOffs(62, 230).addBox(-1.876F, -0.6F, -0.2262F, 3.752F, 1.2F, 0.4524F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-58.9925F, -8.1503F, 0.0F, 0.0F, 0.0F, 0.4189F));

		PartDefinition blade_edge_14_r1 = odachi_katana.addOrReplaceChild("blade_edge_14_r1", CubeListBuilder.create().texOffs(194, 230).addBox(-1.9095F, -0.3197F, -0.1541F, 3.819F, 0.6394F, 0.3082F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-56.1124F, -6.3224F, 0.0F, 0.0F, 0.0F, 0.3732F));

		PartDefinition blade_body_14_r1 = odachi_katana.addOrReplaceChild("blade_body_14_r1", CubeListBuilder.create().texOffs(50, 230).addBox(-1.876F, -0.7993F, -0.2371F, 3.752F, 1.5985F, 0.4742F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-55.9026F, -6.8582F, 0.0F, 0.0F, 0.0F, 0.3732F));

		PartDefinition blade_edge_13_r1 = odachi_katana.addOrReplaceChild("blade_edge_13_r1", CubeListBuilder.create().texOffs(182, 230).addBox(-1.9095F, -0.3544F, -0.1612F, 3.819F, 0.7088F, 0.3224F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-52.9649F, -5.1013F, 0.0F, 0.0F, 0.0F, 0.33F));

		PartDefinition blade_body_13_r1 = odachi_katana.addOrReplaceChild("blade_body_13_r1", CubeListBuilder.create().texOffs(38, 230).addBox(-1.876F, -0.886F, -0.248F, 3.752F, 1.772F, 0.496F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-52.7582F, -5.7048F, 0.0F, 0.0F, 0.0F, 0.33F));

		PartDefinition blade_edge_12_r1 = odachi_katana.addOrReplaceChild("blade_edge_12_r1", CubeListBuilder.create().texOffs(170, 230).addBox(-1.9095F, -0.3876F, -0.1683F, 3.819F, 0.7751F, 0.3366F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-49.767F, -4.016F, 0.0F, 0.0F, 0.0F, 0.2891F));

		PartDefinition blade_body_12_r1 = odachi_katana.addOrReplaceChild("blade_body_12_r1", CubeListBuilder.create().texOffs(26, 230).addBox(-1.876F, -0.9688F, -0.259F, 3.752F, 1.9377F, 0.518F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-49.5681F, -4.6846F, 0.0F, 0.0F, 0.0F, 0.2891F));

		PartDefinition blade_edge_11_r1 = odachi_katana.addOrReplaceChild("blade_edge_11_r1", CubeListBuilder.create().texOffs(158, 230).addBox(-1.9095F, -0.4191F, -0.1754F, 3.819F, 0.8381F, 0.3508F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-46.5272F, -3.0608F, 0.0F, 0.0F, 0.0F, 0.2507F));

		PartDefinition blade_body_11_r1 = odachi_katana.addOrReplaceChild("blade_body_11_r1", CubeListBuilder.create().texOffs(128, 218).addBox(-1.876F, -1.0476F, -0.2699F, 3.752F, 2.0953F, 0.5398F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-46.34F, -3.7915F, 0.0F, 0.0F, 0.0F, 0.2507F));

		PartDefinition blade_edge_10_r1 = odachi_katana.addOrReplaceChild("blade_edge_10_r1", CubeListBuilder.create().texOffs(146, 230).addBox(-1.9095F, -0.4489F, -0.1825F, 3.819F, 0.8977F, 0.365F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-43.2532F, -2.2293F, 0.0F, 0.0F, 0.0F, 0.2149F));

		PartDefinition blade_body_10_r1 = odachi_katana.addOrReplaceChild("blade_body_10_r1", CubeListBuilder.create().texOffs(116, 218).addBox(-1.876F, -1.1222F, -0.2808F, 3.752F, 2.2444F, 0.5616F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-43.0809F, -3.0187F, 0.0F, 0.0F, 0.0F, 0.2149F));

		PartDefinition blade_edge_9_r1 = odachi_katana.addOrReplaceChild("blade_edge_9_r1", CubeListBuilder.create().texOffs(134, 230).addBox(-1.9095F, -0.477F, -0.1896F, 3.819F, 0.9539F, 0.3792F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-39.9521F, -1.5145F, 0.0F, 0.0F, 0.0F, 0.1817F));

		PartDefinition blade_body_9_r1 = odachi_katana.addOrReplaceChild("blade_body_9_r1", CubeListBuilder.create().texOffs(104, 218).addBox(-1.876F, -1.1924F, -0.2917F, 3.752F, 2.3849F, 0.5834F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-39.797F, -2.3589F, 0.0F, 0.0F, 0.0F, 0.1817F));

		PartDefinition blade_edge_8_r1 = odachi_katana.addOrReplaceChild("blade_edge_8_r1", CubeListBuilder.create().texOffs(14, 230).addBox(-1.9095F, -0.5032F, -0.1967F, 3.819F, 1.0065F, 0.3934F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-36.63F, -0.9087F, 0.0F, 0.0F, 0.0F, 0.1511F));

		PartDefinition blade_body_8_r1 = odachi_katana.addOrReplaceChild("blade_body_8_r1", CubeListBuilder.create().texOffs(92, 218).addBox(-1.876F, -1.2582F, -0.3026F, 3.752F, 2.5163F, 0.6052F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-36.4936F, -1.8043F, 0.0F, 0.0F, 0.0F, 0.1511F));

		PartDefinition blade_edge_7_r1 = odachi_katana.addOrReplaceChild("blade_edge_7_r1", CubeListBuilder.create().texOffs(2, 230).addBox(-1.9095F, -0.5276F, -0.2038F, 3.819F, 1.0552F, 0.4076F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-33.2921F, -0.4037F, 0.0F, 0.0F, 0.0F, 0.1232F));

		PartDefinition blade_body_7_r1 = odachi_katana.addOrReplaceChild("blade_body_7_r1", CubeListBuilder.create().texOffs(80, 218).addBox(-1.876F, -1.3191F, -0.3136F, 3.752F, 2.6383F, 0.6272F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-33.1754F, -1.3463F, 0.0F, 0.0F, 0.0F, 0.1232F));

		PartDefinition blade_edge_6_r1 = odachi_katana.addOrReplaceChild("blade_edge_6_r1", CubeListBuilder.create().texOffs(234, 224).addBox(-1.9095F, -0.55F, -0.2109F, 3.819F, 1.1F, 0.4218F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-29.9433F, 0.0091F, 0.0F, 0.0F, 0.0F, 0.0982F));

		PartDefinition blade_body_6_r1 = odachi_katana.addOrReplaceChild("blade_body_6_r1", CubeListBuilder.create().texOffs(68, 218).addBox(-1.876F, -1.375F, -0.3245F, 3.752F, 2.7501F, 0.649F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-29.8462F, -0.9762F, 0.0F, 0.0F, 0.0F, 0.0982F));

		PartDefinition blade_edge_5_r1 = odachi_katana.addOrReplaceChild("blade_edge_5_r1", CubeListBuilder.create().texOffs(222, 224).addBox(-1.9095F, -0.5702F, -0.218F, 3.819F, 1.1405F, 0.436F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-26.5872F, 0.339F, 0.0F, 0.0F, 0.0F, 0.0762F));

		PartDefinition blade_body_5_r1 = odachi_katana.addOrReplaceChild("blade_body_5_r1", CubeListBuilder.create().texOffs(56, 218).addBox(-1.876F, -1.4257F, -0.3354F, 3.752F, 2.8513F, 0.6708F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-26.5091F, -0.6845F, 0.0F, 0.0F, 0.0F, 0.0762F));

		PartDefinition blade_edge_4_r1 = odachi_katana.addOrReplaceChild("blade_edge_4_r1", CubeListBuilder.create().texOffs(210, 224).addBox(-1.9095F, -0.5882F, -0.2251F, 3.819F, 1.1764F, 0.4502F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-23.2272F, 0.5957F, 0.0F, 0.0F, 0.0F, 0.0572F));

		PartDefinition blade_body_4_r1 = odachi_katana.addOrReplaceChild("blade_body_4_r1", CubeListBuilder.create().texOffs(44, 218).addBox(-1.876F, -1.4705F, -0.3463F, 3.752F, 2.941F, 0.6926F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-23.1667F, -0.4613F, 0.0F, 0.0F, 0.0F, 0.0572F));

		PartDefinition blade_edge_3_r1 = odachi_katana.addOrReplaceChild("blade_edge_3_r1", CubeListBuilder.create().texOffs(198, 224).addBox(-1.9095F, -0.6036F, -0.2322F, 3.819F, 1.2072F, 0.4644F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-19.8659F, 0.7894F, 0.0F, 0.0F, 0.0F, 0.0415F));

		PartDefinition blade_body_3_r1 = odachi_katana.addOrReplaceChild("blade_body_3_r1", CubeListBuilder.create().texOffs(98, 198).addBox(-1.876F, -1.5091F, -0.3572F, 3.752F, 3.0181F, 0.7144F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-19.8209F, -0.2962F, 0.0F, 0.0F, 0.0F, 0.0415F));

		PartDefinition blade_edge_2_r1 = odachi_katana.addOrReplaceChild("blade_edge_2_r1", CubeListBuilder.create().texOffs(186, 224).addBox(-1.9095F, -0.6162F, -0.2393F, 3.819F, 1.2324F, 0.4786F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.5055F, 0.931F, 0.0F, 0.0F, 0.0F, 0.0293F));

		PartDefinition blade_body_2_r1 = odachi_katana.addOrReplaceChild("blade_body_2_r1", CubeListBuilder.create().texOffs(86, 198).addBox(-1.876F, -1.5405F, -0.3682F, 3.752F, 3.081F, 0.7364F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.473F, -0.1777F, 0.0F, 0.0F, 0.0F, 0.0293F));

		PartDefinition blade_edge_1_r1 = odachi_katana.addOrReplaceChild("blade_edge_1_r1", CubeListBuilder.create().texOffs(174, 224).addBox(-1.9095F, -0.6254F, -0.2464F, 3.819F, 1.2509F, 0.4928F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.1477F, 1.0319F, 0.0F, 0.0F, 0.0F, 0.021F));

		PartDefinition blade_body_1_r1 = odachi_katana.addOrReplaceChild("blade_body_1_r1", CubeListBuilder.create().texOffs(74, 198).addBox(-1.876F, -1.5636F, -0.3791F, 3.752F, 3.1272F, 0.7582F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.1241F, -0.0936F, 0.0F, 0.0F, 0.0F, 0.021F));

		PartDefinition blade_edge_0_r1 = odachi_katana.addOrReplaceChild("blade_edge_0_r1", CubeListBuilder.create().texOffs(162, 224).addBox(-1.9095F, -0.63F, -0.2535F, 3.819F, 1.26F, 0.507F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.7945F, 1.1046F, 0.0F, 0.0F, 0.0F, 0.0175F));

		PartDefinition blade_body_0_r1 = odachi_katana.addOrReplaceChild("blade_body_0_r1", CubeListBuilder.create().texOffs(62, 198).addBox(-1.876F, -1.575F, -0.39F, 3.752F, 3.15F, 0.78F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.7747F, -0.0292F, 0.0F, 0.0F, 0.0F, 0.0175F));

		PartDefinition tsuba_electro_core_r1 = odachi_katana.addOrReplaceChild("tsuba_electro_core_r1", CubeListBuilder.create().texOffs(50, 198).addBox(-1.2F, -1.2F, -0.875F, 2.4F, 2.4F, 1.75F, new CubeDeformation(0.0F))
		.texOffs(178, 157).addBox(-2.3F, -2.4F, -0.725F, 4.6F, 4.8F, 1.45F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, -0.1F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition tsuba_guard_flare_bot_r1 = odachi_katana.addOrReplaceChild("tsuba_guard_flare_bot_r1", CubeListBuilder.create().texOffs(222, 182).addBox(-0.75F, -1.7F, -0.65F, 1.5F, 3.4F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.7F, 4.1F, 0.0F, 0.0F, 0.0F, 0.4887F));

		PartDefinition tsuba_guard_flare_top_r1 = odachi_katana.addOrReplaceChild("tsuba_guard_flare_top_r1", CubeListBuilder.create().texOffs(212, 182).addBox(-0.75F, -1.7F, -0.65F, 1.5F, 3.4F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.8F, -4.1F, 0.0F, 0.0F, 0.0F, -0.4887F));

		PartDefinition tsuba_guard_v_r1 = odachi_katana.addOrReplaceChild("tsuba_guard_v_r1", CubeListBuilder.create().texOffs(44, 78).addBox(-0.675F, -4.5F, -0.95F, 1.35F, 9.0F, 1.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2094F));

		PartDefinition pommel_glow_r1 = odachi_katana.addOrReplaceChild("pommel_glow_r1", CubeListBuilder.create().texOffs(26, 198).addBox(-1.3F, -1.15F, -0.85F, 2.6F, 2.3F, 1.7F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(22.8F, -0.2F, 0.0F, 0.0F, 0.0F, 0.2443F));

		PartDefinition left_arm_1_upper = magatsu_mitake_narukami.addOrReplaceChild("left_arm_1_upper", CubeListBuilder.create().texOffs(176, 205).addBox(-10.4438F, -1.8975F, 0.3795F, 8.2579F, 1.035F, 1.035F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.6F, -44.9F, -1.0F, -0.0177F, 0.1999F, 0.3298F));

		PartDefinition left_puppet_arm_1_upper_s3_r1 = left_arm_1_upper.addOrReplaceChild("left_puppet_arm_1_upper_s3_r1", CubeListBuilder.create().texOffs(90, 115).addBox(-2.0752F, -2.0192F, -1.9715F, 4.1503F, 4.0384F, 3.943F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.764F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_upper_s2_r1 = left_arm_1_upper.addOrReplaceChild("left_puppet_arm_1_upper_s2_r1", CubeListBuilder.create().texOffs(156, 91).addBox(-2.2236F, -2.0192F, -2.1124F, 4.4472F, 4.0384F, 4.2248F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.004F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_upper_s1_r1 = left_arm_1_upper.addOrReplaceChild("left_puppet_arm_1_upper_s1_r1", CubeListBuilder.create().texOffs(134, 91).addBox(-2.3719F, -2.0192F, -2.2533F, 4.7437F, 4.0384F, 4.5066F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.244F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_upper_s0_r1 = left_arm_1_upper.addOrReplaceChild("left_puppet_arm_1_upper_s0_r1", CubeListBuilder.create().texOffs(132, 78).addBox(-2.5203F, -2.0192F, -2.3942F, 5.0405F, 4.0384F, 4.7884F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.484F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_cap_trim_r1 = left_arm_1_upper.addOrReplaceChild("left_puppet_arm_1_cap_trim_r1", CubeListBuilder.create().texOffs(160, 63).addBox(-0.69F, -2.898F, -2.898F, 1.38F, 5.796F, 5.796F, new CubeDeformation(0.0F))
		.texOffs(100, 63).addBox(-0.207F, -2.622F, -2.622F, 2.622F, 5.244F, 5.244F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.966F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition left_puppet_arm_1_cap_pt_r1 = left_arm_1_upper.addOrReplaceChild("left_puppet_arm_1_cap_pt_r1", CubeListBuilder.create().texOffs(42, 137).addBox(-1.587F, -1.587F, -1.587F, 3.174F, 3.174F, 3.174F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.932F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition left_arm_1_lower = left_arm_1_upper.addOrReplaceChild("left_arm_1_lower", CubeListBuilder.create().texOffs(56, 147).addBox(-12.1992F, -1.8547F, -1.8547F, 2.3184F, 3.7094F, 3.7094F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.144F, 0.0F, 0.0F, 0.3311F, 0.5377F, 1.824F));

		PartDefinition left_puppet_arm_1_fore_s3_r1 = left_arm_1_lower.addOrReplaceChild("left_puppet_arm_1_fore_s3_r1", CubeListBuilder.create().texOffs(96, 137).addBox(-1.6077F, -1.8424F, -1.5272F, 3.2154F, 3.6848F, 3.0544F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.7807F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_fore_s2_r1 = left_arm_1_lower.addOrReplaceChild("left_puppet_arm_1_fore_s2_r1", CubeListBuilder.create().texOffs(78, 137).addBox(-1.7872F, -1.8424F, -1.6977F, 3.5743F, 3.6848F, 3.3954F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2622F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_fore_s1_r1 = left_arm_1_lower.addOrReplaceChild("left_puppet_arm_1_fore_s1_r1", CubeListBuilder.create().texOffs(60, 137).addBox(-1.9665F, -1.8424F, -1.8682F, 3.933F, 3.6848F, 3.7364F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.7438F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_fore_s0_r1 = left_arm_1_lower.addOrReplaceChild("left_puppet_arm_1_fore_s0_r1", CubeListBuilder.create().texOffs(128, 103).addBox(-2.1459F, -1.8424F, -2.0385F, 4.2918F, 3.6848F, 4.077F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2253F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_1_elbow_r1 = left_arm_1_lower.addOrReplaceChild("left_puppet_arm_1_elbow_r1", CubeListBuilder.create().texOffs(178, 91).addBox(-2.473F, -2.473F, -2.473F, 4.946F, 4.946F, 4.946F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition left_arm_1_hand = left_arm_1_lower.addOrReplaceChild("left_arm_1_hand", CubeListBuilder.create(), PartPose.offsetAndRotation(-11.04F, 0.0F, 0.0F, 2.6093F, -0.544F, -0.7209F));

		PartDefinition thumb_r1 = left_arm_1_hand.addOrReplaceChild("thumb_r1", CubeListBuilder.create().texOffs(52, 235).addBox(-1.2765F, -0.4692F, -0.4692F, 2.553F, 0.9384F, 0.9384F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.9469F, 2.6307F, 1.6247F, 0.0978F, 0.3681F, -0.9683F));

		PartDefinition f3_2_r1 = left_arm_1_hand.addOrReplaceChild("f3_2_r1", CubeListBuilder.create().texOffs(42, 235).addBox(-1.0695F, -0.414F, -0.345F, 2.139F, 0.828F, 0.69F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.614F, 4.6227F, -0.0257F, 0.1492F, -0.1476F, -1.355F));

		PartDefinition f3_1_r1 = left_arm_1_hand.addOrReplaceChild("f3_1_r1", CubeListBuilder.create().texOffs(32, 235).addBox(-1.2765F, -0.483F, -0.3864F, 2.553F, 0.966F, 0.7728F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5598F, 2.7436F, 0.376F, 0.0726F, -0.1966F, -0.9147F));

		PartDefinition f2_2_r1 = left_arm_1_hand.addOrReplaceChild("f2_2_r1", CubeListBuilder.create().texOffs(22, 235).addBox(-1.0695F, -0.414F, -0.345F, 2.139F, 0.828F, 0.69F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.4558F, 4.5239F, -0.9031F, 0.1492F, -0.1476F, -1.355F));

		PartDefinition f2_1_r1 = left_arm_1_hand.addOrReplaceChild("f2_1_r1", CubeListBuilder.create().texOffs(12, 235).addBox(-1.2765F, -0.483F, -0.3864F, 2.553F, 0.966F, 0.7728F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.4016F, 2.6448F, -0.5014F, 0.0726F, -0.1966F, -0.9147F));

		PartDefinition f1_2_r1 = left_arm_1_hand.addOrReplaceChild("f1_2_r1", CubeListBuilder.create().texOffs(2, 235).addBox(-1.0695F, -0.414F, -0.345F, 2.139F, 0.828F, 0.69F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.2952F, 4.4236F, -1.794F, 0.1492F, -0.1476F, -1.355F));

		PartDefinition f1_1_r1 = left_arm_1_hand.addOrReplaceChild("f1_1_r1", CubeListBuilder.create().texOffs(238, 230).addBox(-1.2765F, -0.483F, -0.3864F, 2.553F, 0.966F, 0.7728F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2411F, 2.5444F, -1.3923F, 0.0726F, -0.1966F, -0.9147F));

		PartDefinition f0_2_r1 = left_arm_1_hand.addOrReplaceChild("f0_2_r1", CubeListBuilder.create().texOffs(228, 230).addBox(-1.0695F, -0.414F, -0.345F, 2.139F, 0.828F, 0.69F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.1371F, 4.3247F, -2.6714F, 0.1492F, -0.1476F, -1.355F));

		PartDefinition f0_1_r1 = left_arm_1_hand.addOrReplaceChild("f0_1_r1", CubeListBuilder.create().texOffs(218, 230).addBox(-1.2765F, -0.483F, -0.3864F, 2.553F, 0.966F, 0.7728F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0829F, 2.4456F, -2.2697F, 0.0726F, -0.1966F, -0.9147F));

		PartDefinition palm_r1 = left_arm_1_hand.addOrReplaceChild("palm_r1", CubeListBuilder.create().texOffs(174, 166).addBox(-2.001F, -0.9315F, -1.863F, 4.002F, 1.863F, 3.726F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1782F, 0.9299F, -0.373F, 0.0F, -0.2094F, -0.5585F));

		PartDefinition left_arm_2_upper = magatsu_mitake_narukami.addOrReplaceChild("left_arm_2_upper", CubeListBuilder.create().texOffs(200, 205).addBox(-10.2065F, -1.8975F, 0.3795F, 8.0703F, 1.035F, 1.035F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.6F, -34.0F, -0.8F, 0.0F, 0.2443F, -0.4189F));

		PartDefinition left_puppet_arm_2_upper_s3_r1 = left_arm_2_upper.addOrReplaceChild("left_puppet_arm_2_upper_s3_r1", CubeListBuilder.create().texOffs(200, 126).addBox(-2.0752F, -1.9687F, -1.9715F, 4.1503F, 3.9374F, 3.943F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.5225F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_upper_s2_r1 = left_arm_2_upper.addOrReplaceChild("left_puppet_arm_2_upper_s2_r1", CubeListBuilder.create().texOffs(172, 103).addBox(-2.2235F, -1.9687F, -2.1124F, 4.447F, 3.9374F, 4.2248F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.8315F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_upper_s1_r1 = left_arm_2_upper.addOrReplaceChild("left_puppet_arm_2_upper_s1_r1", CubeListBuilder.create().texOffs(150, 103).addBox(-2.3718F, -1.9687F, -2.2533F, 4.7437F, 3.9374F, 4.5066F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.1405F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_upper_s0_r1 = left_arm_2_upper.addOrReplaceChild("left_puppet_arm_2_upper_s0_r1", CubeListBuilder.create().texOffs(80, 103).addBox(-2.5203F, -1.9687F, -2.3942F, 5.0405F, 3.9374F, 4.7884F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4495F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_cap_trim_r1 = left_arm_2_upper.addOrReplaceChild("left_puppet_arm_2_cap_trim_r1", CubeListBuilder.create().texOffs(178, 63).addBox(-0.69F, -2.898F, -2.898F, 1.38F, 5.796F, 5.796F, new CubeDeformation(0.0F))
		.texOffs(120, 63).addBox(-0.207F, -2.622F, -2.622F, 2.622F, 5.244F, 5.244F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.966F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition left_puppet_arm_2_cap_pt_r1 = left_arm_2_upper.addOrReplaceChild("left_puppet_arm_2_cap_pt_r1", CubeListBuilder.create().texOffs(114, 137).addBox(-1.587F, -1.587F, -1.587F, 3.174F, 3.174F, 3.174F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.932F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition left_arm_2_lower = left_arm_2_upper.addOrReplaceChild("left_arm_2_lower", CubeListBuilder.create().texOffs(72, 147).addBox(-11.6472F, -1.8547F, -1.8547F, 2.3184F, 3.7094F, 3.7094F, new CubeDeformation(0.0F))
		.texOffs(110, 115).addBox(-14.835F, -2.001F, -1.794F, 4.002F, 4.002F, 3.588F, new CubeDeformation(0.0F))
		.texOffs(144, 212).addBox(-15.5595F, 0.9384F, -0.3795F, 2.001F, 0.828F, 2.553F, new CubeDeformation(0.0F))
		.texOffs(158, 212).addBox(-15.5595F, 0.0414F, -0.3795F, 2.001F, 0.828F, 2.553F, new CubeDeformation(0.0F))
		.texOffs(172, 212).addBox(-15.5595F, -0.8694F, -0.3795F, 2.001F, 0.828F, 2.553F, new CubeDeformation(0.0F))
		.texOffs(186, 212).addBox(-15.5595F, -1.7664F, -0.3795F, 2.001F, 0.828F, 2.553F, new CubeDeformation(0.0F))
		.texOffs(74, 230).addBox(-15.0765F, -2.3598F, 0.7245F, 2.277F, 0.9936F, 1.449F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.868F, 0.0F, 0.0F, 0.0F, 0.8116F, 1.3614F));

		PartDefinition left_puppet_arm_2_fore_s3_r1 = left_arm_2_lower.addOrReplaceChild("left_puppet_arm_2_fore_s3_r1", CubeListBuilder.create().texOffs(168, 137).addBox(-1.6077F, -1.7417F, -1.5272F, 3.2153F, 3.4834F, 3.0544F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.2977F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_fore_s2_r1 = left_arm_2_lower.addOrReplaceChild("left_puppet_arm_2_fore_s2_r1", CubeListBuilder.create().texOffs(150, 137).addBox(-1.7872F, -1.7417F, -1.6977F, 3.5743F, 3.4834F, 3.3954F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9172F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_fore_s1_r1 = left_arm_2_lower.addOrReplaceChild("left_puppet_arm_2_fore_s1_r1", CubeListBuilder.create().texOffs(132, 137).addBox(-1.9664F, -1.7417F, -1.8682F, 3.933F, 3.4834F, 3.7364F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5368F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_fore_s0_r1 = left_arm_2_lower.addOrReplaceChild("left_puppet_arm_2_fore_s0_r1", CubeListBuilder.create().texOffs(194, 103).addBox(-2.146F, -1.7417F, -2.0385F, 4.2919F, 3.4834F, 4.077F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1562F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_2_elbow_r1 = left_arm_2_lower.addOrReplaceChild("left_puppet_arm_2_elbow_r1", CubeListBuilder.create().texOffs(200, 91).addBox(-2.473F, -2.473F, -2.473F, 4.946F, 4.946F, 4.946F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition electro_vajra_dagger = left_arm_2_lower.addOrReplaceChild("electro_vajra_dagger", CubeListBuilder.create().texOffs(82, 126).addBox(-1.173F, -2.622F, -1.173F, 2.346F, 5.244F, 2.346F, new CubeDeformation(0.0F))
		.texOffs(228, 212).addBox(-1.656F, -3.588F, -0.966F, 3.312F, 1.242F, 1.932F, new CubeDeformation(0.0F))
		.texOffs(2, 212).addBox(-3.588F, -5.2785F, -0.9315F, 7.176F, 1.863F, 1.863F, new CubeDeformation(0.0F))
		.texOffs(46, 212).addBox(-2.967F, -6.9345F, -0.828F, 5.934F, 1.863F, 1.656F, new CubeDeformation(0.0F))
		.texOffs(98, 212).addBox(-2.346F, -8.5905F, -0.7245F, 4.692F, 1.863F, 1.449F, new CubeDeformation(0.0F))
		.texOffs(2, 218).addBox(-1.725F, -10.2465F, -0.621F, 3.45F, 1.863F, 1.242F, new CubeDeformation(0.0F))
		.texOffs(86, 230).addBox(-1.104F, -11.799F, -0.4968F, 2.208F, 1.794F, 0.9936F, new CubeDeformation(0.0F))
		.texOffs(106, 230).addBox(-0.5865F, -13.2825F, -0.3795F, 1.173F, 1.725F, 0.759F, new CubeDeformation(0.0F))
		.texOffs(122, 230).addBox(-0.2484F, -14.4555F, -0.2622F, 0.4968F, 1.449F, 0.5244F, new CubeDeformation(0.0F))
		.texOffs(228, 63).addBox(-0.5865F, -12.006F, -1.1385F, 1.173F, 8.556F, 2.277F, new CubeDeformation(0.0F))
		.texOffs(16, 218).addBox(-1.656F, 2.346F, -0.966F, 3.312F, 1.242F, 1.932F, new CubeDeformation(0.0F))
		.texOffs(24, 212).addBox(-3.588F, 3.4155F, -0.9315F, 7.176F, 1.863F, 1.863F, new CubeDeformation(0.0F))
		.texOffs(64, 212).addBox(-2.967F, 5.0715F, -0.828F, 5.934F, 1.863F, 1.656F, new CubeDeformation(0.0F))
		.texOffs(114, 212).addBox(-2.346F, 6.7275F, -0.7245F, 4.692F, 1.863F, 1.449F, new CubeDeformation(0.0F))
		.texOffs(30, 218).addBox(-1.725F, 8.3835F, -0.621F, 3.45F, 1.863F, 1.242F, new CubeDeformation(0.0F))
		.texOffs(96, 230).addBox(-1.104F, 10.005F, -0.4968F, 2.208F, 1.794F, 0.9936F, new CubeDeformation(0.0F))
		.texOffs(114, 230).addBox(-0.5865F, 11.5575F, -0.3795F, 1.173F, 1.725F, 0.759F, new CubeDeformation(0.0F))
		.texOffs(128, 230).addBox(-0.2484F, 13.0065F, -0.2622F, 0.4968F, 1.449F, 0.5244F, new CubeDeformation(0.0F))
		.texOffs(240, 63).addBox(-0.5865F, 3.45F, -1.1385F, 1.173F, 8.556F, 2.277F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.8F, 0.0F, 2.76F, 0.9442F, -0.1779F, -1.3028F));

		PartDefinition vajra_b1_wingR_-1_r1 = electro_vajra_dagger.addOrReplaceChild("vajra_b1_wingR_-1_r1", CubeListBuilder.create().texOffs(152, 224).addBox(-0.897F, -0.828F, -0.69F, 1.794F, 1.656F, 1.38F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.898F, 3.795F, 0.0F, 0.0F, 0.0F, 0.4189F));

		PartDefinition vajra_b1_wingL_-1_r1 = electro_vajra_dagger.addOrReplaceChild("vajra_b1_wingL_-1_r1", CubeListBuilder.create().texOffs(142, 224).addBox(-0.897F, -0.828F, -0.69F, 1.794F, 1.656F, 1.38F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.898F, 3.795F, 0.0F, 0.0F, 0.0F, -0.4189F));

		PartDefinition vajra_b1_wingR_1_r1 = electro_vajra_dagger.addOrReplaceChild("vajra_b1_wingR_1_r1", CubeListBuilder.create().texOffs(132, 224).addBox(-0.897F, -0.828F, -0.69F, 1.794F, 1.656F, 1.38F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.898F, -3.795F, 0.0F, 0.0F, 0.0F, -0.4189F));

		PartDefinition vajra_b1_wingL_1_r1 = electro_vajra_dagger.addOrReplaceChild("vajra_b1_wingL_1_r1", CubeListBuilder.create().texOffs(122, 224).addBox(-0.897F, -0.828F, -0.69F, 1.794F, 1.656F, 1.38F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.898F, -3.795F, 0.0F, 0.0F, 0.0F, 0.4189F));

		PartDefinition left_arm_3_upper = magatsu_mitake_narukami.addOrReplaceChild("left_arm_3_upper", CubeListBuilder.create().texOffs(224, 205).addBox(-9.7318F, -1.8975F, 0.3795F, 7.6949F, 1.035F, 1.035F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.6F, -21.0F, 1.6F, 0.0F, 0.3142F, -0.6283F));

		PartDefinition left_puppet_arm_3_upper_s3_r1 = left_arm_3_upper.addOrReplaceChild("left_puppet_arm_3_upper_s3_r1", CubeListBuilder.create().texOffs(220, 126).addBox(-2.0751F, -1.8678F, -1.9715F, 4.1503F, 3.7356F, 3.943F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0395F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_upper_s2_r1 = left_arm_3_upper.addOrReplaceChild("left_puppet_arm_3_upper_s2_r1", CubeListBuilder.create().texOffs(2, 115).addBox(-2.2235F, -1.8678F, -2.1124F, 4.4471F, 3.7356F, 4.2248F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4865F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_upper_s1_r1 = left_arm_3_upper.addOrReplaceChild("left_puppet_arm_3_upper_s1_r1", CubeListBuilder.create().texOffs(216, 103).addBox(-2.3718F, -1.8678F, -2.2533F, 4.7437F, 3.7356F, 4.5066F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.9335F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_upper_s0_r1 = left_arm_3_upper.addOrReplaceChild("left_puppet_arm_3_upper_s0_r1", CubeListBuilder.create().texOffs(104, 103).addBox(-2.5203F, -1.8678F, -2.3942F, 5.0405F, 3.7356F, 4.7884F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.3805F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_cap_trim_r1 = left_arm_3_upper.addOrReplaceChild("left_puppet_arm_3_cap_trim_r1", CubeListBuilder.create().texOffs(196, 63).addBox(-0.69F, -2.898F, -2.898F, 1.38F, 5.796F, 5.796F, new CubeDeformation(0.0F))
		.texOffs(140, 63).addBox(-0.207F, -2.622F, -2.622F, 2.622F, 5.244F, 5.244F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.966F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition left_puppet_arm_3_cap_pt_r1 = left_arm_3_upper.addOrReplaceChild("left_puppet_arm_3_cap_pt_r1", CubeListBuilder.create().texOffs(186, 137).addBox(-1.587F, -1.587F, -1.587F, 3.174F, 3.174F, 3.174F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.932F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition left_arm_3_lower = left_arm_3_upper.addOrReplaceChild("left_arm_3_lower", CubeListBuilder.create().texOffs(88, 147).addBox(-11.6472F, -1.8547F, -1.8547F, 2.3184F, 3.7094F, 3.7094F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.316F, 0.0F, 0.0F, 0.1091F, 0.8941F, 0.8029F));

		PartDefinition thumb_r2 = left_arm_3_lower.addOrReplaceChild("thumb_r2", CubeListBuilder.create().texOffs(142, 235).addBox(-1.2765F, -0.4278F, -0.4692F, 2.553F, 0.8556F, 0.9384F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.8497F, -1.2635F, 2.415F, 0.0F, 0.5934F, 0.5585F));

		PartDefinition f3_2_r2 = left_arm_3_lower.addOrReplaceChild("f3_2_r2", CubeListBuilder.create().texOffs(132, 235).addBox(-1.0695F, -0.3726F, -0.345F, 2.139F, 0.7452F, 0.69F, new CubeDeformation(0.0F))
		.texOffs(112, 235).addBox(-1.0695F, -0.3726F, -1.2834F, 2.139F, 0.7452F, 0.69F, new CubeDeformation(0.0F))
		.texOffs(92, 235).addBox(-1.0695F, -0.3726F, -2.2218F, 2.139F, 0.7452F, 0.69F, new CubeDeformation(0.0F))
		.texOffs(72, 235).addBox(-1.0695F, -0.3726F, -3.1602F, 2.139F, 0.7452F, 0.69F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.1976F, -2.6671F, 1.4076F, 0.0F, 0.0F, 0.8378F));

		PartDefinition f3_1_r2 = left_arm_3_lower.addOrReplaceChild("f3_1_r2", CubeListBuilder.create().texOffs(122, 235).addBox(-1.2765F, -0.4416F, -0.3864F, 2.553F, 0.8832F, 0.7728F, new CubeDeformation(0.0F))
		.texOffs(102, 235).addBox(-1.2765F, -0.4416F, -1.3248F, 2.553F, 0.8832F, 0.7728F, new CubeDeformation(0.0F))
		.texOffs(82, 235).addBox(-1.2765F, -0.4416F, -2.2632F, 2.553F, 0.8832F, 0.7728F, new CubeDeformation(0.0F))
		.texOffs(62, 235).addBox(-1.2765F, -0.4416F, -3.2016F, 2.553F, 0.8832F, 0.7728F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.5133F, -1.4365F, 1.4076F, 0.0F, 0.0F, 0.5236F));

		PartDefinition palm_energy_halo_r1 = left_arm_3_lower.addOrReplaceChild("palm_energy_halo_r1", CubeListBuilder.create().texOffs(108, 126).addBox(-2.622F, -0.8625F, -2.622F, 5.244F, 1.725F, 5.244F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.7859F, -2.1076F, 0.0F, 0.0F, -0.7854F, 0.2443F));

		PartDefinition palm_energy_glow_r1 = left_arm_3_lower.addOrReplaceChild("palm_energy_glow_r1", CubeListBuilder.create().texOffs(134, 126).addBox(-2.001F, -1.449F, -2.001F, 4.002F, 2.898F, 4.002F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.7525F, -2.2415F, 0.0F, 0.0F, -0.7854F, 0.2443F));

		PartDefinition palm_r2 = left_arm_3_lower.addOrReplaceChild("palm_r2", CubeListBuilder.create().texOffs(170, 147).addBox(-2.139F, -0.8625F, -2.001F, 4.278F, 1.725F, 4.002F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.9187F, -0.434F, 0.0F, 0.0F, 0.0F, 0.2443F));

		PartDefinition left_puppet_arm_3_fore_s3_r1 = left_arm_3_lower.addOrReplaceChild("left_puppet_arm_3_fore_s3_r1", CubeListBuilder.create().texOffs(2, 147).addBox(-1.6077F, -1.7415F, -1.5272F, 3.2154F, 3.4832F, 3.0544F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.2977F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_fore_s2_r1 = left_arm_3_lower.addOrReplaceChild("left_puppet_arm_3_fore_s2_r1", CubeListBuilder.create().texOffs(222, 137).addBox(-1.787F, -1.7415F, -1.6977F, 3.5742F, 3.4832F, 3.3954F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9173F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_fore_s1_r1 = left_arm_3_lower.addOrReplaceChild("left_puppet_arm_3_fore_s1_r1", CubeListBuilder.create().texOffs(204, 137).addBox(-1.9665F, -1.7415F, -1.8682F, 3.933F, 3.4832F, 3.7364F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5367F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_fore_s0_r1 = left_arm_3_lower.addOrReplaceChild("left_puppet_arm_3_fore_s0_r1", CubeListBuilder.create().texOffs(24, 115).addBox(-2.1459F, -1.7415F, -2.0385F, 4.2918F, 3.4832F, 4.0772F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1562F, 0.0F, 0.0F, -1.5708F, -1.5708F, 0.0F));

		PartDefinition left_puppet_arm_3_elbow_r1 = left_arm_3_lower.addOrReplaceChild("left_puppet_arm_3_elbow_r1", CubeListBuilder.create().texOffs(222, 91).addBox(-2.473F, -2.473F, -2.473F, 4.946F, 4.946F, 4.946F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		magatsu_mitake_narukami.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}