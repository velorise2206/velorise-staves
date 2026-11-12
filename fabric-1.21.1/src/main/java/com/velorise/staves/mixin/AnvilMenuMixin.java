package com.velorise.staves.mixin;

import com.velorise.staves.item.SwordImbueHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow private int repairItemCountCost;
    @Shadow private String itemName;
    @Shadow @Final private DataSlot cost;

    protected AnvilMenuMixin(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void velorise$allowImbuePickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);
        if (!this.resultSlots.getItem(0).isEmpty()
                && SwordImbueHelper.tryBuildAnvilResult(left, right, this.itemName) != null) {
            int xpCost = this.cost.get();
            cir.setReturnValue((player.getAbilities().instabuild || player.experienceLevel >= xpCost) && xpCost > 0);
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void velorise$handleSwordImbue(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);
        if (SwordImbueHelper.isFireAspectElementConflict(left, right)) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            this.repairItemCountCost = 0;
            this.broadcastChanges();
            ci.cancel();
            return;
        }
        SwordImbueHelper.AnvilResult res = SwordImbueHelper.tryBuildAnvilResult(left, right, this.itemName);
        if (res != null) {
            this.resultSlots.setItem(0, res.output());
            this.cost.set(res.cost());
            this.repairItemCountCost = res.materialCost();
            this.broadcastChanges();
            ci.cancel();
        }
    }
}
