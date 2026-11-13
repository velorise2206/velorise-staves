package com.velorise.staves.mixin;

import com.velorise.staves.client.ClientAuraRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders 32x32 element item textures in GUI slots when enabled in Client Config, and renders
 * a miniature elemental emblem in the corner of Element-Imbued Swords and Vanilla Tridents.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void velorise$render32pxElementItem(
            @Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci
    ) {
        if (ClientAuraRenderer.tryRender32pxElementItem((GuiGraphics) (Object) this, stack, x, y)) {
            ci.cancel();
        }
    }

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
