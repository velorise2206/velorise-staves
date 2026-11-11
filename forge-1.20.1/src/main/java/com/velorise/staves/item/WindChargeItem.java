package com.velorise.staves.item;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.WindChargeEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.20.1 backport of Minecraft's Wind Charge throwable item.
 * Launches a WindChargeEntity forward and bursts with knockback, wind-jump, and 1.0U Anemo.
 */
public final class WindChargeItem extends Item {
    public WindChargeItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                VeloriseStavesMod.WIND_CHARGE_THROW_SOUND.get(), SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            WindChargeEntity projectile = new WindChargeEntity(level, player);
            Vec3 look = player.getLookAngle().normalize();
            projectile.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            projectile.shoot(look.x, look.y, look.z, 1.5F, 1.0F);
            level.addFreshEntity(projectile);
        }

        player.getCooldowns().addCooldown(this, 10);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
