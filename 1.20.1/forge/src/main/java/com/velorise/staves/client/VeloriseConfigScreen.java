package com.velorise.staves.client;

import com.velorise.staves.config.StavesConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VeloriseConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 34;
    private static final int SCROLL_STEP = 18;

    private final Screen parent;
    private final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    private final List<Integer> scrollBaseY = new ArrayList<>();
    private final List<AbstractWidget> footerWidgets = new ArrayList<>();

    private int listTop;
    private int listBottom;
    private int contentHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int scrollbarX;
    private boolean draggingScrollbar = false;

    public VeloriseConfigScreen(Screen parent) {
        super(Component.translatable("config.velorise_staves.title"));
        this.parent = parent;
    }

    private void saveLocalConfig() {
        boolean isSingleplayerOrMenu = this.minecraft == null || this.minecraft.level == null || this.minecraft.hasSingleplayerServer();
        StavesConfig.save(isSingleplayerOrMenu);
    }

    private <T extends AbstractWidget> T addScrollableWidget(T widget, int relativeY) {
        this.scrollWidgets.add(widget);
        this.scrollBaseY.add(relativeY);
        return this.addWidget(widget);
    }

    private void updateScrollLayout() {
        int viewportH = Math.max(20, this.listBottom - this.listTop);
        this.maxScroll = Math.max(0, this.contentHeight - viewportH);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

        for (int i = 0; i < this.scrollWidgets.size(); i++) {
            AbstractWidget w = this.scrollWidgets.get(i);
            int y = this.listTop + this.scrollBaseY.get(i) - this.scrollOffset;
            w.setY(y);
            w.visible = (y + w.getHeight() > this.listTop) && (y < this.listBottom);
        }
    }

    @Override
    protected void init() {
        this.scrollWidgets.clear();
        this.scrollBaseY.clear();
        this.footerWidgets.clear();

        int centerX = this.width / 2;
        int colWidth = 150;
        int colGap = 12;
        int leftColX = centerX - colWidth - (colGap / 2);
        int rightColX = centerX + (colGap / 2);
        int rowHeight = 22;

        this.listTop = HEADER_HEIGHT;
        int bottomY = this.height - 26;
        this.listBottom = Math.max(this.listTop + 20, bottomY - 8);
        this.scrollbarX = Math.min(this.width - 8, rightColX + colWidth + 6);

        // Row 1: Overhead Icons (Left) & Reaction Text (Right)
        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.overhead_icons.desc")))
                .create(leftColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.overhead_icons"), (btn, val) -> {
                    StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.set(val);
                }), 0);

        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_REACTION_TEXT.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.reaction_text.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.reaction_text"), (btn, val) -> {
                    StavesConfig.SHOW_REACTION_TEXT.set(val);
                }), 0);

        // Row 2: Damage Numbers (Left) & Particles (Right)
        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_DAMAGE_NUMBERS.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.damage_numbers.desc")))
                .create(leftColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.damage_numbers"), (btn, val) -> {
                    StavesConfig.SHOW_DAMAGE_NUMBERS.set(val);
                }), rowHeight);

        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_ELEMENTAL_PARTICLES.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.particles.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.particles"), (btn, val) -> {
                    StavesConfig.SHOW_ELEMENTAL_PARTICLES.set(val);
                }), rowHeight);

        // Row 3: EGT System (Left) & Mutate Rate (Right)
        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_ELEMENTAL_GAUGE.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.egt_system.desc")))
                .create(leftColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.egt_system"), (btn, val) -> {
                    StavesConfig.ENABLE_ELEMENTAL_GAUGE.set(val);
                }), rowHeight * 2);

        List<Double> mutateRates = List.of(0.0D, 0.02D, 0.04D, 0.08D, 0.10D, 0.15D, 0.25D, 0.50D, 1.0D);
        double curRate = StavesConfig.INNATE_MUTATION_CHANCE.get();
        Double initialRate = mutateRates.stream().min((a, b) -> Double.compare(Math.abs(a - curRate), Math.abs(b - curRate))).orElse(0.08D);

        this.addScrollableWidget(CycleButton.<Double>builder(val -> Component.literal(Math.round(val * 100.0) + "%"))
                .withValues(mutateRates)
                .withInitialValue(initialRate)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.mutate_rate.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.mutate_rate"), (btn, val) -> {
                    StavesConfig.INNATE_MUTATION_CHANCE.set(val);
                }), rowHeight * 2);

        // Row 4: Damage Multiplier (Left) & Cooldown Multiplier (Right)
        List<Double> dmgMults = List.of(0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 2.0D, 2.5D, 3.0D);
        double curDmg = StavesConfig.DAMAGE_MULTIPLIER.get();
        Double initialDmg = dmgMults.stream().min((a, b) -> Double.compare(Math.abs(a - curDmg), Math.abs(b - curDmg))).orElse(1.0D);

        this.addScrollableWidget(CycleButton.<Double>builder(val -> Component.literal(String.format(Locale.ROOT, "%.2fx", val)))
                .withValues(dmgMults)
                .withInitialValue(initialDmg)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.damage_mult.desc")))
                .create(leftColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.damage_mult"), (btn, val) -> {
                    StavesConfig.DAMAGE_MULTIPLIER.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 3);

        List<Double> cdMults = List.of(0.25D, 0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 2.0D);
        double curCd = StavesConfig.COOLDOWN_MULTIPLIER.get();
        Double initialCd = cdMults.stream().min((a, b) -> Double.compare(Math.abs(a - curCd), Math.abs(b - curCd))).orElse(1.0D);

        this.addScrollableWidget(CycleButton.<Double>builder(val -> Component.literal(String.format(Locale.ROOT, "%.2fx", val)))
                .withValues(cdMults)
                .withInitialValue(initialCd)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.cooldown_mult.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.cooldown_mult"), (btn, val) -> {
                    StavesConfig.COOLDOWN_MULTIPLIER.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 3);

        // Row 5: Elemental Resonance (Left) & Max Staves Cap (Right)
        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_ELEMENTAL_RESONANCE.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.resonance.desc")))
                .create(leftColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.resonance"), (btn, val) -> {
                    StavesConfig.ENABLE_ELEMENTAL_RESONANCE.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 4);

        List<Integer> maxStavesOpts = List.of(4, 6, 8, 12, 0);
        int curCap = StavesConfig.MAX_STAVES_IN_INVENTORY.get();
        Integer initialCap = maxStavesOpts.contains(curCap) ? curCap : 4;
        this.addScrollableWidget(CycleButton.<Integer>builder(val -> Component.literal(val <= 0 ? "∞" : String.valueOf(val)))
                .withValues(maxStavesOpts)
                .withInitialValue(initialCap)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.max_staves.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.max_staves"), (btn, val) -> {
                    StavesConfig.MAX_STAVES_IN_INVENTORY.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 4);

        // Row 6: Unified Texture Resolution (16x16px Standard vs 32x32px HD for Element Icons & Wind Glider)
        this.addScrollableWidget(CycleButton.<Boolean>builder(val -> Component.literal(val ? "32x32px (HD)" : "16x16px"))
                .withValues(List.of(false, true))
                .withInitialValue(StavesConfig.isUse32pxTextures())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.texture_res.desc")))
                .create(centerX - 100, 0, 200, 18, Component.translatable("config.velorise_staves.texture_res"), (btn, val) -> {
                    StavesConfig.USE_32PX_TEXTURES.set(val);
                    StavesConfig.USE_32PX_ELEMENT_ICONS.set(val);
                    StavesConfig.USE_32PX_GLIDER_TEXTURE.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 5);

        // Row 7: Universal Plunge Attack & Wind Glider Toggles
        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_PLUNGE_ATTACK.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.enable_plunge.desc")))
                .create(leftColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.enable_plunge"), (btn, val) -> {
                    StavesConfig.ENABLE_PLUNGE_ATTACK.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 6);

        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_WIND_GLIDER.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.enable_glider.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.enable_glider"), (btn, val) -> {
                    StavesConfig.ENABLE_WIND_GLIDER.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 6);

        // Row 8: Character Voice Settings (Left: Full Panel, Right: Quick Master Toggle)
        this.addScrollableWidget(Button.builder(Component.translatable("config.velorise_staves.voice_settings_btn"), btn -> {
            this.saveLocalConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CharacterVoiceScreen(this));
            }
        }).bounds(leftColX, 0, colWidth, 18).build(), rowHeight * 7);

        this.addScrollableWidget(CycleButton.<String>builder(val -> Component.translatable("config.velorise_staves.voice_master_mode." + val))
                .withValues(List.of("all", "self_only", "disabled"))
                .withInitialValue(com.velorise.staves.audio.CharacterVoiceManager.getMasterMode())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.voice_master_mode.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.voice_master_mode", ""), (btn, val) -> {
                    com.velorise.staves.audio.CharacterVoiceManager.setMasterMode(val);
                }), rowHeight * 7);

        // Row 9: Genshin Stamina (100 -> 240) & Ctrl Dash Toggles
        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_CUSTOM_STAMINA.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.enable_custom_stamina.desc")))
                .create(leftColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.enable_custom_stamina"), (btn, val) -> {
                    StavesConfig.ENABLE_CUSTOM_STAMINA.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 8);

        this.addScrollableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_CUSTOM_DASH.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.enable_custom_dash.desc")))
                .create(rightColX, 0, colWidth, 18, Component.translatable("config.velorise_staves.enable_custom_dash"), (btn, val) -> {
                    StavesConfig.ENABLE_CUSTOM_DASH.set(val);
                    this.saveLocalConfig();
                }), rowHeight * 8);

        this.contentHeight = rowHeight * 9 + 2;
        this.updateScrollLayout();

        // Fixed Bottom Row: Elemental Codex Guide (Left), Reset Defaults (Center), Done (Right)
        int btnW = 98;

        this.footerWidgets.add(this.addRenderableWidget(Button.builder(Component.translatable("config.velorise_staves.open_codex"), btn -> {
            this.saveLocalConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new ElementalCodexScreen(this));
            }
        }).bounds(centerX - btnW - 54, bottomY, btnW, 20).build()));

        this.footerWidgets.add(this.addRenderableWidget(Button.builder(Component.translatable("config.velorise_staves.reset"), btn -> {
            StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.set(true);
            StavesConfig.USE_32PX_TEXTURES.set(false);
            StavesConfig.USE_32PX_ELEMENT_ICONS.set(false);
            StavesConfig.USE_32PX_GLIDER_TEXTURE.set(false);
            StavesConfig.SHOW_REACTION_TEXT.set(true);
            StavesConfig.SHOW_DAMAGE_NUMBERS.set(true);
            StavesConfig.SHOW_ELEMENTAL_PARTICLES.set(true);
            StavesConfig.ENABLE_ELEMENTAL_GAUGE.set(true);
            StavesConfig.ENABLE_ELEMENTAL_RESONANCE.set(true);
            StavesConfig.ENABLE_PLUNGE_ATTACK.set(true);
            StavesConfig.ENABLE_WIND_GLIDER.set(true);
            StavesConfig.ENABLE_CUSTOM_STAMINA.set(true);
            StavesConfig.ENABLE_CUSTOM_DASH.set(true);
            StavesConfig.ENABLE_WALL_CLIMB.set(true);
            StavesConfig.MAX_STAVES_IN_INVENTORY.set(4);
            StavesConfig.INNATE_MUTATION_CHANCE.set(0.08D);
            StavesConfig.OMNI_MUTATION_SHARE.set(0.125D);
            StavesConfig.ALLOW_HOSTILE_MOB_MUTATION.set(true);
            StavesConfig.ALLOW_GOLEM_MUTATION.set(true);
            StavesConfig.ALLOW_NEUTRAL_AND_PASSIVE_MUTATION.set(false);
            StavesConfig.DAMAGE_MULTIPLIER.set(1.0D);
            StavesConfig.COOLDOWN_MULTIPLIER.set(1.0D);
            this.saveLocalConfig();
            this.clearWidgets();
            this.init();
        }).bounds(centerX - 49, bottomY, btnW, 20).build()));

        this.footerWidgets.add(this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            this.saveLocalConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(centerX + 54, bottomY, btnW, 20).build()));
    }

    private void applyScrollDelta(double delta) {
        if (this.maxScroll <= 0) return;
        this.scrollOffset = Mth.clamp(this.scrollOffset - (int) Math.round(delta * SCROLL_STEP), 0, this.maxScroll);
        this.updateScrollLayout();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.maxScroll > 0 && mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.applyScrollDelta(delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.maxScroll > 0
                && mouseX >= this.scrollbarX - 2 && mouseX <= this.scrollbarX + 6
                && mouseY >= this.listTop && mouseY <= this.listBottom) {
            this.draggingScrollbar = true;
            this.scrollToMouseY(mouseY);
            return true;
        }
        // Only allow clicks on footer widgets when outside the virtual scroll viewport
        if (mouseY < this.listTop || mouseY > this.listBottom) {
            for (AbstractWidget w : this.footerWidgets) {
                if (w.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(w);
                    if (button == 0) this.setDragging(true);
                    return true;
                }
            }
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar && button == 0) {
            this.scrollToMouseY(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void scrollToMouseY(double mouseY) {
        int trackH = Math.max(1, this.listBottom - this.listTop);
        double frac = Mth.clamp((mouseY - this.listTop) / (double) trackH, 0.0D, 1.0D);
        this.scrollOffset = Mth.clamp((int) Math.round(frac * this.maxScroll), 0, this.maxScroll);
        this.updateScrollLayout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Fixed title header cleanly separated above the scroll viewport
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        // Invisible virtual scroll viewport with scissor clipping
        boolean inViewport = mouseY >= this.listTop && mouseY <= this.listBottom;
        int effMouseX = inViewport ? mouseX : -10000;
        int effMouseY = inViewport ? mouseY : -10000;

        guiGraphics.enableScissor(0, this.listTop, this.width, this.listBottom);
        for (AbstractWidget w : this.scrollWidgets) {
            if (w.visible) {
                w.render(guiGraphics, effMouseX, effMouseY, partialTick);
            }
        }
        guiGraphics.disableScissor();

        // Subtle thin scrollbar thumb only when content overflows
        if (this.maxScroll > 0) {
            int trackH = this.listBottom - this.listTop;
            int thumbH = Math.max(18, (int) Math.round((double) trackH * trackH / (double) this.contentHeight));
            int thumbY = this.listTop + (int) Math.round((double) this.scrollOffset / (double) this.maxScroll * (trackH - thumbH));
            guiGraphics.fill(this.scrollbarX, this.listTop, this.scrollbarX + 3, this.listBottom, 0x33FFFFFF);
            guiGraphics.fill(this.scrollbarX, thumbY, this.scrollbarX + 3, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    @Override
    public void onClose() {
        this.saveLocalConfig();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
