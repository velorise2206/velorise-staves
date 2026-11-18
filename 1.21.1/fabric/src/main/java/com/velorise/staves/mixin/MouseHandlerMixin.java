package com.velorise.staves.mixin;

import com.velorise.staves.client.OmniWheelOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void velorise$onMousePress(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (this.minecraft != null
                && this.minecraft.getWindow().getWindow() == windowPointer
                && this.minecraft.screen == null
                && OmniWheelOverlay.handleMouseButtonPress(button, action)) {
            ci.cancel();
        }
    }
}
