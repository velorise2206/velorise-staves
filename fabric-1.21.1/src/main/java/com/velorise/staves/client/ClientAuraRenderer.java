package com.velorise.staves.client;

import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.effect.ModMobEffects;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.ElementalReaction;
import com.velorise.staves.element.ElementalResonanceManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

public final class ClientAuraRenderer {
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);
    private static final Map<Element, ResourceLocation> TEXTURES_32 = new EnumMap<>(Element.class);
    private static final ResourceLocation OMNI_TEXTURE = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/omni.png");
    private static final ResourceLocation OMNI_TEXTURE_32 = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/omni_32.png");
    private static final ResourceLocation RESONANCE_TEXTURE = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/resonance.png");
    private static final ResourceLocation RESONANCE_TEXTURE_32 = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/resonance_32.png");
    private static final ResourceLocation MOONSIGN_TEXTURE = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/moonsign.png");
    private static final ResourceLocation MOONSIGN_TEXTURE_32 = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/moonsign_32.png");
    private static final Map<Element, ResourceLocation> HUD_TEXTURES = new EnumMap<>(Element.class);
    private static final Map<Element, ResourceLocation> SHIELD_TEXTURES = new EnumMap<>(Element.class);
    private static final ResourceLocation EFFECT_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/effect_background");
    private static final ResourceLocation EFFECT_BACKGROUND_LARGE_SPRITE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large");
    private static final ResourceLocation EFFECT_BACKGROUND_SMALL_SPRITE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT; // 0xF000F0
    private static final ResourceLocation FROSTED_ICE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/frosted_ice_0.png");

    private static final class FrozenRenderState {
        final float yBodyRot;
        final float yHeadRot;
        final float xRot;
        final float yRot;
        final float walkAnimPos;
        final float attackAnim;

        FrozenRenderState(LivingEntity entity) {
            this.yBodyRot = entity.yBodyRot;
            this.yHeadRot = entity.yHeadRot;
            this.xRot = entity.getXRot();
            this.yRot = entity.getYRot();
            this.walkAnimPos = entity.walkAnimation.position();
            this.attackAnim = entity.attackAnim;
        }
    }

    private static final Map<Integer, FrozenRenderState> FROZEN_RENDER_STATES = new HashMap<>();
    private static boolean wasJumpKeyDown = false;
    private static long lastStruggleSendTime = 0L;
    private static long struggleShakeUntil = 0L;
    private static boolean hasFrozenCam = false;
    private static boolean wasFrozenFirstPerson = false;
    private static float frozenCamYaw = 0.0F;
    private static float frozenCamPitch = 0.0F;

    static {
        for (Element e : Element.values()) {
            String lower = e.name().toLowerCase(Locale.ROOT);
            ResourceLocation res = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/" + lower + ".png");
            ResourceLocation res32 = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/" + lower + "_32.png");
            TEXTURES.put(e, res);
            TEXTURES_32.put(e, res32);
            HUD_TEXTURES.put(e, res);
            if (e == Element.PYRO || e == Element.HYDRO || e == Element.ELECTRO || e == Element.CRYO || e == Element.GEO) {
                SHIELD_TEXTURES.put(e, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/mob_effect/" + lower + "_shield.png"));
            }
        }
    }

    private static boolean renderingScreen = false;
    private static List<net.minecraft.network.chat.Component> pendingInventoryTooltip = null;

    public static boolean isRenderingScreen() {
        return renderingScreen;
    }

    public static void setRenderingScreen(boolean rendering) {
        renderingScreen = rendering;
    }

    private static boolean isLocalOverloadedForHud() {
        Minecraft mc = Minecraft.getInstance();
        return ClientAuraTracker.localOverloaded
                && !ElementalResonanceManager.isOverloadExempt(mc.player);
    }

    public static void clearPendingInventoryTooltip() {
        pendingInventoryTooltip = null;
    }

    public static void renderPendingInventoryTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<net.minecraft.network.chat.Component> tip = pendingInventoryTooltip;
        pendingInventoryTooltip = null;
        if (tip != null && !tip.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.renderTooltip(mc.font, tip, Optional.empty(), mouseX, mouseY);
        }
    }

    public static boolean tryRender32pxElementItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty() || !StavesConfig.isUse32pxElementIcons()) return false;
        if (!(stack.getItem() instanceof com.velorise.staves.item.ElementItem elemItem)) return false;
        ResourceLocation tex = elemItem.isOmni() ? getOmniIconTexture() : getElementIconTexture(elemItem.getElement());
        if (tex == null) return false;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(tex, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
        return true;
    }

    public static void renderItemElementCornerBadge(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        ResourceLocation tex = null;
        if (com.velorise.staves.item.SwordImbueHelper.isOmniImbued(stack)) {
            tex = getOmniIconTexture();
        } else {
            Element imbued = com.velorise.staves.item.SwordImbueHelper.getImbuedElement(stack);
            if (imbued != null) {
                tex = getElementIconTexture(imbued);
            } else if (stack.is(net.minecraft.world.item.Items.TRIDENT)) {
                tex = getElementIconTexture(Element.HYDRO);
            }
        }
        if (tex == null) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(tex, x, y, 0.0F, 0.0F, 6, 6, 6, 6);
        RenderSystem.enableDepthTest();
        guiGraphics.pose().popPose();
    }

    private ClientAuraRenderer() {}

    public static ResourceLocation getElementIconTexture(Element e) {
        if (e == null) return getOmniIconTexture();
        return StavesConfig.isUse32pxElementIcons() ? TEXTURES_32.get(e) : TEXTURES.get(e);
    }

    public static ResourceLocation getOmniIconTexture() {
        return StavesConfig.isUse32pxElementIcons() ? OMNI_TEXTURE_32 : OMNI_TEXTURE;
    }

    public static ResourceLocation getResonanceIconTexture() {
        return StavesConfig.isUse32pxElementIcons() ? RESONANCE_TEXTURE_32 : RESONANCE_TEXTURE;
    }

    public static ResourceLocation getMoonsignIconTexture() {
        return StavesConfig.isUse32pxElementIcons() ? MOONSIGN_TEXTURE_32 : MOONSIGN_TEXTURE;
    }

    public static void resetFrozenClientState() {
        FROZEN_RENDER_STATES.clear();
        hasFrozenCam = false;
        wasFrozenFirstPerson = false;
        pendingInventoryTooltip = null;
    }

    public static boolean isLocalPlayerFrozen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ClientAuraTracker.isFrozen(mc.player.getId());
    }

    public static void tickFrozenPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && System.currentTimeMillis() < ClientAuraTracker.silentDrainUntil) {
            mc.player.invulnerableTime = 0;
            mc.player.hurtTime = 0;
            mc.player.hurtDuration = 0;
        }
        if (mc.level != null && mc.player != null && mc.player.tickCount % 100 == 0) {
            ClientAuraTracker.pruneStale(mc.level);
            FROZEN_RENDER_STATES.entrySet().removeIf(e -> mc.level.getEntity(e.getKey()) == null || !ClientAuraTracker.isFrozen(e.getKey()));
        }
        if (mc.player == null || !ClientAuraTracker.isFrozen(mc.player.getId())) {
            if (mc.player != null) {
                FROZEN_RENDER_STATES.remove(mc.player.getId());
            }
            hasFrozenCam = false;
            wasFrozenFirstPerson = false;
            return;
        }
        FrozenRenderState state = FROZEN_RENDER_STATES.computeIfAbsent(mc.player.getId(), k -> new FrozenRenderState(mc.player));
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        if (!hasFrozenCam) {
            frozenCamYaw = state.yRot;
            frozenCamPitch = state.xRot;
            hasFrozenCam = true;
            wasFrozenFirstPerson = firstPerson;
        } else if (firstPerson) {
            wasFrozenFirstPerson = true;
        } else if (wasFrozenFirstPerson) {
            frozenCamYaw = state.yRot;
            frozenCamPitch = state.xRot;
            wasFrozenFirstPerson = false;
        }
        mc.player.setSprinting(false);
        mc.player.setYRot(state.yRot);
        mc.player.yRotO = state.yRot;
        mc.player.setXRot(state.xRot);
        mc.player.xRotO = state.xRot;
        mc.player.yHeadRot = state.yHeadRot;
        mc.player.yHeadRotO = state.yHeadRot;
        mc.player.yBodyRot = state.yBodyRot;
        mc.player.yBodyRotO = state.yBodyRot;
        mc.player.xBob = state.xRot;
        mc.player.xBobO = state.xRot;
        mc.player.yBob = state.yRot;
        mc.player.yBobO = state.yRot;
    }

    public static boolean handleFrozenPlayerTurn(Entity entity, double rawYaw, double rawPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity != mc.player || !ClientAuraTracker.isFrozen(mc.player.getId())) {
            return false;
        }
        FrozenRenderState state = FROZEN_RENDER_STATES.computeIfAbsent(mc.player.getId(), k -> new FrozenRenderState(mc.player));
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        if (!hasFrozenCam) {
            frozenCamYaw = state.yRot;
            frozenCamPitch = state.xRot;
            hasFrozenCam = true;
            wasFrozenFirstPerson = firstPerson;
        }
        if (!firstPerson) {
            if (wasFrozenFirstPerson) {
                frozenCamYaw = state.yRot;
                frozenCamPitch = state.xRot;
                wasFrozenFirstPerson = false;
            }
            float dYaw = (float) rawYaw * 0.15F;
            float dPitch = (float) rawPitch * 0.15F;
            frozenCamYaw = Mth.wrapDegrees(frozenCamYaw + dYaw);
            frozenCamPitch = Mth.clamp(frozenCamPitch + dPitch, -90.0F, 90.0F);
        } else {
            wasFrozenFirstPerson = true;
        }
        return true;
    }

    public static float getFrozenCameraYaw(Entity entity, float fallback) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity == mc.player && ClientAuraTracker.isFrozen(mc.player.getId())) {
            FrozenRenderState state = FROZEN_RENDER_STATES.computeIfAbsent(mc.player.getId(), k -> new FrozenRenderState(mc.player));
            boolean firstPerson = mc.options.getCameraType().isFirstPerson();
            if (!hasFrozenCam) {
                frozenCamYaw = state.yRot;
                frozenCamPitch = state.xRot;
                hasFrozenCam = true;
                wasFrozenFirstPerson = firstPerson;
            }
            if (!firstPerson) {
                if (wasFrozenFirstPerson) {
                    frozenCamYaw = state.yRot;
                    frozenCamPitch = state.xRot;
                    wasFrozenFirstPerson = false;
                }
                return frozenCamYaw;
            }
            wasFrozenFirstPerson = true;
            return state.yRot;
        }
        return fallback;
    }

    public static float getFrozenCameraPitch(Entity entity, float fallback) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity == mc.player && ClientAuraTracker.isFrozen(mc.player.getId())) {
            FrozenRenderState state = FROZEN_RENDER_STATES.computeIfAbsent(mc.player.getId(), k -> new FrozenRenderState(mc.player));
            boolean firstPerson = mc.options.getCameraType().isFirstPerson();
            if (!hasFrozenCam) {
                frozenCamYaw = state.yRot;
                frozenCamPitch = state.xRot;
                hasFrozenCam = true;
                wasFrozenFirstPerson = firstPerson;
            }
            if (!firstPerson) {
                if (wasFrozenFirstPerson) {
                    frozenCamYaw = state.yRot;
                    frozenCamPitch = state.xRot;
                    wasFrozenFirstPerson = false;
                }
                return frozenCamPitch;
            }
            wasFrozenFirstPerson = true;
            return state.xRot;
        }
        return fallback;
    }

    public static void renderJadeShieldBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        if (!ClientAuraTracker.hasJadeShield(entity)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        VertexConsumer vc = buffer.getBuffer(ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        float time = entity.tickCount + mc.getTimer().getGameTimeDeltaPartialTick(true);

        poseStack.pushPose();
        // Slowly rotate the golden cylinder & sigil
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * 2.2F));
        Matrix4f mat = poseStack.last().pose();

        int segs = 16;
        // Shrunk by ~50%: compact close-fitting radius and height extending from knee (0.23H) to neck (0.77H)
        float radius = Math.max(0.42F, entity.getBbWidth() * 0.62F);
        float h = entity.getBbHeight();
        float yBot = h * 0.23F;
        float yTop = h * 0.77F;
        float yMid = (yBot + yTop) * 0.5F;

        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2.0D * Math.PI / segs);
            float a1 = (float) ((i + 1) * 2.0D * Math.PI / segs);
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;

            // Lower golden cylinder wall (feathered knee bottom -> warm golden body)
            emitCrystalQuad(vc, mat,
                    x0, yBot, z0, 255, 200, 50, 14,
                    x1, yBot, z1, 255, 200, 50, 14,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x0, yMid, z0, 255, 212, 68, 56);
            emitCrystalQuad(vc, mat,
                    x0, yMid, z0, 255, 212, 68, 56,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x1, yBot, z1, 255, 200, 50, 14,
                    x0, yBot, z0, 255, 200, 50, 14);

            // Upper golden cylinder wall (warm golden body -> feathered neck top)
            emitCrystalQuad(vc, mat,
                    x0, yMid, z0, 255, 212, 68, 56,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x1, yTop, z1, 255, 228, 105, 14,
                    x0, yTop, z0, 255, 228, 105, 14);
            emitCrystalQuad(vc, mat,
                    x0, yTop, z0, 255, 228, 105, 14,
                    x1, yTop, z1, 255, 228, 105, 14,
                    x1, yMid, z1, 255, 212, 68, 56,
                    x0, yMid, z0, 255, 212, 68, 56);

            // Crisp luminous Geo border rings at neck top and knee bottom of cylinder (scaled 50%)
            float bandH = 0.018F;
            emitCrystalQuad(vc, mat,
                    x0, yBot, z0, 255, 218, 75, 130,
                    x1, yBot, z1, 255, 218, 75, 130,
                    x1, yBot + bandH, z1, 255, 238, 120, 0,
                    x0, yBot + bandH, z0, 255, 238, 120, 0);
            emitCrystalQuad(vc, mat,
                    x0, yTop - bandH, z0, 255, 238, 120, 0,
                    x1, yTop - bandH, z1, 255, 238, 120, 0,
                    x1, yTop, z1, 255, 218, 75, 130,
                    x0, yTop, z0, 255, 218, 75, 130);
        }

        // 4 Liyue Geo Diamond Runes etched on the cylinder wall (scaled 50%)
        float runeR = radius + 0.006F;
        float dSize = 0.11F;
        for (int k = 0; k < 4; k++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(k * 90.0F));
            Matrix4f rMat = poseStack.last().pose();
            drawShieldDiamondRing(vc, rMat, runeR, yMid, dSize, 0.015F, 255, 224, 82, 165);
            drawShieldDiamondRing(vc, rMat, runeR + 0.002F, yMid, dSize * 0.52F, 0.012F, 255, 245, 150, 195);
            poseStack.popPose();
        }
        poseStack.popPose();

        // 4 Diagonal Floating Dark-Bronze & Gold Monolith Plates (scaled 50%, aligned from neck to knee)
        float camYaw = mc.gameRenderer.getMainCamera().getYRot();
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camYaw));
        float bob = Mth.sin(time * 0.10F) * 0.02F;
        float plateDist = radius + 0.04F;

        float[][] corners = {
                {-0.31F, yTop - 0.14F + bob,  36.0F}, // Top-Left (neck level)
                { 0.31F, yTop - 0.14F - bob, -36.0F}, // Top-Right (neck level)
                {-0.31F, yBot + 0.14F - bob, -36.0F}, // Bottom-Left (knee level)
                { 0.31F, yBot + 0.14F + bob,  36.0F}  // Bottom-Right (knee level)
        };

        for (int c = 0; c < 4; c++) {
            float cx = corners[c][0];
            float cy = corners[c][1];
            float tiltDeg = corners[c][2];

            // Front (+Z) and Back (-Z) sets of compact floating armor plates
            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                poseStack.translate(cx, cy, side * plateDist * 0.68F);
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tiltDeg));
                Matrix4f pMat = poseStack.last().pose();

                drawJadeArmorBox(vc, pMat, -0.085F, -0.028F, -0.013F, 0.085F, 0.028F, 0.013F, 48, 30, 20, 235);
                drawJadeArmorBox(vc, pMat, -0.11F, -0.016F, -0.010F, -0.085F, 0.016F, 0.010F, 36, 22, 15, 235);
                drawJadeArmorBox(vc, pMat,  0.085F, -0.016F, -0.010F,  0.11F, 0.016F, 0.010F, 36, 22, 15, 235);
                float innerY = (cy > yMid) ? -0.036F : 0.028F;
                drawJadeArmorBox(vc, pMat, -0.075F, innerY, -0.009F, 0.075F, innerY + 0.011F, 0.009F, 255, 215, 64, 240);
                drawJadeArmorBox(vc, pMat, -0.06F, -0.006F, -0.014F, 0.06F, 0.006F, 0.014F, 245, 185, 48, 245);

                poseStack.popPose();
            }
        }
        poseStack.popPose();

        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }

    private static void drawShieldDiamondRing(
            VertexConsumer vc, Matrix4f mat,
            float z, float cy, float rad, float thick,
            int r, int g, int b, int a
    ) {
        float rIn = Math.max(0.01F, rad - thick);
        float[][] pts = {
                {0.0F,  1.0F},
                {1.0F,  0.0F},
                {0.0F, -1.0F},
                {-1.0F, 0.0F}
        };
        for (int i = 0; i < 4; i++) {
            float[] p0 = pts[i];
            float[] p1 = pts[(i + 1) % 4];
            emitCrystalQuad(vc, mat,
                    p0[0] * rIn, cy + p0[1] * rIn, z, r, g, b, a,
                    p0[0] * rad, cy + p0[1] * rad, z, r, g, b, a,
                    p1[0] * rad, cy + p1[1] * rad, z, r, g, b, a,
                    p1[0] * rIn, cy + p1[1] * rIn, z, r, g, b, a);
            emitCrystalQuad(vc, mat,
                    p1[0] * rIn, cy + p1[1] * rIn, z, r, g, b, a,
                    p1[0] * rad, cy + p1[1] * rad, z, r, g, b, a,
                    p0[0] * rad, cy + p0[1] * rad, z, r, g, b, a,
                    p0[0] * rIn, cy + p0[1] * rIn, z, r, g, b, a);
        }
    }

    private static void drawJadeArmorBox(
            VertexConsumer vc, Matrix4f mat,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int r, int g, int b, int a
    ) {
        // Front (+Z)
        emitCrystalQuad(vc, mat, x0, y0, z1, r, g, b, a, x1, y0, z1, r, g, b, a, x1, y1, z1, r, g, b, a, x0, y1, z1, r, g, b, a);
        // Back (-Z)
        emitCrystalQuad(vc, mat, x1, y0, z0, r, g, b, a, x0, y0, z0, r, g, b, a, x0, y1, z0, r, g, b, a, x1, y1, z0, r, g, b, a);
        // Top (+Y)
        emitCrystalQuad(vc, mat, x0, y1, z1, r, g, b, a, x1, y1, z1, r, g, b, a, x1, y1, z0, r, g, b, a, x0, y1, z0, r, g, b, a);
        // Bottom (-Y)
        emitCrystalQuad(vc, mat, x0, y0, z0, r, g, b, a, x1, y0, z0, r, g, b, a, x1, y0, z1, r, g, b, a, x0, y0, z1, r, g, b, a);
        // Left (-X)
        emitCrystalQuad(vc, mat, x0, y0, z0, r, g, b, a, x0, y0, z1, r, g, b, a, x0, y1, z1, r, g, b, a, x0, y1, z0, r, g, b, a);
        // Right (+X)
        emitCrystalQuad(vc, mat, x1, y0, z1, r, g, b, a, x1, y0, z0, r, g, b, a, x1, y1, z0, r, g, b, a, x1, y1, z1, r, g, b, a);
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        render(entity, poseStack, buffer);
    }

    public static void clearPyroInnateFire(LivingEntity entity) {
        if (entity == null) return;
        if ((ClientAuraTracker.hasPyroInnate(entity.getId()) || ClientAuraTracker.hasOmniInnate(entity.getId())) && (entity.getRemainingFireTicks() > 0 || entity.isOnFire())) {
            entity.clearFire();
            entity.setSharedFlagOnFire(false);
        }
    }

    private static final java.util.Map<Integer, Integer> LAST_USE_TICKS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Integer, com.velorise.staves.item.ArcaneStaffItem.Kind> LAST_USE_KIND = new java.util.concurrent.ConcurrentHashMap<>();

    private static float smoothStep01(float x) {
        float c = Mth.clamp(x, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    private static void pollClientUseTransitions(LivingEntity entity) {
        if (entity == null) return;
        int id = entity.getId();
        if (entity.isUsingItem()) {
            ItemStack useStack = entity.getUseItem();
            if (useStack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff) {
                com.velorise.staves.item.ArcaneStaffItem.Kind k = staff.kind();
                if (k == com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX
                        || k == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR
                        || k == com.velorise.staves.item.ArcaneStaffItem.Kind.GHASTFIRE
                        || k == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                    int elapsed = Math.max(0, staff.getUseDuration(useStack, entity) - entity.getUseItemRemainingTicks());
                    LAST_USE_TICKS.put(id, elapsed);
                    LAST_USE_KIND.put(id, k);
                    if (k == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR && elapsed == 10) {
                        ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
                        if (d == null || System.currentTimeMillis() >= d.staffAnimUntil) {
                            ClientAuraTracker.triggerStaffAnimation(id, ClientAuraTracker.ANIM_SPLENDOR_SWITCH);
                        }
                    }
                    return;
                }
            }
        }
        Integer prevElapsed = LAST_USE_TICKS.remove(id);
        com.velorise.staves.item.ArcaneStaffItem.Kind prevKind = LAST_USE_KIND.remove(id);
        if (prevElapsed != null && prevKind != null) {
            ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
            long now = System.currentTimeMillis();
            boolean alreadyTriggered = d != null && now < d.staffAnimUntil && (now - d.staffAnimStartTime) < 150L;
            if (!alreadyTriggered) {
                if (prevKind == com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX) {
                    boolean heldFull = prevElapsed >= 26 || entity.isShiftKeyDown();
                    ClientAuraTracker.triggerStaffAnimation(id, heldFull ? ClientAuraTracker.ANIM_VORTEX_HOLD_BURST : ClientAuraTracker.ANIM_VORTEX_TAP);
                } else if (prevKind == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR && prevElapsed < 10) {
                    ClientAuraTracker.triggerStaffAnimation(
                            id,
                            entity.isShiftKeyDown() ? ClientAuraTracker.ANIM_SPLENDOR_SWITCH : ClientAuraTracker.ANIM_SPLENDOR_SUMMON
                    );
                } else if (prevKind == com.velorise.staves.item.ArcaneStaffItem.Kind.GHASTFIRE && prevElapsed >= 6) {
                    ClientAuraTracker.triggerStaffAnimation(id, ClientAuraTracker.ANIM_GHASTFIRE_SLAM);
                } else if (prevKind == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING && prevElapsed >= 10) {
                    ClientAuraTracker.triggerStaffAnimation(id, ClientAuraTracker.ANIM_RAIDEN_DRAW);
                }
            }
        }
        if (entity.swinging && entity.swingTime == 0 && ClientAuraTracker.isRaidenMusouActive(id)) {
            ItemStack main = entity.getMainHandItem();
            if (main.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem s
                    && s.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                ClientAuraTracker.AuraDisplayData d = ClientAuraTracker.get(id);
                long now = System.currentTimeMillis();
                if (d == null || now >= d.staffAnimUntil || d.staffAnimType != ClientAuraTracker.ANIM_RAIDEN_DRAW) {
                    ClientAuraTracker.triggerStaffAnimation(id, ClientAuraTracker.ANIM_RAIDEN_SLASH);
                }
            }
        }
    }

    private static net.minecraft.world.entity.HumanoidArm resolveStaffArm(
            LivingEntity entity,
            @Nullable com.velorise.staves.item.ArcaneStaffItem.Kind targetKind
    ) {
        net.minecraft.world.entity.HumanoidArm main = entity.getMainArm();
        if (entity.isUsingItem()) {
            return entity.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND ? main.getOpposite() : main;
        }
        if (targetKind != null) {
            if (entity.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem s1 && s1.kind() == targetKind) {
                return main;
            }
            if (entity.getOffhandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem s2 && s2.kind() == targetKind) {
                return main.getOpposite();
            }
        }
        return main;
    }

    /**
     * Third-Person HumanoidModel upper-body & arm animations for:
     * - Furina (Splendor of Tranquil Waters):
     *   1) Summon (Tap): Weapon hand plants scepter vertically into the ground while opposite hand raises skyward.
     *   2) Arkhe Switch (Hold 0.5s / Shift+Click): Weapon hand lifts and plants scepter straight down into the ground.
     * - Zhongli (Vortex Vanquisher):
     *   1) Hold (0.0s -> 1.5s): Both arms converge/cross in front of the chest gathering Geo energy.
     *   2) Hold Release / Burst: Both arms sweep wide open outward around the body as the Jade Shield erupts.
     */
    public static void applyStaffHumanoidAnimation(
            LivingEntity entity,
            net.minecraft.client.model.HumanoidModel<?> model,
            float ageInTicks
    ) {
        if (entity == null || model == null || ClientAuraTracker.isFrozen(entity.getId())) return;
        pollClientUseTransitions(entity);

        if (entity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
            model.leftArm.xRot = -0.95F;
            model.leftArm.yRot = 0.14F;
            model.leftArm.zRot = -0.05F;
            int step = bike.getNaStep();
            int naT = bike.getNaTicks();
            if (step == 6 && naT > 0) {
                float armPitch;
                if (naT > 6) {
                    float w = (16.0F - naT) / 10.0F;
                    armPitch = Mth.lerp(w, -0.95F, -2.75F);
                } else if (naT > 3) {
                    float s = (6.0F - naT) / 3.0F;
                    armPitch = Mth.lerp(s, -2.75F, -0.35F);
                } else {
                    float r = (3.0F - naT) / 3.0F;
                    armPitch = Mth.lerp(r, -0.35F, -0.95F);
                }
                model.rightArm.xRot = armPitch;
                model.rightArm.yRot = 0.0F;
                model.rightArm.zRot = 0.08F;
            } else if (step == 1 && naT > 0) {
                float t = 1.0F - (naT / 10.0F);
                model.rightArm.xRot = Mth.lerp(t, -2.1F, -0.35F);
                model.rightArm.yRot = -0.25F;
                model.rightArm.zRot = 0.22F;
            } else if (step == 3 && naT > 0) {
                float t = 1.0F - (naT / 10.0F);
                model.rightArm.xRot = -1.25F;
                model.rightArm.yRot = Mth.lerp(t, -0.85F, 0.75F);
                model.rightArm.zRot = 0.45F;
            } else {
                model.rightArm.xRot = -0.95F;
                model.rightArm.yRot = -0.14F;
                model.rightArm.zRot = 0.05F;
            }
            return;
        }

        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        // 1. Check active holding state (Vortex Hold or Splendor Arkhe charge)
        if (entity.isUsingItem()) {
            ItemStack useStack = entity.getUseItem();
            if (useStack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff) {
                int elapsed = Math.max(0, staff.getUseDuration(useStack, entity) - entity.getUseItemRemainingTicks());
                if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX) {
                    float w = smoothStep01(elapsed / 18.0F);
                    float tremor = Mth.sin(ageInTicks * 1.65F) * 0.032F * w;
                    // Zhongli Hold Phase 1: Bring both arms together crossed in front of the chest
                    model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.32F + tremor);
                    model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.32F - tremor);
                    model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.72F);
                    model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.72F);
                    model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, -0.18F);
                    model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0.18F);
                    model.head.xRot += 0.15F * w;
                    model.hat.copyFrom(model.head);
                    return;
                }
                if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                    float w = smoothStep01(elapsed / 12.0F);
                    net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
                    net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, -1.25F);
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.85F);
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.35F);
                    return;
                }
                if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR && elapsed >= 2 && activeAnim == 0) {
                    // Furina Hold wind-up (ticks 2..10): Lower extended arm in front with vertical scepter
                    float w = smoothStep01((elapsed - 1.5F) / 7.5F);
                    net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR);
                    net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    net.minecraft.client.model.geom.ModelPart offPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
                    float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
                    float offDir = -castDir;

                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, -0.68F);
                    mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.18F);
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.04F);
                    offPart.xRot = Mth.lerp(w, offPart.xRot, -0.35F);
                    offPart.yRot = Mth.lerp(w, offPart.yRot, -offDir * 0.18F);
                    offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.22F);
                    return;
                }
                if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.GHASTFIRE) {
                    float w = smoothStep01(elapsed / 16.0F);
                    float tremor = Mth.sin(ageInTicks * 1.8F) * 0.025F * w;
                    net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.GHASTFIRE);
                    net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
                    mainPart.xRot = Mth.lerp(w, mainPart.xRot, -2.65F + tremor);
                    mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.14F);
                    return;
                }
            }
        }

        if (activeAnim <= 0 || data == null) return;
        float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
        float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);

        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW) {
            net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            net.minecraft.client.model.geom.ModelPart offPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float w = (t < 0.72F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.72F) / 0.28F) * (float) Math.PI));
            if (t < 0.45F) {
                float draw = smoothStep01(t / 0.45F);
                mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(draw, -1.15F, -1.42F));
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(draw, -0.75F, -1.08F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(draw, 0.28F, 0.48F));
                offPart.xRot = Mth.lerp(w, offPart.xRot, 0.25F * draw);
                model.head.xRot += 0.14F * draw;
                model.hat.copyFrom(model.head);
            } else {
                float slash = smoothStep01((t - 0.45F) / 0.25F);
                mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -1.42F, -0.78F));
                mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, -1.08F, 1.15F));
                mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * Mth.lerp(slash, 0.48F, -0.62F));
                offPart.xRot = Mth.lerp(w, offPart.xRot, Mth.lerp(slash, 0.25F, 0.42F));
            }
            return;
        }
        if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_SLASH) {
            net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
            net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float slash = smoothStep01(t / 0.45F);
            float w = (t < 0.55F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.55F) / 0.45F) * (float) Math.PI));
            mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slash, -1.95F, -0.35F));
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, castDir * Mth.lerp(slash, 0.55F, -0.65F));
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.25F);
            return;
        }
        if (activeAnim == ClientAuraTracker.ANIM_GHASTFIRE_SLAM) {
            float slam = smoothStep01(t / 0.35F);
            float w = (t < 0.45F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.45F) / 0.55F) * (float) Math.PI));
            net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.GHASTFIRE);
            net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
            mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(slam, -2.65F, -0.25F));
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.10F * (1.0F - slam));
            return;
        }
        if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON) {
            // Furina Summon: Lower extended casting hand plants scepter vertically into the ground; opposite hand raises high to the sky
            float w;
            if (t < 0.18F) {
                w = Mth.sin((t / 0.18F) * ((float) Math.PI * 0.5F));
            } else if (t < 0.72F) {
                w = 1.0F;
            } else {
                w = 0.5F * (1.0F + Mth.cos(((t - 0.72F) / 0.28F) * (float) Math.PI));
            }
            float plantDown;
            if (t < 0.12F) {
                plantDown = -0.25F * Mth.sin((t / 0.12F) * (float) Math.PI);
            } else {
                plantDown = smoothStep01((t - 0.12F) / 0.20F);
            }
            float skyPulse = Mth.sin(Mth.clamp((t - 0.14F) / 0.56F, 0.0F, 1.0F) * (float) Math.PI) * 0.14F;

            net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR);
            net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            net.minecraft.client.model.geom.ModelPart offPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;

            // Weapon hand: lowered forward-downward, planting the scepter 90 deg vertically onto the ground
            mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(plantDown, -0.72F, -0.40F));
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.18F);
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.04F);

            // Other hand: raised high toward the heavens
            offPart.xRot = Mth.lerp(w, offPart.xRot, -2.72F - skyPulse);
            offPart.yRot = Mth.lerp(w, offPart.yRot, -offDir * 0.16F);
            offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.28F);

            model.head.xRot -= 0.08F * w;
            model.hat.copyFrom(model.head);
        } else if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SWITCH) {
            // Furina Arkhe Switch: Lower extended arm in front and plant scepter tip 90 deg straight down into the ground
            float w;
            if (t < 0.15F) {
                w = Mth.sin((t / 0.15F) * ((float) Math.PI * 0.5F));
            } else if (t < 0.68F) {
                w = 1.0F;
            } else {
                w = 0.5F * (1.0F + Mth.cos(((t - 0.68F) / 0.32F) * (float) Math.PI));
            }
            float plantDown = smoothStep01(t / 0.24F);

            net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR);
            net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            net.minecraft.client.model.geom.ModelPart offPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;

            mainPart.xRot = Mth.lerp(w, mainPart.xRot, Mth.lerp(plantDown, -0.68F, -0.40F));
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.18F);
            mainPart.zRot = Mth.lerp(w, mainPart.zRot, castDir * 0.04F);

            offPart.xRot = Mth.lerp(w, offPart.xRot, -0.36F);
            offPart.yRot = Mth.lerp(w, offPart.yRot, -offDir * 0.18F);
            offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.22F);
        } else if (activeAnim == ClientAuraTracker.ANIM_VORTEX_HOLD_BURST) {
            // Zhongli Hold Phase 2: Both arms sweep powerfully from crossed-chest out to wide radial spread
            float sweep = Mth.sin(Mth.clamp(t / 0.24F, 0.0F, 1.0F) * ((float) Math.PI * 0.5F));
            float w = (t < 0.64F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.64F) / 0.36F) * (float) Math.PI));

            float targetX = Mth.lerp(sweep, -1.32F, -1.45F);
            float targetRightY = Mth.lerp(sweep, -0.72F, 0.82F);
            float targetLeftY = Mth.lerp(sweep, 0.72F, -0.82F);
            float targetRightZ = Mth.lerp(sweep, -0.18F, 0.95F);
            float targetLeftZ = Mth.lerp(sweep, 0.18F, -0.95F);

            model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, targetX);
            model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, targetX);
            model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, targetRightY);
            model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, targetLeftY);
            model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, targetRightZ);
            model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, targetLeftZ);

            model.head.xRot -= 0.14F * sweep * w;
            model.hat.copyFrom(model.head);
        } else if (activeAnim == ClientAuraTracker.ANIM_VORTEX_TAP) {
            // Zhongli Tap: Crisp forward polearm thrust to summon Stone Stele
            float w = Mth.sin(t * (float) Math.PI);
            net.minecraft.world.entity.HumanoidArm castArm = resolveStaffArm(entity, com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX);
            net.minecraft.client.model.geom.ModelPart mainPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            net.minecraft.client.model.geom.ModelPart offPart = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
            float castDir = castArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float offDir = -castDir;

            mainPart.xRot = Mth.lerp(w, mainPart.xRot, -1.48F);
            mainPart.yRot = Mth.lerp(w, mainPart.yRot, -castDir * 0.18F);
            offPart.xRot = Mth.lerp(w, offPart.xRot, 0.32F);
            offPart.zRot = Mth.lerp(w, offPart.zRot, offDir * 0.24F);
        }
    }

    /**
     * Rotates the third-person held item so Furina's Splendor of Tranquil Waters pivots its tip
     * straight downward like a planted royal cane, and Zhongli's Vortex Vanquisher aligns cleanly across the chest.
     */
    public static void applyThirdPersonStaffItemTransform(
            LivingEntity entity,
            ItemStack stack,
            net.minecraft.world.item.ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack
    ) {
        if (entity == null || stack == null || stack.isEmpty()) return;
        if (displayContext != net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && displayContext != net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        if (!(stack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff)) return;

        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR) {
            float plantW = 0.0F;
            float downOffset = 0.0F;
            float targetPitchDeg = -103.0F;
            if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON || activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SWITCH) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                if (t < 0.16F) {
                    plantW = Mth.sin((t / 0.16F) * ((float) Math.PI * 0.5F));
                } else if (t < 0.72F) {
                    plantW = 1.0F;
                } else {
                    plantW = 0.5F * (1.0F + Mth.cos(((t - 0.72F) / 0.28F) * (float) Math.PI));
                }
                float slamDown = smoothStep01(t / 0.24F);
                downOffset = slamDown * 0.14F;
                float armDeg = Mth.lerp(slamDown, -39.0F, -23.0F);
                targetPitchDeg = armDeg - 80.0F;
            } else if (entity.isUsingItem() && entity.getUseItem() == stack) {
                int elapsed = Math.max(0, staff.getUseDuration(stack, entity) - entity.getUseItemRemainingTicks());
                if (elapsed >= 2) {
                    plantW = smoothStep01((elapsed - 1.5F) / 7.5F);
                    float armDeg = Mth.lerp(plantW, -24.0F, -39.0F);
                    targetPitchDeg = armDeg - 80.0F;
                }
            }
            if (plantW > 0.001F) {
                float side = leftHand ? -1.0F : 1.0F;
                // Exact compensation so the tip of Splendor points 90 deg straight down into the ground
                poseStack.translate(0.0F, 0.06F * plantW, -0.08F * plantW + downOffset);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 10.3F * plantW));
                poseStack.mulPose(Axis.XP.rotationDegrees(targetPitchDeg * plantW));
            }
        } else if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX) {
            if (entity.isUsingItem() && entity.getUseItem() == stack) {
                int elapsed = Math.max(0, staff.getUseDuration(stack, entity) - entity.getUseItemRemainingTicks());
                float w = smoothStep01(elapsed / 18.0F);
                float side = leftHand ? -1.0F : 1.0F;
                poseStack.translate(side * 0.06F * w, 0.05F * w, 0.04F * w);
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 28.0F * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(-22.0F * w));
            }
        }
    }

    /**
     * First-Person held weapon animation for Furina (Splendor) and Zhongli (Vortex).
     */
    public static void applyFirstPersonStaffTransform(
            net.minecraft.client.player.AbstractClientPlayer player,
            float partialTicks,
            net.minecraft.world.InteractionHand hand,
            ItemStack stack,
            PoseStack poseStack
    ) {
        if (player == null || stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff)) return;
        pollClientUseTransitions(player);

        net.minecraft.world.entity.HumanoidArm arm = (hand == net.minecraft.world.InteractionHand.MAIN_HAND)
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        float dir = (arm == net.minecraft.world.entity.HumanoidArm.RIGHT) ? 1.0F : -1.0F;

        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX) {
            if (player.isUsingItem() && player.getUseItem() == stack) {
                int elapsed = Math.max(0, staff.getUseDuration(stack, player) - player.getUseItemRemainingTicks());
                float w = smoothStep01((elapsed + partialTicks) / 18.0F);
                float tremor = Mth.sin((player.tickCount + partialTicks) * 1.65F) * 0.010F * w;
                // Zhongli First-Person Hold: Center polearm upright in front of the chest
                poseStack.translate(-dir * 0.48F * w + tremor, 0.14F * w, -0.18F * w);
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 14.0F * w));
                poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(-dir * 12.0F * w));
                return;
            }
            if (activeAnim == ClientAuraTracker.ANIM_VORTEX_HOLD_BURST && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float sweep = Mth.sin(Mth.clamp(t / 0.24F, 0.0F, 1.0F) * ((float) Math.PI * 0.5F));
                float w = (t < 0.64F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.64F) / 0.36F) * (float) Math.PI));

                // Sweep smoothly from center chest outward as shield forms
                float tx = Mth.lerp(sweep, -dir * 0.48F, dir * 0.14F) * w;
                float ty = Mth.lerp(sweep, 0.14F, 0.06F) * w;
                float tz = Mth.lerp(sweep, -0.18F, -0.10F) * w;
                poseStack.translate(tx, ty, tz);
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(sweep, dir * 14.0F, -dir * 24.0F) * w));
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(sweep, -dir * 12.0F, -dir * 32.0F) * w));
                return;
            }
        } else if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR) {
            if (player.isUsingItem() && player.getUseItem() == stack && activeAnim == 0) {
                int elapsed = Math.max(0, staff.getUseDuration(stack, player) - player.getUseItemRemainingTicks());
                if (elapsed >= 2) {
                    float w = smoothStep01((elapsed + partialTicks - 1.5F) / 7.5F);
                    poseStack.translate(-dir * 0.20F * w, -0.08F * w, -0.16F * w);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-195.0F * w));
                    return;
                }
            }
            if ((activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON || activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SWITCH) && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float w;
                if (t < 0.16F) {
                    w = Mth.sin((t / 0.16F) * ((float) Math.PI * 0.5F));
                } else if (t < 0.70F) {
                    w = 1.0F;
                } else {
                    w = 0.5F * (1.0F + Mth.cos(((t - 0.70F) / 0.30F) * (float) Math.PI));
                }
                float lift = (t < 0.14F) ? Mth.sin((t / 0.14F) * (float) Math.PI) : 0.0F;
                float plantDown = smoothStep01(Math.max(0.0F, t - 0.08F) / 0.20F);

                // Lower hand in front and rotate scepter -200 deg around X so the tip points 90 deg straight down into the ground
                poseStack.translate(-dir * 0.20F * w, (-0.06F * lift - 0.28F * plantDown) * w, -0.18F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(plantDown, -165.0F, -200.0F) * w));
            }
        } else if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.GHASTFIRE) {
            if (player.isUsingItem() && player.getUseItem() == stack) {
                int elapsed = Math.max(0, staff.getUseDuration(stack, player) - player.getUseItemRemainingTicks());
                float w = smoothStep01((elapsed + partialTicks) / 16.0F);
                float tremor = Mth.sin((player.tickCount + partialTicks) * 1.8F) * 0.008F * w;
                poseStack.translate(-dir * 0.06F * w + tremor, 0.16F * w, -0.10F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 12.0F * w));
                return;
            }
            if (activeAnim == ClientAuraTracker.ANIM_GHASTFIRE_SLAM && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float slam = smoothStep01(t / 0.35F);
                float w = (t < 0.45F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.45F) / 0.55F) * (float) Math.PI));
                poseStack.translate(-dir * 0.06F * (1.0F - slam) * w, Mth.lerp(slam, 0.16F, -0.22F) * w, Mth.lerp(slam, -0.10F, -0.24F) * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slam, -18.0F, -68.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 12.0F * (1.0F - slam) * w));
                return;
            }
        } else if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            if (player.isUsingItem() && player.getUseItem() == stack) {
                int elapsed = Math.max(0, staff.getUseDuration(stack, player) - player.getUseItemRemainingTicks());
                float w = smoothStep01((elapsed + partialTicks) / 12.0F);
                poseStack.translate(-dir * 0.24F * w, -0.05F * w, -0.14F * w);
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 42.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * 32.0F * w));
                return;
            }
            if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_DRAW && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float w = (t < 0.72F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.72F) / 0.28F) * (float) Math.PI));
                if (t < 0.45F) {
                    float draw = smoothStep01(t / 0.45F);
                    poseStack.translate(-dir * Mth.lerp(draw, 0.22F, 0.34F) * w, Mth.lerp(draw, -0.04F, -0.10F) * w, -0.16F * w);
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(draw, 40.0F, 64.0F) * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(draw, 30.0F, 48.0F) * w));
                } else {
                    float slash = smoothStep01((t - 0.45F) / 0.25F);
                    poseStack.translate(dir * Mth.lerp(slash, -0.34F, 0.22F) * w, Mth.lerp(slash, -0.10F, 0.02F) * w, -0.20F * w);
                    poseStack.mulPose(Axis.YP.rotationDegrees(dir * Mth.lerp(slash, 64.0F, -78.0F) * w));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, 48.0F, -62.0F) * w));
                }
                return;
            }
            if (activeAnim == ClientAuraTracker.ANIM_RAIDEN_SLASH && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float slash = smoothStep01(t / 0.45F);
                float w = (t < 0.55F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.55F) / 0.45F) * (float) Math.PI));
                poseStack.translate(dir * Mth.lerp(slash, 0.12F, -0.22F) * w, Mth.lerp(slash, 0.12F, -0.16F) * w, -0.16F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(slash, 12.0F, -48.0F) * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(dir * Mth.lerp(slash, -35.0F, 42.0F) * w));
                return;
            }
        }
    }

    /**
     * Renders the player's empty offhand in First-Person during two-handed animations:
     * - Furina Summon: Opposite hand rises skyward while main hand plants Splendor into the ground.
     * - Zhongli Vortex Hold & Burst: Opposite hand converges in front of the chest and sweeps wide outward.
     */
    public static boolean setupFirstPersonVortexMainArmPose(
            net.minecraft.client.player.AbstractClientPlayer player,
            float partialTicks,
            net.minecraft.world.entity.HumanoidArm mainArm,
            PoseStack poseStack
    ) {
        if (player == null) return false;
        ItemStack mainStack = player.getMainHandItem();
        if (!(mainStack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff)) return false;
        if (staff.kind() != com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX) return false;
        return setupFirstPersonVortexArmPose(player, staff, mainStack, partialTicks, mainArm, poseStack);
    }

    private static boolean setupFirstPersonVortexArmPose(
            net.minecraft.client.player.AbstractClientPlayer player,
            com.velorise.staves.item.ArcaneStaffItem staff,
            ItemStack mainStack,
            float partialTicks,
            net.minecraft.world.entity.HumanoidArm arm,
            PoseStack poseStack
    ) {
        float armDir = (arm == net.minecraft.world.entity.HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (player.isUsingItem() && player.getUseItem() == mainStack) {
            int elapsed = Math.max(0, staff.getUseDuration(mainStack, player) - player.getUseItemRemainingTicks());
            float w = smoothStep01((elapsed + partialTicks) / 18.0F);
            if (w <= 0.02F) return false;
            float tremor = Mth.sin((player.tickCount + partialTicks) * 1.65F) * 0.010F * w;
            // Symmetrically angle both arms into an inverted-V (/ \) so the hands touch at top-center while sleeves point toward bottom corners
            poseStack.translate(armDir * 0.06F * w + armDir * tremor, -0.22F * (1.0F - w) + 0.02F * w, -0.10F * w);
            poseStack.mulPose(Axis.ZP.rotationDegrees(armDir * 38.0F * w));
            poseStack.mulPose(Axis.XP.rotationDegrees(26.0F * w));
            return true;
        }
        if (activeAnim == ClientAuraTracker.ANIM_VORTEX_HOLD_BURST && data != null) {
            float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
            float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
            float sweep = Mth.sin(Mth.clamp(t / 0.16F, 0.0F, 1.0F) * ((float) Math.PI * 0.5F));
            float w = (t < 0.64F) ? 1.0F : 0.5F * (1.0F + Mth.cos(((t - 0.64F) / 0.36F) * (float) Math.PI));
            if (w <= 0.02F) return false;

            // Snap both arms quickly from the inverted-V apex into parallel vertical (I  I) near screen edges
            float tx = Mth.lerp(sweep, armDir * 0.06F, armDir * 0.34F);
            float ty = Mth.lerp(sweep, 0.02F, -0.04F) - 0.22F * (1.0F - w);
            poseStack.translate(tx, ty, -0.10F * w);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(sweep, armDir * 38.0F, armDir * 18.0F) * w));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(sweep, 26.0F, 20.0F) * w));
            return true;
        }
        return false;
    }

    public static boolean setupFirstPersonEmptyOffhandPose(
            net.minecraft.client.player.AbstractClientPlayer player,
            float partialTicks,
            net.minecraft.world.entity.HumanoidArm offArm,
            PoseStack poseStack
    ) {
        if (player == null) return false;
        ItemStack mainStack = player.getMainHandItem();
        if (!(mainStack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff)) return false;

        float offDir = (offArm == net.minecraft.world.entity.HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        long now = System.currentTimeMillis();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(player.getId());
        int activeAnim = (data != null && now < data.staffAnimUntil) ? data.staffAnimType : 0;

        if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.VORTEX) {
            return setupFirstPersonVortexArmPose(player, staff, mainStack, partialTicks, offArm, poseStack);
        } else if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR) {
            if (activeAnim == ClientAuraTracker.ANIM_SPLENDOR_SUMMON && data != null) {
                float totalMs = Math.max(1.0F, (float) (data.staffAnimUntil - data.staffAnimStartTime));
                float t = Mth.clamp((now - data.staffAnimStartTime) / totalMs, 0.0F, 1.0F);
                float w;
                if (t < 0.18F) {
                    w = Mth.sin((t / 0.18F) * ((float) Math.PI * 0.5F));
                } else if (t < 0.72F) {
                    w = 1.0F;
                } else {
                    w = 0.5F * (1.0F + Mth.cos(((t - 0.72F) / 0.28F) * (float) Math.PI));
                }
                if (w <= 0.02F) return false;
                // Raise offhand high toward the top of the screen (skyward gesture)
                poseStack.translate(-offDir * 0.12F * w, -0.45F * (1.0F - w) + 0.38F * w, -0.22F * w);
                poseStack.mulPose(Axis.XP.rotationDegrees(38.0F * w));
                poseStack.mulPose(Axis.ZP.rotationDegrees(offDir * 18.0F * w));
                return true;
            }
        }
        return false;
    }

    public static void applyFrozenPoseLock(LivingEntity entity) {
        if (entity == null) return;
        clearPyroInnateFire(entity);
        if (!ClientAuraTracker.isFrozen(entity.getId())) {
            FROZEN_RENDER_STATES.remove(entity.getId());
            return;
        }
        FrozenRenderState state = FROZEN_RENDER_STATES.computeIfAbsent(entity.getId(), k -> new FrozenRenderState(entity));
        entity.setTicksFrozen(0);
        if (entity.isUsingItem()) {
            entity.stopUsingItem();
        }
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.setAggressive(false);
        }
        if (entity instanceof net.minecraft.world.entity.npc.AbstractVillager villager && villager.getUnhappyCounter() > 0) {
            villager.setUnhappyCounter(0);
        }

        float shakeOffset = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        double dy = entity.onGround() ? 0.0D : Math.max(-0.08D, entity.getDeltaMovement().y);
        entity.setDeltaMovement(0.0D, dy, 0.0D);
        entity.xo = entity.getX();
        entity.zo = entity.getZ();

        boolean isLocalPlayer = mc.player != null && entity.getId() == mc.player.getId();
        if (isLocalPlayer) {
            if (now < struggleShakeUntil) {
                shakeOffset = (float) Math.sin((struggleShakeUntil - now) * 0.85D) * 2.8F;
            }
            entity.setSprinting(false);
        }

        entity.yBodyRot = state.yBodyRot + shakeOffset;
        entity.yBodyRotO = state.yBodyRot + shakeOffset;
        entity.yHeadRot = state.yHeadRot + shakeOffset;
        entity.yHeadRotO = state.yHeadRot + shakeOffset;
        entity.setXRot(state.xRot);
        entity.xRotO = state.xRot;
        float appliedYRot = isLocalPlayer ? state.yRot : (state.yRot + shakeOffset);
        entity.setYRot(appliedYRot);
        entity.yRotO = appliedYRot;
        entity.attackAnim = state.attackAnim;
        entity.oAttackAnim = state.attackAnim;
        entity.walkAnimation.setSpeed(0.0F);
    }

    public static void renderFrozenIceOverlay(
            LivingEntity entity,
            net.minecraft.client.model.EntityModel<?> model,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        if (renderingScreen || entity == null || model == null) return;
        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        if (!ClientAuraTracker.isFrozen(entity.getId())) {
            FROZEN_RENDER_STATES.remove(entity.getId());
            return;
        }

        FrozenRenderState state = FROZEN_RENDER_STATES.computeIfAbsent(entity.getId(), k -> new FrozenRenderState(entity));
        float shakeOffset = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        if (mc.player != null && entity.getId() == mc.player.getId() && now < struggleShakeUntil) {
            shakeOffset = (float) Math.sin((struggleShakeUntil - now) * 0.85D) * 2.8F;
        }

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - (state.yBodyRot + shakeOffset)));
        float entityScale = entity.getScale();
        if (entity instanceof net.minecraft.world.entity.monster.Slime slime) {
            entityScale *= 0.999F * (float) slime.getSize();
        }
        float iceScale = entityScale * 1.025F;
        poseStack.scale(-iceScale, -iceScale, iceScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        VertexConsumer iceConsumer = buffer.getBuffer(RenderType.entityTranslucent(FROSTED_ICE_TEXTURE));
        model.renderToBuffer(poseStack, iceConsumer, FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0x7378E1FF);
        poseStack.popPose();
    }

    private static final class ShieldRenderTypes extends net.minecraft.client.renderer.RenderStateShard {
        private ShieldRenderTypes() { super("", () -> {}, () -> {}); }
        static final RenderType TRANSLUCENT_NO_DEPTH_WRITE = RenderType.create(
                "velorise_shield_no_depth",
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );
    }

    public static void renderNightsFocusBarrier(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen || entity == null || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        if (!ClientAuraTracker.hasNightsFocusShield(entity)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        VertexConsumer vc = buffer.getBuffer(ShieldRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
        float time = entity.tickCount + mc.getTimer().getGameTimeDeltaPartialTick(true);
        float h = entity.getBbHeight();
        float radius = Math.max(0.95F, h * 0.62F);
        float centerY = h * 0.50F;
        float pulse = 1.0F + 0.025F * Mth.sin(time * 0.14F);
        float rSphere = radius * pulse;

        poseStack.pushPose();
        poseStack.translate(0.0F, centerY, 0.0F);

        // 1. Translucent starry Curtain of Slumber dome
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * 1.4F));
        Matrix4f sphereMat = poseStack.last().pose();
        int stacks = 10;
        int slices = 18;
        for (int i = 0; i < stacks; i++) {
            float phi0 = (float) (-Math.PI * 0.5D + Math.PI * i / stacks);
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * (i + 1) / stacks);
            float y0 = Mth.sin(phi0) * rSphere;
            float y1 = Mth.sin(phi1) * rSphere;
            float ringR0 = Mth.cos(phi0) * rSphere;
            float ringR1 = Mth.cos(phi1) * rSphere;

            float eq0 = 1.0F - Math.abs(Mth.sin(phi0));
            float eq1 = 1.0F - Math.abs(Mth.sin(phi1));
            int r0 = (int) Mth.lerp(eq0, 110, 145);
            int g0 = (int) Mth.lerp(eq0, 130, 225);
            int b0 = 255;
            int a0 = (int) Mth.lerp(eq0, 16, 38);
            int r1 = (int) Mth.lerp(eq1, 110, 145);
            int g1 = (int) Mth.lerp(eq1, 130, 225);
            int b1 = 255;
            int a1 = (int) Mth.lerp(eq1, 16, 38);

            for (int j = 0; j < slices; j++) {
                float th0 = (float) (j * 2.0D * Math.PI / slices);
                float th1 = (float) ((j + 1) * 2.0D * Math.PI / slices);
                float x00 = Mth.cos(th0) * ringR0, z00 = Mth.sin(th0) * ringR0;
                float x01 = Mth.cos(th1) * ringR0, z01 = Mth.sin(th1) * ringR0;
                float x10 = Mth.cos(th0) * ringR1, z10 = Mth.sin(th0) * ringR1;
                float x11 = Mth.cos(th1) * ringR1, z11 = Mth.sin(th1) * ringR1;

                emitCrystalQuad(vc, sphereMat,
                        x00, y0, z00, r0, g0, b0, a0,
                        x01, y0, z01, r0, g0, b0, a0,
                        x11, y1, z11, r1, g1, b1, a1,
                        x10, y1, z10, r1, g1, b1, a1);
                emitCrystalQuad(vc, sphereMat,
                        x10, y1, z10, r1, g1, b1, a1,
                        x11, y1, z11, r1, g1, b1, a1,
                        x01, y0, z01, r0, g0, b0, a0,
                        x00, y0, z00, r0, g0, b0, a0);
            }
        }
        poseStack.popPose();

        // 2. Two tilted celestial astrolabe rings
        renderCelestialOrbitRing(poseStack, vc, rSphere * 1.03F, 0.022F, 24.0F, 12.0F, time * 2.6F, 160, 235, 255, 135);
        renderCelestialOrbitRing(poseStack, vc, rSphere * 1.06F, 0.018F, -32.0F, -18.0F, -time * 1.9F, 150, 175, 255, 110);

        // 3. 0..4 Suspended 3D 4-Pointed Night Stars hanging in an arc inside the upper dome
        int stars = ClientAuraTracker.getNightsFocusStars(entity);
        if (stars > 0) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * 1.4F));
            float[][] archOffsets = {
                    {-0.56F, 0.34F, 0.22F},
                    {-0.24F, 0.56F, 0.34F},
                    { 0.24F, 0.56F, 0.34F},
                    { 0.56F, 0.34F, 0.22F}
            };
            Matrix4f archMat = poseStack.last().pose();
            for (int s = 0; s < stars; s++) {
                float sx = archOffsets[s][0] * rSphere;
                float sy = archOffsets[s][1] * rSphere + 0.03F * Mth.sin(time * 0.16F + s * 1.4F);
                float sz = archOffsets[s][2] * rSphere;
                float ceilY = (float) Math.sqrt(Math.max(0.04F, rSphere * rSphere - sx * sx - sz * sz));
                // Luminous vertical star-thread from dome ceiling down to the hanging star
                float tw = 0.012F;
                emitCrystalQuad(vc, archMat,
                        sx - tw, sy + 0.10F, sz, 180, 235, 255, 165,
                        sx + tw, sy + 0.10F, sz, 180, 235, 255, 165,
                        sx + tw, ceilY, sz, 140, 210, 255, 70,
                        sx - tw, ceilY, sz, 140, 210, 255, 70);
                emitCrystalQuad(vc, archMat,
                        sx, sy + 0.10F, sz - tw, 180, 235, 255, 165,
                        sx, sy + 0.10F, sz + tw, 180, 235, 255, 165,
                        sx, ceilY, sz + tw, 140, 210, 255, 70,
                        sx, ceilY, sz - tw, 140, 210, 255, 70);

                poseStack.pushPose();
                poseStack.translate(sx, sy, sz);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * 4.0F + s * 45.0F));
                renderFourPointStar3D(vc, poseStack.last().pose(), 0.14F, 0.05F, 235, 250, 255, 250, 125, 195, 255, 125);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void renderCelestialOrbitRing(
            PoseStack poseStack, VertexConsumer vc, float ringR, float halfThick,
            float tiltX, float tiltZ, float spinY, int r, int g, int b, int a) {
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(tiltX));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tiltZ));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spinY));
        Matrix4f mat = poseStack.last().pose();
        int segs = 28;
        float rIn = ringR - halfThick;
        float rOut = ringR + halfThick;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2.0D * Math.PI / segs);
            float a1 = (float) ((i + 1) * 2.0D * Math.PI / segs);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            emitCrystalQuad(vc, mat,
                    c0 * rIn, 0.0F, s0 * rIn, r, g, b, a,
                    c1 * rIn, 0.0F, s1 * rIn, r, g, b, a,
                    c1 * rOut, 0.0F, s1 * rOut, r, g, b, a,
                    c0 * rOut, 0.0F, s0 * rOut, r, g, b, a);
            emitCrystalQuad(vc, mat,
                    c0 * rOut, 0.0F, s0 * rOut, r, g, b, a,
                    c1 * rOut, 0.0F, s1 * rOut, r, g, b, a,
                    c1 * rIn, 0.0F, s1 * rIn, r, g, b, a,
                    c0 * rIn, 0.0F, s0 * rIn, r, g, b, a);
        }
        poseStack.popPose();
    }

    private static void renderFourPointStar3D(
            VertexConsumer vc, Matrix4f mat, float outerLen, float innerThick,
            int cr, int cg, int cb, int ca, int gr, int gg, int gb, int ga) {
        emitStarDiamond(vc, mat, outerLen * 1.45F, innerThick * 1.45F, gr, gg, gb, ga);
        emitStarDiamond(vc, mat, outerLen, innerThick, cr, cg, cb, ca);
    }

    private static void emitStarDiamond(VertexConsumer vc, Matrix4f mat, float len, float w, int r, int g, int b, int a) {
        float[][] tips = {{0, len, 0}, {0, -len, 0}, {len, 0, 0}, {-len, 0, 0}, {0, 0, len}, {0, 0, -len}};
        for (float[] tip : tips) {
            if (Math.abs(tip[1]) > 0.001F) {
                emitCrystalQuad(vc, mat,
                        -w, 0, 0, r, g, b, a,
                        0, tip[1], 0, r, g, b, a,
                        w, 0, 0, r, g, b, a,
                        0, 0, w, r, g, b, a);
                emitCrystalQuad(vc, mat,
                        w, 0, 0, r, g, b, a,
                        0, tip[1], 0, r, g, b, a,
                        -w, 0, 0, r, g, b, a,
                        0, 0, -w, r, g, b, a);
            } else if (Math.abs(tip[0]) > 0.001F) {
                emitCrystalQuad(vc, mat,
                        0, -w, 0, r, g, b, a,
                        tip[0], 0, 0, r, g, b, a,
                        0, w, 0, r, g, b, a,
                        0, 0, w, r, g, b, a);
                emitCrystalQuad(vc, mat,
                        0, w, 0, r, g, b, a,
                        tip[0], 0, 0, r, g, b, a,
                        0, -w, 0, r, g, b, a,
                        0, 0, -w, r, g, b, a);
            } else {
                emitCrystalQuad(vc, mat,
                        -w, 0, 0, r, g, b, a,
                        0, 0, tip[2], r, g, b, a,
                        w, 0, 0, r, g, b, a,
                        0, w, 0, r, g, b, a);
                emitCrystalQuad(vc, mat,
                        w, 0, 0, r, g, b, a,
                        0, 0, tip[2], r, g, b, a,
                        -w, 0, 0, r, g, b, a,
                        0, -w, 0, r, g, b, a);
            }
        }
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen) return;
        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;
        renderMavuikaSunAuraRing(entity, poseStack, buffer);
        renderRaidenEyeRing(entity, poseStack, buffer);

        boolean showIcons = true;
        try {
            showIcons = StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.get();
        } catch (Exception ignored) {}
        if (!showIcons) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entity.distanceToSqr(mc.player) > 64.0D * 64.0D) return;
        if (entity != mc.player && !mc.player.hasLineOfSight(entity)) return;

        // Check innate element(s) on entity (including server-synced mutated innate elements, dual Charged Creeper, and Omni)
        EnumSet<Element> innateElements = ElementalGaugeManager.getInnateElements(entity);
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        if (data != null) {
            if (data.permanentInnateElement != null) {
                innateElements.add(data.permanentInnateElement);
            }
            innateElements.addAll(data.permanentInnateElements);
        }
        boolean hasInnate = !innateElements.isEmpty();
        boolean omniInnate = ElementalGaugeManager.isOmniInnate(entity) || (data != null && data.permanentOmniInnate);

        if (data == null && !hasInnate && !omniInnate) return;

        long now = System.currentTimeMillis();
        // Underwater fish / passive aquatic creatures only display overhead icons when an elemental reaction occurs
        if (ElementalGaugeManager.isPassiveAquaticOrAmbient(entity)) {
            boolean fishReactionActive = data != null && ((data.reaction != null && now < data.reactionUntil) || now < data.dualUntil);
            if (!fishReactionActive) return;
        }
        if (data != null) {
            if (data.expireTime > 0 && now >= data.expireTime && !hasInnate) {
                data.elements.clear();
            }
            if (data.reactionUntil <= now) {
                data.reaction = null;
            }
            if (data.dualUntil <= now) {
                data.reactedAuraElement = null;
                data.dualTriggerElement = null;
            }
            if (data.damageUntil <= now) {
                data.lastDamage = 0.0F;
            }
            if (data.elements.isEmpty() && data.reaction == null && data.dualUntil <= now && data.damageUntil <= now
                    && data.staffAnimUntil <= now && data.jadeShieldUntil <= now && data.nightsFocusUntil <= now && !data.isFrozen && !hasInnate && !omniInnate) {
                ClientAuraTracker.remove(entity.getId());
                return;
            }
            if (data.elements.isEmpty() && data.reaction == null && data.dualUntil <= now && data.damageUntil <= now && !hasInnate && !omniInnate) {
                return;
            }
        }

        List<Element> display = new ArrayList<>();
        boolean inDualReaction = data != null && now < data.dualUntil && data.reaction != null && (data.reactedAuraElement != null || data.dualTriggerElement != null);

        if (inDualReaction) {
            if (data.reactedAuraElement != null) {
                display.add(data.reactedAuraElement);
            }
            if (data.dualTriggerElement != null && !display.contains(data.dualTriggerElement)) {
                display.add(data.dualTriggerElement);
            }
            for (Element active : data.elements) {
                if (!display.contains(active)) {
                    display.add(active);
                }
            }
        } else if (data != null && !data.elements.isEmpty()) {
            display.addAll(data.elements);
        }
        for (Element innateElem : innateElements) {
            if (!display.contains(innateElem)) {
                // Always show all innate element icons (e.g. Charged Mutated Creeper dual aura + coexisting auras)
                display.add(innateElem);
            }
        }

        if (display.isEmpty() && !omniInnate) return;

        double height = (entity instanceof net.minecraft.world.entity.player.Player)
                ? entity.getBbHeight() + 0.18D
                : entity.getBbHeight() + 0.15D;
        float iconSize = 16.0F;
        float gap = 3.0F;

        // Draw Overhead Element Icon(s) with Pop & Fade-out Animation
        poseStack.pushPose();
        poseStack.translate(0.0D, height, 0.0D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        float popScale = 1.0F;
        int iconAlpha = 255;

        if (inDualReaction && data != null) {
            long dualAge = Math.max(0L, now - data.dualStartTime);
            long dualDuration = Math.max(1L, data.dualUntil - data.dualStartTime);
            float dualProgress = Math.min(1.0F, (float) dualAge / (float) dualDuration);

            // Scale pop in the first 4 ticks (~200ms)
            if (dualAge < 200L) {
                float popPhase = (float) dualAge / 200.0F;
                popScale = 1.0F + 0.28F * (float) Math.sin(popPhase * Math.PI);
            }
            // Fade out smoothly if the underlying aura was consumed
            if (data.elements.isEmpty() && !hasInnate && dualProgress > 0.55F) {
                float fade = (1.0F - dualProgress) / 0.45F;
                iconAlpha = Math.max(15, Math.min(255, Math.round(fade * 255.0F)));
            }
        }

        // Genshin-style expiration flickering (pulsing when remaining time <= 3.0s):
        if (data != null && data.expireTime > 0 && !hasInnate) {
            long remainingMs = data.expireTime - now;
            if (remainingMs <= 3000L && remainingMs > 0) {
                float pulse = (float) Math.sin((remainingMs / 140.0D) * Math.PI);
                if (pulse < 0.0F) {
                    iconAlpha = Math.max(35, Math.round(iconAlpha * 0.25F));
                }
            }
        }

        poseStack.scale(0.020F * popScale, -0.020F * popScale, 0.020F * popScale);

        if (omniInnate) {
            drawOmniIcon(poseStack, buffer, entity, -iconSize / 2.0F, -iconSize, iconSize, iconSize, 255);
            if (buffer instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderType.text(getOmniIconTexture()));
            }
        } else {
            int count = display.size();
            float totalWidth = count * iconSize + (count - 1) * gap;
            float startX = -totalWidth / 2.0F;

            for (int i = 0; i < count; i++) {
                Element elem = display.get(i);
                int elemAlpha = iconAlpha;
                if (inDualReaction && data != null && !data.elements.contains(elem) && !innateElements.contains(elem)) {
                    long dualAge = Math.max(0L, now - data.dualStartTime);
                    long dualDuration = Math.max(1L, data.dualUntil - data.dualStartTime);
                    float dualProgress = Math.min(1.0F, (float) dualAge / (float) dualDuration);
                    if (dualProgress > 0.55F) {
                        float fade = (1.0F - dualProgress) / 0.45F;
                        elemAlpha = Math.max(15, Math.min(255, Math.round(fade * 255.0F)));
                    }
                }
                drawIcon(poseStack, buffer, elem, startX + i * (iconSize + gap), -iconSize, iconSize, iconSize, elemAlpha);
            }

            // Flush icon render types BEFORE popping icon PoseStack
            if (buffer instanceof MultiBufferSource.BufferSource bs) {
                for (Element e : display) {
                    ResourceLocation tex = getElementIconTexture(e);
                    if (tex != null) {
                        bs.endBatch(RenderType.text(tex));
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private static void drawOmniIcon(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity, float x, float y, float width, float height, int alpha) {
        Matrix4f matrix = poseStack.last().pose();
        int rgb = com.velorise.staves.entity.OmniOrbEntity.getIridescentRgb(entity.tickCount + entity.getId() * 3.0F);
        int r = Math.min(255, 165 + (((rgb >> 16) & 0xFF) * 90) / 255);
        int g = Math.min(255, 165 + (((rgb >> 8) & 0xFF) * 90) / 255);
        int b = Math.min(255, 165 + ((rgb & 0xFF) * 90) / 255);
        VertexConsumer main = buffer.getBuffer(RenderType.text(getOmniIconTexture()));
        drawDoubleSidedQuad(main, matrix, x, y, width, height, r, g, b, alpha);
    }

    private static void drawIcon(PoseStack poseStack, MultiBufferSource buffer, Element element, float x, float y, float width, float height, int alpha) {
        ResourceLocation texture = getElementIconTexture(element);
        if (texture == null) return;

        Matrix4f matrix = poseStack.last().pose();

        // Main vivid layer (with depth test; never renders through solid blocks/terrain)
        VertexConsumer main = buffer.getBuffer(RenderType.text(texture));
        drawDoubleSidedQuad(main, matrix, x, y, width, height, 255, 255, 255, alpha);
    }

    private static void drawDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, float x, float y, float width, float height, int r, int g, int b, int a) {
        // Front face
        consumer.addVertex(matrix, x, y, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y + height, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y + height, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F).setLight(FULL_BRIGHT);

        // Back face (counter-winding)
        consumer.addVertex(matrix, x + width, y, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y + height, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y + height, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F).setLight(FULL_BRIGHT);
    }

    private static int getReactionColor(ElementalReaction reaction) {
        return switch (reaction) {
            case IMMUNE -> 0xE0E0E8;
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE, OVERVAPE -> 0xFF8C33;
            case FORWARD_MELT, REVERSE_MELT -> 0xFF3B30;
            case OVERLOADED -> 0xFF6B4A;
            case SUPERCONDUCT -> 0x5AC8FA;
            case ELECTRO_CHARGED -> 0xBF5AF2;
            case FROZEN -> 0x64D2FF;
            case SHATTER -> 0xE5E5EA;
            case QUICKEN -> 0xA8E063;
            case AGGRAVATE -> 0xFFD60A;
            case SPREAD -> 0x34C759;
            case BLOOM, REVERSE_BLOOM -> 0x32D74B;
            case HYPERBLOOM -> 0xDA70D6;
            case BURGEON -> 0xFF9F0A;
            case BURNING -> 0xFF453A;
            case SWIRL -> 0x55EFC4;
            case CRYSTALLIZE -> 0xFFD60A;
            default -> 0xFFFFFF;
        };
    }

    private static int getElementTextColor(Element element) {
        return switch (element) {
            case PYRO -> 0xFF453A;
            case HYDRO -> 0x0A84FF;
            case CRYO -> 0x64D2FF;
            case ELECTRO -> 0xBF5AF2;
            case ANEMO -> 0x55EFC4;
            case GEO -> 0xFFD60A;
            case DENDRO -> 0x32D74B;
        };
    }

    /**
     * Renders an Elemental Aura Status Effect badge in the top-right corner,
     * positioned similarly to vanilla Minecraft potion effect indicators.
     * Includes countdown timer, colored elemental outline, and expiration flickering.
     */
    public static void renderPlayerAuraHud(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        if (!mc.player.isAlive() || mc.player.isDeadOrDying()) {
            ClientAuraTracker.remove(mc.player.getId());
            ClientAuraTracker.activeCrystallizeShield = null;
            ClientAuraTracker.crystallizeShieldUntil = 0L;
            return;
        }
        if (mc.screen instanceof EffectRenderingInventoryScreen screen && screen.canSeeEffects()) {
            return;
        }

        renderCrystallizeShieldHud(guiGraphics, mc);
        renderResonanceHud(guiGraphics, mc);
        renderFreezeStruggleHud(guiGraphics, mc);
        renderChargeProgressHud(guiGraphics, mc);
        renderMavuikaNightsoulHud(guiGraphics, mc);
        renderRaidenHud(guiGraphics, mc);

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        if (data == null || data.elements.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Environmental refresh: while player is actively burning, in lava, or in water, keep duration refreshed!
        if (data.expireTime > 0 && data.elements.contains(Element.PYRO) && (mc.player.isInLava() || mc.player.isOnFire() || mc.player.getRemainingFireTicks() > 0)) {
            data.expireTime = now + 9500L;
        }
        if (data.expireTime > 0 && data.elements.contains(Element.HYDRO) && mc.player.isInWaterOrRain()) {
            data.expireTime = now + 9500L;
        }

        if (data.expireTime > 0 && now >= data.expireTime) {
            data.elements.clear();
            ClientAuraTracker.remove(mc.player.getId());
            return;
        }

        int screenWidth = guiGraphics.guiWidth();
        int top = mc.isDemo() ? 16 : 1;

        int beneficialCount = 0;
        int harmfulCount = 0;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (inst.showIcon()) {
                if (inst.getEffect().value().isBeneficial()) {
                    beneficialCount++;
                } else {
                    harmfulCount++;
                }
            }
        }
        Element activeShield = ClientAuraTracker.getActiveCrystallizeShield();
        if (activeShield == null) {
            activeShield = ModMobEffects.getActiveShield(mc.player);
        }
        if (activeShield != null) {
            boolean shieldInVanilla = false;
            for (MobEffectInstance inst : mc.player.getActiveEffects()) {
                if (inst.showIcon() && ModMobEffects.getShieldElement(inst.getEffect().value()) != null) {
                    shieldInVanilla = true;
                    break;
                }
            }
            if (!shieldInVanilla) {
                beneficialCount++;
            }
        }
        beneficialCount += getResonanceHudSlotCount();

        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int index = 0;
        for (Element elem : data.elements) {
            int col;
            int row;
            if (beneficialCount > 0 || harmfulCount > 0) {
                // Place in harmful effects row (Row 1) after existing harmful effects
                col = (harmfulCount + index) % 10;
                row = 1 + (harmfulCount + index) / 10;
            } else {
                // No vanilla effects: start from row 0 top-right
                col = index % 10;
                row = index / 10;
            }

            int x = screenWidth - 25 * (col + 1);
            int y = top + 26 * row;

            long remMs = data.expireTime > 0 ? Math.max(0, data.expireTime - now) : 0;
            float alpha = 1.0F;
            if (data.expireTime > 0 && remMs <= 3000L) {
                // Smooth vanilla-style blinking during the last 3 seconds
                alpha = Math.sin((remMs / 140.0D) * Math.PI) < 0 ? 0.35F : 1.0F;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();

            // Vanilla 24x24 effect background (solid background, never blinks)
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SPRITE, x, y, 24, 24);

            // 16x16 Element icon centered inside the 24x24 box (blinks during last 3 seconds)
            ResourceLocation tex = getElementIconTexture(elem);
            if (tex != null) {
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                }
                guiGraphics.blit(tex, x + 4, y + 4, 0.0F, 0.0F, 16, 16, 16, 16);
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            index++;
        }
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderInventoryAura(
            GuiGraphics guiGraphics,
            EffectRenderingInventoryScreen<?> screen,
            int leftPos,
            int topPos,
            int imageWidth,
            int mouseX,
            int mouseY
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        pendingInventoryTooltip = null;

        List<net.minecraft.network.chat.Component> shieldTooltip =
                renderCrystallizeShieldInventory(guiGraphics, mc, leftPos, topPos, imageWidth, screen, mouseX, mouseY);
        if (shieldTooltip != null) {
            pendingInventoryTooltip = shieldTooltip;
        }
        List<net.minecraft.network.chat.Component> resTooltip =
                renderResonanceInventory(guiGraphics, mc, leftPos, topPos, imageWidth, screen, mouseX, mouseY);
        if (pendingInventoryTooltip == null && resTooltip != null) {
            pendingInventoryTooltip = resTooltip;
        }

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        if (data == null || data.elements.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Environmental refresh: while player is actively burning, in lava, or in water, keep duration refreshed!
        if (data.expireTime > 0 && data.elements.contains(Element.PYRO) && (mc.player.isInLava() || mc.player.isOnFire() || mc.player.getRemainingFireTicks() > 0)) {
            data.expireTime = now + 9500L;
        }
        if (data.expireTime > 0 && data.elements.contains(Element.HYDRO) && mc.player.isInWaterOrRain()) {
            data.expireTime = now + 9500L;
        }

        if (data.expireTime > 0 && now >= data.expireTime) {
            data.elements.clear();
            ClientAuraTracker.remove(mc.player.getId());
            return;
        }

        int startX = leftPos + imageWidth + 2;
        int availableWidth = screen.width - startX;
        if (availableWidth < 32) return;

        // Flush any pending screen elements to prevent shader color bleed
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableDepthTest();

        int vanillaCount = 0;
        boolean shieldFoundInVanilla = false;
        Element activeShield = ClientAuraTracker.getActiveCrystallizeShield();
        if (activeShield == null) {
            activeShield = ModMobEffects.getActiveShield(mc.player);
        }
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            vanillaCount++;
            if (ModMobEffects.getShieldElement(inst.getEffect().value()) != null) {
                shieldFoundInVanilla = true;
            }
        }
        int customShieldSlot = (activeShield != null && !shieldFoundInVanilla) ? 1 : 0;
        int totalEffects = vanillaCount + customShieldSlot + data.elements.size() + getResonanceHudSlotCount();
        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        List<net.minecraft.network.chat.Component> pendingTooltip = null;
        int index = 0;
        for (Element elem : data.elements) {
            int y = topPos + (vanillaCount + customShieldSlot + index) * yStep;
            long remMs = data.expireTime > 0 ? Math.max(0, data.expireTime - now) : 0;
            float alpha = 1.0F;
            if (data.expireTime > 0 && remMs <= 3000L) {
                alpha = Math.sin((remMs / 140.0D) * Math.PI) < 0 ? 0.35F : 1.0F;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            String durationStr;
            if (data.expireTime < 0) {
                durationStr = "∞";
            } else if (data.expireTime > 0) {
                long remSec = remMs / 1000L;
                durationStr = String.format(Locale.ROOT, "%d:%02d", remSec / 60, remSec % 60);
            } else {
                durationStr = "";
            }

            ResourceLocation tex = getElementIconTexture(elem);

            // Compact Mode (32x32 square) to avoid overlapping with JEI
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SMALL_SPRITE, startX, y, 32, 32);
            guiGraphics.flush();
            if (tex != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                }
                guiGraphics.blit(tex, startX + 8, y + 8, 0.0F, 0.0F, 16, 16, 16, 16);
                guiGraphics.flush();
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            if (mouseX >= startX && mouseX <= startX + 32 && mouseY >= y && mouseY <= y + 32) {
                List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                tooltip.add(net.minecraft.network.chat.Component.translatable(
                        "hud.velorise_staves.aura_title",
                        net.minecraft.network.chat.Component.translatable(elem.translationKey())
                ).withStyle(elem.color()));
                if (!durationStr.isEmpty()) {
                    tooltip.add(net.minecraft.network.chat.Component.literal(durationStr).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                pendingTooltip = tooltip;
            }

            index++;
        }
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        if (pendingInventoryTooltip == null && pendingTooltip != null) {
            pendingInventoryTooltip = pendingTooltip;
        }
    }

    public static List<Rect2i> getInventoryExtraAreas(EffectRenderingInventoryScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !screen.canSeeEffects()) return Collections.emptyList();

        com.velorise.staves.mixin.AbstractContainerScreenAccessor accessor = (com.velorise.staves.mixin.AbstractContainerScreenAccessor) screen;
        int leftPos = accessor.velorise$getLeftPos();
        int topPos = accessor.velorise$getTopPos();
        int imageWidth = accessor.velorise$getImageWidth();

        int startX = leftPos + imageWidth + 2;
        int availableWidth = screen.width - startX;
        if (availableWidth < 32) return Collections.emptyList();

        Element shield = ClientAuraTracker.getActiveCrystallizeShield();
        if (shield == null) {
            shield = ModMobEffects.getActiveShield(mc.player);
        }

        int vanillaCount = 0;
        boolean shieldFoundInVanilla = false;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            vanillaCount++;
            if (ModMobEffects.getShieldElement(inst.getEffect().value()) != null) {
                shieldFoundInVanilla = true;
            }
        }

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        long now = System.currentTimeMillis();
        boolean hasAura = data != null && !data.elements.isEmpty() && (data.expireTime <= 0 || now < data.expireTime);
        int auraCount = hasAura ? data.elements.size() : 0;
        boolean customShield = shield != null && !shieldFoundInVanilla;

        int resonanceCount = getResonanceHudSlotCount();

        int totalEffects = vanillaCount + auraCount + (customShield ? 1 : 0) + resonanceCount;
        if (totalEffects == 0) return Collections.emptyList();

        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        List<Rect2i> areas = new ArrayList<>();

        if (customShield) {
            int y = topPos + vanillaCount * yStep;
            areas.add(new Rect2i(startX, y, 32, 32));
        }

        if (hasAura) {
            int baseIndex = vanillaCount + (customShield ? 1 : 0);
            for (int i = 0; i < data.elements.size(); i++) {
                int y = topPos + (baseIndex + i) * yStep;
                areas.add(new Rect2i(startX, y, 32, 32));
            }
        }

        if (resonanceCount > 0) {
            int resBase = vanillaCount + (customShield ? 1 : 0) + auraCount;
            for (int i = 0; i < resonanceCount; i++) {
                int y = topPos + (resBase + i) * yStep;
                areas.add(new Rect2i(startX, y, 32, 32));
            }
        }

        return areas;
    }

    public static int getElementBorderColor(Element element) {
        return switch (element) {
            case PYRO -> 0xFFFF4500;   // Flame Red-Orange
            case HYDRO -> 0xFF00C8FF;  // Aqua Cyan
            case ELECTRO -> 0xFFBF40FF;// Electric Violet
            case CRYO -> 0xFF4DF0FF;   // Frost Crystal Cyan
            case DENDRO -> 0xFF32CD32; // Emerald Green
            case ANEMO -> 0xFF40E0D0;  // Turquoise
            case GEO -> 0xFFFFD700;    // Amber Gold
        };
    }

    public static void renderColoredBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        int alpha = (color >> 24) & 0xFF;
        int glow = ((Math.max(40, alpha / 3)) << 24) | (color & 0x00FFFFFF);
        // Outer soft glow outline (1px expand)
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, glow);
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, glow);
        guiGraphics.fill(x - 1, y, x, y + height, glow);
        guiGraphics.fill(x + width, y, x + width + 1, y + height, glow);
        // Inner crisp colored border
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, color);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
        // Subtle colored wash inside box (10% opacity)
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, (0x1A << 24) | (color & 0x00FFFFFF));
    }

    public static void renderCrystallizeShieldHudFrame(GuiGraphics guiGraphics, int x, int y, int color) {
        // Inner gray border of 24x24 HUD box (leaves 1px outer black border intact!)
        RenderType overlay = RenderType.guiOverlay();
        guiGraphics.fill(overlay, x + 2, y + 1, x + 22, y + 2, color);
        guiGraphics.fill(overlay, x + 1, y + 2, x + 3, y + 3, color);
        guiGraphics.fill(overlay, x + 21, y + 2, x + 23, y + 3, color);

        guiGraphics.fill(overlay, x + 1, y + 3, x + 2, y + 21, color);
        guiGraphics.fill(overlay, x + 22, y + 3, x + 23, y + 21, color);

        guiGraphics.fill(overlay, x + 1, y + 21, x + 3, y + 22, color);
        guiGraphics.fill(overlay, x + 21, y + 21, x + 23, y + 22, color);
        guiGraphics.fill(overlay, x + 2, y + 22, x + 22, y + 23, color);
    }

    public static void renderCrystallizeShieldInventoryFrame(GuiGraphics guiGraphics, int x, int y, int color) {
        // Inner gray border of 32x32 Inventory box (leaves 1px outer black border intact!)
        guiGraphics.fill(x + 2, y + 1, x + 29, y + 2, color);
        guiGraphics.fill(x + 1, y + 2, x + 30, y + 3, color);
        guiGraphics.fill(x + 1, y + 3, x + 4, y + 4, color);
        guiGraphics.fill(x + 28, y + 3, x + 31, y + 4, color);

        guiGraphics.fill(x + 1, y + 4, x + 3, y + 28, color);
        guiGraphics.fill(x + 29, y + 4, x + 31, y + 28, color);

        guiGraphics.fill(x + 1, y + 28, x + 4, y + 29, color);
        guiGraphics.fill(x + 28, y + 28, x + 31, y + 29, color);
        guiGraphics.fill(x + 2, y + 29, x + 31, y + 30, color);
        guiGraphics.fill(x + 3, y + 30, x + 30, y + 31, color);
    }

    private static boolean isBeneficial(MobEffectInstance inst) {
        return inst.getEffect().value().isBeneficial();
    }

    private static void renderChargeProgressHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !mc.player.isUsingItem()) return;
        ItemStack stack = mc.player.getUseItem();
        if (!(stack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff) || !staff.isCharged()) return;
        int elapsed = Math.max(0, staff.getUseDuration(stack, mc.player) - mc.player.getUseItemRemainingTicks());
        int targetTicks = switch (staff.kind()) {
            case VORTEX -> 28;
            case SPLENDOR -> 10;
            case THOUSAND_BLAZING_SUNS -> 7;
            case GHASTFIRE -> 40;
            case LIGHTNING -> 60;
            case GUARDIAN -> com.velorise.staves.item.ArcaneStaffItem.isGuardianInstant(stack) ? 60 : (elapsed < 60 ? 60 : 120);
            default -> 30;
        };
        float pct = Mth.clamp((float) elapsed / (float) targetTicks, 0.0F, 1.0F);
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.GUARDIAN) {
            float cx = sw * 0.5F;
            float cy = sh * 0.5F;
            Matrix4f mat = guiGraphics.pose().last().pose();
            VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
            drawHudArc(vc, mat, cx, cy, 12.0F, 15.0F, 0.0F, 360.0F, 32, 10, 26, 48, 145);
            if (pct > 0.005F) {
                float halfDeg = 180.0F * pct;
                int segs = Math.max(2, Math.round(16 * pct));
                int r = pct >= 1.0F ? 175 : 85;
                int g = pct >= 1.0F ? 250 : 225;
                int b = 255;
                drawHudArc(vc, mat, cx, cy, 12.3F, 14.7F, 90.0F, 90.0F + halfDeg, segs, r, g, b, 240);
                drawHudArc(vc, mat, cx, cy, 12.3F, 14.7F, 90.0F - halfDeg, 90.0F, segs, r, g, b, 240);
            }
            guiGraphics.flush();
            return;
        }
        if (staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            ResourceLocation sunsTex = ResourceLocation.fromNamespaceAndPath(
                    com.velorise.staves.VeloriseStavesMod.MODID,
                    "textures/gui/sun_charge.png"
            );
            int iconX = sw / 2 - 8;
            int iconY = sh / 2 + 12;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(0.35F, 0.35F, 0.35F, 0.65F);
            guiGraphics.blit(sunsTex, iconX, iconY, 0, 0, 16, 16, 16, 16);
            int fillH = Math.max(1, Math.round(16 * pct));
            int topY = iconY + (16 - fillH);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(sunsTex, iconX, topY, 0, 16 - fillH, 16, fillH, 16, 16);
            return;
        }
        int barW = 56;
        int barH = 4;
        int x = (sw - barW) / 2;
        int y = sh / 2 + 12;
        guiGraphics.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0x90000000);
        com.velorise.staves.element.Element elem = staff.getElement();
        Integer rgb = elem != null ? elem.color().getColor() : null;
        int elemColor = (rgb != null ? rgb : 0xFFFFFF) | 0xFF000000;
        int fillColor = pct >= 1.0F ? 0xFFFFE066 : elemColor;
        int fillW = Math.round(barW * pct);
        if (fillW > 0) {
            guiGraphics.fill(x, y, x + fillW, y + barH, fillColor);
        }
    }

    private static void renderFreezeStruggleHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        if (data == null || !data.isFrozen) {
            wasJumpKeyDown = false;
            return;
        }
        long now = System.currentTimeMillis();
        if (data.expireTime > 0 && now >= data.expireTime) {
            data.isFrozen = false;
            wasJumpKeyDown = false;
            return;
        }

        boolean jumpDown = mc.screen == null && mc.options.keyJump.isDown();
        if (jumpDown && !wasJumpKeyDown && (now - lastStruggleSendTime) >= 75L) {
            lastStruggleSendTime = now;
            struggleShakeUntil = now + 150L;
            com.velorise.staves.network.ModNetwork.sendFreezeStruggle();
            if (data.expireTime > now + 150L) {
                data.expireTime = Math.max(now + 100L, data.expireTime - 350L);
            }
        }
        wasJumpKeyDown = jumpDown;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int centerX = sw / 2;
        int y = sh - 68;

        String prompt = net.minecraft.network.chat.Component.translatable("message.velorise_staves.freeze_struggle").getString();
        int textW = mc.font.width(prompt);
        int boxW = Math.max(88, textW + 14);
        int boxX = centerX - boxW / 2;

        boolean pressedFlash = (now - lastStruggleSendTime) < 90L;
        int bgColor = pressedFlash ? 0xCC1E4E68 : 0xAA0D2230;
        int borderColor = pressedFlash ? 0xFF99F6FF : 0xFF4DF0FF;

        guiGraphics.fill(boxX, y - 3, boxX + boxW, y + 18, bgColor);
        renderColoredBorder(guiGraphics, boxX, y - 3, boxW, 21, borderColor);

        int textColor = pressedFlash ? 0xFFFFFFFF : 0xFFB8F8FF;
        guiGraphics.drawString(mc.font, prompt, centerX - textW / 2, y, textColor, true);

        // Remaining freeze progress bar
        float progress = 1.0F;
        if (data.expireTime > now) {
            progress = Math.max(0.05F, Math.min(1.0F, (float) (data.expireTime - now) / 4000.0F));
        }
        int barW = boxW - 10;
        int barX = boxX + 5;
        int barY = y + 11;
        guiGraphics.fill(barX, barY, barX + barW, barY + 3, 0x88081520);
        int fillW = Math.max(2, Math.round(barW * progress));
        guiGraphics.fill(barX, barY, barX + fillW, barY + 3, 0xFF64D2FF);
    }

    private static void renderCrystallizeShieldHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        Element shield = ClientAuraTracker.getActiveCrystallizeShield();
        if (shield == null) {
            shield = ModMobEffects.getActiveShield(mc.player);
        }
        if (shield == null) return;

        int beneficialIndex = 0;
        boolean found = false;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (inst.showIcon() && isBeneficial(inst)) {
                if (ModMobEffects.getShieldElement(inst.getEffect().value()) != null) {
                    found = true;
                    break;
                }
                beneficialIndex++;
            }
        }

        int screenWidth = guiGraphics.guiWidth();
        int top = mc.isDemo() ? 16 : 1;
        int col = beneficialIndex % 10;
        int row = beneficialIndex / 10;
        int x = screenWidth - 25 * (col + 1);
        int y = top + 26 * row;

        long now = System.currentTimeMillis();
        long remMs = Math.max(0, ClientAuraTracker.crystallizeShieldUntil - now);
        float alpha = 1.0F;
        if (remMs > 0 && remMs <= 3000L) {
            alpha = Math.sin((remMs / 140.0D) * Math.PI) < 0 ? 0.35F : 1.0F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        if (!found) {
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SPRITE, x, y, 24, 24);
            ResourceLocation shieldTex = SHIELD_TEXTURES.get(shield);
            if (shieldTex != null) {
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                }
                guiGraphics.blit(shieldTex, x + 3, y + 3, 0.0F, 0.0F, 18, 18, 18, 18);
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
        RenderSystem.enableDepthTest();
    }

    private static List<net.minecraft.network.chat.Component> renderCrystallizeShieldInventory(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int leftPos,
            int topPos,
            int imageWidth,
            EffectRenderingInventoryScreen<?> screen,
            int mouseX,
            int mouseY
    ) {
        if (mc.player == null) return null;
        Element shield = ClientAuraTracker.getActiveCrystallizeShield();
        if (shield == null) {
            shield = ModMobEffects.getActiveShield(mc.player);
        }
        if (shield == null) return null;

        int startX = leftPos + imageWidth + 2;
        int availableWidth = screen.width - startX;
        if (availableWidth < 32) return null;

        int vanillaCount = 0;
        int effectIndex = 0;
        boolean found = false;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (!found) {
                if (ModMobEffects.getShieldElement(inst.getEffect().value()) != null) {
                    found = true;
                } else {
                    effectIndex++;
                }
            }
            vanillaCount++;
        }
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        long now = System.currentTimeMillis();
        boolean hasAura = data != null && !data.elements.isEmpty() && (data.expireTime <= 0 || now < data.expireTime);
        int auraCount = hasAura ? data.elements.size() : 0;
        int customShieldSlot = found ? 0 : 1;
        int totalEffects = vanillaCount + customShieldSlot + auraCount + getResonanceHudSlotCount();
        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        int y = topPos + effectIndex * yStep;
        if (!found) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SMALL_SPRITE, startX, y, 32, 32);
            guiGraphics.flush();
            ResourceLocation shieldTex = SHIELD_TEXTURES.get(shield);
            if (shieldTex != null) {
                guiGraphics.blit(shieldTex, startX + 7, y + 7, 0.0F, 0.0F, 18, 18, 18, 18);
                guiGraphics.flush();
            }
            RenderSystem.enableDepthTest();

            if (mouseX >= startX && mouseX <= startX + 32 && mouseY >= y && mouseY <= y + 32) {
                long remSec = Math.max(0, (ClientAuraTracker.crystallizeShieldUntil - now) / 1000L);
                String durationStr = String.format(Locale.ROOT, "%d:%02d", remSec / 60, remSec % 60);
                String key = "effect.velorise_staves." + shield.name().toLowerCase(Locale.ROOT) + "_shield";
                List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                tooltip.add(net.minecraft.network.chat.Component.translatable(key).withStyle(shield.color()));
                tooltip.add(net.minecraft.network.chat.Component.literal(durationStr).withStyle(net.minecraft.ChatFormatting.GRAY));
                return tooltip;
            }
        }
        return null;
    }

    private static List<ElementalResonanceManager.ResonanceType> getActiveResonancesForHud() {
        if (!StavesConfig.isElementalResonanceEnabled() || isLocalOverloadedForHud()) {
            return Collections.emptyList();
        }
        List<ElementalResonanceManager.ResonanceType> list = new ArrayList<>(2);
        for (ElementalResonanceManager.ResonanceType type : ElementalResonanceManager.ResonanceType.values()) {
            if (ClientAuraTracker.hasLocalResonance(type.ordinal())) {
                list.add(type);
            }
        }
        return list;
    }

    private static int getResonanceHudSlotCount() {
        if (!StavesConfig.isElementalResonanceEnabled()) return 0;
        if (isLocalOverloadedForHud()) return 1;
        return getActiveResonancesForHud().size();
    }

    private static int getResonanceBorderColor(ElementalResonanceManager.ResonanceType type) {
        return type.element() != null ? getElementBorderColor(type.element()) : 0xFFFFD60A;
    }

    private static boolean isShieldEffect(MobEffectInstance inst) {
        return ModMobEffects.getShieldElement(inst.getEffect().value()) != null;
    }

    private static void drawHudEffectBox(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blitSprite(EFFECT_BACKGROUND_SPRITE, x, y, 24, 24);
    }

    private static void drawInventoryEffectBox(GuiGraphics guiGraphics, int startX, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blitSprite(EFFECT_BACKGROUND_SMALL_SPRITE, startX, y, 32, 32);
    }

    private static void renderResonanceHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null || !StavesConfig.isElementalResonanceEnabled()) return;
        boolean overloaded = isLocalOverloadedForHud();
        List<ElementalResonanceManager.ResonanceType> resonances = overloaded ? Collections.emptyList() : getActiveResonancesForHud();
        if (!overloaded && resonances.isEmpty()) return;

        int beneficialStart = 0;
        boolean shieldFoundInVanilla = false;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (inst.showIcon() && isBeneficial(inst)) {
                beneficialStart++;
                if (isShieldEffect(inst)) {
                    shieldFoundInVanilla = true;
                }
            }
        }
        Element activeShield = ClientAuraTracker.getActiveCrystallizeShield();
        if (activeShield == null) {
            activeShield = ModMobEffects.getActiveShield(mc.player);
        }
        if (activeShield != null && !shieldFoundInVanilla) {
            beneficialStart++;
        }

        int screenWidth = guiGraphics.guiWidth();
        int top = mc.isDemo() ? 16 : 1;
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        if (overloaded) {
            int col = beneficialStart % 10;
            int row = beneficialStart / 10;
            int x = screenWidth - 25 * (col + 1);
            int y = top + 26 * row;
            drawHudEffectBox(guiGraphics, x, y);
            renderCrystallizeShieldHudFrame(guiGraphics, x, y, 0xFFFF3333);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.setColor(1.0F, 0.45F, 0.45F, 1.0F);
            guiGraphics.blit(getOmniIconTexture(), x + 4, y + 4, 0.0F, 0.0F, 16, 16, 16, 16);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.drawString(mc.font, "!", x + 17, y + 14, 0xFFFF4444, true);
        } else {
            for (int i = 0; i < resonances.size(); i++) {
                ElementalResonanceManager.ResonanceType type = resonances.get(i);
                int idx = beneficialStart + i;
                int col = idx % 10;
                int row = idx / 10;
                int x = screenWidth - 25 * (col + 1);
                int y = top + 26 * row;
                drawHudEffectBox(guiGraphics, x, y);
                renderCrystallizeShieldHudFrame(guiGraphics, x, y, getResonanceBorderColor(type));
                ResourceLocation tex = type.element() != null ? getElementIconTexture(type.element()) : getOmniIconTexture();
                if (tex != null) {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    guiGraphics.blit(tex, x + 4, y + 4, 0.0F, 0.0F, 16, 16, 16, 16);
                }
            }
        }
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static List<net.minecraft.network.chat.Component> renderResonanceInventory(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int leftPos,
            int topPos,
            int imageWidth,
            EffectRenderingInventoryScreen<?> screen,
            int mouseX,
            int mouseY
    ) {
        if (mc.player == null || !StavesConfig.isElementalResonanceEnabled()) return null;
        boolean overloaded = isLocalOverloadedForHud();
        List<ElementalResonanceManager.ResonanceType> resonances = overloaded ? Collections.emptyList() : getActiveResonancesForHud();
        if (!overloaded && resonances.isEmpty()) return null;

        int startX = leftPos + imageWidth + 2;
        int availableWidth = screen.width - startX;
        if (availableWidth < 32) return null;

        int vanillaCount = 0;
        boolean shieldFoundInVanilla = false;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            vanillaCount++;
            if (isShieldEffect(inst)) {
                shieldFoundInVanilla = true;
            }
        }
        Element activeShield = ClientAuraTracker.getActiveCrystallizeShield();
        if (activeShield == null) {
            activeShield = ModMobEffects.getActiveShield(mc.player);
        }
        int customShieldSlot = (activeShield != null && !shieldFoundInVanilla) ? 1 : 0;

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        long now = System.currentTimeMillis();
        boolean hasAura = data != null && !data.elements.isEmpty() && (data.expireTime <= 0 || now < data.expireTime);
        int auraCount = hasAura ? data.elements.size() : 0;
        int resonanceCount = overloaded ? 1 : resonances.size();

        int totalEffects = vanillaCount + customShieldSlot + auraCount + resonanceCount;
        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        int baseIndex = vanillaCount + customShieldSlot + auraCount;
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        List<net.minecraft.network.chat.Component> pendingTooltip = null;
        if (overloaded) {
            int y = topPos + baseIndex * yStep;
            drawInventoryEffectBox(guiGraphics, startX, y);
            renderCrystallizeShieldInventoryFrame(guiGraphics, startX, y, 0xFFFF3333);
            guiGraphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.setColor(1.0F, 0.45F, 0.45F, 1.0F);
            guiGraphics.blit(getOmniIconTexture(), startX + 8, y + 8, 0.0F, 0.0F, 16, 16, 16, 16);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.drawString(mc.font, "!", startX + 24, y + 20, 0xFFFF4444, true);

            if (mouseX >= startX && mouseX <= startX + 32 && mouseY >= y && mouseY <= y + 32) {
                List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                tooltip.add(net.minecraft.network.chat.Component.translatable(
                        "hud.velorise_staves.overload_title",
                        ClientAuraTracker.localStaffCount,
                        StavesConfig.getMaxStavesInInventory()).withStyle(net.minecraft.ChatFormatting.RED));
                com.velorise.staves.item.ArcaneStaffItem.appendWrappedTooltip(
                        tooltip,
                        net.minecraft.network.chat.Component.translatable(
                                "hud.velorise_staves.overload_hint",
                                StavesConfig.getMaxStavesInInventory()),
                        net.minecraft.ChatFormatting.GRAY, 38, "  ");
                pendingTooltip = tooltip;
            }
        } else {
            for (int i = 0; i < resonances.size(); i++) {
                ElementalResonanceManager.ResonanceType type = resonances.get(i);
                int y = topPos + (baseIndex + i) * yStep;
                drawInventoryEffectBox(guiGraphics, startX, y);
                renderCrystallizeShieldInventoryFrame(guiGraphics, startX, y, getResonanceBorderColor(type));
                guiGraphics.flush();
                ResourceLocation tex = type.element() != null ? getElementIconTexture(type.element()) : getOmniIconTexture();
                if (tex != null) {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    guiGraphics.blit(tex, startX + 8, y + 8, 0.0F, 0.0F, 16, 16, 16, 16);
                }

                if (mouseX >= startX && mouseX <= startX + 32 && mouseY >= y && mouseY <= y + 32) {
                    net.minecraft.ChatFormatting color = type.element() != null ? type.element().color() : net.minecraft.ChatFormatting.GOLD;
                    List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                    tooltip.add(net.minecraft.network.chat.Component.translatable(type.translationKey()).withStyle(color));
                    String rawDesc = net.minecraft.network.chat.Component.translatable(type.translationKey() + ".desc").getString();
                    for (String part : rawDesc.split("\\s*\\|\\s*")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) {
                            com.velorise.staves.item.ArcaneStaffItem.appendWrappedTooltip(
                                    tooltip,
                                    net.minecraft.network.chat.Component.literal("• " + trimmed),
                                    net.minecraft.ChatFormatting.GRAY, 38, "  ");
                        }
                    }
                    pendingTooltip = tooltip;
                }
            }
        }
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        return pendingTooltip;
    }

    public static void renderFloatingCombatTexts(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        if (poseStack == null || bufferSource == null || camera == null) return;
        if (ClientAuraTracker.FLOATING_TEXTS.isEmpty()) return;
        boolean showReactionText = true;
        boolean showDamageNumbers = true;
        try {
            showReactionText = StavesConfig.SHOW_REACTION_TEXT.get();
            showDamageNumbers = StavesConfig.SHOW_DAMAGE_NUMBERS.get();
        } catch (Exception ignored) {}
        if (!showReactionText && !showDamageNumbers) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        ClientAuraTracker.FLOATING_TEXTS.removeIf(t -> now >= t.until);
        if (ClientAuraTracker.FLOATING_TEXTS.isEmpty()) return;

        Vec3 camPos = camera.getPosition();
        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        Font font = mc.font;
        var camRot = mc.getEntityRenderDispatcher().cameraOrientation();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int pass = 0; pass < 2; pass++) {
            boolean seeThroughPass = (pass == 0);
            for (ClientAuraTracker.FloatingCombatText entry : ClientAuraTracker.FLOATING_TEXTS) {
                double distSqr = camPos.distanceToSqr(entry.worldX, entry.worldY, entry.worldZ);
                if (distSqr > 64.0D * 64.0D) continue;

                boolean hasReaction = showReactionText && entry.reaction != null && now < entry.reactionUntil;
                boolean hasDamage = showDamageNumbers && entry.damage > 0.0F && now < entry.damageUntil;
                if (!hasReaction && !hasDamage) continue;

                if (seeThroughPass) {
                    Vec3 textPos = new Vec3(entry.worldX, entry.worldY, entry.worldZ);
                    if (distSqr > 9.0D && mc.level.clip(new ClipContext(eyePos, textPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() != HitResult.Type.MISS) {
                        continue;
                    }
                }

                poseStack.pushPose();
                poseStack.translate(entry.worldX - camPos.x, entry.worldY - camPos.y, entry.worldZ - camPos.z);
                poseStack.mulPose(camRot);

                float textPop = 1.0F;
                long primaryAge = Math.max(0L, now - entry.startTime);
                if (primaryAge < 250L) {
                    textPop = 1.0F + 0.38F * (float) Math.sin((primaryAge / 250.0F) * Math.PI);
                }
                poseStack.scale(0.030F * textPop, -0.030F * textPop, 0.030F * textPop);
                Matrix4f matrix = poseStack.last().pose();

                float reactY = 0.0F;
                if (hasReaction) {
                    long reactionAge = Math.max(0L, now - entry.startTime);
                    long reactionDuration = Math.max(1L, entry.reactionUntil - entry.startTime);
                    float progress = Math.max(0.0F, Math.min(1.0F, (float) reactionAge / (float) reactionDuration));

                    reactY = -6.0F - (progress * 18.0F);
                    String reactionKey = entry.reaction.translationKey();
                    String reactionLabel = reactionKey.isEmpty()
                            ? entry.reaction.reactionName()
                            : net.minecraft.network.chat.Component.translatable(reactionKey).getString().toUpperCase(Locale.ROOT);
                    String text = "§l" + reactionLabel;
                    float textX = -font.width(text) / 2.0F;

                    int baseColor = getReactionColor(entry.reaction) & 0x00FFFFFF;
                    int alpha = 255;
                    if (progress > 0.7F) {
                        alpha = Math.max(15, Math.min(255, Math.round(((1.0F - progress) / 0.3F) * 255.0F)));
                    }
                    if (seeThroughPass) {
                        int seeThroughAlpha = Math.max(15, Math.round(alpha * 0.35F));
                        font.drawInBatch(text, textX, reactY, baseColor | (seeThroughAlpha << 24), false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                    } else {
                        font.drawInBatch(text, textX, reactY, baseColor | (alpha << 24), true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
                    }
                }

                if (hasDamage) {
                    long dmgAge = Math.max(0L, now - entry.startTime);
                    long dmgDuration = Math.max(1L, entry.damageUntil - entry.startTime);
                    float dmgProgress = Math.max(0.0F, Math.min(1.0F, (float) dmgAge / (float) dmgDuration));

                    float dmgY = hasReaction ? (reactY + 11.0F) : (-6.0F - (dmgProgress * 18.0F));
                    String dmgText = "§l" + (entry.damage % 1.0F == 0.0F ? String.valueOf((int) entry.damage) : String.format(Locale.ROOT, "%.1f", entry.damage));
                    float dmgX = -font.width(dmgText) / 2.0F;

                    int dmgBaseColor = entry.damageElement != null ? getElementTextColor(entry.damageElement) : 0xFFFFFF;
                    int dmgAlpha = 255;
                    if (dmgProgress > 0.7F) {
                        dmgAlpha = Math.max(15, Math.min(255, Math.round(((1.0F - dmgProgress) / 0.3F) * 255.0F)));
                    }
                    if (seeThroughPass) {
                        int seeThroughAlpha = Math.max(15, Math.round(dmgAlpha * 0.35F));
                        font.drawInBatch(dmgText, dmgX, dmgY, (dmgBaseColor & 0x00FFFFFF) | (seeThroughAlpha << 24), false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                    } else {
                        font.drawInBatch(dmgText, dmgX, dmgY, (dmgBaseColor & 0x00FFFFFF) | (dmgAlpha << 24), true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
                    }
                }

                poseStack.popPose();
            }
            if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch();
            }
        }
    }

    private static void renderGuardianHydroBeams(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 camPos = camera.getPosition();
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = null;
        double timeSec = System.currentTimeMillis() * 0.001D;

        for (net.minecraft.world.entity.player.Player player : mc.level.players()) {
            if (!player.isUsingItem()) continue;
            ItemStack stack = player.getUseItem();
            if (!(stack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff)) continue;
            if (staff.kind() != com.velorise.staves.item.ArcaneStaffItem.Kind.GUARDIAN) continue;

            int elapsed = staff.getUseDuration(stack, player) - player.getUseItemRemainingTicks();
            boolean instant = com.velorise.staves.item.ArcaneStaffItem.isGuardianInstant(stack);
            if (!instant && elapsed < 60) continue;
            if (elapsed < 2) continue;

            if (consumer == null) {
                consumer = bufferSource.getBuffer(RenderType.lightning());
            }

            Vec3 eye = player.getEyePosition(partialTicks);
            Vec3 look = player.getViewVector(partialTicks).normalize();
            Vec3 worldUp = Math.abs(look.y) < 0.96D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 right = look.cross(worldUp).normalize();
            Vec3 up = right.cross(look).normalize();

            double handSign = (player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1.0D : -1.0D)
                    * (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0D : -1.0D);
            Vec3 nozzle = eye.add(look.scale(1.05D)).add(right.scale(handSign * 0.22D)).add(up.scale(-0.16D));

            // Compact converging Hydro seal ring during wind-up and active beam
            double ringRadius = elapsed < 10 ? Mth.lerp(elapsed / 10.0F, 0.34F, 0.19F) : 0.19D;
            int ringSegs = 16;
            for (int i = 0; i < ringSegs; i++) {
                double a1 = (Math.PI * 2.0D * i) / ringSegs + timeSec * 2.4D;
                double a2 = (Math.PI * 2.0D * (i + 1)) / ringSegs + timeSec * 2.4D;
                Vec3 p1 = nozzle.add(right.scale(Math.cos(a1) * ringRadius)).add(up.scale(Math.sin(a1) * ringRadius)).subtract(camPos);
                Vec3 p2 = nozzle.add(right.scale(Math.cos(a2) * ringRadius)).add(up.scale(Math.sin(a2) * ringRadius)).subtract(camPos);
                drawQuadRibbon(consumer, matrix, p1, p2, look, 0.015D, 90, 220, 255, 195);
            }

            if (elapsed < 10) continue;

            double range = staff.isNetherite() ? 28.0D : 24.0D;
            net.minecraft.world.phys.HitResult hit = player.pick(range, partialTicks, false);
            Vec3 end = hit.getLocation();

            Vec3 a = nozzle.subtract(camPos);
            Vec3 b = end.subtract(camPos);
            Vec3 beamDir = b.subtract(a);
            double beamLen = beamDir.length();
            if (beamLen < 0.25D) continue;

            Vec3 normal = beamDir.cross(a.scale(-1.0D));
            if (normal.lengthSqr() < 1.0E-6D) normal = right;
            else normal = normal.normalize();

            // Sleek Neuvillette-style compact Hydro stream: translucent outer stream + bright core
            drawQuadRibbon(consumer, matrix, a, b, normal, 0.075D, 45, 180, 255, 165);
            drawQuadRibbon(consumer, matrix, a, b, normal, 0.028D, 210, 248, 255, 240);

            // Subtle spiraling water ribbon around the stream
            Vec3 unitDir = beamDir.scale(1.0D / beamLen);
            int spiralSegs = Math.max(8, Math.min(36, (int) (beamLen * 1.3D)));
            Vec3 prevSpiral = null;
            for (int i = 0; i <= spiralSegs; i++) {
                double dist = (beamLen * i) / spiralSegs;
                double ang = dist * 1.7D - timeSec * 15.0D;
                Vec3 spiralPt = a.add(unitDir.scale(dist))
                        .add(right.scale(Math.cos(ang) * 0.105D))
                        .add(up.scale(Math.sin(ang) * 0.105D));
                if (prevSpiral != null) {
                    Vec3 sNorm = spiralPt.subtract(prevSpiral).cross(prevSpiral.scale(-1.0D));
                    if (sNorm.lengthSqr() < 1.0E-6D) sNorm = normal;
                    else sNorm = sNorm.normalize();
                    drawQuadRibbon(consumer, matrix, prevSpiral, spiralPt, sNorm, 0.014D, 120, 235, 255, 200);
                }
                prevSpiral = spiralPt;
            }
        }

        if (consumer != null && bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }

    public static void renderActiveElectricArcs(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        renderGuardianHydroBeams(poseStack, bufferSource, camera);
        if (ClientAuraTracker.ACTIVE_ARCS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = System.currentTimeMillis();
        ClientAuraTracker.ACTIVE_ARCS.removeIf(arc -> now >= arc.expireTime);
        if (ClientAuraTracker.ACTIVE_ARCS.isEmpty()) return;

        Vec3 camPos = camera.getPosition();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (ClientAuraTracker.ActiveArc arc : ClientAuraTracker.ACTIVE_ARCS) {
            Vec3 startPos = null;
            if (arc.fromEntityId >= 0) {
                net.minecraft.world.entity.Entity from = mc.level.getEntity(arc.fromEntityId);
                if (from != null) {
                    startPos = from.position().add(0, from.getBbHeight() * 0.45D, 0);
                }
            }
            if (startPos == null) startPos = arc.start;

            Vec3 endPos = null;
            if (arc.toEntityId >= 0) {
                net.minecraft.world.entity.Entity to = mc.level.getEntity(arc.toEntityId);
                if (to != null) {
                    endPos = to.position().add(0, to.getBbHeight() * 0.45D, 0);
                }
            }
            if (endPos == null) endPos = arc.end;

            if (startPos == null || endPos == null) continue;
            if (arc.color == 0xFFB84DFF || arc.color == 0xFFE0B0FF) {
                boolean isMusou = (arc.color == 0xFFE0B0FF);
                float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
                float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
                float fade = (prog < 0.25F) ? (prog / 0.25F) : (1.0F - (prog - 0.25F) / 0.75F);
                int alpha = Mth.clamp((int) (255.0F * fade), 10, 255);
                Vec3 sCam = startPos.subtract(camPos);
                Vec3 eCam = endPos.subtract(camPos);
                Vec3 slashDir = eCam.subtract(sCam);
                if (slashDir.lengthSqr() > 1.0E-5D) {
                    Vec3 unit = slashDir.normalize();
                    Vec3 toCam = sCam.add(eCam).scale(-0.5D);
                    Vec3 perp = unit.cross(toCam.lengthSqr() > 1.0E-4D ? toCam.normalize() : new Vec3(0.0D, 1.0D, 0.0D));
                    if (perp.lengthSqr() < 1.0E-4D) perp = unit.cross(new Vec3(1.0D, 0.0D, 0.0D));
                    perp = perp.normalize();
                    double outerHalf = (isMusou ? 0.24D : 0.13D) * (1.05D - prog * 0.35D);
                    double innerHalf = outerHalf * 0.34D;
                    Vec3 midCam = sCam.lerp(eCam, 0.5D);
                    Vec3 pOut = perp.scale(outerHalf);
                    Vec3 pIn = perp.scale(innerHalf);
                    emitCrystalQuad(consumer, matrix,
                            (float) sCam.x, (float) sCam.y, (float) sCam.z, 178, 72, 255, alpha / 2,
                            (float) (midCam.x + pOut.x), (float) (midCam.y + pOut.y), (float) (midCam.z + pOut.z), 192, 92, 255, alpha,
                            (float) eCam.x, (float) eCam.y, (float) eCam.z, 178, 72, 255, alpha / 2,
                            (float) (midCam.x - pOut.x), (float) (midCam.y - pOut.y), (float) (midCam.z - pOut.z), 192, 92, 255, alpha);
                    emitCrystalQuad(consumer, matrix,
                            (float) sCam.x, (float) sCam.y, (float) sCam.z, 245, 225, 255, alpha,
                            (float) (midCam.x + pIn.x), (float) (midCam.y + pIn.y), (float) (midCam.z + pIn.z), 255, 248, 255, alpha,
                            (float) eCam.x, (float) eCam.y, (float) eCam.z, 245, 225, 255, alpha,
                            (float) (midCam.x - pIn.x), (float) (midCam.y - pIn.y), (float) (midCam.z - pIn.z), 255, 248, 255, alpha);
                }
                continue;
            }
            if (arc.color == 0xFF7BE8FF) {
                float totalMs = Math.max(1.0F, (float) (arc.expireTime - arc.startTime));
                float prog = Mth.clamp((now - arc.startTime) / totalMs, 0.0F, 1.0F);
                Vec3 arcHeight = new Vec3(0.0D, Math.sin(prog * Math.PI) * 1.15D, 0.0D);
                Vec3 headPos = startPos.lerp(endPos, prog).add(arcHeight);
                float tailProg = Math.max(0.0F, prog - 0.32F);
                Vec3 tailPos = startPos.lerp(endPos, tailProg).add(0.0D, Math.sin(tailProg * Math.PI) * 1.15D, 0.0D);

                poseStack.pushPose();
                poseStack.translate(headPos.x - camPos.x, headPos.y - camPos.y, headPos.z - camPos.z);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(prog * 720.0F));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(prog * 360.0F));
                renderFourPointStar3D(consumer, poseStack.last().pose(), 0.22F, 0.07F, 240, 252, 255, 255, 110, 210, 255, 130);
                poseStack.popPose();

                Vec3 hCam = headPos.subtract(camPos);
                Vec3 tCam = tailPos.subtract(camPos);
                Vec3 dir = hCam.subtract(tCam);
                if (dir.lengthSqr() > 1.0E-5D) {
                    Vec3 side = dir.normalize().cross(new Vec3(0.0D, 1.0D, 0.0D));
                    if (side.lengthSqr() < 1.0E-4D) side = new Vec3(1.0D, 0.0D, 0.0D);
                    side = side.normalize().scale(0.045D);
                    emitCrystalQuad(consumer, matrix,
                            (float) (tCam.x - side.x), (float) (tCam.y - side.y), (float) (tCam.z - side.z), 110, 175, 255, 20,
                            (float) (tCam.x + side.x), (float) (tCam.y + side.y), (float) (tCam.z + side.z), 110, 175, 255, 20,
                            (float) (hCam.x + side.x * 1.8D), (float) (hCam.y + side.y * 1.8D), (float) (hCam.z + side.z * 1.8D), 220, 248, 255, 220,
                            (float) (hCam.x - side.x * 1.8D), (float) (hCam.y - side.y * 1.8D), (float) (hCam.z - side.z * 1.8D), 220, 248, 255, 220);
                }
                continue;
            }
            Vec3 diff = endPos.subtract(startPos);
            double dist = diff.length();
            if (dist < 0.05D) continue;

            if (arc.points.isEmpty() || (now - arc.lastJitterTime) >= 70L) {
                arc.points.clear();
                int segs = Math.max(3, (int) Math.round(dist * 1.5D));
                Vec3 dir = diff.normalize();
                Vec3 arbitrary = Math.abs(dir.y) < 0.9D ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                Vec3 u = dir.cross(arbitrary).normalize();
                Vec3 v = dir.cross(u).normalize();

                arc.points.add(new org.joml.Vector3f((float) startPos.x, (float) startPos.y, (float) startPos.z));
                for (int i = 1; i < segs; i++) {
                    double t = (double) i / (double) segs;
                    double maxJitter = Math.min(0.18D, dist * 0.05D);
                    double jx = (Math.random() - 0.5D) * 2.0D * maxJitter;
                    double jy = (Math.random() - 0.5D) * 2.0D * maxJitter;
                    Vec3 pt = startPos.add(diff.scale(t)).add(u.scale(jx)).add(v.scale(jy));
                    arc.points.add(new org.joml.Vector3f((float) pt.x, (float) pt.y, (float) pt.z));
                }
                arc.points.add(new org.joml.Vector3f((float) endPos.x, (float) endPos.y, (float) endPos.z));
                arc.lastJitterTime = now;
            }

            int ptsCount = arc.points.size();
            for (int i = 0; i < ptsCount - 1; i++) {
                org.joml.Vector3f p1 = arc.points.get(i);
                org.joml.Vector3f p2 = arc.points.get(i + 1);

                Vec3 a = new Vec3(p1.x - camPos.x, p1.y - camPos.y, p1.z - camPos.z);
                Vec3 b = new Vec3(p2.x - camPos.x, p2.y - camPos.y, p2.z - camPos.z);

                Vec3 segDir = b.subtract(a);
                Vec3 toCam = a.scale(-1.0D);
                Vec3 normal = segDir.cross(toCam);
                if (normal.lengthSqr() < 1e-6D) {
                    normal = new Vec3(0, 1, 0).cross(segDir);
                    if (normal.lengthSqr() < 1e-6D) normal = new Vec3(1, 0, 0);
                }
                normal = normal.normalize();

                int c = arc.color;
                int r = (c == 0 || c == 0xBF40FF) ? 185 : ((c >> 16) & 0xFF);
                int g = (c == 0 || c == 0xBF40FF) ? 90 : ((c >> 8) & 0xFF);
                int bCol = (c == 0 || c == 0xBF40FF) ? 255 : (c & 0xFF);

                // Pass 1: Outer elemental lightning ribbon (~4cm wide)
                drawQuadRibbon(consumer, matrix, a, b, normal, 0.02D, r, g, bCol, 230);
                // Pass 2: Ultra-thin inner white-hot core (~1.5cm wide)
                drawQuadRibbon(consumer, matrix, a, b, normal, 0.007D, 255, 255, 255, 255);
            }
        }
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }

    private static void drawQuadRibbon(
            VertexConsumer consumer, Matrix4f matrix,
            Vec3 a, Vec3 b, Vec3 normal, double halfWidth,
            int r, int g, int bCol, int alpha
    ) {
        Vec3 off = normal.scale(halfWidth);
        float x1 = (float) (a.x - off.x);
        float y1 = (float) (a.y - off.y);
        float z1 = (float) (a.z - off.z);

        float x2 = (float) (a.x + off.x);
        float y2 = (float) (a.y + off.y);
        float z2 = (float) (a.z + off.z);

        float x3 = (float) (b.x + off.x);
        float y3 = (float) (b.y + off.y);
        float z3 = (float) (b.z + off.z);

        float x4 = (float) (b.x - off.x);
        float y4 = (float) (b.y - off.y);
        float z4 = (float) (b.z - off.z);

        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, bCol, alpha);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, bCol, alpha);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, bCol, alpha);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, bCol, alpha);

        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, bCol, alpha);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, bCol, alpha);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, bCol, alpha);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, bCol, alpha);
    }

    public static Element getCrystallizeShardElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String desc = stack.getItem().getDescriptionId();
        if (desc == null) return null;
        if (desc.contains("pyro_crystallize_shard")) return Element.PYRO;
        if (desc.contains("hydro_crystallize_shard")) return Element.HYDRO;
        if (desc.contains("electro_crystallize_shard")) return Element.ELECTRO;
        if (desc.contains("cryo_crystallize_shard")) return Element.CRYO;
        return null;
    }

    private static int[] getCrystallizeColors(Element element) {
        return switch (element) {
            case PYRO -> new int[]{255, 95, 30, 255, 190, 140};
            case HYDRO -> new int[]{35, 175, 255, 170, 230, 255};
            case ELECTRO -> new int[]{195, 80, 255, 235, 185, 255};
            case CRYO -> new int[]{75, 235, 255, 195, 250, 255};
            default -> new int[]{255, 255, 255, 255, 255, 255};
        };
    }

    public static void renderCrystallizeShards(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = camera.getPosition();
        AABB searchBox = new AABB(
                camPos.x - 48.0D, camPos.y - 32.0D, camPos.z - 48.0D,
                camPos.x + 48.0D, camPos.y + 32.0D, camPos.z + 48.0D
        );

        List<ItemEntity> shards = mc.level.getEntitiesOfClass(ItemEntity.class, searchBox, item ->
                item.isAlive() && getCrystallizeShardElement(item.getItem()) != null
        );

        if (shards.isEmpty()) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        for (ItemEntity item : shards) {
            Element elem = getCrystallizeShardElement(item.getItem());
            if (elem == null) continue;

            int[] cols = getCrystallizeColors(elem);
            int r = cols[0], g = cols[1], b = cols[2];
            int hr = cols[3], hg = cols[4], hb = cols[5];

            float age = (float) item.tickCount;
            float bob = (float) Math.sin(age * 0.08F) * 0.035F;

            double cx = item.getX();
            double cy = item.getY() + 0.38D + bob;
            double cz = item.getZ();

            double dx = camPos.x - cx;
            double dy = camPos.y - cy;
            double dz = camPos.z - cz;
            double dLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float vx = dLen > 1.0E-4D ? (float) (dx / dLen) : 0.0F;
            float vy = dLen > 1.0E-4D ? (float) (dy / dLen) : 0.0F;
            float vz = dLen > 1.0E-4D ? (float) (dz / dLen) : 1.0F;

            poseStack.pushPose();
            poseStack.translate(cx - camPos.x, cy - camPos.y, cz - camPos.z);

            // 1. Pixel-faceted Translucent 3D Octagonal Halo Shell
            renderSmoothAuraSphere(consumer, poseStack.last().pose(), vx, vy, vz, 0.34F, 6, 8, r, g, b, hr, hg, hb);

            // 2. Central Faceted Pixel-Style Spinning Crystal Prism Core
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(age * 2.2F)));
            renderSmoothCrystalCore(consumer, poseStack.last().pose(), 0.16F, 0.095F, r, g, b, hr, hg, hb);
            poseStack.popPose();

            poseStack.popPose();
        }

        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }

    private static void renderSmoothAuraSphere(
            VertexConsumer consumer, Matrix4f matrix,
            float vx, float vy, float vz,
            float radius, int latBands, int lonSegs,
            int r, int g, int b,
            int hr, int hg, int hb
    ) {
        // Outer low-poly faceted halo shell
        renderFresnelSpherePass(consumer, matrix, vx, vy, vz, radius, latBands, lonSegs, r, g, b, hr, hg, hb, false);
        // Inner stepped core mist shell
        renderFresnelSpherePass(consumer, matrix, vx, vy, vz, radius * 0.72F, 4, 6, hr, hg, hb, 255, 255, 255, true);
    }

    private static float quantizeBand(float v, int steps) {
        if (steps <= 1) return v;
        return Math.round(Math.max(0.0F, Math.min(1.0F, v)) * (steps - 1)) / (float) (steps - 1);
    }

    private static void renderFresnelSpherePass(
            VertexConsumer consumer, Matrix4f matrix,
            float vx, float vy, float vz,
            float radius, int latBands, int lonSegs,
            int r, int g, int b,
            int hr, int hg, int hb,
            boolean innerMist
    ) {
        int rows = latBands + 1;
        int cols = lonSegs + 1;
        float[] px = new float[rows * cols];
        float[] py = new float[rows * cols];
        float[] pz = new float[rows * cols];
        float[] vnx = new float[rows * cols];
        float[] vny = new float[rows * cols];
        float[] vnz = new float[rows * cols];

        for (int i = 0; i <= latBands; i++) {
            double phi = -Math.PI * 0.5D + Math.PI * (double) i / (double) latBands;
            float sinPhi = (float) Math.sin(phi);
            float cosPhi = (float) Math.cos(phi);
            for (int j = 0; j <= lonSegs; j++) {
                double theta = 2.0D * Math.PI * (double) j / (double) lonSegs;
                float nx = cosPhi * (float) Math.cos(theta);
                float ny = sinPhi;
                float nz = cosPhi * (float) Math.sin(theta);

                int idx = i * cols + j;
                px[idx] = radius * nx;
                py[idx] = radius * ny;
                pz[idx] = radius * nz;
                vnx[idx] = nx;
                vny[idx] = ny;
                vnz[idx] = nz;
            }
        }

        for (int i = 0; i < latBands; i++) {
            for (int j = 0; j < lonSegs; j++) {
                int i00 = i * cols + j;
                int i10 = (i + 1) * cols + j;
                int i11 = (i + 1) * cols + (j + 1);
                int i01 = i * cols + (j + 1);

                float fnx = (vnx[i00] + vnx[i10] + vnx[i11] + vnx[i01]) * 0.25F;
                float fny = (vny[i00] + vny[i10] + vny[i11] + vny[i01]) * 0.25F;
                float fnz = (vnz[i00] + vnz[i10] + vnz[i11] + vnz[i01]) * 0.25F;
                float nLen = (float) Math.sqrt(fnx * fnx + fny * fny + fnz * fnz);
                if (nLen > 1.0E-5F) {
                    fnx /= nLen;
                    fny /= nLen;
                    fnz /= nLen;
                }

                float ndotv = Math.abs(fnx * vx + fny * vy + fnz * vz);
                float edgeFeather = Math.min(1.0F, ndotv * 3.4F);
                int fr, fg, fb, fa;
                if (innerMist) {
                    float haze = quantizeBand(0.70F * ndotv + 0.30F * edgeFeather, 4);
                    fr = r;
                    fg = g;
                    fb = b;
                    fa = clampColor((int) (24.0F * haze));
                } else {
                    float fresnel = quantizeBand((float) Math.pow(1.0F - ndotv * 0.72F, 1.35D) * edgeFeather, 4);
                    float rimT = quantizeBand(1.0F - ndotv, 4);
                    fr = clampColor((int) (r + (hr - r) * rimT * 0.45F));
                    fg = clampColor((int) (g + (hg - g) * rimT * 0.45F));
                    fb = clampColor((int) (b + (hb - b) * rimT * 0.45F));
                    fa = clampColor((int) (58.0F * fresnel));
                }

                emitCrystalQuad(consumer, matrix,
                        px[i00], py[i00], pz[i00], fr, fg, fb, fa,
                        px[i10], py[i10], pz[i10], fr, fg, fb, fa,
                        px[i11], py[i11], pz[i11], fr, fg, fb, fa,
                        px[i01], py[i01], pz[i01], fr, fg, fb, fa);
            }
        }
    }

    private static void renderSmoothCrystalCore(
            VertexConsumer consumer, Matrix4f matrix,
            float halfH, float eqRadius,
            int r, int g, int b,
            int hr, int hg, int hb
    ) {
        renderCrystalSpindle(consumer, matrix, halfH, eqRadius, 4, 6, r, g, b, hr, hg, hb, 238);
        renderCrystalSpindle(consumer, matrix, halfH * 0.56F, eqRadius * 0.52F, 2, 4, hr, hg, hb, 255, 255, 255, 205);
    }

    private static void renderCrystalSpindle(
            VertexConsumer consumer, Matrix4f matrix,
            float halfH, float eqRadius, int latBands, int lonSegs,
            int r, int g, int b,
            int hr, int hg, int hb,
            int baseAlpha
    ) {
        int rows = latBands + 1;
        int cols = lonSegs + 1;
        float[] px = new float[rows * cols];
        float[] py = new float[rows * cols];
        float[] pz = new float[rows * cols];

        for (int i = 0; i <= latBands; i++) {
            double alphaV = -Math.PI * 0.5D + Math.PI * (double) i / (double) latBands;
            double u = Math.sin(alphaV);
            double cosAlpha = Math.cos(alphaV);
            double ringBase = Math.pow(Math.max(0.0D, cosAlpha), 0.78D);
            float y = (float) (halfH * u);

            for (int j = 0; j <= lonSegs; j++) {
                double theta = 2.0D * Math.PI * (double) j / (double) lonSegs;
                int idx = i * cols + j;
                px[idx] = (float) (eqRadius * ringBase * Math.cos(theta));
                py[idx] = y;
                pz[idx] = (float) (eqRadius * ringBase * Math.sin(theta));
            }
        }

        final float lx = 0.36F, ly = 0.82F, lz = 0.44F;
        for (int i = 0; i < latBands; i++) {
            for (int j = 0; j < lonSegs; j++) {
                int i00 = i * cols + j;
                int i10 = (i + 1) * cols + j;
                int i11 = (i + 1) * cols + (j + 1);
                int i01 = i * cols + (j + 1);

                // Flat-shaded crystal facet normal for crisp Minecraft pixel/gem aesthetic
                float e1x = px[i11] - px[i00];
                float e1y = py[i11] - py[i00];
                float e1z = pz[i11] - pz[i00];
                float e2x = px[i10] - px[i01];
                float e2y = py[i10] - py[i01];
                float e2z = pz[i10] - pz[i01];
                float nx = e1y * e2z - e1z * e2y;
                float ny = e1z * e2x - e1x * e2z;
                float nz = e1x * e2y - e1y * e2x;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 1.0E-5F) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                } else {
                    nx = 0.0F;
                    ny = (i < latBands / 2) ? -1.0F : 1.0F;
                    nz = 0.0F;
                }
                if (px[i00] * nx + py[i00] * ny + pz[i00] * nz < 0.0F) {
                    nx = -nx;
                    ny = -ny;
                    nz = -nz;
                }

                float dot = Math.max(0.0F, nx * lx + ny * ly + nz * lz);
                float fillDot = Math.max(0.0F, -(nx * 0.30F + ny * 0.40F + nz * 0.70F));
                float shade = 0.56F + 0.44F * quantizeBand(dot, 4) + 0.08F * quantizeBand(fillDot, 3);
                float spec = quantizeBand((float) Math.pow(dot, 2.2D) * 0.65F, 4);

                float br = r * shade;
                float bg = g * shade;
                float bb = b * shade;
                int fr = clampColor((int) (br + (hr - br) * spec));
                int fg = clampColor((int) (bg + (hg - bg) * spec));
                int fb = clampColor((int) (bb + (hb - bb) * spec));

                emitCrystalQuad(consumer, matrix,
                        px[i00], py[i00], pz[i00], fr, fg, fb, baseAlpha,
                        px[i10], py[i10], pz[i10], fr, fg, fb, baseAlpha,
                        px[i11], py[i11], pz[i11], fr, fg, fb, baseAlpha,
                        px[i01], py[i01], pz[i01], fr, fg, fb, baseAlpha);
            }
        }
    }

    private static int clampColor(int c) {
        return Math.max(0, Math.min(255, c));
    }

    private static void emitCrystalQuad(
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

    public static void renderMavuikaSunAuraRing(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (!ClientAuraTracker.isMavuikaAuraActive(entity.getId())) return;
        if (entity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        ClientAuraTracker.MavuikaDisplayState state = ClientAuraTracker.getMavuika(entity.getId());
        boolean netherite = state != null && state.netherite;
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);

        ResourceLocation sunTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/mavuika_sun_aura.png");
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(sunTex));
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.72F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        poseStack.translate(0.0F, 0.0F, -0.46F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 1.6F));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        float pulse = 1.0F + 0.03F * Mth.sin(time * 0.22F);
        float half = 0.88F * pulse;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        int cr = 255, cg = netherite ? 255 : 225, cb = netherite ? 255 : 195, ca = 240;

        // Front face
        vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
        vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
        vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
        vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);

        // Back face
        vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
        vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
        vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
        vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);

        poseStack.popPose();
    }

    private static void renderMavuikaNightsoulHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        boolean holdingSuns = (mc.player.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem sm
                && sm.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                || (mc.player.getOffhandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem so
                && so.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                || (mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity);
        if (!holdingSuns) return;
        ClientAuraTracker.MavuikaDisplayState state = ClientAuraTracker.getMavuika(mc.player.getId());
        if (state == null) return;
        float auraTicks = state.effectiveAuraTicks();
        float points = state.nightsoulPoints;
        if (auraTicks <= 0.0F && points <= 0.001F) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        float cx = sw * 0.5F - 38.0F;
        float cy = sh * 0.5F;

        // Curved Natlan crescent sweeping along left side of crosshair (125 deg -> 235 deg)
        float startDeg = 128.0F;
        float endDeg = 232.0F;
        int segments = 24;

        Matrix4f mat = guiGraphics.pose().last().pose();
        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        float ultiCdTicks = state.effectiveUltiCooldownTicks();
        boolean cdReady = ultiCdTicks <= 0.0F;
        boolean qReady = points >= 5.0F && cdReady;

        // 1. Background curved track for Nightsoul (inner arc)
        drawHudArc(vc, mat, cx, cy, 29.0F, 35.0F, startDeg, endDeg, segments, 18, 12, 16, qReady ? 195 : 135);

        // 2. Filled Nightsoul arc (0..10 points)
        float nsFrac = Mth.clamp(points / 10.0F, 0.0F, 1.0F);
        if (nsFrac > 0.005F) {
            float fillEnd = Mth.lerp(nsFrac, startDeg, endDeg);
            float pulse = qReady ? (0.85F + 0.15F * Mth.sin(System.currentTimeMillis() * 0.01F)) : 1.0F;
            int r = qReady ? 255 : (cdReady ? 175 : 120);
            int g = qReady ? Math.round(215 * pulse) : (cdReady ? 75 : 48);
            int b = qReady ? Math.round(70 * pulse) : (cdReady ? 22 : 18);
            int a = qReady ? 250 : 175;
            drawHudArc(vc, mat, cx, cy, 29.5F, 34.5F, startDeg, fillEnd, Math.max(2, Math.round(segments * nsFrac)), r, g, b, a);
        }

        // 3. 5.0-point Ulti unlock notch at midpoint
        float midDeg = (startDeg + endDeg) * 0.5F;
        drawHudArc(vc, mat, cx, cy, 27.5F, 36.5F, midDeg - 1.8F, midDeg + 1.8F, 1,
                qReady ? 255 : 150, qReady ? 245 : 135, qReady ? 190 : 105, qReady ? 245 : 155);

        // Shared Nightsoul's Blessing curved red bar (80 max C0 / 120 max C1 Netherite, 10 pts per segment)
        if (auraTicks > 0.0F) {
            float fuelFrac = Mth.clamp(state.nightsoulFuel() / state.maxNightsoulFuel(), 0.0F, 1.0F);
            int fuelTicks = state.netherite ? 12 : 8;
            drawHudArc(vc, mat, cx, cy, 35.0F, 42.5F, startDeg, endDeg, segments, 24, 10, 10, 210);
            if (fuelFrac > 0.005F) {
                int fillSegs = Math.max(2, Math.round(segments * fuelFrac));
                for (int s = 0; s < fillSegs; s++) {
                    float f0 = (float) s / segments;
                    float f1 = Math.min(fuelFrac, (float) (s + 1) / segments);
                    if (f1 <= f0) break;
                    float d0 = Mth.lerp(f0, startDeg, endDeg);
                    float d1 = Mth.lerp(f1, startDeg, endDeg);
                    float t = (f0 + f1) * 0.5F;
                    int r = 240 + Math.round(15.0F * t);
                    int g = 38 + Math.round(135.0F * t);
                    int b = 16 + Math.round(28.0F * t);
                    drawHudArc(vc, mat, cx, cy, 35.8F, 41.7F, d0, d1, 1, r, g, b, 245);
                }
            }
            for (int tick = 1; tick < fuelTicks; tick++) {
                float tickDeg = Mth.lerp(tick / (float) fuelTicks, startDeg, endDeg);
                drawHudArc(vc, mat, cx, cy, 34.8F, 42.7F, tickDeg - 1.3F, tickDeg + 1.3F, 1, 10, 6, 6, 255);
            }
        }

        guiGraphics.flush();

        // Compact numeric / [R] Ulti cue beside the crescent
        Font font = mc.font;
        int labelX = Math.round(cx - 46);
        int labelY = Math.round(cy - 4);
        String valText = String.format(java.util.Locale.ROOT, "%.1f", points);
        int valColor = qReady ? 0xFFFFE066 : (cdReady ? 0xFFCC7744 : 0xFF996655);
        guiGraphics.drawString(font, valText, labelX, labelY - 5, valColor, true);
        if (qReady) {
            guiGraphics.drawString(font, "[R]", labelX + 2, labelY + 5, 0xFFFFF0A0, true);
        } else if (!cdReady) {
            String cdText = String.format(java.util.Locale.ROOT, "%.0fs", Math.ceil(ultiCdTicks / 20.0F));
            guiGraphics.drawString(font, cdText, labelX + 2, labelY + 5, 0xFFAA8877, true);
        }
    }

    public static void renderRaidenEyeRing(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        ClientAuraTracker.RaidenDisplayState state = ClientAuraTracker.getRaiden(entity.getId());
        if (state == null) return;
        float eyeTicks = state.effectiveEyeTicks();
        float musouTicks = state.effectiveMusouTicks();
        if (eyeTicks <= 0.0F && musouTicks <= 0.0F && state.resolve <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float time = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        if (state.resolve > 0 || musouTicks > 0.0F || eyeTicks > 0.0F) {
            float resolveFrac = musouTicks > 0.0F ? 1.0F : Mth.clamp(state.resolve / 60.0F, 0.0F, 1.0F);
            ResourceLocation ringTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_chakra_ring.png");
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(ringTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.76F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, -0.44F);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float pulse = 1.0F + 0.025F * Mth.sin(time * 0.20F);
            float half = (musouTicks > 0.0F ? 0.94F : Mth.lerp(resolveFrac, 0.78F, 0.88F)) * pulse;
            int baseCr = musouTicks > 0.0F ? 225 : 145;
            int baseCg = musouTicks > 0.0F ? (state.netherite ? 175 : 150) : 75;
            int baseCb = 255;
            int baseCa = musouTicks > 0.0F ? 210 : (state.resolve > 0 ? 115 : 65);

            vc.addVertex(mat, -half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat, -half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);

            vc.addVertex(mat, -half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat, -half, -half, 0.0F).setColor(baseCr, baseCg, baseCb, baseCa).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);

            if (resolveFrac > 0.001F) {
                VertexConsumer glowVc = buffer.getBuffer(RenderType.lightning());
                float rMid = half * 0.74F;
                float rIn = rMid - half * 0.065F;
                float rOut = rMid + half * 0.065F;
                float rCoreIn = rMid - half * 0.028F;
                float rCoreOut = rMid + half * 0.028F;
                float maxTheta = (float) Math.PI * resolveFrac;
                int segs = 24;
                int outerAlpha = musouTicks > 0.0F ? 220 : 175;
                int coreAlpha = 245;

                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < segs; i++) {
                        float t0 = (float) i / (float) segs;
                        float t1 = (float) (i + 1) / (float) segs;
                        float a0 = -(float) (Math.PI * 0.5) + side * (maxTheta * t0);
                        float a1 = -(float) (Math.PI * 0.5) + side * (maxTheta * t1);
                        float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
                        float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

                        for (float z : new float[]{0.003F, -0.003F}) {
                            glowVc.addVertex(mat, c0 * rIn,  s0 * rIn,  z).setColor(185, 95, 255, outerAlpha);
                            glowVc.addVertex(mat, c0 * rOut, s0 * rOut, z).setColor(185, 95, 255, outerAlpha);
                            glowVc.addVertex(mat, c1 * rOut, s1 * rOut, z).setColor(185, 95, 255, outerAlpha);
                            glowVc.addVertex(mat, c1 * rIn,  s1 * rIn,  z).setColor(185, 95, 255, outerAlpha);

                            glowVc.addVertex(mat, c0 * rCoreIn,  s0 * rCoreIn,  z * 1.4F).setColor(245, 225, 255, coreAlpha);
                            glowVc.addVertex(mat, c0 * rCoreOut, s0 * rCoreOut, z * 1.4F).setColor(245, 225, 255, coreAlpha);
                            glowVc.addVertex(mat, c1 * rCoreOut, s1 * rCoreOut, z * 1.4F).setColor(245, 225, 255, coreAlpha);
                            glowVc.addVertex(mat, c1 * rCoreIn,  s1 * rCoreIn,  z * 1.4F).setColor(245, 225, 255, coreAlpha);
                        }
                    }
                }
            }
            poseStack.popPose();
        }

        if (eyeTicks > 0.0F) {
            ResourceLocation eyeTex = ResourceLocation.fromNamespaceAndPath("velorise_staves", "textures/entity/raiden_eye_aura.png");
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(eyeTex));
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() + 0.36F + 0.04F * Mth.sin(time * 0.14F), 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0F, 0.0F, -0.22F);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();
            float half = 0.34F;
            int cr = 255, cg = state.netherite ? 235 : 210, cb = 255, ca = 245;

            vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
            vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);

            vc.addVertex(mat, -half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half,  half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat,  half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            vc.addVertex(mat, -half, -half, 0.0F).setColor(cr, cg, cb, ca).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, -1.0F);
            poseStack.popPose();
        }
    }

    private static void renderRaidenHud(GuiGraphics guiGraphics, Minecraft mc) {
        if (mc.player == null) return;
        ClientAuraTracker.RaidenDisplayState state = ClientAuraTracker.getRaiden(mc.player.getId());
        boolean holdingRaiden = (mc.player.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem sm
                && sm.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING)
                || (mc.player.getOffhandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem so
                && so.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING);
        if (state == null) return;
        float eyeTicks = state.effectiveEyeTicks();
        float musouTicks = state.effectiveMusouTicks();
        if (!holdingRaiden && eyeTicks <= 0.0F && musouTicks <= 0.0F) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        float cx = sw * 0.5F + 38.0F;
        float cy = sh * 0.5F;

        Matrix4f mat = guiGraphics.pose().last().pose();
        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        float startDeg = -52.0F;
        float endDeg = 52.0F;
        int segments = 20;

        drawHudArc(vc, mat, cx, cy, 29.0F, 35.0F, startDeg, endDeg, segments, 28, 12, 42, 165);
        float resolveFrac = Mth.clamp(state.resolve / 60.0F, 0.0F, 1.0F);
        if (resolveFrac > 0.01F) {
            float fillEnd = Mth.lerp(resolveFrac, startDeg, endDeg);
            drawHudArc(vc, mat, cx, cy, 29.5F, 34.5F, startDeg, fillEnd, Math.max(2, Math.round(segments * resolveFrac)), 195, 95, 255, 240);
        }

        if (musouTicks > 0.0F) {
            float mFrac = Mth.clamp(musouTicks / 140.0F, 0.0F, 1.0F);
            drawHudArc(vc, mat, cx, cy, 36.0F, 41.0F, startDeg, endDeg, segments, 22, 8, 36, 190);
            if (mFrac > 0.01F) {
                float mEnd = Mth.lerp(mFrac, startDeg, endDeg);
                drawHudArc(vc, mat, cx, cy, 36.5F, 40.5F, startDeg, mEnd, Math.max(2, Math.round(segments * mFrac)), 235, 175, 255, 250);
            }
        }
        guiGraphics.flush();

        Font font = mc.font;
        int labelX = Math.round(cx + 38);
        int labelY = Math.round(cy - 9);
        if (eyeTicks > 0.0F) {
            String eyeStr = String.format(java.util.Locale.ROOT, "\u25C9 %.0fs", Math.ceil(eyeTicks / 20.0F));
            guiGraphics.drawString(font, eyeStr, labelX, labelY, 0xFFD99BFF, true);
            labelY += 10;
        }
        guiGraphics.drawString(font, "Resolve " + state.resolve + "/60", labelX, labelY, 0xFFE6C2FF, true);
        if (musouTicks > 0.0F) {
            String mStr = String.format(java.util.Locale.ROOT, "Musou %.1fs", musouTicks / 20.0F);
            guiGraphics.drawString(font, mStr, labelX, labelY + 10, 0xFFFFD6FF, true);
        }
    }

    private static void drawHudArc(VertexConsumer vc, Matrix4f mat, float cx, float cy,
                                   float rIn, float rOut, float startDeg, float endDeg, int segs,
                                   int r, int g, int b, int a) {
        for (int i = 0; i < segs; i++) {
            float a0 = (float) Math.toRadians(Mth.lerp((float) i / segs, startDeg, endDeg));
            float a1 = (float) Math.toRadians(Mth.lerp((float) (i + 1) / segs, startDeg, endDeg));
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            vc.addVertex(mat, cx + c0 * rIn, cy + s0 * rIn, 0.0F).setColor(r, g, b, a);
            vc.addVertex(mat, cx + c1 * rIn, cy + s1 * rIn, 0.0F).setColor(r, g, b, a);
            vc.addVertex(mat, cx + c1 * rOut, cy + s1 * rOut, 0.0F).setColor(r, g, b, a);
            vc.addVertex(mat, cx + c0 * rOut, cy + s0 * rOut, 0.0F).setColor(r, g, b, a);
        }
    }
}
