package com.velorise.staves.mixin;

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
        if (this.player != null && this.player.getVehicle() instanceof FlamestriderBikeEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (this.player != null
                && com.velorise.staves.client.ClientAuraTracker.isRaidenMusouActive(this.player.getId())
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
        if (this.player != null && this.player.getVehicle() instanceof FlamestriderBikeEntity) {
            ci.cancel();
            return;
        }
        if (this.player != null
                && com.velorise.staves.client.ClientAuraTracker.isRaidenMusouActive(this.player.getId())
                && this.player.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem staff
                && staff.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            ci.cancel();
        }
    }
}
