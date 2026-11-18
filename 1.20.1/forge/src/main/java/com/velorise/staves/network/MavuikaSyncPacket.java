package com.velorise.staves.network;

import com.velorise.staves.client.ClientAuraTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MavuikaSyncPacket {
    private final int entityId;
    private final int auraRemainingTicks;
    private final float nightsoulPoints;
    private final boolean netherite;
    private final int ultiCooldownTicks;

    public MavuikaSyncPacket(int entityId, int auraRemainingTicks, float nightsoulPoints, boolean netherite, int ultiCooldownTicks) {
        this.entityId = entityId;
        this.auraRemainingTicks = auraRemainingTicks;
        this.nightsoulPoints = nightsoulPoints;
        this.netherite = netherite;
        this.ultiCooldownTicks = ultiCooldownTicks;
    }

    public MavuikaSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.auraRemainingTicks = buf.readVarInt();
        this.nightsoulPoints = buf.readFloat();
        this.netherite = buf.readBoolean();
        this.ultiCooldownTicks = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(auraRemainingTicks);
        buf.writeFloat(nightsoulPoints);
        buf.writeBoolean(netherite);
        buf.writeVarInt(ultiCooldownTicks);
    }

    public static void handle(MavuikaSyncPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientAuraTracker.updateMavuika(
                packet.entityId, packet.auraRemainingTicks, packet.nightsoulPoints, packet.netherite, packet.ultiCooldownTicks
        ));
        ctx.setPacketHandled(true);
    }
}
