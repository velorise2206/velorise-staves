package com.velorise.staves.client;

import com.velorise.staves.effect.ModMobEffects;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active overhead elemental icons, dual-icon reaction animations, and floating 3D reaction text on the client.
 */
public final class ClientAuraTracker {
    public static final int ANIM_SPLENDOR_SUMMON = 1;
    public static final int ANIM_SPLENDOR_SWITCH = 2;
    public static final int ANIM_VORTEX_HOLD_BURST = 3;
    public static final int ANIM_VORTEX_TAP = 4;
    public static final int ANIM_GHASTFIRE_SLAM = 5;
    public static final int ANIM_RAIDEN_EYE = 6;
    public static final int ANIM_RAIDEN_CA = 7;
    public static final int ANIM_RAIDEN_DRAW = 8;
    public static final int ANIM_RAIDEN_SLASH = 9;
    public static final int ANIM_ZHONGLI_PLANET_BEFALL = 15;
    public static final int ANIM_ZHONGLI_PLANET_BEFALL_FIRST = 65;
    public static final int ANIM_XILONEN_SKATE_DASH = 20;
    public static final int ANIM_XILONEN_KICK_1 = 21;
    public static final int ANIM_XILONEN_KICK_2 = 22;
    public static final int ANIM_XILONEN_BURST = 23;
    public static final int ANIM_NEUVILLETTE_BURST = 24;
    public static final int ANIM_NEUVILLETTE_SKILL_E = 25;
    public static final int ANIM_PLUNGE_START = 26;
    public static final int ANIM_PLUNGE_END = 27;
    public static final int ANIM_GLIDER_START = 28;
    public static final int ANIM_GLIDER_STOP = 29;
    public static final int ANIM_GLIDER_UNLOCKED = 30;
    public static final int ANIM_GLIDER_LOCKED = 31;
    public static final int ANIM_MAVUIKA_BURST = 32;
    public static final int ANIM_VENTI_BURST = 33;
    public static final int ANIM_VENTI_SKILL_E = 66;
    public static final int ANIM_FURINA_BURST = 34;
    public static final int ANIM_LAYLA_BURST = 35;
    public static final int ANIM_XILONEN_JUMP_FLIP = 36;
    public static final long XILONEN_JUMP_FLIP_DURATION_MS = 740L;
    public static volatile long lastVentiSkillETimeMs = 0L;

    // Nahida (Thousand Floating Dreams)
    public static final int ANIM_NAHIDA_NA_1 = 40;
    public static final int ANIM_NAHIDA_NA_2 = 41;
    public static final int ANIM_NAHIDA_NA_3 = 42;
    public static final int ANIM_NAHIDA_NA_4 = 43;
    public static final int ANIM_NAHIDA_CA = 44;
    public static final int ANIM_NAHIDA_TAP_E = 45;
    public static final int ANIM_NAHIDA_HOLD_E = 46;
    public static final int ANIM_NAHIDA_BURST = 47;

    // Tsaritsa (Cryo Archon Bow & Spear)
    public static final int ANIM_TSARITSA_BOW_NA = 50;
    public static final int ANIM_TSARITSA_CA_SHOT = 51;
    public static final int ANIM_TSARITSA_TAP_E = 52;
    public static final int ANIM_TSARITSA_HOLD_E = 53;
    public static final int ANIM_TSARITSA_SPEAR_NA_1 = 54;
    public static final int ANIM_TSARITSA_SPEAR_NA_2 = 55;
    public static final int ANIM_TSARITSA_SPEAR_NA_3 = 56;
    public static final int ANIM_TSARITSA_BURST_SUPPORT = 57;
    public static final int ANIM_TSARITSA_BURST_DPS = 58;
    public static final int ANIM_TSARITSA_PUNISHMENT = 59;
    public static final int ANIM_TSARITSA_UNSEAL = 60;
    public static final int ANIM_TSARITSA_CROWN_FLASH = 61;
    public static final int ANIM_LAYLA_DREAMSPHERE = 62;
    public static final int ANIM_DRAGONFIRE_PET_OFF = 4070;
    public static final int ANIM_DRAGONFIRE_PET_ON = 4071;
    public static final int ANIM_DRAGONFIRE_IDLE_ON = 4072;
    public static final int ANIM_DRAGONFIRE_ULTI_SKYFALL = 4073;
    public static final int ANIM_DRAGONFIRE_ULTI_SWEEP = 4074;
    public static final int ANIM_STEVE_WALL_CLIMB_ON = 4080;
    public static final int ANIM_STEVE_WALL_CLIMB_OFF = 4081;
    public static final int ANIM_STEVE_WALL_CLIMB_JUMP = 4082;
    public static final int ANIM_STEVE_WALL_CLIMB_MANTLE = 4083;
    public static final int ANIM_STAMINA_TIER_100 = 86;
    public static final int ANIM_STAMINA_TIER_170 = 87;
    public static final int ANIM_STAMINA_TIER_240 = 88;
    public static final int ANIM_DASH_IMPULSE = 89;

    public static volatile float localMaxStamina = 100.0F;
    public static volatile float localStamina = 100.0F;
    public static volatile int localStaminaRegenDelayTicks = 0;
    public static volatile boolean localStaminaExhausted = false;
    public static volatile int localStaminaFullHideTicks = 0;
    private static boolean wasSprintKeyDownForDash = false;
    private static int localDashCooldownTicks = 0;
    private static int localDashChainCount = 0;
    private static int localDashChainResetTicks = 0;
    private static boolean localSteveWallClimbing = false;
    private static com.velorise.staves.fsm.MovementActionState localMovementState = com.velorise.staves.fsm.MovementActionState.GROUNDED;
    private static int localMoveElapsedTicks = 0;
    private static int localWallJumpTicks = 0;
    private static int localMantleTicks = 0;
    private static net.minecraft.core.Direction localWallNormalFace = null;
    private static boolean wasWallJumpKeyDown = false;
    private static boolean localExhaustedClimbFall = false;
    private static final com.velorise.staves.fsm.InputBuffer<com.velorise.staves.fsm.MovementActionState.BufferedInput> localWallInputBuffer = new com.velorise.staves.fsm.InputBuffer<>();

    public static volatile boolean localWindGliderUnlocked = false;
    public static volatile int ventiUltiHoldTicks = 0;
    public static volatile int localTsaritsaHoldETicks = 0;
    public static volatile boolean localTsaritsaHoldELocked = false;
    public static volatile int localTsaritsaBowPullTicks = 0;
    public static volatile float localFurinaCaHoldProgress = 0.0F;
    public static volatile int localDaybreakBowPullTicks = 0;
    public static long localNextRaidenSwingMs = 0L;
    public static boolean localRaidenAttackHeld = false;

    public static boolean canLocalRaidenSwing() {
        return System.currentTimeMillis() >= localNextRaidenSwingMs;
    }

    public static boolean tryConsumeLocalRaidenSwing(boolean isCharged) {
        long now = System.currentTimeMillis();
        if (now < localNextRaidenSwingMs) return false;
        localNextRaidenSwingMs = now + (isCharged ? 550L : 400L);
        return true;
    }

    public static class AuraDisplayData {
        public final List<Element> elements = new ArrayList<>();
        public @Nullable Element permanentInnateElement = null;
        public final java.util.EnumSet<Element> permanentInnateElements = java.util.EnumSet.noneOf(Element.class);
        public boolean permanentOmniInnate = false;
        public @Nullable Element reactedAuraElement = null;
        public @Nullable Element dualTriggerElement = null;
        public long dualStartTime = 0;
        public long dualUntil = 0;
        public @Nullable ElementalReaction reaction = null;
        public long reactionStartTime = 0;
        public long reactionUntil = 0;
        public long appliedTime = 0;
        public float lastDamage = 0.0F;
        public @Nullable Element damageElement = null;
        public long damageStartTime = 0;
        public long damageUntil = 0;
        public int durationTicks = 0;
        public long expireTime = 0; // ms timestamp when aura completely expires
        public boolean isFrozen = false;
        public long frozenUntil = 0L;
        public long jadeShieldUntil = 0L;
        public long nightsFocusUntil = 0L;
        public long laylaDreamsphereUntil = 0L;
        public long laylaDreamsphereStartMs = 0L;
        public net.minecraft.world.phys.Vec3 laylaDreamsphereAnchorPos = null;
        public long dragonfirePetUntil = 0L;
        public long dragonfirePetAttackUntilMs = 0L;
        public int dragonfirePetTargetId = -1;
        public net.minecraft.world.phys.Vec3 dragonfirePetAttackTarget = null;
        public float dragonfirePetOrbitAngle = 0.0F;
        public long dragonfirePetOrbitLastMs = 0L;
        public net.minecraft.world.phys.Vec3 dragonfirePetMouthWorldPos = null;
        public boolean dragonfireIdle = false;
        public float dragonfireIdleFade = 0.0F;
        public long dragonfireIdleStartMs = 0L;
        public float dragonfireIdleYaw = 0.0F;
        public boolean dragonfireIdleYawSet = false;
        public float dragonfireUltiYaw = 0.0F;
        public boolean dragonfireUltiYawSet = false;
        public long nahidaNaPlatformUntilMs = 0L;
        public float nahidaNaPlatformFade = 0.0F;
        public long nahidaNaPlatformLastMs = 0L;
        public boolean steveWallClimbing = false;
        public float steveWallClimbFade = 0.0F;
        public long steveWallClimbLastMs = 0L;
        public float steveWallClimbDist = 0.0F;
        public long steveWallJumpUntilMs = 0L;
        public long steveWallMantleUntilMs = 0L;
        public net.minecraft.core.Direction wallNormalFace = null;
        public final com.velorise.staves.fsm.PoseBlender.HumanoidPose wallClimbPose = new com.velorise.staves.fsm.PoseBlender.HumanoidPose();
        public int nightsFocusStars = 0;
        public long nightsFocusAppliedTime = 0L;
        public long frozenTotalMs = 0L;
        public boolean poseCaptured = false;
        public float capturedWalkPos = 0.0F;
        public float capturedWalkSpeed = 0.0F;
        public float capturedAttackAnim = 0.0F;
        public float capturedYBodyRot = 0.0F;
        public float capturedYHeadRot = 0.0F;
        public float capturedXRot = 0.0F;
        public int capturedTickCount = 0;
        public long lastStruggleTime = 0L;
        public int staffAnimType = 0;
        public long staffAnimStartTime = 0L;
        public long staffAnimUntil = 0L;
        public int raidenComboStep = 0;
        public int raidenNextComboStep = 0;
        public long raidenLastSlashTime = 0L;
        public long petrifiedUntil = 0L;
        public boolean xilonenSkating = false;
        public boolean xilonenWallClimbing = false;
        public long xilonenJumpFlipStartTime = 0L;
        public long xilonenJumpFlipUntil = 0L;
        public boolean xilonenNetherite = false;
        public int xilonenNightsoulPoints = 0;
        public long xilonenSkateSyncUntil = 0L;
        public int xilonenSampleMask = 0;
        public long xilonenSamplesUntil = 0L;
        public boolean gliding = false;
        public boolean plunging = false;
        public float dashPoseFade = 0.0F;
        public long neuvilletteActiveUntil = 0L;
        public boolean neuvilletteFiringBeam = false;
        public int neuvilletteChargeProgress = 0;
        public int neuvilletteBeamElapsed = 0;

