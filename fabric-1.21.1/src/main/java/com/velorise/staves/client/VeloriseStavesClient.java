package com.velorise.staves.client;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import com.velorise.staves.network.AuraSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.ArrayList;
import java.util.List;

public final class VeloriseStavesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AuraSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                List<Element> elements = new ArrayList<>();
                for (Element e : Element.values()) {
                    if ((payload.activeMask() & (1 << e.ordinal())) != 0) {
                        elements.add(e);
                    }
                }
                Element reactedAura = (payload.reactedAuraOrdinal() > 0 && payload.reactedAuraOrdinal() <= Element.values().length)
                        ? Element.values()[payload.reactedAuraOrdinal() - 1] : null;
                Element trigger = (payload.triggerElementOrdinal() > 0 && payload.triggerElementOrdinal() <= Element.values().length)
                        ? Element.values()[payload.triggerElementOrdinal() - 1] : null;
                ElementalReaction reaction = (payload.reactionOrdinal() > 0 && payload.reactionOrdinal() <= ElementalReaction.values().length)
                        ? ElementalReaction.values()[payload.reactionOrdinal() - 1] : null;

                boolean isFrozen = (payload.activeMask() & 0x80) != 0;
                ClientAuraTracker.update(payload.entityId(), elements, reactedAura, trigger, reaction, payload.damage(), payload.durationTicks(), isFrozen);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(com.velorise.staves.network.ElectricArcPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientAuraTracker.addArc(
                        payload.fromEntityId(),
                        new net.minecraft.world.phys.Vec3(payload.startX(), payload.startY(), payload.startZ()),
                        payload.toEntityId(),
                        new net.minecraft.world.phys.Vec3(payload.endX(), payload.endY(), payload.endZ()),
                        payload.durationTicks(),
                        payload.color()
                );
            });
        });

        // Register World Render for 3D Electric Arcs & Crystallize Shards
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            ClientAuraRenderer.renderActiveElectricArcs(context.matrixStack(), context.consumers(), context.camera());
            ClientAuraRenderer.renderCrystallizeShards(context.matrixStack(), context.consumers(), context.camera());
        });

        // Register In-Game HUD overlay
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            ClientAuraRenderer.renderPlayerAuraHud(guiGraphics);
            OmniWheelOverlay.render(guiGraphics);
        });

        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(OmniWheelOverlay.OMNI_WHEEL_KEY);

        ClientPlayNetworking.registerGlobalReceiver(com.velorise.staves.network.ConfigSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> com.velorise.staves.config.StavesConfig.applyServerSync(payload.data()));
        });

        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()
                    && entity instanceof net.minecraft.world.entity.LivingEntity living
                    && ClientAuraTracker.isFrozen(living.getId())) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientAuraRenderer.tickFrozenPlayer());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientAuraRenderer.tickFrozenPlayer();
            OmniWheelOverlay.tick(client);
        });

        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.velorise.staves.VeloriseStavesMod.GEO_FANG_PILLAR,
                GeoFangPillarRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.velorise.staves.VeloriseStavesMod.DENDRO_CORE_ENTITY,
                DendroCoreRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.velorise.staves.VeloriseStavesMod.OMNI_ORB,
                OmniOrbRenderer::new
        );

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientAuraTracker.clear();
            ClientAuraRenderer.resetFrozenClientState();
            OmniWheelOverlay.reset();
            com.velorise.staves.config.StavesConfig.clearServerSync();
        });
    }
}
