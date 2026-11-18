package com.velorise.staves.event;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.DendroCoreManager;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.manager.TemporaryWebManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Server-side impact and range resolution for Arcane Staves projectiles. */
public final class ArcaneStaffEvents {
    private static final String TAG_PREFIX_KIND = "velorise:kind:";
    private static final String TAG_PREFIX_RANGE = "velorise:range:";
    private static final String TAG_PREFIX_ORIGIN = "velorise:origin:";
    private static final String TAG_PREFIX_SHULKER_TARGET = "velorise:shulker_target:";

    private ArcaneStaffEvents() {}

    public static void markKind(Projectile projectile, String kind) {
        projectile.addTag(TAG_PREFIX_KIND + kind);
    }

    public static void markShulkerTarget(Projectile projectile, java.util.UUID targetId) {
        if (targetId != null) {
            projectile.addTag(TAG_PREFIX_SHULKER_TARGET + targetId);
        }
    }

    public static @Nullable java.util.UUID getShulkerTarget(Projectile projectile) {
        for (String tag : projectile.getTags()) {
            if (tag.startsWith(TAG_PREFIX_SHULKER_TARGET)) {
                try {
                    return java.util.UUID.fromString(tag.substring(TAG_PREFIX_SHULKER_TARGET.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static void markRange(Projectile projectile, Vec3 origin, double maxRange) {
        projectile.addTag(TAG_PREFIX_ORIGIN + origin.x + ":" + origin.y + ":" + origin.z);
        projectile.addTag(TAG_PREFIX_RANGE + Math.max(1.0D, maxRange));
    }

    public static String getKind(Projectile projectile) {
        for (String tag : projectile.getTags()) {
            if (tag.startsWith(TAG_PREFIX_KIND)) {
                return tag.substring(TAG_PREFIX_KIND.length());
            }
        }
        return null;
    }

    public static void onProjectileTick(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        String kind = getKind(projectile);
        if (kind != null && !kind.isEmpty()) {
            if (isBeyondRange(projectile, projectile.position())) {
                level.sendParticles(ParticleTypes.SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.12D, 0.12D, 0.12D, 0.01D);
                projectile.discard();
                return;
            }
        }

        // Check Cryo water surface freezing for plain Snowball and Frost staff projectile
        boolean isSnowball = projectile instanceof Snowball && (kind == null || kind.isEmpty());
        boolean isFrostStaff = kind != null && kind.endsWith("frost");
        if (isSnowball || isFrostStaff) {
            Vec3 pos = projectile.position();
            Vec3 delta = projectile.getDeltaMovement();
            Vec3 nextPos = pos.add(delta);

            // Fluid raycast ahead to intercept water surface before projectile sinks into deep water
            BlockHitResult fluidHit = level.clip(new ClipContext(
                pos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                projectile
            ));

            boolean hitWater = false;
            Vec3 freezeLoc = null;

            if (fluidHit.getType() == HitResult.Type.BLOCK && level.getFluidState(fluidHit.getBlockPos()).is(FluidTags.WATER)) {
                hitWater = true;
                freezeLoc = fluidHit.getLocation();
            } else if (projectile.isInWater() || level.getFluidState(projectile.blockPosition()).is(FluidTags.WATER)) {
                hitWater = true;
                freezeLoc = pos;
            }

            if (hitWater) {
                int radius = (isFrostStaff && kind.startsWith("netherite_")) ? 2 : 1;
                freezeWaterNear(level, freezeLoc, radius);
                level.sendParticles(ParticleTypes.SNOWFLAKE, freezeLoc.x, freezeLoc.y + 0.15D, freezeLoc.z, 18, 0.35D, 0.1D, 0.35D, 0.03D);
                level.playSound(null, freezeLoc.x, freezeLoc.y, freezeLoc.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.5F);
                projectile.discard();
                return;
            }
        }

        if (projectile instanceof net.minecraft.world.entity.projectile.ShulkerBullet bullet
                && ("shulker".equals(kind) || "netherite_shulker".equals(kind))) {
            java.util.UUID targetId = getShulkerTarget(bullet);
            if (targetId != null) {
                Entity targetEnt = level.getEntity(targetId);
                if (targetEnt instanceof LivingEntity target && target.isAlive() && !target.isSpectator()) {
                    double speed = bullet.getTags().contains("velorise_staves:finale_shot") ? 0.85D : 0.68D;
                    Vec3 bPos = bullet.position();
                    Vec3 aimPos = target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(0.35D));
                    BlockHitResult los = level.clip(new ClipContext(bPos, aimPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
                    Vec3 steerTarget = (los.getType() == HitResult.Type.MISS)
                            ? aimPos
                            : findNextAStarWaypoint(level, bullet.blockPosition(), BlockPos.containing(aimPos), 160);
                    Vec3 steerDir = steerTarget.subtract(bPos);
                    if (steerDir.lengthSqr() > 1.0E-5D) {
                        bullet.setDeltaMovement(steerDir.normalize().scale(speed));
                        bullet.hurtMarked = true;
                    }
                }
            }
        }
    }

    private static Vec3 findNextAStarWaypoint(ServerLevel level, BlockPos start, BlockPos goal, int maxNodes) {
        if (start.equals(goal)) return Vec3.atCenterOf(goal);
        java.util.PriorityQueue<AStarNode> open = new java.util.PriorityQueue<>(java.util.Comparator.comparingDouble(n -> n.f));
        java.util.Map<BlockPos, Double> bestG = new java.util.HashMap<>();
        java.util.Map<BlockPos, BlockPos> cameFrom = new java.util.HashMap<>();
        double h0 = Math.sqrt(start.distSqr(goal));
        open.add(new AStarNode(start, 0.0D, h0));
        bestG.put(start, 0.0D);

        BlockPos closest = start;
        double closestH = h0;
        int explored = 0;
        int[][] dirs = {
                {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1},
                {1,1,0},{-1,1,0},{0,1,1},{0,1,-1},{1,-1,0},{-1,-1,0},{0,-1,1},{0,-1,-1}
        };

        while (!open.isEmpty() && explored < maxNodes) {
            AStarNode cur = open.poll();
            if (cur.pos.equals(goal)) {
                closest = goal;
                break;
            }
            explored++;
            if (cur.g > bestG.getOrDefault(cur.pos, Double.MAX_VALUE) + 1.0E-4D) continue;
            for (int[] d : dirs) {
                BlockPos next = cur.pos.offset(d[0], d[1], d[2]);
                if (!next.equals(goal) && !level.getBlockState(next).getCollisionShape(level, next).isEmpty()) {
                    continue;
                }
                double stepCost = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                double ng = cur.g + stepCost;
                if (ng < bestG.getOrDefault(next, Double.MAX_VALUE)) {
                    bestG.put(next, ng);
                    cameFrom.put(next, cur.pos);
                    double nh = Math.sqrt(next.distSqr(goal));
                    if (nh < closestH) {
                        closestH = nh;
                        closest = next;
                    }
                    open.add(new AStarNode(next, ng, ng + nh * 1.15D));
                }
            }
        }
        if (closest.equals(start)) return Vec3.atCenterOf(goal);
        BlockPos step = closest;
        while (cameFrom.containsKey(step) && !cameFrom.get(step).equals(start)) {
            step = cameFrom.get(step);
        }
        return Vec3.atCenterOf(step);
    }

    private record AStarNode(BlockPos pos, double g, double f) {}

    public static boolean onProjectileHit(Projectile projectile, HitResult hitResult) {
        if (!(projectile.level() instanceof ServerLevel level)) return false;

        // Splash & Lingering Potions apply 1.0U mapped element(s) to entities in splash radius
        if (projectile instanceof ThrownPotion potion) {
            handlePotionSplash(level, potion, hitResult.getLocation());
        }

        // Tipped Arrows apply 1.0U mapped element(s) on direct hit
        if (projectile instanceof Arrow arrow) {
            LivingEntity directHit = hitEntity(hitResult);
            if (directHit != null) {
                ElementalGaugeManager.handleTippedArrowHit(level, arrow, directHit);
            }
        }

        String kind = getKind(projectile);
        if (hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr && ehr.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal) {
            Entity owner = projectile.getOwner();
            DamageSource ds = owner instanceof ServerPlayer sp ? level.damageSources().playerAttack(sp) : level.damageSources().magic();
            crystal.hurt(ds, 5.0F);
        }
        if (hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr
                && ehr.getEntity() instanceof com.velorise.staves.entity.GeoFangPillarEntity construct) {
            if (construct.handleProjectileBlocked(level, projectile, kind != null ? kind : "")) {
                projectile.discard();
            }
            return true;
        }
        if (kind == null || kind.isEmpty()) {
            if (projectile.isOnFire()
                    || projectile instanceof net.minecraft.world.entity.projectile.SmallFireball
                    || projectile instanceof net.minecraft.world.entity.projectile.LargeFireball) {
                Vec3 fireHit = hitResult.getLocation();
                com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(level, fireHit, 2.0D);
            }
            if (projectile instanceof Snowball) {
                Vec3 hit = hitResult.getLocation();
                Entity owner = projectile.getOwner();
                ServerPlayer thrower = owner instanceof ServerPlayer sp ? sp : null;
                LivingEntity directHit = hitEntity(hitResult);
                if (directHit != null && directHit != owner) {
                    ElementalGaugeManager.applyElement(level, directHit, Element.CRYO, 0.5F, thrower, 0.0F);
                }
                freezeWaterNear(level, hit, 1);
            }
            return false;
        }

        Vec3 hit = hitResult.getLocation();
        if (isBeyondRange(projectile, hit)) {
            level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 8, 0.12D, 0.12D, 0.12D, 0.01D);
            projectile.discard();
            return true;
        }

        Entity owner = projectile.getOwner();
        boolean isNetherite = kind.startsWith("netherite_");
        String baseKind = isNetherite ? kind.substring("netherite_".length()) : kind;

        if ("ghastfire".equals(baseKind)) {
            resolveGhastfire(level, hit, owner, hitResult, isNetherite);
            projectile.discard();
            return true; // Cancel vanilla LargeFireball explosion to let EGT control damage
        }

        com.velorise.staves.element.DragonfireCloudManager.setCurrentImpactFinale(projectile.getTags().contains("velorise_staves:finale_shot"));
        try {
        switch (baseKind) {
            case "dragonfire" -> resolveDragonfire(level, hit, owner, isNetherite);
            case "inferno" -> resolveInferno(level, hit, owner, hitResult, isNetherite);
            case "wither" -> resolveWither(level, hit, owner, isNetherite);
            case "shulker" -> resolveShulker(level, hit, owner, hitResult, isNetherite);
            case "frost" -> resolveFrost(level, hit, owner, hitResult, isNetherite);
            case "web" -> resolveWeb(level, hit, owner, hitResult, isNetherite);
            case "wind" -> resolveWind(level, hit, owner, hitResult, isNetherite);
            case "daybreak" -> resolveDaybreak(level, hit, owner, projectile, hitResult, isNetherite);
            case "guardian_orb" -> resolveGuardianOrb(level, hit, owner, hitResult, isNetherite);
            case "omni_trident" -> resolveOmniTrident(level, hit, owner, hitResult, isNetherite);
            default -> {}
        }
        } finally {
            com.velorise.staves.element.DragonfireCloudManager.setCurrentImpactFinale(false);
        }
        projectile.discard();
        return true;
    }

    private static void resolveDragonfire(ServerLevel level, Vec3 hit, Entity owner, boolean isNetherite) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, hit.x, hit.y, hit.z);
        if (owner instanceof LivingEntity living) {
            cloud.setOwner(living);
        }
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        float radius = isNetherite ? 4.5F : 3.5F;
        cloud.setRadius(0.0F);
        cloud.setDuration(120); // 6 seconds
        cloud.setRadiusPerTick(0.0F);
        cloud.setWaitTime(140);
        cloud.addTag("velorise_staves:dragonfire_cloud");
        level.addFreshEntity(cloud);

        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.DRAGONFIRE, isNetherite);
        float attackU = isNetherite ? 3.0F : 2.0F;
        for (LivingEntity target : targets(level, hit, radius, owner)) {
            ElementalGaugeManager.applyElement(level, target, Element.ELECTRO, attackU, player, baseDmg);
        }
        com.velorise.staves.element.DendroCoreManager.checkReactionsNear(level, hit, radius, Element.ELECTRO, player);

        // Immediately electrify water beneath/around impact pos and register 1s expanding ring manager
        BlockPos hitPos = BlockPos.containing(hit);
        com.velorise.staves.element.ElectrifiedWaterManager.electrify(level, hitPos, radius + 1.0D, 120, player, 1.0F);
        com.velorise.staves.element.DragonfireCloudManager.register(cloud, level, player, isNetherite, radius);

        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.15F);
    }

    private static void resolveInferno(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        LivingEntity target = hitEntity(hitResult);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        boolean isFinale = com.velorise.staves.element.DragonfireCloudManager.isCurrentImpactFinale();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.INFERNO, isNetherite) * (isFinale ? 1.5F : 1.0F);
        float attackU = isFinale ? (isNetherite ? 3.0F : 2.0F) : (isNetherite ? 1.5F : 1.0F);
        if (target != null && target != owner) {
            boolean hadElectro = ElementalGaugeManager.hasElement(target, Element.ELECTRO)
                    || ElementalGaugeManager.hasInnateElement(target, Element.ELECTRO);
            if (!ElementalGaugeManager.isPyroInnate(target) && !hadElectro) {
                target.igniteForSeconds(isNetherite ? 5 : 3);
            }
            ElementalGaugeManager.applyElement(level, target, Element.PYRO, attackU, player, baseDmg);
        }
        if (isFinale) {
            double finaleRadius = isNetherite ? 3.2D : 2.5D;
            for (LivingEntity splash : targets(level, hit, finaleRadius, owner)) {
                if (splash == target) continue;
                boolean hadElectro = ElementalGaugeManager.hasElement(splash, Element.ELECTRO)
                        || ElementalGaugeManager.hasInnateElement(splash, Element.ELECTRO);
                if (!ElementalGaugeManager.isPyroInnate(splash) && !hadElectro) {
                    splash.igniteForSeconds(isNetherite ? 5 : 3);
                }
                ElementalGaugeManager.applyElement(level, splash, Element.PYRO, attackU, player, baseDmg * 0.85F);
            }
            level.sendParticles(ParticleTypes.LAVA, hit.x, hit.y + 0.2D, hit.z, 18, 0.55D, 0.35D, 0.55D, 0.05D);
            level.sendParticles(ParticleTypes.EXPLOSION, hit.x, hit.y + 0.25D, hit.z, 2, 0.25D, 0.15D, 0.25D, 0.0D);
            level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.85F, 1.25F);
        } else if (hitResult instanceof BlockHitResult bhr) {
            com.velorise.staves.element.EnvironmentalBlockManager.igniteSingleHitBlock(level, bhr.getBlockPos(), bhr.getDirection());
        }
        meltIceNear(level, hit, isNetherite ? 2 : 1);
        checkEndCrystalsNear(level, hit, isNetherite ? 2.5D : 1.8D, owner);
        com.velorise.staves.element.DendroCoreManager.checkReactionsNear(level, hit, 3.0D, Element.PYRO, player);
        com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(level, hit, isNetherite ? 2.5D : 2.0D);
        level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, isNetherite ? 35 : 25, 0.3D, 0.3D, 0.3D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void resolveGhastfire(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        double radius = isNetherite ? 4.5D : 3.5D;
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.GHASTFIRE, isNetherite);
        float attackU = isNetherite ? 3.0F : 2.0F;
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        List<LivingEntity> affected = targets(level, hit, radius, owner);
        for (LivingEntity target : affected) {
            boolean hadElectro = ElementalGaugeManager.hasElement(target, Element.ELECTRO)
                    || ElementalGaugeManager.hasInnateElement(target, Element.ELECTRO);
            if (!ElementalGaugeManager.isPyroInnate(target) && !hadElectro) {
                target.igniteForSeconds(isNetherite ? 5 : 4);
            }
            ElementalGaugeManager.applyElement(level, target, Element.PYRO, attackU, player, baseDmg);
        }
        checkEndCrystalsNear(level, hit, radius, owner);
        if (affected.isEmpty() && hitResult instanceof BlockHitResult bhr) {
            com.velorise.staves.element.EnvironmentalBlockManager.igniteSingleHitBlock(level, bhr.getBlockPos(), bhr.getDirection());
        }
        meltIceNear(level, hit, (int) Math.round(radius * 0.6D));
        com.velorise.staves.element.DendroCoreManager.checkReactionsNear(level, hit, radius, Element.PYRO, player);
        com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(level, hit, radius);
        level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, isNetherite ? 45 : 35,
            radius * 0.35D, radius * 0.25D, radius * 0.35D, 0.05D);

