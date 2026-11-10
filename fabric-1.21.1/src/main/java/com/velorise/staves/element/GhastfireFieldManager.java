package com.velorise.staves.element;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active Ghastfire lingering AreaEffectClouds (5s fire fields).
 * Continuously burns and applies Pyro (1.0U) to all entities inside,
 * and triggers Burgeon on nearby Dendro Cores.
 */
public final class GhastfireFieldManager {
    public static class FieldEntry {
        public final AreaEffectCloud cloud;
        public final ServerLevel level;
        public final @Nullable UUID ownerId;
        public final boolean isNetherite;

        public FieldEntry(AreaEffectCloud cloud, ServerLevel level, @Nullable UUID ownerId, boolean isNetherite) {
            this.cloud = cloud;
            this.level = level;
            this.ownerId = ownerId;
            this.isNetherite = isNetherite;
        }
    }

    private static final List<FieldEntry> FIELDS = new CopyOnWriteArrayList<>();

    private GhastfireFieldManager() {}

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite) {
        FIELDS.add(new FieldEntry(cloud, level, owner != null ? owner.getUUID() : null, isNetherite));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 10 != 0) return; // Every 0.5s (10 ticks)

        Iterator<FieldEntry> it = FIELDS.iterator();
        while (it.hasNext()) {
            FieldEntry entry = it.next();
            if (entry.level != level) continue;
            if (!entry.cloud.isAlive() || entry.cloud.isRemoved()) {
                FIELDS.remove(entry);
                continue;
            }

            AreaEffectCloud cloud = entry.cloud;
            float radius = cloud.getRadius();
            ServerPlayer owner = entry.ownerId != null ? level.getServer().getPlayerList().getPlayer(entry.ownerId) : null;
            float damage = entry.isNetherite ? 2.5F : 1.5F;

            // 1. Burn and apply Pyro (1.0U) to all entities inside the fire field
            AABB box = cloud.getBoundingBox().inflate(0.5D);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isRemoved() && e != owner)) {
                target.igniteForSeconds(3);
                ElementalGaugeManager.applyElement(level, target, Element.PYRO, 1.0F, owner, damage);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.5D, target.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.03D);
            }

            // 2. Detonate any Dendro Cores in the fire field (Burgeon)
            DendroCoreManager.checkReactionsNear(level, cloud.position(), radius, Element.PYRO, owner);

            // 3. Ambient visual flame particles and crackling sound
            level.sendParticles(ParticleTypes.FLAME, cloud.getX(), cloud.getY() + 0.1D, cloud.getZ(), 6, radius * 0.4D, 0.15D, radius * 0.4D, 0.02D);
            level.sendParticles(ParticleTypes.SMOKE, cloud.getX(), cloud.getY() + 0.2D, cloud.getZ(), 3, radius * 0.3D, 0.2D, radius * 0.3D, 0.01D);
            if (now % 20 == 0) {
                level.playSound(null, cloud.getX(), cloud.getY(), cloud.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.0F);
            }
        }
    }

    public static void clearAll() {
        FIELDS.clear();
    }
}
