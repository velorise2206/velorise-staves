package com.velorise.staves.compat.jade;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
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

        Element innateElement = ElementalGaugeManager.getInnateElement(living);
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(living.getId());

        long now = System.currentTimeMillis();
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
            display.add(innateElement);
        }

        if (display.isEmpty()) return;

        IElementHelper helper = IElementHelper.get();
        // Clean display matching overhead style: 1 icon or 2 icons side-by-side
        tooltip.add(new ElementIcon(TEXTURES.get(display.get(0)), 12, 12));
        if (display.size() > 1) {
            tooltip.append(helper.spacer(3, 0));
            tooltip.append(new ElementIcon(TEXTURES.get(display.get(1)), 12, 12));
        }
    }
}
