package com.velorise.staves.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * In-game interactive guidebook explaining the 7 elements, gauge units,
 * reaction formulas, environmental interactions, staves catalog, and server multipliers.
 */
public class ElementalCodexItem extends Item {
    public ElementalCodexItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(player, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9F, 1.0F);
        if (level.isClientSide()) {
            openClientCodexScreen();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static volatile java.lang.reflect.Method cachedOpenMethod;
    private static volatile boolean openMethodResolved;

    private static void openClientCodexScreen() {
        try {
            if (!openMethodResolved) {
                openMethodResolved = true;
                Class<?> cls = Class.forName("com.velorise.staves.client.ElementalCodexScreen");
                cachedOpenMethod = cls.getMethod("open");
            }
            java.lang.reflect.Method m = cachedOpenMethod;
            if (m != null) {
                m.invoke(null);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.velorise_staves.codex.subtitle").withStyle(ChatFormatting.GOLD));
        ArcaneStaffItem.appendWrappedTooltip(tooltip, Component.translatable("tooltip.velorise_staves.codex.desc1"), ChatFormatting.GRAY);
        ArcaneStaffItem.appendWrappedTooltip(tooltip, Component.translatable("tooltip.velorise_staves.codex.desc2"), ChatFormatting.GRAY);
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.velorise_staves.codex.use_hint").withStyle(ChatFormatting.AQUA));
    }
}
