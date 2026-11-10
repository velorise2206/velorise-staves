package com.velorise.staves.mixin;

import com.velorise.staves.client.OmniWheelOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void velorise$suppressTabOverlayDuringWheel(
            GuiGraphics guiGraphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci
    ) {
        if (OmniWheelOverlay.shouldSuppressTabList()) {
            ci.cancel();
        }
    }
}
