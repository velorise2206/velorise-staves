package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.effect.ModMobEffects;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Genshin Impact Crystallize Elemental Shards in Minecraft.
 * When Geo hits Pyro/Hydro/Electro/Cryo, drops a 3D spinning elemental crystal shard.
 * Only players can pick it up; grants 1 golden heart Absorption (15s) with 250% matching elemental absorption efficiency.
 * If the Absorption shield hearts are depleted (getAbsorptionAmount() <= 0), the elemental shield breaks immediately.
 * Despawns after 17.5s (350 ticks) if not picked up.
 */
public final class CrystallizeShardManager {
    public static final String TAG_CRYSTALLIZE_SHARD = "velorise_staves:crystallize_shard";
    public static final int SHARD_LIFETIME_TICKS = 350; // 17.5 seconds
    public static final int SHIELD_DURATION_TICKS = 300; // 15 seconds

    public static class ShardEntry {
        public final ItemEntity entity;
        public final ServerLevel level;
        public final Element element;
        public final long spawnTime;
        public final @Nullable UUID sourceMobId;
        public final int emLevel;

        public ShardEntry(ItemEntity entity, ServerLevel level, Element element, long spawnTime, @Nullable UUID sourceMobId) {
            this(entity, level, element, spawnTime, sourceMobId, 0);
        }

        public ShardEntry(ItemEntity entity, ServerLevel level, Element element, long spawnTime, @Nullable UUID sourceMobId, int emLevel) {
            this.entity = entity;
            this.level = level;
            this.element = element;
            this.spawnTime = spawnTime;
            this.sourceMobId = sourceMobId;
            this.emLevel = Math.max(0, emLevel);
        }
    }

    public static class ShieldEntry {
        public final Element element;
        public final long expireTick;

        public ShieldEntry(Element element, long expireTick) {
            this.element = element;
            this.expireTick = expireTick;
        }
    }

    private static final List<ShardEntry> SHARDS = new CopyOnWriteArrayList<>();
    private static final Map<UUID, ShieldEntry> ACTIVE_SHIELDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MOB_SHARD_TICK = new ConcurrentHashMap<>();

    private CrystallizeShardManager() {}

    public static @Nullable Item getShardItem(Element element) {
        return switch (element) {
            case PYRO -> VeloriseStavesMod.PYRO_CRYSTALLIZE_SHARD.get();
            case HYDRO -> VeloriseStavesMod.HYDRO_CRYSTALLIZE_SHARD.get();
            case ELECTRO -> VeloriseStavesMod.ELECTRO_CRYSTALLIZE_SHARD.get();
            case CRYO -> VeloriseStavesMod.CRYO_CRYSTALLIZE_SHARD.get();
            default -> null;
        };
    }

    private static ChatFormatting getTeamColor(Element element) {
        return switch (element) {
            case PYRO -> ChatFormatting.RED;
            case HYDRO -> ChatFormatting.AQUA;
            case ELECTRO -> ChatFormatting.LIGHT_PURPLE;
            case CRYO -> ChatFormatting.WHITE;
            case DENDRO -> ChatFormatting.GREEN;
            case ANEMO -> ChatFormatting.DARK_AQUA;
            case GEO -> ChatFormatting.GOLD;
        };
    }

    public static void spawn(ServerLevel level, LivingEntity target, Element reactedElement) {
        spawn(level, target, reactedElement, target.getUUID());
    }

    public static void spawn(ServerLevel level, LivingEntity target, Element reactedElement, @Nullable UUID sourceMobId) {
        spawn(level, target, reactedElement, sourceMobId, 0);
    }

    public static void spawn(ServerLevel level, LivingEntity target, Element reactedElement, @Nullable UUID sourceMobId, int emLevel) {
        double spawnY = target.getY() + target.getBbHeight() * 0.80D;
        Vec3 spawnPos = new Vec3(target.getX(), spawnY, target.getZ());
        spawnAt(level, spawnPos, reactedElement, sourceMobId, emLevel);
    }

    public static void spawn(ServerLevel level, Vec3 pos, Element reactedElement) {
        spawnAt(level, pos, reactedElement, null, 0);
    }

    public static void spawn(ServerLevel level, Vec3 pos, Element reactedElement, @Nullable UUID sourceMobId) {
        spawnAt(level, pos, reactedElement, sourceMobId, 0);
    }

