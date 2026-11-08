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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager for Fabric 1.21.1.
 * Tracks Gauge Units (U), Aura Decay, Multi-element Reactions, and combat feedback.
 */
public final class ElementalGaugeManager {
    private static final Map<UUID, Map<Element, GaugeState>> GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FROZEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> QUICKEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BURNING_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ELECTRO_CHARGE_TICK = new ConcurrentHashMap<>();

    private ElementalGaugeManager() {}

    public static void syncAuras(LivingEntity target, Collection<Element> elements, @Nullable Element trigger, @Nullable ElementalReaction reaction) {
        com.velorise.staves.network.ModNetwork.sendAuraSync(target, elements, trigger, reaction);
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
        ELECTRO_CHARGE_TICK.clear();
    }

    /**
     * Resolves an elemental attack hitting a target entity.
     * Calculates reactions, consumption, bonus damage, status effects, and Actionbar feedback.
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

        // 1. Quicken Checks (Aggravate / Spread)
        Long quickenEnd = QUICKEN_UNTIL.get(targetId);
        if (quickenEnd != null && now < quickenEnd) {
            if (attackElement == Element.ELECTRO) {
                float bonus = (float) (5.0F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 18, 0.35D, 0.4D, 0.35D, 0.08D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
                sendActionbar(attacker, ChatFormatting.YELLOW + "⚡ Aggravate! (+5.0 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GREEN + "Quicken remaining " + String.format("%.1fs", (quickenEnd - now) / 20.0F));
                return;
            } else if (attackElement == Element.DENDRO) {
                float bonus = (float) (6.5F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.35D, 0.4D, 0.35D, 0.05D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 0.9F, 1.4F);
                sendActionbar(attacker, ChatFormatting.DARK_GREEN + "🌿 Spread! (+6.5 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GREEN + "Quicken remaining " + String.format("%.1fs", (quickenEnd - now) / 20.0F));
                return;
            }
        }

        // 2. Frozen Checks (Shatter / Melt)
        Long frozenEnd = FROZEN_UNTIL.get(targetId);
        if (frozenEnd != null && now < frozenEnd) {
            if (attackElement == Element.GEO) {
                FROZEN_UNTIL.remove(targetId);
                float bonus = (float) (10.0F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + 0.8D, target.getZ(), 30, 0.35D, 0.4D, 0.35D, 0.06D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                sendActionbar(attacker, ChatFormatting.WHITE + "🪨 Shatter! (+10.0 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GRAY + "Ice shattered");
                return;
            } else if (attackElement == Element.PYRO) {
                FROZEN_UNTIL.remove(targetId);
                float meltDamage = effectiveDamage * 2.0F;
                target.hurt(level.damageSources().magic(), meltDamage);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 25, 0.3D, 0.35D, 0.3D, 0.04D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                sendActionbar(attacker, ChatFormatting.RED + "🔥 Melt! (x2.0 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GRAY + "Frozen melted");
                return;
            }
        }

        // 3. Aura Reaction Checks
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(targetId, k -> new EnumMap<>(Element.class));

        // Check Overvape (Target has both Hydro & Electro, attacked with Pyro)
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
            syncAuras(target, targetGauges.keySet(), attackElement, ElementalReaction.OVERLOADED);
            return;
        }

        // Single Aura Reaction Resolution
        Element auraElement = findPrimaryAura(targetGauges);
        if (auraElement != null && auraElement != attackElement) {
            ElementalReaction reaction = determineReaction(auraElement, attackElement);
            if (reaction != ElementalReaction.NONE) {
                GaugeState aura = targetGauges.get(auraElement);
                float consumed = attackU * reaction.getConsumptionModifier();
                aura.consume(consumed);
                boolean auraDepleted = aura.isExpired();
                if (auraDepleted) {
                    targetGauges.remove(auraElement);
                }

                float finalDmg = effectiveDamage * reaction.getDamageMultiplier();
                target.hurt(level.damageSources().magic(), finalDmg);
                executeReactionEffect(level, target, reaction, auraElement, attacker, dmgMult);

                if (auraDepleted) {
                    sendActionbar(attacker, reaction.color() + reaction.reactionName() + " (" + reaction.formula() + ")! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GRAY + "Depleted " + auraElement.symbol() + " " + auraElement.displayName() + " aura");
                } else {
                    sendActionbar(attacker, reaction.color() + reaction.reactionName() + " (" + reaction.formula() + ")! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.WHITE + "Remaining: " + auraElement.formatted() + String.format(" %.1fU", aura.getUnits()));
                }
                syncAuras(target, targetGauges.keySet(), attackElement, reaction);
                return;
            }
        }

        // 4. No Reaction: Apply or Refresh Aura (Aura elements only!)
        if (attackElement.isAura()) {
            if (targetGauges.containsKey(attackElement)) {
                GaugeState existing = targetGauges.get(attackElement);
                existing.addUnits(attackU * 0.8F);
                sendActionbar(attacker, attackElement.color() + attackElement.symbol() + " Refreshed " + attackElement.displayName() + " aura" + String.format(" (Total: %.1fU)", existing.getUnits()));
            } else {
                GaugeState newAura = new GaugeState(attackElement, attackU);
                targetGauges.put(attackElement, newAura);
                sendActionbar(attacker, attackElement.color() + attackElement.symbol() + " Applied " + attackElement.displayName() + " aura" + String.format(" (%.1fU)", newAura.getUnits()));
            }
            syncAuras(target, targetGauges.keySet(), null, null);
        }

        // Deal normal damage
        target.hurt(level.damageSources().magic(), effectiveDamage);
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
            if (trigger == Element.DENDRO) return ElementalReaction.BLOOM;
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
            if (trigger == Element.HYDRO) return ElementalReaction.BLOOM;
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
            case BLOOM -> {
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
     * Server tick loop for decay, continuous reaction damage, and ambient particles.
     */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();
            Iterator<Map.Entry<UUID, Map<Element, GaugeState>>> it = GAUGES.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<UUID, Map<Element, GaugeState>> entry = it.next();
                UUID id = entry.getKey();
                Entity entity = level.getEntity(id);

