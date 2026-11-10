package com.velorise.staves.element;

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
 * Manages active Ghastfire lingering AreaEffectClouds (5s fire fields).
 * Instead of dense continuous random particles, renders expanding Pyro flame rings
 * synchronized with a 1.0s (20 ticks) pulse that applies Pyro (1.0U), burns entities inside,
 * triggers Burgeon on nearby Dendro Cores, and mines Amethyst Clusters via Overloaded.
 */
public final class GhastfireFieldManager {
    public static class FieldEntry {
        public final AreaEffectCloud cloud;
        public final ServerLevel level;
        public final @Nullable UUID ownerId;
        public final boolean isNetherite;
        public final float radius;
        public final long startTick;

        public FieldEntry(AreaEffectCloud cloud, ServerLevel level, @Nullable UUID ownerId, boolean isNetherite, float radius, long startTick) {
            this.cloud = cloud;
            this.level = level;
            this.ownerId = ownerId;
            this.isNetherite = isNetherite;
            this.radius = radius;
            this.startTick = startTick;
        }
    }

    private static final List<FieldEntry> FIELDS = new CopyOnWriteArrayList<>();

    private GhastfireFieldManager() {}

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite) {
        float r = cloud.getRadius() > 0.1F ? cloud.getRadius() : (isNetherite ? 4.5F : 3.5F);
        register(cloud, level, owner, isNetherite, r);
    }

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite, float radius) {
        // Suppress vanilla dense continuous particle spam from AreaEffectCloud
        cloud.setRadius(0.0F);
        cloud.setRadiusPerTick(0.0F);
        cloud.setWaitTime(cloud.getDuration() + 20);
        FIELDS.add(new FieldEntry(cloud, level, owner != null ? owner.getUUID() : null, isNetherite, radius, level.getGameTime()));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<FieldEntry> it = FIELDS.iterator();

        while (it.hasNext()) {
            FieldEntry entry = it.next();
            if (entry.level != level) continue;
            if (!entry.cloud.isAlive() || entry.cloud.isRemoved()) {
                FIELDS.remove(entry);
                continue;
            }

            AreaEffectCloud cloud = entry.cloud;
            float radius = entry.radius;
            long age = Math.max(0L, now - entry.startTick);
            int phase = (int) (age % 20L);

            // Expanding Pyro flame ring animation synchronized with the 20-tick (1.0s) aura application pulse
            if (phase % 2 == 0) {
                double progress = (double) (phase + 2) / 20.0D;
                double ringRadius = radius * progress;
                spawnExpandingPyroRing(level, cloud.position(), ringRadius);
            }

            // Every 20 ticks (1.0s) when the ring completes its expansion, apply Pyro (1.0U) pulse
            if (age > 0L && phase == 0) {
                ServerPlayer owner = entry.ownerId != null ? level.getServer().getPlayerList().getPlayer(entry.ownerId) : null;
                float damage = entry.isNetherite ? 2.5F : 1.5F;
                Vec3 centerPos = cloud.position();
                double r2 = (double) radius * (double) radius;

                // 1. Burn and apply Pyro (1.0U) to all entities inside the fire field radius
                AABB box = new AABB(
                        centerPos.x - radius, centerPos.y - 1.0D, centerPos.z - radius,
                        centerPos.x + radius, centerPos.y + 2.0D, centerPos.z + radius
                );
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && !e.isRemoved() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()))) {
                    double dx = target.getX() - centerPos.x;
                    double dz = target.getZ() - centerPos.z;
                    if (dx * dx + dz * dz <= r2) {
                        target.igniteForSeconds(3);
                        ElementalGaugeManager.applyElementBypassIcd(level, target, Element.PYRO, 1.0F, owner, damage);
                        level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.5D, target.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.03D);
                    }
                }

                // 2. Detonate any Dendro Cores in the fire field (Burgeon) & mine Amethyst Clusters (Overloaded)
                DendroCoreManager.checkReactionsNear(level, centerPos, radius, Element.PYRO, owner);
                EnvironmentalBlockManager.checkPyroAmethystMining(level, centerPos, radius);
                level.playSound(null, centerPos.x, centerPos.y, centerPos.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.05F);
            }
        }
    }

    private static void spawnExpandingPyroRing(ServerLevel level, Vec3 center, double ringRadius) {
        int points = Math.max(12, (int) Math.round(ringRadius * 8.0D));
        double y = center.y + 0.15D;
        for (int i = 0; i < points; i++) {
            double angle = (2.0D * Math.PI * i) / points;
            double px = center.x + Math.cos(angle) * ringRadius;
            double pz = center.z + Math.sin(angle) * ringRadius;
            level.sendParticles(ParticleTypes.FLAME, px, y, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, px, y + 0.05D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    public static void clearAll() {
        FIELDS.clear();
    }
}
