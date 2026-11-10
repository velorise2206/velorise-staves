package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void velorise$onTurn(double yRot, double xRot, CallbackInfo ci) {
        if (((Entity) (Object) this).level().isClientSide()
                && ClientAuraRenderer.handleFrozenPlayerTurn((Entity) (Object) this, yRot, xRot)) {
            ci.cancel();
        }
    }
}
