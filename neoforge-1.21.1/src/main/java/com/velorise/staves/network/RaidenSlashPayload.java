package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.ElementalGaugeManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    public static void handle(RaidenSlashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ElementalGaugeManager.performRaidenMusouSwing(player, payload.charged());
            }
        });
    }
}
