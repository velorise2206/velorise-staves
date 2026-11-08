package com.velorise.staves.network;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Collection;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VeloriseStavesMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(AuraSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(AuraSyncPacket::new)
                .encoder(AuraSyncPacket::toBytes)
                .consumerMainThread(AuraSyncPacket::handle)
                .add();
    }

    public static void sendAuraSync(Entity target, Collection<Element> elements, Element trigger, ElementalReaction reaction) {
        if (target.level().isClientSide()) return;
        AuraSyncPacket packet = new AuraSyncPacket(target.getId(), elements, trigger, reaction);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        AuraSyncPacket packet = new AuraSyncPacket(target.getId(), elements, null, null);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
