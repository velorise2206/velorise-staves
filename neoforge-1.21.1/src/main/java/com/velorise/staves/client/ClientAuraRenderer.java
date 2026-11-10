package com.velorise.staves.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.effect.ModMobEffects;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.player.Input;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.*;

@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientAuraRenderer {
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);
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
            TEXTURES.put(e, res);
            HUD_TEXTURES.put(e, res);
            if (e == Element.PYRO || e == Element.HYDRO || e == Element.ELECTRO || e == Element.CRYO) {
                SHIELD_TEXTURES.put(e, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/mob_effect/" + lower + "_shield.png"));
            }
        }
    }

    private static boolean renderingScreen = false;

    private ClientAuraRenderer() {}

    @SubscribeEvent
    public static void onClientLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAuraTracker.clear();
        FROZEN_RENDER_STATES.clear();
        hasFrozenCam = false;
        wasFrozenFirstPerson = false;
        StavesConfig.clearServerSync();
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        syncAndLockFrozenPlayer(false);
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        syncAndLockFrozenPlayer(false);
    }

    @SubscribeEvent
    public static void onRenderFramePre(RenderFrameEvent.Pre event) {
        syncAndLockFrozenPlayer(true);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null
                && event.getCamera().getEntity() == mc.player
                && ClientAuraTracker.isFrozen(mc.player.getId())) {
            FrozenRenderState state = FROZEN_RENDER_STATES.computeIfAbsent(mc.player.getId(), k -> new FrozenRenderState(mc.player));
            if (!mc.options.getCameraType().isFirstPerson() && hasFrozenCam) {
                event.setYaw(frozenCamYaw);
                event.setPitch(frozenCamPitch);
            } else {
                event.setYaw(state.yRot);
                event.setPitch(state.xRot);
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null
                && event.getEntity() == mc.player
                && ClientAuraTracker.isFrozen(mc.player.getId())) {
            Input input = event.getInput();
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }

    private static void syncAndLockFrozenPlayer(boolean captureMouseDelta) {
        Minecraft mc = Minecraft.getInstance();
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

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        renderingScreen = true;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        renderingScreen = false;
        if (event.getScreen() instanceof EffectRenderingInventoryScreen<?> screen && screen.canSeeEffects()) {
            renderInventoryAura(event.getGuiGraphics(), screen, screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), event.getMouseX(), event.getMouseY());
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (renderingScreen) return;
        applyFrozenPoseLock(event.getEntity());
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (renderingScreen) return;
        LivingEntity entity = event.getEntity();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        renderFrozenIceOverlay(entity, event.getRenderer().getModel(), poseStack, buffer);
        render(entity, poseStack, buffer);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        renderPlayerAuraHud(event.getGuiGraphics());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onClientEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                && event.getTarget() instanceof LivingEntity living
                && ClientAuraTracker.isFrozen(living.getId())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onClientEntityInteractSpecific(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()
                && event.getTarget() instanceof LivingEntity living
                && ClientAuraTracker.isFrozen(living.getId())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    public static void clearPyroInnateFire(LivingEntity entity) {
        if (entity == null) return;
        if (ClientAuraTracker.hasPyroInnate(entity.getId()) && (entity.getRemainingFireTicks() > 0 || entity.isOnFire())) {
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

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen) return;
        boolean showIcons = true;
        boolean showReactionText = true;
        boolean showDamageNumbers = true;
        try {
            showIcons = StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.get();
            showReactionText = StavesConfig.SHOW_REACTION_TEXT.get();
            showDamageNumbers = StavesConfig.SHOW_DAMAGE_NUMBERS.get();
        } catch (Exception ignored) {}
        if (!showIcons && !showReactionText && !showDamageNumbers) return;

        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entity.distanceToSqr(mc.player) > 64.0D * 64.0D) return;
        if (entity != mc.player && !mc.player.hasLineOfSight(entity)) return;

        // Check innate element on entity (including server-synced mutated innate elements)
        Element innateElement = ElementalGaugeManager.getInnateElement(entity);
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        if (innateElement == null && data != null && data.permanentInnateElement != null) {
            innateElement = data.permanentInnateElement;
        }

        if (data == null && innateElement == null) return;

        long now = System.currentTimeMillis();
        // Underwater fish / passive aquatic creatures only display overhead icons when an elemental reaction occurs
        if (ElementalGaugeManager.isPassiveAquaticOrAmbient(entity)) {
            boolean fishReactionActive = data != null && ((data.reaction != null && now < data.reactionUntil) || now < data.dualUntil);
            if (!fishReactionActive) return;
        }
        if (data != null) {
            if (data.expireTime > 0 && now >= data.expireTime && innateElement == null) {
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
            if (data.elements.isEmpty() && data.reaction == null && data.dualUntil <= now && data.damageUntil <= now && innateElement == null) {
                ClientAuraTracker.remove(entity.getId());
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
        if (innateElement != null && !display.contains(innateElement)) {
            // Always show innate element icon (and allow coexisting auras like Dendro on Geo)
            display.add(0, innateElement);
        }

        boolean hasReaction = showReactionText && data != null && data.reaction != null && now < data.reactionUntil;
        boolean hasDamage = showDamageNumbers && data != null && data.damageUntil > now && data.lastDamage > 0.0F;

        if ((!showIcons || display.isEmpty()) && !hasReaction && !hasDamage) return;

        double height = (entity instanceof net.minecraft.world.entity.player.Player)
                ? entity.getBbHeight() + 0.18D
                : entity.getBbHeight() + 0.15D;
        float iconSize = 16.0F;
        float gap = 3.0F;

        // Branch 1: Draw Overhead Element Icon(s) with Pop & Fade-out Animation
        if (showIcons && !display.isEmpty()) {
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
                if (data.elements.isEmpty() && innateElement == null && dualProgress > 0.55F) {
                    float fade = (1.0F - dualProgress) / 0.45F;
                    iconAlpha = Math.max(15, Math.min(255, Math.round(fade * 255.0F)));
                }
            }

            // Genshin-style expiration flickering (pulsing when remaining time <= 3.0s):
            if (data != null && data.expireTime > 0 && innateElement == null) {
                long remainingMs = data.expireTime - now;
                if (remainingMs <= 3000L && remainingMs > 0) {
                    float pulse = (float) Math.sin((remainingMs / 140.0D) * Math.PI);
                    if (pulse < 0.0F) {
                        iconAlpha = Math.max(35, Math.round(iconAlpha * 0.25F));
                    }
                }
            }

            poseStack.scale(0.020F * popScale, -0.020F * popScale, 0.020F * popScale);

            int count = display.size();
            float totalWidth = count * iconSize + (count - 1) * gap;
            float startX = -totalWidth / 2.0F;

            for (int i = 0; i < count; i++) {
                Element elem = display.get(i);
                int elemAlpha = iconAlpha;
                if (inDualReaction && data != null && !data.elements.contains(elem) && elem != innateElement) {
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
                    ResourceLocation tex = TEXTURES.get(e);
                    if (tex != null) {
                        bs.endBatch(RenderType.text(tex));
                    }
                }
            }

            poseStack.popPose();
        }

        // Branch 2: Floating Combat Feedback (Reaction Text & Damage Numbers - Genshin Style)
        if (hasReaction || hasDamage) {
            poseStack.pushPose();
            // Centered at torso / combat height so combat text is always visible and never covered by overhead icons
            double combatHeight = Math.max(0.65D, entity.getBbHeight() * 0.65D);
            poseStack.translate(0.0D, combatHeight, 0.0D);
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

            float textPop = 1.0F;
            long primaryAge = hasReaction ? (now - data.reactionStartTime) : (now - data.damageStartTime);
            if (primaryAge < 250L && primaryAge >= 0L) {
                textPop = 1.0F + 0.38F * (float) Math.sin((primaryAge / 250.0F) * Math.PI);
            }
            poseStack.scale(0.030F * textPop, -0.030F * textPop, 0.030F * textPop);

            Font font = mc.font;
            Matrix4f matrix = poseStack.last().pose();

            float reactY = 0.0F;
            if (hasReaction) {
                long reactionAge = Math.max(0L, now - data.reactionStartTime);
                long reactionDuration = Math.max(1L, data.reactionUntil - data.reactionStartTime);
                float progress = Math.max(0.0F, Math.min(1.0F, (float) reactionAge / (float) reactionDuration));

                reactY = -6.0F - (progress * 18.0F);
                String text = "§l" + data.reaction.reactionName();
                float textWidth = font.width(text);
                float textX = -textWidth / 2.0F;

                int baseColor = getReactionColor(data.reaction) & 0x00FFFFFF;
                int alpha = 255;
                if (progress > 0.7F) {
                    alpha = Math.max(15, Math.min(255, Math.round(((1.0F - progress) / 0.3F) * 255.0F)));
                }
                int fgColor = baseColor | (alpha << 24);
                int seeThroughAlpha = Math.max(15, Math.round(alpha * 0.35F));
                int seeThroughColor = baseColor | (seeThroughAlpha << 24);

                // See-through pass
                font.drawInBatch(text, textX, reactY, seeThroughColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                // Foreground pass with dropShadow = true
                font.drawInBatch(text, textX, reactY, fgColor, true, matrix, buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            }

            if (hasDamage) {
                long dmgAge = Math.max(0L, now - data.damageStartTime);
                long dmgDuration = Math.max(1L, data.damageUntil - data.damageStartTime);
                float dmgProgress = Math.max(0.0F, Math.min(1.0F, (float) dmgAge / (float) dmgDuration));

                float dmgY = hasReaction ? (reactY + 11.0F) : (-6.0F - (dmgProgress * 18.0F));
                String dmgText = "§l" + (data.lastDamage % 1.0F == 0.0F ? String.valueOf((int) data.lastDamage) : String.format(Locale.ROOT, "%.1f", data.lastDamage));
                float dmgWidth = font.width(dmgText);
                float dmgX = -dmgWidth / 2.0F;

                int dmgBaseColor = data.damageElement != null ? getElementTextColor(data.damageElement) : 0xFFFFFF;
                int dmgAlpha = 255;
                if (dmgProgress > 0.7F) {
                    dmgAlpha = Math.max(15, Math.min(255, Math.round(((1.0F - dmgProgress) / 0.3F) * 255.0F)));
                }
                int dmgFg = (dmgBaseColor & 0x00FFFFFF) | (dmgAlpha << 24);
                int dmgSeeThrough = (dmgBaseColor & 0x00FFFFFF) | (Math.max(15, Math.round(dmgAlpha * 0.35F)) << 24);

                font.drawInBatch(dmgText, dmgX, dmgY, dmgSeeThrough, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                font.drawInBatch(dmgText, dmgX, dmgY, dmgFg, true, matrix, buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            }

            poseStack.popPose();
        }
    }

    private static void drawIcon(PoseStack poseStack, MultiBufferSource buffer, Element element, float x, float y, float width, float height, int alpha) {
        ResourceLocation texture = TEXTURES.get(element);
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

    public static void renderPlayerAuraHud(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
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
        renderFreezeStruggleHud(guiGraphics, mc);

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

            // Vanilla 24x24 effect background (solid background, never blinks)
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SPRITE, x, y, 24, 24);

            // 16x16 Element icon centered inside the 24x24 box (blinks during last 3 seconds)
            ResourceLocation tex = HUD_TEXTURES.get(elem);
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

        renderCrystallizeShieldInventory(guiGraphics, mc, leftPos, topPos, imageWidth, screen, mouseX, mouseY);

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

        // Flush any pending screen elements (such as hovered slot tooltip) to prevent shader color bleed
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

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
        int totalEffects = vanillaCount + customShieldSlot + data.elements.size();
        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        int index = 0;
        for (Element elem : data.elements) {
            int y = topPos + (vanillaCount + customShieldSlot + index) * yStep;
            long remMs = data.expireTime > 0 ? Math.max(0, data.expireTime - now) : 0;
            float alpha = 1.0F;
            if (data.expireTime > 0 && remMs <= 3000L) {
                alpha = Math.sin((remMs / 140.0D) * Math.PI) < 0 ? 0.35F : 1.0F;
            }

            RenderSystem.enableBlend();

            String elemName = elem.name().charAt(0) + elem.name().substring(1).toLowerCase(Locale.ROOT);
            String title = elemName + " Aura";
            String durationStr;
            if (data.expireTime < 0) {
                durationStr = "∞";
            } else if (data.expireTime > 0) {
                long remSec = remMs / 1000L;
                durationStr = String.format(Locale.ROOT, "%d:%02d", remSec / 60, remSec % 60);
            } else {
                durationStr = "";
            }

            ResourceLocation tex = HUD_TEXTURES.get(elem);

            // Compact Mode (32x32 square) to avoid overlapping with JEI
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SMALL_SPRITE, startX, y, 32, 32);
            if (tex != null) {
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                }
                guiGraphics.blit(tex, startX + 8, y + 8, 0.0F, 0.0F, 16, 16, 16, 16);
                if (alpha < 1.0F) {
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            if (mouseX >= startX && mouseX <= startX + 32 && mouseY >= y && mouseY <= y + 32) {
                List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                tooltip.add(net.minecraft.network.chat.Component.literal(title).withStyle(elem.color()));
                if (!durationStr.isEmpty()) {
                    tooltip.add(net.minecraft.network.chat.Component.literal(durationStr).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                guiGraphics.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY);
            }

            index++;
        }
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static List<Rect2i> getInventoryExtraAreas(EffectRenderingInventoryScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !screen.canSeeEffects()) return Collections.emptyList();

        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        int imageWidth = screen.getXSize();

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

        int totalEffects = vanillaCount + auraCount + (customShield ? 1 : 0);
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
        guiGraphics.fill(x + 2, y + 1, x + 22, y + 2, color);
        guiGraphics.fill(x + 1, y + 2, x + 3, y + 3, color);
        guiGraphics.fill(x + 21, y + 2, x + 23, y + 3, color);

        guiGraphics.fill(x + 1, y + 3, x + 2, y + 21, color);
        guiGraphics.fill(x + 22, y + 3, x + 23, y + 21, color);

        guiGraphics.fill(x + 1, y + 21, x + 3, y + 22, color);
        guiGraphics.fill(x + 21, y + 21, x + 23, y + 22, color);
        guiGraphics.fill(x + 2, y + 22, x + 22, y + 23, color);
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
    }

    private static void renderCrystallizeShieldInventory(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int leftPos,
            int topPos,
            int imageWidth,
            EffectRenderingInventoryScreen<?> screen,
            int mouseX,
            int mouseY
    ) {
        if (mc.player == null) return;
        Element shield = ClientAuraTracker.getActiveCrystallizeShield();
        if (shield == null) {
            shield = ModMobEffects.getActiveShield(mc.player);
        }
        if (shield == null) return;

        int startX = leftPos + imageWidth + 2;
        int availableWidth = screen.width - startX;
        if (availableWidth < 32) return;

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
        int totalEffects = vanillaCount + customShieldSlot + auraCount;
        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        int y = topPos + effectIndex * yStep;
        if (!found) {
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SMALL_SPRITE, startX, y, 32, 32);
            ResourceLocation shieldTex = SHIELD_TEXTURES.get(shield);
            if (shieldTex != null) {
                guiGraphics.blit(shieldTex, startX + 7, y + 7, 0.0F, 0.0F, 18, 18, 18, 18);
            }

            if (mouseX >= startX && mouseX <= startX + 32 && mouseY >= y && mouseY <= y + 32) {
                long remSec = Math.max(0, (ClientAuraTracker.crystallizeShieldUntil - now) / 1000L);
                String durationStr = String.format(Locale.ROOT, "%d:%02d", remSec / 60, remSec % 60);
                String key = "effect.velorise_staves." + shield.name().toLowerCase(Locale.ROOT) + "_shield";
                List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                tooltip.add(net.minecraft.network.chat.Component.translatable(key).withStyle(shield.color()));
                tooltip.add(net.minecraft.network.chat.Component.literal(durationStr).withStyle(net.minecraft.ChatFormatting.GRAY));
                guiGraphics.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            renderActiveElectricArcs(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getCamera());
            renderCrystallizeShards(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getCamera());
        }
    }

    public static void renderActiveElectricArcs(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera) {
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

            poseStack.pushPose();
            poseStack.translate(cx - camPos.x, cy - camPos.y, cz - camPos.z);

            // 1. Perfect Translucent 3D Spherical Shell (Genshin Impact style)
            renderUvSphere(consumer, poseStack.last().pose(), 0.32F, 18, 28, r, g, b, 45);
            renderUvSphere(consumer, poseStack.last().pose(), 0.25F, 14, 22, hr, hg, hb, 28);

            // 2. Central Spinning Faceted Crystal Core
            poseStack.pushPose();
            poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.toRadians(age * 2.2F)));
            renderOctahedronCrystal(consumer, poseStack.last().pose(), 0.15F, 0.08F, r, g, b, hr, hg, hb);
            poseStack.popPose();

            poseStack.popPose();
        }

        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.lightning());
        }
    }

    private static void renderUvSphere(
            VertexConsumer consumer, Matrix4f matrix,
            float radius, int latBands, int lonSegs,
            int r, int g, int b, int alpha
    ) {
        for (int i = 0; i < latBands; i++) {
            double phi1 = -Math.PI / 2.0D + Math.PI * (double) i / (double) latBands;
            double phi2 = -Math.PI / 2.0D + Math.PI * (double) (i + 1) / (double) latBands;

            float y1 = (float) (radius * Math.sin(phi1));
            float r1 = (float) (radius * Math.cos(phi1));
            float y2 = (float) (radius * Math.sin(phi2));
            float r2 = (float) (radius * Math.cos(phi2));

            for (int j = 0; j < lonSegs; j++) {
                double theta1 = 2.0D * Math.PI * (double) j / (double) lonSegs;
                double theta2 = 2.0D * Math.PI * (double) (j + 1) / (double) lonSegs;

                float x11 = (float) (r1 * Math.cos(theta1));
                float z11 = (float) (r1 * Math.sin(theta1));

                float x21 = (float) (r2 * Math.cos(theta1));
                float z21 = (float) (r2 * Math.sin(theta1));

                float x22 = (float) (r2 * Math.cos(theta2));
                float z22 = (float) (r2 * Math.sin(theta2));

                float x12 = (float) (r1 * Math.cos(theta2));
                float z12 = (float) (r1 * Math.sin(theta2));

                drawQuadBothSides(consumer, matrix,
                        x11, y1, z11,
                        x21, y2, z21,
                        x22, y2, z22,
                        x12, y1, z12,
                        r, g, b, alpha);
            }
        }
    }

    private static void renderOctahedronCrystal(
            VertexConsumer consumer, Matrix4f matrix,
            float halfH, float eqRadius,
            int r, int g, int b,
            int hr, int hg, int hb
    ) {
        int sides = 8;
        float[] ex = new float[sides];
        float[] ez = new float[sides];
        for (int i = 0; i < sides; i++) {
            double a = 2.0D * Math.PI * (double) i / (double) sides;
            ex[i] = (float) (eqRadius * Math.cos(a));
            ez[i] = (float) (eqRadius * Math.sin(a));
        }

        float lx = 0.408F, ly = 0.816F, lz = 0.408F;

        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;

            // Upper Pyramid Facet
            float ux = ex[i], uy = -halfH, uz = ez[i];
            float vx = ex[next], vy = -halfH, vz = ez[next];
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-4F) { nx /= len; ny /= len; nz /= len; }

            float dot = Math.max(0.0F, nx * lx + ny * ly + nz * lz);
            float shade = 0.60F + 0.40F * dot;

            int fr = Math.min(255, (int) (r * shade));
            int fg = Math.min(255, (int) (g * shade));
            int fb = Math.min(255, (int) (b * shade));

            drawTriangleBothSides(consumer, matrix,
                    0.0F, halfH, 0.0F,
                    ex[i], 0.0F, ez[i],
                    ex[next], 0.0F, ez[next],
                    fr, fg, fb, 240);

            // Lower Pyramid Facet
            float lux = ex[next], luy = halfH, luz = ez[next];
            float lvx = ex[i], lvy = halfH, lvz = ez[i];
            float lnx = luy * lvz - luz * lvy;
            float lny = luz * lvx - lux * lvz;
            float lnz = lux * lvy - luy * lvx;
            float llen = (float) Math.sqrt(lnx * lnx + lny * lny + lnz * lnz);
            if (llen > 1e-4F) { lnx /= llen; lny /= llen; lnz /= llen; }

            float ldot = Math.max(0.0F, lnx * lx + lny * ly + lnz * lz);
            float lshade = 0.50F + 0.35F * ldot;

            int lfr = Math.min(255, (int) (r * lshade));
            int lfg = Math.min(255, (int) (g * lshade));
            int lfb = Math.min(255, (int) (b * lshade));

            drawTriangleBothSides(consumer, matrix,
                    0.0F, -halfH, 0.0F,
                    ex[next], 0.0F, ez[next],
                    ex[i], 0.0F, ez[i],
                    lfr, lfg, lfb, 240);
        }

        // Inner bright specular core needle (spark of light inside the gem)
        float innerH = halfH * 0.70F;
        float innerR = eqRadius * 0.45F;
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            double a1 = 2.0D * Math.PI * (double) i / (double) sides;
            double a2 = 2.0D * Math.PI * (double) next / (double) sides;
            float ix1 = (float) (innerR * Math.cos(a1));
            float iz1 = (float) (innerR * Math.sin(a1));
            float ix2 = (float) (innerR * Math.cos(a2));
            float iz2 = (float) (innerR * Math.sin(a2));

            drawTriangleBothSides(consumer, matrix, 0.0F, innerH, 0.0F, ix1, 0.0F, iz1, ix2, 0.0F, iz2, hr, hg, hb, 210);
            drawTriangleBothSides(consumer, matrix, 0.0F, -innerH, 0.0F, ix2, 0.0F, iz2, ix1, 0.0F, iz1, hr, hg, hb, 210);
        }
    }

    private static void drawQuadBothSides(
            VertexConsumer consumer, Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int r, int g, int b, int a
    ) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);

        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
    }

    private static void drawTriangleBothSides(
            VertexConsumer consumer, Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int r, int g, int b, int a
    ) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);

        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
    }
}
