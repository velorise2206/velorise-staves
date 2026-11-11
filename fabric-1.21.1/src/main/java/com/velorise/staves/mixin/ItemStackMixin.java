package com.velorise.staves.mixin;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.MagicMirrorItem;
import com.velorise.staves.item.VoidTranslocatorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void velorise$dynamicConfigMaxDamage(CallbackInfoReturnable<Integer> cir) {
        Item item = this.getItem();
        if (item instanceof ArcaneStaffItem staff) {
            cir.setReturnValue(StavesConfig.getMaxDurability(staff.kind(), staff.isNetherite()));
        } else if (item instanceof VoidTranslocatorItem translocator) {
            cir.setReturnValue(StavesConfig.getArtifactMaxDurability(true, translocator.isNetherite()));
        } else if (item instanceof MagicMirrorItem mirror) {
            cir.setReturnValue(StavesConfig.getArtifactMaxDurability(false, mirror.isNetherite()));
        }
    }
}
