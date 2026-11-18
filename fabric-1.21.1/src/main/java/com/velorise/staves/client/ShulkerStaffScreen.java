package com.velorise.staves.client;

import com.velorise.staves.inventory.ShulkerStaffMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Portable 3-row Shulker storage screen with vertical scrollbar for the 6-row Netherite upgrade.
 */
public class ShulkerStaffScreen extends AbstractContainerScreen<ShulkerStaffMenu> {
    private static final ResourceLocation SHULKER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");
    private static final int SCROLL_TRACK_REL_X = 176;
    private static final int SCROLL_TRACK_REL_Y = 18;
    private static final int SCROLL_TRACK_W = 10;
    private static final int SCROLL_TRACK_H = 54;
    private static final int SCROLL_THUMB_H = 15;

    private boolean scrolling = false;

    public ShulkerStaffScreen(ShulkerStaffMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(SHULKER_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.isNetherite()) {
            // Side scrollbar extension panel styled to match vanilla Shulker Box container border
            int panelX = x + 173;
            int panelY = y + 14;
            int panelW = 18;
            int panelH = 62;
            guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFFC6C6C6);
            guiGraphics.fill(panelX, panelY, panelX + panelW - 1, panelY + 1, 0xFFFFFFFF);
            guiGraphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF555555);
            guiGraphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF555555);

            // Recessed track
            int trackX = x + SCROLL_TRACK_REL_X;
            int trackY = y + SCROLL_TRACK_REL_Y;
            guiGraphics.fill(trackX - 1, trackY - 1, trackX + SCROLL_TRACK_W + 1, trackY + SCROLL_TRACK_H + 1, 0xFF373737);
            guiGraphics.fill(trackX, trackY, trackX + SCROLL_TRACK_W, trackY + SCROLL_TRACK_H, 0xFF8B8B8B);

            // Draggable Shulker-purple / gold-trimmed scrollbar thumb
            int offset = this.menu.getScrollRowOffset();
            int maxOffset = Math.max(1, this.menu.getMaxRowOffset());
            int thumbY = trackY + Math.round(((float) offset / (float) maxOffset) * (SCROLL_TRACK_H - SCROLL_THUMB_H));
            boolean hovered = mouseX >= trackX && mouseX <= trackX + SCROLL_TRACK_W && mouseY >= trackY && mouseY <= trackY + SCROLL_TRACK_H;
            int thumbColor = (this.scrolling || hovered) ? 0xFFA56CD1 : 0xFF8D57B8;
            guiGraphics.fill(trackX, thumbY, trackX + SCROLL_TRACK_W, thumbY + SCROLL_THUMB_H, thumbColor);
            guiGraphics.fill(trackX, thumbY, trackX + SCROLL_TRACK_W - 1, thumbY + 1, 0xFFE3BFFF);
            guiGraphics.fill(trackX, thumbY, trackX + 1, thumbY + SCROLL_THUMB_H - 1, 0xFFE3BFFF);
            guiGraphics.fill(trackX + SCROLL_TRACK_W - 1, thumbY, trackX + SCROLL_TRACK_W, thumbY + SCROLL_THUMB_H, 0xFF4B236E);
            guiGraphics.fill(trackX, thumbY + SCROLL_THUMB_H - 1, trackX + SCROLL_TRACK_W, thumbY + SCROLL_THUMB_H, 0xFF4B236E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        if (this.menu.isNetherite()) {
            int startRow = this.menu.getScrollRowOffset() + 1;
            int endRow = startRow + 2;
            String indicator = startRow + "-" + endRow + "/6";
            int w = this.font.width(indicator);
            guiGraphics.drawString(this.font, indicator, this.imageWidth - 8 - w, this.titleLabelY, 0x6A3A96, false);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        if (this.menu.isNetherite()) {
            if (mouseX >= guiLeft + 173 && mouseX <= guiLeft + 191 && mouseY >= guiTop + 14 && mouseY <= guiTop + 76) {
                return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.menu.isNetherite() && Math.abs(scrollY) > 0.0D) {
            int delta = scrollY > 0.0D ? -1 : 1;
            applyScrollOffset(this.menu.getScrollRowOffset() + delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.isNetherite() && isInsideScrollbar(mouseX, mouseY)) {
            this.scrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.menu.isNetherite()) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsideScrollbar(double mouseX, double mouseY) {
        int trackX = this.leftPos + SCROLL_TRACK_REL_X - 1;
        int trackY = this.topPos + SCROLL_TRACK_REL_Y - 1;
        return mouseX >= trackX && mouseX <= trackX + SCROLL_TRACK_W + 2
                && mouseY >= trackY && mouseY <= trackY + SCROLL_TRACK_H + 2;
    }

    private void updateScrollFromMouse(double mouseY) {
        int trackY = this.topPos + SCROLL_TRACK_REL_Y;
        float rel = (float) (mouseY - trackY - SCROLL_THUMB_H * 0.5D) / (float) Math.max(1, SCROLL_TRACK_H - SCROLL_THUMB_H);
        int target = Math.round(Mth.clamp(rel, 0.0F, 1.0F) * this.menu.getMaxRowOffset());
        applyScrollOffset(target);
    }

    private void applyScrollOffset(int rawOffset) {
        int next = Mth.clamp(rawOffset, 0, this.menu.getMaxRowOffset());
        if (next != this.menu.getScrollRowOffset()) {
            this.menu.setClientScrollOffset(next);
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, next);
            }
        }
    }
}
