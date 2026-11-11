package com.velorise.staves.entity;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.DendroCoreManager;
import com.velorise.staves.element.ElectrifiedWaterManager;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.element.EnvironmentalBlockManager;
import com.velorise.staves.event.ArcaneStaffEvents;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 3D spinning crystalline prism orb launched by Omni Staff.
 * In OMNI mode (0), automatically resolves the strongest elemental trigger on impact;
 * in modes 1-7, strikes directly with the locked Element.
 */
public class OmniOrbEntity extends ThrowableProjectile {
    public static final Element[] WHEEL_ELEMENTS = {
            null,            // 0: OMNI (Auto Best)
            Element.PYRO,    // 1
            Element.HYDRO,   // 2
            Element.ANEMO,   // 3
            Element.ELECTRO, // 4
            Element.DENDRO,  // 5
            Element.CRYO,    // 6
            Element.GEO      // 7
    };

    private static final EntityDataAccessor<Byte> DATA_MODE =
            SynchedEntityData.defineId(OmniOrbEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_NETHERITE =
            SynchedEntityData.defineId(OmniOrbEntity.class, EntityDataSerializers.BOOLEAN);

    private Vec3 origin;
    private double maxRange = 36.0D;
    private float baseDamage = 10.0F;
    private float attackU = 2.0F;
    private final Set<UUID> zappedEntities = new HashSet<>();
    private @Nullable Vec3 lastWaterElectrifyPos;

    public OmniOrbEntity(EntityType<? extends OmniOrbEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public OmniOrbEntity(EntityType<? extends OmniOrbEntity> type, Level level, LivingEntity owner, boolean isNetherite, int omniMode) {
        this(type, level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.origin = owner.position();
        this.maxRange = isNetherite ? 48.0D : 36.0D;
        this.baseDamage = isNetherite ? 15.0F : 10.0F;
        this.attackU = isNetherite ? 3.0F : 2.0F;
        this.entityData.set(DATA_MODE, (byte) clampMode(omniMode));
        this.entityData.set(DATA_NETHERITE, isNetherite);
    }

    public static void clearCycleCache() {
    }

    public static void removeCycleEntry(UUID id) {
    }

    public static int clampMode(int mode) {
        return (mode >= 0 && mode < WHEEL_ELEMENTS.length) ? mode : 0;
    }

    public static @Nullable Element modeToElement(int mode) {
        return WHEEL_ELEMENTS[clampMode(mode)];
    }

    public static int getElementRgb(Element element) {
        return switch (element) {
            case PYRO -> 0xFF5522;
            case HYDRO -> 0x18BFFF;
            case ANEMO -> 0x45E5B8;
            case ELECTRO -> 0xC44DFF;
            case DENDRO -> 0x5CE036;
            case CRYO -> 0x7CF4FF;
            case GEO -> 0xFFCE3B;
        };
    }

    public static int getIridescentRgb(float phaseTicks) {
        float hue = ((phaseTicks * 0.045F) % 1.0F + 1.0F) % 1.0F;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.72F, 1.0F);
        return rgb & 0x00FFFFFF;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, (byte) 0);
        builder.define(DATA_NETHERITE, false);
    }

    public int getOmniMode() {
        return clampMode(this.entityData.get(DATA_MODE));
    }

    public boolean isNetheriteTier() {
        return this.entityData.get(DATA_NETHERITE);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && entity != this.getOwner()
                && !(entity instanceof GeoFangPillarEntity)
                && !(entity instanceof DendroCoreEntity);
    }

    @Override
    public void tick() {
        Vec3 prevPos = this.position();
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.isRemoved() || !(this.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (this.origin == null) {
                this.origin = prevPos;
            }

            Element fixedElem = modeToElement(getOmniMode());
            if (fixedElem == Element.CRYO) {
                BlockHitResult fluidHit = serverLevel.clip(new ClipContext(
                        prevPos,
                        this.position(),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.ANY,
                        this
                ));
                if (fluidHit.getType() == HitResult.Type.BLOCK && serverLevel.getFluidState(fluidHit.getBlockPos()).is(FluidTags.WATER)) {
                    detonate(serverLevel, null, fluidHit.getLocation(), false);
                    return;
                }
                if (this.isInWater() || serverLevel.getFluidState(this.blockPosition()).is(FluidTags.WATER)) {
                    detonate(serverLevel, null, this.position(), false);
                    return;
                }
            } else if (fixedElem == Element.ELECTRO) {
                tickBallLightningPath(serverLevel, prevPos, this.position());
            }

            if (this.position().distanceToSqr(this.origin) >= this.maxRange * this.maxRange || this.tickCount > 110) {
                detonate(serverLevel, null, this.position(), true);
            }
            return;
        }

        if (StavesConfig.SHOW_ELEMENTAL_PARTICLES.get()) {
            spawnClientTrail();
        }
    }

    private void spawnClientTrail() {
        int mode = getOmniMode();
        Element fixed = modeToElement(mode);
        int rgb = fixed != null ? getElementRgb(fixed) : getIridescentRgb(this.tickCount + this.getId() * 5.0F);
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;

        this.level().addParticle(
                new DustParticleOptions(new Vector3f(r, g, b), isNetheriteTier() ? 1.25F : 1.05F),
                this.getX(), this.getY(), this.getZ(),
                0.0D, 0.0D, 0.0D
        );
        if (fixed == Element.ELECTRO) {
            double rx = (this.random.nextDouble() - 0.5D) * 1.5D;
            double ry = (this.random.nextDouble() - 0.5D) * 1.5D;
            double rz = (this.random.nextDouble() - 0.5D) * 1.5D;
            this.level().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + rx, this.getY() + ry, this.getZ() + rz,
                    0.0D, 0.0D, 0.0D
            );
        }
        if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private void tickBallLightningPath(ServerLevel level, Vec3 from, Vec3 to) {
        boolean neth = isNetheriteTier();
        ServerPlayer attacker = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        double auraRadius = 1.0D;
        double auraRadiusSq = auraRadius * auraRadius;
        float zapU = neth ? 1.5F : 1.0F;
        float zapDmg = this.baseDamage * 0.35F;

        // 1. Zap nearby LivingEntities within 1 block of the flight segment
        AABB swept = new AABB(from, to).inflate(auraRadius + 0.5D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, swept,
                e -> e.isAlive() && e != this.getOwner() && !e.isSpectator()
                        && !this.zappedEntities.contains(e.getUUID()))) {
            AABB bb = target.getBoundingBox();
            Vec3 center = bb.getCenter();
            Vec3 segPt = closestPointOnSegment(from, to, center);
            double cx = Mth.clamp(segPt.x, bb.minX, bb.maxX);
            double cy = Mth.clamp(segPt.y, bb.minY, bb.maxY);
            double cz = Mth.clamp(segPt.z, bb.minZ, bb.maxZ);
            Vec3 hitPt = new Vec3(cx, cy, cz);
            Vec3 orbPt = closestPointOnSegment(from, to, hitPt);
            if (orbPt.distanceToSqr(hitPt) <= auraRadiusSq) {
                this.zappedEntities.add(target.getUUID());
                Vec3 arcEnd = hitPt.lerp(center, 0.45D);
                ElectrifiedWaterManager.drawElectricArc(level, orbPt, arcEnd, 0.18D, true);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, arcEnd.x, arcEnd.y, arcEnd.z, 6, 0.2D, 0.2D, 0.2D, 0.04D);
                level.playSound(null, orbPt.x, orbPt.y, orbPt.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.25F, 1.9F);
                ElementalGaugeManager.applyElement(level, target, Element.ELECTRO, zapU, attacker, zapDmg);
            }
        }

