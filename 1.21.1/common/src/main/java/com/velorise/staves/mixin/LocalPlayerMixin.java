package com.velorise.staves.mixin;

import com.mojang.authlib.GameProfile;
import com.velorise.staves.client.ClientAuraTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ParCool!-style enforced movement hook on LocalPlayer.move().
 * Intercepts MoverType.SELF during wall climbing and 2-phase cubic-eased ledge mantling
 * so vanilla gravity and drag never fight wall-climb displacement (zero 20Hz jitter).
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    protected LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void velorise$enforceWallClimbMove(MoverType moverType, Vec3 movement, CallbackInfo ci) {
        if (moverType != MoverType.SELF) return;
        Vec3 enforced = ClientAuraTracker.getEnforcedWallClimbMove((LocalPlayer) (Object) this);
        if (enforced != null) {
            ci.cancel();
            this.setDeltaMovement(enforced);
            super.move(moverType, enforced);
        }
    }
}
