package com.velorise.staves.mixin;

import com.velorise.staves.element.NeuvilletteManager;
import com.velorise.staves.element.PlungeAndGliderManager;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void velorise$suppressBikeStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player == null) return;
        if (this.player.getVehicle() instanceof FlamestriderBikeEntity) {
            cir.setReturnValue(false);
            return;
        }
        // Item 3: Block normal attack & block breaking when holding Neuvillette's Guardian Lens
        if (NeuvilletteManager.isHoldingGuardianLens(this.player)) {
            cir.setReturnValue(false);
            return;
        }
        // Item 4: Prioritize Universal Plunge Attack over vanilla Steve normal attack in mid-air
        if (PlungeAndGliderManager.canTriggerMidAirPlunge(this.player)) {
            com.velorise.staves.client.ClientAuraTracker.triggerStaffAnimation(
                    this.player.getId(), com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_START);
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                    com.velorise.staves.network.MavuikaActionPayload.ACTION_PLUNGE_ATTACK);
            cir.setReturnValue(false);
            return;
        }
        if (com.velorise.staves.client.ClientAuraTracker.isPlunging(this.player.getId())) {
            cir.setReturnValue(false);
            return;
        }
        if (com.velorise.staves.client.ClientAuraTracker.isRaidenMusouActive(this.player.getId())
                && this.player.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff
                && staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            if (com.velorise.staves.client.ClientAuraTracker.tryConsumeLocalRaidenSwing(false)) {
                com.velorise.staves.client.ClientAuraTracker.triggerStaffAnimation(
                        this.player.getId(), com.velorise.staves.client.ClientAuraTracker.ANIM_RAIDEN_SLASH);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new com.velorise.staves.network.RaidenSlashPayload(false));
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void velorise$suppressBikeContinueAttack(boolean leftClick, CallbackInfo ci) {
        if (this.player == null) return;
        if (this.player.getVehicle() instanceof FlamestriderBikeEntity
                || NeuvilletteManager.isHoldingGuardianLens(this.player)
                || com.velorise.staves.client.ClientAuraTracker.isPlunging(this.player.getId())) {
            ci.cancel();
            return;
        }
        if (com.velorise.staves.client.ClientAuraTracker.isRaidenMusouActive(this.player.getId())
                && this.player.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff
                && staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            ci.cancel();
        }
    }
}
