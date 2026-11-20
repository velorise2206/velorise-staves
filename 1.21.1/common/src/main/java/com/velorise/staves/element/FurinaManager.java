package com.velorise.staves.element;

import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.velorise.staves.element.PlayerGenshinState.*;
import static com.velorise.staves.element.ElementalGaugeManager.HP_CHANGE_BUFF_TICKS;

/**
 * Dedicated server-side idle manager for Hydro Archon Furina (Splendor of Tranquil Waters):
 * - 4041: Ousia (Salon Members playful orbit)
 * - 4042: Pneuma (Hydro Bubble seat + 3-tier Cake Tower)
 */
public final class FurinaManager {
    private FurinaManager() {}

    private static final class FurinaIdleState {
        Vec3 lastStillPos = Vec3.ZERO;
        int stillTicks = 0;
        int activeCode = 4040;
        int lastSyncedCode = -1;
    }

    private static final Map<UUID, FurinaIdleState> FURINA_IDLE_STATES = new ConcurrentHashMap<>();

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            FURINA_IDLE_STATES.remove(playerId);
            FURINA_FANFARE_UNTIL.remove(playerId);
            FURINA_FANFARE_POINTS.remove(playerId);
            FURINA_FANFARE_NETHERITE.remove(playerId);
            PLAYER_LAST_HP.remove(playerId);
            PLAYER_HP_CHANGE_BUFF_UNTIL.remove(playerId);
        }
    }

    public static void clearAll() {
        FURINA_IDLE_STATES.clear();
        FURINA_FANFARE_UNTIL.clear();
        FURINA_FANFARE_POINTS.clear();
        FURINA_FANFARE_NETHERITE.clear();
        PLAYER_LAST_HP.clear();
        PLAYER_HP_CHANGE_BUFF_UNTIL.clear();
    }

    public static void updatePlayerHpTracking(ServerPlayer player, long now) {
        if (player == null || !player.isAlive()) return;
        UUID id = player.getUUID();
        float curHp = player.getHealth();
        Float prevHp = PLAYER_LAST_HP.put(id, curHp);
        if (prevHp != null && Math.abs(curHp - prevHp) > 0.01F) {
            PLAYER_HP_CHANGE_BUFF_UNTIL.put(id, now + HP_CHANGE_BUFF_TICKS);
            if (player.level() instanceof ServerLevel sl) {
                float deltaFraction = Math.abs(curHp - prevHp) / Math.max(1.0F, player.getMaxHealth());
                recordFurinaHpFluctuation(sl, player, deltaFraction);
            }
        }
    }

    public static void recordFurinaHpFluctuation(ServerLevel level, LivingEntity changedEntity, float hpFractionDelta) {
        if (level == null || changedEntity == null || hpFractionDelta <= 0.001F) return;
        long now = level.getGameTime();
        int gainPoints = Math.max(1, Math.round(hpFractionDelta * 100.0F * 4.0F));
        for (ServerPlayer sp : level.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(changedEntity) <= 256.0D)) {
            if (com.velorise.staves.entity.HydroSalonSummonEntity.hasActiveSummon(sp)) {
                boolean neth = com.velorise.staves.entity.HydroSalonSummonEntity.hasNetheriteSummon(sp);
                UUID spId = sp.getUUID();
                Long until = FURINA_FANFARE_UNTIL.get(spId);
                int current = (until != null && now < until) ? FURINA_FANFARE_POINTS.getOrDefault(spId, 0) : 0;
                int updated = Math.min(300, current + gainPoints);
                FURINA_FANFARE_POINTS.put(spId, updated);
                FURINA_FANFARE_UNTIL.put(spId, now + 360L); // 18s Fanfare duration
                FURINA_FANFARE_NETHERITE.put(spId, neth);
            }
        }
    }

    public static void activateFurinaUniversalRevelry(ServerPlayer player, boolean isNetherite) {
        if (player == null || player.level().isClientSide()) return;
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        int cur = (now < FURINA_FANFARE_UNTIL.getOrDefault(id, 0L)) ? FURINA_FANFARE_POINTS.getOrDefault(id, 0) : 0;
        FURINA_FANFARE_POINTS.put(id, Math.min(300, Math.max(cur + (isNetherite ? 240 : 160), isNetherite ? 300 : 180)));
        FURINA_FANFARE_UNTIL.put(id, now + 360L); // 18.0s Universal Revelry duration
        FURINA_FANFARE_NETHERITE.put(id, isNetherite);
    }

    public static float getFanfareBonusMultiplier(@Nullable LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return 1.0F;
        long now = entity.level().getGameTime();
        UUID bestOwner = null;
        int bestPoints = 0;
        if (entity instanceof Player p) {
            Long until = FURINA_FANFARE_UNTIL.get(p.getUUID());
            if (until != null && now < until) {
                bestPoints = FURINA_FANFARE_POINTS.getOrDefault(p.getUUID(), 0);
                bestOwner = p.getUUID();
            }
        }
        if (bestPoints < 300 && entity.level() instanceof ServerLevel sl) {
            for (ServerPlayer sp : sl.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(entity) <= 256.0D)) {
                Long until = FURINA_FANFARE_UNTIL.get(sp.getUUID());
                if (until != null && now < until) {
                    int pts = FURINA_FANFARE_POINTS.getOrDefault(sp.getUUID(), 0);
                    if (pts > bestPoints) {
                        bestPoints = pts;
                        bestOwner = sp.getUUID();
                    }
                }
            }
        }
        if (bestPoints <= 0 || bestOwner == null) return 1.0F;
        boolean neth = Boolean.TRUE.equals(FURINA_FANFARE_NETHERITE.get(bestOwner));
        float maxBonus = neth ? 0.30F : 0.25F;
        return 1.0F + (bestPoints / 300.0F) * maxBonus;
    }

    public static void drainHealthSilently(LivingEntity entity, float newHealth) {
        if (entity == null || !entity.isAlive()) return;
        Vec3 prevVel = entity.getDeltaMovement();
        float clamped = Math.max(0.5F, Math.min(entity.getMaxHealth(), newHealth));
        if (!entity.level().isClientSide()) {
            ModNetwork.sendSilentHealthSync(entity, clamped);
        }
        entity.setHealth(clamped);
        entity.invulnerableTime = 0;
        entity.hurtTime = 0;
        entity.hurtDuration = 0;
        entity.hurtMarked = false;
        entity.setDeltaMovement(prevVel);
    }

    public static void applyFurinaWaterWalking(Player player) {
        if (player == null || player.isShiftKeyDown() || player.isSpectator() || player.getAbilities().flying) return;
        if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) return;
        if (!player.level().getEntitiesOfClass(
                com.velorise.staves.entity.GeoFangPillarEntity.class,
                player.getBoundingBox().inflate(0.25D, 0.75D, 0.25D),
                Entity::isAlive
        ).isEmpty()) {
            return;
        }

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        var level = player.level();

        double[][] offsets = {
            {0.0D, 0.0D},
            {-0.28D, -0.28D},
            {0.28D, -0.28D},
            {-0.28D, 0.28D},
            {0.28D, 0.28D}
        };

        BlockPos waterPos = null;
        for (double[] off : offsets) {
            double cx = px + off[0];
            double cz = pz + off[1];
            BlockPos feet = BlockPos.containing(cx, py - 0.01D, cz);
            BlockPos below = BlockPos.containing(cx, py - 0.45D, cz);
            BlockPos deepBelow = BlockPos.containing(cx, py + Math.min(-0.95D, player.getDeltaMovement().y - 0.25D), cz);
            if (level.getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER)
                    && !level.getFluidState(feet.above()).is(net.minecraft.tags.FluidTags.WATER)
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                if (waterPos == null || feet.getY() > waterPos.getY()) {
                    waterPos = feet;
                }
            } else if (level.getFluidState(below).is(net.minecraft.tags.FluidTags.WATER)
                    && !level.getFluidState(below.above()).is(net.minecraft.tags.FluidTags.WATER)
                    && level.getBlockState(below.above()).getCollisionShape(level, below.above()).isEmpty()) {
                if (waterPos == null || below.getY() > waterPos.getY()) {
                    waterPos = below;
                }
            } else if (level.getFluidState(deepBelow).is(net.minecraft.tags.FluidTags.WATER)
                    && !level.getFluidState(deepBelow.above()).is(net.minecraft.tags.FluidTags.WATER)
                    && level.getBlockState(deepBelow.above()).getCollisionShape(level, deepBelow.above()).isEmpty()) {
                if (waterPos == null || deepBelow.getY() > waterPos.getY()) {
                    waterPos = deepBelow;
                }
            }
        }
        if (waterPos == null) return;

        double surfaceY = waterPos.getY() + 1.0D;
        Vec3 dv = player.getDeltaMovement();
        if (dv.y > 0.08D) {
            return;
        }

        double minSurfaceOffset = Math.max(NeuvilletteManager.isChannelingGuardianLens(player) ? 1.15D : 0.65D, -dv.y + 0.25D);
        boolean atSurface = (py <= surfaceY + 0.12D && py >= surfaceY - minSurfaceOffset);
        boolean crossingSurface = (py > surfaceY && (py + dv.y) <= surfaceY + 0.08D && py <= surfaceY + Math.max(0.85D, -dv.y + 0.35D));
        if (!atSurface && !crossingSurface) {
            return;
        }

        if (py < surfaceY || crossingSurface) {
            player.setPos(px, surfaceY, pz);
        }
        if (dv.y < 0.0D) {
            player.setDeltaMovement(dv.x, 0.0D, dv.z);
        }
        if (player.fallDistance > 3.0F && !player.level().isClientSide() && !PlungeAndGliderManager.isPlungeFallImmune(player)) {
            player.causeFallDamage(player.fallDistance, 1.0F, player.damageSources().fall());
        }
        player.setOnGround(true);
        player.fallDistance = 0.0F;
        player.setSwimming(false);
    }

    public static boolean isStandingOnWaterSurface(Player player) {
        if (player == null || player.isShiftKeyDown() || player.isSpectator()) return false;
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.05D, player.getZ());
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.50D, player.getZ());
        var level = player.level();
        if (level.getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER)) {
            double surfaceY = feet.getY() + 1.0D;
            return Math.abs(player.getY() - surfaceY) <= 0.35D;
        }
        if (level.getFluidState(below).is(net.minecraft.tags.FluidTags.WATER)) {
            double surfaceY = below.getY() + 1.0D;
            return Math.abs(player.getY() - surfaceY) <= 0.35D;
        }
        return false;
    }

    public static boolean hasRecentHpChangeBuff(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return false;
        long now = player.level().getGameTime();
        updatePlayerHpTracking(player, now);
        Long until = PLAYER_HP_CHANGE_BUFF_UNTIL.get(player.getUUID());
        return until != null && now < until;
    }

    public static void tickIdle(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            ItemStack main = player.getMainHandItem();
            ItemStack furinaStack = ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.SPLENDOR
                    ? main
                    : ItemStack.EMPTY;
            boolean holding = !furinaStack.isEmpty();

            FurinaIdleState st = FURINA_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new FurinaIdleState());
            if (st.lastStillPos == null || st.lastStillPos.equals(Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distSq = player.position().distanceToSqr(st.lastStillPos);
            boolean canIdle = holding && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting() && player.getVehicle() == null;
            if (canIdle && distSq < 0.0025D) {
                st.stillTicks++;
                if (st.stillTicks >= 200) {
                    if (st.activeCode == 4040) {
                        st.activeCode = 4042;
                    }
                } else {
                    st.activeCode = 4040;
                }
            } else {
                st.stillTicks = 0;
                st.lastStillPos = player.position();
                st.activeCode = 4040;
            }
            if (st.activeCode != st.lastSyncedCode || (st.activeCode > 4040 && now % 20L == 0L)) {
                st.lastSyncedCode = st.activeCode;
                ModNetwork.sendStaffAnimation(player, st.activeCode);
            }
        }
    }

    public static void cancelIdle(ServerPlayer player) {
        if (player == null) return;
        FurinaIdleState st = FURINA_IDLE_STATES.get(player.getUUID());
        if (st != null) {
            st.stillTicks = 0;
            st.lastStillPos = player.position();
            if (st.activeCode != 4040 || st.lastSyncedCode != 4040) {
                st.activeCode = 4040;
                st.lastSyncedCode = 4040;
                ModNetwork.sendStaffAnimation(player, 4040);
            }
        }
    }
}
