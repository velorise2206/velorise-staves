package com.velorise.staves.effect;

import com.velorise.staves.element.Element;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class CrystallizeShieldEffect extends MobEffect {
    private final Element element;

    public CrystallizeShieldEffect(Element element, int color) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
