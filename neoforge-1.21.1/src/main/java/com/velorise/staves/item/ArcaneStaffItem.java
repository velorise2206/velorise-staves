package com.velorise.staves.item;

import com.velorise.staves.event.ArcaneStaffEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Standalone magic staves powered by vanilla durability and cooldowns. */
public final class ArcaneStaffItem extends Item {
    private static final String TAG_INFERNO_SHOTS = "ArcaneStaffInfernoShots";
    private static final String TAG_INFERNO_LAST = "ArcaneStaffInfernoLast";
    private static final String TAG_WEB_SHOTS = "ArcaneStaffWebShots";
    private static final String TAG_WEB_LAST = "ArcaneStaffWebLast";
    private static final String TAG_WITHER_SHOTS = "ArcaneStaffWitherShots";
    private static final String TAG_WITHER_LAST = "ArcaneStaffWitherLast";
    private static final String TAG_FROST_SHOTS = "ArcaneStaffFrostShots";
    private static final String TAG_FROST_LAST = "ArcaneStaffFrostLast";
    private static final int BURST_RESET_TICKS = 20;

    public enum Kind {
        DRAGONFIRE, ECHO, INFERNO, GHASTFIRE,
        WITHER, SHULKER, GUARDIAN, EVOKER, LIGHTNING, FROST, WEB
    }

    private final Kind kind;
    private final boolean isNetherite;

    public ArcaneStaffItem(Kind kind, int maxDurability) {
        this(kind, maxDurability, false);
    }

