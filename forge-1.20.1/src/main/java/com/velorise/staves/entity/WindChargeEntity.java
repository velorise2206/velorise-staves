package com.velorise.staves.entity;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.event.ArcaneStaffEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 1.20.1 custom Wind Charge projectile shared by Wind Charge item and Wind Staff.
 * Flies straight without gravity and bursts on impact with knockback and Anemo gauge.
 */
public class WindChargeEntity extends ThrowableItemProjectile {
    public WindChargeEntity(EntityType<? extends WindChargeEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public WindChargeEntity(Level level, LivingEntity owner) {
        super(VeloriseStavesMod.WIND_CHARGE_ENTITY.get(), owner, level);
        this.setNoGravity(true);
    }

    public WindChargeEntity(Level level, double x, double y, double z) {
        super(VeloriseStavesMod.WIND_CHARGE_ENTITY.get(), x, y, z, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return VeloriseStavesMod.WIND_CHARGE.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        } else if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide()) return;
        // Staves with TAG_KIND set are resolved by ArcaneStaffEvents.onProjectileImpact
        if (!this.getPersistentData().getString(ArcaneStaffEvents.TAG_KIND).isEmpty()) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 hit = result.getLocation();
            LivingEntity directHit = result instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living
                    ? living
                    : null;
            ArcaneStaffEvents.resolveWindBurst(serverLevel, hit, this.getOwner(), directHit, 0.0F, 1.0F, 2.5D, 0.95D);
        }
        this.discard();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
