package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RaidenSlashPayload(boolean charged) implements CustomPacketPayload {
    public static final Type<RaidenSlashPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "raiden_slash"));

    public static final StreamCodec<ByteBuf, RaidenSlashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RaidenSlashPayload::charged,
            RaidenSlashPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
