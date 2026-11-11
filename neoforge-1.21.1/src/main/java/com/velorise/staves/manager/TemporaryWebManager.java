package com.velorise.staves.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Tracks temporary Webcaster cobwebs and restores previous blocks upon expiration or server shutdown. */
public final class TemporaryWebManager {
    private static final int VENOM_PULSE_INTERVAL_TICKS = 20;
    private static final int POISON_DURATION_TICKS = 60;
    private static final Map<ServerLevel, Map<BlockPos, Entry>> WEBS = new ConcurrentHashMap<>();

    private TemporaryWebManager() {}

    public static boolean place(ServerLevel level, BlockPos pos, int durationTicks, int poisonAmplifier, UUID ownerId) {
        BlockState current = level.getBlockState(pos);
        Map<BlockPos, Entry> entries = WEBS.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>());
        BlockPos key = pos.immutable();
        Entry existing = entries.get(key);
        long now = level.getGameTime();
        long expiresAt = now + Math.max(1, durationTicks);

        if (existing != null && current.is(Blocks.COBWEB)) {
            existing.expiresAt = Math.max(existing.expiresAt, expiresAt);
            existing.poisonAmplifier = Math.max(existing.poisonAmplifier, poisonAmplifier);
            if (ownerId != null) existing.ownerId = ownerId;
            return true;
        }

        if (!current.canBeReplaced() || !level.getFluidState(pos).isEmpty()) return false;
        entries.put(key, new Entry(current, expiresAt, poisonAmplifier, ownerId));
        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<ServerLevel, Map<BlockPos, Entry>>> levels = WEBS.entrySet().iterator();
        while (levels.hasNext()) {
            Map.Entry<ServerLevel, Map<BlockPos, Entry>> levelEntry = levels.next();
            ServerLevel level = levelEntry.getKey();
            if (level.getServer() != server) continue;

            long now = level.getGameTime();
            boolean venomPulse = now % VENOM_PULSE_INTERVAL_TICKS == 0L;
            Iterator<Map.Entry<BlockPos, Entry>> webs = levelEntry.getValue().entrySet().iterator();
            while (webs.hasNext()) {
                Map.Entry<BlockPos, Entry> web = webs.next();
                BlockPos pos = web.getKey();
                Entry entry = web.getValue();
                if (!level.hasChunkAt(pos)) {
                    if ((now - entry.expiresAt) > 6000L || (entry.expiresAt - now) > 12000L) {
                        webs.remove();
                    }
                    continue;
                }

                if (!level.getBlockState(pos).is(Blocks.COBWEB)) {
                    webs.remove();
                    continue;
                }

                if (now >= entry.expiresAt || (entry.expiresAt - now) > 12000L) {
                    level.setBlock(pos, entry.previous, 3);
                    webs.remove();
                    continue;
                }

                if (venomPulse) {
                    AABB webBox = new AABB(pos).inflate(0.05D);
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, webBox, LivingEntity::isAlive)) {
                        if (entry.ownerId != null && entry.ownerId.equals(target.getUUID())) continue;
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, entry.poisonAmplifier, true, true, true));
                    }
                }
            }

            if (levelEntry.getValue().isEmpty()) levels.remove();
        }
    }

    public static boolean isTemporaryWeb(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Entry> entries = WEBS.get(level);
        return entries != null && entries.containsKey(pos);
    }

    public static void clearAll(MinecraftServer server) {
        Iterator<Map.Entry<ServerLevel, Map<BlockPos, Entry>>> levels = WEBS.entrySet().iterator();
        while (levels.hasNext()) {
            Map.Entry<ServerLevel, Map<BlockPos, Entry>> levelEntry = levels.next();
            ServerLevel level = levelEntry.getKey();
            if (server != null && level.getServer() != server) continue;

            for (Map.Entry<BlockPos, Entry> web : levelEntry.getValue().entrySet()) {
                BlockPos pos = web.getKey();
                Entry entry = web.getValue();
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.COBWEB)) {
                    level.setBlock(pos, entry.previous, 3);
                }
            }
            levels.remove();
        }
    }

    private static final class Entry {
        private final BlockState previous;
        private long expiresAt;
        private int poisonAmplifier;
        private UUID ownerId;

        private Entry(BlockState previous, long expiresAt, int poisonAmplifier, UUID ownerId) {
            this.previous = previous;
            this.expiresAt = expiresAt;
            this.poisonAmplifier = poisonAmplifier;
            this.ownerId = ownerId;
        }
    }
}
