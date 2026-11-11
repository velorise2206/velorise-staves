package com.velorise.staves.client;

import com.velorise.staves.config.StavesConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public class VeloriseConfigScreen extends Screen {
    private final Screen parent;

    public VeloriseConfigScreen(Screen parent) {
        super(Component.translatable("config.velorise_staves.title"));
        this.parent = parent;
    }

    private void saveLocalConfig() {
        boolean isSingleplayerOrMenu = this.minecraft == null || this.minecraft.level == null || this.minecraft.hasSingleplayerServer();
        StavesConfig.save(isSingleplayerOrMenu);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int colWidth = 150;
        int colGap = 12;
        int leftColX = centerX - colWidth - (colGap / 2);
        int rightColX = centerX + (colGap / 2);
        int startY = 44;
        int rowHeight = 25;

        // Row 1: Overhead Icons (Left) & Reaction Text (Right)
        this.addRenderableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.overhead_icons.desc")))
                .create(leftColX, startY, colWidth, 20, Component.translatable("config.velorise_staves.overhead_icons"), (btn, val) -> {
                    StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.set(val);
                }));

        this.addRenderableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_REACTION_TEXT.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.reaction_text.desc")))
                .create(rightColX, startY, colWidth, 20, Component.translatable("config.velorise_staves.reaction_text"), (btn, val) -> {
                    StavesConfig.SHOW_REACTION_TEXT.set(val);
                }));

        // Row 2: Damage Numbers (Left) & Particles (Right)
        this.addRenderableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_DAMAGE_NUMBERS.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.damage_numbers.desc")))
                .create(leftColX, startY + rowHeight, colWidth, 20, Component.translatable("config.velorise_staves.damage_numbers"), (btn, val) -> {
                    StavesConfig.SHOW_DAMAGE_NUMBERS.set(val);
                }));

        this.addRenderableWidget(CycleButton.onOffBuilder(StavesConfig.SHOW_ELEMENTAL_PARTICLES.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.particles.desc")))
                .create(rightColX, startY + rowHeight, colWidth, 20, Component.translatable("config.velorise_staves.particles"), (btn, val) -> {
                    StavesConfig.SHOW_ELEMENTAL_PARTICLES.set(val);
                }));

        // Row 3: EGT System (Left) & Mutate Rate (Right)
        this.addRenderableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_ELEMENTAL_GAUGE.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.egt_system.desc")))
                .create(leftColX, startY + rowHeight * 2, colWidth, 20, Component.translatable("config.velorise_staves.egt_system"), (btn, val) -> {
                    StavesConfig.ENABLE_ELEMENTAL_GAUGE.set(val);
                }));

        List<Double> mutateRates = List.of(0.0D, 0.02D, 0.04D, 0.07D, 0.10D, 0.15D, 0.25D, 0.50D, 1.0D);
        double curRate = StavesConfig.INNATE_MUTATION_CHANCE.get();
        Double initialRate = mutateRates.stream().min((a, b) -> Double.compare(Math.abs(a - curRate), Math.abs(b - curRate))).orElse(0.07D);

        this.addRenderableWidget(CycleButton.<Double>builder(val -> Component.literal(Math.round(val * 100.0) + "%"))
                .withValues(mutateRates)
                .withInitialValue(initialRate)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.mutate_rate.desc")))
                .create(rightColX, startY + rowHeight * 2, colWidth, 20, Component.translatable("config.velorise_staves.mutate_rate"), (btn, val) -> {
                    StavesConfig.INNATE_MUTATION_CHANCE.set(val);
                }));

        // Row 4: Damage Multiplier (Left) & Cooldown Multiplier (Right)
        List<Double> dmgMults = List.of(0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 2.0D, 2.5D, 3.0D);
        double curDmg = StavesConfig.DAMAGE_MULTIPLIER.get();
        Double initialDmg = dmgMults.stream().min((a, b) -> Double.compare(Math.abs(a - curDmg), Math.abs(b - curDmg))).orElse(1.0D);

        this.addRenderableWidget(CycleButton.<Double>builder(val -> Component.literal(String.format(Locale.ROOT, "%.2fx", val)))
                .withValues(dmgMults)
                .withInitialValue(initialDmg)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.damage_mult.desc")))
                .create(leftColX, startY + rowHeight * 3, colWidth, 20, Component.translatable("config.velorise_staves.damage_mult"), (btn, val) -> {
                    StavesConfig.DAMAGE_MULTIPLIER.set(val);
                    this.saveLocalConfig();
                }));

        List<Double> cdMults = List.of(0.25D, 0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 2.0D);
        double curCd = StavesConfig.COOLDOWN_MULTIPLIER.get();
        Double initialCd = cdMults.stream().min((a, b) -> Double.compare(Math.abs(a - curCd), Math.abs(b - curCd))).orElse(1.0D);

        this.addRenderableWidget(CycleButton.<Double>builder(val -> Component.literal(String.format(Locale.ROOT, "%.2fx", val)))
                .withValues(cdMults)
                .withInitialValue(initialCd)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.cooldown_mult.desc")))
                .create(rightColX, startY + rowHeight * 3, colWidth, 20, Component.translatable("config.velorise_staves.cooldown_mult"), (btn, val) -> {
                    StavesConfig.COOLDOWN_MULTIPLIER.set(val);
                    this.saveLocalConfig();
                }));

        // Row 5: Elemental Resonance (Left) & Max Staves Cap (Right)
        this.addRenderableWidget(CycleButton.onOffBuilder(StavesConfig.ENABLE_ELEMENTAL_RESONANCE.get())
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.resonance.desc")))
                .create(leftColX, startY + rowHeight * 4, colWidth, 20, Component.translatable("config.velorise_staves.resonance"), (btn, val) -> {
                    StavesConfig.ENABLE_ELEMENTAL_RESONANCE.set(val);
                    this.saveLocalConfig();
                }));

        List<Integer> maxStavesOpts = List.of(4, 6, 8, 12, 0);
        int curCap = StavesConfig.MAX_STAVES_IN_INVENTORY.get();
        Integer initialCap = maxStavesOpts.contains(curCap) ? curCap : 4;
        this.addRenderableWidget(CycleButton.<Integer>builder(val -> Component.literal(val <= 0 ? "∞" : String.valueOf(val)))
                .withValues(maxStavesOpts)
                .withInitialValue(initialCap)
                .withTooltip(val -> Tooltip.create(Component.translatable("config.velorise_staves.max_staves.desc")))
                .create(rightColX, startY + rowHeight * 4, colWidth, 20, Component.translatable("config.velorise_staves.max_staves"), (btn, val) -> {
                    StavesConfig.MAX_STAVES_IN_INVENTORY.set(val);
                    this.saveLocalConfig();
                }));

        // Bottom Row: Elemental Codex Guide (Left), Reset Defaults (Center), Done (Right)
        int btnW = 98;
        int bottomY = startY + rowHeight * 5 + 9;

        this.addRenderableWidget(Button.builder(Component.translatable("config.velorise_staves.open_codex"), btn -> {
            this.saveLocalConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new ElementalCodexScreen(this));
            }
        }).bounds(centerX - btnW - 54, bottomY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("config.velorise_staves.reset"), btn -> {
            StavesConfig.SHOW_OVERHEAD_ELEMENT_ICONS.set(true);
            StavesConfig.SHOW_REACTION_TEXT.set(true);
            StavesConfig.SHOW_DAMAGE_NUMBERS.set(true);
            StavesConfig.SHOW_ELEMENTAL_PARTICLES.set(true);
            StavesConfig.ENABLE_ELEMENTAL_GAUGE.set(true);
            StavesConfig.ENABLE_ELEMENTAL_RESONANCE.set(true);
            StavesConfig.MAX_STAVES_IN_INVENTORY.set(4);
            StavesConfig.INNATE_MUTATION_CHANCE.set(0.07D);
            StavesConfig.OMNI_MUTATION_SHARE.set(0.143D);
            StavesConfig.ALLOW_HOSTILE_MOB_MUTATION.set(true);
            StavesConfig.ALLOW_GOLEM_MUTATION.set(true);
            StavesConfig.ALLOW_NEUTRAL_AND_PASSIVE_MUTATION.set(false);
            StavesConfig.DAMAGE_MULTIPLIER.set(1.0D);
            StavesConfig.COOLDOWN_MULTIPLIER.set(1.0D);
            this.saveLocalConfig();
            this.clearWidgets();
            this.init();
        }).bounds(centerX - 49, bottomY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            this.saveLocalConfig();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(centerX + 54, bottomY, btnW, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("config.velorise_staves.subtitle"), this.width / 2, 28, 0x88CCFF);
    }

    @Override
    public void onClose() {
        this.saveLocalConfig();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
