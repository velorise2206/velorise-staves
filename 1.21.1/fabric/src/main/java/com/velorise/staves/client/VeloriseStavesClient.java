package com.velorise.staves.client;

import com.velorise.staves.element.Element;
import com.velorise.staves.element.ElementalReaction;
import com.velorise.staves.network.AuraSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.ArrayList;
import java.util.List;

public final class VeloriseStavesClient implements ClientModInitializer {
    private static long lastDaybreakClientShotMs = 0L;
    private static int queuedNahidaFastClicks = 0;
    private static int queuedTsaritsaFastClicks = 0;
    private static int queuedDaybreakFastClicks = 0;
    private static int queuedDragonfireFastClicks = 0;
    private static long lastDragonfireClientShotMs = 0L;
    private static int furinaCaHoldTicks = 0;
    private static boolean furinaCaHoldTriggered = false;

    public static float getFurinaCaHoldProgress() {
        if (furinaCaHoldTicks <= 0 || furinaCaHoldTriggered) return 0.0F;
        return net.minecraft.util.Mth.clamp(furinaCaHoldTicks / 10.0F, 0.0F, 1.0F);
    }

    public static void recordFastAttackClick(com.velorise.staves.item.ArcaneStaffItem.Kind effKind) {
        if (effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK) {
            queuedDaybreakFastClicks++;
        } else if (effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) {
            queuedNahidaFastClicks++;
        } else if (effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_BOW
                || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_SPEAR) {
            queuedTsaritsaFastClicks++;
        } else if (effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DRAGONFIRE
                || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.SHULKER) {
            queuedDragonfireFastClicks++;
        }
    }
    @Override
    public void onInitializeClient() {
        net.minecraft.client.gui.screens.MenuScreens.register(
                com.velorise.staves.VeloriseStavesMod.SHULKER_STAFF_MENU,
                ShulkerStaffScreen::new
        );
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
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(OmniWheelOverlay.OPEN_CONFIG_KEY);
        net.minecraft.client.KeyMapping elementalBurstKey = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.KeyMapping(
                        "key.velorise_staves.elemental_burst",
                        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                        org.lwjgl.glfw.GLFW.GLFW_KEY_R,
                        "key.categories.velorise_staves"
                )
        );
        net.minecraft.client.KeyMapping plungeAttackKey = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.KeyMapping(
                        "key.velorise_staves.plunge_attack",
                        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                        org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
                        "key.categories.velorise_staves"
                )
        );
        net.minecraft.client.KeyMapping windGliderKey = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.KeyMapping(
                        "key.velorise_staves.wind_glider",
                        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                        org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
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
                    payload.entityId(), payload.eyeRemainingTicks(), payload.musouRemainingTicks(), payload.resolve(), payload.netherite(), payload.burstCooldownTicks()
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
            com.velorise.staves.item.ArcaneStaffItem.Kind effKind = com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(player.getMainHandItem());
            if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity || bikeDismountWaitAttackRelease[0]
                    || com.velorise.staves.element.NeuvilletteManager.isHoldingGuardianLens(player)
                    || com.velorise.staves.element.PlungeAndGliderManager.isPlunging(player)
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_BOW
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_SPEAR
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DRAGONFIRE
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.SHULKER) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            com.velorise.staves.item.ArcaneStaffItem.Kind effKind = com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(player.getMainHandItem());
            if (player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity || bikeDismountWaitAttackRelease[0]
                    || com.velorise.staves.element.NeuvilletteManager.isHoldingGuardianLens(player)
                    || com.velorise.staves.element.PlungeAndGliderManager.isPlunging(player)
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_BOW
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_SPEAR
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DRAGONFIRE
                    || effKind == com.velorise.staves.item.ArcaneStaffItem.Kind.SHULKER) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        final int[] queuedFastAttackClicks = {0};
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ClientAuraRenderer.tickFrozenPlayer();
            ClientAuraTracker.prepareLocalWallClimbPreTick();
            if (client.player != null && client.screen == null
                    && client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) {
                while (client.options.keyAttack.consumeClick()) {
                    // Suppress vanilla block-breaking; raw physical presses are captured by GenshinInputState
                }
            }
        });
        final boolean[] wasBikeDashDown = {false};
        final com.velorise.staves.fsm.MavuikaInputBuffer bikeInputBuffer = new com.velorise.staves.fsm.MavuikaInputBuffer();
        final boolean[] bikeSprintActive = {false};
        final boolean[] bikeMountWaitRelease = {false};
        final boolean[] wasRidingBike = {false};
        final int[] bikeAttackHoldTicks = {0};
        final boolean[] bikeSpinActive = {false};
        final int[] bikeSpinReleaseWindowTicks = {0};
        final boolean[] bikeHeldThroughDash = {false};
        final boolean[] bikeAutoSpinLatched = {false};
        final int[] bikeSpinTapCount = {0};
        final int[] bikeDoubleTapWindowTicks = {0};
        final int[] raidenAttackHoldTicks = {0};
        final boolean[] ventiUltiFiredThisHold = {false};
        final boolean[] neuvilletteLeftHoldSent = {false};
        final boolean[] wasAirJumpDown = {false};
        final boolean[] wasAirUseDown = {false};
        final boolean[] wasAirAttackDown = {false};
        final boolean[] wasOnGround = {true};
        final boolean[] wasCreativeFlying = {false};
        final boolean[] wasXilonenSkateActive = {false};
        final boolean[] wasXilonenDashKeyDown = {false};
        final int[] furinaAttackHoldTicks = {0};
        final boolean[] furinaCaTriggeredThisHold = {false};
        final boolean[] raidenCaTriggeredThisHold = {false};
        final int[] nahidaAttackHoldTicks = {0};
        final boolean[] nahidaCaTriggeredThisHold = {false};
        final int[] tsaritsaSpearHoldTick = {0};
        final int[] lastSelectedHotbarSlot = {-1};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientAuraTracker.syncIdleLockedYaw(client);
            ClientAuraRenderer.tickFrozenPlayer();
            OmniWheelOverlay.tick(client);
            if (client.player != null) {
                if (client.level != null && client.player.tickCount % 100 == 0) {
                    ClientAuraTracker.pruneStale(client.level);
                    com.velorise.staves.client.reaction.ElementalReactionBadgeRenderer.pruneStaleFrozenStates(client.level);
                }
                com.velorise.staves.item.ArcaneStaffItem.tickCraftingMenuOmniPreview(client.player);
                int curSlot = client.player.getInventory().selected;
                if (lastSelectedHotbarSlot[0] != -1 && curSlot != lastSelectedHotbarSlot[0]) {
                    if (ClientAuraTracker.isSlotSwitchLocked(client.player.getId())) {
                        client.player.getInventory().selected = lastSelectedHotbarSlot[0];
                        curSlot = lastSelectedHotbarSlot[0];
                    } else {
                        net.minecraft.world.item.ItemStack main = client.player.getMainHandItem();
                        ClientAuraTracker.interruptActiveAnimations(client.player.getId());
                        if (main.getItem() instanceof com.velorise.staves.item.ArcaneStaffItem) {
                            com.velorise.staves.element.ElementalResonanceManager.notifyClientSlotSwitchedToStaff(client.player, main);
                        }
                    }
                }
                lastSelectedHotbarSlot[0] = curSlot;
                if (ClientAuraTracker.isSlotSwitchLocked(client.player.getId())) {
                    while (client.options.keySwapOffhand.consumeClick()) {
                    }
                    while (client.options.keyDrop.consumeClick()) {
                    }
                }
            } else {
                lastSelectedHotbarSlot[0] = -1;
            }
            if (client.player != null) {
                if (ClientAuraTracker.isGliding(client.player.getId())
                        && !client.player.onGround() && !client.player.isInWater() && !client.player.isInLava()
                        && !client.player.getAbilities().flying && client.player.getVehicle() == null) {
                    boolean hasVenti = com.velorise.staves.element.VentiManager.hasVentiWeapon(client.player);
                    boolean hasDragonGlider = com.velorise.staves.element.DragonfireCloudManager.hasDragonfireWeapon(client.player);
                    double speedMult = (hasVenti ? 2.0D : 1.0D) * (hasDragonGlider ? 1.5D : 1.0D);
                    double maxFallSpeed = (hasVenti && hasDragonGlider) ? -0.04D : (hasVenti ? -0.055D : (hasDragonGlider ? -0.08D : -0.11D));
                    double maxSpeed = 0.42D * speedMult;
                    double accel = 0.055D * speedMult;

                    net.minecraft.world.phys.Vec3 vel = client.player.getDeltaMovement();
                    double dy = vel.y < maxFallSpeed ? Math.max(maxFallSpeed, vel.y * 0.65D) : vel.y;
                    net.minecraft.world.phys.Vec3 look = client.player.getLookAngle();
                    net.minecraft.world.phys.Vec3 horiz = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z);
                    double vx = vel.x;
                    double vz = vel.z;
                    if (horiz.lengthSqr() > 1.0E-4D && (client.player.zza > 0.01F || client.screen != null || Math.hypot(vx, vz) > 0.08D)) {
                        horiz = horiz.normalize();
                        vx = vx * 0.86D + horiz.x * accel;
                        vz = vz * 0.86D + horiz.z * accel;
                        double hs = Math.hypot(vx, vz);
                        if (hs > maxSpeed) {
                            vx = vx / hs * maxSpeed;
                            vz = vz / hs * maxSpeed;
                        }
                    }
                    client.player.setDeltaMovement(vx, dy, vz);
                    client.player.fallDistance = 0.0F;
                }
                if (client.screen != null) {
                    ClientAuraTracker.tickLocalStaminaAndDash(
                            client.player,
                            false,
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_TOGGLE_GLIDER,
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_PLAYER_DASH
                    );
                }
            }
            if (client.player == null || client.screen != null) {
                wasBikeDashDown[0] = false;
                bikeSprintActive[0] = false;
                bikeMountWaitRelease[0] = false;
                wasRidingBike[0] = false;
                bikeAttackHoldTicks[0] = 0;
                bikeSpinActive[0] = false;
                bikeSpinReleaseWindowTicks[0] = 0;
                bikeHeldThroughDash[0] = false;
                bikeAutoSpinLatched[0] = false;
                bikeSpinTapCount[0] = 0;
                bikeDoubleTapWindowTicks[0] = 0;
                bikeInputBuffer.reset();
                com.velorise.staves.client.GenshinInputState.reset();
                queuedFastAttackClicks[0] = 0;
                raidenAttackHoldTicks[0] = 0;
                ClientAuraTracker.ventiUltiHoldTicks = 0;
                ventiUltiFiredThisHold[0] = false;
                furinaAttackHoldTicks[0] = 0;
                furinaCaTriggeredThisHold[0] = false;
                furinaCaHoldTicks = 0;
                furinaCaHoldTriggered = false;
                raidenCaTriggeredThisHold[0] = false;
                nahidaAttackHoldTicks[0] = 0;
                nahidaCaTriggeredThisHold[0] = false;
                tsaritsaSpearHoldTick[0] = 0;
                queuedNahidaFastClicks = 0;
                queuedTsaritsaFastClicks = 0;
                queuedDaybreakFastClicks = 0;
                queuedDragonfireFastClicks = 0;
                ClientAuraTracker.localTsaritsaBowPullTicks = 0;
                ClientAuraTracker.localDaybreakBowPullTicks = 0;
                ClientAuraTracker.localTsaritsaHoldETicks = 0;
                ClientAuraTracker.localTsaritsaHoldELocked = false;
                return;
            }
            if (ClientAuraTracker.isPlunging(client.player.getId()) || com.velorise.staves.element.PlungeAndGliderManager.isPlunging(client.player)) {
                client.player.swinging = false;
                client.player.swingTime = 0;
                client.player.attackAnim = 0.0F;
                client.player.oAttackAnim = 0.0F;
            }
            if (client.player != null && ClientAuraTracker.isUltiCastActive(client.player.getId())) {
                client.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                client.player.setSprinting(false);
                if (client.player.input != null) {
                    client.player.input.forwardImpulse = 0.0F;
                    client.player.input.leftImpulse = 0.0F;
                    client.player.input.up = false;
                    client.player.input.down = false;
                    client.player.input.left = false;
                    client.player.input.right = false;
                    client.player.input.jumping = false;
                    client.player.input.shiftKeyDown = false;
                }
                while (client.options.keyAttack.consumeClick()) {}
                while (client.options.keyUse.consumeClick()) {}
                while (client.options.keyPickItem.consumeClick()) {}
                while (client.options.keyJump.consumeClick()) {}
                while (client.options.keySprint.consumeClick()) {}
                while (client.options.keyShift.consumeClick()) {}
                while (client.options.keyUp.consumeClick()) {}
                while (client.options.keyDown.consumeClick()) {}
                while (client.options.keyLeft.consumeClick()) {}
                while (client.options.keyRight.consumeClick()) {}
                while (elementalBurstKey.consumeClick()) {}
                return;
            }
            ClientAuraTracker.tickLocalStaminaAndDash(
                    client.player,
                    client.options.keySprint.isDown(),
                    com.velorise.staves.network.MavuikaActionPayload.ACTION_TOGGLE_GLIDER,
                    com.velorise.staves.network.MavuikaActionPayload.ACTION_PLAYER_DASH
            );
            while (OmniWheelOverlay.OPEN_CONFIG_KEY.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new VeloriseConfigScreen(null));
                }
            }
            while (elementalBurstKey.consumeClick()) {
                if (client.player != null) {
                    if (!com.velorise.staves.item.ArcaneStaffItem.isGenshinCharacterWeapon(client.player.getMainHandItem())) {
                        continue;
                    }
                    boolean grounded = com.velorise.staves.element.ArchonUltiManager.isPlayerGroundedForBurst(client.player);
                    if (!grounded) {
                        continue;
                    }
                }
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_ULTI);
            }
            com.velorise.staves.client.character.RaidenVisualRenderer.tickClientAttackHold(client);
            if (ClientAuraTracker.isRaidenMusouActive(client.player.getId())
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(client.player.getMainHandItem()) == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
                if (client.player.swinging && client.player.swingTime == 0 && !ClientAuraTracker.localRaidenAttackHeld
                        && ClientAuraTracker.tryConsumeLocalRaidenSwing(false)) {
                    ClientAuraTracker.triggerStaffAnimation(client.player.getId(), ClientAuraTracker.ANIM_RAIDEN_SLASH);
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new com.velorise.staves.network.RaidenSlashPayload(false));
                }
            }

            boolean holdingGuardian = com.velorise.staves.element.NeuvilletteManager.isHoldingGuardianLens(client.player);
            if (holdingGuardian && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
                if (client.options.keyAttack.isDown()) {
                    while (client.options.keyAttack.consumeClick()) {
                        // Suppress melee swing / block breaking while channeling Equitable Judgment
                    }
                    if (!neuvilletteLeftHoldSent[0]) {
                        neuvilletteLeftHoldSent[0] = true;
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_NEUVILLETTE_CA_START);
                    }
                } else if (neuvilletteLeftHoldSent[0]) {
                    neuvilletteLeftHoldSent[0] = false;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_NEUVILLETTE_CA_STOP);
                }
            } else if (neuvilletteLeftHoldSent[0]) {
                neuvilletteLeftHoldSent[0] = false;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                        com.velorise.staves.network.MavuikaActionPayload.ACTION_NEUVILLETTE_CA_STOP);
            }

            boolean holdingFurina = com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(client.player.getMainHandItem()) == com.velorise.staves.item.ArcaneStaffItem.Kind.SPLENDOR;
            if (holdingFurina && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) && client.player.onGround()) {
                if (client.options.keyAttack.isDown()) {
                    furinaAttackHoldTicks[0]++;
                    furinaCaHoldTicks = furinaAttackHoldTicks[0];
                    if (furinaAttackHoldTicks[0] >= 10 && !furinaCaTriggeredThisHold[0]) {
                        furinaCaTriggeredThisHold[0] = true;
                        furinaCaHoldTriggered = true;
                    ClientAuraTracker.localFurinaCaHoldProgress = 0.0F;
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_FURINA_CA_SWITCH);
                    }
                } else {
                    furinaAttackHoldTicks[0] = 0;
                    furinaCaTriggeredThisHold[0] = false;
                    furinaCaHoldTicks = 0;
                    furinaCaHoldTriggered = false;
                }
            } else {
                furinaAttackHoldTicks[0] = 0;
                furinaCaTriggeredThisHold[0] = false;
                furinaCaHoldTicks = 0;
                furinaCaHoldTriggered = false;
            }

            // Raiden Musou Hold Left-Click Charged Attack: 5-block dash + 5 trailing slashes
            boolean holdingRaidenMusou = ClientAuraTracker.isRaidenMusouActive(client.player.getId())
                    && com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(client.player.getMainHandItem()) == com.velorise.staves.item.ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
            if (holdingRaidenMusou && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
                if (client.options.keyAttack.isDown()) {
                    raidenAttackHoldTicks[0]++;
                    if (raidenAttackHoldTicks[0] >= 8 && !raidenCaTriggeredThisHold[0]
                            && ClientAuraTracker.tryConsumeLocalRaidenSwing(true)) {
                        raidenCaTriggeredThisHold[0] = true;
                        ClientAuraTracker.localRaidenAttackHeld = true;
                        ClientAuraTracker.triggerStaffAnimation(client.player.getId(), ClientAuraTracker.ANIM_RAIDEN_CA);
                        com.velorise.staves.network.ModNetwork.sendRaidenSlash(true);
                    }
                } else {
                    raidenAttackHoldTicks[0] = 0;
                    raidenCaTriggeredThisHold[0] = false;
                    ClientAuraTracker.localRaidenAttackHeld = false;
                }
            } else {
                raidenAttackHoldTicks[0] = 0;
                raidenCaTriggeredThisHold[0] = false;
                ClientAuraTracker.localRaidenAttackHeld = false;
            }

            if (!client.options.keyUse.isDown()) {
                ClientAuraTracker.localTsaritsaHoldETicks = 0;
                ClientAuraTracker.localTsaritsaHoldELocked = false;
            }

            com.velorise.staves.item.ArcaneStaffItem.Kind mainEffKind =
                    com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(client.player.getMainHandItem());
            boolean holdingNahida = (mainEffKind == com.velorise.staves.item.ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS);
            int nahidaFast = queuedNahidaFastClicks;
            queuedNahidaFastClicks = 0;
            if (holdingNahida && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                    && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(client.player)) {
                while (client.options.keyAttack.consumeClick()) {
                    nahidaFast++;
                }
                if (client.options.keyAttack.isDown()) {
                    nahidaAttackHoldTicks[0]++;
                    if (nahidaAttackHoldTicks[0] >= 10 && !nahidaCaTriggeredThisHold[0]) {
                        nahidaCaTriggeredThisHold[0] = true;
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_NAHIDA_CA);
                    }
                } else if (nahidaAttackHoldTicks[0] > 0) {
                    if (!nahidaCaTriggeredThisHold[0]) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_NAHIDA_NA);
                    }
                    nahidaAttackHoldTicks[0] = 0;
                    nahidaCaTriggeredThisHold[0] = false;
                } else if (nahidaFast > 0) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_NAHIDA_NA);
                }
            } else {
                nahidaAttackHoldTicks[0] = 0;
                nahidaCaTriggeredThisHold[0] = false;
            }

            boolean holdingTsaritsa = (mainEffKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_BOW
                    || mainEffKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_SPEAR);
            int tsaritsaFast = queuedTsaritsaFastClicks;
            queuedTsaritsaFastClicks = 0;
            if (holdingTsaritsa && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                    && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(client.player)) {
                while (client.options.keyAttack.consumeClick()) {
                    tsaritsaFast++;
                }
                boolean spearStance = (mainEffKind == com.velorise.staves.item.ArcaneStaffItem.Kind.TSARITSA_SPEAR)
                        || ClientAuraTracker.isTsaritsaSpearStance(client.player.getId());
                if (spearStance) {
                    ClientAuraTracker.localTsaritsaBowPullTicks = 0;
                    if (client.options.keyAttack.isDown()) {
                        if (tsaritsaSpearHoldTick[0] % 7 == 0) {
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                    com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                        }
                        tsaritsaSpearHoldTick[0]++;
                    } else {
                        if (tsaritsaFast > 0 && tsaritsaSpearHoldTick[0] == 0) {
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                    com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                        }
                        tsaritsaSpearHoldTick[0] = 0;
                    }
                } else {
                    tsaritsaSpearHoldTick[0] = 0;
                    boolean leftPhysDown = client.options.keyAttack.isDown()
                            || org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                    if (leftPhysDown) {
                        if (client.gameMode != null) {
                            client.gameMode.stopDestroyBlock();
                        }
                        client.player.swinging = false;
                        client.player.swingTime = 0;
                        ClientAuraTracker.localTsaritsaBowPullTicks = Math.min(60, ClientAuraTracker.localTsaritsaBowPullTicks + 1);
                    } else {
                        int pulled = ClientAuraTracker.localTsaritsaBowPullTicks;
                        ClientAuraTracker.localTsaritsaBowPullTicks = 0;
                        if (pulled >= 16) {
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                    com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_CA_FULL);
                        } else if (pulled >= 6) {
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                    com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_CA);
                        } else if (pulled > 0 || tsaritsaFast > 0) {
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                    com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                        }
                    }
                }
            } else {
                ClientAuraTracker.localTsaritsaBowPullTicks = 0;
                tsaritsaSpearHoldTick[0] = 0;
                if (mainEffKind != null && ClientAuraTracker.isTsaritsaDpsSeal(client.player.getId())
                        && client.player.swinging && client.player.swingTime == 0) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                }
            }

            boolean holdingDaybreak = (mainEffKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK);
            int daybreakFast = queuedDaybreakFastClicks;
            queuedDaybreakFastClicks = 0;
            if (holdingDaybreak && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                    && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(client.player)) {
                client.missTime = 0;
                while (client.options.keyAttack.consumeClick()) {
                    daybreakFast++;
                }
                boolean leftPhysDown = client.options.keyAttack.isDown()
                        || org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                if (leftPhysDown) {
                    if (client.gameMode != null) {
                        client.gameMode.stopDestroyBlock();
                    }
                    client.player.swinging = false;
                    client.player.swingTime = 0;
                    ClientAuraTracker.localDaybreakBowPullTicks = Math.min(60, ClientAuraTracker.localDaybreakBowPullTicks + 1);
                    ClientAuraTracker.ventiUltiHoldTicks = ClientAuraTracker.localDaybreakBowPullTicks;
                } else {
                    int pulled = ClientAuraTracker.localDaybreakBowPullTicks;
                    ClientAuraTracker.localDaybreakBowPullTicks = 0;
                    ClientAuraTracker.ventiUltiHoldTicks = 0;
                    long nowMs = System.currentTimeMillis();
                    if (pulled >= 16) {
                        lastDaybreakClientShotMs = nowMs;
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_DAYBREAK_CA_FULL);
                    } else if (pulled >= 6) {
                        lastDaybreakClientShotMs = nowMs;
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_DAYBREAK_CA);
                    } else if ((pulled > 0 || daybreakFast > 0) && nowMs - lastDaybreakClientShotMs >= 150L) {
                        lastDaybreakClientShotMs = nowMs;
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_DAYBREAK_NA);
                    }
                }
            } else {
                ClientAuraTracker.localDaybreakBowPullTicks = 0;
                ClientAuraTracker.ventiUltiHoldTicks = 0;
            }

            boolean holdingDragonfire = (mainEffKind == com.velorise.staves.item.ArcaneStaffItem.Kind.DRAGONFIRE
                    || mainEffKind == com.velorise.staves.item.ArcaneStaffItem.Kind.SHULKER);
            int dragonfireFast = queuedDragonfireFastClicks;
            queuedDragonfireFastClicks = 0;
            if (holdingDragonfire && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                    && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(client.player)) {
                client.missTime = 0;
                while (client.options.keyAttack.consumeClick()) {
                    dragonfireFast++;
                }
                long nowMs = System.currentTimeMillis();
                if (dragonfireFast > 0 && nowMs - lastDragonfireClientShotMs >= 200L) {
                    lastDragonfireClientShotMs = nowMs;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_DRAGONFIRE_NA);
                }
            }

            if (ClientAuraTracker.isXilonenSkating(client.player.getId()) && !(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
                if (!wasXilonenSkateActive[0]) {
                    wasXilonenSkateActive[0] = true;
                    wasXilonenDashKeyDown[0] = client.options.keySprint.isDown() || client.options.keyUse.isDown();
                } else {
                    boolean dashKeyDown = client.options.keySprint.isDown() || (client.options.keyUse.isDown() && !client.player.isShiftKeyDown());
                    if (dashKeyDown && !wasXilonenDashKeyDown[0]
                            && (client.player.onGround() || ClientAuraTracker.isXilonenWallClimbing(client.player.getId()))) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_XILONEN_DASH);
                    }
                    wasXilonenDashKeyDown[0] = dashKeyDown;
                }
            } else {
                wasXilonenSkateActive[0] = false;
                wasXilonenDashKeyDown[0] = false;
            }

            if (!(client.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
                boolean jumpDown = client.options.keyJump.isDown();
                if (jumpDown && !wasAirJumpDown[0]) {
                    if (ClientAuraTracker.isXilonenWallClimbing(client.player.getId())) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_XILONEN_WALL_LEAP);
                    } else if (ClientAuraTracker.isXilonenSkating(client.player.getId()) && (client.player.onGround() || wasOnGround[0])) {
                        net.minecraft.world.phys.Vec3 v = client.player.getDeltaMovement();
                        client.player.setDeltaMovement(v.x, Math.max(v.y, 0.58D), v.z);
                        ClientAuraTracker.triggerStaffAnimation(client.player.getId(), ClientAuraTracker.ANIM_XILONEN_JUMP_FLIP);
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                                com.velorise.staves.network.MavuikaActionPayload.ACTION_XILONEN_JUMP);
                    }
                }
                wasAirJumpDown[0] = jumpDown;
                net.minecraft.client.KeyMapping activeGliderKey = windGliderKey.isUnbound() ? client.options.keyUse : windGliderKey;
                boolean useDown = activeGliderKey.isDown();
                if (useDown && !wasAirUseDown[0]
                        && (ClientAuraTracker.localWindGliderUnlocked || com.velorise.staves.element.PlungeAndGliderManager.isWindGliderUnlocked(client.player))
                        && !client.player.onGround() && !wasOnGround[0]
                        && !client.player.isInWater() && !client.player.isInLava()
                        && !client.player.onClimbable() && client.player.getVehicle() == null
                        && !ClientAuraTracker.isPlunging(client.player.getId())
                        && (client.hitResult == null || client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
                        && com.velorise.staves.element.PlungeAndGliderManager.canRightClickToggleGlider(client.player)) {
                    ClientAuraTracker.localWindGliderUnlocked = true;
                    while (activeGliderKey.consumeClick()) {
                    }
                    if (activeGliderKey != client.options.keyUse && activeGliderKey.same(client.options.keyUse)) {
                        while (client.options.keyUse.consumeClick()) {
                        }
                    }
                    if (client.player.getAbilities().flying) {
                        client.player.getAbilities().flying = false;
                        client.player.onUpdateAbilities();
                    }
                    if (client.player.isFallFlying()) {
                        client.player.stopFallFlying();
                    }
                    boolean nextGlide = !ClientAuraTracker.isGliding(client.player.getId());
                    ClientAuraTracker.triggerStaffAnimation(client.player.getId(),
                            nextGlide ? ClientAuraTracker.ANIM_GLIDER_START : ClientAuraTracker.ANIM_GLIDER_STOP);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_TOGGLE_GLIDER);
                }
                wasAirUseDown[0] = useDown;
                wasOnGround[0] = client.player.onGround();
                if (client.player.onGround()) {
                    wasCreativeFlying[0] = false;
                } else if (client.player.getAbilities().flying && !ClientAuraTracker.isGliding(client.player.getId())) {
                    wasCreativeFlying[0] = true;
                }

                net.minecraft.client.KeyMapping activePlungeKey = plungeAttackKey.isUnbound() ? client.options.keyAttack : plungeAttackKey;
                boolean atkDown = activePlungeKey.isDown();
                if (atkDown && !wasAirAttackDown[0] && !holdingGuardian
                        && com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(client.player)) {
                    while (activePlungeKey.consumeClick()) {
                    }
                    if (activePlungeKey != client.options.keyAttack && activePlungeKey.same(client.options.keyAttack)) {
                        while (client.options.keyAttack.consumeClick()) {
                        }
                    }
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(
                            com.velorise.staves.network.MavuikaActionPayload.ACTION_PLUNGE_ATTACK);
                }
                wasAirAttackDown[0] = atkDown;
            } else {
                wasAirJumpDown[0] = false;
                wasAirUseDown[0] = false;
                wasAirAttackDown[0] = false;
                wasOnGround[0] = true;
                wasCreativeFlying[0] = false;
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
                com.velorise.staves.client.GenshinInputState.sampleTick(client);
                queuedFastAttackClicks[0] = 0;
                while (client.options.keyAttack.consumeClick()) {
                    // Suppress vanilla melee swing/block-breaking while riding bike
                }
                while (client.options.keyUse.consumeClick()) {
                    // Suppress vanilla right-click re-triggers while riding bike
                }
                if (com.velorise.staves.client.GenshinInputState.jump().justPressed()) {
                    bikeInputBuffer.notifyJumpPressed();
                }
                com.velorise.staves.fsm.MavuikaInputBuffer.TickDecision bikeDec = bikeInputBuffer.tickAttackInput(
                        com.velorise.staves.client.GenshinInputState.attack().isDown(),
                        com.velorise.staves.client.GenshinInputState.attack().physicalPresses(),
                        bike.getUltiTicks() > 8,
                        bike.isSpinning(),
                        bike.getActiveNaStep(),
                        bike.getActiveNaTicks()
                );
                bikeSpinActive[0] = bikeInputBuffer.isSpinActive();
                bikeAutoSpinLatched[0] = bikeInputBuffer.isAutoSpinLatched();
                if (bikeDec.cancelClientFinisherToSpin) {
                    bike.cancelClientFinisherToSpin();
                }
                if (bikeDec.triggerClientFinisher) {
                    bike.triggerClientFinisher(client.player);
                }
                if (bikeDec.sendSpinStart) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_START);
                }
                if (bikeDec.sendSpinStop) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_STOP);
                }
                if (bikeDec.sendSpinTapPulse) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_START);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_STOP);
                }
                if (bikeDec.sendNormalAttack) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_BIKE_NA);
                }
                boolean useHeld = !bikeMountWaitRelease[0] && com.velorise.staves.client.GenshinInputState.skill().isDown();
                boolean sprintDown = com.velorise.staves.client.GenshinInputState.sprint().isDown() || useHeld;
                boolean dashTriggered = sprintDown && !wasBikeDashDown[0] && !bike.isOnDashCooldown() && bike.getUltiTicks() <= 0;
                wasBikeDashDown[0] = sprintDown;
                if (dashTriggered) {
                    boolean wasSpinningOrHolding = bikeSpinActive[0] || bike.isSpinning() || bikeSpinReleaseWindowTicks[0] > 0
                            || bikeAutoSpinLatched[0] || client.options.keyAttack.isDown();
                    bike.triggerClientDash(client.player);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_DASH_CANCEL);
                    bikeInputBuffer.notifyDashTriggered(client.options.keyAttack.isDown(), bike.isSpinning());
                    bikeHeldThroughDash[0] = wasSpinningOrHolding && client.options.keyAttack.isDown();
                    bikeSpinReleaseWindowTicks[0] = wasSpinningOrHolding ? 7 : 0;
                    bikeAutoSpinLatched[0] = false;
                    bikeSpinTapCount[0] = 0;
                    bikeDoubleTapWindowTicks[0] = 0;
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
                bikeSpinReleaseWindowTicks[0] = 0;
                bikeHeldThroughDash[0] = false;
                bikeAutoSpinLatched[0] = false;
                bikeSpinTapCount[0] = 0;
                bikeDoubleTapWindowTicks[0] = 0;
                bikeInputBuffer.reset();
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
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.velorise.staves.VeloriseStavesMod.CRYO_REGISVINE,
                CryoRegisvineRenderer::new
        );
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.velorise.staves.VeloriseStavesMod.CRYO_ICICLE,
                CryoIcicleRenderer::new
        );

        net.minecraft.resources.ResourceLocation pullId = net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull");
        net.minecraft.resources.ResourceLocation pullingId = net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling");
        for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                com.velorise.staves.VeloriseStavesMod.THE_DAYBREAK_CHRONICLES,
                com.velorise.staves.VeloriseStavesMod.NETHERITE_THE_DAYBREAK_CHRONICLES
        }) {
            net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                    (stack, level, entity, seed) -> ClientAuraRenderer.getDaybreakPullProgress(entity, stack));
            net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                    (stack, level, entity, seed) -> ClientAuraRenderer.isDaybreakPulling(entity, stack) ? 1.0F : 0.0F);
        }
        net.minecraft.resources.ResourceLocation spearStanceId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("velorise_staves", "spear_stance");
        for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                com.velorise.staves.VeloriseStavesMod.TSARITSA_BOW,
                com.velorise.staves.VeloriseStavesMod.NETHERITE_TSARITSA_BOW
        }) {
            net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                    (stack, level, entity, seed) -> ClientAuraRenderer.getTsaritsaPullProgress(entity, stack));
            net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                    (stack, level, entity, seed) -> ClientAuraRenderer.isTsaritsaPulling(entity, stack) ? 1.0F : 0.0F);
            net.minecraft.client.renderer.item.ItemProperties.register(item, spearStanceId,
                    (stack, level, entity, seed) -> (entity != null && ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) ? 1.0F : 0.0F);
        }
        for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[]{
                com.velorise.staves.VeloriseStavesMod.OMNI_STAFF,
                com.velorise.staves.VeloriseStavesMod.NETHERITE_OMNI_STAFF
        }) {
            net.minecraft.client.renderer.item.ItemProperties.register(item, pullId,
                    (stack, level, entity, seed) -> com.velorise.staves.item.ArcaneStaffItem.getEffectiveKind(stack) == com.velorise.staves.item.ArcaneStaffItem.Kind.DAYBREAK
                            ? ClientAuraRenderer.getDaybreakPullProgress(entity, stack)
                            : ClientAuraRenderer.getTsaritsaPullProgress(entity, stack));
            net.minecraft.client.renderer.item.ItemProperties.register(item, pullingId,
                    (stack, level, entity, seed) -> (ClientAuraRenderer.isDaybreakPulling(entity, stack) || ClientAuraRenderer.isTsaritsaPulling(entity, stack)) ? 1.0F : 0.0F);
            net.minecraft.client.renderer.item.ItemProperties.register(item, spearStanceId,
                    (stack, level, entity, seed) -> (entity != null && ClientAuraTracker.isTsaritsaSpearStance(entity.getId())) ? 1.0F : 0.0F);
        }

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientAuraTracker.clear();
            com.velorise.staves.element.ElementalResonanceManager.clearClientState();
            ClientAuraRenderer.resetFrozenClientState();
            OmniWheelOverlay.reset();
            com.velorise.staves.config.StavesConfig.clearServerSync();
        });
    }
}
