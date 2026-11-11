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
    public static class AuraDisplayData {
        public final List<Element> elements = new ArrayList<>();
        public @Nullable Element permanentInnateElement = null;
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
    }

    private static final Map<Integer, AuraDisplayData> DATA = new ConcurrentHashMap<>();
    public static @Nullable Element activeCrystallizeShield = null;
    public static long crystallizeShieldUntil = 0L;
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

    public static void addFloatingText(
            double x,
            double y,
            double z,
            @Nullable ElementalReaction reaction,
            float damage,
            @Nullable Element damageElement
    ) {
        if ((reaction == null || reaction == ElementalReaction.NONE) && damage <= 0.0F) return;
        long now = System.currentTimeMillis();
        if (FLOATING_TEXTS.size() > 256) {
            FLOATING_TEXTS.removeIf(t -> now >= t.until);
            while (FLOATING_TEXTS.size() > 256) {
                FLOATING_TEXTS.remove(0);
            }
        }
        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
        double ox = (rng.nextDouble() - 0.5D) * 0.56D;
        double oy = (rng.nextDouble() - 0.5D) * 0.28D;
        double oz = (rng.nextDouble() - 0.5D) * 0.56D;
        FLOATING_TEXTS.add(new FloatingCombatText(
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
        if (reaction == ElementalReaction.CRYSTALLIZE && mc.player != null && mc.player.getId() == entityId && elements.isEmpty() && trigger == null) {
            // Dedicated Crystallize Shield sync packet from server: do NOT overwrite player's active Aura or show dual icons!
            if (reactedAura != null) {
                activeCrystallizeShield = reactedAura;
                crystallizeShieldUntil = now + (durationTicks > 0 ? (long) durationTicks * 50L : 15000L);
                ModMobEffects.removeAllShieldsExcept(mc.player, reactedAura);
            } else {
                activeCrystallizeShield = null;
                crystallizeShieldUntil = 0L;
                ModMobEffects.removeAllShields(mc.player);
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
            if (entry.permanentInnateElement == null && !elements.isEmpty()) {
                entry.permanentInnateElement = elements.get(0);
            }
        } else {
            entry.expireTime = (entry.permanentInnateElement != null || entry.permanentOmniInnate) ? -1L : 0L;
        }
        if (entry.permanentInnateElement != null && !entry.elements.contains(entry.permanentInnateElement)) {
            entry.elements.add(0, entry.permanentInnateElement);
        }

        boolean spawnReaction = false;
        if (reaction != null && reaction != ElementalReaction.NONE) {
            boolean keepOngoingBurningBanner = (reaction == ElementalReaction.BURNING
                    && entry.reaction == ElementalReaction.BURNING
                    && now < entry.reactionUntil);
            if (!keepOngoingBurningBanner) {
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

        if (spawnReaction || spawnDamage) {
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
                Element dmgElem = trigger != null ? trigger : reactedAura;
                addFloatingText(bx, by, bz, spawnReaction ? reaction : null, spawnDamage ? damage : 0.0F, dmgElem);
            }
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

        if (entry.elements.isEmpty() && entry.permanentInnateElement == null && !entry.permanentOmniInnate && !entry.isFrozen && entry.reactionUntil <= now && entry.dualUntil <= now && entry.damageUntil <= now) {
            DATA.remove(entityId);
        }
    }

    public static boolean hasPyroInnate(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.permanentInnateElement == Element.PYRO;
    }

    public static boolean hasOmniInnate(int entityId) {
        AuraDisplayData data = DATA.get(entityId);
        return data != null && data.permanentOmniInnate;
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

    public static void remove(int entityId) {
        DATA.remove(entityId);
    }

    public static void clear() {
        DATA.clear();
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
            boolean depleted = !mc.player.isCreative() && mc.player.getAbsorptionAmount() <= 0.0F;
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
