package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnvironmentalBlockManager {

    private static final Map<UUID, Long> LAST_AMETHYST_ZAP = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> LAST_SENSOR_ZAP = new ConcurrentHashMap<>();

    private EnvironmentalBlockManager() {}

    public static void clearAll() {
        LAST_AMETHYST_ZAP.clear();
        LAST_SENSOR_ZAP.clear();
    }

    /**
     * Genshin Impact Electro Crystal mining mechanic:
     * When a Pyro attack or Overloaded explosion hits near an Amethyst Cluster,
     * it triggers an Overloaded detonation on the cluster and drops Amethyst Shards.
     */
    public static void checkPyroAmethystMining(ServerLevel level, Vec3 hitPos, double radius) {
        BlockPos center = BlockPos.containing(hitPos);
        int r = Math.max(1, (int) Math.ceil(radius));
        double r2 = (radius + 0.75D) * (radius + 0.75D);

        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (!level.hasChunkAt(p)) continue;
            Vec3 blockCenter = Vec3.atCenterOf(p);
            if (blockCenter.distanceToSqr(hitPos) > r2) continue;

            BlockState st = level.getBlockState(p);
            if (isAmethystClusterOrBud(st)) {
                boolean isFullCluster = st.is(Blocks.AMETHYST_CLUSTER);
                level.removeBlock(p, false);

                // Overloaded detonation visuals & sound
                level.sendParticles(ParticleTypes.EXPLOSION, blockCenter.x, blockCenter.y, blockCenter.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, blockCenter.x, blockCenter.y, blockCenter.z, 16, 0.35D, 0.35D, 0.35D, 0.06D);
                level.sendParticles(ParticleTypes.FLAME, blockCenter.x, blockCenter.y, blockCenter.z, 12, 0.3D, 0.3D, 0.3D, 0.04D);
                level.playSound(null, p, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.75F, 1.35F);
                level.playSound(null, p, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 1.1F);

                if (isFullCluster) {
                    int dropCount = 2 + level.random.nextInt(3); // 2 to 4 amethyst shards
                    ItemStack dropStack = new ItemStack(Items.AMETHYST_SHARD, dropCount);
                    ItemEntity item = new ItemEntity(level, blockCenter.x, blockCenter.y, blockCenter.z, dropStack);
                    item.setDeltaMovement(
                            (level.random.nextDouble() - 0.5D) * 0.18D,
                            0.25D,
                            (level.random.nextDouble() - 0.5D) * 0.18D
                    );
                    level.addFreshEntity(item);
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        long serverNow = server.overworld().getGameTime();
        if (serverNow % 100 == 0) {
            LAST_AMETHYST_ZAP.entrySet().removeIf(e -> (serverNow - e.getValue()) > 100L);
            LAST_SENSOR_ZAP.entrySet().removeIf(e -> (serverNow - e.getValue()) > 100L);
        }

        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) continue;

            Set<Long> visitedChunks = new HashSet<>();
            List<BlockPos> activeSensors8 = new ArrayList<>();
            List<BlockPos> activeCalibrated16 = new ArrayList<>();

            for (ServerPlayer player : players) {
                ChunkPos cp = player.chunkPosition();
                for (int cx = cp.x - 3; cx <= cp.x + 3; cx++) {
                    for (int cz = cp.z - 3; cz <= cp.z + 3; cz++) {
                        long chunkKey = ChunkPos.asLong(cx, cz);
                        if (!visitedChunks.add(chunkKey)) continue;
                        if (!level.hasChunk(cx, cz)) continue;
                        LevelChunk chunk = level.getChunk(cx, cz);
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            if (be instanceof CalibratedSculkSensorBlockEntity) {
                                BlockState st = be.getBlockState();
                                if (SculkSensorBlock.getPhase(st) == SculkSensorPhase.ACTIVE) {
                                    activeCalibrated16.add(be.getBlockPos());
                                }
                            } else if (be instanceof SculkSensorBlockEntity) {
                                BlockState st = be.getBlockState();
                                if (SculkSensorBlock.getPhase(st) == SculkSensorPhase.ACTIVE) {
                                    activeSensors8.add(be.getBlockPos());
                                }
                            }
                        }
                    }
                }
            }

            // Sculk Sensor (8m) & Calibrated Sculk Sensor (16m):
            // Only zaps the specific entity that triggered the sensor signal (closest entity within range)
            for (BlockPos sensorPos : activeSensors8) {
                Long lastZap = LAST_SENSOR_ZAP.get(sensorPos);
                if (lastZap == null || serverNow - lastZap >= 20L) {
                    AABB search = new AABB(sensorPos).inflate(8.0D);
                    LivingEntity closest = level.getNearestEntity(
                            LivingEntity.class,
                            net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().range(8.0D).ignoreLineOfSight(),
                            null,
                            sensorPos.getX() + 0.5D, sensorPos.getY() + 0.5D, sensorPos.getZ() + 0.5D,
                            search
                    );
                    if (closest != null && !closest.isSpectator() && !ElementalGaugeManager.isPassiveAquaticOrAmbient(closest)) {
                        LAST_SENSOR_ZAP.put(sensorPos.immutable(), serverNow);
                        ElementalGaugeManager.applyElement(level, closest, Element.ELECTRO, 1.0F, null, 0.0F);
                        Vec3 start = Vec3.atCenterOf(sensorPos);
                        Vec3 end = closest.position().add(0, closest.getBbHeight() * 0.5D, 0);
                        ElectrifiedWaterManager.drawElectricArc(level, start, end, 0.15D, false);
                        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, end.x, end.y, end.z, 3, 0.2D, 0.2D, 0.2D, 0.02D);
                        level.playSound(null, sensorPos.getX(), sensorPos.getY(), sensorPos.getZ(),
                                SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.8F, 1.4F);
                    }
                }
            }

            for (BlockPos sensorPos : activeCalibrated16) {
                Long lastZap = LAST_SENSOR_ZAP.get(sensorPos);
                if (lastZap == null || serverNow - lastZap >= 20L) {
                    AABB search = new AABB(sensorPos).inflate(16.0D);
                    LivingEntity closest = level.getNearestEntity(
                            LivingEntity.class,
                            net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().range(16.0D).ignoreLineOfSight(),
                            null,
                            sensorPos.getX() + 0.5D, sensorPos.getY() + 0.5D, sensorPos.getZ() + 0.5D,
                            search
                    );
                    if (closest != null && !closest.isSpectator() && !ElementalGaugeManager.isPassiveAquaticOrAmbient(closest)) {
                        LAST_SENSOR_ZAP.put(sensorPos.immutable(), serverNow);
                        ElementalGaugeManager.applyElement(level, closest, Element.ELECTRO, 1.0F, null, 0.0F);
                        Vec3 start = Vec3.atCenterOf(sensorPos);
                        Vec3 end = closest.position().add(0, closest.getBbHeight() * 0.5D, 0);
                        ElectrifiedWaterManager.drawElectricArc(level, start, end, 0.15D, false);
                        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, end.x, end.y, end.z, 3, 0.2D, 0.2D, 0.2D, 0.02D);
                        level.playSound(null, sensorPos.getX(), sensorPos.getY(), sensorPos.getZ(),
                                SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.8F, 1.4F);
                    }
                }
            }

            Set<UUID> processed = new HashSet<>();
            for (ServerPlayer player : players) {
                AABB area = player.getBoundingBox().inflate(32.0D);
                List<LivingEntity> entities = level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        e -> e.isAlive() && !e.isRemoved() && !ElementalGaugeManager.isPassiveAquaticOrAmbient(e)
                );
                for (LivingEntity living : entities) {
                    if (processed.add(living.getUUID())) {
                        if ((living.tickCount + Math.abs(living.getId())) % 20 == 0) {
                            tickEntityEnvironmentalAuras(level, living);
                        }
                    }
                }
            }
        }
    }

    private static void tickEntityEnvironmentalAuras(
            ServerLevel level,
            LivingEntity living
    ) {
        BlockPos pos = living.blockPosition();
        if (!level.hasChunkAt(pos)) return;

        // 1. Physical contact check: Redstone Block, Slime/Honey Block, and solid Amethyst Block / Budding Amethyst
        AABB touchBox = living.getBoundingBox().inflate(0.10D, 0.10D, 0.10D);
        BlockPos min = BlockPos.containing(touchBox.minX, touchBox.minY - 0.15D, touchBox.minZ);
        BlockPos max = BlockPos.containing(touchBox.maxX, touchBox.maxY, touchBox.maxZ);
        boolean touchingRedstone = false;
        boolean touchingSlimeOrHoney = false;
        BlockPos amethystZapPos = null;
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(p)) continue;
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.REDSTONE_BLOCK)) {
                touchingRedstone = true;
            }
            if (st.is(Blocks.SLIME_BLOCK) || st.is(Blocks.HONEY_BLOCK)) {
                touchingSlimeOrHoney = true;
            }
            if (isSolidAmethystBlock(st) && amethystZapPos == null) {
                amethystZapPos = p.immutable();
            }
        }
        if (touchingRedstone) {
            ElementalGaugeManager.applyElement(level, living, Element.ELECTRO, 1.0F, null, 0.0F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + 0.3D, living.getZ(), 2, 0.15D, 0.15D, 0.15D, 0.02D);
        }
        if (touchingSlimeOrHoney) {
            ElementalGaugeManager.applyElement(level, living, Element.DENDRO, 1.0F, null, 0.0F);
        }

        // 2. Amethyst Cluster: Radiates Electro aura within 1.0 block every 1s
        if (amethystZapPos == null) {
            AABB clusterBox = living.getBoundingBox().inflate(1.0D);
            BlockPos minCluster = BlockPos.containing(clusterBox.minX, clusterBox.minY, clusterBox.minZ);
            BlockPos maxCluster = BlockPos.containing(clusterBox.maxX, clusterBox.maxY, clusterBox.maxZ);
            for (BlockPos p : BlockPos.betweenClosed(minCluster, maxCluster)) {
                if (!level.hasChunkAt(p)) continue;
                BlockState st = level.getBlockState(p);
                if (st.is(Blocks.AMETHYST_CLUSTER)) {
                    amethystZapPos = p.immutable();
                    break;
                }
            }
        }

        if (amethystZapPos != null && ElementalGaugeManager.getInnateElement(living) != Element.ELECTRO) {
            long now = level.getGameTime();
            Long lastZap = LAST_AMETHYST_ZAP.get(living.getUUID());
            if (lastZap == null || (now - lastZap) >= 20L) {
                LAST_AMETHYST_ZAP.put(living.getUUID(), now);
                ElementalGaugeManager.applyElement(level, living, Element.ELECTRO, 1.0F, null, 1.0F);

                Vec3 end = living.position().add(0, living.getBbHeight() * 0.5D, 0);
                ElectrifiedWaterManager.drawElectricArc(level, Vec3.atCenterOf(amethystZapPos), end, 0.15D, false);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 4, 0.2D, 0.2D, 0.2D, 0.03D);
                level.playSound(null, amethystZapPos.getX(), amethystZapPos.getY(), amethystZapPos.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.6F);
                level.playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.4F, 1.9F);
            }
        }
    }

    public static boolean isSolidAmethystBlock(BlockState st) {
        return st.is(Blocks.AMETHYST_BLOCK) || st.is(Blocks.BUDDING_AMETHYST);
    }

    public static boolean isAmethystClusterOrBud(BlockState st) {
        return st.is(Blocks.AMETHYST_CLUSTER)
                || st.is(Blocks.LARGE_AMETHYST_BUD)
                || st.is(Blocks.MEDIUM_AMETHYST_BUD)
                || st.is(Blocks.SMALL_AMETHYST_BUD);
    }

    public static boolean isAmethystBlock(BlockState st) {
        return isSolidAmethystBlock(st) || isAmethystClusterOrBud(st);
    }
}
