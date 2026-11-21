package com.velorise.staves.api;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loader-agnostic event callback registry for Velorise's Staves Elemental Gauge Theory (EGT) engine.
 * Works identically across NeoForge 1.21.1, Fabric 1.21.1, and Forge 1.20.1 without loader-specific event buses.
 */
public final class VeloriseElementEvents {

    private VeloriseElementEvents() {}

    @FunctionalInterface
    public interface PreAuraListener {
        /**
         * Invoked before an elemental application is processed on a target.
         *
         * @return {@code false} to cancel elemental application, {@code true} to allow it.
         */
        boolean onPreApplyAura(
                ServerLevel level,
                LivingEntity target,
                @Nullable LivingEntity attacker,
                Element element,
                float gaugeUnits
        );
    }

    @FunctionalInterface
    public interface ReactionListener {
        /**
         * Invoked whenever an elemental reaction executes on a target.
         */
        void onReactionTriggered(
                ServerLevel level,
                LivingEntity target,
                @Nullable LivingEntity attacker,
                Element triggerElement,
                ElementalReaction reaction,
                float reactedUnits
        );
    }

    @FunctionalInterface
    public interface ShieldBreakListener {
        /**
         * Invoked when an entity's active Elemental Shield Gauge reaches 0U and shatters.
         */
        void onShieldBroken(
                ServerLevel level,
                LivingEntity target,
                @Nullable LivingEntity attacker,
                Element shieldElement,
                @Nullable Element breakerElement
        );
    }

    private static final List<PreAuraListener> PRE_AURA_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<ReactionListener> REACTION_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<ShieldBreakListener> SHIELD_BREAK_LISTENERS = new CopyOnWriteArrayList<>();

    public static void registerPreAuraListener(PreAuraListener listener) {
        if (listener != null && !PRE_AURA_LISTENERS.contains(listener)) {
            PRE_AURA_LISTENERS.add(listener);
        }
    }

    public static void unregisterPreAuraListener(PreAuraListener listener) {
        PRE_AURA_LISTENERS.remove(listener);
    }

    public static void registerReactionListener(ReactionListener listener) {
        if (listener != null && !REACTION_LISTENERS.contains(listener)) {
            REACTION_LISTENERS.add(listener);
        }
    }

    public static void unregisterReactionListener(ReactionListener listener) {
        REACTION_LISTENERS.remove(listener);
    }

    public static void registerShieldBreakListener(ShieldBreakListener listener) {
        if (listener != null && !SHIELD_BREAK_LISTENERS.contains(listener)) {
            SHIELD_BREAK_LISTENERS.add(listener);
        }
    }

    public static void unregisterShieldBreakListener(ShieldBreakListener listener) {
        SHIELD_BREAK_LISTENERS.remove(listener);
    }

    public static boolean firePreApplyAura(
            ServerLevel level,
            LivingEntity target,
            @Nullable LivingEntity attacker,
            Element element,
            float gaugeUnits
    ) {
        for (PreAuraListener listener : PRE_AURA_LISTENERS) {
            if (!listener.onPreApplyAura(level, target, attacker, element, gaugeUnits)) {
                return false;
            }
        }
        return true;
    }

    public static void fireReactionTriggered(
            ServerLevel level,
            LivingEntity target,
            @Nullable LivingEntity attacker,
            Element triggerElement,
            ElementalReaction reaction,
            float reactedUnits
    ) {
        if (reaction == null || reaction == ElementalReaction.NONE) return;
        for (ReactionListener listener : REACTION_LISTENERS) {
            listener.onReactionTriggered(level, target, attacker, triggerElement, reaction, reactedUnits);
        }
    }

    public static void fireShieldBroken(
            ServerLevel level,
            LivingEntity target,
            @Nullable LivingEntity attacker,
            Element shieldElement,
            @Nullable Element breakerElement
    ) {
        for (ShieldBreakListener listener : SHIELD_BREAK_LISTENERS) {
            listener.onShieldBroken(level, target, attacker, shieldElement, breakerElement);
        }
    }
}
