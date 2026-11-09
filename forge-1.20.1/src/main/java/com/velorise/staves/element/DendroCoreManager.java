package com.velorise.staves.element;

import com.mojang.math.Transformation;
import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Genshin Impact Dendro Cores in Forge 1.20.1.
 * Handles Bloom seed physical dropping, water buoyancy, 6s timeout explosion,
 * Hyperbloom (50% seed projectile with Genshin crescent homing arc), and Burgeon (Pyro 5m AoE blast).
 */
public final class DendroCoreManager {
    public static final String TAG_DENDRO_CORE = "velorise_staves:dendro_core";
    public static final String TAG_HYPERBLOOM_VISUAL = "velorise_staves:hyperbloom_visual";
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
        public final @Nullable Display.ItemDisplay visualEntity;

        public HyperbloomMissile(
                ServerLevel level,
                Vec3 p0,
                Vec3 p1,
                Vec3 p2,
                @Nullable LivingEntity target,
                Vec3 initialTargetPos,
                float damage,
                @Nullable ServerPlayer attacker,
                int totalTicks,
                @Nullable Display.ItemDisplay visualEntity
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
            this.visualEntity = visualEntity;
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
    private static final Set<UUID> ACTIVE_MISSILE_VISUALS = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    // Subtle, faint footprint trail particles (delicate 0.55F neon lime dots, replacing particle clutter)
    private static final DustParticleOptions HYPERBLOOM_FOOTPRINT = new DustParticleOptions(new Vector3f(0.42F, 0.95F, 0.22F), 0.55F);
    private static final Map<UUID, Long> LAST_DMG_TICK = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DMG_INSTANCE_COUNT = new java.util.concurrent.ConcurrentHashMap<>();

    private DendroCoreManager() {}

    private static @Nullable Display.ItemDisplay spawnVisualSeed(ServerLevel level, Vec3 pos) {
        try {
            Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
            if (display == null) return null;
            display.setPos(pos.x, pos.y, pos.z);
            display.getSlot(0).set(new ItemStack(VeloriseStavesMod.DENDRO_CORE.get()));
            display.setNoGravity(true);
            display.noPhysics = true;
            display.addTag(TAG_HYPERBLOOM_VISUAL);

            CompoundTag nbt = new CompoundTag();
            ListTag posList = new ListTag();
            posList.add(DoubleTag.valueOf(pos.x));
            posList.add(DoubleTag.valueOf(pos.y));
            posList.add(DoubleTag.valueOf(pos.z));
            nbt.put("Pos", posList);

            ListTag rotList = new ListTag();
            rotList.add(FloatTag.valueOf(0.0F));
            rotList.add(FloatTag.valueOf(0.0F));
            nbt.put("Rotation", rotList);

            nbt.putString("billboard", "center");

            // Scale down to 50% (0.5F, 0.5F, 0.5F)
            Transformation transformation = new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F),
                    new Vector3f(0.5F, 0.5F, 0.5F),
                    new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F)
            );
            Tag transTag = Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().orElse(null);
            if (transTag != null) {
                nbt.put("transformation", transTag);
            }

            display.load(nbt);
            display.addTag(TAG_HYPERBLOOM_VISUAL);
            display.getSlot(0).set(new ItemStack(VeloriseStavesMod.DENDRO_CORE.get()));
            level.addFreshEntity(display);
            ACTIVE_MISSILE_VISUALS.add(display.getUUID());
            return display;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean canTakeBloomDamage(LivingEntity victim, long now) {
        UUID id = victim.getUUID();
        Long lastTick = LAST_DMG_TICK.get(id);
        if (lastTick == null || now - lastTick >= 10L) { // 0.5s window
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
        item.setDeltaMovement((level.random.nextDouble() - 0.5D) * 0.12D, 0.22D, (level.random.nextDouble() - 0.5D) * 0.12D);
        item.addTag(TAG_DENDRO_CORE);
        item.setGlowingTag(true);
        try {
            Scoreboard sb = level.getScoreboard();
            PlayerTeam team = sb.getPlayerTeam("vs_dendro");
            if (team == null) {
                team = sb.addPlayerTeam("vs_dendro");
                team.setColor(ChatFormatting.GREEN);
            }
            sb.addPlayerToTeam(item.getScoreboardName(), team);
        } catch (Throwable ignored) {}
        level.addFreshEntity(item);

        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.5D, pos.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 1.2F);

        CORES.add(new CoreEntry(item, level, level.getGameTime(), owner != null ? owner.getUUID() : null, damageMultiplier));
    }

