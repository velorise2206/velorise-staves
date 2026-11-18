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
 * Manages Genshin Impact Electrified Water in Minecraft (1.0.0 behavior & visuals).
 * When an Electro attack hits a body of water, or a mob carrying Electro enters water,
 * the water becomes electrified, crackling with crisp electric arcs across the water surface
 * and between submerged mobs while dealing periodic 0.5 heart (1.0F) Electro-Charged shocks once per second.
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
    private static final ThreadLocal<Boolean> IS_SHOCKING = ThreadLocal.withInitial(() -> Boolean.FALSE);

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
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        drawElectricArc(level, centerPos, centerPos.add(Math.cos(angle) * 0.4D, 0.0D, Math.sin(angle) * 0.4D), 0.15D, false);

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

        long now = level.getGameTime();
        UUID attackerUuid = attacker != null ? attacker.getUUID() : null;
        float shockDmg = 1.0F; // Standardized 0.5 heart (1.0 damage) per electric shock
        ZONES.add(new WaterZone(level, pos, radius, now + durationTicks, attackerUuid, shockDmg));

        // High voltage electric impact sizzle sound
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.85F);
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);

        // Initial radial lightning burst across the water surface (1.0.0 style)
        Vec3 centerPos = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D);
        for (int i = 0; i < 5; i++) {
            double angle = i * (Math.PI * 0.4D) + (level.random.nextDouble() - 0.5D) * 0.4D;
            double dist = radius * (0.65D + level.random.nextDouble() * 0.35D);
            Vec3 endPos = centerPos.add(Math.cos(angle) * dist, (level.random.nextDouble() - 0.5D) * 0.08D, Math.sin(angle) * dist);
            drawElectricArc(level, centerPos, endPos, 0.3D, true);
        }

        // Immediate shock to anyone already in this water
        shockEntities(level, pos, radius, attacker, shockDmg);
        return true;
    }

    private static boolean isWaterAt(ServerLevel level, BlockPos p) {
        return level.getFluidState(p).is(FluidTags.WATER) || level.getBlockState(p).is(Blocks.WATER);
    }

    /**
     * Renders a thin, crisp 3D jagged electric lightning bolt between two points (1.0.0 Genshin-style).
     */
    public static void drawElectricArc(ServerLevel level, Vec3 start, Vec3 end, double jitter, boolean branching) {
        Vec3 diff = end.subtract(start);
        if (diff.lengthSqr() < 0.01D) return;
        ModNetwork.sendElectricArc(level, start, end, 8, 0xBF40FF);
    }

    public static void drawElectricArc(ServerLevel level, Entity from, Entity to, int durationTicks) {
        if (from == null || to == null) return;
        ModNetwork.sendElectricArc(level, from, to, durationTicks, 0xBF40FF);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<WaterZone> it = ZONES.iterator();
        Set<UUID> shockedThisTick = (now % 20 == 0) ? new HashSet<>() : null;

        while (it.hasNext()) {
            WaterZone zone = it.next();
            if (zone.level != level) continue;

            if (now >= zone.expireTick || (zone.expireTick - now) > 1200L) {
                ZONES.remove(zone);
                continue;
            }

            boolean singleBlock = zone.radius <= 0.5D;

            // 1.0.0 Visual electric arcs crackling across the water surface
            if (now % (singleBlock ? 4 : 2) == 0) {
                int arcCount = singleBlock ? 1 : (1 + level.random.nextInt(2));
                for (int i = 0; i < arcCount; i++) {
                    double r1 = level.random.nextDouble() * zone.radius;
                    double a1 = level.random.nextDouble() * Math.PI * 2.0D;
                    double sx = zone.center.getX() + 0.5D + Math.cos(a1) * r1;
                    double sz = zone.center.getZ() + 0.5D + Math.sin(a1) * r1;

                    double len = singleBlock ? (0.3D + level.random.nextDouble() * 0.35D) : (0.8D + level.random.nextDouble() * 1.4D);
                    double a2 = level.random.nextDouble() * Math.PI * 2.0D;
                    double ex = sx + Math.cos(a2) * len;
                    double ez = sz + Math.sin(a2) * len;

                    if (singleBlock) {
                        ex = Math.max(zone.center.getX() + 0.05D, Math.min(zone.center.getX() + 0.95D, ex));
                        ez = Math.max(zone.center.getZ() + 0.05D, Math.min(zone.center.getZ() + 0.95D, ez));
                    }

                    double y = zone.center.getY() + 0.85D + (level.random.nextDouble() - 0.5D) * 0.08D;
                    drawElectricArc(level, new Vec3(sx, y, sz), new Vec3(ex, y, ez), 0.25D, !singleBlock);
                }
            }

            if (now % 4 == 0 || now % 3 == 0 || now % 20 == 0) {
                AABB zoneBox = singleBlock
                        ? new AABB(zone.center)
                        : new AABB(
                                zone.center.getX() - zone.radius, zone.center.getY() - 1.5D, zone.center.getZ() - zone.radius,
                                zone.center.getX() + zone.radius + 1.0D, zone.center.getY() + 2.5D, zone.center.getZ() + zone.radius + 1.0D
                        );
                List<LivingEntity> inWaterMobs = level.getEntitiesOfClass(LivingEntity.class, zoneBox,
                        e -> e.isAlive() && ElementalGaugeManager.isEntityInWater(e));

                if (inWaterMobs.size() >= 2 && now % 4 == 0) {
                    for (int i = 0; i < inWaterMobs.size() - 1; i++) {
                        LivingEntity m1 = inWaterMobs.get(i);
                        LivingEntity m2 = inWaterMobs.get(i + 1);
                        if (m1.position().distanceToSqr(m2.position()) <= 25.0D) {
                            drawElectricArc(level, m1, m2, 8);
                        }
                    }
                } else if (!inWaterMobs.isEmpty() && now % 3 == 0) {
                    LivingEntity mob = inWaterMobs.get(level.random.nextInt(inWaterMobs.size()));
                    double spread = singleBlock ? 0.45D : 1.6D;
                    Vec3 waterPt = new Vec3(mob.getX() + (level.random.nextDouble() - 0.5D) * spread,
                                            zone.center.getY() + 0.85D,
                                            mob.getZ() + (level.random.nextDouble() - 0.5D) * spread);
                    drawElectricArc(level, waterPt, mob.position().add(0, mob.getBbHeight() * 0.4D, 0), 0.22D, false);
                }

                // Periodic electric shock damage and Electro aura application (every 20 ticks = 1.0s)
                if (now % 20 == 0 && shockedThisTick != null) {
                    ServerPlayer attacker = zone.attackerId != null ? level.getServer().getPlayerList().getPlayer(zone.attackerId) : null;
                    shockEntities(level, zone.center, attacker, inWaterMobs, shockedThisTick);
                }
            }
        }
    }

    private static void shockEntities(ServerLevel level, BlockPos center, double radius, @Nullable ServerPlayer attacker, float damage) {
        AABB box = new AABB(center).inflate(radius + 0.5D, 1.0D, radius + 0.5D);
        List<LivingEntity> inWaterMobs = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && (e.isInWaterOrRain() || isWaterAt(level, e.blockPosition())));
        shockEntities(level, center, attacker, inWaterMobs, null);
    }

    private static void shockEntities(ServerLevel level, BlockPos center, @Nullable ServerPlayer attacker, List<LivingEntity> inWaterMobs, Set<UUID> shockedThisTick) {
        if (Boolean.TRUE.equals(IS_SHOCKING.get())) return;
        IS_SHOCKING.set(true);
        try {
            Vec3 centerVec = Vec3.atCenterOf(center);
            for (LivingEntity victim : inWaterMobs) {
                if (victim.isSpectator() || (victim instanceof Player p && p.isCreative())) continue;
                if (attacker != null && ElementalGaugeManager.isRaidenProtectedAlly(attacker, victim)) continue;
                // Innate Electro beings are immune to electrified water shocks
                if (ElementalGaugeManager.hasInnateElement(victim, Element.ELECTRO)) continue;
                if (shockedThisTick.add(victim.getUUID())) {
                    ElementalGaugeManager.applyElement(level, victim, Element.ELECTRO, 0.8F, attacker, 1.0F);
                    MobHazardAvoidanceManager.reportReactionOrHazard(victim, attacker, centerVec, Element.ELECTRO, Element.HYDRO);
                }
            }
        } finally {
            IS_SHOCKING.set(false);
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
