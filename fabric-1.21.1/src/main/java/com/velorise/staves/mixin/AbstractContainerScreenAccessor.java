package com.velorise.staves.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int velorise$getLeftPos();

    @Accessor("topPos")
    int velorise$getTopPos();

    @Accessor("imageWidth")
    int velorise$getImageWidth();
}