    public ArcaneStaffItem(Kind kind, int maxDurability, boolean isNetherite) {
        super(isNetherite
            ? new Item.Properties().stacksTo(1).durability(maxDurability).fireResistant()
            : new Item.Properties().stacksTo(1).durability(maxDurability));
        this.kind = kind;
        this.isNetherite = isNetherite;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isNetherite() {
        return isNetherite;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        if (isCharged()) {
            player.startUsingItem(hand);
            if (!level.isClientSide()) {
                level.playSound(null, player.blockPosition(),
                    kind == Kind.ECHO ? SoundEvents.WARDEN_SONIC_CHARGE : SoundEvents.GUARDIAN_AMBIENT,
                    SoundSource.PLAYERS, 0.8F, kind == Kind.ECHO ? 0.85F : 1.25F);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);

        if (isBurstKind()) {
            prepareBurst(serverPlayer, burstShotsTag(), burstLastTag(), BURST_RESET_TICKS);
        }

        boolean shouldConsumeDurability = !isBurstKind() || isFirstBurstShot(serverPlayer, burstShotsTag());

        boolean activated = switch (kind) {
            case DRAGONFIRE -> shootDragonfire(serverPlayer);
            case INFERNO -> shootInferno(serverPlayer);
            case GHASTFIRE -> shootGhastfire(serverPlayer);
            case WITHER -> shootWither(serverPlayer);
            case SHULKER -> shootShulker(serverPlayer);
            case EVOKER -> castFangs(serverPlayer);
            case LIGHTNING -> castLightning(serverPlayer);
            case FROST -> shootSnowball(serverPlayer, "frost", 1.45F);
            case WEB -> shootSnowball(serverPlayer, "web", 1.35F);
            case ECHO, GUARDIAN -> false;
        };

        if (!activated) return InteractionResultHolder.fail(stack);

        if (shouldConsumeDurability) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }

        if (isBurstKind()) {
            advanceBurst(serverPlayer, burstShotsTag(), burstLastTag(), 3, getCooldownTicks());
        } else {
            player.getCooldowns().addCooldown(this, getCooldownTicks());
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.swing(hand, true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !isBurstKind() || !(entity instanceof ServerPlayer player)) return;
        String tagShots = burstShotsTag();
        String tagLast = burstLastTag();
        int shots = player.getPersistentData().getInt(tagShots);
        if (shots <= 0) return;
        long elapsed = level.getGameTime() - player.getPersistentData().getLong(tagLast);
        if (elapsed > BURST_RESET_TICKS) {
            player.getPersistentData().remove(tagShots);
            player.getPersistentData().remove(tagLast);
            if (!player.getCooldowns().isOnCooldown(this)) {
                player.getCooldowns().addCooldown(this, getCooldownTicks());
            }
        }
    }

    private boolean isBurstKind() {
        return kind == Kind.INFERNO || kind == Kind.WEB || kind == Kind.WITHER || kind == Kind.FROST;
    }

    private String burstShotsTag() {
        return switch (kind) {
            case INFERNO -> TAG_INFERNO_SHOTS;
            case WEB -> TAG_WEB_SHOTS;
            case WITHER -> TAG_WITHER_SHOTS;
            case FROST -> TAG_FROST_SHOTS;
            default -> "";
        };
    }

    private String burstLastTag() {
        return switch (kind) {
            case INFERNO -> TAG_INFERNO_LAST;
            case WEB -> TAG_WEB_LAST;
            case WITHER -> TAG_WITHER_LAST;
            case FROST -> TAG_FROST_LAST;
            default -> "";
        };
    }

    private boolean isFirstBurstShot(ServerPlayer player, String tagShots) {
        return player.getPersistentData().getInt(tagShots) <= 0;
    }

    private boolean isCharged() {
        return kind == Kind.ECHO || kind == Kind.GUARDIAN;
    }

    private int getCooldownTicks() {
        return switch (kind) {
            case DRAGONFIRE -> 160; // 8.0s
            case ECHO -> 160;       // 8.0s
            case INFERNO -> 80;     // 4.0s
            case GHASTFIRE -> 100;  // 5.0s
            case WITHER -> 120;     // 6.0s
            case SHULKER -> 120;    // 6.0s
            case GUARDIAN -> 100;   // 5.0s
            case EVOKER -> 100;     // 5.0s
            case LIGHTNING -> 120;  // 6.0s
            case FROST -> 80;       // 4.0s
            case WEB -> 100;        // 5.0s
        };
    }

    private void prepareBurst(ServerPlayer player, String tagShots, String tagLast, int resetTicks) {
        int shots = player.getPersistentData().getInt(tagShots);
        if (shots <= 0) return;
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(tagLast);
        if (elapsed > resetTicks) {
            player.getPersistentData().remove(tagShots);
            player.getPersistentData().remove(tagLast);
        }
    }

    private void advanceBurst(ServerPlayer player, String tagShots, String tagLast, int maxShots, int cdTicks) {
        int shots = player.getPersistentData().getInt(tagShots) + 1;
        player.getPersistentData().putLong(tagLast, player.level().getGameTime());
        if (shots >= maxShots) {
            player.getPersistentData().remove(tagShots);
            player.getPersistentData().remove(tagLast);
            player.getCooldowns().addCooldown(this, cdTicks);
        } else {
            player.getPersistentData().putInt(tagShots, shots);
            player.getCooldowns().addCooldown(this, 4);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (kind == Kind.GUARDIAN) return 50;
        return isCharged() ? 30 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isCharged() ? UseAnim.BOW : UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!isCharged() || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (kind == Kind.GUARDIAN) {
            int elapsed = 50 - count;
            if (elapsed < 0 || elapsed % 5 != 0 || !(entity instanceof ServerPlayer player)) return;
            LivingEntity target = findTarget(player, isNetherite ? 28.0D : 24.0D, 1.2D);
            fireGuardian(player, target);
            if (elapsed > 0 && elapsed % 20 == 0) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
            return;
        }
        if (count % 5 != 0) return;
        Vec3 eye = entity.getEyePosition();
        serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP, eye.x, eye.y, eye.z, 2, 0.18D, 0.18D, 0.18D, 0.02D);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!isCharged() || level.isClientSide() || !(entity instanceof ServerPlayer player)) return stack;
        if (kind == Kind.GUARDIAN) {
            finishGuardianBeam(player);
            return stack;
        }
        fireEcho(player);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, getCooldownTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (kind == Kind.GUARDIAN && !level.isClientSide() && entity instanceof ServerPlayer player && timeLeft < 50) {
            finishGuardianBeam(player);
        }
    }

    private void finishGuardianBeam(ServerPlayer player) {
        player.getCooldowns().addCooldown(this, getCooldownTicks());
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private boolean shootDragonfire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        DragonFireball projectile = EntityType.DRAGON_FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_dragonfire" : "dragonfire", 32.0D, 1.35F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private boolean shootGhastfire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LargeFireball projectile = EntityType.FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_ghastfire" : "ghastfire", 36.0D, 1.2F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private boolean shootInferno(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        SmallFireball projectile = EntityType.SMALL_FIREBALL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_inferno" : "inferno", 30.0D, 1.5F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    private boolean shootWither(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WitherSkull projectile = EntityType.WITHER_SKULL.create(level);
        if (projectile == null) return false;
        prepareProjectile(projectile, player, isNetherite ? "netherite_wither" : "wither", 32.0D, 1.35F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.85F, 1.15F);
        return true;
    }

    private boolean shootShulker(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double range = 30.0D;
        LivingEntity target = findTarget(player, range, 1.5D);
        int shots = 2;
        int spawned = 0;
        String kindTag = isNetherite ? "netherite_shulker" : "shulker";
        for (int i = 0; i < shots; i++) {
            Vec3 direction = fanDirection(player.getLookAngle(), i, shots, 9.0F);
            Vec3 spawnPos = player.getEyePosition().add(direction.scale(0.8D));
            ShulkerBullet projectile;
            if (target != null) {
                projectile = new ShulkerBullet(level, player, target, Direction.Axis.Y);
                projectile.moveTo(spawnPos.x, spawnPos.y - 0.1D, spawnPos.z, player.getYRot(), player.getXRot());
                projectile.setDeltaMovement(direction.scale(0.45D));
                projectile.getPersistentData().putString(ArcaneStaffEvents.TAG_KIND, kindTag);
                ArcaneStaffEvents.markRange(projectile, player.position(), range);
            } else {
                projectile = new ShulkerBullet(EntityType.SHULKER_BULLET, level);
                prepareProjectile(projectile, player, kindTag, range, 1.25F, direction);
                projectile.setPos(spawnPos.x, spawnPos.y - 0.1D, spawnPos.z);
            }
            level.addFreshEntity(projectile);
            spawned++;
        }
        if (spawned <= 0) return false;
        level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 0.8F, 1.15F);
        return true;
    }

    private boolean shootSnowball(ServerPlayer player, String kind, float velocity) {
        ServerLevel level = player.serverLevel();
        Snowball projectile = EntityType.SNOWBALL.create(level);
        if (projectile == null) return false;
        String tag = isNetherite ? "netherite_" + kind : kind;
        prepareProjectile(projectile, player, tag, 28.0D, velocity);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(),
            "web".equals(kind) ? SoundEvents.SPIDER_AMBIENT : SoundEvents.SNOW_GOLEM_SHOOT,
            SoundSource.PLAYERS, 0.7F, "web".equals(kind) ? 1.45F : 1.1F);
        return true;
    }

