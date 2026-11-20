package com.velorise.staves.element;

import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager.
 * Tracks Gauge Units (U), Aura Decay Inheritance, Underlying Freeze & Quicken Auras,
 * Simultaneous Reaction Priority, Innate Lock, Environmental Auras, and combat feedback.
 */

public final class VentiManager {
    private VentiManager() {}

    public static final java.util.List<DaybreakVortexField> DAYBREAK_VORTICES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            VENTI_IDLE_STATES.remove(playerId);
            DAYBREAK_VORTICES.removeIf(v -> playerId.equals(v.ownerId));
        }
    }

    public static void clearAll() {
        DAYBREAK_VORTICES.clear();
        VENTI_IDLE_STATES.clear();
    }

    public static final class DaybreakVortexField {
        final ServerLevel level;
        final Vec3 center;
        final UUID ownerId;
        final boolean isNetherite;
        final long expireTick;
        final double radius;
        @Nullable Element absorbedElement;

        DaybreakVortexField(ServerLevel level, Vec3 center, UUID ownerId, boolean isNetherite, long expireTick, double radius) {
            this.level = level;
            this.center = center;
            this.ownerId = ownerId;
            this.isNetherite = isNetherite;
            this.expireTick = expireTick;
            this.radius = radius;
        }
    }

    public static boolean isInsideAnemoUpdraft(ServerLevel level, Vec3 pos) {
        if (level == null || pos == null || DAYBREAK_VORTICES.isEmpty()) return false;
        for (DaybreakVortexField v : DAYBREAK_VORTICES) {
            if (v.level != level) continue;
            double dx = pos.x - v.center.x;
            double dz = pos.z - v.center.z;
            double r = v.radius + 1.5D;
            if ((dx * dx + dz * dz) <= r * r && pos.y >= v.center.y - 3.0D && pos.y <= v.center.y + 14.0D) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasVentiWeapon(@Nullable Player player) {
        if (player == null) return false;
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK) {
                return true;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (!s.isEmpty() && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(s) == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK) {
                return true;
            }
        }
        return false;
    }

    public static void summonDaybreakVortex(ServerPlayer owner, Vec3 center, boolean isNetherite) {
        ServerLevel level = owner.serverLevel();
        long now = level.getServer().overworld().getGameTime();
        long duration = 160L; // 8.0s Genshin Wind's Grand Ode (same for Basic & Netherite)
        double radius = isNetherite ? 6.0D : 4.0D; // 8m Basic, 12m Netherite (x1.5)
        DAYBREAK_VORTICES.removeIf(v -> v.ownerId.equals(owner.getUUID()));
        DAYBREAK_VORTICES.add(new DaybreakVortexField(level, center, owner.getUUID(), isNetherite, now + duration, radius));
        com.velorise.staves.network.ModNetwork.sendVortexSphere(level, center.add(0.0D, 1.0D, 0.0D), radius, (int) duration, 0xFF66FFD8);
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, center.x, center.y + 0.9D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.2F, 0.85F);
    }

    public static void tickDaybreakVortices(MinecraftServer server, long serverNow) {
        if (DAYBREAK_VORTICES.isEmpty()) return;
        DAYBREAK_VORTICES.removeIf(v -> serverNow >= v.expireTick || v.level == null);
        for (DaybreakVortexField vortex : DAYBREAK_VORTICES) {
            ServerLevel level = vortex.level;
            Vec3 center = vortex.center;
            double radius = vortex.radius;
            double radiusSqr = radius * radius;
            ServerPlayer owner = server.getPlayerList().getPlayer(vortex.ownerId);

            if (serverNow % 2L == 0L) {
                AABB pullBox = new AABB(center, center).inflate(radius);
                Vec3 eyeTarget = center.add(0.0D, 0.85D, 0.0D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e.isAlive() && !e.isRemoved())) {
                    if (target.getUUID().equals(vortex.ownerId)) continue;
                    if (target instanceof net.minecraft.world.entity.TamableAnimal tame && vortex.ownerId.equals(tame.getOwnerUUID())) continue;
                    if (target instanceof ServerPlayer && !server.isPvpAllowed()) continue;
                    if (isEntityFrozen(target)) continue;
                    double distSqr = target.position().distanceToSqr(center);
                    if (distSqr > radiusSqr) continue;

                    Vec3 diff = eyeTarget.subtract(target.getBoundingBox().getCenter());
                    double dist = diff.length();
                    if (dist > 0.25D) {
                        Vec3 dir = diff.scale(1.0D / dist);
                        double bossScale = target.getMaxHealth() >= 100.0F ? 0.35D : 1.0D;
                        double pullStrength = Math.min(0.42D, 0.16D + (dist / radius) * 0.26D) * bossScale;
                        Vec3 cur = target.getDeltaMovement();
                        double nextY = (target.getMaxHealth() >= 100.0F)
                                ? cur.y
                                : Math.max(-0.08D, Math.min(0.32D, cur.y * 0.65D + dir.y * 0.22D + 0.06D));
                        target.setDeltaMovement(
                                cur.x * 0.72D + dir.x * pullStrength,
                                nextY,
                                cur.z * 0.72D + dir.z * pullStrength
                        );
                        target.fallDistance = 0.0F;
                        target.hurtMarked = true;
                    }
                }
                for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, pullBox, e -> e.isAlive() && !e.isRemoved())) {
                    if (item.position().distanceToSqr(center) > radiusSqr) continue;
                    Vec3 diff = eyeTarget.subtract(item.position());
                    double dist = diff.length();
                    if (dist > 0.35D) {
                        Vec3 dir = diff.scale(1.0D / dist);
                        double pull = Math.min(0.32D, 0.12D + (dist / radius) * 0.20D);
                        Vec3 cur = item.getDeltaMovement();
                        item.setDeltaMovement(cur.x * 0.72D + dir.x * pull, Math.max(-0.05D, Math.min(0.24D, cur.y * 0.65D + dir.y * 0.18D + 0.04D)), cur.z * 0.72D + dir.z * pull);
                    }
                }
            }

            if (serverNow % 10L == 0L) {
                AABB tickBox = new AABB(center, center).inflate(radius);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, tickBox, e -> {
                    if (!e.isAlive() || e.isRemoved() || e.getUUID().equals(vortex.ownerId)) return false;
                    if (e instanceof net.minecraft.world.entity.TamableAnimal tame && vortex.ownerId.equals(tame.getOwnerUUID())) return false;
                    if (e instanceof ServerPlayer && !server.isPvpAllowed()) return false;
                    return e.position().distanceToSqr(center) <= radiusSqr;
                });

                if (vortex.absorbedElement == null) {
                    Element absorbed = detectDaybreakAbsorption(level, center, targets);
                    if (absorbed != null) {
                        vortex.absorbedElement = absorbed;
                        int absorbedColor = switch (absorbed) {
                            case PYRO -> 0xFFFF5522;
                            case HYDRO -> 0xFF18BFFF;
                            case ELECTRO -> 0xFFC44DFF;
                            case CRYO -> 0xFF7CF4FF;
                            default -> 0xFF66FFD8;
                        };
                        int remainingTicks = (int) Math.max(5L, vortex.expireTick - serverNow);
                        com.velorise.staves.network.ModNetwork.sendVortexSphere(
                                level, center.add(0.0D, 1.0D, 0.0D), radius, remainingTicks, absorbedColor);
                        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);
                    }
                }

                float baseDmg = StavesConfig.getStaffBaseDamage(com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK, vortex.isNetherite);
                float anemoTickDmg = baseDmg * 0.6F;
                float absorbedTickDmg = baseDmg * 0.45F;
                for (LivingEntity target : targets) {
                    applyElement(level, target, Element.ANEMO, 1.0F, owner, anemoTickDmg);
                    if (vortex.absorbedElement != null) {
                        applyElement(level, target, vortex.absorbedElement, 1.0F, owner, absorbedTickDmg);
                    }
                }
                level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.35F, 1.4F);
            }
        }
    }

    public static @Nullable Element detectDaybreakAbsorption(ServerLevel level, Vec3 center, List<LivingEntity> targets) {
        Element[] priority = {Element.PYRO, Element.HYDRO, Element.ELECTRO, Element.CRYO};
        for (Element candidate : priority) {
            for (LivingEntity target : targets) {
                Map<Element, GaugeState> gauges = GAUGES.get(target.getUUID());
                if (gauges != null && gauges.containsKey(candidate)) {
                    return candidate;
                }
                if (candidate == Element.PYRO && target.isOnFire()) {
                    return Element.PYRO;
                }
                if (candidate == Element.HYDRO && target.isInWaterOrRain()) {
                    return Element.HYDRO;
                }
            }
        }
        // Environmental fire absorption near vortex center (Fire, Soul Fire, Lava, lit Campfire, Torches)
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.LAVA) || st.is(Blocks.MAGMA_BLOCK)
                    || st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)
                    || st.is(Blocks.SOUL_TORCH) || st.is(Blocks.SOUL_WALL_TORCH)
                    || (st.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock
                        && (!st.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
                            || st.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)))) {
                return Element.PYRO;
            }
        }
        return null;
    }

    /**
     * Server tick loop for decay, environmental water/rain extinguish, and periodic reactions.
     */

    private static final class ArchonStillState {
        net.minecraft.world.phys.Vec3 lastStillPos = net.minecraft.world.phys.Vec3.ZERO;
        int stillTicks = 0;
        int activeCode = 0;
        int lastSyncedCode = -1;
        int idleVariant = 0;
        int songIndex = 0;
    }

    private static final java.util.Map<java.util.UUID, ArchonStillState> VENTI_IDLE_STATES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void cancelIdle(ServerPlayer player) {
        if (player == null) return;
        ArchonStillState vst = VENTI_IDLE_STATES.get(player.getUUID());
        if (vst != null && (vst.activeCode == 4051 || vst.stillTicks > 0)) {
            vst.stillTicks = 0;
            vst.lastStillPos = player.position();
            vst.activeCode = 4050;
            vst.lastSyncedCode = 4050;
            com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 4050);
        }
    }

    public static void tickIdle(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();

            // Venti (DAYBREAK)
            ArchonStillState vst = VENTI_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new ArchonStillState());
            boolean holdingVenti = ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.DAYBREAK;
            if (vst.lastStillPos == null || vst.lastStillPos.equals(net.minecraft.world.phys.Vec3.ZERO)) {
                vst.lastStillPos = player.position();
            }
            double vDistSq = player.position().distanceToSqr(vst.lastStillPos);
            boolean canVenti = holdingVenti && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting() && player.getVehicle() == null;
            if (canVenti && vDistSq < 0.0025D) {
                vst.stillTicks++;
                if (vst.stillTicks == 400) {
                    vst.idleVariant = (vst.idleVariant + 1) % 2;
                    if (vst.idleVariant == 0) {
                        vst.songIndex = (vst.songIndex + 1) % 2;
                    }
                }
                if (vst.stillTicks >= 200) {
                    if (vst.idleVariant == 1) {
                        // Levitation idle hovers for ~5s (ticks 400..500) then gently descends
                        if (vst.stillTicks <= 500) {
                            vst.activeCode = 4052;
                        } else {
                            vst.activeCode = 4050;
                            vst.stillTicks = 0;
                        }
                    } else {
                        vst.activeCode = 4051;
                    }
                } else {
                    vst.activeCode = 4050;
                }
            } else {
                vst.stillTicks = 0;
                vst.lastStillPos = player.position();
                vst.activeCode = 4050;
            }
            if (vst.activeCode != vst.lastSyncedCode || (vst.activeCode == 4051 && now % 20L == 0L)) {
                vst.lastSyncedCode = vst.activeCode;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, vst.activeCode);
            }
            if (vst.activeCode == 4051 && vst.stillTicks >= 200) {
                // {note, durationTicks} pairs for expressive rhythm (2t fast runs, 3-4t normal, 6-8t held notes)
                int[][][] melodies = {
                    // 1. Venti's Theme (Bard's Adventure / A New Day with Hope)
                    {
                        {17,3},{20,3},{22,3},{24,7}, {22,2},{20,3},{17,3},{15,6},
                        {17,3},{20,3},{22,3},{20,3},{17,2},{15,3},{12,7},
                        {15,3},{17,3},{20,3},{22,3},{24,7}, {22,2},{20,3},{17,6},
                        {17,3},{20,3},{22,3},{24,5}, {22,2},{24,3},{27,6},{24,6},
                        {22,2},{20,3},{17,3},{15,3},{17,3},{20,6},
                        // Chorus
                        {24,4},{24,2},{22,3},{20,3},{22,3},{24,7}, {27,4},{24,3},{22,3},{20,3},{17,6},
                        {20,2},{22,2},{24,3},{27,6},{24,3},{22,3},{20,3},{17,3},{15,6},
                        {17,3},{20,3},{22,3},{20,3},{17,3},{15,3},{12,8}, {17,8}
                    },
                    // 2. Furina Execution (Sinner's Finale / La Vaguelette)
                    {
                        {13,4},{18,4},{20,4},{21,8}, {20,3},{18,3},{17,3},{18,7},
                        {13,4},{14,3},{13,3},{11,3},{9,3},{8,3},{6,8},
                        {13,4},{18,4},{20,4},{21,7}, {20,3},{21,3},{23,6},{21,7},
                        {20,3},{18,3},{17,3},{18,3},{20,7},
                        // Chorus
                        {25,5},{25,3},{23,3},{21,3},{20,3},{21,8}, {23,4},{21,3},{20,3},{18,3},{17,7},
                        {18,3},{20,3},{21,4},{25,7},{23,3},{21,3},{20,3},{18,3},{17,7},
                        {13,4},{18,4},{20,4},{21,6},{20,3},{18,3},{17,4},{18,10}
                    }
                };
                int[][] song = melodies[Math.floorMod(vst.songIndex, melodies.length)];
                int elapsed = vst.stillTicks - 60;
                int acc = 0;
                boolean played = false;
                for (int[] pair : song) {
                    if (elapsed == acc) {
                        int note = pair[0];
                        if (note >= 0 && player.level() instanceof ServerLevel sl) {
                            float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
                            sl.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(),
                                    SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 0.55F, pitch);
                            if (pair[1] >= 6) {
                                sl.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(),
                                        SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 0.25F, pitch);
                            }
                        }
                        played = true;
                        break;
                    }
                    acc += pair[1];
                }
                if (!played && elapsed >= acc) {
                    vst.activeCode = 4050;
                    vst.stillTicks = 0;
                }
            }

        }
    }
}
