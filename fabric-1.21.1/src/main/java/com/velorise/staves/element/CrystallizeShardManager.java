package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

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

        public ShardEntry(ItemEntity entity, ServerLevel level, Element element, long spawnTime, @Nullable UUID sourceMobId) {
            this.entity = entity;
            this.level = level;
            this.element = element;
            this.spawnTime = spawnTime;
            this.sourceMobId = sourceMobId;
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
            case PYRO -> VeloriseStavesMod.PYRO_CRYSTALLIZE_SHARD;
            case HYDRO -> VeloriseStavesMod.HYDRO_CRYSTALLIZE_SHARD;
            case ELECTRO -> VeloriseStavesMod.ELECTRO_CRYSTALLIZE_SHARD;
            case CRYO -> VeloriseStavesMod.CRYO_CRYSTALLIZE_SHARD;
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

    public static void spawn(ServerLevel level, Vec3 pos, Element reactedElement) {
        spawn(level, pos, reactedElement, null);
    }

    public static void spawn(ServerLevel level, Vec3 pos, Element reactedElement, @Nullable UUID sourceMobId) {
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
        ItemEntity item = new ItemEntity(level, pos.x, pos.y + 0.15D, pos.z, stack);
        item.setPickUpDelay(32767); // Cannot be picked up normally into inventory
        item.setDeltaMovement((level.random.nextDouble() - 0.5D) * 0.12D, 0.24D, (level.random.nextDouble() - 0.5D) * 0.12D);
        item.addTag(TAG_CRYSTALLIZE_SHARD);
        item.setGlowingTag(true);

        try {
            Scoreboard sb = level.getScoreboard();
            String teamName = "vs_" + reactedElement.name().toLowerCase(java.util.Locale.ROOT);
            PlayerTeam team = sb.getPlayerTeam(teamName);
            if (team == null) {
                team = sb.addPlayerTeam(teamName);
                team.setColor(getTeamColor(reactedElement));
            }
            sb.addPlayerToTeam(item.getScoreboardName(), team);
        } catch (Throwable ignored) {}

        level.addFreshEntity(item);
        SHARDS.add(new ShardEntry(item, level, reactedElement, level.getGameTime(), sourceMobId));

        // Elemental birth sound and burst particles
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.4F);
        spawnBurstParticles(level, pos, reactedElement);
    }

    private static void spawnBurstParticles(ServerLevel level, Vec3 pos, Element element) {
        switch (element) {
            case PYRO -> level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.4D, pos.z, 16, 0.3D, 0.3D, 0.3D, 0.04D);
            case HYDRO -> level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y + 0.4D, pos.z, 20, 0.3D, 0.3D, 0.3D, 0.05D);
            case ELECTRO -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.4D, pos.z, 16, 0.3D, 0.3D, 0.3D, 0.06D);
            case CRYO -> level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.4D, pos.z, 20, 0.3D, 0.3D, 0.3D, 0.03D);
            default -> {}
        }
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 100 == 0) {
            LAST_MOB_SHARD_TICK.entrySet().removeIf(e -> (now - e.getValue()) >= 100L);
            ACTIVE_SHIELDS.entrySet().removeIf(e -> now >= e.getValue().expireTick);
        }

        // Break Crystallize shield immediately if player's Absorption hearts are depleted
        for (ServerPlayer player : level.players()) {
            UUID pid = player.getUUID();
            ShieldEntry entry = ACTIVE_SHIELDS.get(pid);
            if (entry != null) {
                if (now >= entry.expireTick || player.getAbsorptionAmount() <= 0.0F || !player.hasEffect(MobEffects.ABSORPTION)) {
                    ACTIVE_SHIELDS.remove(pid);
                    ModNetwork.sendCrystallizeShieldToPlayer(player, null, 0);
                } else if (now % 4 == 0) {
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

            Vec3 p = shard.entity.position();

            // Ambient sparkle around the floating crystal
            if (now % 6 == 0) {
                switch (shard.element) {
                    case PYRO -> {
                        level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                        level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.3F, 0.1F), 0.75F), p.x, p.y + 0.25D, p.z, 2, 0.12D, 0.12D, 0.12D, 0.02D);
                        level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    case HYDRO -> {
                        level.sendParticles(ParticleTypes.FALLING_WATER, p.x, p.y + 0.25D, p.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                        level.sendParticles(new DustParticleOptions(new Vector3f(0.1F, 0.75F, 1.0F), 0.75F), p.x, p.y + 0.25D, p.z, 2, 0.12D, 0.12D, 0.12D, 0.02D);
                        level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    case ELECTRO -> {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.25D, p.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                        level.sendParticles(new DustParticleOptions(new Vector3f(0.8F, 0.3F, 1.0F), 0.75F), p.x, p.y + 0.25D, p.z, 2, 0.12D, 0.12D, 0.12D, 0.02D);
                        level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    case CRYO -> {
                        level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.25D, p.z, 2, 0.1D, 0.1D, 0.1D, 0.01D);
                        level.sendParticles(new DustParticleOptions(new Vector3f(0.4F, 0.9F, 1.0F), 0.75F), p.x, p.y + 0.25D, p.z, 2, 0.12D, 0.12D, 0.12D, 0.02D);
                        level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    default -> {}
                }
            }

            // Player pickup detection (within 1.5m radius)
            AABB pickupBox = shard.entity.getBoundingBox().inflate(1.25D, 0.6D, 1.25D);
            List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, pickupBox, pl -> pl.isAlive() && !pl.isSpectator());
            if (!nearbyPlayers.isEmpty()) {
                ServerPlayer player = nearbyPlayers.get(0);
                pickupShard(level, player, shard.element);
                cleanUpShardTeam(shard);
                shard.entity.discard();
                SHARDS.remove(shard);
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

    private static void pickupShard(ServerLevel level, ServerPlayer player, Element element) {
        long now = level.getGameTime();

        // Overwrite absorption: 1 golden heart (2 absorption health) for 15s (300 ticks)
        player.removeEffect(MobEffects.ABSORPTION);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHIELD_DURATION_TICKS, 0, false, true, true));

        // Register 250% elemental counter shield
        ACTIVE_SHIELDS.put(player.getUUID(), new ShieldEntry(element, now + SHIELD_DURATION_TICKS));
        ModNetwork.sendCrystallizeShieldToPlayer(player, element, SHIELD_DURATION_TICKS);

        // Pickup chime sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.55F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
    }

    public static @Nullable Element getActiveShield(ServerPlayer player, long now) {
        if (player.getAbsorptionAmount() <= 0.0F || !player.hasEffect(MobEffects.ABSORPTION)) {
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
