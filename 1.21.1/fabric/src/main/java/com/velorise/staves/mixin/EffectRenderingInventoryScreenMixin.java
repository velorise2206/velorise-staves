package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders custom inventory aura/shield/resonance effect boxes cleanly after vanilla renderEffects
 * and strictly before item tooltips draw at Z=400.
 */
@Mixin(EffectRenderingInventoryScreen.class)
public abstract class EffectRenderingInventoryScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void velorise$renderInventoryAuraAfterEffects(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        EffectRenderingInventoryScreen<?> screen = (EffectRenderingInventoryScreen<?>) (Object) this;
        if (screen.canSeeEffects()) {
            AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
            ClientAuraRenderer.renderInventoryAura(
                    guiGraphics,
                    screen,
                    accessor.velorise$getLeftPos(),
                    accessor.velorise$getTopPos(),
                    accessor.velorise$getImageWidth(),
                    mouseX,
                    mouseY
            );
        }
    }
}
