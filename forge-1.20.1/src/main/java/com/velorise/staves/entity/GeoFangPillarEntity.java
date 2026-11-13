package com.velorise.staves.entity;

import com.velorise.staves.advancement.ModAdvancements;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Physical Geo Stone Stele summoned by Evoker Fang Staff (Basic: 1x1x1, 1x creator MaxHP)
 * and Netherite Evoker Fang Staff (Netherite: 1x2x1, 2x creator MaxHP).
 * Lasts 30s (max 3 per player), blocks damage and projectiles, taunts hit mobs onto the pillar
 * without aggroing them onto the creator, and emits expanding square Geo waves every 2s
 * (Basic: 3 blocks, 0.5 heart / 0.5U; Netherite: 5 blocks, 1 heart / 1U) that resonate with nearby Geo Constructs.
 */
public class GeoFangPillarEntity extends Entity {
    public static final float MAX_HEALTH = 40.0F; // Default fallback for Netherite (2x standard 20 HP)
    public static final float BASIC_MAX_HEALTH = 20.0F; // Default fallback for Basic (1x standard 20 HP)
    public static final int MAX_LIFETIME_TICKS = 600; // 30 seconds
    public static final int MAX_CONSTRUCTS_PER_OWNER = 3;
    public static final int PULSE_INTERVAL_TICKS = 40; // 2 seconds
    public static final int WAVE_DURATION_TICKS = 20; // 1.0s outward travel
    public static final double WAVE_START_RADIUS = 0.55D;
    public static final double BASIC_WAVE_END_RADIUS = 3.5D; // 3 blocks around 0.5m half-width pillar
    public static final double NETHERITE_WAVE_END_RADIUS = 5.5D; // 5 blocks around 0.5m half-width pillar
    public static final double WAVE_END_RADIUS = NETHERITE_WAVE_END_RADIUS;
    public static final float BASIC_WAVE_GEO_DAMAGE = 1.0F; // 0.5 heart
    public static final float NETHERITE_WAVE_GEO_DAMAGE = 2.0F; // 1.0 heart
    public static final float WAVE_GEO_DAMAGE = BASIC_WAVE_GEO_DAMAGE;
    public static final float BASIC_WAVE_GEO_UNITS = 0.5F;
    public static final float NETHERITE_WAVE_GEO_UNITS = 1.0F;
    public static final float WAVE_GEO_UNITS = BASIC_WAVE_GEO_UNITS;

    private static final Vector3f GEO_GOLD_COLOR = new Vector3f(0.96F, 0.76F, 0.22F);

