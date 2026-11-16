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

public final class RaidenManager {
    private RaidenManager() {}

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
        EnumSet<Element> innates = getInnateElements(target);
        Collection<Element> active = getEntityElements(target.getUUID());
        boolean frozen = isEntityFrozen(target);
        boolean quickened = isEntityQuickened(target);
        boolean avoidPyroDamage = target.fireImmune() && !innates.contains(Element.CRYO);

        // 1. Cryo / Frozen target -> Forward Melt 2.0x with Pyro (or Superconduct if fire-immune)
        if (frozen || active.contains(Element.CRYO) || innates.contains(Element.CRYO)) {
            if (!innates.contains(Element.PYRO) && !target.fireImmune()) return Element.PYRO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 2. Pyro aura / innate -> Forward Vaporize 2.0x with Hydro
        if (active.contains(Element.PYRO) || innates.contains(Element.PYRO)) {
            if (!innates.contains(Element.HYDRO)) return Element.HYDRO;
            if (!innates.contains(Element.CRYO)) return Element.CRYO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 3. Hydro aura / innate -> Reverse Vaporize 1.5x / Overvape with Pyro (or Dendro Bloom)
        if (active.contains(Element.HYDRO) || innates.contains(Element.HYDRO)) {
            if (!avoidPyroDamage && !innates.contains(Element.PYRO)) return Element.PYRO;
            if (!innates.contains(Element.DENDRO)) return Element.DENDRO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 4. Quicken or Dendro aura / innate -> Aggravate / Burning / Quicken
        if (quickened) {
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
            if (!innates.contains(Element.DENDRO)) return Element.DENDRO;
        }
        if (active.contains(Element.DENDRO) || innates.contains(Element.DENDRO)) {
            if (!avoidPyroDamage && !innates.contains(Element.PYRO)) return Element.PYRO;
            if (!innates.contains(Element.ELECTRO)) return Element.ELECTRO;
        }
        // 5. Electro aura / innate -> Overloaded / Quicken / Superconduct
        if (active.contains(Element.ELECTRO) || innates.contains(Element.ELECTRO)) {
            if (!avoidPyroDamage && !innates.contains(Element.PYRO)) return Element.PYRO;
            if (!innates.contains(Element.DENDRO)) return Element.DENDRO;
            if (!innates.contains(Element.CRYO)) return Element.CRYO;
        }
        // 6. Anemo or Geo innate -> Swirl / Crystallize
        if (innates.contains(Element.ANEMO) || innates.contains(Element.GEO)) {
            return avoidPyroDamage ? Element.HYDRO : Element.PYRO;
        }
        // 7. No active aura on target -> pick a random element from all 7 elements
        Element[] all = Element.values();
        return all[attacker.getRandom().nextInt(all.length)];
    }

    public static void onPlayerCastStaffSkill(@Nullable ServerPlayer player) {
        if (player == null) return;
        grantRaidenResolveOnStaffSkill(player);
        if (!hasActiveNightsFocusShield(player)) return;
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        long lastSkill = NIGHTS_FOCUS_LAST_SKILL_STAR.getOrDefault(id, -100L);
        if (now >= lastSkill && now - lastSkill < 6L) return; // 0.3s cooldown
        NIGHTS_FOCUS_LAST_SKILL_STAR.put(id, now);
        addNightsFocusStars(player, 2, now);
    }

    public static void grantRaidenResolveOnStaffSkill(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        UUID id = player.getUUID();
        long now = player.serverLevel().getServer().overworld().getGameTime();
        long last = RAIDEN_LAST_STAFF_RESOLVE_TICK.getOrDefault(id, -100L);
        if (now >= last && now - last < 20L) return;
        boolean hasRaiden = now < RAIDEN_EYE_UNTIL.getOrDefault(id, 0L)
                || ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                || ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (!hasRaiden) {
            for (ItemStack invStack : player.getInventory().items) {
                if (ArcaneStaffItem.getEffectiveKind(invStack) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                    hasRaiden = true;
                    break;
                }
            }
        }
        if (!hasRaiden) return;
        RAIDEN_LAST_STAFF_RESOLVE_TICK.put(id, now);
        addRaidenResolve(player, 6);
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
    public static Element pickDefenderAuraForThorns(LivingEntity defender) {
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

    public static void reflectThornsElement(
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

    public static void recordPreAttackStrength(@Nullable Player player, float scale) {
        if (player != null) {
            PRE_ATTACK_STRENGTH.put(player.getUUID(), scale);
        }
    }

    public static void clearPreAttackStrength(@Nullable Player player) {
        if (player != null) {
            PRE_ATTACK_STRENGTH.remove(player.getUUID());
        }
    }

    public static float getEffectiveAttackStrengthScale(Player attacker) {
        Float recorded = PRE_ATTACK_STRENGTH.get(attacker.getUUID());
        float current = attacker.getAttackStrengthScale(0.5F);
        return recorded != null ? Math.max(recorded, current) : current;
    }

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
        if (imbueKey == null && !hasFireAspect) {
            return incomingDamage;
        }
        if (com.velorise.staves.item.SwordImbueHelper.isNonPyroImbued(weapon) && hasFireAspect) {
            ensurePreDeathPyroIgnite(target, 4);
            return incomingDamage;
        }

        if (getEffectiveAttackStrengthScale(attacker) < 0.9F) {
            if (hasFireAspect) {
                ensurePreDeathPyroIgnite(target, 4);
            }
            return incomingDamage;
        }

        Element chosen;
        if (imbueKey == null) {
            chosen = Element.PYRO; // Non-imbued Fire Aspect weapon applies 1U Pyro on full-charge swing
        } else if (com.velorise.staves.item.SwordImbueHelper.OMNI_KEY.equals(imbueKey)) {
            chosen = com.velorise.staves.entity.OmniOrbEntity.chooseBestElementForTarget(target, attacker);
        } else {
            chosen = com.velorise.staves.item.SwordImbueHelper.getImbuedElement(weapon);
        }
        if (chosen == null) return incomingDamage;

        float[] outDamage = new float[] { incomingDamage };
        IS_PROCESSING_SWORD_IMBUE.set(true);
        try {
            applyElement(level, target, chosen, 1.0F, attacker, incomingDamage, true, outDamage);
        } finally {
            IS_PROCESSING_SWORD_IMBUE.set(false);
        }
        if ((chosen == Element.PYRO || hasFireAspect) && !hasElement(target, Element.ELECTRO) && !hasInnateElement(target, Element.ELECTRO)) {
            ensurePreDeathPyroIgnite(target, 4);
        }
        return outDamage[0];
    }

    public static void triggerOverloadExplosion(ServerLevel level, LivingEntity target, @Nullable ServerPlayer attacker, double dmgMult) {
        Vec3 pos = target.position();
        if (canEmitReactionVisual(level, pos, 2)) {
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5D, pos.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.8D, pos.z, 8, 0.5D, 0.5D, 0.5D, 0.05D);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.1F);
        }
        EnvironmentalBlockManager.applyOverloadEnvironmentEffects(level, pos, 3.5D);

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
            victim.invulnerableTime = 0;
            LivingEntity olCredit = attacker != null ? attacker : CURRENT_REACTION_ATTACKER.get();
            if (dmg > 0.0F) {
                ElementalDeathMessageHelper.recordReactionForDeath(victim, ElementalReaction.OVERLOADED, Element.PYRO, olCredit);
            }
            hurtWithPreAppliedElementalRes(victim, getElementalDamageSource(level, attacker, ElementalReaction.OVERLOADED, Element.PYRO), dmg);
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

    public static void handleCreeperExplosionBurst(
            ServerLevel level,
            Creeper creeper,
            Vec3 center,
            float explosionRadius
    ) {
        if (level == null || creeper == null || center == null) return;
        UUID creeperId = creeper.getUUID();
        long now = level.getGameTime();
        Long prevBurst = EXPLODED_CREEPERS.putIfAbsent(creeperId, now);
        if (prevBurst != null) return;
        if (creeper.getTags().contains("velorise_staves:explosion_burst_done")) return;
        creeper.addTag("velorise_staves:explosion_burst_done");

        boolean isOmni = isOmniInnate(creeper);
        EnumSet<Element> innateElements = getInnateElements(creeper);
        if (!isOmni && innateElements.isEmpty()) {
            return;
        }

        boolean powered = creeper.isPowered();
        double baseRad = explosionRadius > 0.0F ? explosionRadius : (powered ? 6.0D : 3.0D);
        double burstRadius = powered ? Math.max(6.0D, baseRad * 1.5D) : Math.max(3.5D, baseRad * 1.6D);
        float applyU = powered ? 2.0F : 1.5F;
        double dmgMult = 1.0D;
        try {
            dmgMult = StavesConfig.DAMAGE_MULTIPLIER.get();
        } catch (Exception ignored) {}
        float baseElemDmg = (float) ((powered ? 3.0F : 2.0F) * dmgMult);

        // Environmental element signatures around crater center
        if (isOmni) {
            level.sendParticles(ParticleTypes.FIREWORK, center.x, center.y + 0.6D, center.z, 26, 1.0D, 0.8D, 1.0D, 0.08D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.6D, center.z, 18, 0.9D, 0.7D, 0.9D, 0.05D);
        } else {
            if (innateElements.contains(Element.PYRO)) {
                level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.6D, center.z, 32, 1.2D, 0.6D, 1.2D, 0.06D);
                BlockPos bp = BlockPos.containing(center);
                for (BlockPos p : BlockPos.betweenClosed(bp.offset(-2, -1, -2), bp.offset(2, 2, 2))) {
                    if (!level.hasChunkAt(p)) continue;
                    BlockState st = level.getBlockState(p);
                    if (st.is(Blocks.FROSTED_ICE) || st.is(Blocks.ICE)) {
                        level.setBlockAndUpdate(p, level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState());
                    }
                }
            }
            if (innateElements.contains(Element.HYDRO)) {
                level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.6D, center.z, 34, 1.2D, 0.6D, 1.2D, 0.1D);
                BlockPos bp = BlockPos.containing(center);
                for (BlockPos p : BlockPos.betweenClosed(bp.offset(-2, -1, -2), bp.offset(2, 2, 2))) {
                    if (level.hasChunkAt(p) && level.getBlockState(p).is(Blocks.FIRE)) {
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            if (innateElements.contains(Element.ELECTRO)) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.7D, center.z, 34, 1.2D, 0.8D, 1.2D, 0.08D);
                ElectrifiedWaterManager.electrify(level, BlockPos.containing(center), powered ? 3.5D : 2.5D, 60, null, 1.5F);
            }
            if (innateElements.contains(Element.CRYO)) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.6D, center.z, 32, 1.2D, 0.7D, 1.2D, 0.05D);
                com.velorise.staves.event.ArcaneStaffEvents.freezeWaterNear(level, center, powered ? 4 : 3);
            }
            if (innateElements.contains(Element.DENDRO)) {
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.7D, center.z, 28, 1.1D, 0.7D, 1.1D, 0.05D);
                BlockPos bp = BlockPos.containing(center);
                if (level.getFluidState(bp).is(FluidTags.WATER) || level.getFluidState(bp.below()).is(FluidTags.WATER)) {
                    DendroCoreManager.spawn(level, center, null, 1.0F);
                }
            }
            if (innateElements.contains(Element.ANEMO)) {
                level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.7D, center.z, 24, 1.0D, 0.5D, 1.0D, 0.1D);
            }
            if (innateElements.contains(Element.GEO)) {
                level.sendParticles(ParticleTypes.WAX_ON, center.x, center.y + 0.6D, center.z, 24, 1.1D, 0.6D, 1.1D, 0.05D);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.9F, 1.2F);
            }
        }

        AABB area = new AABB(center.x - burstRadius, center.y - burstRadius, center.z - burstRadius,
                center.x + burstRadius, center.y + burstRadius, center.z + burstRadius);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                e.isAlive() && !e.isRemoved() && e != creeper && !e.isSpectator()
                        && !(e instanceof Player p && p.isCreative()));

        LivingEntity prevAttacker = CURRENT_REACTION_ATTACKER.get();
        CURRENT_REACTION_ATTACKER.set(creeper);
        try {
            for (LivingEntity victim : victims) {
                double dist = Math.sqrt(victim.distanceToSqr(center));
                if (dist > burstRadius) continue;
                float seen = net.minecraft.world.level.Explosion.getSeenPercent(center, victim);
                if (seen <= 0.0F) continue;
                float falloff = (float) Math.max(0.35D, 1.0D - (dist / (burstRadius + 0.5D)));
                float elemDmg = baseElemDmg * falloff * seen;

                if (isOmni) {
                    Element first = chooseOmniMutatedAttackElement(victim, creeper);
                    victim.invulnerableTime = 0;
                    if (first == Element.PYRO) ensurePreDeathPyroIgnite(victim, 4);
                    applyElementBypassIcd(level, victim, first, applyU, null, elemDmg);
                    if (powered && victim.isAlive()) {
                        Element second = Element.ELECTRO != first ? Element.ELECTRO : Element.PYRO;
                        victim.invulnerableTime = 0;
                        applyElementBypassIcd(level, victim, second, 1.5F, null, elemDmg * 0.7F);
                    }
                    victim.invulnerableTime = 0;
                } else {
                    for (Element elem : innateElements) {
                        if (!victim.isAlive()) break;
                        victim.invulnerableTime = 0;
                        if (elem == Element.PYRO) {
                            ensurePreDeathPyroIgnite(victim, 4);
                        }
                        applyElementBypassIcd(level, victim, elem, applyU, null, elemDmg);
                        if (elem == Element.ANEMO) {
                            Vec3 dir = victim.position().subtract(center);
                            Vec3 norm = dir.lengthSqr() > 1.0E-4D ? dir.normalize() : new Vec3(0.0D, 0.3D, 0.0D);
                            victim.setDeltaMovement(victim.getDeltaMovement().add(norm.x * 0.75D * falloff, 0.35D * falloff, norm.z * 0.75D * falloff));
                            victim.hurtMarked = true;
                        }
                    }
                    victim.invulnerableTime = 0;
                }
            }
        } finally {
            if (prevAttacker == null) CURRENT_REACTION_ATTACKER.remove();
            else CURRENT_REACTION_ATTACKER.set(prevAttacker);
        }
    }

    static boolean isRaidenProtectedAlly(ServerPlayer owner, LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID())) {
            return true;
        }
        return e instanceof ServerPlayer && owner.getServer() != null && !owner.getServer().isPvpAllowed();
    }

    public static boolean isRaidenSkillOnCooldown(UUID id, long now) {
        return now < RAIDEN_SKILL_CD_UNTIL.getOrDefault(id, 0L);
    }

    public static long getRaidenSkillRemainingTicks(UUID id, long now) {
        return Math.max(0L, RAIDEN_SKILL_CD_UNTIL.getOrDefault(id, 0L) - now);
    }

    public static boolean isRaidenBurstOnCooldown(UUID id, long now) {
        return now < RAIDEN_BURST_CD_UNTIL.getOrDefault(id, 0L);
    }

    public static long getRaidenBurstRemainingTicks(UUID id, long now) {
        return Math.max(0L, RAIDEN_BURST_CD_UNTIL.getOrDefault(id, 0L) - now);
    }

    public static boolean isRaidenMusouActive(UUID id, long now) {
        return now < RAIDEN_MUSOU_UNTIL.getOrDefault(id, 0L);
    }

    public static boolean isRaidenIframeActive(@Nullable Entity entity) {
        if (!(entity instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel sl)) return false;
        long now = sl.getServer().overworld().getGameTime();
        return now < RAIDEN_IFRAME_UNTIL.getOrDefault(sp.getUUID(), 0L)
                || ArchonUltiManager.isZhongliIframeActive(entity)
                || NeuvilletteManager.isNeuvilletteIframeActive(sp);
    }

    public static int getRaidenResolve(UUID id) {
        return RAIDEN_RESOLVE.getOrDefault(id, 0);
    }

    public static void syncRaidenState(ServerPlayer player, long now) {
        if (player == null) return;
        UUID id = player.getUUID();
        int eyeRem = (int) Math.max(0L, RAIDEN_EYE_UNTIL.getOrDefault(id, 0L) - now);
        int musouRem = (int) Math.max(0L, RAIDEN_MUSOU_UNTIL.getOrDefault(id, 0L) - now);
        int resolve = RAIDEN_RESOLVE.getOrDefault(id, 0);
        boolean neth = RAIDEN_MUSOU_NETHERITE.getOrDefault(id, false) || RAIDEN_EYE_NETHERITE.getOrDefault(id, false);
        int burstCd = (int) Math.max(0L, RAIDEN_BURST_CD_UNTIL.getOrDefault(id, 0L) - now);
        com.velorise.staves.network.ModNetwork.sendRaidenSync(player, eyeRem, musouRem, resolve, neth, burstCd);
    }

    public static void sendRaidenSlashArc(ServerLevel level, Vec3 center, Vec3 forward, int comboStep, boolean isMusouBurst, boolean isCharged, double span, boolean isLeftArm) {
        Vec3 fwd = new Vec3(forward.x, 0.0D, forward.z);
        if (fwd.lengthSqr() < 1.0E-4D) fwd = new Vec3(0.0D, 0.0D, 1.0D);
        else fwd = fwd.normalize();
        Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x).normalize();
        if (isLeftArm) right = right.scale(-1.0D);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        int color = (isMusouBurst || isCharged) ? 0xFFE0B0FF : 0xFFB84DFF;
        int dur = isMusouBurst ? 10 : 6;

        if (isCharged) {
            Vec3 s1 = center.add(right.scale(-span * 0.90D)).add(up.scale(-span * 0.48D));
            Vec3 e1 = center.add(right.scale(span * 0.90D)).add(up.scale(span * 0.58D));
            Vec3 c2 = center.add(fwd.scale(1.35D));
            Vec3 s2 = c2.add(right.scale(span * 0.90D)).add(up.scale(-span * 0.45D));
            Vec3 e2 = c2.add(right.scale(-span * 0.90D)).add(up.scale(span * 0.62D));
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s1, e1, dur, color);
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s2, e2, dur, color);
            return;
        }
        if (isMusouBurst) {
            Vec3 s = center.add(right.scale(-span)).add(up.scale(0.05D));
            Vec3 e = center.add(right.scale(span)).add(up.scale(0.05D));
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s, e, dur, color);
            return;
        }
        int step = Math.floorMod(comboStep, 5);
        if (step == 3) {
            Vec3 s1 = center.add(right.scale(-span * 0.85D)).add(up.scale(-span * 0.42D));
            Vec3 e1 = center.add(right.scale(span * 0.85D)).add(up.scale(span * 0.45D));
            Vec3 s2 = center.add(right.scale(span * 0.85D)).add(up.scale(-span * 0.42D));
            Vec3 e2 = center.add(right.scale(-span * 0.85D)).add(up.scale(span * 0.45D));
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s1, e1, dur, color);
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, s2, e2, dur, color);
            return;
        }
        Vec3 s;
        Vec3 e;
        int arcColor = color;
        switch (step) {
            case 0 -> {
                s = center.add(right.scale(span)).add(up.scale(0.10D));
                e = center.add(right.scale(-span)).add(up.scale(-0.08D));
            }
            case 1 -> {
                s = center.add(right.scale(-span * 0.85D)).add(up.scale(-span * 0.48D));
                e = center.add(right.scale(span * 0.85D)).add(up.scale(span * 0.52D));
            }
            case 2 -> {
                s = center.add(right.scale(span * 0.85D)).add(up.scale(span * 0.52D));
                e = center.add(right.scale(-span * 0.85D)).add(up.scale(-span * 0.45D));
            }
            default -> {
                s = center.add(up.scale(span * 1.15D));
                e = center.add(up.scale(-span * 0.85D)).add(fwd.scale(0.35D));
                arcColor = 0xFFE0B0FF;
            }
        }
        com.velorise.staves.network.ModNetwork.sendElectricArc(level, s, e, dur, arcColor);
    }

    public static void sendRaidenSlashArc(ServerLevel level, Vec3 center, Vec3 forward, int comboStep, boolean isMusouBurst, boolean isCharged, double span) {
        sendRaidenSlashArc(level, center, forward, comboStep, isMusouBurst, isCharged, span, false);
    }

    public static void addRaidenResolve(ServerPlayer player, int baseAmount) {
        if (player == null || baseAmount <= 0) return;
        boolean hasNetherite = ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING
                && ArcaneStaffItem.getEffectiveNetherite(player.getMainHandItem());
        int gained = hasNetherite ? (int) Math.ceil(baseAmount * 1.5D) : baseAmount;
        UUID id = player.getUUID();
        int updated = Math.min(60, RAIDEN_RESOLVE.getOrDefault(id, 0) + gained);
        RAIDEN_RESOLVE.put(id, updated);
        if (player.level() instanceof ServerLevel sl) {
            syncRaidenState(player, sl.getServer().overworld().getGameTime());
        }
    }

    public static boolean activateRaidenEye(ServerPlayer player, boolean isNetherite) {
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (isRaidenSkillOnCooldown(id, now)) {
            double rem = getRaidenSkillRemainingTicks(id, now) / 20.0D;
            player.displayClientMessage(Component.literal(String.format(java.util.Locale.ROOT, "§dBaleful Omen CD: %.1fs", rem)), true);
            return false;
        }
        RAIDEN_EYE_UNTIL.put(id, now + 300L);
        RAIDEN_EYE_NETHERITE.put(id, isNetherite);
        RAIDEN_EYE_NEXT_PROC.put(id, now + 18L);
        RAIDEN_SKILL_CD_UNTIL.put(id, now + 200L);
        addRaidenResolve(player, 10);

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float dmg = (float) (3.5D * scale * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 1.5F : 1.0F;
        double radius = isNetherite ? 6.75D : 4.5D;
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);

        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            AABB box = player.getBoundingBox().inflate(radius, 2.5D, radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                applyElement(level, target, Element.ELECTRO, attackU, player, dmg);
            }
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }
        EnvironmentalBlockManager.applyElectroEnvironmentEffects(level, center, radius, player, dmg);

        DustParticleOptions electroDust = new DustParticleOptions(new Vector3f(0.76F, 0.25F, 1.0F), isNetherite ? 1.6F : 1.25F);
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0D * i) / 24.0D;
            double px = center.x + Math.cos(angle) * (radius * 0.6D);
            double pz = center.z + Math.sin(angle) * (radius * 0.6D);
            level.sendParticles(electroDust, px, center.y, pz, 1, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 18, 0.8D, 0.4D, 0.8D, 0.08D);
        Vec3 frontEyePos = center.add(player.getLookAngle().scale(1.2D));
        level.sendParticles(ParticleTypes.END_ROD, frontEyePos.x, frontEyePos.y + 0.2D, frontEyePos.z, 10, 0.35D, 0.35D, 0.35D, 0.05D);
        sendRaidenSlashArc(level, frontEyePos, player.getLookAngle(), 0, false, false, isNetherite ? 2.6D : 1.9D, player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.55F);
        syncRaidenState(player, now);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 6);
        return true;
    }

    public static boolean activateRaidenMusouBurst(ServerPlayer player, boolean isNetherite) {
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (isRaidenBurstOnCooldown(id, now)) {
            ArchonUltiManager.sendMinimalistBurstCooldownAlert(player, Element.ELECTRO, getRaidenBurstRemainingTicks(id, now));
            return false;
        }
        int resolve = RAIDEN_RESOLVE.getOrDefault(id, 0);
        RAIDEN_RESOLVE.put(id, 0);
        float resolveMult = 1.0F + (resolve / 60.0F) * 1.50F;

        RAIDEN_MUSOU_UNTIL.put(id, now + 172L); // 32t draw windup + 7.0s (140t) Musou Isshin stance
        RAIDEN_MUSOU_NETHERITE.put(id, isNetherite);
        RAIDEN_BURST_CD_UNTIL.put(id, now + 360L); // Genshin Q cooldown: 18.0s
        RAIDEN_IFRAME_UNTIL.put(id, now + 34L); // 1.6s Burst i-frame covering full draw animation
        RAIDEN_NEXT_SWING_TICK.put(id, now + 30L); // Lock NA attacks until chest draw + slash finishes
        player.invulnerableTime = Math.max(player.invulnerableTime, 34);
        ArchonUltiManager.applyBurstRoot(player, 32);

        RAIDEN_PENDING_HITOTACHI_TICK.put(id, now + 28L);
        RAIDEN_PENDING_HITOTACHI_MULT.put(id, resolveMult);

        DustParticleOptions chargeGlow = new DustParticleOptions(new Vector3f(0.82F, 0.30F, 1.0F), isNetherite ? 1.5F : 1.25F);
        level.sendParticles(chargeGlow, player.getX(), player.getY() + 1.15D, player.getZ(), 12, 0.28D, 0.25D, 0.28D, 0.02D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.15D, player.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.05D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.35F);
        syncRaidenState(player, now);
        return true;
    }


    private static void electrifyWaterAlongSlash(ServerLevel level, ServerPlayer player, Vec3 slashCenter, double radius, float damage) {
        if (level == null || player == null || slashCenter == null) return;
        Vec3 playerPos = player.position();
        int steps = 3;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 sample = playerPos.lerp(slashCenter, t);
            BlockPos base = BlockPos.containing(sample);
            for (int dy = 1; dy >= -3; dy--) {
                BlockPos check = base.offset(0, dy, 0);
                if (level.getFluidState(check).is(FluidTags.WATER)) {
                    ElectrifiedWaterManager.electrify(level, check, radius, 80, player, damage);
                    break;
                }
            }
        }
    }

    public static void executeRaidenHitotachiImpact(ServerPlayer player, boolean isNetherite, float resolveMult) {
        ServerLevel level = player.serverLevel();
        com.velorise.staves.advancement.ModAdvancements.trigger(player, com.velorise.staves.advancement.ModAdvancements.EVENT_INAZUMA_SHINES_ETERNAL);
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float hitotachiDmg = (float) (12.0D * scale * resolveMult * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 3.0F : 2.0F;
        double reach = isNetherite ? 9.0D : 6.0D;
        double aoeRadius = isNetherite ? 5.25D : 3.5D;
        Vec3 look = player.getLookAngle().normalize();
        Vec3 slashCenter = player.getEyePosition().add(look.scale(reach * 0.5D));

        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            AABB box = new AABB(slashCenter, slashCenter).inflate(aoeRadius, 3.0D, aoeRadius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                applyElement(level, target, Element.ELECTRO, attackU, player, hitotachiDmg);
            }
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }
        EnvironmentalBlockManager.applyElectroEnvironmentEffects(level, slashCenter, aoeRadius, player, hitotachiDmg);
        electrifyWaterAlongSlash(level, player, slashCenter, aoeRadius, hitotachiDmg * 0.35F);

        DustParticleOptions violetSlash = new DustParticleOptions(new Vector3f(0.82F, 0.30F, 1.0F), isNetherite ? 2.1F : 1.7F);
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-4D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        for (int i = -10; i <= 10; i++) {
            double t = i / 10.0D;
            Vec3 arcPos = slashCenter.add(right.scale(t * aoeRadius)).add(0.0D, Math.cos(t * 1.4D) * 0.6D - 0.3D, 0.0D);
            level.sendParticles(violetSlash, arcPos.x, arcPos.y, arcPos.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, arcPos.x, arcPos.y, arcPos.z, 2, 0.12D, 0.12D, 0.12D, 0.06D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.15F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.75F);
        sendRaidenSlashArc(level, slashCenter, look, 0, true, false, aoeRadius, player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
    }

    public static void triggerRaidenUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack raidenStack = ItemStack.EMPTY;
        boolean fromMain = false;
        if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            raidenStack = main;
            fromMain = true;
        } else if (!ArcaneStaffItem.isGenshinCharacterWeapon(main) && ArcaneStaffItem.getEffectiveKind(off) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            raidenStack = off;
        }
        if (raidenStack.isEmpty()) return;
        boolean isNetherite = ArcaneStaffItem.getEffectiveNetherite(raidenStack);
        if (activateRaidenMusouBurst(player, isNetherite)) {
            ArcaneStaffItem.hurtStaffAndCheckBreak(raidenStack, 1, player,
                    fromMain ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(raidenStack.getItem()));
            com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 8);
        }
    }

    public static void triggerRaidenEyeCoordinatedStrike(LivingEntity target, DamageSource source) {
        if (target == null || !target.isAlive() || !(target.level() instanceof ServerLevel level)) return;
        if (Boolean.TRUE.equals(IS_PROCESSING_RAIDEN_PROC.get())) return;
        ServerPlayer attacker = null;
        if (source.getEntity() instanceof ServerPlayer sp) attacker = sp;
        else if (source.getDirectEntity() instanceof ServerPlayer sp) attacker = sp;
        if (attacker == null || attacker == target) return;

        long now = level.getServer().overworld().getGameTime();
        UUID id = attacker.getUUID();
        if (now >= RAIDEN_EYE_UNTIL.getOrDefault(id, 0L)) return;
        if (now < RAIDEN_EYE_NEXT_PROC.getOrDefault(id, 0L)) return;

        RAIDEN_EYE_NEXT_PROC.put(id, now + 18L);
        boolean isNetherite = RAIDEN_EYE_NETHERITE.getOrDefault(id, false);
        addRaidenResolve(attacker, 4);

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float dmg = (float) (2.5D * scale * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 1.5F : 1.0F;
        double aoe = isNetherite ? 1.5D : 1.0D;
        Vec3 pos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);

        final ServerPlayer finalAttacker = attacker;
        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            AABB box = target.getBoundingBox().inflate(aoe, 1.0D, aoe);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !e.isSpectator() && e != finalAttacker && !isRaidenProtectedAlly(finalAttacker, e))) {
                applyElement(level, victim, Element.ELECTRO, attackU, finalAttacker, dmg);
            }
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }
        EnvironmentalBlockManager.applyElectroEnvironmentEffects(level, pos, aoe + 1.5D, finalAttacker, dmg);

        DustParticleOptions slashDust = new DustParticleOptions(new Vector3f(0.78F, 0.25F, 1.0F), isNetherite ? 1.4F : 1.1F);
        level.sendParticles(slashDust, pos.x, pos.y, pos.z, 10, 0.4D, 0.3D, 0.4D, 0.02D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 8, 0.3D, 0.3D, 0.3D, 0.06D);
        sendRaidenSlashArc(level, pos, attacker.getLookAngle(), 1, false, false, isNetherite ? 1.35D : 1.0D, attacker.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.35F, 1.8F);
    }

    public static float applyRaidenMusouIsshinHit(LivingEntity target, DamageSource source, float incomingDamage) {
        if (target == null || !target.isAlive() || incomingDamage <= 0.0F) return incomingDamage;
        if (!(target.level() instanceof ServerLevel level)) return incomingDamage;
        if (Boolean.TRUE.equals(IS_PROCESSING_RAIDEN_PROC.get())) return incomingDamage;
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return incomingDamage;
        if (!(source.getDirectEntity() instanceof ServerPlayer attacker) || attacker == target) return incomingDamage;

        long now = level.getServer().overworld().getGameTime();
        UUID id = attacker.getUUID();
        if (!isRaidenMusouActive(id, now)) return incomingDamage;
        ItemStack mainHand = attacker.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(mainHand) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            return incomingDamage;
        }

        boolean isNetherite = ArcaneStaffItem.getEffectiveNetherite(mainHand);
        boolean isCharged = false;
        if (now < RAIDEN_NEXT_SWING_TICK.getOrDefault(id, 0L)) {
            return 0.0F;
        }
        RAIDEN_LAST_MELEE_TICK.put(id, now);
        RAIDEN_NEXT_SWING_TICK.put(id, now + (isCharged ? 11L : 8L));
        int comboStep = RAIDEN_NA_COMBO.getOrDefault(id, 0);
        RAIDEN_NA_COMBO.put(id, isCharged ? 0 : ((comboStep + 1) % 5));
        float atkStrength = Math.max(0.35F, getEffectiveAttackStrengthScale(attacker));
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float finisherMult = (!isCharged && comboStep == 4) ? 1.25F : 1.0F;
        float chargedMult = isCharged ? 1.35F : finisherMult;
        float musouResMult = RAIDEN_PENDING_HITOTACHI_MULT.getOrDefault(id, 1.0F);
        float bonusDmg = (float) (5.0D * scale * musouResMult * atkStrength * chargedMult * StavesConfig.getDamageMultiplier());
        float totalDmg = bonusDmg;
        float attackU = isNetherite ? 1.5F : 1.0F;
        float[] outDamage = new float[] { totalDmg };

        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            applyElement(level, target, Element.ELECTRO, attackU, attacker, totalDmg, true, outDamage);
            double sweepRadius = (isNetherite ? 5.25D : 3.5D) * (isCharged ? 1.35D : 1.0D);
            AABB sweepBox = target.getBoundingBox().inflate(sweepRadius, 1.75D, sweepRadius);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    e -> e.isAlive() && !e.isSpectator() && e != attacker && e != target && !isRaidenProtectedAlly(attacker, e))) {
                applyElement(level, nearby, Element.ELECTRO, attackU, attacker, bonusDmg * 0.75F);
            }
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }

        Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        electrifyWaterAlongSlash(level, attacker, hitPos, isNetherite ? 4.5D : 3.5D, bonusDmg * 0.45F);
        sendRaidenSlashArc(level, hitPos, attacker.getLookAngle(), comboStep, false, isCharged, (isNetherite ? 2.4D : 1.75D) * (isCharged ? 1.3D : 1.0D), attacker.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        DustParticleOptions slash = new DustParticleOptions(new Vector3f(0.84F, 0.32F, 1.0F), isNetherite ? 1.6F : 1.25F);
        level.sendParticles(slash, hitPos.x, hitPos.y, hitPos.z, 10, 0.45D, 0.3D, 0.45D, 0.03D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y, hitPos.z, 10, 0.4D, 0.3D, 0.4D, 0.07D);
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, isCharged ? 0.95F : 1.35F);
        return outDamage[0];
    }

    public static void performRaidenMusouSwing(ServerPlayer player, boolean isCharged) {
        if (player == null || !player.isAlive()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        boolean musouActive = isRaidenMusouActive(id, now);
        if (!musouActive && !isCharged) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack mainHand = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(mainHand) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            return;
        }
        if (now < RAIDEN_NEXT_SWING_TICK.getOrDefault(id, 0L)) return;
        long lastMelee = RAIDEN_LAST_MELEE_TICK.getOrDefault(id, -100L);
        if (now >= lastMelee && now - lastMelee <= 1L) return;

        RAIDEN_NEXT_SWING_TICK.put(id, now + (isCharged ? 11L : 8L));
        boolean isNetherite = ArcaneStaffItem.getEffectiveNetherite(mainHand);
        int comboStep = RAIDEN_NA_COMBO.getOrDefault(id, 0);
        RAIDEN_NA_COMBO.put(id, isCharged ? 0 : ((comboStep + 1) % 5));

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.ENGULFING_LIGHTNING, isNetherite);
        float finisherMult = (!isCharged && comboStep == 4) ? 1.25F : 1.0F;
        float chargedMult = isCharged ? 1.35F : finisherMult;
        float musouResMult = RAIDEN_PENDING_HITOTACHI_MULT.getOrDefault(id, 1.0F);
        float slashDmg = (float) (5.0D * scale * musouResMult * chargedMult * StavesConfig.getDamageMultiplier());
        float attackU = isNetherite ? 1.5F : 1.0F;
        Vec3 look = player.getLookAngle().normalize();
        double reach = isCharged ? (isNetherite ? 9.5D : 7.5D) : ((isNetherite ? 5.25D : 3.75D) * finisherMult);
        Vec3 slashPos = player.getEyePosition().add(look.scale(reach * 0.55D)).add(0.0D, -0.25D, 0.0D);

        IS_PROCESSING_RAIDEN_PROC.set(true);
        try {
            if (isCharged) {
                java.util.Set<UUID> hitIds = new java.util.HashSet<>();
                for (double frac : new double[]{0.35D, 0.75D}) {
                    Vec3 boxCenter = player.getEyePosition().add(look.scale(reach * frac)).add(0.0D, -0.25D, 0.0D);
                    AABB sweepBox = new AABB(boxCenter, boxCenter).inflate(2.4D, 1.85D, 2.4D);
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                            e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                        if (hitIds.add(target.getUUID())) {
                            applyElement(level, target, Element.ELECTRO, attackU, player, slashDmg);
                        }
                    }
                }
            } else {
                AABB sweepBox = new AABB(slashPos, slashPos).inflate(reach * 0.65D, 1.75D, reach * 0.65D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                        e -> e.isAlive() && !e.isSpectator() && e != player && !isRaidenProtectedAlly(player, e))) {
                    applyElement(level, target, Element.ELECTRO, attackU, player, slashDmg);
                }
            }
        } finally {
            IS_PROCESSING_RAIDEN_PROC.set(false);
        }

        electrifyWaterAlongSlash(level, player, slashPos, isNetherite ? 4.5D : 3.5D, slashDmg * 0.45F);
        sendRaidenSlashArc(level, slashPos, look, comboStep, false, isCharged, (isNetherite ? 2.4D : 1.75D) * (isCharged ? 1.3D : 1.0D), player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        if (isCharged) {
            Vec3 farSlashPos = player.getEyePosition().add(look.scale(reach * 0.85D)).add(0.0D, -0.15D, 0.0D);
            sendRaidenSlashArc(level, farSlashPos, look, (comboStep + 2) % 4, false, true, (isNetherite ? 2.6D : 2.0D) * 1.3D, player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, farSlashPos.x, farSlashPos.y, farSlashPos.z, 10, 0.55D, 0.3D, 0.55D, 0.08D);
        }
        DustParticleOptions slash = new DustParticleOptions(new Vector3f(0.84F, 0.32F, 1.0F), isNetherite ? 1.55F : 1.25F);
        level.sendParticles(slash, slashPos.x, slashPos.y, slashPos.z, 8, 0.45D, 0.25D, 0.45D, 0.03D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, slashPos.x, slashPos.y, slashPos.z, 8, 0.4D, 0.25D, 0.4D, 0.06D);
        level.playSound(null, slashPos.x, slashPos.y, slashPos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, isCharged ? 0.95F : 1.35F);
    }

    public static void tickRaidenState(MinecraftServer server, long serverNow) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            boolean eyeActive = serverNow < RAIDEN_EYE_UNTIL.getOrDefault(id, 0L);
            boolean musouActive = serverNow < RAIDEN_MUSOU_UNTIL.getOrDefault(id, 0L);

            if (musouActive) {
                ItemStack mainHand = player.getMainHandItem();
                if (ArcaneStaffItem.getEffectiveKind(mainHand) != ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                    RAIDEN_MUSOU_UNTIL.remove(id);
                    RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
                    RAIDEN_PENDING_HITOTACHI_MULT.remove(id);
                    musouActive = false;
                    syncRaidenState(player, serverNow);
                } else {
                    Long pendingHitTick = RAIDEN_PENDING_HITOTACHI_TICK.get(id);
                    if (pendingHitTick != null && serverNow >= pendingHitTick) {
                        RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
                        Float pendingMult = RAIDEN_PENDING_HITOTACHI_MULT.getOrDefault(id, 1.0F);
                        if (player.isAlive()) {
                            boolean neth = RAIDEN_MUSOU_NETHERITE.getOrDefault(id, false);
                            executeRaidenHitotachiImpact(player, neth, pendingMult);
                        }
                    }
                    if (serverNow % 3 == 0 && player.level() instanceof ServerLevel sl) {
                        boolean neth = RAIDEN_MUSOU_NETHERITE.getOrDefault(id, false);
                        DustParticleOptions aura = new DustParticleOptions(new Vector3f(0.85F, 0.35F, 1.0F), neth ? 1.3F : 1.05F);
                        sl.sendParticles(aura, player.getX(), player.getY() + 1.05D, player.getZ(), 3, 0.35D, 0.4D, 0.35D, 0.01D);
                        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.05D, player.getZ(), 1, 0.25D, 0.35D, 0.25D, 0.01D);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.05D, player.getZ(), 2, 0.3D, 0.35D, 0.3D, 0.02D);
                    }
                }
            }
            if (!musouActive) {
                RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
                RAIDEN_PENDING_HITOTACHI_MULT.remove(id);
            }

            if ((eyeActive || musouActive || RAIDEN_RESOLVE.getOrDefault(id, 0) > 0 || isRaidenBurstOnCooldown(id, serverNow)) && serverNow % 10 == 0) {
                syncRaidenState(player, serverNow);
            }
            if (!eyeActive && RAIDEN_EYE_UNTIL.containsKey(id)) {
                RAIDEN_EYE_UNTIL.remove(id);
                syncRaidenState(player, serverNow);
            }
        }
    }
}
