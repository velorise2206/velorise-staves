package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.ElementalResonanceManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Synchronizes the local player's active Elemental Resonances and 4-staff Overload state to the client. */
public record ResonanceSyncPayload(int resonanceMask, boolean overloaded, int staffCount) implements CustomPacketPayload {
    public static final Type<ResonanceSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "resonance_sync"));

    public static final StreamCodec<ByteBuf, ResonanceSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ResonanceSyncPayload::resonanceMask,
            ByteBufCodecs.BOOL,
            ResonanceSyncPayload::overloaded,
            ByteBufCodecs.VAR_INT,
            ResonanceSyncPayload::staffCount,
            ResonanceSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResonanceSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHandler.handle(payload));
    }

    private static final class ClientHandler {
        private static void handle(ResonanceSyncPayload payload) {
            ElementalResonanceManager.setClientResonanceState(payload.resonanceMask(), payload.overloaded(), payload.staffCount());
            ClientAuraTracker.updateLocalResonance(payload.resonanceMask(), payload.overloaded(), payload.staffCount());
        }
    }
}
