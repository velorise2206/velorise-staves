package com.velorise.staves.entity;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.api.IcdPolicy;
import com.velorise.staves.api.VeloriseElementAPI;
import com.velorise.staves.api.VeloriseElementEvents;
import com.velorise.staves.element.Element;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Cryo Regisvine World Boss — Showcase Boss #1 built 100% on top of VeloriseElementAPI
 * and VeloriseElementEvents. Rooted at its spawn anchor, zero terrain griefing, 16U Cryo Corolla Shield,
 * 14 baked Blockbench animations, and Genshin-authentic break -> stun -> recovery loop.
 */
public class CryoRegisvineEntity extends Monster {

    public static final int STATE_DORMANT = 0;
    public static final int STATE_AWAKEN  = 1;
    public static final int STATE_COMBAT  = 2;
    public static final int STATE_STUNNED = 3;
    public static final int STATE_DEATH   = 4;

    public static final int ANIM_DORMANT          = 0;
    public static final int ANIM_AWAKEN           = 1;
    public static final int ANIM_IDLE_COMBAT      = 2;
    public static final int ANIM_ICE_BEAMS_CROSS  = 3;
    public static final int ANIM_HOMING_ICICLES   = 4;
    public static final int ANIM_FACEPLANT_SINGLE = 5;
    public static final int ANIM_FACEPLANT_TRIPLE = 6;
    public static final int ANIM_SPIN_FROST_RING  = 7;
    public static final int ANIM_BLIZZARD_CHANNEL = 8;
    public static final int ANIM_FLAIL_360        = 9;
    public static final int ANIM_HALF_FLAIL_COMBO = 10;
    public static final int ANIM_ROTARY_SPRAY     = 11;
    public static final int ANIM_STUNNED          = 12;
    public static final int ANIM_DEATH            = 13;

    public static final String[] ANIM_NAMES = {
            "dormant_buried",
            "awaken_emerge",
            "idle_combat",
            "ice_beams_cross",
            "homing_icicles",
            "faceplant_single",
            "faceplant_triple",
            "spin_frost_ring",
            "blizzard_channel",
            "flail_360",
            "half_flail_combo",
            "rotary_spray_beam",
            "stunned_paralyzed",
            "death_wither"
    };

    public static final int[] ANIM_DURATIONS_TICKS = {
            60, 44, 80, 52, 48, 44, 84, 56, 72, 56, 80, 96, 80, 52
    };

