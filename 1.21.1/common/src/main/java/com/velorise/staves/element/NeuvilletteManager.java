package com.velorise.staves.element;

import com.velorise.staves.config.StavesConfig;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import java.util.ArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Neuvillette's reworked Guardian Lens kit:
 * - Blocks all normal melee attacks and block breaking while holding Guardian Lens (or Omni Staff locked to Hydro with Guardian Lens).
 * - Hold Left-Click (Charged Attack: Equitable Judgment): No cooldown! Charges for 3.0s (60 ticks),
 *   absorbing up to 3 Sourcewater Droplets within 12m (-1.0s / 20 ticks charge time & +16% Max HP heal per droplet;
 *   3 droplets = 0.0s instant beam!). Once charged, channels the 3.0s (60 ticks) HP-scaled Hydro beam.
 * - Right-Click (Skill E: O Tears, I Shall Repay): 12.0s CD. Summons a surging Hydro waterfall blast in front of the player,
 *   spawning 3 Sourcewater Droplets ONLY when hitting at least 1 enemy.
 * - [R] Key (Burst Q: O Tides, I Have Returned): 18.0s CD. 1.7s (34 ticks) of I-frames + burst animation,
 *   unleashes massive AoE Hydro waves + 2 follow-up waterfall torrents, and ALWAYS spawns 6 Sourcewater Droplets immediately.
 */
public final class NeuvilletteManager {
    public static final class NeuvilletteState {
        public boolean leftClickHeld = false;
        public boolean netherite = false;
        public int chargeTicks = 0;
        public int absorbedDroplets = 0;
        public boolean firingBeam = false;
        public int beamElapsedTicks = 0;
        public long ultiCooldownUntilTick = 0L;
        public long iframeUntilTick = 0L;
        public long followUpWave1Tick = 0L;
        public long followUpWave2Tick = 0L;
        public Vec3 burstCenter = Vec3.ZERO;
        public boolean burstNetherite = false;
        public int lastSyncedCode = -1;
    }

    private static final Map<UUID, NeuvilletteState> STATES = new ConcurrentHashMap<>();

    private NeuvilletteManager() {}

