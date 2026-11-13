package com.velorise.staves.item;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Recall artifact that channels and returns the player to their bed or spawn. */
public class MagicMirrorItem extends Item {
    public int getCooldownTicks() {
        double seconds = StavesConfig.getMagicMirrorCooldownSeconds();
        double mult = StavesConfig.getCooldownMultiplier();
        return Math.max(1, (int) Math.round(seconds * 20.0D * mult));
    }

    public int getUseDurationTicks() {
        return Math.max(1, (int) Math.round(StavesConfig.getMagicMirrorChannelSeconds() * 20.0D));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return StavesConfig.getArtifactMaxDurability(false, isNetherite);
    }
    private final boolean isNetherite;

    private record Target(ServerLevel level, Vec3 pos, float angle, boolean bedObstructed) {}

    public MagicMirrorItem(int maxDurability) {
        this(maxDurability, false);
    }

    public MagicMirrorItem(int maxDurability, boolean isNetherite) {
        super(isNetherite
            ? new Item.Properties().stacksTo(1).durability(maxDurability).fireResistant()
            : new Item.Properties().stacksTo(1).durability(maxDurability));
        this.isNetherite = isNetherite;
    }

    public boolean isNetherite() {
        return isNetherite;
    }

    private Target resolveTarget(ServerPlayer player) {
        boolean hadRespawn = player.getRespawnPosition() != null;
        DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        return new Target(
            transition.newLevel(), transition.pos(), transition.yRot(),
            hadRespawn && transition.missingRespawnBlock()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getUseDurationTicks();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (count % 3 == 0) player.swing(player.getUsedItemHand(), true);
            if (count % 5 == 0) {
                ((ServerLevel) player.level()).sendParticles(
                    ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.15
                );
            }
            if (count % 20 == 0) {
                player.playNotifySound(VeloriseStavesMod.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 0.4F, 1.5F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            Target target = resolveTarget(player);
            if (target == null || target.level == null) {
                player.displayClientMessage(Component.translatable("message.velorise_staves.mirror_no_spawn"), true);
                return stack;
            }

            boolean allowCrossDim = isNetherite || StavesConfig.BASIC_MIRROR_CROSS_DIMENSION.get();
            if (!allowCrossDim && player.serverLevel() != target.level) {
                player.displayClientMessage(Component.translatable("message.velorise_staves.mirror_dimension_blocked"), true);
                return stack;
            }

            ServerLevel departure = player.serverLevel();
            departure.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.5, 0.5, 0.5);
            departure.playSound(null, player.blockPosition(), VeloriseStavesMod.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            player.teleportTo(target.level, target.pos.x, target.pos.y, target.pos.z, target.angle, 0.0F);

            target.level.sendParticles(ParticleTypes.PORTAL, target.pos.x, target.pos.y + 1.0, target.pos.z, 30, 0.5, 0.5, 0.5, 0.5);
            target.level.playSound(null, target.pos.x, target.pos.y, target.pos.z, VeloriseStavesMod.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (target.bedObstructed) {
                player.displayClientMessage(Component.translatable("message.velorise_staves.mirror_no_spawn"), false);
            } else {
                player.displayClientMessage(Component.translatable("message.velorise_staves.mirror_teleport"), true);
            }

            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            com.velorise.staves.element.ElementalResonanceManager.applyCooldown(player, this, getCooldownTicks());
            com.velorise.staves.advancement.ModAdvancements.trigger(
                    player, com.velorise.staves.advancement.ModAdvancements.EVENT_SPATIAL_TRAVELER);
        }
        return stack;
    }

    @Override
    public int getEnchantmentValue() {
        return isNetherite ? 20 : 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        if (isNetherite && repair.is(Items.NETHERITE_INGOT)) return true;
        return repair.is(Items.ECHO_SHARD) || repair.is(Items.DIAMOND) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String subtitleKey = isNetherite ? "tooltip.velorise_staves.mirror_subtitle.netherite" : "tooltip.velorise_staves.mirror_subtitle";
        tooltip.add(Component.literal("✦ ").append(Component.translatable(subtitleKey)).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.velorise_staves.when_used").withStyle(ChatFormatting.GRAY));
        float cdSec = com.velorise.staves.element.ElementalResonanceManager.getClientAdjustedCooldownTicks(getCooldownTicks()) / 20.0F;
        tooltip.add(Component.translatable("tooltip.velorise_staves.cooldown", String.format("%.1f", cdSec)).withStyle(ChatFormatting.BLUE));

        if (flag.isAdvanced()) {
            tooltip.add(Component.empty());
            ArcaneStaffItem.appendWrappedTooltip(tooltip, Component.translatable(getDescriptionId() + ".desc"), ChatFormatting.DARK_GRAY);
        }
    }
}