        // 2. Electrify water, Tesla Coils & Dendro Cores within 1 block along the path
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        int steps = Math.max(1, (int) Math.ceil(len / 0.85D));
        for (int s = 0; s <= steps; s++) {
            double t = len < 1.0E-4D ? 1.0D : (double) s / (double) steps;
            Vec3 pt = from.add(delta.scale(t));
            DendroCoreManager.checkReactionsNear(level, pt, 1.35D, Element.ELECTRO, attacker);
            EnvironmentalBlockManager.activateTeslaCoilsNear(level, pt, 1.35D);

            if (this.lastWaterElectrifyPos == null || pt.distanceToSqr(this.lastWaterElectrifyPos) >= 2.25D) {
                BlockPos centerBlock = BlockPos.containing(pt);
                for (BlockPos p : BlockPos.betweenClosed(centerBlock.offset(-1, -1, -1), centerBlock.offset(1, 1, 1))) {
                    if (level.hasChunkAt(p) && level.getFluidState(p).is(FluidTags.WATER) && p.distToCenterSqr(pt) <= 1.5625D) {
                        this.lastWaterElectrifyPos = pt;
                        ElectrifiedWaterManager.electrify(level, p.immutable(), neth ? 1.5D : 1.0D, 70, attacker, neth ? 1.5F : 1.0F);
                        break;
                    }
                }
            }
        }

