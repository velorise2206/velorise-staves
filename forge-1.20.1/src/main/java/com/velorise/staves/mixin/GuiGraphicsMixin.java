package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders a miniature elemental emblem in the corner of Element-Imbued Swords and Vanilla Tridents
 * across Inventory, Hotbar, and JEI/REI/EMI item slots.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL")
    )
    private void velorise$renderElementCornerIndicator(
            Font font, ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci
    ) {
        ClientAuraRenderer.renderItemElementCornerBadge((GuiGraphics) (Object) this, stack, x, y);
    }
}
