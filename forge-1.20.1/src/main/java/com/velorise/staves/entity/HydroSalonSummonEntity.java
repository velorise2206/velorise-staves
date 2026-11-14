package com.velorise.staves.entity;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.ElementalResonanceManager;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Furina's Hydro Summons for Splendor of Tranquil Waters:
 * - Role 0 (Ousia): Gentilhomme Usher (Mid-range AoE bubble cannon every 35 ticks)
 * - Role 1 (Ousia): Surintendante Chevalmarin (Rapid single-target bubble jet every 20 ticks)
 * - Role 2 (Ousia): Mademoiselle Crabaletta (Heavy leaping pincer slam every 50 ticks)
 * - Role 3 (Pneuma): Singer of Many Waters (Stationary healing wave totem pulsing every 40 ticks)
 */
public class HydroSalonSummonEntity extends Entity {
    public static final int ROLE_USHER = 0;
    public static final int ROLE_CHEVALMARIN = 1;
    public static final int ROLE_CRABALETTA = 2;
    public static final int ROLE_SINGER = 3;

    public static final int MAX_LIFETIME_TICKS = 300; // 15 seconds

    private static final EntityDataAccessor<Byte> DATA_ROLE =
            SynchedEntityData.defineId(HydroSalonSummonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_NETHERITE =
            SynchedEntityData.defineId(HydroSalonSummonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LAST_ACTION_TICK =
            SynchedEntityData.defineId(HydroSalonSummonEntity.class, EntityDataSerializers.INT);

    private static final Map<UUID, Long> ACTIVE_OWNER_SUMMON_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ACTIVE_OWNER_NETHERITE = new ConcurrentHashMap<>();

    private @Nullable UUID ownerUuid;
    private int ageTicks;
    private Vec3 anchorPos = Vec3.ZERO;
    private Vec3 wanderPos = Vec3.ZERO;
    private int nextWanderTick = 0;
    private double clientPrevX;
    private double clientPrevY;
    private double clientPrevZ;
    private boolean clientPosInit;
    private float clientMoveSpeed;
    private float clientTentaclePhase;
    private float clientPrevTentaclePhase;

    public float getTentaclePhase(float partialTicks) {
        return Mth.lerp(partialTicks, this.clientPrevTentaclePhase, this.clientTentaclePhase);
    }

    public HydroSalonSummonEntity(EntityType<? extends HydroSalonSummonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public HydroSalonSummonEntity(
            EntityType<? extends HydroSalonSummonEntity> type,
            Level level,
            ServerPlayer owner,
            int role,
            boolean isNetherite,
            Vec3 spawnPos
    ) {
        this(type, level);
        this.ownerUuid = owner.getUUID();
        this.setRole(role);
        this.setNetherite(isNetherite);
        this.anchorPos = spawnPos;
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.setYRot(owner.getYRot());
        ACTIVE_OWNER_SUMMON_UNTIL.put(owner.getUUID(), level.getGameTime() + 15L);
        ACTIVE_OWNER_NETHERITE.put(owner.getUUID(), isNetherite);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ROLE, (byte) 0);
        this.entityData.define(DATA_NETHERITE, false);
        this.entityData.define(DATA_LAST_ACTION_TICK, -100);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public int getRole() {
        return Mth.clamp(this.entityData.get(DATA_ROLE), 0, 3);
    }

    public void setRole(int role) {
        this.entityData.set(DATA_ROLE, (byte) Mth.clamp(role, 0, 3));
    }

    public boolean isNetherite() {
        return this.entityData.get(DATA_NETHERITE);
    }

    public void setNetherite(boolean netherite) {
        this.entityData.set(DATA_NETHERITE, netherite);
    }

    public int getLastActionTick() {
        return this.entityData.get(DATA_LAST_ACTION_TICK);
    }

    public void setLastActionTick(int tick) {
        this.entityData.set(DATA_LAST_ACTION_TICK, tick);
    }

    public @Nullable UUID getOwnerUUID() {
        return ownerUuid;
    }

    public static boolean hasActiveSummon(@Nullable Player player) {
        if (player == null) return false;
        if (!carriesSplendorStaff(player)) return false;
        Long until = ACTIVE_OWNER_SUMMON_UNTIL.get(player.getUUID());
        if (until != null && player.level().getGameTime() <= until) {
            return true;
        }
        AABB box = player.getBoundingBox().inflate(64.0D);
        List<HydroSalonSummonEntity> list = player.level().getEntitiesOfClass(
                HydroSalonSummonEntity.class, box, Entity::isAlive);
        if (!list.isEmpty()) {
            ACTIVE_OWNER_SUMMON_UNTIL.put(player.getUUID(), player.level().getGameTime() + 10L);
            return true;
        }
        return false;
    }

    public static boolean hasNetheriteSummon(@Nullable Player player) {
        if (player == null) return false;
        return Boolean.TRUE.equals(ACTIVE_OWNER_NETHERITE.get(player.getUUID()));
    }

    public static boolean carriesSplendorStaff(@Nullable Player player) {
        if (player == null) return false;
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && s.getItem() instanceof ArcaneStaffItem staff
                    && staff.kind() == ArcaneStaffItem.Kind.SPLENDOR) {
                return true;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (!s.isEmpty() && s.getItem() instanceof ArcaneStaffItem staff
                    && staff.kind() == ArcaneStaffItem.Kind.SPLENDOR) {
                return true;
            }
        }
        return false;
    }

    public static void clearOwnerSummons(ServerLevel level, UUID ownerId) {
        for (ServerLevel lvl : level.getServer().getAllLevels()) {
            for (Entity e : lvl.getAllEntities()) {
                if (e instanceof HydroSalonSummonEntity summon
                        && ownerId.equals(summon.ownerUuid)) {
                    summon.discard();
                }
            }
        }
        ACTIVE_OWNER_SUMMON_UNTIL.remove(ownerId);
        ACTIVE_OWNER_NETHERITE.remove(ownerId);
    }

    public static boolean hasAnySummonForOwner(ServerLevel level, UUID ownerId) {
        for (ServerLevel lvl : level.getServer().getAllLevels()) {
            for (Entity e : lvl.getAllEntities()) {
                if (e instanceof HydroSalonSummonEntity summon
                        && summon.isAlive()
                        && ownerId.equals(summon.ownerUuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void switchArkheInPlace(
            ServerLevel level,
            ServerPlayer owner,
            boolean isNetherite,
            boolean pneumaMode
    ) {
        UUID ownerId = owner.getUUID();
        Vec3 sumPos = Vec3.ZERO;
        int count = 0;
        int preservedAge = 0;
        ServerLevel targetLevel = level;
        for (ServerLevel lvl : level.getServer().getAllLevels()) {
            for (Entity e : lvl.getAllEntities()) {
                if (e instanceof HydroSalonSummonEntity summon
                        && summon.isAlive()
                        && ownerId.equals(summon.ownerUuid)) {
                    sumPos = sumPos.add(summon.position());
                    preservedAge = Math.max(preservedAge, summon.ageTicks);
                    targetLevel = lvl;
                    count++;
                }
            }
        }
        if (count == 0) return;
        Vec3 anchor = sumPos.scale(1.0D / count);
        clearOwnerSummons(level, ownerId);

        EntityType<HydroSalonSummonEntity> type = VeloriseStavesMod.HYDRO_SALON_SUMMON.get();
        if (pneumaMode) {
            HydroSalonSummonEntity singer = new HydroSalonSummonEntity(
                    type, targetLevel, owner, ROLE_SINGER, isNetherite, anchor);
            singer.ageTicks = preservedAge;
            targetLevel.addFreshEntity(singer);
            targetLevel.sendParticles(ParticleTypes.END_ROD, anchor.x, anchor.y + 0.6D, anchor.z, 18, 0.35D, 0.45D, 0.35D, 0.04D);
            targetLevel.sendParticles(ParticleTypes.SPLASH, anchor.x, anchor.y + 0.3D, anchor.z, 24, 0.45D, 0.25D, 0.45D, 0.06D);
        } else {
            for (int role = 0; role < 3; role++) {
                double angleRad = Math.toRadians(owner.getYRot() + role * 120.0D);
                Vec3 offset = new Vec3(Math.cos(angleRad) * 1.6D, 0.35D + role * 0.12D, Math.sin(angleRad) * 1.6D);
                Vec3 spawnPos = anchor.add(offset);
                HydroSalonSummonEntity member = new HydroSalonSummonEntity(
                        type, targetLevel, owner, role, isNetherite, spawnPos);
                member.ageTicks = preservedAge;
                targetLevel.addFreshEntity(member);
                targetLevel.sendParticles(ParticleTypes.BUBBLE_POP, spawnPos.x, spawnPos.y + 0.3D, spawnPos.z, 14, 0.25D, 0.25D, 0.25D, 0.04D);
            }
        }
    }

    public static void summonOrSwitchArkhe(
            ServerLevel level,
            ServerPlayer owner,
            boolean isNetherite,
            boolean pneumaMode
    ) {
        clearOwnerSummons(level, owner.getUUID());
        EntityType<HydroSalonSummonEntity> type = VeloriseStavesMod.HYDRO_SALON_SUMMON.get();
        if (pneumaMode) {
            Vec3 look = owner.getLookAngle();
            Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
            if (horiz.lengthSqr() < 1.0E-4D) horiz = new Vec3(0.0D, 0.0D, 1.0D);
            else horiz = horiz.normalize();
            Vec3 spawnPos = owner.position().add(horiz.scale(2.2D)).add(0.0D, 0.5D, 0.0D);
            HydroSalonSummonEntity singer = new HydroSalonSummonEntity(
                    type, level, owner, ROLE_SINGER, isNetherite, spawnPos);
            level.addFreshEntity(singer);
            level.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y + 0.6D, spawnPos.z, 18, 0.35D, 0.45D, 0.35D, 0.04D);
            level.sendParticles(ParticleTypes.SPLASH, spawnPos.x, spawnPos.y + 0.3D, spawnPos.z, 24, 0.45D, 0.25D, 0.45D, 0.06D);
            level.playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.25F);
        } else {
            for (int role = 0; role < 3; role++) {
                double angleRad = Math.toRadians(owner.getYRot() + role * 120.0D);
                Vec3 offset = new Vec3(Math.cos(angleRad) * 2.1D, 1.05D + role * 0.15D, Math.sin(angleRad) * 2.1D);
                Vec3 spawnPos = owner.position().add(offset);
                HydroSalonSummonEntity member = new HydroSalonSummonEntity(
                        type, level, owner, role, isNetherite, spawnPos);
                level.addFreshEntity(member);
                level.sendParticles(ParticleTypes.BUBBLE_POP, spawnPos.x, spawnPos.y + 0.3D, spawnPos.z, 14, 0.25D, 0.25D, 0.25D, 0.04D);
            }
            level.playSound(null, owner.blockPosition(), SoundEvents.DOLPHIN_PLAY, SoundSource.PLAYERS, 0.9F, 1.15F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.ageTicks++;

        if (this.level().isClientSide()) {
            if (!this.clientPosInit) {
                this.clientPrevX = this.getX();
                this.clientPrevY = this.getY();
                this.clientPrevZ = this.getZ();
                this.clientPosInit = true;
            }
            double dx = this.getX() - this.clientPrevX;
            double dy = this.getY() - this.clientPrevY;
            double dz = this.getZ() - this.clientPrevZ;
            this.clientPrevX = this.getX();
            this.clientPrevY = this.getY();
            this.clientPrevZ = this.getZ();

            float instantSpeed = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            this.clientMoveSpeed = Mth.lerp(0.35F, this.clientMoveSpeed, Math.min(instantSpeed, 0.5F));
            this.clientPrevTentaclePhase = this.clientTentaclePhase;
            this.clientTentaclePhase += this.clientMoveSpeed * 3.2F;
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (this.ageTicks > MAX_LIFETIME_TICKS || this.ownerUuid == null) {
            this.discard();
            return;
        }

        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUuid);
        if (owner == null || !owner.isAlive() || owner.level() != serverLevel || !carriesSplendorStaff(owner)) {
            this.discard();
            return;
        }

        ACTIVE_OWNER_SUMMON_UNTIL.put(owner.getUUID(), serverLevel.getGameTime() + 15L);
        ACTIVE_OWNER_NETHERITE.put(owner.getUUID(), this.isNetherite());

        int role = getRole();
        if (role == ROLE_SINGER) {
            tickSingerOfManyWaters(serverLevel, owner);
        } else {
            tickSalonMember(serverLevel, owner, role);
        }
    }

    private void tickSingerOfManyWaters(ServerLevel level, ServerPlayer owner) {
        if (this.anchorPos.equals(Vec3.ZERO)) {
            this.anchorPos = this.position();
        }
        double bob = Math.sin((this.ageTicks + this.getId() * 7) * 0.08D) * 0.14D;
        this.setPos(this.anchorPos.x, this.anchorPos.y + bob, this.anchorPos.z);
        this.setYRot((this.getYRot() + 2.5F) % 360.0F);

        // Strictly heal once every 2.0 seconds (40 ticks) per Genshin Singer of Many Waters interval
        if (this.ageTicks >= 40 && this.ageTicks % 40 == 0) {
            this.setLastActionTick(this.tickCount);
            double radius = isNetherite() ? 8.0D : 6.0D;
            float ownerMaxHp = Math.max(20.0F, owner.getMaxHealth());
            float baseHeal = isNetherite() ? Math.max(3.0F, ownerMaxHp * 0.08F + 1.4F) : 2.0F;

            AABB healBox = this.getBoundingBox().inflate(radius, 4.0D, radius);
            List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, healBox, e ->
                    e.isAlive() && !e.isSpectator() && isAlliedWithOwner(owner, e));

            for (LivingEntity ally : allies) {
                float before = ally.getHealth();
                if (isNetherite()) {
                    ally.heal(baseHeal);
                } else {
                    // Basic tier: strictly heals 1 heart (2.0 HP) per 2s pulse
                    ally.setHealth(Math.min(ally.getMaxHealth(), before + 2.0F));
                }
                if (ally.getRemainingFireTicks() > 0 && !ally.isInLava()) {
                    ally.clearFire();
                }
                float gained = Math.max(0.0F, ally.getHealth() - before);
                if (gained > 0.01F) {
                    ElementalGaugeManager.recordFurinaHpFluctuation(level, owner, gained / Math.max(1.0F, ally.getMaxHealth()));
                }
                level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + ally.getBbHeight() + 0.25D, ally.getZ(),
                        2, 0.25D, 0.15D, 0.25D, 0.02D);
            }

            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.85F, 1.35F);
        }
    }

    private static double resolveNaturalGroundY(ServerLevel level, double x, double refY, double z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                Mth.floor(x), Mth.floor(refY) + 3, Mth.floor(z));
        for (int i = 0; i < 8; i++) {
            BlockPos below = cursor.below();
            boolean solidOrLiquidBelow = !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                    || !level.getFluidState(below).isEmpty();
            boolean freeAtCursor = level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()
                    && level.getFluidState(cursor).isEmpty();
            if (solidOrLiquidBelow && freeAtCursor) {
                return below.getY() + 1.0D;
            }
            cursor.move(Direction.DOWN);
        }
        return refY;
    }

    private void tickSalonMember(ServerLevel level, ServerPlayer owner, int role) {
        LivingEntity target = findBestTarget(level, owner, 16.0D);

        int interval = switch (role) {
            case ROLE_CHEVALMARIN -> 32; // ~1.6s bubble stream
            case ROLE_CRABALETTA -> 72;  // ~3.6s heavy crab slam
            default -> 48;               // ~2.4s AoE bubble cannon
        };

        int phase = this.ageTicks + role * 13;
        boolean attackingNow = target != null && this.ageTicks >= 12 && (phase % interval == 0);

        double hoverOffset = switch (role) {
            case ROLE_CRABALETTA -> 0.52D;
            case ROLE_USHER -> 0.82D;
            default -> 1.08D;
        };

        // Re-pick a natural world position periodically or when far from owner/target
        boolean needNewSpot = this.wanderPos.equals(Vec3.ZERO) || this.ageTicks >= this.nextWanderTick;
        if (target != null) {
            double dTargetSqr = this.wanderPos.distanceToSqr(target.position());
            if (dTargetSqr > 49.0D || dTargetSqr < 1.2D) {
                needNewSpot = true;
            }
        } else if (this.wanderPos.distanceToSqr(owner.position()) > 36.0D) {
            needNewSpot = true;
        }

        if (this.position().distanceToSqr(owner.position()) > 484.0D) {
            // Teleport near owner if owner moved > 22 blocks away
            double baseAng = Math.toRadians(owner.getYRot() + 110.0D + role * 70.0D);
            double tx = owner.getX() + Math.cos(baseAng) * 2.4D;
            double tz = owner.getZ() + Math.sin(baseAng) * 2.4D;
            double ty = resolveNaturalGroundY(level, tx, owner.getY(), tz) + hoverOffset;
            this.wanderPos = new Vec3(tx, ty, tz);
            this.setPos(tx, ty, tz);
            this.nextWanderTick = this.ageTicks + 40 + this.random.nextInt(35);
            needNewSpot = false;
        }

        if (needNewSpot) {
            if (target != null) {
                double standOffDist = switch (role) {
                    case ROLE_CRABALETTA -> 2.1D + this.random.nextDouble() * 1.1D;
                    case ROLE_USHER -> 3.4D + this.random.nextDouble() * 1.6D;
                    default -> 4.2D + this.random.nextDouble() * 1.8D;
                };
                Vec3 fromTargetToOwner = owner.position().subtract(target.position());
                double baseYaw = fromTargetToOwner.lengthSqr() > 1.0E-4D
                        ? Math.atan2(fromTargetToOwner.z, fromTargetToOwner.x)
                        : (this.random.nextDouble() * Math.PI * 2.0D);
                double roleSpread = (role - 1) * 0.95D + (this.random.nextDouble() - 0.5D) * 0.75D;
                double ang = baseYaw + roleSpread;
                double wx = target.getX() + Math.cos(ang) * standOffDist;
                double wz = target.getZ() + Math.sin(ang) * standOffDist;
                double wy = resolveNaturalGroundY(level, wx, target.getY(), wz) + hoverOffset;
                this.wanderPos = new Vec3(wx, wy, wz);
                this.nextWanderTick = this.ageTicks + 35 + this.random.nextInt(35);
            } else {
                double dist = 2.0D + this.random.nextDouble() * 2.8D;
                double baseAngle = Math.toRadians(owner.getYRot() + 90.0D) + (role - 1) * 1.15D + (this.random.nextDouble() - 0.5D) * 1.1D;
                double wx = owner.getX() + Math.cos(baseAngle) * dist;
                double wz = owner.getZ() + Math.sin(baseAngle) * dist;
                double wy = resolveNaturalGroundY(level, wx, owner.getY(), wz) + hoverOffset;
                this.wanderPos = new Vec3(wx, wy, wz);
                this.nextWanderTick = this.ageTicks + 45 + this.random.nextInt(45);
            }
        }

        double bob = Math.sin((this.ageTicks + role * 19) * 0.10D) * 0.12D;
        Vec3 desiredPos = new Vec3(this.wanderPos.x, this.wanderPos.y + bob, this.wanderPos.z);

        if (target != null && role == ROLE_CRABALETTA && (phase % interval >= interval - 10)) {
            // Mademoiselle Crabaletta lunges toward the target right as she slams!
            Vec3 toSelf = this.position().subtract(target.position());
            Vec3 standOff = new Vec3(toSelf.x, 0.0D, toSelf.z);
            standOff = standOff.lengthSqr() > 1.0E-4D ? standOff.normalize().scale(1.05D) : new Vec3(0.85D, 0.0D, 0.0D);
            desiredPos = target.position().add(standOff).add(0.0D, Math.max(0.35D, target.getBbHeight() * 0.45D), 0.0D);
        }

        Vec3 delta = desiredPos.subtract(this.position());
        double horizLen = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        boolean crabLunging = target != null && role == ROLE_CRABALETTA && (phase % interval >= interval - 10);
        double maxHorizSpeed = crabLunging ? 0.52D : 0.16D;
        double moveX = delta.x * (crabLunging ? 0.36D : 0.14D);
        double moveZ = delta.z * (crabLunging ? 0.36D : 0.14D);
        double stepHoriz = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (stepHoriz > maxHorizSpeed && stepHoriz > 1.0E-5D) {
            moveX = (moveX / stepHoriz) * maxHorizSpeed;
            moveZ = (moveZ / stepHoriz) * maxHorizSpeed;
        }
        double moveY = Mth.clamp(delta.y * 0.20D, -0.22D, 0.22D);

        this.setPos(
                this.getX() + moveX,
                this.getY() + moveY,
                this.getZ() + moveZ
        );

        if (target != null) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            this.setYRot(Mth.rotLerp(0.30F, this.getYRot(), targetYaw));
        } else if (horizLen > 0.22D) {
            float moveYaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
            this.setYRot(Mth.rotLerp(0.22F, this.getYRot(), moveYaw));
        } else {
            double dx = owner.getX() - this.getX();
            double dz = owner.getZ() - this.getZ();
            if (dx * dx + dz * dz > 0.25D) {
                float lookOwnerYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(Mth.rotLerp(0.12F, this.getYRot(), lookOwnerYaw));
            }
        }

        if (attackingNow) {
            this.setLastActionTick(this.tickCount);
            performOusiaAttack(level, owner, target, role);
        }
    }

    private void performOusiaAttack(ServerLevel level, ServerPlayer owner, LivingEntity primaryTarget, int role) {
        // 1. Drain 2% Max HP from up to 4 allied entities (including owner) currently above 50% Max HP
        AABB allyBox = owner.getBoundingBox().inflate(16.0D);
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, allyBox, e ->
                e.isAlive() && !e.isSpectator() && isAlliedWithOwner(owner, e) && e.getHealth() > e.getMaxHealth() * 0.50F);

        int drainedAllies = 0;
        for (LivingEntity ally : allies) {
            float maxHp = Math.max(1.0F, ally.getMaxHealth());
            float floorHp = maxHp * 0.50F;
            float curHp = ally.getHealth();
            if (curHp > floorHp + 0.01F) {
                float drain = Math.min(curHp - floorHp, maxHp * 0.02F);
                ElementalGaugeManager.drainHealthSilently(ally, Math.max(floorHp, curHp - drain));
                ElementalGaugeManager.recordFurinaHpFluctuation(level, owner, drain / maxHp);
                drainedAllies++;
                if (drainedAllies >= 4) break;
            }
        }

        // Up to +40% (1.40x) Salon Solitaire damage multiplier when 4 allies/owner offer HP > 50%
        float drainDamageMult = 1.0F + 0.10F * Math.min(4, drainedAllies);
        float ownerMaxHp = Math.max(20.0F, owner.getMaxHealth());
        float hpRatio = switch (role) {
            case ROLE_CHEVALMARIN -> isNetherite() ? 0.075F : 0.05F;
            case ROLE_CRABALETTA -> isNetherite() ? 0.21F : 0.14F;
            default -> isNetherite() ? 0.12F : 0.08F;
        };
        float baseDamage = ownerMaxHp * hpRatio * drainDamageMult;
        float gaugeU = isNetherite() ? 1.5F : 1.0F;



        if (role == ROLE_CHEVALMARIN) {
            ElementalGaugeManager.applyElement(level, primaryTarget, Element.HYDRO, gaugeU, owner, baseDamage);
            level.playSound(null, this.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 0.85F, 1.4F);
        } else {
            double aoeRadius = (role == ROLE_CRABALETTA) ? 2.0D : 2.5D;
            AABB hitBox = primaryTarget.getBoundingBox().inflate(aoeRadius);
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, hitBox, e ->
                    e.isAlive() && !e.isSpectator() && !isAlliedWithOwner(owner, e));
            if (!hits.contains(primaryTarget)) {
                ElementalGaugeManager.applyElement(level, primaryTarget, Element.HYDRO, gaugeU, owner, baseDamage);
            }
            for (LivingEntity hit : hits) {
                ElementalGaugeManager.applyElement(level, hit, Element.HYDRO, gaugeU, owner, baseDamage);
            }

            level.playSound(null, primaryTarget.blockPosition(),
                    role == ROLE_CRABALETTA ? SoundEvents.PLAYER_SPLASH_HIGH_SPEED : SoundEvents.GENERIC_SPLASH,
                    SoundSource.PLAYERS, 0.85F, role == ROLE_CRABALETTA ? 0.95F : 1.25F);
        }
    }

