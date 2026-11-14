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

        CHANNEL.messageBuilder(FreezeStrugglePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(FreezeStrugglePacket::new)
                .encoder(FreezeStrugglePacket::toBytes)
                .consumerMainThread(FreezeStrugglePacket::handle)
                .add();

        CHANNEL.messageBuilder(ConfigSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ConfigSyncPacket::new)
                .encoder(ConfigSyncPacket::toBytes)
                .consumerMainThread(ConfigSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(ResonanceSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ResonanceSyncPacket::new)
                .encoder(ResonanceSyncPacket::toBytes)
                .consumerMainThread(ResonanceSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectOmniModePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectOmniModePacket::new)
                .encoder(SelectOmniModePacket::toBytes)
                .consumerMainThread(SelectOmniModePacket::handle)
                .add();

        CHANNEL.messageBuilder(MavuikaSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(MavuikaSyncPacket::new)
                .encoder(MavuikaSyncPacket::toBytes)
                .consumerMainThread(MavuikaSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(MavuikaActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(MavuikaActionPacket::new)
                .encoder(MavuikaActionPacket::toBytes)
                .consumerMainThread(MavuikaActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(RaidenSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(RaidenSyncPacket::new)
                .encoder(RaidenSyncPacket::toBytes)
                .consumerMainThread(RaidenSyncPacket::handle)
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
        boolean hasPos = (reaction != null && reaction != ElementalReaction.NONE) || damage > 0.0F;
        double px = target.getX();
        double py = target.getY() + Math.max(0.65D, target.getBbHeight() * 0.65D);
        double pz = target.getZ();
        AuraSyncPacket packet = new AuraSyncPacket(
                target.getId(), elements, reactedAura, trigger, reaction, damage, durationTicks,
                com.velorise.staves.element.ElementalGaugeManager.isEntityFrozen(target),
                hasPos, px, py, pz
        );
        if (hasPos && (!target.isAlive() || target.isRemoved()) && target.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(px, py, pz, 64.0D, sl.dimension())), packet);
        } else {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
        }
    }

    public static void sendStaffAnimation(Entity target, int animType) {
        if (target == null || target.level().isClientSide()) return;
        AuraSyncPacket packet = new AuraSyncPacket(
                target.getId(), java.util.Collections.emptyList(), null, null, null, (float) animType, -5, false
        );
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
    }

    public static void sendSilentHealthSync(Entity target, float newHealth) {
        if (target == null || target.level().isClientSide()) return;
        AuraSyncPacket packet = new AuraSyncPacket(
                target.getId(), java.util.Collections.emptyList(), null, null, null, newHealth, -4, false
        );
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
    }

    public static void sendPhysicalDamage(Entity target, float damage) {
        if (target == null || target.level().isClientSide() || damage <= 0.0F) return;
        double px = target.getX();
        double py = target.getY() + Math.max(0.65D, target.getBbHeight() * 0.65D);
        double pz = target.getZ();
        AuraSyncPacket packet = new AuraSyncPacket(
                target.getId(), java.util.Collections.emptyList(), null, null, null, damage, -3,
                false, true, px, py, pz
        );
        if ((!target.isAlive() || target.isRemoved()) && target.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(px, py, pz, 64.0D, sl.dimension())), packet);
        } else {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
        }
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements) {
        sendAuraSyncToPlayer(player, target, elements, 0);
    }

    public static void sendAuraSyncToPlayer(ServerPlayer player, Entity target, Collection<Element> elements, int durationTicks) {
        AuraSyncPacket packet = new AuraSyncPacket(target.getId(), elements, null, null, null, 0.0F, durationTicks, com.velorise.staves.element.ElementalGaugeManager.isEntityFrozen(target));
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
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static void sendConfigSyncToPlayer(ServerPlayer player) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConfigSyncPacket(com.velorise.staves.config.StavesConfig.createSnapshotFromLocal()));
    }

    public static void sendResonanceSyncToPlayer(ServerPlayer player, int mask, boolean overloaded, int staffCount) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ResonanceSyncPacket(mask, overloaded, staffCount));
    }

    public static void sendFreezeStruggle() {
        CHANNEL.sendToServer(new FreezeStrugglePacket());
    }

    public static void sendSelectOmniMode(int mode) {
        CHANNEL.sendToServer(new SelectOmniModePacket(mode));
    }

    public static void sendMavuikaSync(Entity target, int auraRemainingTicks, float nightsoulPoints, boolean netherite) {
        if (target == null || target.level().isClientSide()) return;
        int ultiCooldownTicks = com.velorise.staves.element.ElementalGaugeManager.getMavuikaUltiCooldownTicks(target);
        MavuikaSyncPacket packet = new MavuikaSyncPacket(target.getId(), auraRemainingTicks, nightsoulPoints, netherite, ultiCooldownTicks);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
    }

    public static void sendMavuikaAction(byte action) {
        CHANNEL.sendToServer(new MavuikaActionPacket(action));
    }

    public static void sendRaidenSync(Entity target, int eyeRemainingTicks, int musouRemainingTicks, int resolve, boolean netherite) {
        if (target == null || target.level().isClientSide()) return;
        RaidenSyncPacket packet = new RaidenSyncPacket(target.getId(), eyeRemainingTicks, musouRemainingTicks, resolve, netherite);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
    }
}
