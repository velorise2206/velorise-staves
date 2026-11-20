package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Genshin-style Freeze state application, statue pose locking,
 * freeze expiration guards, and player struggle-to-break-free mechanics.
 */
public final class FreezeStateManager {

    private FreezeStateManager() {}

    public static void enforceFrozenPoseIfFrozen(LivingEntity target) {
        if (target != null && isEntityFrozen(target)) {
            enforceFrozenPose(target, target.getUUID());
        }
    }

    public static void applyFreezeState(LivingEntity target, UUID targetId, GaugeState newFreeze, int freezeTicks, long now) {
        FROZEN_GAUGES.put(targetId, newFreeze);
        FROZEN_UNTIL.put(targetId, now + freezeTicks);
        target.setTicksFrozen(0);
        MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        target.stopUsingItem();
        target.releaseUsingItem();
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = false;
        target.hurtMarked = true;

        FROZEN_POSES.computeIfAbsent(targetId, k -> new FrozenPoseState(
                target.getX(), target.getY(), target.getZ(),
                target.getYRot(), target.getXRot(), target.getYHeadRot(), target.yBodyRot
        ));

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setAggressive(false);
            mob.setZza(0.0F);
            mob.setXxa(0.0F);
            mob.setYya(0.0F);
            mob.setSpeed(0.0F);
            mob.setJumping(false);
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
            }
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
            if (!mob.getTags().contains(TAG_FROZEN_STATUE)) {
                mob.addTag(TAG_FROZEN_STATUE);
            }
        }
    }

    public static void enforceFrozenPose(LivingEntity target, UUID id) {
        target.setTicksFrozen(0);
        if (target.isUsingItem()) {
            target.stopUsingItem();
            target.releaseUsingItem();
        }
        if (target instanceof net.minecraft.world.entity.npc.AbstractVillager villager && villager.getUnhappyCounter() > 0) {
            villager.setUnhappyCounter(0);
        }
        FrozenPoseState pose = FROZEN_POSES.get(id);
        if (pose != null) {
            double dy = target.onGround() ? 0.0D : Math.max(-0.08D, target.getDeltaMovement().y);
            target.setDeltaMovement(0.0D, dy, 0.0D);
            target.hasImpulse = false;
            target.hurtMarked = true;
            if (target.distanceToSqr(pose.x(), target.getY(), pose.z()) > 0.0001D) {
                target.teleportTo(pose.x(), target.getY(), pose.z());
            }
            target.setYRot(pose.yRot());
            target.setXRot(pose.xRot());
            target.setYHeadRot(pose.yHeadRot());
            target.setYBodyRot(pose.yBodyRot());
            target.yRotO = pose.yRot();
            target.xRotO = pose.xRot();
            target.yHeadRotO = pose.yHeadRot();
            target.yBodyRotO = pose.yBodyRot();
        } else {
            target.setDeltaMovement(0.0D, target.getDeltaMovement().y < 0 ? -0.05D : 0.0D, 0.0D);
            target.hasImpulse = false;
            target.hurtMarked = true;
        }

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.setZza(0.0F);
            mob.setXxa(0.0F);
            mob.setYya(0.0F);
            mob.setSpeed(0.0F);
            mob.setJumping(false);
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
            }
            if (!mob.isNoAi()) {
                mob.setNoAi(true);
            }
            if (!mob.getTags().contains(TAG_FROZEN_STATUE)) {
                mob.addTag(TAG_FROZEN_STATUE);
            }
        }
    }

    public static int getFrozenRemainingTicks(UUID id, long now) {
        int rem = 0;
        GaugeState fg = FROZEN_GAUGES.get(id);
        if (fg != null && !fg.isExpired()) {
            rem = fg.getRemainingTicks();
        }
        Long frozenEnd = FROZEN_UNTIL.get(id);
        if (frozenEnd != null && now < frozenEnd) {
            int endRem = (int) (frozenEnd - now);
            rem = rem > 0 ? Math.min(rem, endRem) : endRem;
        }
        return Math.max(0, rem);
    }

    public static void tickEntityFreezeGuard(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        UUID id = entity.getUUID();
        if (isEntityFrozen(entity)) {
            enforceFrozenPose(entity, id);
        } else if (entity instanceof Mob mob && (mob.getTags().contains(TAG_FROZEN_STATUE) || FROZEN_POSES.containsKey(id))) {
            clearFreezeState(entity, id);
        }
    }

    public static void clearFreezeState(LivingEntity target, UUID id) {
        boolean hadFreeze = FROZEN_GAUGES.remove(id) != null
                || FROZEN_UNTIL.remove(id) != null
                || PETRIFIED_UNTIL.remove(id) != null
                || FROZEN_POSES.remove(id) != null;
        LAST_FREEZE_STRUGGLE.remove(id);
        target.setTicksFrozen(0);
        MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 6) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        boolean hadTag = false;
        if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
            hadTag = true;
        }
        if ((hadFreeze || hadTag) && !target.level().isClientSide() && target.isAlive()) {
            syncAuras(target, getEntityElements(id), null, null, null, 0.0F);
        }
    }

    public static void handlePlayerFreezeStruggle(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) return;
        UUID id = player.getUUID();
        GaugeState fg = FROZEN_GAUGES.get(id);
        Long frozenEnd = FROZEN_UNTIL.get(id);
        long now = player.level().getGameTime();
        if (fg == null && (frozenEnd == null || now >= frozenEnd)) return;

        Long lastStruggle = LAST_FREEZE_STRUGGLE.get(id);
        if (lastStruggle != null && now >= lastStruggle && (now - lastStruggle) < 2L) {
            return; // Rate limit: max 1 struggle every 2 ticks (10 presses/sec)
        }
        LAST_FREEZE_STRUGGLE.put(id, now);

        if (fg != null) {
            fg.consume(0.35F);
        }
        long newEnd = (frozenEnd != null ? frozenEnd - 8L : now);
        if (fg != null && !fg.isExpired()) {
            newEnd = Math.min(newEnd, now + fg.getRemainingTicks());
        }
        FROZEN_UNTIL.put(id, newEnd);

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.9D, player.getZ(), 5, 0.25D, 0.35D, 0.25D, 0.03D);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, player.getX(), player.getY() + 0.9D, player.getZ(), 4, 0.2D, 0.3D, 0.2D, 0.04D);

        Map<Element, GaugeState> targetGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        if ((fg != null && fg.isExpired()) || now >= newEnd) {
            clearFreezeState(player, id);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.85F, 1.35F);
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL, player.getX(), player.getY() + 0.9D, player.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.06D);
            syncAuras(player, targetGauges.keySet(), null, null, null);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 0.65F, 1.55F + player.getRandom().nextFloat() * 0.3F);
            syncAuras(player, targetGauges.keySet(), null, null, null);
        }
    }
}
