package com.velorise.staves.item;

import com.velorise.staves.element.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handles Anvil imbuing of Swords (including modded SwordItems) with elemental essences
 * and renders their imbued element badge on item tooltips.
 */
public final class SwordImbueHelper {
    public static final String TAG_IMBUED_ELEMENT = "VeloriseImbued";
    public static final String OMNI_KEY = "OMNI";

    public record AnvilResult(ItemStack output, int cost, int materialCost) {}

    private SwordImbueHelper() {}

    public static @Nullable String getImbuedKey(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        String val = tag.getString(TAG_IMBUED_ELEMENT);
        return (val == null || val.isEmpty()) ? null : val;
    }

    public static boolean isImbued(ItemStack stack) {
        return getImbuedKey(stack) != null;
    }

    public static boolean isOmniImbued(ItemStack stack) {
        return OMNI_KEY.equals(getImbuedKey(stack));
    }

    public static @Nullable Element getImbuedElement(ItemStack stack) {
        String key = getImbuedKey(stack);
        if (key == null || OMNI_KEY.equals(key)) return null;
        try {
            return Element.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static ItemStack createImbuedCopy(ItemStack swordStack, String imbueKey) {
        ItemStack result = swordStack.copy();
        result.setCount(1);
        result.getOrCreateTag().putString(TAG_IMBUED_ELEMENT, imbueKey);
        return result;
    }

    public static @Nullable AnvilResult tryBuildAnvilResult(ItemStack left, ItemStack right, @Nullable String itemName) {
        if (left.isEmpty() || right.isEmpty()) return null;
        if (!(left.getItem() instanceof SwordItem) || !(right.getItem() instanceof ElementItem elemItem)) return null;

        Element rightElem = elemItem.getElement();
        String targetKey = rightElem != null ? rightElem.name() : OMNI_KEY;
        if (targetKey.equals(getImbuedKey(left))) {
            return null;
        }

        ItemStack output = createImbuedCopy(left, targetKey);
        int xpCost = 3;

        if (itemName != null && !StringUtil.isNullOrEmpty(itemName) && !itemName.trim().isEmpty()) {
            if (!itemName.equals(left.getHoverName().getString())) {
                output.setHoverName(Component.literal(itemName));
                xpCost += 1;
            }
        } else if (left.hasCustomHoverName()) {
            output.resetHoverName();
            xpCost += 1;
        }

        return new AnvilResult(output, xpCost, 1);
    }

    public static void appendImbueTooltip(ItemStack stack, List<Component> tooltip) {
        String key = getImbuedKey(stack);
        if (key == null) return;

        Component badge;
        if (OMNI_KEY.equals(key)) {
            Component icon = Component.literal("\uE00A").withStyle(ChatFormatting.WHITE);
            Component elemName = Component.translatable("tooltip.velorise_staves.imbued.omni");
            badge = Component.empty().append(icon).append(Component.literal(" "))
                    .append(Component.translatable("tooltip.velorise_staves.imbued", elemName).withStyle(ChatFormatting.GOLD));
        } else {
            Element elem = getImbuedElement(stack);
            if (elem == null) return;
            Component icon = Component.literal(elem.iconString()).withStyle(ChatFormatting.WHITE);
            Component elemName = Component.translatable(elem.translationKey());
            badge = Component.empty().append(icon).append(Component.literal(" "))
                    .append(Component.translatable("tooltip.velorise_staves.imbued", elemName).withStyle(elem.color()));
        }

        Component hint = Component.translatable("tooltip.velorise_staves.imbued.hint").withStyle(ChatFormatting.DARK_GRAY);
        int insertAt = tooltip.isEmpty() ? 0 : 1;
        tooltip.add(insertAt, badge);
        tooltip.add(insertAt + 1, hint);
    }
}
