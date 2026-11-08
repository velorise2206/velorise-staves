package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public record AuraSyncPayload(int entityId, byte elementMask, byte triggerOrdinal, byte reactionOrdinal) implements CustomPacketPayload {
    public static final Type<AuraSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "aura_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuraSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AuraSyncPayload::entityId,
            ByteBufCodecs.BYTE, AuraSyncPayload::elementMask,
            ByteBufCodecs.BYTE, AuraSyncPayload::triggerOrdinal,
            ByteBufCodecs.BYTE, AuraSyncPayload::reactionOrdinal,
            AuraSyncPayload::new
    );

    public AuraSyncPayload(int entityId, Collection<Element> elements, Element trigger, ElementalReaction reaction) {
        this(entityId, toMask(elements),
                trigger != null ? (byte) (trigger.ordinal() + 1) : 0,
                reaction != null && reaction != ElementalReaction.NONE ? (byte) (reaction.ordinal() + 1) : 0);
    }

    private static byte toMask(Collection<Element> elements) {
        byte mask = 0;
        for (Element e : elements) {
            mask |= (byte) (1 << e.ordinal());
        }
        return mask;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
