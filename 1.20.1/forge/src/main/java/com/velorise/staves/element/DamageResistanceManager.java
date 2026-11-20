package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * Manages physical/elemental damage classification, mob resistances,
 * co-op mob scaling, and incoming damage hooks across loaders.
 */
public final class DamageResistanceManager {

    private DamageResistanceManager() {}

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
        String msgId = source.type().msgId();
        return "velorise_elemental".equals(msgId)
                || "wind_charge".equals(msgId)
                || "sonic_boom".equals(msgId)
                || source instanceof ElementalDeathMessageHelper.ElementalReactionDamageSource
                || Boolean.TRUE.equals(ELEMENTAL_RES_APPLIED.get());
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
        if (WitherShulkerMechanicManager.tryDeflectProjectileWithWitherCarapace(entity, source)) {
            return 0.0F;
        }
        amount = WitherShulkerMechanicManager.modifyShulkerShellGuardDamage(entity, source, amount);
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
            double curVal = cur != null ? cur.getAmount() : 0.0D;
            if (Math.abs(curVal - hpBonus) > 0.001D) {
                float oldMax = mob.getMaxHealth();
                float ratio = oldMax > 0.0F ? Math.min(1.0F, mob.getHealth() / oldMax) : 1.0F;
                if (cur != null) hpAttr.removeModifier(COOP_HP_MOD_ID);
                if (hpBonus > 0.001D) {
                    hpAttr.addTransientModifier(new AttributeModifier(
                            COOP_HP_MOD_ID, "Co-Op Mob HP Scale", hpBonus, AttributeModifier.Operation.MULTIPLY_BASE));
                }
                mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * ratio));
            }
        }

        AttributeInstance atkAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            AttributeModifier cur = atkAttr.getModifier(COOP_ATK_MOD_ID);
            double curVal = cur != null ? cur.getAmount() : 0.0D;
            if (Math.abs(curVal - atkBonus) > 0.001D) {
                if (cur != null) atkAttr.removeModifier(COOP_ATK_MOD_ID);
                if (atkBonus > 0.001D) {
                    atkAttr.addTransientModifier(new AttributeModifier(
                            COOP_ATK_MOD_ID, "Co-Op Mob ATK Scale", atkBonus, AttributeModifier.Operation.MULTIPLY_BASE));
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

                // 1b. Mutated Innate Mob Attack (takes priority over mob-specific default elements like Husk/Ravager)
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
                    EnumSet<Element> innateSet = getInnateElements(attackerMob);
                    if (!innateSet.isEmpty()) {
                        if (!innateSet.contains(Element.PYRO) && attackerMob.isOnFire() && !target.isInLava()) {
                            target.clearFire();
                        }
                        ServerPlayer attackerPlayer = attackerMob instanceof ServerPlayer sp ? sp : null;
                        for (Element innate : innateSet) {
                            if (!target.isAlive()) break;
                            applyElementBypassIcd(level, target, innate, 1.0F, attackerPlayer, 0.0F);
                        }
                    }
                }
    }
}