    private static void cleanUpCoreTeam(CoreEntry core) {
        try {
            Scoreboard sb = core.level.getScoreboard();
            PlayerTeam team = sb.getPlayerTeam("vs_dendro");
            if (team != null) {
                sb.removePlayerFromTeam(core.entity.getScoreboardName(), team);
            }
        } catch (Throwable ignored) {}
    }

    public static void checkReactionsNear(ServerLevel level, Vec3 hitPos, double radius, Element attackElement, @Nullable ServerPlayer attacker) {
        if (attackElement != Element.ELECTRO && attackElement != Element.PYRO) return;

        double r2 = radius * radius;
        for (CoreEntry core : CORES) {
            if (!core.entity.isAlive() || core.entity.isRemoved()) {
                cleanUpCoreTeam(core);
                CORES.remove(core);
                continue;
            }
            if (core.level != level) {
                continue;
            }
            if (core.entity.position().distanceToSqr(hitPos) <= r2) {
                CORES.remove(core);
                if (attackElement == Element.ELECTRO) {
                    triggerHyperbloom(core, attacker);
                } else if (attackElement == Element.PYRO) {
                    triggerBurgeon(core, attacker);
                }
            }
        }

        // Also detect and react any untracked or orphaned seeds in the blast zone
        AABB box = new AABB(hitPos.x - radius, hitPos.y - radius, hitPos.z - radius, hitPos.x + radius, hitPos.y + radius, hitPos.z + radius);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.isRemoved())) {
            if (item.getTags().contains(TAG_DENDRO_CORE) || item.getItem().getItem() == VeloriseStavesMod.DENDRO_CORE.get()) {
                CoreEntry tempCore = new CoreEntry(item, level, level.getGameTime(), attacker != null ? attacker.getUUID() : null, 1.0F);
                if (attackElement == Element.ELECTRO) {
                    triggerHyperbloom(tempCore, attacker);
                } else if (attackElement == Element.PYRO) {
                    triggerBurgeon(tempCore, attacker);
                }
            }
        }
    }

    private static void detonateBloom(CoreEntry core) {
        cleanUpCoreTeam(core);
        Vec3 pos = core.entity.position();
        core.entity.discard();
        core.level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.3D, pos.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
        core.level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y + 0.5D, pos.z, 25, 0.7D, 0.5D, 0.7D, 0.05D);
        core.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.3F);

        float dmg = 8.0F * core.damageMultiplier;
        AABB box = new AABB(pos.x - 5.0D, pos.y - 3.0D, pos.z - 5.0D, pos.x + 5.0D, pos.y + 3.0D, pos.z + 5.0D);
        for (LivingEntity victim : core.level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (canTakeBloomDamage(victim, core.level.getGameTime())) {
                victim.hurt(core.level.damageSources().magic(), dmg);
                ElementalGaugeManager.syncAuras(victim, Collections.emptyList(), Element.DENDRO, null, ElementalReaction.BLOOM, dmg);
            }
        }
    }

    private static void triggerHyperbloom(CoreEntry core, @Nullable ServerPlayer attacker) {
        cleanUpCoreTeam(core);
        Vec3 pos = core.entity.position();
        core.entity.discard();

        // Electro trigger burst at origin
        core.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.35D, pos.z, 10, 0.2D, 0.2D, 0.2D, 0.06D);
        core.level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.8F);

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

        // Calculate Genshin crescent swoop control points:
        // Missile launches outward to the flank and climbs, reaching an apex in an arc before swooping down into target
        Vec3 u = horizDist > 0.01D ? new Vec3(diff.x / horizDist, 0, diff.z / horizDist) : new Vec3(1, 0, 0);
        Vec3 w = new Vec3(-u.z, 0, u.x); // Perpendicular horizontal normal
        double sideSign = core.level.random.nextBoolean() ? 1.0D : -1.0D;
        double lateralDist = 2.6D + core.level.random.nextDouble() * 1.6D; // 2.6 to 4.2 blocks to the flank
        double climbHeight = Math.max(3.2D, Math.max(diff.y, 0.0D) + 2.8D + horizDist * 0.15D);

        // P1: launches outwards laterally and upwards with slight forward motion
        Vec3 p1 = p0.add(w.scale(sideSign * lateralDist))
                .add(0, climbHeight, 0)
                .add(u.scale(Math.min(horizDist * 0.25D, 1.5D)));

        // P2: curves at apex and approaches target from the flank above
        Vec3 p2 = p3.add(w.scale(sideSign * lateralDist * 0.45D))
                .add(0, Math.max(1.8D, climbHeight * 0.5D), 0)
                .subtract(u.scale(Math.min(horizDist * 0.35D, 2.0D)));

        float hyperbloomDmg = 16.0F * core.damageMultiplier;
        // Travel duration: 14 to 22 ticks (~0.7s to 1.1s) for a smooth, readable swoop
        int flightTicks = Math.max(14, Math.min(22, (int) Math.round(dist * 0.65D + 11.0D)));

        // Spawn 50% scale Dendro Core seed projectile
        Display.ItemDisplay visualSeed = spawnVisualSeed(core.level, p0);

        MISSILES.add(new HyperbloomMissile(core.level, p0, p1, p2, nearest, p3, hyperbloomDmg, attacker, flightTicks, visualSeed));
    }

    private static void triggerBurgeon(CoreEntry core, @Nullable ServerPlayer attacker) {
        cleanUpCoreTeam(core);
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
        if (now % 100 == 0) {
            LAST_DMG_TICK.entrySet().removeIf(e -> {
                if (now - e.getValue() >= 20L) {
                    DMG_INSTANCE_COUNT.remove(e.getKey());
                    return true;
                }
                return false;
            });
        }

        // 1. Tick Hyperbloom Bézier homing missiles (Sprawling Shot 3D flight)
        Iterator<HyperbloomMissile> missileIt = MISSILES.iterator();
        while (missileIt.hasNext()) {
            HyperbloomMissile m = missileIt.next();
            if (m.level != level) continue;

            m.currentTick++;
            float tCurr = (float) m.currentTick / (float) m.totalTicks;

            // Dynamically update target position if enemy is still alive and moving (e.g. flying Vex)
            Vec3 currentTargetPos;
            if (m.target != null && m.target.isAlive() && !m.target.isRemoved()) {
                currentTargetPos = m.target.position().add(0, m.target.getBbHeight() * 0.55D, 0);
                m.lastKnownTargetPos = currentTargetPos;
            } else {
                currentTargetPos = m.lastKnownTargetPos;
            }

            Vec3 p = m.evaluate(tCurr, currentTargetPos);

            // Move the 50% scale seed visual entity along the curve
            if (m.visualEntity != null && m.visualEntity.isAlive()) {
                m.visualEntity.teleportTo(p.x, p.y, p.z);
            }

            // Clean, delicate footprint trail behind the seed (no particle clutter)
            level.sendParticles(HYPERBLOOM_FOOTPRINT, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            if (m.currentTick % 3 == 0) {
                level.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }

            // Play flying hum / whistling sound at launch and mid-flight
            if (m.currentTick == 2 || m.currentTick == m.totalTicks / 2) {
                level.playSound(null, p.x, p.y, p.z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 1.9F);
            }

            // Impact!
            if (m.currentTick >= m.totalTicks) {
                MISSILES.remove(m);
                if (m.visualEntity != null) {
                    ACTIVE_MISSILE_VISUALS.remove(m.visualEntity.getUUID());
                    m.visualEntity.discard();
                }

                Vec3 hitPos = m.evaluate(1.0F, currentTargetPos);

                // Clean impact burst: delicate pop of green particles and electric sparks
                level.sendParticles(HYPERBLOOM_FOOTPRINT, hitPos.x, hitPos.y, hitPos.z, 14, 0.25D, 0.25D, 0.25D, 0.02D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y, hitPos.z, 8, 0.2D, 0.2D, 0.2D, 0.05D);
                level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.75F, 1.8F);

                // Genshin Impact: Hyperbloom deals small AoE splash damage with 1.0m radius around impact point
                AABB splashBox = new AABB(hitPos.x - 1.0D, hitPos.y - 1.0D, hitPos.z - 1.0D, hitPos.x + 1.0D, hitPos.y + 1.0D, hitPos.z + 1.0D);
                boolean hitAny = false;
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, splashBox, e -> e.isAlive() && e != m.attacker)) {
                    if (canTakeBloomDamage(nearby, level.getGameTime())) {
                        nearby.hurt(level.damageSources().magic(), m.damage);
                        ElementalGaugeManager.syncAuras(nearby, Collections.emptyList(), Element.DENDRO, Element.ELECTRO, ElementalReaction.HYPERBLOOM, m.damage);
                        hitAny = true;
                    }
                }
                LivingEntity victim = m.target;
                if (!hitAny && victim != null && victim.isAlive() && !victim.isRemoved() && victim != m.attacker) {
                    if (canTakeBloomDamage(victim, level.getGameTime())) {
                        victim.hurt(level.damageSources().magic(), m.damage);
                    }
                    ElementalGaugeManager.syncAuras(victim, Collections.emptyList(), Element.DENDRO, Element.ELECTRO, ElementalReaction.HYPERBLOOM, m.damage);
                }
            }
        }

        // 2. Tick physical Dendro Cores on field
        Iterator<CoreEntry> it = CORES.iterator();
        while (it.hasNext()) {
            CoreEntry core = it.next();
            if (core.level != level) continue;
            if (!core.entity.isAlive() || core.entity.isRemoved()) {
                cleanUpCoreTeam(core);
                CORES.remove(core);
                continue;
            }

            // Ambient pulsating green particles and soft glowing aura around the Dendro Core
            if (now % 6 == 0) {
                Vec3 p = core.entity.position();
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y + 0.25D, p.z, 3, 0.2D, 0.2D, 0.2D, 0.02D);
                level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 2, 0.15D, 0.15D, 0.15D, 0.01D);
                level.sendParticles(new DustParticleOptions(new Vector3f(0.38F, 0.95F, 0.2F), 0.75F), p.x, p.y + 0.25D, p.z, 2, 0.12D, 0.12D, 0.12D, 0.02D);
            }

            // Explode after 6 seconds (120 ticks)
            if (now - core.spawnTime >= CORE_LIFETIME_TICKS) {
                CORES.remove(core);
                detonateBloom(core);
            }
        }

        // 3. Auto-Purge Orphaned Seeds & Visual Entities
        if (now % 20 == 0) {
            net.minecraft.world.item.Item dendroCoreItem = VeloriseStavesMod.DENDRO_CORE.get();
            for (ServerPlayer player : level.players()) {
                AABB scanBox = player.getBoundingBox().inflate(64.0D);

                // A. Purge orphaned ItemDisplay seeds (from previous crashes, disconnects, etc.)
                List<Display.ItemDisplay> displays = level.getEntitiesOfClass(Display.ItemDisplay.class, scanBox);
                for (Display.ItemDisplay disp : displays) {
                    if (disp.getTags().contains(TAG_HYPERBLOOM_VISUAL)
                            || disp.getSlot(0).get().getItem() == dendroCoreItem) {
                        if (!ACTIVE_MISSILE_VISUALS.contains(disp.getUUID())) {
                            disp.discard();
                        }
                    }
                }

                // B. Adopt or clean orphaned ItemEntity Bloom seeds
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, scanBox);
                for (ItemEntity item : items) {
                    if (item.getTags().contains(TAG_DENDRO_CORE) || item.getItem().getItem() == dendroCoreItem) {
                        boolean tracked = false;
                        for (CoreEntry c : CORES) {
                            if (c.entity == item || c.entity.getUUID().equals(item.getUUID())) {
                                tracked = true;
                                break;
                            }
                        }
                        if (!tracked) {
                            if (item.getAge() >= CORE_LIFETIME_TICKS) {
                                cleanUpCoreTeam(new CoreEntry(item, level, now, null, 1.0F));
                                item.discard();
                            } else {
                                CORES.add(new CoreEntry(item, level, now - Math.max(0, item.getAge()), null, 1.0F));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void clearAll() {
        for (CoreEntry core : CORES) {
            cleanUpCoreTeam(core);
            core.entity.discard();
        }
        CORES.clear();
        for (HyperbloomMissile m : MISSILES) {
            if (m.visualEntity != null) {
                m.visualEntity.discard();
            }
        }
        MISSILES.clear();
        ACTIVE_MISSILE_VISUALS.clear();
        LAST_DMG_TICK.clear();
        DMG_INSTANCE_COUNT.clear();
    }
}