    private static boolean isAlliedWithOwner(ServerPlayer owner, LivingEntity candidate) {
        if (candidate == owner) return true;
        if (candidate instanceof Player otherPlayer) {
            return owner.isAlliedTo(otherPlayer) || !owner.canHarmPlayer(otherPlayer);
        }
        if (candidate instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) {
            return true;
        }
        return owner.isAlliedTo(candidate);
    }

    private @Nullable LivingEntity findBestTarget(ServerLevel level, ServerPlayer owner, double radius) {
        LivingEntity lastHurt = owner.getLastHurtMob();
        if (lastHurt != null && lastHurt.isAlive() && !lastHurt.isSpectator()
                && !isAlliedWithOwner(owner, lastHurt)
                && lastHurt.distanceToSqr(owner) <= radius * radius) {
            return lastHurt;
        }
        LivingEntity lastBy = owner.getLastHurtByMob();
        if (lastBy != null && lastBy.isAlive() && !lastBy.isSpectator()
                && !isAlliedWithOwner(owner, lastBy)
                && lastBy.distanceToSqr(owner) <= radius * radius) {
            return lastBy;
        }
        AABB searchBox = owner.getBoundingBox().inflate(radius);
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, searchBox, e ->
                e.isAlive() && !e.isSpectator() && e instanceof Enemy && !isAlliedWithOwner(owner, e));
        return hostiles.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(owner)))
                .orElse(null);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.ageTicks = tag.getInt("AgeTicks");
        this.setRole(tag.getByte("Role"));
        this.setNetherite(tag.getBoolean("Netherite"));
        if (tag.contains("AnchorX")) {
            this.anchorPos = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("AgeTicks", this.ageTicks);
        tag.putByte("Role", (byte) getRole());
        tag.putBoolean("Netherite", isNetherite());
        tag.putDouble("AnchorX", this.anchorPos.x);
        tag.putDouble("AnchorY", this.anchorPos.y);
        tag.putDouble("AnchorZ", this.anchorPos.z);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
