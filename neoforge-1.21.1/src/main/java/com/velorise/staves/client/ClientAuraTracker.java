package com.velorise.staves.client;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientAuraTracker {
    public static class AuraDisplayData {
        public final List<Element> elements = new ArrayList<>();
        public @Nullable Element dualTriggerElement = null;
        public long dualUntil = 0;
        public @Nullable ElementalReaction reaction = null;
        public long reactionUntil = 0;
        public long appliedTime = 0;
    }

    private static final Map<Integer, AuraDisplayData> DATA = new ConcurrentHashMap<>();

    private ClientAuraTracker() {}

    public static void update(int entityId, List<Element> elements, @Nullable Element trigger, @Nullable ElementalReaction reaction) {
        long now = System.currentTimeMillis();
        AuraDisplayData entry = DATA.computeIfAbsent(entityId, k -> new AuraDisplayData());
        entry.elements.clear();
        entry.elements.addAll(elements);
        entry.appliedTime = now;

        if (trigger != null) {
            entry.dualTriggerElement = trigger;
            entry.dualUntil = now + 900; // 900ms side-by-side display
        }

        if (reaction != null && reaction != ElementalReaction.NONE) {
            entry.reaction = reaction;
            entry.reactionUntil = now + 1200; // 1200ms floating reaction text
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
