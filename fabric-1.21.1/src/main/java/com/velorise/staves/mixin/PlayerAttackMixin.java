package com.velorise.staves.mixin;

import com.velorise.staves.element.ElementalGaugeManager;
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
 * attackStrengthTicker to 0, and hooks Player.actuallyHurt() so incoming-damage modifiers
 * (including imbued sword strikes, Crystallize shields, and Elemental Resonance) also run
 * when the target is a Player in PvP.
 */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void velorise$captureAttackStrengthPre(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
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
