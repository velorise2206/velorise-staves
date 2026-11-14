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
        registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC, ConfigSyncPayload::handle);
        registrar.playToClient(ResonanceSyncPayload.TYPE, ResonanceSyncPayload.STREAM_CODEC, ResonanceSyncPayload::handle);
        registrar.playToClient(ElectricArcPayload.TYPE, ElectricArcPayload.STREAM_CODEC, ElectricArcPayload::handle);
        registrar.playToClient(MavuikaSyncPayload.TYPE, MavuikaSyncPayload.STREAM_CODEC, MavuikaSyncPayload::handle);
        registrar.playToClient(RaidenSyncPayload.TYPE, RaidenSyncPayload.STREAM_CODEC, RaidenSyncPayload::handle);
        registrar.playToServer(FreezeStrugglePayload.TYPE, FreezeStrugglePayload.STREAM_CODEC, FreezeStrugglePayload::handle);
        registrar.playToServer(SelectOmniModePayload.TYPE, SelectOmniModePayload.STREAM_CODEC, SelectOmniModePayload::handle);
        registrar.playToServer(MavuikaActionPayload.TYPE, MavuikaActionPayload.STREAM_CODEC, MavuikaActionPayload::handle);
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
        boolean hasPos = (reaction != null && reaction != ElementalReaction.NONE) || damage > 0.0F;
        double px = target.getX();
        double py = target.getY() + Math.max(0.65D, target.getBbHeight() * 0.65D);
        double pz = target.getZ();
        AuraSyncPayload payload = new AuraSyncPayload(
                target.getId(), elements, reactedAura, trigger, reaction, damage, durationTicks,
                com.velorise.staves.element.ElementalGaugeManager.isEntityFrozen(target),
                hasPos, px, py, pz
        );
        if (hasPos && (!target.isAlive() || target.isRemoved()) && target.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            PacketDistributor.sendToPlayersNear(sl, null, px, py, pz, 64.0D, payload);
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
        }
    }

    public static void sendStaffAnimation(Entity target, int animType) {
        if (target == null || target.level().isClientSide()) return;
        AuraSyncPayload payload = new AuraSyncPayload(
                target.getId(), java.util.Collections.emptyList(), null, null, null, (float) animType, -5, false
        );
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
    }

    public static void sendSilentHealthSync(Entity target, float newHealth) {
        if (target == null || target.level().isClientSide()) return;
        AuraSyncPayload payload = new AuraSyncPayload(
                target.getId(), java.util.Collections.emptyList(), null, null, null, newHealth, -4, false
        );
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
    }

    public static void sendPhysicalDamage(Entity target, float damage) {
        if (target == null || target.level().isClientSide() || damage <= 0.0F) return;
        double px = target.getX();
        double py = target.getY() + Math.max(0.65D, target.getBbHeight() * 0.65D);
        double pz = target.getZ();
        AuraSyncPayload payload = new AuraSyncPayload(
                target.getId(), java.util.Collections.emptyList(), null, null, null, damage, -3,
                false, true, px, py, pz
        );
        if ((!target.isAlive() || target.isRemoved()) && target.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            PacketDistributor.sendToPlayersNear(sl, null, px, py, pz, 64.0D, payload);
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
        }
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
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }

    public static void sendConfigSyncToPlayer(ServerPlayer player) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new ConfigSyncPayload(com.velorise.staves.config.StavesConfig.createSnapshotFromLocal()));
    }

    public static void sendResonanceSyncToPlayer(ServerPlayer player, int mask, boolean overloaded, int staffCount) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new ResonanceSyncPayload(mask, overloaded, staffCount));
    }

    public static void sendFreezeStruggle() {
        PacketDistributor.sendToServer(new FreezeStrugglePayload());
    }

    public static void sendSelectOmniMode(int mode) {
        PacketDistributor.sendToServer(new SelectOmniModePayload((byte) com.velorise.staves.entity.OmniOrbEntity.clampMode(mode)));
    }

    public static void sendMavuikaSync(Entity target, int auraRemainingTicks, float nightsoulPoints, boolean netherite) {
        if (target == null || target.level().isClientSide()) return;
        int ultiCooldownTicks = com.velorise.staves.element.ElementalGaugeManager.getMavuikaUltiCooldownTicks(target);
        MavuikaSyncPayload payload = new MavuikaSyncPayload(target.getId(), auraRemainingTicks, nightsoulPoints, netherite, ultiCooldownTicks);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
    }

    public static void sendMavuikaAction(byte action) {
        PacketDistributor.sendToServer(new MavuikaActionPayload(action));
    }

    public static void sendRaidenSync(Entity target, int eyeRemainingTicks, int musouRemainingTicks, int resolve, boolean netherite) {
        if (target == null || target.level().isClientSide()) return;
        RaidenSyncPayload payload = new RaidenSyncPayload(target.getId(), eyeRemainingTicks, musouRemainingTicks, resolve, netherite);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, payload);
    }
}
