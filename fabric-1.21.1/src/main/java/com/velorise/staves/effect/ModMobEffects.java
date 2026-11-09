package com.velorise.staves.effect;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class ModMobEffects {
    public static final Holder<MobEffect> PYRO_SHIELD = register("pyro_shield", new CrystallizeShieldEffect(Element.PYRO, 0xFF4500));
    public static final Holder<MobEffect> HYDRO_SHIELD = register("hydro_shield", new CrystallizeShieldEffect(Element.HYDRO, 0x00C8FF));
    public static final Holder<MobEffect> ELECTRO_SHIELD = register("electro_shield", new CrystallizeShieldEffect(Element.ELECTRO, 0xBF40FF));
    public static final Holder<MobEffect> CRYO_SHIELD = register("cryo_shield", new CrystallizeShieldEffect(Element.CRYO, 0x4DF0FF));

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, name), effect);
    }

    public static void init() {}

    private ModMobEffects() {}

    public static @Nullable Holder<MobEffect> getShieldForElement(Element element) {
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
