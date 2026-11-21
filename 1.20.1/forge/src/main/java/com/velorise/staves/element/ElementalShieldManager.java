package com.velorise.staves.element;

import com.velorise.staves.api.VeloriseElementEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Genshin-authentic Elemental Shield Gauges (measured in Gauge Units, U) on bosses and mobs.
 * Counter-elements deplete shield units faster according to elemental matchup multipliers and fire
 * {@link VeloriseElementEvents#fireShieldBroken} when depleted.
 */
public final class ElementalShieldManager {

    private ElementalShieldManager() {}

    public static final class ShieldState {
        private final Element element;
        private final float maxUnits;
        private float currentUnits;
        private final boolean absorbHealthDamage;
        private final float hpDamagePassThrough;

        public ShieldState(Element element, float maxUnits, boolean absorbHealthDamage, float hpDamagePassThrough) {
            this.element = element;
            this.maxUnits = Math.max(0.1F, maxUnits);
            this.currentUnits = this.maxUnits;
            this.absorbHealthDamage = absorbHealthDamage;
            this.hpDamagePassThrough = Math.max(0.0F, Math.min(1.0F, hpDamagePassThrough));
        }

        public Element getElement() { return element; }
        public float getMaxUnits() { return maxUnits; }
        public float getCurrentUnits() { return currentUnits; }
        public boolean isAbsorbHealthDamage() { return absorbHealthDamage; }
        public float getHpDamagePassThrough() { return hpDamagePassThrough; }

        public float getProgress() {
            return maxUnits <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, currentUnits / maxUnits));
        }

        public boolean consume(float units) {
            if (units <= 0.0F) return false;
            currentUnits = Math.max(0.0F, currentUnits - units);
            return currentUnits <= 0.0001F;
        }
    }

    public record ShieldHitOutcome(
            boolean hadShield,
            boolean immune,
            boolean broken,
            float remainingUnits,
            float damageScale
    ) {
        public static final ShieldHitOutcome NO_SHIELD = new ShieldHitOutcome(false, false, false, 0.0F, 1.0F);
    }

    private static final Map<UUID, ShieldState> SHIELDS = new ConcurrentHashMap<>();

    public static void setShield(LivingEntity entity, Element element, float maxUnits) {
        setShield(entity, element, maxUnits, true, 0.15F);
    }

    public static void setShield(
            LivingEntity entity,
            Element element,
            float maxUnits,
            boolean absorbHealthDamage,
            float hpDamagePassThrough
    ) {
        if (entity == null || element == null || maxUnits <= 0.0F) return;
        ShieldState state = new ShieldState(element, maxUnits, absorbHealthDamage, hpDamagePassThrough);
        SHIELDS.put(entity.getUUID(), state);
        if (!entity.level().isClientSide() && element.isAura()) {
            Map<Element, GaugeState> gauges = ElementalGaugeManager.GAUGES.computeIfAbsent(entity.getUUID(), k -> new ConcurrentHashMap<>());
            gauges.put(element, new GaugeState(element, Math.max(2.0F, maxUnits)));
            ElementalGaugeManager.syncAuras(entity, gauges.keySet(), element, null, null, 0.0F);
        }
    }

    public static void clearShield(@Nullable LivingEntity entity) {
        if (entity == null) return;
        SHIELDS.remove(entity.getUUID());
    }

    public static void clearEntity(@Nullable UUID id) {
        if (id == null) return;
        SHIELDS.remove(id);
    }

    public static void clearAll() {
        SHIELDS.clear();
    }

    public static boolean hasShield(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        ShieldState s = SHIELDS.get(entity.getUUID());
        return s != null && s.getCurrentUnits() > 0.0001F;
    }

    public static @Nullable ShieldState getShield(@Nullable LivingEntity entity) {
        return entity == null ? null : SHIELDS.get(entity.getUUID());
    }

    public static @Nullable Element getShieldElement(@Nullable LivingEntity entity) {
        ShieldState s = getShield(entity);
        return s != null ? s.getElement() : null;
    }

    public static float getRemainingUnits(@Nullable LivingEntity entity) {
        ShieldState s = getShield(entity);
        return s != null ? s.getCurrentUnits() : 0.0F;
    }

    public static float getMaxUnits(@Nullable LivingEntity entity) {
        ShieldState s = getShield(entity);
        return s != null ? s.getMaxUnits() : 0.0F;
    }

    public static float getProgress(@Nullable LivingEntity entity) {
        ShieldState s = getShield(entity);
        return s != null ? s.getProgress() : 0.0F;
    }

    /**
     * Genshin-aligned elemental shield consumption multipliers.
     * Strong counters (Pyro -> Cryo, Hydro -> Pyro, Cryo -> Electro, Dendro -> Hydro) deal 2.0x gauge drain.
     */
    public static float getShieldConsumptionMultiplier(Element shieldElem, Element attackElem) {
        if (shieldElem == null || attackElem == null) return 0.0F;
        if (shieldElem == attackElem) {
            return shieldElem == Element.GEO ? 1.5F : 0.0F;
        }
        return switch (shieldElem) {
            case CRYO -> switch (attackElem) {
                case PYRO -> 2.0F;
                case ELECTRO -> 1.0F;
                case GEO, ANEMO -> 0.5F;
                case HYDRO, DENDRO -> 0.25F;
                default -> 0.0F;
            };
            case PYRO -> switch (attackElem) {
                case HYDRO -> 2.0F;
                case ELECTRO -> 1.0F;
                case CRYO, GEO, ANEMO -> 0.5F;
                case DENDRO -> 0.25F;
                default -> 0.0F;
            };
            case HYDRO -> switch (attackElem) {
                case DENDRO -> 2.0F;
                case CRYO, ELECTRO -> 1.0F;
                case PYRO, GEO, ANEMO -> 0.5F;
                default -> 0.0F;
            };
            case ELECTRO -> switch (attackElem) {
                case CRYO, PYRO, DENDRO -> 1.5F;
                case HYDRO, GEO, ANEMO -> 0.5F;
                default -> 0.0F;
            };
            case DENDRO -> switch (attackElem) {
                case PYRO -> 2.0F;
                case ELECTRO -> 1.0F;
                case HYDRO, CRYO, GEO, ANEMO -> 0.5F;
                default -> 0.0F;
            };
            case GEO -> switch (attackElem) {
                case GEO -> 1.5F;
                case PYRO, ELECTRO -> 0.75F;
                default -> 0.35F;
            };
            case ANEMO -> 1.0F;
        };
    }

    /**
     * Processes an incoming elemental hit against an entity's active Elemental Shield.
     */
    public static ShieldHitOutcome processIncomingElement(
            ServerLevel level,
            LivingEntity target,
            @Nullable LivingEntity attacker,
            Element attackElement,
            float attackU
    ) {
        ShieldState state = SHIELDS.get(target.getUUID());
        if (state == null || state.getCurrentUnits() <= 0.0001F) {
            return ShieldHitOutcome.NO_SHIELD;
        }

        Element shieldElem = state.getElement();
        float mult = getShieldConsumptionMultiplier(shieldElem, attackElement);
        if (mult <= 0.0F) {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.02D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.7F, 1.5F);
            return new ShieldHitOutcome(true, true, false, state.getCurrentUnits(), 0.0F);
        }

        float drain = Math.max(0.25F, attackU) * mult;
        boolean broken = state.consume(drain);

        if (broken) {
            SHIELDS.remove(target.getUUID());
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 4, 0.4D, 0.4D, 0.4D, 0.02D);
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 24, 0.6D, 0.6D, 0.6D, 0.08D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4F, 0.75F);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.3F, 0.9F);
            VeloriseElementEvents.fireShieldBroken(level, target, attacker, shieldElem, attackElement);
            return new ShieldHitOutcome(true, false, true, 0.0F, 1.0F);
        } else {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(), 10, 0.35D, 0.35D, 0.35D, 0.06D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 0.85F, 1.25F);
            float scale = state.isAbsorbHealthDamage() ? state.getHpDamagePassThrough() : 1.0F;
            return new ShieldHitOutcome(true, false, false, state.getCurrentUnits(), scale);
        }
    }
}
