package com.velorise.staves.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.element.Element;
import com.velorise.staves.entity.OmniOrbEntity;
import com.velorise.staves.item.ArcaneStaffItem;
import com.velorise.staves.network.ModNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * 8-sector Iron's-Spells-style radial wheel for selecting Omni Staff mode
 * (0 = OMNI at top, 1..7 = clockwise Elements) while holding Tab (or custom key).
 */
@EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class OmniWheelOverlay {
    public static final KeyMapping OMNI_WHEEL_KEY = new KeyMapping(
            "key.velorise_staves.omni_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            "key.categories.velorise_staves"
    );

    private static final ResourceLocation[] SLOT_TEXTURES = new ResourceLocation[8];
    private static boolean wheelActive = false;
    private static boolean suppressedUntilRelease = false;
    private static int hoveredSlot = -1;

    static {
        SLOT_TEXTURES[0] = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "textures/element/omni.png");
        for (int i = 1; i < 8; i++) {
            Element el = OmniOrbEntity.WHEEL_ELEMENTS[i];
            SLOT_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(
                    VeloriseStavesMod.MODID,
                    "textures/element/" + el.name().toLowerCase(Locale.ROOT) + ".png"
            );
        }
    }

    private OmniWheelOverlay() {}

    private static ResourceLocation getSlotTexture(int slot) {
        int clamped = OmniOrbEntity.clampMode(slot);
        if (clamped == 0) return ClientAuraRenderer.getOmniIconTexture();
        return ClientAuraRenderer.getElementIconTexture(OmniOrbEntity.WHEEL_ELEMENTS[clamped]);
    }

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
    private static boolean bikeDismountWaitAttackRelease = false;
    private static int queuedFastAttackClicks = 0;

    @EventBusSubscriber(modid = VeloriseStavesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OMNI_WHEEL_KEY);
            event.register(MAVUIKA_ULTI_KEY);
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
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        tick(mc);
        if (mc.player == null || mc.screen != null) {
            wasBikeDashDown = false;
            bikeSprintActive = false;
            bikeMountWaitRelease = false;
            wasRidingBike = false;
            bikeAttackHoldTicks = 0;
            bikeSpinActive = false;
            queuedFastAttackClicks = 0;
            return;
        }
        while (MAVUIKA_ULTI_KEY.consumeClick()) {
            com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_ULTI);
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
            if (bike.getUltiTicks() > 8 || (bike.getNaStep() == 6 && bike.getNaTicks() > 4)) {
                bikeAttackHoldTicks = 0;
                bikeSpinActive = false;
            } else {
                boolean attackDown = mc.options.keyAttack.isDown();
                if (attackDown) {
                    bikeAttackHoldTicks++;
                    if (!bikeSpinActive && bikeAttackHoldTicks >= 5) {
                        bikeSpinActive = true;
                        com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_START);
                    }
                } else if (bikeAttackHoldTicks > 0) {
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(bikeSpinActive
                            ? com.velorise.staves.network.MavuikaActionPayload.ACTION_SPIN_STOP
                            : com.velorise.staves.network.MavuikaActionPayload.ACTION_BIKE_NA);
                    bikeAttackHoldTicks = 0;
                    bikeSpinActive = false;
                } else if (attackClicks > 0) {
                    // Ultra-fast tap pressed and released within the same client tick
                    com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_BIKE_NA);
                }
            }
            boolean useHeld = !bikeMountWaitRelease && mc.options.keyUse.isDown();
            boolean sprintDown = mc.options.keySprint.isDown() || useHeld;
            boolean dashTriggered = sprintDown && !wasBikeDashDown && !bike.isOnDashCooldown();
            wasBikeDashDown = sprintDown;
            if (dashTriggered) {
                bike.triggerClientDash(mc.player);
                com.velorise.staves.network.ModNetwork.sendMavuikaAction(com.velorise.staves.network.MavuikaActionPayload.ACTION_DASH_CANCEL);
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
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.TAB_LIST) && shouldSuppressTabList()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!wheelActive || event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            int chosen = hoveredSlot >= 0 ? hoveredSlot : ArcaneStaffItem.getOmniModeIndex(getHeldOmniStack(mc.player));
            commitSelection(mc, chosen);
            wheelActive = false;
            suppressedUntilRelease = true;
            hoveredSlot = -1;
            mc.mouseHandler.grabMouse();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    public static void reset() {
        wheelActive = false;
        suppressedUntilRelease = false;
        hoveredSlot = -1;
        OmniOrbEntity.clearCycleCache();
    }

    public static boolean shouldSuppressTabList() {
        if (wheelActive || suppressedUntilRelease) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || getHeldOmniStack(mc.player).isEmpty()) return false;
        return OMNI_WHEEL_KEY.getKey().equals(mc.options.keyPlayerList.getKey()) && isWheelKeyHeld(mc);
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (wheelActive) {
                wheelActive = false;
                hoveredSlot = -1;
                suppressedUntilRelease = false;
            }
            return;
        }

        ItemStack heldOmni = getHeldOmniStack(mc.player);
        boolean canUse = mc.screen == null && !mc.options.hideGui && !heldOmni.isEmpty();
        boolean keyHeld = canUse && isWheelKeyHeld(mc);

        if (!keyHeld) {
            if (wheelActive) {
                int targetMode = hoveredSlot >= 0 ? hoveredSlot : ArcaneStaffItem.getOmniModeIndex(heldOmni);
                commitSelection(mc, targetMode);
                wheelActive = false;
                hoveredSlot = -1;
                if (mc.screen == null) {
                    mc.mouseHandler.grabMouse();
                }
            }
            suppressedUntilRelease = false;
            return;
        }

        if (!suppressedUntilRelease) {
            if (!wheelActive) {
                wheelActive = true;
                hoveredSlot = -1;
                mc.mouseHandler.releaseMouse();
            } else if (mc.mouseHandler.isMouseGrabbed()) {
                int targetMode = hoveredSlot >= 0 ? hoveredSlot : ArcaneStaffItem.getOmniModeIndex(heldOmni);
                commitSelection(mc, targetMode);
                wheelActive = false;
                hoveredSlot = -1;
                suppressedUntilRelease = true;
            }
        }
    }

    private static void commitSelection(Minecraft mc, int mode) {
        int clamped = OmniOrbEntity.clampMode(mode);
        if (mc.player != null) {
            ItemStack stack = getHeldOmniStack(mc.player);
            if (!stack.isEmpty()) {
                ArcaneStaffItem.setOmniModeIndex(stack, clamped);
            }
            ModNetwork.sendSelectOmniMode(clamped);
        }
    }

    private static ItemStack getHeldOmniStack(Player player) {
        if (player == null) return ItemStack.EMPTY;
        if (ArcaneStaffItem.isOmniStaff(player.getMainHandItem())) return player.getMainHandItem();
        if (ArcaneStaffItem.isOmniStaff(player.getOffhandItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    private static boolean isWheelKeyHeld(Minecraft mc) {
        InputConstants.Key key = OMNI_WHEEL_KEY.getKey();
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return OMNI_WHEEL_KEY.isDown();
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ItemStack omniStack = getHeldOmniStack(mc.player);
        if (omniStack.isEmpty()) return;

        int currentMode = ArcaneStaffItem.getOmniModeIndex(omniStack);
        if (mc.screen == null) {
            renderHotbarBadge(guiGraphics, mc, currentMode);
        }

        if (!wheelActive || mc.screen != null) return;

        int guiW = guiGraphics.guiWidth();
        int guiH = guiGraphics.guiHeight();
        double cx = guiW * 0.5D;
        double cy = guiH * 0.5D;
        double innerR = 24.0D;
        double outerR = 76.0D;
        double rimR = 79.5D;
        double iconDist = 51.0D;

        double mx = mc.mouseHandler.xpos() * (double) guiW / (double) Math.max(1, mc.getWindow().getScreenWidth());
        double my = mc.mouseHandler.ypos() * (double) guiH / (double) Math.max(1, mc.getWindow().getScreenHeight());
        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.hypot(dx, dy);
        if (dist >= 16.0D) {
            double angle = Math.atan2(dy, dx);
            double shifted = ((angle + Math.PI * 0.5D + Math.PI / 8.0D) % (Math.PI * 2.0D) + Math.PI * 2.0D) % (Math.PI * 2.0D);
            hoveredSlot = Math.max(0, Math.min(7, (int) Math.floor(shifted / (Math.PI * 0.25D))));
        } else {
            hoveredSlot = -1;
        }

        int previewSlot = hoveredSlot >= 0 ? hoveredSlot : currentMode;
        long timeMs = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose = guiGraphics.pose().last().pose();

        // Draw center dark hub
        drawDisc(buffer, pose, cx, cy, 0.0D, innerR - 1.5D, 0.05F, 0.06F, 0.10F, 0.80F, 32);

        // Draw 8 radial sectors + glowing outer rim
        double sectorSpan = Math.PI * 2.0D / 8.0D;
        int stepsPerSector = 6;
        for (int s = 0; s < 8; s++) {
            double startA = -Math.PI * 0.5D - sectorSpan * 0.5D + s * sectorSpan;
            int rgb = getSlotAccentRgb(s, timeMs);
            float er = ((rgb >> 16) & 0xFF) / 255.0F;
            float eg = ((rgb >> 8) & 0xFF) / 255.0F;
            float eb = (rgb & 0xFF) / 255.0F;

            boolean isHovered = (s == hoveredSlot);
            boolean isCurrent = (s == currentMode);

            float fillR = isHovered ? er * 0.65F + 0.08F : (isCurrent ? er * 0.28F + 0.07F : 0.07F);
            float fillG = isHovered ? eg * 0.65F + 0.09F : (isCurrent ? eg * 0.28F + 0.08F : 0.09F);
            float fillB = isHovered ? eb * 0.65F + 0.12F : (isCurrent ? eb * 0.28F + 0.12F : 0.13F);
            float fillA = isHovered ? 0.78F : (isCurrent ? 0.74F : 0.68F);

            float rimRCol = isHovered ? er : (isCurrent ? er * 0.85F : 0.48F);
            float rimGCol = isHovered ? eg : (isCurrent ? eg * 0.85F : 0.52F);
            float rimBCol = isHovered ? eb : (isCurrent ? eb * 0.85F : 0.60F);
            float rimAlpha = isHovered ? 0.96F : (isCurrent ? 0.78F : 0.40F);

            drawArcSegment(buffer, pose, cx, cy, innerR, outerR, startA, startA + sectorSpan, stepsPerSector, fillR, fillG, fillB, fillA);
            drawArcSegment(buffer, pose, cx, cy, outerR, rimR, startA, startA + sectorSpan, stepsPerSector, rimRCol, rimGCol, rimBCol, rimAlpha);
        }

        // Draw crisp dividing spokes
        for (int s = 0; s < 8; s++) {
            double edgeA = -Math.PI * 0.5D - sectorSpan * 0.5D + s * sectorSpan;
            drawSpoke(buffer, pose, cx, cy, innerR, rimR, edgeA, 1.35D, 0.85F, 0.78F, 0.55F, 0.55F);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // Draw 8 icons + center active preview icon
        guiGraphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int s = 0; s < 8; s++) {
            double midA = -Math.PI * 0.5D + s * sectorSpan;
            int iconSize = (s == hoveredSlot) ? 22 : 18;
            int ix = (int) Math.round(cx + Math.cos(midA) * iconDist - iconSize * 0.5D);
            int iy = (int) Math.round(cy + Math.sin(midA) * iconDist - iconSize * 0.5D);
            guiGraphics.blit(getSlotTexture(s), ix, iy, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        }

        // Center hub active icon
        int centerIconSize = 18;
        guiGraphics.blit(getSlotTexture(previewSlot),
                (int) Math.round(cx - centerIconSize * 0.5D),
                (int) Math.round(cy - centerIconSize * 0.5D),
                0.0F, 0.0F, centerIconSize, centerIconSize, centerIconSize, centerIconSize);

        // Label header above wheel
        Font font = mc.font;
        Element previewElem = OmniOrbEntity.modeToElement(previewSlot);
        int accentColor = 0xFF000000 | getSlotAccentRgb(previewSlot, timeMs);
        Component titleComp = previewElem != null
                ? Component.translatable(previewElem.translationKey())
                : Component.translatable("element.velorise_staves.omni");
        Component subComp = previewElem != null
                ? Component.translatable("wheel.velorise_staves.omni.locked")
                : Component.translatable("wheel.velorise_staves.omni.auto");

        int titleW = font.width(titleComp);
        int subW = font.width(subComp);
        int boxW = Math.max(titleW, subW) + 18;
        int boxX = (int) Math.round(cx) - boxW / 2;
        int boxY = (int) Math.round(cy - rimR) - 32;
        guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 25, 0xCC0D1018);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, boxX, boxY, boxW, 25, accentColor);
        guiGraphics.drawCenteredString(font, titleComp, (int) Math.round(cx), boxY + 4, accentColor);
        guiGraphics.drawCenteredString(font, subComp, (int) Math.round(cx), boxY + 14, 0xFFCBD5E1);

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderHotbarBadge(GuiGraphics guiGraphics, Minecraft mc, int mode) {
        Element el = OmniOrbEntity.modeToElement(mode);
        Font font = mc.font;
        Component label = el != null
                ? Component.translatable(el.translationKey())
                : Component.translatable("element.velorise_staves.omni");

        int textW = font.width(label);
        int badgeW = 20 + textW + 5;
        int badgeX = guiGraphics.guiWidth() / 2 + 98;
        if (badgeX + badgeW > guiGraphics.guiWidth() - 4) {
            badgeX = guiGraphics.guiWidth() / 2 - 98 - badgeW;
        }
        int badgeY = guiGraphics.guiHeight() - 20;

        int accent = 0xFF000000 | getSlotAccentRgb(mode, System.currentTimeMillis());
        guiGraphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 18, 0xB80E111A);
        ClientAuraRenderer.renderColoredBorder(guiGraphics, badgeX, badgeY, badgeW, 18, accent);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(getSlotTexture(mode), badgeX + 3, badgeY + 2, 0.0F, 0.0F, 14, 14, 14, 14);
        guiGraphics.drawString(font, label, badgeX + 20, badgeY + 5, accent, true);
    }

    private static int getSlotAccentRgb(int slot, long timeMs) {
        Element el = OmniOrbEntity.modeToElement(slot);
        if (el != null) {
            return OmniOrbEntity.getElementRgb(el);
        }
        float phase = (timeMs % 6000L) / 50.0F;
        return OmniOrbEntity.getIridescentRgb(phase);
    }

    private static void drawDisc(
            BufferBuilder buffer, Matrix4f pose,
            double cx, double cy, double innerR, double outerR,
            float r, float g, float b, float a, int segments
    ) {
        drawArcSegment(buffer, pose, cx, cy, innerR, outerR, 0.0D, Math.PI * 2.0D, segments, r, g, b, a);
    }

    private static void drawArcSegment(
            BufferBuilder buffer, Matrix4f pose,
            double cx, double cy, double innerR, double outerR,
            double startA, double endA, int steps,
            float r, float g, float b, float a
    ) {
        for (int i = 0; i < steps; i++) {
            double a0 = startA + (endA - startA) * i / steps;
            double a1 = startA + (endA - startA) * (i + 1) / steps;
            float x0i = (float) (cx + Math.cos(a0) * innerR);
            float y0i = (float) (cy + Math.sin(a0) * innerR);
            float x0o = (float) (cx + Math.cos(a0) * outerR);
            float y0o = (float) (cy + Math.sin(a0) * outerR);
            float x1o = (float) (cx + Math.cos(a1) * outerR);
            float y1o = (float) (cy + Math.sin(a1) * outerR);
            float x1i = (float) (cx + Math.cos(a1) * innerR);
            float y1i = (float) (cy + Math.sin(a1) * innerR);

            buffer.addVertex(pose, x0i, y0i, 0.0F).setColor(r, g, b, a);
            buffer.addVertex(pose, x0o, y0o, 0.0F).setColor(r, g, b, a);
            buffer.addVertex(pose, x1o, y1o, 0.0F).setColor(r, g, b, a);
            buffer.addVertex(pose, x1i, y1i, 0.0F).setColor(r, g, b, a);
        }
    }

    private static void drawSpoke(
            BufferBuilder buffer, Matrix4f pose,
            double cx, double cy, double innerR, double outerR,
            double angle, double halfWidth,
            float r, float g, float b, float a
    ) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double px = -sin * (halfWidth * 0.5D);
        double py = cos * (halfWidth * 0.5D);

        float x0 = (float) (cx + cos * innerR - px);
        float y0 = (float) (cy + sin * innerR - py);
        float x1 = (float) (cx + cos * outerR - px);
        float y1 = (float) (cy + sin * outerR - py);
        float x2 = (float) (cx + cos * outerR + px);
        float y2 = (float) (cy + sin * outerR + py);
        float x3 = (float) (cx + cos * innerR + px);
        float y3 = (float) (cy + sin * innerR + py);

        buffer.addVertex(pose, x0, y0, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(pose, x1, y1, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(pose, x2, y2, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(pose, x3, y3, 0.0F).setColor(r, g, b, a);
    }
}
