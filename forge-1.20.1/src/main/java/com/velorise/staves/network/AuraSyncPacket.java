package com.velorise.staves.network;

import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class AuraSyncPacket {
    private final int entityId;
    private final byte elementMask;
    private final byte triggerOrdinal;
    private final byte reactionOrdinal;

    public AuraSyncPacket(int entityId, byte elementMask, byte triggerOrdinal, byte reactionOrdinal) {
        this.entityId = entityId;
        this.elementMask = elementMask;
        this.triggerOrdinal = triggerOrdinal;
        this.reactionOrdinal = reactionOrdinal;
    }

    public AuraSyncPacket(int entityId, Collection<Element> elements, Element trigger, ElementalReaction reaction) {
        this.entityId = entityId;
        byte mask = 0;
        for (Element e : elements) {
            mask |= (byte) (1 << e.ordinal());
        }
        this.elementMask = mask;
        this.triggerOrdinal = trigger != null ? (byte) (trigger.ordinal() + 1) : 0;
        this.reactionOrdinal = reaction != null && reaction != ElementalReaction.NONE ? (byte) (reaction.ordinal() + 1) : 0;
    }

    public AuraSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.elementMask = buf.readByte();
        this.triggerOrdinal = buf.readByte();
        this.reactionOrdinal = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.elementMask);
        buf.writeByte(this.triggerOrdinal);
        buf.writeByte(this.reactionOrdinal);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            List<Element> elements = new ArrayList<>();
            for (Element e : Element.values()) {
                if ((this.elementMask & (1 << e.ordinal())) != 0) {
                    elements.add(e);
                }
            }
            Element trigger = (this.triggerOrdinal > 0 && this.triggerOrdinal <= Element.values().length)
                    ? Element.values()[this.triggerOrdinal - 1] : null;
            ElementalReaction reaction = (this.reactionOrdinal > 0 && this.reactionOrdinal <= ElementalReaction.values().length)
                    ? ElementalReaction.values()[this.reactionOrdinal - 1] : null;

            ClientAuraTracker.update(this.entityId, elements, trigger, reaction);
        });
        ctx.setPacketHandled(true);
    }
}
