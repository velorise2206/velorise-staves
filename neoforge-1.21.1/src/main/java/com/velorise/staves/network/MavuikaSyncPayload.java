package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraTracker;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MavuikaSyncPayload(int entityId, int auraRemainingTicks, float nightsoulPoints, boolean netherite, int ultiCooldownTicks)
        implements CustomPacketPayload {
    public static final Type<MavuikaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "mavuika_sync"));

    public static final StreamCodec<ByteBuf, MavuikaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MavuikaSyncPayload::entityId,
            ByteBufCodecs.VAR_INT,
            MavuikaSyncPayload::auraRemainingTicks,
            ByteBufCodecs.FLOAT,
            MavuikaSyncPayload::nightsoulPoints,
            ByteBufCodecs.BOOL,
            MavuikaSyncPayload::netherite,
            ByteBufCodecs.VAR_INT,
            MavuikaSyncPayload::ultiCooldownTicks,
            MavuikaSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MavuikaSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientAuraTracker.updateMavuika(
                payload.entityId(), payload.auraRemainingTicks(), payload.nightsoulPoints(), payload.netherite(), payload.ultiCooldownTicks()
        ));
    }
}
