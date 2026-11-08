package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager.
 * Tracks Gauge Units (U), Aura Decay, Innate Lock & Re-infusion, Environmental Auras, Multi-element Reactions, and combat feedback.
 */
public final class ElementalGaugeManager {
    private static final Map<UUID, Map<Element, GaugeState>> GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FROZEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> QUICKEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BURNING_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BLOOM_SEED_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> INNATE_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ELECTRO_CHARGE_TICK = new ConcurrentHashMap<>();

    private ElementalGaugeManager() {}

    public static void syncAuras(
            LivingEntity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        com.velorise.staves.network.ModNetwork.sendAuraSync(target, elements, reactedAura, trigger, reaction);
    }

    public static Collection<Element> getEntityElements(UUID entityId) {
        Map<Element, GaugeState> gauges = GAUGES.get(entityId);
        return gauges != null ? gauges.keySet() : Collections.emptyList();
    }

    public static void clearAll() {
        GAUGES.clear();
        FROZEN_UNTIL.clear();
        QUICKEN_UNTIL.clear();
        BURNING_UNTIL.clear();
        BLOOM_SEED_UNTIL.clear();
        INNATE_COOLDOWN_UNTIL.clear();
        ELECTRO_CHARGE_TICK.clear();
    }

    /**
     * Checks whether a vanilla mob has an innate elemental aura:
     * - #pyro_creatures: Blaze, MagmaCube, Strider -> PYRO
     * - #cryo_creatures: Stray, SnowGolem -> CRYO
     * - #hydro_creatures: Guardian, ElderGuardian, Drowned -> HYDRO
     * - #anemo_creatures: Breeze (1.21+) -> ANEMO
     */
    public static @Nullable Element getInnateElement(LivingEntity target) {
        if (target instanceof Blaze || target instanceof MagmaCube || target instanceof Strider) {
            return Element.PYRO;
        }
        if (target instanceof Stray || target instanceof SnowGolem) {
            return Element.CRYO;
        }
        if (target instanceof Guardian || target instanceof ElderGuardian || target instanceof Drowned) {
            return Element.HYDRO;
        }
        if (target instanceof Breeze) {
            return Element.ANEMO;
        }
        return null;
    }

    /**
     * Passive ocean/ambient critters (Fish, Squid, Dolphin, Bat) should not display overhead icons unless in active reaction.
     */
    public static boolean isPassiveAquaticOrAmbient(LivingEntity entity) {
        return entity instanceof WaterAnimal || entity instanceof AmbientCreature;
    }

    /**
     * Resolves an elemental attack hitting a target entity.
     * Calculates Innate Immunity, Innate Lock & Re-infusion, On-Hit Environmental Auras, Reactions, and Actionbar feedback.
     */
    public static void applyElement(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage
    ) {
        if (!target.isAlive() || target.isInvulnerable()) return;

        boolean egtEnabled = StavesConfig.ENABLE_ELEMENTAL_GAUGE.get();
        double dmgMult = StavesConfig.DAMAGE_MULTIPLIER.get();
        float effectiveDamage = (float) (baseDamage * dmgMult);

        if (!egtEnabled) {
            target.hurt(level.damageSources().magic(), effectiveDamage);
            return;
        }

        UUID targetId = target.getUUID();
        long now = level.getGameTime();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(targetId, k -> new EnumMap<>(Element.class));

        // 0A. Innate Elemental Creature Logic (Immunity to same element + Innate Aura Lock & Re-infusion)
        Element innateElement = getInnateElement(target);
        if (innateElement != null) {
            // Same-element immunity: e.g. Blaze hit by Pyro deals 0 DMG and pops "IMMUNE"
            if (attackElement == innateElement) {
                if (innateElement.isAura() && !targetGauges.containsKey(innateElement)) {
                    targetGauges.put(innateElement, new GaugeState(innateElement, 1.25F));
                }
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.4F);
                sendActionbar(attacker, ChatFormatting.GRAY + "🛡️ IMMUNE! (" + target.getName().getString() + " is immune to " + attackElement.formatted() + ChatFormatting.GRAY + ")");
                syncAuras(target, targetGauges.keySet(), innateElement, attackElement, ElementalReaction.IMMUNE);
                return;
            }

            // Ensure innate aura is present if not currently in 0.8s post-reaction recovery window
            Long cooldownUntil = INNATE_COOLDOWN_UNTIL.get(targetId);
            if (innateElement.isAura() && (cooldownUntil == null || now >= cooldownUntil)) {
                if (!targetGauges.containsKey(innateElement)) {
                    targetGauges.put(innateElement, new GaugeState(innateElement, 1.25F)); // 1.0U innate aura
                }
            }
        }