        // Lingering 5-second (100 ticks) fire field AoE with 1s expanding ring pulse
        AreaEffectCloud fireField = new AreaEffectCloud(level, hit.x, hit.y, hit.z);
        if (owner instanceof LivingEntity living) {
            fireField.setOwner(living);
        }
        fireField.setParticle(ParticleTypes.FLAME);
        fireField.setRadius(0.0F);
        fireField.setDuration(100); // 5 seconds
        fireField.setRadiusPerTick(0.0F);
        fireField.setWaitTime(120);
        fireField.addTag("velorise_staves:ghastfire_field");
        level.addFreshEntity(fireField);
        com.velorise.staves.element.GhastfireFieldManager.register(fireField, level, player, isNetherite, (float) radius);

        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void meltIceNear(ServerLevel level, Vec3 hit, int radius) {
        BlockPos center = BlockPos.containing(hit);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (level.getBlockState(p).is(Blocks.FROSTED_ICE) || level.getBlockState(p).is(Blocks.ICE)) {
                level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                level.sendParticles(ParticleTypes.CLOUD, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 6, 0.2D, 0.2D, 0.2D, 0.02D);
                level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
            }
        }
    }

    public static void checkEndCrystalsNear(ServerLevel level, Vec3 hit, double radius, @Nullable Entity owner) {
        AABB box = new AABB(hit, hit).inflate(radius);
        for (net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal : level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, box)) {
            DamageSource ds = owner instanceof ServerPlayer sp ? level.damageSources().playerAttack(sp) : level.damageSources().magic();
            crystal.hurt(ds, 5.0F);
        }
    }

    private static void resolveWither(ServerLevel level, Vec3 hit, Entity owner, boolean isNetherite) {
        double radius = isNetherite ? 4.0D : 3.0D;
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.WITHER, isNetherite);
        float attackU = isNetherite ? 1.5F : 1.0F;
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        for (LivingEntity target : targets(level, hit, radius, owner)) {
            com.velorise.staves.element.DragonfireCloudManager.markWitherContagion(target, player, isNetherite, 160);
            ElementalGaugeManager.applyElementBypassIcd(level, target, Element.DENDRO, attackU, player, baseDmg);
            if (ElementalGaugeManager.getInnateElement(target) != Element.DENDRO) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, isNetherite ? 150 : 100, isNetherite ? 1 : 0));
            }
        }
        level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, isNetherite ? 40 : 30,
            radius * 0.4D, radius * 0.35D, radius * 0.4D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void resolveShulker(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        LivingEntity target = hitEntity(hitResult);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        boolean isFinale = com.velorise.staves.element.DragonfireCloudManager.isCurrentImpactFinale();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.SHULKER, isNetherite);
        float attackU = isFinale ? (isNetherite ? 2.5F : 2.0F) : (isNetherite ? 1.5F : 1.0F);
        if (target != null && target != owner) {
            ElementalGaugeManager.applyElement(level, target, Element.ELECTRO, attackU, player, baseDmg);
            if (isFinale) {
                com.velorise.staves.element.DragonfireCloudManager.scheduleShulkerGravitySlam(level, target, player, isNetherite);
            } else if (ElementalGaugeManager.getInnateElement(target) != Element.ELECTRO
                    && !ElementalGaugeManager.isBossEntity(target)
                    && !ElementalGaugeManager.isEntityFrozen(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, isNetherite ? 70 : 50, 0));
            }
        }
        double radius = isNetherite ? 3.0D : 2.2D;
        for (LivingEntity splashTarget : targets(level, hit, radius, owner)) {
            if (splashTarget == target) continue;
            ElementalGaugeManager.applyElement(level, splashTarget, Element.ELECTRO, 1.0F, player, baseDmg * 0.5F);
            if (ElementalGaugeManager.getInnateElement(splashTarget) != Element.ELECTRO
                    && !ElementalGaugeManager.isBossEntity(splashTarget)
                    && !ElementalGaugeManager.isEntityFrozen(splashTarget)) {
                splashTarget.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
            }
        }
        com.velorise.staves.element.DendroCoreManager.checkReactionsNear(level, hit, 3.5D, Element.ELECTRO, player);
        level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 18, 0.55D, 0.35D, 0.55D, 0.06D);
        level.sendParticles(ParticleTypes.PORTAL, hit.x, hit.y, hit.z, 20, 0.65D, 0.45D, 0.65D, 0.08D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 0.85F, 1.0F);
    }

    private static void resolveFrost(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        LivingEntity target = hitEntity(hitResult);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        boolean isFinale = com.velorise.staves.element.DragonfireCloudManager.isCurrentImpactFinale();
        float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.FROST, isNetherite) * (isFinale ? 1.5F : 1.0F);
        float attackU = isFinale ? (isNetherite ? 3.0F : 2.0F) : (isNetherite ? 1.5F : 1.0F);
        int slowAmp = isFinale ? 3 : 1;
        if (target != null && target != owner) {
            ElementalGaugeManager.applyElement(level, target, Element.CRYO, attackU, player, baseDmg);
            if (ElementalGaugeManager.getInnateElement(target) != Element.CRYO
                    && !ElementalGaugeManager.isBossEntity(target)
                    && !ElementalGaugeManager.isEntityFrozen(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, isNetherite ? 110 : 80, slowAmp));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), isFinale ? 260 : (isNetherite ? 200 : 140)));
            }
            ElementalGaugeManager.enforceFrozenPoseIfFrozen(target);
        }
        double radius = isFinale ? (isNetherite ? 3.5D : 2.5D) : (isNetherite ? 3.2D : 2.2D);
        float splashDmg = baseDmg * (isFinale ? 0.85F : (isNetherite ? 0.75F : 0.6F));
        float splashU = isFinale ? (isNetherite ? 2.0F : 1.5F) : 1.0F;
        for (LivingEntity splashTarget : targets(level, hit, radius, owner)) {
            if (splashTarget != target) {
                ElementalGaugeManager.applyElement(level, splashTarget, Element.CRYO, splashU, player, splashDmg);
                if (ElementalGaugeManager.getInnateElement(splashTarget) != Element.CRYO
                        && !ElementalGaugeManager.isBossEntity(splashTarget)
                        && !ElementalGaugeManager.isEntityFrozen(splashTarget)) {
                    splashTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, isNetherite ? 90 : 60, slowAmp));
                    splashTarget.setTicksFrozen(splashTarget.getTicksFrozen() + (isFinale ? 160 : 100));
                }
                ElementalGaugeManager.enforceFrozenPoseIfFrozen(splashTarget);
            }
        }

        // Cryo ice bridging (Kaeya/Ayaka effect) + Finale Permafrost ground frost
        freezeWaterNear(level, hit, isFinale ? (isNetherite ? 3 : 2) : (isNetherite ? 2 : 1));
        if (isFinale) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, hit.x, hit.y + 0.25D, hit.z, 36, radius * 0.45D, 0.25D, radius * 0.45D, 0.06D);
            level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.9F, 0.85F);
        }

        level.sendParticles(ParticleTypes.SNOWFLAKE, hit.x, hit.y, hit.z, 28, 0.55D, 0.35D, 0.55D, 0.05D);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, hit.x, hit.y, hit.z, 16, 0.45D, 0.30D, 0.45D, 0.08D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.65F, 1.6F);
    }

    public static void freezeWaterNear(ServerLevel level, Vec3 hit, int radius) {
        BlockPos center = BlockPos.containing(hit);
        boolean froze = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius + 1) {
                    BlockPos col = center.offset(dx, 0, dz);
                    BlockPos targetWater = null;

                    // 1. Search vertically near hit Y (-3 to +3) for water
                    for (int dy = 3; dy >= -3; dy--) {
                        BlockPos check = col.above(dy);
                        if (level.getFluidState(check).is(FluidTags.WATER)) {
                            targetWater = check;
                            break;
                        }
                    }

                    // 2. If water found, always ascend to the topmost surface water block
                    if (targetWater != null) {
                        while (targetWater.getY() < level.getMaxBuildHeight() && level.getFluidState(targetWater.above()).is(FluidTags.WATER)) {
                            targetWater = targetWater.above();
                        }
                        if (level.getFluidState(targetWater).isSource()) {
                            BlockState above = level.getBlockState(targetWater.above());
                            if (above.isAir() || above.canBeReplaced() || above.is(Blocks.SNOW)) {
                                level.setBlockAndUpdate(targetWater, Blocks.FROSTED_ICE.defaultBlockState());
                                level.scheduleTick(targetWater, Blocks.FROSTED_ICE, level.random.nextInt(40) + 50);
                                froze = true;
                            }
                        }
                    }
                }
            }
        }
        if (froze) {
            level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.8F, 1.4F);
            level.sendParticles(ParticleTypes.SNOWFLAKE, hit.x, hit.y + 0.2D, hit.z, 20, 0.5D, 0.1D, 0.5D, 0.02D);
        }
    }

    private static void resolveWeb(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        BlockPos pos;
        if (hitResult instanceof EntityHitResult entityHit) {
            pos = entityHit.getEntity().blockPosition();
        } else if (hitResult instanceof BlockHitResult blockHit) {
            pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            pos = BlockPos.containing(hit);
        }
        int baseDuration = StavesConfig.TEMPORARY_WEB_DURATION_TICKS.get();
        int webTicks = isNetherite ? (int) Math.round(baseDuration * 1.5D) : baseDuration;
        int poisonAmplifier = isNetherite ? 1 : 0;
        java.util.UUID webOwner = owner instanceof ServerPlayer player ? player.getUUID() : null;
        if (!TemporaryWebManager.place(level, pos, webTicks, poisonAmplifier, webOwner)) {
            TemporaryWebManager.place(level, pos.above(), webTicks, poisonAmplifier, webOwner);
        }

        LivingEntity directHit = hitEntity(hitResult);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        BlockPos hitPos = BlockPos.containing(hit);
        boolean hitWater = level.getFluidState(hitPos).is(FluidTags.WATER)
            || level.getBlockState(hitPos).is(Blocks.WATER)
            || level.getFluidState(hitPos.below()).is(FluidTags.WATER);
        if (hitWater) {
            com.velorise.staves.element.DendroCoreManager.spawn(level, hit, player, isNetherite ? 1.5F : 1.0F);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, hit.x, hit.y + 0.4D, hit.z, 20, 0.5D, 0.3D, 0.5D, 0.04D);
            level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        if (directHit != null && directHit != owner) {
            float baseDmg = 0.0F;
            float attackU = isNetherite ? 1.5F : 1.0F;
            ElementalGaugeManager.applyElement(level, directHit, Element.DENDRO, attackU, player, baseDmg);
            if (player != null && directHit instanceof net.minecraft.world.entity.monster.Spider) {
                com.velorise.staves.advancement.ModAdvancements.trigger(
                        player, com.velorise.staves.advancement.ModAdvancements.EVENT_WEB_DEVELOPER);
            }
        }
        if (player != null && !level.getEntitiesOfClass(net.minecraft.world.entity.monster.Spider.class,
                new AABB(pos).inflate(0.85D), LivingEntity::isAlive).isEmpty()) {
            com.velorise.staves.advancement.ModAdvancements.trigger(
                    player, com.velorise.staves.advancement.ModAdvancements.EVENT_WEB_DEVELOPER);
        }

        level.sendParticles(ParticleTypes.POOF, hit.x, hit.y, hit.z, 14, 0.25D, 0.25D, 0.25D, 0.02D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.7F, 1.5F);
    }

    private static void resolveOmniTrident(ServerLevel level, Vec3 hit, Entity owner, HitResult event, boolean isNetherite) {
        LivingEntity target = hitEntity(event);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        float dmg = (isNetherite ? 13.5F : 9.0F) * (float) StavesConfig.getDamageMultiplier();
        float attackU = isNetherite ? 1.5F : 1.0F;
        if (target != null && target != owner) {
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.HYDRO, attackU, player, dmg);
        }
        com.velorise.staves.element.DendroCoreManager.checkReactionsNear(level, hit, 2.5D, Element.HYDRO, player);
        com.velorise.staves.element.EnvironmentalBlockManager.applyHydroEnvironmentEffects(level, hit, hit, 1.5D);
        level.sendParticles(ParticleTypes.SPLASH, hit.x, hit.y + 0.25D, hit.z, 22, 0.35D, 0.25D, 0.35D, 0.08D);
        level.sendParticles(ParticleTypes.BUBBLE_POP, hit.x, hit.y + 0.25D, hit.z, 12, 0.25D, 0.25D, 0.25D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void resolveGuardianOrb(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        LivingEntity target = hitEntity(hitResult);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        if (target != null && target != owner) {
            float attackU = isNetherite ? 1.5F : 1.0F;
            ElementalGaugeManager.applyElement(level, target, Element.HYDRO, attackU, player, 2.0F);
        }
        com.velorise.staves.element.DendroCoreManager.checkReactionsNear(level, hit, 2.0D, Element.HYDRO, player);
        com.velorise.staves.element.EnvironmentalBlockManager.applyHydroEnvironmentEffects(level, hit, hit, 1.0D);
        level.sendParticles(ParticleTypes.SPLASH, hit.x, hit.y + 0.15D, hit.z, 18, 0.25D, 0.2D, 0.25D, 0.06D);
        level.sendParticles(ParticleTypes.BUBBLE_POP, hit.x, hit.y + 0.15D, hit.z, 8, 0.2D, 0.2D, 0.2D, 0.03D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.75F, 1.25F);
    }

    private static void resolveDaybreak(ServerLevel level, Vec3 hit, Entity owner, Projectile projectile, HitResult hitResult, boolean isNetherite) {
        boolean sideBolt = projectile.getTags().contains("velorise_daybreak_side");
        float charge01 = 1.0F;
        for (String t : projectile.getTags()) {
            if (t.startsWith("velorise_daybreak_charge:")) {
                try {
                    int pct = Integer.parseInt(t.substring("velorise_daybreak_charge:".length()));
                    charge01 = net.minecraft.util.Mth.clamp(pct / 100.0F, 0.15F, 1.0F);
                } catch (NumberFormatException ignored) {
                }
                break;
            }
        }
        float chargeScale = 0.55F + 1.25F * charge01;
        float pushScale = (charge01 >= 0.85F) ? 1.45F : (charge01 >= 0.45F ? 0.85F : 0.45F);
        boolean isSkillE = projectile.getTags().contains("velorise_daybreak_skill_e");
        resolveWindInternal(level, hit, owner, hitResult, isNetherite, ArcaneStaffItem.Kind.DAYBREAK, (sideBolt ? 0.5F : 1.0F) * chargeScale, pushScale, isSkillE);
    }

    private static void resolveWind(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite) {
        resolveWindInternal(level, hit, owner, hitResult, isNetherite, ArcaneStaffItem.Kind.WIND, 1.0F, 1.0F, false);
    }

    private static void resolveWindInternal(ServerLevel level, Vec3 hit, Entity owner, HitResult hitResult, boolean isNetherite, ArcaneStaffItem.Kind kind, float dmgScale, float pushScale, boolean isSkillE) {
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        float baseDmg = StavesConfig.getStaffBaseDamage(kind, isNetherite) * dmgScale;
        float attackU = isNetherite ? 1.5F : 1.0F;
        LivingEntity directHit = hitEntity(hitResult);
        boolean isDaybreak = (kind == ArcaneStaffItem.Kind.DAYBREAK);
        Vec3 windCenter = (isDaybreak && directHit != null) ? directHit.position() : hit;
        double radius = isDaybreak
                ? (isSkillE ? (isNetherite ? 4.2D : 3.5D) : (isNetherite ? 2.0D : 1.7D))
                : ((isNetherite ? 4.5D : 3.5D) * (isSkillE ? 1.65D : 1.0D));
        double radiusSqr = radius * radius;

        if (isSkillE) {
            com.velorise.staves.network.ModNetwork.sendElectricArc(level, windCenter, windCenter.add(0.0D, 6.8D, 0.0D), 18, 0xFF5CE6C8);
        } else {
            if (isDaybreak) {
                com.velorise.staves.network.ModNetwork.sendElectricArc(level, windCenter, windCenter.add(0.0D, 4.8D, 0.0D), 14, 0xFF5CE6C8);
            }
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, windCenter.x, windCenter.y + 0.2D, windCenter.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.GUST, windCenter.x, windCenter.y + 0.3D, windCenter.z, 2, 0.25D, 0.15D, 0.25D, 0.0D);
        }
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

        // 1. Self Wind-Jump / Knockback on the caster when shooting near themselves or at the ground (only on charged shots / Wind Staff)
        if (pushScale >= 0.80F && owner instanceof LivingEntity ownerLiving && ownerLiving.isAlive()) {
            double distSqr = ownerLiving.position().distanceToSqr(hit);
            if (distSqr <= radiusSqr) {
                double dist = Math.sqrt(distSqr);
                double falloff = Math.max(0.30D, 1.0D - (dist / (radius + 0.5D)));
                Vec3 toOwner = ownerLiving.getBoundingBox().getCenter().subtract(hit);
                Vec3 horizDir = new Vec3(toOwner.x, 0.0D, toOwner.z);
                if (horizDir.lengthSqr() > 1.0E-4D) {
                    horizDir = horizDir.normalize();
                }
                double boostScale = isNetherite ? 1.25D : 1.05D;
                double vx = horizDir.x * 0.55D * falloff * boostScale * pushScale;
                double vz = horizDir.z * 0.55D * falloff * boostScale * pushScale;
                double vy = Math.max(0.45D * pushScale, (0.95D * falloff + 0.20D) * boostScale * pushScale);
                Vec3 cur = ownerLiving.getDeltaMovement();
                ownerLiving.setDeltaMovement(cur.x * 0.35D + vx, Math.max(cur.y, vy), cur.z * 0.35D + vz);
                ownerLiving.fallDistance = 0.0F;
                ownerLiving.hurtMarked = true;
                if (ownerLiving instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                }
            }
        }
        checkEndCrystalsNear(level, hit, radius, owner);

        // 2. Direct hit target
        if (directHit != null && directHit != owner) {
            ElementalGaugeManager.applyElement(level, directHit, Element.ANEMO, attackU, player, baseDmg);
            if (player != null) {
                com.velorise.staves.advancement.ModAdvancements.trigger(
                        player, com.velorise.staves.advancement.ModAdvancements.EVENT_TAKE_MY_BALL);
            }
            if (!ElementalGaugeManager.isEntityFrozen(directHit)) {
                float targetPush = isDaybreak ? (pushScale * 0.60F) : pushScale;
                Vec3 diff = directHit.getBoundingBox().getCenter().subtract(windCenter);
                Vec3 knock = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(0, 1, 0);
                double liftY = (isDaybreak ? (isSkillE ? 0.85D : 0.72D) : 0.45D) * targetPush;
                double horizPush = isDaybreak ? 0.18D : 0.65D;
                directHit.setDeltaMovement(directHit.getDeltaMovement().add(knock.x * horizPush * targetPush, liftY, knock.z * horizPush * targetPush));
                directHit.hurtMarked = true;
                if (directHit instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                }
                if (isDaybreak && player != null) {
                    com.velorise.staves.advancement.ModAdvancements.recordVentiWindLaunch(player, directHit);
                }
            }
        }

        // 3. Splash AoE targets (cylindrical match to the rising circular wind rings for Daybreak)
        double windH = isSkillE ? 6.8D : 4.8D;
        AABB blastBox = isDaybreak
                ? new AABB(windCenter.x - radius, windCenter.y - 1.5D, windCenter.z - radius,
                           windCenter.x + radius, windCenter.y + windH, windCenter.z + radius)
                : new AABB(hit, hit).inflate(radius);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e.isAlive() && e != owner && e != directHit)) {
            double dx = nearby.getX() - windCenter.x;
            double dz = nearby.getZ() - windCenter.z;
            boolean inZone = isDaybreak
                    ? ((dx * dx + dz * dz) <= radiusSqr && nearby.getY() >= windCenter.y - 1.5D && nearby.getY() <= windCenter.y + windH)
                    : (nearby.position().distanceToSqr(hit) <= radiusSqr);
            if (inZone) {
                ElementalGaugeManager.applyElement(level, nearby, Element.ANEMO, attackU, player, baseDmg);
                if (player != null) {
                    com.velorise.staves.advancement.ModAdvancements.trigger(
                            player, com.velorise.staves.advancement.ModAdvancements.EVENT_TAKE_MY_BALL);
                }
                if (!ElementalGaugeManager.isEntityFrozen(nearby)) {
                    float targetPush = isDaybreak ? (pushScale * 0.60F) : pushScale;
                    Vec3 diff = nearby.position().subtract(windCenter);
                    Vec3 knock = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : new Vec3(0, 1, 0);
                    double liftY = (isDaybreak ? (isSkillE ? 0.78D : 0.56D) : 0.42D) * targetPush;
                    double horizPush = isDaybreak ? 0.18D : 0.65D;
                    nearby.setDeltaMovement(nearby.getDeltaMovement().add(knock.x * horizPush * targetPush, liftY, knock.z * horizPush * targetPush));
                    nearby.hurtMarked = true;
                    if (nearby instanceof ServerPlayer sp) {
                        sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                    }
                    if (isDaybreak && player != null) {
                        com.velorise.staves.advancement.ModAdvancements.recordVentiWindLaunch(player, nearby);
                    }
                }
            }
        }
        checkEndCrystalsNear(level, hit, radius, owner);
    }

    public static void resolveLightning(ServerLevel level, Vec3 hit, ServerPlayer owner, boolean isNetherite) {
        resolveLightning(level, hit, owner, isNetherite, null);
    }

    public static void resolveLightning(ServerLevel level, Vec3 hit, ServerPlayer owner, boolean isNetherite, LightningBolt bolt) {
        double radius = isNetherite ? 4.5D : 3.5D;
        float baseDmg = isNetherite ? 7.5F : 5.0F;
        float attackU = isNetherite ? 3.0F : 2.0F;
        LightningBolt effectiveBolt = bolt != null ? bolt : EntityType.LIGHTNING_BOLT.create(level);
        boolean chargeCreeper = StavesConfig.ARC_CASTER_CHARGE_CREEPER.get();
        for (LivingEntity target : targets(level, hit, radius, owner)) {
            if (target instanceof net.minecraft.world.entity.monster.Creeper creeper && chargeCreeper) {
                if (effectiveBolt != null) creeper.thunderHit(level, effectiveBolt);
                creeper.clearFire();
            }
            target.clearFire();
            ElementalGaugeManager.applyElement(level, target, Element.ELECTRO, attackU, owner, baseDmg);
            target.clearFire();
        }
        com.velorise.staves.element.DendroCoreManager.checkReactionsNear(level, hit, radius, Element.ELECTRO, owner);
        // ParticleTypes.ELECTRIC_SPARK removed
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void handlePotionSplash(ServerLevel level, ThrownPotion potion, Vec3 hitPos) {
        ItemStack stack = potion.getItem();
        List<Element> elements = ElementalGaugeManager.getElementsFromPotionStack(stack);
        if (elements.isEmpty()) return;

        Entity owner = potion.getOwner();
        ServerPlayer thrower = owner instanceof ServerPlayer sp ? sp : null;
        AABB splashBox = new AABB(hitPos, hitPos).inflate(4.0D, 2.0D, 4.0D);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, splashBox, e -> e.isAlive() && !e.isSpectator());
        for (LivingEntity living : nearby) {
            if (living.distanceToSqr(hitPos) <= 16.0D) {
                for (Element elem : elements) {
                    ElementalGaugeManager.applyElementBypassIcd(level, living, elem, 1.0F, thrower, 0.0F);
                }
            }
        }
        if (elements.contains(Element.CRYO)) {
            freezeWaterNear(level, hitPos, 2);
        }
        if (elements.contains(Element.ELECTRO)) {
            // ParticleTypes.ELECTRIC_SPARK removed
        }
    }

    private static LivingEntity hitEntity(HitResult hitResult) {
        return hitResult instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static List<LivingEntity> targets(ServerLevel level, Vec3 hit, double radius, Entity owner) {
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(hit, hit).inflate(radius),
            target -> target.isAlive() && target != owner && target.position().distanceToSqr(hit) <= radiusSqr);
    }

    private static boolean isBeyondRange(Projectile projectile, Vec3 hit) {
        double maxRange = -1.0D;
        Vec3 origin = null;
        for (String tag : projectile.getTags()) {
            if (tag.startsWith(TAG_PREFIX_RANGE)) {
                try {
                    maxRange = Double.parseDouble(tag.substring(TAG_PREFIX_RANGE.length()));
                } catch (NumberFormatException ignored) {}
            } else if (tag.startsWith(TAG_PREFIX_ORIGIN)) {
                String[] parts = tag.substring(TAG_PREFIX_ORIGIN.length()).split(":");
                if (parts.length == 3) {
                    try {
                        origin = new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (maxRange <= 0.0D || origin == null) return false;
        return origin.distanceToSqr(hit) > maxRange * maxRange;
    }
}
