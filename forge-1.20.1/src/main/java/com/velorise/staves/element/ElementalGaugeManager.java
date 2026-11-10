package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
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
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager.
 * Tracks Gauge Units (U), Aura Decay Inheritance, Underlying Freeze & Quicken Auras,
 * Simultaneous Reaction Priority, Innate Lock, Environmental Auras, and combat feedback.
 */
public final class ElementalGaugeManager {
    public static final TagKey<EntityType<?>> PYRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(VeloriseStavesMod.MODID, "pyro_creatures"));
    public static final TagKey<EntityType<?>> HYDRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(VeloriseStavesMod.MODID, "hydro_creatures"));
    public static final TagKey<EntityType<?>> ELECTRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(VeloriseStavesMod.MODID, "electro_creatures"));
    public static final TagKey<EntityType<?>> CRYO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(VeloriseStavesMod.MODID, "cryo_creatures"));
    public static final TagKey<EntityType<?>> DENDRO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(VeloriseStavesMod.MODID, "dendro_creatures"));
    public static final TagKey<EntityType<?>> ANEMO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(VeloriseStavesMod.MODID, "anemo_creatures"));
    public static final TagKey<EntityType<?>> GEO_CREATURES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(VeloriseStavesMod.MODID, "geo_creatures"));

    private static final Map<UUID, Map<Element, GaugeState>> GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, GaugeState> FROZEN_GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, GaugeState> QUICKEN_GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FROZEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> QUICKEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BURNING_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BURN_PYRO_REFRESH = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> INNATE_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ELECTRO_CHARGE_TICK = new ConcurrentHashMap<>();
    private static final Map<Long, Long> LAST_PROXIMITY_SHOCK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BLOOM_SPAWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SUPERCONDUCT_UNTIL = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> IS_PROCESSING_DAMAGE = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> IS_PROCESSING_REACTION = ThreadLocal.withInitial(() -> false);

    // Genshin Impact Simultaneous Reaction Priority order for coexisting auras
    private static final Element[] REACTION_PRIORITY_ORDER = {
        Element.ELECTRO,
        Element.PYRO,
        Element.HYDRO,
        Element.CRYO,
        Element.DENDRO
    };

    private static class IcdEntry {
        int hitCount;
        long firstHitTime;

        IcdEntry(int hitCount, long firstHitTime) {
            this.hitCount = hitCount;
            this.firstHitTime = firstHitTime;
        }
    }

    private static final Map<Long, IcdEntry> ICD_TRACKER = new ConcurrentHashMap<>();
    private static final Map<Long, Long> REACTION_ICD_TRACKER = new ConcurrentHashMap<>();

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

    public static int getEntityMaxDurationTicks(UUID entityId) {
        int max = 0;
        Map<Element, GaugeState> gauges = GAUGES.get(entityId);
        if (gauges != null) {
            for (GaugeState gs : gauges.values()) {
                if (gs != null) {
                    int rem = gs.getRemainingTicks();
                    if (rem > max) max = rem;
                }
            }
        }
        GaugeState qg = QUICKEN_GAUGES.get(entityId);
        if (qg != null) {
            max = Math.max(max, qg.getRemainingTicks());
        }
        GaugeState fg = FROZEN_GAUGES.get(entityId);
        if (fg != null) {
            max = Math.max(max, fg.getRemainingTicks());
        }
        return max;
    }

    public static @Nullable Map<Element, GaugeState> getEntityGauges(UUID entityId) {
        return GAUGES.get(entityId);
    }

    public static @Nullable GaugeState getQuickenGauge(UUID entityId) {
        return QUICKEN_GAUGES.get(entityId);
    }

    public static @Nullable GaugeState getFrozenGauge(UUID entityId) {
        return FROZEN_GAUGES.get(entityId);
    }

    /**
     * Returns all active display elements on the entity (including Quicken Dendro aura and Frozen Cryo aura).
     */
    public static Collection<Element> getEffectiveElements(LivingEntity target, Collection<Element> baseElements) {
        EnumSet<Element> set = EnumSet.noneOf(Element.class);
        if (baseElements != null) {
            set.addAll(baseElements);
        }
        UUID id = target.getUUID();
        long now = target.level().getGameTime();
        Long quickenEnd = QUICKEN_UNTIL.get(id);
        GaugeState qg = QUICKEN_GAUGES.get(id);
        if ((quickenEnd != null && now < quickenEnd) || (qg != null && !qg.isExpired())) {
            set.add(Element.DENDRO);
        }
        Long frozenEnd = FROZEN_UNTIL.get(id);
        GaugeState fg = FROZEN_GAUGES.get(id);
        if ((frozenEnd != null && now < frozenEnd) || (fg != null && !fg.isExpired())) {
            set.add(Element.CRYO);
        }
        Element innate = getInnateElement(target);
        if (innate != null && innate.isAura()) {
            set.add(innate);
        }
        return set;
    }

    public static void syncAuras(
            LivingEntity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage
    ) {
        UUID id = target.getUUID();
        long now = target.level().getGameTime();
        Collection<Element> effective = getEffectiveElements(target, elements);
        int durationTicks = getEntityMaxDurationTicks(id);
        Long quickenEnd = QUICKEN_UNTIL.get(id);
        if (quickenEnd != null && now < quickenEnd) {
            durationTicks = Math.max(durationTicks, (int) (quickenEnd - now));
        }
        Long frozenEnd = FROZEN_UNTIL.get(id);
        if (frozenEnd != null && now < frozenEnd) {
            durationTicks = Math.max(durationTicks, (int) (frozenEnd - now));
        }
        Element innate = getInnateElement(target);
        if (innate != null && effective.contains(innate)) {
            durationTicks = -1; // -1 represents permanent innate aura
        }
        com.velorise.staves.network.ModNetwork.sendAuraSync(target, effective, reactedAura, trigger, reaction, damage, durationTicks);
    }

    public static Collection<Element> getEntityElements(UUID entityId) {
        EnumSet<Element> set = EnumSet.noneOf(Element.class);
        Map<Element, GaugeState> gauges = GAUGES.get(entityId);
        if (gauges != null) {
            set.addAll(gauges.keySet());
        }
        GaugeState qg = QUICKEN_GAUGES.get(entityId);
        if (qg != null && !qg.isExpired()) {
            set.add(Element.DENDRO);
        }
        GaugeState fg = FROZEN_GAUGES.get(entityId);
        if (fg != null && !fg.isExpired()) {
            set.add(Element.CRYO);
        }
        return set;
    }

    public static boolean hasElement(LivingEntity target, Element element) {
        if (target == null) return false;
        return getEntityElements(target.getUUID()).contains(element);
    }

    public static boolean hasSuperconductDebuff(LivingEntity entity, long now) {
        Long until = SUPERCONDUCT_UNTIL.get(entity.getUUID());
        return until != null && now < until;
    }

    public static boolean isPhysicalDamage(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return false;
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        if (source.is(DamageTypeTags.IS_FREEZING)) return false;
        if (source.is(DamageTypeTags.IS_LIGHTNING)) return false;
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) return false;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.GENERIC)) return false;
        if (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT)) return true;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return true;
        return source.getDirectEntity() instanceof net.minecraft.world.entity.player.Player
                || source.getEntity() instanceof net.minecraft.world.entity.player.Player;
    }

    public static void clearAll() {
        GAUGES.clear();
        FROZEN_GAUGES.clear();
        QUICKEN_GAUGES.clear();
        FROZEN_UNTIL.clear();
        QUICKEN_UNTIL.clear();
        BURNING_UNTIL.clear();
        LAST_BURN_PYRO_REFRESH.clear();
        INNATE_COOLDOWN_UNTIL.clear();
        ELECTRO_CHARGE_TICK.clear();
        LAST_PROXIMITY_SHOCK.clear();
        LAST_BLOOM_SPAWN.clear();
        SUPERCONDUCT_UNTIL.clear();
        ICD_TRACKER.clear();
        REACTION_ICD_TRACKER.clear();
        DendroCoreManager.clearAll();
        CrystallizeShardManager.clearAll();
        ElectrifiedWaterManager.clearAll();
        DragonfireCloudManager.clearAll();
        GhastfireFieldManager.clearAll();
        EnvironmentalBlockManager.clearAll();
    }

    private static void clearFreezeState(LivingEntity target, UUID id) {
        FROZEN_GAUGES.remove(id);
        FROZEN_UNTIL.remove(id);
        target.setTicksFrozen(0);
        MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 6) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private static void clearQuickenState(UUID id) {
        QUICKEN_GAUGES.remove(id);
        QUICKEN_UNTIL.remove(id);
    }

    /**
     * Checks if the entity is physically submerged in water blocks/fluid (excluding rain on dry land).
     * Used for water surface freezing, electrified water zones, and underwater voltaic arcing.
     */
    public static boolean isEntitySubmergedInWater(LivingEntity target) {
        if (target.isInWater()) return true;
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
     * Checks if the entity is exposed to environmental Hydro (submerged in water, bubble column, or standing in rain).
     */
    public static boolean isEntityInWater(LivingEntity target) {
        return isEntitySubmergedInWater(target) || target.isInWaterRainOrBubble() || target.isInWaterOrRain();
    }

    /**
     * Handles environmental damage types and innate elemental mob attacks.
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

                // 7b. Warden Sonic Boom -> Anemo (2.0U) (swirls target's elements)
                if (source.is(DamageTypes.SONIC_BOOM) || "sonic_boom".equals(source.type().msgId())) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.ANEMO, 2.0F, attacker, 0.0F);
                    return;
                }

                // 8. Falling Stalactite / Stalagmite / Anvil / Falling Block -> Geo (1.5U)
                if (source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.STALAGMITE)
                        || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_BLOCK)) {
                    applyElement(level, target, Element.GEO, 1.5F, null, 0.0F);
                    return;
                }

                // 9. Trident damage (Thrown Trident, Trident melee, or Drowned attack) -> Hydro (1.0U)
                if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident
                        || source.is(DamageTypes.TRIDENT)
                        || (source.getEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().is(Items.TRIDENT))) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.HYDRO, 1.0F, attacker, 0.0F);
                    return;
                }

                // 10. Direct Ender projectiles (Dragon Fireball, Shulker Bullet, Ender Pearl) -> Electro (1.5U)
                if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.DragonFireball
                        || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ShulkerBullet
                        || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl) {
                    applyElement(level, target, Element.ELECTRO, 1.5F, null, 0.0F);
                    return;
                }

                // 11. Husk melee attack -> Geo (0.5U)
                if (source.getEntity() instanceof Husk) {
                    applyElement(level, target, Element.GEO, 0.5F, null, 0.0F);
                    return;
                }

                // 12. Ravager: Roar shockwave -> Anemo (1.5U), Stomp/Melee -> Geo (1.0U)
                if (source.getEntity() instanceof Ravager ravager) {
                    if (ravager.getRoarTick() > 0) {
                        applyElement(level, target, Element.ANEMO, 1.5F, null, 0.0F);
                    } else {
                        applyElement(level, target, Element.GEO, 1.0F, null, 0.0F);
                    }
                    return;
                }

                // 13. Evoker Fangs -> Geo (1.0U) (only for natural/wild Evokers)
                if (source.getDirectEntity() instanceof EvokerFangs fangs) {
                    if (fangs.getTags().contains("velorise_staves:geo_fangs")) {
                        return; // Handled with custom Geo damage & immunity at bite time
                    }
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.GEO, 1.0F, attacker, 0.0F);
                    return;
                }

                // 14. Vex melee attack -> Electro (0.5U)
                if (source.getEntity() instanceof Vex) {
                    applyElement(level, target, Element.ELECTRO, 0.5F, null, 0.0F);
                    return;
                }

                // 15. Fireworks explosion -> Pyro (1.5U)
                if (source.is(DamageTypes.FIREWORKS)
                        || source.getDirectEntity() instanceof FireworkRocketEntity) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.PYRO, 1.5F, attacker, 0.0F);
                    return;
                }

                // 16. Attacker with Innate Element
                if (source.getEntity() instanceof LivingEntity attackerMob && attackerMob != target) {
                    Element innate = getInnateElement(attackerMob);
                    if (innate != null) {
                        boolean isMutated = isMutatedInnate(attackerMob);
                        boolean canApply = true;
                        if (isMutated) {
                            // Mutated mobs only apply their innate element on plain, unenchanted attacks/projectiles
                            if (source.is(DamageTypeTags.IS_FIRE)
                                    || (source.getDirectEntity() != null && source.getDirectEntity().isOnFire())
                                    || attackerMob.getMainHandItem().isEnchanted()
                                    || attackerMob.getOffhandItem().isEnchanted()
                                    || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.SpectralArrow) {
                                canApply = false;
                            } else if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Arrow arrow) {
                                if (arrow.getColor() != -1) {
                                    canApply = false;
                                }
                            }
                        }
                        if (canApply) {
                            ServerPlayer attackerPlayer = attackerMob instanceof ServerPlayer sp ? sp : null;
                            applyElement(level, target, innate, 1.0F, attackerPlayer, 0.0F);
                        }
                    }
                }
            }
        } finally {
            IS_PROCESSING_DAMAGE.set(false);
        }
    }

    /**
     * Pre-damage hook for environmental fire damage (sunlight burning on Stray, fire blocks, lava, magma).
     * Amplifies incoming fire damage when Forward Melt (2.0x) or Reverse Vaporize (1.5x) is triggered.
     */
    public static float modifyIncomingEnvironmentalDamage(LivingEntity target, DamageSource source, float amount) {
        if (!target.isAlive() || target.isSpectator() || target.level().isClientSide()) return amount;
        if (IS_PROCESSING_REACTION.get()) return amount;
        if (!source.is(DamageTypeTags.IS_FIRE)) return amount;
        if (isEntityInWater(target)) return amount;

        Element innate = getInnateElement(target);
        if (innate == Element.PYRO) return 0.0F;

        UUID id = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        long now = target.level().getGameTime();
        double dmgMult = StavesConfig.DAMAGE_MULTIPLIER.get();

        // 1. Frozen or Cryo (including Innate Cryo like Stray burning in sunlight) -> Forward Melt (2.0x damage)
        GaugeState fg = FROZEN_GAUGES.get(id);
        if ((fg != null && !fg.isExpired()) || targetGauges.containsKey(Element.CRYO) || innate == Element.CRYO) {
            if (checkReactionIcd(target, ElementalReaction.FORWARD_MELT, now)) {
                if (fg != null) {
                    clearFreezeState(target, id);
                } else {
                    targetGauges.remove(Element.CRYO);
                }
                enforceInnateLock(target, targetGauges, id, innate, now);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.8D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.04D);
                }
                float meltDmg = Math.max(amount * ElementalReaction.FORWARD_MELT.getDamageMultiplier(), (float) (2.0F * dmgMult));
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, meltDmg);
                return meltDmg;
            }
            return amount;
        }

        // 2. Hydro (including Innate Hydro) -> Reverse Vaporize (1.5x damage)
        if (targetGauges.containsKey(Element.HYDRO) || innate == Element.HYDRO) {
            if (checkReactionIcd(target, ElementalReaction.REVERSE_VAPORIZE, now)) {
                targetGauges.remove(Element.HYDRO);
                enforceInnateLock(target, targetGauges, id, innate, now);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.3F);
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
                }
                float vapeDmg = Math.max(amount * ElementalReaction.REVERSE_VAPORIZE.getDamageMultiplier(), (float) (1.5F * dmgMult));
                syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE, vapeDmg);
                return vapeDmg;
            }
            return amount;
        }

        return amount;
    }

    /**
     * Maintains Pyro 1U aura on burning entities in tick() without prematurely stealing Melt/Vaporize before the fire damage tick.
     */
    public static void maintainBurningPyroAura(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || isEntityInWater(target)) return;
        Element innate = getInnateElement(target);
        if (innate == Element.PYRO || innate == Element.CRYO || innate == Element.HYDRO) return;

        UUID id = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        GaugeState fg = FROZEN_GAUGES.get(id);
        if ((fg != null && !fg.isExpired()) || targetGauges.containsKey(Element.CRYO) || targetGauges.containsKey(Element.HYDRO)) {
            return; // Wait for actual fire damage tick to trigger Forward Melt / Reverse Vaporize with amplified damage
        }
        handleEnvironmentalFireDamage(target);
    }

    /**
     * Handles environmental fire damage (lava, fire block, campfire, magma, burning) on entities.
     */
    public static void handleEnvironmentalFireDamage(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator()) return;
        Element innate = getInnateElement(target);
        if (innate == Element.PYRO) {
            return;
        }
        if (isEntityInWater(target)) return;

        UUID id = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        long now = target.level().getGameTime();

        // 1. Check if entity has Frozen or Cryo -> Forward Melt (if not already handled in modifyIncomingEnvironmentalDamage)
        GaugeState fg = FROZEN_GAUGES.get(id);
        if ((fg != null && !fg.isExpired()) || targetGauges.containsKey(Element.CRYO) || innate == Element.CRYO) {
            if (!checkReactionIcd(target, ElementalReaction.FORWARD_MELT, now)) return;
            if (fg != null) {
                clearFreezeState(target, id);
            } else {
                targetGauges.remove(Element.CRYO);
            }
            enforceInnateLock(target, targetGauges, id, innate, now);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.8D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.04D);
            }
            syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, 2.0F);
            return;
        }

        // 2. Check if entity has Electro -> Overloaded
        if (targetGauges.containsKey(Element.ELECTRO) || innate == Element.ELECTRO) {
            if (!checkReactionIcd(target, ElementalReaction.OVERLOADED, now)) return;
            targetGauges.remove(Element.ELECTRO);
            enforceInnateLock(target, targetGauges, id, innate, now);
            if (target.level() instanceof ServerLevel sl) {
                triggerOverloadExplosion(sl, target, null, 1.0D);
            }
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.PYRO, ElementalReaction.OVERLOADED, 6.0F);
            return;
        }

        // 3. Check if entity has Hydro -> Reverse Vaporize
        if (targetGauges.containsKey(Element.HYDRO) || innate == Element.HYDRO) {
            if (!checkReactionIcd(target, ElementalReaction.REVERSE_VAPORIZE, now)) return;
            targetGauges.remove(Element.HYDRO);
            enforceInnateLock(target, targetGauges, id, innate, now);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.3F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
            }
            syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE, 1.5F);
            return;
        }

        // 4. Dendro or Quicken -> Burning
        GaugeState qg = QUICKEN_GAUGES.get(id);
        boolean hasQuicken = qg != null && !qg.isExpired();
        if (targetGauges.containsKey(Element.DENDRO) || hasQuicken || innate == Element.DENDRO) {
            if (!BURNING_UNTIL.containsKey(id)) {
                BURNING_UNTIL.put(id, now + 200L);
                LAST_BURN_PYRO_REFRESH.put(id, now);
                if (innate == null) {
                    targetGauges.put(Element.PYRO, GaugeState.createBurning(2.0F));
                }
                target.setSecondsOnFire(5);
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.PYRO, ElementalReaction.BURNING, 0.0F);
            }
            return;
        }

        // No reactive aura -> apply or refresh Pyro 1.0U (on normal mobs, innate mobs on fire, and players)
        if (innate != Element.PYRO) {
            boolean wasMissing = !targetGauges.containsKey(Element.PYRO);
            if (wasMissing) {
                targetGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.25F));
                syncAuras(target, targetGauges.keySet(), null, null, null, 0.0F);
            } else {
                GaugeState existing = targetGauges.get(Element.PYRO);
                if (existing != null && (target.isInLava() || target.isOnFire() || target.getRemainingFireTicks() > 0)) {
                    existing.refresh(1.25F);
                    if (now % 20 == 0) {
                        syncAuras(target, targetGauges.keySet(), null, null, null, 0.0F);
                    }
                }
            }
        }
    }

    public static boolean isNaturallyWaterSensitive(LivingEntity target) {
        return target.isSensitiveToWater()
                || target instanceof Blaze
                || target instanceof Strider
                || target instanceof MagmaCube
                || target instanceof SnowGolem
                || target instanceof EnderMan;
    }

    /**
     * Handles environmental water/rain on entities holding active auras.
     */
    public static void handleEnvironmentalWater(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator()) return;
        UUID id = target.getUUID();
        Element innate = getInnateElement(target);

        // 1A. Innate Dendro mob: immune to self-blooming in rain/water
        if (innate == Element.DENDRO) {
            return;
        }

        // 1B. Innate Pyro mob in water or rain -> Vaporize!
        if (innate == Element.PYRO) {
            long now = target.level().getGameTime();
            Long lastVape = INNATE_COOLDOWN_UNTIL.get(id);
            if (lastVape == null || now >= lastVape) {
                INNATE_COOLDOWN_UNTIL.put(id, now + 20L); // 1.0s ICD
                boolean sensitive = isNaturallyWaterSensitive(target);
                float vapeDmg = sensitive ? 3.0F : 0.0F;
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 14, 0.3D, 0.3D, 0.3D, 0.04D);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.2F);
                    if (sensitive) {
                        target.hurt(sl.damageSources().magic(), vapeDmg);
                    }
                }
                syncAuras(target, Collections.singleton(Element.PYRO), Element.PYRO, Element.HYDRO, ElementalReaction.FORWARD_VAPORIZE, vapeDmg);
            }
            return;
        }

        // 1C. Innate Electro mob in water or rain -> Electro discharge into water, immune to self-shock!
        if (innate == Element.ELECTRO) {
            long now = target.level().getGameTime();
            Long lastEc = INNATE_COOLDOWN_UNTIL.get(id);
            if (lastEc == null || now >= lastEc) {
                INNATE_COOLDOWN_UNTIL.put(id, now + 20L); // 1.0s ICD
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.8D, target.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.02D);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.8F);
                    // Innate Electro beings are immune to their own electrical shocks in water

                    if (isEntitySubmergedInWater(target)) {
                        AABB chainBox = target.getBoundingBox().inflate(1.0D);
                        for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, chainBox, e -> e.isAlive() && e != target)) {
                            if (isEntitySubmergedInWater(nearby) && getInnateElement(nearby) != Element.ELECTRO) {
                                drawVoltaicArc(sl, target, nearby);
                                applyElement(sl, nearby, Element.ELECTRO, 0.5F, null, 1.0F);
                            }
                        }
                    }
                }
            }
            return;
        }

        // 1D. Innate Cryo mob physically in water or stepping on water surface -> Freeze Water!
        if (innate == Element.CRYO) {
            checkCryoWaterFreezing(target);
            return;
        }

        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        long now = target.level().getGameTime();

        // 2. Pyro in water/rain -> Extinguish & Vaporize
        if (targetGauges.containsKey(Element.PYRO)) {
            targetGauges.remove(Element.PYRO);
            BURNING_UNTIL.remove(id);
            LAST_BURN_PYRO_REFRESH.remove(id);
            target.clearFire();
            boolean sensitive = isNaturallyWaterSensitive(target);
            float vapeDmg = sensitive ? 2.5F : 0.0F;
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.03D);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.3F);
                if (sensitive) {
                    target.hurt(target.level().damageSources().magic(), vapeDmg);
                }
            }
            syncAuras(target, targetGauges.keySet(), Element.PYRO, Element.HYDRO, ElementalReaction.FORWARD_VAPORIZE, vapeDmg);
            return;
        }

        // 3. Applied Dendro or Quicken in water/rain -> Bloom reaction & spawn Dendro Core!
        GaugeState qg = QUICKEN_GAUGES.get(id);
        boolean hasQuicken = qg != null && !qg.isExpired();
        if (targetGauges.containsKey(Element.DENDRO) || hasQuicken) {
            GaugeState dg = targetGauges.get(Element.DENDRO);
            if (dg != null) {
                dg.consume(0.5F);
                if (dg.isExpired()) targetGauges.remove(Element.DENDRO);
            }
            if (qg != null) {
                qg.consume(0.5F);
                if (qg.isExpired()) clearQuickenState(id);
            }
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
            return;
        }

        // 4. Electro in water/rain -> Trigger Electro-Charged shock on entity itself (with reaction ICD, 1.0F = 0.5 heart damage)
        if (targetGauges.containsKey(Element.ELECTRO)) {
            if (!checkReactionIcd(target, ElementalReaction.ELECTRO_CHARGED, now)) return;
            GaugeState electro = targetGauges.get(Element.ELECTRO);
            if (electro != null) {
                electro.consume(0.4F);
                if (electro.isExpired()) targetGauges.remove(Element.ELECTRO);
            }
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.8D, target.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.02D);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.8F);
                target.hurt(sl.damageSources().magic(), 1.0F);
            }
            syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, 1.0F);
            return;
        }

        // 5. Environmental Wet: Apply or keep Hydro refreshed while in water or rain
        if (innate == null) {
            if (!targetGauges.containsKey(Element.HYDRO)) {
                targetGauges.put(Element.HYDRO, new GaugeState(Element.HYDRO, 1.25F));
                syncAuras(target, targetGauges.keySet(), null, null, null, 0.0F);
            } else {
                GaugeState hydro = targetGauges.get(Element.HYDRO);
                if (hydro != null) {
                    hydro.refresh(1.25F);
                }
            }
        }
    }

    public static void checkCryoWaterFreezing(LivingEntity entity) {
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel sl)) return;
        if (getInnateElement(entity) != Element.CRYO && !hasElement(entity, Element.CRYO)) return;

        BlockPos pos = entity.blockPosition();
        BlockPos below = pos.below();
        Vec3 motion = entity.getDeltaMovement();
        Vec3 predictedPos = entity.position().add(motion);
        BlockPos predBlock = BlockPos.containing(predictedPos);

        boolean touchingWater = entity.isInWater() || isEntitySubmergedInWater(entity)
                || sl.getFluidState(pos).is(FluidTags.WATER)
                || sl.getFluidState(below).is(FluidTags.WATER)
                || sl.getFluidState(predBlock).is(FluidTags.WATER)
                || sl.getFluidState(predBlock.below()).is(FluidTags.WATER);

        if (!touchingWater) {
            AABB scan = entity.getBoundingBox().expandTowards(motion).inflate(0.4D, 0.5D, 0.4D);
            BlockPos min = BlockPos.containing(scan.minX, scan.minY, scan.minZ);
            BlockPos max = BlockPos.containing(scan.maxX, scan.maxY, scan.maxZ);
            for (BlockPos p : BlockPos.betweenClosed(min, max)) {
                if (sl.hasChunkAt(p) && sl.getFluidState(p).is(FluidTags.WATER)) {
                    touchingWater = true;
                    break;
                }
            }
        }

        if (touchingWater) {
            com.velorise.staves.event.ArcaneStaffEvents.freezeWaterNear(sl, entity.position(), 2);
            if (motion.lengthSqr() > 0.005D) {
                com.velorise.staves.event.ArcaneStaffEvents.freezeWaterNear(sl, predictedPos, 2);
            }
            // If the water block at the entity's feet just froze into Frosted Ice while falling/pushed, lift onto the ice surface
            BlockPos feetPos = entity.blockPosition();
            if (sl.getBlockState(feetPos).is(Blocks.FROSTED_ICE)) {
                entity.setPos(entity.getX(), feetPos.getY() + 1.0D, entity.getZ());
                if (motion.y < 0.0D) {
                    entity.setDeltaMovement(motion.x, 0.0D, motion.z);
                }
            }
        }
    }

    public static void checkInnateMutation(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof net.minecraft.world.entity.player.Player || isBossEntity(entity) || isPassiveAquaticOrAmbient(entity)) return;

        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        if (data.contains("VeloriseInnateChecked") || entity.getTags().contains("VeloriseInnateChecked")) return;
        data.putBoolean("VeloriseInnateChecked", true);
        entity.addTag("VeloriseInnateChecked");

        // If the entity already has a natural innate element, do not mutate it
        if (getNaturalInnateElement(entity) != null) return;

        // 1% chance (0.01) to mutate into one of the 7 innate elements
        if (entity.getRandom().nextFloat() < 0.01F) {
            Element[] elements = Element.values();
            Element chosen = elements[entity.getRandom().nextInt(elements.length)];
            data.putString("VeloriseInnate", chosen.name());
            entity.addTag("VeloriseInnate:" + chosen.name());

            // Immediately initialize innate aura in GAUGES so it syncs and renders right away
            if (chosen.isAura()) {
                Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(entity.getUUID(), k -> new ConcurrentHashMap<>());
                gauges.put(chosen, new GaugeState(chosen, 1.25F));
                syncAuras(entity, Collections.singleton(chosen), null, null, null, 0.0F);
            }
        }
    }

    public static @Nullable Element getInnateElement(LivingEntity target) {
        if (!target.level().isClientSide() && !target.getPersistentData().contains("VeloriseInnateChecked") && !target.getTags().contains("VeloriseInnateChecked")) {
            checkInnateMutation(target);
        }
        if (target.getPersistentData().contains("VeloriseInnate")) {
            String elemName = target.getPersistentData().getString("VeloriseInnate");
            try {
                return Element.valueOf(elemName);
            } catch (IllegalArgumentException ignored) {}
        }
        for (String tag : target.getTags()) {
            if (tag.startsWith("VeloriseInnate:")) {
                try {
                    return Element.valueOf(tag.substring("VeloriseInnate:".length()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return getNaturalInnateElement(target);
    }

    public static boolean isMutatedInnate(LivingEntity entity) {
        if (entity.getPersistentData().contains("VeloriseInnate")) return true;
        for (String tag : entity.getTags()) {
            if (tag.startsWith("VeloriseInnate:")) return true;
        }
        return false;
    }

    public static @Nullable Element getNaturalInnateElement(LivingEntity target) {
        EntityType<?> type = target.getType();
        if (type.is(PYRO_CREATURES)) return Element.PYRO;
        if (type.is(HYDRO_CREATURES)) return Element.HYDRO;
        if (type.is(CRYO_CREATURES)) return Element.CRYO;
        if (type.is(ELECTRO_CREATURES)) return Element.ELECTRO;
        if (type.is(DENDRO_CREATURES) && !(target instanceof MagmaCube)) return Element.DENDRO;
        if (type.is(ANEMO_CREATURES)) return Element.ANEMO;
        if (type.is(GEO_CREATURES)) return Element.GEO;

        // Fallback / dynamic state checks
        if (target instanceof Blaze
                || target instanceof MagmaCube
                || target instanceof Strider
                || target instanceof Ghast
                || target instanceof ZombifiedPiglin
                || target instanceof Zoglin
                || target instanceof WitherSkeleton) {
            return Element.PYRO;
        }
        if (target instanceof Stray || target instanceof SnowGolem || target instanceof PolarBear) {
            return Element.CRYO;
        }
        if (target instanceof Guardian || target instanceof ElderGuardian || target instanceof Drowned
                || target instanceof Axolotl || target instanceof Frog) {
            return Element.HYDRO;
        }
        if (target.getType().getDescriptionId().contains("breeze")) {
            return Element.ANEMO;
        }
        if ((target instanceof Creeper creeper && creeper.isPowered())
                || target instanceof EnderMan
                || target instanceof EnderDragon
                || target instanceof Endermite
                || target instanceof Shulker
                || target instanceof Vex) {
            return Element.ELECTRO;
        }
        if (target instanceof MushroomCow
                || target instanceof Bee || target instanceof CaveSpider
                || (target instanceof Slime && !(target instanceof MagmaCube))
                || target.getType().getDescriptionId().contains("bogged")) {
            return Element.DENDRO;
        }
        if (target instanceof IronGolem || target.getType().getDescriptionId().contains("armadillo")) {
            return Element.GEO;
        }
        return null;
    }

    public static boolean isBossEntity(LivingEntity entity) {
        return entity instanceof Warden
                || entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof ElderGuardian;
    }

    public static boolean isPassiveAquaticOrAmbient(LivingEntity entity) {
        return entity instanceof WaterAnimal || entity instanceof AmbientCreature;
    }

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

    public static int getReactionCooldownTicks(ElementalReaction reaction) {
        return switch (reaction) {
            case OVERLOADED, SUPERCONDUCT, SHATTER, SWIRL -> 10; // 0.5s (10 ticks)
            case ELECTRO_CHARGED -> 10; // 0.5s (10 ticks)
            case BLOOM, REVERSE_BLOOM, HYPERBLOOM, BURGEON -> 10; // 0.5s (10 ticks)
            case CRYSTALLIZE -> 20; // 1.0s (20 ticks - Genshin shard drop cooldown)
            case FROZEN -> 10; // 0.5s (10 ticks)
            case BURNING -> 8; // 0.4s
            case AGGRAVATE, SPREAD -> 5; // 0.25s
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE, FORWARD_MELT, REVERSE_MELT, OVERVAPE -> 8; // 0.4s
            default -> 5;
        };
    }

    public static boolean checkReactionIcd(LivingEntity target, ElementalReaction reaction, long now) {
        long key = (((long) target.getId()) << 8) | (reaction.ordinal() & 0xFFL);
        Long lastTrigger = REACTION_ICD_TRACKER.get(key);
        int cdTicks = getReactionCooldownTicks(reaction);
        if (lastTrigger != null && (now - lastTrigger) < cdTicks) {
            return false;
        }
        REACTION_ICD_TRACKER.put(key, now);
        return true;
    }

    /**
     * Resolves an elemental attack hitting a target entity.
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

        // Crystallize Shield matching elemental resistance:
        // 250% absorption efficiency -> 40% incoming damage (damage / 2.5), only while Absorption hearts remain
        if (target instanceof ServerPlayer sp && effectiveDamage > 0.0F) {
            Element activeShield = CrystallizeShardManager.getActiveShield(sp, level.getGameTime());
            if (activeShield == attackElement) {
                effectiveDamage *= 0.4F;
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.8F);
            }
        }

        // Always allow Electro/Pyro attacks to detonate nearby physical 3D Dendro Cores (Hyperbloom / Burgeon)
        DendroCoreManager.checkReactionsNear(level, target.position(), 3.5D, attackElement, attacker);

        // Same-element immunity (Genshin Impact): Innate elemental beings are 100% immune to their own element
        Element innateElement = getInnateElement(target);
        if (innateElement != null && attackElement == innateElement) {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
            Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(target.getUUID(), k -> new ConcurrentHashMap<>());
            syncAuras(target, targetGauges.keySet(), innateElement, attackElement, ElementalReaction.IMMUNE);
            return;
        }

        // Warden & Vanilla Fire-Immune Mob Check for Pyro attacks:
        // Deal 0 damage, but ALLOW Pyro aura to be applied!
        if (attackElement == Element.PYRO && (target instanceof Warden || (target.fireImmune() && innateElement == null))) {
            baseDamage = 0.0F;
            effectiveDamage = 0.0F;
        }

        if (!egtEnabled) {
            target.hurt(level.damageSources().magic(), effectiveDamage);
            return;
        }

        long now = level.getGameTime();

        // Standard ICD check (3-Hit / 2.5s rule) for elemental attacks
        boolean canApply = checkStandardIcd(target, attackElement, attacker, now);
        if (!canApply) {
            if (effectiveDamage > 0.0F) {
                target.hurt(level.damageSources().magic(), effectiveDamage);
            }
            return;
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
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>());

        // 0A. Innate Elemental Creature Logic
        Element innateElement = getInnateElement(target);
        if (innateElement != null) {
            if (attackElement == innateElement) {
                if (innateElement.isAura() && !targetGauges.containsKey(innateElement)) {
                    targetGauges.put(innateElement, new GaugeState(innateElement, 1.25F));
                }
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
                syncAuras(target, targetGauges.keySet(), innateElement, attackElement, ElementalReaction.IMMUNE);
                return;
            }

            if (innateElement == Element.ANEMO && (attackElement == Element.PYRO || attackElement == Element.HYDRO || attackElement == Element.ELECTRO || attackElement == Element.CRYO)) {
                if (checkReactionIcd(target, ElementalReaction.SWIRL, now)) {
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    float removedU = Math.min(1.0F, attackU * 0.5F);
                    executeReactionEffect(level, target, ElementalReaction.SWIRL, attackElement, removedU, attacker, dmgMult);
                    syncAuras(target, Collections.singleton(Element.ANEMO), attackElement, Element.ANEMO, ElementalReaction.SWIRL, effectiveDamage);
                    return;
                }
            } else if (innateElement == Element.GEO && (attackElement == Element.PYRO || attackElement == Element.HYDRO || attackElement == Element.ELECTRO || attackElement == Element.CRYO)) {
                if (checkReactionIcd(target, ElementalReaction.CRYSTALLIZE, now)) {
                    executeReactionEffect(level, target, ElementalReaction.CRYSTALLIZE, attackElement, 0.5F, attacker, dmgMult);
                }
                // If target was set on fire or is burning and hit by Pyro, ensure Pyro is attached/refreshed
                if (attackElement == Element.PYRO && (target.isOnFire() || target.getRemainingFireTicks() > 0 || target.isInLava())) {
                    if (targetGauges.containsKey(Element.PYRO)) {
                        targetGauges.get(Element.PYRO).refresh(attackU);
                    } else {
                        targetGauges.put(Element.PYRO, new GaugeState(Element.PYRO, attackU));
                    }
                }
                // If target has no attached coexisting auras (e.g. Dendro), finish Crystallize hit directly
                if (!targetGauges.containsKey(Element.DENDRO)) {
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    syncAuras(target, targetGauges.keySet(), attackElement, Element.GEO, ElementalReaction.CRYSTALLIZE, effectiveDamage);
                    return;
                }
                // Otherwise, fall through to resolve reaction with the attached DENDRO aura (Quicken, Bloom, Burning)!
            } else if (innateElement == Element.GEO && attackElement == Element.DENDRO) {
                // Geo and Dendro do not react; Dendro attaches as an independent coexisting aura
                if (targetGauges.containsKey(Element.DENDRO)) {
                    targetGauges.get(Element.DENDRO).refresh(attackU);
                } else {
                    targetGauges.put(Element.DENDRO, new GaugeState(Element.DENDRO, attackU));
                }
                target.hurt(level.damageSources().magic(), effectiveDamage);
                syncAuras(target, targetGauges.keySet(), Element.GEO, Element.DENDRO, null, effectiveDamage);
                return;
            }

            if (innateElement.isAura() && !targetGauges.containsKey(innateElement)) {
                targetGauges.put(innateElement, new GaugeState(innateElement, 1.25F));
            }
        }

        // 0B. On-Hit Environmental Auras
        if (isEntityInWater(target)) {
            if (targetGauges.remove(Element.PYRO) != null || BURNING_UNTIL.remove(targetId) != null) {
                LAST_BURN_PYRO_REFRESH.remove(targetId);
                target.clearFire();
            }
            if (innateElement == null && !targetGauges.containsKey(Element.HYDRO)) {
                targetGauges.put(Element.HYDRO, new GaugeState(Element.HYDRO, 1.25F));
            }
        } else if (target.isInLava() || target.isOnFire() || target.getRemainingFireTicks() > 0) {
            if (innateElement == null) {
                if (!targetGauges.containsKey(Element.PYRO)) {
                    targetGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.25F));
                } else {
                    GaugeState existing = targetGauges.get(Element.PYRO);
                    if (existing != null) {
                        existing.refresh(1.25F);
                    }
                }
            }
        } else if (target.isInPowderSnow) {
            if (innateElement == null && !targetGauges.containsKey(Element.CRYO)) {
                targetGauges.put(Element.CRYO, new GaugeState(Element.CRYO, 1.25F));
            }
        }

        // 1. Quicken / Catalyze Checks (Independent Quicken Aura coexisting with Dendro/Electro):
        //    - Electro triggers Aggravate (+1.0 DMG) without consuming Quicken, and can STILL react with underlying Dendro (to refresh Quicken) or coexist!
        //    - Dendro triggers Spread (+1.25 DMG) without consuming Quicken, and can STILL react with underlying Electro (to refresh Quicken) or coexist!
        //    - Pyro / Hydro react directly with the Quicken aura (acting as a Dendro gauge).
        GaugeState quickenGauge = QUICKEN_GAUGES.get(targetId);
        Long quickenEnd = QUICKEN_UNTIL.get(targetId);
        boolean hasQuicken = (quickenGauge != null && !quickenGauge.isExpired()) && (quickenEnd == null || now < quickenEnd);
        ElementalReaction catalyzeReaction = null;

        if (hasQuicken) {
            if (attackElement == Element.ELECTRO) {
                if (checkReactionIcd(target, ElementalReaction.AGGRAVATE, now)) {
                    float bonus = (float) (1.0F * dmgMult);
                    effectiveDamage += bonus;
                    catalyzeReaction = ElementalReaction.AGGRAVATE;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 18, 0.35D, 0.4D, 0.35D, 0.08D);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
                }
                // If no underlying Dendro aura is present to re-trigger Quicken, coexist as underlying Electro aura and deal the Aggravate hit
                if (!targetGauges.containsKey(Element.DENDRO) && !targetGauges.containsKey(Element.HYDRO) && !targetGauges.containsKey(Element.PYRO) && !targetGauges.containsKey(Element.CRYO)) {
                    if (innateElement == null) {
                        if (targetGauges.containsKey(Element.ELECTRO)) {
                            targetGauges.get(Element.ELECTRO).refresh(attackU);
                        } else {
                            targetGauges.put(Element.ELECTRO, new GaugeState(Element.ELECTRO, attackU));
                        }
                    }
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.ELECTRO, catalyzeReaction, effectiveDamage);
                    return;
                }
            } else if (attackElement == Element.DENDRO) {
                if (checkReactionIcd(target, ElementalReaction.SPREAD, now)) {
                    float bonus = (float) (1.25F * dmgMult);
                    effectiveDamage += bonus;
                    catalyzeReaction = ElementalReaction.SPREAD;
                    level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.35D, 0.4D, 0.35D, 0.05D);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 0.9F, 1.4F);
                }
                // If no underlying Electro aura is present to re-trigger Quicken, coexist as underlying Dendro aura and deal the Spread hit
                if (!targetGauges.containsKey(Element.ELECTRO) && !targetGauges.containsKey(Element.HYDRO) && !targetGauges.containsKey(Element.PYRO)) {
                    if (innateElement == null) {
                        if (targetGauges.containsKey(Element.DENDRO)) {
                            targetGauges.get(Element.DENDRO).refresh(attackU);
                        } else {
                            targetGauges.put(Element.DENDRO, new GaugeState(Element.DENDRO, attackU));
                        }
                    }
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.DENDRO, catalyzeReaction, effectiveDamage);
                    return;
                }
            } else if (attackElement == Element.PYRO || attackElement == Element.HYDRO) {
                // Expose Quicken gauge into DENDRO slot if no underlying Dendro gauge exists so Pyro/Hydro can consume it
                if (!targetGauges.containsKey(Element.DENDRO) && quickenGauge != null) {
                    targetGauges.put(Element.DENDRO, quickenGauge);
                }
            }
        }

        // 2. Frozen Checks (Genshin Impact Frozen rules with Underlying Hydro/Cryo Aura preservation):
        //    - Geo triggers Shatter (+10.0 DMG, consumes 8.0U Freeze) and exposes any underlying Hydro/Cryo aura
        //    - Pyro triggers Forward Melt (x2.0 DMG, consumes 2.0x Freeze)
        //    - Electro triggers Superconduct (consumes 1.0x Freeze)
        //    - Anemo swirls the Freeze aura (and any underlying Hydro if gauge remains)
        GaugeState freezeGauge = FROZEN_GAUGES.get(targetId);
        Long frozenEnd = FROZEN_UNTIL.get(targetId);
        boolean isFrozen = (freezeGauge != null && !freezeGauge.isExpired()) && (frozenEnd == null || now < frozenEnd);

        if (isFrozen && freezeGauge != null) {
            if (attackElement == Element.GEO) {
                if (!checkReactionIcd(target, ElementalReaction.SHATTER, now)) {
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    return;
                }
                freezeGauge.consume(8.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                float bonus = (float) (10.0F * dmgMult);
                float shatterDamage = effectiveDamage + bonus;
                target.hurt(level.damageSources().magic(), shatterDamage);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + 0.8D, target.getZ(), 30, 0.35D, 0.4D, 0.35D, 0.06D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                // Note: underlying HYDRO or CRYO in targetGauges is preserved (enabling Shatter-Crystallize or Shatter-Vape)!
                // Shatter-Crystallize: If target has underlying HYDRO or CRYO aura, Geo triggers Crystallize immediately!
                Element underlying = null;
                if (targetGauges.containsKey(Element.CRYO)) underlying = Element.CRYO;
                else if (targetGauges.containsKey(Element.HYDRO)) underlying = Element.HYDRO;
                else if (innateElement == Element.CRYO || innateElement == Element.HYDRO) underlying = innateElement;

                if (underlying != null && checkReactionIcd(target, ElementalReaction.CRYSTALLIZE, now)) {
                    GaugeState underAura = targetGauges.get(underlying);
                    float underBefore = underAura != null ? underAura.getUnits() : 1.25F;
                    float consumed = attackU * ElementalReaction.CRYSTALLIZE.getConsumptionModifier();
                    float removedU = Math.min(underBefore, consumed);
                    if (underAura != null) {
                        underAura.consume(consumed);
                        if (underAura.isExpired()) {
                            targetGauges.remove(underlying);
                        }
                    }
                    executeReactionEffect(level, target, ElementalReaction.CRYSTALLIZE, underlying, removedU, attacker, dmgMult);
                }
                enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, attackElement, ElementalReaction.SHATTER, shatterDamage);
                return;
            } else if (attackElement == Element.PYRO) {
                if (!checkReactionIcd(target, ElementalReaction.FORWARD_MELT, now)) {
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    return;
                }
                freezeGauge.consume(attackU * 2.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                float meltDamage = effectiveDamage * 2.0F;
                target.hurt(level.damageSources().magic(), meltDamage);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 25, 0.3D, 0.35D, 0.3D, 0.04D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, meltDamage);
                return;
            } else if (attackElement == Element.ELECTRO) {
                if (!checkReactionIcd(target, ElementalReaction.SUPERCONDUCT, now)) {
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    return;
                }
                freezeGauge.consume(attackU * 1.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                target.hurt(level.damageSources().magic(), effectiveDamage);
                executeReactionEffect(level, target, ElementalReaction.SUPERCONDUCT, Element.CRYO, attackU, attacker, dmgMult);
                enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ELECTRO, ElementalReaction.SUPERCONDUCT, effectiveDamage);
                return;
            } else if (attackElement == Element.ANEMO) {
                if (checkReactionIcd(target, ElementalReaction.SWIRL, now)) {
                    float freezeBefore = freezeGauge.getUnits();
                    float removedFreezeU = Math.min(freezeBefore, attackU * 0.5F);
                    freezeGauge.consume(attackU * 0.5F);
                    if (freezeGauge.isExpired()) {
                        clearFreezeState(target, targetId);
                    }
                    float remainingAnemoU = Math.max(0.0F, attackU - (freezeBefore / 0.5F));
                    // If underlying HYDRO exists and Anemo gauge remains, swirl underlying Hydro first/too!
                    if (remainingAnemoU > 0.01F && targetGauges.containsKey(Element.HYDRO)) {
                        GaugeState hydro = targetGauges.get(Element.HYDRO);
                        float hydroBefore = hydro.getUnits();
                        float removedHydroU = Math.min(hydroBefore, remainingAnemoU * 0.5F);
                        hydro.consume(remainingAnemoU * 0.5F);
                        if (hydro.isExpired()) targetGauges.remove(Element.HYDRO);
                        executeReactionEffect(level, target, ElementalReaction.SWIRL, Element.HYDRO, removedHydroU, attacker, dmgMult);
                    }
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    executeReactionEffect(level, target, ElementalReaction.SWIRL, Element.CRYO, removedFreezeU, attacker, dmgMult);
                    enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                    syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ANEMO, ElementalReaction.SWIRL, effectiveDamage);
                    return;
                }
            }
        }

        // 3. Check Overvape (Target has both Hydro & Electro, attacked with Pyro)
        if (attackElement == Element.PYRO && targetGauges.containsKey(Element.HYDRO) && targetGauges.containsKey(Element.ELECTRO)) {
            if (!checkReactionIcd(target, ElementalReaction.OVERVAPE, now)) {
                target.hurt(level.damageSources().magic(), effectiveDamage);
                return;
            }
            GaugeState electro = targetGauges.get(Element.ELECTRO);
            GaugeState hydro = targetGauges.get(Element.HYDRO);
            float electroBefore = electro.getUnits();
            electro.consume(attackU * 1.0F);
            if (electro.isExpired()) targetGauges.remove(Element.ELECTRO);

            float remainingPyroU = Math.max(0.0F, attackU - electroBefore);
            float vapeConsumed = Math.max(0.25F, remainingPyroU > 0.0F ? remainingPyroU * 0.5F : attackU * 0.5F);
            hydro.consume(vapeConsumed);
            if (hydro.isExpired()) targetGauges.remove(Element.HYDRO);

            float overvapeDmg = effectiveDamage * 1.5F;
            target.hurt(level.damageSources().magic(), overvapeDmg);
            triggerOverloadExplosion(level, target, attacker, dmgMult);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
            enforceInnateLock(target, targetGauges, targetId, innateElement, now);
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, attackElement, ElementalReaction.OVERVAPE, overvapeDmg);
            return;
        }

        // 4. Reaction Resolution with Simultaneous Reaction Priority (Electro -> Pyro -> Hydro -> Cryo -> Dendro)
        Element auraElement = findPrimaryReactiveAura(targetGauges, attackElement);
        if (auraElement != null && auraElement != attackElement) {
            ElementalReaction reaction = determineReaction(auraElement, attackElement);
            if (reaction != ElementalReaction.NONE) {
                if (!checkReactionIcd(target, reaction, now)) {
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    return;
                }
                GaugeState aura = targetGauges.get(auraElement);

                // Electro-Charged Coexistence: both HYDRO and ELECTRO coexist on normal targets;
                // Trigger aura is created with standard 0.8x Aura Tax, then initial EC tick consumes 0.4U from both.
                if (reaction == ElementalReaction.ELECTRO_CHARGED) {
                    if (innateElement == null) {
                        GaugeState triggerState = targetGauges.get(attackElement);
                        if (triggerState == null) {
                            triggerState = new GaugeState(attackElement, attackU);
                            targetGauges.put(attackElement, triggerState);
                        } else {
                            triggerState.refresh(attackU);
                        }
                        aura.consume(0.4F);
                        triggerState.consume(0.4F);
                        if (aura.isExpired()) targetGauges.remove(auraElement);
                        if (triggerState.isExpired()) targetGauges.remove(attackElement);
                    } else {
                        aura.consume(0.4F);
                        if (aura.isExpired()) targetGauges.remove(auraElement);
                        enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                    }

                    float ecDmg = effectiveDamage + (float) (1.0F * dmgMult);
                    target.hurt(level.damageSources().magic(), ecDmg);
                    executeReactionEffect(level, target, reaction, auraElement, 0.4F, attacker, dmgMult);
                    ELECTRO_CHARGE_TICK.put(targetId, 0);
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, ecDmg);
                    return;
                }

                // Burning Coexistence: maintains 2.0U Burning Pyro state and consumes Dendro/Quicken at 0.4U/s until depleted
                if (reaction == ElementalReaction.BURNING) {
                    BURNING_UNTIL.put(targetId, now + 200L);
                    if (innateElement == null) {
                        targetGauges.put(Element.PYRO, GaugeState.createBurning(2.0F));
                        if (attackElement == Element.DENDRO) {
                            if (targetGauges.containsKey(Element.DENDRO)) {
                                targetGauges.get(Element.DENDRO).refresh(attackU);
                            } else {
                                targetGauges.put(Element.DENDRO, new GaugeState(Element.DENDRO, attackU));
                            }
                        }
                    } else {
                        enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                    }
                    LAST_BURN_PYRO_REFRESH.put(targetId, now);
                    target.setSecondsOnFire(5);
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                    executeReactionEffect(level, target, reaction, auraElement, 0.4F, attacker, dmgMult);
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, effectiveDamage);
                    return;
                }

                float auraUnitsBefore = aura.getUnits();
                float consumed = (reaction == ElementalReaction.QUICKEN)
                        ? Math.min(auraUnitsBefore, attackU * 0.8F)
                        : attackU * reaction.getConsumptionModifier();
                float removedAuraU = Math.min(auraUnitsBefore, consumed);
                aura.consume(consumed);
                boolean auraDepleted = aura.isExpired();
                if (auraDepleted) {
                    targetGauges.remove(auraElement);
                    if (auraElement == Element.PYRO) {
                        BURNING_UNTIL.remove(targetId);
                        LAST_BURN_PYRO_REFRESH.remove(targetId);
                    }
                }

                // Also consume from independent Quicken gauge if Hydro triggered Bloom against Quicken/Dendro
                if (auraElement == Element.DENDRO && (reaction == ElementalReaction.BLOOM || reaction == ElementalReaction.REVERSE_BLOOM)) {
                    GaugeState qg = QUICKEN_GAUGES.get(targetId);
                    if (qg != null) {
                        qg.consume(consumed);
                        if (qg.isExpired()) {
                            clearQuickenState(targetId);
                        }
                    }
                }

                // When Frozen is triggered:
                // - Preserve any unconsumed underlying HYDRO or CRYO aura in targetGauges!
                // - Compute U_Freeze = 2 * min(U_aura, U_trigger) and dynamic duration T_Freeze = 2 * sqrt(5 * U_Freeze + 4) - 4s
                // - Genshin Boss Freeze Immunity: Bosses (Warden, Ender Dragon, Wither, Elder Guardian) cannot be immobilized
                if (reaction == ElementalReaction.FROZEN) {
                    float freezeU = 2.0F * Math.min(auraUnitsBefore, attackU);
                    GaugeState newFreeze = GaugeState.createFrozen(Math.max(0.8F, freezeU));
                    int freezeTicks = newFreeze.getRemainingTicks();
                    if (innateElement == null && !isBossEntity(target)) {
                        FROZEN_GAUGES.put(targetId, newFreeze);
                        FROZEN_UNTIL.put(targetId, now + freezeTicks);
                        target.setTicksFrozen(freezeTicks + 40);
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeTicks, 6));
                    }
                }

                // When Quicken is triggered:
                // - U_Quicken = min(U_aura, U_trigger * 0.8), Duration = 5 * U_Quicken + 6s
                // - Store in independent QUICKEN_GAUGES so leftover Dendro/Electro can coexist in targetGauges
                if (reaction == ElementalReaction.QUICKEN) {
                    float postTaxAttackU = attackU * 0.8F;
                    float quickenU = Math.max(0.4F, Math.min(auraUnitsBefore, postTaxAttackU));
                    GaugeState newQuicken = GaugeState.createQuicken(quickenU);
                    GaugeState existingQuicken = QUICKEN_GAUGES.get(targetId);
                    if (existingQuicken == null || newQuicken.getRemainingTicks() > existingQuicken.getRemainingTicks()) {
                        QUICKEN_GAUGES.put(targetId, newQuicken);
                        QUICKEN_UNTIL.put(targetId, now + newQuicken.getRemainingTicks());
                    }

                    // Leftover trigger gauge if trigger attack was stronger than reacted aura
                    float leftoverTriggerU = postTaxAttackU - auraUnitsBefore;
                    if (leftoverTriggerU > 0.05F && innateElement == null) {
                        if (targetGauges.containsKey(attackElement)) {
                            targetGauges.get(attackElement).refresh(leftoverTriggerU);
                        } else {
                            targetGauges.put(attackElement, GaugeState.createLeftover(attackElement, leftoverTriggerU, attackU));
                        }
                    }
                }

                // Simultaneous Reaction Priority: if the first aura was completely consumed and trigger gauge remains,
                // immediately resolve a secondary reaction against any remaining reactive aura (e.g. Cryo/Anemo/Dendro on Electro + Hydro)!
                float neededTriggerU = reaction.getConsumptionModifier() > 0.0F
                        ? (auraUnitsBefore / reaction.getConsumptionModifier())
                        : attackU;
                float remainingAttackU = Math.max(0.0F, attackU - neededTriggerU);
                if (auraDepleted && remainingAttackU > 0.01F) {
                    Element secondaryAura = findPrimaryReactiveAura(targetGauges, attackElement);
                    if (secondaryAura != null && secondaryAura != auraElement) {
                        ElementalReaction secondaryReaction = determineReaction(secondaryAura, attackElement);
                        if (secondaryReaction != ElementalReaction.NONE) {
                            GaugeState aura2 = targetGauges.get(secondaryAura);
                            if (aura2 != null) {
                                float aura2Before = aura2.getUnits();
                                float consumed2 = remainingAttackU * secondaryReaction.getConsumptionModifier();
                                float removed2 = Math.min(aura2Before, consumed2);
                                aura2.consume(consumed2);
                                if (aura2.isExpired()) {
                                    targetGauges.remove(secondaryAura);
                                    if (secondaryAura == Element.PYRO) {
                                        BURNING_UNTIL.remove(targetId);
                                        LAST_BURN_PYRO_REFRESH.remove(targetId);
                                    }
                                }
                                if (secondaryReaction == ElementalReaction.FROZEN) {
                                    float freezeU2 = 2.0F * Math.min(aura2Before, remainingAttackU);
                                    GaugeState newFreeze2 = GaugeState.createFrozen(Math.max(0.8F, freezeU2));
                                    int freezeTicks2 = newFreeze2.getRemainingTicks();
                                    if (innateElement == null && !isBossEntity(target)) {
                                        FROZEN_GAUGES.put(targetId, newFreeze2);
                                        FROZEN_UNTIL.put(targetId, now + freezeTicks2);
                                        target.setTicksFrozen(freezeTicks2 + 40);
                                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeTicks2, 6));
                                    }
                                }
                                executeReactionEffect(level, target, secondaryReaction, secondaryAura, removed2, attacker, dmgMult);
                            }
                        }
                    }
                }

                if (reaction == ElementalReaction.REVERSE_VAPORIZE && isEntityInWater(target)) {
                    target.clearFire();
                }

                enforceInnateLock(target, targetGauges, targetId, innateElement, now);

                float finalDmg = effectiveDamage * reaction.getDamageMultiplier();
                if (finalDmg > 0.0F) {
                    target.hurt(level.damageSources().magic(), finalDmg);
                }
                executeReactionEffect(level, target, reaction, auraElement, removedAuraU, attacker, dmgMult);

                ElementalReaction displayReaction = (catalyzeReaction != null && reaction == ElementalReaction.QUICKEN)
                        ? catalyzeReaction : reaction;
                syncAuras(target, targetGauges.keySet(), auraElement, attackElement, displayReaction, finalDmg);
                return;
            }
        }

        // 5. No Reaction: Apply or Refresh Aura (Aura elements only!)
        if (attackElement.isAura()) {
            if (attackElement == Element.PYRO && isEntityInWater(target)) {
                target.clearFire();
                float vapeDmg = effectiveDamage * 1.5F;
                target.hurt(level.damageSources().magic(), vapeDmg);
                syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE, vapeDmg);
                return;
            }

            if (innateElement != null && attackElement != innateElement) {
                if (effectiveDamage > 0.0F) {
                    target.hurt(level.damageSources().magic(), effectiveDamage);
                }
                syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, effectiveDamage);
                return;
            }

            if (targetGauges.containsKey(attackElement)) {
                GaugeState existing = targetGauges.get(attackElement);
                existing.refresh(attackU);
            } else {
                GaugeState newAura = new GaugeState(attackElement, attackU);
                targetGauges.put(attackElement, newAura);
            }
            syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, effectiveDamage);
            if (effectiveDamage > 0.0F) {
                target.hurt(level.damageSources().magic(), effectiveDamage);
            }
            return;
        }

        // Deal normal damage
        if (effectiveDamage > 0.0F) {
            target.hurt(level.damageSources().magic(), effectiveDamage);
        }
        syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, effectiveDamage);
    }

    private static void enforceInnateLock(
            @Nullable LivingEntity target,
            Map<Element, GaugeState> targetGauges,
            UUID targetId,
            @Nullable Element innateElement,
            long now
    ) {
        if (innateElement == null) return;
        boolean isBurning = target != null && (target.isOnFire() || target.getRemainingFireTicks() > 0 || target.isInLava());
        // Allow DENDRO to coexist on GEO innate creatures, and allow PYRO when actively burning on fire/lava
        targetGauges.keySet().removeIf(e -> e != innateElement
                && !(innateElement == Element.GEO && e == Element.DENDRO)
                && !(isBurning && e == Element.PYRO));
        if (innateElement.isAura() && !targetGauges.containsKey(innateElement)) {
            targetGauges.put(innateElement, new GaugeState(innateElement, 1.25F));
        }
    }

    /**
     * Selects the primary aura to react with the incoming trigger element according to
     * Genshin Impact's Simultaneous Reaction Priority (Electro -> Pyro -> Hydro -> Cryo -> Dendro).
     */
    private static @Nullable Element findPrimaryReactiveAura(Map<Element, GaugeState> gauges, Element trigger) {
        for (Element priorityElem : REACTION_PRIORITY_ORDER) {
            if (priorityElem == trigger) continue;
            GaugeState gs = gauges.get(priorityElem);
            if (gs != null && !gs.isExpired() && determineReaction(priorityElem, trigger) != ElementalReaction.NONE) {
                return priorityElem;
            }
        }
        return null;
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
            float removedAuraU,
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
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.7F);
            }
            case SUPERCONDUCT -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 25, 1.2D, 0.5D, 1.2D, 0.03D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 15, 0.8D, 0.5D, 0.8D, 0.06D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.9F, 1.2F);
                long debuffEnd = level.getGameTime() + 240L; // 12 seconds Physical RES shred (-40% Physical RES)
                SUPERCONDUCT_UNTIL.put(target.getUUID(), debuffEnd);
                AABB box = target.getBoundingBox().inflate(3.5D);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
                    float dmg = (float) (1.0F * dmgMult); // 0.5 heart Cryo AoE damage
                    if (nearby instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, level.getGameTime()) == Element.CRYO) {
                        dmg *= 0.4F;
                    }
                    nearby.invulnerableTime = 0;
                    nearby.hurt(level.damageSources().freeze(), dmg);
                    SUPERCONDUCT_UNTIL.put(nearby.getUUID(), debuffEnd);
                    nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0));
                }
            }
            case FROZEN -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.8D, pos.z, 25, 0.4D, 0.6D, 0.4D, 0.02D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.9F, 1.4F);
                if (isEntitySubmergedInWater(target)) {
                    com.velorise.staves.event.ArcaneStaffEvents.freezeWaterNear(level, target.position(), 1);
                }
            }
            case QUICKEN -> {
                level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y + 0.8D, pos.z, 20, 0.35D, 0.5D, 0.35D, 0.04D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
            case BLOOM, REVERSE_BLOOM -> {
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
                BURNING_UNTIL.put(target.getUUID(), level.getGameTime() + 200L);
                target.setSecondsOnFire(5);
                level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.8D, pos.z, 15, 0.3D, 0.4D, 0.3D, 0.03D);
            }
            case SWIRL -> {
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.1F);
                AABB box = target.getBoundingBox().inflate(5.0D);
                // Genshin Impact rule: Pyro, Electro, Cryo Swirls deal AoE damage + spread element; Hydro Swirl only spreads Wet aura!
                // Spread gauge formula: U_Swirl = (U_removed - 0.04) * 1.25 + 1.5
                float swirlSpreadU = Math.max(0.5F, (Math.max(0.04F, removedAuraU) - 0.04F) * 1.25F + 1.5F);
                float swirlAoeDmg = (auraElement == Element.HYDRO) ? 0.0F : (float) (4.0F * dmgMult);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != target && e != attacker)) {
                    applyElement(level, nearby, auraElement, swirlSpreadU, attacker, swirlAoeDmg);
                }
            }
            case CRYSTALLIZE -> {
                double popY = target.getY() + target.getBbHeight() * 0.80D;
                level.sendParticles(ParticleTypes.CRIT, target.getX(), popY, target.getZ(), 15, 0.35D, 0.35D, 0.35D, 0.05D);
                level.playSound(null, target.getX(), popY, target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.4F);
                CrystallizeShardManager.spawn(level, target, auraElement, target.getUUID());
            }
            default -> {}
        }
    }

    private static void triggerOverloadExplosion(ServerLevel level, LivingEntity target, @Nullable ServerPlayer attacker, double dmgMult) {
        Vec3 pos = target.position();
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5D, pos.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 20, 0.6D, 0.6D, 0.6D, 0.06D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.1F);
        EnvironmentalBlockManager.checkPyroAmethystMining(level, pos, 3.5D);

        AABB box = target.getBoundingBox().inflate(3.5D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
            float dmg = (float) (6.0F * dmgMult);
            if (victim instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, level.getGameTime()) == Element.PYRO) {
                dmg *= 0.4F;
            }
            victim.invulnerableTime = 0;
            victim.hurt(level.damageSources().magic(), dmg);
            Vec3 diff = victim == target && attacker != null
                ? target.position().subtract(attacker.position())
                : victim.position().subtract(pos);
            Vec3 knock = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(0, 1, 0);
            victim.setDeltaMovement(victim.getDeltaMovement().add(knock.x * 0.85D, 0.45D, knock.z * 0.85D));
            victim.hurtMarked = true;
        }
    }

    public static void drawVoltaicArc(ServerLevel level, Vec3 start, Vec3 end) {
        ElectrifiedWaterManager.drawElectricArc(level, start, end, 0.0D, false);
    }

    public static void drawVoltaicArc(ServerLevel level, Entity from, Entity to) {
        ElectrifiedWaterManager.drawElectricArc(level, from, to, 8);
    }

    /**
     * Server tick loop for decay, environmental water/rain extinguish, and periodic reactions.
     */
    public static void tick(MinecraftServer server) {
        for (ServerLevel lvl : server.getAllLevels()) {
            DendroCoreManager.tick(lvl);
            CrystallizeShardManager.tick(lvl);
            ElectrifiedWaterManager.tick(lvl);
            DragonfireCloudManager.tick(lvl);
            GhastfireFieldManager.tick(lvl);
        }
        EnvironmentalBlockManager.tick(server);

        long serverNow = server.overworld().getGameTime();
        if (serverNow % 100 == 0) {
            ICD_TRACKER.entrySet().removeIf(e -> (serverNow - e.getValue().firstHitTime) > 100L);
            REACTION_ICD_TRACKER.entrySet().removeIf(e -> (serverNow - e.getValue()) > 100L);
            SUPERCONDUCT_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue());
            INNATE_COOLDOWN_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue());
            LAST_PROXIMITY_SHOCK.entrySet().removeIf(e -> (serverNow - e.getValue()) > 40L);
            LAST_BLOOM_SPAWN.entrySet().removeIf(e -> (serverNow - e.getValue()) > 40L);
        }

        Set<UUID> activeEntityIds = new HashSet<>(GAUGES.keySet());
        activeEntityIds.addAll(QUICKEN_GAUGES.keySet());
        activeEntityIds.addAll(FROZEN_GAUGES.keySet());

        for (UUID id : activeEntityIds) {
            LivingEntity target = null;
            ServerLevel targetLevel = null;

            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(id);
            if (onlinePlayer != null && onlinePlayer.isAlive() && !onlinePlayer.isRemoved()) {
                target = onlinePlayer;
                targetLevel = (ServerLevel) onlinePlayer.level();
            } else {
                for (ServerLevel lvl : server.getAllLevels()) {
                    Entity e = lvl.getEntity(id);
                    if (e instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                        target = living;
                        targetLevel = lvl;
                        break;
                    }
                }
            }

            if (target == null || targetLevel == null) {
                GAUGES.remove(id);
                FROZEN_GAUGES.remove(id);
                QUICKEN_GAUGES.remove(id);
                FROZEN_UNTIL.remove(id);
                QUICKEN_UNTIL.remove(id);
                BURNING_UNTIL.remove(id);
                LAST_BURN_PYRO_REFRESH.remove(id);
                INNATE_COOLDOWN_UNTIL.remove(id);
                ELECTRO_CHARGE_TICK.remove(id);
                LAST_BLOOM_SPAWN.remove(id);
                SUPERCONDUCT_UNTIL.remove(id);
                continue;
            }

            long now = targetLevel.getGameTime();
            Map<Element, GaugeState> entityGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            Element innate = getInnateElement(target);
            boolean changed = false;

            // 1. Environmental Water / Fire check & Cryo Water Freezing
            if (innate == Element.CRYO || entityGauges.containsKey(Element.CRYO)) {
                checkCryoWaterFreezing(target);
            }
            if (isEntityInWater(target)) {
                handleEnvironmentalWater(target);
                if (entityGauges.isEmpty() && !QUICKEN_GAUGES.containsKey(id) && !FROZEN_GAUGES.containsKey(id)) {
                    GAUGES.remove(id);
                    continue;
                }
            } else if (target.isOnFire() || target.getRemainingFireTicks() > 0 || target.isInLava()) {
                maintainBurningPyroAura(target);
            }

            // Electro Innate Mutant & Powered Creeper: Radiates electric shock within 1.0 block once per second (20 ticks)
            if ((target instanceof Creeper creeper && creeper.isPowered())
                    || (innate == Element.ELECTRO && isMutatedInnate(target))) {
                if (now % 20 == 0) {
                    final LivingEntity electroSource = target;
                    AABB auraField = electroSource.getBoundingBox().inflate(1.0D);
                    for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, auraField,
                            e -> e.isAlive() && !e.isRemoved() && e != electroSource && !e.isSpectator()
                                    && !(e instanceof net.minecraft.world.entity.player.Player p && p.isCreative())
                                    && getInnateElement(e) != Element.ELECTRO)) {
                        drawVoltaicArc(targetLevel, electroSource, nearby);
                        applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 1.0F);
                    }

                    if (isEntitySubmergedInWater(electroSource)) {
                        ElectrifiedWaterManager.electrify(targetLevel, electroSource.blockPosition(), 1.0D, 30, null, 1.0F);
                    }
                }
            }

            // 2. Tick gauges decay
            Iterator<Map.Entry<Element, GaugeState>> gaugeIt = entityGauges.entrySet().iterator();
            while (gaugeIt.hasNext()) {
                Map.Entry<Element, GaugeState> gEntry = gaugeIt.next();
                if (innate != null && gEntry.getKey() == innate) {
                    continue; // Innate lock: never decay naturally
                }
                if (gEntry.getKey() == Element.PYRO && (target.isInLava() || target.isOnFire() || target.getRemainingFireTicks() > 0 || BURNING_UNTIL.containsKey(id))) {
                    continue;
                }
                if (gEntry.getKey() == Element.HYDRO && isEntityInWater(target)) {
                    continue;
                }
                GaugeState state = gEntry.getValue();
                state.tick();
                if (state.isExpired()) {
                    gaugeIt.remove();
                    changed = true;
                }
            }

            // Tick independent Quicken gauge
            GaugeState qg = QUICKEN_GAUGES.get(id);
            Long quickenEnd = QUICKEN_UNTIL.get(id);
            if (qg != null) {
                qg.tick();
                if (qg.isExpired() || (quickenEnd != null && now >= quickenEnd)) {
                    clearQuickenState(id);
                    changed = true;
                }
            } else if (quickenEnd != null && now >= quickenEnd) {
                clearQuickenState(id);
                changed = true;
            }

            // Tick independent Freeze gauge
            GaugeState fg = FROZEN_GAUGES.get(id);
            Long frozenEnd = FROZEN_UNTIL.get(id);
            if (isBossEntity(target)) {
                if (fg != null || frozenEnd != null) {
                    clearFreezeState(target, id);
                    changed = true;
                }
            } else if (fg != null) {
                fg.tick();
                if (fg.isExpired() || (frozenEnd != null && now >= frozenEnd)) {
                    clearFreezeState(target, id);
                    changed = true;
                } else {
                    target.setDeltaMovement(0.0D, target.getDeltaMovement().y < 0 ? -0.05D : 0.0D, 0.0D);
                }
            } else if (frozenEnd != null) {
                if (now >= frozenEnd) {
                    clearFreezeState(target, id);
                    changed = true;
                } else {
                    target.setDeltaMovement(0.0D, target.getDeltaMovement().y < 0 ? -0.05D : 0.0D, 0.0D);
                }
            }

            // 2B. Proximity Voltaic Arcing (ONLY when BOTH entities are physically submerged in water)
            if (entityGauges.containsKey(Element.ELECTRO) && isEntitySubmergedInWater(target) && (now % 4 == 0)) {
                final LivingEntity srcTarget = target;
                AABB arcBox = target.getBoundingBox().inflate(4.0D);
                for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, arcBox, e -> e.isAlive() && !e.isRemoved() && e != srcTarget)) {
                    if (isEntitySubmergedInWater(nearby)) {
                        long pairKey = (((long) Math.min(target.getId(), nearby.getId())) << 32) | ((long) Math.max(target.getId(), nearby.getId()) & 0xFFFFFFFFL);
                        Long lastShock = LAST_PROXIMITY_SHOCK.get(pairKey);
                        if (lastShock == null || now - lastShock >= 20) {
                            LAST_PROXIMITY_SHOCK.put(pairKey, now);
                            drawVoltaicArc(targetLevel, target, nearby);
                            targetLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
                            applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 1.0F);
                            if (innate != Element.ELECTRO) {
                                target.hurt(targetLevel.damageSources().magic(), 1.0F);
                                syncAuras(target, entityGauges.keySet(), Element.ELECTRO, Element.HYDRO, ElementalReaction.ELECTRO_CHARGED, 1.0F);
                            }
                            break;
                        }
                    }
                }
            }

            // 3. Electro-Charged periodic damage (every 20 ticks = 1 second, 0.5 heart = 1.0F damage, consuming 0.4U from both Hydro and Electro)
            if (entityGauges.containsKey(Element.HYDRO) && entityGauges.containsKey(Element.ELECTRO)) {
                int counter = ELECTRO_CHARGE_TICK.getOrDefault(id, 0) + 1;
                GaugeState hydroState = entityGauges.get(Element.HYDRO);
                GaugeState electroState = entityGauges.get(Element.ELECTRO);
                // Genshin EC premature final tick: if either gauge is about to expire via decay and >= 0.5s (10 ticks) has passed since last tick
                boolean prematureFinalTick = counter >= 10 && (
                        (hydroState != null && hydroState.getRemainingTicks() <= 1) ||
                        (electroState != null && electroState.getRemainingTicks() <= 1)
                );
                if (counter >= 20 || prematureFinalTick) {
                    counter = 0;
                    float ecDmg = 1.0F;
                    if (target instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, targetLevel.getGameTime()) == Element.ELECTRO) {
                        ecDmg *= 0.4F;
                    }
                    if (innate != Element.ELECTRO) {
                        target.hurt(targetLevel.damageSources().magic(), ecDmg);
                        syncAuras(target, entityGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, ecDmg);
                    }

                    if (isEntitySubmergedInWater(target)) {
                        final LivingEntity srcTarget = target;
                        AABB chainBox = target.getBoundingBox().inflate(4.0D);
                        for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, chainBox, e -> e.isAlive() && e != srcTarget)) {
                            if (isEntitySubmergedInWater(nearby)) {
                                drawVoltaicArc(targetLevel, target, nearby);
                                applyElement(targetLevel, nearby, Element.ELECTRO, 0.5F, null, 1.0F);
                            }
                        }
                    }

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

            // 4. Burning periodic damage (Genshin standard: ticks every 0.25s = 5 ticks, consumes 0.4U/s Dendro/Quicken = 0.1U per 5 ticks,
            //    and continues until all Dendro & Quicken gauges are completely depleted!)
            if (BURNING_UNTIL.containsKey(id)) {
                boolean hasDendroFuel = entityGauges.containsKey(Element.DENDRO) || QUICKEN_GAUGES.containsKey(id) || innate == Element.DENDRO;
                boolean hasPyro = entityGauges.containsKey(Element.PYRO) || innate == Element.PYRO;
                if (!hasDendroFuel || !hasPyro || target.isInWaterOrRain() || target.fireImmune() || target instanceof Warden) {
                    BURNING_UNTIL.remove(id);
                    LAST_BURN_PYRO_REFRESH.remove(id);
                    changed = true;
                } else if (now % 5 == 0) {
                    float burnDmg = 0.75F; // 0.75 per 0.25s = 3.0 DMG/s
                    if (target instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, now) == Element.PYRO) {
                        burnDmg *= 0.4F;
                    }
                    IS_PROCESSING_REACTION.set(true);
                    try {
                        target.hurt(targetLevel.damageSources().inFire(), burnDmg);
                    } finally {
                        IS_PROCESSING_REACTION.set(false);
                    }
                    target.setSecondsOnFire(2);

                    // Consume 0.1U per 5 ticks (= 0.4U/s) from Dendro and Quicken gauges
                    GaugeState dendroState = entityGauges.get(Element.DENDRO);
                    if (dendroState != null && innate != Element.DENDRO) {
                        dendroState.consume(0.1F);
                        if (dendroState.isExpired()) {
                            entityGauges.remove(Element.DENDRO);
                            changed = true;
                        }
                    }
                    GaugeState quickenState = QUICKEN_GAUGES.get(id);
                    if (quickenState != null) {
                        quickenState.consume(0.1F);
                        if (quickenState.isExpired()) {
                            clearQuickenState(id);
                            changed = true;
                        }
                    }

                    // Re-apply 1U Pyro aura every 2.0s (40 ticks) per Genshin Burning self-refresh ICD
                    Long lastPyroRefresh = LAST_BURN_PYRO_REFRESH.get(id);
                    if (innate == null && (lastPyroRefresh == null || now - lastPyroRefresh >= 40L)) {
                        LAST_BURN_PYRO_REFRESH.put(id, now);
                        if (entityGauges.containsKey(Element.PYRO)) {
                            entityGauges.get(Element.PYRO).refresh(1.0F);
                        } else {
                            entityGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.0F));
                        }
                    }

                    if (!entityGauges.containsKey(Element.DENDRO) && !QUICKEN_GAUGES.containsKey(id) && innate != Element.DENDRO) {
                        BURNING_UNTIL.remove(id);
                        LAST_BURN_PYRO_REFRESH.remove(id);
                        changed = true;
                    }
                    targetLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 4, 0.25D, 0.3D, 0.25D, 0.02D);
                }
            }

            // 5. Cryo Aura passive debuff (-15% Movement Speed, -15% Attack Speed)
            boolean hasCryoOrFreeze = entityGauges.containsKey(Element.CRYO) || FROZEN_GAUGES.containsKey(id);
            updateCryoDebuff(target, id, hasCryoOrFreeze, innate);

            if (entityGauges.isEmpty() && !QUICKEN_GAUGES.containsKey(id) && !FROZEN_GAUGES.containsKey(id)) {
                updateCryoDebuff(target, id, false, innate);
                GAUGES.remove(id);
            }

            if (changed) {
                syncAuras(target, entityGauges.keySet(), null, null, null);
            }

            // 6. Ambient aura particles (every 10 ticks)
            if ((!entityGauges.isEmpty() || QUICKEN_GAUGES.containsKey(id) || FROZEN_GAUGES.containsKey(id)) && now % 10 == 0 && StavesConfig.SHOW_ELEMENTAL_PARTICLES.get()) {
                spawnAmbientParticles(targetLevel, target, entityGauges, now);
            }
        }

        // 7. Environmental water, fire, poison & cryo checks on active online players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isAlive() && !player.isSpectator()) {
                if (isEntityInWater(player)) {
                    handleEnvironmentalWater(player);
                } else if (player.isOnFire() || player.getRemainingFireTicks() > 0 || player.isInLava()) {
                    maintainBurningPyroAura(player);
                }
                Collection<Element> playerElements = getEntityElements(player.getUUID());
                updateCryoDebuff(player, player.getUUID(), playerElements.contains(Element.CRYO), null);

                if (player.level() instanceof ServerLevel sl) {
                    AABB searchBox = player.getBoundingBox().inflate(32.0D);
                    for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !e.isRemoved())) {
                        Element nearbyInnate = getInnateElement(nearby);
                        if (nearbyInnate == Element.CRYO || hasElement(nearby, Element.CRYO)) {
                            checkCryoWaterFreezing(nearby);
                        }
                        if (nearbyInnate != null && player.tickCount % 20 == 0) {
                            if (isEntityInWater(nearby)) {
                                handleEnvironmentalWater(nearby);
                            } else if (nearby.isOnFire() || nearby.getRemainingFireTicks() > 0 || nearby.isInLava()) {
                                maintainBurningPyroAura(nearby);
                            }
                        }
                        if (((nearby instanceof Creeper creeper && creeper.isPowered())
                                || (nearbyInnate == Element.ELECTRO && isMutatedInnate(nearby)))
                                && !GAUGES.containsKey(nearby.getUUID())) {
                            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(nearby.getUUID(), k -> new ConcurrentHashMap<>());
                            gauges.put(Element.ELECTRO, new GaugeState(Element.ELECTRO, 1.25F));
                            syncAuras(nearby, gauges.keySet(), null, null, null);
                        }
                    }
                }
            }
        }
    }

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
            if (slow != null && slow.getAmplifier() == 0 && !FROZEN_GAUGES.containsKey(id)) {
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
        if (gauges.containsKey(Element.CRYO) || FROZEN_GAUGES.containsKey(target.getUUID())) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 2, 0.25D, 0.25D, 0.25D, 0.01D);
        }
        if (gauges.containsKey(Element.ELECTRO)) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 2, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        if (gauges.containsKey(Element.DENDRO) || QUICKEN_GAUGES.containsKey(target.getUUID())) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 2, 0.2D, 0.25D, 0.2D, 0.01D);
        }
    }
}