        // Nahida states
        public boolean nahidaIdleSwing = false;
        public float nahidaIdleSwingFade = 0.0F;
        public long nahidaIdleSwingLastMs = 0L;
        public float nahidaIdleYaw = 0.0F;
        public boolean nahidaIdleYawSet = false;
        public long nahidaWeaponHideUntil = 0L;
        public float ultiYaw = 0.0F;
        public boolean ultiYawSet = false;
        public boolean zhongliIdleMeteor = false;
        public float zhongliIdleFade = 0.0F;
        public long zhongliIdleLastMs = 0L;
        public float zhongliIdleYaw = 0.0F;
        public boolean zhongliIdleYawSet = false;
        public boolean raidenIdle = false;
        public float raidenIdleFade = 0.0F;
        public long raidenIdleLastMs = 0L;
        public float raidenIdleYaw = 0.0F;
        public boolean raidenIdleYawSet = false;
        public boolean mavuikaIdle = false;
        public float mavuikaIdleFade = 0.0F;
        public long mavuikaIdleLastMs = 0L;
        public long mavuikaIdleStartMs = 0L;
        public float mavuikaIdleYaw = 0.0F;
        public boolean mavuikaIdleYawSet = false;
        public int furinaIdleMode = 0; // 0=off, 1=Ousia Salon, 2=Pneuma Bubble+Cake
        public float furinaIdleFade = 0.0F;
        public long furinaIdleLastMs = 0L;
        public int furinaIdleLastMode = 0;
        public float furinaIdleYaw = 0.0F;
        public boolean furinaIdleYawSet = false;
        public long furinaIdleStartMs = 0L;
        public boolean ventiIdle = false;
        public int ventiIdleMode = 0;
        public int ventiIdleLastMode = 1;
        public float ventiIdleFade = 0.0F;
        public long ventiIdleLastMs = 0L;
        public float ventiIdleYaw = 0.0F;
        public boolean ventiIdleYawSet = false;
        public boolean tsaritsaIdle = false;
        public float tsaritsaIdleFade = 0.0F;
        public long tsaritsaIdleLastMs = 0L;
        public long tsaritsaIdleStartMs = 0L;
        public float tsaritsaIdleYaw = 0.0F;
        public boolean tsaritsaIdleYawSet = false;
        public boolean nahidaViewfinder = false;
        public float tsaritsaSpearFade = 0.0F;
        public long tsaritsaSpearLastMs = 0L;
        public float nahidaViewfinderFade = 0.0F;
        public long nahidaViewfinderLastMs = 0L;
        public long skandhaMarkedUntil = 0L;
        public int nahidaShrineMask = 0;
        public long nahidaShrineUntil = 0L;
        public double lastShrineStepX = Double.NaN;
        public double lastShrineStepZ = Double.NaN;
        public double lastShrineStepY = Double.NaN;
        public long lastShrineStepMs = 0L;
        public int shrineStepVariant = 0;

        // Tsaritsa states
        public boolean tsaritsaCrownActive = false;
        public boolean tsaritsaSpearStance = false;
        public boolean tsaritsaSupportBurst = false;
        public boolean tsaritsaDpsSeal = false;
        public boolean tsaritsaNetherite = false;
        public int tsaritsaPunishmentCharges = 0;
        public long tsaritsaSyncUntil = 0L;
        public long tsaritsaCrownFlashUntil = 0L;
    }

    private static final Map<Integer, AuraDisplayData> DATA = new ConcurrentHashMap<>();
    public static @Nullable Element activeCrystallizeShield = null;
    public static long crystallizeShieldUntil = 0L;
    public static long crystallizeShieldAppliedTime = 0L;
    public static long jadeShieldAppliedTime = 0L;
    public static volatile long silentDrainUntil = 0L;
    public static int localResonanceMask = 0;
    public static boolean localOverloaded = false;
    public static int localStaffCount = 0;

    public static void updateLocalResonance(int mask, boolean overloaded, int staffCount) {
        localResonanceMask = mask;
        localOverloaded = overloaded;
        localStaffCount = staffCount;
    }

    public static boolean hasLocalResonance(int ordinal) {
        return !localOverloaded && (localResonanceMask & (1 << ordinal)) != 0;
    }

    public static class ActiveArc {
        public int fromEntityId;
        public net.minecraft.world.phys.Vec3 start;
        public int toEntityId;
        public net.minecraft.world.phys.Vec3 end;
        public long expireTime;
        public long startTime;
        public int color;
        public final List<org.joml.Vector3f> points = new ArrayList<>();
        public long lastJitterTime = 0L;
    }

    public static class FloatingCombatText {
        public final int entityId;
        public final double worldX;
        public final double worldY;
        public final double worldZ;
        public final @Nullable ElementalReaction reaction;
        public final float damage;
        public final @Nullable Element damageElement;
        public final long startTime;
        public final long reactionUntil;
        public final long damageUntil;
        public final long until;

        public FloatingCombatText(
                int entityId,
                double worldX,
                double worldY,
                double worldZ,
                @Nullable ElementalReaction reaction,
                float damage,
                @Nullable Element damageElement,
                long startTime,
                long reactionUntil,
                long damageUntil
        ) {
            this.entityId = entityId;
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldZ = worldZ;
            this.reaction = reaction;
            this.damage = damage;
            this.damageElement = damageElement;
            this.startTime = startTime;
            this.reactionUntil = reactionUntil;
            this.damageUntil = damageUntil;
            this.until = Math.max(reaction != null ? reactionUntil : 0L, damage > 0.0F ? damageUntil : 0L);
        }
    }

