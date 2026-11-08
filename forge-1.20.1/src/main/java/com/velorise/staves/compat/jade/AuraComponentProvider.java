package com.velorise.staves.compat.jade;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

public enum AuraComponentProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(VeloriseStavesMod.MODID, "elemental_aura");

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

        // 1. Active reaction: [Reacted Aura] + [Trigger] ➔ §lREACTION
        if (hasActiveReaction && data != null && data.reaction != null) {
            MutableComponent reactionLine = Component.literal("");
            Element rAura = data.reactedAuraElement;
            Element trig = data.dualTriggerElement;

            if (rAura != null) {
                reactionLine.append(getElementBadge(rAura)).append(" ");
            }
            if (trig != null) {
                reactionLine.append(Component.literal("+ ").withStyle(ChatFormatting.GRAY))
                            .append(getElementBadge(trig)).append(" ");
            }
            reactionLine.append(Component.literal("➔ ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(data.reaction.reactionName()).withStyle(data.reaction.color(), ChatFormatting.BOLD));

            tooltip.add(reactionLine);
            return;
        }

        // 2. Dual aura transition
        if (hasDualAnimation && data != null) {
            MutableComponent dualLine = Component.literal("");
            dualLine.append(getElementBadge(data.reactedAuraElement))
                    .append(Component.literal(" + ").withStyle(ChatFormatting.GRAY))
                    .append(getElementBadge(data.dualTriggerElement));
            tooltip.add(dualLine);
            return;
        }

        // 3. Standard active auras (shows dual co-existing elements like Quicken: Dendro + Electro, or Electro-Charged: Hydro + Electro)
        if (!activeElements.isEmpty()) {
            MutableComponent auraLine = Component.literal("");
            for (int i = 0; i < activeElements.size(); i++) {
                Element elem = activeElements.get(i);
                if (i > 0) {
                    auraLine.append(Component.literal(" + ").withStyle(ChatFormatting.GRAY));
                }
                auraLine.append(getElementBadge(elem));
                if (elem == innate) {
                    auraLine.append(Component.literal(" (Innate)").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            tooltip.add(auraLine);
        }
    }

    private static Component getElementBadge(Element elem) {
        return switch (elem) {
            case PYRO -> Component.literal("🔥 Pyro").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            case HYDRO -> Component.literal("💧 Hydro").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD);
            case CRYO -> Component.literal("❄ Cryo").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            case ELECTRO -> Component.literal("⚡ Electro").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            case DENDRO -> Component.literal("🌿 Dendro").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            case ANEMO -> Component.literal("🌀 Anemo").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
            case GEO -> Component.literal("🛡 Geo").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        };
    }
}
