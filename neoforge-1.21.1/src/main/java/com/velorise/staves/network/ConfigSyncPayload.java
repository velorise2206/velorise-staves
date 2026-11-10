package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.config.StavesConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Synchronizes server configuration values (multipliers, cooldowns, ranges, durabilities)
 * to connected clients on login so item tooltips and client-side prediction always match the server.
 */
public record ConfigSyncPayload(StavesConfig.SyncedData data) implements CustomPacketPayload {
    public static final Type<ConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> payload.write(buf),
            ConfigSyncPayload::new
    );

    public ConfigSyncPayload(FriendlyByteBuf buf) {
        this(StavesConfig.SyncedData.read(buf));
    }

    public void write(FriendlyByteBuf buf) {
        data.write(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> StavesConfig.applyServerSync(payload.data()));
    }
}
