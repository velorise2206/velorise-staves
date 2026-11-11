package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
        int durationTicks,
        boolean hasPos,
        double posX,
        double posY,
        double posZ
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
                buf.writeBoolean(payload.hasPos());
                if (payload.hasPos()) {
                    buf.writeDouble(payload.posX());
                    buf.writeDouble(payload.posY());
                    buf.writeDouble(payload.posZ());
                }
            },
            buf -> {
                int entityId = ByteBufCodecs.VAR_INT.decode(buf);
                byte activeMask = ByteBufCodecs.BYTE.decode(buf);
                byte reactedAuraOrdinal = ByteBufCodecs.BYTE.decode(buf);
                byte triggerElementOrdinal = ByteBufCodecs.BYTE.decode(buf);
                byte reactionOrdinal = ByteBufCodecs.BYTE.decode(buf);
                float damage = ByteBufCodecs.FLOAT.decode(buf);
                int durationTicks = ByteBufCodecs.VAR_INT.decode(buf);
                boolean hasPos = buf.readBoolean();
                double px = hasPos ? buf.readDouble() : 0.0D;
                double py = hasPos ? buf.readDouble() : 0.0D;
                double pz = hasPos ? buf.readDouble() : 0.0D;
                return new AuraSyncPayload(
                        entityId, activeMask, reactedAuraOrdinal, triggerElementOrdinal, reactionOrdinal,
                        damage, durationTicks, hasPos, px, py, pz
                );
            }
    );

    public AuraSyncPayload(
            int entityId,
            byte activeMask,
            byte reactedAuraOrdinal,
            byte triggerElementOrdinal,
            byte reactionOrdinal,
            float damage,
            int durationTicks
    ) {
        this(entityId, activeMask, reactedAuraOrdinal, triggerElementOrdinal, reactionOrdinal, damage, durationTicks, false, 0.0D, 0.0D, 0.0D);
    }

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks,
            boolean isFrozen,
            boolean hasPos,
            double posX,
            double posY,
            double posZ
    ) {
        this(
                entityId,
                (byte) (toMask(elements) | (isFrozen ? 0x80 : 0)),
                reactedAura != null ? (byte) (reactedAura.ordinal() + 1) : 0,
                trigger != null ? (byte) (trigger.ordinal() + 1) : 0,
                reaction != null && reaction != ElementalReaction.NONE ? (byte) (reaction.ordinal() + 1) : 0,
                damage,
                durationTicks,
                hasPos,
                posX,
                posY,
                posZ
        );
    }

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks,
            boolean isFrozen
    ) {
        this(entityId, elements, reactedAura, trigger, reaction, damage, durationTicks, isFrozen, false, 0.0D, 0.0D, 0.0D);
    }

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks
    ) {
        this(entityId, elements, reactedAura, trigger, reaction, damage, durationTicks, reaction == ElementalReaction.FROZEN);
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
        this(entityId, elements, reactedAura, trigger, reaction, 0.0F, 0);
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
