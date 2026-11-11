package com.velorise.staves.network;

import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.ElementalResonanceManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes the local player's active Elemental Resonances and 4-staff Overload state to the client. */
public final class ResonanceSyncPacket {
    private final int resonanceMask;
    private final boolean overloaded;
    private final int staffCount;

    public ResonanceSyncPacket(int resonanceMask, boolean overloaded, int staffCount) {
        this.resonanceMask = resonanceMask;
        this.overloaded = overloaded;
        this.staffCount = staffCount;
    }

    public ResonanceSyncPacket(FriendlyByteBuf buf) {
        this.resonanceMask = buf.readVarInt();
        this.overloaded = buf.readBoolean();
        this.staffCount = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.resonanceMask);
        buf.writeBoolean(this.overloaded);
        buf.writeVarInt(this.staffCount);
    }

    public static void handle(ResonanceSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ElementalResonanceManager.setClientResonanceState(msg.resonanceMask, msg.overloaded, msg.staffCount);
            ClientAuraTracker.updateLocalResonance(msg.resonanceMask, msg.overloaded, msg.staffCount);
        }));
        ctx.setPacketHandled(true);
    }
}
