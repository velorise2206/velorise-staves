package com.velorise.staves.recipe;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Shapeless upgrade recipe: 1 non-Omni ArcaneStaffItem + 1 Omni Element -> Omni Staff
 * (or Netherite Omni Staff if the input staff is Netherite-tier), preserving all enchantments
 * and any custom Anvil name.
 */
public final class OmniStaffUpgradeRecipe extends CustomRecipe {
    public OmniStaffUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        boolean hasStaff = false;
        boolean hasOmniElement = false;
        int totalItems = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            totalItems++;
            if (totalItems > 2) return false;

            if (stack.getItem() instanceof ArcaneStaffItem staff && staff.kind() != ArcaneStaffItem.Kind.OMNI) {
                if (hasStaff) return false;
                hasStaff = true;
            } else if (VeloriseStavesMod.isOmniElementItem(stack)) {
                if (hasOmniElement) return false;
                hasOmniElement = true;
            } else {
                return false;
            }
        }
        return totalItems == 2 && hasStaff && hasOmniElement;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registries) {
        ItemStack staffStack = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ArcaneStaffItem staff && staff.kind() != ArcaneStaffItem.Kind.OMNI) {
                staffStack = stack;
                break;
            }
        }
        if (staffStack.isEmpty()) return ItemStack.EMPTY;

        ArcaneStaffItem staff = (ArcaneStaffItem) staffStack.getItem();
        Item target = staff.isNetherite() ? VeloriseStavesMod.NETHERITE_OMNI_STAFF.get() : VeloriseStavesMod.OMNI_STAFF.get();
        ItemStack result = new ItemStack(target);

        var enchantments = EnchantmentHelper.getEnchantments(staffStack);
        if (!enchantments.isEmpty()) {
            EnchantmentHelper.setEnchantments(enchantments, result);
        }
        if (staffStack.hasCustomHoverName()) {
            result.setHoverName(staffStack.getHoverName());
        }
        int repairCost = staffStack.getBaseRepairCost();
        if (repairCost > 0) {
            result.setRepairCost(repairCost);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VeloriseStavesMod.OMNI_STAFF_UPGRADE_SERIALIZER.get();
    }
}
