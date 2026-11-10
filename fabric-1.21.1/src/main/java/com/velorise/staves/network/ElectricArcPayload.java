package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ElectricArcPayload(
        int fromEntityId,
        double startX, double startY, double startZ,
        int toEntityId,
        double endX, double endY, double endZ,
        int durationTicks,
        int color
) implements CustomPacketPayload {
    public static final Type<ElectricArcPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "electric_arc"));

    public static final StreamCodec<FriendlyByteBuf, ElectricArcPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> payload.write(buf),
            ElectricArcPayload::new
    );

    public ElectricArcPayload(FriendlyByteBuf buf) {
        this(
                buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readVarInt(),
                buf.readInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(fromEntityId);
        buf.writeDouble(startX);
        buf.writeDouble(startY);
        buf.writeDouble(startZ);
        buf.writeVarInt(toEntityId);
        buf.writeDouble(endX);
        buf.writeDouble(endY);
        buf.writeDouble(endZ);
        buf.writeVarInt(durationTicks);
        buf.writeInt(color);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
