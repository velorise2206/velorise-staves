package com.velorise.staves.mixin;

import com.velorise.staves.element.CrystallizeShardManager;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @ModifyVariable(
            method = "actuallyHurt",
            at = @At(value = "STORE", ordinal = 0),
            argsOnly = true
    )
    private float velorise$modifyIncomingDamage(float amount, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() || amount <= 0.0F) return amount;
        long now = entity.level().getGameTime();

        if (entity instanceof ServerPlayer sp) {
            Element activeShield = CrystallizeShardManager.getActiveShield(sp, now);
            if (activeShield != null && CrystallizeShardManager.matchesElementalShield(activeShield, source)) {
                amount *= 0.4F;
                sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.8F);
            }
        }

        if (ElementalGaugeManager.hasSuperconductDebuff(entity, now) && ElementalGaugeManager.isPhysicalDamage(source)) {
            amount *= 1.4F;
            if (entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.05D);
            }
        }
        return amount;
    }
}
