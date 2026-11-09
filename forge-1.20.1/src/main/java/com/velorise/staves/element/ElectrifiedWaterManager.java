package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Genshin Impact Electrified Water in Minecraft.
 * When an Electro attack hits a body of water, or a mob carrying Electro enters water,
 * the water becomes electrified for 3.5 seconds (70 ticks), continuously crackling with electric sparks,
 * dealing periodic Electro damage, applying Electro aura, and triggering Electro-Charged reactions.
 */
public final class ElectrifiedWaterManager {
    public static class WaterZone {
        public final ServerLevel level;
        public final BlockPos center;
        public final double radius;
        public final long expireTick;
        public final @Nullable UUID attackerId;
        public final float damage;

        public WaterZone(ServerLevel level, BlockPos center, double radius, long expireTick, @Nullable UUID attackerId, float damage) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.expireTick = expireTick;
            this.attackerId = attackerId;
            this.damage = damage;
        }
    }

    private static final List<WaterZone> ZONES = new CopyOnWriteArrayList<>();

    private ElectrifiedWaterManager() {}

    /**
     * Checks if the position or immediate surroundings contain water, and if so electrifies it.
     */
    public static boolean electrify(ServerLevel level, BlockPos pos, double radius, int durationTicks, @Nullable ServerPlayer attacker, float damage) {
        boolean hasWater = isWaterAt(level, pos) || isWaterAt(level, pos.below()) || isWaterAt(level, pos.above())
                || isWaterAt(level, pos.north()) || isWaterAt(level, pos.south()) || isWaterAt(level, pos.east()) || isWaterAt(level, pos.west());
        if (!hasWater) return false;

        // Merge or refresh existing zone nearby
        for (WaterZone zone : ZONES) {
            if (zone.level == level && zone.center.closerThan(pos, radius)) {
                ZONES.remove(zone);
                break;
            }
        }

        long now = level.getGameTime();
        UUID attackerUuid = attacker != null ? attacker.getUUID() : null;
        ZONES.add(new WaterZone(level, pos, radius, now + durationTicks, attackerUuid, damage));

        // High voltage electric impact sizzle sound
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.85F);
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);

        // Burst of electric sparks across water surface
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 18, radius * 0.5D, 0.2D, radius * 0.5D, 0.05D);

        // Immediate shock to anyone already in this water
        shockEntities(level, pos, radius, attacker, damage);
        return true;
    }

    private static boolean isWaterAt(ServerLevel level, BlockPos p) {
        return level.getFluidState(p).is(FluidTags.WATER) || level.getBlockState(p).is(Blocks.WATER);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<WaterZone> it = ZONES.iterator();

        while (it.hasNext()) {
            WaterZone zone = it.next();
            if (zone.level != level) continue;

            if (now >= zone.expireTick) {
                ZONES.remove(zone);
                continue;
            }

            // Visual electric sparks crackling on the water surface (every 4 ticks)
            if (now % 4 == 0) {
                double ox = (level.random.nextDouble() - 0.5D) * 2.0D * zone.radius;
                double oz = (level.random.nextDouble() - 0.5D) * 2.0D * zone.radius;
                double px = zone.center.getX() + 0.5D + ox;
                double pz = zone.center.getZ() + 0.5D + oz;
                double py = zone.center.getY() + 0.85D;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 2, 0.1D, 0.1D, 0.1D, 0.02D);
            }

            // Periodic electric shock damage and Electro aura application (every 10 ticks = 0.5s)
            if (now % 10 == 0) {
                ServerPlayer attacker = zone.attackerId != null ? level.getServer().getPlayerList().getPlayer(zone.attackerId) : null;
                shockEntities(level, zone.center, zone.radius, attacker, zone.damage);
            }
        }
    }

    private static void shockEntities(ServerLevel level, BlockPos center, double radius, @Nullable ServerPlayer attacker, float damage) {
        AABB box = new AABB(
                center.getX() - radius, center.getY() - 1.5D, center.getZ() - radius,
                center.getX() + radius + 1.0D, center.getY() + 2.5D, center.getZ() + radius + 1.0D
        );

        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (victim == attacker) continue;
            if (ElementalGaugeManager.isEntityInWater(victim)) {
                // Apply 0.8U Electro aura with damage, reacting with environmental Hydro wetness
                ElementalGaugeManager.applyElement(level, victim, Element.ELECTRO, 0.8F, attacker, damage);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 0.6D, victim.getZ(), 8, 0.25D, 0.3D, 0.25D, 0.04D);
            }
        }
    }

    public static void clearAll() {
        ZONES.clear();
    }
}
