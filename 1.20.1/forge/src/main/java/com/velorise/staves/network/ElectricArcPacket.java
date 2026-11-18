package com.velorise.staves.network;

import com.velorise.staves.client.ClientAuraTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ElectricArcPacket {
    public final int fromEntityId;
    public final double startX;
    public final double startY;
    public final double startZ;
    public final int toEntityId;
    public final double endX;
    public final double endY;
    public final double endZ;
    public final int durationTicks;
    public final int color;

    public ElectricArcPacket(
            int fromEntityId,
            double startX, double startY, double startZ,
            int toEntityId,
            double endX, double endY, double endZ,
            int durationTicks,
            int color
    ) {
        this.fromEntityId = fromEntityId;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.toEntityId = toEntityId;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
        this.durationTicks = durationTicks;
        this.color = color;
    }

    public ElectricArcPacket(FriendlyByteBuf buf) {
        this.fromEntityId = buf.readVarInt();
        this.startX = buf.readDouble();
        this.startY = buf.readDouble();
        this.startZ = buf.readDouble();
        this.toEntityId = buf.readVarInt();
        this.endX = buf.readDouble();
        this.endY = buf.readDouble();
        this.endZ = buf.readDouble();
        this.durationTicks = buf.readVarInt();
        this.color = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.fromEntityId);
        buf.writeDouble(this.startX);
        buf.writeDouble(this.startY);
        buf.writeDouble(this.startZ);
        buf.writeVarInt(this.toEntityId);
        buf.writeDouble(this.endX);
        buf.writeDouble(this.endY);
        buf.writeDouble(this.endZ);
        buf.writeVarInt(this.durationTicks);
        buf.writeInt(this.color);
    }

    public static void handle(ElectricArcPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientAuraTracker.addArc(
                    packet.fromEntityId,
                    new Vec3(packet.startX, packet.startY, packet.startZ),
                    packet.toEntityId,
                    new Vec3(packet.endX, packet.endY, packet.endZ),
                    packet.durationTicks,
                    packet.color
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
