package com.velorise.staves.effect;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, VeloriseStavesMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> PYRO_SHIELD = MOB_EFFECTS.register("pyro_shield",
            () -> new CrystallizeShieldEffect(Element.PYRO, 0xFF4500));
    public static final DeferredHolder<MobEffect, MobEffect> HYDRO_SHIELD = MOB_EFFECTS.register("hydro_shield",
            () -> new CrystallizeShieldEffect(Element.HYDRO, 0x00C8FF));
    public static final DeferredHolder<MobEffect, MobEffect> ELECTRO_SHIELD = MOB_EFFECTS.register("electro_shield",
            () -> new CrystallizeShieldEffect(Element.ELECTRO, 0xBF40FF));
    public static final DeferredHolder<MobEffect, MobEffect> CRYO_SHIELD = MOB_EFFECTS.register("cryo_shield",
            () -> new CrystallizeShieldEffect(Element.CRYO, 0x4DF0FF));

    private ModMobEffects() {}

    public static @Nullable DeferredHolder<MobEffect, MobEffect> getShieldForElement(Element element) {
        return switch (element) {
            case PYRO -> PYRO_SHIELD;
            case HYDRO -> HYDRO_SHIELD;
            case ELECTRO -> ELECTRO_SHIELD;
            case CRYO -> CRYO_SHIELD;
            default -> null;
        };
    }

    public static @Nullable Element getShieldElement(MobEffect effect) {
        if (effect instanceof CrystallizeShieldEffect cse) {
            return cse.getElement();
        }
        return null;
    }

    public static @Nullable Element getActiveShield(LivingEntity entity) {
        if (entity.hasEffect(PYRO_SHIELD)) return Element.PYRO;
        if (entity.hasEffect(HYDRO_SHIELD)) return Element.HYDRO;
        if (entity.hasEffect(ELECTRO_SHIELD)) return Element.ELECTRO;
        if (entity.hasEffect(CRYO_SHIELD)) return Element.CRYO;
        return null;
    }

    public static void removeAllShields(LivingEntity entity) {
        entity.removeEffect(PYRO_SHIELD);
        entity.removeEffect(HYDRO_SHIELD);
        entity.removeEffect(ELECTRO_SHIELD);
        entity.removeEffect(CRYO_SHIELD);
    }
}
