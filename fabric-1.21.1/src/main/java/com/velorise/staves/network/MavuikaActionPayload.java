package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.ArchonUltiManager;
import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record MavuikaActionPayload(byte action) implements CustomPacketPayload {
    public static final byte ACTION_ULTI = 0;
    public static final byte ACTION_DASH_CANCEL = 1;
    public static final byte ACTION_TOGGLE_BIKE = 2;
    public static final byte ACTION_SPIN_START = 3;
    public static final byte ACTION_SPIN_STOP = 4;
    public static final byte ACTION_BIKE_NA = 5;
    public static final byte ACTION_SPRINT_START = 6;
    public static final byte ACTION_SPRINT_STOP = 7;
    public static final byte ACTION_VENTI_ULTI_CHARGE = 8;
    public static final byte ACTION_VENTI_ULTI_CAST = 9;
    public static final byte ACTION_PLUNGE_ATTACK = 10;
    public static final byte ACTION_TOGGLE_GLIDER = 11;
    public static final byte ACTION_NEUVILLETTE_CA_START = 12;
    public static final byte ACTION_NEUVILLETTE_CA_STOP = 13;
    public static final byte ACTION_XILONEN_WALL_LEAP = 14;
    public static final byte ACTION_XILONEN_DASH = 15;
    public static final byte ACTION_XILONEN_JUMP = 16;
    public static final byte ACTION_FURINA_CA_SWITCH = 17;
    public static final byte ACTION_NAHIDA_NA = 18;
    public static final byte ACTION_NAHIDA_CA = 19;
    public static final byte ACTION_TSARITSA_NA = 20;
    public static final byte ACTION_TSARITSA_CA = 21;
    public static final byte ACTION_TSARITSA_CA_FULL = 22;

    public static final Type<MavuikaActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "mavuika_action"));
    public static final StreamCodec<ByteBuf, MavuikaActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            MavuikaActionPayload::action,
            MavuikaActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerPlayer player, byte action) {
        if (player == null) return;
        if (action == ACTION_ULTI) {
            ArchonUltiManager.triggerUlti(player);
        } else if (action == ACTION_DASH_CANCEL) {
            if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                bike.handleDashCancel(player);
            }
        } else if (action == ACTION_TOGGLE_BIKE) {
            ElementalGaugeManager.toggleMavuikaBike(player);
        } else if (action == ACTION_SPIN_START || action == ACTION_SPIN_STOP) {
            if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                bike.setSpinInputHeld(action == ACTION_SPIN_START);
            }
        } else if (action == ACTION_BIKE_NA) {
            if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                bike.performNormalAttack(player);
            }
        } else if (action == ACTION_SPRINT_START || action == ACTION_SPRINT_STOP) {
            if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                bike.setSprintInputHeld(action == ACTION_SPRINT_START);
            }
        } else if (action == ACTION_VENTI_ULTI_CHARGE) {
            ArchonUltiManager.tickVentiUltiCharge(player);
        } else if (action == ACTION_VENTI_ULTI_CAST) {
            ArchonUltiManager.triggerVentiUlti(player);
        } else if (action == ACTION_PLUNGE_ATTACK) {
            com.velorise.staves.element.PlungeAndGliderManager.handleStartPlunge(player);
        } else if (action == ACTION_TOGGLE_GLIDER) {
            com.velorise.staves.element.PlungeAndGliderManager.handleToggleGlider(player);
        } else if (action == ACTION_NEUVILLETTE_CA_START || action == ACTION_NEUVILLETTE_CA_STOP) {
            com.velorise.staves.element.NeuvilletteManager.handleLeftClickHold(player, action == ACTION_NEUVILLETTE_CA_START);
        } else if (action == ACTION_XILONEN_WALL_LEAP) {
            com.velorise.staves.element.XilonenManager.handleWallLeap(player);
        } else if (action == ACTION_XILONEN_DASH) {
            com.velorise.staves.element.XilonenManager.handleSkateDash(player);
        } else if (action == ACTION_XILONEN_JUMP) {
            com.velorise.staves.element.XilonenManager.handleSkateJumpBoost(player);
        } else if (action == ACTION_FURINA_CA_SWITCH) {
            com.velorise.staves.item.ArcaneStaffItem.handleFurinaLeftClickHoldSwitch(player);
        } else if (action == ACTION_NAHIDA_NA) {
            if (!com.velorise.staves.element.TsaritsaManager.tryTriggerStellarPunishment(player)) {
                com.velorise.staves.element.NahidaManager.handleNormalAttack(player);
            }
        } else if (action == ACTION_NAHIDA_CA) {
            if (!com.velorise.staves.element.TsaritsaManager.tryTriggerStellarPunishment(player)) {
                com.velorise.staves.element.NahidaManager.handleChargedAttack(player);
            }
        } else if (action == ACTION_TSARITSA_NA) {
            if (!com.velorise.staves.element.TsaritsaManager.tryTriggerStellarPunishment(player)) {
                com.velorise.staves.element.TsaritsaManager.performNormalAttack(player);
            }
        } else if (action == ACTION_TSARITSA_CA) {
            com.velorise.staves.element.TsaritsaManager.performChargedBowShot(player, false);
        } else if (action == ACTION_TSARITSA_CA_FULL) {
            com.velorise.staves.element.TsaritsaManager.performChargedBowShot(player, true);
        }
    }
}
