package com.velorise.staves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
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
            TEXTURES.put(e, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/" + e.name().toLowerCase(Locale.ROOT) + ".png"));
        }
    }

    private ClientAuraRenderer() {}

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        try {
            if (!StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.get()) return;
        } catch (Exception ignored) {}

        if (!entity.isAlive() || entity.isInvisible() || entity.isSpectator()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entity.distanceToSqr(mc.player) > 64.0D * 64.0D) return;

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(entity.getId());
        if (data == null) return;

        long now = System.currentTimeMillis();
        if (data.reactionUntil <= now) {
            data.reaction = null;
        }

        if (data.elements.isEmpty() && data.reaction == null && data.dualUntil <= now) {
            ClientAuraTracker.remove(entity.getId());
            return;
        }

        List<Element> display = new ArrayList<>(data.elements);
        if (now < data.dualUntil && data.dualTriggerElement != null && !display.contains(data.dualTriggerElement)) {
            display.add(data.dualTriggerElement);
        }

        if (display.isEmpty() && data.reaction == null) return;

        poseStack.pushPose();
        double height = entity.getBbHeight() + 0.35D;
        poseStack.translate(0.0D, height, 0.0D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        // 1. Draw Element Icons
        int count = display.size();
        float iconSize = 16.0F;
        float gap = 2.0F;

        if (count == 1) {
            drawIcon(poseStack, buffer, display.get(0), -iconSize / 2.0F, -iconSize, iconSize, iconSize);
        } else if (count == 2) {
            float totalWidth = iconSize * 2.0F + gap;
            float startX = -totalWidth / 2.0F;
            drawIcon(poseStack, buffer, display.get(0), startX, -iconSize, iconSize, iconSize);
            drawIcon(poseStack, buffer, display.get(1), startX + iconSize + gap, -iconSize, iconSize, iconSize);
        } else if (count > 2) {
            float totalWidth = count * iconSize + (count - 1) * gap;
            float startX = -totalWidth / 2.0F;
            for (int i = 0; i < count; i++) {
                drawIcon(poseStack, buffer, display.get(i), startX + i * (iconSize + gap), -iconSize, iconSize, iconSize);
            }
        }

        // Flush icon render types immediately
        if (buffer instanceof MultiBufferSource.BufferSource bs) {
            for (Element e : display) {
                ResourceLocation tex = TEXTURES.get(e);
                if (tex != null) {
                    bs.endBatch(RenderType.textSeeThrough(tex));
                    bs.endBatch(RenderType.text(tex));
                }
            }
        }

        // 2. Draw Floating Reaction Text
        if (data.reaction != null && now < data.reactionUntil) {
            float elapsed = (now - (data.reactionUntil - 1200)) / 1200.0F;
            elapsed = Math.max(0.0F, Math.min(1.0F, elapsed));
            float floatY = -iconSize - 4.0F - (elapsed * 10.0F);
            String text = data.reaction.reactionName();
            Font font = mc.font;
            float textWidth = font.width(text);
            float textX = -textWidth / 2.0F;

            int color = getReactionColor(data.reaction);
            if (elapsed > 0.7F) {
                int alpha = (int) ((1.0F - elapsed) / 0.3F * 255.0F);
                color = (color & 0x00FFFFFF) | (Math.max(10, alpha) << 24);
            } else {
                color |= 0xFF000000;
            }

            // See-through pass for reaction text
            font.drawInBatch(
                    text,
                    textX,
                    floatY,
                    (color & 0x00FFFFFF) | (0x66 << 24),
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    FULL_BRIGHT
            );

            // Foreground pass for reaction text
            font.drawInBatch(
                    text,
                    textX,
                    floatY,
                    color,
                    true,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.NORMAL,
                    0,
                    FULL_BRIGHT
            );
        }

        poseStack.popPose();
    }

    private static void drawIcon(PoseStack poseStack, MultiBufferSource buffer, Element element, float x, float y, float width, float height) {
        ResourceLocation texture = TEXTURES.get(element);
        if (texture == null) return;

        Matrix4f matrix = poseStack.last().pose();

        // Pass 1: See-through layer (always visible through geometry with 40% opacity)
        VertexConsumer seeThrough = buffer.getBuffer(RenderType.textSeeThrough(texture));
        drawDoubleSidedQuad(seeThrough, matrix, x, y, width, height, 255, 255, 255, 102);

        // Pass 2: Main vivid layer (100% opacity with depth test)
        VertexConsumer main = buffer.getBuffer(RenderType.text(texture));
        drawDoubleSidedQuad(main, matrix, x, y, width, height, 255, 255, 255, 255);
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
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE, OVERVAPE -> 0xFF8C33;
            case FORWARD_MELT, REVERSE_MELT -> 0xFF3B30;
            case OVERLOADED -> 0xAF52DE;
            case SUPERCONDUCT -> 0x5AC8FA;
            case ELECTRO_CHARGED -> 0xBF5AF2;
            case FROZEN -> 0x30D158;
            case SHATTER -> 0xE5E5EA;
            case QUICKEN -> 0xA8E063;
            case AGGRAVATE -> 0xFFD60A;
            case SPREAD -> 0x34C759;
            case BLOOM -> 0x32D74B;
            case BURNING -> 0xFF453A;
            case SWIRL -> 0x64D2FF;
            case CRYSTALLIZE -> 0xFFD60A;
            default -> 0xFFFFFF;
        };
    }
}
