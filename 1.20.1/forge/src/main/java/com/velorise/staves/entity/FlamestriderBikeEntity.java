package com.velorise.staves.entity;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
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
    private static final EntityDataAccessor<Integer> DATA_DISMOUNT_TICKS =
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
    private int pendingNaHitStep = 0;
    private int pendingNaHitAtRemainingTicks = -1;
    private boolean pendingDiscardAfterFinisher = false;
    private int clientGlideTicks = 0;
    private int clientGlideBoostTicks = 0;
    private int clientGlideCharges = 1;
    private boolean clientWasJumping = false;
    private float clientLastTurnSign = 1.0F;
    private float idleStopBlend = 0.0F;
    private float prevIdleStopBlend = 0.0F;
    private double clientFinisherTargetX = 0.0D;
    private double clientFinisherTargetZ = 0.0D;
    private boolean clientFinisherHasTarget = false;
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
    private int clientNaTotalTicks = 25;
    private int lastSyncedServerNaStep = 0;
    private int lastSyncedServerNaTicks = 0;
    private int clientFinisherYawBlendTicks = 0;
    private static final double FRONT_WHEEL_PIVOT_OFFSET = 0.86D;
    private static final double REAR_WHEEL_OFFSET = 0.82D;
    private static final double FRONT_WHEEL_INNER_RADIUS = 1.15D;
    private static final double BIKE_CENTER_SPIN_RADIUS = FRONT_WHEEL_PIVOT_OFFSET + FRONT_WHEEL_INNER_RADIUS; // 2.01m (fits cleanly inside 3P camera)
    private static final float DONUT_FULL_ROTATION_TICKS = 16.0F; // 0.80s (16.0 ticks at 20 TPS) matching Genshin 0.83s/rev for silky-smooth 20 TPS orbit
    private static final float DONUT_DEG_PER_TICK = 360.0F / DONUT_FULL_ROTATION_TICKS; // 22.5 deg/tick
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
    private int clientJumpCooldownTicks = 0;
    private int clientPlungeRecoveryTicks = 0;
    private boolean clientWasPlunging = false;
    private Vec3 clientDashVec = Vec3.ZERO;
    public float wheelSpinAngle = 0.0F;
    public float prevFrontWheelSpinAngle = 0.0F;
    public float frontWheelSpinAngle = 0.0F;
    public float prevRearWheelSpinAngle = 0.0F;
    public float rearWheelSpinAngle = 0.0F;
    public float prevSteeringAngle = 0.0F;
    public float steeringAngle = 0.0F;
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
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_NETHERITE, false);
        this.entityData.define(DATA_SPINNING, false);
        this.entityData.define(DATA_GLIDING, false);
        this.entityData.define(DATA_SPRINTING, false);
        this.entityData.define(DATA_PLUNGING, false);
        this.entityData.define(DATA_PLUNGE_WINDUP, 0);
        this.entityData.define(DATA_ULTI_TICKS, 0);
        this.entityData.define(DATA_NA_STEP, 0);
        this.entityData.define(DATA_NA_TICKS, 0);
        this.entityData.define(DATA_DISMOUNT_TICKS, 0);
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

    public com.velorise.staves.fsm.BikeActionState getActionState() {
        return com.velorise.staves.fsm.BikeActionState.resolveActiveState(
                this.getUltiTicks(),
                this.isPlunging(),
                this.getPlungeWindup(),
                this.clientPlungeRecoveryTicks,
                this.isDashingServer() || this.clientDashTicks > 0,
                this.isSpinning(),
                this.getActiveNaStep(),
                this.getActiveNaTicks(),
                this.clientWallCrestTicks > 0,
                this.isWallClimbing(),
                this.isGliding(),
                this.isInWater(),
                this.isBikeSprinting(),
                !this.onGround(),
                this.getDeltaMovement().horizontalDistanceSqr() > 0.0025D
        );
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

    public int getDismountTicks() {
        return this.entityData.get(DATA_DISMOUNT_TICKS);
    }

    public void beginSmoothDismount(@Nullable ServerPlayer sp) {
        if (this.level().isClientSide() || getDismountTicks() > 0) return;
        this.spinInputHeld = false;
        setSpinning(false);
        this.entityData.set(DATA_NA_STEP, 0);
        this.entityData.set(DATA_NA_TICKS, 0);
        this.entityData.set(DATA_DISMOUNT_TICKS, 9);
        this.noPhysics = true;
        Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        if (sp != null && sp.isAlive()) {
            if (sp.getVehicle() == this) {
                sp.stopRiding();
            }
            // Genshin smooth aerial vault dismount: gentle upward hop & forward carry, zero fall snap
            sp.setDeltaMovement(fwd.x * 0.16D, 0.34D, fwd.z * 0.16D);
            sp.hurtMarked = true;
            sp.resetFallDistance();
            com.velorise.staves.network.ModNetwork.sendStaffAnimation(sp, com.velorise.staves.client.ClientAuraTracker.ANIM_MAVUIKA_DISMOUNT);
        }
        this.setDeltaMovement(fwd.x * 0.18D, 0.0D, fwd.z * 0.18D);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5D, this.getZ(), 18, 0.45D, 0.28D, 0.45D, 0.06D);
            sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.55F, 1.35F);
        }
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
        float total = Math.max(1.0F, (float) (this.clientNaTotalTicks > 0 ? this.clientNaTotalTicks : 27));
        return Mth.clamp(1.0F - (naTf / total), 0.0F, 1.0F);
    }

    public void triggerClientFinisher(Player rider) {
        if (this.pendingDiscardAfterFinisher || this.isPlunging() || this.getUltiTicks() > 8) {
            return;
        }
        this.setSpinning(false);
        this.clientNaStep = 6;
        this.clientNaTicks = 27;
        this.clientNaTotalTicks = 27;
        this.clientFinisherYawBlendTicks = 10;
        if (this.spinOrbitRadius > 0.05D) {
            this.clientFinisherTargetX = this.spinPivotX;
            this.clientFinisherTargetZ = this.spinPivotZ;
        } else {
            this.clientFinisherTargetX = this.getX();
            this.clientFinisherTargetZ = this.getZ();
        }
        this.clientFinisherHasTarget = true;
    }

    public void cancelClientFinisherToSpin() {
        if (this.clientNaStep == 6 && this.clientNaTicks <= 10) {
            this.clientNaStep = 0;
            this.clientNaTicks = 0;
            this.clientFinisherYawBlendTicks = 0;
        }
    }

    public boolean shouldOverrideFirstPersonCamera() {
        return this.getUltiTicks() > 0
                || this.isSpinning()
                || this.clientDashTicks > 0
                || this.clientFinisherYawBlendTicks > 0
                || (getActiveNaStep() == 6 && getActiveNaTicks() > 0)
                || com.velorise.staves.fsm.BikePoseBlender.hasActivePose(this.getId());
    }

    public float getFirstPersonCameraYaw(float partialTick, float playerCameraYaw) {
        float bikeYaw = Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
        int ultiT = this.getUltiTicks();
        if (ultiT > 0) {
            com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot rPose =
                    new com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot();
            com.velorise.staves.fsm.BikePoseBlender.evalUltiRiderPose(ultiT, partialTick, rPose);
            return Mth.wrapDegrees(bikeYaw - rPose.yawDeg);
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
            com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot rPose =
                    new com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot();
            com.velorise.staves.fsm.BikePoseBlender.evalUltiRiderPose(ultiT, partialTick, rPose);
            return rPose.pitchDeg * 0.65F;
        }
        if (this.isSpinning()) {
            return 0.0F;
        }
        if (getActiveNaStep() != 6 || getActiveNaTicks() <= 0) {
            if (this.clientDashTicks > 0 || com.velorise.staves.fsm.BikePoseBlender.hasActivePose(this.getId())) {
                com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot pSnap =
                        com.velorise.staves.fsm.BikePoseBlender.getCurrentPose(this.getId());
                if (pSnap != null && pSnap.pitchDeg < -0.5F) {
                    // Sync 1st-person camera nose-up tilt smoothly with 3rd-person Dash wheelie
                    return Mth.clamp(pSnap.pitchDeg * 0.55F, -12.0F, 0.0F);
                }
            }
            return 0.0F;
        }
        float p = getFinisherProgress(partialTick);
        if (p < 0.11F) {
            float u = p / 0.11F;
            return Mth.sin(u * (float) Math.PI) * 1.2F;
        } else if (p < 0.37F) {
            // Phase 1: rider vaults high above bike
            float u = (p - 0.11F) / 0.26F;
            float e = 1.0F - (1.0F - u) * (1.0F - u);
            return Mth.lerp(e, 0.0F, -11.0F);
        } else if (p < 0.52F) {
            // Phase 2: plunge slam onto hitmark (p = 0.52)
            float u = (p - 0.37F) / 0.15F;
            float e = u * u;
            return Mth.lerp(e, -11.0F, 10.5F);
        } else if (p < 0.62F) {
            // Phase 3a: pop into wheelie nose-up
            float u = (p - 0.52F) / 0.10F;
            float e = 1.0F - (1.0F - u) * (1.0F - u);
            return Mth.lerp(e, 10.5F, -9.0F);
        } else if (p < 0.84F) {
            float u = (p - 0.62F) / 0.22F;
            return Mth.lerp(u, -9.0F, -7.2F);
        } else {
            float u = Mth.clamp((p - 0.84F) / 0.16F, 0.0F, 1.0F);
            float e = u * u * (3.0F - 2.0F * u);
            return Mth.lerp(e, -7.2F, 0.0F);
        }
    }

    /**
     * Vertical 1st-person camera offset for non-physics visual bike arcs:
     * - NA5 front-flip slam (+0.88m leap -> u^2.4 drop -> -0.08m squash)
     * - Finisher landing suspension squash (-0.08m at p = 0.48..0.58, layered on top of real +1.40m physics leap)
     */
    public float getFirstPersonActionCameraOffsetY(float partialTick) {
        if (this.isSpinning() || this.getUltiTicks() > 0) {
            return 0.0F;
        }
        int step = getActiveNaStep();
        int naT = getActiveNaTicks();
        if (naT <= 0) {
            if (this.clientDashTicks > 0 || com.velorise.staves.fsm.BikePoseBlender.hasActivePose(this.getId())) {
                com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot pSnap =
                        com.velorise.staves.fsm.BikePoseBlender.getCurrentPose(this.getId());
                if (pSnap != null && Math.abs(pSnap.offsetY) > 0.002F) {
                    return pSnap.offsetY;
                }
            }
            return 0.0F;
        }
        if (step == 8) {
            float p = Mth.clamp(1.0F - (getSmoothNaTicks(partialTick) / 10.0F), 0.0F, 1.0F);
            if (p < 0.65F) {
                return Mth.sin((p / 0.65F) * (float) Math.PI) * 0.24F;
            }
            return 0.0F;
        }
        if (step == 6) {
            float p = getFinisherProgress(partialTick);
            if (p >= 0.11F && p < 0.37F) {
                float u = (p - 0.11F) / 0.26F;
                return (1.0F - (1.0F - u) * (1.0F - u)) * 1.15F;
            } else if (p >= 0.37F && p < 0.52F) {
                float u = (p - 0.37F) / 0.15F;
                return (1.0F - (float) Math.pow(u, 2.3D)) * 1.15F;
            } else if (p >= 0.52F && p < 0.60F) {
                float u = (p - 0.52F) / 0.08F;
                return -Mth.sin(u * (float) Math.PI) * 0.10F;
            }
            return 0.0F;
        }
        if (step == 5) {
            float naTf = getSmoothNaTicks(partialTick);
            float p = Mth.clamp(1.0F - (naTf / 23.0F), 0.0F, 1.0F);
            if (p < 0.32F) {
                float u = p / 0.32F;
                return (1.0F - (1.0F - u) * (1.0F - u)) * 1.45F;
            } else if (p < 0.52F) {
                float u = (p - 0.32F) / 0.20F;
                return (1.0F - (float) Math.pow(u, 2.5D)) * 1.45F;
            } else if (p < 0.62F) {
                float u = (p - 0.52F) / 0.10F;
                return -Mth.sin(u * (float) Math.PI) * 0.14F;
            }
        }
        return 0.0F;
    }

    public Vec3 getFirstPersonUltiCameraOffset(float partialTick) {
        int ultiT = this.getUltiTicks();
        if (ultiT <= 0) {
            return Vec3.ZERO;
        }
        com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot rPose =
                new com.velorise.staves.fsm.BikePoseBlender.BikePoseSnapshot();
        com.velorise.staves.fsm.BikePoseBlender.evalUltiRiderPose(ultiT, partialTick, rPose);
        float bikeYaw = Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
        double yawRad = Math.toRadians(bikeYaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double worldX = rPose.offsetX * cos - rPose.offsetZ * sin;
        double worldZ = rPose.offsetX * sin + rPose.offsetZ * cos;
        return new Vec3(worldX, rPose.offsetY, worldZ);
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
        this.pendingNaHitStep = 0;
        this.pendingNaHitAtRemainingTicks = -1;
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
        this.ultiVoiceMaxTicks = 40;
        this.ultiVoiceLine = this.random.nextInt(20) == 0 ? 3 : this.random.nextInt(3);
        this.entityData.set(DATA_ULTI_TICKS, 40);
        this.hurtMarked = true;
    }

    public void triggerUltiAnimation() {
        this.ultiVoiceMaxTicks = 40;
        this.ultiVoiceLine = this.random.nextInt(20) == 0 ? 3 : this.random.nextInt(3);
        this.entityData.set(DATA_ULTI_TICKS, 40);
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
    public double getPassengersRidingOffset() {
        return 0.72D;
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

        double stepDist = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        if (stepDist > 0.001D && stepDist < 8.0D) {
            this.distanceTraveledBlocks += stepDist;
        }
        if (this.level().isClientSide()) {
            tickClientWheelAndSteeringVisuals();
        }
    }

    private void tickServer() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        int dismountT = getDismountTicks();
        if (dismountT > 0) {
            Vec3 vel = this.getDeltaMovement();
            this.setPos(this.getX() + vel.x, this.getY(), this.getZ() + vel.z);
            this.setDeltaMovement(vel.x * 0.82D, 0.0D, vel.z * 0.82D);
            sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.45D, this.getZ(), 7, 0.55D, 0.25D, 0.55D, 0.04D);
            int nextDismount = dismountT - 1;
            this.entityData.set(DATA_DISMOUNT_TICKS, nextDismount);
            if (nextDismount <= 0) {
                this.discard();
            }
            return;
        }
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
            ServerPlayer ownerSp = ownerId != null ? sl.getServer().getPlayerList().getPlayer(ownerId) : null;
            if (ownerSp != null && ownerSp.isAlive() && ownerSp.level() == sl && ownerSp.distanceToSqr(this) < 16.0D) {
                beginSmoothDismount(ownerSp);
            } else {
                this.discard();
            }
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
            boolean holdingMavuika = ArcaneStaffItem.getEffectiveKind(sp.getMainHandItem()) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS
                    || ArcaneStaffItem.getEffectiveKind(sp.getOffhandItem()) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS;
            if (!holdingMavuika && this.getUltiTicks() <= 0) {
                beginSmoothDismount(sp);
                return;
            }
            if (ElementalGaugeManager.isMavuikaAuraActive(sp.getUUID(), now)) {
                this.pendingDiscardAfterFinisher = false;
            } else {
                if (this.spinInputHeld || isSpinning()) {
                    this.spinInputHeld = false;
                    setSpinning(false);
                    this.entityData.set(DATA_NA_STEP, 6);
                    this.entityData.set(DATA_NA_TICKS, 27);
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
                    beginSmoothDismount(sp);
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
                int elapsed = 40 - nextUlti;
                Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
                if (elapsed <= 8) {
                    // Phase 1: Rising exhaust flame trail behind launching bike
                    double frac = elapsed / 8.0D;
                    Vec3 pPos = this.position().add(fwd.scale(frac * 1.8D)).add(0.0D, 0.4D + frac * 4.4D, 0.0D);
                    sl.sendParticles(ParticleTypes.FLAME, pPos.x, pPos.y, pPos.z, 8, 0.25D, 0.20D, 0.25D, 0.04D);
                } else if (elapsed == 9) {
                    // Phase 2: Solar cross-flare burst at sky apex when Steve ejects off the bike
                    Vec3 apex = this.position().add(fwd.scale(1.8D)).add(0.0D, 5.1D, 0.0D);
                    sl.sendParticles(ParticleTypes.FLASH, apex.x, apex.y, apex.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    sl.sendParticles(ParticleTypes.CRIT, apex.x, apex.y, apex.z, 18, 0.45D, 0.45D, 0.45D, 0.22D);
                    sl.sendParticles(ParticleTypes.FLAME, apex.x, apex.y, apex.z, 16, 0.40D, 0.40D, 0.40D, 0.10D);
                    sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9F, 1.45F);
                } else if (elapsed >= 14 && elapsed <= 22) {
                    // Phase 3: Vertical meteor streak falling straight down to center
                    double frac = Math.min(1.0D, (elapsed - 14.0D) / 6.0D);
                    double py = 5.0D - frac * 4.2D;
                    sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + py, this.getZ(), 10, 0.18D, 0.35D, 0.18D, 0.03D);
                    sl.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + py, this.getZ(), 3, 0.12D, 0.20D, 0.12D, 0.02D);
                    if (elapsed == 22) {
                        sp.swing(InteractionHand.MAIN_HAND, true);
                    }
                } else if (elapsed == 24 && pendingUltiDamage > 0.0F) {
                    // Phase 4: Sunfell Slice 360-deg cleave hitmark!
                    float dmg = pendingUltiDamage;
                    pendingUltiDamage = 0.0F;
                    sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    for (int a = 0; a < 360; a += 18) {
                        Vec3 rDir = Vec3.directionFromRotation(0.0F, this.getYRot() + a);
                        Vec3 rPos = this.position().add(rDir.scale(4.2D));
                        sl.sendParticles(ParticleTypes.FLAME, rPos.x, this.getY() + 0.35D, rPos.z, 3, 0.16D, 0.08D, 0.16D, 0.05D);
                    }
                    ElementalGaugeManager.executeMavuikaUltiImpact(sl, sp, dmg);
                    com.velorise.staves.advancement.ModAdvancements.trigger(sp, com.velorise.staves.advancement.ModAdvancements.EVENT_RAT_FROM_THE_SKY);
                } else if (elapsed == 26) {
                    // Phase 5: Bike re-summon wheelie spark burst underneath Steve
                    sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.35D, this.getZ(), 16, 0.65D, 0.20D, 0.65D, 0.06D);
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
                        this.entityData.set(DATA_NA_TICKS, 27);
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
                    sl.sendParticles(ParticleTypes.FLAME, tailX, this.getY() + 0.12D, tailZ, 1, 0.06D, 0.02D, 0.06D, 0.01D);
                    if (nextNa == 5) {
                        executeMountWhipImpact(sl, sp);
                    }
                } else if (pendingNaHitStep >= 1 && pendingNaHitStep <= 5 && curStep == pendingNaHitStep && nextNa == pendingNaHitAtRemainingTicks) {
                    int hitStep = pendingNaHitStep;
                    pendingNaHitStep = 0;
                    pendingNaHitAtRemainingTicks = -1;
                    executeNormalAttackHitmark(sl, sp, hitStep);
                } else if (pendingFinisher && curStep == 6 && nextNa == 20) {
                    // Crisp golden-fire star burst at rider aerial vault apex (zero giant white FLASH billboard!)
                    sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.35D, this.getZ(), 10, 0.22D, 0.22D, 0.22D, 0.14D);
                    sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 2.20D, this.getZ(), 8, 0.24D, 0.18D, 0.24D, 0.06D);
                } else if (pendingFinisher && curStep == 6 && nextNa == 14) {
                    pendingFinisher = false;
                    executeChargedFinisher(sl, sp);
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
            boolean inCommittedFinisher = getNaStep() == 6 && getNaTicks() > 0;
            boolean holdingSuns = (spinInputHeld || spinReleaseGraceTicks > 0)
                    && serverDashTicks <= 0 && !inCommittedFinisher && !isPlunging() && getUltiTicks() <= 8;
            setSpinning(holdingSuns);
            setBikeSprinting(sprintInputHeld && !holdingSuns && getUltiTicks() <= 8);

            if (holdingSuns) {
                serverContinuousSpinTicks++;
                serverSpinAngle = (serverSpinAngle + DONUT_DEG_PER_TICK) % 360.0F;
                performDonutSweepHit(sl, sp, now);
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

        if (now % 4L == 0L) {
            com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(sl, midPoint, 3.0D, false, true);
        }
        AABB box = this.getBoundingBox().inflate(3.2D, 1.6D, 3.2D);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> isValidBikeCombatTarget(sp, e));
        boolean anyHit = false;
        for (LivingEntity target : targets) {
            double dMid = target.position().add(0.0D, target.getBbHeight() * 0.4D, 0.0D).distanceToSqr(midPoint);
            double dTail = target.position().add(0.0D, target.getBbHeight() * 0.4D, 0.0D).distanceToSqr(tailPoint);
            if (dMid > 10.24D && dTail > 10.24D) continue; // 3.2m Donut Charged Attack Cyclic AoE radius
            long lastHit = spinHitCooldowns.getOrDefault(target.getId(), -100L);
            if (now - lastHit < 11L) continue; // 1 hit per full 360-deg rotation (11 ticks = 0.55s)
            spinHitCooldowns.put(target.getId(), now);
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            sl.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.28D, 0.25D, 0.28D, 0.05D);
            anyHit = true;
        }
        if (anyHit) {
            serverSpinHitsSinceDash++;
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
                e -> isValidBikeCombatTarget(sp, e));
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
        com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(sl, center, 3.8D);
        AABB box = new AABB(center.x - 4.0D, center.y - 1.5D, center.z - 4.0D,
                center.x + 4.0D, center.y + 2.5D, center.z + 4.0D);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> isValidBikeCombatTarget(sp, e));
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

    public void triggerMountCatch() {
        this.entityData.set(DATA_NA_STEP, 8);
        this.entityData.set(DATA_NA_TICKS, 10);
        this.pendingMountWhipDamage = false;
        if (this.level() instanceof ServerLevel sl) {
            Vec3 back = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize().scale(-1.6D);
            sl.sendParticles(ParticleTypes.FLAME, this.getX() + back.x, this.getY() + 0.5D, this.getZ() + back.z, 18, 0.45D, 0.25D, 0.45D, 0.07D);
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.65F, 1.25F);
        }
    }

    private void executeMountWhipImpact(ServerLevel sl, ServerPlayer sp) {
        spawnFierySweep(sl, this.position().add(0.0D, 0.45D, 0.0D), sp.getYRot(), 3.2D, false);
        Vec3 fwd = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x).normalize();
        Vec3 arcCenter = this.position().add(0.0D, 0.65D, 0.0D).add(fwd.scale(1.85D));
        com.velorise.staves.network.ModNetwork.sendElectricArc(
                sl,
                arcCenter.add(right.scale(-2.35D)),
                arcCenter.add(right.scale(2.35D)),
                8,
                0xFFFF8824
        );
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.90F, 1.05F);
        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.85F, 1.15F);
        if (!this.pendingMountWhipDamage) return;
        this.pendingMountWhipDamage = false;
        boolean neth = isNetherite();
        float castDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth) * 0.68F;
        float units = neth ? 1.5F : 1.0F;
        Vec3 sweepCenter = this.position().add(fwd.scale(1.8D)).add(0.0D, 0.5D, 0.0D);
        com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(sl, sweepCenter, 3.2D);
        AABB sweepBox = new AABB(
                sweepCenter.x - 3.6D, sweepCenter.y - 1.5D, sweepCenter.z - 3.6D,
                sweepCenter.x + 3.6D, sweepCenter.y + 2.2D, sweepCenter.z + 3.6D
        );
        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, sweepBox,
                e -> isValidBikeCombatTarget(sp, e) && !ElementalGaugeManager.isPassiveAquaticOrAmbient(e))) {
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
        if (getNaStep() == 6 && getNaTicks() > 0) return;
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
        com.velorise.staves.fsm.BikeActionState prevNaState = com.velorise.staves.fsm.BikeActionState.fromNaStep(Math.max(1, naComboStep));
        long requiredCd = com.velorise.staves.fsm.BikeFrameData.get(prevNaState).naCancelTick();
        if (now - lastNaTick < requiredCd) return;
        naComboStep = (now - lastNaTick > 40L || naComboStep >= 5) ? 1 : naComboStep + 1;
        lastNaTick = now;
        ElementalGaugeManager.consumeMavuikaNightsoulFuel(sp, 1.0F);
        com.velorise.staves.fsm.BikeFrameData.FrameSpec naSpec =
                com.velorise.staves.fsm.BikeFrameData.get(com.velorise.staves.fsm.BikeActionState.fromNaStep(naComboStep));
        int dur = naSpec.durationTicks();
        this.entityData.set(DATA_NA_STEP, naComboStep);
        this.entityData.set(DATA_NA_TICKS, dur);
        this.pendingNaHitStep = naComboStep;
        this.pendingNaHitAtRemainingTicks = Math.max(0, dur - naSpec.hitmarkTick());
        // Only swing weapon hand on NA1 & NA2 (Genshin: sword dissolves at T00 of NA3, both hands grip handlebars for NA3..NA5)
        if (naComboStep <= 2) {
            net.minecraft.world.InteractionHand swingHand =
                    (ArcaneStaffItem.getEffectiveKind(sp.getMainHandItem()) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                            ? net.minecraft.world.InteractionHand.MAIN_HAND
                            : ((ArcaneStaffItem.getEffectiveKind(sp.getOffhandItem()) == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                                    ? net.minecraft.world.InteractionHand.OFF_HAND
                                    : net.minecraft.world.InteractionHand.MAIN_HAND);
            sp.swing(swingHand, true);
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 horizFwd = new Vec3(forward.x, 0.0D, forward.z);
        if (horizFwd.lengthSqr() < 1.0E-4D) {
            horizFwd = Vec3.directionFromRotation(0.0F, sp.getYRot());
        }
        horizFwd = horizFwd.normalize();
        Vec3 right = new Vec3(-horizFwd.z, 0.0D, horizFwd.x).normalize();
        // Subtle Genshin-style per-hit Root Motion nudge so the bike drifts/advances organically through the combo
        if (this.onGround() || naComboStep == 5) {
            Vec3 rootNudge = switch (naComboStep) {
                case 1 -> horizFwd.scale(0.24D).add(right.scale(-0.08D));
                case 2 -> horizFwd.scale(0.32D);
                case 3 -> horizFwd.scale(0.14D);
                case 4 -> horizFwd.scale(0.28D).add(right.scale(0.08D));
                default -> horizFwd.scale(0.42D);
            };
            Vec3 curVel = this.getDeltaMovement();
            this.setDeltaMovement(curVel.x * 0.45D + rootNudge.x, curVel.y, curVel.z * 0.45D + rootNudge.z);
        }
        // Golden spark burst at T00 of NA3 showing claymore dissolving into Pyro embers
        if (naComboStep == 3) {
            Vec3 handPos = this.position().add(right.scale(0.55D)).add(0.0D, 1.15D, 0.0D);
            sl.sendParticles(ParticleTypes.FLAME, handPos.x, handPos.y, handPos.z, 10, 0.22D, 0.22D, 0.22D, 0.04D);
            sl.sendParticles(ParticleTypes.CRIT, handPos.x, handPos.y, handPos.z, 6, 0.18D, 0.18D, 0.18D, 0.08D);
        }
        sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                0.45F, 1.05F + naComboStep * 0.06F);
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
            // Once in CA_SPIN_FINISHER (naStep == 6), LMB CANNOT cancel Finisher (only Dash or Burst can cancel it).
            boolean committedFinisherSlam = (getNaStep() == 6 && getNaTicks() > 0);
            this.spinReleaseGraceTicks = 0;
            this.spinInputHeld = true;
            // Genshin Re-Hold / Continuous CA ("(" / CCD tech):
            // Starting or re-holding Charged Attack resets the Donut Cyclic hit cooldown (with a 4-tick = 0.2s anti-macro gate)
            // so each intentional C / re-hold "(" in CCD CD F strikes immediately!
            if (now - this.lastSpinStartHitResetTick >= 4L) {
                this.lastSpinStartHitResetTick = now;
                this.spinHitCooldowns.clear();
            }
            // Allow dC charge-cancel at Dash hitmark (tick 2 -> serverDashTicks <= 1) without swallowing hit C
            if (this.serverDashTicks > 0 && this.serverDashTicks <= 1) {
                this.serverDashTicks = 0;
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
            if (this.serverDashTicks > 0 || (!this.onGround() && !this.isInWater())) {
                // Released Left-Click during Dash Cancel ("CD") or Jump Cancel: end spin cleanly without Finisher
                this.spinReleaseGraceTicks = 0;
                this.serverSpinAngle = 0.0F;
                setSpinning(false);
                return;
            }
            if (wasSpinningOrGrace && !this.isFinisherPlaying()) {
                // Launch Finisher ("F") immediately with zero release delay; early windup ticks (25..19) remain
                // seamlessly re-hold cancelable back into Donut Spin if player presses Left-Click again ("(" tech).
                this.spinReleaseGraceTicks = 0;
                setSpinning(false);
                this.serverSpinAngle = 0.0F;
                this.entityData.set(DATA_NA_STEP, 6);
                this.entityData.set(DATA_NA_TICKS, 27);
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
    private void executeNormalAttackHitmark(ServerLevel sl, ServerPlayer sp, int step) {
        boolean neth = isNetherite();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth);
        float mult = switch (step) {
            case 2 -> 1.05F;
            case 3 -> 1.15F;
            case 4 -> 1.30F;
            case 5 -> 1.65F;
            default -> 0.95F;
        };
        float dmg = baseDmg * (mult + ElementalGaugeManager.getMavuikaCrucibleNaBonusMultiplier(sp));
        float units = step == 5 ? (neth ? 1.5F : 1.0F) : 1.0F;
        double range = step == 5 ? 4.4D : (step >= 3 ? 3.8D : 3.5D);

        Vec3 forward = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 origin = this.position().add(0.0D, 0.5D, 0.0D);
        AABB box = this.getBoundingBox().inflate(range, 1.8D, range);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> isValidBikeCombatTarget(sp, e));
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
        com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(
                sl, origin.add(forward.scale(range * 0.55D)), Math.min(3.0D, range * 0.65D), false, true);
        if (anyHit) {
            ElementalGaugeManager.addMavuikaNormalAttackFightingSpirit(sp);
        }
        // Hitmark-synced authentic Genshin VFX per step (sword slashes ONLY on NA1 & NA2; bike Pyro impacts on NA3..NA5)
        Vec3 horizFwd = new Vec3(forward.x, 0.0D, forward.z);
        if (horizFwd.lengthSqr() < 1.0E-4D) horizFwd = Vec3.directionFromRotation(0.0F, sp.getYRot());
        horizFwd = horizFwd.normalize();
        Vec3 right = new Vec3(-horizFwd.z, 0.0D, horizFwd.x).normalize();
        boolean isLeftArm = sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        Vec3 armRight = isLeftArm ? right.scale(-1.0D) : right;
        Vec3 slashCenter = origin.add(horizFwd.scale(2.15D)).add(0.0D, 0.25D, 0.0D);
        if (step == 1) {
            // NA1: Wide flat horizontal claymore cleave Right -> Left
            Vec3 slashStart = slashCenter.add(armRight.scale(2.15D)).add(0.0D, 0.30D, 0.0D);
            Vec3 slashEnd = slashCenter.add(armRight.scale(-2.15D)).add(0.0D, 0.18D, 0.0D);
            com.velorise.staves.network.ModNetwork.sendElectricArc(
                    sl, slashStart, slashEnd, 8, isLeftArm ? 0xFFFF8824 : 0xFFFF8825);
            sl.sendParticles(ParticleTypes.FLAME, slashCenter.x, slashCenter.y + 0.10D, slashCenter.z, 10, 0.65D, 0.15D, 0.65D, 0.04D);
            sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 0.95F);
        } else if (step == 2) {
            // NA2: Massive 300-deg horizontal claymore ring cleave Left -> Right
            Vec3 slashStart = slashCenter.add(armRight.scale(-2.60D)).add(0.0D, 0.22D, 0.0D);
            Vec3 slashEnd = slashCenter.add(armRight.scale(2.60D)).add(0.0D, 0.32D, 0.0D);
            com.velorise.staves.network.ModNetwork.sendElectricArc(
                    sl, slashStart, slashEnd, 10, isLeftArm ? 0xFFFF8825 : 0xFFFF8824);
            for (int a = 0; a < 360; a += 36) {
                Vec3 rDir = Vec3.directionFromRotation(0.0F, sp.getYRot() + a);
                Vec3 rPos = origin.add(rDir.scale(2.45D));
                sl.sendParticles(ParticleTypes.FLAME, rPos.x, this.getY() + 0.55D, rPos.z, 1, 0.08D, 0.05D, 0.08D, 0.02D);
            }
            sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.95F, 0.88F);
        } else if (step == 3) {
            // NA3: NO SWORD — Rear-tire Stoppie ground fire ring explosion!
            Vec3 impactCenter = this.position().add(horizFwd.scale(1.15D));
            for (int a = 0; a < 360; a += 24) {
                Vec3 rDir = Vec3.directionFromRotation(0.0F, sp.getYRot() + a);
                Vec3 rPos = impactCenter.add(rDir.scale(2.20D));
                sl.sendParticles(ParticleTypes.FLAME, rPos.x, this.getY() + 0.15D, rPos.z, 2, 0.10D, 0.08D, 0.10D, 0.04D);
            }
            sl.sendParticles(ParticleTypes.LAVA, impactCenter.x, this.getY() + 0.25D, impactCenter.z, 8, 0.85D, 0.15D, 0.85D, 0.02D);
            sl.sendParticles(ParticleTypes.CRIT, impactCenter.x, this.getY() + 0.30D, impactCenter.z, 10, 0.70D, 0.20D, 0.70D, 0.12D);
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.85F, 0.82F);
        } else if (step == 4) {
            // NA4: NO SWORD — Wheelie Drift directional Pyro spark jet from bike exhaust/front cowling!
            Vec3 jetBase = this.position().add(horizFwd.scale(0.80D)).add(0.0D, 0.65D, 0.0D);
            for (int i = 1; i <= 6; i++) {
                Vec3 pPos = jetBase.add(horizFwd.scale(i * 0.42D)).add(right.scale(-i * 0.18D));
                sl.sendParticles(ParticleTypes.FLAME, pPos.x, pPos.y, pPos.z, 3, 0.12D, 0.10D, 0.12D, 0.05D);
                sl.sendParticles(ParticleTypes.CRIT, pPos.x, pPos.y, pPos.z, 2, 0.10D, 0.08D, 0.10D, 0.10D);
            }
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.80F, 1.25F);
        } else {
            // NA5: NO SWORD — Towering vertical Pyro pillar + wide ground shockwave ring on Superman slam!
            Vec3 impactCenter = this.position().add(horizFwd.scale(1.60D));
            for (int yStep = 0; yStep < 10; yStep++) {
                sl.sendParticles(ParticleTypes.FLAME, impactCenter.x, this.getY() + 0.25D + yStep * 0.45D, impactCenter.z,
                        4, 0.32D, 0.12D, 0.32D, 0.03D);
            }
            for (int a = 0; a < 360; a += 20) {
                Vec3 rDir = Vec3.directionFromRotation(0.0F, sp.getYRot() + a);
                Vec3 rPos = impactCenter.add(rDir.scale(3.40D));
                sl.sendParticles(ParticleTypes.FLAME, rPos.x, this.getY() + 0.16D, rPos.z, 2, 0.12D, 0.06D, 0.12D, 0.04D);
            }
            sl.sendParticles(ParticleTypes.LAVA, impactCenter.x, this.getY() + 0.35D, impactCenter.z, 12, 1.15D, 0.25D, 1.15D, 0.04D);
            sl.sendParticles(ParticleTypes.CRIT, impactCenter.x, this.getY() + 0.40D, impactCenter.z, 14, 0.90D, 0.35D, 0.90D, 0.16D);
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.72F);
            sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.80F);
        }
    }

    private void executeChargedFinisher(ServerLevel sl, ServerPlayer sp) {
        // Completing a Charged Attack Final ("F") clears Donut hit ICD (Dash cooldown is never bypassed)
        this.spinHitCooldowns.clear();

        boolean neth = isNetherite();
        float dmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth)
                * (1.8F + ElementalGaugeManager.getMavuikaCrucibleChargedBonusMultiplier(sp));
        float units = neth ? 2.0F : 1.5F;

        Vec3 forward = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 hitCenter = this.position().add(forward.scale(2.3D)).add(0.0D, 0.5D, 0.0D);
        double radius = 4.5D;

        com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(sl, hitCenter, radius);
        AABB box = new AABB(
                hitCenter.x - radius, hitCenter.y - 1.5D, hitCenter.z - radius,
                hitCenter.x + radius, hitCenter.y + 2.2D, hitCenter.z + radius
        );
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> isValidBikeCombatTarget(sp, e));
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
        Vec3 arcMid = this.position().add(0.0D, 0.55D, 0.0D).add(finFwd.scale(1.55D));
        com.velorise.staves.network.ModNetwork.sendElectricArc(
                sl,
                arcMid.add(finRight.scale(-5.10D)).add(0.0D, -0.08D, 0.0D),
                arcMid.add(finRight.scale(5.10D)).add(0.0D, 0.18D, 0.0D),
                13,
                isLeftFin ? 0xFFFF7A19 : 0xFFFF7A18);
        sl.sendParticles(ParticleTypes.CRIT, arcMid.x, arcMid.y + 0.25D, arcMid.z, 14, 0.65D, 0.22D, 0.65D, 0.16D);
        sl.sendParticles(ParticleTypes.FLAME, hitCenter.x, hitCenter.y, hitCenter.z, 14, 1.15D, 0.18D, 1.15D, 0.06D);
        for (int a = 0; a < 360; a += 30) {
            Vec3 rDir = Vec3.directionFromRotation(0.0F, sp.getYRot() + a);
            Vec3 rPos = hitCenter.add(rDir.scale(3.9D));
            sl.sendParticles(ParticleTypes.FLAME, rPos.x, this.getY() + 0.15D, rPos.z, 1, 0.05D, 0.03D, 0.05D, 0.015D);
        }
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
        int dashCd = (serverDashChain >= 2) ? 24 : 2; // 2-charge Dash: 2t gap for CDCDC, 24t (1.2s) recovery after 2nd dash
        if (serverDashChain >= 2) {
            serverDashChain = 0;
        }
        serverDashCooldownUntil = now + dashCd;
        serverDashTicks = combatCancel ? 2 : 5;
        serverDashIframeTicks = 10;
        sp.invulnerableTime = Math.max(sp.invulnerableTime, 10);
        this.invulnerableTime = Math.max(this.invulnerableTime, 10);
        ElementalGaugeManager.consumeMavuikaNightsoulFuel(sp, 3.0F);
        if (dashCd >= 15) {
            if (sp.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem sm
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(sp.getMainHandItem()) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                sp.getCooldowns().addCooldown(sm, dashCd);
            }
            if (sp.getOffhandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem so
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(sp.getOffhandItem()) == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
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
                || (this.level().isClientSide() && ClientInputAccess.isAttackDown());
        clientDashChain++;
        clientDashTicks = combatCancel ? 2 : 5;
        if (clientDashChain >= 2) {
            clientDashChain = 0;
            clientDashWindowTicks = 0;
            clientDashCooldownTicks = 24; // 1.2s recovery after 2nd consecutive dash (synced 1:1 with server & CC spin)
        } else {
            clientDashWindowTicks = 28;
            clientDashCooldownTicks = 2; // 2t gap matching Dash hitmark so CDCDC never swallows 2nd dash
        }
        this.clientSpinOrbitGraceTicks = 0;
        this.clientNaStep = 0;
        this.clientNaTicks = 0;
        this.clientFinisherHasTarget = false;
        this.clientFinisherYawBlendTicks = combatCancel ? 7 : 0;
        this.spinCamIntroStartX = 0.0D;
        this.spinCamIntroStartZ = 0.0D;
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot()).normalize();
        this.clientDashVec = forward.scale(combatCancel ? 0.55D : 1.15D);
        this.yRotO = this.getYRot();
        this.setYRot(rider.getYRot());
    }

    private void tickClientWheelAndSteeringVisuals() {
        this.prevFrontWheelSpinAngle = this.frontWheelSpinAngle;
        this.prevRearWheelSpinAngle = this.rearWheelSpinAngle;
        this.prevSteeringAngle = this.steeringAngle;

        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        double horizSpeed = Math.hypot(dx, dz);
        double climbSpeed = this.clientWallClimbing ? Math.max(0.0D, this.getY() - this.yo) : 0.0D;
        double yawRad = Math.toRadians(this.getYRot());
        double forwardDot = dx * (-Math.sin(yawRad)) + dz * Math.cos(yawRad);
        float spinDirection = (!this.clientWallClimbing && forwardDot < -0.002D) ? -1.0F : 1.0F;
        float baseSpinDelta = (float) (Math.hypot(horizSpeed, climbSpeed) * 2.6F) * spinDirection;
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

        float targetSteeringAngle;
        if (isSpinning()) {
            targetSteeringAngle = 0.34F;
        } else {
            float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            float strafeInput = (this.getControllingPassenger() instanceof Player rider) ? rider.xxa : 0.0F;
            if (this.idleStopBlend < 0.15F) {
                if (Math.abs(strafeInput) > 0.05F) {
                    this.clientLastTurnSign = strafeInput < 0.0F ? 1.0F : -1.0F;
                } else if (Math.abs(yawDelta) > 1.2F) {
                    this.clientLastTurnSign = Math.signum(yawDelta);
                }
            }
            if (this.idleStopBlend > 0.05F) {
                targetSteeringAngle = this.clientLastTurnSign * 0.14F * this.idleStopBlend;
            } else {
                targetSteeringAngle = Mth.clamp(yawDelta * 0.045F - strafeInput * 0.30F, -0.42F, 0.42F);
            }
        }
        this.steeringAngle = Mth.lerp(0.32F, this.steeringAngle, targetSteeringAngle);
        if (Math.abs(this.steeringAngle) < 0.004F) {
            this.steeringAngle = 0.0F;
        }
    }

    public float getIdleStopBlend(float partialTick) {
        return Mth.lerp(partialTick, this.prevIdleStopBlend, this.idleStopBlend);
    }

    public float getIdleStopSide() {
        return this.clientLastTurnSign >= 0.0F ? 1.0F : -1.0F;
    }

    private void tickClientVisuals() {
        this.prevSpinLean = this.spinLean;
        this.prevIdleStopBlend = this.idleStopBlend;
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
            this.idleStopBlend = 0.0F;
            this.clientNaStep = 0;
            this.clientNaTicks = 0;
            this.clientFinisherYawBlendTicks = 0;
            this.spinYawOffset = (this.spinYawOffset + 24.0F) % 360.0F;
            float leanLerp = this.clientContinuousSpinTicks <= 1 ? 0.20F
                    : (this.clientContinuousSpinTicks == 2 ? 0.30F : 0.42F);
            this.spinLean = Mth.lerp(leanLerp, this.spinLean, 30.0F);
        } else {
            if (srvStep > 0 && srvTicks > 0) {
                boolean staleFinisherTail = srvStep == 6
                        && this.lastSyncedServerNaStep == 6
                        && srvTicks <= this.lastSyncedServerNaTicks
                        && this.clientNaTicks <= 0;
                boolean newServerCombo = !staleFinisherTail && (this.clientNaStep != srvStep
                        || this.clientNaTicks <= 0
                        || (srvStep != 6 && srvTicks > this.lastSyncedServerNaTicks + 1));
                if (newServerCombo) {
                    this.clientNaStep = srvStep;
                    this.clientNaTicks = srvTicks;
                    this.clientNaTotalTicks = com.velorise.staves.fsm.BikeFrameData.get(com.velorise.staves.fsm.BikeActionState.fromNaStep(srvStep)).durationTicks();
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
            boolean hasRider = this.getControllingPassenger() != null;
            boolean solidBelow = !this.level().noCollision(this, this.getBoundingBox().move(0.0D, -0.15D, 0.0D));
            boolean grounded = this.onGround() || solidBelow;
            double horizStep = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
            boolean movingInput = (this.getControllingPassenger() instanceof Player p)
                    && (Math.abs(p.zza) > 0.01F || Math.abs(p.xxa) > 0.01F);
            boolean idleStopped = hasRider && grounded && !movingInput && horizStep < 0.06D
                    && this.clientNaTicks <= 0 && srvTicks <= 0 && this.getUltiTicks() <= 0
                    && this.clientDashTicks <= 0 && !this.isGliding() && !this.clientWallClimbing && !this.isPlunging();
            this.idleStopBlend = Mth.lerp(idleStopped ? 0.18F : 0.45F, this.idleStopBlend, idleStopped ? 1.0F : 0.0F);
            if (this.idleStopBlend < 0.005F) {
                this.idleStopBlend = 0.0F;
            } else if (this.idleStopBlend > 0.995F) {
                this.idleStopBlend = 1.0F;
            }
            float targetLean = (this.idleStopBlend > 0.005F)
                    ? (this.getIdleStopSide() * 10.5F * this.idleStopBlend)
                    : 0.0F;
            this.spinLean = Mth.lerp(idleStopped ? 0.24F : 0.32F, this.spinLean, targetLean);
            if (Math.abs(this.spinLean) < 0.05F && targetLean == 0.0F) {
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
        boolean firstPerson = !this.level().isClientSide() || ClientInputAccess.isFirstPerson();
        boolean combatAimLock = finNaT > 0 || this.clientFinisherYawBlendTicks > 0
                || this.getUltiTicks() > 0 || this.clientDashTicks > 0
                || this.getActiveNaTicks() > 0 || this.isGliding() || this.clientWallClimbing;
        if (!spinningNow) {
            this.yRotO = this.getYRot();
            if (finNaT > 0 || this.clientFinisherYawBlendTicks > 0) {
                // Smoothly rotate out of any Donut orbit angle into the front-center Finisher direction
                float nextYaw = Mth.rotLerp(finNaT >= 25 ? 0.44F : (finNaT >= 13 ? 0.64F : 0.72F), this.getYRot(), targetYaw);
                if (Math.abs(Mth.wrapDegrees(targetYaw - nextYaw)) < 0.5F) {
                    nextYaw = targetYaw;
                }
                this.setYRot(nextYaw);
            } else if (firstPerson || combatAimLock) {
                // 1st-person or active combat/glide/climb: align bike cleanly with camera aim yaw
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
                if (!firstPerson && !spinningNow && !combatAimLock) {
                    // Genshin 3rd-person steering: pressing S / A / D never reverses — bike picks the shorter side
                    // (breaking exact 180-deg ties smoothly via clientLastTurnSign) and whips a tight U-turn forward!
                    float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float diff = Mth.wrapDegrees(desiredYaw - this.getYRot());
                    if (Math.abs(diff) > 168.0F) {
                        if (Math.abs(strafe) > 0.05F) {
                            clientLastTurnSign = strafe > 0.0F ? -1.0F : 1.0F;
                        }
                        diff = clientLastTurnSign * 170.0F;
                    } else if (Math.abs(diff) > 6.0F) {
                        clientLastTurnSign = Math.signum(diff);
                    }
                    float maxTurnDeg = sprintingNow ? 22.0F : 26.0F;
                    float turnStep = Mth.clamp(diff, -maxTurnDeg, maxTurnDeg);
                    float nextYaw = this.getYRot() + turnStep;
                    if (Math.abs(Mth.wrapDegrees(desiredYaw - nextYaw)) < 1.0F) {
                        nextYaw = desiredYaw;
                    }
                    this.setYRot(nextYaw);
                    // Subtle motorcycle lean into the U-turn (+ZP leans right when turning right, matching idle kickstand side)
                    float targetLean = Mth.clamp(turnStep * 0.72F, -18.0F, 18.0F);
                    this.spinLean = Mth.lerp(0.35F, this.spinLean, targetLean);
                    // Drive forward along the bike's turning nose so U-turns carve a true motorcycle arc
                    Vec3 bikeNose = Vec3.directionFromRotation(0.0F, nextYaw).normalize();
                    double turnSpeedScale = Math.abs(diff) > 75.0F ? 0.78D : 1.0D;
                    inputVec = bikeNose.scale(speed * turnSpeedScale);
                } else {
                    inputVec = new Vec3(dx / len * speed, 0.0D, dz / len * speed);
                }
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
            float rawYawU = Mth.clamp(clientContinuousSpinTicks / 4.0F, 0.0F, 1.0F);
            float yawBlend = rawYawU * rawYawU * (3.0F - 2.0F * rawYawU);
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
        } else if (ultiT > 0) {
            // Keep entity hitbox cleanly grounded at center while the +4.8m sky launch, mid-air eject, meteor dive, and wheelie re-summon animate smoothly in render space
            vx *= 0.12D;
            vz *= 0.12D;
            vy = onSurface ? -0.04D : Math.max(-0.8D, prev.y - 0.12D);
        } else if (getActiveNaStep() == 7 && getActiveNaTicks() > 0) {
            // Genshin Hold E Mount tail-whip: surge forward (~2.0 blocks) while whipping rear wheel in a fire arc
            int whipT = getActiveNaTicks();
            Vec3 fwdDir = Vec3.directionFromRotation(0.0F, targetYaw).normalize();
            double surgeSpeed = whipT >= 4 ? (0.36D * ((whipT - 2.0D) / 8.0D) + 0.14D) : 0.08D;
            vx = fwdDir.x * surgeSpeed;
            vz = fwdDir.z * surgeSpeed;
            vy = onSurface ? -0.04D : Math.max(-0.8D, prev.y - 0.12D);
        } else if (getActiveNaStep() == 8 && getActiveNaTicks() > 0) {
            // Genshin Re-Mount catch-up: gentle forward glide as the bike scoops the leaping rider
            int catchT = getActiveNaTicks();
            Vec3 fwdDir = Vec3.directionFromRotation(0.0F, targetYaw).normalize();
            double glideSpeed = 0.08D + 0.14D * (catchT / 10.0D);
            vx = fwdDir.x * glideSpeed;
            vz = fwdDir.z * glideSpeed;
            vy = onSurface ? -0.04D : Math.max(-0.8D, prev.y - 0.12D);
        } else if (finNaT > 0) {
            // Authentic 27-tick Genshin Finisher synced to hitmark at finNaT == 13 (tick 14 = 0.70s):
            // Phase 0 (finNaT 27..25, 3t): grounded pull onto forward centerline (never pulls backward!)
            // Phase 1 (finNaT 24..18, 7t): bike glides low & sleek forward while rider vaults +1.65m high above saddle
            // Phase 2 (finNaT 17..14, 4t): rider plunges straight onto saddle -> hitmark cleave at finNaT == 13
            // Phase 3 (finNaT 13..1, 13t): iconic Genshin sideways wheelie tail-slide forward then clean settle
            Vec3 fwdDir = Vec3.directionFromRotation(0.0F, targetYaw).normalize();
            if (!this.clientFinisherHasTarget) {
                if (this.spinOrbitRadius > 0.05D) {
                    double relX = this.getX() - this.spinPivotX;
                    double relZ = this.getZ() - this.spinPivotZ;
                    double clampedFwd = Math.max(0.0D, relX * fwdDir.x + relZ * fwdDir.z);
                    this.clientFinisherTargetX = this.spinPivotX + fwdDir.x * clampedFwd;
                    this.clientFinisherTargetZ = this.spinPivotZ + fwdDir.z * clampedFwd;
                } else {
                    this.clientFinisherTargetX = this.getX();
                    this.clientFinisherTargetZ = this.getZ();
                }
                this.clientFinisherHasTarget = true;
            }
            if (finNaT >= 25) {
                double toX = this.clientFinisherTargetX - this.getX();
                double toZ = this.clientFinisherTargetZ - this.getZ();
                double remTicks = Math.max(1.0D, (finNaT - 24));
                double stepX = toX / remTicks;
                double stepZ = toZ / remTicks;
                double stepLen = Math.hypot(stepX, stepZ);
                if (stepLen > 0.85D) {
                    stepX = (stepX / stepLen) * 0.85D;
                    stepZ = (stepZ / stepLen) * 0.85D;
                }
                vx = stepX;
                vz = stepZ;
                vy = onSurface ? -0.04D : Math.max(-0.8D, prev.y - 0.12D);
            } else if (finNaT >= 18) {
                // Bike stays low & sleek beneath the vaulting rider (+0.26m subtle hop)
                vx = fwdDir.x * 0.24D;
                vz = fwdDir.z * 0.24D;
                float u = (24.0F - finNaT) / 6.0F;
                vy = u < 0.45F ? 0.12D : (onSurface ? -0.04D : -0.14D);
            } else if (finNaT >= 14) {
                vx = fwdDir.x * 0.18D;
                vz = fwdDir.z * 0.18D;
                vy = onSurface ? -0.04D : Math.max(-0.8D, prev.y - 0.18D);
            } else {
                this.clientFinisherHasTarget = false;
                // Gentle forward tail-slide momentum during wheelie recovery (finNaT 13..6)
                double slide = finNaT >= 6 ? (0.16D * ((finNaT - 5.0D) / 8.0D)) : 0.0D;
                vx = Mth.lerp(0.35D, prev.x, inputVec.x + fwdDir.x * slide);
                vz = Mth.lerp(0.35D, prev.z, inputVec.z + fwdDir.z * slide);
                vy = onSurface ? -0.04D : Math.max(-1.05D, prev.y - 0.22D);
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
        } else if (finNaT >= 25 && this.clientFinisherHasTarget) {
            // Lock camera anchor smoothly onto the Finisher convergence target during Phase 0 so the camera NEVER lurches
            // outward toward the rim and back inward while the bike glides smoothly into the forward centerline!
            double desiredAnchorX = Mth.lerp(0.35D, prevAnchorWorldX, this.clientFinisherTargetX);
            double desiredAnchorZ = Mth.lerp(0.35D, prevAnchorWorldZ, this.clientFinisherTargetZ);
            this.camAnchorOffsetX = desiredAnchorX - this.getX();
            this.camAnchorOffsetZ = desiredAnchorZ - this.getZ();
        } else {
            this.camAnchorOffsetX *= 0.72D;
            this.camAnchorOffsetZ *= 0.72D;
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

    private static boolean isValidBikeCombatTarget(ServerPlayer sp, LivingEntity e) {
        return e.isAlive() && !e.isRemoved() && e != sp && !e.isSpectator()
                && !sp.isAlliedTo(e) && !ElementalGaugeManager.isRaidenProtectedAlly(sp, e);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID("Owner")));
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
        return ClientInputAccess.isJumpDown();
    }

    private static final class ClientInputAccess {
        static boolean isAttackDown() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null && mc.options != null && mc.options.keyAttack.isDown();
        }

        static boolean isJumpDown() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null && mc.options != null && mc.options.keyJump.isDown();
        }

        static boolean isFirstPerson() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc == null || mc.options == null || mc.options.getCameraType().isFirstPerson();
        }
    }
}
