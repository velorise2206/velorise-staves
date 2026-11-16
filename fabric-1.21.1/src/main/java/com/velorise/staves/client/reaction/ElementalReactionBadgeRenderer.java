package com.velorise.staves.client.reaction;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.client.ClientAuraTracker;
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

/**
 * Dedicated renderer for floating 3D Elemental Reaction & Aura badges,
 * Floating Combat Texts, Player Aura/Resonance/Shield HUDs, Frozen overlays,
 * and Crystallize world shards.
 */
public final class ElementalReactionBadgeRenderer {
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);
    private static final Map<Element, ResourceLocation> TEXTURES_32 = new EnumMap<>(Element.class);
    private static final Map<Element, ResourceLocation> HUD_TEXTURES = new EnumMap<>(Element.class);
    private static final Map<Element, ResourceLocation> SHIELD_TEXTURES = new EnumMap<>(Element.class);
    private static final ResourceLocation OMNI_TEXTURE = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/omni.png");
    private static final ResourceLocation OMNI_TEXTURE_32 = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/omni_32.png");
    private static final ResourceLocation RESONANCE_TEXTURE = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/resonance.png");
    private static final ResourceLocation RESONANCE_TEXTURE_32 = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/resonance_32.png");
    private static final ResourceLocation MOONSIGN_TEXTURE = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/moonsign.png");
    private static final ResourceLocation MOONSIGN_TEXTURE_32 = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/gui/moonsign_32.png");
    private static final ResourceLocation EFFECT_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/effect_background");
    private static final ResourceLocation EFFECT_BACKGROUND_LARGE_SPRITE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_large");
    private static final ResourceLocation EFFECT_BACKGROUND_SMALL_SPRITE = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static final ResourceLocation FROSTED_ICE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/frosted_ice_0.png");
    private static final ResourceLocation STONE_STATUE_TEXTURE = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/entity/stone_stele.png");

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

    private ElementalReactionBadgeRenderer() {}

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

    public static boolean isLocalOverloadedForHud() {
        Minecraft mc = Minecraft.getInstance();
        return ClientAuraTracker.localOverloaded
                && !ElementalResonanceManager.isOverloadExempt(mc.player);
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

    public static void resetFrozenClientState() {
        FROZEN_RENDER_STATES.clear();
        hasFrozenCam = false;
        wasFrozenFirstPerson = false;
    }

    public static void pruneStaleFrozenStates(net.minecraft.client.multiplayer.ClientLevel level) {
        if (level == null) return;
        FROZEN_RENDER_STATES.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null || !ClientAuraTracker.isFrozen(e.getKey()));
    }

    public static boolean isLocalPlayerFrozen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ClientAuraTracker.isFrozen(mc.player.getId());
    }

    public static void syncAndLockFrozenPlayer(boolean captureMouseDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && System.currentTimeMillis() < ClientAuraTracker.silentDrainUntil) {
            mc.player.invulnerableTime = 0;
            mc.player.hurtTime = 0;
            mc.player.hurtDuration = 0;
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
        }
        if (captureMouseDelta) {
            float dYaw = Mth.wrapDegrees(mc.player.getYRot() - state.yRot);
            float dPitch = mc.player.getXRot() - state.xRot;
            if (!firstPerson) {
                if (wasFrozenFirstPerson) {
                    frozenCamYaw = state.yRot;
                    frozenCamPitch = state.xRot;
                    wasFrozenFirstPerson = false;
                }
                frozenCamYaw = Mth.wrapDegrees(frozenCamYaw + dYaw);
                frozenCamPitch = Mth.clamp(frozenCamPitch + dPitch, -90.0F, 90.0F);
            } else {
                wasFrozenFirstPerson = true;
            }
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

    public static void tickFrozenPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && System.currentTimeMillis() < ClientAuraTracker.silentDrainUntil) {
            mc.player.invulnerableTime = 0;
            mc.player.hurtTime = 0;
            mc.player.hurtDuration = 0;
        }
        if (mc.level != null && mc.player != null && mc.player.tickCount % 100 == 0) {
            ClientAuraTracker.pruneStale(mc.level);
            pruneStaleFrozenStates(mc.level);
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

    public static void clearPyroInnateFire(LivingEntity entity) {
        if (entity == null) return;
        if ((ClientAuraTracker.hasPyroInnate(entity.getId()) || ClientAuraTracker.hasOmniInnate(entity.getId())) && (entity.getRemainingFireTicks() > 0 || entity.isOnFire())) {
            entity.clearFire();
            entity.setSharedFlagOnFire(false);
        }
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
        entity.walkAnimation.update(0.0F, 0.0F);
        if (entity instanceof net.minecraft.world.entity.animal.IronGolem golem) {
            golem.offerFlower(false);
            try {
                java.lang.reflect.Field f = net.minecraft.world.entity.animal.IronGolem.class.getDeclaredField("attackAnimationTick");
                f.setAccessible(true);
                f.setInt(golem, 0);
            } catch (Throwable ignored) {
                try {
                    java.lang.reflect.Field f = net.minecraft.world.entity.animal.IronGolem.class.getDeclaredField("f_28828_");
                    f.setAccessible(true);
                    f.setInt(golem, 0);
                } catch (Throwable ignored2) {}
            }
        }
        if (entity instanceof net.minecraft.world.entity.animal.Squid squid) {
            squid.tentacleAngle = squid.oldTentacleAngle;
            squid.tentacleMovement = 0.0F;
            squid.xBodyRot = squid.xBodyRotO;
            squid.zBodyRot = squid.zBodyRotO;
        } else if (entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse) {
            horse.tailCounter = 0;
            horse.setStanding(false);
        } else if (entity instanceof net.minecraft.world.entity.monster.Guardian) {
            entity.yBodyRot = state.yBodyRot;
            entity.yBodyRotO = state.yBodyRot;
        }
    }

    public static void renderFrozenIceOverlay(
            LivingEntity entity,
            net.minecraft.client.model.EntityModel<?> model,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        if (ClientAuraRenderer.isRenderingScreen() || entity == null || model == null) return;
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
        ClientAuraTracker.AuraDisplayData auraData = ClientAuraTracker.get(entity.getId());
        boolean isPetrified = auraData != null && System.currentTimeMillis() < auraData.petrifiedUntil;
        float iceScale = entityScale * (isPetrified ? 1.012F : 1.025F);
        poseStack.scale(-iceScale, -iceScale, iceScale);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        VertexConsumer iceConsumer = buffer.getBuffer(isPetrified
                ? RenderType.entityCutoutNoCull(STONE_STATUE_TEXTURE)
                : RenderType.entityTranslucent(FROSTED_ICE_TEXTURE));
        model.renderToBuffer(poseStack, iceConsumer, FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, isPetrified ? 0xFFFFF8EA : 0x7378E1FF);

        if (isPetrified) {
            float crackScale = 1.002F;
            poseStack.scale(crackScale, crackScale, crackScale);
            VertexConsumer crackConsumer = buffer.getBuffer(RenderType.entityTranslucent(FROSTED_ICE_TEXTURE));
            model.renderToBuffer(poseStack, crackConsumer, FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xEEFFCC33);
        }

        poseStack.popPose();
    }
    public static void renderReactionBadges(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        boolean showIcons = true;
        try {
            showIcons = StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.get();
        } catch (Exception ignored) {}
        if (!showIcons) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entity.distanceToSqr(mc.player) > 64.0D * 64.0D) return;
        if (entity != mc.player && !mc.player.hasLineOfSight(entity)) return;

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
                    && data.staffAnimUntil <= now && data.jadeShieldUntil <= now && data.nightsFocusUntil <= now && data.laylaDreamsphereUntil <= now
                    && !data.xilonenSkating && data.xilonenSamplesUntil <= now && !data.gliding && !data.plunging && data.neuvilletteActiveUntil <= now
                    && !data.tsaritsaCrownActive && data.tsaritsaSyncUntil <= now
                    && !data.nahidaIdleSwing && !data.nahidaViewfinder && data.skandhaMarkedUntil <= now && data.nahidaShrineUntil <= now
                    && !data.isFrozen && !hasInnate && !omniInnate) {
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
                display.add(innateElem);
            }
        }

        if (display.isEmpty() && !omniInnate) return;

        double height = (entity instanceof net.minecraft.world.entity.player.Player)
                ? entity.getBbHeight() + 0.18D
                : entity.getBbHeight() + 0.28D + Math.min(0.5D, entity.getBbHeight() * 0.10D);
        float iconSize = 16.0F;
        float gap = 3.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, height, 0.0D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        float popScale = 1.0F;
        int iconAlpha = 255;

        if (inDualReaction && data != null) {
            long dualAge = Math.max(0L, now - data.dualStartTime);
            long dualDuration = Math.max(1L, data.dualUntil - data.dualStartTime);
            float dualProgress = Math.min(1.0F, (float) dualAge / (float) dualDuration);

            if (dualAge < 200L) {
                float popPhase = (float) dualAge / 200.0F;
                popScale = 1.0F + 0.28F * (float) Math.sin(popPhase * Math.PI);
            }
            if (data.elements.isEmpty() && !hasInnate && dualProgress > 0.55F) {
                float fade = (1.0F - dualProgress) / 0.45F;
                iconAlpha = Math.max(15, Math.min(255, Math.round(fade * 255.0F)));
            }
        }

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
        VertexConsumer main = buffer.getBuffer(RenderType.text(texture));
        drawDoubleSidedQuad(main, matrix, x, y, width, height, 255, 255, 255, alpha);
    }

    private static void drawDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, float x, float y, float width, float height, int r, int g, int b, int a) {
        consumer.addVertex(matrix, x, y, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y + height, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y + height, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F).setLight(FULL_BRIGHT);

        consumer.addVertex(matrix, x + width, y, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x + width, y + height, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y + height, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F).setLight(FULL_BRIGHT);
        consumer.addVertex(matrix, x, y, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F).setLight(FULL_BRIGHT);
    }
    public static int getReactionColor(ElementalReaction reaction) {
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

    public static int getElementTextColor(Element element) {
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

    public static int getElementBorderColor(Element element) {
        return switch (element) {
            case PYRO -> 0xFFFF4500;
            case HYDRO -> 0xFF00C8FF;
            case ELECTRO -> 0xFFBF40FF;
            case CRYO -> 0xFF4DF0FF;
            case DENDRO -> 0xFF32CD32;
            case ANEMO -> 0xFF40E0D0;
            case GEO -> 0xFFFFD700;
        };
    }

    public static void renderColoredBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        int alpha = (color >> 24) & 0xFF;
        int glow = ((Math.max(40, alpha / 3)) << 24) | (color & 0x00FFFFFF);
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, glow);
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, glow);
        guiGraphics.fill(x - 1, y, x, y + height, glow);
        guiGraphics.fill(x + width, y, x + width + 1, y + height, glow);
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, color);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, (0x1A << 24) | (color & 0x00FFFFFF));
    }

    public static void renderCrystallizeShieldHudFrame(GuiGraphics guiGraphics, int x, int y, int color) {
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
    public static void renderFreezeStruggleHud(GuiGraphics guiGraphics, Minecraft mc) {
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

    public static void renderCrystallizeShieldHud(GuiGraphics guiGraphics, Minecraft mc) {
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
                if (isShieldEffect(inst)) {
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
            drawHudEffectBox(guiGraphics, x, y);
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

    public static List<net.minecraft.network.chat.Component> renderCrystallizeShieldInventory(
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
                if (isShieldEffect(inst)) {
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
            drawInventoryEffectBox(guiGraphics, startX, y);
            guiGraphics.flush();
            ResourceLocation shieldTex = SHIELD_TEXTURES.get(shield);
            if (shieldTex != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
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

    public static List<ElementalResonanceManager.ResonanceType> getActiveResonancesForHud() {
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

    public static int getResonanceHudSlotCount() {
        if (!StavesConfig.isElementalResonanceEnabled()) return 0;
        if (isLocalOverloadedForHud()) return 1;
        return getActiveResonancesForHud().size();
    }

    private static int getResonanceBorderColor(ElementalResonanceManager.ResonanceType type) {
        return type.element() != null ? getElementBorderColor(type.element()) : 0xFFFFD60A;
    }

    public static void renderResonanceHud(GuiGraphics guiGraphics, Minecraft mc) {
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

    public static List<net.minecraft.network.chat.Component> renderResonanceInventory(
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

    public static void renderPlayerAuraBars(GuiGraphics guiGraphics, Minecraft mc) {
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        if (data == null || data.elements.isEmpty()) return;

        long now = System.currentTimeMillis();

        if (data.expireTime > 0 && data.elements.contains(Element.PYRO) && (mc.player.isInLava() || mc.player.isOnFire() || mc.player.getRemainingFireTicks() > 0)) {
            data.expireTime = now + 9500L;
        }
        if (data.expireTime > 0 && data.elements.contains(Element.HYDRO) && mc.player.isInWaterOrRain()) {
            data.expireTime = now + 9500L;
        }

        if (data.expireTime > 0 && now >= data.expireTime) {
            data.elements.clear();
            data.expireTime = 0L;
            return;
        }

        int screenWidth = guiGraphics.guiWidth();
        int top = mc.isDemo() ? 16 : 1;

        int beneficialCount = 0;
        int harmfulCount = 0;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (inst.showIcon()) {
                if (isBeneficial(inst)) {
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
                if (inst.showIcon() && isShieldEffect(inst)) {
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
                col = (harmfulCount + index) % 10;
                row = 1 + (harmfulCount + index) / 10;
            } else {
                col = index % 10;
                row = index / 10;
            }

            int x = screenWidth - 25 * (col + 1);
            int y = top + 26 * row;

            long remMs = data.expireTime > 0 ? Math.max(0, data.expireTime - now) : 0;
            float alpha = 1.0F;
            if (data.expireTime > 0 && remMs <= 3000L) {
                alpha = Math.sin((remMs / 140.0D) * Math.PI) < 0 ? 0.35F : 1.0F;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();

            drawHudEffectBox(guiGraphics, x, y);

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

    public static List<net.minecraft.network.chat.Component> renderInventoryAura(
            GuiGraphics guiGraphics,
            EffectRenderingInventoryScreen<?> screen,
            int leftPos,
            int topPos,
            int imageWidth,
            int mouseX,
            int mouseY
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        List<net.minecraft.network.chat.Component> resultTooltip = null;

        List<net.minecraft.network.chat.Component> shieldTooltip =
                renderCrystallizeShieldInventory(guiGraphics, mc, leftPos, topPos, imageWidth, screen, mouseX, mouseY);
        if (shieldTooltip != null) {
            resultTooltip = shieldTooltip;
        }
        List<net.minecraft.network.chat.Component> resTooltip =
                renderResonanceInventory(guiGraphics, mc, leftPos, topPos, imageWidth, screen, mouseX, mouseY);
        if (resultTooltip == null && resTooltip != null) {
            resultTooltip = resTooltip;
        }

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        if (data == null || data.elements.isEmpty()) return resultTooltip;

        long now = System.currentTimeMillis();

        if (data.expireTime > 0 && data.elements.contains(Element.PYRO) && (mc.player.isInLava() || mc.player.isOnFire() || mc.player.getRemainingFireTicks() > 0)) {
            data.expireTime = now + 9500L;
        }
        if (data.expireTime > 0 && data.elements.contains(Element.HYDRO) && mc.player.isInWaterOrRain()) {
            data.expireTime = now + 9500L;
        }

        if (data.expireTime > 0 && now >= data.expireTime) {
            data.elements.clear();
            data.expireTime = 0L;
            return resultTooltip;
        }

        int startX = leftPos + imageWidth + 2;
        int availableWidth = screen.width - startX;
        if (availableWidth < 32) return resultTooltip;

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
            if (isShieldEffect(inst)) {
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

            drawInventoryEffectBox(guiGraphics, startX, y);
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
        if (resultTooltip == null && pendingTooltip != null) {
            resultTooltip = pendingTooltip;
        }
        return resultTooltip;
    }

    public static List<Rect2i> getInventoryExtraAreas(EffectRenderingInventoryScreen<?> screen, int leftPos, int topPos, int imageWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !screen.canSeeEffects()) return Collections.emptyList();

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
            if (isShieldEffect(inst)) {
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

            renderSmoothAuraSphere(consumer, poseStack.last().pose(), vx, vy, vz, 0.34F, 6, 8, r, g, b, hr, hg, hb);

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
        renderFresnelSpherePass(consumer, matrix, vx, vy, vz, radius, latBands, lonSegs, r, g, b, hr, hg, hb, false);
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
                    fa = ClientAuraRenderer.clampColor((int) (24.0F * haze));
                } else {
                    float fresnel = quantizeBand((float) Math.pow(1.0F - ndotv * 0.72F, 1.35D) * edgeFeather, 4);
                    float rimT = quantizeBand(1.0F - ndotv, 4);
                    fr = ClientAuraRenderer.clampColor((int) (r + (hr - r) * rimT * 0.45F));
                    fg = ClientAuraRenderer.clampColor((int) (g + (hg - g) * rimT * 0.45F));
                    fb = ClientAuraRenderer.clampColor((int) (b + (hb - b) * rimT * 0.45F));
                    fa = ClientAuraRenderer.clampColor((int) (58.0F * fresnel));
                }

                ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
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
                int fr = ClientAuraRenderer.clampColor((int) (br + (hr - br) * spec));
                int fg = ClientAuraRenderer.clampColor((int) (bg + (hg - bg) * spec));
                int fb = ClientAuraRenderer.clampColor((int) (bb + (hb - bb) * spec));

                ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                        px[i00], py[i00], pz[i00], fr, fg, fb, baseAlpha,
                        px[i10], py[i10], pz[i10], fr, fg, fb, baseAlpha,
                        px[i11], py[i11], pz[i11], fr, fg, fb, baseAlpha,
                        px[i01], py[i01], pz[i01], fr, fg, fb, baseAlpha);
            }
        }
    }

    public static void renderGenericLightningArc(
            ClientAuraTracker.ActiveArc arc,
            Vec3 startPos,
            Vec3 endPos,
            Vec3 camPos,
            long now,
            VertexConsumer consumer,
            Matrix4f matrix
    ) {
        Vec3 diff = endPos.subtract(startPos);
        double dist = diff.length();
        if (dist < 0.05D) return;

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

            drawQuadRibbon(consumer, matrix, a, b, normal, 0.02D, r, g, bCol, 230);
            drawQuadRibbon(consumer, matrix, a, b, normal, 0.007D, 255, 255, 255, 255);
        }
    }

    public static void drawQuadRibbon(
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

        ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                x1, y1, z1, r, g, bCol, alpha,
                x2, y2, z2, r, g, bCol, alpha,
                x3, y3, z3, r, g, bCol, alpha,
                x4, y4, z4, r, g, bCol, alpha);
        ClientAuraRenderer.emitCrystalQuad(consumer, matrix,
                x4, y4, z4, r, g, bCol, alpha,
                x3, y3, z3, r, g, bCol, alpha,
                x2, y2, z2, r, g, bCol, alpha,
                x1, y1, z1, r, g, bCol, alpha);
    }
}
