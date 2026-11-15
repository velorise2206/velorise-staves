package com.velorise.staves.item;

import com.velorise.staves.element.PlungeAndGliderManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Wind Glider (Phong Chi Duc): Right-click once to permanently unlock the Wind Glider for the player
 * without occupying any armor/equipment slot. Once unlocked, press Space in mid-air to deploy/fold wings.
 */
public final class WindGliderItem extends Item {
    public WindGliderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            boolean alreadyUnlocked = PlungeAndGliderManager.isWindGliderUnlocked(sp);
            if (!alreadyUnlocked) {
                PlungeAndGliderManager.setWindGliderUnlocked(sp, true);
                if (!sp.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);
                level.playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 0.9F, 1.15F);
                sp.displayClientMessage(
                        Component.translatable("message.velorise_staves.wind_glider_unlocked").withStyle(ChatFormatting.AQUA),
                        true
                );
                sp.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.consume(stack);
            } else if (sp.isShiftKeyDown()) {
                PlungeAndGliderManager.setWindGliderUnlocked(sp, false);
                level.playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 0.75F, 0.85F);
                sp.displayClientMessage(
                        Component.translatable("message.velorise_staves.wind_glider_disabled").withStyle(ChatFormatting.YELLOW),
                        true
                );
                return InteractionResultHolder.sidedSuccess(stack, false);
            } else {
                sp.displayClientMessage(
                        Component.translatable("message.velorise_staves.wind_glider_already_unlocked").withStyle(ChatFormatting.AQUA),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level context, List<Component> tooltip, TooltipFlag flag) {
        ArcaneStaffItem.appendWrappedTooltip(tooltip, Component.translatable("tooltip.velorise_staves.wind_glider"), ChatFormatting.AQUA, 36, "  ");
        ArcaneStaffItem.appendWrappedTooltip(tooltip, Component.translatable("tooltip.velorise_staves.wind_glider.plunge"), ChatFormatting.GRAY, 36, "  ");
    }
}
