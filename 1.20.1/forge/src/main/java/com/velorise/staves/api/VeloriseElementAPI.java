package com.velorise.staves.api;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.ElementalReaction;
import com.velorise.staves.element.ElementalShieldManager;
import com.velorise.staves.element.ElementalTickManager;
import com.velorise.staves.element.InnateMobManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Stable public facade for third-party Minecraft mods, addons, and KubeJS scripts to interact with
 * Velorise's Staves Elemental Gauge Theory (EGT) engine, Innate Auras, and Elemental Shields.
 *
 * <p>KubeJS scripts can invoke the String-based overloads directly via:
 * {@code const VeloriseAPI = Java.loadClass('com.velorise.staves.api.VeloriseElementAPI');}
 */
public final class VeloriseElementAPI {

    private VeloriseElementAPI() {}

    /**
     * Parses an element name (case-insensitive, e.g. "pyro", "CRYO", "velorise_staves:hydro") into an {@link Element}.
     */
    public static @Nullable Element parseElement(@Nullable String elementId) {
        if (elementId == null || elementId.isBlank()) return null;
        String clean = elementId.trim();
        int colon = clean.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < clean.length()) {
            clean = clean.substring(colon + 1);
        }
        try {
            return Element.valueOf(clean.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // =========================================================================
    // 1. APPLY ELEMENTAL AURA (0 DAMAGE)
    // =========================================================================

    public static ReactionResult applyAura(
            LivingEntity target,
            Element element,
            float gaugeUnits,
            @Nullable LivingEntity attacker
    ) {
        return applyAura(target, element, gaugeUnits, attacker, IcdPolicy.STANDARD);
    }

    public static ReactionResult applyAura(
            LivingEntity target,
            Element element,
            float gaugeUnits,
            @Nullable LivingEntity attacker,
            IcdPolicy icdPolicy
    ) {
        return dealElementalDamage(target, attacker, element, 0.0F, gaugeUnits, icdPolicy);
    }

    /** KubeJS / scripting-friendly overload using an element name string (e.g. "cryo"). */
    public static ReactionResult applyAura(
            LivingEntity target,
            String elementId,
            float gaugeUnits,
            @Nullable LivingEntity attacker
    ) {
        Element elem = parseElement(elementId);
        return elem == null ? ReactionResult.EMPTY : applyAura(target, elem, gaugeUnits, attacker, IcdPolicy.STANDARD);
    }

    // =========================================================================
    // 2. DEAL ELEMENTAL DAMAGE (FULL EGT + REACTIONS + POP-UPS)
    // =========================================================================

    public static ReactionResult dealElementalDamage(
            LivingEntity target,
            @Nullable LivingEntity attacker,
            Element element,
            float baseDamage,
            float gaugeUnits
    ) {
        return dealElementalDamage(target, attacker, element, baseDamage, gaugeUnits, IcdPolicy.STANDARD);
    }

    public static ReactionResult dealElementalDamage(
            LivingEntity target,
            @Nullable LivingEntity attacker,
            Element element,
            float baseDamage,
            float gaugeUnits,
            IcdPolicy icdPolicy
    ) {
        if (target == null || element == null || !(target.level() instanceof ServerLevel level)) {
            return ReactionResult.EMPTY;
        }
        boolean bypassIcd = (icdPolicy == IcdPolicy.BYPASS);
        return ElementalGaugeManager.applyElementFromEntity(
                level, target, element, Math.max(0.0F, gaugeUnits), attacker, Math.max(0.0F, baseDamage), bypassIcd
        );
    }

    /** KubeJS / scripting-friendly overload using an element name string (e.g. "pyro"). */
    public static ReactionResult dealElementalDamage(
            LivingEntity target,
            @Nullable LivingEntity attacker,
            String elementId,
            float baseDamage,
            float gaugeUnits
    ) {
        Element elem = parseElement(elementId);
        return elem == null ? ReactionResult.EMPTY : dealElementalDamage(target, attacker, elem, baseDamage, gaugeUnits, IcdPolicy.STANDARD);
    }

    // =========================================================================
    // 3. INNATE ELEMENTS & OMNI-INNATE
    // =========================================================================

    /**
     * Assigns (or clears if {@code null}) a permanent Innate Element on any living entity.
     */
    public static void setInnateElement(LivingEntity entity, @Nullable Element element) {
        if (entity == null || entity.level().isClientSide()) return;
        InnateMobManager.setExplicitInnateElement(entity, element);
    }

    /** KubeJS / scripting-friendly overload. Pass null or "none" to clear. */
    public static void setInnateElement(LivingEntity entity, @Nullable String elementId) {
        if (entity == null || entity.level().isClientSide()) return;
        if (elementId == null || elementId.isBlank() || elementId.equalsIgnoreCase("none")) {
            clearInnateElement(entity);
            return;
        }
        Element elem = parseElement(elementId);
        if (elem != null) {
            setInnateElement(entity, elem);
        }
    }

    public static void clearInnateElement(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        InnateMobManager.setExplicitInnateElement(entity, null);
    }

    public static @Nullable Element getInnateElement(@Nullable LivingEntity entity) {
        return entity == null ? null : InnateMobManager.getInnateElement(entity);
    }

    public static void setOmniInnate(LivingEntity entity, boolean omni) {
        if (entity == null || entity.level().isClientSide()) return;
        InnateMobManager.setOmniInnate(entity, omni);
    }

    public static boolean isOmniInnate(@Nullable LivingEntity entity) {
        return InnateMobManager.isOmniInnate(entity);
    }

    // =========================================================================
    // 4. ELEMENTAL SHIELD GAUGE (BOSSES & MOBS)
    // =========================================================================

    public static void setElementalShield(LivingEntity entity, Element shieldElement, float maxUnits) {
        ElementalShieldManager.setShield(entity, shieldElement, maxUnits);
    }

    public static void setElementalShield(
            LivingEntity entity,
            Element shieldElement,
            float maxUnits,
            boolean absorbHealthDamage,
            float hpDamagePassThrough
    ) {
        ElementalShieldManager.setShield(entity, shieldElement, maxUnits, absorbHealthDamage, hpDamagePassThrough);
    }

    /** KubeJS / scripting-friendly overload. */
    public static void setElementalShield(LivingEntity entity, String elementId, float maxUnits) {
        Element elem = parseElement(elementId);
        if (elem != null) {
            setElementalShield(entity, elem, maxUnits);
        }
    }

    public static void clearElementalShield(@Nullable LivingEntity entity) {
        ElementalShieldManager.clearShield(entity);
    }

    public static boolean hasElementalShield(@Nullable LivingEntity entity) {
        return ElementalShieldManager.hasShield(entity);
    }

    public static @Nullable Element getShieldElement(@Nullable LivingEntity entity) {
        return ElementalShieldManager.getShieldElement(entity);
    }

    public static float getRemainingShieldUnits(@Nullable LivingEntity entity) {
        return ElementalShieldManager.getRemainingUnits(entity);
    }

    public static float getMaxShieldUnits(@Nullable LivingEntity entity) {
        return ElementalShieldManager.getMaxUnits(entity);
    }

    public static float getShieldProgress(@Nullable LivingEntity entity) {
        return ElementalShieldManager.getProgress(entity);
    }

    // =========================================================================
    // 5. AURA QUERIES & STATE CLEANUP
    // =========================================================================

    public static boolean hasAura(@Nullable LivingEntity target, @Nullable Element element) {
        return target != null && element != null && ElementalGaugeManager.hasElement(target, element);
    }

    public static boolean hasAura(@Nullable LivingEntity target, @Nullable String elementId) {
        Element elem = parseElement(elementId);
        return elem != null && hasAura(target, elem);
    }

    public static float getAuraUnits(@Nullable LivingEntity target, @Nullable Element element) {
        return (target == null || element == null) ? 0.0F : ElementalGaugeManager.getAuraUnits(target, element);
    }

    public static Collection<Element> getActiveAuras(@Nullable LivingEntity target) {
        if (target == null) return Collections.emptyList();
        return ElementalGaugeManager.getEntityElements(target.getUUID());
    }

    public static boolean isFrozen(@Nullable LivingEntity target) {
        return ElementalGaugeManager.isEntityFrozen(target);
    }

    public static boolean isQuickened(@Nullable LivingEntity target) {
        return ElementalGaugeManager.isEntityQuickened(target);
    }

    public static void clearAuras(@Nullable LivingEntity target) {
        if (target == null) return;
        ElementalTickManager.clearEntityStateInternal(target);
    }
}
