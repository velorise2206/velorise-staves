package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RaidenSyncPayload(
        int entityId,
        int eyeRemainingTicks,
        int musouRemainingTicks,
        int resolve,
        boolean netherite
) implements CustomPacketPayload {
    public static final Type<RaidenSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "raiden_sync"));
    public static final StreamCodec<ByteBuf, RaidenSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RaidenSyncPayload::entityId,
            ByteBufCodecs.VAR_INT,
            RaidenSyncPayload::eyeRemainingTicks,
            ByteBufCodecs.VAR_INT,
            RaidenSyncPayload::musouRemainingTicks,
            ByteBufCodecs.VAR_INT,
            RaidenSyncPayload::resolve,
            ByteBufCodecs.BOOL,
            RaidenSyncPayload::netherite,
            RaidenSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
