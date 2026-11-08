package com.velorise.staves.event;

import com.velorise.staves.manager.TemporaryWebManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Server-side impact and range resolution for Arcane Staves projectiles. */
public final class ArcaneStaffEvents {
    private static final String TAG_PREFIX_KIND = "velorise:kind:";
    private static final String TAG_PREFIX_RANGE = "velorise:range:";
    private static final String TAG_PREFIX_ORIGIN = "velorise:origin:";

    private ArcaneStaffEvents() {}

    public static void markKind(Projectile projectile, String kind) {
        projectile.addTag(TAG_PREFIX_KIND + kind);
    }

    public static void markRange(Projectile projectile, Vec3 origin, double maxRange) {
        projectile.addTag(TAG_PREFIX_ORIGIN + origin.x + ":" + origin.y + ":" + origin.z);
        projectile.addTag(TAG_PREFIX_RANGE + Math.max(1.0D, maxRange));
    }

    public static String getKind(Projectile projectile) {
        for (String tag : projectile.getTags()) {
            if (tag.startsWith(TAG_PREFIX_KIND)) {
                return tag.substring(TAG_PREFIX_KIND.length());
            }
        }
        return null;
    }

    public static void onProjectileTick(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        String kind = getKind(projectile);
        if (kind == null || kind.isEmpty()) return;
        if (isBeyondRange(projectile, projectile.position())) {
            level.sendParticles(ParticleTypes.SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.12D, 0.12D, 0.12D, 0.01D);
            projectile.discard();
        }
    }

    public static boolean onProjectileHit(Projectile projectile, HitResult hitResult) {
        if (!(projectile.level() instanceof ServerLevel level)) return false;
        String kind = getKind(projectile);
        if (kind == null || kind.isEmpty()) return false;

        Vec3 hit = hitResult.getLocation();
        if (isBeyondRange(projectile, hit)) {
            level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 8, 0.12D, 0.12D, 0.12D, 0.01D);
            projectile.discard();
            return true;
        }

        Entity owner = projectile.getOwner();
        boolean isNetherite = kind.startsWith("netherite_");
        String baseKind = isNetherite ? kind.substring("netherite_".length()) : kind;

        if ("ghastfire".equals(baseKind)) {
            resolveGhastfire(level, hit, owner, isNetherite);
            return false; // Allow vanilla LargeFireball explosion to run
        }

