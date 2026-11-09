package com.velorise.staves.element;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnvironmentalBlockManager {

    private static final Map<UUID, Long> LAST_AMETHYST_ZAP = new ConcurrentHashMap<>();

    private EnvironmentalBlockManager() {}

    public static void clearAll() {
        LAST_AMETHYST_ZAP.clear();
    }

    public static void tick(MinecraftServer server) {
        long serverNow = server.overworld().getGameTime();
        if (serverNow % 100 == 0) {
            LAST_AMETHYST_ZAP.entrySet().removeIf(e -> (serverNow - e.getValue()) > 100);
        }

        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) continue;

            Set<LivingEntity> processed = new HashSet<>();
            for (ServerPlayer player : players) {
                AABB area = player.getBoundingBox().inflate(32.0D);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);
                for (LivingEntity living : entities) {
                    if (processed.add(living)) {
                        // Stagger checks across 20 ticks (1s interval per entity)
                        if ((living.tickCount + Math.abs(living.getId())) % 20 == 0) {
                            tickEntityEnvironmentalAuras(level, living);
                        }
                    }
                }
            }
        }
    }

    private static void tickEntityEnvironmentalAuras(ServerLevel level, LivingEntity living) {
        BlockPos pos = living.blockPosition();

        // 1. Redstone Block: Emits Electro aura within 8 blocks radius (1s / 1U)
        boolean nearRedstone = BlockPos.findClosestMatch(pos, 8, 4, p -> level.getBlockState(p).is(Blocks.REDSTONE_BLOCK)).isPresent();
        if (nearRedstone) {
            ElementalGaugeManager.applyElement(level, living, Element.ELECTRO, 1.0F, null, 0.0F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + 0.5D, living.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.05D);
        }

        // 2. Slime Block & Honey Block: Emits Dendro aura when touching/standing on them
        AABB touchBox = living.getBoundingBox().inflate(0.25D);
        BlockPos min = BlockPos.containing(touchBox.minX, touchBox.minY, touchBox.minZ);
        BlockPos max = BlockPos.containing(touchBox.maxX, touchBox.maxY, touchBox.maxZ);
        boolean touchDendroBlock = false;
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.SLIME_BLOCK) || st.is(Blocks.HONEY_BLOCK)) {
                touchDendroBlock = true;
                break;
            }
        }
        if (touchDendroBlock) {
            ElementalGaugeManager.applyElement(level, living, Element.DENDRO, 1.0F, null, 0.0F);
        }

        // 3. Sculk Sensor (8 blocks) & Calibrated Sculk Sensor (16 blocks): Emits Electro when ACTIVATED
        boolean nearActiveSensor = BlockPos.findClosestMatch(pos, 8, 4, p -> {
            BlockState st = level.getBlockState(p);
            return st.is(Blocks.SCULK_SENSOR) && SculkSensorBlock.getPhase(st) == SculkSensorPhase.ACTIVE;
        }).isPresent();

        if (!nearActiveSensor) {
            nearActiveSensor = BlockPos.findClosestMatch(pos, 16, 6, p -> {
                BlockState st = level.getBlockState(p);
                return st.is(Blocks.CALIBRATED_SCULK_SENSOR) && SculkSensorBlock.getPhase(st) == SculkSensorPhase.ACTIVE;
            }).isPresent();
        }

        if (nearActiveSensor) {
            ElementalGaugeManager.applyElement(level, living, Element.ELECTRO, 1.0F, null, 0.0F);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, living.getX(), living.getY() + 0.5D, living.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.02D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + 0.5D, living.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.05D);
        }

        // 4. Every Amethyst: Innate Electro, zaps nearby entities within 1 block for 1 damage (like Genshin)
        AABB zapBox = living.getBoundingBox().inflate(1.2D);
        BlockPos minZap = BlockPos.containing(zapBox.minX, zapBox.minY, zapBox.minZ);
        BlockPos maxZap = BlockPos.containing(zapBox.maxX, zapBox.maxY, zapBox.maxZ);
        BlockPos amethystPos = null;
        for (BlockPos p : BlockPos.betweenClosed(minZap, maxZap)) {
            BlockState st = level.getBlockState(p);
            if (isAmethystBlock(st)) {
                amethystPos = p.immutable();
                break;
            }
        }
        if (amethystPos != null && ElementalGaugeManager.getInnateElement(living) != Element.ELECTRO) {
            long now = level.getGameTime();
            Long lastZap = LAST_AMETHYST_ZAP.get(living.getUUID());
            if (lastZap == null || (now - lastZap) >= 20) {
                LAST_AMETHYST_ZAP.put(living.getUUID(), now);
                living.hurt(level.damageSources().magic(), 1.0F);
                ElementalGaugeManager.applyElement(level, living, Element.ELECTRO, 1.0F, null, 0.0F);

                Vec3 end = living.position().add(0, living.getBbHeight() * 0.5D, 0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 6, 0.25D, 0.25D, 0.25D, 0.05D);
                level.playSound(null, amethystPos.getX(), amethystPos.getY(), amethystPos.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.6F);
                level.playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.4F, 1.9F);
            }
        }
    }

    public static boolean isAmethystBlock(BlockState st) {
        return st.is(Blocks.AMETHYST_BLOCK)
                || st.is(Blocks.BUDDING_AMETHYST)
                || st.is(Blocks.AMETHYST_CLUSTER)
                || st.is(Blocks.LARGE_AMETHYST_BUD)
                || st.is(Blocks.MEDIUM_AMETHYST_BUD)
                || st.is(Blocks.SMALL_AMETHYST_BUD);
    }
}
