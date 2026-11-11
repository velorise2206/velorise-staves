package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active Dragonfire lingering AreaEffectClouds (Ender Staff AoE).
 * Instead of dense continuous random particles, renders expanding square elemental waves
 * synchronized with a 1.0s (20 ticks) pulse that applies Electro (1.0U), shocks entities,
 * electrifies water, and triggers Hyperbloom on nearby Dendro Cores.
 */
public final class DragonfireCloudManager {
    public static class CloudEntry {
        public final AreaEffectCloud cloud;
        public final ServerLevel level;
        public final @Nullable UUID ownerId;
        public final boolean isNetherite;
        public final float radius;
        public final long startTick;

        public CloudEntry(AreaEffectCloud cloud, ServerLevel level, @Nullable UUID ownerId, boolean isNetherite, float radius, long startTick) {
            this.cloud = cloud;
            this.level = level;
            this.ownerId = ownerId;
            this.isNetherite = isNetherite;
            this.radius = radius;
            this.startTick = startTick;
        }
    }

    private static final List<CloudEntry> CLOUDS = new CopyOnWriteArrayList<>();

    private DragonfireCloudManager() {}

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite) {
        float r = cloud.getRadius() > 0.1F ? cloud.getRadius() : (isNetherite ? 4.5F : 3.5F);
        register(cloud, level, owner, isNetherite, r);
    }

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite, float radius) {
        // Suppress vanilla dense continuous particle spam from AreaEffectCloud
        cloud.setRadius(0.0F);
        cloud.setRadiusPerTick(0.0F);
        cloud.setWaitTime(cloud.getDuration() + 20);
        CLOUDS.add(new CloudEntry(cloud, level, owner != null ? owner.getUUID() : null, isNetherite, radius, level.getGameTime()));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<CloudEntry> it = CLOUDS.iterator();

        while (it.hasNext()) {
            CloudEntry entry = it.next();
            if (entry.level != level) continue;
            if (!entry.cloud.isAlive() || entry.cloud.isRemoved()) {
                CLOUDS.remove(entry);
                continue;
            }

            AreaEffectCloud cloud = entry.cloud;
            float radius = entry.radius;
            long age = Math.max(0L, now - entry.startTick);
            int phase = (int) (age % 20L);

            // Expanding elemental square wave animation synchronized with the 20-tick (1.0s) pulse
            if (phase % 2 == 0) {
                double progress = (double) (phase + 2) / 20.0D;
                double halfExtent = radius * progress;
                ElementalGaugeManager.spawnExpandingSquareParticles(
                        level, cloud.position(), halfExtent, 0.15D,
                        ParticleTypes.DRAGON_BREATH, ParticleTypes.ELECTRIC_SPARK, 3
                );
            }

            // Every 20 ticks (1.0s) when the square wave completes its expansion, apply Electro (1.0U) pulse
            if (age > 0L && phase == 0) {
                BlockPos center = cloud.blockPosition();
                ServerPlayer owner = entry.ownerId != null ? level.getServer().getPlayerList().getPlayer(entry.ownerId) : null;
                float damage = entry.isNetherite ? 2.5F : 1.8F;

                // 1. Periodically electrify water beneath and around the cloud (every 1s, 1.0F shock damage)
                ElectrifiedWaterManager.electrify(level, center, radius + 0.5D, 25, owner, 1.0F);

                // 2. Periodically shock and apply Electro (1.0U) to all entities inside the square AoE
                Vec3 centerPos = cloud.position();
                AABB box = new AABB(
                        centerPos.x - radius, centerPos.y - 1.0D, centerPos.z - radius,
                        centerPos.x + radius, centerPos.y + 2.0D, centerPos.z + radius
                );
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && !e.isRemoved() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()))) {
                    ElementalGaugeManager.applyElementBypassIcd(level, target, Element.ELECTRO, 1.0F, owner, damage);
                    MobHazardAvoidanceManager.reportReactionOrHazard(target, owner, centerPos, Element.ELECTRO, null);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.6D, target.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.04D);
                }

                // 3. Detonate any Dendro Cores in the cloud (Hyperbloom)
                DendroCoreManager.checkReactionsNear(level, centerPos, radius, Element.ELECTRO, owner);
                level.playSound(null, centerPos.x, centerPos.y, centerPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.45F, 1.75F);
            }
        }
    }

    public static @Nullable Vec3 findNearestHazard(ServerLevel level, Vec3 pos, double margin) {
        if (CLOUDS.isEmpty()) return null;
        Vec3 nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (CloudEntry entry : CLOUDS) {
            if (entry.level != level || !entry.cloud.isAlive() || entry.cloud.isRemoved()) continue;
            Vec3 c = entry.cloud.position();
            double maxExt = entry.radius + margin;
            if (Math.abs(pos.y - c.y) <= 2.6D
                    && Math.abs(pos.x - c.x) <= maxExt
                    && Math.abs(pos.z - c.z) <= maxExt) {
                double d2 = pos.distanceToSqr(c);
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    nearest = c;
                }
            }
        }
        return nearest;
    }

    public static void clearAll() {
        CLOUDS.clear();
    }
}
