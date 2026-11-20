package com.velorise.staves.element;

import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Bee;
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
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
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
    private static final Map<UUID, com.velorise.staves.fsm.MavuikaFormState> MAVUIKA_FORM_STATES = new ConcurrentHashMap<>();
    public static com.velorise.staves.fsm.MavuikaFormState getFormState(UUID id) {
        return MAVUIKA_FORM_STATES.computeIfAbsent(id, k -> new com.velorise.staves.fsm.MavuikaFormState());
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
            boolean neth = MAVUIKA_AURA_NETHERITE.getOrDefault(id, false);
            float fsEfficiency = neth ? 1.25F : 1.0F;
            float curPts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
            MAVUIKA_NIGHTSOUL_POINTS.put(id, Math.min(10.0F, curPts + drained * 0.05F * fsEfficiency));
        }
    }

    public static void addMavuikaNormalAttackFightingSpirit(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        UUID id = player.getUUID();
        long now = player.serverLevel().getServer().overworld().getGameTime();
        ItemStack main = player.getMainHandItem();
        boolean holdingMain = com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(main) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS;
        if (!holdingMain && !isMavuikaAuraActive(id, now)) return;
        long lastNaFs = MAVUIKA_LAST_NA_FS_TICK.getOrDefault(id, -100L);
        if (now >= lastNaFs && now - lastNaFs < 2L) return; // Genshin A1 passive: 0.1s ICD
        MAVUIKA_LAST_NA_FS_TICK.put(id, now);
        boolean neth = MAVUIKA_AURA_NETHERITE.getOrDefault(id,
                holdingMain && com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(main));
        float fsEfficiency = neth ? 1.25F : 1.0F;
        float curPts = MAVUIKA_NIGHTSOUL_POINTS.getOrDefault(id, 0.0F);
        float updated = Math.min(10.0F, curPts + 0.075F * fsEfficiency);
        if (updated != curPts) {
            MAVUIKA_NIGHTSOUL_POINTS.put(id, updated);
            int rem = Math.round(MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F) * 10.0F);
            com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, rem, updated, neth);
        }
    }

    public static boolean isMavuikaAuraActive(UUID id, long now) {
        return MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F) > 0.01F
                || now < MAVUIKA_CRUCIBLE_UNTIL.getOrDefault(id, 0L);
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
        if (dealCastSweep) {
            // Genshin Hold E mount: whip the rear wheel in a 180-deg half-donut arc first, then trigger Pyro AoE damage at the whip apex
            bike.triggerMountWhip(firstCast);
        }
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5D, player.getZ(), 22, 0.55D, 0.3D, 0.55D, 0.07D);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 0.95F);
    }

    public static void toggleMavuikaBike(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
            if (bike.getUltiTicks() > 0) return;
            player.stopRiding();
            bike.discard();
            return;
        }
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack main = player.getMainHandItem();
        if (com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(main) != com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            return;
        }
        long now = player.serverLevel().getServer().overworld().getGameTime();
        if (!isMavuikaAuraActive(player.getUUID(), now) && isMavuikaOnCooldown(player.getUUID(), now)) return;
        summonAndMountFlamestrider(player, com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(main), 300);
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
        return MAVUIKA_CRUCIBLE_POINTS.getOrDefault(id, 0.0F) * 0.051F;
    }

    public static float getMavuikaCrucibleChargedBonusMultiplier(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return 0.0F;
        long now = player.serverLevel().getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        if (now >= MAVUIKA_CRUCIBLE_UNTIL.getOrDefault(id, 0L)) return 0.0F;
        return MAVUIKA_CRUCIBLE_POINTS.getOrDefault(id, 0.0F) * 0.102F;
    }

    public static void triggerMavuikaUlti(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !player.isAlive()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return;
        ItemStack main = player.getMainHandItem();
        if (com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(main) != com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            return;
        }
        boolean isNetherite = com.velorise.staves.item.ArcaneStaffItem.getEffectiveNetherite(main);

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

        float maxFuel = isNetherite ? 120.0F : 80.0F;
        MAVUIKA_NIGHTSOUL_FUEL.put(id, Math.min(maxFuel, Math.max(MAVUIKA_NIGHTSOUL_FUEL.getOrDefault(id, 0.0F) + 20.0F, 80.0F)));
        player.setShiftKeyDown(false);
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        if (!(player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
            summonAndMountFlamestrider(player, isNetherite, 0, false);
        }
        ArchonUltiManager.grantUltiIframe(player, 36); // Full unstoppable Genshin Q I-frames
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_MAVUIKA_BURST);
        MAVUIKA_CRUCIBLE_UNTIL.put(id, now + 162L); // Genshin Q: 22t Sunfell Slice windup + 140t (7.0s) Crucible of Death and Life
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
                MAVUIKA_FORM_STATES.remove(id);
                MAVUIKA_NIGHTSOUL_POINTS.remove(id);
                com.velorise.staves.network.ModNetwork.sendMavuikaSync(player, 0, 0.0F, neth);
                continue;
            }

            ServerLevel level = player.serverLevel();
            boolean onBike = player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity;
            boolean crucibleActive = serverNow < MAVUIKA_CRUCIBLE_UNTIL.getOrDefault(id, 0L);
            com.velorise.staves.fsm.MavuikaFormState formState = getFormState(id);
            formState.setForm(onBike
                    ? com.velorise.staves.fsm.MavuikaFormState.Form.FLAMESTRIDER
                    : com.velorise.staves.fsm.MavuikaFormState.Form.RING_OF_SEARING_RADIANCE);
            boolean bikeDashing = false;
            boolean bikeWallClimbing = false;
            boolean bikeWaterSurfing = false;
            boolean bikeGliding = false;
            boolean bikeSprinting = false;
            boolean bikeSpinning = false;
            if (onBike && player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
                bikeDashing = bike.isDashingServer();
                bikeWallClimbing = bike.isWallClimbing();
                bikeWaterSurfing = bike.isInWater();
                bikeGliding = bike.isGliding();
                bikeSprinting = bike.isBikeSprinting();
                bikeSpinning = bike.isSpinning();
                if (bike.onGround() && !bikeWaterSurfing && !bikeWallClimbing) {
                    formState.rechargeOnGround();
                } else {
                    if (bikeWallClimbing) formState.consumeWallClimbTick();
                    if (bikeWaterSurfing) formState.consumeWaterSurfTick();
                    if (bikeGliding) formState.consumeGlideTick();
                }
            }
            float drainPerTick = com.velorise.staves.fsm.MavuikaFormState.computeDrainPerTick(
                    formState.getForm(),
                    crucibleActive,
                    bikeDashing,
                    bikeWallClimbing,
                    bikeWaterSurfing,
                    bikeGliding,
                    bikeSprinting,
                    bikeSpinning
            );
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
                MAVUIKA_FORM_STATES.remove(id);
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

    public static void clearPlayerState(UUID playerId) {
        if (playerId == null) return;
        MAVUIKA_AURA_UNTIL.remove(playerId);
        MAVUIKA_AURA_NETHERITE.remove(playerId);
        MAVUIKA_AURA_NEXT_STRIKE.remove(playerId);
        MAVUIKA_NIGHTSOUL_POINTS.remove(playerId);
        MAVUIKA_NIGHTSOUL_FUEL.remove(playerId);
        MAVUIKA_CRUCIBLE_UNTIL.remove(playerId);
        MAVUIKA_CRUCIBLE_POINTS.remove(playerId);
        MAVUIKA_SKILL_COOLDOWN_UNTIL.remove(playerId);
        MAVUIKA_ULTI_COOLDOWN_UNTIL.remove(playerId);
        MAVUIKA_IDLE_STATES.remove(playerId);
    }

    public static void clearAll() {
        MAVUIKA_FORM_STATES.clear();
        MAVUIKA_IDLE_STATES.clear();
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
            boolean holding = ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS;
            if (st.lastStillPos == null || st.lastStillPos.equals(net.minecraft.world.phys.Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distSq = player.position().distanceToSqr(st.lastStillPos);
            boolean canIdle = holding && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting()
                    && !(player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity);
            if (canIdle && distSq < 0.0025D) {
                st.stillTicks++;
                if (st.stillTicks >= 200 && !st.idleActive) {
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