    public static final ThreadLocal<Boolean> IS_GEO_PULSE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final EntityDataAccessor<Float> DATA_MAX_HEALTH =
            SynchedEntityData.defineId(GeoFangPillarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(GeoFangPillarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_PULSE_SEQ =
            SynchedEntityData.defineId(GeoFangPillarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HURT_TICK =
            SynchedEntityData.defineId(GeoFangPillarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_NETHERITE =
            SynchedEntityData.defineId(GeoFangPillarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STELE =
            SynchedEntityData.defineId(GeoFangPillarEntity.class, EntityDataSerializers.BOOLEAN);

    private static final class ActiveGeoWave {
        int age;
        final Set<UUID> hitEntities = new HashSet<>();
        final Set<UUID> triggeredConstructs;

        ActiveGeoWave(Set<UUID> triggeredConstructs) {
            this.triggeredConstructs = triggeredConstructs;
        }
    }

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;
    private int ageTicks;
    private long createdGameTime;
    private long lastWaveStartGameTime = -100L;
    private final List<ActiveGeoWave> activeWaves = new ArrayList<>();
    private final Set<UUID> tauntedMobs = new HashSet<>();
    private final Map<UUID, Long> lastTauntAttackTime = new HashMap<>();

    // Client-side concentric ring visual tracking
    private int lastClientPulseSeq;
    private int lastClientWaveStartTick = -100;
    private final List<Float> clientWaveAges = new ArrayList<>();

    public GeoFangPillarEntity(EntityType<? extends GeoFangPillarEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.blocksBuilding = true;
        this.refreshDimensions();
    }

    public GeoFangPillarEntity(
            EntityType<? extends GeoFangPillarEntity> type,
            Level level,
            double x,
            double y,
            double z,
            float yawDegrees,
            @Nullable LivingEntity owner
    ) {
        this(type, level, x, y, z, yawDegrees, owner, true);
    }

    public GeoFangPillarEntity(
            EntityType<? extends GeoFangPillarEntity> type,
            Level level,
            double x,
            double y,
            double z,
            float yawDegrees,
            @Nullable LivingEntity owner,
            boolean isNetherite
    ) {
        this(type, level);
        this.setNetherite(isNetherite);
        this.setPos(x, y, z);
        this.setYRot(yawDegrees);
        this.setOwner(owner);
        float mult = isNetherite ? 2.0F : 1.0F;
        float fallbackMax = isNetherite ? MAX_HEALTH : BASIC_MAX_HEALTH;
        float scaledMax = owner != null ? Math.max(2.0F, owner.getMaxHealth() * mult) : fallbackMax;
        this.entityData.set(DATA_MAX_HEALTH, scaledMax);
        this.entityData.set(DATA_HEALTH, scaledMax);
        this.refreshDimensions();
        this.createdGameTime = level.getGameTime();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_MAX_HEALTH, MAX_HEALTH);
        this.entityData.define(DATA_HEALTH, MAX_HEALTH);
        this.entityData.define(DATA_PULSE_SEQ, 0);
        this.entityData.define(DATA_HURT_TICK, -100);
        this.entityData.define(DATA_NETHERITE, false);
        this.entityData.define(DATA_STELE, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_NETHERITE.equals(key) || DATA_STELE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(1.0F, (isNetherite() || isStoneStele()) ? 2.0F : 1.0F);
    }

    public boolean isStoneStele() {
        return this.entityData.get(DATA_STELE);
    }

    public void setStoneStele(boolean stele) {
        this.entityData.set(DATA_STELE, stele);
        this.refreshDimensions();
    }

    public boolean isNetherite() {
        return this.entityData.get(DATA_NETHERITE);
    }

    public void setNetherite(boolean netherite) {
        this.entityData.set(DATA_NETHERITE, netherite);
        this.refreshDimensions();
    }

    public double getWaveEndRadius() {
        if (isStoneStele()) {
            return 6.5D; // 6-block resonant radius around 0.5m half-width Stone Stele
        }
        return isNetherite() ? NETHERITE_WAVE_END_RADIUS : BASIC_WAVE_END_RADIUS;
    }

    public float getWaveGeoDamage() {
        if (isStoneStele()) {
            Entity owner = getOwnerEntity();
            float maxHp = owner instanceof LivingEntity le ? le.getMaxHealth() : 20.0F;
            return (isNetherite() ? 3.0F : 2.0F) + maxHp * 0.019F; // Dominance of Earth: +1.9% Max HP
        }
        return isNetherite() ? NETHERITE_WAVE_GEO_DAMAGE : BASIC_WAVE_GEO_DAMAGE;
    }

    public float getWaveGeoUnits() {
        if (isStoneStele()) {
            return isNetherite() ? 1.5F : 1.0F;
        }
        return isNetherite() ? NETHERITE_WAVE_GEO_UNITS : BASIC_WAVE_GEO_UNITS;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getMaxConstructHealth() {
        return Math.max(2.0F, this.entityData.get(DATA_MAX_HEALTH));
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH, Math.max(0.0F, Math.min(getMaxConstructHealth(), health)));
    }

    public int getPulseSeq() {
        return this.entityData.get(DATA_PULSE_SEQ);
    }

    public int getLastHurtTick() {
        return this.entityData.get(DATA_HURT_TICK);
    }

    public int getLastClientWaveStartTick() {
        return this.lastClientWaveStartTick;
    }

    public int getAgeTicks() {
        return this.ageTicks;
    }

    public List<Float> getClientWaveAges() {
        return this.clientWaveAges;
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
    public AABB getBoundingBoxForCulling() {
        double cullR = getWaveEndRadius() + 0.5D;
        return this.getBoundingBox().inflate(cullR, 0.5D, cullR);
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
            EntityType<? extends GeoFangPillarEntity> type,
            UUID ownerUuid
    ) {
        List<GeoFangPillarEntity> owned = new ArrayList<>();
        MinecraftServer server = level.getServer();
        if (server != null) {
            for (ServerLevel world : server.getAllLevels()) {
                owned.addAll(world.getEntities(type, c -> c.isAlive() && !c.isRemoved() && ownerUuid.equals(c.getOwnerUUID())));
            }
        } else {
            owned.addAll(level.getEntities(type, c -> c.isAlive() && !c.isRemoved() && ownerUuid.equals(c.getOwnerUUID())));
        }
        if (owned.size() < MAX_CONSTRUCTS_PER_OWNER) return;

        owned.sort(Comparator.comparingLong(GeoFangPillarEntity::getCreatedGameTime).thenComparingInt(Entity::getId));
        int toRemove = owned.size() - (MAX_CONSTRUCTS_PER_OWNER - 1);
        for (int i = 0; i < toRemove; i++) {
            owned.get(i).shatter();
        }
    }

    public static int countActiveStelesForOwner(
            ServerLevel level,
            EntityType<GeoFangPillarEntity> type,
            UUID ownerId
    ) {
        int count = 0;
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof GeoFangPillarEntity construct
                    && construct.getType() == type
                    && construct.isAlive()
                    && !construct.isRemoved()
                    && construct.isStoneStele()
                    && ownerId.equals(construct.getOwnerUUID())) {
                count++;
            }
        }
        return count;
    }

    public static void enforceMaxSteleForOwner(
            ServerLevel level,
            EntityType<GeoFangPillarEntity> type,
            UUID ownerId,
            int maxAllowed
    ) {
        List<GeoFangPillarEntity> owned = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof GeoFangPillarEntity construct
                    && construct.getType() == type
                    && construct.isAlive()
                    && !construct.isRemoved()
                    && construct.isStoneStele()
                    && ownerId.equals(construct.getOwnerUUID())) {
                owned.add(construct);
            }
        }
        if (owned.size() < maxAllowed) return;
        owned.sort(Comparator.comparingLong(GeoFangPillarEntity::getCreatedGameTime).thenComparingInt(Entity::getId));
        int toRemove = owned.size() - (maxAllowed - 1);
        for (int i = 0; i < toRemove; i++) {
            owned.get(i).shatter();
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        float expectedHeight = (isNetherite() || isStoneStele()) ? 2.0F : 1.0F;
        if (Math.abs(this.getBbHeight() - expectedHeight) > 0.01F) {
            this.refreshDimensions();
        }

        if (!(this.level() instanceof ServerLevel sl)) {
            tickClientWaves();
            return;
        }

        if (this.ageTicks <= 2) {
            this.refreshDimensions();
            liftOverlappingEntities(sl);
            if (this.ageTicks == 0) {
                sl.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(),
                        SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.75F);
                sl.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.35F),
                        this.getX(), this.getY() + 0.4D, this.getZ(), 14, 0.45D, 0.35D, 0.45D, 0.04D);
            }
        }

        this.ageTicks++;
        if (this.ageTicks >= MAX_LIFETIME_TICKS) {
            shatter();
            return;
        }

        if (this.ageTicks == 4 || (this.ageTicks > 4 && (this.ageTicks - 4) % PULSE_INTERVAL_TICKS == 0)) {
            Set<UUID> chain = new HashSet<>();
            chain.add(this.getUUID());
            startWave(sl, chain);
        }

        Iterator<ActiveGeoWave> it = this.activeWaves.iterator();
        while (it.hasNext()) {
            ActiveGeoWave wave = it.next();
            tickActiveWave(sl, wave);
            if (wave.age >= WAVE_DURATION_TICKS) {
                it.remove();
            }
        }

        tickTauntedMobs(sl);
    }