    private static void prepareProjectile(Projectile projectile, ServerPlayer player, String kind, double range, float velocity) {
        prepareProjectile(projectile, player, kind, range, velocity, player.getLookAngle());
    }

    private static void prepareProjectile(Projectile projectile, ServerPlayer player, String kind, double range, float velocity, Vec3 direction) {
        Vec3 shot = direction.normalize();
        projectile.setOwner(player);
        projectile.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        projectile.shoot(shot.x, shot.y, shot.z, velocity, 0.0F);
        projectile.getPersistentData().putString(ArcaneStaffEvents.TAG_KIND, kind);
        ArcaneStaffEvents.markRange(projectile, player.position(), range);
    }

    private static Vec3 fanDirection(Vec3 look, int index, int count, float totalSpreadDegrees) {
        if (count <= 1) return look.normalize();
        float step = totalSpreadDegrees / (count - 1);
        float angle = -totalSpreadDegrees * 0.5F + step * index;
        return look.yRot((float)Math.toRadians(angle)).normalize();
    }

    private boolean castFangs(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = player.getLookAngle();
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 0.001D) flat = new Vec3(0.0D, 0.0D, 1.0D);
        flat = flat.normalize();
        int count = isNetherite ? 13 : 9;
        float yaw = (float)Math.atan2(flat.z, flat.x);

