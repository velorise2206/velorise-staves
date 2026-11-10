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
 * Manages Genshin Impact Dendro Cores in Fabric 1.21.1.
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
        public Vec3 pos;
        public final @Nullable LivingEntity target;
        public Vec3 lastKnownTargetPos;
        public final float damage;
        public final @Nullable LivingEntity attacker;
        public int currentTick;
        public final @Nullable Display.ItemDisplay visualEntity;

        public HyperbloomMissile(
                ServerLevel level,
                Vec3 initialPos,
                @Nullable LivingEntity target,
                Vec3 initialTargetPos,
                float damage,
                @Nullable LivingEntity attacker,
                @Nullable Display.ItemDisplay visualEntity
        ) {
            this.level = level;
            this.pos = initialPos;
            this.target = target;
            this.lastKnownTargetPos = initialTargetPos;
            this.damage = damage;
            this.attacker = attacker;
            this.currentTick = 0;
            this.visualEntity = visualEntity;
        }
    }

    private static final List<CoreEntry> CORES = new CopyOnWriteArrayList<>();
    private static final List<HyperbloomMissile> MISSILES = new CopyOnWriteArrayList<>();
    private static final Set<UUID> ACTIVE_MISSILE_VISUALS = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    // Vibrant neon lime dust (size 1.25F, bright dendro green)
    private static final DustParticleOptions HYPERBLOOM_FOOTPRINT = new DustParticleOptions(new Vector3f(0.35F, 1.0F, 0.2F), 1.25F);
    private static final Map<UUID, Long> LAST_DMG_TICK = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DMG_INSTANCE_COUNT = new java.util.concurrent.ConcurrentHashMap<>();

    private DendroCoreManager() {}

    private static @Nullable Display.ItemDisplay spawnVisualSeed(ServerLevel level, Vec3 pos) {
        try {
            Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level);
            if (display == null) return null;
            display.setPos(pos.x, pos.y, pos.z);
            display.getSlot(0).set(new ItemStack(VeloriseStavesMod.DENDRO_CORE));
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

            // Fullbright (15 sky, 15 block) so it shines brilliantly even in darkness
            CompoundTag brightnessTag = new CompoundTag();
            brightnessTag.putInt("sky", 15);
            brightnessTag.putInt("block", 15);
            nbt.put("brightness", brightnessTag);

            // Glowing outline tag & vibrant neon lime color override
            nbt.putByte("Glowing", (byte) 1);
            nbt.putInt("glow_color_override", 0x55FF22);
            nbt.putInt("teleport_duration", 1);

            // Scale to 65% (0.65F, 0.65F, 0.65F)
            Transformation transformation = new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F),
                    new Vector3f(0.65F, 0.65F, 0.65F),
                    new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F)
            );
            Tag transTag = Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().orElse(null);
            if (transTag != null) {
                nbt.put("transformation", transTag);
            }

            display.load(nbt);
            display.addTag(TAG_HYPERBLOOM_VISUAL);
            display.getSlot(0).set(new ItemStack(VeloriseStavesMod.DENDRO_CORE));
            display.setGlowingTag(true);

            try {
                Scoreboard sb = level.getScoreboard();
                PlayerTeam team = sb.getPlayerTeam("vs_dendro");
                if (team == null) {
                    team = sb.addPlayerTeam("vs_dendro");
                    team.setColor(ChatFormatting.GREEN);
                }
                sb.addPlayerToTeam(display.getScoreboardName(), team);
            } catch (Throwable ignored) {}

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

        ItemStack seedItem = new ItemStack(VeloriseStavesMod.DENDRO_CORE);
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

    public static void checkReactionsNear(ServerLevel level, Vec3 hitPos, double radius, Element attackElement, @Nullable LivingEntity attacker) {
        if (attackElement != Element.ELECTRO && attackElement != Element.PYRO) return;

        double r2 = radius * radius;
        for (CoreEntry core : CORES) {
            if (core.level != level) {
                continue;
            }
            if (!core.entity.isAlive() || core.entity.isRemoved()) {
                cleanUpCoreTeam(core);
                CORES.remove(core);
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
            if (item.getTags().contains(TAG_DENDRO_CORE) || item.getItem().getItem() == VeloriseStavesMod.DENDRO_CORE) {
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

    private static void triggerHyperbloom(CoreEntry core, @Nullable LivingEntity attacker) {
        cleanUpCoreTeam(core);
        Vec3 pos = core.entity.position();
        core.entity.discard();

        Vec3 p0 = pos.add(0, 0.35D, 0);

        // Electro trigger burst at origin: crisp electric sparks + luminous glow
        core.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p0.x, p0.y, p0.z, 6, 0.15D, 0.15D, 0.15D, 0.04D);
        core.level.sendParticles(ParticleTypes.GLOW, p0.x, p0.y, p0.z, 8, 0.2D, 0.2D, 0.2D, 0.04D);
        core.level.playSound(null, p0.x, p0.y, p0.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.65F, 1.8F);

        // Find random living target within 16 blocks (excluding triggerer/attacker)
        AABB search = new AABB(pos.x - 16.0D, pos.y - 12.0D, pos.z - 16.0D, pos.x + 16.0D, pos.y + 16.0D, pos.z + 16.0D);
        List<LivingEntity> candidates = core.level.getEntitiesOfClass(
                LivingEntity.class,
                search,
                e -> e.isAlive() && !e.isSpectator() && e != attacker
        );
        LivingEntity chosenTarget = candidates.isEmpty() ? null : candidates.get(core.level.random.nextInt(candidates.size()));

        Vec3 targetPos;
        if (chosenTarget != null) {
            targetPos = chosenTarget.position().add(0, chosenTarget.getBbHeight() * 0.55D, 0);
        } else {
            targetPos = p0.add(core.level.random.nextDouble() * 4.0D - 2.0D, 12.0D, core.level.random.nextDouble() * 4.0D - 2.0D);
        }

        float hyperbloomDmg = 16.0F * core.damageMultiplier;

        // Spawn fullbright, glowing Dendro Core seed projectile
        Display.ItemDisplay visualSeed = spawnVisualSeed(core.level, p0);

        MISSILES.add(new HyperbloomMissile(core.level, p0, chosenTarget, targetPos, hyperbloomDmg, attacker, visualSeed));
    }

    private static void triggerBurgeon(CoreEntry core, @Nullable LivingEntity attacker) {
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

        // 1. Tick Hyperbloom homing missiles (GenshinCraft flight physics & speed 0.8 blocks/tick)
        Iterator<HyperbloomMissile> missileIt = MISSILES.iterator();
        while (missileIt.hasNext()) {
            HyperbloomMissile m = missileIt.next();
            if (m.level != level) continue;

            m.currentTick++;

            // Dynamically update target position if enemy is still alive and moving
            Vec3 currentTargetPos;
            if (m.target != null && m.target.isAlive() && !m.target.isRemoved()) {
                currentTargetPos = m.target.position().add(0, m.target.getBbHeight() * 0.55D, 0);
                m.lastKnownTargetPos = currentTargetPos;
            } else {
                currentTargetPos = m.lastKnownTargetPos;
            }

            Vec3 toTarget = currentTargetPos.subtract(m.pos);
            double dist = toTarget.length();

            // Hit detection: close enough or intersecting target bounding box
            boolean hit = dist <= 0.85D;
            if (!hit && m.target != null && m.target.isAlive()) {
                AABB missileBox = new AABB(m.pos.x - 0.4D, m.pos.y - 0.4D, m.pos.z - 0.4D, m.pos.x + 0.4D, m.pos.y + 0.4D, m.pos.z + 0.4D);
                hit = m.target.getBoundingBox().intersects(missileBox);
            }

            if (hit || m.currentTick > 100) { // Hit or timeout (5 seconds)
                MISSILES.remove(m);
                if (m.visualEntity != null) {
                    try {
                        Scoreboard sb = level.getScoreboard();
                        PlayerTeam team = sb.getPlayerTeam("vs_dendro");
                        if (team != null) {
                            sb.removePlayerFromTeam(m.visualEntity.getScoreboardName(), team);
                        }
                    } catch (Throwable ignored) {}
                    ACTIVE_MISSILE_VISUALS.remove(m.visualEntity.getUUID());
                    m.visualEntity.discard();
                }

                Vec3 hitPos = m.pos;

                // Impact burst: luminous green flash, electric sparks, and chime
                level.sendParticles(HYPERBLOOM_FOOTPRINT, hitPos.x, hitPos.y, hitPos.z, 16, 0.25D, 0.25D, 0.25D, 0.05D);
                level.sendParticles(ParticleTypes.GLOW, hitPos.x, hitPos.y, hitPos.z, 12, 0.25D, 0.25D, 0.25D, 0.04D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y, hitPos.z, 6, 0.2D, 0.2D, 0.2D, 0.03D);
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
                continue;
            }

            // GenshinCraft flight vector formula
            Vec3 dir;
            if (m.target == null) {
                dir = new Vec3(0.0D, 0.5D, 0.0D);
            } else {
                dir = dist > 1.0E-4D ? toTarget.scale(1.0D / dist) : new Vec3(0, 1, 0);
                int age = m.currentTick;
                if (age < 10) {
                    dir = dir.add(0.0D, 1.0D, 0.0D).normalize();
                } else if (age < 20) {
                    dir = dir.add(0.0D, (20 - age) * 0.1D, 0.0D).normalize();
                }
            }

            double stepSpeed = 0.8D; // GenshinCraft exact speed: 0.8 blocks/tick (16 blocks/second)
            Vec3 nextPos = m.pos.add(dir.scale(stepSpeed));
            m.pos = nextPos;

            // Move the glowing 50% scale seed visual entity along the trajectory
            if (m.visualEntity != null && m.visualEntity.isAlive()) {
                m.visualEntity.teleportTo(nextPos.x, nextPos.y, nextPos.z);
            }

            // Brilliant glowing Hyperbloom homing missile trail (vivid emerald lime + glow sparkles)
            level.sendParticles(HYPERBLOOM_FOOTPRINT, nextPos.x, nextPos.y, nextPos.z, 3, 0.04D, 0.04D, 0.04D, 0.01D);
            level.sendParticles(ParticleTypes.GLOW, nextPos.x, nextPos.y, nextPos.z, 2, 0.03D, 0.03D, 0.03D, 0.01D);
            if (m.currentTick % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, nextPos.x, nextPos.y, nextPos.z, 1, 0.01D, 0.01D, 0.01D, 0.005D);
            }

            // Play flying hum / whistling sound at launch and mid-flight
            if (m.currentTick == 2 || m.currentTick % 8 == 0) {
                level.playSound(null, nextPos.x, nextPos.y, nextPos.z, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.4F, 1.9F);
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

            // Proximity check: ANY player or monster near the Dendro Core with Electro or Pyro triggers Hyperbloom / Burgeon!
            AABB triggerBox = core.entity.getBoundingBox().inflate(1.6D, 0.8D, 1.6D);
            LivingEntity electroTriggerer = null;
            LivingEntity pyroTriggerer = null;
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, triggerBox, LivingEntity::isAlive)) {
                Element activeShield = CrystallizeShardManager.getActiveShield(nearby);
                if (ElementalGaugeManager.hasElement(nearby, Element.ELECTRO)
                        || ElementalGaugeManager.getInnateElement(nearby) == Element.ELECTRO
                        || activeShield == Element.ELECTRO) {
                    electroTriggerer = nearby;
                    break;
                }
                if (ElementalGaugeManager.hasElement(nearby, Element.PYRO)
                        || ElementalGaugeManager.getInnateElement(nearby) == Element.PYRO
                        || nearby.isOnFire()
                        || activeShield == Element.PYRO) {
                    pyroTriggerer = nearby;
                }
            }

            if (electroTriggerer != null) {
                CORES.remove(core);
                triggerHyperbloom(core, electroTriggerer);
                continue;
            } else if (pyroTriggerer != null) {
                CORES.remove(core);
                triggerBurgeon(core, pyroTriggerer);
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
            net.minecraft.world.item.Item dendroCoreItem = VeloriseStavesMod.DENDRO_CORE;
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
                try {
                    Scoreboard sb = m.level.getScoreboard();
                    PlayerTeam team = sb.getPlayerTeam("vs_dendro");
                    if (team != null) {
                        sb.removePlayerFromTeam(m.visualEntity.getScoreboardName(), team);
                    }
                } catch (Throwable ignored) {}
                m.visualEntity.discard();
            }
        }
        MISSILES.clear();
        ACTIVE_MISSILE_VISUALS.clear();
        LAST_DMG_TICK.clear();
        DMG_INSTANCE_COUNT.clear();
    }
}
