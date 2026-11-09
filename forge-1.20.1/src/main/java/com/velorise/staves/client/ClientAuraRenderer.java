package com.velorise.staves.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.*;

@Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientAuraRenderer {
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);
    private static final Map<Element, ResourceLocation> HUD_TEXTURES = new EnumMap<>(Element.class);
    private static final ResourceLocation INVENTORY_LOCATION = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT; // 0xF000F0

    static {
        for (Element e : Element.values()) {
            ResourceLocation res = new ResourceLocation(VeloriseStavesMod.MODID, "textures/element/" + e.name().toLowerCase(Locale.ROOT) + ".png");
            TEXTURES.put(e, res);
            HUD_TEXTURES.put(e, res);
        }
    }

    private static boolean renderingScreen = false;

    private ClientAuraRenderer() {}

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
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (renderingScreen) return;
        LivingEntity entity = event.getEntity();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        render(entity, poseStack, buffer);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            renderPlayerAuraHud(event.getGuiGraphics());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        if (renderingScreen) return;
        try {
            if (!StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.get()) return;
        } catch (Exception ignored) {}

        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entity.distanceToSqr(mc.player) > 64.0D * 64.0D) return;

        // Check innate element on entity (e.g., Blaze/MagmaCube/Strider -> PYRO, Stray/SnowGolem -> CRYO, Guardian/Drowned -> HYDRO, Breeze -> ANEMO)
        Element innateElement = ElementalGaugeManager.getInnateElement(entity);
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());

        // Filter out passive ocean fish/squid unless explicitly hit in combat
        if (data == null && innateElement == null) return;
        if (data == null && ElementalGaugeManager.isPassiveAquaticOrAmbient(entity)) return;

        long now = System.currentTimeMillis();
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
        } else if (innateElement != null) {
            // Always show innate element icon above Blaze, MagmaCube, Strider, Stray, SnowGolem, Guardian, Drowned, Breeze
            display.add(innateElement);
        }

        boolean hasReaction = data != null && data.reaction != null && now < data.reactionUntil;
        boolean hasDamage = data != null && data.damageUntil > now && data.lastDamage > 0.0F;

        if (display.isEmpty() && !hasReaction && !hasDamage) return;

        double height = entity.getBbHeight() + 0.35D;
        float iconSize = 16.0F;
        float gap = 3.0F;

        // Branch 1: Draw Overhead Element Icon(s) with Pop & Fade-out Animation
        if (!display.isEmpty()) {
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

            poseStack.scale(-0.025F * popScale, -0.025F * popScale, 0.025F * popScale);

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
                        bs.endBatch(RenderType.textSeeThrough(tex));
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
            poseStack.scale(-0.030F * textPop, -0.030F * textPop, 0.030F * textPop);

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
                font.drawInBatch(
                        text,
                        textX,
                        reactY,
                        seeThroughColor,
                        false,
                        matrix,
                        buffer,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        FULL_BRIGHT
                );

                // Foreground pass with dropShadow = true for crisp visibility against any terrain
                font.drawInBatch(
                        text,
                        textX,
                        reactY,
                        fgColor,
                        true,
                        matrix,
                        buffer,
                        Font.DisplayMode.NORMAL,
                        0,
                        FULL_BRIGHT
                );
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
        int seeThroughAlpha = Math.max(10, Math.round(alpha * 0.4F));

        // Pass 1: See-through layer (always visible through geometry)
        VertexConsumer seeThrough = buffer.getBuffer(RenderType.textSeeThrough(texture));
        drawDoubleSidedQuad(seeThrough, matrix, x, y, width, height, 255, 255, 255, seeThroughAlpha);

        // Pass 2: Main vivid layer (with depth test)
        VertexConsumer main = buffer.getBuffer(RenderType.text(texture));
        drawDoubleSidedQuad(main, matrix, x, y, width, height, 255, 255, 255, alpha);
    }

    private static void drawDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, float x, float y, float width, float height, int r, int g, int b, int a) {
        // Front face
        consumer.vertex(matrix, x, y, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x, y + height, 0.0F).color(r, g, b, a).uv(0.0F, 1.0F).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x + width, y + height, 0.0F).color(r, g, b, a).uv(1.0F, 1.0F).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x + width, y, 0.0F).color(r, g, b, a).uv(1.0F, 0.0F).uv2(FULL_BRIGHT).endVertex();

        // Back face (counter-winding)
        consumer.vertex(matrix, x + width, y, 0.0F).color(r, g, b, a).uv(1.0F, 0.0F).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x + width, y + height, 0.0F).color(r, g, b, a).uv(1.0F, 1.0F).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x, y + height, 0.0F).color(r, g, b, a).uv(0.0F, 1.0F).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x, y, 0.0F).color(r, g, b, a).uv(0.0F, 0.0F).uv2(FULL_BRIGHT).endVertex();
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
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.screen instanceof EffectRenderingInventoryScreen screen && screen.canSeeEffects()) {
            return;
        }

        renderCrystallizeShieldHud(guiGraphics, mc);

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
                if (inst.getEffect().isBeneficial()) {
                    beneficialCount++;
                } else {
                    harmfulCount++;
                }
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

            // Vanilla 24x24 effect background (from inventory.png at u=141, v=166, solid background, never blinks)
            guiGraphics.blit(INVENTORY_LOCATION, x, y, 141, 166, 24, 24);

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

        renderCrystallizeShieldInventory(guiGraphics, mc, leftPos, topPos, imageWidth, screen);

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

        boolean isWide = availableWidth >= 120;
        int vanillaCount = mc.player.getActiveEffects().size();
        int totalEffects = vanillaCount + data.elements.size();
        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        int index = 0;
        for (Element elem : data.elements) {
            int y = topPos + (vanillaCount + index) * yStep;
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

            if (isWide) {
                // Wide Mode: 120x32 effect card matching vanilla inventory.png layout
                guiGraphics.blit(INVENTORY_LOCATION, startX, y, 0, 166, 120, 32);
                if (tex != null) {
                    if (alpha < 1.0F) {
                        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                    }
                    guiGraphics.blit(tex, startX + 7, y + 8, 0.0F, 0.0F, 16, 16, 16, 16);
                    if (alpha < 1.0F) {
                        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
                guiGraphics.drawString(mc.font, title, startX + 28, y + 6, getElementTextColor(elem), true);
                if (!durationStr.isEmpty()) {
                    guiGraphics.drawString(mc.font, durationStr, startX + 28, y + 17, 0x808080, false);
                }
            } else {
                // Compact Mode: 32x32 square
                guiGraphics.blit(INVENTORY_LOCATION, startX, y, 141, 166, 32, 32);
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
                    tooltip.add(net.minecraft.network.chat.Component.literal(title));
                    if (!durationStr.isEmpty()) {
                        tooltip.add(net.minecraft.network.chat.Component.literal(durationStr).withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                    guiGraphics.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY);
                }
            }

            index++;
        }
        guiGraphics.flush();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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

    private static boolean isBeneficial(MobEffectInstance inst) {
        return inst.getEffect().isBeneficial();
    }

    private static boolean isAbsorption(MobEffectInstance inst) {
        return inst.getEffect() == net.minecraft.world.effect.MobEffects.ABSORPTION;
    }

    private static void renderCrystallizeShieldHud(GuiGraphics guiGraphics, Minecraft mc) {
        Element shield = ClientAuraTracker.getActiveCrystallizeShield();
        if (shield == null || mc.player == null || !mc.player.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION)) {
            return;
        }

        int beneficialIndex = 0;
        boolean found = false;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (inst.showIcon() && isBeneficial(inst)) {
                if (isAbsorption(inst)) {
                    found = true;
                    break;
                }
                beneficialIndex++;
            }
        }

        if (found) {
            int screenWidth = guiGraphics.guiWidth();
            int top = mc.isDemo() ? 16 : 1;
            int col = beneficialIndex % 10;
            int row = beneficialIndex / 10;
            int x = screenWidth - 25 * (col + 1);
            int y = top + 26 * row;
            renderColoredBorder(guiGraphics, x, y, 24, 24, getElementBorderColor(shield));
        }
    }

    private static void renderCrystallizeShieldInventory(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int leftPos,
            int topPos,
            int imageWidth,
            EffectRenderingInventoryScreen<?> screen
    ) {
        Element shield = ClientAuraTracker.getActiveCrystallizeShield();
        if (shield == null || mc.player == null || !mc.player.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION)) {
            return;
        }

        int startX = leftPos + imageWidth + 2;
        int availableWidth = screen.width - startX;
        if (availableWidth < 32) return;

        boolean isWide = availableWidth >= 120;
        int vanillaCount = mc.player.getActiveEffects().size();
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(mc.player.getId());
        int auraCount = (data != null) ? data.elements.size() : 0;
        int totalEffects = vanillaCount + auraCount;
        int yStep = 33;
        if (totalEffects > 5) {
            yStep = Math.max(1, 132 / (totalEffects - 1));
        }

        int effectIndex = 0;
        boolean found = false;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            if (inst.showIcon()) {
                if (isAbsorption(inst)) {
                    found = true;
                    break;
                }
                effectIndex++;
            }
        }

        if (found) {
            int y = topPos + effectIndex * yStep;
            renderColoredBorder(guiGraphics, startX, y, isWide ? 120 : 32, 32, getElementBorderColor(shield));
        }
    }
}