        for (int i = 0; i < count; i++) {
            double distance = 1.5D + i * 1.5D;
            double x = player.getX() + flat.x * distance;
            double z = player.getZ() + flat.z * distance;
            double y = findGroundY(level, x, player.getY(), z);
            level.addFreshEntity(new EvokerFangs(level, x, y, z, yaw, i * 2, player));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static double findGroundY(ServerLevel level, double x, double originY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos((int)Math.floor(x), (int)Math.floor(originY) + 2, (int)Math.floor(z));
        for (int i = 0; i < 7; i++) {
            BlockPos below = cursor.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                return below.getY() + 1.0D;
            }
            cursor.move(Direction.DOWN);
        }
        return originY;
    }

    private boolean castLightning(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double range = isNetherite ? 36.0D : 32.0D;
        LivingEntity target = findTarget(player, range, 1.3D);
        Vec3 impact = target != null ? target.getBoundingBox().getCenter() : rayEnd(player, range);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.setPos(impact.x, impact.y, impact.z);
            bolt.setCause(player);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        ArcaneStaffEvents.resolveLightning(level, impact, player, isNetherite);
        return true;
    }

    private static Vec3 rayEnd(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private void fireGuardian(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        double range = isNetherite ? 28.0D : 24.0D;
        Vec3 end = target == null ? rayEnd(player, range) : target.getBoundingBox().getCenter();
        if (target != null) {
            target.hurt(level.damageSources().magic(), isNetherite ? 3.5F : 2.5F);
        }
        drawBeam(level, start, end, ParticleTypes.ELECTRIC_SPARK, 0.45D);
        level.playSound(null, player.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.9F, 1.2F);
    }

    private void fireEcho(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double range = isNetherite ? 32.0D : 28.0D;
        double beamRadius = isNetherite ? 2.5D : 2.0D;
        AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(beamRadius + 1.0D);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
                target -> target.isAlive() && target != player)
            .stream()
            .filter(target -> distanceFromRay(start, direction, target.getBoundingBox().getCenter(), range)
                <= beamRadius + target.getBbWidth() * 0.5D)
            .sorted(Comparator.comparingDouble(target -> projection(start, direction, target.getBoundingBox().getCenter())))
            .toList();

        float damage = isNetherite ? 28.0F : 22.0F;
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().magic(), damage);
        }
        Vec3 end = start.add(direction.scale(range));
        drawBeam(level, start, end, ParticleTypes.SONIC_BOOM, 1.5D);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void drawBeam(ServerLevel level, Vec3 start, Vec3 end,
                                 net.minecraft.core.particles.SimpleParticleType particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 0.001D) return;
        Vec3 direction = delta.scale(1.0D / length);
        for (double d = 0.5D; d <= length; d += spacing) {
            Vec3 point = start.add(direction.scale(d));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static LivingEntity findTarget(ServerPlayer player, double range, double beamRadius) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(beamRadius + 2.0D);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                target -> target.isAlive() && target != player && player.hasLineOfSight(target))
            .stream()
            .filter(target -> distanceFromRay(start, direction, target.getBoundingBox().getCenter(), range)
                <= beamRadius + target.getBbWidth() * 0.5D)
            .min(Comparator.comparingDouble(target -> projection(start, direction, target.getBoundingBox().getCenter())))
            .orElse(null);
    }

    private static double projection(Vec3 start, Vec3 direction, Vec3 point) {
        return point.subtract(start).dot(direction);
    }

    private static double distanceFromRay(Vec3 start, Vec3 direction, Vec3 point, double range) {
        double projection = projection(start, direction, point);
        if (projection < 0.0D || projection > range) return Double.MAX_VALUE;
        return point.distanceTo(start.add(direction.scale(projection)));
    }

    @Override
    public int getEnchantmentValue() {
        return isNetherite ? 20 : 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        if (isNetherite && repair.is(Items.NETHERITE_INGOT)) return true;
        boolean validBase = switch (kind) {
            case DRAGONFIRE, ECHO, INFERNO, GHASTFIRE, WITHER, GUARDIAN -> repair.is(Items.BLAZE_ROD);
            case SHULKER -> repair.is(Items.END_ROD) || repair.is(Items.SHULKER_SHELL);
            case EVOKER, LIGHTNING, FROST, WEB -> repair.is(Items.BLAZE_ROD) || repair.is(Items.STICK);
        };
        return validBase || super.isValidRepairItem(toRepair, repair);
    }

    public String getRangeDisplay() {
        return switch (kind) {
            case DRAGONFIRE -> isNetherite ? "32m (AOE 4.5m)" : "32m (AOE 3.5m)";
            case ECHO -> isNetherite ? "32m" : "28m";
            case INFERNO -> "30m";
            case GHASTFIRE -> isNetherite ? "36m (AOE 4.5m)" : "36m (AOE 3.5m)";
            case WITHER -> isNetherite ? "32m (AOE 4.0m)" : "32m (AOE 3.0m)";
            case SHULKER -> "30m";
            case GUARDIAN -> isNetherite ? "28m" : "24m";
            case EVOKER -> isNetherite ? "19.5m (x13)" : "13.5m (x9)";
            case LIGHTNING -> isNetherite ? "36m (AOE 4.5m)" : "32m (AOE 3.5m)";
            case FROST -> isNetherite ? "28m (AOE 2.0m)" : "28m";
            case WEB -> "28m";
        };
    }

    public String getDamageDisplay() {
        return switch (kind) {
            case DRAGONFIRE -> isNetherite ? "Harm II (~12.0/s)" : "Harm I (~6.0/s)";
            case ECHO -> isNetherite ? "28.0" : "22.0";
            case INFERNO -> isNetherite ? "8.0 x 3 (24.0)" : "6.0 x 3 (18.0)";
            case GHASTFIRE -> isNetherite ? "16.0 + Blast" : "12.0 + Blast";
            case WITHER -> isNetherite ? "10.0 x 3 (30.0) + Wither III" : "8.0 x 3 (24.0) + Wither II";
            case SHULKER -> isNetherite ? "10.0 x 2 (20.0)" : "8.0 x 2 (16.0)";
            case GUARDIAN -> isNetherite ? "3.5 x 10 (35.0)" : "2.5 x 10 (25.0)";
            case EVOKER -> "6.0 / Fang";
            case LIGHTNING -> isNetherite ? "14.0" : "10.0";
            case FROST -> isNetherite ? "8.0 x 3 (24.0)" : "6.0 x 3 (18.0)";
            case WEB -> isNetherite ? "Poison II (Trapping)" : "Poison I (Trapping)";
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.velorise_staves.range", getRangeDisplay()));
        tooltip.add(Component.translatable("tooltip.velorise_staves.damage", getDamageDisplay()));
        tooltip.add(Component.translatable("tooltip.velorise_staves.cooldown", String.format("%.1f", getCooldownTicks() / 20.0F)));
    }
}
