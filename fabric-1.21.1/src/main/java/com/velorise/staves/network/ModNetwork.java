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
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ResonanceSyncPayload.TYPE, ResonanceSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ElectricArcPayload.TYPE, ElectricArcPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(FreezeStrugglePayload.TYPE, FreezeStrugglePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FreezeStrugglePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> com.velorise.staves.element.ElementalGaugeManager.handlePlayerFreezeStruggle(context.player()));
        });
        PayloadTypeRegistry.playC2S().register(SelectOmniModePayload.TYPE, SelectOmniModePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SelectOmniModePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> SelectOmniModePayload.applySelection(context.player(), payload.mode()));
        });
        PayloadTypeRegistry.playS2C().register(MavuikaSyncPayload.TYPE, MavuikaSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RaidenSyncPayload.TYPE, RaidenSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MavuikaActionPayload.TYPE, MavuikaActionPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MavuikaActionPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> MavuikaActionPayload.handle(context.player(), payload.action()));
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
            for (ServerPlayer player : PlayerLookup.around(sl, new net.minecraft.world.phys.Vec3(px, py, pz), 64.0D)) {
                ServerPlayNetworking.send(player, payload);
            }
        } else {
            for (ServerPlayer player : PlayerLookup.tracking(target)) {
                ServerPlayNetworking.send(player, payload);
            }
            if (target instanceof ServerPlayer self) {
                ServerPlayNetworking.send(self, payload);
            }
        }
    }

    public static void sendStaffAnimation(Entity target, int animType) {
        if (target == null || target.level().isClientSide()) return;
        AuraSyncPayload payload = new AuraSyncPayload(
                target.getId(), java.util.Collections.emptyList(), null, null, null, (float) animType, -5, false
        );
        for (ServerPlayer player : PlayerLookup.tracking(target)) {
            ServerPlayNetworking.send(player, payload);
        }
        if (target instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, payload);
        }
    }

    public static void sendSilentHealthSync(Entity target, float newHealth) {
        if (target == null || target.level().isClientSide()) return;
        AuraSyncPayload payload = new AuraSyncPayload(
                target.getId(), java.util.Collections.emptyList(), null, null, null, newHealth, -4, false
        );
        for (ServerPlayer player : PlayerLookup.tracking(target)) {
            ServerPlayNetworking.send(player, payload);
        }
        if (target instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, payload);
        }
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
            for (ServerPlayer player : PlayerLookup.around(sl, new net.minecraft.world.phys.Vec3(px, py, pz), 64.0D)) {
                ServerPlayNetworking.send(player, payload);
            }
        } else {
            for (ServerPlayer player : PlayerLookup.tracking(target)) {
                ServerPlayNetworking.send(player, payload);
            }
            if (target instanceof ServerPlayer self) {
                ServerPlayNetworking.send(self, payload);
            }
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
        for (ServerPlayer tracker : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracker, payload);
        }
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendConfigSyncToPlayer(ServerPlayer player) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new ConfigSyncPayload(com.velorise.staves.config.StavesConfig.createSnapshotFromLocal()));
    }

    public static void sendResonanceSyncToPlayer(ServerPlayer player, int mask, boolean overloaded, int staffCount) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new ResonanceSyncPayload(mask, overloaded, staffCount));
    }

    public static void sendFreezeStruggle() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new FreezeStrugglePayload());
    }

    public static void sendSelectOmniMode(int mode) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new SelectOmniModePayload((byte) com.velorise.staves.entity.OmniOrbEntity.clampMode(mode))
        );
    }

    public static void sendMavuikaSync(ServerPlayer player, int auraRemainingTicks, float nightsoulPoints, boolean netherite) {
        if (player == null) return;
        int ultiCooldownTicks = com.velorise.staves.element.ElementalGaugeManager.getMavuikaUltiCooldownTicks(player);
        MavuikaSyncPayload payload = new MavuikaSyncPayload(player.getId(), auraRemainingTicks, nightsoulPoints, netherite, ultiCooldownTicks);
        for (ServerPlayer tracker : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracker, payload);
        }
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendMavuikaAction(byte action) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new MavuikaActionPayload(action));
    }

    public static void sendRaidenSync(ServerPlayer player, int eyeRemainingTicks, int musouRemainingTicks, int resolve, boolean netherite) {
        if (player == null) return;
        RaidenSyncPayload payload = new RaidenSyncPayload(player.getId(), eyeRemainingTicks, musouRemainingTicks, resolve, netherite);
        for (ServerPlayer tracker : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracker, payload);
        }
        ServerPlayNetworking.send(player, payload);
    }
}
