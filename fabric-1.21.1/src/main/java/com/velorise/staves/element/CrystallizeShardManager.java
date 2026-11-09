package com.velorise.staves.element;

import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Genshin Impact Crystallize Elemental Shards (Mảnh Kết Tinh) in Minecraft.
 * When Geo hits Pyro/Hydro/Electro/Cryo, drops a 3D spinning elemental crystal shard.
 * Only players can pick it up; grants 1 golden heart Absorption (15s) with 250% matching elemental resistance.
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

        public ShardEntry(ItemEntity entity, ServerLevel level, Element element, long spawnTime) {
            this.entity = entity;
            this.level = level;
            this.element = element;
            this.spawnTime = spawnTime;
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

    public static void spawn(ServerLevel level, Vec3 pos, Element reactedElement) {
        Item shardItem = getShardItem(reactedElement);
        if (shardItem == null) return;

        ItemStack stack = new ItemStack(shardItem);
        ItemEntity item = new ItemEntity(level, pos.x, pos.y + 0.15D, pos.z, stack);
        item.setPickUpDelay(32767); // Cannot be picked up normally into inventory
        item.setUnlimitedLifetime();
        item.setDeltaMovement((level.random.nextDouble() - 0.5D) * 0.12D, 0.24D, (level.random.nextDouble() - 0.5D) * 0.12D);
        item.addTag(TAG_CRYSTALLIZE_SHARD);
        level.addFreshEntity(item);

        SHARDS.add(new ShardEntry(item, level, reactedElement, level.getGameTime()));

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
        Iterator<ShardEntry> it = SHARDS.iterator();

        while (it.hasNext()) {
            ShardEntry shard = it.next();
            if (shard.level != level) continue;

            if (!shard.entity.isAlive() || shard.entity.isRemoved()) {
                SHARDS.remove(shard);
                continue;
            }

            // Despawn after 17.5s (350 ticks)
            if (now - shard.spawnTime >= SHARD_LIFETIME_TICKS) {
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
                        level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    case HYDRO -> {
                        level.sendParticles(ParticleTypes.FALLING_WATER, p.x, p.y + 0.25D, p.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                        level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    case ELECTRO -> {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.25D, p.z, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                        level.sendParticles(ParticleTypes.GLOW, p.x, p.y + 0.25D, p.z, 1, 0.1D, 0.1D, 0.1D, 0.01D);
                    }
                    case CRYO -> {
                        level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.25D, p.z, 2, 0.1D, 0.1D, 0.1D, 0.01D);
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
                shard.entity.discard();
                SHARDS.remove(shard);
            }
        }
    }

    private static void pickupShard(ServerLevel level, ServerPlayer player, Element element) {
        long now = level.getGameTime();

        // Overwrite absorption: 1 golden heart (2 absorption health) for 15s (300 ticks)
        player.removeEffect(MobEffects.ABSORPTION);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHIELD_DURATION_TICKS, 0, false, true, true));

        // Register 250% elemental counter shield
        ACTIVE_SHIELDS.put(player.getUUID(), new ShieldEntry(element, now + SHIELD_DURATION_TICKS));

        // Pickup chime sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.55F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
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
            if (shard.entity.isAlive()) {
                shard.entity.discard();
            }
        }
        SHARDS.clear();
        ACTIVE_SHIELDS.clear();
    }
}
