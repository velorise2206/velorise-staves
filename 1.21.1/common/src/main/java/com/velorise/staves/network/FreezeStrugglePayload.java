package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FreezeStrugglePayload() implements CustomPacketPayload {
    public static final Type<FreezeStrugglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "freeze_struggle"));
    public static final StreamCodec<FriendlyByteBuf, FreezeStrugglePayload> STREAM_CODEC =
            StreamCodec.unit(new FreezeStrugglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
