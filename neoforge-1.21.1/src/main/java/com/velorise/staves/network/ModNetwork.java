package com.velorise.staves.network;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(AuraSyncPayload.TYPE, AuraSyncPayload.STREAM_CODEC, AuraSyncPayload::handle);
        registrar.playToClient(ElectricArcPayload.TYPE, ElectricArcPayload.STREAM_CODEC, ElectricArcPayload::handle);
        registrar.playToServer(FreezeStrugglePayload.TYPE, FreezeStrugglePayload.STREAM_CODEC, FreezeStrugglePayload::handle);
    }

    public static void sendElectricArc(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end, int durationTicks, int color) {
        ElectricArcPayload payload = new ElectricArcPayload(-1, start.x, start.y, start.z, -1, end.x, end.y, end.z, durationTicks, color);
        PacketDistributor.sendToPlayersNear(level, null, start.x, start.y, start.z, 64.0D, payload);
    }

    public static void sendElectricArc(net.minecraft.server.level.ServerLevel level, Entity from, Entity to, int durationTicks, int color) {
        net.minecraft.world.phys.Vec3 start = from.position().add(0, from.getBbHeight() * 0.45D, 0);
        net.minecraft.world.phys.Vec3 end = to.position().add(0, to.getBbHeight() * 0.45D, 0);
        ElectricArcPayload payload = new ElectricArcPayload(from.getId(), start.x, start.y, start.z, to.getId(), end.x, end.y, end.z, durationTicks, color);
        PacketDistributor.sendToPlayersNear(level, null, start.x, start.y, start.z, 64.0D, payload);
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
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        sendAuraSyncToPlayer(player, target, elements, 0);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements, int durationTicks) {
        AuraSyncPayload payload = new AuraSyncPayload(target.getId(), elements, null, null, null, 0.0F, durationTicks, com.velorise.staves.element.ElementalGaugeManager.isEntityFrozen(target));
        PacketDistributor.sendToPlayer(player, payload);
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
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendFreezeStruggle() {
        PacketDistributor.sendToServer(new FreezeStrugglePayload());
    }
}
