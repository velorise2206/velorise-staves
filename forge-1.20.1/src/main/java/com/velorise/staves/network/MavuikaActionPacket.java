package com.velorise.staves.network;

import com.velorise.staves.element.ElementalGaugeManager;
import com.velorise.staves.entity.FlamestriderBikeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MavuikaActionPacket {
    public static final byte ACTION_ULTI = 1;
    public static final byte ACTION_DASH_CANCEL = 2;
    public static final byte ACTION_TOGGLE_BIKE = 3;
    public static final byte ACTION_SPIN_START = 4;
    public static final byte ACTION_SPIN_STOP = 5;
    public static final byte ACTION_BIKE_NA = 6;
    public static final byte ACTION_SPRINT_START = 7;
    public static final byte ACTION_SPRINT_STOP = 8;

    private final byte action;

    public MavuikaActionPacket(byte action) {
        this.action = action;
    }

    public MavuikaActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(action);
    }

    public static void handle(MavuikaActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (packet.action == ACTION_ULTI) {
                ElementalGaugeManager.triggerMavuikaUlti(player);
            } else if (packet.action == ACTION_DASH_CANCEL) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.handleDashCancel(player);
                }
            } else if (packet.action == ACTION_TOGGLE_BIKE) {
                ElementalGaugeManager.toggleMavuikaBike(player);
            } else if (packet.action == ACTION_SPIN_START || packet.action == ACTION_SPIN_STOP) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.setSpinInputHeld(packet.action == ACTION_SPIN_START);
                }
            } else if (packet.action == ACTION_BIKE_NA) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.performNormalAttack(player);
                }
            } else if (packet.action == ACTION_SPRINT_START || packet.action == ACTION_SPRINT_STOP) {
                if (player.getVehicle() instanceof FlamestriderBikeEntity bike) {
                    bike.setSprintInputHeld(packet.action == ACTION_SPRINT_START);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
