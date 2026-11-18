package com.velorise.staves.network;

import com.velorise.staves.element.ArchonUltiManager;
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
    public static final byte ACTION_VENTI_ULTI_CHARGE = 9;
    public static final byte ACTION_VENTI_ULTI_CAST = 10;
    public static final byte ACTION_PLUNGE_ATTACK = 11;
    public static final byte ACTION_TOGGLE_GLIDER = 12;
    public static final byte ACTION_NEUVILLETTE_CA_START = 13;
    public static final byte ACTION_NEUVILLETTE_CA_STOP = 14;
    public static final byte ACTION_XILONEN_WALL_LEAP = 15;
    public static final byte ACTION_XILONEN_DASH = 16;
    public static final byte ACTION_XILONEN_JUMP = 17;
    public static final byte ACTION_FURINA_CA_SWITCH = 18;
    public static final byte ACTION_NAHIDA_NA = 19;
    public static final byte ACTION_NAHIDA_CA = 20;
    public static final byte ACTION_TSARITSA_NA = 21;
    public static final byte ACTION_TSARITSA_CA = 22;
    public static final byte ACTION_TSARITSA_CA_FULL = 23;
    public static final byte ACTION_DAYBREAK_NA = 24;
    public static final byte ACTION_DAYBREAK_CA = 25;
    public static final byte ACTION_DAYBREAK_CA_FULL = 26;
    public static final byte ACTION_DRAGONFIRE_NA = 27;

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
            ArchonUltiManager.resetAllArchonIdleTimers(player);
            if (packet.action != ACTION_TOGGLE_GLIDER
                    && packet.action != ACTION_PLUNGE_ATTACK
                    && packet.action != ACTION_SPIN_STOP
                    && packet.action != ACTION_SPRINT_STOP
                    && packet.action != ACTION_NEUVILLETTE_CA_STOP
                    && com.velorise.staves.element.ElementalResonanceManager.checkAndNotifyOverloaded(player)) {
                return;
            }
            if (packet.action == ACTION_ULTI) {
                ArchonUltiManager.triggerUlti(player);
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
            } else if (packet.action == ACTION_VENTI_ULTI_CHARGE) {
                ArchonUltiManager.tickVentiUltiCharge(player);
            } else if (packet.action == ACTION_VENTI_ULTI_CAST) {
                ArchonUltiManager.triggerVentiUlti(player);
            } else if (packet.action == ACTION_PLUNGE_ATTACK) {
                com.velorise.staves.element.PlungeAndGliderManager.handleStartPlunge(player);
            } else if (packet.action == ACTION_TOGGLE_GLIDER) {
                com.velorise.staves.element.PlungeAndGliderManager.handleToggleGlider(player);
            } else if (packet.action == ACTION_NEUVILLETTE_CA_START || packet.action == ACTION_NEUVILLETTE_CA_STOP) {
                com.velorise.staves.element.NeuvilletteManager.handleLeftClickHold(player, packet.action == ACTION_NEUVILLETTE_CA_START);
            } else if (packet.action == ACTION_XILONEN_WALL_LEAP) {
                com.velorise.staves.element.XilonenManager.handleWallLeap(player);
            } else if (packet.action == ACTION_XILONEN_DASH) {
                com.velorise.staves.element.XilonenManager.handleSkateDash(player);
            } else if (packet.action == ACTION_XILONEN_JUMP) {
                com.velorise.staves.element.XilonenManager.handleSkateJumpBoost(player);
            } else if (packet.action == ACTION_FURINA_CA_SWITCH) {
                com.velorise.staves.item.ArcaneStaffItem.handleFurinaLeftClickHoldSwitch(player);
            } else if (packet.action == ACTION_NAHIDA_NA) {
                if (!com.velorise.staves.element.TsaritsaManager.tryTriggerStellarPunishment(player)) {
                    com.velorise.staves.element.NahidaManager.handleNormalAttack(player);
                }
            } else if (packet.action == ACTION_NAHIDA_CA) {
                if (!com.velorise.staves.element.TsaritsaManager.tryTriggerStellarPunishment(player)) {
                    com.velorise.staves.element.NahidaManager.handleChargedAttack(player);
                }
            } else if (packet.action == ACTION_TSARITSA_NA) {
                if (!com.velorise.staves.element.TsaritsaManager.tryTriggerStellarPunishment(player)) {
                    com.velorise.staves.element.TsaritsaManager.performNormalAttack(player);
                }
            } else if (packet.action == ACTION_TSARITSA_CA) {
                com.velorise.staves.element.TsaritsaManager.performChargedBowShot(player, false);
            } else if (packet.action == ACTION_TSARITSA_CA_FULL) {
                com.velorise.staves.element.TsaritsaManager.performChargedBowShot(player, true);
            } else if (packet.action == ACTION_DAYBREAK_NA) {
                com.velorise.staves.item.ArcaneStaffItem.performDaybreakLeftClickShot(player, 0.25F);
            } else if (packet.action == ACTION_DAYBREAK_CA) {
                com.velorise.staves.item.ArcaneStaffItem.performDaybreakLeftClickShot(player, 0.65F);
            } else if (packet.action == ACTION_DAYBREAK_CA_FULL) {
                com.velorise.staves.item.ArcaneStaffItem.performDaybreakLeftClickShot(player, 1.0F);
            } else if (packet.action == ACTION_DRAGONFIRE_NA) {
                com.velorise.staves.element.DragonfireCloudManager.handleLeftClickOrb(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
