package com.velorise.staves.network;

import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FreezeStrugglePacket {
    public FreezeStrugglePacket() {}

    public FreezeStrugglePacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ElementalGaugeManager.handlePlayerFreezeStruggle(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
