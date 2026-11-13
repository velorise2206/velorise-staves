package com.velorise.staves.mixin;

import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void velorise$onCompleteUsingItem(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel sl) {
            ElementalGaugeManager.handleItemUseFinish(sl, entity, entity.getUseItem());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void velorise$onLivingTickPost(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide()) {
            ElementalGaugeManager.tickEntityFreezeGuard(entity);
            if ((entity.getRemainingFireTicks() > 0 || entity.isOnFire())
                    && (ElementalGaugeManager.isPyroInnate(entity) || ElementalGaugeManager.isOmniInnate(entity))) {
                ElementalGaugeManager.extinguishPyroInnate(entity);
            }
        }
    }

    @ModifyVariable(
            method = "heal",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private float velorise$modifyIncomingHeal(float amount) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return com.velorise.staves.element.ElementalResonanceManager.modifyIncomingHeal(entity, amount);
    }

    @ModifyVariable(
            method = "actuallyHurt",
            at = @At(value = "STORE", ordinal = 0),
            argsOnly = true
    )
    private float velorise$modifyIncomingDamage(float amount, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return ElementalGaugeManager.modifyFabricIncomingDamage(entity, source, amount);
    }
}