    public static final List<ActiveArc> ACTIVE_ARCS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<FloatingCombatText> FLOATING_TEXTS = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static boolean matchesFloatingTarget(FloatingCombatText t, int entityId, double x, double y, double z) {
        if (entityId > 0 && t.entityId > 0) {
            return t.entityId == entityId;
        }
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return false;
        }
        double dx = t.worldX - x;
        double dy = t.worldY - y;
        double dz = t.worldZ - z;
        return (dx * dx + dy * dy + dz * dz) <= 2.25D;
    }

    private static boolean hasActiveReactionText(
            int entityId,
            double x,
            double y,
            double z,
            @Nullable ElementalReaction reaction,
            long now
    ) {
        if (reaction == null || reaction == ElementalReaction.NONE || FLOATING_TEXTS.isEmpty()) {
            return false;
        }
        for (FloatingCombatText t : FLOATING_TEXTS) {
            if (t.reaction == reaction && now < t.reactionUntil && matchesFloatingTarget(t, entityId, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRecentIdenticalDamage(
            int entityId,
            double x,
            double y,
            double z,
            float damage,
            @Nullable Element damageElement,
            long now
    ) {
        if (damage <= 0.0F || FLOATING_TEXTS.isEmpty()) {
            return false;
        }
        int rounded = Math.round(damage * 10.0F);
        for (FloatingCombatText t : FLOATING_TEXTS) {
            if (t.damage > 0.0F
                    && (now - t.startTime) < 250L
                    && now < t.damageUntil
                    && Math.round(t.damage * 10.0F) == rounded
                    && java.util.Objects.equals(t.damageElement, damageElement)
                    && matchesFloatingTarget(t, entityId, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public static void addFloatingText(
            double x,
            double y,
            double z,
            @Nullable ElementalReaction reaction,
            float damage,
            @Nullable Element damageElement
    ) {
        addFloatingText(-1, x, y, z, reaction, damage, damageElement);
    }

    public static void addFloatingText(
            int entityId,
            double x,
            double y,
            double z,
            @Nullable ElementalReaction reaction,
            float damage,
            @Nullable Element damageElement
    ) {
        if ((reaction == null || reaction == ElementalReaction.NONE) && damage <= 0.0F) return;
        long now = System.currentTimeMillis();
        if (!FLOATING_TEXTS.isEmpty()) {
            FLOATING_TEXTS.removeIf(t -> now >= t.until);
        }
        if (reaction != null && hasActiveReactionText(entityId, x, y, z, reaction, now)) {
            reaction = null;
        }
        if (damage > 0.0F && hasRecentIdenticalDamage(entityId, x, y, z, damage, damageElement, now)) {
            damage = 0.0F;
        }
        if ((reaction == null || reaction == ElementalReaction.NONE) && damage <= 0.0F) return;
        if (FLOATING_TEXTS.size() > 256) {
            while (FLOATING_TEXTS.size() > 256) {
                FLOATING_TEXTS.remove(0);
            }
        }
        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
        double ox = (rng.nextDouble() - 0.5D) * 0.56D;
        double oy = (rng.nextDouble() - 0.5D) * 0.28D;
        double oz = (rng.nextDouble() - 0.5D) * 0.56D;
        FLOATING_TEXTS.add(new FloatingCombatText(
                entityId,
                x + ox,
                y + oy,
                z + oz,
                reaction,
                damage,
                damageElement,
                now,
                now + 1600L,
                now + 1500L
        ));
    }

    public static void addArc(int fromId, net.minecraft.world.phys.Vec3 start, int toId, net.minecraft.world.phys.Vec3 end, int durationTicks, int color) {
        long now = System.currentTimeMillis();
        if (color == 0xFFDF00FF && start != null) {
            int ownerId = (int) Math.round(start.x);
            int tgtId = (int) Math.round(start.y);
            if (ownerId > 0) {
                AuraDisplayData entry = DATA.computeIfAbsent(ownerId, k -> new AuraDisplayData());
                entry.dragonfirePetAttackUntilMs = now + 450L;
                entry.dragonfirePetTargetId = tgtId;
                entry.dragonfirePetAttackTarget = end;
            }
            return;
        }
        long nextExpire = now + (durationTicks > 0 ? (long) durationTicks * 50L : 400L);
        if ((fromId == -2 || fromId == -5 || fromId == -6) && start != null) {
            for (ActiveArc existing : ACTIVE_ARCS) {
                if (existing.fromEntityId == fromId && existing.start != null && existing.start.distanceToSqr(start) < 0.25D) {
                    existing.color = color;
                    existing.end = end;
                    existing.expireTime = nextExpire;
                    return;
                }
            }
        }
        if (ACTIVE_ARCS.size() > 256) {
            ACTIVE_ARCS.removeIf(a -> now >= a.expireTime);
            if (ACTIVE_ARCS.size() > 256) {
                return;
            }
        }
        ActiveArc arc = new ActiveArc();
        boolean isNahidaWorldVfx = ((color & 0xFFFF0000) == 0x4E410000) || ((color & 0xFFFF0000) == 0x4E420000);
        int resolvedFromId = isNahidaWorldVfx ? -8 : fromId;
        net.minecraft.world.phys.Vec3 resolvedStart = start;
        int resolvedToId = isNahidaWorldVfx ? -8 : toId;
        if (color == 0xFFD05CE3 && start != null) {
            int bestOwner = fromId > 0 ? fromId : -1;
            int bestTargetId = toId > 0 ? toId : -1;
            if (bestOwner <= 0) {
                for (Map.Entry<Integer, AuraDisplayData> e : DATA.entrySet()) {
                    AuraDisplayData d = e.getValue();
                    if (d != null && (now <= d.dragonfirePetAttackUntilMs || now < d.dragonfirePetUntil)) {
                        bestOwner = e.getKey();
                        if (bestTargetId <= 0) {
                            bestTargetId = d.dragonfirePetTargetId;
                        }
                        break;
                    }
                }
            }
            if (bestOwner > 0) {
                AuraDisplayData entry = DATA.computeIfAbsent(bestOwner, k -> new AuraDisplayData());
                long attackDurMs = durationTicks > 0 ? (long) durationTicks * 50L : 550L;
                entry.dragonfirePetAttackUntilMs = now + Math.max(500L, attackDurMs);
                if (bestTargetId > 0) {
                    entry.dragonfirePetTargetId = bestTargetId;
                }
                entry.dragonfirePetAttackTarget = end;
                resolvedFromId = -10000 - bestOwner;
                if (bestTargetId > 0) {
                    resolvedToId = bestTargetId;
                }
                net.minecraft.world.phys.Vec3 liveMouth =
                        com.velorise.staves.client.character.DragonfireVisualRenderer.snapPetToAttackAndGetMouthPos(bestOwner, bestTargetId, end);
                if (liveMouth != null) {
                    entry.dragonfirePetMouthWorldPos = liveMouth;
                    resolvedStart = liveMouth;
                } else if (entry.dragonfirePetMouthWorldPos != null) {
                    resolvedStart = entry.dragonfirePetMouthWorldPos;
                }
            }
        }
        arc.fromEntityId = resolvedFromId;
        arc.start = resolvedStart;
        arc.toEntityId = resolvedToId;
        arc.end = end;
        arc.startTime = now;
        arc.expireTime = nextExpire;
        arc.color = color;
        ACTIVE_ARCS.add(arc);
    }

    public static void pruneStale(net.minecraft.world.level.Level level) {
        if (level == null) return;
        long now = System.currentTimeMillis();
        DATA.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null);
        MAVUIKA_STATES.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null);
        RAIDEN_STATES.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null);
        ACTIVE_ARCS.removeIf(a -> now >= a.expireTime);
        FLOATING_TEXTS.removeIf(t -> now >= t.until);
    }


    public static void triggerStaffAnimation(int entityId, int animType) {
        if (entityId <= 0 || animType <= 0) return;
        boolean forceFirstZhongliOrder = (animType == ANIM_ZHONGLI_PLANET_BEFALL_FIRST);
        if (forceFirstZhongliOrder) {
            animType = ANIM_ZHONGLI_PLANET_BEFALL;
        }
        long now = System.currentTimeMillis();
        if (animType == ANIM_GLIDER_UNLOCKED) {
            localWindGliderUnlocked = true;
            return;
        }
        if (animType == ANIM_GLIDER_LOCKED) {
            localWindGliderUnlocked = false;
            return;
        }
        if (animType == ANIM_STAMINA_TIER_100) {
            localMaxStamina = 100.0F;
            if (localStamina > 100.0F) localStamina = 100.0F;
            return;
        }
        if (animType == ANIM_STAMINA_TIER_170) {
            boolean upgraded = localMaxStamina < 170.0F;
            localMaxStamina = 170.0F;
            if (upgraded) {
                localStamina = 170.0F;
                localStaminaExhausted = false;
                localStaminaFullHideTicks = 60;
            }
            return;
        }
        if (animType == ANIM_STAMINA_TIER_240) {
            boolean upgraded = localMaxStamina < 240.0F;
            localMaxStamina = 240.0F;
            if (upgraded) {
                localStamina = 240.0F;
                localStaminaExhausted = false;
                localStaminaFullHideTicks = 60;
            }
            return;
        }
        if (animType == ANIM_XILONEN_JUMP_FLIP) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            // Ignore duplicate server echo packet if the client already started this jump flip within the last 350ms
            if (now < entry.xilonenJumpFlipUntil && (now - entry.xilonenJumpFlipStartTime) >= 0L && (now - entry.xilonenJumpFlipStartTime) < 350L) {
                return;
            }
            entry.xilonenJumpFlipStartTime = now;
            entry.xilonenJumpFlipUntil = now + XILONEN_JUMP_FLIP_DURATION_MS;
            return;
        }
        if (animType == ANIM_GLIDER_START || animType == ANIM_GLIDER_STOP) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.gliding = (animType == ANIM_GLIDER_START);
            if (entry.gliding) {
                entry.plunging = false;
                entry.xilonenJumpFlipUntil = 0L;
            }
            return;
        }
        if (animType == ANIM_PLUNGE_START) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.plunging = true;
            entry.gliding = false;
            entry.xilonenJumpFlipUntil = 0L;
            entry.staffAnimType = ANIM_PLUNGE_START;
            entry.staffAnimStartTime = now;
            entry.staffAnimUntil = now + 3500L;
            return;
        }
        if (animType == ANIM_PLUNGE_END) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.plunging = false;
            entry.staffAnimType = ANIM_PLUNGE_END;
            entry.staffAnimStartTime = now;
            entry.staffAnimUntil = now + 360L;
            return;
        }
        if (animType >= 1000 && animType <= 1999) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (animType == 1999) {
                entry.xilonenSkating = false;
                entry.xilonenWallClimbing = false;
                entry.xilonenJumpFlipUntil = 0L;
                entry.xilonenNightsoulPoints = 0;
                entry.xilonenSkateSyncUntil = 0L;
            } else {
                int rem = animType - 1000;
                entry.xilonenNetherite = rem >= 200;
                if (entry.xilonenNetherite) rem -= 200;
                entry.xilonenWallClimbing = rem >= 100;
                if (entry.xilonenWallClimbing) {
                    rem -= 100;
                    entry.xilonenJumpFlipUntil = 0L;
                }
                entry.xilonenNightsoulPoints = Math.max(0, Math.min(90, rem));
                entry.xilonenSkating = entry.xilonenNightsoulPoints > 0;
                entry.xilonenSkateSyncUntil = now + 15000L;
            }
            return;
        }
        if (animType >= 2000 && animType <= 2099) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            int mask = animType - 2000;
            entry.xilonenSampleMask = mask;
            entry.xilonenSamplesUntil = mask > 0 ? (now + 15500L) : 0L;
            return;
        }
        if (animType >= 3000 && animType <= 3199) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (animType == 3199) {
                entry.neuvilletteActiveUntil = 0L;
                entry.neuvilletteFiringBeam = false;
                entry.neuvilletteChargeProgress = 0;
                entry.neuvilletteBeamElapsed = 0;
            } else if (animType >= 3100) {
                entry.neuvilletteFiringBeam = true;
                entry.neuvilletteChargeProgress = 60;
                entry.neuvilletteBeamElapsed = animType - 3100;
                entry.neuvilletteActiveUntil = now + 450L;
            } else {
                entry.neuvilletteFiringBeam = false;
                entry.neuvilletteChargeProgress = animType - 3000;
                entry.neuvilletteBeamElapsed = 0;
                entry.neuvilletteActiveUntil = now + 450L;
            }
            return;
        }
        if (animType >= 4000 && animType <= 4002) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.nahidaIdleSwing = (animType == 4001);
            if (!entry.nahidaIdleSwing) {
                entry.nahidaIdleYawSet = false;
            }
            entry.nahidaViewfinder = (animType == 4002);
            return;
        }
        if (animType == 4010 || animType == 4011) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.zhongliIdleMeteor = (animType == 4011);
            if (!entry.zhongliIdleMeteor) {
                entry.zhongliIdleYawSet = false;
            }
            return;
        }
        if (animType == 4020 || animType == 4021) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.raidenIdle = (animType == 4021);
            if (!entry.raidenIdle) {
                entry.raidenIdleYawSet = false;
            }
            return;
        }
        if (animType == 4030 || animType == 4031) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            boolean was = entry.mavuikaIdle;
            entry.mavuikaIdle = (animType == 4031);
            if (entry.mavuikaIdle && !was) {
                entry.mavuikaIdleStartMs = now;
            } else if (!entry.mavuikaIdle) {
                entry.mavuikaIdleYawSet = false;
            }
            return;
        }
        if (animType >= 4040 && animType <= 4042) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            int prevMode = entry.furinaIdleMode;
            entry.furinaIdleMode = animType - 4040;
            if (entry.furinaIdleMode > 0) {
                entry.furinaIdleLastMode = entry.furinaIdleMode;
                if (prevMode == 0) {
                    entry.furinaIdleStartMs = now;
                }
            } else {
                entry.furinaIdleStartMs = 0L;
                entry.furinaIdleYawSet = false;
            }
            return;
        }
        if (animType >= 4050 && animType <= 4052) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.ventiIdle = (animType > 4050);
            entry.ventiIdleMode = animType - 4050;
            if (entry.ventiIdleMode > 0) {
                entry.ventiIdleLastMode = entry.ventiIdleMode;
            } else {
                entry.ventiIdleYawSet = false;
            }
            return;
        }
        if (animType == 4060 || animType == 4061) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            boolean prevTsaIdle = entry.tsaritsaIdle;
            entry.tsaritsaIdle = (animType == 4061);
            if (entry.tsaritsaIdle && !prevTsaIdle) {
                entry.tsaritsaIdleStartMs = now;
            } else if (!entry.tsaritsaIdle) {
                entry.tsaritsaIdleStartMs = 0L;
            }
            return;
        }
        if (animType == ANIM_DRAGONFIRE_PET_OFF || animType == ANIM_DRAGONFIRE_PET_ON || animType == ANIM_DRAGONFIRE_IDLE_ON) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (animType == ANIM_DRAGONFIRE_PET_OFF) {
                entry.dragonfirePetUntil = 0L;
                entry.dragonfireIdle = false;
                entry.dragonfireIdleYawSet = false;
            } else if (animType == ANIM_DRAGONFIRE_PET_ON) {
                entry.dragonfirePetUntil = now + 31000L;
                entry.dragonfireIdle = false;
                entry.dragonfireIdleYawSet = false;
            } else {
                if (entry.dragonfirePetUntil <= now) {
                    if (!entry.dragonfireIdle) {
                        entry.dragonfireIdleStartMs = now;
                        entry.dragonfireIdleYawSet = false;
                    }
                    entry.dragonfireIdle = true;
                }
            }
            return;
        }
        if (animType == ANIM_DRAGONFIRE_ULTI_SKYFALL || animType == ANIM_DRAGONFIRE_ULTI_SWEEP) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (animType == ANIM_DRAGONFIRE_ULTI_SKYFALL) {
                entry.dragonfirePetUntil = 0L;
            }
            entry.dragonfireIdle = false;
            entry.dragonfireIdleFade = 0.0F;
            entry.dragonfireIdleYawSet = false;
            entry.dragonfireUltiYawSet = false;
            entry.staffAnimType = animType;
            entry.staffAnimStartTime = now;
            entry.staffAnimUntil = now + (animType == ANIM_DRAGONFIRE_ULTI_SKYFALL ? 850L : 1500L);
            return;
        }
        if (animType == ANIM_STEVE_WALL_CLIMB_ON || animType == ANIM_STEVE_WALL_CLIMB_OFF) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.steveWallClimbing = (animType == ANIM_STEVE_WALL_CLIMB_ON);
            if (!entry.steveWallClimbing) entry.wallNormalFace = null;
            return;
        }
        if (animType == ANIM_STEVE_WALL_CLIMB_JUMP) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.steveWallJumpUntilMs = now + 420L;
            return;
        }
        if (animType == ANIM_STEVE_WALL_CLIMB_MANTLE) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.steveWallMantleUntilMs = now + 320L;
            return;
        }
        if (animType >= 4100 && animType <= 4101) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.skandhaMarkedUntil = (animType == 4101) ? (now + 25000L) : 0L;
            return;
        }
        if (animType >= 4200 && animType <= 4231) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            int mask = animType - 4200;
            entry.nahidaShrineMask = mask;
            entry.nahidaShrineUntil = mask > 0 ? (now + 23500L) : 0L;
            return;
        }
        if (animType >= 5000 && animType <= 5255) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            int mask = animType - 5000;
            entry.tsaritsaCrownActive = (mask & 1) != 0;
            entry.tsaritsaSpearStance = (mask & 2) != 0;
            entry.tsaritsaSupportBurst = (mask & 4) != 0;
            entry.tsaritsaDpsSeal = (mask & 8) != 0;
            entry.tsaritsaNetherite = (mask & 16) != 0;
            entry.tsaritsaPunishmentCharges = (mask >> 5) & 3;
            entry.tsaritsaSyncUntil = mask > 0 ? (now + 60000L) : 0L;
            return;
        }
        if (animType == ANIM_TSARITSA_CROWN_FLASH) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.tsaritsaCrownFlashUntil = now + 500L;
            return;
        }
        if (animType == ANIM_LAYLA_DREAMSPHERE) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.laylaDreamsphereStartMs = now;
            entry.laylaDreamsphereUntil = now + 12500L;
            net.minecraft.world.level.Level clientLevel = ClientAccess.getClientLevel();
            if (clientLevel != null) {
                net.minecraft.world.entity.Entity caster = clientLevel.getEntity(entityId);
                if (caster != null) {
                    entry.laylaDreamsphereAnchorPos = caster.position().add(0.0D, 2.75D, 0.0D);
                }
            }
            return;
        }
        if (animType >= 10 && animType <= 14) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (entry.nightsFocusUntil <= now) {
                entry.nightsFocusAppliedTime = now;
                entry.nightsFocusUntil = now + 12000L;
            }
            entry.nightsFocusStars = animType - 10;
            return;
        }
        if (animType == 16 || animType == 17) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            long durMs = animType == 17 ? 5000L : 4000L;
            entry.isFrozen = true;
            entry.frozenUntil = Math.max(entry.frozenUntil, now + durMs);
            entry.petrifiedUntil = Math.max(entry.petrifiedUntil, now + durMs);
            entry.frozenTotalMs = Math.max(entry.frozenTotalMs, durMs);
            return;
        }
        if (animType == 19) {
            AuraDisplayData entry = DATA.get(entityId);
            if (entry != null) {
                entry.nightsFocusUntil = 0L;
                entry.nightsFocusStars = 0;
            }
            return;
        }
        AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
        if (animType >= ANIM_NAHIDA_NA_1 && animType <= ANIM_NAHIDA_NA_4) {
            entry.nahidaNaPlatformUntilMs = now + 950L;
        }
        if (animType == ANIM_XILONEN_SKATE_DASH) {
            entry.xilonenJumpFlipUntil = 0L;
        }
        if (animType == ANIM_RAIDEN_SLASH) {
            if (now - entry.raidenLastSlashTime > 1500L) {
                entry.raidenNextComboStep = 0;
            }
            entry.raidenComboStep = entry.raidenNextComboStep;
            entry.raidenNextComboStep = (entry.raidenNextComboStep + 1) % 5;
            entry.raidenLastSlashTime = now;
        } else if (animType == ANIM_RAIDEN_DRAW || animType == ANIM_RAIDEN_CA || animType == ANIM_RAIDEN_EYE) {
            entry.raidenNextComboStep = 0;
            if (animType == ANIM_RAIDEN_DRAW) {
                localNextRaidenSwingMs = Math.max(localNextRaidenSwingMs, now + 1300L);
            }
        }
        long durationMs = switch (animType) {
            case ANIM_SPLENDOR_SUMMON -> 850L;
            case ANIM_SPLENDOR_SWITCH -> 650L;
            case ANIM_VORTEX_HOLD_BURST -> 750L;
            case ANIM_VORTEX_TAP -> 500L;
            case ANIM_GHASTFIRE_SLAM -> 450L;
            case ANIM_RAIDEN_EYE -> 420L;
            case ANIM_RAIDEN_DRAW -> 1600L;
            case ANIM_RAIDEN_SLASH -> (entry.raidenComboStep == 3 || entry.raidenComboStep == 4) ? 420L : 340L;
            case ANIM_RAIDEN_CA -> 700L;
            case ANIM_ZHONGLI_PLANET_BEFALL -> 2800L;
            case ANIM_XILONEN_SKATE_DASH -> 480L;
            case ANIM_XILONEN_KICK_1 -> 380L;
            case ANIM_XILONEN_KICK_2 -> 460L;
            case ANIM_XILONEN_BURST -> 1800L;
            case ANIM_NEUVILLETTE_BURST -> 1800L;
            case ANIM_NEUVILLETTE_SKILL_E -> 550L;
            case ANIM_MAVUIKA_BURST -> 1700L;
            case ANIM_VENTI_BURST -> 2000L;
            case ANIM_VENTI_SKILL_E -> 900L;
            case ANIM_FURINA_BURST -> 1800L;
            case ANIM_LAYLA_BURST -> 1600L;
            case ANIM_NAHIDA_NA_1, ANIM_NAHIDA_NA_2, ANIM_NAHIDA_NA_3 -> 380L;
            case ANIM_NAHIDA_NA_4 -> 520L;
            case ANIM_NAHIDA_CA -> 650L;
            case ANIM_NAHIDA_TAP_E -> 650L;
            case ANIM_NAHIDA_HOLD_E -> 520L;
            case ANIM_NAHIDA_BURST -> 2000L;
            case ANIM_TSARITSA_BOW_NA -> 350L;
            case ANIM_TSARITSA_CA_SHOT -> 520L;
            case ANIM_TSARITSA_TAP_E -> 500L;
            case ANIM_TSARITSA_HOLD_E -> 680L;
            case ANIM_TSARITSA_SPEAR_NA_1, ANIM_TSARITSA_SPEAR_NA_2 -> 380L;
            case ANIM_TSARITSA_SPEAR_NA_3 -> 480L;
            case ANIM_TSARITSA_BURST_SUPPORT -> 2000L;
            case ANIM_TSARITSA_BURST_DPS -> 2000L;
            case ANIM_TSARITSA_PUNISHMENT -> 520L;
            case ANIM_TSARITSA_UNSEAL -> 650L;
            case ANIM_DASH_IMPULSE -> 380L;
            default -> 600L;
        };
        if (animType == ANIM_VENTI_SKILL_E) {
            lastVentiSkillETimeMs = now;
        }
        if (animType == ANIM_VENTI_BURST) {
            entry.ventiIdle = false;
            entry.ventiIdleFade = 0.0F;
            entry.ventiIdleYawSet = false;
        }
        if (animType >= ANIM_TSARITSA_BOW_NA && animType <= ANIM_TSARITSA_UNSEAL) {
            entry.tsaritsaIdle = false;
            entry.tsaritsaIdleFade = 0.0F;
            entry.tsaritsaIdleYawSet = false;
        }
        entry.staffAnimType = animType;
        entry.staffAnimStartTime = now;
        entry.staffAnimUntil = now + durationMs;
        if (animType == ANIM_VORTEX_TAP) {
            com.velorise.staves.audio.CharacterVoiceManager.playClientZhongliVoice(entityId, false);
        } else if (animType == ANIM_VORTEX_HOLD_BURST) {
            com.velorise.staves.audio.CharacterVoiceManager.playClientZhongliVoice(entityId, true);
        } else if (animType == ANIM_ZHONGLI_PLANET_BEFALL) {
            com.velorise.staves.audio.CharacterVoiceManager.playClientZhongliBurstVoice(entityId, forceFirstZhongliOrder);
        }
    }

    private ClientAuraTracker() {}

    public static void update(
            int entityId,
            List<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        update(entityId, elements, reactedAura, trigger, reaction, 0.0F, 0);
    }

    public static void update(
            int entityId,
            List<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage
    ) {
        update(entityId, elements, reactedAura, trigger, reaction, damage, 0);
    }

    public static void update(
            int entityId,
            List<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks
    ) {
        update(entityId, elements, reactedAura, trigger, reaction, damage, durationTicks, reaction == ElementalReaction.FROZEN);
    }

    public static void update(
            int entityId,
            List<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks,
            boolean isFrozenState
    ) {
        update(entityId, elements, reactedAura, trigger, reaction, damage, durationTicks, isFrozenState, false, Double.NaN, Double.NaN, Double.NaN);
    }

    public static void update(
            int entityId,
            List<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks,
            boolean isFrozenState,
            boolean hasPos,
            double posX,
            double posY,
            double posZ
    ) {
        long now = System.currentTimeMillis();
        net.minecraft.world.entity.player.Player localPlayer = ClientAccess.getLocalPlayer();
        if (reaction == ElementalReaction.CRYSTALLIZE && elements.isEmpty() && trigger == null) {
            // Dedicated Crystallize / Jade Shield sync packet from server: do NOT overwrite active Aura or show dual icons!
            AuraDisplayData shieldEntry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (reactedAura == Element.GEO && durationTicks > 0) {
                shieldEntry.jadeShieldUntil = now + (long) durationTicks * 50L;
                jadeShieldAppliedTime = now;
            } else {
                shieldEntry.jadeShieldUntil = 0L;
            }
            if (localPlayer != null && localPlayer.getId() == entityId) {
                if (reactedAura != null) {
                    activeCrystallizeShield = reactedAura;
                    crystallizeShieldAppliedTime = now;
                    crystallizeShieldUntil = now + (durationTicks > 0 ? (long) durationTicks * 50L : 15000L);
                    if (reactedAura != Element.GEO) {
                        ModMobEffects.removeAllShieldsExcept(localPlayer, reactedAura);
                    }
                } else {
                    activeCrystallizeShield = null;
                    crystallizeShieldUntil = 0L;
                    ModMobEffects.removeAllShields(localPlayer);
                }
            }
            return;
        }
        if (durationTicks == -5) {
            triggerStaffAnimation(entityId, Math.round(damage));
            return;
        }
        if (durationTicks == -4) {
            silentDrainUntil = now + 400L;
            if (localPlayer != null && localPlayer.getId() == entityId) {
                localPlayer.setHealth(damage);
                localPlayer.invulnerableTime = 0;
                localPlayer.hurtTime = 0;
                localPlayer.hurtDuration = 0;
            }
            net.minecraft.world.level.Level clientLvl = ClientAccess.getClientLevel();
            if (clientLvl != null) {
                net.minecraft.world.entity.Entity ent = clientLvl.getEntity(entityId);
                if (ent instanceof net.minecraft.world.entity.LivingEntity le) {
                    le.setHealth(damage);
                    le.invulnerableTime = 0;
                    le.hurtTime = 0;
                    le.hurtDuration = 0;
                }
            }
            return;
        }
        if (durationTicks == -3) {
            if (damage > 0.0F) {
                if (localPlayer != null && entityId == localPlayer.getId() && ClientAccess.isFirstPerson()) {
                    return;
                }
                double bx = posX;
                double by = posY;
                double bz = posZ;
                if (!hasPos || Double.isNaN(bx) || Double.isNaN(by) || Double.isNaN(bz)) {
                    net.minecraft.world.level.Level clientLvl = ClientAccess.getClientLevel();
                    net.minecraft.world.entity.Entity e = clientLvl != null ? clientLvl.getEntity(entityId) : null;
                    if (e != null) {
                        bx = e.getX();
                        by = e.getY() + e.getBbHeight() + 0.35D + Math.min(0.6D, e.getBbHeight() * 0.12D);
                        bz = e.getZ();
                    } else {
                        bx = Double.NaN;
                    }
                }
                if (!Double.isNaN(bx)) {
                    addFloatingText(entityId, bx, by, bz, null, damage, null);
                }
            }
            return;
        }
        AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
        entry.elements.clear();
        entry.elements.addAll(elements);
        entry.appliedTime = now;
        entry.durationTicks = durationTicks;
        if (durationTicks == -2) {
            entry.permanentOmniInnate = true;
            entry.expireTime = -1L; // Permanent Omni Mutated state
        } else if (durationTicks > 0) {
            entry.expireTime = now + (long) durationTicks * 50L;
        } else if (durationTicks < 0) {
            entry.expireTime = -1L; // Permanent innate aura
            if (!elements.isEmpty()) {
                entry.permanentInnateElements.clear();
                entry.permanentInnateElements.addAll(elements);
                entry.permanentInnateElement = elements.get(0);
            }
        } else {
            if (!elements.isEmpty()) {
                entry.expireTime = now + 9500L; // Default 9.5s (1U) fallback so temporary auras never stay stuck at expireTime=0
            } else {
                entry.expireTime = (!entry.permanentInnateElements.isEmpty() || entry.permanentInnateElement != null || entry.permanentOmniInnate) ? -1L : 0L;
            }
        }
        for (Element perm : entry.permanentInnateElements) {
            if (!entry.elements.contains(perm)) {
                entry.elements.add(perm);
            }
        }
        if (entry.permanentInnateElement != null && !entry.elements.contains(entry.permanentInnateElement)) {
            entry.elements.add(0, entry.permanentInnateElement);
        }

        double resolvedX = posX;
        double resolvedY = posY;
        double resolvedZ = posZ;
        if (!hasPos || Double.isNaN(resolvedX) || Double.isNaN(resolvedY) || Double.isNaN(resolvedZ)) {
            net.minecraft.world.level.Level clientLvl = ClientAccess.getClientLevel();
            net.minecraft.world.entity.Entity e = clientLvl != null ? clientLvl.getEntity(entityId) : null;
            if (e != null) {
                resolvedX = e.getX();
                resolvedY = e.getY() + e.getBbHeight() + 0.35D + Math.min(0.6D, e.getBbHeight() * 0.12D);
                resolvedZ = e.getZ();
            } else {
                resolvedX = Double.NaN;
            }
        }

        boolean spawnReaction = false;
        if (reaction != null && reaction != ElementalReaction.NONE) {
            boolean keepOngoingText = (entry.reaction == reaction && now < entry.reactionUntil)
                    || hasActiveReactionText(entityId, resolvedX, resolvedY, resolvedZ, reaction, now);
            if (!keepOngoingText) {
                spawnReaction = true;
                entry.reaction = reaction;
                entry.reactionStartTime = now;
                entry.reactionUntil = now + 1600; // 32 ticks (1.6 seconds) floating reaction text

                if (reactedAura != null || trigger != null) {
                    entry.reactedAuraElement = reactedAura;
                    entry.dualTriggerElement = trigger;
                    entry.dualStartTime = now;
                    entry.dualUntil = now + 1300; // 26 ticks (1.3 seconds) Genshin dual-icon pop & fade
                }
            }
        }

        boolean spawnDamage = damage > 0.0F;
        if (spawnDamage) {
            entry.lastDamage = damage;
            entry.damageElement = trigger != null ? trigger : (reactedAura != null ? reactedAura : null);
            entry.damageStartTime = now;
            entry.damageUntil = now + 1500; // 30 ticks (1.5 seconds) floating damage number
        }

        if ((spawnReaction || spawnDamage) && !Double.isNaN(resolvedX)) {
            Element dmgElem = trigger != null ? trigger : reactedAura;
            addFloatingText(entityId, resolvedX, resolvedY, resolvedZ, spawnReaction ? reaction : null, spawnDamage ? damage : 0.0F, dmgElem);
        }

        if (isFrozenState || reaction == ElementalReaction.FROZEN) {
            entry.isFrozen = true;
            long remMs = durationTicks > 0 ? (long) durationTicks * 50L : 4000L;
            entry.frozenUntil = now + remMs;
            if (entry.frozenTotalMs <= 0L || reaction == ElementalReaction.FROZEN || remMs > entry.frozenTotalMs) {
                entry.frozenTotalMs = Math.max(500L, remMs);
            }
        } else {
            if (now >= entry.petrifiedUntil) {
                entry.isFrozen = false;
                entry.frozenUntil = 0L;
                entry.petrifiedUntil = 0L;
                entry.frozenTotalMs = 0L;
                entry.poseCaptured = false;
            }
        }

        if (entry.elements.isEmpty() && entry.permanentInnateElements.isEmpty() && entry.permanentInnateElement == null && !entry.permanentOmniInnate && !entry.isFrozen && entry.jadeShieldUntil <= now && entry.nightsFocusUntil <= now && entry.staffAnimUntil <= now && !entry.gliding && !entry.plunging && !entry.xilonenSkating && entry.xilonenSamplesUntil <= now && entry.neuvilletteActiveUntil <= now && entry.dragonfirePetUntil <= now && (!entry.dragonfireIdle && entry.dragonfireIdleFade <= 0.01F) && (!entry.nahidaIdleSwing && entry.nahidaIdleSwingFade <= 0.01F) && (!entry.zhongliIdleMeteor && entry.zhongliIdleFade <= 0.01F) && (!entry.raidenIdle && entry.raidenIdleFade <= 0.01F) && (!entry.mavuikaIdle && entry.mavuikaIdleFade <= 0.01F) && (entry.furinaIdleMode == 0 && entry.furinaIdleFade <= 0.01F) && (!entry.ventiIdle && entry.ventiIdleFade <= 0.01F) && (!entry.tsaritsaIdle && entry.tsaritsaIdleFade <= 0.01F) && (!entry.nahidaViewfinder && entry.nahidaViewfinderFade <= 0.01F) && entry.skandhaMarkedUntil <= now && entry.nahidaShrineUntil <= now && entry.nahidaWeaponHideUntil + 550L <= now && entry.tsaritsaSyncUntil <= now && entry.reactionUntil <= now && entry.dualUntil <= now && entry.damageUntil <= now) {
            DATA.remove(entityId);
        }
    }

    public static boolean hasPyroInnate(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && (data.permanentInnateElement == Element.PYRO || data.permanentInnateElements.contains(Element.PYRO));
    }

    public static boolean hasOmniInnate(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.permanentOmniInnate;
    }

    public static boolean hasJadeShield(net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        long now = System.currentTimeMillis();
        boolean graceActive = (now - jadeShieldAppliedTime) < 1500L || (now - crystallizeShieldAppliedTime) < 1500L;
        if (!graceActive && !entity.isSpectator() && !(entity instanceof net.minecraft.world.entity.player.Player p && p.isCreative()) && entity.getAbsorptionAmount() <= 0.01F) {
            return false;
        }
        net.minecraft.world.entity.player.Player localPlayer = ClientAccess.getLocalPlayer();
        if (localPlayer != null && entity.getId() == localPlayer.getId() && activeCrystallizeShield == Element.GEO && now < crystallizeShieldUntil) {
            return true;
        }
        AuraDisplayData data = DATA.get(entity.getId());
        return data != null && now < data.jadeShieldUntil;
    }

    public static boolean hasNightsFocusShield(net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        AuraDisplayData data = DATA.get(entity.getId());
        if (data == null) return false;
        long now = System.currentTimeMillis();
        if (now >= data.nightsFocusUntil) return false;
        boolean graceActive = (now - data.nightsFocusAppliedTime) < 1500L;
        if (!graceActive && !entity.isSpectator() && !(entity instanceof net.minecraft.world.entity.player.Player p && p.isCreative()) && entity.getAbsorptionAmount() <= 0.01F) {
            return false;
        }
        return true;
    }

    public static int getNightsFocusStars(net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null) return 0;
        AuraDisplayData data = DATA.get(entity.getId());
        return data != null ? Math.max(0, Math.min(4, data.nightsFocusStars)) : 0;
    }

    public static boolean isFrozen(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null || !data.isFrozen) return false;
        if (System.currentTimeMillis() >= data.frozenUntil) {
            data.isFrozen = false;
            data.poseCaptured = false;
            return false;
        }
        return true;
    }

    public static @Nullable AuraDisplayData get(int entityId) {
        return DATA.get(entityId);
    }

    public static final class MavuikaDisplayState {
        public int auraRemainingTicks;
        public float nightsoulPoints;
        public boolean netherite;
        public int ultiCooldownTicks;
        public long syncedAtMs;

        public float effectiveAuraTicks() {
            return Math.max(0.0F, auraRemainingTicks);
        }

        public float maxNightsoulFuel() {
            return netherite ? 120.0F : 80.0F;
        }

        public float nightsoulFuel() {
            return Math.max(0.0F, Math.min(maxNightsoulFuel(), auraRemainingTicks / 10.0F));
        }

        public float effectiveUltiCooldownTicks() {
            return Math.max(0.0F, ultiCooldownTicks - ((System.currentTimeMillis() - syncedAtMs) / 50.0F));
        }

        public boolean isUltiReady() {
            return nightsoulPoints >= 5.0F && effectiveUltiCooldownTicks() <= 0.0F;
        }
    }

    private static final java.util.Map<Integer, MavuikaDisplayState> MAVUIKA_STATES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void updateMavuika(int entityId, int auraRemainingTicks, float nightsoulPoints, boolean netherite, int ultiCooldownTicks) {
        if (auraRemainingTicks <= 0 && nightsoulPoints <= 0.001F && ultiCooldownTicks <= 0) {
            MAVUIKA_STATES.remove(entityId);
            return;
        }
        MavuikaDisplayState state = MAVUIKA_STATES.computeIfAbsent(entityId, id -> new MavuikaDisplayState());
        state.auraRemainingTicks = Math.max(0, auraRemainingTicks);
        state.nightsoulPoints = Math.max(0.0F, Math.min(10.0F, nightsoulPoints));
        state.netherite = netherite;
        state.ultiCooldownTicks = Math.max(0, ultiCooldownTicks);
        state.syncedAtMs = System.currentTimeMillis();
    }

    public static @Nullable MavuikaDisplayState getMavuika(int entityId) {
        MavuikaDisplayState state = MAVUIKA_STATES.get(entityId);
        if (state == null) return null;
        if (state.effectiveAuraTicks() <= 0.0F && state.nightsoulPoints <= 0.001F && state.effectiveUltiCooldownTicks() <= 0.0F) {
            MAVUIKA_STATES.remove(entityId);
            return null;
        }
        return state;
    }

    public static boolean isMavuikaAuraActive(int entityId) {
        MavuikaDisplayState state = getMavuika(entityId);
        return state != null && state.effectiveAuraTicks() > 0.0F;
    }

    public static final class RaidenDisplayState {
        public int eyeRemainingTicks;
        public int musouRemainingTicks;
        public int resolve;
        public boolean netherite;
        public int burstCooldownTicks;
        public long syncedAtMs;

        public float effectiveEyeTicks() {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - syncedAtMs);
            return Math.max(0.0F, eyeRemainingTicks - (elapsedMs / 50.0F));
        }

        public float effectiveMusouTicks() {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - syncedAtMs);
            return Math.max(0.0F, musouRemainingTicks - (elapsedMs / 50.0F));
        }

        public float effectiveBurstCooldownTicks() {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - syncedAtMs);
            return Math.max(0.0F, burstCooldownTicks - (elapsedMs / 50.0F));
        }
    }

    private static final java.util.Map<Integer, RaidenDisplayState> RAIDEN_STATES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void updateRaiden(int entityId, int eyeRemainingTicks, int musouRemainingTicks, int resolve, boolean netherite, int burstCooldownTicks) {
        if (eyeRemainingTicks <= 0 && musouRemainingTicks <= 0 && resolve <= 0 && burstCooldownTicks <= 0) {
            RAIDEN_STATES.remove(entityId);
            return;
        }
        RaidenDisplayState state = RAIDEN_STATES.computeIfAbsent(entityId, id -> new RaidenDisplayState());
        state.eyeRemainingTicks = Math.max(0, eyeRemainingTicks);
        state.musouRemainingTicks = Math.max(0, musouRemainingTicks);
        state.resolve = Math.max(0, Math.min(60, resolve));
        state.netherite = netherite;
        state.burstCooldownTicks = Math.max(0, burstCooldownTicks);
        state.syncedAtMs = System.currentTimeMillis();
    }

    public static @Nullable RaidenDisplayState getRaiden(int entityId) {
        RaidenDisplayState state = RAIDEN_STATES.get(entityId);
        if (state == null) return null;
        if (state.effectiveEyeTicks() <= 0.0F && state.effectiveMusouTicks() <= 0.0F && state.resolve <= 0 && state.effectiveBurstCooldownTicks() <= 0.0F) {
            RAIDEN_STATES.remove(entityId);
            return null;
        }
        return state;
    }

    public static boolean isRaidenEyeActive(int entityId) {
        RaidenDisplayState state = getRaiden(entityId);
        return state != null && state.effectiveEyeTicks() > 0.0F;
    }

    public static boolean isRaidenMusouActive(int entityId) {
        RaidenDisplayState state = getRaiden(entityId);
        return state != null && state.effectiveMusouTicks() > 0.0F;
    }

    public static boolean isRaidenSlashingOrBursting(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null || System.currentTimeMillis() >= data.staffAnimUntil) return false;
        int anim = data.staffAnimType;
        return anim == ANIM_RAIDEN_DRAW || anim == ANIM_RAIDEN_SLASH || anim == ANIM_RAIDEN_CA;
    }

    public static boolean isSlotSwitchLocked(int entityId) {
        if (isUltiCastActive(entityId) || isRaidenSlashingOrBursting(entityId)) {
            return true;
        }
        AuraDisplayData data = DATA.get(entityId);
        if (data == null) return false;
        long now = System.currentTimeMillis();
        if (now >= data.staffAnimUntil || data.staffAnimType <= 0) return false;
        int anim = data.staffAnimType;
        if (anim == ANIM_PLUNGE_START || anim == ANIM_PLUNGE_END
                || anim == ANIM_GLIDER_START || anim == ANIM_GLIDER_STOP
                || anim == ANIM_GLIDER_UNLOCKED || anim == ANIM_GLIDER_LOCKED
                || anim == ANIM_DRAGONFIRE_PET_OFF || anim == ANIM_DRAGONFIRE_PET_ON
                || anim == ANIM_DRAGONFIRE_IDLE_ON
                || anim == ANIM_DASH_IMPULSE
                || anim == ANIM_STAMINA_TIER_100 || anim == ANIM_STAMINA_TIER_170 || anim == ANIM_STAMINA_TIER_240) {
            return false;
        }
        if (anim == ANIM_DRAGONFIRE_ULTI_SKYFALL) {
            return (now - data.staffAnimStartTime) < 850L;
        }
        if (anim == ANIM_DRAGONFIRE_ULTI_SWEEP) {
            return (now - data.staffAnimStartTime) < 1500L;
        }
        return true;
    }

    public static float getDragonfireSkyfallShakeIntensity(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null || data.staffAnimType != ANIM_DRAGONFIRE_ULTI_SKYFALL) return 0.0F;
        long elapsed = System.currentTimeMillis() - data.staffAnimStartTime;
        if (elapsed < 560L || elapsed > 950L) return 0.0F;
        return 1.0F - (float) (elapsed - 560L) / 390.0F;
    }


    public static boolean isXilonenSkating(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.xilonenSkating && System.currentTimeMillis() < data.xilonenSkateSyncUntil;
    }

    public static boolean isXilonenWallClimbing(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.xilonenSkating && data.xilonenWallClimbing && System.currentTimeMillis() < data.xilonenSkateSyncUntil;
    }

    public static float getXilonenJumpFlipProgress(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null || !data.xilonenSkating || data.xilonenWallClimbing || data.plunging || data.gliding) {
            return -1.0F;
        }
        long now = System.currentTimeMillis();
        if (now >= data.xilonenJumpFlipUntil || data.xilonenJumpFlipStartTime <= 0L) {
            return -1.0F;
        }
        long elapsed = now - data.xilonenJumpFlipStartTime;
        if (elapsed < 0L) return 0.0F;
        net.minecraft.world.level.Level clientLevel = ClientAccess.getClientLevel();
        if (elapsed > 120L && clientLevel != null && clientLevel.getEntity(entityId) instanceof net.minecraft.world.entity.LivingEntity le && le.onGround()) {
            data.xilonenJumpFlipUntil = 0L;
            return -1.0F;
        }
        float totalMs = Math.max(1.0F, (float) (data.xilonenJumpFlipUntil - data.xilonenJumpFlipStartTime));
        return net.minecraft.util.Mth.clamp(elapsed / totalMs, 0.0F, 1.0F);
    }

    public static boolean isXilonenJumpFlipping(int entityId) {
        return getXilonenJumpFlipProgress(entityId) >= 0.0F;
    }

    public static boolean hasXilonenSourceSamples(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.xilonenSampleMask > 0 && System.currentTimeMillis() < data.xilonenSamplesUntil;
    }

    public static void stopGliding(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data != null) {
            data.gliding = false;
        }
    }

    public static void stopPlunging(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data != null) {
            data.plunging = false;
            if (data.staffAnimType == ANIM_PLUNGE_START) {
                data.staffAnimUntil = 0L;
                data.staffAnimType = 0;
            }
        }
    }

    public static boolean isUltiCastActive(int entityId) {
        if (isFiveStarBurstRootActive(entityId)) return true;
        AuraDisplayData data = DATA.get(entityId);
        if (data != null && System.currentTimeMillis() < data.staffAnimUntil && data.staffAnimType == ANIM_MAVUIKA_BURST) {
            return true;
        }
        net.minecraft.world.level.Level clientLevel = ClientAccess.getClientLevel();
        net.minecraft.world.entity.Entity ent = clientLevel != null ? clientLevel.getEntity(entityId) : null;
        if (ent != null
                && ent.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike
                && bike.getUltiTicks() > 0) {
            return true;
        }
        return false;
    }

    public static void interruptActiveAnimations(int entityId) {
        if (isSlotSwitchLocked(entityId)) {
            return;
        }
        AuraDisplayData data = DATA.get(entityId);
        if (data != null) {
            data.staffAnimUntil = 0L;
            data.staffAnimType = 0;
            data.plunging = false;
            data.xilonenJumpFlipUntil = 0L;
        }
    }

    public static boolean isGliding(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null || !data.gliding) return false;
        net.minecraft.world.level.Level clientLevel = ClientAccess.getClientLevel();
        if (clientLevel != null && clientLevel.getEntity(entityId) instanceof net.minecraft.world.entity.LivingEntity le) {
            if (le.onGround() || le.isInWater() || le.isInLava() || le.getVehicle() != null
                    || (le instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().flying)) {
                data.gliding = false;
                return false;
            }
        }
        return true;
    }

    public static boolean isPlunging(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null || !data.plunging) return false;
        net.minecraft.world.level.Level clientLevel = ClientAccess.getClientLevel();
        if (clientLevel != null && clientLevel.getEntity(entityId) instanceof net.minecraft.world.entity.LivingEntity le) {
            if (le.onGround() || le.isInWater() || le.isInLava() || le.getVehicle() != null
                    || (le instanceof net.minecraft.world.entity.player.Player p && (p.getAbilities().flying || com.velorise.staves.element.ElementalGaugeManager.isStandingOnWaterSurface(p)))) {
                data.plunging = false;
                return false;
            }
        }
        return true;
    }

    public static boolean hasDragonfirePet(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && System.currentTimeMillis() < data.dragonfirePetUntil;
    }

    public static boolean isDragonfireIdle(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && (data.dragonfireIdle || data.dragonfireIdleFade > 0.01F);
    }

    public static boolean hasLaylaDreamsphere(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && System.currentTimeMillis() < data.laylaDreamsphereUntil;
    }

    public static java.util.Collection<AuraDisplayData> getAllDisplayData() {
        return DATA.values();
    }

    public static boolean isFiveStarBurstRootActive(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null || System.currentTimeMillis() >= data.staffAnimUntil) return false;
        int anim = data.staffAnimType;
        return anim == ANIM_ZHONGLI_PLANET_BEFALL
                || anim == ANIM_ZHONGLI_PLANET_BEFALL_FIRST
                || anim == ANIM_VORTEX_HOLD_BURST
                || anim == ANIM_VENTI_BURST
                || anim == ANIM_NAHIDA_BURST
                || anim == ANIM_FURINA_BURST
                || anim == ANIM_NEUVILLETTE_BURST
                || anim == ANIM_RAIDEN_DRAW
                || anim == ANIM_XILONEN_BURST
                || anim == ANIM_MAVUIKA_BURST
                || anim == ANIM_LAYLA_BURST
                || anim == ANIM_TSARITSA_BURST_SUPPORT
                || anim == ANIM_TSARITSA_BURST_DPS
                || anim == ANIM_DRAGONFIRE_ULTI_SKYFALL
                || anim == ANIM_DRAGONFIRE_ULTI_SWEEP;
    }

    public static void resetLocalStaminaState() {
        localStamina = localMaxStamina;
        localStaminaRegenDelayTicks = 0;
        localStaminaExhausted = false;
        localStaminaFullHideTicks = 0;
        wasSprintKeyDownForDash = false;
        localDashCooldownTicks = 0;
        localDashChainCount = 0;
        localDashChainResetTicks = 0;
        localSteveWallClimbing = false;
        localMovementState = com.velorise.staves.fsm.MovementActionState.GROUNDED;
        localMoveElapsedTicks = 0;
        localWallJumpTicks = 0;
        localMantleTicks = 0;
        localWallNormalFace = null;
        wasWallJumpKeyDown = false;
        localExhaustedClimbFall = false;
        localWallInputBuffer.clear();
    }

    public static void tickLocalStaminaAndDash(
            net.minecraft.world.entity.player.Player player,
            boolean sprintKeyDown,
            byte toggleGliderAction,
            byte dashAction
    ) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            wasSprintKeyDownForDash = false;
            localDashChainCount = 0;
            localDashChainResetTicks = 0;
            if (localSteveWallClimbing) {
                localSteveWallClimbing = false;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 30);
            }
            return;
        }
        if (localDashCooldownTicks > 0) {
            localDashCooldownTicks--;
        }
        if (localDashChainResetTicks > 0) {
            localDashChainResetTicks--;
            if (localDashChainResetTicks == 0) {
                localDashChainCount = 0;
            }
        }

        boolean customStamina = com.velorise.staves.config.StavesConfig.isEnableCustomStamina();
        boolean customDash = com.velorise.staves.config.StavesConfig.isEnableCustomDash();
        boolean creative = player.isCreative();
        int pid = player.getId();

        if (!customStamina) {
            localStamina = localMaxStamina;
            localStaminaExhausted = false;
            localStaminaRegenDelayTicks = 0;
            localStaminaFullHideTicks = 0;
        }

        boolean blocked = isUltiCastActive(pid)
                || isFrozen(pid)
                || player.getVehicle() != null
                || isPlunging(pid)
                || isGliding(pid)
                || isXilonenSkating(pid)
                || isNeuvilletteChargingOrFiring(pid)
                || player.getAbilities().flying
                || player.isUsingItem();

        if (customDash && sprintKeyDown && !wasSprintKeyDownForDash && !blocked && localDashCooldownTicks <= 0
                && (player.onGround() || player.isInWater())) {
            boolean enoughStamina = !customStamina || creative || (!localStaminaExhausted && localStamina >= 18.0F);
            if (enoughStamina) {
                tickLocalWallClimbMachine(player, customStamina, creative, blocked);

        if (customStamina && !creative) {
                    localStamina = Math.max(0.0F, localStamina - 18.0F);
                    localStaminaRegenDelayTicks = 30;
                    if (localStamina <= 0.01F) {
                        localStamina = 0.0F;
                        localStaminaExhausted = true;
                    }
                }
                localDashChainCount++;
                if (localDashChainCount >= 2) {
                    localDashChainCount = 0;
                    localDashChainResetTicks = 0;
                    localDashCooldownTicks = 16;
                } else {
                    localDashChainResetTicks = 28;
                    localDashCooldownTicks = 3;
                }
                triggerStaffAnimation(pid, ANIM_DASH_IMPULSE);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(dashAction);

                float fwdIn = ClientAccess.getForwardImpulse(player);
                float leftIn = ClientAccess.getLeftImpulse(player);
                double yawRad = Math.toRadians(player.getYRot());
                double fwdX = -Math.sin(yawRad);
                double fwdZ = Math.cos(yawRad);
                double leftX = Math.cos(yawRad);
                double leftZ = Math.sin(yawRad);
                double dirX = fwdX * fwdIn + leftX * leftIn;
                double dirZ = fwdZ * fwdIn + leftZ * leftIn;
                double len = Math.hypot(dirX, dirZ);
                if (len < 1.0E-4D) {
                    dirX = fwdX;
                    dirZ = fwdZ;
                } else {
                    dirX /= len;
                    dirZ /= len;
                }
                double dashSpeed = player.isInWater() ? 0.44D : 0.72D;
                net.minecraft.world.phys.Vec3 curVel = player.getDeltaMovement();
                player.setDeltaMovement(dirX * dashSpeed, Math.max(curVel.y, 0.0D), dirZ * dashSpeed);
                if (!customStamina || creative || !localStaminaExhausted) {
                    player.setSprinting(true);
                }
            }
        }
        wasSprintKeyDownForDash = sprintKeyDown;

        if (customDash && sprintKeyDown && !blocked && (!customStamina || creative || !localStaminaExhausted)
                && ClientAccess.getForwardImpulse(player) > 0.01F && !player.isShiftKeyDown()) {
            player.setSprinting(true);
        }

        if (customStamina && !creative) {
            boolean glidingNow = isGliding(pid) && !player.onGround() && !player.getAbilities().flying && player.getVehicle() == null;
            boolean wallClimbingNow = localSteveWallClimbing || localMovementState.isWallState();
            boolean sprintingNow = !glidingNow && !wallClimbingNow && player.isSprinting() && !player.getAbilities().flying
                    && player.getVehicle() == null && !isXilonenSkating(pid) && !blocked;
            if (glidingNow) {
                float drain = com.velorise.staves.element.VentiManager.hasVentiWeapon(player) ? 0.15F : 0.30F;
                localStamina = Math.max(0.0F, localStamina - drain);
                localStaminaRegenDelayTicks = 25;
                if (localStamina <= 0.0F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    stopGliding(pid);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(toggleGliderAction);
                }
            } else if (wallClimbingNow) {
                localStaminaRegenDelayTicks = 25;
            } else if (sprintingNow) {
                localStamina = Math.max(0.0F, localStamina - 0.45F);
                localStaminaRegenDelayTicks = 25;
                if (localStamina <= 0.0F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    player.setSprinting(false);
                }
            } else {
                if (localStaminaRegenDelayTicks > 0) {
                    localStaminaRegenDelayTicks--;
                } else if (localStamina < localMaxStamina) {
                    localStamina = Math.min(localMaxStamina, localStamina + 1.25F);
                }
                if (localStaminaExhausted && localStamina >= 20.0F) {
                    localStaminaExhausted = false;
                }
            }
            if (localStaminaExhausted) {
                player.setSprinting(false);
            }
            if (localStamina < localMaxStamina - 0.01F) {
                localStaminaFullHideTicks = 35;
            } else if (localStaminaFullHideTicks > 0) {
                localStaminaFullHideTicks--;
            }
        }
    }

    public static boolean isNeuvilletteChargingOrFiring(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && System.currentTimeMillis() < data.neuvilletteActiveUntil;
    }

    public static boolean isNeuvilletteFiringBeam(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.neuvilletteFiringBeam && System.currentTimeMillis() < data.neuvilletteActiveUntil;
    }

    public static boolean isNahidaIdleSwing(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && (data.nahidaIdleSwing || data.nahidaIdleSwingFade > 0.01F);
    }

    public static boolean isZhongliIdleMeteor(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.zhongliIdleMeteor;
    }

    public static boolean isRaidenIdle(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && (data.raidenIdle || data.raidenIdleFade > 0.01F);
    }

    public static boolean isMavuikaIdle(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && (data.mavuikaIdle || data.mavuikaIdleFade > 0.01F);
    }

    public static boolean isFurinaIdle(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && (data.furinaIdleMode > 0 || data.furinaIdleFade > 0.01F);
    }

    public static boolean isVentiIdle(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null) return false;
        long now = System.currentTimeMillis();
        boolean activeBurst = data.staffAnimType == ANIM_VENTI_BURST && now < data.staffAnimUntil;
        net.minecraft.world.entity.player.Player localPlayer = ClientAccess.getLocalPlayer();
        boolean localPulling = localPlayer != null && entityId == localPlayer.getId()
                && (localDaybreakBowPullTicks > 0 || ventiUltiHoldTicks > 0 || localPlayer.isUsingItem() || localPlayer.swinging);
        if (activeBurst || localPulling) {
            data.ventiIdle = false;
            data.ventiIdleMode = 0;
            data.ventiIdleFade = 0.0F;
            data.ventiIdleYawSet = false;
            return false;
        }
        return data.ventiIdle || data.ventiIdleFade > 0.01F;
    }

    public static void syncIdleLockedYaw(Object mcObj) {
        if (mcObj == null) return;
        ClientAccess.syncIdleLockedYawInternal();
    }

    private static void syncIdleLockedYawWithPlayer(net.minecraft.world.entity.player.Player localPlayer, boolean isFirstPerson, boolean hasMovementInput) {
        if (localPlayer == null || localPlayer.level() == null) return;
        AuraDisplayData d = DATA.get(localPlayer.getId());
        if (d == null) return;
        boolean movingOrActing = localPlayer.isShiftKeyDown() || localPlayer.isCrouching() || localPlayer.isUsingItem() || localPlayer.swinging
                || !localPlayer.onGround() || localPlayer.getVehicle() != null
                || hasMovementInput;
        long nowUlti = System.currentTimeMillis();
        boolean inUlti = nowUlti < d.staffAnimUntil && (
                d.staffAnimType == ANIM_RAIDEN_DRAW
                || d.staffAnimType == ANIM_ZHONGLI_PLANET_BEFALL
                || d.staffAnimType == ANIM_ZHONGLI_PLANET_BEFALL_FIRST
                || d.staffAnimType == ANIM_VORTEX_HOLD_BURST
                || d.staffAnimType == ANIM_VENTI_BURST
                || d.staffAnimType == ANIM_NAHIDA_BURST
                || d.staffAnimType == ANIM_MAVUIKA_BURST
                || d.staffAnimType == ANIM_TSARITSA_BURST_SUPPORT
                || d.staffAnimType == ANIM_TSARITSA_BURST_DPS
        );
        boolean isFirstPersonUlti = isFirstPerson;
        if (inUlti) {
            if (isFirstPersonUlti || !d.ultiYawSet) {
                d.ultiYaw = localPlayer.getYRot();
                d.ultiYawSet = true;
            }
            localPlayer.yBodyRot = d.ultiYaw;
            localPlayer.yBodyRotO = d.ultiYaw;
        } else {
            d.ultiYawSet = false;
        }
        if (movingOrActing) {
            d.tsaritsaIdle = false; d.tsaritsaIdleFade = 0.0F; d.tsaritsaIdleYawSet = false;
            d.raidenIdle = false; d.raidenIdleFade = 0.0F; d.raidenIdleYawSet = false;
            d.mavuikaIdle = false; d.mavuikaIdleFade = 0.0F; d.mavuikaIdleYawSet = false;
            d.ventiIdle = false; d.ventiIdleFade = 0.0F; d.ventiIdleYawSet = false; d.ventiIdleMode = 0;
            d.zhongliIdleMeteor = false; d.zhongliIdleFade = 0.0F; d.zhongliIdleYawSet = false;
            d.furinaIdleMode = 0; d.furinaIdleFade = 0.0F; d.furinaIdleYawSet = false;
            d.nahidaIdleSwing = false; d.nahidaIdleSwingFade = 0.0F; d.nahidaIdleYawSet = false;
            d.dragonfireIdle = false; d.dragonfireIdleFade = 0.0F; d.dragonfireIdleYawSet = false;
            return;
        }
        if (isTsaritsaIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.tsaritsaIdleYawSet) {
                d.tsaritsaIdleYaw = localPlayer.getYRot();
                d.tsaritsaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.tsaritsaIdleYaw;
            localPlayer.yBodyRotO = d.tsaritsaIdleYaw;
        } else {
            d.tsaritsaIdleYawSet = false;
        }
        if (isRaidenIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.raidenIdleYawSet) {
                d.raidenIdleYaw = localPlayer.getYRot();
                d.raidenIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.raidenIdleYaw;
            localPlayer.yBodyRotO = d.raidenIdleYaw;
        } else {
            d.raidenIdleYawSet = false;
        }
        if (isMavuikaIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.mavuikaIdleYawSet) {
                d.mavuikaIdleYaw = localPlayer.getYRot();
                d.mavuikaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.mavuikaIdleYaw;
            localPlayer.yBodyRotO = d.mavuikaIdleYaw;
        } else {
            d.mavuikaIdleYawSet = false;
        }
        if (isVentiIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.ventiIdleYawSet) {
                d.ventiIdleYaw = localPlayer.getYRot();
                d.ventiIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.ventiIdleYaw;
            localPlayer.yBodyRotO = d.ventiIdleYaw;
        } else {
            d.ventiIdleYawSet = false;
        }
        if (isZhongliIdleMeteor(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.zhongliIdleYawSet) {
                d.zhongliIdleYaw = localPlayer.getYRot();
                d.zhongliIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.zhongliIdleYaw;
            localPlayer.yBodyRotO = d.zhongliIdleYaw;
        } else {
            d.zhongliIdleYawSet = false;
        }
        if (isFurinaIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.furinaIdleYawSet) {
                d.furinaIdleYaw = localPlayer.getYRot();
                d.furinaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.furinaIdleYaw;
            localPlayer.yBodyRotO = d.furinaIdleYaw;
        } else {
            d.furinaIdleYawSet = false;
        }
        if (isNahidaIdleSwing(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (isFirstPerson || !d.nahidaIdleYawSet) {
                d.nahidaIdleYaw = localPlayer.getYRot();
                d.nahidaIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.nahidaIdleYaw;
            localPlayer.yBodyRotO = d.nahidaIdleYaw;
        } else {
            d.nahidaIdleYawSet = false;
        }
        if (isDragonfireIdle(localPlayer.getId()) && !localPlayer.isUsingItem() && !localPlayer.swinging) {
            if (!d.dragonfireIdleYawSet) {
                d.dragonfireIdleYaw = localPlayer.getYRot();
                d.dragonfireIdleYawSet = true;
            }
            localPlayer.yBodyRot = d.dragonfireIdleYaw;
            localPlayer.yBodyRotO = d.dragonfireIdleYaw;
        } else {
            d.dragonfireIdleYawSet = false;
        }
    }

    public static boolean isTsaritsaIdle(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        if (data == null) return false;
        long now = System.currentTimeMillis();
        boolean activeAction = data.staffAnimType >= ANIM_TSARITSA_BOW_NA
                && data.staffAnimType <= ANIM_TSARITSA_UNSEAL
                && now < data.staffAnimUntil;
        net.minecraft.world.entity.player.Player localPlayer = ClientAccess.getLocalPlayer();
        boolean localCharging = localPlayer != null && entityId == localPlayer.getId()
                && (localTsaritsaBowPullTicks > 0 || localTsaritsaHoldETicks > 0);
        if (activeAction || localCharging) {
            data.tsaritsaIdle = false;
            data.tsaritsaIdleFade = 0.0F;
            data.tsaritsaIdleYawSet = false;
            return false;
        }
        return data.tsaritsaIdle || data.tsaritsaIdleFade > 0.01F;
    }

    public static boolean isNahidaViewfinder(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.nahidaViewfinder;
    }

    public static boolean hasSkandhaMark(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && System.currentTimeMillis() < data.skandhaMarkedUntil;
    }

    public static boolean hasNahidaShrine(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.nahidaShrineMask > 0 && System.currentTimeMillis() < data.nahidaShrineUntil;
    }

    public static boolean isTsaritsaCrownActive(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.tsaritsaCrownActive && !data.tsaritsaSpearStance && System.currentTimeMillis() < data.tsaritsaSyncUntil;
    }

    public static boolean isTsaritsaSpearStance(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.tsaritsaSpearStance && System.currentTimeMillis() < data.tsaritsaSyncUntil;
    }

    public static boolean isTsaritsaSupportBurst(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.tsaritsaSupportBurst && System.currentTimeMillis() < data.tsaritsaSyncUntil;
    }

    public static boolean isTsaritsaDpsSeal(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.tsaritsaDpsSeal && System.currentTimeMillis() < data.tsaritsaSyncUntil;
    }

    public static void remove(int entityId) {
        DATA.remove(entityId);
        MAVUIKA_STATES.remove(entityId);
        RAIDEN_STATES.remove(entityId);
    }

    public static void clear() {
        com.velorise.staves.audio.CharacterVoiceManager.resetWorldSession();
        DATA.clear();
        MAVUIKA_STATES.clear();
        RAIDEN_STATES.clear();
        ACTIVE_ARCS.clear();
        FLOATING_TEXTS.clear();
        activeCrystallizeShield = null;
        crystallizeShieldUntil = 0L;
        localResonanceMask = 0;
        localOverloaded = false;
        localStaffCount = 0;
        localWindGliderUnlocked = false;
        localMaxStamina = 100.0F;
        resetLocalStaminaState();
        localTsaritsaHoldETicks = 0;
        localTsaritsaHoldELocked = false;
        localTsaritsaBowPullTicks = 0;
        localFurinaCaHoldProgress = 0.0F;
        localDaybreakBowPullTicks = 0;
        lastVentiSkillETimeMs = 0L;
        com.velorise.staves.client.glider.WindGliderRenderer.clearClientCaches();
        com.velorise.staves.client.character.VentiVisualRenderer.clearClientCaches();
        com.velorise.staves.client.character.TsaritsaVisualRenderer.clearClientCaches();
        com.velorise.staves.client.character.MavuikaVisualRenderer.clearClientCaches();
        com.velorise.staves.client.character.DragonfireVisualRenderer.clearClientCaches();
        com.velorise.staves.client.reaction.ElementalReactionBadgeRenderer.resetFrozenClientState();
    }

    public static boolean isOnClientMainThread() {
        return ClientAccess.isSameThread();
    }

    public static boolean isLocalPlayer(@Nullable net.minecraft.world.entity.Entity entity) {
        return entity != null && entity.level().isClientSide() && ClientAccess.getLocalPlayer() == entity;
    }

    private static final class ClientAccess {
        static @Nullable net.minecraft.world.entity.player.Player getLocalPlayer() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null ? mc.player : null;
        }

        static @Nullable net.minecraft.world.level.Level getClientLevel() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null ? mc.level : null;
        }

        static boolean isSameThread() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null && mc.isSameThread();
        }

        static boolean isFirstPerson() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null && mc.options != null && mc.options.getCameraType().isFirstPerson();
        }

        static float getForwardImpulse(net.minecraft.world.entity.player.Player player) {
            if (player instanceof net.minecraft.client.player.LocalPlayer lp && lp.input != null) {
                return lp.input.forwardImpulse;
            }
            return 0.0F;
        }

        static float getLeftImpulse(net.minecraft.world.entity.player.Player player) {
            if (player instanceof net.minecraft.client.player.LocalPlayer lp && lp.input != null) {
                return lp.input.leftImpulse;
            }
            return 0.0F;
        }

        static void syncIdleLockedYawInternal() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) return;
            boolean hasMove = mc.player.input != null
                    && (Math.abs(mc.player.input.forwardImpulse) > 0.01F
                    || Math.abs(mc.player.input.leftImpulse) > 0.01F
                    || mc.player.input.jumping);
            boolean firstPerson = mc.options != null && mc.options.getCameraType().isFirstPerson();
            syncIdleLockedYawWithPlayer(mc.player, firstPerson, hasMove);
        }
    }

    public static @Nullable Element getTrackedCrystallizeShield() {
        net.minecraft.world.entity.player.Player localPlayer = ClientAccess.getLocalPlayer();
        if (localPlayer == null) return null;
        if (activeCrystallizeShield != null) {
            long now = System.currentTimeMillis();
            boolean graceActive = (now - crystallizeShieldAppliedTime) < 1500L;
            boolean depleted = !graceActive && !localPlayer.isCreative() && localPlayer.getAbsorptionAmount() <= 0.0F;
            if (now >= crystallizeShieldUntil || depleted) {
                activeCrystallizeShield = null;
                return null;
            }
            return activeCrystallizeShield;
        }
        return null;
    }

    public static @Nullable Element getActiveCrystallizeShield() {
        net.minecraft.world.entity.player.Player localPlayer = ClientAccess.getLocalPlayer();
        if (localPlayer == null) return null;
        Element mobShield = ModMobEffects.getActiveShield(localPlayer);
        if (mobShield != null) {
            if (activeCrystallizeShield != mobShield || crystallizeShieldUntil <= System.currentTimeMillis()) {
                activeCrystallizeShield = mobShield;
                crystallizeShieldUntil = System.currentTimeMillis() + 15000L;
            }
            return mobShield;
        }
        return getTrackedCrystallizeShield();
    }

    public static boolean isSteveWallClimbing(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.steveWallClimbing;
    }

    public static boolean isLocalExhaustedClimbFall() {
        return localExhaustedClimbFall;
    }

    public static net.minecraft.core.Direction getWallNormalFace(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null ? data.wallNormalFace : null;
    }

    private static void exitLocalWallClimb(net.minecraft.world.entity.player.Player player, boolean exhausted) {
        int pid = player.getId();
        boolean wasClimbing = localSteveWallClimbing || localMovementState.isWallState();
        localSteveWallClimbing = false;
        localWallNormalFace = null;
        if (player != null && !player.getAbilities().flying && player.getVehicle() == null) {
            player.setNoGravity(false);
        }
        localMoveElapsedTicks = 0;
        localWallJumpTicks = 0;
        localMantleTicks = 0;
        localWallInputBuffer.clear();
        AuraDisplayData entry = DATA.get(pid);
        if (entry != null) {
            entry.steveWallClimbing = false;
            entry.wallNormalFace = null;
        }
        if (exhausted) {
            localMovementState = com.velorise.staves.fsm.MovementActionState.EXHAUSTED_FALL;
            localExhaustedClimbFall = true;
            if (wasClimbing) {
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 32);
            }
        } else {
            localMovementState = player.onGround()
                    ? com.velorise.staves.fsm.MovementActionState.GROUNDED
                    : com.velorise.staves.fsm.MovementActionState.GROUNDED;
            if (wasClimbing) {
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 30);
            }
        }
    }

    private static void tickLocalWallClimbMachine(
            net.minecraft.world.entity.player.Player player,
            boolean customStamina,
            boolean creative,
            boolean blockedByOtherLayer
    ) {
        int pid = player.getId();
        localWallInputBuffer.tick();

        if (player.onGround() || player.isInWater() || player.isInLava()) {
            localExhaustedClimbFall = false;
            if (localMovementState == com.velorise.staves.fsm.MovementActionState.EXHAUSTED_FALL) {
                localMovementState = com.velorise.staves.fsm.MovementActionState.GROUNDED;
            }
        }

        boolean wallClimbEnabled = com.velorise.staves.config.StavesConfig.isEnableWallClimb();
        boolean higherPriorityActive = blockedByOtherLayer
                || !wallClimbEnabled
                || player.onClimbable()
                || player.isInWater()
                || player.isInLava()
                || player.getAbilities().flying
                || player.getVehicle() != null
                || isXilonenSkating(pid)
                || isGliding(pid)
                || isPlunging(pid);

        if (higherPriorityActive) {
            if (localSteveWallClimbing || localMovementState.isWallState()) {
                exitLocalWallClimb(player, false);
            }
            wasWallJumpKeyDown = false;
            return;
        }

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        boolean jumpDown = mc != null && mc.options != null && mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !wasWallJumpKeyDown;
        wasWallJumpKeyDown = jumpDown;

        float fwdIn = ClientAccess.getForwardImpulse(player);
        float leftIn = ClientAccess.getLeftImpulse(player);
        boolean sneakDown = player.isShiftKeyDown();

        if (localSteveWallClimbing && jumpPressed) {
            localWallInputBuffer.push(com.velorise.staves.fsm.MovementActionState.BufferedInput.CLIMB_JUMP, 4);
        }

        // Active ledge mantle (6 ticks)
        if (localMovementState == com.velorise.staves.fsm.MovementActionState.LEDGE_MANTLE) {
            localMoveElapsedTicks++;
            localMantleTicks--;
            net.minecraft.core.Direction wallFace = localWallNormalFace;
            if (wallFace != null) {
                net.minecraft.core.Direction inward = wallFace.getOpposite();
                player.setDeltaMovement(inward.getStepX() * 0.24D, 0.34D, inward.getStepZ() * 0.24D);
                player.fallDistance = 0.0F;
            }
            if (localMantleTicks <= 0 || player.onGround()) {
                exitLocalWallClimb(player, false);
            }
            return;
        }

        // Active climb jump (8 ticks, cancel window at tick 5)
        if (localMovementState == com.velorise.staves.fsm.MovementActionState.WALL_JUMP) {
            localMoveElapsedTicks++;
            localWallJumpTicks--;
            player.fallDistance = 0.0F;
            if (localWallJumpTicks <= 0) {
                net.minecraft.core.Direction regrab = com.velorise.staves.element.WallClimbHelper.findClimbWall(
                        player, com.velorise.staves.element.WallClimbHelper.CLIENT_WALL_PROBE_DIST + 0.15D);
                if (regrab != null && (!customStamina || creative || localStamina > 0.01F)) {
                    localWallNormalFace = regrab;
                    localMovementState = com.velorise.staves.fsm.MovementActionState.WALL_CLING;
                    localMoveElapsedTicks = 0;
                    AuraDisplayData entry = DATA.computeIfAbsent(pid, k -> new AuraDisplayData());
                    entry.wallNormalFace = regrab;
                } else {
                    exitLocalWallClimb(player, customStamina && !creative && localStamina <= 0.01F);
                }
            }
            return;
        }

        // Manual drop-off via Shift
        if (localSteveWallClimbing && sneakDown) {
            exitLocalWallClimb(player, false);
            return;
        }

        net.minecraft.core.Direction wallFace = com.velorise.staves.element.WallClimbHelper.findClimbWall(
                player, com.velorise.staves.element.WallClimbHelper.CLIENT_WALL_PROBE_DIST);

        if (!localSteveWallClimbing) {
            boolean enoughToStart = !customStamina || creative || (!localStaminaExhausted && !localExhaustedClimbFall && localStamina >= 5.0F);
            boolean wantsAttach = wallFace != null && !sneakDown && enoughToStart
                    && fwdIn > 0.10F;
            if (wantsAttach) {
                localSteveWallClimbing = true;
                localWallNormalFace = wallFace;
                localMovementState = com.velorise.staves.fsm.MovementActionState.WALL_CLING;
                localMoveElapsedTicks = 0;
                AuraDisplayData entry = DATA.computeIfAbsent(pid, k -> new AuraDisplayData());
                entry.steveWallClimbing = true;
                entry.wallNormalFace = wallFace;
                player.setNoGravity(true);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 29);
            } else {
                return;
            }
        }

        // Already climbing: validate wall contact
        if (wallFace == null) {
            exitLocalWallClimb(player, false);
            return;
        }
        localWallNormalFace = wallFace;
        AuraDisplayData entry = DATA.computeIfAbsent(pid, k -> new AuraDisplayData());
        entry.steveWallClimbing = true;
        entry.wallNormalFace = wallFace;
        localMoveElapsedTicks++;

        // Check Ledge Mantle when pushing forward near top edge
        if (fwdIn > 0.10F && com.velorise.staves.element.WallClimbHelper.canLedgeMantle(player, wallFace)) {
            localMovementState = com.velorise.staves.fsm.MovementActionState.LEDGE_MANTLE;
            localMoveElapsedTicks = 0;
            localMantleTicks = com.velorise.staves.fsm.MovementActionState.LEDGE_MANTLE_DURATION_TICKS;
            entry.steveWallMantleUntilMs = System.currentTimeMillis() + 320L;
            return;
        }

        // Consume buffered Climb Jump (Space = -25 Stamina)
        if (localWallInputBuffer.consumeIf(i -> i == com.velorise.staves.fsm.MovementActionState.BufferedInput.CLIMB_JUMP) != null) {
            boolean canJump = !customStamina || creative || localStamina >= 25.0F;
            if (canJump) {
                if (customStamina && !creative) {
                    localStamina = Math.max(0.0F, localStamina - 25.0F);
                    localStaminaRegenDelayTicks = 30;
                }
                if (fwdIn < -0.20F) {
                    // Authentic Genshin: downward + jump kicks away from wall
                    player.setDeltaMovement(wallFace.getStepX() * 0.42D, 0.38D, wallFace.getStepZ() * 0.42D);
                    exitLocalWallClimb(player, false);
                    return;
                }
                // Upward / diagonal climb leap along the wall
                double rightX = -wallFace.getStepZ();
                double rightZ = wallFace.getStepX();
                net.minecraft.core.Direction inward = wallFace.getOpposite();
                double strafe = -leftIn * 0.22D;
                player.setDeltaMovement(
                        inward.getStepX() * 0.08D + rightX * strafe,
                        0.46D,
                        inward.getStepZ() * 0.08D + rightZ * strafe
                );
                player.fallDistance = 0.0F;
                localMovementState = com.velorise.staves.fsm.MovementActionState.WALL_JUMP;
                localMoveElapsedTicks = 0;
                localWallJumpTicks = com.velorise.staves.fsm.MovementActionState.WALL_JUMP_DURATION_TICKS;
                entry.steveWallJumpUntilMs = System.currentTimeMillis() + 420L;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction((byte) 31);
                if (customStamina && !creative && localStamina <= 0.01F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    exitLocalWallClimb(player, true);
                }
                return;
            }
        }

        // Compute tangent-plane climbing velocity (W/S = up/down, A/D = strafe along wall)
        boolean movingOnWall = Math.abs(fwdIn) > 0.05F || Math.abs(leftIn) > 0.05F;
        localMovementState = movingOnWall
                ? com.velorise.staves.fsm.MovementActionState.WALL_CLIMB
                : com.velorise.staves.fsm.MovementActionState.WALL_CLING;

        net.minecraft.core.Direction inward = wallFace.getOpposite();
        double rightX = -wallFace.getStepZ();
        double rightZ = wallFace.getStepX();

        double vy = fwdIn * 0.155D;
        if (vy > 0.0D && com.velorise.staves.element.WallClimbHelper.hasOverhangAbove(player)) {
            vy = 0.0D;
        }
        double strafe = -leftIn * 0.125D;
        double vx = inward.getStepX() * 0.04D + rightX * strafe;
        double vz = inward.getStepZ() * 0.04D + rightZ * strafe;

        player.setNoGravity(true);
        player.setDeltaMovement(vx, vy, vz);
        player.fallDistance = 0.0F;

        // If descending onto solid ground, exit cleanly
        if (fwdIn < -0.10F && player.onGround()) {
            exitLocalWallClimb(player, false);
            return;
        }

        // Authentic Genshin stamina rule: clinging idle costs 0 but pauses regen; moving drains stamina
        if (customStamina && !creative) {
            localStaminaRegenDelayTicks = 25;
            if (movingOnWall) {
                float drain = 0.45F; // ~9 Stamina/s
                localStamina = Math.max(0.0F, localStamina - drain);
                if (localStamina <= 0.01F) {
                    localStamina = 0.0F;
                    localStaminaExhausted = true;
                    exitLocalWallClimb(player, true);
                }
            }
        }
    }
}
