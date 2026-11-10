package com.velorise.staves.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Physical 3D Dendro Core (Bloom Seed / Hyperbloom Core) matching Genshin Craft behavior:
 * gentle bobbing, slow Y-axis rotation, water buoyancy, and no ambient particles.
 */
public class DendroCoreEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_HYPERBLOOM =
            SynchedEntityData.defineId(DendroCoreEntity.class, EntityDataSerializers.BOOLEAN);

    public final float bobOffs = this.random.nextFloat() * (float) Math.PI * 2.0F;
    private int serverAgeTicks;

    public DendroCoreEntity(EntityType<? extends DendroCoreEntity> type, Level level) {
        super(type, level);
    }

    public DendroCoreEntity(
            EntityType<? extends DendroCoreEntity> type,
            Level level,
            double x,
            double y,
            double z
    ) {
        this(type, level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HYPERBLOOM, false);
    }

    public boolean isHyperbloomVisual() {
        return this.entityData.get(DATA_HYPERBLOOM);
    }

    public void setHyperbloomVisual(boolean value) {
        this.entityData.set(DATA_HYPERBLOOM, value);
        if (value) {
            this.setNoGravity(true);
            this.noPhysics = true;
        }
    }

    public int getAge() {
        return this.tickCount;
    }

    public int getServerAgeTicks() {
        return this.serverAgeTicks;
    }

    public void setServerAgeTicks(int serverAgeTicks) {
        this.serverAgeTicks = Math.max(0, serverAgeTicks);
    }

    public float getSpin(float partialTick) {
        float rate = isHyperbloomVisual() ? 6.0F : 20.0F;
        return (this.tickCount + partialTick) / rate + this.bobOffs;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.serverAgeTicks++;
        }

        if (isHyperbloomVisual()) {
            return;
        }

        if (this.isInWater()) {
            Vec3 vel = this.getDeltaMovement();
            this.setDeltaMovement(vel.x * 0.99D, vel.y + (vel.y < 0.06D ? 0.005D : 0.0D), vel.z * 0.99D);
        } else if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5D || (this.tickCount + this.getId()) % 4 == 0) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            float friction = 0.98F;
            if (this.onGround()) {
                BlockPos below = BlockPos.containing(this.getX(), this.getBoundingBox().minY - 0.15D, this.getZ());
                friction = this.level().getBlockState(below).getBlock().getFriction() * 0.98F;
            }
            this.setDeltaMovement(this.getDeltaMovement().multiply(friction, 0.98D, friction));
            if (this.onGround()) {
                Vec3 vel = this.getDeltaMovement();
                if (vel.y < 0.0D) {
                    this.setDeltaMovement(vel.multiply(1.0D, -0.5D, 1.0D));
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.serverAgeTicks = tag.getInt("AgeTicks");
        if (tag.contains("HyperbloomVisual")) {
            setHyperbloomVisual(tag.getBoolean("HyperbloomVisual"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("AgeTicks", this.serverAgeTicks);
        tag.putBoolean("HyperbloomVisual", isHyperbloomVisual());
    }
}
