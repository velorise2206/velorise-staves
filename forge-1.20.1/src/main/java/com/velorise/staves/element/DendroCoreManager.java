package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

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

    public static class HyperbloomMissile {
        public final ServerLevel level;
        public final Vec3 p0;
        public final Vec3 p1;
        public final Vec3 p2;
        public final @Nullable LivingEntity target;
        public Vec3 lastKnownTargetPos;
        public final float damage;
        public final @Nullable ServerPlayer attacker;
        public final int totalTicks;
        public int currentTick;

        public HyperbloomMissile(
                ServerLevel level,
                Vec3 p0,
                Vec3 p1,
                Vec3 p2,
                @Nullable LivingEntity target,
                Vec3 initialTargetPos,
                float damage,
                @Nullable ServerPlayer attacker,
                int totalTicks
        ) {
            this.level = level;
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            this.target = target;
            this.lastKnownTargetPos = initialTargetPos;
            this.damage = damage;
            this.attacker = attacker;
            this.totalTicks = Math.max(3, totalTicks);
            this.currentTick = 0;
        }

        public Vec3 evaluate(float t, Vec3 currentTargetPos) {
            float u = 1.0F - t;
            float tt = t * t;
            float uu = u * u;
            float uuu = uu * u;
            float ttt = tt * t;

            // Cubic Bézier: B(t) = (1-t)^3 * P0 + 3(1-t)^2 * t * P1 + 3(1-t) * t^2 * P2 + t^3 * P3
            double x = uuu * p0.x + 3.0D * uu * t * p1.x + 3.0D * u * tt * p2.x + ttt * currentTargetPos.x;
            double y = uuu * p0.y + 3.0D * uu * t * p1.y + 3.0D * u * tt * p2.y + ttt * currentTargetPos.y;
            double z = uuu * p0.z + 3.0D * uu * t * p1.z + 3.0D * u * tt * p2.z + ttt * currentTargetPos.z;

            return new Vec3(x, y, z);
        }
    }

    private static final List<CoreEntry> CORES = new CopyOnWriteArrayList<>();
    private static final List<HyperbloomMissile> MISSILES = new CopyOnWriteArrayList<>();
    private static final DustParticleOptions HYPERBLOOM_GREEN = new DustParticleOptions(new Vector3f(0.32F, 1.0F, 0.15F), 1.6F);
    private static final DustParticleOptions HYPERBLOOM_BRIGHT_GREEN = new DustParticleOptions(new Vector3f(0.55F, 1.0F, 0.25F), 1.2F);
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
        // Electro discharge at seed bursting into vibrant Dendro green energy
        core.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.35D, pos.z, 15, 0.25D, 0.25D, 0.25D, 0.08D);
        core.level.sendParticles(HYPERBLOOM_GREEN, pos.x, pos.y + 0.35D, pos.z, 20, 0.3D, 0.3D, 0.3D, 0.0D);
        core.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.8F);

        // Find nearest living enemy within 16 blocks (Sprawling Shot homing)
        AABB search = new AABB(pos.x - 16.0D, pos.y - 12.0D, pos.z - 16.0D, pos.x + 16.0D, pos.y + 16.0D, pos.z + 16.0D);
        LivingEntity nearest = null;
        double minD2 = Double.MAX_VALUE;
        for (LivingEntity entity : core.level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != attacker)) {
            double d2 = entity.position().distanceToSqr(pos);
            if (d2 < minD2) {
                minD2 = d2;
                nearest = entity;
            }
        }

        Vec3 p0 = pos.add(0, 0.35D, 0);
        Vec3 p3;
        if (nearest != null) {
            p3 = nearest.position().add(0, nearest.getBbHeight() * 0.55D, 0);
        } else {
            p3 = p0.add(core.level.random.nextDouble() * 4.0D - 2.0D, 3.5D, core.level.random.nextDouble() * 4.0D - 2.0D);
        }

        Vec3 diff = p3.subtract(p0);
        double dist = diff.length();
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        // Perpendicular horizontal vector for lateral arc sway (uốn lượn hình cánh cung)
        Vec3 horizDir = horizDist > 0.01D ? new Vec3(diff.x / horizDist, 0, diff.z / horizDist) : new Vec3(1, 0, 0);
        Vec3 sideVec = new Vec3(-horizDir.z, 0, horizDir.x);
        double sideOffset = (core.level.random.nextBoolean() ? 1.0D : -1.0D) * (0.9D + core.level.random.nextDouble() * 0.9D);
        Vec3 lateralOffset = sideVec.scale(sideOffset);

        // Launch apex calculation: always pops UP into the air first (even if target is a flying Vex)
        double apexHeight = Math.max(p0.y + 2.2D + horizDist * 0.15D, p3.y + 1.2D);
        Vec3 p1 = new Vec3(p0.x + diff.x * 0.2D, apexHeight, p0.z + diff.z * 0.2D).add(lateralOffset);
        Vec3 p2 = new Vec3(p0.x + diff.x * 0.7D, Math.max(p3.y + 0.8D, apexHeight - 0.4D), p0.z + diff.z * 0.7D).add(lateralOffset.scale(0.6D));

        float hyperbloomDmg = 16.0F * core.damageMultiplier;
        // Travel time: 5 to 9 ticks (~0.25s to 0.45s) based on distance
        int flightTicks = Math.max(5, Math.min(9, (int) Math.round(dist * 0.35D + 4.0D)));

        MISSILES.add(new HyperbloomMissile(core.level, p0, p1, p2, nearest, p3, hyperbloomDmg, attacker, flightTicks));
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
                if (victim instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, core.level.getGameTime()) == Element.PYRO) {
                    effectiveDmg *= 0.4F;
                }
                victim.hurt(core.level.damageSources().magic(), effectiveDmg);
                ElementalGaugeManager.syncAuras(victim, Collections.emptyList(), Element.DENDRO, Element.PYRO, ElementalReaction.BURGEON, effectiveDmg);
            }
        }
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();

        // 1. Tick Hyperbloom Bézier homing missiles (Sprawling Shot 3D flight)
        Iterator<HyperbloomMissile> missileIt = MISSILES.iterator();
        while (missileIt.hasNext()) {
            HyperbloomMissile m = missileIt.next();
            if (m.level != level) continue;

            m.currentTick++;
            float tPrev = (float) (m.currentTick - 1) / (float) m.totalTicks;
            float tCurr = (float) m.currentTick / (float) m.totalTicks;

            // Dynamically update target position if enemy is still alive and moving (e.g. flying Vex)
            Vec3 currentTargetPos;
            if (m.target != null && m.target.isAlive() && !m.target.isRemoved()) {
                currentTargetPos = m.target.position().add(0, m.target.getBbHeight() * 0.55D, 0);
                m.lastKnownTargetPos = currentTargetPos;
            } else {
                currentTargetPos = m.lastKnownTargetPos;
            }

            // Draw dense interpolated bright neon-green particle trail between tPrev and tCurr (matching Genshin Impact Sprawling Shot)
            int subSteps = 7;
            for (int s = 1; s <= subSteps; s++) {
                float subT = tPrev + (tCurr - tPrev) * ((float) s / (float) subSteps);
                Vec3 p = m.evaluate(subT, currentTargetPos);
                // Vibrant neon lime-green core and outer aura
                level.sendParticles(HYPERBLOOM_GREEN, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(HYPERBLOOM_BRIGHT_GREEN, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
                if (s == subSteps) {
                    level.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 2, 0.03D, 0.03D, 0.03D, 0.01D);
                    level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.01D);
                }
            }

            // Play flying hum sound on tick 2
            if (m.currentTick == 2) {
                Vec3 midP = m.evaluate(tCurr, currentTargetPos);
                level.playSound(null, midP.x, midP.y, midP.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3F, 2.0F);
            }

            // Impact!
            if (m.currentTick >= m.totalTicks) {
                MISSILES.remove(m);
                Vec3 hitPos = m.evaluate(1.0F, currentTargetPos);

                // Impact particles & sound: Burst of Dendro green dust, spore blossom, electric sparks, and glow
                level.sendParticles(HYPERBLOOM_GREEN, hitPos.x, hitPos.y, hitPos.z, 30, 0.35D, 0.35D, 0.35D, 0.0D);
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, hitPos.x, hitPos.y, hitPos.z, 25, 0.4D, 0.4D, 0.4D, 0.05D);
                level.sendParticles(ParticleTypes.GLOW, hitPos.x, hitPos.y, hitPos.z, 8, 0.2D, 0.2D, 0.2D, 0.02D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y, hitPos.z, 15, 0.3D, 0.3D, 0.3D, 0.08D);
                level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.85F, 1.7F);

                LivingEntity victim = m.target;
                if (victim != null && victim.isAlive() && !victim.isRemoved()) {
                    if (canTakeBloomDamage(victim, level.getGameTime())) {
                        victim.hurt(level.damageSources().magic(), m.damage);
                    }
                    ElementalGaugeManager.syncAuras(victim, Collections.emptyList(), Element.DENDRO, Element.ELECTRO, ElementalReaction.HYPERBLOOM, m.damage);
                } else {
                    // Target died or moved away: hit nearest living entity within 2.5 blocks of impact point
                    AABB splashBox = new AABB(hitPos.x - 2.5D, hitPos.y - 2.5D, hitPos.z - 2.5D, hitPos.x + 2.5D, hitPos.y + 2.5D, hitPos.z + 2.5D);
                    for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, splashBox, e -> e.isAlive() && e != m.attacker)) {
                        if (canTakeBloomDamage(nearby, level.getGameTime())) {
                            nearby.hurt(level.damageSources().magic(), m.damage);
                            ElementalGaugeManager.syncAuras(nearby, Collections.emptyList(), Element.DENDRO, Element.ELECTRO, ElementalReaction.HYPERBLOOM, m.damage);
                            break;
                        }
                    }
                }
            }
        }

        // 2. Tick physical Dendro Cores on field
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
        MISSILES.clear();
        LAST_DMG_TICK.clear();
        DMG_INSTANCE_COUNT.clear();
    }
}
