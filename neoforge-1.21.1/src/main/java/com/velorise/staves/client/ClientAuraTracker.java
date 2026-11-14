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
    public static final int ANIM_RAIDEN_DRAW = 8;
    public static final int ANIM_RAIDEN_SLASH = 9;
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
        arc.expireTime = now + (durationTicks > 0 ? (long) durationTicks * 50L : 400L);
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
        if (animType >= 10 && animType <= 14) {
            AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
            if (entry.nightsFocusUntil <= now) {
                entry.nightsFocusAppliedTime = now;
                entry.nightsFocusUntil = now + 12000L;
            }
            entry.nightsFocusStars = animType - 10;
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
        long durationMs = switch (animType) {
            case ANIM_SPLENDOR_SUMMON -> 850L;
            case ANIM_SPLENDOR_SWITCH -> 650L;
            case ANIM_VORTEX_HOLD_BURST -> 750L;
            case ANIM_VORTEX_TAP -> 500L;
            case ANIM_GHASTFIRE_SLAM -> 450L;
            case ANIM_RAIDEN_DRAW -> 500L;
            case ANIM_RAIDEN_SLASH -> 280L;
            default -> 600L;
        };
        AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
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
            entry.frozenTotalMs = 0L;
            entry.poseCaptured = false;
        }

        if (entry.elements.isEmpty() && entry.permanentInnateElements.isEmpty() && entry.permanentInnateElement == null && !entry.permanentOmniInnate && !entry.isFrozen && entry.jadeShieldUntil <= now && entry.nightsFocusUntil <= now && entry.staffAnimUntil <= now && entry.reactionUntil <= now && entry.dualUntil <= now && entry.damageUntil <= now) {
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
