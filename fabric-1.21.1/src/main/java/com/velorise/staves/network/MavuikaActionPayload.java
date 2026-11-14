package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
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
            ElementalGaugeManager.triggerMavuikaUlti(player);
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
        }
    }
}
