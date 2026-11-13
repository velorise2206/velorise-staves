package com.velorise.staves.item;

import com.velorise.staves.element.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        String val = data.copyTag().getString(TAG_IMBUED_ELEMENT);
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
        ItemStack result = swordStack.copyWithCount(1);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putString(TAG_IMBUED_ELEMENT, imbueKey));
        return result;
    }

    public static boolean hasFireAspect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return containsFireAspect(stack.getEnchantments())
                || containsFireAspect(stack.get(DataComponents.STORED_ENCHANTMENTS));
    }

    private static boolean containsFireAspect(@Nullable ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) return false;
        for (var entry : enchantments.entrySet()) {
            if (entry.getIntValue() > 0 && (entry.getKey().is(Enchantments.FIRE_ASPECT)
                    || entry.getKey().unwrapKey().map(k -> k.location().getPath().equals("fire_aspect")).orElse(false))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNonPyroImbued(ItemStack stack) {
        String key = getImbuedKey(stack);
        return key != null && !Element.PYRO.name().equals(key);
    }

    public static boolean isFireAspectElementConflict(ItemStack left, ItemStack right) {
        if (left == null || left.isEmpty() || right == null || right.isEmpty()) return false;
        if (left.getItem() instanceof SwordItem && right.getItem() instanceof ElementItem elemItem) {
            return hasFireAspect(left) && elemItem.getElement() != Element.PYRO;
        }
        if (left.getItem() instanceof SwordItem && hasFireAspect(right)) {
            return isNonPyroImbued(left);
        }
        if (right.getItem() instanceof SwordItem && hasFireAspect(left)) {
            return isNonPyroImbued(right);
        }
        return false;
    }

    public static @Nullable AnvilResult tryBuildAnvilResult(ItemStack left, ItemStack right, @Nullable String itemName) {
        if (left.isEmpty() || right.isEmpty()) return null;
        if (!(left.getItem() instanceof SwordItem) || !(right.getItem() instanceof ElementItem elemItem)) return null;

        Element rightElem = elemItem.getElement();
        if (hasFireAspect(left) && rightElem != Element.PYRO) {
            return null;
        }
        String targetKey = rightElem != null ? rightElem.name() : OMNI_KEY;
        if (targetKey.equals(getImbuedKey(left))) {
            return null;
        }

        ItemStack output = createImbuedCopy(left, targetKey);
        int xpCost = 3;

        if (itemName != null && !StringUtil.isBlank(itemName)) {
            if (!itemName.equals(left.getHoverName().getString())) {
                output.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
                xpCost += 1;
            }
        } else if (left.has(DataComponents.CUSTOM_NAME)) {
            output.remove(DataComponents.CUSTOM_NAME);
            xpCost += 1;
        }

        return new AnvilResult(output, xpCost, 1);
    }

    public static void appendImbueTooltip(ItemStack stack, List<Component> tooltip) {
        if (stack != null && !stack.isEmpty() && stack.is(net.minecraft.world.item.Items.TRIDENT)) {
            Component icon = Component.literal(Element.HYDRO.iconString()).withStyle(ChatFormatting.WHITE);
            Component elemName = Component.translatable(Element.HYDRO.translationKey());
            Component line = Component.empty()
                    .append(icon)
                    .append(Component.literal(" "))
                    .append(Component.translatable("tooltip.velorise_staves.trident_hydro", elemName).withStyle(ChatFormatting.AQUA));
            List<Component> wrappedHint = new ArrayList<>();
            ArcaneStaffItem.appendWrappedTooltip(wrappedHint,
                    Component.translatable("tooltip.velorise_staves.trident_hydro.hint"),
                    ChatFormatting.DARK_GRAY, 44, "  ");
            int insertAt = tooltip.isEmpty() ? 0 : 1;
            tooltip.add(insertAt, line);
            tooltip.addAll(insertAt + 1, wrappedHint);
            return;
        }
        String key = getImbuedKey(stack);
        if (key == null) return;

        boolean suppressed = isNonPyroImbued(stack) && hasFireAspect(stack);
        Component badge;
        if (OMNI_KEY.equals(key)) {
            Component icon = Component.literal("\uE00A").withStyle(ChatFormatting.WHITE);
            Component elemName = Component.translatable("tooltip.velorise_staves.imbued.omni");
            var namePart = Component.translatable("tooltip.velorise_staves.imbued", elemName).withStyle(suppressed ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD);
            if (suppressed) namePart = namePart.withStyle(ChatFormatting.STRIKETHROUGH);
            badge = Component.empty().append(icon).append(Component.literal(" ")).append(namePart);
        } else {
            Element elem = getImbuedElement(stack);
            if (elem == null) return;
            Component icon = Component.literal(elem.iconString()).withStyle(ChatFormatting.WHITE);
            Component elemName = Component.translatable(elem.translationKey());
            var namePart = Component.translatable("tooltip.velorise_staves.imbued", elemName).withStyle(suppressed ? ChatFormatting.DARK_GRAY : elem.color());
            if (suppressed) namePart = namePart.withStyle(ChatFormatting.STRIKETHROUGH);
            badge = Component.empty().append(icon).append(Component.literal(" ")).append(namePart);
        }

        List<Component> wrappedHint = new ArrayList<>();
        ArcaneStaffItem.appendWrappedTooltip(wrappedHint,
                Component.translatable("tooltip.velorise_staves.imbued.hint"),
                ChatFormatting.DARK_GRAY, 44, "  ");
        int insertAt = tooltip.isEmpty() ? 0 : 1;
        tooltip.add(insertAt, badge);
        tooltip.addAll(insertAt + 1, wrappedHint);
    }
}
