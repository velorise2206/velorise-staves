package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.SwordImbueHelper;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Universal Plunge Attack and Permanent Wind Glider on NeoForge 1.21.1.
 */
public final class PlungeAndGliderManager {
    public static final String TAG_WIND_GLIDER_UNLOCKED = "VeloriseWindGliderUnlocked";

    public static final class AirState {
        public boolean gliding = false;
        public long gliderOpenedTick = 0L;
        public boolean plunging = false;
        public float plungeStartFallDist = 0.0F;
        public double plungeStartY = 0.0D;
        public long plungeStartedTick = 0L;
        public long plungeLandedTick = -100L;
    }

    private static final Map<UUID, AirState> STATES = new ConcurrentHashMap<>();

    private PlungeAndGliderManager() {}

    public static void clearAll() {
        STATES.clear();
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) STATES.remove(playerId);
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
        return player.getPersistentData().getBoolean(TAG_WIND_GLIDER_UNLOCKED);
    }

    public static void setWindGliderUnlocked(ServerPlayer player, boolean unlocked) {
        if (player == null) return;
        player.getPersistentData().putBoolean(TAG_WIND_GLIDER_UNLOCKED, unlocked);
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
        if (oldPlayer.getPersistentData().getBoolean(TAG_WIND_GLIDER_UNLOCKED)) {
            newPlayer.getPersistentData().putBoolean(TAG_WIND_GLIDER_UNLOCKED, true);
        }
    }

    public static void syncPlayerOnJoinOrRespawn(ServerPlayer player) {
        if (player == null) return;
        boolean unlocked = player.getPersistentData().getBoolean(TAG_WIND_GLIDER_UNLOCKED);
        ModNetwork.sendStaffAnimation(player, unlocked
                ? com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_UNLOCKED
                : com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_LOCKED);
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
            if (com.velorise.staves.client.ClientAuraTracker.isGliding(player.getId())) return true;
            double minClear = com.velorise.staves.client.ClientAuraTracker.isXilonenSkating(player.getId()) ? 1.10D : 1.50D;
            return player.fallDistance >= 1.50F || hasClearAirBeneath(player, minClear);
        }
        AirState st = STATES.get(player.getUUID());
        if (st != null && st.plunging) return false;
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
        if (st.plunging) return;

        if (st.gliding) {
            st.gliding = false;
            ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_GLIDER_STOP);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 0.65F, 0.90F);
        } else {
            st.gliding = true;
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
        st.plungeStartFallDist = player.fallDistance;
        st.plungeStartY = player.getY();
        st.plungeStartedTick = player.level().getGameTime();

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
            if (now % 40L == 0L) {
                syncPlayerOnJoinOrRespawn(player);
            }
            AirState st = STATES.get(player.getUUID());
            if (st == null) continue;
            ServerLevel level = player.serverLevel();

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
                    st.plunging = false;
                    st.plungeLandedTick = now;
                    player.fallDistance = 0.0F;
                    ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_END);
                    executePlungeImpact(level, player);
                } else if (player.isInWater() || player.isInLava() || player.getAbilities().flying || player.getVehicle() != null) {
                    st.plunging = false;
                    st.plungeLandedTick = now;
                    ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_END);
                } else {
                    Vec3 dv = player.getDeltaMovement();
                    if (dv.y > -0.625D) {
                        player.setDeltaMovement(dv.x * 0.92D, -0.675D, dv.z * 0.92D);
                        player.hasImpulse = true;
                        player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    }
                    level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5D, player.getZ(),
                            2, 0.18D, 0.25D, 0.18D, 0.02D);
                }
            }
        }
    }

    private static void executePlungeImpact(ServerLevel level, ServerPlayer player) {
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

        float plungeDmg = (isNeth ? 9.0F : 6.5F) * (float) StavesConfig.getDamageMultiplier();
        float gaugeU = isNeth ? 1.5F : 1.0F;
        double radius = 3.5D;

        AABB box = player.getBoundingBox().inflate(radius, 1.8D, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            if (elem != null) {
                ElementalGaugeManager.applyElement(level, target, elem, gaugeU, player, plungeDmg);
            } else {
                target.hurt(level.damageSources().playerAttack(player), plungeDmg);
            }
        }

        Vec3 pos = player.position();
        for (int i = 0; i < 20; i++) {
            double ang = (Math.PI * 2.0D * i) / 20.0D;
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.x + Math.cos(ang) * 2.2D, pos.y + 0.15D, pos.z + Math.sin(ang) * 2.2D,
                    2, 0.12D, 0.05D, 0.12D, 0.03D);
            level.sendParticles(ParticleTypes.CRIT,
                    pos.x + Math.cos(ang) * 1.8D, pos.y + 0.25D, pos.z + Math.sin(ang) * 1.8D,
                    1, 0.1D, 0.1D, 0.1D, 0.04D);
        }
        level.playSound(null, BlockPos.containing(pos), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.85F);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.55F, 1.35F);
    }
}
