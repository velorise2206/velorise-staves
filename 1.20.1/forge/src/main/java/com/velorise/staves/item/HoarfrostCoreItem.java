package com.velorise.staves.item;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.CryoRegisvineEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Summoning relic for the Cryo Regisvine World Boss. Right-click open ground to plant
 * a dormant Cryo Regisvine anchored at that location.
 */
public class HoarfrostCoreItem extends Item {

    public HoarfrostCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }
        for (int dy = 0; dy < 4; dy++) {
            if (!sl.isEmptyBlock(pos.above(dy))) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.velorise_staves.hoarfrost_core.blocked").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.FAIL;
            }
        }
        CryoRegisvineEntity boss = VeloriseStavesMod.getCryoRegisvineType().create(sl);
        if (boss == null) {
            return InteractionResult.FAIL;
        }
        Vec3 spawnPos = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        boss.setAnchor(spawnPos);
        if (player != null) {
            float yaw = player.getYRot() + 180.0F;
            boss.setYRot(yaw);
            boss.yBodyRot = yaw;
            boss.yHeadRot = yaw;
        }
        sl.addFreshEntity(boss);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, spawnPos.x, spawnPos.y + 1.0D, spawnPos.z, 40, 1.5D, 1.0D, 1.5D, 0.06D);
        sl.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3F, 0.85F);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.velorise_staves.hoarfrost_core").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
