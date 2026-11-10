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

    public static final List<ActiveArc> ACTIVE_ARCS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addArc(int fromId, net.minecraft.world.phys.Vec3 start, int toId, net.minecraft.world.phys.Vec3 end, int durationTicks, int color) {
        long now = System.currentTimeMillis();
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
        long now = System.currentTimeMillis();
        AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
        entry.elements.clear();
        entry.elements.addAll(elements);
        entry.appliedTime = now;
        entry.durationTicks = durationTicks;
        if (durationTicks > 0) {
            entry.expireTime = now + (long) durationTicks * 50L;
        } else if (durationTicks < 0) {
            entry.expireTime = -1L; // Permanent innate aura
            if (entry.permanentInnateElement == null && !elements.isEmpty()) {
                entry.permanentInnateElement = elements.get(0);
            }
        } else {
            entry.expireTime = entry.permanentInnateElement != null ? -1L : 0L;
        }
        if (entry.permanentInnateElement != null && !entry.elements.contains(entry.permanentInnateElement)) {
            entry.elements.add(0, entry.permanentInnateElement);
        }

        if (reaction != null && reaction != ElementalReaction.NONE) {
            entry.reaction = reaction;
            entry.reactionStartTime = now;
            entry.reactionUntil = now + 1600; // 32 ticks (1.6 seconds) floating reaction text

            if (reactedAura != null || trigger != null) {
                entry.reactedAuraElement = reactedAura;
                entry.dualTriggerElement = trigger;
                entry.dualStartTime = now;
                entry.dualUntil = now + 1300; // 26 ticks (1.3 seconds) Genshin dual-icon pop & fade
            }

            if (reaction == ElementalReaction.CRYSTALLIZE) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null && mc.player.getId() == entityId) {
                    if (reactedAura != null || trigger != null) {
                        activeCrystallizeShield = reactedAura != null ? reactedAura : trigger;
                        crystallizeShieldUntil = now + (durationTicks > 0 ? (long) durationTicks * 50L : 15000L);
                    } else {
                        activeCrystallizeShield = null;
                        crystallizeShieldUntil = 0L;
                    }
                }
            }
        } else {
            entry.reaction = null;
            entry.reactedAuraElement = null;
            entry.dualTriggerElement = null;
            entry.dualUntil = 0L;
        }

        if (damage > 0.0F) {
            entry.lastDamage = damage;
            entry.damageElement = trigger != null ? trigger : (reactedAura != null ? reactedAura : null);
            entry.damageStartTime = now;
            entry.damageUntil = now + 1500; // 30 ticks (1.5 seconds) floating damage number
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

        if (entry.elements.isEmpty() && entry.permanentInnateElement == null && !entry.isFrozen && entry.reactionUntil <= now && entry.dualUntil <= now && entry.damageUntil <= now) {
            DATA.remove(entityId);
        }
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
        activeCrystallizeShield = null;
        crystallizeShieldUntil = 0L;
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
