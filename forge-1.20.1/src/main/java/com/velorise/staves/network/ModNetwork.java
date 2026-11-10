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
import org.jetbrains.annotations.Nullable;

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

        CHANNEL.messageBuilder(ElectricArcPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ElectricArcPacket::new)
                .encoder(ElectricArcPacket::toBytes)
                .consumerMainThread(ElectricArcPacket::handle)
                .add();
    }

    public static void sendElectricArc(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end, int durationTicks, int color) {
        ElectricArcPacket packet = new ElectricArcPacket(-1, start.x, start.y, start.z, -1, end.x, end.y, end.z, durationTicks, color);
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(start.x, start.y, start.z, 64.0D, level.dimension())), packet);
    }

    public static void sendElectricArc(net.minecraft.server.level.ServerLevel level, Entity from, Entity to, int durationTicks, int color) {
        net.minecraft.world.phys.Vec3 start = from.position().add(0, from.getBbHeight() * 0.45D, 0);
        net.minecraft.world.phys.Vec3 end = to.position().add(0, to.getBbHeight() * 0.45D, 0);
        ElectricArcPacket packet = new ElectricArcPacket(from.getId(), start.x, start.y, start.z, to.getId(), end.x, end.y, end.z, durationTicks, color);
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(start.x, start.y, start.z, 64.0D, level.dimension())), packet);
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
        AuraSyncPacket packet = new AuraSyncPacket(target.getId(), elements, reactedAura, trigger, reaction, damage, durationTicks);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        sendAuraSyncToPlayer(player, target, elements, 0);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements, int durationTicks) {
        AuraSyncPacket packet = new AuraSyncPacket(target.getId(), elements, null, null, null, 0.0F, durationTicks);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendCrystallizeShieldToPlayer(ServerPlayer player, @Nullable Element element, int durationTicks) {
        AuraSyncPacket packet = new AuraSyncPacket(
                player.getId(),
                java.util.Collections.emptyList(),
                element,
                null,
                ElementalReaction.CRYSTALLIZE,
                0.0F,
                durationTicks
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
