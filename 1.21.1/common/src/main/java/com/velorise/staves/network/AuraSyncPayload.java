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

/**
 * S2C Packet syncing an entity's active elemental auras and any newly triggered reaction.
 */
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

    public static final Type<AuraSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "aura_sync"));

    public static final StreamCodec<ByteBuf, AuraSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AuraSyncPayload decode(ByteBuf buf) {
            int entityId = ByteBufCodecs.VAR_INT.decode(buf);
            byte activeMask = ByteBufCodecs.BYTE.decode(buf);
            byte reactedAuraOrdinal = ByteBufCodecs.BYTE.decode(buf);
            byte triggerElementOrdinal = ByteBufCodecs.BYTE.decode(buf);
            byte reactionOrdinal = ByteBufCodecs.BYTE.decode(buf);
            float damage = ByteBufCodecs.FLOAT.decode(buf);
            int durationTicks = ByteBufCodecs.VAR_INT.decode(buf);
            boolean hasPos = ByteBufCodecs.BOOL.decode(buf);
            double posX = hasPos ? ByteBufCodecs.DOUBLE.decode(buf) : 0.0D;
            double posY = hasPos ? ByteBufCodecs.DOUBLE.decode(buf) : 0.0D;
            double posZ = hasPos ? ByteBufCodecs.DOUBLE.decode(buf) : 0.0D;
            return new AuraSyncPayload(
                    entityId, activeMask, reactedAuraOrdinal, triggerElementOrdinal, reactionOrdinal,
                    damage, durationTicks, hasPos, posX, posY, posZ
            );
        }

        @Override
        public void encode(ByteBuf buf, AuraSyncPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buf, payload.entityId);
            ByteBufCodecs.BYTE.encode(buf, payload.activeMask);
            ByteBufCodecs.BYTE.encode(buf, payload.reactedAuraOrdinal);
            ByteBufCodecs.BYTE.encode(buf, payload.triggerElementOrdinal);
            ByteBufCodecs.BYTE.encode(buf, payload.reactionOrdinal);
            ByteBufCodecs.FLOAT.encode(buf, payload.damage);
            ByteBufCodecs.VAR_INT.encode(buf, payload.durationTicks);
            ByteBufCodecs.BOOL.encode(buf, payload.hasPos);
            if (payload.hasPos) {
                ByteBufCodecs.DOUBLE.encode(buf, payload.posX);
                ByteBufCodecs.DOUBLE.encode(buf, payload.posY);
                ByteBufCodecs.DOUBLE.encode(buf, payload.posZ);
            }
        }
    };

    public AuraSyncPayload(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks,
            boolean frozen,
            boolean hasPos,
            double posX,
            double posY,
            double posZ
    ) {
        this(
                entityId,
                packMask(elements, frozen),
                (byte) (reactedAura != null ? reactedAura.ordinal() + 1 : 0),
                (byte) (trigger != null ? trigger.ordinal() + 1 : 0),
                (byte) (reaction != null && reaction != ElementalReaction.NONE ? reaction.ordinal() + 1 : 0),
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
            boolean frozen
    ) {
        this(entityId, elements, reactedAura, trigger, reaction, damage, durationTicks, frozen, false, 0.0D, 0.0D, 0.0D);
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
        this(entityId, elements, reactedAura, trigger, reaction, damage, durationTicks, false, false, 0.0D, 0.0D, 0.0D);
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

    private static byte packMask(Collection<Element> elements, boolean frozen) {
        byte mask = 0;
        if (elements != null) {
            for (Element e : elements) {
                mask |= (byte) (1 << e.ordinal());
            }
        }
        if (frozen) {
            mask |= (byte) 0x80;
        }
        return mask;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(AuraSyncPayload payload) {
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

        boolean isFrozen = (payload.activeMask & 0x80) != 0;
        com.velorise.staves.client.ClientAuraTracker.update(
                payload.entityId, elements, reactedAura, trigger, reaction, payload.damage, payload.durationTicks, isFrozen,
                payload.hasPos, payload.posX, payload.posY, payload.posZ
        );
    }
}
