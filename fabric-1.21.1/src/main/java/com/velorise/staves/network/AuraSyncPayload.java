package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record AuraSyncPayload(
        int entityId,
        byte activeMask,
        byte reactedAuraOrdinal,
        byte triggerElementOrdinal,
        byte reactionOrdinal
) implements CustomPacketPayload {
    public static final Type<AuraSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "aura_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuraSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AuraSyncPayload::entityId,
            ByteBufCodecs.BYTE, AuraSyncPayload::activeMask,
            ByteBufCodecs.BYTE, AuraSyncPayload::reactedAuraOrdinal,
            ByteBufCodecs.BYTE, AuraSyncPayload::triggerElementOrdinal,
            ByteBufCodecs.BYTE, AuraSyncPayload::reactionOrdinal,
            AuraSyncPayload::new
    );

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        this(
                entityId,
                toMask(elements),
                reactedAura != null ? (byte) (reactedAura.ordinal() + 1) : 0,
                trigger != null ? (byte) (trigger.ordinal() + 1) : 0,
                reaction != null && reaction != ElementalReaction.NONE ? (byte) (reaction.ordinal() + 1) : 0
        );
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
