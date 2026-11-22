package com.velorise.staves.element;

import static com.velorise.staves.element.ElementalGaugeManager.*;
import static com.velorise.staves.element.PlayerGenshinState.*;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Elemental Gauge Theory (EGT) system manager.
 * Tracks Gauge Units (U), Aura Decay Inheritance, Underlying Freeze & Quicken Auras,
 * Simultaneous Reaction Priority, Innate Lock, Environmental Auras, and combat feedback.
 */

public final class ZhongliManager {
    private ZhongliManager() {}

    public static final java.util.Map<UUID, PendingEvokerConstruct> PENDING_EVOKER_CONSTRUCTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class ZhongliIdleState {
        Vec3 lastStillPos = Vec3.ZERO;
        int stillTicks = 0;
        boolean idleMeteorActive = false;
        int lastSyncedCode = -1;
    }

    private static final Map<UUID, ZhongliIdleState> ZHONGLI_IDLE_STATES = new ConcurrentHashMap<>();

    private static boolean isHoldingZhongli(@Nullable Player player) {
        if (player == null) return false;
        return ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.VORTEX;
    }

    public static void tick(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            ZhongliIdleState st = ZHONGLI_IDLE_STATES.computeIfAbsent(player.getUUID(), k -> new ZhongliIdleState());
            boolean holdingZhongli = isHoldingZhongli(player);
            if (st.lastStillPos == null || st.lastStillPos.equals(Vec3.ZERO)) {
                st.lastStillPos = player.position();
            }
            double distSq = player.position().distanceToSqr(st.lastStillPos);
            boolean canIdle = holdingZhongli && player.onGround() && !player.isUsingItem() && !player.swinging && !player.isShiftKeyDown() && !player.isCrouching() && !player.isSprinting() && player.getVehicle() == null;
            if (canIdle && distSq < 0.0025D) {
                st.stillTicks++;
                if (st.stillTicks >= 200 && !st.idleMeteorActive) {
                    st.idleMeteorActive = true;
                }
            } else {
                st.stillTicks = 0;
                st.lastStillPos = player.position();
                st.idleMeteorActive = false;
            }
            int code = st.idleMeteorActive ? 4011 : 4010;
            if (code != st.lastSyncedCode || (st.idleMeteorActive && now % 20L == 0L)) {
                st.lastSyncedCode = code;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, code);
            }
        }
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            ZHONGLI_IDLE_STATES.remove(playerId);
            JADE_SHIELD_UNTIL.remove(playerId);
            JADE_SHIELD_NETHERITE.remove(playerId);
            PETRIFIED_UNTIL.remove(playerId);
            NightsFocusManager.clearPlayerState(playerId);
        }
    }

    public static void clearAll() {
        PENDING_EVOKER_CONSTRUCTS.clear();
        NightsFocusManager.clearAll();
        ZHONGLI_IDLE_STATES.clear();
    }

    private record PendingEvokerConstruct(
            ServerLevel level,
            UUID evokerId,
            long tick,
            double x,
            double y,
            double z,
            float yawDeg,
            int maxWarmup
    ) {}

    public static void setPlayerShieldAbsorption(ServerPlayer player, float targetAbsorption) {
        if (player == null) return;
        player.setAbsorptionAmount(targetAbsorption);
    }

    public static void activateJadeShield(ServerPlayer player, boolean isNetherite) {
        if (player == null || player.level().isClientSide()) return;
        long now = player.level().getGameTime();
        JADE_SHIELD_UNTIL.put(player.getUUID(), now + 400L); // 20s Jade Shield
        JADE_SHIELD_NETHERITE.put(player.getUUID(), isNetherite);
        float maxHp = Math.max(20.0F, player.getMaxHealth());
        // Base Jade Shield capacity + 20% Golden Majesty Shield Strength bonus
        float shieldHp = ((isNetherite ? (maxHp * 0.25F + 5.4167F) : (maxHp * 0.18F + 3.9F)) * 1.20F); // 9.0 HP Basic / 12.5 HP Netherite at 20 MaxHP
        if (ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.GEO)) {
            shieldHp *= 1.15F;
        }
        com.velorise.staves.effect.ModMobEffects.removeAllShields(player);
        var geoShieldObj = com.velorise.staves.effect.ModMobEffects.getShieldForElement(Element.GEO);
        if (geoShieldObj != null && geoShieldObj.isPresent()) {
            player.addEffect(new MobEffectInstance(geoShieldObj.get(), 400, isNetherite ? 1 : 0, false, false, true));
        }
        setPlayerShieldAbsorption(player, Math.max(player.getAbsorptionAmount(), shieldHp));
        CrystallizeShardManager.onShieldApplied(player, Element.GEO, 400);
    }

    public static boolean hasActiveJadeShield(@Nullable LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        Long until = JADE_SHIELD_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now >= until) {
            JADE_SHIELD_UNTIL.remove(player.getUUID());
            if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
                Element cryst = CrystallizeShardManager.getActiveShield(sp, now);
                if (cryst == null) {
                    com.velorise.staves.network.ModNetwork.sendCrystallizeShieldToPlayer(sp, null, 0);
                }
            }
            return false;
        }
        if (!player.isCreative() && player.getAbsorptionAmount() <= 0.01F) {
            JADE_SHIELD_UNTIL.remove(player.getUUID());
            if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
                Element cryst = CrystallizeShardManager.getActiveShield(sp, now);
                if (cryst == null) {
                    com.velorise.staves.network.ModNetwork.sendCrystallizeShieldToPlayer(sp, null, 0);
                }
                ((ServerLevel) sp.level()).playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 0.9F, 0.75F);
            }
            return false;
        }
        return true;
    }

    public static boolean isNearJadeShieldAura(@Nullable LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel sl)) return false;
        for (ServerPlayer sp : sl.getPlayers(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(target) <= 20.25D)) {
            if (hasActiveJadeShield(sp) && sp != target && !sp.isAlliedTo(target)) {
                return true;
            }
        }
        return false;
    }

    public static float getVortexGoldenMajestyMultiplier(@Nullable ServerPlayer attacker) {
        return 1.0F;
    }

    public static void onEvokerFangJoin(ServerLevel level, EvokerFangs fangs) {
        if (level == null || fangs == null || fangs.isRemoved()) return;
        if (fangs.getTags().contains("velorise_staves:geo_fangs")
                || fangs.getTags().contains("velorise_staves:evoker_construct_checked")) {
            return;
        }
        if (!(fangs.getOwner() instanceof net.minecraft.world.entity.monster.Evoker evoker) || !evoker.isAlive()) {
            return;
        }
        fangs.addTag("velorise_staves:evoker_construct_checked");
        int warmup = 0;
        try {
            CompoundTag nbt = new CompoundTag();
            fangs.saveWithoutId(nbt);
            warmup = nbt.getInt("Warmup");
        } catch (Exception ignored) {}
        long now = level.getGameTime();
        UUID evokerId = evoker.getUUID();
        PendingEvokerConstruct cur = PENDING_EVOKER_CONSTRUCTS.get(evokerId);
        if (cur == null || cur.tick() != now || warmup >= cur.maxWarmup()) {
            PENDING_EVOKER_CONSTRUCTS.put(evokerId, new PendingEvokerConstruct(
                    level, evokerId, now, fangs.getX(), fangs.getY(), fangs.getZ(), fangs.getYRot(), warmup
            ));
        }
    }

    public static void flushPendingEvokerConstructs(MinecraftServer server) {
        if (PENDING_EVOKER_CONSTRUCTS.isEmpty()) return;
        List<PendingEvokerConstruct> batch = new ArrayList<>(PENDING_EVOKER_CONSTRUCTS.values());
        PENDING_EVOKER_CONSTRUCTS.clear();
        for (PendingEvokerConstruct entry : batch) {
            if (entry.level() == null) continue;
            Entity ownerEnt = entry.level().getEntity(entry.evokerId());
            if (!(ownerEnt instanceof net.minecraft.world.entity.monster.Evoker evoker) || !evoker.isAlive() || evoker.isRemoved()) {
                continue;
            }
            double cx = Math.floor(entry.x()) + 0.5D;
            double cz = Math.floor(entry.z()) + 0.5D;
            double cy = entry.y();
            AABB overlapBox = new AABB(cx - 0.65D, cy - 0.5D, cz - 0.65D, cx + 0.65D, cy + 1.5D, cz + 0.65D);
            boolean alreadyPresent = !entry.level().getEntitiesOfClass(
                    com.velorise.staves.entity.GeoFangPillarEntity.class, overlapBox,
                    p -> p.isAlive() && !p.isRemoved()).isEmpty();
            if (alreadyPresent) continue;

            com.velorise.staves.entity.GeoFangPillarEntity.enforceMaxConstructsForOwner(
                    entry.level(), VeloriseStavesMod.getGeoFangPillarType(), evoker.getUUID());
            com.velorise.staves.entity.GeoFangPillarEntity construct =
                    new com.velorise.staves.entity.GeoFangPillarEntity(
                            VeloriseStavesMod.getGeoFangPillarType(),
                            entry.level(),
                            cx, cy, cz,
                            entry.yawDeg(),
                            evoker,
                            false
                    );
            entry.level().addFreshEntity(construct);
        }
    }

    static void applyPetrifyState(LivingEntity target, int petrifyTicks, long targetGameTime) {
        if (target == null || !target.isAlive() || isBossEntity(target)) return;
        UUID targetId = target.getUUID();
        long until = targetGameTime + petrifyTicks;
        Long prevFreezeUntil = FROZEN_UNTIL.get(targetId);
        FROZEN_UNTIL.put(targetId, prevFreezeUntil == null ? until : Math.max(prevFreezeUntil, until));
        Long prevPetrifyUntil = PETRIFIED_UNTIL.get(targetId);
        PETRIFIED_UNTIL.put(targetId, prevPetrifyUntil == null ? until : Math.max(prevPetrifyUntil, until));

        target.stopUsingItem();
        target.releaseUsingItem();
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = false;
        target.hurtMarked = true;

        FROZEN_POSES.computeIfAbsent(targetId, k -> new FrozenPoseState(
                target.getX(), target.getY(), target.getZ(),
                target.getYRot(), target.getXRot(), target.getYHeadRot(), target.yBodyRot
        ));

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setAggressive(false);
            mob.setNoAi(true);
            mob.addTag(TAG_FROZEN_STATUE);
        }
        enforceFrozenPose(target, targetId);

        Map<Element, GaugeState> entityGauges = GAUGES.get(targetId);
        syncAuras(target, entityGauges != null ? entityGauges.keySet() : Collections.emptySet(), null, null, null);
        com.velorise.staves.network.ModNetwork.sendStaffAnimation(target, petrifyTicks >= 95 ? 17 : 16);
    }

    /**
     * Dominus Lapidis (Hold E) Geo Mining mechanic:
     * Shatters nearby ore blocks and Amethyst clusters in a spherical shockwave around Zhongli,
     * dropping proper pickaxe loot (Diamond Pickaxe for Basic, Netherite Pickaxe + Ancient Debris for Netherite) and ore XP.
     */
    private static boolean isZhongliShatterableOre(net.minecraft.world.level.block.state.BlockState st, boolean isNetherite) {
        if (st.is(net.minecraft.tags.BlockTags.COAL_ORES)
                || st.is(net.minecraft.tags.BlockTags.IRON_ORES)
                || st.is(net.minecraft.tags.BlockTags.COPPER_ORES)
                || st.is(net.minecraft.tags.BlockTags.GOLD_ORES)
                || st.is(net.minecraft.tags.BlockTags.REDSTONE_ORES)
                || st.is(net.minecraft.tags.BlockTags.LAPIS_ORES)
                || st.is(net.minecraft.tags.BlockTags.DIAMOND_ORES)
                || st.is(net.minecraft.tags.BlockTags.EMERALD_ORES)
                || st.is(net.minecraft.world.level.block.Blocks.NETHER_QUARTZ_ORE)
                || st.is(net.minecraft.world.level.block.Blocks.NETHER_GOLD_ORE)
                || st.is(net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER)
                || st.is(net.minecraft.world.level.block.Blocks.LARGE_AMETHYST_BUD)
                || st.is(net.minecraft.world.level.block.Blocks.MEDIUM_AMETHYST_BUD)
                || st.is(net.minecraft.world.level.block.Blocks.SMALL_AMETHYST_BUD)) {
            return true;
        }
        return isNetherite && st.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS);
    }

    public static int shatterNearbyOres(ServerLevel level, ServerPlayer player, Vec3 center, boolean isNetherite) {
        return shatterNearbyOres(level, player, center, isNetherite, isNetherite ? 6.0D : 4.5D);
    }

    public static int shatterNearbyOres(ServerLevel level, ServerPlayer player, Vec3 center, boolean isNetherite, double customRadius) {
        if (level == null || player == null || center == null || !player.mayBuild()) return 0;
        double radius = Math.max(0.75D, customRadius);
        int r = (int) Math.ceil(radius);
        boolean isTapPoint = radius <= 1.25D;
        double pad = isTapPoint ? 0.35D : 0.5D;
        double r2 = (radius + pad) * (radius + pad);
        int minY = isTapPoint ? -1 : -2;
        int maxY = isTapPoint ? 1 : 3;
        net.minecraft.core.BlockPos origin = net.minecraft.core.BlockPos.containing(center);
        boolean doDrops = !player.isCreative() && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
        net.minecraft.world.item.ItemStack pickaxe = doDrops
                ? new net.minecraft.world.item.ItemStack(isNetherite ? Items.NETHERITE_PICKAXE : Items.DIAMOND_PICKAXE)
                : net.minecraft.world.item.ItemStack.EMPTY;
        if (doDrops) {
            net.minecraft.world.item.ItemStack heldStaff = player.getMainHandItem();
            if (ArcaneStaffItem.getEffectiveKind(heldStaff) != ArcaneStaffItem.Kind.VORTEX) {
                heldStaff = player.getOffhandItem();
            }
            if (ArcaneStaffItem.getEffectiveKind(heldStaff) == ArcaneStaffItem.Kind.VORTEX) {
                int fortuneLvl = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, heldStaff);
                if (fortuneLvl > 0) {
                    pickaxe.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, fortuneLvl);
                }
            }
        }
        int shattered = 0;
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-r, minY, -r), origin.offset(r, maxY, r))) {
            if (!level.hasChunkAt(p) || !level.mayInteract(player, p)) continue;
            Vec3 bCenter = Vec3.atCenterOf(p);
            if (bCenter.distanceToSqr(center) > r2) continue;
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(p);
            if (st.isAir() || !isZhongliShatterableOre(st, isNetherite)) continue;
            if (!com.velorise.staves.event.ArcaneStaffEvents.canPlayerBreakBlock(level, player, p, st)) continue;
            if (doDrops) {
                Block.dropResources(st, level, p, level.getBlockEntity(p), player, pickaxe);
                st.spawnAfterBreak(level, p, pickaxe, true);
            }
            level.destroyBlock(p, false, player);
            shattered++;
        }
        if (shattered > 0) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.9F, 0.85F);
            level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.8D, center.z, Math.min(24, shattered * 4), 1.6D, 0.8D, 1.6D, 0.08D);
        }
        return shattered;
    }

    public static void cancelIdle(ServerPlayer player) {
        if (player == null) return;
        ZhongliIdleState st = ZHONGLI_IDLE_STATES.get(player.getUUID());
        if (st != null) {
            st.stillTicks = 0;
            st.lastStillPos = player.position();
            if (st.idleMeteorActive || st.lastSyncedCode == 4011) {
                st.idleMeteorActive = false;
                st.lastSyncedCode = 4010;
                com.velorise.staves.network.ModNetwork.sendStaffAnimation(player, 4010);
            }
        }
    }
}
