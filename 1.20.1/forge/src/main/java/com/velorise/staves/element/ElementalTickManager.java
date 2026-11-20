package com.velorise.staves.element;
import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages central server-side elemental ticks, ambient aura particles,
 * and global entity/world state cleanup.
 */
public final class ElementalTickManager {

    private ElementalTickManager() {}

    public static void spawnAmbientParticles(ServerLevel level, LivingEntity target, Map<Element, GaugeState> gauges, long now) {
        if (!canEmitReactionVisual(level, target.position(), 6)) return;
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.65D;
        double z = target.getZ();

        if (gauges.containsKey(Element.PYRO)) {
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.2D, 0.25D, 0.2D, 0.01D);
        }
        if (gauges.containsKey(Element.HYDRO)) {
            level.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 1, 0.2D, 0.25D, 0.2D, 0.0D);
        }
        if (gauges.containsKey(Element.CRYO) || FROZEN_GAUGES.containsKey(target.getUUID())) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0.25D, 0.25D, 0.25D, 0.01D);
        }
        if (gauges.containsKey(Element.ELECTRO)) {
            // ParticleTypes.ELECTRIC_SPARK removed
        }
        if (gauges.containsKey(Element.DENDRO) || QUICKEN_GAUGES.containsKey(target.getUUID())) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 1, 0.2D, 0.25D, 0.2D, 0.01D);
        }
    }

    public static void spawnExpandingSquareParticles(
            ServerLevel level,
            Vec3 center,
            double halfExtent,
            double yOffset,
            ParticleOptions primary,
            @Nullable ParticleOptions accent,
            int accentStride
    ) {
        if (halfExtent <= 0.05D) return;
        int steps = Math.max(3, (int) Math.round(halfExtent * 3.5D));
        double y = center.y + yOffset;
        double[][] edges = {
                {-1.0D, -1.0D,  1.0D,  0.0D},
                { 1.0D, -1.0D,  0.0D,  1.0D},
                { 1.0D,  1.0D, -1.0D,  0.0D},
                {-1.0D,  1.0D,  0.0D, -1.0D}
        };
        int idx = 0;
        for (double[] edge : edges) {
            for (int i = 0; i < steps; i++) {
                double frac = (2.0D * i) / steps;
                double px = center.x + (edge[0] + edge[2] * frac) * halfExtent;
                double pz = center.z + (edge[1] + edge[3] * frac) * halfExtent;
                level.sendParticles(primary, px, y, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (accent != null && accentStride > 0 && idx % accentStride == 0) {
                    level.sendParticles(accent, px, y + 0.05D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                idx++;
            }
        }
    }

    public static void tickActivePlayersAndEnvironment(MinecraftServer server, long serverNow) {
        // 7. Environmental water, fire, lingering potion clouds & cryo checks on active online players
        tickNightsFocusProjectiles();
        tickMavuikaSunAura(server, serverNow);
        tickRaidenState(server, serverNow);
        ArchonUltiManager.tick(server, serverNow);
        DragonfireCloudManager.tickPlayers(server, serverNow);
        Set<UUID> visitedNearby = new HashSet<>();
        Set<UUID> visitedClouds = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isDeadOrDying()) {
                clearEntityState(player);
                continue;
            }
            updatePlayerHpTracking(player, player.level().getGameTime());
            if (!player.isShiftKeyDown() && (com.velorise.staves.entity.HydroSalonSummonEntity.hasActiveSummon(player) || isChannelingGuardianLens(player))) {
                applyFurinaWaterWalking(player);
            }
            if (hasActiveJadeShield(player) && player.level() instanceof ServerLevel jsl && serverNow % 10 == 0) {
                jsl.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.0D, player.getZ(), 3, 0.35D, 0.45D, 0.35D, 0.01D);
            }
            tickNightsFocusShield(player, serverNow);
            ElementalResonanceManager.tickPlayer(player);
            if (!player.isSpectator()) {
                if (isEntityInWater(player)) {
                    handleEnvironmentalWater(player);
                } else if (isTouchingEnvironmentalFireBlock(player) || isTouchingSoulCampfire(player)) {
                    maintainBurningPyroAura(player);
                }
                Collection<Element> playerElements = getEntityElements(player.getUUID());
                updateCryoDebuff(player, player.getUUID(), playerElements.contains(Element.CRYO) && !isEntityFrozen(player), null);

                if (player.level() instanceof ServerLevel sl) {
                    if (serverNow % 5 == 0) {
                        tryApplyMushroomContactAura(sl, player, sl.getGameTime());
                    }
                    AABB searchBox = player.getBoundingBox().inflate(32.0D);

                    // Lingering Potion AreaEffectCloud elemental application (every 20 ticks = 1.0s)
                    if (serverNow % 20 == 0) {
                        for (AreaEffectCloud cloud : sl.getEntitiesOfClass(AreaEffectCloud.class, searchBox, e -> e.isAlive() && !e.isRemoved())) {
                            if (!visitedClouds.add(cloud.getUUID())) {
                                continue;
                            }
                            if (cloud.getTags().contains("velorise_staves:dragonfire_cloud")
                                    || cloud.getTags().contains("velorise_staves:ghastfire_field")) {
                                continue;
                            }
                            List<Element> cloudElements = getElementsFromAreaEffectCloud(sl, cloud);
                            if (!cloudElements.isEmpty()) {
                                ServerPlayer cloudOwner = cloud.getOwner() instanceof ServerPlayer sp ? sp : null;
                                for (LivingEntity inCloud : sl.getEntitiesOfClass(LivingEntity.class, cloud.getBoundingBox(),
                                        e -> e.isAlive() && !e.isRemoved() && !e.isSpectator())) {
                                    for (Element elem : cloudElements) {
                                        applyElementBypassIcd(sl, inCloud, elem, 1.0F, cloudOwner, 0.0F);
                                    }
                                }
                            }
                        }
                    }

                    for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !e.isRemoved())) {
                        if (!visitedNearby.add(nearby.getUUID())) {
                            continue;
                        }
                        if (nearby instanceof net.minecraft.world.entity.npc.WanderingTrader trader) {
                            InnateMobManager.checkWanderingTraderProgress(sl, trader);
                        }
                        if (nearby instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE) && !isEntityFrozen(mob)) {
                            mob.setNoAi(false);
                            mob.removeTag(TAG_FROZEN_STATUE);
                        }
                        if (serverNow % 20 == 0) {
                            updateCoopMobScaling(sl, nearby);
                        }
                        EnumSet<Element> nearbyInnates = getInnateElements(nearby);
                        if (nearbyInnates.contains(Element.CRYO) || hasElement(nearby, Element.CRYO)) {
                            checkCryoWaterFreezing(nearby);
                        }
                        if (!nearbyInnates.isEmpty() && serverNow % 20 == 0) {
                            if (isEntityInWater(nearby)) {
                                handleEnvironmentalWater(nearby);
                            } else if (isTouchingEnvironmentalFireBlock(nearby) || isTouchingSoulCampfire(nearby)) {
                                maintainBurningPyroAura(nearby);
                            }
                        }
                        if (serverNow % 5 == 0 && !isPassiveAquaticOrAmbient(nearby)) {
                            tryApplyMushroomContactAura(sl, nearby, sl.getGameTime());
                        }
                        if (isOmniInnate(nearby)) {
                            OMNI_INNATE_ENTITIES.add(nearby.getUUID());
                            extinguishPyroInnate(nearby);
                            if (serverNow % 20 == 0 && nearby instanceof net.minecraft.world.entity.TamableAnimal ta
                                    && ta.isTame() && player.getUUID().equals(ta.getOwnerUUID())) {
                                com.velorise.staves.advancement.ModAdvancements.trigger(
                                        player, com.velorise.staves.advancement.ModAdvancements.EVENT_TAME_OMNI_PET);
                            }
                            if (serverNow % 40 == 0) {
                                com.velorise.staves.network.ModNetwork.sendAuraSync(nearby, Collections.emptyList(), null, null, null, 0.0F, -2);
                            }
                            continue;
                        }
                        // Ensure Powered Creeper (including dual-element Charged Mutated) and ALL Mutated Mobs stay registered in GAUGES
                        if (!nearbyInnates.isEmpty()
                                && ((nearby instanceof Creeper creeper && creeper.isPowered()) || isMutatedInnate(nearby))) {
                            Map<Element, GaugeState> gauges = GAUGES.computeIfAbsent(nearby.getUUID(), k -> new ConcurrentHashMap<>());
                            boolean added = false;
                            for (Element keepElem : nearbyInnates) {
                                if (!gauges.containsKey(keepElem)) {
                                    gauges.put(keepElem, new GaugeState(keepElem, 1.25F));
                                    added = true;
                                }
                            }
                            if (added || serverNow % 40 == 0) {
                                syncAuras(nearby, gauges.keySet(), null, null, null);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void clearEntityStateInternal(@Nullable LivingEntity entity) {
        if (entity == null) return;
        UUID id = entity.getUUID();
        PlayerGenshinState.remove(id);
        GAUGES.remove(id);
        FROZEN_GAUGES.remove(id);
        QUICKEN_GAUGES.remove(id);
        FROZEN_UNTIL.remove(id);
        FROZEN_POSES.remove(id);
        LAST_FREEZE_STRUGGLE.remove(id);
        QUICKEN_UNTIL.remove(id);
        BURNING_UNTIL.remove(id);
        BURNING_EM_LEVEL.remove(id);
        BURNING_ATTACKER_UUID.remove(id);
        ELECTRO_CHARGE_EM_LEVEL.remove(id);
        SUPPRESSED_PHYSICAL_FLOAT_TICK.remove(id);
        ElementalDeathMessageHelper.clearEntityState(entity);
        LAST_BURN_PYRO_REFRESH.remove(id);
        INNATE_COOLDOWN_UNTIL.remove(id);
        ELECTRO_CHARGE_TICK.remove(id);
        LAST_BLOOM_SPAWN.remove(id);
        SUPERCONDUCT_UNTIL.remove(id);
        OMNI_INNATE_ENTITIES.remove(id);
        LAST_OMNI_ENV_IMMUNE.remove(id);
        LAST_MUSHROOM_CONTACT.remove(id);
        EXPLODED_CREEPERS.remove(id);
        ZhongliManager.PENDING_EVOKER_CONSTRUCTS.remove(id);
        PLAYER_LAST_HP.remove(id);
        PRE_ATTACK_STRENGTH.remove(id);
        PLAYER_HP_CHANGE_BUFF_UNTIL.remove(id);
        DRACONIC_GLORIES_STACKS.remove(id);
        FURINA_FANFARE_UNTIL.remove(id);
        FURINA_FANFARE_POINTS.remove(id);
        FURINA_FANFARE_NETHERITE.remove(id);
        JADE_SHIELD_UNTIL.remove(id);
        JADE_SHIELD_NETHERITE.remove(id);
        NIGHTS_FOCUS_UNTIL.remove(id);
        NIGHTS_FOCUS_NETHERITE.remove(id);
        NIGHTS_FOCUS_STARS.remove(id);
        NIGHTS_FOCUS_LAST_AUTO_STAR.remove(id);
        NIGHTS_FOCUS_LAST_SKILL_STAR.remove(id);
        NIGHTS_FOCUS_BASE_HP.remove(id);
        NIGHTS_FOCUS_PENDING_SHOTS.remove(id);
        LAYLA_DREAMSPHERE_UNTIL.remove(id);
        LAYLA_DREAMSPHERE_NETHERITE.remove(id);
        LAYLA_DREAMSPHERE_NEXT_SHOT.remove(id);
        MavuikaManager.clearPlayerState(id);
        if (!entity.level().isClientSide() && entity.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
            entity.stopRiding();
            bike.discard();
        }
        RAIDEN_EYE_UNTIL.remove(id);
        RAIDEN_EYE_NETHERITE.remove(id);
        RAIDEN_EYE_NEXT_PROC.remove(id);
        RAIDEN_RESOLVE.remove(id);
        RAIDEN_MUSOU_UNTIL.remove(id);
        RAIDEN_MUSOU_NETHERITE.remove(id);
        RAIDEN_SKILL_CD_UNTIL.remove(id);
        RAIDEN_BURST_CD_UNTIL.remove(id);
        RAIDEN_IFRAME_UNTIL.remove(id);
        RAIDEN_NA_COMBO.remove(id);
        RAIDEN_LAST_MELEE_TICK.remove(id);
        RAIDEN_NEXT_SWING_TICK.remove(id);
        RAIDEN_PENDING_HITOTACHI_TICK.remove(id);
        RAIDEN_PENDING_HITOTACHI_MULT.remove(id);
        ArchonUltiManager.clearEntityState(id);
        NahidaManager.clearPlayerState(id);
        TsaritsaManager.clearPlayer(id);
        XilonenManager.clearPlayerState(id);
        NeuvilletteManager.clearPlayerState(id);
        PlungeAndGliderManager.clearPlayerState(id);
        FurinaManager.clearPlayerState(id);
        ZhongliManager.clearPlayerState(id);
        RaidenManager.clearPlayerState(id);
        VentiManager.clearPlayerState(id);
        DragonfireCloudManager.clearPlayerState(id);
        InnateMobManager.clearEntity(id);
        PETRIFIED_UNTIL.remove(id);
        LAST_ELEMENTAL_ARMOR_WEAR.remove(id);
        EnvironmentalBlockManager.clearPlayerState(id);
        com.velorise.staves.entity.HydroSalonSummonEntity.clearPlayerState(id);
        com.velorise.staves.entity.OmniOrbEntity.removeCycleEntry(id);
        com.velorise.staves.item.ArcaneStaffItem.clearPlayerState(id);
        MobHazardAvoidanceManager.clearEntityState(entity);
        if (entity instanceof Mob mob && mob.getTags().contains(TAG_FROZEN_STATUE)) {
            mob.setNoAi(false);
            mob.removeTag(TAG_FROZEN_STATUE);
        }
        if (!entity.level().isClientSide()) {
            if (entity instanceof ServerPlayer sp) {
                XilonenManager.clearPlayerState(sp);
                NahidaManager.clearPlayerState(sp);
                CrystallizeShardManager.clearPlayerShield(sp);
                ElementalResonanceManager.clearPlayerState(sp.getUUID());
            }
            com.velorise.staves.network.ModNetwork.sendAuraSync(entity, Collections.emptyList(), null, null, null, 0.0F, 0);
        }
    }

    public static void clearAllInternal() {
        PlayerGenshinState.clearAll();
        NahidaManager.clearAll();
        TsaritsaManager.clearAll();
        GAUGES.clear();
        FROZEN_GAUGES.clear();
        QUICKEN_GAUGES.clear();
        FROZEN_UNTIL.clear();
        FROZEN_POSES.clear();
        LAST_FREEZE_STRUGGLE.clear();
        QUICKEN_UNTIL.clear();
        BURNING_UNTIL.clear();
        BURNING_EM_LEVEL.clear();
        ELECTRO_CHARGE_EM_LEVEL.clear();
        LAST_BURN_PYRO_REFRESH.clear();
        INNATE_COOLDOWN_UNTIL.clear();
        ELECTRO_CHARGE_TICK.clear();
        LAST_PROXIMITY_SHOCK.clear();
        LAST_BLOOM_SPAWN.clear();
        SUPERCONDUCT_UNTIL.clear();
        OMNI_INNATE_ENTITIES.clear();
        LAST_OMNI_ENV_IMMUNE.clear();
        LAST_MUSHROOM_CONTACT.clear();
        PLAYER_LAST_HP.clear();
        PRE_ATTACK_STRENGTH.clear();
        PLAYER_HP_CHANGE_BUFF_UNTIL.clear();
        DRACONIC_GLORIES_STACKS.clear();
        FURINA_FANFARE_UNTIL.clear();
        FURINA_FANFARE_POINTS.clear();
        FURINA_FANFARE_NETHERITE.clear();
        JADE_SHIELD_UNTIL.clear();
        JADE_SHIELD_NETHERITE.clear();
        NIGHTS_FOCUS_UNTIL.clear();
        NIGHTS_FOCUS_NETHERITE.clear();
        NIGHTS_FOCUS_STARS.clear();
        NIGHTS_FOCUS_LAST_AUTO_STAR.clear();
        NIGHTS_FOCUS_LAST_SKILL_STAR.clear();
        NIGHTS_FOCUS_BASE_HP.clear();
        NIGHTS_FOCUS_PENDING_SHOTS.clear();
        LAYLA_DREAMSPHERE_UNTIL.clear();
        LAYLA_DREAMSPHERE_NETHERITE.clear();
        LAYLA_DREAMSPHERE_NEXT_SHOT.clear();
        MAVUIKA_AURA_UNTIL.clear();
        MAVUIKA_AURA_NETHERITE.clear();
        MAVUIKA_AURA_NEXT_STRIKE.clear();
        MAVUIKA_NIGHTSOUL_POINTS.clear();
        MAVUIKA_NIGHTSOUL_FUEL.clear();
        MavuikaManager.clearAll();
        MAVUIKA_CRUCIBLE_UNTIL.clear();
        MAVUIKA_CRUCIBLE_POINTS.clear();
        MAVUIKA_SKILL_COOLDOWN_UNTIL.clear();
        MAVUIKA_ULTI_COOLDOWN_UNTIL.clear();
        RAIDEN_EYE_UNTIL.clear();
        RAIDEN_EYE_NETHERITE.clear();
        RAIDEN_EYE_NEXT_PROC.clear();
        RAIDEN_RESOLVE.clear();
        RAIDEN_MUSOU_UNTIL.clear();
        RAIDEN_MUSOU_NETHERITE.clear();
        RAIDEN_SKILL_CD_UNTIL.clear();
        RAIDEN_BURST_CD_UNTIL.clear();
        RAIDEN_IFRAME_UNTIL.clear();
        RAIDEN_NA_COMBO.clear();
        RAIDEN_LAST_MELEE_TICK.clear();
        RAIDEN_NEXT_SWING_TICK.clear();
        RAIDEN_PENDING_HITOTACHI_TICK.clear();
        RAIDEN_PENDING_HITOTACHI_MULT.clear();
        ArchonUltiManager.clearAll();
        XilonenManager.clearAll();
        PlungeAndGliderManager.clearAll();
        NeuvilletteManager.clearAll();
        PETRIFIED_UNTIL.clear();
        LAST_ELEMENTAL_ARMOR_WEAR.clear();
        ICD_TRACKER.clear();
        REACTION_ICD_TRACKER.clear();
        LAST_THORNS_REFLECT.clear();
        DendroCoreManager.clearAll();
        CrystallizeShardManager.clearAll();
        ElectrifiedWaterManager.clearAll();
        DragonfireCloudManager.clearAll();
        GhastfireFieldManager.clearAll();
        EnvironmentalBlockManager.clearAll();
        ElementalResonanceManager.clearAll();
        MobHazardAvoidanceManager.clearAll();
        ElementalDeathMessageHelper.clearAll();
        BURNING_ATTACKER_UUID.clear();
        SUPPRESSED_PHYSICAL_FLOAT_TICK.clear();
        EXPLODED_CREEPERS.clear();
        InnateMobManager.clearAll();
        ZhongliManager.clearAll();
        VentiManager.clearAll();
        FurinaManager.clearAll();
        RaidenManager.clearAll();
        com.velorise.staves.entity.HydroSalonSummonEntity.clearAll();
        com.velorise.staves.item.ArcaneStaffItem.clearAll();
        com.velorise.staves.advancement.ModAdvancements.clearAll();
    }

    public static void tickActiveEntityGauges(MinecraftServer server, long serverNow) {
        Set<UUID> activeEntityIds = new HashSet<>(GAUGES.keySet());
        activeEntityIds.addAll(QUICKEN_GAUGES.keySet());
        activeEntityIds.addAll(FROZEN_GAUGES.keySet());
        activeEntityIds.addAll(FROZEN_UNTIL.keySet());
        activeEntityIds.addAll(PETRIFIED_UNTIL.keySet());

        for (UUID id : activeEntityIds) {
            LivingEntity target = null;
            ServerLevel targetLevel = null;

            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(id);
            if (onlinePlayer != null && onlinePlayer.isAlive() && !onlinePlayer.isRemoved()) {
                target = onlinePlayer;
                targetLevel = (ServerLevel) onlinePlayer.level();
            } else {
                for (ServerLevel lvl : server.getAllLevels()) {
                    Entity e = lvl.getEntity(id);
                    if (e instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                        target = living;
                        targetLevel = lvl;
                        break;
                    }
                }
            }

            if (target == null || targetLevel == null) {
                PlayerGenshinState.remove(id);
                GAUGES.remove(id);
                FROZEN_GAUGES.remove(id);
                QUICKEN_GAUGES.remove(id);
                FROZEN_UNTIL.remove(id);
                PETRIFIED_UNTIL.remove(id);
                FROZEN_POSES.remove(id);
                LAST_FREEZE_STRUGGLE.remove(id);
                QUICKEN_UNTIL.remove(id);
                BURNING_UNTIL.remove(id);
                BURNING_EM_LEVEL.remove(id);
                BURNING_ATTACKER_UUID.remove(id);
                ELECTRO_CHARGE_EM_LEVEL.remove(id);
                SUPPRESSED_PHYSICAL_FLOAT_TICK.remove(id);
                LAST_BURN_PYRO_REFRESH.remove(id);
                INNATE_COOLDOWN_UNTIL.remove(id);
                ELECTRO_CHARGE_TICK.remove(id);
                LAST_BLOOM_SPAWN.remove(id);
                SUPERCONDUCT_UNTIL.remove(id);
        OMNI_INNATE_ENTITIES.remove(id);
        LAST_OMNI_ENV_IMMUNE.remove(id);
        LAST_MUSHROOM_CONTACT.remove(id);
        EXPLODED_CREEPERS.remove(id);
        ZhongliManager.PENDING_EVOKER_CONSTRUCTS.remove(id);
        PLAYER_LAST_HP.remove(id);
        PLAYER_HP_CHANGE_BUFF_UNTIL.remove(id);
        DRACONIC_GLORIES_STACKS.remove(id);
                continue;
            }

            long now = targetLevel.getGameTime();
            Map<Element, GaugeState> entityGauges = GAUGES.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            EnumSet<Element> innateSet = getInnateElements(target);
            boolean changed = false;

            if (innateSet.contains(Element.PYRO)) {
                extinguishPyroInnate(target);
            }

            // 1. Environmental Water / Fire check & Cryo Water Freezing
            if (innateSet.contains(Element.CRYO) || entityGauges.containsKey(Element.CRYO)) {
                checkCryoWaterFreezing(target);
            }
            if (isEntityInWater(target)) {
                handleEnvironmentalWater(target);
                if (entityGauges.isEmpty() && !QUICKEN_GAUGES.containsKey(id) && !FROZEN_GAUGES.containsKey(id)) {
                    GAUGES.remove(id);
                    continue;
                }
            } else if (isTouchingEnvironmentalFireBlock(target)) {
                maintainBurningPyroAura(target);
            }

            // Powered Creeper (special lightning creature): radiates electric shock within 1.0 block in air & water once per second
            if (target instanceof Creeper creeper && creeper.isPowered()) {
                if (now % 20 == 0) {
                    final LivingEntity electroSource = target;
                    AABB auraField = electroSource.getBoundingBox().inflate(1.0D);
                    for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, auraField,
                            e -> e.isAlive() && !e.isRemoved() && e != electroSource && !e.isSpectator()
                                    && !(e instanceof Player p && p.isCreative())
                                    && !hasInnateElement(e, Element.ELECTRO))) {
                        drawVoltaicArc(targetLevel, electroSource, nearby);
                        applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 1.0F);
                    }

                    if (isEntitySubmergedInWater(electroSource)) {
                        ElectrifiedWaterManager.electrify(targetLevel, electroSource.blockPosition(), 1.0D, 30, null, 1.0F);
                    }
                }
            } else if (innateSet.contains(Element.ELECTRO) && isMutatedInnate(target)) {
                // Mutated Electro mob: does NOT radiate Electro into the air; only electrifies the 1 single water block it touches
                if (now % 20 == 0 && isEntitySubmergedInWater(target)) {
                    ElectrifiedWaterManager.electrifySingleBlock(targetLevel, target.blockPosition(), 30, null, 1.0F);
                }
            }

            // 2. Tick gauges decay
            Iterator<Map.Entry<Element, GaugeState>> gaugeIt = entityGauges.entrySet().iterator();
            while (gaugeIt.hasNext()) {
                Map.Entry<Element, GaugeState> gEntry = gaugeIt.next();
                if (innateSet.contains(gEntry.getKey())) {
                    continue; // Innate lock: never decay naturally
                }
                if (gEntry.getKey() == Element.PYRO && (isTouchingEnvironmentalFireBlock(target) || BURNING_UNTIL.containsKey(id))) {
                    continue;
                }
                if (gEntry.getKey() == Element.HYDRO && isEntityInWater(target)) {
                    continue;
                }
                GaugeState state = gEntry.getValue();
                state.tick();
                if (state.isExpired()) {
                    gaugeIt.remove();
                    changed = true;
                }
            }

            // Tick independent Quicken gauge
            GaugeState qg = QUICKEN_GAUGES.get(id);
            Long quickenEnd = QUICKEN_UNTIL.get(id);
            if (qg != null) {
                qg.tick();
                if (qg.isExpired() || (quickenEnd != null && now >= quickenEnd)) {
                    clearQuickenState(id);
                    changed = true;
                }
            } else if (quickenEnd != null && now >= quickenEnd) {
                clearQuickenState(id);
                changed = true;
            }

            // Tick independent Freeze gauge (Genshin Statue Freeze: lock AI, position, and rotation)
            GaugeState fg = FROZEN_GAUGES.get(id);
            Long frozenEnd = FROZEN_UNTIL.get(id);
            Long petrifiedEnd = PETRIFIED_UNTIL.get(id);
            if (petrifiedEnd != null && now >= petrifiedEnd) {
                PETRIFIED_UNTIL.remove(id);
                petrifiedEnd = null;
                changed = true;
            }
            if (isBossEntity(target)) {
                if (fg != null || frozenEnd != null || petrifiedEnd != null) {
                    clearFreezeState(target, id);
                    changed = true;
                }
            } else if (fg != null) {
                fg.tick();
                if (fg.isExpired() || (frozenEnd != null && now >= frozenEnd)) {
                    if (petrifiedEnd != null && now < petrifiedEnd) {
                        FROZEN_GAUGES.remove(id);
                        changed = true;
                        enforceFrozenPose(target, id);
                    } else {
                        clearFreezeState(target, id);
                        changed = true;
                    }
                } else {
                    enforceFrozenPose(target, id);
                }
            } else if (frozenEnd != null) {
                if (now >= frozenEnd) {
                    clearFreezeState(target, id);
                    changed = true;
                } else {
                    enforceFrozenPose(target, id);
                }
            } else if (petrifiedEnd != null) {
                PETRIFIED_UNTIL.remove(id);
                changed = true;
            }

            // 2B. Proximity Voltaic Arcing (1.0.0 behavior: ONLY when BOTH entities are physically submerged in water)
            if (entityGauges.containsKey(Element.ELECTRO) && isEntitySubmergedInWater(target) && (now % 10 == 0)) {
                final LivingEntity srcTarget = target;
                AABB arcBox = target.getBoundingBox().inflate(4.0D);
                for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, arcBox, e -> e.isAlive() && !e.isRemoved() && e != srcTarget)) {
                    if (isEntitySubmergedInWater(nearby)) {
                        long pairKey = (((long) Math.min(target.getId(), nearby.getId())) << 32) | ((long) Math.max(target.getId(), nearby.getId()) & 0xFFFFFFFFL);
                        Long lastShock = LAST_PROXIMITY_SHOCK.get(pairKey);
                        if (lastShock == null || now < lastShock || (now - lastShock) >= 20) {
                            LAST_PROXIMITY_SHOCK.put(pairKey, now);
                            drawVoltaicArc(targetLevel, target, nearby);
                            targetLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
                            applyElement(targetLevel, nearby, Element.ELECTRO, 1.0F, null, 1.0F);
                            if (ElementalGaugeManager.getInnateElement(target) != Element.ELECTRO) {
                                float nearEcDmg = applyMobElementalResistance(target, Element.ELECTRO, 1.0F);
                                hurtWithPreAppliedElementalRes(target, ElementalDeathMessageHelper.createDamageSource(targetLevel, null, ElementalReaction.ELECTRO_CHARGED, Element.ELECTRO), nearEcDmg);
                                syncAuras(target, entityGauges.keySet(), Element.ELECTRO, Element.HYDRO, ElementalReaction.ELECTRO_CHARGED, nearEcDmg);
                            }
                            break;
                        }
                    }
                }
            }

            // 3. Electro-Charged periodic damage (1.0.0 behavior: every 20 ticks = 1 second, 0.5 heart = 1.0F damage, consuming 0.4U from both Hydro and Electro)
            if (entityGauges.containsKey(Element.HYDRO) && entityGauges.containsKey(Element.ELECTRO)
                    && !(innateSet.contains(Element.HYDRO) && innateSet.contains(Element.ELECTRO))) {
                int counter = ELECTRO_CHARGE_TICK.getOrDefault(id, 0) + 1;
                GaugeState hydroState = entityGauges.get(Element.HYDRO);
                GaugeState electroState = entityGauges.get(Element.ELECTRO);
                // Genshin EC premature final tick: if either gauge is about to expire via decay and >= 0.5s (10 ticks) has passed since last tick
                boolean prematureFinalTick = counter >= 10 && (
                        (hydroState != null && hydroState.getRemainingTicks() <= 1) ||
                        (electroState != null && electroState.getRemainingTicks() <= 1)
                );
                if (counter >= 20 || prematureFinalTick) {
                    counter = 0;
                    int ecEmLvl = ELECTRO_CHARGE_EM_LEVEL.getOrDefault(id, 0);
                    float ecDmg = applyMobElementalResistance(target, Element.ELECTRO,
                            1.0F * getTransformativeMultiplier(ecEmLvl));
                    if (target instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, targetLevel.getGameTime()) == Element.ELECTRO) {
                        ecDmg *= 0.4F;
                    }
                    if (!innateSet.contains(Element.ELECTRO)) {
                        hurtWithPreAppliedElementalRes(target, ElementalDeathMessageHelper.createDamageSource(targetLevel, null, ElementalReaction.ELECTRO_CHARGED, Element.ELECTRO), ecDmg);
                        syncAuras(target, entityGauges.keySet(), Element.HYDRO, Element.ELECTRO, ElementalReaction.ELECTRO_CHARGED, ecDmg);
                    }

                    if (isEntitySubmergedInWater(target)) {
                        final LivingEntity srcTarget = target;
                        AABB chainBox = target.getBoundingBox().inflate(4.0D);
                        boolean chained = false;
                        for (LivingEntity nearby : targetLevel.getEntitiesOfClass(LivingEntity.class, chainBox, e -> e.isAlive() && e != srcTarget)) {
                            if (isEntitySubmergedInWater(nearby)) {
                                drawVoltaicArc(targetLevel, target, nearby);
                                applyElement(targetLevel, nearby, Element.ELECTRO, 0.5F, null, 1.0F);
                                chained = true;
                            }
                        }
                        if (!chained) {
                            ElectrifiedWaterManager.drawBodyInternalArc(targetLevel, target);
                        }
                    } else {
                        ElectrifiedWaterManager.drawBodyInternalArc(targetLevel, target);
                    }

                    if (hydroState != null && !innateSet.contains(Element.HYDRO)) {
                        hydroState.consume(0.4F);
                        if (hydroState.isExpired()) {
                            entityGauges.remove(Element.HYDRO);
                            changed = true;
                        }
                    }
                    if (electroState != null && !innateSet.contains(Element.ELECTRO)) {
                        electroState.consume(0.4F);
                        if (electroState.isExpired()) {
                            entityGauges.remove(Element.ELECTRO);
                            changed = true;
                        }
                    }
                }
                ELECTRO_CHARGE_TICK.put(id, counter);
            } else {
                ELECTRO_CHARGE_TICK.remove(id);
            }

            // 4. Burning periodic damage (Genshin standard: ticks every 0.25s = 5 ticks, consumes 0.4U/s Dendro/Quicken = 0.1U per 5 ticks,
            //    and continues until all Dendro & Quicken gauges are completely depleted!)
            if (BURNING_UNTIL.containsKey(id)) {
                boolean hasDendroFuel = entityGauges.containsKey(Element.DENDRO) || QUICKEN_GAUGES.containsKey(id) || innateSet.contains(Element.DENDRO);
                boolean hasPyro = entityGauges.containsKey(Element.PYRO) || innateSet.contains(Element.PYRO);
                if (!hasDendroFuel || !hasPyro || target.isInWaterOrRain() || target.fireImmune() || target instanceof Warden) {
                    BURNING_UNTIL.remove(id);
                    BURNING_EM_LEVEL.remove(id);
                    BURNING_ATTACKER_UUID.remove(id);
                    LAST_BURN_PYRO_REFRESH.remove(id);
                    if (innateSet.contains(Element.DENDRO)) {
                        entityGauges.remove(Element.PYRO);
                        if (!target.isInLava()) {
                            target.clearFire();
                        }
                    }
                    changed = true;
                } else if (now % 5 == 0) {
                    int burnEmLvl = BURNING_EM_LEVEL.getOrDefault(id, 0);
                    float burnDmg = applyMobElementalResistance(target, Element.PYRO,
                            0.75F * getTransformativeMultiplier(burnEmLvl)); // 0.75 per 0.25s = 3.0 DMG/s scaled by EM
                    if (target instanceof ServerPlayer sp && CrystallizeShardManager.getActiveShield(sp, now) == Element.PYRO) {
                        burnDmg *= 0.4F;
                    }
                    UUID burnCreditId = BURNING_ATTACKER_UUID.get(id);
                    Entity burnCreditEnt = burnCreditId != null ? targetLevel.getEntity(burnCreditId) : null;
                    LivingEntity burnCredit = burnCreditEnt instanceof LivingEntity livingCredit ? livingCredit : null;
                    IS_PROCESSING_REACTION.set(true);
                    try {
                        hurtWithPreAppliedElementalRes(target, ElementalDeathMessageHelper.createTypedDamageSource(
                                targetLevel.damageSources().inFire(), burnCredit, ElementalReaction.BURNING, Element.PYRO), burnDmg);
                    } finally {
                        IS_PROCESSING_REACTION.set(false);
                    }
                    if (!target.isAlive() && burnDmg > 0.0F) {
                        syncAuras(target, Collections.emptyList(), Element.DENDRO, Element.PYRO, ElementalReaction.BURNING, burnDmg);
                    } else if (target.isAlive() && burnDmg > 0.0F) {
                        MobHazardAvoidanceManager.reportReactionOrHazard(target, null, target.position(), Element.DENDRO, Element.PYRO);
                    }
                    BURNING_UNTIL.put(id, now + 200L);
                    target.setSecondsOnFire(2);

                    // Consume 0.1U per 5 ticks (= 0.4U/s) from Dendro and Quicken gauges
                    GaugeState dendroState = entityGauges.get(Element.DENDRO);
                    if (dendroState != null && !innateSet.contains(Element.DENDRO)) {
                        dendroState.consume(0.1F);
                        if (dendroState.isExpired()) {
                            entityGauges.remove(Element.DENDRO);
                            changed = true;
                        }
                    }
                    GaugeState quickenState = QUICKEN_GAUGES.get(id);
                    if (quickenState != null) {
                        quickenState.consume(0.1F);
                        if (quickenState.isExpired()) {
                            clearQuickenState(id);
                            changed = true;
                        }
                    }

                    // Re-apply 1U Pyro aura every 2.0s (40 ticks) per Genshin Burning self-refresh ICD
                    Long lastPyroRefresh = LAST_BURN_PYRO_REFRESH.get(id);
                    if ((innateSet.isEmpty() || innateSet.contains(Element.DENDRO) || innateSet.contains(Element.GEO)) && (lastPyroRefresh == null || now < lastPyroRefresh || (now - lastPyroRefresh) >= 40L)) {
                        LAST_BURN_PYRO_REFRESH.put(id, now);
                        if (entityGauges.containsKey(Element.PYRO)) {
                            entityGauges.get(Element.PYRO).refresh(1.0F);
                        } else {
                            entityGauges.put(Element.PYRO, new GaugeState(Element.PYRO, 1.0F));
                        }
                    }

                    if (!entityGauges.containsKey(Element.DENDRO) && !QUICKEN_GAUGES.containsKey(id) && !innateSet.contains(Element.DENDRO)) {
                        BURNING_UNTIL.remove(id);
                        BURNING_EM_LEVEL.remove(id);
                        LAST_BURN_PYRO_REFRESH.remove(id);
                        changed = true;
                    }
                    if (now % 10 == 0 && canEmitReactionVisual(targetLevel, target.position(), 4)) {
                        targetLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8D, target.getZ(), 2, 0.25D, 0.3D, 0.25D, 0.02D);
                    }
                }
            }

            // 5. Cryo Aura passive debuff (-15% Movement Speed, -15% Attack Speed) when not Statue-Frozen
            boolean hasUnfrozenCryo = entityGauges.containsKey(Element.CRYO) && !isEntityFrozen(target);
            Element cryoInnateFlag = innateSet.contains(Element.CRYO) ? Element.CRYO : null;
            updateCryoDebuff(target, id, hasUnfrozenCryo, cryoInnateFlag);

            if (entityGauges.isEmpty() && !QUICKEN_GAUGES.containsKey(id) && !FROZEN_GAUGES.containsKey(id)) {
                updateCryoDebuff(target, id, false, cryoInnateFlag);
                GAUGES.remove(id);
            }

            if (changed) {
                syncAuras(target, entityGauges.keySet(), null, null, null);
            }

            // 6. Ambient aura particles (every 10 ticks)
            if ((!entityGauges.isEmpty() || QUICKEN_GAUGES.containsKey(id) || FROZEN_GAUGES.containsKey(id)) && now % 10 == 0 && StavesConfig.SHOW_ELEMENTAL_PARTICLES.get()) {
                spawnAmbientParticles(targetLevel, target, entityGauges, now);
            }
        }

    }
}
