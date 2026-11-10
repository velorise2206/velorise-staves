package com.velorise.staves.item;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Short-range blink teleportation tool powered by durability and cooldown. */
public class VoidTranslocatorItem extends Item {
    public double getEffectiveRange() {
        return StavesConfig.getVoidTranslocatorRange(isNetherite);
    }

    public int getCooldownTicks() {
        double seconds = StavesConfig.getVoidTranslocatorCooldownSeconds();
        double mult = StavesConfig.getCooldownMultiplier();
        return Math.max(1, (int) Math.round(seconds * 20.0D * mult));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return StavesConfig.getArtifactMaxDurability(true, isNetherite);
    }
    private final double range;
    private final boolean isNetherite;

    public VoidTranslocatorItem(int maxDurability) {
        this(maxDurability, 24.0D, false);
    }

    public VoidTranslocatorItem(int maxDurability, double range, boolean isNetherite) {
        super(isNetherite
            ? new Item.Properties().stacksTo(1).durability(maxDurability).fireResistant()
            : new Item.Properties().stacksTo(1).durability(maxDurability));
        this.range = range;
        this.isNetherite = isNetherite;
    }

    public boolean isNetherite() {
        return isNetherite;
    }

    public double getRange() {
        return range;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            Vec3 end = start.add(look.scale(getEffectiveRange()));
            BlockHitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 destination = resolveDestination(level, look, end, hitResult);

            if (destination == null) {
                player.displayClientMessage(Component.translatable("message.velorise_staves.translocator_blocked"), true);
                return InteractionResultHolder.fail(stack);
            }

            ServerLevel serverLevel = serverPlayer.serverLevel();
            spawnPortalParticles(serverLevel, player.position().add(0, 1.0, 0));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);

            player.teleportTo(destination.x, destination.y, destination.z);
            player.fallDistance = 0.0F;

            // Self-inflict Electro aura upon blink activation (1.0U Basic / 1.5U Netherite)
            float selfElectroU = isNetherite ? 1.5F : 1.0F;
            ElementalGaugeManager.applyElementBypassIcd(serverLevel, serverPlayer, Element.ELECTRO, selfElectroU, null, 0.0F);

            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            player.getCooldowns().addCooldown(this, getCooldownTicks());

            spawnPortalParticles(serverLevel, destination.add(0, 1.0, 0));
            level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);

            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static Vec3 resolveDestination(Level level, Vec3 look, Vec3 end, BlockHitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();
            BlockPos above = hitPos.above();
            if (isSafePosition(level, above)) return Vec3.atBottomCenterOf(above);
            Direction face = hitResult.getDirection();
            BlockPos adjacent = hitPos.relative(face);
            if (isSafePosition(level, adjacent)) return Vec3.atBottomCenterOf(adjacent);
            Vec3 fallback = hitResult.getLocation().subtract(look.scale(1.2D));
            return isSafePosition(level, BlockPos.containing(fallback)) ? fallback : null;
        }
        return isSafePosition(level, BlockPos.containing(end)) ? end : null;
    }

    private static boolean isSafePosition(Level level, BlockPos pos) {
        return !level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)
            && !level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above());
    }

    private static void spawnPortalParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 24, 0.4, 0.6, 0.4, 0.2);
    }

    @Override
    public int getEnchantmentValue() {
        return isNetherite ? 20 : 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        if (isNetherite && repair.is(Items.NETHERITE_INGOT)) return true;
        return repair.is(Items.ENDER_PEARL) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String subtitleKey = isNetherite ? "tooltip.velorise_staves.translocator_subtitle.netherite" : "tooltip.velorise_staves.translocator_subtitle";
        tooltip.add(Component.literal("✦ ").append(Component.translatable(subtitleKey)).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.velorise_staves.when_used").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.velorise_staves.range", (int) getEffectiveRange() + "m").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.velorise_staves.cooldown", String.format("%.1f", getCooldownTicks() / 20.0F)).withStyle(ChatFormatting.BLUE));

        if (flag.isAdvanced()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
