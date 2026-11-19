package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.SwordImbueHelper;
import com.velorise.staves.network.ModNetwork;
import com.velorise.staves.fsm.MovementActionState;
import com.velorise.staves.mixin.ServerGamePacketListenerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Universal Plunge Attack and Permanent Wind Glider on Fabric 1.21.1.
 */
public final class PlungeAndGliderManager {
    public static final String TAG_WIND_GLIDER_UNLOCKED = "VeloriseWindGliderUnlocked";
    public static final String TAG_STAMINA_NETHER_UNLOCKED = "VeloriseStaminaNether";
    public static final String TAG_STAMINA_END_UNLOCKED = "VeloriseStaminaEnd";

    private static final double HIGH_PLUNGE_HEIGHT_THRESHOLD = 2.4D;
    private static final long MID_AIR_PLUNGE_INTERVAL_TICKS = 6L;
    private static final double MID_AIR_PLUNGE_RADIUS = 1.0D;
    private static final float MID_AIR_PLUNGE_MULT = 0.64F;
    private static final float LOW_PLUNGE_MULT = 1.28F;
    private static final float HIGH_PLUNGE_MULT = 1.60F;
    private static final double LOW_PLUNGE_RADIUS = 3.0D;
    private static final double HIGH_PLUNGE_RADIUS = 5.0D;
    private static final double LOW_PLUNGE_KNOCKBACK = 0.25D;
    private static final double HIGH_PLUNGE_KNOCKBACK = 0.55D;
    private static final double HIGH_PLUNGE_LIFT = 0.22D;

    private static final ThreadLocal<Boolean> APPLYING_PLUNGE_FALL_DAMAGE = ThreadLocal.withInitial(() -> Boolean.FALSE);


    public static final class AirState {
        public boolean gliding = false;
        public long gliderOpenedTick = 0L;
        public boolean plunging = false;
        public float plungeStartFallDist = 0.0F;
        public double plungeStartY = 0.0D;
        public long plungeStartedTick = 0L;
        public long plungeLandedTick = -100L;
        public long lastMidAirHitTick = -100L;
        public boolean wallClimbing = false;
        public MovementActionState actionState = MovementActionState.GROUNDED;
        public int stateElapsedTicks = 0;
        public Direction wallNormalFace = null;
        public long wallJumpGraceUntilTick = 0L;
        public int wallLostTicks = 0;
        public boolean exhaustedClimbFall = false;
    }

    private static final Map<UUID, AirState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_DASH_TICK = new ConcurrentHashMap<>();

    private PlungeAndGliderManager() {}

