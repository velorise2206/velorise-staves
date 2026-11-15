package com.velorise.staves.network;

import com.velorise.staves.element.ElementalResonanceManager;
import com.velorise.staves.entity.OmniOrbEntity;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectOmniModePacket {
    private final byte mode;

    public SelectOmniModePacket(int mode) {
        this.mode = (byte) OmniOrbEntity.clampMode(mode);
    }

    public SelectOmniModePacket(FriendlyByteBuf buf) {
        this.mode = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(this.mode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                int clamped = OmniOrbEntity.clampMode(this.mode);
                ItemStack main = player.getMainHandItem();
                if (ArcaneStaffItem.isOmniStaff(main)) {
                    ArcaneStaffItem.setOmniModeIndex(main, clamped);
                    ElementalResonanceManager.syncOmniModeCooldown(player, main, clamped);
                    ElementalResonanceManager.syncPlayerImmediate(player);
                    return;
                }
                ItemStack off = player.getOffhandItem();
                if (ArcaneStaffItem.isOmniStaff(off)) {
                    ArcaneStaffItem.setOmniModeIndex(off, clamped);
                    ElementalResonanceManager.syncOmniModeCooldown(player, off, clamped);
                    ElementalResonanceManager.syncPlayerImmediate(player);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
