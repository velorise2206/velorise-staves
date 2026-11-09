package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.block.Blocks;
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
    private static final Map<Long, Long> LAST_PROXIMITY_SHOCK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BLOOM_SPAWN = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> IS_PROCESSING_DAMAGE = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> IS_PROCESSING_REACTION = ThreadLocal.withInitial(() -> false);

    private static class IcdEntry {
        int hitCount;
        long firstHitTime;

        IcdEntry(int hitCount, long firstHitTime) {
            this.hitCount = hitCount;
            this.firstHitTime = firstHitTime;
        }
    }

    private static final Map<Long, IcdEntry> ICD_TRACKER = new ConcurrentHashMap<>();

    private ElementalGaugeManager() {}

    public static void syncAuras(
            LivingEntity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        syncAuras(target, elements, reactedAura, trigger, reaction, 0.0F);
    }

    public static void syncAuras(
            LivingEntity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage
    ) {
        com.velorise.staves.network.ModNetwork.sendAuraSync(target, elements, reactedAura, trigger, reaction, damage);
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
        LAST_PROXIMITY_SHOCK.clear();
        LAST_BLOOM_SPAWN.clear();
        ICD_TRACKER.clear();
        DendroCoreManager.clearAll();
        CrystallizeShardManager.clearAll();
        ElectrifiedWaterManager.clearAll();
        DragonfireCloudManager.clearAll();
    }

    public static boolean isEntityInWater(LivingEntity target) {
        if (target.isInWaterRainOrBubble() || target.isInWaterOrRain() || target.isInWater()) return true;
        BlockPos feet = target.blockPosition();
        if (target.level().getBlockState(feet).is(Blocks.WATER) || target.level().getFluidState(feet).is(FluidTags.WATER)) {
            return true;
        }
        if (target.level().getFluidState(feet.below()).is(FluidTags.WATER)) {
            return true;
        }
        BlockPos eye = BlockPos.containing(target.getX(), target.getEyeY(), target.getZ());
        return target.level().getBlockState(eye).is(Blocks.WATER) || target.level().getFluidState(eye).is(FluidTags.WATER);
    }

    /**
     * Handles environmental damage types and innate elemental mob attacks:
     * - Fire damage -> Fire reaction / 1.0U Pyro aura
     * - Freezing / Powder Snow -> Cryo reaction / 1.0U Cryo aura
     * - Lightning -> Electro reaction / 2.0U Electro aura
     * - Dragon's Breath -> Electro reaction / 1.5U Electro aura
     * - Melee/Ranged attack from mob with innate element (Enderman, Ender Dragon, Shulker, Blaze, Stray, Bogged) -> applies 1.0U innate aura
     */
    public static void handleLivingIncomingDamage(LivingEntity target, DamageSource source) {
        if (!target.isAlive() || target.isInvulnerable() || target.level().isClientSide()) return;
        if (IS_PROCESSING_DAMAGE.get() || IS_PROCESSING_REACTION.get()) return;

        IS_PROCESSING_DAMAGE.set(true);
        try {
            if (target.level() instanceof ServerLevel level) {
                // 1. Environmental Fire
                if (source.is(DamageTypeTags.IS_FIRE)) {
                    handleEnvironmentalFireDamage(target);
                    return;
                }

                // 2. Dragon's Breath (DamageTypes.DRAGON_BREATH) -> Electro
                if (source.is(DamageTypes.DRAGON_BREATH)) {
                    applyElement(level, target, Element.ELECTRO, 1.5F, null, 0.0F);
                    return;
                }

                // 3. Lightning Bolt (DamageTypeTags.IS_LIGHTNING) -> Electro
                if (source.is(DamageTypeTags.IS_LIGHTNING)) {
                    applyElement(level, target, Element.ELECTRO, 2.0F, null, 0.0F);
                    return;
                }

                // 4. Freezing / Powder snow (DamageTypeTags.IS_FREEZING) -> Cryo
                if (source.is(DamageTypeTags.IS_FREEZING)) {
                    applyElement(level, target, Element.CRYO, 1.0F, null, 0.0F);
                    return;
                }

                // 5. Environmental Plant / Thorn damage (Cactus, Sweet Berry Bush) -> Dendro (1.0U)
                if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)) {
                    applyElement(level, target, Element.DENDRO, 1.0F, null, 0.0F);
                    return;
                }

                // 6. Cave Spider venom bite -> Dendro (1.0U)
                if (source.getEntity() instanceof CaveSpider) {
                    applyElement(level, target, Element.DENDRO, 1.0F, null, 0.0F);
                    return;
                }

                // 7. Wind Charge damage (Vanilla Wind Charge / Breeze) -> Anemo (1.0U)
                if (source.type().msgId().contains("wind_charge") || source.type().msgId().contains("breeze")) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.ANEMO, 1.0F, attacker, 1.0F);
                    return;
                }

                // 8. Falling Stalactite / Stalagmite / Anvil / Falling Block -> Geo (1.5U)
                if (source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.STALAGMITE)
                        || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_BLOCK)) {
                    applyElement(level, target, Element.GEO, 1.5F, null, 0.0F);
                    return;
                }

                // 9. Thrown Trident -> Hydro (1.0U)
                if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.HYDRO, 1.0F, attacker, 0.0F);
                    return;
                }

                // 7. Direct Ender projectiles (Dragon Fireball, Shulker Bullet, Ender Pearl) -> Electro (1.5U)
                if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.DragonFireball
                        || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ShulkerBullet
                        || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl) {
                    applyElement(level, target, Element.ELECTRO, 1.5F, null, 0.0F);
                    return;
                }

                // 10. Husk melee attack -> Geo (0.5U)
                if (source.getEntity() instanceof Husk) {
                    applyElement(level, target, Element.GEO, 0.5F, null, 0.0F);
                    return;
                }

                // 11. Ravager: Roar shockwave -> Anemo (1.5U), Stomp/Melee -> Geo (1.0U)
                if (source.getEntity() instanceof Ravager ravager) {
                    if (ravager.getRoarTick() > 0) {
                        applyElement(level, target, Element.ANEMO, 1.5F, null, 0.0F);
                    } else {
                        applyElement(level, target, Element.GEO, 1.0F, null, 0.0F);
                    }
                    return;
                }

                // 12. Evoker Fangs -> Geo (1.0U)
                if (source.getDirectEntity() instanceof EvokerFangs) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.GEO, 1.0F, attacker, 0.0F);
                    return;
                }

                // 13. Vex melee attack -> Electro (0.5U)
                if (source.getEntity() instanceof Vex) {
                    applyElement(level, target, Element.ELECTRO, 0.5F, null, 0.0F);
                    return;
                }

                // 14. Fireworks explosion -> Pyro (1.5U)
                if (source.is(DamageTypes.FIREWORKS)
                        || source.getDirectEntity() instanceof FireworkRocketEntity) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.PYRO, 1.5F, attacker, 0.0F);
                    return;
                }

                // 8. Attacker with Innate Element (Enderman, Ender Dragon, Shulker, Endermite, Blaze, Stray)
                if (source.getEntity() instanceof LivingEntity attackerMob && attackerMob != target) {
                    Element innate = getInnateElement(attackerMob);
                    if (innate != null) {
                        ServerPlayer attackerPlayer = attackerMob instanceof ServerPlayer sp ? sp : null;
                        applyElement(level, target, innate, 1.0F, attackerPlayer, 0.0F);
                    }
                }
            }
        } finally {
            IS_PROCESSING_DAMAGE.set(false);
        }
    }

    /**
     * Handles environmental fire damage (lava, fire block, campfire, magma, burning) on entities.
     * Triggers reactions if entity holds Hydro/Cryo/Electro/Dendro, or applies 1.0U Pyro aura.
     */
    public static void handleEnvironmentalFireDamage(LivingEntity target) {
        if (!target.isAlive() || target.isInvulnerable()) return;
        Element innate = getInnateElement(target);
        if (innate == Element.PYRO || target.fireImmune() || target instanceof Warden) {
            long now = target.level().getGameTime();
            Long lastImmune = INNATE_COOLDOWN_UNTIL.get(target.getUUID());
            if (lastImmune == null || now >= lastImmune) {
                INNATE_COOLDOWN_UNTIL.put(target.getUUID(), now + 30L);
                syncAuras(target, Collections.emptyList(), null, Element.PYRO, ElementalReaction.IMMUNE);
            }
            return;
        }
        if (isEntityInWater(target)) return;

        UUID id = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new EnumMap<>(Element.class));
        long now = target.level().getGameTime();

        // Check if there is an existing aura that reacts with Pyro
        if (targetGauges.containsKey(Element.HYDRO)) {
            targetGauges.remove(Element.HYDRO);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.3F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
            }
            syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE, 2.5F);
            return;
        }

        if (targetGauges.containsKey(Element.CRYO)) {
            targetGauges.remove(Element.CRYO);
            FROZEN_UNTIL.remove(id);
            target.setTicksFrozen(0);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.8D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.04D);
            }
            syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, 3.0F);
            return;
        }

        if (targetGauges.containsKey(Element.ELECTRO)) {
            targetGauges.remove(Element.ELECTRO);
            if (target.level() instanceof ServerLevel sl) {
                triggerOverloadExplosion(sl, target, null, 1.0D);
            }
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.PYRO, ElementalReaction.OVERLOADED, 6.0F);
            return;
        }

        if (targetGauges.containsKey(Element.DENDRO)) {
            if (!BURNING_UNTIL.containsKey(id)) {
                BURNING_UNTIL.put(id, now + 100);
                target.setSecondsOnFire(5);
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.PYRO, ElementalReaction.BURNING, 0.0F);
            }
            return;
        }

        // No reactive aura -> apply Pyro 1.0U
        if (!targetGauges.containsKey(Element.PYRO)) {
            targetGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.25F));
            syncAuras(target, targetGauges.keySet(), null, null, null, 0.0F);
        }
    }

    /**
     * Handles environmental water on entities holding active auras:
     * - Pyro in water -> Extinguishes & triggers Vaporize
     * - Dendro in water / Innate Dendro (Bogged, Dendro Slime) in rain -> Triggers Bloom reaction & spawns Dendro Core (0.5s ICD)
     */
    public static void handleEnvironmentalWater(LivingEntity target) {
        if (!target.isAlive()) return;
        UUID id = target.getUUID();
        Element innate = getInnateElement(target);

        // 1. Innate Dendro mob (Bogged, Dendro Slime) in water or rain -> Bloom reaction!
        if (innate == Element.DENDRO) {
            long now = target.level().getGameTime();
            Long lastSpawn = LAST_BLOOM_SPAWN.get(id);
            if (lastSpawn == null || now - lastSpawn >= 10L) { // 0.5s ICD
                if (target.level() instanceof ServerLevel sl) {
                    DendroCoreManager.spawn(sl, target.position(), null, 1.0F);
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 0.8D, target.getZ(), 20, 0.6D, 0.4D, 0.6D, 0.04D);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                LAST_BLOOM_SPAWN.put(id, now);
                syncAuras(target, Collections.singleton(Element.DENDRO), Element.DENDRO, Element.HYDRO, ElementalReaction.BLOOM, 0.0F);
            }
            return;
        }

        Map<Element, GaugeState> targetGauges = GAUGES.get(id);
        if (targetGauges == null || targetGauges.isEmpty()) return;

        // 2. Pyro in water -> Extinguish & Vaporize
        if (innate != Element.PYRO && targetGauges.containsKey(Element.PYRO)) {
            targetGauges.remove(Element.PYRO);
            BURNING_UNTIL.remove(id);
            target.clearFire();
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.03D);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.3F);
            }
            target.hurt(target.level().damageSources().magic(), 2.5F);
            syncAuras(target, targetGauges.keySet(), Element.PYRO, Element.HYDRO, ElementalReaction.FORWARD_VAPORIZE, 2.5F);
        }

        // 3. Applied Dendro in water -> Bloom reaction & spawn Dendro Core!
        if (targetGauges.containsKey(Element.DENDRO)) {
            targetGauges.remove(Element.DENDRO);
            long now = target.level().getGameTime();
            Long lastSpawn = LAST_BLOOM_SPAWN.get(id);
            if (lastSpawn == null || now - lastSpawn >= 10L) {
                if (target.level() instanceof ServerLevel sl) {
                    DendroCoreManager.spawn(sl, target.position(), null, 1.0F);
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 0.8D, target.getZ(), 20, 0.6D, 0.4D, 0.6D, 0.04D);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                LAST_BLOOM_SPAWN.put(id, now);
            }
            syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.HYDRO, ElementalReaction.BLOOM, 0.0F);
        }

        // 4. Electro in water -> Trigger Electro-Charged shock on entity itself (water does NOT turn electrified)
        if (targetGauges.containsKey(Element.ELECTRO)) {
            GaugeState electro = targetGauges.get(Element.ELECTRO);
            if (electro != null) {
                electro.consume(0.4F);
                if (electro.isExpired()) targetGauges.remove(Element.ELECTRO);
            }
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.8D, target.getZ(), 16, 0.3D, 0.3D, 0.3D, 0.06D);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.8F);
                target.hurt(sl.damageSources().magic(), 2.5F);
            }
            syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, 2.5F);
        }
    }


    /**
     * Checks whether a vanilla mob has an innate elemental aura:
     * - #pyro_creatures: Blaze, MagmaCube, Strider -> PYRO
     * - #cryo_creatures: Stray, SnowGolem -> CRYO
     * - #hydro_creatures: Guardian, ElderGuardian, Drowned -> HYDRO
     * - #electro_creatures: Charged Creeper -> ELECTRO
     */
    public static @Nullable Element getInnateElement(LivingEntity target) {
        if (target instanceof Blaze
                || target instanceof MagmaCube
                || target instanceof Strider
                || target instanceof Ghast
                || target instanceof WitherSkeleton
                || target instanceof ZombifiedPiglin) {
            return Element.PYRO;
        }
        if (target instanceof Stray || target instanceof SnowGolem) {
            return Element.CRYO;
        }
        if (target instanceof Guardian || target instanceof ElderGuardian || target instanceof Drowned) {
            return Element.HYDRO;
        }
        if ((target instanceof Creeper creeper && creeper.isPowered())
                || target instanceof EnderMan
                || target instanceof EnderDragon
                || target instanceof Endermite
                || target instanceof Shulker) {
            return Element.ELECTRO;
        }
        if (target instanceof MushroomCow) {
            return Element.DENDRO;
        }
        if (target instanceof IronGolem) {
            return Element.GEO;
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
     * Standard ICD (3 hits / 2.5s timer):
     * Follows Genshin Impact's standard internal cooldown rule for elemental attacks.
     * Hit 1, 4, 7... or attacks separated by >= 2.5s (50 ticks) apply element / trigger reaction.
     * Rapid hits in between deal base damage without applying an aura or triggering reactions.
     */
    public static boolean checkStandardIcd(LivingEntity target, Element element, @Nullable ServerPlayer attacker, long now) {
        int attackerKey = attacker != null ? attacker.getId() : 0;
        long key = (((long) attackerKey) << 36) | (((long) target.getId()) << 4) | (element.ordinal() & 0xFL);

        IcdEntry entry = ICD_TRACKER.get(key);
        if (entry == null || (now - entry.firstHitTime) >= 50) {
            ICD_TRACKER.put(key, new IcdEntry(1, now));
            return true;
        }

        entry.hitCount++;
        return (entry.hitCount % 3 == 1);
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

        long now = level.getGameTime();

        // Standard ICD check (3-Hit / 2.5s rule) for damaging elemental attacks:
        // If on ICD, deal effective base damage directly without applying aura or triggering reactions.
        if (baseDamage > 0.0F) {
            boolean canApply = checkStandardIcd(target, attackElement, attacker, now);
            if (!canApply) {
                target.hurt(level.damageSources().magic(), effectiveDamage);
                return;
            }
        }

        IS_PROCESSING_REACTION.set(true);
        try {
            applyElementInternal(level, target, attackElement, attackU, attacker, baseDamage, effectiveDamage, dmgMult, now);
        } finally {
            IS_PROCESSING_REACTION.set(false);
        }
    }

    private static void applyElementInternal(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            float effectiveDamage,
            double dmgMult,
            long now
    ) {
        UUID targetId = target.getUUID();
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

        // Warden & Vanilla Fire-Immune Mob Check for Pyro attacks (deal 0 DMG and pop IMMUNE)
        if (attackElement == Element.PYRO && (target instanceof Warden || target.fireImmune())) {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.4F);
            syncAuras(target, targetGauges.keySet(), null, attackElement, ElementalReaction.IMMUNE);
            return;
        }

        // 0B. On-Hit Environmental Auras (Water/Rain -> 1.0U HYDRO, Lava/Fire -> 1.0U PYRO, Powder Snow -> 1.0U CRYO)
        if (isEntityInWater(target)) {
            if (targetGauges.remove(Element.PYRO) != null || BURNING_UNTIL.remove(targetId) != null) {
                target.clearFire();
            }
            if (innateElement != Element.PYRO && !targetGauges.containsKey(Element.HYDRO)) {
                targetGauges.put(Element.HYDRO, new GaugeState(Element.HYDRO, 1.25F)); // 1.0U Environmental Wet
            }
        } else if (target.isInLava() || (target.isOnFire() && !targetGauges.containsKey(Element.PYRO))) {
            if (innateElement == null && !target.fireImmune() && !(target instanceof Warden) && !targetGauges.containsKey(Element.PYRO)) {
                targetGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.25F));
            }
        } else if (target.isInPowderSnow) {
            if (innateElement == null && !targetGauges.containsKey(Element.CRYO)) {
                targetGauges.put(Element.CRYO, new GaugeState(Element.CRYO, 1.25F));
            }
        }

        // Check if attack element detonates nearby Dendro Cores (Hyperbloom / Burgeon)
        DendroCoreManager.checkReactionsNear(level, target.position(), 3.5D, attackElement, attacker);

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
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.ELECTRO, ElementalReaction.HYPERBLOOM, effectiveDamage + bonus);
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
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.PYRO, ElementalReaction.BURGEON, effectiveDamage + bonus);
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
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.ELECTRO, ElementalReaction.AGGRAVATE, effectiveDamage + bonus);
                return;
            } else if (attackElement == Element.DENDRO) {
                float bonus = (float) (6.5F * dmgMult);
                target.hurt(level.damageSources().magic(), effectiveDamage + bonus);
                level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.35D, 0.4D, 0.35D, 0.05D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 0.9F, 1.4F);
                sendActionbar(attacker, ChatFormatting.DARK_GREEN + "🌿 Spread! (+6.5 DMG) " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.GREEN + "Quicken remaining " + String.format("%.1fs", (quickenEnd - now) / 20.0F));
                syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.DENDRO, ElementalReaction.SPREAD, effectiveDamage + bonus);
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
                syncAuras(target, targetGauges.keySet(), Element.CRYO, attackElement, ElementalReaction.SHATTER, effectiveDamage + bonus);
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
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, meltDamage);
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
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ELECTRO, ElementalReaction.SUPERCONDUCT, effectiveDamage);
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
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, attackElement, ElementalReaction.OVERVAPE, overvapeDmg);
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

                    float ecDmg = effectiveDamage + (float) (2.5F * dmgMult);
                    target.hurt(level.damageSources().magic(), ecDmg);
                    executeReactionEffect(level, target, reaction, auraElement, attacker, dmgMult);
                    ELECTRO_CHARGE_TICK.put(targetId, 0);
                    sendActionbar(attacker, reaction.color() + reaction.reactionName() + " (" + reaction.formula() + ")! " + ChatFormatting.DARK_GRAY + "| " + ChatFormatting.YELLOW + "Hydro + Electro shock");
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, ecDmg);
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
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, effectiveDamage);
                    return;
                }

                float consumed = attackU * reaction.getConsumptionModifier();
                aura.consume(consumed);
                boolean auraDepleted = aura.isExpired();
                if (auraDepleted) {
                    targetGauges.remove(auraElement);
                }

                // If target is in water and Reverse Vaporize occurred, ensure fire is extinguished
                if (reaction == ElementalReaction.REVERSE_VAPORIZE && isEntityInWater(target)) {
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
                syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, finalDmg);
                return;
            }
        }

        // 5. No Reaction: Apply or Refresh Aura (Aura elements only!)
        if (attackElement.isAura()) {
            // Strictly block PYRO aura application if the target is in water or rain
            if (attackElement == Element.PYRO && isEntityInWater(target)) {
                target.clearFire();
                float vapeDmg = effectiveDamage * 1.5F;
                target.hurt(level.damageSources().magic(), vapeDmg);
                syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE, vapeDmg);
                return;
            }

            // Innate Lock: never allow foreign elemental auras (e.g. Hydro/Cryo on Blaze) to linger on innate elemental mobs
            if (innateElement != null && attackElement != innateElement) {
                target.hurt(level.damageSources().magic(), effectiveDamage);
                syncAuras(target, targetGauges.keySet(), null, attackElement, null, effectiveDamage);
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
            syncAuras(target, targetGauges.keySet(), null, attackElement, null, effectiveDamage);
            target.hurt(level.damageSources().magic(), effectiveDamage);
            return;
        }

        // Deal normal damage
        target.hurt(level.damageSources().magic(), effectiveDamage);
        syncAuras(target, targetGauges.keySet(), null, attackElement, null, effectiveDamage);
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
                if (isEntityInWater(target)) {
                    com.velorise.staves.event.ArcaneStaffEvents.freezeWaterNear(level, target.position(), 1);
                }
            }
            case QUICKEN -> {
                QUICKEN_UNTIL.put(target.getUUID(), level.getGameTime() + 140); // 7 seconds
                level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y + 0.8D, pos.z, 20, 0.35D, 0.5D, 0.35D, 0.04D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
            case BLOOM, REVERSE_BLOOM -> {
                BLOOM_SEED_UNTIL.put(target.getUUID(), level.getGameTime() + 120); // 6s Dendro Core window for Hyperbloom/Burgeon
                long now = level.getGameTime();
                Long lastSpawn = LAST_BLOOM_SPAWN.get(target.getUUID());
                if (lastSpawn == null || now - lastSpawn >= 10L) { // 0.5s ICD per target
                    DendroCoreManager.spawn(level, target.position(), attacker, (float) dmgMult);
                    LAST_BLOOM_SPAWN.put(target.getUUID(), now);
                }
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.8D, pos.z, 25, 0.8D, 0.5D, 0.8D, 0.04D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 1.1F);
            }
            case BURNING -> {
                BURNING_UNTIL.put(target.getUUID(), level.getGameTime() + 100); // 5 seconds
                target.setSecondsOnFire(5);
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
                CrystallizeShardManager.spawn(level, target.position(), auraElement);
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
            Vec3 diff = victim == target && attacker != null
                ? target.position().subtract(attacker.position())
                : victim.position().subtract(pos);
            Vec3 knock = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(0, 1, 0);
            victim.setDeltaMovement(victim.getDeltaMovement().add(knock.x * 0.85D, 0.45D, knock.z * 0.85D));
            victim.hurtMarked = true;
        }
    }

    private static void sendActionbar(@Nullable ServerPlayer player, String message) {
        // Silenced: all combat feedback (damage numbers, reactions, IMMUNE) is rendered as 3D floating text near the mob (Genshin style)
    }

    public static void drawVoltaicArc(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        int steps = Math.max(6, (int) Math.round(dist * 3.5D));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double offsetX = (level.random.nextDouble() - 0.5D) * 0.25D;
            double offsetY = (level.random.nextDouble() - 0.5D) * 0.25D;
            double offsetZ = (level.random.nextDouble() - 0.5D) * 0.25D;
            Vec3 p = start.add(diff.scale(t)).add(offsetX, offsetY, offsetZ);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.01D);
        }
    }

    /**
     * Server tick loop for decay, innate aura re-infusion after 0.8s, environmental water/rain extinguish, and periodic reactions.
     * Note: We NEVER iterate over ocean fish/squid or auto-attach HYDRO in tick().
     */
    public static void tick(MinecraftServer server) {
        // Tick physical Dendro Cores and Electrified Water in all levels
        for (ServerLevel lvl : server.getAllLevels()) {
            DendroCoreManager.tick(lvl);
            CrystallizeShardManager.tick(lvl);
            ElectrifiedWaterManager.tick(lvl);
            DragonfireCloudManager.tick(lvl);
        }

        long serverNow = server.overworld().getGameTime();
        if (serverNow % 100 == 0) {
            ICD_TRACKER.entrySet().removeIf(e -> (serverNow - e.getValue().firstHitTime) > 100);
        }

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

            // 1. Environmental Water / Fire / Poison check
            if (isEntityInWater(target)) {
                handleEnvironmentalWater(target);
                if (entityGauges.isEmpty()) {
                    it.remove();
                    continue;
                }
            } else if (target.isOnFire()) {
                handleEnvironmentalFireDamage(target);
            }

            // Special Mutant: Powered Creeper
            if (target instanceof Creeper creeper && creeper.isPowered()) {
                // 1. Passive 1-block aura field (every 10 ticks = 0.5s):
                if (now % 10 == 0) {
                    AABB auraField = creeper.getBoundingBox().inflate(1.2D);
                    for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, auraField, e -> e.isAlive() && !e.isRemoved() && e != creeper)) {
                        applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 0.0F);
                    }
                }

                // 2. Mutant Water Exception: Charged Creeper electrifies water in 3x3 zone!
                if (isEntityInWater(creeper)) {
                    ElectrifiedWaterManager.electrify(targetLevel, creeper.blockPosition(), 2.0D, 30, null, 2.5F);
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

            // 2B. Proximity Voltaic Arcing (ONLY when BOTH entities are submerged in water)
            // On land, two mobs with different auras standing next to each other DO NOT auto-trigger reactions.
            if (entityGauges.containsKey(Element.ELECTRO) && isEntityInWater(target) && (now % 4 == 0)) {
                final LivingEntity srcTarget = target;
                AABB arcBox = target.getBoundingBox().inflate(4.0D);
                for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, arcBox, e -> e.isAlive() && !e.isRemoved() && e != srcTarget)) {
                    if (isEntityInWater(nearby)) {
                        long pairKey = (((long) Math.min(target.getId(), nearby.getId())) << 32) | ((long) Math.max(target.getId(), nearby.getId()) & 0xFFFFFFFFL);
                        Long lastShock = LAST_PROXIMITY_SHOCK.get(pairKey);
                        if (lastShock == null || now - lastShock >= 18) { // 0.9s discharge interval
                            LAST_PROXIMITY_SHOCK.put(pairKey, now);
                            drawVoltaicArc(targetLevel, target.getEyePosition(), nearby.getEyePosition());
                            targetLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
                            applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 2.5F);
                            target.hurt(targetLevel.damageSources().magic(), 1.5F);
                            syncAuras(target, entityGauges.keySet(), Element.ELECTRO, Element.HYDRO, ElementalReaction.ELECTRO_CHARGED, 1.5F);
                            break;
                        }
                    }
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
                    syncAuras(target, entityGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, 2.5F);

                    // Chain discharge arc to nearby wet entities ONLY IF both are in water
                    if (isEntityInWater(target)) {
                        final LivingEntity srcTarget = target;
                        AABB chainBox = target.getBoundingBox().inflate(4.0D);
                        for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, chainBox, e -> e.isAlive() && e != srcTarget)) {
                            if (isEntityInWater(nearby)) {
                                drawVoltaicArc(targetLevel, target.getEyePosition(), nearby.getEyePosition());
                                nearby.hurt(targetLevel.damageSources().magic(), 2.0F);
                                applyElement(targetLevel, nearby, Element.ELECTRO, 0.5F, null, 2.0F);
                                targetLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, nearby.getX(), nearby.getY() + 0.8D, nearby.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.05D);
                            }
                        }
                    }

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

            // 3B. Proximity discharge between Electro and Hydro mobs (every 15 ticks)
            if (entityGauges.containsKey(Element.ELECTRO) && now % 15 == 0) {
                final LivingEntity electroMob = target;
                AABB proxBox = target.getBoundingBox().inflate(3.5D);
                for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, proxBox, e -> e.isAlive() && e != electroMob)) {
                    if (getEntityElements(nearby.getUUID()).contains(Element.HYDRO) || isEntityInWater(nearby)) {
                        targetLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.8F);
                        targetLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.05D);
                        targetLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, nearby.getX(), nearby.getY() + 0.8D, nearby.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.05D);
                        applyElement(targetLevel, nearby, Element.ELECTRO, 0.8F, null, 2.0F);
                        applyElement(targetLevel, target, Element.HYDRO, 0.8F, null, 0.0F);
                        break;
                    }
                }
            }

            // 4. Burning periodic damage (every 10 ticks = 0.5s)
            Long burningEnd = BURNING_UNTIL.get(id);
            if (burningEnd != null) {
                if (now >= burningEnd || !entityGauges.containsKey(Element.DENDRO) || target.isInWaterOrRain() || target.fireImmune() || target instanceof Warden) {
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

            // 6. Cryo Aura passive debuff (-15% Movement Speed, -15% Attack Speed)
            updateCryoDebuff(target, id, entityGauges.containsKey(Element.CRYO), innate);

            if (entityGauges.isEmpty()) {
                updateCryoDebuff(target, id, false, innate);
                it.remove();
            }

            if (changed) {
                syncAuras(target, entityGauges.keySet(), null, null, null);
            }

            // 7. Ambient aura particles (every 10 ticks)
            if (!entityGauges.isEmpty() && now % 10 == 0 && StavesConfig.SHOW_ELEMENTAL_PARTICLES.get()) {
                spawnAmbientParticles(targetLevel, target, entityGauges, now);
            }
        }

        // 8. Environmental water, fire, poison & cryo checks on active online players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isAlive()) {
                if (isEntityInWater(player)) {
                    handleEnvironmentalWater(player);
                } else if (player.isOnFire()) {
                    handleEnvironmentalFireDamage(player);
                }
                Collection<Element> playerElements = getEntityElements(player.getUUID());
                updateCryoDebuff(player, player.getUUID(), playerElements.contains(Element.CRYO), null);

                // Auto-register nearby Charged Creepers and process nearby innate mobs in water/rain within 32m
                if (player.level() instanceof ServerLevel sl && player.tickCount % 10 == 0) {
                    AABB searchBox = player.getBoundingBox().inflate(32.0D);
                    for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !e.isRemoved())) {
                        Element nearbyInnate = getInnateElement(nearby);
                        if (nearbyInnate != null && isEntityInWater(nearby)) {
                            handleEnvironmentalWater(nearby);
                        }
                        if (nearby instanceof Creeper creeper && creeper.isPowered() && !GAUGES.containsKey(creeper.getUUID())) {
                            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(creeper.getUUID(), k -> new EnumMap<>(Element.class));
                            gauges.put(Element.ELECTRO, new GaugeState(Element.ELECTRO, 1.25F));
                            syncAuras(creeper, gauges.keySet(), null, null, null);
                        }
                    }
                }
            }
        }
    }

    /**
     * Cryo Aura passive debuff (Genshin Impact):
     * When afflicted with Cryo, reduces movement speed by 15% (Slowness I) and attack speed by 15% (Mining Fatigue I).
     * Clears debuff immediately when Cryo aura expires or is removed by reactions (e.g. Melt).
     */
    private static void updateCryoDebuff(LivingEntity target, UUID id, boolean hasCryo, @Nullable Element innate) {
        if (innate == Element.CRYO) return;
        if (hasCryo) {
            if (!target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false, true));
            }
            if (!target.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 0, false, false, true));
            }
        } else {
            if (target.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                target.removeEffect(MobEffects.DIG_SLOWDOWN);
            }
            MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slow != null && slow.getAmplifier() == 0 && !FROZEN_UNTIL.containsKey(id)) {
                target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
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
