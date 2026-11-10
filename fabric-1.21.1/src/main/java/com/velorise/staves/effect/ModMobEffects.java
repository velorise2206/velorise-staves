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
        Element active = null;
        for (Element elem : new Element[]{Element.PYRO, Element.HYDRO, Element.ELECTRO, Element.CRYO}) {
            var holder = getShieldForElement(elem);
            if (holder != null && entity.hasEffect(holder)) {
                if (active == null) {
                    active = elem;
                } else if (!entity.level().isClientSide()) {
                    // Mutual exclusivity safety guard: if multiple shield effects exist, remove the extras immediately
                    entity.removeEffect(holder);
                }
            }
        }
        return active;
    }

    public static void removeAllShields(LivingEntity entity) {
        removeAllShieldsExcept(entity, null);
    }

    public static void removeAllShieldsExcept(LivingEntity entity, @Nullable Element keepElement) {
        for (Element elem : new Element[]{Element.PYRO, Element.HYDRO, Element.ELECTRO, Element.CRYO}) {
            if (elem != keepElement) {
                var holder = getShieldForElement(elem);
                if (holder != null && entity.hasEffect(holder)) {
                    entity.removeEffect(holder);
                }
            }
        }
    }
}
