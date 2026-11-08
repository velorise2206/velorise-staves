package com.velorise.staves.network;

import com.velorise.staves.client.ClientAuraTracker;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class AuraSyncPacket {
    private final int entityId;
    private final byte activeMask;
    private final byte reactedAuraOrdinal;
    private final byte triggerElementOrdinal;
    private final byte reactionOrdinal;

    public AuraSyncPacket(
            int entityId,
            byte activeMask,
            byte reactedAuraOrdinal,
            byte triggerElementOrdinal,
            byte reactionOrdinal
    ) {
        this.entityId = entityId;
        this.activeMask = activeMask;
        this.reactedAuraOrdinal = reactedAuraOrdinal;
        this.triggerElementOrdinal = triggerElementOrdinal;
        this.reactionOrdinal = reactionOrdinal;
    }

    public AuraSyncPacket(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        this.entityId = entityId;
        byte mask = 0;
        for (Element e : elements) {
            mask |= (byte) (1 << e.ordinal());
        }
        this.activeMask = mask;
        this.reactedAuraOrdinal = reactedAura != null ? (byte) (reactedAura.ordinal() + 1) : 0;
        this.triggerElementOrdinal = trigger != null ? (byte) (trigger.ordinal() + 1) : 0;
        this.reactionOrdinal = reaction != null && reaction != ElementalReaction.NONE ? (byte) (reaction.ordinal() + 1) : 0;
    }

    public AuraSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.activeMask = buf.readByte();
        this.reactedAuraOrdinal = buf.readByte();
        this.triggerElementOrdinal = buf.readByte();
        this.reactionOrdinal = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.activeMask);
        buf.writeByte(this.reactedAuraOrdinal);
        buf.writeByte(this.triggerElementOrdinal);
        buf.writeByte(this.reactionOrdinal);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            List<Element> elements = new ArrayList<>();
            for (Element e : Element.values()) {
                if ((this.activeMask & (1 << e.ordinal())) != 0) {
                    elements.add(e);
                }
            }
            Element reactedAura = (this.reactedAuraOrdinal > 0 && this.reactedAuraOrdinal <= Element.values().length)
                    ? Element.values()[this.reactedAuraOrdinal - 1] : null;
            Element trigger = (this.triggerElementOrdinal > 0 && this.triggerElementOrdinal <= Element.values().length)
                    ? Element.values()[this.triggerElementOrdinal - 1] : null;
            ElementalReaction reaction = (this.reactionOrdinal > 0 && this.reactionOrdinal <= ElementalReaction.values().length)
                    ? ElementalReaction.values()[this.reactionOrdinal - 1] : null;

            ClientAuraTracker.update(this.entityId, elements, reactedAura, trigger, reaction);
        });
        ctx.setPacketHandled(true);
    }
}
