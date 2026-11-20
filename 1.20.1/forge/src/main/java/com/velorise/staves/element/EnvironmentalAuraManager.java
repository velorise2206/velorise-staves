package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.warden.Warden;
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
 * Manages entity-level environmental elemental auras and hazards:
 * water/rain Hydro, fire/lava/campfire Pyro, mushroom Dendro contact,
 * Cryo water freezing, and Cryo slowdown debuffs.
 */
public final class EnvironmentalAuraManager {

    private EnvironmentalAuraManager() {}

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

    public static boolean isTouchingEnvironmentalFireBlock(LivingEntity target) {
        if (target == null || target.level().isClientSide()) return false;
        if (target.isInLava()) return true;
        AABB box = target.getBoundingBox().inflate(0.06D, 0.12D, 0.06D);
        BlockPos min = BlockPos.containing(box.minX, box.minY - 0.12D, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (!target.level().hasChunkAt(p)) continue;
            BlockState st = target.level().getBlockState(p);
            if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.MAGMA_BLOCK)) {
                return true;
            }
            if ((st.is(Blocks.CAMPFIRE) || st.is(Blocks.SOUL_CAMPFIRE))
                    && (!st.hasProperty(CampfireBlock.LIT) || st.getValue(CampfireBlock.LIT))) {
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
        if (isOmniInnate(target) || hasInnateElement(target, Element.DENDRO)) return;
        UUID id = target.getUUID();
        Long last = LAST_MUSHROOM_CONTACT.get(id);
        if (last != null && now >= last && (now - last) < 20L) return;
        if (!isTouchingMushroomBlock(target)) return;
        LAST_MUSHROOM_CONTACT.put(id, now);
        applyElementBypassIcd(level, target, Element.DENDRO, 1.25F, null, 0.0F);
    }

    public static float modifyIncomingEnvironmentalDamage(LivingEntity target, DamageSource source, float amount) {
        boolean prev = Boolean.TRUE.equals(IS_MODIFYING_ENV_DAMAGE.get());
        IS_MODIFYING_ENV_DAMAGE.set(Boolean.TRUE);
        try {
            return modifyIncomingEnvironmentalDamageInternal(target, source, amount);
        } finally {
            IS_MODIFYING_ENV_DAMAGE.set(prev);
        }
    }

