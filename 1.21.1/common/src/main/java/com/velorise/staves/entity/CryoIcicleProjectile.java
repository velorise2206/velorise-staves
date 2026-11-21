package com.velorise.staves.entity;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.api.IcdPolicy;
import com.velorise.staves.api.VeloriseElementAPI;
import com.velorise.staves.element.Element;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight homing Cryo icicle launched by CryoRegisvineEntity during its homing_icicles attack.
 * Calls VeloriseElementAPI.dealElementalDamage directly on impact.
 */
public class CryoIcicleProjectile extends Entity {
    private @Nullable UUID ownerUuid;
    private @Nullable UUID targetUuid;
    private float damage = 7.0F;
    private int lifeTicks = 0;

    public CryoIcicleProjectile(EntityType<? extends CryoIcicleProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static CryoIcicleProjectile spawn(
            ServerLevel level,
            LivingEntity owner,
            @Nullable LivingEntity target,
            Vec3 origin,
            Vec3 initialVel,
            float damage
    ) {
        CryoIcicleProjectile proj = new CryoIcicleProjectile(VeloriseStavesMod.getCryoIcicleType(), level);
        proj.setPos(origin.x, origin.y, origin.z);
        proj.setDeltaMovement(initialVel);
        proj.ownerUuid = owner.getUUID();
        proj.targetUuid = target != null ? target.getUUID() : null;
        proj.damage = damage;
        level.addFreshEntity(proj);
        return proj;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;
        if (lifeTicks > 90) {
            discard();
            return;
        }

        Vec3 vel = getDeltaMovement();
        if (!level().isClientSide() && level() instanceof ServerLevel sl) {
            LivingEntity target = resolveLiving(sl, targetUuid);
            if (target != null && target.isAlive() && lifeTicks > 4 && lifeTicks < 60) {
                Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D).subtract(position());
                if (toTarget.lengthSqr() > 0.04D) {
                    Vec3 desired = toTarget.normalize().scale(0.68D);
                    vel = vel.lerp(desired, 0.12D);
                    if (vel.lengthSqr() > 1e-4D) {
                        vel = vel.normalize().scale(0.68D);
                    }
                    setDeltaMovement(vel);
                }
            }

            Vec3 next = position().add(vel);
            setPos(next.x, next.y, next.z);

            if (lifeTicks % 2 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 2, 0.06D, 0.06D, 0.06D, 0.01D);
            }

            if (!sl.isEmptyBlock(blockPosition()) && !sl.getBlockState(blockPosition()).getCollisionShape(sl, blockPosition()).isEmpty()) {
                sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY(), getZ(), 8, 0.15D, 0.15D, 0.15D, 0.05D);
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.55F, 1.7F);
                discard();
                return;
            }

            LivingEntity owner = resolveLiving(sl, ownerUuid);
            AABB box = getBoundingBox().inflate(0.45D);
            List<LivingEntity> hits = sl.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != owner && !(e instanceof CryoRegisvineEntity));
            if (!hits.isEmpty()) {
                LivingEntity hit = hits.get(0);
                VeloriseElementAPI.dealElementalDamage(hit, owner, Element.CRYO, damage, 1.0F, IcdPolicy.STANDARD);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, hit.getX(), hit.getY() + hit.getBbHeight() * 0.5D, hit.getZ(), 12, 0.25D, 0.25D, 0.25D, 0.05D);
                sl.playSound(null, hit.getX(), hit.getY(), hit.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.7F, 1.5F);
                discard();
            }
        } else {
            Vec3 next = position().add(vel);
            setPos(next.x, next.y, next.z);
        }
    }

    private static @Nullable LivingEntity resolveLiving(ServerLevel level, @Nullable UUID id) {
        if (id == null) return null;
        Entity e = level.getEntity(id);
        return e instanceof LivingEntity le ? le : null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
        if (tag.hasUUID("Target")) targetUuid = tag.getUUID("Target");
        if (tag.contains("Damage")) damage = tag.getFloat("Damage");
        lifeTicks = tag.getInt("LifeTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        if (targetUuid != null) tag.putUUID("Target", targetUuid);
        tag.putFloat("Damage", damage);
        tag.putInt("LifeTicks", lifeTicks);
    }
}
