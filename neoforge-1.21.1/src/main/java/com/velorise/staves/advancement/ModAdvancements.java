package com.velorise.staves.advancement;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
    public static final String EVENT_STARRY_NIGHT = "starry_night";
    public static final String EVENT_BROKEN_CONTRACT = "broken_contract";
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

    public static void handleLivingDeath(@Nullable LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        if (entity instanceof ServerPlayer player) {
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArcaneStaffItem staff) {
                ArcaneStaffItem.Kind k = staff.kind();
                if (k == ArcaneStaffItem.Kind.VORTEX || k == ArcaneStaffItem.Kind.NIGHTS_FOCUS) {
                    trigger(player, EVENT_BROKEN_CONTRACT);
                }
            }
        } else if (entity instanceof OwnableEntity ownable) {
            UUID ownerUuid = ownable.getOwnerUUID();
            if (ownerUuid != null && entity.level() instanceof ServerLevel sl) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUuid);
                if (owner != null) {
                    trigger(owner, EVENT_HIDDEN_ANEMO);
                }
            }
        }
    }
}