    public static float modifyIncomingEnvironmentalDamageInternal(LivingEntity target, DamageSource source, float amount) {
        if (!target.isAlive() || target.isSpectator() || target.level().isClientSide()) return amount;
        if (IS_PROCESSING_REACTION.get()) return amount;
        if (!source.is(DamageTypeTags.IS_FIRE)) return amount;
        if (isEntityInWater(target)) return amount;

        if (isOmniInnate(target)) return 0.0F;
        EnumSet<Element> innateSet = getInnateElements(target);
        if (innateSet.contains(Element.PYRO)) return 0.0F;

        UUID id = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        long now = target.level().getGameTime();
        double dmgMult = StavesConfig.DAMAGE_MULTIPLIER.get();

        // 1. Frozen or Cryo (including Innate Cryo like Stray burning in sunlight) -> Forward Melt (2.0x damage)
        GaugeState fg = FROZEN_GAUGES.get(id);
        if ((fg != null && !fg.isExpired()) || targetGauges.containsKey(Element.CRYO) || innateSet.contains(Element.CRYO)) {
            if (checkReactionIcd(target, ElementalReaction.FORWARD_MELT, now)) {
                if (fg != null) {
                    clearFreezeState(target, id);
                } else {
                    targetGauges.remove(Element.CRYO);
                }
                enforceInnateLock(target, targetGauges, id, innateSet, now);
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
        if (targetGauges.containsKey(Element.HYDRO) || innateSet.contains(Element.HYDRO)) {
            if (checkReactionIcd(target, ElementalReaction.REVERSE_VAPORIZE, now)) {
                targetGauges.remove(Element.HYDRO);
                enforceInnateLock(target, targetGauges, id, innateSet, now);
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

    public static void maintainBurningPyroAura(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || isEntityInWater(target)) return;
        if (isOmniInnate(target)) {
            extinguishPyroInnate(target);
            return;
        }
        EnumSet<Element> innateSet = getInnateElements(target);
        if (innateSet.contains(Element.PYRO)) {
            extinguishPyroInnate(target);
            return;
        }
        if (innateSet.contains(Element.CRYO) || innateSet.contains(Element.HYDRO)) return;

        UUID id = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        GaugeState fg = FROZEN_GAUGES.get(id);
        if ((fg != null && !fg.isExpired()) || targetGauges.containsKey(Element.CRYO) || targetGauges.containsKey(Element.HYDRO)) {
            return; // Wait for actual fire damage tick to trigger Forward Melt / Reverse Vaporize with amplified damage
        }
        handleEnvironmentalFireDamage(target);
    }

    public static void handleEnvironmentalFireDamage(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator()) return;
        if (isOmniInnate(target)) {
            extinguishPyroInnate(target);
            return;
        }
        EnumSet<Element> innateSet = getInnateElements(target);
        if (innateSet.contains(Element.PYRO)) {
            return;
        }
        if (isEntityInWater(target)) return;

        UUID id = target.getUUID();
        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        long now = target.level().getGameTime();

        // 1. Check if entity has Frozen or Cryo -> Forward Melt (if not already handled in modifyIncomingEnvironmentalDamage)
        GaugeState fg = FROZEN_GAUGES.get(id);
        if ((fg != null && !fg.isExpired()) || targetGauges.containsKey(Element.CRYO) || innateSet.contains(Element.CRYO)) {
            if (!checkReactionIcd(target, ElementalReaction.FORWARD_MELT, now)) return;
            if (fg != null) {
                clearFreezeState(target, id);
            } else {
                targetGauges.remove(Element.CRYO);
            }
            enforceInnateLock(target, targetGauges, id, innateSet, now);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.8D, target.getZ(), 15, 0.3D, 0.3D, 0.3D, 0.04D);
            }
            syncAuras(target, targetGauges.keySet(), Element.CRYO, Element.PYRO, ElementalReaction.FORWARD_MELT, 2.0F);
            return;
        }

        // 2. Check if entity has Electro -> Overloaded
        if (targetGauges.containsKey(Element.ELECTRO) || innateSet.contains(Element.ELECTRO)) {
            if (!checkReactionIcd(target, ElementalReaction.OVERLOADED, now)) {
                if (!target.isInLava()) {
                    target.clearFire();
                }
                return;
            }
            targetGauges.remove(Element.ELECTRO);
            targetGauges.remove(Element.PYRO);
            BURNING_UNTIL.remove(id);
            BURNING_EM_LEVEL.remove(id);
            BURNING_ATTACKER_UUID.remove(id);
            LAST_BURN_PYRO_REFRESH.remove(id);
            if (!target.isInLava()) {
                target.clearFire();
            }
            enforceInnateLock(target, targetGauges, id, innateSet, now);
            if (target.level() instanceof ServerLevel sl) {
                triggerOverloadExplosion(sl, target, null, 1.0D);
            }
            syncAuras(target, targetGauges.keySet(), Element.ELECTRO, Element.PYRO, ElementalReaction.OVERLOADED, 6.0F);
            return;
        }

        // 3. Check if entity has Hydro -> Reverse Vaporize
        if (targetGauges.containsKey(Element.HYDRO) || innateSet.contains(Element.HYDRO)) {
            if (!checkReactionIcd(target, ElementalReaction.REVERSE_VAPORIZE, now)) return;
            targetGauges.remove(Element.HYDRO);
            enforceInnateLock(target, targetGauges, id, innateSet, now);
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
        if (targetGauges.containsKey(Element.DENDRO) || hasQuicken || innateSet.contains(Element.DENDRO)) {
            if (target.fireImmune() || target instanceof Warden) {
                return;
            }
            if (!BURNING_UNTIL.containsKey(id) && checkReactionIcd(target, ElementalReaction.BURNING, now)) {
                long burnTicks = ElementalResonanceManager.getBurningDurationTicksForTarget(
                        target, 200L);
                BURNING_UNTIL.put(id, now + burnTicks);
                LAST_BURN_PYRO_REFRESH.put(id, now);
                if (innateSet.isEmpty() || innateSet.contains(Element.DENDRO) || innateSet.contains(Element.GEO)) {
                    targetGauges.put(Element.PYRO, GaugeState.createBurning(2.0F));
                }
                if (target.getRemainingFireTicks() < 100) {
                    target.setSecondsOnFire(5);
                }
                enforceInnateLock(target, targetGauges, id, innateSet, now);
                syncAuras(target, targetGauges.keySet(), Element.DENDRO, Element.PYRO, ElementalReaction.BURNING, 0.0F);
            }
            return;
        }

        // No reactive aura -> apply or refresh Pyro (2.0U on Soul Campfire, 1.0U on regular fire/lava/campfire)
        if (!innateSet.contains(Element.PYRO)) {
            boolean soulFire = isTouchingSoulCampfire(target);
            float appliedPyro = soulFire ? 2.5F : 1.25F; // 2.5F * 0.8 tax = 2.0U; 1.25F * 0.8 tax = 1.0U
            boolean wasMissing = !targetGauges.containsKey(Element.PYRO);
            if (wasMissing) {
                targetGauges.put(Element.PYRO, createAuraGauge(target, Element.PYRO, appliedPyro));
                syncAuras(target, targetGauges.keySet(), null, null, null, 0.0F);
            } else {
                GaugeState existing = targetGauges.get(Element.PYRO);
                if (existing != null && (soulFire || isTouchingEnvironmentalFireBlock(target))) {
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

    public static void handleEnvironmentalWater(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || isOmniInnate(target)) return;
        UUID id = target.getUUID();
        EnumSet<Element> innateSet = getInnateElements(target);

        if (!innateSet.isEmpty()) {
            long now = target.level().getGameTime();
            // 1A. Innate Dendro mob (Bee, Bogged, Slime, Mutated Dendro) in water or rain -> Trigger Bloom & spawn Dendro Core!
            if (innateSet.contains(Element.DENDRO)) {
                Long lastBloom = INNATE_COOLDOWN_UNTIL.get(id);
                if (lastBloom == null || now >= lastBloom) {
                    INNATE_COOLDOWN_UNTIL.put(id, now + 20L); // 1.0s ICD
                    if (target.level() instanceof ServerLevel sl) {
                        DendroCoreManager.spawn(sl, target.position(), null, 1.0F);
                        if (canEmitReactionVisual(sl, target.position(), 2)) {
                            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 0.8D, target.getZ(), 8, 0.5D, 0.35D, 0.5D, 0.03D);
                            sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F);
                        }
                    }
                    syncAuras(target, innateSet, Element.DENDRO, Element.HYDRO, ElementalReaction.BLOOM, 0.0F);
                }
            }

            // 1B. Innate Pyro mob in water or rain -> Vaporize!
            if (innateSet.contains(Element.PYRO)) {
                Long lastVape = INNATE_COOLDOWN_UNTIL.get(id);
                if (lastVape == null || now >= lastVape) {
                    INNATE_COOLDOWN_UNTIL.put(id, now + 20L); // 1.0s ICD
                    boolean sensitive = isNaturallyWaterSensitive(target);
                    float vapeDmg = sensitive ? applyMobElementalResistance(target, Element.HYDRO, 3.0F) : 0.0F;
                    if (target.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 14, 0.3D, 0.3D, 0.3D, 0.04D);
                        sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.2F);
                        if (sensitive && vapeDmg > 0.0F) {
                            hurtWithPreAppliedElementalRes(target, ElementalDeathMessageHelper.createDamageSource(sl, null, ElementalReaction.FORWARD_VAPORIZE, Element.HYDRO), vapeDmg);
                        }
                    }
                    syncAuras(target, innateSet, Element.PYRO, Element.HYDRO, ElementalReaction.FORWARD_VAPORIZE, vapeDmg);
                }
            }

            // 1C. Innate Electro mob in water or rain -> Electro discharge into water, immune to self-shock!
            if (innateSet.contains(Element.ELECTRO)) {
                Long lastEc = INNATE_COOLDOWN_UNTIL.get(id);
                if (lastEc == null || now >= lastEc) {
                    INNATE_COOLDOWN_UNTIL.put(id, now + 20L); // 1.0s ICD
                    if (target.level() instanceof ServerLevel sl) {
                        if (canEmitReactionVisual(sl, target.position(), 2)) {
                            // ParticleTypes.ELECTRIC_SPARK removed
                            sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.8F);
                        }
                        if (isEntitySubmergedInWater(target)) {
                            AABB chainBox = target.getBoundingBox().inflate(1.0D);
                            int chained = 0;
                            for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, chainBox, e -> e.isAlive() && e != target)) {
                                if (isEntitySubmergedInWater(nearby) && !hasInnateElement(nearby, Element.ELECTRO)) {
                                    if (chained++ >= 2) break;
                                    drawVoltaicArc(sl, target, nearby);
                                    applyElement(sl, nearby, Element.ELECTRO, 0.5F, null, 1.0F);
                                }
                            }
                        }
                    }
                }
            }

