package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void velorise$lockFrozenInput(boolean isSneaking, float sneakSlowDownFactor, CallbackInfo ci) {
        if (ClientAuraRenderer.isLocalPlayerFrozen()) {
            this.forwardImpulse = 0.0F;
            this.leftImpulse = 0.0F;
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.jumping = false;
            this.shiftKeyDown = false;
        }
    }
}