    public static void clearAll() {
        STATES.clear();
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) STATES.remove(playerId);
    }

    public static boolean isHoldingGuardianLens(@Nullable Player player) {
        if (player == null) return false;
        return ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.GUARDIAN;
    }

    public static boolean isChannelingOrFiring(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            com.velorise.staves.client.ClientAuraTracker.AuraDisplayData d =
                    com.velorise.staves.client.ClientAuraTracker.get(player.getId());
            return d != null && System.currentTimeMillis() < d.neuvilletteActiveUntil;
        }
        NeuvilletteState st = STATES.get(player.getUUID());
        return st != null && (st.leftClickHeld || st.firingBeam);
    }

    public static boolean isNeuvilletteIframeActive(@Nullable LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return false;
        NeuvilletteState st = STATES.get(player.getUUID());
        return st != null && player.level().getGameTime() < st.iframeUntilTick;
    }

    public static void handleLeftClickHold(ServerPlayer player, boolean held) {
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        NeuvilletteState st = STATES.computeIfAbsent(player.getUUID(), k -> new NeuvilletteState());
        if (!held) {
            if (st.leftClickHeld || st.firingBeam) {
                stopChanneling(player, st);
            }
            return;
        }
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return;
        ItemStack stack = getHeldGuardianLens(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof ArcaneStaffItem)) return;

        if (!st.leftClickHeld && !st.firingBeam) {
            st.leftClickHeld = true;
            st.netherite = ArcaneStaffItem.getEffectiveNetherite(stack);
            st.chargeTicks = 0;
            st.absorbedDroplets = 0;
            st.firingBeam = false;
            st.beamElapsedTicks = 0;
            if (player.level() instanceof ServerLevel sl) {
                int absorbed = ElementalGaugeManager.consumeSourcewaterDropletsForChannel(sl, player, 12.0D, 3);
                st.absorbedDroplets = absorbed;
                sl.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.85F, 1.15F);
                if (absorbed >= 3) {
                    st.firingBeam = true;
                    st.beamElapsedTicks = 0;
                    sl.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.75F, 1.35F);
                }
            }
            syncStateToClients(player, st, true);
        }
    }

    public static void tryInterruptChanneling(LivingEntity victim, float damageDealt) {
        if (damageDealt <= 0.0F || !(victim instanceof ServerPlayer player)) return;
        if (player.getAbsorptionAmount() > 0.0F) return;
        NeuvilletteState st = STATES.get(player.getUUID());
        if (st == null || (!st.leftClickHeld && !st.firingBeam)) return;
        if (st.netherite) return; // C1 Netherite: 100% interruption immunity while charging/firing!
        stopChanneling(player, st);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.55F, 1.25F);
    }

    private static void stopChanneling(ServerPlayer player, NeuvilletteState st) {
        st.leftClickHeld = false;
        st.firingBeam = false;
        st.chargeTicks = 0;
        st.absorbedDroplets = 0;
        st.beamElapsedTicks = 0;
        syncStateToClients(player, st, true);
    }

    public static ItemStack getHeldGuardianLens(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.GUARDIAN) {
            return main;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Right-Click Skill E (O Tears, I Shall Repay): CD 12.0s.
     * Summons a surging Hydro waterfall at the aimed enemy or point ahead (range 16m, AoE 3.5m / 4.5m).
     * Spawns 3 Sourcewater Droplets ONLY if at least 1 enemy is hit!
     */
    public static boolean castSkillE(ServerPlayer player, ItemStack stack, boolean isNetherite) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player)) return false;
        NeuvilletteState st = STATES.get(player.getUUID());
        if (st != null && (st.leftClickHeld || st.firingBeam)) {
            stopChanneling(player, st);
        }
        long now = level.getGameTime();
        double range = 16.0D;
        double aoeRadius = isNetherite ? 4.5D : 3.5D;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 targetCenter = null;

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity cand : candidates) {
            Vec3 toCand = cand.getBoundingBox().getCenter().subtract(eye);
            double proj = toCand.dot(look);
            if (proj < 0.5D || proj > range) continue;
            Vec3 closest = eye.add(look.scale(proj));
            double perp = closest.distanceTo(cand.getBoundingBox().getCenter());
            if (perp <= 1.8D + cand.getBbWidth() * 0.5D && proj < bestDist) {
                bestDist = proj;
                targetCenter = cand.position();
            }
        }
        if (targetCenter == null) {
            HitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            targetCenter = hit.getLocation();
        }

        for (int y = 0; y <= 5; y++) {
            double py = targetCenter.y + y * 0.75D;
            level.sendParticles(ParticleTypes.SPLASH, targetCenter.x, py, targetCenter.z, 14, aoeRadius * 0.35D, 0.25D, aoeRadius * 0.35D, 0.08D);
            level.sendParticles(ParticleTypes.BUBBLE_POP, targetCenter.x, py, targetCenter.z, 8, aoeRadius * 0.30D, 0.20D, aoeRadius * 0.30D, 0.04D);
        }
        level.sendParticles(ParticleTypes.CLOUD, targetCenter.x, targetCenter.y + 0.2D, targetCenter.z, 16, aoeRadius * 0.4D, 0.15D, aoeRadius * 0.4D, 0.03D);
        level.playSound(null, BlockPos.containing(targetCenter), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0F, 1.10F);
        level.playSound(null, BlockPos.containing(targetCenter), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.55F, 1.45F);

        float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.GUARDIAN, isNetherite);
        float skillDmg = (1.5F + player.getMaxHealth() * 0.05F) * scale;
        float gaugeU = isNetherite ? 1.5F : 1.0F;

        AABB aoeBox = new AABB(
                targetCenter.x - aoeRadius, targetCenter.y - 1.5D, targetCenter.z - aoeRadius,
                targetCenter.x + aoeRadius, targetCenter.y + 3.5D, targetCenter.z + aoeRadius
        );
        List<LivingEntity> hitEnemies = level.getEntitiesOfClass(LivingEntity.class, aoeBox,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity enemy : hitEnemies) {
            enemy.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, enemy, Element.HYDRO, gaugeU, player, skillDmg);
        }

        if (!hitEnemies.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ElementalGaugeManager.spawnSourcewaterDroplet(level, targetCenter, player.getUUID(), now);
            }
        }

        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_NEUVILLETTE_SKILL_E);
        return true;
    }

    /**
     * [R] Key Burst Q (O Tides, I Have Returned): CD 18.0s.
     * Grants 1.7s (34 ticks) of I-frames, triggers Burst animation, deals crashing AoE Hydro damage
     * + 2 follow-up waterfall waves, and ALWAYS spawns 6 Sourcewater Droplets immediately.
     */
    public static boolean triggerNeuvilletteUlti(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        if (ElementalResonanceManager.checkAndNotifyOverloaded(player) || ElementalResonanceManager.isOnSwapCooldown(player)) return false;
        ItemStack stack = getHeldGuardianLens(player);
        if (stack.isEmpty()) return false;

        long now = level.getGameTime();
        NeuvilletteState st = STATES.computeIfAbsent(player.getUUID(), k -> new NeuvilletteState());
        if (st.leftClickHeld || st.firingBeam) {
            stopChanneling(player, st);
        }
        if (now < st.ultiCooldownUntilTick) {
            ArchonUltiManager.sendMinimalistBurstCooldownAlert(player, Element.HYDRO, st.ultiCooldownUntilTick - now);
            return false;
        }

        boolean isNeth = ArcaneStaffItem.getEffectiveNetherite(stack);
        int baseCdTicks = 360; // 18.0s
        int adjustedCd = ElementalResonanceManager.hasResonance(player, ElementalResonanceManager.ResonanceType.ANEMO)
                ? Math.max(20, (int) Math.round(baseCdTicks * 0.90D))
                : baseCdTicks;
        st.ultiCooldownUntilTick = now + adjustedCd;
        st.iframeUntilTick = now + 38L; // 1.8s (36 ticks) cast animation + root
        player.invulnerableTime = Math.max(player.invulnerableTime, 38);
        ArchonUltiManager.applyBurstRoot(player, 36);
        st.burstCenter = player.position().add(player.getLookAngle().multiply(3.5D, 0.0D, 3.5D));
        st.burstNetherite = isNeth;
        st.followUpWave1Tick = now + 34L + 10L;
        st.followUpWave2Tick = now + 34L + 20L;

        ModNetwork.sendStaffAnimation(player, com.velorise.staves.client.ClientAuraTracker.ANIM_NEUVILLETTE_BURST);
        ArchonUltiManager.scheduleBurstAction(player, 34, () -> {
            if (!player.isAlive() || !(player.level() instanceof ServerLevel sl)) return;
            long eruptNow = sl.getGameTime();
            Vec3 dropCenter = player.position().add(player.getLookAngle().multiply(2.8D, 0.0D, 2.8D));
            for (int i = 0; i < 6; i++) {
                ElementalGaugeManager.spawnSourcewaterDroplet(sl, dropCenter, player.getUUID(), eruptNow);
            }
            float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.GUARDIAN, isNeth);
            float initialDmg = (2.5F + player.getMaxHealth() * 0.08F) * scale;
            float gaugeU = isNeth ? 3.0F : 2.0F;
            triggerBurstHydroWave(sl, player, st.burstCenter, isNeth ? 9.0D : 7.0D, initialDmg, gaugeU);
        });
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.95F, 1.20F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0F, 0.85F);
        return true;
    }

    private static void triggerBurstHydroWave(ServerLevel level, ServerPlayer player, Vec3 center, double radius, float dmg, float gaugeU) {
        AABB box = new AABB(
                center.x - radius, center.y - 2.0D, center.z - radius,
                center.x + radius, center.y + 4.5D, center.z + radius
        );
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity enemy : enemies) {
            enemy.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, enemy, Element.HYDRO, gaugeU, player, dmg);
        }
        for (int i = 0; i < 28; i++) {
            double ang = (Math.PI * 2.0D * i) / 28.0D;
            double rx = center.x + Math.cos(ang) * (radius * 0.65D);
            double rz = center.z + Math.sin(ang) * (radius * 0.65D);
            level.sendParticles(ParticleTypes.SPLASH, rx, center.y + 0.5D, rz, 6, 0.25D, 0.8D, 0.25D, 0.08D);
            level.sendParticles(ParticleTypes.BUBBLE_POP, rx, center.y + 1.2D, rz, 3, 0.2D, 0.6D, 0.2D, 0.04D);
        }
        EnvironmentalBlockManager.applyHydroEnvironmentEffects(level, player.position(), center, radius * 0.5D);
        level.playSound(null, BlockPos.containing(center), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.90F, 1.05F);
    }

    public static void tick(MinecraftServer server, long now) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            NeuvilletteState st = STATES.get(player.getUUID());
            if (st == null) continue;
            ServerLevel level = player.serverLevel();

            if (st.followUpWave1Tick > 0L && now >= st.followUpWave1Tick) {
                st.followUpWave1Tick = 0L;
                float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.GUARDIAN, st.burstNetherite);
                float waveDmg = (1.0F + player.getMaxHealth() * 0.035F) * scale;
                triggerBurstHydroWave(level, player, st.burstCenter, st.burstNetherite ? 9.0D : 7.0D, waveDmg, st.burstNetherite ? 1.5F : 1.0F);
            }
            if (st.followUpWave2Tick > 0L && now >= st.followUpWave2Tick) {
                st.followUpWave2Tick = 0L;
                float scale = StavesConfig.getStaffDamageScale(ArcaneStaffItem.Kind.GUARDIAN, st.burstNetherite);
                float waveDmg = (1.0F + player.getMaxHealth() * 0.035F) * scale;
                triggerBurstHydroWave(level, player, st.burstCenter, st.burstNetherite ? 9.0D : 7.0D, waveDmg, st.burstNetherite ? 1.5F : 1.0F);
            }

            if (st.leftClickHeld || st.firingBeam) {
                ItemStack stack = getHeldGuardianLens(player);
                if (stack.isEmpty() || ElementalResonanceManager.isOverloaded(player)) {
                    stopChanneling(player, st);
                    continue;
                }
                st.netherite = ArcaneStaffItem.getEffectiveNetherite(stack);
                if (!player.isShiftKeyDown()) {
                    ElementalGaugeManager.applyFurinaWaterWalking(player);
                }

                if (!st.firingBeam) {
                    if (st.absorbedDroplets < 3) {
                        int newlyAbsorbed = ElementalGaugeManager.consumeSourcewaterDropletsForChannel(level, player, 12.0D, 3 - st.absorbedDroplets);
                        st.absorbedDroplets += newlyAbsorbed;
                    }
                    st.chargeTicks++;
                    int requiredTicks = Math.max(0, 60 - st.absorbedDroplets * 20);
                    if (st.chargeTicks >= requiredTicks) {
                        st.firingBeam = true;
                        st.beamElapsedTicks = 0;
                        level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.75F, 1.35F);
                    } else if (st.chargeTicks % 6 == 0) {
                        level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 0.70F, 0.95F + st.chargeTicks * 0.01F);
                    }
                    syncStateToClients(player, st, false);
                } else {
                    int beamElapsed = st.beamElapsedTicks++;
                    if (beamElapsed >= 60 || !st.leftClickHeld) {
                        stopChanneling(player, st);
                        continue;
                    }
                    if (beamElapsed % 10 == 0 && !player.isCreative()) {
                        float maxHp = Math.max(1.0F, player.getMaxHealth());
                        float halfHp = maxHp * 0.50F;
                        if (player.getHealth() > halfHp) {
                            float drainAmount = maxHp * 0.08F;
                            float newHp = Math.max(halfHp, player.getHealth() - drainAmount);
                            float actualDrained = player.getHealth() - newHp;
                            if (actualDrained > 0.0F) {
                                ElementalGaugeManager.drainHealthSilently(player, newHp);
                                ElementalGaugeManager.recordFurinaHpFluctuation(level, player, actualDrained / maxHp);
                            }
                        }
                    }
                    if (beamElapsed % 2 == 0) {
                        double range = st.netherite ? 28.0D : 24.0D;
                        HitResult hit = level.clip(new ClipContext(player.getEyePosition(),
                                player.getEyePosition().add(player.getLookAngle().normalize().scale(range)),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                        Vec3 end = hit.getLocation();
                        level.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 4, 0.20D, 0.18D, 0.20D, 0.06D);
                        level.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y, end.z, 2, 0.14D, 0.14D, 0.14D, 0.03D);
                        if (beamElapsed % 4 == 0) {
                            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.55F, 1.35F);
                            level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 0.75F, 1.20F);
                        }
                    }
                    if (beamElapsed % 8 == 0) {
                        fireEquitableJudgmentTick(level, player, st.netherite);
                    }
                    if (beamElapsed == 0 || beamElapsed == 24 || beamElapsed == 48) {
                        InteractionHand usedHand = ArcaneStaffItem.getEffectiveKind(player.getMainHandItem()) == ArcaneStaffItem.Kind.GUARDIAN ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                        ArcaneStaffItem.hurtStaffAndCheckBreak(stack, 1, player, usedHand);
                        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                    }
                    syncStateToClients(player, st, false);
                }
            }
        }
    }

    private static void fireEquitableJudgmentTick(ServerLevel level, ServerPlayer player, boolean isNetherite) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double range = isNetherite ? 28.0D : 24.0D;
        double beamRadius = isNetherite ? 1.35D : 1.2D;
        HitResult hit = level.clip(new ClipContext(start, start.add(direction.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = hit.getLocation();
        double maxDist = start.distanceTo(end);

        AABB search = player.getBoundingBox().expandTowards(direction.scale(maxDist)).inflate(beamRadius + 1.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, search,
                t -> t.isAlive() && t != player && !t.isSpectator() && !player.isAlliedTo(t) && !ElementalGaugeManager.isRaidenProtectedAlly(player, t));
        List<LivingEntity> targets = new ArrayList<>(candidates.size());
        for (LivingEntity t : candidates) {
            Vec3 c = t.getBoundingBox().getCenter();
            double proj = Mth.clamp(c.subtract(start).dot(direction), 0.0D, maxDist);
            double maxRad = beamRadius + t.getBbWidth() * 0.5D;
            if (start.add(direction.scale(proj)).distanceToSqr(c) <= maxRad * maxRad) {
                targets.add(t);
            }
        }
        targets.sort(Comparator.comparingDouble(t -> t.getBoundingBox().getCenter().subtract(start).dot(direction)));

        float draconicMult = ElementalGaugeManager.getDraconicGloriesMultiplier(player);
        boolean hpBuff = ElementalGaugeManager.hasRecentHpChangeBuff(player);
        float hpRatio = player.getMaxHealth() > 0.0F ? (player.getHealth() / player.getMaxHealth()) : 1.0F;
        float a4Bonus = Mth.clamp((hpRatio - 0.30F) * 0.60F, 0.0F, 0.30F);
        float totalMult = draconicMult * (1.0F + a4Bonus + (hpBuff ? 0.15F : 0.0F));
        float baseDmg = ArcaneStaffItem.computeGuardianBaseDamage(player.getMaxHealth(), isNetherite) * totalMult;
        float attackU = isNetherite ? 1.5F : 1.0F;

        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            ElementalGaugeManager.applyElement(level, target, Element.HYDRO, attackU, player, baseDmg);
            Vec3 tc = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.SPLASH, tc.x, tc.y, tc.z, 10, 0.25D, 0.25D, 0.25D, 0.06D);
            if (totalMult > 1.05F) {
                level.sendParticles(ParticleTypes.CRIT, tc.x, tc.y, tc.z, 4, 0.2D, 0.2D, 0.2D, 0.05D);
            }
        }
        EnvironmentalBlockManager.applyHydroEnvironmentEffects(level, start, end, isNetherite ? 2.0D : 1.35D);
        level.playSound(null, player.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.9F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 0.70F, 1.15F);
    }

    private static void syncStateToClients(ServerPlayer player, NeuvilletteState st, boolean force) {
        int code;
        if (st.firingBeam) {
            code = 3100 + Math.min(60, st.beamElapsedTicks);
        } else if (st.leftClickHeld) {
            int req = Math.max(1, 60 - st.absorbedDroplets * 20);
            int pct60 = Mth.clamp(Math.round((st.chargeTicks * 60.0F) / req), 0, 59);
            code = 3000 + pct60;
        } else {
            code = 3199;
        }
        if (force || code != st.lastSyncedCode || (st.firingBeam && st.beamElapsedTicks % 4 == 0)) {
            st.lastSyncedCode = code;
            ModNetwork.sendStaffAnimation(player, code);
        }
    }
}
