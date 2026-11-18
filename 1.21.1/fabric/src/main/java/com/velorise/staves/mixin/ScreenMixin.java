package com.velorise.staves.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void velorise$beforeRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ClientAuraRenderer.setRenderingScreen(true);
        ClientAuraRenderer.clearPendingInventoryTooltip();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void velorise$afterRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ClientAuraRenderer.setRenderingScreen(false);
        if ((Object) this instanceof EffectRenderingInventoryScreen<?> screen && screen.canSeeEffects()) {
            ClientAuraRenderer.renderPendingInventoryTooltip(guiGraphics, mouseX, mouseY);
        } else {
            ClientAuraRenderer.clearPendingInventoryTooltip();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