    private void tickClientWaves() {
        for (int i = this.clientWaveAges.size() - 1; i >= 0; i--) {
            float next = this.clientWaveAges.get(i) + 1.0F;
            if (next >= WAVE_DURATION_TICKS) {
                this.clientWaveAges.remove(i);
            } else {
                this.clientWaveAges.set(i, next);
            }
        }
        int seq = getPulseSeq();
        if (this.tickCount <= 2 && seq > 1) {
            this.lastClientPulseSeq = seq;
        } else if (seq > this.lastClientPulseSeq) {
            int diff = Math.min(2, seq - this.lastClientPulseSeq);
            for (int i = 0; i < diff; i++) {
                this.clientWaveAges.add(0.0F);
            }
            this.lastClientPulseSeq = seq;
            this.lastClientWaveStartTick = this.tickCount;
        }
    }

    public void liftEntityOntoPillar(LivingEntity living) {
        if (living == null || !living.isAlive() || living.isSpectator()) return;
        this.refreshDimensions();
        double topY = this.getBoundingBox().maxY + 0.06D;
        double dy = topY - living.getY();
        if (dy > -0.25D && dy <= 2.65D) {
            AABB targetBox = living.getBoundingBox().move(
                    this.getX() - living.getX(),
                    Math.max(0.0D, dy),
                    this.getZ() - living.getZ()
            );
            if (this.level().noCollision(living, targetBox)) {
                living.teleportTo(this.getX(), topY, this.getZ());
                Vec3 dv = living.getDeltaMovement();
                living.setDeltaMovement(dv.x, Math.max(0.12D, dv.y), dv.z);
                living.setOnGround(true);
                living.fallDistance = 0.0F;
                living.hurtMarked = true;
            }
        }
    }