        // 0B. On-Hit Environmental Auras (Water/Rain -> 1.0U HYDRO, Lava/Fire -> 1.0U PYRO, Powder Snow -> 1.0U CRYO)
        if (target.isInWaterOrRain()) {
            if (targetGauges.remove(Element.PYRO) != null || BURNING_UNTIL.remove(targetId) != null) {
                target.clearFire();
            }
            if (innateElement != Element.PYRO && !targetGauges.containsKey(Element.HYDRO)) {
                targetGauges.put(Element.HYDRO, new GaugeState(Element.HYDRO, 1.25F)); // 1.0U Environmental Wet
            }
        } else if (target.isInLava() || (target.isOnFire() && !targetGauges.containsKey(Element.PYRO))) {
            if (innateElement == null && !targetGauges.containsKey(Element.PYRO)) {
                targetGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.25F));
            }
        } else if (target.isInPowderSnow) {
            if (innateElement == null && !targetGauges.containsKey(Element.CRYO)) {
                targetGauges.put(Element.CRYO, new GaugeState(Element.CRYO, 1.25F));
            }
        }

        // 0C. Dendro Core Secondary Reactions (Hyperbloom / Burgeon)
        Long bloomUntil = BLOOM_SEED_UNTIL.get(targetId);
        if (bloomUntil != null && now < bloomUntil) {
            if (attackElement == Element.ELECTRO) {
                BLOOM_SEED_UNTIL.remove(targetId);
                float bonus = (float) (9.0F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 0.9D, target.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.06D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.9D, target.getZ(), 18, 0.4D, 0.4D, 0.4D, 0.08D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.9F, 1.5F);
                sendActionbar(attacker, ChatFormatting.LIGHT_PURPLE + "🌱⚡ Hyperbloom! (+9.0 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GREEN + "Dendro Core homing burst");
                enforceInnateLock(targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.ELECTRO, ElementalReaction.HYPERBLOOM);
                return;
            } else if (attackElement == Element.PYRO) {
                BLOOM_SEED_UNTIL.remove(targetId);
                float bonus = (float) (9.5F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                AABB box = target.getBoundingBox().inflate(3.5D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker && e != target)) {
                    nearby.hurt(level.damageSources().magic(), bonus);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.6D, target.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 0.8D, target.getZ(), 25, 0.8D, 0.5D, 0.8D, 0.05D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.25F);
                sendActionbar(attacker, ChatFormatting.GOLD + "🌱🔥 Burgeon! (+9.5 AOE DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GREEN + "Dendro Core detonated");
                enforceInnateLock(targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.PYRO, ElementalReaction.BURGEON);
                return;
            }
        }

        // 1. Quicken Checks (Aggravate / Spread - does NOT consume Quicken aura U)
        Long quickenEnd = QUICKEN_UNTIL.get(targetId);
        if (quickenEnd != null && now < quickenEnd) {
            if (attackElement == Element.ELECTRO) {
                float bonus = (float) (5.0F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 18, 0.35D, 0.4D, 0.35D, 0.08D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
                sendActionbar(attacker, ChatFormatting.YELLOW + "⚡ Aggravate! (+5.0 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GREEN + "Quicken remaining " + String.format("%.1fs", (quickenEnd - now) / 20.0F));
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.ELECTRO, ElementalReaction.AGGRAVATE);
                return;
            } else if (attackElement == Element.DENDRO) {
                float bonus = (float) (6.5F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.35D, 0.4D, 0.35D, 0.05D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 0.9F, 1.4F);
                sendActionbar(attacker, ChatFormatting.DARK_GREEN + "🌿 Spread! (+6.5 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GREEN + "Quicken remaining " + String.format("%.1fs", (quickenEnd - now) / 20.0F));
                syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.DENDRO, ElementalReaction.SPREAD);
                return;
            }
        }

        // 2. Frozen Checks (Shatter / Forward Melt / Superconduct)
        Long frozenEnd = FROZEN_UNTIL.get(targetId);
        if (frozenEnd != null && now < frozenEnd) {
            if (attackElement == Element.GEO || attackElement == Element.ANEMO) {
                FROZEN_UNTIL.remove(targetId);
                target.setTicksFrozen(0);
                float bonus = (float) (10.0F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + 0.8D, target.getZ(), 30, 0.35D, 0.4D, 0.35D, 0.06D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                sendActionbar(attacker, ChatFormatting.WHITE + "🪨 Shatter! (+10.0 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GRAY + "Ice shattered");
                enforceInnateLock(targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, attackElement, ElementalReaction.SHATTER);
                return;
            } else if (attackElement == Element.PYRO) {
                FROZEN_UNTIL.remove(targetId);
                target.setTicksFrozen(0);
                GaugeState cryoGauge = targetGauges.get(Element.CRYO);
                if (cryoGauge != null) {
                    cryoGauge.consume(attackU * 2.0F);
                    if (cryoGauge.isExpired()) targetGauges.remove(Element.CRYO);
                }
                float meltDamage = effectiveDamage * 2.0F;
                target.hurt(level.damageSources().magic(), meltDamage);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 25, 0.3D, 0.35D, 0.3D, 0.04D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                sendActionbar(attacker, ChatFormatting.RED + "🔥 Melt! (x2.0 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GRAY + "Frozen melted");
                enforceInnateLock(targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT);
                return;
            } else if (attackElement == Element.ELECTRO) {
                FROZEN_UNTIL.remove(targetId);
                target.setTicksFrozen(0);
                GaugeState cryoGauge = targetGauges.get(Element.CRYO);
                if (cryoGauge != null) {
                    cryoGauge.consume(attackU * 1.0F);
                    if (cryoGauge.isExpired()) targetGauges.remove(Element.CRYO);
                }
                target.hurt(level.damageSources().magic(), effectiveDamage);
                executeReactionEffect(level, target, ElementalReaction.SUPERCONDUCT, Element.CRYO, attacker, dmgMult);
                sendActionbar(attacker, ChatFormatting.AQUA + "❄️⚡ Superconduct! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GRAY + "Frozen superconducted");
                enforceInnateLock(targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ELECTRO, ElementalReaction.SUPERCONDUCT);
                return;
            }
        }

        // 3. Check Overvape (Target has both Hydro & Electro, attacked with Pyro)
        if (attackElement == Element.PYRO && targetGauges.containsKey(Element.HYDRO) && targetGauges.containsKey(Element.ELECTRO)) {
            GaugeState hydro = targetGauges.get(Element.HYDRO);
            GaugeState electro = targetGauges.get(Element.ELECTRO);
            hydro.consume(attackU * 0.5F);
            electro.consume(attackU * 1.0F);
            if (hydro.isExpired()) targetGauges.remove(Element.HYDRO);
            if (electro.isExpired()) targetGauges.remove(Element.ELECTRO);

            float overvapeDmg = effectiveDamage * 1.5F;
            target.hurt(level.damageSources().magic(), overvapeDmg);
            triggerOverloadExplosion(level, target, attacker, dmgMult);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
            sendActionbar(attacker, ChatFormatting.GOLD + "💥 Overvape! (Vaporize x1.5 + Overload 3.5m AOE)");
            enforceInnateLock(targetGauges, targetId, innateElement, now);
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, attackElement, ElementalReaction.OVERVAPE);
            return;
        }

        // 4. Single Aura Reaction Resolution
        Element auraElement = findPrimaryAura(targetGauges);
        if (auraElement != null && auraElement != attackElement) {
            ElementalReaction reaction = determineReaction(auraElement, attackElement);
            if (reaction != ElementalReaction.NONE) {
                GaugeState aura = targetGauges.get(auraElement);

                // Electro-Charged Coexistence: both HYDRO and ELECTRO coexist on normal targets; on Innate mobs, foreign aura is locked out
                if (reaction == ElementalReaction.ELECTRO_CHARGED) {
                    if (innateElement == null) {
                        GaugeState triggerState = targetGauges.computeIfAbsent(attackElement, e -> new GaugeState(e, attackU));
                        aura.consume(0.4F);
                        triggerState.consume(0.4F);
                        if (aura.isExpired()) targetGauges.remove(auraElement);
                        if (triggerState.isExpired()) targetGauges.remove(attackElement);
                    } else {
                        aura.consume(0.4F);
                        if (aura.isExpired()) targetGauges.remove(auraElement);
                        enforceInnateLock(targetGauges, targetId, innateElement, now);
                    }

                    target.hurt(level.damageSources().magic(), effectiveDamage + (float) (2.5F * dmgMult));
                    executeReactionEffect(level, target, reaction, auraElement, attacker, dmgMult);
                    ELECTRO_CHARGE_TICK.put(targetId, 0);
                    sendActionbar(attacker, reaction.color() + reaction.reactionName() + " (" + reaction.formula() + ")! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.YELLOW + "Hydro + Electro shock");
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction);
                    return;
                }

                // Burning Coexistence: keep DENDRO and PYRO active while Burning ticks
                if (reaction == ElementalReaction.BURNING) {
                    if (innateElement == null) {
                        targetGauges.computeIfAbsent(Element.PYRO, e -> new GaugeState(e, attackU));
                        targetGauges.computeIfAbsent(Element.DENDRO, e -> new GaugeState(e, attackU));
                    } else {
                        enforceInnateLock(targetGauges, targetId, innateElement, now);
                    }
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    executeReactionEffect(level, target, reaction, auraElement, attacker, dmgMult);
                    sendActionbar(attacker, reaction.color() + reaction.reactionName() + " (" + reaction.formula() + ")! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.RED + "Continuous Burning active");
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction);
                    return;
                }

                float consumed = attackU * reaction.getConsumptionModifier();
                aura.consume(consumed);
                boolean auraDepleted = aura.isExpired();
                if (auraDepleted) {
                    targetGauges.remove(auraElement);
                }

                // If target is submerged in water and Reverse Vaporize occurred, ensure environmental Hydro remains
                if (reaction == ElementalReaction.REVERSE_VAPORIZE && target.isInWater()) {
                    target.clearFire();
                }

                enforceInnateLock(targetGauges, targetId, innateElement, now);

                float finalDmg = effectiveDamage * reaction.getDamageMultiplier();
                target.hurt(level.damageSources().magic(), finalDmg);
                executeReactionEffect(level, target, reaction, auraElement, attacker, dmgMult);

                if (auraDepleted) {
                    sendActionbar(attacker, reaction.color() + reaction.reactionName() + " (" + reaction.formula() + ")! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GRAY + "Depleted " + auraElement.symbol() + " " + auraElement.displayName() + " aura");
                } else {
                    sendActionbar(attacker, reaction.color() + reaction.reactionName() + " (" + reaction.formula() + ")! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.WHITE + "Remaining: " + auraElement.formatted() + String.format(" %.1fU", aura.getUnits()));
                }
                syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction);
                return;
            }
        }

        // 5. No Reaction: Apply or Refresh Aura (Aura elements only!)
        if (attackElement.isAura()) {
            // Strictly block PYRO aura application if the target is submerged in water
            if (attackElement == Element.PYRO && target.isInWater()) {
                target.clearFire();
                target.hurt(level.damageSources().magic(), effectiveDamage);
                return;
            }

            // Innate Lock: never allow foreign elemental auras (e.g. Hydro/Cryo on Blaze) to linger on innate elemental mobs
            if (innateElement != null && attackElement != innateElement) {
                target.hurt(level.damageSources().magic(), effectiveDamage);
                return;
            }

            if (targetGauges.containsKey(attackElement)) {
                GaugeState existing = targetGauges.get(attackElement);
                float refreshedU = Math.max(existing.getUnits(), attackU * 0.8F);
                existing.consume(existing.getUnits());
                existing.addUnits(refreshedU);
                sendActionbar(attacker, attackElement.color() + attackElement.symbol() + " Refreshed " + attackElement.displayName() + " aura" + String.format(" (%.1fU)", existing.getUnits()));
            } else {
                GaugeState newAura = new GaugeState(attackElement, attackU);
                targetGauges.put(attackElement, newAura);
                sendActionbar(attacker, attackElement.color() + attackElement.symbol() + " Applied " + attackElement.displayName() + " aura" + String.format(" (%.1fU)", newAura.getUnits()));
            }
            syncAuras(target, targetGauges.keySet(), null, null, null);
        }

        // Deal normal damage
        target.hurt(level.damageSources().magic(), effectiveDamage);
    }

    /**
     * Enforces Innate Lock & Re-infusion: removes any foreign elements from an innate creature
     * and schedules 0.8s (16 ticks) re-infusion if its innate aura was depleted by a reaction.
     */
    private static void enforceInnateLock(
            Map<Element, GaugeState> targetGauges,
            UUID targetId,
            @Nullable Element innateElement,
            long now
    ) {
        if (innateElement == null) return;
        targetGauges.keySet().removeIf(e -> e != innateElement);
        if (innateElement.isAura() && !targetGauges.containsKey(innateElement)) {
            INNATE_COOLDOWN_UNTIL.put(targetId, now + 16L); // Re-infuse innate aura after 0.8 seconds (16 ticks)
        }
    }

    private static Element findPrimaryAura(Map<Element, GaugeState> gauges) {
        Element best = null;
        float maxU = -1.0F;
        for (Map.Entry<Element, GaugeState> entry : gauges.entrySet()) {
            if (entry.getValue().getUnits() > maxU) {
                maxU = entry.getValue().getUnits();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static ElementalReaction determineReaction(Element aura, Element trigger) {
        if (aura == Element.PYRO) {
            if (trigger == Element.HYDRO) return ElementalReaction.FORWARD_VAPORIZE;
            if (trigger == Element.CRYO) return ElementalReaction.REVERSE_MELT;
            if (trigger == Element.ELECTRO) return ElementalReaction.OVERLOADED;
            if (trigger == Element.DENDRO) return ElementalReaction.BURNING;
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.HYDRO) {
            if (trigger == Element.PYRO) return ElementalReaction.REVERSE_VAPORIZE;
            if (trigger == Element.CRYO) return ElementalReaction.FROZEN;
            if (trigger == Element.ELECTRO) return ElementalReaction.ELECTRO_CHARGED;
            if (trigger == Element.DENDRO) return ElementalReaction.REVERSE_BLOOM; // Dendro on Hydro = 2.0x consumption
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.CRYO) {
            if (trigger == Element.PYRO) return ElementalReaction.FORWARD_MELT;
            if (trigger == Element.HYDRO) return ElementalReaction.FROZEN;
            if (trigger == Element.ELECTRO) return ElementalReaction.SUPERCONDUCT;
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.ELECTRO) {
            if (trigger == Element.PYRO) return ElementalReaction.OVERLOADED;
            if (trigger == Element.HYDRO) return ElementalReaction.ELECTRO_CHARGED;
            if (trigger == Element.CRYO) return ElementalReaction.SUPERCONDUCT;
            if (trigger == Element.DENDRO) return ElementalReaction.QUICKEN;
            if (trigger == Element.ANEMO) return ElementalReaction.SWIRL;
            if (trigger == Element.GEO) return ElementalReaction.CRYSTALLIZE;
        } else if (aura == Element.DENDRO) {
            if (trigger == Element.PYRO) return ElementalReaction.BURNING;
            if (trigger == Element.HYDRO) return ElementalReaction.BLOOM; // Hydro on Dendro = 0.5x consumption
            if (trigger == Element.ELECTRO) return ElementalReaction.QUICKEN;
        }
        return ElementalReaction.NONE;
    }

    private static void executeReactionEffect(
            ServerLevel level,
            LivingEntity target,
            ElementalReaction reaction,
            Element auraElement,
            @Nullable ServerPlayer attacker,
            double dmgMult
    ) {
        Vec3 pos = target.position();
        switch (reaction) {
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE -> {
                if (reaction == ElementalReaction.FORWARD_VAPORIZE) {
                    target.clearFire();
                }
                level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.8D, pos.z, 20, 0.3D, 0.3D, 0.3D, 0.05D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.3F);
            }
            case FORWARD_MELT, REVERSE_MELT -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 20, 0.35D, 0.35D, 0.35D, 0.04D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            case OVERLOADED -> {
                triggerOverloadExplosion(level, target, attacker, dmgMult);
            }
            case ELECTRO_CHARGED -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 16, 0.35D, 0.35D, 0.35D, 0.06D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.7F);
            }
            case SUPERCONDUCT -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 25, 1.2D, 0.5D, 1.2D, 0.03D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 15, 0.8D, 0.5D, 0.8D, 0.06D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.9F, 1.2F);
                AABB box = target.getBoundingBox().inflate(3.5D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
                    nearby.hurt(level.damageSources().freeze(), (float) (4.5F * dmgMult));
                    nearby.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                    nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0));
                }
            }
            case FROZEN -> {
                FROZEN_UNTIL.put(target.getUUID(), level.getGameTime() + 70); // 3.5 seconds
                target.setTicksFrozen(200);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 6));
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 25, 0.4D, 0.6D, 0.4D, 0.02D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.9F, 1.4F);
            }
            case QUICKEN -> {
                QUICKEN_UNTIL.put(target.getUUID(), level.getGameTime() + 140); // 7 seconds
                level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y + 0.8D, pos.z, 20, 0.35D, 0.5D, 0.35D, 0.04D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
            case BLOOM, REVERSE_BLOOM -> {
                BLOOM_SEED_UNTIL.put(target.getUUID(), level.getGameTime() + 120); // 6s Dendro Core window for Hyperbloom/Burgeon
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.8D, pos.z, 25, 0.8D, 0.5D, 0.8D, 0.04D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 1.1F);
                AABB box = target.getBoundingBox().inflate(3.0D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
                    nearby.hurt(level.damageSources().magic(), (float) (6.0F * dmgMult));
                }
            }
            case BURNING -> {
                BURNING_UNTIL.put(target.getUUID(), level.getGameTime() + 100); // 5 seconds
                target.igniteForSeconds(5);
                level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.8D, pos.z, 15, 0.3D, 0.4D, 0.3D, 0.03D);
            }
            case SWIRL -> {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.8D, pos.z, 4, 0.5D, 0.2D, 0.5D, 0.0D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.1F);
                AABB box = target.getBoundingBox().inflate(5.0D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != target && e != attacker)) {
                    nearby.hurt(level.damageSources().magic(), (float) (4.0F * dmgMult));
                    applyElement(level, nearby, auraElement, 1.0F, attacker, 0.0F);
                }
            }
            case CRYSTALLIZE -> {
                level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.8D, pos.z, 15, 0.35D, 0.4D, 0.35D, 0.05D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.2F);
                if (attacker != null) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, true, true));
                }
            }
            default -> {}
        }
    }

    private static void triggerOverloadExplosion(ServerLevel level, LivingEntity target, @Nullable ServerPlayer attacker, double dmgMult) {
        Vec3 pos = target.position();
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5D, pos.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 20, 0.6D, 0.6D, 0.6D, 0.06D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.1F);

        AABB box = target.getBoundingBox().inflate(3.5D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
            victim.hurt(level.damageSources().magic(), (float) (6.0F * dmgMult));
            Vec3 knock = victim.position().subtract(pos).normalize();
            victim.setDeltaMovement(victim.getDeltaMovement().add(knock.x * 0.75D, 0.42D, knock.z * 0.75D));
            victim.hurtMarked = true;
        }
    }

    private static void sendActionbar(@Nullable ServerPlayer player, String message) {
        if (player == null) return;
        if (!StavesConfig.SHOW_ACTIONBAR_FEEDBACK.get()) return;
        player.displayClientMessage(Component.literal(message), true);
    }

    /**
     * Server tick loop for decay, innate aura re-infusion after 0.8s, environmental water/rain extinguish, and periodic reactions.
     * Note: We NEVER iterate over ocean fish/squid or auto-attach HYDRO in tick().
     */
    public static void tick(MinecraftServer server) {
        // 0. Check Innate Re-infusion timers (0.8s = 16 ticks after an innate mob's aura was consumed)
        Iterator<Map.Entry<UUID, Long>> innateIt = INNATE_COOLDOWN_UNTIL.entrySet().iterator();
        while (innateIt.hasNext()) {
            Map.Entry<UUID, Long> cdEntry = innateIt.next();
            UUID id = cdEntry.getKey();
            LivingEntity target = null;
            ServerLevel targetLevel = null;
            for (ServerLevel lvl : server.getAllLevels()) {
                Entity e = lvl.getEntity(id);
                if (e instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                    target = living;
                    targetLevel = lvl;
                    break;
                }
            }
            if (target == null || targetLevel == null) {
                innateIt.remove();
                continue;
            }
            if (targetLevel.getGameTime() >= cdEntry.getValue()) {
                innateIt.remove();
                Element innate = getInnateElement(target);
                if (innate != null && innate.isAura()) {
                    Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(id, k -> new EnumMap<>(Element.class));
                    gauges.put(innate, new GaugeState(innate, 1.25F));
                    syncAuras(target, gauges.keySet(), null, null, null);
                }
            }
        }

        Iterator<Map.Entry<UUID, Map<Element, GaugeState>>> it = GAUGES.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Map<Element, GaugeState>> entry = it.next();
            UUID id = entry.getKey();
            LivingEntity target = null;
            ServerLevel targetLevel = null;

            for (ServerLevel lvl : server.getAllLevels()) {
                Entity e = lvl.getEntity(id);
                if (e instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                    target = living;
                    targetLevel = lvl;
                    break;
                }
            }

            if (target == null || targetLevel == null) {
                it.remove();
                FROZEN_UNTIL.remove(id);
                QUICKEN_UNTIL.remove(id);
                BURNING_UNTIL.remove(id);
                BLOOM_SEED_UNTIL.remove(id);
                INNATE_COOLDOWN_UNTIL.remove(id);
                ELECTRO_CHARGE_TICK.remove(id);
                continue;
            }

            long now = targetLevel.getGameTime();
            Map<Element, GaugeState> entityGauges = entry.getValue();
            Element innate = getInnateElement(target);
            boolean changed = false;

            // 1. Environmental Water / Rain check for non-innate entities holding PYRO aura
            if (innate != Element.PYRO && entityGauges.containsKey(Element.PYRO) && target.isInWaterOrRain()) {
                entityGauges.remove(Element.PYRO);
                BURNING_UNTIL.remove(id);
                target.clearFire();
                targetLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 12, 0.25D, 0.25D, 0.25D, 0.03D);
                targetLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.3F);
                syncAuras(target, entityGauges.keySet(), Element.PYRO, Element.HYDRO, ElementalReaction.FORWARD_VAPORIZE);
                if (entityGauges.isEmpty()) {
                    it.remove();
                    continue;
                }
            }

            // 2. Tick gauges decay (Innate aura on an innate mob does not expire naturally)
            Iterator<Map.Entry<Element, GaugeState>> gaugeIt = entityGauges.entrySet().iterator();
            while (gaugeIt.hasNext()) {
                Map.Entry<Element, GaugeState> gEntry = gaugeIt.next();
                if (innate != null && gEntry.getKey() == innate) {
                    continue; // Innate lock: never decay below 1.0U naturally
                }
                GaugeState state = gEntry.getValue();
                state.tick();
                if (state.isExpired()) {
                    gaugeIt.remove();
                    changed = true;
                }
            }

            // 3. Electro-Charged periodic damage (every 20 ticks = 1 second)
            if (entityGauges.containsKey(Element.HYDRO) && entityGauges.containsKey(Element.ELECTRO)) {
                int counter = ELECTRO_CHARGE_TICK.getOrDefault(id, 0) + 1;
                if (counter >= 20) {
                    counter = 0;
                    target.hurt(targetLevel.damageSources().magic(), 2.5F);
                    targetLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.05D);
                    targetLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);

                    GaugeState hydroState = entityGauges.get(Element.HYDRO);
                    GaugeState electroState = entityGauges.get(Element.ELECTRO);
                    if (hydroState != null && innate != Element.HYDRO) {
                        hydroState.consume(0.4F);
                        if (hydroState.isExpired()) {
                            entityGauges.remove(Element.HYDRO);
                            changed = true;
                        }
                    }
                    if (electroState != null && innate != Element.ELECTRO) {
                        electroState.consume(0.4F);
                        if (electroState.isExpired()) {
                            entityGauges.remove(Element.ELECTRO);
                            changed = true;
                        }
                    }
                }
                ELECTRO_CHARGE_TICK.put(id, counter);
            } else {
                ELECTRO_CHARGE_TICK.remove(id);
            }

            // 4. Burning periodic damage (every 10 ticks = 0.5s)
            Long burningEnd = BURNING_UNTIL.get(id);
            if (burningEnd != null) {
                if (now >= burningEnd || !entityGauges.containsKey(Element.DENDRO) || target.isInWaterOrRain()) {
                    BURNING_UNTIL.remove(id);
                } else if (now % 10 == 0) {
                    target.hurt(targetLevel.damageSources().inFire(), 1.5F);
                    GaugeState dendroState = entityGauges.get(Element.DENDRO);
                    if (dendroState != null) {
                        dendroState.consume(0.2F);
                        if (dendroState.isExpired()) {
                            entityGauges.remove(Element.DENDRO);
                            BURNING_UNTIL.remove(id);
                            changed = true;
                        }
                    }
                    targetLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 6, 0.25D, 0.3D, 0.25D, 0.02D);
                }
            }

            // 5. Frozen check
            Long frozenEnd = FROZEN_UNTIL.get(id);
            if (frozenEnd != null) {
                if (now >= frozenEnd) {
                    FROZEN_UNTIL.remove(id);
                } else {
                    target.setDeltaMovement(0.0D, target.getDeltaMovement().y < 0 ? -0.05D : 0.0D, 0.0D);
                }
            }

            if (entityGauges.isEmpty()) {
                it.remove();
            }

            if (changed) {
                syncAuras(target, entityGauges.keySet(), null, null, null);
            }

            // 6. Ambient aura particles (every 10 ticks)
            if (!entityGauges.isEmpty() && now % 10 == 0 && StavesConfig.SHOW_ELEMENTAL_PARTICLES.get()) {
                spawnAmbientParticles(targetLevel, target, entityGauges, now);
            }
        }
    }

    private static void spawnAmbientParticles(ServerLevel level, LivingEntity target, Map<Element, GaugeState> gauges, long now) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.65D;
        double z = target.getZ();

        if (gauges.containsKey(Element.PYRO)) {
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.2D, 0.25D, 0.2D, 0.01D);
        }
        if (gauges.containsKey(Element.HYDRO)) {
            level.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 3, 0.2D, 0.25D, 0.2D, 0.0D);
        }
        if (gauges.containsKey(Element.CRYO)) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 2, 0.25D, 0.25D, 0.25D, 0.01D);
        }
        if (gauges.containsKey(Element.ELECTRO)) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 2, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        if (gauges.containsKey(Element.DENDRO)) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 2, 0.2D, 0.25D, 0.2D, 0.01D);
        }
    }
}
