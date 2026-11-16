package com.velorise.staves.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.character.MavuikaVisualRenderer;
import com.velorise.staves.client.character.NahidaVisualRenderer;
import com.velorise.staves.client.character.NeuvilletteVisualRenderer;
import com.velorise.staves.client.character.RaidenVisualRenderer;
import com.velorise.staves.client.character.TsaritsaVisualRenderer;
import com.velorise.staves.client.character.VentiVisualRenderer;
import com.velorise.staves.client.character.XilonenVisualRenderer;
import com.velorise.staves.client.character.ZhongliVisualRenderer;
import com.velorise.staves.client.glider.WindGliderRenderer;
import com.velorise.staves.client.reaction.ElementalReactionBadgeRenderer;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalResonanceManager;
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
import net.minecraft.world.InteractionResult;
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
        private ShieldRenderTypes(String n, Runnable s, Runnable c) { super(n, s, c); }
    }

    public static RenderType getTranslucentNoDepthWrite() { return ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE; }
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



    public static void applyStaffHumanoidAnimation(LivingEntity entity, HumanoidModel<?> model, float ageInTicks) {
        if (entity == null || model == null || ClientAuraTracker.isFrozen(entity.getId())) return;
        WindGliderRenderer.pollClientUseTransitions(entity);
        if (MavuikaVisualRenderer.applyBikeRidingHumanoidAnimation(entity, model)) return;

        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;
        float totalMs = (data != null) ? Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime)) : 1.0F;
        float t = (data != null) ? Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F) : 0.0F;

        if (WindGliderRenderer.applyPlungeAndGliderHumanoidPose(entity, model, ageInTicks)) return;
        if (XilonenVisualRenderer.applySkateStrideAndWallClimb(entity, model, ageInTicks, activeAnim)) return;
        if (NeuvilletteVisualRenderer.applyChannelingHumanoidPose(entity, model, activeAnim)) return;
        if (ZhongliVisualRenderer.applyHumanoidAnimation(entity, model, ageInTicks, activeAnim, t)) return;
        if (WindGliderRenderer.applyAuxiliaryStaffHumanoidAnimation(entity, model, ageInTicks, activeAnim, t)) return;
        if (NahidaVisualRenderer.applyIdleAndViewfinderHumanoidPose(entity, model, ageInTicks)) return;
        if (TsaritsaVisualRenderer.applyBowPullHumanoidPose(entity, model)) return;
        if (VentiVisualRenderer.applyBowPullHumanoidPose(entity, model)) return;

        if (activeAnim <= 0 || data == null) return;

        if (RaidenVisualRenderer.applyHumanoidAnimation(entity, model, ageInTicks, data, activeAnim, t)) return;
        if (XilonenVisualRenderer.applyActionHumanoidAnimation(model, activeAnim, t)) return;
        if (NeuvilletteVisualRenderer.applyActionHumanoidAnimation(model, activeAnim, t)) return;
        if (MavuikaVisualRenderer.applyBurstHumanoidAnimation(entity, model, activeAnim, t)) return;
        if (VentiVisualRenderer.applyBurstHumanoidAnimation(model, activeAnim, t)) return;
        if (NahidaVisualRenderer.applyActionHumanoidAnimation(model, ageInTicks, activeAnim, t)) return;
        TsaritsaVisualRenderer.applyActionHumanoidAnimation(entity, model, activeAnim, t);
    }

    public static void applyThirdPersonStaffItemTransform(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack) {
        if (entity == null || stack == null || stack.isEmpty()) return;
        if (displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && displayContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return;
        if (WindGliderRenderer.applyThirdPersonPlungeTransform(entity, leftHand, poseStack)) return;
        if (!(stack.getItem() instanceof ArcaneStaffItem staff)) return;
        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(stack);
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (WindGliderRenderer.applyAuxiliaryThirdPersonItemTransform(entity, stack, staff, effKind, leftHand, poseStack, data, activeAnim, now)) return;
        if (ZhongliVisualRenderer.applyThirdPersonItemTransform(entity, stack, staff, effKind, leftHand, poseStack, data, activeAnim, now)) return;
        if (RaidenVisualRenderer.applyThirdPersonItemTransform(effKind, leftHand, poseStack, data, activeAnim, now)) return;
        MavuikaVisualRenderer.applyThirdPersonItemTransform(entity, effKind, leftHand, poseStack);
    }

    public static void applyFirstPersonStaffTransform(AbstractClientPlayer player, float partialTicks, InteractionHand hand, ItemStack stack, PoseStack poseStack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        HumanoidArm arm = (hand == InteractionHand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
        float dir = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        if (WindGliderRenderer.applyFirstPersonPlungeTransform(player, dir, poseStack)) return;
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
        if (NeuvilletteVisualRenderer.applyFirstPersonTransform(dir, poseStack, data, activeAnim, now)) return;
        WindGliderRenderer.applyAuxiliaryFirstPersonTransform(player, partialTicks, stack, staff, effKind, dir, poseStack, data, activeAnim, now);
    }

    public static boolean setupFirstPersonVortexMainArmPose(AbstractClientPlayer player, float partialTicks, HumanoidArm mainArm, PoseStack poseStack) {
        return ZhongliVisualRenderer.setupFirstPersonVortexMainArmPose(player, partialTicks, mainArm, poseStack);
    }

    public static boolean setupFirstPersonEmptyOffhandPose(AbstractClientPlayer player, float partialTicks, HumanoidArm offArm, PoseStack poseStack) {
        if (player == null) return false;
        ItemStack mainStack = player.getMainHandItem();
        if (!(mainStack.getItem() instanceof ArcaneStaffItem staff)) return false;
        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(mainStack);
        if (effKind == ArcaneStaffItem.Kind.VORTEX) {
            return ZhongliVisualRenderer.setupFirstPersonVortexArmPose(player, staff, mainStack, partialTicks, offArm, poseStack);
        }
        if (effKind == ArcaneStaffItem.Kind.SPLENDOR) {
            return WindGliderRenderer.setupFirstPersonSplendorOffhandPose(player, offArm, poseStack);
        }
        return false;
    }

    public static void renderMavuikaVirtualHelmet(LivingEntity entity, EntityModel<?> model, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen || entity == null || model == null) return;
        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        XilonenVisualRenderer.renderRollerSkatesAndSamples(entity, model, partialTicks, poseStack, buffer);
        WindGliderRenderer.renderGliderWings(entity, model, partialTicks, poseStack, buffer);
        MavuikaVisualRenderer.renderMavuikaVirtualHelmet(entity, model, partialTicks, poseStack, buffer);
    }

    public static void renderJadeShieldBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (!renderingScreen) ZhongliVisualRenderer.renderJadeShieldBarrier(entity, poseStack, buffer);
    }

    public static void renderNightsFocusBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (!renderingScreen) WindGliderRenderer.renderNightsFocusBarrier(entity, poseStack, buffer);
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        render(entity, poseStack, buffer, 0);
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (renderingScreen || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        MavuikaVisualRenderer.renderMavuikaSunAuraRing(entity, poseStack, buffer);
        RaidenVisualRenderer.renderRaidenEyeRing(entity, poseStack, buffer);
        NahidaVisualRenderer.renderEntityEffects(entity, poseStack, buffer);
        TsaritsaVisualRenderer.renderEntityEffects(entity, poseStack, buffer);
        XilonenVisualRenderer.renderXilonenDjBooth(entity, poseStack, buffer);
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
        WindGliderRenderer.renderChargeProgressHud(guiGraphics, mc);
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
        if (ClientAuraTracker.ACTIVE_ARCS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.currentTimeMillis();
        ClientAuraTracker.ACTIVE_ARCS.removeIf(arc -> now >= arc.expireTime);
        if (ClientAuraTracker.ACTIVE_ARCS.isEmpty()) return;

        Vec3 camPos = camera.getPosition();
        ZhongliVisualRenderer.renderWorldMeteor(poseStack, bufferSource, camPos, now);
        VentiVisualRenderer.renderVentiWindChargeOrbs(poseStack, bufferSource, camPos, now);
        NahidaVisualRenderer.renderWorldArcs(poseStack, bufferSource, camPos, now);
        TsaritsaVisualRenderer.renderWorldArcs(poseStack, bufferSource, camPos, now);
        XilonenVisualRenderer.renderFirstPersonXilonenDjBooth(poseStack, bufferSource, camPos, now);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            if (arc.fromEntityId == -3 || arc.fromEntityId == -5 || arc.fromEntityId == -6 || arc.fromEntityId == -7 || arc.fromEntityId == -8) continue;
            Vec3 startPos = (arc.fromEntityId >= 0 && mc.level.getEntity(arc.fromEntityId) != null)
                    ? mc.level.getEntity(arc.fromEntityId).position().add(0, mc.level.getEntity(arc.fromEntityId).getBbHeight() * 0.45D, 0)
                    : arc.start;
            Vec3 endPos = (arc.toEntityId >= 0 && mc.level.getEntity(arc.toEntityId) != null)
                    ? mc.level.getEntity(arc.toEntityId).position().add(0, mc.level.getEntity(arc.toEntityId).getBbHeight() * 0.45D, 0)
                    : arc.end;
            if (startPos == null || endPos == null) continue;
            if (VentiVisualRenderer.renderVortexStreamArc(arc, startPos, endPos, camPos, now, consumer, matrix)) continue;
            if (MavuikaVisualRenderer.renderSlashArc(arc, startPos, endPos, camPos, now, consumer, matrix)) continue;
            if (RaidenVisualRenderer.renderSlashArc(arc, startPos, endPos, camPos, now, consumer, matrix)) continue;
            if (WindGliderRenderer.renderStarProjectileArc(arc, startPos, endPos, camPos, now, poseStack, consumer, matrix)) continue;
            ElementalReactionBadgeRenderer.renderGenericLightningArc(arc, startPos, endPos, camPos, now, consumer, matrix);
        }
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }
}
