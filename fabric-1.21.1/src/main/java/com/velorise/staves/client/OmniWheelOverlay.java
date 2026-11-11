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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * 8-sector Iron's-Spells-style radial wheel for selecting Omni Staff mode
 * (0 = OMNI at top, 1..7 = clockwise Elements) while holding Tab (or custom key).
 */
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

    public static void reset() {
        wheelActive = false;
        suppressedUntilRelease = false;
        hoveredSlot = -1;
        OmniOrbEntity.clearCycleCache();
    }

    public static boolean handleMouseButtonPress(int button, int action) {
        if (!wheelActive || action != GLFW.GLFW_PRESS) return false;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;
        Minecraft mc = Minecraft.getInstance();
        int chosen = hoveredSlot >= 0 ? hoveredSlot : ArcaneStaffItem.getOmniModeIndex(getHeldOmniStack(mc.player));
        commitSelection(mc, chosen);
        wheelActive = false;
        suppressedUntilRelease = true;
        hoveredSlot = -1;
        mc.mouseHandler.grabMouse();
        return true;
    }

    public static boolean shouldSuppressTabList() {
        if (wheelActive || suppressedUntilRelease) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || getHeldOmniStack(mc.player).isEmpty()) return false;
        InputConstants.Key omniKey = KeyBindingHelper.getBoundKeyOf(OMNI_WHEEL_KEY);
        InputConstants.Key listKey = KeyBindingHelper.getBoundKeyOf(mc.options.keyPlayerList);
        return omniKey.equals(listKey) && isWheelKeyHeld(mc);
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
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(OMNI_WHEEL_KEY);
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

        drawDisc(buffer, pose, cx, cy, 0.0D, innerR - 1.5D, 0.05F, 0.06F, 0.10F, 0.80F, 32);

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

        for (int s = 0; s < 8; s++) {
            double edgeA = -Math.PI * 0.5D - sectorSpan * 0.5D + s * sectorSpan;
            drawSpoke(buffer, pose, cx, cy, innerR, rimR, edgeA, 1.35D, 0.85F, 0.78F, 0.55F, 0.55F);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        guiGraphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int s = 0; s < 8; s++) {
            double midA = -Math.PI * 0.5D + s * sectorSpan;
            int iconSize = (s == hoveredSlot) ? 22 : 18;
            int ix = (int) Math.round(cx + Math.cos(midA) * iconDist - iconSize * 0.5D);
            int iy = (int) Math.round(cy + Math.sin(midA) * iconDist - iconSize * 0.5D);
            guiGraphics.blit(SLOT_TEXTURES[s], ix, iy, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        }

        int centerIconSize = 18;
        guiGraphics.blit(SLOT_TEXTURES[previewSlot],
                (int) Math.round(cx - centerIconSize * 0.5D),
                (int) Math.round(cy - centerIconSize * 0.5D),
                0.0F, 0.0F, centerIconSize, centerIconSize, centerIconSize, centerIconSize);

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
        guiGraphics.blit(SLOT_TEXTURES[mode], badgeX + 3, badgeY + 2, 0.0F, 0.0F, 14, 14, 14, 14);
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