        // 3. Ambient crackling ball-lightning arcs radiating ~1 block around the orb
        if (this.tickCount % 2 == 0) {
            double yaw = this.random.nextDouble() * Math.PI * 2.0D;
            double pitch = (this.random.nextDouble() - 0.5D) * Math.PI;
            double r = 0.7D + this.random.nextDouble() * 0.35D;
            Vec3 arcEnd = to.add(
                    Math.cos(yaw) * Math.cos(pitch) * r,
                    Math.sin(pitch) * r,
                    Math.sin(yaw) * Math.cos(pitch) * r
            );
            ElectrifiedWaterManager.drawElectricArc(level, to, arcEnd, 0.14D, false);
        }
    }

    private static Vec3 closestPointOnSegment(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq <= 1.0E-6D) return a;
        double t = Mth.clamp(p.subtract(a).dot(ab) / lenSq, 0.0D, 1.0D);
        return a.add(ab.scale(t));
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.discard();
            return;
        }
        detonate(serverLevel, hitResult, hitResult.getLocation(), false);
    }

    private void detonate(ServerLevel serverLevel, @Nullable HitResult hitResult, Vec3 hitPos, boolean isTimeout) {
        if (this.isRemoved()) {
            return;
        }
        ServerPlayer attacker = this.getOwner() instanceof ServerPlayer sp ? sp : null;
        int mode = getOmniMode();
        Element chosen = modeToElement(mode);
        if (chosen == null) {
            chosen = resolveBestOmniElement(serverLevel, hitResult, hitPos, attacker);
        }

        LivingEntity directTarget = (hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living != this.getOwner())
                ? living
                : null;

        if (directTarget != null && directTarget.isAlive()) {
            ElementalGaugeManager.applyElement(serverLevel, directTarget, chosen, this.attackU, attacker, this.baseDamage);
            applyPostHitControl(chosen, directTarget, hitPos);
        }

        double splashRadius = isNetheriteTier() ? 2.5D : 1.75D;
        double splashRadiusSq = splashRadius * splashRadius;
        float splashDmg = this.baseDamage * 0.45F;
        float splashU = Math.max(1.0F, this.attackU * 0.5F);
        AABB box = new AABB(hitPos, hitPos).inflate(splashRadius);
        for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != this.getOwner() && e != directTarget)) {
            if (nearby.position().distanceToSqr(hitPos) <= splashRadiusSq) {
                ElementalGaugeManager.applyElement(serverLevel, nearby, chosen, splashU, attacker, splashDmg);
                applyPostHitControl(chosen, nearby, hitPos);
            }
        }

        applyEnvironmentImpact(serverLevel, hitPos, chosen, attacker);
        if (directTarget == null && chosen == Element.PYRO && hitResult instanceof BlockHitResult bhr) {
            EnvironmentalBlockManager.igniteSingleHitBlock(serverLevel, bhr.getBlockPos(), bhr.getDirection());
        }
        spawnImpactEffects(serverLevel, hitPos, chosen, mode == 0);
        if (isTimeout) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, hitPos.x, hitPos.y + 0.1D, hitPos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        this.discard();
    }

    private void applyPostHitControl(Element element, LivingEntity target, Vec3 hitPos) {
        if (element == Element.ANEMO && !ElementalGaugeManager.isEntityFrozen(target)) {
            Vec3 delta = target.getBoundingBox().getCenter().subtract(hitPos);
            Vec3 horiz = new Vec3(delta.x, 0.0D, delta.z);
            if (horiz.lengthSqr() > 1.0E-4D) {
                horiz = horiz.normalize();
                target.push(horiz.x * 0.42D, 0.22D, horiz.z * 0.42D);
                target.hurtMarked = true;
            }
        } else if (element == Element.CRYO
                && ElementalGaugeManager.getInnateElement(target) != Element.CRYO
                && !ElementalGaugeManager.isEntityFrozen(target)) {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), isNetheriteTier() ? 120 : 80));
        }
    }

    private void applyEnvironmentImpact(ServerLevel level, Vec3 hitPos, Element element, @Nullable ServerPlayer attacker) {
        boolean neth = isNetheriteTier();
        double envRadius = neth ? 2.4D : 1.75D;
        DendroCoreManager.checkReactionsNear(level, hitPos, envRadius + 1.0D, element, attacker);

        switch (element) {
            case PYRO -> EnvironmentalBlockManager.checkPyroAmethystMining(level, hitPos, envRadius);
            case HYDRO -> EnvironmentalBlockManager.applyHydroEnvironmentEffects(level, hitPos, hitPos, envRadius);
            case CRYO -> ArcaneStaffEvents.freezeWaterNear(level, hitPos, neth ? 3 : 2);
            case ELECTRO -> {
                EnvironmentalBlockManager.activateTeslaCoilsNear(level, hitPos, envRadius + 0.5D);
                BlockPos hitBlock = BlockPos.containing(hitPos);
                ElectrifiedWaterManager.electrify(level, hitBlock, envRadius + 0.75D, 70, attacker, neth ? 1.5F : 1.0F);
            }
            case ANEMO, DENDRO, GEO -> {
                // Handled directly through EGT reactions (Swirl, Bloom/Spread, Crystallize)
            }
        }
    }

    private void spawnImpactEffects(ServerLevel level, Vec3 hitPos, Element element, boolean wasOmniMode) {
        int count = isNetheriteTier() ? 24 : 16;
        switch (element) {
            case PYRO -> level.sendParticles(ParticleTypes.FLAME, hitPos.x, hitPos.y + 0.1D, hitPos.z, count, 0.35D, 0.35D, 0.35D, 0.06D);
            case HYDRO -> level.sendParticles(ParticleTypes.SPLASH, hitPos.x, hitPos.y + 0.1D, hitPos.z, count + 6, 0.4D, 0.3D, 0.4D, 0.08D);
            case ANEMO -> level.sendParticles(ParticleTypes.CLOUD, hitPos.x, hitPos.y + 0.1D, hitPos.z, count, 0.4D, 0.25D, 0.4D, 0.05D);
            case ELECTRO -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y + 0.1D, hitPos.z, count + 4, 0.4D, 0.35D, 0.4D, 0.08D);
            case DENDRO -> level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, hitPos.x, hitPos.y + 0.1D, hitPos.z, count + 4, 0.45D, 0.35D, 0.45D, 0.05D);
            case CRYO -> level.sendParticles(ParticleTypes.SNOWFLAKE, hitPos.x, hitPos.y + 0.1D, hitPos.z, count + 4, 0.4D, 0.35D, 0.4D, 0.05D);
            case GEO -> {
                int rgb = getElementRgb(Element.GEO);
                DustParticleOptions geoDust = new DustParticleOptions(
                        new Vector3f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F),
                        1.4F);
                level.sendParticles(geoDust, hitPos.x, hitPos.y + 0.1D, hitPos.z, count, 0.4D, 0.35D, 0.4D, 0.04D);
            }
        }
        if (wasOmniMode) {
            level.sendParticles(ParticleTypes.END_ROD, hitPos.x, hitPos.y + 0.1D, hitPos.z, 10, 0.35D, 0.35D, 0.35D, 0.07D);
        }
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, wasOmniMode ? 1.35F : 1.1F);
        level.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.55F, 1.4F);
    }

    private Element resolveBestOmniElement(ServerLevel level, HitResult hitResult, Vec3 hitPos, @Nullable ServerPlayer attacker) {
        if (hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity directTarget) {
            return chooseBestElementForTarget(directTarget, attacker);
        }

        // Check environmental triggers near block impact
        AABB coreSearch = new AABB(hitPos, hitPos).inflate(2.5D);
        List<DendroCoreEntity> cores = level.getEntitiesOfClass(DendroCoreEntity.class, coreSearch,
                c -> c.isAlive() && !c.isHyperbloomVisual());
        if (!cores.isEmpty()) {
            return Element.ELECTRO;
        }

        BlockPos hitBlock = (hitResult instanceof BlockHitResult bhr) ? bhr.getBlockPos() : BlockPos.containing(hitPos);
        BlockPos adjacentBlock = (hitResult instanceof BlockHitResult bhr) ? hitBlock.relative(bhr.getDirection()) : hitBlock;

        if (isTeslaCoilNearby(level, hitBlock)
                || level.getFluidState(hitBlock).is(FluidTags.WATER)
                || level.getFluidState(adjacentBlock).is(FluidTags.WATER)
                || this.isInWater()) {
            return Element.ELECTRO;
        }

        if (isExtinguishableFire(level, hitBlock) || isExtinguishableFire(level, adjacentBlock)) {
            return Element.HYDRO;
        }

        BlockState hitState = level.getBlockState(hitBlock);
        if (EnvironmentalBlockManager.isAmethystClusterOrBud(hitState)
                || hitState.is(Blocks.TNT)
                || hitState.is(Blocks.ICE)
                || hitState.is(Blocks.FROSTED_ICE)
                || hitState.is(Blocks.SNOW)
                || hitState.is(Blocks.SNOW_BLOCK)) {
            return Element.PYRO;
        }

        double mobScan = isNetheriteTier() ? 3.0D : 2.25D;
        LivingEntity nearestMob = level.getEntitiesOfClass(LivingEntity.class, new AABB(hitPos, hitPos).inflate(mobScan),
                e -> e.isAlive() && e != this.getOwner())
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(hitPos)))
                .orElse(null);
        if (nearestMob != null) {
            return chooseBestElementForTarget(nearestMob, attacker);
        }

        return Element.PYRO;
    }

    public static Element chooseBestElementForTarget(LivingEntity target, @Nullable ServerPlayer attacker) {
        Element innate = ElementalGaugeManager.getInnateElement(target);
        Collection<Element> active = ElementalGaugeManager.getEntityElements(target.getUUID());
        boolean frozen = ElementalGaugeManager.isEntityFrozen(target);
        boolean quickened = ElementalGaugeManager.isEntityQuickened(target);
        boolean fireResistant = ElementalGaugeManager.isPyroResistant(target);
        boolean avoidPyroDamage = fireResistant && innate != Element.CRYO;

        // Cryo / Frozen -> Pyro
        if (frozen || active.contains(Element.CRYO) || innate == Element.CRYO) {
            if (!fireResistant && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.ELECTRO) return Element.ELECTRO;
        }

        // Pyro -> Hydro
        if (active.contains(Element.PYRO) || innate == Element.PYRO) {
            if (innate != Element.HYDRO) return Element.HYDRO;
            if (innate != Element.CRYO) return Element.CRYO;
            if (innate != Element.ELECTRO) return Element.ELECTRO;
        }

        // Hydro -> Pyro
        if (active.contains(Element.HYDRO) || innate == Element.HYDRO) {
            if (!avoidPyroDamage && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.ELECTRO) return Element.ELECTRO;
            if (innate != Element.DENDRO) return Element.DENDRO;
        }

        // Dendro / Quickened -> Electro
        if (quickened || active.contains(Element.DENDRO) || innate == Element.DENDRO) {
            if (innate != Element.ELECTRO) return Element.ELECTRO;
            if (!avoidPyroDamage && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.HYDRO) return Element.HYDRO;
        }

        // Electro -> Cryo
        if (active.contains(Element.ELECTRO) || innate == Element.ELECTRO) {
            if (innate != Element.CRYO) return Element.CRYO;
            if (!avoidPyroDamage && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.DENDRO) return Element.DENDRO;
        }

        // Geo -> Pyro
        if (innate == Element.GEO) {
            if (!avoidPyroDamage && innate != Element.PYRO) return Element.PYRO;
            if (innate != Element.HYDRO) return Element.HYDRO;
        }

        // Anemo / Clean target -> deterministic base element (Pyro, or Hydro vs Pyro-resistant)
        return avoidPyroDamage ? Element.HYDRO : Element.PYRO;
    }

    private static boolean isTeslaCoilNearby(ServerLevel level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            if (EnvironmentalBlockManager.isTeslaCoilBlock(level, p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExtinguishableFire(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState st = level.getBlockState(pos);
        if (st.is(BlockTags.FIRE)) return true;
        if (st.getBlock() instanceof CampfireBlock && st.hasProperty(CampfireBlock.LIT) && st.getValue(CampfireBlock.LIT)) {
            return true;
        }
        return st.getBlock() instanceof AbstractCandleBlock && st.hasProperty(AbstractCandleBlock.LIT) && st.getValue(AbstractCandleBlock.LIT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("OmniMode", (byte) getOmniMode());
        tag.putBoolean("IsNetherite", isNetheriteTier());
        tag.putDouble("MaxRange", this.maxRange);
        tag.putFloat("BaseDamage", this.baseDamage);
        tag.putFloat("AttackU", this.attackU);
        if (this.origin != null) {
            tag.putDouble("OriginX", this.origin.x);
            tag.putDouble("OriginY", this.origin.y);
            tag.putDouble("OriginZ", this.origin.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_MODE, (byte) clampMode(tag.getByte("OmniMode")));
        this.entityData.set(DATA_NETHERITE, tag.getBoolean("IsNetherite"));
        if (tag.contains("MaxRange")) this.maxRange = tag.getDouble("MaxRange");
        if (tag.contains("BaseDamage")) this.baseDamage = tag.getFloat("BaseDamage");
        if (tag.contains("AttackU")) this.attackU = tag.getFloat("AttackU");
        if (tag.contains("OriginX") && tag.contains("OriginY") && tag.contains("OriginZ")) {
            this.origin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        }
    }
}
