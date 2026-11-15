package com.velorise.staves.network;

import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RaidenSlashPacket {
    private final boolean charged;

    public RaidenSlashPacket(boolean charged) {
        this.charged = charged;
    }

    public RaidenSlashPacket(FriendlyByteBuf buf) {
        this.charged = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.charged);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ElementalGaugeManager.performRaidenMusouSwing(player, this.charged);
            }
        });
        ctx.setPacketHandled(true);
    }
}
