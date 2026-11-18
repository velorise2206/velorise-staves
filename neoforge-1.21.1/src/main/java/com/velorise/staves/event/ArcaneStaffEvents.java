package com.velorise.staves.event;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.element.DendroCoreManager;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.item.SwordImbueHelper;
import com.velorise.staves.manager.TemporaryWebManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.projectile.EvokerFangs;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Server-side impact resolution for Arcane Staves projectiles and server tick managers. */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArcaneStaffEvents {
    public static final String TAG_KIND = "ArcaneStaffKind";
    private static final String TAG_ORIGIN_X = "ArcaneStaffOriginX";
    private static final String TAG_ORIGIN_Y = "ArcaneStaffOriginY";
    private static final String TAG_ORIGIN_Z = "ArcaneStaffOriginZ";
    private static final String TAG_MAX_RANGE = "ArcaneStaffMaxRange";
    private static final Set<Projectile> TRACKED_PROJECTILES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private ArcaneStaffEvents() {}

    public static void markRange(Projectile projectile, Vec3 origin, double maxRange) {
        projectile.getPersistentData().putDouble(TAG_ORIGIN_X, origin.x);
        projectile.getPersistentData().putDouble(TAG_ORIGIN_Y, origin.y);
        projectile.getPersistentData().putDouble(TAG_ORIGIN_Z, origin.z);
        projectile.getPersistentData().putDouble(TAG_MAX_RANGE, Math.max(1.0D, maxRange));
        TRACKED_PROJECTILES.add(projectile);
    }

    public static void trackProjectile(Projectile projectile) {
        TRACKED_PROJECTILES.add(projectile);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            ElementalGaugeManager.checkInnateMutation(living);
            com.velorise.staves.element.MobHazardAvoidanceManager.attachGoalIfEligible(living);
        }
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs && event.getLevel() instanceof ServerLevel sl) {
            ElementalGaugeManager.onEvokerFangJoin(sl, fangs);
        }
        if (event.getEntity() instanceof Snowball snowball && !event.getLevel().isClientSide()) {
            TRACKED_PROJECTILES.add(snowball);
        }
    }

    @SubscribeEvent
    public static void onLivingUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        ElementalGaugeManager.handleItemUseFinish(level, event.getEntity(), event.getItem());
    }

    @SubscribeEvent
    public static void onAnimalTame(net.neoforged.neoforge.event.entity.living.AnimalTameEvent event) {
        if (!event.getAnimal().level().isClientSide()
                && event.getTamer() instanceof ServerPlayer sp
                && ElementalGaugeManager.isOmniInnate(event.getAnimal())) {
            com.velorise.staves.advancement.ModAdvancements.trigger(
                    sp, com.velorise.staves.advancement.ModAdvancements.EVENT_TAME_OMNI_PET);
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(net.neoforged.neoforge.event.entity.living.LivingHealEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getAmount() > 0.0F) {
            event.setAmount(com.velorise.staves.element.ElementalResonanceManager.modifyIncomingHeal(event.getEntity(), event.getAmount()));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            com.velorise.staves.advancement.ModAdvancements.handleLivingDeath(event.getEntity());
            com.velorise.staves.element.ElementalDeathMessageHelper.enhanceLivingDeath(event.getEntity());
            ElementalGaugeManager.tryDropElementOnDeath(event.getEntity(), event.getSource());
            ElementalGaugeManager.clearEntityState(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(event.getEntity().getMainHandItem());
        if (com.velorise.staves.element.NeuvilletteManager.isHoldingGuardianLens(event.getEntity())
                || com.velorise.staves.element.PlungeAndGliderManager.isPlunging(event.getEntity())
                || effKind == ArcaneStaffItem.Kind.DAYBREAK
                || effKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS
                || effKind == ArcaneStaffItem.Kind.TSARITSA_BOW
                || effKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR
                || effKind == ArcaneStaffItem.Kind.DRAGONFIRE) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ArcaneStaffItem.onOmniStaffCrafted(event.getEntity(), event.getCrafting(), event.getInventory());
    }

    @SubscribeEvent
    public static void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(event.getEntity().getMainHandItem());
        if (com.velorise.staves.element.NeuvilletteManager.isHoldingGuardianLens(event.getEntity())
                || com.velorise.staves.element.PlungeAndGliderManager.isPlunging(event.getEntity())
                || effKind == ArcaneStaffItem.Kind.DAYBREAK
                || effKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS
                || effKind == ArcaneStaffItem.Kind.TSARITSA_BOW
                || effKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR
                || effKind == ArcaneStaffItem.Kind.DRAGONFIRE) {
            event.setCanceled(true);
            return;
        }
        if (!event.getEntity().level().isClientSide()) {
            ElementalGaugeManager.recordPreAttackStrength(event.getEntity(), event.getEntity().getAttackStrengthScale(0.5F));
            if (event.getEntity() instanceof ServerPlayer sp && event.getTarget() instanceof LivingEntity) {
                ElementalGaugeManager.addMavuikaNormalAttackFightingSpirit(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(net.neoforged.neoforge.event.AnvilUpdateEvent event) {
        if (SwordImbueHelper.isFireAspectElementConflict(event.getLeft(), event.getRight())) {
            event.setCanceled(true);
            return;
        }
        SwordImbueHelper.AnvilResult res = SwordImbueHelper.tryBuildAnvilResult(event.getLeft(), event.getRight(), event.getName());
        if (res != null) {
            event.setOutput(res.output());
            event.setCost(res.cost());
            event.setMaterialCost(res.materialCost());
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        SwordImbueHelper.appendImbueTooltip(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide()) {
            com.velorise.staves.element.PlungeAndGliderManager.copyPersistentState(event.getOriginal(), event.getEntity());
            if (event.getOriginal().getTags().contains("velorise_staves:received_codex")) {
                event.getEntity().addTag("velorise_staves:received_codex");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            com.velorise.staves.element.PlungeAndGliderManager.syncPlayerOnJoinOrRespawn(player);
            if (!player.getTags().contains("velorise_staves:received_codex")) {
                player.addTag("velorise_staves:received_codex");
                ItemStack codex = new ItemStack(VeloriseStavesMod.ELEMENTAL_CODEX.get());
                if (!player.getInventory().add(codex)) {
                    player.drop(codex, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            com.velorise.staves.element.PlungeAndGliderManager.syncPlayerOnJoinOrRespawn(player);
            ElementalGaugeManager.clearEntityState(player);
            com.velorise.staves.element.ElementalResonanceManager.syncPlayerImmediate(player);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)) return;

        // Splash & Lingering Potions apply 1.0U mapped element(s) to entities in splash radius
        if (projectile instanceof ThrownPotion potion) {
            handlePotionSplash(level, potion, event.getRayTraceResult().getLocation());
        }

        // Tipped Arrows apply 1.0U mapped element(s) on direct hit
        if (projectile instanceof Arrow arrow) {
            LivingEntity directHit = hitEntity(event);
            if (directHit != null) {
                ElementalGaugeManager.handleTippedArrowHit(level, arrow, directHit);
            }
        }

        String kind = projectile.getPersistentData().getString(TAG_KIND);
        if (event.getRayTraceResult() instanceof EntityHitResult ehr && ehr.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal) {
            Entity owner = projectile.getOwner();
            DamageSource ds = owner instanceof ServerPlayer sp ? level.damageSources().playerAttack(sp) : level.damageSources().magic();
            crystal.hurt(ds, 5.0F);
        }
        if (event.getRayTraceResult() instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof com.velorise.staves.entity.GeoFangPillarEntity construct) {
            event.setCanceled(true);
            TRACKED_PROJECTILES.remove(projectile);
            if (construct.handleProjectileBlocked(level, projectile, kind)) {
                projectile.discard();
            }
            return;
        }
        if (kind.isEmpty()) {
            if (projectile.isOnFire()
                    || projectile instanceof net.minecraft.world.entity.projectile.SmallFireball
                    || projectile instanceof net.minecraft.world.entity.projectile.LargeFireball) {
                Vec3 fireHit = event.getRayTraceResult().getLocation();
                com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(level, fireHit, 2.0D);
            }
            if (projectile instanceof Snowball) {
                Vec3 hit = event.getRayTraceResult().getLocation();
                Entity owner = projectile.getOwner();
                ServerPlayer thrower = owner instanceof ServerPlayer sp ? sp : null;
                LivingEntity directHit = hitEntity(event);
                if (directHit != null && directHit != owner) {
                    ElementalGaugeManager.applyElement(level, directHit, Element.CRYO, 0.5F, thrower, 0.0F);
                }
                freezeWaterNear(level, hit, 1);
            }
            return;
        }

        TRACKED_PROJECTILES.remove(projectile);
        Vec3 hit = event.getRayTraceResult().getLocation();
        if (isBeyondRange(projectile, hit)) {
            event.setCanceled(true);
            level.sendParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 8, 0.12D, 0.12D, 0.12D, 0.01D);
            projectile.discard();
            return;
        }

        Entity owner = projectile.getOwner();
        boolean isNetherite = kind.startsWith("netherite_");
        String baseKind = isNetherite ? kind.substring("netherite_".length()) : kind;

        if ("ghastfire".equals(baseKind)) {
            event.setCanceled(true);
            resolveGhastfire(level, hit, owner, event.getRayTraceResult(), isNetherite);
            projectile.discard();
            return;
        }

        event.setCanceled(true);
        com.velorise.staves.element.DragonfireCloudManager.setCurrentImpactFinale(projectile.getTags().contains("velorise_staves:finale_shot"));
        try {
        switch (baseKind) {
            case "dragonfire" -> resolveDragonfire(level, hit, owner, isNetherite);
            case "inferno" -> resolveInferno(level, hit, owner, event, isNetherite);
            case "wither" -> resolveWither(level, hit, owner, isNetherite);
            case "shulker" -> resolveShulker(level, hit, owner, event, isNetherite);
            case "frost" -> resolveFrost(level, hit, owner, event, isNetherite);
            case "web" -> resolveWeb(level, hit, owner, event, isNetherite);
            case "wind" -> resolveWind(level, hit, owner, event, isNetherite);
            case "daybreak" -> resolveDaybreak(level, hit, owner, event, isNetherite);
            case "guardian_orb" -> resolveGuardianOrb(level, hit, owner, event, isNetherite);
            case "omni_trident" -> resolveOmniTrident(level, hit, owner, event, isNetherite);
            default -> {}
        }
        } finally {
            com.velorise.staves.element.DragonfireCloudManager.setCurrentImpactFinale(false);
        }
        projectile.discard();
    }

    @SubscribeEvent
    public static void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()
                && event.getTarget() instanceof LivingEntity living
                && ElementalGaugeManager.isEntityFrozen(living)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getLevel().isClientSide()
                && event.getTarget() instanceof LivingEntity living
                && ElementalGaugeManager.isEntityFrozen(living)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInvulnerabilityCheck(net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            if (event.getSource().is(DamageTypeTags.IS_FALL)
                    && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.STALAGMITE)
                    && (ElementalGaugeManager.isAnemoInnateMob(living)
                            || com.velorise.staves.element.PlungeAndGliderManager.isPlungeFallImmune(living))) {
                living.fallDistance = 0.0F;
                event.setInvulnerable(true);
                return;
            }
            if (event.getSource().is(DamageTypeTags.IS_FIRE)
                    && (ElementalGaugeManager.isPyroInnate(living) || ElementalGaugeManager.isOmniInnate(living))) {
                ElementalGaugeManager.extinguishPyroInnate(living);
                event.setInvulnerable(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource src = event.getSource();
        if (!entity.level().isClientSide() && com.velorise.staves.element.DragonfireCloudManager.tryDeflectProjectileWithWitherCarapace(entity, src)) {
            event.setCanceled(true);
            return;
        }
        if (!entity.level().isClientSide() && (ElementalGaugeManager.isRaidenIframeActive(entity)
                || ElementalGaugeManager.isMavuikaBikeIframeActive(entity, src)
                || (src.is(DamageTypeTags.IS_FALL) && com.velorise.staves.element.PlungeAndGliderManager.isPlungeFallImmune(entity)))) {
            if (src.is(DamageTypeTags.IS_FALL)) entity.fallDistance = 0.0F;
            event.setCanceled(true);
            return;
        }
        if (!entity.level().isClientSide()
                && !(src.getDirectEntity() instanceof EvokerFangs f && f.getTags().contains("velorise_staves:geo_fangs"))
                && com.velorise.staves.entity.GeoFangPillarEntity.interceptIncomingDamage(entity, src, event.getAmount())) {
            event.setCanceled(true);
            return;
        }
        if (!entity.level().isClientSide()
                && ElementalGaugeManager.isOmniInnate(entity)
                && ElementalGaugeManager.isElementalOrMagicDamage(src)) {
            if (entity.level() instanceof ServerLevel sl) {
                ElementalGaugeManager.notifyOmniImmuneEnvironmental(sl, entity);
            }
            event.setCanceled(true);
            return;
        }
        if (!entity.level().isClientSide()
                && src.getDirectEntity() instanceof EvokerFangs fangs
                && fangs.getTags().contains("velorise_staves:geo_fangs")) {
            entity.invulnerableTime = 0;
            if (entity.level() instanceof ServerLevel sl) {
                boolean isNetherite = fangs.getTags().contains("velorise_staves:netherite_geo_fangs");
                float baseDmg = StavesConfig.getStaffBaseDamage(ArcaneStaffItem.Kind.EVOKER, isNetherite);
                float attackU = isNetherite ? 1.5F : 1.0F;
                ServerPlayer attacker = src.getEntity() instanceof ServerPlayer sp ? sp : null;
                ElementalGaugeManager.applyElement(sl, entity, Element.GEO, attackU, attacker, baseDmg);
            }
            event.setCanceled(true);
            return;
        }
        if (!entity.level().isClientSide()
                && event.getSource().is(DamageTypeTags.IS_FIRE)
                && ElementalGaugeManager.isPyroInnate(entity)) {
            ElementalGaugeManager.extinguishPyroInnate(entity);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && entity instanceof LivingEntity living) {
            ElementalGaugeManager.tickEntityFreezeGuard(living);
            if ((living.getRemainingFireTicks() > 0 || living.isOnFire())
                    && (ElementalGaugeManager.isPyroInnate(living) || ElementalGaugeManager.isOmniInnate(living))) {
                ElementalGaugeManager.extinguishPyroInnate(living);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        ElementalGaugeManager.beginDamageFloatTracking(entity);
        long now = entity.level().getGameTime();
        DamageSource src = event.getSource();

        // 0B. Environmental Fire Reaction Amplification (Forward Melt on Stray in sunlight, Reverse Vaporize)
        event.setNewDamage(ElementalGaugeManager.modifyIncomingEnvironmentalDamage(entity, src, event.getNewDamage()));

        // 1. Crystallize Elemental Shield (250% absorption against matching element)
        if (entity instanceof ServerPlayer sp) {
            Element activeShield = com.velorise.staves.element.CrystallizeShardManager.getActiveShield(sp, now);
            if (activeShield != null && com.velorise.staves.element.CrystallizeShardManager.matchesElementalShield(activeShield, src)) {
                event.setNewDamage(event.getNewDamage() * 0.4F);
                sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.9F, 1.8F);
            }
        }

        // 1B. Imbued Sword elemental hit (full-strength swing >= 0.9F applies 1U + scales physical damage on reactions)
        event.setNewDamage(ElementalGaugeManager.applyImbuedSwordHit(entity, src, event.getNewDamage()));
        event.setNewDamage(ElementalGaugeManager.applyRaidenMusouIsshinHit(entity, src, event.getNewDamage()));

        // 2. Base Mob Resistances (10% Elemental/Physical, Iron Golem 50% Physical, Warden 70% Anemo) & Superconduct Physical RES Shred (-40%)
        event.setNewDamage(ElementalGaugeManager.applyMobDamageResistances(entity, src, event.getNewDamage()));

        // 3. Elemental Resonance: Geo Shield DMG, Cryo CRIT on physical hits, Protective Canopy DMG reduction
        float resDmg = com.velorise.staves.element.ElementalResonanceManager.modifyPhysicalAttackDamage(entity, src, event.getNewDamage());
        resDmg = com.velorise.staves.element.ElementalResonanceManager.modifyIncomingDefenseDamage(entity, src, resDmg);
        event.setNewDamage(resDmg);
    }

    @SubscribeEvent
    public static void onLivingDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        ElementalGaugeManager.trySendPhysicalDamageFloat(entity, event.getSource(), event.getNewDamage());
        ElementalGaugeManager.tryInterruptGuardianChanneling(entity, event.getNewDamage());
        ElementalGaugeManager.handleLivingIncomingDamage(entity, event.getSource());
        ElementalGaugeManager.triggerRaidenEyeCoordinatedStrike(entity, event.getSource());
        ElementalGaugeManager.enforceFrozenPoseIfFrozen(entity);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TemporaryWebManager.tick(event.getServer());
        ElementalGaugeManager.tick(event.getServer());
        tickProjectiles();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TemporaryWebManager.clearAll(event.getServer());
        ElementalGaugeManager.clearAll();
        ArcaneStaffItem.clearAll();
        TRACKED_PROJECTILES.clear();
    }

    private static void tickProjectiles() {
        synchronized (TRACKED_PROJECTILES) {
            Iterator<Projectile> it = TRACKED_PROJECTILES.iterator();
            while (it.hasNext()) {
                Projectile projectile = it.next();
                if (projectile == null || projectile.isRemoved() || !(projectile.level() instanceof ServerLevel level)) {
                    it.remove();
                    continue;
                }
                if (isBeyondRange(projectile, projectile.position())) {
                    level.sendParticles(ParticleTypes.SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.12D, 0.12D, 0.12D, 0.01D);
                    projectile.discard();
                    it.remove();
                    continue;
                }
                String kindTag = projectile.getPersistentData().getString(TAG_KIND);

                // Check Cryo water surface freezing for plain Snowball and Frostbolt wand
                boolean isSnowball = projectile instanceof Snowball && kindTag.isEmpty();
                boolean isFrostStaff = kindTag.endsWith("frost");
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
                        int radius = (isFrostStaff && kindTag.startsWith("netherite_")) ? 2 : 1;
                        freezeWaterNear(level, freezeLoc, radius);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, freezeLoc.x, freezeLoc.y + 0.15D, freezeLoc.z, 18, 0.35D, 0.1D, 0.35D, 0.03D);
                        level.playSound(null, freezeLoc.x, freezeLoc.y, freezeLoc.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.5F);
                        projectile.discard();
                        it.remove();
                        continue;
                    }
                }
                if (projectile instanceof net.minecraft.world.entity.projectile.ShulkerBullet bullet
                        && ("shulker".equals(kindTag) || "netherite_shulker".equals(kindTag))
                        && bullet.getPersistentData().hasUUID("VeloriseShulkerTarget")) {
                    java.util.UUID targetId = bullet.getPersistentData().getUUID("VeloriseShulkerTarget");
                    Entity targetEnt = level.getEntity(targetId);
                    if (targetEnt instanceof LivingEntity target && target.isAlive() && !target.isSpectator()) {
                        double speed = bullet.getTags().contains("velorise_staves:finale_shot") ? 0.85D : 0.68D;
                        Vec3 bPos = bullet.position();
                        Vec3 aimPos = target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(0.35D));
                        BlockHitResult los = level.clip(new ClipContext(bPos, aimPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
                        Vec3 steerTarget = (los.getType() == HitResult.Type.MISS)
                                ? aimPos
                                : findNextAStarWaypoint(level, bullet.blockPosition(), BlockPos.containing(aimPos), 160);
                        Vec3 dir = steerTarget.subtract(bPos);
                        if (dir.lengthSqr() > 1.0E-5D) {
                            bullet.setDeltaMovement(dir.normalize().scale(speed));
                            bullet.hurtMarked = true;
                        }
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
        DendroCoreManager.checkReactionsNear(level, hit, radius, Element.ELECTRO, player);

        // Immediately electrify water beneath/around impact pos and register 1s expanding ring manager
        BlockPos hitPos = BlockPos.containing(hit);
        com.velorise.staves.element.ElectrifiedWaterManager.electrify(level, hitPos, radius + 1.0D, 120, player, 1.0F);
        com.velorise.staves.element.DragonfireCloudManager.register(cloud, level, player, isNetherite, radius);

        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.15F);
    }

    private static void resolveInferno(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
        LivingEntity target = hitEntity(event);
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
        } else if (event.getRayTraceResult() instanceof BlockHitResult bhr) {
            com.velorise.staves.element.EnvironmentalBlockManager.igniteSingleHitBlock(level, bhr.getBlockPos(), bhr.getDirection());
        }
        meltIceNear(level, hit, isNetherite ? 2 : 1);
        checkEndCrystalsNear(level, hit, isNetherite ? 2.5D : 1.8D, owner);
        DendroCoreManager.checkReactionsNear(level, hit, 3.0D, Element.PYRO, player);
        com.velorise.staves.element.EnvironmentalBlockManager.checkPyroAmethystMining(level, hit, isNetherite ? 2.5D : 2.0D);
        level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, isNetherite ? 35 : 25, 0.3D, 0.3D, 0.3D, 0.04D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void resolveGhastfire(ServerLevel level, Vec3 hit, Entity owner, net.minecraft.world.phys.HitResult hitResult, boolean isNetherite) {
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
        DendroCoreManager.checkReactionsNear(level, hit, radius, Element.PYRO, player);
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

    private static void resolveShulker(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
        LivingEntity target = hitEntity(event);
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
        DendroCoreManager.checkReactionsNear(level, hit, 3.5D, Element.ELECTRO, player);
        level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 18, 0.55D, 0.35D, 0.55D, 0.06D);
        level.sendParticles(ParticleTypes.PORTAL, hit.x, hit.y, hit.z, 20, 0.65D, 0.45D, 0.65D, 0.08D);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 0.85F, 1.0F);
    }

    private static void resolveFrost(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
        LivingEntity target = hitEntity(event);
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

    private static void resolveWeb(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
        BlockPos pos;
        if (event.getRayTraceResult() instanceof EntityHitResult entityHit) {
            pos = entityHit.getEntity().blockPosition();
        } else if (event.getRayTraceResult() instanceof BlockHitResult blockHit) {
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

        LivingEntity directHit = hitEntity(event);
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        BlockPos hitPos = BlockPos.containing(hit);
        boolean hitWater = level.getFluidState(hitPos).is(FluidTags.WATER)
            || level.getBlockState(hitPos).is(Blocks.WATER)
            || level.getFluidState(hitPos.below()).is(FluidTags.WATER);
        if (hitWater) {
            DendroCoreManager.spawn(level, hit, player, isNetherite ? 1.5F : 1.0F);
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

    private static void resolveOmniTrident(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
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

    private static void resolveGuardianOrb(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
        LivingEntity target = hitEntity(event);
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

    private static void resolveDaybreak(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
        boolean sideBolt = event.getProjectile().getPersistentData().getBoolean("VeloriseDaybreakSideBolt");
        float charge01 = event.getProjectile().getPersistentData().getFloat("VeloriseDaybreakCharge");
        if (charge01 <= 0.0F) charge01 = 1.0F;
        float tensionMult = 0.55F + 1.25F * net.minecraft.util.Mth.clamp(charge01, 0.15F, 1.0F);
        float pushScale = (charge01 >= 0.85F) ? 1.45F : (charge01 >= 0.45F ? 0.85F : 0.45F);
        boolean isSkillE = event.getProjectile().getTags().contains("velorise_daybreak_skill_e") || event.getProjectile().getPersistentData().getBoolean("VeloriseDaybreakSkillE");
        resolveWindInternal(level, hit, owner, event, isNetherite, ArcaneStaffItem.Kind.DAYBREAK, (sideBolt ? 0.5F : 1.0F) * tensionMult, pushScale, isSkillE);
    }

    private static void resolveWind(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite) {
        resolveWindInternal(level, hit, owner, event, isNetherite, ArcaneStaffItem.Kind.WIND, 1.0F, 1.0F, false);
    }

    private static void resolveWindInternal(ServerLevel level, Vec3 hit, Entity owner, ProjectileImpactEvent event, boolean isNetherite, ArcaneStaffItem.Kind kind, float dmgScale, float pushScale, boolean isSkillE) {
        ServerPlayer player = owner instanceof ServerPlayer sp ? sp : null;
        float baseDmg = StavesConfig.getStaffBaseDamage(kind, isNetherite) * dmgScale;
        float attackU = isNetherite ? 1.5F : 1.0F;
        LivingEntity directHit = hitEntity(event);
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
        DendroCoreManager.checkReactionsNear(level, hit, radius, Element.ELECTRO, owner);
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

    private static LivingEntity hitEntity(ProjectileImpactEvent event) {
        return event.getRayTraceResult() instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static List<LivingEntity> targets(ServerLevel level, Vec3 hit, double radius, Entity owner) {
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(hit, hit).inflate(radius),
            target -> target.isAlive() && target != owner && target.position().distanceToSqr(hit) <= radiusSqr);
    }

    private static boolean isBeyondRange(Projectile projectile, Vec3 hit) {
        double maxRange = projectile.getPersistentData().getDouble(TAG_MAX_RANGE);
        if (maxRange <= 0.0D) return false;
        Vec3 origin = new Vec3(
            projectile.getPersistentData().getDouble(TAG_ORIGIN_X),
            projectile.getPersistentData().getDouble(TAG_ORIGIN_Y),
            projectile.getPersistentData().getDouble(TAG_ORIGIN_Z));
        return origin.distanceToSqr(hit) > maxRange * maxRange;
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer player) {
            ElementalGaugeManager.syncEntityOnStartTracking(player, living);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ElementalGaugeManager.clearEntityState(player);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel sl
                && event.getExplosion().getDirectSourceEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper) {
            ElementalGaugeManager.handleCreeperExplosionBurst(sl, creeper, event.getExplosion().center(), event.getExplosion().radius());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            com.velorise.staves.element.PlayerGenshinState.onPlayerChangedDimension(player);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && (!event.getEntity().isAlive() || event.getEntity().isRemoved())) {
            com.velorise.staves.element.PlayerGenshinState.onEntityRemoved(event.getEntity());
        }
    }
}
