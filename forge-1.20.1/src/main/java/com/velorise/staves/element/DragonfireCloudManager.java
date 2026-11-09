package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active Dragonfire lingering AreaEffectClouds.
 * Continuously electrifies water beneath and around the cloud, periodically applies Electro (1.0U),
 * shocks all entities within the cloud, and triggers Hyperbloom on nearby Dendro Cores until the cloud expires.
 */
public final class DragonfireCloudManager {
    public static class CloudEntry {
        public final AreaEffectCloud cloud;
        public final ServerLevel level;
        public final @Nullable UUID ownerId;
        public final boolean isNetherite;

        public CloudEntry(AreaEffectCloud cloud, ServerLevel level, @Nullable UUID ownerId, boolean isNetherite) {
            this.cloud = cloud;
            this.level = level;
            this.ownerId = ownerId;
            this.isNetherite = isNetherite;
        }
    }

    private static final List<CloudEntry> CLOUDS = new CopyOnWriteArrayList<>();

    private DragonfireCloudManager() {}

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite) {
        CLOUDS.add(new CloudEntry(cloud, level, owner != null ? owner.getUUID() : null, isNetherite));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 20 != 0) return; // Every 1.0s (20 ticks)

        Iterator<CloudEntry> it = CLOUDS.iterator();
        while (it.hasNext()) {
            CloudEntry entry = it.next();
            if (entry.level != level) continue;
            if (!entry.cloud.isAlive() || entry.cloud.isRemoved()) {
                CLOUDS.remove(entry);
                continue;
            }

            AreaEffectCloud cloud = entry.cloud;
            float radius = cloud.getRadius();
            BlockPos center = cloud.blockPosition();
            ServerPlayer owner = entry.ownerId != null ? level.getServer().getPlayerList().getPlayer(entry.ownerId) : null;
            float damage = entry.isNetherite ? 2.5F : 1.8F;

            // 1. Periodically electrify water beneath and around the cloud (every 1s)
            ElectrifiedWaterManager.electrify(level, center, radius + 0.5D, 25, owner, damage);

            // 2. Periodically shock and apply Electro (1.0U) to all entities inside the cloud (every 1s)
            AABB box = cloud.getBoundingBox();
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isRemoved() && e != owner)) {
                ElementalGaugeManager.applyElement(level, target, Element.ELECTRO, 1.0F, owner, damage);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.6D, target.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.05D);
            }

            // 3. Detonate any Dendro Cores in the cloud (Hyperbloom)
            DendroCoreManager.checkReactionsNear(level, cloud.position(), radius, Element.ELECTRO, owner);
        }
    }

    public static void clearAll() {
        CLOUDS.clear();
    }
}
