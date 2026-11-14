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
    private static final EntityDataAccessor<Integer> DATA_NA_TICKS =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_NA_STEP =
            SynchedEntityData.defineId(FlamestriderBikeEntity.class, EntityDataSerializers.INT);

    private long lastSpinDamageTick = -100L;
    private long lastDashTick = -100L;
    private int serverDashTicks = 0;
    private long lastNaTick = -100L;
    private int naComboStep = 0;
    private float pendingUltiDamage = 0.0F;
    private boolean pendingFinisher = false;
    private boolean pendingDiscardAfterFinisher = false;
    private boolean spinInputHeld = false;
    private boolean sprintInputHeld = false;
    private int plungeTicks = 0;
    private long plungeRecoveryUntil = 0L;
    private float serverSpinAngle = 0.0F;
    private final java.util.Map<Integer, Long> spinHitCooldowns = new java.util.HashMap<>();
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
    private float spinOrbitAngle = 0.0F;
    private double spinOrbitRadius = 0.0D;
    private int clientDashTicks = 0;
    private int clientDashCooldownTicks = 0;
    private int clientJumpCooldownTicks = 0;
    private int clientPlungeRecoveryTicks = 0;
    private boolean clientWasPlunging = false;
    private Vec3 clientDashVec = Vec3.ZERO;
    public float wheelSpinAngle = 0.0F;
    public float spinYawOffset = 0.0F;

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
        this.entityData.define(DATA_NA_TICKS, 0);
        this.entityData.define(DATA_NA_STEP, 0);
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
        return this.pendingFinisher || (getNaStep() == 6 && getNaTicks() > 0);
    }

    public int getPlungeWindup() {
        return this.entityData.get(DATA_PLUNGE_WINDUP);
    }

    public boolean isSpinCameraAnchored() {
        return this.clientWasSpinning && this.spinOrbitRadius > 0.05D;
    }

    public double getSpinPivotX() {
        return this.spinPivotX;
    }

    public double getSpinPivotZ() {
        return this.spinPivotZ;
    }

    public double getSpinPivotX(float partialTick) {
        return Mth.lerp(partialTick, this.spinPivotXO, this.spinPivotX);
    }

    public double getSpinPivotZ(float partialTick) {
        return Mth.lerp(partialTick, this.spinPivotZO, this.spinPivotZ);
    }

    public int getUltiTicks() {
        return this.entityData.get(DATA_ULTI_TICKS);
    }

    public int getNaTicks() {
        return this.entityData.get(DATA_NA_TICKS);
    }

    public int getNaStep() {
        return this.entityData.get(DATA_NA_STEP);
    }

    public boolean isDashingServer() {
        return serverDashTicks > 0;
    }

    public void triggerUltiAnimation() {
        this.entityData.set(DATA_ULTI_TICKS, 30);
    }

    public void startUltiSlam(float ultiDmg) {
        this.pendingUltiDamage = ultiDmg;
        this.entityData.set(DATA_ULTI_TICKS, 30);
    }

    @Override
    public float maxUpStep() {
        return 1.05F;
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
        if (passenger instanceof Player p) {
            if (p.getMainHandItem().getItem() instanceof ArcaneStaffItem sm && sm.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                p.getCooldowns().addCooldown(sm, 12);
            }
            if (p.getOffhandItem().getItem() instanceof ArcaneStaffItem so && so.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
                p.getCooldowns().addCooldown(so, 12);
            }
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
        super.tick();
        this.fallDistance = 0.0F;
        if (this.clientDashCooldownTicks > 0) {
            this.clientDashCooldownTicks--;
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
            boolean holdingMavuika = (sp.getMainHandItem().getItem() instanceof ArcaneStaffItem sm
                    && sm.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS)
                    || (sp.getOffhandItem().getItem() instanceof ArcaneStaffItem so
                    && so.kind() == ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS);
            if (!holdingMavuika) {
                this.spinInputHeld = false;
                setSpinning(false);
                sp.stopRiding();
                sl.playSound(null, sp.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
                this.discard();
                return;
            }
            if (!ElementalGaugeManager.isMavuikaAuraActive(sp.getUUID(), now)) {
                if (this.spinInputHeld || isSpinning()) {
                    this.spinInputHeld = false;
                    setSpinning(false);
                    this.entityData.set(DATA_NA_STEP, 6);
                    this.entityData.set(DATA_NA_TICKS, 16);
                    this.pendingFinisher = true;
                    this.pendingDiscardAfterFinisher = true;
                } else if (serverDashTicks > 0 || isPlunging() || getUltiTicks() > 0
                        || (this.pendingDiscardAfterFinisher && (this.pendingFinisher || getNaTicks() > 0))) {
                    // Let active dash, plunge, ulti, or finisher complete before dismounting
                    this.sprintInputHeld = false;
                    setBikeSprinting(false);
                } else {
                    sp.stopRiding();
                    this.discard();
                    return;
                }
            }
            if (serverDashTicks > 0) {
                serverDashTicks--;
            }
            int ultiT = getUltiTicks();
            if (ultiT > 0) {
                if (passenger instanceof LivingEntity le) {
                    le.invulnerableTime = Math.max(le.invulnerableTime, 4);
                }
                int nextUlti = ultiT - 1;
                this.entityData.set(DATA_ULTI_TICKS, nextUlti);
                if (nextUlti > 8) {
                    sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 2.0D, this.getZ(), 6, 0.55D, 0.35D, 0.55D, 0.05D);
                } else if (nextUlti == 8 && pendingUltiDamage > 0.0F) {
                    float dmg = pendingUltiDamage;
                    pendingUltiDamage = 0.0F;
                    sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    spawnFierySweep(sl, this.position().add(0.0D, 0.7D, 0.0D), sp.getYRot(), 4.2D);
                    ElementalGaugeManager.executeMavuikaUltiImpact(sl, sp, dmg);
                }
            }
            int naT = getNaTicks();
            if (naT > 0) {
                int nextNa = naT - 1;
                this.entityData.set(DATA_NA_TICKS, nextNa);
                if (pendingFinisher && getNaStep() == 6 && nextNa == 6) {
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
                boolean landed = this.onGround()
                        || !sl.noCollision(this, this.getBoundingBox().move(0.0D, -0.35D, 0.0D))
                        || this.isInWater()
                        || sl.getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.30D, this.getZ())).getFluidState().is(FluidTags.WATER);
                if ((windup <= 0 && landed && plungeTicks >= 6) || plungeTicks > 65) {
                    setPlunging(false);
                    this.entityData.set(DATA_PLUNGE_WINDUP, 0);
                    plungeTicks = 0;
                    executePlungeImpact(sl, sp);
                }
            }

            // Hold left-click while riding -> Donut Spin Charged Attack (sweeps 360° around pivot)
            boolean holdingSuns = spinInputHeld;
            setSpinning(holdingSuns);
            setBikeSprinting(sprintInputHeld && !holdingSuns && getUltiTicks() <= 8);

            if (holdingSuns) {
                serverSpinAngle = (serverSpinAngle + 24.0F) % 360.0F;
                performDonutSweepHit(sl, sp, now);
            } else {
                serverSpinAngle = 0.0F;
                if (serverDashTicks > 0 || isBikeSprinting()) {
                    performRamSweepHit(sl, sp, now, serverDashTicks > 0);
                }
            }

            // Exhaust flame trail particles
            if (this.tickCount % 2 == 0) {
                Vec3 back = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(-0.95D);
                sl.sendParticles(ParticleTypes.FLAME,
                        this.getX() + back.x, this.getY() + 0.35D, this.getZ() + back.z,
                        2, 0.12D, 0.08D, 0.12D, 0.02D);
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
        float dmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth);
        float units = neth ? 1.5F : 1.0F;

        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        Vec3 midPoint = this.position().add(0.0D, 0.4D, 0.0D);
        Vec3 tailPoint = midPoint.subtract(forward.scale(1.15D));

        AABB box = this.getBoundingBox().inflate(2.4D, 1.5D, 2.4D);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != sp);
        boolean anyHit = false;
        for (LivingEntity target : targets) {
            double dMid = target.position().add(0.0D, target.getBbHeight() * 0.4D, 0.0D).distanceToSqr(midPoint);
            double dTail = target.position().add(0.0D, target.getBbHeight() * 0.4D, 0.0D).distanceToSqr(tailPoint);
            if (dMid > 2.6D && dTail > 2.6D) continue; // ~1.6m sweep radius along bike/tail
            long lastHit = spinHitCooldowns.getOrDefault(target.getId(), -100L);
            if (now - lastHit < 12L) continue; // Hit once per ~0.6s sweep pass
            spinHitCooldowns.put(target.getId(), now);
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            sl.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.28D, 0.25D, 0.28D, 0.05D);
            anyHit = true;
        }
        if (anyHit) {
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.25F);
        }
        // Donut flame tire trail at outer edge
        sl.sendParticles(ParticleTypes.FLAME, tailPoint.x, this.getY() + 0.12D, tailPoint.z, 3, 0.12D, 0.04D, 0.12D, 0.02D);
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
            long lastHit = spinHitCooldowns.getOrDefault(target.getId(), -100L);
            if (now - lastHit < 10L) continue;
            spinHitCooldowns.put(target.getId(), now);
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
        float dmg = baseDmg * 1.85F;
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
        spawnFierySweep(sl, this.position().add(0.0D, 0.55D, 0.0D), sp.getYRot(), 3.4D);
        sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.3D, this.getZ(), 42, 1.8D, 0.35D, 1.8D, 0.12D);
        sl.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.25D, this.getZ(), 16, 1.2D, 0.25D, 1.2D, 0.04D);
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.15F);
    }

    private static void spawnFierySweep(ServerLevel sl, Vec3 center, float yawDeg, double arcRadius) {
        DustParticleOptions flameDust = new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.38F, 0.05F), 1.55F);
        for (int i = -6; i <= 6; i++) {
            double angleRad = Math.toRadians(yawDeg + i * 11.0D);
            double px = center.x - Math.sin(angleRad) * arcRadius;
            double pz = center.z + Math.cos(angleRad) * arcRadius;
            sl.sendParticles(flameDust, px, center.y + 0.15D, pz, 2, 0.06D, 0.05D, 0.06D, 0.01D);
            sl.sendParticles(ParticleTypes.FLAME, px, center.y + 0.15D, pz, 2, 0.05D, 0.05D, 0.05D, 0.02D);
        }
    }

    public void setSpinInputHeld(boolean held) {
        if (this.pendingDiscardAfterFinisher || this.isFinisherPlaying()) {
            this.spinInputHeld = false;
            setSpinning(false);
            return;
        }
        boolean wasHeld = this.spinInputHeld;
        this.spinInputHeld = held;
        if (!held) {
            this.serverSpinAngle = 0.0F;
            setSpinning(false);
            if (wasHeld && this.level() instanceof ServerLevel sl && this.getFirstPassenger() instanceof ServerPlayer) {
                this.entityData.set(DATA_NA_STEP, 6);
                this.entityData.set(DATA_NA_TICKS, 16);
                this.pendingFinisher = true;
                sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.45F);
            }
        }
    }

    /**
     * Charged Attack Finisher Slam: rider & bike leap up together and slam a fiery sword sweep forward (no explosion).
     */
    private void executeChargedFinisher(ServerLevel sl, ServerPlayer sp) {
        boolean neth = isNetherite();
        float dmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth) * 1.8F;
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

        spawnFierySweep(sl, hitCenter, sp.getYRot(), 2.8D);
        sl.sendParticles(ParticleTypes.FLAME, hitCenter.x, hitCenter.y, hitCenter.z, 26, 0.95D, 0.25D, 0.95D, 0.08D);
        sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.85F, 1.15F);
        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.85F, 0.95F);
    }

    /**
     * 5-hit Normal Attack combo on Flamestrider:
     * 1: right-hand sword forward slash, 2: tail whip sweep, 3: right-hand sword reverse sweep,
     * 4: rising wheelie slam, 5: full 360° front-flip fiery slam.
     */
    public void triggerMountWhip() {
        this.entityData.set(DATA_NA_STEP, 2);
        this.entityData.set(DATA_NA_TICKS, 8);
        if (this.level() instanceof ServerLevel sl && this.getFirstPassenger() instanceof ServerPlayer sp) {
            spawnFierySweep(sl, this.position().add(0.0D, 0.55D, 0.0D), sp.getYRot(), 3.2D);
        }
    }

    public void performNormalAttack(ServerPlayer sp) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (isPlunging() || this.pendingDiscardAfterFinisher || this.isFinisherPlaying()) return;
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
        long requiredCd = (naComboStep == 5) ? 20L : 9L; // ~0.45s between hits 1-4, 1.0s recovery after strong 5th hit
        if (now - lastNaTick < requiredCd) return;
        naComboStep = (now - lastNaTick <= 30L) ? (naComboStep % 5) + 1 : 1;
        lastNaTick = now;
        ElementalGaugeManager.consumeMavuikaNightsoulFuel(sp, 1.0F);

        int animTicks = naComboStep == 5 ? 14 : 10;
        this.entityData.set(DATA_NA_STEP, naComboStep);
        this.entityData.set(DATA_NA_TICKS, animTicks);
        sp.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

        boolean neth = isNetherite();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS, neth);
        float mult = switch (naComboStep) {
            case 2 -> 1.1F;
            case 3 -> 1.25F;
            case 4 -> 1.35F;
            case 5 -> 1.6F;
            default -> 1.0F;
        };
        float dmg = baseDmg * mult;
        float units = neth ? 1.5F : 1.0F;

        Vec3 forward = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        Vec3 hitCenter = this.position().add(forward.scale(naComboStep == 5 ? 2.4D : 2.0D)).add(0.0D, 0.6D, 0.0D);
        double radius = naComboStep == 5 ? 4.2D : 3.5D;

        AABB box = new AABB(
                hitCenter.x - radius, hitCenter.y - 1.5D, hitCenter.z - radius,
                hitCenter.x + radius, hitCenter.y + 2.0D, hitCenter.z + radius
        );
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isRemoved() && e != sp);
        for (LivingEntity target : targets) {
            ElementalGaugeManager.applyElement(sl, target, Element.PYRO, units, sp, dmg);
            Vec3 kb = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (kb.lengthSqr() > 1.0E-4D) {
                kb = kb.normalize().scale(naComboStep == 5 ? 0.65D : 0.35D);
                target.push(kb.x, naComboStep == 5 ? 0.32D : 0.14D, kb.z);
            }
            sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.25D, 0.25D, 0.25D, 0.05D);
        }

        spawnFierySweep(sl, hitCenter, sp.getYRot(), naComboStep == 5 ? 2.6D : 2.1D);
        sl.sendParticles(ParticleTypes.FLAME, hitCenter.x, hitCenter.y, hitCenter.z,
                naComboStep == 5 ? 28 : 14, 0.9D, 0.35D, 0.9D, 0.08D);
        if (naComboStep == 5) {
            sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.85F, 1.1F);
        } else {
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, naComboStep % 2 == 0 ? 1.2F : 1.0F);
        }
    }

    /**
     * Server-side Dash Cancel: lunges bike forward, resets spin timer for instant follow-up CA.
     */
    public void handleDashCancel(ServerPlayer sp) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (this.tickCount < 10) return; // Mount grace delay so holding right-click to mount never auto-dashes
        long now = sl.getServer().overworld().getGameTime();
        if (now - lastDashTick < 20L) return; // 1.0s Genshin-style dash cooldown
        lastDashTick = now;
        serverDashTicks = 5;
        ElementalGaugeManager.consumeMavuikaNightsoulFuel(sp, 10.0F);
        if (sp.getMainHandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem sm
                && sm.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            sp.getCooldowns().addCooldown(sm, 20);
        }
        if (sp.getOffhandItem().getItem() instanceof com.velorise.staves.item.ArcaneStaffItem so
                && so.kind() == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_BLAZING_SUNS) {
            sp.getCooldowns().addCooldown(so, 20);
        }
        lastSpinDamageTick = -100L;
        spinHitCooldowns.clear(); // Reset donut hit ICD so follow-up CA strikes immediately
        serverSpinAngle = 0.0F;
        spinInputHeld = false;
        pendingFinisher = false;
        this.entityData.set(DATA_NA_TICKS, 0);
        setSpinning(false);

        Vec3 look = Vec3.directionFromRotation(0.0F, sp.getYRot()).normalize();
        sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.45D, this.getZ(), 16, 0.35D, 0.2D, 0.35D, 0.08D);
        sl.sendParticles(ParticleTypes.LAVA, this.getX() - look.x * 0.6D, this.getY() + 0.35D, this.getZ() - look.z * 0.6D, 5, 0.2D, 0.1D, 0.2D, 0.02D);
        sl.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.35F);
    }

    public void triggerClientDash(Player rider) {
        if (this.tickCount < 10 || clientDashTicks > 0 || clientDashCooldownTicks > 0) return;
        clientDashTicks = 5;
        clientDashCooldownTicks = 20;
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot()).normalize();
        this.clientDashVec = forward.scale(1.15D);
        this.setYRot(rider.getYRot());
    }

    private void tickClientVisuals() {
        double horizSpeed = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        this.wheelSpinAngle += (float) (horizSpeed * 2.6F);
        if (isSpinning()) {
            this.spinYawOffset = (this.spinYawOffset + 24.0F) % 360.0F;
        } else {
            this.spinYawOffset = 0.0F;
        }
    }

    private void tickLocalControl(Player rider) {
        boolean spinningNow = this.isSpinning();
        float targetYaw = rider.getYRot();
        if (!spinningNow) {
            // Align bike heading smoothly with rider's camera yaw
            this.setYRot(targetYaw);
            this.yRotO = targetYaw;
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

        // Double-jump Space in mid-air -> deploy wings; press Space again while gliding -> close wings & drop
        boolean jumping = this.level().isClientSide() && isLocalJumpDown();
        if (jumping && !clientWasJumping) {
            if (onSurface) {
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
                clientGlideTicks = 60; // 3.0s level wing glide at current altitude
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
        if (glidingNow) {
            double wingBoost = clientGlideBoostTicks > 0 ? 0.34D * (clientGlideBoostTicks / 12.0D) : 0.0D;
            speed = (sprintingNow ? 0.74D : 0.64D) + wingBoost;
        } else if (canSkimWater && (waterHere || waterBelow)) {
            speed = sprintingNow ? 0.72D : 0.58D;
        } else {
            speed = sprintingNow ? 0.72D : 0.54D;
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

        if (spinningNow && clientDashTicks <= 0 && getUltiTicks() <= 6) {
            if (!clientWasSpinning) {
                spinPivotX = this.getX();
                spinPivotZ = this.getZ();
                spinPivotXO = spinPivotX;
                spinPivotZO = spinPivotZ;
                spinOrbitAngle = targetYaw;
                spinOrbitRadius = 0.0D;
            } else {
                spinPivotXO = spinPivotX;
                spinPivotZO = spinPivotZ;
            }
            double stepX = inputVec.x * 0.35D;
            double stepZ = inputVec.z * 0.35D;
            if (Math.abs(stepX) > 1.0E-5D && this.level().noCollision(this, this.getBoundingBox().move(spinPivotX + stepX - this.getX(), 0.0D, spinPivotZ - this.getZ()))) {
                spinPivotX += stepX;
            }
            if (Math.abs(stepZ) > 1.0E-5D && this.level().noCollision(this, this.getBoundingBox().move(spinPivotX - this.getX(), 0.0D, spinPivotZ + stepZ - this.getZ()))) {
                spinPivotZ += stepZ;
            }
            spinOrbitRadius = Math.min(2.25D, spinOrbitRadius + 0.75D);
            spinOrbitAngle = (spinOrbitAngle + 24.0F) % 360.0F;
            double orbitRad = Math.toRadians(spinOrbitAngle);
            double cosO = Math.cos(orbitRad);
            double sinO = Math.sin(orbitRad);
            double effectiveRadius = spinOrbitRadius;
            Vec3 pivotPos = new Vec3(spinPivotX, this.getY() + 0.65D, spinPivotZ);
            Vec3 desiredOrbit = new Vec3(spinPivotX + cosO * effectiveRadius, this.getY() + 0.65D, spinPivotZ + sinO * effectiveRadius);
            net.minecraft.world.phys.BlockHitResult wallHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                    pivotPos, desiredOrbit,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    this
            ));
            if (wallHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                effectiveRadius = Math.max(0.35D, wallHit.getLocation().distanceTo(pivotPos) - 0.55D);
            }
            double targetX = spinPivotX + cosO * effectiveRadius;
            double targetZ = spinPivotZ + sinO * effectiveRadius;
            vx = targetX - this.getX();
            vz = targetZ - this.getZ();
            float bikeYaw = (spinOrbitAngle + 15.0F) % 360.0F;
            this.setYRot(bikeYaw);
            this.yRotO = bikeYaw;
        } else {
            spinOrbitRadius = 0.0D;
            spinPivotXO = this.getX();
            spinPivotZO = this.getZ();
        }
        clientWasSpinning = spinningNow;

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
        int finNaT = getNaStep() == 6 ? getNaTicks() : 0;
        if (this.isPlunging()) {
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
            vx *= 0.20D;
            vz *= 0.20D;
            if (ultiT > 20) {
                vy = 0.44D; // Hour of Burning Skies: soar high into the air
            } else if (ultiT > 14) {
                vy = 0.02D; // Brief apex windup before Sunfell Slice
            } else {
                vy = -0.95D; // Sunfell Slice: high-speed downward leap slam
            }
            this.setOnGround(false);
        } else if (finNaT > 6) {
            Vec3 fwdJump = Vec3.directionFromRotation(0.0F, targetYaw).normalize().scale(0.14D);
            vx = fwdJump.x;
            vz = fwdJump.z;
            vy = finNaT > 10 ? 0.34D : -0.48D; // Rider & bike jump up together then slam forward
            this.setOnGround(false);
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

        this.setDeltaMovement(vx, vy, vz);
        this.move(MoverType.SELF, this.getDeltaMovement());

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
