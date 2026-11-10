package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.ElementalGaugeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FreezeStrugglePayload() implements CustomPacketPayload {
    public static final Type<FreezeStrugglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "freeze_struggle"));
    public static final StreamCodec<FriendlyByteBuf, FreezeStrugglePayload> STREAM_CODEC =
            StreamCodec.unit(new FreezeStrugglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FreezeStrugglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                ElementalGaugeManager.handlePlayerFreezeStruggle(sp);
            }
        });
    }
}
