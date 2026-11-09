package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Genshin Impact Dendro Cores (Hạt Nhân Thảo) in Forge 1.20.1.
 * Handles Bloom seed physical dropping, water buoyancy, 6s timeout explosion,
 * Hyperbloom (Electro homing missile), and Burgeon (Pyro 5m AoE blast).
 */
public final class DendroCoreManager {
    public static final String TAG_DENDRO_CORE = "velorise_staves:dendro_core";
    private static final int MAX_CORES = 5;
    private static final int CORE_LIFETIME_TICKS = 120; // 6 seconds

    public static class CoreEntry {
        public final ItemEntity entity;
        public final ServerLevel level;
        public final long spawnTime;
        public final @Nullable UUID ownerId;
        public final float damageMultiplier;

        public CoreEntry(ItemEntity entity, ServerLevel level, long spawnTime, @Nullable UUID ownerId, float damageMultiplier) {
            this.entity = entity;
            this.level = level;
            this.spawnTime = spawnTime;
            this.ownerId = ownerId;
            this.damageMultiplier = damageMultiplier;
        }
    }

    private static final List<CoreEntry> CORES = new CopyOnWriteArrayList<>();
    private static final Map<UUID, Long> LAST_DMG_TICK = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DMG_INSTANCE_COUNT = new java.util.concurrent.ConcurrentHashMap<>();

    private DendroCoreManager() {}

    private static boolean canTakeBloomDamage(LivingEntity victim, long now) {
        UUID id = victim.getUUID();
        Long lastTick = LAST_DMG_TICK.get(id);
        if (lastTick == null || now - lastTick >= 10L) { // New 0.5s window
            LAST_DMG_TICK.put(id, now);
            DMG_INSTANCE_COUNT.put(id, 1);
            return true;
        }
        int count = DMG_INSTANCE_COUNT.getOrDefault(id, 0);
        if (count < 2) {
            DMG_INSTANCE_COUNT.put(id, count + 1);
            return true;
        }
        return false;
    }

    public static void spawn(ServerLevel level, Vec3 pos, @Nullable ServerPlayer owner, float damageMultiplier) {
        // Enforce max 5 cores per owner (or 5 for wild/unowned); oldest detonates immediately when 6th is spawned
        UUID ownerId = owner != null ? owner.getUUID() : null;
        int ownerCoreCount = 0;
        CoreEntry oldestOwnerCore = null;
        for (CoreEntry core : CORES) {
            if (core.entity.isAlive() && !core.entity.isRemoved()) {
                boolean sameOwner = (ownerId == null && core.ownerId == null)
                        || (ownerId != null && ownerId.equals(core.ownerId));
                if (sameOwner) {
                    ownerCoreCount++;
                    if (oldestOwnerCore == null) {
                        oldestOwnerCore = core;
                    }
                }
            }
        }

        if (ownerCoreCount >= MAX_CORES && oldestOwnerCore != null) {
            detonateBloom(oldestOwnerCore);
            CORES.remove(oldestOwnerCore);
        }

        ItemStack seedItem = new ItemStack(VeloriseStavesMod.DENDRO_CORE.get());
        ItemEntity item = new ItemEntity(level, pos.x, pos.y + 0.1D, pos.z, seedItem);
        item.setPickUpDelay(32767);
        item.setUnlimitedLifetime();
        item.setDeltaMovement((level.random.nextDouble() - 0.5D) * 0.12D, 0.22D, (level.random.nextDouble() - 0.5D) * 0.12D);
        item.addTag(TAG_DENDRO_CORE);
        level.addFreshEntity(item);

        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.5D, pos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 1.2F);