    private static void spawnAt(ServerLevel level, Vec3 pos, Element reactedElement, @Nullable UUID sourceMobId, int emLevel) {
        if (sourceMobId != null) {
            long now = level.getGameTime();
            Long lastSpawn = LAST_MOB_SHARD_TICK.get(sourceMobId);
            // Limit: each mob can only generate 1 shard at a time, with a 3s (60 ticks) ICD
            if (lastSpawn != null && now - lastSpawn < 60L) {
                return;
            }
            for (ShardEntry s : SHARDS) {
                if (sourceMobId.equals(s.sourceMobId) && s.entity.isAlive() && !s.entity.isRemoved()) {
                    return;
                }
            }
            LAST_MOB_SHARD_TICK.put(sourceMobId, now);
        }

        Item shardItem = getShardItem(reactedElement);
        if (shardItem == null) return;

        ItemStack stack = new ItemStack(shardItem);
        ItemEntity item = new ItemEntity(level, pos.x, pos.y, pos.z, stack);
        item.setPickUpDelay(32767); // Cannot be picked up normally into inventory

        // Pop upward from 80% body height and arc gently outward before falling
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double horizSpeed = 0.08D + level.random.nextDouble() * 0.08D;
        double vx = Math.cos(angle) * horizSpeed;
        double vz = Math.sin(angle) * horizSpeed;
        double vy = 0.32D + level.random.nextDouble() * 0.08D;
        item.setDeltaMovement(vx, vy, vz);
        item.hasImpulse = true;
        item.addTag(TAG_CRYSTALLIZE_SHARD);
        item.setGlowingTag(false);

        level.addFreshEntity(item);
        SHARDS.add(new ShardEntry(item, level, reactedElement, level.getGameTime(), sourceMobId, emLevel));

        // Elemental crystal birth sound (clean amethyst chime)
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.4F);
    }

    public static void clearPlayerShield(ServerPlayer player) {
        if (player == null) return;
        ACTIVE_SHIELDS.remove(player.getUUID());
        ModMobEffects.removeAllShields(player);
        ModNetwork.sendCrystallizeShieldToPlayer(player, null, 0);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 100 == 0) {
            LAST_MOB_SHARD_TICK.entrySet().removeIf(e -> (now - e.getValue()) >= 100L);
            ACTIVE_SHIELDS.entrySet().removeIf(e -> now >= e.getValue().expireTick);
        }

        // Break Crystallize shield if expired or depleted (creative players keep shield for duration)
        for (ServerPlayer player : level.players()) {
            UUID pid = player.getUUID();
            ShieldEntry entry = ACTIVE_SHIELDS.get(pid);
            if (entry != null) {
                Element activeShield = ModMobEffects.getActiveShield(player);
                boolean depleted = !player.isCreative() && player.getAbsorptionAmount() <= 0.0F;
                boolean expired = now >= entry.expireTick;
                boolean changed = (activeShield != null && activeShield != entry.element);
                boolean cleared = (activeShield == null);

                if (changed && !depleted) {
                    // Shield was replaced with a different element (e.g. via command or new shard): sync to new element
                    ACTIVE_SHIELDS.put(pid, new ShieldEntry(activeShield, now + SHIELD_DURATION_TICKS));
                    ModNetwork.sendCrystallizeShieldToPlayer(player, activeShield, SHIELD_DURATION_TICKS);
                } else if (expired || cleared || depleted) {
                    ACTIVE_SHIELDS.remove(pid);
                    ModMobEffects.removeAllShields(player);
                    MobEffectInstance absInst = player.getEffect(MobEffects.ABSORPTION);
                    if (absInst != null && absInst.getAmplifier() == 0) {
                        player.removeEffect(MobEffects.ABSORPTION);
                    }
                    if (expired && player.getAbsorptionAmount() <= 5.0F) {
                        player.setAbsorptionAmount(0.0F);
                    }
                    ModNetwork.sendCrystallizeShieldToPlayer(player, null, 0);
                    if (depleted) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 1.6F);
                    }
                } else if (now % 8 == 0) {
                    spawnShieldOrbitParticles(level, player, entry.element);
                }
            }
        }

        Iterator<ShardEntry> it = SHARDS.iterator();

        while (it.hasNext()) {
            ShardEntry shard = it.next();
            if (shard.level != level) continue;

            if (!shard.entity.isAlive() || shard.entity.isRemoved()) {
                cleanUpShardTeam(shard);
                SHARDS.remove(shard);
                continue;
            }

            // Despawn after 17.5s (350 ticks)
            if (now - shard.spawnTime >= SHARD_LIFETIME_TICKS) {
                cleanUpShardTeam(shard);
                shard.entity.discard();
                SHARDS.remove(shard);
                continue;
            }

            // Player pickup detection (within 1.5m radius)
            AABB pickupBox = shard.entity.getBoundingBox().inflate(1.25D, 0.6D, 1.25D);
            List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, pickupBox, pl -> pl.isAlive() && !pl.isSpectator());
            if (!nearbyPlayers.isEmpty()) {
                ServerPlayer player = nearbyPlayers.get(0);
                pickupShard(level, player, shard.element, shard.emLevel);
                cleanUpShardTeam(shard);
                shard.entity.discard();
                SHARDS.remove(shard);
            }
        }

        // Auto-Purge Orphaned Crystallize Shards
        if (now % 20 == 0) {
            for (ServerPlayer player : level.players()) {
                AABB scanBox = player.getBoundingBox().inflate(64.0D);
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, scanBox, e -> e.isAlive() && !e.isRemoved())) {
                    if (item.getTags().contains(TAG_CRYSTALLIZE_SHARD)) {
                        boolean tracked = false;
                        for (ShardEntry s : SHARDS) {
                            if (s.entity == item || s.entity.getUUID().equals(item.getUUID())) {
                                tracked = true;
                                break;
                            }
                        }
                        if (!tracked && item.getAge() >= SHARD_LIFETIME_TICKS) {
                            try {
                                Scoreboard sb = level.getScoreboard();
                                PlayerTeam team = sb.getPlayersTeam(item.getScoreboardName());
                                if (team != null && team.getName().startsWith("vs_")) {
                                    sb.removePlayerFromTeam(item.getScoreboardName(), team);
                                }
                            } catch (Throwable ignored) {}
                            item.discard();
                        }
                    }
                }
            }
        }
    }

    private static void cleanUpShardTeam(ShardEntry shard) {
        try {
            Scoreboard sb = shard.level.getScoreboard();
            String teamName = "vs_" + shard.element.name().toLowerCase(java.util.Locale.ROOT);
            PlayerTeam team = sb.getPlayerTeam(teamName);
            if (team != null) {
                sb.removePlayerFromTeam(shard.entity.getScoreboardName(), team);
            }
        } catch (Throwable ignored) {}
    }

    private static void spawnShieldOrbitParticles(ServerLevel level, ServerPlayer player, Element element) {
        double angle = (level.getGameTime() * 0.22D) % (Math.PI * 2.0D);
        double r = 0.8D;
        double px1 = player.getX() + Math.cos(angle) * r;
        double py1 = player.getY() + 0.9D + Math.sin(angle * 2.0D) * 0.15D;
        double pz1 = player.getZ() + Math.sin(angle) * r;

        double angle2 = angle + Math.PI;
        double px2 = player.getX() + Math.cos(angle2) * r;
        double py2 = player.getY() + 0.9D + Math.sin(angle2 * 2.0D) * 0.15D;
        double pz2 = player.getZ() + Math.sin(angle2) * r;

        switch (element) {
            case PYRO -> {
                level.sendParticles(ParticleTypes.FLAME, px1, py1, pz1, 1, 0, 0, 0, 0.005D);
                level.sendParticles(ParticleTypes.FLAME, px2, py2, pz2, 1, 0, 0, 0, 0.005D);
            }
            case HYDRO -> {
                level.sendParticles(ParticleTypes.FALLING_WATER, px1, py1, pz1, 1, 0, 0, 0, 0.005D);
                level.sendParticles(ParticleTypes.FALLING_WATER, px2, py2, pz2, 1, 0, 0, 0, 0.005D);
            }
            case ELECTRO -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px1, py1, pz1, 1, 0, 0, 0, 0.005D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px2, py2, pz2, 1, 0, 0, 0, 0.005D);
            }
            case CRYO -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, px1, py1, pz1, 1, 0, 0, 0, 0.005D);
                level.sendParticles(ParticleTypes.SNOWFLAKE, px2, py2, pz2, 1, 0, 0, 0, 0.005D);
            }
            default -> {}
        }
    }

    public static void onShieldApplied(ServerPlayer player, Element element, int durationTicks) {
        long now = player.serverLevel().getGameTime();
        int safeDuration = durationTicks > 0 ? durationTicks : SHIELD_DURATION_TICKS;
        ACTIVE_SHIELDS.put(player.getUUID(), new ShieldEntry(element, now + safeDuration));
        ModNetwork.sendCrystallizeShieldToPlayer(player, element, safeDuration);
    }

    private static void pickupShard(ServerLevel level, ServerPlayer player, Element element, int emLevel) {
        // Replace any existing Crystallize shield (same or different element) and reset shield absorption
        ModMobEffects.removeAllShields(player);
        MobEffectInstance oldAbs = player.getEffect(MobEffects.ABSORPTION);
        if (oldAbs != null && oldAbs.getAmplifier() <= 1) {
            player.removeEffect(MobEffects.ABSORPTION);
            if (player.getAbsorptionAmount() <= 5.0F) {
                player.setAbsorptionAmount(0.0F);
            }
        }
        // Elemental Mastery I-III grants +1.0 HP (+0.5 golden heart) shield absorption per level: 2.0 / 3.0 / 4.0 / 5.0 HP
        float shieldHp = 2.0F + 1.0F * Math.max(0, emLevel);
        int shieldAmp = Math.max(0, emLevel);
        var shieldObj = ModMobEffects.getShieldForElement(element);
        if (shieldObj != null) {
            player.addEffect(new MobEffectInstance(shieldObj, SHIELD_DURATION_TICKS, shieldAmp, false, false, false)); // showParticles=false, showIcon=false: suppress vanilla HUD
        }

        // Grant fresh 15s vanilla Absorption scaled by Elemental Mastery
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHIELD_DURATION_TICKS, shieldAmp > 0 ? 1 : 0, false, false, false));
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), shieldHp));

        onShieldApplied(player, element, SHIELD_DURATION_TICKS);

        // Pickup chime sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.55F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
    }

    public static @Nullable Element getActiveShield(LivingEntity entity) {
        Element shield = ModMobEffects.getActiveShield(entity);
        if (shield != null) return shield;
        if (entity instanceof ServerPlayer sp) {
            return getActiveShield(sp.getUUID(), entity.level().getGameTime());
        }
        return null;
    }

    public static @Nullable Element getActiveShield(ServerPlayer player, long now) {
        Element shield = ModMobEffects.getActiveShield(player);
        if (shield != null) return shield;
        if (!player.isCreative() && player.getAbsorptionAmount() <= 0.0F) {
            ACTIVE_SHIELDS.remove(player.getUUID());
            return null;
        }
        return getActiveShield(player.getUUID(), now);
    }

    public static boolean matchesElementalShield(@Nullable Element activeShield, DamageSource src) {
        if (activeShield == null) return false;
        Element attackerInnate = src.getEntity() instanceof LivingEntity attackerMob
                ? ElementalGaugeManager.getInnateElement(attackerMob) : null;
        if (attackerInnate == activeShield) return true;

        if (activeShield == Element.PYRO) {
            return src.is(DamageTypeTags.IS_FIRE)
                    || src.is(DamageTypes.FIREWORKS);
        }
        if (activeShield == Element.ELECTRO) {
            return src.is(DamageTypeTags.IS_LIGHTNING)
                    || src.is(DamageTypes.DRAGON_BREATH);
        }
        if (activeShield == Element.CRYO) {
            return src.is(DamageTypeTags.IS_FREEZING);
        }
        if (activeShield == Element.HYDRO) {
            return src.is(DamageTypeTags.IS_DROWNING)
                    || src.is(DamageTypes.TRIDENT)
                    || src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident;
        }
        if (activeShield == Element.DENDRO) {
            return src.is(DamageTypes.CACTUS)
                    || src.is(DamageTypes.SWEET_BERRY_BUSH);
        }
        if (activeShield == Element.GEO) {
            return src.is(DamageTypes.FALLING_STALACTITE)
                    || src.is(DamageTypes.STALAGMITE)
                    || src.is(DamageTypes.FALLING_ANVIL)
                    || src.is(DamageTypes.FALLING_BLOCK);
        }
        return false;
    }

    public static @Nullable Element getActiveShield(UUID playerId, long now) {
        ShieldEntry entry = ACTIVE_SHIELDS.get(playerId);
        if (entry != null && now < entry.expireTick) {
            return entry.element;
        }
        if (entry != null) {
            ACTIVE_SHIELDS.remove(playerId);
        }
        return null;
    }

    public static void clearAll() {
        for (ShardEntry shard : SHARDS) {
            cleanUpShardTeam(shard);
            if (shard.entity.isAlive()) {
                shard.entity.discard();
            }
        }
        SHARDS.clear();
        ACTIVE_SHIELDS.clear();
        LAST_MOB_SHARD_TICK.clear();
    }
}
