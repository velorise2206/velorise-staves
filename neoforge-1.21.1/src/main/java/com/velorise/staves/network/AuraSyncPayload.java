package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record AuraSyncPayload(
        int entityId,
        byte activeMask,
        byte reactedAuraOrdinal,
        byte triggerElementOrdinal,
        byte reactionOrdinal,
        float damage,
        int durationTicks
) implements CustomPacketPayload {
    public static final Type<AuraSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "aura_sync"));

    public static final StreamCodec<ByteBuf, AuraSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.VAR_INT.encode(buf, payload.entityId());
                ByteBufCodecs.BYTE.encode(buf, payload.activeMask());
                ByteBufCodecs.BYTE.encode(buf, payload.reactedAuraOrdinal());
                ByteBufCodecs.BYTE.encode(buf, payload.triggerElementOrdinal());
                ByteBufCodecs.BYTE.encode(buf, payload.reactionOrdinal());
                ByteBufCodecs.FLOAT.encode(buf, payload.damage());
                ByteBufCodecs.VAR_INT.encode(buf, payload.durationTicks());
            },
            buf -> new AuraSyncPayload(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BYTE.decode(buf),
                    ByteBufCodecs.BYTE.decode(buf),
                    ByteBufCodecs.BYTE.decode(buf),
                    ByteBufCodecs.BYTE.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks
    ) {
        this(
                entityId,
                toMask(elements),
                reactedAura != null ? (byte) (reactedAura.ordinal() + 1) : 0,
                trigger != null ? (byte) (trigger.ordinal() + 1) : 0,
                reaction != null && reaction != ElementalReaction.NONE ? (byte) (reaction.ordinal() + 1) : 0,
                damage,
                durationTicks
        );
    }

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage
    ) {
        this(entityId, elements, reactedAura, trigger, reaction, damage, 0);
    }

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        this(entityId, elements, reactedAura, trigger, reaction, 0.0F);
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

    public static void handle(AuraSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadHandler.handle(payload));
    }

    private static final class ClientPayloadHandler {
        private static void handle(AuraSyncPayload payload) {
            List<Element> elements = new ArrayList<>();
            for (Element e : Element.values()) {
                if ((payload.activeMask & (1 << e.ordinal())) != 0) {
                    elements.add(e);
                }
            }
            Element reactedAura = (payload.reactedAuraOrdinal > 0 && payload.reactedAuraOrdinal <= Element.values().length)
                    ? Element.values()[payload.reactedAuraOrdinal - 1] : null;
            Element trigger = (payload.triggerElementOrdinal > 0 && payload.triggerElementOrdinal <= Element.values().length)
                    ? Element.values()[payload.triggerElementOrdinal - 1] : null;
            ElementalReaction reaction = (payload.reactionOrdinal > 0 && payload.reactionOrdinal <= ElementalReaction.values().length)
                    ? ElementalReaction.values()[payload.reactionOrdinal - 1] : null;

            com.velorise.staves.client.ClientAuraTracker.update(payload.entityId, elements, reactedAura, trigger, reaction, payload.damage, payload.durationTicks);
        }
    }
}