                if (!(entity instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) {
                    it.remove();
                    FROZEN_UNTIL.remove(id);
                    QUICKEN_UNTIL.remove(id);
                    BURNING_UNTIL.remove(id);
                    ELECTRO_CHARGE_TICK.remove(id);
                    continue;
                }

                Map<Element, GaugeState> entityGauges = entry.getValue();
                boolean changed = false;

                // 1. Tick gauges decay
                Iterator<Map.Entry<Element, GaugeState>> gaugeIt = entityGauges.entrySet().iterator();
                while (gaugeIt.hasNext()) {
                    GaugeState state = gaugeIt.next().getValue();
                    state.tick();
                    if (state.isExpired()) {
                        gaugeIt.remove();
                        changed = true;
                    }
                }

                if (entityGauges.isEmpty()) {
                    it.remove();
                }

                if (changed) {
                    syncAuras(target, entityGauges.keySet(), null, null);
                }

                // 2. Electro-Charged periodic damage (every 20 ticks = 1 second)
                if (entityGauges.containsKey(Element.HYDRO) && entityGauges.containsKey(Element.ELECTRO)) {
                    int counter = ELECTRO_CHARGE_TICK.getOrDefault(id, 0) + 1;
                    if (counter >= 20) {
                        counter = 0;
                        target.hurt(level.damageSources().magic(), 2.5F);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.05D);
                        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);

                        entityGauges.get(Element.HYDRO).consume(0.4F);
                        entityGauges.get(Element.ELECTRO).consume(0.4F);
                    }
                    ELECTRO_CHARGE_TICK.put(id, counter);
                } else {
                    ELECTRO_CHARGE_TICK.remove(id);
                }

                // 3. Burning periodic damage (every 10 ticks = 0.5s)
                Long burningEnd = BURNING_UNTIL.get(id);
                if (burningEnd != null) {
                    if (now >= burningEnd || !entityGauges.containsKey(Element.DENDRO)) {
                        BURNING_UNTIL.remove(id);
                    } else if (now % 10 == 0) {
                        target.hurt(level.damageSources().inFire(), 1.5F);
                        entityGauges.get(Element.DENDRO).consume(0.2F);
                        level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 6, 0.25D, 0.3D, 0.25D, 0.02D);
                    }
                }

                // 4. Frozen check
                Long frozenEnd = FROZEN_UNTIL.get(id);
                if (frozenEnd != null) {
                    if (now >= frozenEnd) {
                        FROZEN_UNTIL.remove(id);
                    } else {
                        target.setDeltaMovement(0.0D, target.getDeltaMovement().y < 0 ? -0.05D : 0.0D, 0.0D);
                    }
                }

                // 5. Ambient aura particles (every 10 ticks)
                if (now % 10 == 0 && StavesConfig.SHOW_ELEMENTAL_PARTICLES.get()) {
                    spawnAmbientParticles(level, target, entityGauges, now);
                }
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
