package com.velorise.staves.network;

import com.velorise.staves.client.ClientAuraTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RaidenSyncPacket {
    private final int entityId;
    private final int eyeRemainingTicks;
    private final int musouRemainingTicks;
    private final int resolve;
    private final boolean netherite;
    private final int burstCooldownTicks;

    public RaidenSyncPacket(int entityId, int eyeRemainingTicks, int musouRemainingTicks, int resolve, boolean netherite, int burstCooldownTicks) {
        this.entityId = entityId;
        this.eyeRemainingTicks = eyeRemainingTicks;
        this.musouRemainingTicks = musouRemainingTicks;
        this.resolve = resolve;
        this.netherite = netherite;
        this.burstCooldownTicks = burstCooldownTicks;
    }

    public RaidenSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.eyeRemainingTicks = buf.readVarInt();
        this.musouRemainingTicks = buf.readVarInt();
        this.resolve = buf.readVarInt();
        this.netherite = buf.readBoolean();
        this.burstCooldownTicks = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(eyeRemainingTicks);
        buf.writeVarInt(musouRemainingTicks);
        buf.writeVarInt(resolve);
        buf.writeBoolean(netherite);
        buf.writeVarInt(burstCooldownTicks);
    }

    public static void handle(RaidenSyncPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientAuraTracker.updateRaiden(
                        packet.entityId, packet.eyeRemainingTicks, packet.musouRemainingTicks, packet.resolve, packet.netherite, packet.burstCooldownTicks
                )
        ));
        ctx.setPacketHandled(true);
    }
}
