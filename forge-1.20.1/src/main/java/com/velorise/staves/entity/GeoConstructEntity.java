package com.velorise.staves.entity;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Physical 1x2x1 Geo Stone Stele summoned by the Netherite Evoker Fang Staff.
 * Has 50 hearts (100 HP), lasts 30s (max 3 per player), blocks damage and projectiles,
 * pulses 1.0U Geo 2 blocks around every 1s, and resonates with nearby Geo Constructs.
 */
public class GeoConstructEntity extends Entity {
    public static final float MAX_HEALTH = 100.0F; // 50 hearts
    public static final int MAX_LIFETIME_TICKS = 600; // 30 seconds
    public static final int MAX_CONSTRUCTS_PER_OWNER = 3;
    public static final int PULSE_INTERVAL_TICKS = 20; // 1 second
    public static final double PULSE_AOE_INFLATE = 2.0D;
    public static final double RESONANCE_RADIUS = 6.0D;
    public static final int RESONANCE_BEAM_COLOR = 0xFFD60A;

    private static final Vector3f GEO_GOLD_COLOR = new Vector3f(0.96F, 0.76F, 0.22F);

    public static final ThreadLocal<Boolean> IS_GEO_PULSE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(GeoConstructEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_PULSE_TICK =
            SynchedEntityData.defineId(GeoConstructEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HURT_TICK =
            SynchedEntityData.defineId(GeoConstructEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;
    private int ageTicks;
    private long createdGameTime;
    private long lastPulseServerTick = -100L;

    public GeoConstructEntity(EntityType<? extends GeoConstructEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.blocksBuilding = true;
    }

    public GeoConstructEntity(
            EntityType<? extends GeoConstructEntity> type,
            Level level,
            double x,
            double y,
            double z,
            float yawDegrees,
            @Nullable LivingEntity owner
    ) {
        this(type, level);
        this.setPos(x, y, z);
        this.setYRot(yawDegrees);
        this.setOwner(owner);
        this.createdGameTime = level.getGameTime();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH, MAX_HEALTH);
        this.entityData.define(DATA_PULSE_TICK, -100);
        this.entityData.define(DATA_HURT_TICK, -100);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH, Math.max(0.0F, Math.min(MAX_HEALTH, health)));
    }

    public int getLastPulseTick() {
        return this.entityData.get(DATA_PULSE_TICK);
    }

    public int getLastHurtTick() {
        return this.entityData.get(DATA_HURT_TICK);
    }

    public void setOwner(@Nullable Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
            this.cachedOwner = owner;
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    @Nullable
    public Entity getOwnerEntity() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        }
        if (this.ownerUUID != null && this.level() instanceof ServerLevel sl) {
            this.cachedOwner = sl.getEntity(this.ownerUUID);
            if (this.cachedOwner == null && sl.getServer() != null) {
                this.cachedOwner = sl.getServer().getPlayerList().getPlayer(this.ownerUUID);
            }
            return this.cachedOwner;
        }
        return null;
    }

    @Nullable
    private ServerPlayer resolveOwnerPlayer(ServerLevel level) {
        if (this.ownerUUID == null || level.getServer() == null) return null;
        return level.getServer().getPlayerList().getPlayer(this.ownerUUID);
    }

