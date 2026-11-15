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
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MavuikaActionPayload(byte action) implements CustomPacketPayload {
    public static final byte ACTION_ULTI = 1;
    public static final byte ACTION_DASH_CANCEL = 2;
    public static final byte ACTION_TOGGLE_BIKE = 3;
    public static final byte ACTION_SPIN_START = 4;
    public static final byte ACTION_SPIN_STOP = 5;
    public static final byte ACTION_BIKE_NA = 6;
    public static final byte ACTION_SPRINT_START = 7;
    public static final byte ACTION_SPRINT_STOP = 8;
    public static final byte ACTION_VENTI_ULTI_CHARGE = 9;
    public static final byte ACTION_VENTI_ULTI_CAST = 10;
    public static final byte ACTION_PLUNGE_ATTACK = 11;
    public static final byte ACTION_TOGGLE_GLIDER = 12;
    public static final byte ACTION_NEUVILLETTE_CA_START = 13;
    public static final byte ACTION_NEUVILLETTE_CA_STOP = 14;
    public static final byte ACTION_XILONEN_WALL_LEAP = 15;

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

    public static void handle(MavuikaActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.action() == ACTION_ULTI) {
                ArchonUltiManager.triggerUlti(player);
            } else if (payload.action() == ACTION_DASH_CANCEL) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.handleDashCancel(player);
                }
            } else if (payload.action() == ACTION_TOGGLE_BIKE) {
                ElementalGaugeManager.toggleMavuikaBike(player);
            } else if (payload.action() == ACTION_SPIN_START || payload.action() == ACTION_SPIN_STOP) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.setSpinInputHeld(payload.action() == ACTION_SPIN_START);
                }
            } else if (payload.action() == ACTION_BIKE_NA) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.performNormalAttack(player);
                }
            } else if (payload.action() == ACTION_SPRINT_START || payload.action() == ACTION_SPRINT_STOP) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.setSprintInputHeld(payload.action() == ACTION_SPRINT_START);
                }
            } else if (payload.action() == ACTION_VENTI_ULTI_CHARGE) {
                ArchonUltiManager.tickVentiUltiCharge(player);
            } else if (payload.action() == ACTION_VENTI_ULTI_CAST) {
                ArchonUltiManager.triggerVentiUlti(player);
            } else if (payload.action() == ACTION_PLUNGE_ATTACK) {
                com.velorise.staves.element.PlungeAndGliderManager.handleStartPlunge(player);
            } else if (payload.action() == ACTION_TOGGLE_GLIDER) {
                com.velorise.staves.element.PlungeAndGliderManager.handleToggleGlider(player);
            } else if (payload.action() == ACTION_NEUVILLETTE_CA_START || payload.action() == ACTION_NEUVILLETTE_CA_STOP) {
                com.velorise.staves.element.NeuvilletteManager.handleLeftClickHold(player, payload.action() == ACTION_NEUVILLETTE_CA_START);
            } else if (payload.action() == ACTION_XILONEN_WALL_LEAP) {
                com.velorise.staves.element.XilonenManager.handleWallLeap(player);
            }
        });
    }
}
