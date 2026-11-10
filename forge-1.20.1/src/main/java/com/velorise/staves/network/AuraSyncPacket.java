package com.velorise.staves.network;

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
    private final float damage;
    private final int durationTicks;

    public AuraSyncPacket(
            int entityId,
            byte activeMask,
            byte reactedAuraOrdinal,
            byte triggerElementOrdinal,
            byte reactionOrdinal,
            float damage,
            int durationTicks
    ) {
        this.entityId = entityId;
        this.activeMask = activeMask;
        this.reactedAuraOrdinal = reactedAuraOrdinal;
        this.triggerElementOrdinal = triggerElementOrdinal;
        this.reactionOrdinal = reactionOrdinal;
        this.damage = damage;
        this.durationTicks = durationTicks;
    }

    public AuraSyncPacket(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks,
            boolean isFrozen
    ) {
        this.entityId = entityId;
        byte mask = 0;
        for (Element e : elements) {
            mask |= (byte) (1 << e.ordinal());
        }
        if (isFrozen) {
            mask |= (byte) 0x80;
        }
        this.activeMask = mask;
        this.reactedAuraOrdinal = reactedAura != null ? (byte) (reactedAura.ordinal() + 1) : 0;
        this.triggerElementOrdinal = trigger != null ? (byte) (trigger.ordinal() + 1) : 0;
        this.reactionOrdinal = reaction != null && reaction != ElementalReaction.NONE ? (byte) (reaction.ordinal() + 1) : 0;
        this.damage = damage;
        this.durationTicks = durationTicks;
    }

    public AuraSyncPacket(
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

    public AuraSyncPacket(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage
    ) {
        this(entityId, elements, reactedAura, trigger, reaction, damage, 0);
    }

    public AuraSyncPacket(
            int entityId,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        this(entityId, elements, reactedAura, trigger, reaction, 0.0F, 0);
    }

    public AuraSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.activeMask = buf.readByte();
        this.reactedAuraOrdinal = buf.readByte();
        this.triggerElementOrdinal = buf.readByte();
        this.reactionOrdinal = buf.readByte();
        this.damage = buf.readFloat();
        this.durationTicks = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.activeMask);
        buf.writeByte(this.reactedAuraOrdinal);
        buf.writeByte(this.triggerElementOrdinal);
        buf.writeByte(this.reactionOrdinal);
        buf.writeFloat(this.damage);
        buf.writeVarInt(this.durationTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleAuraSync(this)
        ));
        ctx.setPacketHandled(true);
    }

    private static final class ClientPacketHandler {
        private static void handleAuraSync(AuraSyncPacket pkt) {
            List<Element> elements = new ArrayList<>();
            for (Element e : Element.values()) {
                if ((pkt.activeMask & (1 << e.ordinal())) != 0) {
                    elements.add(e);
                }
            }
            Element reactedAura = (pkt.reactedAuraOrdinal > 0 && pkt.reactedAuraOrdinal <= Element.values().length)
                    ? Element.values()[pkt.reactedAuraOrdinal - 1] : null;
            Element trigger = (pkt.triggerElementOrdinal > 0 && pkt.triggerElementOrdinal <= Element.values().length)
                    ? Element.values()[pkt.triggerElementOrdinal - 1] : null;
            ElementalReaction reaction = (pkt.reactionOrdinal > 0 && pkt.reactionOrdinal <= ElementalReaction.values().length)
                    ? ElementalReaction.values()[pkt.reactionOrdinal - 1] : null;

            boolean isFrozen = (pkt.activeMask & 0x80) != 0;
            com.velorise.staves.client.ClientAuraTracker.update(pkt.entityId, elements, reactedAura, trigger, reaction, pkt.damage, pkt.durationTicks, isFrozen);
        }
    }
}
