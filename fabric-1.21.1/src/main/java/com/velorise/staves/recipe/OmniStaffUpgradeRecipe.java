package com.velorise.staves.recipe;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Shapeless upgrade recipe: 1 non-Omni ArcaneStaffItem + 1 Omni Element -> Omni Staff
 * (or Netherite Omni Staff if the input staff is Netherite-tier), preserving all enchantments
 * and any custom Anvil name.
 */
public final class OmniStaffUpgradeRecipe extends CustomRecipe {
    public OmniStaffUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasStaff = false;
        boolean hasOmniElement = false;
        int totalItems = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack staffStack = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ArcaneStaffItem staff && staff.kind() != ArcaneStaffItem.Kind.OMNI) {
                staffStack = stack;
                break;
            }
        }
        if (staffStack.isEmpty()) return ItemStack.EMPTY;

        ArcaneStaffItem staff = (ArcaneStaffItem) staffStack.getItem();
        Item target = staff.isNetherite() ? VeloriseStavesMod.NETHERITE_OMNI_STAFF : VeloriseStavesMod.OMNI_STAFF;
        ItemStack result = new ItemStack(target);

        ItemEnchantments enchantments = staffStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            result.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        Component customName = staffStack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            result.set(DataComponents.CUSTOM_NAME, customName);
        }
        Integer repairCost = staffStack.get(DataComponents.REPAIR_COST);
        if (repairCost != null && repairCost > 0) {
            result.set(DataComponents.REPAIR_COST, repairCost);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VeloriseStavesMod.OMNI_STAFF_UPGRADE_SERIALIZER;
    }
}
