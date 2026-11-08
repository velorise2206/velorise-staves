package com.velorise.staves.client;

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
        public @Nullable Element reactedAuraElement = null;
        public @Nullable Element dualTriggerElement = null;
        public long dualStartTime = 0;
        public long dualUntil = 0;
        public @Nullable ElementalReaction reaction = null;
        public long reactionStartTime = 0;
        public long reactionUntil = 0;
        public long appliedTime = 0;
    }

    private static final Map<Integer, AuraDisplayData> DATA = new ConcurrentHashMap<>();

    private ClientAuraTracker() {}

    public static void update(
            int entityId,
            List<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        long now = System.currentTimeMillis();
        AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
        entry.elements.clear();
        entry.elements.addAll(elements);
        entry.appliedTime = now;

        if (reaction != null && reaction != ElementalReaction.NONE) {
            entry.reaction = reaction;
            entry.reactionStartTime = now;
            entry.reactionUntil = now + 1200; // 24 ticks (1.2 seconds) floating reaction text
        }

        if (reactedAura != null || trigger != null) {
            entry.reactedAuraElement = reactedAura;
            entry.dualTriggerElement = trigger;
            entry.dualStartTime = now;
            entry.dualUntil = now + 1100; // 22 ticks (1.1 seconds) Genshin dual-icon pop & fade
        }

        if (elements.isEmpty() && entry.reactionUntil <= now && entry.dualUntil <= now) {
            DATA.remove(entityId);
        }
    }

    public static @Nullable AuraDisplayData get(int entityId) {
        return DATA.get(entityId);
    }

    public static void remove(int entityId) {
        DATA.remove(entityId);
    }

    public static void clear() {
        DATA.clear();
    }
}
