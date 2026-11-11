package com.velorise.staves.advancement;

import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ModAdvancements {
    public static final String EVENT_FIRST_REACTION = "first_reaction";
    public static final String EVENT_DENDRO_CORE_REACTION = "dendro_core_reaction";
    public static final String EVENT_CRYSTALLIZE_SHIELD = "crystallize_shield";
    public static final String EVENT_GEO_FANG_PILLAR = "geo_fang_pillar";
    public static final String EVENT_TESLA_COIL = "tesla_coil";
    public static final String EVENT_ELEMENTAL_MASTERY = "elemental_mastery";
    public static final String EVENT_SPATIAL_TRAVELER = "spatial_traveler";

    private ModAdvancements() {}

    public static void trigger(@Nullable ServerPlayer player, String event) {
        if (player == null || player.level().isClientSide()) return;
        try {
            VeloriseStavesMod.ADVANCEMENT_TRIGGER.get().trigger(player, event);
        } catch (Exception ignored) {}
    }
}
