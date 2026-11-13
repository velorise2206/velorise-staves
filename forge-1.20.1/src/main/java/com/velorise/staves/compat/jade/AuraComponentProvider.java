package com.velorise.staves.compat.jade;

import com.velorise.staves.client.ClientAuraRenderer;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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
import java.util.UUID;

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
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(this.texture, Math.round(x), Math.round(y), 0.0F, 0.0F, (int) this.size.x, (int) this.size.y, (int) this.size.x, (int) this.size.y);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }


    private static ChatFormatting getElementChatFormatting(Element element) {
        return switch (element) {
            case PYRO -> ChatFormatting.RED;
            case HYDRO -> ChatFormatting.AQUA;
            case ELECTRO -> ChatFormatting.LIGHT_PURPLE;
            case CRYO -> ChatFormatting.WHITE;
            case DENDRO -> ChatFormatting.GREEN;
            case ANEMO -> ChatFormatting.DARK_AQUA;
            case GEO -> ChatFormatting.GOLD;
        };
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (!living.isAlive()) return;

        CompoundTag serverDataCheck = accessor.getServerData();
        boolean isOmni = ElementalGaugeManager.isOmniInnate(living)
                || ClientAuraTracker.hasOmniInnate(living.getId())
                || (serverDataCheck != null && serverDataCheck.getBoolean("vs_omni"));
        if (isOmni) {
            IElementHelper helper = IElementHelper.get();
            tooltip.add(new ElementIcon(ClientAuraRenderer.getOmniIconTexture(), 12, 12));
            tooltip.append(helper.spacer(2, 0));
            Component comp = com.velorise.staves.element.ElementalDeathMessageHelper.createChromaticOmniComponent()
                    .append(Component.literal(" ∞").withStyle(ChatFormatting.GOLD));
            tooltip.append(helper.text(comp));
            return;
        }

        java.util.EnumSet<Element> innateSet = ElementalGaugeManager.getInnateElements(living);
        CompoundTag serverData = accessor.getServerData();
        if (serverData != null && serverData.contains("vs_innate_mask")) {
            byte mask = serverData.getByte("vs_innate_mask");
            for (Element e : Element.values()) {
                if ((mask & (1 << e.ordinal())) != 0) {
                    innateSet.add(e);
                }
            }
        } else if (serverData != null && serverData.contains("vs_innate")) {
            try {
                innateSet.add(Element.valueOf(serverData.getString("vs_innate")));
            } catch (IllegalArgumentException ignored) {}
        }
        ClientAuraTracker.AuraDisplayData data = ClientAuraTracker.get(living.getId());
        if (data != null) {
            if (data.permanentInnateElement != null) {
                innateSet.add(data.permanentInnateElement);
            }
            innateSet.addAll(data.permanentInnateElements);
        }

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
        } else if (data != null && !data.elements.isEmpty() && (data.expireTime <= 0L || now < data.expireTime)) {
            display.addAll(data.elements);
        }
        for (Element innateElem : innateSet) {
            if (!display.contains(innateElem)) {
                display.add(innateElem);
            }
        }

        boolean envWet = (serverData != null && serverData.getBoolean("vs_env_wet"))
                || (!ElementalGaugeManager.isPassiveAquaticOrAmbient(living) && ElementalGaugeManager.isEntityInWater(living));
        if (envWet && !display.contains(Element.HYDRO)) {
            display.add(Element.HYDRO);
        }

        if (display.isEmpty()) return;

        CompoundTag gaugesTag = (serverData != null && serverData.contains("vs_gauges")) ? serverData.getCompound("vs_gauges") : null;

        IElementHelper helper = IElementHelper.get();
        for (int i = 0; i < Math.min(3, display.size()); i++) {
            Element elem = display.get(i);
            ResourceLocation tex = TEXTURES.get(elem);
            if (tex == null) continue;

            if (i == 0) {
                tooltip.add(new ElementIcon(tex, 12, 12));
            } else {
                tooltip.append(helper.spacer(5, 0));
                tooltip.append(new ElementIcon(tex, 12, 12));
            }
            tooltip.append(helper.spacer(2, 0));

            // Format gauge unit text (e.g. 2.5U, Wet (Env), or ∞)
            String uText;
            if (innateSet.contains(elem)) {
                uText = "∞";
            } else if (elem == Element.HYDRO && envWet && (gaugesTag == null || !gaugesTag.contains(Element.HYDRO.name()))) {
                uText = Component.translatable("jade.velorise_staves.env_wet").getString();
            } else if (gaugesTag != null && gaugesTag.contains(elem.name())) {
                float u = gaugesTag.getFloat(elem.name());
                uText = String.format(Locale.ROOT, "%.1fU", u);
            } else if (gaugesTag != null && elem == Element.DENDRO && gaugesTag.contains("QUICKEN")) {
                float u = gaugesTag.getFloat("QUICKEN");
                uText = String.format(Locale.ROOT, "%.1fU", u);
            } else if (gaugesTag != null && elem == Element.CRYO && gaugesTag.contains("FROZEN")) {
                float u = gaugesTag.getFloat("FROZEN");
                uText = String.format(Locale.ROOT, "%.1fU", u);
            } else if (data != null && data.expireTime > 0) {
                long remMs = Math.max(0, data.expireTime - now);
                float remSec = remMs / 1000.0F;
                float estU = Math.max(0.1F, (remSec - 7.0F) / 2.5F);
                uText = String.format(Locale.ROOT, "%.1fU", estU);
            } else {
                uText = "1.0U";
            }

            Component comp = Component.literal(uText).withStyle(getElementChatFormatting(elem));
            tooltip.append(helper.text(comp));
        }
    }
}
