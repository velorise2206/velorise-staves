package com.velorise.staves.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.velorise.staves.client.character.DragonfireVisualRenderer;
import com.velorise.staves.client.character.LaylaVisualRenderer;
import com.velorise.staves.client.character.MavuikaVisualRenderer;
import com.velorise.staves.client.character.NahidaVisualRenderer;
import com.velorise.staves.client.character.NeuvilletteVisualRenderer;
import com.velorise.staves.client.character.RaidenVisualRenderer;
import com.velorise.staves.client.character.TsaritsaVisualRenderer;
import com.velorise.staves.client.character.FurinaVisualRenderer;
import com.velorise.staves.client.character.VentiVisualRenderer;
import com.velorise.staves.client.character.XilonenVisualRenderer;
import com.velorise.staves.client.character.ZhongliVisualRenderer;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.client.reaction.ElementalReactionBadgeRenderer;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.velorise.staves.mixin.AbstractContainerScreenAccessor;

import java.util.List;
import java.util.Optional;

public final class ClientAuraRenderer {
    private static boolean renderingScreen = false;
    private static List<Component> pendingInventoryTooltip = null;

    private ClientAuraRenderer() {}

    public static final class ShieldRenderTypes extends RenderStateShard {
        public static final RenderType TRANSLUCENT_NO_DEPTH_WRITE = RenderType.create(
                "velorise_shield_translucent",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
        public static final RenderType TRANSLUCENT_DEPTH_WRITE = RenderType.create(
                "velorise_translucent_depth_write",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false)
        );
        public static final RenderType METEOR_SOLID = RenderType.create(
                "velorise_meteor_solid",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false)
        );
        private static final java.util.Map<ResourceLocation, RenderType> PETRIFIED_STONE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<ResourceLocation, RenderType> PETRIFIED_CRACK_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

