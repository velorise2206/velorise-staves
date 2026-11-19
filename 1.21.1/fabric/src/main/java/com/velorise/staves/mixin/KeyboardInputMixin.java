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
        } else {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && com.velorise.staves.client.ClientAuraTracker.isUltiCastActive(mc.player.getId())) {
                this.shiftKeyDown = false;
                mc.options.keyShift.setDown(false);
                mc.player.setShiftKeyDown(false);
                mc.player.setSprinting(false);
                mc.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                this.forwardImpulse = 0.0F;
                this.leftImpulse = 0.0F;
                this.up = false;
                this.down = false;
                this.left = false;
                this.right = false;
                this.jumping = false;
            } else if (mc.player != null && com.velorise.staves.client.ClientAuraTracker.isLocalWallClimbing()) {
                this.forwardImpulse = 0.0F;
                this.leftImpulse = 0.0F;
                this.up = false;
                this.down = false;
                this.left = false;
                this.right = false;
                this.jumping = false;
            }
        }
    }
}