            // 1D. Innate Cryo mob physically in water or stepping on water surface -> Freeze Water!
            if (innateSet.contains(Element.CRYO)) {
                checkCryoWaterFreezing(target);
            }
            return;
        }

        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        long now = target.level().getGameTime();

        // 2. Pyro in water/rain -> Extinguish & Vaporize
        if (targetGauges.containsKey(Element.PYRO)) {
            targetGauges.remove(Element.PYRO);
            BURNING_UNTIL.remove(id);
            BURNING_EM_LEVEL.remove(id);
            BURNING_ATTACKER_UUID.remove(id);
            ELECTRO_CHARGE_EM_LEVEL.remove(id);
            LAST_BURN_PYRO_REFRESH.remove(id);
            target.clearFire();
            boolean sensitive = isNaturallyWaterSensitive(target);
            float vapeDmg = sensitive ? applyMobElementalResistance(target, Element.HYDRO, 2.5F) : 0.0F;
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.03D);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.3F);
                if (sensitive && vapeDmg > 0.0F) {
                    hurtWithPreAppliedElementalRes(target, ElementalDeathMessageHelper.createDamageSource(sl, null, ElementalReaction.FORWARD_VAPORIZE, Element.HYDRO), vapeDmg);
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

        // 4. Electro in water/rain -> Keep Hydro aura active so section 3 ticks Electro-Charged cleanly once per 1.0s (no double tick)

        // 5. Environmental Wet: Apply or keep Hydro refreshed while in water or rain
        if (innateSet.isEmpty()) {
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
        if (!hasInnateElement(entity, Element.CRYO) && !hasElement(entity, Element.CRYO)) return;

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

    public static void updateCryoDebuff(LivingEntity target, UUID id, boolean hasCryo, @Nullable Element innate) {
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
}
