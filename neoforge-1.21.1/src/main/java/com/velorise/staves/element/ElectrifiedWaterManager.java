package com.velorise.staves.element;

import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Genshin Impact Electrified Water in Minecraft.
 * When an Electro attack hits a body of water, or a mob carrying Electro enters water,
 * the water becomes electrified for 3.5 seconds (70 ticks), continuously crackling with electric sparks,
 * dealing periodic 0.5 heart (1.0F) Electro damage, applying Electro aura, and triggering Electro-Charged reactions.
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
    public static final ThreadLocal<Boolean> IS_SHOCKING = ThreadLocal.withInitial(() -> false);

    private ElectrifiedWaterManager() {}

    /**
     * Electrifies only the single water block that the entity is directly touching (used for mutated Electro mobs).
     */
    public static boolean electrifySingleBlock(ServerLevel level, BlockPos pos, int durationTicks, @Nullable ServerPlayer attacker, float damage) {
        BlockPos waterPos = null;
        if (isWaterAt(level, pos)) {
            waterPos = pos.immutable();
        } else if (isWaterAt(level, pos.below())) {
            waterPos = pos.below().immutable();
        } else if (isWaterAt(level, pos.above())) {
            waterPos = pos.above().immutable();
        }
        if (waterPos == null) return false;

        for (WaterZone zone : ZONES) {
            if (zone.level == level && zone.center.equals(waterPos)) {
                ZONES.remove(zone);
                break;
            }
        }

        long now = level.getGameTime();
        UUID attackerUuid = attacker != null ? attacker.getUUID() : null;
        float shockDmg = 1.0F;
        // radius = 0.45D strictly confines the zone to this single 1x1x1 water block
        ZONES.add(new WaterZone(level, waterPos, 0.45D, now + durationTicks, attackerUuid, shockDmg));

        Vec3 centerPos = new Vec3(waterPos.getX() + 0.5D, waterPos.getY() + 0.85D, waterPos.getZ() + 0.5D);
        emitWaterSurfaceBranchCluster(level, centerPos, 0.45D, 2);

        shockEntities(level, waterPos, 0.45D, attacker, shockDmg);
        return true;
    }

    /**
     * Checks if the position or immediate surroundings contain water, and if so electrifies it.
     */
    public static boolean electrify(ServerLevel level, BlockPos pos, double radius, int durationTicks, @Nullable ServerPlayer attacker, float damage) {
        if (radius <= 0.5D) {
            return electrifySingleBlock(level, pos, durationTicks, attacker, damage);
        }
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

        while (ZONES.size() >= 8) {
            ZONES.remove(0);
        }

        long now = level.getGameTime();
        UUID attackerUuid = attacker != null ? attacker.getUUID() : null;
        float shockDmg = 1.0F; // Standardized 0.5 heart (1.0 damage) per electric shock
        int cappedTicks = Math.min(durationTicks, 35);
        ZONES.add(new WaterZone(level, pos, radius, now + cappedTicks, attackerUuid, shockDmg));

        Vec3 centerPos = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D);
        if (ElementalGaugeManager.canEmitReactionVisual(level, centerPos, 2)) {
            // High voltage electric impact sizzle sound
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.55F, 1.85F);
            level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.45F, 1.6F);

            // Short branching Arc Caster lightning segments spreading along both X & Z across the water surface
            emitWaterSurfaceBranchCluster(level, centerPos, radius, 3);
        }

        // Immediate shock to anyone already in this water
        shockEntities(level, pos, radius, attacker, shockDmg);
        return true;
    }

    private static boolean isWaterAt(ServerLevel level, BlockPos p) {
        return level.getFluidState(p).is(FluidTags.WATER) || level.getBlockState(p).is(Blocks.WATER);
    }

    /**
     * Renders a thin, crisp 3D jagged electric lightning bolt between two points (Genshin-style).
     */
    private static void emitWaterSurfaceBranchCluster(ServerLevel level, Vec3 origin, double maxRadius, int mainCount) {
        double surfaceY = origin.y + 0.04D;
        int count = Math.max(1, Math.min(3, mainCount));
        for (int i = 0; i < count; i++) {
            double rOff = (maxRadius > 0.6D) ? (level.random.nextDouble() * maxRadius * 0.40D) : 0.0D;
            double angOff = level.random.nextDouble() * Math.PI * 2.0D;
            double sx = origin.x + Math.cos(angOff) * rOff;
            double sz = origin.z + Math.sin(angOff) * rOff;
            double dirAngle = (i * (Math.PI * 2.0D / Math.max(2, count))) + (level.random.nextDouble() - 0.5D) * 0.85D;
            double mainLen = maxRadius <= 0.5D ? (0.35D + level.random.nextDouble() * 0.25D) : (1.15D + level.random.nextDouble() * 0.85D);
            Vec3 mainStart = new Vec3(sx, surfaceY + (level.random.nextDouble() - 0.5D) * 0.04D, sz);
            Vec3 mainEnd = new Vec3(
                    sx + Math.cos(dirAngle) * mainLen,
                    surfaceY + (level.random.nextDouble() - 0.5D) * 0.04D,
                    sz + Math.sin(dirAngle) * mainLen
            );
            drawElectricArc(level, mainStart, mainEnd, 0.22D, true);

            if (maxRadius > 0.5D) {
                Vec3 medBase = mainStart.lerp(mainEnd, 0.25D + level.random.nextDouble() * 0.55D);
                double medAngle = dirAngle + (level.random.nextBoolean() ? 1.0D : -1.0D) * (0.60D + level.random.nextDouble() * 0.70D);
                double medLen = 0.55D + level.random.nextDouble() * 0.45D;
                Vec3 medEnd = new Vec3(
                        medBase.x + Math.cos(medAngle) * medLen,
                        surfaceY + (level.random.nextDouble() - 0.5D) * 0.04D,
                        medBase.z + Math.sin(medAngle) * medLen
                );
                drawElectricArc(level, medBase, medEnd, 0.16D, false);


            }
        }
    }

    public static void drawElectricArc(ServerLevel level, Vec3 start, Vec3 end, double jitter, boolean branching) {
        Vec3 diff = end.subtract(start);
        if (diff.lengthSqr() < 0.01D) return;
        ModNetwork.sendElectricArc(level, start, end, 8, 0xBF40FE);
    }

    public static void drawElectricArc(ServerLevel level, Entity from, Entity to, int durationTicks) {
        if (from == null || to == null) return;
        ModNetwork.sendElectricArc(level, from, to, durationTicks, 0xBF40FF);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<WaterZone> it = ZONES.iterator();

        while (it.hasNext()) {
            WaterZone zone = it.next();
            if (zone.level != level) continue;

            if (now >= zone.expireTick || (zone.expireTick - now) > 1200L) {
                ZONES.remove(zone);
                continue;
            }

            boolean singleBlock = zone.radius <= 0.5D;

            // Visual electric arcs crackling across the water surface (short branching segments in X & Z)
            if (now % (singleBlock ? 8 : 5) == 0) {
                Vec3 zonePos = new Vec3(zone.center.getX() + 0.5D, zone.center.getY() + 0.85D, zone.center.getZ() + 0.5D);
                emitWaterSurfaceBranchCluster(level, zonePos, zone.radius, singleBlock ? 1 : 2);
            }

            AABB zoneBox = singleBlock
                    ? new AABB(zone.center)
                    : new AABB(
                            zone.center.getX() - zone.radius, zone.center.getY() - 1.0D, zone.center.getZ() - zone.radius,
                            zone.center.getX() + zone.radius + 1.0D, zone.center.getY() + 2.5D, zone.center.getZ() + zone.radius + 1.0D
                    );
            List<LivingEntity> inWaterMobs = level.getEntitiesOfClass(LivingEntity.class, zoneBox,
                    e -> e.isAlive() && ElementalGaugeManager.isEntityInWater(e));

            if (inWaterMobs.size() >= 2 && now % 6 == 0) {
                int maxLinks = Math.min(inWaterMobs.size() - 1, 4);
                for (int i = 0; i < maxLinks; i++) {
                    LivingEntity m1 = inWaterMobs.get(i);
                    LivingEntity m2 = inWaterMobs.get(i + 1);
                    if (m1.position().distanceToSqr(m2.position()) <= 25.0D) {
                        drawElectricArc(level, m1, m2, 8);
                    }
                }
            }
            if (!inWaterMobs.isEmpty() && now % 6 == 0) {
                int zapCount = Math.min(inWaterMobs.size(), 3);
                for (int m = 0; m < zapCount; m++) {
                    LivingEntity mob = inWaterMobs.get(m);
                    double spread = singleBlock ? 0.45D : 1.6D;
                    Vec3 waterPt = new Vec3(mob.getX() + (level.random.nextDouble() - 0.5D) * spread,
                                            zone.center.getY() + 0.85D,
                                            mob.getZ() + (level.random.nextDouble() - 0.5D) * spread);
                    drawElectricArc(level, waterPt, mob.position().add(0, mob.getBbHeight() * 0.4D, 0), 0.22D, false);
                }
            }

            // Periodic electric shock damage and Electro aura application (every 20 ticks = 1.0s)
            if (now % 20 == 0) {
                ServerPlayer attacker = zone.attackerId != null ? level.getServer().getPlayerList().getPlayer(zone.attackerId) : null;
                shockEntities(level, zone.center, zone.radius, attacker, zone.damage);
            }
        }
    }

    private static void shockEntities(ServerLevel level, BlockPos center, double radius, @Nullable ServerPlayer attacker, float damage) {
        boolean singleBlock = radius <= 0.5D;
        AABB box = singleBlock
                ? new AABB(center)
                : new AABB(
                        center.getX() - radius, center.getY() - 1.5D, center.getZ() - radius,
                        center.getX() + radius + 1.0D, center.getY() + 2.5D, center.getZ() + radius + 1.0D
                );

        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (victim.isSpectator() || (victim instanceof Player p && p.isCreative())) continue;
            // Innate Electro beings are immune to electrified water shocks
            if (ElementalGaugeManager.getInnateElement(victim) == Element.ELECTRO) continue;
            if (ElementalGaugeManager.isEntityInWater(victim)) {
                IS_SHOCKING.set(true);
                try {
                    ElementalGaugeManager.applyElement(level, victim, Element.ELECTRO, 0.4F, attacker, 1.0F);
                } finally {
                    IS_SHOCKING.set(false);
                }
                MobHazardAvoidanceManager.reportReactionOrHazard(victim, attacker, Vec3.atCenterOf(center), Element.ELECTRO, Element.HYDRO);
            }
        }
    }

    public static boolean isBlockElectrified(ServerLevel level, BlockPos blockPos) {
        if (ZONES.isEmpty()) return false;
        for (WaterZone zone : ZONES) {
            if (zone.level != level) continue;
            if (Math.abs(blockPos.getY() - zone.center.getY()) > 2) continue;
            double dx = blockPos.getX() - zone.center.getX();
            double dz = blockPos.getZ() - zone.center.getZ();
            double r = zone.radius + 0.5D;
            if (dx * dx + dz * dz <= r * r) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable Vec3 findNearestHazard(ServerLevel level, Vec3 pos, double margin) {
        if (ZONES.isEmpty()) return null;
        Vec3 nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (WaterZone zone : ZONES) {
            if (zone.level != level) continue;
            if (Math.abs(pos.y - zone.center.getY()) > 2.8D) continue;
            double maxDist = zone.radius + margin;
            double dx = pos.x - (zone.center.getX() + 0.5D);
            double dz = pos.z - (zone.center.getZ() + 0.5D);
            double d2 = dx * dx + dz * dz;
            if (d2 <= maxDist * maxDist && d2 < bestDistSq) {
                bestDistSq = d2;
                nearest = new Vec3(zone.center.getX() + 0.5D, zone.center.getY() + 0.5D, zone.center.getZ() + 0.5D);
            }
        }
        return nearest;
    }

    public static void clearAll() {
        ZONES.clear();
    }
}
