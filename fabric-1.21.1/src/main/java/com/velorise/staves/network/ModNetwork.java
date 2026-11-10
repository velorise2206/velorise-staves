package com.velorise.staves.network;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(AuraSyncPayload.TYPE, AuraSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ElectricArcPayload.TYPE, ElectricArcPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(FreezeStrugglePayload.TYPE, FreezeStrugglePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FreezeStrugglePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> com.velorise.staves.element.ElementalGaugeManager.handlePlayerFreezeStruggle(context.player()));
        });
    }

    public static void sendElectricArc(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end, int durationTicks, int color) {
        ElectricArcPayload payload = new ElectricArcPayload(-1, start.x, start.y, start.z, -1, end.x, end.y, end.z, durationTicks, color);
        for (ServerPlayer player : PlayerLookup.around(level, start, 64.0D)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendElectricArc(net.minecraft.server.level.ServerLevel level, Entity from, Entity to, int durationTicks, int color) {
        net.minecraft.world.phys.Vec3 start = from.position().add(0, from.getBbHeight() * 0.45D, 0);
        net.minecraft.world.phys.Vec3 end = to.position().add(0, to.getBbHeight() * 0.45D, 0);
        ElectricArcPayload payload = new ElectricArcPayload(from.getId(), start.x, start.y, start.z, to.getId(), end.x, end.y, end.z, durationTicks, color);
        for (ServerPlayer player : PlayerLookup.around(level, start, 64.0D)) {
            ServerPlayNetworking.send(player, payload);
        }
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
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, reactedAura, trigger, reaction, damage, durationTicks, com.velorise.staves.element.ElementalGaugeManager.isEntityFrozen(target));
        for (ServerPlayer player : PlayerLookup.tracking(target)) {
            ServerPlayNetworking.send(player, payload);
        }
        if (target instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, payload);
        }
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        sendAuraSyncToPlayer(player, target, elements, 0);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements, int durationTicks) {
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, null, null, null, 0.0F, durationTicks, com.velorise.staves.element.ElementalGaugeManager.isEntityFrozen(target));
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendCrystallizeShieldToPlayer(ServerPlayer player, @Nullable Element element, int durationTicks) {
        byte elemOrd = element != null ? (byte) (element.ordinal() + 1) : 0;
        AuraSyncPayload payload = new AuraSyncPayload(
                player.getId(),
                (byte) 0,
                elemOrd,
                (byte) 0,
                (byte) (ElementalReaction.CRYSTALLIZE.ordinal() + 1),
                0.0F,
                durationTicks
        );
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendFreezeStruggle() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new FreezeStrugglePayload());
    }
}
