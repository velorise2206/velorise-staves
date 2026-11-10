package com.velorise.staves.effect;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        Element latest = null;
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            latest = com.velorise.staves.element.CrystallizeShardManager.getActiveShield(sp.getUUID(), sp.level().getGameTime());
        }
        if (latest == null && entity.level().isClientSide()) {
            latest = com.velorise.staves.client.ClientAuraTracker.getTrackedCrystallizeShield();
        }

        Element found = null;
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (net.minecraft.world.effect.MobEffectInstance inst : entity.getActiveEffects()) {
            Element elem = getShieldElement(inst.getEffect().value());
            if (elem != null) {
                if (latest != null) {
                    if (elem == latest) {
                        found = elem;
                    } else {
                        toRemove.add(inst.getEffect());
                    }
                } else {
                    if (found == null) {
                        found = elem;
                    } else {
                        toRemove.add(inst.getEffect());
                    }
                }
            }
        }
        for (Holder<MobEffect> h : toRemove) {
            entity.removeEffect(h);
        }
        return latest != null ? latest : found;
    }

    public static void removeAllShields(LivingEntity entity) {
        removeAllShieldsExcept(entity, null);
    }

    public static void removeAllShieldsExcept(LivingEntity entity, @Nullable Element keepElement) {
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (net.minecraft.world.effect.MobEffectInstance inst : entity.getActiveEffects()) {
            Element elem = getShieldElement(inst.getEffect().value());
            if (elem != null && elem != keepElement) {
                toRemove.add(inst.getEffect());
            }
        }
        for (Holder<MobEffect> h : toRemove) {
            entity.removeEffect(h);
        }
    }
}