    public long getCreatedGameTime() {
        return this.createdGameTime;
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** Enforces the max 3 alive Geo Constructs limit per player by shattering oldest excess constructs. */
    public static void enforceMaxConstructsForOwner(
            ServerLevel level,
            EntityType<? extends GeoConstructEntity> type,
            UUID ownerUuid
    ) {
        List<GeoConstructEntity> owned = new ArrayList<>();
        MinecraftServer server = level.getServer();
        if (server != null) {
            for (ServerLevel world : server.getAllLevels()) {
                owned.addAll(world.getEntities(type, c -> c.isAlive() && !c.isRemoved() && ownerUuid.equals(c.getOwnerUUID())));
            }
        } else {
            owned.addAll(level.getEntities(type, c -> c.isAlive() && !c.isRemoved() && ownerUuid.equals(c.getOwnerUUID())));
        }
        if (owned.size() < MAX_CONSTRUCTS_PER_OWNER) return;

        owned.sort(Comparator.comparingLong(GeoConstructEntity::getCreatedGameTime).thenComparingInt(Entity::getId));
        int toRemove = owned.size() - (MAX_CONSTRUCTS_PER_OWNER - 1);
        for (int i = 0; i < toRemove; i++) {
            owned.get(i).shatter();
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);

        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }

        if (this.ageTicks == 0) {
            liftOverlappingEntities(sl);
            sl.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(),
                    SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.75F);
            sl.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.35F),
                    this.getX(), this.getY() + 0.4D, this.getZ(), 14, 0.45D, 0.35D, 0.45D, 0.04D);
        }

        this.ageTicks++;
        if (this.ageTicks >= MAX_LIFETIME_TICKS) {
            shatter();
            return;
        }

        if (this.ageTicks == 4 || (this.ageTicks > 4 && (this.ageTicks - 4) % PULSE_INTERVAL_TICKS == 0)) {
            long now = sl.getGameTime();
            if (now - this.lastPulseServerTick >= 16L || this.ageTicks == 4) {
                emitPulseAndResonate(sl, new HashSet<>());
            }
        }
    }

    private void liftOverlappingEntities(ServerLevel level) {
        List<LivingEntity> overlapping = level.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(0.05D),
                e -> e.isAlive() && !e.isSpectator()
        );
        for (LivingEntity living : overlapping) {
            double dy = (this.getBoundingBox().maxY + 0.05D) - living.getY();
            if (dy > 0.0D && dy <= 2.2D && level.noCollision(living, living.getBoundingBox().move(0.0D, dy, 0.0D))) {
                living.teleportTo(living.getX(), living.getY() + dy, living.getZ());
            }
        }
    }

    public void emitPulseAndResonate(ServerLevel level, Set<UUID> chainVisited) {
        if (!this.isAlive() || this.isRemoved()) return;
        if (!chainVisited.add(this.getUUID())) return;

        long now = level.getGameTime();
        this.lastPulseServerTick = now;
        this.entityData.set(DATA_PULSE_TICK, this.tickCount);

        spawnGeoPulseRing(level);
        level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.75F, 0.9F + level.random.nextFloat() * 0.2F);
        level.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(),
                SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.55F, 0.8F);

        ServerPlayer ownerPlayer = resolveOwnerPlayer(level);
        Entity owner = getOwnerEntity();

        AABB pulseBox = this.getBoundingBox().inflate(PULSE_AOE_INFLATE);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                pulseBox,
                e -> e.isAlive() && !e.isRemoved() && !e.isSpectator()
        );

        IS_GEO_PULSE.set(Boolean.TRUE);
        try {
            for (LivingEntity target : targets) {
                if (this.ownerUUID != null && target.getUUID().equals(this.ownerUUID)) continue;
                if (owner != null && target.isAlliedTo(owner)) continue;
                ElementalGaugeManager.applyElement(level, target, Element.GEO, 1.0F, ownerPlayer, 2.0F);
            }
        } finally {
            IS_GEO_PULSE.remove();
        }

        Vec3 thisCenter = this.position().add(0.0D, 1.1D, 0.0D);
        List<GeoConstructEntity> nearby = level.getEntitiesOfClass(
                GeoConstructEntity.class,
                this.getBoundingBox().inflate(RESONANCE_RADIUS),
                other -> other != this && other.isAlive() && !other.isRemoved()
        );
        for (GeoConstructEntity other : nearby) {
            if (other.distanceToSqr(this) > RESONANCE_RADIUS * RESONANCE_RADIUS) continue;
            Vec3 otherCenter = other.position().add(0.0D, 1.1D, 0.0D);
            ModNetwork.sendElectricArc(level, thisCenter, otherCenter, 10, RESONANCE_BEAM_COLOR);
            if (!chainVisited.contains(other.getUUID()) && (now - other.lastPulseServerTick) >= 4L) {
                other.emitPulseAndResonate(level, chainVisited);
            }
        }
    }

    private void spawnGeoPulseRing(ServerLevel level) {
        DustParticleOptions dust = new DustParticleOptions(GEO_GOLD_COLOR, 1.3F);
        int points = 18;
        double radius = 2.0D;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double px = this.getX() + Math.cos(angle) * radius;
            double pz = this.getZ() + Math.sin(angle) * radius;
            level.sendParticles(dust, px, this.getY() + 0.2D, pz, 1, 0.06D, 0.08D, 0.06D, 0.01D);
        }
        level.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 1.0D, this.getZ(), 6, 0.45D, 0.65D, 0.45D, 0.02D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || !this.isAlive() || amount <= 0.0F) {
            return false;
        }
        if (Boolean.TRUE.equals(IS_GEO_PULSE.get())) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            shatter();
            return true;
        }
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.DROWN)) {
            return false;
        }
        if (source.getDirectEntity() instanceof EvokerFangs fangs && fangs.getTags().contains("velorise_staves:geo_fangs")) {
            Entity fangOwner = fangs.getOwner();
            if (fangOwner != null && fangOwner.getUUID().equals(this.ownerUUID)) {
                return false;
            }
        }

        float nextHp = getHealth() - amount;
        setHealth(nextHp);
        this.entityData.set(DATA_HURT_TICK, this.tickCount);

        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.9F, 0.85F + sl.random.nextFloat() * 0.25F);
        sl.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.15F),
                this.getX(), this.getY() + 1.0D, this.getZ(), 7, 0.35D, 0.65D, 0.35D, 0.02D);

        if (nextHp <= 0.0F) {
            shatter();
        }
        return true;
    }

    public boolean handleProjectileBlocked(ServerLevel level, Projectile projectile, String staffKind) {
        if (!this.isAlive() || this.isRemoved()) return false;
        float dmg;
        if (!staffKind.isEmpty()) {
            boolean isNetherite = staffKind.startsWith("netherite_");
            String baseKind = isNetherite ? staffKind.substring("netherite_".length()) : staffKind;
            float base = switch (baseKind) {
                case "dragonfire" -> isNetherite ? 10.0F : 7.0F;
                case "ghastfire" -> isNetherite ? 9.0F : 6.0F;
                case "wither", "shulker" -> isNetherite ? 6.0F : 4.0F;
                case "inferno", "frost" -> isNetherite ? 4.5F : 3.0F;
                case "wind" -> isNetherite ? 2.5F : 1.5F;
                default -> 1.0F;
            };
            dmg = (float) (base * StavesConfig.DAMAGE_MULTIPLIER.get());
        } else if (projectile instanceof AbstractArrow arrow) {
            dmg = (float) Math.max(2.0D, arrow.getDeltaMovement().length() * arrow.getBaseDamage());
        } else if (projectile instanceof AbstractHurtingProjectile) {
            dmg = (projectile instanceof LargeFireball || projectile instanceof WitherSkull) ? 6.0F : 3.0F;
        } else if (projectile instanceof ShulkerBullet) {
            dmg = 4.0F;
        } else {
            dmg = 2.0F;
        }

        level.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 0.9F, 1.05F);
        level.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.2F),
                projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
        this.hurt(level.damageSources().thrown(projectile, projectile.getOwner()), dmg);
        return true;
    }

    /**
     * Checks whether any alive Geo Construct lies strictly along the path between the incoming
     * damage origin and the victim, transferring the incoming damage to the pillar if intercepted.
     */
    public static boolean interceptIncomingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim.level() instanceof ServerLevel level) || amount <= 0.0F || !victim.isAlive()) {
            return false;
        }
        if (Boolean.TRUE.equals(IS_GEO_PULSE.get())) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Vec3 origin = getDamageOrigin(source);
        if (origin == null || source.getEntity() == victim) {
            return false;
        }

        Vec3 victimCenter = victim.getBoundingBox().getCenter();
        Vec3 victimEyes = victim.getEyePosition();
        List<GeoConstructEntity> nearby = level.getEntitiesOfClass(
                GeoConstructEntity.class,
                victim.getBoundingBox().inflate(8.0D),
                c -> c.isAlive() && !c.isRemoved()
        );
        for (GeoConstructEntity construct : nearby) {
            AABB shieldBox = construct.getBoundingBox().inflate(0.2D, 0.15D, 0.2D);
            if (shieldBox.contains(origin)) continue;
            if (shieldBox.clip(origin, victimCenter).isPresent() || shieldBox.clip(origin, victimEyes).isPresent()) {
                if (construct.hurt(source, amount)) {
                    level.playSound(null, construct.getX(), construct.getY() + 1.0D, construct.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 0.9F, 1.0F);
                    level.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.2F),
                            construct.getX(), construct.getY() + 1.0D, construct.getZ(),
                            10, 0.4D, 0.6D, 0.4D, 0.03D);
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static Vec3 getDamageOrigin(DamageSource source) {
        if (source.getSourcePosition() != null) {
            return source.getSourcePosition();
        }
        if (source.getDirectEntity() != null) {
            return source.getDirectEntity().getBoundingBox().getCenter();
        }
        if (source.getEntity() != null) {
            return source.getEntity().getBoundingBox().getCenter();
        }
        return null;
    }

    public void shatter() {
        if (this.isRemoved()) return;
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                    SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.1F, 0.85F);
            sl.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.5F),
                    this.getX(), this.getY() + 1.0D, this.getZ(), 24, 0.45D, 0.8D, 0.45D, 0.06D);
            sl.sendParticles(ParticleTypes.WAX_ON,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 10, 0.4D, 0.7D, 0.4D, 0.04D);
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("ConstructHealth")) {
            setHealth(tag.getFloat("ConstructHealth"));
        }
        this.ageTicks = tag.getInt("Age");
        this.createdGameTime = tag.getLong("CreatedGameTime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putFloat("ConstructHealth", getHealth());
        tag.putInt("Age", this.ageTicks);
        tag.putLong("CreatedGameTime", this.createdGameTime);
    }
}
