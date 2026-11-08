package com.velorise.staves.compat.jade;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum AuraComponentProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(VeloriseStavesMod.MODID, "elemental_aura");
    private static final Map<Element, ResourceLocation> TEXTURES = new EnumMap<>(Element.class);

    static {
        for (Element e : Element.values()) {
            TEXTURES.put(e, new ResourceLocation(VeloriseStavesMod.MODID, "textures/element/" + e.name().toLowerCase(Locale.ROOT) + ".png"));
        }
    }

    public static class ElementIcon extends snownee.jade.api.ui.Element {
        private final ResourceLocation texture;
        private final net.minecraft.world.phys.Vec2 size;

        public ElementIcon(ResourceLocation texture, int width, int height) {
            this.texture = texture;
            this.size = new net.minecraft.world.phys.Vec2(width, height);
        }

        @Override
        public net.minecraft.world.phys.Vec2 getSize() {
            return this.size;
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
            if (this.texture == null) return;
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            guiGraphics.blit(this.texture, Math.round(x), Math.round(y), 0.0F, 0.0F, (int) this.size.x, (int) this.size.y, (int) this.size.x, (int) this.size.y);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (!living.isAlive()) return;

        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(living.getId());
        Element innate = ElementalGaugeManager.getInnateElement(living);

        List<Element> activeElements = new ArrayList<>();
        if (data != null && !data.elements.isEmpty()) {
            activeElements.addAll(data.elements);
        } else if (innate != null) {
            activeElements.add(innate);
        }

        long now = System.currentTimeMillis();
        boolean hasActiveReaction = data != null && data.reaction != null && now < data.reactionUntil;
        boolean hasDualAnimation = data != null && data.reactedAuraElement != null && data.dualTriggerElement != null && now < data.dualUntil;

        IElementHelper helper = IElementHelper.get();

        // 1. Active reaction: [Reacted Aura Icon] Aura + [Trigger Icon] Trigger ➔ §lREACTION
        if (hasActiveReaction && data != null && data.reaction != null) {
            Element rAura = data.reactedAuraElement;
            Element trig = data.dualTriggerElement;

            if (rAura != null) {
                appendElementBadge(tooltip, rAura, true);
                if (trig != null) {
                    tooltip.append(Component.literal(" + ").withStyle(ChatFormatting.GRAY));
                    appendElementBadge(tooltip, trig, false);
                }
            } else if (trig != null) {
                appendElementBadge(tooltip, trig, true);
            }

            tooltip.append(Component.literal(" ➔ ").withStyle(ChatFormatting.WHITE));
            tooltip.append(Component.literal(data.reaction.reactionName()).withStyle(data.reaction.color(), ChatFormatting.BOLD));
            return;
        }

        // 2. Dual aura transition: [Aura 1 Icon] Aura 1 + [Aura 2 Icon] Aura 2
        if (hasDualAnimation && data != null && data.reactedAuraElement != null && data.dualTriggerElement != null) {
            appendElementBadge(tooltip, data.reactedAuraElement, true);
            tooltip.append(Component.literal(" + ").withStyle(ChatFormatting.GRAY));
            appendElementBadge(tooltip, data.dualTriggerElement, false);
            return;
        }

        // 3. Standard active auras (shows dual co-existing elements like Quicken or Electro-Charged)
        if (!activeElements.isEmpty()) {
            for (int i = 0; i < activeElements.size(); i++) {
                Element elem = activeElements.get(i);
                if (i == 0) {
                    appendElementBadge(tooltip, elem, true);
                } else {
                    tooltip.append(Component.literal(" + ").withStyle(ChatFormatting.GRAY));
                    appendElementBadge(tooltip, elem, false);
                }
                if (elem == innate) {
                    tooltip.append(Component.literal(" (Innate)").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
    }

    private static void appendElementBadge(ITooltip tooltip, Element elem, boolean startOfLine) {
        ResourceLocation iconTex = TEXTURES.get(elem);
        IElementHelper helper = IElementHelper.get();
        if (startOfLine) {
            tooltip.add(new ElementIcon(iconTex, 10, 10));
        } else {
            tooltip.append(new ElementIcon(iconTex, 10, 10));
        }
        tooltip.append(helper.spacer(2, 0));
        tooltip.append(Component.literal(elem.displayName()).withStyle(elem.color(), ChatFormatting.BOLD));
    }
}
