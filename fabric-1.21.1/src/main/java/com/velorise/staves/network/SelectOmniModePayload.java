package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.entity.OmniOrbEntity;
import com.velorise.staves.item.ArcaneStaffItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record SelectOmniModePayload(byte mode) implements CustomPacketPayload {
    public static final Type<SelectOmniModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "select_omni_mode"));
    public static final StreamCodec<ByteBuf, SelectOmniModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            SelectOmniModePayload::mode,
            SelectOmniModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void applySelection(ServerPlayer player, int rawMode) {
        int clamped = OmniOrbEntity.clampMode(rawMode);
        ItemStack main = player.getMainHandItem();
        if (ArcaneStaffItem.isOmniStaff(main)) {
            ArcaneStaffItem.setOmniModeIndex(main, clamped);
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (ArcaneStaffItem.isOmniStaff(off)) {
            ArcaneStaffItem.setOmniModeIndex(off, clamped);
        }
    }
}
