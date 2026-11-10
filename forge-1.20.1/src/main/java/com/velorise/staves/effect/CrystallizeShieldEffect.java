package com.velorise.staves.effect;

import com.velorise.staves.element.Element;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
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
        entity.setAbsorptionAmount(entity.getAbsorptionAmount() + 2.0F);
        super.addAttributeModifiers(entity, attributeMap, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount() - 2.0F, 0.0F));
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
    }

    public Element getElement() {
        return element;
    }
}
