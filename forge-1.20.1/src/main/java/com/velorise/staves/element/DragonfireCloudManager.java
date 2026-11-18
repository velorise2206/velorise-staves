package com.velorise.staves.element;

import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active Dragonfire lingering AreaEffectClouds, the reworked Dragonfire Scepter
 * (Left-Click Orb, Right-Click Mini Ender Dragon Pet, [R] Skyfall / Sweeping Strafe Ulti,
 * 3D Idle Animation, and 5-block Dragon-Wing Glider passive), plus Resonant Echo anti-dummy
 * validation, Wither Scepter passives (Wither Carapace & Withering Contagion), and Shulker
 * Seeker upgrades (3rd-Shot Gravity Slam & Shulker Shell Guard).
 */
public final class DragonfireCloudManager {
    public static class CloudEntry {
        public final AreaEffectCloud cloud;
        public final ServerLevel level;
        public final @Nullable UUID ownerId;
        public final boolean isNetherite;
        public final float radius;
        public final long startTick;

        public CloudEntry(AreaEffectCloud cloud, ServerLevel level, @Nullable UUID ownerId, boolean isNetherite, float radius, long startTick) {
            this.cloud = cloud;
            this.level = level;
            this.ownerId = ownerId;
            this.isNetherite = isNetherite;
            this.radius = radius;
            this.startTick = startTick;
        }
    }

    public static final class DragonfireIdleState {
        public Vec3 lastStillPos = Vec3.ZERO;
        public int stillTicks = 0;
        public boolean idleActive = false;
    }

    public record WitherContagionState(@Nullable UUID attackerId, boolean isNetherite, long untilTick) {}

    public static final class ShulkerSlamState {
        public final ServerLevel level;
        public final UUID targetId;
        public final @Nullable UUID casterId;
        public final boolean isNetherite;
        public final long slamDownTick;
        public final long impactTick;
        public boolean slammedDown = false;

        public ShulkerSlamState(ServerLevel level, UUID targetId, @Nullable UUID casterId, boolean isNetherite, long slamDownTick, long impactTick) {
            this.level = level;
            this.targetId = targetId;
            this.casterId = casterId;
            this.isNetherite = isNetherite;
            this.slamDownTick = slamDownTick;
            this.impactTick = impactTick;
        }
    }

    private static final List<CloudEntry> CLOUDS = new CopyOnWriteArrayList<>();
    private static final Map<UUID, Long> DRAGONFIRE_PET_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DRAGONFIRE_PET_NETHERITE = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> DRAGONFIRE_PET_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DRAGONFIRE_PET_NEXT_ATK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DRAGONFIRE_ORB_CD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DRAGONFIRE_ULTI_CD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, DragonfireIdleState> DRAGONFIRE_IDLE_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, WitherContagionState> WITHER_CONTAGION_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, ShulkerSlamState> SHULKER_SLAM_STATES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> CURRENT_IMPACT_FINALE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DragonfireCloudManager() {}

    public static void setCurrentImpactFinale(boolean finale) {
        CURRENT_IMPACT_FINALE.set(finale);
    }

