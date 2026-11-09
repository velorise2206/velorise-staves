package com.velorise.staves.network;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(AuraSyncPayload.TYPE, AuraSyncPayload.STREAM_CODEC);
    }

    public static void sendAuraSync(
            Entity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction
    ) {
        sendAuraSync(target, elements, reactedAura, trigger, reaction, 0.0F);
    }

    public static void sendAuraSync(
            Entity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage
    ) {
        sendAuraSync(target, elements, reactedAura, trigger, reaction, damage, 0);
    }

    public static void sendAuraSync(
            Entity target,
            Collection<Element> elements,
            @Nullable Element reactedAura,
            @Nullable Element trigger,
            @Nullable ElementalReaction reaction,
            float damage,
            int durationTicks
    ) {
        if (target.level().isClientSide()) return;
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, reactedAura, trigger, reaction, damage, durationTicks);
        Set<ServerPlayer> recipients = new HashSet<>(PlayerLookup.tracking(target));
        if (target.level() instanceof ServerLevel serverLevel) {
            recipients.addAll(PlayerLookup.around(serverLevel, target.position(), 64.0D));
        }
        if (target instanceof ServerPlayer self) {
            recipients.add(self);
        }
        for (ServerPlayer player : recipients) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        sendAuraSyncToPlayer(player, target, elements, 0);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements, int durationTicks) {
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, null, null, null, 0.0F, durationTicks);
        ServerPlayNetworking.send(player, payload);
    }
}
