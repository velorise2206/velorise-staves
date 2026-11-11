package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.entity.monster.Phantom;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
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
    public static final TagKey<Block> MUSHROOM_BLOCKS = TagKey.create(Registries.BLOCK, new ResourceLocation(VeloriseStavesMod.MODID, "mushroom_blocks"));

    private static final Map<UUID, Map<Element, GaugeState>> GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, GaugeState> FROZEN_GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, GaugeState> QUICKEN_GAUGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FROZEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> QUICKEN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BURNING_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BURNING_EM_LEVEL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ELECTRO_CHARGE_EM_LEVEL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BURN_PYRO_REFRESH = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> INNATE_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ELECTRO_CHARGE_TICK = new ConcurrentHashMap<>();
    private static final Map<Long, Long> LAST_PROXIMITY_SHOCK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BLOOM_SPAWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SUPERCONDUCT_UNTIL = new ConcurrentHashMap<>();
    public static final String TAG_FROZEN_STATUE = "velorise_staves:frozen_statue";
    public static final String TAG_INNATE_OMNI = "velorise_staves:innate_omni";
    private static final Set<UUID> OMNI_INNATE_ENTITIES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, Long> LAST_OMNI_ENV_IMMUNE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MUSHROOM_CONTACT = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> PLAYER_LAST_HP = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PLAYER_HP_CHANGE_BUFF_UNTIL = new ConcurrentHashMap<>();
    private static final long HP_CHANGE_BUFF_TICKS = 200L;

    public static record FrozenPoseState(double x, double y, double z, float yRot, float xRot, float yHeadRot, float yBodyRot) {}

    private static final Map<UUID, FrozenPoseState> FROZEN_POSES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_FREEZE_STRUGGLE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> IS_PROCESSING_DAMAGE = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> IS_PROCESSING_REACTION = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_PROCESSING_THORNS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_PROCESSING_SWORD_IMBUE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ELEMENTAL_RES_APPLIED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Integer> REACTION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_REACTION_DEPTH = 4;
    private static final ThreadLocal<Element> THORNS_CONTEXT_ELEMENT = new ThreadLocal<>();
    private static final ThreadLocal<@Nullable LivingEntity> CURRENT_REACTION_ATTACKER = new ThreadLocal<>();
    private static final Map<Long, Long> LAST_THORNS_REFLECT = new ConcurrentHashMap<>();

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

    public static boolean isEntityFrozen(@Nullable Entity entity) {
        if (entity == null) return false;
        UUID id = entity.getUUID();
        GaugeState fg = FROZEN_GAUGES.get(id);
        if (fg != null && !fg.isExpired()) return true;
        Long until = FROZEN_UNTIL.get(id);
        return until != null && entity.level().getGameTime() < until;
    }

    public static boolean isEntityQuickened(@Nullable Entity entity) {
        if (entity == null) return false;
        GaugeState qg = QUICKEN_GAUGES.get(entity.getUUID());
        return qg != null && !qg.isExpired();
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
        if (innate != null) {
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
        boolean alive = target.isAlive();
        Collection<Element> effective = alive ? getEffectiveElements(target, elements) : Collections.emptyList();
        int durationTicks = alive ? getEntityMaxDurationTicks(id) : 0;
        if (alive) {
            Long quickenEnd = QUICKEN_UNTIL.get(id);
            if (quickenEnd != null && now < quickenEnd) {
                durationTicks = Math.max(durationTicks, (int) (quickenEnd - now));
            }
            Long frozenEnd = FROZEN_UNTIL.get(id);
            if (frozenEnd != null && now < frozenEnd) {
                durationTicks = Math.max(durationTicks, (int) (frozenEnd - now));
            }
            Element innate = getInnateElement(target);
            if (isOmniInnate(target)) {
                durationTicks = -2; // -2 represents permanent Omni Mutated innate state
            } else if (innate != null && effective.contains(innate)) {
                durationTicks = -1; // -1 represents permanent innate aura
            }
        }
        if (alive && reaction != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE
                && reaction != ElementalReaction.BLOOM && reaction != ElementalReaction.REVERSE_BLOOM
                && reaction != ElementalReaction.BURGEON && reaction != ElementalReaction.HYPERBLOOM) {
            MobHazardAvoidanceManager.reportReactionOrHazard(target, CURRENT_REACTION_ATTACKER.get(), target.position(), reactedAura, trigger);
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
        String msgId = source.type().msgId();
        if ("wind_charge".equals(msgId) || "sonic_boom".equals(msgId)) return false;
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return false;
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        if (source.is(DamageTypeTags.IS_FREEZING)) return false;
        if (source.is(DamageTypeTags.IS_LIGHTNING)) return false;
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) return false;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.GENERIC)) return false;
        if (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT)
                || "mob_attack_no_aggro".equals(msgId) || "sting".equals(msgId) || "mace_smash".equals(msgId)) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return true;
        return source.getDirectEntity() instanceof net.minecraft.world.entity.player.Player
                || source.getEntity() instanceof net.minecraft.world.entity.player.Player;
    }

    public static boolean isElementalOrMagicDamage(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (isPhysicalDamage(source)) return false;
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypeTags.IS_LIGHTNING)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.WITHER_SKULL)
                || source.is(DamageTypes.SONIC_BOOM)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.FIREWORKS)
                || source.is(DamageTypes.THORNS)) {
            return true;
        }
        return "wind_charge".equals(source.type().msgId()) || "sonic_boom".equals(source.type().msgId());
    }

    public static void notifyOmniImmuneEnvironmental(ServerLevel level, LivingEntity target) {
        if (level == null || target == null || !target.isAlive()) return;
        extinguishPyroInnate(target);
        long now = level.getGameTime();
        Long last = LAST_OMNI_ENV_IMMUNE.get(target.getUUID());
        if (last == null || now < last || (now - last) >= 20L) {
            LAST_OMNI_ENV_IMMUNE.put(target.getUUID(), now);
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
            com.velorise.staves.network.ModNetwork.sendAuraSync(target, Collections.emptyList(), null, null, ElementalReaction.IMMUNE, 0.0F, -2);
        }
    }

    public static Element chooseOmniMutatedAttackElement(LivingEntity target, LivingEntity attacker) {
        Element innate = getInnateElement(target);
        Collection<Element> active = getEntityElements(target.getUUID());
        boolean frozen = isEntityFrozen(target);
        boolean quickened = isEntityQuickened(target);
        boolean avoidPyroDamage = target.fireImmune() && innate != Element.CRYO;

        // 1. Cryo / Frozen target -> Forward Melt 2.0x with Pyro (or Superconduct if fire-immune)
        if (frozen || active.contains(Element.CRYO) || innate == Element.CRYO) {
            if (innate != Element.PYRO && !target.fireImmune()) return Element.PYRO;
            if (innate != Element.ELECTRO) return Element.ELECTRO;
        }
        // 2. Pyro aura / innate -> Forward Vaporize 2.0x with Hydro
        if (active.contains(Element.PYRO) || innate == Element.PYRO) {
            if (innate != Element.HYDRO) return Element.HYDRO;
            if (innate != Element.CRYO) return Element.CRYO;
            if (innate != Element.ELECTRO) return Element.ELECTRO;
        }
        // 3. Hydro aura / innate -> Reverse Vaporize 1.5x / Overvape with Pyro (or Dendro Bloom)
        if (active.contains(Element.HYDRO) || innate == Element.HYDRO) {
            if (!avoidPyroDamage && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.DENDRO) return Element.DENDRO;
            if (innate != Element.ELECTRO) return Element.ELECTRO;
        }
        // 4. Quicken or Dendro aura / innate -> Aggravate / Burning / Quicken
        if (quickened) {
            if (innate != Element.ELECTRO) return Element.ELECTRO;
            if (innate != Element.DENDRO) return Element.DENDRO;
        }
        if (active.contains(Element.DENDRO) || innate == Element.DENDRO) {
            if (!avoidPyroDamage && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.ELECTRO) return Element.ELECTRO;
        }
        // 5. Electro aura / innate -> Overloaded / Quicken / Superconduct
        if (active.contains(Element.ELECTRO) || innate == Element.ELECTRO) {
            if (!avoidPyroDamage && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.DENDRO) return Element.DENDRO;
            if (innate != Element.CRYO) return Element.CRYO;
        }
        // 6. Anemo or Geo innate -> Swirl / Crystallize
        if (innate == Element.ANEMO || innate == Element.GEO) {
            return avoidPyroDamage ? Element.HYDRO : Element.PYRO;
        }
        // 7. No active aura on target -> pick a random element from all 7 elements
        Element[] all = Element.values();
        return all[attacker.getRandom().nextInt(all.length)];
    }

    public static DamageSource getElementalDamageSource(ServerLevel level, @Nullable LivingEntity attacker) {
        if (attacker != null && !Boolean.TRUE.equals(com.velorise.staves.entity.GeoFangPillarEntity.IS_GEO_PULSE.get())) {
            return level.damageSources().indirectMagic(attacker, attacker);
        }
        return level.damageSources().magic();
    }

    public static float getMobElementalResistance(@Nullable LivingEntity target, @Nullable Element element) {
        if (!(target instanceof Mob)) return 0.0F;
        if (element == Element.ANEMO && target instanceof Warden) return 0.70F;
        return 0.10F;
    }

    public static float getMobPhysicalResistance(@Nullable LivingEntity target) {
        if (!(target instanceof Mob)) return 0.0F;
        if (target instanceof IronGolem) return 0.70F;
        return 0.10F;
    }

    public static float getEffectivePhysicalMultiplier(LivingEntity target, long now) {
        float res = getMobPhysicalResistance(target);
        if (hasSuperconductDebuff(target, now)) {
            res -= 0.40F;
        }
        return res >= 0.0F ? Math.max(0.0F, 1.0F - res) : (1.0F - res);
    }

    public static float applyMobElementalResistance(@Nullable LivingEntity target, @Nullable Element element, float damage) {
        if (damage <= 0.0F || !(target instanceof Mob)) return damage;
        return damage * Math.max(0.0F, 1.0F - getMobElementalResistance(target, element));
    }

    public static @Nullable Element inferElementFromDamageSource(DamageSource source) {
        String msgId = source.type().msgId();
        if ("sonic_boom".equals(msgId) || "wind_charge".equals(msgId) || source.is(DamageTypes.SONIC_BOOM)) {
            return Element.ANEMO;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) return Element.PYRO;
        if (source.is(DamageTypeTags.IS_FREEZING)) return Element.CRYO;
        if (source.is(DamageTypeTags.IS_LIGHTNING)) return Element.ELECTRO;
        if (source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.FALLING_ANVIL)) {
            return Element.GEO;
        }
        return null;
    }

    public static boolean hurtWithPreAppliedElementalRes(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F || target == null) return false;
        boolean prev = Boolean.TRUE.equals(ELEMENTAL_RES_APPLIED.get());
        ELEMENTAL_RES_APPLIED.set(Boolean.TRUE);
        try {
            return target.hurt(source, amount);
        } finally {
            ELEMENTAL_RES_APPLIED.set(prev);
        }
    }

    public static float applyMobDamageResistances(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0.0F || entity == null || !entity.isAlive() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        long now = entity.level().getGameTime();
        if (isPhysicalDamage(source)) {
            float mult = getEffectivePhysicalMultiplier(entity, now);
            if (hasSuperconductDebuff(entity, now) && entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.05D);
            }
            return amount * mult;
        }
        if (!Boolean.TRUE.equals(ELEMENTAL_RES_APPLIED.get()) && isElementalOrMagicDamage(source)) {
            return applyMobElementalResistance(entity, inferElementFromDamageSource(source), amount);
        }
        return amount;
    }

    public static void triggerMobAggro(ServerLevel level, LivingEntity target, @Nullable LivingEntity attacker) {
        if (attacker == null || target == attacker || !target.isAlive()) return;
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        target.setLastHurtByMob(attacker);
        if (attacker instanceof Player player) {
            target.setLastHurtByPlayer(player);
        }
        if (target instanceof IronGolem golem && golem.isPlayerCreated() && attacker instanceof Player) {
            golem.setPlayerCreated(false);
        }
        if (target instanceof NeutralMob neutral) {
            neutral.setPersistentAngerTarget(attacker.getUUID());
            neutral.startPersistentAngerTimer();
        }
        if (target instanceof Mob mob && !isEntityFrozen(mob)) {
            mob.setTarget(attacker);
        }
    }

    public static void clearEntityState(LivingEntity entity) {
        if (entity == null) return;
        UUID id = entity.getUUID();
        GAUGES.remove(id);
        FROZEN_GAUGES.remove(id);
        QUICKEN_GAUGES.remove(id);
        FROZEN_UNTIL.remove(id);
        FROZEN_POSES.remove(id);
        LAST_FREEZE_STRUGGLE.remove(id);
        QUICKEN_UNTIL.remove(id);
        BURNING_UNTIL.remove(id);
        BURNING_EM_LEVEL.remove(id);
        ELECTRO_CHARGE_EM_LEVEL.remove(id);
        LAST_BURN_PYRO_REFRESH.remove(id);
        INNATE_COOLDOWN_UNTIL.remove(id);
        ELECTRO_CHARGE_TICK.remove(id);
        LAST_BLOOM_SPAWN.remove(id);
        SUPERCONDUCT_UNTIL.remove(id);
        OMNI_INNATE_ENTITIES.remove(id);
        LAST_OMNI_ENV_IMMUNE.remove(id);
        LAST_MUSHROOM_CONTACT.remove(id);
        PLAYER_LAST_HP.remove(id);
        PLAYER_HP_CHANGE_BUFF_UNTIL.remove(id);
        com.velorise.staves.entity.OmniOrbEntity.removeCycleEntry(id);
        com.velorise.staves.item.ArcaneStaffItem.clearPlayerState(id);
        MobHazardAvoidanceManager.clearEntityState(entity);
        if (entity instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
        }
        if (!entity.level().isClientSide()) {
            if (entity instanceof ServerPlayer sp) {
                CrystallizeShardManager.clearPlayerShield(sp);
                ElementalResonanceManager.clearPlayerState(sp.getUUID());
            }
            com.velorise.staves.network.ModNetwork.sendAuraSync(entity, Collections.emptyList(), null, null, null, 0.0F, 0);
        }
    }

    public static void syncEntityOnStartTracking(ServerPlayer player, LivingEntity living) {
        if (player == null || living == null || living.level().isClientSide()) return;
        if (isOmniInnate(living)) {
            com.velorise.staves.network.ModNetwork.sendAuraSyncToPlayer(player, living, Collections.emptyList(), -2);
            return;
        }
        Element innate = getInnateElement(living);
        Collection<Element> elements = getEffectiveElements(living, getEntityElements(living.getUUID()));
        if (!elements.isEmpty()) {
            int duration = (innate != null && elements.contains(innate)) ? -1 : getEntityMaxDurationTicks(living.getUUID());
            com.velorise.staves.network.ModNetwork.sendAuraSyncToPlayer(player, living, elements, duration);
        }
    }

    public static void enforceFrozenPoseIfFrozen(LivingEntity target) {
        if (target != null && isEntityFrozen(target)) {
            enforceFrozenPose(target, target.getUUID());
        }
    }

    public static void clearAll() {
        GAUGES.clear();
        FROZEN_GAUGES.clear();
        QUICKEN_GAUGES.clear();
        FROZEN_UNTIL.clear();
        FROZEN_POSES.clear();
        LAST_FREEZE_STRUGGLE.clear();
        QUICKEN_UNTIL.clear();
        BURNING_UNTIL.clear();
        BURNING_EM_LEVEL.clear();
        ELECTRO_CHARGE_EM_LEVEL.clear();
        LAST_BURN_PYRO_REFRESH.clear();
        INNATE_COOLDOWN_UNTIL.clear();
        ELECTRO_CHARGE_TICK.clear();
        LAST_PROXIMITY_SHOCK.clear();
        LAST_BLOOM_SPAWN.clear();
        SUPERCONDUCT_UNTIL.clear();
        OMNI_INNATE_ENTITIES.clear();
        LAST_OMNI_ENV_IMMUNE.clear();
        LAST_MUSHROOM_CONTACT.clear();
        PLAYER_LAST_HP.clear();
        PLAYER_HP_CHANGE_BUFF_UNTIL.clear();
        ICD_TRACKER.clear();
        REACTION_ICD_TRACKER.clear();
        LAST_THORNS_REFLECT.clear();
        DendroCoreManager.clearAll();
        CrystallizeShardManager.clearAll();
        ElectrifiedWaterManager.clearAll();
        DragonfireCloudManager.clearAll();
        GhastfireFieldManager.clearAll();
        EnvironmentalBlockManager.clearAll();
        ElementalResonanceManager.clearAll();
        MobHazardAvoidanceManager.clearAll();
    }

    private static void applyFreezeState(LivingEntity target, UUID targetId, GaugeState newFreeze, int freezeTicks, long now) {
        FROZEN_GAUGES.put(targetId, newFreeze);
        FROZEN_UNTIL.put(targetId, now + freezeTicks);
        target.setTicksFrozen(0);
        MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        target.stopUsingItem();
        target.releaseUsingItem();
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = false;
        target.hurtMarked = true;

        FROZEN_POSES.computeIfAbsent(targetId, k -> new FrozenPoseState(
                target.getX(), target.getY(), target.getZ(),
                target.getYRot(), target.getXRot(), target.getYHeadRot(), target.yBodyRot
        ));

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setAggressive(false);
            mob.setZza(0.0F);
            mob.setXxa(0.0F);
            mob.setYya(0.0F);
            mob.setSpeed(0.0F);
            mob.setJumping(false);
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
            }
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
            if (!mob.getTags().contains(TAG_FROZEN_STATUE)) {
                mob.addTag(TAG_FROZEN_STATUE);
            }
        }
    }

    private static void enforceFrozenPose(LivingEntity target, UUID id) {
        target.setTicksFrozen(0);
        if (target.isUsingItem()) {
            target.stopUsingItem();
            target.releaseUsingItem();
        }
        if (target instanceof net.minecraft.world.entity.npc.AbstractVillager villager && villager.getUnhappyCounter() > 0) {
            villager.setUnhappyCounter(0);
        }
        FrozenPoseState pose = FROZEN_POSES.get(id);
        if (pose != null) {
            double dy = target.onGround() ? 0.0D : Math.max(-0.08D, target.getDeltaMovement().y);
            target.setDeltaMovement(0.0D, dy, 0.0D);
            target.hasImpulse = false;
            target.hurtMarked = true;
            if (target.distanceToSqr(pose.x(), target.getY(), pose.z()) > 0.0001D) {
                target.teleportTo(pose.x(), target.getY(), pose.z());
            }
            target.setYRot(pose.yRot());
            target.setXRot(pose.xRot());
            target.setYHeadRot(pose.yHeadRot());
            target.setYBodyRot(pose.yBodyRot());
            target.yRotO = pose.yRot();
            target.xRotO = pose.xRot();
            target.yHeadRotO = pose.yHeadRot();
            target.yBodyRotO = pose.yBodyRot();
        } else {
            target.setDeltaMovement(0.0D, target.getDeltaMovement().y < 0 ? -0.05D : 0.0D, 0.0D);
            target.hasImpulse = false;
            target.hurtMarked = true;
        }

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setAggressive(false);
            mob.setZza(0.0F);
            mob.setXxa(0.0F);
            mob.setYya(0.0F);
            mob.setSpeed(0.0F);
            mob.setJumping(false);
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
            }
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
            if (!mob.getTags().contains(TAG_FROZEN_STATUE)) {
                mob.addTag(TAG_FROZEN_STATUE);
            }
        }
    }

    private static void clearFreezeState(LivingEntity target, UUID id) {
        FROZEN_GAUGES.remove(id);
        FROZEN_UNTIL.remove(id);
        FROZEN_POSES.remove(id);
        LAST_FREEZE_STRUGGLE.remove(id);
        target.setTicksFrozen(0);
        MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 6) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
        }
    }

    /**
     * Genshin Impact Spacebar Struggle: allows a Frozen player to mash Space to shave off freeze duration faster.
     */
    public static void handlePlayerFreezeStruggle(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) return;
        UUID id = player.getUUID();
        GaugeState fg = FROZEN_GAUGES.get(id);
        Long frozenEnd = FROZEN_UNTIL.get(id);
        long now = player.level().getGameTime();
        if (fg == null && (frozenEnd == null || now >= frozenEnd)) return;

        Long lastStruggle = LAST_FREEZE_STRUGGLE.get(id);
        if (lastStruggle != null && now >= lastStruggle && (now - lastStruggle) < 2L) {
            return; // Rate limit: max 1 struggle every 2 ticks (10 presses/sec)
        }
        LAST_FREEZE_STRUGGLE.put(id, now);

        if (fg != null) {
            fg.consume(0.35F);
        }
        long newEnd = (frozenEnd != null ? frozenEnd - 8L : now);
        if (fg != null && !fg.isExpired()) {
            newEnd = Math.min(newEnd, now + fg.getRemainingTicks());
        }
        FROZEN_UNTIL.put(id, newEnd);

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.9D, player.getZ(), 5, 0.25D, 0.35D, 0.25D, 0.03D);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, player.getX(), player.getY() + 0.9D, player.getZ(), 4, 0.2D, 0.3D, 0.2D, 0.04D);

        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        if ((fg != null && fg.isExpired()) || now >= newEnd) {
            clearFreezeState(player, id);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.85F, 1.35F);
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL, player.getX(), player.getY() + 0.9D, player.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.06D);
            syncAuras(player, targetGauges.keySet(), null, null, null);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 0.65F, 1.55F + player.getRandom().nextFloat() * 0.3F);
            syncAuras(player, targetGauges.keySet(), null, null, null);
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

    public static boolean isTouchingSoulCampfire(LivingEntity target) {
        if (target == null || target.level().isClientSide()) return false;
        AABB box = target.getBoundingBox().inflate(0.10D, 0.20D, 0.10D);
        BlockPos min = BlockPos.containing(box.minX, box.minY - 0.15D, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (!target.level().hasChunkAt(p)) continue;
            BlockState st = target.level().getBlockState(p);
            if (st.is(Blocks.SOUL_CAMPFIRE) && (!st.hasProperty(CampfireBlock.LIT) || st.getValue(CampfireBlock.LIT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTouchingMushroomBlock(LivingEntity target) {
        if (target == null || target.level().isClientSide()) return false;
        AABB box = target.getBoundingBox().inflate(0.08D, 0.08D, 0.08D);
        BlockPos min = BlockPos.containing(box.minX, box.minY - 0.12D, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (!target.level().hasChunkAt(p)) continue;
            BlockState st = target.level().getBlockState(p);
            if (st.is(MUSHROOM_BLOCKS)
                    || st.getBlock() instanceof HugeMushroomBlock
                    || st.is(Blocks.BROWN_MUSHROOM_BLOCK)
                    || st.is(Blocks.RED_MUSHROOM_BLOCK)
                    || st.is(Blocks.MUSHROOM_STEM)
                    || st.is(Blocks.BROWN_MUSHROOM)
                    || st.is(Blocks.RED_MUSHROOM)) {
                return true;
            }
        }
        return false;
    }

    public static void tryApplyMushroomContactAura(ServerLevel level, LivingEntity target, long now) {
        if (target == null || !target.isAlive() || target.isSpectator()) return;
        if (isOmniInnate(target) || getInnateElement(target) == Element.DENDRO) return;
        UUID id = target.getUUID();
        Long last = LAST_MUSHROOM_CONTACT.get(id);
        if (last != null && now >= last && (now - last) < 20L) return;
        if (!isTouchingMushroomBlock(target)) return;
        LAST_MUSHROOM_CONTACT.put(id, now);
        applyElementBypassIcd(level, target, Element.DENDRO, 1.25F, null, 0.0F);
    }

    public static void updatePlayerHpTracking(ServerPlayer player, long now) {
        if (player == null || !player.isAlive()) return;
        UUID id = player.getUUID();
        float curHp = player.getHealth();
        Float prevHp = PLAYER_LAST_HP.put(id, curHp);
        if (prevHp != null && Math.abs(curHp - prevHp) > 0.01F) {
            PLAYER_HP_CHANGE_BUFF_UNTIL.put(id, now + HP_CHANGE_BUFF_TICKS);
        }
    }

    public static boolean hasRecentHpChangeBuff(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return false;
        long now = player.level().getGameTime();
        updatePlayerHpTracking(player, now);
        Long until = PLAYER_HP_CHANGE_BUFF_UNTIL.get(player.getUUID());
        return until != null && now < until;
    }

    public static @Nullable Element getElementForEffect(MobEffect effect) {
        if (effect == null) return null;
        if (effect == MobEffects.MOVEMENT_SPEED) return Element.ANEMO;
        if (effect == MobEffects.MOVEMENT_SLOWDOWN) return Element.CRYO;
        if (effect == MobEffects.DAMAGE_RESISTANCE) return Element.GEO;
        if (effect == MobEffects.POISON) return Element.DENDRO;
        String desc = effect.getDescriptionId();
        if (desc != null) {
            if (desc.contains("wind_charged")) return Element.ANEMO;
            if (desc.contains("oozing")) return Element.DENDRO;
            if (desc.contains("infested")) return Element.GEO;
        }
        return null;
    }

    public static List<Element> getElementsFromPotionStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptyList();
        List<Element> result = new ArrayList<>();
        Potion potion = PotionUtils.getPotion(stack);
        if (potion == Potions.WATER) {
            result.add(Element.HYDRO);
        }
        for (MobEffectInstance inst : PotionUtils.getMobEffects(stack)) {
            Element mapped = getElementForEffect(inst.getEffect());
            if (mapped != null && !result.contains(mapped)) {
                result.add(mapped);
            } else if (inst.getEffect() == MobEffects.HARM && !result.contains(Element.ELECTRO)) {
                result.add(Element.ELECTRO);
            }
        }
        return result;
    }

    public static List<Element> getElementsFromArrow(ServerLevel level, Arrow arrow) {
        if (arrow == null) return Collections.emptyList();
        List<Element> result = new ArrayList<>();
        try {
            CompoundTag nbt = new CompoundTag();
            arrow.saveWithoutId(nbt);
            for (MobEffectInstance inst : PotionUtils.getAllEffects(nbt)) {
                Element mapped = getElementForEffect(inst.getEffect());
                if (mapped != null && !result.contains(mapped)) {
                    result.add(mapped);
                } else if (inst.getEffect() == MobEffects.HARM && !result.contains(Element.ELECTRO)) {
                    result.add(Element.ELECTRO);
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    public static List<Element> getElementsFromAreaEffectCloud(ServerLevel level, AreaEffectCloud cloud) {
        if (cloud == null) return Collections.emptyList();
        List<Element> result = new ArrayList<>();
        try {
            if (cloud.getPotion() == Potions.WATER) {
                result.add(Element.HYDRO);
            }
            CompoundTag nbt = new CompoundTag();
            cloud.saveWithoutId(nbt);
            for (MobEffectInstance inst : PotionUtils.getAllEffects(nbt)) {
                Element mapped = getElementForEffect(inst.getEffect());
                if (mapped != null && !result.contains(mapped)) {
                    result.add(mapped);
                } else if (inst.getEffect() == MobEffects.HARM && !result.contains(Element.ELECTRO)) {
                    result.add(Element.ELECTRO);
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    public static void handleTippedArrowHit(ServerLevel level, Arrow arrow, LivingEntity target) {
        if (arrow == null || target == null || !target.isAlive() || arrow.getTags().contains("velorise_staves:arrow_elem_applied")) {
            return;
        }
        List<Element> elements = getElementsFromArrow(level, arrow);
        if (elements.isEmpty()) return;
        arrow.addTag("velorise_staves:arrow_elem_applied");
        ServerPlayer shooter = arrow.getOwner() instanceof ServerPlayer sp ? sp : null;
        for (Element elem : elements) {
            applyElementBypassIcd(level, target, elem, 1.0F, shooter, 0.0F);
        }
    }

    public static void handleItemUseFinish(ServerLevel level, LivingEntity entity, ItemStack stack) {
        if (level == null || entity == null || !entity.isAlive() || stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof PotionItem)) return;
        List<Element> elements = getElementsFromPotionStack(stack);
        if (elements.isEmpty()) return;
        ServerPlayer player = entity instanceof ServerPlayer sp ? sp : null;
        for (Element elem : elements) {
            applyElementBypassIcd(level, entity, elem, 1.0F, player, 0.0F);
        }
    }

    public static int getMaceWindBurstLevel(LivingEntity attacker) {
        if (attacker == null) return 0;
        ItemStack mainHand = attacker.getMainHandItem();
        if (mainHand.isEmpty()) return 0;
        String desc = mainHand.getItem().getDescriptionId();
        if (desc == null || !desc.contains("mace")) return 0;
        var enchTags = mainHand.getEnchantmentTags();
        for (int i = 0; i < enchTags.size(); i++) {
            CompoundTag tag = enchTags.getCompound(i);
            if (tag.getString("id").contains("wind_burst")) {
                return Math.max(1, tag.getInt("lvl"));
            }
        }
        return 0;
    }


    public static int getElementalMasteryLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var enchTags = stack.getEnchantmentTags();
        for (int i = 0; i < enchTags.size(); i++) {
            CompoundTag tag = enchTags.getCompound(i);
            if (tag.getString("id").contains("elemental_mastery")) {
                return Math.min(3, Math.max(0, tag.getInt("lvl")));
            }
        }
        return 0;
    }

    public static int getElementalMasteryLevel(@Nullable LivingEntity attacker) {
        if (attacker == null) return 0;
        int mainLvl = getElementalMasteryLevel(attacker.getMainHandItem());
        int base = mainLvl > 0 ? mainLvl : getElementalMasteryLevel(attacker.getOffhandItem());
        return base + ElementalResonanceManager.getBonusElementalMasteryLevels(attacker);
    }

    private static GaugeState createAuraGauge(LivingEntity target, Element elem, float appliedUnits) {
        float scale = ElementalResonanceManager.getPlayerIncomingAuraDurationScale(target, elem);
        return GaugeState.createWithDurationScale(elem, appliedUnits, scale);
    }

    private static void refreshAuraGauge(LivingEntity target, GaugeState existing, Element elem, float appliedUnits) {
        float scale = ElementalResonanceManager.getPlayerIncomingAuraDurationScale(target, elem);
        existing.refresh(appliedUnits, scale);
    }

    private static void notifyResonanceReaction(ElementalReaction reaction, @Nullable ServerPlayer attacker, long now) {
        if (attacker == null || reaction == ElementalReaction.NONE || reaction == ElementalReaction.IMMUNE) return;
        switch (reaction) {
            case OVERLOADED, OVERVAPE, SUPERCONDUCT, ELECTRO_CHARGED ->
                    ElementalResonanceManager.onElectroReactionTriggered(attacker, now);
            case BURNING, BLOOM, REVERSE_BLOOM, SPREAD ->
                    ElementalResonanceManager.onDendroReactionTriggered(attacker, now);
            case QUICKEN, AGGRAVATE -> {
                ElementalResonanceManager.onElectroReactionTriggered(attacker, now);
                ElementalResonanceManager.onDendroReactionTriggered(attacker, now);
            }
            default -> {}
        }
    }

    public static float getAmplifyingMultiplier(int emLevel) {
        return 1.0F + 0.15F * Math.max(0, emLevel);
    }

    public static float getTransformativeMultiplier(int emLevel) {
        return 1.0F + 0.30F * Math.max(0, emLevel);
    }

    public static float getCatalyzeMultiplier(int emLevel) {
        return 1.0F + 0.25F * Math.max(0, emLevel);
    }

    public static int getThornsEnchantmentLevel(LivingEntity entity) {
        if (entity == null) return 0;
        int maxLevel = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.isEmpty()) continue;
            var enchTags = armor.getEnchantmentTags();
            for (int i = 0; i < enchTags.size(); i++) {
                CompoundTag tag = enchTags.getCompound(i);
                String id = tag.getString("id");
                int lvl = tag.getInt("lvl");
                if (lvl > maxLevel && (id.equals("minecraft:thorns") || id.endsWith(":thorns"))) {
                    maxLevel = lvl;
                }
            }
        }
        return maxLevel;
    }

    public static boolean hasThornsEnchantment(LivingEntity entity) {
        return getThornsEnchantmentLevel(entity) > 0;
    }

    @Nullable
    private static Element pickDefenderAuraForThorns(LivingEntity defender) {
        if (defender == null) return null;
        UUID id = defender.getUUID();
        Map<Element, GaugeState> gauges = GAUGES.get(id);
        if (gauges != null && !gauges.isEmpty()) {
            for (Element elem : REACTION_PRIORITY_ORDER) {
                GaugeState gs = gauges.get(elem);
                if (gs != null && !gs.isExpired()) {
                    return elem;
                }
            }
            for (Map.Entry<Element, GaugeState> entry : gauges.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isExpired()) {
                    return entry.getKey();
                }
            }
        }
        GaugeState frozen = FROZEN_GAUGES.get(id);
        if (frozen != null && !frozen.isExpired()) {
            return Element.CRYO;
        }
        GaugeState quicken = QUICKEN_GAUGES.get(id);
        if (quicken != null && !quicken.isExpired()) {
            return Element.DENDRO;
        }
        return null;
    }

    private static void reflectThornsElement(
            ServerLevel level,
            LivingEntity defender,
            LivingEntity attacker,
            long now,
            @Nullable Element fallbackElement
    ) {
        if (level == null || defender == null || attacker == null
                || defender == attacker || !attacker.isAlive() || attacker.isRemoved()
                || IS_PROCESSING_THORNS.get()) {
            return;
        }

        Element reflected = getInnateElement(defender);
        if (reflected == null) {
            reflected = pickDefenderAuraForThorns(defender);
        }
        if (reflected == null) {
            reflected = fallbackElement;
        }
        if (reflected == null && defender.getRemainingFireTicks() > 0 && !defender.fireImmune() && !isEntityInWater(defender)) {
            reflected = Element.PYRO;
        }
        if (reflected == null) {
            return;
        }

        long dedupKey = (((long) attacker.getId()) << 32) | (defender.getId() & 0xFFFFFFFFL);
        Long lastTick = LAST_THORNS_REFLECT.get(dedupKey);
        if (lastTick != null && now >= lastTick && (now - lastTick) < 2L) {
            return;
        }
        LAST_THORNS_REFLECT.put(dedupKey, now);

        int thornsLevel = getThornsEnchantmentLevel(defender);
        if (thornsLevel <= 0) {
            thornsLevel = 1; // e.g., Guardian natural Thorns spikes
        }
        float reflectU = 0.5F * thornsLevel;

        IS_PROCESSING_THORNS.set(true);
        try {
            ServerPlayer defenderPlayer = defender instanceof ServerPlayer sp ? sp : null;
            applyElementBypassIcd(level, attacker, reflected, reflectU, defenderPlayer, 0.0F);
            Vec3 hitPos = attacker.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, hitPos.x, hitPos.y, hitPos.z,
                    6, 0.25D, 0.25D, 0.25D, 0.05D);
        } finally {
            IS_PROCESSING_THORNS.set(false);
        }
    }

    /**
     * Handles environmental damage types and innate elemental mob attacks.
     */
    public static void handleLivingIncomingDamage(LivingEntity target, DamageSource source) {
        if (!target.isAlive() || target.isInvulnerable() || target.level().isClientSide()) return;
        if (source.is(DamageTypes.THORNS)) {
            if (target.level() instanceof ServerLevel level
                    && source.getEntity() instanceof LivingEntity defender
                    && defender != target
                    && !IS_PROCESSING_THORNS.get()) {
                reflectThornsElement(level, defender, target, level.getGameTime(), THORNS_CONTEXT_ELEMENT.get());
            }
            return;
        }
        if (IS_PROCESSING_DAMAGE.get() || IS_PROCESSING_REACTION.get()) return;

        IS_PROCESSING_DAMAGE.set(true);
        try {
            if (target.level() instanceof ServerLevel level) {
                Element preHitAura = hasThornsEnchantment(target) ? pickDefenderAuraForThorns(target) : null;
                processIncomingDamageSources(level, target, source);
                if (source.getEntity() instanceof LivingEntity attacker
                        && attacker != target
                        && attacker.isAlive()
                        && !IS_PROCESSING_THORNS.get()
                        && hasThornsEnchantment(target)) {
                    reflectThornsElement(level, target, attacker, level.getGameTime(), preHitAura);
                }
            }
        } finally {
            IS_PROCESSING_DAMAGE.set(false);
        }
    }

    private static void processIncomingDamageSources(ServerLevel level, LivingEntity target, DamageSource source) {
                if ((target.getRemainingFireTicks() > 0 || target.isOnFire()) && (isPyroInnate(target) || isOmniInnate(target))) {
                    extinguishPyroInnate(target);
                }
                // 0. Omni Mutated Mob Attack: adaptive reaction if target has aura, else random 1 of 7 elements
                if (source.getEntity() instanceof LivingEntity omniAttacker && omniAttacker != target && isOmniInnate(omniAttacker)) {
                    if (omniAttacker.isOnFire() && !target.isInLava()) {
                        target.clearFire();
                    }
                    Element chosenElem = chooseOmniMutatedAttackElement(target, omniAttacker);
                    ServerPlayer attackerPlayer = omniAttacker instanceof ServerPlayer sp ? sp : null;
                    applyElementBypassIcd(level, target, chosenElem, 1.0F, attackerPlayer, 0.0F);
                    if (chosenElem == Element.PYRO
                            && target.isAlive()
                            && !isPyroInnate(target)
                            && !isOmniInnate(target)
                            && !target.fireImmune()
                            && !(target instanceof Warden)) {
                        if (!isEntityInWater(target) && target.getRemainingFireTicks() < 40) {
                            target.setRemainingFireTicks(40);
                        }
                        float fireDmg = (float) (2.0F * StavesConfig.DAMAGE_MULTIPLIER.get());
                        if (fireDmg > 0.0F) {
                            int prevInvuln = target.invulnerableTime;
                            target.invulnerableTime = 0;
                            target.hurt(level.damageSources().onFire(), fireDmg);
                            target.invulnerableTime = prevInvuln;
                        }
                        level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                                6, 0.25D, 0.25D, 0.25D, 0.03D);
                    }
                    return;
                }
                if (isOmniInnate(target)) {
                    return;
                }
                // 1. Environmental Fire
                if (source.is(DamageTypeTags.IS_FIRE)) {
                    handleEnvironmentalFireDamage(target);
                    return;
                }

                // 1b. Mutated Innate Mob Attack (takes priority over mob-specific default elements like Husk/Ravager)
                if (source.getEntity() instanceof LivingEntity mutatedAttacker && mutatedAttacker != target && isMutatedInnate(mutatedAttacker)) {
                    Element mutatedElem = getInnateElement(mutatedAttacker);
                    if (mutatedElem != null) {
                        if (mutatedElem != Element.PYRO && mutatedAttacker.isOnFire() && !target.isInLava()) {
                            target.clearFire();
                        }
                        ServerPlayer attackerPlayer = mutatedAttacker instanceof ServerPlayer sp ? sp : null;
                        applyElementBypassIcd(level, target, mutatedElem, 1.0F, attackerPlayer, 0.0F);
                        if (mutatedElem == Element.PYRO
                                && target.isAlive()
                                && !isPyroInnate(target)
                                && !target.fireImmune()
                                && !(target instanceof Warden)) {
                            if (!isEntityInWater(target) && target.getRemainingFireTicks() < 40) {
                                target.setRemainingFireTicks(40);
                            }
                            float fireDmg = (float) (2.0F * StavesConfig.DAMAGE_MULTIPLIER.get());
                            if (fireDmg > 0.0F) {
                                int prevInvuln = target.invulnerableTime;
                                target.invulnerableTime = 0;
                                target.hurt(level.damageSources().onFire(), fireDmg);
                                target.invulnerableTime = prevInvuln;
                            }
                            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                                    6, 0.25D, 0.25D, 0.25D, 0.03D);
                        }
                        return;
                    }
                }

                // 1c. Wither Skull (Wither Boss, mobs, or player-launched/reflected skulls) -> Dendro (1.0U)
                if (source.is(DamageTypes.WITHER_SKULL)
                        || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.WitherSkull) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElementBypassIcd(level, target, Element.DENDRO, 1.0F, attacker, 0.0F);
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
                if ((source.type().msgId().contains("wind_charge") || source.type().msgId().contains("breeze"))) {
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

                // 14. Vex melee attack -> Anemo (0.5U)
                if (source.getEntity() instanceof Vex) {
                    applyElement(level, target, Element.ANEMO, 0.5F, null, 0.0F);
                    return;
                }

                // 15. Fireworks explosion -> Pyro (1.5U)
                if (source.is(DamageTypes.FIREWORKS)
                        || source.getDirectEntity() instanceof FireworkRocketEntity) {
                    ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sp ? sp : null;
                    applyElement(level, target, Element.PYRO, 1.5F, attacker, 0.0F);
                    return;
                }

                // 15B. Tipped Arrow elemental application (1.0U mapped element)
                if (source.getDirectEntity() instanceof Arrow arrow) {
                    handleTippedArrowHit(level, arrow, target);
                }

                // 15C. Mace with Wind Burst enchantment -> deals bonus Anemo damage & triggers Swirl
                if (source.getEntity() instanceof LivingEntity maceAttacker && maceAttacker != target
                        && source.getDirectEntity() == maceAttacker) {
                    int windBurstLvl = getMaceWindBurstLevel(maceAttacker);
                    if (windBurstLvl > 0) {
                        ServerPlayer attackerPlayer = maceAttacker instanceof ServerPlayer sp ? sp : null;
                        float anemoU = 1.0F + (windBurstLvl - 1) * 0.5F;
                        float bonusAnemoDmg = 2.0F + windBurstLvl * 1.5F;
                        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.6D, target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 1.15F);
                        applyElementBypassIcd(level, target, Element.ANEMO, anemoU, attackerPlayer, bonusAnemoDmg);
                    }
                }

                // 16. Attacker with Innate Element
                if (source.getEntity() instanceof LivingEntity attackerMob && attackerMob != target) {
                    Element innate = getInnateElement(attackerMob);
                    if (innate != null) {
                        if (innate != Element.PYRO && attackerMob.isOnFire() && !target.isInLava()) {
                            target.clearFire();
                        }
                        ServerPlayer attackerPlayer = attackerMob instanceof ServerPlayer sp ? sp : null;
                        applyElementBypassIcd(level, target, innate, 1.0F, attackerPlayer, 0.0F);
                    }
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

        if (isOmniInnate(target)) return 0.0F;
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
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT,
                        applyMobElementalResistance(target, Element.PYRO, meltDmg));
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
                syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE,
                        applyMobElementalResistance(target, Element.PYRO, vapeDmg));
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
        if (isOmniInnate(target)) {
            extinguishPyroInnate(target);
            return;
        }
        Element innate = getInnateElement(target);
        if (innate == Element.PYRO) {
            extinguishPyroInnate(target);
            return;
        }
        if (innate == Element.CRYO || innate == Element.HYDRO) return;

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
        if (isOmniInnate(target)) {
            extinguishPyroInnate(target);
            return;
        }
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
            if (target.fireImmune() || target instanceof Warden) {
                return;
            }
            if (!BURNING_UNTIL.containsKey(id) && checkReactionIcd(target, ElementalReaction.BURNING, now)) {
                long burnTicks = ElementalResonanceManager.getBurningDurationTicksForTarget(
                        target, (innate == Element.DENDRO) ? 100L : 200L);
                BURNING_UNTIL.put(id, now + burnTicks);
                LAST_BURN_PYRO_REFRESH.put(id, now);
                if (innate == null || innate == Element.DENDRO || innate == Element.GEO) {
                    targetGauges.put(Element.PYRO, GaugeState.createBurning(2.0F));
                }
                if (target.getRemainingFireTicks() < 100) {
                    target.setSecondsOnFire(5);
                }
                enforceInnateLock(target, targetGauges, id, innate, now);
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.PYRO, ElementalReaction.BURNING, 0.0F);
            }
            return;
        }

        // No reactive aura -> apply or refresh Pyro (2.0U on Soul Campfire, 1.0U on regular fire/lava/campfire)
        if (innate != Element.PYRO) {
            boolean soulFire = isTouchingSoulCampfire(target);
            float appliedPyro = soulFire ? 2.5F : 1.25F; // 2.5F * 0.8 tax = 2.0U; 1.25F * 0.8 tax = 1.0U
            boolean wasMissing = !targetGauges.containsKey(Element.PYRO);
            if (wasMissing) {
                targetGauges.put(Element.PYRO, createAuraGauge(target, Element.PYRO, appliedPyro));
                syncAuras(target, targetGauges.keySet(), null, null, null, 0.0F);
            } else {
                GaugeState existing = targetGauges.get(Element.PYRO);
                if (existing != null && (soulFire || target.isInLava() || target.isOnFire() || target.getRemainingFireTicks() > 0)) {
                    refreshAuraGauge(target, existing, Element.PYRO, appliedPyro);
                    if (soulFire || now % 20 == 0) {
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
        if (!target.isAlive() || target.isSpectator() || isOmniInnate(target)) return;
        UUID id = target.getUUID();
        Element innate = getInnateElement(target);

        // 1A. Innate Dendro mob (Bee, Bogged, Slime, Mutated Dendro) in water or rain -> Trigger Bloom & spawn Dendro Core!
        if (innate == Element.DENDRO) {
            long now = target.level().getGameTime();
            Long lastBloom = INNATE_COOLDOWN_UNTIL.get(id);
            if (lastBloom == null || now >= lastBloom) {
                INNATE_COOLDOWN_UNTIL.put(id, now + 20L); // 1.0s ICD
                if (target.level() instanceof ServerLevel sl) {
                    DendroCoreManager.spawn(sl, target.position(), null, 1.0F);
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 0.8D, target.getZ(), 20, 0.6D, 0.4D, 0.6D, 0.04D);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
                syncAuras(target, Collections.singleton(Element.DENDRO), Element.DENDRO, Element.HYDRO, ElementalReaction.BLOOM, 0.0F);
            }
            return;
        }

        // 1B. Innate Pyro mob in water or rain -> Vaporize!
        if (innate == Element.PYRO) {
            long now = target.level().getGameTime();
            Long lastVape = INNATE_COOLDOWN_UNTIL.get(id);
            if (lastVape == null || now >= lastVape) {
                INNATE_COOLDOWN_UNTIL.put(id, now + 20L); // 1.0s ICD
                boolean sensitive = isNaturallyWaterSensitive(target);
                float vapeDmg = sensitive ? applyMobElementalResistance(target, Element.HYDRO, 3.0F) : 0.0F;
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 14, 0.3D, 0.3D, 0.3D, 0.04D);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.2F);
                    if (sensitive && vapeDmg > 0.0F) {
                        hurtWithPreAppliedElementalRes(target, sl.damageSources().magic(), vapeDmg);
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
        BURNING_EM_LEVEL.remove(id);
        ELECTRO_CHARGE_EM_LEVEL.remove(id);
            LAST_BURN_PYRO_REFRESH.remove(id);
            target.clearFire();
            boolean sensitive = isNaturallyWaterSensitive(target);
            float vapeDmg = sensitive ? applyMobElementalResistance(target, Element.HYDRO, 2.5F) : 0.0F;
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.03D);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.3F);
                if (sensitive && vapeDmg > 0.0F) {
                    hurtWithPreAppliedElementalRes(target, target.level().damageSources().magic(), vapeDmg);
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
            if (lastSpawn == null || now < lastSpawn || (now - lastSpawn) >= 10L) {
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
            float ecDmg = applyMobElementalResistance(target, Element.ELECTRO, 1.0F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.8D, target.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.02D);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.8F);
                hurtWithPreAppliedElementalRes(target, sl.damageSources().magic(), ecDmg);
            }
            syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, ecDmg);
            return;
        }

        // 5. Environmental Wet: Apply or keep Hydro refreshed while in water or rain
        if (innate == null) {
            if (!targetGauges.containsKey(Element.HYDRO)) {
                targetGauges.put(Element.HYDRO, createAuraGauge(target, Element.HYDRO, 1.25F));
                syncAuras(target, targetGauges.keySet(), null, null, null, 0.0F);
            } else {
                GaugeState hydro = targetGauges.get(Element.HYDRO);
                if (hydro != null) {
                    refreshAuraGauge(target, hydro, Element.HYDRO, 1.25F);
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

    public static boolean isHostileMob(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) return false;
        if (entity instanceof Npc) return false;
        if (entity instanceof Animal) return false;
        if (entity instanceof AbstractGolem) return false;
        if (isBossEntity(entity) || isPassiveAquaticOrAmbient(entity)) return false;
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    public static void checkInnateMutation(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE) && !isEntityFrozen(mob)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
        }
        // Only hostile mobs with attack capability can mutate (Villagers, passive animals, golems, etc. never mutate)
        if (!isHostileMob(entity)) return;

        CompoundTag data = entity.getPersistentData();
        if (data.contains("VeloriseInnateChecked") || entity.getTags().contains("VeloriseInnateChecked")) return;
        data.putBoolean("VeloriseInnateChecked", true);
        entity.addTag("VeloriseInnateChecked");

        // If the entity already has a natural innate element, do not mutate it
        if (getNaturalInnateElement(entity) != null) return;

        // Configurable chance (default 7% = 0.07): 6/7 basic element (all 7 elements), 1/7 Omni Mutated
        double mutateChance = 0.07D;
        try {
            mutateChance = StavesConfig.INNATE_MUTATION_CHANCE.get();
        } catch (Exception ignored) {}
        double roll = entity.getRandom().nextDouble();
        if (mutateChance > 0.0D && roll < mutateChance) {
            if (roll < (mutateChance / 7.0D)) {
                data.putString("VeloriseInnate", "OMNI");
                entity.addTag(TAG_INNATE_OMNI);
                entity.addTag("VeloriseInnate:OMNI");
                OMNI_INNATE_ENTITIES.add(entity.getUUID());
                extinguishPyroInnate(entity);
                com.velorise.staves.network.ModNetwork.sendAuraSync(entity, Collections.emptyList(), null, null, null, 0.0F, -2);
                return;
            }
            Element[] elements = Element.values();
            Element chosen = elements[entity.getRandom().nextInt(elements.length)];
            data.putString("VeloriseInnate", chosen.name());
            entity.addTag("VeloriseInnate:" + chosen.name());
            if (chosen == Element.PYRO) {
                extinguishPyroInnate(entity);
            }

            // Immediately initialize mutated innate element in GAUGES (including Anemo & Geo) so it always syncs and renders overhead
            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(entity.getUUID(), k -> new ConcurrentHashMap<>());
            gauges.put(chosen, new GaugeState(chosen, 1.25F));
            syncAuras(entity, Collections.singleton(chosen), null, null, null, 0.0F);
        }
    }

    public static boolean isOmniInnate(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.level().isClientSide() && !living.getPersistentData().contains("VeloriseInnateChecked") && !living.getTags().contains("VeloriseInnateChecked")) {
            checkInnateMutation(living);
        }
        if (OMNI_INNATE_ENTITIES.contains(living.getUUID())) return true;
        if (living.getTags().contains(TAG_INNATE_OMNI) || living.getTags().contains("VeloriseInnate:OMNI")) {
            OMNI_INNATE_ENTITIES.add(living.getUUID());
            return true;
        }
        if (living.getPersistentData().contains("VeloriseInnate")
                && "OMNI".equalsIgnoreCase(living.getPersistentData().getString("VeloriseInnate"))) {
            OMNI_INNATE_ENTITIES.add(living.getUUID());
            return true;
        }
        return false;
    }

    public static @Nullable Element getInnateElement(LivingEntity target) {
        if (!target.level().isClientSide() && !target.getPersistentData().contains("VeloriseInnateChecked") && !target.getTags().contains("VeloriseInnateChecked")) {
            checkInnateMutation(target);
        }
        if (target.getTags().contains(TAG_INNATE_OMNI) || target.getTags().contains("VeloriseInnate:OMNI")) {
            OMNI_INNATE_ENTITIES.add(target.getUUID());
            return null;
        }
        if (target.getPersistentData().contains("VeloriseInnate")) {
            String elemName = target.getPersistentData().getString("VeloriseInnate");
            if ("OMNI".equalsIgnoreCase(elemName)) {
                OMNI_INNATE_ENTITIES.add(target.getUUID());
                return null;
            }
            try {
                return Element.valueOf(elemName);
            } catch (IllegalArgumentException ignored) {}
        }
        for (String tag : target.getTags()) {
            if (tag.startsWith("VeloriseInnate:")) {
                String val = tag.substring("VeloriseInnate:".length());
                if ("OMNI".equalsIgnoreCase(val)) {
                    OMNI_INNATE_ENTITIES.add(target.getUUID());
                    return null;
                }
                try {
                    return Element.valueOf(val);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return getNaturalInnateElement(target);
    }

    public static boolean isPyroInnate(@Nullable Entity entity) {
        return entity instanceof LivingEntity living && getInnateElement(living) == Element.PYRO;
    }

    public static boolean isPyroResistant(@Nullable LivingEntity target) {
        return target != null && (target instanceof Warden || target.fireImmune() || target.hasEffect(MobEffects.FIRE_RESISTANCE));
    }

    public static void ensurePreDeathPyroIgnite(@Nullable LivingEntity target, int seconds) {
        if (target == null || !target.isAlive() || seconds <= 0) return;
        if (isPyroInnate(target) || isPyroResistant(target) || isEntityInWater(target)) return;
        if (target.getRemainingFireTicks() < seconds * 20) {
            target.setSecondsOnFire(seconds);
        }
    }

    public static void extinguishPyroInnate(@Nullable LivingEntity entity) {
        if (entity == null || (!isPyroInnate(entity) && !isOmniInnate(entity))) return;
        if (entity.getRemainingFireTicks() > 0 || entity.isOnFire()) {
            entity.clearFire();
            entity.setSharedFlagOnFire(false);
        }
        if (!entity.level().isClientSide()) {
            UUID id = entity.getUUID();
            BURNING_UNTIL.remove(id);
            BURNING_EM_LEVEL.remove(id);
            LAST_BURN_PYRO_REFRESH.remove(id);
        }
    }

    public static boolean isMutatedInnate(LivingEntity entity) {
        if (entity.getPersistentData().contains("VeloriseInnate")) return true;
        for (String tag : entity.getTags()) {
            if (tag.startsWith("VeloriseInnate:")) return true;
        }
        return false;
    }

    /**
     * Rolls fixed elemental essence drops on death for Hostile Enemies killed by a player
     * (no Looting scaling, split tiny/medium Slimes and Magma Cubes excluded):
     * Omni Mutated 25%, Regular Mutated 15%, Natural Innate 5%.
     */
    public static void tryDropElementOnDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(entity instanceof Enemy)) return;
        if (entity instanceof Slime slime && slime.getSize() < 3) return;
        boolean playerKill = (source.getEntity() instanceof Player) || (entity.getKillCredit() instanceof Player);
        if (!playerKill) return;

        double chance;
        net.minecraft.world.item.Item dropItem;
        if (isOmniInnate(entity)) {
            chance = 0.25D;
            dropItem = VeloriseStavesMod.getElementItem(null);
        } else if (isMutatedInnate(entity)) {
            Element mutated = getInnateElement(entity);
            if (mutated == null) return;
            chance = 0.15D;
            dropItem = VeloriseStavesMod.getElementItem(mutated);
        } else {
            Element natural = getNaturalInnateElement(entity);
            if (natural == null) return;
            chance = 0.05D;
            dropItem = VeloriseStavesMod.getElementItem(natural);
        }

        if (level.random.nextDouble() < chance) {
            ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5D, entity.getZ(), new ItemStack(dropItem));
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
            level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.6D, entity.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.04D);
        }
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
                || target instanceof Axolotl) {
            return Element.HYDRO;
        }
        if (target.getType().getDescriptionId().contains("breeze")
                || target instanceof Phantom
                || target instanceof Vex) {
            return Element.ANEMO;
        }
        if ((target instanceof Creeper creeper && creeper.isPowered())
                || target instanceof EnderMan
                || target instanceof EnderDragon
                || target instanceof Endermite
                || target instanceof Shulker) {
            return Element.ELECTRO;
        }
        if (target instanceof MushroomCow
                || target instanceof Bee || target instanceof CaveSpider
                || (target instanceof Slime && !(target instanceof MagmaCube))
                || target.getType().getDescriptionId().contains("bogged")) {
            return Element.DENDRO;
        }
        if (target.getType().getDescriptionId().contains("armadillo")) {
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
        if (entry == null || now < entry.firstHitTime || (now - entry.firstHitTime) >= 50) {
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
            case BURNING -> 40; // 2.0s
            case AGGRAVATE, SPREAD -> 5; // 0.25s
            case FORWARD_VAPORIZE, REVERSE_VAPORIZE, FORWARD_MELT, REVERSE_MELT, OVERVAPE -> 8; // 0.4s
            default -> 5;
        };
    }

    public static boolean checkReactionIcd(LivingEntity target, ElementalReaction reaction, long now) {
        long key = (((long) target.getId()) << 8) | (reaction.ordinal() & 0xFFL);
        Long lastTrigger = REACTION_ICD_TRACKER.get(key);
        int cdTicks = getReactionCooldownTicks(reaction);
        if (lastTrigger != null && now >= lastTrigger && (now - lastTrigger) < cdTicks) {
            return false;
        }
        REACTION_ICD_TRACKER.put(key, now);
        return true;
    }

    /**
     * Resolves an elemental attack hitting a target entity.
     */
    public static void applyElementBypassIcd(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage
    ) {
        applyElement(level, target, attackElement, attackU, attacker, baseDamage, true);
    }

    public static void applyElement(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage
    ) {
        applyElement(level, target, attackElement, attackU, attacker, baseDamage, false, null);
    }

    private static void applyElement(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            boolean bypassStandardIcd
    ) {
        applyElement(level, target, attackElement, attackU, attacker, baseDamage, bypassStandardIcd, null);
    }

    /**
     * Evaluates an imbued melee sword hit against a target inside incoming-damage handling.
     * Applies 1.0U and triggers EGT reactions only on full-strength swings (>= 0.9F scale);
     * returns the final incoming sword damage (amplified/boosted by reactions, or unchanged
     * physical damage when immune / below full charge).
     */
    public static float applyImbuedSwordHit(LivingEntity target, DamageSource source, float incomingDamage) {
        if (target == null || !target.isAlive() || incomingDamage <= 0.0F) return incomingDamage;
        if (!(target.level() instanceof ServerLevel level)) return incomingDamage;
        if (Boolean.TRUE.equals(IS_PROCESSING_SWORD_IMBUE.get())) return incomingDamage;
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return incomingDamage;
        if (!(source.getDirectEntity() instanceof ServerPlayer attacker)) return incomingDamage;
        if (attacker == target) return incomingDamage;

        ItemStack weapon = attacker.getMainHandItem();
        boolean hasFireAspect = com.velorise.staves.item.SwordImbueHelper.hasFireAspect(weapon);
        String imbueKey = com.velorise.staves.item.SwordImbueHelper.getImbuedKey(weapon);
        if (imbueKey == null) {
            if (hasFireAspect) {
                ensurePreDeathPyroIgnite(target, 4);
            }
            return incomingDamage;
        }
        if (com.velorise.staves.item.SwordImbueHelper.isNonPyroImbued(weapon) && hasFireAspect) {
            ensurePreDeathPyroIgnite(target, 4);
            return incomingDamage;
        }

        if (attacker.getAttackStrengthScale(0.5F) < 0.9F) {
            if (hasFireAspect) {
                ensurePreDeathPyroIgnite(target, 4);
            }
            return incomingDamage;
        }

        Element chosen = com.velorise.staves.item.SwordImbueHelper.OMNI_KEY.equals(imbueKey)
                ? com.velorise.staves.entity.OmniOrbEntity.chooseBestElementForTarget(target, attacker)
                : com.velorise.staves.item.SwordImbueHelper.getImbuedElement(weapon);
        if (chosen == null) return incomingDamage;
        if (chosen == Element.PYRO || hasFireAspect) {
            ensurePreDeathPyroIgnite(target, 4);
        }

        float[] outDamage = new float[] { incomingDamage };
        IS_PROCESSING_SWORD_IMBUE.set(true);
        try {
            applyElement(level, target, chosen, 1.0F, attacker, incomingDamage, true, outDamage);
        } finally {
            IS_PROCESSING_SWORD_IMBUE.set(false);
        }
        return outDamage[0];
    }

    private static float dealOrDeferDamage(
            ServerLevel level,
            LivingEntity target,
            @Nullable ServerPlayer attacker,
            float damage,
            Element attackElement,
            float @Nullable [] meleeOut
    ) {
        if (meleeOut != null) {
            meleeOut[0] = damage;
            return damage * getEffectivePhysicalMultiplier(target, level.getGameTime());
        }
        float resDamage = applyMobElementalResistance(target, attackElement, damage);
        if (resDamage > 0.0F) {
            hurtWithPreAppliedElementalRes(target, getElementalDamageSource(level, attacker), resDamage);
        }
        return resDamage;
    }

    private static void applyElement(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            boolean bypassStandardIcd,
            float @Nullable [] meleeOut
    ) {
        if (!target.isAlive() || target.isInvulnerable()) return;
        int depth = REACTION_DEPTH.get();
        if (depth >= MAX_REACTION_DEPTH) {
            if (meleeOut != null) meleeOut[0] = baseDamage;
            return;
        }
        REACTION_DEPTH.set(depth + 1);
        LivingEntity prevAttacker = CURRENT_REACTION_ATTACKER.get();
        CURRENT_REACTION_ATTACKER.set(attacker);
        try {
            applyElementGuarded(level, target, attackElement, attackU, attacker, baseDamage, bypassStandardIcd, meleeOut);
        } finally {
            if (prevAttacker == null) CURRENT_REACTION_ATTACKER.remove();
            else CURRENT_REACTION_ATTACKER.set(prevAttacker);
            REACTION_DEPTH.set(depth);
        }
    }

    private static void applyElementGuarded(
            ServerLevel level,
            LivingEntity target,
            Element attackElement,
            float attackU,
            @Nullable ServerPlayer attacker,
            float baseDamage,
            boolean bypassStandardIcd,
            float @Nullable [] meleeOut
    ) {

        boolean physicalMelee = meleeOut != null;
        if (meleeOut != null) {
            meleeOut[0] = baseDamage;
        }

        // Notify mob aggro when a player attacks directly with staves/magic (skip for GeoFangPillar waves which taunt onto the pillar)
        if (!physicalMelee && !Boolean.TRUE.equals(com.velorise.staves.entity.GeoFangPillarEntity.IS_GEO_PULSE.get())) {
            triggerMobAggro(level, target, attacker);
        }

        boolean egtEnabled = StavesConfig.ENABLE_ELEMENTAL_GAUGE.get();
        double dmgMult = StavesConfig.DAMAGE_MULTIPLIER.get();
        float effectiveDamage = physicalMelee ? baseDamage : (float) (baseDamage * dmgMult);

        // Crystallize Shield matching elemental resistance:
        // 250% absorption efficiency -> 40% incoming damage (damage / 2.5), only while Absorption hearts remain
        if (!physicalMelee && target instanceof ServerPlayer sp && effectiveDamage > 0.0F) {
            Element activeShield = CrystallizeShardManager.getActiveShield(sp, level.getGameTime());
            if (activeShield == attackElement) {
                effectiveDamage *= 0.4F;
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.8F);
            }
        }

        Element preThornsAura = pickDefenderAuraForThorns(target);
        Element thornsContext = preThornsAura != null ? preThornsAura : attackElement;

        // Omni Mutated immunity: 100% immune to all elemental attacks and auras (takes only physical damage)
        if (isOmniInnate(target)) {
            extinguishPyroInnate(target);
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
            enforceFrozenPoseIfFrozen(target);
            com.velorise.staves.network.ModNetwork.sendAuraSync(target, Collections.emptyList(), attackElement, attackElement, ElementalReaction.IMMUNE, 0.0F, -2);
            return;
        }

        // Same-element immunity (Genshin Impact): Innate elemental beings are 100% immune to their own element
        Element innateElement = getInnateElement(target);
        if (innateElement != null && attackElement == innateElement) {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
            Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(target.getUUID(), k -> new ConcurrentHashMap<>());
            if (innateElement.isAura() || isMutatedInnate(target)) {
                targetGauges.putIfAbsent(innateElement, new GaugeState(innateElement, 1.25F));
            }
            enforceFrozenPoseIfFrozen(target);
            syncAuras(target, targetGauges.keySet(), innateElement, attackElement, ElementalReaction.IMMUNE);
            if (!physicalMelee && attacker != null && attacker != target && target.isAlive()
                    && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
                reflectThornsElement(level, target, attacker, level.getGameTime(), innateElement);
            }
            return;
        }

        // Pyro-resistant targets (Warden, fireImmune mobs, Fire Resistance effect):
        // - Elemental-imbued swords: cannot activate Pyro mark or trigger reactions -> IMMUNE feedback, normal physical damage only.
        // - Staves / magic: deal 0 Pyro damage, while allowing Pyro aura attachment rules to proceed.
        if (attackElement == Element.PYRO && isPyroResistant(target)) {
            if (physicalMelee) {
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
                enforceFrozenPoseIfFrozen(target);
                syncAuras(target, getEntityElements(target.getUUID()), null, Element.PYRO, ElementalReaction.IMMUNE);
                return;
            }
            baseDamage = 0.0F;
            effectiveDamage = 0.0F;
        } else if (attackElement == Element.PYRO) {
            ensurePreDeathPyroIgnite(target, 3);
        }

        // Always allow non-immune Electro/Pyro attacks to detonate nearby physical 3D Dendro Cores (Hyperbloom / Burgeon)
        DendroCoreManager.checkReactionsNear(level, target.position(), 3.5D, attackElement, attacker);
        if (attackElement == Element.PYRO) {
            EnvironmentalBlockManager.applyPyroEnvironmentEffects(level, target.position(), 1.5D);
        }

        if (!physicalMelee) {
            effectiveDamage = ElementalResonanceManager.applyOffensiveElementalResonance(
                    level, target, attackElement, attacker, effectiveDamage);
        }

        DamageSource elemDmgSource = getElementalDamageSource(level, attacker);

        if (!egtEnabled) {
            if (physicalMelee) {
                meleeOut[0] = effectiveDamage;
                return;
            }
            float resDmg = applyMobElementalResistance(target, attackElement, effectiveDamage);
            Element prevCtx = THORNS_CONTEXT_ELEMENT.get();
            THORNS_CONTEXT_ELEMENT.set(thornsContext);
            try {
                hurtWithPreAppliedElementalRes(target, elemDmgSource, resDmg);
                enforceFrozenPoseIfFrozen(target);
            } finally {
                if (prevCtx == null) THORNS_CONTEXT_ELEMENT.remove();
                else THORNS_CONTEXT_ELEMENT.set(prevCtx);
            }
            if (attacker != null && attacker != target && target.isAlive()
                    && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
                reflectThornsElement(level, target, attacker, level.getGameTime(), thornsContext);
            }
            return;
        }

        long now = level.getGameTime();

        // Standard ICD check (3-Hit / 2.5s rule) for elemental attacks
        boolean canApply = bypassStandardIcd || checkStandardIcd(target, attackElement, attacker, now);
        if (!canApply) {
            if (physicalMelee) {
                meleeOut[0] = effectiveDamage;
                return;
            }
            float resDmg = applyMobElementalResistance(target, attackElement, effectiveDamage);
            if (resDmg > 0.0F) {
                Element prevCtx = THORNS_CONTEXT_ELEMENT.get();
                THORNS_CONTEXT_ELEMENT.set(thornsContext);
                try {
                    hurtWithPreAppliedElementalRes(target, elemDmgSource, resDmg);
                    enforceFrozenPoseIfFrozen(target);
                } finally {
                    if (prevCtx == null) THORNS_CONTEXT_ELEMENT.remove();
                    else THORNS_CONTEXT_ELEMENT.set(prevCtx);
                }
                syncAuras(target, getEntityElements(target.getUUID()), null, attackElement, null, resDmg);
            }
            if (attacker != null && attacker != target && target.isAlive()
                    && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
                reflectThornsElement(level, target, attacker, now, thornsContext);
            }
            return;
        }

        boolean prevReaction = IS_PROCESSING_REACTION.get();
        Element prevCtx = THORNS_CONTEXT_ELEMENT.get();
        IS_PROCESSING_REACTION.set(true);
        THORNS_CONTEXT_ELEMENT.set(thornsContext);
        try {
            applyElementInternal(level, target, attackElement, attackU, attacker, baseDamage, effectiveDamage, dmgMult, now, meleeOut);
            enforceFrozenPoseIfFrozen(target);
        } finally {
            if (prevCtx == null) THORNS_CONTEXT_ELEMENT.remove();
            else THORNS_CONTEXT_ELEMENT.set(prevCtx);
            IS_PROCESSING_REACTION.set(prevReaction);
        }
        if (!physicalMelee && attacker != null && attacker != target && target.isAlive()
                && !IS_PROCESSING_THORNS.get() && hasThornsEnchantment(target)) {
            reflectThornsElement(level, target, attacker, now, thornsContext);
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
            long now,
            float @Nullable [] meleeOut
    ) {
        UUID targetId = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>());
        int emLevel = getElementalMasteryLevel(attacker);
        float ampEmMult = getAmplifyingMultiplier(emLevel);
        float transEmMult = getTransformativeMultiplier(emLevel);
        float catEmMult = getCatalyzeMultiplier(emLevel);

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
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    float removedU = Math.min(1.0F, attackU * 0.5F);
                    executeReactionEffect(level, target, ElementalReaction.SWIRL, attackElement, removedU, attacker, dmgMult);
                    syncAuras(target, Collections.singleton(Element.ANEMO), attackElement, Element.ANEMO, ElementalReaction.SWIRL, resDmg);
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
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    syncAuras(target, targetGauges.keySet(), attackElement, Element.GEO, ElementalReaction.CRYSTALLIZE, resDmg);
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
                float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                syncAuras(target, targetGauges.keySet(), Element.GEO, Element.DENDRO, null, resDmg);
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
                targetGauges.put(Element.HYDRO, createAuraGauge(target, Element.HYDRO, 1.25F));
            }
        } else if (target.isInLava() || target.isOnFire() || target.getRemainingFireTicks() > 0) {
            if (innateElement == null) {
                if (!targetGauges.containsKey(Element.PYRO)) {
                    targetGauges.put(Element.PYRO, createAuraGauge(target, Element.PYRO, 1.25F));
                } else {
                    GaugeState existing = targetGauges.get(Element.PYRO);
                    if (existing != null) {
                        refreshAuraGauge(target, existing, Element.PYRO, 1.25F);
                    }
                }
            }
        } else if (target.isInPowderSnow) {
            if (innateElement == null && !targetGauges.containsKey(Element.CRYO)) {
                targetGauges.put(Element.CRYO, createAuraGauge(target, Element.CRYO, 1.25F));
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
                    float bonus = (float) (1.0F * dmgMult) * catEmMult;
                    effectiveDamage += bonus;
                    catalyzeReaction = ElementalReaction.AGGRAVATE;
                    notifyResonanceReaction(ElementalReaction.AGGRAVATE, attacker, now);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 18, 0.35D, 0.4D, 0.35D, 0.08D);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
                }
                // If no underlying Dendro aura is present to re-trigger Quicken, coexist as underlying Electro aura and deal the Aggravate hit
                if (!targetGauges.containsKey(Element.DENDRO) && !targetGauges.containsKey(Element.HYDRO) && !targetGauges.containsKey(Element.PYRO) && !targetGauges.containsKey(Element.CRYO)) {
                    if (innateElement == null) {
                        if (targetGauges.containsKey(Element.ELECTRO)) {
                            refreshAuraGauge(target, targetGauges.get(Element.ELECTRO), Element.ELECTRO, attackU);
                        } else {
                            targetGauges.put(Element.ELECTRO, createAuraGauge(target, Element.ELECTRO, attackU));
                        }
                    }
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.ELECTRO, catalyzeReaction, resDmg);
                    return;
                }
            } else if (attackElement == Element.DENDRO) {
                if (checkReactionIcd(target, ElementalReaction.SPREAD, now)) {
                    float bonus = (float) (1.25F * dmgMult) * catEmMult;
                    effectiveDamage += bonus;
                    catalyzeReaction = ElementalReaction.SPREAD;
                    notifyResonanceReaction(ElementalReaction.SPREAD, attacker, now);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
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
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.DENDRO, catalyzeReaction, resDmg);
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
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    return;
                }
                freezeGauge.consume(8.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                float bonus = (float) (10.0F * dmgMult) * transEmMult;
                float shatterDamage = effectiveDamage + bonus;
                float resShatter = dealOrDeferDamage(level, target, attacker, shatterDamage, attackElement, meleeOut);
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
                syncAuras(target, targetGauges.keySet(), Element.CRYO, attackElement, ElementalReaction.SHATTER, resShatter);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
                return;
            } else if (attackElement == Element.PYRO) {
                if (!checkReactionIcd(target, ElementalReaction.FORWARD_MELT, now)) {
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    return;
                }
                freezeGauge.consume(attackU * 2.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                float meltDamage = effectiveDamage * 2.0F * ampEmMult;
                float resMelt = dealOrDeferDamage(level, target, attacker, meltDamage, attackElement, meleeOut);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 25, 0.3D, 0.35D, 0.3D, 0.04D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, resMelt);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
                return;
            } else if (attackElement == Element.ELECTRO) {
                if (!checkReactionIcd(target, ElementalReaction.SUPERCONDUCT, now)) {
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    return;
                }
                freezeGauge.consume(attackU * 1.0F);
                if (freezeGauge.isExpired()) {
                    clearFreezeState(target, targetId);
                }
                float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                executeReactionEffect(level, target, ElementalReaction.SUPERCONDUCT, Element.CRYO, attackU, attacker, dmgMult);
                enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ELECTRO, ElementalReaction.SUPERCONDUCT, resDmg);
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
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    executeReactionEffect(level, target, ElementalReaction.SWIRL, Element.CRYO, removedFreezeU, attacker, dmgMult);
                    enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                    syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.ANEMO, ElementalReaction.SWIRL, resDmg);
                    return;
                }
            }
        }

        // 3. Check Overvape (Target has both Hydro & Electro, attacked with Pyro)
        if (attackElement == Element.PYRO && targetGauges.containsKey(Element.HYDRO) && targetGauges.containsKey(Element.ELECTRO)) {
            if (!checkReactionIcd(target, ElementalReaction.OVERVAPE, now)) {
                float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                if (resDmg > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
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

            float overvapeDmg = effectiveDamage * 1.5F * ampEmMult;
            float resOvervape = dealOrDeferDamage(level, target, attacker, overvapeDmg, attackElement, meleeOut);
            triggerOverloadExplosion(level, target, attacker, dmgMult);
            notifyResonanceReaction(ElementalReaction.OVERVAPE, attacker, now);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
            enforceInnateLock(target, targetGauges, targetId, innateElement, now);
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, attackElement, ElementalReaction.OVERVAPE, resOvervape);
                    if (attacker != null) com.velorise.staves.advancement.ModAdvancements.trigger(attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
            return;
        }

        // 4. Reaction Resolution with Simultaneous Reaction Priority (Electro -> Pyro -> Hydro -> Cryo -> Dendro)
        Element auraElement = findPrimaryReactiveAura(targetGauges, attackElement);
        if (auraElement != null && auraElement != attackElement) {
            ElementalReaction reaction = determineReaction(auraElement, attackElement);
            if (reaction != ElementalReaction.NONE) {
                boolean alreadyBurning = reaction == ElementalReaction.BURNING && BURNING_UNTIL.containsKey(targetId);
                if (!alreadyBurning && !checkReactionIcd(target, reaction, now)) {
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (effectiveDamage > 0.0F) syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    return;
                }
                GaugeState aura = targetGauges.get(auraElement);

                // Electro-Charged Coexistence: both HYDRO and ELECTRO coexist on normal targets;
                // Trigger aura is created with standard 0.8x Aura Tax, then initial EC tick consumes 0.4U from both.
                if (reaction == ElementalReaction.ELECTRO_CHARGED) {
                    if (innateElement == null) {
                        GaugeState triggerState = targetGauges.get(attackElement);
                        if (triggerState == null) {
                            triggerState = createAuraGauge(target, attackElement, attackU);
                            targetGauges.put(attackElement, triggerState);
                        } else {
                            refreshAuraGauge(target, triggerState, attackElement, attackU);
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

                    ELECTRO_CHARGE_EM_LEVEL.put(targetId, emLevel);
                    float ecDmg = effectiveDamage + (float) (1.0F * dmgMult) * transEmMult;
                    float resEc = dealOrDeferDamage(level, target, attacker, ecDmg, attackElement, meleeOut);
                    executeReactionEffect(level, target, reaction, auraElement, 0.4F, attacker, dmgMult);
                    ELECTRO_CHARGE_TICK.put(targetId, 0);
                    syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, resEc);
                    return;
                }

                // Burning Coexistence: maintains 2.0U Burning Pyro state and consumes Dendro/Quicken at 0.4U/s until depleted
                if (reaction == ElementalReaction.BURNING) {
                    boolean immuneToBurn = innateElement == Element.PYRO || target.fireImmune() || target instanceof Warden;
                    if (!immuneToBurn) {
                        long burnTicks = ElementalResonanceManager.getBurningDurationTicksForTarget(
                                target, (innateElement == Element.DENDRO) ? 100L : 200L);
                        BURNING_UNTIL.put(targetId, now + burnTicks);
                        BURNING_EM_LEVEL.put(targetId, emLevel);
                        if (innateElement == null || innateElement == Element.DENDRO || innateElement == Element.GEO) {
                            targetGauges.put(Element.PYRO, GaugeState.createBurning(2.0F));
                            if (attackElement == Element.DENDRO && innateElement != Element.DENDRO) {
                                if (targetGauges.containsKey(Element.DENDRO)) {
                                    refreshAuraGauge(target, targetGauges.get(Element.DENDRO), Element.DENDRO, attackU);
                                } else {
                                    targetGauges.put(Element.DENDRO, createAuraGauge(target, Element.DENDRO, attackU));
                                }
                            }
                        }
                        LAST_BURN_PYRO_REFRESH.put(targetId, now);
                        target.setSecondsOnFire(5);
                    }
                    enforceInnateLock(target, targetGauges, targetId, innateElement, now);
                    float resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                    if (!alreadyBurning) {
                        executeReactionEffect(level, target, reaction, auraElement, 0.4F, attacker, dmgMult);
                        syncAuras(target, targetGauges.keySet(), auraElement, attackElement, reaction, resDmg);
                    } else {
                        syncAuras(target, targetGauges.keySet(), null, attackElement, null, resDmg);
                    }
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
                        applyFreezeState(target, targetId, newFreeze, freezeTicks, now);
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
                                        applyFreezeState(target, targetId, newFreeze2, freezeTicks2, now);
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

                float reactionMult = reaction.getDamageMultiplier();
                if (reactionMult > 1.0F) {
                    reactionMult *= ampEmMult;
                }
                float finalDmg = effectiveDamage * reactionMult;
                float resFinalDmg = 0.0F;
                if (finalDmg > 0.0F) {
                    resFinalDmg = dealOrDeferDamage(level, target, attacker, finalDmg, attackElement, meleeOut);
                }
                executeReactionEffect(level, target, reaction, auraElement, removedAuraU, attacker, dmgMult);

                ElementalReaction displayReaction = (catalyzeReaction != null && reaction == ElementalReaction.QUICKEN)
                        ? catalyzeReaction : reaction;
                syncAuras(target, targetGauges.keySet(), auraElement, attackElement, displayReaction, resFinalDmg);
                return;
            }
        }

        // 5. No Reaction: Apply or Refresh Aura (Aura elements only!)
        if (attackElement.isAura()) {
            if (attackElement == Element.PYRO && isEntityInWater(target)) {
                target.clearFire();
                float vapeDmg = effectiveDamage * 1.5F * ampEmMult;
                float resVape = dealOrDeferDamage(level, target, attacker, vapeDmg, attackElement, meleeOut);
                syncAuras(target, targetGauges.keySet(), Element.HYDRO, Element.PYRO, ElementalReaction.REVERSE_VAPORIZE, resVape);
                return;
            }

            if (innateElement != null && attackElement != innateElement) {
                float resDmg = 0.0F;
                if (effectiveDamage > 0.0F) {
                    resDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
                }
                syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, resDmg);
                return;
            }

            if (targetGauges.containsKey(attackElement)) {
                GaugeState existing = targetGauges.get(attackElement);
                refreshAuraGauge(target, existing, attackElement, attackU);
            } else {
                GaugeState newAura = createAuraGauge(target, attackElement, attackU);
                targetGauges.put(attackElement, newAura);
            }
            float previewResDmg = (meleeOut != null)
                    ? effectiveDamage * getEffectivePhysicalMultiplier(target, now)
                    : applyMobElementalResistance(target, attackElement, effectiveDamage);
            syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, previewResDmg);
            if (effectiveDamage > 0.0F) {
                dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
            }
            return;
        }

        // Deal normal damage
        float resNormalDmg = 0.0F;
        if (effectiveDamage > 0.0F) {
            resNormalDmg = dealOrDeferDamage(level, target, attacker, effectiveDamage, attackElement, meleeOut);
        }
        syncAuras(target, targetGauges.keySet(), null, attackElement, catalyzeReaction, resNormalDmg);
    }

    private static void enforceInnateLock(
            @Nullable LivingEntity target,
            Map<Element, GaugeState> targetGauges,
            UUID targetId,
            @Nullable Element innateElement,
            long now
    ) {
        if (innateElement == null) return;
        boolean isBurning = target != null && (target.isOnFire() || target.getRemainingFireTicks() > 0 || target.isInLava() || BURNING_UNTIL.containsKey(targetId));
        // Allow DENDRO to coexist on GEO innate creatures, and allow PYRO when actively burning on fire/lava
        targetGauges.keySet().removeIf(e -> e != innateElement
                && !(innateElement == Element.GEO && e == Element.DENDRO)
                && !(isBurning && e == Element.PYRO));
        if ((innateElement.isAura() || isMutatedInnate(target)) && !targetGauges.containsKey(innateElement)) {
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
        if (attacker != null && reaction != ElementalReaction.NONE && reaction != ElementalReaction.IMMUNE) {
            com.velorise.staves.advancement.ModAdvancements.trigger(
                    attacker, com.velorise.staves.advancement.ModAdvancements.EVENT_FIRST_REACTION);
            notifyResonanceReaction(reaction, attacker, level.getGameTime());
        }
        Vec3 pos = target.position();
        int emLevel = getElementalMasteryLevel(attacker);
        float transEmMult = getTransformativeMultiplier(emLevel);
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
                    if (isOmniInnate(nearby)) {
                        notifyOmniImmuneEnvironmental(level, nearby);
                        continue;
                    }
                    float dmg = applyMobElementalResistance(nearby, Element.CRYO,
                            (float) (1.0F * dmgMult) * transEmMult); // 0.5 heart Cryo AoE damage scaled by EM
                    if (nearby instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, level.getGameTime()) == Element.CRYO) {
                        dmg *= 0.4F;
                    }
                    nearby.invulnerableTime = 0;
                    hurtWithPreAppliedElementalRes(nearby, level.damageSources().freeze(), dmg);
                    SUPERCONDUCT_UNTIL.put(nearby.getUUID(), debuffEnd);
                    nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0));
                    if (nearby != target && dmg > 0.0F) {
                        syncAuras(nearby, getEntityElements(nearby.getUUID()), Element.CRYO, Element.ELECTRO, ElementalReaction.SUPERCONDUCT, dmg);
                    }
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
                if (lastSpawn == null || now < lastSpawn || (now - lastSpawn) >= 10L) { // 0.5s ICD per target
                    DendroCoreManager.spawn(level, target.position(), attacker, (float) dmgMult * transEmMult);
                    LAST_BLOOM_SPAWN.put(target.getUUID(), now);
                }
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.8D, pos.z, 25, 0.8D, 0.5D, 0.8D, 0.04D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 1.1F);
            }
            case BURNING -> {
                if (getInnateElement(target) != Element.PYRO && !target.fireImmune() && !(target instanceof Warden)) {
                    long burnTicks = ElementalResonanceManager.getBurningDurationTicksForTarget(
                            target, getInnateElement(target) == Element.DENDRO ? 100L : 200L);
                    BURNING_UNTIL.put(target.getUUID(), level.getGameTime() + burnTicks);
                    BURNING_EM_LEVEL.put(target.getUUID(), emLevel);
                    EnvironmentalBlockManager.applyPyroEnvironmentEffects(level, pos, 1.5D);
                    target.setSecondsOnFire(5);
                    level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.8D, pos.z, 15, 0.3D, 0.4D, 0.3D, 0.03D);
                }
            }
            case SWIRL -> {
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.1F);
                AABB box = target.getBoundingBox().inflate(5.0D);
                // Genshin Impact rule: Pyro, Electro, Cryo Swirls deal AoE damage + spread element; Hydro Swirl only spreads Wet aura!
                // Spread gauge formula: U_Swirl = (U_removed - 0.04) * 1.25 + 1.5
                float swirlSpreadU = Math.max(0.5F, (Math.max(0.04F, removedAuraU) - 0.04F) * 1.25F + 1.5F);
                if (auraElement == Element.PYRO) {
                    EnvironmentalBlockManager.applyPyroEnvironmentEffects(level, pos, 3.5D);
                }
                float swirlAoeDmg = (auraElement == Element.HYDRO) ? 0.0F : (float) (4.0F * dmgMult) * transEmMult;
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != target && e != attacker)) {
                    applyElement(level, nearby, auraElement, swirlSpreadU, attacker, swirlAoeDmg);
                }
            }
            case CRYSTALLIZE -> {
                double popY = target.getY() + target.getBbHeight() * 0.80D;
                level.sendParticles(ParticleTypes.CRIT, target.getX(), popY, target.getZ(), 15, 0.35D, 0.35D, 0.35D, 0.05D);
                level.playSound(null, target.getX(), popY, target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.4F);
                CrystallizeShardManager.spawn(level, target, auraElement, target.getUUID(), emLevel);
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
        float transEmMult = getTransformativeMultiplier(getElementalMasteryLevel(attacker));
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
            if (isOmniInnate(victim)) {
                notifyOmniImmuneEnvironmental(level, victim);
                continue;
            }
            float dmg = applyMobElementalResistance(victim, Element.PYRO, (float) (6.0F * dmgMult) * transEmMult);
            if (victim instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, level.getGameTime()) == Element.PYRO) {
                dmg *= 0.4F;
            }
            ensurePreDeathPyroIgnite(victim, 3);
            victim.invulnerableTime = 0;
            hurtWithPreAppliedElementalRes(victim, getElementalDamageSource(level, attacker), dmg);
            if (victim != target && dmg > 0.0F) {
                syncAuras(victim, getEntityElements(victim.getUUID()), Element.ELECTRO, Element.PYRO, ElementalReaction.OVERLOADED, dmg);
            }
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
        MobHazardAvoidanceManager.tick(server);

        long serverNow = server.overworld().getGameTime();
        if (serverNow % 100 == 0) {
            ICD_TRACKER.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue().firstHitTime) > 100L);
            REACTION_ICD_TRACKER.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            SUPERCONDUCT_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L);
            INNATE_COOLDOWN_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 200L);
            LAST_PROXIMITY_SHOCK.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 40L);
            LAST_BLOOM_SPAWN.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 40L);
            LAST_THORNS_REFLECT.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            LAST_OMNI_ENV_IMMUNE.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            LAST_MUSHROOM_CONTACT.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            LAST_FREEZE_STRUGGLE.entrySet().removeIf(e -> Math.abs(serverNow - e.getValue()) > 100L);
            PLAYER_HP_CHANGE_BUFF_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 400L);
            QUICKEN_UNTIL.entrySet().removeIf(e -> !QUICKEN_GAUGES.containsKey(e.getKey()) && (serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L));
            FROZEN_UNTIL.entrySet().removeIf(e -> !FROZEN_GAUGES.containsKey(e.getKey()) && (serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L));
            BURNING_UNTIL.entrySet().removeIf(e -> !GAUGES.containsKey(e.getKey()) && (serverNow >= e.getValue() || (e.getValue() - serverNow) > 600L));
            BURNING_EM_LEVEL.keySet().removeIf(id -> !BURNING_UNTIL.containsKey(id));
            LAST_BURN_PYRO_REFRESH.keySet().removeIf(id -> !BURNING_UNTIL.containsKey(id));
            ELECTRO_CHARGE_EM_LEVEL.keySet().removeIf(id -> !GAUGES.containsKey(id));
            ELECTRO_CHARGE_TICK.keySet().removeIf(id -> !GAUGES.containsKey(id));
            FROZEN_POSES.keySet().removeIf(id -> !FROZEN_GAUGES.containsKey(id) && !FROZEN_UNTIL.containsKey(id));
        }
        if (serverNow % 600 == 0 && !OMNI_INNATE_ENTITIES.isEmpty()) {
            OMNI_INNATE_ENTITIES.removeIf(id -> {
                for (ServerLevel lvl : server.getAllLevels()) {
                    Entity e = lvl.getEntity(id);
                    if (e instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                        return false;
                    }
                }
                return true;
            });
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
                FROZEN_POSES.remove(id);
                LAST_FREEZE_STRUGGLE.remove(id);
                QUICKEN_UNTIL.remove(id);
                BURNING_UNTIL.remove(id);
        BURNING_EM_LEVEL.remove(id);
        ELECTRO_CHARGE_EM_LEVEL.remove(id);
                LAST_BURN_PYRO_REFRESH.remove(id);
                INNATE_COOLDOWN_UNTIL.remove(id);
                ELECTRO_CHARGE_TICK.remove(id);
                LAST_BLOOM_SPAWN.remove(id);
                SUPERCONDUCT_UNTIL.remove(id);
        OMNI_INNATE_ENTITIES.remove(id);
        LAST_OMNI_ENV_IMMUNE.remove(id);
        LAST_MUSHROOM_CONTACT.remove(id);
        PLAYER_LAST_HP.remove(id);
        PLAYER_HP_CHANGE_BUFF_UNTIL.remove(id);
                continue;
            }

            long now = targetLevel.getGameTime();
            Map<Element, GaugeState> entityGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            Element innate = getInnateElement(target);
            boolean changed = false;

            if (innate == Element.PYRO) {
                extinguishPyroInnate(target);
            }

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

            // Powered Creeper (special lightning creature): radiates electric shock within 1.0 block in air & water once per second
            if (target instanceof Creeper creeper && creeper.isPowered()) {
                if (now % 20 == 0) {
                    final LivingEntity electroSource = target;
                    AABB auraField = electroSource.getBoundingBox().inflate(1.0D);
                    for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, auraField,
                            e -> e.isAlive() && !e.isRemoved() && e != electroSource && !e.isSpectator()
                                    && !(e instanceof Player p && p.isCreative())
                                    && getInnateElement(e) != Element.ELECTRO)) {
                        drawVoltaicArc(targetLevel, electroSource, nearby);
                        applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 1.0F);
                    }

                    if (isEntitySubmergedInWater(electroSource)) {
                        ElectrifiedWaterManager.electrify(targetLevel, electroSource.blockPosition(), 1.0D, 30, null, 1.0F);
                    }
                }
            } else if (innate == Element.ELECTRO && isMutatedInnate(target)) {
                // Mutated Electro mob: does NOT radiate Electro into the air; only electrifies the 1 single water block it touches
                if (now % 20 == 0 && isEntitySubmergedInWater(target)) {
                    ElectrifiedWaterManager.electrifySingleBlock(targetLevel, target.blockPosition(), 30, null, 1.0F);
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

            // Tick independent Freeze gauge (Genshin Statue Freeze: lock AI, position, and rotation)
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
                    enforceFrozenPose(target, id);
                }
            } else if (frozenEnd != null) {
                if (now >= frozenEnd) {
                    clearFreezeState(target, id);
                    changed = true;
                } else {
                    enforceFrozenPose(target, id);
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
                        if (lastShock == null || now < lastShock || (now - lastShock) >= 20) {
                            LAST_PROXIMITY_SHOCK.put(pairKey, now);
                            drawVoltaicArc(targetLevel, target, nearby);
                            targetLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
                            applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 1.0F);
                            if (innate != Element.ELECTRO) {
                                float ecDmg = applyMobElementalResistance(target, Element.ELECTRO, 1.0F);
                                hurtWithPreAppliedElementalRes(target, targetLevel.damageSources().magic(), ecDmg);
                                syncAuras(target, entityGauges.keySet(), Element.ELECTRO, Element.HYDRO, ElementalReaction.ELECTRO_CHARGED, ecDmg);
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
                    int ecEmLvl = ELECTRO_CHARGE_EM_LEVEL.getOrDefault(id, 0);
                    float ecDmg = applyMobElementalResistance(target, Element.ELECTRO,
                            1.0F * getTransformativeMultiplier(ecEmLvl));
                    if (target instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, targetLevel.getGameTime()) == Element.ELECTRO) {
                        ecDmg *= 0.4F;
                    }
                    if (innate != Element.ELECTRO) {
                        hurtWithPreAppliedElementalRes(target, targetLevel.damageSources().magic(), ecDmg);
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
                Long burningEnd = BURNING_UNTIL.get(id);
                boolean hasDendroFuel = entityGauges.containsKey(Element.DENDRO) || QUICKEN_GAUGES.containsKey(id) || innate == Element.DENDRO;
                boolean hasPyro = entityGauges.containsKey(Element.PYRO) || innate == Element.PYRO;
                boolean innateDendroExpired = (innate == Element.DENDRO && burningEnd != null && now >= burningEnd);
                if (!hasDendroFuel || !hasPyro || innateDendroExpired || target.isInWaterOrRain() || target.fireImmune() || target instanceof Warden) {
                    BURNING_UNTIL.remove(id);
                    BURNING_EM_LEVEL.remove(id);
                    LAST_BURN_PYRO_REFRESH.remove(id);
                    if (innate == Element.DENDRO) {
                        entityGauges.remove(Element.PYRO);
                        if (!target.isInLava()) {
                            target.clearFire();
                        }
                    }
                    changed = true;
                } else if (now % 5 == 0) {
                    int burnEmLvl = BURNING_EM_LEVEL.getOrDefault(id, 0);
                    float burnDmg = applyMobElementalResistance(target, Element.PYRO,
                            0.75F * getTransformativeMultiplier(burnEmLvl)); // 0.75 per 0.25s = 3.0 DMG/s scaled by EM
                    if (target instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, now) == Element.PYRO) {
                        burnDmg *= 0.4F;
                    }
                    IS_PROCESSING_REACTION.set(true);
                    try {
                        hurtWithPreAppliedElementalRes(target, targetLevel.damageSources().inFire(), burnDmg);
                    } finally {
                        IS_PROCESSING_REACTION.set(false);
                    }
                    if (!target.isAlive() && burnDmg > 0.0F) {
                        syncAuras(target, Collections.emptyList(), Element.DENDRO, Element.PYRO, ElementalReaction.BURNING, burnDmg);
                    } else if (target.isAlive() && burnDmg > 0.0F) {
                        MobHazardAvoidanceManager.reportReactionOrHazard(target, null, target.position(), Element.DENDRO, Element.PYRO);
                    }
                    if (innate != Element.DENDRO) {
                        target.setSecondsOnFire(2);
                    }

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
                    if ((innate == null || innate == Element.GEO) && (lastPyroRefresh == null || now < lastPyroRefresh || (now - lastPyroRefresh) >= 40L)) {
                        LAST_BURN_PYRO_REFRESH.put(id, now);
                        if (entityGauges.containsKey(Element.PYRO)) {
                            entityGauges.get(Element.PYRO).refresh(1.0F);
                        } else {
                            entityGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.0F));
                        }
                    }

                    if (!entityGauges.containsKey(Element.DENDRO) && !QUICKEN_GAUGES.containsKey(id) && innate != Element.DENDRO) {
                        BURNING_UNTIL.remove(id);
                        BURNING_EM_LEVEL.remove(id);
                        LAST_BURN_PYRO_REFRESH.remove(id);
                        changed = true;
                    }
                    targetLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 4, 0.25D, 0.3D, 0.25D, 0.02D);
                }
            }

            // 5. Cryo Aura passive debuff (-15% Movement Speed, -15% Attack Speed) when not Statue-Frozen
            boolean hasUnfrozenCryo = entityGauges.containsKey(Element.CRYO) && !isEntityFrozen(target);
            updateCryoDebuff(target, id, hasUnfrozenCryo, innate);

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

        // 7. Environmental water, fire, lingering potion clouds & cryo checks on active online players
        Set<UUID> visitedNearby = new HashSet<>();
        Set<UUID> visitedClouds = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isDeadOrDying()) {
                clearEntityState(player);
                continue;
            }
            updatePlayerHpTracking(player, player.level().getGameTime());
            ElementalResonanceManager.tickPlayer(player);
            if (!player.isSpectator()) {
                if (isEntityInWater(player)) {
                    handleEnvironmentalWater(player);
                } else if (player.isOnFire() || player.getRemainingFireTicks() > 0 || player.isInLava() || isTouchingSoulCampfire(player)) {
                    maintainBurningPyroAura(player);
                }
                Collection<Element> playerElements = getEntityElements(player.getUUID());
                updateCryoDebuff(player, player.getUUID(), playerElements.contains(Element.CRYO) && !isEntityFrozen(player), null);

                if (player.level() instanceof ServerLevel sl) {
                    if (serverNow % 5 == 0) {
                        tryApplyMushroomContactAura(sl, player, sl.getGameTime());
                    }
                    AABB searchBox = player.getBoundingBox().inflate(32.0D);

                    // Lingering Potion AreaEffectCloud elemental application (every 20 ticks = 1.0s)
                    if (serverNow % 20 == 0) {
                        for (AreaEffectCloud cloud : sl.getEntitiesOfClass(AreaEffectCloud.class, searchBox, e -> e.isAlive() && !e.isRemoved())) {
                            if (!visitedClouds.add(cloud.getUUID())) {
                                continue;
                            }
                            if (cloud.getTags().contains("velorise_staves:dragonfire_cloud")
                                    || cloud.getTags().contains("velorise_staves:ghastfire_field")) {
                                continue;
                            }
                            List<Element> cloudElements = getElementsFromAreaEffectCloud(sl, cloud);
                            if (!cloudElements.isEmpty()) {
                                ServerPlayer cloudOwner = cloud.getOwner() instanceof ServerPlayer sp ? sp : null;
                                for (LivingEntity inCloud : sl.getEntitiesOfClass(LivingEntity.class, cloud.getBoundingBox(),
                                        e -> e.isAlive() && !e.isRemoved() && !e.isSpectator())) {
                                    for (Element elem : cloudElements) {
                                        applyElementBypassIcd(sl, inCloud, elem, 1.0F, cloudOwner, 0.0F);
                                    }
                                }
                            }
                        }
                    }

                    for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !e.isRemoved())) {
                        if (!visitedNearby.add(nearby.getUUID())) {
                            continue;
                        }
                        if (nearby instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE) && !isEntityFrozen(mob)) {
                            mob.setNoAi(false);
                            mob.removeTag(TAG_FROZEN_STATUE);
                        }
                        Element nearbyInnate = getInnateElement(nearby);
                        if (nearbyInnate == Element.CRYO || hasElement(nearby, Element.CRYO)) {
                            checkCryoWaterFreezing(nearby);
                        }
                        if (nearbyInnate != null && serverNow % 20 == 0) {
                            if (isEntityInWater(nearby)) {
                                handleEnvironmentalWater(nearby);
                            } else if (nearby.isOnFire() || nearby.getRemainingFireTicks() > 0 || nearby.isInLava() || isTouchingSoulCampfire(nearby)) {
                                maintainBurningPyroAura(nearby);
                            }
                        }
                        if (serverNow % 5 == 0 && !isPassiveAquaticOrAmbient(nearby)) {
                            tryApplyMushroomContactAura(sl, nearby, sl.getGameTime());
                        }
                        if (isOmniInnate(nearby)) {
                            OMNI_INNATE_ENTITIES.add(nearby.getUUID());
                            extinguishPyroInnate(nearby);
                            if (serverNow % 40 == 0) {
                                com.velorise.staves.network.ModNetwork.sendAuraSync(nearby, Collections.emptyList(), null, null, null, 0.0F, -2);
                            }
                            continue;
                        }
                        // Ensure Powered Creeper and ALL Mutated Mobs stay registered in GAUGES and synced to clients so their overhead icon is always visible
                        if ((nearby instanceof Creeper creeper && creeper.isPowered())
                                || (nearbyInnate != null && isMutatedInnate(nearby))) {
                            Element innateToKeep = (nearby instanceof Creeper c && c.isPowered()) ? Element.ELECTRO : nearbyInnate;
                            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(nearby.getUUID(), k -> new ConcurrentHashMap<>());
                            if (!gauges.containsKey(innateToKeep)) {
                                gauges.put(innateToKeep, new GaugeState(innateToKeep, 1.25F));
                                syncAuras(nearby, gauges.keySet(), null, null, null);
                            } else if (serverNow % 40 == 0) {
                                syncAuras(nearby, gauges.keySet(), null, null, null);
                            }
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

    /** Renders a horizontal expanding square perimeter wave used by lingering AoE field zones. */
    public static void spawnExpandingSquareParticles(
            ServerLevel level,
            Vec3 center,
            double halfExtent,
            double yOffset,
            ParticleOptions primary,
            @Nullable ParticleOptions accent,
            int accentStride
    ) {
        if (halfExtent <= 0.05D) return;
        int steps = Math.max(3, (int) Math.round(halfExtent * 3.5D));
        double y = center.y + yOffset;
        double[][] edges = {
                {-1.0D, -1.0D,  1.0D,  0.0D},
                { 1.0D, -1.0D,  0.0D,  1.0D},
                { 1.0D,  1.0D, -1.0D,  0.0D},
                {-1.0D,  1.0D,  0.0D, -1.0D}
        };
        int idx = 0;
        for (double[] edge : edges) {
            for (int i = 0; i < steps; i++) {
                double frac = (2.0D * i) / steps;
                double px = center.x + (edge[0] + edge[2] * frac) * halfExtent;
                double pz = center.z + (edge[1] + edge[3] * frac) * halfExtent;
                level.sendParticles(primary, px, y, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (accent != null && accentStride > 0 && idx % accentStride == 0) {
                    level.sendParticles(accent, px, y + 0.05D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                idx++;
            }
        }
    }
}
