package com.velorise.staves.mixin;

import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.NeuvilletteManager;
import com.velorise.staves.element.PlungeAndGliderManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the player's melee attack strength scale BEFORE vanilla Player.attack() resets
 * attackStrengthTicker to 0, blocks normal attacks while holding Guardian Lens or Plunging,
 * and hooks Player.actuallyHurt() for incoming-damage modifiers.
 */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void velorise$captureAttackStrengthPre(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (NeuvilletteManager.isHoldingGuardianLens(self) || PlungeAndGliderManager.isPlunging(self)) {
            ci.cancel();
            return;
        }
        if (!self.level().isClientSide()) {
            ElementalGaugeManager.recordPreAttackStrength(self, self.getAttackStrengthScale(0.5F));
            if (self instanceof net.minecraft.server.level.ServerPlayer sp && target instanceof net.minecraft.world.entity.LivingEntity) {
                ElementalGaugeManager.addMavuikaNormalAttackFightingSpirit(sp);
            }
        }
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void velorise$clearAttackStrengthPost(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) {
            ElementalGaugeManager.clearPreAttackStrength(self);
        }
    }

    @ModifyVariable(
            method = "actuallyHurt",
            at = @At(value = "STORE", ordinal = 0),
            argsOnly = true
    )
    private float velorise$modifyPlayerIncomingDamage(float amount, DamageSource source) {
        Player self = (Player) (Object) this;
        return ElementalGaugeManager.modifyFabricIncomingDamage(self, source, amount);
    }
}
