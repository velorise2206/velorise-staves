package com.velorise.staves.effect;

import com.velorise.staves.element.CrystallizeShardManager;
import com.velorise.staves.element.Element;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class CrystallizeShieldEffect extends MobEffect {
    private final Element element;

    public CrystallizeShieldEffect(Element element, int color) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.element = element;
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        // Mutual exclusivity guard: remove any other elemental shield effect immediately
        ModMobEffects.removeAllShieldsExcept(entity, this.element);
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), 2.0F));
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        if (entity instanceof ServerPlayer player && !player.level().isClientSide) {
            var ro = ModMobEffects.getShieldForElement(this.element);
            MobEffectInstance inst = ro != null && ro.isPresent() ? player.getEffect(ro.get()) : null;
            int remainingTicks = inst != null ? inst.getDuration() : CrystallizeShardManager.SHIELD_DURATION_TICKS;
            CrystallizeShardManager.onShieldApplied(player, this.element, remainingTicks);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        float removedShield = (2.0F + (1.0F * amplifier))
                * (com.velorise.staves.element.ElementalResonanceManager.hasResonance(
                        entity, com.velorise.staves.element.ElementalResonanceManager.ResonanceType.GEO) ? 1.15F : 1.0F);
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount() - removedShield, 0.0F));
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
    }

    public Element getElement() {
        return element;
    }
}
