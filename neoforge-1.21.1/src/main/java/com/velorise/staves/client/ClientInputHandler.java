package com.velorise.staves.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Dedicated client-side combat input, keybind registration, and character skill/burst/movement controller.
 * Separated from OmniWheelOverlay so OmniWheelOverlay remains purely a HUD/UI renderer.
 */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static long lastDaybreakClientShotMs = 0L;
    private static final java.lang.reflect.Field MISS_TIME_FIELD;
    static {
        java.lang.reflect.Field f = null;
        try {
            f = Minecraft.class.getDeclaredField("missTime");
            f.setAccessible(true);
        } catch (Throwable ignored) {}
        MISS_TIME_FIELD = f;
    }

    private static void resetMissTime(Minecraft mc) {
        if (mc != null && MISS_TIME_FIELD != null) {
            try {
                MISS_TIME_FIELD.setInt(mc, 0);
            } catch (Throwable ignored) {}
        }
    }

    private ClientInputHandler() {}

    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.velorise_staves.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.velorise_staves"
    );

    public static final KeyMapping MAVUIKA_ULTI_KEY = new KeyMapping(
            "key.velorise_staves.mavuika_ulti",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.velorise_staves"
    );
    private static boolean wasBikeDashDown = false;
    private static boolean bikeSprintActive = false;
    private static boolean bikeMountWaitRelease = false;
    private static boolean wasRidingBike = false;
    private static int bikeAttackHoldTicks = 0;
    private static boolean bikeSpinActive = false;
    private static int bikeSpinReleaseWindowTicks = 0;
    private static boolean bikeHeldThroughDash = false;
    private static boolean bikeAutoSpinLatched = false;
    private static int bikeSpinTapCount = 0;
    private static int bikeDoubleTapWindowTicks = 0;
    private static boolean bikeDismountWaitAttackRelease = false;
    private static int queuedFastAttackClicks = 0;
    private static boolean ventiUltiFiredThisHold = false;
    private static boolean neuvilletteLeftHoldSent = false;
    private static boolean wasAirJumpDown = false;
    private static boolean wasAirUseDown = false;
    private static boolean wasAirAttackDown = false;
    private static boolean wasOnGround = true;
    private static boolean wasCreativeFlying = false;
    private static boolean wasXilonenSkateActive = false;
    private static boolean wasXilonenDashKeyDown = false;
    private static int furinaAttackHoldTicks = 0;
    private static boolean furinaCaTriggeredThisHold = false;
    private static int raidenAttackHoldTicks = 0;
    private static boolean raidenCaTriggeredThisHold = false;
    private static int nahidaAttackHoldTicks = 0;
    private static boolean nahidaCaTriggeredThisHold = false;
    private static int queuedNahidaFastClicks = 0;
    private static int queuedTsaritsaFastClicks = 0;
    private static int queuedDaybreakFastClicks = 0;
    private static int tsaritsaSpearHoldTick = 0;
    private static int lastSelectedHotbarSlot = -1;

    @EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OmniWheelOverlay.OMNI_WHEEL_KEY);
            event.register(MAVUIKA_ULTI_KEY);
            event.register(OPEN_CONFIG_KEY);
        }
    }

    @SubscribeEvent
    public static void onInteractionKeyTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity || bikeDismountWaitAttackRelease)) {
            if (mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) {
                queuedFastAttackClicks++;
            }
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (mc.player != null && com.velorise.staves.element.NeuvilletteManager.isHoldingGuardianLens(mc.player)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (mc.player != null && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
            ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem());
            if (effKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) {
                queuedNahidaFastClicks++;
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            if (effKind == ArcaneStaffItem.Kind.DAYBREAK) {
                queuedDaybreakFastClicks++;
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            if (effKind == ArcaneStaffItem.Kind.TSARITSA_BOW || effKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR) {
                queuedTsaritsaFastClicks++;
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            if (effKind != null && ClientAuraTracker.isTsaritsaDpsSeal(mc.player.getId())) {
                event.setSwingHand(true);
                event.setCanceled(true);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                return;
            }
        }
        if (mc.player != null
                && (com.velorise.staves.client.ClientAuraTracker.isPlunging(mc.player.getId())
                        || com.velorise.staves.element.PlungeAndGliderManager.isPlunging(mc.player))) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (mc.player != null
                && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                && com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            mc.player.swinging = false;
            mc.player.swingTime = 0;
            mc.player.attackAnim = 0.0F;
            mc.player.oAttackAnim = 0.0F;
            com.velorise.staves.client.ClientAuraTracker.triggerStaffAnimation(
                    mc.player.getId(), com.velorise.staves.client.ClientAuraTracker.ANIM_PLUNGE_START);
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_PLUNGE_ATTACK);
            return;
        }
        if (mc.player != null
                && com.velorise.staves.client.ClientAuraTracker.isRaidenMusouActive(mc.player.getId())
                && ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING) {
            event.setSwingHand(false);
            event.setCanceled(true);
            if (!com.velorise.staves.client.ClientAuraTracker.localRaidenAttackHeld
                    && com.velorise.staves.client.ClientAuraTracker.tryConsumeLocalRaidenSwing(false)) {
                com.velorise.staves.client.ClientAuraTracker.triggerStaffAnimation(
                        mc.player.getId(), com.velorise.staves.client.ClientAuraTracker.ANIM_RAIDEN_SLASH);
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new com.velorise.staves.network.RaidenSlashPayload(false));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        OmniWheelOverlay.tick(mc);
        if (mc.player != null) {
            ArcaneStaffItem.tickCraftingMenuOmniPreview(mc.player);
            int curSlot = mc.player.getInventory().selected;
            if (lastSelectedHotbarSlot != -1 && curSlot != lastSelectedHotbarSlot) {
                ItemStack main = mc.player.getMainHandItem();
                ClientAuraTracker.interruptActiveAnimations(mc.player.getId());
                if (main.getItem() instanceof ArcaneStaffItem) {
                    com.velorise.staves.element.ElementalResonanceManager.notifyClientSlotSwitchedToStaff(mc.player, main);
                }
            }
            lastSelectedHotbarSlot = curSlot;
        } else {
            lastSelectedHotbarSlot = -1;
        }
        if (mc.player == null || mc.screen != null) {
            wasBikeDashDown = false;
            bikeSprintActive = false;
            bikeMountWaitRelease = false;
            wasRidingBike = false;
            bikeAttackHoldTicks = 0;
            bikeSpinActive = false;
            bikeSpinReleaseWindowTicks = 0;
            bikeHeldThroughDash = false;
            bikeAutoSpinLatched = false;
            bikeSpinTapCount = 0;
            bikeDoubleTapWindowTicks = 0;
            queuedFastAttackClicks = 0;
            ClientAuraTracker.ventiUltiHoldTicks = 0;
            ventiUltiFiredThisHold = false;
            furinaAttackHoldTicks = 0;
            furinaCaTriggeredThisHold = false;
            raidenAttackHoldTicks = 0;
            raidenCaTriggeredThisHold = false;
            nahidaAttackHoldTicks = 0;
            nahidaCaTriggeredThisHold = false;
            queuedNahidaFastClicks = 0;
            queuedTsaritsaFastClicks = 0;
            tsaritsaSpearHoldTick = 0;
            ClientAuraTracker.localTsaritsaBowPullTicks = 0;
            ClientAuraTracker.localDaybreakBowPullTicks = 0;
            queuedDaybreakFastClicks = 0;
            ClientAuraTracker.localTsaritsaHoldETicks = 0;
            ClientAuraTracker.localTsaritsaHoldELocked = false;
            return;
        }
        if (ClientAuraTracker.isPlunging(mc.player.getId()) || com.velorise.staves.element.PlungeAndGliderManager.isPlunging(mc.player)) {
            mc.player.swinging = false;
            mc.player.swingTime = 0;
            mc.player.attackAnim = 0.0F;
            mc.player.oAttackAnim = 0.0F;
        }
        if (ClientAuraTracker.isFiveStarBurstRootActive(mc.player.getId())) {
            mc.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
        while (OPEN_CONFIG_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new VeloriseConfigScreen(null));
            }
        }
        while (MAVUIKA_ULTI_KEY.consumeClick()) {
            if (mc.player != null) {
                boolean grounded = com.velorise.staves.element.ArchonUltiManager.isPlayerGroundedForBurst(mc.player);
                if (!grounded) {
                    continue;
                }
            }
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_ULTI);
        }

        // Item 3: Neuvillette Hold Left-Click Charged Attack (Equitable Judgment)
        boolean holdingGuardian = com.velorise.staves.element.NeuvilletteManager.isHoldingGuardianLens(mc.player);
        if (holdingGuardian && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
            if (mc.options.keyAttack.isDown()) {
                while (mc.options.keyAttack.consumeClick()) {
                    // Suppress melee swing / block breaking while channeling Equitable Judgment
                }
                if (!neuvilletteLeftHoldSent) {
                    neuvilletteLeftHoldSent = true;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_NEUVILLETTE_CA_START);
                }
            } else if (neuvilletteLeftHoldSent) {
                neuvilletteLeftHoldSent = false;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_NEUVILLETTE_CA_STOP);
            }
        } else if (neuvilletteLeftHoldSent) {
            neuvilletteLeftHoldSent = false;
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_NEUVILLETTE_CA_STOP);
        }

        // Furina Hold Left-Click Charged Attack (Arkhe Pneuma / Ousia Switch)
        boolean holdingFurina = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.SPLENDOR
                || (!ArcaneStaffItem.isGenshinCharacterWeapon(mc.player.getMainHandItem())
                        && ArcaneStaffItem.getEffectiveKind(mc.player.getOffhandItem()) == ArcaneStaffItem.Kind.SPLENDOR);
        if (holdingFurina && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) && mc.player.onGround()) {
            if (mc.options.keyAttack.isDown()) {
                furinaAttackHoldTicks++;
                if (furinaAttackHoldTicks >= 10 && !furinaCaTriggeredThisHold) {
                    furinaCaTriggeredThisHold = true;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_FURINA_CA_SWITCH);
                }
            } else {
                furinaAttackHoldTicks = 0;
                furinaCaTriggeredThisHold = false;
            }
        } else {
            furinaAttackHoldTicks = 0;
            furinaCaTriggeredThisHold = false;
        }

        // Raiden Musou Hold Left-Click Charged Attack: 5-block dash + 5 trailing slashes
        boolean holdingRaidenMusou = ClientAuraTracker.isRaidenMusouActive(mc.player.getId())
                && ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.ENGULFING_LIGHTNING;
        if (holdingRaidenMusou && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
            if (mc.options.keyAttack.isDown()) {
                raidenAttackHoldTicks++;
                if (raidenAttackHoldTicks >= 8 && !raidenCaTriggeredThisHold
                        && ClientAuraTracker.tryConsumeLocalRaidenSwing(true)) {
                    raidenCaTriggeredThisHold = true;
                    ClientAuraTracker.localRaidenAttackHeld = true;
                    ClientAuraTracker.triggerStaffAnimation(mc.player.getId(), ClientAuraTracker.ANIM_RAIDEN_CA);
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.velorise.staves.network.RaidenSlashPayload(true));
                }
            } else {
                raidenAttackHoldTicks = 0;
                raidenCaTriggeredThisHold = false;
                ClientAuraTracker.localRaidenAttackHeld = false;
            }
        } else {
            raidenAttackHoldTicks = 0;
            raidenCaTriggeredThisHold = false;
            ClientAuraTracker.localRaidenAttackHeld = false;
        }

        // Reset Tsaritsa Hold E lock once Right-Click is released
        if (!mc.options.keyUse.isDown()) {
            ClientAuraTracker.localTsaritsaHoldETicks = 0;
            ClientAuraTracker.localTsaritsaHoldELocked = false;
        }

        // Nahida Left-Click (Tap = 4-step Hopscotch NA, Hold = Digital Square CA)
        ArcaneStaffItem.Kind mainEffKind = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem());
        boolean holdingNahida = (mainEffKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS);
        int nahidaFast = queuedNahidaFastClicks;
        queuedNahidaFastClicks = 0;
        if (holdingNahida && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
            while (mc.options.keyAttack.consumeClick()) {
                nahidaFast++;
            }
            if (mc.options.keyAttack.isDown()) {
                nahidaAttackHoldTicks++;
                if (nahidaAttackHoldTicks >= 10 && !nahidaCaTriggeredThisHold) {
                    nahidaCaTriggeredThisHold = true;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_NAHIDA_CA);
                }
            } else if (nahidaAttackHoldTicks > 0) {
                if (!nahidaCaTriggeredThisHold) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_NAHIDA_NA);
                }
                nahidaAttackHoldTicks = 0;
                nahidaCaTriggeredThisHold = false;
            } else if (nahidaFast > 0) {
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_NAHIDA_NA);
            }
        } else {
            nahidaAttackHoldTicks = 0;
            nahidaCaTriggeredThisHold = false;
        }

        // Tsaritsa Left-Click (Bow: Tap = NA arrow, Hold = 3-stage CA _pulling_0/1/2 without HUD bar; Spear: melee NA, CA locked)
        boolean holdingTsaritsa = (mainEffKind == ArcaneStaffItem.Kind.TSARITSA_BOW || mainEffKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR);
        int tsaritsaFast = queuedTsaritsaFastClicks;
        queuedTsaritsaFastClicks = 0;
        if (holdingTsaritsa && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
            while (mc.options.keyAttack.consumeClick()) {
                tsaritsaFast++;
            }
            boolean spearStance = (mainEffKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR)
                    || ClientAuraTracker.isTsaritsaSpearStance(mc.player.getId());
            if (spearStance) {
                ClientAuraTracker.localTsaritsaBowPullTicks = 0;
                if (mc.options.keyAttack.isDown()) {
                    if (tsaritsaSpearHoldTick % 7 == 0) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                    }
                    tsaritsaSpearHoldTick++;
                } else {
                    if (tsaritsaFast > 0 && tsaritsaSpearHoldTick == 0) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                    }
                    tsaritsaSpearHoldTick = 0;
                }
            } else {
                tsaritsaSpearHoldTick = 0;
                boolean leftPhysDown = mc.options.keyAttack.isDown()
                        || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                if (leftPhysDown) {
                    ClientAuraTracker.localTsaritsaBowPullTicks = Math.min(60, ClientAuraTracker.localTsaritsaBowPullTicks + 1);
                } else {
                    int pulled = ClientAuraTracker.localTsaritsaBowPullTicks;
                    ClientAuraTracker.localTsaritsaBowPullTicks = 0;
                    if (pulled >= 16) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_CA_FULL);
                    } else if (pulled >= 6) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_CA);
                    } else if (pulled > 0 || tsaritsaFast > 0) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_TSARITSA_NA);
                    }
                }
            }
        } else {
            ClientAuraTracker.localTsaritsaBowPullTicks = 0;
            tsaritsaSpearHoldTick = 0;
        }

        // Venti / Daybreak Chronicles Left-Click (Tap = Quick Wind Shot, Hold = 3-stage Charged Aim Shot _pulling_0/1/2)
        boolean holdingDaybreak = (mainEffKind == ArcaneStaffItem.Kind.DAYBREAK);
        int daybreakFast = queuedDaybreakFastClicks;
        queuedDaybreakFastClicks = 0;
        if (holdingDaybreak && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
            resetMissTime(mc);
            while (mc.options.keyAttack.consumeClick()) {
                daybreakFast++;
            }
            boolean leftPhysDown = mc.options.keyAttack.isDown()
                    || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (leftPhysDown) {
                ClientAuraTracker.localDaybreakBowPullTicks = Math.min(60, ClientAuraTracker.localDaybreakBowPullTicks + 1);
                ClientAuraTracker.ventiUltiHoldTicks = ClientAuraTracker.localDaybreakBowPullTicks;
            } else {
                int pulled = ClientAuraTracker.localDaybreakBowPullTicks;
                ClientAuraTracker.localDaybreakBowPullTicks = 0;
                ClientAuraTracker.ventiUltiHoldTicks = 0;
                long nowMs = System.currentTimeMillis();
                if (pulled >= 16) {
                    lastDaybreakClientShotMs = nowMs;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_DAYBREAK_CA_FULL);
                } else if (pulled >= 6) {
                    lastDaybreakClientShotMs = nowMs;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_DAYBREAK_CA);
                } else if ((pulled > 0 || daybreakFast > 0) && nowMs - lastDaybreakClientShotMs >= 150L) {
                    lastDaybreakClientShotMs = nowMs;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_DAYBREAK_NA);
                }
            }
        } else {
            ClientAuraTracker.localDaybreakBowPullTicks = 0;
            ClientAuraTracker.ventiUltiHoldTicks = 0;
        }

        // Item 1: Xilonen Roller-Skate Dash (release & re-press Sprint or Right-Click with 0.5s ICD)
        if (ClientAuraTracker.isXilonenSkating(mc.player.getId()) && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
            if (!wasXilonenSkateActive) {
                wasXilonenSkateActive = true;
                wasXilonenDashKeyDown = mc.options.keySprint.isDown() || mc.options.keyUse.isDown();
            } else {
                boolean dashKeyDown = mc.options.keySprint.isDown() || (mc.options.keyUse.isDown() && !mc.player.isShiftKeyDown());
                if (dashKeyDown && !wasXilonenDashKeyDown
                        && (mc.player.onGround() || ClientAuraTracker.isXilonenWallClimbing(mc.player.getId()))) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_XILONEN_DASH);
                }
                wasXilonenDashKeyDown = dashKeyDown;
            }
        } else {
            wasXilonenSkateActive = false;
            wasXilonenDashKeyDown = false;
        }

        // Item 1 & 4: Mid-Air Space (Xilonen Skate Jump / Wall-Leap / Wind Glider Toggle) & Mid-Air Left-Click (Universal Plunge Attack)
        if (!(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)) {
            boolean jumpDown = mc.options.keyJump.isDown();
            if (jumpDown && !wasAirJumpDown) {
                if (ClientAuraTracker.isXilonenWallClimbing(mc.player.getId())) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_XILONEN_WALL_LEAP);
                } else if (ClientAuraTracker.isXilonenSkating(mc.player.getId()) && (mc.player.onGround() || wasOnGround)) {
                    net.minecraft.world.phys.Vec3 v = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(v.x, Math.max(v.y, 0.58D), v.z);
                    ClientAuraTracker.triggerStaffAnimation(mc.player.getId(), ClientAuraTracker.ANIM_XILONEN_JUMP_FLIP);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_XILONEN_JUMP);
                }
            }
            wasAirJumpDown = jumpDown;
            boolean useDown = mc.options.keyUse.isDown();
            if (useDown && !wasAirUseDown
                    && (ClientAuraTracker.localWindGliderUnlocked || com.velorise.staves.element.PlungeAndGliderManager.isWindGliderUnlocked(mc.player))
                    && !mc.player.onGround() && !wasOnGround
                    && !mc.player.isInWater() && !mc.player.isInLava()
                    && !mc.player.onClimbable() && mc.player.getVehicle() == null
                    && !ClientAuraTracker.isPlunging(mc.player.getId())
                    && (mc.hitResult == null || mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
                    && com.velorise.staves.element.PlungeAndGliderManager.canRightClickToggleGlider(mc.player)) {
                ClientAuraTracker.localWindGliderUnlocked = true;
                while (mc.options.keyUse.consumeClick()) {
                }
                if (mc.player.getAbilities().flying) {
                    mc.player.getAbilities().flying = false;
                    mc.player.onUpdateAbilities();
                }
                if (mc.player.isFallFlying()) {
                    mc.player.stopFallFlying();
                }
                boolean nextGlide = !ClientAuraTracker.isGliding(mc.player.getId());
                ClientAuraTracker.triggerStaffAnimation(mc.player.getId(),
                        nextGlide ? ClientAuraTracker.ANIM_GLIDER_START : ClientAuraTracker.ANIM_GLIDER_STOP);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_TOGGLE_GLIDER);
            }
            wasAirUseDown = useDown;
            wasOnGround = mc.player.onGround();
            if (mc.player.onGround()) {
                wasCreativeFlying = false;
            } else if (mc.player.getAbilities().flying && !ClientAuraTracker.isGliding(mc.player.getId())) {
                wasCreativeFlying = true;
            }

            if (ClientAuraTracker.isGliding(mc.player.getId())
                    && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava()
                    && !mc.player.getAbilities().flying) {
                boolean hasVenti = com.velorise.staves.element.VentiManager.hasVentiWeapon(mc.player);
                double maxFallSpeed = hasVenti ? -0.055D : -0.11D;
                double maxSpeed = hasVenti ? 0.84D : 0.42D;
                double accel = hasVenti ? 0.11D : 0.055D;

                net.minecraft.world.phys.Vec3 vel = mc.player.getDeltaMovement();
                double dy = vel.y < maxFallSpeed ? Math.max(maxFallSpeed, vel.y * 0.65D) : vel.y;
                net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
                net.minecraft.world.phys.Vec3 horiz = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z);
                double vx = vel.x;
                double vz = vel.z;
                if (horiz.lengthSqr() > 1.0E-4D && (mc.player.zza > 0.01F || Math.hypot(vx, vz) > 0.08D)) {
                    horiz = horiz.normalize();
                    vx = vx * 0.86D + horiz.x * accel;
                    vz = vz * 0.86D + horiz.z * accel;
                    double hs = Math.hypot(vx, vz);
                    if (hs > maxSpeed) {
                        vx = vx / hs * maxSpeed;
                        vz = vz / hs * maxSpeed;
                    }
                }
                mc.player.setDeltaMovement(vx, dy, vz);
                mc.player.fallDistance = 0.0F;
            }

            boolean atkDown = mc.options.keyAttack.isDown();
            if (atkDown && !wasAirAttackDown && !holdingGuardian
                    && com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
                while (mc.options.keyAttack.consumeClick()) {
                    // Suppress vanilla air swing when initiating plunge
                }
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_PLUNGE_ATTACK);
            }
            wasAirAttackDown = atkDown;
        } else {
            wasAirJumpDown = false;
            wasAirUseDown = false;
            wasAirAttackDown = false;
            wasOnGround = true;
            wasCreativeFlying = false;
        }
        if (mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity bike) {
            if (!wasRidingBike) {
                wasRidingBike = true;
                bikeMountWaitRelease = mc.options.keyUse.isDown();
                bikeDismountWaitAttackRelease = false;
                queuedFastAttackClicks = 0;
            }
            if (bikeMountWaitRelease && !mc.options.keyUse.isDown()) {
                bikeMountWaitRelease = false;
            }
            int attackClicks = queuedFastAttackClicks;
            queuedFastAttackClicks = 0;
            while (mc.options.keyAttack.consumeClick()) {
                // Suppress melee swing/block-breaking while riding bike, but count fast taps
                attackClicks++;
            }
            while (mc.options.keyUse.consumeClick()) {
                // Suppress item right-click re-triggers while riding bike
            }
            if (bikeSpinReleaseWindowTicks > 0) {
                bikeSpinReleaseWindowTicks--;
            }
            if (bikeDoubleTapWindowTicks > 0) {
                bikeDoubleTapWindowTicks--;
                if (bikeDoubleTapWindowTicks == 0) {
                    bikeSpinTapCount = 0;
                }
            }
            if (mc.options.keyJump.isDown()) {
                bikeAutoSpinLatched = false;
            }
            if (bike.getUltiTicks() > 8) {
                bikeAttackHoldTicks = 0;
                bikeSpinActive = false;
                bikeSpinReleaseWindowTicks = 0;
                bikeHeldThroughDash = false;
                bikeAutoSpinLatched = false;
                bikeSpinTapCount = 0;
                bikeDoubleTapWindowTicks = 0;
            } else {
                boolean attackDown = mc.options.keyAttack.isDown();
                if (!attackDown && bikeHeldThroughDash) {
                    bikeHeldThroughDash = false;
                }
                boolean committedFinisherSlam = bike.getActiveNaStep() == 6 && bike.getActiveNaTicks() > 5 && bike.getActiveNaTicks() <= 8;
                // Only use 1-tick fast re-entry when re-holding out of an active Donut Spin ("(" / CCD) or holding continuously through a Dash ("CD")
                boolean fastSpinReentry = bikeSpinReleaseWindowTicks > 0 || bikeHeldThroughDash
                        || (bike.getActiveNaStep() == 6 && bike.getActiveNaTicks() > 0 && (bike.getActiveNaTicks() >= 9 || bike.getActiveNaTicks() <= 5));
                int requiredHoldTicks = fastSpinReentry ? 1 : 6; // 6 ticks (300ms) hold from neutral so 1-5 tick clicks always perform Normal Attack!
                if (attackDown) {
                    if (bikeAttackHoldTicks == 0 && (bike.isSpinning() || bikeSpinReleaseWindowTicks > 0 || bikeAutoSpinLatched)) {
                        // Player tapped/pressed Left-Click while spinning or in release grace window
                        if (bikeAutoSpinLatched && bikeDoubleTapWindowTicks == 0) {
                            // Deliberate new press while already hands-free spinning: unlatch on release to allow manual Finisher ("F")
                            bikeAutoSpinLatched = false;
                        } else {
                            bikeSpinTapCount++;
                            bikeDoubleTapWindowTicks = 10; // 0.5s window for double-tap
                            if (bikeSpinTapCount >= 2) {
                                // Genshin Hands-Free Continuous Charged Attack Tech (Double-Tap while spinning)
                                bikeAutoSpinLatched = true;
                            }
                        }
                    }
                    bikeAttackHoldTicks++;
                    if (!bikeSpinActive && !committedFinisherSlam && bikeAttackHoldTicks >= requiredHoldTicks) {
                        bikeSpinActive = true;
                        if (bikeDoubleTapWindowTicks == 0 && !bikeAutoSpinLatched) {
                            bikeSpinTapCount = 0;
                            bikeDoubleTapWindowTicks = 10;
                        }
                        bike.cancelClientFinisherToSpin();
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_START);
                    }
                } else if (bikeAttackHoldTicks > 0) {
                    if (bikeSpinActive) {
                        if (bikeAutoSpinLatched) {
                            // Hands-Free Continuous Spin Latched: releasing Left-Click keeps spinning without triggering Finisher ("F")!
                            bikeSpinReleaseWindowTicks = 8;
                        } else {
                            bikeSpinReleaseWindowTicks = 6; // 300ms re-hold window for "(" / CCD / double-tap
                            if (bikeDoubleTapWindowTicks == 0) {
                                bikeDoubleTapWindowTicks = 10;
                            }
                            bike.triggerClientFinisher(mc.player);
                                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_STOP);
                            bikeSpinActive = false;
                        }
                    } else {
                        // Released before 6-tick hold threshold -> Normal Attack (N1..N5)!
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_BIKE_NA);
                    }
                    bikeAttackHoldTicks = 0;
                } else if (attackClicks > 0) {
                    // Ultra-fast click pressed and released within the same client tick
                    if (bike.isSpinning() || bikeSpinReleaseWindowTicks > 0 || bikeAutoSpinLatched) {
                        bikeSpinTapCount += attackClicks;
                        bikeDoubleTapWindowTicks = 10;
                        if (bikeSpinTapCount >= 2) {
                            bikeAutoSpinLatched = true;
                            bikeSpinActive = true;
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_START);
                        } else {
                            bikeSpinReleaseWindowTicks = 6;
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_START);
                            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_STOP);
                        }
                    } else {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_BIKE_NA);
                    }
                }
            }
            boolean useHeld = !bikeMountWaitRelease && mc.options.keyUse.isDown();
            boolean sprintDown = mc.options.keySprint.isDown() || useHeld;
            boolean dashTriggered = sprintDown && !wasBikeDashDown && !bike.isOnDashCooldown();
            wasBikeDashDown = sprintDown;
            if (dashTriggered) {
                boolean wasSpinningOrHolding = bikeSpinActive || bike.isSpinning() || bikeSpinReleaseWindowTicks > 0
                        || bikeAutoSpinLatched || mc.options.keyAttack.isDown();
                bike.triggerClientDash(mc.player);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_DASH_CANCEL);
                bikeHeldThroughDash = wasSpinningOrHolding && mc.options.keyAttack.isDown();
                bikeSpinReleaseWindowTicks = wasSpinningOrHolding ? 7 : 0;
                bikeAutoSpinLatched = false;
                bikeSpinTapCount = 0;
                bikeDoubleTapWindowTicks = 0;
                bikeAttackHoldTicks = 0;
                bikeSpinActive = false;
            }
            boolean wantSprint = sprintDown && bike.tickCount >= 10;
            if (wantSprint != bikeSprintActive) {
                bikeSprintActive = wantSprint;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(wantSprint
                        ? com.velorise.staves.network.MavuikaActionPayload.ACTION_SPRINT_START
                        : com.velorise.staves.network.MavuikaActionPayload.ACTION_SPRINT_STOP);
            }
        } else {
            if (wasRidingBike && mc.options.keyAttack.isDown()) {
                bikeDismountWaitAttackRelease = true;
            }
            wasRidingBike = false;
            bikeMountWaitRelease = false;
            wasBikeDashDown = false;
            bikeSprintActive = false;
            bikeAttackHoldTicks = 0;
            bikeSpinActive = false;
            bikeSpinReleaseWindowTicks = 0;
            bikeHeldThroughDash = false;
            bikeAutoSpinLatched = false;
            bikeSpinTapCount = 0;
            bikeDoubleTapWindowTicks = 0;
            if (bikeDismountWaitAttackRelease) {
                if (!mc.options.keyAttack.isDown()) {
                    bikeDismountWaitAttackRelease = false;
                } else {
                    while (mc.options.keyAttack.consumeClick()) {
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && !OmniWheelOverlay.isWheelActive() && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null
                    && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                    && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
                ArcaneStaffItem.Kind effKind = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem());
                if (effKind == ArcaneStaffItem.Kind.DAYBREAK) {
                    resetMissTime(mc);
                    queuedDaybreakFastClicks++;
                } else if (effKind == ArcaneStaffItem.Kind.THOUSAND_FLOATING_DREAMS) {
                    resetMissTime(mc);
                    queuedNahidaFastClicks++;
                } else if (effKind == ArcaneStaffItem.Kind.TSARITSA_BOW || effKind == ArcaneStaffItem.Kind.TSARITSA_SPEAR) {
                    resetMissTime(mc);
                    queuedTsaritsaFastClicks++;
                }
            }
        }
    }
}