    public static boolean isCurrentImpactFinale() {
        return Boolean.TRUE.equals(CURRENT_IMPACT_FINALE.get());
    }

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite) {
        float r = cloud.getRadius() > 0.1F ? cloud.getRadius() : (isNetherite ? 4.5F : 3.5F);
        register(cloud, level, owner, isNetherite, r);
    }

    public static void register(AreaEffectCloud cloud, ServerLevel level, @Nullable ServerPlayer owner, boolean isNetherite, float radius) {
        cloud.setRadius(0.0F);
        cloud.setRadiusPerTick(0.0F);
        cloud.setWaitTime(cloud.getDuration() + 20);
        CLOUDS.add(new CloudEntry(cloud, level, owner != null ? owner.getUUID() : null, isNetherite, radius, level.getGameTime()));
    }

    public static void spawnDragonfireCloudAt(ServerLevel level, Vec3 pos, @Nullable ServerPlayer owner, boolean isNetherite, float radius, int durationTicks) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
        if (owner != null) {
            cloud.setOwner(owner);
        }
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(0.0F);
        cloud.setDuration(durationTicks);
        cloud.setRadiusPerTick(0.0F);
        cloud.setWaitTime(durationTicks + 20);
        cloud.addTag("velorise_staves:dragonfire_cloud");
        level.addFreshEntity(cloud);
        ElectrifiedWaterManager.electrify(level, BlockPos.containing(pos), radius + 1.0D, durationTicks, owner, 1.0F);
        register(cloud, level, owner, isNetherite, radius);
    }

    public static boolean isHoldingDragonfireWeapon(@Nullable Player player) {
        if (player == null) return false;
        if (ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.DRAGONFIRE) {
            return true;
        }
        return !ArcaneStaffItem.isGenshinCharacterWeapon(player.getMainHandItem())
                && ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.DRAGONFIRE;
    }

    private static boolean stackGrantsDragonfireGlider(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArcaneStaffItem staff)) return false;
        if (staff.kind() == ArcaneStaffItem.Kind.DRAGONFIRE) return true;
        if (staff.kind() == ArcaneStaffItem.Kind.OMNI) {
            ArcaneStaffItem electroAbsorbed = ArcaneStaffItem.getAbsorbedStaffForElement(stack, Element.ELECTRO);
            return electroAbsorbed != null && electroAbsorbed.kind() == ArcaneStaffItem.Kind.DRAGONFIRE;
        }
        return false;
    }

    /**
     * Hidden Passive (Dragon-Wing Glider): Returns true if the player possesses Dragonfire Scepter
     * (or an Omni Staff that has absorbed Dragonfire Scepter) anywhere in their inventory or hands.
     */
    public static boolean hasDragonfireWeapon(@Nullable Player player) {
        if (player == null) return false;
        if (stackGrantsDragonfireGlider(player.getMainHandItem()) || stackGrantsDragonfireGlider(player.getOffhandItem())) {
            return true;
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (stackGrantsDragonfireGlider(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasActiveMiniDragon(@Nullable Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            return ClientAuraTracker.hasDragonfirePet(player.getId());
        }
        if (!(player.level() instanceof ServerLevel sl) || sl.getServer() == null) return false;
        long now = sl.getServer().overworld().getGameTime();
        return now < DRAGONFIRE_PET_UNTIL.getOrDefault(player.getUUID(), 0L);
    }

    /**
     * Right-Click (E Skill): Summons a 1-block Mini Ender Dragon pet for 30s (600 ticks)
     * that smoothly patrols around the player, teleports if > 18m away, and attacks the
     * nearest enemy every 2.0s (40 ticks).
     */
    public static boolean summonMiniDragon(ServerPlayer player, boolean isNetherite) {
        if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel level) || level.getServer() == null) {
            return false;
        }
        cancelIdle(player);
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        DRAGONFIRE_PET_UNTIL.put(id, now + 600L); // 30.0s duration
        DRAGONFIRE_PET_NETHERITE.put(id, isNetherite);
        DRAGONFIRE_PET_POS.put(id, player.position().add(0.0D, 1.45D, 0.0D));
        DRAGONFIRE_PET_NEXT_ATK.put(id, now + 20L);

        ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_DRAGONFIRE_PET_ON);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.4D, player.getZ(), 18, 0.45D, 0.35D, 0.45D, 0.03D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.4D, player.getZ(), 8, 0.35D, 0.25D, 0.35D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.85F, 1.35F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.55F, 1.55F);
        return true;
    }

    /**
     * Left-Click (Normal Attack): Fires the Ender Dragon plasma orb (4.0s internal cooldown).
     */
    public static void handleLeftClickOrb(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) return;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) != ArcaneStaffItem.Kind.DRAGONFIRE
                || !(main.getItem() instanceof ArcaneStaffItem mainStaff)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.getServer() == null) return;
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = DRAGONFIRE_ORB_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            ArchonUltiManager.sendMinimalistBurstCooldownAlert(player, Element.ELECTRO, cdUntil - now);
            return;
        }

        ArcaneStaffItem targetStaff = mainStaff.kind() == ArcaneStaffItem.Kind.OMNI
                ? ArcaneStaffItem.getAbsorbedStaffForElement(main, Element.ELECTRO)
                : mainStaff;
        if (targetStaff == null) return;

        cancelIdle(player);
        if (targetStaff.triggerDragonfireOrbFromLeftClick(player)) {
            long baseCd = Math.max(20L, Math.round(80.0D * StavesConfig.getCooldownMultiplier())); // 4.0s (80t) orb cooldown
            if (ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)) {
                baseCd = Math.max(20L, Math.round(baseCd * 0.90D));
            }
            DRAGONFIRE_ORB_CD_UNTIL.put(id, now + baseCd);
            ArcaneStaffItem.hurtStaffAndCheckBreak(main, 1, player, InteractionHand.MAIN_HAND);
            player.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    /**
     * [R] Elemental Burst (20.0s CD):
     * - Variant 1 (When Mini Ender Dragon Pet is active — Skyfall Dive):
     *   Mini Dragon shoots straight into the sky and vanishes; 2.0s (40t) later, a Giant Ender Dragon
     *   crashes vertically into the ground 1 block ahead of the player for 5x5 AoE Electro damage.
     * - Variant 2 (When Mini Ender Dragon Pet is NOT active — Sweeping Strafe):
     *   A Giant Ender Dragon swoops in a curved arc skimming the ground along a 3m wide x 10m long
     *   corridor in the player's look direction.
     */
    public static void triggerDragonfireUlti(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.level().isClientSide()) return;
        cancelIdle(player);
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;

        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getMainHandItem();
        boolean isNeth;
        if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.DRAGONFIRE) {
            isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        } else if (!ArcaneStaffItem.isGenshinCharacterWeapon(stack)) {
            hand = InteractionHand.OFF_HAND;
            stack = player.getOffhandItem();
            if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.DRAGONFIRE) {
                isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
            } else {
                return;
            }
        } else {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level.getServer() == null) return;
        long now = level.getServer().overworld().getGameTime();
        UUID id = player.getUUID();
        long cdUntil = DRAGONFIRE_ULTI_CD_UNTIL.getOrDefault(id, 0L);
        if (now < cdUntil) {
            ArchonUltiManager.sendMinimalistBurstCooldownAlert(player, Element.ELECTRO, cdUntil - now);
            return;
        }

        long cdTicks = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO) ? 360L : 400L; // 20.0s base CD
        DRAGONFIRE_ULTI_CD_UNTIL.put(id, now + cdTicks);
        ArcaneStaffItem.hurtStaffAndCheckBreak(stack, 1, player, hand);
        ElementalGaugeManager.onPlayerCastStaffSkill(player);

        Vec3 look = player.getLookAngle();
        Vec3 horizFwd = new Vec3(look.x, 0.0D, look.z);
        if (horizFwd.lengthSqr() < 1.0E-4D) {
            horizFwd = Vec3.directionFromRotation(0.0F, player.getYRot());
        } else {
            horizFwd = horizFwd.normalize();
        }
        final Vec3 lockedFwd = horizFwd;
        final boolean finalIsNeth = isNeth;
        boolean hadPet = now < DRAGONFIRE_PET_UNTIL.getOrDefault(id, 0L);

        if (hadPet) {
            // Variant 1: Skyfall Dive (Mini Dragon rockets into the sky -> 2.0s later Giant Dragon slams 5x5 ahead)
            DRAGONFIRE_PET_UNTIL.remove(id);
            DRAGONFIRE_PET_POS.remove(id);
            DRAGONFIRE_PET_NEXT_ATK.remove(id);
            ArchonUltiManager.grantUltiIframe(player, 44);
            ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_DRAGONFIRE_ULTI_SKYFALL);

            Vec3 launchPos = player.position().add(0.0D, 1.5D, 0.0D);
            for (int i = 0; i < 16; i++) {
                level.sendParticles(ParticleTypes.DRAGON_BREATH, launchPos.x, launchPos.y + i * 0.65D, launchPos.z, 3, 0.2D, 0.2D, 0.2D, 0.04D);
                level.sendParticles(ParticleTypes.END_ROD, launchPos.x, launchPos.y + i * 0.65D, launchPos.z, 1, 0.1D, 0.1D, 0.1D, 0.02D);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.1F, 1.45F);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.9F, 1.25F);

            ArchonUltiManager.scheduleBurstAction(player, 40, () -> {
                if (!player.isAlive() || !(player.level() instanceof ServerLevel sl)) return;
                Vec3 curLook = player.getLookAngle();
                Vec3 fwd = new Vec3(curLook.x, 0.0D, curLook.z);
                if (fwd.lengthSqr() < 1.0E-4D) fwd = lockedFwd;
                else fwd = fwd.normalize();

                // 1 block in front of the player, 5x5 square AoE (half-extent = 2.5m, or 3.25m on Netherite)
                Vec3 rawTarget = player.position().add(fwd.scale(3.5D)); // Center of the 5x5 zone starting 1 block ahead
                BlockHitResult downHit = sl.clip(new ClipContext(
                        rawTarget.add(0.0D, 4.0D, 0.0D), rawTarget.subtract(0.0D, 8.0D, 0.0D),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 impactPos = downHit.getType() == HitResult.Type.BLOCK ? downHit.getLocation() : rawTarget;

                double halfExt = finalIsNeth ? 3.25D : 2.5D; // 5x5 blocks base (6.5x6.5 Netherite)
                float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.DRAGONFIRE, finalIsNeth);
                float diveDmg = (finalIsNeth ? 24.0F : 16.0F) * scale;
                float gaugeU = finalIsNeth ? 4.0F : 3.5F;

                AABB box = new AABB(
                        impactPos.x - halfExt, impactPos.y - 1.5D, impactPos.z - halfExt,
                        impactPos.x + halfExt, impactPos.y + 3.5D, impactPos.z + halfExt
                );
                for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && !e.isSpectator() && e != player && !player.isAlliedTo(e))) {
                    target.invulnerableTime = 0;
                    ElementalGaugeManager.applyElementBypassIcd(sl, target, Element.ELECTRO, gaugeU, player, diveDmg);
                }
                DendroCoreManager.checkReactionsNear(sl, impactPos, halfExt + 0.5D, Element.ELECTRO, player);
                spawnDragonfireCloudAt(sl, impactPos, player, finalIsNeth, (float) halfExt, 120);

                sl.sendParticles(ParticleTypes.DRAGON_BREATH, impactPos.x, impactPos.y + 0.5D, impactPos.z, 65, halfExt * 0.45D, 0.4D, halfExt * 0.45D, 0.08D);
                sl.sendParticles(ParticleTypes.END_ROD, impactPos.x, impactPos.y + 0.8D, impactPos.z, 28, halfExt * 0.4D, 0.5D, halfExt * 0.4D, 0.06D);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactPos.x, impactPos.y + 0.4D, impactPos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                sl.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.75F);
                sl.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.85F);
                sl.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0F, 0.80F);
            });
        } else {
            // Variant 2: Sweeping Strafe (Giant Dragon swoops in a curved arc skimming a 3m wide x 10m long corridor)
            ArchonUltiManager.grantUltiIframe(player, 36);
            ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_DRAGONFIRE_ULTI_SWEEP);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0F, 0.90F);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.1F, 0.85F);

            ArchonUltiManager.scheduleBurstAction(player, 14, () -> {
                if (!player.isAlive() || !(player.level() instanceof ServerLevel sl)) return;
                Vec3 origin = player.position();
                Vec3 curLook = player.getLookAngle();
                Vec3 fwd = new Vec3(curLook.x, 0.0D, curLook.z);
                if (fwd.lengthSqr() < 1.0E-4D) fwd = lockedFwd;
                else fwd = fwd.normalize();

                double length = finalIsNeth ? 12.0D : 10.0D; // 10 blocks long (12m Netherite)
                double halfWidth = finalIsNeth ? 2.0D : 1.5D; // 3 blocks wide (4m Netherite)
                float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.DRAGONFIRE, finalIsNeth);
                float sweepDmg = (finalIsNeth ? 18.0F : 12.0F) * scale;
                float gaugeU = finalIsNeth ? 3.5F : 3.0F;

                Vec3 end = origin.add(fwd.scale(length));
                AABB corridorBox = new AABB(origin, end).inflate(halfWidth + 1.0D, 2.5D, halfWidth + 1.0D);
                for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, corridorBox,
                        e -> e.isAlive() && !e.isSpectator() && e != player && !player.isAlliedTo(e))) {
                    Vec3 rel = target.position().subtract(origin);
                    double along = rel.x * fwd.x + rel.z * fwd.z;
                    if (along < -0.5D || along > length + 0.5D) continue;
                    double perpX = rel.x - fwd.x * along;
                    double perpZ = rel.z - fwd.z * along;
                    double perpDist = Math.sqrt(perpX * perpX + perpZ * perpZ);
                    if (perpDist <= halfWidth + target.getBbWidth() * 0.5D) {
                        target.invulnerableTime = 0;
                        ElementalGaugeManager.applyElementBypassIcd(sl, target, Element.ELECTRO, gaugeU, player, sweepDmg);
                    }
                }

                for (double d = 1.0D; d <= length; d += 1.5D) {
                    Vec3 pt = origin.add(fwd.scale(d));
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, pt.x, pt.y + 0.5D, pt.z, 10, halfWidth * 0.45D, 0.25D, halfWidth * 0.45D, 0.05D);
                    sl.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y + 0.8D, pt.z, 3, halfWidth * 0.35D, 0.2D, halfWidth * 0.35D, 0.03D);
                    ElectrifiedWaterManager.electrify(sl, BlockPos.containing(pt), halfWidth + 0.5D, 80, player, 1.0F);
                    DendroCoreManager.checkReactionsNear(sl, pt, halfWidth + 0.5D, Element.ELECTRO, player);
                }
                sl.playSound(null, origin.x + fwd.x * 5.0D, origin.y, origin.z + fwd.z * 5.0D,
                        SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.05F, 0.90F);
                sl.playSound(null, origin.x + fwd.x * 5.0D, origin.y, origin.z + fwd.z * 5.0D,
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.85F, 1.15F);
            });
        }
    }

    public static void cancelIdle(@Nullable ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel sl) || sl.getServer() == null) return;
        UUID id = player.getUUID();
        DragonfireIdleState st = DRAGONFIRE_IDLE_STATES.get(id);
        if (st != null) {
            st.stillTicks = 0;
            st.lastStillPos = player.position();
            if (st.idleActive) {
                st.idleActive = false;
                long now = sl.getServer().overworld().getGameTime();
                boolean petActive = now < DRAGONFIRE_PET_UNTIL.getOrDefault(id, 0L);
                ModNetwork.sendStaffAnimation(player, petActive
                        ? ClientAuraTracker.ANIM_DRAGONFIRE_PET_ON
                        : ClientAuraTracker.ANIM_DRAGONFIRE_PET_OFF);
            }
        }
    }

    /**
     * Ticks active Mini Ender Dragon pets, Dragonfire 3D idle transitions, and Shulker Gravity Slams.
     */
    public static void tickPlayers(MinecraftServer server, long serverNow) {
        if (server == null) return;

        // 1. Tick Shulker 3rd-Shot Gravity Slams
        if (!SHULKER_SLAM_STATES.isEmpty()) {
            for (Map.Entry<UUID, ShulkerSlamState> entry : SHULKER_SLAM_STATES.entrySet()) {
                ShulkerSlamState slam = entry.getValue();
                Entity ent = slam.level.getEntity(slam.targetId);
                if (!(ent instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) {
                    SHULKER_SLAM_STATES.remove(entry.getKey());
                    continue;
                }
                if (!slam.slammedDown && serverNow >= slam.slamDownTick) {
                    slam.slammedDown = true;
                    target.removeEffect(MobEffects.LEVITATION);
                    Vec3 dv = target.getDeltaMovement();
                    target.setDeltaMovement(dv.x * 0.25D, -1.45D, dv.z * 0.25D);
                    target.hurtMarked = true;
                    if (target instanceof ServerPlayer sp) {
                        sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                    }
                    slam.level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                            16, 0.3D, 0.3D, 0.3D, 0.1D);
                    slam.level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.75F);
                }
                if (slam.slammedDown && (target.onGround() || serverNow >= slam.impactTick)) {
                    SHULKER_SLAM_STATES.remove(entry.getKey());
                    ServerPlayer caster = slam.casterId != null ? server.getPlayerList().getPlayer(slam.casterId) : null;
                    double radius = slam.isNetherite ? 3.0D : 2.5D;
                    float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.SHULKER, slam.isNetherite);
                    float slamDmg = (slam.isNetherite ? 7.5F : 5.0F) * scale;
                    float slamU = slam.isNetherite ? 2.5F : 2.0F;
                    Vec3 hitPos = target.position();
                    AABB box = new AABB(hitPos, hitPos).inflate(radius, 1.8D, radius);
                    for (LivingEntity nearby : slam.level.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && !e.isSpectator() && (caster == null || (e != caster && !caster.isAlliedTo(e))))) {
                        nearby.invulnerableTime = 0;
                        ElementalGaugeManager.applyElementBypassIcd(slam.level, nearby, Element.ELECTRO, slamU, caster, slamDmg);
                    }
                    DendroCoreManager.checkReactionsNear(slam.level, hitPos, radius + 0.5D, Element.ELECTRO, caster);
                    slam.level.sendParticles(ParticleTypes.END_ROD, hitPos.x, hitPos.y + 0.25D, hitPos.z, 24, radius * 0.45D, 0.2D, radius * 0.45D, 0.06D);
                    slam.level.sendParticles(ParticleTypes.PORTAL, hitPos.x, hitPos.y + 0.35D, hitPos.z, 28, radius * 0.5D, 0.25D, radius * 0.5D, 0.08D);
                    slam.level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.65F, 1.35F);
                    slam.level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 0.95F, 0.75F);
                }
            }
        }

        // 2. Prune expired Wither Contagion states every 40 ticks
        if (serverNow % 40L == 0L && !WITHER_CONTAGION_STATES.isEmpty()) {
            WITHER_CONTAGION_STATES.entrySet().removeIf(e -> serverNow >= e.getValue().untilTick());
        }

        // 3. Tick per-player Mini Ender Dragon pet & Dragonfire 3D Idle
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!player.isAlive() || player.isSpectator()) {
                clearPlayerState(id);
                continue;
            }
            ServerLevel level = player.serverLevel();

            // Wither Carapace subtle aura feedback when <= 50% HP
            if (serverNow % 15L == 0L && isWitherCarapaceActive(player)) {
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.3D, 0.4D, 0.3D, 0.01D);
            }

            // A. Mini Ender Dragon Pet Patrol & 2.0s Auto-Attack
            Long petUntil = DRAGONFIRE_PET_UNTIL.get(id);
            boolean petActive = petUntil != null && serverNow < petUntil;
            if (petUntil != null && !petActive) {
                DRAGONFIRE_PET_UNTIL.remove(id);
                DRAGONFIRE_PET_NETHERITE.remove(id);
                DRAGONFIRE_PET_POS.remove(id);
                DRAGONFIRE_PET_NEXT_ATK.remove(id);
                DragonfireIdleState idleSt = DRAGONFIRE_IDLE_STATES.get(id);
                if (idleSt == null || !idleSt.idleActive) {
                    ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_DRAGONFIRE_PET_OFF);
                }
            } else if (petActive) {
                double orbitAng = (serverNow * 0.07D);
                Vec3 desiredOrbit = player.position().add(Math.cos(orbitAng) * 1.55D, 1.45D + Math.sin(serverNow * 0.12D) * 0.18D, Math.sin(orbitAng) * 1.55D);
                Vec3 curPetPos = DRAGONFIRE_PET_POS.getOrDefault(id, desiredOrbit);
                if (curPetPos.distanceToSqr(player.position()) > 324.0D) { // > 18m away: teleport to master like Furina summons
                    curPetPos = desiredOrbit;
                    level.sendParticles(ParticleTypes.PORTAL, curPetPos.x, curPetPos.y, curPetPos.z, 12, 0.25D, 0.25D, 0.25D, 0.05D);
                    level.playSound(null, BlockPos.containing(curPetPos), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.55F, 1.45F);
                } else {
                    curPetPos = curPetPos.lerp(desiredOrbit, 0.35D);
                }
                DRAGONFIRE_PET_POS.put(id, curPetPos);

                DragonfireIdleState idleSt = DRAGONFIRE_IDLE_STATES.get(id);
                boolean inIdle = idleSt != null && idleSt.idleActive;
                if (serverNow % 40L == 0L) {
                    ModNetwork.sendStaffAnimation(player, inIdle
                            ? ClientAuraTracker.ANIM_DRAGONFIRE_IDLE_ON
                            : ClientAuraTracker.ANIM_DRAGONFIRE_PET_ON);
                }

                long nextAtk = DRAGONFIRE_PET_NEXT_ATK.getOrDefault(id, 0L);
                if (serverNow >= nextAtk) {
                    DRAGONFIRE_PET_NEXT_ATK.put(id, serverNow + 40L); // Attack every 2.0s (40 ticks)
                    LivingEntity target = findNearestMiniDragonTarget(level, player, 12.0D);
                    if (target != null) {
                        boolean isNeth = DRAGONFIRE_PET_NETHERITE.getOrDefault(id, false);
                        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.DRAGONFIRE, isNeth);
                        float petDmg = (isNeth ? 3.0F : 2.0F) * scale; // 1.0 heart Basic / 1.5 hearts Netherite
                        Vec3 targetCenter = target.getBoundingBox().getCenter();
                        ModNetwork.sendElectricArc(level, curPetPos, targetCenter, 10, 0xFFD05CE3);
                        level.sendParticles(ParticleTypes.DRAGON_BREATH, targetCenter.x, targetCenter.y, targetCenter.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
                        level.playSound(null, BlockPos.containing(curPetPos), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.45F, 1.65F);
                        ElementalGaugeManager.applyElement(level, target, Element.ELECTRO, 1.0F, player, petDmg);
                    }
                }
            }

            // B. Dragonfire 3D Idle Animation (standing still 3.0s while holding Dragonfire Scepter)
            boolean holdingDragonfire = isHoldingDragonfireWeapon(player);
            DragonfireIdleState st = DRAGONFIRE_IDLE_STATES.computeIfAbsent(id, k -> new DragonfireIdleState());
            boolean stoodStill = holdingDragonfire
                    && player.onGround()
                    && !player.isShiftKeyDown()
                    && !player.isCrouching()
                    && !player.isUsingItem()
                    && !player.swinging
                    && player.getVehicle() == null
                    && !player.isInWater()
                    && player.position().distanceToSqr(st.lastStillPos) <= 0.0025D
                    && player.getDeltaMovement().horizontalDistanceSqr() <= 0.0025D;
            st.lastStillPos = player.position();
            if (stoodStill) {
                st.stillTicks++;
                if (st.stillTicks >= 60 && !st.idleActive) {
                    st.idleActive = true;
                    ModNetwork.sendStaffAnimation(player, ClientAuraTracker.ANIM_DRAGONFIRE_IDLE_ON);
                }
            } else {
                st.stillTicks = 0;
                if (st.idleActive) {
                    st.idleActive = false;
                    ModNetwork.sendStaffAnimation(player, petActive
                            ? ClientAuraTracker.ANIM_DRAGONFIRE_PET_ON
                            : ClientAuraTracker.ANIM_DRAGONFIRE_PET_OFF);
                }
            }
        }
    }

    private static @Nullable LivingEntity findNearestMiniDragonTarget(ServerLevel level, ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius, 6.0D, radius);
        double maxDistSqr = radius * radius;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                ent -> ent.isAlive() && !ent.isSpectator() && ent != player && !player.isAlliedTo(ent))) {
            if (!isValidResonanceTarget(player, e)) continue;
            boolean hostile = e instanceof Enemy || e == player.getLastHurtMob() || e == player.getLastHurtByMob();
            if (!hostile) continue;
            double d = e.distanceToSqr(player);
            if (d <= maxDistSqr && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    /**
     * Anti-Dummy / Anti-Exploit validation for Resonant Echo's "Resonance" cooldown reduction
     * and automated pet targeting. Excludes armor stands, target dummies, invulnerable/NoAI entities,
     * tamed pets, allies, and passive animals.
     */
    public static boolean isValidResonanceTarget(ServerPlayer attacker, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || target == attacker || target.isSpectator() || target.isInvulnerable()) {
            return false;
        }
        if (target instanceof ArmorStand) return false;
        if (target instanceof Mob mob && mob.isNoAi()) return false;
        if (attacker.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal ta && ta.isTame()) return false;
        if (target instanceof OwnableEntity oe && oe.getOwnerUUID() != null) return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (target instanceof Animal && !(target instanceof Enemy)) return false;

        String regKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(Locale.ROOT);
        String clsName = target.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (regKey.contains("dummy") || regKey.contains("stand") || regKey.contains("mannequin") || regKey.contains("statue")
                || clsName.contains("dummy") || clsName.contains("stand") || clsName.contains("mannequin")) {
            return false;
        }
        return true;
    }

    /**
     * Wither Scepter Passive 1 — Wither Carapace:
     * When HP <= 50% Max HP while holding Wither Scepter (or Omni Staff locked to Wither Scepter)
     * in main hand or offhand, grants 100% immunity to incoming projectiles.
     */
    public static boolean isWitherCarapaceActive(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player) || !player.isAlive() || player.isSpectator()) return false;
        if (player.getHealth() > player.getMaxHealth() * 0.50F + 0.001F) return false;
        return ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.WITHER
                || ArcaneStaffItem.getEffectiveKind(player.getOffhandItem()) == ArcaneStaffItem.Kind.WITHER;
    }

    public static boolean tryDeflectProjectileWithWitherCarapace(LivingEntity entity, DamageSource source) {
        if (entity == null || entity.level().isClientSide() || !isWitherCarapaceActive(entity)) return false;
        boolean isProj = source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile;
        if (!isProj) return false;

        if (source.getDirectEntity() instanceof Projectile proj) {
            Vec3 dv = proj.getDeltaMovement();
            proj.setDeltaMovement(dv.scale(-0.65D));
            proj.hurtMarked = true;
        }
        if (entity.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    14, 0.35D, 0.35D, 0.35D, 0.03D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    6, 0.25D, 0.25D, 0.25D, 0.02D);
            sl.playSound(null, entity.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.70F, 1.55F);
        }
        return true;
    }

    /**
     * Wither Scepter Passive 2 — Withering Contagion:
     * Marks hit enemies for 8.0s; if they die while marked (or while afflicted with Wither + Dendro),
     * they detonate in a 3.0m Withering Contagion cloud spreading 1.5U Dendro + Wither II (80t).
     */
    public static void markWitherContagion(LivingEntity target, @Nullable ServerPlayer attacker, boolean isNetherite, int durationTicks) {
        if (target == null || !target.isAlive() || target.level().isClientSide()) return;
        long now = target.level().getGameTime();
        WITHER_CONTAGION_STATES.put(target.getUUID(),
                new WitherContagionState(attacker != null ? attacker.getUUID() : null, isNetherite, now + durationTicks));
    }

    public static void tryTriggerWitherContagionOnDeath(LivingEntity deadEntity, DamageSource source) {
        if (deadEntity == null || !(deadEntity.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        WitherContagionState st = WITHER_CONTAGION_STATES.remove(deadEntity.getUUID());
        boolean validMark = st != null && now <= st.untilTick();
        boolean naturalCombo = deadEntity.hasEffect(MobEffects.WITHER)
                && ElementalGaugeManager.hasElement(deadEntity, Element.DENDRO);
        if (!validMark && !naturalCombo) return;

        boolean isNeth = st != null && st.isNetherite();
        ServerPlayer attacker = (st != null && st.attackerId() != null && level.getServer() != null)
                ? level.getServer().getPlayerList().getPlayer(st.attackerId())
                : (source.getEntity() instanceof ServerPlayer sp ? sp : null);

        double radius = isNeth ? 3.8D : 3.0D;
        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.WITHER, isNeth);
        float popDmg = (isNeth ? 4.5F : 3.0F) * scale; // 1.5 hearts Basic / 2.25 hearts Netherite
        float gaugeU = isNeth ? 2.0F : 1.5F;
        Vec3 pos = deadEntity.position().add(0.0D, deadEntity.getBbHeight() * 0.5D, 0.0D);

        AABB box = new AABB(pos, pos).inflate(radius, 2.0D, radius);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != deadEntity && !e.isSpectator()
                        && (attacker == null || (e != attacker && !attacker.isAlliedTo(e)))
                        && !(e instanceof Player p && p.isCreative()))) {
            if (nearby.position().distanceToSqr(pos) > radius * radius) continue;
            ElementalGaugeManager.applyElementBypassIcd(level, nearby, Element.DENDRO, gaugeU, attacker, popDmg);
            if (ElementalGaugeManager.getInnateElement(nearby) != Element.DENDRO) {
                nearby.addEffect(new MobEffectInstance(MobEffects.WITHER, isNeth ? 120 : 80, 1)); // Wither II
            }
            markWitherContagion(nearby, attacker, isNeth, 120);
        }
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 32, radius * 0.4D, 0.35D, radius * 0.4D, 0.04D);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.x, pos.y, pos.z, 24, radius * 0.4D, 0.35D, radius * 0.4D, 0.04D);
        level.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 12, radius * 0.3D, 0.3D, radius * 0.3D, 0.03D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.75F, 1.35F);
    }

    /**
     * Shulker Seeker Active — 3rd-Shot Gravity Slam:
     * Launches hit target rapidly upward (+0.78Y) and after 10 ticks slams them into the ground (-1.45Y)
     * for 2.5m AoE Electro Gravity Slam damage.
     */
    public static void scheduleShulkerGravitySlam(ServerLevel level, LivingEntity target, @Nullable ServerPlayer caster, boolean isNetherite) {
        if (level == null || target == null || !target.isAlive() || ElementalGaugeManager.isBossEntity(target)
                || ElementalGaugeManager.isEntityFrozen(target)) {
            return;
        }
        target.removeEffect(MobEffects.LEVITATION);
        Vec3 dv = target.getDeltaMovement();
        target.setDeltaMovement(dv.x * 0.4D, 0.78D, dv.z * 0.4D);
        target.hurtMarked = true;
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
        long serverNow = level.getServer() != null ? level.getServer().overworld().getGameTime() : level.getGameTime();
        SHULKER_SLAM_STATES.put(target.getUUID(), new ShulkerSlamState(
                level,
                target.getUUID(),
                caster != null ? caster.getUUID() : null,
                isNetherite,
                serverNow + 9L,
                serverNow + 18L
        ));
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.5D, target.getZ(), 14, 0.25D, 0.3D, 0.25D, 0.06D);
        level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 0.9F, 1.45F);
    }

    /**
     * Shulker Seeker Passive — Shulker Shell Guard:
     * Holding Shulker Seeker (or Omni Staff locked to Shulker Seeker) in the Main Hand
     * reduces incoming damage by 30% (40% on Netherite), like a closed Shulker shell.
     */
    public static float modifyShulkerShellGuardDamage(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0.0F || !(entity instanceof Player player) || !player.isAlive()
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) != ArcaneStaffItem.Kind.SHULKER) {
            return amount;
        }
        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(main);
        float mult = isNeth ? 0.60F : 0.70F; // -40% Netherite / -30% Basic incoming damage
        if (amount >= 1.0F && entity.level() instanceof ServerLevel sl) {
            sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.SHULKER_HURT_CLOSED, SoundSource.PLAYERS, 0.45F, 1.35F);
        }
        return amount * mult;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<CloudEntry> it = CLOUDS.iterator();

        while (it.hasNext()) {
            CloudEntry entry = it.next();
            if (entry.level != level) continue;
            if (!entry.cloud.isAlive() || entry.cloud.isRemoved()) {
                CLOUDS.remove(entry);
                continue;
            }

            AreaEffectCloud cloud = entry.cloud;
            float radius = entry.radius;
            long age = Math.max(0L, now - entry.startTick);
            int phase = (int) (age % 20L);

            if (phase % 2 == 0) {
                double progress = (double) (phase + 2) / 20.0D;
                double halfExtent = radius * progress;
                ElementalGaugeManager.spawnExpandingSquareParticles(
                        level, cloud.position(), halfExtent, 0.15D,
                        ParticleTypes.DRAGON_BREATH, ParticleTypes.DRAGON_BREATH, 3
                );
            }

            if (age > 0L && phase == 0) {
                BlockPos center = cloud.blockPosition();
                ServerPlayer owner = entry.ownerId != null ? level.getServer().getPlayerList().getPlayer(entry.ownerId) : null;
                float damage = entry.isNetherite ? 2.7F : 1.8F;
                float pulseU = entry.isNetherite ? 1.5F : 1.0F;

                ElectrifiedWaterManager.electrify(level, center, radius + 0.5D, 25, owner, 1.0F);

                Vec3 centerPos = cloud.position();
                AABB box = new AABB(
                        centerPos.x - radius, centerPos.y - 1.0D, centerPos.z - radius,
                        centerPos.x + radius, centerPos.y + 2.0D, centerPos.z + radius
                );
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && !e.isRemoved() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()))) {
                    ElementalGaugeManager.applyElementBypassIcd(level, target, Element.ELECTRO, pulseU, owner, damage);
                    MobHazardAvoidanceManager.reportReactionOrHazard(target, owner, centerPos, Element.ELECTRO, null);
                }

                DendroCoreManager.checkReactionsNear(level, centerPos, radius, Element.ELECTRO, owner);
                level.playSound(null, centerPos.x, centerPos.y, centerPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.45F, 1.75F);
            }
        }
    }

    public static @Nullable Vec3 findNearestHazard(ServerLevel level, Vec3 pos, double margin) {
        if (CLOUDS.isEmpty()) return null;
        Vec3 nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (CloudEntry entry : CLOUDS) {
            if (entry.level != level || !entry.cloud.isAlive() || entry.cloud.isRemoved()) continue;
            Vec3 c = entry.cloud.position();
            double maxExt = entry.radius + margin;
            if (Math.abs(pos.y - c.y) <= 2.6D
                    && Math.abs(pos.x - c.x) <= maxExt
                    && Math.abs(pos.z - c.z) <= maxExt) {
                double d2 = pos.distanceToSqr(c);
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    nearest = c;
                }
            }
        }
        return nearest;
    }

    public static void clearPlayerState(UUID id) {
        if (id == null) return;
        DRAGONFIRE_PET_UNTIL.remove(id);
        DRAGONFIRE_PET_NETHERITE.remove(id);
        DRAGONFIRE_PET_POS.remove(id);
        DRAGONFIRE_PET_NEXT_ATK.remove(id);
        DRAGONFIRE_ORB_CD_UNTIL.remove(id);
        DRAGONFIRE_ULTI_CD_UNTIL.remove(id);
        DRAGONFIRE_IDLE_STATES.remove(id);
        WITHER_CONTAGION_STATES.remove(id);
        SHULKER_SLAM_STATES.remove(id);
    }

    public static void clearAll() {
        CLOUDS.clear();
        DRAGONFIRE_PET_UNTIL.clear();
        DRAGONFIRE_PET_NETHERITE.clear();
        DRAGONFIRE_PET_POS.clear();
        DRAGONFIRE_PET_NEXT_ATK.clear();
        DRAGONFIRE_ORB_CD_UNTIL.clear();
        DRAGONFIRE_ULTI_CD_UNTIL.clear();
        DRAGONFIRE_IDLE_STATES.clear();
        WITHER_CONTAGION_STATES.clear();
        SHULKER_SLAM_STATES.clear();
        CURRENT_IMPACT_FINALE.remove();
    }
}
