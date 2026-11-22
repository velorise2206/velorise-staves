package com.velorise.staves.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Dedicated client-side combat input, keybind registration, and character skill/burst/movement controller.
 * Separated from OmniWheelOverlay so OmniWheelOverlay remains purely a HUD/UI renderer.
 */
@Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static long lastDaybreakClientShotMs = 0L;
    private static final java.lang.reflect.Field MISS_TIME_FIELD;
    static {
        java.lang.reflect.Field f = null;
        for (String fieldName : new String[]{"missTime", "f_91084_", "f_91078_"}) {
            try {
                f = Minecraft.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                break;
            } catch (Throwable ignored) {}
        }
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

    public static final KeyMapping ELEMENTAL_BURST_KEY = new KeyMapping(
            "key.velorise_staves.elemental_burst",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.velorise_staves"
    );

    public static final KeyMapping PLUNGE_ATTACK_KEY = new KeyMapping(
            "key.velorise_staves.plunge_attack",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.velorise_staves"
    );

    public static final KeyMapping WIND_GLIDER_KEY = new KeyMapping(
            "key.velorise_staves.wind_glider",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.velorise_staves"
    );
    private static boolean wasBikeDashDown = false;
    private static final com.velorise.staves.fsm.MavuikaInputBuffer BIKE_INPUT_BUFFER = new com.velorise.staves.fsm.MavuikaInputBuffer();
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
    private static int queuedDragonfireFastClicks = 0;
    private static long lastDragonfireClientShotMs = 0L;
    private static int tsaritsaSpearHoldTick = 0;
    private static int lastSelectedHotbarSlot = -1;

    public static float getFurinaCaHoldProgress() {
        if (furinaAttackHoldTicks <= 0 || furinaCaTriggeredThisHold) return 0.0F;
        return net.minecraft.util.Mth.clamp(furinaAttackHoldTicks / 10.0F, 0.0F, 1.0F);
    }

    @Mod.EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OmniWheelOverlay.OMNI_WHEEL_KEY);
            event.register(ELEMENTAL_BURST_KEY);
            event.register(OPEN_CONFIG_KEY);
            event.register(PLUNGE_ATTACK_KEY);
            event.register(WIND_GLIDER_KEY);
        }
    }

    @SubscribeEvent
    public static void onInteractionKeyTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && ClientAuraTracker.isUltiCastActive(mc.player.getId())) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (!event.isAttack()) return;
        if (mc.player != null && (mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity || bikeDismountWaitAttackRelease)) {
            // Strictly cancel vanilla mining/attack; raw physical presses are captured by GenshinInputState
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
            if (effKind == ArcaneStaffItem.Kind.DRAGONFIRE || effKind == ArcaneStaffItem.Kind.SHULKER) {
                queuedDragonfireFastClicks++;
                event.setSwingHand(true);
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
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_TSARITSA_NA);
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
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_PLUNGE_ATTACK);
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
                com.velorise.staves.network.ModNetwork.CHANNEL.sendToServer(
                        new com.velorise.staves.network.RaidenSlashPacket(false));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            OmniWheelOverlay.tick(mc);
            if (mc.player != null) {
                ArcaneStaffItem.tickCraftingMenuOmniPreview(mc.player);
                int curSlot = mc.player.getInventory().selected;
                if (lastSelectedHotbarSlot != -1 && curSlot != lastSelectedHotbarSlot) {
                    if (ClientAuraTracker.isSlotSwitchLocked(mc.player.getId())) {
                        mc.player.getInventory().selected = lastSelectedHotbarSlot;
                        curSlot = lastSelectedHotbarSlot;
                    } else {
                        ItemStack main = mc.player.getMainHandItem();
                        ClientAuraTracker.interruptActiveAnimations(mc.player.getId());
                        if (main.getItem() instanceof ArcaneStaffItem) {
                            com.velorise.staves.element.ElementalResonanceManager.notifyClientSlotSwitchedToStaff(mc.player, main);
                        }
                    }
                }
                lastSelectedHotbarSlot = curSlot;
                if (ClientAuraTracker.isSlotSwitchLocked(mc.player.getId())) {
                    while (mc.options.keySwapOffhand.consumeClick()) {
                    }
                    while (mc.options.keyDrop.consumeClick()) {
                    }
                }
            } else {
                lastSelectedHotbarSlot = -1;
            }
            if (mc.player != null) {
                if (ClientAuraTracker.isGliding(mc.player.getId())
                        && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava()
                        && !mc.player.getAbilities().flying && mc.player.getVehicle() == null) {
                    boolean hasVenti = com.velorise.staves.element.VentiManager.hasVentiWeapon(mc.player);
                    boolean hasDragonGlider = com.velorise.staves.element.DragonfireCloudManager.hasDragonfireWeapon(mc.player);
                    double speedMult = (hasVenti ? 2.0D : 1.0D) * (hasDragonGlider ? 1.5D : 1.0D);
                    double maxFallSpeed = (hasVenti && hasDragonGlider) ? -0.04D : (hasVenti ? -0.055D : (hasDragonGlider ? -0.08D : -0.11D));
                    double maxSpeed = 0.42D * speedMult;
                    double accel = 0.055D * speedMult;

                    net.minecraft.world.phys.Vec3 vel = mc.player.getDeltaMovement();
                    double dy = vel.y < maxFallSpeed ? Math.max(maxFallSpeed, vel.y * 0.65D) : vel.y;
                    net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
                    net.minecraft.world.phys.Vec3 horiz = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z);
                    double vx = vel.x;
                    double vz = vel.z;
                    if (horiz.lengthSqr() > 1.0E-4D && (mc.player.zza > 0.01F || mc.screen != null || Math.hypot(vx, vz) > 0.08D)) {
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
                if (mc.screen != null) {
                    ClientAuraTracker.tickLocalStaminaAndDash(
                            mc.player,
                            false,
                            com.velorise.staves.network.MavuikaActionPacket.ACTION_TOGGLE_GLIDER,
                            com.velorise.staves.network.MavuikaActionPacket.ACTION_PLAYER_DASH
                    );
                }
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
            BIKE_INPUT_BUFFER.reset();
                GenshinInputState.reset();
                queuedFastAttackClicks = 0;
                ClientAuraTracker.ventiUltiHoldTicks = 0;
                ventiUltiFiredThisHold = false;
                furinaAttackHoldTicks = 0;
                furinaCaTriggeredThisHold = false;
                nahidaAttackHoldTicks = 0;
                nahidaCaTriggeredThisHold = false;
                queuedNahidaFastClicks = 0;
                queuedTsaritsaFastClicks = 0;
                tsaritsaSpearHoldTick = 0;
            ClientAuraTracker.localTsaritsaBowPullTicks = 0;
            ClientAuraTracker.localDaybreakBowPullTicks = 0;
            queuedDaybreakFastClicks = 0;
            queuedDragonfireFastClicks = 0;
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
        if (ClientAuraTracker.isUltiCastActive(mc.player.getId())) {
            mc.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            mc.player.setSprinting(false);
            if (mc.player.input != null) {
                mc.player.input.forwardImpulse = 0.0F;
                mc.player.input.leftImpulse = 0.0F;
                mc.player.input.up = false;
                mc.player.input.down = false;
                mc.player.input.left = false;
                mc.player.input.right = false;
                mc.player.input.jumping = false;
                mc.player.input.shiftKeyDown = false;
            }
            while (mc.options.keyAttack.consumeClick()) {}
            while (mc.options.keyUse.consumeClick()) {}
            while (mc.options.keyPickItem.consumeClick()) {}
            while (mc.options.keyJump.consumeClick()) {}
            while (mc.options.keySprint.consumeClick()) {}
            while (mc.options.keyShift.consumeClick()) {}
            while (mc.options.keyUp.consumeClick()) {}
            while (mc.options.keyDown.consumeClick()) {}
            while (mc.options.keyLeft.consumeClick()) {}
            while (mc.options.keyRight.consumeClick()) {}
            while (ELEMENTAL_BURST_KEY.consumeClick()) {}
            return;
        }
        ClientAuraTracker.tickLocalStaminaAndDash(
                mc.player,
                mc.options.keySprint.isDown(),
                com.velorise.staves.network.MavuikaActionPacket.ACTION_TOGGLE_GLIDER,
                com.velorise.staves.network.MavuikaActionPacket.ACTION_PLAYER_DASH
        );
        while (OPEN_CONFIG_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new VeloriseConfigScreen(null));
            }
        }
        while (ELEMENTAL_BURST_KEY.consumeClick()) {
            if (mc.player != null) {
                if (!ArcaneStaffItem.isGenshinCharacterWeapon(mc.player.getMainHandItem())) {
                    continue;
                }
                boolean grounded = com.velorise.staves.element.ArchonUltiManager.isPlayerGroundedForBurst(mc.player);
                if (!grounded) {
                    continue;
                }
            }
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_ULTI);
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
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_NEUVILLETTE_CA_START);
                }
            } else if (neuvilletteLeftHoldSent) {
                neuvilletteLeftHoldSent = false;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_NEUVILLETTE_CA_STOP);
            }
        } else if (neuvilletteLeftHoldSent) {
            neuvilletteLeftHoldSent = false;
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_NEUVILLETTE_CA_STOP);
        }

        // Furina Hold Left-Click Charged Attack (Arkhe Pneuma / Ousia Switch)
        boolean holdingFurina = ArcaneStaffItem.getEffectiveKind(mc.player.getMainHandItem()) == ArcaneStaffItem.Kind.SPLENDOR;
        if (holdingFurina && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity) && mc.player.onGround()) {
            if (mc.options.keyAttack.isDown()) {
                furinaAttackHoldTicks++;
                ClientAuraTracker.localFurinaCaHoldProgress = getFurinaCaHoldProgress();
                if (furinaAttackHoldTicks >= 10 && !furinaCaTriggeredThisHold) {
                    furinaCaTriggeredThisHold = true;
                    ClientAuraTracker.localFurinaCaHoldProgress = 0.0F;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_FURINA_CA_SWITCH);
                }
            } else {
                furinaAttackHoldTicks = 0;
                furinaCaTriggeredThisHold = false;
                ClientAuraTracker.localFurinaCaHoldProgress = 0.0F;
            }
        } else {
            furinaAttackHoldTicks = 0;
            furinaCaTriggeredThisHold = false;
            ClientAuraTracker.localFurinaCaHoldProgress = 0.0F;
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
                    com.velorise.staves.network.ModNetwork.sendRaidenSlash(true);
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
                    if (com.velorise.staves.client.ClientMovementTracker.tryConsumeChargedAttackStamina(mc.player, 50.0F)) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_NAHIDA_CA);
                    }
                }
            } else if (nahidaAttackHoldTicks > 0) {
                if (!nahidaCaTriggeredThisHold) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_NAHIDA_NA);
                }
                nahidaAttackHoldTicks = 0;
                nahidaCaTriggeredThisHold = false;
            } else if (nahidaFast > 0) {
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_NAHIDA_NA);
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
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_TSARITSA_NA);
                    }
                    tsaritsaSpearHoldTick++;
                } else {
                    if (tsaritsaFast > 0 && tsaritsaSpearHoldTick == 0) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_TSARITSA_NA);
                    }
                    tsaritsaSpearHoldTick = 0;
                }
            } else {
                tsaritsaSpearHoldTick = 0;
                boolean leftPhysDown = mc.options.keyAttack.isDown()
                        || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                if (leftPhysDown) {
                    if (mc.gameMode != null) {
                        mc.gameMode.stopDestroyBlock();
                    }
                    mc.player.swinging = false;
                    mc.player.swingTime = 0;
                    ClientAuraTracker.localTsaritsaBowPullTicks = Math.min(60, ClientAuraTracker.localTsaritsaBowPullTicks + 1);
                } else {
                    int pulled = ClientAuraTracker.localTsaritsaBowPullTicks;
                    ClientAuraTracker.localTsaritsaBowPullTicks = 0;
                    if (pulled >= 16) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_TSARITSA_CA_FULL);
                    } else if (pulled >= 6) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_TSARITSA_CA);
                    } else if (pulled > 0 || tsaritsaFast > 0) {
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_TSARITSA_NA);
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
                if (mc.gameMode != null) {
                    mc.gameMode.stopDestroyBlock();
                }
                mc.player.swinging = false;
                mc.player.swingTime = 0;
                ClientAuraTracker.localDaybreakBowPullTicks = Math.min(60, ClientAuraTracker.localDaybreakBowPullTicks + 1);
                ClientAuraTracker.ventiUltiHoldTicks = ClientAuraTracker.localDaybreakBowPullTicks;
            } else {
                int pulled = ClientAuraTracker.localDaybreakBowPullTicks;
                ClientAuraTracker.localDaybreakBowPullTicks = 0;
                ClientAuraTracker.ventiUltiHoldTicks = 0;
                long nowMs = System.currentTimeMillis();
                if (pulled >= 16) {
                    lastDaybreakClientShotMs = nowMs;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_DAYBREAK_CA_FULL);
                } else if (pulled >= 6) {
                    lastDaybreakClientShotMs = nowMs;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_DAYBREAK_CA);
                } else if ((pulled > 0 || daybreakFast > 0) && nowMs - lastDaybreakClientShotMs >= 150L) {
                    lastDaybreakClientShotMs = nowMs;
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_DAYBREAK_NA);
                }
            }
        } else {
            ClientAuraTracker.localDaybreakBowPullTicks = 0;
            ClientAuraTracker.ventiUltiHoldTicks = 0;
        }

        boolean holdingDragonfire = (mainEffKind == ArcaneStaffItem.Kind.DRAGONFIRE || mainEffKind == ArcaneStaffItem.Kind.SHULKER);
        int dragonfireFast = queuedDragonfireFastClicks;
        queuedDragonfireFastClicks = 0;
        if (holdingDragonfire && !(mc.player.getVehicle() instanceof com.velorise.staves.entity.FlamestriderBikeEntity)
                && !com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
            resetMissTime(mc);
            while (mc.options.keyAttack.consumeClick()) {
                dragonfireFast++;
            }
            long nowMs = System.currentTimeMillis();
            if (dragonfireFast > 0 && nowMs - lastDragonfireClientShotMs >= 200L) {
                lastDragonfireClientShotMs = nowMs;
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_DRAGONFIRE_NA);
            }
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
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_XILONEN_DASH);
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
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_XILONEN_WALL_LEAP);
                } else if (ClientAuraTracker.isXilonenSkating(mc.player.getId()) && (mc.player.onGround() || wasOnGround)) {
                    net.minecraft.world.phys.Vec3 v = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(v.x, Math.max(v.y, 0.58D), v.z);
                    ClientAuraTracker.triggerStaffAnimation(mc.player.getId(), ClientAuraTracker.ANIM_XILONEN_JUMP_FLIP);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_XILONEN_JUMP);
                }
            }
            wasAirJumpDown = jumpDown;
            KeyMapping activeGliderKey = WIND_GLIDER_KEY.isUnbound() ? mc.options.keyUse : WIND_GLIDER_KEY;
            boolean useDown = activeGliderKey.isDown();
            if (useDown && !wasAirUseDown
                    && (ClientAuraTracker.localWindGliderUnlocked || com.velorise.staves.element.PlungeAndGliderManager.isWindGliderUnlocked(mc.player))
                    && !mc.player.onGround() && !wasOnGround
                    && !mc.player.isInWater() && !mc.player.isInLava()
                    && !mc.player.onClimbable() && mc.player.getVehicle() == null
                    && !ClientAuraTracker.isPlunging(mc.player.getId())
                    && (mc.hitResult == null || mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
                    && com.velorise.staves.element.PlungeAndGliderManager.canRightClickToggleGlider(mc.player)) {
                ClientAuraTracker.localWindGliderUnlocked = true;
                while (activeGliderKey.consumeClick()) {
                }
                if (activeGliderKey != mc.options.keyUse && activeGliderKey.same(mc.options.keyUse)) {
                    while (mc.options.keyUse.consumeClick()) {
                    }
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
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_TOGGLE_GLIDER);
            }
            wasAirUseDown = useDown;
            wasOnGround = mc.player.onGround();
            if (mc.player.onGround()) {
                wasCreativeFlying = false;
            } else if (mc.player.getAbilities().flying && !ClientAuraTracker.isGliding(mc.player.getId())) {
                wasCreativeFlying = true;
            }

            KeyMapping activePlungeKey = PLUNGE_ATTACK_KEY.isUnbound() ? mc.options.keyAttack : PLUNGE_ATTACK_KEY;
            boolean atkDown = activePlungeKey.isDown();
            if (atkDown && !wasAirAttackDown && !holdingGuardian
                    && com.velorise.staves.element.PlungeAndGliderManager.canTriggerMidAirPlunge(mc.player)) {
                while (activePlungeKey.consumeClick()) {
                }
                if (activePlungeKey != mc.options.keyAttack && activePlungeKey.same(mc.options.keyAttack)) {
                    while (mc.options.keyAttack.consumeClick()) {
                        // Suppress vanilla air swing when initiating plunge
                    }
                }
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_PLUNGE_ATTACK);
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
                GenshinInputState.sampleTick(mc);
                queuedFastAttackClicks = 0;
                while (mc.options.keyAttack.consumeClick()) {
                    // Suppress vanilla melee swing/block-breaking while riding bike
                }
                while (mc.options.keyUse.consumeClick()) {
                    // Suppress vanilla right-click re-triggers while riding bike
                }
                if (GenshinInputState.jump().justPressed()) {
                    BIKE_INPUT_BUFFER.notifyJumpPressed();
                }
                com.velorise.staves.fsm.MavuikaInputBuffer.TickDecision bikeDec = BIKE_INPUT_BUFFER.tickAttackInput(
                        GenshinInputState.attack().isDown(),
                        GenshinInputState.attack().physicalPresses(),
                        bike.getUltiTicks() > 8,
                        bike.isSpinning(),
                        bike.getActiveNaStep(),
                        bike.getActiveNaTicks()
                );
                bikeSpinActive = BIKE_INPUT_BUFFER.isSpinActive();
                bikeAutoSpinLatched = BIKE_INPUT_BUFFER.isAutoSpinLatched();
                if (bikeDec.cancelClientFinisherToSpin) {
                    bike.cancelClientFinisherToSpin();
                }
                if (bikeDec.triggerClientFinisher) {
                    bike.triggerClientFinisher(mc.player);
                }
                if (bikeDec.sendSpinStart) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_SPIN_START);
                }
                if (bikeDec.sendSpinStop) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_SPIN_STOP);
                }
                if (bikeDec.sendSpinTapPulse) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_SPIN_START);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_SPIN_STOP);
                }
                if (bikeDec.sendNormalAttack) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_BIKE_NA);
                }
                boolean useHeld = !bikeMountWaitRelease && GenshinInputState.skill().isDown();
                boolean sprintDown = GenshinInputState.sprint().isDown() || useHeld;
                boolean dashTriggered = sprintDown && !wasBikeDashDown && !bike.isOnDashCooldown() && bike.getUltiTicks() <= 0;
                wasBikeDashDown = sprintDown;
                if (dashTriggered) {
                    boolean wasSpinningOrHolding = bikeSpinActive || bike.isSpinning() || bikeSpinReleaseWindowTicks > 0
                            || bikeAutoSpinLatched || mc.options.keyAttack.isDown();
                    bike.triggerClientDash(mc.player);
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPacket.ACTION_DASH_CANCEL);
                    BIKE_INPUT_BUFFER.notifyDashTriggered(mc.options.keyAttack.isDown(), bike.isSpinning());
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
                            ? com.velorise.staves.network.MavuikaActionPacket.ACTION_SPRINT_START
                            : com.velorise.staves.network.MavuikaActionPacket.ACTION_SPRINT_STOP);
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
            BIKE_INPUT_BUFFER.reset();
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
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!OmniWheelOverlay.isWheelActive()) {
            GenshinInputState.onRawMouseButton(event.getButton(), event.getAction());
        }
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
                } else if (effKind == ArcaneStaffItem.Kind.DRAGONFIRE || effKind == ArcaneStaffItem.Kind.SHULKER) {
                    resetMissTime(mc);
                    queuedDragonfireFastClicks++;
                }
            }
        }
    }
}
