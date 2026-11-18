package com.velorise.staves.effect;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.CrystallizeShardManager;
import com.velorise.staves.element.Element;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class CrystallizeShieldEffect extends MobEffect {
    private final Element element;

    public CrystallizeShieldEffect(Element element, int color) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.element = element;
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                VeloriseStavesMod.MODID,
                "effect." + element.name().toLowerCase(java.util.Locale.ROOT) + "_shield_absorption"
        );
        // Allow sufficient MAX_ABSORPTION headroom for EM + Geo Resonance (+15%) shields
        this.addAttributeModifier(Attributes.MAX_ABSORPTION, modifierId, 3.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        // Mutual exclusivity guard: remove any other elemental shield effect immediately
        ModMobEffects.removeAllShieldsExcept(entity, this.element);
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), 2.0F));

        if (entity instanceof ServerPlayer sp && !sp.level().isClientSide()) {
            MobEffectInstance inst = sp.getEffect(ModMobEffects.getShieldForElement(this.element));
            int remainingTicks = inst != null ? inst.getDuration() : CrystallizeShardManager.SHIELD_DURATION_TICKS;
            CrystallizeShardManager.onShieldApplied(sp, this.element, remainingTicks);
        }
    }

    public Element getElement() {
        return element;
    }
}
