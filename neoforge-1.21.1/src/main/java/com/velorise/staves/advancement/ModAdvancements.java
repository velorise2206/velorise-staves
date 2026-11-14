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
    public static final String EVENT_TAME_OMNI_PET = "tame_omni_pet";
    public static final String EVENT_BEHEADED = "beheaded";
    public static final String EVENT_NO_ORDER = "i_dont_have_order";
    public static final String EVENT_WEB_DEVELOPER = "web_developer";
    public static final String EVENT_TAKE_MY_BALL = "take_my_ball";
    public static final String EVENT_GEO_ARCHITECT = "geo_architect";
    public static final String EVENT_HIDDEN_PYRO = "hidden_pyro";
    public static final String EVENT_HIDDEN_HYDRO = "hidden_hydro";
    public static final String EVENT_HIDDEN_CRYO = "hidden_cryo";
    public static final String EVENT_HIDDEN_ELECTRO = "hidden_electro";
    public static final String EVENT_HIDDEN_ANEMO = "hidden_anemo";
    public static final String EVENT_HIDDEN_GEO = "hidden_geo";
    public static final String EVENT_HIDDEN_DENDRO = "hidden_dendro";
    public static final String EVENT_HIDDEN_OMNI = "hidden_omni";

    private ModAdvancements() {}

    public static void trigger(@Nullable ServerPlayer player, String event) {
        if (player == null || player.level().isClientSide()) return;
        try {
            VeloriseStavesMod.ADVANCEMENT_TRIGGER.get().trigger(player, event);
        } catch (Exception ignored) {}
    }
}
