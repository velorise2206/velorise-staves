package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
    // 50% particle size electric arc particles (scale = 0.5F)
    private static final DustParticleOptions ELECTRIC_DUST_VIOLET = new DustParticleOptions(new Vector3f(0.75F, 0.4F, 1.0F), 0.5F);
    private static final DustParticleOptions ELECTRIC_DUST_CYAN = new DustParticleOptions(new Vector3f(0.35F, 0.85F, 1.0F), 0.5F);

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

        // Burst of crawling electric arcs radiating outwards across water surface
        Vec3 centerPos = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D);
        for (int i = 0; i < 5; i++) {
            double angle = (i * Math.PI * 0.4D) + (level.random.nextDouble() - 0.5D) * 0.4D;
            double r = radius * (0.65D + level.random.nextDouble() * 0.35D);
            Vec3 edge = centerPos.add(Math.cos(angle) * r, (level.random.nextDouble() - 0.5D) * 0.08D, Math.sin(angle) * r);
            drawElectricArc(level, centerPos, edge, 0.3D, true);
        }

        // Immediate shock to anyone already in this water
        shockEntities(level, pos, radius, attacker, damage);
        return true;
    }

    private static boolean isWaterAt(ServerLevel level, BlockPos p) {
        return level.getFluidState(p).is(FluidTags.WATER) || level.getBlockState(p).is(Blocks.WATER);
    }

    /**
     * Renders a continuous, jagged electric arc between two 3D points with branching forks.
     */
    public static void drawElectricArc(ServerLevel level, Vec3 start, Vec3 end, double jitter, boolean branching) {
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        if (dist < 0.1D) return;

        int numSegs = Math.max(3, (int) Math.round(dist * 2.5D));
        Vec3 prev = start;

        for (int i = 1; i <= numSegs; i++) {
            double t = (double) i / (double) numSegs;
            Vec3 target = (i == numSegs) ? end : start.add(diff.scale(t));
            if (i < numSegs) {
                double jx = (level.random.nextDouble() - 0.5D) * jitter;
                double jy = (level.random.nextDouble() - 0.5D) * (jitter * 0.4D);
                double jz = (level.random.nextDouble() - 0.5D) * jitter;
                target = target.add(jx, jy, jz);
            }

            // Dense interpolation between prev and target to form an unbroken glowing electric streak (50% smaller particles)
            Vec3 seg = target.subtract(prev);
            double segDist = seg.length();
            int subSteps = Math.max(2, (int) Math.ceil(segDist / 0.10D));
            for (int s = 0; s <= subSteps; s++) {
                Vec3 p = prev.add(seg.scale((double) s / (double) subSteps));
                DustParticleOptions dust = (s % 2 == 0) ? ELECTRIC_DUST_VIOLET : ELECTRIC_DUST_CYAN;
                level.sendParticles(dust, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }

            // Optional branching lightning fork splitting off outward
            if (branching && i == numSegs / 2 && level.random.nextBoolean()) {
                Vec3 forkDir = new Vec3(
                        (level.random.nextDouble() - 0.5D) * 0.9D,
                        (level.random.nextDouble() - 0.2D) * 0.35D,
                        (level.random.nextDouble() - 0.5D) * 0.9D
                );
                int forkSteps = Math.max(2, (int) Math.ceil(forkDir.length() / 0.10D));
                for (int fs = 0; fs <= forkSteps; fs++) {
                    Vec3 fp = target.add(forkDir.scale((double) fs / (double) forkSteps));
                    DustParticleOptions dust = (fs % 2 == 0) ? ELECTRIC_DUST_CYAN : ELECTRIC_DUST_VIOLET;
                    level.sendParticles(dust, fp.x, fp.y, fp.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }

            prev = target;
        }
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

            // Visual electric arcs crackling and crawling randomly across the water surface (every 2 ticks)
            if (now % 2 == 0) {
                int arcCount = 1 + level.random.nextInt(2);
                for (int a = 0; a < arcCount; a++) {
                    double r1 = level.random.nextDouble() * zone.radius;
                    double a1 = level.random.nextDouble() * Math.PI * 2.0D;
                    double x1 = zone.center.getX() + 0.5D + Math.cos(a1) * r1;
                    double z1 = zone.center.getZ() + 0.5D + Math.sin(a1) * r1;

                    // Branch to a nearby point on the surface (0.8m to 2.2m away)
                    double arcDist = 0.8D + level.random.nextDouble() * 1.4D;
                    double a2 = level.random.nextDouble() * Math.PI * 2.0D;
                    double x2 = x1 + Math.cos(a2) * arcDist;
                    double z2 = z1 + Math.sin(a2) * arcDist;

                    double y = zone.center.getY() + 0.85D + (level.random.nextDouble() - 0.5D) * 0.08D;
                    drawElectricArc(level, new Vec3(x1, y, z1), new Vec3(x2, y, z2), 0.25D, true);
                }
            }

            // Crawling electric arcs jumping between mobs or from water to mobs in this zone
            AABB zoneBox = new AABB(
                    zone.center.getX() - zone.radius, zone.center.getY() - 1.0D, zone.center.getZ() - zone.radius,
                    zone.center.getX() + zone.radius + 1.0D, zone.center.getY() + 2.5D, zone.center.getZ() + zone.radius + 1.0D
            );
            List<LivingEntity> inWaterMobs = level.getEntitiesOfClass(LivingEntity.class, zoneBox,
                    e -> e.isAlive() && ElementalGaugeManager.isEntityInWater(e));

            if (inWaterMobs.size() >= 2 && now % 4 == 0) {
                for (int i = 0; i < inWaterMobs.size() - 1; i++) {
                    LivingEntity m1 = inWaterMobs.get(i);
                    LivingEntity m2 = inWaterMobs.get(i + 1);
                    if (m1.position().distanceToSqr(m2.position()) <= 25.0D) {
                        drawElectricArc(level, m1.position().add(0, m1.getBbHeight() * 0.45D, 0),
                                              m2.position().add(0, m2.getBbHeight() * 0.45D, 0), 0.3D, true);
                    }
                }
            } else if (!inWaterMobs.isEmpty() && now % 3 == 0) {
                LivingEntity mob = inWaterMobs.get(level.random.nextInt(inWaterMobs.size()));
                Vec3 waterPt = new Vec3(mob.getX() + (level.random.nextDouble() - 0.5D) * 1.6D,
                                        zone.center.getY() + 0.85D,
                                        mob.getZ() + (level.random.nextDouble() - 0.5D) * 1.6D);
                drawElectricArc(level, waterPt, mob.position().add(0, mob.getBbHeight() * 0.4D, 0), 0.22D, false);
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
            // Innate Electro beings are immune to electrified water shocks
            if (ElementalGaugeManager.getInnateElement(victim) == Element.ELECTRO) continue;
            if (ElementalGaugeManager.isEntityInWater(victim)) {
                // Apply 0.8U Electro aura with damage, reacting with environmental Hydro wetness
                ElementalGaugeManager.applyElement(level, victim, Element.ELECTRO, 0.8F, attacker, damage);
                level.sendParticles(ELECTRIC_DUST_VIOLET, victim.getX(), victim.getY() + 0.6D, victim.getZ(), 6, 0.2D, 0.25D, 0.2D, 0.02D);
                level.sendParticles(ELECTRIC_DUST_CYAN, victim.getX(), victim.getY() + 0.6D, victim.getZ(), 4, 0.2D, 0.25D, 0.2D, 0.02D);
            }
        }
    }

    public static void clearAll() {
        ZONES.clear();
    }
}
