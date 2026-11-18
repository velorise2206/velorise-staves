package com.velorise.staves.enchantment;

import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Genshin Impact-inspired Elemental Mastery I–III enchantment for combat staves in Forge 1.20.1.
 * Amplifies Elemental Reaction damage:
 * - Amplifying Reactions (Vaporize, Melt, Overvape): +15% per level
 * - Transformative Reactions (Overloaded, Swirl, Shatter, Electro-Charged, Superconduct, Bloom, Hyperbloom, Burgeon, Burning): +30% per level
 * - Catalyze / Additive Reactions (Aggravate, Spread): +25% per level
 * - Crystallize Shields: +1.0 HP (0.5 golden heart) absorption per level
 */
public class ElementalMasteryEnchantment extends Enchantment {
    public ElementalMasteryEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return 40 + (level - 1) * 10;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof ArcaneStaffItem
                || stack.is(Items.BOOK)
                || stack.is(Items.ENCHANTED_BOOK);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem() instanceof ArcaneStaffItem || stack.is(Items.BOOK);
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }

    @Override
    public boolean isAllowedOnBooks() {
        return true;
    }
}
