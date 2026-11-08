package com.velorise.staves.network;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(AuraSyncPayload.TYPE, AuraSyncPayload.STREAM_CODEC);
    }

    public static void sendAuraSync(Entity target, Collection<Element> elements, Element trigger, ElementalReaction reaction) {
        if (target.level().isClientSide()) return;
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, trigger, reaction);
        for (ServerPlayer player : PlayerLookup.tracking(target)) {
            ServerPlayNetworking.send(player, payload);
        }
        if (target instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, payload);
        }
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, null, null);
        ServerPlayNetworking.send(player, payload);
    }
}
