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
 * Manages Universal Plunge Attack and Permanent Wind Glider on Forge 1.20.1.
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

    public static boolean hasClearAirBeneath(Player player, double minClearBlocks) {
        if (player == null || player.onGround() || player.isInWater() || player.isInLava()) {
            return false;
        }
        if (player.fallDistance >= (float) minClearBlocks) return true;
        AABB check = player.getBoundingBox().expandTowards(0.0D, -minClearBlocks, 0.0D);
        return player.level().noCollision(player, check);
    }

    public static boolean canTriggerMidAirPlunge(Player player) {
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
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 0.65F, 0.90F);
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
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 0.85F, 1.25F);
        }
    }

    public static void handleStartPlunge(ServerPlayer player) {
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

        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        if (horiz.lengthSqr() > 1.0E-4D) horiz = horiz.normalize();
        player.setDeltaMovement(horiz.x * 0.22D, -1.35D, horiz.z * 0.22D);
        player.hasImpulse = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.85F, 0.85F);
        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_START);
    }

    public static float modifyPlungeOrGliderFallDamage(LivingEntity entity, float rawFallDamage) {
        if (!(entity instanceof ServerPlayer player)) return rawFallDamage;
        AirState st = STATES.get(player.getUUID());
        if (st == null) return rawFallDamage;
        if (st.gliding) {
            player.fallDistance = 0.0F;
            return 0.0F;
        }
        if (st.plunging || (player.level().getGameTime() - st.plungeStartedTick) <= 10L) {
            float effectiveFall = Math.max(player.fallDistance, (float) Math.max(0.0D, st.plungeStartY - player.getY()));
            if (effectiveFall <= 15.0F) {
                return 0.0F;
            }
            float reduced = (effectiveFall - 15.0F) * 0.5F;
            float maxCap = player.getMaxHealth() * 0.40F;
            return Math.min(reduced, maxCap);
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
                boolean graceExpired = (now - st.gliderOpenedTick) > 10L;
                if (!graceExpired && player.getAbilities().flying) {
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                if ((graceExpired && player.onGround()) || player.isInWater() || player.isInLava() || (graceExpired && player.getAbilities().flying) || player.getVehicle() != null) {
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
                        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.9D, player.getZ(),
                                1, 0.25D, 0.1D, 0.25D, 0.01D);
                    }
                }
            }

            if (st.plunging) {
                if (player.isInWater() || player.isInLava() || player.getAbilities().flying || player.getVehicle() != null) {
                    st.plunging = false;
                    ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_END);
                } else if (player.onGround() || (now - st.plungeStartedTick) > 100L) {
                    st.plunging = false;
                    ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_END);
                    executePlungeImpact(level, player);
                } else {
                    Vec3 dv = player.getDeltaMovement();
                    if (dv.y > -1.25D) {
                        player.setDeltaMovement(dv.x * 0.92D, -1.35D, dv.z * 0.92D);
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
        level.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.55F, 1.35F);
    }
}
