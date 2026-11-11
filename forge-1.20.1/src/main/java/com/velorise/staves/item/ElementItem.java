package com.velorise.staves.item;

import com.velorise.staves.element.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure elemental essence dropped by innate/mutated mobs or synthesized into Omni Element.
 * Used for element crafting recipes, repair in an Anvil, upgrading staves to Omni Staff,
 * and imbuing swords with elemental auras.
 */
public final class ElementItem extends Item {
    private final @Nullable Element element;

    public ElementItem(@Nullable Element element) {
        super(new Item.Properties());
        this.element = element;
    }

    public @Nullable Element getElement() {
        return element;
    }

    public boolean isOmni() {
        return element == null;
    }

    @Override
    public Component getName(ItemStack stack) {
        ChatFormatting color = element != null ? element.color() : ChatFormatting.GOLD;
        return Component.translatable(this.getDescriptionId(stack)).withStyle(color);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return element == null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (element != null) {
            Component icon = Component.literal(element.iconString()).withStyle(ChatFormatting.WHITE);
            Component elemName = Component.translatable(element.translationKey()).withStyle(element.color());
            tooltip.add(Component.empty().append(icon).append(Component.literal(" ")).append(elemName));
            tooltip.add(Component.translatable("tooltip.velorise_staves.element_item.hint").withStyle(ChatFormatting.GRAY));
        } else {
            Component icon = Component.literal("\uE00A").withStyle(ChatFormatting.WHITE);
            Component elemName = Component.translatable("tooltip.velorise_staves.omni_element.tag").withStyle(ChatFormatting.GOLD);
            tooltip.add(Component.empty().append(icon).append(Component.literal(" ")).append(elemName));
            tooltip.add(Component.translatable("tooltip.velorise_staves.omni_element_item.hint").withStyle(ChatFormatting.GRAY));
        }
    }
}