    public static final float ROOTS_SHIELD_UNITS = 12.0F;
    public static final float COROLLA_SHIELD_UNITS = 6.0F;
    private static final int STUN_DURATION_TICKS = 280; // 14.0 seconds

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(CryoRegisvineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIM_ID =
            SynchedEntityData.defineId(CryoRegisvineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIM_TICK =
            SynchedEntityData.defineId(CryoRegisvineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SHIELD_PROGRESS =
            SynchedEntityData.defineId(CryoRegisvineEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COROLLA_PHASE =
            SynchedEntityData.defineId(CryoRegisvineEntity.class, EntityDataSerializers.INT);

    private static volatile boolean SHIELD_LISTENER_REGISTERED = false;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.velorise_staves.cryo_regisvine"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private Vec3 anchorPos = Vec3.ZERO;
    private boolean anchorInitialized = false;
    private int stateTicks = 0;
    private int attackCooldown = 30;
    private int noPlayerTicks = 0;
    private int lastAttackAnim = -1;
    private float attackStartYaw = 0.0F;
    private boolean customDeathHandled = false;

    private static final class BlizzardZone {
        final double x, y, z;
        int ticksLeft;
        BlizzardZone(double x, double y, double z, int ticksLeft) {
            this.x = x; this.y = y; this.z = z; this.ticksLeft = ticksLeft;
        }
    }

    private final List<BlizzardZone> blizzardZones = new ArrayList<>();

    public CryoRegisvineEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        this.setPersistenceRequired();
        ensureShieldBreakListener();
    }

    private static void ensureShieldBreakListener() {
        if (!SHIELD_LISTENER_REGISTERED) {
            SHIELD_LISTENER_REGISTERED = true;
            VeloriseElementEvents.registerShieldBreakListener((level, target, attacker, shieldElem, breakerElem) -> {
                if (target instanceof CryoRegisvineEntity boss && boss.getBossState() == STATE_COMBAT) {
                    boss.onCorollaShieldBroken(level);
                }
            });
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 360.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, STATE_DORMANT);
        builder.define(DATA_ANIM_ID, ANIM_DORMANT);
        builder.define(DATA_ANIM_TICK, 0);
        builder.define(DATA_SHIELD_PROGRESS, 0.0F);
        builder.define(DATA_COROLLA_PHASE, 0);
    }

    public int getBossState() { return entityData.get(DATA_STATE); }
    public int getAnimId() { return entityData.get(DATA_ANIM_ID); }
    public int getAnimTick() { return entityData.get(DATA_ANIM_TICK); }
    public float getShieldProgress() { return entityData.get(DATA_SHIELD_PROGRESS); }
    public int getCorollaPhase() { return entityData.get(DATA_COROLLA_PHASE); }

    public String getCurrentAnimName() {
        int id = getAnimId();
        return (id >= 0 && id < ANIM_NAMES.length) ? ANIM_NAMES[id] : "idle_combat";
    }

    public void setAnchor(Vec3 pos) {
        this.anchorPos = pos;
        this.anchorInitialized = true;
        this.setPos(pos.x, pos.y, pos.z);
    }

    private void transitionTo(int newState, int animId) {
        entityData.set(DATA_STATE, newState);
        entityData.set(DATA_ANIM_ID, animId);
        entityData.set(DATA_ANIM_TICK, 0);
        stateTicks = 0;
    }

    private void playAnim(int animId) {
        entityData.set(DATA_ANIM_ID, animId);
        entityData.set(DATA_ANIM_TICK, 0);
    }

    public void onCorollaShieldBroken(ServerLevel level) {
        transitionTo(STATE_STUNNED, ANIM_STUNNED);
        entityData.set(DATA_SHIELD_PROGRESS, 0.0F);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.4F, 0.7F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2.5D, getZ(), 40, 1.8D, 1.5D, 1.8D, 0.08D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(Entity entity) { }

    @Override
    public void knockback(double strength, double x, double z) { }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        int st = getBossState();
        if (st == STATE_DEATH || st == STATE_AWAKEN) return false;
        if (st == STATE_DORMANT && !level().isClientSide()) {
            awaken((ServerLevel) level());
            return false;
        }
        if (st == STATE_STUNNED) {
            amount *= 1.35F;
        } else if (VeloriseElementAPI.hasElementalShield(this)) {
            amount *= 0.25F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        if (!level().isClientSide() && !customDeathHandled) {
            customDeathHandled = true;
            VeloriseElementAPI.clearElementalShield(this);
            transitionTo(STATE_DEATH, ANIM_DEATH);
            if (level() instanceof ServerLevel sl) {
                dropBossRewards(sl);
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.6F);
            }
        }
        super.die(cause);
    }

    private void dropBossRewards(ServerLevel sl) {
        int cryoCount = 2 + random.nextInt(3);
        spawnDrop(sl, new ItemStack(VeloriseStavesMod.getElementItem(Element.CRYO), cryoCount));
        if (VeloriseStavesMod.getElementItem(null) != null && random.nextDouble() < 0.25D) {
            spawnDrop(sl, new ItemStack(VeloriseStavesMod.getElementItem(null), 1));
        }
        net.minecraft.world.item.Item cryoRod = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("velorise_staves", "cryo_rod"));
        if (cryoRod != null && cryoRod != net.minecraft.world.item.Items.AIR) {
            spawnDrop(sl, new ItemStack(cryoRod, 1));
        }
    }

    private void spawnDrop(ServerLevel sl, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(sl, getX(), getY() + 1.0D, getZ(), stack);
        item.setDefaultPickUpDelay();
        sl.addFreshEntity(item);
    }

    private void awaken(ServerLevel sl) {
        transitionTo(STATE_AWAKEN, ANIM_AWAKEN);
        float shieldUnits = (getCorollaPhase() == 0) ? ROOTS_SHIELD_UNITS : COROLLA_SHIELD_UNITS;
        VeloriseElementAPI.setElementalShield(this, Element.CRYO, shieldUnits, true, 0.15F);
        entityData.set(DATA_SHIELD_PROGRESS, 1.0F);
        entityData.set(DATA_COROLLA_PHASE, 0);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.2F, 1.35F);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.0D, getZ(), 36, 1.5D, 1.2D, 1.5D, 0.08D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!anchorInitialized) {
            anchorPos = position();
            anchorInitialized = true;
        }
        setDeltaMovement(Vec3.ZERO);
        if (position().distanceToSqr(anchorPos) > 0.01D) {
            setPos(anchorPos.x, anchorPos.y, anchorPos.z);
        }

        if (level().isClientSide()) {
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        stateTicks++;
        entityData.set(DATA_ANIM_TICK, getAnimTick() + 1);
        tickBlizzardZones(sl);

        int st = getBossState();
        updateBossBar(st);

        if (st == STATE_DEATH) {
            if (stateTicks >= ANIM_DURATIONS_TICKS[ANIM_DEATH]) {
                discard();
            }
            return;
        }

        Player nearest = findNearestActivePlayer(sl, 28.0D);

        if (st == STATE_DORMANT) {
            bossEvent.setVisible(false);
            if (tickCount % 10 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.5D, getZ(), 4, 1.2D, 0.3D, 1.2D, 0.01D);
            }
            if (nearest != null && nearest.distanceToSqr(this) <= 100.0D) {
                awaken(sl);
            }
            return;
        }

        bossEvent.setVisible(true);

        if (nearest == null) {
            noPlayerTicks++;
            if (noPlayerTicks >= 200) {
                noPlayerTicks = 0;
                setHealth(getMaxHealth());
                VeloriseElementAPI.clearElementalShield(this);
                entityData.set(DATA_SHIELD_PROGRESS, 0.0F);
                transitionTo(STATE_DORMANT, ANIM_DORMANT);
            }
            return;
        } else {
            noPlayerTicks = 0;
        }

        if (st == STATE_AWAKEN) {
            faceTarget(nearest, 12.0F);
            if (stateTicks >= ANIM_DURATIONS_TICKS[ANIM_AWAKEN]) {
                transitionTo(STATE_COMBAT, ANIM_IDLE_COMBAT);
                attackCooldown = 25;
            }
            return;
        }

        if (st == STATE_STUNNED) {
            if (stateTicks % 15 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.2D, getZ(), 6, 1.4D, 0.4D, 1.4D, 0.01D);
            }
            if (stateTicks >= STUN_DURATION_TICKS) {
                int nextPhase = (getCorollaPhase() + 1) % 2;
                entityData.set(DATA_COROLLA_PHASE, nextPhase);
                float shieldUnits = (getCorollaPhase() == 0) ? ROOTS_SHIELD_UNITS : COROLLA_SHIELD_UNITS;
        VeloriseElementAPI.setElementalShield(this, Element.CRYO, shieldUnits, true, 0.15F);
                entityData.set(DATA_SHIELD_PROGRESS, 1.0F);
                transitionTo(STATE_COMBAT, ANIM_SPIN_FROST_RING);
                attackStartYaw = getYRot();
                attackCooldown = 35;
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4F, 0.85F);
            }
            return;
        }

        float shieldProg = VeloriseElementAPI.getShieldProgress(this);
        entityData.set(DATA_SHIELD_PROGRESS, shieldProg);

        int anim = getAnimId();
        int aTick = getAnimTick();

        if (anim == ANIM_IDLE_COMBAT) {
            faceTarget(nearest, 8.0F);
            if (attackCooldown > 0) {
                attackCooldown--;
            } else {
                pickNextAttack(nearest);
            }
        } else {
            executeAttackTick(sl, nearest, anim, aTick);
            if (aTick >= ANIM_DURATIONS_TICKS[anim]) {
                playAnim(ANIM_IDLE_COMBAT);
                attackCooldown = 24 + random.nextInt(18);
            }
        }
    }

    private void pickNextAttack(Player target) {
        double dist = Math.sqrt(target.distanceToSqr(this));
        int[] pool;
        if (dist < 7.5D) {
            pool = new int[]{
                    ANIM_FACEPLANT_SINGLE, ANIM_FACEPLANT_TRIPLE, ANIM_FLAIL_360,
                    ANIM_HALF_FLAIL_COMBO, ANIM_SPIN_FROST_RING
            };
        } else {
            pool = new int[]{
                    ANIM_ICE_BEAMS_CROSS, ANIM_HOMING_ICICLES, ANIM_BLIZZARD_CHANNEL,
                    ANIM_ROTARY_SPRAY, ANIM_FACEPLANT_TRIPLE
            };
        }
        int chosen = pool[random.nextInt(pool.length)];
        if (chosen == lastAttackAnim && pool.length > 1) {
            chosen = pool[(random.nextInt(pool.length - 1) + 1) % pool.length];
        }
        lastAttackAnim = chosen;
        faceTarget(target, 180.0F);
        attackStartYaw = getYRot();
        playAnim(chosen);
    }

    private void executeAttackTick(ServerLevel sl, Player target, int anim, int t) {
        switch (anim) {
            case ANIM_FACEPLANT_SINGLE -> {
                if (t < 16) faceTarget(target, 10.0F);
                if (t == 24) impactSlamInFront(sl, 6.2D, 3.6D, 11.0F);
            }
            case ANIM_FACEPLANT_TRIPLE -> {
                if (t < 14 || (t > 24 && t < 38) || (t > 48 && t < 62)) faceTarget(target, 12.0F);
                if (t == 20 || t == 44 || t == 68) impactSlamInFront(sl, 6.4D, 3.4D, 9.5F);
            }
            case ANIM_FLAIL_360 -> {
                if (t == 24 || t == 34) ringAoE(sl, 7.8D, 10.5F, 0.7D);
            }
            case ANIM_HALF_FLAIL_COMBO -> {
                if (t < 16 || (t > 34 && t < 44)) faceTarget(target, 10.0F);
                if (t == 24 || t == 52) arcSweepInFront(sl, 8.2D, 10.0F);
            }
            case ANIM_SPIN_FROST_RING -> {
                if (t == 26 || t == 36) ringAoE(sl, 9.0D, 9.0F, 1.1D);
            }
            case ANIM_ICE_BEAMS_CROSS -> {
                if (t < 14) faceTarget(target, 10.0F);
                if (t >= 18 && t <= 42 && t % 4 == 0) {
                    float progress = (t - 18) / 24.0F;
                    float angleA = getYRot() - 35.0F + progress * 70.0F;
                    float angleB = getYRot() + 35.0F - progress * 70.0F;
                    sweepBeamRay(sl, angleA, 17.0D, 7.5F);
                    sweepBeamRay(sl, angleB, 17.0D, 7.5F);
                }
            }
            case ANIM_ROTARY_SPRAY -> {
                // stem_1 animation already sweeps 0 -> -360 deg from 0.7s (14t) to 4.1s (82t); keep entity yaw fixed!
                if (t >= 16 && t <= 82 && t % 3 == 0) {
                    float a = Mth.clamp((t - 14.0F) / 68.0F, 0.0F, 1.0F);
                    float smooth = a * a * (3.0F - 2.0F * a);
                    float beamYaw = attackStartYaw + smooth * 360.0F;
                    sweepBeamRay(sl, beamYaw, 18.0D, 8.0F);
                }
            }
            case ANIM_HOMING_ICICLES -> {
                faceTarget(target, 10.0F);
                if (t == 16 || t == 21 || t == 26 || t == 31 || t == 36) {
                    double side = (t % 2 == 0) ? 2.2D : -2.2D;
                    float rad = getYRot() * Mth.DEG_TO_RAD;
                    Vec3 right = new Vec3(Math.cos(rad), 0.0D, Math.sin(rad));
                    Vec3 origin = position().add(right.scale(side)).add(0.0D, 4.8D, 0.0D);
                    Vec3 dir = target.position().add(0.0D, 1.0D, 0.0D).subtract(origin).normalize().scale(0.65D);
                    CryoIcicleProjectile.spawn(sl, this, target, origin, dir, 7.0F);
                    sl.playSound(null, origin.x, origin.y, origin.z, SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.HOSTILE, 0.9F, 1.4F);
                }
            }
            case ANIM_BLIZZARD_CHANNEL -> {
                faceTarget(target, 6.0F);
                if (t == 14 || t == 24 || t == 34 || t == 44 || t == 54) {
                    double ox = (random.nextDouble() - 0.5D) * 5.0D;
                    double oz = (random.nextDouble() - 0.5D) * 5.0D;
                    blizzardZones.add(new BlizzardZone(target.getX() + ox, target.getY(), target.getZ() + oz, 22));
                }
            }
            default -> { }
        }
    }

    private void impactSlamInFront(ServerLevel sl, double forwardDist, double radius, float dmg) {
        float rad = getYRot() * Mth.DEG_TO_RAD;
        Vec3 center = position().add(-Math.sin(rad) * forwardDist, 0.5D, Math.cos(rad) * forwardDist);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.4D, center.z, 28, radius * 0.45D, 0.4D, radius * 0.45D, 0.06D);
        sl.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5D, center.z, 2, 0.4D, 0.2D, 0.4D, 0.01D);
        sl.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.9F, 1.25F);
        AABB box = new AABB(center.x - radius, center.y - 1.5D, center.z - radius, center.x + radius, center.y + 3.0D, center.z + radius);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, le -> le.isAlive() && le != this && !(le instanceof CryoRegisvineEntity))) {
            if (e.position().distanceToSqr(center) <= radius * radius) {
                VeloriseElementAPI.dealElementalDamage(e, this, Element.CRYO, dmg, 1.0F, IcdPolicy.STANDARD);
            }
        }
    }

    private void ringAoE(ServerLevel sl, double radius, float dmg, double pushStrength) {
        for (int i = 0; i < 32; i++) {
            double ang = (Math.PI * 2.0D * i) / 32.0D;
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX() + Math.cos(ang) * radius * 0.75D, getY() + 0.8D, getZ() + Math.sin(ang) * radius * 0.75D, 2, 0.2D, 0.2D, 0.2D, 0.04D);
        }
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.3F, 0.7F);
        AABB box = getBoundingBox().inflate(radius, 2.5D, radius);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, le -> le.isAlive() && le != this && !(le instanceof CryoRegisvineEntity))) {
            double distSq = e.distanceToSqr(this);
            if (distSq <= radius * radius) {
                VeloriseElementAPI.dealElementalDamage(e, this, Element.CRYO, dmg, 1.0F, IcdPolicy.STANDARD);
                Vec3 push = e.position().subtract(position()).normalize().scale(pushStrength);
                e.push(push.x, 0.35D, push.z);
            }
        }
    }

    private void arcSweepInFront(ServerLevel sl, double radius, float dmg) {
        float rad = getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.85F);
        AABB box = getBoundingBox().inflate(radius, 2.5D, radius);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, le -> le.isAlive() && le != this && !(le instanceof CryoRegisvineEntity))) {
            Vec3 to = e.position().subtract(position());
            double dSq = to.horizontalDistanceSqr();
            if (dSq <= radius * radius && to.normalize().dot(forward) > 0.1D) {
                VeloriseElementAPI.dealElementalDamage(e, this, Element.CRYO, dmg, 1.0F, IcdPolicy.STANDARD);
            }
        }
    }

    private void sweepBeamRay(ServerLevel sl, float yawDeg, double length, float dmg) {
        float rad = yawDeg * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
        Vec3 start = position().add(0.0D, 1.4D, 0.0D);
        for (double step = 2.0D; step <= length; step += 1.6D) {
            Vec3 pt = start.add(dir.scale(step));
            sl.sendParticles(ParticleTypes.SNOWFLAKE, pt.x, pt.y, pt.z, 2, 0.18D, 0.18D, 0.18D, 0.01D);
        }
        AABB sweepBox = getBoundingBox().inflate(length, 3.0D, length);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sweepBox, le -> le.isAlive() && le != this && !(le instanceof CryoRegisvineEntity))) {
            Vec3 rel = e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D).subtract(start);
            double proj = rel.dot(dir);
            if (proj > 1.0D && proj <= length) {
                Vec3 closest = start.add(dir.scale(proj));
                if (e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D).distanceToSqr(closest) <= 2.25D) {
                    VeloriseElementAPI.dealElementalDamage(e, this, Element.CRYO, dmg, 1.0F, IcdPolicy.STANDARD);
                }
            }
        }
    }

    private void tickBlizzardZones(ServerLevel sl) {
        if (blizzardZones.isEmpty()) return;
        Iterator<BlizzardZone> it = blizzardZones.iterator();
        while (it.hasNext()) {
            BlizzardZone z = it.next();
            z.ticksLeft--;
            if (z.ticksLeft > 0) {
                if (z.ticksLeft % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double ang = (Math.PI * 2.0D * i) / 8.0D;
                        sl.sendParticles(ParticleTypes.SNOWFLAKE, z.x + Math.cos(ang) * 2.4D, z.y + 0.15D, z.z + Math.sin(ang) * 2.4D, 1, 0.0D, 0.05D, 0.0D, 0.01D);
                    }
                }
            } else {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, z.x, z.y + 1.0D, z.z, 24, 1.2D, 1.8D, 1.2D, 0.08D);
                sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, z.x, z.y + 0.6D, z.z, 12, 1.0D, 0.5D, 1.0D, 0.08D);
                sl.playSound(null, z.x, z.y, z.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.85F, 1.35F);
                AABB box = new AABB(z.x - 2.6D, z.y - 1.0D, z.z - 2.6D, z.x + 2.6D, z.y + 3.5D, z.z + 2.6D);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, le -> le.isAlive() && le != this && !(le instanceof CryoRegisvineEntity))) {
                    VeloriseElementAPI.dealElementalDamage(e, this, Element.CRYO, 9.0F, 1.0F, IcdPolicy.STANDARD);
                }
                it.remove();
            }
        }
    }

    private void updateBossBar(int st) {
        bossEvent.setProgress(getHealth() / Math.max(1.0F, getMaxHealth()));
        if (st == STATE_STUNNED) {
            bossEvent.setColor(BossEvent.BossBarColor.WHITE);
            bossEvent.setName(Component.translatable("entity.velorise_staves.cryo_regisvine")
                    .append(Component.literal(" — STUNNED")));
        } else {
            float rem = VeloriseElementAPI.getRemainingShieldUnits(this);
            bossEvent.setColor(BossEvent.BossBarColor.BLUE);
            if (rem > 0.01F) {
                String loc = getCorollaPhase() == 0 ? "Root" : "Corolla";
                bossEvent.setName(Component.translatable("entity.velorise_staves.cryo_regisvine")
                        .append(Component.literal(String.format(Locale.ROOT, " — Cryo %s Shield: %.1fU", loc, rem))));
            } else {
                bossEvent.setName(Component.translatable("entity.velorise_staves.cryo_regisvine"));
            }
        }
    }

    private void faceTarget(LivingEntity target, float maxTurnDeg) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float desired = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float next = Mth.approachDegrees(getYRot(), desired, maxTurnDeg);
        setYRot(next);
        yBodyRot = next;
        yHeadRot = next;
    }

    private @Nullable Player findNearestActivePlayer(ServerLevel sl, double radius) {
        Player best = null;
        double bestSq = radius * radius;
        for (Player p : sl.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            double dSq = p.distanceToSqr(this);
            if (dSq < bestSq) {
                bestSq = dSq;
                best = p;
            }
        }
        return best;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnchorX")) {
            anchorPos = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
            anchorInitialized = true;
        }
        entityData.set(DATA_COROLLA_PHASE, tag.getInt("CorollaPhase"));
        int savedState = tag.getInt("BossState");
        if (savedState == STATE_COMBAT || savedState == STATE_AWAKEN) {
            transitionTo(STATE_DORMANT, ANIM_DORMANT);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (anchorInitialized) {
            tag.putDouble("AnchorX", anchorPos.x);
            tag.putDouble("AnchorY", anchorPos.y);
            tag.putDouble("AnchorZ", anchorPos.z);
        }
        tag.putInt("CorollaPhase", getCorollaPhase());
        tag.putInt("BossState", getBossState());
    }
}
