package com.velorise.staves.element;

import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
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
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
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

public final class ZhongliManager {
    private ZhongliManager() {}

    public static final java.util.Map<UUID, PendingEvokerConstruct> PENDING_EVOKER_CONSTRUCTS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.List<NightsFocusStarProjectile> NIGHTS_FOCUS_PROJECTILES = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static final class ZhongliIdleState {
        Vec3 lastStillPos = Vec3.ZERO;
        int stillTicks = 0;
        boolean idleMeteorActive = false;
        int lastSyncedCode = -1;
    }

    private static final Map<UUID, ZhongliIdleState> ZHONGLI_IDLE_STATES = new ConcurrentHashMap<>();

    private static boolean isHoldingZhongli(@Nullable Player player) {
        if (player == null) return false;
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.VORTEX) return true;
        if (ArcaneStaffItem.isGenshinCharacterWeapon(main)) return false;
        return ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.VORTEX;
    }

    public static void tick(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            ZhongliIdleState st = ZHONGLI_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new ZhongliIdleState());
            boolean holdingZhongli = isHoldingZhongli(player);
            if (st.lastStillPos == null || st.lastStillPos.equals(Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distSq = player.position().distanceToSqr(st.lastStillPos);
            boolean canIdle = holdingZhongli && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting() && player.getVehicle() == null;
            if (canIdle && distSq < 0.0025D) {
                st.stillTicks++;
                if (st.stillTicks >= 60 && !st.idleMeteorActive) {
                    st.idleMeteorActive = true;
                }
            } else {
                st.stillTicks = 0;
                st.lastStillPos = player.position();
                st.idleMeteorActive = false;
            }
            int code = st.idleMeteorActive ? 4011 : 4010;
            if (code != st.lastSyncedCode || (st.idleMeteorActive && now % 20L == 0L)) {
                st.lastSyncedCode = code;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, code);
            }
        }
    }

    public static void clearAll() {
        PENDING_EVOKER_CONSTRUCTS.clear();
        NIGHTS_FOCUS_PROJECTILES.clear();
        ZHONGLI_IDLE_STATES.clear();
    }

    private record PendingEvokerConstruct(
            ServerLevel level, UUID evokerId, long tick,
            double x, double y, double z, float yawDeg, int maxWarmup
    ) {}



    public static boolean isPhysicalDamage(DamageSource source) {
        String msgId = source.type().msgId();
        if ("wind_charge".equals(msgId) || "sonic_boom".equals(msgId) || "velorise_elemental".equals(msgId)) return false;
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

    public static DamageSource getElementalDamageSource(ServerLevel level, @Nullable LivingEntity attacker) {
        return getElementalDamageSource(level, attacker, null, null);
    }

    public static DamageSource getElementalDamageSource(
            ServerLevel level,
            @Nullable LivingEntity attacker,
            @Nullable ElementalReaction reaction,
            @Nullable Element element
    ) {
        LivingEntity credit = (attacker != null && !Boolean.TRUE.equals(com.velorise.staves.entity.GeoFangPillarEntity.IS_GEO_PULSE.get()))
                ? attacker
                : (!Boolean.TRUE.equals(com.velorise.staves.entity.GeoFangPillarEntity.IS_GEO_PULSE.get())
                    ? CURRENT_REACTION_ATTACKER.get()
                    : null);
        return ElementalDeathMessageHelper.createDamageSource(level, credit, reaction, element);
    }

    public static boolean isGeoInnateMob(@Nullable Entity entity) {
        return entity instanceof Mob mob && hasInnateElement(mob, Element.GEO);
    }

    public static boolean isAnemoInnateMob(@Nullable Entity entity) {
        return entity instanceof Mob mob && hasInnateElement(mob, Element.ANEMO);
    }

    public static boolean isProjectileDamageSource(@Nullable DamageSource source) {
        return source != null && (source.is(DamageTypeTags.IS_PROJECTILE)
                || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile);
    }

    public static boolean isFallDamageImmunityApplicable(@Nullable LivingEntity entity, @Nullable DamageSource source) {
        return entity != null && source != null
                && source.is(DamageTypeTags.IS_FALL)
                && !source.is(DamageTypes.STALAGMITE)
                && isAnemoInnateMob(entity);
    }

    public static void beginDamageFloatTracking(@Nullable LivingEntity entity) {
        if (entity != null && !entity.level().isClientSide()) {
            SUPPRESSED_PHYSICAL_FLOAT_TICK.remove(entity.getUUID());
        }
    }

    public static void trySendPhysicalDamageFloat(@Nullable LivingEntity target, @Nullable DamageSource source, float finalDamage) {
        if (target == null || source == null || target.level().isClientSide()) return;
        Long suppressed = SUPPRESSED_PHYSICAL_FLOAT_TICK.remove(target.getUUID());
        if (finalDamage <= 0.05F) return;
        try {
            if (!StavesConfig.SHOW_DAMAGE_NUMBERS.get()) return;
        } catch (Exception ignored) {}
        if (suppressed != null && suppressed == target.level().getGameTime()) return;
        if (Boolean.TRUE.equals(IS_PROCESSING_REACTION.get()) || Boolean.TRUE.equals(ELEMENTAL_RES_APPLIED.get())) return;
        if (source instanceof ElementalDeathMessageHelper.ElementalReactionDamageSource) return;
        if (isVanillaPhysicalDisplayDamage(source)) {
            com.velorise.staves.network.ModNetwork.sendPhysicalDamage(target, finalDamage);
        }
    }

    public static boolean isVanillaPhysicalDisplayDamage(DamageSource source) {
        return isPhysicalDamage(source)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.FALLING_BLOCK)
                || source.is(DamageTypes.FALLING_STALACTITE)
                || source.is(DamageTypes.STALAGMITE);
    }

    public static float getMobElementalResistance(@Nullable LivingEntity target, @Nullable Element element) {
        if (!(target instanceof Mob)) return 0.0F;
        if (element == null) return 0.10F;

        if (element == Element.ANEMO) {
            if (target instanceof Warden) return 0.70F;
            if (target instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                    || target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                    || target instanceof net.minecraft.world.entity.animal.Parrot
                    || target instanceof net.minecraft.world.entity.ambient.Bat
                    || target instanceof net.minecraft.world.entity.animal.allay.Allay) {
                return 0.50F;
            }
        }
        if (element == Element.GEO) {
            if (target instanceof IronGolem) return 0.50F;
        }
        if (element == Element.PYRO) {
            if (target instanceof net.minecraft.world.entity.monster.Husk) return 0.30F;
            if (target instanceof net.minecraft.world.entity.animal.camel.Camel
                    || target instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin
                    || target instanceof net.minecraft.world.entity.monster.hoglin.Hoglin) {
                return 0.50F;
            }
        }
        if (element == Element.CRYO) {
            if (target instanceof net.minecraft.world.entity.animal.goat.Goat) return 0.50F;
        }
        if (element == Element.HYDRO) {
            if (target instanceof net.minecraft.world.entity.animal.AbstractFish
                    || target instanceof net.minecraft.world.entity.animal.Squid) {
                return 0.50F;
            }
        }
        return 0.10F;
    }

    public static float getMobPhysicalResistance(@Nullable LivingEntity target) {
        if (!(target instanceof Mob)) return 0.0F;
        if (target instanceof IronGolem) return 0.50F;
        if (target instanceof net.minecraft.world.entity.monster.Ravager) return 0.30F;
        return 0.10F;
    }

    public static float computeResistanceMultiplier(float res) {
        if (res < 0.0F) {
            return 1.0F - (res / 2.0F);
        }
        if (res <= 0.75F) {
            return 1.0F - res;
        }
        return 1.0F / (4.0F * res + 1.0F);
    }

    public static float getEffectivePhysicalMultiplier(LivingEntity target, long now) {
        return getEffectivePhysicalMultiplier(target, null, now);
    }

    public static float getEffectivePhysicalMultiplier(LivingEntity target, @Nullable DamageSource source, long now) {
        float res = getMobPhysicalResistance(target);
        if (source != null && (isProjectileDamageSource(source) || source.is(DamageTypeTags.IS_EXPLOSION)) && isGeoInnateMob(target)) {
            res += 0.50F;
        }
        if (hasSuperconductDebuff(target, now) || TsaritsaManager.getActivePolestarFieldAt(target.level(), target.position()) != null) {
            res -= 0.40F;
        }
        if (isNearJadeShieldAura(target)) {
            res -= 0.20F; // Zhongli Jade Shield: -20% Physical RES within 4.5m
        }
        return computeResistanceMultiplier(res);
    }

    public static float getEffectiveElementalResistance(@Nullable LivingEntity target, @Nullable Element element, long now) {
        float res = getMobElementalResistance(target, element);
        if (target != null && element == Element.GEO && ElementalResonanceManager.hasGeoResShred(target, now)) {
            res -= 0.20F;
        }
        if (isNearJadeShieldAura(target)) {
            res -= 0.20F; // Zhongli Jade Shield: -20% All Elemental RES within 4.5m
        }
        res -= XilonenManager.getXilonenResShred(target, element, now);
        return res;
    }

    public static float applyMobElementalResistance(@Nullable LivingEntity target, @Nullable Element element, float damage) {
        if (damage <= 0.0F || !(target instanceof Mob)) return damage;
        long now = target.level().getGameTime();
        float res = getEffectiveElementalResistance(target, element, now);
        return damage * computeResistanceMultiplier(res);
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
        if (entity instanceof ServerPlayer sp && RaidenManager.isRaidenMusouActive(sp.getUUID(), entity.level().getGameTime())) {
            if (source instanceof ElementalDeathMessageHelper.ElementalReactionDamageSource erds) {
                if (erds.getReaction() == ElementalReaction.ELECTRO_CHARGED) {
                    return 0.0F;
                }
            }
        }
        if (isFallDamageImmunityApplicable(entity, source)) {
            entity.fallDistance = 0.0F;
            return 0.0F;
        }
        if (source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypes.STALAGMITE)) {
            amount = PlungeAndGliderManager.modifyPlungeOrGliderFallDamage(entity, amount);
            if (amount <= 0.0F) {
                entity.fallDistance = 0.0F;
                return 0.0F;
            }
        }
        long now = entity.level().getGameTime();
        boolean isGeoProjOrExp = isGeoInnateMob(entity) && (source.is(DamageTypeTags.IS_EXPLOSION)
                || isProjectileDamageSource(source)
                || (source instanceof ElementalDeathMessageHelper.ElementalReactionDamageSource erds && erds.isExplosionReaction()));
        if (isPhysicalDamage(source)) {
            float mult = getEffectivePhysicalMultiplier(entity, source, now);
            if (hasSuperconductDebuff(entity, now) && entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.05D);
            }
            return amount * mult;
        }
        if (!Boolean.TRUE.equals(ELEMENTAL_RES_APPLIED.get()) && isElementalOrMagicDamage(source)) {
            Element elem = (source instanceof ElementalDeathMessageHelper.ElementalReactionDamageSource erds && erds.getElement() != null)
                    ? erds.getElement()
                    : inferElementFromDamageSource(source);
            float elemRes = getEffectiveElementalResistance(entity, elem, now);
            if (isGeoProjOrExp) {
                elemRes += 0.50F;
            }
            float afterMobRes = entity instanceof Mob ? amount * computeResistanceMultiplier(elemRes) : amount;
            return applyElementalProtectionAndArmorWear(entity, source, afterMobRes);
        }
        if (isGeoProjOrExp) {
            amount *= 0.50F;
        }
        return applyElementalProtectionAndArmorWear(entity, source, amount);
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

    public static void enforceFrozenPoseIfFrozen(LivingEntity target) {
        if (target != null && isEntityFrozen(target)) {
            enforceFrozenPose(target, target.getUUID());
        }
    }

    public static void applyFreezeState(LivingEntity target, UUID targetId, GaugeState newFreeze, int freezeTicks, long now) {
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

    public static void enforceFrozenPose(LivingEntity target, UUID id) {
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
            mob.setTarget(null);
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

    public static int getFrozenRemainingTicks(UUID id, long now) {
        int rem = 0;
        GaugeState fg = FROZEN_GAUGES.get(id);
        if (fg != null && !fg.isExpired()) {
            rem = fg.getRemainingTicks();
        }
        Long frozenEnd = FROZEN_UNTIL.get(id);
        if (frozenEnd != null && now < frozenEnd) {
            int endRem = (int) (frozenEnd - now);
            rem = rem > 0 ? Math.min(rem, endRem) : endRem;
        }
        return Math.max(0, rem);
    }

    public static void tickEntityFreezeGuard(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        UUID id = entity.getUUID();
        if (isEntityFrozen(entity)) {
            enforceFrozenPose(entity, id);
        } else if (entity instanceof Mob mob && (mob.getTags().contains(TAG_FROZEN_STATUE) || FROZEN_POSES.containsKey(id))) {
            clearFreezeState(entity, id);
        }
    }

    public static void clearFreezeState(LivingEntity target, UUID id) {
        boolean hadFreeze = FROZEN_GAUGES.remove(id) != null
                || FROZEN_UNTIL.remove(id) != null
                || PETRIFIED_UNTIL.remove(id) != null
                || FROZEN_POSES.remove(id) != null;
        LAST_FREEZE_STRUGGLE.remove(id);
        target.setTicksFrozen(0);
        MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 6) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        boolean hadTag = false;
        if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
            hadTag = true;
        }
        if ((hadFreeze || hadTag) && !target.level().isClientSide() && target.isAlive()) {
            syncAuras(target, getEntityElements(id), null, null, null, 0.0F);
        }
    }

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

    public static void setPlayerShieldAbsorption(ServerPlayer player, float targetAbsorption) {
        if (player == null) return;
        var maxAbsAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION);
        if (maxAbsAttr != null && maxAbsAttr.getValue() < targetAbsorption) {
            maxAbsAttr.setBaseValue(Math.max(maxAbsAttr.getBaseValue(), targetAbsorption));
        }
        player.setAbsorptionAmount(targetAbsorption);
    }

    public static void activateJadeShield(ServerPlayer player, boolean isNetherite) {
        if (player == null || player.level().isClientSide()) return;
        long now = player.level().getGameTime();
        JADE_SHIELD_UNTIL.put(player.getUUID(), now + 400L); // 20s Jade Shield
        JADE_SHIELD_NETHERITE.put(player.getUUID(), isNetherite);
        float maxHp = Math.max(20.0F, player.getMaxHealth());
        // Base Jade Shield capacity + 20% Golden Majesty Shield Strength bonus
        float shieldHp = ((isNetherite ? (maxHp * 0.25F + 5.4167F) : (maxHp * 0.18F + 3.9F)) * 1.20F); // 9.0 HP Basic / 12.5 HP Netherite at 20 MaxHP
        if (ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.GEO)) {
            shieldHp *= 1.15F;
        }
        com.velorise.staves.effect.ModMobEffects.removeAllShields(player);
        var geoShieldObj = com.velorise.staves.effect.ModMobEffects.getShieldForElement(Element.GEO);
        if (geoShieldObj != null) {
            player.addEffect(new MobEffectInstance(geoShieldObj, 400, isNetherite ? 1 : 0, false, false, true));
        }
        setPlayerShieldAbsorption(player, Math.max(player.getAbsorptionAmount(), shieldHp));
        CrystallizeShardManager.onShieldApplied(player, Element.GEO, 400);
    }

    public static void activateNightsFocusShield(ServerPlayer player, boolean isNetherite) {
        if (player == null || player.level().isClientSide()) return;
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        NIGHTS_FOCUS_UNTIL.put(id, now + 240L); // 12s Cryo Shield
        NIGHTS_FOCUS_NETHERITE.put(id, isNetherite);
        NIGHTS_FOCUS_STARS.put(id, 0);
        NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
        NIGHTS_FOCUS_LAST_SKILL_STAR.remove(id);
        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);

        float maxHp = Math.max(20.0F, player.getMaxHealth());
        float shieldHp = maxHp * (isNetherite ? 0.60F : 0.40F);
        if (ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.GEO)) {
            shieldHp *= 1.15F;
        }
        NIGHTS_FOCUS_BASE_HP.put(id, shieldHp);
        JADE_SHIELD_UNTIL.remove(id);

        com.velorise.staves.effect.ModMobEffects.removeAllShields(player);
        var cryoShieldObj = com.velorise.staves.effect.ModMobEffects.getShieldForElement(Element.CRYO);
        if (cryoShieldObj != null) {
            player.addEffect(new MobEffectInstance(cryoShieldObj, 240, isNetherite ? 1 : 0, false, false, true));
        }
        setPlayerShieldAbsorption(player, Math.max(player.getAbsorptionAmount(), shieldHp));
        CrystallizeShardManager.onShieldApplied(player, Element.CRYO, 240);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10);
    }

    public static void activateNightsFocusBurstStars(ServerPlayer player, boolean isNetherite) {
        if (player == null || player.level().isClientSide()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ArchonUltiManager.grantUltiIframe(player, 24);
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        if (now >= NIGHTS_FOCUS_UNTIL.getOrDefault(id, 0L)) {
            activateNightsFocusShield(player, isNetherite);
        }
        NIGHTS_FOCUS_STARS.put(id, 4);
        LAYLA_DREAMSPHERE_UNTIL.put(id, now + 240L); // 12.0s Celestial Dreamsphere
        LAYLA_DREAMSPHERE_NETHERITE.put(id, isNetherite);
        LAYLA_DREAMSPHERE_NEXT_SHOT.put(id, now + 20L);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 14);
    }

    public static void clearNightsFocusShield(ServerPlayer player, boolean playBreakSound) {
        UUID id = player.getUUID();
        NIGHTS_FOCUS_UNTIL.remove(id);
        NIGHTS_FOCUS_NETHERITE.remove(id);
        NIGHTS_FOCUS_STARS.remove(id);
        NIGHTS_FOCUS_LAST_AUTO_STAR.remove(id);
        NIGHTS_FOCUS_LAST_SKILL_STAR.remove(id);
        NIGHTS_FOCUS_BASE_HP.remove(id);
        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);
        MAVUIKA_AURA_UNTIL.remove(id);
        MAVUIKA_AURA_NETHERITE.remove(id);
        MAVUIKA_AURA_NEXT_STRIKE.remove(id);
        MAVUIKA_NIGHTSOUL_POINTS.remove(id);
        MAVUIKA_LAST_NA_FS_TICK.remove(id);
        MAVUIKA_SKILL_COOLDOWN_UNTIL.remove(id);
        RAIDEN_EYE_UNTIL.remove(id);
        RAIDEN_EYE_NETHERITE.remove(id);
        RAIDEN_EYE_NEXT_PROC.remove(id);
        RAIDEN_RESOLVE.remove(id);
        RAIDEN_MUSOU_UNTIL.remove(id);
        RAIDEN_MUSOU_NETHERITE.remove(id);
        RAIDEN_SKILL_CD_UNTIL.remove(id);
        RAIDEN_BURST_CD_UNTIL.remove(id);
        RAIDEN_IFRAME_UNTIL.remove(id);
        RAIDEN_NA_COMBO.remove(id);
        RAIDEN_LAST_MELEE_TICK.remove(id);
        RAIDEN_NEXT_SWING_TICK.remove(id);
        RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
        RAIDEN_PENDING_HITOTACHI_MULT.remove(id);
        long now = player.level().getGameTime();
        Element cryst = CrystallizeShardManager.getActiveShield(player, now);
        if (cryst == null) {
            com.velorise.staves.network.ModNetwork.sendCrystallizeShieldToPlayer(player, null, 0);
        }
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 19);
        if (playBreakSound && player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9F, 1.35F);
        }
    }

    public static boolean hasActiveNightsFocusShield(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        Long until = NIGHTS_FOCUS_UNTIL.get(player.getUUID());
        if (until == null) return false;
        long now = player.level().getGameTime();
        if (now >= until) {
            if (player instanceof ServerPlayer sp) {
                clearNightsFocusShield(sp, false);
            } else {
                NIGHTS_FOCUS_UNTIL.remove(player.getUUID());
            }
            return false;
        }
        if (!player.isCreative() && player.getAbsorptionAmount() <= 0.01F) {
            if (player instanceof ServerPlayer sp) {
                clearNightsFocusShield(sp, true);
            } else {
                NIGHTS_FOCUS_UNTIL.remove(player.getUUID());
            }
            return false;
        }
        return true;
    }

    public static void addNightsFocusStars(ServerPlayer player, int amount, long now) {
        UUID id = player.getUUID();
        int prev = NIGHTS_FOCUS_STARS.getOrDefault(id, 0);
        int next = Math.min(4, prev + amount);
        if (next > prev) {
            int gained = next - prev;
            NIGHTS_FOCUS_STARS.put(id, next);
            float baseHp = NIGHTS_FOCUS_BASE_HP.getOrDefault(id, Math.max(20.0F, player.getMaxHealth()) * 0.40F);
            float bonus = baseHp * 0.06F * gained;
            if (bonus > 0.0F && player.getAbsorptionAmount() > 0.0F) {
                setPlayerShieldAbsorption(player, Math.min(baseHp * 1.24F, player.getAbsorptionAmount() + bonus));
            }
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.5D, player.getZ(),
                        4 * gained, 0.45D, 0.3D, 0.45D, 0.02D);
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.55F, 1.4F + 0.1F * next);
            }
            com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10 + next);
        }
    }

    public static void tickNightsFocusShield(ServerPlayer player, long now) {
        UUID dsId = player.getUUID();
        long dsUntil = LAYLA_DREAMSPHERE_UNTIL.getOrDefault(dsId, 0L);
        if (now < dsUntil && player.level() instanceof ServerLevel dsLevel) {
            long nextShot = LAYLA_DREAMSPHERE_NEXT_SHOT.getOrDefault(dsId, 0L);
            if (now >= nextShot) {
                LAYLA_DREAMSPHERE_NEXT_SHOT.put(dsId, now + 30L); // Fires Starlight Slug every 1.5s (30 ticks)
                LivingEntity dsTarget = findNearestNightsFocusTarget(dsLevel, player);
                if (dsTarget != null) {
                    boolean dsNeth = LAYLA_DREAMSPHERE_NETHERITE.getOrDefault(dsId, false);
                    float slugDmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.NIGHTS_FOCUS, dsNeth) * 0.90F;
                    float slugUnits = dsNeth ? 1.0F : 0.75F;
                    Vec3 spherePos = player.position().add(0.0D, player.getBbHeight() + 1.15D, 0.0D);
                    NIGHTS_FOCUS_PROJECTILES.add(new NightsFocusStarProjectile(dsLevel, player, dsTarget, spherePos, slugDmg, slugUnits));
                    dsLevel.sendParticles(ParticleTypes.SNOWFLAKE, spherePos.x, spherePos.y, spherePos.z, 6, 0.18D, 0.18D, 0.18D, 0.03D);
                    dsLevel.sendParticles(ParticleTypes.END_ROD, spherePos.x, spherePos.y, spherePos.z, 3, 0.12D, 0.12D, 0.12D, 0.02D);
                    dsLevel.playSound(null, spherePos.x, spherePos.y, spherePos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.70F, 1.60F);
                    if (hasActiveNightsFocusShield(player)) {
                        addNightsFocusStars(player, 1, now);
                    }
                }
            }
        } else if (dsUntil > 0L) {
            LAYLA_DREAMSPHERE_UNTIL.remove(dsId);
            LAYLA_DREAMSPHERE_NETHERITE.remove(dsId);
            LAYLA_DREAMSPHERE_NEXT_SHOT.remove(dsId);
        }
        if (!hasActiveNightsFocusShield(player)) return;
        UUID id = player.getUUID();
        if (player.level() instanceof ServerLevel sl) {
            if (now % 10 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(),
                        3, 0.35D, 0.45D, 0.35D, 0.01D);
            }
            int pending = NIGHTS_FOCUS_PENDING_SHOTS.getOrDefault(id, 0);
            if (pending > 0 && now % 9 == 0) {
                if (fireSingleNightsFocusStar(sl, player)) {
                    int remaining = pending - 1;
                    if (remaining <= 0) {
                        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);
                        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10);
                    } else {
                        NIGHTS_FOCUS_PENDING_SHOTS.put(id, remaining);
                        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 10 + remaining);
                    }
                }
            }
        }

        if (NIGHTS_FOCUS_PENDING_SHOTS.getOrDefault(id, 0) > 0) {
            NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
        } else {
            long lastAuto = NIGHTS_FOCUS_LAST_AUTO_STAR.getOrDefault(id, now);
            if (now - lastAuto >= 30L) { // 1.5s per Night Star
                NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
                addNightsFocusStars(player, 1, now);
            }
        }

        int stars = NIGHTS_FOCUS_STARS.getOrDefault(id, 0);
        if (stars >= 4 && NIGHTS_FOCUS_PENDING_SHOTS.getOrDefault(id, 0) <= 0 && player.level() instanceof ServerLevel sl) {
            LivingEntity nearest = findNearestNightsFocusTarget(sl, player);
            if (nearest != null) {
                NIGHTS_FOCUS_STARS.put(id, 0);
                NIGHTS_FOCUS_LAST_AUTO_STAR.put(id, now);
                if (fireSingleNightsFocusStar(sl, player)) {
                    NIGHTS_FOCUS_PENDING_SHOTS.put(id, 3);
                    com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 13);
                } else {
                    NIGHTS_FOCUS_PENDING_SHOTS.put(id, 4);
                    com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 14);
                }
            }
        }
    }

    public static @Nullable LivingEntity findNearestNightsFocusTarget(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(16.0D);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                ent -> ent.isAlive() && !ent.isSpectator() && ent != player && !player.isAlliedTo(ent)
                        && (ent instanceof net.minecraft.world.entity.monster.Enemy || ent instanceof Player))) {
            double d = e.distanceToSqr(player);
            if (d <= 256.0D && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    public static final class NightsFocusStarProjectile {
        final ServerLevel level;
        final ServerPlayer owner;
        @Nullable LivingEntity target;
        Vec3 pos;
        Vec3 lastTargetPos;
        final float damage;
        final float units;
        int age;

        NightsFocusStarProjectile(ServerLevel level, ServerPlayer owner, LivingEntity target, Vec3 pos, float damage, float units) {
            this.level = level;
            this.owner = owner;
            this.target = target;
            this.pos = pos;
            this.lastTargetPos = target.getBoundingBox().getCenter();
            this.damage = damage;
            this.units = units;
        }
    }

    public static boolean fireSingleNightsFocusStar(ServerLevel level, ServerPlayer player) {
        LivingEntity target = findNearestNightsFocusTarget(level, player);
        if (target == null) return false;
        boolean isNetherite = NIGHTS_FOCUS_NETHERITE.getOrDefault(player.getUUID(), false);
        float dmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.NIGHTS_FOCUS, isNetherite) * 0.75F;
        float units = isNetherite ? 0.75F : 0.5F;

        Vec3 start = player.position().add(0.0D, player.getBbHeight() + 0.65D, 0.0D);
        NIGHTS_FOCUS_PROJECTILES.add(new NightsFocusStarProjectile(level, player, target, start, dmg, units));
        level.sendParticles(ParticleTypes.SNOWFLAKE, start.x, start.y, start.z, 4, 0.1D, 0.1D, 0.1D, 0.02D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 0.6F, 1.55F);
        return true;
    }

    public static void tickNightsFocusProjectiles() {
        if (NIGHTS_FOCUS_PROJECTILES.isEmpty()) return;
        for (NightsFocusStarProjectile p : NIGHTS_FOCUS_PROJECTILES) {
            p.age++;
            Vec3 targetPos = (p.target != null && p.target.isAlive() && !p.target.isRemoved())
                    ? p.target.getBoundingBox().getCenter()
                    : p.lastTargetPos;
            p.lastTargetPos = targetPos;

            Vec3 toTarget = targetPos.subtract(p.pos);
            double dist = toTarget.length();
            boolean hit = dist <= 0.75D;
            if (!hit && p.target != null && p.target.isAlive()) {
                AABB box = new AABB(p.pos.x - 0.35D, p.pos.y - 0.35D, p.pos.z - 0.35D,
                        p.pos.x + 0.35D, p.pos.y + 0.35D, p.pos.z + 0.35D);
                hit = p.target.getBoundingBox().intersects(box);
            }

            if (hit || p.age > 65) {
                NIGHTS_FOCUS_PROJECTILES.remove(p);
                if (hit && p.target != null && p.target.isAlive()) {
                    p.level.sendParticles(ParticleTypes.SNOWFLAKE, p.pos.x, p.pos.y, p.pos.z, 8, 0.18D, 0.18D, 0.18D, 0.04D);
                    p.level.sendParticles(ParticleTypes.END_ROD, p.pos.x, p.pos.y, p.pos.z, 4, 0.1D, 0.1D, 0.1D, 0.02D);
                    p.level.playSound(null, p.pos.x, p.pos.y, p.pos.z,
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 1.7F);
                    applyElement(p.level, p.target, Element.CRYO, p.units, p.owner, p.damage);
                }
                continue;
            }

            Vec3 dir = dist > 1.0E-4D ? toTarget.scale(1.0D / dist) : new Vec3(0.0D, 1.0D, 0.0D);
            if (p.age <= 4) {
                dir = dir.add(0.0D, 0.35D, 0.0D).normalize();
            }
            p.pos = p.pos.add(dir.scale(0.55D)); // Moderate flight speed (~11 blocks/s)
            p.level.sendParticles(ParticleTypes.SNOWFLAKE, p.pos.x, p.pos.y, p.pos.z, 2, 0.04D, 0.04D, 0.04D, 0.01D);
            p.level.sendParticles(ParticleTypes.END_ROD, p.pos.x, p.pos.y, p.pos.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    public static boolean hasActiveJadeShield(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        Long until = JADE_SHIELD_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now >= until) {
            JADE_SHIELD_UNTIL.remove(player.getUUID());
            if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
                Element cryst = CrystallizeShardManager.getActiveShield(sp, now);
                if (cryst == null) {
                    com.velorise.staves.network.ModNetwork.sendCrystallizeShieldToPlayer(sp, null, 0);
                }
            }
            return false;
        }
        if (!player.isCreative() && player.getAbsorptionAmount() <= 0.01F) {
            JADE_SHIELD_UNTIL.remove(player.getUUID());
            if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
                Element cryst = CrystallizeShardManager.getActiveShield(sp, now);
                if (cryst == null) {
                    com.velorise.staves.network.ModNetwork.sendCrystallizeShieldToPlayer(sp, null, 0);
                }
                sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 0.9F, 0.75F);
            }
            return false;
        }
        return true;
    }

    public static boolean isNearJadeShieldAura(@Nullable LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel sl)) return false;
        for (ServerPlayer sp : sl.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(target) <= 20.25D)) {
            if (hasActiveJadeShield(sp) && sp != target && !sp.isAlliedTo(target)) {
                return true;
            }
        }
        return false;
    }

    public static float getVortexGoldenMajestyMultiplier(@Nullable ServerPlayer attacker) {
        return 1.0F;
    }

    public static void updateCoopMobScaling(ServerLevel level, LivingEntity entity) {
        if (level == null || entity == null || !entity.isAlive() || entity.level().isClientSide()) return;
        if (!(entity instanceof Mob mob) || entity instanceof Player || entity instanceof Npc) return;
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) return;
        boolean eligible = entity instanceof Enemy || isBossEntity(entity) || isMutatedInnate(entity) || isOmniInnate(entity) || !getInnateElements(entity).isEmpty();
        if (!eligible) return;

        int nearbyPlayers = 0;
        if (level.players().size() > 1) {
            double radius = StavesConfig.COOP_SCAN_RADIUS.get();
            double r2 = radius * radius;
            nearbyPlayers = level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(entity) <= r2).size();
        }
        int maxCap = Math.max(1, StavesConfig.MAX_COOP_PLAYERS_CAP.get());
        int extraPlayers = Math.min(maxCap - 1, Math.max(0, nearbyPlayers - 1));

        double hpBonus = (StavesConfig.ENABLE_COOP_MOB_SCALING.get() && extraPlayers > 0)
                ? (extraPlayers * StavesConfig.HP_SCALE_PER_EXTRA_PLAYER.get())
                : 0.0D;
        double atkBonus = 0.0D;
        if (StavesConfig.ENABLE_COOP_ATK_SCALING.get() && extraPlayers > 0) {
            atkBonus = switch (extraPlayers) {
                case 1 -> 0.10D;
                case 2 -> 0.25D;
                case 3 -> 0.40D;
                default -> 0.40D + (extraPlayers - 3) * 0.15D;
            };
        }

        AttributeInstance hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            AttributeModifier cur = hpAttr.getModifier(COOP_HP_MOD_ID);
            double curVal = cur != null ? cur.amount() : 0.0D;
            if (Math.abs(curVal - hpBonus) > 0.001D) {
                float oldMax = mob.getMaxHealth();
                float ratio = oldMax > 0.0F ? Math.min(1.0F, mob.getHealth() / oldMax) : 1.0F;
                if (cur != null) hpAttr.removeModifier(COOP_HP_MOD_ID);
                if (hpBonus > 0.001D) {
                    hpAttr.addTransientModifier(new AttributeModifier(
                            COOP_HP_MOD_ID, hpBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
                mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * ratio));
            }
        }

        AttributeInstance atkAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            AttributeModifier cur = atkAttr.getModifier(COOP_ATK_MOD_ID);
            double curVal = cur != null ? cur.amount() : 0.0D;
            if (Math.abs(curVal - atkBonus) > 0.001D) {
                if (cur != null) atkAttr.removeModifier(COOP_ATK_MOD_ID);
                if (atkBonus > 0.001D) {
                    atkAttr.addTransientModifier(new AttributeModifier(
                            COOP_ATK_MOD_ID, atkBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
        }
    }

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
                LivingEntity prevCredit = CURRENT_REACTION_ATTACKER.get();
                LivingEntity sourceAttacker = source.getEntity() instanceof LivingEntity le && le != target
                        ? le : (source.getDirectEntity() instanceof LivingEntity de && de != target ? de : null);
                if (sourceAttacker != null) {
                    CURRENT_REACTION_ATTACKER.set(sourceAttacker);
                }
                try {
                    processIncomingDamageSources(level, target, source);
                } finally {
                    if (prevCredit == null) CURRENT_REACTION_ATTACKER.remove();
                    else CURRENT_REACTION_ATTACKER.set(prevCredit);
                }
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

    public static void processIncomingDamageSources(ServerLevel level, LivingEntity target, DamageSource source) {
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
                    boolean hadElectroBeforeOmni = hasElement(target, Element.ELECTRO) || hasInnateElement(target, Element.ELECTRO);
                    applyElementBypassIcd(level, target, chosenElem, 1.0F, attackerPlayer, 0.0F);
                    if (chosenElem == Element.PYRO && !hadElectroBeforeOmni
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
                            target.hurt(ElementalDeathMessageHelper.createTypedDamageSource(level.damageSources().onFire(), omniAttacker, null, Element.PYRO), fireDmg);
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
                // 1. Environmental Fire (skip refreshing existing Pyro from residual ON_FIRE afterburn unless touching real fire/lava or reacting)
                if (source.is(DamageTypeTags.IS_FIRE)) {
                    if (source.is(DamageTypes.ON_FIRE)
                            && !isTouchingEnvironmentalFireBlock(target)
                            && !BURNING_UNTIL.containsKey(target.getUUID())) {
                        Map<Element, GaugeState> tg = GAUGES.get(target.getUUID());
                        GaugeState fg = FROZEN_GAUGES.get(target.getUUID());
                        boolean hasReactive = (fg != null && !fg.isExpired())
                                || (tg != null && (tg.containsKey(Element.CRYO)
                                        || tg.containsKey(Element.HYDRO)
                                        || tg.containsKey(Element.ELECTRO)
                                        || tg.containsKey(Element.DENDRO)))
                                || QUICKEN_GAUGES.containsKey(target.getUUID());
                        if (!hasReactive && tg != null && tg.containsKey(Element.PYRO)) {
                            return;
                        }
                    }
                    handleEnvironmentalFireDamage(target);
                    return;
                }

                // 1b. Mutated Innate Mob Attack (takes priority over mob-specific default elements; supports dual Charged Mutated Creeper)
                if (source.getEntity() instanceof LivingEntity mutatedAttacker && mutatedAttacker != target && isMutatedInnate(mutatedAttacker)) {
                    EnumSet<Element> mutatedSet = getInnateElements(mutatedAttacker);
                    if (!mutatedSet.isEmpty()) {
                        if (!mutatedSet.contains(Element.PYRO) && mutatedAttacker.isOnFire() && !target.isInLava()) {
                            target.clearFire();
                        }
                        ServerPlayer attackerPlayer = mutatedAttacker instanceof ServerPlayer sp ? sp : null;
                        for (Element mutatedElem : mutatedSet) {
                            if (!target.isAlive()) break;
                            boolean hadElectroBeforeMut = hasElement(target, Element.ELECTRO) || hasInnateElement(target, Element.ELECTRO);
                            applyElementBypassIcd(level, target, mutatedElem, 1.0F, attackerPlayer, 0.0F);
                            if (mutatedElem == Element.PYRO && !hadElectroBeforeMut
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
                                    target.hurt(ElementalDeathMessageHelper.createTypedDamageSource(level.damageSources().onFire(), mutatedAttacker, null, Element.PYRO), fireDmg);
                                    target.invulnerableTime = prevInvuln;
                                }
                                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                                        6, 0.25D, 0.25D, 0.25D, 0.03D);
                            }
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
                if (source.is(DamageTypes.WIND_CHARGE)) {
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
                        level.sendParticles(ParticleTypes.GUST, target.getX(), target.getY() + 0.6D, target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.85F, 1.15F);
                        applyElementBypassIcd(level, target, Element.ANEMO, anemoU, attackerPlayer, bonusAnemoDmg);
                    }
                }

                // 16. Attacker with Innate Element
                if (source.getEntity() instanceof LivingEntity attackerMob && attackerMob != target) {
                    EnumSet<Element> innateSet = getInnateElements(attackerMob);
                    if (!innateSet.isEmpty()) {
                        if (!innateSet.contains(Element.PYRO) && attackerMob.isOnFire() && !target.isInLava()) {
                            target.clearFire();
                        }
                        ServerPlayer attackerPlayer = attackerMob instanceof ServerPlayer sp ? sp : null;
                        for (Element innate : innateSet) {
                            applyElementBypassIcd(level, target, innate, 1.0F, attackerPlayer, 0.0F);
                        }
                    }
                }
    }

    public static float modifyFabricIncomingDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity == null || entity.level().isClientSide() || amount <= 0.0F) return amount;
        if (source.is(DamageTypeTags.IS_FALL) && PlungeAndGliderManager.isPlungeFallImmune(entity)) {
            entity.fallDistance = 0.0F;
            return 0.0F;
        }
        beginDamageFloatTracking(entity);
        long now = entity.level().getGameTime();

        amount = modifyIncomingEnvironmentalDamage(entity, source, amount);

        if (entity instanceof ServerPlayer sp) {
            Element activeShield = CrystallizeShardManager.getActiveShield(sp, now);
            if (activeShield != null && CrystallizeShardManager.matchesElementalShield(activeShield, source)) {
                amount *= 0.4F;
                sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.8F);
            }
        }

        amount = applyImbuedSwordHit(entity, source, amount);
        amount = applyRaidenMusouIsshinHit(entity, source, amount);
        amount = applyMobDamageResistances(entity, source, amount);
        amount = ElementalResonanceManager.modifyPhysicalAttackDamage(entity, source, amount);
        amount = ElementalResonanceManager.modifyIncomingDefenseDamage(entity, source, amount);
        return amount;
    }

    public static void onEvokerFangJoin(ServerLevel level, EvokerFangs fangs) {
        if (level == null || fangs == null || fangs.isRemoved()) return;
        if (fangs.getTags().contains("velorise_staves:geo_fangs")
                || fangs.getTags().contains("velorise_staves:evoker_construct_checked")) {
            return;
        }
        if (!(fangs.getOwner() instanceof net.minecraft.world.entity.monster.Evoker evoker) || !evoker.isAlive()) {
            return;
        }
        fangs.addTag("velorise_staves:evoker_construct_checked");
        int warmup = 0;
        try {
            CompoundTag nbt = new CompoundTag();
            fangs.saveWithoutId(nbt);
            warmup = nbt.getInt("Warmup");
        } catch (Exception ignored) {}
        long now = level.getGameTime();
        UUID evokerId = evoker.getUUID();
        PendingEvokerConstruct cur = PENDING_EVOKER_CONSTRUCTS.get(evokerId);
        if (cur == null || cur.tick() != now || warmup >= cur.maxWarmup()) {
            PENDING_EVOKER_CONSTRUCTS.put(evokerId, new PendingEvokerConstruct(
                    level, evokerId, now, fangs.getX(), fangs.getY(), fangs.getZ(), fangs.getYRot(), warmup
            ));
        }
    }

    public static void flushPendingEvokerConstructs(MinecraftServer server) {
        if (PENDING_EVOKER_CONSTRUCTS.isEmpty()) return;
        List<PendingEvokerConstruct> batch = new ArrayList<>(PENDING_EVOKER_CONSTRUCTS.values());
        PENDING_EVOKER_CONSTRUCTS.clear();
        for (PendingEvokerConstruct entry : batch) {
            if (entry.level() == null) continue;
            Entity ownerEnt = entry.level().getEntity(entry.evokerId());
            if (!(ownerEnt instanceof net.minecraft.world.entity.monster.Evoker evoker) || !evoker.isAlive() || evoker.isRemoved()) {
                continue;
            }
            double cx = Math.floor(entry.x()) + 0.5D;
            double cz = Math.floor(entry.z()) + 0.5D;
            double cy = entry.y();
            AABB overlapBox = new AABB(cx - 0.65D, cy - 0.5D, cz - 0.65D, cx + 0.65D, cy + 1.5D, cz + 0.65D);
            boolean alreadyPresent = !entry.level().getEntitiesOfClass(
                    com.velorise.staves.entity.GeoFangPillarEntity.class, overlapBox,
                    p -> p.isAlive() && !p.isRemoved()).isEmpty();
            if (alreadyPresent) continue;

            com.velorise.staves.entity.GeoFangPillarEntity.enforceMaxConstructsForOwner(
                    entry.level(), VeloriseStavesMod.getGeoFangPillarType(), evoker.getUUID());
            com.velorise.staves.entity.GeoFangPillarEntity construct =
                    new com.velorise.staves.entity.GeoFangPillarEntity(
                            VeloriseStavesMod.getGeoFangPillarType(),
                            entry.level(),
                            cx, cy, cz,
                            entry.yawDeg(),
                            evoker,
                            false
                    );
            entry.level().addFreshEntity(construct);
        }
    }

    static void applyPetrifyState(LivingEntity target, int petrifyTicks, long targetGameTime) {
        if (target == null || !target.isAlive() || isBossEntity(target)) return;
        UUID targetId = target.getUUID();
        long until = targetGameTime + petrifyTicks;
        Long prevFreezeUntil = FROZEN_UNTIL.get(targetId);
        FROZEN_UNTIL.put(targetId, prevFreezeUntil == null ? until : Math.max(prevFreezeUntil, until));
        Long prevPetrifyUntil = PETRIFIED_UNTIL.get(targetId);
        PETRIFIED_UNTIL.put(targetId, prevPetrifyUntil == null ? until : Math.max(prevPetrifyUntil, until));

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
            mob.setNoAi(true);
            mob.addTag(TAG_FROZEN_STATUE);
        }
        enforceFrozenPose(target, targetId);

        Map<Element, GaugeState> entityGauges = GAUGES.get(targetId);
        syncAuras(target, entityGauges != null ? entityGauges.keySet() : Collections.emptySet(), null, null, null);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(target, petrifyTicks >= 95 ? 17 : 16);
    }

    public static void cancelIdle(ServerPlayer player) {
        if (player == null) return;
        ZhongliIdleState st = ZHONGLI_IDLE_STATES.get(player.getUUID());
        if (st != null) {
            st.stillTicks = 0;
            st.lastStillPos = player.position();
            if (st.idleMeteorActive || st.lastSyncedCode == 4011) {
                st.idleMeteorActive = false;
                st.lastSyncedCode = 4010;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 4010);
            }
        }
    }
}
