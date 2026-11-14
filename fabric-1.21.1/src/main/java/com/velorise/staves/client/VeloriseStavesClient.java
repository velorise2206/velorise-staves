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
            if (payload.durationTicks() == -4) {
                ClientAuraTracker.silentDrainUntil = System.currentTimeMillis() + 800L;
            }
            Runnable task = () -> {
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
                ClientAuraTracker.update(
                        payload.entityId(), elements, reactedAura, trigger, reaction, payload.damage(), payload.durationTicks(), isFrozen,
                        payload.hasPos(), payload.posX(), payload.posY(), payload.posZ()
                );
            };
            if (context.client().isSameThread()) {
                task.run();
            } else {
                context.client().execute(task);
            }
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

        // Register World Render for 3D Electric Arcs, Crystallize Shards & Floating Combat Text
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            ClientAuraRenderer.renderActiveElectricArcs(context.matrixStack(), context.consumers(), context.camera());
            ClientAuraRenderer.renderCrystallizeShards(context.matrixStack(), context.consumers(), context.camera());
            ClientAuraRenderer.renderFloatingCombatTexts(context.matrixStack(), context.consumers(), context.camera());
        });

        // Register In-Game HUD overlay
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            ClientAuraRenderer.renderPlayerAuraHud(guiGraphics);
            OmniWheelOverlay.render(guiGraphics);
        });

        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(OmniWheelOverlay.OMNI_WHEEL_KEY);
        net.minecraft.client.KeyMapping mavuikaUltiKey = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.KeyMapping(
                        "key.velorise_staves.mavuika_ulti",
                        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                        org.lwjgl.glfw.GLFW.GLFW_KEY_R,
                        "key.categories.velorise_staves"
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(com.velorise.staves.network.MavuikaSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientAuraTracker.updateMavuika(
                    payload.entityId(), payload.auraRemainingTicks(), payload.nightsoulPoints(), payload.netherite(), payload.ultiCooldownTicks()
            ));
        });
        ClientPlayNetworking.registerGlobalReceiver(com.velorise.staves.network.RaidenSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientAuraTracker.updateRaiden(
                    payload.entityId(), payload.eyeRemainingTicks(), payload.musouRemainingTicks(), payload.resolve(), payload.netherite()
            ));
        });

        ClientPlayNetworking.registerGlobalReceiver(com.velorise.staves.network.ConfigSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> com.velorise.staves.config.StavesConfig.applyServerSync(payload.data()));
        });

        ClientPlayNetworking.registerGlobalReceiver(com.velorise.staves.network.ResonanceSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                com.velorise.staves.element.ElementalResonanceManager.setClientResonanceState(payload.resonanceMask(), payload.overloaded(), payload.staffCount());
                ClientAuraTracker.updateLocalResonance(payload.resonanceMask(), payload.overloaded(), payload.staffCount());
            });
        });

        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            com.velorise.staves.item.SwordImbueHelper.appendImbueTooltip(stack, lines);
        });

        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()
                    && entity instanceof net.minecraft.world.entity.LivingEntity living
                    && ClientAuraTracker.isFrozen(living.getId())) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        final boolean[] bikeDismountWaitAttackRelease = {false};
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity || bikeDismountWaitAttackRelease[0]) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity || bikeDismountWaitAttackRelease[0]) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        final int[] queuedFastAttackClicks = {0};
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ClientAuraRenderer.tickFrozenPlayer();
            if (client.player != null && client.screen == null
                    && client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) {
                while (client.options.keyAttack.consumeClick()) {
                    queuedFastAttackClicks[0]++;
                }
            }
        });
        final boolean[] wasBikeDashDown = {false};
        final boolean[] bikeSprintActive = {false};
        final boolean[] bikeMountWaitRelease = {false};
        final boolean[] wasRidingBike = {false};
        final int[] bikeAttackHoldTicks = {0};
        final boolean[] bikeSpinActive = {false};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientAuraRenderer.tickFrozenPlayer();
            OmniWheelOverlay.tick(client);
            if (client.player == null || client.screen != null) {
                wasBikeDashDown[0] = false;
                bikeSprintActive[0] = false;
                bikeMountWaitRelease[0] = false;
                wasRidingBike[0] = false;
                bikeAttackHoldTicks[0] = 0;
                bikeSpinActive[0] = false;
                queuedFastAttackClicks[0] = 0;
                return;
            }
            while (mavuikaUltiKey.consumeClick()) {
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_ULTI);
            }
            if (client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
                if (!wasRidingBike[0]) {
                    wasRidingBike[0] = true;
                    bikeMountWaitRelease[0] = client.options.keyUse.isDown();
                    bikeDismountWaitAttackRelease[0] = false;
                    queuedFastAttackClicks[0] = 0;
                }
                if (bikeMountWaitRelease[0] && !client.options.keyUse.isDown()) {
                    bikeMountWaitRelease[0] = false;
                }
                int attackClicks = queuedFastAttackClicks[0];
                queuedFastAttackClicks[0] = 0;
                while (client.options.keyAttack.consumeClick()) {
                    // Suppress melee swing/block-breaking while riding bike, but count fast taps
                    attackClicks++;
                }
                while (client.options.keyUse.consumeClick()) {
                    // Suppress item right-click re-triggers while riding bike
                }
                if (bike.getUltiTicks() > 8 || (bike.getNaStep() == 6 && bike.getNaTicks() > 4)) {
                    bikeAttackHoldTicks[0] = 0;
                    bikeSpinActive[0] = false;
                } else {
                    boolean attackDown = client.options.keyAttack.isDown();
                    if (attackDown) {
                        bikeAttackHoldTicks[0]++;
                        if (!bikeSpinActive[0] && bikeAttackHoldTicks[0] >= 5) {
                            bikeSpinActive[0] = true;
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_START);
                        }
                    } else if (bikeAttackHoldTicks[0] > 0) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(bikeSpinActive[0]
                                ? com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_STOP
                                : com.velorise.staves.network.MavuikaActionPayload.ACTION_BIKE_NA);
                        bikeAttackHoldTicks[0] = 0;
                        bikeSpinActive[0] = false;
                    } else if (attackClicks > 0) {
                        // Ultra-fast tap pressed and released within the same client tick
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_BIKE_NA);
                    }
                }
                boolean useHeld = !bikeMountWaitRelease[0] && client.options.keyUse.isDown();
                boolean sprintDown = client.options.keySprint.isDown() || useHeld;
                boolean dashTriggered = sprintDown && !wasBikeDashDown[0] && !bike.isOnDashCooldown();
                wasBikeDashDown[0] = sprintDown;
                if (dashTriggered) {
                    bike.triggerClientDash(client.player);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_DASH_CANCEL);
                    bikeAttackHoldTicks[0] = 0;
                    bikeSpinActive[0] = false;
                }
                boolean wantSprint = sprintDown && bike.tickCount >= 10;
                if (wantSprint != bikeSprintActive[0]) {
                    bikeSprintActive[0] = wantSprint;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(wantSprint
                            ? com.velorise.staves.network.MavuikaActionPayload.ACTION_SPRINT_START
                            : com.velorise.staves.network.MavuikaActionPayload.ACTION_SPRINT_STOP);
                }
            } else {
                if (wasRidingBike[0] && client.options.keyAttack.isDown()) {
                    bikeDismountWaitAttackRelease[0] = true;
                }
                wasRidingBike[0] = false;
                bikeMountWaitRelease[0] = false;
                wasBikeDashDown[0] = false;
                bikeSprintActive[0] = false;
                bikeAttackHoldTicks[0] = 0;
                bikeSpinActive[0] = false;
                if (bikeDismountWaitAttackRelease[0]) {
                    if (!client.options.keyAttack.isDown()) {
                        bikeDismountWaitAttackRelease[0] = false;
                    } else {
                        while (client.options.keyAttack.consumeClick()) {
                        }
                    }
                }
            }
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
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.velorise.staves.VeloriseStavesMod.HYDRO_SALON_SUMMON,
                HydroSalonSummonRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.velorise.staves.VeloriseStavesMod.FLAMESTRIDER_BIKE,
                FlamestriderBikeRenderer::new
        );

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientAuraTracker.clear();
            com.velorise.staves.element.ElementalResonanceManager.clearClientState();
            ClientAuraRenderer.resetFrozenClientState();
            OmniWheelOverlay.reset();
            com.velorise.staves.config.StavesConfig.clearServerSync();
        });
    }
}
