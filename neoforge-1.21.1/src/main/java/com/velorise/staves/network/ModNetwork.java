package com.velorise.staves.network;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Collection;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(AuraSyncPayload.TYPE, AuraSyncPayload.STREAM_CODEC, AuraSyncPayload::handle);
    }

    public static void sendAuraSync(Entity target, Collection<Element> elements, Element trigger, ElementalReaction reaction) {
        if (target.level().isClientSide()) return;
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, trigger, reaction);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
        if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, target.getX(), target.getY(), target.getZ(), 64.0D, payload);
        }
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, null, null);
        PacketDistributor.sendToPlayer(player, payload);
    }
}
