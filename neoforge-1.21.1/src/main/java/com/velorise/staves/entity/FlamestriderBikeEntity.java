package com.velorise.staves.entity;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mavuika's Flamestrider Pyro motorcycle entity.
 * Supports fast ground travel, water-skimming, double-jump brief wing flight,
 * hold-to-spin Charged Attack (3m AoE), Dash Cancel combos, and Netherite Ulti slams.
 */
public class FlamestriderBikeEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_NETHERITE =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SPINNING =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GLIDING =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SPRINTING =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLUNGING =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PLUNGE_WINDUP =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ULTI_TICKS =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NA_STEP =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NA_TICKS =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.INT);

    private long lastSpinDamageTick = -100L;
    private long lastDashTick = -100L;
    private long serverDashCooldownUntil = -100L;
    private int serverDashChain = 0;
    private int serverDashTicks = 0;
    private int serverDashIframeTicks = 0;
    private boolean spinInputHeld = false;
    private boolean sprintInputHeld = false;
    private int plungeTicks = 0;
    private long plungeRecoveryUntil = 0L;
    private float serverSpinAngle = 0.0F;
    private final java.util.Map<Integer, Long> spinHitCooldowns = new java.util.HashMap<>();
    private final java.util.Map<Integer, Long> dashHitCooldowns = new java.util.HashMap<>();
    private int spinReleaseGraceTicks = 0;
    private long lastSpinStartHitResetTick = -100L;
    private int clientSpinOrbitGraceTicks = 0;
    private boolean pendingMountWhipDamage = false;
    private float spinStartYaw = 0.0F;
    private int serverContinuousSpinTicks = 0;
    private int serverSpinHitsSinceDash = 0;
    private int clientContinuousSpinTicks = 0;
    private int naComboStep = 0;
    private long lastNaTick = -100L;
    private float pendingUltiDamage = 0.0F;
    private int ultiVoiceLine = 0;
    private int ultiVoiceMaxTicks = 30;
    private boolean pendingFinisher = false;
    private boolean pendingDiscardAfterFinisher = false;
    private int clientGlideTicks = 0;
    private int clientGlideBoostTicks = 0;
    private int clientGlideCharges = 1;
    private boolean clientWasJumping = false;
    private boolean clientWasOnGround = false;
    private boolean clientWasSpinning = false;
    private double spinPivotX = 0.0D;
    private double spinPivotZ = 0.0D;
    private double spinPivotXO = 0.0D;
    private double spinPivotZO = 0.0D;
    private double camAnchorOffsetX = 0.0D;
    private double camAnchorOffsetZ = 0.0D;
    private double camAnchorOffsetXO = 0.0D;
    private double camAnchorOffsetZO = 0.0D;
    private double spinCamIntroStartX = 0.0D;
    private double spinCamIntroStartZ = 0.0D;
    private int clientNaStep = 0;
    private int clientNaTicks = 0;
    private int clientNaTotalTicks = 14;
    private int lastSyncedServerNaStep = 0;
    private int lastSyncedServerNaTicks = 0;
    private int clientFinisherYawBlendTicks = 0;
    private static final double FRONT_WHEEL_PIVOT_OFFSET = 0.86D;
    private static final double REAR_WHEEL_OFFSET = 0.82D;
    private static final double FRONT_WHEEL_INNER_RADIUS = 2.05D;
    private static final double BIKE_CENTER_SPIN_RADIUS = FRONT_WHEEL_PIVOT_OFFSET + FRONT_WHEEL_INNER_RADIUS; // 2.91m
    private static final float DONUT_FULL_ROTATION_TICKS = 11.0F; // 0.55s (11.0 ticks at 20 TPS) per 1 full 360° Donut loop
    private static final float DONUT_DEG_PER_TICK = 360.0F / DONUT_FULL_ROTATION_TICKS; // 32.727 deg/tick
    private static final double REAR_WHEEL_OUTER_RADIUS = BIKE_CENTER_SPIN_RADIUS + REAR_WHEEL_OFFSET; // 3.73m (7.46m outer diameter)
    private float spinOrbitAngle = 0.0F;
    private double spinOrbitRadius = 0.0D;
    private boolean clientWallClimbing = false;
    private int clientWallCrestTicks = 0;
    public float wallClimbPitch = 0.0F;
    public float prevWallClimbPitch = 0.0F;
    private int clientDashTicks = 0;
    private int clientDashCooldownTicks = 0;
    private int clientDashChain = 0;
    private int clientDashWindowTicks = 0;
    private int clientPostDoubleDashFinisherTicks = 0;
    private int clientJumpCooldownTicks = 0;
    private int clientPlungeRecoveryTicks = 0;
    private boolean clientWasPlunging = false;
    private Vec3 clientDashVec = Vec3.ZERO;
    public float wheelSpinAngle = 0.0F;
    public float frontWheelSpinAngle = 0.0F;
    public float rearWheelSpinAngle = 0.0F;
    public float spinYawOffset = 0.0F;
    public float spinLean = 0.0F;
    public float prevSpinLean = 0.0F;
    private double distanceTraveledBlocks = 0.0D;

    public double getDistanceTraveledBlocks() {
        return this.distanceTraveledBlocks;
    }

    public double getCurrentSpeedBlocksPerSec() {
        return Math.hypot(this.getX() - this.xo, this.getZ() - this.zo) * 20.0D;
    }

    public FlamestriderBikeEntity(EntityType<? extends FlamestriderBikeEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_NETHERITE, false);
        builder.define(DATA_SPINNING, false);
        builder.define(DATA_GLIDING, false);
        builder.define(DATA_SPRINTING, false);
        builder.define(DATA_PLUNGING, false);
        builder.define(DATA_PLUNGE_WINDUP, 0);
        builder.define(DATA_ULTI_TICKS, 0);
        builder.define(DATA_NA_STEP, 0);
        builder.define(DATA_NA_TICKS, 0);
    }

    public void setOwner(@Nullable Player owner) {
        this.entityData.set(DATA_OWNER_UUID, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public boolean isNetherite() {
        return this.entityData.get(DATA_NETHERITE);
    }

    public void setNetherite(boolean netherite) {
        this.entityData.set(DATA_NETHERITE, netherite);
    }

    public boolean isSpinning() {
        return this.entityData.get(DATA_SPINNING);
    }

    public void setSpinning(boolean spinning) {
        this.entityData.set(DATA_SPINNING, spinning);
    }

    public boolean isGliding() {
        return this.entityData.get(DATA_GLIDING);
    }

    public void setGliding(boolean gliding) {
        this.entityData.set(DATA_GLIDING, gliding);
    }

    public boolean isWallClimbing() {
        if (!com.velorise.staves.config.StavesConfig.isEnableMavuikaWallClimb()) {
            return false;
        }
        if (this.level().isClientSide()) {
            return this.clientWallClimbing;
        }
        if (this.isSpinning() || this.isPlunging() || this.getUltiTicks() > 8 || this.getActiveNaTicks() > 0) {
            return false;
        }
        Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        boolean wallAhead = this.horizontalCollision
                || !this.level().noCollision(this, this.getBoundingBox().move(fwd.x * 0.55D, 0.15D, fwd.z * 0.55D));
        return wallAhead && this.getDeltaMovement().y > 0.20D;
    }

    public boolean isBikeSprinting() {
        return this.entityData.get(DATA_SPRINTING);
    }

    public void setBikeSprinting(boolean sprinting) {
        this.entityData.set(DATA_SPRINTING, sprinting);
    }

    public void setSprintInputHeld(boolean held) {
        this.sprintInputHeld = held;
    }

    public boolean isOnDashCooldown() {
        return this.tickCount < 10 || clientDashTicks > 0 || clientDashCooldownTicks > 0;
    }

    public boolean isPlunging() {
        return this.entityData.get(DATA_PLUNGING);
    }

    public void setPlunging(boolean plunging) {
        this.entityData.set(DATA_PLUNGING, plunging);
    }

    public boolean isFinisherPlaying() {
        return this.pendingFinisher || (getActiveNaStep() == 6 && getActiveNaTicks() > 0);
    }

    public boolean isSpinInputHeld() {
        return this.spinInputHeld;
    }

    public int getPlungeWindup() {
        return this.entityData.get(DATA_PLUNGE_WINDUP);
    }

    public boolean isSpinCameraAnchored() {
        return (this.clientWasSpinning && this.spinOrbitRadius > 0.05D)
                || Math.hypot(this.camAnchorOffsetX, this.camAnchorOffsetZ) > 0.005D
                || Math.hypot(this.camAnchorOffsetXO, this.camAnchorOffsetZO) > 0.005D;
    }

    public double getSpinCameraOffsetX(float partialTick) {
        return Mth.lerp(partialTick, this.camAnchorOffsetXO, this.camAnchorOffsetX);
    }

    public double getSpinCameraOffsetZ(float partialTick) {
        return Mth.lerp(partialTick, this.camAnchorOffsetZO, this.camAnchorOffsetZ);
    }

    public double getSpinPivotX() {
        return this.spinPivotX;
    }

    public double getSpinPivotZ() {
        return this.spinPivotZ;
    }

    public double getSpinPivotX(float partialTick) {
        return Mth.lerp(partialTick, this.xo, this.getX()) + getSpinCameraOffsetX(partialTick);
    }

    public double getSpinPivotZ(float partialTick) {
        return Mth.lerp(partialTick, this.zo, this.getZ()) + getSpinCameraOffsetZ(partialTick);
    }

    public int getUltiTicks() {
        return this.entityData.get(DATA_ULTI_TICKS);
    }

    public int getNaStep() {
        return this.entityData.get(DATA_NA_STEP);
    }

    public int getNaTicks() {
        return this.entityData.get(DATA_NA_TICKS);
    }

    public int getActiveNaStep() {
        if (this.level().isClientSide() && this.clientNaTicks > 0 && this.clientNaStep > 0) {
            return this.clientNaStep;
        }
        return this.entityData.get(DATA_NA_STEP);
    }

    public int getActiveNaTicks() {
        if (this.level().isClientSide() && this.clientNaTicks > 0) {
            return this.clientNaTicks;
        }
        return this.entityData.get(DATA_NA_TICKS);
    }

    public float getSmoothNaTicks(float partialTick) {
        int ticks = getActiveNaTicks();
        if (ticks <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, (float) ticks - partialTick);
    }

    public float getFinisherProgress(float partialTick) {
        if (getActiveNaStep() != 6) {
            return 0.0F;
        }
        float naTf = getSmoothNaTicks(partialTick);
        float total = Math.max(1.0F, (float) (this.clientNaTotalTicks > 0 ? this.clientNaTotalTicks : 14));
        return Mth.clamp(1.0F - (naTf / total), 0.0F, 1.0F);
    }

    public void triggerClientFinisher(Player rider) {
        if (this.pendingDiscardAfterFinisher || this.isPlunging() || this.getUltiTicks() > 8) {
            return;
        }
        this.setSpinning(false);
        this.clientNaStep = 6;
        this.clientNaTicks = 14;
        this.clientNaTotalTicks = 14;
        this.clientFinisherYawBlendTicks = 7;
    }

    public void cancelClientFinisherToSpin() {
        if (this.clientNaStep == 6 && this.clientNaTicks >= 9) {
            this.clientNaStep = 0;
            this.clientNaTicks = 0;
            this.clientFinisherYawBlendTicks = 0;
        }
    }

    public boolean shouldOverrideFirstPersonCamera() {
        return this.getUltiTicks() > 0
                || this.isSpinning()
                || this.clientFinisherYawBlendTicks > 0
                || (getActiveNaStep() == 6 && getActiveNaTicks() > 0);
    }

    public float getFirstPersonCameraYaw(float partialTick, float playerCameraYaw) {
        float bikeYaw = Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
        int ultiT = this.getUltiTicks();
        if (ultiT > 0) {
            float u = Math.max(0.0F, (float) ultiT - partialTick);
            if (u > 8.0F) {
                if (u >= 19.0F) {
                    float p1 = Mth.clamp((30.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s1 = p1 * p1 * (3.0F - 2.0F * p1);
                    return Mth.wrapDegrees(bikeYaw + s1 * 180.0F);
                } else {
                    float p2 = Mth.clamp((19.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s2 = p2 * p2 * (3.0F - 2.0F * p2);
                    return Mth.wrapDegrees(bikeYaw + 180.0F + s2 * 180.0F);
                }
            }
            return bikeYaw;
        }
        if (this.isSpinning()) {
            float spinTicks = this.clientContinuousSpinTicks <= 0
                    ? 0.0F
                    : (this.clientContinuousSpinTicks - 1.0F) + partialTick;
            if (spinTicks < 5.0F) {
                float u = Mth.clamp(spinTicks / 5.0F, 0.0F, 1.0F);
                float smooth = u * u * (3.0F - 2.0F * u);
                return playerCameraYaw + Mth.wrapDegrees(bikeYaw - playerCameraYaw) * smooth;
            }
            return bikeYaw;
        }
        if (getActiveNaStep() == 6 && getActiveNaTicks() > 0) {
            float p = getFinisherProgress(partialTick);
            if (p < 0.65F) {
                float u = p / 0.65F;
                float smooth = u * u * (3.0F - 2.0F * u);
                float blend = 1.0F - smooth;
                return playerCameraYaw + Mth.wrapDegrees(bikeYaw - playerCameraYaw) * blend;
            }
        }
        if (this.clientFinisherYawBlendTicks > 0) {
            float blend = Mth.clamp((this.clientFinisherYawBlendTicks - partialTick) / 7.0F, 0.0F, 1.0F);
            float smooth = blend * blend * (3.0F - 2.0F * blend);
            return playerCameraYaw + Mth.wrapDegrees(bikeYaw - playerCameraYaw) * smooth;
        }
        return playerCameraYaw;
    }

    public float getFirstPersonFinisherPitchOffset(float partialTick) {
        int ultiT = this.getUltiTicks();
        if (ultiT > 0) {
            float u = Math.max(0.0F, (float) ultiT - partialTick);
            if (u > 8.0F) {
                if (u >= 19.0F) {
                    float p1 = Mth.clamp((30.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s1 = p1 * p1 * (3.0F - 2.0F * p1);
                    float theta = s1 * (float) Math.PI;
                    return -Mth.sin(theta) * 20.0F;
                } else {
                    float p2 = Mth.clamp((19.0F - u) / 11.0F, 0.0F, 1.0F);
                    float s2 = p2 * p2 * (3.0F - 2.0F * p2);
                    return Mth.sin(s2 * (float) Math.PI) * 22.0F;
                }
            } else {
                float rec = Mth.clamp(u / 8.0F, 0.0F, 1.0F);
                return rec * 8.0F;
            }
        }
        if (this.isSpinning() || getActiveNaStep() != 6 || getActiveNaTicks() <= 0) {
            return 0.0F;
        }
        float p = getFinisherProgress(partialTick);
        if (p < 0.36F) {
            float e = 0.5F - 0.5F * Mth.cos((p / 0.36F) * (float) Math.PI);
            return -8.5F * e;
        } else if (p < 0.72F) {
            float e = 0.5F - 0.5F * Mth.cos(((p - 0.36F) / 0.36F) * (float) Math.PI);
            return Mth.lerp(e, -8.5F, 9.5F);
        } else {
            float e = 0.5F - 0.5F * Mth.cos(((p - 0.72F) / 0.28F) * (float) Math.PI);
            return Mth.lerp(e, 9.5F, 0.0F);
        }
    }

    public Vec3 getFirstPersonUltiCameraOffset(float partialTick) {
        int ultiT = this.getUltiTicks();
        if (ultiT <= 0) {
            return Vec3.ZERO;
        }
        float u = Math.max(0.0F, (float) ultiT - partialTick);
        float sideX;
        float arcY;
        float backZ;
        if (u > 8.0F) {
            if (u >= 19.0F) {
                float p1 = Mth.clamp((30.0F - u) / 11.0F, 0.0F, 1.0F);
                float s1 = p1 * p1 * (3.0F - 2.0F * p1);
                float theta = s1 * (float) Math.PI;
                sideX = Mth.sin(theta) * 1.15F;
                backZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                arcY = p1 * 0.85F;
            } else {
                float p2 = Mth.clamp((19.0F - u) / 11.0F, 0.0F, 1.0F);
                float s2 = p2 * p2 * (3.0F - 2.0F * p2);
                float theta = (float) Math.PI * (1.0F + s2);
                sideX = Mth.sin(theta) * 1.15F;
                backZ = -(1.0F - Mth.cos(theta)) * 0.55F;
                arcY = (1.0F - p2 * p2) * 0.85F;
            }
        } else {
            float rec = Mth.clamp(u / 8.0F, 0.0F, 1.0F);
            sideX = 0.0F;
            arcY = 0.0F;
            backZ = rec * 0.25F;
        }
        float bikeYaw = Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
        double yawRad = Math.toRadians(bikeYaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double worldX = sideX * cos - backZ * sin;
        double worldZ = sideX * sin + backZ * cos;
        return new Vec3(worldX, arcY, worldZ);
    }

    public boolean isDashingServer() {
        return serverDashTicks > 0;
    }

    public boolean isDashIframeActive() {
        return serverDashTicks > 0 || serverDashIframeTicks > 0;
    }

    public void startUltiSlam(float ultiDamage) {
        this.pendingUltiDamage = ultiDamage;
        this.spinInputHeld = false;
        this.spinReleaseGraceTicks = 0;
        setSpinning(false);
        this.pendingFinisher = false;
        this.pendingDiscardAfterFinisher = false;
        this.pendingMountWhipDamage = false;
        this.plungeRecoveryUntil = 0L;
        this.serverDashTicks = 0;
        this.serverDashCooldownUntil = -100L;
        this.serverDashChain = 0;
        setPlunging(false);
        this.entityData.set(DATA_PLUNGE_WINDUP, 0);
        this.entityData.set(DATA_NA_STEP, 0);
        this.entityData.set(DATA_NA_TICKS, 0);
        this.ultiVoiceMaxTicks = 30;
        this.ultiVoiceLine = this.random.nextInt(20) == 0 ? 3 : this.random.nextInt(3);
        this.entityData.set(DATA_ULTI_TICKS, 30);
        Vec3 dm = this.getDeltaMovement();
        this.setDeltaMovement(dm.x, Math.max(dm.y, 0.28D), dm.z);
        this.hurtMarked = true;
        if (this.getFirstPassenger() instanceof ServerPlayer sp) {
            sp.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    public void triggerUltiAnimation() {
        this.ultiVoiceMaxTicks = 22;
        this.ultiVoiceLine = this.random.nextInt(20) == 0 ? 3 : this.random.nextInt(3);
        this.entityData.set(DATA_ULTI_TICKS, 22);
    }

    @Override
    public float maxUpStep() {
        return 1.25F;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        if (this.getUltiTicks() > 0) {
            return;
        }
        super.push(x, y, z);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (this.getUltiTicks() > 0 || this.isDashIframeActive()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity le ? le : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, 0.72D, -0.05D);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide() && passenger instanceof ServerPlayer sp) {
            ElementalGaugeManager.applyMavuikaDismountOrRemainingCooldown(sp);
        }
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return this.getControllingPassenger() instanceof Player p && p.isLocalPlayer();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        UUID ownerId = getOwnerUUID();
        if (ownerId != null && !ownerId.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        if (this.getUltiTicks() > 0 && this.getFirstPassenger() instanceof Player ultiRider) {
            ultiRider.setShiftKeyDown(false);
            ultiRider.setPose(net.minecraft.world.entity.Pose.STANDING);
        }
        super.tick();
        if (this.getUltiTicks() > 0 && this.getFirstPassenger() instanceof Player ultiRiderPost) {
            ultiRiderPost.setShiftKeyDown(false);
            ultiRiderPost.setPose(net.minecraft.world.entity.Pose.STANDING);
        }
        this.fallDistance = 0.0F;
        double stepDist = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        if (stepDist > 0.001D && stepDist < 8.0D) {
            this.distanceTraveledBlocks += stepDist;
        }
        if (this.clientDashCooldownTicks > 0) {
            this.clientDashCooldownTicks--;
        }
        if (this.clientDashWindowTicks > 0) {
            this.clientDashWindowTicks--;
            if (this.clientDashWindowTicks == 0) {
                this.clientDashChain = 0;
            }
        }

        if (!this.level().isClientSide()) {
            tickServer();
        } else {
            tickClientVisuals();
        }

        if (this.isControlledByLocalInstance() && this.getControllingPassenger() instanceof Player rider) {
            tickLocalControl(rider);
        } else if (!this.level().isClientSide() && this.getControllingPassenger() == null) {
            // Apply basic gravity/drag ONLY when unoccupied; rider movement is synced via MoveVehiclePacket
            Vec3 vel = this.getDeltaMovement();
            double vy = this.onGround() ? 0.0D : Math.max(-1.2D, vel.y - 0.08D);
            this.setDeltaMovement(vel.x * 0.85D, vy, vel.z * 0.85D);
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    private void tickServer() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        long now = sl.getServer().overworld().getGameTime();
        UUID ownerId = getOwnerUUID();
        Entity passenger = this.getFirstPassenger();
        if (passenger == null && this.getUltiTicks() > 0 && ownerId != null) {
            ServerPlayer ownerSp = sl.getServer().getPlayerList().getPlayer(ownerId);
            if (ownerSp != null && ownerSp.isAlive() && ownerSp.level() == sl) {
                ownerSp.setShiftKeyDown(false);
                ownerSp.startRiding(this, true);
                passenger = ownerSp;
            }
        }
        if (this.tickCount > 5 && (!(passenger instanceof ServerPlayer sp) || (ownerId != null && !ownerId.equals(sp.getUUID())))) {
            this.discard();
            return;
        }
        if (passenger instanceof ServerPlayer sp) {
            if (!sp.isAlive()) {
                sp.stopRiding();
                this.discard();
                return;
            }
            if (this.getUltiTicks() > 0) {
                sp.setShiftKeyDown(false);
                sp.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
            boolean holdingMavuika = (sp.getMainHandItem().getItem() instanceof ArcaneStaffItem sm
                    && sm.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                    || (sp.getOffhandItem().getItem() instanceof ArcaneStaffItem so
                    && so.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS);
            if (!holdingMavuika && this.getUltiTicks() <= 0) {
                this.spinInputHeld = false;
                setSpinning(false);
                sp.stopRiding();
                sl.playSound(null, sp.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
                this.discard();
                return;
            }
            if (ElementalGaugeManager.isMavuikaAuraActive(sp.getUUID(), now)) {
                this.pendingDiscardAfterFinisher = false;
            } else {
                if (this.spinInputHeld || isSpinning()) {
                    this.spinInputHeld = false;
                    setSpinning(false);
                    this.entityData.set(DATA_NA_STEP, 6);
                    this.entityData.set(DATA_NA_TICKS, 14);
                    this.pendingFinisher = true;
                    this.pendingDiscardAfterFinisher = true;
                    sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.45F);
                } else if (serverDashTicks > 0 || isPlunging() || getUltiTicks() > 0
                        || isFinisherPlaying()
                        || (this.pendingDiscardAfterFinisher && (this.pendingFinisher || getNaTicks() > 0))) {
                    // Let active dash, plunge, ulti, or finisher complete before dismounting
                    this.sprintInputHeld = false;
                    setBikeSprinting(false);
                    this.pendingDiscardAfterFinisher = true;
                } else {
                    sp.stopRiding();
                    this.discard();
                    return;
                }
            }
            if (serverDashTicks > 0) {
                serverDashTicks--;
            }
            if (serverDashIframeTicks > 0) {
                serverDashIframeTicks--;
                this.invulnerableTime = Math.max(this.invulnerableTime, 6);
                if (passenger instanceof LivingEntity le) {
                    le.invulnerableTime = Math.max(le.invulnerableTime, 6);
                }
            }
            int ultiT = getUltiTicks();
            if (ultiT > 0) {
                if (passenger instanceof LivingEntity le) {
                    le.invulnerableTime = Math.max(le.invulnerableTime, 4);
                }
                int nextUlti = ultiT - 1;
                this.entityData.set(DATA_ULTI_TICKS, nextUlti);
                playUltiVoiceBeat(sl, nextUlti);
                if (nextUlti == 14) {
                    sp.swing(InteractionHand.MAIN_HAND, true);
                }
                if (nextUlti > 8) {
                    sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 2.0D, this.getZ(), 6, 0.55D, 0.35D, 0.55D, 0.05D);
                } else if (nextUlti == 8 && pendingUltiDamage > 0.0F) {
                    float dmg = pendingUltiDamage;
                    pendingUltiDamage = 0.0F;
                    sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    ElementalGaugeManager.executeMavuikaUltiImpact(sl, sp, dmg);
                    com.velorise.staves.advancement.ModAdvancements.trigger(sp, com.velorise.staves.advancement.ModAdvancements.EVENT_RAT_FROM_THE_SKY);
                }
            }
            if (spinReleaseGraceTicks > 0) {
                if (spinInputHeld || serverDashTicks > 0 || isPlunging() || getUltiTicks() > 8) {
                    spinReleaseGraceTicks = 0;
                } else {
                    spinReleaseGraceTicks--;
                    if (spinReleaseGraceTicks == 0) {
                        setSpinning(false);
                        this.serverSpinAngle = 0.0F;
                        this.entityData.set(DATA_NA_STEP, 6);
                        this.entityData.set(DATA_NA_TICKS, 14);
                        this.pendingFinisher = true;
                        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.65F, 1.40F);
                        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.75F, 1.20F);
                    }
                }
            }
            int naT = getNaTicks();
            if (naT > 0) {
                int curStep = getNaStep();
                int nextNa = naT - 1;
                this.entityData.set(DATA_NA_TICKS, nextNa);
                if (curStep == 7) {
                    // Hold E 180-deg half-donut rear-wheel tail whip: emit rear tire drift trail & trigger AoE Pyro sweep at apex (nextNa == 5)
                    double prog = 1.0D - (nextNa / 10.0D);
                    double whipYawDeg = sp.getYRot() + (1.0D - prog) * 180.0D;
                    double whipRad = Math.toRadians(whipYawDeg);
                    double tailX = this.getX() + Math.sin(whipRad) * 1.35D;
                    double tailZ = this.getZ() - Math.cos(whipRad) * 1.35D;
                    sl.sendParticles(ParticleTypes.FLAME, tailX, this.getY() + 0.18D, tailZ, 4, 0.14D, 0.05D, 0.14D, 0.02D);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, tailX, this.getY() + 0.10D, tailZ, 2, 0.10D, 0.03D, 0.10D, 0.01D);
                    if (nextNa == 5) {
                        executeMountWhipImpact(sl, sp);
                    }
                } else if (pendingFinisher && curStep == 6 && nextNa == 6) {
                    if (!this.onGround() && !this.isInWater() && this.tickCount % 16 != 0) {
                        this.entityData.set(DATA_NA_TICKS, 7);
                        return;
                    }
                    pendingFinisher = false;
                    executeChargedFinisher(sl, sp);
                } else if (!pendingFinisher && !pendingDiscardAfterFinisher && curStep == 6 && nextNa <= 5 && spinInputHeld && serverDashTicks <= 0) {
                    // Genshin Finisher Cancel ("F -> C" / "F CDCD("): transition immediately out of Finisher recovery into Donut Spin
                    this.entityData.set(DATA_NA_STEP, 0);
                    this.entityData.set(DATA_NA_TICKS, 0);
                    if (now - this.lastSpinStartHitResetTick >= 4L) {
                        this.lastSpinStartHitResetTick = now;
                        this.spinHitCooldowns.clear();
                    }
                }
            }

            if (isPlunging()) {
                plungeTicks++;
                int windup = getPlungeWindup();
                if (windup > 0) {
                    this.entityData.set(DATA_PLUNGE_WINDUP, windup - 1);
                }
                sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5D, this.getZ(), 5, 0.25D, 0.25D, 0.25D, 0.04D);
                sl.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.3D, this.getZ(), 2, 0.18D, 0.15D, 0.18D, 0.01D);
                boolean hitSolid = !sl.noCollision(this, this.getBoundingBox().move(0.0D, -0.35D, 0.0D))
                        || (this.onGround() && !this.isInWater()
                                && sl.getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.15D, this.getZ())).blocksMotion());
                if (windup <= 0 && hitSolid && plungeTicks >= 6) {
                    setPlunging(false);
                    this.entityData.set(DATA_PLUNGE_WINDUP, 0);
                    plungeTicks = 0;
                    executePlungeImpact(sl, sp);
                } else if (plungeTicks > 70) {
                    setPlunging(false);
                    this.entityData.set(DATA_PLUNGE_WINDUP, 0);
                    plungeTicks = 0;
                }
            }

            // Hold left-click while riding -> Donut Spin Charged Attack (sweeps 360° around pivot)
            // Keep spinning smoothly during spinReleaseGraceTicks so re-holding Left-Click ("(" tech) never stutters or triggers Finisher early.
            boolean inCommittedFinisher = getNaStep() == 6 && getNaTicks() > 0 && (pendingFinisher || getNaTicks() > 5 || pendingDiscardAfterFinisher);
            boolean holdingSuns = (spinInputHeld || spinReleaseGraceTicks > 0)
                    && serverDashTicks <= 0 && !inCommittedFinisher && !isPlunging() && getUltiTicks() <= 8;
            setSpinning(holdingSuns);
            setBikeSprinting(sprintInputHeld && !holdingSuns && getUltiTicks() <= 8);

            if (holdingSuns) {
                serverContinuousSpinTicks++;
                serverSpinAngle = (serverSpinAngle + DONUT_DEG_PER_TICK) % 360.0F;
                performDonutSweepHit(sl, sp, now);
                // Genshin "CC" No-Finisher Combo Mechanic: spinning for 1 full 0.42s rotation (~8.4 ticks or 2 C hits) resets the 2-dash cooldown
                if (serverContinuousSpinTicks >= 8 || serverSpinHitsSinceDash >= 2) {
                    this.serverDashCooldownUntil = -100L;
                    this.serverDashChain = 0;
                }
            } else {
                serverContinuousSpinTicks = 0;
                serverSpinAngle = 0.0F;
            }
            if (serverDashTicks > 0 || (!holdingSuns && isBikeSprinting())) {
                performRamSweepHit(sl, sp, now, serverDashTicks > 0);
            }

            // Exhaust flame trail particles & high-speed (> 20 B/s) scorch/spark trails
            if (this.tickCount % 2 == 0) {
                Vec3 back = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(-0.95D);
                double speedBps = getCurrentSpeedBlocksPerSec();
                int flameCount = speedBps >= 20.0D || isBikeSprinting() ? 5 : 2;
                sl.sendParticles(ParticleTypes.FLAME,
                        this.getX() + back.x, this.getY() + 0.35D, this.getZ() + back.z,
                        flameCount, 0.14D, 0.08D, 0.14D, 0.03D);
                if (speedBps >= 20.0D || isBikeSprinting() || serverDashTicks > 0) {
                    sl.sendParticles(ParticleTypes.LAVA,
                            this.getX() + back.x, this.getY() + 0.25D, this.getZ() + back.z,
                            2, 0.18D, 0.05D, 0.18D, 0.02D);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                            this.getX() + back.x, this.getY() + 0.08D, this.getZ() + back.z,
                            2, 0.15D, 0.03D, 0.15D, 0.01D);
                }
                if (isGliding()) {
                    sl.sendParticles(ParticleTypes.LAVA,
                            this.getX() + back.x, this.getY() + 0.45D, this.getZ() + back.z,
                            2, 0.25D, 0.08D, 0.25D, 0.01D);
                }
            }
        }
    }

    private void performDonutSweepHit(ServerLevel sl, ServerPlayer sp, long now) {
        boolean neth = isNetherite();
        float dmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth)
                * (1.0F + ElementalGaugeManager.getMavuikaCrucibleChargedBonusMultiplier(sp));
        float units = neth ? 1.5F : 1.0F;

        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        Vec3 midPoint = this.position().add(0.0D, 0.4D, 0.0D);
        Vec3 frontPoint = midPoint.add(forward.scale(FRONT_WHEEL_PIVOT_OFFSET));
        Vec3 tailPoint = midPoint.subtract(forward.scale(REAR_WHEEL_OFFSET));

        AABB box = this.getBoundingBox().inflate(3.2D, 1.6D, 3.2D);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != sp);
        boolean anyHit = false;
        for (LivingEntity target : targets) {
            double dMid = target.position().add(0.0D, target.getBbHeight() * 0.4D, 0.0D).distanceToSqr(midPoint);
            double dTail = target.position().add(0.0D, target.getBbHeight() * 0.4D, 0.0D).distanceToSqr(tailPoint);
            if (dMid > 10.24D && dTail > 10.24D) continue; // 3.2m Donut Charged Attack Cyclic AoE radius
            long lastHit = spinHitCooldowns.getOrDefault(target.getId(), -100L);
            if (now - lastHit < 8L) continue; // Hit at start (0t) and end (~8.4t = 0.42s) of 1 full rotation ("CC")
            spinHitCooldowns.put(target.getId(), now);
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            sl.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.28D, 0.25D, 0.28D, 0.05D);
            anyHit = true;
        }
        if (anyHit) {
            serverSpinHitsSinceDash++;
            if (serverSpinHitsSinceDash >= 2) {
                this.serverDashCooldownUntil = -100L;
                this.serverDashChain = 0;
            }
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.25F);
        }
        // Concentric Donut flame tire trails: inner ring (front wheel) + outer ring (rear wheel)
        sl.sendParticles(ParticleTypes.FLAME, frontPoint.x, this.getY() + 0.10D, frontPoint.z, 1, 0.05D, 0.02D, 0.05D, 0.01D);
        sl.sendParticles(ParticleTypes.FLAME, tailPoint.x, this.getY() + 0.12D, tailPoint.z, 4, 0.12D, 0.04D, 0.12D, 0.02D);
        sl.sendParticles(ParticleTypes.SMOKE, tailPoint.x, this.getY() + 0.08D, tailPoint.z, 2, 0.10D, 0.02D, 0.10D, 0.01D);
    }

    private void performRamSweepHit(ServerLevel sl, ServerPlayer sp, long now, boolean isBurstDash) {
        boolean neth = isNetherite();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth);
        float dmg = baseDmg * (isBurstDash ? 1.25F : 0.90F);
        float units = neth ? 1.5F : 1.0F;

        AABB box = this.getBoundingBox().inflate(1.5D, 1.0D, 1.5D);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != sp);
        boolean anyHit = false;
        for (LivingEntity target : targets) {
            long lastHit = dashHitCooldowns.getOrDefault(target.getId(), -100L);
            if (now - lastHit < 10L) continue;
            dashHitCooldowns.put(target.getId(), now);
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            Vec3 kb = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (kb.lengthSqr() > 1.0E-4D) {
                kb = kb.normalize().scale(isBurstDash ? 0.55D : 0.32D);
                target.push(kb.x, 0.18D, kb.z);
            }
            sl.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.25D, 0.25D, 0.25D, 0.05D);
            anyHit = true;
        }
        if (anyHit) {
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.65F, isBurstDash ? 1.3F : 1.15F);
        }
        if (isBurstDash) {
            Vec3 back = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(-0.8D);
            sl.sendParticles(ParticleTypes.FLAME, this.getX() + back.x, this.getY() + 0.35D, this.getZ() + back.z, 6, 0.22D, 0.12D, 0.22D, 0.04D);
        }
    }

    private void executePlungeImpact(ServerLevel sl, ServerPlayer sp) {
        plungeRecoveryUntil = sl.getServer().overworld().getGameTime() + 12L; // 0.6s Genshin post-plunge recovery
        boolean neth = isNetherite();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth);
        float dmg = baseDmg * (1.85F + ElementalGaugeManager.getMavuikaCrucibleChargedBonusMultiplier(sp));
        float units = 1.5F;
        Vec3 center = this.position().add(0.0D, 0.4D, 0.0D);
        AABB box = new AABB(center.x - 4.0D, center.y - 1.5D, center.z - 4.0D,
                center.x + 4.0D, center.y + 2.5D, center.z + 4.0D);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != sp);
        for (LivingEntity target : targets) {
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            Vec3 kb = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (kb.lengthSqr() > 1.0E-4D) {
                kb = kb.normalize().scale(0.65D);
                target.push(kb.x, 0.32D, kb.z);
            }
            sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    14, 0.3D, 0.3D, 0.3D, 0.06D);
        }
        sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 0.6D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        spawnFierySweep(sl, this.position().add(0.0D, 0.55D, 0.0D), sp.getYRot(), 3.4D, sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.3D, this.getZ(), 42, 1.8D, 0.35D, 1.8D, 0.12D);
        sl.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.25D, this.getZ(), 16, 1.2D, 0.25D, 1.2D, 0.04D);
        // Ulti slam explosion sound removed per user request
    }

    public void triggerMountWhip() {
        triggerMountWhip(true);
    }

    public void triggerMountWhip(boolean dealDamage) {
        this.entityData.set(DATA_NA_STEP, 7);
        this.entityData.set(DATA_NA_TICKS, 10);
        this.pendingMountWhipDamage = dealDamage;
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.80F, 1.15F);
        }
    }

    private void executeMountWhipImpact(ServerLevel sl, ServerPlayer sp) {
        boolean isLeft = sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        spawnFierySweep(sl, this.position().add(0.0D, 0.45D, 0.0D), sp.getYRot(), 3.2D, isLeft);
        Vec3 fwd = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x).normalize().scale(isLeft ? -1.0D : 1.0D);
        Vec3 arcCenter = this.position().add(0.0D, 0.65D, 0.0D).add(fwd.scale(1.85D));
        com.velorise.staves.network.ModNetwork.sendElectricArc(
                sl,
                arcCenter.add(right.scale(-2.35D)),
                arcCenter.add(right.scale(2.35D)),
                8,
                isLeft ? 0xFFFF8824 : 0xFFFF8825
        );
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.90F, 1.05F);
        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.85F, 1.15F);
        if (!this.pendingMountWhipDamage) return;
        this.pendingMountWhipDamage = false;
        boolean neth = isNetherite();
        float castDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth) * 0.68F;
        float units = neth ? 1.5F : 1.0F;
        Vec3 sweepCenter = this.position().add(fwd.scale(1.8D)).add(0.0D, 0.5D, 0.0D);
        AABB sweepBox = new AABB(
                sweepCenter.x - 3.6D, sweepCenter.y - 1.5D, sweepCenter.z - 3.6D,
                sweepCenter.x + 3.6D, sweepCenter.y + 2.2D, sweepCenter.z + 3.6D
        );
        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, sweepBox,
                e -> e.isAlive() && !e.isRemoved() && e != sp && !ElementalGaugeManager.isPassiveAquaticOrAmbient(e))) {
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, castDmg);
            Vec3 kb = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (kb.lengthSqr() > 1.0E-4D) {
                kb = kb.normalize().scale(0.42D);
                target.push(kb.x, 0.20D, kb.z);
            }
            sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    12, 0.28D, 0.25D, 0.28D, 0.05D);
        }
    }

    public void performNormalAttack(ServerPlayer sp) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (getUltiTicks() > 8 || isPlunging() || this.pendingDiscardAfterFinisher) return;
        if (this.pendingFinisher && getNaStep() == 6 && getNaTicks() > 6 && getNaTicks() <= 8) return;
        this.pendingFinisher = false;
        this.spinReleaseGraceTicks = 0;
        if (sl.getServer().overworld().getGameTime() < plungeRecoveryUntil) return;
        boolean airborneHigh = !this.onGround()
                && !this.isInWater()
                && sl.noCollision(this, this.getBoundingBox().move(0.0D, -0.85D, 0.0D))
                && !sl.getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.75D, this.getZ())).getFluidState().is(FluidTags.WATER);
        if (airborneHigh) {
            setGliding(false);
            setSpinning(false);
            setBikeSprinting(false);
            setPlunging(true);
            this.entityData.set(DATA_PLUNGE_WINDUP, 5);
            plungeTicks = 0;
            ElementalGaugeManager.consumeMavuikaNightsoulFuel(sp, 2.0F);
            sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.85F, 1.35F);
            return;
        }
        long now = sl.getServer().overworld().getGameTime();
        long requiredCd = (naComboStep == 5) ? 12L : 7L; // 0.35s between hits, 0.60s recovery after strong 5th hit
        if (now - lastNaTick < requiredCd) return;
        naComboStep = (now - lastNaTick > 32L || naComboStep >= 5) ? 1 : naComboStep + 1;
        lastNaTick = now;
        ElementalGaugeManager.consumeMavuikaNightsoulFuel(sp, 1.0F);
        this.entityData.set(DATA_NA_STEP, naComboStep);
        this.entityData.set(DATA_NA_TICKS, naComboStep == 5 ? 14 : 10);
        net.minecraft.world.InteractionHand swingHand =
                (sp.getMainHandItem().getItem() instanceof ArcaneStaffItem sm
                        && sm.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                        ? net.minecraft.world.InteractionHand.MAIN_HAND
                        : ((sp.getOffhandItem().getItem() instanceof ArcaneStaffItem so
                                && so.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                                ? net.minecraft.world.InteractionHand.OFF_HAND
                                : net.minecraft.world.InteractionHand.MAIN_HAND);
        sp.swing(swingHand, true);

        boolean neth = isNetherite();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth);
        float mult = switch (naComboStep) {
            case 2 -> 1.05F;
            case 3 -> 1.15F;
            case 4 -> 1.30F;
            case 5 -> 1.65F;
            default -> 0.95F;
        };
        float dmg = baseDmg * (mult + ElementalGaugeManager.getMavuikaCrucibleNaBonusMultiplier(sp));
        float units = naComboStep == 5 ? (neth ? 1.5F : 1.0F) : 1.0F;
        double range = naComboStep == 5 ? 4.4D : (naComboStep >= 3 ? 3.8D : 3.5D);

        Vec3 forward = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 origin = this.position().add(0.0D, 0.5D, 0.0D);
        AABB box = this.getBoundingBox().inflate(range, 1.8D, range);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != sp);
        boolean anyHit = false;
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(origin);
            double distSqr = toTarget.x * toTarget.x + toTarget.z * toTarget.z;
            if (distSqr > range * range) continue;
            Vec3 horiz = new Vec3(toTarget.x, 0.0D, toTarget.z);
            if (horiz.lengthSqr() > 1.0E-4D && horiz.normalize().dot(forward) < -0.15D) continue;
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            anyHit = true;
        }
        if (anyHit) {
            ElementalGaugeManager.addMavuikaNormalAttackFightingSpirit(sp);
        }

        Vec3 horizFwd = new Vec3(forward.x, 0.0D, forward.z);
        if (horizFwd.lengthSqr() < 1.0E-4D) {
            horizFwd = Vec3.directionFromRotation(0.0F, sp.getYRot());
        }
        horizFwd = horizFwd.normalize();
        Vec3 right = new Vec3(-horizFwd.z, 0.0D, horizFwd.x).normalize();
        boolean isLeftArm = sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        if (isLeftArm) {
            right = right.scale(-1.0D);
        }
        Vec3 slashCenter = origin.add(horizFwd.scale(2.15D)).add(0.0D, 0.25D, 0.0D);
        Vec3 slashStart;
        Vec3 slashEnd;
        int arcColor;
        switch (naComboStep) {
            case 1 -> { // Hit 1: High-Right -> Low-Left (or High-Left -> Low-Right when left-handed) diagonal cleave
                slashStart = slashCenter.add(right.scale(1.65D)).add(0.0D, 1.05D, 0.0D);
                slashEnd = slashCenter.add(right.scale(-1.65D)).add(0.0D, -0.35D, 0.0D);
                arcColor = isLeftArm ? 0xFFFF8824 : 0xFFFF8825;
            }
            case 2 -> { // Hit 2: Low-Left -> High-Right (or Low-Right -> High-Left when left-handed) upward backhand
                slashStart = slashCenter.add(right.scale(-1.65D)).add(0.0D, -0.35D, 0.0D);
                slashEnd = slashCenter.add(right.scale(1.65D)).add(0.0D, 1.05D, 0.0D);
                arcColor = isLeftArm ? 0xFFFF8825 : 0xFFFF8824;
            }
            case 3 -> { // Hit 3: Wide Right -> Left (or Wide Left -> Right when left-handed) horizontal sweep
                slashStart = slashCenter.add(right.scale(1.90D)).add(0.0D, 0.25D, 0.0D);
                slashEnd = slashCenter.add(right.scale(-1.90D)).add(0.0D, 0.25D, 0.0D);
                arcColor = isLeftArm ? 0xFFFF8824 : 0xFFFF8825;
            }
            case 4 -> { // Hit 4: Low-Left -> Mid-Right (or Low-Right -> Mid-Left when left-handed) rising cross sweep
                slashStart = slashCenter.add(right.scale(-1.85D)).add(0.0D, 0.05D, 0.0D);
                slashEnd = slashCenter.add(right.scale(1.85D)).add(0.0D, 0.60D, 0.0D);
                arcColor = isLeftArm ? 0xFFFF8825 : 0xFFFF8824;
            }
            default -> { // Hit 5: Heavy Top-Right -> Bottom-Left (or Heavy Top-Left -> Bottom-Right when left-handed) overhead cleave
                slashStart = slashCenter.add(right.scale(1.30D)).add(0.0D, 1.55D, 0.0D);
                slashEnd = slashCenter.add(right.scale(-1.15D)).add(0.0D, -0.55D, 0.0D);
                arcColor = isLeftArm ? 0xFFFF8824 : 0xFFFF8825;
            }
        }
        com.velorise.staves.network.ModNetwork.sendElectricArc(
                sl, slashStart, slashEnd, naComboStep == 5 ? 9 : 7, arcColor);
        sl.sendParticles(ParticleTypes.FLAME, slashCenter.x, slashCenter.y + 0.15D, slashCenter.z,
                naComboStep == 5 ? 16 : 8, 0.55D, 0.25D, 0.55D, 0.04D);
        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
                naComboStep == 5 ? 0.85F : 0.65F, 0.9F + naComboStep * 0.08F);
    }

    private static void spawnFierySweep(ServerLevel sl, Vec3 center, float yawDeg, double arcRadius) {
        spawnFierySweep(sl, center, yawDeg, arcRadius, false);
    }

    private static void spawnFierySweep(ServerLevel sl, Vec3 center, float yawDeg, double arcRadius, boolean leftHand) {
        double sweepDir = leftHand ? -1.0D : 1.0D;
        DustParticleOptions flameDust = new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.38F, 0.05F), 1.55F);
        for (int i = -6; i <= 6; i++) {
            double angleRad = Math.toRadians(yawDeg + (i * sweepDir) * 11.0D);
            double px = center.x - Math.sin(angleRad) * arcRadius;
            double pz = center.z + Math.cos(angleRad) * arcRadius;
            sl.sendParticles(flameDust, px, center.y + 0.15D, pz, 2, 0.06D, 0.05D, 0.06D, 0.01D);
            sl.sendParticles(ParticleTypes.FLAME, px, center.y + 0.15D, pz, 2, 0.05D, 0.05D, 0.05D, 0.02D);
        }
    }

    public void setSpinInputHeld(boolean held) {
        if (this.pendingDiscardAfterFinisher) {
            this.spinInputHeld = false;
            this.spinReleaseGraceTicks = 0;
            setSpinning(false);
            return;
        }
        long now = (this.level() instanceof ServerLevel sl) ? sl.getServer().overworld().getGameTime() : this.tickCount;
        if (held) {
            // If Finisher is in its committed downward slam frames (naStep == 6, pendingFinisher == true, naTicks <= 8),
            // buffer spinInputHeld = true so Donut Spin starts right after impact (naTicks <= 5).
            boolean committedFinisherSlam = this.pendingFinisher && getNaStep() == 6 && getNaTicks() > 0 && getNaTicks() <= 8;
            if (!committedFinisherSlam) {
                // Cancel any uncommitted Finisher windup (naTicks >= 9) or post-Finisher recovery (pendingFinisher == false)
                if (getNaStep() == 6) {
                    this.pendingFinisher = false;
                    this.entityData.set(DATA_NA_STEP, 0);
                    this.entityData.set(DATA_NA_TICKS, 0);
                }
            }
            this.spinReleaseGraceTicks = 0;
            this.spinInputHeld = true;
            // Genshin Re-Hold / Continuous CA ("(" / CCD tech):
            // Starting or re-holding Charged Attack resets the Donut Cyclic hit cooldown (with a 4-tick = 0.2s anti-macro gate)
            // so each intentional C / re-hold "(" in CCD CD F strikes immediately!
            if (now - this.lastSpinStartHitResetTick >= 4L) {
                this.lastSpinStartHitResetTick = now;
                this.spinHitCooldowns.clear();
            }
            if (this.serverDashTicks <= 0 && !committedFinisherSlam && !isPlunging() && getUltiTicks() <= 8) {
                setSpinning(true);
                if (this.level() instanceof ServerLevel sl && this.getFirstPassenger() instanceof ServerPlayer sp) {
                    performDonutSweepHit(sl, sp, now);
                }
            }
        } else {
            boolean wasSpinningOrGrace = this.spinInputHeld || this.spinReleaseGraceTicks > 0 || isSpinning();
            this.spinInputHeld = false;
            if (this.serverDashTicks > 0) {
                // Released Left-Click during a Dash Cancel ("CD"): end spin cleanly without forcing a Finisher jump
                this.spinReleaseGraceTicks = 0;
                this.serverSpinAngle = 0.0F;
                setSpinning(false);
                return;
            }
            if (wasSpinningOrGrace && !this.isFinisherPlaying()) {
                // Launch Finisher ("F") immediately with zero release delay; early windup ticks (14..9) remain
                // seamlessly re-hold cancelable back into Donut Spin if player presses Left-Click again ("(" tech).
                this.spinReleaseGraceTicks = 0;
                setSpinning(false);
                this.serverSpinAngle = 0.0F;
                this.entityData.set(DATA_NA_STEP, 6);
                this.entityData.set(DATA_NA_TICKS, 14);
                this.pendingFinisher = true;
                if (this.level() instanceof ServerLevel sl) {
                    sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.65F, 1.40F);
                    sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.75F, 1.20F);
                }
            } else {
                this.spinReleaseGraceTicks = 0;
                this.serverSpinAngle = 0.0F;
                setSpinning(false);
            }
        }
    }

    /**
     * Charged Attack Finisher Slam: rider & bike leap up together and slam a fiery sword sweep forward (no explosion).
     */
    private void executeChargedFinisher(ServerLevel sl, ServerPlayer sp) {
        // Completing a Charged Attack Final ("F") refreshes the 2-dash chain cooldown and Donut hit ICD for the next CD CD( F loop
        this.serverDashCooldownUntil = -100L;
        this.serverDashChain = 0;
        this.spinHitCooldowns.clear();

        boolean neth = isNetherite();
        float dmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth)
                * (1.8F + ElementalGaugeManager.getMavuikaCrucibleChargedBonusMultiplier(sp));
        float units = neth ? 2.0F : 1.5F;

        Vec3 forward = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 hitCenter = this.position().add(forward.scale(2.3D)).add(0.0D, 0.5D, 0.0D);
        double radius = 4.5D;

        AABB box = new AABB(
                hitCenter.x - radius, hitCenter.y - 1.5D, hitCenter.z - radius,
                hitCenter.x + radius, hitCenter.y + 2.2D, hitCenter.z + radius
        );
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != sp);
        for (LivingEntity target : targets) {
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            Vec3 kb = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (kb.lengthSqr() > 1.0E-4D) {
                kb = kb.normalize().scale(0.65D);
                target.push(kb.x, 0.28D, kb.z);
            }
            sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    14, 0.32D, 0.3D, 0.32D, 0.06D);
        }

        Vec3 finFwd = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        boolean isLeftFin = sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        Vec3 finRight = new Vec3(-finFwd.z, 0.0D, finFwd.x).normalize().scale(isLeftFin ? -1.0D : 1.0D);
        Vec3 arcMid = this.position().add(0.0D, 1.05D, 0.0D).add(finFwd.scale(2.35D));
        com.velorise.staves.network.ModNetwork.sendElectricArc(
                sl,
                arcMid.add(finRight.scale(-2.45D)).add(0.0D, -0.08D, 0.0D),
                arcMid.add(finRight.scale(2.45D)).add(0.0D, 0.18D, 0.0D),
                10,
                isLeftFin ? 0xFFFF7A19 : 0xFFFF7A18);
        sl.sendParticles(ParticleTypes.FLAME, hitCenter.x, hitCenter.y, hitCenter.z, 18, 0.85D, 0.20D, 0.85D, 0.06D);
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.85F);
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.90F);
        sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.95F, 1.05F);
        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.85F, 0.85F);
    }

    /**
     * Server-side Dash Cancel: lunges bike forward, resets spin timer for instant follow-up CA.
     */
    public void handleDashCancel(ServerPlayer sp) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (this.tickCount < 10 || this.getUltiTicks() > 0) return; // Cannot dash-cancel Ulti
        long now = sl.getServer().overworld().getGameTime();
        if (now < serverDashCooldownUntil || serverDashTicks > 0) return;
        if (now - lastDashTick > 28L) {
            serverDashChain = 0;
        }
        boolean combatCancel = this.spinInputHeld || this.isSpinning() || this.spinReleaseGraceTicks > 0
                || (getNaStep() == 6 && getNaTicks() > 0);
        serverDashChain++;
        lastDashTick = now;
        int dashCd = (serverDashChain >= 2) ? 15 : 5; // 2 consecutive dashes (5t gap), then 0.75s (15t) stagger before 3rd
        if (serverDashChain >= 2) {
            serverDashChain = 0;
        }
        serverDashCooldownUntil = now + dashCd;
        serverDashTicks = combatCancel ? 3 : 5;
        serverDashIframeTicks = 10;
        sp.invulnerableTime = Math.max(sp.invulnerableTime, 10);
        this.invulnerableTime = Math.max(this.invulnerableTime, 10);
        ElementalGaugeManager.consumeMavuikaNightsoulFuel(sp, 3.0F);
        if (dashCd >= 15) {
            if (sp.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem sm
                    && sm.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                sp.getCooldowns().addCooldown(sm, dashCd);
            }
            if (sp.getOffhandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem so
                    && so.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                sp.getCooldowns().addCooldown(so, dashCd);
            }
        }
        lastSpinDamageTick = -100L;
        serverSpinHitsSinceDash = 0;
        serverContinuousSpinTicks = 0;
        dashHitCooldowns.clear();
        spinHitCooldowns.clear(); // Reset donut hit ICD so follow-up CA strikes immediately
        serverSpinAngle = 0.0F;
        spinReleaseGraceTicks = 0;
        spinInputHeld = false;
        if (!this.pendingDiscardAfterFinisher) {
            pendingFinisher = false;
            this.entityData.set(DATA_NA_STEP, 0);
            this.entityData.set(DATA_NA_TICKS, 0);
        }
        setSpinning(false);
        // Immediately deal the Dash Ram hit on frame 1 of the cancel
        performRamSweepHit(sl, sp, now, true);

        Vec3 look = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.45D, this.getZ(), 16, 0.35D, 0.2D, 0.35D, 0.08D);
        sl.sendParticles(ParticleTypes.LAVA, this.getX() - look.x * 0.6D, this.getY() + 0.35D, this.getZ() - look.z * 0.6D, 5, 0.2D, 0.1D, 0.2D, 0.02D);
        sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.35F);
    }

    private void playUltiVoiceBeat(ServerLevel sl, int nextUlti) {
        double sx = this.getFirstPassenger() != null ? this.getFirstPassenger().getX() : this.getX();
        double sy = this.getFirstPassenger() != null ? this.getFirstPassenger().getY() : this.getY();
        double sz = this.getFirstPassenger() != null ? this.getFirstPassenger().getZ() : this.getZ();
        if (nextUlti == 29) {
            sl.playSound(null, sx, sy, sz, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.75F, 1.18F);
        } else if (nextUlti == 13) {
            sl.playSound(null, sx, sy, sz, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.85F, 0.95F);
        }
    }

    public void triggerClientDash(Player rider) {
        if (this.tickCount < 10 || this.getUltiTicks() > 0 || clientDashTicks > 0 || clientDashCooldownTicks > 0) return;
        if (clientDashWindowTicks <= 0) {
            clientDashChain = 0;
        }
        boolean combatCancel = this.isSpinning() || this.spinOrbitRadius > 0.05D
                || (getNaStep() == 6 && getNaTicks() > 0)
                || (this.level().isClientSide() && net.minecraft.client.Minecraft.getInstance().options.keyAttack.isDown());
        clientDashChain++;
        clientDashTicks = combatCancel ? 3 : 5;
        if (clientDashChain >= 2) {
            clientDashChain = 0;
            clientDashWindowTicks = 0;
            clientDashCooldownTicks = 30; // 1.5s Genshin-accurate cooldown after 2nd consecutive dash
            if (combatCancel) {
                clientPostDoubleDashFinisherTicks = 11; // 3t dash + 8t NAx2 buffer window before auto-Finisher
            }
        } else {
            clientDashWindowTicks = 28;
            clientDashCooldownTicks = 5; // Quick 2nd dash follow-up for CCD CD F
            clientPostDoubleDashFinisherTicks = 0;
        }
        this.clientSpinOrbitGraceTicks = 0;
        this.clientNaStep = 0;
        this.clientNaTicks = 0;
        this.clientFinisherYawBlendTicks = combatCancel ? 7 : 0;
        this.spinCamIntroStartX = 0.0D;
        this.spinCamIntroStartZ = 0.0D;
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot()).normalize();
        this.clientDashVec = forward.scale(combatCancel ? 0.55D : 1.15D);
        this.yRotO = this.getYRot();
        this.setYRot(rider.getYRot());
    }

    private void tickClientVisuals() {
        double horizSpeed = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        double climbSpeed = this.clientWallClimbing ? Math.max(0.0D, this.getY() - this.yo) : 0.0D;
        float baseSpinDelta = (float) (Math.hypot(horizSpeed, climbSpeed) * 2.6F);
        this.wheelSpinAngle += baseSpinDelta;
        if (isSpinning()) {
            double omega = Math.toRadians(24.0D);
            double curR1 = Math.max(0.08D, this.spinOrbitRadius - FRONT_WHEEL_PIVOT_OFFSET);
            double curR2 = Math.max(FRONT_WHEEL_PIVOT_OFFSET, this.spinOrbitRadius) + REAR_WHEEL_OFFSET;
            this.frontWheelSpinAngle += (float) (omega * curR1 * 2.6D);
            this.rearWheelSpinAngle += (float) (omega * curR2 * 2.6D);
        } else {
            this.frontWheelSpinAngle += baseSpinDelta;
            this.rearWheelSpinAngle += baseSpinDelta;
        }
        this.prevSpinLean = this.spinLean;
        this.prevWallClimbPitch = this.wallClimbPitch;
        float targetClimbPitch = this.clientWallClimbing ? -74.0F : 0.0F;
        this.wallClimbPitch = Mth.lerp(this.clientWallClimbing ? 0.38F : 0.28F, this.wallClimbPitch, targetClimbPitch);
        if (Math.abs(this.wallClimbPitch) < 0.05F) {
            this.wallClimbPitch = 0.0F;
        }
        if (this.clientFinisherYawBlendTicks > 0) {
            this.clientFinisherYawBlendTicks--;
        }
        int srvStep = this.entityData.get(DATA_NA_STEP);
        int srvTicks = this.entityData.get(DATA_NA_TICKS);
        if (isSpinning()) {
            this.clientNaStep = 0;
            this.clientNaTicks = 0;
            this.clientFinisherYawBlendTicks = 0;
            this.spinYawOffset = (this.spinYawOffset + 24.0F) % 360.0F;
            float leanLerp = this.clientContinuousSpinTicks <= 1 ? 0.20F
                    : (this.clientContinuousSpinTicks == 2 ? 0.30F : 0.42F);
            this.spinLean = Mth.lerp(leanLerp, this.spinLean, 24.0F);
        } else {
            if (srvStep > 0 && srvTicks > 0) {
                boolean newServerCombo = this.clientNaStep != srvStep
                        || this.clientNaTicks <= 0
                        || (srvStep != 6 && srvTicks > this.lastSyncedServerNaTicks + 1);
                if (newServerCombo) {
                    this.clientNaStep = srvStep;
                    this.clientNaTicks = srvTicks;
                    this.clientNaTotalTicks = (srvStep == 6 || srvStep == 5) ? 14 : 10;
                    if (srvStep == 6) {
                        this.clientFinisherYawBlendTicks = Math.max(this.clientFinisherYawBlendTicks, 7);
                    }
                } else if (this.clientNaTicks > 0) {
                    this.clientNaTicks--;
                }
            } else if (this.clientNaTicks > 0) {
                this.clientNaTicks--;
                if (this.clientNaTicks == 0) {
                    this.clientNaStep = 0;
                }
            }
            this.spinYawOffset = 0.0F;
            this.spinLean = Mth.lerp(0.32F, this.spinLean, 0.0F);
            if (Math.abs(this.spinLean) < 0.05F) {
                this.spinLean = 0.0F;
            }
        }
        this.lastSyncedServerNaStep = srvStep;
        this.lastSyncedServerNaTicks = srvTicks;
    }

    private void tickLocalControl(Player rider) {
        boolean spinningNow = this.isSpinning();
        float targetYaw = rider.getYRot();
        int finNaT = getActiveNaStep() == 6 ? getActiveNaTicks() : 0;
        if (finNaT == 6) {
            this.clientDashCooldownTicks = 0;
            this.clientDashChain = 0;
            this.clientDashWindowTicks = 0;
        }
        if (this.clientPostDoubleDashFinisherTicks > 0) {
            if (this.clientNaStep >= 1 && this.clientNaStep <= 5) {
                this.clientPostDoubleDashFinisherTicks = 0;
            } else if (--this.clientPostDoubleDashFinisherTicks == 0) {
                this.triggerClientFinisher(rider);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_STOP);
            }
        }
        if (!spinningNow) {
            this.yRotO = this.getYRot();
            if (finNaT > 0 || this.clientFinisherYawBlendTicks > 0) {
                // Smoothly rotate out of any Donut orbit angle into the front-center Finisher direction
                float nextYaw = Mth.rotLerp(finNaT >= 8 ? 0.46F : 0.68F, this.getYRot(), targetYaw);
                if (Math.abs(Mth.wrapDegrees(targetYaw - nextYaw)) < 0.5F) {
                    nextYaw = targetYaw;
                }
                this.setYRot(nextYaw);
            } else {
                // Preserve yRotO from previous tick so partialTick rotLerp never snaps
                float nextYaw = Mth.rotLerp(0.72F, this.getYRot(), targetYaw);
                if (Math.abs(Mth.wrapDegrees(targetYaw - nextYaw)) < 0.5F) {
                    nextYaw = targetYaw;
                }
                this.setYRot(nextYaw);
            }
        }

        boolean waterHere = this.isInWater();
        BlockPos belowPos = BlockPos.containing(this.getX(), this.getY() - 0.12D, this.getZ());
        boolean waterBelow = this.level().getBlockState(belowPos).getFluidState().is(FluidTags.WATER);
        boolean solidBelow = !this.level().noCollision(this, this.getBoundingBox().move(0.0D, -0.15D, 0.0D));
        boolean canSkimWater = !this.isSpinning() && !this.isPlunging() && this.getNaTicks() <= 0 && this.getUltiTicks() <= 0;
        boolean onSurface = this.onGround() || solidBelow || (canSkimWater && (waterHere || waterBelow));

        if (onSurface) {
            clientGlideCharges = 1;
        }

        if (clientJumpCooldownTicks > 0) clientJumpCooldownTicks--;
        if (clientPlungeRecoveryTicks > 0) clientPlungeRecoveryTicks--;
        boolean plungingNow = this.isPlunging();
        if (plungingNow) {
            clientJumpCooldownTicks = Math.max(clientJumpCooldownTicks, 12);
        } else if (clientWasPlunging) {
            clientPlungeRecoveryTicks = 8;
            clientJumpCooldownTicks = Math.max(clientJumpCooldownTicks, 12);
        }
        clientWasPlunging = plungingNow;

        // Double-jump Space in mid-air -> deploy wings; press Space while wall-climbing -> Wall Leap; press Space while gliding -> close wings & drop
        boolean jumping = this.level().isClientSide() && isLocalJumpDown();
        if (jumping && !clientWasJumping) {
            if (this.clientWallClimbing) {
                this.clientWallClimbing = false;
                this.clientWallCrestTicks = 4;
                Vec3 fwdLeap = Vec3.directionFromRotation(0.0F, targetYaw).normalize();
                this.setDeltaMovement(fwdLeap.x * 0.42D, 0.64D, fwdLeap.z * 0.42D);
                this.setOnGround(false);
                clientJumpCooldownTicks = 10;
            } else if (onSurface) {
                if (clientJumpCooldownTicks <= 0 && clientPlungeRecoveryTicks <= 0) {
                    Vec3 curVel = this.getDeltaMovement();
                    Vec3 fwdBoost = Vec3.directionFromRotation(0.0F, targetYaw).normalize().scale(0.18D);
                    this.setDeltaMovement(curVel.x + fwdBoost.x, 0.60D, curVel.z + fwdBoost.z);
                    this.setOnGround(false);
                    clientJumpCooldownTicks = 12; // 0.6s Genshin jump cooldown to prevent bunny-hop/plunge spam
                }
            } else if (clientGlideTicks > 0) {
                clientGlideTicks = 0; // Press Space while gliding -> fold wings & drop
                clientGlideBoostTicks = 0;
            } else if (clientGlideCharges > 0) {
                clientGlideCharges = 0;
                clientGlideTicks = 60; // 3.0s level wing glide at current altitude (Genshin cap)
                clientGlideBoostTicks = 12; // 0.6s temporary forward surge when deploying wings
                Vec3 wingSurge = Vec3.directionFromRotation(0.0F, targetYaw).normalize().scale(0.96D);
                this.setDeltaMovement(wingSurge.x, 0.0D, wingSurge.z);
            }
        }
        clientWasJumping = jumping;

        if (clientGlideTicks > 0) {
            clientGlideTicks--;
            if (clientGlideBoostTicks > 0) {
                clientGlideBoostTicks--;
            }
            if (onSurface && !jumping) {
                clientGlideTicks = 0;
                clientGlideBoostTicks = 0;
            }
        } else {
            clientGlideBoostTicks = 0;
        }
        boolean glidingNow = clientGlideTicks > 0 && !onSurface;
        if (this.isGliding() != glidingNow) {
            this.setGliding(glidingNow);
        }

        // Compute horizontal movement from rider input (W/S/A/D); auto-cruise forward while gliding
        float forward = rider.zza;
        if (glidingNow && Math.abs(forward) < 0.01F) {
            forward = 1.0F;
        }
        float strafe = rider.xxa;
        boolean sprintingNow = this.isBikeSprinting();
        double speed;
        double cfgSpeedMult = com.velorise.staves.config.StavesConfig.getMavuikaBikeSpeedMultiplier();
        if (glidingNow) {
            double wingBoost = clientGlideBoostTicks > 0 ? 0.34D * (clientGlideBoostTicks / 12.0D) : 0.0D;
            speed = ((sprintingNow ? 0.74D : 0.64D) + wingBoost) * cfgSpeedMult;
        } else if (canSkimWater && (waterHere || waterBelow)) {
            speed = (sprintingNow ? 0.72D : 0.58D) * cfgSpeedMult;
        } else {
            speed = (sprintingNow ? 0.72D : 0.54D) * cfgSpeedMult;
        }
        Vec3 inputVec = Vec3.ZERO;
        if (Math.abs(forward) > 0.01F || Math.abs(strafe) > 0.01F) {
            float rad = targetYaw * ((float) Math.PI / 180.0F);
            double sin = Mth.sin(rad);
            double cos = Mth.cos(rad);
            double dx = strafe * cos - forward * sin;
            double dz = forward * cos + strafe * sin;
            double len = Math.hypot(dx, dz);
            if (len > 1.0E-4D) {
                inputVec = new Vec3(dx / len * speed, 0.0D, dz / len * speed);
            }
        }

        Vec3 prev = this.getDeltaMovement();
        double vx = Mth.lerp(0.35D, prev.x, inputVec.x);
        double vz = Mth.lerp(0.35D, prev.z, inputVec.z);
        double vy = prev.y;

        boolean spinningActive = spinningNow && clientDashTicks <= 0 && getUltiTicks() <= 6;
        double prevAnchorWorldX = this.getX() + this.camAnchorOffsetX;
        double prevAnchorWorldZ = this.getZ() + this.camAnchorOffsetZ;
        if (spinningActive) {
            this.clientWallClimbing = false;
            this.clientWallCrestTicks = 0;
            if (!clientWasSpinning) {
                // Concentric Circular Motion with 9 O'Clock (9h) Tail Start & Expanding Inner Front-Wheel Orbit:
                // Start the Donut orbit at 9h (targetYaw + 90 deg, so the rear wheel begins at the 9 o'clock position
                // relative to the rider's look direction and sweeps clockwise 9h -> 12h -> 3h -> 6h -> 9h in 0.42s).
                // At frame 0, inner orbit radius R1 starts at 0.0m and expands smoothly to FRONT_WHEEL_INNER_RADIUS (1.45m).
                float startOrbitAngle = Mth.wrapDegrees(targetYaw + 90.0F);
                double startOrbitRad = Math.toRadians(startOrbitAngle);
                spinStartYaw = this.getYRot();
                spinPivotX = this.getX() - Math.sin(startOrbitRad) * FRONT_WHEEL_PIVOT_OFFSET;
                spinPivotZ = this.getZ() + Math.cos(startOrbitRad) * FRONT_WHEEL_PIVOT_OFFSET;
                spinPivotXO = spinPivotX;
                spinPivotZO = spinPivotZ;
                spinOrbitAngle = startOrbitAngle;
                spinOrbitRadius = FRONT_WHEEL_PIVOT_OFFSET;
                clientContinuousSpinTicks = 0;
                this.spinCamIntroStartX = prevAnchorWorldX - spinPivotX;
                this.spinCamIntroStartZ = prevAnchorWorldZ - spinPivotZ;
            } else {
                spinPivotXO = spinPivotX;
                spinPivotZO = spinPivotZ;
            }
            clientContinuousSpinTicks++;
            clientSpinOrbitGraceTicks = 8;
            double stepX = inputVec.x * 0.35D;
            double stepZ = inputVec.z * 0.35D;
            if (Math.abs(stepX) > 1.0E-5D && this.level().noCollision(this, this.getBoundingBox().move(stepX, 0.0D, 0.0D))) {
                spinPivotX += stepX;
            }
            if (Math.abs(stepZ) > 1.0E-5D && this.level().noCollision(this, this.getBoundingBox().move(0.0D, 0.0D, stepZ))) {
                spinPivotZ += stepZ;
            }
            double innerExpandProg = Mth.clamp(clientContinuousSpinTicks / (double) DONUT_FULL_ROTATION_TICKS, 0.0D, 1.0D);
            double smoothInnerExpand = 0.5D * (1.0D - Math.cos(Math.PI * innerExpandProg));
            double currentInnerRadius = FRONT_WHEEL_INNER_RADIUS * smoothInnerExpand;
            spinOrbitRadius = FRONT_WHEEL_PIVOT_OFFSET + currentInnerRadius;
            // Full 360° Donut loop in 0.42s (8.4 ticks -> 42.857143 deg/tick)
            float spinEaseIn = clientContinuousSpinTicks <= 3
                    ? (0.45F + 0.55F * (clientContinuousSpinTicks / 3.0F))
                    : 1.0F;
            float angleStep = DONUT_DEG_PER_TICK * spinEaseIn;
            spinOrbitAngle = Mth.wrapDegrees(spinOrbitAngle + angleStep);
            double yawRad = Math.toRadians(spinOrbitAngle);
            double sinYaw = Math.sin(yawRad);
            double cosYaw = Math.cos(yawRad);
            double effectiveRadius = spinOrbitRadius;
            Vec3 pivotPos = new Vec3(spinPivotX, this.getY() + 0.65D, spinPivotZ);
            Vec3 desiredCenter = new Vec3(spinPivotX + sinYaw * effectiveRadius, this.getY() + 0.65D, spinPivotZ - cosYaw * effectiveRadius);
            net.minecraft.world.phys.BlockHitResult wallHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                    pivotPos, desiredCenter,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    this
            ));
            if (wallHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                effectiveRadius = Math.max(0.25D, wallHit.getLocation().distanceTo(pivotPos) - 0.45D);
            }
            // Place bike center at (FrontWheelPivot - Forward * effectiveRadius) so Front Wheel stays on inner orbit around (spinPivotX, spinPivotZ)
            double targetX = spinPivotX + sinYaw * effectiveRadius;
            double targetZ = spinPivotZ - cosYaw * effectiveRadius;
            vx = targetX - this.getX();
            vz = targetZ - this.getZ();
            float yawBlend = Mth.clamp(clientContinuousSpinTicks / 3.0F, 0.0F, 1.0F);
            float bikeYaw = Mth.rotLerp(yawBlend, spinStartYaw, spinOrbitAngle);
            this.yRotO = this.getYRot();
            this.setYRot(bikeYaw);
        } else {
            if (clientWasSpinning) {
                this.clientFinisherYawBlendTicks = Math.max(this.clientFinisherYawBlendTicks, 7);
            }
            clientContinuousSpinTicks = 0;
            this.spinCamIntroStartX = 0.0D;
            this.spinCamIntroStartZ = 0.0D;
            if (clientDashTicks > 0 || getUltiTicks() > 6) {
                clientSpinOrbitGraceTicks = 0;
                spinOrbitRadius = 0.0D;
            } else if (clientSpinOrbitGraceTicks > 0) {
                clientSpinOrbitGraceTicks--;
                spinOrbitRadius *= 0.60D;
            } else {
                spinOrbitRadius = 0.0D;
            }
            spinPivotXO = spinPivotX;
            spinPivotZO = spinPivotZ;
        }
        clientWasSpinning = spinningActive;

        if (clientPlungeRecoveryTicks > 0 && clientDashTicks <= 0 && !this.isPlunging()) {
            vx *= 0.45D;
            vz *= 0.45D;
        }
        if (clientDashTicks > 0) {
            clientDashTicks--;
            double dashScale = 1.0D;
            if (inputVec.lengthSqr() > 1.0E-4D && clientDashVec.lengthSqr() > 1.0E-4D) {
                double dot = (clientDashVec.x * inputVec.x + clientDashVec.z * inputVec.z)
                        / (clientDashVec.length() * inputVec.length());
                if (dot < -0.25D) {
                    dashScale = 0.35D;
                    if (clientDashTicks > 1) {
                        clientDashTicks = 1;
                    }
                }
            }
            vx = clientDashVec.x * dashScale;
            vz = clientDashVec.z * dashScale;
            vy = Math.max(vy, 0.04D);
        }

        int ultiT = getUltiTicks();
        Vec3 fwdClimbDir = Vec3.directionFromRotation(0.0F, targetYaw).normalize();
        boolean canWallClimb = com.velorise.staves.config.StavesConfig.isEnableMavuikaWallClimb()
                && !spinningNow && !plungingNow && ultiT <= 8 && finNaT <= 0 && clientDashTicks <= 0;
        boolean wallClimbingNow = false;
        if (canWallClimb && forward > 0.05F) {
            double probeDist = this.clientWallClimbing ? 0.68D : 0.52D;
            boolean wallLow = this.horizontalCollision
                    || !this.level().noCollision(this, this.getBoundingBox().move(fwdClimbDir.x * probeDist, 0.15D, fwdClimbDir.z * probeDist));
            boolean wallHigh = !this.level().noCollision(this, this.getBoundingBox().move(fwdClimbDir.x * probeDist, 1.28D, fwdClimbDir.z * probeDist));
            if (this.clientWallClimbing) {
                if (wallLow) {
                    wallClimbingNow = true;
                } else {
                    // Reached cliff top edge: vault smoothly onto the flat ledge
                    this.clientWallCrestTicks = 5;
                }
            } else if (wallLow && (wallHigh || this.horizontalCollision)) {
                wallClimbingNow = true;
            }
        }
        this.clientWallClimbing = wallClimbingNow;
        if (this.clientWallCrestTicks > 0) {
            this.clientWallCrestTicks--;
        }

        if (this.isPlunging()) {
            this.clientWallClimbing = false;
            this.clientWallCrestTicks = 0;
            clientGlideTicks = 0;
            if (getPlungeWindup() > 0) {
                vx *= 0.15D;
                vz *= 0.15D;
                vy = 0.08D; // Brief Genshin-style mid-air windup before diving
            } else {
                vx *= 0.25D;
                vz *= 0.25D;
                vy = onSurface ? 0.0D : -1.25D;
            }
            if (!onSurface) {
                this.setOnGround(false);
            }
        } else if (ultiT > 8) {
            vx *= 0.18D;
            vz *= 0.18D;
            if (ultiT >= 19) {
                // Half 1 (ticks 30 -> 19): steady upward climb while executing the 180-deg half-donut spin
                vy = 0.28D;
            } else {
                // Half 2 (ticks 19 -> 8): smooth downward dive back to ground, cleaving on touchdown at ultiT == 8
                float diveProgress = (19 - ultiT) / 11.0F;
                vy = -0.26D - diveProgress * 0.48D;
            }
            this.setOnGround(false);
        } else if (finNaT > 0) {
            // Higher forward leap & powerful downward slam along rider's look direction
            Vec3 fwdDir = Vec3.directionFromRotation(0.0F, targetYaw).normalize();
            if (finNaT >= 9) {
                double forwardSpeed = 0.36D;
                vx = Mth.lerp(0.45D, prev.x, fwdDir.x * forwardSpeed + inputVec.x * 0.25D);
                vz = Mth.lerp(0.45D, prev.z, fwdDir.z * forwardSpeed + inputVec.z * 0.25D);
                float liftProgress = Mth.clamp((14.0F - finNaT) / 5.0F, 0.0F, 1.0F);
                vy = Mth.lerp(liftProgress, 0.48D, 0.12D);
                this.setOnGround(false);
            } else if (finNaT >= 5) {
                vx = Mth.lerp(0.40D, prev.x, fwdDir.x * 0.24D + inputVec.x * 0.22D);
                vz = Mth.lerp(0.40D, prev.z, fwdDir.z * 0.24D + inputVec.z * 0.22D);
                vy = onSurface ? -0.04D : Math.max(-0.92D, prev.y - 0.22D);
            } else {
                vx = Mth.lerp(0.35D, prev.x, inputVec.x);
                vz = Mth.lerp(0.35D, prev.z, inputVec.z);
                vy = onSurface ? -0.04D : Math.max(-1.05D, prev.y - 0.10D);
            }
        } else if (wallClimbingNow) {
            clientGlideTicks = 0;
            clientGlideBoostTicks = 0;
            clientGlideCharges = 1;
            double climbVy = (sprintingNow ? 0.56D : 0.42D) * cfgSpeedMult;
            vx = fwdClimbDir.x * 0.24D + inputVec.x * 0.25D;
            vz = fwdClimbDir.z * 0.24D + inputVec.z * 0.25D;
            vy = climbVy;
            this.setOnGround(false);
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.FLAME,
                        this.getX() + fwdClimbDir.x * 0.55D, this.getY() + 0.35D, this.getZ() + fwdClimbDir.z * 0.55D,
                        0.0D, 0.08D, 0.0D);
            }
        } else if (this.clientWallCrestTicks > 0) {
            vx = Mth.lerp(0.55D, vx, fwdClimbDir.x * 0.50D * cfgSpeedMult);
            vz = Mth.lerp(0.55D, vz, fwdClimbDir.z * 0.50D * cfgSpeedMult);
            vy = Math.max(vy, 0.16D);
        } else if (waterHere && canSkimWater) {
            vy = Math.max(vy, 0.14D); // Skim up to water surface
        } else if (waterBelow && canSkimWater && vy <= 0.0D && !glidingNow) {
            vy = 0.0D; // Ride cleanly on water surface
            this.setOnGround(true);
        } else if (glidingNow) {
            vy = 0.0D; // Maintain current altitude while gliding; holding Space will not climb higher
        } else if (!this.onGround()) {
            vy = Math.max(-1.2D, vy - 0.08D);
        } else if (vy <= 0.0D) {
            vy = -0.04D; // Keep downward contact so Entity.move() preserves onGround = true
        }

        this.camAnchorOffsetXO = this.camAnchorOffsetX;
        this.camAnchorOffsetZO = this.camAnchorOffsetZ;
        this.setDeltaMovement(vx, vy, vz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (spinningActive) {
            // Smoothly lerp the world-space camera focus from the initial bike position into spinPivot over 10 ticks (0.5s)
            // using a C1-continuous cosine ease-in-out curve while keeping circular orbit cancellation at 100% from frame 0.
            double introWeight = clientContinuousSpinTicks >= 10
                    ? 0.0D
                    : 0.5D * (1.0D + Math.cos(Math.PI * clientContinuousSpinTicks / 10.0D));
            double anchorWorldX = this.spinPivotX + this.spinCamIntroStartX * introWeight;
            double anchorWorldZ = this.spinPivotZ + this.spinCamIntroStartZ * introWeight;
            this.camAnchorOffsetX = anchorWorldX - this.getX();
            this.camAnchorOffsetZ = anchorWorldZ - this.getZ();
        } else {
            this.camAnchorOffsetX *= 0.64D;
            this.camAnchorOffsetZ *= 0.64D;
            if (Math.hypot(this.camAnchorOffsetX, this.camAnchorOffsetZ) < 0.004D) {
                this.camAnchorOffsetX = 0.0D;
                this.camAnchorOffsetZ = 0.0D;
            }
        }

        if (waterHere || waterBelow) {
            if (Math.hypot(vx, vz) > 0.1D && this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.1D, this.getZ(), 0.0D, 0.12D, 0.0D);
            }
        }
        clientWasOnGround = this.onGround();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            setOwner(this.level().getPlayerByUUID(tag.getUUID("Owner")));
        }
        setNetherite(tag.getBoolean("Netherite"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Netherite", isNetherite());
    }

    private static boolean isLocalJumpDown() {
        return net.minecraft.client.Minecraft.getInstance().options.keyJump.isDown();
    }
}