        switch (baseKind) {
            case "dragonfire" -> resolveDragonfire(level, hit, owner, isNetherite);
            case "inferno" -> resolveInferno(level, hit, owner, hitResult, isNetherite);
            case "wither" -> resolveWither(level, hit, owner, isNetherite);
            case "shulker" -> resolveShulker(level, hit, owner, hitResult, isNetherite);
            case "frost" -> resolveFrost(level, hit, owner, hitResult, isNetherite);
            case "web" -> resolveWeb(level, hit, owner, hitResult, isNetherite);
            default -> {}
        }
        projectile.discard();
        return true;
    }

    private static void resolveDragonfire(ServerLevel level, Vec3 hit, Entity owner, boolean isNetherite) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, hit.x, hit.y, hit.z);
        if (owner instanceof LivingEntity living) {
            cloud.setOwner(living);
        }
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        float radius = isNetherite ? 4.5F : 3.5F;
        cloud.setRadius(radius);
        cloud.setDuration(120); // 6 seconds
        cloud.setRadiusPerTick(((radius + 1.0F) - cloud.getRadius()) / (float) cloud.getDuration());
        cloud.setWaitTime(10);
        cloud.addEffect(new MobEffectInstance(MobEffects.HARM, 1, isNetherite ? 1 : 0));
        level.addFreshEntity(cloud);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.15F);
    }

    private static void resolveInferno(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        LivingEntity target = hitEntity(hitResult);
        if (target != null && target != owner) {
            target.igniteForSeconds(isNetherite ? 6 : 4);
            target.hurt(level.damageSources().magic(), isNetherite ? 8.0F : 6.0F);
        }
        level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, isNetherite ? 35 : 25, 0.3D, 0.3D, 0.3D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void resolveGhastfire(ServerLevel level, Vec3 hit, Entity owner, boolean isNetherite) {
        double radius = isNetherite ? 4.5D : 3.5D;
        float damage = isNetherite ? 16.0F : 12.0F;
        List<LivingEntity> affected = targets(level, hit, radius, owner);
        for (LivingEntity target : affected) {
            target.igniteForSeconds(isNetherite ? 6 : 5);
            target.hurt(level.damageSources().magic(), damage);
        }
        level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, isNetherite ? 45 : 35,
            radius * 0.35D, radius * 0.25D, radius * 0.35D, 0.05D);
    }

    private static void resolveWither(ServerLevel level, Vec3 hit, Entity owner, boolean isNetherite) {
        double radius = isNetherite ? 4.0D : 3.0D;
        float damage = isNetherite ? 10.0F : 8.0F;
        for (LivingEntity target : targets(level, hit, radius, owner)) {
            target.hurt(level.damageSources().magic(), damage);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, isNetherite ? 2 : 1));
        }
        level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, isNetherite ? 40 : 30,
            radius * 0.4D, radius * 0.35D, radius * 0.4D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void resolveShulker(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        LivingEntity target = hitEntity(hitResult);
        if (target != null && target != owner) {
            target.hurt(level.damageSources().magic(), isNetherite ? 10.0F : 8.0F);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, isNetherite ? 120 : 100, 0));
        }
        level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 16, 0.25D, 0.25D, 0.25D, 0.03D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void resolveFrost(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        LivingEntity target = hitEntity(hitResult);
        if (target != null && target != owner) {
            target.hurt(level.damageSources().freeze(), isNetherite ? 8.0F : 6.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, isNetherite ? 100 : 80, 1));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), isNetherite ? 180 : 140));
        }
        if (isNetherite) {
            for (LivingEntity splashTarget : targets(level, hit, 2.0D, owner)) {
                if (splashTarget != target) {
                    splashTarget.hurt(level.damageSources().freeze(), 4.0F);
                    splashTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    splashTarget.setTicksFrozen(100);
                }
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, hit.x, hit.y, hit.z, isNetherite ? 30 : 20, 0.3D, 0.3D, 0.3D, 0.025D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.65F, 1.6F);
    }

    private static void resolveWeb(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        BlockPos pos;
        if (hitResult instanceof EntityHitResult entityHit) {
            pos = entityHit.getEntity().blockPosition();
        } else if (hitResult instanceof BlockHitResult blockHit) {
            pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            pos = BlockPos.containing(hit);
        }
        int webTicks = isNetherite ? 140 : 100;
        int poisonAmplifier = isNetherite ? 1 : 0;
        java.util.UUID webOwner = owner instanceof ServerPlayer player ? player.getUUID() : null;
        if (!TemporaryWebManager.place(level, pos, webTicks, poisonAmplifier, webOwner)) {
            TemporaryWebManager.place(level, pos.above(), webTicks, poisonAmplifier, webOwner);
        }
        level.sendParticles(ParticleTypes.POOF, hit.x, hit.y, hit.z, 14, 0.25D, 0.25D, 0.25D, 0.02D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.7F, 1.5F);
    }

    public static void resolveLightning(ServerLevel level, Vec3 hit, ServerPlayer owner, boolean isNetherite) {
        double radius = isNetherite ? 4.5D : 3.5D;
        float damage = isNetherite ? 14.0F : 10.0F;
        for (LivingEntity target : targets(level, hit, radius, owner)) {
            target.igniteForSeconds(isNetherite ? 5 : 4);
            target.hurt(level.damageSources().lightningBolt(), damage);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y + 0.5D, hit.z, isNetherite ? 40 : 30,
            radius * 0.35D, 0.8D, radius * 0.35D, 0.08D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static LivingEntity hitEntity(HitResult hitResult) {
        return hitResult instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static List<LivingEntity> targets(ServerLevel level, Vec3 hit, double radius, Entity owner) {
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(hit, hit).inflate(radius),
            target -> target.isAlive() && target != owner && target.position().distanceToSqr(hit) <= radiusSqr);
    }

    private static boolean isBeyondRange(Projectile projectile, Vec3 hit) {
        double maxRange = -1.0D;
        Vec3 origin = null;
        for (String tag : projectile.getTags()) {
            if (tag.startsWith(TAG_PREFIX_RANGE)) {
                try {
                    maxRange = Double.parseDouble(tag.substring(TAG_PREFIX_RANGE.length()));
                } catch (NumberFormatException ignored) {}
            } else if (tag.startsWith(TAG_PREFIX_ORIGIN)) {
                String[] parts = tag.substring(TAG_PREFIX_ORIGIN.length()).split(":");
                if (parts.length == 3) {
                    try {
                        origin = new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (maxRange <= 0.0D || origin == null) return false;
        return origin.distanceToSqr(hit) > maxRange * maxRange;
    }
}