        public static RenderType getPetrifiedStone(ResourceLocation texture) {
            return PETRIFIED_STONE_CACHE.computeIfAbsent(texture, tex -> RenderType.create(
                    "velorise_petrified_stone",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    1536,
                    true,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setLayeringState(POLYGON_OFFSET_LAYERING)
                            .createCompositeState(true)
            ));
        }
        public static RenderType getPetrifiedCrack(ResourceLocation texture) {
            return PETRIFIED_CRACK_CACHE.computeIfAbsent(texture, tex -> RenderType.create(
                    "velorise_petrified_crack",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    1536,
                    true,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .createCompositeState(true)
            ));
        }
        public static final RenderType CROSSHAIR_CONTRAST = RenderType.create(
                "velorise_crosshair_contrast",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                                "crosshair_invert",
                                () -> {
                                    RenderSystem.enableBlend();
                                    RenderSystem.blendFuncSeparate(
                                            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                                            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                                            com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                                            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO
                                    );
                                },
                                () -> {
                                    RenderSystem.defaultBlendFunc();
                                    RenderSystem.disableBlend();
                                }
                        ))
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );
        private ShieldRenderTypes(String n, Runnable s, Runnable c) { super(n, s, c); }
    }

    public static RenderType getTranslucentNoDepthWrite() { return ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE; }
    public static RenderType getTranslucentDepthWrite() { return ShieldRenderTypes.TRANSLUCENT_DEPTH_WRITE; }
    public static RenderType getMeteorSolid() { return ShieldRenderTypes.METEOR_SOLID; }
    public static boolean isRenderingScreen() { return renderingScreen; }
    public static void setRenderingScreen(boolean rendering) { renderingScreen = rendering; }

    public static int clampColor(int c) { return Math.max(0, Math.min(255, c)); }
    public static int clampColorPublic(int c) { return clampColor(c); }

    public static void emitCrystalQuad(
            VertexConsumer consumer, Matrix4f matrix,
            float x1, float y1, float z1, int r1, int g1, int b1, int a1,
            float x2, float y2, float z2, int r2, int g2, int b2, int a2,
            float x3, float y3, float z3, int r3, int g3, int b3, int a3,
            float x4, float y4, float z4, int r4, int g4, int b4, int a4
    ) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r1, g1, b1, a1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r2, g2, b2, a2);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r3, g3, b3, a3);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r4, g4, b4, a4);
    }

    public static void emitCrystalQuadPublic(
            VertexConsumer consumer, Matrix4f matrix,
            float x1, float y1, float z1, int r1, int g1, int b1, int a1,
            float x2, float y2, float z2, int r2, int g2, int b2, int a2,
            float x3, float y3, float z3, int r3, int g3, int b3, int a3,
            float x4, float y4, float z4, int r4, int g4, int b4, int a4
    ) {
        emitCrystalQuad(consumer, matrix, x1, y1, z1, r1, g1, b1, a1, x2, y2, z2, r2, g2, b2, a2, x3, y3, z3, r3, g3, b3, a3, x4, y4, z4, r4, g4, b4, a4);
    }

    public static ResourceLocation getElementIconTexture(Element e) { return ElementalReactionBadgeRenderer.getElementIconTexture(e); }
    public static ResourceLocation getOmniIconTexture() { return ElementalReactionBadgeRenderer.getOmniIconTexture(); }
    public static ResourceLocation getResonanceIconTexture() { return ElementalReactionBadgeRenderer.getResonanceIconTexture(); }
    public static ResourceLocation getMoonsignIconTexture() { return ElementalReactionBadgeRenderer.getMoonsignIconTexture(); }
    public static boolean tryRender32pxElementItem(GuiGraphics g, ItemStack s, int x, int y) { return ElementalReactionBadgeRenderer.tryRender32pxElementItem(g, s, x, y); }
    public static void renderItemElementCornerBadge(GuiGraphics g, ItemStack s, int x, int y) { ElementalReactionBadgeRenderer.renderItemElementCornerBadge(g, s, x, y); }
    public static int getElementBorderColor(Element e) { return ElementalReactionBadgeRenderer.getElementBorderColor(e); }
    public static void renderColoredBorder(GuiGraphics g, int x, int y, int w, int h, int c) { ElementalReactionBadgeRenderer.renderColoredBorder(g, x, y, w, h, c); }
    public static void renderCrystallizeShieldHudFrame(GuiGraphics g, int x, int y, int c) { ElementalReactionBadgeRenderer.renderCrystallizeShieldHudFrame(g, x, y, c); }
    public static void renderCrystallizeShieldInventoryFrame(GuiGraphics g, int x, int y, int c) { ElementalReactionBadgeRenderer.renderCrystallizeShieldInventoryFrame(g, x, y, c); }

    public static void clearPendingInventoryTooltip() { pendingInventoryTooltip = null; }
    public static void renderPendingInventoryTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Component> tip = pendingInventoryTooltip;
        pendingInventoryTooltip = null;
        if (tip != null && !tip.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.renderTooltip(mc.font, tip, Optional.empty(), mouseX, mouseY);
        }
    }

    public static void resetFrozenClientState() {
        ElementalReactionBadgeRenderer.resetFrozenClientState();
        pendingInventoryTooltip = null;
    }
    public static boolean isLocalPlayerFrozen() { return ElementalReactionBadgeRenderer.isLocalPlayerFrozen(); }
    public static void tickFrozenPlayer() { ElementalReactionBadgeRenderer.tickFrozenPlayer(); }
    public static boolean handleFrozenPlayerTurn(Entity e, double y, double p) { return ElementalReactionBadgeRenderer.handleFrozenPlayerTurn(e, y, p); }
    public static float getTotalLevitationOffsetY(Entity entity, float partialTick) {
        return VentiVisualRenderer.getLevitationCameraOffsetY(entity, partialTick)
                + RaidenVisualRenderer.getLevitationCameraOffsetY(entity, partialTick)
                + MavuikaVisualRenderer.getIdleCameraOffsetY(entity, partialTick)
                + NahidaVisualRenderer.getViewfinderCameraOffsetY(entity, partialTick)
                + TsaritsaVisualRenderer.getBurstDpsLevitationOffsetY(entity, partialTick);
    }

    public static float getFrozenCameraYaw(Entity e, float fb) { return ElementalReactionBadgeRenderer.getFrozenCameraYaw(e, fb); }
    public static float getFrozenCameraPitch(Entity e, float fb) { return ElementalReactionBadgeRenderer.getFrozenCameraPitch(e, fb); }
    public static void clearPyroInnateFire(LivingEntity e) { ElementalReactionBadgeRenderer.clearPyroInnateFire(e); }
    public static void applyFrozenPoseLock(LivingEntity e) { ElementalReactionBadgeRenderer.applyFrozenPoseLock(e); }
    public static void renderFrozenIceOverlay(LivingEntity e, EntityModel<?> m, PoseStack ps, MultiBufferSource b) {
        if (!renderingScreen) ElementalReactionBadgeRenderer.renderFrozenIceOverlay(e, m, ps, b);
    }

    public static boolean isDaybreakPulling(LivingEntity e, ItemStack s) { return VentiVisualRenderer.isDaybreakPulling(e, s); }
    public static float getDaybreakPullProgress(LivingEntity e, ItemStack s) { return VentiVisualRenderer.getDaybreakPullProgress(e, s); }
    public static boolean isTsaritsaPulling(LivingEntity e, ItemStack s) { return TsaritsaVisualRenderer.isTsaritsaPulling(e, s); }
    public static float getTsaritsaPullProgress(LivingEntity e, ItemStack s) { return TsaritsaVisualRenderer.getTsaritsaPullProgress(e, s); }
    public static void renderDaybreakNockedWindCharge(LivingEntity e, ItemStack s, ItemDisplayContext dc, boolean lh, PoseStack ps, MultiBufferSource bs) {
        VentiVisualRenderer.renderDaybreakNockedWindCharge(e, s, dc, lh, ps, bs);
    }



    public static void resetHumanoidPoseDefaults(HumanoidModel<?> model) {
        if (model == null) return;
        model.rightArm.visible = true;
        if (model instanceof net.minecraft.client.model.PlayerModel<?> pm) {
            pm.rightSleeve.visible = true;
        }
        if (model.crouching) {
            model.body.xRot = 0.5F;
            model.body.setPos(0.0F, 3.2F, 0.0F);
            model.head.setPos(0.0F, 4.2F, 0.0F);
            model.hat.setPos(0.0F, 4.2F, 0.0F);
            model.rightArm.setPos(-5.0F, 5.2F, 0.0F);
            model.leftArm.setPos(5.0F, 5.2F, 0.0F);
            model.rightLeg.setPos(-1.9F, 12.2F, 4.0F);
            model.leftLeg.setPos(1.9F, 12.2F, 4.0F);
        } else {
            model.body.xRot = 0.0F;
            model.body.setPos(0.0F, 0.0F, 0.0F);
            model.head.setPos(0.0F, 0.0F, 0.0F);
            model.hat.setPos(0.0F, 0.0F, 0.0F);
            model.rightArm.setPos(-5.0F, 2.0F, 0.0F);
            model.leftArm.setPos(5.0F, 2.0F, 0.0F);
            model.rightLeg.setPos(-1.9F, 12.0F, 0.0F);
            model.leftLeg.setPos(1.9F, 12.0F, 0.0F);
        }

        model.rightLeg.zRot = 0.005F;
        model.leftLeg.zRot = -0.005F;
        model.rightLeg.yRot = 0.005F;
        model.leftLeg.yRot = -0.005F;
        model.body.zRot = 0.0F;
        model.body.yRot = 0.0F;
        model.head.zRot = 0.0F;
        model.hat.zRot = 0.0F;
        model.hat.yRot = 0.0F;
    }

    public static void renderTsaritsaCrown(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (entity == null || (ClientAuraTracker.isTsaritsaSpearStance(entity.getId()) && !ClientAuraTracker.isTsaritsaIdle(entity.getId()))) return;
        TsaritsaVisualRenderer.renderEntityEffects(entity, poseStack, buffer);
    }

    public static void applyStaffHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (entity == null || model == null || ClientAuraTracker.isFrozen(entity.getId())) return;
        resetHumanoidPoseDefaults(model);
        if (entity.onGround() || entity.isInWater() || entity.isInLava() || entity.getVehicle() != null) {
            if (ClientAuraTracker.isGliding(entity.getId())) {
                ClientAuraTracker.stopGliding(entity.getId());
            }
            if (ClientAuraTracker.isPlunging(entity.getId())) {
                ClientAuraTracker.stopPlunging(entity.getId());
            }
        }
        WindGliderRenderer.pollClientUseTransitions(entity);
        if (MavuikaVisualRenderer.applyBikeRidingHumanoidAnimation(entity, model)) return;

        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;
        float totalMs = (data != null) ? Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime)) : 1.0F;
        float t = (data != null) ? Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F) : 0.0F;

        if (WindGliderRenderer.applyPlungeAndGliderHumanoidPose(entity, model, ageInTicks)) return;
        if (XilonenVisualRenderer.applySkateStrideAndWallClimb(entity, model, ageInTicks, activeAnim)) return;
        if (com.velorise.staves.client.character.WallClimbVisualRenderer.applyWallClimbHumanoidPose(entity, model, ageInTicks)) return;
        if (NeuvilletteVisualRenderer.applyChannelingHumanoidPose(entity, model, activeAnim)) return;
        if (ZhongliVisualRenderer.applyHumanoidAnimation(entity, model, ageInTicks, activeAnim, t)) return;
        if (WindGliderRenderer.applyAuxiliaryStaffHumanoidAnimation(entity, model, ageInTicks, activeAnim, t)) return;
        if (NahidaVisualRenderer.applyIdleAndViewfinderHumanoidPose(entity, model, ageInTicks)) return;
        if (ZhongliVisualRenderer.applyIdleHumanoidPose(entity, model, ageInTicks)) return;
        if (RaidenVisualRenderer.applyIdleHumanoidPose(entity, model, ageInTicks)) return;
        if (MavuikaVisualRenderer.applyIdleHumanoidPose(entity, model, ageInTicks)) return;
        if (FurinaVisualRenderer.applyIdleHumanoidPose(entity, model, ageInTicks)) return;
        if (VentiVisualRenderer.applyIdleHumanoidPose(entity, model, ageInTicks)) return;
        if (TsaritsaVisualRenderer.applyIdleHumanoidPose(entity, model, ageInTicks)) return;
        if (DragonfireVisualRenderer.applyDragonfireIdleHumanoidPose(entity, model, ageInTicks)) return;
        if (TsaritsaVisualRenderer.applyBowPullHumanoidPose(entity, model)) return;
        if (VentiVisualRenderer.applyBowPullHumanoidPose(entity, model)) return;
        if (TsaritsaVisualRenderer.applySpearStanceGlidePose(entity, model, activeAnim)) return;

        if (activeAnim <= 0 || data == null) return;

        boolean ultiActive = (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW
                || activeAnim == ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL
                || activeAnim == ClientAuraTracker.ANIM_ZHONGLI_PLANET_BEFALL_FIRST
                || activeAnim == ClientAuraTracker.ANIM_VORTEX_HOLD_BURST
                || activeAnim == ClientAuraTracker.ANIM_VENTI_BURST
                || activeAnim == ClientAuraTracker.ANIM_NAHIDA_BURST
                || activeAnim == ClientAuraTracker.ANIM_MAVUIKA_BURST
                || activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_SUPPORT
                || activeAnim == ClientAuraTracker.ANIM_TSARITSA_BURST_DPS);
        boolean handled = RaidenVisualRenderer.applyHumanoidAnimation(entity, model, ageInTicks, data, activeAnim, t)
                || XilonenVisualRenderer.applyActionHumanoidAnimation(model, activeAnim, t)
                || NeuvilletteVisualRenderer.applyActionHumanoidAnimation(model, activeAnim, t)
                || MavuikaVisualRenderer.applyBurstHumanoidAnimation(entity, model, activeAnim, t)
                || VentiVisualRenderer.applyBurstHumanoidAnimation(entity, model, activeAnim, t)
                || NahidaVisualRenderer.applyActionHumanoidAnimation(model, ageInTicks, activeAnim, t)
                || TsaritsaVisualRenderer.applyActionHumanoidAnimation(entity, model, activeAnim, t);
        if (handled && ultiActive) {
            boolean raidenRecovery = (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW && (System.currentTimeMillis() - data.staffAnimStartTime) >= 1600L);
            if (!raidenRecovery) {
                model.head.yRot = 0.0F;
                model.hat.copyFrom(model.head);
            }
        }
    }

    public static void applyThirdPersonStaffItemTransform(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack) {
        if (entity == null || stack == null || stack.isEmpty()) return;
        if (displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && displayContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return;
        if (WindGliderRenderer.applyThirdPersonPlungeTransform(entity, leftHand, poseStack)) return;
        ClientAuraTracker.AuraDisplayData idleChk = ClientAuraTracker.get(entity.getId());
        boolean ventiIdleActive = ClientAuraTracker.isVentiIdle(entity.getId()) && !entity.isUsingItem() && !entity.swinging;
        boolean tsaritsaIdleActive = ClientAuraTracker.isTsaritsaIdle(entity.getId()) && !entity.isUsingItem() && !entity.swinging;
        if (idleChk != null && (idleChk.raidenIdle || idleChk.raidenIdleFade > 0.05F
                || idleChk.mavuikaIdle || idleChk.mavuikaIdleFade > 0.05F
                || idleChk.furinaIdleMode > 0 || idleChk.furinaIdleFade > 0.05F
                || ventiIdleActive
                || tsaritsaIdleActive
                || idleChk.nahidaIdleSwing || idleChk.nahidaIdleSwingFade > 0.05F
                || ((idleChk.zhongliIdleMeteor || idleChk.zhongliIdleFade > 0.05F) && !entity.isUsingItem() && !entity.swinging)
                || ((idleChk.dragonfireIdle || idleChk.dragonfireIdleFade > 0.05F) && !entity.isUsingItem() && !entity.swinging))) {
            poseStack.scale(0.0F, 0.0F, 0.0F);
            return;
        }
        if (!(stack.getItem() instanceof ArcaneStaffItem staff)) return;
        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(stack);
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (WindGliderRenderer.applyAuxiliaryThirdPersonItemTransform(entity, stack, staff, effKind, leftHand, poseStack, data, activeAnim, now)) return;
        if (ZhongliVisualRenderer.applyThirdPersonItemTransform(entity, stack, staff, effKind, leftHand, poseStack, data, activeAnim, now)) return;
        if (RaidenVisualRenderer.applyThirdPersonItemTransform(entity, effKind, leftHand, poseStack, data, activeAnim, now)) return;
        MavuikaVisualRenderer.applyThirdPersonItemTransform(entity, effKind, leftHand, poseStack);
        NahidaVisualRenderer.applyThirdPersonItemTransform(entity, effKind, poseStack, activeAnim);
        TsaritsaVisualRenderer.applyThirdPersonItemTransform(entity, effKind, leftHand, poseStack, activeAnim);
    }

    public static void applyFirstPersonStaffTransform(AbstractClientPlayer player, float partialTicks, InteractionHand hand, ItemStack stack, PoseStack poseStack) {
        if (player == null) return;
        if (com.velorise.staves.client.character.WallClimbVisualRenderer.applyFirstPersonWallClimbTransform(player, partialTicks, hand, poseStack)) return;
        if (stack == null || stack.isEmpty()) return;
        HumanoidArm arm = (hand == InteractionHand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
        float dir = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        if (WindGliderRenderer.applyFirstPersonPlungeTransform(player, dir, poseStack)) return;
        ClientAuraTracker.AuraDisplayData fpIdle = ClientAuraTracker.get(player.getId());
        boolean fpVentiIdle = ClientAuraTracker.isVentiIdle(player.getId()) && !player.isUsingItem() && !player.swinging;
        boolean fpTsaritsaIdle = ClientAuraTracker.isTsaritsaIdle(player.getId()) && !player.isUsingItem() && !player.swinging;
        boolean fpZhongliIdle = fpIdle != null && (fpIdle.zhongliIdleMeteor || fpIdle.zhongliIdleFade > 0.05F) && !player.isUsingItem() && !player.swinging;
        boolean fpMavuikaIdle = fpIdle != null && (fpIdle.mavuikaIdle || fpIdle.mavuikaIdleFade > 0.05F) && !player.isUsingItem() && !player.swinging && !(player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity);
        boolean fpFurinaIdle = fpIdle != null && (fpIdle.furinaIdleMode > 0 || fpIdle.furinaIdleFade > 0.05F) && !player.isUsingItem() && !player.swinging;
        boolean fpDragonfireIdle = fpIdle != null && (fpIdle.dragonfireIdle || fpIdle.dragonfireIdleFade > 0.05F) && !player.isUsingItem() && !player.swinging;
        if (fpIdle != null && (fpVentiIdle || fpIdle.raidenIdle || fpIdle.raidenIdleFade > 0.05F || fpTsaritsaIdle || fpIdle.nahidaIdleSwing || fpIdle.nahidaIdleSwingFade > 0.05F || fpZhongliIdle || fpMavuikaIdle || fpFurinaIdle || fpDragonfireIdle)) {
            poseStack.scale(0.0F, 0.0F, 0.0F);
            return;
        }
        if (!(stack.getItem() instanceof ArcaneStaffItem staff)) return;
        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(stack);
        WindGliderRenderer.pollClientUseTransitions(player);
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (ZhongliVisualRenderer.applyFirstPersonTransform(player, partialTicks, stack, staff, effKind, dir, poseStack, data, activeAnim, now)) return;
        if (RaidenVisualRenderer.applyFirstPersonTransform(player, partialTicks, effKind, dir, poseStack, data, activeAnim, now)) return;
        if (MavuikaVisualRenderer.applyFirstPersonTransform(player, partialTicks, dir, effKind, activeAnim, data, now, poseStack)) return;
        if (TsaritsaVisualRenderer.applyFirstPersonTransform(player, dir, effKind, activeAnim, data, now, poseStack)) return;
        if (NahidaVisualRenderer.applyFirstPersonTransform(effKind, dir, poseStack, data, activeAnim, now)) return;
        if (XilonenVisualRenderer.applyFirstPersonTransform(dir, poseStack, data, activeAnim, now)) return;
        if (VentiVisualRenderer.applyFirstPersonTransform(effKind, dir, poseStack, data, activeAnim, now)) return;
        if (NeuvilletteVisualRenderer.applyFirstPersonTransform(dir, poseStack, data, activeAnim, now)) return;
        WindGliderRenderer.applyAuxiliaryFirstPersonTransform(player, partialTicks, stack, staff, effKind, dir, poseStack, data, activeAnim, now);
    }

    public static void renderFirstPersonIdleEffects(AbstractClientPlayer player, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen || player == null || !player.isAlive() || player.isInvisible() || player.isSpectator()) return;
        VentiVisualRenderer.renderIdleEffects(player, poseStack, buffer);
        RaidenVisualRenderer.renderIdleEffects(player, poseStack, buffer);
        TsaritsaVisualRenderer.renderIdleThrone(player, poseStack, buffer);
    }

    public static boolean setupFirstPersonVortexMainArmPose(AbstractClientPlayer player, float partialTicks, HumanoidArm mainArm, PoseStack poseStack) {
        if (MavuikaVisualRenderer.setupFirstPersonBikeArmPose(player, partialTicks, mainArm, poseStack)) return true;
        if (VentiVisualRenderer.setupFirstPersonLyreArmPose(player, partialTicks, mainArm, poseStack)) return true;
        if (NahidaVisualRenderer.setupFirstPersonCameraArmPose(player, mainArm, poseStack)) return true;
        if (TsaritsaVisualRenderer.setupFirstPersonQuillArmPose(player, partialTicks, mainArm, poseStack)) return true;
        if (DragonfireVisualRenderer.setupFirstPersonDragonfireIdleArmPose(player, partialTicks, mainArm, poseStack)) return true;
        return ZhongliVisualRenderer.setupFirstPersonVortexMainArmPose(player, partialTicks, mainArm, poseStack);
    }

    public static boolean setupFirstPersonEmptyOffhandPose(AbstractClientPlayer player, float partialTicks, HumanoidArm offArm, PoseStack poseStack) {
        if (player == null) return false;
        if (com.velorise.staves.client.character.WallClimbVisualRenderer.setupFirstPersonEmptyOffhandClimbPose(player, partialTicks, offArm, poseStack)) return true;
        if (MavuikaVisualRenderer.setupFirstPersonBikeArmPose(player, partialTicks, offArm, poseStack)) return true;
        if (VentiVisualRenderer.setupFirstPersonLyreArmPose(player, partialTicks, offArm, poseStack)) return true;
        if (TsaritsaVisualRenderer.setupFirstPersonQuillArmPose(player, partialTicks, offArm, poseStack)) return true;
        ItemStack mainStack = player.getMainHandItem();
        if (!(mainStack.getItem() instanceof ArcaneStaffItem staff)) return false;
        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(mainStack);
        if (effKind == ArcaneStaffItem.Kind.VORTEX) {
            return ZhongliVisualRenderer.setupFirstPersonVortexArmPose(player, staff, mainStack, partialTicks, offArm, poseStack);
        }
        if (effKind == ArcaneStaffItem.Kind.SPLENDOR) {
            return WindGliderRenderer.setupFirstPersonSplendorOffhandPose(player, offArm, poseStack);
        }
        if (effKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) {
            return NahidaVisualRenderer.setupFirstPersonCameraArmPose(player, offArm, poseStack);
        }
        return false;
    }

    public static void renderMavuikaVirtualHelmet(LivingEntity entity, EntityModel<?> model, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen || entity == null || model == null) return;
        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        XilonenVisualRenderer.renderRollerSkatesAndSamples(entity, model, partialTicks, poseStack, buffer);
        WindGliderRenderer.renderGliderWings(entity, model, partialTicks, poseStack, buffer);
        MavuikaVisualRenderer.renderMavuikaVirtualHelmet(entity, model, partialTicks, poseStack, buffer);
        MavuikaVisualRenderer.renderIdleSunglasses(entity, model, partialTicks, poseStack, buffer);
    }

    public static void renderJadeShieldBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (!renderingScreen) ZhongliVisualRenderer.renderJadeShieldBarrier(entity, poseStack, buffer);
    }

    public static void renderNightsFocusBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (!renderingScreen) com.velorise.staves.client.character.LaylaVisualRenderer.renderNightsFocusBarrier(entity, poseStack, buffer);
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        render(entity, poseStack, buffer, 0);
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (renderingScreen || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        MavuikaVisualRenderer.renderMavuikaSunAuraRing(entity, poseStack, buffer);
        RaidenVisualRenderer.renderRaidenEyeRing(entity, poseStack, buffer);
        NahidaVisualRenderer.renderEntityEffects(entity, poseStack, buffer);
        ZhongliVisualRenderer.renderEntityEffects(entity, poseStack, buffer);
        RaidenVisualRenderer.renderIdleEffects(entity, poseStack, buffer);
        MavuikaVisualRenderer.renderIdleBike(entity, poseStack, buffer);
        FurinaVisualRenderer.renderIdleEffects(entity, poseStack, buffer);
        VentiVisualRenderer.renderIdleEffects(entity, poseStack, buffer);
        TsaritsaVisualRenderer.renderEntityEffects(entity, poseStack, buffer);
        XilonenVisualRenderer.renderXilonenDjBooth(entity, poseStack, buffer);
        DragonfireVisualRenderer.renderThirdPersonDragonfire(entity, poseStack, buffer);
        ElementalReactionBadgeRenderer.renderReactionBadges(entity, poseStack, buffer);
    }

    public static void renderPlayerAuraHud(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        if (!mc.player.isAlive() || mc.player.isDeadOrDying()) {
            ClientAuraTracker.remove(mc.player.getId());
            ClientAuraTracker.activeCrystallizeShield = null;
            ClientAuraTracker.crystallizeShieldUntil = 0L;
            return;
        }
        if (mc.screen instanceof EffectRenderingInventoryScreen<?> screen && screen.canSeeEffects()) return;
        ElementalReactionBadgeRenderer.renderCrystallizeShieldHud(guiGraphics, mc);
        ElementalReactionBadgeRenderer.renderResonanceHud(guiGraphics, mc);
        ElementalReactionBadgeRenderer.renderFreezeStruggleHud(guiGraphics, mc);
        StaffHudRenderer.renderChargeProgressHud(guiGraphics, mc);
        MavuikaVisualRenderer.renderMavuikaNightsoulHud(guiGraphics, mc);
        XilonenVisualRenderer.renderXilonenNightsoulHud(guiGraphics, mc);
        RaidenVisualRenderer.renderRaidenHud(guiGraphics, mc);
        ElementalReactionBadgeRenderer.renderPlayerAuraBars(guiGraphics, mc);
    }

    public static void renderInventoryAura(GuiGraphics guiGraphics, EffectRenderingInventoryScreen<?> screen, int leftPos, int topPos, int imageWidth, int mouseX, int mouseY) {
        pendingInventoryTooltip = ElementalReactionBadgeRenderer.renderInventoryAura(guiGraphics, screen, leftPos, topPos, imageWidth, mouseX, mouseY);
    }

    public static List<Rect2i> getInventoryExtraAreas(EffectRenderingInventoryScreen<?> screen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        return ElementalReactionBadgeRenderer.getInventoryExtraAreas(screen, accessor.velorise$getLeftPos(), accessor.velorise$getTopPos(), accessor.velorise$getImageWidth());
    }

    public static void renderCrystallizeShards(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        ElementalReactionBadgeRenderer.renderCrystallizeShards(poseStack, bufferSource, camera);
    }

    public static void renderFloatingCombatTexts(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        ElementalReactionBadgeRenderer.renderFloatingCombatTexts(poseStack, bufferSource, camera);
    }

    public static void renderActiveElectricArcs(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        NeuvilletteVisualRenderer.renderGuardianHydroBeams(poseStack, bufferSource, camera);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.currentTimeMillis();
        Vec3 camPos = camera.getPosition();
        ZhongliVisualRenderer.renderWorldMeteor(poseStack, bufferSource, camPos, now);
        VentiVisualRenderer.renderVentiWindChargeOrbs(poseStack, bufferSource, camPos, now);
        XilonenVisualRenderer.renderFirstPersonXilonenDjBooth(poseStack, bufferSource, camPos, now);
        TsaritsaVisualRenderer.renderFirstPersonIdleDesk(poseStack, bufferSource, camPos, now);
        ZhongliVisualRenderer.renderFirstPersonIdleMeteor(poseStack, bufferSource, camPos, now);
        DragonfireVisualRenderer.renderFirstPersonWorldDragonfire(poseStack, bufferSource, camPos);
        LaylaVisualRenderer.renderWorldLaylaDreamspheres(poseStack, bufferSource, camPos);
        StaffHudRenderer.renderShulkerTargetLockWorld(poseStack, bufferSource, camPos);

        if (ClientAuraTracker.ACTIVE_ARCS.isEmpty()) return;
        ClientAuraTracker.ACTIVE_ARCS.removeIf(arc -> now >= arc.expireTime);
        if (ClientAuraTracker.ACTIVE_ARCS.isEmpty()) return;

        NahidaVisualRenderer.renderWorldArcs(poseStack, bufferSource, camPos, now);
        TsaritsaVisualRenderer.renderWorldArcs(poseStack, bufferSource, camPos, now);
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.start != null && arc.end != null && arc.color == 0xFF5CE6C8) {
                VentiVisualRenderer.renderUpdraftArc(arc, arc.start, arc.end, camPos, now, poseStack, bufferSource);
            }
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.color == 0xFF5CE6C8) {
                continue;
            }
            if (arc.fromEntityId == -3 || arc.fromEntityId == -5 || arc.fromEntityId == -6 || arc.fromEntityId == -7 || arc.fromEntityId == -8) continue;
            net.minecraft.world.entity.Entity fromEnt = arc.fromEntityId >= 0 ? mc.level.getEntity(arc.fromEntityId) : null;
            net.minecraft.world.entity.Entity toEnt = arc.toEntityId >= 0 ? mc.level.getEntity(arc.toEntityId) : null;
            float arcPt = mc.getTimer().getGameTimeDeltaPartialTick(true);
            Vec3 startPos;
            if (arc.fromEntityId <= -10000) {
                int petOwnerId = -10000 - arc.fromEntityId;
                Vec3 liveMouth = DragonfireVisualRenderer.getActivePetMouthWorldPos(petOwnerId);
                if (liveMouth != null) {
                    arc.start = liveMouth;
                }
                startPos = arc.start;
            } else {
                startPos = fromEnt != null
                        ? fromEnt.getPosition(arcPt).add(0.0D, fromEnt.getBbHeight() * 0.45D, 0.0D)
                        : arc.start;
            }
            Vec3 endPos = toEnt != null
                    ? toEnt.getPosition(arcPt).add(0.0D, toEnt.getBbHeight() * 0.55D, 0.0D)
                    : arc.end;
            if (startPos == null || endPos == null) continue;
            if (VentiVisualRenderer.renderVortexStreamArc(arc, startPos, endPos, camPos, now, consumer, matrix)) continue;
            if (MavuikaVisualRenderer.renderSlashArc(arc, startPos, endPos, camPos, now, consumer, matrix)) continue;
            if (RaidenVisualRenderer.renderSlashArc(arc, startPos, endPos, camPos, now, consumer, matrix)) continue;
            if (LaylaVisualRenderer.renderStarProjectileArc(arc, startPos, endPos, camPos, now, poseStack, consumer, matrix)) continue;
            ElementalReactionBadgeRenderer.renderGenericLightningArc(arc, startPos, endPos, camPos, now, consumer, matrix);
        }
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
        // Translucent cosmic void-rift pass for Raiden Shogun Musou Isshin slashes
        boolean renderedRaidenRift = false;
        PoseStack.Pose riftPose = poseStack.last();
        float riftPt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.color != 0xFFB84DFF && arc.color != 0xFFE0B0FF) continue;
            Entity fromEnt = (arc.fromEntityId >= 0) ? mc.level.getEntity(arc.fromEntityId) : null;
            Entity toEnt = (arc.toEntityId >= 0) ? mc.level.getEntity(arc.toEntityId) : null;
            Vec3 startPos = fromEnt != null
                    ? fromEnt.getPosition(riftPt).add(0.0D, fromEnt.getBbHeight() * 0.45D, 0.0D)
                    : arc.start;
            Vec3 endPos = toEnt != null
                    ? toEnt.getPosition(riftPt).add(0.0D, toEnt.getBbHeight() * 0.55D, 0.0D)
                    : arc.end;
            if (startPos == null || endPos == null) continue;
            RaidenVisualRenderer.renderSlashRiftTexture(arc, startPos, endPos, camPos, now, bufferSource, riftPose);
            renderedRaidenRift = true;
        }
        if (renderedRaidenRift && bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
    }
}
