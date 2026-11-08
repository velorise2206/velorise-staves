package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

import java.util.*;

public final class ClientAuraRenderer {
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT; // 0xF000F0

    static {
        for (Element e : Element.values()) {
            TEXTURES.put(e, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/" + e.name().toLowerCase(Locale.ROOT) + "_32.png"));
        }
    }

    private ClientAuraRenderer() {}

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        render(entity, poseStack, buffer);
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
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
        boolean inDualReaction = data != null && now < data.dualUntil && (data.reactedAuraElement != null || data.dualTriggerElement != null);

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

        // Branch 2: Separate PoseStack for 3D Floating Reaction Text & Damage Numbers (Genshin Style)
        if (hasReaction || hasDamage) {
            poseStack.pushPose();
            poseStack.translate(0.0D, height, 0.0D);
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

            float textPop = 1.0F;
            long primaryAge = hasReaction ? (now - data.reactionStartTime) : (now - data.damageStartTime);
            if (primaryAge < 220L && primaryAge >= 0L) {
                textPop = 1.0F + 0.35F * (float) Math.sin((primaryAge / 220.0F) * Math.PI);
            }
            poseStack.scale(-0.028F * textPop, -0.028F * textPop, 0.028F * textPop);

            Font font = mc.font;
            float baseIconOffset = display.isEmpty() ? 0.0F : iconSize;

            if (hasReaction) {
                long reactionAge = Math.max(0L, now - data.reactionStartTime);
                long reactionDuration = Math.max(1L, data.reactionUntil - data.reactionStartTime);
                float progress = Math.max(0.0F, Math.min(1.0F, (float) reactionAge / (float) reactionDuration));

                float floatY = -baseIconOffset - 6.0F - (progress * 14.0F);
                String text = "§l" + data.reaction.reactionName();
                float textWidth = font.width(text);
                float textX = -textWidth / 2.0F;

                int baseColor = getReactionColor(data.reaction) & 0x00FFFFFF;
                int alpha = 255;
                if (progress > 0.7F) {
                    alpha = Math.max(15, Math.min(255, Math.round(((1.0F - progress) / 0.3F) * 255.0F)));
                }
                int fgColor = baseColor | (alpha << 24);
                int seeThroughAlpha = Math.max(10, Math.round(alpha * 0.45F));
                int seeThroughColor = baseColor | (seeThroughAlpha << 24);

                // See-through pass
                font.drawInBatch(text, textX, floatY, seeThroughColor, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                // Foreground pass with dropShadow = true
                font.drawInBatch(text, textX, floatY, fgColor, true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            }

            if (hasDamage) {
                long dmgAge = Math.max(0L, now - data.damageStartTime);
                long dmgDuration = Math.max(1L, data.damageUntil - data.damageStartTime);
                float dmgProgress = Math.max(0.0F, Math.min(1.0F, (float) dmgAge / (float) dmgDuration));

                float dmgY = hasReaction
                        ? (-baseIconOffset + 4.0F - (dmgProgress * 8.0F))
                        : (-baseIconOffset - 6.0F - (dmgProgress * 14.0F));
                String dmgText = "§l" + (data.lastDamage % 1.0F == 0.0F ? String.valueOf((int) data.lastDamage) : String.format(Locale.ROOT, "%.1f", data.lastDamage));
                float dmgWidth = font.width(dmgText);
                float dmgX = -dmgWidth / 2.0F;

                int dmgBaseColor = data.damageElement != null ? getElementTextColor(data.damageElement) : 0xFFFFFF;
                int dmgAlpha = 255;
                if (dmgProgress > 0.7F) {
                    dmgAlpha = Math.max(15, Math.min(255, Math.round(((1.0F - dmgProgress) / 0.3F) * 255.0F)));
                }
                int dmgFg = (dmgBaseColor & 0x00FFFFFF) | (dmgAlpha << 24);
                int dmgSeeThrough = (dmgBaseColor & 0x00FFFFFF) | (Math.max(10, Math.round(dmgAlpha * 0.45F)) << 24);

                font.drawInBatch(dmgText, dmgX, dmgY, dmgSeeThrough, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
                font.drawInBatch(dmgText, dmgX, dmgY, dmgFg, true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            }

            // Flush font batch BEFORE poseStack.popPose() so text vertices keep the billboard matrix (compatible with Sodium/Embeddium)
            if (buffer instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch();
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
            case IMMUNE -> 0xB0B0B8;
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
}