    public static void clearAll() {
        STATES.clear();
        LAST_DASH_TICK.clear();
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            STATES.remove(playerId);
            LAST_DASH_TICK.remove(playerId);
        }
    }

    public static boolean isPlunging(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            return com.velorise.staves.client.ClientAuraTracker.isPlunging(player.getId());
        }
        AirState st = STATES.get(player.getUUID());
        return st != null && st.plunging;
    }

    public static boolean isWindGliderUnlocked(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            return com.velorise.staves.client.ClientAuraTracker.localWindGliderUnlocked;
        }
        return player.getTags().contains(TAG_WIND_GLIDER_UNLOCKED);
    }

    public static void setWindGliderUnlocked(ServerPlayer player, boolean unlocked) {
        if (player == null) return;
        if (unlocked) {
            player.addTag(TAG_WIND_GLIDER_UNLOCKED);
        } else {
            player.removeTag(TAG_WIND_GLIDER_UNLOCKED);
        }
        if (!unlocked) {
            AirState st = STATES.get(player.getUUID());
            if (st != null && st.gliding) {
                st.gliding = false;
                ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_STOP);
            }
        }
        ModNetwork.sendStaffAnimation(player, unlocked
                ? com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_UNLOCKED
                : com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_LOCKED);
    }

    public static void copyPersistentState(Player oldPlayer, Player newPlayer) {
        if (oldPlayer == null || newPlayer == null) return;
        if (oldPlayer.getTags().contains("VeloriseFirstZhongliBurstDone")) {
            newPlayer.addTag("VeloriseFirstZhongliBurstDone");
        }
        if (oldPlayer.getTags().contains(TAG_WIND_GLIDER_UNLOCKED)) {
            newPlayer.addTag(TAG_WIND_GLIDER_UNLOCKED);
        }
        if (oldPlayer.getTags().contains(TAG_STAMINA_NETHER_UNLOCKED)) {
            newPlayer.addTag(TAG_STAMINA_NETHER_UNLOCKED);
        }
        if (oldPlayer.getTags().contains(TAG_STAMINA_END_UNLOCKED)) {
            newPlayer.addTag(TAG_STAMINA_END_UNLOCKED);
        }
    }

    public static void checkStaminaMilestones(ServerPlayer player) {
        if (player == null || player.serverLevel().getServer() == null) return;
        ResourceKey<Level> dim = player.serverLevel().dimension();
        var tags = player.getTags();
        if (dim == Level.NETHER && !tags.contains(TAG_STAMINA_NETHER_UNLOCKED)) {
            player.addTag(TAG_STAMINA_NETHER_UNLOCKED);
            ModNetwork.sendStaffAnimation(player, tags.contains(TAG_STAMINA_END_UNLOCKED)
                    ? com.velorise.staves.client.ClientAuraTracker.ANIM_STAMINA_TIER_240
                    : com.velorise.staves.client.ClientAuraTracker.ANIM_STAMINA_TIER_170);
            if (StavesConfig.isEnableCustomStamina()) {
                player.displayClientMessage(Component.translatable("message.velorise_staves.stamina_nether_unlocked"), true);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);
            }
        } else if (dim == Level.END && !tags.contains(TAG_STAMINA_END_UNLOCKED)) {
            player.addTag(TAG_STAMINA_NETHER_UNLOCKED);
            player.addTag(TAG_STAMINA_END_UNLOCKED);
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_STAMINA_TIER_240);
            if (StavesConfig.isEnableCustomStamina()) {
                player.displayClientMessage(Component.translatable("message.velorise_staves.stamina_end_unlocked"), true);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.55F);
            }
        }
    }

    public static void syncPlayerOnJoinOrRespawn(ServerPlayer player) {
        if (player == null) return;
        // Clear any stale NoGravity flag persisted in player NBT from earlier wall-climb builds
        if (player.isNoGravity()) {
            player.setNoGravity(false);
        }
        checkStaminaMilestones(player);
        var tags = player.getTags();
        boolean unlocked = tags.contains(TAG_WIND_GLIDER_UNLOCKED);
        ModNetwork.sendStaffAnimation(player, unlocked
                ? com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_UNLOCKED
                : com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_LOCKED);
        int tierAnim = tags.contains(TAG_STAMINA_END_UNLOCKED)
                ? com.velorise.staves.client.ClientAuraTracker.ANIM_STAMINA_TIER_240
                : (tags.contains(TAG_STAMINA_NETHER_UNLOCKED)
                        ? com.velorise.staves.client.ClientAuraTracker.ANIM_STAMINA_TIER_170
                        : com.velorise.staves.client.ClientAuraTracker.ANIM_STAMINA_TIER_100);
        ModNetwork.sendStaffAnimation(player, tierAnim);
    }

    public static void handlePlayerDash(ServerPlayer player) {
        if (!StavesConfig.isEnableCustomDash() || player == null || !player.isAlive() || player.isSpectator()) return;
        if (player.getVehicle() != null || player.getAbilities().flying) return;
        ServerLevel level = player.serverLevel();
        if (level.getServer() == null) return;
        long now = level.getServer().overworld().getGameTime();
        long prev = LAST_DASH_TICK.getOrDefault(player.getUUID(), -100L);
        if (now - prev < 2L) return;
        LAST_DASH_TICK.put(player.getUUID(), now);

        ArchonUltiManager.grantUltiIframe(player, 7);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_DASH_IMPULSE);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.45F, 1.55F);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.15D, player.getZ(),
                4, 0.22D, 0.08D, 0.22D, 0.04D);
    }

    public static boolean canHoldPlungeWeapon(Player player) {
        if (player == null || NeuvilletteManager.isHoldingGuardianLens(player)) return false;
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof ArcaneStaffItem || main.getItem() instanceof SwordItem;
    }

    public static boolean isHandBlockingGliderRightClick(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof com.velorise.staves.item.WindGliderItem) {
            return !isWindGliderUnlocked(player);
        }
        if (item instanceof ArcaneStaffItem) {
            if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.SHULKER) {
                return true;
            }
            return !player.getCooldowns().isOnCooldown(item) && !ElementalResonanceManager.isOverloaded(player);
        }
        if (item instanceof com.velorise.staves.item.MagicMirrorItem
                || item instanceof com.velorise.staves.item.VoidTranslocatorItem
                || item instanceof com.velorise.staves.item.ElementalCodexItem) {
            return !player.getCooldowns().isOnCooldown(item);
        }
        if (stack.getUseAnimation() != net.minecraft.world.item.UseAnim.NONE) {
            return true;
        }
        if (item == net.minecraft.world.item.Items.ENDER_PEARL
                || item instanceof net.minecraft.world.item.SnowballItem
                || item instanceof net.minecraft.world.item.EggItem
                || item instanceof net.minecraft.world.item.ExperienceBottleItem
                || item instanceof net.minecraft.world.item.ThrowablePotionItem
                || item instanceof net.minecraft.world.item.FishingRodItem
                || item instanceof net.minecraft.world.item.FireworkRocketItem
                || item instanceof net.minecraft.world.item.SpawnEggItem
                || item instanceof net.minecraft.world.item.BucketItem) {
            return true;
        }
        return false;
    }

    public static boolean canRightClickToggleGlider(Player player) {
        if (!StavesConfig.isEnableWindGlider()) return false;
        if (player == null || !player.isAlive() || player.isSpectator()) return false;
        if (!isWindGliderUnlocked(player)) return false;
        if (player.onGround() || player.isInWater() || player.isInLava()
                || player.onClimbable() || player.getVehicle() != null || player.getAbilities().flying) {
            return false;
        }
        if (isPlunging(player) || player.isUsingItem()) return false;
        if (isHandBlockingGliderRightClick(player, player.getMainHandItem())) return false;
        if (isHandBlockingGliderRightClick(player, player.getOffhandItem())) return false;
        return true;
    }

    public static boolean hasClearAirBeneath(Player player, double minClearBlocks) {
        if (player == null || player.onGround() || player.isInWater() || player.isInLava()) {
            return false;
        }
        BlockPos basePos = player.blockPosition();
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos checkPos = basePos.below(dy);
            if (player.level().getFluidState(checkPos).is(net.minecraft.tags.FluidTags.WATER)) {
                double waterSurfaceY = checkPos.getY() + 1.0D;
                double distToWater = player.getY() - waterSurfaceY;
                if (distToWater < 1.80D) {
                    return false;
                }
                break;
            }
            if (!player.level().getBlockState(checkPos).getCollisionShape(player.level(), checkPos).isEmpty()) {
                break;
            }
        }
        if (player.fallDistance >= (float) minClearBlocks) return true;
        AABB check = player.getBoundingBox().expandTowards(0.0D, -minClearBlocks, 0.0D);
        return player.level().noCollision(player, check);
    }

    public static boolean canTriggerMidAirPlunge(Player player) {
        if (!StavesConfig.isEnablePlungeAttack()) return false;
        if (player == null || !player.isAlive() || player.isSpectator()) return false;
        if (player.getVehicle() != null || player.onGround() || player.isInWater() || player.isInLava() || player.getAbilities().flying) {
            return false;
        }
        if (!canHoldPlungeWeapon(player)) return false;
        if (player.level().isClientSide()) {
            if (com.velorise.staves.client.ClientAuraTracker.isPlunging(player.getId())) return false;
            if (com.velorise.staves.client.ClientAuraTracker.isSteveWallClimbing(player.getId())) return false;
            if (com.velorise.staves.client.ClientAuraTracker.isLocalExhaustedClimbFall()) return false;
            if (com.velorise.staves.client.ClientAuraTracker.isGliding(player.getId())) return true;
            double minClear = com.velorise.staves.client.ClientAuraTracker.isXilonenSkating(player.getId()) ? 1.10D : 1.50D;
            return player.fallDistance >= 1.50F || hasClearAirBeneath(player, minClear);
        }
        AirState st = STATES.get(player.getUUID());
        if (st != null && (st.plunging || st.wallClimbing || st.exhaustedClimbFall)) return false;
        if (st != null && !MovementActionState.canTransition(st.actionState, MovementActionState.PLUNGE_DIVE, st.stateElapsedTicks)) return false;
        if (st != null && st.gliding) return true;
        double minClear = XilonenManager.isSkating(player) ? 1.10D : 1.50D;
        return player.fallDistance >= 1.50F || hasClearAirBeneath(player, minClear);
    }

    public static void handleToggleGlider(ServerPlayer player) {
        if (!StavesConfig.isEnableWindGlider()) return;
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        if (!isWindGliderUnlocked(player)) return;
        if (player.getVehicle() != null || player.isInWater() || player.isInLava()) {
            return;
        }
        // Cleanly separate from Creative flight and Elytra fall-flying conflicts
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }

        AirState st = STATES.computeIfAbsent(player.getUUID(), k -> new AirState());
        if (st.plunging || st.wallClimbing || st.exhaustedClimbFall) return;

        if (st.gliding) {
            st.gliding = false;
            if (st.actionState == MovementActionState.GLIDING) {
                st.actionState = MovementActionState.GROUNDED;
                st.stateElapsedTicks = 0;
            }
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_STOP);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 0.65F, 0.90F);
        } else {
            if (!MovementActionState.canTransition(st.actionState, MovementActionState.GLIDING, st.stateElapsedTicks)) return;
            if (st.wallClimbing) {
                exitServerWallClimb(player, st, false);
            }
            st.gliding = true;
            st.actionState = MovementActionState.GLIDING;
            st.stateElapsedTicks = 0;
            st.gliderOpenedTick = player.level().getGameTime();
            player.setOnGround(false);
            player.fallDistance = 0.0F;
            Vec3 dv = player.getDeltaMovement();
            player.setDeltaMovement(dv.x, Math.min(dv.y, -0.08D), dv.z);
            player.hasImpulse = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_START);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 0.85F, 1.25F);
        }
    }

    public static MovementActionState.MovementLayer resolveMovementLayer(Player player) {
        if (player == null) return MovementActionState.MovementLayer.UNIVERSAL_AIR_MOVE;
        if (player.getVehicle() instanceof FlamestriderBikeEntity || player.getVehicle() != null) {
            return MovementActionState.MovementLayer.MAVUIKA_BIKE;
        }
        if (XilonenManager.isSkating(player)) {
            return MovementActionState.MovementLayer.XILONEN_SKATE;
        }
        if (player.onClimbable()) {
            return MovementActionState.MovementLayer.VANILLA_CLIMBABLE;
        }
        return MovementActionState.MovementLayer.UNIVERSAL_AIR_MOVE;
    }

    private static void exitServerWallClimb(ServerPlayer player, AirState st, boolean exhausted) {
        if (player == null || st == null) return;
        boolean wasWall = st.wallClimbing || st.actionState.isWallState();
        st.wallClimbing = false;
        st.wallNormalFace = null;
        st.wallJumpGraceUntilTick = 0L;
        st.wallLostTicks = 0;
        if (exhausted) {
            st.exhaustedClimbFall = true;
            st.actionState = MovementActionState.EXHAUSTED_FALL;
            st.stateElapsedTicks = 0;
        } else if (st.actionState.isWallState()) {
            st.actionState = MovementActionState.GROUNDED;
            st.stateElapsedTicks = 0;
        }
        if (wasWall) {
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_STEVE_WALL_CLIMB_OFF);
        }
    }

    public static void handleWallClimbState(ServerPlayer player, boolean climbing) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        AirState st = STATES.computeIfAbsent(player.getUUID(), k -> new AirState());
        if (!climbing) {
            exitServerWallClimb(player, st, false);
            return;
        }
        if (!StavesConfig.isEnableWallClimb()
                || st.exhaustedClimbFall
                || st.plunging
                || player.isInWater()
                || player.isInLava()
                || player.getAbilities().flying
                || resolveMovementLayer(player) != MovementActionState.MovementLayer.UNIVERSAL_AIR_MOVE) {
            return;
        }
        Direction wall = WallClimbHelper.findClimbWall(player, 1.15D);
        if (wall == null) return;
        boolean alreadyClimbing = st.wallClimbing;
        if (st.gliding) {
            st.gliding = false;
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_STOP);
        }
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        st.wallClimbing = true;
        st.wallNormalFace = wall;
        st.wallLostTicks = 0;
        if (!alreadyClimbing) {
            st.actionState = MovementActionState.WALL_CLING;
            st.stateElapsedTicks = 0;
        }
        player.fallDistance = 0.0F;
        ((ServerGamePacketListenerAccessor) player.connection).velorise$setAboveGroundTickCount(0);
        if (!alreadyClimbing) {
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_STEVE_WALL_CLIMB_ON);
        }
    }

    public static void handleWallClimbJump(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        AirState st = STATES.get(player.getUUID());
        if (st == null || !st.wallClimbing) return;
        long now = player.level().getGameTime();
        st.actionState = MovementActionState.WALL_JUMP;
        st.stateElapsedTicks = 0;
        st.wallJumpGraceUntilTick = now + MovementActionState.WALL_JUMP_DURATION_TICKS + 4L;
        player.fallDistance = 0.0F;
        ((ServerGamePacketListenerAccessor) player.connection).velorise$setAboveGroundTickCount(0);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_STEVE_WALL_CLIMB_JUMP);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.35F, 1.45F);
    }

    public static void handleWallClimbExhausted(ServerPlayer player) {
        if (player == null) return;
        AirState st = STATES.computeIfAbsent(player.getUUID(), k -> new AirState());
        exitServerWallClimb(player, st, true);
    }

    public static void handleStartPlunge(ServerPlayer player) {
        if (!StavesConfig.isEnablePlungeAttack()) return;
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        if (player.getVehicle() instanceof FlamestriderBikeEntity) return;
        if (player.getVehicle() != null || player.onGround() || player.isInWater() || player.isInLava() || player.getAbilities().flying) {
            return;
        }
        if (!canHoldPlungeWeapon(player)) return;
        AirState st = STATES.computeIfAbsent(player.getUUID(), k -> new AirState());
        if (st.plunging) return;
        double minClear = XilonenManager.isSkating(player) ? 1.10D : 1.50D;
        if (!st.gliding && player.fallDistance < 1.50F && !hasClearAirBeneath(player, minClear)) return;

        if (st.gliding) {
            st.gliding = false;
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_STOP);
        }
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        st.plunging = true;
        st.actionState = MovementActionState.PLUNGE_DIVE;
        st.stateElapsedTicks = 0;
        st.plungeStartFallDist = player.fallDistance;
        st.plungeStartY = player.getY();
        st.plungeStartedTick = player.level().getGameTime();
        st.lastMidAirHitTick = -100L;

        player.fallDistance = 0.0F;
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        if (horiz.lengthSqr() > 1.0E-4D) horiz = horiz.normalize();
        player.setDeltaMovement(horiz.x * 0.18D, -0.675D, horiz.z * 0.18D);
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.85F, 0.85F);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_START);
    }

    public static boolean isPlungeFallImmune(LivingEntity entity) {
        if (APPLYING_PLUNGE_FALL_DAMAGE.get()) return false;
        if (!(entity instanceof Player player)) return false;
        if (player.level().isClientSide()) {
            return com.velorise.staves.client.ClientAuraTracker.isGliding(player.getId())
                    || com.velorise.staves.client.ClientAuraTracker.isPlunging(player.getId());
        }
        AirState st = STATES.get(player.getUUID());
        if (st == null) return false;
        long now = player.level().getGameTime();
        return st.gliding || st.plunging
                || (now - st.plungeLandedTick) <= 20L
                || (now - st.plungeStartedTick) <= 20L;
    }

    public static float modifyPlungeOrGliderFallDamage(LivingEntity entity, float rawFallDamage) {
        if (APPLYING_PLUNGE_FALL_DAMAGE.get()) return rawFallDamage;
        if (isPlungeFallImmune(entity)) {
            entity.fallDistance = 0.0F;
            return 0.0F;
        }
        return rawFallDamage;
    }

    public static void tick(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            checkStaminaMilestones(player);
            if (now % 40L == 0L) {
                syncPlayerOnJoinOrRespawn(player);
            }
            AirState st = STATES.get(player.getUUID());
            if (st == null) continue;
            ServerLevel level = player.serverLevel();

            st.stateElapsedTicks++;
            if (player.onGround() || player.isInWater() || player.isInLava()) {
                st.exhaustedClimbFall = false;
                if (st.actionState == MovementActionState.EXHAUSTED_FALL) {
                    st.actionState = MovementActionState.GROUNDED;
                    st.stateElapsedTicks = 0;
                }
            }

            if (st.wallClimbing) {
                boolean invalidLayer = resolveMovementLayer(player) != MovementActionState.MovementLayer.UNIVERSAL_AIR_MOVE;
                boolean inJumpGrace = now <= st.wallJumpGraceUntilTick;
                Direction wallNow = WallClimbHelper.findClimbWall(player, WallClimbHelper.SERVER_WALL_TOLERANCE);
                if (wallNow != null || inJumpGrace) {
                    st.wallLostTicks = 0;
                } else {
                    st.wallLostTicks++;
                }
                if (!StavesConfig.isEnableWallClimb()
                        || invalidLayer
                        || st.plunging
                        || st.gliding
                        || player.isInWater()
                        || player.isInLava()
                        || player.getAbilities().flying
                        || st.wallLostTicks >= 5) {
                    exitServerWallClimb(player, st, false);
                } else {
                    if (wallNow != null) {
                        st.wallNormalFace = wallNow;
                    }
                    player.fallDistance = 0.0F;
                    ((ServerGamePacketListenerAccessor) player.connection).velorise$setAboveGroundTickCount(0);
                }
            }

            if (st.gliding) {
                if (player.isFallFlying()) {
                    player.stopFallFlying();
                }
                boolean graceExpired = (now - st.gliderOpenedTick) > 2L;
                if (!graceExpired && player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                if (player.onGround() || player.isInWater() || player.isInLava() || (graceExpired && player.getAbilities().flying) || player.getVehicle() != null) {
                    st.gliding = false;
                    ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_STOP);
                } else {
                    player.fallDistance = 0.0F;
                    if ((now - st.gliderOpenedTick) % 10L == 0L) {
                        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_START);
                    }
                    boolean inUpdraft = ElementalGaugeManager.isInsideAnemoUpdraft(level, player.position());
                    if (inUpdraft) {
                        Vec3 dv = player.getDeltaMovement();
                        Vec3 look = player.getLookAngle();
                        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
                        if (horiz.lengthSqr() > 1.0E-4D) horiz = horiz.normalize();
                        double targetX = Mth.lerp(0.18D, dv.x, horiz.x * 0.48D);
                        double targetZ = Mth.lerp(0.18D, dv.z, horiz.z * 0.48D);
                        player.setDeltaMovement(targetX, 0.38D, targetZ);
                        player.hasImpulse = true;
                        player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    }
                    if (now % 6L == 0L) {
                        boolean hasVenti = VentiManager.hasVentiWeapon(player);
                        level.sendParticles(hasVenti ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.CLOUD,
                                player.getX(), player.getY() + 0.9D, player.getZ(),
                                1, 0.25D, 0.1D, 0.25D, 0.01D);
                    }
                }
            }

            if (st.plunging) {
                player.fallDistance = 0.0F;

                boolean waterLanding = false;
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();
                boolean hasFurinaWaterWalk = ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.SPLENDOR
                        || ArcaneStaffItem.getEffectiveKind(off) == ArcaneStaffItem.Kind.SPLENDOR
                        || ElementalGaugeManager.isStandingOnWaterSurface(player);

                if (hasFurinaWaterWalk) {
                    BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.20D, player.getZ());
                    BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.75D, player.getZ());
                    if (level.getFluidState(feet).is(FluidTags.WATER) || level.getFluidState(below).is(FluidTags.WATER)) {
                        waterLanding = true;
                        double surfY = Math.max(
                                level.getFluidState(feet).is(FluidTags.WATER) ? feet.getY() + 1.0D : 0.0D,
                                level.getFluidState(below).is(FluidTags.WATER) ? below.getY() + 1.0D : 0.0D
                        );
                        player.setPos(player.getX(), surfY, player.getZ());
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                        player.setOnGround(true);
                    }
                }

                if (waterLanding || player.onGround() || (now - st.plungeStartedTick) > 200L) {
                    double dropHeight = Math.max(0.0D, st.plungeStartY - player.getY());
                    st.plunging = false;
                    st.actionState = MovementActionState.GROUNDED;
                    st.stateElapsedTicks = 0;
                    st.plungeLandedTick = now;
                    player.fallDistance = 0.0F;
                    ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_END);
                    executePlungeImpact(level, player, dropHeight);
                    applyPlungeFallDamage(level, player, dropHeight);
                } else if (player.isInWater() || player.isInLava() || player.getAbilities().flying || player.getVehicle() != null) {
                    st.plunging = false;
                    st.actionState = MovementActionState.GROUNDED;
                    st.stateElapsedTicks = 0;
                    st.plungeLandedTick = now;
                    ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_END);
                } else {
                    Vec3 dv = player.getDeltaMovement();
                    if (dv.y > -0.625D) {
                        player.setDeltaMovement(dv.x * 0.92D, -0.675D, dv.z * 0.92D);
                        player.hasImpulse = true;
                        player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    }
                    if (now - st.lastMidAirHitTick >= MID_AIR_PLUNGE_INTERVAL_TICKS) {
                        st.lastMidAirHitTick = now;
                        executeMidAirPlungeTick(level, player);
                    }
                    level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5D, player.getZ(),
                            2, 0.18D, 0.25D, 0.18D, 0.02D);
                }
            }
        }
    }

    private static float resolveWeaponBaseDamage(ServerPlayer player, ItemStack held) {
        if (held.getItem() instanceof ArcaneStaffItem staff) {
            boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(held);
            return StavesConfig.getStaffBaseDamage(staff.kind(), isNeth);
        }
        AttributeInstance atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        return atkAttr != null ? Math.max(1.0F, (float) atkAttr.getValue()) : 4.5F;
    }

    private static void executeMidAirPlungeTick(ServerLevel level, ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        float baseDmg = resolveWeaponBaseDamage(player, held);
        float midAirDmg = baseDmg * MID_AIR_PLUNGE_MULT * (float) StavesConfig.getDamageMultiplier();
        AABB box = player.getBoundingBox().inflate(MID_AIR_PLUNGE_RADIUS, 0.75D, MID_AIR_PLUNGE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), midAirDmg);
        }
    }

    private static void applyPlungeFallDamage(ServerLevel level, ServerPlayer player, double plungeDropHeight) {
        if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) {
            return;
        }
        // Authentic Genshin Impact Plunging Attack fall damage tiers (measured from plunge start):
        // 0-40m: 100% reduction (0% taken); 40-60m: 90% reduction (10% taken);
        // 60-100m: 80% reduction (20% taken); >100m: 60% reduction (40% taken).
        float takenRatio;
        if (plungeDropHeight <= 40.0D) {
            return;
        } else if (plungeDropHeight <= 60.0D) {
            takenRatio = 0.10F;
        } else if (plungeDropHeight <= 100.0D) {
            takenRatio = 0.20F;
        } else {
            takenRatio = 0.40F;
        }
        float rawFall = Math.max(0.0F, (float) plungeDropHeight - 3.0F);
        float finalFallDamage = rawFall * takenRatio;
        if (finalFallDamage <= 0.0F) {
            return;
        }
        APPLYING_PLUNGE_FALL_DAMAGE.set(Boolean.TRUE);
        try {
            player.hurt(level.damageSources().fall(), finalFallDamage);
        } finally {
            APPLYING_PLUNGE_FALL_DAMAGE.set(Boolean.FALSE);
        }
    }

    private static void executePlungeImpact(ServerLevel level, ServerPlayer player, double dropHeight) {
        player.fallDistance = 0.0F;
        ItemStack held = player.getMainHandItem();
        boolean isNeth = false;
        Element elem = null;
        if (held.getItem() instanceof ArcaneStaffItem staff) {
            isNeth = ArcaneStaffItem.getEffectiveNetherite(held);
            elem = staff.kind() == ArcaneStaffItem.Kind.OMNI
                    ? ArcaneStaffItem.getOmniActiveElement(held)
                    : staff.getElement();
        } else {
            elem = SwordImbueHelper.getImbuedElement(held);
        }

        boolean isHighPlunge = dropHeight > HIGH_PLUNGE_HEIGHT_THRESHOLD;
        float baseDmg = resolveWeaponBaseDamage(player, held);
        float plungeDmg = baseDmg * (isHighPlunge ? HIGH_PLUNGE_MULT : LOW_PLUNGE_MULT);
        float gaugeU = isNeth ? 1.5F : 1.0F;
        double radius = isHighPlunge ? HIGH_PLUNGE_RADIUS : LOW_PLUNGE_RADIUS;
        double knockback = isHighPlunge ? HIGH_PLUNGE_KNOCKBACK : LOW_PLUNGE_KNOCKBACK;

        Vec3 pos = player.position();
        AABB box = player.getBoundingBox().inflate(radius, 1.8D, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            if (elem != null) {
                ElementalGaugeManager.applyElement(level, target, elem, gaugeU, player, plungeDmg);
            } else {
                float physPlungeDmg = plungeDmg * (float) StavesConfig.getDamageMultiplier();
                target.hurt(level.damageSources().playerAttack(player), physPlungeDmg);
            }
            double dx = target.getX() - pos.x;
            double dz = target.getZ() - pos.z;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist > 1.0E-4D) {
                double nx = dx / horizDist;
                double nz = dz / horizDist;
                Vec3 tv = target.getDeltaMovement();
                target.setDeltaMovement(
                        tv.x + nx * knockback,
                        isHighPlunge ? Math.max(tv.y, HIGH_PLUNGE_LIFT) : tv.y,
                        tv.z + nz * knockback
                );
                target.hurtMarked = true;
            }
        }

        int ringCount = isHighPlunge ? 28 : 18;
        double cloudRing = radius * 0.75D;
        double critRing = radius * 0.60D;
        for (int i = 0; i < ringCount; i++) {
            double ang = (Math.PI * 2.0D * i) / ringCount;
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.x + Math.cos(ang) * cloudRing, pos.y + 0.15D, pos.z + Math.sin(ang) * cloudRing,
                    2, 0.12D, 0.05D, 0.12D, 0.03D);
            level.sendParticles(ParticleTypes.CRIT,
                    pos.x + Math.cos(ang) * critRing, pos.y + 0.25D, pos.z + Math.sin(ang) * critRing,
                    1, 0.1D, 0.1D, 0.1D, 0.04D);
        }
        level.playSound(null, BlockPos.containing(pos), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, isHighPlunge ? 0.75F : 0.90F);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, isHighPlunge ? 0.70F : 0.45F, isHighPlunge ? 0.95F : 1.35F);
    }
}
