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

public final class MavuikaManager {
    private MavuikaManager() {}

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

    public static boolean isMavuikaOnCooldown(UUID id, long now) {
        Long cdUntil = MAVUIKA_SKILL_COOLDOWN_UNTIL.get(id);
        return cdUntil != null && now < cdUntil;
    }

    public static long getMavuikaSkillRemainingTicks(UUID id, long now) {
        return Math.max(0L, MAVUIKA_SKILL_COOLDOWN_UNTIL.getOrDefault(id, 0L) - now);
    }

    public static void applyMavuikaDismountOrRemainingCooldown(ServerPlayer player) {
        if (player == null) return;
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        long remCd = getMavuikaSkillRemainingTicks(player.getUUID(), now);
        int clientCd;
        if (isMavuikaAuraActive(player.getUUID(), now)) {
            clientCd = 12;
        } else {
            long effectiveRem = Math.max(remCd, 100L);
            MAVUIKA_SKILL_COOLDOWN_UNTIL.put(player.getUUID(), Math.max(
                    MAVUIKA_SKILL_COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L),
                    now + effectiveRem));
            clientCd = (int) Math.min(Integer.MAX_VALUE, effectiveRem);
        }
        for (ItemStack invStack : player.getInventory().items) {
            if (invStack.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(invStack) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                player.getCooldowns().addCooldown(staff, clientCd);
            }
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff
                && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(off) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            player.getCooldowns().addCooldown(staff, clientCd);
        }
    }

    public static void activateMavuikaSunAura(ServerPlayer player, boolean isNetherite, int cooldownTicks) {
        if (player == null || player.level().isClientSide()) return;
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        float maxFuel = isNetherite ? 120.0F : 80.0F; // Genshin C0: 80, C1 (Netherite): 120 max Nightsoul points
        MAVUIKA_AURA_UNTIL.put(id, now + 400L);
        MAVUIKA_NIGHTSOUL_FUEL.put(id, maxFuel);
        MAVUIKA_AURA_NETHERITE.put(id, isNetherite);
        MAVUIKA_AURA_NEXT_STRIKE.put(id, now + 20L);
        long cdTicks = Math.max(300L, cooldownTicks > 0 ? cooldownTicks : 300L);
        MAVUIKA_SKILL_COOLDOWN_UNTIL.put(id, now + cdTicks);
        float pts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
        com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, Math.round(maxFuel * 10.0F), pts, isNetherite);
        for (int a = 0; a < 12; a++) {
            double ang = a * (Math.PI * 2.0D / 12.0D);
            double px = player.getX() + Math.cos(ang) * 0.9D;
            double pz = player.getZ() + Math.sin(ang) * 0.9D;
            level.sendParticles(ParticleTypes.FLAME, px, player.getY() + 1.05D, pz, 2, 0.05D, 0.12D, 0.05D, 0.02D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.85F, 1.1F);
    }

    public static boolean isMavuikaInCombat(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return false;
        ServerLevel level = player.serverLevel();
        AABB nearBox = player.getBoundingBox().inflate(14.0D, 6.0D, 14.0D);
        return !level.getEntitiesOfClass(LivingEntity.class, nearBox,
                e -> e.isAlive() && !e.isRemoved() && !(e instanceof Player) && !isPassiveAquaticOrAmbient(e)).isEmpty();
    }

    public static void consumeMavuikaNightsoulFuel(ServerPlayer player, float amount) {
        if (player == null || amount <= 0.0F) return;
        UUID id = player.getUUID();
        long now = player.serverLevel().getServer().overworld().getGameTime();
        if (now < MAVUIKA_CRUCIBLE_UNTIL.getOrDefault(id, 0L)) return; // Crucible of Death and Life: no Nightsoul cost
        Float cur = MAVUIKA_NIGHTSOUL_FUEL.get(id);
        if (cur == null || cur <= 0.0F) return;
        float next = Math.max(0.0F, cur - amount);
        float drained = cur - next;
        MAVUIKA_NIGHTSOUL_FUEL.put(id, next);
        if (drained > 0.0F && isMavuikaInCombat(player)) {
            // Genshin: Only when in combat do consumed Nightsoul points convert to Fighting Spirit (C1 Netherite: +25% efficiency)
            boolean neth = MAVUIKA_AURA_NETHERITE.getOrDefault(id, false);
            float fsEfficiency = neth ? 1.25F : 1.0F;
            float curPts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
            MAVUIKA_NIGHTSOUL_POINTS.put(id, Math.min(10.0F, curPts + drained * 0.05F * fsEfficiency));
        }
    }

    public static void addMavuikaFightingSpiritFromExternalNightsoul(ServerPlayer player, float nightsoulConsumed) {
        if (player == null || nightsoulConsumed <= 0.0F) return;
        boolean hasSuns = false;
        boolean nethSuns = false;
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                hasSuns = true;
                if (com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(s)) nethSuns = true;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                hasSuns = true;
                if (com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(s)) nethSuns = true;
            }
        }
        UUID id = player.getUUID();
        if (!hasSuns && !MAVUIKA_NIGHTSOUL_POINTS.containsKey(id)) return;
        boolean neth = MAVUIKA_AURA_NETHERITE.getOrDefault(id, nethSuns);
        float fsEfficiency = neth ? 1.25F : 1.0F;
        float curPts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
        float updated = Math.min(10.0F, curPts + nightsoulConsumed * 0.05F * fsEfficiency);
        if (updated != curPts) {
            MAVUIKA_NIGHTSOUL_POINTS.put(id, updated);
            int rem = Math.round(MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F) * 10.0F);
            com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, rem, updated, neth);
        }
    }

    public static void addMavuikaNormalAttackFightingSpirit(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        UUID id = player.getUUID();
        long now = player.serverLevel().getServer().overworld().getGameTime();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean holdingMain = com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(main) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS;
        boolean holdingOff = com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(off) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS;
        if (!holdingMain && !holdingOff && !isMavuikaAuraActive(id, now)) return;
        long lastNaFs = MAVUIKA_LAST_NA_FS_TICK.getOrDefault(id, -100L);
        if (now >= lastNaFs && now - lastNaFs < 2L) return; // Genshin A1 passive: 0.1s ICD
        MAVUIKA_LAST_NA_FS_TICK.put(id, now);
        boolean neth = MAVUIKA_AURA_NETHERITE.getOrDefault(id,
                (holdingMain && com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(main))
                        || (holdingOff && com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(off)));
        float fsEfficiency = neth ? 1.25F : 1.0F;
        float curPts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
        float updated = Math.min(10.0F, curPts + 0.075F * fsEfficiency); // Genshin: +1.5 Fighting Spirit per NA hit (C1: +25%)
        if (updated != curPts) {
            MAVUIKA_NIGHTSOUL_POINTS.put(id, updated);
            int rem = Math.round(MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F) * 10.0F);
            com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, rem, updated, neth);
        }
    }

    public static boolean isMavuikaAuraActive(UUID id, long now) {
        return MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F) > 0.01F;
    }

    public static void summonAndMountFlamestrider(ServerPlayer player, boolean isNetherite, int cooldownTicks) {
        summonAndMountFlamestrider(player, isNetherite, cooldownTicks, true);
    }

    public static void summonAndMountFlamestrider(ServerPlayer player, boolean isNetherite, int cooldownTicks, boolean dealCastSweep) {
        if (player == null || player.level().isClientSide()) return;
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        boolean firstCast = !isMavuikaAuraActive(player.getUUID(), now);
        if (firstCast) {
            activateMavuikaSunAura(player, isNetherite, cooldownTicks);
        }
        if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) {
            return;
        }
        com.velorise.staves.entity.FlamestriderBikeEntity bike =
                com.velorise.staves.VeloriseStavesMod.getFlamestriderBikeType().create(level);
        if (bike == null) return;
        bike.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        bike.setOwner(player);
        bike.setNetherite(isNetherite);
        level.addFreshEntity(bike);
        player.startRiding(bike, true);
        if (firstCast && dealCastSweep) {
            // Genshin Hold E (first cast): initial forward bike tail-whip Pyro sweep on mount
            float castDmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, isNetherite) * 0.65F;
            Vec3 fwd = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
            Vec3 sweepCenter = player.position().add(fwd.scale(2.0D)).add(0.0D, 0.5D, 0.0D);
            AABB sweepBox = new AABB(sweepCenter.x - 3.6D, sweepCenter.y - 1.5D, sweepCenter.z - 3.6D,
                    sweepCenter.x + 3.6D, sweepCenter.y + 2.2D, sweepCenter.z + 3.6D);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    e -> e.isAlive() && !e.isRemoved() && e != player && !isPassiveAquaticOrAmbient(e))) {
                applyElement(level, target, Element.PYRO, isNetherite ? 1.5F : 1.0F, player, castDmg);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        10, 0.25D, 0.25D, 0.25D, 0.05D);
            }
            bike.triggerMountWhip();
        }
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5D, player.getZ(), 22, 0.55D, 0.3D, 0.55D, 0.07D);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 0.95F);
    }

    public static void toggleMavuikaBike(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
            player.stopRiding();
            bike.discard();
            return;
        }
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack matched = ItemStack.EMPTY;
        if (com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(main) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            matched = main;
        } else if (!com.velorise.staves.item.ArcaneStaffItem.isGenshinCharacterWeapon(main)
                && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(off) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            matched = off;
        }
        if (matched.isEmpty()) return;
        long now = player.serverLevel().getServer().overworld().getGameTime();
        if (!isMavuikaAuraActive(player.getUUID(), now) && isMavuikaOnCooldown(player.getUUID(), now)) return;
        summonAndMountFlamestrider(player, com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(matched), 300);
    }

    public static int getMavuikaUltiCooldownTicks(net.minecraft.world.entity.Entity target) {
        if (!(target instanceof ServerPlayer sp)) return 0;
        long now = sp.serverLevel().getServer().overworld().getGameTime();
        return (int) Math.max(0L, MAVUIKA_ULTI_COOLDOWN_UNTIL.getOrDefault(sp.getUUID(), 0L) - now);
    }

    public static float getMavuikaCrucibleNaBonusMultiplier(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return 0.0F;
        long now = player.serverLevel().getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (now >= MAVUIKA_CRUCIBLE_UNTIL.getOrDefault(id, 0L)) return 0.0F;
        // Genshin Lv.10 Crucible: +0.51% ATK per Fighting Spirit (1.0 mod pt = 20 FS; 1.0x baseDmg ~= 200% ATK)
        return MAVUIKA_CRUCIBLE_POINTS.getOrDefault(id, 0.0F) * 0.051F;
    }

    public static float getMavuikaCrucibleChargedBonusMultiplier(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return 0.0F;
        long now = player.serverLevel().getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (now >= MAVUIKA_CRUCIBLE_UNTIL.getOrDefault(id, 0L)) return 0.0F;
        // Genshin Lv.10 Crucible: +1.02% ATK per Fighting Spirit (1.0 mod pt = 20 FS; 1.0x baseDmg ~= 200% ATK)
        return MAVUIKA_CRUCIBLE_POINTS.getOrDefault(id, 0.0F) * 0.102F;
    }

    public static void triggerMavuikaUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack sunsStack = ItemStack.EMPTY;
        if (com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(main) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            sunsStack = main;
        } else if (!com.velorise.staves.item.ArcaneStaffItem.isGenshinCharacterWeapon(main)
                && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(off) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            sunsStack = off;
        }
        if (sunsStack.isEmpty()) return;
        boolean isNetherite = com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(sunsStack);

        UUID id = player.getUUID();
        ServerLevel level = player.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        long cdUntil = MAVUIKA_ULTI_COOLDOWN_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            ArchonUltiManager.sendMinimalistBurstCooldownAlert(player, Element.PYRO, cdUntil - now);
            return;
        }
        float pts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
        if (pts < 5.0F) return;
        MAVUIKA_ULTI_COOLDOWN_UNTIL.put(id, now + 360L);
        float clampedPts = Math.min(10.0F, pts);
        // Genshin Lv.10 Sunfell Slice: 800.64% + 2.9% * FS (1.0 mod pt = 20 FS; 1.0x baseDmg ~= 200% ATK -> 4.00x + 0.29x/pt)
        float baseDmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, isNetherite);
        float ultiDmg = baseDmg * (4.00F + clampedPts * 0.29F);

        MAVUIKA_NIGHTSOUL_POINTS.put(id, 0.0F);
        if (!isMavuikaAuraActive(id, now)) {
            MAVUIKA_AURA_UNTIL.put(id, now + 140L);
            MAVUIKA_AURA_NETHERITE.put(id, isNetherite);
            MAVUIKA_AURA_NEXT_STRIKE.put(id, now + 20L);
        } else {
            MAVUIKA_AURA_UNTIL.put(id, Math.max(MAVUIKA_AURA_UNTIL.getOrDefault(id, 0L), now + 140L));
        }

        MAVUIKA_NIGHTSOUL_FUEL.put(id, Math.max(MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F), 60.0F));
        if (!(player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
            summonAndMountFlamestrider(player, isNetherite, 0, false);
        }
        ArchonUltiManager.grantUltiIframe(player, 34); // 1.7s Genshin Q I-frames
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_MAVUIKA_BURST);
        MAVUIKA_CRUCIBLE_UNTIL.put(id, now + 140L); // Genshin Q: 7s Crucible of Death and Life (no Nightsoul drain)
        MAVUIKA_CRUCIBLE_POINTS.put(id, clampedPts);
        if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
            bike.startUltiSlam(ultiDmg);
        } else {
            executeMavuikaUltiImpact(level, player, ultiDmg);
        }

        int rem = Math.round(MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F) * 10.0F);
        com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, rem, 0.0F, isNetherite);
        com.velorise.staves.advancement.ModAdvancements.trigger(player,
                com.velorise.staves.advancement.ModAdvancements.EVENT_RAT_FROM_THE_SKY);
    }

    public static void executeMavuikaUltiImpact(ServerLevel level, ServerPlayer player, float ultiDmg) {
        if (level == null || player == null || !player.isAlive()) return;
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
        Vec3 origin = player.position();
        AABB box = player.getBoundingBox().inflate(5.5D, 2.5D, 5.5D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != player);
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(origin);
            double distSqr = toTarget.x * toTarget.x + toTarget.z * toTarget.z;
            if (distSqr > 25.0D || Math.abs(toTarget.y) > 3.2D) continue;
            Vec3 horiz = new Vec3(toTarget.x, 0.0D, toTarget.z);
            if (horiz.lengthSqr() > 1.0E-4D && horiz.normalize().dot(forward) < 0.0D) continue;
            applyElement(level, target, Element.PYRO, 2.0F, player, ultiDmg);
        }

        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x).normalize();
        Vec3 slashStart = origin.add(forward.scale(2.1D)).subtract(right.scale(4.4D)).add(0.0D, 0.55D, 0.0D);
        Vec3 slashEnd = origin.add(forward.scale(4.2D)).add(right.scale(4.6D)).add(0.0D, 2.35D, 0.0D);
        com.velorise.staves.network.ModNetwork.sendElectricArc(level, slashStart, slashEnd, 14, 0xFFFF6A00);
        for (int k = 0; k <= 12; k++) {
            double frac = k / 12.0D;
            double bow = Math.sin(frac * Math.PI) * 2.2D;
            Vec3 slashPt = slashStart.lerp(slashEnd, frac).add(forward.scale(bow));
            level.sendParticles(ParticleTypes.FLAME, slashPt.x, slashPt.y, slashPt.z, 3, 0.10D, 0.10D, 0.10D, 0.03D);
            if (k % 2 == 0) {
                level.sendParticles(ParticleTypes.LAVA, slashPt.x, slashPt.y, slashPt.z, 1, 0.06D, 0.06D, 0.06D, 0.01D);
            }
        }

        Vec3 slamCenter = origin.add(forward.scale(2.8D));
        level.sendParticles(ParticleTypes.EXPLOSION, slamCenter.x, slamCenter.y + 0.8D, slamCenter.z, 2, 0.6D, 0.3D, 0.6D, 0.0D);
        for (int a = 0; a < 360; a += 15) {
            Vec3 dir = Vec3.directionFromRotation(0.0F, player.getYRot() + a);
            Vec3 ringPos = origin.add(dir.scale(3.2D));
            level.sendParticles(ParticleTypes.FLAME, ringPos.x, origin.y + 0.18D, ringPos.z, 2, 0.08D, 0.05D, 0.08D, 0.02D);
            if (a >= -90 && a <= 90) {
                for (double r = 1.8D; r <= 5.0D; r += 1.4D) {
                    Vec3 p = origin.add(dir.scale(r));
                    level.sendParticles(ParticleTypes.FLAME, p.x, origin.y + 0.4D, p.z, 2, 0.12D, 0.18D, 0.12D, 0.04D);
                    level.sendParticles(ParticleTypes.LAVA, p.x, origin.y + 0.5D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    public static void tickMavuikaSunAura(MinecraftServer server, long serverNow) {
        if (MAVUIKA_NIGHTSOUL_FUEL.isEmpty() && MAVUIKA_AURA_UNTIL.isEmpty() && MAVUIKA_NIGHTSOUL_POINTS.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            float fuel = MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F);
            boolean active = fuel > 0.01F;
            boolean neth = MAVUIKA_AURA_NETHERITE.getOrDefault(id, false);
            if (!active) {
                if (MAVUIKA_AURA_UNTIL.remove(id) != null || MAVUIKA_NIGHTSOUL_FUEL.remove(id) != null) {
                    if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
                        if (!bike.isSpinning() && !bike.isSpinInputHeld() && !bike.isFinisherPlaying()
                                && !bike.isPlunging() && !bike.isDashingServer() && bike.getUltiTicks() <= 0) {
                            player.stopRiding();
                            bike.discard();
                        }
                    }
                    float pts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
                    com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, 0, pts, neth);
                    applyMavuikaDismountOrRemainingCooldown(player);
                }
                continue;
            }
            if (!player.isAlive()) {
                MAVUIKA_AURA_UNTIL.remove(id);
                MAVUIKA_NIGHTSOUL_FUEL.remove(id);
                MAVUIKA_NIGHTSOUL_POINTS.remove(id);
                com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, 0, 0.0F, neth);
                continue;
            }

            ServerLevel level = player.serverLevel();
            boolean onBike = player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity;
            boolean crucibleActive = serverNow < MAVUIKA_CRUCIBLE_UNTIL.getOrDefault(id, 0L);
            float drainPerTick = 0.325F; // ~6.5 pts/s off-bike Sun Ring (~12.3s)
            if (crucibleActive) {
                drainPerTick = 0.0F; // Crucible of Death and Life: 7s without Nightsoul consumption
            } else if (onBike && player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
                drainPerTick = 0.45F; // 9 pts/s base riding
                if (bike.isDashingServer()) {
                    drainPerTick = 1.45F; // 29 pts/s while dashing
                } else if (bike.isInWater()) {
                    drainPerTick = 0.70F; // 14 pts/s surfing water
                } else if (bike.isGliding()) {
                    drainPerTick = 0.60F; // 12 pts/s gliding mid-air
                } else if (bike.isBikeSprinting()) {
                    drainPerTick = 0.70F; // 14 pts/s continuous hold-sprint
                } else if (bike.isSpinning()) {
                    drainPerTick = 0.55F; // 11 pts/s Charged Attack
                }
            }
            float prevFuel = fuel;
            fuel = Math.max(0.0F, fuel - drainPerTick);
            float drained = prevFuel - fuel;
            if (drained > 0.0F && isMavuikaInCombat(player)) {
                float fsEfficiency = neth ? 1.25F : 1.0F;
                float curPts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
                MAVUIKA_NIGHTSOUL_POINTS.put(id, Math.min(10.0F, curPts + drained * 0.05F * fsEfficiency));
            }
            MAVUIKA_NIGHTSOUL_FUEL.put(id, fuel);
            if (fuel <= 0.01F) {
                MAVUIKA_NIGHTSOUL_FUEL.remove(id);
                MAVUIKA_AURA_UNTIL.remove(id);
                if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
                    if (!bike.isSpinning() && !bike.isSpinInputHeld() && !bike.isFinisherPlaying()
                            && !bike.isPlunging() && !bike.isDashingServer() && bike.getUltiTicks() <= 0) {
                        player.stopRiding();
                        bike.discard();
                    }
                }
                float pts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
                com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, 0, pts, neth);
                applyMavuikaDismountOrRemainingCooldown(player);
                continue;
            }
            long nextStrike = MAVUIKA_AURA_NEXT_STRIKE.getOrDefault(id, 0L);
            if (!onBike && serverNow >= nextStrike) {
                MAVUIKA_AURA_NEXT_STRIKE.put(id, serverNow + 40L);
                double radius = neth ? 8.0D : 6.0D;
                AABB box = player.getBoundingBox().inflate(radius, 3.5D, radius);
                List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                        cand -> cand.isAlive() && !cand.isRemoved() && cand != player && !isPassiveAquaticOrAmbient(cand)
                                && cand.distanceToSqr(player) <= radius * radius);
                if (!candidates.isEmpty()) {
                    candidates.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)));
                    int maxTargets = Math.min(10, candidates.size()); // Genshin Ring of Searing Radiance: up to 10 opponents
                    float strikeDmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth) * 0.5F;
                    for (int ti = 0; ti < maxTargets; ti++) {
                        LivingEntity target = candidates.get(ti);
                        applyElement(level, target, Element.PYRO, neth ? 1.5F : 1.0F, player, strikeDmg);
                        for (int h = 0; h < 6; h++) {
                            level.sendParticles(ParticleTypes.FLAME,
                                    target.getX(), target.getY() + target.getBbHeight() + 0.3D + h * 0.45D, target.getZ(),
                                    2, 0.08D, 0.08D, 0.08D, 0.02D);
                        }
                    }
                    level.playSound(null, candidates.get(0).blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.55F, 1.35F);
                }
            }

            if (serverNow % 2L == 0L) {
                int rem = Math.max(1, Math.round(fuel * 10.0F));
                float pts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
                com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, rem, pts, neth);
            }
        }
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
                    target.igniteForSeconds(5);
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

    public static boolean isHostileMob(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) return false;
        if (entity instanceof Npc) return false;
        if (entity instanceof Animal) return false;
        if (entity instanceof AbstractGolem) return false;
        if (isBossEntity(entity) || isPassiveAquaticOrAmbient(entity)) return false;
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }


    private static final Map<UUID, UUID> TRADER_LAST_CUSTOMER = new ConcurrentHashMap<>();

    public static void tryPopulateWanderingTraderOffers(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.npc.WanderingTrader trader) || entity.level().isClientSide()) return;
        if (trader.getTags().contains("VeloriseTraderChecked")) return;
        trader.addTag("VeloriseTraderChecked");

        // 50% chance for a spawned Wandering Trader to offer 2 to 3 Elemental Essence slots
        if (trader.getRandom().nextDouble() >= 0.50D) return;

        net.minecraft.world.item.trading.MerchantOffers offers = trader.getOffers();
        if (offers == null) return;

        int slotCount = 2 + trader.getRandom().nextInt(2); // 2 to 3 slots
        Element[] allElements = Element.values();
        Set<net.minecraft.world.item.Item> addedItems = new HashSet<>();

        for (int s = 0; s < slotCount; s++) {
            // 7 parts Normal Element (87.5%) : 1 part Omni Element (12.5%)
            boolean rollOmni = (trader.getRandom().nextInt(8) == 0);
            net.minecraft.world.item.Item chosenItem = null;
            int emeraldPrice;
            int xpReward;

            if (rollOmni && !addedItems.contains(VeloriseStavesMod.getElementItem(null))) {
                chosenItem = VeloriseStavesMod.getElementItem(null);
                emeraldPrice = 48 + trader.getRandom().nextInt(17); // 48 - 64 Emeralds
                xpReward = 25;
            } else {
                for (int attempt = 0; attempt < 10; attempt++) {
                    Element pick = allElements[trader.getRandom().nextInt(allElements.length)];
                    net.minecraft.world.item.Item candidate = VeloriseStavesMod.getElementItem(pick);
                    if (candidate != null && !addedItems.contains(candidate)) {
                        chosenItem = candidate;
                        break;
                    }
                }
                if (chosenItem == null) {
                    chosenItem = VeloriseStavesMod.getElementItem(allElements[s % allElements.length]);
                }
                emeraldPrice = 24 + trader.getRandom().nextInt(9); // 24 - 32 Emeralds
                xpReward = 12;
            }

            if (chosenItem != null && addedItems.add(chosenItem)) {
                offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD, emeraldPrice), new ItemStack(chosenItem, 1), 1, xpReward, 0.05F));
            }
        }
    }

    public static void checkWanderingTraderProgress(ServerLevel level, net.minecraft.world.entity.npc.WanderingTrader trader) {
        if (level == null || trader == null || !trader.isAlive()) return;
        tryPopulateWanderingTraderOffers(trader);
        if (trader.getTradingPlayer() instanceof ServerPlayer activeCustomer) {
            TRADER_LAST_CUSTOMER.put(trader.getUUID(), activeCustomer.getUUID());
        }
        net.minecraft.world.item.trading.MerchantOffers offers = trader.getOffers();
        if (offers == null || offers.isEmpty()) return;
        for (int i = 0; i < offers.size(); i++) {
            net.minecraft.world.item.trading.MerchantOffer offer = offers.get(i);
            if (offer != null && offer.getUses() > 0 && offer.getResult().getItem() instanceof com.velorise.staves.item.ElementItem) {
                String grantTag = "VeloriseTraderSoldSlot:" + i;
                if (!trader.getTags().contains(grantTag)) {
                    trader.addTag(grantTag);
                    ServerPlayer buyer = null;
                    if (trader.getTradingPlayer() instanceof ServerPlayer sp) {
                        buyer = sp;
                    } else {
                        UUID lastId = TRADER_LAST_CUSTOMER.get(trader.getUUID());
                        if (lastId != null && level.getServer() != null) {
                            buyer = level.getServer().getPlayerList().getPlayer(lastId);
                        }
                    }
                    if (buyer != null) {
                        com.velorise.staves.advancement.ModAdvancements.trigger(
                                buyer, com.velorise.staves.advancement.ModAdvancements.EVENT_WHERE_IS_HE_FROM);
                    }
                }
            }
        }
    }

    public static void checkInnateMutation(LivingEntity entity) {
        if (entity == null || entity instanceof Player || entity.level().isClientSide()) return;
        if (entity instanceof net.minecraft.world.entity.npc.WanderingTrader) {
            tryPopulateWanderingTraderOffers(entity);
        }
        if (entity instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE) && !isEntityFrozen(mob)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
        }

        if (!entity.getTags().contains("VeloriseInnateChecked")) {
            entity.addTag("VeloriseInnateChecked");
            rollInnateMutationOnce(entity);
        }

        if (entity.level() instanceof ServerLevel slInit) {
            updateCoopMobScaling(slInit, entity);
        }
    }

    public static void rollInnateMutationOnce(LivingEntity entity) {
        // If the entity already has a natural innate element (other than non-mutated Charged Creeper lightning), do not mutate it
        if (!(entity instanceof Creeper) && getNaturalInnateElement(entity) != null) return;

        StavesConfig.MutationRules rules = StavesConfig.getMutationRules();
        double mutateChance = rules.resolveMutationChance(entity);
        if (mutateChance <= 0.0D) return;

        double roll = entity.getRandom().nextDouble();
        if (roll < mutateChance) {
            if (rules.omniShare > 0.0D && roll < (mutateChance * rules.omniShare)) {
                entity.addTag(TAG_INNATE_OMNI);
                entity.addTag("VeloriseInnate:OMNI");
                OMNI_INNATE_ENTITIES.add(entity.getUUID());
                extinguishPyroInnate(entity);
                com.velorise.staves.network.ModNetwork.sendAuraSync(entity, Collections.emptyList(), null, null, null, 0.0F, -2);
                return;
            }
            Element[] elements = Element.values();
            Element chosen = elements[entity.getRandom().nextInt(elements.length)];
            entity.addTag("VeloriseInnate:" + chosen.name());
            if (chosen == Element.PYRO) {
                extinguishPyroInnate(entity);
            }

            // Immediately initialize mutated innate element(s) in GAUGES (including Charged Mutated Creeper dual aura)
            EnumSet<Element> innateSet = getInnateElements(entity);
            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(entity.getUUID(), k -> new ConcurrentHashMap<>());
            for (Element keep : innateSet) {
                gauges.put(keep, new GaugeState(keep, 1.25F));
            }
            syncAuras(entity, innateSet, null, null, null, 0.0F);
        }
    }

    public static boolean isOmniInnate(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living) || living instanceof Player) return false;
        if (!living.level().isClientSide() && !living.getTags().contains("VeloriseInnateChecked")) {
            checkInnateMutation(living);
        }
        if (OMNI_INNATE_ENTITIES.contains(living.getUUID())) return true;
        if (living.getTags().contains(TAG_INNATE_OMNI) || living.getTags().contains("VeloriseInnate:OMNI")) {
            OMNI_INNATE_ENTITIES.add(living.getUUID());
            return true;
        }
        return false;
    }

    public static @Nullable Element getInnateElement(LivingEntity target) {
        if (target == null || target instanceof Player) return null;
        if (!target.level().isClientSide() && !target.getTags().contains("VeloriseInnateChecked")) {
            checkInnateMutation(target);
        }
        if (target.getTags().contains(TAG_INNATE_OMNI) || target.getTags().contains("VeloriseInnate:OMNI")) {
            OMNI_INNATE_ENTITIES.add(target.getUUID());
            return null;
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

    public static EnumSet<Element> getInnateElements(@Nullable LivingEntity target) {
        EnumSet<Element> set = EnumSet.noneOf(Element.class);
        if (target == null) return set;
        if (isOmniInnate(target)) return set;
        Element primary = getInnateElement(target);
        if (primary != null) {
            set.add(primary);
        }
        if (target instanceof Creeper creeper && creeper.isPowered()) {
            set.add(Element.ELECTRO);
        }
        return set;
    }

    public static boolean hasInnateElement(@Nullable LivingEntity target, @Nullable Element element) {
        return target != null && element != null && getInnateElements(target).contains(element);
    }

    public static boolean isPyroInnate(@Nullable Entity entity) {
        return entity instanceof LivingEntity living && hasInnateElement(living, Element.PYRO);
    }

    public static boolean isPyroResistant(@Nullable LivingEntity target) {
        return target != null && (target instanceof Warden || target.fireImmune() || target.hasEffect(MobEffects.FIRE_RESISTANCE));
    }

    public static void ensurePreDeathPyroIgnite(@Nullable LivingEntity target, int seconds) {
        if (target == null || !target.isAlive() || seconds <= 0) return;
        if (isPyroInnate(target) || isPyroResistant(target) || isEntityInWater(target)) return;
        if (hasElement(target, Element.ELECTRO) || hasInnateElement(target, Element.ELECTRO)) return;
        if (target.getRemainingFireTicks() < seconds * 20) {
            target.igniteForSeconds(seconds);
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
        for (String tag : entity.getTags()) {
            if (tag.startsWith("VeloriseInnate:")) return true;
        }
        return false;
    }

    public static void tryDropElementOnDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(entity instanceof Enemy) && !isMutatedInnate(entity) && !isOmniInnate(entity)) return;
        if (entity instanceof Player || entity instanceof Npc) return;
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
        if (target instanceof Breeze
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
        if (target instanceof Bogged || target instanceof MushroomCow
                || target instanceof Bee || target instanceof CaveSpider
                || (target instanceof Slime && !(target instanceof MagmaCube))) {
            return Element.DENDRO;
        }
        if (target instanceof Armadillo) {
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

    public static void spawnAmbientParticles(ServerLevel level, LivingEntity target, Map<Element, GaugeState> gauges, long now) {
        if (!canEmitReactionVisual(level, target.position(), 6)) return;
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.65D;
        double z = target.getZ();

        if (gauges.containsKey(Element.PYRO)) {
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.2D, 0.25D, 0.2D, 0.01D);
        }
        if (gauges.containsKey(Element.HYDRO)) {
            level.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 1, 0.2D, 0.25D, 0.2D, 0.0D);
        }
        if (gauges.containsKey(Element.CRYO) || FROZEN_GAUGES.containsKey(target.getUUID())) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0.25D, 0.25D, 0.25D, 0.01D);
        }
        if (gauges.containsKey(Element.ELECTRO)) {
            // ParticleTypes.ELECTRIC_SPARK removed
        }
        if (gauges.containsKey(Element.DENDRO) || QUICKEN_GAUGES.containsKey(target.getUUID())) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 1, 0.2D, 0.25D, 0.2D, 0.01D);
        }
    }

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

    public static boolean isMavuikaBikeIframeActive(@Nullable Entity entity, @Nullable DamageSource source) {
        if (entity == null || (source != null && source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))) {
            return false;
        }
        com.velorise.staves.entity.FlamestriderBikeEntity bike = null;
        if (entity instanceof com.velorise.staves.entity.FlamestriderBikeEntity b) {
            bike = b;
        } else if (entity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity b) {
            bike = b;
        }
        return bike != null && (bike.isDashIframeActive() || bike.getUltiTicks() > 0);
    }

    public static void tickActivePlayersAndEnvironment(MinecraftServer server, long serverNow) {
        // 7. Environmental water, fire, lingering potion clouds & cryo checks on active online players
        tickNightsFocusProjectiles();
        tickMavuikaSunAura(server, serverNow);
        tickRaidenState(server, serverNow);
        ArchonUltiManager.tick(server, serverNow);
        Set<UUID> visitedNearby = new HashSet<>();
        Set<UUID> visitedClouds = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isDeadOrDying()) {
                clearEntityState(player);
                continue;
            }
            updatePlayerHpTracking(player, player.level().getGameTime());
            if (!player.isShiftKeyDown() && (com.velorise.staves.entity.HydroSalonSummonEntity.hasActiveSummon(player) || isChannelingGuardianLens(player))) {
                applyFurinaWaterWalking(player);
            }
            if (hasActiveJadeShield(player) && player.level() instanceof ServerLevel jsl && serverNow % 10 == 0) {
                jsl.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.0D, player.getZ(), 3, 0.35D, 0.45D, 0.35D, 0.01D);
            }
            tickNightsFocusShield(player, serverNow);
            ElementalResonanceManager.tickPlayer(player);
            if (!player.isSpectator()) {
                if (isEntityInWater(player)) {
                    handleEnvironmentalWater(player);
                } else if (isTouchingEnvironmentalFireBlock(player) || isTouchingSoulCampfire(player)) {
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
                        if (nearby instanceof net.minecraft.world.entity.npc.WanderingTrader trader) {
                            checkWanderingTraderProgress(sl, trader);
                        }
                        if (nearby instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE) && !isEntityFrozen(mob)) {
                            mob.setNoAi(false);
                            mob.removeTag(TAG_FROZEN_STATUE);
                        }
                        if (serverNow % 20 == 0) {
                            updateCoopMobScaling(sl, nearby);
                        }
                        EnumSet<Element> nearbyInnates = getInnateElements(nearby);
                        if (nearbyInnates.contains(Element.CRYO) || hasElement(nearby, Element.CRYO)) {
                            checkCryoWaterFreezing(nearby);
                        }
                        if (!nearbyInnates.isEmpty() && serverNow % 20 == 0) {
                            if (isEntityInWater(nearby)) {
                                handleEnvironmentalWater(nearby);
                            } else if (isTouchingEnvironmentalFireBlock(nearby) || isTouchingSoulCampfire(nearby)) {
                                maintainBurningPyroAura(nearby);
                            }
                        }
                        if (serverNow % 5 == 0 && !isPassiveAquaticOrAmbient(nearby)) {
                            tryApplyMushroomContactAura(sl, nearby, sl.getGameTime());
                        }
                        if (isOmniInnate(nearby)) {
                            OMNI_INNATE_ENTITIES.add(nearby.getUUID());
                            extinguishPyroInnate(nearby);
                            if (serverNow % 20 == 0 && nearby instanceof net.minecraft.world.entity.TamableAnimal ta
                                    && ta.isTame() && player.getUUID().equals(ta.getOwnerUUID())) {
                                com.velorise.staves.advancement.ModAdvancements.trigger(
                                        player, com.velorise.staves.advancement.ModAdvancements.EVENT_TAME_OMNI_PET);
                            }
                            if (serverNow % 40 == 0) {
                                com.velorise.staves.network.ModNetwork.sendAuraSync(nearby, Collections.emptyList(), null, null, null, 0.0F, -2);
                            }
                            continue;
                        }
                        // Ensure Powered Creeper and ALL Mutated Mobs stay registered in GAUGES (preserving dual innates!) and synced to clients
                        if ((nearby instanceof Creeper creeper && creeper.isPowered())
                                || (!nearbyInnates.isEmpty() && isMutatedInnate(nearby))) {
                            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(nearby.getUUID(), k -> new ConcurrentHashMap<>());
                            boolean missingInnate = false;
                            for (Element keep : nearbyInnates) {
                                if (!gauges.containsKey(keep)) {
                                    gauges.put(keep, new GaugeState(keep, 1.25F));
                                    missingInnate = true;
                                }
                            }
                            if (missingInnate || serverNow % 40 == 0) {
                                syncAuras(nearby, gauges.keySet(), null, null, null);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void clearEntityStateInternal(@Nullable LivingEntity entity) {
        if (entity == null) return;
        UUID id = entity.getUUID();
        PlayerGenshinState.remove(id);
        GAUGES.remove(id);
        FROZEN_GAUGES.remove(id);
        QUICKEN_GAUGES.remove(id);
        FROZEN_UNTIL.remove(id);
        FROZEN_POSES.remove(id);
        LAST_FREEZE_STRUGGLE.remove(id);
        QUICKEN_UNTIL.remove(id);
        BURNING_UNTIL.remove(id);
        BURNING_EM_LEVEL.remove(id);
        BURNING_ATTACKER_UUID.remove(id);
        ELECTRO_CHARGE_EM_LEVEL.remove(id);
        SUPPRESSED_PHYSICAL_FLOAT_TICK.remove(id);
        ElementalDeathMessageHelper.clearEntityState(entity);
        LAST_BURN_PYRO_REFRESH.remove(id);
        INNATE_COOLDOWN_UNTIL.remove(id);
        ELECTRO_CHARGE_TICK.remove(id);
        LAST_BLOOM_SPAWN.remove(id);
        SUPERCONDUCT_UNTIL.remove(id);
        OMNI_INNATE_ENTITIES.remove(id);
        LAST_OMNI_ENV_IMMUNE.remove(id);
        LAST_MUSHROOM_CONTACT.remove(id);
        PLAYER_LAST_HP.remove(id);
        PRE_ATTACK_STRENGTH.remove(id);
        PLAYER_HP_CHANGE_BUFF_UNTIL.remove(id);
        DRACONIC_GLORIES_STACKS.remove(id);
        FURINA_FANFARE_UNTIL.remove(id);
        FURINA_FANFARE_POINTS.remove(id);
        FURINA_FANFARE_NETHERITE.remove(id);
        JADE_SHIELD_UNTIL.remove(id);
        JADE_SHIELD_NETHERITE.remove(id);
        NIGHTS_FOCUS_UNTIL.remove(id);
        NIGHTS_FOCUS_NETHERITE.remove(id);
        NIGHTS_FOCUS_STARS.remove(id);
        NIGHTS_FOCUS_LAST_AUTO_STAR.remove(id);
        NIGHTS_FOCUS_LAST_SKILL_STAR.remove(id);
        NIGHTS_FOCUS_BASE_HP.remove(id);
        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);
        LAYLA_DREAMSPHERE_UNTIL.remove(id);
        LAYLA_DREAMSPHERE_NETHERITE.remove(id);
        LAYLA_DREAMSPHERE_NEXT_SHOT.remove(id);
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
        ArchonUltiManager.clearEntityState(id);
        NahidaManager.clearPlayerState(id);
        TsaritsaManager.clearPlayer(id);
        PETRIFIED_UNTIL.remove(id);
        LAST_ELEMENTAL_ARMOR_WEAR.remove(id);
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

    public static void clearAllInternal() {
        PlayerGenshinState.clearAll();
        NahidaManager.clearAll();
        TsaritsaManager.clearAll();
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
        EXPLODED_CREEPERS.clear();
        PLAYER_LAST_HP.clear();
        PRE_ATTACK_STRENGTH.clear();
        PLAYER_HP_CHANGE_BUFF_UNTIL.clear();
        DRACONIC_GLORIES_STACKS.clear();
        FURINA_FANFARE_UNTIL.clear();
        FURINA_FANFARE_POINTS.clear();
        FURINA_FANFARE_NETHERITE.clear();
        JADE_SHIELD_UNTIL.clear();
        JADE_SHIELD_NETHERITE.clear();
        NIGHTS_FOCUS_UNTIL.clear();
        NIGHTS_FOCUS_NETHERITE.clear();
        NIGHTS_FOCUS_STARS.clear();
        NIGHTS_FOCUS_LAST_AUTO_STAR.clear();
        NIGHTS_FOCUS_LAST_SKILL_STAR.clear();
        NIGHTS_FOCUS_BASE_HP.clear();
        NIGHTS_FOCUS_PENDING_SHOTS.clear();
        LAYLA_DREAMSPHERE_UNTIL.clear();
        LAYLA_DREAMSPHERE_NETHERITE.clear();
        LAYLA_DREAMSPHERE_NEXT_SHOT.clear();
        MAVUIKA_AURA_UNTIL.clear();
        MAVUIKA_AURA_NETHERITE.clear();
        MAVUIKA_AURA_NEXT_STRIKE.clear();
        MAVUIKA_NIGHTSOUL_POINTS.clear();
        MAVUIKA_LAST_NA_FS_TICK.clear();
        MAVUIKA_SKILL_COOLDOWN_UNTIL.clear();
        RAIDEN_EYE_UNTIL.clear();
        RAIDEN_EYE_NETHERITE.clear();
        RAIDEN_EYE_NEXT_PROC.clear();
        RAIDEN_RESOLVE.clear();
        RAIDEN_MUSOU_UNTIL.clear();
        RAIDEN_MUSOU_NETHERITE.clear();
        RAIDEN_SKILL_CD_UNTIL.clear();
        RAIDEN_BURST_CD_UNTIL.clear();
        RAIDEN_IFRAME_UNTIL.clear();
        RAIDEN_NA_COMBO.clear();
        RAIDEN_LAST_MELEE_TICK.clear();
        RAIDEN_NEXT_SWING_TICK.clear();
        RAIDEN_PENDING_HITOTACHI_TICK.clear();
        RAIDEN_PENDING_HITOTACHI_MULT.clear();
        ArchonUltiManager.clearAll();
        XilonenManager.clearAll();
        PlungeAndGliderManager.clearAll();
        NeuvilletteManager.clearAll();
        PETRIFIED_UNTIL.clear();
        LAST_ELEMENTAL_ARMOR_WEAR.clear();
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
        ElementalDeathMessageHelper.clearAll();
        BURNING_ATTACKER_UUID.clear();
        SUPPRESSED_PHYSICAL_FLOAT_TICK.clear();
        TRADER_LAST_CUSTOMER.clear();
        ZhongliManager.clearAll();
        VentiManager.clearAll();
    }

    public static void tickActiveEntityGauges(MinecraftServer server, long serverNow) {
        Set<UUID> activeEntityIds = new HashSet<>(GAUGES.keySet());
        activeEntityIds.addAll(QUICKEN_GAUGES.keySet());
        activeEntityIds.addAll(FROZEN_GAUGES.keySet());
        activeEntityIds.addAll(FROZEN_UNTIL.keySet());
        activeEntityIds.addAll(PETRIFIED_UNTIL.keySet());

        for (UUID id : activeEntityIds) {
            LivingEntity target = null;
            ServerLevel targetLevel = null;

            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(id);
            if (onlinePlayer != null && onlinePlayer.isAlive() && !onlinePlayer.isRemoved()) {
                target = onlinePlayer;
                targetLevel = onlinePlayer.serverLevel();
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
                PlayerGenshinState.remove(id);
                GAUGES.remove(id);
                FROZEN_GAUGES.remove(id);
                QUICKEN_GAUGES.remove(id);
                FROZEN_UNTIL.remove(id);
                PETRIFIED_UNTIL.remove(id);
                FROZEN_POSES.remove(id);
                LAST_FREEZE_STRUGGLE.remove(id);
                QUICKEN_UNTIL.remove(id);
                BURNING_UNTIL.remove(id);
                BURNING_EM_LEVEL.remove(id);
                BURNING_ATTACKER_UUID.remove(id);
                ELECTRO_CHARGE_EM_LEVEL.remove(id);
                SUPPRESSED_PHYSICAL_FLOAT_TICK.remove(id);
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
        DRACONIC_GLORIES_STACKS.remove(id);
                continue;
            }

            long now = targetLevel.getGameTime();
            Map<Element, GaugeState> entityGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            EnumSet<Element> innateSet = getInnateElements(target);
            Element innate = getInnateElement(target);
            boolean changed = false;

            if (innateSet.contains(Element.PYRO)) {
                extinguishPyroInnate(target);
            }

            // 1. Environmental Water / Fire check & Cryo Water Freezing
            if (innateSet.contains(Element.CRYO) || entityGauges.containsKey(Element.CRYO)) {
                checkCryoWaterFreezing(target);
            }
            if (isEntityInWater(target)) {
                handleEnvironmentalWater(target);
                if (entityGauges.isEmpty() && !QUICKEN_GAUGES.containsKey(id) && !FROZEN_GAUGES.containsKey(id)) {
                    GAUGES.remove(id);
                    continue;
                }
            } else if (isTouchingEnvironmentalFireBlock(target)) {
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
                                    && !hasInnateElement(e, Element.ELECTRO))) {
                        drawVoltaicArc(targetLevel, electroSource, nearby);
                        applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 1.0F);
                    }

                    if (isEntitySubmergedInWater(electroSource)) {
                        ElectrifiedWaterManager.electrify(targetLevel, electroSource.blockPosition(), 1.0D, 30, null, 1.0F);
                    }
                }
            } else if (innateSet.contains(Element.ELECTRO) && isMutatedInnate(target)) {
                // Mutated Electro mob: does NOT radiate Electro into the air; only electrifies the 1 single water block it touches
                if (now % 20 == 0 && isEntitySubmergedInWater(target)) {
                    ElectrifiedWaterManager.electrifySingleBlock(targetLevel, target.blockPosition(), 30, null, 1.0F);
                }
            }

            // 2. Tick gauges decay
            Iterator<Map.Entry<Element, GaugeState>> gaugeIt = entityGauges.entrySet().iterator();
            while (gaugeIt.hasNext()) {
                Map.Entry<Element, GaugeState> gEntry = gaugeIt.next();
                if (innateSet.contains(gEntry.getKey())) {
                    continue; // Innate lock: never decay naturally
                }
                if (gEntry.getKey() == Element.PYRO && (isTouchingEnvironmentalFireBlock(target) || BURNING_UNTIL.containsKey(id))) {
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
            Long petrifiedEnd = PETRIFIED_UNTIL.get(id);
            if (petrifiedEnd != null && now >= petrifiedEnd) {
                PETRIFIED_UNTIL.remove(id);
                petrifiedEnd = null;
                changed = true;
            }
            if (isBossEntity(target)) {
                if (fg != null || frozenEnd != null || petrifiedEnd != null) {
                    clearFreezeState(target, id);
                    changed = true;
                }
            } else if (fg != null) {
                fg.tick();
                if (fg.isExpired() || (frozenEnd != null && now >= frozenEnd)) {
                    if (petrifiedEnd != null && now < petrifiedEnd) {
                        FROZEN_GAUGES.remove(id);
                        changed = true;
                        enforceFrozenPose(target, id);
                    } else {
                        clearFreezeState(target, id);
                        changed = true;
                    }
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
            } else if (petrifiedEnd != null) {
                PETRIFIED_UNTIL.remove(id);
                changed = true;
            }

            // 2B. Proximity Voltaic Arcing (ONLY when BOTH entities are physically submerged in water)
            if (entityGauges.containsKey(Element.ELECTRO) && isEntitySubmergedInWater(target) && (now % 10 == 0)) {
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
                            if (getInnateElement(nearby) != Element.ELECTRO && checkReactionIcd(nearby, ElementalReaction.ELECTRO_CHARGED, now)) {
                                float nearEcDmg = applyMobElementalResistance(nearby, Element.ELECTRO, 1.0F);
                                hurtWithPreAppliedElementalRes(nearby, ElementalDeathMessageHelper.createDamageSource(targetLevel, null, ElementalReaction.ELECTRO_CHARGED, Element.ELECTRO), nearEcDmg);
                                Map<Element, GaugeState> nearGauges = GAUGES.getOrDefault(nearby.getUUID(), Collections.emptyMap());
                                syncAuras(nearby, nearGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, nearEcDmg);
                            }
                            break;
                        }
                    }
                }
            }

            // 3. Electro-Charged periodic damage (every 20 ticks = 1 second, 0.5 heart = 1.0F damage, consuming 0.4U from both Hydro and Electro)
            boolean dualHydroElectroInnate = innateSet.contains(Element.HYDRO) && innateSet.contains(Element.ELECTRO);
            if (!dualHydroElectroInnate && entityGauges.containsKey(Element.HYDRO) && entityGauges.containsKey(Element.ELECTRO)) {
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
                    if (!innateSet.contains(Element.ELECTRO)) {
                        hurtWithPreAppliedElementalRes(target, ElementalDeathMessageHelper.createDamageSource(targetLevel, null, ElementalReaction.ELECTRO_CHARGED, Element.ELECTRO), ecDmg);
                        syncAuras(target, entityGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, ecDmg);
                    }

                    if (isEntitySubmergedInWater(target)) {
                        final LivingEntity srcTarget = target;
                        AABB chainBox = target.getBoundingBox().inflate(4.0D);
                        int chained = 0;
                        for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, chainBox, e -> e.isAlive() && e != srcTarget)) {
                            if (isEntitySubmergedInWater(nearby)) {
                                if (chained++ >= 2) break;
                                drawVoltaicArc(targetLevel, target, nearby);
                                if (getInnateElement(nearby) != Element.ELECTRO && checkReactionIcd(nearby, ElementalReaction.ELECTRO_CHARGED, now)) {
                                    float nearEcDmg = applyMobElementalResistance(nearby, Element.ELECTRO,
                                            1.0F * getTransformativeMultiplier(ecEmLvl));
                                    hurtWithPreAppliedElementalRes(nearby, ElementalDeathMessageHelper.createDamageSource(targetLevel, null, ElementalReaction.ELECTRO_CHARGED, Element.ELECTRO), nearEcDmg);
                                    Map<Element, GaugeState> nearGauges = GAUGES.getOrDefault(nearby.getUUID(), Collections.emptyMap());
                                    syncAuras(nearby, nearGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, nearEcDmg);
                                }
                            }
                        }
                    }

                    if (hydroState != null && !innateSet.contains(Element.HYDRO)) {
                        hydroState.consume(0.4F);
                        if (hydroState.isExpired()) {
                            entityGauges.remove(Element.HYDRO);
                            changed = true;
                        }
                    }
                    if (electroState != null && !innateSet.contains(Element.ELECTRO)) {
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
                boolean hasDendroFuel = entityGauges.containsKey(Element.DENDRO) || QUICKEN_GAUGES.containsKey(id) || innateSet.contains(Element.DENDRO);
                boolean hasPyro = entityGauges.containsKey(Element.PYRO) || innateSet.contains(Element.PYRO);
                if (!hasDendroFuel || !hasPyro || target.isInWaterOrRain() || target.fireImmune() || target instanceof Warden) {
                    BURNING_UNTIL.remove(id);
                    BURNING_EM_LEVEL.remove(id);
                    BURNING_ATTACKER_UUID.remove(id);
                    LAST_BURN_PYRO_REFRESH.remove(id);
                    if (innateSet.contains(Element.DENDRO)) {
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
                    UUID burnCreditId = BURNING_ATTACKER_UUID.get(id);
                    Entity burnCreditEnt = burnCreditId != null ? targetLevel.getEntity(burnCreditId) : null;
                    LivingEntity burnCredit = burnCreditEnt instanceof LivingEntity livingCredit ? livingCredit : null;
                    IS_PROCESSING_REACTION.set(true);
                    try {
                        hurtWithPreAppliedElementalRes(target, ElementalDeathMessageHelper.createTypedDamageSource(
                                targetLevel.damageSources().inFire(), burnCredit, ElementalReaction.BURNING, Element.PYRO), burnDmg);
                    } finally {
                        IS_PROCESSING_REACTION.set(false);
                    }
                    if (!target.isAlive() && burnDmg > 0.0F) {
                        syncAuras(target, Collections.emptyList(), Element.DENDRO, Element.PYRO, ElementalReaction.BURNING, burnDmg);
                    } else if (target.isAlive() && burnDmg > 0.0F) {
                        MobHazardAvoidanceManager.reportReactionOrHazard(target, null, target.position(), Element.DENDRO, Element.PYRO);
                    }
                    BURNING_UNTIL.put(id, now + 200L);
                    target.igniteForSeconds(2);

                    // Consume 0.1U per 5 ticks (= 0.4U/s) from Dendro and Quicken gauges
                    GaugeState dendroState = entityGauges.get(Element.DENDRO);
                    if (dendroState != null && !innateSet.contains(Element.DENDRO)) {
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
                    if ((innateSet.isEmpty() || innateSet.contains(Element.DENDRO) || innateSet.contains(Element.GEO)) && (lastPyroRefresh == null || now < lastPyroRefresh || (now - lastPyroRefresh) >= 40L)) {
                        LAST_BURN_PYRO_REFRESH.put(id, now);
                        if (entityGauges.containsKey(Element.PYRO)) {
                            entityGauges.get(Element.PYRO).refresh(1.0F);
                        } else {
                            entityGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.0F));
                        }
                    }

                    if (!entityGauges.containsKey(Element.DENDRO) && !QUICKEN_GAUGES.containsKey(id) && !innateSet.contains(Element.DENDRO)) {
                        BURNING_UNTIL.remove(id);
                        BURNING_EM_LEVEL.remove(id);
                        LAST_BURN_PYRO_REFRESH.remove(id);
                        changed = true;
                    }
                    if (now % 10 == 0 && canEmitReactionVisual(targetLevel, target.position(), 4)) {
                        targetLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 2, 0.25D, 0.3D, 0.25D, 0.02D);
                    }
                }
            }

            // 5. Cryo Aura passive debuff (-15% Movement Speed, -15% Attack Speed) when not Statue-Frozen
            boolean hasUnfrozenCryo = entityGauges.containsKey(Element.CRYO) && !isEntityFrozen(target);
            updateCryoDebuff(target, id, hasUnfrozenCryo, innateSet.contains(Element.CRYO) ? Element.CRYO : innate);

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

    }

    private static final class MavuikaIdleState {
        net.minecraft.world.phys.Vec3 lastStillPos = net.minecraft.world.phys.Vec3.ZERO;
        int stillTicks = 0;
        boolean idleActive = false;
        int lastSyncedCode = -1;
    }

    private static final java.util.Map<java.util.UUID, MavuikaIdleState> MAVUIKA_IDLE_STATES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void tickIdle(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            MavuikaIdleState st = MAVUIKA_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new MavuikaIdleState());
            ItemStack main = player.getMainHandItem();
            boolean holding = ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS
                    || (!ArcaneStaffItem.isGenshinCharacterWeapon(main)
                    && ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS);
            if (st.lastStillPos == null || st.lastStillPos.equals(net.minecraft.world.phys.Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distSq = player.position().distanceToSqr(st.lastStillPos);
            boolean canIdle = holding && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting()
                    && !(player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity);
            if (canIdle && distSq < 0.0025D) {
                st.stillTicks++;
                if (st.stillTicks >= 60 && !st.idleActive) {
                    st.idleActive = true;
                }
            } else {
                st.stillTicks = 0;
                st.lastStillPos = player.position();
                st.idleActive = false;
            }
            int code = st.idleActive ? 4031 : 4030;
            if (code != st.lastSyncedCode || (st.idleActive && now % 20L == 0L)) {
                st.lastSyncedCode = code;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, code);
            }
        }
    }

    public static void cancelIdle(ServerPlayer player) {
        if (player == null) return;
        MavuikaIdleState st = MAVUIKA_IDLE_STATES.get(player.getUUID());
        if (st != null) {
            st.stillTicks = 0;
            st.lastStillPos = player.position();
            if (st.idleActive || st.lastSyncedCode == 4031) {
                st.idleActive = false;
                st.lastSyncedCode = 4030;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 4030);
            }
        }
    }
}
