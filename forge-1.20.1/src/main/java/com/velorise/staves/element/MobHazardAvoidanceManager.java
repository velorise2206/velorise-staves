package com.velorise.staves.element;

import com.velorise.staves.manager.TemporaryWebManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes mobs avoid walking into elemental hazard blocks/zones/aura sources and flee away
 * when damaged by an elemental reaction with no visible enemy nearby.
 */
public final class MobHazardAvoidanceManager {
    private static final long FLEE_DURATION_TICKS = 90L; // 4.5 seconds

    private record DangerState(ServerLevel level, Vec3 dangerPos, long expireTick) {}

    private static final Map<UUID, DangerState> DANGER_STATES = new ConcurrentHashMap<>();

    private MobHazardAvoidanceManager() {}

    public static void clearEntityState(@Nullable LivingEntity entity) {
        if (entity != null) {
            DANGER_STATES.remove(entity.getUUID());
        }
    }

    public static void clearAll() {
        DANGER_STATES.clear();
    }

    public static void attachGoalIfEligible(@Nullable LivingEntity entity) {
        if (!(entity instanceof PathfinderMob mob) || mob.level().isClientSide()) return;
        if (isExcludedBoss(mob)) return;
        for (var wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof AvoidElementalHazardsGoal) {
                return;
            }
        }
        mob.goalSelector.addGoal(1, new AvoidElementalHazardsGoal(mob));
    }

    /**
     * Records a reaction/hazard danger location for a mob when hit with no nearby visible enemy,
     * causing the mob to treat the reaction spot as dangerous and flee away from it.
     */
    public static void reportReactionOrHazard(
            @Nullable LivingEntity target,
            @Nullable LivingEntity attacker,
            @Nullable Vec3 sourcePos,
            @Nullable Element elem1,
            @Nullable Element elem2
    ) {
        if (target == null || sourcePos == null || !(target.level() instanceof ServerLevel sl)) return;
        if (!(target instanceof Mob mob) || !mob.isAlive() || mob.isNoAi() || mob.isPassenger()) return;
        if (isExcludedBoss(mob) || ElementalGaugeManager.isOmniInnate(mob) || ElementalGaugeManager.isEntityFrozen(mob)) {
            return;
        }
        if (isImmuneToAllElements(mob, elem1, elem2)) {
            return;
        }
        if (hasNearbyVisibleEnemy(mob, attacker)) {
            return;
        }
        DANGER_STATES.put(mob.getUUID(), new DangerState(sl, sourcePos, sl.getGameTime() + FLEE_DURATION_TICKS));
    }

    public static void tick(MinecraftServer server) {
        if (DANGER_STATES.isEmpty()) return;
        Iterator<Map.Entry<UUID, DangerState>> it = DANGER_STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DangerState> entry = it.next();
            DangerState state = entry.getValue();
            if (state.level.getServer() != server) continue;
            long now = state.level.getGameTime();
            if (now >= state.expireTick || (state.expireTick - now) > 600L) {
                it.remove();
                continue;
            }
            if (!(state.level.getEntity(entry.getKey()) instanceof Mob mob) || !mob.isAlive() || mob.isNoAi()) {
                it.remove();
                continue;
            }
            if (mob instanceof PathfinderMob || ElementalGaugeManager.isEntityFrozen(mob)) {
                continue;
            }
            if (hasNearbyVisibleEnemy(mob, null)) {
                it.remove();
                continue;
            }
            Vec3 away = mob.position().subtract(state.dangerPos);
            Vec3 horiz = new Vec3(away.x, 0.0D, away.z);
            if (horiz.lengthSqr() < 1.0E-4D) {
                double ang = (mob.getId() * 1.37D) % (Math.PI * 2.0D);
                horiz = new Vec3(Math.cos(ang), 0.0D, Math.sin(ang));
            } else {
                horiz = horiz.normalize();
            }
            Vec3 steer = mob.position().add(horiz.scale(6.0D));
            mob.getMoveControl().setWantedPosition(steer.x, mob.getY(), steer.z, 1.25D);
        }
    }

    private static boolean isExcludedBoss(Mob mob) {
        return mob instanceof EnderDragon || mob instanceof WitherBoss;
    }

    private static boolean isImmuneToAllElements(Mob mob, @Nullable Element e1, @Nullable Element e2) {
        if (e1 == null && e2 == null) return false;
        boolean imm1 = (e1 == null) || isHazardImmune(mob, e1);
        boolean imm2 = (e2 == null) || isHazardImmune(mob, e2);
        return imm1 && imm2;
    }

    private static boolean isHazardImmune(Mob mob, @Nullable Element elem) {
        if (elem == null) return false;
        if (ElementalGaugeManager.isOmniInnate(mob)) return true;
        Element innate = ElementalGaugeManager.getInnateElement(mob);
        if (innate == elem) return true;
        return elem == Element.PYRO && (mob.fireImmune() || mob.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE));
    }

    private static boolean hasNearbyVisibleEnemy(Mob mob, @Nullable LivingEntity attacker) {
        LivingEntity curTarget = mob.getTarget();
        if (curTarget != null && curTarget.isAlive() && mob.distanceToSqr(curTarget) <= 256.0D) {
            return true;
        }
        if (attacker != null && attacker != mob && attacker.isAlive()
                && !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()))
                && mob.distanceToSqr(attacker) <= 196.0D && mob.hasLineOfSight(attacker)) {
            return true;
        }
        LivingEntity lastHurt = mob.getLastHurtByMob();
        return lastHurt != null && lastHurt != mob && lastHurt.isAlive()
                && (mob.tickCount - mob.getLastHurtByMobTimestamp()) <= 60
                && mob.distanceToSqr(lastHurt) <= 144.0D
                && mob.hasLineOfSight(lastHurt);
    }

    private static @Nullable Element getBlockHazardElement(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        if (ElectrifiedWaterManager.isBlockElectrified(level, pos)) {
            return Element.ELECTRO;
        }
        BlockState st = level.getBlockState(pos);
        if (st.isAir()) return null;

        if (st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.LAVA)
                || st.is(Blocks.MAGMA_BLOCK) || level.getFluidState(pos).is(FluidTags.LAVA)) {
            return Element.PYRO;
        }
        if ((st.getBlock() instanceof CampfireBlock)
                && (!st.hasProperty(CampfireBlock.LIT) || st.getValue(CampfireBlock.LIT))) {
            return Element.PYRO;
        }
        if (st.is(Blocks.POWDER_SNOW)) {
            return Element.CRYO;
        }
        if (EnvironmentalBlockManager.isBlockFormRedstoneLightningSource(st)
                || (st.is(Blocks.REDSTONE_WIRE) && st.hasProperty(RedStoneWireBlock.POWER) && st.getValue(RedStoneWireBlock.POWER) > 0)
                || st.is(Blocks.AMETHYST_CLUSTER)
                || EnvironmentalBlockManager.isSolidAmethystBlock(st)
                || st.is(Blocks.SCULK_SENSOR)
                || st.is(Blocks.CALIBRATED_SCULK_SENSOR)
                || st.is(Blocks.LIGHTNING_ROD)) {
            return Element.ELECTRO;
        }
        if (st.is(Blocks.SLIME_BLOCK) || st.is(Blocks.HONEY_BLOCK)
                || st.is(Blocks.CACTUS) || st.is(Blocks.SWEET_BERRY_BUSH)
                || st.is(Blocks.COBWEB) || TemporaryWebManager.isTemporaryWeb(level, pos)) {
            return Element.DENDRO;
        }
        if (st.is(Blocks.POINTED_DRIPSTONE)) {
            return Element.GEO;
        }
        return null;
    }

    private static @Nullable Element checkPosHazard(ServerLevel level, BlockPos pos, Mob mob) {
        Element feet = getBlockHazardElement(level, pos);
        if (feet != null && !isHazardImmune(mob, feet)) return feet;
        Element below = getBlockHazardElement(level, pos.below());
        if (below != null && !isHazardImmune(mob, below)) return below;
        return null;
    }

    private static @Nullable Vec3 findZoneHazard(ServerLevel level, Vec3 pos, double margin, Mob mob) {
        if (!isHazardImmune(mob, Element.PYRO)) {
            Vec3 fire = GhastfireFieldManager.findNearestHazard(level, pos, margin);
            if (fire != null) return fire;
        }
        if (!isHazardImmune(mob, Element.ELECTRO)) {
            Vec3 dragon = DragonfireCloudManager.findNearestHazard(level, pos, margin);
            if (dragon != null) return dragon;
            Vec3 water = ElectrifiedWaterManager.findNearestHazard(level, pos, margin);
            if (water != null) return water;
        }
        if (!isHazardImmune(mob, Element.DENDRO)) {
            Vec3 core = DendroCoreManager.findNearestHazard(level, pos, margin);
            if (core != null) return core;
        }
        return null;
    }

    private static @Nullable Vec3 findImmediateHazardForMob(PathfinderMob mob, ServerLevel level) {
        Vec3 pos = mob.position();
        Vec3 zone = findZoneHazard(level, pos, 1.1D, mob);
        if (zone != null) return zone;

        BlockPos foot = mob.blockPosition();
        if (checkPosHazard(level, foot, mob) != null) {
            return Vec3.atCenterOf(foot);
        }

        Vec3 vel = mob.getDeltaMovement();
        Vec3 stepDir = new Vec3(vel.x, 0.0D, vel.z);
        if (stepDir.lengthSqr() < 1.0E-4D) {
            Vec3 look = mob.getLookAngle();
            stepDir = new Vec3(look.x, 0.0D, look.z);
        }
        if (stepDir.lengthSqr() >= 1.0E-4D) {
            Vec3 ahead = pos.add(stepDir.normalize().scale(1.15D));
            Vec3 aheadZone = findZoneHazard(level, ahead, 0.65D, mob);
            if (aheadZone != null) return aheadZone;
            BlockPos aheadBlock = BlockPos.containing(ahead.x, ahead.y, ahead.z);
            if (checkPosHazard(level, aheadBlock, mob) != null) {
                return Vec3.atCenterOf(aheadBlock);
            }
        }

        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            int idx = path.getNextNodeIndex();
            int limit = Math.min(path.getNodeCount(), idx + 2);
            for (int i = idx; i < limit; i++) {
                Node node = path.getNode(i);
                BlockPos nodePos = new BlockPos(node.x, node.y, node.z);
                if (checkPosHazard(level, nodePos, mob) != null) {
                    return Vec3.atCenterOf(nodePos);
                }
                Vec3 nodeZone = findZoneHazard(level, Vec3.atCenterOf(nodePos), 0.5D, mob);
                if (nodeZone != null) return nodeZone;
            }
        }
        return null;
    }

    private static boolean isUnsafeSpot(ServerLevel level, Vec3 candidate, Mob mob) {
        BlockPos bp = BlockPos.containing(candidate.x, candidate.y, candidate.z);
        return checkPosHazard(level, bp, mob) != null || findZoneHazard(level, candidate, 0.8D, mob) != null;
    }

    private static @Nullable Vec3 findSafeAwaySpot(PathfinderMob mob, ServerLevel level, Vec3 hazardPos, int horizRange, int vertRange) {
        Vec3 bestFallback = null;
        double bestDistSq = mob.position().distanceToSqr(hazardPos);
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 cand = DefaultRandomPos.getPosAway(mob, horizRange, vertRange, hazardPos);
            if (cand == null) continue;
            if (!isUnsafeSpot(level, cand, mob)) {
                return cand;
            }
            double d2 = cand.distanceToSqr(hazardPos);
            if (d2 > bestDistSq) {
                bestDistSq = d2;
                bestFallback = cand;
            }
        }
        return bestFallback;
    }

    private static final class AvoidElementalHazardsGoal extends Goal {
        private final PathfinderMob mob;
        private double targetX;
        private double targetY;
        private double targetZ;
        private double moveSpeed;
        private Vec3 dangerPos = Vec3.ZERO;
        private boolean isReactionFlee;
        private long nextCheckTick;
        private long avoidCooldownUntil;
        private long nextRepathTick;

        private AvoidElementalHazardsGoal(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!mob.isAlive() || mob.isNoAi() || mob.isPassenger() || ElementalGaugeManager.isEntityFrozen(mob)) {
                return false;
            }
            if (mob instanceof TamableAnimal tame && tame.isOrderedToSit()) {
                return false;
            }
            if (!(mob.level() instanceof ServerLevel level)) {
                return false;
            }

            long now = level.getGameTime();
            DangerState danger = DANGER_STATES.get(mob.getUUID());
            if (danger != null && danger.level == level && now < danger.expireTick) {
                if (hasNearbyVisibleEnemy(mob, null)) {
                    DANGER_STATES.remove(mob.getUUID());
                } else if (mob.position().distanceToSqr(danger.dangerPos) < 121.0D || isUnsafeSpot(level, mob.position(), mob)) {
                    Vec3 away = findSafeAwaySpot(mob, level, danger.dangerPos, 12, 5);
                    if (away != null) {
                        this.targetX = away.x;
                        this.targetY = away.y;
                        this.targetZ = away.z;
                        this.dangerPos = danger.dangerPos;
                        this.moveSpeed = 1.38D;
                        this.isReactionFlee = true;
                        return true;
                    }
                }
            }

            if (now < this.avoidCooldownUntil || now < this.nextCheckTick) {
                return false;
            }
            this.nextCheckTick = now + 5L;

            Vec3 hazard = findImmediateHazardForMob(mob, level);
            if (hazard == null) {
                return false;
            }

            boolean inCombat = mob.getTarget() != null && mob.getTarget().isAlive();
            Vec3 away = findSafeAwaySpot(mob, level, hazard, inCombat ? 6 : 9, inCombat ? 3 : 4);
            this.avoidCooldownUntil = now + (inCombat ? 24L : 14L);
            if (away == null) {
                return false;
            }

            this.targetX = away.x;
            this.targetY = away.y;
            this.targetZ = away.z;
            this.dangerPos = hazard;
            this.moveSpeed = inCombat ? 1.25D : 1.18D;
            this.isReactionFlee = false;
            return true;
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(this.targetX, this.targetY, this.targetZ, this.moveSpeed);
            this.nextRepathTick = mob.level().getGameTime() + 12L;
        }

        @Override
        public boolean canContinueToUse() {
            if (!mob.isAlive() || mob.isNoAi() || mob.isPassenger() || ElementalGaugeManager.isEntityFrozen(mob)) {
                return false;
            }
            if (!(mob.level() instanceof ServerLevel level)) {
                return false;
            }
            if (this.isReactionFlee) {
                long now = level.getGameTime();
                DangerState danger = DANGER_STATES.get(mob.getUUID());
                if (danger == null || now >= danger.expireTick || hasNearbyVisibleEnemy(mob, null)) {
                    return false;
                }
                if (mob.position().distanceToSqr(danger.dangerPos) >= 100.0D && !isUnsafeSpot(level, mob.position(), mob)) {
                    return false;
                }
                if (mob.getNavigation().isDone() && now >= this.nextRepathTick) {
                    this.nextRepathTick = now + 15L;
                    Vec3 nextAway = findSafeAwaySpot(mob, level, danger.dangerPos, 12, 5);
                    if (nextAway != null) {
                        this.targetX = nextAway.x;
                        this.targetY = nextAway.y;
                        this.targetZ = nextAway.z;
                        mob.getNavigation().moveTo(this.targetX, this.targetY, this.targetZ, this.moveSpeed);
                    } else {
                        return false;
                    }
                }
                return true;
            }
            return !mob.getNavigation().isDone();
        }

        @Override
        public void stop() {
            this.isReactionFlee = false;
        }
    }
}
