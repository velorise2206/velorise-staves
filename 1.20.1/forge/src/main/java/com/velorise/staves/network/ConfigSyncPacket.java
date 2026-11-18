package com.velorise.staves.network;

import com.velorise.staves.config.StavesConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Synchronizes server configuration values (multipliers, cooldowns, ranges, durabilities)
 * to connected clients on login so item tooltips and client-side prediction always match the server.
 */
public final class ConfigSyncPacket {
    private final StavesConfig.SyncedData data;

    public ConfigSyncPacket(StavesConfig.SyncedData data) {
        this.data = data;
    }

    public ConfigSyncPacket(FriendlyByteBuf buf) {
        this.data = StavesConfig.SyncedData.read(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        this.data.write(buf);
    }

    public static void handle(ConfigSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> StavesConfig.applyServerSync(msg.data));
        ctx.setPacketHandled(true);
    }
}