        CORES.add(new CoreEntry(item, level, level.getGameTime(), owner != null ? owner.getUUID() : null, damageMultiplier));
    }

    public static void checkReactionsNear(ServerLevel level, Vec3 hitPos, double radius, Element attackElement, @Nullable ServerPlayer attacker) {
        if (attackElement != Element.ELECTRO && attackElement != Element.PYRO) return;

        Iterator<CoreEntry> it = CORES.iterator();
        while (it.hasNext()) {
            CoreEntry core = it.next();
            if (core.level != level || !core.entity.isAlive() || core.entity.isRemoved()) {
                CORES.remove(core);
                continue;
            }
            if (core.entity.position().distanceToSqr(hitPos) <= radius * radius) {
                CORES.remove(core);
                if (attackElement == Element.ELECTRO) {
                    triggerHyperbloom(core, attacker);
                } else if (attackElement == Element.PYRO) {
                    triggerBurgeon(core, attacker);
                }
            }
        }
    }

    private static void detonateBloom(CoreEntry core) {
        Vec3 pos = core.entity.position();
        core.entity.discard();
        core.level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.3D, pos.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
        core.level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.5D, pos.z, 25, 0.7D, 0.5D, 0.7D, 0.05D);
        core.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.3F);

        float dmg = 8.0F * core.damageMultiplier;
        AABB box = new AABB(pos.x - 3.0D, pos.y - 2.0D, pos.z - 3.0D, pos.x + 3.0D, pos.y + 3.0D, pos.z + 3.0D);
        for (LivingEntity victim : core.level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (canTakeBloomDamage(victim, core.level.getGameTime())) {
                victim.hurt(core.level.damageSources().magic(), dmg);
                ElementalGaugeManager.syncAuras(victim, Collections.emptyList(), Element.DENDRO, null, ElementalReaction.BLOOM, dmg);
            }
        }
    }

    private static void triggerHyperbloom(CoreEntry core, @Nullable ServerPlayer attacker) {
        Vec3 pos = core.entity.position();
        core.entity.discard();
        core.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.4D, pos.z, 20, 0.3D, 0.3D, 0.3D, 0.08D);
        core.level.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.4D, pos.z, 15, 0.3D, 0.3D, 0.3D, 0.06D);
        core.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.9F, 1.6F);

        // Find nearest living enemy within 16 blocks (Sprawling Shot homing)
        AABB search = new AABB(pos.x - 16.0D, pos.y - 8.0D, pos.z - 16.0D, pos.x + 16.0D, pos.y + 8.0D, pos.z + 16.0D);
        LivingEntity nearest = null;
        double minD2 = Double.MAX_VALUE;
        for (LivingEntity entity : core.level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != attacker)) {
            double d2 = entity.position().distanceToSqr(pos);
            if (d2 < minD2) {
                minD2 = d2;
                nearest = entity;
            }
        }

        if (nearest != null) {
            float hyperbloomDmg = 16.0F * core.damageMultiplier;
            if (canTakeBloomDamage(nearest, core.level.getGameTime())) {
                nearest.hurt(core.level.damageSources().magic(), hyperbloomDmg);
            }
            Vec3 targetPos = nearest.position();
            drawHomingTrail(core.level, pos, targetPos.add(0, nearest.getBbHeight() * 0.5D, 0));
            core.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetPos.x, targetPos.y + 0.8D, targetPos.z, 25, 0.4D, 0.4D, 0.4D, 0.09D);
            core.level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, targetPos.x, targetPos.y + 0.8D, targetPos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
            ElementalGaugeManager.syncAuras(nearest, Collections.emptyList(), Element.DENDRO, Element.ELECTRO, ElementalReaction.HYPERBLOOM, hyperbloomDmg);
        }
    }

    private static void drawHomingTrail(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        int steps = Math.max(6, (int) Math.round(dist * 3.5D));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            Vec3 p = start.add(diff.scale(t));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.01D);
            level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.01D);
        }
    }

    private static void triggerBurgeon(CoreEntry core, @Nullable ServerPlayer attacker) {
        Vec3 pos = core.entity.position();
        core.entity.discard();
        core.level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5D, pos.z, 3, 0.3D, 0.3D, 0.3D, 0.0D);
        core.level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5D, pos.z, 35, 0.8D, 0.5D, 0.8D, 0.08D);
        core.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.1F);

        float burgeonDmg = 17.5F * core.damageMultiplier;
        AABB box = new AABB(pos.x - 5.0D, pos.y - 3.0D, pos.z - 5.0D, pos.x + 5.0D, pos.y + 3.0D, pos.z + 5.0D);
        for (LivingEntity victim : core.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != attacker)) {
            if (canTakeBloomDamage(victim, core.level.getGameTime())) {
                float effectiveDmg = burgeonDmg;
                if (victim instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp.getUUID(), core.level.getGameTime()) == Element.PYRO) {
                    effectiveDmg *= 0.4F;
                }
                victim.hurt(core.level.damageSources().magic(), effectiveDmg);
                ElementalGaugeManager.syncAuras(victim, Collections.emptyList(), Element.DENDRO, Element.PYRO, ElementalReaction.BURGEON, effectiveDmg);
            }
        }
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<CoreEntry> it = CORES.iterator();
        while (it.hasNext()) {
            CoreEntry core = it.next();
            if (core.level != level) continue;
            if (!core.entity.isAlive() || core.entity.isRemoved()) {
                CORES.remove(core);
                continue;
            }

            // Ambient pulsating green particles around the Dendro Core
            if (now % 8 == 0) {
                Vec3 p = core.entity.position();
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y + 0.25D, p.z, 3, 0.2D, 0.2D, 0.2D, 0.02D);
                level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 2, 0.15D, 0.15D, 0.15D, 0.01D);
            }

            // Explode after 6 seconds (120 ticks)
            if (now - core.spawnTime >= CORE_LIFETIME_TICKS) {
                CORES.remove(core);
                detonateBloom(core);
            }
        }
    }

    public static void clearAll() {
        for (CoreEntry core : CORES) {
            if (core.entity.isAlive()) {
                core.entity.discard();
            }
        }
        CORES.clear();
        LAST_DMG_TICK.clear();
        DMG_INSTANCE_COUNT.clear();
    }
}