    private void liftOverlappingEntities(ServerLevel level) {
        List<LivingEntity> overlapping = level.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(0.25D, 0.25D, 0.25D),
                e -> e.isAlive() && !e.isSpectator()
        );
        for (LivingEntity living : overlapping) {
            double topY = this.getBoundingBox().maxY + 0.06D;
            double dy = topY - living.getY();
            if (dy > 0.0D && dy <= 2.65D && level.noCollision(living, living.getBoundingBox().move(0.0D, dy, 0.0D))) {
                living.teleportTo(living.getX(), topY, living.getZ());
                Vec3 dv = living.getDeltaMovement();
                living.setDeltaMovement(dv.x, Math.max(0.12D, dv.y), dv.z);
                living.setOnGround(true);
                living.fallDistance = 0.0F;
                living.hurtMarked = true;
            }
        }
    }

    public void startWave(ServerLevel level, Set<UUID> sharedChain) {
        if (!this.isAlive() || this.isRemoved()) return;
        this.lastWaveStartGameTime = level.getGameTime();
        this.entityData.set(DATA_PULSE_SEQ, getPulseSeq() + 1);
        this.activeWaves.add(new ActiveGeoWave(sharedChain));

        double centerH = isNetherite() ? 1.0D : 0.5D;
        level.playSound(null, this.getX(), this.getY() + centerH, this.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.75F, 0.9F + level.random.nextFloat() * 0.2F);
        level.playSound(null, this.getX(), this.getY() + 0.5D, this.getZ(),
                SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.5F, 0.8F);
    }

    private double extentAtAge(int age) {
        double t = Mth.clamp((double) age / (double) WAVE_DURATION_TICKS, 0.0D, 1.0D);
        return Mth.lerp(t, WAVE_START_RADIUS, getWaveEndRadius());
    }

    private void tickActiveWave(ServerLevel level, ActiveGeoWave wave) {
        double prevR = extentAtAge(wave.age);
        wave.age++;
        double currR = extentAtAge(wave.age);
        double innerBand = Math.max(0.0D, prevR - 0.65D);

        AABB scan = new AABB(
                this.getX() - currR - 1.0D, this.getY() - 1.5D, this.getZ() - currR - 1.0D,
                this.getX() + currR + 1.0D, this.getY() + 3.0D, this.getZ() + currR + 1.0D
        );

        ServerPlayer ownerPlayer = resolveOwnerPlayer(level);
        Entity owner = getOwnerEntity();
        LivingEntity ownerLiving = owner instanceof LivingEntity le ? le : null;
        float waveDamage = getWaveGeoDamage();
        float waveUnits = getWaveGeoUnits();
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                scan,
                e -> e.isAlive() && !e.isRemoved() && !e.isSpectator()
        );

        IS_GEO_PULSE.set(Boolean.TRUE);
        try {
            for (LivingEntity target : candidates) {
                if (wave.hitEntities.contains(target.getUUID())) continue;
                if (isOwnerOrAlly(target, owner)) continue;
                double edgeDist = horizontalSquareEdgeDistance(target);
                if (edgeDist <= currR && edgeDist >= innerBand) {
                    wave.hitEntities.add(target.getUUID());
                    if (ownerPlayer != null) {
                        ElementalGaugeManager.applyElement(level, target, Element.GEO, waveUnits, ownerPlayer, waveDamage);
                    } else {
                        ElementalGaugeManager.applyElementWithCredit(level, target, Element.GEO, waveUnits, ownerLiving, waveDamage);
                    }
                    tauntMob(target, owner);
                }
            }
        } finally {
            IS_GEO_PULSE.remove();
        }

        // Expanding square wave physically triggers other Geo Constructs on contact to launch their own wave
        long now = level.getGameTime();
        List<GeoFangPillarEntity> nearbyConstructs = level.getEntitiesOfClass(
                GeoFangPillarEntity.class,
                scan.inflate(0.5D, 1.0D, 0.5D),
                other -> other != this && other.isAlive() && !other.isRemoved()
        );
        for (GeoFangPillarEntity other : nearbyConstructs) {
            if (wave.triggeredConstructs.contains(other.getUUID())) continue;
            double edgeDist = horizontalSquareEdgeDistance(other);
            if (edgeDist <= currR && (now < other.lastWaveStartGameTime || (now - other.lastWaveStartGameTime) >= 4L)) {
                wave.triggeredConstructs.add(other.getUUID());
                other.startWave(level, wave.triggeredConstructs);
                if (ownerPlayer != null) {
                    ModAdvancements.trigger(ownerPlayer, ModAdvancements.EVENT_GEO_FANG_PILLAR);
                }
            }
        }
    }

    private boolean isOwnerOrAlly(LivingEntity target, @Nullable Entity owner) {
        if (this.ownerUUID != null && target.getUUID().equals(this.ownerUUID)) {
            return true;
        }
        if (owner == null) {
            return false;
        }
        if (target == owner || target.isAlliedTo(owner)) {
            return true;
        }
        return target instanceof net.minecraft.world.entity.monster.Vex vex && vex.getOwner() == owner;
    }

    /**
     * Redirects a hit mob's hostility away from the pillar's owner and lures it toward this construct.
     */
    public void tauntMob(LivingEntity target, @Nullable Entity owner) {
        if (!(target instanceof Mob mob) || !mob.isAlive() || mob.isRemoved() || ElementalGaugeManager.isEntityFrozen(mob)) {
            return;
        }
        if (isOwnerOrAlly(mob, owner)) {
            return;
        }
        this.tauntedMobs.add(mob.getUUID());
        redirectMobToConstruct(mob, owner);
    }

    private void clearOwnerAggro(Mob mob, @Nullable Entity owner) {
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && ((this.ownerUUID != null && currentTarget.getUUID().equals(this.ownerUUID))
                || (owner != null && (currentTarget == owner || currentTarget.isAlliedTo(owner))))) {
            mob.setTarget(null);
        }
        LivingEntity lastHurtBy = mob.getLastHurtByMob();
        if (lastHurtBy != null && ((this.ownerUUID != null && lastHurtBy.getUUID().equals(this.ownerUUID))
                || (owner != null && (lastHurtBy == owner || lastHurtBy.isAlliedTo(owner))))) {
            mob.setLastHurtByMob(null);
        }
        if (mob instanceof NeutralMob neutral && this.ownerUUID != null && this.ownerUUID.equals(neutral.getPersistentAngerTarget())) {
            neutral.stopBeingAngry();
        }
    }

    @Nullable
    private static InteractionHand findWeaponHand(Mob mob, Class<?> itemClass) {
        if (itemClass.isInstance(mob.getMainHandItem().getItem())) return InteractionHand.MAIN_HAND;
        if (itemClass.isInstance(mob.getOffhandItem().getItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    private enum TauntCombatStyle {
        BOW, CROSSBOW, TRIDENT_RANGED, BLAZE_FIREBALL, WITCH_POTION, SNOWBALL, MELEE
    }

    private static TauntCombatStyle classifyCombatStyle(Mob mob, double edgeDist) {
        if (findWeaponHand(mob, BowItem.class) != null) return TauntCombatStyle.BOW;
        if (findWeaponHand(mob, CrossbowItem.class) != null) return TauntCombatStyle.CROSSBOW;
        if (findWeaponHand(mob, TridentItem.class) != null && edgeDist > 2.2D) return TauntCombatStyle.TRIDENT_RANGED;
        if (mob instanceof Blaze) return TauntCombatStyle.BLAZE_FIREBALL;
        if (mob instanceof Witch) return TauntCombatStyle.WITCH_POTION;
        if (mob instanceof SnowGolem) return TauntCombatStyle.SNOWBALL;
        return TauntCombatStyle.MELEE;
    }

    private static double getPreferredRange(TauntCombatStyle style) {
        return switch (style) {
            case BLAZE_FIREBALL -> 12.0D;
            case BOW, CROSSBOW, TRIDENT_RANGED, SNOWBALL -> 10.0D;
            case WITCH_POTION -> 8.0D;
            case MELEE -> 0.0D;
        };
    }

    private void redirectMobToConstruct(Mob mob, @Nullable Entity owner) {
        clearOwnerAggro(mob, owner);
        mob.getLookControl().setLookAt(this.getX(), this.getY() + (isNetherite() ? 1.0D : 0.5D), this.getZ(), 30.0F, 30.0F);
        double edgeDist = horizontalEdgeDistance(mob);
        TauntCombatStyle style = classifyCombatStyle(mob, edgeDist);
        if (style != TauntCombatStyle.MELEE) {
            double preferredRange = getPreferredRange(style);
            if (edgeDist <= preferredRange && mob.hasLineOfSight(this)) {
                mob.getNavigation().stop();
                return;
            }
        } else {
            double meleeStop = Math.max(0.75D, mob.getBbWidth() * 0.62D);
            if (edgeDist <= meleeStop) {
                mob.getNavigation().stop();
                return;
            }
        }
        mob.getNavigation().moveTo(this.getX(), this.getY(), this.getZ(), 1.15D);
    }

    private void tickTauntedMobs(ServerLevel level) {
        if (this.tauntedMobs.isEmpty()) return;
        long now = level.getGameTime();
        Entity owner = getOwnerEntity();
        double tetherLimit = Math.max(13.0D, getWaveEndRadius() + 7.5D);
        Iterator<UUID> it = this.tauntedMobs.iterator();
        while (it.hasNext()) {
            UUID mobId = it.next();
            Entity e = level.getEntity(mobId);
            if (!(e instanceof Mob mob) || !mob.isAlive() || mob.isRemoved()) {
                it.remove();
                this.lastTauntAttackTime.remove(mobId);
                continue;
            }
            if (ElementalGaugeManager.isEntityFrozen(mob)) {
                mob.stopUsingItem();
                continue;
            }
            clearOwnerAggro(mob, owner);
            double edgeDist = horizontalEdgeDistance(mob);
            if (edgeDist > tetherLimit) {
                mob.stopUsingItem();
                it.remove();
                this.lastTauntAttackTime.remove(mobId);
                continue;
            }
            if ((now + mob.getId()) % 4L == 0L) {
                redirectMobToConstruct(mob, owner);
            }

            TauntCombatStyle style = classifyCombatStyle(mob, edgeDist);
            if (style != TauntCombatStyle.MELEE) {
                mob.getLookControl().setLookAt(this.getX(), this.getY() + (isNetherite() ? 1.0D : 0.5D), this.getZ(), 30.0F, 30.0F);
                handleTauntedRangedAttack(level, mob, mobId, style, edgeDist, now);
                continue;
            }

            mob.stopUsingItem();
            double dy = Math.abs(mob.getY() - this.getY());
            double meleeReach = Math.max(1.35D, mob.getBbWidth() * 1.05D);
            if (edgeDist <= meleeReach && dy <= 2.2D) {
                Long lastAtk = this.lastTauntAttackTime.get(mobId);
                boolean heavyMob = mob instanceof IronGolem || mob instanceof Ravager || mob instanceof Warden
                        || mob instanceof Hoglin || mob instanceof Zoglin;
                long atkCooldown = heavyMob ? 24L : 20L;
                if (lastAtk == null) {
                    // Brief windup upon reaching the construct so the attack animation plays cleanly
                    this.lastTauntAttackTime.put(mobId, now - (atkCooldown - 10L));
                    continue;
                }
                if (now < lastAtk || (now - lastAtk) >= atkCooldown) {
                    this.lastTauntAttackTime.put(mobId, now);
                    if (heavyMob) {
                        level.broadcastEntityEvent(mob, (byte) 4);
                        if (mob instanceof IronGolem) {
                            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                                    SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        }
                    } else {
                        mob.swing(InteractionHand.MAIN_HAND);
                    }
                    float atkDmg = 3.0F;
                    AttributeInstance inst = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (inst != null && inst.getValue() > 0.0D) {
                        atkDmg = (float) inst.getValue();
                    }
                    this.hurt(level.damageSources().mobAttack(mob), atkDmg);
                    if (!this.isAlive() || this.isRemoved()) {
                        return;
                    }
                }
            }
        }
    }

    private void handleTauntedRangedAttack(
            ServerLevel level,
            Mob mob,
            UUID mobId,
            TauntCombatStyle style,
            double edgeDist,
            long now
    ) {
        double maxRange = getPreferredRange(style);
        if (edgeDist > maxRange || !mob.hasLineOfSight(this)) {
            mob.stopUsingItem();
            return;
        }
        mob.getNavigation().stop();
        long interval = switch (style) {
            case BLAZE_FIREBALL, WITCH_POTION -> 45L;
            case SNOWBALL -> 25L;
            default -> 40L;
        };
        Long lastAtk = this.lastTauntAttackTime.get(mobId);
        if (lastAtk == null || now < lastAtk) {
            lastAtk = now - Math.max(0L, interval - 22L);
            this.lastTauntAttackTime.put(mobId, lastAtk);
        }
        long elapsed = now - lastAtk;
        if (elapsed < interval) {
            if ((style == TauntCombatStyle.BOW || style == TauntCombatStyle.CROSSBOW) && elapsed >= interval - 20L) {
                InteractionHand hand = findWeaponHand(mob, style == TauntCombatStyle.BOW ? BowItem.class : CrossbowItem.class);
                if (hand != null && !mob.isUsingItem()) {
                    mob.startUsingItem(hand);
                }
            }
            return;
        }

        this.lastTauntAttackTime.put(mobId, now);
        mob.stopUsingItem();

        double targetY = this.getY() + (isNetherite() ? 1.0D : 0.5D);
        double dx = this.getX() - mob.getX();
        double dy = targetY - mob.getEyeY();
        double dz = this.getZ() - mob.getZ();
        double horiz = Math.hypot(dx, dz);

        switch (style) {
            case BOW -> {
                InteractionHand hand = findWeaponHand(mob, BowItem.class);
                if (hand == null) return;
                ItemStack bow = mob.getItemInHand(hand);
                ItemStack ammo = mob.getProjectile(bow);
                if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);
                AbstractArrow arrow = ProjectileUtil.getMobArrow(mob, ammo, 1.0F);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                arrow.shoot(dx, dy + horiz * 0.2D, dz, 1.6F, 2.5F);
                level.addFreshEntity(arrow);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F / (level.random.nextFloat() * 0.4F + 0.8F));
            }
            case CROSSBOW -> {
                InteractionHand hand = findWeaponHand(mob, CrossbowItem.class);
                if (hand == null) return;
                ItemStack crossbow = mob.getItemInHand(hand);
                ItemStack ammo = mob.getProjectile(crossbow);
                if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);
                AbstractArrow bolt = ProjectileUtil.getMobArrow(mob, ammo, 1.0F);
                bolt.setCritArrow(true);
                bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
                bolt.shoot(dx, dy + horiz * 0.08D, dz, 1.8F, 1.5F);
                level.addFreshEntity(bolt);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            case TRIDENT_RANGED -> {
                InteractionHand hand = findWeaponHand(mob, TridentItem.class);
                if (hand == null) return;
                ThrownTrident trident = new ThrownTrident(level, mob, mob.getItemInHand(hand).copy());
                trident.pickup = AbstractArrow.Pickup.DISALLOWED;
                trident.shoot(dx, dy + horiz * 0.2D, dz, 1.6F, 2.0F);
                level.addFreshEntity(trident);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.DROWNED_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            case BLAZE_FIREBALL -> {
                double spread = Math.sqrt(Math.sqrt(horiz * horiz + dy * dy)) * 0.35D;
                Vec3 aim = new Vec3(
                        dx + mob.getRandom().triangle(0.0D, 0.25D * spread),
                        dy,
                        dz + mob.getRandom().triangle(0.0D, 0.25D * spread)
                );
                SmallFireball fireball = new SmallFireball(level, mob, aim.x, aim.y, aim.z);
                fireball.setPos(mob.getX(), mob.getY(0.5D) + 0.5D, mob.getZ());
                level.addFreshEntity(fireball);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            case WITCH_POTION -> {
                ThrownPotion potion = new ThrownPotion(level, mob);
                potion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.HARMING));
                potion.shoot(dx, dy + horiz * 0.2D, dz, 0.75F, 4.0F);
                level.addFreshEntity(potion);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            }
            case SNOWBALL -> {
                Snowball snowball = new Snowball(level, mob);
                snowball.shoot(dx, dy + horiz * 0.15D, dz, 1.4F, 4.0F);
                level.addFreshEntity(snowball);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            case MELEE -> {}
        }
    }

    private double horizontalEdgeDistance(Entity target) {
        AABB box = target.getBoundingBox();
        double cx = Mth.clamp(this.getX(), box.minX, box.maxX);
        double cz = Mth.clamp(this.getZ(), box.minZ, box.maxZ);
        return Math.hypot(cx - this.getX(), cz - this.getZ());
    }

    private double horizontalSquareEdgeDistance(Entity target) {
        AABB box = target.getBoundingBox();
        double cx = Mth.clamp(this.getX(), box.minX, box.maxX);
        double cz = Mth.clamp(this.getZ(), box.minZ, box.maxZ);
        return Math.max(Math.abs(cx - this.getX()), Math.abs(cz - this.getZ()));
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

        double centerH = isNetherite() ? 1.0D : 0.5D;
        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.getX(), this.getY() + centerH, this.getZ(),
                SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.9F, 0.85F + sl.random.nextFloat() * 0.25F);
        sl.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.15F),
                this.getX(), this.getY() + centerH, this.getZ(), 7, 0.35D, isNetherite() ? 0.65D : 0.35D, 0.35D, 0.02D);

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
        } else if (projectile instanceof ThrownTrident) {
            dmg = 8.0F;
        } else if (projectile instanceof AbstractArrow arrow) {
            dmg = (float) Math.max(2.0D, arrow.getDeltaMovement().length() * arrow.getBaseDamage());
        } else if (projectile instanceof ThrownPotion) {
            dmg = 3.0F;
        } else if (projectile instanceof Snowball) {
            dmg = 0.5F;
        } else if (projectile instanceof AbstractHurtingProjectile) {
            dmg = (projectile instanceof LargeFireball || projectile instanceof WitherSkull) ? 6.0F : 3.0F;
        } else if (projectile instanceof ShulkerBullet) {
            dmg = 4.0F;
        } else {
            dmg = 2.0F;
        }

        double centerH = isNetherite() ? 1.0D : 0.5D;
        level.playSound(null, this.getX(), this.getY() + centerH, this.getZ(),
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
        List<GeoFangPillarEntity> nearby = level.getEntitiesOfClass(
                GeoFangPillarEntity.class,
                victim.getBoundingBox().inflate(8.0D),
                c -> c.isAlive() && !c.isRemoved()
        );
        for (GeoFangPillarEntity construct : nearby) {
            AABB shieldBox = construct.getBoundingBox().inflate(0.2D, 0.15D, 0.2D);
            if (shieldBox.contains(origin)) continue;
            if (shieldBox.clip(origin, victimCenter).isPresent() || shieldBox.clip(origin, victimEyes).isPresent()) {
                if (construct.hurt(source, amount)) {
                    double centerH = construct.isNetherite() ? 1.0D : 0.5D;
                    level.playSound(null, construct.getX(), construct.getY() + centerH, construct.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.BLOCKS, 0.9F, 1.0F);
                    level.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.2F),
                            construct.getX(), construct.getY() + centerH, construct.getZ(),
                            10, 0.4D, construct.isNetherite() ? 0.6D : 0.35D, 0.4D, 0.03D);
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
            for (UUID mobId : this.tauntedMobs) {
                if (sl.getEntity(mobId) instanceof Mob mob) {
                    mob.stopUsingItem();
                }
            }
        }
        this.tauntedMobs.clear();
        this.lastTauntAttackTime.clear();
        if (this.level() instanceof ServerLevel sl) {
            double centerH = isNetherite() ? 1.0D : 0.5D;
            double spreadY = isNetherite() ? 0.8D : 0.45D;
            sl.playSound(null, this.getX(), this.getY() + centerH, this.getZ(),
                    SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.1F, 0.85F);
            sl.sendParticles(new DustParticleOptions(GEO_GOLD_COLOR, 1.5F),
                    this.getX(), this.getY() + centerH, this.getZ(), 24, 0.45D, spreadY, 0.45D, 0.06D);
            sl.sendParticles(ParticleTypes.WAX_ON,
                    this.getX(), this.getY() + centerH, this.getZ(), 10, 0.4D, spreadY, 0.4D, 0.04D);
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("IsNetherite")) {
            setNetherite(tag.getBoolean("IsNetherite"));
        }
        if (tag.contains("ConstructMaxHealth")) {
            this.entityData.set(DATA_MAX_HEALTH, Math.max(2.0F, tag.getFloat("ConstructMaxHealth")));
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
        tag.putBoolean("IsNetherite", isNetherite());
        tag.putFloat("ConstructMaxHealth", getMaxConstructHealth());
        tag.putFloat("ConstructHealth", getHealth());
        tag.putInt("Age", this.ageTicks);
        tag.putLong("CreatedGameTime", this.createdGameTime);
    }
}
