package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
    private static final Map<Long, Long> LAST_OBSERVER_ZAP = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> LAST_LIGHTNING_ROD_ZAP = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> LAST_TESLA_COIL_ZAP = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> TESLA_COIL_OVERCHARGE_UNTIL = new ConcurrentHashMap<>();

    private EnvironmentalBlockManager() {}

    public static void clearAll() {
        LAST_AMETHYST_ZAP.clear();
        LAST_SENSOR_ZAP.clear();
        LAST_OBSERVER_ZAP.clear();
        LAST_LIGHTNING_ROD_ZAP.clear();
        LAST_TESLA_COIL_ZAP.clear();
        TESLA_COIL_OVERCHARGE_UNTIL.clear();
    }

    /**
     * Genshin Impact Electro Crystal mining mechanic:
     * When a Pyro attack or Overloaded explosion hits near an Amethyst Cluster,
     * it triggers an Overloaded detonation on the cluster and drops Amethyst Shards.
     */
    /**
     * Applies Pyro environmental effects around a hit position:
     * 1. Melts ice (Frosted Ice, Ice, Packed Ice) and snow (Snow layer, Snow Block, Powder Snow, Powder Snow Cauldron).
     * 2. Ignites unlit Campfires, unlit Candles, and TNT blocks.
     * 3. Burns replaceable flammable vegetation/cobwebs and ignites flammable materials (wood, leaves, wool, hay, etc.).
     */
    public static void applyPyroEnvironmentEffects(ServerLevel level, Vec3 hitPos, double radius) {
        BlockPos center = BlockPos.containing(hitPos);
        int r = Math.max(1, (int) Math.ceil(radius));
        double r2 = (radius + 0.65D) * (radius + 0.65D);
        boolean isUltraWarm = level.dimensionType().ultraWarm();

        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 2, r))) {
            if (!level.hasChunkAt(p)) continue;
            Vec3 blockCenter = Vec3.atCenterOf(p);
            if (blockCenter.distanceToSqr(hitPos) > r2) continue;

            BlockState st = level.getBlockState(p);
            if (st.isAir()) continue;

            // A. Melt Ice & Snow
            if (st.is(Blocks.FROSTED_ICE) || st.is(Blocks.ICE)) {
                level.setBlockAndUpdate(p, isUltraWarm ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState());
                level.sendParticles(ParticleTypes.CLOUD, blockCenter.x, blockCenter.y, blockCenter.z, 5, 0.2D, 0.2D, 0.2D, 0.02D);
                level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.4F);
                continue;
            }
            if (st.is(Blocks.PACKED_ICE) && radius >= 2.0D) {
                level.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
                level.sendParticles(ParticleTypes.CLOUD, blockCenter.x, blockCenter.y, blockCenter.z, 4, 0.2D, 0.2D, 0.2D, 0.02D);
                continue;
            }
            if (st.is(Blocks.SNOW) || st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.POWDER_SNOW)) {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                level.sendParticles(ParticleTypes.CLOUD, blockCenter.x, blockCenter.y, blockCenter.z, 4, 0.2D, 0.2D, 0.2D, 0.02D);
                level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.5F);
                continue;
            }
            if (st.is(Blocks.POWDER_SNOW_CAULDRON)) {
                level.setBlockAndUpdate(p, Blocks.WATER_CAULDRON.defaultBlockState());
                level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.5F);
                continue;
            }

            // B. Light unlit Campfires & Candles, or detonate TNT
            if (st.getBlock() instanceof CampfireBlock && CampfireBlock.canLight(st)) {
                level.setBlockAndUpdate(p, st.setValue(CampfireBlock.LIT, true));
                level.playSound(null, p, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
                continue;
            }
            if (st.getBlock() instanceof net.minecraft.world.level.block.CandleBlock && net.minecraft.world.level.block.CandleBlock.canLight(st)) {
                level.setBlockAndUpdate(p, st.setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
                level.playSound(null, p, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
                continue;
            }
            if (st.is(Blocks.TNT)) {
                level.removeBlock(p, false);
                net.minecraft.world.entity.item.PrimedTnt tnt = new net.minecraft.world.entity.item.PrimedTnt(
                        level, blockCenter.x, p.getY(), blockCenter.z, null
                );
                level.addFreshEntity(tnt);
                level.playSound(null, p, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                continue;
            }
        }
    }

    /**
     * Ignites at most the single block (or struck face) targeted directly by a Pyro projectile,
     * without spreading fire to surrounding blocks.
     */
    public static void igniteSingleHitBlock(ServerLevel level, BlockPos targetPos, Direction hitFace) {
        if (targetPos == null || !level.hasChunkAt(targetPos)) return;
        BlockState st = level.getBlockState(targetPos);
        if (isBurnableVegetationOrCobweb(st)) {
            BlockPos below = targetPos.below();
            if (level.getBlockState(below).isSolidRender(level, below)) {
                level.setBlockAndUpdate(targetPos, Blocks.FIRE.defaultBlockState());
            } else {
                level.removeBlock(targetPos, false);
            }
            Vec3 bc = Vec3.atCenterOf(targetPos);
            level.sendParticles(ParticleTypes.FLAME, bc.x, bc.y, bc.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
            return;
        }
        if (st.isAir() || st.canBeReplaced()) {
            if (net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, targetPos, hitFace)) {
                level.setBlockAndUpdate(targetPos, Blocks.FIRE.defaultBlockState());
            }
            return;
        }
        BlockPos firePos = targetPos.relative(hitFace);
        if (level.hasChunkAt(firePos)
                && level.getBlockState(firePos).isAir()
                && net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, firePos, hitFace)) {
            level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
        }
    }

    private static boolean isBurnableVegetationOrCobweb(BlockState st) {
        return st.is(Blocks.COBWEB)
                || st.is(Blocks.DEAD_BUSH)
                || st.is(Blocks.VINE)
                || st.is(Blocks.TALL_GRASS)
                || st.is(Blocks.FERN)
                || st.is(Blocks.LARGE_FERN)
                || st.is(Blocks.HANGING_ROOTS)
                || st.is(Blocks.GLOW_LICHEN)
                || st.is(net.minecraft.tags.BlockTags.FLOWERS)
                || st.is(net.minecraft.tags.BlockTags.SAPLINGS);
    }

    public static void checkPyroAmethystMining(ServerLevel level, Vec3 hitPos, double radius) {
        applyPyroEnvironmentEffects(level, hitPos, radius);
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

    /**
     * Discharges 1.0U Electro around a Lightning Rod when it is struck by lightning.
     */
    public static void dischargeLightningRod(ServerLevel level, BlockPos rodPos, long now) {
        BlockPos immutableRod = rodPos.immutable();
        Long lastZap = LAST_LIGHTNING_ROD_ZAP.get(immutableRod);
        if (lastZap != null && now >= lastZap && (now - lastZap) < 10L) {
            return;
        }
        LAST_LIGHTNING_ROD_ZAP.put(immutableRod, now);

        double radius = 4.5D;
        Vec3 rodCenter = Vec3.atCenterOf(immutableRod);

        // Visual electric arcs radiating from the Lightning Rod
        for (int i = 0; i < 6; i++) {
            double angle = (i * Math.PI / 3.0D) + (level.random.nextDouble() - 0.5D) * 0.3D;
            double dist = 1.5D + level.random.nextDouble() * 2.5D;
            Vec3 endPt = rodCenter.add(Math.cos(angle) * dist, (level.random.nextDouble() - 0.5D) * 0.8D, Math.sin(angle) * dist);
            ElectrifiedWaterManager.drawElectricArc(level, rodCenter, endPt, 0.25D, true);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, rodCenter.x, rodCenter.y + 0.3D, rodCenter.z, 25, 0.8D, 0.5D, 0.8D, 0.08D);
        level.playSound(null, rodCenter.x, rodCenter.y, rodCenter.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.9F, 1.65F);

        // Apply 1.0U Electro to all nearby LivingEntities
        AABB box = new AABB(immutableRod).inflate(radius);
        double r2 = radius * radius;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isRemoved() && !e.isSpectator())) {
            if (target.position().distanceToSqr(rodCenter) <= r2) {
                Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5D, 0);
                ElectrifiedWaterManager.drawElectricArc(level, rodCenter, targetCenter, 0.2D, false);
                ElementalGaugeManager.applyElementBypassIcd(level, target, Element.ELECTRO, 1.0F, null, 1.0F);
            }
        }

        // Electrify nearby water & trigger Hyperbloom on nearby Dendro Cores
        ElectrifiedWaterManager.electrify(level, immutableRod, radius, 70, null, 1.0F);
        DendroCoreManager.checkReactionsNear(level, rodCenter, radius, Element.ELECTRO, null);
    }

    public static void tick(MinecraftServer server) {
        long serverNow = server.overworld().getGameTime();
        if (serverNow % 100 == 0) {
            LAST_AMETHYST_ZAP.entrySet().removeIf(e -> serverNow < e.getValue() || (serverNow - e.getValue()) > 100L);
            LAST_OBSERVER_ZAP.entrySet().removeIf(e -> serverNow < e.getValue() || (serverNow - e.getValue()) > 40L);
            LAST_LIGHTNING_ROD_ZAP.entrySet().removeIf(e -> serverNow < e.getValue() || (serverNow - e.getValue()) > 40L);
            LAST_TESLA_COIL_ZAP.entrySet().removeIf(e -> serverNow < e.getValue() || (serverNow - e.getValue()) > 100L);
            TESLA_COIL_OVERCHARGE_UNTIL.entrySet().removeIf(e -> serverNow >= e.getValue() || (e.getValue() - serverNow) > 1200L);
        }

        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) continue;

            Set<Long> visitedChunks = new HashSet<>();
            List<BlockPos> activeSensors8 = new ArrayList<>();
            List<BlockPos> activeCalibrated16 = new ArrayList<>();
            Set<BlockPos> currentActiveSensors = new HashSet<>();

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
                                    BlockPos pos = be.getBlockPos().immutable();
                                    activeCalibrated16.add(pos);
                                    currentActiveSensors.add(pos);
                                }
                            } else if (be instanceof SculkSensorBlockEntity) {
                                BlockState st = be.getBlockState();
                                if (SculkSensorBlock.getPhase(st) == SculkSensorPhase.ACTIVE) {
                                    BlockPos pos = be.getBlockPos().immutable();
                                    activeSensors8.add(pos);
                                    currentActiveSensors.add(pos);
                                }
                            }
                        }
                    }
                }
            }

            // Immediately clear sensors that left SculkSensorPhase.ACTIVE so their next activation zaps on frame 0
            LAST_SENSOR_ZAP.keySet().retainAll(currentActiveSensors);

            // Sculk Sensor (8m -> 1.0U Electro) & Calibrated Sculk Sensor (16m -> 2.0U Electro):
            // Synchronized with vanilla SculkSensorPhase.ACTIVE animation (zaps immediately on activation and every 10 ticks while active)
            // Also supports zapping underwater fish (WaterAnimal), excluding only bats (AmbientCreature).
            for (BlockPos sensorPos : activeSensors8) {
                Long lastZap = LAST_SENSOR_ZAP.get(sensorPos);
                if (lastZap == null || serverNow < lastZap || serverNow - lastZap >= 10L) {
                    AABB search = new AABB(sensorPos).inflate(8.0D);
                    LivingEntity closest = level.getNearestEntity(
                            LivingEntity.class,
                            net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().range(8.0D).ignoreLineOfSight(),
                            null,
                            sensorPos.getX() + 0.5D, sensorPos.getY() + 0.5D, sensorPos.getZ() + 0.5D,
                            search
                    );
                    if (closest != null && !closest.isSpectator() && !(closest instanceof AmbientCreature)) {
                        LAST_SENSOR_ZAP.put(sensorPos, serverNow);
                        ElementalGaugeManager.applyElementBypassIcd(level, closest, Element.ELECTRO, 1.0F, null, 0.0F);
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
                if (lastZap == null || serverNow < lastZap || serverNow - lastZap >= 10L) {
                    AABB search = new AABB(sensorPos).inflate(16.0D);
                    LivingEntity closest = level.getNearestEntity(
                            LivingEntity.class,
                            net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().range(16.0D).ignoreLineOfSight(),
                            null,
                            sensorPos.getX() + 0.5D, sensorPos.getY() + 0.5D, sensorPos.getZ() + 0.5D,
                            search
                    );
                    if (closest != null && !closest.isSpectator() && !(closest instanceof AmbientCreature)) {
                        LAST_SENSOR_ZAP.put(sensorPos, serverNow);
                        // Calibrated Sculk Sensor applies strong 2.0U Electro
                        ElementalGaugeManager.applyElementBypassIcd(level, closest, Element.ELECTRO, 2.0F, null, 0.0F);
                        Vec3 start = Vec3.atCenterOf(sensorPos);
                        Vec3 end = closest.position().add(0, closest.getBbHeight() * 0.5D, 0);
                        ElectrifiedWaterManager.drawElectricArc(level, start, end, 0.15D, false);
                        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, end.x, end.y, end.z, 3, 0.2D, 0.2D, 0.2D, 0.02D);
                        level.playSound(null, sensorPos.getX(), sensorPos.getY(), sensorPos.getZ(),
                                SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.8F, 1.4F);
                    }
                }
            }

            // Check active LightningBolts striking Lightning Rods
            for (ServerPlayer player : players) {
                AABB boltScan = player.getBoundingBox().inflate(64.0D);
                for (LightningBolt bolt : level.getEntitiesOfClass(LightningBolt.class, boltScan, e -> e.isAlive() && !e.isRemoved())) {
                    BlockPos bp = bolt.blockPosition();
                    for (BlockPos check : new BlockPos[]{bp, bp.below(), bp.above(), bp.north(), bp.south(), bp.east(), bp.west()}) {
                        if (level.hasChunkAt(check) && level.getBlockState(check).is(Blocks.LIGHTNING_ROD)) {
                            if (isTeslaCoilBlock(level, check)) {
                                activateTeslaCoil(level, check, serverNow);
                            } else {
                                dischargeLightningRod(level, check, serverNow);
                            }
                        }
                    }
                }
            }

            Set<UUID> processed = new HashSet<>();
            Set<BlockPos> tickedTeslaCoils = new HashSet<>();
            for (ServerPlayer player : players) {
                // Check active Tesla Coils (Lightning Rod on Redstone Block), powered Lightning Rods, and block-form energized Redstone
                BlockPos playerPos = player.blockPosition();
                int redstoneArcBudget = (serverNow % 3L == 0L) ? 4 : 0;
                for (BlockPos p : BlockPos.betweenClosed(playerPos.offset(-14, -6, -14), playerPos.offset(14, 6, 14))) {
                    if (!level.hasChunkAt(p)) continue;
                    BlockState st = level.getBlockState(p);
                    if (st.is(Blocks.LIGHTNING_ROD)) {
                        BlockPos below = p.below();
                        if (level.hasChunkAt(below) && level.getBlockState(below).is(Blocks.REDSTONE_BLOCK)) {
                            if (st.hasProperty(LightningRodBlock.POWERED) && st.getValue(LightningRodBlock.POWERED)) {
                                activateTeslaCoil(level, p, serverNow);
                            }
                            if (tickedTeslaCoils.add(p.immutable())) {
                                tickTeslaCoil(level, p, st, serverNow);
                            }
                            continue;
                        }
                        if (Math.abs(p.getX() - playerPos.getX()) <= 8
                                && Math.abs(p.getY() - playerPos.getY()) <= 4
                                && Math.abs(p.getZ() - playerPos.getZ()) <= 8
                                && st.hasProperty(LightningRodBlock.POWERED)
                                && st.getValue(LightningRodBlock.POWERED)) {
                            dischargeLightningRod(level, p, serverNow);
                        }
                    } else if (redstoneArcBudget > 0
                            && Math.abs(p.getX() - playerPos.getX()) <= 8
                            && Math.abs(p.getY() - playerPos.getY()) <= 4
                            && Math.abs(p.getZ() - playerPos.getZ()) <= 8
                            && level.random.nextInt(8) == 0
                            && isBlockFormRedstoneLightningSource(st)
                            && hasExposedFace(level, p)) {
                        emitRedstoneBlockLightningArc(level, p);
                        redstoneArcBudget--;
                    }
                }

                AABB area = player.getBoundingBox().inflate(32.0D);
                List<LivingEntity> entities = level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        e -> e.isAlive() && !e.isRemoved() && !ElementalGaugeManager.isPassiveAquaticOrAmbient(e)
                );
                for (LivingEntity living : entities) {
                    if (processed.add(living.getUUID())) {
                        // Fast check every tick for 2-tick Observer pulses
                        tickObserverAndFastRedstone(level, living, serverNow);
                        if ((living.tickCount + Math.abs(living.getId())) % 20 == 0) {
                            tickEntityEnvironmentalAuras(level, living);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks short-pulsing Observer blocks (2-tick pulse) every tick so entities standing at the
     * single back output face of a triggered Observer immediately receive 1.0U Electro.
     */
    private static void tickObserverAndFastRedstone(ServerLevel level, LivingEntity living, long now) {
        BlockPos pos = living.blockPosition();
        if (!level.hasChunkAt(pos)) return;

        AABB entityBox = living.getBoundingBox().inflate(0.15D);
        BlockPos min = BlockPos.containing(entityBox.minX - 1.0D, entityBox.minY - 1.0D, entityBox.minZ - 1.0D);
        BlockPos max = BlockPos.containing(entityBox.maxX + 1.0D, entityBox.maxY + 1.0D, entityBox.maxZ + 1.0D);

        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(p)) continue;
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.OBSERVER) && st.hasProperty(ObserverBlock.POWERED) && st.getValue(ObserverBlock.POWERED)) {
                // Vanilla Observer outputs Redstone strictly from its back face (FACING.getOpposite())
                Direction outputDir = st.getValue(ObserverBlock.FACING).getOpposite();
                BlockPos outputPos = p.relative(outputDir);
                AABB outputBox = new AABB(outputPos);
                if (entityBox.intersects(outputBox)) {
                    long key = (((long) living.getId()) << 32) | (p.asLong() & 0xFFFFFFFFL);
                    Long lastObsZap = LAST_OBSERVER_ZAP.get(key);
                    if (lastObsZap == null || now < lastObsZap || (now - lastObsZap) >= 6L) {
                        LAST_OBSERVER_ZAP.put(key, now);
                        ElementalGaugeManager.applyElementBypassIcd(level, living, Element.ELECTRO, 1.0F, null, 0.0F);
                        Vec3 portPos = Vec3.atCenterOf(p).add(
                                outputDir.getStepX() * 0.5D,
                                outputDir.getStepY() * 0.5D,
                                outputDir.getStepZ() * 0.5D
                        );
                        Vec3 targetPos = living.position().add(0, living.getBbHeight() * 0.5D, 0);
                        ElectrifiedWaterManager.drawElectricArc(level, portPos, targetPos, 0.12D, false);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, targetPos.x, targetPos.y, targetPos.z, 4, 0.15D, 0.15D, 0.15D, 0.03D);
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

        // 1. Physical contact check: Redstone Block, powered Redstone Dust, lit Redstone Ore,
        //    Soul Campfire (2.0U Pyro), Slime/Honey Block, and solid Amethyst Block / Budding Amethyst
        AABB touchBox = living.getBoundingBox().inflate(0.10D, 0.10D, 0.10D);
        BlockPos min = BlockPos.containing(touchBox.minX, touchBox.minY - 0.20D, touchBox.minZ);
        BlockPos max = BlockPos.containing(touchBox.maxX, touchBox.maxY, touchBox.maxZ);
        boolean touchingRedstone = false;
        BlockPos redstoneArcPos = null;
        boolean touchingSlimeOrHoney = false;
        boolean touchingSoulCampfire = false;
        BlockPos amethystZapPos = null;
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(p)) continue;
            BlockState st = level.getBlockState(p);
            if (isBlockFormRedstoneLightningSource(st)) {
                touchingRedstone = true;
                if (redstoneArcPos == null) {
                    redstoneArcPos = p.immutable();
                }
            } else if (st.is(Blocks.REDSTONE_WIRE) && st.hasProperty(RedStoneWireBlock.POWER) && st.getValue(RedStoneWireBlock.POWER) > 0) {
                touchingRedstone = true;
            }
            if (st.is(Blocks.SOUL_CAMPFIRE) && (!st.hasProperty(CampfireBlock.LIT) || st.getValue(CampfireBlock.LIT))) {
                touchingSoulCampfire = true;
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
            if (redstoneArcPos != null) {
                Vec3 start = Vec3.atCenterOf(redstoneArcPos).add(
                        (level.random.nextDouble() - 0.5D) * 0.6D,
                        0.45D,
                        (level.random.nextDouble() - 0.5D) * 0.6D
                );
                Vec3 end = living.position().add(0, living.getBbHeight() * (0.35D + level.random.nextDouble() * 0.4D), 0);
                ElectrifiedWaterManager.drawElectricArc(level, start, end, 0.24D, true);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + 0.3D, living.getZ(), 2, 0.15D, 0.15D, 0.15D, 0.02D);
        }
        if (touchingSoulCampfire) {
            ElementalGaugeManager.applyElement(level, living, Element.PYRO, 2.0F, null, 0.0F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, living.getX(), living.getY() + 0.3D, living.getZ(), 3, 0.15D, 0.15D, 0.15D, 0.02D);
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
            if (lastZap == null || now < lastZap || (now - lastZap) >= 20L) {
                LAST_AMETHYST_ZAP.put(living.getUUID(), now);
                ElementalGaugeManager.applyElement(level, living, Element.ELECTRO, 1.0F, null, 1.0F);
                MobHazardAvoidanceManager.reportReactionOrHazard(living, null, Vec3.atCenterOf(amethystZapPos), Element.ELECTRO, null);

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

    public static boolean isBlockFormRedstoneLightningSource(BlockState st) {
        if (st.is(Blocks.REDSTONE_BLOCK)) return true;
        return (st.is(Blocks.REDSTONE_ORE) || st.is(Blocks.DEEPSLATE_REDSTONE_ORE) || st.is(Blocks.REDSTONE_LAMP))
                && st.hasProperty(BlockStateProperties.LIT)
                && st.getValue(BlockStateProperties.LIT);
    }

    private static boolean hasExposedFace(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (!level.hasChunkAt(adj)) continue;
            if (!level.getBlockState(adj).canOcclude()) return true;
        }
        return false;
    }

    private static void emitRedstoneBlockLightningArc(ServerLevel level, BlockPos pos) {
        List<Direction> open = new ArrayList<>(6);
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (level.hasChunkAt(adj) && !level.getBlockState(adj).canOcclude()) {
                open.add(dir);
            }
        }
        if (open.isEmpty()) return;
        Direction face = open.get(level.random.nextInt(open.size()));
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 faceOffset = new Vec3(face.getStepX() * 0.54D, face.getStepY() * 0.54D, face.getStepZ() * 0.54D);
        double rx = face.getAxis() == Direction.Axis.X ? 0.0D : (level.random.nextDouble() - 0.5D) * 0.92D;
        double ry = face.getAxis() == Direction.Axis.Y ? 0.0D : (level.random.nextDouble() - 0.5D) * 0.92D;
        double rz = face.getAxis() == Direction.Axis.Z ? 0.0D : (level.random.nextDouble() - 0.5D) * 0.92D;
        Vec3 start = center.add(faceOffset).add(rx, ry, rz);

        Vec3 end = null;
        BlockPos neighborCandidate = pos.offset(
                level.random.nextInt(5) - 2,
                level.random.nextInt(3) - 1,
                level.random.nextInt(5) - 2
        );
        if (!neighborCandidate.equals(pos) && level.hasChunkAt(neighborCandidate)
                && isBlockFormRedstoneLightningSource(level.getBlockState(neighborCandidate))
                && hasExposedFace(level, neighborCandidate)) {
            end = Vec3.atCenterOf(neighborCandidate).add(faceOffset);
        }
        if (end == null) {
            double ex = face.getAxis() == Direction.Axis.X
                    ? (level.random.nextBoolean() ? face.getStepX() * 0.54D : (level.random.nextDouble() - 0.5D) * 0.5D)
                    : (level.random.nextDouble() - 0.5D) * 1.1D;
            double ey = face.getAxis() == Direction.Axis.Y
                    ? (level.random.nextBoolean() ? face.getStepY() * 0.54D : (level.random.nextDouble() - 0.5D) * 0.5D)
                    : (level.random.nextDouble() - 0.5D) * 1.1D;
            double ez = face.getAxis() == Direction.Axis.Z
                    ? (level.random.nextBoolean() ? face.getStepZ() * 0.54D : (level.random.nextDouble() - 0.5D) * 0.5D)
                    : (level.random.nextDouble() - 0.5D) * 1.1D;
            end = center.add(ex, ey, ez);
            if (end.distanceToSqr(start) < 0.25D) {
                end = center.add(-faceOffset.x * 1.05D, -faceOffset.y * 1.05D, -faceOffset.z * 1.05D)
                        .add((level.random.nextDouble() - 0.5D) * 0.7D,
                             (level.random.nextDouble() - 0.5D) * 0.7D,
                             (level.random.nextDouble() - 0.5D) * 0.7D);
            }
        }
        ElectrifiedWaterManager.drawElectricArc(level, start, end, 0.22D, level.random.nextFloat() < 0.45F);
    }

    public static boolean isSolidAmethystBlock(BlockState st) {
        return st.is(Blocks.AMETHYST_BLOCK)
                || st.is(Blocks.BUDDING_AMETHYST)
                || st.is(Blocks.SMALL_AMETHYST_BUD)
                || st.is(Blocks.MEDIUM_AMETHYST_BUD)
                || st.is(Blocks.LARGE_AMETHYST_BUD);
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

    public static boolean isTeslaCoilBlock(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return false;
        return level.getBlockState(pos).is(Blocks.LIGHTNING_ROD)
                && level.getBlockState(pos.below()).is(Blocks.REDSTONE_BLOCK);
    }

    public static void activateTeslaCoil(ServerLevel level, BlockPos rodPos, long now) {
        if (!isTeslaCoilBlock(level, rodPos)) return;
        BlockPos immutableRod = rodPos.immutable();
        Long currentUntil = TESLA_COIL_OVERCHARGE_UNTIL.get(immutableRod);
        long newUntil = now + 600L; // 30s
        boolean freshActivation = currentUntil == null || currentUntil <= now || (newUntil - currentUntil) > 15L;
        TESLA_COIL_OVERCHARGE_UNTIL.put(immutableRod, Math.max(currentUntil != null ? currentUntil : 0L, newUntil));
        if (freshActivation) {
            Vec3 tip = Vec3.atCenterOf(immutableRod).add(0.0D, 0.35D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 10, 0.25D, 0.25D, 0.25D, 0.08D);
            for (ServerPlayer sp : level.getEntitiesOfClass(ServerPlayer.class, new AABB(immutableRod).inflate(24.0D), p -> p.isAlive() && !p.isSpectator())) {
                com.velorise.staves.advancement.ModAdvancements.trigger(sp, com.velorise.staves.advancement.ModAdvancements.EVENT_TESLA_COIL);
            }
        }
    }

    public static void activateTeslaCoilsNear(ServerLevel level, Vec3 pos, double radius) {
        if (level == null || pos == null) return;
        long now = level.getGameTime();
        int rCeil = (int) Math.ceil(radius);
        double rSq = radius * radius;
        BlockPos center = BlockPos.containing(pos);
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-rCeil, -rCeil, -rCeil),
                center.offset(rCeil, rCeil, rCeil))) {
            if (p.distToCenterSqr(pos) <= rSq && isTeslaCoilBlock(level, p)) {
                activateTeslaCoil(level, p, now);
            }
        }
    }

    /**
     * Ticks an active Tesla Coil (Copper Lightning Rod placed on top of a Redstone Block).
     * Normal mode: every 2s (40 ticks), 3-5 bolts in 5 blocks applying 0.5U Electro + 0.5 heart (1.0F dmg).
     * Overcharged mode (during rain or 30s after lightning): all stats x2
     * (every 1s, 6-10 bolts in 10 blocks applying 1.0U Electro + 1.0 heart (2.0F dmg)).
     */
    private static void tickTeslaCoil(ServerLevel level, BlockPos rodPos, BlockState rodState, long now) {
        BlockPos immutableRod = rodPos.immutable();
        Long overchargeUntil = TESLA_COIL_OVERCHARGE_UNTIL.get(immutableRod);
        boolean lightningActive = overchargeUntil != null && now < overchargeUntil;
        boolean rainActive = level.isRainingAt(immutableRod.above());
        boolean overcharged = lightningActive || rainActive;

        Direction facing = rodState.hasProperty(LightningRodBlock.FACING)
                ? rodState.getValue(LightningRodBlock.FACING)
                : Direction.UP;
        Vec3 origin = Vec3.atCenterOf(immutableRod).add(
                facing.getStepX() * 0.35D,
                facing.getStepY() * 0.35D,
                facing.getStepZ() * 0.35D
        );

        if (overcharged && (now % 10L == 0L)) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                    3, 0.16D, 0.12D, 0.16D, 0.03D);
        }

        long intervalTicks = overcharged ? 20L : 40L;
        Long last = LAST_TESLA_COIL_ZAP.get(immutableRod);
        if (last != null && now >= last && (now - last) < intervalTicks) {
            return;
        }
        LAST_TESLA_COIL_ZAP.put(immutableRod, now);
        for (ServerPlayer sp : level.getEntitiesOfClass(ServerPlayer.class, new AABB(immutableRod).inflate(16.0D), p -> p.isAlive() && !p.isSpectator())) {
            com.velorise.staves.advancement.ModAdvancements.trigger(sp, com.velorise.staves.advancement.ModAdvancements.EVENT_TESLA_COIL);
        }

        int rangeBlocks = overcharged ? 10 : 5;
        int maxRangeSq = rangeBlocks * rangeBlocks;
        double maxRayDistSq = overcharged ? 105.0D : 27.5D;
        int boltCount = overcharged
                ? (6 + level.random.nextInt(5))
                : (3 + level.random.nextInt(3));
        float electroU = overcharged ? 1.0F : 0.5F;
        float boltDamage = overcharged ? 2.0F : 1.0F;
        double waterRadius = overcharged ? 4.0D : 2.0D;
        int waterDuration = overcharged ? 70 : 35;
        double dendroCoreRadius = overcharged ? 2.5D : 1.25D;

        int span = rangeBlocks * 2 + 1;
        int tyMin = overcharged ? -2 : -1;
        int tyCount = overcharged ? 8 : 5;
        Set<UUID> hitEntitiesThisPulse = new HashSet<>();

        for (int b = 0; b < boltCount; b++) {
            int dx = 0;
            int dy = 0;
            int dz = 0;
            boolean picked = false;
            for (int attempt = 0; attempt < 10; attempt++) {
                int tx = level.random.nextInt(span) - rangeBlocks;
                int ty = level.random.nextInt(tyCount) + tyMin;
                int tz = level.random.nextInt(span) - rangeBlocks;
                int d2 = tx * tx + ty * ty + tz * tz;
                if (d2 >= 4 && d2 <= maxRangeSq && !(tx == 0 && ty == -1 && tz == 0)) {
                    dx = tx;
                    dy = ty;
                    dz = tz;
                    picked = true;
                    break;
                }
            }
            if (!picked) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                double minFallback = overcharged ? 4.0D : 2.5D;
                double distChoice = minFallback + level.random.nextDouble() * (rangeBlocks - minFallback);
                dx = (int) Math.round(Math.cos(angle) * distChoice);
                dy = level.random.nextInt(4) - 1;
                dz = (int) Math.round(Math.sin(angle) * distChoice);
                if (dx == 0 && dz == 0) dx = 2;
            }

            BlockPos chosenPos = immutableRod.offset(dx, dy, dz);
            Vec3 targetVec = Vec3.atCenterOf(chosenPos);
            Vec3 delta = targetVec.subtract(origin);
            double rayLen = delta.length();
            if (rayLen < 0.5D) continue;

            int steps = Math.max(4, (int) Math.ceil(rayLen / 0.45D));
            Vec3 arcEnd = origin;

            for (int s = 1; s <= steps; s++) {
                double t = (double) s / (double) steps;
                Vec3 pt = origin.add(delta.scale(t));
                if (pt.distanceToSqr(origin) > maxRayDistSq) {
                    break;
                }

                BlockPos stepPos = BlockPos.containing(pt);
                if (!level.hasChunkAt(stepPos)) {
                    break;
                }

                boolean isSelfBlock = stepPos.equals(immutableRod) || stepPos.equals(immutableRod.below());
                if (!isSelfBlock) {
                    if (level.getFluidState(stepPos).is(FluidTags.WATER)) {
                        ElectrifiedWaterManager.electrify(level, stepPos, waterRadius, waterDuration, null, electroU);
                    }
                    DendroCoreManager.checkReactionsNear(level, pt, dendroCoreRadius, Element.ELECTRO, null);

                    AABB hitBox = new AABB(
                            pt.x - 1.15D, pt.y - 1.15D, pt.z - 1.15D,
                            pt.x + 1.15D, pt.y + 1.15D, pt.z + 1.15D
                    );
                    LivingEntity struck = null;
                    double bestDistSq = Double.MAX_VALUE;
                    for (LivingEntity cand : level.getEntitiesOfClass(
                            LivingEntity.class,
                            hitBox,
                            e -> e.isAlive()
                                    && !e.isRemoved()
                                    && !e.isSpectator()
                                    && !ElementalGaugeManager.isPassiveAquaticOrAmbient(e)
                                    && !hitEntitiesThisPulse.contains(e.getUUID())
                    )) {
                        Vec3 center = cand.getBoundingBox().getCenter();
                        double dSq = center.distanceToSqr(pt);
                        if (dSq < bestDistSq && center.distanceToSqr(origin) <= maxRayDistSq) {
                            bestDistSq = dSq;
                            struck = cand;
                        }
                    }

                    if (struck != null) {
                        hitEntitiesThisPulse.add(struck.getUUID());
                        arcEnd = struck.getBoundingBox().getCenter();
                        ElementalGaugeManager.applyElementBypassIcd(level, struck, Element.ELECTRO, electroU, null, boltDamage);
                        MobHazardAvoidanceManager.reportReactionOrHazard(struck, null, origin, Element.ELECTRO, null);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, arcEnd.x, arcEnd.y, arcEnd.z,
                                5, 0.2D, 0.2D, 0.2D, 0.04D);
                        break;
                    }

                    if (level.getBlockState(stepPos).canOcclude()) {
                        arcEnd = pt;
                        break;
                    }
                }
                arcEnd = pt;
            }

            if (arcEnd.distanceToSqr(origin) >= 0.35D) {
                ElectrifiedWaterManager.drawElectricArc(level, origin, arcEnd, overcharged ? 0.30D : 0.24D, true);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, arcEnd.x, arcEnd.y, arcEnd.z,
                        overcharged ? 3 : 2, 0.12D, 0.12D, 0.12D, 0.02D);
            }
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                overcharged ? 5 : 3, 0.12D, 0.12D, 0.12D, 0.02D);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS,
                overcharged ? 0.38F : 0.25F, overcharged ? 1.75F : 1.9F);
    }

    /**
     * Extinguishes environmental fire blocks (Fire, Soul Fire, lit Campfires, lit Candles / Candle Cakes)
     * touched along a Hydro beam and within splash radius around the impact point.
     */
    public static void applyHydroEnvironmentEffects(ServerLevel level, Vec3 start, Vec3 end, double radius) {
        if (level == null || start == null || end == null) return;
        Vec3 delta = end.subtract(start);
        double len = delta.length();
        int steps = Math.max(1, (int) Math.ceil(len / 0.55D));
        Set<BlockPos> checked = new HashSet<>();
        int extinguishedCount = 0;

        for (int s = 0; s <= steps; s++) {
            double t = len < 1.0E-4D ? 1.0D : (double) s / (double) steps;
            Vec3 pt = start.add(delta.scale(t));
            BlockPos center = BlockPos.containing(pt);
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                if (p.distToCenterSqr(pt) <= 0.85D * 0.85D) {
                    BlockPos imm = p.immutable();
                    if (checked.add(imm) && tryExtinguishBlock(level, imm)) {
                        extinguishedCount++;
                    }
                }
            }
        }

        BlockPos endCenter = BlockPos.containing(end);
        int rCeil = (int) Math.ceil(radius);
        double rSq = radius * radius;
        for (BlockPos p : BlockPos.betweenClosed(
                endCenter.offset(-rCeil, -rCeil, -rCeil),
                endCenter.offset(rCeil, rCeil, rCeil))) {
            if (p.distToCenterSqr(end) <= rSq) {
                BlockPos imm = p.immutable();
                if (checked.add(imm) && tryExtinguishBlock(level, imm)) {
                    extinguishedCount++;
                }
            }
        }

        if (extinguishedCount > 0) {
            level.playSound(null, end.x, end.y, end.z,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.1F + level.random.nextFloat() * 0.25F);
        }
    }

    private static boolean tryExtinguishBlock(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState st = level.getBlockState(pos);
        if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)) {
            level.removeBlock(pos, false);
            spawnExtinguishParticles(level, pos);
            return true;
        }
        if ((st.getBlock() instanceof CampfireBlock || st.getBlock() instanceof AbstractCandleBlock)
                && st.hasProperty(BlockStateProperties.LIT)
                && st.getValue(BlockStateProperties.LIT)) {
            level.setBlock(pos, st.setValue(BlockStateProperties.LIT, false), 3);
            spawnExtinguishParticles(level, pos);
            return true;
        }
        return false;
    }

    private static void spawnExtinguishParticles(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.45D;
        double cz = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 4, 0.22D, 0.18D, 0.22D, 0.02D);
        level.sendParticles(ParticleTypes.SPLASH, cx, cy, cz, 4, 0.25D, 0.18D, 0.25D, 0.05D);
    }
}
