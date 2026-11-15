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
    public static final int ANIM_FURINA_BURST = 34;
    public static final int ANIM_LAYLA_BURST = 35;
    public static final int ANIM_XILONEN_JUMP_FLIP = 36;

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

    public static volatile boolean localWindGliderUnlocked = false;
    public static volatile int ventiUltiHoldTicks = 0;
    public static volatile int localTsaritsaHoldETicks = 0;
    public static volatile boolean localTsaritsaHoldELocked = false;
    public static volatile int localTsaritsaBowPullTicks = 0;
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
        public long neuvilletteActiveUntil = 0L;
        public boolean neuvilletteFiringBeam = false;
        public int neuvilletteChargeProgress = 0;
        public int neuvilletteBeamElapsed = 0;

        // Nahida states
        public boolean nahidaIdleSwing = false;
        public boolean nahidaViewfinder = false;
        public long skandhaMarkedUntil = 0L;
        public int nahidaShrineMask = 0;
        public long nahidaShrineUntil = 0L;

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
        arc.fromEntityId = fromId;
        arc.start = start;
        arc.toEntityId = toId;
        arc.end = end;
        arc.startTime = now;
        arc.expireTime = nextExpire;
        arc.color = color;
        ACTIVE_ARCS.add(arc);
    }

    public static void pruneStale(net.minecraft.client.multiplayer.ClientLevel level) {
        if (level == null) return;
        long now = System.currentTimeMillis();
        DATA.entrySet().removeIf(e -> level.getEntity(e.getKey()) == null);
        ACTIVE_ARCS.removeIf(a -> now >= a.expireTime);
        FLOATING_TEXTS.removeIf(t -> now >= t.until);
    }


    public static void triggerStaffAnimation(int entityId, int animType) {
        if (entityId <= 0 || animType <= 0) return;
        long now = System.currentTimeMillis();
        if (animType == ANIM_GLIDER_UNLOCKED) {
            localWindGliderUnlocked = true;
            return;
        }
        if (animType == ANIM_GLIDER_LOCKED) {
            localWindGliderUnlocked = false;
            return;
        }
        if (animType == ANIM_XILONEN_JUMP_FLIP) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.xilonenJumpFlipStartTime = now;
            entry.xilonenJumpFlipUntil = now + 550L;
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
            entry.nahidaViewfinder = (animType == 4002);
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
            entry.tsaritsaSyncUntil = mask > 0 ? (now + 22000L) : 0L;
            return;
        }
        if (animType == ANIM_TSARITSA_CROWN_FLASH) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            entry.tsaritsaCrownFlashUntil = now + 500L;
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
            case ANIM_RAIDEN_CA -> 480L;
            case ANIM_ZHONGLI_PLANET_BEFALL -> 2800L;
            case ANIM_XILONEN_SKATE_DASH -> 480L;
            case ANIM_XILONEN_KICK_1 -> 380L;
            case ANIM_XILONEN_KICK_2 -> 460L;
            case ANIM_XILONEN_BURST -> 1600L;
            case ANIM_NEUVILLETTE_BURST -> 1700L;
            case ANIM_NEUVILLETTE_SKILL_E -> 550L;
            case ANIM_MAVUIKA_BURST -> 1700L;
            case ANIM_VENTI_BURST -> 1600L;
            case ANIM_FURINA_BURST -> 1600L;
            case ANIM_LAYLA_BURST -> 1600L;
            case ANIM_NAHIDA_NA_1, ANIM_NAHIDA_NA_2, ANIM_NAHIDA_NA_3 -> 380L;
            case ANIM_NAHIDA_NA_4 -> 520L;
            case ANIM_NAHIDA_CA -> 620L;
            case ANIM_NAHIDA_TAP_E -> 480L;
            case ANIM_NAHIDA_HOLD_E -> 520L;
            case ANIM_NAHIDA_BURST -> 1700L;
            case ANIM_TSARITSA_BOW_NA -> 350L;
            case ANIM_TSARITSA_CA_SHOT -> 520L;
            case ANIM_TSARITSA_TAP_E -> 500L;
            case ANIM_TSARITSA_HOLD_E -> 680L;
            case ANIM_TSARITSA_SPEAR_NA_1, ANIM_TSARITSA_SPEAR_NA_2 -> 380L;
            case ANIM_TSARITSA_SPEAR_NA_3 -> 480L;
            case ANIM_TSARITSA_BURST_SUPPORT -> 1600L;
            case ANIM_TSARITSA_BURST_DPS -> 1600L;
            case ANIM_TSARITSA_PUNISHMENT -> 520L;
            case ANIM_TSARITSA_UNSEAL -> 650L;
            default -> 600L;
        };
        entry.staffAnimType = animType;
        entry.staffAnimStartTime = now;
        entry.staffAnimUntil = now + durationMs;
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
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (reaction == ElementalReaction.CRYSTALLIZE && elements.isEmpty() && trigger == null) {
            // Dedicated Crystallize / Jade Shield sync packet from server: do NOT overwrite active Aura or show dual icons!
            AuraDisplayData shieldEntry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (reactedAura == Element.GEO && durationTicks > 0) {
                shieldEntry.jadeShieldUntil = now + (long) durationTicks * 50L;
                jadeShieldAppliedTime = now;
            } else {
                shieldEntry.jadeShieldUntil = 0L;
            }
            if (mc.player != null && mc.player.getId() == entityId) {
                if (reactedAura != null) {
                    activeCrystallizeShield = reactedAura;
                    crystallizeShieldAppliedTime = now;
                    crystallizeShieldUntil = now + (durationTicks > 0 ? (long) durationTicks * 50L : 15000L);
                    if (reactedAura != Element.GEO) {
                        ModMobEffects.removeAllShieldsExcept(mc.player, reactedAura);
                    }
                } else {
                    activeCrystallizeShield = null;
                    crystallizeShieldUntil = 0L;
                    ModMobEffects.removeAllShields(mc.player);
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
            if (mc.player != null && mc.player.getId() == entityId) {
                mc.player.setHealth(damage);
                mc.player.invulnerableTime = 0;
                mc.player.hurtTime = 0;
                mc.player.hurtDuration = 0;
            }
            if (mc.level != null) {
                net.minecraft.world.entity.Entity ent = mc.level.getEntity(entityId);
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
                if (mc.player != null && entityId == mc.player.getId() && mc.options.getCameraType().isFirstPerson()) {
                    return;
                }
                double bx = posX;
                double by = posY;
                double bz = posZ;
                if (!hasPos || Double.isNaN(bx) || Double.isNaN(by) || Double.isNaN(bz)) {
                    net.minecraft.world.entity.Entity e = mc.level != null ? mc.level.getEntity(entityId) : null;
                    if (e != null) {
                        bx = e.getX();
                        by = e.getY() + Math.max(0.65D, e.getBbHeight() * 0.65D);
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
            net.minecraft.world.entity.Entity e = mc.level != null ? mc.level.getEntity(entityId) : null;
            if (e != null) {
                resolvedX = e.getX();
                resolvedY = e.getY() + Math.max(0.65D, e.getBbHeight() * 0.65D);
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
            entry.isFrozen = false;
            entry.frozenUntil = 0L;
            entry.petrifiedUntil = 0L;
            entry.frozenTotalMs = 0L;
            entry.poseCaptured = false;
        }

        if (entry.elements.isEmpty() && entry.permanentInnateElements.isEmpty() && entry.permanentInnateElement == null && !entry.permanentOmniInnate && !entry.isFrozen && entry.jadeShieldUntil <= now && entry.nightsFocusUntil <= now && entry.staffAnimUntil <= now && !entry.gliding && !entry.plunging && !entry.xilonenSkating && entry.xilonenSamplesUntil <= now && entry.neuvilletteActiveUntil <= now && !entry.nahidaIdleSwing && !entry.nahidaViewfinder && entry.skandhaMarkedUntil <= now && entry.nahidaShrineUntil <= now && entry.tsaritsaSyncUntil <= now && entry.reactionUntil <= now && entry.dualUntil <= now && entry.damageUntil <= now) {
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
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId() && activeCrystallizeShield == Element.GEO && now < crystallizeShieldUntil) {
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
        public long syncedAtMs;

        public float effectiveEyeTicks() {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - syncedAtMs);
            return Math.max(0.0F, eyeRemainingTicks - (elapsedMs / 50.0F));
        }

        public float effectiveMusouTicks() {
            long elapsedMs = Math.max(0L, System.currentTimeMillis() - syncedAtMs);
            return Math.max(0.0F, musouRemainingTicks - (elapsedMs / 50.0F));
        }
    }

    private static final java.util.Map<Integer, RaidenDisplayState> RAIDEN_STATES = new java.util.concurrent.ConcurrentHashMap<>();

    public static void updateRaiden(int entityId, int eyeRemainingTicks, int musouRemainingTicks, int resolve, boolean netherite) {
        if (eyeRemainingTicks <= 0 && musouRemainingTicks <= 0 && resolve <= 0) {
            RAIDEN_STATES.remove(entityId);
            return;
        }
        RaidenDisplayState state = RAIDEN_STATES.computeIfAbsent(entityId, id -> new RaidenDisplayState());
        state.eyeRemainingTicks = Math.max(0, eyeRemainingTicks);
        state.musouRemainingTicks = Math.max(0, musouRemainingTicks);
        state.resolve = Math.max(0, Math.min(60, resolve));
        state.netherite = netherite;
        state.syncedAtMs = System.currentTimeMillis();
    }

    public static @Nullable RaidenDisplayState getRaiden(int entityId) {
        RaidenDisplayState state = RAIDEN_STATES.get(entityId);
        if (state == null) return null;
        if (state.effectiveEyeTicks() <= 0.0F && state.effectiveMusouTicks() <= 0.0F && state.resolve <= 0) {
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


    public static boolean isXilonenSkating(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.xilonenSkating && System.currentTimeMillis() < data.xilonenSkateSyncUntil;
    }

    public static boolean isXilonenWallClimbing(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.xilonenSkating && data.xilonenWallClimbing && System.currentTimeMillis() < data.xilonenSkateSyncUntil;
    }

    public static boolean isXilonenJumpFlipping(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        long now = System.currentTimeMillis();
        return data != null && data.xilonenSkating && !data.xilonenWallClimbing
                && !data.plunging && !data.gliding && now < data.xilonenJumpFlipUntil;
    }

    public static boolean hasXilonenSourceSamples(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.xilonenSampleMask > 0 && System.currentTimeMillis() < data.xilonenSamplesUntil;
    }

    public static boolean isGliding(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.gliding;
    }

    public static boolean isPlunging(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.plunging;
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
        return data != null && data.nahidaIdleSwing;
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
        return data != null && data.tsaritsaCrownActive && System.currentTimeMillis() < data.tsaritsaSyncUntil;
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
        localTsaritsaHoldETicks = 0;
        localTsaritsaHoldELocked = false;
        localTsaritsaBowPullTicks = 0;
    }

    public static @Nullable Element getTrackedCrystallizeShield() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return null;
        if (activeCrystallizeShield != null) {
            long now = System.currentTimeMillis();
            boolean graceActive = (now - crystallizeShieldAppliedTime) < 1500L;
            boolean depleted = !graceActive && !mc.player.isCreative() && mc.player.getAbsorptionAmount() <= 0.0F;
            if (now >= crystallizeShieldUntil || depleted) {
                activeCrystallizeShield = null;
                return null;
            }
            return activeCrystallizeShield;
        }
        return null;
    }

    public static @Nullable Element getActiveCrystallizeShield() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return null;
        Element mobShield = ModMobEffects.getActiveShield(mc.player);
        if (mobShield != null) {
            if (activeCrystallizeShield != mobShield || crystallizeShieldUntil <= System.currentTimeMillis()) {
                activeCrystallizeShield = mobShield;
                crystallizeShieldUntil = System.currentTimeMillis() + 15000L;
            }
            return mobShield;
        }
        return getTrackedCrystallizeShield();
    }
}
